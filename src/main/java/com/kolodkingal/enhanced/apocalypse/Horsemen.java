package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Four Horsemen (spec 3.8). In the film Apocalypse "enhances" Storm, Psylocke, Archangel and Magneto; in the mod, at
 * the first transition, he enhances <em>Husks</em> (vanilla desert mobs: no new models, and they fit the sand): twice the
 * health, four more attack damage, quicker, harder, glowing, with a name above their heads, and one trait each from the
 * film, run by a server timer. They are marked (the fight's mark, and their kind in their persistent data), drop nothing
 * and give no experience, so they are never farmed, and they live until killed or until the second transition, when they
 * crumble to sand.
 *
 * <ul>
 *     <li><b>War</b> (Magneto's slot): iron helmet, chestplate and axe, so it is <em>magnetic</em>; every 100 ticks a
 *     Shard Volley (a tell of 12 ticks, then three iron shards in a fan, 4 damage each);</li>
 *     <li><b>Famine</b> (Storm's slot): every 120 ticks a Gale (14 ticks of white lightning on its hands, then a bolt
 *     beside the target, visual only, and a gust: knockback 1.6 within 6 blocks, 3 damage);</li>
 *     <li><b>Pestilence</b> (Psylocke's slot): iron sword; every 100 ticks a Psi-Blade Dash (the sword glows violet for 10
 *     ticks, then a 6-block leap: 7 damage and Slowness I for 3 seconds);</li>
 *     <li><b>Death</b> (Archangel's slot): every 140 ticks a Dive (it springs up and hangs for 14 ticks, then a diving
 *     slam of radius 2.5, 9 damage).</li>
 * </ul>
 */
public final class Horsemen {
    /** The Horsemen in the order the first fighters get them. */
    public enum Kind {
        WAR(100, 12), FAMINE(120, 14), PESTILENCE(100, 10), DEATH(140, 14);

        private final int period;
        private final int tell;

        Kind(int period, int tell) {
            this.period = period;
            this.tell = tell;
        }

        /** The ticks between two uses of its trait. */
        public int period() {
            return period;
        }

        /** The ticks of the tell. */
        public int tell() {
            return tell;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static @Nullable Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id().equals(id)) {
                    return kind;
                }
            }
            return null;
        }
    }

    /** The persistent data key that marks a Husk as a Horseman, and says which. */
    public static final String MARK = Enhanced.MODID + ":horseman";

    private static final ResourceLocation HEALTH_ID = Enhanced.id("horseman_health");
    private static final ResourceLocation DAMAGE_ID = Enhanced.id("horseman_damage");
    private static final ResourceLocation SPEED_ID = Enhanced.id("horseman_speed");
    private static final ResourceLocation ARMOR_ID = Enhanced.id("horseman_armor");

    private static final class Rider {
        final Husk husk;
        final Kind kind;
        long nextAt;
        int tell;
        Vec3 aim = Vec3.ZERO;
        /** Ticks left of a leap or a dive that is still hurting what it meets. */
        int strike;
        boolean struck;

        Rider(Husk husk, Kind kind, long nextAt) {
            this.husk = husk;
            this.kind = kind;
            this.nextAt = nextAt;
        }
    }

    private final BossFight fight;
    private final List<Rider> riders = new ArrayList<>();

    public Horsemen(BossFight fight) {
        this.fight = fight;
    }

    /** The kind of Horseman an entity is, or null if it is none. */
    @Nullable
    public static Kind kindOf(Entity entity) {
        return entity.getPersistentData().contains(MARK) ? Kind.byId(entity.getPersistentData().getString(MARK)) : null;
    }

    /** How many Horsemen the fight calls for {@code fighters} fighters: {@code min(horsemenMax, 1 + fighters)}. */
    public static int countFor(int fighters) {
        return ApocalypseBossConfig.HORSEMEN.get() ? Math.max(0, Math.min(ApocalypseBossConfig.HORSEMEN_MAX.get(), 1 + fighters)) : 0;
    }

    /** The Horsemen who are alive. */
    public List<Husk> alive() {
        List<Husk> alive = new ArrayList<>();
        for (Rider rider : riders) {
            if (rider.husk.isAlive() && !rider.husk.isRemoved()) {
                alive.add(rider.husk);
            }
        }
        return alive;
    }

    // ---- calling them ----

    /** A Husk of the fight, enhanced and named, at {@code at}: the Horseman of {@code kind}. */
    public Husk spawn(ServerLevel level, Kind kind, Vec3 at) {
        Husk husk = EntityType.HUSK.create(level);
        husk.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        husk.setPersistenceRequired();
        husk.setCanPickUpLoot(false);
        husk.setBaby(false);
        husk.getPersistentData().putString(MARK, kind.id());
        enhance(husk);
        switch (kind) {
            case WAR -> {
                husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                husk.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            }
            case PESTILENCE -> husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            default -> {
            }
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            husk.setDropChance(slot, 0.0F);
        }
        husk.setGlowingTag(true);
        husk.setCustomName(Component.translatable("boss.enhanced.apocalypse.horseman", Component.translatable("horseman.enhanced." + kind.id()))
                .withStyle(ChatFormatting.GOLD));
        husk.setCustomNameVisible(true);
        fight.spawn(husk);
        riders.add(new Rider(husk, kind, level.getGameTime() + 40 + riders.size() * 15L));
        ApocalypseFx.send(level, at, ApocalypseFxPayload.PORTAL, -1, at.add(0, 1.0, 0), at.add(0, 1.0, 0), 30, 15);
        level.playSound(null, at.x, at.y, at.z, ModSounds.APOCALYPSE_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        return husk;
    }

    /** Twice the health, four more damage, quicker, harder: what he does to the Husks he calls. */
    private static void enhance(Husk husk) {
        modify(husk, Attributes.MAX_HEALTH, HEALTH_ID, ApocalypseBossConfig.HORSEMAN_HEALTH_FACTOR.get() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modify(husk, Attributes.ATTACK_DAMAGE, DAMAGE_ID, ApocalypseBossConfig.HORSEMAN_DAMAGE_BONUS.get(), AttributeModifier.Operation.ADD_VALUE);
        modify(husk, Attributes.MOVEMENT_SPEED, SPEED_ID, 0.04, AttributeModifier.Operation.ADD_VALUE);
        modify(husk, Attributes.ARMOR, ARMOR_ID, 4.0, AttributeModifier.Operation.ADD_VALUE);
        husk.setHealth(husk.getMaxHealth());
    }

    private static void modify(Husk husk, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                               ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = husk.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /** They crumble to sand (the second transition, or the end of a lost fight): no drops, no experience. */
    public void crumble() {
        for (Rider rider : riders) {
            if (rider.husk.isAlive() && !rider.husk.isRemoved() && rider.husk.level() instanceof ServerLevel level) {
                SandControl.puff(level, rider.husk.position().add(0, 1.0, 0), 30, 0.5);
                level.playSound(null, rider.husk.getX(), rider.husk.getY(), rider.husk.getZ(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F);
                rider.husk.discard();
            }
        }
        riders.clear();
    }

    // ---- their traits ----

    /** One tick of every Horseman's timer. */
    public void tick(ServerLevel level, List<ServerPlayer> targets) {
        riders.removeIf(rider -> !rider.husk.isAlive() || rider.husk.isRemoved());
        long now = level.getGameTime();
        for (Rider rider : riders) {
            tickRider(level, rider, now, targets);
        }
    }

    private void tickRider(ServerLevel level, Rider rider, long now, List<ServerPlayer> targets) {
        Husk husk = rider.husk;
        if (SlowMotion.skipTick(husk)) {
            return; // Quicksilver Time: the Husk lives slower, and so does its trick
        }
        if (rider.strike > 0) {
            strike(level, rider);
            return;
        }
        if (rider.tell == 0) {
            if (now < rider.nextAt) {
                return;
            }
            LivingEntity target = husk.getTarget() != null && husk.getTarget().isAlive() ? husk.getTarget() : nearest(husk, targets);
            if (target == null || husk.distanceTo(target) > 24.0F || !husk.hasLineOfSight(target)) {
                return;
            }
            rider.tell = 1;
            rider.aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
            begin(level, rider);
            return;
        }
        LivingEntity target = husk.getTarget() != null && husk.getTarget().isAlive() ? husk.getTarget() : nearest(husk, targets);
        if (target != null && rider.tell < rider.kind.tell() - 2) {
            rider.aim = target.position().add(0, target.getBbHeight() * 0.5, 0); // it tracks until the last moments
        }
        telling(level, rider);
        if (++rider.tell > rider.kind.tell()) {
            rider.tell = 0;
            rider.nextAt = now + rider.kind.period();
            fire(level, rider);
        }
    }

    @Nullable
    private static ServerPlayer nearest(Husk husk, List<ServerPlayer> targets) {
        ServerPlayer best = null;
        double distance = Double.MAX_VALUE;
        for (ServerPlayer player : targets) {
            double d = player.distanceToSqr(husk);
            if (d < distance && !player.isCreative()) {
                distance = d;
                best = player;
            }
        }
        return best;
    }

    private void begin(ServerLevel level, Rider rider) {
        Husk husk = rider.husk;
        switch (rider.kind) {
            case DEATH -> {
                // It springs up and hangs.
                husk.setDeltaMovement(husk.getDeltaMovement().x, 0.7, husk.getDeltaMovement().z);
                husk.hurtMarked = true;
            }
            case WAR -> level.playSound(null, husk.getX(), husk.getY(), husk.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.0F, 0.6F);
            case FAMINE -> level.playSound(null, husk.getX(), husk.getY(), husk.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.3F, 2.0F);
            case PESTILENCE -> level.playSound(null, husk.getX(), husk.getY(), husk.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    private void telling(ServerLevel level, Rider rider) {
        Husk husk = rider.husk;
        Vec3 hand = husk.position().add(0, husk.getBbHeight() * 0.8, 0);
        switch (rider.kind) {
            case WAR -> level.sendParticles(ParticleTypes.CRIT, hand.x, hand.y + 0.4, hand.z, 2, 0.3, 0.2, 0.3, 0.02);
            case FAMINE -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 4, 0.3, 0.3, 0.3, 0.05);
            case PESTILENCE -> level.sendParticles(ParticleTypes.WITCH, hand.x, hand.y, hand.z, 3, 0.3, 0.3, 0.3, 0.02);
            case DEATH -> {
                // It hangs for the rest of the tell.
                if (rider.tell > 6) {
                    husk.setDeltaMovement(0.0, 0.0, 0.0);
                    husk.setNoGravity(true);
                }
                level.sendParticles(ParticleTypes.CLOUD, husk.getX(), husk.getY(), husk.getZ(), 2, 0.3, 0.1, 0.3, 0.02);
            }
        }
    }

    private void fire(ServerLevel level, Rider rider) {
        Husk husk = rider.husk;
        Vec3 eye = husk.getEyePosition();
        Vec3 toward = rider.aim.subtract(eye);
        switch (rider.kind) {
            case WAR -> {
                for (int i = -1; i <= 1; i++) {
                    Vec3 direction = rotateY(toward.normalize(), Math.toRadians(8.0 * i));
                    IronShrapnelEntity shard = new IronShrapnelEntity(level, husk);
                    shard.configure(4.0F, 1);
                    shard.setPos(eye.x, eye.y - 0.2, eye.z);
                    shard.shoot(direction.x, direction.y, direction.z, 2.2F, 0.0F);
                    fight.spawn(shard);
                }
                level.playSound(null, husk.getX(), husk.getY(), husk.getZ(), ModSounds.APOCALYPSE_SHIELD_HIT.get(), SoundSource.HOSTILE, 0.7F, 0.6F);
            }
            case FAMINE -> {
                Vec3 at = new Vec3(rider.aim.x + 2.0, SandControl.groundAt(level, rider.aim.x + 2.0, rider.aim.y + 1.0, rider.aim.z), rider.aim.z);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                bolt.moveTo(at);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(6.0, 3.0, 6.0),
                        e -> e != husk && SandControl.canHurt(husk, e))) {
                    if (victim.position().distanceToSqr(at) > 36.0) {
                        continue;
                    }
                    victim.hurt(level.damageSources().mobAttack(husk), 3.0F);
                    Vec3 out = SandControl.flat(at, victim.position());
                    victim.knockback(1.6, -out.x, -out.z);
                    victim.hurtMarked = true;
                }
            }
            case PESTILENCE -> {
                Vec3 flat = SandControl.flat(husk.position(), rider.aim);
                husk.setDeltaMovement(flat.x * 1.0, 0.35, flat.z * 1.0);
                husk.hurtMarked = true;
                rider.strike = 8;
                rider.struck = false;
                level.playSound(null, husk.getX(), husk.getY(), husk.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.7F);
            }
            case DEATH -> {
                husk.setNoGravity(false);
                Vec3 flat = SandControl.flat(husk.position(), rider.aim);
                husk.setDeltaMovement(flat.x * 0.5, -1.2, flat.z * 0.5);
                husk.hurtMarked = true;
                rider.strike = 14;
                rider.struck = false;
            }
            default -> {
            }
        }
    }

    /** A leap or a dive in flight: it hurts what it meets, once. */
    private void strike(ServerLevel level, Rider rider) {
        Husk husk = rider.husk;
        rider.strike--;
        if (rider.struck) {
            return;
        }
        if (rider.kind == Kind.PESTILENCE) {
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, husk.getBoundingBox().inflate(0.9), e -> e != husk && SandControl.canHurt(husk, e))) {
                victim.hurt(level.damageSources().mobAttack(husk), 7.0F);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), husk);
                rider.struck = true;
                break;
            }
        } else if (rider.kind == Kind.DEATH && (husk.onGround() || rider.strike == 0)) {
            Vec3 at = husk.position();
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, husk.getBoundingBox().inflate(2.5, 1.5, 2.5), e -> e != husk && SandControl.canHurt(husk, e))) {
                if (Math.hypot(victim.getX() - at.x, victim.getZ() - at.z) <= 2.5) {
                    victim.hurt(level.damageSources().mobAttack(husk), 9.0F);
                }
            }
            SandControl.puff(level, at.add(0, 0.3, 0), 20, 0.8);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6F, 1.4F);
            rider.struck = true;
        }
    }

    private static Vec3 rotateY(Vec3 v, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }
}

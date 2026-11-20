package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.ModuleLogic;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 3.18.1: the witches' cantrips ({@code PowerModule.CANTRIP}), the small spells a witch's empty keys hold until her levels
 * open a spell of her own there ({@link SpellLevels#fill}): one per role, in her magic's colour, a little stronger with
 * each level (the pick's strength is her level). Weak on purpose: a real spell opened must feel like a step up.
 * <ul>
 * <li>{@code ward}, the mini shield (protection): a small shield that takes up to 6 damage for four seconds.</li>
 * <li>{@code blink}, the blink step (mobility): a quick leap of 5 blocks ahead, and no fall damage for two seconds.</li>
 * <li>{@code mend} (healing): 4 health back over four seconds.</li>
 * <li>{@code repel} (control): a wave of magic that pushes back every enemy within 4 blocks and does each 1 damage.</li>
 * <li>{@code spark} (attack): a little bolt of magic, 3 damage, up to 20 blocks.</li>
 * <li>{@code light} (helper): a ball of light that goes with her for a minute (she sees in the dark), and every hostile
 * creature within 16 blocks glows for five seconds.</li>
 * </ul>
 */
public final class Cantrips implements ModuleLogic {
    /** A shield up: how much it still takes, and until when. */
    private record Ward(float left, long until) {
    }

    /** A healing under way: how much each second, and until when. */
    private record Mend(float perSecond, long until) {
    }

    /** The mini shield's room in the body's absorption while it is up. */
    private static final net.minecraft.resources.ResourceLocation WARD_ID = com.kolodkingal.enhanced.Enhanced.id("cantrip_ward");
    private static final Map<UUID, Ward> WARDS = new HashMap<>();
    private static final Map<UUID, Mend> MENDS = new HashMap<>();
    private static final Map<UUID, Long> LIGHTS = new HashMap<>();

    @Override
    public void key(Ctx c, boolean down) {
        if (!down || !c.ready()) {
            return;
        }
        String option = c.pick.option();
        switch (option) {
            case "blink" -> blink(c);
            case "mend" -> mend(c);
            case "repel" -> repel(c);
            case "spark" -> spark(c);
            case "light" -> light(c);
            default -> ward(c);
        }
    }

    private static void ward(Ctx c) {
        float amount = c.v(ModuleStats.CANTRIP_WARD_SHIELD);
        ServerPlayer p = c.player;
        WARDS.put(p.getUUID(), new Ward(amount, c.now() + c.ticks(ModuleStats.CANTRIP_WARD_TIME)));
        // (absorption is held to the most a body may absorb: the little shield raises that while it is up)
        room(p, amount);
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), amount));
        Shots.ring(c.level(), p.position().add(0, 1.0, 0), 1.2, c.color());
        c.sparks(p.position().add(0, 1.0, 0), 16, 0.5, 1.0F);
        c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.2F);
        c.cooldown(ModuleStats.CANTRIP_WARD_COOLDOWN);
    }

    private static void blink(Ctx c) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level();
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            c.refuse("message.enhanced.cantrip.blink_blocked");
            return;
        }
        flat = flat.normalize();
        double distance = c.v(ModuleStats.CANTRIP_BLINK_DISTANCE);
        Vec3 from = p.position();
        Vec3 to = null;
        for (double d = distance; d >= 1.0; d -= 0.5) {
            Vec3 at = from.add(flat.scale(d));
            if (level.noCollision(p, p.getBoundingBox().move(at.subtract(from))) && Shots.hexCut(level, p.getEyePosition(),
                    at.add(0, p.getEyeHeight(), 0)) == null) {
                to = at;
                break;
            }
        }
        if (to == null) {
            c.refuse("message.enhanced.cantrip.blink_blocked");
            return;
        }
        level.sendParticles(new EnergyParticleOptions(c.color(), 1.2F), from.x, from.y + 1.0, from.z, 14, 0.3, 0.5, 0.3, 0.02);
        p.teleportTo(to.x, to.y, to.z);
        p.resetFallDistance();
        CustomHeroes.guardFall(p, 40);
        level.sendParticles(new EnergyParticleOptions(c.glow(), 1.2F), to.x, to.y + 1.0, to.z, 14, 0.3, 0.5, 0.3, 0.02);
        c.sound(SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.6F);
        c.cooldown(ModuleStats.CANTRIP_BLINK_COOLDOWN);
    }

    private static void mend(Ctx c) {
        int ticks = c.ticks(ModuleStats.CANTRIP_MEND_TIME);
        float perSecond = c.v(ModuleStats.CANTRIP_MEND_HEAL) / Math.max(1.0F, ticks / 20.0F);
        MENDS.put(c.player.getUUID(), new Mend(perSecond, c.now() + ticks));
        c.level().sendParticles(ParticleTypes.HEART, c.player.getX(), c.player.getY() + 1.8, c.player.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        c.sparks(c.player.position().add(0, 1.0, 0), 12, 0.4, 1.0F);
        c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.4F);
        c.cooldown(ModuleStats.CANTRIP_MEND_COOLDOWN);
    }

    private static void repel(Ctx c) {
        ServerPlayer p = c.player;
        double radius = c.v(ModuleStats.CANTRIP_REPEL_RADIUS);
        float damage = c.v(ModuleStats.CANTRIP_REPEL_DAMAGE);
        for (LivingEntity e : Shots.area(p, p.position().add(0, 1.0, 0), radius)) {
            if (e instanceof Enemy || e instanceof net.minecraft.world.entity.player.Player) {
                Shots.push(e, p.position(), 1.1, 0.3);
                Shots.hurt(p, e, damage, ModDamageTypes.HERO_MAGIC);
            }
        }
        Shots.ring(c.level(), p.position().add(0, 0.2, 0), radius, c.color());
        Shots.ring(c.level(), p.position().add(0, 0.9, 0), radius * 0.6, c.glow());
        c.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.3F);
        c.cooldown(ModuleStats.CANTRIP_REPEL_COOLDOWN);
    }

    private static void spark(Ctx c) {
        ServerPlayer p = c.player;
        Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.CANTRIP_SPARK_RANGE));
        Shots.line(c.level(), Shots.hand(p), aim.end(), c.color(), c.glow());
        c.level().sendParticles(ParticleTypes.ENCHANT, aim.end().x, aim.end().y, aim.end().z, 10, 0.2, 0.2, 0.2, 0.4);
        if (aim.target() != null && AreaTargets.mayHurt(p, aim.target(), true)) {
            Shots.hurt(p, aim.target(), c.v(ModuleStats.CANTRIP_SPARK_DAMAGE), ModDamageTypes.HERO_MAGIC);
        }
        c.sound(ModSounds.CUSTOM_SPELL.get(), 0.6F, 1.8F);
        c.cooldown(ModuleStats.CANTRIP_SPARK_COOLDOWN);
    }

    private static void light(Ctx c) {
        ServerPlayer p = c.player;
        int ticks = c.ticks(ModuleStats.CANTRIP_LIGHT_TIME);
        LIGHTS.put(p.getUUID(), c.now() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks, 0, true, false, true));
        double radius = c.v(ModuleStats.CANTRIP_LIGHT_RADIUS);
        AABB box = p.getBoundingBox().inflate(radius);
        for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e instanceof Enemy)) {
            if (e.distanceTo(p) <= radius) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), p);
            }
        }
        c.sparks(p.getEyePosition().add(0, 0.6, 0), 20, 0.3, 1.4F);
        c.sound(SoundEvents.AMETHYST_CLUSTER_PLACE, 1.0F, 1.5F);
        c.cooldown(ModuleStats.CANTRIP_LIGHT_COOLDOWN);
    }

    @Override
    public void tick(Ctx c) {
        ServerPlayer p = c.player;
        UUID id = p.getUUID();
        long now = c.now();
        Ward ward = WARDS.get(id);
        if (ward != null && now >= ward.until()) {
            // (what is left of the little shield goes; absorption from anything else stays)
            float left = Math.max(0.0F, p.getAbsorptionAmount() - Math.min(ward.left(), p.getAbsorptionAmount()));
            room(p, 0.0F);
            p.setAbsorptionAmount(left);
            WARDS.remove(id);
        } else if (ward != null && p.getAbsorptionAmount() < ward.left()) {
            WARDS.put(id, new Ward(p.getAbsorptionAmount(), ward.until()));
        }
        Mend mend = MENDS.get(id);
        if (mend != null) {
            if (now >= mend.until()) {
                MENDS.remove(id);
            } else if (p.tickCount % 20 == 0) {
                p.heal(mend.perSecond());
                c.level().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.9, p.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            }
        }
        Long light = LIGHTS.get(id);
        if (light != null) {
            if (now >= light) {
                LIGHTS.remove(id);
            } else if (p.tickCount % 4 == 0) {
                Vec3 side = Vec3.directionFromRotation(0, p.getYRot() + 60.0F).scale(0.7);
                Vec3 at = p.getEyePosition().add(side).add(0, 0.4 + 0.1 * Math.sin(now / 6.0), 0);
                c.level().sendParticles(new EnergyParticleOptions(c.glow(), 1.3F), at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
                c.level().sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    @Override
    public void end(Ctx c) {
        UUID id = c.player.getUUID();
        Ward ward = WARDS.remove(id);
        if (ward != null) {
            float left = Math.max(0.0F, c.player.getAbsorptionAmount() - ward.left());
            room(c.player, 0.0F);
            c.player.setAbsorptionAmount(left);
        }
        MENDS.remove(id);
        LIGHTS.remove(id);
    }

    /** The mini shield's room in the body's absorption (a modifier never saved; 0: none). */
    private static void room(ServerPlayer p, float amount) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance max = p.getAttribute(Attributes.MAX_ABSORPTION);
        if (max == null) {
            return;
        }
        max.removeModifier(WARD_ID);
        if (amount > 0.0F) {
            max.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(WARD_ID, amount,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void forget(ServerPlayer player) {
        if (WARDS.containsKey(player.getUUID())) {
            room(player, 0.0F);
        }
        WARDS.remove(player.getUUID());
        MENDS.remove(player.getUUID());
        LIGHTS.remove(player.getUUID());
    }

    @Override
    public void clear() {
        WARDS.clear();
        MENDS.clear();
        LIGHTS.clear();
    }

    /** Whether this player has a mini shield up (the tests). */
    public static boolean warded(ServerPlayer player) {
        return WARDS.containsKey(player.getUUID());
    }

    /** Whether a healing of the mend is under way (the tests). */
    public static boolean mending(ServerPlayer player) {
        return MENDS.containsKey(player.getUUID());
    }
}

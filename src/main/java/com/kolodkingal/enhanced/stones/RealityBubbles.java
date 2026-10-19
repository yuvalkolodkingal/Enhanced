package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.custom.StoneRow;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.StonePowers;
import com.kolodkingal.enhanced.thanos.ThanosEffects;
import com.kolodkingal.enhanced.thanos.ThanosSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Reality Stone (3.6), "the chaos stone". Passive, the <b>Reality Bubbles</b>: while he holds it bare, or selected
 * in a Gauntlet he wields (or a custom hero's {@code stone.reality} switch is on and he carries it), every projectile
 * heading for him within {@code [reality_stone] radius} turns into harmless bubbles that pop, as Thanos turned the
 * attacks of Titan (the tick turns the ones it sees coming; {@link #catches} the one too fast for it). Not the {@code
 * reality_proof} ones (Mjolnir, Stormbreaker, Gungnir, the Titan's own), nor his own or his pets'; beams and
 * explosions are no projectiles. A boss's are turned too, but in a boss arena at most one every {@code bossRate}
 * ticks, at {@code bossCost} times the hunger: a volley still gets through in part. Use, the <b>Transmute</b>: the
 * creature he looks at turns to stone for a while (bosses shake it off). Sneak + use, the <b>Illusion</b>: he vanishes
 * and a red-shimmering copy of him walks on, and whoever hunted him hunts it. Server only.
 */
public final class RealityBubbles {
    /** Hunger of one Transmute, and of one Illusion. */
    static final float TRANSMUTE_EXHAUSTION = 1.0F;
    static final float ILLUSION_EXHAUSTION = 1.0F;
    /** How far the Illusion's copy walks off, and how far the creatures that hunted him are turned to it. */
    static final double ILLUSION_WALK = 12.0;
    static final double ILLUSION_LURE = 24.0;

    /** Player -> the game time a bubble was last taken in a boss arena (the boss rate). */
    private static final Map<UUID, Long> LAST_BOSS_BUBBLE = new HashMap<>();
    /** Player -> how many bubbles his stone has made (the HUD's count). */
    private static final Map<UUID, Integer> COUNT = new HashMap<>();
    /** The Reality Stone's illusions alive, and whose (they shimmer red while they last). */
    private static final Map<UUID, UUID> ILLUSIONS = new HashMap<>();

    // ---- the bubbles ----

    /** Whether the player's Reality Stone turns projectiles into bubbles now. */
    public static boolean active(Player player, PowerData data) {
        if (player.isSpectator() || !player.isAlive() || StoneUse.restrained(player)) {
            return false;
        }
        if (StoneUse.inHand(player, StoneKind.REALITY)) {
            return true;
        }
        return data.power() == Power.CUSTOM && data.kit() != null && data.kit().has(StoneRow.REALITY)
                && data.toggle(Toggle.REALITY_VEIL) && StoneUse.carried(player, StoneKind.REALITY);
    }

    /** How many projectiles this player's Reality Stone has turned since he logged in (the HUD's count). */
    public static int count(Player player) {
        return COUNT.getOrDefault(player.getUUID(), 0);
    }

    static void tick(ServerPlayer player, PowerData data) {
        if (!ILLUSIONS.isEmpty() && player.tickCount % 3 == 0) {
            shimmer(player);
        }
        if (!active(player, data)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 centre = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        double radius = StonesConfig.REALITY_RADIUS.get();
        AABB box = new AABB(centre, centre).inflate(radius);
        List<Vec3> popped = new ArrayList<>();
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box,
                p -> !p.getType().is(ModTags.REALITY_PROOF) && !ownedBy(p.getOwner(), player))) {
            Vec3 toCentre = centre.subtract(projectile.position());
            if (projectile.getDeltaMovement().dot(toCentre) <= 0.0 || toCentre.lengthSqr() > radius * radius
                    || Shelters.keepsOut(level, player, centre, projectile.position(), false)) {
                continue;
            }
            if (!pay(player, level)) {
                break;
            }
            pop(level, projectile);
            popped.add(projectile.position());
        }
        if (!popped.isEmpty()) {
            tell(player, popped);
        }
    }

    /**
     * A projectile is about to hit the player: his Reality Stone takes it, if it is active and the boss rate allows.
     *
     * @return whether it was taken (the impact is then cancelled)
     */
    public static boolean catches(ServerPlayer player, Projectile projectile) {
        PowerData data = com.kolodkingal.enhanced.power.Powers.get(player);
        if (!active(player, data) || projectile.getType().is(ModTags.REALITY_PROOF)
                || ownedBy(projectile.getOwner(), player)) {
            return false;
        }
        if (!pay(player, player.serverLevel())) {
            return false;
        }
        Vec3 at = projectile.position();
        if (!StonePowers.realityCatches(player, projectile)) {
            return false;
        }
        tell(player, List.of(at));
        return true;
    }

    /**
     * What one bubble costs: its hunger (more in a boss arena), and in a boss arena only one every {@code bossRate}
     * ticks. False if this one cannot be taken now.
     */
    private static boolean pay(ServerPlayer player, ServerLevel level) {
        float exhaustion = StonesConfig.REALITY_EXHAUSTION_PER_BUBBLE.get().floatValue();
        if (BossFights.inAnyArena(level, player.blockPosition())) {
            long now = level.getGameTime();
            Long last = LAST_BOSS_BUBBLE.get(player.getUUID());
            if (last != null && now - last < StonesConfig.REALITY_BOSS_RATE.get() && now >= last) {
                return false;
            }
            LAST_BOSS_BUBBLE.put(player.getUUID(), now);
            exhaustion *= StonesConfig.REALITY_BOSS_COST.get().floatValue();
        }
        player.causeFoodExhaustion(exhaustion);
        COUNT.merge(player.getUUID(), 1, Integer::sum);
        return true;
    }

    private static void pop(ServerLevel level, Projectile projectile) {
        Vec3 at = projectile.position();
        level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.02);
        level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.core(), 1.4F), at.x, at.y, at.z, 6, 0.15, 0.15,
                0.15, 0.02);
        ThanosSounds.REALITY_POP.play(level, at);
        projectile.discard();
    }

    /** The clients near see where the bubbles popped (the holder's screen ripples red at its edge). */
    private static void tell(ServerPlayer player, List<Vec3> popped) {
        List<Vec3> points = popped.size() > StoneFxPayload.MAX_POINTS ? popped.subList(0, StoneFxPayload.MAX_POINTS)
                : popped;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new StoneFxPayload(StoneFxPayload.BUBBLE,
                player.getId(), List.copyOf(points), count(player)));
    }

    /** Whether {@code entity} is {@code holder} or a pet of his. */
    private static boolean ownedBy(@Nullable Entity entity, LivingEntity holder) {
        return entity == holder || entity instanceof TamableAnimal pet && pet.isOwnedBy(holder);
    }

    /** A custom hero's {@code stone.reality} switch: on (he must carry the stone for it to work) or off. */
    static void toggle(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        PowerData data = cast.data();
        boolean on = !data.toggle(Toggle.REALITY_VEIL);
        data.setToggle(Toggle.REALITY_VEIL, on);
        if (on) {
            ServerLevel level = player.serverLevel();
            ThanosSounds.REALITY_POP.play(level, player.position(), 1.0F, 0.9F);
            StoneKind.REALITY.voice().use(level, player.position(), 0.6F, 1.0F);
            level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.core(), 1.2F), player.getX(),
                    player.getY() + 1.0, player.getZ(), 16, 0.5, 0.8, 0.5, 0.05);
        }
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.stones.reality.on"
                : "message.enhanced.stones.reality.off"), true);
    }

    // ---- the Transmute ----

    /** The creature he looks at turns to stone: a red wave from the stone, and it is grey for a while. */
    static void transmute(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = player.serverLevel();
        StoneAim.Aim aim = StoneAim.aim(player, StonesConfig.REALITY_TRANSMUTE_RANGE.get(), 0.3,
                target -> !(target instanceof ArmorStand));
        LivingEntity target = aim.creature();
        if (target == null || StonePowers.untouchable(target)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.no_target"), true);
            return;
        }
        if (StoneAim.pvpRefuses(player, target)) {
            return;
        }
        Vec3 hand = StoneAim.hand(player);
        Vec3 chest = StoneAim.chestOf(target);
        cast.startCooldown(StonesConfig.REALITY_TRANSMUTE_COOLDOWN.get());
        player.causeFoodExhaustion(TRANSMUTE_EXHAUSTION);
        StoneKind.REALITY.voice().use(level, player.position(), 0.6F, 1.0F);
        ThanosEffects.beam(level, player, hand, chest, 0.35F, 8, StoneKind.REALITY.color());
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, hand, chest, 1.0F);
        if (shield != null) {
            if (shield.owner() instanceof ServerPlayer wanda && player.canHarmPlayer(wanda)) {
                WandaAbilities.wear(wanda, 2.0F);
            }
            Vec3 at = shield.point();
            level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.core(), 1.2F), at.x, at.y, at.z, 6, 0.15,
                    0.15, 0.15, 0.1);
            return;
        }
        if (target instanceof LokiIllusionEntity illusion) {
            illusion.dissolve(chest);
            return;
        }
        if (StonePowers.petrify(level, player, target, StonesConfig.REALITY_TRANSMUTE_TICKS.get())) {
            ThanosSounds.REALITY_POP.play(level, target.position());
            ThanosEffects.wave(level, target.position(), 1.5F, 10, StoneKind.REALITY.color());
        } else {
            // A boss shakes the stone off: it cracks and falls.
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    chest.x, chest.y, chest.z, 14, 0.3, 0.4, 0.3, 0.1);
            ThanosSounds.REALITY_POP.play(level, target.position(), 1.0F, 0.6F);
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.shaken_off"), true);
        }
    }

    // ---- the Illusion ----

    /**
     * He vanishes (Invisibility, his name hidden) and a copy of him, shimmering red, walks on the way he looked; the
     * creatures that hunted him within {@link #ILLUSION_LURE} hunt the copy instead.
     */
    static void illusion(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = player.serverLevel();
        int ticks = StonesConfig.REALITY_ILLUSION_TICKS.get();
        cast.startCooldown(StonesConfig.REALITY_ILLUSION_COOLDOWN.get());
        player.causeFoodExhaustion(ILLUSION_EXHAUSTION);
        Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot());
        LokiIllusionEntity copy = LokiIllusionEntity.conjure(player, player.position(),
                player.position().add(look.scale(ILLUSION_WALK)), LokiIllusionEntity.Mode.REALITY, ticks);
        ILLUSIONS.put(copy.getUUID(), player.getUUID());
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(ILLUSION_LURE),
                m -> m.getTarget() == player)) {
            mob.setTarget(copy);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.REALITY_ILLUSION.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        StoneKind.REALITY.voice().use(level, player.position(), 0.6F, 1.1F);
        ThanosEffects.illusion(level, player.position(), 1.6F, 20);
        level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.color(), 1.4F), player.getX(),
                player.getY() + 1.0, player.getZ(), 24, 0.4, 0.8, 0.4, 0.05);
    }

    /** Whether this illusion is a Reality Stone's (the tests ask). */
    public static boolean isStoneIllusion(Entity entity) {
        return ILLUSIONS.containsKey(entity.getUUID());
    }

    /** The illusions of this player shimmer red, and the ones that are gone are forgotten. */
    private static void shimmer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ILLUSIONS.entrySet().removeIf(e -> {
            if (!e.getValue().equals(player.getUUID())) {
                return false;
            }
            Entity copy = level.getEntity(e.getKey());
            if (copy == null || copy.isRemoved()) {
                return true;
            }
            level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.color(), 0.9F), copy.getX(),
                    copy.getY() + copy.getBbHeight() * 0.5, copy.getZ(), 2, 0.3, 0.6, 0.3, 0.01);
            return false;
        });
    }

    static void forget(Player player) {
        LAST_BOSS_BUBBLE.remove(player.getUUID());
        COUNT.remove(player.getUUID());
        ILLUSIONS.values().removeIf(owner -> owner.equals(player.getUUID()));
    }

    static void clear() {
        LAST_BOSS_BUBBLE.clear();
        COUNT.clear();
        ILLUSIONS.clear();
    }

    private RealityBubbles() {
    }
}

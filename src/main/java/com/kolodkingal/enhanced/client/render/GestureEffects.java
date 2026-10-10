package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The small flourishes that go with the acting of a gesture: motes gathering into the Scarlet Witch's hands as she raises
 * her shield and a flash when she throws it open, sparks where Thor catches his weapon, the glow of a raised hand. Each is
 * started with its gesture, comes from the places the model really has hands ({@link HandTracker}), and is thinned by
 * distance and by the {@link EnhancedClientConfig#flightEffects} switch like the flight effects.
 */
public final class GestureEffects {
    /** What a timed effect does each tick it lasts. */
    private enum Kind {
        /** Crimson motes drawn into the chest from both hands (the shield gathering). */
        SHIELD_GATHER,
        /** The shield thrown open: a ring and a burst from the chest. */
        SHIELD_FLASH,
        /** Crimson motes rising off both raised hands (the Hex growing). */
        HEX_RISE,
        /** A glow at the casting hand in the hero's colour. */
        CAST_HAND,
        /** Motes at both hands (Billy's freeze). */
        CAST_BOTH,
        /** Jittering motes at the main hand (Tommy's vibrating palm). */
        VIBRATE_HAND
    }

    private record Timed(int player, long from, long to, Kind kind) {
    }

    private static final List<Timed> TIMED = new ArrayList<>();

    /** A gesture just began for this hero: start what goes with it. */
    static void started(AbstractClientPlayer player, HeroAnimState state, GestureId id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible() || !EnhancedClientConfig.flightEffects()) {
            return;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (player.position().distanceToSqr(camera) > 64.0 * 64.0) {
            return;
        }
        long now = level.getGameTime();
        switch (id) {
            case THOR_CATCH -> catchSparks(level, player, state);
            case THOR_BEAM_KICK, THOR_BEAM_KICK_BARE -> kickSparks(level, player, state);
            case WANDA_SHIELD_RAISE -> {
                add(player, now + 4, now + 14, Kind.SHIELD_GATHER);
                add(player, now + 15, now + 15, Kind.SHIELD_FLASH);
            }
            case WANDA_SHIELD_SNAP -> add(player, now + 4, now + 4, Kind.SHIELD_FLASH);
            case WANDA_HEX_RISE -> add(player, now + 8, now + 36, Kind.HEX_RISE);
            case LOKI_CAST -> add(player, now + 3, now + 9, Kind.CAST_HAND);
            case MAG_DISARM -> add(player, now + 5, now + 8, Kind.CAST_HAND);
            case BILLY_FREEZE_CAST -> add(player, now + 5, now + 20, Kind.CAST_BOTH);
            case TOMMY_ACCEL -> add(player, now + 2, now + 7, Kind.VIBRATE_HAND);
            default -> {
            }
        }
    }

    private static void add(AbstractClientPlayer player, long from, long to, Kind kind) {
        TIMED.add(new Timed(player.getId(), from, to, kind));
    }

    /** Runs the timed effects of this client tick. */
    public static void tick(ClientLevel level) {
        if (TIMED.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        TIMED.removeIf(t -> now > t.to());
        for (Timed t : TIMED) {
            if (now < t.from() || !(level.getEntity(t.player()) instanceof AbstractClientPlayer player) || player.isInvisible()) {
                continue;
            }
            HeroAnimState state = HeroAnimator.peek(player);
            if (state == null) {
                continue;
            }
            switch (t.kind()) {
                case SHIELD_GATHER -> gather(level, player);
                case SHIELD_FLASH -> flash(level, player);
                case HEX_RISE -> rise(level, player);
                case CAST_HAND -> castHand(level, player, state, false);
                case CAST_BOTH -> {
                    castHand(level, player, state, true);
                    castHand(level, player, state, false);
                }
                case VIBRATE_HAND -> vibrate(level, player);
            }
        }
    }

    public static void clear() {
        TIMED.clear();
    }

    // ---- the effects ----

    /** The middle of a hero's chest. */
    private static Vec3 chest(AbstractClientPlayer player) {
        Vec3 shoulder = HandTracker.shoulder(player, HumanoidArm.RIGHT, 0.0F);
        Vec3 other = HandTracker.shoulder(player, HumanoidArm.LEFT, 0.0F);
        return shoulder != null && other != null ? shoulder.add(other).scale(0.5).add(0.0, -0.2, 0.0)
                : player.position().add(0.0, player.getBbHeight() * 0.65, 0.0);
    }

    /** A hand of the hero: the tracked one, or (the local player in first person) where energy leaves the hand. */
    private static Vec3 hand(AbstractClientPlayer player, HumanoidArm arm) {
        Vec3 tracked = HandTracker.hand(player, arm, 0.0F);
        if (tracked != null) {
            return tracked;
        }
        return arm == player.getMainArm() ? WorldEffectsRenderer.handPosition(Minecraft.getInstance(), player, 1.0F)
                : player.position().add(0.0, player.getBbHeight() * 0.6, 0.0).add(player.getLookAngle().scale(0.6));
    }

    private static void gather(ClientLevel level, AbstractClientPlayer player) {
        RandomSource random = level.random;
        Vec3 chest = chest(player);
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 hand = hand(player, arm);
            Vec3 toChest = chest.subtract(hand);
            for (int i = 0; i < 2; i++) {
                Vec3 at = hand.add((random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3);
                level.addParticle(new EnergyParticleOptions(i == 0 ? Power.SCARLET_WITCH.color : Power.SCARLET_WITCH.glow, 0.9F), at.x, at.y, at.z,
                        toChest.x * 0.12, toChest.y * 0.12, toChest.z * 0.12);
            }
        }
    }

    private static void flash(ClientLevel level, AbstractClientPlayer player) {
        Vec3 chest = chest(player);
        Vec3 look = player.getLookAngle();
        FlightEffects.ring(level, player.getId(), chest, look, 8, 0.3F, 2.4F, 0.12F, Power.SCARLET_WITCH.glow, 0.7F);
        RandomSource random = level.random;
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            Vec3 out = new Vec3(Math.cos(angle), (random.nextDouble() - 0.5) * 0.8, Math.sin(angle)).normalize().scale(0.18);
            level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? Power.SCARLET_WITCH.color : Power.SCARLET_WITCH.glow, 1.1F), chest.x, chest.y, chest.z,
                    out.x, out.y, out.z);
        }
    }

    private static void rise(ClientLevel level, AbstractClientPlayer player) {
        RandomSource random = level.random;
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 hand = hand(player, arm);
            level.addParticle(new EnergyParticleOptions(random.nextInt(3) == 0 ? Power.SCARLET_WITCH.glow : Power.SCARLET_WITCH.color, 1.0F),
                    hand.x + (random.nextDouble() - 0.5) * 0.2, hand.y, hand.z + (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.02,
                    0.05 + random.nextDouble() * 0.03, (random.nextDouble() - 0.5) * 0.02);
        }
    }

    private static void castHand(ClientLevel level, AbstractClientPlayer player, HeroAnimState state, boolean other) {
        RandomSource random = level.random;
        HumanoidArm arm = other ? player.getMainArm().getOpposite() : player.getMainArm();
        Vec3 hand = hand(player, arm);
        int color = state.power == Power.BILLY ? BillyAbilities.MAIN : state.power.color;
        int glow = state.power == Power.BILLY ? BillyAbilities.INNER : state.power.glow;
        for (int i = 0; i < 2; i++) {
            level.addParticle(new EnergyParticleOptions(i == 0 ? color : glow, 0.9F), hand.x + (random.nextDouble() - 0.5) * 0.25,
                    hand.y + (random.nextDouble() - 0.5) * 0.25, hand.z + (random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.04,
                    0.02 + random.nextDouble() * 0.03, (random.nextDouble() - 0.5) * 0.04);
        }
    }

    private static void vibrate(ClientLevel level, AbstractClientPlayer player) {
        RandomSource random = level.random;
        Vec3 hand = hand(player, player.getMainArm());
        for (int i = 0; i < 3; i++) {
            level.addParticle(new EnergyParticleOptions(i == 0 ? Power.TOMMY.glow : Power.TOMMY.color, 0.7F), hand.x + (random.nextDouble() - 0.5) * 0.2,
                    hand.y + (random.nextDouble() - 0.5) * 0.2, hand.z + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.0, 0.0);
        }
    }

    /** The weapon slaps into Thor's hand: a burst of sparks and a small ring. */
    private static void catchSparks(ClientLevel level, AbstractClientPlayer player, HeroAnimState state) {
        InteractionHand weaponHand = ThorAbilities.weaponHand(player);
        HumanoidArm arm = weaponHand == null || weaponHand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        Vec3 hand = hand(player, arm);
        RandomSource random = level.random;
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.3) * 0.4,
                    (random.nextDouble() - 0.5) * 0.5);
        }
        Vec3 toHand = hand.subtract(player.getEyePosition());
        FlightEffects.ring(level, player.getId(), hand, toHand.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : toHand.normalize(), 5, 0.2F, 0.9F, 0.06F, 0x4DB8FF, 0.8F);
    }

    /** The lightning leaves Thor's hand: a few sparks thrown back at the muzzle. */
    private static void kickSparks(ClientLevel level, AbstractClientPlayer player, HeroAnimState state) {
        Vec3 muzzle = HandTracker.weaponHead(player, 0.0F);
        if (muzzle == null) {
            muzzle = hand(player, player.getMainArm());
        }
        RandomSource random = level.random;
        Vec3 back = player.getLookAngle().scale(-0.2);
        for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, muzzle.x, muzzle.y, muzzle.z, back.x + (random.nextDouble() - 0.5) * 0.3,
                    back.y + (random.nextDouble() - 0.5) * 0.3, back.z + (random.nextDouble() - 0.5) * 0.3);
        }
    }

    /** A blue glint at the tip of Loki's Scepter as it touches. */
    static void scepterGlint(AbstractClientPlayer player) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible() || !EnhancedClientConfig.flightEffects()) {
            return;
        }
        Vec3 hand = hand(player, player.getMainArm());
        Vec3 tip = hand.add(player.getLookAngle().scale(0.45)).add(0.0, 0.4, 0.0);
        RandomSource random = level.random;
        for (int i = 0; i < 2; i++) {
            level.addParticle(new EnergyParticleOptions(0x4DB8FF, 0.6F), tip.x + (random.nextDouble() - 0.5) * 0.1, tip.y + (random.nextDouble() - 0.5) * 0.1,
                    tip.z + (random.nextDouble() - 0.5) * 0.1, 0.0, 0.01, 0.0);
        }
    }

    private GestureEffects() {
    }
}

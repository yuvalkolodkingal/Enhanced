package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
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

    /**
     * An effect that runs from tick {@code from} to {@code to} of the game's clock. A shield effect belongs to a
     * shield being raised: it stops if the shield is let go of (or refused) before it forms, so nothing is thrown open
     * that never formed.
     */
    private static final class Timed {
        final int player;
        final long born;
        final long from;
        final long to;
        final Kind kind;
        /** 3.5: the ability the effect is of (its colour), or null. */
        @Nullable
        final HeroAbility row;
        /** The data has shown the shield being raised. */
        boolean seen;

        Timed(int player, long born, long from, long to, Kind kind, @Nullable HeroAbility row) {
            this.row = row;
            this.player = player;
            this.born = born;
            this.from = from;
            this.to = to;
            this.kind = kind;
        }

        boolean isShield() {
            return kind == Kind.SHIELD_GATHER || kind == Kind.SHIELD_FLASH;
        }
    }

    private static final List<Timed> TIMED = new ArrayList<>();

    /** A gesture just began for this hero ({@code param} as in {@link HeroAnimator#start}): start what goes with it. */
    static void started(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
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
                // The motes gather while the arms do (t4 to t14 of the keys), the flash is the arms thrown open (t15):
                // both stretched with the wind-up, so the shield is complete as they open.
                float k = GestureRoutes.shieldRate(Math.round(param));
                add(player, now, now + Math.round(4 / k), now + Math.max(Math.round(14 / k), Math.round(4 / k)), Kind.SHIELD_GATHER,
                        HeroAbility.WANDA_CHAOS_SHIELD);
                add(player, now, now + Math.round(15 / k), now + Math.round(15 / k), Kind.SHIELD_FLASH,
                        HeroAbility.WANDA_CHAOS_SHIELD);
            }
            case WANDA_SHIELD_SNAP -> add(player, now, now + 4, now + 4, Kind.SHIELD_FLASH,
                    HeroAbility.WANDA_CHAOS_SHIELD);
            case WANDA_HEX_RISE -> add(player, now, now + 8, now + 36, Kind.HEX_RISE, HeroAbility.WANDA_HEX);
            case LOKI_CAST -> add(player, now, now + 3, now + 9, Kind.CAST_HAND,
                    AbilityColors.first(player, HeroAbility.LOKI_SHAPESHIFT, HeroAbility.LOKI_VANISH));
            case MAG_DISARM -> add(player, now, now + 5, now + 8, Kind.CAST_HAND, HeroAbility.MAGNETO_DISARM);
            case MAG_PULL -> add(player, now, now + 5, now + 12, Kind.CAST_HAND, HeroAbility.MAGNETO_MAGNETIC_PULL);
            case BILLY_FREEZE_CAST -> add(player, now, now + 5, now + 20, Kind.CAST_BOTH, HeroAbility.BILLY_FREEZE);
            case TOMMY_ACCEL -> add(player, now, now + 2, now + 7, Kind.VIBRATE_HAND, HeroAbility.TOMMY_ACCELERATE);
            default -> {
                // The heroes of 3.0 start the flourishes of their own gestures themselves.
                HeroClient hero = HeroClients.of(state.power);
                if (hero != null) {
                    hero.gestureStarted(player, state, id, param);
                }
            }
        }
    }

    private static void add(AbstractClientPlayer player, long born, long from, long to, Kind kind,
            @Nullable HeroAbility row) {
        TIMED.add(new Timed(player.getId(), born, from, to, kind, row));
    }

    /** 3.5: the colour a custom hero's pick chose for the effect being made, or -1. */
    private static int tint = -1;

    /** A colour of the effect, or it recoloured to the colour chosen for it. */
    private static int c(int rgb) {
        return tint < 0 ? rgb : AbilityColors.recolor(rgb, tint);
    }

    /** Runs the timed effects of this client tick. */
    public static void tick(ClientLevel level) {
        if (TIMED.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        TIMED.removeIf(t -> now > t.to || t.isShield() && shieldGone(level, t, now));
        for (Timed t : TIMED) {
            if (now < t.from || !(level.getEntity(t.player) instanceof AbstractClientPlayer player) || player.isInvisible()) {
                continue;
            }
            HeroAnimState state = HeroAnimator.peek(player);
            if (state == null) {
                continue;
            }
            tint = t.row == null ? -1 : AbilityColors.chosen(player, t.row);
            switch (t.kind) {
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
        tint = -1;
    }

    /** Whether the shield that a shield effect belongs to was let go of or refused before it could be thrown open. */
    private static boolean shieldGone(ClientLevel level, Timed t, long now) {
        if (!(level.getEntity(t.player) instanceof AbstractClientPlayer player)) {
            return true;
        }
        boolean up = Powers.get(player).chaosShieldActive();
        t.seen |= up;
        return GestureRoutes.shieldRaiseAborted(up, t.seen, now - t.born);
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

    /**
     * How large the particles of an effect at a hero's hands are drawn: the local player's own hands are a hand's
     * length from the camera in first person, where a full-size mote would fill a tenth of the screen.
     */
    private static float size(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson() ? 0.4F : 1.0F;
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
        int color = c(CustomActing.tint(player, Power.SCARLET_WITCH.color, false));
        int glow = c(CustomActing.tint(player, Power.SCARLET_WITCH.glow, true));
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 hand = hand(player, arm);
            Vec3 toChest = chest.subtract(hand);
            for (int i = 0; i < 2; i++) {
                Vec3 at = hand.add((random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3);
                level.addParticle(new EnergyParticleOptions(i == 0 ? color : glow, 0.9F * size(player)), at.x, at.y, at.z,
                        toChest.x * 0.12, toChest.y * 0.12, toChest.z * 0.12);
            }
        }
    }

    private static void flash(ClientLevel level, AbstractClientPlayer player) {
        Vec3 chest = chest(player);
        Vec3 look = player.getLookAngle();
        int color = c(CustomActing.tint(player, Power.SCARLET_WITCH.color, false));
        int glow = c(CustomActing.tint(player, Power.SCARLET_WITCH.glow, true));
        FlightEffects.ring(level, player.getId(), chest, look, 8, 0.3F, 2.4F, 0.12F, glow, 0.7F);
        RandomSource random = level.random;
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            Vec3 out = new Vec3(Math.cos(angle), (random.nextDouble() - 0.5) * 0.8, Math.sin(angle)).normalize().scale(0.18);
            level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? color : glow, 1.1F * size(player)), chest.x, chest.y, chest.z,
                    out.x, out.y, out.z);
        }
    }

    private static void rise(ClientLevel level, AbstractClientPlayer player) {
        RandomSource random = level.random;
        int color = c(CustomActing.tint(player, Power.SCARLET_WITCH.color, false));
        int glow = c(CustomActing.tint(player, Power.SCARLET_WITCH.glow, true));
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 hand = hand(player, arm);
            level.addParticle(new EnergyParticleOptions(random.nextInt(3) == 0 ? glow : color, 1.0F * size(player)),
                    hand.x + (random.nextDouble() - 0.5) * 0.2, hand.y, hand.z + (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.02,
                    0.05 + random.nextDouble() * 0.03, (random.nextDouble() - 0.5) * 0.02);
        }
    }

    private static void castHand(ClientLevel level, AbstractClientPlayer player, HeroAnimState state, boolean other) {
        RandomSource random = level.random;
        HumanoidArm arm = other ? player.getMainArm().getOpposite() : player.getMainArm();
        Vec3 hand = hand(player, arm);
        // (3.4: a custom hero's hand glows in his own colours.)
        int color = c(state.power == Power.BILLY ? BillyAbilities.MAIN : CustomActing.tint(player, state.power.color, false));
        int glow = c(state.power == Power.BILLY ? BillyAbilities.INNER : CustomActing.tint(player, state.power.glow, true));
        for (int i = 0; i < 2; i++) {
            level.addParticle(new EnergyParticleOptions(i == 0 ? color : glow, 0.9F * size(player)), hand.x + (random.nextDouble() - 0.5) * 0.25,
                    hand.y + (random.nextDouble() - 0.5) * 0.25, hand.z + (random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.04,
                    0.02 + random.nextDouble() * 0.03, (random.nextDouble() - 0.5) * 0.04);
        }
    }

    private static void vibrate(ClientLevel level, AbstractClientPlayer player) {
        RandomSource random = level.random;
        Vec3 hand = hand(player, player.getMainArm());
        int color = c(CustomActing.tint(player, Power.TOMMY.color, false));
        int glow = c(CustomActing.tint(player, Power.TOMMY.glow, true));
        for (int i = 0; i < 3; i++) {
            level.addParticle(new EnergyParticleOptions(i == 0 ? glow : color, 0.7F * size(player)), hand.x + (random.nextDouble() - 0.5) * 0.2,
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
            level.addParticle(new EnergyParticleOptions(0x4DB8FF, 0.6F * size(player)), tip.x + (random.nextDouble() - 0.5) * 0.1, tip.y + (random.nextDouble() - 0.5) * 0.1,
                    tip.z + (random.nextDouble() - 0.5) * 0.1, 0.0, 0.01, 0.0);
        }
    }

    private GestureEffects() {
    }
}

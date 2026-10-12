package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.client.render.AnimMath.lerp;
import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * How En Sabah Nur acts what he does: the rows of his two stances, what his two stances and six gestures add to the hands
 * in first person, the shield's raise played at the speed of its wind-up, and the small flourishes that go with each
 * gesture (sand off the palms at the Wave, the hand closing on the Entomb, the gold gathering into the shield). The big
 * effects of his abilities (the ridge of the Wave, the stripe of the Entomb, the portal, the storm) are drawn from the
 * server's payloads by {@link SandFx}, the same for everyone.
 */
final class ApocalypseActing {
    private static final ParticleOptions SAND = SandParticles.GRAIN;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    // ====================================================================================================================
    // The stances
    // ====================================================================================================================

    /** The aimed rows: the Shield (both forearms up, palms out) and the Storm (both arms flung wide, the head back). */
    static void aimed(PoseContext c) {
        HeroAnimState st = c.st;
        float w;
        if ((w = st.stance(Stance.APOC_SHIELD)) > 0.001F) {
            c.arm(true, StancePoses.APOC_SHIELD_M[0], StancePoses.APOC_SHIELD_M[1], StancePoses.APOC_SHIELD_M[2], w);
            c.arm(false, StancePoses.APOC_SHIELD_O[0], StancePoses.APOC_SHIELD_O[1], StancePoses.APOC_SHIELD_O[2], w);
            c.head(StancePoses.APOC_SHIELD_HEAD, 0, 0, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.APOC_STORM)) > 0.001F) {
            c.arm(true, StancePoses.APOC_STORM_M[0], StancePoses.APOC_STORM_M[1], StancePoses.APOC_STORM_M[2], w);
            c.arm(false, StancePoses.APOC_STORM_O[0], StancePoses.APOC_STORM_O[1], StancePoses.APOC_STORM_O[2], w);
            c.head(StancePoses.APOC_STORM_HEAD, 0, 0, w);
            // The sleeves billow a little in the wind he raised; his legs are planted (not in the air).
            c.tremble(true, 1.4F, 0.35F, w);
            c.tremble(false, 1.4F, 0.35F, w);
            float planted = w * (1.0F - st.wAir);
            c.leg(true, 6, 3, planted);
            c.leg(false, -6, -3, planted);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    // ====================================================================================================================
    // First person
    // ====================================================================================================================

    /** What the stances and gestures of the hero add to his hands in first person (the rows of the design, 6.5). */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        if ((w = s.stance(Stance.APOC_SHIELD)) > 0.001F) {
            both(m, o, w, -0.12F, 0.30F, 0.12F, -44, 8, 0);
        }
        if ((w = s.stance(Stance.APOC_STORM)) > 0.001F) {
            // The arms are flung wide and up: the hands go out to the edges of the screen and rise, the palms turned outward.
            both(m, o, w, 0.20F, 0.18F, 0.02F, -22, -20, -16);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w2 = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case APOC_SAND_WAVE -> {
                    // The hands sweep forward and down.
                    float u = ss(0, 4, t);
                    both(m, o, w2, lerp(u, -0.10F, -0.14F), lerp(u, 0.10F, 0.04F), lerp(u, 0.0F, 0.26F), lerp(u, -10, -58), 6, 0);
                }
                case APOC_ENTOMB -> m.add(w2, -0.10F, 0.26F, 0.22F, -52, 8, 0); // a hand reaching ahead
                case APOC_PORTAL -> {
                    // The arms spread: the hands go out to the edges of the screen, turned outward (the design's row drew them
                    // together, which is how they come back across the body, not how they open).
                    float u = ss(0, 5, t);
                    float back = ss(7, 12, t);
                    both(m, o, w2, lerp(u, -0.10F, 0.20F) - 0.34F * back, lerp(u, 0.10F, 0.16F) - 0.04F * back, lerp(u, 0.0F, 0.06F) + 0.10F * back,
                            lerp(u, -10, -14) - 8 * back, lerp(u, 6, -24) + 40 * back, 0);
                }
                case APOC_BESTOW -> m.add(w2, -0.12F, 0.20F, 0.24F, -34, 6, 0); // a hand offered
                default -> {
                }
            }
        }
    }

    private static void both(FirstPersonArms.Offset m, FirstPersonArms.Offset o, float w, float dx, float dy, float dz, float rx, float ry, float rz) {
        m.add(w, dx, dy, dz, rx, ry, rz);
        o.add(w, dx, dy, dz, rx, ry, rz);
    }

    // ====================================================================================================================
    // The edges of a tick
    // ====================================================================================================================

    /**
     * Before the generic edges of the gesture table: the take-off's flourish, the shield's raise retimed to the wind-up
     * the data says, and the cooldown the Sandstorm starts is not read as a Bestowal.
     */
    static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        float age = p.tickCount;
        // The take-off (the same condition as the animator's own): a ring of sand and a burst round his feet.
        if (!s.prevFlyingEff && s.flyingEff && (s.onGround || s.airTicks <= 8)) {
            ApocalypseFlight.takeoff(p);
        }
        // The raise of the shield is keyed for fifteen ticks and played so that the arms are open as the sphere is complete.
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active && slot.id == GestureId.APOC_SHIELD_RAISE && slot.param == 0.0F && Float.isNaN(slot.releaseStart)) {
                int windUp = windUp(data);
                float rate = ApocalypseHeroMath.raiseRate(windUp);
                float t = (age - slot.start) * slot.rate;
                slot.rate = rate;
                slot.start = age - t / rate;
                slot.param = Math.max(1, windUp);
            }
        }
        // The Sandstorm begins with a cooldown on N, like a Bestowal: the table would act both, and the second would cut
        // the first. Tell it the cooldown is known.
        if (data.is(Live.SANDSTORM) && !s.table.lives[Live.SANDSTORM.ordinal()]) {
            s.table.cooldown[5] = Math.max(s.table.cooldown[5], data.cooldownUntil(5));
        }
    }

    /**
     * Whether the local player can pay for the shield (it costs hunger). Kept here and not in the plug-in: the plug-in is
     * loaded by the game tests on a server, and it must not need a client class (the local player) to be verified.
     */
    static boolean canPayForShield() {
        net.minecraft.client.player.LocalPlayer self = Minecraft.getInstance().player;
        return self != null && ApocalypseGuard.canAfford(self);
    }

    /** The wind-up (ticks) of the shield that is being raised: the data's if it has said, else the config's. */
    static int windUp(PowerData data) {
        if (data.chaosShieldActive() && data.shieldFormedAt() > data.shieldStart()) {
            return (int) (data.shieldFormedAt() - data.shieldStart());
        }
        return ApocalypseGuard.windUpTicks();
    }

    // ====================================================================================================================
    // The flourishes of the gestures
    // ====================================================================================================================

    private enum Kind {
        /** Sand thrown off both palms as the Wave is flung down. */
        WAVE_THROW,
        /** The hand closes: sand and gold drawn into the fist. */
        ENTOMB_CLENCH,
        /** Violet motes off both hands as the arms spread. */
        PORTAL_SPREAD,
        /** Gold motes drawn from both hands into the chest. */
        SHIELD_GATHER,
        /** The arms thrown open: a ring and a burst of gold. */
        SHIELD_FLASH,
        /** The arms flung up: a ring of sand on the ground and a burst. */
        STORM_BURST
    }

    private record Timed(int player, long from, long to, Kind kind) {
    }

    private static final List<Timed> TIMED = new ArrayList<>();
    private static final int MAX_TIMED = 48;

    /** A gesture of the hero just began: start what goes with it. */
    static void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        switch (id) {
            case APOC_SAND_WAVE -> add(player, now + 9, now + 9, Kind.WAVE_THROW);
            case APOC_ENTOMB -> add(player, now + 18, now + 18, Kind.ENTOMB_CLENCH);
            case APOC_PORTAL -> add(player, now + 5, now + 12, Kind.PORTAL_SPREAD);
            case APOC_SHIELD_RAISE -> {
                // The gold gathers while the arms do (t4 to t14 of the keys) and is thrown open at t15, stretched with the wind-up.
                int windUp = param > 0 ? Math.round(param) : windUp(com.kolodkingal.enhanced.power.Powers.get(player));
                float k = ApocalypseHeroMath.raiseRate(windUp);
                add(player, now + Math.round(4 / k), now + Math.max(Math.round(14 / k), Math.round(4 / k)), Kind.SHIELD_GATHER);
                add(player, now + Math.round(15 / k), now + Math.round(15 / k), Kind.SHIELD_FLASH);
            }
            case APOC_STORM_START -> add(player, now + 11, now + 11, Kind.STORM_BURST);
            default -> {
            }
        }
    }

    private static void add(AbstractClientPlayer player, long from, long to, Kind kind) {
        if (quiet()) {
            return;
        }
        if (TIMED.size() >= MAX_TIMED) {
            TIMED.remove(0);
        }
        TIMED.add(new Timed(player.getId(), from, to, kind));
    }

    /** Runs the flourishes of this client tick. */
    static void tick(ClientLevel level) {
        if (TIMED.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        TIMED.removeIf(t -> now > t.to());
        if (quiet()) {
            return;
        }
        for (Timed t : TIMED) {
            if (now < t.from() || !(level.getEntity(t.player()) instanceof AbstractClientPlayer player) || player.isInvisible()) {
                continue;
            }
            switch (t.kind()) {
                case WAVE_THROW -> waveThrow(level, player);
                case ENTOMB_CLENCH -> entombClench(level, player);
                case PORTAL_SPREAD -> portalSpread(level, player);
                case SHIELD_GATHER -> gather(level, player);
                case SHIELD_FLASH -> flash(level, player);
                case STORM_BURST -> stormBurst(level, player);
            }
        }
    }

    static void clear() {
        TIMED.clear();
    }

    // ---- the flourishes ----

    private static void waveThrow(ClientLevel level, AbstractClientPlayer p) {
        RandomSource r = level.random;
        Vec3 look = flat(p);
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 h = hand(p, arm);
            for (int i = 0; i < Math.round(7 * size(p) + 2); i++) {
                level.addParticle(SAND, h.x, h.y, h.z, look.x * 0.28 + (r.nextDouble() - 0.5) * 0.14, -0.04 + r.nextDouble() * 0.07,
                        look.z * 0.28 + (r.nextDouble() - 0.5) * 0.14);
            }
        }
        FlightEffects.ring(level, p.getId(), p.position().add(look.scale(1.3)).add(0, 0.06, 0), UP, 7, 0.3F, 2.2F, 0.14F, SandPalette.MAIN, 0.7F);
    }

    private static void entombClench(ClientLevel level, AbstractClientPlayer p) {
        RandomSource r = level.random;
        Vec3 h = hand(p, p.getMainArm());
        for (int i = 0; i < 8; i++) {
            Vec3 at = h.add((r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 0.9);
            Vec3 in = h.subtract(at).scale(0.18);
            level.addParticle(i % 3 == 0 ? new EnergyParticleOptions(SandPalette.GOLD, 0.7F * size(p)) : SAND, at.x, at.y, at.z, in.x, in.y, in.z);
        }
    }

    private static void portalSpread(ClientLevel level, AbstractClientPlayer p) {
        RandomSource r = level.random;
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 h = hand(p, arm);
            for (int i = 0; i < 2; i++) {
                level.addParticle(new EnergyParticleOptions(i == 0 ? SandPalette.VIOLET : SandPalette.VIOLET_CORE, 0.8F * size(p)),
                        h.x + (r.nextDouble() - 0.5) * 0.2, h.y + (r.nextDouble() - 0.5) * 0.2, h.z + (r.nextDouble() - 0.5) * 0.2,
                        (r.nextDouble() - 0.5) * 0.05, 0.02 + r.nextDouble() * 0.03, (r.nextDouble() - 0.5) * 0.05);
            }
        }
    }

    private static void gather(ClientLevel level, AbstractClientPlayer p) {
        RandomSource r = level.random;
        Vec3 chest = chest(p);
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 h = hand(p, arm);
            Vec3 in = chest.subtract(h);
            double d = Math.max(0.2, in.length());
            for (int i = 0; i < 2; i++) {
                level.addParticle(new EnergyParticleOptions(i == 0 ? SandPalette.GOLD : SandPalette.GOLD_CORE, 0.8F * size(p)),
                        h.x + (r.nextDouble() - 0.5) * 0.2, h.y + (r.nextDouble() - 0.5) * 0.2, h.z + (r.nextDouble() - 0.5) * 0.2,
                        in.x / d * 0.1, in.y / d * 0.1, in.z / d * 0.1);
            }
        }
    }

    private static void flash(ClientLevel level, AbstractClientPlayer p) {
        RandomSource r = level.random;
        Vec3 chest = chest(p);
        Vec3 look = p.getLookAngle();
        FlightEffects.ring(level, p.getId(), chest.add(look.scale(0.4)), look, 6, 0.3F, 2.0F, 0.1F, SandPalette.GOLD, 0.8F);
        for (int i = 0; i < Math.round(14 * size(p) + 4); i++) {
            Vec3 d = new Vec3(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().scale(0.2 + r.nextDouble() * 0.15);
            level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? SandPalette.GOLD : SandPalette.GOLD_CORE, 0.9F * size(p)), chest.x, chest.y, chest.z,
                    d.x, d.y, d.z);
        }
    }

    private static void stormBurst(ClientLevel level, AbstractClientPlayer p) {
        RandomSource r = level.random;
        Vec3 feet = p.position();
        FlightEffects.ring(level, p.getId(), feet.add(0, 0.06, 0), UP, 10, 0.4F, 4.0F, 0.18F, SandPalette.HAZE, 0.75F);
        for (int i = 0; i < Math.round(18 * size(p) + 6); i++) {
            double a = r.nextDouble() * Mth.TWO_PI;
            double speed = 0.12 + r.nextDouble() * 0.16;
            level.addParticle(SAND, feet.x, feet.y + 0.1, feet.z, Math.cos(a) * speed, 0.12 + r.nextDouble() * 0.2, Math.sin(a) * speed);
        }
    }

    // ---- places ----

    /** A hand of the hero: the tracked one, or (the local player in first person) where energy leaves the hand. */
    static Vec3 hand(AbstractClientPlayer player, HumanoidArm arm) {
        Vec3 tracked = HandTracker.hand(player, arm, 0.0F);
        if (tracked != null) {
            return tracked;
        }
        return arm == player.getMainArm() ? WorldEffectsRenderer.handPosition(Minecraft.getInstance(), player, 1.0F)
                : player.position().add(0.0, player.getBbHeight() * 0.6, 0.0).add(player.getLookAngle().scale(0.6));
    }

    static Vec3 chest(AbstractClientPlayer player) {
        return player.position().add(0.0, player.getBbHeight() * 0.7, 0.0).add(player.getLookAngle().scale(0.3));
    }

    /** The look along the ground (unit), or the way he faces when he looks straight up or down. */
    private static Vec3 flat(AbstractClientPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 f = new Vec3(look.x, 0, look.z);
        return f.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, p.getYRot()) : f.normalize();
    }

    /** Smaller particles for the local player in first person (they are near the camera). */
    static float size(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson() ? 0.4F : 1.0F;
    }

    /** Whether the flourishes of this client are switched off. */
    static boolean quiet() {
        return !EnhancedClientConfig.flightEffects();
    }

    /** A grain of sand, for the other classes of the hero. */
    static ParticleOptions sand() {
        return SAND;
    }

    private ApocalypseActing() {
    }
}

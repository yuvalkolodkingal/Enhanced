package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.client.render.FalconActing;
import com.kolodkingal.enhanced.falcon.FalconAbilities;
import com.kolodkingal.enhanced.falcon.FalconWings;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * The wings' pose of each player (3.8, PLAN 8.2): which pose his state asks for (folded on the pack, spread for
 * flight, flat and wide for the glide, swept back for the Dive and the Boost Climb, wrapped round the front for the
 * Wing Shield, flung out for the Wing Slash), eased toward it tick by tick and interpolated between ticks with the
 * frame's part of a tick, so nothing pops; and the flap of a hover, worked out at the frame. Pure numbers for {@link
 * FalconWingsLayer}.
 *
 * <p>A pose is {@link #SIZE} numbers for the right wing (the left one is its mirror): the hinge's place on the pack
 * (x outward, y down, z back; pixels), its sweep (degrees, back positive), the top blade's elevation and the step to
 * each next one down (degrees), how far the blades are drawn out (0 nested, 1 full), and the tilt of the wing's plane.
 */
public final class FalconWingsState {
    public static final int HX = 0;
    public static final int HY = 1;
    public static final int HZ = 2;
    public static final int SWEEP = 3;
    public static final int TOP = 4;
    public static final int STEP = 5;
    public static final int EXTEND = 6;
    public static final int TILT = 7;
    public static final int SIZE = 8;

    /** Folded on the pack: the blades nested and stacked, pointing down. */
    public static final float[] FOLDED = {1.6F, 1.5F, 3.9F, 0.0F, -82.0F, 3.0F, 0.0F, 0.0F};
    /** Spread for flight: wide, fanned from a little up to well down. */
    public static final float[] SPREAD = {3.0F, 2.0F, 3.9F, 12.0F, 24.0F, 10.0F, 1.0F, 0.0F};
    /** The glide: flat and wide. */
    public static final float[] GLIDE = {3.0F, 2.0F, 3.9F, 4.0F, 7.0F, 4.0F, 1.0F, 0.0F};
    /** The Dive: swept back and half drawn in. */
    public static final float[] DIVE = {2.5F, 2.0F, 3.9F, 62.0F, -18.0F, 7.0F, 0.65F, 0.0F};
    /** The Boost Climb: swept back and down, the nozzles roaring. */
    public static final float[] CLIMB = {2.5F, 2.0F, 3.9F, 48.0F, -30.0F, 6.0F, 0.5F, 0.0F};
    /** The Wing Shield: the hinges at his shoulders, the wings wrapped round the front, fanned top to knee. */
    public static final float[] SHIELD = {5.5F, 1.0F, 0.5F, -118.0F, 20.0F, 14.0F, 0.6F, 0.0F};
    /** The Wing Slash: flung out wide and a little forward. */
    public static final float[] SLASH = {3.0F, 2.0F, 3.9F, -5.0F, 16.0F, 8.0F, 1.0F, 0.0F};

    /** How much of the gap to the asked pose is closed each tick. */
    static final float EASE = 0.35F;

    /** One player's wings: the pose of the last tick and of this one, the flap's strength. */
    static final class Anim {
        final float[] prev = FOLDED.clone();
        final float[] cur = FOLDED.clone();
        float flapPrev;
        float flap;
        long lastTick = Long.MIN_VALUE;
    }

    private static final Map<Integer, Anim> ANIMS = new HashMap<>();

    /** Whether the player's wings are drawn: the Exo-Wings on his chest, or a custom hero's Falcon rows. */
    public static boolean drawsWings(Player player) {
        if (FalconWings.worn(player)) {
            return true;
        }
        PowerData data = Powers.get(player);
        return data.power() == Power.CUSTOM && FalconAbilities.anyRow(data);
    }

    /** Whether the wings move for him (Falcon or a custom hero with his rows, with wings): else they stay folded. */
    static boolean alive(Player player, PowerData data) {
        return (data.power() == Power.FALCON || data.power() == Power.CUSTOM) && FalconWings.hasWings(player, data);
    }

    /** Whether the player wears the Winter Soldier's look (the darker EXO-7 wings). */
    public static boolean winterSoldier(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.FALCON && FalconAbilities.WINTER_SOLDIER.equals(Variants.current(data).id());
    }

    /** The pose his state asks for now. */
    static float[] target(Player player, PowerData data, float[] flapOut) {
        flapOut[0] = 0.0F;
        if (!alive(player, data)) {
            return FOLDED;
        }
        if (data.is(Live.FALCON_WING_SHIELD)) {
            return SHIELD;
        }
        if (FalconActing.slash(player, 0.0F) > 0.0F) {
            return SLASH;
        }
        if (data.is(Live.FALCON_DIVING)) {
            return DIVE;
        }
        if (FalconActing.climb(player, 0.0F) > 0.0F) {
            return CLIMB;
        }
        if (player.isFallFlying()) {
            flapOut[0] = 0.15F;
            return GLIDE;
        }
        if (data.flying() && !player.onGround()) {
            double speed = player.getDeltaMovement().horizontalDistance();
            flapOut[0] = (float) Mth.clamp(1.0 - speed * 1.5, 0.25, 1.0);
            return SPREAD;
        }
        return FOLDED;
    }

    /** Once per client tick for each player in view: the pose eases toward the asked one. */
    static void tick(Player player, long now) {
        Anim anim = ANIMS.computeIfAbsent(player.getId(), k -> new Anim());
        if (anim.lastTick == now) {
            return;
        }
        boolean fresh = anim.lastTick == Long.MIN_VALUE || now - anim.lastTick > 20;
        anim.lastTick = now;
        float[] flap = new float[1];
        float[] goal = target(player, Powers.get(player), flap);
        System.arraycopy(anim.cur, 0, anim.prev, 0, SIZE);
        anim.flapPrev = anim.flap;
        for (int i = 0; i < SIZE; i++) {
            anim.cur[i] = fresh ? goal[i] : anim.cur[i] + (goal[i] - anim.cur[i]) * EASE;
        }
        anim.flap = fresh ? flap[0] : anim.flap + (flap[0] - anim.flap) * EASE;
        if (fresh) {
            System.arraycopy(anim.cur, 0, anim.prev, 0, SIZE);
            anim.flapPrev = anim.flap;
        }
    }

    /** The player's pose at this frame (into {@code out}), and the flap's strength (returned). */
    public static float pose(Player player, float partialTick, float[] out) {
        Anim anim = ANIMS.get(player.getId());
        if (anim == null) {
            System.arraycopy(FOLDED, 0, out, 0, SIZE);
            return 0.0F;
        }
        for (int i = 0; i < SIZE; i++) {
            out[i] = Mth.lerp(partialTick, anim.prev[i], anim.cur[i]);
        }
        return Mth.lerp(partialTick, anim.flapPrev, anim.flap);
    }

    /** Where one of his nozzles is in the world (for the flames' particles), near enough. */
    public static Vec3 nozzle(Player player, boolean right) {
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 facing = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = new Vec3(-facing.z, 0, facing.x);
        if (player.isFallFlying()) {
            // Lying along his look: the pack on his back (up), the nozzles toward his feet (behind).
            Vec3 look = player.getLookAngle();
            return player.position().add(0, 0.45, 0).subtract(look.scale(0.15)).add(side.scale(right ? 0.13 : -0.13));
        }
        return player.position().add(0, player.getBbHeight() * 0.45, 0).subtract(facing.scale(0.26))
                .add(side.scale(right ? 0.13 : -0.13));
    }

    /** The way the nozzles' flames blow: down, or behind him while he glides. */
    public static Vec3 exhaust(Player player) {
        return player.isFallFlying() ? player.getLookAngle().scale(-1.0) : new Vec3(0, -1, 0);
    }

    /** Every client tick: each player's wings ease; the gone are forgotten. */
    static void tickAll(net.minecraft.client.multiplayer.ClientLevel level) {
        long now = level.getGameTime();
        for (Player player : level.players()) {
            if (drawsWings(player)) {
                tick(player, now);
            }
        }
        ANIMS.values().removeIf(a -> now - a.lastTick > 200 || now < a.lastTick);
    }

    static void clear() {
        ANIMS.clear();
    }

    private FalconWingsState() {
    }
}

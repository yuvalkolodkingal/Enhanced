package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.Gestures.Target;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * What one {@code setupAnim} call is working on, and the helpers every layer of the pose uses to blend into the model:
 * arms and legs by raw angle, by world elevation or aimed at something, the head, a tremble, the shoulders' twist.
 *
 * <p>Pose rows are written for a right-handed player: {@code M} is the main arm with the right arm's signs, {@code O} the
 * off arm with the left arm's. A left-handed player's rows are mirrored with one rule: y and z are multiplied by
 * {@link #side}. For Thor, {@code M} is the arm that holds his weapon. One instance is reused for every call (the render
 * thread draws one player at a time, and a call finishes before the next starts).
 */
final class PoseContext {
    private static final float RAD = Mth.DEG_TO_RAD;
    /** Ticks a bolt's or a strike's target is remembered: the length of the gestures that aim at it. */
    private static final long BOLT_MEMORY = 12;

    PlayerModel<?> model;
    Player player;
    HeroAnimState st;
    /** The age of this frame and its partial tick. */
    float age;
    float pt;
    /** The way the body is turned this frame (degrees). */
    float bodyYaw;
    /** +1 when M is a right arm, -1 when it is a left arm. */
    float side;
    /** +1 for a right-handed player, -1 for a left-handed one: the sign of the extra turn of the body. */
    float bodySide;
    ModelPart mArm;
    ModelPart oArm;
    HumanoidArm mSide;
    /** The vanilla head angles (radians) before any change: where the camera looks relative to the body. */
    float lookPitch;
    float lookYaw;
    /** The lean actually drawn (degrees) and the extra turn of the body. */
    float pitch;
    float roll;
    float yaw;
    /** An arm that holds a pose of its own (a bow, a shield, an eaten item) is left alone by every layer. */
    boolean yieldM;
    boolean yieldO;
    /** How much each arm is held by an aimed stance or a forced gesture (0..1): the swing of an attack is muted by it. */
    float occupiedM;
    float occupiedO;
    /** The twist of the shoulders asked for by the layers, degrees, and its weight. */
    float twist;
    boolean forceM;
    boolean forceO;
    /** How much the swing of the attack arm is replaced by a jab (0..1); the arm it concerns. */
    float jab;
    /** White Vision: his gestures move with one easing only. */
    boolean mechanical;
    private final float[] tmp3 = new float[3];

    void begin(PlayerModel<?> model, Player player, HeroAnimState st, float age, float pt) {
        this.model = model;
        this.player = player;
        this.st = st;
        this.age = age;
        this.pt = pt;
        this.bodyYaw = st.bodyYaw;
        HumanoidArm weaponArm = null;
        if (st.power == com.kolodkingal.enhanced.power.Power.THOR) {
            var hand = com.kolodkingal.enhanced.ability.ThorAbilities.weaponHand(player);
            if (hand != null) {
                weaponArm = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            } else if (player.tickCount - st.weaponHandTick <= 24) {
                // Just thrown or shattered: the arm that held it is still the arm that acts.
                weaponArm = st.weaponInMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            }
        }
        mSide = weaponArm != null ? weaponArm : player.getMainArm();
        mArm = mSide == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        oArm = mSide == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        side = mSide == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        bodySide = player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        lookPitch = model.head.xRot;
        lookYaw = model.head.yRot;
        pitch = st.renderPitch;
        roll = st.renderRoll;
        yaw = st.yawAdd;
        occupiedM = 0.0F;
        occupiedO = 0.0F;
        twist = 0.0F;
        forceM = false;
        forceO = false;
        jab = 0.0F;
        mechanical = st.power == com.kolodkingal.enhanced.power.Power.VISION && "white_vision".equals(st.look);
    }

    // ---- arms ----

    /** Blends the main (M) or off (O) arm toward raw angles (degrees) by weight {@code w}. */
    void arm(boolean main, float x, float y, float z, float w) {
        if (w <= 0.0F || (main ? yieldM && !forceM : yieldO && !forceO)) {
            return;
        }
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += (x * RAD - arm.xRot) * w;
        arm.yRot += (y * side * RAD - arm.yRot) * w;
        arm.zRot += (z * side * RAD - arm.zRot) * w;
    }

    /** Blends an arm toward absolute angles in radians (already mirrored). */
    void armRad(boolean main, float x, float y, float z, float w) {
        if (w <= 0.0F || (main ? yieldM && !forceM : yieldO && !forceO)) {
            return;
        }
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += (x - arm.xRot) * w;
        arm.yRot += (y - arm.yRot) * w;
        arm.zRot += (z - arm.zRot) * w;
    }

    /** Adds angles in radians (already mirrored) to an arm, times the weight. */
    void armAddRad(boolean main, float x, float y, float z, float w) {
        if (w <= 0.0F || (main ? yieldM && !forceM : yieldO && !forceO)) {
            return;
        }
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += x * w;
        arm.yRot += y * w;
        arm.zRot += z * w;
    }

    /** Adds raw angles (degrees) to an arm, times the weight. */
    void armAdd(boolean main, float x, float y, float z, float w) {
        if (w <= 0.0F || (main ? yieldM && !forceM : yieldO && !forceO)) {
            return;
        }
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += x * RAD * w;
        arm.yRot += y * side * RAD * w;
        arm.zRot += z * side * RAD * w;
    }

    /** The arm held by its world elevation: 0 hangs, 90 points level forward, 180 straight up; y and z are raw. */
    void armWorld(boolean main, float phi, float y, float z, float w) {
        arm(main, -(phi + pitch), y, z, w);
    }

    /** The raw x angle (degrees) of an arm held at world elevation {@code phi} on this frame's body. */
    float worldX(float phi) {
        return -(phi + pitch);
    }

    /** Aims an arm at a target (or along the look) plus an offset in degrees, by weight {@code w}. */
    void aim(boolean main, Target target, float dx, float dy, float dz, float w) {
        if (w <= 0.0F || (main ? yieldM && !forceM : yieldO && !forceO)) {
            return;
        }
        aimAngles(main, target, dx, dy, dz, tmp3);
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += (tmp3[0] - arm.xRot) * w;
        arm.yRot += (tmp3[1] - arm.yRot) * w;
        arm.zRot += (tmp3[2] - arm.zRot) * w;
    }

    /**
     * The absolute angles (radians) of an arm aimed at a target with an offset (degrees; y and z are mirrored for a
     * left-handed player), clamped to what the arm reaches.
     */
    void aimAngles(boolean main, Target target, float dx, float dy, float dz, float[] out) {
        Vector3f dir = direction(main, target);
        float yawDeg = yaw * bodySide;
        float[] a = AimMath.arm(dir, pitch, roll, yawDeg);
        HumanoidArm which = main ? mSide : mSide.getOpposite();
        boolean right = which == HumanoidArm.RIGHT;
        float x = a[0] + dx * RAD;
        // The offsets are written like every pose row (M for a right arm, O for a left one): mirrored by the one rule.
        float y = a[1] + dy * side * RAD;
        float z = dz * side * RAD;
        x = Mth.clamp(x, -190.0F * RAD, 60.0F * RAD);
        y = right ? Mth.clamp(y, -60.0F * RAD, 100.0F * RAD) : Mth.clamp(y, -100.0F * RAD, 60.0F * RAD);
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }

    /** The unit direction in the yaw-aligned world frame (x right, y up, z forward) from an arm's shoulder to a target. */
    Vector3f direction(boolean main, Target target) {
        Vec3 point = target == Target.LOOK ? null : targetPoint(target);
        if (point == null) {
            return AimMath.lookDirection(lookPitch, lookYaw);
        }
        Vector3f shoulder = shoulder(main);
        Vector3f to = toLocal(point);
        Vector3f d = to.sub(shoulder);
        if (d.lengthSquared() < 1.0E-6F) {
            return AimMath.lookDirection(lookPitch, lookYaw);
        }
        return d.normalize();
    }

    /** The world point a target stands for, or null when there is none (the arm then follows the look). */
    Vec3 targetPoint(Target target) {
        return switch (target) {
            case LOOK -> null;
            case HELD -> st.heldEntity == null || st.heldEntity.isRemoved() ? null : center(st.heldEntity);
            case PROJECTILE -> st.caughtCount > 0 ? st.caught[0] : null;
            case BOLT -> st.boltTarget != null && player.level().getGameTime() - st.boltTick <= BOLT_MEMORY ? st.boltTarget : null;
            case WEAPON -> st.thrownWeapon != null && !st.thrownWeapon.isRemoved() ? center(st.thrownWeapon) : st.lastWeaponPos;
            case WEAPON_LAST -> st.catchFrom != null ? st.catchFrom : st.lastWeaponPos;
            case STRIKE -> st.strikeTarget != null && player.level().getGameTime() - st.strikeTick <= BOLT_MEMORY ? st.strikeTarget : null;
            case ANCHOR -> st.anchor;
        };
    }

    private Vec3 center(Entity entity) {
        return entity.getPosition(pt).add(0.0, entity.getBbHeight() * 0.5, 0.0);
    }

    /** A world position as an offset from the player's feet in the yaw-aligned frame (x right, y up, z forward). */
    Vector3f toLocal(Vec3 world) {
        Vec3 feet = player.getPosition(pt);
        double dx = world.x - feet.x;
        double dy = world.y - feet.y;
        double dz = world.z - feet.z;
        float yawRad = bodyYaw * RAD;
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);
        return new Vector3f((float) (-dx * cos - dz * sin), (float) dy, (float) (-dx * sin + dz * cos));
    }

    /**
     * Where an arm's shoulder is, as an offset from the feet in the yaw-aligned frame: the model point through the
     * renderer's own chain (the flip, the lean about its pivot, the extra turn and the landing dip).
     */
    Vector3f shoulder(boolean main) {
        HumanoidArm which = main ? mSide : mSide.getOpposite();
        return modelPoint(which == HumanoidArm.RIGHT ? -5.0F : 5.0F, 2.0F, 0.0F);
    }

    /** A point of the model (pixels) in the yaw-aligned frame relative to the feet, as the renderer draws this frame. */
    Vector3f modelPoint(float mx, float my, float mz) {
        float scale = 0.9375F;
        float x = mx / 16.0F * scale;
        float y = (my / 16.0F - 1.501F) * scale;
        float z = mz / 16.0F * scale;
        // The flip: S(-1, -1, 1).
        Vector3f v = new Vector3f(-x, -y, z);
        // The lean about its pivot: T(pivot) ZP(-roll) XP(-pitch) T(-pivot).
        float pivotY = st.pivot;
        v.y -= pivotY;
        v.rotateX(-pitch * RAD).rotateZ(-roll * RAD);
        v.y += pivotY;
        // The extra turn, then the offsets.
        v.rotateY(-yaw * bodySide * RAD);
        v.add(st.jx, st.bob - st.drop.x - st.addDrop, st.jz);
        // Entity-local has z pointing back; the yaw-aligned frame has z forward.
        return new Vector3f(v.x, v.y, -v.z);
    }

    /** Blends the physical right or left arm toward raw angles (degrees, that arm's own signs) by weight {@code w}. */
    void physArm(boolean right, float x, float y, float z, float w) {
        boolean main = right == (mSide == HumanoidArm.RIGHT);
        if (w <= 0.0F || (main ? yieldM && !forceM : yieldO && !forceO)) {
            return;
        }
        ModelPart arm = right ? model.rightArm : model.leftArm;
        arm.xRot += (x * RAD - arm.xRot) * w;
        arm.yRot += (y * RAD - arm.yRot) * w;
        arm.zRot += (z * RAD - arm.zRot) * w;
    }

    // ---- legs ----

    void physLegX(boolean right, float x, float w) {
        legX(right, x, w);
    }


    /** Blends a leg toward raw angles (degrees; x and z): the right leg has outward z positive. */
    void leg(boolean right, float x, float z, float w) {
        if (w <= 0.0F) {
            return;
        }
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        leg.xRot += (x * RAD - leg.xRot) * w;
        leg.zRot += (z * RAD - leg.zRot) * w;
    }

    void legX(boolean right, float x, float w) {
        if (w <= 0.0F) {
            return;
        }
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        leg.xRot += (x * RAD - leg.xRot) * w;
    }

    void legAdd(boolean right, float x, float z, float w) {
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        leg.xRot += x * RAD * w;
        leg.zRot += z * RAD * w;
    }

    // ---- head, tremble, twist ----

    /** Adds degrees to the head's angles (y and z mirrored for a left-handed player), times the weight. */
    void head(float x, float y, float z, float w) {
        ModelPart head = model.head;
        head.xRot += x * RAD * w;
        head.yRot += y * side * RAD * w;
        head.zRot += z * side * RAD * w;
    }

    /** A tremble: {@code amp} degrees of {@code sin(speed * age + phase)} on the arm's x and a cosine on its z. */
    void tremble(boolean main, float amp, float speed, float w) {
        if (amp <= 0.0F || w <= 0.0F || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        float phase = main ? 0.0F : 1.3F;
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += amp * RAD * Mth.sin(speed * age + phase) * w;
        arm.zRot += amp * RAD * Mth.cos(speed * age + phase + 0.4F) * w;
    }

    /** Marks an arm as held by an aimed pose with this weight: the swing of an attack is muted by it. */
    void occupy(boolean main, float w) {
        if (main) {
            occupiedM = Math.max(occupiedM, w);
        } else {
            occupiedO = Math.max(occupiedO, w);
        }
    }

    /** Asks for the shoulders to twist by this many degrees (the weight is applied by the caller). */
    void twist(float degrees, float w) {
        twist += degrees * side * w;
    }
}

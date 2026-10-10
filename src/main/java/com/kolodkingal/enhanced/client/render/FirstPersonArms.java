package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.mixin.client.ItemInHandRendererAccessor;
import com.kolodkingal.enhanced.power.Power;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import static com.kolodkingal.enhanced.client.render.AnimMath.lerp;
import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * The hands of the local hero in first person. Vanilla draws only the empty main arm and what the hands hold; a gesture or a
 * stance adds a movement to them here (hands brought in and up for a shield, a palm thrust out for the Hex Bolt, the dagger
 * flicked forward, the weapon whirling at the edge of the screen). Nothing else changes: the body and the head are not
 * drawn, and the camera is never touched.
 *
 * <p>The movement is a translation in view space (blocks: +x right, +y up, +z away from the camera) and a rotation (degrees)
 * about the hand's own pivot, summed over every active stance and gesture times its weight, in a right arm's signs; the left
 * arm mirrors {@code dx}, {@code ry} and {@code rz}. The event is cancelled and the hand drawn again with the movement (the
 * pose stack the hands share has no push around each hand, so a transform added without cancelling would leak into the
 * other). A hand with nothing to move is left to vanilla.
 *
 * <p>The empty off hand, which vanilla never draws, is drawn when a pose really uses it (both hands raised for the shield),
 * sliding up from below the screen as vanilla's equip animation does. It stays at the edge: hands may enter the middle of
 * the screen only for the passes listed in the design, each under three ticks.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class FirstPersonArms {
    /** The pivot of a right hand's rotation (blocks, view space); the left hand's x is mirrored. */
    private static final float PIVOT_X = 0.40F;
    private static final float PIVOT_Y = -0.45F;
    private static final float PIVOT_Z = -0.72F;
    private static final float MAX_OFFSET = 0.8F;

    /** What is added to one hand, in a right hand's signs, and how strongly a pose really uses the hand. */
    public static final class Offset {
        public float dx;
        public float dy;
        public float dz;
        public float rx;
        public float ry;
        public float rz;
        /** The weight of the poses that use this hand (not the ambient motion): above 0.01 an empty off hand is drawn. */
        float used;

        void clear() {
            dx = dy = dz = rx = ry = rz = used = 0.0F;
        }

        void add(float w, float dx, float dy, float dz, float rx, float ry, float rz) {
            this.dx += w * dx;
            this.dy += w * dy;
            this.dz += w * dz;
            this.rx += w * rx;
            this.ry += w * ry;
            this.rz += w * rz;
            this.used = Math.max(this.used, w);
        }

        /** Motion that does not make an empty hand appear: the wind, the breathing. */
        void ambient(float w, float dx, float dy, float dz) {
            this.dx += w * dx;
            this.dy += w * dy;
            this.dz += w * dz;
        }

        float length() {
            return Mth.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    private static final Offset RIGHT = new Offset();
    private static final Offset LEFT = new Offset();
    private static double computedFor = Double.NaN;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !EnhancedClientConfig.animations() || player.isScoping()) {
            return;
        }
        InteractionHand hand = event.getHand();
        ItemStack stack = event.getItemStack();
        if ((player.isUsingItem() && player.getUsedItemHand() == hand) || stack.getItem() instanceof MapItem) {
            return;
        }
        float pt = event.getPartialTick();
        HeroAnimState s = HeroAnimator.frame(player, Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot), pt);
        if (s == null || s.wVanilla > 0.0F) {
            return;
        }
        if (computedFor != s.time) {
            computedFor = s.time;
            collect(s, player);
        }
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        Offset o = arm == HumanoidArm.RIGHT ? RIGHT : LEFT;
        boolean emptyOffHand = hand == InteractionHand.OFF_HAND && stack.isEmpty();
        boolean drawEmpty = emptyOffHand && o.used > 0.01F && !player.isInvisible();
        if (o.length() < 1.0E-4F && Math.abs(o.rx) + Math.abs(o.ry) + Math.abs(o.rz) < 1.0E-3F && !drawEmpty) {
            return;
        }
        if (emptyOffHand && !drawEmpty) {
            return;
        }
        float swing = event.getSwingProgress();
        if (s.power == Power.THOR && throwing(s)) {
            swing = 0.0F;
        }
        event.setCanceled(true);
        PoseStack ps = event.getPoseStack();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float length = o.length();
        float k = length > MAX_OFFSET ? MAX_OFFSET / length : 1.0F;
        ps.pushPose();
        ps.translate(o.dx * side * k, o.dy * k, -o.dz * k);
        ps.translate(PIVOT_X * side, PIVOT_Y, PIVOT_Z);
        ps.mulPose(Axis.ZP.rotationDegrees(o.rz * side));
        ps.mulPose(Axis.YP.rotationDegrees(o.ry * side));
        ps.mulPose(Axis.XP.rotationDegrees(o.rx));
        ps.translate(-PIVOT_X * side, -PIVOT_Y, -PIVOT_Z);
        ItemInHandRendererAccessor renderer = (ItemInHandRendererAccessor) mc.gameRenderer.itemInHandRenderer;
        if (drawEmpty) {
            renderer.enhanced$renderPlayerArm(ps, event.getMultiBufferSource(), event.getPackedLight(), 1.0F - Math.min(1.0F, o.used), 0.0F, arm);
        } else {
            renderer.enhanced$renderArmWithItem(player, pt, event.getInterpolatedPitch(), hand, swing, stack, event.getEquipProgress(), ps,
                    event.getMultiBufferSource(), event.getPackedLight());
        }
        ps.popPose();
        // Where energy leaves the hand follows the hand (see WorldEffectsRenderer.handPosition).
        if (hand == InteractionHand.MAIN_HAND || emptyOffHand) {
            s.fpAge = s.time;
        }
    }

    /** Whether a throw of the weapon is under way (it replaces the punch). */
    private static boolean throwing(HeroAnimState s) {
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active && slot.id.group == GestureId.Group.THOR_ARM && slot.id.force && slot.weight > 0.5F) {
                return true;
            }
        }
        return false;
    }

    /**
     * The movement of the main hand now, in the signs of a right hand, for the effects that leave it: the Chaos Beam, the
     * telekinetic tether, Billy's bolt. All zero when the hero is not animated or has not been drawn lately.
     */
    public static void mainHand(LocalPlayer player, float[] out) {
        java.util.Arrays.fill(out, 0.0F);
        HeroAnimState s = HeroAnimator.peek(player);
        if (s == null || s.wVanilla > 0.0F || Math.abs(s.time - s.fpAge) > 2.0 || !EnhancedClientConfig.animations()) {
            return;
        }
        Offset o = player.getMainArm() == HumanoidArm.RIGHT ? RIGHT : LEFT;
        float length = o.length();
        float k = length > MAX_OFFSET ? MAX_OFFSET / length : 1.0F;
        out[0] = o.dx * k;
        out[1] = o.dy * k;
        out[2] = o.dz * k;
    }

    // ====================================================================================================================
    // The rows: what each stance and gesture does to the hands
    // ====================================================================================================================

    private static void collect(HeroAnimState s, LocalPlayer player) {
        RIGHT.clear();
        LEFT.clear();
        float age = (float) s.time;
        boolean calm = EnhancedClientConfig.reducedMotion();
        // M is the arm of the weapon for Thor, else the main arm (as in the poses).
        HumanoidArm mArm = player.getMainArm();
        if (s.power == Power.THOR) {
            InteractionHand weapon = ThorAbilities.weaponHand(player);
            if (weapon == InteractionHand.OFF_HAND) {
                mArm = mArm.getOpposite();
            }
        }
        Offset m = mArm == HumanoidArm.RIGHT ? RIGHT : LEFT;
        Offset o = mArm == HumanoidArm.RIGHT ? LEFT : RIGHT;
        float w;

        // ---- stances ----
        if ((w = s.stance(Stance.BEAM_WANDA)) > 0.001F) {
            both(w, -0.26F, 0.14F, 0.22F, -20, 10, -5);
            tremble(m, o, w, 0.006F, 1.9F, age, calm);
        }
        if ((w = s.stance(Stance.SHIELD_WANDA)) > 0.001F) {
            both(w, -0.12F, 0.36F, 0.10F, -38, 6, 0);
        }
        if ((w = s.stance(Stance.TK_HOLD)) > 0.001F) {
            m.add(w, -0.20F, 0.22F, 0.30F, -42, 8, 0);
            tremble(m, null, w, s.power == Power.SCARLET_WITCH ? 0.012F : 0.006F, 1.9F, age, calm);
        }
        if ((w = s.stance(Stance.TK_STRAIN)) > 0.001F) {
            both(w, -0.10F, 0.26F, 0.34F, -48, 10, 0);
            tremble(m, o, w, 0.012F, 1.9F, age, calm);
        }
        if ((w = s.stance(Stance.MAG_SHRAPNEL)) > 0.001F) {
            float pulse = Math.max(0.0F, Mth.cos(Mth.TWO_PI * age / 3.0F));
            pulse *= pulse;
            pulse *= pulse;
            m.add(w, -0.14F, 0.12F, 0.18F - 0.05F * pulse, -18, 4, 0);
        }
        if ((w = s.stance(Stance.BILLY_FREEZE)) > 0.001F) {
            both(w, -0.10F, 0.30F, 0.28F, -52, 10, 0);
            tremble(m, o, w, 0.010F, 1.9F, age, calm);
        }
        if ((w = s.stance(Stance.THOR_BEAM)) > 0.001F) {
            m.add(w, 0.0F, 0.14F, 0.42F, -24, 6, 0);
            tremble(m, null, w, 0.008F * HeroAnimator.beamPower(s), 1.9F, age, calm);
        }
        if ((w = s.stance(Stance.THOR_WAIT)) > 0.001F) {
            m.add(w, -0.10F, 0.16F, 0.30F, -30, 0, 0);
        }
        if ((w = s.stance(Stance.THOR_BIFROST)) > 0.001F) {
            both(w, -0.14F, 0.38F, 0.16F, -45, 8, 0);
        }

        // ---- gestures ----
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active && slot.weight > 0.001F) {
                gesture(slot, m, o, age, calm);
            }
        }

        // ---- ambient motion: the weapon's whirl, the wind, the breathing of a hover ----
        if (s.wSpin > 0.02F) {
            float a = s.spinAngle * Mth.DEG_TO_RAD;
            m.ambient(s.wSpin, 0.10F * Mth.cos(a) - 0.06F, 0.10F * Mth.sin(a) + 0.06F, 0.15F);
        }
        if (s.wAir > 0.001F && !calm) {
            float push = -0.10F * ss(1.0F, 2.0F, s.s) * s.wAir;
            RIGHT.ambient(1.0F, 0.0F, push, push);
            LEFT.ambient(1.0F, 0.0F, push, push);
            if (s.power != Power.THOR) {
                float breathe = 0.012F * Mth.sin(0.07F * age) * s.wAir * (1.0F - ss(0.2F, 0.8F, s.sH));
                RIGHT.ambient(1.0F, 0.0F, breathe, 0.0F);
                LEFT.ambient(1.0F, 0.0F, breathe, 0.0F);
            }
        }
    }

    /** One gesture's rows. */
    private static void gesture(HeroAnimState.Slot slot, Offset m, Offset o, float age, boolean calm) {
        float w = slot.weight;
        float t = slot.t;
        switch (slot.id) {
            case WANDA_SHIELD_RAISE -> {
                // The hands are up already (the stance rises with the shield's flag, and its row is summed with
                // this one): the raise only adds how they gather across the chest before they are thrown open: at t4
                // they are 0.18 further in. (Its own copy of the stance's row would double it: the arms would fill the
                // screen.)
                float cross = ss(0.0F, 4.0F, t) * (1.0F - ss(8.0F, 15.0F, t));
                if (cross > 0.0F) {
                    both(w, -0.18F * cross, 0.0F, 0.0F, 0, 0, 0);
                }
            }
            case BILLY_BOLT -> {
                // The hand drawn back, then thrust at the target.
                float u = ss(0.0F, 1.0F, t);
                m.add(w, lerp(u, -0.10F, -0.16F), lerp(u, 0.30F, 0.18F), lerp(u, 0.0F, 0.38F), lerp(u, -50, -36), 5, 0);
            }
            case LOKI_DAGGER, LOKI_FAN -> {
                // A flick forward over two ticks.
                float u = ss(0.0F, 2.0F, t);
                m.add(w, lerp(u, 0.0F, -0.12F), lerp(u, 0.30F, 0.10F), lerp(u, -0.10F, 0.36F), lerp(u, -60, -20), 0, 0);
            }
            case QS_BLITZ -> {
                // The jabs alternate: the main fist, then the off fist, every 1.6 ticks.
                float jab = Mth.sin(Mth.PI * t / 1.6F);
                m.add(w, -0.10F, 0.04F, 0.40F * Math.max(0.0F, jab), -15, 0, 0);
                o.add(w, -0.10F, 0.04F, 0.40F * Math.max(0.0F, -jab), -15, 0, 0);
            }
            case TOMMY_ACCEL -> {
                m.add(w, -0.16F, 0.16F, 0.34F, -30, 6, 0);
                tremble(m, null, w, 0.015F, 2.2F, age, calm);
            }
            case THOR_THROW -> {
                // Over two ticks the hand swings back and forward: the throw replaces the punch.
                float u = ss(0.0F, 2.0F, t);
                m.add(w, lerp(u, 0.0F, -0.08F), lerp(u, 0.34F, 0.05F), lerp(u, -0.12F, 0.50F), lerp(u, -75, -10), 0, 0);
            }
            default -> {
            }
        }
    }

    private static void both(float w, float dx, float dy, float dz, float rx, float ry, float rz) {
        RIGHT.add(w, dx, dy, dz, rx, ry, rz);
        LEFT.add(w, dx, dy, dz, rx, ry, rz);
    }

    /** A tremble of at most 0.015 blocks, none with reduced motion. */
    private static void tremble(Offset a, Offset b, float w, float amplitude, float speed, float age, boolean calm) {
        if (calm) {
            return;
        }
        float amp = Math.min(0.015F, amplitude) * w;
        a.ambient(1.0F, amp * Mth.sin(speed * age), amp * Mth.cos(speed * age + 0.7F), 0.0F);
        if (b != null) {
            b.ambient(1.0F, amp * Mth.sin(speed * age + 1.3F), amp * Mth.cos(speed * age + 2.0F), 0.0F);
        }
    }

    private FirstPersonArms() {
    }
}

package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Moves a hero's whole body on the render pose stack (so armour, held items, capes and every overlay layer follow, and the
 * name tag stays where it is): the lean and bank of a flight or a run, the hover bob, the dip of a landing, the turn of a
 * whirlwind. The motion itself is worked out by {@link HeroAnimator}; this class is the small, stable door the renderers
 * and effects use:
 * <ul>
 *     <li>{@link #apply}, called from {@link com.kolodkingal.enhanced.mixin.client.PlayerRendererMixin}, where vanilla
 *     applies the elytra and swimming pitch;</li>
 *     <li>{@link #transform}, for the speedster ghosts that are drawn with the body's own motion;</li>
 *     <li>{@link #follow}, which moves a point of the upright body (where energy leaves a hand or a head) to where the body
 *     has put it;</li>
 *     <li>{@link #pitch} and {@link #roll}, which the cape and the aimed poses read.</li>
 * </ul>
 */
public final class BodyLean {

    /**
     * Updates this player's animation for the frame being drawn and applies the body's motion to the pose stack, which must
     * be in the entity's frame: already turned to its body yaw, before the model is flipped upright.
     */
    public static void apply(AbstractClientPlayer player, PoseStack poseStack, float yBodyRot, float partialTick) {
        HeroAnimState state = HeroAnimator.frame(player, yBodyRot, partialTick);
        if (state != null && state.wVanilla <= 0.0F) {
            HeroAnimator.applyBody(poseStack, state, player);
        }
    }

    /** Moves the pose stack (in the entity's frame, like {@link #apply}) by the motion last applied to this player. */
    public static void transform(PoseStack poseStack, Player player) {
        HeroAnimState state = HeroAnimator.peek(player);
        if (state != null && state.wVanilla <= 0.0F) {
            HeroAnimator.applyBody(poseStack, state, player);
        }
    }

    /**
     * Moves a point of the upright body (a world position, like the eye's) to where the body's motion has taken it, so that
     * energy leaves the head or the hand that is drawn. A body that is not drawn moving, or not drawn lately (the local
     * player in first person), leaves the point be.
     */
    public static Vec3 follow(Player player, Vec3 point, float partialTick) {
        return HeroAnimator.follow(player, point, partialTick);
    }

    /** The forward pitch (radians) this player's body is drawn with, or 0. */
    public static float pitch(Player player) {
        HeroAnimState state = HeroAnimator.peek(player);
        return state != null && state.wVanilla <= 0.0F ? state.renderPitch * Mth.DEG_TO_RAD : 0.0F;
    }

    /** The bank (degrees, right side down positive) this player's body is drawn with, or 0. */
    public static float roll(Player player) {
        HeroAnimState state = HeroAnimator.peek(player);
        return state != null && state.wVanilla <= 0.0F ? state.renderRoll : 0.0F;
    }

    /** Forgets players who left or are no longer in this world. */
    public static void tick(ClientLevel level) {
        HeroAnimator.tick(level);
    }

    public static void clear() {
        HeroAnimator.clear();
    }

    private BodyLean() {
    }
}

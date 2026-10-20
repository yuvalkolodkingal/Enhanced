package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.ironman.SuitParts;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * Lane L3's generic suit-up ({@link SuitAssembly#GENERIC}, PLAN 5.8: "the plates slide in from around him"): the parts
 * come in five groups, one after another (the boots and shins, the thighs, the chest and back, the arms, the helmet and
 * its faceplate last), each plate flying in from a little way off (the legs from below and the sides, the body from the
 * front and back, the arms from the sides, the helmet from above), turning into place, growing to its size and locking
 * with a small overshoot. A take-off plays it backwards: the helmet leaves first. Every value is a pure function of the
 * phase's progress, which the layer interpolates with the partial tick.
 */
public final class GenericAssembly {
    /** The groups' order (0 first). */
    static int group(SuitParts part) {
        return switch (part) {
            case RIGHT_BOOT, LEFT_BOOT, RIGHT_SHIN, LEFT_SHIN -> 0;
            case RIGHT_THIGH, LEFT_THIGH -> 1;
            case CHEST, BACK -> 2;
            case RIGHT_UPPER_ARM, LEFT_UPPER_ARM, RIGHT_FOREARM, LEFT_FOREARM, RIGHT_GAUNTLET, LEFT_GAUNTLET -> 3;
            case HELMET, FACEPLATE -> 4;
        };
    }

    static final int GROUPS = 5;
    /** How much of the phase one group's flight takes (they overlap a little). */
    static final float SPAN = 0.32F;

    /**
     * How far a part of group {@code g} is in place at the phase's {@code progress} (0 away, 1 locked), with its own
     * small overshoot past 1 just before it locks.
     */
    public static float placed(int g, float progress) {
        float start = g * (1.0F - SPAN) / (GROUPS - 1);
        float t = Mth.clamp((progress - start) / SPAN, 0.0F, 1.0F);
        // (An ease-out back: quick in, a little past the mark, settles.)
        float c = 1.4F;
        float u = t - 1.0F;
        return 1.0F + (c + 1.0F) * u * u * u + c * u * u;
    }

    static void pose(SuitAssembly.Poses parts, float progress, boolean up, AbstractClientPlayer player,
                     float partialTick) {
        for (SuitParts part : SuitParts.VALUES) {
            SuitAssembly.Pose pose = parts.get(part);
            int g = group(part);
            // (A take-off is the suit-up backwards: the last group to lock is the first to leave.)
            float k = up ? placed(g, progress) : placed(g, 1.0F - progress);
            float away = 1.0F - Mth.clamp(k, 0.0F, 1.2F);
            float in = Mth.clamp(k, 0.0F, 1.0F);
            pose.shown = in <= 0.001F ? 0.0F : Math.min(1.0F, in * 1.6F);
            pose.scale = 0.55F + 0.45F * Math.min(1.0F, Math.max(0.0F, k));
            float dist = 10.0F * away;
            float side = part.right() ? -1.0F : part.left() ? 1.0F : 0.0F;
            switch (g) {
                case 0, 1 -> {
                    pose.x = side * dist * 0.6F;
                    pose.y = dist * 0.8F;
                    pose.zRot = side * 0.6F * away;
                }
                case 2 -> {
                    pose.z = part == SuitParts.BACK ? dist : -dist;
                    pose.xRot = (part == SuitParts.BACK ? -0.5F : 0.5F) * away;
                }
                case 3 -> {
                    pose.x = side * dist;
                    pose.zRot = side * 0.9F * away;
                }
                default -> {
                    pose.y = -dist;
                    pose.xRot = -0.8F * away;
                }
            }
        }
    }

    private GenericAssembly() {
    }
}

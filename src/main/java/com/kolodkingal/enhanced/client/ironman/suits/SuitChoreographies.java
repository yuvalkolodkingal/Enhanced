package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.client.ironman.SuitAssembly;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * The six suit-ups (3.8, PLAN 7.2): how each piece moves inside its {@link SuitTimelines} window. Offsets are pixels in
 * the space of the skeleton's part the piece rides on (y down, z forward is negative), turns in radians. A take-off is
 * the timeline backwards; where the film's take-off differs (the Mark 1's plates fall away) the DOWN branch says so.
 */
final class SuitChoreographies {
    /** Where the faceplate hinges on the head (its top edge, over the forehead) and how far it opens. */
    private static final float VISOR_Y = -7.5F;
    private static final float VISOR_Z = -1.0F;
    private static final float VISOR_OPEN = -2.3F;

    static void register() {
        SuitAssembly.Registry.register(SuitKind.MARK_1, SuitChoreographies::mark1);
        SuitAssembly.Registry.register(SuitKind.MARK_3, SuitChoreographies::mark3);
        SuitAssembly.Registry.register(SuitKind.MARK_5, SuitChoreographies::mark5);
        SuitAssembly.Registry.register(SuitKind.MARK_50, SuitChoreographies::nanotech);
        SuitAssembly.Registry.register(SuitKind.MARK_85, SuitChoreographies::nanotech);
        SuitAssembly.Registry.register(SuitKind.HULKBUSTER, SuitChoreographies::hulkbuster);
    }

    /**
     * Mark 1: the crude plates clank on one by one, each swinging in from its side with a jolt (it overshoots and
     * settles); the helmet drops on last and the slit faceplate swings down. Off: the helmet first, the rest fall away.
     */
    private static void mark1(SuitAssembly.Poses parts, float progress, boolean up, AbstractClientPlayer player,
                              float partialTick) {
        for (SuitParts part : SuitParts.VALUES) {
            SuitAssembly.Pose pose = parts.get(part);
            float r = SuitTimelines.reveal(parts.kind, part, progress, up);
            pose.shown = r > 0.0F ? 1.0F : 0.0F;
            if (r >= 1.0F || r <= 0.0F) {
                continue;
            }
            if (!up) {
                float k = 1.0F - r;
                pose.y = k * k * 14.0F;
                pose.xRot = k * 0.7F * (part.left() ? -1.0F : 1.0F);
                pose.zRot = k * 0.5F * side(part);
                continue;
            }
            float away = 1.0F - backOut(r);
            switch (part) {
                case HELMET -> pose.y = -12.0F * away;
                case FACEPLATE -> visor(pose, away);
                case CHEST -> pose.z = -10.0F * away;
                case BACK -> pose.z = 10.0F * away;
                default -> {
                    pose.x = 10.0F * away * side(part);
                    pose.zRot = -0.9F * away * side(part);
                }
            }
        }
    }

    /**
     * Mark 3: at the workshop's gantry the robot arms lower each piece from above and bolt it on with a twist (the
     * display's renderer draws the arms); anywhere else the plates slide in from a ring round him and lock. The
     * faceplate swings shut last.
     */
    private static void mark3(SuitAssembly.Poses parts, float progress, boolean up, AbstractClientPlayer player,
                              float partialTick) {
        boolean gantry = player != null && SuitFx.gantryOf(player) != null;
        for (SuitParts part : SuitParts.VALUES) {
            SuitAssembly.Pose pose = parts.get(part);
            float r = SuitTimelines.reveal(parts.kind, part, progress, up);
            pose.shown = r > 0.0F ? 1.0F : 0.0F;
            if (r >= 1.0F || r <= 0.0F) {
                continue;
            }
            float away = 1.0F - cubicOut(r);
            if (part == SuitParts.FACEPLATE) {
                visor(pose, away);
                continue;
            }
            if (gantry) {
                pose.y = -18.0F * away;
                pose.yRot = 0.8F * away * (part.left() ? -1.0F : 1.0F);
                continue;
            }
            switch (part) {
                case HELMET -> {
                    pose.z = 10.0F * away;
                    pose.y = -4.0F * away;
                }
                case CHEST -> pose.z = -12.0F * away;
                case BACK -> pose.z = 12.0F * away;
                default -> {
                    pose.x = 12.0F * away * side(part);
                    pose.z = (part.name().contains("THIGH") || part.name().contains("UPPER") ? -4.0F : 4.0F) * away;
                }
            }
        }
    }

    /**
     * Mark 5: the panels climb out of the suitcase at his feet, up the legs (unfolding as they rise), wrap the body
     * from the sides, run up the arms from the hands, the helmet folds over from behind and the faceplate slams shut.
     * Off: it all folds back down into the case.
     */
    private static void mark5(SuitAssembly.Poses parts, float progress, boolean up, AbstractClientPlayer player,
                              float partialTick) {
        for (SuitParts part : SuitParts.VALUES) {
            SuitAssembly.Pose pose = parts.get(part);
            float r = SuitTimelines.reveal(parts.kind, part, progress, up);
            pose.shown = r > 0.0F ? 1.0F : 0.0F;
            if (r >= 1.0F || r <= 0.0F) {
                continue;
            }
            float away = 1.0F - cubicOut(r);
            switch (part) {
                case RIGHT_BOOT, LEFT_BOOT, RIGHT_SHIN, LEFT_SHIN, RIGHT_THIGH, LEFT_THIGH -> {
                    float top = part.name().endsWith("BOOT") ? 10.0F : part.name().endsWith("SHIN") ? 6.0F : 0.0F;
                    pose.y = (13.0F - top) * away;
                    pose.z = -3.0F * away;
                    pose.scale = 0.35F + 0.65F * (1.0F - away);
                    pose.xRot = -1.2F * away;
                }
                case CHEST -> {
                    pose.z = -5.0F * away;
                    pose.yRot = 1.3F * away;
                    pose.scale = 0.6F + 0.4F * (1.0F - away);
                }
                case BACK -> {
                    pose.z = 5.0F * away;
                    pose.yRot = -1.3F * away;
                }
                case HELMET -> {
                    // folds over from behind the neck
                    float a = 1.9F * away;
                    hinge(pose, 0.0F, 0.0F, 4.0F, a);
                }
                case FACEPLATE -> visor(pose, (float) Math.pow(away, 0.5F));
                default -> {
                    // the arm pieces run up from the hand
                    float top = part.name().endsWith("GAUNTLET") ? 8.0F : part.name().endsWith("FOREARM") ? 4.0F
                            : -2.0F;
                    pose.y = (11.0F - top) * away;
                    pose.scale = 0.4F + 0.6F * (1.0F - away);
                }
            }
        }
    }

    /**
     * Mark 50 and Mark 85: no piece moves; the nanites' wave reveals each part segment by segment (the model draws
     * them, growing each from a thin film). A part shows from its first segment on.
     */
    private static void nanotech(SuitAssembly.Poses parts, float progress, boolean up, AbstractClientPlayer player,
                                 float partialTick) {
        for (SuitParts part : SuitParts.VALUES) {
            parts.get(part).shown = SuitTimelines.reveal(parts.kind, part, progress, up) > 0.0F
                    || SuitTimelines.segment(parts.kind, part, 0, 2, progress, up) > 0.0F ? 1.0F : 0.0F;
        }
    }

    /** The Hulkbuster: the shells close round him, legs first; the faceplate drops last. */
    private static void hulkbuster(SuitAssembly.Poses parts, float progress, boolean up, AbstractClientPlayer player,
                                   float partialTick) {
        for (SuitParts part : SuitParts.VALUES) {
            SuitAssembly.Pose pose = parts.get(part);
            float r = SuitTimelines.reveal(parts.kind, part, progress, up);
            pose.shown = r > 0.0F ? 1.0F : 0.0F;
            if (r >= 1.0F || r <= 0.0F) {
                continue;
            }
            float away = 1.0F - cubicOut(r);
            if (part == SuitParts.FACEPLATE) {
                hinge(pose, 0.0F, -5.0F, -1.0F, -1.3F * away);
            } else {
                pose.x = 6.0F * away * side(part);
                pose.z = part == SuitParts.BACK ? 6.0F * away : -4.0F * away;
            }
        }
    }

    /** The faceplate swung open by {@code away} (1 fully up over the forehead, 0 shut). */
    private static void visor(SuitAssembly.Pose pose, float away) {
        hinge(pose, 0.0F, VISOR_Y, VISOR_Z, VISOR_OPEN * away);
    }

    /** A turn of {@code angle} about the x axis through the point (hx, hy, hz) of the part's space. */
    static void hinge(SuitAssembly.Pose pose, float hx, float hy, float hz, float angle) {
        float c = Mth.cos(angle);
        float s = Mth.sin(angle);
        pose.xRot = angle;
        pose.x = 0.0F;
        pose.y = hy - (hy * c - hz * s);
        pose.z = hz - (hy * s + hz * c);
    }

    /** -1 for the right side (x negative), 1 for the left, 0 for the middle. */
    private static float side(SuitParts part) {
        return part.right() ? -1.0F : part.left() ? 1.0F : 0.0F;
    }

    static float cubicOut(float t) {
        float k = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - k * k * k;
    }

    /** Eases out past the end and back: the plate's jolt as it slams home. */
    static float backOut(float t) {
        float k = Mth.clamp(t, 0.0F, 1.0F) - 1.0F;
        float s = 2.2F;
        return 1.0F + k * k * ((s + 1.0F) * k + s);
    }

    private SuitChoreographies() {
    }
}

package com.kolodkingal.enhanced.client.render;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * The camera's own beams in the third-person back view (3.6). The camera then sits behind his eyes on the line he looks
 * along, so what he looks at is in the middle of the screen, behind his own head: a beam from his hand to it runs along
 * the line of sight, a few degrees from the middle at most, and the depth test hid all of it behind his head, shoulder
 * and raised arm (no beam was seen at all, while the front view and first person, with no body in the way, drew it).
 * Those beams are drawn in a pass of their own over everything ({@link EnhancedRenderTypes#ENERGY_BEAM_OVER}); a beam
 * always stops at the first solid block, so nothing but his body and what the beam passes through lies in front of it.
 */
public final class OwnBeams {
    /** Whether {@code shooter}'s beams are drawn in the pass over everything (he is the camera, seen from behind). */
    public static boolean behind(Minecraft mc, Entity shooter) {
        return shooter == mc.getCameraEntity() && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK;
    }

    private OwnBeams() {
    }
}

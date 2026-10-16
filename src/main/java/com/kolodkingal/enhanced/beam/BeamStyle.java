package com.kolodkingal.enhanced.beam;

/**
 * What a continuous beam looks like and who fires it (3.2): the palette from the white-hot core out to the fringe, the
 * name the HUD and the lang files know it by, and the hand it leaves. The server names a style on the wire by its
 * {@link #id}; both sides find it again in {@link BeamStyles}. Billy's Hex Beam is {@link BeamStyles#BILLY}; another
 * source of a beam (the Darkhold's purple one) makes a style of its own with {@link BeamStyles#register} and fires it with
 * {@link ContinuousBeam}, and nothing of the renderer, the payload or the server logic is copied.
 *
 * @param id      the short name used on the wire and in the registry ({@code billy_hex})
 * @param nameKey the lang key of its name ({@code beam.enhanced.billy_hex})
 * @param core    the white-hot line at the middle (0xRRGGBB)
 * @param inner   the pale colour round the core
 * @param main    the body of the light
 * @param body    the deeper colour round it
 * @param fringe  the dark halo at the outside
 * @param offHand whether it leaves the off hand (and the pose aims that arm) instead of the main hand
 */
public record BeamStyle(String id, String nameKey, int core, int inner, int main, int body, int fringe, boolean offHand) {
}

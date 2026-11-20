package com.kolodkingal.enhanced.client.particle;

import com.kolodkingal.enhanced.ability.Demiurge;
import com.kolodkingal.enhanced.custom.AbilityColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;

/**
 * (3.18.1) The small stars in Billy the Demiurge's energy. Every effect of his magic (and of the magic type he took on) is
 * drawn in the Demiurge's blue ({@code AbilityColors}: {@link Demiurge#BLUE}, or a palette recoloured to its hue), so a mote of
 * energy in that hue, wherever it was sent from, now and then lets out a star: a twinkling glint of white light.
 */
public final class DemiurgeStars {
    private static final float HUE = AbilityColors.hsl(Demiurge.BLUE)[0];

    private DemiurgeStars() {
    }

    /** Whether a colour is the Demiurge's blue, or one of a palette recoloured to it. */
    public static boolean tinted(int rgb) {
        float[] hsl = AbilityColors.hsl(rgb & 0xFFFFFF);
        return Math.abs(hsl[0] - HUE) < 0.004F && hsl[1] > 0.6F && hsl[2] > 0.25F && hsl[2] < 0.97F;
    }

    /** A mote of energy of colour {@code rgb} was just made here: one in five of the Demiurge's lets out a star. */
    public static void spark(ClientLevel level, int rgb, double x, double y, double z, double xd, double yd, double zd) {
        if (level.random.nextInt(5) != 0 || !tinted(rgb)) {
            return;
        }
        level.addParticle(ParticleTypes.END_ROD, x, y, z, xd * 0.25 + (level.random.nextDouble() - 0.5) * 0.02,
                yd * 0.25 + (level.random.nextDouble() - 0.5) * 0.02, zd * 0.25 + (level.random.nextDouble() - 0.5) * 0.02);
    }
}

package com.kolodkingal.enhanced.vishanti;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.config.VishantiConfig;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * How far a sorcerer's ward reaches: the stronger the sorcerer, the larger the ward. The Scarlet Witch, the strongest,
 * has the biggest ({@code wardRadiusWanda}); Billy's grows from {@code wardRadiusBillyMin} as an Apprentice to {@code
 * wardRadiusBillyMax} at his highest Build Magic rank; Loki has one fixed size ({@code wardRadiusLoki}), and so has Agatha
 * ({@code wardRadiusAgatha}, 3.3). Nobody else has
 * a ward at all (0). Read live, on both sides (the server config is synced).
 */
public final class WardStrength {
    /**
     * Whether the Book of the Vishanti can be read by someone with this power: Loki, the Scarlet Witch, Billy and (3.3)
     * Agatha Harkness. The one list of the sorcerers: the Darkhold, the Hex Runes and the Witches' Road ask it too.
     */
    public static boolean isSorcerer(Power power) {
        return Traits.of(power).contains(Trait.SORCERER);
    }

    /** Whether this player is a sorcerer. */
    public static boolean isSorcerer(Player player) {
        // (3.4: a custom hero with a sorcerer's ability is one too.)
        return Traits.has(player, Trait.SORCERER);
    }

    /**
     * How far Billy's Build Magic has come, 0 (an Apprentice) to 1 (his highest rank, {@code build_magic_billy.maxRank}); 1 if
     * there is only one rank. The one place the ward reads his rank.
     */
    public static double billyFraction(Player billy) {
        int max = BillyRanks.maxRank();
        if (max <= BillyRanks.MIN_RANK) {
            return 1.0;
        }
        int rank = BillyRanks.rank(billy.getData(ModAttachments.BUILD_DATA));
        return Mth.clamp((rank - BillyRanks.MIN_RANK) / (double) (max - BillyRanks.MIN_RANK), 0.0, 1.0);
    }

    /** The radius of a ward of this power, {@code billyFraction} being how far Billy has come (see {@link #billyFraction}). */
    public static int radius(Power power, double billyFraction) {
        if (!loaded() || !VishantiConfig.ENABLED.get() || !VishantiConfig.WARDS_ENABLED.get()) {
            return 0;
        }
        double radius = switch (power) {
            case SCARLET_WITCH -> VishantiConfig.WARD_RADIUS_WANDA.get();
            case BILLY -> {
                int low = VishantiConfig.WARD_RADIUS_BILLY_MIN.get();
                int high = VishantiConfig.WARD_RADIUS_BILLY_MAX.get();
                yield low + (high - low) * Mth.clamp(billyFraction, 0.0, 1.0);
            }
            case LOKI -> VishantiConfig.WARD_RADIUS_LOKI.get();
            case AGATHA -> VishantiConfig.WARD_RADIUS_AGATHA.get();
            default -> 0;
        };
        return radius <= 0 ? 0 : Mth.clamp((int) Math.round(radius), 1, VishantiConfig.MAX_RADIUS);
    }

    /** The radius of this player's ward now. */
    public static int radiusOf(Player player) {
        Power power = Powers.power(player);
        return radius(power, power == Power.BILLY ? billyFraction(player) : 0.0);
    }

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    private WardStrength() {
    }
}

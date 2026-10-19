package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.StoneSitesConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;
import java.util.Optional;

/**
 * The server options that stop a kind of structure from generating in new chunks (3.6, {@code [stone_sites]}): a
 * structure names its option in the optional field {@code enabled_by} (the option's name: {@code sanctums}, {@code
 * avengersTowers}, {@code endTunnels}, {@code vormirCliffs}; 3.7: {@code agatha_cottage.enabled}, {@code
 * [agatha_cottage] enabled}); with the option off it finds no place to stand, so it is neither built nor located.
 * Existing sites stay. Before the server config is loaded every option is on.
 */
public final class SiteSwitch {
    /** The field's codec: an option's name, checked when the structure is read. */
    public static final Codec<String> OPTION = Codec.STRING.validate(name -> known(name)
            ? DataResult.success(name) : DataResult.error(() -> "no site option " + name));

    /**
     * How a site's ground level is taken from the nine heights of its footprint ({@code ground}): {@code median}
     * (3.3's) or {@code highest} (3.6's Sanctum and tower: nothing round the building stands higher than its door; the
     * footing fills in where the ground falls away).
     */
    public static final Codec<String> GROUND = Codec.STRING.validate(name -> name.equals("median")
            || name.equals("highest") ? DataResult.success(name) : DataResult.error(() -> "no ground rule " + name));

    /** The ground level of a footprint by the rule {@code ground}, from its nine heights in order. */
    public static int ground(String rule, int[] sorted) {
        return rule.equals("highest") ? sorted[sorted.length - 1] : sorted[sorted.length / 2];
    }

    /** Whether a structure switched by {@code option} may generate now (empty: not switched, always). */
    public static boolean enabled(Optional<String> option) {
        if (option.isEmpty() || !EnhancedConfig.SPEC.isLoaded()) {
            return true;
        }
        ModConfigSpec.BooleanValue value = option(option.get());
        return value == null || value.get();
    }

    private static boolean known(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "sanctums", "avengerstowers", "endtunnels", "vormircliffs", "agatha_cottage.enabled" -> true;
            default -> false;
        };
    }

    private static ModConfigSpec.BooleanValue option(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "sanctums" -> StoneSitesConfig.SANCTUMS;
            case "avengerstowers" -> StoneSitesConfig.AVENGERS_TOWERS;
            case "endtunnels" -> StoneSitesConfig.END_TUNNELS;
            case "vormircliffs" -> StoneSitesConfig.VORMIR_CLIFFS;
            // (3.7: Agatha's Cottage.)
            case "agatha_cottage.enabled" -> com.kolodkingal.enhanced.config.CottageConfig.ENABLED;
            default -> null;
        };
    }

    private SiteSwitch() {
    }
}

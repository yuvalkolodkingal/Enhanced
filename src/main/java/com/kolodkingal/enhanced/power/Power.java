package com.kolodkingal.enhanced.power;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * The hero a player has become. A player has exactly one power at a time.
 */
public enum Power implements StringRepresentable {
    NONE(0xFFFFFF, 0xFFFFFF),
    // Energy colours follow the films and shows: Magneto's magnetism has no glow in the X-Men films (iron-grey metal
    // dust), the Scarlet Witch's chaos magic is crimson with hot pink highlights, Fox Quicksilver leaves only a grey
    // blur, and the Mind Stone's beam is pale gold.
    MAGNETO(0x8A9096, 0xD2D6DA),
    SCARLET_WITCH(0xE0193F, 0xFF6F86),
    QUICKSILVER(0x9AA3AD, 0xC8D0D8),
    VISION(0xF5D77A, 0xFFF6D5),
    BILLY(0x5CC8FF, 0xCFEFFF),
    TOMMY(0x2E9C7E, 0x7FD8B8),
    LOKI(0x76B154, 0xCAF297),
    THOR(0x4DB8FF, 0xBFE6FF),
    // The heroes of 3.0, in Eilon's order. They are appended after Thor and in this order: the stream codec sends the
    // ordinal, so every hero of one version must be in the same build (saves use the name, so the order never matters
    // on disk). Phoenix fire is gold-orange, Spider-Man's suit red with silver webs, the Infinity Stones purple, the
    // Sentry-bright amber of Ultron's body and the ochre and gold of Apocalypse.
    PHOENIX(0xFF8A1F, 0xFFD65A),
    SPIDER_MAN(0xDC2B2F, 0xDCE6F2),
    THANOS(0x9B4DEB, 0xE6D2FF),
    ULTRON(0xFFA23A, 0xFFE9B8),
    APOCALYPSE(0xF2B84B, 0xFFF0B8);

    /**
     * By name. A name this version does not know (a hero it no longer has) reads as {@link #NONE} instead of
     * failing, so a world saved with that hero still loads.
     */
    public static final Codec<Power> CODEC = Codec.STRING.xmap(name -> {
        Power power = byName(name);
        return power == null ? NONE : power;
    }, Power::getSerializedName);

    /** Main colour (UI text, particles). */
    public final int color;
    /** Secondary glow colour. */
    public final int glow;

    Power(int color, int glow) {
        this.color = color;
        this.glow = glow;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Colour of the hero's name (titles, HUD): the energy colour, except for three whose energy would not read as the
     * hero: Magneto's power has no colour, Ultron is named in the red of his eyes and Apocalypse in the blue of his.
     */
    public int titleColor() {
        return switch (this) {
            case MAGNETO -> 0xC8323C;
            case ULTRON -> 0xD8262B;
            case APOCALYPSE -> 0x4A9BC4;
            default -> color;
        };
    }

    public boolean hasPowers() {
        return this != NONE;
    }

    public Component displayName() {
        return Component.translatable("power.enhanced." + getSerializedName());
    }

    /** Translation key for the given ability (1-5). */
    public String abilityKey(int ability) {
        return "ability.enhanced." + getSerializedName() + "." + ability;
    }

    /**
     * Quicksilver, Tommy, Spider-Man and Thanos run, swing or step through portals, and Loki walks; everyone else can
     * fly (Thor only with Mjolnir, see ThorAbilities).
     */
    public boolean canFly() {
        return switch (this) {
            case MAGNETO, SCARLET_WITCH, VISION, BILLY, THOR, PHOENIX, ULTRON, APOCALYPSE -> true;
            case NONE, QUICKSILVER, TOMMY, LOKI, SPIDER_MAN, THANOS -> false;
        };
    }

    public float flightSpeed() {
        return switch (this) {
            case MAGNETO -> EnhancedConfig.MAGNETO_FLIGHT_SPEED.get().floatValue();
            case SCARLET_WITCH -> EnhancedConfig.WANDA_FLIGHT_SPEED.get().floatValue();
            case VISION -> EnhancedConfig.VISION_FLIGHT_SPEED.get().floatValue();
            case BILLY -> EnhancedConfig.BILLY_FLIGHT_SPEED.get().floatValue();
            case THOR -> EnhancedConfig.THOR_FLIGHT_SPEED.get().floatValue();
            case PHOENIX -> PhoenixConfig.FLIGHT_SPEED.get().floatValue();
            case ULTRON -> UltronConfig.FLIGHT_SPEED.get().floatValue();
            case APOCALYPSE -> ApocalypseConfig.FLIGHT_SPEED.get().floatValue();
            case NONE, QUICKSILVER, TOMMY, LOKI, SPIDER_MAN, THANOS -> 0.05F;
        };
    }

    /** The ability (1-5) that toggles flight for this hero, matching the original concepts' numbering. */
    public int flightAbility() {
        return switch (this) {
            case MAGNETO -> 5;
            case SCARLET_WITCH, VISION, BILLY, THOR, PHOENIX, ULTRON, APOCALYPSE -> 2;
            case NONE, QUICKSILVER, TOMMY, LOKI, SPIDER_MAN, THANOS -> -1;
        };
    }

    public static Power byName(String name) {
        for (Power p : values()) {
            if (p.getSerializedName().equals(name)) {
                return p;
            }
        }
        return null;
    }
}

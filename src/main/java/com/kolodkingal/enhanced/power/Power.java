package com.kolodkingal.enhanced.power;

import com.kolodkingal.enhanced.EnhancedConfig;
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
    THOR(0x4DB8FF, 0xBFE6FF);

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

    /** Colour of the hero's name (titles, HUD): the energy colour, except Magneto's, whose power has no colour. */
    public int titleColor() {
        return this == MAGNETO ? 0xC8323C : color;
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

    /** Quicksilver and Tommy run and Loki walks; everyone else can fly (Thor only with Mjolnir, see ThorAbilities). */
    public boolean canFly() {
        return switch (this) {
            case MAGNETO, SCARLET_WITCH, VISION, BILLY, THOR -> true;
            case NONE, QUICKSILVER, TOMMY, LOKI -> false;
        };
    }

    public float flightSpeed() {
        return switch (this) {
            case MAGNETO -> EnhancedConfig.MAGNETO_FLIGHT_SPEED.get().floatValue();
            case SCARLET_WITCH -> EnhancedConfig.WANDA_FLIGHT_SPEED.get().floatValue();
            case VISION -> EnhancedConfig.VISION_FLIGHT_SPEED.get().floatValue();
            case BILLY -> EnhancedConfig.BILLY_FLIGHT_SPEED.get().floatValue();
            case THOR -> EnhancedConfig.THOR_FLIGHT_SPEED.get().floatValue();
            case NONE, QUICKSILVER, TOMMY, LOKI -> 0.05F;
        };
    }

    /** The ability (1-5) that toggles flight for this hero, matching the original concepts' numbering. */
    public int flightAbility() {
        return switch (this) {
            case MAGNETO -> 5;
            case SCARLET_WITCH, VISION, BILLY, THOR -> 2;
            case NONE, QUICKSILVER, TOMMY, LOKI -> -1;
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

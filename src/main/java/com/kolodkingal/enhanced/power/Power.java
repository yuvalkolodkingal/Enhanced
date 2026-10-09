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
    MAGNETO(0xB01CFF, 0xC27BFF),
    SCARLET_WITCH(0xE0102A, 0xFF4058),
    PHOTON(0xFFE9A8, 0x9FF3FF);

    public static final Codec<Power> CODEC = StringRepresentable.fromEnum(Power::values);

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

    public boolean canFly() {
        return this != NONE;
    }

    public float flightSpeed() {
        return switch (this) {
            case MAGNETO -> EnhancedConfig.MAGNETO_FLIGHT_SPEED.get().floatValue();
            case SCARLET_WITCH -> EnhancedConfig.WANDA_FLIGHT_SPEED.get().floatValue();
            case PHOTON -> EnhancedConfig.PHOTON_FLIGHT_SPEED.get().floatValue();
            case NONE -> 0.05F;
        };
    }

    /** The ability (1-5) that toggles flight for this hero, matching the original concepts' numbering. */
    public int flightAbility() {
        return switch (this) {
            case MAGNETO -> 5;
            case SCARLET_WITCH, PHOTON -> 2;
            case NONE -> -1;
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

package com.kolodkingal.enhanced.platform;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** The attributes a loader adds to every living entity (NeoForge: {@code NeoForgeMod}). */
public interface EntityAttributesCompat {
    /** The swim speed multiplier ({@code neoforge:swim_speed}). */
    Holder<Attribute> swimSpeed();

    /** Whether a player may fly as in Creative ({@code neoforge:creative_flight}). */
    Holder<Attribute> creativeFlight();
}

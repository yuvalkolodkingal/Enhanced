package com.kolodkingal.enhanced.ironman;

/**
 * The parts every suit is drawn in (3.8, PLAN 7.1): the client's {@code SuitModels} draw each, {@code SuitAssembly}
 * poses each through a suit-up, and {@link SuitTimelines} (pure, tested) says how far each is on. Common, not client,
 * so the timelines and their GameTests can name them. Written by the 3.8 skeleton; lane L7 appends the extras of a suit
 * (the Mark 85's back flaps, blades and shield, the nano parts, the Hulkbuster's frame) at the end.
 */
public enum SuitParts {
    HELMET,
    FACEPLATE,
    CHEST,
    BACK,
    RIGHT_UPPER_ARM,
    LEFT_UPPER_ARM,
    RIGHT_FOREARM,
    LEFT_FOREARM,
    RIGHT_GAUNTLET,
    LEFT_GAUNTLET,
    RIGHT_THIGH,
    LEFT_THIGH,
    RIGHT_SHIN,
    LEFT_SHIN,
    RIGHT_BOOT,
    LEFT_BOOT;

    public static final SuitParts[] VALUES = values();

    /** Whether it is on the right side of the body (a right arm or leg). */
    public boolean right() {
        return name().startsWith("RIGHT_");
    }

    /** Whether it is on the left side of the body. */
    public boolean left() {
        return name().startsWith("LEFT_");
    }

    /** Whether it is part of the head (the helmet and its faceplate: the last to close on every suit-up). */
    public boolean head() {
        return this == HELMET || this == FACEPLATE;
    }
}

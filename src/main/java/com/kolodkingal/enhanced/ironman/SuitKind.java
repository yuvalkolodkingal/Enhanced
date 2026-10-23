package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * Iron Man's six suits (3.8, PLAN 5.3): the order is the HUD's and the synced bytes' ({@link IronManState} keeps an
 * ordinal, -1 for none). The classic suits are worn on the chest slot; the nanotech ones live in their housings and the
 * Nano Arc Reactor. Written by the 3.8 skeleton; append only.
 */
public enum SuitKind implements StringRepresentable {
    MARK_1("mark_1", false, false),
    MARK_3("mark_3", true, false),
    MARK_5("mark_5", true, false),
    MARK_50("mark_50", true, true),
    MARK_85("mark_85", true, true),
    HULKBUSTER("hulkbuster", true, false);

    public static final SuitKind[] VALUES = values();
    /** The byte of "no suit" in {@link IronManState}. */
    public static final byte NONE = -1;

    private final String id;
    private final boolean flies;
    private final boolean nanotech;

    SuitKind(String id, boolean flies, boolean nanotech) {
        this.id = id;
        this.flies = flies;
        this.nanotech = nanotech;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** {@code mark_3}: the item is {@code enhanced:iron_man_<id>}. */
    public String id() {
        return id;
    }

    /** Whether the suit flies (the Mark 1 has only its one rocket boost). */
    public boolean flies() {
        return flies;
    }

    /** A nanotech suit (Mark 50, Mark 85): deployed from the Nano Arc Reactor on the chest. */
    public boolean nanotech() {
        return nanotech;
    }

    /** A suit worn as a chest item (Mark 1, 3, 5, the Hulkbuster). */
    public boolean classic() {
        return !nanotech;
    }

    /** The ordinal as {@link IronManState} keeps it. */
    public byte asByte() {
        return (byte) ordinal();
    }

    /** The suit of a synced byte, or null for {@link #NONE} (or a byte this version does not know). */
    @Nullable
    public static SuitKind of(byte b) {
        return b >= 0 && b < VALUES.length ? VALUES[b] : null;
    }

    /** By id ({@code mark_85}), or null. */
    @Nullable
    public static SuitKind byId(String id) {
        for (SuitKind kind : VALUES) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * The ticks of its suit-up ({@code [iron_man] suitUpTicks...}; the plan's defaults where the config is not loaded).
     */
    public int upTicks() {
        ConfigSpec.IntValue value = switch (this) {
            case MARK_1 -> IronManConfig.SUIT_UP_TICKS_MARK1;
            case MARK_3 -> IronManConfig.SUIT_UP_TICKS_MARK3;
            case MARK_5 -> IronManConfig.SUIT_UP_TICKS_MARK5;
            case MARK_50 -> IronManConfig.SUIT_UP_TICKS_MARK50;
            case MARK_85 -> IronManConfig.SUIT_UP_TICKS_MARK85;
            case HULKBUSTER -> IronManConfig.SUIT_UP_TICKS_HULKBUSTER;
        };
        return EnhancedConfig.SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** The ticks of its take-off: {@code suitDownFactor} of the suit-up (half by default), at least one. */
    public int downTicks() {
        double factor = EnhancedConfig.SPEC.isLoaded() ? IronManConfig.SUIT_DOWN_FACTOR.get()
                : IronManConfig.SUIT_DOWN_FACTOR.getDefault();
        return Math.max(1, (int) Math.round(upTicks() * factor));
    }
}

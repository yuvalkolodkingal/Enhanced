package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.item.InfinityStone;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

import java.util.List;

/**
 * The six Infinity Stones as the Thanos fight knows them: their order on the Gauntlet, their colours (what every effect
 * of the stone is painted with), the colour of the boss bar while the stone is the newest, and their voices.
 *
 * <p>Thanos is given five of them, in the film's order: Power, Space, Reality, Soul, Time ({@link #granted}). The Mind
 * Stone is the sixth socket and never comes: it is in Loki's Scepter, in the hand of the one who calls him, and in
 * Vision's forehead. No move, no clock and no command can give it to him.
 */
public enum StoneKind {
    POWER(InfinityStone.POWER, 0x9B4DEB, 0xE6D2FF, BossEvent.BossBarColor.PURPLE),
    SPACE(InfinityStone.SPACE, 0x2F9BFF, 0xBFE6FF, BossEvent.BossBarColor.BLUE),
    REALITY(InfinityStone.REALITY, 0xE5253B, 0xFFB3BB, BossEvent.BossBarColor.RED),
    SOUL(InfinityStone.SOUL, 0xFF8A1E, 0xFFD6A0, BossEvent.BossBarColor.YELLOW),
    TIME(InfinityStone.TIME, 0x35D46A, 0xB8FFC9, BossEvent.BossBarColor.GREEN),
    MIND(InfinityStone.MIND, 0xF5D77A, 0xFFF6D5, BossEvent.BossBarColor.WHITE);

    private static final List<StoneKind> GRANTED = List.of(POWER, SPACE, REALITY, SOUL, TIME);

    private final InfinityStone voice;
    private final int color;
    private final int core;
    private final BossEvent.BossBarColor barColor;

    StoneKind(InfinityStone voice, int color, int core, BossEvent.BossBarColor barColor) {
        this.voice = voice;
        this.color = color;
        this.core = core;
        this.barColor = barColor;
    }

    /** The five stones Thanos can hold, in the order they arrive. */
    public static List<StoneKind> granted() {
        return GRANTED;
    }

    /** The stone that arrives {@code n}th (1 is Power, which he has from the start), or null past the fifth. */
    public static StoneKind nth(int n) {
        return n >= 1 && n <= GRANTED.size() ? GRANTED.get(n - 1) : null;
    }

    /** The stone's name in lang keys and sounds: {@code power}, {@code space}, ... */
    public String id() {
        return voice.id();
    }

    /** The main colour of its energy ({@code 0xRRGGBB}). */
    public int color() {
        return color;
    }

    /** The bright core colour of its energy. */
    public int core() {
        return core;
    }

    /** The colour of the boss bar while this is his newest stone. */
    public BossEvent.BossBarColor barColor() {
        return barColor;
    }

    /** The stone's voice. */
    public InfinityStone voice() {
        return voice;
    }

    /** Its name as players read it: "Power Stone". */
    public Component displayName() {
        return Component.translatable("stone.enhanced." + id());
    }
}

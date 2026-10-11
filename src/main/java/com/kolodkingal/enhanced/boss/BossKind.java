package com.kolodkingal.enhanced.boss;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** The three villains who are fought as bosses, and whose defeat wins the hero (see {@link BossFights}). */
public enum BossKind {
    ULTRON,
    THANOS,
    APOCALYPSE;

    /** The name in commands and messages: {@code ultron}, {@code thanos}, {@code apocalypse}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static BossKind byId(String id) {
        for (BossKind kind : values()) {
            if (kind.id().equals(id)) {
                return kind;
            }
        }
        return null;
    }
}

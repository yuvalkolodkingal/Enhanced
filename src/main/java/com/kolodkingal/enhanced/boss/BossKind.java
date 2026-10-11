package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.registry.ModTicketTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.TicketType;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/** The three villains who are fought as bosses, and whose defeat wins the hero (see {@link BossFights}). */
public enum BossKind {
    ULTRON,
    THANOS,
    APOCALYPSE;

    /** The name in commands and messages: {@code ultron}, {@code thanos}, {@code apocalypse}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The boss's name as players read it: its entity's name. */
    public Component displayName() {
        return Component.translatable("entity.enhanced." + id());
    }

    /** The chunk ticket that keeps this boss's arena loaded while its fight runs. */
    public TicketType<UUID> ticket() {
        return switch (this) {
            case ULTRON -> ModTicketTypes.ENHANCED_ULTRON;
            case THANOS -> ModTicketTypes.ENHANCED_THANOS;
            case APOCALYPSE -> ModTicketTypes.ENHANCED_APOCALYPSE;
        };
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

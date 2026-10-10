package com.kolodkingal.enhanced.power;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * The shape Loki has taken, as saved in {@link PowerData#form()}: a creature's entity type id
 * ({@code minecraft:zombie}), or another player as {@code minecraft:player;<uuid>;<name>} (he wears that player's skin
 * and shows their name).
 *
 * @param type   the creature's type ({@link EntityType#PLAYER} for another player)
 * @param player the copied player's UUID, or null for a creature
 * @param name   the copied player's name, or empty for a creature
 */
public record LokiForm(EntityType<?> type, @Nullable UUID player, String name) {
    private static final String PLAYER_PREFIX = "minecraft:player;";

    public static LokiForm creature(EntityType<?> type) {
        return new LokiForm(type, null, "");
    }

    public static LokiForm player(UUID uuid, String name) {
        return new LokiForm(EntityType.PLAYER, uuid, name);
    }

    /** The form saved in {@code data}, or empty if Loki is himself (or the saved form no longer exists). */
    public static Optional<LokiForm> of(PowerData data) {
        return parse(data.form());
    }

    public static Optional<LokiForm> parse(String form) {
        if (form.isEmpty()) {
            return Optional.empty();
        }
        if (form.startsWith(PLAYER_PREFIX)) {
            String[] parts = form.substring(PLAYER_PREFIX.length()).split(";", 2);
            try {
                return Optional.of(player(UUID.fromString(parts[0]), parts.length > 1 ? parts[1] : ""));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return EntityType.byString(form).filter(type -> type != EntityType.PLAYER).map(LokiForm::creature);
    }

    public String serialize() {
        return player != null ? PLAYER_PREFIX + player + ";" + name : EntityType.getKey(type).toString();
    }

    public boolean isPlayer() {
        return player != null;
    }

    /** What Loki looks like now: the creature's name, or the copied player's. */
    public Component displayName() {
        return isPlayer() ? Component.literal(name) : type.getDescription();
    }
}

package com.kolodkingal.enhanced.config.core;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The server's settings changed from inside the game (3.17): by {@code /enhanced config} ({@code config.ConfigCommand})
 * and by an operator's settings screen on a server that is not his own ({@link ConfigEditPayload}). Every setting of the
 * server's config file has a key, its sections' names and its own joined by dots ({@code custom_hero.budget}).
 *
 * <p>A change is checked here, on the server, by the rules the config's spec has (type, range, the allowed choices): a
 * value that is not acceptable changes nothing. An accepted one is put in use at once, written to the config file the
 * server reads (the world's own copy in {@code serverconfig/} if there is one, else {@code config/}), exactly as a hand
 * edit of that file is picked up, and sent to every player ({@link ConfigValuePayload}), whose client uses the server's
 * values while connected. Nobody's game changes who never uses the command or the screen.
 */
public final class ConfigEdits {
    /** The permission level that may change the server's settings: an operator's, as for the rest of {@code /enhanced}. */
    public static final int PERMISSION = 2;

    /** The player whose screen asks for the change being made (the server's thread only), or null: the command's. */
    @Nullable
    private static ServerPlayer asking;

    private ConfigEdits() {
    }

    /** A value that was refused, with the words for whoever asked. */
    public static final class Refused extends Exception {
        private final transient Component reason;

        public Refused(Component reason) {
            super(reason.getString());
            this.reason = reason;
        }

        public Component reason() {
            return reason;
        }
    }

    /** The key of a setting: its path joined by dots. */
    public static String key(ConfigSpec.ConfigValue<?> value) {
        return String.join(".", value.getPath());
    }

    /** Every setting of the server's config files, in the files' order. */
    public static List<ConfigSpec.ConfigValue<?>> values() {
        List<ConfigSpec.ConfigValue<?>> all = new ArrayList<>();
        for (ConfigFile file : Configs.of(ConfigFile.Type.SERVER)) {
            all.addAll(file.spec().values());
        }
        return all;
    }

    /** Every key, in the files' order. */
    public static List<String> keys() {
        List<String> keys = new ArrayList<>();
        for (ConfigSpec.ConfigValue<?> value : values()) {
            keys.add(key(value));
        }
        return keys;
    }

    /** The setting of {@code key}, or null. */
    @Nullable
    public static ConfigSpec.ConfigValue<?> find(String key) {
        for (ConfigSpec.ConfigValue<?> value : values()) {
            if (key(value).equals(key)) {
                return value;
            }
        }
        return null;
    }

    /** A value as text: what {@link #parse} reads back, what the command shows, what travels to the clients. */
    public static String text(@Nullable Object value) {
        if (value instanceof Enum<?> constant) {
            return constant.name();
        }
        if (value instanceof List<?>) {
            Toml.Table table = new Toml.Table();
            table.set(List.of("v"), value);
            String written = Toml.write(table, "\n").strip();
            return written.substring(written.indexOf('=') + 1).strip();
        }
        return String.valueOf(value);
    }

    /** What the setting accepts, in a few words (its range, its choices, its type). */
    public static String accepts(ConfigSpec.ConfigValue<?> value) {
        ConfigSpec.ValueSpec spec = value.getSpec();
        if (spec.getRange() != null) {
            return spec.getRange().toString();
        }
        if (value instanceof ConfigSpec.BooleanValue) {
            return "true, false";
        }
        if (value instanceof ConfigSpec.EnumValue<?> choice) {
            List<String> names = new ArrayList<>();
            for (Enum<?> constant : choice.enumClass().getEnumConstants()) {
                if (spec.test(constant)) {
                    names.add(constant.name());
                }
            }
            return String.join(", ", names);
        }
        return spec instanceof ConfigSpec.ListValueSpec ? "[ ... ]" : spec.getDefault() instanceof String ? "text" : "";
    }

    /**
     * Reads {@code text} as a value of this setting and checks it by the spec's own rules.
     *
     * @throws Refused if it is not a value of the setting's type or the spec does not accept it
     */
    public static Object parse(ConfigSpec.ConfigValue<?> value, String text) throws Refused {
        ConfigSpec.ValueSpec spec = value.getSpec();
        String trimmed = text.strip();
        Object parsed;
        try {
            if (value instanceof ConfigSpec.BooleanValue) {
                if (!trimmed.equalsIgnoreCase("true") && !trimmed.equalsIgnoreCase("false")) {
                    throw new NumberFormatException(trimmed);
                }
                parsed = Boolean.parseBoolean(trimmed.toLowerCase(Locale.ROOT));
            } else if (value instanceof ConfigSpec.IntValue) {
                parsed = Integer.decode(trimmed);
            } else if (value instanceof ConfigSpec.LongValue) {
                parsed = Long.decode(trimmed);
            } else if (value instanceof ConfigSpec.DoubleValue) {
                parsed = Double.parseDouble(trimmed);
            } else if (value instanceof ConfigSpec.EnumValue<?> choice) {
                parsed = null;
                for (Enum<?> constant : choice.enumClass().getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(trimmed)) {
                        parsed = constant;
                    }
                }
            } else if (spec instanceof ConfigSpec.ListValueSpec || !(spec.getDefault() instanceof String)) {
                parsed = Toml.parse("v = " + trimmed + "\n").get(List.of("v"));
                Object def = spec.getDefault();
                if (parsed instanceof Number number && def instanceof Integer) {
                    parsed = number.intValue();
                } else if (parsed instanceof Number number && def instanceof Long) {
                    parsed = number.longValue();
                } else if (parsed instanceof Number number && def instanceof Double) {
                    parsed = number.doubleValue();
                }
            } else {
                parsed = trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")
                        ? trimmed.substring(1, trimmed.length() - 1) : text;
            }
        } catch (NumberFormatException | Toml.ParseException e) {
            parsed = null;
        }
        if (parsed == null || !spec.test(parsed)) {
            throw new Refused(Component.translatableWithFallback("commands.enhanced.config.refused",
                    "%s does not accept \"%s\" (it accepts: %s). Nothing was changed.", key(value), trimmed, accepts(value)));
        }
        return parsed;
    }

    /**
     * Puts a checked value in use on the server, writes the config file and tells every player. On the server's thread.
     */
    @SuppressWarnings("unchecked")
    public static void set(ConfigSpec.ConfigValue<?> value, Object checked) {
        ConfigSpec spec = value.owner();
        if (!spec.isLoaded()) {
            return;
        }
        ((ConfigSpec.ConfigValue<Object>) value).set(checked);
        spec.save();
        // (as when the watcher reads a hand-edited file again: every value that needs no restart is read afresh)
        spec.afterReload();
        ConfigValuePayload news = new ConfigValuePayload(key(value), text(checked));
        ServerPlayer from = asking;
        if (from == null) {
            Net.sendToAllPlayers(news);
        } else {
            // (whose screen made the change has it already: told again, a value typed a letter at a time would come back
            // to his screen a step behind his typing)
            for (ServerPlayer other : from.level().getServer().getPlayerList().getPlayers()) {
                if (other != from) {
                    Net.sendToPlayer(other, news);
                }
            }
        }
        Enhanced.LOGGER.info("Config: {} = {}", key(value), text(checked));
    }

    /** Reads, checks and sets: the command's and the screen's one way in. Returns the value now in use. */
    public static Object change(String key, @Nullable String text) throws Refused {
        ConfigSpec.ConfigValue<?> value = find(key);
        if (value == null || !value.owner().isLoaded()) {
            throw new Refused(Component.translatableWithFallback("commands.enhanced.config.unknown",
                    "There is no setting %s.", key));
        }
        Object checked = text == null ? value.getDefault() : parse(value, text);
        set(value, checked);
        return checked;
    }

    /** Whether this player may change the server's settings. */
    public static boolean mayEdit(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    /**
     * A change asked for by a player's settings screen ({@link ConfigEditPayload}): the server checks who asks and what.
     * A refused one changes nothing; the player is told why and sent the value that stands, so his screen is right again.
     *
     * @return whether it was made
     */
    public static boolean fromClient(ServerPlayer player, String key, String text, boolean reset) {
        return fromClient(player, mayEdit(player), key, text, reset);
    }

    /** The same, for a player whose right to change the settings has been looked up already. */
    public static boolean fromClient(ServerPlayer player, boolean operator, String key, String text, boolean reset) {
        Component refusal;
        if (!operator) {
            refusal = Component.translatableWithFallback("commands.enhanced.config.not_operator",
                    "Only an operator can change the server's settings.");
        } else {
            asking = player;
            try {
                change(key, reset ? null : text);
                return true;
            } catch (Refused refused) {
                refusal = refused.reason();
            } finally {
                asking = null;
            }
        }
        player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), false);
        ConfigSpec.ConfigValue<?> value = find(key);
        if (value != null && value.owner().isLoaded()) {
            Net.sendToPlayer(player, new ConfigValuePayload(key, text(value.getRaw())));
        }
        return false;
    }
}

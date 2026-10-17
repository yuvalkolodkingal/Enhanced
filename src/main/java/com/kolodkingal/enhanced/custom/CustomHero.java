package com.kolodkingal.enhanced.custom;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A hero of a player's own, made with the Hero Creator (3.3): what {@code Power.CUSTOM} is for that player. It is data,
 * not code: a name, two colours, a look, the powers picked from the catalogue ({@link PowerModule}) with their strength
 * and option, and which power each of the five keys (R, G, V, B, N) uses. It is saved with the player, kept on death,
 * synced to every client that sees him (his skin with it) and written to and read from a JSON file
 * ({@link CustomHeroJson}). Nothing here is trusted: the server passes every definition it gets through
 * {@link CustomHeroRules#sanitize} first.
 *
 * @param version the format of the definition ({@link #VERSION} when it was made by this version)
 * @param name    the hero's name (1 to {@link #MAX_NAME} characters once sanitised)
 * @param color   the primary colour of his effects, beams, particles and HUD (0xRRGGBB)
 * @param glow    the glow colour (cores, highlights)
 * @param look    {@link #LOOK_OWN} (the player's own skin), {@link #LOOK_SKIN} (the PNG in {@code skin}) or
 *                {@code <power>/<look>}: the skin of a look of a hero of the mod
 * @param slim    the skin has the slim (3-pixel) arms (only for {@link #LOOK_SKIN}; a hero's look says its own)
 * @param skin    a 64x64 PNG (empty when there is none)
 * @param powers  the powers picked, in the order they were picked
 * @param keys    the module id each key uses, R G V B N ({@link #KEYS} entries; empty: nothing)
 */
public record CustomHero(int version, String name, int color, int glow, String look, boolean slim, byte[] skin, List<Pick> powers,
                         List<String> keys) {
    /** The format this version writes. */
    public static final int VERSION = 1;
    /** Ability keys a hero has (R, G, V, B, N). */
    public static final int KEYS = 5;
    public static final int MAX_NAME = 24;
    public static final String LOOK_OWN = "own";
    public static final String LOOK_SKIN = "skin";
    /** No custom skin can be bigger than this on the wire, whatever the config says (a packet to the server holds 32 KiB). */
    public static final int MAX_SKIN_WIRE = 24 * 1024;
    /** What the network accepts of the rest, to keep a forged packet small. */
    public static final int MAX_PICKS_WIRE = 40;
    private static final int MAX_TEXT_WIRE = 64;

    /** A hero with nothing picked yet: what a player starts with in the creator, and what a player without one has. */
    public static final CustomHero EMPTY = new CustomHero(VERSION, "", 0x3FA9F5, 0xE0F4FF, LOOK_OWN, false, new byte[0], List.of(),
            List.of("", "", "", "", ""));

    /**
     * One power picked from the catalogue.
     *
     * @param id     the module's id (a {@link PowerModule#id}; an id this version does not know is dropped by the rules)
     * @param strength 1 to {@link PowerModule#MAX_STRENGTH}: how strong it is (its numbers, {@link ModuleStats}, and its cost)
     * @param option the id of one of the module's options
     * @param color  its own colour (0xRRGGBB), or -1 for the hero's
     */
    public record Pick(String id, int strength, String option, int color) {
        /** The module, or null for a power this version does not know. */
        @Nullable
        public PowerModule module() {
            return PowerModule.byId(id);
        }

        /** How its key acts (none for a module this version does not know). */
        public PowerModule.Act act() {
            PowerModule module = module();
            return module == null ? PowerModule.Act.NONE : module.act(option);
        }

        public Pick withStrength(int strength) {
            return new Pick(id, strength, option, color);
        }

        public Pick withOption(String option) {
            return new Pick(id, strength, option, color);
        }

        public Pick withColor(int color) {
            return new Pick(id, strength, option, color);
        }
    }

    public CustomHero {
        name = name == null ? "" : name;
        look = look == null ? LOOK_OWN : look;
        skin = skin == null ? new byte[0] : skin;
        powers = powers == null ? List.of() : List.copyOf(powers);
        List<String> k = new ArrayList<>(keys == null ? List.of() : keys);
        while (k.size() < KEYS) {
            k.add("");
        }
        keys = Collections.unmodifiableList(new ArrayList<>(k.subList(0, KEYS)));
    }

    // ---- reading it ----

    /** Whether nothing has been made yet (no powers and no name). */
    public boolean isEmpty() {
        return powers.isEmpty() && name.isEmpty();
    }

    /** The pick of this module, or null. */
    @Nullable
    public Pick pick(PowerModule module) {
        for (Pick pick : powers) {
            if (pick.id().equals(module.id)) {
                return pick;
            }
        }
        return null;
    }

    public boolean has(PowerModule module) {
        return pick(module) != null;
    }

    /** The pick on key {@code slot} (1-5), or null when the key is free (or names a power that is not picked). */
    @Nullable
    public Pick onKey(int slot) {
        if (slot < 1 || slot > KEYS) {
            return null;
        }
        PowerModule module = PowerModule.byId(keys.get(slot - 1));
        return module == null ? null : pick(module);
    }

    /** The key (1-5) this module is on, or 0. */
    public int keyOf(PowerModule module) {
        for (int i = 0; i < KEYS; i++) {
            if (module.id.equals(keys.get(i))) {
                return i + 1;
            }
        }
        return 0;
    }

    /** The colour of a picked power's effects: its own, or the hero's. */
    public int colorOf(@Nullable Pick pick) {
        return pick == null || pick.color() < 0 ? color : pick.color() & 0xFFFFFF;
    }

    /** Whether the look is the custom PNG and there is one. */
    public boolean wearsSkin() {
        return LOOK_SKIN.equals(look) && skin.length > 0;
    }

    // ---- changing it (each returns a new definition) ----

    public CustomHero withName(String name) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys);
    }

    public CustomHero withColors(int color, int glow) {
        return new CustomHero(version, name, color & 0xFFFFFF, glow & 0xFFFFFF, look, slim, skin, powers, keys);
    }

    public CustomHero withLook(String look, boolean slim) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys);
    }

    public CustomHero withSkin(byte[] skin) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys);
    }

    public CustomHero withPowers(List<Pick> powers) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys);
    }

    public CustomHero withKeys(List<String> keys) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys);
    }

    /** This definition with {@code pick} replacing the pick of its module (or added at the end). */
    public CustomHero with(Pick pick) {
        List<Pick> list = new ArrayList<>(powers);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(pick.id())) {
                list.set(i, pick);
                return withPowers(list);
            }
        }
        list.add(pick);
        return withPowers(list);
    }

    /** This definition without the module (and off every key it was on). */
    public CustomHero without(PowerModule module) {
        List<Pick> list = new ArrayList<>(powers);
        list.removeIf(p -> p.id().equals(module.id));
        List<String> k = new ArrayList<>(keys);
        k.replaceAll(id -> id.equals(module.id) ? "" : id);
        return withPowers(list).withKeys(k);
    }

    /** This definition with the module on key {@code slot} (1-5), taken off any other key it was on. */
    public CustomHero withKey(int slot, String moduleId) {
        List<String> k = new ArrayList<>(keys);
        if (!moduleId.isEmpty()) {
            k.replaceAll(id -> id.equals(moduleId) ? "" : id);
        }
        k.set(slot - 1, moduleId);
        return withKeys(k);
    }

    // ---- equality: a record compares arrays by reference ----

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CustomHero h && version == h.version && color == h.color && glow == h.glow && slim == h.slim
                && name.equals(h.name) && look.equals(h.look) && Arrays.equals(skin, h.skin) && powers.equals(h.powers) && keys.equals(h.keys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, name, color, glow, look, slim, Arrays.hashCode(skin), powers, keys);
    }

    @Override
    public String toString() {
        return "CustomHero[" + name + ", " + look + ", " + powers + ", keys " + keys + ", skin " + skin.length + " bytes]";
    }

    // ---- codecs ----

    /**
     * How the definition is saved with the player: its JSON (the format of the export, {@link CustomHeroJson}) as one
     * string, so the save and the file are one format. A save that does not read becomes {@link #EMPTY}.
     */
    public static final Codec<CustomHero> CODEC = Codec.STRING.xmap(text -> {
        CustomHeroJson.Read read = CustomHeroJson.read(text);
        return read.hero() == null ? EMPTY : read.hero();
    }, CustomHeroJson::write);

    private static final StreamCodec<ByteBuf, String> TEXT = ByteBufCodecs.stringUtf8(MAX_TEXT_WIRE);

    private static final StreamCodec<ByteBuf, Pick> PICK_CODEC = StreamCodec.composite(
            TEXT, Pick::id,
            ByteBufCodecs.VAR_INT, Pick::strength,
            TEXT, Pick::option,
            ByteBufCodecs.INT, Pick::color,
            Pick::new);

    /** The wire form (the skin raw): what a client sends the server, and what the server syncs to every client. */
    public static final StreamCodec<ByteBuf, CustomHero> STREAM_CODEC = StreamCodec.of(CustomHero::write, CustomHero::read);

    private static void write(ByteBuf buf, CustomHero hero) {
        ByteBufCodecs.VAR_INT.encode(buf, hero.version);
        TEXT.encode(buf, clip(hero.name));
        ByteBufCodecs.INT.encode(buf, hero.color);
        ByteBufCodecs.INT.encode(buf, hero.glow);
        TEXT.encode(buf, clip(hero.look));
        ByteBufCodecs.BOOL.encode(buf, hero.slim);
        ByteBufCodecs.byteArray(MAX_SKIN_WIRE).encode(buf, hero.skin.length <= MAX_SKIN_WIRE ? hero.skin : new byte[0]);
        List<Pick> picks = hero.powers.size() <= MAX_PICKS_WIRE ? hero.powers : hero.powers.subList(0, MAX_PICKS_WIRE);
        ByteBufCodecs.VAR_INT.encode(buf, picks.size());
        for (Pick pick : picks) {
            PICK_CODEC.encode(buf, new Pick(clip(pick.id()), pick.strength(), clip(pick.option()), pick.color()));
        }
        for (String key : hero.keys) {
            TEXT.encode(buf, clip(key));
        }
    }

    private static CustomHero read(ByteBuf buf) {
        int version = ByteBufCodecs.VAR_INT.decode(buf);
        String name = TEXT.decode(buf);
        int color = ByteBufCodecs.INT.decode(buf);
        int glow = ByteBufCodecs.INT.decode(buf);
        String look = TEXT.decode(buf);
        boolean slim = ByteBufCodecs.BOOL.decode(buf);
        byte[] skin = ByteBufCodecs.byteArray(MAX_SKIN_WIRE).decode(buf);
        int count = ByteBufCodecs.VAR_INT.decode(buf);
        if (count < 0 || count > MAX_PICKS_WIRE) {
            throw new IllegalArgumentException("A hero with " + count + " powers");
        }
        List<Pick> picks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            picks.add(PICK_CODEC.decode(buf));
        }
        List<String> keys = new ArrayList<>(KEYS);
        for (int i = 0; i < KEYS; i++) {
            keys.add(TEXT.decode(buf));
        }
        return new CustomHero(version, name, color, glow, look, slim, skin, picks, keys);
    }

    /** A string cut to what the wire carries (in characters; a long name is cut by the rules long before this). */
    private static String clip(String s) {
        return s.length() <= MAX_TEXT_WIRE ? s : s.substring(0, MAX_TEXT_WIRE);
    }
}

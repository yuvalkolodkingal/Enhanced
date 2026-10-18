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
 * and option, and which power each of the five keys (R, G, V, B, N) uses. 3.4 (format 2): the catalogue holds the
 * built-in heroes' abilities too ({@link HeroAbility}), each key has a Sneak binding, each binding a gesture, and the
 * hero a way to move ({@link Anim}) and the layers of his look ({@link Layers}). It is saved with the player, kept on death,
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
 * @param sneakKeys 3.4: what each key does with Sneak held, R G V B N: {@code <hero id>#alt} (the Sneak variant of the hero
 *                ability on the plain key), {@code <module>#<option>} (another press option of the module on the plain key),
 *                a picked entry's id, or empty (Sneak + the key does what the key does)
 * @param moves   3.4: the gesture of each binding ({@link #MOVES} entries: R G V B N, then Sneak + R G V B N), each
 *                {@link #AUTO}, {@link #NONE} or a gesture of {@code AnimCatalog.GESTURES}
 * @param anim    3.4: how the hero moves (flight style, run, speedster run, idle)
 * @param layers  3.4: the optional layers of his look (cape, hat, visor, eyes, aura)
 */
public record CustomHero(int version, String name, int color, int glow, String look, boolean slim, byte[] skin, List<Pick> powers,
                         List<String> keys, List<String> sneakKeys, List<String> moves, Anim anim, Layers layers) {
    /** The format this version writes (3.4: 2; a 3.3 file, version 1, is read and migrated by {@link CustomHeroJson}). */
    public static final int VERSION = 2;
    /** The value of a move, a stance or an animation choice that leaves it to the mod. */
    public static final String AUTO = "auto";
    /** The value of a move or a stance that plays nothing. */
    public static final String NONE = "none";
    /** Bindings a hero has: five keys, plain and with Sneak. */
    public static final int MOVES = 10;
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
     * @param id     the entry's id: a {@link PowerModule#id} or a {@link HeroAbility#id} (an id this version does not know
     *               is dropped by the rules)
     * @param strength 1 to {@link PowerModule#MAX_STRENGTH}: how strong it is (its numbers, {@link ModuleStats}, and its cost)
     * @param option the id of one of the module's options; for a hero's ability {@link HeroAbility#BOTH} or
     *               {@link HeroAbility#MAIN} (its Sneak variant off)
     * @param color  its own colour (0xRRGGBB), or -1 for the hero's
     * @param stance 3.4: the stance while it is held or on: {@link #AUTO}, {@link #NONE} or one of {@code AnimCatalog}'s
     */
    public record Pick(String id, int strength, String option, int color, String stance) {
        public Pick {
            id = id == null ? "" : id;
            option = option == null ? "" : option;
            stance = stance == null || stance.isEmpty() ? AUTO : stance;
        }

        /** A pick as 3.3 made it (the stance automatic). */
        public Pick(String id, int strength, String option, int color) {
            this(id, strength, option, color, AUTO);
        }

        /** The module, or null for a hero's ability or a power this version does not know. */
        @Nullable
        public PowerModule module() {
            return PowerModule.byId(id);
        }

        /** The hero's ability, or null for a generic module or a power this version does not know. */
        @Nullable
        public HeroAbility ability() {
            return HeroAbility.byId(id);
        }

        /** The catalogue entry (a module or a hero's ability), or null. */
        @Nullable
        public CatalogEntry entry() {
            return Catalog.byId(id);
        }

        /** How its key acts (none for an entry this version does not know). */
        public PowerModule.Act act() {
            CatalogEntry entry = entry();
            return entry == null ? PowerModule.Act.NONE : entry.act(option);
        }

        public Pick withStrength(int strength) {
            return new Pick(id, strength, option, color, stance);
        }

        public Pick withOption(String option) {
            return new Pick(id, strength, option, color, stance);
        }

        public Pick withColor(int color) {
            return new Pick(id, strength, option, color, stance);
        }

        public Pick withStance(String stance) {
            return new Pick(id, strength, option, color, stance);
        }
    }

    /**
     * How the hero moves (3.4), each {@link #AUTO} by default: the flight style ({@code AnimCatalog.FLIGHT_STYLES}), the run
     * character ({@code RUN_CHARACTERS}), the speedster's run ({@code SPEED_RUNS}) and the idle stance ({@code IDLES}).
     */
    public record Anim(String flight, String run, String speedRun, String idle) {
        public static final Anim AUTO_ALL = new Anim(AUTO, AUTO, AUTO, AUTO);

        public Anim {
            flight = orAuto(flight);
            run = orAuto(run);
            speedRun = orAuto(speedRun);
            idle = orAuto(idle);
        }
    }

    /**
     * One optional layer of the look (3.4).
     *
     * @param style one of {@code LookCatalog}'s styles of its kind, {@link #NONE} for none
     * @param color its tint (0xRRGGBB), or -1 for the hero's colour
     * @param row   the glowing eyes only: the row of the face they sit on, 0-3
     * @param when  eyes and aura: {@code always}, {@code powers} (while a power is on) or {@code flying}
     */
    public record Layer(String style, int color, int row, String when) {
        public static final String ALWAYS = "always";
        public static final Layer NONE_LAYER = new Layer(NONE, -1, 0, ALWAYS);

        public Layer {
            style = style == null || style.isEmpty() ? NONE : style;
            when = when == null || when.isEmpty() ? ALWAYS : when;
        }

        public boolean isNone() {
            return NONE.equals(style);
        }
    }

    /** The five layers of the look (3.4); each {@link Layer#NONE_LAYER} unless chosen. */
    public record Layers(Layer cape, Layer hat, Layer visor, Layer eyes, Layer aura) {
        public static final Layers NONE_ALL = new Layers(Layer.NONE_LAYER, Layer.NONE_LAYER, Layer.NONE_LAYER, Layer.NONE_LAYER,
                Layer.NONE_LAYER);
        /** The names of the five kinds, in order (the JSON's keys and {@code LookCatalog}'s lists). */
        public static final List<String> KINDS = List.of("cape", "hat", "visor", "eyes", "aura");

        public Layers {
            cape = cape == null ? Layer.NONE_LAYER : cape;
            hat = hat == null ? Layer.NONE_LAYER : hat;
            visor = visor == null ? Layer.NONE_LAYER : visor;
            eyes = eyes == null ? Layer.NONE_LAYER : eyes;
            aura = aura == null ? Layer.NONE_LAYER : aura;
        }

        /** The layers in the order of {@link #KINDS}. */
        public List<Layer> list() {
            return List.of(cape, hat, visor, eyes, aura);
        }

        /** The layer of kind {@code i} (the index in {@link #KINDS}). */
        public Layer get(int i) {
            return list().get(i);
        }

        /** These layers with kind {@code i} replaced. */
        public Layers with(int i, Layer layer) {
            List<Layer> l = new ArrayList<>(list());
            l.set(i, layer);
            return of(l);
        }

        public static Layers of(List<Layer> five) {
            return new Layers(five.get(0), five.get(1), five.get(2), five.get(3), five.get(4));
        }

        public boolean allNone() {
            return list().stream().allMatch(Layer::isNone);
        }
    }

    public CustomHero {
        name = name == null ? "" : name;
        look = look == null ? LOOK_OWN : look;
        skin = skin == null ? new byte[0] : skin;
        powers = powers == null ? List.of() : List.copyOf(powers);
        keys = fill(keys, KEYS, "");
        sneakKeys = fill(sneakKeys, KEYS, "");
        moves = fill(moves, MOVES, AUTO);
        anim = anim == null ? Anim.AUTO_ALL : anim;
        layers = layers == null ? Layers.NONE_ALL : layers;
    }

    /** A hero as 3.3 made it: no Sneak bindings, every move and animation automatic, no layers. */
    public CustomHero(int version, String name, int color, int glow, String look, boolean slim, byte[] skin, List<Pick> powers,
                      List<String> keys) {
        this(version, name, color, glow, look, slim, skin, powers, keys, null, null, null, null);
    }

    private static List<String> fill(@Nullable List<String> in, int size, String blank) {
        List<String> k = new ArrayList<>(in == null ? List.of() : in);
        k.replaceAll(s -> s == null || s.isEmpty() ? blank : s);
        while (k.size() < size) {
            k.add(blank);
        }
        return Collections.unmodifiableList(new ArrayList<>(k.subList(0, size)));
    }

    private static String orAuto(@Nullable String s) {
        return s == null || s.isEmpty() ? AUTO : s;
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

    /** The pick of this entry (a module or a hero's ability), or null. */
    @Nullable
    public Pick pick(CatalogEntry entry) {
        for (Pick pick : powers) {
            if (pick.id().equals(entry.id())) {
                return pick;
            }
        }
        return null;
    }

    public boolean has(CatalogEntry entry) {
        return pick(entry) != null;
    }

    /**
     * What a binding names (3.4): the text of key {@code column} (1-5) plain or with Sneak, as saved ({@code thor.hammer},
     * {@code thor.hammer#alt}, {@code energy_blast#burst}, or empty). {@code HeroKit} resolves it.
     */
    public String binding(int column, boolean sneak) {
        if (column < 1 || column > KEYS) {
            return "";
        }
        return (sneak ? sneakKeys : keys).get(column - 1);
    }

    /** The move (gesture) of a binding: {@link #AUTO}, {@link #NONE} or a gesture id. */
    public String move(int column, boolean sneak) {
        return column < 1 || column > KEYS ? AUTO : moves.get((sneak ? KEYS : 0) + column - 1);
    }

    /** The index in {@link #moves} of a binding. */
    public static int moveIndex(int column, boolean sneak) {
        return (sneak ? KEYS : 0) + column - 1;
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

    /**
     * The key (1-5) this module is on, or 0: its plain key, else (3.4) the key whose Sneak cell names it (alone or with
     * one of its options).
     */
    public int keyOf(PowerModule module) {
        for (int i = 0; i < KEYS; i++) {
            if (module.id.equals(keys.get(i))) {
                return i + 1;
            }
        }
        for (int i = 0; i < KEYS; i++) {
            String s = sneakKeys.get(i);
            if (module.id.equals(s) || s.startsWith(module.id + "#")) {
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
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    public CustomHero withColors(int color, int glow) {
        return new CustomHero(version, name, color & 0xFFFFFF, glow & 0xFFFFFF, look, slim, skin, powers, keys, sneakKeys, moves,
                anim, layers);
    }

    public CustomHero withLook(String look, boolean slim) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    public CustomHero withSkin(byte[] skin) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    public CustomHero withPowers(List<Pick> powers) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    public CustomHero withKeys(List<String> keys) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    public CustomHero withSneakKeys(List<String> sneakKeys) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    public CustomHero withMoves(List<String> moves) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    public CustomHero withAnim(Anim anim) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    public CustomHero withLayers(Layers layers) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    public CustomHero withVersion(int version) {
        return new CustomHero(version, name, color, glow, look, slim, skin, powers, keys, sneakKeys, moves, anim, layers);
    }

    /** This definition with binding {@code column} (1-5) plain or with Sneak set to {@code text}. */
    public CustomHero withBinding(int column, boolean sneak, String text) {
        List<String> k = new ArrayList<>(sneak ? sneakKeys : keys);
        k.set(column - 1, text);
        return sneak ? withSneakKeys(k) : withKeys(k);
    }

    /** This definition with the move of a binding set. */
    public CustomHero withMove(int column, boolean sneak, String move) {
        List<String> m = new ArrayList<>(moves);
        m.set(moveIndex(column, sneak), move);
        return withMoves(m);
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
        return without((CatalogEntry) module);
    }

    /** This definition without the entry (and off every key, plain or with Sneak, it was on). */
    public CustomHero without(CatalogEntry entry) {
        List<Pick> list = new ArrayList<>(powers);
        list.removeIf(p -> p.id().equals(entry.id()));
        List<String> k = new ArrayList<>(keys);
        k.replaceAll(id -> id.equals(entry.id()) ? "" : id);
        List<String> sk = new ArrayList<>(sneakKeys);
        sk.replaceAll(id -> id.equals(entry.id()) || id.startsWith(entry.id() + "#") ? "" : id);
        return withPowers(list).withKeys(k).withSneakKeys(sk);
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
                && name.equals(h.name) && look.equals(h.look) && Arrays.equals(skin, h.skin) && powers.equals(h.powers) && keys.equals(h.keys)
                && sneakKeys.equals(h.sneakKeys) && moves.equals(h.moves) && anim.equals(h.anim) && layers.equals(h.layers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, name, color, glow, look, slim, Arrays.hashCode(skin), powers, keys, sneakKeys, moves, anim, layers);
    }

    @Override
    public String toString() {
        return "CustomHero[" + name + ", " + look + ", " + powers + ", keys " + keys + ", sneak " + sneakKeys + ", moves " + moves
                + ", " + anim + ", " + layers + ", skin " + skin.length + " bytes]";
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
            TEXT, Pick::stance,
            Pick::new);

    private static final StreamCodec<ByteBuf, Layer> LAYER_CODEC = StreamCodec.composite(
            TEXT, Layer::style,
            ByteBufCodecs.INT, Layer::color,
            ByteBufCodecs.VAR_INT, Layer::row,
            TEXT, Layer::when,
            Layer::new);

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
            PICK_CODEC.encode(buf, new Pick(clip(pick.id()), pick.strength(), clip(pick.option()), pick.color(), clip(pick.stance())));
        }
        for (String key : hero.keys) {
            TEXT.encode(buf, clip(key));
        }
        // 3.4 (protocol 8): the Sneak bindings, the moves, the animation and the layers, in this order.
        for (String key : hero.sneakKeys) {
            TEXT.encode(buf, clip(key));
        }
        for (String move : hero.moves) {
            TEXT.encode(buf, clip(move));
        }
        TEXT.encode(buf, clip(hero.anim.flight()));
        TEXT.encode(buf, clip(hero.anim.run()));
        TEXT.encode(buf, clip(hero.anim.speedRun()));
        TEXT.encode(buf, clip(hero.anim.idle()));
        for (Layer layer : hero.layers.list()) {
            LAYER_CODEC.encode(buf, new Layer(clip(layer.style()), layer.color(), layer.row(), clip(layer.when())));
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
        List<String> sneakKeys = new ArrayList<>(KEYS);
        for (int i = 0; i < KEYS; i++) {
            sneakKeys.add(TEXT.decode(buf));
        }
        List<String> moves = new ArrayList<>(MOVES);
        for (int i = 0; i < MOVES; i++) {
            moves.add(TEXT.decode(buf));
        }
        Anim anim = new Anim(TEXT.decode(buf), TEXT.decode(buf), TEXT.decode(buf), TEXT.decode(buf));
        List<Layer> layers = new ArrayList<>(5);
        for (int i = 0; i < Layers.KINDS.size(); i++) {
            layers.add(LAYER_CODEC.decode(buf));
        }
        return new CustomHero(version, name, color, glow, look, slim, skin, picks, keys, sneakKeys, moves, anim, Layers.of(layers));
    }

    /** A string cut to what the wire carries (in characters; a long name is cut by the rules long before this). */
    private static String clip(String s) {
        return s.length() <= MAX_TEXT_WIRE ? s : s.substring(0, MAX_TEXT_WIRE);
    }
}

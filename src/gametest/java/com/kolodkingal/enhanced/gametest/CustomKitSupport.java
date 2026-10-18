package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroJson;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Making and driving custom heroes in GameTests (3.4), for every lane's tests: a definition from JSON or from a small
 * builder, judged by the rules and become by a Survival player; a key pressed plain or with Sneak, down and up, through
 * the same entry as a client's key ({@code AbilityHandler.onKey}); ticks; a bare {@link PowerData} of any power (with a
 * custom kit) for the pure tests.
 *
 * <pre>
 * ServerPlayer p = CustomKitSupport.hero(f, 3, 3, CustomKitSupport.def()
 *         .pick("agatha.drain", 5).key(1, "agatha.drain")      // Agatha's drain on R
 *         .pick("billy.freeze", 5).key(2, "billy.freeze")      // Billy's freeze on G, his barrier on Sneak + G (forced)
 *         .build());
 * CustomKitSupport.tap(p, 1, false);                          // R
 * CustomKitSupport.press(p, 2, true, true);                   // Sneak + G down (the barrier) ... and later up:
 * CustomKitSupport.press(p, 2, false, false);                 // (the release reaches the barrier, Sneak or not)
 * </pre>
 */
public final class CustomKitSupport {

    /** A definition being built: picks, plain and Sneak bindings, and anything else through {@link #with}. */
    public static final class Def {
        private CustomHero hero = CustomHero.EMPTY.withName("Kit Tester").withColors(0x3FA9F5, 0xE0F4FF);
        private final List<CustomHero.Pick> picks = new ArrayList<>();

        /** Picks an entry (a module or a hero ability's id) at a strength, in its default option. */
        public Def pick(String id, int strength) {
            return pick(id, strength, "");
        }

        /** Picks an entry in an option (a module's option, or {@code both}/{@code main} for a hero ability). */
        public Def pick(String id, int strength, String option) {
            picks.add(new CustomHero.Pick(id, strength, option, -1));
            return this;
        }

        /** Puts {@code text} on key {@code column} (1-5, R G V B N), plain. */
        public Def key(int column, String text) {
            hero = hero.withBinding(column, false, text);
            return this;
        }

        /** Puts {@code text} on key {@code column}'s Sneak cell ({@code <id>}, {@code <module>#<option>}). */
        public Def sneak(int column, String text) {
            hero = hero.withBinding(column, true, text);
            return this;
        }

        /** Anything else of the definition (moves, animation, layers, look). */
        public Def with(java.util.function.UnaryOperator<CustomHero> change) {
            hero = change.apply(hero);
            return this;
        }

        /** The definition as written (not judged). */
        public CustomHero raw() {
            return hero.withPowers(picks);
        }

        /** The definition as the default rules judge it. */
        public CustomHero build() {
            return CustomHeroRules.sanitize(raw(), CustomHeroRules.Limits.DEFAULT).hero();
        }
    }

    public static Def def() {
        return new Def();
    }

    /** A definition from its JSON (it must read). */
    public static CustomHero json(String json) {
        CustomHeroJson.Read read = CustomHeroJson.read(json);
        check(read.ok(), "the hero's JSON reads: " + read.notes());
        return read.hero();
    }

    /** A Survival player at ({@code x}, floor, {@code z}) who becomes {@code hero} (judged by the default rules first). */
    public static ServerPlayer hero(TestKit.Fixture f, double x, double z, CustomHero hero) {
        ServerPlayer player = f.player(x, FLOOR, z);
        become(player, hero);
        return player;
    }

    /** The player becomes {@code hero}, judged by the default rules. */
    public static void become(ServerPlayer player, CustomHero hero) {
        CustomHeroes.become(player, CustomHeroRules.sanitize(hero, CustomHeroRules.Limits.DEFAULT).hero());
    }

    /** Key {@code column} (1-5) goes down or up, with Sneak held or not (Sneak stays as set until changed). */
    public static void press(ServerPlayer player, int column, boolean sneak, boolean down) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, column, down);
    }

    /** Key {@code column} pressed and released, with Sneak held or not; Sneak is let go after. */
    public static void tap(ServerPlayer player, int column, boolean sneak) {
        press(player, column, sneak, true);
        press(player, column, sneak, false);
        player.setShiftKeyDown(false);
    }

    /** {@code n} server ticks of the player's powers (nothing ticks a player without a client). */
    public static void tickN(ServerPlayer player, int n) {
        for (int i = 0; i < n; i++) {
            player.tickCount++;
            PowerTicker.tick(player);
        }
    }

    /** The player's kit (he must be a custom hero). */
    public static HeroKit kit(ServerPlayer player) {
        HeroKit kit = Powers.get(player).kit();
        check(kit != null, "a custom hero has a kit");
        return kit;
    }

    /** A bare power data of {@code power} (as loaded from a save), for pure tests. */
    public static PowerData data(Power power) {
        CompoundTag tag = new CompoundTag();
        tag.putString("power", power.getSerializedName());
        return PowerData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    /** A bare power data of a custom hero with the kit of {@code hero}. */
    public static PowerData data(CustomHero hero) {
        PowerData data = data(Power.CUSTOM);
        data.setKit(HeroKit.of(hero));
        return data;
    }

    /** The key column a hero ability sits on in the player's kit (0: none). */
    public static int column(ServerPlayer player, HeroAbility a) {
        return kit(player).column(a);
    }

    /** The strength of an entry in a definition (0 when not picked). */
    public static int strength(CustomHero hero, CatalogEntry entry) {
        CustomHero.Pick pick = hero.pick(entry);
        return pick == null ? 0 : pick.strength();
    }

    private CustomKitSupport() {
    }
}

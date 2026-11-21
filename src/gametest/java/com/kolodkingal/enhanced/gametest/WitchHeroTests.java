package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.network.HeroCreatorPayload;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.spellbook.SpellLevels;
import com.kolodkingal.enhanced.spellbook.Spellbook;
import com.kolodkingal.enhanced.spellbook.SpellbookData;
import com.kolodkingal.enhanced.spellbook.WitchIcons;
import com.kolodkingal.enhanced.spellbook.WitchLevels;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * 3.18.1, the spellbooks' witches as heroes of their own: each has a hero id ({@code ethereal_witch}), the Hero Creator's
 * "Choose a Hero" makes a player her (with the creator's checks, at her level in the book, from no power or from another
 * hero), a save of before is still known as her, and every spell any of her five levels puts on a key (the cantrips and the
 * Green witch's Root Grasp too) has its icon, as every hero's ability has. Server-safe: no client class is loaded.
 */
public final class WitchHeroTests {
    private static final String BATCH = "witch_heroes";

    private static PacketContext ctx(ServerPlayer player) {
        return new PacketContext() {
            @Override
            public net.minecraft.world.entity.player.Player player() {
                return player;
            }

            @Override
            public void enqueueWork(Runnable work) {
                work.run();
            }

            @Override
            public boolean isClientbound() {
                return false;
            }

            @Override
            public void disconnect(net.minecraft.network.chat.Component reason) {
            }
        };
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void eachWitchHasAHeroIdAndANameInBothLanguages(GameTestHelper helper) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        for (Spellbook book : Spellbook.values()) {
            check(Spellbook.byHeroId(book.heroId) == book, "her hero id names her: " + book.heroId);
            check(Power.byName(book.heroId) == null, "no hero of the mod has her id: " + book.heroId);
            check(book.heroId.length() <= 32, "her id fits the creator's message");
            ids.add(book.heroId);
        }
        check(ids.equals(new LinkedHashSet<>(Spellbook.heroIds())) && ids.size() == 3, "three witches: " + ids);
        check(Spellbook.byHeroId("GREEN_WITCH") == Spellbook.GREEN && Spellbook.byHeroId("thor") == null, "by id, any case");
        for (String name : List.of("en_us", "he_il")) {
            JsonObject lang = TestKit.lang(name);
            for (Spellbook book : Spellbook.values()) {
                check(lang.has("spellbook.enhanced." + book.id + ".caster"), name + ": her name");
            }
            check(lang.has("screen.enhanced.hero_creator.witch.desc"), name + ": the creator's words for a witch");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theCreatorsMessageCarriesAWitch(GameTestHelper helper) {
        for (Spellbook book : Spellbook.values()) {
            ByteBuf buf = Unpooled.buffer();
            HeroCreatorPayload.STREAM_CODEC.encode(buf, HeroCreatorPayload.chooseWitch(book));
            HeroCreatorPayload back = HeroCreatorPayload.STREAM_CODEC.decode(buf);
            check(back.action() == HeroCreatorPayload.CHOOSE && Spellbook.byHeroId(back.power()) == book,
                    "the message names the witch: " + back.power());
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void choosingAWitchInTheCreatorMakesHerAtHerLevel(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5, GameType.CREATIVE);
        HeroCreatorPayload.handle(HeroCreatorPayload.chooseWitch(Spellbook.GREEN), ctx(player));
        check(Powers.power(player) == Power.CUSTOM && Spellbook.of(player) == Spellbook.GREEN, "she is the Green witch");
        check(WitchLevels.level(player) == 1, "a book never begun starts at level 1: " + WitchLevels.level(player));
        check(CustomHeroes.name(player).getString().equals(Spellbook.GREEN.title().getString()), "her panel names her");
        check(CustomHero.LOOK_OWN.equals(ModAttachments.CUSTOM_HERO.get(player).look()), "she keeps her own skin");
        WitchLevels.setLevel(player, Spellbook.GREEN.id, 4);
        helper.runAfterDelay(45, () -> {
            HeroCreatorPayload.handle(HeroCreatorPayload.chooseWitch(Spellbook.ETHEREAL), ctx(player));
            check(Spellbook.of(player) == Spellbook.ETHEREAL && WitchLevels.level(player) == 1, "from one witch to another");
            check(Powers.get(player).kit() != null && Powers.get(player).kit().plain(1) != null, "her level 1 spell is on R");
            helper.runAfterDelay(45, () -> {
                HeroCreatorPayload.handle(HeroCreatorPayload.chooseWitch(Spellbook.GREEN), ctx(player));
                check(Spellbook.of(player) == Spellbook.GREEN && WitchLevels.level(player) == 4,
                        "back to the Green witch, at her level in the book");
                helper.runAfterDelay(45, () -> {
                    HeroCreatorPayload.handle(HeroCreatorPayload.choose(Power.NONE, ""), ctx(player));
                    check(Powers.power(player) == Power.NONE && Spellbook.of(player) == null, "Stop being a hero ends her");
                    helper.succeed();
                });
            });
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aHeroBecomesAWitchAndASurvivalPlayerIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(4.5, FLOOR, 4.5, GameType.CREATIVE);
        Powers.setPower(thor, Power.THOR);
        HeroCreatorPayload.handle(HeroCreatorPayload.chooseWitch(Spellbook.ELDRITCH), ctx(thor));
        check(Powers.power(thor) == Power.CUSTOM && Spellbook.of(thor) == Spellbook.ELDRITCH, "a hero chooses the Eldritch Sorcerer");
        ServerPlayer plain = f.player(12.5, FLOOR, 12.5);
        HeroCreatorPayload.handle(HeroCreatorPayload.chooseWitch(Spellbook.ETHEREAL), ctx(plain));
        check(Powers.power(plain) == Power.NONE, "a survival player without a Hero Creator is refused");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aSaveOfBeforeIsStillKnownAsHer(GameTestHelper helper) {
        for (Spellbook book : Spellbook.values()) {
            Spellbook.Level legacy = Spellbook.match(book.legacy());
            check(legacy != null && legacy.book() == book && legacy.level() == SpellLevels.MAX, "the definition of before: " + book);
            for (int level = 1; level <= SpellLevels.MAX; level++) {
                Spellbook.Level m = Spellbook.match(book.definition(level, SpellbookData.DEFAULT_HAT, true));
                check(m != null && m.book() == book && m.level() == level, book + " at level " + level);
            }
        }
        helper.succeed();
    }

    /** Whether the mod has this icon's texture. */
    private static boolean hasTexture(String key) {
        try (InputStream in = Enhanced.class.getResourceAsStream("/assets/enhanced/" + WitchIcons.texture(key))) {
            return in != null;
        } catch (IOException e) {
            return false;
        }
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everySpellOfEveryWitchHasItsIcon(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        boolean roots = false;
        Set<String> cantrips = new LinkedHashSet<>();
        for (Spellbook book : Spellbook.values()) {
            for (int level = 1; level <= SpellLevels.MAX; level++) {
                for (boolean withCantrips : new boolean[]{false, true}) {
                    CustomHero def = book.withHat(book.base(level, withCantrips), SpellbookData.DEFAULT_HAT, true);
                    HeroKit kit = HeroKit.of(def);
                    for (int c = 1; c <= CustomHero.KEYS; c++) {
                        for (HeroKit.Binding b : new HeroKit.Binding[]{kit.plain(c), kit.sneak(c)}) {
                            if (b == null) {
                                continue;
                            }
                            String key = WitchIcons.key(def, b);
                            if (key == null || !key.startsWith("ability.enhanced." + book.heroId + ".")) {
                                missing.add(book + " L" + level + " key " + c + ": no icon key (" + key + ")");
                                continue;
                            }
                            seen.add(key);
                            roots |= key.endsWith(".nature_roots");
                            if (b.module() == PowerModule.CANTRIP) {
                                cantrips.add(b.action());
                            }
                            if (!hasTexture(key)) {
                                missing.add(key);
                            }
                        }
                    }
                }
            }
            // every cantrip any level could give her (her empty keys take them by role)
            for (SpellLevels.Role role : SpellLevels.Role.values()) {
                String key = "ability.enhanced." + book.heroId + ".cantrip_" + role.cantrip;
                if (!hasTexture(key)) {
                    missing.add(key);
                }
            }
        }
        for (SpellLevels.Role role : SpellLevels.Role.values()) {
            // (the cantrips of a witch of the Demiurge's gift, or of a custom hero given them)
            if (!hasTexture("ability.enhanced." + WitchIcons.CANTRIP + "." + role.cantrip)) {
                missing.add("cantrip " + role.cantrip);
            }
        }
        check(missing.isEmpty(), "icons missing: " + missing + " (seen " + seen + ")");
        check(roots, "the Green witch's Root Grasp is among them: " + seen);
        check(!cantrips.isEmpty(), "the cantrips are among them: " + seen);
        CustomHero gift = SpellLevels.fill(CustomHero.EMPTY.withName("Gifted"), 1);
        HeroKit kit = HeroKit.of(gift);
        check(kit.plain(1) != null && kit.plain(1).module() == PowerModule.CANTRIP
                        && ("ability.enhanced.cantrip." + kit.plain(1).action()).equals(WitchIcons.key(gift, kit.plain(1))),
                "anyone else's cantrip has the cantrips' own icon");
        helper.succeed();
    }
}

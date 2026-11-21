package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.config.WitchLevelsConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.spellbook.Cantrips;
import com.kolodkingal.enhanced.spellbook.SpellLevels;
import com.kolodkingal.enhanced.spellbook.Spellbook;
import com.kolodkingal.enhanced.spellbook.SpellbookData;
import com.kolodkingal.enhanced.spellbook.Spellbooks;
import com.kolodkingal.enhanced.spellbook.WitchLevelPayload;
import com.kolodkingal.enhanced.spellbook.WitchLevels;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * 3.18.1, the witches' levels: each of a book's five levels opens the right spell on the right key at the right strengths,
 * and level 5 is the book of before; no ability of a real hero is ever above its strength 5 at any level; the book is known
 * at every level, with or without the cantrips and the hat; the cantrips fill the empty keys by the rule of roles; a caster
 * from before the levels starts at level 5; experience raises the level (within the hourly cap); the cantrips and the
 * Root Grasp work; the operators' commands; and the Demiurge's gift ({@link WitchLevels#startGift}) grows by the same
 * levels. Server-safe: no client class is loaded.
 */
public final class WitchLevelsTests {
    private static final String BATCH = "witch_levels";

    private WitchLevelsTests() {
    }

    private static void use(ServerPlayer player, Item book, boolean sneak) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(book));
        player.setShiftKeyDown(sneak);
        book.use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
    }

    private static void press(ServerPlayer player, int key, boolean sneak) {
        AbilityHandler.onKey(player, key, true, sneak);
        AbilityHandler.onKey(player, key, false, sneak);
    }

    private static Zombie zombie(GameTestHelper helper, double x, double z) {
        Zombie zombie = TestKit.spawn(helper, EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        return zombie;
    }

    /** What key {@code column} does at a level of a book (with the cantrips), as a cell: "{@code id}" or "{@code id#action}". */
    private static String cell(Spellbook book, int level, int column, boolean sneak) {
        HeroKit kit = HeroKit.of(book.base(level, true));
        HeroKit.Binding b = sneak ? kit.sneak(column) : kit.plain(column);
        if (b == null) {
            return "";
        }
        String id = b.picked().id();
        if (b.picked() != b.entry()) {
            return id; // (a variant row: by its own id)
        }
        return HeroKit.MAIN.equals(b.action()) ? id : id + "#" + b.action();
    }

    private static int strength(Spellbook book, int level, String id) {
        CustomHero.Pick pick = book.base(level, true).powers().stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
        return pick == null ? 0 : pick.strength();
    }

    // ---- what each level opens ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void eachLevelOfTheEtherealBookOpensItsSpell(GameTestHelper helper) {
        Spellbook b = Spellbook.ETHEREAL;
        check(cell(b, 1, 1, false).equals("agatha.purple_beam") && cell(b, 1, 1, true).equals("agatha.purple_beam#alt"),
                "level 1: R the Purple Beam, with the hex mark on Sneak + R");
        check(strength(b, 1, "agatha.body") == 1, "and Agatha's durable body, at 1");
        check(cell(b, 1, 2, false).startsWith("cantrip#"), "G is still a cantrip at level 1");
        check(cell(b, 2, 2, false).equals("flight"), "level 2: G flight");
        check(cell(b, 3, 3, false).equals("magic") && "orb".equals(b.base(3, true).pick(PowerModule.MAGIC).option()),
                "level 3: V the Ethereal Orb");
        check(cell(b, 4, 4, false).equals("agatha.shield"), "level 4: B the Witch's Shield");
        check(cell(b, 5, 5, false).equals("agatha.runes"), "level 5: N the Rune Trap");
        int[][] table = {{2, 3, 3, 4, 5}, {1, 2, 2, 3, 3}, {0, 2, 3, 3, 4}, {0, 0, 4, 5, 6}, {0, 0, 0, 4, 5}, {0, 0, 0, 0, 5}};
        String[] ids = {"agatha.purple_beam", "agatha.body", "flight", "magic", "agatha.shield", "agatha.runes"};
        for (int i = 0; i < ids.length; i++) {
            for (int l = 1; l <= 5; l++) {
                check(strength(b, l, ids[i]) == table[i][l - 1], ids[i] + " at level " + l + ": " + strength(b, l, ids[i]));
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void eachLevelOfTheEldritchBookOpensItsSpell(GameTestHelper helper) {
        Spellbook b = Spellbook.ELDRITCH;
        check(cell(b, 1, 2, false).equals("magic") && cell(b, 1, 1, false).startsWith("cantrip#"), "level 1: G eldritch bolts");
        check(cell(b, 2, 1, false).equals("strange.whip") && cell(b, 2, 1, true).equals("strange.whip#alt"),
                "level 2: R the whip, Sneak + R the grapple");
        check(cell(b, 3, 3, false).equals("strange.shields"), "level 3: V the Shields of the Seraphim");
        check(cell(b, 4, 5, false).equals("strange.portal"), "level 4: N the Sling Ring's gate");
        check(cell(b, 4, 4, false).startsWith("cantrip#"), "B still a cantrip at level 4");
        check(cell(b, 5, 4, false).equals("strange.mirror") && cell(b, 5, 4, true).equals("strange.astral"),
                "level 5: B the Mirror Dimension, Sneak + B the astral form");
        check(strength(b, 1, "magic") == 2 && strength(b, 2, "strange.whip") == 3 && strength(b, 5, "magic") == 5, "its strengths");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void eachLevelOfTheGreenBookOpensItsSpellAndLevelFiveTheRootGrasp(GameTestHelper helper) {
        Spellbook b = Spellbook.GREEN;
        check(cell(b, 1, 1, false).equals("earth") && strength(b, 1, "healing") == 2, "level 1: R thrown earth, and the healing");
        check(cell(b, 1, 1, true).isEmpty(), "no Earth Wall yet");
        check(cell(b, 2, 2, false).equals("nature"), "level 2: G growth");
        check(cell(b, 3, 1, true).equals("earth#wall"), "level 3: Sneak + R the Earth Wall");
        check(cell(b, 4, 3, false).equals("agatha.runes") && strength(b, 4, "agatha.runes") == 2, "level 4: V the runes, at 2");
        check(cell(b, 5, 4, false).equals("nature#roots"), "level 5: B the Root Grasp (new)");
        check(cell(b, 5, 5, false).equals("cantrip#ward"), "N stays a cantrip: the mini shield");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void levelFiveIsTheBookOfBefore(GameTestHelper helper) {
        for (Spellbook book : List.of(Spellbook.ETHEREAL, Spellbook.ELDRITCH)) {
            CustomHero five = book.base(5, true);
            CustomHero before = book.legacy();
            check(five.powers().equals(before.powers()) && five.keys().equals(before.keys()) && five.sneakKeys().equals(before.sneakKeys()),
                    book + ": level 5 is exactly the book of before");
        }
        CustomHero green = Spellbook.GREEN.base(5, false);
        check(green.powers().equals(Spellbook.GREEN.legacy().powers())
                        && green.withBinding(4, false, "").keys().equals(Spellbook.GREEN.legacy().keys()),
                "the Green book's level 5 is the book of before and the Root Grasp");
        helper.succeed();
    }

    // ---- never stronger than the real hero ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void noAbilityOfARealHeroIsEverAboveItsStrengthFive(GameTestHelper helper) {
        for (Spellbook book : Spellbook.values()) {
            for (int level = 1; level <= 5; level++) {
                for (boolean cantrips : new boolean[]{false, true}) {
                    for (CustomHero.Pick pick : book.base(level, cantrips).powers()) {
                        if (pick.entry() instanceof HeroAbility a) {
                            check(pick.strength() <= HeroAbility.FIXED_STRENGTH && HeroKit.factor(pick.strength()) <= 1.0F,
                                    book + " level " + level + ": " + a + " at " + pick.strength());
                        }
                    }
                }
            }
        }
        // Agatha's own numbers, strength 1 to 5: 60% to 100% of her
        check(Math.abs(HeroKit.factor(1) - 0.6F) < 1.0E-4F && Math.abs(HeroKit.factor(5) - 1.0F) < 1.0E-4F, "60% at 1, 100% at 5");
        check(Spellbook.ETHEREAL.base(5, true).pick(HeroAbility.AGATHA_BODY).strength() == 3, "the witch's body is 80% of Agatha's");
        CustomHero strong = CustomHero.EMPTY.withName("Strong").withPowers(List.of(new CustomHero.Pick("agatha.purple_beam", 10, "both", -1),
                new CustomHero.Pick("flight", 9, "hover", -1))).withKeys(List.of("agatha.purple_beam", "flight", "", "", ""));
        for (int level = 1; level <= 5; level++) {
            CustomHero at = SpellLevels.giftLevel(strong, level);
            check(at.pick(HeroAbility.AGATHA_PURPLE_BEAM) == null || at.pick(HeroAbility.AGATHA_PURPLE_BEAM).strength() <= 5,
                    "a gift's ability is capped at level " + level);
        }
        helper.succeed();
    }

    // ---- the book is known at every level ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBookIsKnownAtEveryLevel(GameTestHelper helper) {
        for (Spellbook book : Spellbook.values()) {
            for (int level = 1; level <= 5; level++) {
                for (boolean cantrips : new boolean[]{false, true}) {
                    for (boolean hat : new boolean[]{false, true}) {
                        CustomHero def = book.withHat(book.base(level, cantrips), 0x123456, hat);
                        Spellbook.Level match = Spellbook.match(def);
                        check(match != null && match.book() == book && match.level() == level,
                                book + " level " + level + " (cantrips " + cantrips + ", hat " + hat + "): " + match);
                    }
                }
            }
            check(Spellbook.of(book.legacy()) == book, book + "'s definition of before is known");
        }
        CustomHero mine = CustomHeroRules.sanitize(CustomHero.EMPTY.withName("Mine").withPowers(List.of(
                new CustomHero.Pick("flight", 3, "jet", -1))).withKeys(List.of("flight", "", "", "", "")), CustomHeroRules.Limits.DEFAULT).hero();
        check(Spellbook.of(mine) == null, "a hero of one's own making is no book");
        helper.succeed();
    }

    // ---- the cantrips ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theCantripsFillTheEmptyKeysByTheirRoles(GameTestHelper helper) {
        check(List.of(cell(Spellbook.ETHEREAL, 1, 2, false), cell(Spellbook.ETHEREAL, 1, 3, false), cell(Spellbook.ETHEREAL, 1, 4, false),
                        cell(Spellbook.ETHEREAL, 1, 5, false)).equals(List.of("cantrip#ward", "cantrip#blink", "cantrip#mend", "cantrip#repel")),
                "Ethereal, level 1: G mini shield, V blink step, B mend, N repel");
        check(List.of(cell(Spellbook.ELDRITCH, 1, 1, false), cell(Spellbook.ELDRITCH, 1, 3, false), cell(Spellbook.ELDRITCH, 1, 4, false),
                        cell(Spellbook.ELDRITCH, 1, 5, false)).equals(List.of("cantrip#ward", "cantrip#blink", "cantrip#mend", "cantrip#repel")),
                "Eldritch, level 1: R mini shield, V blink step, B mend, N repel");
        check(List.of(cell(Spellbook.GREEN, 1, 2, false), cell(Spellbook.GREEN, 1, 3, false), cell(Spellbook.GREEN, 1, 4, false),
                        cell(Spellbook.GREEN, 1, 5, false)).equals(List.of("cantrip#ward", "cantrip#blink", "cantrip#repel", "cantrip#light")),
                "Green, level 1: G mini shield, V blink step, B repel, N light (her healing is the healing)");
        for (Spellbook book : Spellbook.values()) {
            for (int level = 1; level <= 5; level++) {
                check(!book.base(level, false).has(PowerModule.CANTRIP), book + " without the cantrips has none");
                CustomHero.Pick pick = book.base(level, true).pick(PowerModule.CANTRIP);
                check(pick == null || pick.strength() == level, "a cantrip grows with the level");
                for (int c = 1; c <= 5; c++) {
                    check(!book.base(level, true).keys().get(c - 1).isEmpty(), book + " level " + level + ": no key is empty");
                }
            }
        }
        check(PowerModule.CANTRIP.hidden() && CustomHeroRules.sanitize(CustomHero.EMPTY.withName("X").withPowers(List.of(
                        new CustomHero.Pick("cantrip", 3, "spark", -1))).withKeys(List.of("cantrip", "", "", "", "")),
                CustomHeroRules.Limits.DEFAULT).hero().powers().isEmpty(), "the Hero Creator cannot pick the cantrips");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theCantripsWork(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(4.5, FLOOR, 10.5);
        use(player, ModItems.BOOK_OF_ETHEREAL_WITCHCRAFT.get(), false);
        check(WitchLevels.level(player) == 1, "a new witch is at level 1");
        press(player, 2, false);
        check(Cantrips.warded(player) && player.getAbsorptionAmount() >= 6.0F, "G: the mini shield");
        player.hurt(player.damageSources().generic(), 3.0F);
        player.setHealth(10.0F);
        press(player, 4, false);
        check(Cantrips.mending(player), "B: mend");
        Zombie zombie = zombie(helper, 6.5, 10.5);
        float before = zombie.getHealth();
        press(player, 5, false);
        check(zombie.getHealth() < before, "N: repel hurts the zombie near her");
        double x = player.getX();
        TestKit.look(player, TestKit.EAST, 0.0F);
        player.moveTo(player.getX(), player.getY(), player.getZ() + 3, TestKit.EAST, 0.0F);
        press(player, 3, false);
        check(Math.abs(player.getX() - x) > 2.0, "V: the blink step: " + (player.getX() - x));
        helper.succeed();
    }

    // ---- levels from experience ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anExistingCasterStartsAtLevelFive(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Spellbook book : Spellbook.values()) {
            ServerPlayer player = f.player(8.5, FLOOR, 8.5);
            CustomHeroes.become(player, book.legacy(), false);
            ModAttachments.SPELLBOOK.set(player, SpellbookData.EMPTY);
            check(Spellbook.of(player) == book, book + ": an older save's caster is known");
            WitchLevels.onLogin(player);
            check(WitchLevels.level(player) == 5 && ModAttachments.SPELLBOOK.get(player).xp(book.id) == 850,
                    book + ": she starts at level 5, written down");
            CustomHero now = ModAttachments.CUSTOM_HERO.get(player);
            if (book == Spellbook.GREEN) {
                check(Powers.get(player).kit().plain(4) != null && "roots".equals(Powers.get(player).kit().plain(4).action()),
                        "the Green witch gains the Root Grasp");
            } else {
                check(now.powers().equals(book.legacy().powers()) && now.keys().equals(book.legacy().keys()), book + ": nothing changes for her");
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void experienceRaisesTheLevelAndOpensItsSpell(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        use(player, ModItems.BOOK_OF_ETHEREAL_WITCHCRAFT.get(), false);
        check(WitchLevels.level(player) == 1 && Powers.get(player).kit().plain(2).module() == PowerModule.CANTRIP, "level 1: no flight yet");
        check(WitchLevels.gain(player, 99, false) == 99 && WitchLevels.level(player) == 1, "99 points: still level 1");
        WitchLevels.gain(player, 1, false);
        check(WitchLevels.level(player) == 2 && Powers.get(player).kit().plain(2).module() == PowerModule.FLIGHT,
                "100 points: level 2, flight on G");
        check(Spellbooks.wearsHat(player), "her hat stays on");
        WitchLevels.gain(player, 5000, false);
        check(WitchLevels.level(player) == 5 && WitchLevels.xp(player) == 850, "level 5 is the end of the way: 850");
        // the hourly cap: hits and kills give at most 600 an hour
        ServerPlayer other = f.player(30.5, FLOOR, 30.5);
        use(other, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        int total = 0;
        for (int i = 0; i < 100; i++) {
            total += WitchLevels.gain(other, 15, true);
        }
        check(total == 600, "the hourly cap: " + total);
        check(WitchLevels.gain(other, 60, false) == 60, "study is not in the cap");
        // a book given up and taken again keeps its level
        int level = WitchLevels.level(other);
        use(other, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), true);
        check(Powers.power(other) == Power.NONE, "given up");
        use(other, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        check(WitchLevels.level(other) == level, "back at her level: " + level);
        // the levels off: everyone has all of the book
        f.config(WitchLevelsConfig.ENABLED, false);
        ServerPlayer third = f.player(20.5, FLOOR, 8.5);
        use(third, ModItems.BOOK_OF_ELDRITCH_WITCHCRAFT.get(), false);
        check(WitchLevels.level(third) == 5 && Powers.get(third).kit().plain(5).ability() == HeroAbility.STRANGE_PORTAL,
                "with the levels off, a new caster has all of the book");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void killsAndSpellsGiveExperience(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(4.5, FLOOR, 10.5);
        use(player, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        Zombie zombie = zombie(helper, 8.5, 10.5);
        net.minecraft.world.damagesource.DamageSource spell = ModDamageTypes.source(player.level(), ModDamageTypes.HERO_MAGIC, player, player);
        WitchLevels.onDamage(zombie, spell);
        check(WitchLevels.xp(player) == WitchLevels.CAST_XP, "a spell that hits: 2");
        WitchLevels.onDamage(zombie, spell);
        check(WitchLevels.xp(player) == WitchLevels.CAST_XP, "the same spell again at once: nothing");
        WitchLevels.onDeath(zombie, spell);
        check(WitchLevels.xp(player) == WitchLevels.CAST_XP + WitchLevels.KILL_XP, "a kill by a spell: 5");
        WitchLevels.onDeath(zombie, player.damageSources().playerAttack(player));
        check(WitchLevels.xp(player) == WitchLevels.CAST_XP + WitchLevels.KILL_XP, "a punch is no spell");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theRootGraspHoldsAnEnemy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(4.5, FLOOR, 10.5);
        use(player, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        WitchLevels.setLevel(player, Spellbook.GREEN.id, 5);
        Zombie zombie = zombie(helper, 10.5, 10.5);
        TestKit.lookAt(player, zombie);
        press(player, 4, false);
        check(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "B: the roots hold the zombie");
        check(zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() <= 41, "for 2 seconds: "
                + zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theOperatorsCommands(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        CommandSourceStack self = player.createCommandSourceStack().withPermission(4);
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(self, "enhanced spellbook level @s eldritch 3");
        check(Spellbook.of(player) == Spellbook.ELDRITCH && WitchLevels.level(player) == 3, "level @s eldritch 3");
        server.getCommands().performPrefixedCommand(self, "enhanced spellbook xp @s add 250");
        check(WitchLevels.xp(player) == 500 && WitchLevels.level(player) == 4, "xp add 250: " + WitchLevels.xp(player));
        server.getCommands().performPrefixedCommand(self, "enhanced spellbook xp @s set 0");
        check(WitchLevels.level(player) == 1, "xp set 0: level 1");
        helper.succeed();
    }

    // ---- the Demiurge's gift ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aWitchOfTheDemiurgesGiftGrowsByTheSameLevels(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        // a fake gift: Agatha's beam (asked at 10), flight, Magic's bolts and a durable body
        CustomHero gift = CustomHeroRules.sanitize(CustomHero.EMPTY.withName("Gifted").withColors(0x33CCFF, 0xCCF2FF).withPowers(List.of(
                        new CustomHero.Pick("agatha.purple_beam", 10, "both", -1), new CustomHero.Pick("flight", 6, "hover", -1),
                        new CustomHero.Pick("magic", 8, "bolts", -1), new CustomHero.Pick("durability", 4, "armor", -1)))
                .withKeys(List.of("agatha.purple_beam", "flight", "magic", "", "")), CustomHeroRules.Limits.DEFAULT).hero();
        WitchLevels.startGift(player, gift);
        WitchLevels.Source source = WitchLevels.source(player);
        check(source != null && source.kind() == WitchLevels.Kind.GIFT, "she is a witch of the gift");
        check(WitchLevels.level(player) == 1, "at level 1");
        CustomHero now = ModAttachments.CUSTOM_HERO.get(player);
        HeroKit kit = Powers.get(player).kit();
        check(kit.plain(1) != null && kit.plain(1).ability() == HeroAbility.AGATHA_PURPLE_BEAM, "level 1: the gift's first key");
        check(now.pick(PowerModule.DURABILITY) != null, "and its power that needs no key");
        check(kit.plain(2).module() == PowerModule.CANTRIP && kit.plain(3).module() == PowerModule.CANTRIP, "the rest are cantrips");
        check(now.pick(HeroAbility.AGATHA_PURPLE_BEAM).strength() <= 5, "never above the real hero");
        WitchLevelPayload payload = WitchLevels.payload(player);
        check(payload.source() == 4 && payload.level() == 1, "her client is told: the gift, level 1");
        WitchLevels.setLevel(player, WitchLevels.GIFT_KEY, 3);
        kit = Powers.get(player).kit();
        check(WitchLevels.level(player) == 3 && kit.plain(2).module() == PowerModule.FLIGHT
                && kit.plain(3).module() == PowerModule.CANTRIP, "level 3: two of its three keys");
        WitchLevels.setLevel(player, WitchLevels.GIFT_KEY, 5);
        now = ModAttachments.CUSTOM_HERO.get(player);
        kit = Powers.get(player).kit();
        check(kit.plain(3).module() == PowerModule.MAGIC && now.pick(PowerModule.MAGIC).strength() == 8, "level 5: the full gift");
        check(now.pick(HeroAbility.AGATHA_PURPLE_BEAM).strength() == 5, "its beam at the real hero's strength 5");
        check(WitchLevels.gain(player, 10, false) == 0 || WitchLevels.xp(player) == 850, "the way ends at 850");
        // a book refuses her, but opens her Book of Spells
        use(player, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        check(WitchLevels.source(player) != null && WitchLevels.source(player).kind() == WitchLevels.Kind.GIFT, "she stays the gift's witch");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyWitchLevelWordIsInBothLanguages(GameTestHelper helper) throws IOException {
        for (String name : List.of("en_us", "he_il")) {
            JsonObject lang = TestKit.lang(name);
            for (String id : List.of("ethereal", "eldritch", "green", "gift")) {
                for (int l = 1; l <= 5; l++) {
                    check(lang.has("spellbook.enhanced." + id + ".level." + l), name + ": " + id + " level " + l);
                }
            }
            for (PowerModule.Option o : PowerModule.CANTRIP.options()) {
                check(lang.has(PowerModule.CANTRIP.optionKey(o.id())), name + ": cantrip " + o.id());
            }
            for (String key : List.of("power_module.enhanced.nature.roots", "message.enhanced.witch_levels.level_title",
                    "message.enhanced.witch_levels.new_spell", "message.enhanced.witch_levels.mastered", "screen.enhanced.spells.title",
                    "hud.enhanced.witch_levels.level", "hud.enhanced.witch_levels.locked", "commands.enhanced.spellbook.level",
                    "commands.enhanced.spellbook.xp_add", "commands.enhanced.spellbook.xp_set", "message.enhanced.witch_levels.study")) {
                check(lang.has(key), name + ": " + key);
            }
        }
        helper.succeed();
    }
}

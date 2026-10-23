package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosClock;
import com.kolodkingal.enhanced.thanos.ThanosDefence;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosFight;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Mad Titan, the boss: his data, his rules of defence, his clock of stones, his plates and his poise, and then every
 * move of his, his immunities, his death and his reward. Every fight test has a batch of its own (a fight is server-wide
 * state) and takes {@link TestKit#bossFixture}. A test drives the fight by the hooks the entity offers ({@code
 * forceStones}, {@code forceMove}, {@code forceCombatTicks}, {@code setPassive}) instead of waiting for it.
 */
public final class ThanosBossTests {
    /** The middle of the floor: Thanos stands here, and the arena is round it. */
    static final Vec3 CENTRE = new Vec3(10.5, FLOOR, 10.5);

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A fixture with the announcements off and a small arena (the real one would load a hundred chunks of the grid). */
    static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.config(ThanosBossConfig.ARENA_RADIUS, 16);
        return f;
    }

    /** Thanos standing in the middle of the floor, risen from his arrival and passive: he chooses nothing by himself. */
    static ThanosEntity boss(TestKit.Fixture f, ServerPlayer starter) {
        ThanosFight fight = ThanosFight.spawnAt(f.level(), f.helper().absoluteVec(CENTRE), starter);
        ThanosEntity boss = fight.boss();
        check(boss != null, "the fight made him");
        boss.forceArrived();
        boss.setPassive(true);
        return boss;
    }

    /** A blow of a player's hand, with the invulnerability of the last one forgotten. */
    static boolean hit(ThanosEntity boss, ServerPlayer player, float amount) {
        boss.invulnerableTime = 0;
        return boss.hurt(player.damageSources().playerAttack(player), amount);
    }

    static boolean hit(ThanosEntity boss, DamageSource source, float amount) {
        boss.invulnerableTime = 0;
        return boss.hurt(source, amount);
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?[sd]");

    private static List<String> placeholders(String text) {
        List<String> found = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

    private static JsonObject lang(IModFile mod, String name) throws IOException {
        Path file = mod.findResource("assets", Enhanced.MODID, "lang", name + ".json");
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    // ------------------------------------------------------------------------------------------------------------
    // A. Data and registries

    /** Every key the fight uses is in both languages with the same placeholders. */
    @EnhancedTest(template = ARENA, batch = "thanos_data")
    public static void everyThanosKeyIsInBothLanguagesWithTheSamePlaceholders(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        JsonObject en = lang(mod, "en_us");
        JsonObject he = lang(mod, "he_il");
        List<String> keys = new ArrayList<>(List.of("boss.enhanced.thanos", "boss.enhanced.thanos.enraged", "boss.enhanced.thanos.five",
                "boss.enhanced.thanos.next", "message.enhanced.thanos.stone.subtitle", "message.enhanced.thanos.stone.soon",
                "message.enhanced.thanos.summon.title", "message.enhanced.thanos.summon.subtitle", "message.enhanced.thanos.summon.charging",
                "message.enhanced.thanos.summon.need_both", "message.enhanced.thanos.summon.no_sky", "message.enhanced.thanos.summon.no_room",
                "message.enhanced.thanos.summon.wrong_dimension", "message.enhanced.thanos.summon.peaceful", "message.enhanced.thanos.summon.active",
                "message.enhanced.thanos.summon.resting", "message.enhanced.thanos.summon.disabled", "message.enhanced.thanos.coming",
                "message.enhanced.thanos.left", "message.enhanced.thanos.last_words", "message.enhanced.thanos.reward",
                "message.enhanced.thanos.loki", "message.enhanced.thanos.already_powered", "item.enhanced.tesseract.desc4",
                "item.enhanced.lokis_scepter.desc4", "entity.enhanced.thanos", "entity.enhanced.thanos_sword", "entity.enhanced.thanos_meteor",
                "entity.enhanced.thanos_orb", "entity.enhanced.thanos_phantom", "effect.enhanced.petrified", "effect.enhanced.soulworld",
                "effect.enhanced.time_locked", "item.enhanced.infinity_gauntlet"));
        for (StoneKind stone : StoneKind.values()) {
            keys.add("stone.enhanced." + stone.id());
        }
        for (String key : keys) {
            check(en.has(key) && he.has(key), key + " is in both languages");
            check(placeholders(en.get(key).getAsString()).equals(placeholders(he.get(key).getAsString())), key + " has the same placeholders in both");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "thanos_data")
    public static void thanosTagsAndDamageTypesAreWhatTheFightReads(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        EntityType<?> thanos = ModEntities.THANOS.get();
        for (var tag : List.of(ModTags.HEX_RESISTANT, ModTags.PIN_IMMUNE, ModTags.PULL_IMMUNE, ModTags.SHAPESHIFT_IMMUNE,
                ModTags.SPELLBOUND_IMMUNE, ModTags.CONFUSED_IMMUNE, ModTags.BOSSES, ModTags.TELEPATHY_IMMUNE)) {
            check(thanos.is(tag), "Thanos is in " + tag.location());
        }
        check(!thanos.is(ModTags.TELEKINESIS_IMMUNE), "telekinesis holds Thanos as a boss: he is not in telekinesis_immune");
        for (EntityType<?> small : List.of(ModEntities.THANOS_SWORD.get(), ModEntities.THANOS_METEOR.get(), ModEntities.THANOS_ORB.get())) {
            check(small.is(ModTags.PULL_IMMUNE) && small.is(ModTags.SHAPESHIFT_IMMUNE) && small.is(ModTags.REALITY_PROOF),
                    EntityType.getKey(small) + " cannot be pulled, copied or bubbled");
            check(!small.is(ModTags.TELEKINESIS_IMMUNE), EntityType.getKey(small) + ": Billy and the Phoenix catch it; the villains' pulls do not");
        }
        check(ModEntities.THANOS_PHANTOM.get().is(ModTags.SHAPESHIFT_IMMUNE) && ModEntities.THANOS_PHANTOM.get().is(ModTags.BOSS_MINIONS)
                && !ModEntities.THANOS_PHANTOM.get().is(ModTags.TELEKINESIS_IMMUNE), "a phantom is a minion that Wanda can lift");
        var damageTypes = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (var type : List.of(ModDamageTypes.THANOS_BLOW, ModDamageTypes.THANOS_SWORD, ModDamageTypes.STONE_POWER, ModDamageTypes.STONE_BLAST,
                ModDamageTypes.STONE_SOUL)) {
            check(damageTypes.getHolder(type).isPresent(), type.location() + " exists");
        }
        var soul = damageTypes.getHolderOrThrow(ModDamageTypes.STONE_SOUL);
        check(soul.is(DamageTypeTags.BYPASSES_ARMOR) && soul.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) && soul.is(DamageTypeTags.BYPASSES_COOLDOWN),
                "the Soul Stone ignores armour, enchantments and the damage cooldown");
        check(damageTypes.getHolderOrThrow(ModDamageTypes.STONE_BLAST).is(DamageTypeTags.IS_EXPLOSION), "a stone blast is an explosion");
        for (var type : List.of(ModDamageTypes.THANOS_BLOW, ModDamageTypes.THANOS_SWORD)) {
            var holder = damageTypes.getHolderOrThrow(type);
            check(holder.is(ModTags.MELEE_BLOWS) && holder.is(DamageTypeTags.BYPASSES_ENCHANTMENTS), type.location() + " is a melee blow that Protection does not soften");
        }
        for (var type : List.of(ModDamageTypes.CHAOS_BEAM, ModDamageTypes.MIND_STONE_BEAM, ModDamageTypes.STORMBREAKER)) {
            check(damageTypes.getHolderOrThrow(type).is(ModTags.STONE_BORN), type.location() + " is stone-born");
        }
        helper.succeed();
    }

    /** Every number of the config lies in its range, and no sixth stone can be asked for. */
    @EnhancedTest(template = ARENA, batch = "thanos_data")
    public static void thanosConfigDefaultsAreInRangeAndThereIsNoSixthStone(GameTestHelper helper) throws IllegalAccessException {
        int checked = 0;
        for (Class<?> config : List.of(ThanosBossConfig.class, ThanosConfig.class)) {
            for (Field field : config.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                ModConfigSpec.ConfigValue<?> value = (ModConfigSpec.ConfigValue<?>) field.get(null);
                check(value != null, config.getSimpleName() + "." + field.getName() + " is defined");
                Object spec = EnhancedConfig.SPEC.getSpec().get(value.getPath());
                check(spec instanceof ModConfigSpec.ValueSpec valueSpec && valueSpec.test(value.getDefault()),
                        config.getSimpleName() + "." + field.getName() + " has a default in its range");
                checked++;
            }
        }
        check(checked >= 139, "all the numbers are there (" + checked + ")");
        ModConfigSpec.ValueSpec stones = (ModConfigSpec.ValueSpec) EnhancedConfig.SPEC.getSpec().get(ThanosBossConfig.MAX_STONES.getPath());
        check(stones.test(5) && !stones.test(6), "maxStones ends at five: there is no sixth stone to ask for");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "thanos_data")
    public static void theMindStoneIsNeverGranted(GameTestHelper helper) {
        check(StoneKind.values().length == 6, "six stones exist");
        check(StoneKind.granted().equals(List.of(StoneKind.POWER, StoneKind.SPACE, StoneKind.REALITY, StoneKind.SOUL, StoneKind.TIME)),
                "five are granted, in the film's order");
        for (ThanosMove move : ThanosMove.values()) {
            check(move.stone() != StoneKind.MIND, move + " does not draw on the Mind Stone");
            check(!move.name().contains("SNAP"), move + " is no Snap");
        }
        check(StoneKind.nth(6) == null && StoneKind.nth(5) == StoneKind.TIME, "the sixth is nobody's");
        check(ThanosClock.stonesAt(Long.MAX_VALUE / 4) == 5, "no length of fight gives a sixth");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The damage pipeline (pure)

    private static ThanosDefence.Stance stance(ThanosState state, boolean barrier, int plates, boolean locked, float taken) {
        return new ThanosDefence.Stance(state, barrier, plates, locked, 3000.0F, taken);
    }

    private static final ThanosDefence.Hit PLAIN = new ThanosDefence.Hit(false, false, false, false, false);

    @EnhancedTest(template = ARENA, batch = "thanos_defence")
    public static void theDamagePipelineTakesTheStepsInOrder(GameTestHelper helper) {
        ThanosDefence.Stance bare = stance(ThanosState.IDLE, false, 0, false, 0.0F);
        check(Math.abs(ThanosDefence.damageTaken(PLAIN, bare, 50.0F) - 50.0F) < 1.0E-3, "a plain hit is what it is");
        // (1) a creature that is not a player does a quarter
        ThanosDefence.Hit mob = new ThanosDefence.Hit(false, true, false, false, false);
        check(Math.abs(ThanosDefence.damageTaken(mob, bare, 40.0F) - 10.0F) < 1.0E-3, "a golem does x0.25");
        // (3) each plate takes a tenth off
        for (int plates = 0; plates <= 4; plates++) {
            int mask = new int[]{0, ThanosDefence.HELMET, ThanosDefence.HELMET | ThanosDefence.R_PAULDRON,
                    ThanosDefence.HELMET | ThanosDefence.R_PAULDRON | ThanosDefence.L_PAULDRON, ThanosDefence.ALL_PLATES}[plates];
            float expected = 100.0F * (1.0F - 0.1F * plates);
            check(Math.abs(ThanosDefence.damageTaken(PLAIN, stance(ThanosState.IDLE, false, mask, false, 0.0F), 100.0F) - expected) < 1.0E-3,
                    plates + " plates leave " + expected);
        }
        // (4) the surge, a stagger and a locked Gauntlet open him up
        check(Math.abs(ThanosDefence.damageTaken(PLAIN, stance(ThanosState.SURGE, false, 0, false, 0.0F), 40.0F) - 60.0F) < 1.0E-3, "the surge takes x1.5");
        check(Math.abs(ThanosDefence.damageTaken(PLAIN, stance(ThanosState.STAGGERED, false, 0, false, 0.0F), 40.0F) - 50.0F) < 1.0E-3, "a stagger takes x1.25");
        check(Math.abs(ThanosDefence.damageTaken(PLAIN, stance(ThanosState.IDLE, false, 0, true, 0.0F), 40.0F) - 50.0F) < 1.0E-3, "a locked Gauntlet takes x1.25");
        // (5) Stormbreaker
        ThanosDefence.Hit storm = new ThanosDefence.Hit(false, false, true, true, true);
        check(Math.abs(ThanosDefence.damageTaken(storm, bare, 40.0F) - 100.0F) < 1.0E-3, "Stormbreaker does x2.5");
        check(Math.abs(ThanosDefence.damageTaken(storm, stance(ThanosState.SURGE, false, 0, false, 0.0F), 40.0F) - 150.0F) < 1.0E-3, "and the surge adds to it");
        // (6) one hit takes at most 6 percent, one second at most 15
        check(Math.abs(ThanosDefence.damageTaken(PLAIN, bare, 5000.0F) - 180.0F) < 1.0E-3, "no hit takes more than 6 percent of 3000");
        check(Math.abs(ThanosDefence.damageTaken(PLAIN, stance(ThanosState.IDLE, false, 0, false, 400.0F), 5000.0F) - 50.0F) < 1.0E-3,
                "no second takes more than 15 percent: 450 less the 400 already taken");
        check(ThanosDefence.damageTaken(PLAIN, stance(ThanosState.IDLE, false, 0, false, 450.0F), 100.0F) < 0.01F, "and then nothing more that second");
        // (0) nothing when arriving or dying, unless it cannot be stopped
        check(ThanosDefence.damageTaken(PLAIN, stance(ThanosState.ARRIVING, false, 0, false, 0.0F), 100.0F) == 0.0F, "nothing while he arrives");
        check(ThanosDefence.damageTaken(PLAIN, stance(ThanosState.DYING, false, 0, false, 0.0F), 100.0F) == 0.0F, "nothing while he dies");
        ThanosDefence.Hit kill = new ThanosDefence.Hit(true, false, false, false, false);
        check(ThanosDefence.damageTaken(kill, stance(ThanosState.DYING, true, ThanosDefence.ALL_PLATES, true, 9999.0F), 1.0E6F) == 1.0E6F,
                "/kill and the void always go through, uncapped");
        // (2) the Barrier turns away ranged damage that is not stone-born
        ThanosDefence.Stance barrier = stance(ThanosState.IDLE, true, 0, false, 0.0F);
        check(ThanosDefence.damageTaken(new ThanosDefence.Hit(false, false, true, false, false), barrier, 30.0F) == 0.0F, "the Barrier stops an arrow");
        check(ThanosDefence.damageTaken(new ThanosDefence.Hit(false, false, true, true, false), barrier, 30.0F) == 30.0F,
                "but not a stone-born beam");
        check(ThanosDefence.damageTaken(PLAIN, barrier, 30.0F) == 30.0F, "and not a blow of the hand");
        // the poise
        check(Math.abs(ThanosDefence.poise(10.0F, false) - 20.0F) < 1.0E-3 && Math.abs(ThanosDefence.poise(10.0F, true) - 60.0F) < 1.0E-3,
                "poise is x2, and x3 more for stone-born damage");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "thanos_defence")
    public static void platesBreakWhereTheHealthCrossesTheirThresholds(GameTestHelper helper) {
        check(ThanosDefence.crossed(0.80F, 0.70F) == ThanosDefence.R_PAULDRON, "below 75 percent the right pauldron goes");
        check(ThanosDefence.crossed(0.60F, 0.50F) == ThanosDefence.L_PAULDRON, "below 55 the left");
        check(ThanosDefence.crossed(0.40F, 0.30F) == ThanosDefence.BREASTPLATE, "below 35 the breastplate");
        check(ThanosDefence.crossed(0.25F, 0.10F) == ThanosDefence.HELMET, "below 20 the helmet");
        check(ThanosDefence.crossed(0.80F, 0.10F) == ThanosDefence.ALL_PLATES, "a great fall takes them all");
        check(ThanosDefence.crossed(0.70F, 0.65F) == 0, "a plate whose threshold is already behind is not asked again");
        check(ThanosDefence.firstIntact(ThanosDefence.ALL_PLATES, 0) == ThanosDefence.R_PAULDRON && ThanosDefence.firstIntact(0, 3) == 0,
                "a tear takes an intact plate, if there is one");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // C. The boss

    @EnhancedTest(template = ARENA, batch = "thanos_fight_health")
    public static void healthScalesWithTheFightersAndKeepsItsFraction(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(6.5, FLOOR, 6.5);
        ThanosEntity boss = boss(f, a);
        f.player(14.5, FLOOR, 6.5);
        f.player(6.5, FLOOR, 14.5, GameType.CREATIVE);
        f.player(14.5, FLOOR, 14.5, GameType.SPECTATOR);
        helper.runAfterDelay(3, () -> {
            check(boss.mortals().size() == 2, "Creative and Spectator players are not fighters: " + boss.mortals().size());
            boss.rescaleToFighters();
            check(Math.abs(boss.maxPoints() - 4500.0) < 0.5, "two fighters: 4500, not " + boss.maxPoints());
            boss.setHealth(boss.getMaxHealth() * 0.5F);
            f.player(8.5, FLOOR, 8.5);
            f.player(12.5, FLOOR, 8.5);
            helper.runAfterDelay(3, () -> {
                check(boss.mortals().size() == 4, "four fighters now: " + boss.mortals().size());
                boss.rescaleToFighters();
                check(Math.abs(boss.maxPoints() - 7500.0) < 0.5, "four fighters: 7500");
                check(Math.abs(boss.points() / boss.maxPoints() - 0.5) < 0.01, "the fraction he has left is kept");
                f.player(9.5, FLOOR, 9.5);
                helper.runAfterDelay(3, () -> {
                    boss.rescaleToFighters();
                    check(Math.abs(boss.maxPoints() - 7500.0) < 0.5, "a fifth fighter is not counted: 7500");
                    helper.succeed();
                });
            });
        });
    }

    @EnhancedTest(template = ARENA, batch = "thanos_fight_clock")
    public static void theClockAddsTheStonesInOrderAndNeverASixth(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(6.5, FLOOR, 6.5);
        ThanosEntity boss = boss(f, a);
        check(boss.stones() == 1 && boss.hasStone(StoneKind.POWER) && !boss.hasStone(StoneKind.SPACE), "he starts with the Power Stone");
        StoneKind[] order = {StoneKind.POWER, StoneKind.SPACE, StoneKind.REALITY, StoneKind.SOUL, StoneKind.TIME};
        for (int n = 1; n <= 4; n++) {
            boss.forceCombatTicks(1800L * n);
            check(boss.stones() == n + 1, "after " + n + " intervals he has " + (n + 1) + ", not " + boss.stones());
            check(boss.newestStone() == order[n], "the newest is " + order[n]);
        }
        boss.forceCombatTicks(1800L * 40);
        check(boss.stones() == 5 && boss.hasTimeStone() && !boss.hasStone(StoneKind.MIND), "never a sixth");
        boss.forceStones(9);
        check(boss.stones() == 5, "forcing nine leaves five");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "thanos_fight_clock_max")
    public static void maxStonesAndTheIntervalAreTheConfigs(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(ThanosBossConfig.MAX_STONES, 3);
        f.config(ThanosBossConfig.STONE_INTERVAL_TICKS, 400);
        ServerPlayer a = f.player(6.5, FLOOR, 6.5);
        ThanosEntity boss = boss(f, a);
        boss.forceCombatTicks(399);
        check(boss.stones() == 1, "before the interval, one stone");
        boss.forceCombatTicks(400);
        check(boss.stones() == 2, "after it, two");
        boss.forceCombatTicks(100_000);
        check(boss.stones() == 3, "maxStones 3 stops at three");
        boss.forceStones(5);
        check(boss.stones() == 3, "and forcing cannot pass it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "thanos_fight_clock_fighters", timeoutTicks = 100)
    public static void theClockNeedsAFighterInTheArena(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer starter = f.player(6.5, FLOOR, 6.5, GameType.CREATIVE);
        ThanosEntity boss = boss(f, starter);
        ServerPlayer spectator = f.player(8.5, FLOOR, 8.5, GameType.SPECTATOR);
        long[] mark = new long[1];
        helper.runAfterDelay(10, () -> {
            check(boss.combatTicks() == 0, "with only a Creative player and a Spectator near, the clock stands still: " + boss.combatTicks());
            f.player(14.5, FLOOR, 14.5);
            helper.runAfterDelay(10, () -> {
                mark[0] = boss.combatTicks();
                check(mark[0] >= 5, "a Survival player inside the arena starts it: " + mark[0]);
                helper.runAfterDelay(10, () -> {
                    check(boss.combatTicks() >= mark[0] + 8, "and it goes on");
                    helper.succeed();
                });
            });
        });
    }

    @EnhancedTest(template = ARENA, batch = "thanos_fight_surge", timeoutTicks = 200)
    public static void aNewStoneIsASurgeAndIsAnnouncedBefore(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(6.5, FLOOR, 6.5);
        ThanosEntity boss = boss(f, a);
        TestKit.sentPackets(a);
        // Ten seconds before the second stone: the socket pulses and the warning is sent.
        boss.forceCombatTicks(1800 - 190);
        int[] surgeStart = {-1};
        f.everyTick(150, tick -> {
            if (tick == 4) {
                check("message.enhanced.thanos.stone.soon".equals(BossTestSupport.lastActionBarKey(a)), "the warning reached the arena");
                check(boss.nextStoneIn() > 0 && boss.nextStoneIn() < 200, "the bar counts down: " + boss.nextStoneIn());
                check(boss.stones() == 1, "the stone has not come");
                TestKit.heardSounds(a);
                boss.forceCombatTicks(1800);
                surgeStart[0] = tick;
                check(boss.stones() == 2 && boss.state() == ThanosState.SURGE, "the stone fuses and he kneels: " + boss.state());
                check(boss.newestStone().barColor() == net.minecraft.world.BossEvent.BossBarColor.BLUE, "the bar turns blue");
                var voices = TestKit.heardSounds(a).stream().map(TestKit.HeardSound::id).toList();
                check(voices.contains(Enhanced.id("space_stone")), "the Space Stone's voice sounded: " + voices);
                check(Math.abs(ThanosDefence.damageTaken(PLAIN, stance(boss.state(), false, 0, false, 0.0F), 40.0F) - 60.0F) < 1.0E-3,
                        "he takes x1.5 in the surge");
            }
            if (surgeStart[0] < 0) {
                return false;
            }
            if (tick == surgeStart[0] + 5) {
                check(boss.currentMove() == null, "no move starts in the surge");
            }
            if (boss.state() == ThanosState.SURGE) {
                return false;
            }
            int length = tick - surgeStart[0];
            check(length >= 55 && length <= 65, "the surge lasts sixty ticks, not " + length);
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = "thanos_fight_plates")
    public static void platesBreakAtTheHealthThresholdsInTheEntity(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(6.5, FLOOR, 6.5);
        ThanosEntity boss = boss(f, a);
        float max = boss.getMaxHealth();
        check(boss.plates() == ThanosDefence.ALL_PLATES, "four intact plates");
        boss.setHealth(max * 0.76F);
        hit(boss, a, 100.0F);
        check((boss.plates() & ThanosDefence.R_PAULDRON) == 0 && (boss.plates() & ThanosDefence.L_PAULDRON) != 0, "the right pauldron breaks first");
        boss.setHealth(max * 0.56F);
        hit(boss, a, 100.0F);
        check((boss.plates() & ThanosDefence.L_PAULDRON) == 0 && (boss.plates() & ThanosDefence.BREASTPLATE) != 0, "then the left");
        boss.setHealth(max * 0.36F);
        hit(boss, a, 100.0F);
        check((boss.plates() & ThanosDefence.BREASTPLATE) == 0 && (boss.plates() & ThanosDefence.HELMET) != 0, "then the breastplate");
        boss.setHealth(max * 0.21F);
        hit(boss, a, 100.0F);
        check(boss.plates() == 0, "and the helmet last");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "thanos_fight_plates_mjolnir")
    public static void mjolnirBreaksTheHelmetAtOnceAndWandasBeamTearsPlates(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer thor = f.player(6.5, FLOOR, 6.5);
        ThanosEntity boss = boss(f, thor);
        hit(boss, ModDamageTypes.source(f.level(), ModDamageTypes.MJOLNIR, thor, thor), 10.0F);
        check((boss.plates() & ThanosDefence.HELMET) == 0 && boss.plates() == (ThanosDefence.ALL_PLATES & ~ThanosDefence.HELMET),
                "Mjolnir breaks the helmet at once, and only that");
        ServerPlayer wanda = f.player(8.5, FLOOR, 6.5);
        DamageSource beam = ModDamageTypes.source(f.level(), ModDamageTypes.CHAOS_BEAM, wanda, wanda);
        for (int i = 0; i < 59; i++) {
            hit(boss, beam, 1.0F);
        }
        check(Integer.bitCount(boss.plates()) == 3, "two seconds and a half of beam tear nothing");
        hit(boss, beam, 1.0F);
        check(Integer.bitCount(boss.plates()) == 2, "three seconds tear a plate away: " + boss.plates());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "thanos_fight_poise", timeoutTicks = 100)
    public static void poiseInterruptsAClenchAndLeavesTheSwordMoves(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(6.5, FLOOR, 4.5);
        ThanosEntity boss = boss(f, a);
        boss.forceStones(2);
        helper.runAfterDelay(70, () -> {
            boss.forceMove(ThanosMove.POWER_BLAST, a);
            check(boss.state() == ThanosState.TELL && boss.clenching(), "the Power Blast begins with a clench");
            hit(boss, a, 60.0F);
            check(boss.clenching() && boss.state() == ThanosState.TELL, "one hit of 36 after the plates adds 72 poise: not yet");
            hit(boss, a, 60.0F);
            check(boss.state() == ThanosState.STAGGERED && boss.currentMove() == null, "100 poise breaks the clench and staggers him: " + boss.state());
            check(boss.gauntletLocked() && boss.flag(ThanosEntity.FLAG_GAUNTLET_LOCKED), "and his stones stay dark");
            for (int i = 0; i < 100; i++) {
                ThanosMove pick = boss.pickMove(a);
                check(pick == null || !pick.clench(), "no stone move is chosen while the Gauntlet is dark: " + pick);
            }
            // A sword move has no clench: hits add no poise.
            boss.forceMove(ThanosMove.SWORD_SWEEP, a);
            hit(boss, a, 60.0F);
            hit(boss, a, 60.0F);
            hit(boss, a, 60.0F);
            check(boss.state() == ThanosState.TELL && boss.currentMove() == ThanosMove.SWORD_SWEEP, "a sweep is not a clench: nothing interrupts it");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "thanos_fight_stormbreaker", timeoutTicks = 120)
    public static void stormbreakerCutsTheBlastAndStaggersHim(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer thor = f.player(10.5, FLOOR, 4.5);
        ThanosEntity boss = boss(f, thor);
        ServerPlayer bystander = f.player(4.5, FLOOR, 10.5);
        boss.forceMove(ThanosMove.POWER_BLAST, thor);
        double[] before = new double[1];
        boolean[] cut = new boolean[1];
        f.everyTick(100, tick -> {
            if (!cut[0] && boss.state() == ThanosState.ACTIVE && boss.moveTick() >= 23) {
                cut[0] = true;
                before[0] = boss.points();
                check(thor.getHealth() < thor.getMaxHealth(), "the first tick of the beam hit the one in its line");
                hit(boss, ModDamageTypes.source(f.level(), ModDamageTypes.STORMBREAKER, thor, thor), 40.0F);
                check(boss.state() == ThanosState.STAGGERED && boss.currentMove() == null, "Stormbreaker staggers him and ends the beam: " + boss.state());
                check(before[0] - boss.points() >= 59.0, "and does x2.5: " + (before[0] - boss.points()));
                // The beam is gone: someone who now steps into its line is untouched.
                Vec3 inLine = f.helper().absoluteVec(new Vec3(10.5, FLOOR, 7.5));
                bystander.teleportTo(f.level(), inLine.x, inLine.y, inLine.z, 0.0F, 0.0F);
                return false;
            }
            if (cut[0] && tick > 60) {
                check(bystander.getHealth() == bystander.getMaxHealth(), "the cut beam hurt nobody else");
                return true;
            }
            return false;
        });
    }
}

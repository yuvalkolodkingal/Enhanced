package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.balance.Balance;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * SMP Balance Mode (3.16): the names of the numbers sort into the six settings; each preset changes the powers' numbers
 * as read (never the file), caps and not removals; Wanda's Build Magic goes to the scale of Billy's; an override wins
 * over the preset; the bosses are never touched. Each test sets the balance and puts it back within one call (the tests
 * share one server). Batch {@code balance}.
 */
public final class BalanceTests {
    private static final String BATCH = "balance";

    private static ConfigSpec.ConfigValue<?> value(String section, String key) {
        ConfigSpec.ConfigValue<?> v = EnhancedConfig.SPEC.value(List.of(section, key));
        check(v != null, "the config has " + section + "." + key);
        return v;
    }

    private static double now(String section, String key) {
        return ((Number) value(section, key).get()).doubleValue();
    }

    private static double raw(String section, String key) {
        return ((Number) value(section, key).getRaw()).doubleValue();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theNumbersSortIntoTheSettings(GameTestHelper helper) {
        check(Balance.classify("telekinesisRange") == Balance.Setting.RANGE, "a range");
        check(Balance.classify("mindReadRange") == Balance.Setting.RANGE, "mind read is a range, not a minimum");
        check(Balance.classify("hexMaxRadius") == Balance.Setting.RANGE, "a radius is a range");
        check(Balance.classify("disarmCooldown") == Balance.Setting.COOLDOWN, "a cooldown");
        check(Balance.classify("beamInterval") == Balance.Setting.COOLDOWN, "an interval is a cooldown");
        check(Balance.classify("webShotExhaustion") == Balance.Setting.COST, "exhaustion is the cost");
        check(Balance.classify("chaosBeamDamage") == Balance.Setting.DAMAGE, "damage");
        check(Balance.classify("hexHoldDamageMax") == Balance.Setting.DAMAGE, "the most damage is damage");
        check(Balance.classify("maxBlocks") == Balance.Setting.SIZE, "blocks are a size");
        check(Balance.classify("freezeDuration") == Balance.Setting.DURATION, "a duration");
        check(Balance.classify("webbedTicks") == Balance.Setting.DURATION, "ticks are a duration");
        check(Balance.classify("minFall") == null, "a minimum is never changed");
        check(Balance.classify("flightSpeed") == null, "speeds are not changed");
        check(Balance.classify("enabled") == null && Balance.classify("hexDeathChance") == null,
                "switches and chances are not changed");
        check(Balance.classify("pullPlayerGraceTicks") == null, "a victim's grace is not changed");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void thePresetsCapEveryPowerButNeverTheBosses(GameTestHelper helper) {
        try {
            Balance.apply(Balance.Preset.FULL, Map.of());
            double range = raw("magneto", "telekinesisRange");
            check(now("magneto", "telekinesisRange") == range, "full: as configured");

            Balance.apply(Balance.Preset.BALANCED, Map.of());
            check(now("magneto", "telekinesisRange") == Math.round(range * 0.8), "balanced: Magneto at 80%: "
                    + now("magneto", "telekinesisRange"));
            check(Math.abs(now("magneto", "disarmCooldown") - Math.round(raw("magneto", "disarmCooldown") / 0.8)) < 1e-9,
                    "a cooldown grows as the power weakens: " + now("magneto", "disarmCooldown"));
            check(Math.abs(now("scarlet_witch", "chaosBeamDamage") - raw("scarlet_witch", "chaosBeamDamage") * 0.6) < 1e-9,
                    "the Scarlet Witch, among the strongest, at 60%");
            check(now("spider_man", "webShotRange") == raw("spider_man", "webShotRange"), "Spider-Man as he is");
            check(now("thanos_boss", "health") == raw("thanos_boss", "health"), "a boss is never touched");
            check(value("magneto", "telekinesisRange").getRaw().equals((int) range), "the file is never changed");
            // Wanda's Build Magic on the scale of Billy's at his highest rank
            double billyTop = raw("build_magic_billy", "reach") + 8 * (raw("build_magic_billy", "maxRank") - 1);
            check(now("build_magic_wanda", "reach") == Math.min(Math.round(raw("build_magic_wanda", "reach") * 0.6), billyTop),
                    "Wanda's reach within Billy's: " + now("build_magic_wanda", "reach") + " of " + billyTop);
            check(now("build_magic_wanda", "maxBlocks") > 0 && now("build_magic_wanda", "maxBlocks")
                    <= raw("build_magic_billy", "meter") * 64, "her blocks have a ceiling now (0 was none): "
                    + now("build_magic_wanda", "maxBlocks"));
            check(now("build_magic_billy", "reach") == raw("build_magic_billy", "reach"), "Billy as he is");

            Balance.apply(Balance.Preset.HARDCORE, Map.of());
            check(now("spider_man", "webShotRange") == Math.round(raw("spider_man", "webShotRange") * 0.5),
                    "hardcore: Spider-Man at half");
            check(now("build_magic_wanda", "repeatMax") == now("build_magic_billy", "repeatMax"),
                    "hardcore: Wanda's repeats as an Apprentice Billy's (his hardcore ones): "
                    + now("build_magic_wanda", "repeatMax") + " / " + now("build_magic_billy", "repeatMax"));
            check(now("magneto", "telekinesisRange") >= 1, "never removed: " + now("magneto", "telekinesisRange"));

            // an override wins over the preset, and lifts the cap of what it overrides
            Balance.apply(Balance.Preset.HARDCORE, Map.of("magneto.range", 1.0, "build_magic_wanda.range", 1.0));
            check(now("magneto", "telekinesisRange") == range, "the override: Magneto's range in full");
            check(now("magneto", "disarmCooldown") > raw("magneto", "disarmCooldown"), "his cooldowns still hardcore");
            check(now("build_magic_wanda", "reach") == raw("build_magic_wanda", "reach"), "her reach overridden");
        } finally {
            Balance.apply(Balance.Preset.FULL, Map.of());
        }
        check(now("magneto", "telekinesisRange") == raw("magneto", "telekinesisRange"), "back to full");
        helper.succeed();
    }
}

package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.hud.PassiveHudRules;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/** The setting "Show always-on powers on the HUD" (3.17): Always / Briefly / Never. Pure rules: no client class is used. */
public final class PassiveHudTests {
    @EnhancedTest(template = ARENA)
    public static void briefIsTheDefaultAndShowsForEightSecondsThenFades(GameTestHelper helper) {
        check(EnhancedClientConfig.passiveHud() == PassiveHudRules.Mode.BRIEFLY, "the default is Briefly");
        check(PassiveHudRules.SHOWN_TICKS == 160, "8 seconds");
        long start = 1000;
        long until = start + PassiveHudRules.SHOWN_TICKS;
        check(PassiveHudRules.visibility(PassiveHudRules.Mode.BRIEFLY, until, start) == 1.0F, "shown at once");
        check(PassiveHudRules.visibility(PassiveHudRules.Mode.BRIEFLY, until, until - PassiveHudRules.FADE_TICKS - 1) == 1.0F, "fully shown until the fade");
        float mid = PassiveHudRules.visibility(PassiveHudRules.Mode.BRIEFLY, until, until - PassiveHudRules.FADE_TICKS / 2);
        check(mid > 0.0F && mid < 1.0F, "fading out: " + mid);
        check(PassiveHudRules.visibility(PassiveHudRules.Mode.BRIEFLY, until, until) == 0.0F, "hidden after 8 seconds");
        check(PassiveHudRules.visibility(PassiveHudRules.Mode.BRIEFLY, until, until + 5000) == 0.0F, "and it stays hidden");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void alwaysAlwaysShowsAndNeverNeverDoes(GameTestHelper helper) {
        for (long now : new long[]{0, 100, 100000}) {
            check(PassiveHudRules.visibility(PassiveHudRules.Mode.ALWAYS, 0, now) == 1.0F, "Always: shown at " + now);
            check(PassiveHudRules.visibility(PassiveHudRules.Mode.NEVER, 1 << 30, now) == 0.0F, "Never: hidden at " + now);
        }
        helper.succeed();
    }
}

package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroPresets;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.love.EyeLaser;
import com.kolodkingal.enhanced.love.LoveAbilities;
import com.kolodkingal.enhanced.love.ThunderStrike;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Love (3.8, lane L8, PLAN 10.6): her body (32 health, worthy of Mjolnir); the Axe Throw only with Stormbreaker, Thor's
 * throw that returns to her hand; the Thunder Strike's 8 within 2 (and nothing farther); her lightning beam's damage is
 * Thor's; the Eye Laser's 6 to a zombie once per 20 ticks, setting it burning, then its rest; flight only while she
 * holds the axe. Batch {@code love}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class LoveTests {
    private static final String BATCH = "love";

    /** Love at full health, gone when the test ends. */
    static ServerPlayer love(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = f.player(x, FLOOR, z, Power.LOVE);
        player.setHealth(player.getMaxHealth());
        f.onClose(() -> HeroLifecycle.onLogout(player));
        return player;
    }

    /** A zombie with no armour and 100 health that stands where it is put. */
    static Zombie dummy(GameTestHelper helper, double x, double z) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        zombie.setHealth(100.0F);
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000, 0, false, false));
        return zombie;
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void herBodyIsThorsDaughters(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer love = love(f, 4.5, 4.5);
        assertNear(love.getMaxHealth(), 32.0, 1.0E-4, "32 health: sixteen hearts");
        assertNear(love.getAttributeValue(Attributes.ARMOR), 4.0, 1.0E-4, "4 armour");
        check(Worthiness.isWorthy(love), "worthy of Mjolnir");
        ItemStack hammer = new ItemStack(ModItems.MJOLNIR.get());
        check(Worthiness.canLift(love, hammer), "she lifts it");
        Powers.setPower(love, Power.NONE);
        assertNear(love.getMaxHealth(), 20.0, 1.0E-4, "and none of it once she is no longer Love");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theAxeFliesOnlyWithStormbreakerAndComesHome(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer love = love(f, 3.5, 10.5);
        PowerData data = Powers.get(love);
        TestKit.look(love, TestKit.EAST, 0.0F);
        LoveAbilities.onKey(love, data, 1, true, false);
        check(data.hammerEntityId() == -1, "no axe, no throw");
        love.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        LoveAbilities.onKey(love, data, 1, true, false);
        check(data.hammerEntityId() == -1 && love.getMainHandItem().is(ModItems.MJOLNIR.get()),
                "Mjolnir is not her axe");
        love.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        LoveAbilities.onKey(love, data, 1, true, false);
        check(love.getMainHandItem().isEmpty() && helper.getLevel().getEntity(data.hammerEntityId())
                instanceof ThrownWeaponEntity thrown && ThorAbilities.isStormbreaker(thrown.weapon()),
                "Stormbreaker flies");
        f.everyTick(160, tick -> love.getMainHandItem().is(ModItems.STORMBREAKER.get())
                && data.hammerEntityId() == -1);
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theThunderStrikeHitsEightWithinTwo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer love = love(f, 3.5, 10.5);
        Zombie near = dummy(helper, 10.5, 10.5);
        Zombie far = dummy(helper, 15.5, 10.5);
        // She looks at the floor right under the near zombie's feet, a block before it.
        TestKit.lookAt(love, helper.absoluteVec(new Vec3(9.6, FLOOR - 0.01, 10.5)));
        check(ThunderStrike.strike(love, Powers.get(love)), "the bolt falls");
        assertNear(near.getHealth(), 92.0, 1.0E-3, "8 to the zombie within 2");
        assertNear(far.getHealth(), 100.0, 1.0E-3, "nothing to the one 5 away");
        check(!ThunderStrike.strike(love, Powers.get(love)) && ThunderStrike.cooldown(love) > 90,
                "then it rests 100 ticks");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void herLightningIsThors(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer love = love(f, 3.5, 10.5);
        love.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        Zombie target = dummy(helper, 9.5, 10.5);
        TestKit.lookAt(love, target);
        PowerData data = Powers.get(love);
        LoveAbilities.onKey(love, data, 3, true, false);
        check(data.is(Live.CALLING_LIGHTNING), "her beam burns");
        int[] colours = ThorAbilities.lightningBeamColours(love);
        check(colours[0] == LoveAbilities.LIGHTNING && colours[1] == LoveAbilities.LIGHTNING_GLOW, "in her colours");
        LoveAbilities.tick(love, data);
        float thors = ThorAbilities.beamDamage(new ItemStack(ModItems.STORMBREAKER.get()), false);
        assertNear(100.0 - target.getHealth(), thors, 1.0E-3, "Thor's damage through Stormbreaker");
        LoveAbilities.onKey(love, data, 3, false, false);
        check(!data.is(Live.CALLING_LIGHTNING), "and it stops");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theEyeLaserBurnsSixOncePerSecond(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer love = love(f, 3.5, 10.5);
        Zombie target = dummy(helper, 9.5, 10.5);
        Zombie behind = dummy(helper, 13.5, 10.5);
        PowerData data = Powers.get(love);
        float[] after = new float[3];
        f.everyTick(150, tick -> {
            TestKit.lookAt(love, target);
            if (tick == 1) {
                LoveAbilities.onKey(love, data, 4, true, false);
                check(data.is(Live.BEAMING) && EyeLaser.firing(love), "her eyes burn");
            }
            LoveAbilities.tick(love, data);
            if (tick == 1) {
                after[0] = target.getHealth();
                check(target.isOnFire(), "it burns");
                assertNear(after[0], 94.0, 1.0E-3, "6 to the zombie");
                assertNear(behind.getHealth(), 94.0, 1.0E-3, "and to the one behind it: the laser goes through");
            }
            if (tick == 15) {
                assertNear(target.getHealth(), 94.0, 1.0E-3, "no second hit within 20 ticks");
            }
            if (tick == 22) {
                assertNear(target.getHealth(), 88.0, 1.0E-3, "the second hit after 20 ticks");
            }
            if (tick == 105) {
                check(!data.is(Live.BEAMING) && !EyeLaser.firing(love), "it burns 100 ticks at most");
                check(!data.isReady(HeroAbility.LOVE_EYE_LASER, love.level().getGameTime()), "then it rests");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void sheFliesOnlyWithTheAxe(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer love = love(f, 4.5, 4.5);
        PowerTicker.applyPassives(love, Powers.get(love));
        check(!love.mayFly(), "no axe, no flight");
        love.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        PowerTicker.applyPassives(love, Powers.get(love));
        check(love.mayFly(), "the axe in a hand: she flies");
        love.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        PowerTicker.applyPassives(love, Powers.get(love));
        check(!love.mayFly(), "and not once she lets it go");
        helper.succeed();
    }

    /** Her five rows and two Sneak variants are hers. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void herRowsWork(GameTestHelper helper) {
        List<HeroAbility> rows = List.of(HeroAbility.LOVE_AXE, HeroAbility.LOVE_LIGHTNING, HeroAbility.LOVE_EYE_LASER,
                HeroAbility.LOVE_BIFROST, HeroAbility.LOVE_BODY);
        for (HeroAbility row : rows) {
            check(row.hero == Power.LOVE, row + " is Love's");
        }
        check(HeroAbility.LOVE_THUNDER_STRIKE.isVariant() && HeroAbility.LOVE_BIFROST_LOOK.isVariant(),
                "her two Sneak variants");
        helper.succeed();
    }

    /** Her preset (for the integrator's {@code HeroPresets.HEROES}) passes the rules at 60 points, with no note. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void herPresetFitsSixtyPoints(GameTestHelper helper) throws IOException {
        Path file = ModList.get().getModFileById(Enhanced.MODID).getFile()
                .findResource("assets", Enhanced.MODID, "hero_presets", "love.json");
        String json = Files.readString(file);
        HeroPresets.Preset preset = HeroPresets.read("love", json);
        check(preset != null && preset.like() == Power.LOVE, "her preset reads as Love's");
        CustomHeroRules.Result r = CustomHeroRules.sanitize(preset.hero(),
                CustomHeroRules.Limits.DEFAULT.withBudget(HeroPresets.MAX_POINTS));
        check(r.notes().isEmpty(), "no note: " + r.notes());
        int cost = CustomHeroRules.cost(r.hero());
        check(cost <= HeroPresets.MAX_POINTS, "at most 60 points: " + cost);
        helper.succeed();
    }

    private LoveTests() {
    }
}

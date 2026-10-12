package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Quicksilver. Tests that start Quicksilver Time get their own batch and a small slow-motion radius, because the
 * slow motion would otherwise reach into the neighbouring test arenas. (Super Speed slows nothing, so a test that
 * only uses Super Speed needs no small radius.)
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class QuicksilverTests {

    /** Makes the player sprint and runs his tick, as a connected client's tick would. */
    private static void run(ServerPlayer player, boolean sprinting) {
        player.setSprinting(sprinting);
        PowerTicker.tick(player);
    }

    @GameTest(template = ARENA)
    public static void gogglesAwakenQuicksilver(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.QUICKSILVER_GOGGLES.get()));
        player.doTick();
        check(Powers.is(player, Power.QUICKSILVER), "wearing the goggles makes you Quicksilver");
        assertNear(player.getAttributeValue(Attributes.ARMOR), 2.0, 1.0E-6, "the goggles protect like an iron helmet");
        check(!player.mayFly(), "Quicksilver runs, he doesn't fly");

        // Someone with another power only gets a pair of goggles.
        ServerPlayer magneto = f.player(4.5, FLOOR, 4.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.QUICKSILVER_GOGGLES.get()));
        magneto.doTick();
        check(Powers.is(magneto, Power.MAGNETO), "powers never stack");
        helper.succeed();
    }

    /** Presses (and releases) ability key {@code ability}. */
    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    /** Presses ability key {@code ability} while sneaking. */
    private static void pressSneaking(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        press(player, ability);
        player.setShiftKeyDown(false);
    }

    /** Turns Quicksilver Time (R) or Super Speed (Sneak + R) on or off. */
    private static void toggle(ServerPlayer player, boolean time) {
        if (time) {
            press(player, 1);
        } else {
            pressSneaking(player, 1);
        }
    }

    /** A zombie that stands still with a long effect on it: the effect only runs down while the zombie really ticks. */
    private static Zombie timedZombie(GameTestHelper helper, double x) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(x, FLOOR, 10.5));
        zombie.addEffect(new MobEffectInstance(MobEffects.LUCK, EFFECT_TICKS));
        return zombie;
    }

    /** How many ticks the zombie has really ticked since {@link #timedZombie}. */
    private static int ticked(Zombie zombie) {
        return EFFECT_TICKS - zombie.getEffect(MobEffects.LUCK).getDuration();
    }

    private static final int EFFECT_TICKS = 400;

    @GameTest(template = ARENA, batch = "quicksilver_speed")
    public static void sprintingInQuicksilverTimeIsSuperSpeed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 5);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        double walking = peter.getAttributeValue(Attributes.MOVEMENT_SPEED);
        PowerData data = Powers.get(peter);
        check(!data.quicksilverTime() && !data.superSpeed(), "both modes are off to begin with");

        // With both modes off, sprinting is only sprinting (it multiplies speed by 1.3).
        run(peter, true);
        check(!data.slowMo() && !data.is(Live.SPEEDING), "sprinting alone starts nothing");
        assertNear(peter.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 1.3, 1.0E-4, "just a sprint");

        press(peter, 1);
        check(data.quicksilverTime(), "R turns Quicksilver Time on");
        run(peter, true);
        check(data.slowMo(), "Quicksilver Time slows the world");
        check(data.is(Live.SPEEDING) && QuicksilverAbilities.isRunning(peter), "sprinting in Quicksilver Time is running");
        double bonus = EnhancedConfig.QUICKSILVER_SPEED_BONUS.get();
        // Sprinting itself multiplies speed by 1.3; running multiplies it again by (1 + bonus).
        assertNear(peter.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 1.3 * (1 + bonus), 1.0E-4, "super speed");
        check(peter.getAttributeValue(Attributes.STEP_HEIGHT) >= 1.2, "he runs up whole blocks");

        peter.fallDistance = 10;
        float health = peter.getHealth();
        peter.hurt(helper.getLevel().damageSources().fall(), 6.0F);
        assertNear(peter.getHealth(), health, 1.0E-4, "no fall damage while running");

        // Stopping ends the run, but not Quicksilver Time: the world stays slowed while he walks or stands.
        run(peter, false);
        check(!data.is(Live.SPEEDING), "stopping ends the run");
        check(data.slowMo() && data.quicksilverTime(), "but Quicksilver Time stays on");
        assertNear(peter.getAttributeValue(Attributes.MOVEMENT_SPEED), walking, 1.0E-4, "normal speed again");

        // Ability 1 again turns it off, so he can sprint normally.
        press(peter, 1);
        run(peter, true);
        check(!data.quicksilverTime() && !data.slowMo(), "Quicksilver Time can be switched off");
        check(!data.is(Live.SPEEDING), "and then sprinting is just sprinting");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "quicksilver_standing", timeoutTicks = 100)
    public static void quicksilverTimeSlowsAZombieWhileHeStandsStill(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 6);
        ServerPlayer peter = f.player(2.5, FLOOR, 10.5, Power.QUICKSILVER);
        Zombie near = timedZombie(helper, 5.5);
        Zombie far = timedZombie(helper, 19.5);
        PowerData data = Powers.get(peter);
        double walking = peter.getAttributeValue(Attributes.MOVEMENT_SPEED);
        press(peter, 1);

        f.everyTick(30, tick -> {
            run(peter, false);
            if (tick < 30) {
                return false;
            }
            check(!peter.isSprinting() && !data.is(Live.SPEEDING), "he is standing still, not running");
            check(data.slowMo(), "Quicksilver Time slows the world anyway");
            assertNear(peter.getAttributeValue(Attributes.MOVEMENT_SPEED), walking, 1.0E-6, "and he walks at his normal speed");
            check(ticked(far) >= 28, "the far zombie ticks normally (" + ticked(far) + " ticks)");
            check(ticked(near) <= 10, "the zombie next to him is slowed (" + ticked(near) + " ticks of " + ticked(far) + ")");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "quicksilver_super_speed", timeoutTicks = 100)
    public static void superSpeedSlowsNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(2.5, FLOOR, 10.5, Power.QUICKSILVER);
        Zombie near = timedZombie(helper, 5.5);
        Zombie far = timedZombie(helper, 19.5);
        PowerData data = Powers.get(peter);
        double walking = peter.getAttributeValue(Attributes.MOVEMENT_SPEED);
        pressSneaking(peter, 1);
        check(data.superSpeed() && !data.quicksilverTime(), "Sneak + R turns Super Speed on");
        float exhaustion = peter.getFoodData().getExhaustionLevel();

        f.everyTick(30, tick -> {
            run(peter, true);
            if (tick == 1) {
                check(data.is(Live.SPEEDING), "sprinting in Super Speed is running");
                double bonus = EnhancedConfig.QUICKSILVER_SPEED_BONUS.get();
                assertNear(peter.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 1.3 * (1 + bonus), 1.0E-4, "super speed");
                check(peter.getAttributeValue(Attributes.STEP_HEIGHT) >= 1.2, "he runs up whole blocks");
                peter.fallDistance = 10;
                float health = peter.getHealth();
                peter.hurt(helper.getLevel().damageSources().fall(), 6.0F);
                assertNear(peter.getHealth(), health, 1.0E-4, "no fall damage while running");
            }
            check(!data.slowMo(), "Super Speed doesn't slow the world");
            check(SlowMotion.zones(helper.getLevel()).isEmpty(), "so there is no slow-motion zone");
            if (tick < 30) {
                return false;
            }
            check(!SlowMotion.isInZone(helper.getLevel(), near.position()), "the zombie next to him is not in any zone");
            check(ticked(near) >= 28 && ticked(far) >= 28, "no zombie is slowed (" + ticked(near) + " and " + ticked(far) + " ticks)");
            double cost = EnhancedConfig.QUICKSILVER_EXHAUSTION.get() * 30;
            assertNear(peter.getFoodData().getExhaustionLevel() - exhaustion, cost, 0.01, "running at super speed costs hunger");
            return true;
        });
    }

    @GameTest(template = ARENA)
    public static void theTwoModesExcludeEachOther(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        PowerData data = Powers.get(peter);

        press(peter, 1);
        check(data.quicksilverTime() && !data.superSpeed(), "R: Quicksilver Time");
        pressSneaking(peter, 1);
        check(data.superSpeed() && !data.quicksilverTime(), "Sneak + R: Super Speed instead");
        press(peter, 1);
        check(data.quicksilverTime() && !data.superSpeed(), "R again: back to Quicksilver Time");
        pressSneaking(peter, 1);
        pressSneaking(peter, 1);
        check(!data.superSpeed() && !data.quicksilverTime(), "Sneak + R twice leaves both modes off");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bothModesAreSavedAndSynced(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        PowerData data = Powers.get(peter);
        for (boolean time : new boolean[]{true, false}) {
            data.setQuicksilverTime(time);
            data.setSuperSpeed(!time);
            Tag saved = PowerData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            PowerData loaded = PowerData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
            check(loaded.quicksilverTime() == time && loaded.superSpeed() == !time, "the mode is saved with the player");
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            PowerData.STREAM_CODEC.encode(buf, data);
            PowerData synced = PowerData.STREAM_CODEC.decode(buf);
            check(synced.quicksilverTime() == time && synced.superSpeed() == !time, "and synced to the clients");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "quicksilver_hunger")
    public static void quicksilverTimeCostsHungerAndStopsWhenStarving(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 5);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        PowerData data = Powers.get(peter);
        double cost = EnhancedConfig.QUICKSILVER_EXHAUSTION.get();
        press(peter, 1);

        // Standing still in Quicksilver Time costs half of what running in it does.
        float before = peter.getFoodData().getExhaustionLevel();
        for (int i = 0; i < 20; i++) {
            run(peter, false);
        }
        assertNear(peter.getFoodData().getExhaustionLevel() - before, cost * 20 * 0.5, 0.005, "standing in Quicksilver Time is half price");
        before = peter.getFoodData().getExhaustionLevel();
        for (int i = 0; i < 20; i++) {
            run(peter, true);
        }
        assertNear(peter.getFoodData().getExhaustionLevel() - before, cost * 20, 0.005, "running in Quicksilver Time is full price");

        // With nothing left to eat it switches itself off, and can't be started again.
        peter.getFoodData().setFoodLevel(0);
        run(peter, true);
        check(!data.quicksilverTime() && !data.slowMo() && !data.is(Live.SPEEDING), "starving ends Quicksilver Time");
        press(peter, 1);
        check(!data.quicksilverTime(), "and he can't start it while starving");

        // Unless he is invulnerable (Creative): hunger doesn't matter to him.
        peter.getAbilities().invulnerable = true;
        press(peter, 1);
        run(peter, true);
        check(data.quicksilverTime() && data.slowMo() && data.is(Live.SPEEDING), "invulnerable, he is never too hungry");
        peter.getAbilities().invulnerable = false;

        // Super Speed doesn't switch off, but a starving Quicksilver can't run.
        pressSneaking(peter, 1);
        run(peter, true);
        check(data.superSpeed() && !data.is(Live.SPEEDING), "starving, Super Speed stays chosen but he can't run");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "quicksilver_actions")
    public static void bothModesDoEverythingFaster(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 5);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        double speed = EnhancedConfig.QUICKSILVER_ACTION_SPEED.get();
        check(speed == 4.0, "everything is four times as fast by default");
        BlockState stone = Blocks.STONE.defaultBlockState();
        run(peter, false);
        double attack = peter.getAttributeValue(Attributes.ATTACK_SPEED);
        double mining = peter.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        double swimming = peter.getAttributeValue(NeoForgeMod.SWIM_SPEED);
        double digging = peter.getDestroySpeed(stone);

        for (boolean time : new boolean[]{true, false}) {
            String mode = time ? "Quicksilver Time" : "Super Speed";
            toggle(peter, time);
            run(peter, false);
            assertNear(peter.getAttributeValue(Attributes.ATTACK_SPEED), attack * speed, 1.0E-6, "attack speed in " + mode);
            assertNear(peter.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), mining * speed, 1.0E-6, "block break speed in " + mode);
            assertNear(peter.getAttributeValue(NeoForgeMod.SWIM_SPEED), swimming * speed, 1.0E-6, "swim speed in " + mode);
            assertNear(peter.getDestroySpeed(stone), digging * speed, 1.0E-4, "mining stone in " + mode);

            toggle(peter, time);
            run(peter, false);
            assertNear(peter.getAttributeValue(Attributes.ATTACK_SPEED), attack, 1.0E-6, "attack speed is normal with " + mode + " off");
            assertNear(peter.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), mining, 1.0E-6, "block break speed is normal with " + mode + " off");
            assertNear(peter.getAttributeValue(NeoForgeMod.SWIM_SPEED), swimming, 1.0E-6, "swim speed is normal with " + mode + " off");
        }

        // Someone who stops being Quicksilver is not faster at anything.
        press(peter, 1);
        run(peter, false);
        Powers.setPower(peter, Power.TOMMY);
        assertNear(peter.getAttributeValue(Attributes.ATTACK_SPEED), attack, 1.0E-6, "a hero who is no longer Quicksilver attacks normally");
        assertNear(peter.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), mining, 1.0E-6, "and mines normally");
        assertNear(peter.getAttributeValue(NeoForgeMod.SWIM_SPEED), swimming, 1.0E-6, "and swims normally");
        helper.succeed();
    }

    /** Eats a steak and counts the ticks it takes. */
    private static int eatSteak(ServerPlayer player) {
        player.getFoodData().setFoodLevel(6);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        int ticks = 0;
        while (player.isUsingItem() && ticks < 100) {
            player.doTick();
            ticks++;
        }
        check(!player.isUsingItem() && player.getMainHandItem().isEmpty(), "the steak was eaten after " + ticks + " ticks");
        check(player.getFoodData().getFoodLevel() == 14, "and the food was eaten: " + player.getFoodData().getFoodLevel());
        return ticks;
    }

    @GameTest(template = ARENA, batch = "solo_eating_finishes_in_a_quarter_of_the_ticks")
    public static void eatingFinishesInAQuarterOfTheTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 5);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        int normal = eatSteak(peter);
        check(normal >= 30, "a steak takes about 32 ticks to eat (took " + normal + ")");

        toggle(peter, true);
        int inTime = eatSteak(peter);
        toggle(peter, false);
        int inSpeed = eatSteak(peter);
        check(inTime >= normal / 4 - 1 && inTime <= normal / 4 + 1, "in Quicksilver Time eating takes a quarter of the ticks (" + inTime + ")");
        check(inSpeed >= normal / 4 - 1 && inSpeed <= normal / 4 + 1, "and so in Super Speed (" + inSpeed + ")");

        // The extra ticks are taken off the use; fractions carry over, and a use is never cut below its last tick.
        ItemStack steak = new ItemStack(Items.COOKED_BEEF);
        f.config(EnhancedConfig.QUICKSILVER_ACTION_SPEED, 2.5);
        int skipped = 0;
        for (int t = 0; t < 10; t++) {
            peter.tickCount = t;
            skipped += 1000 - QuicksilverAbilities.fasterUse(peter, steak, 1000);
        }
        check(skipped == 15, "2.5 times as fast skips 15 ticks in 10 (skipped " + skipped + ")");
        f.config(EnhancedConfig.QUICKSILVER_ACTION_SPEED, 4.0);
        peter.tickCount = 0;
        check(QuicksilverAbilities.fasterUse(peter, steak, 3) == 1 && QuicksilverAbilities.fasterUse(peter, steak, 2) == 1
                && QuicksilverAbilities.fasterUse(peter, steak, 1) == 1, "the use never drops below its last tick, so vanilla ends it");
        check(QuicksilverAbilities.fasterUse(peter, steak, 0) == 0 && QuicksilverAbilities.fasterUse(peter, steak, -2) == -2,
                "and a use that is over is left alone");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "solo_brushes_spyglasses_and_horns_keep_their_own_pace")
    public static void brushesSpyglassesAndHornsKeepTheirOwnPace(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 5);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        toggle(peter, true);
        peter.tickCount = 0;

        // A brush strokes when ten ticks of use have passed (the 5th, the 15th...): rushed by four it would never stroke.
        // (It only goes on while it points at a block, so he looks at the floor.)
        peter.setXRot(90F);
        peter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
        peter.startUsingItem(InteractionHand.MAIN_HAND);
        int brushing = peter.getUseItemRemainingTicks();
        for (int i = 0; i < 10; i++) {
            peter.doTick();
        }
        check(brushing - peter.getUseItemRemainingTicks() == 10, "a brush goes tick by tick in Quicksilver Time (used "
                + (brushing - peter.getUseItemRemainingTicks()) + " ticks in 10)");
        peter.stopUsingItem();

        // A spyglass would run out after 15 seconds, and a horn's call would end long before its sound.
        check(QuicksilverAbilities.fasterUse(peter, new ItemStack(Items.SPYGLASS), 1200) == 1200, "a spyglass keeps its minute");
        check(QuicksilverAbilities.fasterUse(peter, new ItemStack(Items.GOAT_HORN), 140) == 140, "and a goat horn its call");
        check(QuicksilverAbilities.fasterUse(peter, new ItemStack(Items.BOW), 72000) < 72000, "while a bow is drawn faster");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "solo_others_are_not_rushed_by_quicksilver_time")
    public static void othersAreNotRushedByQuicksilverTime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 5);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        ServerPlayer other = f.player(12.5, FLOOR, 10.5);
        double attack = other.getAttributeValue(Attributes.ATTACK_SPEED);
        double mining = other.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        double swimming = other.getAttributeValue(NeoForgeMod.SWIM_SPEED);

        toggle(peter, true);
        run(peter, false);
        check(Powers.get(peter).slowMo() && other.distanceTo(peter) < SlowMotion.radius(), "the other player stands in his Quicksilver Time");
        check(QuicksilverAbilities.isQuick(peter) && !QuicksilverAbilities.isQuick(other), "but only Quicksilver is quicker");
        assertNear(other.getAttributeValue(Attributes.ATTACK_SPEED), attack, 1.0E-6, "he attacks at his own speed");
        assertNear(other.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), mining, 1.0E-6, "mines at his own speed");
        assertNear(other.getAttributeValue(NeoForgeMod.SWIM_SPEED), swimming, 1.0E-6, "and swims at his own speed");
        int ticks = eatSteak(other);
        check(ticks >= 30, "and eats a steak in the usual time (took " + ticks + " ticks)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "quicksilver_bows")
    public static void bowsStillReleaseAndCrossbowsStillCharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 5);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        // In the main inventory: the bow and the crossbow take the hotbar slot in his hand.
        peter.getInventory().setItem(10, new ItemStack(Items.ARROW, 64));

        // Five ticks is a quick tap for a normal archer (a bow takes 20 to draw): a weak shot, and a crossbow that is
        // nowhere near loaded (it takes 25).
        check(!shoot(helper, peter, 5).isCritArrow(), "without a mode, five ticks only draw the bow part of the way");
        check(!loadCrossbow(peter, 7), "and a crossbow takes much longer to load");

        for (boolean time : new boolean[]{true, false}) {
            String mode = time ? "Quicksilver Time" : "Super Speed";
            toggle(peter, time);
            check(!shoot(helper, peter, 4).isCritArrow(), "in " + mode + " four ticks still only draw the bow part of the way");
            check(shoot(helper, peter, 5).isCritArrow(), "in " + mode + " five ticks draw the bow fully: the arrow flies at full power");
            check(!loadCrossbow(peter, 6), "in " + mode + " six ticks don't load a crossbow yet");
            check(loadCrossbow(peter, 7), "but seven ticks do");
            toggle(peter, time);
        }
        helper.succeed();
    }

    /** Draws a bow for {@code ticks} ticks and lets go; returns the arrow that flies off. */
    private static AbstractArrow shoot(GameTestHelper helper, ServerPlayer player, int ticks) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        for (int i = 0; i < ticks; i++) {
            player.doTick();
        }
        player.releaseUsingItem();
        List<AbstractArrow> arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(4));
        check(arrows.size() == 1, "letting go of the bow shoots one arrow (found " + arrows.size() + ")");
        AbstractArrow arrow = arrows.get(0);
        arrow.discard();
        return arrow;
    }

    /** Charges a crossbow for {@code ticks} ticks and lets go; returns whether it is loaded. */
    private static boolean loadCrossbow(ServerPlayer player, int ticks) {
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        for (int i = 0; i < ticks; i++) {
            player.doTick();
        }
        player.releaseUsingItem();
        return CrossbowItem.isCharged(crossbow);
    }

    @GameTest(template = ARENA, batch = "quicksilver_slow_blocks", timeoutTicks = 100)
    public static void quicksilverTimeSlowsTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 6);
        ServerPlayer peter = f.player(2.5, FLOOR, 10.5, Power.QUICKSILVER);
        press(peter, 1);
        run(peter, true);

        // Two blocks of sand dropped from the same height: one next to him, one far outside the slow motion.
        Vec3 nearPos = helper.absoluteVec(new Vec3(5.5, 8.0, 10.5));
        Vec3 farPos = helper.absoluteVec(new Vec3(19.5, 8.0, 10.5));
        FallingBlockEntity near = fallingSand(helper, nearPos);
        FallingBlockEntity far = fallingSand(helper, farPos);
        // Another player standing next to him is slowed down instead.
        ServerPlayer friend = f.player(4.5, FLOOR, 12.5);

        f.everyTick(15, tick -> {
            run(peter, true);
            if (tick < 15) {
                return false;
            }
            double nearFell = nearPos.y - near.getY();
            double farFell = farPos.y - far.getY();
            check(farFell > 2.5, "the far block falls normally (fell " + farFell + ")");
            check(nearFell < farFell * 0.4, "the block next to Quicksilver falls in slow motion (fell " + nearFell + " vs " + farFell + ")");
            MobEffectInstance slowness = friend.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            check(slowness != null && slowness.getAmplifier() >= 3, "other players nearby are slowed down");
            return true;
        });
    }

    private static FallingBlockEntity fallingSand(GameTestHelper helper, Vec3 pos) {
        FallingBlockEntity sand = EntityType.FALLING_BLOCK.create(helper.getLevel());
        check(sand != null, "falling block");
        sand.moveTo(pos.x, pos.y, pos.z);
        sand.setNoGravity(false);
        sand.dropItem = false;
        helper.getLevel().addFreshEntity(sand);
        // FallingBlockEntity defaults to sand when created this way.
        return sand;
    }

    @GameTest(template = ARENA, batch = "quicksilver_arrows", timeoutTicks = 60)
    public static void arrowsHangInTheAir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 8);
        ServerPlayer peter = f.player(2.5, FLOOR, 10.5, Power.QUICKSILVER);
        press(peter, 1);
        run(peter, true);

        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        Vec3 start = helper.absoluteVec(new Vec3(4.5, 6.0, 4.5));
        arrow.moveTo(start.x, start.y, start.z);
        arrow.setDeltaMovement(0, 0, 1.0);
        double baseDamage = arrow.getBaseDamage();
        helper.getLevel().addFreshEntity(arrow);
        int factor = EnhancedConfig.SLOW_MOTION_FACTOR.get();

        double[] speedAt3 = new double[1];
        f.everyTick(30, tick -> {
            if (tick <= 8) {
                run(peter, true);
                if (tick == 3) {
                    assertNear(arrow.getDeltaMovement().z, 1.0 / factor, 0.05, "the arrow crawls at 1/" + factor + " speed");
                    check(arrow.isNoGravity(), "a slowed arrow doesn't drop out of the air");
                    assertNear(arrow.getBaseDamage(), baseDamage, 1.0E-6, "its damage is untouched");
                    speedAt3[0] = arrow.getDeltaMovement().z;
                }
                if (tick == 8) {
                    // Five slowed ticks are one normal tick: the air slows it down by 1% (x0.99), not 5% (x0.99^5).
                    double kept = arrow.getDeltaMovement().z / speedAt3[0];
                    assertNear(kept, Math.pow(0.99, 5.0 / factor), 0.002, "its air drag is spread over the slowed time");
                }
                return false;
            }
            if (tick == 9) {
                // Hitting something gives it its full speed back first, so it hits as hard as it would have.
                SlowMotion.unslow(arrow);
                check(arrow.getDeltaMovement().z > 0.7, "a slowed arrow hits at full speed (" + arrow.getDeltaMovement().z + ")");
                return false;
            }
            if (tick == 10) {
                press(peter, 1);
            }
            run(peter, false);
            if (tick == 11) {
                check(arrow.getDeltaMovement().z > 0.7, "the arrow speeds up again when Quicksilver Time ends (" + arrow.getDeltaMovement().z + ")");
                check(!arrow.isNoGravity(), "and falls normally again");
                assertNear(arrow.getBaseDamage(), baseDamage, 1.0E-6, "and has its normal damage back");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "quicksilver_trident", timeoutTicks = 40)
    public static void returningTridentLeavesTheSlowMotion(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 8);
        ServerPlayer peter = f.player(2.5, FLOOR, 10.5, Power.QUICKSILVER);
        press(peter, 1);
        run(peter, true);

        // A volley of arrows and two Loyalty tridents, all slowed down together. (Two tridents turning back in the same tick
        // make sure one of them is let go while others are still being looked at.)
        List<Arrow> arrows = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
            Vec3 at = helper.absoluteVec(new Vec3(3.5 + (i % 4), 5.0 + i / 4, 6.5));
            arrow.moveTo(at.x, at.y, at.z);
            arrow.setDeltaMovement(0, 0, 0.5);
            helper.getLevel().addFreshEntity(arrow);
            arrows.add(arrow);
        }
        List<ThrownTrident> tridents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ItemStack tridentItem = new ItemStack(Items.TRIDENT);
            tridentItem.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOYALTY), 3);
            ThrownTrident trident = new ThrownTrident(helper.getLevel(), peter, tridentItem);
            Vec3 at = helper.absoluteVec(new Vec3(4.5 + 2 * i, 6.0, 8.5));
            trident.moveTo(at.x, at.y, at.z);
            trident.setDeltaMovement(0, 0, 0.5);
            helper.getLevel().addFreshEntity(trident);
            tridents.add(trident);
        }

        f.everyTick(20, tick -> {
            run(peter, true);
            if (tick == 2) {
                check(tridents.stream().allMatch(ThrownTrident::isNoGravity) && arrows.stream().allMatch(Arrow::isNoGravity),
                        "everything in the air is slowed");
                // Loyalty kicks in: the tridents turn around and fly home.
                tridents.forEach(t -> t.setNoPhysics(true));
            }
            if (tick == 4) {
                // The level ticks in between ran without a crash while the trident was let go among the slowed arrows.
                check(tridents.stream().allMatch(t -> !t.isRemoved() && !t.isNoGravity()), "the returning tridents are no longer slowed");
                check(arrows.stream().allMatch(a -> !a.isRemoved() && a.isNoGravity()), "the arrows are still slowed");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA)
    public static void walkmanTogglesTheSong(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        PowerData data = Powers.get(peter);
        check(data.walkman(), "the Walkman is on from the start");
        AbilityHandler.onKey(peter, 5, true);
        AbilityHandler.onKey(peter, 5, false);
        check(!data.walkman(), "N turns the song off");
        AbilityHandler.onKey(peter, 5, true);
        check(data.walkman(), "and on again");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void speedBlitzAndPickpocket(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(2.5, FLOOR, 10.5, Power.QUICKSILVER);

        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        lookAt(peter, zombie);

        // Ability 3: the sword is in his hands before the zombie knows it.
        AbilityHandler.onKey(peter, 3, true);
        check(zombie.getMainHandItem().isEmpty(), "Pickpocket empties the zombie's hand");
        check(peter.getInventory().contains(new ItemStack(Items.IRON_SWORD)), "and the sword is in Quicksilver's inventory");

        // Ability 2: next to the zombie in an instant, six fast punches.
        float health = zombie.getHealth();
        AbilityHandler.onKey(peter, 2, true);
        check(peter.distanceTo(zombie) < 2.5, "Speed Blitz puts him right next to the target (" + peter.distanceTo(zombie) + ")");
        check(health - zombie.getHealth() > 9.0F, "and lands every punch (took " + (health - zombie.getHealth()) + ")");
        check(!Powers.get(peter).isReady(2, helper.getLevel().getGameTime()), "then it needs to cool down");
        helper.succeed();
    }
}

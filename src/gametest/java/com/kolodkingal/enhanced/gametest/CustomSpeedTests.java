package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MagnetPull;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.ability.SpeedStrikes;
import com.kolodkingal.enhanced.ability.SpeedSurfaces;
import com.kolodkingal.enhanced.ability.SurfaceRun;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.kolodkingal.enhanced.config.SpeedConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroDamage;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.network.SurfaceRunPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Speed and metal as a custom hero's powers (3.4, lane L2): every ability of Quicksilver, Tommy, Vision and Magneto works
 * for a custom hero who picked it, on a key that is not its hero's (its cooldown rests on that key, scaled by its
 * strength), its Sneak variant on the Sneak cell, and only the upkeep of the abilities he has runs (a custom hero with
 * Tommy's run but not his slow time never slows the world). Batches {@code custom_speed}, {@code custom_speed_slow} (a
 * small slow-motion radius), {@code custom_speed_shield} (the shield never throws back) and {@code custom_speed_surface}.
 */
public final class CustomSpeedTests {
    private static final String BATCH = "custom_speed";
    private static final List<Power> HEROES = List.of(Power.MAGNETO, Power.QUICKSILVER, Power.VISION, Power.TOMMY);

    /** Sprints (or not) and runs his tick, as a connected client's tick would. */
    private static void run(ServerPlayer player, boolean sprinting) {
        player.setSprinting(sprinting);
        CustomKitSupport.tickN(player, 1);
    }

    private static long now(GameTestHelper helper) {
        return helper.getLevel().getGameTime();
    }

    /** A zombie with a helmet (no sunburn), standing still. */
    private static Zombie zombie(GameTestHelper helper, double x, double z) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        return zombie;
    }

    // ---- the catalogue ----

    /** Every row of the four heroes has its name and line (and its Sneak variant's name iff it has one) in both languages. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyRowOfSpeedAndMetalIsNamedInBothLanguages(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        List<String> missing = new ArrayList<>();
        for (String name : List.of("en_us", "he_il")) {
            Path file = mod.findResource("assets", Enhanced.MODID, "lang", name + ".json");
            JsonObject lang = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Power hero : HEROES) {
                for (HeroAbility a : HeroAbility.of(hero)) {
                    for (String key : List.of(a.nameKey(), a.descKey())) {
                        if (!lang.has(key)) {
                            missing.add(name + ": " + key);
                        }
                    }
                    if (lang.has(a.altKey()) != a.hasAlt()) {
                        missing.add(name + ": " + a.altKey() + (a.hasAlt() ? " missing" : " for a row without a variant"));
                    }
                }
            }
        }
        check(missing.isEmpty(), "names: " + missing);
        check(HeroAbility.byId("vision.body") == HeroAbility.VISION_BODY && HeroAbility.VISION_BODY.passive()
                && HeroAbility.VISION_BODY.traits().contains(Trait.METAL_BODY), "Vision's synthetic body is a row (a body, metal)");
        helper.succeed();
    }

    /** For the built-in heroes nothing changes: their own colours, their own slow time, their own walls. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBuiltInSpeedstersAreUnchanged(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(3.5, FLOOR, 3.5, Power.QUICKSILVER);
        ServerPlayer tommy = f.player(6.5, FLOOR, 3.5, Power.TOMMY);
        check(SpeedStrikes.Style.of(peter, Power.QUICKSILVER).equals(SpeedStrikes.Style.of(Power.QUICKSILVER)), "Quicksilver's own colours");
        check(SpeedStrikes.Style.of(tommy, Power.TOMMY).equals(SpeedStrikes.Style.of(Power.TOMMY)), "Tommy's own colours");
        check(SlowMotion.slowTimeOf(Powers.get(peter)) == Power.QUICKSILVER && SlowMotion.slowTimeOf(Powers.get(tommy)) == Power.TOMMY,
                "their own slow times");
        check(SpeedSurfaces.isSpeedster(Powers.get(peter)) && SpeedSurfaces.isSpeedster(Powers.get(tommy)), "both run on walls");
        for (Power p : Power.values()) {
            if (p != Power.QUICKSILVER && p != Power.TOMMY) {
                PowerData data = CustomKitSupport.data(p);
                check(!SpeedSurfaces.isSpeedster(data) && SlowMotion.slowTimeOf(data) == Power.NONE, p + " is no speedster");
            }
        }
        ServerPlayer custom = CustomKitSupport.hero(f, 9.5, 3.5, CustomKitSupport.def().pick("tommy.vibrate", 5).key(1, "tommy.vibrate").build());
        SpeedStrikes.Style own = SpeedStrikes.Style.of(custom, Power.TOMMY);
        check(own.color() == 0x3FA9F5 && own.glow() == 0xE0F4FF, "a custom hero strikes in his own colours: " + own);
        helper.succeed();
    }

    // ---- Quicksilver ----

    /** The Speed Blitz on N strikes and rests on N; the Pickpocket (Sneak + G of another hero) snatches and rests on G. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void quicksilversBlitzAndPickpocketWorkOnAnyKey(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer blitzer = CustomKitSupport.hero(f, 3.5, 4.5, CustomKitSupport.def()
                .pick("quicksilver.blitz", 5).key(5, "quicksilver.blitz").build());
        check(CustomHeroes.hero(blitzer).binding(5, true).equals("quicksilver.blitz#alt"), "Sneak + N holds the Pickpocket");
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(12.5, FLOOR, 4.5));
        lookAt(blitzer, golem);
        float health = golem.getHealth();
        CustomKitSupport.tap(blitzer, 5, false);
        PowerData data = Powers.get(blitzer);
        check(golem.getHealth() < health, "N blitzes the golem");
        check(blitzer.distanceTo(golem) < 3.0, "he dashed to its side");
        check(data.cooldownUntil(5) == now(helper) + QuicksilverAbilities.BLITZ_COOLDOWN, "it rests on N, not on his V");
        check(data.isReady(3, now(helper)), "V is free");

        ServerPlayer thief = CustomKitSupport.hero(f, 3.5, 16.5, CustomKitSupport.def()
                .pick("quicksilver.blitz", 5).key(2, "quicksilver.blitz").build());
        Zombie zombie = zombie(helper, 12.5, 16.5);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        lookAt(thief, zombie);
        CustomKitSupport.tap(thief, 2, true);
        check(zombie.getMainHandItem().isEmpty() && thief.getInventory().countItem(Items.IRON_SWORD) == 1, "Sneak + G picks its pocket");
        check(Powers.get(thief).cooldownUntil(2) == now(helper) + QuicksilverAbilities.PICKPOCKET_COOLDOWN, "and rests on G");
        helper.succeed();
    }

    /** Super Speed on B runs only while it is on (Live.SPEEDING and the speed); Sneak + B dashes, resting on B, scaled. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void quicksilversSuperSpeedRunsOnlyWhileOnAndDashesOnSneak(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = CustomKitSupport.hero(f, 3.5, 10.5, CustomKitSupport.def()
                .pick("quicksilver.speed", 10).key(4, "quicksilver.speed").build());
        look(p, EAST, 0.0F);
        PowerData data = Powers.get(p);
        double walking = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        run(p, true);
        check(!data.is(Live.SPEEDING), "off: sprinting is only sprinting");
        CustomKitSupport.tap(p, 4, false);
        check(data.superSpeed(), "B turns Super Speed on");
        run(p, true);
        check(data.is(Live.SPEEDING) && QuicksilverAbilities.isRunning(p) && QuicksilverAbilities.isQuick(p), "on: he runs, and does all faster");
        double bonus = EnhancedConfig.QUICKSILVER_SPEED_BONUS.get();
        assertNear(p.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 1.3 * (1 + bonus), 1.0E-4, "at Quicksilver's speed");
        check(p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED) > 1.5, "he mines faster");
        check(!data.slowMo(), "Super Speed slows nothing");
        CustomKitSupport.tap(p, 4, false);
        run(p, true);
        check(!data.superSpeed() && !data.is(Live.SPEEDING), "B again: off");
        assertNear(p.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 1.3, 1.0E-4, "a sprint again");
        check(p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED) < 1.0001, "and the quick hands are gone");
        run(p, false);

        double x = p.getX();
        CustomKitSupport.tap(p, 4, true);
        check(p.getX() > x + 1.0, "Sneak + B dashes ahead");
        check(!data.superSpeed(), "and does not touch the switch");
        // Strength 10: the cooldown is divided by 1.5.
        check(data.cooldownUntil(4) == now(helper) + Math.round(SpeedStrikes.value(SpeedConfig.DASH_COOLDOWN, 100) / 1.5F), "the dash rests on B, shorter at strength 10: "
                + (data.cooldownUntil(4) - now(helper)));
        check(data.isReady(2, now(helper)), "not on his G");
        helper.succeed();
    }

    /** Quicksilver Time on G slows a creature next to him and not one far away; Sneak + G is the Walkman. */
    @EnhancedTest(template = ARENA, batch = "custom_speed_slow", timeoutTicks = 100)
    public static void quicksilverTimeSlowsTheWorldForACustomHero(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 6);
        ServerPlayer p = CustomKitSupport.hero(f, 2.5, 10.5, CustomKitSupport.def()
                .pick("quicksilver.time", 5).key(2, "quicksilver.time").build());
        PowerData data = Powers.get(p);
        Zombie near = timedZombie(helper, 5.5);
        Zombie far = timedZombie(helper, 19.5);
        CustomKitSupport.tap(p, 2, false);
        check(data.quicksilverTime(), "G turns Quicksilver Time on");
        boolean walkman = data.walkman();
        CustomKitSupport.tap(p, 2, true);
        check(data.walkman() != walkman && data.quicksilverTime(), "Sneak + G switches the Walkman, and the time stays");
        f.everyTick(30, tick -> {
            run(p, false);
            if (tick < 30) {
                return false;
            }
            check(data.slowMo() && SlowMotion.slowsTime(data), "his slow time runs");
            check(SlowMotion.zoneOwner(helper.getLevel(), near.position()) == Power.QUICKSILVER, "in Quicksilver's shade");
            check(ticked(far) >= 28, "the far zombie ticks normally (" + ticked(far) + ")");
            check(ticked(near) <= 10, "the zombie next to him is slowed (" + ticked(near) + ")");
            return true;
        });
    }

    private static final int EFFECT_TICKS = 400;

    private static Zombie timedZombie(GameTestHelper helper, double x) {
        Zombie zombie = zombie(helper, x, 10.5);
        zombie.addEffect(new MobEffectInstance(MobEffects.LUCK, EFFECT_TICKS));
        return zombie;
    }

    private static int ticked(Zombie zombie) {
        return EFFECT_TICKS - zombie.getEffect(MobEffects.LUCK).getDuration();
    }

    /** The Pebble Barrage on V; and a hero with only that gets none of Quicksilver's upkeep (no run, no slow time). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onlyThePickedAbilitiesUpkeepRuns(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = CustomKitSupport.hero(f, 3.5, 10.5, CustomKitSupport.def()
                .pick("quicksilver.barrage", 5).key(3, "quicksilver.barrage").pick("tommy.accelerate", 5).key(1, "tommy.accelerate").build());
        look(p, EAST, 0.0F);
        PowerData data = Powers.get(p);
        check(data.acts(Power.QUICKSILVER) && data.acts(Power.TOMMY), "both speedsters' code runs for him");
        check(!data.toggle(Toggle.SUPER_SPEED), "no Tommy run picked: it does not start on");
        // Even with the switches of the others set, nothing of theirs runs.
        data.setSuperSpeed(true);
        data.setQuicksilverTime(true);
        data.setToggle(Toggle.SUPER_SPEED, true);
        data.setToggle(Toggle.TOMMY_TIME, true);
        p.getFoodData().setFoodLevel(10);
        double walking = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        run(p, true);
        run(p, true);
        check(!data.is(Live.SPEEDING) && !data.slowMo() && !SlowMotion.slowsTime(data), "no run and no slow time");
        check(!QuicksilverAbilities.isQuick(p) && !QuicksilverAbilities.isRunning(p) && !TommyAbilities.isRunning(p), "nor their speed");
        assertNear(p.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 1.3, 1.0E-4, "his sprint is only a sprint");
        check(p.getFoodData().getFoodLevel() == 10, "and no synthetic body feeds him");
        check(!SpeedSurfaces.isSpeedster(data), "he does not run on walls");
        CustomKitSupport.tap(p, 3, false);
        check(data.cooldownUntil(3) == now(helper) + SpeedStrikes.value(SpeedConfig.BARRAGE_COOLDOWN, 120), "V flings the pebbles and rests on V");
        check(data.isReady(5, now(helper)), "not on his N");
        helper.succeed();
    }

    /** Twelve hits of the blitz at strength 2 and at strength 9: the stronger hurts more (HeroDamage, SPEED_BLITZ). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theSpeedStrikesScaleWithStrength(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        float[] lost = new float[2];
        int[] strengths = {2, 9};
        for (int i = 0; i < 2; i++) {
            double z = 4.5 + i * 12;
            ServerPlayer p = CustomKitSupport.hero(f, 3.5, z, CustomKitSupport.def().pick("quicksilver.blitz", strengths[i])
                    .key(1, "quicksilver.blitz").build());
            IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(12.5, FLOOR, z));
            lookAt(p, golem);
            float health = golem.getHealth();
            CustomKitSupport.tap(p, 1, false);
            lost[i] = health - golem.getHealth();
            check(HeroDamage.abilityOf(ModDamageTypes.SPEED_BLITZ, CustomKitSupport.kit(p)) == HeroAbility.QUICKSILVER_BLITZ, "his blitz deals it");
        }
        check(lost[0] > 0 && lost[1] > lost[0] * 1.5F, "strength 2 hurts less than 9: " + lost[0] + " / " + lost[1]);
        helper.succeed();
    }

    // ---- Tommy ----

    /** Tommy's run starts on and runs at his speed; without his slow time the world never slows; Sneak + R dashes. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void tommysSpeedWithoutHisTimeNeverSlowsTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = CustomKitSupport.hero(f, 3.5, 10.5, CustomKitSupport.def().pick("tommy.speed", 5).key(1, "tommy.speed").build());
        look(p, EAST, 0.0F);
        PowerData data = Powers.get(p);
        check(data.toggle(Toggle.SUPER_SPEED), "Tommy's run starts on");
        double walking = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        run(p, true);
        check(data.is(Live.SPEEDING) && TommyAbilities.isRunning(p), "he runs");
        assertNear(p.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 1.3 * (1 + EnhancedConfig.TOMMY_SPEED_BONUS.get()), 1.0E-4,
                "at Tommy's speed");
        data.setToggle(Toggle.TOMMY_TIME, true);
        for (int i = 0; i < 5; i++) {
            run(p, true);
            check(!data.slowMo() && !SlowMotion.slowsTime(data), "no slow time without the row (tick " + i + ")");
        }
        check(SlowMotion.slowTimeOf(data) == Power.NONE, "no slow time is his");
        CustomKitSupport.tap(p, 1, false);
        run(p, true);
        check(!data.toggle(Toggle.SUPER_SPEED) && !data.is(Live.SPEEDING), "R switches the run off");
        run(p, false);
        double x = p.getX();
        CustomKitSupport.tap(p, 1, true);
        check(p.getX() > x + 1.0 && data.cooldownUntil(1) == now(helper) + SpeedStrikes.value(SpeedConfig.DASH_COOLDOWN, 100), "Sneak + R dashes and rests on R");
        helper.succeed();
    }

    /** Tommy's slow time on V slows the world; Sneak + V switches his Wall Run, not the time. */
    @EnhancedTest(template = ARENA, batch = "custom_speed_slow", timeoutTicks = 100)
    public static void tommysSlowTimeOnAnotherKey(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 6);
        ServerPlayer p = CustomKitSupport.hero(f, 2.5, 10.5, CustomKitSupport.def().pick("tommy.time", 5).key(3, "tommy.time").build());
        PowerData data = Powers.get(p);
        Zombie near = timedZombie(helper, 5.5);
        CustomKitSupport.tap(p, 3, false);
        check(data.toggle(Toggle.TOMMY_TIME), "V turns his slow time on");
        check(SpeedSurfaces.isSpeedster(data) && SpeedSurfaces.toggleOn(data), "his Wall Run is on");
        CustomKitSupport.tap(p, 3, true);
        check(data.toggle(Toggle.TOMMY_WALL_RUN_OFF) && data.toggle(Toggle.TOMMY_TIME), "Sneak + V switches the Wall Run off, not the time");
        f.everyTick(30, tick -> {
            run(p, false);
            if (tick < 30) {
                return false;
            }
            check(data.slowMo() && SlowMotion.zoneOwner(helper.getLevel(), near.position()) == Power.TOMMY, "Tommy's slow time");
            check(ticked(near) <= 10, "the zombie next to him is slowed (" + ticked(near) + ")");
            return true;
        });
    }

    /** The Whirlwind on N, the Sonic Boom on Sneak + V, the Vibrate on G, the Disarm Blitz on Sneak + B, the Acceleration on R. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void tommysStrikesWorkOnAnyKey(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer whirl = CustomKitSupport.hero(f, 3.5, 2.5, CustomKitSupport.def().pick("tommy.whirlwind", 5).key(5, "tommy.whirlwind").build());
        CustomKitSupport.tap(whirl, 5, false);
        check(Powers.get(whirl).is(Live.WHIRLWIND) && TommyAbilities.hasWhirlwind(whirl), "N: the Whirlwind");
        check(Powers.get(whirl).cooldownUntil(5) == now(helper) + TommyAbilities.WHIRLWIND_COOLDOWN, "resting on N");
        CustomKitSupport.tap(whirl, 5, true);
        check(!Powers.get(whirl).is(Live.SONIC_BOOM), "no Sonic Boom while it rests");
        CustomKitSupport.tickN(whirl, TommyAbilities.WHIRLWIND_TICKS + 1);
        check(!Powers.get(whirl).is(Live.WHIRLWIND) && !TommyAbilities.hasWhirlwind(whirl), "the Whirlwind ends by itself");

        ServerPlayer boom = CustomKitSupport.hero(f, 17.5, 2.5, CustomKitSupport.def().pick("tommy.whirlwind", 5).key(3, "tommy.whirlwind").build());
        CustomKitSupport.tap(boom, 3, true);
        check(Powers.get(boom).is(Live.SONIC_BOOM) && Powers.get(boom).cooldownUntil(3) == now(helper) + SpeedStrikes.value(SpeedConfig.BOOM_COOLDOWN, 160), "Sneak + V: the Sonic Boom, on V");

        ServerPlayer vibrate = CustomKitSupport.hero(f, 4.5, 10.5, CustomKitSupport.def().pick("tommy.vibrate", 5).key(2, "tommy.vibrate").build());
        look(vibrate, EAST, 0.0F);
        for (int x = 5; x <= 7; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 10), Blocks.STONE);
            }
        }
        CustomKitSupport.tap(vibrate, 2, false);
        Vec3 rel = helper.relativeVec(vibrate.position());
        check(Math.abs(rel.x - 8.5) < 0.01, "G: through the wall: " + rel);
        check(Powers.get(vibrate).cooldownUntil(2) == now(helper) + TommyAbilities.VIBRATE_COOLDOWN, "resting on G");

        ServerPlayer disarmer = CustomKitSupport.hero(f, 3.5, 17.5, CustomKitSupport.def().pick("tommy.vibrate", 5).key(4, "tommy.vibrate").build());
        Zombie armed = zombie(helper, 6.5, 17.5);
        armed.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        CustomKitSupport.tap(disarmer, 4, true);
        check(armed.getMainHandItem().isEmpty() && disarmer.getInventory().countItem(Items.IRON_SWORD) == 1, "Sneak + B: the Disarm Blitz");
        check(Powers.get(disarmer).cooldownUntil(4) == now(helper) + TommyAbilities.BLITZ_COOLDOWN, "resting on B");

        ServerPlayer accel = CustomKitSupport.hero(f, 12.5, 14.5, CustomKitSupport.def().pick("tommy.accelerate", 5).key(1, "tommy.accelerate").build());
        BlockPos stone = new BlockPos(15, 2, 14);
        helper.setBlock(stone, Blocks.STONE);
        lookAt(accel, helper.absoluteVec(Vec3.atCenterOf(stone)));
        CustomKitSupport.tap(accel, 1, false);
        helper.assertBlockPresent(Blocks.AIR, stone);
        check(Powers.get(accel).cooldownUntil(1) == now(helper) + TommyAbilities.ACCELERATE_COOLDOWN, "R shatters it and rests on R");
        helper.succeed();
    }

    // ---- Vision ----

    /** Vision's Phase on R passes through walls and floats him (without Flight); his Density on G stops his Flight. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void visionsPhasePassesWallsAndDensityStopsFlight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer ghost = CustomKitSupport.hero(f, 4.5, 4.5, CustomKitSupport.def().pick("vision.phase", 5).key(1, "vision.phase").build());
        PowerData data = Powers.get(ghost);
        check(!ghost.mayFly(), "no Flight: he walks");
        CustomKitSupport.tap(ghost, 1, false);
        check(data.visionPhased() && VisionAbilities.isPhased(ghost), "R: he phases");
        CustomKitSupport.tickN(ghost, 2);
        check(ghost.mayFly() && ghost.getAbilities().flying, "and floats, as Vision does");
        ghost.doTick();
        check(ghost.noPhysics, "no collisions: he passes through walls");
        float health = ghost.getHealth();
        ghost.hurt(helper.getLevel().damageSources().generic(), 10.0F);
        assertNear(ghost.getHealth(), health, 1.0E-6, "nothing hurts him");
        CustomKitSupport.tap(ghost, 1, false);
        check(!data.visionPhased(), "R again: solid");
        CustomKitSupport.tickN(ghost, 1);
        check(!ghost.mayFly() && !ghost.getAbilities().flying, "and on his feet again");

        ServerPlayer heavy = CustomKitSupport.hero(f, 12.5, 12.5, CustomKitSupport.def().pick("flight", 5).key(5, "flight")
                .pick("vision.density", 5).key(2, "vision.density").build());
        CustomKitSupport.tickN(heavy, 1);
        check(heavy.mayFly(), "his Flight flies");
        double armor = heavy.getAttributeValue(Attributes.ARMOR);
        CustomKitSupport.tap(heavy, 2, false);
        CustomKitSupport.tickN(heavy, 1);
        check(Powers.get(heavy).dense() && VisionAbilities.isDense(heavy), "G: dense");
        assertNear(heavy.getAttributeValue(Attributes.ARMOR), armor + 12.0, 1.0E-6, "much harder");
        check(!heavy.mayFly(), "too heavy to fly");
        CustomKitSupport.tap(heavy, 2, false);
        CustomKitSupport.tickN(heavy, 1);
        check(heavy.mayFly() && !Powers.get(heavy).dense(), "light again, he flies");
        helper.succeed();
    }

    /** The Mind Stone beam held on B; the synthetic body only with its row, scaled; the disguise on N. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void visionsBeamBodyAndDisguise(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer beamer = CustomKitSupport.hero(f, 3.5, 4.5, CustomKitSupport.def().pick("vision.mind_beam", 5).key(4, "vision.mind_beam")
                .pick("vision.disguise", 5).key(5, "vision.disguise").build());
        Zombie zombie = zombie(helper, 10.5, 4.5);
        lookAt(beamer, zombie);
        PowerData data = Powers.get(beamer);
        beamer.getFoodData().setFoodLevel(10);
        float health = zombie.getHealth();
        CustomKitSupport.press(beamer, 4, false, true);
        check(data.beamActive(), "holding B: the beam");
        CustomKitSupport.tickN(beamer, 10);
        check(zombie.getHealth() < health, "it burns the zombie");
        CustomKitSupport.press(beamer, 4, false, false);
        check(!data.beamActive(), "let go: it stops");
        check(beamer.getFoodData().getFoodLevel() == 10 && beamer.getMaxHealth() == 20.0F, "no synthetic body without its row");
        check(!Traits.has(beamer, Trait.METAL_BODY), "nor is he metal");
        CustomKitSupport.tap(beamer, 5, false);
        check(data.disguise(), "N: the disguise");

        ServerPlayer synth = CustomKitSupport.hero(f, 3.5, 14.5, CustomKitSupport.def().pick("vision.body", 10).build());
        synth.getFoodData().setFoodLevel(10);
        synth.setAirSupply(3);
        CustomKitSupport.tickN(synth, 1);
        assertNear(synth.getMaxHealth(), 20.0 + 10.0 * 1.5, 1.0E-4, "+10 health, x1.5 at strength 10");
        check(synth.getFoodData().getFoodLevel() == 20 && synth.getAirSupply() == synth.getMaxAirSupply(), "never hungry nor out of breath");
        check(Traits.has(synth, Trait.METAL_BODY), "and metal");
        helper.succeed();
    }

    // ---- Magneto ----

    /** Sneak + G with nothing held: the Magnetic Pull, resting on G; plain G grips metal; Sneak + G sets it down. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void magnetosPullRestsOnTheCustomKey(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos ore = new BlockPos(14, 1, 10);
        helper.setBlock(ore, Blocks.IRON_ORE);
        ServerPlayer m = CustomKitSupport.hero(f, 10.5, 10.5, CustomKitSupport.def().pick("magneto.metal", 5).key(2, "magneto.metal").build());
        look(m, EAST, 0.0F);
        check(CustomHeroes.hero(m).binding(2, true).equals("magneto.metal#alt"), "Sneak + G holds the pull");
        CustomKitSupport.tap(m, 2, true);
        check(MagnetPull.isPulling(m), "Sneak + G: the pull");
        f.everyTick(40, tick -> {
            CustomKitSupport.tickN(m, 1);
            if (MagnetPull.isPulling(m)) {
                return false;
            }
            PowerData data = Powers.get(m);
            check(helper.getBlockState(ore).is(Blocks.STONE) && m.getInventory().countItem(Items.RAW_IRON) == 1, "the ore's iron came");
            check(!data.isReady(2, now(helper)) && data.isReady(1, now(helper)), "the pull rests on G, not on his R");
            return true;
        });
    }

    /** The Strip on N, the Shrapnel held on B (it melts his iron), the grip of metal on V. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void magnetosStripShrapnelAndGripOnOtherKeys(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer stripper = CustomKitSupport.hero(f, 3.5, 3.5, CustomKitSupport.def().pick("magneto.disarm", 5).key(5, "magneto.disarm").build());
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 3.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        lookAt(stripper, zombie);
        CustomKitSupport.tap(stripper, 5, false);
        check(zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "N tears the iron helmet off");
        check(Powers.get(stripper).cooldownUntil(5) == now(helper) + EnhancedConfig.DISARM_COOLDOWN.get(), "and rests on N");

        ServerPlayer storm = CustomKitSupport.hero(f, 3.5, 10.5, CustomKitSupport.def().pick("magneto.shrapnel", 5).key(4, "magneto.shrapnel").build());
        look(storm, EAST, 0.0F);
        storm.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT));
        CustomKitSupport.press(storm, 4, false, true);
        check(Powers.get(storm).shrapnelActive(), "holding B: the storm");
        storm.tickCount = -1; // (the storm fires on every interval-th tick; tickN counts up first)
        CustomKitSupport.tickN(storm, 1);
        check(helper.getEntities(ModEntities.IRON_SHRAPNEL.get()).size() == 1 && Powers.get(storm).ironCharge() == 8, "a shard of his ingot flies");
        CustomKitSupport.press(storm, 4, false, false);
        check(!Powers.get(storm).shrapnelActive(), "let go: it stops");

        ServerPlayer gripper = CustomKitSupport.hero(f, 3.5, 17.5, CustomKitSupport.def().pick("magneto.metal", 5).key(3, "magneto.metal").build());
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(8.5, FLOOR, 17.5));
        lookAt(gripper, golem);
        CustomKitSupport.tap(gripper, 3, false);
        check(Telekinesis.isHolding(gripper), "V grips the iron golem");
        CustomKitSupport.tap(gripper, 3, true);
        check(!Telekinesis.isHolding(gripper) && !MagnetPull.isPulling(gripper),
                "Sneak + V while holding sets it down (no pull)");
        helper.succeed();
    }

    /** The Magnetic Shield on V catches a metal arrow shot at a custom hero. */
    @EnhancedTest(template = ARENA, batch = "custom_speed_shield", timeoutTicks = 60)
    public static void magnetosShieldCatchesMetalArrows(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SHIELD_REFLECT_CHANCE, 0.0);
        ServerPlayer m = CustomKitSupport.hero(f, 14.5, 10.5, CustomKitSupport.def().pick("magneto.shield", 5).key(3, "magneto.shield").build());
        CustomKitSupport.tap(m, 3, false);
        check(Powers.get(m).magneticShield() && MagnetoAbilities.hasShieldUp(m), "V raises the Magnetic Shield");
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(3.5, FLOOR, 10.5));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        skeleton.setNoAi(true);
        Arrow arrow = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), null);
        Vec3 dir = m.position().add(0, 0.9, 0).subtract(arrow.position());
        arrow.shoot(dir.x, dir.y, dir.z, 2.5F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
        float max = m.getHealth();
        f.everyTick(50, tick -> {
            CustomKitSupport.tickN(m, 1);
            check(m.getHealth() >= max, "the arrow must not hurt him");
            return MagnetoAbilities.isHeld(arrow);
        });
    }

    // ---- walls ----

    private static void report(ServerPlayer player, Direction normal) {
        SpeedSurfaces.onReport(player, SurfaceRunPayload.WALL, normal.get3DDataValue());
    }

    /** Running on walls comes with Quicksilver's Wall Run (switched by its key) or Tommy's slow time with its variant. */
    @EnhancedTest(template = ARENA, batch = "custom_speed_surface")
    public static void wallRunComesWithItsRows(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (int y = 2; y <= 8; y++) {
            for (int z = 8; z <= 12; z++) {
                helper.setBlock(new BlockPos(12, y, z), Blocks.STONE);
            }
        }
        ServerPlayer peter = f.player(11.7, 4.0, 10.5);
        CustomKitSupport.become(peter, CustomKitSupport.def().pick("quicksilver.speed", 5).key(2, "quicksilver.speed")
                .pick("quicksilver.wall_run", 5).key(4, "quicksilver.wall_run").build());
        PowerData data = Powers.get(peter);
        check(SpeedSurfaces.isSpeedster(data) && data.wallRun() && SpeedSurfaces.toggleOn(data), "Wall & Water Run: on");
        report(peter, Direction.WEST);
        CustomKitSupport.tickN(peter, 1);
        check(data.is(Live.SURFACE_WALL) && SpeedSurfaces.fallSafe(peter), "his word is believed");
        CustomKitSupport.tap(peter, 4, false);
        check(!data.wallRun(), "B switches it off");
        CustomKitSupport.tickN(peter, 1);
        check(!data.is(Live.SURFACE_WALL), "and then he runs on nothing");

        ServerPlayer blitzer = f.player(11.7, 4.0, 9.5);
        CustomKitSupport.become(blitzer, CustomKitSupport.def().pick("quicksilver.blitz", 5).key(1, "quicksilver.blitz").build());
        report(blitzer, Direction.WEST);
        CustomKitSupport.tickN(blitzer, 1);
        check(!SpeedSurfaces.isSpeedster(Powers.get(blitzer)) && !Powers.get(blitzer).is(Live.SURFACE_WALL), "the blitz alone runs on no wall");

        ServerPlayer tommy = f.player(11.7, 4.0, 11.5);
        CustomKitSupport.become(tommy, CustomKitSupport.def().pick("tommy.time", 5).key(1, "tommy.time").build());
        PowerData tommyData = Powers.get(tommy);
        check(SpeedSurfaces.isSpeedster(tommyData) && SpeedSurfaces.toggleOn(tommyData), "Tommy's Wall Run comes with his slow time");
        report(tommy, Direction.WEST);
        CustomKitSupport.tickN(tommy, 1);
        check(tommyData.is(Live.SURFACE_WALL), "he runs on the wall");
        CustomKitSupport.tap(tommy, 1, true);
        CustomKitSupport.tickN(tommy, 1);
        check(!tommyData.is(Live.SURFACE_WALL) && !tommyData.toggle(Toggle.TOMMY_TIME), "Sneak + R: off (and no slow time)");

        CustomHero main = CustomKitSupport.def().pick("tommy.time", 5, HeroAbility.MAIN).key(1, "tommy.time").build();
        check(!SpeedSurfaces.isSpeedster(CustomKitSupport.data(main)), "with its variant switched off, no Wall Run");
        check(SurfaceRun.Surface.wall(Direction.WEST).touches(), "(a wall is a surface)");
        helper.succeed();
    }

    private CustomSpeedTests() {
    }
}

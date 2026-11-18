package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.JimFireballs;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;

/**
 * Jim Hammond (3.17): the Source Code makes him (and only for a player who is not Vision), his six abilities (the Fire Beam
 * and its magma, fire immunity, the durable body that heals fast, fireballs, the Nova Flame, the burning body), his looks, and
 * his place in the Hero Creator. Server-safe: no client class is loaded.
 */
public final class JimTests {
    /** {@code ticks} server ticks of his powers (nothing ticks a player without a client), then {@code after}. */
    private static void run(GameTestHelper helper, ServerPlayer player, int ticks, Runnable after) {
        for (int i = 1; i <= ticks; i++) {
            helper.runAfterDelay(i, () -> {
                player.tickCount++;
                PowerTicker.tick(player);
            });
        }
        helper.runAfterDelay(ticks + 1, after);
    }

    private static ServerPlayer jim(TestKit.Fixture f, double x, double z) {
        return f.player(x, FLOOR, z, Power.JIM_HAMMOND);
    }

    // ---- how you become him ----

    @EnhancedTest(template = ARENA)
    public static void theSourceCodeMakesAPlayerWithNoHeroJimAndIsUsedUp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOURCE_CODE.get()));
        ModItems.SOURCE_CODE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        check(Powers.is(player, Power.JIM_HAMMOND), "he has become Jim Hammond");
        check(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the item is used up");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void theSourceCodeMakesAnotherHeroJimToo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5, Power.MAGNETO);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOURCE_CODE.get()));
        ModItems.SOURCE_CODE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        check(Powers.is(player, Power.JIM_HAMMOND), "Magneto is replaced");
        check(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the item is used up");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void aServerCanKeepItFromReplacingAHero(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.SOURCE_CODE_REPLACES_HERO, false);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5, Power.MAGNETO);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOURCE_CODE.get()));
        ModItems.SOURCE_CODE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        check(Powers.is(player, Power.MAGNETO), "he stays Magneto");
        check(!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "and keeps the item");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void visionsOwnUseIsUnchanged(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(8.5, FLOOR, 8.5, Power.VISION);
        vision.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOURCE_CODE.get()));
        ModItems.SOURCE_CODE.get().use(vision.level(), vision, InteractionHand.MAIN_HAND);
        check(Powers.is(vision, Power.VISION) && !vision.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "red Vision is still Vision and is not given Jim");
        Powers.setVariant(vision, Variants.get(Power.VISION, "white_vision"));
        ModItems.SOURCE_CODE.get().use(vision.level(), vision, InteractionHand.MAIN_HAND);
        check(Powers.is(vision, Power.VISION) && "source_code".equals(Powers.get(vision).variant()), "White Vision becomes Source Code Vision");
        helper.succeed();
    }

    // ---- the Fire Beam ----

    @EnhancedTest(template = ARENA)
    public static void theFireBeamBurnsAndAChargedOneMakesMagma(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.BEAM_CHARGE_TICKS, 5);
        f.config(JimConfig.BEAM_TAP_TICKS, 2);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        helper.setBlock(new BlockPos(10, (int) FLOOR + 1, 10), Blocks.STONE);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(7.5, FLOOR, 10.5));
        zombie.setRemainingFireTicks(0);
        look(jim, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(jim, 1, true);
        check(Powers.get(jim).is(Live.BEAM_CHARGING), "R held: he charges");
        helper.runAfterDelay(8, () -> {
            AbilityHandler.onKey(jim, 1, false);
            check(zombie.getHealth() < zombie.getMaxHealth() && zombie.getRemainingFireTicks() > 0, "it hurts and burns what it crosses");
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(10, (int) FLOOR + 1, 10))).is(Blocks.MAGMA_BLOCK),
                    "charged, it turns the ground it hit to magma: " + helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(10, (int) FLOOR + 1, 10))));
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void aTapMakesNoMagmaButLeavesFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        helper.setBlock(new BlockPos(10, (int) FLOOR + 1, 10), Blocks.OAK_PLANKS);
        look(jim, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(jim, 1, true);
        AbilityHandler.onKey(jim, 1, false);
        check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(10, (int) FLOOR + 1, 10))).is(Blocks.OAK_PLANKS), "a tap leaves the block");
        BlockPos fire = helper.absolutePos(new BlockPos(9, (int) FLOOR + 1, 10));
        check(helper.getLevel().getBlockState(fire).is(Blocks.FIRE), "and leaves fire where it strikes: " + helper.getLevel().getBlockState(fire));
        check(Powers.get(jim).cooldownUntil(Powers.get(jim).slot(HeroAbility.JIM_BEAM)) > helper.getLevel().getGameTime(), "and a cooldown runs");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "mob_griefing_off_jim")
    public static void withMobGriefingOffNoFireAndNoMagma(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.BEAM_CHARGE_TICKS, 5);
        f.config(JimConfig.BEAM_TAP_TICKS, 2);
        HulkTests.mobGriefing(f, false);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        helper.setBlock(new BlockPos(10, (int) FLOOR + 1, 10), Blocks.STONE);
        look(jim, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(jim, 1, true);
        helper.runAfterDelay(8, () -> {
            AbilityHandler.onKey(jim, 1, false);
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(10, (int) FLOOR + 1, 10))).is(Blocks.STONE), "no magma with mobGriefing off");
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(9, (int) FLOOR + 1, 10))).isAir(), "and no fire");
            helper.succeed();
        });
    }

    // ---- fire immunity, the body, the burning body ----

    @EnhancedTest(template = ARENA)
    public static void fireCannotHurtHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        PowerTicker.tick(jim);
        jim.setRemainingFireTicks(200);
        PowerTicker.tick(jim);
        check(!jim.isOnFire(), "he does not burn");
        check(jim.hasEffect(MobEffects.FIRE_RESISTANCE), "fire resistance is kept up");
        float health = jim.getHealth();
        jim.hurt(helper.getLevel().damageSources().lava(), 10.0F);
        jim.hurt(helper.getLevel().damageSources().inFire(), 10.0F);
        check(jim.getHealth() >= health, "lava and fire do nothing: " + jim.getHealth());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void hisBodyIsAlmostAsDurableAsVisionsAndHealsFast(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(6.5, FLOOR, 8.5, Power.VISION);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        PowerTicker.tick(vision);
        PowerTicker.tick(jim);
        check(jim.getMaxHealth() > 20.0F && jim.getMaxHealth() < vision.getMaxHealth(), "more health than a player, a little less than Vision: "
                + jim.getMaxHealth() + " / " + vision.getMaxHealth());
        check(jim.getAttributeValue(Attributes.ARMOR) > 0 && jim.getAttributeValue(Attributes.ARMOR) < vision.getAttributeValue(Attributes.ARMOR),
                "armour a little less than Vision's");
        jim.setHealth(jim.getMaxHealth() - 10.0F);
        float start = jim.getHealth();
        run(helper, jim, 60, () -> {
            float healed = jim.getHealth() - start;
            // a normal player heals one health in four seconds (80 ticks): five times as fast is one in 16 ticks: 60 ticks, three
            check(healed >= 3.0F, "he healed " + healed + " in 3 seconds");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void hisBodyBurnsWhileHeFliesAndAttackersCatchFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 8.5));
        PowerTicker.tick(jim);
        check(!Powers.get(jim).is(Live.JIM_FLAMING), "on the ground he does not burn");
        zombie.setRemainingFireTicks(0);
        float health = jim.getHealth();
        jim.hurt(helper.getLevel().damageSources().mobAttack(zombie), 1.0F);
        check(!zombie.isOnFire() && jim.getHealth() < health, "a blow on the ground hurts him and sets nobody alight");
        TestKit.clearSpawnInvulnerability(jim);
        jim.invulnerableTime = 0;
        jim.getAbilities().flying = true;
        PowerTicker.tick(jim);
        check(Powers.get(jim).is(Live.JIM_FLAMING), "flying: his whole body burns");
        health = jim.getHealth();
        jim.hurt(helper.getLevel().damageSources().mobAttack(zombie), 1.0F);
        check(jim.getHealth() < health, "the blow still hurts him");
        check(zombie.isOnFire() || zombie.getRemainingFireTicks() > 0, "and the one who struck him burns");
        // an arrow is not a blow by hand
        Zombie other = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 8.5));
        other.setRemainingFireTicks(0);
        jim.invulnerableTime = 0;
        var arrow = TestKit.spawn(helper, EntityType.ARROW, new Vec3(11.5, FLOOR + 1, 9.5));
        jim.hurt(helper.getLevel().damageSources().arrow(arrow, other), 1.0F);
        check(other.getRemainingFireTicks() <= 0, "an arrow is not a blow by hand");
        // the human disguise puts the flames out
        AbilityHandler.onKey(jim, 5, true);
        PowerTicker.tick(jim);
        check(Powers.get(jim).disguise() && !Powers.get(jim).is(Live.JIM_FLAMING), "disguised as a human he does not burn");
        helper.succeed();
    }

    // ---- fireballs and the Nova Flame ----

    @EnhancedTest(template = ARENA)
    public static void aFireballHurtsBurnsAndLightsTheGround(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        zombie.setRemainingFireTicks(0);
        look(jim, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(jim, 3, true);
        check(JimFireballs.inFlight(jim) == 1, "a fireball is in the air");
        run(helper, jim, 30, () -> {
            check(JimFireballs.inFlight(jim) == 0, "it struck");
            check(zombie.getHealth() < zombie.getMaxHealth(), "it hurt the zombie");
            check(zombie.getRemainingFireTicks() > 0, "and set it alight");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void theNovaFlameNeedsItsChargeThenBlastsAndRests(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.NOVA_CHARGE_TICKS, 10);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 8.5));
        zombie.setRemainingFireTicks(0);
        float health = jim.getHealth();
        AbilityHandler.onKey(jim, 4, true);
        check(Powers.get(jim).is(Live.JIM_NOVA_CHARGING), "B held: he gathers his energy");
        PowerTicker.tick(jim);
        check(zombie.getHealth() == zombie.getMaxHealth(), "nothing yet: it takes a while");
        run(helper, jim, 14, () -> {
            check(zombie.getHealth() < zombie.getMaxHealth() && zombie.getRemainingFireTicks() > 0, "the blast hurt and burned the zombie");
            check(jim.getHealth() >= health, "never him");
            long left = Powers.get(jim).cooldownUntil(Powers.get(jim).slot(HeroAbility.JIM_NOVA)) - helper.getLevel().getGameTime();
            check(left > 20 * 60, "then a long cooldown: " + left);
            check(!Powers.get(jim).is(Live.JIM_NOVA_CHARGING), "no longer gathering");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void lettingGoBeforeTheNovaIsFullLosesIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 8.5));
        AbilityHandler.onKey(jim, 4, true);
        PowerTicker.tick(jim);
        AbilityHandler.onKey(jim, 4, false);
        PowerTicker.tick(jim);
        check(!Powers.get(jim).is(Live.JIM_NOVA_CHARGING), "he let go");
        check(zombie.getHealth() == zombie.getMaxHealth(), "no blast");
        helper.succeed();
    }

    // ---- looks, the Creator ----

    @EnhancedTest(template = ARENA)
    public static void hisLooksAreSkinsOfTheRightSizeWithACape(GameTestHelper helper) throws IOException {
        check(Variants.of(Power.JIM_HAMMOND).stream().map(v -> v.id()).toList().equals(java.util.List.of("android", "classic", "human", "own")),
                "android, classic, human, own");
        int[][] sizes = {{128, 128}, {64, 64}, {64, 64}, {64, 32}};
        String[] names = {"jim_android", "jim_classic", "jim_human", "jim_android_cape"};
        for (int i = 0; i < names.length; i++) {
            try (InputStream in = JimTests.class.getResourceAsStream("/assets/enhanced/textures/entity/suit/" + names[i] + ".png")) {
                check(in != null, names[i] + " is in the jar");
                var image = ImageIO.read(in);
                check(image.getWidth() == sizes[i][0] && image.getHeight() == sizes[i][1], names[i] + " is " + sizes[i][0] + "x" + sizes[i][1]
                        + ": " + image.getWidth() + "x" + image.getHeight());
            }
        }
        check(Variants.get(Power.JIM_HAMMOND, "android").cape() != null && Variants.get(Power.JIM_HAMMOND, "classic").cape() == null,
                "the android has the white cape");
        for (int i = 0; i < 4; i++) {
            try (InputStream in = JimTests.class.getResourceAsStream("/assets/enhanced/textures/entity/jim/flame_" + i + ".png")) {
                check(in != null && ImageIO.read(in).getWidth() == 64, "flame picture " + i);
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void heIsInTheHeroCreatorWithAllHisAbilities(GameTestHelper helper) {
        check(com.kolodkingal.enhanced.custom.HeroLooks.heroes().contains(Power.JIM_HAMMOND), "listed as a hero to become");
        check(HeroAbility.of(Power.JIM_HAMMOND).size() == 7, "seven rows: " + HeroAbility.of(Power.JIM_HAMMOND).size());
        for (HeroAbility a : HeroAbility.of(Power.JIM_HAMMOND)) {
            check(com.kolodkingal.enhanced.custom.Catalog.byId(a.id()) == a, a.id() + " is in the catalogue");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void aCustomHeroWithHisFireImmunityIsImmune(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer custom = CustomKitSupport.hero(f, 8.5, 8.5, CustomKitSupport.def().pick("jim_hammond.fire_immunity", 5).build());
        CustomKitSupport.tickN(custom, 2);
        custom.setRemainingFireTicks(100);
        CustomKitSupport.tickN(custom, 1);
        check(!custom.isOnFire(), "the pick works for a hero of the creator");
        helper.succeed();
    }
}

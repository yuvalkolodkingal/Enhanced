package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.block.WeaponXPodBlock;
import com.kolodkingal.enhanced.block.WeaponXPodBlockEntity;
import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.deadpool.DeadpoolAbilities;
import com.kolodkingal.enhanced.deadpool.DeadpoolBulletEntity;
import com.kolodkingal.enhanced.deadpool.DeadpoolLines;
import com.kolodkingal.enhanced.deadpool.DeadpoolState;
import com.kolodkingal.enhanced.deadpool.DogpoolEntity;
import com.kolodkingal.enhanced.deadpool.Katanas;
import com.kolodkingal.enhanced.deadpool.Pistols;
import com.kolodkingal.enhanced.deadpool.PodTreatment;
import com.kolodkingal.enhanced.deadpool.Taunt;
import com.kolodkingal.enhanced.deadpool.TeleportBelt;
import com.kolodkingal.enhanced.healing.HealingBurst;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.BooleanSupplier;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Deadpool (3.7, lane L3): his body and Kidpool's size; the katana combo 5/5/8 (and the X-cut's sweep, the reset); the
 * Parry (an arrow bounced back his way, a blow from the front cut by 60 %, its 80 ticks); the pistols (16 rounds of 4,
 * the 40-tick reload) and the Trick Shot round a corner; the Teleport Belt (a safe spot, never in a wall, never through
 * a Hex's wall, the glitch 3-6 off) and the Drop-In (8, no fall damage); the Taunt (five zombies off a villager, never
 * a boss); the Chimichanga Break; Dogpool (out, biting, following, a nap at 0 health and when his time is up, one at a
 * time, never a player without PvP); the Smoke Vanish; the chimichanga's recipe; his lines' gap; a custom hero's rows;
 * two healers who cannot finish each other; the Weapon X Pod (Deadpool after 100 ticks inside, not 99; leaving cancels;
 * a hero refused). Batches {@code deadpool}, {@code deadpool_cfg} (the belt's glitch, Dogpool's time, the Hex), {@code
 * deadpool_pvp} and {@code deadpool_pod}.
 *
 * <p>The Healing Burst and the healing factor's DOWN are lane L1's: while their stubs stand (the burst heals nothing),
 * the Chimichanga Break's test checks the refused path (nothing eaten, nothing waits) and the two healers' test the
 * blows; with L1 in, both check the whole rule.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class DeadpoolTests {
    private static final String BATCH = "deadpool";
    private static final String CFG = "deadpool_cfg";
    private static final String PVP = "deadpool_pvp";
    private static final String POD = "deadpool_pod";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Deadpool at full health, his looks' default, gone (and his Dogpool home) when the test ends. */
    static ServerPlayer deadpool(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = f.player(x, FLOOR, z, Power.DEADPOOL);
        player.setHealth(player.getMaxHealth());
        f.onClose(() -> HeroLifecycle.onLogout(player));
        return player;
    }

    /** A husk with no armour and 200 health that stands where it is put (no AI, unless {@code free}). */
    static Husk husk(GameTestHelper helper, double x, double z, boolean free) {
        Husk husk = free ? helper.spawn(EntityType.HUSK, new Vec3(x, FLOOR, z))
                : helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    static float lost(LivingEntity e) {
        return e.getMaxHealth() - e.getHealth();
    }

    static PowerData data(ServerPlayer player) {
        return Powers.get(player);
    }

    static DeadpoolState state(ServerPlayer player) {
        return player.getData(ModAttachments.DEADPOOL);
    }

    /** Key {@code ability} pressed and let go, with Sneak or not. */
    static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /** Key {@code ability} held down (not let go), with Sneak. */
    static void hold(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        AbilityHandler.onKey(player, ability, true);
        player.setShiftKeyDown(false);
    }

    /**
     * Ticks the players once a server tick until {@code done} (at most {@code max} ticks), then runs {@code then}; one
     * entry at a time in the test's schedule (as {@code HulkTests.until}).
     */
    static void until(GameTestHelper helper, int max, BooleanSupplier done, Runnable then, ServerPlayer... players) {
        untilStep(helper, 1, max, done, then, players);
    }

    private static void untilStep(GameTestHelper helper, int tick, int max, BooleanSupplier done, Runnable then,
                                  ServerPlayer... players) {
        helper.runAfterDelay(1, () -> {
            for (ServerPlayer p : players) {
                p.doTick();
            }
            if (done.getAsBoolean()) {
                then.run();
            } else if (tick >= max) {
                throw new GameTestAssertException("not done within " + max + " ticks");
            } else {
                untilStep(helper, tick + 1, max, done, then, players);
            }
        });
    }

    /** Ticks the players for {@code n} ticks, then runs {@code then}. */
    static void step(GameTestHelper helper, int n, Runnable then, ServerPlayer... players) {
        int[] count = {0};
        until(helper, n + 1, () -> ++count[0] > n, then, players);
    }

    /** Absolute centre of the top of the floor block under relative ({@code x}, {@code z}). */
    static Vec3 floorTop(GameTestHelper helper, int x, int z) {
        return helper.absoluteVec(new Vec3(x + 0.5, FLOOR, z + 0.5));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The body

    /** His body: 30 health, faster and stronger; Kidpool is a fifth smaller; a plain player has none of it. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void hisBodyAndKidpoolsSize(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 4.5, 4.5);
        assertNear(wade.getMaxHealth(), 30.0, 1.0E-4, "his health");
        assertNear(wade.getAttributeValue(Attributes.ATTACK_DAMAGE), 3.0, 1.0E-4, "his blow (1 + 2)");
        assertNear(wade.getAttributeValue(Attributes.SAFE_FALL_DISTANCE), 6.0, 1.0E-4, "his safe fall (3 + 3)");
        assertNear(wade.getAttributeValue(Attributes.SCALE), 1.0, 1.0E-6, "the classic suit's size");
        Powers.setVariant(wade, Variants.get(Power.DEADPOOL, DeadpoolAbilities.KIDPOOL));
        wade.doTick();
        assertNear(wade.getAttributeValue(Attributes.SCALE), 0.8, 1.0E-6, "Kidpool's size");
        Powers.setPower(wade, Power.NONE);
        assertNear(wade.getMaxHealth(), 20.0, 1.0E-4, "no body without the power");
        assertNear(wade.getAttributeValue(Attributes.SCALE), 1.0, 1.0E-6, "nor Kidpool's size");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The katanas

    /** R three times: 5, 5, 8 on the husk before him; the X-cut sweeps three; after 30 quiet ticks it starts again. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theKatanaComboCutsFiveFiveEight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 4.5, 10.5);
        Husk first = husk(helper, 6.5, 10.5, false);
        Husk far = husk(helper, 12.5, 10.5, false);
        TestKit.look(wade, TestKit.EAST, 10.0F);
        press(wade, 1, false);
        assertNear(lost(first), 5.0, 1.0E-3, "the first cut");
        step(helper, 9, () -> {
            press(wade, 1, false);
            assertNear(lost(first), 10.0, 1.0E-3, "the second cut");
            // (Two more for the X-cut, a little farther than the first, still in reach and in the cone.)
            Husk left = husk(helper, 7.5, 9.3, false);
            Husk right = husk(helper, 7.5, 11.7, false);
            step(helper, 9, () -> {
                press(wade, 1, false);
                assertNear(lost(first), 18.0, 1.0E-3, "the X-cut");
                check(lost(left) == 8.0F && lost(right) == 8.0F, "the X-cut sweeps three: " + lost(left) + ", "
                        + lost(right));
                check(lost(far) == 0.0F, "nobody beyond his reach");
                step(helper, Katanas.COMBO_RESET + 2, () -> {
                    float before = lost(first);
                    press(wade, 1, false);
                    assertNear(lost(first) - before, 5.0, 1.0E-3, "after a pause the combo starts again at 5");
                    check(Katanas.nextStep(3, 5) == 1 && Katanas.nextStep(1, 5) == 2 && Katanas.nextStep(2, 31) == 1,
                            "the steps");
                    helper.succeed();
                }, wade);
            }, wade);
        }, wade);
    }

    /** The cuts keep their gap: a second R within 8 ticks does nothing. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theCutsKeepTheirGap(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 4.5, 4.5);
        Husk husk = husk(helper, 6.5, 4.5, false);
        TestKit.look(wade, TestKit.EAST, 10.0F);
        press(wade, 1, false);
        press(wade, 1, false);
        assertNear(lost(husk), 5.0, 1.0E-3, "one cut");
        helper.succeed();
    }

    /** The Parry, held: an arrow from the front bounces back his way and is his; it does not hurt him. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theParryBouncesAnArrowBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 4.5, 10.5);
        TestKit.look(wade, TestKit.EAST, 0.0F);
        hold(wade, 1);
        check(data(wade).is(Live.DEADPOOL_PARRYING), "he parries");
        Vec3 from = helper.absoluteVec(new Vec3(12.5, FLOOR + 1.5, 10.5));
        Arrow arrow = new Arrow(helper.getLevel(), from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
        arrow.setDeltaMovement(new Vec3(-1.5, 0.0, 0.0));
        helper.getLevel().addFreshEntity(arrow);
        float health = wade.getHealth();
        until(helper, 40, () -> arrow.getOwner() == wade || arrow.isRemoved(), () -> {
            check(arrow.getOwner() == wade, "the arrow is his now");
            check(arrow.getDeltaMovement().x > 0.5, "and flies back east: " + arrow.getDeltaMovement());
            assertNear(wade.getHealth(), health, 1.0E-3, "it did not hurt him");
            helper.succeed();
        }, wade);
    }

    /** A blow from the front loses 60 % while he parries (not from behind); the Parry ends at 80 ticks and R rests. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theParryCutsABlowFromTheFrontAndEndsAt80(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 4.5, 6.5);
        ServerPlayer plain = deadpool(f, 4.5, 14.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(6.5, FLOOR, 6.5));
        TestKit.look(wade, TestKit.EAST, 0.0F);
        TestKit.look(plain, TestKit.EAST, 0.0F);
        hold(wade, 1);
        wade.hurt(wade.damageSources().mobAttack(zombie), 10.0F);
        assertNear(wade.getHealth(), 26.0, 1.0E-3, "from the front: 4 of 10");
        plain.hurt(plain.damageSources().mobAttack(zombie), 10.0F);
        assertNear(plain.getHealth(), 20.0, 1.0E-3, "without the Parry: 10");
        TestKit.look(wade, 90.0F, 0.0F);
        wade.invulnerableTime = 0;
        wade.hurt(wade.damageSources().mobAttack(zombie), 10.0F);
        assertNear(wade.getHealth(), 16.0, 1.0E-3, "from behind: all of it");
        step(helper, 78, () -> {
            check(data(wade).is(Live.DEADPOOL_PARRYING), "still parrying at tick 79");
            step(helper, 2, () -> {
                check(!data(wade).is(Live.DEADPOOL_PARRYING), "over at 80");
                long now = helper.getLevel().getGameTime();
                check(!data(wade).isReady(HeroAbility.DEADPOOL_PARRY, now), "R's slot rests after it");
                hold(wade, 1);
                check(!data(wade).is(Live.DEADPOOL_PARRYING), "no Parry while it rests");
                helper.succeed();
            }, wade);
        }, wade);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The pistols

    /** Sixteen rounds of 4 (eight pairs), then the reload: nothing for 40 ticks, then a full magazine. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void sixteenRoundsThenAFortyTickReload(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 3.5, 10.5);
        Husk husk = husk(helper, 9.5, 10.5, false);
        TestKit.lookAt(wade, husk);
        int[] pairs = {0};
        int[] ticks = {0};
        long[] emptyAt = {-1};
        until(helper, 100, () -> {
            if (state(wade).ammo() > 0 && state(wade).reloadAt() == 0L && ticks[0] % 7 == 0) {
                press(wade, 2, false);
                pairs[0]++;
            }
            if (state(wade).ammo() == 0 && emptyAt[0] < 0) {
                emptyAt[0] = helper.getLevel().getGameTime();
            }
            return ++ticks[0] >= 70;
        }, () -> {
            check(pairs[0] == 8, "eight pairs empty it: " + pairs[0] + " (ammo " + state(wade).ammo() + ", lost "
                    + lost(husk) + ")");
            assertNear(lost(husk), 64.0, 1.0E-3, "sixteen rounds of 4");
            check(state(wade).ammo() == 0, "empty");
            check(data(wade).is(Live.DEADPOOL_AIMING), "the pistols are drawn");
            press(wade, 2, false);
            int bullets = helper.getLevel().getEntitiesOfClass(DeadpoolBulletEntity.class,
                    wade.getBoundingBox().inflate(16.0)).size();
            check(bullets == 0, "no round while he reloads");
            long reloadAt = state(wade).reloadAt();
            check(reloadAt - emptyAt[0] == 40, "the reload is 40 ticks: " + (reloadAt - emptyAt[0]));
            until(helper, 60, () -> state(wade).ammo() == Pistols.magazine(), () -> {
                check(helper.getLevel().getGameTime() >= reloadAt, "not before it is done");
                check(state(wade).reloadAt() == 0L, "reloaded");
                helper.succeed();
            }, wade);
        }, wade);
    }

    /** The Trick Shot: off the wall he aims at, round the corner, into the husk he cannot see: 8. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theTrickShotBouncesRoundACorner(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (int z = 6; z <= 14; z++) {
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(11, y, z), Blocks.STONE);
            }
        }
        for (int x = 5; x <= 8; x++) {
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(x, y, 13), Blocks.STONE);
            }
        }
        ServerPlayer wade = deadpool(f, 3.5, 10.5);
        Husk husk = husk(helper, 8.5, 16.5, false);
        check(!wade.hasLineOfSight(husk), "he cannot see it");
        TestKit.lookAt(wade, helper.absoluteVec(new Vec3(11.0, 3.4, 10.5)));
        press(wade, 2, true);
        long now = helper.getLevel().getGameTime();
        check(!data(wade).isReady(HeroAbility.DEADPOOL_TRICK_SHOT, now), "G's slot rests");
        check(state(wade).ammo() == Pistols.magazine(), "it takes no round from the magazine");
        until(helper, 40, () -> lost(husk) > 0.0F, () -> {
            assertNear(lost(husk), 8.0, 1.0E-3, "the ricochet hit for 8");
            helper.succeed();
        }, wade);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Teleport Belt

    /** V: onto the floor he looks at; at a wall, before it, never in it. (The glitch off.) */
    @GameTest(template = ARENA, batch = CFG)
    public static void theBeltLandsOnASafeSpotNeverInAWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DeadpoolConfig.GLITCH_CHANCE, 0.0);
        ServerPlayer wade = deadpool(f, 3.5, 4.5);
        TestKit.lookAt(wade, floorTop(helper, 12, 4));
        press(wade, 3, false);
        Vec3 at = helper.relativeVec(wade.position());
        assertNear(at.x, 12.5, 1.0E-3, "on the spot looked at (x)");
        assertNear(at.z, 4.5, 1.0E-3, "on the spot looked at (z)");
        assertNear(at.y, FLOOR, 1.0E-3, "on the floor");
        long now = helper.getLevel().getGameTime();
        check(!data(wade).isReady(HeroAbility.DEADPOOL_BELT, now), "V rests");

        ServerPlayer other = deadpool(f, 3.5, 14.5);
        for (int y = 2; y <= 6; y++) {
            helper.setBlock(new BlockPos(12, y, 14), Blocks.STONE);
        }
        TestKit.lookAt(other, helper.absoluteVec(new Vec3(12.0, 3.5, 14.5)));
        press(other, 3, false);
        BlockPos feet = other.blockPosition();
        check(helper.getLevel().getBlockState(feet).isAir() && helper.getLevel().getBlockState(feet.above()).isAir(),
                "not in the wall: " + helper.relativePos(feet));
        check(TeleportBelt.safe(helper.getLevel(), other, feet), "a safe spot");
        assertNear(helper.relativeVec(other.position()).x, 11.5, 1.0E-3, "before the wall");
        helper.succeed();
    }

    /** At a glitch chance of 1 the belt lands him 3 to 6 blocks off the spot, still safe. */
    @GameTest(template = ARENA, batch = CFG)
    public static void aGlitchLandsHimThreeToSixOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DeadpoolConfig.GLITCH_CHANCE, 1.0);
        ServerPlayer wade = deadpool(f, 3.5, 10.5);
        TestKit.lookAt(wade, floorTop(helper, 11, 10));
        press(wade, 3, false);
        Vec3 at = helper.relativeVec(wade.position());
        double off = Math.hypot(at.x - 11.5, at.z - 10.5);
        check(off >= TeleportBelt.GLITCH_MIN - 1.0E-3 && off <= TeleportBelt.GLITCH_MAX + 0.75,
                "3 to 6 off: " + off + " at " + at);
        check(TeleportBelt.safe(helper.getLevel(), wade, wade.blockPosition()), "on a safe spot");
        helper.succeed();
    }

    /** The belt does not take him through the wall of a Hex that keeps him out. */
    @GameTest(template = ARENA, batch = CFG, timeoutTicks = 60)
    public static void theBeltNeverCrossesAHexWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DeadpoolConfig.GLITCH_CHANCE, 0.0);
        f.config(com.kolodkingal.enhanced.EnhancedConfig.HEX_DEFAULT_RADIUS, 4);
        f.config(com.kolodkingal.enhanced.EnhancedConfig.HEX_MIN_RADIUS, 4);
        ServerPlayer wanda = f.player(15.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Hex hex = HexManager.create(wanda);
        f.onClose(() -> HexManager.dissolveAllOwnedBy(f.level().getServer(), wanda.getUUID()));
        ServerPlayer wade = deadpool(f, 3.5, 10.5);
        check(!hex.contains(wade.position()), "he starts outside");
        TestKit.lookAt(wade, floorTop(helper, 15, 10));
        press(wade, 3, false);
        check(!hex.contains(wade.position()), "and is still outside: " + helper.relativeVec(wade.position()));
        helper.succeed();
    }

    /** Sneak + V: above the husk he looks at, the plunge, 8 where he lands on it, no fall damage. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theDropInHitsForEightWithNoFallDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 3.5, 10.5);
        Husk husk = husk(helper, 11.5, 10.5, false);
        TestKit.lookAt(wade, husk);
        press(wade, 3, true);
        check(wade.getY() > husk.getY() + husk.getBbHeight() + 1.0, "above it");
        assertNear(wade.getX(), husk.getX(), 0.6, "over it");
        until(helper, 40, () -> lost(husk) > 0.0F, () -> {
            assertNear(lost(husk), 8.0, 1.0E-3, "8 where he lands");
            float health = wade.getHealth();
            wade.invulnerableTime = 0;
            wade.hurt(wade.damageSources().fall(), 6.0F);
            assertNear(wade.getHealth(), health, 1.0E-3, "no fall damage");
            helper.succeed();
        }, wade);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Taunt, the Chimichanga Break, the smoke

    /** B: five zombies after a villager turn on him and keep at him; a boss is not taunted. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theTauntTurnsFiveZombiesOffAVillagerNotABoss(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 3.5, 10.5);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(15.5, FLOOR, 10.5));
        List<Zombie> zombies = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 6.5 + 2 * i));
            zombie.setTarget(villager);
            zombies.add(zombie);
        }
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(7.5, FLOOR, 16.5));
        press(wade, 4, false);
        for (Zombie zombie : zombies) {
            check(zombie.getTarget() == wade, "a zombie turned on him");
            zombie.setTarget(villager);
            check(zombie.getTarget() == wade, "and keeps at him");
        }
        check(!Taunt.tauntable(warden) && !Taunt.taunted(warden), "the warden is a boss: not taunted");
        long now = helper.getLevel().getGameTime();
        check(!data(wade).isReady(HeroAbility.DEADPOOL_TAUNT, now), "B rests");
        check(DeadpoolLines.lastLine(wade) == now, "a joke over his head");
        helper.succeed();
    }

    /**
     * Sneak + B: with a chimichanga, +100 % and Absorption II, the chimichanga eaten; without, +50 %. While the burst
     * is still lane L1's stub (it heals nobody), the refused path: nothing eaten, nothing waits.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theChimichangaBreak(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer probe = f.player(16.5, FLOOR, 16.5);
        probe.setHealth(4.0F);
        boolean live = HealingBurst.burst(probe, 0.5) && probe.getHealth() > 4.0F;
        ServerPlayer fed = deadpool(f, 3.5, 4.5);
        ServerPlayer hungry = deadpool(f, 3.5, 12.5);
        fed.getInventory().add(new ItemStack(ModItems.CHIMICHANGA.get(), 2));
        fed.setHealth(2.0F);
        hungry.setHealth(2.0F);
        press(fed, 4, true);
        press(hungry, 4, true);
        int left = fed.getInventory().countItem(ModItems.CHIMICHANGA.get());
        if (live) {
            assertNear(fed.getHealth(), 30.0, 1.0E-3, "+100 % with a chimichanga");
            check(fed.hasEffect(MobEffects.ABSORPTION), "and Absorption");
            check(left == 1, "one chimichanga eaten");
            assertNear(hungry.getHealth(), 17.0, 1.0E-3, "+50 % without");
            press(hungry, 4, true);
            assertNear(hungry.getHealth(), 17.0, 1.0E-3, "the break waits");
        } else {
            check(left == 2, "refused: nothing eaten");
            assertNear(fed.getHealth(), 2.0, 1.0E-3, "and nothing healed (the stub)");
        }
        helper.succeed();
    }

    /** Sneak + N: invisible, and a zombie after him loses him and does not find him again. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theSmokeVanishLosesTheMonsters(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 3.5, 10.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 10.5));
        zombie.setTarget(wade);
        press(wade, 5, true);
        check(wade.hasEffect(MobEffects.INVISIBILITY), "invisible");
        check(zombie.getTarget() == null, "the zombie lost him");
        zombie.setTarget(wade);
        check(zombie.getTarget() == null, "and does not find him again");
        check(DogpoolEntity.find(wade) == null, "the smoke is no Dogpool");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Dogpool

    /** N: Dogpool beside him, his, 20 health; N again sends him home; then N waits (one at a time). */
    @GameTest(template = ARENA, batch = BATCH)
    public static void dogpoolComesOneAtATime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 6.5, 10.5);
        press(wade, 5, false);
        DogpoolEntity dog = DogpoolEntity.find(wade);
        check(dog != null, "Dogpool is out");
        check(dog.distanceTo(wade) < 3.0F, "beside him");
        check(dog.isTame() && dog.getOwner() == wade, "his");
        assertNear(dog.getMaxHealth(), 20.0, 1.0E-4, "20 health");
        check(state(wade).dogpool().isPresent(), "his state knows him");
        press(wade, 5, false);
        check(dog.isRemoved() && DogpoolEntity.find(wade) == null, "N again: home");
        press(wade, 5, false);
        check(DogpoolEntity.find(wade) == null, "N waits now");
        check(helper.getLevel().getEntitiesOfClass(DogpoolEntity.class, new AABB(wade.blockPosition()).inflate(20))
                .isEmpty(), "none left");
        helper.succeed();
    }

    /** Dogpool bites the husk he is set on; at 0 health he poofs (no death), and his owner's state forgets him. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void dogpoolBitesAndNapsAtZero(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 4.5, 10.5);
        Husk husk = husk(helper, 8.5, 10.5, false);
        press(wade, 5, false);
        DogpoolEntity dog = DogpoolEntity.find(wade);
        check(dog != null, "out");
        dog.setTarget(husk);
        until(helper, 150, () -> lost(husk) > 0.0F, () -> {
            check(lost(husk) >= 4.0F - 1.0E-3, "a bite of 4: " + lost(husk));
            dog.hurt(dog.damageSources().generic(), 100.0F);
            check(dog.isRemoved(), "a nap at 0 health");
            check(state(wade).dogpool().isEmpty(), "his state forgets him");
            helper.succeed();
        }, wade);
    }

    /** Dogpool follows Deadpool across the arena. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void dogpoolFollows(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 3.5, 3.5);
        press(wade, 5, false);
        DogpoolEntity dog = DogpoolEntity.find(wade);
        check(dog != null, "out");
        Vec3 to = helper.absoluteVec(new Vec3(17.5, FLOOR, 17.5));
        wade.teleportTo(to.x, to.y, to.z);
        until(helper, 160, () -> dog.distanceTo(wade) < 4.0F, helper::succeed, wade);
    }

    /** Dogpool never bites a player while PvP is off. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void dogpoolNeverBitesAPlayerWithoutPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 4.5, 10.5);
        ServerPlayer friend = f.player(6.5, FLOOR, 10.5);
        press(wade, 5, false);
        DogpoolEntity dog = DogpoolEntity.find(wade);
        check(dog != null, "out");
        dog.doHurtTarget(friend);
        assertNear(friend.getHealth(), 20.0, 1.0E-4, "no bite");
        check(!dog.canAttack(friend), "he will not go for him");
        helper.succeed();
    }

    /** Dogpool goes home when his time is up (10 seconds here). */
    @GameTest(template = ARENA, batch = CFG, timeoutTicks = 300)
    public static void dogpoolGoesHomeWhenHisTimeIsUp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DeadpoolConfig.DOGPOOL_SECONDS, 10);
        ServerPlayer wade = deadpool(f, 4.5, 10.5);
        press(wade, 5, false);
        DogpoolEntity dog = DogpoolEntity.find(wade);
        check(dog != null, "out");
        long start = helper.getLevel().getGameTime();
        until(helper, 260, dog::isRemoved, () -> {
            long t = helper.getLevel().getGameTime() - start;
            check(t >= 199 && t <= 202, "home after 200 ticks: " + t);
            check(state(wade).dogpool().isEmpty(), "his state forgets him");
            helper.succeed();
        }, wade);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The chimichanga, the lines, the custom hero

    /** Bread, a cooked filling and a carrot make two chimichangas; a chimichanga feeds 8. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theChimichangaRecipe(GameTestHelper helper) {
        CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(Items.BREAD),
                new ItemStack(Items.COOKED_CHICKEN), new ItemStack(Items.CARROT)));
        var level = helper.getLevel();
        var found = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        check(found.isPresent(), "the recipe matches");
        ItemStack out = found.get().value().assemble(input, level.registryAccess());
        check(out.is(ModItems.CHIMICHANGA.get()) && out.getCount() == 2, "two chimichangas: " + out);
        helper.succeed();
    }

    /** His lines keep their gap (the Taunt's does not), and every line has its key. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void hisLinesKeepTheirGap(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wade = deadpool(f, 4.5, 4.5);
        check(DeadpoolLines.say(wade, DeadpoolLines.Group.READER, false), "a line");
        check(!DeadpoolLines.say(wade, DeadpoolLines.Group.READER, false), "not another at once");
        check(DeadpoolLines.say(wade, DeadpoolLines.Group.RISE, true), "a line that is always said");
        int count = DeadpoolLines.keys().size();
        check(count >= 40, "about forty lines: " + count);
        for (String key : DeadpoolLines.keys()) {
            check(net.minecraft.locale.Language.getInstance().has(key), "a line without words: " + key);
        }
        int arg = 2 * DeadpoolLines.POSE + DeadpoolLines.Group.DOGPOOL.ordinal() * DeadpoolLines.GROUP + 3;
        check(DeadpoolLines.Group.DOGPOOL.key(3).equals(DeadpoolLines.keyOf(arg)) && DeadpoolLines.poseOf(arg) == 2,
                "a bubble's number is its line and its pose");
        helper.succeed();
    }

    /** A custom hero with the pistols on R (not their key): a pair from the magazine; Sneak + R the Trick Shot. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aCustomHerosPistolsOnR(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero def = CustomKitSupport.def().pick("deadpool.pistols", 3).key(1, "deadpool.pistols").build();
        ServerPlayer hero = CustomKitSupport.hero(f, 4.5, 4.5, def);
        f.onClose(() -> HeroLifecycle.onLogout(hero));
        TestKit.look(hero, TestKit.EAST, 0.0F);
        CustomKitSupport.tap(hero, 1, false);
        check(state(hero).ammo() == Pistols.magazine() - 1, "a round left the magazine");
        CustomKitSupport.tap(hero, 1, true);
        long now = helper.getLevel().getGameTime();
        check(!data(hero).isReady(1, now), "the Trick Shot rests R's slot");
        helper.succeed();
    }

    /**
     * His gestures' keys and routes, his run, his rows' moves, his lines' effects, and his preset (60 points at most).
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theAnimationTablesAndThePreset(GameTestHelper helper) throws java.io.IOException {
        int count = 0;
        for (com.kolodkingal.enhanced.client.render.GestureId id
                : com.kolodkingal.enhanced.client.render.GestureId.values()) {
            if (id.name().startsWith("DEADPOOL_")) {
                count++;
                var def = com.kolodkingal.enhanced.client.render.Gestures.get(id);
                check(def != null && def.problems().isEmpty(), id + " is well keyed");
                check(com.kolodkingal.enhanced.client.render.GestureRoutes.startedElsewhere(id),
                        id + " is started by his effects");
            }
        }
        check(count == 16, "sixteen gestures: " + count);
        check(com.kolodkingal.enhanced.client.render.DeadpoolGestures.gestureOf(
                com.kolodkingal.enhanced.network.DeadpoolFxPayload.SLASH, 3)
                == com.kolodkingal.enhanced.client.render.GestureId.DEADPOOL_SLASH_3, "the X-cut");
        int heart = 2 * DeadpoolLines.POSE + DeadpoolLines.Group.TAUNT.ordinal() * DeadpoolLines.GROUP + 1;
        check(com.kolodkingal.enhanced.client.render.DeadpoolGestures.gestureOf(
                com.kolodkingal.enhanced.network.DeadpoolFxPayload.TAUNT, heart)
                == com.kolodkingal.enhanced.client.render.GestureId.DEADPOOL_TAUNT_HEART, "the Taunt's heart");
        int rise = DeadpoolLines.Group.RISE.ordinal() * DeadpoolLines.GROUP;
        check(com.kolodkingal.enhanced.client.render.DeadpoolGestures.gestureOf(
                com.kolodkingal.enhanced.network.DeadpoolFxPayload.TAUNT, rise) == null, "a rise's line is no pose");
        check(com.kolodkingal.enhanced.client.render.RunCharacter.of(Power.DEADPOOL, false)
                == com.kolodkingal.enhanced.client.render.RunCharacter.DEADPOOL, "his jog");
        check(com.kolodkingal.enhanced.client.custom.HeroAbilityAnims.shown(HeroAbility.DEADPOOL_KATANAS, true)
                == com.kolodkingal.enhanced.client.render.GestureId.DEADPOOL_PARRY, "the katanas' variant");
        String path = "/assets/enhanced/" + com.kolodkingal.enhanced.custom.HeroPresets.path("deadpool");
        try (var in = DeadpoolTests.class.getResourceAsStream(path)) {
            check(in != null, "his preset is there");
            String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            var preset = com.kolodkingal.enhanced.custom.HeroPresets.read("deadpool", json);
            check(preset != null && preset.like() == Power.DEADPOOL, "his preset reads");
            int cost = com.kolodkingal.enhanced.custom.CustomHeroRules.cost(preset.hero());
            check(cost <= 60, "his preset costs 60 at most: " + cost);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // PvP

    /** His swords and rounds reach a player only with PvP on. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void hisBladesReachAPlayerOnlyWithPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(false);
        ServerPlayer wade = deadpool(f, 4.5, 10.5);
        ServerPlayer target = f.player(6.5, FLOOR, 10.5);
        TestKit.look(wade, TestKit.EAST, 10.0F);
        press(wade, 1, false);
        assertNear(target.getHealth(), 20.0, 1.0E-4, "PvP off: untouched");
        f.pvp(true);
        step(helper, 10, () -> {
            press(wade, 1, false);
            check(target.getHealth() < 20.0F, "PvP on: cut");
            helper.succeed();
        }, wade);
    }

    /**
     * Two Deadpools fighting with PvP on: neither can finish the other (the healing factor's DOWN, lane L1); while its
     * stub stands, the blows land.
     */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 200)
    public static void twoHealersCannotFinishEachOther(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer probe = deadpool(f, 16.5, 16.5);
        probe.setHealth(2.0F);
        probe.hurt(probe.damageSources().generic(), 10.0F);
        boolean healing = probe.isAlive() && probe.getHealth() >= 1.0F;
        ServerPlayer a = deadpool(f, 4.5, 10.5);
        ServerPlayer b = deadpool(f, 6.0, 10.5);
        TestKit.look(a, TestKit.EAST, 10.0F);
        TestKit.look(b, 90.0F, 10.0F);
        a.setHealth(6.0F);
        b.setHealth(6.0F);
        int[] cuts = {0};
        until(helper, 120, () -> {
            if (helper.getLevel().getGameTime() % 9 == 0) {
                press(a, 1, false);
                press(b, 1, false);
                cuts[0]++;
            }
            return cuts[0] >= 6 || !healing && (!a.isAlive() || !b.isAlive());
        }, () -> {
            if (healing) {
                check(a.isAlive() && b.isAlive(), "neither died");
            } else {
                check(a.getHealth() < 6.0F || !a.isAlive(), "the blows landed (the healing factor is L1's)");
            }
            helper.succeed();
        }, a, b);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Weapon X Pod

    /** The pod at relative (10, 2, 10), its two halves. */
    static BlockPos pod(GameTestHelper helper) {
        BlockPos rel = new BlockPos(10, 2, 10);
        helper.setBlock(rel, ModBlocks.WEAPON_X_POD.get().defaultBlockState().setValue(WeaponXPodBlock.FACING,
                Direction.EAST).setValue(WeaponXPodBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(rel.above(), ModBlocks.WEAPON_X_POD.get().defaultBlockState().setValue(WeaponXPodBlock.FACING,
                Direction.EAST).setValue(WeaponXPodBlock.HALF, DoubleBlockHalf.UPPER));
        return helper.absolutePos(rel);
    }

    /** Whether the pod's block entity (lane L4's: the hatch, light and hum) says a treatment runs. */
    static boolean podRunning(GameTestHelper helper, BlockPos pod) {
        return helper.getLevel().getBlockEntity(pod) instanceof WeaponXPodBlockEntity entity && entity.running();
    }

    /** A powerless player in the pod is Deadpool after 100 ticks of treatment, not 99. */
    @GameTest(template = ARENA, batch = POD, timeoutTicks = 200)
    public static void thePodMakesDeadpoolAfterAHundredTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos pod = pod(helper);
        ServerPlayer wade = f.player(11.35, FLOOR, 10.5);
        f.onClose(() -> HeroLifecycle.onLogout(wade));
        check(PodTreatment.inside(wade, pod), "he stands in the pod");
        PodTreatment.use(wade, pod.above());
        check(PodTreatment.underWay(wade), "the treatment begins (the upper half answers too)");
        check(podRunning(helper, pod), "the pod's hatch shuts and it hums (its block entity runs)");
        long start = helper.getLevel().getGameTime();
        int[] lastNone = {-1};
        until(helper, 160, () -> {
            long t = helper.getLevel().getGameTime() - start;
            if (Powers.power(wade) == Power.NONE) {
                lastNone[0] = (int) t;
                return false;
            }
            return true;
        }, () -> {
            check(lastNone[0] == 99, "Deadpool on the 100th tick, after " + lastNone[0]);
            check(Powers.power(wade) == Power.DEADPOOL, "Deadpool");
            check(!PodTreatment.underWay(wade), "the treatment is over");
            check(!podRunning(helper, pod), "the pod opens again");
            assertNear(wade.getAttributeValue(Attributes.MOVEMENT_SPEED), wade.getAttributeBaseValue(
                    Attributes.MOVEMENT_SPEED) * 1.15, 1.0E-4, "the hatch let him go (and his speed is his)");
            helper.succeed();
        }, wade);
    }

    /** Sneaking out of the pod cancels the treatment; a hero is refused; one who stands outside is told to step in. */
    @GameTest(template = ARENA, batch = POD, timeoutTicks = 100)
    public static void thePodCancelsAndRefuses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos pod = pod(helper);
        ServerPlayer wade = f.player(11.35, FLOOR, 10.5);
        f.onClose(() -> HeroLifecycle.onLogout(wade));
        ServerPlayer thor = f.player(10.5, FLOOR, 9.65, Power.THOR);
        ServerPlayer far = f.player(14.5, FLOOR, 10.5);
        PodTreatment.use(thor, pod);
        check(!PodTreatment.underWay(thor) && Powers.power(thor) == Power.THOR, "a hero is refused");
        PodTreatment.use(far, pod);
        check(!PodTreatment.underWay(far), "outside the pod: nothing");
        PodTreatment.use(wade, pod);
        check(PodTreatment.underWay(wade), "the treatment begins");
        check(wade.getAttributeValue(Attributes.MOVEMENT_SPEED) < 1.0E-6, "the hatch holds him");
        step(helper, 20, () -> {
            wade.setShiftKeyDown(true);
            step(helper, 2, () -> {
                wade.setShiftKeyDown(false);
                check(!PodTreatment.underWay(wade), "sneaking cancels it");
                check(!podRunning(helper, pod), "the pod opens again");
                check(Powers.power(wade) == Power.NONE, "still powerless");
                check(wade.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.05, "and free to walk");
                helper.succeed();
            }, wade);
        }, wade);
    }

    private DeadpoolTests() {
    }
}

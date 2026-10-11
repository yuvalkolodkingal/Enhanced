package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.ThanosAbilities;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.item.InfinityStone;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
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
 * Thanos, the hero (DESIGN section 15.4): his Titan's body and healing factor, and the first three stones: the Power
 * Stone's Blast and Slam, the Space Stone's Portal Step and Pull, the Reality Stone's Transmute and Veil. The Soul and
 * Time Stones, the lifecycle and the hero beside the boss are in {@link ThanosSoulTimeTests}. Nothing here keeps state
 * between tests but what is the player's own, so the tests that tick for a while have a batch each only where another
 * test running at the same time could be reached by what they do.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ThanosTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers (shared with ThanosSoulTimeTests)

    static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    static void pressSneaking(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        press(player, ability);
        player.setShiftKeyDown(false);
    }

    /** A Thanos in the arena at {@code (x, z)}, facing east, with his body applied and his health full. */
    static ServerPlayer thanos(TestKit.Fixture f, double x, double z) {
        ServerPlayer thanos = f.player(x, FLOOR, z, Power.THANOS);
        PowerTicker.tick(thanos);
        thanos.setHealth(thanos.getMaxHealth());
        TestKit.look(thanos, TestKit.EAST, 0.0F);
        return thanos;
    }

    /** A zombie (a husk) that stands where it is: no mind, no armour (so a damage reads as it is). */
    static Zombie zombie(GameTestHelper helper, double x, double z) {
        // (A husk: the sun does not burn it, so what it loses is only what a test did to it.)
        Zombie zombie = helper.spawn(EntityType.HUSK, new Vec3(x, FLOOR, z));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        return zombie;
    }

    static float lost(LivingEntity creature) {
        return creature.getMaxHealth() - creature.getHealth();
    }

    /** Makes the ability ready again, whatever it was doing. */
    static void ready(ServerPlayer player, int ability) {
        Powers.get(player).startCooldown(ability, 0L, 0);
    }

    static long cooldownLeft(ServerPlayer player, int ability) {
        return Powers.get(player).cooldownUntil(ability) - player.level().getGameTime();
    }

    /** Whether the packets hold the message with this translation key on the action bar. */
    static boolean told(List<Object> packets, String key) {
        for (Object sent : packets) {
            if (sent instanceof ClientboundSystemChatPacket chat && chat.overlay() && key.equals(ThanosSummonTests.key(chat.content()))) {
                return true;
            }
        }
        return false;
    }

    /** A Chaos Shield, complete, with this much strength. */
    static void shield(TestKit.Fixture f, ServerPlayer wanda, float strength) {
        Powers.get(wanda).raiseChaosShield(f.level().getGameTime() - 100L, 0, strength);
    }

    /** Mjolnir lies on the player (Thor does it, for the test). */
    static void pin(TestKit.Fixture f, ServerPlayer victim) {
        f.pvp(true);
        ServerPlayer thor = f.player(1.5, FLOOR, 1.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, victim);
        check(MjolnirPin.isPinned(victim), "he is pinned");
    }

    // ------------------------------------------------------------------------------------------------------------
    // The body

    /**
     * A Titan's body: forty health, six armour and two toughness more, half the knockback, three more strength, a tenth
     * taller, a longer reach and stride, half the fall; no flight; and all of it gone when the power is.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_body")
    public static void thanosHasATitansBodyAndCannotFly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = f.player(10.5, FLOOR, 10.5, Power.THANOS);
        ServerPlayer plain = f.player(5.5, FLOOR, 5.5);
        PowerTicker.tick(thanos);
        PowerTicker.tick(plain);
        assertNear(thanos.getMaxHealth(), 20.0 + ThanosConfig.HEALTH_BONUS.get(), 1.0E-6, "twenty hearts more health");
        assertNear(thanos.getAttributeValue(Attributes.ARMOR), ThanosConfig.ARMOR_BONUS.get(), 1.0E-6, "six armour");
        assertNear(thanos.getAttributeValue(Attributes.ARMOR_TOUGHNESS), ThanosConfig.TOUGHNESS_BONUS.get(), 1.0E-6, "two toughness");
        assertNear(thanos.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), ThanosConfig.KNOCKBACK_RESISTANCE.get(), 1.0E-6, "half the knockback");
        assertNear(thanos.getAttributeValue(Attributes.ATTACK_DAMAGE), 1.0 + ThanosConfig.ATTACK_BONUS.get(), 1.0E-6, "Titan strength");
        assertNear(thanos.getAttributeValue(Attributes.SCALE), 1.0 + ThanosConfig.SCALE.get(), 1.0E-6, "a tenth taller");
        assertNear(thanos.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), 3.0 + ThanosConfig.REACH_BONUS.get(), 1.0E-6, "long arms");
        assertNear(thanos.getAttributeValue(Attributes.STEP_HEIGHT), 0.6 + ThanosConfig.STEP_BONUS.get(), 1.0E-6, "a Titan's stride walks up whole blocks");
        assertNear(thanos.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 0.5, 1.0E-6, "half the fall");
        check(!thanos.mayFly() && !thanos.getAbilities().flying, "he does not fly");
        check(!Power.THANOS.canFly() && Power.THANOS.flightAbility() == -1, "and has no flight key");
        assertNear(plain.getMaxHealth(), 20.0, 1.0E-6, "someone who is not Thanos has his own health");
        assertNear(plain.getAttributeValue(Attributes.SCALE), 1.0, 1.0E-6, "and his own size");
        // The power goes, and the body with it.
        Powers.setPower(thanos, Power.NONE);
        assertNear(thanos.getMaxHealth(), 20.0, 1.0E-6, "the health is gone with the power");
        assertNear(thanos.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-6, "the armour too");
        assertNear(thanos.getAttributeValue(Attributes.SCALE), 1.0, 1.0E-6, "and the size");
        assertNear(thanos.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), 3.0, 1.0E-6, "and the reach");
        assertNear(thanos.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 1.0, 1.0E-6, "and the gentle landing");
        helper.succeed();
    }

    /**
     * Another hero's power given to a Thanos takes his body off him at once (the modifiers are removed by the plug-in).
     */
    @GameTest(template = ARENA, batch = "thanos_hero_body")
    public static void anotherPowerTakesTheTitansBodyAway(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        thanos.setHealth(thanos.getMaxHealth());
        check(thanos.getMaxHealth() > 39.0, "he has forty health");
        Powers.setPower(thanos, Power.THOR);
        PowerTicker.tick(thanos);
        assertNear(thanos.getMaxHealth(), 30.0, 1.0E-6, "he has Thor's thirty, not the Titan's forty");
        assertNear(thanos.getAttributeValue(Attributes.SCALE), 1.0, 1.0E-6, "and Thor's size");
        check(thanos.getHealth() <= thanos.getMaxHealth(), "his health fits his new body");
        helper.succeed();
    }

    /**
     * The Titan healing factor: not hurt for a while, he heals half a point a second, for a little hunger; a hurt makes
     * it wait; with an empty hunger bar there is none.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_regen", timeoutTicks = 200)
    public static void theTitanHealingFactorWaitsAfterAHurtAndCostsHunger(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(ThanosConfig.REGEN_DELAY_TICKS, 40);
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        ServerPlayer starving = thanos(f, 5.5, 5.5);
        thanos.hurt(thanos.damageSources().generic(), 10.0F);
        starving.hurt(starving.damageSources().generic(), 10.0F);
        starving.getFoodData().setFoodLevel(0);
        float hurt = thanos.getHealth();
        float exhaustion = thanos.getFoodData().getExhaustionLevel();
        f.everyTick(150, t -> {
            PowerTicker.tick(thanos);
            PowerTicker.tick(starving);
            if (t == 35) {
                check(thanos.getHealth() == hurt, "he waits after a hurt: " + thanos.getHealth() + " of " + hurt);
            }
            if (t < 140) {
                return false;
            }
            // (100 ticks of healing: 0.5 a second.)
            assertNear(thanos.getHealth() - hurt, 2.5, 0.3, "he healed half a point a second for a hundred ticks");
            check(thanos.getFoodData().getExhaustionLevel() - exhaustion > 0.5, "and it cost him hunger: " + thanos.getFoodData().getExhaustionLevel());
            check(starving.getHealth() == hurt, "with an empty hunger bar there is no healing: " + starving.getHealth());
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // R, the Power Stone

    /**
     * The Power Blast: twelve to the first creature in the beam (the zombie, thrown), six to the other hostile ones
     * round where it ends (not the cow, not the one far off), the cooldown of the key and a little hunger, and no block
     * changes even with block damage on.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_blast", timeoutTicks = 60)
    public static void thePowerBlastHurtsOneSplashesTheRestAndBreaksNoBlock(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(ThanosBossConfig.BLOCK_DAMAGE, true);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        Zombie target = zombie(helper, 9.5, 10.5);
        Zombie beside = zombie(helper, 10.5, 12.0);
        Zombie far = zombie(helper, 9.5, 17.5);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(10.5, FLOOR, 9.0));
        cow.setNoAi(true);
        BlockPos dirt = new BlockPos(11, (int) FLOOR, 10);
        helper.setBlock(dirt, Blocks.DIRT);
        float exhaustion = thanos.getFoodData().getExhaustionLevel();
        press(thanos, 1);
        assertNear(lost(target), ThanosConfig.POWER_BLAST_DAMAGE.get(), 0.01, "the first creature in the beam takes the blast");
        assertNear(lost(beside), ThanosConfig.POWER_BLAST_SPLASH.get(), 0.01, "the hostile one by where it ends takes the splash");
        check(lost(cow) == 0.0F, "the cow, who is no enemy, is spared");
        check(lost(far) == 0.0F, "so is the one far off");
        check(thanos.getHealth() == thanos.getMaxHealth(), "he is not hurt by his own blast");
        check(cooldownLeft(thanos, 1) == ThanosConfig.POWER_BLAST_COOLDOWN.get(), "the key's cooldown is the blast's: " + cooldownLeft(thanos, 1));
        assertNear(thanos.getFoodData().getExhaustionLevel() - exhaustion, ThanosConfig.POWER_BLAST_EXHAUSTION.get(), 0.01, "and the blast cost hunger");
        helper.assertBlockPresent(Blocks.DIRT, dirt);
        // Fired into a wall, it breaks nothing either.
        target.discard();
        beside.discard();
        for (int y = 0; y < 4; y++) {
            for (int z = 8; z <= 12; z++) {
                helper.setBlock(new BlockPos(16, (int) FLOOR + y, z), Blocks.STONE);
            }
        }
        ready(thanos, 1);
        press(thanos, 1);
        for (int y = 0; y < 4; y++) {
            for (int z = 8; z <= 12; z++) {
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(16, (int) FLOOR + y, z));
            }
        }
        helper.succeed();
    }

    /**
     * A creature standing between: only the first is hit by the blast itself; one behind it takes nothing from the
     * beam.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_blast", timeoutTicks = 60)
    public static void thePowerBlastStopsAtTheFirstCreature(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 2.5, 10.5);
        Zombie first = zombie(helper, 7.5, 10.5);
        Zombie behind = zombie(helper, 14.5, 10.5);
        press(thanos, 1);
        assertNear(lost(first), ThanosConfig.POWER_BLAST_DAMAGE.get(), 0.01, "the first takes it");
        check(lost(behind) == 0.0F, "the one behind it is outside the burst and takes nothing");
        helper.succeed();
    }

    /**
     * Players are hit only with PvP on, and the stone says so when PvP spared one; a Creative player is never hit; the
     * Blast passes them by.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_blast", timeoutTicks = 60)
    public static void thePowerBlastSparesPlayersWithPvpOffAndCreativeAlways(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 2.5, 10.5);
        ServerPlayer victim = f.player(8.5, FLOOR, 10.5);
        TestKit.sentPackets(thanos);
        press(thanos, 1);
        check(lost(victim) == 0.0F, "with PvP off he is untouched");
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.pvp_off"), "and Thanos is told why");
        f.pvp(true);
        ready(thanos, 1);
        press(thanos, 1);
        assertNear(lost(victim), ThanosConfig.POWER_BLAST_DAMAGE.get(), 0.01, "with PvP on he takes it");
        ServerPlayer creative = f.player(8.5, FLOOR, 15.5, GameType.CREATIVE);
        ServerPlayer ghost = f.player(8.5, FLOOR, 5.5, GameType.SPECTATOR);
        lookAt(thanos, ghost);
        ready(thanos, 1);
        press(thanos, 1);
        check(lost(ghost) == 0.0F, "nor a Spectator");
        lookAt(thanos, creative);
        ready(thanos, 1);
        press(thanos, 1);
        check(lost(creative) == 0.0F, "a Creative player is never hit");
        helper.succeed();
    }

    /**
     * A complete Chaos Shield in the way stops the beam at its surface and loses twelve; the creature behind is
     * untouched.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_blast", timeoutTicks = 60)
    public static void aChaosShieldStopsThePowerBlastAndLosesTwelve(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer thanos = thanos(f, 2.5, 10.5);
        ServerPlayer wanda = f.player(8.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Zombie behind = zombie(helper, 14.5, 10.5);
        shield(f, wanda, 100.0F);
        press(thanos, 1);
        check(wanda.getHealth() == wanda.getMaxHealth(), "the blast never reached her");
        assertNear(100.0F - Powers.get(wanda).shieldStrength(), ThanosConfig.POWER_BLAST_DAMAGE.get(), 0.01, "the shield lost twelve");
        check(lost(behind) == 0.0F, "and nothing came through behind her");
        helper.succeed();
    }

    /**
     * The key is one cooldown slot: after the Blast the Slam must wait for the Blast's seventy, and the Slam's makes it
     * wait longer.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_blast", timeoutTicks = 60)
    public static void theBlastAndTheSlamShareTheKeysCooldown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        Zombie target = zombie(helper, 9.5, 10.5);
        press(thanos, 1);
        float afterBlast = lost(target);
        check(cooldownLeft(thanos, 1) == ThanosConfig.POWER_BLAST_COOLDOWN.get(), "the blast's cooldown");
        pressSneaking(thanos, 1);
        check(cooldownLeft(thanos, 1) == ThanosConfig.POWER_BLAST_COOLDOWN.get(), "the slam waits for the blast: nothing changed");
        ready(thanos, 1);
        pressSneaking(thanos, 1);
        check(cooldownLeft(thanos, 1) == ThanosConfig.SLAM_COOLDOWN.get(), "the slam's cooldown is its own: " + cooldownLeft(thanos, 1));
        check(afterBlast > 0.0F, "the blast had hit");
        helper.succeed();
    }

    /**
     * The Titan Slam: a ring runs out from his feet at under a block a tick; whoever stands on the ground in its way is
     * thrown and takes ten (the zombies, once), the one in the air when it passes takes nothing, nor does the cow, nor
     * Thanos himself, nor the one beyond the ring.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_slam", timeoutTicks = 60)
    public static void theTitanSlamIsJumpable(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        Zombie near = zombie(helper, 13.5, 10.5);
        Zombie mid = zombie(helper, 10.5, 15.5);
        Zombie outside = zombie(helper, 19.5, 17.5);
        Zombie jumper = zombie(helper, 10.5, 5.5);
        jumper.setNoGravity(true);
        Vec3 up = helper.absoluteVec(new Vec3(10.5, FLOOR + 1.5, 5.5));
        jumper.setPos(up);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, FLOOR, 10.5));
        cow.setNoAi(true);
        pressSneaking(thanos, 1);
        check(cooldownLeft(thanos, 1) == ThanosConfig.SLAM_COOLDOWN.get(), "the slam's cooldown");
        double[] nearX = {near.getX()};
        f.everyTick(14, t -> {
            PowerTicker.tick(thanos);
            jumper.setNoGravity(true);
            jumper.setDeltaMovement(Vec3.ZERO);
            jumper.setPos(up);
            if (t < 12) {
                return false;
            }
            assertNear(lost(near), ThanosConfig.SLAM_DAMAGE.get(), 0.01, "the near one takes it, once");
            assertNear(lost(mid), ThanosConfig.SLAM_DAMAGE.get(), 0.01, "so does the one further out");
            check(lost(jumper) == 0.0F, "the one in the air when it passed takes nothing");
            check(lost(cow) == 0.0F, "the cow is spared");
            check(lost(outside) == 0.0F, "beyond the ring nothing is touched");
            check(thanos.getHealth() == thanos.getMaxHealth(), "and he is never hurt by it");
            check(near.getX() > nearX[0] + 0.05 || near.getDeltaMovement().x > 0.1,
                    "the near one was thrown outward: " + near.getX() + " from " + nearX[0] + ", moving " + near.getDeltaMovement());
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // G, the Space Stone

    /**
     * The Portal Step: he steps onto the block he looks at, or right behind the creature; it costs the cooldown and a
     * little hunger, and if there is no room behind, or nothing to look at, he says so and pays nothing.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_step", timeoutTicks = 60)
    public static void thePortalStepLandsOnTheBlockOrBehindTheCreature(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        BlockPos block = new BlockPos(12, (int) FLOOR, 10);
        helper.setBlock(block, Blocks.STONE);
        Vec3 top = helper.absoluteVec(new Vec3(12.5, FLOOR + 1.0, 10.5));
        lookAt(thanos, helper.absoluteVec(new Vec3(12.5, FLOOR + 0.5, 10.5)));
        press(thanos, 2);
        assertNear(thanos.getX(), top.x, 0.2, "he stands on the block he looked at (x)");
        assertNear(thanos.getY(), top.y, 0.2, "(y)");
        assertNear(thanos.getZ(), top.z, 0.2, "(z)");
        check(cooldownLeft(thanos, 2) == ThanosConfig.SPACE_STEP_COOLDOWN.get(), "the step's cooldown: " + cooldownLeft(thanos, 2));
        // Behind a creature that faces south: to its north.
        helper.setBlock(block, Blocks.AIR);
        Zombie target = zombie(helper, 12.5, 10.5);
        target.yBodyRot = 0.0F;
        thanos.teleportTo(level, helper.absoluteVec(new Vec3(3.5, FLOOR, 10.5)).x, helper.absoluteVec(new Vec3(3.5, FLOOR, 10.5)).y,
                helper.absoluteVec(new Vec3(3.5, FLOOR, 10.5)).z, TestKit.EAST, 0.0F);
        ready(thanos, 2);
        lookAt(thanos, target);
        press(thanos, 2);
        check(thanos.position().z < target.position().z - 0.5 && Math.abs(thanos.getX() - target.getX()) < 0.8,
                "he stands right behind it: " + thanos.position() + " behind " + target.position());
        // Looking at nothing within the range of the stone: a message, no cooldown.
        f.config(ThanosConfig.SPACE_STEP_RANGE, 4.0);
        thanos.teleportTo(level, helper.absoluteVec(new Vec3(3.5, FLOOR, 4.5)).x, helper.absoluteVec(new Vec3(3.5, FLOOR, 4.5)).y,
                helper.absoluteVec(new Vec3(3.5, FLOOR, 4.5)).z, TestKit.EAST, 0.0F);
        ready(thanos, 2);
        TestKit.sentPackets(thanos);
        press(thanos, 2);
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.no_block"), "'Look at a block within 4 blocks'");
        check(cooldownLeft(thanos, 2) <= 0, "a step with no target costs no cooldown");
        helper.succeed();
    }

    /** No room behind the creature: "No room there", nothing paid. */
    @GameTest(template = ARENA, batch = "thanos_hero_step", timeoutTicks = 60)
    public static void thePortalStepNeedsRoomForHisWholeBody(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        Zombie target = zombie(helper, 12.5, 10.5);
        target.yBodyRot = 0.0F;
        for (int x = 10; x <= 14; x++) {
            for (int y = 0; y < 4; y++) {
                helper.setBlock(new BlockPos(x, (int) FLOOR + y, 9), Blocks.STONE);
                helper.setBlock(new BlockPos(x, (int) FLOOR + y, 8), Blocks.STONE);
            }
        }
        // (Behind it, to the north, is a wall two blocks thick: every spot he could stand on is inside it.)
        lookAt(thanos, target);
        Vec3 before = thanos.position();
        TestKit.sentPackets(thanos);
        press(thanos, 2);
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.no_space"), "'No room there'");
        check(thanos.position().equals(before) && cooldownLeft(thanos, 2) <= 0, "he did not move and paid nothing");
        helper.succeed();
    }

    /** A Hex wall refuses the step into it (no cooldown, no move); and Mjolnir's pin refuses it as well. */
    @GameTest(template = ARENA, batch = "thanos_hero_step_hex", timeoutTicks = 60)
    public static void theHexAndThePinRefuseThePortalStep(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_MIN_RADIUS, 4);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 4);
        ServerPlayer wanda = f.player(16.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        HexManager.create(wanda);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        BlockPos block = new BlockPos(15, (int) FLOOR, 10);
        helper.setBlock(block, Blocks.STONE);
        lookAt(thanos, helper.absoluteVec(new Vec3(15.5, FLOOR + 0.5, 10.5)));
        Vec3 before = thanos.position();
        press(thanos, 2);
        check(thanos.position().equals(before), "the wall bounced the step: he stayed");
        check(cooldownLeft(thanos, 2) <= 0, "and no cooldown was paid");
        // Outside the Hex, a pinned Thanos cannot step either.
        helper.setBlock(block, Blocks.AIR);
        BlockPos near = new BlockPos(8, (int) FLOOR, 10);
        helper.setBlock(near, Blocks.STONE);
        lookAt(thanos, helper.absoluteVec(new Vec3(8.5, FLOOR + 0.5, 10.5)));
        pin(f, thanos);
        before = thanos.position();
        press(thanos, 2);
        check(thanos.position().equals(before) && cooldownLeft(thanos, 2) <= 0, "Mjolnir's pin refuses the step too");
        helper.succeed();
    }

    /**
     * The Space Pull: the creature he looks at is drawn to two blocks in front of him, dazed. A boss, a pinned
     * creature, an armour stand and a player with PvP off are not.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_pull", timeoutTicks = 80)
    public static void theSpacePullDrawsCreaturesInButNotBossesOrThePinned(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 10.5, 4.5);
        TestKit.look(thanos, 0.0F, 0.0F);
        Zombie target = zombie(helper, 10.5, 16.5);
        pressSneaking(thanos, 2);
        check(cooldownLeft(thanos, 2) == ThanosConfig.PULL_COOLDOWN.get(), "the pull's cooldown: " + cooldownLeft(thanos, 2));
        f.everyTick(12, t -> {
            PowerTicker.tick(thanos);
            if (t < 10) {
                return false;
            }
            assertNear(target.getZ(), thanos.getZ() + 2.0, 0.6, "he arrives two blocks in front of Thanos");
            assertNear(target.getX(), thanos.getX(), 0.6, "straight ahead");
            check(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "dazed");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "thanos_hero_pull_not", timeoutTicks = 80)
    public static void theSpacePullLeavesBossesThePinnedAndPlayersWithPvpOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 10.5, 4.5);
        TestKit.look(thanos, 0.0F, 0.0F);
        // A boss cannot be pulled (nothing is paid), nor an armour stand.
        var warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(10.5, FLOOR, 16.5));
        TestKit.sentPackets(thanos);
        pressSneaking(thanos, 2);
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.no_target") && cooldownLeft(thanos, 2) <= 0, "a boss cannot be pulled: nothing paid");
        warden.discard();
        var stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(10.5, FLOOR, 16.5));
        pressSneaking(thanos, 2);
        check(cooldownLeft(thanos, 2) <= 0, "nor an armour stand");
        stand.discard();
        // A player with PvP off is not pulled, and the stone says why.
        ServerPlayer player = f.player(10.5, FLOOR, 16.5);
        pressSneaking(thanos, 2);
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.pvp_off") && cooldownLeft(thanos, 2) <= 0, "PvP off: refused, free");
        player.teleportTo(f.level(), thanos.getX() + 8.0, thanos.getY(), thanos.getZ() + 12.0, 0.0F, 0.0F);
        // Mjolnir's pin keeps a creature where it is.
        Zombie pinned = zombie(helper, 10.5, 16.5);
        ServerPlayer thor = f.player(1.5, FLOOR, 1.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, pinned);
        check(MjolnirPin.isPinned(pinned), "the zombie is pinned");
        TestKit.sentPackets(thanos);
        pressSneaking(thanos, 2);
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.no_target") && cooldownLeft(thanos, 2) <= 0, "a pinned creature cannot be pulled");
        helper.succeed();
    }

    /** The Hex judges the landing of what the Pull draws in: a hero outside a Hex is not drawn into it. */
    @GameTest(template = ARENA, batch = "thanos_hero_pull_hex", timeoutTicks = 60)
    public static void aHexWallRefusesWhatThePullWouldDrawIn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(EnhancedConfig.HEX_MIN_RADIUS, 4);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 4);
        ServerPlayer wanda = f.player(16.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        HexManager.create(wanda);
        ServerPlayer thanos = thanos(f, 16.5, 12.5);
        TestKit.look(thanos, 90.0F, 0.0F);
        ServerPlayer thor = f.player(6.5, FLOOR, 12.5, Power.THOR);
        Vec3 before = thor.position();
        pressSneaking(thanos, 2);
        check(cooldownLeft(thanos, 2) == ThanosConfig.PULL_COOLDOWN.get(), "the pull was cast");
        f.everyTick(12, t -> {
            PowerTicker.tick(thanos);
            if (t < 10) {
                return false;
            }
            check(thor.position().distanceTo(before) < 0.2, "the wall refused the landing: he is where he was");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // V, the Reality Stone

    /**
     * Transmute: the sphere takes six ticks, then the creature is stone for sixty: it cannot move, and takes half of
     * what hurts it. A boss shakes it off; a player is stone only with PvP on.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_transmute", timeoutTicks = 120)
    public static void transmuteTurnsACreatureToStoneForHalfDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        Zombie target = zombie(helper, 10.5, 10.5);
        press(thanos, 3);
        check(cooldownLeft(thanos, 3) == ThanosConfig.TRANSMUTE_COOLDOWN.get(), "the cooldown: " + cooldownLeft(thanos, 3));
        check(!target.hasEffect(ModEffects.PETRIFIED), "the sphere is still on its way");
        f.everyTick(70, t -> {
            PowerTicker.tick(thanos);
            if (t == 8) {
                check(target.hasEffect(ModEffects.PETRIFIED), "turned to stone");
                MobEffectInstance stone = target.getEffect(ModEffects.PETRIFIED);
                check(stone != null && stone.getDuration() > 40 && stone.getDuration() <= ThanosConfig.TRANSMUTE_TICKS.get(), "for the config's time");
                assertNear(target.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.0, 1.0E-6, "it cannot move");
                target.invulnerableTime = 0;
                target.hurt(target.damageSources().generic(), 10.0F);
                assertNear(lost(target), 10.0 * ThanosConfig.PETRIFY_DAMAGE_FACTOR.get(), 0.01, "stone takes half the damage");
            }
            if (t < 68) {
                return false;
            }
            check(!target.hasEffect(ModEffects.PETRIFIED), "and the stone ends");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "thanos_hero_transmute_not", timeoutTicks = 60)
    public static void transmuteIsHeldBackByPvpAndByShields(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        ServerPlayer victim = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        // A player, PvP off: refused with the reason, and no cooldown.
        TestKit.sentPackets(thanos);
        press(thanos, 3);
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.pvp_off") && cooldownLeft(thanos, 3) <= 0, "PvP off: refused, free");
        // PvP on: a Chaos Shield stops the sphere and loses two.
        f.pvp(true);
        shield(f, victim, 50.0F);
        press(thanos, 3);
        f.everyTick(12, t -> {
            PowerTicker.tick(thanos);
            if (t < 9) {
                return false;
            }
            check(!victim.hasEffect(ModEffects.PETRIFIED), "the shield stopped the sphere");
            assertNear(50.0F - Powers.get(victim).shieldStrength(), 2.0, 0.01, "and lost two");
            // Without the shield she is stone.
            Powers.get(victim).setChaosShieldActive(false);
            ready(thanos, 3);
            press(thanos, 3);
            return true;
        });
    }

    /** A Creative player and a Loki's duplicate: no target for the first; the second dissolves. */
    @GameTest(template = ARENA, batch = "thanos_hero_transmute_not", timeoutTicks = 60)
    public static void transmuteNeverHoldsCreativePlayers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        ServerPlayer creative = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        TestKit.sentPackets(thanos);
        press(thanos, 3);
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.no_target") && cooldownLeft(thanos, 3) <= 0, "no target, no cooldown");
        check(!creative.hasEffect(ModEffects.PETRIFIED), "a Creative player is never stone");
        helper.succeed();
    }

    /**
     * The Reality Veil: an arrow coming at him within eight blocks is a bubble that pops (a particle burst, the arrow
     * gone); the Veil costs a little hunger a tick, is saved with him, and goes out at an empty hunger bar. A hammer
     * of Thor's is not a bubble.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_veil", timeoutTicks = 100)
    public static void theRealityVeilTurnsArrowsIntoBubbles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        PowerData data = Powers.get(thanos);
        pressSneaking(thanos, 3);
        check(data.toggle(Toggle.REALITY_VEIL), "the Veil is on");
        check(cooldownLeft(thanos, 3) <= 0, "the toggle has no cooldown");
        float exhaustion = thanos.getFoodData().getExhaustionLevel();
        Arrow arrow = EntityType.ARROW.create(level);
        arrow.setNoGravity(true);
        arrow.moveTo(helper.absoluteVec(new Vec3(10.5 + 7.0, FLOOR + 1.5, 10.5)));
        arrow.setDeltaMovement(-0.5, 0.0, 0.0);
        level.addFreshEntity(arrow);
        Arrow away = EntityType.ARROW.create(level);
        away.setNoGravity(true);
        away.moveTo(helper.absoluteVec(new Vec3(10.5 + 3.0, FLOOR + 1.5, 10.5)));
        away.setDeltaMovement(0.5, 0.0, 0.0);
        level.addFreshEntity(away);
        TestKit.sentPackets(thanos);
        f.everyTick(40, t -> {
            PowerTicker.tick(thanos);
            if (t == 6) {
                check(arrow.isRemoved(), "the arrow that came at him is a bubble");
                check(!away.isRemoved(), "the one that flies away from him is left alone");
                check(thanos.getHealth() == thanos.getMaxHealth(), "and he was not hit");
                check(!TestKit.particlesIn(TestKit.sentPackets(thanos)).isEmpty(), "bubbles were sent");
                check(thanos.getFoodData().getExhaustionLevel() > exhaustion, "the Veil burns hunger");
            }
            if (t == 20) {
                // Starving puts it out.
                thanos.getFoodData().setFoodLevel(0);
            }
            if (t == 23) {
                check(!data.toggle(Toggle.REALITY_VEIL), "the Veil goes out at an empty hunger bar");
                return true;
            }
            return false;
        });
    }

    /**
     * The Veil is saved with the player; the impact hook takes an arrow that was too fast for the tick, and not
     * Mjolnir.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_veil", timeoutTicks = 60)
    public static void theVeilIsSavedAndLeavesTheUnstoppableAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        pressSneaking(thanos, 3);
        var saved = PowerData.CODEC.encodeStart(NbtOps.INSTANCE, Powers.get(thanos)).getOrThrow();
        PowerData loaded = PowerData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        check(loaded.toggle(Toggle.REALITY_VEIL) && loaded.power() == Power.THANOS, "the Veil is saved with him");
        Arrow arrow = EntityType.ARROW.create(level);
        arrow.moveTo(thanos.position().add(2.0, 1.0, 0.0));
        level.addFreshEntity(arrow);
        check(ThanosAbilities.veilTakes(thanos, arrow) && arrow.isRemoved(), "an arrow at the moment of impact is a bubble");
        ServerPlayer thor = f.player(1.5, FLOOR, 1.5, Power.THOR);
        ThrownWeaponEntity hammer =
                new ThrownWeaponEntity(level, thor, new ItemStack(ModItems.MJOLNIR.get()));
        level.addFreshEntity(hammer);
        check(!ThanosAbilities.veilTakes(thanos, hammer) && !hammer.isRemoved(), "Mjolnir is not a bubble");
        pressSneaking(thanos, 3);
        Arrow late = EntityType.ARROW.create(level);
        late.moveTo(thanos.position().add(2.0, 1.0, 0.0));
        level.addFreshEntity(late);
        check(!ThanosAbilities.veilTakes(thanos, late) && !late.isRemoved(), "with the Veil down an arrow is an arrow");
        hammer.discard();
        helper.succeed();
    }

    /** What he shot himself is not a bubble, even if it comes back at him. */
    @GameTest(template = ARENA, batch = "thanos_hero_veil", timeoutTicks = 60)
    public static void theVeilLeavesWhatHeShotHimselfAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        pressSneaking(thanos, 3);
        Arrow own = EntityType.ARROW.create(level);
        own.setOwner(thanos);
        own.setNoGravity(true);
        own.moveTo(helper.absoluteVec(new Vec3(10.5 + 6.0, FLOOR + 1.5, 10.5)));
        own.setDeltaMovement(-0.5, 0.0, 0.0);
        level.addFreshEntity(own);
        f.everyTick(8, t -> {
            PowerTicker.tick(thanos);
            if (t < 6) {
                return false;
            }
            check(!own.isRemoved(), "his own arrow is not turned into a bubble");
            own.discard();
            return true;
        });
    }

    /** A creature that is pinned while the vortex draws it in stays where it is. */
    @GameTest(template = ARENA, batch = "thanos_hero_pull_not", timeoutTicks = 60)
    public static void aCreaturePinnedWhileTheVortexDrawsItStaysWhereItIs(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 10.5, 4.5);
        TestKit.look(thanos, 0.0F, 0.0F);
        Zombie target = zombie(helper, 10.5, 16.5);
        pressSneaking(thanos, 2);
        check(cooldownLeft(thanos, 2) == ThanosConfig.PULL_COOLDOWN.get(), "the pull was cast");
        ServerPlayer thor = f.player(1.5, FLOOR, 1.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, target);
        check(MjolnirPin.isPinned(target), "pinned in flight");
        Vec3 before = target.position();
        f.everyTick(12, t -> {
            PowerTicker.tick(thanos);
            if (t < 10) {
                return false;
            }
            check(target.position().distanceTo(before) < 0.2, "the vortex did not draw him in");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Every key, every refusal

    /**
     * While he is held (Billy's spell, stone, the Soul World), no stone answers: the film's flaw. Nothing starts, no
     * cooldown is paid, he is told why; and when he is free again they answer.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_held")
    public static void aHeldThanosUsesNoStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        Zombie target = zombie(helper, 10.5, 10.5);
        PowerData data = Powers.get(thanos);
        for (var effect : List.of(ModEffects.SPELLBOUND, ModEffects.PETRIFIED, ModEffects.SOULWORLD)) {
            thanos.addEffect(new MobEffectInstance(effect, 200, 0, false, false));
            TestKit.sentPackets(thanos);
            for (int ability = 1; ability <= 5; ability++) {
                press(thanos, ability);
                pressSneaking(thanos, ability);
            }
            check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.held"), "he is told: " + effect.getRegisteredName());
            for (int ability = 1; ability <= 5; ability++) {
                check(data.isReady(ability, f.level().getGameTime()), effect.getRegisteredName() + ": ability " + ability + " paid nothing");
            }
            check(lost(target) == 0.0F && !data.toggle(Toggle.REALITY_VEIL) && !data.is(Live.SOUL_DRAINING) && !data.is(Live.SOUL_SIGHT),
                    effect.getRegisteredName() + ": nothing happened");
            thanos.removeEffect(effect);
        }
        press(thanos, 1);
        check(lost(target) > 0.0F, "free again, the stone answers");
        helper.succeed();
    }

    /**
     * An empty hunger bar: "too hungry for the stones", nothing starts; in Creative there is no hunger to pay; a
     * Spectator does nothing.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_hunger")
    public static void hungerCreativeAndSpectatorDecideWhetherTheStonesAnswer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        Zombie target = zombie(helper, 10.5, 10.5);
        thanos.getFoodData().setFoodLevel(0);
        TestKit.sentPackets(thanos);
        press(thanos, 1);
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.too_hungry") && lost(target) == 0.0F && cooldownLeft(thanos, 1) <= 0,
                "starving: told, nothing happened, nothing paid");
        thanos.setGameMode(GameType.CREATIVE);
        float exhaustion = thanos.getFoodData().getExhaustionLevel();
        press(thanos, 1);
        check(lost(target) > 0.0F && thanos.getFoodData().getExhaustionLevel() == exhaustion, "in Creative the stone answers and costs nothing");
        thanos.setGameMode(GameType.SPECTATOR);
        ready(thanos, 1);
        float health = target.getHealth();
        press(thanos, 1);
        check(target.getHealth() == health && cooldownLeft(thanos, 1) <= 0, "a Spectator does nothing");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The looks and the colours

    /**
     * Three looks, H cycles them (the first is the Infinity War, the last his own skin), and the powers stay; the
     * colours are the Power Stone's.
     */
    @GameTest(template = ARENA, batch = "thanos_hero_looks")
    public static void thanosHasThreeLooksAndThePowerStonesColours(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        Powers.awaken(player, Power.THANOS);
        check(Powers.power(player) == Power.THANOS, "he is Thanos");
        check(Variants.of(Power.THANOS).size() == 3, "three looks");
        check(Powers.variant(player).id().equals("infinity_war"), "he starts in the Infinity War look");
        check(Powers.color(player) == StoneKind.POWER.color() && Powers.glow(player) == StoneKind.POWER.core(), "the colours are the Power Stone's");
        Powers.cycleVariant(player, 1);
        check(Powers.variant(player).id().equals("endgame"), "H goes to the Endgame armour");
        Powers.cycleVariant(player, 1);
        check(Powers.variant(player).id().equals("own"), "then to his own skin");
        check(Powers.power(player) == Power.THANOS, "the powers stay");
        check(Powers.color(player) == StoneKind.POWER.color(), "and so do the colours");
        Powers.cycleVariant(player, -1);
        check(Powers.variant(player).id().equals("endgame"), "Sneak + H goes back");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Small helpers kept for the other class

    /** The stone voices heard, in order, among the sounds the player was sent. */
    static List<String> voices(ServerPlayer player) {
        List<String> heard = new ArrayList<>();
        for (TestKit.HeardSound sound : TestKit.heardSounds(player)) {
            for (InfinityStone stone : InfinityStone.values()) {
                if (sound.id().getPath().equals(stone.id() + "_stone")) {
                    heard.add(stone.id());
                }
            }
        }
        return heard;
    }

    private ThanosTests() {
    }
}

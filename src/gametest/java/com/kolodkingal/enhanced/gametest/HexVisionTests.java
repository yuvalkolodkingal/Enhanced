package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.hex.HexVisions;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.HexTests.abs;
import static com.kolodkingal.enhanced.gametest.HexTests.inside;
import static com.kolodkingal.enhanced.gametest.HexTests.raiseHex;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The Vision of the Hex: raised with the Hex, bound to it, fighting for Wanda. The Hex is hers, in the middle of the
 * arena (10.5, 2, 10.5), radius 8.
 */
public final class HexVisionTests {
    private static final double CENTER = 10.5;

    /** The Vision her Hex raised. */
    private static HexVisionEntity visionOf(ServerPlayer wanda) {
        HexVisionEntity vision = HexVisions.of(wanda.getUUID());
        check(vision != null, "Wanda's Hex has raised a Vision");
        return vision;
    }

    /** Every Vision of a Hex in the arena. */
    private static List<HexVisionEntity> visionsIn(GameTestHelper helper) {
        return helper.getLevel().getEntities(ModEntities.HEX_VISION.get(), new AABB(helper.absoluteVec(Vec3.ZERO), helper.absoluteVec(new Vec3(22, 12, 22))),
                e -> true);
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_raise")
    public static void raisingTheHexRaisesVisionBesideWandaAndDissolvingItTakesHimAway(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        Hex hex = HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow();
        HexVisionEntity vision = visionOf(wanda);
        check(vision.isAlive() && !vision.isRemoved() && vision.level() == helper.getLevel(), "Vision is in the world");
        check(visionsIn(helper).size() == 1, "one Vision");
        check(hex.id().equals(vision.hexId()) && wanda.getUUID().equals(vision.getOwnerUUID()), "bound to the Hex and to Wanda");
        check(vision.distanceTo(wanda) < 3.5, "beside her (" + vision.distanceTo(wanda) + " blocks)");
        check(inside(f, wanda, vision), "inside the Hex");
        assertNear(vision.getMaxHealth(), EnhancedConfig.HEX_VISION_HEALTH.get(), 1.0E-4, "the health the config gives him");
        assertNear(vision.getHealth(), vision.getMaxHealth(), 1.0E-4, "whole");

        HexManager.handleAction(wanda, HexAction.DISSOLVE, Optional.empty(), 0);
        check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).isEmpty(), "the Hex is gone");
        check(vision.isRemoved() && HexVisions.of(wanda.getUUID()) == null, "and so is the Vision it raised");
        check(visionsIn(helper).isEmpty(), "nothing is left of him");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_off")
    public static void aServerCanTurnTheVisionOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_VISION_ENABLED, false);
        ServerPlayer wanda = raiseHex(f);
        helper.runAfterDelay(10, () -> {
            check(HexVisions.of(wanda.getUUID()) == null && visionsIn(helper).isEmpty(), "no Vision comes with the Hex");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_one")
    public static void thereIsOnePerHex(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity first = visionOf(wanda);
        Hex hex = HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow();
        // Raised again (or by the level's tick): no second one.
        HexVisions.raise(wanda, hex);
        f.everyTick(30, tick -> {
            if (tick >= 25) {
                check(visionsIn(helper).size() == 1, "still one Vision for the Hex");
                check(visionOf(wanda) == first && !first.isRemoved(), "and it is the first");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_two")
    public static void eachWandaHasHerOwn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        ServerPlayer wanda = f.player(CENTER, FLOOR, CENTER, Power.SCARLET_WITCH);
        AbilityHandler.onKey(wanda, 5, true);
        ServerPlayer other = f.player(CENTER, FLOOR, 19.5, Power.SCARLET_WITCH);
        AbilityHandler.onKey(other, 5, true);
        HexVisionEntity mine = visionOf(wanda);
        HexVisionEntity theirs = visionOf(other);
        check(mine != theirs && visionsIn(helper).size() == 2, "two Wandas, two Visions");
        check(!mine.hexId().equals(theirs.hexId()), "each bound to her own Hex");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_wall")
    public static void visionCanNeverLeaveTheWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        Hex hex = HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow();
        check(HexManager.judge(hex, vision, true) == HexManager.Verdict.HELD, "the wall holds him when he goes out");
        check(HexManager.judge(hex, vision, false) == HexManager.Verdict.PASS, "and lets him in");

        f.everyTick(20, tick -> {
            if (tick == 2) {
                Vec3 out = abs(helper, CENTER, FLOOR, 1.0);
                vision.teleportTo(out.x, out.y, out.z);
                check(!inside(f, wanda, vision), "carried out through the wall");
            }
            if (tick >= 6) {
                check(inside(f, wanda, vision), "he is inside again (tick " + tick + ")");
                check(vision.isAlive(), "and well");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_other_wall")
    public static void anotherWandasHexBouncesHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        ServerPlayer wanda = f.player(CENTER, FLOOR, CENTER, Power.SCARLET_WITCH);
        AbilityHandler.onKey(wanda, 5, true);
        HexVisionEntity vision = visionOf(wanda);
        ServerPlayer other = f.player(CENTER, FLOOR, 19.5, Power.SCARLET_WITCH);
        AbilityHandler.onKey(other, 5, true);
        Hex hexOfOther = HexManager.ownedBy(helper.getLevel(), other.getUUID()).orElseThrow();
        check(HexManager.judge(hexOfOther, vision, false) == HexManager.Verdict.BLOCK, "the other Hex throws him back, it doesn't kill him");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_defend")
    public static void visionGoesForWhateverAttacksWanda(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(CENTER + 4.0, FLOOR, CENTER));
        check(vision.getTarget() == null, "Vision has nothing to fight");
        float health = zombie.getHealth();

        wanda.hurt(helper.getLevel().damageSources().mobAttack(zombie), 1.0F);
        check(vision.getTarget() == zombie, "the zombie that hit Wanda is Vision's target");

        f.everyTick(80, tick -> {
            if (!zombie.isAlive() || zombie.getHealth() < health) {
                check(wanda.isAlive(), "Wanda is fine");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_join")
    public static void visionJoinsInWhenWandaStrikes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        Pig pig = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(CENTER - 4.0, FLOOR, CENTER));

        pig.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM, wanda, wanda), 1.0F);
        check(vision.getTarget() == pig, "what Wanda attacks, Vision attacks");
        f.everyTick(100, tick -> !pig.isAlive());
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_point")
    public static void visionAttacksTheCreatureWandaPointsAt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(CENTER + 5.0, FLOOR, CENTER));
        Zombie behind = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(CENTER - 5.0, FLOOR, CENTER));
        float health = zombie.getHealth();
        check(vision.getTarget() == null, "Vision has nothing to fight");

        lookAt(wanda, zombie);
        HexManager.handleAction(wanda, HexAction.VISION_TARGET, Optional.empty(), 0);
        check(vision.getTarget() == zombie, "Vision, that one!");
        check(vision.isCommanded(), "it was an order");
        check(behind.getHealth() == behind.getMaxHealth(), "(the other zombie is not touched yet)");

        f.everyTick(80, tick -> {
            if (zombie.getHealth() < health) {
                check(behind.getHealth() == behind.getMaxHealth(), "Vision goes for the one he was told to");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_recall")
    public static void lookingAtNothingCallsVisionBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(CENTER + 5.0, FLOOR, CENTER));

        lookAt(wanda, zombie);
        HexManager.handleAction(wanda, HexAction.VISION_TARGET, Optional.empty(), 0);
        check(vision.getTarget() == zombie, "sent at the zombie");
        TestKit.look(wanda, 0.0F, -90.0F);
        HexManager.handleAction(wanda, HexAction.VISION_TARGET, Optional.empty(), 0);
        check(vision.getTarget() == null && !vision.isCommanded(), "looking at the sky calls him back");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_friends")
    public static void visionNeverHurtsWandaHerAlliesOrHerPets(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        Hex hex = HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow();
        ServerPlayer friend = f.player(CENTER + 2.5, FLOOR, CENTER + 2.5);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(friend.getUUID()), 1);
        Wolf pet = TestKit.spawnWithNoFreeWill(helper, EntityType.WOLF, new Vec3(CENTER - 2.5, FLOOR, CENTER - 2.5));
        pet.tame(wanda);
        Wolf friendsPet = TestKit.spawnWithNoFreeWill(helper, EntityType.WOLF,
                new Vec3(CENTER - 2.5, FLOOR, CENTER + 2.5));
        friendsPet.tame(friend);
        f.pvp(true);

        vision.setTarget(wanda);
        check(vision.getTarget() == null, "he won't set his mind on Wanda");
        vision.command(friend);
        check(vision.getTarget() == null, "nor on a player she allowed into the Hex, even with PvP on");
        vision.command(pet);
        check(vision.getTarget() == null, "nor on her pet");
        vision.command(friendsPet);
        check(vision.getTarget() == null, "nor on an ally's pet");

        float wandaHealth = wanda.getHealth();
        float friendHealth = friend.getHealth();
        check(!vision.doHurtTarget(wanda) && !vision.doHurtTarget(friend) && !vision.doHurtTarget(pet), "his fists don't land on them");
        check(wanda.getHealth() == wandaHealth && friend.getHealth() == friendHealth && pet.getHealth() == pet.getMaxHealth(), "nobody is hurt");
        check(!HexVisions.mayHarm(helper.getLevel().getServer(), hex, wanda), "the rule says so");

        // Pointing at them is refused in the same way.
        lookAt(wanda, friend);
        HexManager.handleAction(wanda, HexAction.VISION_TARGET, Optional.empty(), 0);
        check(vision.getTarget() == null, "Wanda can't point him at her ally");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_pvp_off")
    public static void visionRespectsPvpOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        Hex hex = HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow();
        ServerPlayer stranger = f.player(CENTER + 3.0, FLOOR, CENTER + 3.0);
        ServerPlayer creative = f.player(CENTER - 3.0, FLOOR, CENTER + 3.0, net.minecraft.world.level.GameType.CREATIVE);

        f.pvp(false);
        wanda.hurt(helper.getLevel().damageSources().playerAttack(stranger), 1.0F);
        check(vision.getTarget() == null, "with PvP off Vision leaves players alone, even one who hits Wanda");
        vision.command(stranger);
        check(vision.getTarget() == null && !vision.doHurtTarget(stranger), "and doesn't touch them");

        f.pvp(true);
        check(HexVisions.mayHarm(helper.getLevel().getServer(), hex, stranger), "with PvP on a stranger who hits Wanda is fair game");
        wanda.hurt(helper.getLevel().damageSources().playerAttack(stranger), 1.0F);
        check(vision.getTarget() == stranger, "and Vision goes for him");
        check(!HexVisions.mayHarm(helper.getLevel().getServer(), hex, creative), "never a Creative player");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_punch")
    public static void hisPunchIsDense(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(CENTER + 2.0, FLOOR, CENTER));
        vision.setTarget(zombie);
        float before = zombie.getHealth();
        check(vision.doHurtTarget(zombie), "the punch lands");
        // (a zombie has two points of armor)
        assertNear(before - zombie.getHealth(), EnhancedConfig.HEX_VISION_PUNCH_DAMAGE.get(), 0.5, "for the damage the config gives it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_beam")
    public static void aMindStoneBeamReachesWhatFistsCant(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        // Stays put (so the target stays out of reach of his fists); the zombie is six blocks off.
        HexManager.handleAction(wanda, HexAction.SET_VISION_STAYS, Optional.empty(), 1);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(CENTER + 6.5, FLOOR, CENTER));
        float before = zombie.getHealth();
        vision.defend(zombie);

        f.everyTick(60, tick -> {
            if (zombie.getHealth() < before) {
                // (a zombie has two points of armor)
                assertNear(before - zombie.getHealth(), EnhancedConfig.HEX_VISION_BEAM_DAMAGE.get(), 0.3, "the beam's damage, one hit");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_fall", timeoutTicks = 160)
    public static void aFallenVisionIsRebuiltAfterTheDelay(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_VISION_REBUILD_TICKS, 60);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        java.util.UUID id = vision.getUUID();

        vision.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        check(vision.isRemoved(), "he falls and fades");
        check(HexVisions.of(wanda.getUUID()) == null && visionsIn(helper).isEmpty(), "nothing of him is left");
        check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).isPresent(), "the Hex stands");

        helper.runAfterDelay(30, () -> check(visionsIn(helper).isEmpty(), "Wanda is still rebuilding him"));
        helper.runAfterDelay(75, () -> {
            HexVisionEntity again = visionOf(wanda);
            check(!again.getUUID().equals(id), "a new Vision");
            check(visionsIn(helper).size() == 1 && inside(f, wanda, again), "one, inside the Hex");
            assertNear(again.getHealth(), again.getMaxHealth(), 1.0E-4, "whole again");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_leave")
    public static void visionGoesWhenWandaLeavesTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        helper.getLevel().getServer().getPlayerList().remove(wanda);
        check(vision.isRemoved() && HexVisions.of(wanda.getUUID()) == null, "without Wanda there is no Vision");
        check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).isPresent(), "(her Hex stays)");
        helper.runAfterDelay(10, () -> {
            check(visionsIn(helper).isEmpty(), "and none is raised while she is away");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_orders")
    public static void aVisionWhoHoldsHisPostStaysWhereHeIs(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        HexManager.handleAction(wanda, HexAction.SET_VISION_STAYS, Optional.empty(), 1);
        check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow().visionStays(), "the order is kept in the Hex");
        Vec3 post = vision.position();

        f.everyTick(50, tick -> {
            if (tick == 2) {
                Vec3 away = abs(helper, CENTER - 5.0, FLOOR, CENTER - 5.0);
                wanda.teleportTo(away.x, away.y, away.z);
            }
            if (tick >= 45) {
                check(vision.position().distanceTo(post) < 1.5, "he holds his post (moved " + vision.position().distanceTo(post) + ")");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_follow", timeoutTicks = 200)
    public static void visionFollowsWanda(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);

        f.everyTick(120, tick -> {
            if (tick == 2) {
                Vec3 away = abs(helper, CENTER - 6.0, FLOOR, CENTER - 4.0);
                wanda.teleportTo(away.x, away.y, away.z);
            }
            if (tick >= 100) {
                check(vision.distanceTo(wanda) < 5.0, "he is by her side again (" + vision.distanceTo(wanda) + " blocks)");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_vision_friendly")
    public static void wandasOwnBeamAndBlowsDoNotHurtHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        HexVisionEntity vision = visionOf(wanda);
        float health = vision.getHealth();
        check(!vision.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM, wanda, wanda), 5.0F), "her Chaos Beam does nothing to him");
        check(!vision.hurt(helper.getLevel().damageSources().playerAttack(wanda), 5.0F), "nor do her blows");
        check(vision.getHealth() == health, "he is unhurt");
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(CENTER + 3.0, FLOOR, CENTER));
        check(!WandaAbilities.beamTargets(wanda).test(vision), "her beam passes through him");
        check(WandaAbilities.beamTargets(wanda).test(zombie), "(and still strikes a zombie)");
        check(vision.hurt(helper.getLevel().damageSources().mobAttack(zombie), 5.0F), "(a zombie's does)");
        check(vision.getHealth() < health, "(and hurts)");
        check(vision.getTarget() == zombie, "(and he strikes back)");
        helper.succeed();
    }

    private HexVisionTests() {
    }
}

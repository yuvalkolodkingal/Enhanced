package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.HexTests.abs;
import static com.kolodkingal.enhanced.gametest.HexTests.inside;
import static com.kolodkingal.enhanced.gametest.HexTests.raiseHex;
import static com.kolodkingal.enhanced.gametest.HexTests.walkTo;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Wanda's Hex holds Vision: inside a standing Hex he can't leave it (walking, flying, phasing, any teleport or
 * portal), whether or not she allowed him in; he comes apart a little at the wall (White Vision is only thrown back),
 * never dies of it, and is free once the Hex is gone, the wall is no longer round him, or Wanda is badly hurt, dead or
 * gone from the world.
 *
 * <p>The Hex is Wanda's, in the middle of the arena (10.5, 2, 10.5), radius 8; the arena is 21 blocks across, so the
 * wall is in reach of its edge: a spot at z = 1.0 is outside the Hex, one at z = 5.0 inside.
 */
public final class HexHoldsVisionTests {
    private static final double CENTER = 10.5;
    /** Orange and yellow: what a Vision comes apart in at the wall (the Mind Stone's gold is not among these). */
    private static final int FLAKE_ORANGE = 0xFFB02E;
    private static final int FLAKE_DARK_ORANGE = 0xFF8A1E;

    /** True if the packets hold a burst of the orange flakes Vision comes apart in. */
    private static boolean flakedOrange(List<Object> packets) {
        for (ClientboundLevelParticlesPacket packet : TestKit.particlesIn(packets)) {
            if (packet.getParticle() instanceof EnergyParticleOptions energy
                    && (energy.color() == FLAKE_ORANGE || energy.color() == FLAKE_DARK_ORANGE)) {
                return true;
            }
        }
        return false;
    }

    private static boolean heard(List<TestKit.HeardSound> sounds, net.minecraft.sounds.SoundEvent event) {
        return sounds.stream().anyMatch(sound -> sound.id().equals(event.getLocation()));
    }

    /**
     * Whether a portal to the Nether is refused to {@code entity} (the travel-to-dimension event). TODO(port L1): post
     * the common event ({@code Events.post(new EntityTravelToDimensionEvent(entity, Level.NETHER)).isCanceled()}) once
     * {@code CommonEvents.onTravelToDimension} (L1) is a {@code @Subscribe} handler; until then only NeoForge's bus
     * reaches it (and, through the bridge, every handler already moved).
     */
    private static boolean portalRefused(ServerPlayer entity) {
        var portal = new net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent(entity, Level.NETHER);
        return net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(portal).isCanceled();
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_walk")
    public static void visionWalkingIntoTheWallIsThrownBackAndComesApart(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        check(inside(f, wanda, vision), "Vision starts inside the Hex");
        float health = vision.getHealth();

        f.everyTick(12, tick -> {
            if (tick == 2) {
                TestKit.sentPackets(vision);
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
                check(!inside(f, wanda, vision), "Vision walks out through the wall");
            }
            if (tick == 4) {
                check(vision.isAlive() && Powers.is(vision, Power.VISION), "the wall doesn't kill him");
                check(inside(f, wanda, vision), "the Hex holds Vision: he is thrown back inside");
                assertNear(health - vision.getHealth(), 2.0, 1.0E-4, "the first throw back at the wall costs one heart");
                check(flakedOrange(TestKit.sentPackets(vision)), "pieces of him flake off in orange and yellow");
            }
            if (tick >= 6) {
                check(inside(f, wanda, vision), "and he stays inside");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_crackle")
    public static void visionCracklesAtTheWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        helper.runAfterDelay(2, () -> {
            TestKit.heardSounds(vision);
            walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
        });
        helper.runAfterDelay(5, () -> {
            check(inside(f, wanda, vision), "thrown back inside");
            check(heard(TestKit.heardSounds(vision), SoundEvents.CAMPFIRE_CRACKLE), "with a crackling sound");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_allowed")
    public static void anAllowedVisionIsHeldTooButEntersFreely(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 1.0, Power.VISION);
        ServerPlayer stranger = f.player(1.0, FLOOR, CENTER, Power.VISION);
        check(!inside(f, wanda, vision) && !inside(f, wanda, stranger), "both start outside");
        float strangerHealth = stranger.getHealth();
        float visionHealth = vision.getHealth();
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(vision.getUUID()), 1);

        f.everyTick(20, tick -> {
            if (tick == 2) {
                // Getting in keeps the old rules: Wanda's permission lets Vision in, and bounces him if she gave none.
                walkTo(vision, abs(helper, CENTER, FLOOR, 3.5));
                walkTo(stranger, abs(helper, 3.5, FLOOR, CENTER));
            }
            if (tick == 5) {
                check(inside(f, wanda, vision), "the Vision Wanda allowed walks in");
                check(!inside(f, wanda, stranger), "the Vision she did not allow is thrown back (the old rule)");
                check(stranger.getHealth() == strangerHealth, "without a scratch");
            }
            if (tick == 6) {
                // Once inside, being allowed doesn't let him out.
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
            }
            if (tick >= 9) {
                check(inside(f, wanda, vision), "an allowed Vision is held inside as well (tick " + tick + ")");
                check(vision.getHealth() < visionHealth, "and comes apart at the wall");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_white")
    public static void whiteVisionIsHeldTooButDoesNotComeApart(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        Powers.setVariant(vision, Variants.get(Power.VISION, "white_vision"));
        check("white_vision".equals(Powers.get(vision).variant()), "this is White Vision");
        // Wanda let him in, so the old rule (a hero is thrown back) would let him out again.
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(vision.getUUID()), 1);
        float health = vision.getHealth();

        f.everyTick(10, tick -> {
            if (tick == 2) {
                TestKit.sentPackets(vision);
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
            }
            if (tick >= 4) {
                check(inside(f, wanda, vision), "any look of Vision is held inside the Hex");
                check(vision.getHealth() == health, "White Vision is only thrown back: the wall doesn't hurt him");
                check(!flakedOrange(TestKit.sentPackets(vision)), "and he doesn't come apart");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_white_in")
    public static void whiteVisionGetsInOnlyIfWandaAllowsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer allowed = f.player(CENTER, FLOOR, 1.0, Power.VISION);
        ServerPlayer stranger = f.player(1.0, FLOOR, CENTER, Power.VISION);
        Powers.setVariant(allowed, Variants.get(Power.VISION, "white_vision"));
        Powers.setVariant(stranger, Variants.get(Power.VISION, "white_vision"));
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(allowed.getUUID()), 1);
        float health = stranger.getHealth();

        f.everyTick(10, tick -> {
            if (tick == 2) {
                walkTo(allowed, abs(helper, CENTER, FLOOR, 3.5));
                walkTo(stranger, abs(helper, 3.5, FLOOR, CENTER));
            }
            if (tick >= 5) {
                check(inside(f, wanda, allowed), "the White Vision Wanda allowed walks in");
                check(!inside(f, wanda, stranger), "the one she did not allow is thrown back");
                check(stranger.getHealth() == health, "without coming apart");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_release")
    public static void aBadlyHurtWandaCanNoLongerHoldVision(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        // Hurt, but not badly: she still holds him.
        wanda.setHealth(wanda.getMaxHealth() * 0.5F);

        f.everyTick(16, tick -> {
            if (tick == 2) {
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
            }
            if (tick == 4) {
                check(inside(f, wanda, vision), "at half her health Wanda still holds Vision in");
                // Badly hurt (30% by default): the Hex can no longer hold him.
                wanda.setHealth(wanda.getMaxHealth() * 0.25F);
            }
            if (tick == 6) {
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
            }
            if (tick >= 9) {
                check(!inside(f, wanda, vision), "with Wanda badly hurt, Vision walks out of her Hex");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_release_kill")
    public static void killingWandaFreesVision(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);

        f.everyTick(12, tick -> {
            if (tick == 2) {
                wanda.kill();
            }
            if (tick == 4) {
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
            }
            if (tick >= 7) {
                check(HexManager.data(helper.getLevel()).byOwner(wanda.getUUID()).isEmpty(), "Wanda's death dissolves her Hex");
                check(vision.isAlive() && vision.position().z < abs(helper, CENTER, FLOOR, 3.0).z, "and Vision walks free");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_release_gone")
    public static void withWandaGoneFromTheWorldNobodyHoldsVision(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        Hex hex = HexManager.data(helper.getLevel()).byOwner(wanda.getUUID()).orElseThrow();
        check(HexManager.holdsVision(hex, vision), "with Wanda here, her Hex holds Vision");
        helper.getLevel().getServer().getPlayerList().remove(wanda);
        check(!HexManager.holdsVision(hex, vision), "with her gone from the world, there is no one to hold him");
        check(HexManager.judge(hex, vision, true) == HexManager.Verdict.PASS, "and he may walk out");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_phase")
    public static void aPhasingVisionCannotSlipThroughEither(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(vision.getUUID()), 1);
        AbilityHandler.onKey(vision, 3, true);
        check(Powers.get(vision).visionPhased(), "Vision is intangible");
        float health = vision.getHealth();

        f.everyTick(12, tick -> {
            if (tick == 2) {
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
            }
            if (tick >= 4) {
                check(inside(f, wanda, vision), "phasing doesn't take him through a wall of reality (tick " + tick + ")");
                check(Powers.get(vision).visionPhased(), "he is still phased");
                check(vision.getHealth() < health, "and the wall tears at him even then");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_fly")
    public static void aFlyingVisionIsHeldAtTheCeilingOfTheWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR + 3.0, CENTER, Power.VISION);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(vision.getUUID()), 1);
        vision.getAbilities().flying = true;

        f.everyTick(12, tick -> {
            if (tick == 2) {
                // Up and out through the roof of the dome: the arena is 10 high, the wall 8 above Wanda's feet.
                walkTo(vision, abs(helper, CENTER, FLOOR + 8.4, CENTER));
                check(!inside(f, wanda, vision), "he flew out of the dome");
            }
            if (tick >= 4) {
                check(inside(f, wanda, vision), "flying doesn't get Vision out of the Hex (tick " + tick + ")");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_teleport")
    public static void noTeleportTakesVisionOutOfTheHex(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(vision.getUUID()), 1);
        Vec3 outside = abs(helper, CENTER, FLOOR, 1.0);
        Vec3 insideSpot = abs(helper, CENTER, FLOOR, 7.0);
        float health = vision.getHealth();
        TestKit.sentPackets(vision);

        // An Ender Pearl.
        boolean pearl = Platform.get().hooks().enderPearlTeleportCancelled(vision, outside.x, outside.y, outside.z,
                new ThrownEnderpearl(helper.getLevel(), vision), 5.0F, BlockHitResult.miss(outside, Direction.UP, vision.blockPosition()));
        check(pearl, "an Ender Pearl doesn't take Vision out of the Hex");
        check(flakedOrange(TestKit.sentPackets(vision)), "he flakes where he stands");
        // The mod's own teleports (the Bifrost, Loki's Tesseract, the Witches' Road, a Vibrate...) ask the same.
        check(!HexManager.onTeleport(helper.getLevel(), vision, outside), "no teleport of the mod takes him out");
        check(HexManager.onTeleport(helper.getLevel(), vision, insideSpot), "but one that stays inside is fine");
        check(inside(f, wanda, vision) && vision.getHealth() == health, "he is where he was, unhurt");

        // A portal.
        check(portalRefused(vision), "a portal doesn't take him out either");
        ServerPlayer nobody = f.player(CENTER, FLOOR, 6.0);
        check(!portalRefused(nobody), "(control: anyone else may go)");

        // Chorus Fruit: it lands first and is judged after, and then he is thrown back.
        HexManager.rememberTeleportStart(vision);
        walkTo(vision, outside);
        helper.runAfterDelay(3, () -> {
            check(inside(f, wanda, vision), "after a Chorus Fruit he is back inside");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_free_dissolve")
    public static void visionIsFreeOnceTheHexIsGone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        float health = vision.getHealth();

        f.everyTick(14, tick -> {
            if (tick == 2) {
                HexManager.handleAction(wanda, HexAction.DISSOLVE, Optional.empty(), 0);
                check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).isEmpty(), "the Hex is gone");
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
            }
            if (tick >= 6) {
                check(vision.getZ() < helper.absoluteVec(new Vec3(0, 0, 2.5)).z, "Vision walked out: nothing holds him");
                check(vision.getHealth() == health, "and nothing hurt him");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_free_shrunk")
    public static void visionIsFreeWhenWandaShrinksTheHexPastHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 16);
        ServerPlayer wanda = f.player(CENTER, FLOOR, CENTER, Power.SCARLET_WITCH);
        AbilityHandler.onKey(wanda, 5, true);
        Hex hex = HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow();
        check(hex.radius() == 16, "a wide Hex");
        // Ten blocks from its middle: inside a Hex of 16, outside one of 8.
        ServerPlayer vision = f.player(CENTER, FLOOR, 0.6, Power.VISION);
        float health = vision.getHealth();

        f.everyTick(14, tick -> {
            if (tick == 3) {
                check(inside(f, wanda, vision), "Vision is inside the wide Hex");
                HexManager.handleAction(wanda, HexAction.SET_RADIUS, Optional.empty(), 8);
                check(!inside(f, wanda, vision), "the wall now runs inside of him");
            }
            if (tick >= 8) {
                check(!inside(f, wanda, vision), "Wanda let him go: he is not dragged back");
                check(vision.getHealth() == health, "and nothing hurt him");
                // Walking in again is entering, and the old rules apply: Wanda did not allow him.
                walkTo(vision, abs(helper, CENTER, FLOOR, 4.0));
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_creative")
    public static void creativeAndSpectatorVisionsAreNotHeld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer creative = f.player(CENTER, FLOOR, 5.0, GameType.CREATIVE);
        Powers.setPower(creative, Power.VISION);
        ServerPlayer spectator = f.player(5.0, FLOOR, CENTER, GameType.SPECTATOR);
        Powers.setPower(spectator, Power.VISION);
        check(inside(f, wanda, creative) && inside(f, wanda, spectator), "both start inside");

        f.everyTick(12, tick -> {
            if (tick == 2) {
                walkTo(creative, abs(helper, CENTER, FLOOR, 1.0));
                walkTo(spectator, abs(helper, 1.0, FLOOR, CENTER));
            }
            if (tick >= 5) {
                check(!inside(f, wanda, creative), "a Creative Vision walks out");
                check(!inside(f, wanda, spectator), "a Spectator Vision floats out");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_damage", timeoutTicks = 160)
    public static void theWallHurtsMoreWhileHeKeepsPushingButNeverKills(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        vision.setHealth(vision.getMaxHealth());
        float[] taken = new float[6];

        // A push every 12 ticks: the wall bites at most twice a second.
        f.everyTick(80, tick -> {
            int push = (tick - 1) / 12;
            if ((tick - 1) % 12 == 0 && push < 6) {
                if (push == 4) {
                    // Worn down to a few points: the wall still can't take the last half heart.
                    vision.setHealth(3.0F);
                }
                vision.invulnerableTime = 0;
                taken[push] = vision.getHealth();
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
            }
            if ((tick - 1) % 12 == 3 && push < 6) {
                taken[push] -= vision.getHealth();
                check(inside(f, wanda, vision), "thrown back inside, push " + push);
            }
            if (tick == 69) {
                assertNear(taken[0], 2.0, 1.0E-4, "the first push: one heart");
                assertNear(taken[1], 4.0, 1.0E-4, "pushing again: two hearts");
                assertNear(taken[2], 6.0, 1.0E-4, "and again: three");
                assertNear(taken[3], 8.0, 1.0E-4, "and again: four, the most");
                assertNear(taken[4], 2.0, 1.0E-4, "with three points of health left it takes two and leaves the last");
                assertNear(taken[5], 0.0, 1.0E-4, "and with one left it takes nothing");
                check(vision.isAlive() && vision.getHealth() >= 1.0F, "he never dies of it");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_hold_on")
    public static void leaningOnTheWallHurtsAtMostTwiceASecond(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        vision.setHealth(vision.getMaxHealth());
        float[] before = new float[1];

        f.everyTick(14, tick -> {
            if (tick == 2) {
                before[0] = vision.getHealth();
            }
            if (tick >= 2 && tick <= 9 && tick % 2 == 0) {
                // Holding the key down: back at the wall every other tick.
                vision.invulnerableTime = 0;
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
            }
            if (tick == 12) {
                check(inside(f, wanda, vision), "held inside all the while");
                assertNear(before[0] - vision.getHealth(), 2.0, 1.0E-4, "four throws back in a few ticks cost one bite: one heart");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_rest", timeoutTicks = 200)
    public static void theWallForgetsHisPushingWhenHeRests(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        vision.setHealth(vision.getMaxHealth());
        float[] taken = new float[3];

        helper.runAfterDelay(2, () -> {
            vision.invulnerableTime = 0;
            taken[0] = vision.getHealth();
            walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
        });
        helper.runAfterDelay(4, () -> taken[0] -= vision.getHealth());
        // Leaning on the wall again within three seconds: the second bite, two hearts more.
        helper.runAfterDelay(15, () -> {
            vision.invulnerableTime = 0;
            taken[1] = vision.getHealth();
            walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
        });
        helper.runAfterDelay(17, () -> taken[1] -= vision.getHealth());
        // Three seconds of leaving the wall alone, and the next push costs a single heart again.
        helper.runAfterDelay(100, () -> {
            vision.invulnerableTime = 0;
            taken[2] = vision.getHealth();
            walkTo(vision, abs(helper, CENTER, FLOOR, 1.0));
        });
        helper.runAfterDelay(102, () -> {
            taken[2] -= vision.getHealth();
            assertNear(taken[0], 2.0, 1.0E-4, "first push");
            assertNear(taken[1], 4.0, 1.0E-4, "second push right after");
            assertNear(taken[2], 2.0, 1.0E-4, "after a rest, one heart again");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "hex_holds_floor")
    public static void leaningOnTheWallDoesNotSinkHimIntoTheFloor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer vision = f.player(CENTER, FLOOR, 5.0, Power.VISION);
        vision.setHealth(vision.getMaxHealth());
        double floor = abs(helper, CENTER, FLOOR, 5.0).y;
        double wallZ = abs(helper, CENTER, FLOOR, 1.0).z;

        f.everyTick(26, tick -> {
            if (tick >= 2 && tick <= 20 && tick % 2 == 0) {
                // Holding the key down: his client has moved him past the wall before the tick starts, so where he was
                // last tick is already past the wall too (and the wall must put him back at the height he was).
                vision.invulnerableTime = 0;
                walkTo(vision, new Vec3(vision.getX(), vision.getY(), wallZ));
                vision.setOldPosAndRot();
            }
            check(vision.getY() > floor - 1.0E-3, "thrown back at the wall, Vision sinks into the floor (tick " + tick + ", "
                    + (vision.getY() - floor) + " blocks)");
            if (tick == 24) {
                check(inside(f, wanda, vision), "and he is held inside all the while");
                return true;
            }
            return false;
        });
    }

    private HexHoldsVisionTests() {
    }
}

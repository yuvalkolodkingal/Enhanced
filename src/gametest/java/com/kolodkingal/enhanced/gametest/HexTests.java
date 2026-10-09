package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Wanda's Hex. Each test has its own batch: a Hex affects everything around it and some tests force the Hex
 * death chance, which is a server-wide setting.
 *
 * <p>The Hex is raised by Wanda standing in the middle of the arena (10.5, 2, 10.5) with radius 8. Crossings
 * are left to the real server tick (the Hex is checked at the end of every level tick); players are moved the
 * way a client's movement packets move them.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class HexTests {
    private static final double CENTER = 10.5;
    private static final int RADIUS = 8;

    /** Creates Wanda in the middle of the arena and lets her press N. */
    private static ServerPlayer raiseHex(TestKit.Fixture f) {
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, RADIUS);
        ServerPlayer wanda = f.player(CENTER, FLOOR, CENTER, Power.SCARLET_WITCH);
        AbilityHandler.onKey(wanda, 5, true);
        Optional<Hex> hex = HexManager.ownedBy(f.level(), wanda.getUUID());
        check(hex.isPresent(), "N raises the Hex");
        check(hex.get().radius() == RADIUS, "Hex radius " + hex.get().radius());
        return wanda;
    }

    private static Vec3 abs(GameTestHelper helper, double x, double y, double z) {
        return helper.absoluteVec(new Vec3(x, y, z));
    }

    private static boolean inside(TestKit.Fixture f, ServerPlayer wanda, LivingEntity entity) {
        Hex hex = HexManager.ownedBy(f.level(), wanda.getUUID()).orElseThrow();
        return hex.contains(entity.position().add(0, entity.getBbHeight() * 0.5, 0));
    }

    /** What a client's movement packet does: the position changes, the previous-tick position doesn't. */
    private static void walkTo(ServerPlayer player, Vec3 pos) {
        player.absMoveTo(pos.x, pos.y, pos.z);
    }

    @GameTest(template = ARENA, batch = "hex_death")
    public static void hexWallKillsCreatures(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        ServerPlayer wanda = raiseHex(f);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(CENTER, FLOOR, 1.5));
        check(!inside(f, wanda, pig), "the pig starts outside");

        f.everyTick(10, tick -> {
            if (tick == 2) {
                Vec3 in = abs(helper, CENTER, FLOOR, 5.5);
                pig.teleportTo(in.x, in.y, in.z);
            }
            return tick > 2 && !pig.isAlive();
        });
    }

    @GameTest(template = ARENA, batch = "hex_creatures_allowed")
    public static void hexLetsCreaturesInWhenAllowed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        ServerPlayer wanda = raiseHex(f);
        HexManager.handleAction(wanda, HexAction.SET_CREATURES, Optional.empty(), 1);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(CENTER, FLOOR, 1.5));

        f.everyTick(10, tick -> {
            if (tick == 2) {
                Vec3 in = abs(helper, CENTER, FLOOR, 5.5);
                pig.teleportTo(in.x, in.y, in.z);
            }
            check(pig.isAlive(), "Wanda allowed creatures: the pig must survive");
            return tick >= 8 && inside(f, wanda, pig);
        });
    }

    @GameTest(template = ARENA, batch = "hex_photon")
    public static void hexSurvivorBecomesPhoton(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 0.0);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer player = f.player(CENTER, FLOOR, 2.0);
        check(!inside(f, wanda, player), "the player starts outside");

        f.everyTick(10, tick -> {
            if (tick == 2) {
                walkTo(player, abs(helper, CENTER, FLOOR, 2.6));
                check(inside(f, wanda, player), "walked through the wall");
            }
            if (tick > 2 && Powers.is(player, Power.PHOTON)) {
                check(player.isAlive(), "the survivor lives");
                check(inside(f, wanda, player), "and is inside the Hex");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "hex_player_death")
    public static void hexWallKillsPowerlessPlayers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer player = f.player(CENTER, FLOOR, 2.0);

        f.everyTick(10, tick -> {
            if (tick == 2) {
                walkTo(player, abs(helper, CENTER, FLOOR, 2.6));
            }
            return tick > 2 && player.isDeadOrDying() && Powers.is(player, Power.NONE);
        });
    }

    @GameTest(template = ARENA, batch = "hex_bounce")
    public static void hexBouncesPoweredIntruders(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer magneto = f.player(CENTER, FLOOR, 2.0, Power.MAGNETO);
        check(!inside(f, wanda, magneto), "Magneto starts outside");

        f.everyTick(10, tick -> {
            if (tick == 2) {
                walkTo(magneto, abs(helper, CENTER, FLOOR, 2.6));
                check(inside(f, wanda, magneto), "Magneto steps through the wall");
            }
            if (tick >= 4) {
                check(magneto.isAlive() && Powers.is(magneto, Power.MAGNETO), "the wall can't kill someone with powers");
                check(!inside(f, wanda, magneto), "Magneto must be thrown back outside the Hex (tick " + tick + ")");
                return tick >= 6;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "hex_allowed")
    public static void hexLetsAllowedPlayersIn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer friend = f.player(CENTER, FLOOR, 2.0);
        ServerPlayer magneto = f.player(2.0, FLOOR, CENTER, Power.MAGNETO);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(friend.getUUID()), 1);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(magneto.getUUID()), 1);
        Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(19.0, FLOOR, CENTER));
        pet.tame(friend);
        Pig stranger = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(CENTER, FLOOR, 19.5));

        f.everyTick(10, tick -> {
            if (tick == 2) {
                walkTo(friend, abs(helper, CENTER, FLOOR, 2.6));
                walkTo(magneto, abs(helper, 2.6, FLOOR, CENTER));
                Vec3 in = abs(helper, 15.5, FLOOR, CENTER);
                pet.teleportTo(in.x, in.y, in.z);
                Vec3 in2 = abs(helper, CENTER, FLOOR, 15.5);
                stranger.teleportTo(in2.x, in2.y, in2.z);
            }
            if (tick >= 4) {
                check(friend.isAlive() && Powers.is(friend, Power.NONE), "an allowed player crosses safely");
                check(inside(f, wanda, friend), "the allowed player is inside");
                check(inside(f, wanda, magneto), "an allowed hero is not bounced");
                check(pet.isAlive() && inside(f, wanda, pet), "pets of an allowed player pass safely");
                check(!stranger.isAlive(), "creatures are still not allowed (control: the wall works)");
                return tick >= 6;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "hex_teleport")
    public static void hexStopsEnderPearls(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer magneto = f.player(CENTER, FLOOR, 1.5, Power.MAGNETO);
        ServerPlayer nobody = f.player(1.5, FLOOR, CENTER);
        Vec3 target = abs(helper, CENTER, FLOOR, CENTER + 3);

        EntityTeleportEvent.EnderPearl magnetoPearl = EventHooks.onEnderPearlLand(magneto, target.x, target.y, target.z,
                new ThrownEnderpearl(helper.getLevel(), magneto), 5.0F, BlockHitResult.miss(target, net.minecraft.core.Direction.UP, magneto.blockPosition()));
        check(magnetoPearl.isCanceled(), "a hero can't pearl into the Hex");
        check(magneto.isAlive(), "and isn't hurt by trying");

        EntityTeleportEvent.EnderPearl nobodyPearl = EventHooks.onEnderPearlLand(nobody, target.x, target.y, target.z,
                new ThrownEnderpearl(helper.getLevel(), nobody), 5.0F, BlockHitResult.miss(target, net.minecraft.core.Direction.UP, nobody.blockPosition()));
        check(nobodyPearl.isCanceled(), "the unlucky player doesn't arrive");
        check(nobody.isDeadOrDying(), "teleporting across the wall faces the same price (death chance forced to 100%)");

        EntityTeleportEvent.EnderPearl wandaPearl = EventHooks.onEnderPearlLand(wanda, target.x, target.y, target.z,
                new ThrownEnderpearl(helper.getLevel(), wanda), 5.0F, BlockHitResult.miss(target, net.minecraft.core.Direction.UP, wanda.blockPosition()));
        check(!wandaPearl.isCanceled(), "Wanda goes where she likes");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "hex_dissolve")
    public static void hexCollapsesWhenWandaDies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        wanda.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        check(wanda.isDeadOrDying(), "Wanda died");
        check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).isEmpty(), "her Hex collapses when she dies");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "hex_dissolve")
    public static void sneakNDissolvesTheHex(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        AbilityHandler.onKey(wanda, 5, true);
        check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).isPresent(), "N with a Hex up keeps it (opens the screen)");
        HexManager.handleAction(wanda, HexAction.SET_RADIUS, Optional.empty(), 500);
        check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow().radius() == 96, "the Hex grows to at most 96");
        HexManager.handleAction(wanda, HexAction.SET_RADIUS, Optional.empty(), 1);
        check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow().radius() == 8, "and shrinks to at least 8");
        wanda.setShiftKeyDown(true);
        AbilityHandler.onKey(wanda, 5, true);
        check(HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).isEmpty(), "Sneak + N dissolves the Hex");
        helper.succeed();
    }

    private HexTests() {
    }
}

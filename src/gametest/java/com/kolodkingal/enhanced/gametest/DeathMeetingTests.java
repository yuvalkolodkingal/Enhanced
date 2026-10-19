package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.DeathMeetingConfig;
import com.kolodkingal.enhanced.death.DeathMeeting;
import com.kolodkingal.enhanced.death.NetherLanding;
import com.kolodkingal.enhanced.network.DeathMeetingPayload;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The meeting with Death (3.7, lane L8; Eilon's request): a player who dies is offered the meeting before vanilla's
 * death packet; only a dead player's choice is heard; "Start again" respawns him as vanilla does; "Start again in the
 * Nether" puts him on a safe spot in the real Nether with Fire Resistance; the search refuses lava, fire, bedrock and
 * the roof in a fake Nether built in the arena, and builds its platform when nothing is safe; a healer saved by his
 * healing factor never meets her; Death off, the Nether not allowed, {@code doImmediateRespawn}: no offer (or no
 * Nether) and the choice starts him at home; the Fire Resistance follows the config. Batches: {@code death_meeting},
 * and one per changed config or game rule ({@code death_meeting_cfg*}).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class DeathMeetingTests {
    private static final String BATCH = "death_meeting";
    private static final String CFG = "death_meeting_cfg";

    private DeathMeetingTests() {
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** The Offer among the packets sent, or null; and whether it came before vanilla's death packet. */
    @Nullable
    private static DeathMeetingPayload.Offer offerIn(List<Object> packets, boolean[] beforeDeath) {
        DeathMeetingPayload.Offer offer = null;
        boolean deathSeen = false;
        for (Object packet : packets) {
            if (packet instanceof ClientboundPlayerCombatKillPacket) {
                deathSeen = true;
            } else if (packet instanceof ClientboundCustomPayloadPacket custom
                    && custom.payload() instanceof DeathMeetingPayload.Offer found) {
                offer = found;
                beforeDeath[0] = !deathSeen;
            }
        }
        return offer;
    }

    /** Kills him (as /kill does) and returns what the server offered him (null: nothing). */
    @Nullable
    private static DeathMeetingPayload.Offer die(ServerPlayer player) {
        TestKit.sentPackets(player);
        player.kill();
        check(player.isDeadOrDying(), "he is dead");
        return offerIn(TestKit.sentPackets(player), new boolean[1]);
    }

    /** The player's body now (after a respawn, a new one), removed from the server when the test ends. */
    private static ServerPlayer now(TestKit.Fixture f, UUID id) {
        MinecraftServer server = f.level().getServer();
        ServerPlayer body = server.getPlayerList().getPlayer(id);
        check(body != null, "he is on the server");
        f.onClose(() -> {
            if (server.getPlayerList().getPlayer(id) == body) {
                server.getPlayerList().remove(body);
            }
        });
        return body;
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), state);
                }
            }
        }
    }

    /** Two blocks of air at {@code feet} (relative), over {@code floor}. */
    private static void pocket(GameTestHelper helper, BlockPos feet, BlockState floor) {
        helper.setBlock(feet.below(), floor);
        helper.setBlock(feet, Blocks.AIR.defaultBlockState());
        helper.setBlock(feet.above(), Blocks.AIR.defaultBlockState());
    }

    // ------------------------------------------------------------------------------------------------------------
    // The offer, and who is heard

    /** A death is offered the meeting (with the Nether and 10 s of Fire Resistance), before vanilla's death packet. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aDeathIsOfferedTheMeetingBeforeVanillasDeathPacket(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        TestKit.sentPackets(player);
        player.kill();
        boolean[] before = new boolean[1];
        DeathMeetingPayload.Offer offer = offerIn(TestKit.sentPackets(player), before);
        check(offer != null, "Death is offered");
        check(offer.nether() && offer.fireSeconds() == 10, "with the Nether and 10 seconds of Fire Resistance: "
                + offer);
        check(before[0], "before vanilla's death packet, so the client swaps the screen it opens");
        DeathMeeting.choose(player, DeathMeeting.AGAIN);
        now(f, player.getUUID());
        helper.succeed();
    }

    /** A living player's choice is refused: nothing happens to him. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aLivingPlayersChoiceIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        float health = player.getHealth();
        check(DeathMeeting.choose(player, DeathMeeting.NETHER) == DeathMeeting.Result.REFUSED, "the Nether is refused");
        check(DeathMeeting.choose(player, DeathMeeting.AGAIN) == DeathMeeting.Result.REFUSED, "and so is home");
        MinecraftServer server = f.level().getServer();
        check(server.getPlayerList().getPlayer(player.getUUID()) == player, "he is the same body");
        check(player.level() == f.level() && player.getHealth() == health && player.isAlive(),
                "where he was, unharmed");
        helper.succeed();
    }

    /** "Start again" respawns him as vanilla does (the Overworld), and the dead body's second answer is refused. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void startAgainRespawnsHimAsVanillaAndASecondAnswerIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        check(die(player) != null, "Death is offered");
        check(DeathMeeting.choose(player, DeathMeeting.AGAIN) == DeathMeeting.Result.AGAIN, "he starts again");
        ServerPlayer reborn = now(f, player.getUUID());
        check(reborn != player && reborn.isAlive(), "a new, living body");
        check(reborn.level().dimension() == Level.OVERWORLD, "at home in the Overworld");
        check(!reborn.hasEffect(MobEffects.FIRE_RESISTANCE), "without the Nether's Fire Resistance");
        check(DeathMeeting.choose(player, DeathMeeting.NETHER) == DeathMeeting.Result.REFUSED,
                "the old body's answer is refused");
        check(reborn.level().dimension() == Level.OVERWORLD, "and he stays at home");
        helper.succeed();
    }

    /** A healer whose healing factor saves him (DOWN) never dies, so Death is never offered. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aHealerSavedByHisFactorNeverMeetsDeath(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wolverine = f.player(5.5, FLOOR, 5.5, Power.WOLVERINE);
        wolverine.setHealth(wolverine.getMaxHealth());
        TestKit.sentPackets(wolverine);
        wolverine.hurt(wolverine.damageSources().generic(), 1000.0F);
        check(wolverine.isAlive() && wolverine.getHealth() > 0.0F, "he is DOWN, not dead");
        check(offerIn(TestKit.sentPackets(wolverine), new boolean[1]) == null, "and Death is not offered");
        check(DeathMeeting.choose(wolverine, DeathMeeting.NETHER) == DeathMeeting.Result.REFUSED,
                "nor is a choice heard");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Nether

    /** "Start again in the Nether": the real Nether, a safe spot near the spawn ÷ 8, Fire Resistance, Rio's line. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theNetherChoicePutsHimOnASafeSpotInTheNether(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = f.level().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        check(nether != null, "the test server has a Nether");
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        check(die(player) != null, "Death is offered");
        check(DeathMeeting.choose(player, DeathMeeting.NETHER) == DeathMeeting.Result.NETHER,
                "he starts in the Nether");
        ServerPlayer reborn = now(f, player.getUUID());
        check(reborn.isAlive() && reborn.level().dimension() == Level.NETHER, "alive, in the Nether");
        BlockPos feet = reborn.blockPosition();
        check(NetherLanding.safe(nether, feet, NetherLanding.top(nether)),
                "on a safe spot (floor, air, no lava or fire near, under the roof): " + feet);
        BlockPos spawn = server.overworld().getSharedSpawnPos();
        int reach = NetherLanding.RADIUS + 4;
        check(Math.abs(feet.getX() - spawn.getX() / 8) <= reach && Math.abs(feet.getZ() - spawn.getZ() / 8) <= reach,
                "near the world's spawn ÷ 8 (" + spawn + "): " + feet);
        MobEffectInstance fire = reborn.getEffect(MobEffects.FIRE_RESISTANCE);
        check(fire != null && fire.getDuration() > 150 && fire.getDuration() <= 200,
                "with 10 seconds of Fire Resistance: " + fire);
        helper.succeed();
    }

    /**
     * The search in a fake Nether (netherrack in the arena, the feet's height limited to the arena's): a pocket by
     * lava, one on bedrock, one by fire and one over the "roof" are refused; the one safe pocket further out is found.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theSearchRefusesLavaFireBedrockAndTheRoofAndFindsTheSafePocket(GameTestHelper helper) {
        TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockState rack = Blocks.NETHERRACK.defaultBlockState();
        fill(helper, 2, 2, 2, 18, 8, 18, rack);
        BlockPos lavaPocket = new BlockPos(10, 4, 10);
        pocket(helper, lavaPocket, rack);
        helper.setBlock(lavaPocket.east(), Blocks.LAVA.defaultBlockState());
        BlockPos bedrockPocket = new BlockPos(8, 4, 10);
        pocket(helper, bedrockPocket, Blocks.BEDROCK.defaultBlockState());
        BlockPos firePocket = new BlockPos(10, 4, 7);
        pocket(helper, firePocket, rack);
        helper.setBlock(firePocket.west(), Blocks.FIRE.defaultBlockState());
        BlockPos roofPocket = new BlockPos(10, 7, 11);
        pocket(helper, roofPocket, rack);
        BlockPos good = new BlockPos(13, 4, 12);
        pocket(helper, good, rack);

        int minY = helper.absolutePos(new BlockPos(0, 2, 0)).getY();
        int maxY = helper.absolutePos(new BlockPos(0, 6, 0)).getY();
        check(level.getFluidState(helper.absolutePos(lavaPocket.east())).getType() == Fluids.LAVA, "the lava is there");
        check(level.getBlockState(helper.absolutePos(firePocket.west())).is(Blocks.FIRE), "the fire burns");
        check(!NetherLanding.safe(level, helper.absolutePos(lavaPocket), maxY), "lava beside him is refused");
        check(!NetherLanding.safe(level, helper.absolutePos(bedrockPocket), maxY), "bedrock underfoot is refused");
        check(!NetherLanding.safe(level, helper.absolutePos(firePocket), maxY), "fire beside him is refused");
        check(!NetherLanding.safe(level, helper.absolutePos(roofPocket), maxY), "over the roof is refused");
        check(NetherLanding.safe(level, helper.absolutePos(roofPocket), maxY + 4), "(only for the roof)");
        check(NetherLanding.safe(level, helper.absolutePos(good), maxY), "the good pocket is safe");
        Optional<BlockPos> found = NetherLanding.find(level, helper.absolutePos(lavaPocket), 6, minY, maxY);
        check(found.isPresent() && found.get().equals(helper.absolutePos(good)),
                "the search finds the good pocket: " + found + " (wanted " + helper.absolutePos(good) + ")");
        helper.succeed();
    }

    /** Nothing safe (solid netherrack, lava about): the platform is built, safe, of obsidian and netherrack. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theFallbackPlatformIsBuiltWhenNothingIsSafe(GameTestHelper helper) {
        TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockState rack = Blocks.NETHERRACK.defaultBlockState();
        fill(helper, 2, 2, 2, 18, 8, 18, rack);
        BlockPos centre = new BlockPos(10, 4, 10);
        BlockPos[] lava = {new BlockPos(13, 4, 10), new BlockPos(10, 5, 7), new BlockPos(12, 7, 12)};
        for (BlockPos at : lava) {
            helper.setBlock(at, Blocks.LAVA.defaultBlockState());
        }
        int minY = helper.absolutePos(new BlockPos(0, 2, 0)).getY();
        int maxY = helper.absolutePos(new BlockPos(0, 6, 0)).getY();
        BlockPos absolute = helper.absolutePos(centre);
        check(NetherLanding.find(level, absolute, 6, minY, maxY).isEmpty(), "nothing is safe");
        BlockPos feet = NetherLanding.platform(level, absolute, minY, maxY);
        check(feet.equals(absolute), "the platform stands at the centre: " + feet);
        check(NetherLanding.safe(level, feet, maxY), "and he can stand there safely");
        check(level.getBlockState(feet.below()).is(Blocks.OBSIDIAN), "on obsidian");
        check(level.getBlockState(feet.below().east(2)).is(Blocks.NETHERRACK), "in a ring of netherrack");
        check(level.getBlockState(feet.above(2)).isAir() && level.getBlockState(feet.north(2).above()).isAir(),
                "with room round him");
        for (BlockPos at : lava) {
            check(level.getBlockState(helper.absolutePos(at)).is(Blocks.NETHERRACK), "the lava at " + at
                    + " is stone now");
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The config and the game rule

    /** Death off: no offer (vanilla's screen), and a Nether answer starts him at home. */
    @GameTest(template = ARENA, batch = CFG)
    public static void withDeathOffThereIsNoOfferAndHeStartsAtHome(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DeathMeetingConfig.ENABLED, false);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        check(DeathMeeting.offer(player) == null, "nothing to offer");
        check(die(player) == null, "no offer at his death: the vanilla screen");
        check(DeathMeeting.choose(player, DeathMeeting.NETHER) == DeathMeeting.Result.AGAIN, "he starts again");
        check(now(f, player.getUUID()).level().dimension() == Level.OVERWORLD, "at home");
        helper.succeed();
    }

    /** The Nether not allowed: Death is offered without it, and a Nether answer starts him at home. */
    @GameTest(template = ARENA, batch = CFG + "_nether")
    public static void withTheNetherNotAllowedSheOffersOnlyHome(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DeathMeetingConfig.ALLOW_NETHER, false);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        DeathMeetingPayload.Offer offer = die(player);
        check(offer != null && !offer.nether(), "Death comes, without the Nether: " + offer);
        check(DeathMeeting.choose(player, DeathMeeting.NETHER) == DeathMeeting.Result.AGAIN, "he starts again");
        check(now(f, player.getUUID()).level().dimension() == Level.OVERWORLD, "at home");
        helper.succeed();
    }

    /** {@code doImmediateRespawn}: vanilla's way (no offer, no Nether), the answer starts him at home. */
    @GameTest(template = ARENA, batch = CFG + "_immediate")
    public static void doImmediateRespawnKeepsVanillasWay(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = f.level().getServer();
        GameRules.BooleanValue rule = server.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
        boolean was = rule.get();
        rule.set(true, server);
        f.onClose(() -> rule.set(was, server));
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        check(DeathMeeting.vanillaWay(player) && DeathMeeting.offer(player) == null, "vanilla's way: no offer");
        check(die(player) == null, "no offer at his death");
        check(DeathMeeting.choose(player, DeathMeeting.NETHER) == DeathMeeting.Result.AGAIN, "a Nether answer");
        check(now(f, player.getUUID()).level().dimension() == Level.OVERWORLD, "starts him at home");
        helper.succeed();
    }

    /** {@code fireResistanceSeconds} 3: the Nether start gives 3 seconds; the offer says so. */
    @GameTest(template = ARENA, batch = CFG + "_fire", timeoutTicks = 400)
    public static void theFireResistanceFollowsTheConfig(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DeathMeetingConfig.FIRE_RESISTANCE_SECONDS, 3);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        DeathMeetingPayload.Offer offer = die(player);
        check(offer != null && offer.fireSeconds() == 3, "the offer tells 3 seconds: " + offer);
        check(DeathMeeting.choose(player, DeathMeeting.NETHER) == DeathMeeting.Result.NETHER, "to the Nether");
        ServerPlayer reborn = now(f, player.getUUID());
        MobEffectInstance fire = reborn.getEffect(MobEffects.FIRE_RESISTANCE);
        check(reborn.level().dimension() == Level.NETHER && fire != null && fire.getDuration() <= 60
                && fire.getDuration() > 40, "3 seconds of Fire Resistance: " + fire);
        helper.succeed();
    }
}

package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.MagnetPull;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.Telekinesis.Mode;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.hex.HexVisions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosFight;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.HexTests.abs;
import static com.kolodkingal.enhanced.gametest.HexTests.inside;
import static com.kolodkingal.enhanced.gametest.HexTests.raiseHex;
import static com.kolodkingal.enhanced.gametest.HexTests.walkTo;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The tests of 3.1 that cross two lanes (spec 18.6, step I), of the ones that need only Magneto's metal and the boss
 * telekinesis: Magneto and Vision, Magneto and the Hex, what the Magnetic Pull and R tell each other about fixed metal,
 * Mjolnir, the three villains, and the Hex's Vision set down safely. Each is named in the register (X1 to X9 and X13)
 * and has the batch the register gives it ({@code v31_cross_*}); a test that changes a config value or PvP has a batch
 * of its own, as in {@code docs/HEROES-AND-BOSSES.md} section 5.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class V31CrossTests {
    private static final double CENTER = 10.5;
    /** How close to the listener by the wall a crack or a flame has to be to count as the Vision's. */
    private static final double NEAR = 8.0;

    /** Presses the key of an ability, with Sneak held or not. */
    private static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /** What {@code tick}'s packets say: a key, remembered if it is not null. */
    private static String remember(String last, String key) {
        return key == null ? last : key;
    }

    /** Clears the strip's rest and the pull's, so a second press in the same test is not the cooldown's. */
    private static void rested(ServerPlayer magneto) {
        PowerData data = Powers.get(magneto);
        long now = magneto.serverLevel().getGameTime();
        data.startCooldown(1, now, 0);
        data.startCooldown(3, now, 0);
    }

    /**
     * When the test ends, these holders let go of what they hold. (Tests of a batch run together, so a test of a shared
     * batch lets go of its own grips and does not clear everybody's.)
     */
    private static void letGoAtTheEnd(TestKit.Fixture f, ServerPlayer... holders) {
        f.onClose(() -> {
            for (ServerPlayer holder : holders) {
                Telekinesis.release(holder, false);
            }
        });
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    // ------------------------------------------------------------------------------------------------------------
    // X1: Vision

    /**
     * X1 (M3, PvP off). Magneto's R holds a Vision player; Vision phases (V): the next tick Magneto holds nothing and
     * reads {@code telekinesis.slipped}; R at the phased Vision once more says {@code telekinesis.phased}.
     */
    @GameTest(template = ARENA, batch = "v31_cross_vision", timeoutTicks = 40)
    public static void magnetoHoldsVisionUntilHePhases(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.pvp(false);
        ServerPlayer magneto = f.player(3.5, FLOOR, CENTER, Power.MAGNETO);
        ServerPlayer vision = f.player(CENTER, FLOOR, CENTER, Power.VISION);

        lookAt(magneto, vision);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.holds(magneto, vision), "R holds a Vision (he is made of metal)");
        PowerTicker.tick(magneto);
        check(Telekinesis.holds(magneto, vision), "and goes on holding him while he is solid");

        AbilityHandler.onKey(vision, 3, true);
        check(Powers.get(vision).visionPhased(), "Vision phases");
        lastActionBarKey(magneto);
        PowerTicker.tick(magneto);
        check(!Telekinesis.isHolding(magneto) && !Telekinesis.isHeld(vision), "the next tick Magneto holds nothing");
        check(Telekinesis.SLIPPED.equals(lastActionBarKey(magneto)), "and is told he slipped away");

        lookAt(magneto, vision);
        AbilityHandler.onKey(magneto, 1, true);
        check(!Telekinesis.isHolding(magneto), "R at the phased Vision holds nothing");
        check(Telekinesis.PHASED.equals(lastActionBarKey(magneto)), "it says he is phased");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // X2, X4, X9: the Hex and what Magneto carries into its wall

    /**
     * X2 (M4). Only Magneto's telekinesis may take the Hex's Vision (the tag names him for Magneto alone). Allowed in
     * the Hex, Magneto holds him and looks out past the wall: within 40 ticks Magneto holds nothing, the Vision is
     * inside, his last line is {@code telekinesis.hex_holds}, and he did not come apart: a listener by the wall heard
     * no {@code hex_cross} (nor the crackle), and no small flame was sent.
     */
    @GameTest(template = ARENA, batch = "v31_cross_hex", timeoutTicks = 60)
    public static void onlyMagnetoLiftsTheHexVision(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer magneto = f.player(CENTER, FLOOR, 4.0, Power.MAGNETO);
        ServerPlayer billy = f.player(CENTER, FLOOR, 16.0, Power.BILLY);
        ServerPlayer listener = f.player(CENTER, FLOOR, 1.2);
        letGoAtTheEnd(f, magneto);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(magneto.getUUID()), 1);
        HexVisionEntity vision = HexVisions.of(wanda.getUUID());
        check(vision != null, "Wanda's Hex has raised a Vision");
        vision.setNoAi(true);
        Vec3 at = abs(helper, CENTER, FLOOR, 8.0);
        vision.moveTo(at.x, at.y, at.z);

        check(Telekinesis.canLift(magneto, vision, Mode.MAGNETO), "Magneto may lift the Hex's Vision");
        check(!Telekinesis.canLift(billy, vision, Mode.BILLY), "Billy may not");
        check(!Telekinesis.canLift(wanda, vision, Mode.WANDA), "nor Wanda");

        lookAt(magneto, vision);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.holds(magneto, vision), "Magneto holds him");
        lastActionBarKey(magneto);
        TestKit.sentPackets(listener);
        String[] last = {null};
        boolean[] heard = {false};
        f.everyTick(40, tick -> {
            TestKit.look(magneto, 180.0F, 0.0F); // out past the wall, to the north
            PowerTicker.tick(magneto);
            last[0] = remember(last[0], lastActionBarKey(magneto));
            List<Object> packets = TestKit.sentPackets(listener);
            // Only what happens by the wall: the test beside this one (a Vision coming apart for another reason) may
            // be heard too.
            for (TestKit.HeardSound sound : TestKit.soundsIn(packets)) {
                heard[0] |= sound.at().distanceTo(listener.position()) < NEAR
                        && (sound.id().equals(ModSounds.HEX_CROSS.get().getLocation()) || sound.id().equals(SoundEvents.CAMPFIRE_CRACKLE.getLocation()));
            }
            for (ClientboundLevelParticlesPacket packet : TestKit.particlesIn(packets)) {
                heard[0] |= packet.getParticle().getType() == ParticleTypes.SMALL_FLAME
                        && new Vec3(packet.getX(), packet.getY(), packet.getZ()).distanceTo(listener.position()) < NEAR;
            }
            if (tick < 40) {
                return false;
            }
            check(!Telekinesis.isHolding(magneto) && !Telekinesis.isHeld(vision), "the wall made Magneto let go");
            check(inside(f, wanda, vision), "the Hex's Vision is inside the Hex");
            check("message.enhanced.telekinesis.hex_holds".equals(last[0]), "Magneto was told the Hex holds him in: " + last[0]);
            check(!heard[0], "and he did not come apart: no crack, no crackle, no flame");
            return true;
        });
    }

    /**
     * X4 (M5). A Vision player carried to the wall of a Wanda's Hex by Magneto's grip is not to blame: released inside,
     * his health unchanged, Magneto told {@code hex_holds}. Walking across himself, the wall bites him.
     */
    @GameTest(template = ARENA, batch = "v31_cross_hex", timeoutTicks = 100)
    public static void aVisionCarriedToTheHexWallIsNotBitten(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = raiseHex(f);
        ServerPlayer magneto = f.player(CENTER, FLOOR, 4.0, Power.MAGNETO);
        ServerPlayer vision = f.player(CENTER, FLOOR, 8.0, Power.VISION);
        letGoAtTheEnd(f, magneto);
        float health = vision.getHealth();

        lookAt(magneto, vision);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.holds(magneto, vision), "Magneto holds Vision inside the Hex");
        lastActionBarKey(magneto);
        String[] last = {null};
        int[] freeAt = {-1};
        f.everyTick(70, tick -> {
            if (freeAt[0] < 0) {
                TestKit.look(magneto, 180.0F, 0.0F); // out past the wall
                PowerTicker.tick(magneto);
                SokoviaTestSupport.client(vision); // his client moves him by the velocity the grip gave him
                last[0] = remember(last[0], lastActionBarKey(magneto));
                if (!Telekinesis.isHolding(magneto)) {
                    freeAt[0] = tick;
                    check(inside(f, wanda, vision), "released inside the Hex");
                    check(vision.getHealth() == health, "his health is unchanged");
                    check("message.enhanced.telekinesis.hex_holds".equals(last[0]), "Magneto was told the Hex holds him in: " + last[0]);
                    return false;
                }
                check(tick < 40, "the grip is gone within 40 ticks");
                return false;
            }
            int since = tick - freeAt[0];
            if (since == 1) {
                check(vision.getHealth() == health && inside(f, wanda, vision), "and one tick later he is still whole and inside");
                walkTo(vision, abs(helper, CENTER, FLOOR, 1.0)); // across the wall by himself
                return false;
            }
            if (since == 4) {
                check(inside(f, wanda, vision), "the wall threw him back in");
                check(vision.getHealth() < health, "walking across himself, he is bitten: " + vision.getHealth());
                return true;
            }
            return false;
        });
    }

    /**
     * X9 (M54, PvP off). The Hex's Vision of another player's Hex, thrown at the wall by an untrusted Magneto, is
     * spared the slam: his health is unchanged when the flight ends. (Wanda's Hex stands in the corner so the barrier
     * wall of the arena is inside it.)
     */
    @GameTest(template = ARENA, batch = "v31_cross_hex_friend", timeoutTicks = 80)
    public static void theHexsVisionIsSetDownSafely(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(false);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        ServerPlayer wanda = f.player(6.5, FLOOR, 6.5, Power.SCARLET_WITCH);
        AbilityHandler.onKey(wanda, 5, true);
        ServerPlayer magneto = f.player(8.5, FLOOR, 10.5, Power.MAGNETO);
        letGoAtTheEnd(f, magneto);
        HexVisionEntity vision = HexVisions.of(wanda.getUUID());
        check(vision != null, "Wanda's Hex has raised a Vision");
        vision.setNoAi(true);
        Vec3 at = abs(helper, 4.5, FLOOR, 10.5);
        vision.moveTo(at.x, at.y, at.z);
        float health = vision.getHealth();

        TestKit.look(magneto, 90.0F, 0.0F); // west, at the wall of the arena
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.holds(magneto, vision), "an untrusted Magneto holds the Hex's Vision");
        AbilityHandler.onKey(magneto, 1, true);
        check(!Telekinesis.isHolding(magneto) && Telekinesis.isSpared(vision), "he throws him, and the Vision is spared the landing");
        f.everyTick(30, tick -> {
            if (tick < 30) {
                return false;
            }
            check(vision.getX() < helper.absoluteVec(new Vec3(1.5, FLOOR, 10.5)).x, "the flight reached the wall: " + vision.getX());
            check(vision.isAlive() && vision.getHealth() == health, "and the wall did not hurt him: " + vision.getHealth() + " of " + health);
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // X3: the pull and R agree on fixed metal

    /** A block on a stone pillar, one above the floor, at (x, z). */
    private static void onPillar(GameTestHelper helper, int x, int z, Block block) {
        helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
        helper.setBlock(new BlockPos(x, 3, z), block);
    }

    /** R at the middle of the cell {@code (x, 3, z)} of the arena: the line it says. */
    private static String rAtBlock(GameTestHelper helper, ServerPlayer magneto, int x, int z) {
        lookAt(magneto, helper.absoluteVec(new Vec3(x + 0.5, 3.5, z + 0.5)));
        lastActionBarKey(magneto);
        AbilityHandler.onKey(magneto, 1, true);
        return lastActionBarKey(magneto);
    }

    /**
     * X3 (M15, M34). R at a bell nobody claimed says {@code fixed_metal} (the pull would take it whole), and the bell
     * stays; at a bell a villager claimed it says {@code fixed_metal_off} (the pull would leave it); at an empty hopper
     * {@code fixed_metal}.
     */
    @GameTest(template = ARENA, batch = "v31_cross_fixed", timeoutTicks = 30)
    public static void fixedMetalTellsTheTruthAboutThePull(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = f.player(CENTER, FLOOR, CENTER, Power.MAGNETO);
        onPillar(helper, 14, 10, Blocks.BELL); // nobody's
        onPillar(helper, 6, 10, Blocks.BELL); // a villager's
        onPillar(helper, 10, 14, Blocks.HOPPER); // empty
        f.everyTick(10, tick -> {
            if (tick < 2) {
                return false; // the points of interest are noted a moment after the blocks are set
            }
            BlockPos claimed = helper.absolutePos(new BlockPos(6, 3, 10));
            check(level.getPoiManager().take(type -> true, (type, p) -> p.equals(claimed), claimed, 1).isPresent(), "a villager claims the bell");
            check(!MagnetPull.claimedByVillager(level, helper.absolutePos(new BlockPos(14, 3, 10)), Blocks.BELL.defaultBlockState())
                    && MagnetPull.claimedByVillager(level, claimed, Blocks.BELL.defaultBlockState()), "one bell is claimed, the other is not");

            check("message.enhanced.telekinesis.fixed_metal".equals(rAtBlock(helper, magneto, 14, 10)), "an unclaimed bell: the pull would take it");
            check("message.enhanced.telekinesis.fixed_metal_off".equals(rAtBlock(helper, magneto, 6, 10)), "a villager's bell: the pull would leave it");
            check("message.enhanced.telekinesis.fixed_metal".equals(rAtBlock(helper, magneto, 10, 14)), "an empty hopper: the pull would take it");
            check(!Telekinesis.isHolding(magneto), "R holds none of them");
            for (int[] at : new int[][] {{14, 10}, {6, 10}}) {
                check(helper.getBlockState(new BlockPos(at[0], 3, at[1])).is(Blocks.BELL), "the bell at " + at[0] + " stays");
            }
            check(helper.getBlockState(new BlockPos(10, 3, 14)).is(Blocks.HOPPER), "and so does the hopper");
            return true;
        });
    }

    /** X3, the other half: with {@code magneto.pullTakesBuiltMetal} off, the hopper (and the bell) say {@code fixed_metal_off}. */
    @GameTest(template = ARENA, batch = "v31_cross_fixed_off", timeoutTicks = 30)
    public static void fixedMetalTellsTheTruthWithTheSwitchOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(EnhancedConfig.PULL_TAKES_BUILT_METAL, false);
        ServerPlayer magneto = f.player(CENTER, FLOOR, CENTER, Power.MAGNETO);
        onPillar(helper, 14, 10, Blocks.HOPPER);
        onPillar(helper, 6, 10, Blocks.BELL);
        check("message.enhanced.telekinesis.fixed_metal_off".equals(rAtBlock(helper, magneto, 14, 10)),
                "with the switch off the pull would leave the hopper");
        check("message.enhanced.telekinesis.fixed_metal_off".equals(rAtBlock(helper, magneto, 6, 10)), "and the bell");
        check(helper.getBlockState(new BlockPos(14, 3, 10)).is(Blocks.HOPPER) && helper.getBlockState(new BlockPos(6, 3, 10)).is(Blocks.BELL),
                "both stay");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // X5: Mjolnir

    /** Proves the player worthy: grants Hero of the Village. */
    private static void makeWorthy(ServerPlayer player) {
        AdvancementHolder hero = player.server.getAdvancements().get(Worthiness.HERO_OF_THE_VILLAGE);
        check(hero != null, "the Hero of the Village advancement exists");
        for (String criterion : hero.value().criteria().keySet()) {
            player.getAdvancements().award(hero, criterion);
        }
    }

    /** The hammers in the arena: item entities of Mjolnir and the blocks a dropped one comes to rest as. */
    private static int hammers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int found = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absoluteVec(Vec3.ZERO), helper.absoluteVec(new Vec3(21, 12, 21))),
                item -> item.getItem().is(ModItems.MJOLNIR.get())).size();
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(20, 6, 20)))) {
            if (level.getBlockState(pos).is(ModBlocks.MJOLNIR.get())) {
                found++;
            }
        }
        return found;
    }

    /** Takes every hammer, ingot and zombie of the round away, so the next one starts bare. */
    private static void sweep(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB arena = new AABB(helper.absoluteVec(Vec3.ZERO), helper.absoluteVec(new Vec3(21, 12, 21)));
        level.getEntitiesOfClass(ItemEntity.class, arena).forEach(ItemEntity::discard);
        level.getEntitiesOfClass(Zombie.class, arena).forEach(Zombie::discard);
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 2, 0)), helper.absolutePos(new BlockPos(20, 6, 20)))) {
            if (level.getBlockState(pos).is(ModBlocks.MJOLNIR.get())) {
                level.removeBlock(pos, false);
            }
        }
    }

    /**
     * The first half of a round of X5: Mjolnir in sight, in a zombie's hand and on the floor beside an ingot, against R
     * and V; then the pull is pressed. Returns the zombie.
     */
    private static Zombie mjolnirRound(GameTestHelper helper, ServerPlayer magneto, String who) {
        ServerLevel level = helper.getLevel();
        Vec3 hammerAt = helper.absoluteVec(new Vec3(14.5, FLOOR, 10.5));
        ItemEntity hammer = new ItemEntity(level, hammerAt.x, hammerAt.y, hammerAt.z, new ItemStack(ModItems.MJOLNIR.get()));
        hammer.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(hammer);
        lookAt(magneto, hammer);
        lastActionBarKey(magneto);
        AbilityHandler.onKey(magneto, 1, true);
        check(!Telekinesis.isHolding(magneto) && !Telekinesis.isHeld(hammer), who + ": R holds no dropped Mjolnir");
        check(Telekinesis.MJOLNIR.equals(lastActionBarKey(magneto)), who + ": and says Mjolnir answers to no magnetism");

        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(CENTER, FLOOR, 4.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // it does not burn in the sun
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MJOLNIR.get()));
        lookAt(magneto, zombie);
        AbilityHandler.onKey(magneto, 1, true);
        check(!Telekinesis.isHolding(magneto) && !Telekinesis.isHeld(zombie), who + ": R holds no zombie whose only metal is Mjolnir");
        check(Telekinesis.MJOLNIR.equals(lastActionBarKey(magneto)), who + ": the same line");
        lookAt(magneto, zombie);
        AbilityHandler.onKey(magneto, 3, true);
        check("message.enhanced.disarm.no_metal".equals(lastActionBarKey(magneto)), who + ": V finds no metal");
        check(zombie.getMainHandItem().is(ModItems.MJOLNIR.get()), who + ": and leaves Mjolnir in its hand");
        check(Powers.get(magneto).isReady(3, level.getGameTime()), who + ": the strip does not rest");

        Vec3 ingotAt = helper.absoluteVec(new Vec3(8.5, FLOOR, 12.5));
        ItemEntity ingot = new ItemEntity(level, ingotAt.x, ingotAt.y, ingotAt.z, new ItemStack(Items.IRON_INGOT));
        ingot.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(ingot);
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        press(magneto, 1, true);
        return zombie;
    }

    /** The second half of a round of X5, when the pull is over: the ingot is his, Mjolnir is where it lay. */
    private static void mjolnirAfterThePull(GameTestHelper helper, ServerPlayer magneto, Zombie zombie, String who) {
        check(count(magneto, Items.IRON_INGOT) == 1, who + ": the pull took the ingot");
        check(count(magneto, ModItems.MJOLNIR.get()) == 0, who + ": and not Mjolnir");
        check(hammers(helper) == 1, who + ": the level still holds its one Mjolnir, where it lay: " + hammers(helper));
        check(zombie.getMainHandItem().is(ModItems.MJOLNIR.get()), who + ": nor was the zombie's taken");
    }

    /**
     * X5 (M8). Mjolnir never answers to magnetism: R at a dropped one says so, R at a zombie whose only metal is
     * Mjolnir holds nothing, V leaves it in the hand, the pull takes an ingot beside it and leaves it; the same for a
     * worthy Magneto.
     */
    @GameTest(template = ARENA, batch = "v31_cross_mjolnir", timeoutTicks = 200)
    public static void mjolnirNeverAnswersToMagnetism(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.onClose(MagnetPull::clear);
        ServerPlayer magneto = f.player(CENTER, FLOOR, CENTER, Power.MAGNETO);
        check(!Worthiness.isWorthy(magneto), "he starts unworthy");
        Zombie[] zombie = {mjolnirRound(helper, magneto, "unworthy")};
        boolean[] worthy = {false};
        f.everyTick(150, tick -> {
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            if (!worthy[0]) {
                mjolnirAfterThePull(helper, magneto, zombie[0], "unworthy");
                sweep(helper);
                magneto.getInventory().clearContent();
                makeWorthy(magneto);
                rested(magneto);
                check(Worthiness.isWorthy(magneto), "a Hero of the Village is worthy");
                zombie[0] = mjolnirRound(helper, magneto, "worthy");
                worthy[0] = true;
                return false;
            }
            mjolnirAfterThePull(helper, magneto, zombie[0], "worthy");
            sweep(helper);
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // X6, X13: Thanos

    /** The plates Thanos still wears. */
    private static int plates(ThanosEntity thanos) {
        return Integer.bitCount(thanos.plates());
    }

    /**
     * X6 (M37). Magneto's strip and pull tear a plate off Thanos, one in six seconds at most: arriving, nothing; once
     * he has arrived, V tears one and rests; V again after its rest, within the six seconds, none; the pull (120 ticks
     * after the first) one more; R catches the sword he throws; and dying, nothing.
     */
    @GameTest(template = ARENA, batch = "v31_cross_thanos", timeoutTicks = 520)
    public static void magnetoStripsAPlateFromThanos(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer magneto = ThanosMoveSupport.fighter(f, -5.0, 0.0, Power.MAGNETO);
        ServerPlayer killer = ThanosMoveSupport.fighter(f, 5.0, 5.0, Power.NONE);
        ThanosFight fight = ThanosFight.spawnAt(f.level(), helper.absoluteVec(ThanosBossTests.CENTRE), magneto);
        ThanosEntity thanos = fight.boss();
        check(thanos != null, "the fight made him");
        PowerData data = Powers.get(magneto);
        int all = plates(thanos);
        check(all > 0, "he wears his plates");

        lookAt(magneto, thanos);
        press(magneto, 3, false);
        check(plates(thanos) == all, "arriving, V tears nothing");
        rested(magneto);

        thanos.forceArrived();
        thanos.setPassive(true);
        int[] stage = {0};
        long[] tornAt = {0};
        ThanosSwordEntity[] sword = {null};
        f.everyTick(500, tick -> {
            PowerTicker.tick(magneto);
            long now = f.level().getGameTime();
            lookAt(magneto, thanos);
            switch (stage[0]) {
                case 0 -> { // V: one plate, and the strip rests
                    press(magneto, 3, false);
                    check(plates(thanos) == all - 1, "V tears one plate: " + plates(thanos) + " of " + all);
                    check(!data.isReady(3, now), "and the strip rests");
                    tornAt[0] = now;
                    stage[0] = 1;
                }
                case 1 -> { // V again as soon as it is ready: within the six seconds, nothing
                    if (!data.isReady(3, now)) {
                        return false;
                    }
                    check(now - tornAt[0] < 120, "still within the six seconds");
                    press(magneto, 3, false);
                    check(plates(thanos) == all - 1, "V again within six seconds tears none: " + plates(thanos));
                    stage[0] = 2;
                }
                case 2 -> { // the pull, 120 ticks after the first
                    if (now - tornAt[0] < 120) {
                        return false;
                    }
                    press(magneto, 1, true);
                    stage[0] = 3;
                }
                case 3 -> {
                    if (MagnetPull.isPulling(magneto)) {
                        return false;
                    }
                    check(plates(thanos) == all - 2, "the pull tears one more: " + plates(thanos) + " of " + all);
                    tornAt[0] = now;
                    rested(magneto);
                    thanos.forceMove(ThanosMove.SWORD_THROW, magneto);
                    stage[0] = 4;
                }
                case 4 -> { // R catches the sword in flight
                    List<ThanosSwordEntity> swords = f.level().getEntitiesOfClass(ThanosSwordEntity.class, thanos.getBoundingBox().inflate(40.0),
                            s -> !s.isRemoved());
                    if (swords.isEmpty() || swords.get(0).position().subtract(thanos.position()).horizontalDistance() < 3.0) {
                        check(now - tornAt[0] < 100, "Thanos throws his sword");
                        return false;
                    }
                    sword[0] = swords.get(0);
                    lookAt(magneto, sword[0]);
                    press(magneto, 1, false);
                    check(Telekinesis.holds(magneto, sword[0]), "R catches the sword in flight");
                    Telekinesis.release(magneto, false);
                    stage[0] = 5;
                }
                case 5 -> { // wait out the six seconds, then he dies
                    if (now - tornAt[0] < 125 || thanos.swordAway()) {
                        return false;
                    }
                    thanos.setHealth(5.0F);
                    ThanosBossTests.hit(thanos, killer, 100.0F);
                    check(thanos.state() == ThanosState.DYING, "he is dying: " + thanos.state());
                    rested(magneto);
                    int before = plates(thanos);
                    lookAt(magneto, thanos);
                    press(magneto, 3, false);
                    check(plates(thanos) == before, "dying, he loses no plate");
                    return true;
                }
                default -> throw new IllegalStateException();
            }
            return false;
        });
    }

    /**
     * X13 (R8, R9). Thanos throws his sword at Magneto, who catches it: it is held 59 ticks, still (within a block of
     * where it was caught each tick), and at the 60th tears free ({@code sword_slips}) and flies home; Billy catches it
     * on its way and his grip holds 39 ticks and tears free at the 40th.
     */
    @GameTest(template = ARENA, batch = "v31_cross_sword", timeoutTicks = 400)
    public static void magnetoHoldsTheSwordLongerThanBilly(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer magneto = ThanosMoveSupport.fighter(f, -7.0, 0.0, Power.MAGNETO);
        ServerPlayer billy = ThanosMoveSupport.fighter(f, -7.0, 4.0, Power.BILLY);
        ThanosEntity thanos = ThanosBossTests.boss(f, magneto);
        thanos.forceMove(ThanosMove.SWORD_THROW, magneto);
        int[] caughtAt = {-1};
        int[] billyAt = {-1};
        ThanosSwordEntity[] held = {null};
        Vec3[] grip = {null};
        f.everyTick(380, tick -> {
            PowerTicker.tick(magneto);
            PowerTicker.tick(billy);
            if (caughtAt[0] < 0) {
                List<ThanosSwordEntity> swords = f.level().getEntitiesOfClass(ThanosSwordEntity.class, thanos.getBoundingBox().inflate(40.0),
                        s -> !s.isRemoved());
                check(tick < 80, "Thanos throws his sword");
                if (swords.isEmpty() || swords.get(0).position().subtract(thanos.position()).horizontalDistance() < 3.0) {
                    return false;
                }
                held[0] = swords.get(0);
                lookAt(magneto, held[0]);
                press(magneto, 1, false);
                check(Telekinesis.holds(magneto, held[0]) && !held[0].isReturning(), "Magneto catches the sword in flight");
                grip[0] = held[0].position();
                caughtAt[0] = tick;
                return false;
            }
            ThanosSwordEntity sword = held[0];
            if (billyAt[0] < 0) {
                int since = tick - caughtAt[0];
                if (since < 60) {
                    check(Telekinesis.holds(magneto, sword) && !sword.isReturning(), "Magneto still holds it, still, at " + since);
                    check(sword.position().distanceTo(grip[0]) < 1.0, "within a block of the grip point at " + since + ": "
                            + sword.position().distanceTo(grip[0]));
                    return false;
                }
                check(!Telekinesis.isHolding(magneto), "at 60 ticks the sword tears free of Magneto");
                check(ThanosSwordEntity.SLIPS.equals(lastActionBarKey(magneto)), "and Magneto reads sword_slips");
                check(sword.isReturning(), "it flies home");
                lookAt(billy, sword);
                press(billy, 3, false);
                check(Telekinesis.holds(billy, sword) && !sword.isReturning(), "Billy catches it on its way home");
                billyAt[0] = tick;
                return false;
            }
            int since = tick - billyAt[0];
            if (since < 40) {
                check(Telekinesis.holds(billy, sword) && !sword.isReturning(), "Billy still holds it at " + since);
                return false;
            }
            check(!Telekinesis.isHolding(billy), "at 40 ticks it tears free of Billy");
            check(ThanosSwordEntity.SLIPS.equals(lastActionBarKey(billy)), "and Billy reads sword_slips");
            check(sword.isReturning(), "and flies home again");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // X7, X8: Ultron

    /** X7 (M40). V at Ultron's body on the ground says it is all metal ({@code disarm.metal_body}) and starts no rest. */
    @GameTest(template = ARENA, batch = "v31_cross_ultron_strip", timeoutTicks = 40)
    public static void ultronsBodyHasNothingToStrip(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        ServerPlayer magneto = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.MAGNETO);
        UltronTestSupport.tank(magneto);
        UltronFight fight = UltronTestSupport.onTheGround(f, magneto);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, magneto, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 9.5, FLOOR, 10.5);

        float health = body.getHealth();
        lookAt(magneto, body);
        lastActionBarKey(magneto);
        press(magneto, 3, false);
        check("message.enhanced.disarm.metal_body".equals(lastActionBarKey(magneto)), "V says the metal is the body");
        check(Powers.get(magneto).isReady(3, f.level().getGameTime()), "and the strip does not rest");
        check(body.isAlive() && body.getHealth() == health, "and he is not hurt");
        helper.succeed();
    }

    /**
     * X8 (M51, default config). Magneto alone keeps Ultron acting two thirds of the time: whenever nobody holds him and
     * the press would not be refused, Magneto takes hold of him, and throws him at the edge of the arena on the last
     * tick before he would break free. Over 1,400 ticks (four whole cycles of 350) his brain runs at least 934 times,
     * he is grabbed at least 4 times, and never within {@code boss.regripTicks} of the end of the last throw's still.
     */
    @GameTest(template = ARENA, batch = "v31_cross_duty", timeoutTicks = 1500)
    public static void magnetoAloneKeepsUltronActingTwoThirds(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer magneto = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.MAGNETO);
        UltronTestSupport.tank(magneto);
        UltronFight fight = UltronTestSupport.onTheGround(f, magneto);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, magneto, 10.5, FLOOR, 3.5);
        long ticksBefore = body.brain().ticked();
        int[] grabbedAt = {0};
        int[] grabs = {0};
        boolean[] holding = {false};
        long[] stillEnded = {Long.MIN_VALUE};
        boolean[] wasStill = {false};
        Vec3 centre = fight.arena().center();
        f.everyTick(1400, tick -> {
            PowerTicker.tick(magneto);
            long now = f.level().getGameTime();
            boolean still = BossGrip.stills(body);
            if (wasStill[0] && !still) {
                stillEnded[0] = now;
            }
            wasStill[0] = still;
            if (holding[0] && Telekinesis.holds(magneto, body)) {
                if (tick - grabbedAt[0] == BossGrip.holdLimit(body, Mode.MAGNETO) - 1) {
                    // The last tick before he would break free: thrown at the edge of the arena, away from the middle.
                    Vec3 away = body.position().subtract(centre).multiply(1.0, 0.0, 1.0);
                    away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
                    lookAt(magneto, body.position().add(away.scale(20.0)).add(0.0, 1.0, 0.0));
                    press(magneto, 1, false);
                    check(!Telekinesis.isHolding(magneto), "Magneto throws him at tick " + tick);
                    holding[0] = false;
                }
            } else if (!Telekinesis.isHeld(body)) {
                holding[0] = false;
                if (Telekinesis.refusal(magneto, body, Mode.MAGNETO) == null && Telekinesis.canLift(magneto, body, Mode.MAGNETO)) {
                    lookAt(magneto, body);
                    press(magneto, 1, false);
                    check(Telekinesis.holds(magneto, body), "a press that is not refused takes hold of him (tick " + tick + ")");
                    long since = stillEnded[0] == Long.MIN_VALUE || still ? -1 : now - stillEnded[0];
                    check(grabs[0] == 0 || since >= BossConfig.REGRIP_TICKS.get(),
                            "no grab comes while he is still, nor within the regrip time of the last still's end: " + since);
                    grabs[0]++;
                    grabbedAt[0] = tick;
                    holding[0] = true;
                }
            }
            if (tick < 1400) {
                return false;
            }
            long brainTicks = body.brain().ticked() - ticksBefore;
            check(brainTicks >= 934, "his brain ran at least two thirds of the time: " + brainTicks + " of 1400");
            check(grabs[0] >= 4, "and he was grabbed at least 4 times: " + grabs[0]);
            return true;
        });
    }

    private V31CrossTests() {
    }
}

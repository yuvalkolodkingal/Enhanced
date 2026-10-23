package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BlockLifting;
import com.kolodkingal.enhanced.ability.MagnetPull;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.Telekinesis.Mode;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.block.MjolnirBlockEntity;
import com.kolodkingal.enhanced.boss.ArenaJournalWriter;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.build.BillyMagic;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildRules.Left;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.hex.HexVisions;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosFight;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.boxSpell;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.HexTests.abs;
import static com.kolodkingal.enhanced.gametest.HexTests.inside;
import static com.kolodkingal.enhanced.gametest.HexTests.raiseHex;
import static com.kolodkingal.enhanced.gametest.HexTests.walkTo;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The tests of 3.1 that cross two lanes (spec 18.6, step I): Magneto and Vision, Magneto and the Hex, what the Magnetic
 * Pull and R tell each other about fixed metal, Mjolnir, the three villains, the Hex's Vision set down safely (X1 to X9,
 * X13), and Build Magic against the rest: a spell and the pull on every arena, the Sokovia island, an undo after the pull,
 * pinned creatures, Billy's blocks in build mode, Mjolnir lying as a block, a held block, and a Move that meets the pull
 * (X10 to X12, X14 to X18). Each is named in the register and has the batch the register gives it ({@code v31_cross_*});
 * a test that changes a config value or PvP has a batch of its own, as in {@code docs/HEROES-AND-BOSSES.md} section 5.
 */
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_vision", timeoutTicks = 40)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_hex", timeoutTicks = 60)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_hex", timeoutTicks = 100)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_hex_friend", timeoutTicks = 80)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_fixed", timeoutTicks = 30)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_fixed_off", timeoutTicks = 30)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_mjolnir", timeoutTicks = 200)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_thanos", timeoutTicks = 520)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_sword", timeoutTicks = 400)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_ultron_strip", timeoutTicks = 40)
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
    @EnhancedTest(template = ARENA, batch = "v31_cross_duty", timeoutTicks = 1500)
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

    // ------------------------------------------------------------------------------------------------------------
    // Build Magic across the other lanes (X10 to X12, X14 to X18)

    /** Sneak + R with nothing held, pressed and let go, as his client sends it: the Magnetic Pull. */
    private static void pull(ServerPlayer magneto) {
        press(magneto, 1, true);
    }

    /** A test fight round the middle of the floor (radius 8), protecting its blocks or not, started by {@code starter}. */
    private static void startFight(TestKit.Fixture f, ServerPlayer starter, BossKind kind, boolean protectBlocks) {
        BossArena arena = new BossTestSupport.TestArena(f.level(), f.helper().absoluteVec(new Vec3(CENTER, FLOOR, CENTER)), 8.0,
                BossArena.Containment.NONE, protectBlocks).containAt(6.0).joinWithin(9.0).tractorDepth(0.0);
        BossTestSupport.TestFight fight = new BossTestSupport.TestFight(f.level(), kind, arena);
        BossFights.register(kind, new BossTestSupport.TestLane(kind, player -> fight));
        check(BossFights.start(kind, starter, false), "the test fight starts");
    }

    /** The block the player holds now, or null. */
    private static TelekineticBlockEntity heldBlock(GameTestHelper helper, ServerPlayer player) {
        int held = Powers.get(player).heldEntityId();
        for (TelekineticBlockEntity block : helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get())) {
            if (block.getId() == held && Telekinesis.isHolding(player)) {
                return block;
            }
        }
        return null;
    }

    /** The cell of the floor's layer (relative y 2) under a floating block, as a relative position. */
    private static BlockPos cellUnder(GameTestHelper helper, TelekineticBlockEntity block) {
        Vec3 at = helper.relativeVec(block.position());
        return new BlockPos(Mth.floor(at.x), 2, Mth.floor(at.z));
    }

    /** Looks at the middle of a cell of the test (relative position) and takes the block there, as the hero's V does. */
    private static TelekineticBlockEntity tearOut(GameTestHelper helper, ServerPlayer hero, BlockPos relative) {
        lookAt(hero, Vec3.atCenterOf(helper.absolutePos(relative)));
        check(BlockLifting.tryLift(hero, Mode.WANDA), "a block is in reach at " + relative);
        return heldBlock(helper, hero);
    }

    /** Thor with Mjolnir in his hand, ready to set it down on someone. */
    private static ServerPlayer thor(TestKit.Fixture f, double x, double z) {
        ServerPlayer thor = f.player(x, FLOOR, z, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(thor);
        return thor;
    }

    /** The states of the cells of a box of the test (relative), in order. */
    private static List<BlockState> states(GameTestHelper helper, BoundingBox box) {
        List<BlockState> states = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            states.add(helper.getBlockState(pos));
        }
        return states;
    }

    /** How many blocks of {@code block} stand in the arena (relative 0, 1, 0 to 20, 12, 20). */
    private static int blocksInTheArena(TestKit.Fixture f, Block block) {
        return BuildTestSupport.count(f, new BoundingBox(0, 1, 0, 20, 12, 20), block);
    }

    /** Every item entity of the arena. */
    private static int itemsInTheArena(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM).size();
    }

    // ------------------------------------------------------------------------------------------------------------
    // X10: a spell and the pull agree on every arena

    /**
     * X10 (R5, default config). An unprotected test fight (a Thanos-like arena, which protects no block itself): the
     * Scarlet Witch's fill across its edge changes only the cells outside it, and Magneto's pull leaves the ore inside it
     * and takes the one outside (he reads {@code pull.done_protected}). Both go by {@code build_magic.allArenasProtected}.
     */
    @EnhancedTest(template = ARENA, batch = "v31_cross_arena", timeoutTicks = 100)
    public static void aSpellAndThePullAgreeOnEveryArena(GameTestHelper helper) {
        arenaAgreement(helper, true);
    }

    /**
     * X10 with {@code build_magic.allArenasProtected} off: the same fight's arena is open to both, so the fill changes the
     * cells inside it too and the pull takes both ores.
     */
    @EnhancedTest(template = ARENA, batch = "v31_cross_arena_open", timeoutTicks = 100)
    public static void aSpellAndThePullAgreeWhenArenasAreOpen(GameTestHelper helper) {
        arenaAgreement(helper, false);
    }

    private static void arenaAgreement(GameTestHelper helper, boolean closed) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.onClose(Telekinesis::clear);
        if (!closed) {
            f.config(BuildConfig.ALL_ARENAS_PROTECTED, false);
        }
        ServerLevel level = f.level();
        ServerPlayer starter = f.player(CENTER, FLOOR, 12.5);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, Blocks.STONE);
        ServerPlayer magneto = f.player(16.5, FLOOR, CENTER, Power.MAGNETO);
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        startFight(f, starter, BossKind.THANOS, false);
        BlockPos inside = new BlockPos(13, 1, 10);
        BlockPos outside = new BlockPos(19, 1, 10);
        helper.setBlock(inside, Blocks.IRON_ORE);
        helper.setBlock(outside, Blocks.IRON_ORE);
        check(BossFights.inAnyArena(level, helper.absolutePos(inside)) && !BossFights.inAnyArena(level, helper.absolutePos(outside)),
                "the arena holds one ore and not the other");

        // The spell: a row of cells across the arena's edge.
        BoundingBox row = new BoundingBox(0, 2, 0, 20, 2, 4);
        cast(wanda, boxSpell(f, Spell.FILL, row));
        finish(wanda);
        BoundingBox world = BuildTestSupport.abs(f, row);
        int inArena = 0;
        for (BlockPos pos : BlockPos.betweenClosed(world.minX(), world.minY(), world.minZ(), world.maxX(), world.maxY(), world.maxZ())) {
            boolean in = BossFights.inAnyArena(level, pos);
            inArena += in ? 1 : 0;
            check(level.getBlockState(pos).isAir() == (in && closed), (closed ? "a cell of the arena is left and one outside is filled: "
                    : "every cell is filled, the arena's too: ") + pos);
        }
        check(inArena > 0, "the row does cross the arena's edge: " + inArena);

        // The pull: the ores.
        pull(magneto);
        f.everyTick(40, tick -> {
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(helper.getBlockState(outside).is(Blocks.STONE), "the ore outside the arena comes");
            if (closed) {
                check(helper.getBlockState(inside).is(Blocks.IRON_ORE), "the ore inside the arena stays");
                check(count(magneto, Items.RAW_IRON) == 1, "one raw iron is his: " + count(magneto, Items.RAW_IRON));
                check("message.enhanced.pull.done_protected".equals(lastActionBarKey(magneto)), "he reads that the rest is protected");
            } else {
                check(helper.getBlockState(inside).is(Blocks.STONE), "the ore inside the open arena comes too");
                check(count(magneto, Items.RAW_IRON) == 2, "two raw iron are his: " + count(magneto, Items.RAW_IRON));
            }
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // X11: the Sokovia island against every feature (5.4)

    /**
     * X11 (spec 5.4, R48). With Ultron's island up: the Scarlet Witch's fill over the island and over the ground hidden
     * under it leaves every cell (counted protected); Magneto's pull, on the ground under it and on the island, takes
     * nothing; Wanda borrows an island block and it mends. When the island has landed it is air, the ground is as before
     * the fight, nothing lies anywhere, and both journals (the arena's, the island's note, which says it landed and owes
     * nothing) are still on disk, and gone after two saves of the world.
     */
    @EnhancedTest(template = SokoviaTestSupport.PAD, batch = "v31_cross_island", timeoutTicks = 900)
    public static void theIslandIsSafeFromEveryFeature(GameTestHelper helper) {
        TestKit.Fixture f = SokoviaTestSupport.pad(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(BossConfig.ARENA_MEND_TICKS, 20);
        f.onClose(Telekinesis::clear);
        BuildTestSupport.steadyClock(); // (the cast's 2 ms preview is wall-clock time: a busy server must not cut it short)
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        BlockPos ore = new BlockPos(24, SokoviaTestSupport.SURFACE - 1, 21);
        BlockPos surface = new BlockPos(20, SokoviaTestSupport.SURFACE, 22);
        BlockPos ring = new BlockPos(38, SokoviaTestSupport.SURFACE, 21);
        BlockPos borrowedHome = surface.above(SokoviaTestSupport.LIFT);
        BlockPos oreCopy = ore.above(SokoviaTestSupport.LIFT);
        helper.setBlock(ore, Blocks.IRON_ORE);
        long ground = SokoviaTestSupport.groundHash(helper);
        ServerPlayer wanda = f.player(SokoviaTestSupport.MIDDLE_X, SokoviaTestSupport.GROUND, SokoviaTestSupport.MIDDLE_Z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        ServerPlayer magneto = f.player(SokoviaTestSupport.MIDDLE_X + 3.0, SokoviaTestSupport.GROUND, SokoviaTestSupport.MIDDLE_Z, Power.MAGNETO);
        UltronTestSupport.tank(magneto);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronTestSupport.put(f, wanda, SokoviaTestSupport.MIDDLE_X - 3.0, SokoviaTestSupport.GROUND, SokoviaTestSupport.MIDDLE_Z);
        BoundingBox skyBox = new BoundingBox(19, SokoviaTestSupport.SURFACE + SokoviaTestSupport.LIFT - 2, 19, 25,
                SokoviaTestSupport.SURFACE + SokoviaTestSupport.LIFT + 1, 25);
        BoundingBox groundBox = new BoundingBox(19, SokoviaTestSupport.SURFACE - 2, 19, 25, SokoviaTestSupport.SURFACE + 1, 25);
        List<BlockState> sky = new ArrayList<>();
        List<BlockState> hidden = new ArrayList<>();
        BlockState[] wasHome = new BlockState[1];
        BlockState[] wasRing = new BlockState[1];
        TelekineticBlockEntity[] block = new TelekineticBlockEntity[1];
        TelekineticBlockEntity[] ringBlock = new TelekineticBlockEntity[1];
        Vec3[] up = new Vec3[1];
        Vec3[] wandaUp = new Vec3[1];

        SokoviaTestSupport.Script script = SokoviaTestSupport.toTheIsland(fight, wanda, magneto);
        script.then("Wanda fills over the island and over the hidden ground", () -> {
            UltronTestSupport.calm(fight);
            fight.setNextDebrisAt(Long.MAX_VALUE / 2);
            wanda.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
            BuildMagic.setMode(wanda, true);
            BuildScheduler.manual(wanda.getUUID(), null);
            check(BuildTestSupport.building(wanda), "Wanda is in build mode on the island");
            check(helper.getBlockState(oreCopy).is(Blocks.IRON_ORE) && fight.island().isIslandUp(), "the island is up, with the ore's copy");
            check(BossFights.hidesGround(level, BuildTestSupport.abs(f, ore.getX(), ore.getY(), ore.getZ())), "(the ore's ground is hidden)");
            sky.addAll(states(helper, skyBox));
            hidden.addAll(states(helper, groundBox));
            check(sky.stream().anyMatch(state -> !state.isAir()) && hidden.stream().anyMatch(state -> !state.isAir()),
                    "the boxes hold island and ground");
            for (BoundingBox box : List.of(skyBox, groundBox)) {
                BuildTestSupport.Seen.of(wanda);
                cast(wanda, boxSpell(f, Spell.FILL, box));
                BuildTestSupport.Seen seen = BuildTestSupport.Seen.of(wanda);
                // Every cell is protected (the arena): nothing can change, so the fill fizzles before it is cast, and says how many.
                check(working(wanda) == null && seen.code() == BuildVerdictPayload.NOTHING_CAN_CHANGE
                        && seen.said("message.enhanced.build.left.protected"), "the fill is refused, the cells counted protected: " + seen);
            }
            check(states(helper, skyBox).equals(sky), "every cell over the island is as it was");
            check(states(helper, groundBox).equals(hidden), "and every cell of the hidden ground");
            check(BuildTestSupport.count(f, skyBox, Blocks.STONE) == sky.stream().filter(state -> state.is(Blocks.STONE)).count()
                    && itemsInTheArena(helper) == 0, "no stone was added, no item lies about");
        });
        script.then("Magneto pulls on the ground under the island", () -> {
            up[0] = SokoviaTestSupport.rel(helper, magneto);
            UltronTestSupport.put(f, magneto, SokoviaTestSupport.MIDDLE_X - 2.0, SokoviaTestSupport.GROUND, SokoviaTestSupport.MIDDLE_Z);
            lastActionBarKey(magneto);
            pull(magneto);
        });
        script.step("the pull ends", () -> {
            PowerTicker.tick(magneto);
            return !MagnetPull.isPulling(magneto);
        });
        script.then("it took nothing", () -> {
            check(helper.getBlockState(ore).is(Blocks.IRON_ORE) && count(magneto, Items.RAW_IRON) == 0, "the hidden ore stays, no raw iron is his");
            check("message.enhanced.pull.protected".equals(lastActionBarKey(magneto)), "he reads that it is protected");
            UltronTestSupport.put(f, magneto, up[0].x, up[0].y, up[0].z);
            rested(magneto);
            pull(magneto);
        });
        script.step("the second pull ends", () -> {
            PowerTicker.tick(magneto);
            return !MagnetPull.isPulling(magneto);
        });
        script.then("it took nothing from the island", () -> {
            check(helper.getBlockState(oreCopy).is(Blocks.IRON_ORE) && helper.getBlockState(ore).is(Blocks.IRON_ORE)
                    && count(magneto, Items.RAW_IRON) == 0, "the island's copy stays, so does the ore below, no raw iron is his");
            wasHome[0] = helper.getBlockState(borrowedHome);
            block[0] = tearOut(helper, wanda, borrowedHome);
            check(block[0] != null && block[0].isBorrowed(), "Wanda borrows an island block");
            check(helper.getBlockState(borrowedHome).isAir(), "its cell is open");
            TestKit.look(wanda, 0.0F, 70.0F);
            press(wanda, 3, false);
            check(!Telekinesis.isHolding(wanda), "she throws it");
        });
        script.step("it turns to dust and the home mends", () -> !block[0].isAlive() && helper.getBlockState(borrowedHome) == wasHome[0]);
        script.then("Wanda borrows a block of the ring of ground round the island", () -> {
            // (The island's own copies are never journaled; the ring is real ground, so the arena's journal has this one.)
            wandaUp[0] = SokoviaTestSupport.rel(helper, wanda);
            UltronTestSupport.put(f, wanda, SokoviaTestSupport.MIDDLE_X + 16.0, SokoviaTestSupport.GROUND, SokoviaTestSupport.MIDDLE_Z);
            wasRing[0] = helper.getBlockState(ring);
            ringBlock[0] = tearOut(helper, wanda, ring);
            check(ringBlock[0] != null && ringBlock[0].isBorrowed(), "the ring's block is lent");
            TestKit.look(wanda, 0.0F, 70.0F);
            press(wanda, 3, false);
            check(!Telekinesis.isHolding(wanda), "she throws it");
        });
        script.step("it turns to dust and the ring mends", () -> !ringBlock[0].isAlive() && helper.getBlockState(ring) == wasRing[0]);
        script.then("the fight ends", () -> {
            UltronTestSupport.put(f, wanda, wandaUp[0].x, wandaUp[0].y, wandaUp[0].z);
            check(itemsInTheArena(helper) == 0, "the borrowed blocks dropped nothing");
            fight.end(BossEnd.COMMAND);
        });
        script.step("the island comes down", () -> {
            SokoviaTestSupport.client(wanda, magneto);
            return fight.isFinished();
        });
        script.then("everything is as it was, and both journals wait for the saves", () -> {
            check(SokoviaTestSupport.groundHash(helper) == ground, "the ground is as it was before the fight");
            check(SokoviaTestSupport.blocksInTheSky(helper) == 0, "the island is air: " + SokoviaTestSupport.blocksInTheSky(helper));
            check(itemsInTheArena(helper) == 0 && helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).isEmpty(),
                    "nothing is borrowed or dropped anywhere");
            ArenaJournalWriter.flush();
            check(Files.isRegularFile(ArenaJournalWriter.file(server, fight.id())), "the arena's journal is still on disk");
            SokoviaJournal.Header note = SokoviaJournal.read(server, fight.id());
            check(note != null && SokoviaJournal.LANDED.equals(note.state()) && !note.refund() && SokoviaJournal.isRetiring(fight.id()),
                    "the island's note is kept, says it landed and owes nothing: " + note);
            NeoForge.EVENT_BUS.post(new LevelEvent.Save(level));
            NeoForge.EVENT_BUS.post(new LevelEvent.Save(level));
            ArenaJournalWriter.flush();
            check(!Files.exists(ArenaJournalWriter.file(server, fight.id())), "two saves later the arena's journal is gone");
            check(SokoviaJournal.read(server, fight.id()) == null && !Files.exists(SokoviaJournal.retiringFile(server, fight.id())),
                    "and so is the island's note");
        });
        script.run(f, 880);
    }

    // ------------------------------------------------------------------------------------------------------------
    // X12: an undo never restores what the pull took

    /**
     * X12 (D-B 12.3, D-M 4). The Scarlet Witch fills 9 iron blocks; Magneto, above them, pulls them into his inventory;
     * her undo restores none of the cells and says {@code kept_changed} 9; the iron blocks in the world and in his
     * inventory are 9, never more.
     */
    @EnhancedTest(template = ARENA, batch = "v31_cross_undo", timeoutTicks = 100)
    public static void anUndoNeverRestoresWhatThePullTook(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, Blocks.IRON_BLOCK);
        // Magneto on a tall pillar, looking down at the layer: each block sees the next one past (a flat layer hides itself).
        for (int y = 2; y <= 6; y++) {
            helper.setBlock(new BlockPos(9, y, 10), Blocks.STONE);
        }
        ServerPlayer magneto = f.player(9.5, 7.0, 10.5, Power.MAGNETO);
        BoundingBox layer = new BoundingBox(11, 2, 9, 13, 2, 11);
        cast(wanda, boxSpell(f, Spell.FILL, layer));
        finish(wanda);
        check(BuildTestSupport.count(f, layer, Blocks.IRON_BLOCK) == 9, "nine iron blocks are filled");
        lookAt(magneto, helper.absoluteVec(new Vec3(12.5, 2.5, 10.5)));
        for (BlockPos pos : BlockPos.betweenClosed(BuildTestSupport.abs(f, 11, 2, 9), BuildTestSupport.abs(f, 13, 2, 11))) {
            check(Raycasts.inSight(magneto, pos), "Magneto sees " + pos);
        }

        pull(magneto);
        f.everyTick(60, tick -> {
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(BuildTestSupport.count(f, layer, Blocks.IRON_BLOCK) == 0 && count(magneto, Items.IRON_BLOCK) == 9,
                    "the pull took all nine whole: " + count(magneto, Items.IRON_BLOCK));
            BuildTestSupport.Seen.of(wanda);
            BuildMagic.undo(wanda);
            finish(wanda);
            BuildTestSupport.Seen seen = BuildTestSupport.Seen.of(wanda);
            check(seen.said("message.enhanced.build.kept_changed") && seen.arg("message.enhanced.build.kept_changed", 0).equals("9"),
                    "her undo says nine blocks changed since: " + seen);
            check(BuildTestSupport.count(f, layer, Blocks.IRON_BLOCK) == 0, "it restored none of the cells");
            int inWorld = blocksInTheArena(f, Blocks.IRON_BLOCK);
            check(inWorld + count(magneto, Items.IRON_BLOCK) == 9 && itemsInTheArena(helper) == 0,
                    "iron blocks in the world plus his inventory are 9, never more: " + inWorld + " + " + count(magneto, Items.IRON_BLOCK));
            check(level.getBlockState(BuildTestSupport.abs(f, 12, 2, 10)).isAir(), "(the middle cell is air)");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // X14: a creature Mjolnir pins, against every power

    /**
     * X14 (R7). A pig Mjolnir pins stands inside the Scarlet Witch's fill: its cell stays air, it does not move, and it
     * is counted among the bodies; a Move of the box leaves it where it is, pinned. Magneto's pull never tugs a pinned
     * golem (one that is not pinned slides toward him), and Wanda's V at it says Mjolnir holds it down.
     */
    @EnhancedTest(template = ARENA, batch = "v31_cross_pinned", timeoutTicks = 100)
    public static void pinnedCreaturesStayPutForEveryPower(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, Blocks.STONE);
        ServerPlayer thorOfThePig = thor(f, 19.5, 19.5);
        ServerPlayer thorOfTheGolem = thor(f, 19.5, 1.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(14.5, FLOOR, 14.5));
        IronGolem pinnedGolem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(5.5, FLOOR, 5.5));
        IronGolem freeGolem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(5.5, FLOOR, 15.5));
        MjolnirPin.pin(thorOfThePig, InteractionHand.MAIN_HAND, pig);
        MjolnirPin.pin(thorOfTheGolem, InteractionHand.MAIN_HAND, pinnedGolem);
        check(MjolnirPin.isPinned(pig) && MjolnirPin.isPinned(pinnedGolem) && !MjolnirPin.isPinned(freeGolem), "the pig and one golem are pinned");
        Vec3 pigAt = pig.position();
        Vec3 golemAt = pinnedGolem.position();

        // The fill: its cells stay air, counted as bodies.
        BoundingBox box = new BoundingBox(12, 2, 12, 16, 4, 16);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob fill = working(wanda);
        finish(wanda);
        check(level.getBlockState(BuildTestSupport.abs(f, 14, 2, 14)).isAir(), "the pig's cell stays air");
        check(BuildTestSupport.count(f, box, Blocks.STONE) == 74, "the rest of the box is filled: " + BuildTestSupport.count(f, box, Blocks.STONE));
        check(fill.left(Left.BODIES) >= 1, "the pig is counted among the bodies: " + fill.left(Left.BODIES));
        check(pig.position().distanceTo(pigAt) < 1.0E-6 && MjolnirPin.isPinned(pig), "it did not move, and Mjolnir still holds it");

        // A Move of the whole box, eight blocks north: the pig stays.
        BoundingBox target = new BoundingBox(12, 2, 4, 16, 4, 8);
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(BuildTestSupport.abs(f, box)).at(BuildTestSupport.abs(f, 14, 1, 6), Direction.UP).build());
        finish(wanda);
        check(BuildTestSupport.count(f, target, Blocks.STONE) == 74 && BuildTestSupport.count(f, box, Blocks.STONE) == 0, "the box moved");
        check(pig.isAlive() && pig.position().distanceTo(pigAt) < 1.0E-6 && MjolnirPin.isPinned(pig), "the pig was left where it stood, pinned");

        // Wanda's V at the pinned golem.
        lookAt(wanda, pinnedGolem);
        lastActionBarKey(wanda);
        press(wanda, 3, false);
        check("message.enhanced.telekinesis.pinned".equals(lastActionBarKey(wanda)) && !Telekinesis.isHolding(wanda),
                "V at the pinned golem says Mjolnir holds it down");

        // Magneto's pull tugs the free golem, never the pinned one.
        ServerPlayer magneto = f.player(CENTER, FLOOR, CENTER, Power.MAGNETO);
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        double freeBefore = freeGolem.distanceTo(magneto);
        pull(magneto);
        f.everyTick(30, tick -> {
            PowerTicker.tick(magneto);
            if (tick < 14) {
                return false;
            }
            check(!MagnetPull.isPulling(magneto), "the pull is over");
            check(freeGolem.distanceTo(magneto) <= freeBefore - 2.0, "the free golem slid nearer: " + freeBefore + " to " + freeGolem.distanceTo(magneto));
            check(MagnetPull.lastTugs(magneto) == 1, "one body was tugged: " + MagnetPull.lastTugs(magneto));
            check(pinnedGolem.position().distanceTo(golemAt) < 1.0E-6 && MjolnirPin.isPinned(pinnedGolem), "the pinned golem did not move");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // X15: Billy in build mode

    /**
     * X15 (spec 4.2, 4.6). With build mode on, Billy's V at a block holds it and his meter is not touched; Sneak + V
     * sets it down, and it is dirt again where it lands. He is still in build mode.
     */
    @EnhancedTest(template = ARENA, batch = "v31_cross_billy", timeoutTicks = 100)
    public static void billyInBuildModeStillLiftsBlocks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = f.level();
        ServerPlayer billy = builder(f, Power.BILLY, 3.5, CENTER, null);
        check(BuildTestSupport.building(billy), "Billy is in build mode");
        BlockPos dirt = new BlockPos(11, 2, 10);
        helper.setBlock(dirt, Blocks.DIRT);
        BuildData data = ModAttachments.BUILD_DATA.get(billy);
        int meter = BillyMagic.meter(data, level.getGameTime());
        lookAt(billy, Vec3.atCenterOf(helper.absolutePos(dirt)));
        press(billy, 3, false);
        TelekineticBlockEntity block = heldBlock(helper, billy);
        check(block != null && block.getBlockState().is(Blocks.DIRT) && helper.getBlockState(dirt).isAir(), "V holds the dirt he looks at");
        check(BillyMagic.meter(data, level.getGameTime()) == meter, "his meter is unchanged: " + BillyMagic.meter(data, level.getGameTime()));
        BlockPos[] landing = {null};
        f.everyTick(80, tick -> {
            if (tick <= 15) {
                TestKit.look(billy, 0.0F, 0.0F);
                PowerTicker.tick(billy);
                if (tick == 15) {
                    landing[0] = cellUnder(helper, block);
                    press(billy, 3, true);
                    check(!Telekinesis.isHolding(billy), "Sneak + V sets it down");
                }
                return false;
            }
            if (block.isAlive()) {
                return false;
            }
            check(helper.getBlockState(landing[0]).is(Blocks.DIRT) && itemsInTheArena(helper) == 0, "it is dirt again where it landed, nothing dropped");
            check(BillyMagic.meter(data, level.getGameTime()) == meter && BuildTestSupport.building(billy),
                    "his meter is still untouched and he is still in build mode");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // X16: Mjolnir lying as a block

    /**
     * X16 (D-B 5.7, D-T, D-M 2.1). Mjolnir lying as a block: the Scarlet Witch's Vanish over it and its neighbours leaves
     * it, her Sneak + V says it is too strong; Magneto's pull (which does take an iron block beside it) and his R leave it;
     * and the level holds exactly one Mjolnir.
     */
    @EnhancedTest(template = ARENA, batch = "v31_cross_mjolnir_block", timeoutTicks = 100)
    public static void mjolnirIsUntouchedByEveryFeature(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 13.5, 4.5, null);
        ServerPlayer magneto = f.player(CENTER, FLOOR, CENTER, Power.MAGNETO);
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        BlockPos hammer = new BlockPos(13, 2, 10);
        BlockPos hammerAt = helper.absolutePos(hammer);
        for (int x = 12; x <= 14; x++) {
            helper.setBlock(new BlockPos(x, 2, 9), Blocks.STONE);
            if (x != 13) {
                helper.setBlock(new BlockPos(x, 2, 10), Blocks.STONE);
            }
        }
        check(MjolnirBlock.setDown(level, hammerAt, new ItemStack(ModItems.MJOLNIR.get())), "Mjolnir lies there");
        BlockState lying = level.getBlockState(hammerAt);
        BlockPos iron = new BlockPos(14, 3, 12);
        helper.setBlock(iron.below(), Blocks.STONE);
        helper.setBlock(iron, Blocks.IRON_BLOCK);

        // Wanda: Vanish over the row, then Sneak + V.
        cast(wanda, boxSpell(f, Spell.VANISH, new BoundingBox(12, 2, 9, 14, 2, 10)));
        finish(wanda);
        check(level.getBlockState(hammerAt) == lying && level.getBlockEntity(hammerAt) instanceof MjolnirBlockEntity, "Vanish leaves Mjolnir");
        check(BuildTestSupport.count(f, new BoundingBox(12, 2, 9, 14, 2, 10), Blocks.STONE) == 0, "(and takes the stone beside it)");
        lookAt(wanda, Vec3.atCenterOf(hammerAt));
        lastActionBarKey(wanda);
        press(wanda, 3, true);
        check("message.enhanced.telekinesis.too_strong".equals(lastActionBarKey(wanda)) && !Telekinesis.isHolding(wanda)
                && level.getBlockState(hammerAt) == lying, "Sneak + V says it is too strong, and takes nothing");

        // Magneto: R, then the pull.
        lookAt(magneto, Vec3.atCenterOf(hammerAt));
        press(magneto, 1, false);
        check(!Telekinesis.isHolding(magneto) && level.getBlockState(hammerAt) == lying, "R at Mjolnir holds nothing");
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        rested(magneto);
        pull(magneto);
        f.everyTick(40, tick -> {
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(helper.getBlockState(iron).isAir() && count(magneto, Items.IRON_BLOCK) == 1, "the pull took the iron block beside it");
            check(level.getBlockState(hammerAt) == lying && level.getBlockEntity(hammerAt) instanceof MjolnirBlockEntity,
                    "and left Mjolnir where it lies");
            int hammers = 0;
            for (BlockPos pos : BlockPos.betweenClosed(BuildTestSupport.abs(f, 0, 1, 0), BuildTestSupport.abs(f, 20, 12, 20))) {
                hammers += level.getBlockEntity(pos) instanceof MjolnirBlockEntity ? 1 : 0;
            }
            for (ItemEntity item : helper.getEntities(EntityType.ITEM)) {
                hammers += item.getItem().is(ModItems.MJOLNIR.get()) ? 1 : 0;
            }
            for (ServerPlayer player : List.of(wanda, magneto)) {
                hammers += count(player, ModItems.MJOLNIR.get());
            }
            check(hammers == 1, "the level holds exactly one Mjolnir: " + hammers);
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // X17: a held block is not a cell

    /**
     * X17 (D-B 17, D-T 2.3). Wanda holds a stone she tore out, floating inside a box; Billy's fill completes around it and
     * the stone is still held; set down where no cell is free, it shatters into its item and no stone stands in the box.
     */
    @EnhancedTest(template = ARENA, batch = "v31_cross_held", timeoutTicks = 100)
    public static void aHeldBlockIsNotACell(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = f.level();
        ServerPlayer wanda = f.player(2.5, FLOOR, CENTER, Power.SCARLET_WITCH);
        ServerPlayer billy = builder(f, Power.BILLY, 7.5, 17.5, Blocks.OAK_PLANKS);
        BlockPos stone = new BlockPos(7, 2, 10);
        helper.setBlock(stone, Blocks.STONE);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(stone)));
        press(wanda, 3, false);
        TelekineticBlockEntity held = heldBlock(helper, wanda);
        check(held != null && held.getBlockState().is(Blocks.STONE), "Wanda holds the stone she tore out");
        BoundingBox[] box = {null};
        f.everyTick(80, tick -> {
            if (box[0] == null) {
                TestKit.look(wanda, TestKit.EAST, 0.0F);
                PowerTicker.tick(wanda);
                if (tick < 10) {
                    return false;
                }
                Vec3 at = helper.relativeVec(held.position());
                int x = Mth.floor(at.x);
                int y = Mth.floor(at.y);
                int z = Mth.floor(at.z);
                box[0] = new BoundingBox(x - 1, 2, z - 1, x + 1, Math.max(4, y + 1), z + 1);
                check(box[0].minY() <= y && y <= box[0].maxY() && box[0].minX() > 2, "the stone floats inside the box: " + at);
                cast(billy, boxSpell(f, Spell.FILL, box[0]));
                finish(billy);
                int volume = box[0].getXSpan() * box[0].getYSpan() * box[0].getZSpan();
                check(BuildTestSupport.count(f, box[0], Blocks.OAK_PLANKS) == volume, "Billy's fill completed around it: "
                        + BuildTestSupport.count(f, box[0], Blocks.OAK_PLANKS) + " of " + volume);
                check(held.isAlive() && Telekinesis.holds(wanda, held), "the stone is still held");
                press(wanda, 3, true);
                check(!Telekinesis.isHolding(wanda), "Wanda sets it down");
                return false;
            }
            if (held.isAlive()) {
                return false;
            }
            int volume = box[0].getXSpan() * box[0].getYSpan() * box[0].getZSpan();
            check(BuildTestSupport.count(f, box[0], Blocks.OAK_PLANKS) == volume && BuildTestSupport.count(f, box[0], Blocks.STONE) == 0,
                    "no cell was free: the box is still planks, with no stone in it");
            AABB around = AABB.of(BuildTestSupport.abs(f, box[0])).inflate(2.0);
            int cobblestone = 0;
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, around)) {
                cobblestone += item.getItem().is(Items.COBBLESTONE) ? item.getItem().getCount() : 0;
            }
            check(cobblestone == 1 && itemsInTheArena(helper) == 1, "the stone shattered into its item: " + cobblestone);
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // X18: a Move and the pull

    /**
     * X18 (R49). The Scarlet Witch's Move of a long box holding 3 iron blocks, slowed to one cell a tick; after it read its
     * source and before its cells' turn, Magneto's pull takes the one in reach: the other two move, the pulled one's source
     * is not moved (it is counted {@code kept_changed}) and its target stays air. Iron blocks in the world plus his
     * inventory are 3, never more.
     */
    @EnhancedTest(template = ARENA, batch = "v31_cross_move_pull", timeoutTicks = 200)
    public static void aMoveAndThePullNeverMakeTwoOfOne(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        ServerPlayer magneto = f.player(0.5, FLOOR, 3.5, Power.MAGNETO);
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        BoundingBox source = new BoundingBox(2, 2, 2, 12, 3, 4);
        BlockPos near = new BlockPos(3, 3, 3);
        BlockPos farA = new BlockPos(12, 3, 2);
        BlockPos farB = new BlockPos(12, 3, 4);
        for (BlockPos pos : List.of(near, farA, farB)) {
            helper.setBlock(pos, Blocks.IRON_BLOCK);
        }
        check(Raycasts.inSight(magneto, helper.absolutePos(near)), "Magneto sees the near block");
        // Eight blocks south of where it stood: the box's middle (7, 3) goes to (7, 11).
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(BuildTestSupport.abs(f, source))
                .at(BuildTestSupport.abs(f, 7, 1, 11), Direction.UP).build());
        BuildJob job = working(wanda);
        check(job != null, "the Move is at work");
        BuildScheduler.Budget slow = BuildScheduler.Budget.of(1, 4096, 1);
        for (int step = 0; step < 300 && job.phase() != BuildJob.Phase.APPLY; step++) {
            BuildScheduler.step(List.of(wanda.getUUID()), slow);
        }
        check(job.phase() == BuildJob.Phase.APPLY && job.written() <= 2, "its source is read, the cells' turns have begun: " + job.written());
        BuildTestSupport.Seen.of(wanda);

        pull(magneto);
        f.everyTick(60, tick -> {
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(helper.getBlockState(near).isAir() && count(magneto, Items.IRON_BLOCK) == 1, "the pull took the near block before its turn");
            check(helper.getBlockState(farA).is(Blocks.IRON_BLOCK) && helper.getBlockState(farB).is(Blocks.IRON_BLOCK), "the far ones are out of reach");
            for (int step = 0; step < 400 && !job.finished(); step++) {
                BuildScheduler.step(List.of(wanda.getUUID()), slow);
            }
            check(job.finished(), "the Move ends");
            BlockPos nearTarget = near.south(8);
            check(helper.getBlockState(nearTarget).isAir() && helper.getBlockState(near).isAir(), "the pulled cell's target stays air");
            check(helper.getBlockState(farA.south(8)).is(Blocks.IRON_BLOCK) && helper.getBlockState(farB.south(8)).is(Blocks.IRON_BLOCK)
                    && helper.getBlockState(farA).isAir() && helper.getBlockState(farB).isAir(), "the other two moved");
            int inWorld = blocksInTheArena(f, Blocks.IRON_BLOCK);
            check(inWorld + count(magneto, Items.IRON_BLOCK) == 3 && itemsInTheArena(helper) == 0,
                    "iron blocks in the world plus his inventory are 3, never more: " + inWorld + " + " + count(magneto, Items.IRON_BLOCK));
            BuildTestSupport.Seen seen = BuildTestSupport.Seen.of(wanda);
            check(job.left(Left.CHANGED) == 1 && seen.arg("message.enhanced.build.kept_changed", 0).equals("1"), "kept_changed 1: " + seen);
            return true;
        });
    }

    private V31CrossTests() {
    }
}

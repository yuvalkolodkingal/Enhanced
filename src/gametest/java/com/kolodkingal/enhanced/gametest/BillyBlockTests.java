package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Billy's block telekinesis (3.1, D-T 2): V tears a block out of the ground within his own limits (16 blocks, hardness
 * 3, one block a second, a switch), Sneak + V with nothing held takes the block behind a creature (every V hero), and a
 * block set down becomes a block again only where its holder may place one (D-T 2.3); refused, it drops as an item, the
 * block itself when its loot is empty (R44).
 */
public final class BillyBlockTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Presses the telekinesis key (V), with Sneak held or not. */
    private static void pressV(ServerPlayer player, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, 3, true);
        player.setShiftKeyDown(false);
    }

    /** Looks at the middle of a block of the arena (relative position). */
    private static void lookAtBlock(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        lookAt(player, Vec3.atCenterOf(helper.absolutePos(pos)));
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

    /**
     * The cell of the floor's layer (relative y 2) under a floating block, as a relative position. (Worked out from {@link
     * GameTestHelper#relativeVec}: {@code relativePos} turns an unrotated test's x and z round.)
     */
    private static BlockPos cellUnder(GameTestHelper helper, TelekineticBlockEntity block) {
        Vec3 at = helper.relativeVec(block.position());
        return new BlockPos(Mth.floor(at.x), 2, Mth.floor(at.z));
    }

    /** How many items of the kind lie within {@code radius} blocks of the middle of a cell of the arena. */
    private static int itemsNear(GameTestHelper helper, BlockPos pos, double radius, Item item) {
        AABB box = new AABB(helper.absolutePos(pos)).inflate(radius);
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item))) {
            count += entity.getItem().getCount();
        }
        return count;
    }

    /** Every item lying in the arena. */
    private static int allItems(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM).size();
    }

    /** A zombie that stands still and does not burn in the sun (the tests run by day). */
    private static Zombie stillZombie(GameTestHelper helper, double x, double z) {
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        return zombie;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Billy tears blocks out of the ground

    /** T1: V at dirt 8 blocks away: he holds it, glowing in his blue; Sneak + V: it lands and is dirt again. */
    @EnhancedTest(template = ARENA, batch = "billy_blocks", timeoutTicks = 100)
    public static void billyTearsABlockOutAndSetsItDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        BlockPos dirt = new BlockPos(11, 2, 10);
        helper.setBlock(dirt, Blocks.DIRT);
        lookAtBlock(helper, billy, dirt);
        pressV(billy, false);

        TelekineticBlockEntity block = heldBlock(helper, billy);
        check(block != null && block.getBlockState().is(Blocks.DIRT), "Billy holds the dirt he tore out of the ground");
        check(block.getColor() == Power.BILLY.color, "it glows in his blue");
        check(helper.getBlockState(dirt).isAir(), "its cell is air");

        BlockPos[] landing = {null};
        f.everyTick(80, tick -> {
            if (tick <= 15) {
                look(billy, 0.0F, 0.0F); // he turns south: the block floats round in front of him
                PowerTicker.tick(billy);
                if (tick == 15) {
                    landing[0] = cellUnder(helper, block);
                    pressV(billy, true); // Sneak + V: set it down
                    check(!Telekinesis.isHolding(billy), "set down, he holds nothing");
                }
                return false;
            }
            if (!helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).isEmpty()) {
                return false;
            }
            check(!landing[0].equals(dirt), "it was carried away from its hole");
            check(helper.getBlockState(landing[0]).is(Blocks.DIRT), "it lands under where it floated and is dirt again");
            check(allItems(helper) == 0, "nothing was dropped");
            check(!Telekinesis.isHolding(billy), "he holds nothing");
            return true;
        });
    }

    /** T2: Billy reaches 16 blocks (here 6) and lifts up to hardness 3; Wanda's limits are her own. */
    @EnhancedTest(template = ARENA, batch = "billy_blocks_limits")
    public static void billysBlocksHaveHisOwnLimits(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(EnhancedConfig.BILLY_BLOCK_LIFT_RANGE, 6);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        BlockPos dirt = new BlockPos(11, 2, 10);
        BlockPos iron = new BlockPos(7, 2, 12);
        BlockPos deepslate = new BlockPos(7, 2, 8);
        helper.setBlock(dirt, Blocks.DIRT);
        helper.setBlock(iron, Blocks.IRON_BLOCK);
        helper.setBlock(deepslate, Blocks.DEEPSLATE);
        lastActionBarKey(billy);

        lookAtBlock(helper, billy, dirt);
        pressV(billy, false);
        check("message.enhanced.telekinesis.nothing".equals(lastActionBarKey(billy)), "dirt 8 blocks away is out of his reach");
        check(!Telekinesis.isHolding(billy) && helper.getBlockState(dirt).is(Blocks.DIRT), "and stays where it is");

        lookAtBlock(helper, billy, iron);
        pressV(billy, false);
        check("message.enhanced.telekinesis.too_strong".equals(lastActionBarKey(billy)), "an iron block (5) is too strong for him");
        check(!Telekinesis.isHolding(billy) && helper.getBlockState(iron).is(Blocks.IRON_BLOCK), "it stays in place");

        lookAtBlock(helper, billy, deepslate);
        pressV(billy, false);
        TelekineticBlockEntity block = heldBlock(helper, billy);
        check(block != null && block.getBlockState().is(Blocks.DEEPSLATE), "deepslate (3) he lifts");
        Telekinesis.release(billy, false);

        // Wanda, standing where he stood, reaches the dirt and lifts the iron.
        BossFrameworkTests.moveTo(helper, billy, 18.5, 18.5);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        lookAtBlock(helper, wanda, iron);
        pressV(wanda, false);
        TelekineticBlockEntity ironBlock = heldBlock(helper, wanda);
        check(ironBlock != null && ironBlock.getBlockState().is(Blocks.IRON_BLOCK), "Wanda lifts the iron block");
        Telekinesis.release(wanda, false);
        lookAtBlock(helper, wanda, dirt);
        pressV(wanda, false);
        TelekineticBlockEntity dirtBlock = heldBlock(helper, wanda);
        check(dirtBlock != null && dirtBlock.getBlockState().is(Blocks.DIRT), "and the dirt 8 blocks away");
        Telekinesis.release(wanda, false);
        helper.succeed();
    }

    /** T3: a second block within {@code billy.blockLiftCooldown} waits; a creature never does. */
    @EnhancedTest(template = ARENA, batch = "billy_blocks_recharge", timeoutTicks = 80)
    public static void billyNeedsAMomentBetweenBlocks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(EnhancedConfig.BILLY_BLOCK_LIFT_COOLDOWN, 20);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        BlockPos first = new BlockPos(7, 2, 10);
        BlockPos second = new BlockPos(7, 2, 14);
        helper.setBlock(first, Blocks.STONE);
        helper.setBlock(second, Blocks.STONE);
        Zombie zombie = stillZombie(helper, 7.5, 6.5);
        lookAtBlock(helper, billy, first);
        pressV(billy, false);
        check(heldBlock(helper, billy) != null, "the first block is lifted");
        long liftedAt = helper.getLevel().getGameTime();

        f.everyTick(60, tick -> {
            long since = helper.getLevel().getGameTime() - liftedAt;
            PowerTicker.tick(billy);
            if (tick < 5) {
                look(billy, EAST, -20.0F); // raise it
                return false;
            }
            if (tick == 5) {
                look(billy, EAST, -10.0F);
                pressV(billy, false); // thrown at the far wall
                check(!Telekinesis.isHolding(billy), "thrown");
                lastActionBarKey(billy);
                lookAtBlock(helper, billy, second);
                pressV(billy, false);
                check("message.enhanced.telekinesis.recharging".equals(lastActionBarKey(billy)), "a second block must wait (" + since + " ticks)");
                check(!Telekinesis.isHolding(billy) && helper.getBlockState(second).is(Blocks.STONE), "nothing is lifted");

                lookAt(billy, zombie);
                pressV(billy, false);
                check(Telekinesis.isHolding(billy) && Powers.get(billy).heldEntityId() == zombie.getId(), "a zombie he catches at once");
                Telekinesis.release(billy, false);
                return false;
            }
            if (since < 20) {
                return false;
            }
            lookAtBlock(helper, billy, second);
            pressV(billy, false);
            TelekineticBlockEntity block = heldBlock(helper, billy);
            check(block != null && helper.getBlockState(second).isAir(), "20 ticks after the first, the second block lifts");
            Telekinesis.release(billy, false);
            return true;
        });
    }

    /** T59 (R25): his recharge outlasts the crown his press puts on, and is not forgotten when the crown goes. */
    @EnhancedTest(template = ARENA, batch = "billy_blocks_long_recharge", timeoutTicks = 260)
    public static void billysRechargeOutlastsHisCrown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(EnhancedConfig.BILLY_BLOCK_LIFT_COOLDOWN, 200);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        BlockPos first = new BlockPos(7, 2, 10);
        BlockPos second = new BlockPos(7, 2, 14);
        helper.setBlock(first, Blocks.STONE);
        helper.setBlock(second, Blocks.STONE);
        lookAtBlock(helper, billy, first);
        pressV(billy, false);
        check(heldBlock(helper, billy) != null, "the first block is lifted");
        long liftedAt = helper.getLevel().getGameTime();

        f.everyTick(240, tick -> {
            long since = helper.getLevel().getGameTime() - liftedAt;
            PowerTicker.tick(billy); // his crown comes and goes as it would in a game
            if (tick < 5) {
                look(billy, EAST, -20.0F);
                return false;
            }
            if (tick == 5) {
                look(billy, EAST, -10.0F);
                pressV(billy, false);
                check(!Telekinesis.isHolding(billy), "thrown");
                return false;
            }
            if (since == 100 || since == 199) {
                check(since == 199 || !Powers.get(billy).is(Live.CROWNED), "the crown of his presses is gone by tick 100");
                lastActionBarKey(billy);
                lookAtBlock(helper, billy, second);
                pressV(billy, false);
                check("message.enhanced.telekinesis.recharging".equals(lastActionBarKey(billy)),
                        "his magic still needs a moment " + since + " ticks after the first block");
                check(!Telekinesis.isHolding(billy) && helper.getBlockState(second).is(Blocks.STONE), "nothing is lifted");
                return false;
            }
            if (since < 200) {
                return false;
            }
            lookAtBlock(helper, billy, second);
            pressV(billy, false);
            check(heldBlock(helper, billy) != null && helper.getBlockState(second).isAir(), "200 ticks after the first, it lifts");
            Telekinesis.release(billy, false);
            return true;
        });
    }

    /** T38: {@code billy.blockLifting} off: his V and Sneak + V lift no block, creatures still; Wanda's are unchanged. */
    @EnhancedTest(template = ARENA, batch = "billy_blocks_switch")
    public static void billysBlockLiftingHasASwitch(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(EnhancedConfig.BILLY_BLOCK_LIFTING, false);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        BlockPos dirt = new BlockPos(8, 2, 10);
        helper.setBlock(dirt, Blocks.DIRT);
        Zombie zombie = stillZombie(helper, 6.5, 15.5);
        lastActionBarKey(billy);

        lookAtBlock(helper, billy, dirt);
        pressV(billy, false);
        check("message.enhanced.telekinesis.nothing".equals(lastActionBarKey(billy)), "V: nothing to lift");
        check(!Telekinesis.isHolding(billy) && helper.getBlockState(dirt).is(Blocks.DIRT), "the dirt stays");
        pressV(billy, true);
        check("message.enhanced.telekinesis.nothing".equals(lastActionBarKey(billy)), "Sneak + V: nothing to lift either");
        check(!Telekinesis.isHolding(billy) && helper.getBlockState(dirt).is(Blocks.DIRT), "the dirt still stays");

        lookAt(billy, zombie);
        pressV(billy, false);
        check(Telekinesis.isHolding(billy) && Powers.get(billy).heldEntityId() == zombie.getId(), "he still catches a zombie");
        Telekinesis.release(billy, false);

        BossFrameworkTests.moveTo(helper, billy, 18.5, 3.5);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        lookAtBlock(helper, wanda, dirt);
        pressV(wanda, false);
        TelekineticBlockEntity block = heldBlock(helper, wanda);
        check(block != null && block.getBlockState().is(Blocks.DIRT) && helper.getBlockState(dirt).isAir(),
                "Wanda in the same spot tears the dirt out: the switch is Billy's");
        Telekinesis.release(wanda, false);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sneak + V: the block, please

    /**
     * T4 (D-T D2): a zombie between the hero and a stone. V takes the zombie; Sneak + V with nothing held takes the stone
     * behind it and leaves the zombie, for Wanda, Billy and the Phoenix alike. (The second press comes more than a second
     * after the zombie was let go, past the Sneak grace of R41.)
     */
    @EnhancedTest(template = ARENA, batch = "telekinesis_sneak", timeoutTicks = 60)
    public static void sneakTakesTheBlockBehindACreature(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        Power[] heroes = {Power.SCARLET_WITCH, Power.BILLY, Power.PHOENIX};
        ServerPlayer[] players = new ServerPlayer[3];
        Zombie[] zombies = new Zombie[3];
        BlockPos[] stones = new BlockPos[3];
        for (int i = 0; i < 3; i++) {
            int z = 4 + 6 * i;
            players[i] = f.player(2.5, FLOOR, z + 0.5, heroes[i]);
            zombies[i] = stillZombie(helper, 6.5, z + 0.5);
            stones[i] = new BlockPos(10, 2, z);
            helper.setBlock(stones[i], Blocks.STONE);
            lookAtBlock(helper, players[i], stones[i]);
            pressV(players[i], false);
            check(Telekinesis.isHolding(players[i]) && Powers.get(players[i]).heldEntityId() == zombies[i].getId(),
                    heroes[i] + ": V takes the zombie in front of the stone");
            Telekinesis.release(players[i], false);
        }
        helper.runAfterDelay(25, () -> {
            for (int i = 0; i < 3; i++) {
                lookAtBlock(helper, players[i], stones[i]);
                pressV(players[i], true);
                TelekineticBlockEntity block = heldBlock(helper, players[i]);
                check(block != null && block.getBlockState().is(Blocks.STONE), heroes[i] + ": Sneak + V takes the stone behind it");
                check(helper.getBlockState(stones[i]).isAir(), heroes[i] + ": the stone is out of the ground");
                check(!Telekinesis.isHeld(zombies[i]), heroes[i] + ": the zombie is not held");
                Telekinesis.release(players[i], false);
            }
            helper.succeed();
        });
    }

    /**
     * T61 (R41): for {@code magneto.telekinesisSneakGraceTicks} (20) after one of their grips ended, however it ended,
     * Sneak + V with nothing held does nothing at all: Wanda's zombie dies in her grip (the grip is lost), Billy sets his
     * down, the Phoenix throws hers. Five ticks later the press tears nothing out and says nothing; 25 ticks later it tears
     * the block out.
     */
    @EnhancedTest(template = ARENA, batch = "telekinesis_sneak_grace", timeoutTicks = 60)
    public static void noBlockRightAfterAGrip(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        Power[] heroes = {Power.SCARLET_WITCH, Power.BILLY, Power.PHOENIX};
        ServerPlayer[] players = new ServerPlayer[3];
        Zombie[] zombies = new Zombie[3];
        BlockPos[] stones = new BlockPos[3];
        for (int i = 0; i < 3; i++) {
            int z = 4 + 6 * i;
            players[i] = f.player(2.5, FLOOR, z + 0.5, heroes[i]);
            zombies[i] = stillZombie(helper, 6.5, z + 0.5);
            stones[i] = new BlockPos(10, 2, z);
            helper.setBlock(stones[i], Blocks.STONE);
            lookAt(players[i], zombies[i]);
            pressV(players[i], false);
            check(Telekinesis.holds(players[i], zombies[i]), heroes[i] + " holds a zombie");
        }

        // Three ways a grip ends: lost (the zombie dies in Wanda's grip), set down (Billy), thrown (the Phoenix).
        zombies[0].kill();
        PowerTicker.tick(players[0]);
        check(!Telekinesis.isHolding(players[0]), "Wanda's grip is lost with the zombie");
        pressV(players[1], true);
        check(!Telekinesis.isHolding(players[1]), "Billy set his zombie down");
        pressV(players[2], false);
        check(!Telekinesis.isHolding(players[2]), "the Phoenix threw hers");
        zombies[2].discard(); // (it would fly on out of the arena)
        long ended = helper.getLevel().getGameTime();
        for (int i = 0; i < 3; i++) {
            check(Telekinesis.lastGripEnd(players[i]) == ended, heroes[i] + ": the end of the grip is remembered");
        }

        f.everyTick(40, tick -> {
            long since = helper.getLevel().getGameTime() - ended;
            if (since == 5) {
                for (int i = 0; i < 3; i++) {
                    lastActionBarKey(players[i]);
                    lookAtBlock(helper, players[i], stones[i]);
                    pressV(players[i], true);
                    check(helper.getBlockState(stones[i]).is(Blocks.STONE) && !Telekinesis.isHolding(players[i]),
                            heroes[i] + ": 5 ticks after the grip ended, Sneak + V tears nothing out");
                    check(lastActionBarKey(players[i]) == null, heroes[i] + ": and says nothing");
                }
            }
            if (since < 25) {
                return false;
            }
            for (int i = 0; i < 3; i++) {
                lookAtBlock(helper, players[i], stones[i]);
                pressV(players[i], true);
                TelekineticBlockEntity block = heldBlock(helper, players[i]);
                check(block != null && block.getBlockState().is(Blocks.STONE) && helper.getBlockState(stones[i]).isAir(),
                        heroes[i] + ": 25 ticks after, Sneak + V tears the stone out");
                Telekinesis.release(players[i], false);
            }
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // A thrown block

    /** T5: Billy's thrown stone hits like Wanda's (a telekinetic slam he owns) and shatters into its loot. */
    @EnhancedTest(template = ARENA, batch = "billy_blocks", timeoutTicks = 60)
    public static void billysThrownBlockHitsLikeWandas(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        BlockPos stone = new BlockPos(6, 2, 10);
        helper.setBlock(stone, Blocks.STONE);
        Zombie zombie = stillZombie(helper, 13.5, 10.5);
        lookAtBlock(helper, billy, stone);
        pressV(billy, false);
        check(heldBlock(helper, billy) != null, "Billy lifts the stone");
        float before = zombie.getHealth();

        f.everyTick(50, tick -> {
            if (tick < 6) {
                look(billy, EAST, -10.0F);
            }
            PowerTicker.tick(billy);
            if (tick == 6) {
                lookAt(billy, zombie);
                pressV(billy, false);
                check(!Telekinesis.isHolding(billy), "thrown at the zombie");
                return false;
            }
            if (tick < 6 || zombie.getHealth() >= before || !helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).isEmpty()) {
                return false;
            }
            DamageSource source = zombie.getLastDamageSource();
            check(source != null && source.is(ModDamageTypes.TELEKINETIC_SLAM) && source.getEntity() == billy,
                    "a telekinetic slam, and Billy is the attacker (" + source + ")");
            float taken = before - zombie.getHealth();
            check(taken >= 6.0F && taken <= 12.0F, "it hits like Wanda's block, took " + taken);
            Vec3 hit = helper.relativeVec(zombie.position());
            BlockPos at = new BlockPos(Mth.floor(hit.x), 2, Mth.floor(hit.z));
            check(itemsNear(helper, at, 2.5, Items.COBBLESTONE) == 1, "it shattered where it hit into its loot, one cobblestone");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Setting a block down: the placing rights (all forms)

    /**
     * T6 (D-T 2.3): a claim (a listener that cancels the holder's place events) makes the set-down block drop as its item;
     * a block torn out outside a protected arena and set down inside it does too, and the cell stays air.
     */
    @EnhancedTest(template = ARENA, batch = "telekinesis_place", timeoutTicks = 140)
    public static void aSetDownBlockRespectsClaimsAndArenas(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer claimed = f.player(3.5, FLOOR, 16.5, Power.BILLY);
        ServerPlayer billy = f.player(3.5, FLOOR, 3.5, Power.BILLY);
        Consumer<BlockEvent.EntityPlaceEvent> claim = event -> {
            if (event.getEntity() == claimed) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityPlaceEvent.class, claim);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(claim));

        BlockPos claimStone = new BlockPos(7, 2, 16);
        BlockPos outside = new BlockPos(1, 2, 1);
        helper.setBlock(claimStone, Blocks.STONE);
        helper.setBlock(outside, Blocks.STONE);
        lookAtBlock(helper, claimed, claimStone);
        pressV(claimed, false);
        TelekineticBlockEntity claimedBlock = heldBlock(helper, claimed);
        check(claimedBlock != null, "the stone is lifted");
        BlockPos[] landing = {null};
        TelekineticBlockEntity[] arenaBlock = {null};
        int[] phase = {0};
        int[] since = {0};

        f.everyTick(120, tick -> {
            PowerTicker.tick(claimed);
            PowerTicker.tick(billy);
            switch (phase[0]) {
                case 0 -> { // the claimed holder sets his stone down
                    look(claimed, EAST, 0.0F);
                    if (tick == 8) {
                        landing[0] = cellUnder(helper, claimedBlock);
                        pressV(claimed, true);
                        phase[0] = 1;
                    }
                }
                case 1 -> { // it fell: the claim refused it
                    if (claimedBlock.isAlive()) {
                        return false;
                    }
                    check(helper.getBlockState(landing[0]).isAir(), "the claim keeps the block from standing");
                    check(itemsNear(helper, landing[0], 1.5, Items.COBBLESTONE) == 1, "it drops as its item, one cobblestone");

                    // A stone torn out of the ground outside the arena-to-be, then carried into it.
                    lookAtBlock(helper, billy, outside);
                    pressV(billy, false);
                    arenaBlock[0] = heldBlock(helper, billy);
                    check(arenaBlock[0] != null, "the stone outside is lifted");
                    BossFrameworkTests.moveTo(helper, billy, 5.5, 10.5);
                    BossFrameworkTests.start(f, BossKind.ULTRON, billy, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, true));
                    check(!BossFights.protects(helper.getLevel(), helper.absolutePos(outside)), "where it was torn out is outside the arena");
                    phase[0] = 2;
                }
                case 2 -> { // carried in, then set down
                    look(billy, EAST, 0.0F);
                    if (++since[0] < 15) {
                        return false;
                    }
                    landing[0] = cellUnder(helper, arenaBlock[0]);
                    check(BossFights.protects(helper.getLevel(), helper.absolutePos(landing[0])), "it floats over the protected arena");
                    pressV(billy, true);
                    phase[0] = 3;
                }
                default -> {
                    if (arenaBlock[0].isAlive()) {
                        return false;
                    }
                    check(helper.getBlockState(landing[0]).isAir(), "a block from outside never stands in a protected arena");
                    check(itemsNear(helper, landing[0], 1.5, Items.COBBLESTONE) == 1, "it drops as its item");
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * T39 (D-T 2.3 rule 1, 5.4, R44): a stone set down onto a zombie's cell never becomes a block there (no wall round the
     * zombie): one cobblestone, the zombie unhurt; a glass block, whose loot is empty, drops as one glass block. In a
     * protected arena a borrowed block set down onto a zombie's cell crumbles: no block, no item.
     */
    @EnhancedTest(template = ARENA, batch = "telekinesis_place_occupied", timeoutTicks = 100)
    public static void aSetDownBlockNeedsAFreeCell(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        Block[] kinds = {Blocks.STONE, Blocks.GLASS};
        Item[] drops = {Items.COBBLESTONE, Items.GLASS};
        ServerPlayer[] wandas = new ServerPlayer[2];
        TelekineticBlockEntity[] blocks = new TelekineticBlockEntity[2];
        Zombie[] zombies = new Zombie[2];
        BlockPos[] landing = new BlockPos[2];
        for (int i = 0; i < 2; i++) {
            int z = 6 + 8 * i;
            wandas[i] = f.player(3.5, FLOOR, z + 0.5, Power.SCARLET_WITCH);
            BlockPos pos = new BlockPos(7, 2, z);
            helper.setBlock(pos, kinds[i]);
            lookAtBlock(helper, wandas[i], pos);
            pressV(wandas[i], false);
            blocks[i] = heldBlock(helper, wandas[i]);
            check(blocks[i] != null, "Wanda lifts the " + kinds[i].getName().getString());
        }

        TelekineticBlockEntity[] borrowed = {null};
        Zombie[] onTheCell = {null};
        BlockPos arenaCell = new BlockPos(12, 2, 10);

        f.everyTick(90, tick -> {
            if (borrowed[0] != null) { // the arena's block, set down onto a zombie
                if (borrowed[0].isAlive()) {
                    return false;
                }
                for (int y = 2; y <= 4; y++) {
                    check(!helper.getBlockState(new BlockPos(arenaCell.getX(), y, arenaCell.getZ())).is(Blocks.DIRT), "the borrowed block does not stand there");
                }
                check(allItems(helper) == 2, "it crumbled: no item (but the two of before)");
                check(onTheCell[0].getHealth() == onTheCell[0].getMaxHealth() && !onTheCell[0].isInWall(), "the zombie is unhurt");
                return true;
            }
            for (int i = 0; i < 2; i++) {
                look(wandas[i], EAST, 0.0F);
                PowerTicker.tick(wandas[i]);
            }
            if (tick < 8) {
                return false;
            }
            if (tick == 8) {
                for (int i = 0; i < 2; i++) {
                    landing[i] = cellUnder(helper, blocks[i]);
                    zombies[i] = stillZombie(helper, landing[i].getX() + 0.5, landing[i].getZ() + 0.5);
                    pressV(wandas[i], true); // set it down onto the zombie
                    check(!Telekinesis.isHolding(wandas[i]), "let go");
                }
                return false;
            }
            if (blocks[0].isAlive() || blocks[1].isAlive()) {
                return false;
            }
            for (int i = 0; i < 2; i++) {
                for (int y = 2; y <= 4; y++) {
                    BlockPos cell = new BlockPos(landing[i].getX(), y, landing[i].getZ());
                    check(!helper.getBlockState(cell).is(kinds[i]), "no " + kinds[i].getName().getString() + " in or over the zombie's cells");
                }
                check(itemsNear(helper, landing[i], 2.0, drops[i]) == 1, "one " + drops[i] + " where it landed");
                check(zombies[i].getHealth() == zombies[i].getMaxHealth() && !zombies[i].isInWall(), "the zombie is unhurt and not in a wall");
            }
            // In a protected arena: a borrowed block set down onto a zombie crumbles.
            BossFrameworkTests.start(f, BossKind.ULTRON, wandas[0], BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, true));
            BlockPos ground = new BlockPos(7, 2, 10);
            helper.setBlock(ground, Blocks.DIRT);
            lookAtBlock(helper, wandas[0], ground);
            pressV(wandas[0], false);
            borrowed[0] = heldBlock(helper, wandas[0]);
            check(borrowed[0] != null && borrowed[0].isBorrowed(), "Wanda borrows a block of the arena");
            onTheCell[0] = stillZombie(helper, arenaCell.getX() + 0.5, arenaCell.getZ() + 0.5);
            Vec3 over = helper.absoluteVec(new Vec3(arenaCell.getX() + 0.5, arenaCell.getY() + 0.01, arenaCell.getZ() + 0.5));
            borrowed[0].setPos(over.x, over.y, over.z);
            pressV(wandas[0], true); // set it down onto the zombie
            check(!Telekinesis.isHolding(wandas[0]), "let go");
            return false;
        });
    }

    /**
     * T40 (D-T 2.3 rule 4, R44): Billy logs out holding a block. Nobody's rights are assumed: over a claimed cell and an
     * unclaimed one alike the stone drops as cobblestone and never stands; a glass block drops as one glass block.
     */
    @EnhancedTest(template = ARENA, batch = "telekinesis_place_offline", timeoutTicks = 100)
    public static void aBlockOfALoggedOutHolderNeverLands(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        Block[] kinds = {Blocks.STONE, Blocks.STONE, Blocks.GLASS};
        Item[] drops = {Items.COBBLESTONE, Items.COBBLESTONE, Items.GLASS};
        ServerPlayer[] billies = new ServerPlayer[3];
        TelekineticBlockEntity[] blocks = new TelekineticBlockEntity[3];
        BlockPos[] landing = new BlockPos[3];
        BlockPos[] claimedCell = {null};
        int[] asked = {0};
        Consumer<BlockEvent.EntityPlaceEvent> claim = event -> {
            if (claimedCell[0] != null && event.getPos().equals(helper.absolutePos(claimedCell[0]))) {
                asked[0]++;
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityPlaceEvent.class, claim);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(claim));
        for (int i = 0; i < 3; i++) {
            int z = 4 + 6 * i;
            billies[i] = f.player(3.5, FLOOR, z + 0.5, Power.BILLY);
            BlockPos pos = new BlockPos(7, 2, z);
            helper.setBlock(pos, kinds[i]);
            lookAtBlock(helper, billies[i], pos);
            pressV(billies[i], false);
            blocks[i] = heldBlock(helper, billies[i]);
            check(blocks[i] != null, "Billy lifts the " + kinds[i].getName().getString());
        }

        f.everyTick(90, tick -> {
            if (tick < 8) {
                for (ServerPlayer billy : billies) {
                    look(billy, EAST, 0.0F);
                    PowerTicker.tick(billy);
                }
                return false;
            }
            if (tick == 8) {
                for (int i = 0; i < 3; i++) {
                    landing[i] = cellUnder(helper, blocks[i]);
                }
                claimedCell[0] = landing[0];
                for (ServerPlayer billy : billies) {
                    helper.getLevel().getServer().getPlayerList().remove(billy); // he disconnects, holding it
                }
                return false;
            }
            for (TelekineticBlockEntity block : blocks) {
                if (block.isAlive()) {
                    return false;
                }
            }
            for (int i = 0; i < 3; i++) {
                check(helper.getBlockState(landing[i]).isAir(), "the " + kinds[i].getName().getString() + " of a holder who left never stands");
                check(itemsNear(helper, landing[i], 1.5, drops[i]) == 1, "it drops as one " + drops[i]);
            }
            check(asked[0] == 0, "nobody's rights are asked for a holder who left");
            return true;
        });
    }

    /**
     * R44 (review fix): a set-down block that may not stand never becomes an item no player could have. Wanda sets an
     * infested stone and a glass block down onto zombies' cells: neither stands; the infested stone, which Silk Touch gives
     * as plain stone, does not drop as itself but gives what mining it gives, nothing but its silverfish; the glass, which
     * Silk Touch gives, drops as one glass block.
     */
    @EnhancedTest(template = ARENA, batch = "telekinesis_place_creative", timeoutTicks = 100)
    public static void aSetDownBlockNeverGivesACreativeOnlyItem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        Block[] kinds = {Blocks.INFESTED_STONE, Blocks.GLASS};
        ServerPlayer[] wandas = new ServerPlayer[2];
        TelekineticBlockEntity[] blocks = new TelekineticBlockEntity[2];
        BlockPos[] landing = new BlockPos[2];
        for (int i = 0; i < 2; i++) {
            int z = 6 + 8 * i;
            wandas[i] = f.player(3.5, FLOOR, z + 0.5, Power.SCARLET_WITCH);
            BlockPos pos = new BlockPos(7, 2, z);
            helper.setBlock(pos, kinds[i]);
            lookAtBlock(helper, wandas[i], pos);
            pressV(wandas[i], false);
            blocks[i] = heldBlock(helper, wandas[i]);
            check(blocks[i] != null && blocks[i].getBlockState().is(kinds[i]), "Wanda lifts the " + kinds[i].getName().getString());
        }

        f.everyTick(90, tick -> {
            for (int i = 0; i < 2; i++) {
                look(wandas[i], EAST, 0.0F);
                PowerTicker.tick(wandas[i]);
            }
            if (tick < 8) {
                return false;
            }
            if (tick == 8) {
                for (int i = 0; i < 2; i++) {
                    landing[i] = cellUnder(helper, blocks[i]);
                    stillZombie(helper, landing[i].getX() + 0.5, landing[i].getZ() + 0.5);
                    pressV(wandas[i], true); // set it down onto the zombie
                    check(!Telekinesis.isHolding(wandas[i]), "let go");
                }
                return false;
            }
            if (blocks[0].isAlive() || blocks[1].isAlive()) {
                return false;
            }
            for (int i = 0; i < 2; i++) {
                check(!helper.getBlockState(landing[i]).is(kinds[i]), "the " + kinds[i].getName().getString() + " does not stand on the zombie");
            }
            check(itemsNear(helper, landing[1], 2.0, Items.GLASS) == 1 && allItems(helper) == 1,
                    "one glass block drops, and nothing else: no infested stone");
            List<Silverfish> silverfish = helper.getEntities(EntityType.SILVERFISH);
            check(silverfish.size() == 1, "the infested stone broke as when mined: its silverfish came out (" + silverfish.size() + ")");
            silverfish.forEach(Silverfish::discard);
            return true;
        });
    }

    private BillyBlockTests() {
    }
}

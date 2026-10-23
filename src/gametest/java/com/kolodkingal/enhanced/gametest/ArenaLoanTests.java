package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BlockLifting;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.boss.ArenaJournalWriter;
import com.kolodkingal.enhanced.boss.ArenaLoans;
import com.kolodkingal.enhanced.boss.ArenaLoansRecovery;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The blocks a protected arena lends to block telekinesis (3.1, D-T 5): a borrowed block leaves a hole that mends, stands
 * a while where it is set down and crumbles, turns to dust when thrown, never drops anything, never builds a fort, and is
 * back when the fight ends; the arena asks the claims, Adventure and its switch, lends nothing a hole would undermine and
 * no more than its cap, and writes nothing that a neighbour would notice. Its journal, written off the server thread,
 * puts the ground back after a crash, and forgets a repair only two saves of the world after it (R48).
 *
 * <p>The arenas are protected {@link TestFight}s round the middle of the floor (radius 8; the field, when there is one, at
 * 6). Every test has a batch of its own: a fight is server-wide state, and most change a config value.
 */
public final class ArenaLoanTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /**
     * The ticks a test's own blocks are left to settle before anything is borrowed: a falling block placed by the test (as
     * any placing does) has a tick scheduled to see whether it falls, which a world that stood a while no longer has.
     */
    private static final int SETTLE = 5;

    /** A boss fixture (the server's announcements off) that also lets go of every grip when the test ends. */
    private static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        return f;
    }

    /** Starts a fight whose arena, round the middle of the floor, protects its ground. */
    private static TestFight arena(TestKit.Fixture f, ServerPlayer starter, Containment containment) {
        return BossFrameworkTests.start(f, BossKind.ULTRON, starter, BossFrameworkTests.fight(f, BossKind.ULTRON, containment, true));
    }

    private static BlockPos abs(GameTestHelper helper, BlockPos relative) {
        return helper.absolutePos(relative);
    }

    /** Presses the telekinesis key (V), with Sneak held or not. */
    private static void pressV(ServerPlayer player, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, 3, true);
        player.setShiftKeyDown(false);
    }

    /** Looks at the middle of a cell of the arena (relative position). */
    private static void lookAtBlock(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        lookAt(player, Vec3.atCenterOf(abs(helper, relative)));
    }

    /** The block the player holds now, or null. */
    @Nullable
    private static TelekineticBlockEntity heldBlock(GameTestHelper helper, ServerPlayer player) {
        int held = Powers.get(player).heldEntityId();
        for (TelekineticBlockEntity block : helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get())) {
            if (block.getId() == held && Telekinesis.isHolding(player)) {
                return block;
            }
        }
        return null;
    }

    /** The hero's block telekinesis at a cell of the arena: what he holds after it (null if nothing). */
    @Nullable
    private static TelekineticBlockEntity borrow(GameTestHelper helper, ServerPlayer hero, Telekinesis.Mode mode, BlockPos relative) {
        lookAtBlock(helper, hero, relative);
        check(BlockLifting.tryLift(hero, mode), "a block is in reach at " + relative);
        return heldBlock(helper, hero);
    }

    /** Wanda borrows the block at a cell: she holds it, and it is borrowed. */
    private static TelekineticBlockEntity borrowed(GameTestHelper helper, ServerPlayer hero, Telekinesis.Mode mode, BlockPos relative) {
        TelekineticBlockEntity block = borrow(helper, hero, mode, relative);
        check(block != null && block.isBorrowed(), "the block at " + relative + " is lent: " + lastActionBarKey(hero));
        return block;
    }

    /** Puts the held block just over a cell of the arena and lets go of it gently: it lands there at the next tick. */
    private static void setDown(GameTestHelper helper, ServerPlayer hero, TelekineticBlockEntity block, BlockPos relative) {
        Vec3 at = helper.absoluteVec(new Vec3(relative.getX() + 0.5, relative.getY() + 0.01, relative.getZ() + 0.5));
        block.setPos(at.x, at.y, at.z);
        Telekinesis.release(hero, true);
    }

    /** Lets go of the held block, which turns to dust (a test that needs the hole, not the block). */
    private static void dropDust(ServerPlayer hero, TelekineticBlockEntity block) {
        Telekinesis.release(hero, false);
        block.crumble();
    }

    private static int items(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM).size();
    }

    private static int lifted(GameTestHelper helper) {
        return helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).size();
    }

    /** A zombie that stands still and does not burn in the sun (the tests run by day). */
    private static Zombie stillZombie(GameTestHelper helper, double x, double z) {
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        return zombie;
    }

    /** One save of the dimension, as the game posts it once the chunks are queued to the disk. */
    private static void saveHook(ServerLevel level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Save(level));
    }

    /** The fight's journal as it is on the disk once everything handed over is written, or null if there is none. */
    @Nullable
    private static CompoundTag journal(MinecraftServer server, BossFight fight) {
        ArenaJournalWriter.flush();
        Path file = ArenaJournalWriter.file(server, fight.id());
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new GameTestAssertException("The journal " + file + " cannot be read: " + e);
        }
    }

    /** Whether a list of the journal ({@code homes} or {@code standing}) has an entry at the cell (absolute). */
    private static boolean lists(CompoundTag journal, String list, BlockPos pos) {
        return entries(journal, list, pos) > 0;
    }

    /** How many entries a list of the journal ({@code homes} or {@code standing}) has at the cell (absolute). */
    private static int entries(CompoundTag journal, String list, BlockPos pos) {
        int count = 0;
        for (Tag tag : journal.getList(list, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            if (entry.getInt("x") == pos.getX() && entry.getInt("y") == pos.getY() && entry.getInt("z") == pos.getZ()) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Borrowing, mending, standing

    /**
     * T24 (D-T 5.2 to 5.4): Wanda borrows dirt from a protected arena: the cell is air and the block she holds is borrowed
     * (the fight's), and she is told so; 20 ticks later the hole has mended though she still holds the block; thrown at a
     * zombie it hurts him and turns to dust, no item; a second one held when the fight is stopped is gone, its cell dirt.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_mend", timeoutTicks = 140)
    public static void aBlockBorrowedFromAnArenaComesBack(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 20);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos dirt = new BlockPos(8, 2, 6);
        BlockPos spare = new BlockPos(8, 2, 14);
        helper.setBlock(dirt, Blocks.DIRT);
        helper.setBlock(spare, Blocks.DIRT);
        Zombie zombie = stillZombie(helper, 15.5, 10.5);
        float health = zombie.getHealth();

        lookAtBlock(helper, wanda, dirt);
        pressV(wanda, false);
        TelekineticBlockEntity block = heldBlock(helper, wanda);
        check(block != null && block.isBorrowed() && fight.owns(block) && BossFights.isFightEntity(block), "Wanda holds a block the fight lent");
        check(helper.getBlockState(dirt).isAir() && fight.loans().isOpenHome(abs(helper, dirt)), "its cell is air, a home waiting for it");
        check(ArenaLoans.BORROWED.equals(lastActionBarKey(wanda)), "she is told it is borrowed");
        long lent = level.getGameTime();
        int[] step = {0};

        f.everyTick(120, tick -> {
            lookAt(wanda, zombie);
            PowerTicker.tick(wanda);
            switch (step[0]) {
                case 0 -> { // she holds it while the hole mends
                    if (!helper.getBlockState(dirt).is(Blocks.DIRT)) {
                        check(level.getGameTime() - lent < 20, "the hole mends in 20 ticks");
                        return false;
                    }
                    check(level.getGameTime() - lent == 20, "the hole mends when it is due: " + (level.getGameTime() - lent));
                    check(Telekinesis.holds(wanda, block) && block.isAlive(), "while she still holds the block");
                    check(!fight.loans().isOpenHome(abs(helper, dirt)), "and its home is closed");
                    step[0] = 1;
                }
                case 1 -> { // thrown at the zombie
                    pressV(wanda, false);
                    check(!Telekinesis.isHolding(wanda), "she throws it at the zombie");
                    step[0] = 2;
                }
                case 2 -> {
                    if (block.isAlive()) {
                        return false;
                    }
                    check(zombie.getHealth() < health, "the borrowed block hurt the zombie");
                    check(items(helper) == 0, "and turned to dust: no item");
                    TelekineticBlockEntity second = borrowed(helper, wanda, Telekinesis.Mode.WANDA, spare);
                    check(fight.loans().borrowed() == 1 && second.isAlive(), "a second one is held");
                    BossFights.stopAll(level.getServer(), StopReason.COMMAND);
                    step[0] = 3;
                }
                default -> {
                    if (!fight.isFinished()) {
                        return false;
                    }
                    check(lifted(helper) == 0, "after the fight nothing borrowed is left");
                    check(helper.getBlockState(dirt).is(Blocks.DIRT) && helper.getBlockState(spare).is(Blocks.DIRT), "both cells are dirt");
                    check(items(helper) == 0, "and nothing dropped");
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * T25 (D-T 5.4): a borrowed block set down 4 blocks from anyone stands where it landed, a block again; 40 ticks later
     * ({@code borrowedBlockTicks}) its cell is air and nothing dropped. A second one set down is removed at once when the
     * fight is stopped before its time.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_stand", timeoutTicks = 100)
    public static void aBorrowedBlockStandsThenCrumbles(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.BORROWED_BLOCK_TICKS, 40);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos first = new BlockPos(7, 2, 10);
        BlockPos second = new BlockPos(7, 2, 12);
        BlockPos stand = new BlockPos(12, 2, 14);
        BlockPos standToo = new BlockPos(12, 2, 7);
        helper.setBlock(first, Blocks.DIRT);
        helper.setBlock(second, Blocks.DIRT);
        setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, first), stand);
        long[] stood = {-1};

        f.everyTick(90, tick -> {
            long now = level.getGameTime();
            if (stood[0] < 0) {
                check(helper.getBlockState(stand).is(Blocks.DIRT) && fight.loans().isStanding(abs(helper, stand)),
                        "the borrowed dirt stands where it landed");
                check(lifted(helper) == 0, "a block, not a lifted one any more");
                stood[0] = now;
                return false;
            }
            if (now - stood[0] < 40) {
                check(helper.getBlockState(stand).is(Blocks.DIRT), "it stands for 40 ticks (" + (now - stood[0]) + ")");
                return false;
            }
            if (now - stood[0] == 40) {
                check(helper.getBlockState(stand).isAir() && !fight.loans().isStanding(abs(helper, stand)), "then it crumbles: the cell is air");
                check(items(helper) == 0, "and nothing dropped");
                setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, second), standToo);
                return false;
            }
            check(helper.getBlockState(standToo).is(Blocks.DIRT), "a second one stands");
            BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
            check(helper.getBlockState(standToo).isAir(), "the fight stopped before its time: it is removed at once");
            check(helper.getBlockState(first).is(Blocks.DIRT) && helper.getBlockState(second).is(Blocks.DIRT), "and both holes are mended");
            check(items(helper) == 0 && lifted(helper) == 0, "nothing dropped, nothing left");
            return true;
        });
    }

    /**
     * T26 (D-T 5.5): a hole is never deeper than one block. With a surface block borrowed, the block under its open home says
     * {@code arena_mending} and stays; once the hole has mended it is lent.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_dig", timeoutTicks = 100)
    public static void theArenaLendsNoDeeperThanOneBlock(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 40);
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos top = new BlockPos(8, 3, 10);
        BlockPos under = new BlockPos(8, 2, 10);
        helper.setBlock(under, Blocks.DIRT);
        helper.setBlock(top, Blocks.DIRT);
        dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, top));
        check(borrow(helper, wanda, Telekinesis.Mode.WANDA, under) == null, "the block under the hole is not lent");
        check(ArenaLoans.MENDING.equals(lastActionBarKey(wanda)), "the ground is still mending");
        check(helper.getBlockState(under).is(Blocks.DIRT), "and it stays");

        f.everyTick(90, tick -> {
            if (!helper.getBlockState(top).is(Blocks.DIRT)) {
                return false;
            }
            check(!fight.loans().isOpenHome(abs(helper, top)), "the hole has mended");
            TelekineticBlockEntity block = borrowed(helper, wanda, Telekinesis.Mode.WANDA, under);
            check(helper.getBlockState(under).isAir() && block.getBlockState().is(Blocks.DIRT), "now the block under it is lent");
            dropDust(wanda, block);
            return true;
        });
    }

    /**
     * T27 (D-T 5.5): with a cap of 2, one block standing and one held, the Phoenix's press says {@code arena_full}; once the
     * standing one crumbles, the arena lends again.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_cap", timeoutTicks = 100)
    public static void theArenaLendsAtMostItsLimit(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.MAX_BORROWED_BLOCKS, 2);
        f.config(BossConfig.BORROWED_BLOCK_TICKS, 30);
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer phoenix = f.player(4.5, FLOOR, 6.5, Power.PHOENIX);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos standing = new BlockPos(7, 2, 10);
        BlockPos held = new BlockPos(7, 2, 12);
        BlockPos third = new BlockPos(7, 2, 6);
        BlockPos stand = new BlockPos(13, 2, 13);
        for (BlockPos cell : new BlockPos[]{standing, held, third}) {
            helper.setBlock(cell, Blocks.DIRT);
        }
        setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, standing), stand);
        TelekineticBlockEntity holding = borrowed(helper, wanda, Telekinesis.Mode.WANDA, held);
        check(borrow(helper, phoenix, Telekinesis.Mode.JEAN, third) == null && ArenaLoans.FULL.equals(lastActionBarKey(phoenix)),
                "two out (one falling, one held): the third press says the arena lends no more");
        check(helper.getBlockState(third).is(Blocks.DIRT), "and nothing is torn out");

        f.everyTick(90, tick -> {
            look(wanda, EAST, -45.0F); // she holds hers up in the air
            PowerTicker.tick(wanda);
            check(Telekinesis.holds(wanda, holding), "Wanda holds hers all along");
            if (tick == 1) {
                check(fight.loans().isStanding(abs(helper, stand)) && fight.loans().borrowed() == 2, "one stands, one is held");
                check(borrow(helper, phoenix, Telekinesis.Mode.JEAN, third) == null && ArenaLoans.FULL.equals(lastActionBarKey(phoenix)),
                        "still nothing more");
                return false;
            }
            if (helper.getBlockState(stand).is(Blocks.DIRT)) {
                return false;
            }
            check(fight.loans().borrowed() == 1, "the standing one crumbled");
            TelekineticBlockEntity block = borrowed(helper, phoenix, Telekinesis.Mode.JEAN, third);
            check(helper.getBlockState(third).isAir(), "and the arena lends again");
            dropDust(phoenix, block);
            dropDust(wanda, holding);
            return true;
        });
    }

    /**
     * T28 (D-T 4.5): a borrowed block stays in its arena. Held while Wanda walks outward it is never carried past {@code
     * containRadius - 2} from the centre (and reaches at least {@code containRadius - 2.5}); thrown straight outward from 4
     * blocks inside the field it turns to dust at the field's edge, no item, and is gone the next tick.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_edge", timeoutTicks = 100)
    public static void aBorrowedBlockStaysInTheArena(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.FIELD);
        double contain = fight.arena().containRadius();
        BlockPos dirt = new BlockPos(8, 2, 10);
        helper.setBlock(dirt, Blocks.DIRT);
        TelekineticBlockEntity block = borrowed(helper, wanda, Telekinesis.Mode.WANDA, dirt);
        double[] farthest = {0.0};
        int[] thrownAt = {-1};

        f.everyTick(90, tick -> {
            if (tick <= 20) { // she walks east, from the centre to 5 blocks out, looking ahead
                BossFrameworkTests.moveTo(helper, wanda, 10.5 + tick * 0.25, 10.5);
                look(wanda, EAST, 0.0F);
                PowerTicker.tick(wanda);
                double out = fight.arena().distance(block.position());
                check(out <= contain - 2.0 + 1.0E-6, "the block is never carried past containRadius - 2: " + out);
                farthest[0] = Math.max(farthest[0], out);
                return false;
            }
            if (tick <= 28) { // 4 blocks inside the field, the block before her as far out as it may go
                check(farthest[0] >= contain - 2.5, "it was carried out to the limit: " + farthest[0]);
                BossFrameworkTests.moveTo(helper, wanda, 10.5 + contain - 4.0, 10.5);
                look(wanda, EAST, 0.0F);
                PowerTicker.tick(wanda);
                if (tick == 28) {
                    TestKit.sentPackets(wanda);
                    pressV(wanda, false);
                    check(!Telekinesis.isHolding(wanda), "she throws it straight outward");
                    thrownAt[0] = tick;
                }
                return false;
            }
            if (block.isAlive()) {
                check(tick - thrownAt[0] < 3, "the block is stopped at the field within a tick or two");
                return false;
            }
            List<BlockPos> dust = new ArrayList<>();
            for (Object packet : TestKit.sentPackets(wanda)) {
                if (packet instanceof ClientboundLevelEventPacket event && event.getType() == 2001) {
                    dust.add(event.getPos());
                }
            }
            check(dust.size() == 1, "it turned to dust once: " + dust);
            double at = fight.arena().distance(Vec3.atCenterOf(dust.get(0)));
            check(at <= contain + 0.5 + Math.sqrt(0.5), "at the field's edge, not beyond it: " + at);
            check(items(helper) == 0 && lifted(helper) == 0, "no item, and the block is gone");
            return true;
        });
    }

    /**
     * T29 (D-T 5.2, 7): a loan asks what a hand would. A claim (a NORMAL listener that cancels the break) refuses it, {@code
     * protected}; without the claim the same cell is lent (the arena's own listener lets the lent cell through); an
     * Adventure Wanda is refused; with {@code boss.lendArenaBlocks} off nothing is lent (3.0).
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_rights", timeoutTicks = 40)
    public static void loansAskClaimsAdventureAndTheSwitch(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer adventurer = f.player(4.5, FLOOR, 6.5, GameType.ADVENTURE);
        Powers.setPower(adventurer, Power.SCARLET_WITCH);
        arena(f, wanda, Containment.NONE);
        BlockPos claimed = new BlockPos(8, 2, 10);
        BlockPos other = new BlockPos(8, 2, 6);
        BlockPos last = new BlockPos(8, 2, 13);
        for (BlockPos cell : new BlockPos[]{claimed, other, last}) {
            helper.setBlock(cell, Blocks.DIRT);
        }
        BlockPos claimedAt = abs(helper, claimed);
        Consumer<BlockEvent.BreakEvent> claim = event -> {
            if (event.getPos().equals(claimedAt)) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, claim);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(claim));

        check(borrow(helper, wanda, Telekinesis.Mode.WANDA, claimed) == null, "a claim keeps the arena from lending its block");
        check(ArenaLoans.PROTECTED.equals(lastActionBarKey(wanda)) && helper.getBlockState(claimed).is(Blocks.DIRT), "\"This place is protected\"");
        NeoForge.EVENT_BUS.unregister(claim);
        dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, claimed));
        check(helper.getBlockState(claimed).isAir(), "without the claim the same cell is lent");

        check(borrow(helper, adventurer, Telekinesis.Mode.WANDA, other) == null, "an Adventure Wanda borrows nothing");
        check(ArenaLoans.PROTECTED.equals(lastActionBarKey(adventurer)) && helper.getBlockState(other).is(Blocks.DIRT), "she is told it is protected");

        f.config(BossConfig.LEND_ARENA_BLOCKS, false);
        check(borrow(helper, wanda, Telekinesis.Mode.WANDA, last) == null, "with lendArenaBlocks off the arena lends nothing");
        check(ArenaLoans.PROTECTED.equals(lastActionBarKey(wanda)) && helper.getBlockState(last).is(Blocks.DIRT), "as in 3.0");
        helper.succeed();
    }

    /**
     * T30 (D-T 5.2): neighbours never notice a borrowed block. A wall torch on the side of a stone, sand on another stone, a
     * poppy on a grass block: each supporting block is borrowed, and nothing drops, the sand does not fall, the torch and the
     * poppy stay; after the mends every one of the nine cells (the three supports, what they hold, the floor under them) is
     * exactly as before.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_quiet", timeoutTicks = 100)
    public static void neighboursNeverNoticeABorrowedBlock(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 20);
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos torchStone = new BlockPos(7, 2, 7);
        BlockPos sandStone = new BlockPos(7, 2, 13);
        BlockPos grass = new BlockPos(13, 2, 7);
        helper.setBlock(torchStone, Blocks.STONE);
        helper.setBlock(torchStone.east(), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST));
        helper.setBlock(sandStone, Blocks.STONE);
        helper.setBlock(sandStone.above(), Blocks.SAND);
        helper.setBlock(grass, Blocks.GRASS_BLOCK);
        helper.setBlock(grass.above(), Blocks.POPPY);
        BlockPos[] cells = {torchStone, torchStone.east(), torchStone.below(), sandStone, sandStone.above(), sandStone.below(),
                grass, grass.above(), grass.below()};
        BlockState[] before = new BlockState[cells.length];
        for (int i = 0; i < cells.length; i++) {
            before[i] = helper.getBlockState(cells[i]);
        }

        f.everyTick(90, tick -> {
            if (tick < SETTLE) {
                return false;
            }
            if (tick == SETTLE) {
                for (BlockPos support : new BlockPos[]{torchStone, sandStone, grass}) {
                    dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, support));
                    check(helper.getBlockState(support).isAir(), "the support at " + support + " is lent");
                }
            }
            check(helper.getEntities(EntityType.FALLING_BLOCK).isEmpty(), "the sand does not fall");
            check(items(helper) == 0, "nothing drops");
            if (fight.loans().isOpenHome(abs(helper, torchStone)) || fight.loans().isOpenHome(abs(helper, sandStone))
                    || fight.loans().isOpenHome(abs(helper, grass))) {
                check(helper.getBlockState(torchStone.east()).is(Blocks.WALL_TORCH), "the torch stays on the air");
                check(helper.getBlockState(sandStone.above()).is(Blocks.SAND), "the sand stays over the hole");
                check(helper.getBlockState(grass.above()).is(Blocks.POPPY), "the poppy stays");
                return false;
            }
            for (int i = 0; i < cells.length; i++) {
                check(helper.getBlockState(cells[i]) == before[i], "after the mends " + cells[i] + " is " + before[i] + ", not " + helper.getBlockState(cells[i]));
            }
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The end of a fight, the journal, a crash

    /**
     * T31 (D-T 5.7, R48): one borrowed block held, one flying, one standing, and their homes open, when the fight is ended:
     * after the wind-down every home has mended, the standing cell is air, no borrowed block is left and nothing dropped.
     * The journal is still on disk once everything handed over is written; two saves of the dimension later it is gone.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_end", timeoutTicks = 60)
    public static void nothingBorrowedIsAwayWhenTheFightEnds(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer billy = f.player(4.5, FLOOR, 14.5, Power.BILLY);
        ServerPlayer phoenix = f.player(4.5, FLOOR, 6.5, Power.PHOENIX);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos held = new BlockPos(7, 2, 10);
        BlockPos flying = new BlockPos(7, 2, 14);
        BlockPos standing = new BlockPos(7, 2, 6);
        BlockPos stand = new BlockPos(13, 2, 9);
        for (BlockPos cell : new BlockPos[]{held, flying, standing}) {
            helper.setBlock(cell, Blocks.DIRT);
        }
        setDown(helper, phoenix, borrowed(helper, phoenix, Telekinesis.Mode.JEAN, standing), stand);

        f.everyTick(50, tick -> {
            if (tick == 1) {
                check(fight.loans().isStanding(abs(helper, stand)), "one stands");
                borrowed(helper, wanda, Telekinesis.Mode.WANDA, held);
                borrowed(helper, billy, Telekinesis.Mode.BILLY, flying);
                look(billy, EAST, -10.0F);
                pressV(billy, false);
                check(!Telekinesis.isHolding(billy) && Telekinesis.isHolding(wanda), "one flies, one is held");
                check(fight.loans().isOpenHome(abs(helper, held)), "and the homes are open");
                fight.end(BossEnd.COMMAND);
                return false;
            }
            if (!fight.isFinished()) {
                return false;
            }
            for (BlockPos home : new BlockPos[]{held, flying, standing}) {
                check(helper.getBlockState(home).is(Blocks.DIRT), "the home " + home + " has mended");
            }
            check(helper.getBlockState(stand).isAir(), "the standing block is gone");
            check(lifted(helper) == 0 && items(helper) == 0, "nothing borrowed is left, nothing dropped");
            MinecraftServer server = level.getServer();
            check(journal(server, fight) != null, "the journal is still on disk: no save holds the repairs yet");
            saveHook(level);
            saveHook(level);
            check(journal(server, fight) == null, "two saves later it is gone");
            return true;
        });
    }

    /**
     * T32 (D-T 5.8, R48): the journal puts the ground back after a crash. Two cells borrowed and one block standing: the
     * journal (2 homes, 1 standing; not the copy cell also borrowed) was written by the journal's thread, not the server's.
     * The fight is lost as a crash would lose it; the recovery, at the next tick, puts both homes back and the standing cell
     * to air, and keeps the journal; recovering it again changes nothing (a block a player put on the standing cell
     * meanwhile stays); two saves later the journal is gone. The copy home stays empty: the island's own recovery clears it.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_crash", timeoutTicks = 60)
    public static void theLedgerIsRestoredAfterACrash(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, true);
        BlockPos copy = new BlockPos(7, 2, 6);
        BlockPos copyAt = abs(helper, copy);
        fight.copyCells = pos -> pos.equals(copyAt);
        BossFrameworkTests.start(f, BossKind.ULTRON, wanda, fight);
        BlockPos homeA = new BlockPos(7, 2, 10);
        BlockPos homeB = new BlockPos(7, 2, 13);
        BlockPos stand = new BlockPos(13, 2, 10);
        for (BlockPos cell : new BlockPos[]{copy, homeA, homeB}) {
            helper.setBlock(cell, Blocks.DIRT);
        }
        dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, copy));
        setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, homeA), stand);
        int[] step = {0};

        f.everyTick(50, tick -> {
            switch (step[0]) {
                case 0 -> {
                    check(fight.loans().isStanding(abs(helper, stand)), "one block stands");
                    TelekineticBlockEntity loose = borrowed(helper, wanda, Telekinesis.Mode.WANDA, homeB);
                    CompoundTag journal = journal(server, fight);
                    check(journal != null, "the fight has a journal");
                    check(journal.getList("homes", Tag.TAG_COMPOUND).size() == 2 && journal.getList("standing", Tag.TAG_COMPOUND).size() == 1,
                            "2 homes and 1 standing block: " + journal);
                    check(lists(journal, "homes", abs(helper, homeA)) && lists(journal, "homes", abs(helper, homeB))
                            && lists(journal, "standing", abs(helper, stand)), "the right ones");
                    check(!lists(journal, "homes", copyAt), "a copy home is never written");
                    check(ArenaJournalWriter.THREAD.equals(ArenaJournalWriter.lastWriter())
                            && !server.getRunningThread().getName().equals(ArenaJournalWriter.lastWriter()),
                            "written by the journal's thread, not the server's: " + ArenaJournalWriter.lastWriter());
                    // The crash: the fight is gone without giving anything back, and so is what was held.
                    BossFights.forgetForTests(fight);
                    Telekinesis.release(wanda, false);
                    loose.discard();
                    check(helper.getBlockState(homeA).isAir() && helper.getBlockState(homeB).isAir() && helper.getBlockState(stand).is(Blocks.DIRT),
                            "the world as the crash left it");
                    ArenaLoansRecovery.recoverAll(server);
                    step[0] = 1;
                }
                case 1 -> {
                    check(helper.getBlockState(homeA).is(Blocks.DIRT) && helper.getBlockState(homeB).is(Blocks.DIRT), "the next tick both homes are back");
                    check(helper.getBlockState(stand).isAir(), "and the standing cell is air");
                    check(helper.getBlockState(copy).isAir(), "the copy home is not put back (its island clears it)");
                    check(journal(server, fight) != null, "the journal is kept until the world has saved it");
                    helper.setBlock(stand, Blocks.COBBLESTONE); // a player builds where the borrowed block stood
                    ArenaLoansRecovery.recoverAll(server);
                    step[0] = 2;
                }
                default -> {
                    check(helper.getBlockState(stand).is(Blocks.COBBLESTONE), "recovering it again leaves the player's block");
                    check(helper.getBlockState(homeA).is(Blocks.DIRT) && helper.getBlockState(homeB).is(Blocks.DIRT), "and changes nothing else");
                    check(items(helper) == 0, "nothing dropped");
                    saveHook(level);
                    saveHook(level);
                    check(journal(server, fight) == null, "two saves later the journal is gone");
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * T60 (R48): a mend stays in the journal until the world has saved it. Borrowed and mended with no save: the snapshot
     * the journal gets still lists the home; after one save it still does; after the second it does not, and once the fight
     * is over (with nothing left) its journal is deleted. A fight that ended with a repair still waiting: when the server
     * has stopped (the stop is simulated) everything left is retired, and the journal deleted.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_journal", timeoutTicks = 80)
    public static void aMendIsJournaledUntilTheWorldSavedIt(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 20);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos home = new BlockPos(7, 2, 10);
        BlockPos second = new BlockPos(7, 2, 13);
        helper.setBlock(home, Blocks.DIRT);
        helper.setBlock(second, Blocks.DIRT);
        BlockPos homeAt = abs(helper, home);
        dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, home));
        CompoundTag written = journal(server, fight);
        check(written != null && lists(written, "homes", homeAt), "the journal knows of the hole");

        f.everyTick(60, tick -> {
            if (!helper.getBlockState(home).is(Blocks.DIRT)) {
                return false;
            }
            // (All in the tick of the mend: no save of the world comes between.)
            check(lists(fight.loans().snapshot(), "homes", homeAt), "mended, and no save: the snapshot still lists the home");
            CompoundTag next = journal(server, fight);
            check(next != null && lists(next, "homes", homeAt), "and so does the journal's next snapshot");
            saveHook(level);
            check(lists(fight.loans().snapshot(), "homes", homeAt), "one save: still listed");
            saveHook(level);
            check(!lists(fight.loans().snapshot(), "homes", homeAt), "the second: gone");
            CompoundTag after = journal(server, fight);
            check(after != null && !lists(after, "homes", homeAt), "from the journal too");
            BossFights.stopAll(server, StopReason.SERVER_STOPPING);
            check(journal(server, fight) == null, "the fight over, with nothing left to keep, its journal is deleted");

            // A fight that ends with a repair no save holds yet: its journal waits, until the server has stopped.
            TestFight later = arena(f, wanda, Containment.NONE);
            dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, second));
            BossFights.stopAll(server, StopReason.SERVER_STOPPING);
            check(helper.getBlockState(second).is(Blocks.DIRT), "its hole was mended at the end");
            CompoundTag waiting = journal(server, later);
            check(waiting != null && lists(waiting, "homes", abs(helper, second)), "and its journal waits for the saves");
            ArenaJournalWriter.retireAll(server);
            check(journal(server, later) == null, "the server stopped: everything left is retired");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Gravity, forts, the shield, the queue

    /**
     * T52 (D-T 5.2, 5.3, 5.5): mended sand never falls. (a) Sand on stone borrowed and mended is back in its cell with no
     * tick scheduled, and never falls; (b) sand over a 3-block air pocket is not lent ({@code arena_hanging}); (c) of two
     * gravel blocks stacked on stone, the lower one is lent and a zombie stands in its hole: the upper one, now over an open
     * home, is not lent, and stays for 100 ticks; the zombie gone, the lower one mends within 20 ticks, every cell as before;
     * (d) a stone under a standing borrowed block is not lent ({@code arena_mending}); (e) a sand home over the air pocket
     * mends and the sand stays put for 60 ticks, the pocket empty.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_gravity", timeoutTicks = 200)
    public static void mendedSandNeverFalls(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 20);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos sand = new BlockPos(7, 2, 7);
        BlockPos hanging = new BlockPos(13, 5, 7);
        BlockPos lowGravel = new BlockPos(10, 2, 14);
        BlockPos highGravel = lowGravel.above();
        BlockPos stone = new BlockPos(13, 2, 13);
        BlockPos spare = new BlockPos(7, 2, 10);
        helper.setBlock(sand, Blocks.SAND);
        helper.setBlock(lowGravel, Blocks.GRAVEL);
        helper.setBlock(highGravel, Blocks.GRAVEL);
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(spare, Blocks.DIRT);
        level.setBlock(abs(helper, hanging), Blocks.SAND.defaultBlockState(), ArenaLoans.FLAGS);
        ArenaLoans.quiet(level, abs(helper, hanging));
        Zombie[] zombie = {null};
        long[] sandBack = {-1};
        long[] pocketBack = {-1};
        int[] gone = {-1};

        f.everyTick(190, tick -> {
            check(helper.getEntities(EntityType.FALLING_BLOCK).isEmpty(), "no block falls, ever");
            if (tick < SETTLE) {
                return false;
            }
            long now = level.getGameTime();
            if (tick == SETTLE) {
                // (a)
                dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, sand));
                // (b)
                check(borrow(helper, wanda, Telekinesis.Mode.WANDA, hanging) == null && ArenaLoans.HANGING.equals(lastActionBarKey(wanda)),
                        "(b) sand over an air pocket hangs: the arena won't lend it");
                check(helper.getBlockState(hanging).is(Blocks.SAND), "(b) it stays");
                // (c)
                dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, lowGravel));
                zombie[0] = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(lowGravel.getX() + 0.5, FLOOR, lowGravel.getZ() + 0.5));
                zombie[0].setBaby(true);
                check(borrow(helper, wanda, Telekinesis.Mode.WANDA, highGravel) == null && ArenaLoans.HANGING.equals(lastActionBarKey(wanda)),
                        "(c) the gravel over the open home hangs: not lent");
                // (d): a borrowed block set down on the stone
                setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, spare), stone.above());
                // (e): the pocket's cell emptied, and a sand home recorded there
                level.setBlock(abs(helper, hanging), Blocks.AIR.defaultBlockState(), ArenaLoans.FLAGS);
                fight.loans().recordHomeForTests(abs(helper, hanging), Blocks.SAND.defaultBlockState());
                return false;
            }
            if (tick == SETTLE + 1) {
                check(fight.loans().isStanding(abs(helper, stone.above())), "(d) a borrowed block stands on the stone");
                check(borrow(helper, wanda, Telekinesis.Mode.WANDA, stone) == null && ArenaLoans.MENDING.equals(lastActionBarKey(wanda)),
                        "(d) the stone under it is not lent");
                check(helper.getBlockState(stone).is(Blocks.STONE), "(d) it stays");
            }
            if (sandBack[0] < 0 && helper.getBlockState(sand).is(Blocks.SAND)) {
                sandBack[0] = now;
                check(!level.getBlockTicks().hasScheduledTick(abs(helper, sand), Blocks.SAND), "(a) the mended sand has no tick to fall at");
            }
            if (pocketBack[0] < 0 && helper.getBlockState(hanging).is(Blocks.SAND)) {
                pocketBack[0] = now;
                check(!level.getBlockTicks().hasScheduledTick(abs(helper, hanging), Blocks.SAND), "(e) nor the sand over the pocket");
            }
            if (pocketBack[0] >= 0) {
                check(helper.getBlockState(hanging).is(Blocks.SAND), "(e) the sand over the pocket stays put");
                for (int y = 2; y < hanging.getY(); y++) {
                    check(helper.getBlockState(new BlockPos(hanging.getX(), y, hanging.getZ())).isAir(), "(e) the pocket under it stays empty");
                }
            }
            if (sandBack[0] >= 0) {
                check(helper.getBlockState(sand).is(Blocks.SAND), "(a) the mended sand stays in its cell");
            }
            if (tick <= SETTLE + 100) {
                check(helper.getBlockState(highGravel).is(Blocks.GRAVEL), "(c) the upper gravel stays in its cell");
                check(helper.getBlockState(lowGravel).isAir(), "(c) the lower home waits for the zombie");
                if (tick == SETTLE + 100) {
                    zombie[0].discard();
                    gone[0] = tick;
                }
                return false;
            }
            if (!helper.getBlockState(lowGravel).is(Blocks.GRAVEL)) {
                check(tick - gone[0] < 20, "(c) the lower gravel mends within 20 ticks of the zombie leaving");
                return false;
            }
            check(helper.getBlockState(highGravel).is(Blocks.GRAVEL), "(c) every gravel cell as before");
            if (sandBack[0] < 0 || pocketBack[0] < 0 || now - sandBack[0] < 60 || now - pocketBack[0] < 60) {
                return false;
            }
            check(items(helper) == 0, "nothing dropped");
            return true;
        });
    }

    /**
     * T53 (D-T 5.4): borrowed blocks build no fort. Wanda's three set down 4 blocks from anyone all stand; a fourth crumbles
     * (no block, no item) and she is told the arena takes it back; one set down on top of a standing borrowed block and one
     * next to a zombie crumble too; when one of hers has crumbled (its time was short) a set-down stands again; Billy can
     * still stand his own three.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_no_fort", timeoutTicks = 120)
    public static void borrowedBlocksBuildNoFort(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.BORROWED_BLOCK_TICKS, 30);
        f.config(EnhancedConfig.BILLY_BLOCK_LIFT_COOLDOWN, 0);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer billy = f.player(3.5, FLOOR, 14.5, Power.BILLY);
        TestFight fight = arena(f, wanda, Containment.NONE);
        Zombie zombie = stillZombie(helper, 10.5, 4.5);
        BlockPos[] sources = new BlockPos[10];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = new BlockPos(6, 2, 6 + i);
            helper.setBlock(sources[i], Blocks.DIRT);
        }
        BlockPos p1 = new BlockPos(11, 2, 9);
        BlockPos p2 = new BlockPos(13, 2, 11);
        BlockPos p3 = new BlockPos(11, 2, 13);
        BlockPos p4 = new BlockPos(14, 2, 8);
        BlockPos p5 = new BlockPos(14, 2, 14);
        BlockPos byZombie = new BlockPos(11, 2, 4);
        BlockPos[] billys = {new BlockPos(9, 2, 15), new BlockPos(12, 2, 16), new BlockPos(15, 2, 12)};
        int[] next = {0};
        int[] step = {0};
        long[] p1Stood = {-1};
        // P1 stands for 30 ticks only; the others long.
        setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, sources[next[0]++]), p1);

        f.everyTick(110, tick -> {
            switch (step[0]) {
                case 0 -> {
                    check(fight.loans().isStanding(abs(helper, p1)), "the first stands");
                    p1Stood[0] = level.getGameTime();
                    f.config(BossConfig.BORROWED_BLOCK_TICKS, 600); // the others stand long
                    setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, sources[next[0]++]), p2);
                }
                case 1 -> {
                    check(fight.loans().isStanding(abs(helper, p2)), "the second stands");
                    setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, sources[next[0]++]), p2.above());
                }
                case 2 -> {
                    check(helper.getBlockState(p2.above()).isAir() && !fight.loans().isStanding(abs(helper, p2.above())),
                            "one set down on top of a standing borrowed block crumbles");
                    check(ArenaLoans.CRUMBLES.equals(lastActionBarKey(wanda)), "and she is told the arena takes it back");
                    setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, sources[next[0]++]), byZombie);
                }
                case 3 -> {
                    check(helper.getBlockState(byZombie).isAir(), "one set down a block from the zombie crumbles");
                    setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, sources[next[0]++]), p3);
                }
                case 4 -> {
                    check(fight.loans().isStanding(abs(helper, p1)) && fight.loans().isStanding(abs(helper, p2))
                            && fight.loans().isStanding(abs(helper, p3)), "her three stand");
                    lastActionBarKey(wanda);
                    setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, sources[next[0]++]), p4);
                }
                case 5 -> {
                    check(helper.getBlockState(p4).isAir() && !fight.loans().isStanding(abs(helper, p4)), "a fourth crumbles");
                    check(ArenaLoans.CRUMBLES.equals(lastActionBarKey(wanda)), "the arena takes it back: no building");
                    check(items(helper) == 0, "no item");
                }
                case 6 -> {
                    if (helper.getBlockState(p1).is(Blocks.DIRT)) {
                        check(level.getGameTime() - p1Stood[0] < 30, "the first one stands its 30 ticks");
                        return false;
                    }
                    setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, sources[next[0]++]), p5);
                }
                case 7 -> {
                    check(fight.loans().isStanding(abs(helper, p5)), "one of hers crumbled: a set-down stands again");
                    for (int i = 0; i < billys.length; i++) {
                        setDown(helper, billy, borrowed(helper, billy, Telekinesis.Mode.BILLY, sources[next[0]++]), billys[i]);
                    }
                }
                default -> {
                    for (BlockPos cell : billys) {
                        check(fight.loans().isStanding(abs(helper, cell)), "Billy stands his own three: " + cell);
                    }
                    check(items(helper) == 0, "nothing ever dropped");
                    check(zombie.isAlive(), "the zombie was never walled in");
                    return true;
                }
            }
            step[0]++;
            return false;
        });
    }

    /**
     * T54 (D-T 9): a block borrowed from Apocalypse's floor and thrown by Wanda wears his shield by half a slam: {@code 10.4
     * x slamFactor x 0.5}; a zombie she throws as fast 20 ticks later wears it by the full slam, {@code 6 x slamFactor}.
     * Both are her mind power, so both are critical on top ({@code critShieldFactor} 2: spec 8a.2 doubles the numbers).
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_shield", timeoutTicks = 60)
    public static void borrowedBlocksWearHisShieldByHalf(GameTestHelper helper) {
        shieldWear(helper, 0.5, true);
    }

    /** T54, with {@code apocalypse_shield.borrowedSlamFactor} 1.0: the borrowed block wears the shield by 10.4 (x2). */
    @EnhancedTest(template = ARENA, batch = "arena_loans_shield_full", timeoutTicks = 60)
    public static void borrowedBlocksWearHisShieldInFullAtFactorOne(GameTestHelper helper) {
        shieldWear(helper, 1.0, false);
    }

    private static void shieldWear(GameTestHelper helper, double borrowedFactor, boolean zombieToo) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        d.f().config(ApocalypseShieldConfig.BORROWED_SLAM_FACTOR, borrowedFactor);
        ApocalypseEntity boss = d.boss();
        ServerPlayer wanda = d.hero();
        float slam = ApocalypseShieldConfig.SLAM_FACTOR.get().floatValue();
        double crit = ApocalypseShieldConfig.CRIT_SHIELD_FACTOR.get(); // her mind power is critical on his shield (8a)
        BlockPos floor = new BlockPos(5, 1, 10);
        check(BossFights.protects(d.level(), abs(helper, floor)), "his floor is protected");
        TelekineticBlockEntity block = borrowed(helper, wanda, Telekinesis.Mode.WANDA, floor);
        check(boss.shield().isUp(), "his shield is up");
        // The block a step and a half before him, at his chest, and thrown straight at him.
        double x = boss.getBoundingBox().minX - 1.5;
        block.setPos(x, boss.getY() + 1.0, boss.getZ());
        look(wanda, EAST, 0.0F);
        float before = boss.shield().strength();
        float health = boss.getHealth();
        pressV(wanda, false);
        check(!Telekinesis.isHolding(wanda), "she throws the borrowed block");
        float[] afterBlock = {-1.0F};

        d.f().everyTick(50, tick -> {
            if (afterBlock[0] < 0.0F) {
                if (block.isAlive()) {
                    return false;
                }
                afterBlock[0] = boss.shield().strength();
                assertNear(before - afterBlock[0], 10.4 * slam * borrowedFactor * crit, 0.01, "the borrowed block wears the shield by 10.4 x slamFactor x "
                        + borrowedFactor + " x " + crit);
                check(boss.getHealth() == health && boss.shield().isUp(), "his shield took it, not his health");
                check(items(helper) == 0, "and it turned to dust");
                return !zombieToo;
            }
            if (tick < 25) {
                return false;
            }
            if (tick == 25) {
                Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(5.5, FLOOR, 13.5));
                lookAt(wanda, zombie);
                pressV(wanda, false);
                check(Telekinesis.holds(wanda, zombie), "she takes hold of a zombie");
                zombie.moveTo(boss.getBoundingBox().minX - 1.5, boss.getY() + 0.5, boss.getZ(), zombie.getYRot(), 0.0F);
                look(wanda, EAST, 0.0F);
                pressV(wanda, false);
                check(!Telekinesis.isHolding(wanda), "and throws it at him as fast");
                return false;
            }
            if (boss.shield().strength() >= afterBlock[0]) {
                check(tick < 40, "the zombie never reached his shield");
                return false;
            }
            assertNear(afterBlock[0] - boss.shield().strength(), 6.0 * slam * crit, 0.01, "the zombie wears it by the full slam, 6 x slamFactor x " + crit);
            return true;
        });
    }

    /**
     * T55 (D-T 5.3): a blocked home does not hold up the others. Home A is lent and a zombie stands in it; 10 ticks later
     * home B is lent: B mends when it is due (20 ticks after its loan), while A waits; A mends within 20 ticks of the
     * zombie's leaving.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_queue", timeoutTicks = 100)
    public static void aBlockedHomeDoesNotHoldUpTheOthers(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 20);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        arena(f, wanda, Containment.NONE);
        BlockPos homeA = new BlockPos(8, 2, 8);
        BlockPos homeB = new BlockPos(8, 2, 13);
        helper.setBlock(homeA, Blocks.DIRT);
        helper.setBlock(homeB, Blocks.DIRT);
        dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, homeA));
        Zombie zombie = stillZombie(helper, homeA.getX() + 0.5, homeA.getZ() + 0.5);
        long[] lentB = {-1};
        long[] left = {-1};

        f.everyTick(90, tick -> {
            long now = level.getGameTime();
            if (tick == 10) {
                dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, homeB));
                lentB[0] = now;
                return false;
            }
            if (lentB[0] < 0) {
                return false;
            }
            if (now - lentB[0] < 20) {
                check(helper.getBlockState(homeB).isAir(), "B waits for its time");
                return false;
            }
            if (now - lentB[0] == 20) {
                check(helper.getBlockState(homeB).is(Blocks.DIRT), "B mends when it is due");
                check(helper.getBlockState(homeA).isAir(), "while A waits for the zombie to leave");
                return false;
            }
            if (left[0] < 0) {
                if (now - lentB[0] == 30) {
                    zombie.discard();
                    left[0] = now;
                }
                return false;
            }
            if (!helper.getBlockState(homeA).is(Blocks.DIRT)) {
                check(now - left[0] <= 20, "A mends within 20 ticks of the zombie's leaving");
                return false;
            }
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the world puts into a hole; a cell lent again and again (review round 1)

    /**
     * D-T 5.3, 5.7, 5.8 (review fix): what the world puts into an open hole by itself never keeps the ground from coming
     * back. Three cells of dirt are borrowed and their holes filled as the world would: a snow layer (it snowed into the
     * hole), ice (the water the hole kept froze) and a poppy (a bone meal grew it on the dirt under the hole). The snow and
     * the ice are mended over when they are due; the poppy is not a hole any more, so its home waits, and the end of the
     * fight mends it too, with nothing dropped. After a crash the recovery puts back a hole the snow fell into.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_weather", timeoutTicks = 100)
    public static void aHoleTheWorldFillsStillMends(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 20);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos snowed = new BlockPos(8, 2, 6);
        BlockPos frozen = new BlockPos(8, 2, 10);
        BlockPos grown = new BlockPos(8, 2, 14);
        BlockPos[] cells = {snowed, frozen, grown};
        BlockState[] fills = {Blocks.SNOW.defaultBlockState(), Blocks.ICE.defaultBlockState(), Blocks.POPPY.defaultBlockState()};
        helper.setBlock(grown.below(), Blocks.DIRT); // (a poppy grows on dirt, not on the smooth stone of the floor)
        for (BlockPos cell : cells) {
            helper.setBlock(cell, Blocks.DIRT);
        }
        for (int i = 0; i < cells.length; i++) {
            dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, cells[i]));
            check(helper.getBlockState(cells[i]).isAir() && fight.loans().isOpenHome(abs(helper, cells[i])), "the dirt at " + cells[i] + " is lent");
            level.setBlock(abs(helper, cells[i]), fills[i], Block.UPDATE_CLIENTS);
        }
        long lent = level.getGameTime();
        int[] step = {0};

        f.everyTick(90, tick -> {
            switch (step[0]) {
                case 0 -> {
                    if (level.getGameTime() - lent < 20) {
                        return false;
                    }
                    check(helper.getBlockState(snowed).is(Blocks.DIRT) && !fight.loans().isOpenHome(abs(helper, snowed)),
                            "the hole the snow fell into mends when it is due");
                    check(helper.getBlockState(frozen).is(Blocks.DIRT) && !fight.loans().isOpenHome(abs(helper, frozen)),
                            "so does the one whose water froze");
                    check(helper.getBlockState(grown).is(Blocks.POPPY) && fight.loans().isOpenHome(abs(helper, grown)),
                            "a poppy is not a hole: its home waits");
                    fight.end(BossEnd.COMMAND);
                    step[0] = 1;
                }
                case 1 -> {
                    if (!fight.isFinished()) {
                        return false;
                    }
                    check(helper.getBlockState(grown).is(Blocks.DIRT) && !fight.loans().isOpenHome(abs(helper, grown)),
                            "the end of the fight mends it all the same: the ground is whole");
                    check(items(helper) == 0 && lifted(helper) == 0, "nothing dropped, the poppy and the snow included");
                    // A crash: a hole the snow fell into, and the fight lost without giving anything back.
                    TestFight crashed = arena(f, wanda, Containment.NONE);
                    dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, snowed));
                    level.setBlock(abs(helper, snowed), Blocks.SNOW.defaultBlockState(), Block.UPDATE_CLIENTS);
                    BossFights.forgetForTests(crashed);
                    ArenaJournalWriter.flush(); // (what was handed over is on the disk, as a crash a moment later leaves it)
                    ArenaLoansRecovery.recoverAll(server);
                    step[0] = 2;
                }
                default -> {
                    check(helper.getBlockState(snowed).is(Blocks.DIRT), "the recovery puts the dirt back over the snow");
                    check(items(helper) == 0, "nothing dropped");
                    saveHook(level);
                    saveHook(level); // (the journals are gone)
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Spec 17, R48 (review fix): the journal keeps one repair of a cell, the newest. With no save of the world between,
     * Wanda borrows the same dirt three times, each time once it has mended, and twice sets a borrowed block down on the
     * same cell, each time once the last has crumbled and its home has mended: the snapshot lists the dirt's home once, the
     * second home once and the standing cell once. (A real autosave between them would change nothing of it: a newer repair
     * replaces an older one, and only two saves retire one.)
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_repairs", timeoutTicks = 160)
    public static void aCellLentAgainAndAgainIsJournaledOnce(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 10);
        f.config(BossConfig.BORROWED_BLOCK_TICKS, 10);
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos home = new BlockPos(7, 2, 8);
        BlockPos source = new BlockPos(7, 2, 12);
        BlockPos stand = new BlockPos(12, 2, 14);
        helper.setBlock(home, Blocks.DIRT);
        helper.setBlock(source, Blocks.DIRT);
        int[] lent = {0};
        int[] stood = {0};

        f.everyTick(150, tick -> {
            if (lent[0] < 3) {
                if (helper.getBlockState(home).is(Blocks.DIRT) && !fight.loans().isOpenHome(abs(helper, home))) {
                    dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, home));
                    lent[0]++;
                }
                return false;
            }
            if (stood[0] < 2 || fight.loans().isStanding(abs(helper, stand)) || fight.loans().isOpenHome(abs(helper, source))) {
                if (stood[0] < 2 && !fight.loans().isStanding(abs(helper, stand)) && helper.getBlockState(source).is(Blocks.DIRT)
                        && !fight.loans().isOpenHome(abs(helper, source))) {
                    setDown(helper, wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, source), stand);
                    stood[0]++;
                }
                return false;
            }
            if (!helper.getBlockState(home).is(Blocks.DIRT) || !helper.getBlockState(stand).isAir()) {
                return false;
            }
            CompoundTag snapshot = fight.loans().snapshot();
            check(entries(snapshot, "homes", abs(helper, home)) == 1, "the dirt lent three times is one repair: " + snapshot);
            check(entries(snapshot, "homes", abs(helper, source)) == 1, "so is the home lent twice");
            check(entries(snapshot, "standing", abs(helper, stand)) == 1, "and the cell two borrowed blocks stood on");
            return true;
        });
    }

    /**
     * Review r3 (D-T 5.5, spec 5.4: the real ground is never edited): what leans on a lent block is safe from its neighbours
     * while the hole is open. The lower of two sands of a step is lent; a snow layer then settles on the upper one (its fall
     * is scheduled): it never falls, and stays over the hole. A grass block with a poppy and one with a rose bush are lent; a
     * stone then set beside the poppy and one beside the rose bush break them: nothing drops, the journal lists them with
     * their homes, and the mends put the grass, the poppy and both halves of the rose bush back. A stone with an item frame
     * on its side is not lent ({@code protected}) and the frame stays.
     */
    @EnhancedTest(template = ARENA, batch = "arena_loans_propped", timeoutTicks = 140)
    public static void whatLeansOnAHoleIsNeverLost(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 40);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestFight fight = arena(f, wanda, Containment.NONE);
        BlockPos lowSand = new BlockPos(7, 2, 7);
        BlockPos highSand = lowSand.above();
        BlockPos grass = new BlockPos(13, 2, 7);
        BlockPos poppy = grass.above();
        BlockPos roseGrass = new BlockPos(13, 2, 13);
        BlockPos roseLow = roseGrass.above();
        BlockPos roseHigh = roseLow.above();
        BlockPos framed = new BlockPos(7, 2, 13);
        helper.setBlock(lowSand, Blocks.SAND);
        helper.setBlock(highSand, Blocks.SAND);
        helper.setBlock(grass, Blocks.GRASS_BLOCK);
        helper.setBlock(poppy, Blocks.POPPY);
        helper.setBlock(roseGrass, Blocks.GRASS_BLOCK);
        DoublePlantBlock.placeAt(level, Blocks.ROSE_BUSH.defaultBlockState(), abs(helper, roseLow), Block.UPDATE_ALL);
        helper.setBlock(framed, Blocks.STONE);
        ItemFrame frame = new ItemFrame(level, abs(helper, framed.north()), Direction.NORTH);
        level.addFreshEntity(frame);
        BlockPos[] cells = {lowSand, highSand, grass, poppy, roseGrass, roseLow, roseHigh, framed};
        BlockState[] before = new BlockState[cells.length];
        for (int i = 0; i < cells.length; i++) {
            before[i] = helper.getBlockState(cells[i]);
        }
        check(before[6].is(Blocks.ROSE_BUSH) && before[6].getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER, "the rose bush is whole");
        long[] lent = {-1};

        f.everyTick(130, tick -> {
            check(helper.getEntities(EntityType.FALLING_BLOCK).isEmpty(), "no block falls, ever (tick " + tick + ")");
            check(items(helper) == 0, "nothing drops, ever (tick " + tick + ")");
            if (tick < SETTLE) {
                return false;
            }
            long now = level.getGameTime();
            if (tick == SETTLE) {
                check(borrow(helper, wanda, Telekinesis.Mode.WANDA, framed) == null && ArenaLoans.PROTECTED.equals(lastActionBarKey(wanda)),
                        "a stone with an item frame on it is not lent");
                for (BlockPos support : new BlockPos[]{lowSand, grass, roseGrass}) {
                    dropDust(wanda, borrowed(helper, wanda, Telekinesis.Mode.WANDA, support));
                }
                check(fight.loans().isPropped(abs(helper, poppy)) && fight.loans().isPropped(abs(helper, roseLow))
                        && fight.loans().isPropped(abs(helper, roseHigh)), "the poppy and both halves of the rose bush lean on the holes");
                lent[0] = now;
                return false;
            }
            if (tick == SETTLE + 1) {
                // The world goes on round the holes: snow settles on the upper sand, stones come beside the flowers.
                level.setBlockAndUpdate(abs(helper, highSand.above()), Blocks.SNOW.defaultBlockState());
                check(level.getBlockTicks().hasScheduledTick(abs(helper, highSand), Blocks.SAND), "the snow wakes the upper sand: its fall is scheduled");
                level.setBlockAndUpdate(abs(helper, poppy.east()), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(abs(helper, roseLow.east()), Blocks.STONE.defaultBlockState());
                check(helper.getBlockState(poppy).isAir() && helper.getBlockState(roseLow).isAir() && helper.getBlockState(roseHigh).isAir(),
                        "the stones break the poppy and the rose bush, which hang in the air");
                CompoundTag snapshot = fight.loans().snapshot();
                check(lists(snapshot, "homes", abs(helper, poppy)) && lists(snapshot, "homes", abs(helper, roseLow))
                        && lists(snapshot, "homes", abs(helper, roseHigh)), "the journal lists them with their homes: " + snapshot);
                return false;
            }
            check(helper.getBlockState(highSand).is(Blocks.SAND), "the upper sand stays over the hole (tick " + tick + ")");
            check(helper.getBlockState(framed).is(Blocks.STONE) && frame.isAlive(), "the framed stone and its frame stay");
            if (fight.loans().isOpenHome(abs(helper, lowSand)) || fight.loans().isOpenHome(abs(helper, grass))
                    || fight.loans().isOpenHome(abs(helper, roseGrass))) {
                return false;
            }
            if (now - lent[0] < 60) {
                return false; // (the upper sand stays a while after the mend, too)
            }
            for (int i = 0; i < cells.length; i++) {
                check(helper.getBlockState(cells[i]) == before[i], "after the mends " + cells[i] + " is " + before[i] + ", not " + helper.getBlockState(cells[i]));
            }
            check(!fight.loans().isPropped(abs(helper, poppy)), "and nothing leans on a hole any more");
            frame.discard();
            return true;
        });
    }

    private ArenaLoanTests() {
    }
}

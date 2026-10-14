package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BlockLifting;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.ArenaJournalWriter;
import com.kolodkingal.enhanced.boss.ArenaLoans;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGifts;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import com.kolodkingal.enhanced.ultron.SokoviaLift;
import com.kolodkingal.enhanced.ultron.SokoviaRecovery;
import com.kolodkingal.enhanced.ultron.SokoviaTemplate;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.GROUND;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.LIFT;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.MIDDLE_X;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.MIDDLE_Z;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.PAD;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.SURFACE;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.Script;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.blocksInTheSky;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.client;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.groundHash;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.journals;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.pad;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.ritual;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.toTheIsland;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.calm;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.onTheGround;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.put;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.tank;

/**
 * Block telekinesis and Ultron's island in the sky (3.1, D-T 5 and 6.1.3): the island lends its blocks whatever the arena's
 * config says, and stays whole; the ground lends nothing while it is about to rise or the island comes down, and the ground
 * hidden under the island gives nothing and takes nothing; the lift carries nothing borrowed, and nothing it carries can be
 * taken; and the island's note, like the arena's journal, forgets the island only once the world has saved it (R48).
 *
 * <p>Every test runs on the Sokovia pad with the real lift ({@link SokoviaTestSupport#pad}), and has a batch of its own: a
 * fight is server-wide state. The cells used are within five blocks of the middle for the island (the rim, which wanders
 * with the fight's seed, is never nearer than six) and 17 blocks out for the ring of ground round it (inside the field),
 * each within two blocks of the hero who takes it, so that the hero's eye ray meets it and not the ground before it. The
 * fighters are moved off the middle, where Ultron floats during the lift.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SokoviaLoanTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Where Wanda stands (and is carried up from): three blocks west of the middle. */
    private static final double WANDA_X = MIDDLE_X - 3.0;
    /** Where a hero stands on the ring of ground round the island (16 blocks east of the middle, inside the field). */
    private static final double RING_X = MIDDLE_X + 16.0;
    /** A cell of the ring next to him: lent by the arena, and no copy of anything. */
    private static final BlockPos RING_CELL = new BlockPos(38, SURFACE, 21);

    /** The pad with the real lift, no Sentries, and every grip let go of when the test ends. */
    private static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.onClose(Telekinesis::clear);
        return f;
    }

    /** Wanda, at the middle of the pad: she starts the fight, which is centred on her ({@link #start} then moves her). */
    private static ServerPlayer wanda(TestKit.Fixture f) {
        ServerPlayer wanda = f.player(MIDDLE_X, GROUND, MIDDLE_Z, Power.SCARLET_WITCH);
        tank(wanda);
        return wanda;
    }

    /** Starts the fight on the ground, calm, with Wanda off the middle. */
    private static UltronFight start(TestKit.Fixture f, ServerPlayer wanda) {
        UltronFight fight = onTheGround(f, wanda);
        calm(fight);
        put(f, wanda, WANDA_X, GROUND, MIDDLE_Z);
        return fight;
    }

    /** Nothing of the fight acts while the test does its work on the island: no wave, no debris. */
    private static void quiet(UltronFight fight) {
        calm(fight);
        fight.setNextDebrisAt(Long.MAX_VALUE / 2);
    }

    /** Presses the telekinesis key (V), with Sneak held or not. */
    private static void pressV(ServerPlayer player, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, 3, true);
        player.setShiftKeyDown(false);
    }

    /** Looks at the middle of a cell of the pad (relative position). */
    private static void lookAtCell(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        lookAt(player, Vec3.atCenterOf(helper.absolutePos(relative)));
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

    /**
     * Wanda's block telekinesis at a cell of the pad (it is that cell she tears out, if any): what she holds after it (null
     * if nothing).
     */
    @Nullable
    private static TelekineticBlockEntity lift(GameTestHelper helper, ServerPlayer hero, BlockPos relative) {
        lookAtCell(helper, hero, relative);
        check(BlockLifting.tryLift(hero, Telekinesis.Mode.WANDA), "a block is in reach at " + relative);
        TelekineticBlockEntity held = heldBlock(helper, hero);
        check(held == null || helper.getBlockState(relative).isAir(), "the block taken is the one at " + relative);
        return held;
    }

    /** Wanda borrows the block at a cell: she holds it, and it is borrowed. */
    private static TelekineticBlockEntity borrowed(GameTestHelper helper, ServerPlayer hero, BlockPos relative) {
        TelekineticBlockEntity block = lift(helper, hero, relative);
        check(block != null && block.isBorrowed(), "the block at " + relative + " is lent: " + lastActionBarKey(hero));
        return block;
    }

    /** Puts the held block over a point of the pad (relative) and lets go of it gently: it falls from there. */
    private static void letGoOver(GameTestHelper helper, ServerPlayer hero, TelekineticBlockEntity block, double x, double y, double z) {
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        block.setPos(at.x, at.y, at.z);
        Telekinesis.release(hero, true);
    }

    /** Moves a player to an absolute position, at rest. */
    private static void moveTo(ServerPlayer player, Vec3 at) {
        player.moveTo(at.x, at.y, at.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static int items(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM).size();
    }

    private static int lifted(GameTestHelper helper) {
        return helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).size();
    }

    private static int falling(GameTestHelper helper) {
        return helper.getEntities(EntityType.FALLING_BLOCK).size();
    }

    /** One save of the dimension, as the game posts it once the chunks are queued to the disk. */
    private static void saveHook(ServerLevel level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Save(level));
    }

    /** The state the lift's copy holds for a cell of the ground (absolute), or null if it copied none there. */
    @Nullable
    private static BlockState copied(SokoviaTemplate template, BlockPos ground) {
        for (int i = 0; i < template.size(); i++) {
            if (template.x(i) == ground.getX() && template.y(i) == ground.getY() && template.z(i) == ground.getZ()) {
                return template.state(i);
            }
        }
        return null;
    }

    private static SokoviaTemplate template(UltronFight fight) {
        SokoviaLift.Lift lift = SokoviaLift.of(fight);
        check(lift != null && lift.template() != null, "the lift copied the island");
        return lift.template();
    }

    /** The fight's arena journal as it is on the disk once everything handed over is written, or null if there is none. */
    @Nullable
    private static CompoundTag arenaJournal(MinecraftServer server, UltronFight fight) {
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

    /** The entry of a list of the journal ({@code homes} or {@code standing}) at the cell (absolute), or null. */
    @Nullable
    private static CompoundTag entry(CompoundTag journal, String list, BlockPos pos) {
        for (Tag tag : journal.getList(list, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            if (entry.getInt("x") == pos.getX() && entry.getInt("y") == pos.getY() && entry.getInt("z") == pos.getZ()) {
                return entry;
            }
        }
        return null;
    }

    /** Whether any block update sent among {@code packets} is for the cell (absolute). */
    private static boolean updates(List<Object> packets, BlockPos pos) {
        boolean[] found = {false};
        for (Object packet : packets) {
            if (packet instanceof ClientboundBlockUpdatePacket update && update.getPos().equals(pos)) {
                found[0] = true;
            } else if (packet instanceof ClientboundSectionBlocksUpdatePacket section) {
                section.runUpdates((at, state) -> found[0] |= at.equals(pos));
            }
        }
        return found[0];
    }

    // ------------------------------------------------------------------------------------------------------------
    // The island lends

    /**
     * T33 (D-T 5.5, 6.1.3): on the island, a diamond-ore copy (set into the ground before the lock) is borrowed and thrown:
     * it turns to dust, nothing drops; its home mends to the state the lift copied; the ore in the ground below, which it
     * was copied from, was never touched.
     */
    @GameTest(template = PAD, batch = "ultron_loans_island", timeoutTicks = 400)
    public static void theIslandLendsItsBlocksAndStaysWhole(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 20);
        ServerLevel level = f.level();
        BlockPos ore = new BlockPos(19, SURFACE, 22);
        BlockPos copy = ore.above(LIFT);
        helper.setBlock(ore, Blocks.DIAMOND_ORE);
        long ground = groundHash(helper);
        ServerPlayer wanda = wanda(f);
        UltronFight fight = start(f, wanda);
        TelekineticBlockEntity[] block = new TelekineticBlockEntity[1];
        Script script = toTheIsland(fight, wanda);
        script.then("Wanda borrows the ore and throws it", () -> {
            quiet(fight);
            check(helper.getBlockState(copy).is(Blocks.DIAMOND_ORE), "the island has the ore's copy");
            check(BossFights.isCopyCell(level, abs(helper, copy.getX(), copy.getY(), copy.getZ())), "on a copy cell");
            block[0] = borrowed(helper, wanda, copy);
            check(helper.getBlockState(copy).isAir(), "its cell is open");
            look(wanda, 0.0F, 70.0F); // (down at the island in front of her)
            pressV(wanda, false);
            check(!Telekinesis.isHolding(wanda), "she throws it");
        });
        script.step("it turns to dust", () -> !block[0].isAlive());
        script.then("nothing drops", () -> check(items(helper) == 0, "a borrowed copy never drops anything: " + items(helper) + " items"));
        script.step("the home mends", () -> helper.getBlockState(copy).is(Blocks.DIAMOND_ORE));
        script.then("as the lift copied it", () -> {
            check(helper.getBlockState(copy) == copied(template(fight), abs(helper, ore.getX(), ore.getY(), ore.getZ())), "the copy is the template's state");
            check(helper.getBlockState(ore).is(Blocks.DIAMOND_ORE) && groundHash(helper) == ground, "the ground below is as it was");
            check(items(helper) == 0, "and no diamond anywhere");
        });
        script.run(f, 380);
    }

    /**
     * T34 (D-T 5.5, 6.1.3, R48): a block of the ring of ground round the island is borrowed, carried up and set down on the
     * island, over a tuft of grass's copy, where it stands, journaled as standing on a copy cell with nothing but air before it (a
     * crash recovery leaves the island's own to clear). A press at the ground
     * hidden under the island takes nothing ({@code protected}). The fight ends: when the island has landed the ring is as
     * it was, the sky is empty and nothing dropped (the clearing never met the ring's block). Both journals, the arena's
     * and the island's note, are still on disk, and gone after two saves of the world.
     */
    @GameTest(template = PAD, batch = "ultron_loans_ground", timeoutTicks = 700)
    public static void aRingBlockSetDownOnTheIslandIsGoneByTheDescent(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.BORROWED_BLOCK_TICKS, 2400);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        BlockPos stand = new BlockPos(24, SURFACE + LIFT + 1, 18);
        helper.setBlock(stand.below(LIFT), Blocks.SHORT_GRASS); // (the island will carry its copy)
        long ground = groundHash(helper);
        ServerPlayer wanda = wanda(f);
        UltronFight fight = start(f, wanda);
        BlockPos standAt = abs(helper, stand.getX(), stand.getY(), stand.getZ());
        BlockPos ringAt = abs(helper, RING_CELL.getX(), RING_CELL.getY(), RING_CELL.getZ());
        BlockPos hidden = new BlockPos(19, SURFACE, 20);
        Script script = toTheIsland(fight, wanda);
        script.then("a ring block is borrowed and carried up", () -> {
            quiet(fight);
            check(helper.getBlockState(stand).is(Blocks.SHORT_GRASS), "(the island has the grass's copy)");
            Vec3 up = wanda.position();
            put(f, wanda, RING_X, GROUND, MIDDLE_Z);
            TelekineticBlockEntity block = borrowed(helper, wanda, RING_CELL);
            check(!BossFights.isCopyCell(level, ringAt), "(the ring is real ground)");
            moveTo(wanda, up);
            letGoOver(helper, wanda, block, stand.getX() + 0.5, stand.getY() + 0.01, stand.getZ() + 0.5);
        });
        script.step("it stands on the island", () -> fight.loans().isStanding(standAt));
        script.then("journaled with air before it", () -> {
            check(helper.getBlockState(stand).is(Blocks.GRASS_BLOCK) && BossFights.isCopyCell(level, standAt), "the ring's grass stands on a copy cell");
            check(helper.getBlockState(stand.below(LIFT)).is(Blocks.SHORT_GRASS), "(the grass on the ground is untouched)");
            CompoundTag journal = arenaJournal(server, fight);
            check(journal != null, "the fight has a journal");
            CompoundTag standing = entry(journal, "standing", standAt);
            check(standing != null && NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), standing.getCompound("previous")).isAir(),
                    "it lists the standing block, with air before it: " + journal);
            check(entry(journal, "homes", ringAt) != null, "and the ring's home");
            // A press at the ground under the island: it is hidden, and gives nothing.
            Vec3 up = wanda.position();
            put(f, wanda, MIDDLE_X - 2.0, GROUND, MIDDLE_Z);
            lookAtCell(helper, wanda, hidden);
            check(BossFights.hidesGround(level, abs(helper, hidden.getX(), hidden.getY(), hidden.getZ())), "(the ground there is hidden)");
            pressV(wanda, false);
            check(ArenaLoans.PROTECTED.equals(lastActionBarKey(wanda)), "a press at the hidden ground says it is protected");
            check(!Telekinesis.isHolding(wanda) && helper.getBlockState(hidden).is(Blocks.GRASS_BLOCK), "and takes nothing");
            moveTo(wanda, up);
            fight.end(BossEnd.COMMAND);
        });
        script.step("the island comes down", () -> {
            client(wanda);
            return fight.isFinished();
        });
        script.then("everything is as it was", () -> {
            check(helper.getBlockState(RING_CELL).is(Blocks.GRASS_BLOCK) && groundHash(helper) == ground, "the ring is as it was before the fight");
            check(blocksInTheSky(helper) == 0, "the sky is empty: " + blocksInTheSky(helper));
            check(items(helper) == 0 && lifted(helper) == 0, "nothing borrowed is left, nothing dropped");
            check(arenaJournal(server, fight) != null, "the arena's journal is still on disk: no save holds the repairs yet");
            SokoviaJournal.Header note = SokoviaJournal.read(server, fight.id());
            check(note != null && SokoviaJournal.LANDED.equals(note.state()) && journals(level).isEmpty(),
                    "the island's note is kept, retiring, and says it landed: " + note);
            saveHook(level);
            saveHook(level);
            check(arenaJournal(server, fight) == null, "two saves later the arena's journal is gone");
            check(!Files.exists(SokoviaJournal.retiringFile(server, fight.id())) && SokoviaJournal.read(server, fight.id()) == null,
                    "and so is the island's note");
        });
        script.run(f, 680);
    }

    /**
     * T35 (D-T 5.6, 5.7): a cell of the ground borrowed before the lift; at the warning it is mended at once and the borrowed
     * block is gone; a press at the ground then says it is about to rise ({@code arena_moving}); at the lock the island
     * copies the cell as it was.
     */
    @GameTest(template = PAD, batch = "ultron_loans_warn", timeoutTicks = 200)
    public static void theGroundLendsNothingWhileItIsAboutToRise(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = wanda(f);
        UltronFight fight = start(f, wanda);
        BlockPos cell = new BlockPos(18, SURFACE, 20);
        BlockPos other = new BlockPos(17, SURFACE, 22);
        TelekineticBlockEntity[] block = new TelekineticBlockEntity[1];
        Script script = new Script();
        script.step("the fighter joins", () -> fight.participants().size() == 1);
        script.then("a cell is borrowed, then the warning", () -> {
            block[0] = borrowed(helper, wanda, cell);
            check(helper.getBlockState(cell).isAir(), "the cell is open");
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            check(fight.isLifting(), "(the island will rise)");
            check(helper.getBlockState(cell).is(Blocks.GRASS_BLOCK), "at the warning the cell is mended at once");
            check(block[0].isRemoved() && items(helper) == 0, "and the borrowed block is gone, leaving nothing");
        });
        script.step("she holds nothing", () -> {
            PowerTicker.tick(wanda); // (what her client's tick does: the grip on a block that is gone ends)
            return !Telekinesis.isHolding(wanda);
        });
        script.then("the ground lends nothing more", () -> {
            lookAtCell(helper, wanda, other);
            pressV(wanda, false);
            check("message.enhanced.telekinesis.arena_moving".equals(lastActionBarKey(wanda)), "a press says the ground is about to rise");
            check(helper.getBlockState(other).is(Blocks.GRASS_BLOCK) && lifted(helper) == 0, "and nothing is torn out");
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
            check(copied(template(fight), abs(helper, cell.getX(), cell.getY(), cell.getZ())) == Blocks.GRASS_BLOCK.defaultBlockState(),
                    "the lift copies the cell as it was");
        });
        script.run(f, 180);
    }

    /**
     * T36 (D-T 4.2, 6.1.1): at the lock, the island carries Wanda, and her press says so ({@code carried}); a player the
     * lift does not carry (Creative, come after the lock, out on the ring) cannot take a Sentry the lift carries.
     */
    @GameTest(template = PAD, batch = "ultron_loans_carry", timeoutTicks = 100)
    public static void theCarriedCanNeitherGrabNorBeGrabbed(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper); // (with the two Sentries of the start)
        f.onClose(Telekinesis::clear);
        ServerPlayer wanda = wanda(f);
        UltronFight fight = start(f, wanda);
        BlockPos cell = new BlockPos(18, SURFACE, 20);
        f.everyTick(80, tick -> {
            if (fight.participants().size() < 1 || fight.sentries().isEmpty()) {
                return false;
            }
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
            check(SokoviaLift.isCarried(wanda) && BossFights.isLocked(wanda), "the island carries Wanda");
            lookAtCell(helper, wanda, cell);
            pressV(wanda, false);
            check("message.enhanced.telekinesis.carried".equals(lastActionBarKey(wanda)), "her press says she is carried");
            pressV(wanda, true);
            check("message.enhanced.telekinesis.carried".equals(lastActionBarKey(wanda)), "and so does Sneak + V");
            check(!Telekinesis.isHolding(wanda) && helper.getBlockState(cell).is(Blocks.GRASS_BLOCK), "she takes nothing");
            ServerPlayer billy = f.player(RING_X, GROUND, MIDDLE_Z, GameType.CREATIVE);
            Powers.setPower(billy, Power.BILLY);
            check(!SokoviaLift.isCarried(billy), "(Billy, come after the lock, is not carried)");
            UltronSentryEntity sentry = fight.sentries().get(0);
            check(SokoviaLift.isCarried(sentry), "the lift carries the Sentry");
            check(!Telekinesis.canLift(billy, sentry, Telekinesis.Mode.BILLY), "Billy cannot take it");
            check(BossGrip.UNTOUCHABLE.equals(Telekinesis.refusal(billy, sentry, Telekinesis.Mode.BILLY)), "it is untouchable");
            return true;
        });
    }

    /**
     * T37 (D-T 5.6, 5.7): Wanda holds a borrowed block of the island when its descent begins: it is gone at once, she holds
     * nothing, no block falls to the ground and nothing drops; a press at the ring's ground then says the island is coming
     * down ({@code arena_descending}).
     */
    @GameTest(template = PAD, batch = "ultron_loans_descent", timeoutTicks = 500)
    public static void theDescentCrumblesWhatIsBorrowed(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = wanda(f);
        UltronFight fight = start(f, wanda);
        BlockPos copy = new BlockPos(19, SURFACE + LIFT, 22);
        TelekineticBlockEntity[] block = new TelekineticBlockEntity[1];
        ServerPlayer[] ringer = new ServerPlayer[1];
        Script script = toTheIsland(fight, wanda);
        script.then("Wanda holds a borrowed block of the island; the fight ends", () -> {
            quiet(fight);
            block[0] = borrowed(helper, wanda, copy);
            fight.end(BossEnd.COMMAND);
            // A hero out on the ring, come after the end: no fighter, so the descent does not take him up.
            ringer[0] = f.player(RING_X, GROUND, MIDDLE_Z, Power.SCARLET_WITCH);
        });
        script.step("the descent begins", () -> {
            client(wanda);
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            return lift != null && lift.step().ordinal() >= SokoviaLift.Step.DESCENT_LOCK.ordinal();
        });
        script.then("the borrowed block is gone", () -> {
            check(block[0].isRemoved() && lifted(helper) == 0, "the borrowed block crumbled at the descent");
            check(items(helper) == 0, "and dropped nothing");
            check(!fight.isParticipant(ringer[0].getUUID()) && !SokoviaLift.isCarried(ringer[0]), "(the hero on the ring is not carried)");
            lookAtCell(helper, ringer[0], RING_CELL);
            pressV(ringer[0], false);
            check("message.enhanced.telekinesis.arena_descending".equals(lastActionBarKey(ringer[0])), "a press at the ring says the island is coming down");
            check(helper.getBlockState(RING_CELL).is(Blocks.GRASS_BLOCK) && !Telekinesis.isHolding(ringer[0]), "and takes nothing");
        });
        script.step("she holds nothing", () -> {
            client(wanda);
            PowerTicker.tick(wanda);
            return !Telekinesis.isHolding(wanda);
        });
        script.step("the island lands", () -> {
            client(wanda);
            check(lifted(helper) == 0, "no borrowed block falls to the ground");
            return fight.isFinished();
        });
        script.then("nothing is left under it", () -> {
            check(items(helper) == 0 && lifted(helper) == 0, "no item and no block anywhere under the island");
            check(blocksInTheSky(helper) == 0, "and the sky is empty");
        });
        script.run(f, 480);
    }

    /**
     * T56 (D-T 5.2): a sand copy on the island, resting on the island's own dirt, is borrowed and mended: it is back as the
     * lift copied it, with no tick scheduled at its cell, and it never falls.
     */
    @GameTest(template = PAD, batch = "ultron_loans_gravity", timeoutTicks = 400)
    public static void theIslandsSandStaysPut(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ARENA_MEND_TICKS, 20);
        ServerLevel level = f.level();
        BlockPos sand = new BlockPos(19, SURFACE, 22);
        BlockPos copy = sand.above(LIFT);
        BlockPos copyAt = abs(helper, copy.getX(), copy.getY(), copy.getZ());
        helper.setBlock(sand, Blocks.SAND);
        ServerPlayer wanda = wanda(f);
        UltronFight fight = start(f, wanda);
        int[] since = {-1};
        Script script = toTheIsland(fight, wanda);
        script.then("the sand copy is borrowed", () -> {
            quiet(fight);
            check(helper.getBlockState(copy).is(Blocks.SAND) && helper.getBlockState(copy.below()).is(Blocks.DIRT), "the island's sand rests on its dirt");
            TelekineticBlockEntity block = borrowed(helper, wanda, copy);
            Telekinesis.release(wanda, false);
            block.crumble();
        });
        script.step("it mends", () -> helper.getBlockState(copy).is(Blocks.SAND));
        script.then("quietly", () -> {
            check(!level.getBlockTicks().hasScheduledTick(copyAt, Blocks.SAND), "no tick is scheduled at the mended sand");
            check(helper.getBlockState(copy) == copied(template(fight), abs(helper, sand.getX(), sand.getY(), sand.getZ())), "it is the lift's copy");
            since[0] = 0;
        });
        script.step("and it stays put", () -> {
            check(falling(helper) == 0, "no falling sand");
            check(helper.getBlockState(copy).is(Blocks.SAND), "the sand is in its cell");
            return ++since[0] >= 40;
        });
        script.run(f, 380);
    }

    /**
     * T57 (D-T 5.1, 5.6): with {@code boss.protectArena} off, the island is still protected and lends its copies, while the
     * ring of ground is the world's: a diamond-ore copy borrowed and thrown at a zombie hurts it and drops nothing; a break
     * event on the island is cancelled and on the ring it is not; the ground hidden under the island still gives nothing; a
     * ring block Wanda lifts is the real one, not borrowed.
     */
    @GameTest(template = PAD, batch = "ultron_loans_unprotected", timeoutTicks = 400)
    public static void theIslandLendsWhateverTheArenaConfigSays(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.PROTECT_ARENA, false);
        ServerLevel level = f.level();
        BlockPos ore = new BlockPos(19, SURFACE, 22);
        BlockPos copy = ore.above(LIFT);
        helper.setBlock(ore, Blocks.DIAMOND_ORE);
        ServerPlayer wanda = wanda(f);
        UltronFight fight = start(f, wanda);
        BlockPos islandAt = abs(helper, copy.getX() + 1, copy.getY(), copy.getZ());
        BlockPos ringAt = abs(helper, RING_CELL.getX(), RING_CELL.getY(), RING_CELL.getZ());
        TelekineticBlockEntity[] block = new TelekineticBlockEntity[1];
        Zombie[] zombie = new Zombie[1];
        Script script = toTheIsland(fight, wanda);
        script.then("the island is protected, the ring is not", () -> {
            quiet(fight);
            check(BossFights.protects(level, islandAt) && !BossFights.protects(level, ringAt), "the island alone is protected");
            BlockEvent.BreakEvent onTheIsland = new BlockEvent.BreakEvent(level, islandAt, level.getBlockState(islandAt), wanda);
            NeoForge.EVENT_BUS.post(onTheIsland);
            BlockEvent.BreakEvent onTheRing = new BlockEvent.BreakEvent(level, ringAt, level.getBlockState(ringAt), wanda);
            NeoForge.EVENT_BUS.post(onTheRing);
            check(onTheIsland.isCanceled() && !onTheRing.isCanceled(), "breaking on the island is refused, on the ring it is not");
            zombie[0] = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(MIDDLE_X + 2.0, GROUND + LIFT, MIDDLE_Z + 2.0));
            zombie[0].setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            block[0] = borrowed(helper, wanda, copy);
            // The copy a step and a half before the zombie, at its chest, and thrown straight at it.
            block[0].setPos(zombie[0].getBoundingBox().minX - 1.5, zombie[0].getY() + 0.5, zombie[0].getZ());
            look(wanda, EAST, 0.0F);
            pressV(wanda, false);
            check(!Telekinesis.isHolding(wanda), "she throws the ore's copy");
        });
        script.step("it hits the zombie", () -> !block[0].isAlive());
        script.then("and turns to dust", () -> {
            check(zombie[0].getHealth() < zombie[0].getMaxHealth(), "the zombie is hurt");
            check(items(helper) == 0, "no diamond drops");
            // The ground hidden under the island gives nothing either, protected or not.
            Vec3 up = wanda.position();
            put(f, wanda, MIDDLE_X - 2.0, GROUND, MIDDLE_Z);
            BlockPos hidden = new BlockPos(19, SURFACE, 20);
            check(!BossFights.protects(level, abs(helper, hidden.getX(), hidden.getY(), hidden.getZ())), "(the ground is not protected)");
            lookAtCell(helper, wanda, hidden);
            pressV(wanda, false);
            check(ArenaLoans.PROTECTED.equals(lastActionBarKey(wanda)), "a press at the hidden ground says it is protected");
            check(!Telekinesis.isHolding(wanda) && helper.getBlockState(hidden).is(Blocks.GRASS_BLOCK), "and takes nothing");
            // A ring block is the world's own: lifted, not borrowed.
            put(f, wanda, RING_X, GROUND, MIDDLE_Z);
            TelekineticBlockEntity real = lift(helper, wanda, RING_CELL);
            check(real != null && !real.isBorrowed(), "a ring block is lifted as in 3.0, not borrowed");
            check(helper.getBlockState(RING_CELL).isAir(), "(it is out of the ground)");
            Telekinesis.release(wanda, false);
            real.discard();
            helper.setBlock(RING_CELL, Blocks.GRASS_BLOCK);
            moveTo(wanda, up);
        });
        script.run(f, 380);
    }

    /**
     * T58 (D-T 5.6): with the island up, a poppy grows on the ground hidden under it, and a tuft of grass beside it (grass a
     * block may replace; a poppy it may not). A borrowed ring block let go over either crumbles where it lands: the plant is
     * still there and no block update for its cell is sent (a client would have shown the cell under the island). A block
     * lifted outside the arena and let go over either does the same, as its item. (The hero on the ring plays a client that
     * was sent the poppy's chunk; the check that his log would see an update of such a cell: the test changes one at the
     * end, and the update is there.)
     */
    @GameTest(template = PAD, batch = "ultron_loans_hidden", timeoutTicks = 500)
    public static void nothingLandsOnTheHiddenGround(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        BlockPos poppy = new BlockPos(19, SURFACE + 1, 19);
        BlockPos grass = new BlockPos(20, SURFACE + 1, 19);
        helper.setBlock(poppy, Blocks.POPPY);
        helper.setBlock(grass, Blocks.SHORT_GRASS);
        ServerPlayer wanda = wanda(f);
        UltronFight fight = start(f, wanda);
        TelekineticBlockEntity[] block = new TelekineticBlockEntity[1];
        ServerPlayer[] ringer = new ServerPlayer[1];
        Script script = toTheIsland(fight, wanda);
        script.then("a hero comes to the ring", () -> {
            quiet(fight);
            for (BlockPos plant : List.of(poppy, grass)) {
                check(BossFights.hidesGround(level, abs(helper, plant.getX(), plant.getY(), plant.getZ())), "the plant at " + plant + " is hidden under the island");
            }
            ringer[0] = f.player(RING_X, GROUND, MIDDLE_Z, Power.SCARLET_WITCH);
            level.getChunkSource().move(ringer[0]); // (his client's view is where he stands, not where he logged in)
        });
        script.step("his client is sent the plants' chunk", () -> {
            ringer[0].connection.chunkSender.onChunkBatchReceivedByClient(64.0F); // (what his client answers each batch)
            return level.getChunkSource().chunkMap.getPlayers(new ChunkPos(abs(helper, poppy.getX(), poppy.getY(), poppy.getZ())), false).contains(ringer[0]);
        });
        // Two borrowed ring blocks, then two blocks of the world lifted outside the arena: over the poppy, then the grass.
        List<BlockPos> ring = List.of(RING_CELL, RING_CELL.south());
        List<BlockPos> outside = List.of(new BlockPos(2, SURFACE, 4), new BlockPos(2, SURFACE, 6));
        for (int i = 0; i < 4; i++) {
            boolean borrowed = i < 2;
            BlockPos plant = i % 2 == 0 ? poppy : grass;
            BlockState was = i % 2 == 0 ? Blocks.POPPY.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState();
            BlockPos from = borrowed ? ring.get(i) : outside.get(i - 2);
            BlockPos plantAt = abs(helper, plant.getX(), plant.getY(), plant.getZ());
            int[] itemsBefore = {0};
            script.then((borrowed ? "a borrowed ring block" : "a block of the world") + " is let go over the " + was.getBlock(), () -> {
                if (borrowed) {
                    block[0] = borrowed(helper, ringer[0], from);
                } else {
                    put(f, ringer[0], 3.5, GROUND, 5.5);
                    check(!BossFights.protects(level, abs(helper, from.getX(), from.getY(), from.getZ())), "(that cell is outside the arena)");
                    block[0] = lift(helper, ringer[0], from);
                    check(block[0] != null && !block[0].isBorrowed(), "a block of the world is lifted");
                    put(f, ringer[0], RING_X, GROUND, MIDDLE_Z);
                }
                itemsBefore[0] = items(helper);
                TestKit.sentPackets(ringer[0]);
                letGoOver(helper, ringer[0], block[0], plant.getX() + 0.5, plant.getY() + 1.0, plant.getZ() + 0.5);
            });
            script.step("it lands", () -> !block[0].isAlive());
            script.then("and does not stay there", () -> {
                check(helper.getBlockState(plant) == was, "the " + was.getBlock() + " is still there: " + helper.getBlockState(plant));
                List<Object> sent = TestKit.sentPackets(ringer[0]);
                check(!updates(sent, plantAt), "no block update was sent for its cell");
                check(lifted(helper) == 0, "the block is gone");
                if (borrowed) {
                    check(items(helper) == itemsBefore[0], "a borrowed block crumbles to nothing");
                    check(ArenaLoans.CRUMBLES.equals(BossTestSupport.lastActionBarKeyIn(sent)), "and its holder is told the arena took it back");
                } else {
                    check(items(helper) == itemsBefore[0] + 1, "a block of the world shatters into its item");
                }
            });
        }
        script.then("the log would see an update", () -> helper.setBlock(poppy, Blocks.DANDELION));
        script.step("the update is there", () -> updates(TestKit.sentPackets(ringer[0]), abs(helper, poppy.getX(), poppy.getY(), poppy.getZ())));
        script.run(f, 480);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The island's note (R48)

    /**
     * T68 (R48, spec 5.4): when the island has landed its note is still on disk, retiring: it says the island landed and
     * owes nothing. A crash then (the server forgets it) and a recovery of it clear the island's column in the sky (a stone
     * left there is gone) and give no Program back. A recovery's own note, of an island whose Program was owed, pays it back
     * once; its note says so until the first save, owes nothing after it, and is gone at the second. The ritual's note,
     * which clears nothing, is still deleted at once.
     */
    @GameTest(template = PAD, batch = "ultron_loans_note", timeoutTicks = 700)
    public static void aLandedIslandsNoteWaitsForTheSave(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(starter);
        UltronFight fight = ritual(f, starter);
        check(fight.programUsed(), "(the ritual used a Program)");
        UUID id = fight.id();
        BlockPos stone = new BlockPos(18, SURFACE + LIFT, 21);
        Script script = new Script();
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            calm(fight);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island rises", () -> {
            client(starter);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("the fight is won", () -> fight.end(BossEnd.VICTORY));
        script.step("the island lands", () -> {
            client(starter);
            return fight.isFinished();
        });
        script.then("its note waits for the save", () -> {
            check(journals(level).isEmpty() && !Files.exists(SokoviaJournal.file(server, id)), "no live note is left");
            SokoviaJournal.Header landed = SokoviaJournal.read(server, id);
            check(Files.exists(SokoviaJournal.retiringFile(server, id)) && SokoviaJournal.isRetiring(id), "the note is kept, retiring");
            check(landed != null && SokoviaJournal.LANDED.equals(landed.state()) && !landed.refund(), "it says the island landed and owes nothing: " + landed);
            long programs = programs(server, starter);

            // A crash now: the server forgets the note; the next start finds it, and a stone left in the island's column.
            SokoviaJournal.forgetForTests(id);
            helper.setBlock(stone, Blocks.STONE);
            // ... and a note of another island, whose Program is owed, as a crash during its fight would leave it.
            UUID other = UUID.randomUUID();
            SokoviaJournal.Header owed = new SokoviaJournal.Header(other, level.dimension(), landed.centre(), landed.spec(), landed.minY(), landed.maxY(),
                    SokoviaJournal.PASTED, starter.getUUID(), true, List.of());
            try {
                SokoviaJournal.write(server, owed);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            check(SokoviaRecovery.recoverAll(server) == 2, "the next start recovers both notes");
            SokoviaRecovery.finishAllNow(); // (the clearings, done at once)
            check(helper.getBlockState(stone).isAir(), "the landed island's column in the sky is cleared");
            check(programs(server, starter) == programs + 1, "the Program is given back once (the owed note's), none for the landed one");
            SokoviaJournal.Header paid = SokoviaJournal.read(server, other);
            check(paid != null && paid.refund() && Files.exists(SokoviaJournal.retiringFile(server, other)) && journals(level).isEmpty(),
                    "the recovered note is kept, retiring, and still says it is owed: no save holds the gift yet");
            check(SokoviaJournal.isRetiring(id) && SokoviaJournal.isRetiring(other), "both notes are retiring");
            saveHook(level);
            paid = SokoviaJournal.read(server, other);
            check(paid != null && !paid.refund(), "after one save it owes nothing");
            check(SokoviaJournal.read(server, id) != null, "the landed note is still there");
            saveHook(level);
            check(SokoviaJournal.read(server, other) == null && !Files.exists(SokoviaJournal.retiringFile(server, other)), "after the second it is gone");
            check(SokoviaJournal.read(server, id) == null && !Files.exists(SokoviaJournal.retiringFile(server, id)), "and so is the landed note");
            check(SokoviaRecovery.recoverAll(server) == 0 && programs(server, starter) == programs + 1, "nothing is recovered or given again");

            // The ritual's note clears nothing: it is deleted at once, as in 3.0.
            UUID ritualId = UUID.randomUUID();
            try {
                SokoviaJournal.write(server, SokoviaJournal.Header.ritual(ritualId, level.dimension(), landed.centre(), landed.spec(), starter.getUUID()));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            SokoviaJournal.delete(server, ritualId);
            check(!Files.exists(SokoviaJournal.file(server, ritualId)) && !Files.exists(SokoviaJournal.retiringFile(server, ritualId))
                    && !SokoviaJournal.isRetiring(ritualId), "a ritual's note is deleted at once");
        });
        script.run(f, 680);
    }

    /** How many Ultron Programs wait for the player as gifts. */
    private static long programs(MinecraftServer server, ServerPlayer player) {
        return BossGifts.pending(server, player.getUUID()).stream().filter(stack -> stack.is(ModItems.ULTRON_PROGRAM.get())).count();
    }

    private SokoviaLoanTests() {
    }
}

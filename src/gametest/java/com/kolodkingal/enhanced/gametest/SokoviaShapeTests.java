package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ultron.SokoviaJob;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import com.kolodkingal.enhanced.ultron.SokoviaShape;
import com.kolodkingal.enhanced.ultron.SokoviaShape.Spec;
import com.kolodkingal.enhanced.ultron.SokoviaTemplate;
import com.kolodkingal.enhanced.ultron.SokoviaValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.GROUND;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.LIFT;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.MIDDLE_X;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.MIDDLE_Z;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.PAD;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.SURFACE;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.pad;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The pieces of the Sky Island that do not need a fight: the shape (the same on the server and every client), the
 * template copied from the ground, the journal on disk, the slices of block edits, and the rules of the site.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SokoviaShapeTests {

    /** Flat natural ground at one height, as far as anyone looks: stone below, a layer of grass on top, air above. */
    private record Flat(int ground) implements BlockGetter {
        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return pos.getY() > ground ? Blocks.AIR.defaultBlockState() : pos.getY() == ground ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The shape

    /**
     * Two instances of the shape (the server's and a client's) with the same seed give the same rim, columns, depths
     * and rock, a different seed gives a different island, the rim stays within three blocks of the radius, and the
     * cone has no block that falls, flows or holds an inventory. At the real size the island has about 15,000 blocks of
     * ground and cone.
     */
    @GameTest(template = TestKit.ARENA, batch = "ultron_lift_shape")
    public static void theShapeIsTheSameOnBothSides(GameTestHelper helper) {
        Spec server = new Spec(20, 40, 5, 16, 14, 0x5EEDL);
        Spec client = new Spec(20, 40, 5, 16, 14, 0x5EEDL);
        Spec other = new Spec(20, 40, 5, 16, 14, 0x5EEEL);
        int differences = 0;
        int columns = 0;
        int expectedCone = 0;
        Set<String> rock = new HashSet<>();
        for (int dz = -23; dz <= 23; dz++) {
            for (int dx = -23; dx <= 23; dx++) {
                check(SokoviaShape.inside(server, dx, dz) == SokoviaShape.inside(client, dx, dz), "the same columns at " + dx + " " + dz);
                int depth = SokoviaShape.coneDepth(server, dx, dz);
                check(depth == SokoviaShape.coneDepth(client, dx, dz), "the same depth");
                check((depth > 0) == SokoviaShape.inside(server, dx, dz), "a column has a cone exactly if it is in the island");
                check(depth == 0 || depth >= 2, "a cone is at least two blocks deep");
                if (SokoviaShape.inside(server, dx, dz) != SokoviaShape.inside(other, dx, dz)) {
                    differences++;
                }
                if (depth > 0) {
                    columns++;
                    expectedCone += depth;
                    for (int k = 0; k < depth; k++) {
                        BlockState block = SokoviaShape.coneBlock(server, dx, dz, k, depth);
                        check(block == SokoviaShape.coneBlock(client, dx, dz, k, depth), "the same rock");
                        check(!(block.getBlock() instanceof FallingBlock) && block.getFluidState().isEmpty() && !block.hasBlockEntity(), "no falling, flowing or holding block: " + block);
                        check(SokoviaShape.isConeRock(block), "and it is one of the cone's rocks: " + block);
                        rock.add(block.getBlock().getDescriptionId());
                        check(!block.is(Blocks.DEEPSLATE) || k >= depth * 0.6, "deepslate only in the lowest 40 percent of a column");
                    }
                }
            }
        }
        check(differences > 20, "another seed gives another island: " + differences + " columns differ");
        check(rock.size() == 7, "all seven rocks are used: " + rock);
        for (int degrees = 0; degrees < 360; degrees++) {
            double rim = SokoviaShape.rim(server, Math.toRadians(degrees));
            check(rim >= 17.0 && rim <= 23.0, "the rim at " + degrees + " degrees is " + rim);
            check(rim == SokoviaShape.rim(client, Math.toRadians(degrees)), "the same rim");
        }
        check(Math.abs(SokoviaShape.rim(server, 0.0) - SokoviaShape.rim(server, Math.PI * 2.0)) < 1.0E-9, "and it closes");
        // The island over flat ground: five layers of skin in every column and the cone.
        SokoviaTemplate template = SokoviaTemplate.capture(new Flat(64), server, new BlockPos(0, 65, 0));
        check(template.size() == columns * 5 + expectedCone, "the template has the skin and the cone: " + template.size() + " of " + (columns * 5 + expectedCone));
        check(template.size() > 14_000 && template.size() < 17_000, "about 15,000 blocks: " + template.size());
        check(Math.abs(columns - Math.PI * 400.0) < Math.PI * 400.0 * 0.12, "about a disc of radius 20: " + columns + " columns");
        check(template.groundCells() == columns * 5, "five layers of ground in every column");
        helper.succeed();
    }

    /** The ease: 0 at the start, 1 at the end, no speed at either, never going back. */
    @GameTest(template = TestKit.ARENA, batch = "ultron_lift_shape")
    public static void theRiseIsAnEase(GameTestHelper helper) {
        check(SokoviaShape.offset(40, 160, 0) == 0.0 && SokoviaShape.offset(40, 160, 160) == 40.0, "from 0 to the height");
        check(SokoviaShape.offset(40, 160, -5) == 0.0 && SokoviaShape.offset(40, 160, 999) == 40.0, "and no further");
        double last = 0.0;
        double fastest = 0.0;
        for (int t = 1; t <= 160; t++) {
            double now = SokoviaShape.offset(40, 160, t);
            check(now >= last, "it never goes back");
            fastest = Math.max(fastest, now - last);
            last = now;
        }
        check(fastest > 0.4 && fastest < 0.5, "at the most about half a block a tick (5 blocks a second): " + fastest);
        check(SokoviaShape.offset(40, 160, 1) < 0.001 && 40.0 - SokoviaShape.offset(40, 160, 159) < 0.001, "slowly at the start and at the end");
        check(SokoviaShape.offset(40, 1, 1) == 40.0, "a move of one tick is a cut");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The template

    /**
     * The island is the ground as it is, column by column: what the island carries is copied, what it does not (a
     * chest, a torch, a wall of somebody's house, a pond) is left behind and the island has its own rock or air there;
     * nothing in it is a block entity or a fluid; and the ground the template read is not changed at all.
     */
    @GameTest(template = PAD, batch = "ultron_lift_template")
    public static void theTemplateCarriesTheGroundAndLeavesWhatWasBuiltBehind(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // On the grass: a chest, a torch beside it, a wall of planks three high, a pond and a flower.
        helper.setBlock(new BlockPos(21, 6, 21), Blocks.CHEST);
        helper.setBlock(new BlockPos(22, 6, 21), Blocks.TORCH);
        for (int y = 6; y <= 8; y++) {
            helper.setBlock(new BlockPos(24, y, 21), Blocks.OAK_PLANKS);
        }
        helper.setBlock(new BlockPos(21, 5, 24), Blocks.WATER);
        helper.setBlock(new BlockPos(21, 6, 24), Blocks.WATER);
        helper.setBlock(new BlockPos(19, 6, 21), Blocks.POPPY);
        Spec spec = new Spec(9, 20, 4, 8, 6, 77L);
        BlockPos centre = abs(helper, 21, 6, 21);
        SokoviaTemplate template = SokoviaTemplate.capture(level, spec, centre);
        SokoviaTemplate again = SokoviaTemplate.capture(level, spec, centre);
        check(template.size() == again.size() && template.minY() == again.minY(), "a second copy is the same");
        for (int i = 0; i < template.size(); i++) {
            check(template.state(i) == again.state(i) && template.x(i) == again.x(i) && template.y(i) == again.y(i), "and cell for cell");
            check(!template.state(i).hasBlockEntity() && template.state(i).getFluidState().isEmpty() && !template.state(i).isAir(), "no inventory, no water and no air in the island");
            if (i > 0) {
                check(template.y(i) >= template.y(i - 1), "the cells are kept bottom-up");
            }
        }
        check(cell(template, helper, 21, 6, 21) == null, "the chest is left behind (and with it the air over it)");
        check(cell(template, helper, 22, 6, 21) == null, "so is the torch");
        for (int y = 6; y <= 8; y++) {
            check(cell(template, helper, 24, y, 21) == null, "so is the wall of planks, at " + y);
        }
        BlockState underTheWall = cell(template, helper, 24, SURFACE, 21);
        check(underTheWall != null && underTheWall.is(Blocks.GRASS_BLOCK), "under the wall the island has the natural ground: " + underTheWall);
        check(cell(template, helper, 21, 5, 24) == null && cell(template, helper, 21, 6, 24) == null, "no water, and no stone in its place");
        check(cell(template, helper, 21, 4, 24) != null && cell(template, helper, 21, 4, 24).is(Blocks.DIRT), "the pond's bed is the ground there");
        check(cell(template, helper, 19, 6, 21) != null && cell(template, helper, 19, 6, 21).is(Blocks.POPPY), "a flower is carried");
        check(cell(template, helper, 15, SURFACE, 21) != null && cell(template, helper, 15, SURFACE, 21).is(Blocks.GRASS_BLOCK), "so is the grass");
        check(template.groundAt(0, 0) == abs(helper, 0, SURFACE, 0).getY(), "the ground of the middle column is the grass, under the chest");
        check(level.getBlockState(centre).is(Blocks.CHEST) && level.getBlockState(abs(helper, 24, 7, 21)).is(Blocks.OAK_PLANKS), "and the real ground still has its chest and its wall");
        helper.succeed();
    }

    private static BlockState cell(SokoviaTemplate template, GameTestHelper helper, int x, int y, int z) {
        BlockPos pos = abs(helper, x, y, z);
        for (int i = 0; i < template.size(); i++) {
            if (template.x(i) == pos.getX() && template.y(i) == pos.getY() && template.z(i) == pos.getZ()) {
                return template.state(i);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The journal

    /**
     * The journal is a few hundred bytes that say where the island is: it comes back from disc as it went, a temporary
     * file left by a crash is ignored, a truncated file or one of another version is set aside without anything being
     * done on its account, and writing it again replaces it whole.
     */
    @GameTest(template = TestKit.ARENA, batch = "ultron_lift_journal")
    public static void theJournalIsWrittenWholeAndReadBackOrRefused(GameTestHelper helper) throws IOException {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        SokoviaTestSupport.deleteJournals(server);
        f.onClose(() -> SokoviaTestSupport.deleteJournals(server));
        Spec spec = new Spec(20, 40, 5, 16, 14, -1234567890123L);
        UUID fight = UUID.randomUUID();
        UUID summoner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        SokoviaJournal.Header header = new SokoviaJournal.Header(fight, helper.getLevel().dimension(), new BlockPos(-100, 64, 200), spec, 50, 120,
                SokoviaJournal.PASTED, summoner, true, List.of(summoner, member));
        SokoviaJournal.write(server, header);
        Path file = SokoviaJournal.file(server, fight);
        check(Files.exists(file) && Files.size(file) < 600, "the note is on disc and small: " + Files.size(file) + " bytes");
        check(!Files.exists(file.resolveSibling(file.getFileName() + ".tmp")), "no temporary file is left");
        List<SokoviaJournal.Header> read = SokoviaJournal.readAll(server);
        check(read.equals(List.of(header)), "it reads back as it was written: " + read);
        // A second write replaces it whole.
        SokoviaJournal.write(server, header.withState(SokoviaJournal.CLEARING).withMembers(List.of(summoner)));
        SokoviaJournal.Header changed = SokoviaJournal.readAll(server).get(0);
        check(changed.state().equals(SokoviaJournal.CLEARING) && changed.members().equals(List.of(summoner)), "a second write replaces the first");
        // A temporary file from a crash is ignored.
        Files.write(SokoviaJournal.directory(server).resolve("leftover.nbt.tmp"), new byte[]{1, 2, 3});
        check(SokoviaJournal.readAll(server).size() == 1, "a temporary file is ignored");
        // A truncated note is refused and set aside.
        byte[] whole = Files.readAllBytes(file);
        Path broken = SokoviaJournal.directory(server).resolve(UUID.randomUUID() + ".nbt");
        Files.write(broken, Arrays.copyOf(whole, whole.length / 2));
        check(SokoviaJournal.readAll(server).size() == 1, "a truncated note is not read");
        check(!Files.exists(broken) && Files.exists(broken.resolveSibling(broken.getFileName() + ".corrupt")), "and is set aside");
        // So is a note of a version this one does not know.
        CompoundTag tag = header.toTag();
        tag.putInt("v", 99);
        Path future = SokoviaJournal.directory(server).resolve(UUID.randomUUID() + ".nbt");
        NbtIo.writeCompressed(tag, future);
        check(SokoviaJournal.readAll(server).size() == 1 && !Files.exists(future), "a note of another version is set aside");
        SokoviaJournal.delete(server, fight);
        check(!Files.exists(file) && SokoviaJournal.readAll(server).isEmpty(), "deleting it deletes it");
        SokoviaJournal.delete(server, fight); // (and again is no error)
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The slices

    /**
     * The island is placed bottom-up and taken down top-down, never more than the budget in a tick, and a slice that
     * meets a chunk that is not loaded waits and goes on when it is.
     */
    @GameTest(template = PAD, batch = "ultron_lift_slices", timeoutTicks = 300)
    public static void theSlicesKeepTheBudgetTheOrderAndWaitForChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Spec spec = new Spec(9, 20, 4, 8, 6, 5L);
        BlockPos centre = abs(helper, 21, 6, 21);
        SokoviaTemplate template = SokoviaTemplate.capture(level, spec, centre);
        int[] loadedAfter = {3};
        int[] slice = {0};
        SokoviaJob paste = SokoviaJob.paste(level, template, LIFT, (x, z) -> slice[0] >= loadedAfter[0]);
        int budget = 300;
        // Three slices in which a chunk is not loaded: nothing is changed.
        for (; slice[0] < 3; slice[0]++) {
            check(!paste.step(budget) && paste.isWaiting() && paste.edits() == 0, "a slice that meets an unloaded chunk waits");
        }
        int lastY = Integer.MIN_VALUE;
        while (!paste.step(budget)) {
            slice[0]++;
            check(paste.mostInATick() <= budget, "no slice placed more than the budget: " + paste.mostInATick());
            int highest = Integer.MIN_VALUE;
            for (int i = 0; i < template.size(); i++) {
                if (level.getBlockState(new BlockPos(template.x(i), template.y(i) + LIFT, template.z(i))) == template.state(i)) {
                    highest = Math.max(highest, template.y(i));
                }
            }
            for (int i = 0; i < template.size(); i++) {
                if (template.y(i) < highest - 1) {
                    check(level.getBlockState(new BlockPos(template.x(i), template.y(i) + LIFT, template.z(i))) == template.state(i), "bottom-up: the layer " + template.y(i)
                            + " is complete when the placing is at " + highest);
                }
            }
            lastY = highest;
        }
        check(paste.isDone() && paste.edits() == template.size() && paste.ticks() > template.size() / budget, "the whole island was placed in slices: " + paste.edits() + " blocks, " + paste.ticks() + " slices");
        check(lastY > Integer.MIN_VALUE, "(the placing took more than one slice)");
        for (int i = 0; i < template.size(); i++) {
            check(level.getBlockState(new BlockPos(template.x(i), template.y(i) + LIFT, template.z(i))) == template.state(i), "block " + i + " is where it belongs");
        }
        // Taking it down is top-down.
        SokoviaJob clear = SokoviaJob.clear(level, spec, centre, template.minY() + LIFT, template.maxY() + LIFT);
        int[] counts = {0};
        while (!clear.step(budget)) {
            counts[0]++;
            check(clear.mostInATick() <= budget, "no slice cleared more than the budget");
            int lowestLeft = Integer.MAX_VALUE;
            int highestLeft = Integer.MIN_VALUE;
            for (int i = 0; i < template.size(); i++) {
                if (!level.getBlockState(new BlockPos(template.x(i), template.y(i) + LIFT, template.z(i))).isAir()) {
                    lowestLeft = Math.min(lowestLeft, template.y(i));
                    highestLeft = Math.max(highestLeft, template.y(i));
                }
            }
            for (int i = 0; i < template.size(); i++) {
                if (template.y(i) > highestLeft + 1) {
                    check(level.getBlockState(new BlockPos(template.x(i), template.y(i) + LIFT, template.z(i))).isAir(), "top-down: the layer " + template.y(i) + " is gone when it is at " + highestLeft);
                }
            }
        }
        check(counts[0] >= 2, "taking it down took more than one slice");
        for (int i = 0; i < template.size(); i++) {
            check(level.getBlockState(new BlockPos(template.x(i), template.y(i) + LIFT, template.z(i))).isAir(), "block " + i + " is gone");
        }
        // The ground was never part of it.
        check(level.getBlockState(abs(helper, 21, SURFACE, 21)).is(Blocks.GRASS_BLOCK), "the ground is as it was");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The rules of the site

    private static String key(@Nullable Component message) {
        return message != null && message.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : null;
    }

    /**
     * Each rule of the Sky Island against a pad that breaks it in one way: an empty sky passes; a block just over the
     * room the island needs (above the island, and in the margin round it), an armour stand in the cylinder, and a
     * claim that forbids building up there are each refused with their message; nothing built on the ground itself is.
     */
    @GameTest(template = PAD, batch = "ultron_lift_site", timeoutTicks = 100)
    public static void eachRuleOfTheSiteRefusesWhatBreaksIt(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerLevel level = f.level();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        Spec spec = Spec.ofConfig(31L);
        BlockPos centre = abs(helper, 21, 6, 21);
        check(SokoviaValidator.refusal(level, spec, centre, starter) == null, "an empty sky passes");
        // Something built on the ground is no reason: a house is left alone.
        for (int y = 6; y <= 8; y++) {
            helper.setBlock(new BlockPos(23, y, 21), Blocks.OAK_PLANKS);
        }
        helper.setBlock(new BlockPos(20, 6, 22), Blocks.CHEST);
        check(SokoviaValidator.refusal(level, spec, centre, starter) == null, "a house and a chest on the ground do not matter");
        // A block in the margin of the room, over the island: refused.
        BlockPos high = abs(helper, 21, SURFACE + LIFT + 6, 21);
        level.setBlock(high, Blocks.STONE.defaultBlockState(), 3);
        check("message.enhanced.ultron.refuse.sky".equals(key(SokoviaValidator.refusal(level, spec, centre, starter))), "a block over the island: " + key(SokoviaValidator.refusal(level, spec, centre, starter)));
        level.setBlock(high, Blocks.AIR.defaultBlockState(), 3);
        // One just outside the margin passes (the room is the rim and six blocks).
        BlockPos beyond = abs(helper, 21 + 12 + 8, SURFACE + LIFT + 2, 21);
        level.setBlock(beyond, Blocks.STONE.defaultBlockState(), 3);
        check(SokoviaValidator.refusal(level, spec, centre, starter) == null, "a block past the margin passes");
        level.setBlock(beyond, Blocks.AIR.defaultBlockState(), 3);
        // Something hanging in the cylinder: refused, with its name and place; the warning does not ask (no starter).
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        Vec3 at = helper.absoluteVec(new Vec3(25.5, 6.0, 21.5));
        stand.moveTo(at.x, at.y, at.z);
        level.addFreshEntity(stand);
        Component hanging = SokoviaValidator.refusal(level, spec, centre, starter);
        check("message.enhanced.ultron.refuse.entity".equals(key(hanging)), "an armour stand in the way: " + key(hanging));
        TranslatableContents contents = (TranslatableContents) hanging.getContents();
        check(contents.getArgs().length == 2 && contents.getArgs()[1].toString().contains(String.valueOf(stand.getBlockX())), "and it says where");
        check(SokoviaValidator.refusal(level, spec, centre, null) == null, "the lift's warning does not ask about hanging things");
        stand.discard();
        // A claim that forbids building up in the sky.
        Consumer<BlockEvent.BreakEvent> claim = event -> {
            if (event.getPos().getY() >= abs(helper, 0, SURFACE + LIFT, 0).getY()) {
                event.setCanceled(true);
            }
        };
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(claim);
        f.onClose(() -> bus.unregister(claim));
        check("message.enhanced.ultron.refuse.protected".equals(key(SokoviaValidator.refusal(level, spec, centre, starter))), "a claim over the sky");
        check(SokoviaValidator.refusal(level, spec, centre, null) == null, "(the warning does not ask the claim again)");
        bus.unregister(claim);
        check(SokoviaValidator.refusal(level, spec, centre, starter) == null, "and it passes when the claim is lifted");
        helper.succeed();
    }
}

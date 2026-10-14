package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.BuildSession;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellPlanner;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.UndoFiles;
import com.kolodkingal.enhanced.build.UndoHistory;
import com.kolodkingal.enhanced.build.UndoStep;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * What the Build Magic tests share (lane BS): a little world of blocks kept in a map, recipes written the way a client
 * would send them, and a pocket the tests fill by hand; for the world tests, builders in build mode whose jobs the test
 * steps itself, casts through the payload's entry, and what the server sent the builder.
 */
final class BuildTestSupport {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** A world of blocks in a map, air everywhere else; no block entities, no chunks to load. */
    static final class World implements BlockGetter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();

        World set(BlockPos pos, BlockState state) {
            if (state.isAir()) {
                blocks.remove(pos);
            } else {
                blocks.put(pos.immutable(), state);
            }
            return this;
        }

        World set(int x, int y, int z, BlockState state) {
            return set(new BlockPos(x, y, z), state);
        }

        /** Every cell of the box. */
        World fill(BoundingBox box, BlockState state) {
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                set(pos, state);
            }
            return this;
        }

        /** Writes every cell the plan changes, as a job would (with no rule of the world in the way). */
        World build(SpellPlanner.Plan plan) {
            List<SpellPlanner.Cell> cells = new ArrayList<>();
            plan.walk(this).forEachRemaining(cells::add);
            for (SpellPlanner.Cell cell : cells) {
                set(cell.pos(), cell.state());
            }
            return this;
        }

        int count(BlockState state) {
            return (int) blocks.values().stream().filter(state::equals).count();
        }

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, AIR);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
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

    /** A pocket of copied cells, filled by hand; a cell never set is absent. */
    static final class Pocket implements SpellPlanner.Source {
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final BlockState[] cells;

        Pocket(int sizeX, int sizeY, int sizeZ) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.cells = new BlockState[sizeX * sizeY * sizeZ];
        }

        Pocket set(int x, int y, int z, BlockState state) {
            cells[(y * sizeZ + z) * sizeX + x] = state;
            return this;
        }

        @Override
        public int sizeX() {
            return sizeX;
        }

        @Override
        public int sizeY() {
            return sizeY;
        }

        @Override
        public int sizeZ() {
            return sizeZ;
        }

        @Override
        public @Nullable BlockState state(int x, int y, int z) {
            if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
                return null;
            }
            return cells[(y * sizeZ + z) * sizeX + x];
        }
    }

    /** A recipe, written field by field; every field starts as a client leaves it when it does not use it. */
    static final class Recipe {
        private final Spell spell;
        private SpellRecipe.Op op = SpellRecipe.Op.CAST;
        private int style;
        private int region = SpellRecipe.NO_REGION;
        private BlockPos a = BlockPos.ZERO;
        private BlockPos b = BlockPos.ZERO;
        private BlockPos anchor = BlockPos.ZERO;
        private int face = SpellRecipe.NO_FACE;
        private int size;
        private int size2;
        private int turn;
        private int flip = SpellRecipe.NO_FLIP;
        private int flags;
        private long seed;
        private List<BlockPos> seeds = List.of();
        private int material = SpellRecipe.HAND;
        private int stroke;

        Recipe(Spell spell) {
            this.spell = spell;
        }

        Recipe op(SpellRecipe.Op op) {
            this.op = op;
            return this;
        }

        Recipe style(Enum<?> style) {
            this.style = style.ordinal();
            return this;
        }

        Recipe box(BlockPos a, BlockPos b) {
            this.region = SpellRecipe.BOX;
            this.a = a;
            this.b = b;
            return this;
        }

        Recipe box(BoundingBox box) {
            return box(new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
        }

        /** The region is the selected thing the test hands the planner in its setup. */
        Recipe mask() {
            this.region = SpellRecipe.MASK;
            return this;
        }

        /** The region is the thing the magic selection floods from these seeds (the server floods it itself). */
        Recipe mask(BlockPos... seeds) {
            this.region = SpellRecipe.MASK;
            this.seeds = List.of(seeds);
            this.a = seeds[0];
            this.b = seeds[0];
            return this;
        }

        Recipe at(BlockPos anchor, @Nullable Direction face) {
            this.anchor = anchor;
            this.face = face == null ? SpellRecipe.NO_FACE : face.get3DDataValue();
            return this;
        }

        Recipe size(int size) {
            this.size = size;
            return this;
        }

        Recipe size2(int size2) {
            this.size2 = size2;
            return this;
        }

        Recipe turn(int turn) {
            this.turn = turn;
            return this;
        }

        Recipe flip(int flip) {
            this.flip = flip;
            return this;
        }

        Recipe hollow(boolean hollow) {
            this.flags = hollow ? flags | SpellRecipe.HOLLOW : flags & ~SpellRecipe.HOLLOW;
            return this;
        }

        Recipe seed(long seed) {
            this.seed = seed;
            return this;
        }

        /** The hotbar's mix (D-B 5.3) instead of one block. */
        Recipe mix() {
            this.flags |= SpellRecipe.MIX;
            return this;
        }

        /** A planted shape (its height is the one sent). */
        Recipe planted(boolean planted) {
            this.flags = planted ? flags | SpellRecipe.PLANTED : flags & ~SpellRecipe.PLANTED;
            return this;
        }

        /** The material: {@link SpellRecipe#HAND} or {@link SpellRecipe#PICKED}. */
        Recipe material(int material) {
            this.material = material;
            return this;
        }

        /** A dab of the Paint stroke {@code stroke}. */
        Recipe dab(int stroke) {
            this.op = SpellRecipe.Op.DAB;
            this.stroke = stroke;
            return this;
        }

        SpellRecipe build() {
            return new SpellRecipe(spell, op, style, region, a, b, seeds, anchor, face, size, size2, turn, flip, flags, material, seed, stroke);
        }
    }

    static Recipe recipe(Spell spell) {
        return new Recipe(spell);
    }

    /** Every cell the plan changes in the world, in its order. */
    static List<SpellPlanner.Cell> cells(SpellPlanner.Plan plan, BlockGetter world) {
        List<SpellPlanner.Cell> cells = new ArrayList<>();
        plan.walk(world).forEachRemaining(cells::add);
        return cells;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The world tests (lane BS, step 2): builders, casts, the scheduler stepped by hand, what the client was sent

    /** A budget with no limit worth the name: a job finishes in one step. */
    static final BuildScheduler.Budget ENDLESS = BuildScheduler.Budget.of(1_000_000, 1 << 20, 1);

    private static int token;

    /**
     * A builder of {@code hero} standing at {@code x, z} on the arena floor, in build mode, holding {@code block}
     * (null: nothing); her jobs kept out of the server's ticks, so the test steps them.
     */
    static ServerPlayer builder(TestKit.Fixture f, Power hero, double x, double z, @Nullable Block block) {
        ServerPlayer player = f.player(x, TestKit.FLOOR, z, hero);
        if (block != null) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block));
        }
        BuildMagic.setMode(player, true);
        BuildScheduler.manual(player.getUUID(), null);
        return player;
    }

    /** The arena's cell {@code x, y, z} in the world. */
    static BlockPos abs(TestKit.Fixture f, int x, int y, int z) {
        return f.helper().absolutePos(new BlockPos(x, y, z));
    }

    /** The arena's box {@code from}-{@code to} in the world. */
    static BoundingBox abs(TestKit.Fixture f, BoundingBox box) {
        return BoundingBox.fromCorners(abs(f, box.minX(), box.minY(), box.minZ()), abs(f, box.maxX(), box.maxY(), box.maxZ()));
    }

    /** A box spell of the arena's box {@code box} (relative), its anchor its low corner looked at from above. */
    static SpellRecipe boxSpell(TestKit.Fixture f, Spell spell, BoundingBox box) {
        BoundingBox world = abs(f, box);
        BlockPos low = new BlockPos(world.minX(), world.minY(), world.minZ());
        return recipe(spell).box(world).at(low, Direction.UP).build();
    }

    /** Casts {@code recipe} as the payload would. */
    static void cast(ServerPlayer player, SpellRecipe recipe) {
        BuildMagic.cast(player, recipe, ++token, 0);
    }

    /** Casts {@code recipe} as the payload would, with these flags (a preflight, a long hold). */
    static void cast(ServerPlayer player, SpellRecipe recipe, int flags) {
        BuildMagic.cast(player, recipe, ++token, flags);
    }

    /** The job at work for {@code player}, or null. */
    static @Nullable BuildJob working(ServerPlayer player) {
        return BuildScheduler.working(player.getUUID());
    }

    /** Steps the jobs of the players with {@code budget} until none of them works (at most 10,000 steps). */
    static int finish(BuildScheduler.Budget budget, UUID... players) {
        int steps = 0;
        while (Arrays.stream(players).anyMatch(id -> BuildScheduler.working(id) != null)) {
            TestKit.check(++steps <= 10_000, "the spells end");
            if (UndoFiles.active()) {
                UndoFiles.drain(); // (a spell waits for its records to be written: give the writer its turn)
            }
            BuildScheduler.step(List.of(players), budget);
        }
        return steps;
    }

    /** Finishes the jobs of the players at once. */
    static void finish(ServerPlayer... players) {
        finish(ENDLESS, Arrays.stream(players).map(ServerPlayer::getUUID).toArray(UUID[]::new));
    }

    /** How many cells of the arena's box (relative) hold {@code block}. */
    static int count(TestKit.Fixture f, BoundingBox box, Block block) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, box.minX(), box.minY(), box.minZ()), abs(f, box.maxX(), box.maxY(), box.maxZ()))) {
            if (f.level().getBlockState(pos).is(block)) {
                n++;
            }
        }
        return n;
    }

    /** Fills the arena's box (relative) with {@code block} at once, as the world was before the test's spell. */
    static void set(TestKit.Fixture f, BoundingBox box, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, box.minX(), box.minY(), box.minZ()), abs(f, box.maxX(), box.maxY(), box.maxZ()))) {
            f.level().setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The undo history's steps of {@code player}. */
    static List<UndoStep> steps(ServerPlayer player) {
        return UndoHistory.of(player.getUUID()).steps();
    }

    /** Adds a listener for the test's length (a claim mod, a counter of events). */
    static <T extends Event> void listen(TestKit.Fixture f, Class<T> type, Consumer<T> listener) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, listener);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(listener));
    }

    /** Whether {@code player} is in build mode, as the server and everyone who sees her know it. */
    static boolean building(ServerPlayer player) {
        BuildSession session = BuildSession.existing(player.getUUID());
        return session != null && session.mode() && player.getExistingData(ModAttachments.BUILD_VIEW).map(BuildView::active).orElse(false);
    }

    /** What the server sent a player since the last look: the verdicts, the effects, the action-bar and chat lines. */
    static final class Seen {
        final List<BuildVerdictPayload> verdicts = new ArrayList<>();
        final List<BuildFxPayload> fx = new ArrayList<>();
        final List<Component> bar = new ArrayList<>();
        final List<Component> chat = new ArrayList<>();

        /** Reads (and so empties) the player's packets. */
        static Seen of(ServerPlayer player) {
            Seen seen = new Seen();
            for (Object packet : TestKit.sentPackets(player)) {
                if (packet instanceof ClientboundCustomPayloadPacket custom) {
                    if (custom.payload() instanceof BuildVerdictPayload verdict) {
                        seen.verdicts.add(verdict);
                    } else if (custom.payload() instanceof BuildFxPayload moment) {
                        seen.fx.add(moment);
                    }
                } else if (packet instanceof ClientboundSystemChatPacket line) {
                    (line.overlay() ? seen.bar : seen.chat).add(line.content());
                }
            }
            return seen;
        }

        /** The last verdict, or null. */
        @Nullable
        BuildVerdictPayload verdict() {
            return verdicts.isEmpty() ? null : verdicts.get(verdicts.size() - 1);
        }

        /** The code of the last verdict, or -1. */
        int code() {
            BuildVerdictPayload verdict = verdict();
            return verdict == null ? -1 : verdict.code();
        }

        /** The line with this key the player was told (action bar or chat), or null. */
        @Nullable
        Component line(String key) {
            for (List<Component> lines : List.of(bar, chat)) {
                for (Component line : lines) {
                    if (key(line).equals(key)) {
                        return line;
                    }
                }
            }
            return null;
        }

        /** Whether the player was told the line with this key. */
        boolean said(String key) {
            return line(key) != null;
        }

        /** The key of the last action-bar line, or "". */
        String lastBar() {
            return bar.isEmpty() ? "" : key(bar.get(bar.size() - 1));
        }

        /** The {@code i}th argument of the line with this key, as a string ("" if there is none). */
        String arg(String key, int i) {
            Component line = line(key);
            if (line != null && line.getContents() instanceof TranslatableContents translatable && translatable.getArgs().length > i) {
                Object arg = translatable.getArgs()[i];
                return arg instanceof Component component ? component.getString() : String.valueOf(arg);
            }
            return "";
        }

        static String key(Component line) {
            return line.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
        }

        @Override
        public String toString() {
            return "verdicts " + verdicts.stream().map(v -> v.code() + "/" + v.changed()).toList() + ", lines "
                    + bar.stream().map(Seen::key).toList() + chat.stream().map(Seen::key).toList();
        }
    }

    private BuildTestSupport() {
    }
}

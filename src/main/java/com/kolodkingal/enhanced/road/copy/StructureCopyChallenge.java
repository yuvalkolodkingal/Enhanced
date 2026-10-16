package com.kolodkingal.enhanced.road.copy;

import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.road.RoadBuild;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadFx;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.RoadPalette;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.RoadVoice;
import com.kolodkingal.enhanced.road.WitchesRoad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trial 2, Copy the Structure: a model stands on a platform next to an empty plot of the same size and the same way round, three
 * blocks apart. The player is given the blocks it is made of (the exact kinds, a good deal of each, and a supply chest that fills
 * itself up again), builds a copy by hand on the plot, and wins when it looks exactly like the model. A Check button counts what is
 * right and marks what is missing, wrong or extra with glowing outlines; a Hint button shows ghost blocks where blocks are missing;
 * neither costs anything, and nothing is lost by failing: there is no way to fail. The only limit is a long, generous time after
 * which the room is taken down and the player sent home (so that no room stands for ever).
 *
 * <p>The player can place and break blocks only inside the plot ({@link #mayPlace}); breaking is instant there and gives the
 * block back. Everything else is the Road's: the room is built in a private cell, taken down after, and the gate opens when he wins.
 */
public final class StructureCopyChallenge implements RoadChallenge {
    public static final String ID = "structure_copy";

    /** The Road's voice at the great moments of this trial ({@code message.enhanced.road.voice.<event>.<1..3>}). */
    public static final String VOICE_START = "copy_start";
    public static final String VOICE_TRY = "copy_try";
    public static final String VOICE_CLOSE = "copy_close";
    public static final String VOICE_HINT = "copy_hint";

    /** The ticks between two presses of Check, and of Hint (a press in between is ignored). */
    private static final int CHECK_COOLDOWN = 10;
    private static final int HINT_COOLDOWN = 20;
    /** How much of the model has to be right for the Road to say that it is close. */
    private static final int CLOSE_PERCENT = 85;

    /** The live rooms, by cell. */
    private static final Map<Integer, CopySession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean resumable() {
        // What the player has built is not kept: he starts the room again from the beginning.
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The model

    /** The size of the model for a player's seed, from the config (0 there: one that depends on the seed). */
    public static int tierFor(long seed) {
        int tier = WitchesRoadConfig.STRUCTURE_COPY_TIER.get();
        return tier == 0 ? 1 + (int) Math.floorMod(seed >>> 17, 3L) : tier;
    }

    /** The model a player's seed gives. */
    public static StructureModel modelFor(long seed) {
        return StructureLibrary.generate(seed, tierFor(seed));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    @Override
    public RoadLayout build(RoadBuild build) {
        StructureModel model = modelFor(build.seed());
        RoadLayout layout = draw(build.canvas(), model, WitchesRoadConfig.FALL_DEPTH.get());
        SESSIONS.put(build.canvas().cell(), new CopySession(model, build.canvas().origin()));
        return layout;
    }

    /** Draws the room for a model on the canvas and says where everything is. */
    public static RoadLayout draw(RoadCanvas canvas, StructureModel model, int fallDepth) {
        CopyGeometry g = CopyGeometry.of(model);
        int w = g.w();
        int d = g.d();
        // The platform, with roots and soul lanterns hanging under it.
        for (int x = g.floorMinX(); x <= g.floorMaxX(); x++) {
            for (int z = g.floorMinZ(); z <= g.floorMaxZ(); z++) {
                boolean edge = x == g.floorMinX() || x == g.floorMaxX() || z == g.floorMinZ() || z == g.floorMaxZ();
                canvas.set(x, 0, z, edge ? RoadPalette.TRIM : RoadPalette.PLANKS);
                canvas.set(x, -1, z, edge ? RoadPalette.LOG : RoadPalette.PLANKS);
            }
        }
        for (int x = g.floorMinX() + 2; x < g.floorMaxX(); x += 4) {
            for (int z = g.floorMinZ() + 2; z < g.floorMaxZ(); z += 4) {
                int h = Math.floorMod(x * 31 + z * 17, 5);
                canvas.set(x, -2, z, h == 0 ? RoadPalette.HANGING_LANTERN : h < 3 ? RoadPalette.ROOTS : RoadPalette.LICHEN);
            }
        }
        // The floor of the model and of the plot, each with an outline one block wide round it.
        footprint(canvas, g, g.modelX0(), RoadPalette.AMETHYST);
        footprint(canvas, g, g.plotX0(), Blocks.SEA_LANTERN.defaultBlockState());
        // The model.
        for (int y = 0; y < model.sizeY(); y++) {
            for (int z = 0; z < model.sizeZ(); z++) {
                for (int x = 0; x < model.sizeX(); x++) {
                    BlockState state = model.at(x, y, z);
                    if (!state.isAir()) {
                        canvas.set(g.modelX0() + x, 1 + y, z, state);
                    }
                }
            }
        }
        // A soul lantern on a post at each corner of both outlines.
        for (int x0 : new int[]{g.modelX0(), g.plotX0()}) {
            for (int x : new int[]{x0 - 1, x0 + w}) {
                for (int z : new int[]{-1, d}) {
                    canvas.set(x, 1, z, RoadPalette.FENCE);
                    canvas.set(x, 2, z, RoadPalette.FENCE);
                    canvas.set(x, 3, z, RoadPalette.LANTERN);
                }
            }
        }
        // The console in front of the aisle: Check on the left, the supply chest in the middle, Hint on the right.
        int cz = g.consoleZ();
        for (int x : new int[]{-1, 1}) {
            canvas.set(x, 1, cz, RoadPalette.AMETHYST);
            canvas.set(x, 2, cz, RoadPalette.AMETHYST);
        }
        canvas.set(0, 1, cz, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        canvas.set(-1, 1, cz + 1, button(Blocks.WARPED_BUTTON));
        canvas.set(1, 1, cz + 1, button(Blocks.CHERRY_BUTTON));
        // The start platform at the front, the gate at the far end of the aisle.
        RoadPalette.startPlatform(canvas, 0, 0, g.startZ());
        RoadPalette.gate(canvas, 0, 1, g.gateZ(), Direction.Axis.X);
        // The plot is the player's: all of it is cleared at the end.
        canvas.reserve(g.plotX0(), 1, 0, g.plotX0() + w - 1, g.plotHeight(), d - 1);

        BlockPos origin = canvas.origin();
        Vec3 start = new Vec3(origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + g.startZ() + 0.5);
        RoadLayout.Checkpoint checkpoint = new RoadLayout.Checkpoint(start, 180.0F, null, origin.getY() - 2 - fallDepth + 1.0);
        return new RoadLayout(List.of(checkpoint), null, new RoadLayout.Gate(canvas.at(0, 1, g.gateZ()), Direction.Axis.X),
                RoadPalette.startExitDoor(canvas, 0, 0, g.startZ()), 0);
    }

    private static BlockState button(Block block) {
        return block.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.SOUTH);
    }

    /** A chequered floor with a coloured outline one block outside it, for the footprint that starts at {@code x0}. */
    private static void footprint(RoadCanvas canvas, CopyGeometry g, int x0, BlockState outline) {
        for (int x = x0 - 1; x <= x0 + g.w(); x++) {
            for (int z = -1; z <= g.d(); z++) {
                boolean ring = x == x0 - 1 || x == x0 + g.w() || z == -1 || z == g.d();
                canvas.set(x, 0, z, ring ? outline : (x - x0 + z) % 2 == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                        : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The room

    private static CopySession session(RoadTrial trial) {
        return SESSIONS.computeIfAbsent(trial.cell(), cell -> new CopySession(modelFor(trial.seed()), trial.canvas().origin()));
    }

    /** The model of a player's trial. */
    public static StructureModel model(RoadTrial trial) {
        return session(trial).model;
    }

    /** The corner of the plot (its lowest north-west block) in the Road dimension. */
    public static BlockPos plotCorner(RoadTrial trial) {
        return session(trial).plotCorner();
    }

    /** The corner of the model. */
    public static BlockPos modelCorner(RoadTrial trial) {
        return session(trial).modelCorner();
    }

    /** Where the Check button is. */
    public static BlockPos checkButton(RoadTrial trial) {
        return session(trial).checkButton();
    }

    /** Where the Hint button is. */
    public static BlockPos hintButton(RoadTrial trial) {
        return session(trial).hintButton();
    }

    /** Where the supply chest is. */
    public static BlockPos supplyChest(RoadTrial trial) {
        return session(trial).chest();
    }

    @Override
    public void onStart(RoadTrial trial, ServerPlayer player) {
        CopySession s = session(trial);
        ServerLevel level = trial.level();
        settle(level, trial.canvas(), s.modelCorner(), s.model);
        StructureSupply.give(player, s.model, ID);
        fillChest(level, s);
        // The words floating over the room.
        CopyGeometry g = s.geo;
        BlockPos o = s.origin;
        trial.track(CopyDisplays.text(level, o.getX() + g.modelX0() + g.w() / 2.0, o.getY() + 1 + g.h() + 1.4, o.getZ() + g.d() / 2.0,
                titled("message.enhanced.road.copy.model_title", null), 1.5F));
        s.plotText = trial.track(CopyDisplays.text(level, o.getX() + g.plotX0() + g.w() / 2.0, o.getY() + 1 + g.h() + 1.4, o.getZ() + g.d() / 2.0,
                titled("message.enhanced.road.copy.plot_title", Component.translatable("message.enhanced.road.copy.plot_hint")), 1.5F));
        trial.track(CopyDisplays.text(level, o.getX() - 0.5, o.getY() + 3.55, o.getZ() + g.consoleZ() + 1.5,
                Component.translatable("message.enhanced.road.copy.check_label").withStyle(style -> style.withColor(WitchesRoad.LIGHT_CORE)), 1.2F));
        trial.track(CopyDisplays.text(level, o.getX() + 1.5, o.getY() + 3.55, o.getZ() + g.consoleZ() + 1.5,
                Component.translatable("message.enhanced.road.copy.hint_label").withStyle(style -> style.withColor(WitchesRoad.LIGHT_CORE)), 1.2F));
        trial.track(CopyDisplays.text(level, o.getX() + 0.5, o.getY() + 2.5, o.getZ() + g.consoleZ() + 1.3,
                Component.translatable("message.enhanced.road.copy.supply_label").withStyle(style -> style.withColor(WitchesRoad.LIGHT_CORE)), 1.0F));
        player.displayClientMessage(Component.translatable("message.enhanced.road.copy.intro").withStyle(style -> style.withColor(WitchesRoad.LIGHT)), false);
        RoadVoice.say(player, VOICE_START, s.model.solidCount());
    }

    /** The model's stairs, fences, walls and panes take the corners and joints the game gives them (the canvas places blocks without telling neighbours). */
    public static void settle(ServerLevel level, RoadCanvas canvas, BlockPos modelCorner, StructureModel model) {
        for (int y = 0; y < model.sizeY(); y++) {
            for (int z = 0; z < model.sizeZ(); z++) {
                for (int x = 0; x < model.sizeX(); x++) {
                    Block block = model.at(x, y, z).getBlock();
                    if (block instanceof StairBlock || block instanceof FenceBlock || block instanceof WallBlock || block instanceof IronBarsBlock) {
                        BlockPos pos = modelCorner.offset(x, y, z);
                        BlockState now = level.getBlockState(pos);
                        BlockState fixed = Block.updateFromNeighbourShapes(now, level, pos);
                        if (fixed != now) {
                            canvas.live(level, pos, fixed);
                        }
                    }
                }
            }
        }
    }

    private static MutableComponent titled(String key, Component line) {
        MutableComponent text = Component.translatable(key).withStyle(style -> style.withColor(WitchesRoad.LIGHT_CORE).withBold(true));
        if (line != null) {
            text.append(Component.literal("\n")).append(line.copy().withStyle(style -> style.withColor(WitchesRoad.LIGHT).withBold(false)));
        }
        return text;
    }

    @Override
    public void tick(RoadTrial trial, ServerPlayer player) {
        CopySession s = session(trial);
        ServerLevel level = trial.level();
        long now = level.getGameTime();
        if (!s.ghosts.isEmpty() && now >= s.ghostsUntil) {
            s.clearGhosts();
        }
        if (!s.marks.isEmpty()) {
            if (now >= s.marksUntil) {
                s.marks.clear();
            } else if (now % 6 == 0) {
                pulse(level, player, s);
            }
        }
        if (now % 20 != 0) {
            return;
        }
        fillChest(level, s);
        if (WitchesRoadConfig.STRUCTURE_COPY_AUTO_CHECK.get()) {
            StructureCompare.Result result = s.compare(level);
            if (result.perfect()) {
                finish(trial, player, s, result);
                return;
            }
        }
        long limit = WitchesRoadConfig.STRUCTURE_COPY_TIME_LIMIT_SECONDS.get() * 20L;
        if (limit > 0 && trial.elapsed() > limit) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.copy.time_up").withStyle(style -> style.withColor(WitchesRoad.LIGHT)), false);
            RoadTrials.leave(player, End.LEFT);
        }
    }

    @Override
    public boolean mayPlace(RoadTrial trial, ServerPlayer player, BlockPos pos) {
        return inPlot(trial, pos);
    }

    @Override
    public boolean mayBreak(RoadTrial trial, ServerPlayer player, BlockPos pos) {
        return inPlot(trial, pos);
    }

    private static boolean inPlot(RoadTrial trial, BlockPos pos) {
        if (trial.phase() != RoadTrial.Phase.ACTIVE) {
            return false;
        }
        CopySession s = session(trial);
        return s.geo.inPlot(pos.getX() - s.origin.getX(), pos.getY() - s.origin.getY(), pos.getZ() - s.origin.getZ());
    }

    /** Tells the player where he may build (when he tried to build somewhere else). */
    static void remindPlot(ServerPlayer player) {
        if (player.tickCount % 10 == 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.copy.outside").withStyle(style -> style.withColor(WitchesRoad.LIGHT)), true);
        }
    }

    @Override
    public void onEnd(RoadTrial trial, End reason) {
        CopySession s = SESSIONS.remove(trial.cell());
        if (s != null) {
            s.clearGhosts();
            // The chest is emptied before the room is taken down, or its contents would spill on the floor of the next room built here.
            if (trial.level().getBlockEntity(s.chest()) instanceof ChestBlockEntity chest) {
                chest.clearContent();
            }
        }
        ServerPlayer player = trial.player();
        if (player != null) {
            StructureSupply.restore(player, ID);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The buttons

    /** The player pressed something in the room (a block at {@code pos}): the Check or the Hint button does its work. @return whether it was one of them */
    public static boolean use(RoadTrial trial, ServerPlayer player, BlockPos pos) {
        if (trial.phase() != RoadTrial.Phase.ACTIVE || !(trial.challenge() instanceof StructureCopyChallenge)) {
            return false;
        }
        CopySession s = session(trial);
        if (pos.equals(s.checkButton())) {
            check(trial, player);
            return true;
        }
        if (pos.equals(s.hintButton())) {
            hint(trial, player);
            return true;
        }
        return false;
    }

    /** Check: counts what is right, marks what is not, and wins if the plot looks exactly like the model. */
    public static void check(RoadTrial trial, ServerPlayer player) {
        if (trial.phase() != RoadTrial.Phase.ACTIVE) {
            return;
        }
        CopySession s = session(trial);
        ServerLevel level = trial.level();
        long now = level.getGameTime();
        if (now - s.lastCheck < CHECK_COOLDOWN) {
            return;
        }
        s.lastCheck = now;
        StructureCompare.Result result = s.compare(level);
        if (result.perfect()) {
            finish(trial, player, s, result);
            return;
        }
        s.mark(result, WitchesRoadConfig.STRUCTURE_COPY_MARKS.get(), now + 20L * 8);
        s.clearGhosts();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.9F);
        player.displayClientMessage(Component.translatable("message.enhanced.road.copy.result", result.correct(), result.total())
                .withStyle(style -> style.withColor(WitchesRoad.LIGHT_CORE)), true);
        player.displayClientMessage(details(result), false);
        if (s.plotText != null) {
            CopyDisplays.retext(level, s.plotText, titled("message.enhanced.road.copy.plot_title",
                    Component.translatable("message.enhanced.road.copy.result", result.correct(), result.total())), 1.5F);
        }
        sayInChat(player, result.percent() >= CLOSE_PERCENT ? VOICE_CLOSE : VOICE_TRY, result.correct());
        pulse(level, player, s);
    }

    /** The Road's word goes in the chat (the action bar keeps the count). */
    private static void sayInChat(ServerPlayer player, String event, int salt) {
        if (WitchesRoadConfig.VOICE.get()) {
            player.displayClientMessage(Component.translatable(RoadVoice.key(event, salt)).withStyle(style -> style.withColor(WitchesRoad.LIGHT_CORE).withItalic(true)), false);
        }
    }

    private static Component details(StructureCompare.Result result) {
        List<Component> parts = new ArrayList<>();
        if (!result.missing().isEmpty()) {
            parts.add(Component.translatable("message.enhanced.road.copy.missing", result.missing().size()));
        }
        if (!result.wrong().isEmpty()) {
            parts.add(Component.translatable("message.enhanced.road.copy.wrong", result.wrong().size()));
        }
        if (!result.extra().isEmpty()) {
            parts.add(Component.translatable("message.enhanced.road.copy.extra", result.extra().size()));
        }
        MutableComponent line = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            line.append(i == 0 ? Component.empty() : Component.literal(", ")).append(parts.get(i));
        }
        return line.append(Component.literal(". ")).append(Component.translatable("message.enhanced.road.copy.marks"))
                .withStyle(style -> style.withColor(WitchesRoad.LIGHT));
    }

    /** The plot looks exactly like the model: the marks and ghosts go, the trial is won. */
    private static void finish(RoadTrial trial, ServerPlayer player, CopySession s, StructureCompare.Result result) {
        s.marks.clear();
        s.clearGhosts();
        ServerLevel level = trial.level();
        player.displayClientMessage(Component.translatable("message.enhanced.road.copy.perfect").withStyle(style -> style.withColor(WitchesRoad.LIGHT_CORE)), false);
        if (s.plotText != null) {
            CopyDisplays.retext(level, s.plotText, titled("message.enhanced.road.copy.plot_title",
                    Component.translatable("message.enhanced.road.copy.perfect_short")), 1.5F);
        }
        RoadFx.sparkle(level, s.plotCorner().getCenter().add(s.geo.w() / 2.0, s.geo.h() / 2.0, s.geo.d() / 2.0), 80);
        trial.succeed();
    }

    /** Hint: ghost blocks where blocks are missing or wrong, the lowest first, for a few seconds. */
    public static void hint(RoadTrial trial, ServerPlayer player) {
        if (trial.phase() != RoadTrial.Phase.ACTIVE) {
            return;
        }
        CopySession s = session(trial);
        ServerLevel level = trial.level();
        long now = level.getGameTime();
        if (now - s.lastHint < HINT_COOLDOWN) {
            return;
        }
        s.lastHint = now;
        if (!WitchesRoadConfig.STRUCTURE_COPY_HINTS.get()) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.copy.hints_off").withStyle(style -> style.withColor(WitchesRoad.LIGHT)), true);
            return;
        }
        StructureCompare.Result result = s.compare(level);
        List<StructureCompare.Cell> cells = new ArrayList<>(result.missing());
        cells.addAll(result.wrong());
        if (cells.isEmpty()) {
            player.displayClientMessage(Component.translatable(result.extra().isEmpty() ? "message.enhanced.road.copy.hint_none"
                    : "message.enhanced.road.copy.hint_extra").withStyle(style -> style.withColor(WitchesRoad.LIGHT)), true);
            if (!result.extra().isEmpty()) {
                s.mark(result, WitchesRoadConfig.STRUCTURE_COPY_MARKS.get(), now + 20L * 8);
                pulse(level, player, s);
            }
            return;
        }
        BlockPos corner = s.plotCorner();
        Vec3 eye = player.getEyePosition();
        cells.sort(Comparator.<StructureCompare.Cell>comparingInt(StructureCompare.Cell::y)
                .thenComparingDouble(c -> corner.offset(c.x(), c.y(), c.z()).getCenter().distanceToSqr(eye)));
        s.clearGhosts();
        int shown = Math.min(cells.size(), WitchesRoadConfig.STRUCTURE_COPY_HINT_BLOCKS.get());
        for (int i = 0; i < shown; i++) {
            StructureCompare.Cell cell = cells.get(i);
            BlockPos pos = corner.offset(cell.x(), cell.y(), cell.z());
            boolean around = !level.getBlockState(pos).isAir();
            Display.BlockDisplay ghost = CopyDisplays.ghost(level, pos, s.model.at(cell.x(), cell.y(), cell.z()), around, WitchesRoad.LIGHT);
            s.ghosts.add(trial.track(ghost));
        }
        s.ghostsUntil = now + 20L * WitchesRoadConfig.STRUCTURE_COPY_HINT_SECONDS.get();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        player.displayClientMessage(Component.translatable("message.enhanced.road.copy.hint", shown, cells.size())
                .withStyle(style -> style.withColor(WitchesRoad.LIGHT_CORE)), true);
        sayInChat(player, VOICE_HINT, cells.size());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Marks and the chest

    private static final ParticleOptions MISSING = new EnergyParticleOptions(WitchesRoad.LIGHT_CORE, 1.3F);
    private static final ParticleOptions WRONG = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 1.3F);
    private static final ParticleOptions EXTRA = new DustParticleOptions(new Vector3f(1.0F, 0.6F, 0.1F), 1.3F);

    /** Draws the outlines of the marked cells (for the player only): the corners and the middle of every edge of the cell's cube. */
    private static void pulse(ServerLevel level, ServerPlayer player, CopySession s) {
        BlockPos corner = s.plotCorner();
        for (CopySession.Mark mark : s.marks) {
            ParticleOptions options = mark.kind() == 0 ? MISSING : mark.kind() == 1 ? WRONG : EXTRA;
            BlockPos pos = corner.offset(mark.cell().x(), mark.cell().y(), mark.cell().z());
            for (int cx = 0; cx <= 2; cx++) {
                for (int cy = 0; cy <= 2; cy++) {
                    for (int cz = 0; cz <= 2; cz++) {
                        // The eight corners (all 0 or 2) and the twelve middles of the edges (exactly one 1).
                        int ones = (cx == 1 ? 1 : 0) + (cy == 1 ? 1 : 0) + (cz == 1 ? 1 : 0);
                        if (ones > 1) {
                            continue;
                        }
                        level.sendParticles(player, options, false, pos.getX() + cx * 0.5, pos.getY() + cy * 0.5, pos.getZ() + cz * 0.5, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
            }
        }
    }

    /** Keeps the supply chest full: three stacks of every block of the model. */
    private static void fillChest(ServerLevel level, CopySession s) {
        BlockEntity entity = level.getBlockEntity(s.chest());
        if (!(entity instanceof ChestBlockEntity chest)) {
            return;
        }
        int slot = 0;
        for (Item item : s.items) {
            for (int i = 0; i < StructureSupply.CHEST_STACKS && slot < chest.getContainerSize(); i++) {
                ItemStack want = StructureSupply.stack(item, item.getDefaultMaxStackSize());
                ItemStack have = chest.getItem(slot);
                if (have.getCount() < want.getCount() || !ItemStack.isSameItemSameComponents(have, want)) {
                    chest.setItem(slot, want);
                }
                slot++;
            }
        }
    }

    /** How many cells the last Check marks: missing, wrong, extra (for the tests and the commands). */
    public static int[] markCounts(RoadTrial trial) {
        int[] counts = new int[3];
        for (CopySession.Mark mark : session(trial).marks) {
            counts[mark.kind()]++;
        }
        return counts;
    }

    /** The ghost blocks a Hint shows now. */
    public static List<Entity> ghosts(RoadTrial trial) {
        return List.copyOf(session(trial).ghosts);
    }

    /** Compares the plot of this trial with its model now (for the tests and the commands). */
    public static StructureCompare.Result compare(RoadTrial trial) {
        return session(trial).compare(trial.level());
    }

    /** The zone of the plot in the Road dimension: where the player builds. */
    public static AABB plotBox(RoadTrial trial) {
        CopySession s = session(trial);
        BlockPos c = s.plotCorner();
        return new AABB(c.getX(), c.getY(), c.getZ(), c.getX() + s.geo.w(), c.getY() + s.geo.plotHeight(), c.getZ() + s.geo.d());
    }
}

package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildSession.Stroke;
import com.kolodkingal.enhanced.build.SpellPlanner.Material;
import com.kolodkingal.enhanced.build.SpellPlanner.Plan;
import com.kolodkingal.enhanced.build.SpellPlanner.Setup;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.BuildPocketPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Build Magic on the server (D-B 11): the entry points the two build payloads call ({@code BuildCastPayload},
 * {@code BuildActionPayload}), and what the builder is told. It validates every cast (D-B 10.2), plans it again from its
 * recipe ({@link SpellPlanner}), answers a preflight ({@link BuildRules#preflight}), and hands the spell to
 * {@link BuildScheduler} as a {@link BuildJob}.
 *
 * <p><b>A cast</b> (D-B 11.3), {@link #cast}: the token bucket (C6), the recipe clamped to the hero's numbers
 * ({@link Limits}), C1 to C5 ({@link BuildRules#player}), the reach (C7), the material as the server reads it (C8: the
 * block he picked, the hotbar's mix, or the main hand, else the off hand, in the state he would place it looking where
 * he looks), the plan, an island being cleared (C9), one spell at a time for Billy and the queues (C10, C11), the
 * ceiling and the long hold (C11, C12). A refusal answers its verdict, its line and the fizzle; a preflight answers what
 * the spell would do; else the job is queued and everyone near sees it start.
 *
 * <p><b>The actions</b>, {@link #action}: build mode on and off, Ctrl + Z and Ctrl + Y (also with build mode off, R42),
 * the middle click's pick, back to the hand, the box the others see.
 *
 * <p><b>Ctrl + Z</b> (D-B 12.2), in the order of what the kid did last: the newest waiting spell is dropped; else the
 * spell at work stops and its partial step is undone at once (the undo jumps the queue); else the newest step is
 * undone; else "Nothing to undo".
 */
public final class BuildMagic {
    /** How often the box the others see may change (ticks): at most 4 times a second. */
    private static final int VIEW_TICKS = 5;
    /**
     * How many cells a preflight's flood (a selected thing, a Pull's surface) may read, within the preflight's 2 ms,
     * before it answers in part.
     */
    private static final int PREFLIGHT_READS = 65_536;
    /** The flood's reads between two looks at the clock. */
    private static final int FLOOD_SLICE = 1024;
    /** The most parts of a pocket sent to its owner a tick (D-B 13.1). */
    private static final int POCKET_PARTS = 4;
    /** What a preflight reads its 2 ms by (a test's clock that stands still sees every cell). */
    private static LongSupplier clock = System::nanoTime;

    // ------------------------------------------------------------------------------------------------------------
    // A cast

    /**
     * A cast arrived: {@code recipe} as the client sent it (clamped first), {@code token} to answer with, {@code flags}
     * the cast's ({@link BuildCastPayload#PREFLIGHT}, {@link BuildCastPayload#HELD_SHORT},
     * {@link BuildCastPayload#HELD_LONG}).
     */
    public static void cast(ServerPlayer player, SpellRecipe recipe, int token, int flags) {
        BuildSession session = BuildSession.of(player.getUUID());
        long now = player.server.getTickCount();
        if (!session.takeRequest(now)) {
            dropped(player, session, now);
            return; // C6: dropped
        }
        boolean preflight = (flags & BuildCastPayload.PREFLIGHT) != 0;
        if (preflight && !session.takePreflight(now)) {
            return;
        }
        boolean dab = recipe.op() == SpellRecipe.Op.DAB && !preflight;
        if (dab && (!session.takeDab(now) || session.painted(recipe.stroke()))) {
            return; // more than 20 dabs a second, or a late dab of a stroke already sealed: dropped
        }
        Stroke open = session.stroke();
        if (open != null && !preflight && (!dab || open.number() != recipe.stroke())) {
            // Another spell, or a new stroke: the stroke painted so far is one step, before it.
            closeStroke(open, player.server);
            open = null;
        }
        Limits limits = Limits.of(player);
        int code = BuildRules.player(player, limits, session.mode(), true);
        if (code == BuildRules.SILENT) {
            return;
        }
        if (code != BuildRules.OK) {
            refuse(player, token, code, 0, BuildRules.message(code, 0), quiet(session, preflight, dab, recipe));
            return;
        }
        SpellRecipe clamped = recipe.clamped(limits);
        code = BuildRules.reach(player, limits, clamped);
        if (code != BuildRules.OK) {
            int extra = code == BuildVerdictPayload.TOO_WIDE ? limits.side() : limits.reach();
            refuse(player, token, code, extra, BuildRules.message(code, extra), quiet(session, preflight, dab, recipe));
            return;
        }
        if (clamped.spell() == Spell.MOVE && clamped.op() == SpellRecipe.Op.PICK_UP) {
            // A Move's pick-up changes nothing: its source is read when it is put down (D-B 4.9).
            send(player, new BuildVerdictPayload(token, BuildVerdictPayload.READY, 0, 0, 0, 0));
            return;
        }
        ServerLevel level = player.serverLevel();
        Setup setup = Setup.at(player.blockPosition()).withSurface(limits.magicSelect(), limits.sideLimit())
                .withWindow(BuildConfig.MAX_TICKET_CHUNKS.get());
        if (clamped.spell().needsMaterial()) {
            Chosen chosen = material(player, clamped, session, limits);
            if (chosen.code != BuildRules.OK) {
                refuse(player, token, chosen.code, 0, chosen.message, quiet(session, preflight, dab, recipe));
                return;
            }
            setup = setup.withMaterial(chosen.material);
        }
        if (clamped.spell() == Spell.COPY && clamped.op() == SpellRecipe.Op.STAMP) {
            setup = setup.withSource(Pocket.held(player.getUUID()));
        }
        Plan plan = null;
        if (clamped.region() != SpellRecipe.MASK) {
            plan = SpellPlanner.plan(level, clamped, setup);
            if (plan.refusal() != SpellPlanner.NONE) {
                refuse(player, token, plan.refusal(), 0, BuildRules.message(plan.refusal(), 0), quiet(session, preflight, dab, recipe));
                return;
            }
        }
        BoundingBox box = plan != null && plan.prepared() ? plan.bounds()
                : clamped.region() != SpellRecipe.NO_REGION ? SpellMath.box(clamped.a(), clamped.b()) : null;
        // A Move empties its source; a Copy or a Repeat only reads theirs, which may lie on the island (spec 5.4).
        BoundingBox source = plan != null && plan.region() != null && clamped.spell() == Spell.MOVE ? plan.region().bounds() : null;
        if (BuildRules.island(level, box) || BuildRules.island(level, source)) {
            refuse(player, token, BuildVerdictPayload.SOKOVIA, 0, BuildRules.message(BuildVerdictPayload.SOKOVIA, 0),
                    quiet(session, preflight, dab, recipe));
            return;
        }
        if (open != null) {
            // The next dab of a stroke being painted: one spell, so no queue or pause of its own; a second of dabs may
            // wait, more are dropped.
            if (open.pending() >= Stroke.MAX_WAITING) {
                return;
            }
        } else {
            code = queueRefusal(player, limits);
            if (code != BuildRules.OK) {
                int extra = code == BuildVerdictPayload.QUEUE_FULL ? BuildScheduler.waiting(player.getUUID()) : 0;
                Component line = code == BuildVerdictPayload.WAIT ? BillyMagic.waitForSpell() : BuildRules.message(code, extra);
                refuse(player, token, code, extra, line, quiet(session, preflight, dab, recipe));
                return;
            }
        }
        if (plan != null && plan.prepared()) {
            long size = plan.size();
            if (limits.maxBlocks() > 0 && size > limits.maxBlocks()) {
                refuse(player, token, BuildVerdictPayload.CEILING, limits.maxBlocks(),
                        BuildRules.message(BuildVerdictPayload.CEILING, limits.maxBlocks()), quiet(session, preflight, dab, recipe));
                return;
            }
            if (size > BuildConfig.HUGE_SPELL_BLOCKS.get() && (flags & BuildCastPayload.HELD_LONG) == 0) {
                refuse(player, token, BuildVerdictPayload.HOLD, size, BuildRules.message(BuildVerdictPayload.HOLD, size),
                        quiet(session, preflight, dab, recipe));
                return;
            }
        }
        if (preflight) {
            preflight(player, limits, clamped, token, setup, plan);
            return;
        }
        if (dab) {
            dab(player, session, limits, clamped, token, setup, plan, open);
            return;
        }
        BuildJob job = BuildJob.spell(player, limits, clamped, token, flags, setup, plan, BuildScheduler.loaded(player.getUUID(), level));
        BuildScheduler.queue(job, false);
        job.fx(BuildFxPayload.START);
        BoundingBox at = job.bounds();
        BuildSounds.cast(level, at != null ? Vec3.atCenterOf(at.getCenter()) : player.position(), limits.hero(),
                plan != null && plan.prepared() ? plan.size() : 0L);
        sync(player.server, player.getUUID());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Paint strokes (D-B 11.8)

    /**
     * A dab of a Paint stroke, checked like any cast: its job is queued behind the stroke's others and writes into the
     * stroke's one step. The first dab opens the stroke (its effects and its sound are the stroke's).
     */
    private static void dab(ServerPlayer player, BuildSession session, Limits limits, SpellRecipe recipe, int token, Setup setup, @Nullable Plan plan,
                            @Nullable Stroke open) {
        Stroke stroke = open;
        if (stroke == null) {
            ServerLevel level = player.serverLevel();
            UndoStep step = new UndoStep(UndoHistory.of(player.getUUID()).nextId(), recipe.spell(), level.dimension(), level.getGameTime());
            net.minecraft.world.entity.player.Player mom = limits.billy() && limits.limited() ? BillyMagic.mom(player) : null;
            stroke = new Stroke(player.getUUID(), recipe.stroke(), level, limits, step, new BuildRules.Cells(level, player), mom != null,
                    mom != null ? mom.getId() : BuildFxPayload.NO_MOM);
            session.open(stroke);
        }
        stroke.lastDab = player.server.getTickCount();
        stroke.token = token;
        stroke.pending++;
        BuildJob job = BuildJob.dab(player, limits, recipe, token, setup, plan, BuildScheduler.loaded(player.getUUID(), player.serverLevel()), stroke);
        BuildScheduler.queue(job, false);
        if (open == null) {
            job.fx(BuildFxPayload.START);
            BoundingBox at = job.bounds();
            BuildSounds.cast(player.serverLevel(), at != null ? Vec3.atCenterOf(at.getCenter()) : player.position(), limits.hero(), 0L);
        }
        sync(player.server, player.getUUID());
    }

    /**
     * The stroke takes no more dabs ({@code STROKE_END}, 40 ticks after its last dab, another spell, a change of
     * power): it is sealed as one step once its last dab is done.
     */
    static void closeStroke(Stroke stroke, MinecraftServer server) {
        if (stroke.closing) {
            return;
        }
        stroke.closing = true;
        if (stroke.pending == 0) {
            BuildJob.sealStroke(stroke, server);
        }
    }

    /**
     * Ctrl + Z (or Ctrl + Y) while a stroke is being painted: it ends now, as one step. Its waiting dabs are dropped,
     * the one at work is finished at once (a dab is small), and it is sealed.
     */
    private static void endStrokeNow(ServerPlayer player, Stroke stroke) {
        UUID id = player.getUUID();
        BuildJob working = BuildScheduler.working(id);
        for (BuildJob job : BuildScheduler.jobs(id)) {
            if (job != working && job.stroke() == stroke) {
                BuildScheduler.drop(job);
            }
        }
        closeStroke(stroke, player.server);
        if (working != null && working.stroke() == stroke) {
            BuildScheduler.finishNow(working);
        }
        if (stroke.pending == 0) {
            BuildJob.sealStroke(stroke, player.server);
        }
    }

    /**
     * Whether a refusal is answered by its verdict alone: a preflight's, and a dab's but the first of its stroke (a
     * held button sends 20 dabs a second: the builder hears why once).
     */
    private static boolean quiet(BuildSession session, boolean preflight, boolean dab, SpellRecipe recipe) {
        return preflight || dab && !session.refusedDab(recipe.stroke());
    }

    /**
     * C10 and C11: Billy's last spell is done and his pause over ({@code WAIT}); the Scarlet Witch's queue has room
     * ({@code QUEUE_FULL}); the server has room ({@code SERVER_BUSY}). {@link BuildRules#OK} when all pass.
     */
    private static int queueRefusal(ServerPlayer player, Limits limits) {
        UUID id = player.getUUID();
        if (limits.limited()) {
            BuildData data = player.getData(ModAttachments.BUILD_DATA);
            if (BuildScheduler.working(id) != null || BillyMagic.paused(data, player.server.overworld().getGameTime())) {
                return BuildVerdictPayload.WAIT;
            }
        } else if (BuildScheduler.working(id) != null && BuildScheduler.waiting(id) >= limits.maxQueued()) {
            return BuildVerdictPayload.QUEUE_FULL;
        }
        if (BuildScheduler.total() >= BuildConfig.MAX_SPELLS_TOTAL.get()) {
            return BuildVerdictPayload.SERVER_BUSY;
        }
        return BuildRules.OK;
    }

    /**
     * The answer to a preflight (D-B 10.1): what the spell would do, with no event posted and nothing changed. A selected
     * thing or a Pull's surface is flooded first, within the preflight's 2 ms and at most {@link #PREFLIGHT_READS}
     * cells; one that does not finish is answered as seen in part ({@link BuildVerdictPayload#CUT_OFF}).
     */
    private static void preflight(ServerPlayer player, Limits limits, SpellRecipe recipe, int token, Setup setup, @Nullable Plan plan) {
        ServerLevel level = player.serverLevel();
        long deadline = clock.getAsLong() + BuildRules.PREFLIGHT_NANOS;
        if (plan == null) {
            MagicSelect select = limits.select(level, recipe.seeds(), player.getEyePosition(), true);
            boolean done = false;
            for (int read = 0; !done && read < PREFLIGHT_READS && clock.getAsLong() < deadline; read += FLOOD_SLICE) {
                done = select.step(FLOOD_SLICE);
            }
            if (!done) {
                send(player, new BuildVerdictPayload(token, BuildVerdictPayload.READY, 0, 0, 0, BuildVerdictPayload.CUT_OFF));
                return;
            }
            if (select.refusal() != SpellPlanner.NONE) {
                send(player, new BuildVerdictPayload(token, select.refusal(), 0, 0, select.limit(), 0));
                return;
            }
            plan = SpellPlanner.plan(level, recipe, setup.withMask(select.mask()));
            if (plan.refusal() != SpellPlanner.NONE) {
                send(player, new BuildVerdictPayload(token, plan.refusal(), 0, 0, 0, 0));
                return;
            }
        }
        boolean prepared = plan.prepared();
        for (int read = 0; !prepared && read < PREFLIGHT_READS && clock.getAsLong() < deadline; read += FLOOD_SLICE) {
            prepared = plan.prepare(FLOOD_SLICE);
        }
        if (!prepared) {
            send(player, new BuildVerdictPayload(token, BuildVerdictPayload.READY, 0, 0, 0, BuildVerdictPayload.CUT_OFF));
            return;
        }
        BuildRules.Preview preview = BuildRules.preflight(level, player, plan, BuildConfig.PREVIEW_CHECKS.get(), clock);
        int cost = limits.limited() ? BillyMagic.cost(preview.changed(), BillyMagic.mom(player) != null) : 0;
        int code = preview.partly() ? BuildVerdictPayload.READY_PARTLY : BuildVerdictPayload.READY;
        send(player, new BuildVerdictPayload(token, code, (int) Math.min(Integer.MAX_VALUE, preview.changed()), cost,
                (int) Math.min(Integer.MAX_VALUE, preview.removed()), preview.flags(), preview.marks(), preview.kinds()));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The material (D-B 5)

    /** The material the server read for a cast, or why there is none. */
    private record Chosen(int code, @Nullable Material material, @Nullable Component message) {
    }

    /**
     * The material of a cast (C8): the hotbar's mix, or the block the hero picked, or the block in the main hand (else
     * the off hand) in the state he would place it, at the anchor and face he looks at.
     */
    private static Chosen material(ServerPlayer player, SpellRecipe recipe, BuildSession session, Limits limits) {
        if (recipe.mix()) {
            List<ItemStack> hotbar = new ArrayList<>();
            for (int slot = 0; slot < 9; slot++) {
                hotbar.add(player.getInventory().getItem(slot));
            }
            SpellMath.Mix mix = SpellMath.Mix.ofHotbar(hotbar, stack -> {
                BlockState state = placement(player, InteractionHand.MAIN_HAND, stack, recipe);
                return state != null && BuildRules.material(state, limits) == BuildRules.OK ? state : null;
            });
            return mix == null ? refusal(BuildVerdictPayload.NO_BLOCK, null) : new Chosen(BuildRules.OK, Material.mix(mix, recipe.seed()), null);
        }
        BlockState state = null;
        // A pick lasts while she holds what she held (D-B 5.2): a new hotbar slot or item goes back to the hand.
        if (recipe.material() == SpellRecipe.PICKED && session.keepsPick(player)) {
            state = session.picked();
            if (state == null && session.pickedBlock() != null) {
                Block block = session.pickedBlock();
                ItemStack stack = block.asItem() instanceof BlockItem ? new ItemStack(block) : ItemStack.EMPTY;
                state = stack.isEmpty() ? block.defaultBlockState() : placement(player, InteractionHand.MAIN_HAND, stack, recipe);
            }
        }
        if (state == null) {
            state = placement(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), recipe);
            if (state == null) {
                state = placement(player, InteractionHand.OFF_HAND, player.getOffhandItem(), recipe);
            }
        }
        int code = BuildRules.material(state, limits);
        if (code != BuildRules.OK) {
            return refusal(code, state);
        }
        return new Chosen(BuildRules.OK, Material.of(state), null);
    }

    private static Chosen refusal(int code, @Nullable BlockState state) {
        Component line = code == BuildVerdictPayload.BAD_BLOCK && state != null
                ? Component.translatable("message.enhanced.build.bad_block", state.getBlock().getName())
                : code == BuildVerdictPayload.VOLATILE ? BillyMagic.tooDangerous() : BuildRules.message(code, 0);
        return new Chosen(code, null, line);
    }

    /**
     * The block an item makes, in the state the hero would place it by hand at the recipe's anchor and face (D-B 5.1):
     * a block item's {@code getStateForPlacement}; a bucket's fluid as its source block; null for anything else.
     */
    public static @Nullable BlockState placement(ServerPlayer player, InteractionHand hand, ItemStack stack, SpellRecipe recipe) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof BlockItem item) {
            Direction face = recipe.direction() != null ? recipe.direction() : Direction.UP;
            BlockPos anchor = recipe.anchor();
            if (!SpellMath.loaded(player.level(), anchor) || !SpellMath.loaded(player.level(), anchor.relative(face))) {
                return item.getBlock().defaultBlockState();
            }
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(anchor), face, anchor, false);
            BlockState state = item.getBlock().getStateForPlacement(new BlockPlaceContext(player, hand, stack, hit));
            return state != null ? state : item.getBlock().defaultBlockState();
        }
        if (stack.getItem() instanceof BucketItem bucket && bucket.content != Fluids.EMPTY) {
            return bucket.content.defaultFluidState().createLegacyBlock();
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The actions

    /**
     * One of the builder's actions arrived ({@code BuildActionPayload}'s {@code MODE}, {@code UNDO}, {@code REDO},
     * {@code PICK}, {@code PICK_BLOCK}, {@code HAND}, {@code STROKE_END}, {@code VIEW}, {@code POCKET}), with its box
     * {@code a}-{@code b} and its number. UNDO and REDO need no build mode (R42).
     */
    public static void action(ServerPlayer player, int action, BlockPos a, BlockPos b, int value) {
        BuildSession session = BuildSession.of(player.getUUID());
        long now = player.server.getTickCount();
        if (!session.takeRequest(now)) {
            dropped(player, session, now);
            return;
        }
        switch (action) {
            case BuildActionPayload.MODE -> setMode(player, value != 0);
            case BuildActionPayload.UNDO -> undo(player);
            case BuildActionPayload.REDO -> redo(player);
            case BuildActionPayload.PICK -> pick(player, a);
            case BuildActionPayload.PICK_BLOCK -> pickBlock(player, value);
            case BuildActionPayload.HAND -> {
                if (session.mode() && (session.picked() != null || session.pickedBlock() != null)) {
                    session.dropPicked();
                    player.displayClientMessage(Component.translatable("message.enhanced.build.hand"), true);
                }
            }
            case BuildActionPayload.STROKE_END -> {
                Stroke stroke = session.stroke();
                if (stroke != null && stroke.number() == value) {
                    closeStroke(stroke, player.server);
                }
            }
            case BuildActionPayload.VIEW -> view(player, a, b, value);
            case BuildActionPayload.POCKET -> {
                Pocket pocket = Pocket.held(player.getUUID());
                Pocket.Stream sending = session.pocketStream();
                if (pocket != null && pocket.id() == value && (sending == null || sending.pocket() != pocket)) {
                    session.setPocketStream(pocket.stream());
                }
            }
            default -> {
                // Any other number is no action: ignored.
            }
        }
    }

    /**
     * Build mode on or off (D-B 2.2): only for the Scarlet Witch and Billy, alive and no Spectator (anyone else is
     * ignored); on is refused, with its line, when Build Magic is off for the hero, for a non-operator where only
     * operators may, in Adventure and in a denied dimension.
     */
    public static void setMode(ServerPlayer player, boolean on) {
        Limits limits = Limits.of(player);
        if (limits == null || !player.isAlive() || player.isSpectator()) {
            return;
        }
        if (!on) {
            leaveMode(player, true);
            return;
        }
        int code = BuildRules.switches(player, limits);
        if (code == BuildRules.OK && !player.mayBuild()) {
            code = BuildVerdictPayload.ADVENTURE;
        }
        if (code == BuildRules.OK && BuildRules.denied(player.serverLevel())) {
            code = BuildVerdictPayload.DIMENSION;
        }
        if (code != BuildRules.OK) {
            // (The client turned its build mode on at once: the verdict tells it why it is off.)
            refuse(player, 0, code, 0, BuildRules.message(code, 0), false);
            return;
        }
        BuildSession session = BuildSession.of(player.getUUID());
        if (session.mode()) {
            return;
        }
        session.setMode(true);
        player.setData(ModAttachments.BUILD_VIEW, player.getData(ModAttachments.BUILD_VIEW).withActive(true));
        BuildSounds.buildModeOn(player);
        player.displayClientMessage(Component.translatable("message.enhanced.build.on", Component.keybind("key.enhanced.build_magic")), true);
    }

    /**
     * Build mode off (the key, a death, a respawn, a logout, a power change, Adventure or Spectator): the view the others
     * see goes; the spells go on. {@code say}: the builder is told.
     */
    public static void leaveMode(ServerPlayer player, boolean say) {
        BuildSession session = BuildSession.existing(player.getUUID());
        boolean was = session != null && session.mode();
        if (session != null) {
            session.setMode(false);
        }
        // (A player who never built has no view, and is given none.)
        BuildView view = player.getExistingData(ModAttachments.BUILD_VIEW).orElse(null);
        if (view != null && view.active()) {
            player.setData(ModAttachments.BUILD_VIEW, view.withActive(false));
        }
        if (was && say) {
            player.displayClientMessage(Component.translatable("message.enhanced.build.off"), true);
        }
    }

    /** The box the others see while she builds (at most 4 times a second): {@code value} 0 for none, else the spell + 1. */
    private static void view(ServerPlayer player, BlockPos a, BlockPos b, int value) {
        BuildSession session = BuildSession.of(player.getUUID());
        if (!session.mode() || value != 0 && player.tickCount % VIEW_TICKS != 0) {
            return;
        }
        BuildView view = player.getData(ModAttachments.BUILD_VIEW);
        Spell spell = value == 0 ? view.spell() : Spell.byId(value - 1);
        Optional<BuildView.Box> box = value == 0 ? Optional.empty() : Optional.of(new BuildView.Box(a, b));
        BuildView next = view.withSpell(spell, box);
        if (!next.equals(view)) {
            player.setData(ModAttachments.BUILD_VIEW, next);
        }
    }

    /**
     * The middle click (D-B 5.2): exactly the block at {@code at} (within reach + 2, loaded), as the server reads it,
     * becomes the material; one magic can never make says "Magic can't make %s".
     */
    private static void pick(ServerPlayer player, BlockPos at) {
        BuildSession session = BuildSession.of(player.getUUID());
        Limits limits = Limits.of(player);
        if (limits == null || !session.mode() || !limits.inReach(player.getEyePosition(), at, true) || !SpellMath.loaded(player.level(), at)) {
            return;
        }
        BlockState state = player.level().getBlockState(at);
        if (state.isAir()) {
            return;
        }
        int code = BuildRules.material(state, limits);
        if (code != BuildRules.OK && code != BuildVerdictPayload.TWO_BLOCK) {
            refuse(player, 0, code, 0, refusal(code, state).message, false);
            return;
        }
        session.setPicked(state, null, player);
        player.displayClientMessage(Component.translatable("message.enhanced.build.picked", state.getBlock().getName()), true);
    }

    /**
     * A block chosen on the palette page (D-B 5.4), by its registry id: it must exist and be a block magic may make;
     * it is then placed as the hero would place it, as the hand's block is.
     */
    private static void pickBlock(ServerPlayer player, int id) {
        BuildSession session = BuildSession.of(player.getUUID());
        Limits limits = Limits.of(player);
        if (limits == null || !session.mode() || id <= 0 || id >= BuiltInRegistries.BLOCK.size()) {
            return;
        }
        Block block = BuiltInRegistries.BLOCK.byId(id);
        BlockState state = block.defaultBlockState();
        if (state.isAir()) {
            return;
        }
        int code = BuildRules.material(state, limits);
        if (code != BuildRules.OK) {
            refuse(player, 0, code, 0, refusal(code, state).message, false);
            return;
        }
        session.setPicked(null, block, player);
        player.displayClientMessage(Component.translatable("message.enhanced.build.picked", block.getName()), true);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Undo and redo (D-B 12.2)

    /** Ctrl + Z (also with build mode off, R42): see the class comment. */
    public static void undo(ServerPlayer player) {
        Limits limits = Limits.of(player);
        int code = BuildRules.player(player, limits, false, false);
        if (code == BuildRules.SILENT) {
            return;
        }
        if (code != BuildRules.OK) {
            refuse(player, 0, code, 0, BuildRules.message(code, 0), false);
            return;
        }
        UUID id = player.getUUID();
        BuildSession session = BuildSession.existing(id);
        if (session != null && session.stroke() != null) {
            // The stroke being painted is what she did last: it ends here, as one step, and is the one undone.
            endStrokeNow(player, session.stroke());
        }
        if (BuildScheduler.dropNewestWaiting(id) != null) {
            send(player, new BuildVerdictPayload(0, BuildVerdictPayload.DROPPED, 0, 0, 0, 0));
            player.displayClientMessage(Component.translatable("message.enhanced.build.dropped"), true);
            sync(player.server, id);
            return;
        }
        BuildJob working = BuildScheduler.working(id);
        if (working != null && working.kind() == BuildJob.Kind.SPELL) {
            BuildScheduler.remove(working);
            BuildJob rewind = working.stopAndRewind(player);
            if (rewind == null) {
                player.displayClientMessage(Component.translatable("message.enhanced.build.stopped"), true);
            } else {
                BuildScheduler.queue(rewind, true);
                rewind.fx(BuildFxPayload.START);
                BuildSounds.undo(rewind.level(), player.position());
            }
            sync(player.server, id);
            return;
        }
        if (working != null) {
            refuse(player, 0, BuildVerdictPayload.WAIT, 0, BillyMagic.waitForSpell(), false);
            return;
        }
        UndoStep step = UndoHistory.of(id).nextToUndo();
        ServerLevel level = step == null ? null : player.server.getLevel(step.dimension());
        if (step == null || level == null) {
            refuse(player, 0, BuildVerdictPayload.NOTHING_TO_UNDO, 0, BuildRules.message(BuildVerdictPayload.NOTHING_TO_UNDO, 0), false);
            return;
        }
        replay(player, limits, level, step, false);
    }

    /** Ctrl + Y (also with build mode off): the newest undone step is done again. */
    public static void redo(ServerPlayer player) {
        Limits limits = Limits.of(player);
        int code = BuildRules.player(player, limits, false, false);
        if (code == BuildRules.SILENT) {
            return;
        }
        if (code != BuildRules.OK) {
            refuse(player, 0, code, 0, BuildRules.message(code, 0), false);
            return;
        }
        UUID id = player.getUUID();
        BuildSession session = BuildSession.existing(id);
        if (session != null && session.stroke() != null) {
            endStrokeNow(player, session.stroke());
        }
        if (BuildScheduler.working(id) != null) {
            refuse(player, 0, BuildVerdictPayload.WAIT, 0, BillyMagic.waitForSpell(), false);
            return;
        }
        UndoStep step = UndoHistory.of(id).nextToRedo();
        ServerLevel level = step == null ? null : player.server.getLevel(step.dimension());
        if (step == null || level == null) {
            refuse(player, 0, BuildVerdictPayload.NOTHING_TO_REDO, 0, BuildRules.message(BuildVerdictPayload.NOTHING_TO_REDO, 0), false);
            return;
        }
        replay(player, limits, level, step, true);
    }

    private static void replay(ServerPlayer player, Limits limits, ServerLevel level, UndoStep step, boolean redo) {
        UUID id = player.getUUID();
        BuildJob job = BuildJob.replay(player, limits, level, step, redo, false, BuildScheduler.loaded(id, level));
        BuildScheduler.queue(job, false);
        job.fx(BuildFxPayload.START);
        BoundingBox box = step.bounds();
        BuildSounds.undo(level, box != null ? Vec3.atCenterOf(box.getCenter()) : player.position());
        sync(player.server, id);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The rest of a builder's life

    /**
     * The player's power changed from {@code from} to {@code to} ({@code Powers.setPower}): build mode goes off, the spell
     * at work finishes and the waiting ones are dropped; the history, the pocket and the meter are kept (D-B 11.9).
     */
    public static void onPowerChanged(ServerPlayer player, Power from, Power to) {
        if (from != Power.SCARLET_WITCH && from != Power.BILLY) {
            return;
        }
        leaveMode(player, false);
        BuildScheduler.dropWaiting(player.getUUID());
        BuildSession session = BuildSession.existing(player.getUUID());
        if (session != null) {
            session.dropPicked();
            if (session.stroke() != null) {
                closeStroke(session.stroke(), player.server);
            }
        }
        sync(player.server, player.getUUID());
    }

    /**
     * A copy was taken: the builder's pocket id and size, and the pocket itself on its way to her client (D-B 13.1),
     * which then draws its ghost; a client that lacks it later asks for it ({@code POCKET}).
     */
    static void pocketTaken(ServerPlayer player, Pocket pocket) {
        BuildData data = player.getData(ModAttachments.BUILD_DATA);
        player.setData(ModAttachments.BUILD_DATA, data.withPocket(pocket.id(), new Vec3i(pocket.sizeX(), pocket.sizeY(), pocket.sizeZ())));
        BuildSession.of(player.getUUID()).setPocketStream(pocket.stream());
    }

    /**
     * Every server tick ({@code ServerTickEvent.Post}): a stroke whose last dab was 40 ticks ago closes (D-B 11.8), and
     * each builder's pocket goes on its way to her client, at most {@value #POCKET_PARTS} parts a tick.
     */
    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (BuildSession session : BuildSession.all()) {
            Stroke stroke = session.stroke();
            if (stroke != null && !stroke.closing() && now - stroke.lastDab >= Stroke.CLOSES_AFTER) {
                closeStroke(stroke, server);
            }
            Pocket.Stream stream = session.pocketStream();
            if (stream == null) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(session.player());
            if (player == null || Pocket.held(session.player()) != stream.pocket()) {
                session.setPocketStream(null); // (away, or another copy taken since: it is sent when asked for)
                continue;
            }
            if (stream.step()) {
                for (BuildPocketPayload part : stream.next(POCKET_PARTS)) {
                    PacketDistributor.sendToPlayer(player, part);
                }
                if (stream.done()) {
                    session.setPocketStream(null);
                }
            }
        }
    }

    /** What the builder's HUD shows of her history and her spells ({@link BuildData}), when she is online. */
    public static void sync(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) {
            return;
        }
        UndoHistory history = UndoHistory.existing(id);
        BuildJob working = BuildScheduler.working(id);
        // (A player who never built has no data, and is given none while there is nothing to show.)
        BuildData data = player.getExistingData(ModAttachments.BUILD_DATA).orElse(BuildData.EMPTY);
        BuildData next = data.withHistory(history == null ? 0 : history.undoCount(), history == null ? 0 : history.redoCount())
                .withWork(working != null, BuildScheduler.waiting(id), working == null ? 0 : working.permille());
        if (!next.equals(data)) {
            player.setData(ModAttachments.BUILD_DATA, next);
        }
    }

    /** Forgets every builder's live state (the sessions, the histories, the pockets): for the server's stop and for tests. */
    public static void clear() {
        BuildSession.clear();
        UndoHistory.clear();
        Pocket.clearHeld();
        clock = System::nanoTime;
    }

    /**
     * The clock a preflight reads its 2 ms by (D-B 10.1), until {@link #clear}: a test's that stands still lets a
     * preflight see every cell on a server that is busy with other things.
     */
    public static void useClock(LongSupplier preflightClock) {
        clock = preflightClock;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Telling

    /**
     * A refusal (D-B 11.3 step 2): the verdict, and unless it is a preflight's, the line and the fizzle (the builder
     * alone hears it).
     */
    static void refuse(ServerPlayer player, int token, int code, long extra, @Nullable Component line, boolean preflight) {
        send(player, new BuildVerdictPayload(token, code, 0, 0, (int) Math.min(Integer.MAX_VALUE, extra), 0));
        if (preflight) {
            return;
        }
        if (line != null) {
            player.displayClientMessage(line, true);
        }
        BuildSounds.fizzle(player);
    }

    /** A request over the token bucket was dropped (C6): said in the server's log at most once a minute a player. */
    private static void dropped(ServerPlayer player, BuildSession session, long tick) {
        if (session.logDropped(tick)) {
            Enhanced.LOGGER.warn("Build Magic: {} sends more than {} requests a second; the rest are dropped", player.getGameProfile().getName(),
                    BuildConfig.MAX_REQUESTS_PER_SECOND.get());
        }
    }

    static void send(ServerPlayer player, BuildVerdictPayload verdict) {
        PacketDistributor.sendToPlayer(player, verdict);
    }

    private BuildMagic() {
    }
}

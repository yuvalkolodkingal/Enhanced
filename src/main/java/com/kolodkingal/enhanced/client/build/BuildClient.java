package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildGrammar;
import com.kolodkingal.enhanced.build.BuildGrammar.Aim;
import com.kolodkingal.enhanced.build.BuildHints;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.build.ChangeStyle;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.build.MagicSelect;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellMath;
import com.kolodkingal.enhanced.build.SpellPlanner;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.VanishStyle;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.network.BuildPocketPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The builder's own Build Magic on the client (D-B 11.10): build mode, the {@link BuildGrammar} (the spell, its style
 * and numbers, the box or the selected thing, the shape, the ghost), the material shown (the hand, a picked block, the
 * hotbar's mix), the selected thing's flood, the client's count of the plan, the hold timer, the preflight and the
 * server's verdicts with their marks, the box others see, the copy of the pocket and the open brush stroke. It sends
 * recipes and actions; the server decides. {@link BuildInput} reads the keys and the mouse and drives it.
 *
 * <p><b>Build mode</b> answers at once: a tap of Y turns it on here and asks the server ({@code MODE}). The server's
 * word ({@link BuildView#active}, synced) wins whenever it changes: build mode ends by itself at a death, a power
 * change, Spectator or Adventure mode (D-B 2.2). A refusal of the mode itself ({@code OFF}, {@code OPERATORS},
 * {@code ADVENTURE}) turns it off here too. A change of world keeps it, and lets go of the box (D-B 3.5).
 *
 * <p><b>The preflight</b> (D-B 10.1): when the plan has been still for {@value #PREFLIGHT_STILL} ticks, the cast goes
 * to the server with the {@code PREFLIGHT} flag, at most 4 a second; the verdict that comes back for it gives the cost,
 * the counts and the marks of the cells that will be left alone. <b>The client's count</b> (D-B 11.10) walks the plan
 * against this client's world, {@value #COUNT_PER_TICK} cells a tick, and decides the short hold (D-B 10.2): a spell
 * that removes or replaces more than {@code holdToConfirmBlocks} existing blocks, or whose count is not done yet, needs
 * the right button held {@code holdToConfirmTicks}; a plan over {@code hugeSpellBlocks}, or one the server answered
 * {@code HOLD} or {@code NO_UNDO_ROOM}, {@code hugeHoldTicks}.
 *
 * <p>Only ever loaded on a client.
 */
public final class BuildClient {
    /** Ticks a plan must stay the same before it is preflighted (D-B 14.7). */
    public static final int PREFLIGHT_STILL = 5;
    /** Ticks between two preflights: at most 4 a second (D-B 14.7). */
    public static final int PREFLIGHT_GAP = 5;
    /** Cells the client's count walks a client tick (D-B 14.7). */
    public static final int COUNT_PER_TICK = 50_000;
    /** Block reads of the selected thing's flood a client tick (D-B 14.7). */
    public static final int FLOOD_PER_TICK = 20_000;
    /** Ticks between two changes of the box others see: at most 4 a second, as the server syncs it. */
    public static final int VIEW_GAP = 5;
    /** Ticks before a missing pocket is asked for again. */
    public static final int POCKET_ASK_GAP = 100;
    /** Ticks between two dabs of Land's or Grow's brush held still (a pulse every 4 ticks, D-B 4.11). */
    public static final int PULSE_TICKS = 4;
    /** The most casts awaiting their verdict that are remembered (a server that never answers forgets nothing else). */
    private static final int MAX_PENDING = 256;

    /** The build mode this client shows (it answers at once; the server's changes win). */
    private static boolean mode;
    /** What the server last said ({@link BuildView#active}), to see when it changes. */
    private static boolean serverMode;
    private static @Nullable LocalPlayer seenPlayer;
    private static @Nullable ClientLevel seenLevel;
    private static @Nullable Power seenPower;
    private static long ticks;

    private static @Nullable BuildGrammar grammar;
    private static @Nullable Aim aim;
    private static @Nullable Limits limits;
    /** The current recipe (null: nothing ready), its material, and the key that says when the plan changes. */
    private static @Nullable SpellRecipe recipe;
    private static @Nullable SpellPlanner.Material material;
    private static @Nullable Object materialKey;
    private static @Nullable Object planKey;

    // The selected thing's flood (its picture, its bounds, the double click's "inside").
    private static @Nullable MagicSelect flood;
    private static List<BlockPos> floodSeeds = List.of();

    // The client's count of the plan.
    private static @Nullable SpellPlanner.Plan plan;
    private static @Nullable SpellPlanner.Count count;

    // The preflight.
    private static @Nullable Object stillKey;
    private static int stillTicks;
    private static @Nullable Object preflightKey;
    private static int preflightToken;
    private static final Set<Integer> PREFLIGHTS = new HashSet<>();
    private static long lastPreflight = Long.MIN_VALUE / 2;
    private static @Nullable BuildVerdictPayload preflight;

    // Casts and their verdicts.
    private static int token;
    private static final Map<Integer, SpellRecipe.Op> PENDING = new HashMap<>();
    private static @Nullable BuildVerdictPayload lastVerdict;
    private static long lastVerdictTick;

    // The hold ring, and the brush stroke.
    private static int holdFor;
    private static int held;
    private static int holdFlags;
    private static int stroke;
    private static boolean strokeOpen;
    private static @Nullable BlockPos lastDab;
    private static long lastDabTick;

    // The box others see.
    private static int sentView;
    private static BlockPos sentA = BlockPos.ZERO;
    private static BlockPos sentB = BlockPos.ZERO;
    private static long lastView = Long.MIN_VALUE / 2;

    // The pocket.
    private static final List<BuildPocketPayload> POCKET_PARTS = new ArrayList<>();
    private static @Nullable ClientPocket pocket;
    private static long pocketAsked = Long.MIN_VALUE / 2;

    // A picked block, and the hotbar slot and item it was picked over (taken a tick later, after vanilla's own pick).
    private static @Nullable BlockState picked;
    private static boolean pickSettled;
    private static int pickedSlot;
    private static Item pickedItem = Items.AIR;

    /** Whether {@code power} builds: the Scarlet Witch and Billy, the two heroes of Build Magic. */
    public static boolean isBuilder(Power power) {
        return power == Power.SCARLET_WITCH || power == Power.BILLY;
    }

    /**
     * Whether the local player is in build mode: what the {@link BuildKeys.Context#BUILD} keys and the panel's sixth
     * line follow. False for anyone who is not a Scarlet Witch or Billy, and for a Spectator.
     */
    public static boolean buildMode() {
        LocalPlayer player = Minecraft.getInstance().player;
        return mode && player != null && !player.isSpectator() && isBuilder(Powers.power(player));
    }

    /** The grammar of what the builder is about to cast (made at first use, with the numbers of the last tick). */
    public static BuildGrammar grammar() {
        if (grammar == null) {
            LocalPlayer player = Minecraft.getInstance().player;
            Limits now = limits != null ? limits : player != null ? Limits.of(player) : null;
            grammar = new BuildGrammar(now != null ? now : Limits.wanda(new BoundingBox(-30_000_000, -64, -30_000_000, 30_000_000, 319,
                    30_000_000)));
        }
        return grammar;
    }

    /** The client's ticks, counted while a player is in a world: the grammar's clock. */
    static long now() {
        return ticks;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Build mode

    /** A tap of Y: build mode on or off. */
    static void toggle() {
        setMode(!mode);
    }

    /** Build mode on or off, here at once and asked of the server ({@code MODE}). Off lets go of the box. */
    static void setMode(boolean on) {
        if (on == mode) {
            return;
        }
        mode = on;
        send(new BuildActionPayload(BuildActionPayload.MODE, on ? 1 : 0));
        if (!on) {
            leaveMode();
        }
    }

    private static void leaveMode() {
        mode = false;
        endStroke();
        stopHold();
        grammar().letGo();
    }

    /** The server's word on build mode, followed whenever it changes; also off for anyone who no longer builds. */
    private static void followServer(LocalPlayer player) {
        boolean now = player.getExistingData(ModAttachments.BUILD_VIEW).map(BuildView::active).orElse(false);
        if (player != seenPlayer) {
            // A new player entity (a respawn, a change of dimension) starts from what the server says now.
            seenPlayer = player;
            serverMode = now;
        } else if (now != serverMode) {
            serverMode = now;
            if (now) {
                mode = true;
            } else if (mode) {
                leaveMode();
            }
        }
        // A change of power ends build mode (D-B 11.9), between the two witches too; so does Spectator mode.
        Power power = Powers.power(player);
        if (mode && (player.isSpectator() || !isBuilder(power) || seenPower != null && power != seenPower)) {
            leaveMode();
        }
        seenPower = power;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Every tick

    /**
     * One client tick of a player ({@link BuildInput} calls it): the server's mode followed, a change of world lets go
     * of the box; in build mode the aim, the material and the recipe brought up to date, the flood and the count walked
     * on, the preflight sent when the plan is still, the box others see sent when it changed, a missing pocket asked.
     */
    static void tick(Minecraft mc, LocalPlayer player) {
        ticks++;
        followServer(player);
        if (mc.level != seenLevel) {
            // The box belongs to the world it was made in (D-B 3.2); build mode stays on.
            seenLevel = mc.level;
            forgetWorld();
        }
        if (!buildMode() || mc.level == null) {
            sendView(0, BlockPos.ZERO, BlockPos.ZERO);
            return;
        }
        update(player);
        BuildGrammar g = grammar();
        floodOn(mc.level, player, g);
        countOn(mc.level);
        preflightWhenStill();
        BoundingBox shown = g.box() != null ? g.box() : g.maskBounds();
        if (shown != null) {
            sendView(g.spell().id() + 1, new BlockPos(shown.minX(), shown.minY(), shown.minZ()),
                    new BlockPos(shown.maxX(), shown.maxY(), shown.maxZ()));
        } else {
            sendView(0, BlockPos.ZERO, BlockPos.ZERO);
        }
        askForPocket(player);
        backToTheHand(player);
    }

    /**
     * Brings the aim, the hero's numbers, the material and the recipe up to date: every tick, and at a click or a
     * scroll, which come between two ticks.
     */
    static void update(LocalPlayer player) {
        Limits now = Limits.of(player);
        if (now == null) {
            return;
        }
        limits = now;
        BuildGrammar g = grammar();
        g.limits(now);
        aim = aimOf(player, now);
        g.aim(aim);
        material = materialOf(player, aim, g);
        recipe = g.recipe(stroke);
        Object key = recipe == null ? null : List.of(asPlanned(recipe), Objects.requireNonNullElse(materialKey, "none"), floodSeeds,
                flood != null && flood.done(), pocket != null ? pocket.id() : 0);
        if (!Objects.equals(key, planKey)) {
            planKey = key;
            plan = null;
            count = null;
        }
    }

    /** Where the player looks, within the hero's reach: the aimed cell, its face, the picked block (D-B 3.1). */
    private static Aim aimOf(LocalPlayer player, Limits limits) {
        int reach = limits.reach();
        Vec3 eyes = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        BlockHitResult hit = Raycasts.block(player, reach, 1.0F);
        BlockHitResult pick = Raycasts.outline(player, reach, 1.0F);
        BlockPos cell = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
        BlockPos at = pick.getType() == HitResult.Type.BLOCK ? pick.getBlockPos() : null;
        return new Aim(cell, cell != null ? hit.getDirection() : null, at, eyes, look);
    }

    /** The cast's material as the server would read it: the hotbar's mix, the picked block, or the block in a hand. */
    private static @Nullable SpellPlanner.Material materialOf(LocalPlayer player, Aim aim, BuildGrammar g) {
        if (g.mix()) {
            List<ItemStack> hotbar = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) {
                hotbar.add(player.getInventory().getItem(i));
            }
            SpellMath.Mix mix = SpellMath.Mix.ofHotbar(hotbar, stack -> placed(player, stack, aim));
            materialKey = mix == null ? null : List.of(mix, g.seed());
            return mix == null ? null : SpellPlanner.Material.mix(mix, g.seed());
        }
        BlockState state = g.material() == SpellRecipe.PICKED && picked != null ? picked : inHand(player, aim);
        materialKey = state;
        return state == null ? null : SpellPlanner.Material.of(state);
    }

    /** The block in the main hand (else the off hand), in the state it would be placed in looking where he looks. */
    private static @Nullable BlockState inHand(LocalPlayer player, Aim aim) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof BlockItem) {
                return placed(player, stack, aim);
            }
        }
        return null;
    }

    /** A block item's block as a hand would place it here (D-B 5.1): stairs face away, logs follow the face's axis. */
    private static @Nullable BlockState placed(LocalPlayer player, ItemStack stack, Aim aim) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return null;
        }
        Block block = item.getBlock();
        BlockPos at = aim.target();
        Vec3 look = aim.look();
        Direction face = aim.face() != null ? aim.face() : Direction.getNearest(look.x, look.y, look.z).getOpposite();
        try {
            BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atCenterOf(at), face, at, false)));
            return state != null ? state : block.defaultBlockState();
        } catch (RuntimeException odd) {
            // A block whose placement wants more than a look at a cell: its plain state.
            return block.defaultBlockState();
        }
    }

    /** The selected thing's flood, {@value #FLOOD_PER_TICK} reads a tick; refused, it is let go and he is told why. */
    private static void floodOn(ClientLevel level, LocalPlayer player, BuildGrammar g) {
        if (!g.hasMask()) {
            if (flood != null) {
                flood = null;
                floodSeeds = List.of();
                g.selectedBy(null);
            }
            return;
        }
        if (flood == null || !floodSeeds.equals(g.seeds())) {
            floodSeeds = List.copyOf(g.seeds());
            MagicSelect started = Objects.requireNonNull(limits).select(level, floodSeeds, player.getEyePosition(1.0F), false);
            flood = started;
            g.selectedBy(pos -> started.found().contains(pos.asLong()));
        }
        if (flood.done()) {
            return;
        }
        flood.step(FLOOD_PER_TICK);
        if (!flood.done()) {
            return;
        }
        if (flood.refusal() == SpellPlanner.NONE) {
            g.maskBounds(flood.bounds());
            return;
        }
        // Over the limit nothing is selected, never a part of the thing (D-B 3.4).
        Component why = flood.message(Powers.power(player) == Power.SCARLET_WITCH);
        if (why != null) {
            player.displayClientMessage(why, true);
        }
        g.letGo();
        flood = null;
        floodSeeds = List.of();
        g.selectedBy(null);
    }

    /** The client's count of the plan against this world, {@value #COUNT_PER_TICK} cells a tick. */
    private static void countOn(ClientLevel level) {
        if (recipe == null || limits == null || aim == null || recipe.spell().isBrush()) {
            return;
        }
        if (plan == null) {
            if (recipe.region() == SpellRecipe.MASK && (flood == null || flood.mask() == null)) {
                return;
            }
            SpellPlanner.Setup setup = SpellPlanner.Setup.at(BlockPos.containing(aim.eyes()))
                    .withMask(flood != null ? flood.mask() : null)
                    .withMaterial(material)
                    .withSource(recipe.op() == SpellRecipe.Op.STAMP ? pocket : null)
                    .withSurface(limits.magicSelect(), limits.sideLimit());
            plan = SpellPlanner.plan(level, recipe.clamped(limits), setup);
        }
        if (!plan.prepared()) {
            plan.prepare(COUNT_PER_TICK);
            return;
        }
        if (count == null) {
            count = plan.count(level);
        }
        if (!count.done()) {
            count.step(COUNT_PER_TICK);
        }
    }

    /**
     * The recipe as far as the plan is concerned: the anchor (and stroke) of a spell that does not read it, which
     * follows the crosshair however still the box is, is left out.
     */
    private static SpellRecipe asPlanned(SpellRecipe r) {
        boolean readsAnchor = switch (r.spell()) {
            case FILL, REPEAT -> false;
            case VANISH -> r.vanishStyle() == VanishStyle.KIND;
            case CHANGE -> r.changeStyle() == ChangeStyle.KIND;
            case COPY -> r.op() == SpellRecipe.Op.STAMP;
            case MOVE -> r.op() == SpellRecipe.Op.PUT_DOWN;
            default -> true;
        };
        return new SpellRecipe(r.spell(), r.op(), r.style(), r.region(), r.a(), r.b(), r.seeds(), readsAnchor ? r.anchor() : BlockPos.ZERO,
                r.face(), r.size(), r.size2(), r.turn(), r.flip(), r.flags(), r.material(), r.seed(), 0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The preflight and the box others see

    private static void preflightWhenStill() {
        SpellRecipe now = recipe;
        Object key = now != null && !now.spell().isBrush() && ready() ? planKey : null;
        if (!Objects.equals(key, stillKey)) {
            stillKey = key;
            stillTicks = 0;
            return;
        }
        stillTicks++;
        if (key == null || stillTicks < PREFLIGHT_STILL || key.equals(preflightKey) || ticks - lastPreflight < PREFLIGHT_GAP) {
            return;
        }
        preflightKey = key;
        preflight = null;
        lastPreflight = ticks;
        preflightToken = nextToken();
        if (PREFLIGHTS.size() > MAX_PENDING) {
            PREFLIGHTS.clear();
        }
        PREFLIGHTS.add(preflightToken);
        send(new BuildCastPayload(preflightToken, BuildCastPayload.PREFLIGHT, now));
    }

    /** The box others see ({@code VIEW}): sent when it changes, at most 4 times a second. */
    private static void sendView(int value, BlockPos a, BlockPos b) {
        if (value == sentView && a.equals(sentA) && b.equals(sentB) || ticks - lastView < VIEW_GAP) {
            return;
        }
        sentView = value;
        sentA = a;
        sentB = b;
        lastView = ticks;
        send(new BuildActionPayload(BuildActionPayload.VIEW, a, b, value));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The left button and the wheel

    /** The left button went down in build mode (the grammar takes it). */
    static void leftDown(LocalPlayer player) {
        update(player);
        if (aim != null) {
            BuildFeedback.heard(grammar().press(aim, ticks), grammar());
            update(player);
        }
    }

    /** The left button came up. */
    static void leftUp(LocalPlayer player) {
        update(player);
        if (aim != null) {
            BuildFeedback.heard(grammar().release(aim, ticks), grammar());
            update(player);
        }
    }

    /** The scroll wheel in build mode; {@link BuildGrammar.Result#UNUSED} leaves it to the hotbar. */
    static BuildGrammar.Result scroll(LocalPlayer player, int notches, boolean fast, boolean second) {
        update(player);
        if (aim == null) {
            return BuildGrammar.Result.UNUSED;
        }
        BuildGrammar.Result result = grammar().scroll(notches, fast, second, aim);
        BuildFeedback.heard(result, grammar());
        update(player);
        return result;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The right button

    /** What the right button would cast now (null: nothing ready, the click stays vanilla's). */
    public static @Nullable SpellRecipe recipe() {
        return recipe;
    }

    /**
     * Whether the spell has something ready for the right button (D-B 11.10): a box or a thing for a box spell, a
     * ghost, a face for Pull, a block in reach for a brush, and a material for the spells that conjure one. Otherwise
     * the right button is vanilla's (doors, eating, placing a block).
     */
    public static boolean ready() {
        return recipe != null && (!recipe.spell().needsMaterial() || material != null);
    }

    /**
     * How long the right button must be held before this cast goes (0: it goes at the click): the long hold for a plan
     * over {@code hugeSpellBlocks} or one the server said needs it, the short hold while the client's count says more
     * than {@code holdToConfirmBlocks} existing blocks are removed or replaced, or is not done yet.
     */
    public static int holdTicks() {
        if (recipe == null || recipe.spell().isBrush()) {
            return 0;
        }
        if (plan == null || !plan.prepared()) {
            return shortHold();
        }
        if (plan.refusal() != SpellPlanner.NONE || !plan.writes()) {
            return 0;
        }
        boolean serverSays = preflight() != null && (preflight.code() == BuildVerdictPayload.HOLD
                || preflight.code() == BuildVerdictPayload.NO_UNDO_ROOM);
        if (serverSays || plan.sizeBound() > config(BuildConfig.HUGE_SPELL_BLOCKS, 1_000_000)) {
            return longHold();
        }
        if (count == null || !count.done() || count.removed() > config(BuildConfig.HOLD_TO_CONFIRM_BLOCKS, 64)) {
            return shortHold();
        }
        return 0;
    }

    private static int shortHold() {
        return config(BuildConfig.HOLD_TO_CONFIRM_TICKS, 20);
    }

    private static int longHold() {
        return Math.max(shortHold(), config(BuildConfig.HUGE_HOLD_TICKS, 40));
    }

    /** Whether the right button must be held for the spell ready now (the hint strip's held row). */
    public static boolean needsHold() {
        return ready() && holdTicks() > 0;
    }

    /**
     * The right button went down with something ready: a brush starts its stroke, a cast that needs no hold goes, one
     * that does starts the ring (held, it goes when the ring is full; let go earlier, nothing is sent).
     */
    static void rightDown(LocalPlayer player) {
        update(player);
        if (recipe == null) {
            return;
        }
        if (recipe.spell().isBrush()) {
            startStroke();
            return;
        }
        // A plan just changed (a scroll, Delete's Vanish) is counted now, so a small spell needs no ring.
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && (count == null || !count.done())) {
            countOn(level);
            countOn(level);
        }
        int need = holdTicks();
        if (need <= 0) {
            cast(player, 0);
            return;
        }
        holdFor = need;
        held = 0;
        holdFlags = need >= longHold() && longHold() > shortHold() ? BuildCastPayload.HELD_SHORT | BuildCastPayload.HELD_LONG
                : BuildCastPayload.HELD_SHORT;
        BuildFeedback.holding(0, need);
    }

    /** One tick with the right button still down: the ring fills (and casts when full), a brush dabs. */
    static void rightHeld(LocalPlayer player) {
        if (strokeOpen) {
            dab(player);
            return;
        }
        if (holdFor <= 0) {
            return;
        }
        held++;
        BuildFeedback.holding(held, holdFor);
        if (held >= holdFor) {
            int flags = holdFlags;
            stopHold();
            update(player);
            cast(player, flags);
        }
    }

    /** The right button came up: a stroke ends; a ring not yet full sends nothing (a soft unwind). */
    static void rightUp() {
        endStroke();
        if (holdFor > 0) {
            BuildFeedback.unwound();
        }
        stopHold();
    }

    /** How full the hold ring is (0 to 1), or -1 with no ring (the HUD draws it). */
    public static float holdProgress() {
        return holdFor > 0 ? Math.min(1.0F, held / (float) holdFor) : -1.0F;
    }

    private static void stopHold() {
        holdFor = 0;
        held = 0;
        holdFlags = 0;
    }

    /**
     * Casts the current recipe with these flags ({@code HELD_SHORT}, {@code HELD_LONG}): Copy's take shows the pocket's
     * ghost, Move's pick-up the ghost of what it lifts, the put-down ends the Move; a mix rolls a new seed for the next
     * spell.
     */
    static void cast(LocalPlayer player, int flags) {
        SpellRecipe now = recipe;
        if (now == null) {
            return;
        }
        int sent = nextToken();
        if (PENDING.size() > MAX_PENDING) {
            PENDING.clear();
        }
        PENDING.put(sent, now.op());
        send(new BuildCastPayload(sent, flags, now));
        BuildGrammar g = grammar();
        switch (now.op()) {
            case TAKE, PICK_UP -> {
                g.holdGhost();
                BuildFeedback.copied();
            }
            case PUT_DOWN -> g.letGo();
            default -> {
            }
        }
        if (g.mix()) {
            g.mix(true, player.getRandom().nextLong());
        }
        update(player);
    }

    private static void startStroke() {
        stroke++;
        strokeOpen = true;
        lastDab = null;
        lastDabTick = Long.MIN_VALUE / 2;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            dab(player);
        }
    }

    /** A dab of the open stroke: Paint's when its brush moved (at most one a tick), Land's and Grow's every pulse. */
    private static void dab(LocalPlayer player) {
        update(player);
        if (recipe == null || recipe.op() != SpellRecipe.Op.DAB || recipe.stroke() != stroke) {
            return;
        }
        boolean moved = !recipe.anchor().equals(lastDab);
        boolean pulse = recipe.spell() != Spell.PAINT && ticks - lastDabTick >= PULSE_TICKS;
        if (!moved && !pulse) {
            return;
        }
        lastDab = recipe.anchor();
        lastDabTick = ticks;
        send(new BuildCastPayload(nextToken(), 0, recipe));
    }

    private static void endStroke() {
        if (strokeOpen) {
            strokeOpen = false;
            send(new BuildActionPayload(BuildActionPayload.STROKE_END, stroke));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The other keys

    /** Ctrl + Z (also out of build mode, R42): undo, or stop and take back a working spell, or drop a waiting one. */
    static void undo() {
        send(new BuildActionPayload(BuildActionPayload.UNDO, 0));
    }

    /** Ctrl + Y (also out of build mode): redo. */
    static void redo() {
        send(new BuildActionPayload(BuildActionPayload.REDO, 0));
    }

    /**
     * Middle click: the block under the picking ray becomes the material as it is (D-B 5.2). Only where it is goes to
     * the server, which reads the block itself; this client shows the one it sees. Vanilla pick-block still happens.
     */
    static void pick(LocalPlayer player) {
        update(player);
        if (aim == null || aim.picked() == null) {
            return;
        }
        picked = player.level().getBlockState(aim.picked());
        pickSettled = false;
        grammar().material(SpellRecipe.PICKED);
        send(new BuildActionPayload(BuildActionPayload.PICK, aim.picked(), aim.picked(), 0));
    }

    /**
     * A picked material lasts until the hotbar slot or the held item changes: then the hand again ({@code HAND}). The
     * slot and the item are taken the tick after the pick, once vanilla's own pick-block has put the block in the hand.
     */
    private static void backToTheHand(LocalPlayer player) {
        BuildGrammar g = grammar();
        if (g.material() != SpellRecipe.PICKED) {
            return;
        }
        int slot = player.getInventory().selected;
        Item item = player.getMainHandItem().getItem();
        if (!pickSettled) {
            pickSettled = true;
            pickedSlot = slot;
            pickedItem = item;
            return;
        }
        if (slot != pickedSlot || item != pickedItem) {
            g.material(SpellRecipe.HAND);
            picked = null;
            send(new BuildActionPayload(BuildActionPayload.HAND, 0));
        }
    }

    /** Chooses a spell; Copy chosen with no box brings the pocket's ghost back (D-B 4.8). */
    static void choose(LocalPlayer player, Spell spell) {
        BuildGrammar g = grammar();
        g.spell(spell);
        if (spell == Spell.COPY && !g.finished() && pocketId(player) != 0) {
            g.holdGhost();
        }
        update(player);
    }

    /** Ctrl + V: the pocket's ghost back, or "Copy something first". */
    static void paste(LocalPlayer player) {
        BuildGrammar g = grammar();
        g.spell(Spell.COPY);
        if (pocketId(player) == 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.build.pocket_empty"), true);
            return;
        }
        g.holdGhost();
        update(player);
    }

    private static int pocketId(LocalPlayer player) {
        return player.getExistingData(ModAttachments.BUILD_DATA).map(BuildData::pocket).orElse(0);
    }

    /** Asks the server for the pocket's copy this client lacks (after a relog), now and then while it lacks it. */
    private static void askForPocket(LocalPlayer player) {
        int id = pocketId(player);
        if (id == 0 || pocket != null && pocket.id() == id || ticks - pocketAsked < POCKET_ASK_GAP) {
            return;
        }
        if (!POCKET_PARTS.isEmpty() && POCKET_PARTS.get(0).pocket() == id) {
            return;
        }
        pocketAsked = ticks;
        send(new BuildActionPayload(BuildActionPayload.POCKET, id));
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the server sends

    /**
     * The server's verdict on a cast, a preflight, an undo or a redo. A preflight's (its token) is kept for the plan it
     * was asked about; any other is the last verdict (the HUD's done pop); a refused take or pick-up drops its ghost; a
     * refusal of build mode itself ({@code OFF}, {@code OPERATORS}, {@code ADVENTURE}) turns it off.
     */
    public static void onVerdict(BuildVerdictPayload payload) {
        if (payload.token() != 0 && PREFLIGHTS.remove(payload.token())) {
            if (payload.token() == preflightToken) {
                preflight = payload;
            }
            return;
        }
        lastVerdict = payload;
        lastVerdictTick = ticks;
        SpellRecipe.Op op = PENDING.remove(payload.token());
        if (!BuildVerdictPayload.isRefusal(payload.code())) {
            return;
        }
        if (op == SpellRecipe.Op.TAKE || op == SpellRecipe.Op.PICK_UP) {
            grammar().dropGhost();
        }
        int code = payload.code();
        if (mode && (code == BuildVerdictPayload.OFF || code == BuildVerdictPayload.OPERATORS || code == BuildVerdictPayload.ADVENTURE)) {
            leaveMode();
        }
    }

    /** One part of the copy in the pocket: the parts are gathered and, once all are here, the copy is kept. */
    public static void onPocketPart(BuildPocketPayload payload) {
        if (!POCKET_PARTS.isEmpty() && POCKET_PARTS.get(0).pocket() != payload.pocket()) {
            POCKET_PARTS.clear();
        }
        POCKET_PARTS.removeIf(part -> part.part() == payload.part());
        POCKET_PARTS.add(payload);
        BuildPocketPayload.Whole whole = BuildPocketPayload.join(POCKET_PARTS);
        if (whole == null) {
            return;
        }
        POCKET_PARTS.clear();
        pocket = ClientPocket.of(whole);
        planKey = null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the HUD and the renderer read

    /** The verdict of the last preflight, while the plan is the one it was asked about; else null. */
    public static @Nullable BuildVerdictPayload preflight() {
        return Objects.equals(planKey, preflightKey) ? preflight : null;
    }

    /** The last verdict on a cast, an undo or a redo. */
    public static Optional<BuildVerdictPayload> lastVerdict() {
        return Optional.ofNullable(lastVerdict);
    }

    /** How many ticks ago the last verdict came. */
    public static long ticksSinceVerdict() {
        return ticks - lastVerdictTick;
    }

    /** The client's count of the current plan (null before it starts). */
    public static @Nullable SpellPlanner.Count count() {
        return count;
    }

    /** The current plan (null when nothing is ready, or a selected thing is still flooding). */
    public static @Nullable SpellPlanner.Plan plan() {
        return plan;
    }

    /** The material of the current plan (null with none in hand). */
    public static @Nullable SpellPlanner.Material material() {
        return material;
    }

    /** The selected thing's flood (null with no selected thing). */
    public static @Nullable MagicSelect flood() {
        return flood;
    }

    /** This client's copy of the pocket (null until one came). */
    public static @Nullable SpellPlanner.Source pocket() {
        return pocket;
    }

    /** Where the builder looked at the last update. */
    public static @Nullable Aim aim() {
        return aim;
    }

    /** The hero's numbers at the last update. */
    public static @Nullable Limits limits() {
        return limits;
    }

    /** The hint strip's stage now: the grammar's, with a box whose spell needs holding made {@code BOX_HOLD}. */
    public static BuildHints.Stage stage() {
        BuildHints.Stage stage = grammar().stage();
        return stage == BuildHints.Stage.BOX && needsHold() ? BuildHints.Stage.BOX_HOLD : stage;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Forgetting

    /** A new world (a change of dimension): the box, the selected thing, the ghost and the plan go; the mode stays. */
    private static void forgetWorld() {
        endStroke();
        stopHold();
        if (grammar != null) {
            grammar.letGo();
        }
        flood = null;
        floodSeeds = List.of();
        plan = null;
        count = null;
        planKey = null;
        stillKey = null;
        preflightKey = null;
        preflight = null;
        aim = null;
        recipe = null;
    }

    /** Forgets everything of the builder (at logout): build mode, the grammar, the pocket's copy, the verdicts. */
    public static void clear() {
        strokeOpen = false;
        forgetWorld();
        mode = false;
        serverMode = false;
        seenPlayer = null;
        seenLevel = null;
        seenPower = null;
        grammar = null;
        limits = null;
        material = null;
        materialKey = null;
        PENDING.clear();
        PREFLIGHTS.clear();
        lastVerdict = null;
        token = 0;
        preflightToken = 0;
        stroke = 0;
        sentView = 0;
        sentA = BlockPos.ZERO;
        sentB = BlockPos.ZERO;
        POCKET_PARTS.clear();
        pocket = null;
        pocketAsked = Long.MIN_VALUE / 2;
        lastPreflight = Long.MIN_VALUE / 2;
        lastView = Long.MIN_VALUE / 2;
        picked = null;
        pickSettled = false;
    }

    private static int nextToken() {
        token = token == Integer.MAX_VALUE ? 1 : token + 1;
        return token;
    }

    private static void send(CustomPacketPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(payload);
        }
    }

    private static int config(ModConfigSpec.IntValue value, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    private BuildClient() {
    }
}

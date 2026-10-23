package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.build.BillyMagic;
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
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.network.BuildPocketPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
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
 * {@code HOLD} or {@code NO_UNDO_ROOM}, {@code hugeHoldTicks}. The material is read exactly as the server reads it
 * (the mix of the blocks magic may make, the picked block, the hand's block placed at the recipe's anchor and face), so
 * the count and the ghost ({@link Ghost}) are what the server will build; after a spell of his ends, the plan is made
 * and counted again against the world it changed.
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
    /** How far beyond his reach a block still counts as "too far" rather than the sky (D-B 8.4). */
    public static final int TOO_FAR_RANGE = 64;
    /** Ticks between two of the words the client says on the action bar by itself (too far, too big). */
    private static final int SAY_GAP = 25;
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
    /** Ticks after a tap of Y during which a server word that disagrees with it is the echo of an earlier tap. */
    private static final int MODE_ECHO_TICKS = 40;
    private static boolean askedMode;
    private static long askedAt = Long.MIN_VALUE / 2;
    /** The tick the client last spoke on the action bar by itself (tellWhyHeld). */
    private static long lastSaid = Long.MIN_VALUE / 2;

    private static @Nullable BuildGrammar grammar;
    private static @Nullable Aim aim;
    private static @Nullable Limits limits;
    /** The current recipe (null: nothing ready), its material, and the key that says when the plan changes. */
    private static @Nullable SpellRecipe recipe;
    private static @Nullable SpellPlanner.Material material;
    private static @Nullable Object materialKey;
    private static @Nullable Object planKey;
    /**
     * What the ghost shows ({@link BuildGrammar#preview}: the recipe, or while nothing is ready, a rubber band's spell),
     * and the key and the plan of a rubber band's spell (never counted: its label shows the rubber band's size).
     */
    private static @Nullable SpellRecipe preview;
    private static @Nullable Object previewKey;
    private static @Nullable SpellPlanner.Plan previewPlan;
    /** How many of the builder's spells ended (done, undone, redone, stopped): plans are made again after each. */
    private static int epoch;

    // The selected thing's flood (its picture, its bounds, the double click's "inside").
    private static @Nullable MagicSelect flood;
    private static List<BlockPos> floodSeeds = List.of();
    /** Set when the thing is found again after a spell changed the world: if it is gone, it is let go without a word. */
    private static boolean quietFlood;

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
    /** The plan and the kind of hold the ring in progress was begun for. */
    private static @Nullable Object ringKey;
    private static int ringKind;
    /** The number of the open (or last) stroke: it starts from the clock, as the server's session keeps the last it saw. */
    private static int stroke = strokeStart();
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
    /** The item a palette block stands for (its placement state is read at the recipe's anchor); empty after a middle click. */
    private static ItemStack paletteStack = ItemStack.EMPTY;
    private static boolean pickSettled;
    private static int pickedSlot;
    private static Item pickedItem = Items.AIR;

    /** Whether {@code power} builds: the Scarlet Witch and Billy, the two heroes of Build Magic. */
    public static boolean isBuilder(Power power) {
        return power == Power.SCARLET_WITCH || power == Power.BILLY;
    }

    /**
     * Whose Build Magic this player has, for everything the client draws and plays of it: the Scarlet Witch's, Billy's, or
     * (3.4) Billy's for a custom hero with his Build Magic (his rules, his looks and sounds); for anyone else his own power,
     * which {@link #isBuilder} refuses.
     */
    public static Power hero(Player player) {
        PowerData data = Powers.get(player);
        if (data.power() == Power.CUSTOM && data.has(HeroAbility.WANDA_BUILD_MAGIC)) {
            return Power.SCARLET_WITCH; // (3.5: the Scarlet Witch's Build Magic, her looks and her Instant)
        }
        return data.power() == Power.CUSTOM && data.has(HeroAbility.BILLY_BUILD_MAGIC) ? Power.BILLY : data.power();
    }

    /**
     * Whether the local player is in build mode: what the {@link BuildKeys.Context#BUILD} keys and the panel's sixth
     * line follow. False for anyone who is not a Scarlet Witch or Billy, and for a Spectator.
     */
    public static boolean buildMode() {
        LocalPlayer player = Minecraft.getInstance().player;
        return mode && player != null && !player.isSpectator() && isBuilder(hero(player));
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
        askedMode = on;
        askedAt = ticks;
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
            // A word that disagrees with the tap just made is the echo of an earlier tap: the answer to this one is to come.
            boolean echo = now != mode && now != askedMode && ticks - askedAt < MODE_ECHO_TICKS;
            if (echo) {
                // Nothing to follow.
            } else if (now) {
                mode = true;
            } else if (mode) {
                leaveMode();
            }
        }
        // A change of power ends build mode (D-B 11.9), between the two witches too; so does Spectator mode.
        Power power = hero(player);
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
            Ghost.clear();
            return;
        }
        update(player);
        BuildGrammar g = grammar();
        floodOn(mc.level, player, g);
        countOn(mc.level);
        previewOn(mc.level);
        Ghost.tick(mc.level, player);
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
     * Brings the aim, the hero's numbers, the recipe and its material up to date: every tick, and at a click or a
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
        recipe = g.recipe(stroke);
        preview = recipe != null ? recipe : g.preview(stroke);
        material = materialOf(player, preview, aim, g);
        Object key = recipe == null ? null : planKeyOf(recipe);
        if (!Objects.equals(key, planKey)) {
            planKey = key;
            plan = null;
            count = null;
        }
        Object shown = recipe != null || preview == null ? null : planKeyOf(preview);
        if (!Objects.equals(shown, previewKey)) {
            previewKey = shown;
            previewPlan = null;
        }
    }

    /** What a plan of {@code r} is made of: when it changes, the plan, its count and its ghost are made again. */
    private static Object planKeyOf(SpellRecipe r) {
        return List.of(asPlanned(r), Objects.requireNonNullElse(materialKey, "none"), floodSeeds, flood != null && flood.done(),
                usablePocket() != null ? pocket.id() : 0, epoch);
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
        // Billy's reach is his lightning's: a block in sight beyond it is "too far", where only a true sky is the floating cell.
        boolean far = cell == null && at == null && limits.limited()
                && Raycasts.block(player, reach + TOO_FAR_RANGE, 1.0F).getType() == HitResult.Type.BLOCK;
        return new Aim(cell, cell != null ? hit.getDirection() : null, at, eyes, look, far);
    }

    /**
     * The cast's material exactly as the server reads it (D-B 5, C8), so that the ghost and the count are what the
     * server builds: the hotbar's mix (of the blocks magic may make, each weighted by its stack), else the picked
     * block, else the block in the main hand, else the off hand, each in the state it would be placed in at the
     * recipe's anchor and face; a bucket makes its fluid.
     */
    private static @Nullable SpellPlanner.Material materialOf(LocalPlayer player, @Nullable SpellRecipe r, Aim aim, BuildGrammar g) {
        if (g.mix()) {
            List<ItemStack> hotbar = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) {
                hotbar.add(player.getInventory().getItem(i));
            }
            SpellMath.Mix mix = SpellMath.Mix.ofHotbar(hotbar, stack -> {
                BlockState state = placed(player, InteractionHand.MAIN_HAND, stack, r, aim);
                return state != null && conjurable(state) ? state : null;
            });
            materialKey = mix == null ? null : List.of(mix, g.seed());
            return mix == null ? null : SpellPlanner.Material.mix(mix, g.seed());
        }
        BlockState state = g.material() != SpellRecipe.PICKED ? null : !paletteStack.isEmpty()
                ? placed(player, InteractionHand.MAIN_HAND, paletteStack, r, aim) : picked;
        if (state == null) {
            state = placed(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), r, aim);
        }
        if (state == null) {
            state = placed(player, InteractionHand.OFF_HAND, player.getOffhandItem(), r, aim);
        }
        materialKey = state;
        return state == null ? null : SpellPlanner.Material.of(state);
    }

    /**
     * The block an item makes as the server reads it (D-B 5.1, 5.5): a block item's block as a hand would place it at
     * the recipe's anchor, clicking its face (else the top; with no recipe yet, where he looks), so stairs face away
     * and logs follow the face's axis, its plain state when that cell or the one in front is not loaded; a bucket's
     * fluid as its source block; null for anything else.
     */
    private static @Nullable BlockState placed(LocalPlayer player, InteractionHand hand, ItemStack stack, @Nullable SpellRecipe r, Aim aim) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof BucketItem bucket && bucket.content != Fluids.EMPTY) {
            return bucket.content.defaultFluidState().createLegacyBlock();
        }
        if (!(stack.getItem() instanceof BlockItem item)) {
            return null;
        }
        Block block = item.getBlock();
        BlockPos at = r != null ? r.anchor() : aim.target();
        Direction face = r != null ? Objects.requireNonNullElse(r.direction(), Direction.UP) : Objects.requireNonNullElse(aim.face(), Direction.UP);
        if (!SpellMath.loaded(player.level(), at) || !SpellMath.loaded(player.level(), at.relative(face))) {
            return block.defaultBlockState();
        }
        try {
            BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, hand, stack, new BlockHitResult(Vec3.atCenterOf(at), face, at,
                    false)));
            return state != null ? state : block.defaultBlockState();
        } catch (RuntimeException odd) {
            // A block whose placement wants more than a look at a cell: its plain state.
            return block.defaultBlockState();
        }
    }

    /**
     * Whether magic may make the block, as the server checks a material (C8): not air, not a block magic never makes
     * or never touches, not a block of two cells, and TNT, lava or fire only for a hero who may make them. A mix takes
     * only these, as the server's does.
     */
    static boolean conjurable(BlockState state) {
        if (state.isAir() || state.is(ModTags.BUILD_UNCONJURABLE) || SpellMath.untouchable(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)) {
            return false;
        }
        return limits == null || limits.volatileAllowed() || !state.is(ModTags.BUILD_VOLATILE);
    }

    /** The selected thing's flood, {@value #FLOOD_PER_TICK} reads a tick; refused, it is let go and he is told why. */
    private static void floodOn(ClientLevel level, LocalPlayer player, BuildGrammar g) {
        if (g.ghost() && g.hasMask()) {
            // The ghost is the pocket's copy, which stays for the next stamp (D-B 4.8): the thing it was copied from is not
            // found again after each stamp (a copy stamped beside it makes one bigger thing), nor let go with the ghost.
            return;
        }
        if (!g.hasMask()) {
            quietFlood = false;
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
            quietFlood = false;
            return;
        }
        // Over the limit nothing is selected, never a part of the thing (D-B 3.4).
        Component why = flood.message(hero(player) == Power.SCARLET_WITCH);
        if (why != null && !quietFlood) {
            player.displayClientMessage(why, true);
        }
        g.letGo();
        flood = null;
        floodSeeds = List.of();
        g.selectedBy(null);
    }

    /**
     * The client's count of the plan against this world, {@value #COUNT_PER_TICK} cells a tick. A brush's dab is
     * planned for its ghost (the blocks under the brush) and never counted: it costs what its stroke paints.
     */
    private static void countOn(ClientLevel level) {
        if (recipe == null || limits == null || aim == null) {
            return;
        }
        if (plan == null) {
            if (recipe.region() == SpellRecipe.MASK && (flood == null || flood.mask() == null)) {
                return;
            }
            plan = SpellPlanner.plan(level, recipe.clamped(limits), setup(recipe, BlockPos.containing(aim.eyes())));
        }
        if (recipe.spell().isBrush()) {
            return;
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

    /** The plan of the rubber band's spell, for its ghost: made once for each change, never counted. */
    private static void previewOn(ClientLevel level) {
        if (previewKey != null && previewPlan == null && preview != null && limits != null && aim != null) {
            previewPlan = SpellPlanner.plan(level, preview.clamped(limits), setup(preview, BlockPos.containing(aim.eyes())));
        }
    }

    /**
     * What a plan of {@code r} needs besides the recipe, as the server gives it (its job's caster and ticket window
     * change only the order of the cells): the selected thing, the material, the pocket for a stamp, and the hero's
     * cap and side for a Pull's surface. {@code caster}: where the work order starts.
     */
    static SpellPlanner.Setup setup(SpellRecipe r, BlockPos caster) {
        SpellPlanner.Setup setup = SpellPlanner.Setup.at(caster)
                .withMask(flood != null ? flood.mask() : null)
                .withMaterial(material)
                .withSource(r.op() == SpellRecipe.Op.STAMP ? usablePocket() : null);
        return limits == null ? setup : setup.withSurface(limits.magicSelect(), limits.sideLimit());
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
            BuildGrammar.Result result = grammar().press(aim, ticks);
            BuildFeedback.heard(result, grammar());
            tellWhyHeld(player, result);
            update(player);
        }
    }

    /**
     * What a limited builder is told when his click or scroll was stopped (D-B 8.4): a wall beyond his reach does
     * nothing ("Too far for your magic"), a side at his longest stops ("Too big for you yet"); a few seconds apart at most.
     */
    private static void tellWhyHeld(LocalPlayer player, BuildGrammar.Result result) {
        Limits now = limits;
        if (now == null || !now.limited() || ticks - lastSaid < SAY_GAP) {
            return;
        }
        BuildGrammar g = grammar();
        if (result == BuildGrammar.Result.TOO_FAR) {
            say(player, Limits.tooFar(now.reach()));
        } else if (g.sideHeld() && (result == BuildGrammar.Result.HELD_BACK || result == BuildGrammar.Result.CORNER_B
                || result == BuildGrammar.Result.DRAGGED)) {
            say(player, Limits.tooWide(now.side()));
        }
    }

    private static void say(LocalPlayer player, Component words) {
        lastSaid = ticks;
        player.displayClientMessage(words, true);
    }

    /** The left button came up. */
    static void leftUp(LocalPlayer player) {
        update(player);
        if (aim != null) {
            BuildGrammar.Result result = grammar().release(aim, ticks);
            BuildFeedback.heard(result, grammar());
            tellWhyHeld(player, result);
            update(player);
        }
    }

    /** The scroll wheel in build mode; {@link BuildGrammar.Result#UNUSED} leaves it to the hotbar. */
    static BuildGrammar.Result scroll(LocalPlayer player, int notches, boolean fast, boolean second) {
        update(player);
        if (aim == null) {
            return BuildGrammar.Result.UNUSED;
        }
        BuildGrammar.Result result = grammar().scroll(notches, fast, second, aim, ticks);
        BuildFeedback.heard(result, grammar());
        tellWhyHeld(player, result);
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
        return switch (holdKind()) {
            case HOLD_SHORT -> shortHold();
            case HOLD_LONG -> longHold();
            default -> 0;
        };
    }

    /** No hold, the short one (more than {@code holdToConfirmBlocks} removed or replaced, or not known yet), the long one. */
    private static final int HOLD_NONE = 0;
    private static final int HOLD_SHORT = 1;
    private static final int HOLD_LONG = 2;

    /**
     * Which hold the plan ready now asks for: the long one for a plan over {@code hugeSpellBlocks} or one the server said
     * needs it; the short one while the count is not done, when more than {@code holdToConfirmBlocks} existing blocks are
     * removed or replaced (the larger of the client's count and the server's preflight), or when some of the plan lies in
     * chunks this client has not loaded (what is there is not known).
     */
    private static int holdKind() {
        if (recipe == null || recipe.spell().isBrush()) {
            return HOLD_NONE;
        }
        if (plan == null || !plan.prepared()) {
            return HOLD_SHORT;
        }
        if (plan.refusal() != SpellPlanner.NONE || !plan.writes()) {
            return HOLD_NONE;
        }
        BuildVerdictPayload server = preflight();
        boolean serverSays = server != null && (server.code() == BuildVerdictPayload.HOLD || server.code() == BuildVerdictPayload.NO_UNDO_ROOM);
        if (serverSays || plan.sizeBound() > config(BuildConfig.HUGE_SPELL_BLOCKS, 1_000_000)) {
            return HOLD_LONG;
        }
        if (count == null || !count.done() || count.unloaded() > 0) {
            return HOLD_SHORT;
        }
        long removed = count.removed();
        if (server != null && (server.code() == BuildVerdictPayload.READY || server.code() == BuildVerdictPayload.READY_PARTLY)) {
            removed = Math.max(removed, server.extra());
        }
        return removed > config(BuildConfig.HOLD_TO_CONFIRM_BLOCKS, 64) ? HOLD_SHORT : HOLD_NONE;
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
        int kind = holdKind();
        int flags = kind == HOLD_LONG ? BuildCastPayload.HELD_SHORT | BuildCastPayload.HELD_LONG
                : kind == HOLD_SHORT ? BuildCastPayload.HELD_SHORT : 0;
        int need = holdTicks();
        if (need <= 0) {
            cast(player, flags);
            return;
        }
        holdFor = need;
        held = 0;
        holdFlags = flags;
        ringKey = planKey;
        ringKind = kind;
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
        // The ring confirms the plan it began for: a scroll or a swing that changed it, or made the hold stricter,
        // lets it go; press again to confirm the new one.
        if (!Objects.equals(planKey, ringKey) || holdKind() > ringKind) {
            BuildFeedback.unwound();
            BuildHud.unwound(holdProgress(), holdHuge());
            stopHold();
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
            BuildHud.unwound(holdProgress(), holdHuge());
        }
        stopHold();
    }

    /** How full the hold ring is (0 to 1), or -1 with no ring (the HUD draws it). */
    public static float holdProgress() {
        return holdFor > 0 ? Math.min(1.0F, held / (float) holdFor) : -1.0F;
    }

    /** Whether the ring being held is the long one of a huge spell (the HUD draws it in the hero's colour, not red). */
    public static boolean holdHuge() {
        return (holdFlags & BuildCastPayload.HELD_LONG) != 0;
    }

    private static void stopHold() {
        ringKey = null;
        ringKind = HOLD_NONE;
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

    /** A number no earlier login of this client used: the server keeps the last stroke it saw for the whole run. */
    private static int strokeStart() {
        return (int) (System.nanoTime() >>> 20) & 0x3FFFFFFF;
    }

    private static void startStroke() {
        stroke++;
        // Each stroke has its own seed (Grow's bloom, a mix): the server grows what this stroke showed.
        LocalPlayer owner = Minecraft.getInstance().player;
        if (owner != null) {
            BuildGrammar g = grammar();
            g.mix(g.mix(), owner.getRandom().nextLong());
        }
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

    /**
     * Ctrl + I or the wheel's button (3.2): the Scarlet Witch's Instant switch, on if it is off and off if it is on. The
     * server decides (it tells Billy the switch is hers, and turns it down where {@code instantMaxBlocks} is 0); the badge
     * and the button show what {@code BuildData.instant} says.
     */
    static void toggleInstant(LocalPlayer player) {
        boolean on = player.getData(ModAttachments.BUILD_DATA).instant();
        send(new BuildActionPayload(BuildActionPayload.INSTANT, on ? 0 : 1));
    }

    /** Whether the Instant switch is on for the local player (the server's word, synced). */
    public static boolean instant(LocalPlayer player) {
        return player.getData(ModAttachments.BUILD_DATA).instant();
    }

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
        paletteStack = ItemStack.EMPTY;
        pickSettled = false;
        BuildGrammar g = grammar();
        g.material(SpellRecipe.PICKED);
        // A picked block is the material whatever the mix was.
        g.mix(false, g.seed());
        BlockPaletteScreen.used(picked.getBlock());
        send(new BuildActionPayload(BuildActionPayload.PICK, aim.picked(), aim.picked(), 0));
    }

    /**
     * A block chosen on the palette page (D-B 5.4): the material, in the state a hand would place it in (the server
     * reads it from its registry id and does the same); it lasts until the hotbar slot or the held item changes, as a
     * middle click's does.
     */
    static void pickBlock(LocalPlayer player, Block block) {
        Item item = block.asItem();
        if (item == Items.AIR) {
            return;
        }
        paletteStack = new ItemStack(item);
        picked = block.defaultBlockState();
        pickSettled = false;
        BuildGrammar g = grammar();
        g.material(SpellRecipe.PICKED);
        g.mix(false, g.seed());
        BlockPaletteScreen.used(block);
        update(player);
        send(new BuildActionPayload(BuildActionPayload.PICK_BLOCK, BuiltInRegistries.BLOCK.getId(block)));
    }

    /** The wheel's hotbar row: that slot's block is the material (the hand again, the mix off). */
    static void useSlot(LocalPlayer player, int slot) {
        BuildGrammar g = grammar();
        g.mix(false, g.seed());
        player.getInventory().selected = slot;
        paletteStack = ItemStack.EMPTY;
        picked = null;
        if (g.material() == SpellRecipe.PICKED) {
            g.material(SpellRecipe.HAND);
            send(new BuildActionPayload(BuildActionPayload.HAND, 0));
        }
        update(player);
    }

    /** The wheel's Mix switch: every cell of a Fill, Change, Paint or Shape gets one of the hotbar's blocks. */
    static void setMix(LocalPlayer player, boolean on) {
        BuildGrammar g = grammar();
        g.mix(on, on ? player.getRandom().nextLong() : g.seed());
        update(player);
    }

    /** The wheel's style row: the current spell's style (or shape) by its ordinal. */
    static void chooseStyle(LocalPlayer player, int style) {
        grammar().style(style);
        update(player);
    }

    /** The wheel's Solid and Hollow: whether the current shape is hollow. */
    static void chooseHollow(LocalPlayer player, boolean hollow) {
        grammar().hollow(hollow);
        update(player);
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
            paletteStack = ItemStack.EMPTY;
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
            BuildTips.nudge(BuildTips.COPY);
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
        if (changesTheWorld(payload.code())) {
            // The world under the box is not what the plan was counted against: count it and draw its ghost again, and
            // find the selected thing again from where it was clicked (a tree that was vanished is gone).
            epoch++;
            flood = null;
            floodSeeds = List.of();
            grammar().selectedBy(null);
            grammar().maskBounds(null);
            quietFlood = true;
        }
        if (!BuildVerdictPayload.isRefusal(payload.code())) {
            return;
        }
        if (op == SpellRecipe.Op.TAKE || op == SpellRecipe.Op.PICK_UP) {
            grammar().dropGhost();
        }
        int code = payload.code();
        // A refusal of build mode itself carries no token; a cast's DIMENSION (a builder who walked into a denied world while
        // on) keeps the mode the server kept.
        boolean modeRefused = code == BuildVerdictPayload.OFF || code == BuildVerdictPayload.OPERATORS
                || code == BuildVerdictPayload.ADVENTURE || code == BuildVerdictPayload.DIMENSION && payload.token() == 0;
        if (mode && modeRefused) {
            leaveMode();
        }
    }

    /** Whether a verdict says blocks changed: a spell done, undone, redone or stopped part way. */
    private static boolean changesTheWorld(int code) {
        return switch (code) {
            case BuildVerdictPayload.DONE, BuildVerdictPayload.DONE_PARTLY, BuildVerdictPayload.UNDONE, BuildVerdictPayload.UNDONE_PARTLY,
                 BuildVerdictPayload.REDONE, BuildVerdictPayload.STOPPED, BuildVerdictPayload.STOPPED_FIGHT -> true;
            default -> false;
        };
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

    /**
     * How many blocks the current plan changes: the server's preflight once it answered {@code READY} or
     * {@code READY_PARTLY}, else the client's own count; -1 while the count is not done (or nothing is planned).
     */
    public static long changed() {
        if (count == null || !count.done()) {
            return -1;
        }
        BuildVerdictPayload server = preflight();
        boolean ready = server != null && (server.code() == BuildVerdictPayload.READY || server.code() == BuildVerdictPayload.READY_PARTLY);
        return ready ? server.changed() : count.changed();
    }

    /** What the current plan costs Billy: the preflight's cost when it named one, else his price for its blocks. */
    public static long cost(BuildData data) {
        long changed = changed();
        if (changed < 0) {
            return -1;
        }
        BuildVerdictPayload server = preflight();
        return server != null && server.cost() > 0 ? server.cost() : BillyMagic.cost(changed, data.momNear());
    }

    /**
     * Whether the ghost is grey (D-B 6.2, 6.3, 8.4): the server refused the plan as it stands (not for something that
     * passes by itself: a hold to give, a meter refilling, a spell still at work, a full queue, a busy server), or it
     * costs Billy more than his whole meter ("never: make it smaller").
     */
    public static boolean refused(LocalPlayer player) {
        BuildVerdictPayload server = preflight();
        if (server != null && BuildVerdictPayload.isRefusal(server.code())) {
            switch (server.code()) {
                case BuildVerdictPayload.HOLD, BuildVerdictPayload.NO_UNDO_ROOM, BuildVerdictPayload.REFILLING, BuildVerdictPayload.WAIT,
                     BuildVerdictPayload.QUEUE_FULL, BuildVerdictPayload.SERVER_BUSY -> {
                }
                default -> {
                    return true;
                }
            }
        }
        if (limits == null || !limits.limited()) {
            return false;
        }
        BuildData data = player.getExistingData(ModAttachments.BUILD_DATA).orElse(BuildData.EMPTY);
        return cost(data) > BillyMagic.top(data);
    }

    /** The current plan (null when nothing is ready, or a selected thing is still flooding). */
    public static @Nullable SpellPlanner.Plan plan() {
        return plan;
    }

    /** What the ghost shows: the recipe ready to cast, or the rubber band's spell ({@link BuildGrammar#preview}). */
    static @Nullable SpellRecipe ghostRecipe() {
        return preview;
    }

    /** The plan of {@link #ghostRecipe} (null while it is not made, or a selected thing is still flooding). */
    static @Nullable SpellPlanner.Plan ghostPlan() {
        return recipe != null ? plan : previewPlan;
    }

    /** What {@link #ghostPlan} is made of: equal keys, equal plans (in a world that did not change meanwhile). */
    static @Nullable Object ghostKey() {
        return recipe != null ? planKey : previewKey;
    }

    /** What the material is made of (a block, or a mix and its seed), to see when it changes. */
    static @Nullable Object materialKey() {
        return materialKey;
    }

    /** How many of the builder's spells ended: it changes when the world under them did. */
    static int epoch() {
        return epoch;
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
        return usablePocket();
    }

    /**
     * The copy kept, while it is the pocket's copy now ({@link BuildData#pocket}): after a new Copy the new one streams in
     * over some ticks, and until it is whole the old contents are neither shown nor counted for the stamp.
     */
    private static @Nullable ClientPocket usablePocket() {
        LocalPlayer player = Minecraft.getInstance().player;
        return pocket != null && player != null && pocket.id() == pocketId(player) ? pocket : null;
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
        // The marks of a verdict belong to the world it was about.
        lastVerdict = null;
        plan = null;
        count = null;
        planKey = null;
        preview = null;
        previewKey = null;
        previewPlan = null;
        Ghost.clear();
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
        askedAt = Long.MIN_VALUE / 2;
        lastSaid = Long.MIN_VALUE / 2;
        grammar = null;
        limits = null;
        material = null;
        materialKey = null;
        PENDING.clear();
        PREFLIGHTS.clear();
        lastVerdict = null;
        token = 0;
        preflightToken = 0;
        stroke = strokeStart();
        sentView = 0;
        sentA = BlockPos.ZERO;
        sentB = BlockPos.ZERO;
        POCKET_PARTS.clear();
        pocket = null;
        pocketAsked = Long.MIN_VALUE / 2;
        lastPreflight = Long.MIN_VALUE / 2;
        lastView = Long.MIN_VALUE / 2;
        picked = null;
        paletteStack = ItemStack.EMPTY;
        pickSettled = false;
    }

    private static int nextToken() {
        token = token == Integer.MAX_VALUE ? 1 : token + 1;
        return token;
    }

    private static void send(CustomPacketPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            Net.sendToServer(payload);
        }
    }

    private static int config(ModConfigSpec.IntValue value, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    private BuildClient() {
    }
}

package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.ultron.SokoviaRecovery;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The one rule book of Build Magic (D-B 10): used by every spell, every undo and every redo, for both heroes. Each
 * refusal has a verdict code ({@link BuildVerdictPayload}) and one summary line at the end; nothing is said per cell.
 *
 * <ul>
 *     <li><b>Once per cast</b> (D-B 10.2): {@link #player} (C1 to C5: the switches, operators only, build mode, sand
 *     on the hands, Adventure, the denied dimensions), {@link #reach} (C7: the anchor, the seeds and Billy's corners
 *     within reach, Billy's longest side), {@link #material} (C8), {@link #island} (C9: an island the Sokovia recovery
 *     is still clearing). The queue, the ceiling and the long hold (C10 to C12) are {@code BuildMagic}'s, which knows the
 *     spells at work; C13 is decided by the job's count.</li>
 *     <li><b>Per cell</b> (D-B 10.3, {@link Cells}), cheapest first, the first refusal wins: P1 (only chunks loaded at
 *     the cast) and P2 (the build height, the border) and P3 (nothing to change) are the job's; then P4 beyond magic,
 *     P5 the area rule of the mass edits ({@link BlockRights#massEditRefuses}: arenas, the hidden ground under the
 *     island, a stranger's Hex), P6 bodies that may not be lifted (C2: a creature Mjolnir pins is one), P7 containers
 *     that hold things, P8 the vanilla rights, P9 the section probe and P10 the claims, per cell; P11 the items a
 *     spell is paid with ({@link #paysMaterials}, {@link #itemOf}, {@link #itemsIn}).</li>
 *     <li><b>The preflight</b> (D-B 10.1, {@link #preflight}): the rules that ask nobody, on the cells of a plan (or a
 *     sample of a big one), in at most 2 ms, posting no event.</li>
 * </ul>
 */
public final class BuildRules {
    /** No refusal. */
    public static final int OK = SpellPlanner.NONE;
    /** A refusal that is not answered (a stale client: C2). */
    public static final int SILENT = -2;

    /** The cells a preflight checks one by one; a bigger plan is sampled ({@code build_magic.previewChecks} is the cap). */
    public static final int SAMPLED_CELLS = 4096;
    /** The longest a preflight looks (D-B 10.1). */
    public static final long PREFLIGHT_NANOS = 2_000_000L;
    /** How many cells go by between two reads of the clock. */
    public static final int CLOCK_EVERY = 64;

    /** Why a cell was left as it was (D-B 10.3), each counted and said once at the end. */
    public enum Left {
        /** P1: its chunk was not loaded when the spell was cast. */
        UNLOADED(BuildVerdictPayload.UNLOADED, "message.enhanced.build.left.unloaded"),
        /** P2: outside the build height or the world border. */
        OUTSIDE(BuildVerdictPayload.PROTECTED, "message.enhanced.build.left.protected"),
        /** P4: beyond magic (bedrock, a spawner, Mjolnir lying as a block). */
        BEYOND(BuildVerdictPayload.PROTECTED, "message.enhanced.build.left.beyond"),
        /** P5, P8, P9, P10: an arena, spawn protection, the border, a claim, a Hex. */
        PROTECTED(BuildVerdictPayload.PROTECTED, "message.enhanced.build.left.protected"),
        /** P6: someone stands there who may not be lifted. */
        BODIES(BuildVerdictPayload.BODIES, "message.enhanced.build.left.bodies"),
        /** P7: a container with things in it. */
        CONTAINERS(BuildVerdictPayload.KEEPS, "message.enhanced.build.left.containers"),
        /** Changed since (an undo, a Move's source): left as someone made it. */
        CHANGED(0, "message.enhanced.build.kept_changed");

        private final int flag;
        private final String key;

        Left(int flag, String key) {
            this.flag = flag;
            this.key = key;
        }

        /** The verdict's flag for it ({@link BuildVerdictPayload#PROTECTED}...). */
        public int flag() {
            return flag;
        }

        /** The summary line, with how many cells. */
        public Component message(long count) {
            return Component.translatable(key, count);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Once per cast (D-B 10.2)

    /**
     * C1 to C5 for a cast (or an undo, which needs no build mode: {@code needsMode} false, R42): the hero, alive and no
     * Spectator ({@link #SILENT} otherwise); the switches ({@code OFF}), operators only ({@code OPERATORS}); sand on the
     * hands ({@code BOUND}); Adventure ({@code ADVENTURE}); a denied dimension ({@code DIMENSION}); and last build mode,
     * whose absence is a stale client's ({@link #SILENT}). The game's own refusals come before build mode, so a hero
     * whose mode ended because of them (Adventure ends it) still hears why. {@link #OK} when all pass.
     */
    public static int player(ServerPlayer player, @Nullable Limits limits, boolean mode, boolean needsMode) {
        if (limits == null || !player.isAlive() || player.isSpectator()) {
            return SILENT; // C2: not a builder (a stale client)
        }
        int switches = switches(player, limits);
        if (switches != OK) {
            return switches;
        }
        if (player.hasEffect(ModEffects.ENTOMBED)) {
            return BuildVerdictPayload.BOUND;
        }
        if (!player.mayBuild()) {
            return BuildVerdictPayload.ADVENTURE;
        }
        if (denied(player.serverLevel())) {
            return BuildVerdictPayload.DIMENSION;
        }
        return needsMode && !mode ? SILENT : OK;
    }

    /** C1: Build Magic and the hero's own switch are on, and the player may use it (operators only). */
    public static int switches(ServerPlayer player, Limits limits) {
        if (!BuildConfig.ENABLED.get() || !limits.enabled()) {
            return BuildVerdictPayload.OFF;
        }
        if (BuildConfig.OPERATORS_ONLY.get() && !player.hasPermissions(2)) {
            return BuildVerdictPayload.OPERATORS;
        }
        return OK;
    }

    /** C5: the level's dimension is one of {@code build_magic.deniedDimensions}. */
    public static boolean denied(ServerLevel level) {
        String id = level.dimension().location().toString();
        for (String denied : BuildConfig.DENIED_DIMENSIONS.get()) {
            if (denied.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * C7: every place of the recipe the hero points at within his reach (+ {@link Limits#REACH_SLACK}) of his eyes:
     * the anchor of the spells that have one, the seeds of a selection, and for Billy the corners of his box, which
     * must also fit his longest side ({@code TOO_WIDE}). {@link #OK}, {@code TOO_FAR} or {@code TOO_WIDE}.
     */
    public static int reach(ServerPlayer player, Limits limits, SpellRecipe recipe) {
        Vec3 eyes = player.getEyePosition();
        if (anchored(recipe) && !limits.inReach(eyes, recipe.anchor(), true)) {
            return BuildVerdictPayload.TOO_FAR;
        }
        if (recipe.region() == SpellRecipe.MASK) {
            for (BlockPos seed : recipe.seeds()) {
                if (!limits.inReach(eyes, seed, true)) {
                    return BuildVerdictPayload.TOO_FAR;
                }
            }
        }
        if (recipe.region() == SpellRecipe.BOX && limits.limited()) {
            if (!limits.inReach(eyes, recipe.a(), true) || !limits.inReach(eyes, recipe.b(), true)) {
                return BuildVerdictPayload.TOO_FAR;
            }
            if (!limits.fits(SpellMath.box(recipe.a(), recipe.b()))) {
                return BuildVerdictPayload.TOO_WIDE;
            }
        }
        return OK;
    }

    /** Whether the recipe's anchor is a place the hero points at (a brush, a face, a shape, a stamp, This kind). */
    private static boolean anchored(SpellRecipe recipe) {
        return switch (recipe.spell()) {
            case PAINT, PULL, SHAPE, LAND, GROW -> true;
            case COPY -> recipe.op() == SpellRecipe.Op.STAMP;
            case MOVE -> recipe.op() == SpellRecipe.Op.PUT_DOWN;
            case VANISH -> recipe.vanishStyle() == VanishStyle.KIND;
            case CHANGE -> recipe.changeStyle() == ChangeStyle.KIND;
            default -> false;
        };
    }

    /**
     * C8: the material, as the server read it ({@code state}, null for none): {@code NO_BLOCK} when there is none,
     * {@code BAD_BLOCK} for a block magic never makes ({@code enhanced:build_unconjurable}), {@code TWO_BLOCK} for a
     * block of two cells (a door, a bed, a tall flower), {@code VOLATILE} for TNT, lava or fire in Billy's hands.
     */
    public static int material(@Nullable BlockState state, Limits limits) {
        if (state == null || state.isAir()) {
            return BuildVerdictPayload.NO_BLOCK;
        }
        if (state.is(ModTags.BUILD_UNCONJURABLE) || SpellMath.untouchable(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            return BuildVerdictPayload.BAD_BLOCK;
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)) {
            return BuildVerdictPayload.TWO_BLOCK;
        }
        if (!limits.volatileAllowed() && state.is(ModTags.BUILD_VOLATILE)) {
            return BuildVerdictPayload.VOLATILE;
        }
        return OK;
    }

    /** C9: the box meets an island the Sokovia recovery is clearing after a crash ({@code SOKOVIA}). */
    public static boolean island(ServerLevel level, @Nullable BoundingBox box) {
        return box != null && SokoviaRecovery.coversAny(level, box);
    }

    /**
     * What the builder is told of a refusal ({@code extra} is the number it names: a reach, a side, a count, a
     * limit); null for one that is not said.
     */
    public static @Nullable Component message(int code, long extra) {
        String key = switch (code) {
            case BuildVerdictPayload.OFF -> "disabled";
            case BuildVerdictPayload.OPERATORS -> "operators";
            case BuildVerdictPayload.ADVENTURE -> "adventure";
            case BuildVerdictPayload.BOUND -> "bound";
            case BuildVerdictPayload.DIMENSION -> "dimension";
            case BuildVerdictPayload.NO_BLOCK -> "no_block";
            case BuildVerdictPayload.TWO_BLOCK -> "two_block";
            case BuildVerdictPayload.VOLATILE -> "volatile";
            case BuildVerdictPayload.NO_BOX -> "no_box";
            case BuildVerdictPayload.LOOK_INSIDE -> "look_inside";
            case BuildVerdictPayload.WAIT -> "wait";
            case BuildVerdictPayload.SERVER_BUSY -> "server_busy";
            case BuildVerdictPayload.SOKOVIA -> "sokovia";
            case BuildVerdictPayload.NO_UNDO_ROOM -> "no_undo_room";
            case BuildVerdictPayload.POCKET_EMPTY -> "pocket_empty";
            case BuildVerdictPayload.POCKET_TOO_BIG -> "pocket_too_big";
            case BuildVerdictPayload.NOTHING_TO_UNDO -> "nothing_to_undo";
            case BuildVerdictPayload.NOTHING_TO_REDO -> "nothing_to_redo";
            default -> null;
        };
        if (key != null) {
            return Component.translatable("message.enhanced.build." + key);
        }
        return switch (code) {
            case BuildVerdictPayload.TOO_FAR -> Limits.tooFar((int) extra);
            case BuildVerdictPayload.TOO_WIDE -> Limits.tooWide((int) extra);
            case BuildVerdictPayload.QUEUE_FULL -> Component.translatable("message.enhanced.build.queue_full", extra);
            case BuildVerdictPayload.CEILING -> Component.translatable("message.enhanced.build.ceiling", extra);
            case BuildVerdictPayload.HOLD -> Component.translatable("message.enhanced.build.huge", extra);
            case BuildVerdictPayload.TOO_BIG_TO_SELECT -> Component.translatable("message.enhanced.build.too_big_to_select", extra);
            default -> null;
        };
    }

    // ------------------------------------------------------------------------------------------------------------
    // Containers (P7)

    /**
     * Whether the block entity holds things (P7): a {@link Container} that is not empty, a lectern with a book, a
     * campfire with food, or any non-empty NeoForge item handler on the block (modded storage).
     */
    public static boolean holdsItems(ServerLevel level, BlockPos pos, @Nullable BlockEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Container container) {
            return !container.isEmpty();
        }
        if (entity instanceof LecternBlockEntity lectern) {
            return lectern.hasBook();
        }
        if (entity instanceof CampfireBlockEntity campfire) {
            return campfire.getItems().stream().anyMatch(stack -> !stack.isEmpty());
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, entity.getBlockState(), entity, null);
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (!handler.getStackInSlot(slot).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Spills what the block entity holds on the spot, as breaking it would ({@code fullContainers = spill}): a shulker box
     * drops as a filled shulker box item; everything else drops its items. The block is then empty.
     */
    public static void spill(ServerLevel level, BlockPos pos, BlockEntity entity) {
        if (entity instanceof ShulkerBoxBlockEntity shulker) {
            ItemStack box = new ItemStack(ShulkerBoxBlock.getBlockByColor(shulker.getColor()));
            box.applyComponents(shulker.collectComponents());
            Block.popResource(level, pos, box);
            shulker.clearContent();
            return;
        }
        if (entity instanceof Container container) {
            Containers.dropContents(level, pos, container);
            container.clearContent();
            return;
        }
        if (entity instanceof LecternBlockEntity lectern && lectern.hasBook()) {
            Block.popResource(level, pos, lectern.getBook().copy());
            lectern.clearContent();
            return;
        }
        if (entity instanceof CampfireBlockEntity campfire) {
            Containers.dropContents(level, pos, campfire.getItems());
            campfire.clearContent();
            return;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, entity.getBlockState(), entity, null);
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack taken = handler.extractItem(slot, Integer.MAX_VALUE, false);
                if (!taken.isEmpty()) {
                    Block.popResource(level, pos, taken);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Materials (P11, D-B 5.6)

    /**
     * Whether {@code player} pays for the blocks his spells make with items from his inventory
     * ({@code build_magic.requireMaterials}): in Survival, while he is here (null: away); Creative never pays.
     */
    public static boolean paysMaterials(@Nullable ServerPlayer player) {
        return player != null && BuildConfig.REQUIRE_MATERIALS.get() && !player.isCreative() && !player.isSpectator();
    }

    /**
     * The item a block is paid with, and given back as when it vanishes: the block itself as an item ({@code asItem});
     * null for a block with none (air, a fluid, fire), which magic makes and takes away for nothing.
     */
    public static @Nullable Item itemOf(BlockState state) {
        Item item = state.getBlock().asItem();
        return item == Items.AIR ? null : item;
    }

    /**
     * How many of its {@link #itemOf item} a cell holding {@code state} is worth: 1; 2 for a double slab; as many as
     * it holds of candles, sea pickles, turtle eggs, petals or snow layers; 0 for the upper half of a door or a tall
     * plant and the head of a bed (the other half is the item), and for a block with no item.
     */
    public static int itemsIn(BlockState state) {
        if (itemOf(state) == null) {
            return 0;
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? 0 : 1;
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            return state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD ? 0 : 1;
        }
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            return state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
        }
        for (IntegerProperty many : List.of(BlockStateProperties.CANDLES, BlockStateProperties.PICKLES, BlockStateProperties.EGGS,
                BlockStateProperties.FLOWER_AMOUNT, BlockStateProperties.LAYERS)) {
            if (state.hasProperty(many)) {
                return state.getValue(many);
            }
        }
        return 1;
    }

    /**
     * How many plain {@code item}s the player carries in his main inventory (the hotbar and the 27 slots): a stack with
     * data of its own (a filled shulker box, a name, an enchantment) is never used.
     */
    public static int plainCount(Player player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (plain(stack, item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Takes {@code count} plain {@code item}s from his main inventory; false, and nothing taken, when he has fewer. */
    public static boolean takePlain(Player player, Item item, int count) {
        if (count <= 0) {
            return true;
        }
        if (plainCount(player, item) < count) {
            return false;
        }
        int left = count;
        for (ItemStack stack : player.getInventory().items) {
            if (left > 0 && plain(stack, item)) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
        player.getInventory().setChanged();
        return true;
    }

    /** Gives {@code count} {@code item}s to his inventory; what does not fit drops at his feet. */
    public static void give(Player player, Item item, int count) {
        int left = count;
        while (left > 0) {
            int n = Math.min(left, item.getDefaultMaxStackSize());
            player.getInventory().placeItemBackInInventory(new ItemStack(item, n));
            left -= n;
        }
    }

    private static boolean plain(ItemStack stack, Item item) {
        return !stack.isEmpty() && stack.is(item) && stack.isComponentsPatchEmpty();
    }

    /** "You need %1$s more %2$s" ({@code MATERIALS}). */
    public static Component needMore(int missing, Item item) {
        return Component.translatable("message.enhanced.build.materials", missing, item.getDescription());
    }

    /** "No more %1$s: %2$s blocks were not placed" (P11: the items ran out while the spell worked). */
    public static Component outOf(Item item, long notPlaced) {
        return Component.translatable("message.enhanced.build.out_of", item.getDescription(), notPlaced);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Per cell (D-B 10.3)

    /**
     * The per-cell rules of one job (P4 to P10), with what they remember: the verdict of each section's probe (P9),
     * the bodies standing in the spell's way this slice (P6), and how many cells were left for each reason.
     */
    public static final class Cells {
        private static final byte ALLOWED = 1;
        private static final byte REFUSED = 2;

        private final ServerLevel level;
        private ServerPlayer player;
        private final Long2ByteOpenHashMap sections = new Long2ByteOpenHashMap();
        private final List<AABB> bodies = new ArrayList<>();
        private final long[] left = new long[Left.values().length];
        private long spilled;
        private boolean spills;

        /** The rules of a job of {@code player} (the caster, or a fake player of his profile) in {@code level}. */
        public Cells(ServerLevel level, ServerPlayer player) {
            this.level = level;
            this.player = player;
        }

        /** Who the cells are checked for this slice: the caster while he is online and alive, else his fake player. */
        public void actAs(ServerPlayer player) {
            this.player = player;
        }

        public ServerPlayer player() {
            return player;
        }

        /**
         * Finds the bodies in {@code box} that may not be lifted out of new blocks (P6), once a slice: another player
         * the caster may not hurt (PvP off, Creative), a boss or anything of a fight, a creature Mjolnir pins (C2), and
         * with {@code liftCreatures} off every living body. {@code caster} is the caster's own body (null when away).
         */
        public void findBodies(AABB box, @Nullable ServerPlayer caster) {
            bodies.clear();
            boolean lift = BuildConfig.LIFT_CREATURES.get();
            for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (!lift || !liftable(body, caster)) {
                    bodies.add(body.getBoundingBox());
                }
            }
        }

        /** Whether a spell of this caster may lift {@code body} out of new blocks (D-B 11.8). */
        public boolean liftable(LivingEntity body, @Nullable ServerPlayer caster) {
            if (body.isSpectator() || BossFights.isFightEntity(body) || MjolnirPin.isPinned(body)) {
                return false;
            }
            if (body instanceof Player other && other != caster) {
                return AreaTargets.mayHurt(player, other, true);
            }
            return true;
        }

        /**
         * P4 to P8 for writing {@code now} over {@code old} at {@code pos}: null when they pass, else why the cell is
         * left. {@code conjured}: the block comes from magic (not a Move, not an undo), so it must not be
         * {@code build_unconjurable}; {@code carried}: a Move carries the block entity, so P7 lets it pass. After it,
         * {@link #spills} says whether P7 wants the container spilled first.
         */
        public @Nullable Left check(BlockPos pos, BlockState old, BlockState now, boolean conjured, boolean carried) {
            spills = false;
            if (SpellMath.untouchable(old, level, pos) || conjured && now.is(ModTags.BUILD_UNCONJURABLE)) {
                return Left.BEYOND;
            }
            if (BlockRights.massEditRefuses(player, level, pos)) {
                return Left.PROTECTED;
            }
            if (!bodies.isEmpty() && endangers(now, pos)) {
                AABB cell = new AABB(pos);
                for (AABB body : bodies) {
                    if (body.intersects(cell)) {
                        return Left.BODIES;
                    }
                }
            }
            if (!carried && old.hasBlockEntity() && holdsItems(level, pos, level.getBlockEntity(pos))) {
                if (BuildConfig.FULL_CONTAINERS.get() != BuildConfig.FullContainers.SPILL) {
                    return Left.CONTAINERS;
                }
                spills = true;
            }
            if (!BlockRights.vanillaRights(player, level, pos)) {
                return Left.PROTECTED;
            }
            return null;
        }

        /**
         * Whether {@code now} at {@code pos} would harm or bury someone standing there: a block with a collision shape, a
         * fluid, fire, TNT, lava, and the blocks that hold or hurt a body (powder snow, cobweb, berry bushes, campfires,
         * magma).
         */
        private boolean endangers(BlockState now, BlockPos pos) {
            if (now.isAir()) {
                return false;
            }
            return !now.getCollisionShape(level, pos).isEmpty() || !now.getFluidState().isEmpty() || now.is(ModTags.BUILD_VOLATILE)
                    || now.is(Blocks.POWDER_SNOW) || now.is(Blocks.COBWEB) || now.is(Blocks.SWEET_BERRY_BUSH) || now.is(Blocks.MAGMA_BLOCK)
                    || now.is(Blocks.WITHER_ROSE) || now.is(BlockTags.FIRE) || now.is(BlockTags.CAMPFIRES);
        }

        /** Whether the last {@link #check} wants the container spilled before the cell is written. */
        public boolean spills() {
            return spills;
        }

        /** Counts a container spilled. */
        public void spilled() {
            spilled++;
        }

        public long spilledCount() {
            return spilled;
        }

        /**
         * P9 and P10, the claims, right before the write: the first changed cell of each section posts the events of P10
         * (a break event when {@code old} is not air, a place event when {@code now} is not air); a refusal leaves the
         * whole section, and no other event is posted there. In a section that passed, every cell posts its own events
         * while {@code build_magic.postEventsPerBlock} is on. True when the cell may be written.
         */
        public boolean claims(BlockPos pos, BlockState old, BlockState now) {
            long key = SectionPos.asLong(pos);
            byte verdict = sections.get(key);
            if (verdict == REFUSED) {
                return false;
            }
            if (verdict == ALLOWED && !BuildConfig.POST_EVENTS_PER_BLOCK.get()) {
                return true;
            }
            boolean allowed = post(pos, old, now);
            if (verdict == 0) {
                sections.put(key, allowed ? ALLOWED : REFUSED);
            }
            return allowed;
        }

        private boolean post(BlockPos pos, BlockState old, BlockState now) {
            if (!old.isAir() && !BlockRights.postBreak(player, level, pos, old)) {
                return false;
            }
            return now.isAir() || BlockRights.postPlace(player, level, pos);
        }

        /** Counts a cell left for {@code why}. */
        public void left(Left why) {
            left[why.ordinal()]++;
        }

        /** Counts {@code cells} cells left for {@code why}. */
        public void left(Left why, long cells) {
            left[why.ordinal()] += cells;
        }

        /** How many cells were left for {@code why}. */
        public long count(Left why) {
            return left[why.ordinal()];
        }

        /** How many cells were left for any reason. */
        public long leftInAll() {
            long all = 0;
            for (long n : left) {
                all += n;
            }
            return all;
        }

        /** The verdict's flags for what was left. */
        public int flags() {
            int flags = 0;
            for (Left why : Left.values()) {
                if (left[why.ordinal()] > 0) {
                    flags |= why.flag();
                }
            }
            return flags;
        }

        /**
         * The one line that says what was left, the most important first: protected, containers, bodies, beyond
         * magic, not loaded; null when nothing was left.
         */
        public @Nullable Component summary() {
            Left[] order = {Left.PROTECTED, Left.CONTAINERS, Left.BODIES, Left.BEYOND, Left.UNLOADED};
            for (Left why : order) {
                long n = count(why) + (why == Left.PROTECTED ? count(Left.OUTSIDE) : 0);
                if (n > 0) {
                    return why.message(n);
                }
            }
            if (spilled > 0) {
                return Component.translatable("message.enhanced.build.left.spilled", spilled);
            }
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The preflight (D-B 10.1)

    /**
     * What a preflight saw: the cells it checked that change ({@code changed}), those of them that remove or replace an
     * existing block, how many it checked, whether it saw only part ({@code cutOff}: a sample, or the 2 ms ran out), the
     * verdict's flags, and up to {@link BuildVerdictPayload#MAX_MARKS} marked cells with their kinds.
     */
    public record Preview(long changed, long removed, long checked, boolean cutOff, int flags, long[] marks, byte[] kinds) {
        /** Whether some cells it checked would be left. */
        public boolean partly() {
            return (flags & ~BuildVerdictPayload.CUT_OFF) != 0;
        }
    }

    /**
     * The preflight of a plan (D-B 10.1): the per-cell rules that ask nobody (P1 not loaded, P2 outside, P4 beyond
     * magic, P5 arenas, the hidden ground and Hexes, P6 bodies, P7 containers, P8 the vanilla rights: spawn protection,
     * the border, the game mode) on every cell of a plan of at most {@code checks} cells, and on about
     * {@link #SAMPLED_CELLS} cells spread over the box of a bigger one plus its eight corners, for at most
     * {@link #PREFLIGHT_NANOS} ({@code clock} read every {@link #CLOCK_EVERY} cells). It posts no event and writes
     * nothing.
     */
    public static Preview preflight(ServerLevel level, ServerPlayer player, SpellPlanner.Plan plan, int checks, LongSupplier clock) {
        Looker looker = new Looker(level, player, plan);
        BoundingBox bounds = plan.bounds();
        if (bounds == null || !plan.writes()) {
            return looker.preview(false);
        }
        long deadline = clock.getAsLong() + PREFLIGHT_NANOS;
        looker.rules.findBodies(AABB.of(bounds).inflate(1.0), player);
        int n = 0;
        if (plan.size() <= checks) {
            SpellPlanner.Cursor cursor = plan.cursor();
            while (cursor.next()) {
                if (++n % CLOCK_EVERY == 0 && clock.getAsLong() > deadline) {
                    return looker.preview(true);
                }
                looker.look(cursor.pos(), () -> cursor.resolve(level));
            }
            return looker.preview(false);
        }
        int perAxis = (int) Math.ceil(Math.cbrt(SAMPLED_CELLS));
        List<BlockPos> sample = new ArrayList<>();
        for (int corner = 0; corner < 8; corner++) {
            sample.add(new BlockPos((corner & 1) == 0 ? bounds.minX() : bounds.maxX(), (corner & 2) == 0 ? bounds.minY() : bounds.maxY(),
                    (corner & 4) == 0 ? bounds.minZ() : bounds.maxZ()));
        }
        for (int iz = 0; iz < perAxis; iz++) {
            for (int iy = 0; iy < perAxis; iy++) {
                for (int ix = 0; ix < perAxis; ix++) {
                    sample.add(new BlockPos(along(bounds.minX(), bounds.maxX(), ix, perAxis), along(bounds.minY(), bounds.maxY(), iy, perAxis),
                            along(bounds.minZ(), bounds.maxZ(), iz, perAxis)));
                }
            }
        }
        for (BlockPos pos : sample) {
            if (++n % CLOCK_EVERY == 0 && clock.getAsLong() > deadline) {
                break;
            }
            if (plan.covers(pos.getX(), pos.getY(), pos.getZ())) {
                looker.look(pos, () -> plan.resolveAt(level, pos));
            }
        }
        return looker.preview(true);
    }

    /** The {@code i}th of {@code of} places spread evenly from {@code min} to {@code max}. */
    private static int along(int min, int max, int i, int of) {
        return of <= 1 ? min : (int) (min + ((long) max - min) * i / (of - 1));
    }

    /** What a preflight has seen so far. */
    private static final class Looker {
        private final ServerLevel level;
        private final Cells rules;
        private final boolean carried;
        private final SpellMath.@Nullable Volume region;
        private final LongArrayList marks = new LongArrayList();
        private final ByteArrayList kinds = new ByteArrayList();
        private long changed;
        private long removed;
        private long checked;
        private int flags;

        Looker(ServerLevel level, ServerPlayer player, SpellPlanner.Plan plan) {
            this.level = level;
            this.rules = new Cells(level, player);
            this.carried = plan.recipe().spell() == Spell.MOVE;
            this.region = carried ? plan.region() : null;
        }

        /** Checks one cell; {@code now} reads what it gets (only once its chunk is known to be loaded). */
        void look(BlockPos pos, Supplier<BlockState> now) {
            checked++;
            if (!SpellMath.loaded(level, pos)) {
                mark(pos, BuildVerdictPayload.MARK_UNLOADED, BuildVerdictPayload.UNLOADED);
                return;
            }
            if (SpellMath.outside(level, pos)) {
                mark(pos, BuildVerdictPayload.MARK_OUTSIDE, BuildVerdictPayload.PROTECTED);
                return;
            }
            BlockState old = level.getBlockState(pos);
            BlockState state = now.get();
            if (state == null || state.equals(old)) {
                return;
            }
            // (a Move carries the contents of its source cells; a chest it lands on from outside is as safe from it as from a Fill)
            boolean carries = region != null && region.contains(pos.getX(), pos.getY(), pos.getZ());
            Left why = rules.check(pos, old, state, !carried, carries);
            if (why != null) {
                mark(pos, switch (why) {
                    case BEYOND -> BuildVerdictPayload.MARK_BEYOND;
                    case BODIES -> BuildVerdictPayload.MARK_BODY;
                    case CONTAINERS -> BuildVerdictPayload.MARK_KEEPS;
                    default -> BuildVerdictPayload.MARK_PROTECTED;
                }, why.flag());
                return;
            }
            changed++;
            if (!old.isAir() && !old.canBeReplaced() && old.getBlock() != state.getBlock()) {
                removed++;
            }
        }

        private void mark(BlockPos pos, byte kind, int flag) {
            flags |= flag;
            if (marks.size() < BuildVerdictPayload.MAX_MARKS) {
                marks.add(pos.asLong());
                kinds.add(kind);
            }
        }

        Preview preview(boolean cutOff) {
            return new Preview(changed, removed, checked, cutOff, flags | (cutOff ? BuildVerdictPayload.CUT_OFF : 0), marks.toLongArray(),
                    kinds.toByteArray());
        }
    }

    private BuildRules() {
    }
}

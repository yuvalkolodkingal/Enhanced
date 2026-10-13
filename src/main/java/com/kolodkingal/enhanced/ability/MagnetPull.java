package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.network.MagnetPullPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Magneto's Magnetic Pull: Sneak + R with nothing held draws the metal all round him into his inventory (D-M 4). Ores give
 * their metal through rock and turn into their ground, raw-metal blocks come whole through rock, loose metal and empty
 * carts come from all round him, and built metal comes whole when he can see it in the cone he faces (never a block or
 * cart that holds things, never a working piston, never a villager's work block: R36). With {@code
 * magneto.pullFromStorage} the metal stacks of the containers, item frames and armour stands he can see come too (R38).
 *
 * <p>One job per Magneto, kept where he pressed and sliced over ticks:
 * <ol>
 *     <li><b>The search</b> walks the chunk sections that meet the sphere, at most {@code magneto.pullScanPerTick} cells a
 *     tick. A section whose chunk is not loaded, that is all air or whose palette holds nothing the pull could take costs
 *     one read. In a section that has some, the first cell that passes the cheap checks probes the claims with one break
 *     event; a refused section is skipped whole, its entities too. Every cell kept goes into a bounded heap of the {@code
 *     magneto.pullMaxTargets} nearest; a built cell that would enter it is first ray-checked for sight, and every cell the
 *     ray reads is charged to the tick's budget. When the blocks are done, one entity query adds the entities.</li>
 *     <li><b>The takes</b>, nearest first, {@code magneto.pullPerTick} a tick. Each is checked again when its turn comes
 *     (a block with the full break check); the first that affects anything starts the cooldown and costs the hunger. What
 *     is taken goes straight into his inventory and what does not fit drops at his feet; each tick's takes go out in one
 *     {@link MagnetPullPayload} to him and everyone who sees him.</li>
 *     <li><b>The end</b>: one action-bar line and the clink of the metal arriving.</li>
 * </ol>
 * A pull ends at once (what it took stays his) when he dies, logs out, changes dimension, loses his power, becomes a
 * Spectator, or is more than the pull's radius from where he pressed.
 */
public final class MagnetPull {

    /** Magneto's UUID → his pull while it runs. */
    private static final Map<UUID, Job> JOBS = new HashMap<>();
    /** Magneto's UUID → the game time his last pull ended without affecting anything (the retry wait, D-M 4.5 step 2). */
    private static final Map<UUID, Long> FOUND_NOTHING = new HashMap<>();
    /** Magneto's UUID → the game time his last search ended. */
    private static final Map<UUID, Long> SEARCH_ENDS = new HashMap<>();

    /** What {@link #searchEndedAt} answers for a Magneto who never searched: long enough ago for any wait. */
    public static final long NEVER = Long.MIN_VALUE / 2;

    // ------------------------------------------------------------------------------------------------------------
    // The press (D-M 4.1)

    /**
     * Sneak + R with nothing held (called by {@code Telekinesis.press}). Nothing at all happens (no message, no search, no
     * cost) for a Spectator, while a pull of his runs, while he rides (Sneak dismounts), for {@code
     * magneto.telekinesisSneakGraceTicks} after a grip of his ended (a late "put it down" press, R41), or for {@code
     * magneto.pullRetryTicks} after a pull that found nothing (a key held down). Then the cooldown and an empty hunger bar
     * refuse with their line. Otherwise the pull is heard and its search begins at once.
     *
     * @return true: the pull always takes the press (Sneak + R never grabs)
     */
    public static boolean press(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        UUID id = player.getUUID();
        if (player.isSpectator() || JOBS.containsKey(id) || player.isPassenger()
                || now - Telekinesis.lastGripEnd(player) < EnhancedConfig.TELEKINESIS_SNEAK_GRACE_TICKS.get()
                || now - FOUND_NOTHING.getOrDefault(id, NEVER) < EnhancedConfig.PULL_RETRY_TICKS.get()) {
            return true;
        }
        PowerData data = Powers.get(player);
        if (!data.isReady(1, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.pull.recharging"), true);
            return true;
        }
        if (!player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.pull.too_hungry"), true);
            return true;
        }
        Job job = new Job(player, level);
        JOBS.put(id, job);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MAGNET_PULL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        job.lastWork = now;
        search(job, player);
        if (!job.searching && job.order.isEmpty()) {
            finish(job, player);
            return true;
        }
        // The field reaches out at once: the clients play its pulse before anything has come.
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new MagnetPullPayload(player.getId(), true, (float) job.radius, List.of()));
        job.sentFirst = true;
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Every tick

    /**
     * One tick of this Magneto's pull, if one runs (from {@code MagnetoAbilities.tick}): the next slice of the search, or
     * the next takes. A tick in which the press already searched does nothing more.
     */
    public static void tick(ServerPlayer player) {
        Job job = JOBS.get(player.getUUID());
        if (job == null) {
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.serverLevel() != job.level) {
            JOBS.remove(player.getUUID()); // what it took is his; nothing more comes
            return;
        }
        long now = job.level.getGameTime();
        if (job.lastWork == now) {
            return;
        }
        job.lastWork = now;
        if (bodyCentre(player).distanceToSqr(job.centre) > job.radius * job.radius) {
            // A pearl, the Bifrost, the Witches' Road, a knockback: the pull stays where he pressed, and ends.
            JOBS.remove(player.getUUID());
            if (job.affected > 0) {
                tell(job, player);
            }
            return;
        }
        if (job.searching) {
            search(job, player);
            if (!job.searching && job.order.isEmpty()) {
                finish(job, player);
            }
            return;
        }
        takeSome(job, player);
        if (job.next >= job.order.size()) {
            finish(job, player);
        }
    }

    /**
     * Ends this Magneto's pull at once (he died, logged out, changed dimension, lost his power); what was taken stays his,
     * and a cooldown already started stays.
     */
    public static void cancel(ServerPlayer player) {
        JOBS.remove(player.getUUID());
    }

    /** Forgets every pull and every wait: for the server's stop and for tests. */
    public static void clear() {
        JOBS.clear();
        FOUND_NOTHING.clear();
        SEARCH_ENDS.clear();
    }

    /** Whether this Magneto's pull runs. */
    @VisibleForTesting
    public static boolean isPulling(ServerPlayer player) {
        return JOBS.containsKey(player.getUUID());
    }

    /** The game time this Magneto's last search ended, or {@link #NEVER}. */
    @VisibleForTesting
    public static long searchEndedAt(ServerPlayer player) {
        return SEARCH_ENDS.getOrDefault(player.getUUID(), NEVER);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The job

    /** What one target is, and so how it is checked again and taken. */
    private enum Kind {
        /** An ore: it turns into its ground and gives its metal, through rock. */
        ORE,
        /** A raw-metal block: it comes whole, through rock. */
        NATURAL,
        /** Built metal: it comes whole, in sight and in the cone he faced. */
        BUILT,
        /** A container in sight: its metal stacks come, it stays ({@code magneto.pullFromStorage}). */
        STORAGE,
        /** An entity: what it gives is its own rule (a dropped item, a cart, a frame's or a stand's metal). */
        ENTITY
    }

    /** One thing the search found: a cell (with the state it had) or an entity, and its distance from the press. */
    private record Target(Kind kind, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Entity entity, double distSq) {
        /** Where its stream starts: the middle of the cell or of the entity. */
        Vec3 middle() {
            return entity != null ? entity.position().add(0, entity.getBbHeight() * 0.5, 0) : Vec3.atCenterOf(pos);
        }
    }

    /** One pull of one Magneto, from the press to the end line. */
    private static final class Job {
        final ServerLevel level;
        /** The middle of his body where he pressed: the sphere's centre. */
        final Vec3 centre;
        final double radius;
        final int maxTargets;
        /** His eyes and where he looked when he pressed: built metal comes only from that cone. */
        final Vec3 eye;
        final Vec3 look;
        final double coneCos;
        /** The blocks the search's palette test lets through (with storage on, anything with a block entity too). */
        final Predicate<BlockState> palette;

        // The search: the sections of the sphere's box, one after another, and the cell to go on from.
        final int minSX;
        final int minSY;
        final int minSZ;
        final int countX;
        final int countY;
        final int countZ;
        int section;
        int column;
        int nextY = Integer.MIN_VALUE;
        boolean searching = true;
        /** Section (as a long) → whether its claim probe let him: a refused section gives nothing, blocks or entities. */
        final Map<Long, Boolean> claims = new HashMap<>();
        /** Ore block → whether its metal comes out of it at all (a mod's ore whose only loot is itself does not). */
        final Map<Block, Boolean> ores = new HashMap<>();
        /** The nearest targets so far, the farthest on top. */
        final PriorityQueue<Target> heap = new PriorityQueue<>(Comparator.comparingDouble(Target::distSq).reversed());

        // The takes.
        List<Target> order = List.of();
        int next;
        /** Things affected (D-M 4.3, "what counts as taken"). */
        int affected;
        /** Whether some metal was refused for protection (a claim, an arena, Adventure, a villager's work). */
        boolean refused;
        boolean paid;
        boolean toldFull;
        boolean sentFirst;
        long lastWork;

        Job(ServerPlayer player, ServerLevel level) {
            this.level = level;
            this.centre = bodyCentre(player);
            this.radius = EnhancedConfig.PULL_RADIUS.get();
            this.maxTargets = EnhancedConfig.PULL_MAX_TARGETS.get();
            this.eye = player.getEyePosition();
            this.look = player.getLookAngle();
            this.coneCos = Math.cos(Math.toRadians(EnhancedConfig.PULL_BUILT_CONE.get() / 2.0));
            this.palette = EnhancedConfig.PULL_FROM_STORAGE.get()
                    ? state -> Metal.isMetalBlock(state) || state.hasBlockEntity() : Metal::isMetalBlock;
            this.minSX = SectionPos.blockToSectionCoord(Mth.floor(centre.x - radius));
            this.minSZ = SectionPos.blockToSectionCoord(Mth.floor(centre.z - radius));
            this.minSY = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(Mth.floor(centre.y - radius)));
            int maxSY = Math.min(level.getMaxSection() - 1, SectionPos.blockToSectionCoord(Mth.floor(centre.y + radius)));
            this.countX = SectionPos.blockToSectionCoord(Mth.floor(centre.x + radius)) - minSX + 1;
            this.countZ = SectionPos.blockToSectionCoord(Mth.floor(centre.z + radius)) - minSZ + 1;
            this.countY = Math.max(0, maxSY - minSY + 1);
        }

        /** Whether a target this far away would enter the heap now (it is not full, or it is nearer than the farthest). */
        boolean admits(double distSq) {
            return heap.size() < maxTargets || distSq < heap.peek().distSq();
        }

        void offer(Target target) {
            heap.add(target);
            if (heap.size() > maxTargets) {
                heap.poll();
            }
        }

        /** Whether the middle of the cell lies in the cone he faced when he pressed ({@code magneto.pullBuiltCone}). */
        boolean inCone(BlockPos pos) {
            Vec3 to = Vec3.atCenterOf(pos).subtract(eye);
            double length = to.length();
            return length < 1.0E-6 || look.dot(to) >= coneCos * length;
        }
    }

    /** The middle of a player's body: the pull's centre, and where "near" is measured from. */
    private static Vec3 bodyCentre(ServerPlayer player) {
        return player.position().add(0, 1.0, 0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The search (D-M 4.2, 4.5 step 1)

    /** One tick's slice of the search; at its end the entities are added and the heap becomes the order of the takes. */
    private static void search(Job job, ServerPlayer player) {
        int budget = EnhancedConfig.PULL_SCAN_PER_TICK.get();
        int sections = job.countX * job.countY * job.countZ;
        while (job.section < sections) {
            if (budget <= 0) {
                return;
            }
            budget = searchSection(job, player, budget);
            if (job.column >= 256) {
                job.section++;
                job.column = 0;
                job.nextY = Integer.MIN_VALUE;
            }
        }
        addEntities(job, player);
        List<Target> order = new ArrayList<>(job.heap);
        order.sort(Comparator.comparingDouble(Target::distSq));
        job.order = order;
        job.heap.clear();
        job.searching = false;
        SEARCH_ENDS.put(player.getUUID(), job.level.getGameTime());
    }

    /**
     * Reads the current section from where the search stopped, as far as the budget goes: the cells of each column that
     * lie in the sphere, straight from the section. Sets {@code job.column} to 256 when the section is done or skipped.
     *
     * @return the budget left
     */
    private static int searchSection(Job job, ServerPlayer player, int budget) {
        int sx = job.minSX + job.section % job.countX;
        int sz = job.minSZ + (job.section / job.countX) % job.countZ;
        int sy = job.minSY + job.section / (job.countX * job.countZ);
        boolean fresh = job.column == 0 && job.nextY == Integer.MIN_VALUE;
        if (fresh && !meetsSphere(job, sx, sy, sz)) {
            job.column = 256;
            return budget - 1;
        }
        LevelChunk chunk = job.level.getChunkSource().getChunkNow(sx, sz);
        if (chunk == null) {
            job.column = 256; // a chunk is never loaded for the pull
            return budget - 1;
        }
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
        if (section.hasOnlyAir() || !section.maybeHas(job.palette)) {
            job.column = 256; // nothing here the pull could take: one look at the palette
            return budget - 1;
        }
        long key = SectionPos.asLong(sx, sy, sz);
        double r2 = job.radius * job.radius;
        int baseX = SectionPos.sectionToBlockCoord(sx);
        int baseY = SectionPos.sectionToBlockCoord(sy);
        int baseZ = SectionPos.sectionToBlockCoord(sz);
        for (; job.column < 256; job.column++, job.nextY = Integer.MIN_VALUE) {
            int lx = job.column & 15;
            int lz = job.column >> 4;
            double dx = baseX + lx + 0.5 - job.centre.x;
            double dz = baseZ + lz + 0.5 - job.centre.z;
            double rest = r2 - dx * dx - dz * dz;
            if (rest < 0) {
                continue;
            }
            double h = Math.sqrt(rest);
            int low = Math.max(baseY, Mth.ceil(job.centre.y - h - 0.5));
            int high = Math.min(baseY + 15, Mth.floor(job.centre.y + h - 0.5));
            for (int y = Math.max(low, job.nextY); y <= high; y++) {
                if (budget <= 0) {
                    job.nextY = y;
                    return budget;
                }
                budget--;
                BlockState state = section.getBlockState(lx, y - baseY, lz);
                if (!job.palette.test(state)) {
                    continue;
                }
                budget = consider(job, player, key, new BlockPos(baseX + lx, y, baseZ + lz), state, budget);
                if (Boolean.FALSE.equals(job.claims.get(key))) {
                    job.column = 256; // a claim refused the section: skip it whole
                    return budget;
                }
            }
        }
        return budget;
    }

    /** Whether some cell of the section could lie in the sphere (its box comes within the radius of the centre). */
    private static boolean meetsSphere(Job job, int sx, int sy, int sz) {
        double x = Mth.clamp(job.centre.x, SectionPos.sectionToBlockCoord(sx), SectionPos.sectionToBlockCoord(sx) + 16);
        double y = Mth.clamp(job.centre.y, SectionPos.sectionToBlockCoord(sy), SectionPos.sectionToBlockCoord(sy) + 16);
        double z = Mth.clamp(job.centre.z, SectionPos.sectionToBlockCoord(sz), SectionPos.sectionToBlockCoord(sz) + 16);
        return job.centre.distanceToSqr(x, y, z) <= job.radius * job.radius;
    }

    /**
     * One cell the palette let through: kept as a target if the pull would take it, no protection refuses it, its
     * section's claim probe let him, it would enter the heap, and (built metal, storage) he can see it.
     *
     * @return the budget left (a sight ray costs a read per cell it may cross)
     */
    private static int consider(Job job, ServerPlayer player, long section, BlockPos pos, BlockState state, int budget) {
        ServerLevel level = job.level;
        Kind kind = blockKind(job, player, level, pos, state);
        if (kind == null) {
            return budget;
        }
        if (BlockRights.massEditRefuses(player, level, pos) || !BlockRights.vanillaRights(player, level, pos)
                || claimedByVillager(level, pos, state)) {
            job.refused = true;
            return budget;
        }
        Boolean allowed = job.claims.get(section);
        if (allowed == null) {
            allowed = BlockRights.postBreak(player, level, pos, state); // one probe of the claims for the whole section
            job.claims.put(section, allowed);
        }
        if (!allowed) {
            job.refused = true;
            return budget;
        }
        double distSq = job.centre.distanceToSqr(Vec3.atCenterOf(pos));
        if (!job.admits(distSq)) {
            return budget;
        }
        if (needsSight(kind)) {
            budget -= rayCost(player, pos);
            if (!Raycasts.inSight(player, pos)) {
                return budget;
            }
        }
        job.offer(new Target(kind, pos, state, null, distSq));
        return budget;
    }

    /**
     * What the pull would do with this cell, from the block alone (and his feet, and the cone he faced), or null: an ore
     * whose metal comes out; a raw-metal block; built metal (with {@code magneto.pullTakesBuiltMetal}, in the cone,
     * holding nothing, no working piston); or, with {@code magneto.pullFromStorage}, a container with metal in it. Never
     * the cell under his feet, an unbreakable or immovable block, or the upper half of a door (its lower half carries it).
     */
    @Nullable
    private static Kind blockKind(Job job, ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (Metal.isMetalBlock(state) && !underFeet(player, pos) && !state.is(ModTags.TELEKINESIS_IMMOVABLE)
                && state.getDestroySpeed(level, pos) >= 0.0F && !isUpperDoorHalf(state)) {
            if (state.is(ModTags.MAGNETIC_ORES)) {
                return job.ores.computeIfAbsent(state.getBlock(), block -> wouldTakeOre(level, pos, state)) ? Kind.ORE : null;
            }
            if (Metal.isNaturalMetal(state)) {
                return Kind.NATURAL;
            }
            if (EnhancedConfig.PULL_TAKES_BUILT_METAL.get() && job.inCone(pos) && !holdsItems(level, pos)
                    && !isWorkingPiston(level, pos, state)) {
                return Kind.BUILT;
            }
        }
        if (EnhancedConfig.PULL_FROM_STORAGE.get() && state.hasBlockEntity() && hasMetalStacks(level, pos)) {
            return Kind.STORAGE;
        }
        return null;
    }

    /** Built metal with {@code magneto.pullBuiltNeedsSight}, and storage always, come only if he can see them. */
    private static boolean needsSight(Kind kind) {
        return kind == Kind.STORAGE || kind == Kind.BUILT && EnhancedConfig.PULL_BUILT_NEEDS_SIGHT.get();
    }

    /**
     * The entities of the sphere, once the blocks are done (one query): each the pull would act on, standing in a section
     * whose claim probe lets him (a section the block search did not probe is probed now, at the cell the entity stands
     * on), and, for a frame or a stand, in sight. Adventure and spawn protection guard blocks, not these.
     */
    private static void addEntities(Job job, ServerPlayer player) {
        double r2 = job.radius * job.radius;
        AABB box = new AABB(job.centre, job.centre).inflate(job.radius);
        for (Entity e : job.level.getEntities((Entity) null, box, e -> e.position().distanceToSqr(job.centre) <= r2)) {
            if (!wantsEntity(player, e)) {
                continue;
            }
            if (!claimAllows(job, player, e)) {
                job.refused = true;
                continue;
            }
            double distSq = e.position().distanceToSqr(job.centre);
            if (!job.admits(distSq)) {
                continue;
            }
            if (needsSight(e) && !Raycasts.inSight(player, e.position().add(0, e.getBbHeight() * 0.5, 0))) {
                continue;
            }
            job.offer(new Target(Kind.ENTITY, null, null, e, distSq));
        }
    }

    /** Whether the claims let him act on what stands in this entity's section (probed once a section). */
    private static boolean claimAllows(Job job, ServerPlayer player, Entity e) {
        long section = SectionPos.asLong(e.blockPosition());
        Boolean allowed = job.claims.get(section);
        if (allowed == null) {
            BlockPos on = e.getOnPos();
            allowed = BlockRights.postBreak(player, job.level, on, job.level.getBlockState(on));
            job.claims.put(section, allowed);
        }
        return allowed;
    }

    /** The cells a sight ray from his eyes to the cell reads at most (charged to the search's budget). */
    private static int rayCost(ServerPlayer player, BlockPos pos) {
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        return 1 + Math.abs(pos.getX() - eye.getX()) + Math.abs(pos.getY() - eye.getY()) + Math.abs(pos.getZ() - eye.getZ());
    }

    /** Whether the cell lies under his feet (his box moved half a block down): he never pulls his own floor away. */
    private static boolean underFeet(ServerPlayer player, BlockPos pos) {
        return player.getBoundingBox().move(0, -0.5, 0).intersects(new AABB(pos));
    }

    private static boolean isUpperDoorHalf(BlockState state) {
        return state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    /** A piston that is extended or powered: part of a working machine, never taken (R36). */
    private static boolean isWorkingPiston(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof PistonBaseBlock && (state.getValue(PistonBaseBlock.EXTENDED) || level.hasNeighborSignal(pos));
    }

    /**
     * Whether a villager has claimed this block (D-M 4.2): the cell is a point of interest whose tickets are not all free
     * (an armorer's blast furnace, a toolsmith's smithing table, a bell that is a meeting point). Only the cell's own POI
     * section is read, which is loaded with its chunk; a block that is no point of interest costs one lookup.
     */
    @SuppressWarnings("deprecation") // getFreeTickets reads the cell's one record; the queries round a cell read nine columns
    public static boolean claimedByVillager(ServerLevel level, BlockPos pos, BlockState state) {
        if (PoiTypes.forState(state).isEmpty()) {
            return false;
        }
        PoiManager pois = level.getPoiManager();
        Optional<Holder<PoiType>> type = pois.getType(pos);
        return type.isPresent() && pois.getFreeTickets(pos) < type.get().value().maxTickets();
    }

    // ------------------------------------------------------------------------------------------------------------
    // What R asks about built metal (R34, R36)

    /**
     * Whether the pull would take this built metal block whole, which R's message at a block it cannot lift names
     * ({@code telekinesis.fixed_metal} or {@code fixed_metal_off}, R34): {@code magneto.pullTakesBuiltMetal} is on, the
     * block holds no items ({@link #holdsItems}), it is no piston that is extended or powered (R36), and no villager has
     * claimed it (D-M 4.2).
     */
    public static boolean wouldTakeBuilt(ServerLevel level, BlockPos pos, BlockState state) {
        if (!EnhancedConfig.PULL_TAKES_BUILT_METAL.get() || holdsItems(level, pos)) {
            return false;
        }
        return !isWorkingPiston(level, pos, state) && !claimedByVillager(level, pos, state);
    }

    /**
     * Whether the block at {@code pos} holds items (a hopper, a blast furnace or a crafter with anything inside, a container
     * whose loot is still to be rolled, or another mod's storage): such a block never comes whole, and R says {@code
     * telekinesis.fixed_metal_full} at it (R36). A pure query: it rolls no loot.
     */
    public static boolean holdsItems(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return false;
        }
        if (entity instanceof RandomizableContainer loot && loot.getLootTable() != null) {
            return true; // asking it whether it is empty would roll its loot
        }
        if (entity instanceof Container container) {
            return !container.isEmpty();
        }
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (items != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                if (!items.getStackInSlot(slot).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What a block gives (D-M 4.3)

    /**
     * Whether the pull takes the metal out of this ore: ancient debris only with {@code magneto.pullAncientDebris}; the
     * game's own metal ores always; any other block (a mod's ore) only if its metal, as {@link #oreYield} rolls it, is not
     * empty, so an ore whose only loot is itself stays as it is rather than turning to stone for nothing.
     */
    public static boolean wouldTakeOre(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.ANCIENT_DEBRIS)) {
            return EnhancedConfig.PULL_ANCIENT_DEBRIS.get();
        }
        return isGameOre(state) || !oreYield(level, pos, state).isEmpty();
    }

    /** The metal ores of the game, whose metal always comes out (D-M 4.3's table). */
    private static boolean isGameOre(BlockState state) {
        return state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE) || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.NETHER_GOLD_ORE) || state.is(Blocks.GILDED_BLACKSTONE);
    }

    /**
     * The metal an ore gives (D-M 4.3): what an unenchanted iron pickaxe mines out of it (no Fortune, no Silk Touch: one
     * raw iron or raw gold, 2 to 5 raw copper, 2 to 6 gold nuggets from nether gold ore), without any drop that is the ore
     * itself; gilded blackstone always gives 2 to 5 gold nuggets and ancient debris one netherite scrap (the pull takes
     * the metal out, where mining would give the block). Rolled anew at each call.
     */
    public static List<ItemStack> oreYield(ServerLevel level, BlockPos pos, BlockState ore) {
        if (ore.is(Blocks.GILDED_BLACKSTONE)) {
            return List.of(new ItemStack(Items.GOLD_NUGGET, 2 + level.getRandom().nextInt(4)));
        }
        if (ore.is(Blocks.ANCIENT_DEBRIS)) {
            return List.of(new ItemStack(Items.NETHERITE_SCRAP));
        }
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(ore, level, pos, null, null, new ItemStack(Items.IRON_PICKAXE)));
        drops.removeIf(stack -> stack.isEmpty() || stack.is(ore.getBlock().asItem()));
        return drops;
    }

    /**
     * The ground a pulled ore becomes (D-M 4.3): the block whose {@code c:ores_in_ground} tag holds it (stone, deepslate,
     * netherrack); gilded blackstone becomes blackstone, ancient debris netherrack; any other by its dimension: the Nether
     * netherrack, the End end stone, anywhere else deepslate below y 0 and stone above.
     */
    public static BlockState groundFor(ServerLevel level, BlockPos pos, BlockState ore) {
        if (ore.is(Blocks.GILDED_BLACKSTONE)) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (ore.is(Blocks.ANCIENT_DEBRIS) || ore.is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK)) {
            return Blocks.NETHERRACK.defaultBlockState();
        }
        if (ore.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)) {
            return Blocks.DEEPSLATE.defaultBlockState();
        }
        if (ore.is(Tags.Blocks.ORES_IN_GROUND_STONE)) {
            return Blocks.STONE.defaultBlockState();
        }
        if (level.dimension() == Level.NETHER) {
            return Blocks.NETHERRACK.defaultBlockState();
        }
        if (level.dimension() == Level.END) {
            return Blocks.END_STONE.defaultBlockState();
        }
        return pos.getY() < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    /**
     * A metal block taken whole, as Silk Touch mining gives it: the block's own loot with a netherite pickaxe enchanted with
     * Silk Touch (an anvil keeps its damage, a double slab gives two slabs, a door one door, a water cauldron a cauldron,
     * every copper block its oxidation and wax); its own item if that loot is empty. Read before the block goes.
     */
    private static List<ItemStack> wholeDrops(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool));
        drops.removeIf(ItemStack::isEmpty);
        if (drops.isEmpty() && state.getBlock().asItem() != Items.AIR) {
            drops.add(new ItemStack(state.getBlock().asItem()));
        }
        return drops;
    }

    /** Whether the container at {@code pos} holds a metal stack the pull could take (a loot still unrolled is not asked). */
    private static boolean hasMetalStacks(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null || entity instanceof RandomizableContainer loot && loot.getLootTable() != null) {
            return false;
        }
        if (entity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (Metal.isMetalItem(container.getItem(slot))) {
                    return true;
                }
            }
            return false;
        }
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (items != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                if (Metal.isMetalItem(items.getStackInSlot(slot))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What an entity gives (D-M 4.2, 4.3)

    /**
     * Whether the pull would act on this entity: never one any telekinesis holds, the Sokovia lift carries, that rides or
     * is ridden; a dropped metal item he could pick up by hand and has room for (never a drop a fight keeps for a dead
     * fighter); an empty minecart (never the command block one); and, with {@code magneto.pullFromStorage}, an item frame
     * or an armour stand with metal on it.
     */
    private static boolean wantsEntity(ServerPlayer player, Entity e) {
        if (e == player || !e.isAlive() || e.isPassenger() || e.isVehicle() || !givesSomething(player, e)) {
            return false;
        }
        return !Telekinesis.isHeld(e) && !BossFights.isLocked(e);
    }

    /** What the entity itself would give, before who holds or carries it is asked. */
    private static boolean givesSomething(ServerPlayer player, Entity e) {
        if (e instanceof ItemEntity item) {
            return wantsItem(player, item);
        }
        if (e instanceof AbstractMinecart cart) {
            return isEmptyCart(cart);
        }
        if (EnhancedConfig.PULL_FROM_STORAGE.get()) {
            if (e instanceof ItemFrame frame) {
                return Metal.isMetalItem(frame.getItem());
            }
            if (e instanceof ArmorStand stand) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (Metal.isMetalItem(stand.getItemBySlot(slot))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** What is someone's build hangs on a wall or stands on a stand: it gives its metal only if he can see it. */
    private static boolean needsSight(Entity e) {
        return e instanceof ItemFrame || e instanceof ArmorStand;
    }

    /**
     * A dropped item comes if it is metal, he could pick it up by hand (no pickup delay, no other owner; what his own strip
     * tore off a creature is marked for him and comes at once), it is not a dead fighter's drop a fight keeps, and some of
     * it fits in his inventory.
     */
    private static boolean wantsItem(ServerPlayer player, ItemEntity item) {
        ItemStack stack = item.getItem();
        if (!Metal.isMetalItem(stack) || BossFights.isKeptDrop(item)) {
            return false;
        }
        if (item.getTarget() != null && !item.getTarget().equals(player.getUUID())) {
            return false;
        }
        boolean marked = item.getPersistentData().hasUUID(MagnetoAbilities.STRIPPED_BY_TAG)
                && player.getUUID().equals(item.getPersistentData().getUUID(MagnetoAbilities.STRIPPED_BY_TAG));
        if (item.hasPickUpDelay() && !marked) {
            return false;
        }
        Inventory inventory = player.getInventory();
        return inventory.getSlotWithRemainingSpace(stack) != -1 || inventory.getFreeSlot() != -1;
    }

    /** A minecart that comes as its item: not the command block one (an operator's), and holding nothing (R36). */
    private static boolean isEmptyCart(AbstractMinecart cart) {
        if (cart.getMinecartType() == AbstractMinecart.Type.COMMAND_BLOCK) {
            return false;
        }
        return !(cart instanceof ContainerEntity container) || container.getLootTable() == null && container.isChestVehicleEmpty();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The takes (D-M 4.3 to 4.5)

    /**
     * What one turn did. {@code turn}: whether it used one of the tick's turns (something still there was taken or
     * refused; a target gone or changed since the search costs none). {@code affected}: whether it counts as taken (D-M
     * 4.3). {@code toGive}: what goes into his inventory now (what does not fit drops at his feet). {@code shown}: what
     * flies to him in the stream, if anything does.
     */
    private record Taken(boolean turn, boolean affected, List<ItemStack> toGive, @Nullable ItemStack shown) {
        static final Taken GONE = new Taken(false, false, List.of(), null);
        static final Taken REFUSED = new Taken(true, false, List.of(), null);

        /** It gives these, into his inventory or at his feet. */
        static Taken giving(List<ItemStack> stacks) {
            return new Taken(true, true, stacks, stacks.isEmpty() ? null : stacks.get(0));
        }

        /** These came straight into his inventory already (only what fits comes out of where it was). */
        static Taken moved(List<ItemStack> came) {
            return came.isEmpty() ? REFUSED : new Taken(true, true, List.of(), came.get(0));
        }
    }

    /** The next targets, until {@code magneto.pullPerTick} turns are used; then this tick's payload. */
    private static void takeSome(Job job, ServerPlayer player) {
        int perTick = EnhancedConfig.PULL_PER_TICK.get();
        List<MagnetPullPayload.Stream> streams = new ArrayList<>();
        boolean piglins = false;
        int turns = 0;
        while (turns < perTick && job.next < job.order.size()) {
            Target target = job.order.get(job.next++);
            Taken taken = target.kind == Kind.ENTITY ? takeEntity(job, player, target) : takeBlock(job, player, target);
            if (taken.turn) {
                turns++;
            }
            if (!taken.affected) {
                continue;
            }
            job.affected++;
            if (!job.paid) {
                // The first thing the pull affects starts its recharge and costs its hunger (an invulnerable player none).
                job.paid = true;
                Powers.get(player).startCooldown(1, job.level.getGameTime(), EnhancedConfig.PULL_COOLDOWN.get());
                player.causeFoodExhaustion(EnhancedConfig.PULL_EXHAUSTION.get().floatValue());
            }
            give(job, player, taken.toGive);
            if (taken.shown != null) {
                streams.add(new MagnetPullPayload.Stream(target.middle(), taken.shown.getItem(), Mth.clamp(taken.shown.getCount(), 1, 3)));
            }
            piglins |= target.state != null && target.state.is(BlockTags.GUARDED_BY_PIGLINS);
        }
        if (piglins) {
            PiglinAi.angerNearbyPiglins(player, false); // as when gold they guard is broken by hand
        }
        if (!streams.isEmpty()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new MagnetPullPayload(player.getId(), !job.sentFirst, (float) job.radius, streams));
            job.sentFirst = true;
        }
    }

    /**
     * A cell's turn: it must still hold what the search saw, in a loaded chunk, within the radius and two blocks of where
     * he is now, not under his feet, still in sight if it needs sight, and pass the full break check (refused: counted as
     * protected). Then an ore turns to its ground and gives its metal, a metal block comes whole, a container gives its
     * metal stacks.
     */
    private static Taken takeBlock(Job job, ServerPlayer player, Target target) {
        ServerLevel level = job.level;
        BlockPos pos = target.pos;
        if (level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) == null) {
            return Taken.GONE; // never load a chunk for it
        }
        BlockState state = level.getBlockState(pos);
        double reach = job.radius + 2.0;
        if (state != target.state || bodyCentre(player).distanceToSqr(Vec3.atCenterOf(pos)) > reach * reach
                || target.kind != Kind.STORAGE && underFeet(player, pos)
                || needsSight(target.kind) && !Raycasts.inSight(player, pos)
                || target.kind == Kind.BUILT && (holdsItems(level, pos) || isWorkingPiston(level, pos, state))) {
            return Taken.GONE;
        }
        if (BlockRights.massEditRefuses(player, level, pos) || claimedByVillager(level, pos, state)
                || !BlockRights.mayBreak(player, level, pos, state)) {
            job.refused = true;
            return Taken.REFUSED;
        }
        return switch (target.kind) {
            case ORE -> {
                List<ItemStack> metal = oreYield(level, pos, state);
                if (metal.isEmpty()) {
                    yield Taken.REFUSED; // a mod's ore with nothing in it this time: left as it is, not counted
                }
                level.levelEvent(2001, pos, Block.getId(state));
                level.setBlock(pos, groundFor(level, pos, state), Block.UPDATE_ALL);
                level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
                yield Taken.giving(metal);
            }
            case NATURAL, BUILT -> Taken.giving(tearOut(level, pos, state, player));
            case STORAGE -> Taken.moved(takeMetalStacks(player, level, pos));
            case ENTITY -> Taken.GONE;
        };
    }

    /**
     * Takes a metal block out whole: what Silk Touch mining gives (read first), its break particles and sound, and the
     * cell left as the block leaves it (air, or water where it was waterlogged). A door's lower half goes first with drops
     * suppressed; the upper half then breaks by its own shape rule, and the door's loot drops only for a lower half, so one
     * door comes and none falls.
     */
    private static List<ItemStack> tearOut(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        List<ItemStack> drops = wholeDrops(level, pos, state, player);
        level.levelEvent(2001, pos, Block.getId(state));
        int flags = state.getBlock() instanceof DoorBlock ? Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS : Block.UPDATE_ALL;
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), flags);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return drops;
    }

    /**
     * The metal stacks of a container, as far as they fit in his inventory (the rest stays inside, and so do the container
     * and everything else in it), moved straight into his inventory; returns what came.
     */
    private static List<ItemStack> takeMetalStacks(ServerPlayer player, Level level, BlockPos pos) {
        List<ItemStack> came = new ArrayList<>();
        Inventory inventory = player.getInventory();
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null || entity instanceof RandomizableContainer loot && loot.getLootTable() != null) {
            return came;
        }
        if (entity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (Metal.isMetalItem(stack)) {
                    ItemStack rest = stack.copy();
                    inventory.add(rest);
                    int moved = stack.getCount() - rest.getCount();
                    if (moved > 0) {
                        came.add(stack.copyWithCount(moved));
                        container.removeItem(slot, moved);
                    }
                }
            }
            container.setChanged();
            return came;
        }
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (items != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                if (Metal.isMetalItem(items.getStackInSlot(slot))) {
                    ItemStack out = items.extractItem(slot, items.getStackInSlot(slot).getCount(), true);
                    ItemStack rest = out.copy();
                    inventory.add(rest);
                    int moved = out.getCount() - rest.getCount();
                    if (moved > 0) {
                        came.add(items.extractItem(slot, moved, false));
                    }
                }
            }
        }
        return came;
    }

    /**
     * An entity's turn: it must still be there, within the radius and two blocks of him, and still what the pull acts on.
     * Then a dropped item gives what fits (the rest stays where it lies), an empty cart comes as its item (a TNT cart
     * without going off), and a frame or a stand gives its metal and stays.
     */
    private static Taken takeEntity(Job job, ServerPlayer player, Target target) {
        Entity e = target.entity;
        double reach = job.radius + 2.0;
        if (e.isRemoved() || e.level() != job.level || bodyCentre(player).distanceToSqr(e.position()) > reach * reach
                || !wantsEntity(player, e)) {
            return Taken.GONE;
        }
        Inventory inventory = player.getInventory();
        if (e instanceof ItemEntity item) {
            ItemStack stack = item.getItem();
            ItemStack rest = stack.copy();
            inventory.add(rest);
            int moved = stack.getCount() - rest.getCount();
            if (moved <= 0) {
                return Taken.REFUSED;
            }
            ItemStack came = stack.copyWithCount(moved);
            if (rest.isEmpty()) {
                item.discard();
            } else {
                item.setItem(rest);
            }
            return Taken.moved(List.of(came));
        }
        if (e instanceof AbstractMinecart cart) {
            ItemStack item = cart.getPickResult();
            cart.discard(); // not destroyed: a TNT cart does not go off, and an empty one has nothing to spill
            return Taken.giving(List.of(item));
        }
        if (e instanceof ItemFrame frame) {
            ItemStack came = frame.getItem().copy();
            ItemStack rest = came.copy();
            inventory.add(rest);
            if (!rest.isEmpty()) {
                return Taken.REFUSED;
            }
            frame.setItem(ItemStack.EMPTY);
            return Taken.moved(List.of(came));
        }
        if (e instanceof ArmorStand stand) {
            List<ItemStack> came = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = stand.getItemBySlot(slot);
                if (Metal.isMetalItem(stack)) {
                    ItemStack rest = stack.copy();
                    inventory.add(rest);
                    if (rest.getCount() < stack.getCount()) {
                        came.add(stack.copyWithCount(stack.getCount() - rest.getCount()));
                        stand.setItemSlot(slot, rest);
                    }
                }
            }
            return Taken.moved(came);
        }
        return Taken.GONE;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Into his inventory (D-M 4.4), and the end (4.7)

    /**
     * Puts what a take gave into his inventory; what does not fit drops at his feet (no motion, the usual pickup delay), and
     * the first time in a pull he reads {@code pull.full}. Nothing waits in between, so nothing on its way can be lost.
     */
    private static void give(Job job, ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemStack rest = stack.copy();
            player.getInventory().add(rest);
            if (rest.isEmpty()) {
                continue;
            }
            ItemEntity drop = new ItemEntity(job.level, player.getX(), player.getY(), player.getZ(), rest);
            drop.setDeltaMovement(Vec3.ZERO);
            drop.setDefaultPickUpDelay();
            job.level.addFreshEntity(drop);
            if (!job.toldFull) {
                job.toldFull = true;
                player.displayClientMessage(Component.translatable("message.enhanced.pull.full"), true);
            }
        }
    }

    /** The pull is over: its line, the clink of the metal arriving; a pull that did nothing makes the next press wait. */
    private static void finish(Job job, ServerPlayer player) {
        JOBS.remove(player.getUUID());
        tell(job, player);
        if (job.affected > 0) {
            job.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 0.7F);
        } else {
            FOUND_NOTHING.put(player.getUUID(), job.level.getGameTime());
        }
    }

    /** The end line (D-M 4.7): how many things it affected, and whether some metal here was protected. */
    private static void tell(Job job, ServerPlayer player) {
        Component line;
        if (job.affected > 0) {
            line = Component.translatable(job.refused ? "message.enhanced.pull.done_protected" : "message.enhanced.pull.done", job.affected);
        } else {
            line = Component.translatable(job.refused ? "message.enhanced.pull.protected" : "message.enhanced.pull.nothing");
        }
        player.displayClientMessage(line, true);
    }

    private MagnetPull() {
    }
}

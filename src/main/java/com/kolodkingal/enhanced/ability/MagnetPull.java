package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.mixin.AbstractArrowAccessor;
import com.kolodkingal.enhanced.mixin.ArmorStandAccessor;
import com.kolodkingal.enhanced.mixin.ItemFrameAccessor;
import com.kolodkingal.enhanced.network.MagnetPullPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Magneto's Magnetic Pull: Sneak + R with nothing held draws the metal all round him into his inventory (D-M 4). Ores give
 * their metal through rock and turn into their ground, raw-metal blocks come whole through rock, loose metal, stuck arrows,
 * falling metal blocks and empty carts come from all round him, and built metal comes whole when he can see it in the cone
 * he faces (never a block or cart that holds things, never a working piston, never a villager's work block: R36). Hostile
 * creatures lose their metal gear (what they would never drop crumbles), Loki's illusions dissolve, golems and other
 * metal bodies are tugged toward him, and a player he may hurt and can see gives one piece into his inventory (R35), then
 * is spared for a while. With {@code magneto.pullFromStorage} the metal stacks of the containers, item frames and armour
 * stands he can see come too (R38).
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
 *     (a block, a frame or a stand with the full break check of its cell, an entity with the claim of the section it
 *     stands in now); the first that affects anything starts the cooldown and costs the hunger. What is taken goes
 *     straight into his inventory and what does not fit drops at his feet (in Creative too); each tick's takes go out in
 *     one {@link MagnetPullPayload} to him and everyone who sees him.</li>
 *     <li><b>The end</b>: one action-bar line and the clink of the metal arriving. When something dropped at his feet in
 *     this pull, "your inventory is full" first stands its {@value #FULL_LINE_TICKS} ticks, and the end line waits for
 *     them.</li>
 * </ol>
 * A pull ends at once (what it took stays his) when he dies, logs out, changes dimension, loses his power, becomes a
 * Spectator, or is more than the pull's radius from where he pressed (then with its line for what came so far).
 */
public final class MagnetPull {

    /** Magneto's UUID → his pull while it runs. */
    private static final Map<UUID, Job> JOBS = new HashMap<>();
    /** Magneto's UUID → the game time his last pull ended without affecting anything (the retry wait, D-M 4.5 step 2). */
    private static final Map<UUID, Long> FOUND_NOTHING = new HashMap<>();
    /** Magneto's UUID → the game time his last search ended. */
    private static final Map<UUID, Long> SEARCH_ENDS = new HashMap<>();
    /** A player's UUID → the game time a pull last acted on him: no pull acts on him again for {@code pullPlayerGraceTicks}. */
    private static final Map<UUID, Long> PLAYER_GRACE = new HashMap<>();
    /** Mobs without AI a pull tugged, which run no physics of their own: the pull flies them until they land. */
    private static final Map<Mob, Tug> TUGS = new LinkedHashMap<>();
    /** Magneto's UUID → how many bodies his last pull tugged. */
    private static final Map<UUID, Integer> LAST_TUGS = new HashMap<>();
    /** Magneto's UUID → the end line of his last pull, held back while "your inventory is full" stands. */
    private static final Map<UUID, HeldLine> HELD_LINES = new HashMap<>();
    /** The order a player's one piece is taken in: what he holds, then his armour from the head down (D-M 4.3). */
    private static final EquipmentSlot[] PLAYER_PIECES = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** How long a tugged mob without AI is flown at most, in ticks. */
    private static final int TUG_TICKS = 10;

    /** What {@link #searchEndedAt} answers for a Magneto who never searched: long enough ago for any wait. */
    public static final long NEVER = Long.MIN_VALUE / 2;
    /**
     * How long "your inventory is full" stands on his action bar before the end line of the same pull may replace it, in
     * ticks: the time the game shows a line at full strength (D-M 4.4, 4.7; spec 21.7 #29).
     */
    public static final int FULL_LINE_TICKS = 40;

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
        HELD_LINES.remove(id); // this press answers for itself: the last pull's end line is not put over its answer
        PowerData data = Powers.get(player);
        if (!data.isReady(HeroAbility.MAGNETO_METAL, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.pull.recharging"), true);
            return true;
        }
        if (!player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.pull.too_hungry"), true);
            return true;
        }
        Job job = new Job(player, level);
        JOBS.put(id, job);
        LAST_TUGS.put(id, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MAGNET_PULL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        job.lastWork = now;
        search(job, player);
        if (!job.searching && job.order.isEmpty()) {
            finish(job, player, true);
            return true;
        }
        // The field reaches out at once: the clients play its pulse before anything has come.
        Net.sendToPlayersTrackingEntityAndSelf(player, new MagnetPullPayload(player.getId(), true, (float) job.radius, List.of()));
        job.sentFirst = true;
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Every tick

    /**
     * One tick of this Magneto's pull, if one runs (from {@code MagnetoAbilities.tick}): the next slice of the search, or
     * the next takes. A tick in which the press already searched does nothing more. First, the end line of his last pull
     * goes out when its turn has come ({@link #tell}).
     */
    public static void tick(ServerPlayer player) {
        HeldLine held = HELD_LINES.get(player.getUUID());
        if (held != null && player.level().getGameTime() >= held.at()) {
            HELD_LINES.remove(player.getUUID());
            player.displayClientMessage(held.line(), true);
        }
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
            // A pearl, the Bifrost, the Witches' Road, a knockback: the pull stays where he pressed, and ends with the line
            // for what it took so far (D-M 4.5 step 3); nothing more arrives, so no clink.
            finish(job, player, false);
            return;
        }
        if (job.searching) {
            search(job, player);
            if (!job.searching && job.order.isEmpty()) {
                finish(job, player, true);
            }
            return;
        }
        takeSome(job, player);
        if (job.next >= job.order.size()) {
            finish(job, player, true);
        }
    }

    /**
     * Ends this Magneto's pull at once (he died, logged out, changed dimension, lost his power); what was taken stays his,
     * and a cooldown already started stays. An end line still held back is not sent.
     */
    public static void cancel(ServerPlayer player) {
        JOBS.remove(player.getUUID());
        HELD_LINES.remove(player.getUUID());
    }

    /**
     * Forgets every pull, every wait, every held end line, every player's grace and every tug: for the server's stop and
     * for tests.
     */
    public static void clear() {
        JOBS.clear();
        HELD_LINES.clear();
        FOUND_NOTHING.clear();
        SEARCH_ENDS.clear();
        PLAYER_GRACE.clear();
        TUGS.clear();
        LAST_TUGS.clear();
    }

    /**
     * Once per level tick (from {@code MagnetoAbilities.tickHeld}): the mobs without AI a pull of this level tugged fly on
     * by their own velocity, as a mob's physics would carry it (D-M 4.3), until they land or {@value #TUG_TICKS} ticks
     * have passed. A tug goes on when the pull that gave it has ended.
     */
    public static void tickTugs(ServerLevel level) {
        if (TUGS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Mob, Tug>> it = TUGS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Mob, Tug> entry = it.next();
            Mob mob = entry.getKey();
            if (mob.level() != level) {
                continue;
            }
            if (mob.isRemoved() || !mob.isAlive() || !mob.isNoAi() || Telekinesis.isHeld(mob) || --entry.getValue().left < 0) {
                it.remove();
                continue;
            }
            Vec3 v = mob.getDeltaMovement();
            mob.move(MoverType.SELF, v);
            mob.setDeltaMovement(v.x * 0.91, (v.y - 0.08) * 0.98, v.z * 0.91);
            mob.hasImpulse = true;
            mob.resetFallDistance();
            if (mob.onGround() && v.y <= 0.0) {
                mob.setDeltaMovement(Vec3.ZERO); // landed: it stays where the tug brought it
                it.remove();
            }
        }
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

    /**
     * How many ticks this player is still spared by every pull, because one acted on him ({@code
     * magneto.pullPlayerGraceTicks}); 0 if none is.
     */
    @VisibleForTesting
    public static long playerGraceLeft(Player player) {
        Long at = PLAYER_GRACE.get(player.getUUID());
        if (at == null) {
            return 0;
        }
        return Math.max(0, at + EnhancedConfig.PULL_PLAYER_GRACE_TICKS.get() - player.level().getGameTime());
    }

    /** How many bodies this Magneto's last pull tugged (his current one, while it runs). */
    @VisibleForTesting
    public static int lastTugs(ServerPlayer player) {
        return LAST_TUGS.getOrDefault(player.getUUID(), 0);
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
        /** An entity: what happens to it is its own rule ({@link Act}). */
        ENTITY
    }

    /** What the pull does to an entity (D-M 4.3). */
    private enum Act {
        /** A dropped metal item: it comes, as far as it fits. */
        ITEM,
        /** A metal arrow or trident lying stuck that anyone may pick up: it comes as walking over it gives it. */
        ARROW,
        /** A falling or loose lifted metal block: it comes as its item. */
        BLOCK,
        /** An empty minecart: it comes as its item. */
        CART,
        /** A conjured body (a Loki illusion) holding or wearing metal: it dissolves, and gives nothing. */
        DISSOLVE,
        /** A creature he may fight wearing or holding metal, or a boss: its gear is torn off (a boss's hook answers first). */
        GEAR,
        /** A player he may hurt and can see: one piece, or his nanites, or a tug. */
        PLAYER,
        /** A metal body (an Iron Golem, a Sentry, the Hex's Vision): it is tugged toward him. */
        TUG,
        /** An item frame with metal in it ({@code magneto.pullFromStorage}): its item comes, the frame stays. */
        FRAME,
        /** An armour stand with metal on it ({@code magneto.pullFromStorage}): its metal comes, the stand stays. */
        STAND
    }

    /** A tugged mob without AI, flown by the pull for a few more ticks. */
    private static final class Tug {
        int left = TUG_TICKS;
    }

    /** The end line of a pull that ended, and the game time it goes out. */
    private record HeldLine(Component line, long at) {
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
        /** Whether he read {@code pull.full} in this pull, and the game time he did. */
        boolean toldFull;
        long fullAt;
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
     * on), and, for a player, a frame or a stand, in sight. Adventure and spawn protection guard blocks, not these; but a
     * frame or a stand is someone's build, asked as a container is ({@link #buildRefused}).
     */
    private static void addEntities(Job job, ServerPlayer player) {
        double r2 = job.radius * job.radius;
        AABB box = new AABB(job.centre, job.centre).inflate(job.radius);
        for (Entity e : job.level.getEntities((Entity) null, box, e -> e.position().distanceToSqr(job.centre) <= r2)) {
            Act act = actOn(player, e);
            if (act == null) {
                continue;
            }
            if (!claimAllows(job, player, e) || isBuild(act) && buildRefused(player, job.level, e, false)) {
                job.refused = true;
                continue;
            }
            double distSq = e.position().distanceToSqr(job.centre);
            if (!job.admits(distSq)) {
                continue;
            }
            if (!seen(player, e)) {
                continue;
            }
            if (Shelters.stops(player, player.getEyePosition(), e, 0.0F)) {
                continue; // 3.3: behind a Hex's wall, or Billy's barrier faces him
            }
            job.offer(new Target(Kind.ENTITY, null, null, e, distSq));
        }
    }

    /**
     * Whether the claims let him act on what stands in this entity's section (probed once a section). Where a boss fight
     * protects the cell the entity stands on no claim is asked: the arena guards its blocks, not what lies or fights on
     * them, and it would refuse any break there (a minion's gear still crumbles, a loose ingot still comes). That
     * answer is the entity's own and is never kept for the section: an arena's round edge cuts through sections, and
     * what lies outside it in the same section (a claimed base's iron farm) is still asked the claim.
     */
    private static boolean claimAllows(Job job, ServerPlayer player, Entity e) {
        BlockPos on = e.getOnPos();
        if (BossFights.protects(job.level, on)) {
            return true;
        }
        long section = SectionPos.asLong(e.blockPosition());
        Boolean allowed = job.claims.get(section);
        if (allowed == null) {
            allowed = BlockRights.postBreak(player, job.level, on, job.level.getBlockState(on));
            job.claims.put(section, allowed);
        }
        return allowed;
    }

    /** An item frame's or an armour stand's metal ({@code magneto.pullFromStorage}): someone's build, as a container is. */
    private static boolean isBuild(Act act) {
        return act == Act.FRAME || act == Act.STAND;
    }

    /**
     * Whether the rights of the frame's or the stand's own cell refuse him its metal (R38: "claims asked"), as a container's
     * cell would: the area rule of the mass edits (an arena, the island's hidden ground, a stranger's Hex), then at the
     * search the game's own rights (Adventure, spawn protection) and at the take ({@code full}) the full break check.
     */
    private static boolean buildRefused(ServerPlayer player, ServerLevel level, Entity e, boolean full) {
        BlockPos pos = e.blockPosition();
        if (BlockRights.massEditRefuses(player, level, pos)) {
            return true;
        }
        return full ? !BlockRights.mayBreak(player, level, pos, level.getBlockState(pos)) : !BlockRights.vanillaRights(player, level, pos);
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
     * every copper block its oxidation and wax); its own item if that loot is empty. Read before the block goes (a falling
     * or lifted block, which stands nowhere, is read where it is, with no block entity).
     */
    private static List<ItemStack> wholeDrops(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                                              ServerPlayer player) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, blockEntity, player, tool));
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
     * What the pull would do to this entity (D-M 4.2, 4.3), or null: never anything to one any telekinesis holds, the
     * Sokovia lift carries, that rides or is ridden. A dropped metal item he could pick up by hand and has room for (never
     * a drop a fight keeps for a dead fighter); a metal arrow or trident lying stuck that walking over it would give
     * him ({@link #isStuckMetal}); a falling or loose lifted metal block; an empty minecart (never the command block
     * one); a Loki illusion holding or wearing metal; a creature he may fight wearing or holding metal, or a boss
     * ({@code magneto.pullTakesGear}); a player he may hurt ({@link #wantsPlayer}); a metal body he may tug ({@link
     * #mayTug}); and, with {@code magneto.pullFromStorage}, an item frame or an armour stand with metal on it that no
     * map maker's lock (a fixed frame, a stand's disabled slot) keeps from a hand. Nothing that belongs to a fight but
     * its creatures' gear.
     */
    @Nullable
    private static Act actOn(ServerPlayer player, Entity e) {
        if (e == player || !e.isAlive() || e.isPassenger() || e.isVehicle() || Telekinesis.isHeld(e) || BossFights.isLocked(e)) {
            return null;
        }
        if (e instanceof ItemEntity item) {
            return wantsItem(player, item) ? Act.ITEM : null;
        }
        if (e instanceof AbstractArrow arrow) {
            return isStuckMetal(player, arrow) && BossFights.fightOf(e) == null
                    && hasRoom(player, arrow.getPickupItemStackOrigin()) ? Act.ARROW : null;
        }
        if (e instanceof FallingBlockEntity || e instanceof TelekineticBlockEntity) {
            return Metal.isMagnetic(e) && BossFights.fightOf(e) == null ? Act.BLOCK : null; // a borrowed block carries its fight's mark
        }
        if (e instanceof AbstractMinecart cart) {
            return isEmptyCart(cart) && BossFights.fightOf(e) == null ? Act.CART : null;
        }
        if (Metal.isConjured(e)) {
            return e instanceof LivingEntity body && wearsMetal(body) ? Act.DISSOLVE : null;
        }
        if (e instanceof Player other) {
            return wantsPlayer(player, other) ? Act.PLAYER : null;
        }
        if (e instanceof LivingEntity living) {
            if (EnhancedConfig.PULL_TAKES_GEAR.get() && AreaTargets.mayHurt(player, living, false)
                    && (isBoss(living) || wearsMetal(living))) {
                return Act.GEAR;
            }
            if (mayTug(player, living)) {
                return Act.TUG;
            }
        }
        if (EnhancedConfig.PULL_FROM_STORAGE.get()) {
            if (e instanceof ItemFrame frame) {
                // A fixed frame (a map maker's lock) gives its item to no hand, so to no pull either.
                return !((ItemFrameAccessor) frame).enhanced$isFixed() && Metal.isMetalItem(frame.getItem())
                        ? Act.FRAME : null;
            }
            if (e instanceof ArmorStand stand) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (standGives(stand, slot)) {
                        return Act.STAND;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Whether this slot of the armour stand holds metal a hand could take: not a slot that its {@code DisabledSlots}
     * lock (a map maker's) closes, whether wholly or only against taking out.
     */
    private static boolean standGives(ArmorStand stand, EquipmentSlot slot) {
        int flag = slot.getFilterFlag();
        return (((ArmorStandAccessor) stand).enhanced$getDisabledSlots() & (1 << flag | 1 << flag + 8)) == 0
                && Metal.isMetalItem(stand.getItemBySlot(slot));
    }

    /**
     * Whether he can see what of the entity the pull needs to see: a player's eyes, the middle of an item frame or an
     * armour stand (someone's build, which gives its metal only in sight). Everything else comes from all round him.
     */
    private static boolean seen(ServerPlayer player, Entity e) {
        if (e instanceof Player) {
            return Raycasts.inSight(player, e.getEyePosition());
        }
        if (e instanceof ItemFrame || e instanceof ArmorStand) {
            return Raycasts.inSight(player, e.position().add(0, e.getBbHeight() * 0.5, 0));
        }
        return true;
    }

    /** A boss, whose own hook answers Magneto's strip (Thanos's plate, Apocalypse's window): it is never tugged. */
    private static boolean isBoss(Entity e) {
        return e instanceof BossHooks && e.getType().is(ModTags.BOSSES);
    }

    /** Whether any slot of the body (armour, either hand, horse armour) holds a metal item. */
    private static boolean wearsMetal(LivingEntity body) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (Metal.isMetalItem(body.getItemBySlot(slot))) {
                return true;
            }
        }
        return false;
    }

    /**
     * A metal arrow or trident lying stuck in the ground that walking over it would give him: one anyone may pick up (a
     * skeleton's arrows stay), done shaking from its landing, and, for a trident, his own or no one's who is here (a
     * thrown trident goes back only to the one who threw it, so another player's stays where it is stuck).
     */
    private static boolean isStuckMetal(ServerPlayer player, AbstractArrow arrow) {
        if (!((AbstractArrowAccessor) arrow).enhanced$isInGround() || arrow.shakeTime > 0
                || arrow.pickup != AbstractArrow.Pickup.ALLOWED
                || !arrow.getType().is(ModTags.METALLIC_PROJECTILES) || arrow.getPickupItemStackOrigin().isEmpty()) {
            return false;
        }
        Entity owner = arrow.getOwner();
        return !(arrow instanceof ThrownTrident) || owner == null || owner.getUUID().equals(player.getUUID());
    }

    /**
     * Whether a player the pull may act on (D-M 4.3, R35): {@code magneto.pullTakesPlayerGear} is on, he may hurt him (PvP
     * on, no teammate, not in Creative), no pull acted on him less than {@code magneto.pullPlayerGraceTicks} ago, and he has
     * something to give: a metal piece in a hand or on his body, an Iron Spider's nanites not yet scattered, or a metal
     * body to tug. (Sight is asked by {@link #seen}.)
     */
    private static boolean wantsPlayer(ServerPlayer magneto, Player other) {
        if (!EnhancedConfig.PULL_TAKES_PLAYER_GEAR.get() || !AreaTargets.mayHurt(magneto, other, false) || playerGraceLeft(other) > 0) {
            return false;
        }
        for (EquipmentSlot slot : PLAYER_PIECES) {
            if (Metal.isMetalItem(other.getItemBySlot(slot))) {
                return true;
            }
        }
        return SpiderServer.isIronSpider(other) && !SpiderServer.isStripped(other) || mayTugPlayer(other);
    }

    /**
     * Whether a player's metal body may be tugged (Vision, the Ultron hero): never a phased Vision, never the Iron Spider
     * (his metal is the nanites, which a pull scatters), never someone Mjolnir pins, and never a fighter of a boss fight,
     * whom a tug would carry past the fight's carry limit.
     */
    private static boolean mayTugPlayer(Player other) {
        return EnhancedConfig.PULL_TUG.get() > 0.0 && Metal.isMetalBody(other) && !SpiderServer.isIronSpider(other)
                && !MjolnirPin.isPinned(other) && !(other instanceof ServerPlayer fighter && BossFights.isParticipant(fighter));
    }

    /**
     * Whether a metal body that is no player and no boss may be tugged (an Iron Golem, a Sentry, the Hex's Vision; {@code
     * magneto.pullTug} above 0): never one a fight owns (a fight's Sentries), never one Mjolnir pins, and never a creature
     * another player owns unless he may hurt that player.
     */
    private static boolean mayTug(ServerPlayer magneto, LivingEntity body) {
        if (EnhancedConfig.PULL_TUG.get() <= 0.0 || !Metal.isMetalBody(body) || isBoss(body) || BossFights.fightOf(body) != null
                || MjolnirPin.isPinned(body)) {
            return false;
        }
        if (body instanceof OwnableEntity pet && pet.getOwnerUUID() != null && !pet.getOwnerUUID().equals(magneto.getUUID())) {
            return pet.getOwner() instanceof Player owner && AreaTargets.mayHurt(magneto, owner, false);
        }
        return true;
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
        return (!item.hasPickUpDelay() || marked) && hasRoom(player, stack);
    }

    /** Whether some of the stack fits in his inventory now: what cannot never takes a place of the heap. */
    private static boolean hasRoom(ServerPlayer player, ItemStack stack) {
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
        /** It did something there that gives him nothing (a tug, a dissolve, a crumble, a scatter, a boss's hook). */
        static final Taken SEEN_THERE = new Taken(true, true, List.of(), null);

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
                Powers.get(player).startCooldown(HeroAbility.MAGNETO_METAL, job.level.getGameTime(), EnhancedConfig.PULL_COOLDOWN.get());
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
            Net.sendToPlayersTrackingEntityAndSelf(player,
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
        List<ItemStack> drops = wholeDrops(level, pos, state, level.getBlockEntity(pos), player);
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
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null || entity instanceof RandomizableContainer loot && loot.getLootTable() != null) {
            return came;
        }
        if (entity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (Metal.isMetalItem(stack)) {
                    int moved = addWhatFits(player, stack.copy());
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
                ItemStack there = items.getStackInSlot(slot);
                if (!Metal.isMetalItem(there)) {
                    continue;
                }
                int fits = Math.min(there.getCount(), room(player.getInventory(), there));
                // Only what really came out is given: a mod's store may hand out less than it promised.
                ItemStack out = fits > 0 ? items.extractItem(slot, fits, false) : ItemStack.EMPTY;
                if (out.isEmpty()) {
                    continue;
                }
                ItemStack rest = out.copy();
                int moved = addWhatFits(player, rest);
                if (moved > 0) {
                    came.add(out.copyWithCount(moved));
                }
                if (!rest.isEmpty()) {
                    dropAtFeet(player, items.insertItem(slot, rest, false)); // back where it was, or at his feet
                }
            }
        }
        return came;
    }

    /**
     * An entity's turn: it must still be there, within the radius and two blocks of him, still what the pull acts on, and
     * still in sight if it needs sight; and the claims must still let him act where it stands now (it may have walked or
     * drifted into a claimed section since the search), a frame or a stand passing the full break check of its cell
     * (refused: counted as protected). Then a dropped item gives what fits (the rest stays where it lies), a stuck arrow
     * comes as walking over it gives it, a falling or lifted block and an empty cart come as their items (a TNT cart
     * without going off), an illusion dissolves, a creature's gear is torn off, a player gives one thing, a metal body is
     * tugged, and a frame or a stand gives its metal and stays.
     */
    private static Taken takeEntity(Job job, ServerPlayer player, Target target) {
        Entity e = target.entity;
        double reach = job.radius + 2.0;
        if (e.isRemoved() || e.level() != job.level || bodyCentre(player).distanceToSqr(e.position()) > reach * reach) {
            return Taken.GONE;
        }
        Act act = actOn(player, e);
        if (act == null || !seen(player, e) || Shelters.shelters(player, player.getEyePosition(), e)) {
            return Taken.GONE; // (3.3: it went behind a wall since the search)
        }
        if (!claimAllows(job, player, e) || isBuild(act) && buildRefused(player, job.level, e, true)) {
            job.refused = true;
            return Taken.REFUSED;
        }
        return switch (act) {
            case ITEM -> takeItem(player, (ItemEntity) e);
            case ARROW -> {
                ItemStack arrow = ((AbstractArrow) e).getPickupItemStackOrigin().copy();
                if (room(player.getInventory(), arrow) < arrow.getCount()) {
                    yield Taken.REFUSED; // no room after all: it stays where it is stuck
                }
                player.getInventory().add(arrow.copy());
                e.discard();
                yield Taken.moved(List.of(arrow));
            }
            case BLOCK -> {
                BlockState state = e instanceof FallingBlockEntity falling
                        ? falling.getBlockState() : ((TelekineticBlockEntity) e).getBlockState();
                List<ItemStack> drops = wholeDrops(job.level, e.blockPosition(), state, null, player);
                e.discard();
                yield Taken.giving(drops);
            }
            case CART -> {
                ItemStack item = ((AbstractMinecart) e).getPickResult();
                item.set(DataComponents.CUSTOM_NAME, e.getCustomName()); // a named cart keeps its name, as when broken
                e.discard(); // not destroyed: a TNT cart does not go off, and an empty one has nothing to spill
                yield Taken.giving(List.of(item));
            }
            case DISSOLVE -> {
                Metal.dissolve(e, null); // magnetism sees through Loki's trick: nothing of what it wore is made real
                yield Taken.SEEN_THERE;
            }
            case GEAR -> tearGear(player, (LivingEntity) e);
            case PLAYER -> takeFromPlayer(job, player, (Player) e);
            case TUG -> {
                tug(player, (LivingEntity) e);
                yield Taken.SEEN_THERE;
            }
            case FRAME -> takeFromFrame(player, (ItemFrame) e);
            case STAND -> takeFromStand(player, (ArmorStand) e);
        };
    }

    /** A dropped item gives what fits; the rest stays where it lies. */
    private static Taken takeItem(ServerPlayer player, ItemEntity item) {
        ItemStack stack = item.getItem();
        ItemStack rest = stack.copy();
        int moved = addWhatFits(player, rest);
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

    /**
     * A creature's metal is torn off (D-M 4.3): a boss answers first through its hook, as to the strip (a plate, a window),
     * and then nothing else is taken; otherwise every slot holding a metal item is emptied, with a chain's break. A real
     * piece comes to him; one the creature would never drop (a Horseman's) crumbles where it was worn.
     */
    private static Taken tearGear(ServerPlayer player, LivingEntity creature) {
        if (creature instanceof BossHooks boss && boss.onMagnetoStrip(player)) {
            return Taken.SEEN_THERE;
        }
        ServerLevel level = player.serverLevel();
        List<ItemStack> real = new ArrayList<>();
        boolean torn = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = creature.getItemBySlot(slot);
            if (!Metal.isMetalItem(stack)) {
                continue;
            }
            boolean given = Metal.isRealGear(creature, slot);
            creature.setItemSlot(slot, ItemStack.EMPTY);
            torn = true;
            if (given) {
                real.add(stack.copy());
            } else {
                Metal.crumble(level, creature, slot, stack);
            }
        }
        if (!torn) {
            return Taken.GONE; // a boss whose hook let it be, with nothing on it
        }
        level.playSound(null, creature.getX(), creature.getY(), creature.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.7F, 0.8F);
        return Taken.giving(real);
    }

    /**
     * A player gives one thing (D-M 4.3, R35), the first that applies: the metal item in his main hand, then his off hand,
     * then his metal armour from the head down, which flies into Magneto's inventory (with {@code
     * magneto.pullPlayerGearToFeet}: drops at the player's own feet, unmarked, with a torn-off piece's pickup delay, as
     * V's would); else an Iron Spider's nanites scatter; else his metal body is tugged. Then every pull spares him for
     * {@code magneto.pullPlayerGraceTicks}.
     */
    private static Taken takeFromPlayer(Job job, ServerPlayer magneto, Player other) {
        for (EquipmentSlot slot : PLAYER_PIECES) {
            ItemStack stack = other.getItemBySlot(slot);
            if (!Metal.isMetalItem(stack)) {
                continue;
            }
            PLAYER_GRACE.put(other.getUUID(), job.level.getGameTime());
            other.setItemSlot(slot, ItemStack.EMPTY);
            job.level.playSound(null, other.getX(), other.getY(), other.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.7F, 0.8F);
            if (EnhancedConfig.PULL_PLAYER_GEAR_TO_FEET.get()) {
                // At his own feet, unmarked, with the 40 ticks of a torn-off piece: he can take it back first.
                ItemEntity drop = new ItemEntity(job.level, other.getX(), other.getY(), other.getZ(), stack.copy(), 0.0, 0.2, 0.0);
                drop.setPickUpDelay(40);
                job.level.addFreshEntity(drop);
                return Taken.SEEN_THERE;
            }
            return Taken.giving(List.of(stack.copy()));
        }
        if (SpiderServer.onStripped(other)) {
            PLAYER_GRACE.put(other.getUUID(), job.level.getGameTime());
            return Taken.SEEN_THERE; // the nanites of an Iron Spider's suit scatter
        }
        if (mayTugPlayer(other)) {
            PLAYER_GRACE.put(other.getUUID(), job.level.getGameTime());
            tug(magneto, other);
            return Taken.SEEN_THERE;
        }
        return Taken.GONE;
    }

    /**
     * A metal body is tugged toward him (D-M 4.3): one impulse of {@code min(magneto.pullTug, 0.15 x distance)} blocks a
     * tick across plus 0.25 up, its fall forgotten, a mob's path stopped and an Iron Golem never set against him; never
     * hurt. A mob without AI runs no physics of its own, so the pull flies it ({@link #tickTugs}). Heard, softer than a
     * grab.
     */
    private static void tug(ServerPlayer magneto, LivingEntity body) {
        Vec3 across = new Vec3(magneto.getX() - body.getX(), 0.0, magneto.getZ() - body.getZ());
        double distance = across.length();
        Vec3 impulse = new Vec3(0.0, 0.25, 0.0);
        if (distance > 1.0E-4) {
            impulse = impulse.add(across.scale(Math.min(EnhancedConfig.PULL_TUG.get(), 0.15 * distance) / distance));
        }
        body.setDeltaMovement(impulse);
        body.hasImpulse = true;
        body.hurtMarked = true; // a player's client moves him: send him the new velocity
        body.resetFallDistance();
        if (body instanceof Mob mob) {
            mob.getNavigation().stop();
            if (mob instanceof IronGolem && mob.getTarget() == magneto) {
                mob.setTarget(null);
            }
            if (mob.isNoAi()) {
                TUGS.put(mob, new Tug());
            }
        }
        LAST_TUGS.merge(magneto.getUUID(), 1, Integer::sum);
        body.level().playSound(null, body.getX(), body.getY(), body.getZ(), ModSounds.MAGNET_GRAB.get(), SoundSource.PLAYERS, 0.5F, 0.8F);
    }

    /** An item frame's metal item comes if it fits; the frame stays. */
    private static Taken takeFromFrame(ServerPlayer player, ItemFrame frame) {
        ItemStack came = frame.getItem().copy();
        if (room(player.getInventory(), came) < came.getCount()) {
            return Taken.REFUSED;
        }
        player.getInventory().add(came.copy());
        frame.setItem(ItemStack.EMPTY);
        return Taken.moved(List.of(came));
    }

    /** An armour stand's metal comes as far as it fits, but never from a locked slot; the stand stays. */
    private static Taken takeFromStand(ServerPlayer player, ArmorStand stand) {
        List<ItemStack> came = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = stand.getItemBySlot(slot);
            if (standGives(stand, slot)) {
                ItemStack rest = stack.copy();
                int moved = addWhatFits(player, rest);
                if (moved > 0) {
                    came.add(stack.copyWithCount(moved));
                    stand.setItemSlot(slot, rest);
                }
            }
        }
        return Taken.moved(came);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Into his inventory (D-M 4.4), and the end (4.7)

    /**
     * Puts as much of {@code stack} into his inventory as fits, as picking it up would, and leaves the rest in {@code
     * stack}. {@code Inventory.add} alone would not do: for a player with infinite materials (Creative) it empties the
     * stack and reports success when it does not all fit, and the pull would delete what it took from someone (D-M 4.4:
     * nothing is ever lost).
     *
     * @return how many went in
     */
    private static int addWhatFits(ServerPlayer player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        int fits = Math.min(stack.getCount(), room(inventory, stack));
        if (fits > 0) {
            inventory.add(stack.split(fits)); // all of it fits, so nothing is cut away
        }
        return fits;
    }

    /**
     * How many of the stack the inventory takes now, as {@code Inventory.add} fills it: the room left in the stacks of the
     * same item (the off hand's too), then whole free slots of the main inventory. A damaged stack only goes into a free
     * slot.
     */
    private static int room(Inventory inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int room = 0;
        if (!stack.isDamaged()) {
            for (ItemStack there : inventory.items) {
                room += roomIn(inventory, there, stack);
            }
            room += roomIn(inventory, inventory.offhand.get(0), stack);
        }
        for (ItemStack there : inventory.items) {
            if (there.isEmpty()) {
                room += inventory.getMaxStackSize(stack);
            }
        }
        return room;
    }

    /** The room a slot's stack has left for more of {@code stack} (the same item, stackable, not full). */
    private static int roomIn(Inventory inventory, ItemStack there, ItemStack stack) {
        if (there.isEmpty() || !there.isStackable() || !ItemStack.isSameItemSameComponents(there, stack)) {
            return 0;
        }
        return Math.max(0, inventory.getMaxStackSize(there) - there.getCount());
    }

    /**
     * Puts what a take gave into his inventory; what does not fit drops at his feet (no motion, the usual pickup delay), and
     * the first time in a pull he reads {@code pull.full}. Nothing waits in between, so nothing on its way can be lost.
     */
    private static void give(Job job, ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemStack rest = stack.copy();
            addWhatFits(player, rest);
            if (rest.isEmpty()) {
                continue;
            }
            dropAtFeet(player, rest);
            if (!job.toldFull) {
                job.toldFull = true;
                job.fullAt = job.level.getGameTime();
                player.displayClientMessage(Component.translatable("message.enhanced.pull.full"), true);
            }
        }
    }

    /** Drops a stack at his feet, with no motion and the usual pickup delay (nothing, if it is empty). */
    private static void dropAtFeet(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
        drop.setDeltaMovement(Vec3.ZERO);
        drop.setDefaultPickUpDelay();
        player.level().addFreshEntity(drop);
    }

    /**
     * The pull is over: its line, and the clink of the metal arriving when it took its last target ({@code arrived}; not
     * when he went too far from where he pressed); a pull that did nothing makes the next press wait.
     */
    private static void finish(Job job, ServerPlayer player, boolean arrived) {
        JOBS.remove(player.getUUID());
        tell(job, player);
        if (job.affected == 0) {
            FOUND_NOTHING.put(player.getUUID(), job.level.getGameTime());
        } else if (arrived) {
            job.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
    }

    /**
     * The end line (D-M 4.7): how many things it affected, and whether some metal here was protected. When he read {@code
     * pull.full} less than {@value #FULL_LINE_TICKS} ticks ago, the end line is held back until then ({@link #tick} sends
     * it), or it would replace that line before he could read it, while the rest lies at his feet. A new press of his, or
     * whatever ends a pull at once, drops it.
     */
    private static void tell(Job job, ServerPlayer player) {
        Component line;
        if (job.affected > 0) {
            line = Component.translatable(job.refused ? "message.enhanced.pull.done_protected" : "message.enhanced.pull.done", job.affected);
        } else {
            line = Component.translatable(job.refused ? "message.enhanced.pull.protected" : "message.enhanced.pull.nothing");
        }
        long readFrom = job.fullAt + FULL_LINE_TICKS;
        if (job.toldFull && job.level.getGameTime() < readFrom) {
            HELD_LINES.put(player.getUUID(), new HeldLine(line, readFrom));
            return;
        }
        player.displayClientMessage(line, true);
    }

    private MagnetPull() {
    }
}

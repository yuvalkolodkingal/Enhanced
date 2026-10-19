package com.kolodkingal.enhanced.cottage;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Asgardians;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.config.CottageConfig;
import com.kolodkingal.enhanced.hydra.HydraGarrison;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.WitchesRoad;
import com.kolodkingal.enhanced.world.SitePoints;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The keeper of Agatha's Cottages (3.7, the plan's 9.4; the {@code HydraGarrison} pattern). Every {@link #QUICK} ticks
 * per player, the cottages near him are found by their structure starts (cached per chunk for a while) and recorded
 * ({@link Cottages}); for each within {@link #RADIUS} of him:
 * <ul>
 *   <li>every {@link #PERIOD} ticks {@link #keep}: each of Agatha, Nicky and Rio exists once (a {@code
 *   cottage_resident}, slot {@code cottage/<key>/<ROLE>}); one missing (lost, {@code /kill}ed) is made again at home,
 *   unseen (the first of each is made at once); Señor Scratchy is kept at his hutch and may run off in the morning; the
 *   cellar's door is added to the Witches' Road. With {@code residents} off nobody lives there.</li>
 *   <li>one of each (3.5.1 rule, {@code agathaYields}): while any player online is Agatha, the cottage's Agatha is away
 *   (she leaves in a violet shimmer, none is made, one loading in is refused) and comes back, in the same shimmer, when
 *   nobody is; Nicky runs to a player who is Agatha: "Mom! You're home!" (once per visit).</li>
 *   <li>every quick look: a run-off Scratchy brought near his hutch is home ({@link Scratchy#quick}); the cottage's
 *   ambience (birds and a kettle) for whoever is inside its grounds.</li>
 * </ul>
 * The residents are not monsters: they live there in Peaceful too. The GameTests drive {@link #keep} and {@link #quick}
 * on fake cottages.
 */
public final class CottageKeeper {
    public static final ResourceKey<Structure> COTTAGE = ResourceKey.create(Registries.STRUCTURE,
            Enhanced.id("agatha_cottage"));
    /** How often the keeper keeps a cottage near a player (ticks). */
    public static final int PERIOD = 100;
    /** How often the quick looks run (ticks). */
    public static final int QUICK = 10;
    /** The cottages within this of a player are kept. */
    public static final double RADIUS = 64.0;
    /** A resident (or Scratchy) is never made again this near a player, or in his sight. */
    public static final double NOT_WITHIN = 12.0;
    /** The chunks round a home that must be loaded before its resident is made. */
    public static final int AREA = 1;
    /** How far a resident strolls from home. */
    public static final int HOME_RADIUS = 16;
    /** Nicky runs to a player who is Agatha within this of him. */
    public static final double GREET_RANGE = 16.0;
    /** A visit ends when the player who is Agatha is this far from the cottage (Nicky greets her again next time). */
    public static final double VISIT_RANGE = 48.0;
    /** The ambience is played this often to whoever is in a cottage's grounds (its loop lasts 20 s). */
    public static final int AMBIENCE_TICKS = 400;
    /** Its volume (the loop is mastered loud: about -17.5 LUFS). */
    public static final float AMBIENCE_VOLUME = 0.5F;
    private static final int CACHE_TICKS = 600;
    private static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.62F, 0.3F, 0.92F), 1.6F);

    private record Found(long until, LongSet starts) {
    }

    private static final Map<ResourceKey<Level>, Map<Long, Found>> CACHE = new HashMap<>();
    /** The players who are Agatha and whom Nicky greeted this visit: player → cottage keys. */
    private static final Map<UUID, Set<Long>> GREETED = new HashMap<>();

    // ---- the options (read when used; their defaults before the config loads) ----

    static boolean residents() {
        return !EnhancedConfig.SPEC.isLoaded() || CottageConfig.RESIDENTS.get();
    }

    static boolean agathaYields() {
        return !EnhancedConfig.SPEC.isLoaded() || CottageConfig.AGATHA_YIELDS.get();
    }

    static boolean roadDoor() {
        return !EnhancedConfig.SPEC.isLoaded() || CottageConfig.ROAD_DOOR.get();
    }

    /** Whether the cottages' Agatha is away now: a player online is Agatha (and {@code agathaYields}). */
    public static boolean agathaAway(MinecraftServer server) {
        return agathaYields() && Asgardians.yields(server, Role.AGATHA);
    }

    // ---- the player's tick ----

    /** Every player tick (from {@link CottageEvents}): the cottages near {@code player}. */
    public static void tick(ServerPlayer player) {
        int beat = player.tickCount + player.getId();
        if (beat % QUICK != 0 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        forgetVisits(level, player);
        if (level.dimension() != Level.OVERWORLD) {
            return; // the cottages stand in the Overworld's forests
        }
        LongSet starts = startsNear(level, player.blockPosition());
        if (starts.isEmpty()) {
            return;
        }
        Cottages store = Cottages.get(level);
        boolean full = beat % PERIOD == 0;
        for (long start : starts) {
            Cottages.Cottage cottage = store.byKey(start);
            if (cottage == null) {
                if (!full) {
                    continue;
                }
                cottage = resolve(level, store, start);
                if (cottage == null) {
                    continue;
                }
            }
            if (cottage.distance(player.position()) > RADIUS) {
                continue;
            }
            if (full) {
                keep(level, cottage);
            }
            quick(level, cottage, player);
        }
    }

    /** The start chunks of the cottages referenced by the loaded chunks round {@code pos} (cached a while). */
    static LongSet startsNear(ServerLevel level, BlockPos pos) {
        long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        long now = level.getGameTime();
        Map<Long, Found> cache = CACHE.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Found found = cache.get(chunk);
        if (found != null && found.until() > now) {
            return found.starts();
        }
        if (cache.size() > 4096) {
            cache.entrySet().removeIf(e -> e.getValue().until() <= now);
        }
        LongSet starts = new LongOpenHashSet();
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(COTTAGE);
        if (structure != null) {
            int reach = ((int) RADIUS + 32) >> 4;
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    LevelChunk c = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                    if (c != null) {
                        starts.addAll(c.getReferencesForStructure(structure));
                    }
                }
            }
        }
        cache.put(chunk, new Found(now + CACHE_TICKS, starts));
        return starts;
    }

    /** Records the cottage whose structure start is in chunk {@code start}, from its piece; null if not loaded. */
    @Nullable
    static Cottages.Cottage resolve(ServerLevel level, Cottages store, long start) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(COTTAGE);
        if (structure == null) {
            return null;
        }
        ChunkPos at = new ChunkPos(start);
        ChunkAccess chunk = level.getChunk(at.x, at.z, ChunkStatus.STRUCTURE_STARTS, false);
        StructureStart found = chunk == null ? null : chunk.getStartForStructure(structure);
        if (found == null || !found.isValid()) {
            return null;
        }
        for (StructurePiece piece : found.getPieces()) {
            if (piece instanceof TemplateSitePiece site) {
                return store.register(start, points(site));
            }
        }
        return null;
    }

    /** A built cottage's points, from its piece. */
    public static Cottages.Points points(TemplateSitePiece piece) {
        return Cottages.Points.of(local -> SitePoints.toWorld(piece, local), piece.getBoundingBox());
    }

    // ---- keeping a cottage ----

    /**
     * The keeper's work for one cottage: its three residents (once each; Agatha away while a player is Agatha), Señor
     * Scratchy, the cellar's Road door and the morning's run-off. Returns what it made.
     */
    public static List<Entity> keep(ServerLevel level, Cottages.Cottage cottage) {
        List<Entity> made = new ArrayList<>();
        Cottages.Points points = cottage.points();
        for (Role role : Cottages.ROLES) {
            UUID id = cottage.resident(role);
            Entity present = id == null ? null : level.getEntity(id);
            BlockPos home = points.home(role);
            if (!residents()) {
                if (present != null) {
                    present.discard();
                }
                if (id != null && (present != null || loaded(level, home))) {
                    cottage.setResident(role, null);
                }
                continue;
            }
            if (role == Role.AGATHA && agathaAway(level.getServer())) {
                // One of each character: a player is Agatha, so the cottage's Agatha is out on the Road.
                if (present instanceof AsgardianEntity npc && npc.isAlive()) {
                    away(npc);
                    cottage.set(Cottages.AGATHA_AWAY, true);
                }
                if (id != null && (present != null || loaded(level, home))) {
                    cottage.setResident(role, null);
                }
                continue;
            }
            if (!loaded(level, home) || present instanceof AsgardianEntity alive && alive.isAlive()) {
                continue;
            }
            boolean back = role == Role.AGATHA && cottage.has(Cottages.AGATHA_AWAY);
            boolean first = !cottage.has(Cottages.made(role));
            if (!first && !back && !HydraGarrison.unseen(level, Vec3.atBottomCenterOf(home), NOT_WITHIN)) {
                continue;
            }
            AsgardianEntity npc = make(level, cottage, role);
            if (npc == null) {
                continue;
            }
            cottage.setResident(role, npc.getUUID());
            if (level.addFreshEntity(npc)) {
                made.add(npc);
                cottage.set(Cottages.made(role), true);
                if (back) {
                    cottage.set(Cottages.AGATHA_AWAY, false);
                    shimmer(npc);
                }
            } else {
                cottage.setResident(role, id);
            }
        }
        Entity rabbit = Scratchy.keep(level, cottage);
        if (rabbit != null) {
            made.add(rabbit);
        }
        registerDoor(level, cottage);
        Scratchy.morning(level, cottage, level.getDayTime(), level.getRandom());
        return made;
    }

    /** A new resident of {@code role} for the cottage (not yet in the level), where the hour says; null: no room. */
    @Nullable
    static AsgardianEntity make(ServerLevel level, Cottages.Cottage cottage, Role role) {
        BlockPos home = cottage.points().home(role);
        BlockPos stand = Asgardians.standOn(level, home);
        if (stand == null) {
            return null;
        }
        AsgardianEntity npc = ModEntities.COTTAGE_RESIDENT.get().create(level);
        if (npc == null) {
            return null;
        }
        npc.setRole(role, 0);
        // a long way round inside the house (the stairs, the doors): they plan further than the Asgardians
        net.minecraft.world.entity.ai.attributes.AttributeInstance range = npc.getAttribute(
                net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        if (range != null) {
            range.setBaseValue(48.0);
        }
        npc.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        npc.setHome(Cottages.slot(cottage.key(), role), home, HOME_RADIUS);
        if (CottageGoals.Phase.at(level.getDayTime()) == CottageGoals.Phase.NIGHT) {
            CottageGoals.settle(npc, cottage, level.getDayTime());
        }
        return npc;
    }

    /** The cottage's resident of {@code role}, if he is loaded and alive. */
    @Nullable
    public static AsgardianEntity resident(ServerLevel level, Cottages.Cottage cottage, Role role) {
        UUID id = cottage.resident(role);
        return id != null && level.getEntity(id) instanceof AsgardianEntity npc && npc.isAlive() ? npc : null;
    }

    /** Agatha leaves for the Road: a violet shimmer, and she is gone. */
    static void away(AsgardianEntity npc) {
        shimmer(npc);
        npc.discard();
    }

    /** The violet shimmer of Agatha leaving for the Road, or coming home from it. */
    static void shimmer(AsgardianEntity npc) {
        if (npc.level() instanceof ServerLevel level) {
            level.sendParticles(VIOLET, npc.getX(), npc.getY() + 1.0, npc.getZ(), 48, 0.4, 1.0, 0.4, 0.0);
            level.sendParticles(ParticleTypes.WITCH, npc.getX(), npc.getY() + 1.0, npc.getZ(), 16, 0.3, 0.8, 0.3, 0.05);
            level.playSound(null, npc.getX(), npc.getY(), npc.getZ(), ModSounds.AGATHA_AWAY.get(), SoundSource.NEUTRAL,
                    1.0F, 1.0F);
        }
    }

    /**
     * The cellar's door joins the Witches' Road the first time the cottage is kept (a door set by world generation is
     * not listed by itself); with {@code roadDoor} off it leaves the Road and is decor only.
     */
    static void registerDoor(ServerLevel level, Cottages.Cottage cottage) {
        BlockPos door = cottage.points().roadDoor();
        if (!level.isLoaded(door)) {
            return;
        }
        if (roadDoor()) {
            if (!cottage.has(Cottages.DOOR_REGISTERED) && WitchesRoad.isDoor(level.getBlockState(door))) {
                WitchesRoad.addDoor(level, door);
                cottage.set(Cottages.DOOR_REGISTERED, true);
            }
        } else if (cottage.has(Cottages.DOOR_REGISTERED)) {
            WitchesRoad.removeDoor(level, door);
            cottage.set(Cottages.DOOR_REGISTERED, false);
        }
    }

    /** Whether the area round a home is loaded with its entities (only then is a missing resident really missing). */
    static boolean loaded(ServerLevel level, BlockPos home) {
        ChunkPos center = new ChunkPos(home);
        for (int dx = -AREA; dx <= AREA; dx++) {
            for (int dz = -AREA; dz <= AREA; dz++) {
                if (!level.areEntitiesLoaded(ChunkPos.asLong(center.x + dx, center.z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---- the quick looks ----

    /** What is looked at every {@link #QUICK} ticks for a player near the cottage. */
    public static void quick(ServerLevel level, Cottages.Cottage cottage, ServerPlayer player) {
        Scratchy.quick(level, cottage);
        if (Powers.power(player) == Power.AGATHA && !greeted(player, cottage)) {
            AsgardianEntity nicky = resident(level, cottage, Role.NICKY);
            if (nicky != null && nicky.distanceTo(player) <= GREET_RANGE && CottageGoals.greetMom(nicky, player)) {
                GREETED.computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(cottage.key());
            }
        }
        if ((level.getGameTime() + player.getId()) % AMBIENCE_TICKS < QUICK
                && cottage.distance(player.position()) == 0.0
                && Math.abs(player.getY() - cottage.points().agathaHome().getY()) < 16) {
            player.playNotifySound(ModSounds.AMBIENT_COTTAGE.get(), SoundSource.AMBIENT, AMBIENCE_VOLUME, 1.0F);
        }
    }

    /** Whether Nicky has greeted this Agatha on her visit to the cottage. */
    public static boolean greeted(ServerPlayer player, Cottages.Cottage cottage) {
        Set<Long> keys = GREETED.get(player.getUUID());
        return keys != null && keys.contains(cottage.key());
    }

    /** A visit ends when she is far from the cottage, or no longer Agatha. */
    static void forgetVisits(ServerLevel level, ServerPlayer player) {
        Set<Long> keys = GREETED.get(player.getUUID());
        if (keys == null) {
            return;
        }
        boolean agatha = Powers.power(player) == Power.AGATHA;
        Cottages store = Cottages.get(level);
        keys.removeIf(key -> {
            Cottages.Cottage cottage = store.byKey(key);
            return !agatha || cottage == null || level.dimension() != Level.OVERWORLD
                    || cottage.distance(player.position()) > VISIT_RANGE;
        });
        if (keys.isEmpty()) {
            GREETED.remove(player.getUUID());
        }
    }

    /** Forgets the caches (the server stops) or a player's visits (he leaves). */
    static void clear() {
        CACHE.clear();
        GREETED.clear();
    }

    static void forget(UUID player) {
        GREETED.remove(player);
    }

    private CottageKeeper() {
    }
}

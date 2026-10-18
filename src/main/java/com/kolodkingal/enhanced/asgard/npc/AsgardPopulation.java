package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.config.AsgardConfig;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The population keeper of Asgard (3.5): every slot of {@link CityPlan#homes()} (one per named role, {@code
 * EINHERJAR#0..11}, {@code CITIZEN#0..13}) has exactly one Asgardian, and the stables two horses ({@code HORSE#0..1}).
 *
 * <ul>
 *   <li>Every 100 ticks, for each slot whose home area (2 chunks round it) is fully loaded: if no entity with the slot's
 *   recorded UUID is in the level, a new one is made at the home, its UUID recorded first ({@link AsgardState#population()}),
 *   then added. A slot whose home has no ground to stand on is skipped.</li>
 *   <li>An Asgardian of a slot joining the level whose UUID is not the recorded one is refused ({@link #refuses}): no
 *   duplicates, ever (one that walked off into an unloaded chunk and comes back after its replacement is gone).</li>
 *   <li>With {@code asgard.npcs} off the keeper removes its own and makes none.</li>
 *   <li>Odin's slot is made asleep in his chamber until Hela first falls.</li>
 * </ul>
 * The keeper runs by itself in Asgard on a real server; the GameTests drive it ({@link #tickNow}, {@link #keep}).
 */
public final class AsgardPopulation {
    /** How often the keeper looks (ticks). */
    public static final int PERIOD = 100;
    /** The radius (chunks) round a home that must be loaded before its Asgardian is made. */
    public static final int AREA = 2;
    /** The horses of the stables. */
    public static final int HORSES = 2;

    /** Where the keeper records the slots' Asgardians: Asgard's state, or a test's own table. */
    public interface Residents {
        @Nullable
        UUID resident(String slot);

        void setResident(String slot, @Nullable UUID id);
    }

    /** The tests' tables, per level (the level of a GameTest is the Overworld). */
    private static final Map<ServerLevel, Residents> TEST_RESIDENTS = new WeakHashMap<>();

    /** Runs the keeper once now, as it runs every 100 ticks (for the tests): the city's homes, Asgard's records. */
    public static void tickNow(ServerLevel level) {
        Residents residents = residents(level);
        if (residents == null) {
            return;
        }
        keep(level, CityPlan.homes(), residents);
        keepHorses(level, residents);
    }

    /** The records of the level: Asgard's state in Asgard, a test's table where a test set one, else null. */
    @Nullable
    public static Residents residents(ServerLevel level) {
        Residents test = TEST_RESIDENTS.get(level);
        if (test != null) {
            return test;
        }
        if (!AsgardDimension.is(level)) {
            return null;
        }
        AsgardState state = AsgardState.get(level.getServer());
        return new Residents() {
            @Override
            public UUID resident(String slot) {
                return state.resident(slot);
            }

            @Override
            public void setResident(String slot, @Nullable UUID id) {
                state.setResident(slot, id);
            }
        };
    }

    /** A GameTest's own records for a level (null: none again). */
    public static void useResidents(ServerLevel level, @Nullable Residents residents) {
        if (residents == null) {
            TEST_RESIDENTS.remove(level);
        } else {
            TEST_RESIDENTS.put(level, residents);
        }
    }

    /**
     * The keeper's work for some homes: each home whose area is loaded and whose Asgardian is missing gets a new one.
     * With {@code asgard.npcs} off, the keeper's own are removed instead. Returns the Asgardians made.
     */
    public static List<AsgardianEntity> keep(ServerLevel level, List<CityPlan.Home> homes, Residents residents) {
        List<AsgardianEntity> made = new ArrayList<>();
        if (!AsgardConfig.NPCS.get()) {
            removeAll(level, homes, residents);
            return made;
        }
        for (CityPlan.Home home : homes) {
            if (!loaded(level, home.pos())) {
                continue;
            }
            UUID id = residents.resident(home.slot());
            Entity present = id == null ? null : level.getEntity(id);
            if (present instanceof AsgardianEntity npc && npc.isAlive()) {
                continue;
            }
            AsgardianEntity npc = make(level, home);
            if (npc == null) {
                continue;
            }
            residents.setResident(home.slot(), npc.getUUID());
            if (level.addFreshEntity(npc)) {
                made.add(npc);
            } else {
                residents.setResident(home.slot(), id);
            }
        }
        return made;
    }

    /** A new Asgardian for a home (not yet in the level), or null if the home has nowhere to stand. */
    @Nullable
    static AsgardianEntity make(ServerLevel level, CityPlan.Home home) {
        BlockPos stand = Asgardians.standOn(level, home.pos());
        if (stand == null) {
            return null;
        }
        AsgardianEntity npc = ModEntities.ASGARDIAN.get().create(level);
        if (npc == null) {
            return null;
        }
        npc.setRole(home.role(), home.index());
        npc.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        npc.setHome(home.slot(), home.pos(), home.radius());
        if (home.role() == Role.ODIN && !Asgardians.odinAwake(level)) {
            npc.setState(NpcState.SLEEPING, 0);
            npc.arm();
        }
        return npc;
    }

    private static void removeAll(ServerLevel level, List<CityPlan.Home> homes, Residents residents) {
        for (CityPlan.Home home : homes) {
            UUID id = residents.resident(home.slot());
            if (id != null) {
                if (level.getEntity(id) instanceof AsgardianEntity npc) {
                    npc.discard();
                }
                if (loaded(level, home.pos())) {
                    residents.setResident(home.slot(), null);
                }
            }
        }
    }

    /** Whether the home area of a slot is loaded with its entities (only then can the keeper know who is missing). */
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

    /** The two horses of Valkyrie's stables. */
    static void keepHorses(ServerLevel level, Residents residents) {
        if (!AsgardDimension.is(level) || !AsgardConfig.NPCS.get() || !loaded(level, CityPlan.STABLES)) {
            return;
        }
        for (int i = 0; i < HORSES; i++) {
            String slot = "HORSE#" + i;
            UUID id = residents.resident(slot);
            if (id != null && level.getEntity(id) instanceof Horse horse && horse.isAlive()) {
                continue;
            }
            BlockPos stand = Asgardians.standOn(level, CityPlan.STABLES.offset(i == 0 ? -3 : 3, 0, 2));
            Horse horse = stand == null ? null : EntityType.HORSE.create(level);
            if (horse == null) {
                continue;
            }
            horse.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0F, 0.0F);
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), MobSpawnType.STRUCTURE, null);
            horse.setPersistenceRequired();
            horse.restrictTo(CityPlan.STABLES, 8);
            horse.getPersistentData().putString(SLOT_TAG, slot);
            residents.setResident(slot, horse.getUUID());
            level.addFreshEntity(horse);
        }
    }

    /** The tag a keeper's horse carries (its slot). */
    static final String SLOT_TAG = Enhanced.MODID + ":asgard_slot";

    /**
     * Whether a joining entity must be refused: an Asgardian (or a stables horse) of a slot whose recorded resident is
     * someone else. One of a slot nobody has is adopted (recorded).
     */
    public static boolean refuses(ServerLevel level, Entity entity) {
        String slot = entity instanceof AsgardianEntity npc && !npc.summoned() ? npc.slot()
                : entity instanceof Horse ? entity.getPersistentData().getString(SLOT_TAG) : "";
        if (slot.isEmpty()) {
            return false;
        }
        Residents residents = residents(level);
        if (residents == null) {
            return false;
        }
        if (!AsgardConfig.NPCS.get() && entity instanceof AsgardianEntity) {
            return true;
        }
        UUID recorded = residents.resident(slot);
        if (recorded == null) {
            residents.setResident(slot, entity.getUUID());
            return false;
        }
        return !recorded.equals(entity.getUUID());
    }

    private AsgardPopulation() {
    }
}

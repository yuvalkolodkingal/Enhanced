package com.kolodkingal.enhanced.asgard;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The state of Asgard itself (3.5), saved with Asgard's level ({@code asgard_state.dat}): the version of the city plan
 * its chunks were made with, whether Odin is awake (he sleeps the Odinsleep until Hela first falls), how often Hela has
 * been beaten, whether the murals of the throne room show her conquests (while she is here), the Asgardians the
 * population keeper made (slot to UUID) and when Hela's Seal can be broken again.
 *
 * <p>The skeleton writes the shell (the fields and their accessors); the lanes use them: L1 the plan version and the
 * murals, L3 Odin, L4 Hela's defeats and the seal, L5 the population; 3.6.1 the one player who took Gungnir. Every
 * setter marks the state dirty.
 */
public final class AsgardState extends SavedData {
    public static final String NAME = "asgard_state";

    private int planVersion;
    private boolean odinAwake;
    private int helaDefeats;
    private boolean muralsConquest;
    private final Map<String, UUID> population = new LinkedHashMap<>();
    private long sealReadyAt;
    /** 3.6.1: the one player who took Gungnir from its stand ({@code [gungnir] onePerWorld}), or null. */
    @Nullable
    private UUID gungnirOwner;
    /** Whether the Hall of Feasts of a city built before 3.7.2 has been mended ({@code FeastHall#mend}). */
    private boolean feastHallMended;

    public static SavedData.Factory<AsgardState> factory() {
        return new SavedData.Factory<>(AsgardState::new, AsgardState::load, null);
    }

    /**
     * The state of the server's Asgard. If the server has no Asgard (the data pack dimension is missing) this is a
     * detached default that is never saved: it reads as a sleeping Odin and an unbeaten Hela.
     */
    public static AsgardState get(MinecraftServer server) {
        ServerLevel asgard = AsgardDimension.level(server);
        return asgard == null ? new AsgardState() : asgard.getDataStorage().computeIfAbsent(factory(), NAME);
    }

    private static AsgardState load(CompoundTag tag, HolderLookup.Provider registries) {
        AsgardState state = new AsgardState();
        state.planVersion = tag.getInt("plan_version");
        state.odinAwake = tag.getBoolean("odin_awake");
        state.helaDefeats = tag.getInt("hela_defeats");
        state.muralsConquest = tag.getBoolean("murals_conquest");
        CompoundTag people = tag.getCompound("population");
        for (String slot : people.getAllKeys()) {
            if (people.hasUUID(slot)) {
                state.population.put(slot, people.getUUID(slot));
            }
        }
        state.sealReadyAt = tag.getLong("seal_ready_at");
        if (tag.hasUUID("gungnir_owner")) {
            state.gungnirOwner = tag.getUUID("gungnir_owner");
        }
        state.feastHallMended = tag.getBoolean("feast_hall_mended");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("plan_version", planVersion);
        tag.putBoolean("odin_awake", odinAwake);
        tag.putInt("hela_defeats", helaDefeats);
        tag.putBoolean("murals_conquest", muralsConquest);
        CompoundTag people = new CompoundTag();
        population.forEach(people::putUUID);
        tag.put("population", people);
        tag.putLong("seal_ready_at", sealReadyAt);
        if (gungnirOwner != null) {
            tag.putUUID("gungnir_owner", gungnirOwner);
        }
        tag.putBoolean("feast_hall_mended", feastHallMended);
        return tag;
    }

    /** The version of {@code CityPlan} the generated chunks were made with (0 before any). */
    public int planVersion() {
        return planVersion;
    }

    public void setPlanVersion(int version) {
        planVersion = version;
        setDirty();
    }

    /** Whether Odin has woken from the Odinsleep (false until Hela first falls). */
    public boolean odinAwake() {
        return odinAwake;
    }

    public void setOdinAwake(boolean awake) {
        odinAwake = awake;
        setDirty();
    }

    /** How many times Hela has been beaten. */
    public int helaDefeats() {
        return helaDefeats;
    }

    public void setHelaDefeats(int defeats) {
        helaDefeats = defeats;
        setDirty();
    }

    /** Whether the murals of the throne room show Hela's conquests (while she is here). */
    public boolean muralsConquest() {
        return muralsConquest;
    }

    public void setMuralsConquest(boolean conquest) {
        muralsConquest = conquest;
        setDirty();
    }

    /** The Asgardian the keeper made for a slot ({@code FRIGGA}, {@code EINHERJAR#3}...), or null. */
    @Nullable
    public UUID resident(String slot) {
        return population.get(slot);
    }

    /** Records (or, with null, forgets) the Asgardian of a slot. */
    public void setResident(String slot, @Nullable UUID id) {
        if (id == null) {
            population.remove(slot);
        } else {
            population.put(slot, id);
        }
        setDirty();
    }

    /** Every slot and its Asgardian (read only). */
    public Map<String, UUID> population() {
        return Collections.unmodifiableMap(population);
    }

    /** The game time from which Hela's Seal can be broken again. */
    public long sealReadyAt() {
        return sealReadyAt;
    }

    public void setSealReadyAt(long time) {
        sealReadyAt = time;
        setDirty();
    }

    /** The one player who took Gungnir from its stand (3.6.1, {@code [gungnir] onePerWorld}), or null. */
    @Nullable
    public UUID gungnirOwner() {
        return gungnirOwner;
    }

    /** Records (or, with null, forgets) the world's Gungnir owner. */
    public void setGungnirOwner(@Nullable UUID owner) {
        gungnirOwner = owner;
        setDirty();
    }

    /** Whether the Hall of Feasts has been looked over and mended (3.7.2; a city built before had a broken hall). */
    public boolean feastHallMended() {
        return feastHallMended;
    }

    public void setFeastHallMended(boolean mended) {
        feastHallMended = mended;
        setDirty();
    }
}

package com.kolodkingal.enhanced.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-player power state. Attached to every player, saved with the player, kept on death and synced to every
 * client that can see the player (other clients need it to draw suits, beams and shields).
 */
public final class PowerData {
    public static final int ABILITIES = 5;

    // ---- persisted ----
    private Power power = Power.NONE;
    /** Magneto: magnetic shield toggle. */
    private boolean magneticShield;
    /** Photon: spectrum vision toggle. */
    private boolean spectrumVision;
    /** Magneto: shards left from the last melted iron item. */
    private int ironCharge;
    /** Photon: absorbed energy (0-100). */
    private float energy;

    // ---- runtime (synced, not saved) ----
    private boolean beamActive;
    private boolean chaosShieldActive;
    private boolean shrapnelActive;
    private boolean flying;
    private int phaseTicks;
    /** Entity currently held with telekinesis, or -1. */
    private int heldEntityId = -1;
    /** Game time at which each ability (index 1-5) is ready again. */
    private final long[] cooldownUntil = new long[ABILITIES + 1];

    private boolean dirty;

    public static final Codec<PowerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Power.CODEC.optionalFieldOf("power", Power.NONE).forGetter(d -> d.power),
            Codec.BOOL.optionalFieldOf("magnetic_shield", false).forGetter(d -> d.magneticShield),
            Codec.BOOL.optionalFieldOf("spectrum_vision", false).forGetter(d -> d.spectrumVision),
            Codec.INT.optionalFieldOf("iron_charge", 0).forGetter(d -> d.ironCharge),
            Codec.FLOAT.optionalFieldOf("energy", 0F).forGetter(d -> d.energy)
    ).apply(i, (power, shield, vision, iron, energy) -> {
        PowerData d = new PowerData();
        d.power = power;
        d.magneticShield = shield;
        d.spectrumVision = vision;
        d.ironCharge = iron;
        d.energy = energy;
        return d;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, PowerData> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> d.write(buf), PowerData::read);

    private void write(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, power.ordinal());
        int flags = (magneticShield ? 1 : 0) | (spectrumVision ? 2 : 0) | (beamActive ? 4 : 0)
                | (chaosShieldActive ? 8 : 0) | (shrapnelActive ? 16 : 0) | (flying ? 32 : 0);
        ByteBufCodecs.VAR_INT.encode(buf, flags);
        ByteBufCodecs.VAR_INT.encode(buf, ironCharge);
        buf.writeFloat(energy);
        ByteBufCodecs.VAR_INT.encode(buf, phaseTicks);
        ByteBufCodecs.VAR_INT.encode(buf, heldEntityId + 1);
        for (int i = 1; i <= ABILITIES; i++) {
            ByteBufCodecs.VAR_LONG.encode(buf, cooldownUntil[i]);
        }
    }

    private static PowerData read(ByteBuf buf) {
        PowerData d = new PowerData();
        Power[] values = Power.values();
        int ordinal = ByteBufCodecs.VAR_INT.decode(buf);
        d.power = ordinal >= 0 && ordinal < values.length ? values[ordinal] : Power.NONE;
        int flags = ByteBufCodecs.VAR_INT.decode(buf);
        d.magneticShield = (flags & 1) != 0;
        d.spectrumVision = (flags & 2) != 0;
        d.beamActive = (flags & 4) != 0;
        d.chaosShieldActive = (flags & 8) != 0;
        d.shrapnelActive = (flags & 16) != 0;
        d.flying = (flags & 32) != 0;
        d.ironCharge = ByteBufCodecs.VAR_INT.decode(buf);
        d.energy = buf.readFloat();
        d.phaseTicks = ByteBufCodecs.VAR_INT.decode(buf);
        d.heldEntityId = ByteBufCodecs.VAR_INT.decode(buf) - 1;
        for (int i = 1; i <= ABILITIES; i++) {
            d.cooldownUntil[i] = ByteBufCodecs.VAR_LONG.decode(buf);
        }
        return d;
    }

    // ---- accessors ----

    public Power power() {
        return power;
    }

    /** Sets the power and resets everything that belonged to the old one. Use {@link Powers#setPower} from game code. */
    void setPowerRaw(Power power) {
        this.power = power;
        this.magneticShield = false;
        this.spectrumVision = false;
        this.ironCharge = 0;
        this.energy = 0;
        this.beamActive = false;
        this.chaosShieldActive = false;
        this.shrapnelActive = false;
        this.phaseTicks = 0;
        this.heldEntityId = -1;
        java.util.Arrays.fill(cooldownUntil, 0L);
        dirty = true;
    }

    public boolean magneticShield() {
        return magneticShield;
    }

    public void setMagneticShield(boolean v) {
        if (magneticShield != v) {
            magneticShield = v;
            dirty = true;
        }
    }

    public boolean spectrumVision() {
        return spectrumVision;
    }

    public void setSpectrumVision(boolean v) {
        if (spectrumVision != v) {
            spectrumVision = v;
            dirty = true;
        }
    }

    public int ironCharge() {
        return ironCharge;
    }

    public void setIronCharge(int v) {
        if (ironCharge != v) {
            ironCharge = v;
            dirty = true;
        }
    }

    public float energy() {
        return energy;
    }

    public void setEnergy(float v) {
        float clamped = Math.max(0F, Math.min(MAX_ENERGY, v));
        if (energy != clamped) {
            energy = clamped;
            dirty = true;
        }
    }

    public static final float MAX_ENERGY = 100F;

    public boolean beamActive() {
        return beamActive;
    }

    public void setBeamActive(boolean v) {
        if (beamActive != v) {
            beamActive = v;
            dirty = true;
        }
    }

    public boolean chaosShieldActive() {
        return chaosShieldActive;
    }

    public void setChaosShieldActive(boolean v) {
        if (chaosShieldActive != v) {
            chaosShieldActive = v;
            dirty = true;
        }
    }

    public boolean shrapnelActive() {
        return shrapnelActive;
    }

    public void setShrapnelActive(boolean v) {
        if (shrapnelActive != v) {
            shrapnelActive = v;
            dirty = true;
        }
    }

    public boolean flying() {
        return flying;
    }

    public void setFlying(boolean v) {
        if (flying != v) {
            flying = v;
            dirty = true;
        }
    }

    public int phaseTicks() {
        return phaseTicks;
    }

    public boolean isPhased() {
        return phaseTicks > 0;
    }

    public void setPhaseTicks(int v) {
        if (phaseTicks != v) {
            // Only sync the start and the end of a phase, not every tick of the countdown.
            if (phaseTicks == 0 || v == 0) {
                dirty = true;
            }
            phaseTicks = v;
        }
    }

    public int heldEntityId() {
        return heldEntityId;
    }

    public void setHeldEntityId(int v) {
        if (heldEntityId != v) {
            heldEntityId = v;
            dirty = true;
        }
    }

    public long cooldownUntil(int ability) {
        return cooldownUntil[ability];
    }

    public boolean isReady(int ability, long gameTime) {
        return gameTime >= cooldownUntil[ability];
    }

    public void startCooldown(int ability, long gameTime, int ticks) {
        cooldownUntil[ability] = gameTime + ticks;
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }
}

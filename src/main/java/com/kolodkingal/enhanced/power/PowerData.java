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
    /** Quicksilver: sprinting starts Quicksilver Time. */
    private boolean quicksilverTime = true;
    /** Quicksilver: run up walls and across water during Quicksilver Time. */
    private boolean wallRun = true;
    /** Quicksilver: his Walkman plays the theme during Quicksilver Time. */
    private boolean walkman = true;
    /** Vision: intangible, passing through walls (saved, so he can't log back in stuck inside a wall). */
    private boolean visionPhased;
    /** Vision: super-dense body. */
    private boolean dense;
    /** Vision: human disguise from WandaVision. */
    private boolean disguise;
    /** The hero's current look (see {@link Variants}); empty means the default one. */
    private String variant = "";
    /** On/off states of the newer heroes, one bit per {@link Toggle}. */
    private int toggles;
    /** Loki: the creature he has shapeshifted into (an entity type id), or empty. */
    private String form = "";

    // ---- runtime (synced, not saved) ----
    private boolean beamActive;
    private boolean chaosShieldActive;
    private boolean shrapnelActive;
    private boolean flying;
    /** Quicksilver Time is running right now: the world around him is slowed. */
    private boolean slowMo;
    private int phaseTicks;
    /** Entity currently held with telekinesis, or -1. */
    private int heldEntityId = -1;
    /** Thor: Mjolnir while it is thrown and flying, or -1. */
    private int hammerEntityId = -1;
    /** Momentary states, one bit per {@link Live}. */
    private int live;
    /** Game time at which each ability (index 1-5) is ready again. */
    private final long[] cooldownUntil = new long[ABILITIES + 1];

    private boolean dirty;

    public static final Codec<PowerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Power.CODEC.optionalFieldOf("power", Power.NONE).forGetter(d -> d.power),
            Codec.BOOL.optionalFieldOf("magnetic_shield", false).forGetter(d -> d.magneticShield),
            Codec.BOOL.optionalFieldOf("spectrum_vision", false).forGetter(d -> d.spectrumVision),
            Codec.INT.optionalFieldOf("iron_charge", 0).forGetter(d -> d.ironCharge),
            Codec.FLOAT.optionalFieldOf("energy", 0F).forGetter(d -> d.energy),
            Codec.BOOL.optionalFieldOf("quicksilver_time", true).forGetter(d -> d.quicksilverTime),
            Codec.BOOL.optionalFieldOf("wall_run", true).forGetter(d -> d.wallRun),
            Codec.BOOL.optionalFieldOf("walkman", true).forGetter(d -> d.walkman),
            Codec.BOOL.optionalFieldOf("vision_phased", false).forGetter(d -> d.visionPhased),
            Codec.BOOL.optionalFieldOf("dense", false).forGetter(d -> d.dense),
            Codec.BOOL.optionalFieldOf("disguise", false).forGetter(d -> d.disguise),
            Codec.STRING.optionalFieldOf("variant", "").forGetter(d -> d.variant),
            Codec.INT.optionalFieldOf("toggles", 0).forGetter(d -> d.toggles),
            Codec.STRING.optionalFieldOf("form", "").forGetter(d -> d.form)
    ).apply(i, (power, shield, vision, iron, energy, qsTime, wallRun, walkman, phased, dense, disguise, variant, toggles, form) -> {
        PowerData d = new PowerData();
        d.power = power;
        d.magneticShield = shield;
        d.spectrumVision = vision;
        d.ironCharge = iron;
        d.energy = energy;
        d.quicksilverTime = qsTime;
        d.wallRun = wallRun;
        d.walkman = walkman;
        d.visionPhased = phased;
        d.dense = dense;
        d.disguise = disguise;
        d.variant = variant;
        d.toggles = toggles;
        d.form = form;
        return d;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, PowerData> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> d.write(buf), PowerData::read);

    private void write(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, power.ordinal());
        int flags = (magneticShield ? 1 : 0) | (spectrumVision ? 2 : 0) | (beamActive ? 4 : 0)
                | (chaosShieldActive ? 8 : 0) | (shrapnelActive ? 16 : 0) | (flying ? 32 : 0)
                | (quicksilverTime ? 64 : 0) | (wallRun ? 128 : 0) | (walkman ? 256 : 0) | (slowMo ? 512 : 0)
                | (visionPhased ? 1024 : 0) | (dense ? 2048 : 0) | (disguise ? 4096 : 0);
        ByteBufCodecs.VAR_INT.encode(buf, flags);
        ByteBufCodecs.VAR_INT.encode(buf, ironCharge);
        buf.writeFloat(energy);
        ByteBufCodecs.VAR_INT.encode(buf, phaseTicks);
        ByteBufCodecs.VAR_INT.encode(buf, heldEntityId + 1);
        ByteBufCodecs.VAR_INT.encode(buf, hammerEntityId + 1);
        ByteBufCodecs.stringUtf8(64).encode(buf, variant);
        ByteBufCodecs.VAR_INT.encode(buf, toggles);
        ByteBufCodecs.VAR_INT.encode(buf, live);
        ByteBufCodecs.stringUtf8(256).encode(buf, form);
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
        d.quicksilverTime = (flags & 64) != 0;
        d.wallRun = (flags & 128) != 0;
        d.walkman = (flags & 256) != 0;
        d.slowMo = (flags & 512) != 0;
        d.visionPhased = (flags & 1024) != 0;
        d.dense = (flags & 2048) != 0;
        d.disguise = (flags & 4096) != 0;
        d.ironCharge = ByteBufCodecs.VAR_INT.decode(buf);
        d.energy = buf.readFloat();
        d.phaseTicks = ByteBufCodecs.VAR_INT.decode(buf);
        d.heldEntityId = ByteBufCodecs.VAR_INT.decode(buf) - 1;
        d.hammerEntityId = ByteBufCodecs.VAR_INT.decode(buf) - 1;
        d.variant = ByteBufCodecs.stringUtf8(64).decode(buf);
        d.toggles = ByteBufCodecs.VAR_INT.decode(buf);
        d.live = ByteBufCodecs.VAR_INT.decode(buf);
        d.form = ByteBufCodecs.stringUtf8(256).decode(buf);
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
        this.quicksilverTime = true;
        this.wallRun = true;
        this.walkman = true;
        this.slowMo = false;
        this.visionPhased = false;
        this.dense = false;
        this.disguise = false;
        this.variant = "";
        this.toggles = Toggle.defaults(power);
        this.live = 0;
        this.form = "";
        this.hammerEntityId = -1;
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

    public boolean quicksilverTime() {
        return quicksilverTime;
    }

    public void setQuicksilverTime(boolean v) {
        if (quicksilverTime != v) {
            quicksilverTime = v;
            dirty = true;
        }
    }

    public boolean wallRun() {
        return wallRun;
    }

    public void setWallRun(boolean v) {
        if (wallRun != v) {
            wallRun = v;
            dirty = true;
        }
    }

    public boolean walkman() {
        return walkman;
    }

    public void setWalkman(boolean v) {
        if (walkman != v) {
            walkman = v;
            dirty = true;
        }
    }

    public boolean slowMo() {
        return slowMo;
    }

    public void setSlowMo(boolean v) {
        if (slowMo != v) {
            slowMo = v;
            dirty = true;
        }
    }

    public boolean visionPhased() {
        return visionPhased;
    }

    public void setVisionPhased(boolean v) {
        if (visionPhased != v) {
            visionPhased = v;
            dirty = true;
        }
    }

    public boolean dense() {
        return dense;
    }

    public void setDense(boolean v) {
        if (dense != v) {
            dense = v;
            dirty = true;
        }
    }

    public boolean disguise() {
        return disguise;
    }

    public void setDisguise(boolean v) {
        if (disguise != v) {
            disguise = v;
            dirty = true;
        }
    }

    /** The id of the hero's current look; empty for the default. Use {@link Variants#current} for the look itself. */
    public String variant() {
        return variant;
    }

    /** Changes the look. Use {@link Powers#setVariant} from game code, which also ends whatever the old look was doing. */
    void setVariantRaw(String v) {
        if (!variant.equals(v)) {
            variant = v;
            dirty = true;
        }
    }

    public boolean toggle(Toggle toggle) {
        return (toggles & toggle.bit()) != 0;
    }

    public void setToggle(Toggle toggle, boolean on) {
        int updated = on ? toggles | toggle.bit() : toggles & ~toggle.bit();
        if (updated != toggles) {
            toggles = updated;
            dirty = true;
        }
    }

    public boolean is(Live state) {
        return (live & state.bit()) != 0;
    }

    public void set(Live state, boolean on) {
        int updated = on ? live | state.bit() : live & ~state.bit();
        if (updated != live) {
            live = updated;
            dirty = true;
        }
    }

    /** Loki's shapeshifted form (an entity type id, or another player: see {@link LokiForm}), or empty for his own body. */
    public String form() {
        return form;
    }

    public void setForm(String v) {
        if (!form.equals(v)) {
            form = v;
            dirty = true;
        }
    }

    public int hammerEntityId() {
        return hammerEntityId;
    }

    public void setHammerEntityId(int v) {
        if (hammerEntityId != v) {
            hammerEntityId = v;
            dirty = true;
        }
    }

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

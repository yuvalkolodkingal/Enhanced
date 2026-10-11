package com.kolodkingal.enhanced.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

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
    /** Magneto: shards left from the last melted iron item. */
    private int ironCharge;
    /** Quicksilver: Quicksilver Time is on (the world around him is slowed, whether he runs or not). */
    private boolean quicksilverTime;
    /** Quicksilver: Super Speed is on (only he is fast, the world isn't slowed); never with Quicksilver Time. */
    private boolean superSpeed;
    /** Quicksilver: run up walls and across water while he runs (in Quicksilver Time or Super Speed). */
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
    /** Scarlet Witch: B is held and the Chaos Shield is forming or up (see {@link #chaosShieldUp}). */
    private boolean chaosShieldActive;
    /** Chaos Shield: game time B was pressed, game time the shield is complete, and the strength it has left. */
    private long shieldStart;
    private long shieldFormedAt;
    private float shieldStrength;
    private boolean shrapnelActive;
    private boolean flying;
    /** Quicksilver Time is slowing the world around him right now (it is on and he is not a spectator). */
    private boolean slowMo;
    /** Entity currently held with telekinesis, or -1. */
    private int heldEntityId = -1;
    /** Thor: Mjolnir while it is thrown and flying, or -1. */
    private int hammerEntityId = -1;
    /** Momentary states, one bit per {@link Live}. */
    private long live;
    /** Game time at which each ability (index 1-5) is ready again. */
    private final long[] cooldownUntil = new long[ABILITIES + 1];

    private boolean dirty;

    public static final Codec<PowerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Power.CODEC.optionalFieldOf("power", Power.NONE).forGetter(d -> d.power),
            Codec.BOOL.optionalFieldOf("magnetic_shield", false).forGetter(d -> d.magneticShield),
            Codec.INT.optionalFieldOf("iron_charge", 0).forGetter(d -> d.ironCharge),
            // A new key: the old "quicksilver_time" meant "sprinting starts it" and was on for everyone.
            Codec.BOOL.optionalFieldOf("quicksilver_time_on", false).forGetter(d -> d.quicksilverTime),
            Codec.BOOL.optionalFieldOf("super_speed_on", false).forGetter(d -> d.superSpeed),
            Codec.BOOL.optionalFieldOf("wall_run", true).forGetter(d -> d.wallRun),
            Codec.BOOL.optionalFieldOf("walkman", true).forGetter(d -> d.walkman),
            Codec.BOOL.optionalFieldOf("vision_phased", false).forGetter(d -> d.visionPhased),
            Codec.BOOL.optionalFieldOf("dense", false).forGetter(d -> d.dense),
            Codec.BOOL.optionalFieldOf("disguise", false).forGetter(d -> d.disguise),
            Codec.STRING.optionalFieldOf("variant", "").forGetter(d -> d.variant),
            Codec.INT.optionalFieldOf("toggles", 0).forGetter(d -> d.toggles),
            Codec.STRING.optionalFieldOf("form", "").forGetter(d -> d.form)
    ).apply(i, (power, shield, iron, qsTime, superSpeed, wallRun, walkman, phased, dense, disguise, variant, toggles, form) -> {
        PowerData d = new PowerData();
        d.power = power;
        d.magneticShield = shield;
        d.ironCharge = iron;
        d.quicksilverTime = qsTime;
        d.superSpeed = superSpeed;
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
        int flags = (magneticShield ? 1 : 0) | (beamActive ? 2 : 0)
                | (chaosShieldActive ? 4 : 0) | (shrapnelActive ? 8 : 0) | (flying ? 16 : 0)
                | (quicksilverTime ? 32 : 0) | (wallRun ? 64 : 0) | (walkman ? 128 : 0) | (slowMo ? 256 : 0)
                | (visionPhased ? 512 : 0) | (dense ? 1024 : 0) | (disguise ? 2048 : 0) | (superSpeed ? 4096 : 0);
        ByteBufCodecs.VAR_INT.encode(buf, flags);
        if (chaosShieldActive) {
            ByteBufCodecs.VAR_LONG.encode(buf, shieldStart);
            ByteBufCodecs.VAR_LONG.encode(buf, shieldFormedAt);
            ByteBufCodecs.FLOAT.encode(buf, shieldStrength);
        }
        ByteBufCodecs.VAR_INT.encode(buf, ironCharge);
        ByteBufCodecs.VAR_INT.encode(buf, heldEntityId + 1);
        ByteBufCodecs.VAR_INT.encode(buf, hammerEntityId + 1);
        ByteBufCodecs.stringUtf8(64).encode(buf, variant);
        ByteBufCodecs.VAR_INT.encode(buf, toggles);
        ByteBufCodecs.VAR_LONG.encode(buf, live);
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
        d.beamActive = (flags & 2) != 0;
        d.chaosShieldActive = (flags & 4) != 0;
        d.shrapnelActive = (flags & 8) != 0;
        d.flying = (flags & 16) != 0;
        d.quicksilverTime = (flags & 32) != 0;
        d.wallRun = (flags & 64) != 0;
        d.walkman = (flags & 128) != 0;
        d.slowMo = (flags & 256) != 0;
        d.visionPhased = (flags & 512) != 0;
        d.dense = (flags & 1024) != 0;
        d.disguise = (flags & 2048) != 0;
        d.superSpeed = (flags & 4096) != 0;
        if (d.chaosShieldActive) {
            d.shieldStart = ByteBufCodecs.VAR_LONG.decode(buf);
            d.shieldFormedAt = ByteBufCodecs.VAR_LONG.decode(buf);
            d.shieldStrength = ByteBufCodecs.FLOAT.decode(buf);
        }
        d.ironCharge = ByteBufCodecs.VAR_INT.decode(buf);
        d.heldEntityId = ByteBufCodecs.VAR_INT.decode(buf) - 1;
        d.hammerEntityId = ByteBufCodecs.VAR_INT.decode(buf) - 1;
        d.variant = ByteBufCodecs.stringUtf8(64).decode(buf);
        d.toggles = ByteBufCodecs.VAR_INT.decode(buf);
        d.live = ByteBufCodecs.VAR_LONG.decode(buf);
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
        this.ironCharge = 0;
        this.quicksilverTime = false;
        this.superSpeed = false;
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
        this.shieldStart = 0L;
        this.shieldFormedAt = 0L;
        this.shieldStrength = 0F;
        this.shrapnelActive = false;
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

    public int ironCharge() {
        return ironCharge;
    }

    public void setIronCharge(int v) {
        if (ironCharge != v) {
            ironCharge = v;
            dirty = true;
        }
    }

    public boolean quicksilverTime() {
        return quicksilverTime;
    }

    public void setQuicksilverTime(boolean v) {
        if (quicksilverTime != v) {
            quicksilverTime = v;
            dirty = true;
        }
    }

    public boolean superSpeed() {
        return superSpeed;
    }

    public void setSuperSpeed(boolean v) {
        if (superSpeed != v) {
            superSpeed = v;
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

    /** Changes the look. Use {@link Powers#setVariant} from game code, which also sends the change to the player. */
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
        long updated = on ? live | state.bit() : live & ~state.bit();
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

    /**
     * Starts raising the Chaos Shield: it is complete {@code windUp} ticks after {@code now}, and takes
     * {@code strength} points of damage before it shatters.
     */
    public void raiseChaosShield(long now, int windUp, float strength) {
        chaosShieldActive = true;
        shieldStart = now;
        shieldFormedAt = now + windUp;
        shieldStrength = strength;
        dirty = true;
    }

    /** The Chaos Shield is held and complete at game time {@code now}: only then does it stop anything. */
    public boolean chaosShieldUp(long now) {
        return chaosShieldActive && now >= shieldFormedAt;
    }

    /** How far the Chaos Shield has formed at game time {@code now} (fractions of a tick allowed): 0 to 1. */
    public float chaosShieldProgress(float now) {
        long span = shieldFormedAt - shieldStart;
        return span <= 0 ? 1F : Mth.clamp((now - shieldStart) / span, 0F, 1F);
    }

    /** Game time at which the Chaos Shield started to form (its wind-up ran from here to {@link #shieldFormedAt}). */
    public long shieldStart() {
        return shieldStart;
    }

    /** Game time at which the Chaos Shield is, or will be, complete. */
    public long shieldFormedAt() {
        return shieldFormedAt;
    }

    /** What is left of the Chaos Shield's strength, in points. */
    public float shieldStrength() {
        return shieldStrength;
    }

    public void setShieldStrength(float v) {
        if (shieldStrength != v) {
            shieldStrength = v;
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

    /**
     * Makes every cooldown that is still running at {@code gameTime} end {@code ticks} sooner (but not before now), the
     * way Apocalypse's Power Bestowal makes the cooldowns of whoever it empowers recharge faster. Marks the data dirty
     * only if a cooldown moved, so a caller that calls it every second tick does not make the server sync idle data.
     */
    public void shiftCooldowns(long gameTime, long ticks) {
        if (ticks <= 0) {
            return;
        }
        for (int i = 1; i <= ABILITIES; i++) {
            if (cooldownUntil[i] > gameTime) {
                cooldownUntil[i] = Math.max(gameTime, cooldownUntil[i] - ticks);
                dirty = true;
            }
        }
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

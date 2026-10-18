package com.kolodkingal.enhanced.custom;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * What a custom hero is doing right now (3.3): which of its powers are switched on, the energy it has stored, its Force
 * Field. Every client that sees him needs it (to draw the field, the glow of his senses, his phasing), so it is synced
 * to them all; it is never saved (a hero logs in with everything off). The cooldowns are the power data's, one per key.
 */
public final class CustomState {
    /** The powers switched on, a bit per {@link PowerModule} (by ordinal: the state is never saved). */
    private int on;
    /** Absorption: the energy stored (points of damage soaked up). */
    private float charge;
    /** Force Field: up (the key held and the field not broken), when it was raised, and the strength it has left. */
    private boolean field;
    private long fieldSince;
    private float fieldStrength;

    private boolean dirty;

    public static final StreamCodec<ByteBuf, CustomState> STREAM_CODEC = StreamCodec.of((buf, s) -> {
        ByteBufCodecs.VAR_INT.encode(buf, s.on);
        ByteBufCodecs.FLOAT.encode(buf, s.charge);
        ByteBufCodecs.BOOL.encode(buf, s.field);
        ByteBufCodecs.VAR_LONG.encode(buf, s.fieldSince);
        ByteBufCodecs.FLOAT.encode(buf, s.fieldStrength);
    }, buf -> {
        CustomState s = new CustomState();
        s.on = ByteBufCodecs.VAR_INT.decode(buf);
        s.charge = ByteBufCodecs.FLOAT.decode(buf);
        s.field = ByteBufCodecs.BOOL.decode(buf);
        s.fieldSince = ByteBufCodecs.VAR_LONG.decode(buf);
        s.fieldStrength = ByteBufCodecs.FLOAT.decode(buf);
        return s;
    });

    public boolean isOn(PowerModule module) {
        return (on & (1 << module.ordinal())) != 0;
    }

    public void set(PowerModule module, boolean value) {
        int updated = value ? on | (1 << module.ordinal()) : on & ~(1 << module.ordinal());
        if (updated != on) {
            on = updated;
            dirty = true;
        }
    }

    /** Whether any power is switched on. */
    public boolean anyOn() {
        return on != 0;
    }

    public float charge() {
        return charge;
    }

    public void setCharge(float value) {
        if (charge != value) {
            charge = value;
            dirty = true;
        }
    }

    public boolean field() {
        return field;
    }

    public long fieldSince() {
        return fieldSince;
    }

    public float fieldStrength() {
        return fieldStrength;
    }

    /** Raises the field at {@code now} with {@code strength}. */
    public void raiseField(long now, float strength) {
        field = true;
        fieldSince = now;
        fieldStrength = strength;
        dirty = true;
    }

    public void dropField() {
        if (field) {
            field = false;
            dirty = true;
        }
    }

    public void setFieldStrength(float value) {
        if (fieldStrength != value) {
            fieldStrength = value;
            dirty = true;
        }
    }

    /** How far the field has grown at {@code time} (it takes {@link #FIELD_GROW_TICKS}): 0 to 1. */
    public float fieldGrowth(float time) {
        return Mth.clamp((time - fieldSince) / FIELD_GROW_TICKS, 0F, 1F);
    }

    /** Ticks a Force Field takes to grow to its full size (it stops everything from the first tick). */
    public static final float FIELD_GROW_TICKS = 5F;

    /** Everything off (the power was lost, the hero was replaced, the player died). */
    public void reset() {
        if (on != 0 || charge != 0 || field) {
            on = 0;
            charge = 0;
            field = false;
            fieldStrength = 0;
            dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }
}

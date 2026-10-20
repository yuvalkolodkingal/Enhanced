package com.kolodkingal.enhanced.ironman;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Iron Man's state (3.8, PLAN 5.4, 5.8): the attachment {@code ModAttachments.IRON_MAN}, synced to everyone who sees
 * him (the suit-up and a deployed nanotech suit are drawn from it, also by a player who starts tracking him mid-phase);
 * saved without the phase (a phase never survives a save: it completes at once), kept on death (the selected suits).
 * Immutable: every change makes a new record. Written by the 3.8 skeleton; lanes L3 and L4 drive it.
 *
 * @param suit        the suit he wears or has deployed ({@link SuitKind#asByte}, {@link SuitKind#NONE} for none)
 * @param energy      the suit energy, 0-{@code energyMax}
 * @param phase       {@link #NONE}, {@link #UP} or {@link #DOWN}
 * @param phaseSuit   the suit of the phase
 * @param phaseStart  the game time the phase began
 * @param phaseTicks  how long the phase lasts
 * @param nanoSuit    the selected nanotech suit (H deploys it)
 * @param lastSuit    the last suit he wore (H suits him up in it again)
 * @param flags       {@link #NANO_OUT}, {@link #CHARGED}, {@link #DRONES} and the lanes' other bits
 * @param dronesUntil the game time the Mark 85's drones go at
 */
public record IronManState(byte suit, float energy, byte phase, byte phaseSuit, long phaseStart, int phaseTicks,
                           byte nanoSuit, byte lastSuit, int flags, long dronesUntil) {
    public static final byte NONE = 0;
    public static final byte UP = 1;
    public static final byte DOWN = 2;
    /** A nanotech suit is deployed. */
    public static final int NANO_OUT = 1;
    /** Lightning struck him lately: the Lightning Refocuser is charged. */
    public static final int CHARGED = 2;
    /** The Mark 85's drones are out. */
    public static final int DRONES = 4;
    /**
     * (L3) The suit works: its eyes, reactor and thrusters are lit (clients read it; see {@code IronSuits.powered}).
     */
    public static final int POWERED = 8;
    /** (L3) The Mark 1 braces (B held): feet planted, arms crossed. */
    public static final int BRACING = 16;
    /** No suit, full energy, the Mark 85 selected. */
    public static final IronManState EMPTY = new IronManState(SuitKind.NONE, 100.0F, NONE, SuitKind.NONE, 0L, 0,
            SuitKind.MARK_85.asByte(), SuitKind.NONE, 0, 0L);

    /** Saved: everything but the phase (it completes at once on a save, a logout or a death). */
    public static final Codec<IronManState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BYTE.optionalFieldOf("suit", SuitKind.NONE).forGetter(IronManState::suit),
            Codec.FLOAT.optionalFieldOf("energy", 100.0F).forGetter(IronManState::energy),
            Codec.BYTE.optionalFieldOf("nano_suit", SuitKind.MARK_85.asByte()).forGetter(IronManState::nanoSuit),
            Codec.BYTE.optionalFieldOf("last_suit", SuitKind.NONE).forGetter(IronManState::lastSuit),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(IronManState::flags),
            Codec.LONG.optionalFieldOf("drones_until", 0L).forGetter(IronManState::dronesUntil)
    ).apply(i, (suit, energy, nano, last, flags, drones) -> new IronManState(suit, energy, NONE, SuitKind.NONE, 0L, 0,
            nano, last, flags, drones)));

    public static final StreamCodec<ByteBuf, IronManState> STREAM_CODEC = StreamCodec.of((buf, s) -> {
        buf.writeByte(s.suit);
        buf.writeFloat(s.energy);
        buf.writeByte(s.phase);
        buf.writeByte(s.phaseSuit);
        ByteBufCodecs.VAR_LONG.encode(buf, s.phaseStart);
        ByteBufCodecs.VAR_INT.encode(buf, s.phaseTicks);
        buf.writeByte(s.nanoSuit);
        buf.writeByte(s.lastSuit);
        ByteBufCodecs.VAR_INT.encode(buf, s.flags);
        ByteBufCodecs.VAR_LONG.encode(buf, s.dronesUntil);
    }, buf -> new IronManState(buf.readByte(), buf.readFloat(), buf.readByte(), buf.readByte(),
            ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), buf.readByte(),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf)));

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    /** The worn or deployed suit, or null. */
    @Nullable
    public SuitKind worn() {
        return SuitKind.of(suit);
    }

    /** The suit of the phase that plays, or null when none plays. */
    @Nullable
    public SuitKind phaseKind() {
        return phase == NONE ? null : SuitKind.of(phaseSuit);
    }

    public boolean inPhase() {
        return phase != NONE;
    }

    public IronManState withSuit(@Nullable SuitKind kind) {
        byte b = kind == null ? SuitKind.NONE : kind.asByte();
        return new IronManState(b, energy, phase, phaseSuit, phaseStart, phaseTicks, nanoSuit,
                kind == null ? lastSuit : b, flags, dronesUntil);
    }

    /** (L3) The suit H puts on next (the last worn, or the one Sneak + H picked). */
    public IronManState withLast(@Nullable SuitKind kind) {
        return new IronManState(suit, energy, phase, phaseSuit, phaseStart, phaseTicks, nanoSuit,
                kind == null ? SuitKind.NONE : kind.asByte(), flags, dronesUntil);
    }

    public IronManState withEnergy(float value) {
        return new IronManState(suit, value, phase, phaseSuit, phaseStart, phaseTicks, nanoSuit, lastSuit, flags,
                dronesUntil);
    }

    /** A phase begins ({@link #UP} or {@link #DOWN}), or ends ({@link #NONE}). */
    public IronManState withPhase(byte value, @Nullable SuitKind kind, long start, int ticks) {
        return new IronManState(suit, energy, value, kind == null ? SuitKind.NONE : kind.asByte(), start, ticks,
                nanoSuit, lastSuit, flags, dronesUntil);
    }

    public IronManState withNanoSuit(SuitKind kind) {
        return new IronManState(suit, energy, phase, phaseSuit, phaseStart, phaseTicks, kind.asByte(), lastSuit, flags,
                dronesUntil);
    }

    public IronManState with(int flag, boolean on) {
        return new IronManState(suit, energy, phase, phaseSuit, phaseStart, phaseTicks, nanoSuit, lastSuit,
                on ? flags | flag : flags & ~flag, dronesUntil);
    }

    public IronManState withDronesUntil(long time) {
        return new IronManState(suit, energy, phase, phaseSuit, phaseStart, phaseTicks, nanoSuit, lastSuit, flags,
                time);
    }
}

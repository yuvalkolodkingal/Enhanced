package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.build.BuildClient;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Arrays;

/**
 * Server → the caster: the server's verdict on a cast or a preflight ({@code BuildCastPayload} with the same
 * {@code token}), or on an undo or a redo. It says whether the spell is ready, done, or refused and why, how many blocks
 * it changes, what it costs Billy, and up to {@link #MAX_MARKS} cells that will be left alone, each with its kind, which
 * the ghost shows as small marks. At most 604 bytes (every number at its largest, 64 marks).
 *
 * <p>The codes are a list that is only ever appended to, never renumbered (D-B 13.1): from {@link #OFF} on, a code is a
 * refusal.
 *
 * @param token   the cast's token (0 for an undo or a redo)
 * @param code    what happened (one of the codes below)
 * @param changed how many blocks change (or changed)
 * @param cost    what it costs Billy (0 for the Scarlet Witch)
 * @param extra   what the message needs besides: seconds to wait, a count, a limit
 * @param flags   {@link #CUT_OFF}, {@link #PROTECTED}, {@link #KEEPS}, {@link #BODIES}, {@link #UNLOADED}
 * @param marks   the marked cells ({@link BlockPos#asLong()}), at most {@link #MAX_MARKS}
 * @param kinds   one kind for each mark ({@link #MARK_PROTECTED}...)
 */
public record BuildVerdictPayload(int token, int code, int changed, int cost, int extra, int flags, long[] marks, byte[] kinds)
        implements CustomPacketPayload {
    // The codes (appended only).
    public static final int READY = 0;
    public static final int READY_PARTLY = 1;
    public static final int DONE = 2;
    public static final int DONE_PARTLY = 3;
    public static final int UNDONE = 4;
    public static final int UNDONE_PARTLY = 5;
    public static final int REDONE = 6;
    public static final int STOPPED = 7;
    public static final int STOPPED_FIGHT = 8;
    public static final int DROPPED = 9;
    public static final int COPIED = 10;
    public static final int NOTHING_CHANGES = 11;
    // The refusals.
    public static final int OFF = 12;
    public static final int OPERATORS = 13;
    public static final int ADVENTURE = 14;
    public static final int BOUND = 15;
    public static final int DIMENSION = 16;
    public static final int NO_BLOCK = 17;
    public static final int BAD_BLOCK = 18;
    public static final int TWO_BLOCK = 19;
    public static final int VOLATILE = 20;
    public static final int VOLATILE_CAP = 21;
    public static final int NO_BOX = 22;
    public static final int LOOK_INSIDE = 23;
    public static final int TOO_FAR = 24;
    public static final int TOO_WIDE = 25;
    public static final int TOO_BIG = 26;
    public static final int REFILLING = 27;
    public static final int WAIT = 28;
    public static final int QUEUE_FULL = 29;
    public static final int SERVER_BUSY = 30;
    public static final int CEILING = 31;
    public static final int SOKOVIA = 32;
    public static final int MATERIALS = 33;
    public static final int HOLD = 34;
    public static final int NO_UNDO_ROOM = 35;
    public static final int POCKET_EMPTY = 36;
    public static final int POCKET_TOO_BIG = 37;
    public static final int TOO_BIG_TO_SELECT = 38;
    public static final int NOTHING_TO_UNDO = 39;
    public static final int NOTHING_TO_REDO = 40;
    /**
     * Every cell the plan would change is left as it is (an arena, spawn protection, a Hex, a chest with things, someone
     * standing there): the preflight says so with the marks, the ghost goes grey, and the cast fizzles with the line of
     * what was left (D-B 6.2).
     */
    public static final int NOTHING_CAN_CHANGE = 41;

    /** Only part of the plan was checked (it was too big, or the 2 ms ran out): the marks are a sample. */
    public static final int CUT_OFF = 1;
    /** Some cells are protected (an arena, spawn protection, a claim, a Hex). */
    public static final int PROTECTED = 2;
    /** Some containers stay because they hold things. */
    public static final int KEEPS = 4;
    /** Someone stands where blocks would go. */
    public static final int BODIES = 8;
    /** Some cells are in chunks that are not loaded. */
    public static final int UNLOADED = 16;

    // The kinds of mark (the colour the client draws it in).
    /** Protected (a red cross): an arena, spawn protection, a claim, a Hex. */
    public static final byte MARK_PROTECTED = 0;
    /** A container that stays because it holds things (yellow). */
    public static final byte MARK_KEEPS = 1;
    /** Someone stands there (white). */
    public static final byte MARK_BODY = 2;
    /** In a chunk that is not loaded (grey). */
    public static final byte MARK_UNLOADED = 3;
    /** Outside the build height or the world border (drawn as protected). */
    public static final byte MARK_OUTSIDE = 4;
    /** Beyond magic: an untouchable or unbreakable block (drawn as protected). */
    public static final byte MARK_BEYOND = 5;

    public static final int MAX_MARKS = 64;
    public static final long[] NO_MARKS = new long[0];
    public static final byte[] NO_KINDS = new byte[0];

    public static final Type<BuildVerdictPayload> TYPE = new Type<>(Enhanced.id("build_verdict"));

    public static final StreamCodec<ByteBuf, BuildVerdictPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.token);
                ByteBufCodecs.VAR_INT.encode(buf, p.code);
                ByteBufCodecs.VAR_INT.encode(buf, p.changed);
                ByteBufCodecs.VAR_INT.encode(buf, p.cost);
                ByteBufCodecs.VAR_INT.encode(buf, p.extra);
                buf.writeByte(p.flags);
                if (p.marks.length > MAX_MARKS || p.kinds.length > MAX_MARKS) {
                    throw new EncoderException("A verdict carries at most " + MAX_MARKS + " marks: " + p.marks.length);
                }
                ByteBufCodecs.VAR_INT.encode(buf, p.marks.length);
                for (long mark : p.marks) {
                    buf.writeLong(mark);
                }
                ByteBufCodecs.VAR_INT.encode(buf, p.kinds.length);
                buf.writeBytes(p.kinds);
            },
            buf -> {
                int token = ByteBufCodecs.VAR_INT.decode(buf);
                int code = ByteBufCodecs.VAR_INT.decode(buf);
                int changed = ByteBufCodecs.VAR_INT.decode(buf);
                int cost = ByteBufCodecs.VAR_INT.decode(buf);
                int extra = ByteBufCodecs.VAR_INT.decode(buf);
                int flags = buf.readUnsignedByte();
                long[] marks = new long[count(buf)];
                for (int i = 0; i < marks.length; i++) {
                    marks[i] = buf.readLong();
                }
                byte[] kinds = new byte[count(buf)];
                buf.readBytes(kinds);
                return new BuildVerdictPayload(token, code, changed, cost, extra, flags, marks, kinds);
            });

    /** A verdict without marks: a refusal, an undo, a done spell. */
    public BuildVerdictPayload(int token, int code, int changed, int cost, int extra, int flags) {
        this(token, code, changed, cost, extra, flags, NO_MARKS, NO_KINDS);
    }

    /** Whether {@code code} is a refusal (nothing happens). */
    public static boolean isRefusal(int code) {
        return code >= OFF;
    }

    /** The {@code i}th marked cell. */
    public BlockPos mark(int i) {
        return BlockPos.of(marks[i]);
    }

    /** The kind of the {@code i}th mark ({@link #MARK_PROTECTED} if the server sent fewer kinds than marks). */
    public byte kind(int i) {
        return i < kinds.length ? kinds[i] : MARK_PROTECTED;
    }

    private static int count(ByteBuf buf) {
        int count = ByteBufCodecs.VAR_INT.decode(buf);
        if (count < 0 || count > MAX_MARKS) {
            throw new DecoderException("A verdict carries at most " + MAX_MARKS + " marks: " + count);
        }
        return count;
    }

    // A record compares arrays by identity: these compare what they hold, so a verdict read back equals the one sent.
    @Override
    public boolean equals(Object other) {
        return other instanceof BuildVerdictPayload p && token == p.token && code == p.code && changed == p.changed && cost == p.cost
                && extra == p.extra && flags == p.flags && Arrays.equals(marks, p.marks) && Arrays.equals(kinds, p.kinds);
    }

    @Override
    public int hashCode() {
        int hash = Arrays.hashCode(new int[]{token, code, changed, cost, extra, flags});
        return 31 * (31 * hash + Arrays.hashCode(marks)) + Arrays.hashCode(kinds);
    }

    @Override
    public String toString() {
        return "BuildVerdictPayload[token=" + token + ", code=" + code + ", changed=" + changed + ", cost=" + cost + ", extra=" + extra
                + ", flags=" + flags + ", marks=" + Arrays.toString(marks) + ", kinds=" + Arrays.toString(kinds) + "]";
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The builder's client keeps it (the client class is loaded only when this runs, on a client). */
    static void handle(BuildVerdictPayload payload, IPayloadContext context) {
        BuildClient.onVerdict(payload);
    }
}

package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.build.BuildClient;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Server → its owner: one part of the copy in a builder's pocket, so that her client can draw the Copy's and the Move's
 * ghost. A pocket travels as its palette (the block states in it) and then its cells, as runs over y, z, x of
 * {@code VAR_INT index, VAR_INT length} (index 0: no block there, as outside a selected thing; index i:
 * {@code palette.get(i - 1)}). Both are split over as many parts as needed: the palette first, then the runs, each part
 * carrying at most {@link #PART_BYTES} of them together ({@link #split}), so every part stays under 31 KiB however many
 * kinds of block the pocket holds. The client puts the parts together again with {@link #join}.
 *
 * @param pocket      the copy's id ({@code BuildData.pocket})
 * @param part        which part this is, from 0
 * @param parts       how many there are
 * @param sx          the copy's size along x (part 0 only; 0 in the others)
 * @param sy          along y (part 0 only)
 * @param sz          along z (part 0 only)
 * @param paletteSize how many states the whole palette has (part 0 only)
 * @param palette     this part's slice of the palette
 * @param data        this part's slice of the runs
 */
public record BuildPocketPayload(int pocket, int part, int parts, int sx, int sy, int sz, int paletteSize, List<BlockState> palette,
                                 byte[] data) implements CustomPacketPayload {
    /** The most bytes of encoded palette and runs together in one part (30 KiB). */
    public static final int PART_BYTES = 30 * 1024;
    /** The most parts one pocket may take (the part numbers are shorts). */
    public static final int MAX_PARTS = Short.MAX_VALUE;

    public static final Type<BuildPocketPayload> TYPE = new Type<>(Enhanced.id("build_pocket"));

    private static final StreamCodec<ByteBuf, List<BlockState>> PALETTE =
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY).apply(ByteBufCodecs.list(PART_BYTES));

    public static final StreamCodec<ByteBuf, BuildPocketPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.pocket);
                buf.writeShort(p.part);
                buf.writeShort(p.parts);
                if (p.part == 0) {
                    ByteBufCodecs.VAR_INT.encode(buf, p.sx);
                    ByteBufCodecs.VAR_INT.encode(buf, p.sy);
                    ByteBufCodecs.VAR_INT.encode(buf, p.sz);
                    ByteBufCodecs.VAR_INT.encode(buf, p.paletteSize);
                }
                PALETTE.encode(buf, p.palette);
                FriendlyByteBuf.writeByteArray(buf, p.data);
            },
            buf -> {
                int pocket = ByteBufCodecs.VAR_INT.decode(buf);
                int part = buf.readShort();
                int parts = buf.readShort();
                int sx = 0;
                int sy = 0;
                int sz = 0;
                int paletteSize = 0;
                if (part == 0) {
                    sx = ByteBufCodecs.VAR_INT.decode(buf);
                    sy = ByteBufCodecs.VAR_INT.decode(buf);
                    sz = ByteBufCodecs.VAR_INT.decode(buf);
                    paletteSize = ByteBufCodecs.VAR_INT.decode(buf);
                }
                List<BlockState> palette = PALETTE.decode(buf);
                byte[] data = FriendlyByteBuf.readByteArray(buf, PART_BYTES);
                return new BuildPocketPayload(pocket, part, parts, sx, sy, sz, paletteSize, palette, data);
            });

    /** The sizes and the palette's length travel in part 0 only; every other part holds 0 for them. */
    public BuildPocketPayload {
        palette = List.copyOf(palette);
        if (part != 0) {
            sx = 0;
            sy = 0;
            sz = 0;
            paletteSize = 0;
        }
    }

    /**
     * A whole pocket in parts for its owner: the palette first, then the runs, each part at most {@link #PART_BYTES} of
     * them together (the two length prefixes counted). Always at least one part, even for an empty pocket.
     *
     * @throws IllegalArgumentException if it would take more than {@link #MAX_PARTS} parts (the server sends a pocket that
     *                                  big as its outer surface only, D-B 13.1)
     */
    public static List<BuildPocketPayload> split(int pocket, int sx, int sy, int sz, List<BlockState> palette, byte[] runs) {
        int room = PART_BYTES - 2 * VarInt.getByteSize(PART_BYTES);
        List<int[]> slices = new ArrayList<>();
        int p = 0;
        int d = 0;
        do {
            int left = room;
            int paletteFrom = p;
            while (p < palette.size()) {
                int bytes = VarInt.getByteSize(Block.getId(palette.get(p)));
                if (bytes > left) {
                    break;
                }
                left -= bytes;
                p++;
            }
            int dataFrom = d;
            if (p == palette.size()) {
                d += Math.min(left, runs.length - d);
            }
            slices.add(new int[]{paletteFrom, p, dataFrom, d});
        } while (p < palette.size() || d < runs.length);
        if (slices.size() > MAX_PARTS) {
            throw new IllegalArgumentException("A pocket of " + slices.size() + " parts is too big to send");
        }
        List<BuildPocketPayload> parts = new ArrayList<>(slices.size());
        for (int i = 0; i < slices.size(); i++) {
            int[] slice = slices.get(i);
            parts.add(new BuildPocketPayload(pocket, i, slices.size(), sx, sy, sz, palette.size(), palette.subList(slice[0], slice[1]),
                    Arrays.copyOfRange(runs, slice[2], slice[3])));
        }
        return parts;
    }

    /**
     * A whole pocket put together again.
     *
     * @param runs the runs of all its cells, over y, z, x ({@link #readRuns})
     */
    public record Whole(int pocket, int sx, int sy, int sz, List<BlockState> palette, byte[] runs) {
    }

    /**
     * The pocket the parts make together (in any order), or null while one is missing or they do not belong together
     * (another pocket's part, a part twice, a palette of the wrong length).
     */
    public static @Nullable Whole join(Collection<BuildPocketPayload> parts) {
        if (parts.isEmpty()) {
            return null;
        }
        BuildPocketPayload any = parts.iterator().next();
        BuildPocketPayload[] inOrder = new BuildPocketPayload[Math.max(0, any.parts)];
        for (BuildPocketPayload part : parts) {
            if (part.pocket != any.pocket || part.parts != inOrder.length || part.part < 0 || part.part >= inOrder.length
                    || inOrder[part.part] != null) {
                return null;
            }
            inOrder[part.part] = part;
        }
        List<BlockState> palette = new ArrayList<>();
        ByteArrayOutputStream runs = new ByteArrayOutputStream();
        for (BuildPocketPayload part : inOrder) {
            if (part == null) {
                return null;
            }
            palette.addAll(part.palette);
            runs.writeBytes(part.data);
        }
        BuildPocketPayload first = inOrder[0];
        if (palette.size() != first.paletteSize) {
            return null;
        }
        return new Whole(first.pocket, first.sx, first.sy, first.sz, List.copyOf(palette), runs.toByteArray());
    }

    /**
     * Appends one run to {@code out}: {@code length} cells of palette index {@code index} (0: no block). The runs of a
     * pocket are then {@code ByteBufUtil.getBytes(out)}.
     */
    public static void writeRun(ByteBuf out, int index, int length) {
        VarInt.write(out, index);
        VarInt.write(out, length);
    }

    /** What reads the runs: {@code length} cells from the cell number {@code first} (over y, z, x) have {@code index}. */
    @FunctionalInterface
    public interface RunReader {
        void run(long first, int length, int index);
    }

    /**
     * Reads {@code runs} in order, run by run, without making an array of every cell. Returns false (after the runs it
     * could read) if they are broken, name a palette index above {@code paletteSize}, or do not cover exactly
     * {@code cells} cells.
     */
    public static boolean readRuns(byte[] runs, long cells, int paletteSize, RunReader reader) {
        ByteBuf buf = Unpooled.wrappedBuffer(runs);
        long at = 0;
        try {
            while (buf.isReadable()) {
                int index = VarInt.read(buf);
                int length = VarInt.read(buf);
                if (index < 0 || index > paletteSize || length <= 0 || length > cells - at) {
                    return false;
                }
                reader.run(at, length, index);
                at += length;
            }
        } catch (RuntimeException broken) {
            return false;
        }
        return at == cells;
    }

    // A record compares arrays by identity: these compare what they hold, so a part read back equals the one sent.
    @Override
    public boolean equals(Object other) {
        return other instanceof BuildPocketPayload p && pocket == p.pocket && part == p.part && parts == p.parts && sx == p.sx
                && sy == p.sy && sz == p.sz && paletteSize == p.paletteSize && palette.equals(p.palette) && Arrays.equals(data, p.data);
    }

    @Override
    public int hashCode() {
        int hash = Arrays.hashCode(new int[]{pocket, part, parts, sx, sy, sz, paletteSize});
        return 31 * (31 * hash + palette.hashCode()) + Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return "BuildPocketPayload[pocket=" + pocket + ", part=" + part + "/" + parts + ", size=" + sx + "x" + sy + "x" + sz
                + ", palette=" + palette.size() + "/" + paletteSize + ", data=" + data.length + " bytes]";
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The owner's client gathers the parts (the client class is loaded only when this runs, on a client). */
    static void handle(BuildPocketPayload payload, PacketContext context) {
        BuildClient.onPocketPart(payload);
    }
}

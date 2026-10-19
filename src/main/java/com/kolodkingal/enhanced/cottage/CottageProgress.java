package com.kolodkingal.enhanced.cottage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's progress at Agatha's Cottages (3.7): the attachment {@code ModAttachments.COTTAGE} (saved, kept on death,
 * synced to him only). Immutable: every change makes a new record. Lane L6 drives it.
 *
 * @param flags           append-only bits (lane L6's: the Scratchy quest accepted...)
 * @param lastGiftDay     the in-game day Nicky last gave him a gift
 * @param lastBlessingDay the in-game day Rio last blessed him
 * @param scratchyRun     the run-off of Señor Scratchy he last brought home (its number), so the reward is once per run
 */
public record CottageProgress(int flags, long lastGiftDay, long lastBlessingDay, long scratchyRun) {
    public static final CottageProgress EMPTY = new CottageProgress(0, -1L, -1L, -1L);

    public static final Codec<CottageProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("flags", 0).forGetter(CottageProgress::flags),
            Codec.LONG.optionalFieldOf("last_gift_day", -1L).forGetter(CottageProgress::lastGiftDay),
            Codec.LONG.optionalFieldOf("last_blessing_day", -1L).forGetter(CottageProgress::lastBlessingDay),
            Codec.LONG.optionalFieldOf("scratchy_run", -1L).forGetter(CottageProgress::scratchyRun)
    ).apply(i, CottageProgress::new));

    public static final StreamCodec<ByteBuf, CottageProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CottageProgress::flags,
            ByteBufCodecs.VAR_LONG, CottageProgress::lastGiftDay,
            ByteBufCodecs.VAR_LONG, CottageProgress::lastBlessingDay,
            ByteBufCodecs.VAR_LONG, CottageProgress::scratchyRun,
            CottageProgress::new);

    public CottageProgress withFlags(int value) {
        return new CottageProgress(value, lastGiftDay, lastBlessingDay, scratchyRun);
    }

    public CottageProgress withLastGiftDay(long day) {
        return new CottageProgress(flags, day, lastBlessingDay, scratchyRun);
    }

    public CottageProgress withLastBlessingDay(long day) {
        return new CottageProgress(flags, lastGiftDay, day, scratchyRun);
    }

    public CottageProgress withScratchyRun(long run) {
        return new CottageProgress(flags, lastGiftDay, lastBlessingDay, run);
    }
}

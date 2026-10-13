package com.kolodkingal.enhanced.build;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * A builder's own Build Magic state (attachment {@code BUILD_DATA}): saved with the player, kept on death, and synced to
 * that player alone, as the Bifrost's places are (nobody else needs Billy's meter or a builder's history counts).
 *
 * <p>Saved: Billy's magic meter, as its value {@code meter} at the game time {@code meterAt} (it refills by itself, so
 * nothing is sent while it does: {@link #meterNow}), his {@code rank} and {@code practice}, and the pocket's id and size.
 * Not saved (what the HUD shows of the moment, set again by the server): whether a Scarlet Witch helps him, how many
 * spells can be undone and redone, the spell at work and the ones waiting, its progress, the pause after his last spell.
 * A player saved before 3.1 has none of it and reads as {@link #EMPTY}: Billy at rank 1 with a full meter.
 *
 * @param meter      Billy's meter at {@code meterAt}, in blocks of magic; {@link #FULL} or anything at or above the top
 *                   reads full
 * @param meterAt    the game time the meter had {@code meter}
 * @param rank       his rank, 1 (Apprentice) to 4 (Wiccan); it never goes down ({@link BillyRanks})
 * @param practice   the blocks of magic he has cast (an undo takes back those it restores)
 * @param pocket     the id of the copy in the pocket (0: the pocket is empty); a client that lacks it asks for it
 * @param pocketSize the copy's size on each axis
 * @param momNear    a Scarlet Witch is near enough to halve his spells' cost
 * @param undo       how many spells can be undone
 * @param redo       how many can be redone
 * @param working    a spell of this builder is at work
 * @param queued     how many more wait behind it
 * @param progress   the working spell's progress, in thousandths
 * @param pauseUntil the game time Billy may cast again after his last spell
 */
public record BuildData(double meter, long meterAt, int rank, long practice, int pocket, Vec3i pocketSize, boolean momNear,
                        int undo, int redo, boolean working, int queued, int progress, long pauseUntil) {
    /** A meter that reads full, whatever its top (bigger than any meter can be). */
    public static final double FULL = 1.0E9;
    /** Billy at rank 1 with a full meter, no pocket, nothing to undo: what a player who never built reads as. */
    public static final BuildData EMPTY = new BuildData(FULL, 0L, BillyRanks.MIN_RANK, 0L, 0, Vec3i.ZERO, false, 0, 0, false, 0, 0, 0L);

    /** Only the saved part: what the moment shows starts empty again after a load (and after a death). */
    public static final Codec<BuildData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("meter", FULL).forGetter(BuildData::meter),
            Codec.LONG.optionalFieldOf("meter_at", 0L).forGetter(BuildData::meterAt),
            Codec.INT.optionalFieldOf("rank", BillyRanks.MIN_RANK).forGetter(BuildData::rank),
            Codec.LONG.optionalFieldOf("practice", 0L).forGetter(BuildData::practice),
            Codec.INT.optionalFieldOf("pocket", 0).forGetter(BuildData::pocket),
            Vec3i.CODEC.optionalFieldOf("pocket_size", Vec3i.ZERO).forGetter(BuildData::pocketSize)
    ).apply(i, (meter, meterAt, rank, practice, pocket, pocketSize) ->
            new BuildData(meter, meterAt, rank, practice, pocket, pocketSize, false, 0, 0, false, 0, 0, 0L)));

    /** Everything, saved or not, to the player himself. */
    public static final StreamCodec<ByteBuf, BuildData> STREAM_CODEC = StreamCodec.of((buf, d) -> {
        ByteBufCodecs.DOUBLE.encode(buf, d.meter);
        ByteBufCodecs.VAR_LONG.encode(buf, d.meterAt);
        ByteBufCodecs.VAR_INT.encode(buf, d.rank);
        ByteBufCodecs.VAR_LONG.encode(buf, d.practice);
        ByteBufCodecs.VAR_INT.encode(buf, d.pocket);
        ByteBufCodecs.VAR_INT.encode(buf, d.pocketSize.getX());
        ByteBufCodecs.VAR_INT.encode(buf, d.pocketSize.getY());
        ByteBufCodecs.VAR_INT.encode(buf, d.pocketSize.getZ());
        ByteBufCodecs.BOOL.encode(buf, d.momNear);
        ByteBufCodecs.VAR_INT.encode(buf, d.undo);
        ByteBufCodecs.VAR_INT.encode(buf, d.redo);
        ByteBufCodecs.BOOL.encode(buf, d.working);
        ByteBufCodecs.VAR_INT.encode(buf, d.queued);
        ByteBufCodecs.VAR_INT.encode(buf, d.progress);
        ByteBufCodecs.VAR_LONG.encode(buf, d.pauseUntil);
    }, buf -> new BuildData(ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            new Vec3i(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)),
            ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf)));

    public BuildData {
        rank = Mth.clamp(rank, BillyRanks.MIN_RANK, BillyRanks.MAX_RANK);
        practice = Math.max(0L, practice);
        pocketSize = new Vec3i(pocketSize.getX(), pocketSize.getY(), pocketSize.getZ());
    }

    /**
     * The meter at game time {@code gameTime}: what it had at {@code meterAt}, refilled by {@code refillPerSecond} each
     * second since, never above {@code top}. The client draws it with this every frame, the server reads it with this
     * before a spell is paid.
     */
    public double meterNow(long gameTime, int top, double refillPerSecond) {
        if (meter >= top) {
            return top;
        }
        double refilled = meter + refillPerSecond * Math.max(0L, gameTime - meterAt) / 20.0;
        return Math.min(top, refilled);
    }

    /** The meter set to {@code meter} at game time {@code gameTime} (a spell paid, an undo refunded, a rank-up's gift). */
    public BuildData withMeter(double meter, long gameTime) {
        return new BuildData(meter, gameTime, rank, practice, pocket, pocketSize, momNear, undo, redo, working, queued, progress, pauseUntil);
    }

    public BuildData withRank(int rank) {
        return new BuildData(meter, meterAt, rank, practice, pocket, pocketSize, momNear, undo, redo, working, queued, progress, pauseUntil);
    }

    public BuildData withPractice(long practice) {
        return new BuildData(meter, meterAt, rank, practice, pocket, pocketSize, momNear, undo, redo, working, queued, progress, pauseUntil);
    }

    public BuildData withPocket(int pocket, Vec3i pocketSize) {
        return new BuildData(meter, meterAt, rank, practice, pocket, pocketSize, momNear, undo, redo, working, queued, progress, pauseUntil);
    }

    public BuildData withMomNear(boolean momNear) {
        return new BuildData(meter, meterAt, rank, practice, pocket, pocketSize, momNear, undo, redo, working, queued, progress, pauseUntil);
    }

    /** How many spells can be undone and redone now. */
    public BuildData withHistory(int undo, int redo) {
        return new BuildData(meter, meterAt, rank, practice, pocket, pocketSize, momNear, undo, redo, working, queued, progress, pauseUntil);
    }

    /** Whether a spell is at work, how many wait behind it, and how far it is (thousandths). */
    public BuildData withWork(boolean working, int queued, int progress) {
        return new BuildData(meter, meterAt, rank, practice, pocket, pocketSize, momNear, undo, redo, working, queued, progress, pauseUntil);
    }

    public BuildData withPauseUntil(long pauseUntil) {
        return new BuildData(meter, meterAt, rank, practice, pocket, pocketSize, momNear, undo, redo, working, queued, progress, pauseUntil);
    }
}

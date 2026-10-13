package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The skeleton of 3.1 (step S of the spec, 18.1): the shared pieces every lane builds on exist and answer as 3.0 did, or
 * as the spec fixes them. Each test that keeps state or changes a config value has a batch of its own ({@code v31_skeleton*}).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class V31SkeletonTests {

    // ------------------------------------------------------------------------------------------------------------
    // Billy's ranks (S8, B11)

    /** Every number of Billy's four ranks grows from the five of his first (D-B 8.2), and practice makes the rank. */
    @GameTest(template = ARENA, batch = "v31_skeleton_ranks")
    public static void billysRanksComeFromFiveNumbers(GameTestHelper helper) {
        int[] meter = {512, 1024, 2048, 4096};
        double[] refill = {16, 24, 32, 40};
        int[] side = {24, 32, 40, 48};
        int[] undos = {10, 12, 14, 16};
        int[] repeat = {8, 12, 16, 20};
        int[] brush = {3, 4, 5, 6};
        for (int rank = 1; rank <= 4; rank++) {
            int i = rank - 1;
            check(BillyRanks.meter(rank) == meter[i], "rank " + rank + ": a meter of " + BillyRanks.meter(rank));
            assertNear(BillyRanks.refill(rank), refill[i], 1.0E-9, "rank " + rank + ": the refill a second");
            check(BillyRanks.side(rank) == side[i] && BillyRanks.reach(rank) == side[i],
                    "rank " + rank + ": side " + BillyRanks.side(rank) + ", reach " + BillyRanks.reach(rank));
            check(BillyRanks.undoSteps(rank) == undos[i], "rank " + rank + ": " + BillyRanks.undoSteps(rank) + " undos");
            check(BillyRanks.repeatMax(rank) == repeat[i], "rank " + rank + ": " + BillyRanks.repeatMax(rank) + " copies");
            check(BillyRanks.brush(rank) == brush[i], "rank " + rank + ": a brush of " + BillyRanks.brush(rank));
            check(BillyRanks.magicSelect(rank) == meter[i], "rank " + rank + ": a double click selects as much as the meter holds");
            check(BillyRanks.rank(BuildData.EMPTY.withRank(rank)) == rank, "rank " + rank + " applies");
        }
        check(BillyRanks.rankFor(0) == 1 && BillyRanks.rankFor(1999) == 1 && BillyRanks.rankFor(2000) == 2 && BillyRanks.rankFor(7999) == 2
                && BillyRanks.rankFor(8000) == 3 && BillyRanks.rankFor(30000) == 4 && BillyRanks.rankFor(Long.MAX_VALUE) == 4,
                "practice makes the rank: 2,000, 8,000, 30,000");
        check(BillyRanks.nameKey(4).equals("hud.enhanced.build.rank.4"), "the fourth rank is Wiccan's");

        // The meter refills by game time, up to its top, and a player who never built has it full.
        check(BillyRanks.meterNow(BuildData.EMPTY, 0L) == 512 && BillyRanks.meterNow(BuildData.EMPTY, 1_000_000L) == 512,
                "a Billy who never built has a full meter");
        BuildData spent = BuildData.EMPTY.withMeter(100.0, 1000L);
        assertNear(BillyRanks.meterNow(spent, 1000L), 100.0, 1.0E-9, "the meter right after a spell");
        assertNear(BillyRanks.meterNow(spent, 1020L), 116.0, 1.0E-9, "one second later: 16 more");
        check(BillyRanks.meterNow(spent, 1000L + 20 * 60) == 512, "never above its top");
        assertNear(BillyRanks.meterNow(spent.withRank(2), 1020L), 124.0, 1.0E-9, "a Spellcaster refills 24 a second");
        helper.succeed();
    }

    /** With {@code build_magic_billy.maxRank} 1 he stays an Apprentice whatever he practises or learned. */
    @GameTest(template = ARENA, batch = "v31_skeleton_rank_cap")
    public static void billysRanksStopAtMaxRank(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(BuildConfig.BILLY_MAX_RANK, 1);
        check(BillyRanks.maxRank() == 1, "the cap is read live");
        check(BillyRanks.rankFor(0) == 1 && BillyRanks.rankFor(30000) == 1 && BillyRanks.rankFor(Long.MAX_VALUE) == 1,
                "rank 1 at any practice");
        BuildData wiccan = BuildData.EMPTY.withRank(4);
        check(BillyRanks.rank(wiccan) == 1 && BillyRanks.meterNow(wiccan, 0L) == 512,
                "a Wiccan under the cap casts as an Apprentice (and his rank is kept for when it is raised)");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The synced data (S9, B13)

    /**
     * {@code BUILD_DATA} syncs to its holder alone and {@code BUILD_VIEW} to whoever sees him too; both codecs give back
     * what they were given; only the meter, the rank, the practice and the pocket are saved; and a player saved before
     * 3.1 reads as {@link BuildData#EMPTY}: rank 1, a full meter.
     */
    @GameTest(template = ARENA, batch = "v31_skeleton_sync")
    public static void buildDataAndViewSyncAndOldWorldsLoad(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(9.5, FLOOR, 10.5, Power.BILLY);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);

        check(!billy.hasData(ModAttachments.BUILD_DATA) && billy.getData(ModAttachments.BUILD_DATA).equals(BuildData.EMPTY)
                && billy.getData(ModAttachments.BUILD_VIEW).equals(BuildView.EMPTY), "a player who never built reads as empty");
        check(BillyRanks.rank(BuildData.EMPTY) == 1 && BillyRanks.meterNow(BuildData.EMPTY, helper.getLevel().getGameTime()) == 512,
                "empty is rank 1 with a full meter");

        BuildData data = BuildData.EMPTY.withMeter(123.5, 4000L).withRank(3).withPractice(9000L).withPocket(17, new Vec3i(7, 5, 9))
                .withMomNear(true).withHistory(4, 1).withWork(true, 2, 640).withPauseUntil(4010L);
        check(roundTrip(BuildData.STREAM_CODEC, data).equals(data), "the data's stream codec");
        BuildView view = new BuildView(true, Spell.COPY, Optional.of(new BuildView.Box(new BlockPos(5, 1, 5), new BlockPos(-3, 9, 2))));
        check(view.box().get().min().equals(new BlockPos(-3, 1, 2)) && view.box().get().max().equals(new BlockPos(5, 9, 5)),
                "a box is kept by its low and high corners");
        check(roundTrip(BuildView.STREAM_CODEC, view).equals(view) && roundTrip(BuildView.STREAM_CODEC, BuildView.EMPTY).equals(BuildView.EMPTY),
                "the view's stream codec");
        BuildData saved = new BuildData(123.5, 4000L, 3, 9000L, 17, new Vec3i(7, 5, 9), false, 0, 0, false, 0, 0, 0L);
        Tag nbt = BuildData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        check(BuildData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow().equals(saved), "only the meter, rank, practice and pocket are saved");
        check(BuildData.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow().equals(BuildData.EMPTY), "nothing saved reads as empty");

        // A player saved with it, and the same player as 3.0 saved him (without it), loaded again.
        billy.setData(ModAttachments.BUILD_DATA, data);
        billy.setData(ModAttachments.BUILD_VIEW, view);
        CompoundTag withIt = billy.saveWithoutId(new CompoundTag());
        withIt.remove("UUID");
        check(withIt.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY).contains(Enhanced.id("build_data").toString())
                && !withIt.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY).contains(Enhanced.id("build_view").toString()),
                "the data is saved with the player, the view is not");
        CompoundTag before31 = withIt.copy();
        before31.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY).remove(Enhanced.id("build_data").toString());
        ServerPlayer reloaded = f.player(8.5, FLOOR, 8.5);
        reloaded.load(withIt);
        check(reloaded.getData(ModAttachments.BUILD_DATA).equals(saved), "the saved part comes back: " + reloaded.getData(ModAttachments.BUILD_DATA));
        check(reloaded.getData(ModAttachments.BUILD_VIEW).equals(BuildView.EMPTY), "build mode is never saved");
        ServerPlayer old = f.player(8.5, FLOOR, 12.5);
        old.load(before31);
        check(old.getData(ModAttachments.BUILD_DATA).equals(BuildData.EMPTY), "a player saved before 3.1 reads as empty: "
                + old.getData(ModAttachments.BUILD_DATA));

        // Who is sent what, once Wanda sees Billy. (The first sight of a player sends all his synced data, whoever it is
        // for: NeoForge's pairing does not ask the attachment; every change after it goes only where the attachment says.)
        see(helper.getLevel(), wanda, billy);
        TestKit.sentPackets(billy);
        TestKit.sentPackets(wanda);
        billy.setData(ModAttachments.BUILD_DATA, data.withHistory(5, 0));
        check(synced(TestKit.sentPackets(billy), ModAttachments.BUILD_DATA.get(), billy.getId()), "Billy is sent his own data");
        check(!synced(TestKit.sentPackets(wanda), ModAttachments.BUILD_DATA.get(), billy.getId()), "nobody else is");
        billy.setData(ModAttachments.BUILD_VIEW, view.withActive(false));
        check(synced(TestKit.sentPackets(billy), ModAttachments.BUILD_VIEW.get(), billy.getId()), "Billy is sent his view");
        check(synced(TestKit.sentPackets(wanda), ModAttachments.BUILD_VIEW.get(), billy.getId()), "and so is whoever sees him");
        helper.succeed();
    }

    /**
     * What {@code watcher}'s client would do to see {@code seen} (nothing ticks a test player's connection): stand where
     * it stands, take the chunks the server has for it, say each batch arrived, and look round.
     */
    private static void see(ServerLevel level, ServerPlayer watcher, ServerPlayer seen) {
        level.getChunkSource().move(watcher);
        long chunk = seen.chunkPosition().toLong();
        for (int batch = 0; batch < 100 && watcher.connection.chunkSender.isPending(chunk); batch++) {
            watcher.connection.chunkSender.sendNextChunks(watcher);
            watcher.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
        }
        level.getChunkSource().move(watcher);
        check(level.getChunkSource().chunkMap.getPlayersWatching(seen).contains(watcher), "the watcher sees the player");
    }

    /** Whether {@code packets} hold a sync of {@code type} for the entity {@code entityId}. */
    private static boolean synced(List<Object> packets, AttachmentType<?> type, int entityId) {
        List<Object> all = new ArrayList<>();
        for (Object packet : packets) {
            if (packet instanceof ClientboundBundlePacket bundle) {
                bundle.subPackets().forEach(all::add);
            } else {
                all.add(packet);
            }
        }
        return all.stream().anyMatch(packet -> packet instanceof ClientboundCustomPayloadPacket custom
                && custom.payload() instanceof SyncAttachmentsPayload sync && sync.types().contains(type)
                && sync.target() instanceof SyncAttachmentsPayload.EntityTarget(int id) && id == entityId);
    }

    private static <T> T roundTrip(StreamCodec<ByteBuf, T> codec, T value) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        T decoded = codec.decode(buf);
        check(buf.readableBytes() == 0, "the codec read everything it wrote (" + buf.readableBytes() + " bytes left)");
        return decoded;
    }
}

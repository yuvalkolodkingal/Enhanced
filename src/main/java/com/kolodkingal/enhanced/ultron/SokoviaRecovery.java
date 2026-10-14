package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.boss.BossGifts;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTicketTypes;
import com.kolodkingal.enhanced.ultron.SokoviaJournal.Header;
import com.kolodkingal.enhanced.ultron.SokoviaShape.Spec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What puts things right after a lift that did not finish: a crash, a kill, a power cut, or a world opened by a server
 * that had a floating island saved in it.
 *
 * <ul>
 *     <li><b>The island.</b> At the start of the server ({@link #recoverAll}) every note the lift left on disk (see
 *     {@link SokoviaJournal}) becomes a job that holds the chunks of the island loaded, waits for them, and clears
 *     every block inside the island's shape and the heights it spans (that air was empty when the lift began, and the
 *     arena was protected since), a slice a tick. Then the items that fell with it get a fresh lifetime, the Program
 *     that started the fight is owed back to whoever used it, and the note is deleted. A note that cannot be read is
 *     set aside and the world is not touched on its account. A note whose island was never placed (it was only about to
 *     be) clears nothing, which is the same job. A note of an island that had landed (kept until two saves hold the
 *     landing, R48) is recovered quietly: its column is looked over once more (normally there is nothing left to
 *     clear), nothing is owed, and nobody is moved (the descent set everyone down).</li>
 *     <li><b>The players.</b> Whoever the note lists is given a record to be set down on the ground when they next log
 *     in inside the box where the island was ({@link SokoviaRelocations}); a player who was saved while carried, with
 *     the lift's marks on them, gets their gravity back and, if they hang in the air, is set down on the ground.</li>
 * </ul>
 *
 * <p>The entities of the fight that were saved with the chunks need no help: the boss framework's orphan sweep
 * discards a boss, Sentry or key of a fight that is not running the moment it joins the world.
 */
public final class SokoviaRecovery {
    /** How far (blocks) a recovery clears and holds loaded beyond the island's rim. */
    private static final int CHUNK_MARGIN = 2;

    private static final List<Job> JOBS = new ArrayList<>();

    /** One note being recovered. */
    private static final class Job {
        final Header header;
        final ServerLevel level;
        final List<ChunkPos> tickets = new ArrayList<>();
        SokoviaJob clearing;
        int waited;

        Job(Header header, ServerLevel level) {
            this.header = header;
            this.level = level;
        }

        /** The box of the island's columns, from its lowest block to its highest. */
        BoundingBox box() {
            int extent = header.spec().extent();
            return new BoundingBox(header.centre().getX() - extent, header.minY(), header.centre().getZ() - extent,
                    header.centre().getX() + extent, header.maxY(), header.centre().getZ() + extent);
        }

        /** Whether the note is of an island that came down by its own descent (R48): its column should be empty. */
        boolean landed() {
            return SokoviaJournal.LANDED.equals(header.state());
        }

        /**
         * Holds the chunks and gives the players their records (but for a landed island: its descent set everyone
         * down already).
         */
        void start() {
            Spec spec = header.spec();
            int reach = spec.extent() + CHUNK_MARGIN;
            int minX = (header.centre().getX() - reach) >> 4;
            int maxX = (header.centre().getX() + reach) >> 4;
            int minZ = (header.centre().getZ() - reach) >> 4;
            int maxZ = (header.centre().getZ() + reach) >> 4;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    ChunkPos chunk = new ChunkPos(x, z);
                    level.getChunkSource().addRegionTicket(ModTicketTypes.ENHANCED_ULTRON, chunk, 2, header.fight(), true);
                    tickets.add(chunk);
                }
            }
            if (!landed()) {
                sendMembersHome(level.getServer(), header);
            }
        }

        /** One tick of work; true when the island is gone and the note is deleted. */
        boolean tick() {
            for (ChunkPos chunk : tickets) {
                if (!level.hasChunk(chunk.x, chunk.z)) {
                    waited++;
                    return false;
                }
            }
            if (clearing == null) {
                clearing = SokoviaJob.clear(level, header.spec(), header.centre(), header.minY(), header.maxY());
            }
            if (!clearing.step()) {
                return false;
            }
            finish();
            return true;
        }

        /**
         * The island is gone: what fell with it is looked after, the debt is paid, the note is deleted, the chunks let
         * go.
         */
        void finish() {
            AABB box = new AABB(header.centre().getX() - header.spec().extent() - 2.0, level.getMinBuildHeight(), header.centre().getZ() - header.spec().extent() - 2.0,
                    header.centre().getX() + header.spec().extent() + 3.0, level.getMaxBuildHeight(), header.centre().getZ() + header.spec().extent() + 3.0);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                item.setExtendedLifetime(); // (a drop kept for the fight would otherwise lie there for ever)
            }
            payBack(level.getServer(), header);
            SokoviaJournal.delete(level.getServer(), header.fight());
            release();
            int cleared = clearing == null ? 0 : clearing.edits();
            if (!landed()) {
                Enhanced.LOGGER.info("Ultron's floating island of fight {} was cleared", header.fight());
            } else if (cleared > 0) {
                Enhanced.LOGGER.info("Ultron's island of fight {} had landed; {} blocks left in its column in the sky were cleared",
                        header.fight(), cleared);
            } else {
                // The usual case: the island came down, and its note only waited for two saves (R48). Nothing to say.
                Enhanced.LOGGER.debug("Ultron's island of fight {} had landed; its column in the sky was empty", header.fight());
            }
        }

        void release() {
            for (ChunkPos chunk : tickets) {
                level.getChunkSource().removeRegionTicket(ModTicketTypes.ENHANCED_ULTRON, chunk, 2, header.fight(), true);
            }
            tickets.clear();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The start of the server

    /**
     * At the start of the server (the levels exist and nothing has ticked): every note on disk becomes a job. A note of
     * a dimension that is not there stays, for when it is.
     *
     * @return how many jobs began (a note of a fight that never lifted anything has no job: its Program is just given
     * back)
     */
    public static int recoverAll(MinecraftServer server) {
        int began = 0;
        for (Header header : SokoviaJournal.readAll(server)) {
            if (JOBS.stream().anyMatch(job -> job.header.fight().equals(header.fight()))) {
                continue;
            }
            if (header.isRitual()) {
                // A crash on the ground: no island was ever placed, so there is nothing to clear, but the Program is owed.
                payBack(server, header);
                SokoviaJournal.delete(server, header.fight());
                Enhanced.LOGGER.warn("Ultron's fight {} was cut short by a crash: the Program is given back", header.fight());
                continue;
            }
            ServerLevel level = server.getLevel(header.dimension());
            if (level == null) {
                Enhanced.LOGGER.error("The Sokovia journal of fight {} is for the dimension {}, which is not here", header.fight(), header.dimension().location());
                continue;
            }
            Job job = new Job(header, level);
            job.start();
            JOBS.add(job);
            began++;
            if (job.landed()) {
                // A note kept until two saves hold its landing (R48), found after a stop that was not clean: its column
                // is looked over once more, quietly (there is normally nothing left up there to clear).
                Enhanced.LOGGER.debug("Ultron's island of fight {} had landed: its column in the sky is looked over once more", header.fight());
            } else {
                Enhanced.LOGGER.warn("Ultron's island of fight {} was left in the sky ({}): clearing it", header.fight(), header.state());
            }
        }
        return began;
    }

    /** Gives the Program back to whoever the note says used it, at their next login (a fight that was cut short). */
    private static void payBack(MinecraftServer server, Header header) {
        if (header.refund() && header.summoner() != null) {
            BossGifts.add(server, header.summoner(), new ItemStack(ModItems.ULTRON_PROGRAM.get()));
        }
    }

    /** Works on the recoveries of this level, a slice a tick. */
    static void tick(ServerLevel level) {
        if (JOBS.isEmpty()) {
            return;
        }
        for (Job job : List.copyOf(JOBS)) {
            if (job.level == level && job.tick()) {
                JOBS.remove(job);
            }
        }
    }

    /**
     * Whether {@code box} meets an island a recovery of this level is still clearing (the columns of its shape's square,
     * from its lowest block to its highest): no Build Magic spell is cast there until it is gone (D-B 10.2 C9), so
     * nothing a spell builds is ever cleared with it, and nothing it clears is ever undone into the sky.
     */
    public static boolean coversAny(ServerLevel level, BoundingBox box) {
        for (Job job : JOBS) {
            if (job.level == level && job.header.maxY() >= job.header.minY() && job.box().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /** How many recoveries are still going. */
    public static int pending() {
        return JOBS.size();
    }

    /** The server is stopping: a recovery that is not finished is finished now. */
    public static void finishAllNow() {
        for (Job job : List.copyOf(JOBS)) {
            for (ChunkPos chunk : job.tickets) {
                job.level.getChunk(chunk.x, chunk.z);
            }
            if (job.clearing == null) {
                job.clearing = SokoviaJob.clear(job.level, job.header.spec(), job.header.centre(), job.header.minY(), job.header.maxY());
            }
            job.clearing.finishNow();
            job.finish();
        }
        JOBS.clear();
    }

    /** Forgets the recoveries (the server has stopped, or a test ends), letting the chunks go. */
    public static void clear() {
        for (Job job : JOBS) {
            job.release();
        }
        JOBS.clear();
    }

    /** Gives everyone the note lists a record to be set down on the ground if they log in up there. */
    private static void sendMembersHome(MinecraftServer server, Header header) {
        if (header.members().isEmpty()) {
            return;
        }
        Spec spec = header.spec();
        SokoviaRelocations.Entry entry = new SokoviaRelocations.Entry(header.centre().getX() + 0.5, header.centre().getZ() + 0.5, spec.extent() + 2.0,
                header.minY() - spec.height(), header.maxY() + 80.0, spec.height(),
                header.minY() + spec.skin() + spec.cone() - UltronBossConfig.MAX_RELIEF.get() - 3.0);
        SokoviaRelocations data = SokoviaRelocations.of(server);
        for (UUID member : header.members()) {
            data = data.with(member, entry);
        }
        SokoviaRelocations.store(server, data);
    }

    // ---------------------------------------------------------------------------------------------------------
    // The players

    /**
     * A player logs in. If the lift marked them (they were saved while it carried them) their gravity is given back;
     * if there is a record for them and they are where the island was, they are set down on the ground; one who was
     * marked and hangs in the air without a record is set down on the ground below. The record goes either way.
     */
    public static void onLogin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        boolean marked = SokoviaCarry.markOf(player) != null;
        if (marked && !SokoviaLift.isCarried(player)) {
            SokoviaCarry.release(player);
        } else if (Powers.get(player).is(Live.LIFTED) && !SokoviaLift.isCarried(player)) {
            Powers.get(player).set(Live.LIFTED, false);
        }
        SokoviaRelocations data = SokoviaRelocations.of(server);
        SokoviaRelocations.Entry entry = data.of(player.getUUID());
        if (entry != null) {
            SokoviaRelocations.store(server, data.without(player.getUUID()));
            if (entry.contains(player.position())) {
                setDown(player, entry);
                return;
            }
        }
        if (marked && hangsInTheAir(player)) {
            setDownOnTheSurface(player);
        }
    }

    /**
     * Sets a player down on the ground where a record says they were: {@code down} blocks below them if they were up in
     * the sky, or on the first ground below them if they were saved on the way up (in the air under the island).
     */
    public static void setDown(ServerPlayer player, SokoviaRelocations.Entry entry) {
        if (entry.inTheSky(player.position())) {
            setDown(player, entry.down());
        } else {
            setDownOnTheSurface(player);
        }
    }

    /**
     * Sets a player on the ground {@code down} blocks below where they are (the island's height): free of the blocks.
     */
    public static void setDown(ServerPlayer player, double down) {
        Vec3 target = new Vec3(player.getX(), player.getY() - down, player.getZ());
        Vec3 spot = SokoviaCarry.freeSpot(player, target);
        if (!SokoviaCarry.fits(player, spot) || !supported(player, spot)) {
            Vec3 ground = Teleports.groundAt(player, player.serverLevel(), player.getBlockX(), player.getBlockZ());
            if (ground == null) {
                setDownOnTheSurface(player);
                return;
            }
            spot = ground;
        }
        SokoviaCarry.teleport(player, spot);
        player.resetFallDistance();
        player.connection.aboveGroundTickCount = 0;
        player.displayClientMessage(Component.translatable("message.enhanced.ultron.lift.relocated").withStyle(ChatFormatting.YELLOW), true);
    }

    /**
     * Whether there is something to stand on right under the spot, within half a block, with the player's own collisions
     * (so that the spot is not a hole in the air).
     */
    static boolean supported(ServerPlayer player, Vec3 spot) {
        return !player.serverLevel().noCollision(player, player.getBoundingBox().move(spot.subtract(player.position())).move(0.0, -0.5, 0.0));
    }

    /** Sets a player on the first ground below them (they were saved hanging in the air). */
    private static void setDownOnTheSurface(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double y = UltronMotion.groundBelow(level, player.getX(), player.getY(), player.getZ());
        Vec3 spot = y <= level.getMinBuildHeight() ? Teleports.groundAt(player, level, player.getBlockX(), player.getBlockZ())
                : SokoviaCarry.freeSpot(player, new Vec3(player.getX(), y, player.getZ()));
        if (spot != null) {
            SokoviaCarry.teleport(player, spot);
            player.resetFallDistance();
            player.displayClientMessage(Component.translatable("message.enhanced.ultron.lift.relocated").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    /** Whether a player has nothing solid within three blocks under their feet (and cannot fly). */
    private static boolean hangsInTheAir(ServerPlayer player) {
        if (player.getAbilities().mayfly || player.isSpectator()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        for (int down = 1; down <= 3; down++) {
            BlockState state = level.getBlockState(player.blockPosition().below(down));
            if (!state.getCollisionShape(level, player.blockPosition().below(down)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lets go of every player of the level who is marked as carried by a lift that no longer carries them (a player who
     * died while carried, or whose lift ended in a way that skipped them): called every second while a lift runs.
     */
    static void sweepStale(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (SokoviaLift.isCarried(player)) {
                continue;
            }
            if (SokoviaCarry.markOf(player) != null) {
                SokoviaCarry.release(player);
            } else if (Powers.get(player).is(Live.LIFTED)) {
                Powers.get(player).set(Live.LIFTED, false);
            }
        }
    }

    private SokoviaRecovery() {
    }
}

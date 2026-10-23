package com.kolodkingal.enhanced.eternity;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.block.EternityAltarBlock;
import com.kolodkingal.enhanced.config.EternityConfig;
import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.network.EternityScreenPayload;
import com.kolodkingal.enhanced.network.EternityWishPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.SpaceRealms;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Eternity's realm, {@code enhanced:eternity} (3.8, PLAN 10.3-10.5): the way in from the shrine's altar, the way home,
 * and Eternity kept in it.
 *
 * <p>{@link #enter} saves where the traveller stood ({@code ModAttachments.ETERNITY}'s return point), takes him alone
 * to {@link #ARRIVAL} facing Eternity, and makes Eternity and the wishing stone if they are not there. {@code
 * screenDelayTicks} later Eternity speaks and the wish screen opens; the wishing stone opens it again. {@link #wish}
 * takes the screen's answer and checks every rule again ({@link Wishes}); {@code returnDelayTicks} after a wish he is
 * sent home ({@link #returnHome}: to the very spot and facing, or the nearest safe one within 8, or his respawn point).
 * A player who logs in inside the realm is sent home; one who dies there respawns as usual and his return point is
 * forgotten ({@link EternityEvents}).
 *
 * <p>Eternity is never saved with his chunk: he is made when a traveller arrives and none sits there, and goes when the
 * realm is empty (his chunk is held loaded only while someone is there), so there are never two.
 */
public final class EternityRealm {
    /** The realm's level. */
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Enhanced.id("eternity"));
    /** Where a traveller arrives, facing north toward Eternity. */
    public static final Vec3 ARRIVAL = new Vec3(0.5, 64.0, 24.5);
    /** Where Eternity sits, facing south. */
    public static final BlockPos ETERNITY = new BlockPos(0, 64, -48);
    /** The wishing stone (a second altar) two blocks north of the arrival. */
    public static final BlockPos WISHING_STONE = BlockPos.containing(ARRIVAL).north(2);
    /** The mirror's layer: the realm's one layer of {@code eternity_mirror}. */
    public static final int MIRROR_Y = 63;
    /** The arrival's facing: north, toward Eternity. */
    public static final float ARRIVAL_YAW = 180.0F;
    /** How far from the return point a safe spot is looked for when the spot itself is no longer safe. */
    public static final int SAFE_SEARCH = 8;

    /** A traveller in the realm: when the screen opens by itself (-1: done) and when he is sent home (-1: not yet). */
    private static final class Visit {
        long screenAt = -1;
        long returnAt = -1;
    }

    private static final Map<UUID, Visit> VISITS = new HashMap<>();

    /** Whether the player is in Eternity's realm. */
    public static boolean in(ServerPlayer player) {
        return player.level().dimension() == LEVEL;
    }

    /** The realm's level on this server, or null (a world without the data pack's dimension). */
    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(LEVEL);
    }

    public static EternityProgress progress(ServerPlayer player) {
        return ModAttachments.ETERNITY.get(player);
    }

    static void setProgress(ServerPlayer player, EternityProgress progress) {
        ModAttachments.ETERNITY.set(player, progress);
    }

    // ---- the way in ----

    /**
     * Takes the player to the realm from the altar at {@code altar} (his return point saved first). The altar has done
     * its checks (the key, the charge); this one only refuses a player already there, or a world without the realm.
     *
     * @return whether he was taken
     */
    public static boolean enter(ServerPlayer player, BlockPos altar) {
        if (in(player) || !player.isAlive() || player.isSpectator()) {
            return false;
        }
        ServerLevel realm = level(player.server);
        if (realm == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_world"), true);
            return false;
        }
        ServerLevel from = player.serverLevel();
        Vec3 origin = player.position();
        EternityProgress.ReturnPoint back = new EternityProgress.ReturnPoint(from.dimension(), origin, player.getYRot(),
                player.getXRot());
        setProgress(player, progress(player).withReturnTo(Optional.of(back)).withVisit());
        from.playSound(null, origin.x, origin.y, origin.z, ModSounds.ETERNITY_LEAVE.get(), SoundSource.PLAYERS, 1.0F,
                1.0F);
        Net.sendToPlayersNear(from, null, origin.x, origin.y, origin.z, 64,
                new EternityFxPayload(EternityFxPayload.ALTAR_GO, player.getId(), altar, 0));
        player.stopRiding();
        player.ejectPassengers();
        prepare(realm);
        player.teleportTo(realm, ARRIVAL.x, ARRIVAL.y, ARRIVAL.z, ARRIVAL_YAW, 0.0F);
        if (player.level() != realm) {
            setProgress(player, progress(player).withReturnTo(Optional.empty()));
            return false;
        }
        player.setYHeadRot(ARRIVAL_YAW);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        keepEternity(realm);
        Visit visit = new Visit();
        visit.screenAt = realm.getGameTime() + cfg(EternityConfig.SCREEN_DELAY_TICKS, 40);
        VISITS.put(player.getUUID(), visit);
        realm.playSound(null, ARRIVAL.x, ARRIVAL.y, ARRIVAL.z, ModSounds.ETERNITY_ARRIVE.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);
        Net.sendToPlayer(player,
                new EternityFxPayload(EternityFxPayload.ARRIVE, player.getId(), BlockPos.containing(ARRIVAL), 0));
        return true;
    }

    /** The realm's fixed things before a traveller lands: Eternity's chunk held loaded, the wishing stone. */
    static void prepare(ServerLevel realm) {
        ChunkPos chunk = new ChunkPos(ETERNITY);
        realm.setChunkForced(chunk.x, chunk.z, true);
        realm.getChunk(WISHING_STONE);
        BlockState stone = realm.getBlockState(WISHING_STONE);
        if (!stone.is(ModBlocks.ETERNITY_ALTAR.get())) {
            realm.setBlockAndUpdate(WISHING_STONE, ModBlocks.ETERNITY_ALTAR.get().defaultBlockState()
                    .setValue(EternityAltarBlock.FACING, Direction.SOUTH).setValue(EternityAltarBlock.GENERATED, true));
        }
    }

    /**
     * Eternity in the realm: the one sitting there, or a new one made now (at {@link #ETERNITY}, facing south). Never a
     * second: the realm's whole middle is searched first, and any extra one goes.
     */
    public static EternityEntity keepEternity(ServerLevel realm) {
        realm.getChunk(ETERNITY);
        List<EternityEntity> there = realm.getEntities(ModEntities.ETERNITY.get(), new AABB(ETERNITY).inflate(96.0),
                e -> e.isAlive());
        if (!there.isEmpty()) {
            for (int i = 1; i < there.size(); i++) {
                there.get(i).discard();
            }
            return there.get(0);
        }
        EternityEntity eternity = new EternityEntity(ModEntities.ETERNITY.get(), realm);
        eternity.moveTo(ETERNITY.getX() + 0.5, ETERNITY.getY(), ETERNITY.getZ() + 0.5, 0.0F, 0.0F);
        eternity.setYHeadRot(0.0F);
        realm.addFreshEntity(eternity);
        return eternity;
    }

    /** The realm is empty: Eternity goes, and his chunk is let go (he is made again for the next traveller). */
    static void sleep(ServerLevel realm) {
        for (EternityEntity e : realm.getEntities(ModEntities.ETERNITY.get(), new AABB(ETERNITY).inflate(96.0),
                e -> true)) {
            e.discard();
        }
        ChunkPos chunk = new ChunkPos(ETERNITY);
        realm.setChunkForced(chunk.x, chunk.z, false);
    }

    // ---- the screen ----

    /** Eternity speaks (his voice; his line is on the screen) and the wish screen opens on the player's screen. */
    public static void openScreen(ServerPlayer player) {
        if (!in(player)) {
            return;
        }
        Visit visit = VISITS.computeIfAbsent(player.getUUID(), id -> new Visit());
        visit.screenAt = -1;
        if (visit.returnAt >= 0) {
            return; // (a wish was granted: he is about to go home)
        }
        boolean may = Wishes.mayWish(player);
        // (His line is the screen's own: only his voice is heard.)
        voice(player);
        Net.sendToPlayer(player, new EternityScreenPayload(may, may && Wishes.loveOffered(),
                Wishes.locks(player)));
    }

    /** One of Eternity's lines on the player's screen (a subtitle under an empty title), with his deep voice. */
    static void say(ServerPlayer player, String key) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(key)
                .withStyle(s -> s.withColor(0xE8E0FF).withItalic(true))));
        voice(player);
    }

    /** Eternity's deep, wordless voice. */
    static void voice(ServerPlayer player) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.ETERNITY_VOICE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    // ---- the wish ----

    /**
     * The wish screen's answer ({@code EternityWishPayload}: an item, Love, or leave), every rule checked here, never
     * trusting the client: he must be in the realm, alive, with a wish left; the item must be one Eternity grants him
     * ({@link Wishes#refusal}).
     */
    public static void wish(ServerPlayer player, byte kind, ResourceLocation item) {
        if (!in(player) || !player.isAlive() || player.isSpectator()) {
            return;
        }
        if (kind == EternityWishPayload.LEAVE) {
            returnHome(player);
            return;
        }
        Visit visit = VISITS.computeIfAbsent(player.getUUID(), id -> new Visit());
        if (visit.returnAt >= 0) {
            return;
        }
        if (!Wishes.mayWish(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.eternity.had_wish")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        byte what;
        if (kind == EternityWishPayload.LOVE) {
            if (!Wishes.loveOffered()) {
                return;
            }
            Wishes.becomeLove(player);
            what = EternityProgress.LOVE;
        } else if (kind == EternityWishPayload.ITEM) {
            if (!Wishes.grant(player, item)) {
                return;
            }
            what = EternityProgress.ITEM;
        } else {
            return;
        }
        setProgress(player, progress(player).withWish(what));
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WISH_GRANTED.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        Net.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 64,
                new EternityFxPayload(EternityFxPayload.WISH_GRANTED, player.getId(), player.blockPosition(), what));
        say(player, "eternity.enhanced.line.done");
        award(player, "careful_what_you_wish_for");
        if (what == EternityProgress.LOVE) {
            award(player, "thunder_runs_in_the_family");
        }
        visit.screenAt = -1;
        visit.returnAt = level.getGameTime() + cfg(EternityConfig.RETURN_DELAY_TICKS, 60);
    }

    // ---- the way home ----

    /**
     * Sends him home to where he came from: the very spot and facing; if that world is gone or the spot is no longer
     * safe (inside blocks, over nothing), the nearest safe spot within {@link #SAFE_SEARCH}; else his respawn point.
     * His return point is forgotten.
     */
    public static void returnHome(ServerPlayer player) {
        VISITS.remove(player.getUUID());
        Optional<EternityProgress.ReturnPoint> point = progress(player).returnTo();
        setProgress(player, progress(player).withReturnTo(Optional.empty()));
        if (in(player)) {
            ServerLevel from = player.serverLevel();
            from.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ETERNITY_LEAVE.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            Net.sendToPlayer(player,
                    new EternityFxPayload(EternityFxPayload.LEAVE, player.getId(), player.blockPosition(), 0));
        }
        player.stopRiding();
        ServerLevel home = point.map(p -> player.server.getLevel(p.dimension())).orElse(null);
        if (home != null) {
            EternityProgress.ReturnPoint p = point.get();
            Vec3 spot = safeSpot(home, player, p.pos());
            if (spot != null) {
                player.teleportTo(home, spot.x, spot.y, spot.z, p.yRot(), p.xRot());
                player.setYHeadRot(p.yRot());
                landed(player);
                return;
            }
        }
        DimensionTransition spawn = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        Vec3 at = spawn.pos();
        player.teleportTo(spawn.newLevel(), at.x, at.y, at.z, spawn.yRot(), spawn.xRot());
        landed(player);
    }

    private static void landed(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ETERNITY_ARRIVE.get(),
                SoundSource.PLAYERS, 0.7F, 1.3F);
    }

    /**
     * Where he may stand: {@code at} itself if it is free and has something under it (not the void), else the nearest
     * safe spot within {@link #SAFE_SEARCH} (rings nearest first, each column from the same height outward), else null.
     */
    @Nullable
    static Vec3 safeSpot(ServerLevel level, ServerPlayer player, Vec3 at) {
        level.getChunk(BlockPos.containing(at));
        if (standable(level, player, at)) {
            return at;
        }
        BlockPos centre = BlockPos.containing(at);
        int[] heights = new int[2 * SAFE_SEARCH + 1];
        for (int i = 0; i < heights.length; i++) {
            heights[i] = (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2);
        }
        for (int ring = 0; ring <= SAFE_SEARCH; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    for (int dy : heights) {
                        BlockPos feet = centre.offset(dx, dy, dz);
                        if (!level.isOutsideBuildHeight(feet) && SpaceRealms.safe(level, feet)
                                && fits(level, player, Vec3.atBottomCenterOf(feet))) {
                            return Vec3.atBottomCenterOf(feet);
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Free of blocks at {@code at}, inside the border, with ground (or water) somewhere below, not the void. */
    static boolean standable(ServerLevel level, ServerPlayer player, Vec3 at) {
        if (!fits(level, player, at)) {
            return false;
        }
        BlockPos.MutableBlockPos below = BlockPos.containing(at).mutable();
        for (int i = 0; i < 64 && below.getY() > level.getMinBuildHeight(); i++) {
            below.move(Direction.DOWN);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                    || !level.getFluidState(below).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean fits(ServerLevel level, ServerPlayer player, Vec3 at) {
        return level.getWorldBorder().isWithinBounds(BlockPos.containing(at))
                && level.noCollision(player, player.getDimensions(Pose.STANDING).makeBoundingBox(at));
    }

    // ---- the realm's clock ----

    /** Every server tick of a player in the realm: the screen opens by itself, the wished one goes home. */
    static void tick(ServerPlayer player) {
        Visit visit = VISITS.get(player.getUUID());
        if (visit == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (visit.returnAt >= 0 && now >= visit.returnAt) {
            returnHome(player);
            return;
        }
        if (visit.screenAt >= 0 && now >= visit.screenAt) {
            openScreen(player);
        }
    }

    /** Every 20 ticks of the server: Eternity is there while someone is, gone when nobody is. */
    static void keep(MinecraftServer server) {
        ServerLevel realm = level(server);
        if (realm == null) {
            return;
        }
        if (realm.players().isEmpty()) {
            if (realm.getForcedChunks().contains(new ChunkPos(ETERNITY).toLong())) {
                sleep(realm);
            }
        } else {
            prepare(realm);
            keepEternity(realm);
        }
    }

    /** Whether the player is waiting to be sent home after his wish (the tests ask). */
    public static boolean goingHome(ServerPlayer player) {
        Visit visit = VISITS.get(player.getUUID());
        return visit != null && visit.returnAt >= 0;
    }

    /** The player left the realm by any way: his visit is over. */
    static void left(ServerPlayer player) {
        VISITS.remove(player.getUUID());
    }

    public static void clear() {
        VISITS.clear();
    }

    // ---- helpers ----

    static void award(ServerPlayer player, String name) {
        AdvancementHolder advancement = player.server.getAdvancements().get(Enhanced.id("avengers/" + name));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "granted");
        }
    }

    static int cfg(ModConfigSpec.IntValue value, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() && value != null ? value.get() : fallback;
    }

    static boolean cfg(ModConfigSpec.BooleanValue value, boolean fallback) {
        return EnhancedConfig.SPEC.isLoaded() && value != null ? value.get() : fallback;
    }

    private EternityRealm() {
    }
}

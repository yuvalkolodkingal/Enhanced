package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.item.TesseractItem;
import com.kolodkingal.enhanced.network.RealmScreenPayload;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosEffects;
import com.kolodkingal.enhanced.thanos.ThanosSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Space Stone (3.6). Use, the <b>Portal Step</b>: a blue portal and he steps to the block he looks at (or right
 * behind the creature), up to {@code [space_stone] stepRange} (Thanos's step; a Hex wall, an arena or Mjolnir's pin
 * refuses). Sneak + use, the <b>Realm Picker</b> (and the Tesseract's Sneak + use): a screen with four realms, the
 * Overworld (his respawn point there, else the world's spawn), the Nether (x/8, z/8 of where he is, on safe ground),
 * the End (the main island, outside the exit portal) and Asgard (Heimdall's Observatory: the one way into Asgard but
 * the Bifrost, through {@link SpaceStone#moving}, unless {@code reachesAsgard} is off or Asgard is closed). The server
 * sends the realms it allows with the reason for the others ({@link RealmScreenPayload}) and checks again when one is
 * chosen ({@link #choose}). Refused in a boss arena, on the Witches' Road, in a Hex that holds him and while pinned.
 * He goes alone, lands with two seconds of Resistance, and the stone rests {@code realmCooldown}. Server only.
 */
public final class SpaceRealms {
    /** The realms of the picker, in the screen's order. */
    public static final List<String> REALMS = List.of("overworld", "the_nether", "the_end", "asgard");
    public static final int OVERWORLD = 0;
    public static final int NETHER = 1;
    public static final int END = 2;
    public static final int ASGARD = 3;

    /** How long a picker the server opened waits for its answer (ticks). */
    static final int PICKER_TICKS = 1200;
    /** Hunger of a step, and of a journey to another realm. */
    static final float STEP_EXHAUSTION = 0.5F;
    static final float REALM_EXHAUSTION = 2.0F;
    /** How far round the Overworld's spawn and the End's island a safe spot is looked for. */
    static final int OVERWORLD_SEARCH = 24;
    static final int END_SEARCH = 16;
    /** The Nether's safe floors lie between these heights. */
    static final int NETHER_MIN_Y = 32;
    static final int NETHER_MAX_Y = 110;

    /** A picker the server opened: the use that opened it, and until when it may be answered. */
    private record Pending(StoneUse.Cast cast, long until) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** Player -> where he last stood in the Nether (the way back there from the End or Asgard). */
    private static final Map<UUID, BlockPos> LAST_NETHER = new HashMap<>();

    // ---- the Portal Step ----

    static void step(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = player.serverLevel();
        double range = StonesConfig.SPACE_STEP_RANGE.get();
        HitResult hit = Raycasts.any(player, range, e -> {
            LivingEntity creature = StoneAim.creatureOf(e);
            return creature != null && creature != player && creature.isAlive() && !creature.isSpectator();
        }, 1.0F);
        Vec3 spot;
        if (hit instanceof EntityHitResult entityHit && StoneAim.creatureOf(entityHit.getEntity()) != null) {
            spot = Teleports.behind(player, StoneAim.creatureOf(entityHit.getEntity()));
        } else if (hit instanceof BlockHitResult blockHit && blockHit.getType() != HitResult.Type.MISS) {
            spot = Teleports.onto(player, blockHit);
        } else {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.no_block", (int) range), true);
            return;
        }
        if (spot == null || level.containsAnyLiquid(player.getBoundingBox().move(spot.subtract(player.position())))) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.no_space"), true);
            return;
        }
        if (!HexManager.onTeleport(level, player, spot)) {
            return;
        }
        cast.startCooldown(StonesConfig.SPACE_STEP_COOLDOWN.get());
        player.causeFoodExhaustion(STEP_EXHAUSTION);
        Vec3 from = player.position();
        ThanosEffects.portal(level, from.add(0.0, 0.2, 0.0), 1.4F, 40, StoneKind.SPACE.color());
        ThanosEffects.portal(level, spot.add(0.0, 0.2, 0.0), 1.4F, 40, StoneKind.SPACE.color());
        level.playSound(null, from.x, from.y, from.z, ModSounds.SPACE_STEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        StoneKind.SPACE.voice().use(level, from, 0.6F, 1.0F);
        player.stopRiding();
        player.teleportTo(level, spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        player.setYHeadRot(player.getYRot());
        player.resetFallDistance();
        ThanosSounds.PORTAL.play(level, spot, 0.8F, 1.5F);
    }

    // ---- the Realm Picker ----

    /** Opens the picker on his screen, with the realms he may go to now and why not the others. */
    static void open(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        Component refusal = refusalHere(player);
        if (refusal != null) {
            player.displayClientMessage(refusal, true);
            return;
        }
        PENDING.put(player.getUUID(), new Pending(cast, cast.now() + PICKER_TICKS));
        StoneKind.SPACE.voice().use(player.serverLevel(), player.position(), 0.5F, 1.2F);
        Net.sendToPlayer(player, new RealmScreenPayload(options(player)));
    }

    /** Whether a picker is open for the player (the tests ask). */
    public static boolean pickerOpen(Player player) {
        return PENDING.containsKey(player.getUUID());
    }

    /** The realms for the screen: every one, allowed or with the lang key of why not. */
    public static List<RealmScreenPayload.RealmOption> options(ServerPlayer player) {
        List<RealmScreenPayload.RealmOption> options = new ArrayList<>();
        for (int i = 0; i < REALMS.size(); i++) {
            String reason = reasonAgainst(player, i);
            options.add(new RealmScreenPayload.RealmOption(REALMS.get(i), reason == null,
                    reason == null ? "" : reason));
        }
        return options;
    }

    /** Why the stone will not take him anywhere from here, or null. */
    @Nullable
    static Component refusalHere(ServerPlayer player) {
        if (BossFights.isParticipant(player) || BossFights.inAnyArena(player.serverLevel(), player.blockPosition())) {
            return Component.translatable("message.enhanced.boss.held");
        }
        if (RoadTrials.isInside(player)) {
            return Component.translatable("message.enhanced.stones.space.road");
        }
        if (MjolnirPin.isPinned(player)) {
            return Component.translatable("message.enhanced.mjolnir.pinned_cannot");
        }
        if (HexManager.holdsInside(player.serverLevel(), player)) {
            return Component.translatable("message.enhanced.stones.space.hex");
        }
        return null;
    }

    /** Why the realm of index {@code realm} cannot be chosen now (a lang key), or null if it can. */
    @Nullable
    static String reasonAgainst(ServerPlayer player, int realm) {
        ResourceKey<Level> here = player.level().dimension();
        return switch (realm) {
            case OVERWORLD -> null;
            case NETHER -> here == Level.NETHER ? "message.enhanced.stones.space.here"
                    : player.server.getLevel(Level.NETHER) == null ? "message.enhanced.bifrost.no_world" : null;
            case END -> here == Level.END ? "message.enhanced.stones.space.here"
                    : player.server.getLevel(Level.END) == null ? "message.enhanced.bifrost.no_world" : null;
            case ASGARD -> {
                if (AsgardDimension.in(player)) {
                    yield "message.enhanced.bifrost.asgard_here";
                }
                if (!SpaceStone.reachesAsgard()) {
                    yield "message.enhanced.stones.space.no_asgard";
                }
                if (!Bifrost.asgardEnabled()) {
                    yield "message.enhanced.bifrost.asgard_closed";
                }
                yield AsgardDimension.level(player.server) == null ? "message.enhanced.bifrost.no_world" : null;
            }
            default -> "message.enhanced.bifrost.no_world";
        };
    }

    /**
     * The player chose the realm of index {@code index} on the screen ({@code RealmScreenPayload}'s list). The server
     * asks everything again: an open picker of his, the stone still his to use, the cooldown, the place.
     */
    public static void choose(ServerPlayer player, int index) {
        Pending pending = PENDING.remove(player.getUUID());
        long now = player.level().getGameTime();
        if (pending == null || now > pending.until() || index < 0 || index >= REALMS.size()) {
            return;
        }
        StoneUse.Cast cast = new StoneUse.Cast(player, pending.cast().data(), StoneKind.SPACE, pending.cast().source(),
                true, pending.cast().column());
        Component stone = StoneUse.refusal(player, StoneKind.SPACE, cast.source());
        if (stone == null && cast.source() != StoneUse.Source.ROW && !StoneUse.carried(player, StoneKind.SPACE)) {
            stone = Component.translatable("message.enhanced.stones.not_carried",
                    Component.translatable("item.enhanced.space_stone"));
        }
        if (stone != null) {
            player.displayClientMessage(stone, true);
            return;
        }
        if (StoneUse.restrained(player) || player.isSpectator()
                || !StoneCooldowns.ready(player, StoneKind.SPACE, now)) {
            return;
        }
        Component here = refusalHere(player);
        String reason = reasonAgainst(player, index);
        if (here != null || reason != null) {
            player.displayClientMessage(here != null ? here : Component.translatable(reason), true);
            return;
        }
        travel(cast, index);
    }

    /**
     * Takes him to the realm (the checks are done). He is told if there is no safe ground there.
     *
     * @return whether he went
     */
    public static boolean travel(StoneUse.Cast cast, int realm) {
        ServerPlayer player = cast.player();
        ServerLevel from = player.serverLevel();
        Arrival arrival = switch (realm) {
            case OVERWORLD -> overworld(player);
            case NETHER -> nether(player);
            case END -> end(player);
            case ASGARD -> asgard(player);
            default -> null;
        };
        if (arrival == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.stones.space.no_ground"), true);
            return false;
        }
        if (!HexManager.onTeleport(arrival.level(), player, arrival.spot())) {
            return false;
        }
        Vec3 origin = player.position();
        GlobalPos left = GlobalPos.of(from.dimension(), player.blockPosition());
        cast.startCooldown(StonesConfig.SPACE_REALM_COOLDOWN.get());
        player.causeFoodExhaustion(REALM_EXHAUSTION);
        swirl(from, origin);
        from.playSound(null, origin.x, origin.y, origin.z, ModSounds.SPACE_REALM_DEPART.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);
        StoneKind.SPACE.voice().use(from, origin, 0.6F, 1.0F);
        Net.sendToPlayersNear(from, null, origin.x, origin.y, origin.z, 64,
                new StoneFxPayload(StoneFxPayload.REALM_DEPART, player.getId(), List.of(origin), realm));
        player.stopRiding();
        ServerLevel to = arrival.level();
        Vec3 spot = arrival.spot();
        SpaceStone.moving(player, () -> {
            player.teleportTo(to, spot.x, spot.y, spot.z, arrival.yaw(), player.getXRot());
            return true;
        });
        if (player.level() != to) {
            return false; // (a gate refused it after all)
        }
        if (realm == ASGARD) {
            Bifrost.rememberCameFrom(player, left);
        }
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                StonesConfig.SPACE_ARRIVAL_RESISTANCE_TICKS.get(), 4, false, false, true));
        swirl(to, spot);
        to.playSound(null, spot.x, spot.y, spot.z, ModSounds.SPACE_REALM_ARRIVE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        Net.sendToPlayersNear(to, null, spot.x, spot.y, spot.z, 64,
                new StoneFxPayload(StoneFxPayload.REALM_ARRIVE, player.getId(), List.of(spot), realm));
        return true;
    }

    /** Where a journey lands: the level, the spot (feet) and the way he faces. */
    public record Arrival(ServerLevel level, Vec3 spot, float yaw) {
    }

    /** The Overworld: his respawn point there (bed or anchor), else the world's spawn; safe ground near it. */
    @Nullable
    static Arrival overworld(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        DimensionTransition spawn = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        BlockPos around = spawn.newLevel() == overworld ? BlockPos.containing(spawn.pos())
                : overworld.getSharedSpawnPos();
        if (spawn.newLevel() == overworld && standsAt(overworld, player, spawn.pos())) {
            return new Arrival(overworld, spawn.pos(), player.getYRot());
        }
        Vec3 spot = surfaceNear(overworld, player, around, OVERWORLD_SEARCH);
        return spot == null ? null : new Arrival(overworld, spot, player.getYRot());
    }

    /** The Nether: x/8, z/8 of where he is (from the End or Asgard: where he last was there), on a safe floor. */
    @Nullable
    static Arrival nether(ServerPlayer player) {
        ServerLevel nether = player.server.getLevel(Level.NETHER);
        if (nether == null) {
            return null;
        }
        BlockPos around;
        if (player.level().dimension() == Level.OVERWORLD) {
            around = BlockPos.containing(player.getX() / 8.0, 64, player.getZ() / 8.0);
        } else {
            around = LAST_NETHER.getOrDefault(player.getUUID(), new BlockPos(0, 64, 0));
        }
        Vec3 spot = floorNear(nether, player, around, StonesConfig.SPACE_NETHER_SEARCH.get());
        return spot == null ? null : new Arrival(nether, spot, player.getYRot());
    }

    /** The End: the main island, at ({@code endArrivalX}, top, 0), outside the exit portal. */
    @Nullable
    static Arrival end(ServerPlayer player) {
        ServerLevel end = player.server.getLevel(Level.END);
        if (end == null) {
            return null;
        }
        BlockPos around = new BlockPos(StonesConfig.SPACE_END_ARRIVAL_X.get(), 64, 0);
        Vec3 spot = surfaceNear(end, player, around, END_SEARCH);
        return spot == null ? null : new Arrival(end, spot, 90.0F);
    }

    /** Asgard: Heimdall's Observatory ({@link CityPlan#ARRIVAL}). */
    @Nullable
    static Arrival asgard(ServerPlayer player) {
        ServerLevel asgard = AsgardDimension.level(player.server);
        if (asgard == null) {
            return null;
        }
        return new Arrival(asgard, Bifrost.landing(asgard, player, CityPlan.ARRIVAL, CityPlan.ARRIVAL),
                CityPlan.ARRIVAL_YAW);
    }

    // ---- safe ground ----

    /** The first safe spot on the surface (the highest ground of a column) within {@code radius} of {@code around}. */
    @Nullable
    static Vec3 surfaceNear(ServerLevel level, Entity traveller, BlockPos around, int radius) {
        for (BlockPos column : spiral(around, radius)) {
            level.getChunk(column.getX() >> 4, column.getZ() >> 4);
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            if (top <= level.getMinBuildHeight()) {
                continue;
            }
            BlockPos feet = new BlockPos(column.getX(), top, column.getZ());
            if (safe(level, feet) && fits(level, traveller, Vec3.atBottomCenterOf(feet))) {
                return Vec3.atBottomCenterOf(feet);
            }
        }
        return null;
    }

    /** The first safe floor between the Nether's heights within {@code radius} of {@code around}. */
    @Nullable
    static Vec3 floorNear(ServerLevel level, Entity traveller, BlockPos around, int radius) {
        for (BlockPos column : spiral(around, radius)) {
            level.getChunk(column.getX() >> 4, column.getZ() >> 4);
            BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos(column.getX(), NETHER_MAX_Y, column.getZ());
            for (; feet.getY() >= NETHER_MIN_Y; feet.move(Direction.DOWN)) {
                if (safe(level, feet) && fits(level, traveller, Vec3.atBottomCenterOf(feet))) {
                    return Vec3.atBottomCenterOf(feet);
                }
            }
        }
        return null;
    }

    /** The columns round {@code centre} out to {@code radius}, nearest rings first (every other ring further out). */
    static List<BlockPos> spiral(BlockPos centre, int radius) {
        List<BlockPos> columns = new ArrayList<>();
        columns.add(centre);
        for (int ring = 1; ring <= radius; ring += ring < 4 ? 1 : 2) {
            int steps = Math.max(8, ring * 4);
            for (int i = 0; i < steps; i++) {
                double angle = Mth.TWO_PI * i / steps;
                columns.add(centre.offset(Mth.floor(Math.cos(angle) * ring), 0, Mth.floor(Math.sin(angle) * ring)));
            }
        }
        return columns;
    }

    /** Solid, harmless ground under {@code feet}; air at the feet and the head; no lava there. */
    public static boolean safe(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP) || !ground.getFluidState().isEmpty() || harmful(ground)) {
            return false;
        }
        for (BlockPos pos : new BlockPos[]{feet, feet.above()}) {
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty() || harmful(state)) {
                return false;
            }
        }
        return true;
    }

    private static boolean harmful(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.LAVA) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.COBWEB)
                || state.is(Blocks.END_PORTAL) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.BEDROCK);
    }

    /** Whether the traveller's standing body fits at {@code feet} in that level. */
    static boolean fits(ServerLevel level, Entity traveller, Vec3 feet) {
        return level.getWorldBorder().isWithinBounds(BlockPos.containing(feet))
                && level.noCollision(traveller, traveller.getDimensions(Pose.STANDING).makeBoundingBox(feet));
    }

    private static boolean standsAt(ServerLevel level, Entity traveller, Vec3 feet) {
        BlockPos below = BlockPos.containing(feet).below();
        return fits(level, traveller, feet) && !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    /** A rising spiral of Space Stone light round someone about to vanish, or just come. */
    private static void swirl(ServerLevel level, Vec3 at) {
        int[] colors = {TesseractItem.DIM, TesseractItem.MID, TesseractItem.BRIGHT, TesseractItem.CORE};
        for (int i = 0; i < 48; i++) {
            double t = i / 48.0;
            double angle = t * Mth.TWO_PI * 3;
            double radius = 1.1 - t * 0.5;
            level.sendParticles(new EnergyParticleOptions(colors[i % colors.length], 1.3F),
                    at.x + Math.cos(angle) * radius, at.y + t * 2.2, at.z + Math.sin(angle) * radius, 0,
                    -Math.sin(angle), 0.15, Math.cos(angle), 0.18);
        }
        level.sendParticles(new EnergyParticleOptions(TesseractItem.MID, 2.0F), at.x, at.y + 1.0, at.z, 30, 0.35, 0.7,
                0.35, 0.1);
    }

    /** Remembers where he stands in the Nether (the way back there from the End or Asgard); old pickers lapse. */
    static void tick(ServerPlayer player) {
        if (player.tickCount % 40 == 0 && player.level().dimension() == Level.NETHER) {
            LAST_NETHER.put(player.getUUID(), player.blockPosition());
        }
        Pending pending = PENDING.get(player.getUUID());
        if (pending != null && player.level().getGameTime() > pending.until()) {
            PENDING.remove(player.getUUID());
        }
    }

    static void forget(Player player) {
        PENDING.remove(player.getUUID());
    }

    static void clear() {
        PENDING.clear();
        LAST_NETHER.clear();
    }

    private SpaceRealms() {
    }
}

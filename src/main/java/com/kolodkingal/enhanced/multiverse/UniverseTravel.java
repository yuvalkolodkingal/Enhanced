package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.ability.Demiurge;
import com.kolodkingal.enhanced.network.UniverseScreenPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.stones.SpaceRealms;
import com.kolodkingal.enhanced.strange.SlingPortals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The ways between the universes (3.19), each opening the Multiverse screen ({@link UniverseScreenPayload}) with seven
 * universes: 616, 10005, 2149, the series' three What Ifs (the Gamma Bomb, Hydra, Ultron) and a random universe. The
 * random universe is a new one each time, unless the player holds an item renamed (at an anvil) with a number or any
 * words, or types a name on the screen (3.19.1): then it is the universe those words name, a world made from them as the
 * books of the game's April Fools update made their dimensions ({@link Multiverse#named}, {@link UniverseRecipe}).
 * <ul>
 *     <li>{@link #DREAM}, the Darkhold's Dream Walking (Sneak + use, looking straight down): the reader sleeps here and wakes there;</li>
 *     <li>{@link #RING}, the Sling Ring of a cosmic hero of the Ultimate versions (Billy the Demiurge, Cosmic Spider-Man;
 *     Sneak + use, looking straight down): a pair of portals to the other universe, open for a while for anyone to cross;</li>
 *     <li>{@link #BRIDGE}, Reed Richards' Multiverse Bridge ({@link MultiverseBridge}): its ring sends whoever stands in
 *     it and stays open for a while.</li>
 * </ul>
 * One lands on safe ground: in 616, 10005 and 2149 at the same place as where one left (they are the same Earth),
 * coming from elsewhere where one last was there (else the spawn, or a street near the Citadel in the two cities); in a
 * random universe in the middle of its land.
 */
public final class UniverseTravel {
    public static final int DREAM = 0;
    public static final int RING = 1;
    public static final int BRIDGE = 2;

    /** Looking down at least this far (degrees) turns the Darkhold's and the ring's Sneak + use to the multiverse. */
    public static final float LOOK_DOWN = 80.0F;
    /** How long an open screen waits for its answer (ticks). */
    static final int PICKER_TICKS = 1200;
    /** The colour of Reed's bridge and the ring's portals to other universes. */
    public static final int BRIDGE_BLUE = 0x3FA9FF;

    /** A screen the server opened: how, the universes it shows, until when, and the bridge (or null). */
    private record Pending(int mode, List<Integer> universes, long until, @Nullable BlockPos bridge) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** Player -> universe -> where he last stood there (the way back to a universe from far away). */
    private static final Map<UUID, Map<Integer, BlockPos>> LAST = new HashMap<>();

    private UniverseTravel() {
    }

    /** Whether the player looks up far enough to reach for the multiverse. */
    public static boolean looksDown(Player player) {
        return player.getXRot() >= LOOK_DOWN;
    }

    /** Whether the player is a cosmic hero of the Ultimate versions, whose Sling Ring reaches other universes. */
    public static boolean cosmic(Player player) {
        // (3.19: Ultimate Wanda too)
        return Demiurge.is(player) || com.kolodkingal.enhanced.ability.CosmicSpider.isCosmic(player)
                || com.kolodkingal.enhanced.ability.UltimateWanda.is(player);
    }

    // ---- the screen ----

    /** Opens the Multiverse screen for the player ({@code bridge}: the bridge's core, for {@link #BRIDGE}). */
    public static void open(ServerPlayer player, int mode, @Nullable BlockPos bridge) {
        int here = Multiverse.universeAt(player.level(), player.blockPosition());
        int whatIf = named(player);
        if (whatIf == 0 || whatIf == here) {
            do {
                whatIf = Multiverse.fold(player.getRandom().nextInt(Multiverse.CELLS) + 1L);
            } while (whatIf == here);
        }
        List<Integer> universes = List.of(Multiverse.MAIN, Multiverse.XMEN, Multiverse.ZOMBIES, Multiverse.GAMMA,
                Multiverse.HYDRA, Multiverse.ULTRON, whatIf);
        List<UniverseScreenPayload.UniverseOption> options = new ArrayList<>();
        for (int universe : universes) {
            String reason = reasonAgainst(player, universe);
            options.add(new UniverseScreenPayload.UniverseOption(universe, reason == null, reason == null ? "" : reason));
        }
        PENDING.put(player.getUUID(), new Pending(mode, universes, player.level().getGameTime() + PICKER_TICKS, bridge));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT,
                SoundSource.PLAYERS, 0.8F, 1.6F);
        Net.sendToPlayer(player, new UniverseScreenPayload(mode, options));
    }

    /** The universe a renamed item in either hand names, or 0. */
    static int named(Player player) {
        for (ItemStack stack : new ItemStack[]{player.getOffhandItem(), player.getMainHandItem()}) {
            if (!stack.isEmpty() && stack.has(DataComponents.CUSTOM_NAME)) {
                String words = stack.getHoverName().getString();
                if (!words.isBlank()) {
                    return Multiverse.named(words);
                }
            }
        }
        return 0;
    }

    /** Why the universe cannot be chosen now (a lang key), or null. */
    @Nullable
    static String reasonAgainst(ServerPlayer player, int universe) {
        int here = Multiverse.universeAt(player.level(), player.blockPosition());
        if (universe == here) {
            return "message.enhanced.multiverse.here";
        }
        return Multiverse.levelOf(player.server, universe) == null ? "message.enhanced.multiverse.no_world" : null;
    }

    /** The player chose the universe of index {@code index} on the screen. The server asks everything again. */
    public static void choose(ServerPlayer player, int index) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null || player.level().getGameTime() > pending.until() || index < 0
                || index >= pending.universes().size() || player.isSpectator()) {
            return;
        }
        go(player, pending, pending.universes().get(index));
    }

    /**
     * (3.19.1) The player typed a name on the screen: the universe those words name ({@link Multiverse#named}), a world
     * made from them as 20w14&infin; made one from the words of a book.
     */
    public static void chooseNamed(ServerPlayer player, String words) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null || player.level().getGameTime() > pending.until() || words.isBlank() || player.isSpectator()) {
            return;
        }
        go(player, pending, Multiverse.named(words));
    }

    private static void go(ServerPlayer player, Pending pending, int universe) {
        String reason = reasonAgainst(player, universe);
        if (reason != null) {
            player.displayClientMessage(Component.translatable(reason), true);
            return;
        }
        switch (pending.mode()) {
            case DREAM -> dreamWalk(player, universe);
            case RING -> ring(player, universe);
            case BRIDGE -> {
                if (pending.bridge() != null) {
                    MultiverseBridge.start(player, pending.bridge(), universe);
                }
            }
            default -> {
            }
        }
    }

    // ---- the journeys ----

    /** Where one lands in a universe: the level, the feet, the way one faces. Null if there is no safe ground. */
    @Nullable
    public static SpaceRealms.Arrival arrival(ServerPlayer player, int universe) {
        ServerLevel level = Multiverse.levelOf(player.server, universe);
        if (level == null) {
            return null;
        }
        int here = Multiverse.universeAt(player.level(), player.blockPosition());
        boolean sameEarth = (here == Multiverse.MAIN || here == Multiverse.XMEN || here == Multiverse.ZOMBIES)
                && (universe == Multiverse.MAIN || universe == Multiverse.XMEN || universe == Multiverse.ZOMBIES)
                && player.level().dimension() != Level.NETHER
                && player.level().dimension() != Level.END
                && (player.level().dimension() == Level.OVERWORLD || Multiverse.isUniverseDimension(player.level().dimension()));
        BlockPos around;
        if (Multiverse.isWhatIf(universe)) {
            BlockPos centre = Multiverse.centreOf(universe);
            around = centre.offset(player.getRandom().nextInt(129) - 64, 0, player.getRandom().nextInt(129) - 64);
        } else if (sameEarth) {
            around = player.blockPosition();
        } else {
            BlockPos last = LAST.getOrDefault(player.getUUID(), Map.of()).get(universe);
            if (last != null) {
                around = last;
            } else if (universe == Multiverse.HYDRA || universe == Multiverse.ULTRON) {
                around = CityChunkGenerator.ARRIVAL;
            } else if (universe == Multiverse.MAIN) {
                return SpaceRealms.overworld(player);
            } else {
                ServerLevel overworld = player.server.overworld();
                around = overworld.getSharedSpawnPos();
            }
        }
        // Over an ocean there is no ground: find land first from the generator's noise alone (no chunk is made for
        // it), then make only the chunks round that one spot.
        if (level.dimension() == Multiverse.CAVES_KEY) {
            // One great cave: the floor under its roof, not the roof.
            Vec3 floor = SpaceRealms.floorNear(level, player, around, 24);
            return floor == null ? null : new SpaceRealms.Arrival(level, floor, player.getYRot());
        }
        BlockPos land = land(level, around);
        Vec3 spot = SpaceRealms.surfaceNear(level, player, land, land.equals(around) ? 24 : 8);
        if (spot == null && !land.equals(around)) {
            spot = SpaceRealms.surfaceNear(level, player, around, 8);
        }
        return spot == null ? null : new SpaceRealms.Arrival(level, spot, player.getYRot());
    }

    /** How far (blocks) the search for land goes out, and how far apart the columns it looks at are. */
    static final int LAND_SEARCH = 768;
    static final int LAND_STEP = 32;

    /**
     * The nearest column round {@code around} whose land, as the generator would make it, stands above the sea: read
     * from the noise, without making a chunk. {@code around} itself if it is land, or if no land is near.
     */
    static BlockPos land(ServerLevel level, BlockPos around) {
        net.minecraft.world.level.chunk.ChunkGenerator generator = level.getChunkSource().getGenerator();
        net.minecraft.world.level.levelgen.RandomState random = level.getChunkSource().randomState();
        int sea = generator.getSeaLevel();
        for (int r = 0; r <= LAND_SEARCH; r += LAND_STEP) {
            int steps = r == 0 ? 1 : Math.max(8, (int) (Mth.TWO_PI * r / LAND_STEP));
            for (int i = 0; i < steps; i++) {
                double angle = i * Mth.TWO_PI / steps;
                int x = around.getX() + Mth.floor(Math.cos(angle) * r);
                int z = around.getZ() + Mth.floor(Math.sin(angle) * r);
                int top = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                if (top > sea) {
                    return new BlockPos(x, around.getY(), z);
                }
            }
        }
        return around;
    }

    /** Takes the player (alone) to the universe, with a swirl at both ends. Returns whether he went. */
    public static boolean send(ServerPlayer player, int universe, @Nullable SpaceRealms.Arrival given, int color) {
        SpaceRealms.Arrival arrival = given != null ? given : arrival(player, universe);
        if (arrival == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.stones.space.no_ground"), true);
            return false;
        }
        if (!HexManager.onTeleport(arrival.level(), player, arrival.spot())) {
            return false;
        }
        remember(player);
        ServerLevel from = player.serverLevel();
        swirl(from, player.position(), color);
        player.stopRiding();
        Vec3 spot = arrival.spot();
        player.teleportTo(arrival.level(), spot.x, spot.y, spot.z, arrival.yaw(), player.getXRot());
        if (player.level() != arrival.level()) {
            return false; // (a gate refused it)
        }
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false, true));
        swirl(arrival.level(), spot, color);
        arrival.level().playSound(null, spot.x, spot.y, spot.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.35F, 1.4F);
        welcome(player, universe);
        return true;
    }

    /** Says where he is now, in the middle of his screen and in chat. */
    public static void welcome(ServerPlayer player, int universe) {
        player.displayClientMessage(Component.translatable("message.enhanced.multiverse.arrived", Multiverse.name(universe))
                .withStyle(ChatFormatting.AQUA), false);
        MultiverseEvents.announced(player, universe);
    }

    /** The Darkhold's Dream Walking: he falls asleep here and wakes in the other universe, dizzy for a moment. */
    static void dreamWalk(ServerPlayer player, int universe) {
        if (!com.kolodkingal.enhanced.darkhold.DarkholdBook.holds(player)) {
            return;
        }
        if (send(player, universe, null, 0x9B30FF)) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, true));
            player.displayClientMessage(Component.translatable("message.enhanced.multiverse.dream_walk")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
        }
    }

    /** The cosmic Sling Ring: a pair of portals, the near one before him and the far one in the other universe. */
    static void ring(ServerPlayer player, int universe) {
        if (!cosmic(player)) {
            return;
        }
        ServerLevel here = player.serverLevel();
        Vec3 near = SlingPortals.nearFeet(player);
        if (!SlingPortals.roomFor(here, near)) {
            player.displayClientMessage(Component.translatable("message.enhanced.multiverse.no_room"), true);
            return;
        }
        SpaceRealms.Arrival arrival = arrival(player, universe);
        if (arrival == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.stones.space.no_ground"), true);
            return;
        }
        remember(player);
        Vec3 far = arrival.spot().add(Vec3.directionFromRotation(0.0F, arrival.yaw()).scale(SlingPortals.NEAR));
        if (!SlingPortals.roomFor(arrival.level(), far)) {
            far = arrival.spot();
        }
        SlingPortals.open(player, here, near, player.getYRot(), arrival.level(), far, arrival.yaw() + 180.0F,
                Demiurge.is(player) ? Demiurge.BLUE : BRIDGE_BLUE);
        player.displayClientMessage(Component.translatable("message.enhanced.multiverse.ring_open", Multiverse.name(universe))
                .withStyle(ChatFormatting.AQUA), true);
    }

    // ---- bookkeeping ----

    /** Remembers where the player stands in his universe (the way back there). */
    public static void remember(ServerPlayer player) {
        Level level = player.level();
        if (level.dimension() == Level.OVERWORLD || Multiverse.isUniverseDimension(level.dimension())) {
            LAST.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                    .put(Multiverse.universeAt(level, player.blockPosition()), player.blockPosition());
        }
    }

    /** Old screens lapse. */
    static void tick(ServerPlayer player) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending != null && player.level().getGameTime() > pending.until()) {
            PENDING.remove(player.getUUID());
        }
    }

    static void forget(Player player) {
        PENDING.remove(player.getUUID());
    }

    /** A rising spiral of light round someone who goes or comes. */
    static void swirl(ServerLevel level, Vec3 at, int color) {
        for (int i = 0; i < 40; i++) {
            double t = i / 40.0;
            double angle = t * Mth.TWO_PI * 3;
            double radius = 1.1 - t * 0.5;
            level.sendParticles(ParticleTypes.PORTAL, at.x + Math.cos(angle) * radius, at.y + t * 2.2,
                    at.z + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.2);
        }
        level.sendParticles(new com.kolodkingal.enhanced.particle.EnergyParticleOptions(color, 1.6F), at.x, at.y + 1.0, at.z,
                30, 0.35, 0.7, 0.35, 0.1);
    }
}

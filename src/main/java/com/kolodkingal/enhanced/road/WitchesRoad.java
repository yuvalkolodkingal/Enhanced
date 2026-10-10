package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.block.WitchesRoadDoorBlock;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.OpenRoadPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Witches' Road. Billy's chaos magic made the legendary road real, and its doors are set in the floors of the world
 * ({@link WitchesRoadDoorBlock}).
 *
 * <ul>
 *     <li>Seeking the Road: someone without powers who holds a Scarlet Witch Gene right-clicks a door. A ring of
 *     blue-green light runs round its outline, the door opens, and they choose whose road to walk: Billy's (Wiccan)
 *     or Tommy's (Speed). Choosing uses up one gene.</li>
 *     <li>Walking the Road: the Scarlet Witch and Billy, whose magic made it, step through a door and come up out of the
 *     next one in the same dimension (in the order the doors were placed, wrapping around).</li>
 * </ul>
 * An open door closes again {@link #OPEN_TICKS} after it opened, whatever happened.
 */
public final class WitchesRoad {
    /** How long the ring of light runs round the door before it opens (2 s). */
    public static final int RITUAL_TICKS = 40;
    /** How long a door stays open (5 s). */
    public static final int OPEN_TICKS = 100;
    /** How close to the open door a seeker must stay to choose. */
    public static final double CHOICE_DISTANCE = 8.0;
    /** Ticks between two walks of the Road, per player (3 s). */
    public static final int WALK_COOLDOWN = 60;
    /** Height of the top of a door (3 pixels): travellers arrive standing on it. */
    public static final double DOOR_HEIGHT = 3.0 / 16.0;

    // ---- the Road's own light: blue-green, not Billy's blue ----
    public static final int LIGHT_CORE = 0xBFF5F0;
    public static final int LIGHT = 0x45C6D6;
    public static final int LIGHT_DEEP = 0x2A98A8;

    /** A door whose ritual is running: who waits for it, and when it started. */
    private static final class Ritual {
        final Set<UUID> seekers = new LinkedHashSet<>();
        final long start;

        Ritual(long start) {
            this.start = start;
        }
    }

    /** Door → the ritual opening it. In memory only: a ritual lost to a restart just never opens its door. */
    private static final Map<GlobalPos, Ritual> RITUALS = new HashMap<>();
    /** Doors this server opened: they pour out light until they close. */
    private static final Set<GlobalPos> POURING = new HashSet<>();
    /** Player → game time of their last walk on the Road. */
    private static final Map<UUID, Long> LAST_WALK = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // The doors of the Road

    public static RoadDoors doors(Level level) {
        return level.getData(ModAttachments.ROAD_DOORS);
    }

    public static boolean isDoor(BlockState state) {
        return state.is(ModBlocks.WITCHES_ROAD_DOOR.get());
    }

    private static boolean isOpen(BlockState state) {
        return isDoor(state) && state.getValue(WitchesRoadDoorBlock.OPEN);
    }

    /** A door joins the end of the Road (it was placed). */
    public static void addDoor(ServerLevel level, BlockPos pos) {
        RoadDoors doors = doors(level);
        RoadDoors updated = doors.with(pos);
        if (updated != doors) {
            level.setData(ModAttachments.ROAD_DOORS, updated);
        }
    }

    /** A door leaves the Road (it was broken). */
    public static void removeDoor(ServerLevel level, BlockPos pos) {
        RoadDoors doors = doors(level);
        if (doors.doors().contains(pos)) {
            level.setData(ModAttachments.ROAD_DOORS, doors.without(pos));
        }
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        RITUALS.remove(key);
        POURING.remove(key);
    }

    /**
     * The door after {@code from} on the Road, or null if {@code from} is the only one. Listed doors that no longer
     * exist are skipped and dropped from the list.
     */
    @Nullable
    public static BlockPos nextDoor(ServerLevel level, BlockPos from) {
        // A door that somehow missed the list (placed while the list was lost) joins it now.
        addDoor(level, from);
        List<BlockPos> doors = doors(level).doors();
        int index = doors.indexOf(from);
        for (int i = 1; i < doors.size(); i++) {
            BlockPos candidate = doors.get((index + i) % doors.size());
            if (isDoor(level.getBlockState(candidate))) {
                return candidate;
            }
            removeDoor(level, candidate);
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Right-clicking a door

    /** Someone right-clicked a door; the gene may be in either hand. */
    public static void use(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Power power = Powers.power(player);
        if (geneHand(player) != null) {
            if (power.hasPowers()) {
                player.displayClientMessage(Component.translatable("message.enhanced.road.own_road").withStyle(ChatFormatting.GRAY), true);
                fizzle(level, pos);
            } else {
                seek(level, pos, player);
            }
        } else if (power == Power.SCARLET_WITCH || power == Power.BILLY) {
            walk(level, pos, player);
        } else {
            player.displayClientMessage(Component.translatable(power.hasPowers()
                    ? "message.enhanced.road.sealed" : "message.enhanced.road.witch_blood"), true);
            fizzle(level, pos);
        }
    }

    /** The hand holding a Scarlet Witch Gene (the main hand first), or null. */
    @Nullable
    private static InteractionHand geneHand(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(ModItems.SCARLET_WITCH_GENE.get())) {
                return hand;
            }
        }
        return null;
    }

    /** Witch blood asks the door to open: the ritual starts (others with the gene can join it before it ends). */
    private static void seek(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (isOpen(level.getBlockState(pos))) {
            // Already open: the Road asks them straight away.
            offerChoice(player, pos);
            return;
        }
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        Ritual ritual = RITUALS.get(key);
        if (ritual == null) {
            ritual = new Ritual(level.getGameTime());
            RITUALS.put(key, ritual);
            level.playSound(null, pos, ModSounds.WITCHES_ROAD.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (ritual.seekers.add(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.ritual").withStyle(s -> s.withColor(LIGHT)), true);
        }
    }

    /** Shows the seeker the "Seekest thou the Road?" screen. */
    private static void offerChoice(ServerPlayer player, BlockPos door) {
        PacketDistributor.sendToPlayer(player, new OpenRoadPayload(door.immutable()));
    }

    /** Called at the end of every level tick: rituals run their course, and open doors pour out light. */
    public static void tick(ServerLevel level) {
        if (!RITUALS.isEmpty()) {
            long now = level.getGameTime();
            List<Map.Entry<GlobalPos, Ritual>> finished = new ArrayList<>();
            Iterator<Map.Entry<GlobalPos, Ritual>> it = RITUALS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<GlobalPos, Ritual> entry = it.next();
                BlockPos pos = entry.getKey().pos();
                if (!entry.getKey().dimension().equals(level.dimension())) {
                    continue;
                }
                if (!level.isLoaded(pos) || !isDoor(level.getBlockState(pos))) {
                    it.remove();
                    continue;
                }
                long age = now - entry.getValue().start;
                if (age >= RITUAL_TICKS) {
                    it.remove();
                    finished.add(entry);
                } else {
                    ritualParticles(level, pos, age);
                }
            }
            for (Map.Entry<GlobalPos, Ritual> entry : finished) {
                finishRitual(level, entry.getKey().pos(), entry.getValue());
            }
        }
        if (!POURING.isEmpty()) {
            POURING.removeIf(key -> key.dimension().equals(level.dimension())
                    && (!level.isLoaded(key.pos()) || !isOpen(level.getBlockState(key.pos()))));
            for (GlobalPos key : POURING) {
                if (key.dimension().equals(level.dimension())) {
                    pourLight(level, key.pos());
                }
            }
        }
    }

    /** The door opens, and everyone still waiting by it (without powers yet) is asked to choose. */
    private static void finishRitual(ServerLevel level, BlockPos pos, Ritual ritual) {
        open(level, pos);
        for (UUID id : ritual.seekers) {
            ServerPlayer seeker = level.getServer().getPlayerList().getPlayer(id);
            if (seeker != null && seeker.level() == level && seeker.isAlive() && !Powers.power(seeker).hasPowers() && near(seeker, pos)) {
                offerChoice(seeker, pos);
            }
        }
    }

    /** Opens a door: the doors are gone and light pours out. It closes by itself {@link #OPEN_TICKS} later. */
    public static void open(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isDoor(state)) {
            return;
        }
        POURING.add(GlobalPos.of(level.dimension(), pos.immutable()));
        if (state.getValue(WitchesRoadDoorBlock.OPEN)) {
            scheduleClose(level, pos);
            return;
        }
        level.setBlock(pos, state.setValue(WitchesRoadDoorBlock.OPEN, true), Block.UPDATE_ALL);
        scheduleClose(level, pos);
        level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.7F);
        RandomSource random = level.getRandom();
        for (int i = 0; i < 24; i++) {
            level.sendParticles(new EnergyParticleOptions(i % 3 == 0 ? LIGHT_CORE : LIGHT, 1.3F),
                    pos.getX() + 0.15 + random.nextDouble() * 0.7, pos.getY() + DOOR_HEIGHT, pos.getZ() + 0.15 + random.nextDouble() * 0.7,
                    0, 0, 1, 0, 0.1 + random.nextDouble() * 0.2);
        }
    }

    /** An open door closes {@link #OPEN_TICKS} from now (unless its closing is already on its way). */
    public static void scheduleClose(ServerLevel level, BlockPos pos) {
        Block door = ModBlocks.WITCHES_ROAD_DOOR.get();
        if (!level.getBlockTicks().hasScheduledTick(pos, door)) {
            level.scheduleTick(pos, door, OPEN_TICKS);
        }
    }

    /** The scheduled tick of an open door: it closes. */
    public static void close(ServerLevel level, BlockPos pos, BlockState state) {
        POURING.remove(GlobalPos.of(level.dimension(), pos.immutable()));
        if (isOpen(state)) {
            level.setBlock(pos, state.setValue(WitchesRoadDoorBlock.OPEN, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Choosing whose road to walk

    /**
     * The seeker's answer on the "Seekest thou the Road?" screen. They must still have no powers, still hold a gene and
     * stand within {@link #CHOICE_DISTANCE} of the open door; then one gene is used up and they awaken as Billy or Tommy.
     *
     * @return whether they awakened
     */
    public static boolean choose(ServerPlayer player, BlockPos door, Power choice) {
        if ((choice != Power.BILLY && choice != Power.TOMMY) || !player.isAlive() || player.isSpectator()
                || Powers.power(player).hasPowers() || !near(player, door)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(door) || !isOpen(level.getBlockState(door))) {
            return false;
        }
        InteractionHand hand = geneHand(player);
        if (hand == null) {
            return false;
        }
        player.getItemInHand(hand).consume(1, player);
        Powers.awaken(player, choice);
        return true;
    }

    private static boolean near(ServerPlayer player, BlockPos door) {
        return player.position().distanceToSqr(Vec3.atBottomCenterOf(door)) <= CHOICE_DISTANCE * CHOICE_DISTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Walking the Road

    /** The Scarlet Witch or Billy steps down through a door and comes up out of the next one. */
    private static void walk(ServerLevel level, BlockPos from, ServerPlayer player) {
        long now = level.getGameTime();
        Long last = LAST_WALK.get(player.getUUID());
        if (last != null && now >= last && now - last < WALK_COOLDOWN) {
            return;
        }
        BlockPos to = nextDoor(level, from);
        if (to == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.only_door"), true);
            fizzle(level, from);
            return;
        }
        Vec3 arrival = new Vec3(to.getX() + 0.5, to.getY() + DOOR_HEIGHT, to.getZ() + 0.5);
        if (!HexManager.onTeleport(level, player, arrival)) {
            return;
        }
        LAST_WALK.put(player.getUUID(), now);
        // Nothing held with telekinesis comes along.
        Telekinesis.release(player, false);
        open(level, from);
        Vec3 start = player.position();
        lightColumn(level, start, false);
        level.playSound(null, start.x, start.y, start.z, ModSounds.ROAD_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 0.9F);

        player.teleportTo(level, arrival.x, arrival.y, arrival.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        open(level, to);
        lightColumn(level, arrival, true);
        level.playSound(null, arrival.x, arrival.y, arrival.z, ModSounds.ROAD_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 1.1F);
        level.playSound(null, arrival.x, arrival.y, arrival.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Effects

    /** A point on the glowing outline of a door, {@code t} (0-1) of the way round it. */
    private static Vec3 outlinePoint(BlockPos pos, double t) {
        double inset = 0.06;
        double side = 1.0 - 2 * inset;
        double d = (t - Math.floor(t)) * 4.0;
        int edge = (int) d;
        double along = (d - edge) * side;
        double x = switch (edge) {
            case 0 -> inset + along;
            case 1 -> 1.0 - inset;
            case 2 -> 1.0 - inset - along;
            default -> inset;
        };
        double z = switch (edge) {
            case 0 -> inset;
            case 1 -> inset + along;
            case 2 -> 1.0 - inset;
            default -> 1.0 - inset - along;
        };
        return new Vec3(pos.getX() + x, pos.getY() + DOOR_HEIGHT + 0.03, pos.getZ() + z);
    }

    /** The ritual: a ring of light runs twice round the door's outline; at the end the whole outline blazes. */
    private static void ritualParticles(ServerLevel level, BlockPos pos, long age) {
        double lap = age * 2.0 / RITUAL_TICKS;
        for (int i = 0; i < 5; i++) {
            Vec3 p = outlinePoint(pos, lap - i * 0.025);
            int color = i < 2 ? LIGHT_CORE : LIGHT;
            level.sendParticles(new EnergyParticleOptions(color, i == 0 ? 1.4F : 1.0F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        if (age >= RITUAL_TICKS - 8) {
            for (int i = 0; i < 12; i++) {
                Vec3 p = outlinePoint(pos, i / 12.0 + level.getRandom().nextDouble() / 12.0);
                level.sendParticles(new EnergyParticleOptions(LIGHT, 1.1F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Light pouring up out of an open door. */
    private static void pourLight(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 2; i++) {
            int color = random.nextInt(3) == 0 ? LIGHT_CORE : LIGHT;
            level.sendParticles(new EnergyParticleOptions(color, 1.0F + random.nextFloat() * 0.5F),
                    pos.getX() + 0.15 + random.nextDouble() * 0.7, pos.getY() + DOOR_HEIGHT, pos.getZ() + 0.15 + random.nextDouble() * 0.7,
                    0, 0, 1, 0, 0.06 + random.nextDouble() * 0.08);
        }
    }

    /** A burst of the Road's light where someone goes down into it ({@code rising} false) or comes up out of it. */
    private static void lightColumn(ServerLevel level, Vec3 at, boolean rising) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 40; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.2 + random.nextDouble() * 0.5;
            int color = i % 3 == 0 ? LIGHT_CORE : LIGHT;
            level.sendParticles(new EnergyParticleOptions(color, 1.4F),
                    at.x + Math.cos(angle) * radius, at.y + random.nextDouble() * 1.8, at.z + Math.sin(angle) * radius,
                    0, 0, rising ? 1 : -1, 0, 0.05 + random.nextDouble() * 0.15);
        }
    }

    /** The door refuses: a small fizzle of its light. */
    private static void fizzle(ServerLevel level, BlockPos pos) {
        level.sendParticles(new EnergyParticleOptions(LIGHT_DEEP, 0.8F), pos.getX() + 0.5, pos.getY() + DOOR_HEIGHT + 0.05,
                pos.getZ() + 0.5, 8, 0.25, 0.02, 0.25, 0.02);
        level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.4F);
    }

    // ------------------------------------------------------------------------------------------------------------

    /** A player left the game. */
    public static void forget(UUID player) {
        LAST_WALK.remove(player);
    }

    /** Forget everything (server stopping). */
    public static void clear() {
        RITUALS.clear();
        POURING.clear();
        LAST_WALK.clear();
    }

    private WitchesRoad() {
    }
}

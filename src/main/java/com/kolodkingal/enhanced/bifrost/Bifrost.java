package com.kolodkingal.enhanced.bifrost;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.client.screen.BifrostScreen;
import com.kolodkingal.enhanced.config.AsgardConfig;
import com.kolodkingal.enhanced.config.BifrostConfig;
import com.kolodkingal.enhanced.config.HeimdallConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.ThorFxPayload;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Bifrost (3.5: moved here out of {@code ThorAbilities}, which keeps one-line delegates): "Heimdall! Open the
 * Bifrost!" A beam of rainbow light gathers round the traveller and, a moment later, takes him home, to where he looks,
 * to a place he saved, to Asgard (always into Heimdall's Observatory, {@link CityPlan#ARRIVAL}) or back where he came
 * from; it burns its circle of runes into the ground where it lands and where it left. The places and the cooldown are
 * the traveller's {@link BifrostData}, the journey is {@link PlaceBook}'s.
 *
 * <p>Who may open it with his own key is {@link BifrostKeys}; the Heimdall NPC, the rune stones, the void catch and
 * Heimdall sending someone open it for him ({@link Source}).
 *
 * <p><b>Group travel</b> (3.5): at the moment it departs, the Bifrost takes everyone inside its circle (the circle of
 * runes, radius {@code bifrost.groupRadius}, {@link #CIRCLE_RADIUS} by default): every player and creature whose feet
 * are in it (horizontally, and at most {@link #GROUP_HEIGHT} blocks up or down), pets, villagers and mounts with their
 * riders, items lying on the ground, boats and minecarts. Never bosses or what a boss fight made, spectators, a player
 * already on a journey of his own, nor (with {@code bifrost.sneakToStay}) a sneaking player. Each lands round the
 * traveller at the place he stood in the circle, on safe ground (never inside a block). Only the traveller pays the
 * cooldown. A gate that refuses one of them (Asgard's, a boss arena's, a Hex wall, the Road) leaves him behind and the
 * others go.
 *
 * <p>Every teleport the Bifrost makes is marked while it runs ({@link #isMoving}), so {@code AsgardGate} lets it
 * through.
 */
public final class Bifrost {
    /** Ticks the Bifrost's light gathers around its traveller before it takes them (a key, the Heimdall NPC). */
    public static final int CHARGE = 30;
    /** Ticks a rune stone's Bifrost gathers (3 s). */
    public static final int STONE_CHARGE = 60;
    /** Ticks the Bifrost gathers round the one Heimdall sends (2 s: he can step out). */
    public static final int SEND_CHARGE = 40;
    /** How far the Bifrost reaches to where the traveller looks. */
    public static final int LOOK_RANGE = 512;
    /** The radius of the circle of runes the Bifrost burns (what the client draws): the default of {@code groupRadius}. */
    public static final double CIRCLE_RADIUS = 3.2;
    /** How far above or below the traveller's feet a member of the group may stand. */
    public static final double GROUP_HEIGHT = 3.0;
    /** The most the Bifrost takes along at once (the nearest first). */
    public static final int MAX_GROUP = 64;
    /** How far up or down a member's landing is searched for safe ground. */
    public static final int LANDING_SEARCH = 4;

    /** Where the Bifrost goes. */
    public sealed interface Destination {
        Destination HOME = new Home();
        Destination LOOK = new Look();
        Destination ASGARD = new Asgard();
        Destination CAME_FROM = new CameFrom();

        /** The traveller's respawn point (bed or anchor, else the world spawn), in any dimension. */
        record Home() implements Destination {
        }

        /** The saved place at {@code index}, if it is still the one called {@code name}. */
        record Place(int index, String name) implements Destination {
        }

        /** The spot the traveller looks at, up to {@link #LOOK_RANGE} blocks away. */
        record Look() implements Destination {
        }

        /** Asgard: always Heimdall's Observatory ({@link CityPlan#ARRIVAL}). Not offered inside Asgard. */
        record Asgard() implements Destination {
        }

        /** Where the traveller last entered Asgard from ({@code AsgardProgress.cameFrom}). */
        record CameFrom() implements Destination {
        }
    }

    /** Who opens the Bifrost for the traveller. */
    public enum Source {
        /** His own key (Mjolnir, Stormbreaker, Hofund, Gungnir, Heimdall's power): its cooldown and its refusals. */
        KEY,
        /** The Heimdall NPC: no key and no cooldown on the traveller. */
        NPC,
        /** A Bifrost rune stone (its own cooldown is the stone's). */
        STONE,
        /** Heimdall catches a fall into the void: at once, no key, no cooldown. */
        RESCUE,
        /** Heimdall sends someone else: the cooldown is Heimdall's. */
        HEIMDALL_SEND;

        int charge() {
            return switch (this) {
                case KEY, NPC -> CHARGE;
                case STONE -> STONE_CHARGE;
                case RESCUE -> 0;
                case HEIMDALL_SEND -> SEND_CHARGE;
            };
        }
    }

    /** Something that wants to know where the Bifrost touches the ground (the Observatory's mechanism, the progress). */
    @FunctionalInterface
    public interface Listener {
        /**
         * @param level     where it touched
         * @param at        the traveller's feet there
         * @param traveller the one who opened it (or for whom it was opened)
         */
        void on(ServerLevel level, Vec3 at, ServerPlayer traveller);
    }

    /** Told when the Bifrost sets its traveller down (after the group). */
    public static final List<Listener> ARRIVED = new CopyOnWriteArrayList<>();
    /** Told when the Bifrost takes its traveller away (before the group goes). */
    public static final List<Listener> DEPARTED = new CopyOnWriteArrayList<>();

    /** The entities the Bifrost is moving right now (see {@link #isMoving}). Server thread only. */
    private static final Set<UUID> MOVING = new HashSet<>();
    /** Traveller -> the centre of his circle (his feet when the Bifrost began to gather). */
    private static final Map<UUID, Vec3> ORIGINS = new HashMap<>();

    // ---- the keys ----

    /**
     * Using a key of the Bifrost (Mjolnir, Stormbreaker, Hofund, one's own Gungnir; right-click): the Bifrost screen
     * opens on the client (home, the saved places, save this one); with Sneak, the Bifrost opens to where the player
     * looks.
     */
    public static InteractionResultHolder<ItemStack> useKey(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer server) {
                toLook(server);
            }
        } else if (level.isClientSide) {
            // Client only: this branch never runs on a dedicated server, so the screen class is never loaded there.
            BifrostScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // ---- the journeys ----

    /** Home, to the traveller's respawn point (bed or respawn anchor, otherwise the world spawn), even in another dimension. */
    public static void home(ServerPlayer player) {
        open(player, Destination.HOME, Source.KEY);
    }

    /** To the spot the player looks at, up to 512 blocks away. */
    public static void toLook(ServerPlayer player) {
        open(player, Destination.LOOK, Source.KEY);
    }

    /**
     * To the saved place at {@code index}, in whatever dimension it is, if it is still the one called {@code name} (the
     * list may have changed since the screen showed it).
     */
    public static void toPlace(ServerPlayer player, int index, String name) {
        open(player, new Destination.Place(index, name), Source.KEY);
    }

    /** To Asgard (3.5): into Heimdall's Observatory. */
    public static void toAsgard(ServerPlayer player) {
        open(player, Destination.ASGARD, Source.KEY);
    }

    /**
     * Opens the Bifrost for {@code player} to {@code destination}. A {@link Source#KEY} asks his key and its cooldown
     * (as Thor's Bifrost always did); the others do not. Returns whether the light began to gather.
     */
    public static boolean open(ServerPlayer player, Destination destination, Source source) {
        // (A place that is not on the list any more is ignored before anything else, as it always was.)
        if (destination instanceof Destination.Place place
                && PlaceBook.placeAt(player, PlaceBook.Kind.BIFROST, place.index(), place.name()) == null) {
            return false;
        }
        if (source == Source.KEY ? !mayOpen(player) : player.isSpectator() || PlaceBook.travelling(player)) {
            return false;
        }
        Target target = resolve(player, destination);
        if (target == null) {
            return false;
        }
        begin(player, target, source);
        if (source.charge() == 0) {
            tick(player, Powers.get(player));
        }
        return true;
    }

    /** Where a destination is for this traveller now, or null (he is told why). */
    @Nullable
    private static Target resolve(ServerPlayer player, Destination destination) {
        return switch (destination) {
            case Destination.Home home -> {
                DimensionTransition spawn = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
                yield new Target(spawn.newLevel().dimension(), spawn.pos(), player.getYRot());
            }
            case Destination.Look look -> {
                BlockHitResult hit = Raycasts.block(player, LOOK_RANGE, 1.0F);
                if (hit.getType() == HitResult.Type.MISS) {
                    player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_ground"), true);
                    yield null;
                }
                yield new Target(player.level().dimension(), Vec3.atBottomCenterOf(hit.getBlockPos().above()), player.getYRot());
            }
            case Destination.Place place -> {
                BifrostData.Place saved = PlaceBook.placeAt(player, PlaceBook.Kind.BIFROST, place.index(), place.name());
                if (saved == null) {
                    yield null;
                }
                if (player.server.getLevel(saved.dimension()) == null) {
                    player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_world"), true);
                    yield null;
                }
                // The Bifrost always enters Asgard at the Observatory: a place in Asgard is reached directly only from Asgard.
                if (saved.dimension() == AsgardDimension.KEY && !AsgardDimension.in(player)) {
                    yield asgard(player);
                }
                yield new Target(saved.dimension(), saved.pos(), saved.yaw());
            }
            case Destination.Asgard asgard -> asgard(player);
            case Destination.CameFrom back -> {
                GlobalPos from = player.getData(ModAttachments.ASGARD).cameFrom();
                if (from == null) {
                    player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_came_from"), true);
                    yield null;
                }
                if (player.server.getLevel(from.dimension()) == null) {
                    player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_world"), true);
                    yield null;
                }
                yield new Target(from.dimension(), Vec3.atBottomCenterOf(from.pos()), player.getYRot());
            }
        };
    }

    /** Asgard's Observatory, or null (Asgard is switched off, missing, or the traveller is already there). */
    @Nullable
    private static Target asgard(ServerPlayer player) {
        if (AsgardDimension.in(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.asgard_here"), true);
            return null;
        }
        if (!asgardEnabled()) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.asgard_closed"), true);
            return null;
        }
        if (AsgardDimension.level(player.server) == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_world"), true);
            return null;
        }
        return new Target(AsgardDimension.KEY, CityPlan.ARRIVAL, CityPlan.ARRIVAL_YAW);
    }

    /** Whether Asgard can be reached ({@code asgard.enabled}). */
    public static boolean asgardEnabled() {
        return !EnhancedConfig.SPEC.isLoaded() || AsgardConfig.ENABLED.get();
    }

    private record Target(ResourceKey<Level> dimension, Vec3 pos, float yaw) {
    }

    /**
     * Heimdall (or a custom hero with Heimdall's Bifrost Strike) sends the player {@code targetId} (the Send mode of
     * the Bifrost screen): {@code name} is {@code "home"}, {@code "asgard"} or the name of one of Heimdall's saved
     * places. The target must be a player within {@code heimdall.sendRange}; the cooldown is Heimdall's Bifrost
     * Strike's.
     */
    public static void send(ServerPlayer heimdall, int targetId, String name) {
        PowerData data = Powers.get(heimdall);
        if (!data.has(HeroAbility.HEIMDALL_STRIKE) || heimdall.isSpectator()) {
            return;
        }
        long now = heimdall.level().getGameTime();
        if (!data.isReady(HeroAbility.HEIMDALL_STRIKE, now)) {
            return;
        }
        if (!(heimdall.serverLevel().getEntity(targetId) instanceof ServerPlayer target) || target == heimdall
                || target.distanceTo(heimdall) > sendRange()) {
            return;
        }
        Target where;
        if ("home".equals(name)) {
            where = resolve(target, Destination.HOME);
        } else if ("asgard".equals(name)) {
            where = asgard(target);
        } else {
            List<BifrostData.Place> places = PlaceBook.Kind.BIFROST.data(heimdall).places();
            BifrostData.Place place = places.stream().filter(p -> p.name().equals(name)).findFirst().orElse(null);
            where = place == null || heimdall.server.getLevel(place.dimension()) == null ? null
                    : place.dimension() == AsgardDimension.KEY && !AsgardDimension.in(target) ? asgard(target)
                    : new Target(place.dimension(), place.pos(), place.yaw());
        }
        if (where == null || target.isSpectator() || PlaceBook.travelling(target)) {
            return;
        }
        begin(target, where, Source.HEIMDALL_SEND);
        data.startCooldown(HeroAbility.HEIMDALL_STRIKE, now, cooldownTicks());
    }

    private static double sendRange() {
        return EnhancedConfig.SPEC.isLoaded() ? HeimdallConfig.SEND_RANGE.get() : 24;
    }

    // ---- the saved places ----

    /**
     * Marks where the player stands, facing the way they face, as a place the Bifrost can take them back to. Saving a
     * name again moves that place; the Bifrost's runes flare at their feet.
     */
    public static void savePlace(ServerPlayer player, String typed) {
        if (player.isSpectator()) {
            return;
        }
        if (!BifrostKeys.holdsKey(player)) {
            player.displayClientMessage(Component.translatable(BifrostKeys.refusalKey(player)), true);
            return;
        }
        BifrostData.Place place = PlaceBook.save(player, PlaceBook.Kind.BIFROST, typed, false);
        if (place == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.full", BifrostData.MAX_PLACES), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        mark(level, player.position());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.2F);
        player.displayClientMessage(Component.translatable("message.enhanced.bifrost.saved", place.name()), true);
    }

    /** Forgets the saved place at {@code index}, if it is still the one called {@code name}. */
    public static void forgetPlace(ServerPlayer player, int index, String name) {
        BifrostData.Place place = PlaceBook.forget(player, PlaceBook.Kind.BIFROST, index, name);
        if (place != null) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.forgotten", place.name()), true);
        }
    }

    // ---- opening ----

    /** The Bifrost answers whoever holds one of its keys ({@link BifrostKeys}), once it has gathered its strength again. */
    private static boolean mayOpen(ServerPlayer player) {
        if (player.isSpectator() || PlaceBook.travelling(player)) {
            return false;
        }
        if (MjolnirPin.isPinned(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.mjolnir.pinned_cannot"), true);
            return false;
        }
        if (!BifrostKeys.mayOpen(player)) {
            player.displayClientMessage(Component.translatable(BifrostKeys.refusalKey(player)), true);
            return false;
        }
        long wait = PlaceBook.cooldown(player, PlaceBook.Kind.BIFROST);
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.not_ready", (wait + 19) / 20), true);
            return false;
        }
        return true;
    }

    private static int cooldownTicks() {
        return EnhancedConfig.BIFROST_COOLDOWN.get();
    }

    private static void begin(ServerPlayer player, Target target, Source source) {
        ServerLevel level = player.serverLevel();
        PowerData data = Powers.get(player);
        if (source == Source.KEY) {
            long now = level.getGameTime();
            int cooldown = cooldownTicks();
            long ready = now + cooldown;
            // The same cooldown, shown on the HUD of the hero whose Bifrost it is (3.4: on a custom hero's key, by the
            // strength of his Bifrost).
            for (HeroAbility row : new HeroAbility[]{HeroAbility.THOR_BIFROST, HeroAbility.HEIMDALL_BIFROST}) {
                if (data.has(row)) {
                    data.startCooldown(row, now, cooldown);
                    int slot = data.slot(row);
                    if (data.power() == Power.CUSTOM && slot > 0) {
                        ready = data.cooldownUntil(slot);
                    }
                    break;
                }
            }
            PlaceBook.readyAt(player, PlaceBook.Kind.BIFROST, ready);
        }
        PlaceBook.begin(player, PlaceBook.Kind.BIFROST, target.dimension(), target.pos(), target.yaw(), source.charge());
        ORIGINS.put(player.getUUID(), player.position());
        data.set(Live.BIFROST, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BIFROST_CHARGE.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
    }

    // ---- the journey: every player, every tick ----

    /** Whoever the Bifrost is about to take is held in its light; when the moment comes, it takes them (and the group). */
    public static void tick(ServerPlayer player, PowerData data) {
        PlaceBook.Journey journey = PlaceBook.journey(player, PlaceBook.Kind.BIFROST);
        if (journey == null) {
            data.set(Live.BIFROST, false);
            ORIGINS.remove(player.getUUID());
            return;
        }
        if (PlaceBook.waiting(player, journey)) {
            // Held in the beam: rising slightly, unable to walk away.
            player.setDeltaMovement(0, 0.02, 0);
            player.hurtMarked = true;
            player.resetFallDistance();
            return;
        }
        data.set(Live.BIFROST, false);
        ServerLevel from = player.serverLevel();
        Vec3 center = ORIGINS.remove(player.getUUID());
        if (center == null || center.distanceToSqr(player.position()) > 4.0) {
            center = player.position();
        }
        // Who stands in the circle is seen now, before the traveller leaves it; his mount, if it goes, carries him again.
        List<Entity> group = player.isAlive() && from.getGameTime() <= journey.arrivesAt() + 5 ? gather(player, from, center) : List.of();
        Entity mount = player.getVehicle();
        PlaceBook.Landing landing;
        MOVING.add(player.getUUID());
        try {
            landing = PlaceBook.complete(player, journey);
        } finally {
            MOVING.remove(player.getUUID());
        }
        if (landing == null) {
            // The world is gone, or the traveller wasn't there to be taken (dead, or away), or a rule refused: the light went out.
            return;
        }
        for (Listener listener : DEPARTED) {
            listener.on(landing.from(), landing.left(), player);
        }
        PacketDistributor.sendToPlayersNear(landing.from(), null, landing.left().x, landing.left().y, landing.left().z, 128,
                new ThorFxPayload(ThorFxPayload.BIFROST_DEPART, player.getId(), List.of(landing.left())));
        mark(landing.from(), landing.left());
        ServerLevel destination = landing.to();
        carry(player, group, mount, center, destination);
        destination.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BIFROST_ARRIVE.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        PacketDistributor.sendToPlayersNear(destination, null, player.getX(), player.getY(), player.getZ(), 128,
                new ThorFxPayload(ThorFxPayload.BIFROST_ARRIVAL, player.getId(), List.of(player.position())));
        mark(destination, player.position());
        for (Listener listener : ARRIVED) {
            listener.on(destination, player.position(), player);
        }
    }

    // ---- the group ----

    /** The radius of the circle the Bifrost takes everyone from ({@code bifrost.groupRadius}). */
    public static double groupRadius() {
        return EnhancedConfig.SPEC.isLoaded() ? BifrostConfig.GROUP_RADIUS.get() : CIRCLE_RADIUS;
    }

    private static boolean sneakToStay() {
        return EnhancedConfig.SPEC.isLoaded() && BifrostConfig.SNEAK_TO_STAY.get();
    }

    /**
     * Everyone the Bifrost takes along with {@code traveller} from the circle round {@code center} (the nearest first,
     * at most {@link #MAX_GROUP}), and the riders of every mount among them.
     */
    public static List<Entity> gather(ServerPlayer traveller, ServerLevel level, Vec3 center) {
        double radius = groupRadius();
        if (radius <= 0) {
            return List.of();
        }
        AABB box = new AABB(center.x - radius, center.y - GROUP_HEIGHT, center.z - radius,
                center.x + radius, center.y + GROUP_HEIGHT + 0.01, center.z + radius);
        List<Entity> found = new ArrayList<>(level.getEntities(traveller, box.inflate(0, 2, 0),
                e -> inCircle(e, center, radius) && goesAlong(traveller, e)));
        found.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(center)));
        List<Entity> group = new ArrayList<>(found.subList(0, Math.min(found.size(), MAX_GROUP)));
        for (Entity e : List.copyOf(group)) {
            for (Entity rider : e.getIndirectPassengers()) {
                if (rider != traveller && !group.contains(rider) && goesAlong(traveller, rider)) {
                    group.add(rider);
                }
            }
        }
        return group;
    }

    /** Whether the entity's feet are inside the circle. */
    public static boolean inCircle(Entity e, Vec3 center, double radius) {
        double dx = e.getX() - center.x;
        double dz = e.getZ() - center.z;
        return dx * dx + dz * dz <= radius * radius && Math.abs(e.getY() - center.y) <= GROUP_HEIGHT;
    }

    /** Whether the Bifrost may take this entity along with its traveller (its place aside). */
    public static boolean goesAlong(ServerPlayer traveller, Entity e) {
        if (e == traveller || !e.isAlive() || e.isRemoved() || e.isSpectator()) {
            return false;
        }
        if (!(e instanceof LivingEntity) && !(e instanceof ItemEntity) && !(e instanceof VehicleEntity) || e instanceof ArmorStand) {
            return false;
        }
        if (e.getType().is(ModTags.BOSSES) || e.getType().is(ModTags.BOSS_MINIONS) || BossFights.isFightEntity(e)
                || BossFights.fightOf(e) != null || MjolnirPin.isPinned(e)) {
            return false;
        }
        if (e instanceof ServerPlayer p) {
            return !PlaceBook.travelling(p) && !(sneakToStay() && p.isShiftKeyDown());
        }
        return true;
    }

    /** Sets the group down round the traveller (who has landed), each where he stood in the circle, on safe ground. */
    private static void carry(ServerPlayer traveller, List<Entity> group, @Nullable Entity travellerMount, Vec3 center,
                              ServerLevel destination) {
        if (group.isEmpty()) {
            return;
        }
        Vec3 base = traveller.position();
        Map<UUID, UUID> rides = new HashMap<>();
        if (travellerMount != null && group.contains(travellerMount)) {
            rides.put(traveller.getUUID(), travellerMount.getUUID());
        }
        for (Entity e : group) {
            Entity vehicle = e.getVehicle();
            if (vehicle != null && group.contains(vehicle)) {
                rides.put(e.getUUID(), vehicle.getUUID());
            }
        }
        for (Entity e : group) {
            e.stopRiding();
        }
        Map<UUID, Entity> arrived = new HashMap<>();
        arrived.put(traveller.getUUID(), traveller);
        for (Entity e : group) {
            Vec3 offset = e.position().subtract(center);
            Vec3 to = landing(destination, e, base.add(offset), base);
            if (!HexManager.onTeleport(destination, e, to)) {
                continue;
            }
            Entity moved = move(e, destination, to);
            if (moved != null && moved.level() == destination) {
                moved.resetFallDistance();
                arrived.put(moved.getUUID(), moved);
            }
        }
        rides.forEach((rider, vehicle) -> {
            Entity r = arrived.get(rider);
            Entity v = arrived.get(vehicle);
            if (r != null && v != null && r.level() == v.level()) {
                r.startRiding(v, true);
            }
        });
    }

    /** Moves one member; returns the entity at the destination (a new one for a creature that changed dimension), or null. */
    @Nullable
    private static Entity move(Entity e, ServerLevel destination, Vec3 to) {
        MOVING.add(e.getUUID());
        try {
            if (e instanceof ServerPlayer p) {
                p.teleportTo(destination, to.x, to.y, to.z, p.getYRot(), p.getXRot());
                return p;
            }
            if (e.level() == destination) {
                e.teleportTo(to.x, to.y, to.z);
                return e;
            }
            return e.changeDimension(new DimensionTransition(destination, to, Vec3.ZERO, e.getYRot(), e.getXRot(),
                    DimensionTransition.DO_NOTHING));
        } finally {
            MOVING.remove(e.getUUID());
        }
    }

    /**
     * Where a member lands: {@code want} (the traveller's landing plus the member's place in the circle), moved up or
     * down by at most {@link #LANDING_SEARCH} to the first spot where he fits and stands on something; else the
     * traveller's own landing {@code base} (or the first spot above it where he fits). Never inside a block.
     */
    public static Vec3 landing(ServerLevel level, Entity e, Vec3 want, Vec3 base) {
        level.getChunkAt(BlockPos.containing(want));
        for (int i = 0; i <= 2 * LANDING_SEARCH; i++) {
            int dy = (i + 1) / 2 * (i % 2 == 0 ? -1 : 1);
            Vec3 at = new Vec3(want.x, Math.floor(want.y + 0.5) + dy, want.z);
            if (fits(level, e, at) && standsOn(level, at)) {
                return at;
            }
        }
        for (int up = 0; up <= PlaceBook.ARRIVAL_SEARCH && base.y + up < level.getMaxBuildHeight(); up++) {
            Vec3 at = base.add(0, up, 0);
            if (fits(level, e, at)) {
                return at;
            }
        }
        return base;
    }

    private static boolean fits(ServerLevel level, Entity e, Vec3 at) {
        Pose pose = e instanceof Player ? Pose.STANDING : e.getPose();
        return level.noCollision(e, e.getDimensions(pose).makeBoundingBox(at));
    }

    private static boolean standsOn(ServerLevel level, Vec3 at) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    /** Whether the Bifrost is moving this entity right now (the gates of Asgard let it through). */
    public static boolean isMoving(Entity entity) {
        return MOVING.contains(entity.getUUID());
    }

    /** Whether the Bifrost is gathering round this player. */
    public static boolean inBifrost(Player player) {
        return PlaceBook.journey(player, PlaceBook.Kind.BIFROST) != null;
    }

    // ---- the runes ----

    /** The Bifrost's circle of runes, burned into the ground (seen by everyone nearby, fading after a minute). */
    public static void mark(ServerLevel level, Vec3 at) {
        Vec3 ground = ground(level, at.add(0, 0.5, 0));
        PacketDistributor.sendToPlayersNear(level, null, ground.x, ground.y, ground.z, 128,
                new ThorFxPayload(ThorFxPayload.BIFROST_MARK, -1, List.of(ground)));
    }

    /** The first solid ground at or below a point (up to 32 blocks down). */
    private static Vec3 ground(ServerLevel level, Vec3 point) {
        BlockHitResult down = level.clip(new ClipContext(point, point.subtract(0, 32, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        return down.getType() == HitResult.Type.MISS ? point : down.getLocation();
    }

    /** Where a traveller comes to Asgard from: remembered so that the Heimdall NPC can send him back (lane L1 sets it). */
    public static void rememberCameFrom(ServerPlayer player, GlobalPos from) {
        player.setData(ModAttachments.ASGARD, player.getData(ModAttachments.ASGARD).withCameFrom(from));
    }

    /** The traveller logged out or died: his circle is forgotten (PlaceBook cancels the journey itself). */
    public static void forget(Player player) {
        ORIGINS.remove(player.getUUID());
    }

    public static void clear() {
        ORIGINS.clear();
        MOVING.clear();
    }

    private Bifrost() {
    }
}

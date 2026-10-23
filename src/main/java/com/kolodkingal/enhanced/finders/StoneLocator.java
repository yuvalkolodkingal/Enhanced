package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.config.LocatorConfig;
import com.kolodkingal.enhanced.config.StonesConfig.Uniqueness;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneUse;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The Stone Locator at work (3.7, lane L5; the item is {@code item/StoneLocatorItem}). Every 20 ticks, while a Locator
 * is in a hand, the server reads where it should point ({@link #read}) and writes it on the stack's {@code
 * enhanced:locator} component, only when it changed (no inventory traffic for a needle that holds still).
 *
 * <p><b>What it looks for</b>: the stones its holder has not got ({@link #need}): one he carries (bare, in a crafted
 * Gauntlet, in its vessel, the Orb too) is got; so is a site stone the ledger will not give him (he took it before, or
 * holds it in {@code world} mode); a site stone someone else holds in {@code world} mode is skipped ("Someone else
 * holds the Time Stone."). Of the kinds he lacks (or the one the filter names: Sneak + use cycles it), it points to the
 * nearest site, or way to its realm ({@link LocatorRoutes}). <b>Use</b>: a spark trail in the stone's colour toward the
 * target ({@code StoneFxPayload.LOCATOR_TRAIL}, everyone near sees it), cooldown {@code pulseCooldown}.
 */
public final class StoneLocator {
    /** How often a held Locator is read. */
    public static final int READ_TICKS = 20;
    /** How far the spark trail flies (blocks), and in how many points. */
    public static final double TRAIL_LENGTH = 12.0;
    public static final int TRAIL_POINTS = 12;

    /** Whether a kind is got, lacking, or held by someone else (skipped). */
    public enum Need {
        GOT, LACKS, SOMEONE_ELSE
    }

    /** When each player's last spark trail flew (game time). */
    private static final Map<UUID, Long> PULSED = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // What he has

    /**
     * Whether {@code kind} is got, lacking or held by someone else, from the facts: whether he carries it, whether it
     * is a site stone the ledger counts, whether the ledger lets him take it, its mode and whether he is its holder.
     */
    public static Need need(boolean carries, boolean siteStone, boolean mayTake, Uniqueness mode, boolean holderIsHim) {
        if (carries) {
            return Need.GOT;
        }
        if (!siteStone || mayTake) {
            return Need.LACKS;
        }
        return mode == Uniqueness.WORLD && !holderIsHim ? Need.SOMEONE_ELSE : Need.GOT;
    }

    /** Whether {@code kind} is got, lacking or held by someone else, for {@code player}. */
    public static Need need(ServerPlayer player, StoneKind kind) {
        boolean site = StoneLedger.isSiteStone(kind);
        boolean mayTake = !site || StoneLedger.mayTake(player, kind);
        UUID holder = site ? StoneLedger.holder(player.server, kind) : null;
        return need(carries(player, kind), site, mayTake, StoneLedger.mode(kind), player.getUUID().equals(holder));
    }

    /**
     * Whether the player carries the stone in any form, whether or not he may use it: the bare stone, a crafted
     * Gauntlet holding it, its vessel (Loki's Scepter, the Tesseract, the Eye of Agamotto) or, for the Power Stone, the
     * Orb.
     */
    public static boolean carries(Player player, StoneKind kind) {
        for (ItemStack stack : player.getInventory().items) {
            if (holds(stack, kind)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (holds(stack, kind)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a stack holds the stone (bare, in a crafted Gauntlet, in its vessel, the Orb). */
    public static boolean holds(ItemStack stack, StoneKind kind) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ModItems.stone(kind))) {
            return true;
        }
        Item vessel = StoneUse.vessel(kind);
        if (vessel != null && stack.is(vessel) || kind == StoneKind.POWER && stack.is(ModItems.THE_ORB.get())) {
            return true;
        }
        GauntletContents gauntlet = stack.get(ModDataComponents.GAUNTLET.get());
        return stack.is(ModItems.INFINITY_GAUNTLET.get()) && gauntlet != null && !gauntlet.titan()
                && gauntlet.has(kind);
    }

    /** The kinds the filter may name now: the ones he lacks, in the stones' order. */
    public static List<StoneKind> lacking(ServerPlayer player) {
        List<StoneKind> kinds = new ArrayList<>();
        for (StoneKind kind : StoneKind.values()) {
            if (need(player, kind) == Need.LACKS) {
                kinds.add(kind);
            }
        }
        return kinds;
    }

    /** The filter after {@code current}: every stone (empty), each lacking kind in turn, then every stone again. */
    public static Optional<StoneKind> nextFilter(Optional<StoneKind> current, List<StoneKind> lacking) {
        if (lacking.isEmpty()) {
            return Optional.empty();
        }
        if (current.isEmpty()) {
            return Optional.of(lacking.get(0));
        }
        int at = lacking.indexOf(current.get());
        if (at < 0) {
            for (StoneKind kind : lacking) {
                if (kind.ordinal() > current.get().ordinal()) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }
        return at + 1 < lacking.size() ? Optional.of(lacking.get(at + 1)) : Optional.empty();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Where it points

    /** One kind's reading: where (if anywhere), what the HUD says. */
    private record Reading(StoneKind kind, @Nullable GlobalPos target, byte status) {
    }

    /** The Locator's state for {@code player} now, from {@code state} (its tuning and filter kept). */
    public static LocatorState read(ServerPlayer player, LocatorState state) {
        Map<StoneKind, Need> needs = new java.util.EnumMap<>(StoneKind.class);
        for (StoneKind kind : StoneKind.values()) {
            needs.put(kind, need(player, kind));
        }
        Optional<StoneKind> filter = state.filter();
        if (filter.isPresent() && needs.get(filter.get()) == Need.GOT) {
            filter = Optional.empty(); // found it: back to every stone
        }
        LocatorState base = state.withFilter(filter);
        if (filter.isPresent() && needs.get(filter.get()) == Need.SOMEONE_ELSE) {
            return base.withReading(Optional.empty(), LocatorState.SOMEONE_ELSE, 0, filter);
        }
        List<Reading> readings = new ArrayList<>();
        StoneKind someoneElse = null;
        for (StoneKind kind : StoneKind.values()) {
            if (filter.isPresent() && filter.get() != kind) {
                continue;
            }
            Need need = needs.get(kind);
            if (need == Need.LACKS) {
                readings.add(reading(player, kind));
            } else if (need == Need.SOMEONE_ELSE && someoneElse == null) {
                someoneElse = kind;
            }
        }
        if (readings.isEmpty()) {
            return someoneElse != null
                    ? base.withReading(Optional.empty(), LocatorState.SOMEONE_ELSE, 0, Optional.of(someoneElse))
                    : base.withReading(Optional.empty(), LocatorState.HAS_ALL, 0, Optional.empty());
        }
        Reading best = null;
        double bestDist = Double.MAX_VALUE;
        for (Reading reading : readings) {
            if (reading.target() != null) {
                double d = distance(player, reading.target().pos());
                if (d < bestDist) {
                    best = reading;
                    bestDist = d;
                }
            }
        }
        if (best != null) {
            int rounded = (int) (Math.round(bestDist / 10.0) * 10);
            return base.withReading(Optional.of(best.target()), best.status(), rounded, Optional.of(best.kind()));
        }
        Reading shown = first(readings, LocatorState.SEARCHING);
        if (shown == null) {
            shown = first(readings, LocatorState.TOO_FAR);
        }
        if (shown == null) {
            shown = first(readings, LocatorState.HIDDEN);
        }
        if (shown == null) {
            shown = readings.get(0);
        }
        int radius = shown.status() == LocatorState.TOO_FAR ? SiteSearch.radius(player.level().dimension()) : 0;
        Optional<StoneKind> pointing = filter.isPresent() || shown.status() != LocatorState.SEARCHING
                ? Optional.of(shown.kind()) : Optional.empty();
        return base.withReading(Optional.empty(), shown.status(), radius, pointing);
    }

    @Nullable
    private static Reading first(List<Reading> readings, byte status) {
        for (Reading reading : readings) {
            if (reading.status() == status) {
                return reading;
            }
        }
        return null;
    }

    /** One kind's reading in the holder's realm. */
    private static Reading reading(ServerPlayer player, StoneKind kind) {
        LocatorRoutes.Realm realm = LocatorRoutes.realm(player.level().dimension());
        boolean vaultOpen = AsgardState.get(player.server).odinAwake();
        LocatorRoutes.Route route = LocatorRoutes.route(realm, kind, gateways(), vaultOpen);
        switch (route.type()) {
            case FIXED -> {
                return new Reading(kind, GlobalPos.of(player.level().dimension(), CityPlan.REALITY_PEDESTAL),
                        route.status());
            }
            case SEARCH, GATEWAY -> {
                StoneKind spent = route.type() == LocatorRoutes.Type.SEARCH ? kind : null;
                SiteSearch.Answer answer = SiteSearch.ask(player, route.sites(), spent,
                        SiteSearch.radius(player.level().dimension()));
                if (answer.isFound()) {
                    return new Reading(kind, GlobalPos.of(player.level().dimension(), answer.pos().get()),
                            route.status());
                }
                if (route.type() == LocatorRoutes.Type.GATEWAY && answer.status() == LocatorState.TOO_FAR) {
                    return new Reading(kind, null, route.status()); // no way found: the words alone
                }
                return new Reading(kind, null, answer.status());
            }
            default -> {
                return new Reading(kind, null, route.status());
            }
        }
    }

    /** The horizontal distance from the player to a point (blocks). */
    private static double distance(Player player, BlockPos pos) {
        double dx = pos.getX() + 0.5 - player.getX();
        double dz = pos.getZ() + 0.5 - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The server's side

    /** A Locator in a hand, every tick (server): every {@link #READ_TICKS} it is read. */
    public static void tick(ServerPlayer player, ItemStack stack) {
        if ((player.level().getGameTime() + player.getId()) % READ_TICKS != 0 || player.isSpectator()) {
            return;
        }
        update(player, stack);
    }

    /** Reads a Locator and writes what changed (true: something did). */
    public static boolean update(ServerPlayer player, ItemStack stack) {
        LocatorState before = state(stack);
        LocatorState now = read(player, before);
        if (now.equals(before)) {
            return false;
        }
        stack.set(ModDataComponents.LOCATOR.get(), now);
        boolean newSite = now.target().isPresent() && !now.target().equals(before.target());
        if (newSite && now.status() == LocatorState.FOUND) {
            player.playNotifySound(ModSounds.LOCATOR_FOUND.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (now.status() == LocatorState.SEARCHING && before.status() != LocatorState.SEARCHING) {
            player.playNotifySound(ModSounds.LOCATOR_TICK.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
        }
        return true;
    }

    /** What a Locator knows (a stack without the component: nothing). */
    public static LocatorState state(ItemStack stack) {
        LocatorState state = stack.get(ModDataComponents.LOCATOR.get());
        return state == null ? LocatorState.EMPTY : state;
    }

    /** Sneak + use: the filter moves on, the holder is told, the needle turns at once. */
    public static void cycle(ServerPlayer player, ItemStack stack) {
        LocatorState state = state(stack);
        Optional<StoneKind> next = nextFilter(state.filter(), lacking(player));
        stack.set(ModDataComponents.LOCATOR.get(), state.withFilter(next));
        update(player, stack);
        player.displayClientMessage(next.isPresent()
                ? Component.translatable("message.enhanced.locator.filter", next.get().displayName())
                .withStyle(s -> s.withColor(next.get().color()))
                : Component.translatable("message.enhanced.locator.filter.all"), true);
        player.playNotifySound(ModSounds.LOCATOR_TICK.get(), SoundSource.PLAYERS, 0.7F, 1.3F);
    }

    /**
     * Use: the spark trail flies {@link #TRAIL_LENGTH} blocks toward the target in the stone's colour (a ring round him
     * when it spins), with a hum; false while it cools down ({@code pulseCooldown}).
     */
    public static boolean pulse(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        Long last = PULSED.get(player.getUUID());
        if (last != null && now - last < config(LocatorConfig.PULSE_COOLDOWN, 100) && now >= last) {
            return false;
        }
        update(player, stack);
        LocatorState state = state(stack);
        PULSED.put(player.getUUID(), now);
        StoneKind kind = state.pointing().orElse(state.tuned().orElse(StoneKind.SPACE));
        Vec3 from = player.getEyePosition().add(0.0, -0.4, 0.0);
        List<Vec3> points = trail(from, state.target()
                .filter(t -> t.dimension() == player.level().dimension()).map(GlobalPos::pos).orElse(null));
        Net.sendToPlayersTrackingEntityAndSelf(player, new StoneFxPayload(StoneFxPayload.LOCATOR_TRAIL,
                player.getId(), points, kind.ordinal()));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOCATOR_PULSE.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    /**
     * The spark trail's points: {@link #TRAIL_POINTS} from {@code from} along the level toward {@code target}, rising
     * a little at the end; no target: a ring of points round him (it spins).
     */
    public static List<Vec3> trail(Vec3 from, @Nullable BlockPos target) {
        List<Vec3> points = new ArrayList<>();
        if (target == null) {
            for (int i = 0; i < TRAIL_POINTS; i++) {
                double a = i * Math.PI * 2.0 / TRAIL_POINTS;
                points.add(from.add(Math.cos(a) * 1.5, 0.0, Math.sin(a) * 1.5));
            }
            return points;
        }
        Vec3 flat = new Vec3(target.getX() + 0.5 - from.x, 0.0, target.getZ() + 0.5 - from.z);
        Vec3 dir = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : flat.normalize();
        double length = Math.min(TRAIL_LENGTH, Math.max(2.0, flat.length()));
        for (int i = 1; i <= TRAIL_POINTS; i++) {
            double f = i / (double) TRAIL_POINTS;
            points.add(from.add(dir.scale(length * f)).add(0.0, Math.sin(f * Math.PI) * 0.8, 0.0));
        }
        return points;
    }

    /** The player left, or the server stopped. */
    public static void forget(UUID player) {
        PULSED.remove(player);
    }

    public static void clear() {
        PULSED.clear();
    }

    /** The kind a recipe's core tunes the Locator to (null: not a core it takes). */
    @Nullable
    public static StoneKind coreKind(ItemStack core) {
        for (StoneKind kind : StoneKind.values()) {
            if (core.is(ModItems.stone(kind))) {
                return kind;
            }
        }
        if (core.is(ModItems.LOKIS_SCEPTER.get())) {
            return StoneKind.MIND;
        }
        if (core.is(ModItems.TESSERACT.get())) {
            return StoneKind.SPACE;
        }
        if (core.is(ModItems.EYE_OF_AGAMOTTO.get())) {
            return StoneKind.TIME;
        }
        if (core.is(ModItems.THE_ORB.get())) {
            return StoneKind.POWER;
        }
        if (core.is(ModItems.INFINITY_GAUNTLET.get())) {
            GauntletContents contents = core.get(ModDataComponents.GAUNTLET.get());
            return contents == null || contents.titan() || contents.count() == 0 ? null
                    : InfinityGauntletItem.selectedOrFirst(contents);
        }
        return null;
    }

    private static boolean gateways() {
        return !EnhancedConfig.SPEC.isLoaded() || LocatorConfig.POINTS_TO_GATEWAYS.get();
    }

    private static int config(net.neoforged.neoforge.common.ModConfigSpec.IntValue value, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    private StoneLocator() {
    }
}

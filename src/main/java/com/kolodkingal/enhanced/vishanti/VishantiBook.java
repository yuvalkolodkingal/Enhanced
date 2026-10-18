package com.kolodkingal.enhanced.vishanti;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.client.screen.VishantiScreen;
import com.kolodkingal.enhanced.config.VishantiConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/**
 * The Book of the Vishanti in use (3.2). Only a sorcerer, Loki, the Scarlet Witch or Billy, can read it: it saves places (the
 * book's own list, the Bifrost's {@link PlaceBook} service with its own cooldown) and teleports to them, in any dimension;
 * and a place saved as a <em>base</em> carries a ward ({@link WardManager}) as large as the sorcerer is strong
 * ({@link WardStrength}). The teleport obeys the rules of every teleport of the mod ({@code HexManager.onTeleport}: a boss
 * arena keeps its fighters in, a Hex wall is judged, a pinned creature stays) and never leaves, enters or saves in the
 * Witches' Road, nor in a dimension of {@code vishanti.deniedDimensions}. Use opens the book (the screen); Sneak + use goes to
 * the first base.
 */
public final class VishantiBook {
    /** The dimension of the Witches' Road (3.2): the book is shut there whatever the config says. */
    public static final String ROAD = "enhanced:witches_road";

    // ---- the item ----

    public static boolean isBook(ItemStack stack) {
        return stack.is(ModItems.BOOK_OF_THE_VISHANTI.get());
    }

    /** The book in either hand. */
    public static boolean holds(LivingEntity entity) {
        return isBook(entity.getMainHandItem()) || isBook(entity.getOffhandItem());
    }

    /**
     * Using the book (right-click). A sorcerer: the screen opens on the client (the saved places, the bases and their wards);
     * with Sneak the book goes to the first base. Anyone else: the runes swim before their eyes.
     */
    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            if (readable(server, true) && server.isShiftKeyDown()) {
                travelToBase(server);
            }
        } else if (level.isClientSide && !player.isShiftKeyDown() && WardStrength.isSorcerer(player) && enabled() && !HexRunes.silences(player)) {
            // Client only: this branch never runs on a dedicated server, so the screen class is never loaded there.
            VishantiScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // ---- the rules ----

    /** The book is on ({@code vishanti.enabled}; before the server's config has reached a client it is). */
    private static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || VishantiConfig.ENABLED.get();
    }

    /** Whether the player can read the book: the book is on and they are a sorcerer. Says why not (in the action bar) if {@code tell}. */
    public static boolean readable(ServerPlayer player, boolean tell) {
        if (!enabled()) {
            say(player, tell, "message.enhanced.vishanti.disabled");
            return false;
        }
        if (!WardStrength.isSorcerer(player)) {
            say(player, tell, "message.enhanced.vishanti.not_sorcerer");
            return false;
        }
        if (HexRunes.silences(player)) {
            // 3.3: the runes on the walls of a Hex hold the book shut
            say(player, tell, "message.enhanced.hex_runes.silenced");
            return false;
        }
        return true;
    }

    private static void say(ServerPlayer player, boolean tell, String key, Object... args) {
        if (tell) {
            player.displayClientMessage(Component.translatable(key, args), true);
        }
    }

    /** Whether the book is shut in this dimension: the Witches' Road, or one of {@code vishanti.deniedDimensions}. */
    public static boolean shutIn(ResourceKey<Level> dimension) {
        String id = dimension.location().toString();
        return id.equals(ROAD) || VishantiConfig.DENIED_DIMENSIONS.get().contains(id);
    }

    /** The book can open a way for this player now: in hand, read by a sorcerer, here not shut, and gathered again. */
    private static boolean mayTravel(ServerPlayer player) {
        if (player.isSpectator() || PlaceBook.travelling(player)) {
            return false;
        }
        if (MjolnirPin.isPinned(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.mjolnir.pinned_cannot"), true);
            return false;
        }
        if (!holds(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.needs_book"), true);
            return false;
        }
        if (!readable(player, true)) {
            return false;
        }
        if (shutIn(player.level().dimension())) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.shut"), true);
            return false;
        }
        long wait = PlaceBook.cooldown(player, PlaceBook.Kind.VISHANTI);
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.not_ready", (wait + 19) / 20), true);
            return false;
        }
        return true;
    }

    // ---- teleporting ----

    /** The way to the saved place at {@code index} (if it is still the one called {@code name}), in whatever dimension it is. */
    public static void travelToPlace(ServerPlayer player, int index, String name) {
        BifrostData.Place place = PlaceBook.placeAt(player, PlaceBook.Kind.VISHANTI, index, name);
        if (place != null && mayTravel(player)) {
            open(player, place);
        }
    }

    /** The way to the first base of the book. */
    public static void travelToBase(ServerPlayer player) {
        if (!mayTravel(player)) {
            return;
        }
        for (BifrostData.Place place : PlaceBook.Kind.VISHANTI.data(player).places()) {
            if (place.base()) {
                open(player, place);
                return;
            }
        }
        player.displayClientMessage(Component.translatable("message.enhanced.vishanti.no_base"), true);
    }

    private static void open(ServerPlayer player, BifrostData.Place place) {
        ServerLevel destination = player.server.getLevel(place.dimension());
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.no_world"), true);
            return;
        }
        if (shutIn(place.dimension())) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.shut_there"), true);
            return;
        }
        // The arena of a boss fight keeps its fighters in (asked again when the way opens: the fight may have begun since).
        if (!BossFights.allowsTeleport(destination, player, place.pos())) {
            BossFights.tellHeld(player);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        PlaceBook.readyAt(player, PlaceBook.Kind.VISHANTI, now + VishantiConfig.TELEPORT_COOLDOWN.get());
        PlaceBook.begin(player, PlaceBook.Kind.VISHANTI, place.dimension(), place.pos(), place.yaw(), VishantiConfig.TELEPORT_CHARGE_TICKS.get());
        pay(player, VishantiConfig.TELEPORT_EXHAUSTION.get());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("message.enhanced.vishanti.opening", place.name()), true);
    }

    private static void pay(ServerPlayer player, double exhaustion) {
        if (!player.isCreative()) {
            player.causeFoodExhaustion((float) exhaustion);
        }
    }

    // ---- saving, and bases ----

    /**
     * Saves where the player stands, facing the way they face. A base also raises a ward round the spot, as large as the
     * sorcerer is strong, unless they have as many as {@code vishanti.maxWardsPerPlayer} bases already (moving one, saving its
     * name again, is always allowed) or the ward would reach into a boss arena.
     */
    public static void savePlace(ServerPlayer player, String typed, boolean base) {
        if (player.isSpectator()) {
            return;
        }
        if (!holds(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.needs_book"), true);
            return;
        }
        if (!readable(player, true)) {
            return;
        }
        if (shutIn(player.level().dimension())) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.shut"), true);
            return;
        }
        // Saving the name of a base as a plain place would take its ward away without a word: it moves the base instead.
        base |= namesABase(player, typed);
        if (base && !mayRaiseWard(player, typed)) {
            return;
        }
        BifrostData.Place place = PlaceBook.save(player, PlaceBook.Kind.VISHANTI, typed, base);
        if (place == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.full", BifrostData.MAX_PLACES), true);
            return;
        }
        // Saving a name again moves it, a base that is saved as a plain place loses its ward: the wards follow the book.
        WardManager.refresh(player);
        ServerLevel level = player.serverLevel();
        int color = Powers.power(player).color;
        flare(level, player.position(), color, 1.2, 28);
        if (base) {
            WardManager.startCooldown(player);
            pay(player, VishantiConfig.WARD_EXHAUSTION.get());
            flare(level, player.position(), color, WardStrength.radiusOf(player), 96);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.4F);
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.ward_raised", place.name(),
                    WardStrength.radiusOf(player)), true);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.2F);
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.saved", place.name()), true);
        }
    }

    /** Whether {@code typed} names (ignoring case) a base of the player's book. */
    private static boolean namesABase(ServerPlayer player, String typed) {
        BlockPos at = player.blockPosition();
        String name = BifrostData.cleanName(typed, at.getX() + ", " + at.getY() + ", " + at.getZ());
        for (BifrostData.Place place : PlaceBook.Kind.VISHANTI.data(player).places()) {
            if (place.base() && place.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a ward may be raised where the player stands now (and says why not). */
    private static boolean mayRaiseWard(ServerPlayer player, String typed) {
        int radius = WardStrength.radiusOf(player);
        if (radius <= 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.ward_off"), true);
            return false;
        }
        BifrostData book = PlaceBook.Kind.VISHANTI.data(player);
        BlockPos at = player.blockPosition();
        String name = BifrostData.cleanName(typed, at.getX() + ", " + at.getY() + ", " + at.getZ());
        boolean moving = false;
        for (BifrostData.Place place : book.places()) {
            moving |= place.base() && place.name().equalsIgnoreCase(name);
        }
        if (!moving && book.bases() >= VishantiConfig.MAX_WARDS.get()) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.ward_max", VishantiConfig.MAX_WARDS.get()), true);
            return false;
        }
        long wait = WardManager.cooldown(player);
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.ward_wait", (wait + 19) / 20), true);
            return false;
        }
        BoundingBox reach = BoundingBox.fromCorners(at.offset(-radius, -radius, -radius), at.offset(radius, radius, radius));
        if (BossFights.overlapsAny(player.serverLevel(), reach)) {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.ward_arena"), true);
            return false;
        }
        return true;
    }

    /** Forgets the saved place at {@code index}, if it is still the one called {@code name}; a base takes its ward with it. */
    public static void forgetPlace(ServerPlayer player, int index, String name) {
        BifrostData.Place place = PlaceBook.forget(player, PlaceBook.Kind.VISHANTI, index, name);
        if (place == null) {
            return;
        }
        if (place.base()) {
            WardManager.refresh(player);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 1.0F, 1.4F);
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.ward_removed", place.name()), true);
        } else {
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.forgotten", place.name()), true);
        }
    }

    // ---- every tick ----

    /** Every player, once a tick: the book gathers its light, and later takes its traveller; a sorcerer's wards stay as strong as they are. */
    public static void everyPlayerTick(ServerPlayer player) {
        PlaceBook.Journey journey = PlaceBook.journey(player, PlaceBook.Kind.VISHANTI);
        if (journey != null) {
            ServerLevel level = player.serverLevel();
            if (PlaceBook.waiting(player, journey)) {
                if ((level.getGameTime() & 1L) == 0L) {
                    gather(level, player, journey);
                }
            } else {
                arrive(player, journey);
            }
        }
        if (player.tickCount % 100 == 11 && PlaceBook.Kind.VISHANTI.data(player).bases() > 0) {
            WardManager.refresh(player);
        }
        if (player.tickCount % 200 == 37) {
            // The clients are told of the wards near them: tell again as the player walks about.
            WardManager.send(player);
        }
    }

    private static void arrive(ServerPlayer player, PlaceBook.Journey journey) {
        int color = Powers.power(player).color;
        PlaceBook.Landing landing = PlaceBook.complete(player, journey);
        if (landing == null) {
            // Nothing happened (the world is gone, or a teleport rule refused): the book is ready again at once.
            PlaceBook.readyAt(player, PlaceBook.Kind.VISHANTI, player.level().getGameTime());
            player.displayClientMessage(Component.translatable("message.enhanced.vishanti.closed"), true);
            return;
        }
        flare(landing.from(), landing.left(), color, 1.4, 48);
        ServerLevel destination = landing.to();
        flare(destination, player.position(), color, 1.4, 48);
        destination.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.2F, 1.0F);
    }

    /** The sparks of the sorcerer's colour circle the traveller, closing in. */
    private static void gather(ServerLevel level, ServerPlayer player, PlaceBook.Journey journey) {
        int charge = Math.max(1, VishantiConfig.TELEPORT_CHARGE_TICKS.get());
        double left = Mth.clamp((journey.arrivesAt() - level.getGameTime()) / (double) charge, 0.0, 1.0);
        double radius = 0.4 + 1.4 * left;
        int color = Powers.power(player).color;
        double turn = level.getGameTime() * 0.5;
        for (int i = 0; i < 6; i++) {
            double angle = turn + i * Mth.TWO_PI / 6.0;
            level.sendParticles(new EnergyParticleOptions(color, 1.0F), player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.1 + (1.0 - left) * 1.6, player.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** A ring of sparks on the ground round {@code at}. */
    static void flare(ServerLevel level, Vec3 at, int color, double radius, int count) {
        EnergyParticleOptions spark = new EnergyParticleOptions(color, 1.3F);
        for (int i = 0; i < count; i++) {
            double angle = i * Mth.TWO_PI / count;
            level.sendParticles(spark, at.x + Math.cos(angle) * radius, at.y + 0.1, at.z + Math.sin(angle) * radius, 1, 0, 0.15, 0, 0.05);
        }
    }

    private VishantiBook() {
    }
}

package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.network.SlingRingActionPayload;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.RoadDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Sling Ring in use (3.6), and the server side of its screen ({@code SlingRingActionPayload} calls {@link #handle}).
 * Only a sorcerer ({@code Traits}: Doctor Strange, Billy, Agatha, Loki, the Scarlet Witch, Odin, a custom hero with a
 * sorcerer's row) can use it: it keeps up to {@code slingRing.places} places (the PlaceBook kind {@code SLING_RING}, its
 * own list and cooldown), and choosing one opens a pair of Sling-Ring portals ({@link SlingPortals}) before him whose
 * twin stands at the place, in another dimension too ({@code slingRing.crossDimension}), for {@code portalSeconds}; then
 * the ring rests {@code slingRing.cooldown}. Never to or from Asgard (the Bifrost's alone) nor the Witches' Road; the
 * Hex Runes silence it; Mjolnir's pin holds it. For anyone else the ring is just a ring.
 */
public final class SlingRingActions {

    public static int places() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.RING_PLACES.get(), 8);
    }

    public static int cooldown() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.RING_COOLDOWN.get(), 200);
    }

    static boolean crossDimension() {
        return StrangeAbilities.cfgBool(() -> StrangeConfig.RING_CROSS_DIMENSION.get(), true);
    }

    /** Whether this player is a sorcerer the ring answers (both sides: the client opens the screen by it). */
    public static boolean isSorcerer(Player player) {
        return Traits.has(player, Trait.SORCERER);
    }

    /** Whether he carries a Sling Ring (anywhere in his inventory). */
    public static boolean carries(Player player) {
        if (player.getOffhandItem().is(ModItems.SLING_RING.get())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.SLING_RING.get())) {
                return true;
            }
        }
        return false;
    }

    /** Whether the ring answers him (a sorcerer the runes do not silence); says why not if {@code tell}. */
    public static boolean wears(ServerPlayer player, boolean tell) {
        if (!isSorcerer(player)) {
            if (tell) {
                player.displayClientMessage(Component.translatable("message.enhanced.sling_ring.not_sorcerer")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        if (HexRunes.silences(player)) {
            if (tell) {
                StrangeAbilities.say(player, "message.enhanced.hex_runes.silenced");
            }
            return false;
        }
        return true;
    }

    /** A button of the ring's screen. */
    public static void handle(ServerPlayer player, int action, int index, String name) {
        if (!carries(player)) {
            StrangeAbilities.say(player, "message.enhanced.sling_ring.needs_ring");
            return;
        }
        if (!wears(player, true)) {
            return;
        }
        switch (action) {
            case SlingRingActionPayload.TRAVEL -> travel(player, index, name);
            case SlingRingActionPayload.SAVE -> save(player, name);
            case SlingRingActionPayload.FORGET -> forget(player, index, name);
            default -> {
            }
        }
    }

    /** Saves where he stands as {@code typed} (a blank name: its coordinates). Refused in Asgard and on the Road. */
    public static void save(ServerPlayer player, String typed) {
        if (PlaceBook.refusesAsgard(player, PlaceBook.Kind.SLING_RING, player.level().dimension())) {
            return;
        }
        if (RoadDimension.in(player)) {
            StrangeAbilities.say(player, "message.enhanced.sling_ring.shut");
            return;
        }
        BifrostData book = PlaceBook.Kind.SLING_RING.data(player);
        String clean = BifrostData.cleanName(typed, "");
        boolean renaming = !clean.isEmpty() && book.places().stream().anyMatch(p -> p.name().equals(clean));
        if (!renaming && book.places().size() >= places()) {
            StrangeAbilities.say(player, "message.enhanced.sling_ring.full", places());
            return;
        }
        BifrostData.Place place = PlaceBook.save(player, PlaceBook.Kind.SLING_RING, typed, false);
        if (place == null) {
            StrangeAbilities.say(player, "message.enhanced.sling_ring.full", places());
            return;
        }
        StrangeAbilities.sound(player, ModSounds.SLING_RING_SAVE.get(), 1.0F, 1.0F);
        StrangeAbilities.say(player, "message.enhanced.sling_ring.saved", place.name());
    }

    static void forget(ServerPlayer player, int index, String name) {
        BifrostData.Place place = PlaceBook.forget(player, PlaceBook.Kind.SLING_RING, index, name);
        if (place != null) {
            StrangeAbilities.say(player, "message.enhanced.sling_ring.forgotten", place.name());
        }
    }

    /** Why the ring cannot open a way to {@code place} now (a lang key, the refusal told), or null. */
    @Nullable
    static String refusal(ServerPlayer player, BifrostData.Place place) {
        if (player.isSpectator() || PlaceBook.travelling(player)) {
            return "message.enhanced.sling_ring.not_now";
        }
        if (MjolnirPin.isPinned(player)) {
            return "message.enhanced.mjolnir.pinned_cannot";
        }
        if (RoadDimension.in(player) || place.dimension() == RoadDimension.KEY) {
            return "message.enhanced.sling_ring.shut";
        }
        if (place.dimension() != player.level().dimension() && !crossDimension()) {
            return "message.enhanced.sling_ring.same_world";
        }
        return null;
    }

    /** Opens a pair of portals to the place {@code index} (if it is still called {@code name}). */
    static void travel(ServerPlayer player, int index, String name) {
        BifrostData.Place place = PlaceBook.placeAt(player, PlaceBook.Kind.SLING_RING, index, name);
        if (place == null) {
            return;
        }
        if (PlaceBook.refusesAsgard(player, PlaceBook.Kind.SLING_RING, place.dimension())) {
            return;
        }
        String refusal = refusal(player, place);
        if (refusal != null) {
            StrangeAbilities.say(player, refusal);
            return;
        }
        long wait = PlaceBook.cooldown(player, PlaceBook.Kind.SLING_RING);
        if (wait > 0) {
            StrangeAbilities.say(player, "message.enhanced.sling_ring.not_ready", (wait + 19) / 20);
            return;
        }
        ServerLevel there = player.server.getLevel(place.dimension());
        if (there == null) {
            StrangeAbilities.say(player, "message.enhanced.sling_ring.no_world");
            return;
        }
        ServerLevel here = player.serverLevel();
        Vec3 near = SlingPortals.nearFeet(player);
        if (!SlingPortals.roomFor(here, near)) {
            StrangeAbilities.say(player, "message.enhanced.strange.portal_no_room");
            return;
        }
        Vec3 far = SlingPortals.fit(there, player, place.pos());
        SlingPortals.open(player, here, near, player.getYRot(), there, far == null ? place.pos() : far, place.yaw());
        PlaceBook.readyAt(player, PlaceBook.Kind.SLING_RING, player.level().getGameTime() + cooldown());
        StrangeAbilities.say(player, "message.enhanced.sling_ring.opening", place.name());
    }

    private SlingRingActions() {
    }
}

package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom heroes (3.3): reading a player's definition and state on either side, and, on the server, making a player the
 * hero he designed or one of the mod's heroes (the Hero Creator's two buttons). {@code Power.CUSTOM} is driven by the
 * definition in {@link ModAttachments#CUSTOM_HERO}; its powers are the {@link ModuleLogic}s of {@link CustomHeroServer}.
 */
public final class CustomHeroes {
    /** When each player last became a hero through the creator (game time), for {@code custom_hero.applyCooldownTicks}. */
    private static final Map<UUID, Long> LAST_APPLY = new HashMap<>();
    /** Whose powers Power Absorption has taken away, until when (game time). */
    private static final Map<UUID, Long> SUPPRESSED = new HashMap<>();
    /** Heroes a power of their own threw into the air (a leap, an updraft, a teleport): no fall damage until when. */
    private static final Map<UUID, Long> FALL_GUARD = new HashMap<>();

    // ---- reading (both sides) ----

    public static CustomHero hero(Player player) {
        return player.getData(ModAttachments.CUSTOM_HERO);
    }

    public static CustomState state(Player player) {
        return player.getData(ModAttachments.CUSTOM_STATE);
    }

    /** Whether the player is a custom hero now. */
    public static boolean is(Player player) {
        return Powers.power(player) == Power.CUSTOM;
    }

    /** The player's pick of this power while he is a custom hero with it, or null. */
    @Nullable
    public static CustomHero.Pick pick(Player player, PowerModule module) {
        return is(player) ? hero(player).pick(module) : null;
    }

    /** Whether the player is a custom hero with this power switched on (or its key held). */
    public static boolean isOn(Player player, PowerModule module) {
        return pick(player, module) != null && state(player).isOn(module);
    }

    /** Whether he has this power picked in this option. */
    public static boolean has(Player player, PowerModule module, String option) {
        CustomHero.Pick pick = pick(player, module);
        return pick != null && (pick.option().equals(option)
                || module.option(pick.option()) == null && module.defaultOption().id().equals(option));
    }

    /** A custom hero passing through walls (Phasing's "phase", switched on): the rules of Vision's phasing apply to him. */
    public static boolean isPhased(Player player) {
        return isOn(player, PowerModule.PHASING) && has(player, PowerModule.PHASING, "phase");
    }

    /** Whether a custom hero may fly: he has Flight, or floats while he phases. */
    public static boolean canFly(Player player) {
        return pick(player, PowerModule.FLIGHT) != null || isPhased(player);
    }

    /** His speed in the air: Flight's at its strength (a phasing hero without Flight drifts at the speed of strength 1). */
    public static float flightSpeed(Player player) {
        CustomHero.Pick flight = pick(player, PowerModule.FLIGHT);
        return ModuleStats.FLIGHT_SPEED.value(flight == null ? 1 : flight.strength());
    }

    /** The look a custom hero is drawn in: no suit of the mod's (his skin is {@code CustomSkins}'), his two colours. */
    public static Variant variant(Player player) {
        CustomHero hero = hero(player);
        return new Variant("custom", Power.CUSTOM, null, null, hero.slim(), hero.color(), hero.glow());
    }

    /** The hero's name as the HUD and the messages show it: his own, or "Custom Hero" before he has one. */
    public static Component name(Player player) {
        String name = hero(player).name();
        return name.isEmpty() ? Power.CUSTOM.displayName() : Component.literal(name);
    }

    // ---- the creator (server) ----

    /** Why this player may not use the Hero Creator now (a lang key), or null if he may. */
    @Nullable
    public static String refusal(ServerPlayer player) {
        if (EnhancedConfig.SPEC.isLoaded() && !CustomHeroConfig.ENABLED.get()) {
            return "message.enhanced.hero_creator.disabled";
        }
        boolean operator = player.hasPermissions(2);
        boolean operatorsOnly = EnhancedConfig.SPEC.isLoaded() && CustomHeroConfig.OPERATORS_ONLY.get();
        if (!operator && (operatorsOnly || !player.isCreative())) {
            return operatorsOnly ? "message.enhanced.hero_creator.operators_only" : "message.enhanced.hero_creator.creative_only";
        }
        return null;
    }

    /** Whether the creator was used too recently (and says so); records this use otherwise. */
    private static boolean tooSoon(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        int wait = EnhancedConfig.SPEC.isLoaded() ? CustomHeroConfig.APPLY_COOLDOWN.get() : 40;
        Long last = LAST_APPLY.get(player.getUUID());
        if (last != null && now >= last && now - last < wait) {
            player.displayClientMessage(Component.translatable("message.enhanced.hero_creator.wait").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        LAST_APPLY.put(player.getUUID(), now);
        return false;
    }

    /**
     * The creator's "Become this hero!": the definition is judged by the server's rules, saved on the player and he becomes
     * it (a hero he already is is replaced: every power of the old one ends first). What the rules changed is told to him.
     *
     * @return whether he became it
     */
    public static boolean apply(ServerPlayer player, CustomHero wanted) {
        String refused = refusal(player);
        if (refused != null) {
            player.displayClientMessage(Component.translatable(refused).withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (tooSoon(player)) {
            return false;
        }
        CustomHeroRules.Result result = CustomHeroRules.sanitize(wanted, CustomHeroRules.Limits.current());
        become(player, result.hero());
        for (CustomHeroRules.Note note : result.notes()) {
            player.sendSystemMessage(note(note).withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    /**
     * Makes the player this (already judged) custom hero, with the awakening's title, sound and burst in his colours.
     * Used by the creator and by the tests.
     */
    public static void become(ServerPlayer player, CustomHero hero) {
        PowerData data = Powers.get(player);
        if (data.power() == Power.CUSTOM) {
            CustomHeroServer.endPowers(player, data);
        }
        player.setData(ModAttachments.CUSTOM_HERO, hero);
        player.syncData(ModAttachments.CUSTOM_HERO);
        CustomState state = state(player);
        state.reset();
        if (data.power() != Power.CUSTOM) {
            Powers.setPower(player, Power.CUSTOM);
        } else {
            for (int slot = 1; slot <= PowerData.ABILITIES; slot++) {
                data.startCooldown(slot, player.serverLevel().getGameTime(), 0);
            }
            PowerTicker.applyPassives(player, data);
        }
        CustomHeroServer.onBecome(player, hero, state);
        syncState(player);
        Powers.syncIfDirty(player);
        announce(player, hero);
    }

    private static void announce(ServerPlayer player, CustomHero hero) {
        ServerLevel level = player.serverLevel();
        Component title = Component.literal(hero.name()).withStyle(s -> s.withColor(hero.color()).withBold(true));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("power.enhanced.custom.awaken")
                .withStyle(s -> s.withColor(hero.glow()))));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CUSTOM_AWAKEN.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        Powers.burst(level, player.getX(), player.getY() + 1.0, player.getZ(), hero.color(), 120, 1.6);
        Powers.burst(level, player.getX(), player.getY() + 1.0, player.getZ(), hero.glow(), 60, 0.8);
        player.server.getPlayerList().broadcastSystemMessage(Component.translatable("power.enhanced.custom.broadcast",
                player.getDisplayName(), Component.literal(hero.name())).withStyle(s -> s.withColor(hero.color())), false);
    }

    /**
     * The creator's "Choose a hero": the player becomes one of the mod's heroes at once, in the look chosen (an unknown
     * look is the hero's first). {@link Power#NONE} takes every power away.
     */
    public static boolean choose(ServerPlayer player, Power power, String look) {
        String refused = refusal(player);
        if (refused != null) {
            player.displayClientMessage(Component.translatable(refused).withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (power == Power.CUSTOM) {
            return false; // (a custom hero is made with APPLY: its definition comes with it)
        }
        if (power != Power.NONE && EnhancedConfig.SPEC.isLoaded() && !CustomHeroConfig.ALLOW_CHOOSE_EXISTING.get()) {
            player.displayClientMessage(Component.translatable("message.enhanced.hero_creator.choose_disabled").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (tooSoon(player)) {
            return false;
        }
        if (power == Power.NONE) {
            Powers.setPower(player, Power.NONE);
            player.displayClientMessage(Component.translatable("message.enhanced.hero_creator.no_power"), true);
            return true;
        }
        String id = Variants.of(power).stream().anyMatch(v -> v.id().equals(look)) ? look : "";
        Powers.awaken(player, power, id);
        return true;
    }

    /** A note of the rules as a message: arguments that are lang keys of powers are translated. */
    public static MutableComponent note(CustomHeroRules.Note note) {
        Object[] args = note.args().clone();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String s && s.startsWith("power_module.")) {
                args[i] = Component.translatable(s);
            }
        }
        return Component.translatable(note.key(), args);
    }

    // ---- Power Absorption's theft ----

    /** Takes a hero's powers away for {@code ticks}. */
    public static void suppress(ServerPlayer target, int ticks) {
        SUPPRESSED.put(target.getUUID(), target.serverLevel().getGameTime() + ticks);
    }

    /** Whether Power Absorption has taken this player's powers for now (his keys do nothing). */
    public static boolean isSuppressed(ServerPlayer player) {
        Long until = SUPPRESSED.get(player.getUUID());
        if (until == null) {
            return false;
        }
        long now = player.serverLevel().getGameTime();
        if (now >= until || until - now > 20L * 600) {
            SUPPRESSED.remove(player.getUUID());
            return false;
        }
        return true;
    }

    // ---- falls ----

    /** The hero's own power threw him: he takes no fall damage for {@code ticks} (or until he lands, whichever is first). */
    public static void guardFall(ServerPlayer player, int ticks) {
        FALL_GUARD.put(player.getUUID(), player.serverLevel().getGameTime() + ticks);
    }

    /** Whether a fall now is one his own power caused. */
    public static boolean fallGuarded(ServerPlayer player) {
        Long until = FALL_GUARD.get(player.getUUID());
        return until != null && player.serverLevel().getGameTime() <= until && until - player.serverLevel().getGameTime() <= 200;
    }

    /** He landed: the guard ends (called from his tick, a few ticks after it was set, so the jump itself is not a landing). */
    static void landed(ServerPlayer player) {
        Long until = FALL_GUARD.get(player.getUUID());
        if (until != null && player.onGround() && player.serverLevel().getGameTime() < until - 90) {
            FALL_GUARD.remove(player.getUUID());
        }
    }

    // ---- sync ----

    /** Sends the player's state to everyone who sees him, if it changed. */
    public static void syncState(ServerPlayer player) {
        CustomState state = state(player);
        if (state.isDirty()) {
            state.clearDirty();
            player.syncData(ModAttachments.CUSTOM_STATE);
        }
    }

    public static void forget(ServerPlayer player) {
        LAST_APPLY.remove(player.getUUID());
        SUPPRESSED.remove(player.getUUID());
        FALL_GUARD.remove(player.getUUID());
    }

    public static void clear() {
        LAST_APPLY.clear();
        SUPPRESSED.clear();
        FALL_GUARD.clear();
    }

    private CustomHeroes() {
    }
}

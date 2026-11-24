package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.multiverse.Multiverse;
import com.kolodkingal.enhanced.network.UniverseTakeoverPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.ultimate.Ultimate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ultimate Wanda (3.19): the Scarlet Witch as the Sorceress Supreme, the ultimate cosmic being, as Billy the Demiurge is
 * Billy's. A transformation of {@link Power#SCARLET_WITCH} that only a player with Ultimate may take ({@link Ultimate#allowed});
 * Sneak + G (her flight key's Sneak, free before) takes it and lets it go.
 *
 * <p><b>The body.</b> Twenty hearts, a netherite set's armour, toughness and knockback resistance on top of what she wears
 * (her crown's own bonus steps aside: twenty hearts is twenty hearts), every healing of hers twice over ({@link #tick}), a
 * faster flight, and every power of hers {@link #DAMAGE_FACTOR} times as hard, one more for every player whose powers her
 * runes took ({@link #outgoing}).
 *
 * <p><b>Two menus.</b> H switches her keys, as the Demiurge's H switches his magic: the Scarlet Witch's own keys, or her
 * Ultimate menu, where R takes over the universe she stands in (Sneak + R lets every universe go), V takes over a body like
 * Professor X but with no time limit, over any telepath, from 30 blocks (V again comes back; Sneak + V: reading minds as Jean
 * Grey does, in red), and B lights the runes in the skies of her universes (Sneak + B: her Chaos Shield). G and N stay hers.
 * Sneak + H switches her hair: the films' red, or the comics' black.
 *
 * <p><b>Taking a universe.</b> Its sky turns red for everyone in it (their client: {@code UniverseTakeoverClient}, told by
 * {@link UniverseTakeoverPayload} only when what their universe is changes). She may hold any number of universes at once.
 * The runes, lit, burn in the sky over everyone in her universes and hold the powers of every player in them, a witch or not
 * ({@link ModEffects#DRAINED}); each new player they take makes her stronger.
 *
 * <p><b>What cannot touch her.</b> Death: her body comes back in red energy a second later, with all its health ({@link
 * #onDeath}). Nothing freezes her (a stopped time, a spellbinding, petrifying, a lock of the Time Stone, webs, sand, ice).
 * The Hex Runes do not hold her, and every Hex lets her through. No ability of hers waits for a recharge (infinite chaos
 * magic: {@code PowerData.isReady}). While her mind is in another body a shield of red light guards her own.
 *
 * <p>Never saved (as the Demiurge): she logs back in as the plain Scarlet Witch, and the universes she held are let go.
 */
public final class UltimateWanda {
    /** Her red, the colour of every effect of the Ultimate. */
    public static final int RED = 0xFF1E3C;
    /** The pale glow of her energy, and the deep crimson of the sky she brings. */
    public static final int GLOW = 0xFF7A90;
    public static final int DEEP = 0x8A0820;

    /** How many times as hard her powers hit. */
    public static final double DAMAGE_FACTOR = 8.0;
    /** How much harder (in steps of the factor) each player whose powers her runes took makes her, and at most how many count. */
    public static final double STOLEN_STEP = 1.0;
    public static final int STOLEN_MAX = 16;
    /** Her body: twenty hearts and a netherite set. */
    public static final double MAX_HEALTH = 40.0;
    public static final double ARMOR = 20.0;
    public static final double TOUGHNESS = 12.0;
    public static final double KNOCKBACK_RESISTANCE = 0.4;
    /** How much faster than the Scarlet Witch she flies. */
    public static final float FLIGHT_FACTOR = 1.6F;
    /** How far she reaches a body to take it over. */
    public static final double TAKE_OVER_RANGE = 30.0;
    /** How long after her death her body comes back (a second). */
    public static final int REVIVE_TICKS = 20;
    /** How long a hold of the runes lasts past their last touch (renewed every second while they burn). */
    private static final int DRAIN_TICKS = 60;

    /** Her menus (in {@link PowerData#ironCharge}, which the Scarlet Witch has no other use for). */
    public static final int MENU_WITCH = 0;
    public static final int MENU_ULTIMATE = 1;

    private static final ResourceLocation HEALTH = Enhanced.id("ultimate_wanda_health");
    private static final ResourceLocation ARMOR_ID = Enhanced.id("ultimate_wanda_armor");
    private static final ResourceLocation TOUGHNESS_ID = Enhanced.id("ultimate_wanda_toughness");
    private static final ResourceLocation KNOCKBACK = Enhanced.id("ultimate_wanda_knockback");

    // ---- the server's state (forgotten when it stops) ----

    /** The universes taken (by number: {@link Multiverse#universeAt}), and who holds each. */
    private static final Map<Integer, UUID> TAKEN = new HashMap<>();
    /** The universes whose skies carry her runes. */
    private static final Set<Integer> RUNES = new HashSet<>();
    /** The players each Ultimate Wanda's runes have taken (each counted once while she is the Ultimate). */
    private static final Map<UUID, Set<UUID>> STOLEN = new HashMap<>();
    /** When the body of each Ultimate Wanda who died comes back (game time). */
    private static final Map<UUID, Long> REVIVE_AT = new HashMap<>();
    /** Her health at the end of her last tick (to heal twice over what she healed). */
    private static final Map<UUID, Float> LAST_HEALTH = new HashMap<>();
    /** What each player's client was last told of the universe they stand in ({@link UniverseTakeoverPayload}). */
    private static final Map<UUID, Integer> TOLD = new HashMap<>();

    private UltimateWanda() {
    }

    // ---- who ----

    /** Whether this player may be the Ultimate now (she has Ultimate). */
    public static boolean allowed(Player player) {
        return Ultimate.allowed(player);
    }

    /** The same for a holder known only as an object (a client class). */
    public static boolean allowedHolder(Object holder) {
        return Ultimate.allowedHolder(holder);
    }

    /** Whether she is Ultimate Wanda right now (both sides). */
    public static boolean is(PowerData data) {
        return data.power() == Power.SCARLET_WITCH && data.is(Live.WANDA_ULTIMATE);
    }

    public static boolean is(@Nullable Entity entity) {
        return entity instanceof Player player && is(Powers.get(player));
    }

    /** Whether her keys are the Ultimate menu's now. */
    public static boolean ultimateMenu(PowerData data) {
        return is(data) && data.ironCharge() == MENU_ULTIMATE;
    }

    /** Whether she wears the comics' black hair (else the films' red). Kept between sessions. */
    public static boolean comicsHair(PowerData data) {
        return data.toggle(Toggle.WANDA_COMICS_HAIR);
    }

    // ---- the keys ----

    /**
     * A key of the Scarlet Witch's (from {@code AbilityHandler}): Sneak + G becomes the Ultimate or lets it go; while her mind
     * is in a body V brings her back; in the Ultimate menu R, V and B are hers. Returns whether the key was the Ultimate's.
     */
    public static boolean onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (data.power() != Power.SCARLET_WITCH) {
            return false;
        }
        if (ability == 2 && alt) {
            if (down && !player.isSpectator()) {
                toggle(player, data);
            }
            return true;
        }
        if (!is(data)) {
            return false;
        }
        if (MindLink.isLinked(player)) {
            // (away in another body: V brings her back, and her own body does nothing else)
            if (down) {
                if (ability == 3) {
                    MindLink.end(player.server, player.getUUID(), MindLink.End.LET_GO);
                } else {
                    player.displayClientMessage(Component.translatable("message.enhanced.xavier.away"), true);
                }
            }
            return true;
        }
        if (!ultimateMenu(data)) {
            return false;
        }
        switch (ability) {
            case 1 -> {
                if (down) {
                    if (alt) {
                        releaseAll(player.server, player.getUUID(), true);
                    } else {
                        toggleTakeover(player);
                    }
                }
                return true;
            }
            case 3 -> {
                if (down) {
                    if (alt) {
                        toggleMindReading(player, data);
                    } else {
                        takeOver(player);
                    }
                }
                return true;
            }
            case 4 -> {
                if (alt || data.chaosShieldActive()) {
                    return false; // (Sneak + B: her Chaos Shield, and its release, are the Scarlet Witch's own)
                }
                if (down) {
                    toggleRunes(player);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** Sneak + G: the Ultimate, or the Scarlet Witch again. */
    public static void toggle(ServerPlayer player, PowerData data) {
        if (is(data)) {
            revert(player, data, "message.enhanced.ultimate_wanda.off");
            return;
        }
        if (!Ultimate.allowed(player)) {
            say(player, "message.enhanced.ultimate_wanda.no_ultimate");
            return;
        }
        enter(player, data);
    }

    /** She becomes Ultimate Wanda (Ultimate is not checked: the key does). */
    public static void enter(ServerPlayer player, PowerData data) {
        if (data.power() != Power.SCARLET_WITCH) {
            return;
        }
        data.set(Live.WANDA_ULTIMATE, true);
        data.setIronCharge(MENU_ULTIMATE);
        data.markDirty();
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.4F, 1.4F);
        burst(level, player, 90);
        applyBody(player, data);
        player.setHealth(player.getMaxHealth());
        LAST_HEALTH.put(player.getUUID(), player.getHealth());
        player.displayClientMessage(Component.translatable("message.enhanced.ultimate_wanda.on")
                .withStyle(s -> s.withColor(RED).withBold(true)), true);
        Powers.syncIfDirty(player);
    }

    /** She is the Scarlet Witch again (her choice, or Ultimate is gone): her universes are let go, her body is her own. */
    public static void revert(ServerPlayer player, PowerData data, @Nullable String message) {
        if (!data.is(Live.WANDA_ULTIMATE)) {
            return;
        }
        letGo(player, data);
        data.set(Live.WANDA_ULTIMATE, false);
        data.setIronCharge(MENU_WITCH);
        data.markDirty();
        applyBody(player, data);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.9F);
        burst(level, player, 24);
        if (message != null) {
            player.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.GRAY), true);
        }
        Powers.syncIfDirty(player);
    }

    /** Everything the Ultimate holds ends: the universes, the runes, a body her mind is in, the reading of minds. */
    static void letGo(ServerPlayer player, PowerData data) {
        releaseAll(player.server, player.getUUID(), false);
        if (MindLink.isLinked(player)) {
            MindLink.end(player.server, player.getUUID(), MindLink.End.LET_GO);
        }
        if (data.toggle(Toggle.MIND_READING)) {
            data.setToggle(Toggle.MIND_READING, false);
            MindReading.stop(player);
        }
        STOLEN.remove(player.getUUID());
        REVIVE_AT.remove(player.getUUID());
    }

    /** H (+1) or Sneak + H (-1) as the Ultimate: her menu, or her hair. False when she is not the Ultimate. */
    public static boolean onLookKey(ServerPlayer player, PowerData data, int step) {
        if (!is(data)) {
            return false;
        }
        if (step < 0) {
            boolean comics = !comicsHair(data);
            data.setToggle(Toggle.WANDA_COMICS_HAIR, comics);
            Powers.syncIfDirty(player);
            player.displayClientMessage(Component.translatable(comics ? "message.enhanced.ultimate_wanda.hair.comics"
                    : "message.enhanced.ultimate_wanda.hair.films").withStyle(s -> s.withColor(GLOW)), true);
            burst(player.serverLevel(), player, 16);
            return true;
        }
        if (!Ultimate.allowed(player)) {
            revert(player, data, "message.enhanced.ultimate_wanda.lost");
            return true;
        }
        int menu = data.ironCharge() == MENU_ULTIMATE ? MENU_WITCH : MENU_ULTIMATE;
        if (data.toggle(Toggle.MIND_READING)) {
            data.setToggle(Toggle.MIND_READING, false);
            MindReading.stop(player);
        }
        PowerTicker.stopHoldAbilities(data);
        data.setIronCharge(menu);
        data.markDirty();
        Powers.syncIfDirty(player);
        ServerLevel level = player.serverLevel();
        burst(level, player, 30);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 0.9F);
        player.displayClientMessage(Component.translatable("message.enhanced.ultimate_wanda.menu", menuName(menu))
                .withStyle(s -> s.withColor(RED).withBold(true)), true);
        return true;
    }

    /** The name of a menu ("The Scarlet Witch", "Ultimate"). */
    public static Component menuName(int menu) {
        return Component.translatable(menu == MENU_ULTIMATE ? "hud.enhanced.ultimate_wanda.menu.ultimate"
                : "hud.enhanced.ultimate_wanda.menu.witch");
    }

    // ---- R: taking over a universe ----

    /** The universe a player stands in. */
    public static int universeOf(Entity entity) {
        return Multiverse.universeAt(entity.level(), entity.blockPosition());
    }

    /** Who holds this universe, or null. */
    @Nullable
    public static UUID holder(int universe) {
        return TAKEN.get(universe);
    }

    /** How many universes this Ultimate Wanda holds. */
    public static int held(UUID wanda) {
        int n = 0;
        for (UUID owner : TAKEN.values()) {
            if (owner.equals(wanda)) {
                n++;
            }
        }
        return n;
    }

    /** R: the universe she stands in is hers (its sky turns red), or, held already, let go. */
    static void toggleTakeover(ServerPlayer player) {
        int universe = universeOf(player);
        UUID owner = TAKEN.get(universe);
        ServerLevel level = player.serverLevel();
        if (player.getUUID().equals(owner)) {
            release(universe);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            player.displayClientMessage(Component.translatable("message.enhanced.ultimate_wanda.universe.released", universe)
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        // (another Ultimate Wanda's universe is taken from her: the stronger will is the one who reaches for it)
        TAKEN.put(universe, player.getUUID());
        burst(level, player, 60);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6F, 0.8F);
        player.displayClientMessage(Component.translatable("message.enhanced.ultimate_wanda.universe.taken", universe, held(player.getUUID()))
                .withStyle(s -> s.withColor(RED).withBold(true)), true);
        Component told = Component.translatable("message.enhanced.ultimate_wanda.universe.felt", player.getDisplayName())
                .withStyle(s -> s.withColor(GLOW));
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player && universeOf(other) == universe) {
                other.displayClientMessage(told, true);
            }
        }
    }

    /** A universe is free again: its sky clears and its runes go out. */
    static void release(int universe) {
        TAKEN.remove(universe);
        RUNES.remove(universe);
    }

    /** Sneak + R (or the Ultimate ending): every universe she holds is let go. */
    public static void releaseAll(MinecraftServer server, UUID wanda, boolean tell) {
        int n = 0;
        for (Iterator<Map.Entry<Integer, UUID>> it = TAKEN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, UUID> e = it.next();
            if (e.getValue().equals(wanda)) {
                RUNES.remove(e.getKey());
                it.remove();
                n++;
            }
        }
        ServerPlayer player = server.getPlayerList().getPlayer(wanda);
        if (tell && player != null) {
            player.displayClientMessage(Component.translatable(n > 0 ? "message.enhanced.ultimate_wanda.universe.released_all"
                    : "message.enhanced.ultimate_wanda.universe.none", n).withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ---- B: the runes in the sky ----

    /** B: the runes burn in the skies of every universe she holds, or go out. */
    static void toggleRunes(ServerPlayer player) {
        List<Integer> mine = new ArrayList<>();
        for (Map.Entry<Integer, UUID> e : TAKEN.entrySet()) {
            if (e.getValue().equals(player.getUUID())) {
                mine.add(e.getKey());
            }
        }
        if (mine.isEmpty()) {
            say(player, "message.enhanced.ultimate_wanda.runes.no_universe");
            return;
        }
        boolean lit = false;
        for (int u : mine) {
            lit |= RUNES.contains(u);
        }
        ServerLevel level = player.serverLevel();
        if (lit) {
            mine.forEach(RUNES::remove);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            say(player, "message.enhanced.ultimate_wanda.runes.off");
            return;
        }
        RUNES.addAll(mine);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.2F, 0.5F);
        player.displayClientMessage(Component.translatable("message.enhanced.ultimate_wanda.runes.on", mine.size())
                .withStyle(s -> s.withColor(RED).withBold(true)), true);
    }

    /** Whether this player stands under her runes (they hold their powers). */
    public static boolean underRunes(Player player) {
        return !RUNES.isEmpty() && RUNES.contains(universeOf(player));
    }

    /** How many players' powers her runes have taken. */
    public static int stolen(UUID wanda) {
        Set<UUID> s = STOLEN.get(wanda);
        return s == null ? 0 : s.size();
    }

    /** Once a second for each Ultimate Wanda: her runes hold everyone with powers in her universes, and the sky shows them. */
    private static void runesTick(ServerPlayer wanda) {
        if (RUNES.isEmpty()) {
            return;
        }
        boolean second = wanda.tickCount % 20 == 0;
        boolean drawn = wanda.tickCount % 6 == 0;
        if (!second && !drawn) {
            return;
        }
        for (ServerPlayer other : wanda.server.getPlayerList().getPlayers()) {
            int u = universeOf(other);
            if (!RUNES.contains(u) || !wanda.getUUID().equals(TAKEN.get(u)) || !other.isAlive()) {
                continue;
            }
            if (drawn) {
                drawRunes(other.serverLevel(), other);
            }
            if (!second || other == wanda || other.isSpectator() || other.isCreative() || is(other)
                    || !Powers.power(other).hasPowers()) {
                continue;
            }
            other.addEffect(new MobEffectInstance(ModEffects.DRAINED, DRAIN_TICKS, 0, false, true, true), wanda);
            Set<UUID> taken = STOLEN.computeIfAbsent(wanda.getUUID(), k -> new HashSet<>());
            if (taken.add(other.getUUID())) {
                other.displayClientMessage(Component.translatable("message.enhanced.ultimate_wanda.runes.taken_you", wanda.getDisplayName())
                        .withStyle(s -> s.withColor(RED)), true);
                wanda.displayClientMessage(Component.translatable("message.enhanced.ultimate_wanda.runes.stronger", other.getDisplayName(),
                        taken.size()).withStyle(s -> s.withColor(GLOW)), true);
                ServerLevel level = wanda.serverLevel();
                burst(level, wanda, 20);
                level.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
        }
    }

    /**
     * The runes over one player: a slowly turning circle of red glyphs high in the sky above them, and the spokes of its
     * sigil (a few dozen motes, every six ticks, sent once for everyone near).
     */
    private static void drawRunes(ServerLevel level, ServerPlayer under) {
        double cx = under.getX();
        double cy = under.getY() + 22.0;
        double cz = under.getZ();
        double turn = level.getGameTime() * 0.01;
        EnergyParticleOptions rune = new EnergyParticleOptions(RED, 2.4F);
        EnergyParticleOptions glow = new EnergyParticleOptions(GLOW, 1.8F);
        for (int i = 0; i < 16; i++) {
            double a = turn + i * Math.PI * 2.0 / 16.0;
            level.sendParticles(rune, cx + Math.cos(a) * 14.0, cy, cz + Math.sin(a) * 14.0, 1, 0.0, 0.0, 0.0, 0.0);
        }
        for (int i = 0; i < 5; i++) {
            double a = -turn * 2.0 + i * Math.PI * 2.0 / 5.0;
            double b = a + Math.PI * 4.0 / 5.0;
            for (int k = 0; k <= 3; k++) {
                double t = k / 3.0;
                double x = Math.cos(a) * 9.0 * (1.0 - t) + Math.cos(b) * 9.0 * t;
                double z = Math.sin(a) * 9.0 * (1.0 - t) + Math.sin(b) * 9.0 * t;
                level.sendParticles(glow, cx + x, cy, cz + z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    // ---- V: taking over a body, and reading minds ----

    /** V: her mind goes into the body she looks at, within 30 blocks (any telepath's too, and never a backlash). */
    static void takeOver(ServerPlayer player) {
        EntityHitResult hit = Raycasts.entity(player, TAKE_OVER_RANGE, 0.3, e -> e instanceof LivingEntity l && l != player
                && l.isAlive() && !l.isSpectator() && !(l instanceof ArmorStand) && !MindLink.isHost(l), 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.xavier.nothing"), true);
            return;
        }
        if (MindLink.take(player, (LivingEntity) hit.getEntity())) {
            burst(player.serverLevel(), player, 20);
        }
    }

    /** Sneak + V: reading minds (Jean Grey's, in red): what they carry, their health, what they feel. */
    static void toggleMindReading(ServerPlayer player, PowerData data) {
        boolean on = !data.toggle(Toggle.MIND_READING);
        data.setToggle(Toggle.MIND_READING, on);
        if (!on) {
            MindReading.stop(player);
        }
        Powers.syncIfDirty(player);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.ultimate_wanda.mind_read.on"
                : "message.enhanced.ultimate_wanda.mind_read.off").withStyle(s -> s.withColor(on ? GLOW : 0xAAAAAA)), true);
        if (on) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }

    /** Whether her own body is guarded by the shield of red light (her mind is in another body). */
    public static boolean shielded(Player player) {
        return is(player) && MindLink.isLinked(player);
    }

    /** The shield's light round her body (a few motes every four ticks). */
    private static void shieldMotes(ServerPlayer player) {
        if (player.tickCount % 4 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        EnergyParticleOptions mote = new EnergyParticleOptions(RED, 1.1F);
        double t = player.tickCount * 0.15;
        for (int i = 0; i < 10; i++) {
            double a = t + i * Math.PI * 2.0 / 10.0;
            double y = Math.sin(t * 0.7 + i) * 0.9;
            double r = Math.sqrt(Math.max(0.0, 1.3 * 1.3 - y * y));
            level.sendParticles(mote, player.getX() + Math.cos(a) * r, player.getY() + 1.0 + y, player.getZ() + Math.sin(a) * r,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // ---- every tick ----

    /** Every tick of a Scarlet Witch (from {@code PowerTicker.heroTick}): Ultimate is checked, and her upkeep runs. */
    static void tick(ServerPlayer player, PowerData data) {
        if (!data.is(Live.WANDA_ULTIMATE)) {
            return;
        }
        if (data.power() != Power.SCARLET_WITCH) {
            data.set(Live.WANDA_ULTIMATE, false);
            return;
        }
        if (!allowed(player)) {
            revert(player, data, "message.enhanced.ultimate_wanda.lost");
            return;
        }
        UUID id = player.getUUID();
        Long revive = REVIVE_AT.get(id);
        if (revive != null) {
            reviving(player, revive);
        }
        unfreeze(player);
        // Every healing of hers twice over: whatever she gained since her last tick (and not by coming back), again.
        Float last = LAST_HEALTH.get(id);
        float now = player.getHealth();
        if (last != null && now > last && revive == null && player.isAlive()) {
            player.heal(now - last);
        }
        LAST_HEALTH.put(id, player.getHealth());
        if (MindLink.isLinked(player)) {
            MindLink.tick(player);
            shieldMotes(player);
        }
        if (data.toggle(Toggle.MIND_READING)) {
            MindReading.tick(player, data);
        }
        runesTick(player);
    }

    /** Nothing freezes her: what holds a body still is shaken off the moment it lands. */
    private static void unfreeze(ServerPlayer player) {
        if (player.getTicksFrozen() > 0) {
            player.setTicksFrozen(0);
        }
        for (Holder<MobEffect> effect : FREEZES) {
            if (player.hasEffect(effect)) {
                player.removeEffect(effect);
            }
        }
    }

    /** The effects that hold a body still (a spellbinding, stone, the Time Stone's lock, webs, a cocoon of sand). */
    private static final List<Holder<MobEffect>> FREEZES = List.of(ModEffects.SPELLBOUND, ModEffects.PETRIFIED,
            ModEffects.TIME_LOCKED, ModEffects.WEBBED, ModEffects.ENTOMBED);

    /** Whether an effect is one nothing can hold her with (for {@code MobEffectEvent.Applicable}). */
    public static boolean refuses(LivingEntity entity, Holder<MobEffect> effect) {
        if (!is(entity)) {
            return false;
        }
        for (Holder<MobEffect> f : FREEZES) {
            if (effect.is(f)) {
                return true;
            }
        }
        return false;
    }

    /** Every player, every tick (server): their client is told when the universe they stand in turns red, or clears. */
    public static void everyPlayerTick(ServerPlayer player) {
        if (TAKEN.isEmpty() && !TOLD.containsKey(player.getUUID())) {
            return;
        }
        int u = universeOf(player);
        int state = !TAKEN.containsKey(u) ? UniverseTakeoverPayload.CLEAR
                : RUNES.contains(u) ? UniverseTakeoverPayload.RUNES : UniverseTakeoverPayload.RED;
        Integer told = TOLD.get(player.getUUID());
        if (told == null ? state != UniverseTakeoverPayload.CLEAR : told != state) {
            Net.sendToPlayer(player, new UniverseTakeoverPayload(state));
            if (state == UniverseTakeoverPayload.CLEAR) {
                TOLD.remove(player.getUUID());
            } else {
                TOLD.put(player.getUUID(), state);
            }
        }
    }

    // ---- her death, and her body ----

    /**
     * She is about to die: unless what kills her bypasses invulnerability (the void, /kill), death is refused and her body
     * comes back in red energy a second later, with all its health. Returns whether her death was refused.
     */
    public static boolean onDeath(ServerPlayer player, DamageSource source) {
        if (!is(player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }
        player.setHealth(1.0F);
        long at = player.serverLevel().getGameTime() + REVIVE_TICKS;
        REVIVE_AT.put(player.getUUID(), at);
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, REVIVE_TICKS + 2, 0, false, false, false));
        ServerLevel level = player.serverLevel();
        level.sendParticles(new EnergyParticleOptions(RED, 1.6F), player.getX(), player.getY() + 1.0, player.getZ(), 120, 0.4, 0.9, 0.4, 0.12);
        level.sendParticles(new EnergyParticleOptions(DEEP, 1.2F), player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7F, 0.6F);
        player.displayClientMessage(Component.translatable("message.enhanced.ultimate_wanda.revive.dying").withStyle(s -> s.withColor(RED)), true);
        return true;
    }

    /** While her body comes back: red energy gathers where it was; at the end of the second, she is whole. */
    private static void reviving(ServerPlayer player, long at) {
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() < at) {
            if (player.tickCount % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(RED, 1.3F), player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.8, 0.3, 0.02);
            }
            return;
        }
        REVIVE_AT.remove(player.getUUID());
        player.removeEffect(MobEffects.INVISIBILITY);
        player.setHealth(player.getMaxHealth());
        LAST_HEALTH.put(player.getUUID(), player.getHealth());
        burst(level, player, 100);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.1F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.enhanced.ultimate_wanda.revive.back").withStyle(s -> s.withColor(RED).withBold(true)), true);
    }

    /**
     * Whether a hurt to her is turned away: while her body comes back, and while her mind is in another body (the shield of
     * red light). What bypasses invulnerability still lands.
     */
    public static boolean guards(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer player) || !is(player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return REVIVE_AT.containsKey(player.getUUID()) || MindLink.isLinked(player);
    }

    /** Her body's modifiers, there while she is the Ultimate and gone otherwise (every player, every tick). */
    public static void applyBody(ServerPlayer player, PowerData data) {
        boolean on = is(data);
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        double base = health == null ? 20.0 : health.getBaseValue();
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, HEALTH, Math.max(0.0, MAX_HEALTH - base), on);
        PowerTicker.setModifier(player, Attributes.ARMOR, ARMOR_ID, ARMOR, on);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, TOUGHNESS, on);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, KNOCKBACK_RESISTANCE, on);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Her flight speed as the Ultimate. */
    public static float flightSpeed(Power power) {
        return power.flightSpeed() * FLIGHT_FACTOR;
    }

    /** How many times as hard her powers hit now (the runes' players make her stronger). */
    public static double damageFactor(UUID wanda) {
        return DAMAGE_FACTOR + STOLEN_STEP * Math.min(STOLEN_MAX, stolen(wanda));
    }

    /**
     * What a hurt done by Ultimate Wanda comes to: her powers (anything but a bare blow of her fist) hit {@link #damageFactor}
     * times as hard; a body she steers hits as hard as she does.
     */
    public static float outgoing(LivingEntity target, DamageSource source, float amount) {
        Entity cause = source.getEntity();
        if (cause instanceof LivingEntity body && !(cause instanceof Player p && is(p))) {
            ServerPlayer steering = MindLink.controller(body);
            if (steering != null && is(steering) && steering != target) {
                return (float) (amount * damageFactor(steering.getUUID()));
            }
        }
        if (!(cause instanceof Player player) || cause == target || !is(player)) {
            return amount;
        }
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player) {
            return amount; // (her bare fist)
        }
        return (float) (amount * damageFactor(player.getUUID()));
    }

    // ---- the player leaves, the server stops ----

    /** She left (or changed nothing but her body): what the Ultimate held ends; her client forgets its sky. */
    public static void onLogout(ServerPlayer player) {
        UUID id = player.getUUID();
        releaseAll(player.server, id, false);
        STOLEN.remove(id);
        REVIVE_AT.remove(id);
        LAST_HEALTH.remove(id);
        TOLD.remove(id);
    }

    public static void clear() {
        TAKEN.clear();
        RUNES.clear();
        STOLEN.clear();
        REVIVE_AT.clear();
        LAST_HEALTH.clear();
        TOLD.clear();
    }

    // ---- effects and words ----

    /** A burst of her red energy. */
    public static void burst(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(new EnergyParticleOptions(RED, 1.4F), player.getX(), player.getY() + 1.0, player.getZ(), count,
                0.6, 0.9, 0.6, 0.15);
        level.sendParticles(new EnergyParticleOptions(GLOW, 1.0F), player.getX(), player.getY() + 1.0, player.getZ(), count / 2,
                0.4, 0.8, 0.4, 0.25);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + 1.0, player.getZ(), count / 3, 0.5, 0.8, 0.5, 0.06);
    }

    static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }
}

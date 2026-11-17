package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.WitchcraftConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casting a spell (3.17): the Spells key casts the chosen spell (the Spell Wheel, with Sneak, chooses it); a witch's R G V B N, and the
 * same with Sneak, cast her first ten. Most spells must be chanted first: Billy's "I want ..." and the witches' words
 * are spoken aloud to everyone near, runes circle the caster's hands ({@link Live#CHANTING}), holding the key chants faster
 * ({@code witchcraft.chantHeldSpeed}), and a heavy blow ({@code witchcraft.interruptDamage} or more) breaks it off and
 * loses half its mana. The Demiurge in his star form needs no incantation: his spells come at once.
 */
public final class Casting {
    /** An incantation under way. */
    private static final class Chant {
        final Spell spell;
        final float needed;
        float progress;
        boolean held;
        final boolean alt;

        Chant(Spell spell, float needed, boolean alt) {
            this.spell = spell;
            this.needed = needed;
            this.alt = alt;
        }
    }

    private static final Map<UUID, Chant> CHANTS = new HashMap<>();
    /** Player -> spell id -> game time it is ready again. */
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

    private Casting() {
    }

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    // ---- the keys ----

    /** The Spells key went down (or up: the chant goes on at its usual pace). */
    public static void key(ServerPlayer player, boolean down) {
        Chant chant = CHANTS.get(player.getUUID());
        if (!down) {
            if (chant != null) {
                chant.held = false;
            }
            return;
        }
        if (chant != null) {
            chant.held = true; // (pressed again while chanting: faster)
            return;
        }
        Spell spell = selected(player);
        if (spell == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.spell.none_chosen").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (cast(player, spell, false) && CHANTS.containsKey(player.getUUID())) {
            CHANTS.get(player.getUUID()).held = true;
        }
    }

    /** A witch's quick key: R G V B N are her first five spells, and with Sneak the next five. */
    public static void quick(ServerPlayer player, int ability, boolean down, boolean sneak) {
        if (!down) {
            Chant chant = CHANTS.get(player.getUUID());
            if (chant != null) {
                chant.held = false;
            }
            return;
        }
        List<Spell> known = Magic.known(player);
        int slot = (ability - 1) + (sneak ? 5 : 0);
        if (slot < 0 || slot >= known.size()) {
            player.displayClientMessage(Component.translatable("message.enhanced.spell.no_quick").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (cast(player, known.get(slot), sneak) && CHANTS.containsKey(player.getUUID())) {
            CHANTS.get(player.getUUID()).held = true;
        }
    }

    /** The Spell Wheel's choice. */
    public static void select(ServerPlayer player, String id) {
        Spell spell = Spell.byId(id);
        if (spell == null || !Magic.knows(player, spell)) {
            return;
        }
        Magic.set(player, Magic.data(player).withSelected(spell.id()));
        player.displayClientMessage(Component.translatable("message.enhanced.spell.chosen", spell.displayName())
                .withStyle(s -> s.withColor(Magic.color(player, spell))), true);
    }

    /** The spell the Spells key casts: the one chosen, or (none chosen yet) the first he knows. */
    public static Spell selected(ServerPlayer player) {
        Spell chosen = Spell.byId(Magic.data(player).selected());
        if (chosen != null && Magic.knows(player, chosen)) {
            return chosen;
        }
        List<Spell> known = Magic.known(player);
        return known.isEmpty() ? null : known.get(0);
    }

    // ---- casting ----

    /** Ticks a spell is still cooling down for him (0: ready). */
    public static long cooldownLeft(ServerPlayer player, Spell spell) {
        Map<String, Long> map = COOLDOWNS.get(player.getUUID());
        Long ready = map == null ? null : map.get(spell.id());
        return ready == null ? 0 : Math.max(0, ready - player.level().getGameTime());
    }

    /** Why he cannot cast now (a lang key), or null. */
    static String refusal(ServerPlayer player, Spell spell) {
        if (player.isSpectator() || !player.isAlive()) {
            return "message.enhanced.spell.cannot";
        }
        if (!Magic.knows(player, spell)) {
            return "message.enhanced.spell.unknown";
        }
        if (MagicZones.silenced(player)) {
            return "message.enhanced.spell.silenced";
        }
        if (cooldownLeft(player, spell) > 0 && !player.getAbilities().instabuild) {
            return "message.enhanced.spell.cooldown";
        }
        if (Magic.mana(player) < spell.mana) {
            return "message.enhanced.spell.no_mana";
        }
        return null;
    }

    /**
     * Starts casting {@code spell}: its incantation, or (the star form, a spell with none) the spell at once.
     *
     * @param alt the quick key's Sneak (some spells have a second way)
     * @return whether it began
     */
    public static boolean cast(ServerPlayer player, Spell spell, boolean alt) {
        String refusal = refusal(player, spell);
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal, spell.displayName(),
                    (cooldownLeft(player, spell) + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        float needed = chantTicks(player, spell);
        if (needed <= 0) {
            release(player, spell, alt);
            return true;
        }
        CHANTS.put(player.getUUID(), new Chant(spell, needed, alt));
        PowerData data = Powers.get(player);
        data.set(Live.CHANTING, true);
        Powers.syncIfDirty(player);
        speak(player, spell);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                0.8F, 1.4F);
        return true;
    }

    /** How long his incantation of this spell lasts: shorter as he grows; none in the star form. */
    static float chantTicks(ServerPlayer player, Spell spell) {
        if (spell.chant <= 0 || Magic.starForm(player)) {
            return 0;
        }
        float f = 1.0F;
        if (Magic.isDemiurge(player)) {
            f = 0.4F;
        } else if (Powers.power(player) == Power.BILLY) {
            f = 1.0F - 0.04F * (Magic.billyRank(player) - 1);
        } else if (Magic.isWitch(player)) {
            f = 1.0F - 0.08F * (Magic.data(player).level() - 1);
        }
        return Math.max(4, spell.chant * f);
    }

    /** He speaks the words: everyone within 24 blocks hears them (Billy's "I want ...", a witch's chant). */
    private static void speak(ServerPlayer player, Spell spell) {
        MutableComponent words = Powers.power(player) == Power.BILLY
                ? Component.translatable("chant.enhanced.i_want", spell.wish())
                : spell.chantWords().copy();
        int color = Magic.color(player, spell);
        Component line = Component.translatable("chant.enhanced.speaks", player.getDisplayName(),
                words.withStyle(s -> s.withColor(color).withItalic(true)));
        for (ServerPlayer near : player.serverLevel().players()) {
            if (near.distanceToSqr(player) <= 24 * 24) {
                near.sendSystemMessage(line);
            }
        }
    }

    /** Every tick of every player: the incantation goes on, and ends in the spell. */
    public static void tick(ServerPlayer player) {
        Chant chant = CHANTS.get(player.getUUID());
        PowerData data = Powers.get(player);
        if (chant == null) {
            if (data.is(Live.CHANTING)) {
                data.set(Live.CHANTING, false);
            }
            return;
        }
        if (!player.isAlive() || player.isSpectator() || !Magic.knows(player, chant.spell)) {
            stop(player);
            return;
        }
        if (MagicZones.silenced(player)) {
            interrupt(player, "message.enhanced.spell.silenced");
            return;
        }
        float speed = chant.held ? (float) (loaded() ? WitchcraftConfig.CHANT_HELD_SPEED.get() : 2.0) : 1.0F;
        chant.progress += speed;
        if (player.tickCount % 4 == 0) {
            Vec3 hands = player.position().add(0, player.getBbHeight() * 0.62, 0).add(player.getLookAngle().scale(0.4));
            MagicFx.burst(player.serverLevel(), hands, Magic.color(player, chant.spell), 2, 0.25, 0.8F);
        }
        if (chant.progress >= chant.needed) {
            CHANTS.remove(player.getUUID());
            data.set(Live.CHANTING, false);
            Powers.syncIfDirty(player);
            release(player, chant.spell, chant.alt);
        }
    }

    /** The incantation's progress (0-1) for the HUD, or -1 when he is not chanting. */
    public static float progress(ServerPlayer player) {
        Chant chant = CHANTS.get(player.getUUID());
        return chant == null ? -1 : Math.min(1.0F, chant.progress / chant.needed);
    }

    /** The spell being chanted, or null. */
    public static Spell chanting(ServerPlayer player) {
        Chant chant = CHANTS.get(player.getUUID());
        return chant == null ? null : chant.spell;
    }

    /** The spell comes: it is paid for, cools down, and does what it does (if it could not, nothing is spent). */
    static void release(ServerPlayer player, Spell spell, boolean alt) {
        if (refusal(player, spell) != null) {
            return;
        }
        float power = Magic.strength(player);
        boolean done = SpellEffects.cast(player, spell, power, alt);
        if (!done) {
            return;
        }
        Magic.spend(player, spell.mana);
        COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(spell.id(),
                player.level().getGameTime() + Math.max(0, (long) (spell.cooldown * (Magic.isDemiurge(player) ? 0.5 : 1.0))));
        Darkness.afterSpell(player, spell);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEX_BOLT.get(), SoundSource.PLAYERS,
                0.5F, 1.3F);
    }

    /** A blow landed on a chanting caster: a heavy one breaks the incantation off. */
    public static void hurt(ServerPlayer player, float amount) {
        Chant chant = CHANTS.get(player.getUUID());
        double limit = loaded() ? WitchcraftConfig.INTERRUPT_DAMAGE.get() : 4.0;
        if (chant == null || limit <= 0 || amount < limit) {
            return;
        }
        Magic.spend(player, Math.min(Magic.mana(player), chant.spell.mana * 0.5F));
        interrupt(player, "message.enhanced.spell.interrupted");
    }

    private static void interrupt(ServerPlayer player, String key) {
        Chant chant = CHANTS.remove(player.getUUID());
        Powers.get(player).set(Live.CHANTING, false);
        Powers.syncIfDirty(player);
        if (chant != null) {
            player.displayClientMessage(Component.translatable(key, chant.spell.displayName()).withStyle(ChatFormatting.RED), true);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CANDLE_EXTINGUISH,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            MagicFx.burst(player.serverLevel(), player.position().add(0, 1.2, 0), 0x777777, 10, 0.3, 0.8F);
        }
    }

    /** Ends his incantation without a word (the power changed, he died). */
    public static void stop(ServerPlayer player) {
        CHANTS.remove(player.getUUID());
        PowerData data = Powers.get(player);
        if (data.is(Live.CHANTING)) {
            data.set(Live.CHANTING, false);
            Powers.syncIfDirty(player);
        }
    }

    /** For the tests and the operators: every cooldown of his gone. */
    public static void resetCooldowns(ServerPlayer player) {
        COOLDOWNS.remove(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        stop(player);
        COOLDOWNS.remove(player.getUUID());
    }

    public static void clear() {
        CHANTS.clear();
        COOLDOWNS.clear();
    }
}

package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.config.WitchcraftConfig;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.ultimate.Ultimate;
import com.kolodkingal.enhanced.vishanti.WardStrength;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Who has magic and how much (3.17): the witches and sorcerers of the witchcraft (a {@link WitchPath} on a player with no
 * hero power), Billy (his spells come with his rank), the Demiurge (Billy with Ultimate Mode on: every spell), and the
 * hero sorcerers (the Scarlet Witch, Agatha, Doctor Strange, Loki), who cast the spells of the grimoires they have read.
 * Both sides read it (the magic is synced to its player).
 */
public final class Magic {
    private Magic() {
    }

    public static MagicData data(Player player) {
        return ModAttachments.MAGIC.get(player);
    }

    public static void set(Player player, MagicData data) {
        ModAttachments.MAGIC.set(player, data);
    }

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    /** Whether witchcraft is on at all ({@code witchcraft.enabled}). */
    public static boolean enabled() {
        return !loaded() || WitchcraftConfig.ENABLED.get();
    }

    /** A witch or sorcerer of the witchcraft: a path, and no hero power (a hero's own power comes first). */
    public static boolean isWitch(Player player) {
        return enabled() && data(player).hasCraft() && !Powers.power(player).hasPowers();
    }

    /** The path of a witch, or null. */
    public static WitchPath path(Player player) {
        return isWitch(player) ? data(player).witchPath() : null;
    }

    /** Whether nothing binds him now: the Coven's Road unbound him, or an Unbinding lasts (mind control, curses, a Hex's runes). */
    public static boolean unbound(Player player) {
        MagicData data = data(player);
        return data.has(MagicData.ROAD_UNBOUND) || data.has(MagicData.UNBOUND);
    }

    /** Billy with Ultimate Mode on: the Demiurge. */
    public static boolean isDemiurge(Player player) {
        return Powers.power(player) == Power.BILLY && Ultimate.on();
    }

    /** The Demiurge in his star form (his ultimate form): no spell needs its incantation. */
    public static boolean starForm(Player player) {
        return isDemiurge(player) && Powers.get(player).is(Live.ULTIMATE);
    }

    /** The heroes who are sorcerers (they cast what their grimoires taught them, and the coven's spells). */
    private static boolean heroSorcerer(Player player) {
        Power power = Powers.power(player);
        return power != Power.BILLY && power != Power.NONE && WardStrength.isSorcerer(player);
    }

    /**
     * Whether this player casts spells with the spell key at all: a witch or sorcerer of the witchcraft; and, only with
     * Ultimate Mode on, Billy (the Demiurge) and the hero sorcerers. With Ultimate Mode off a hero is the hero he was
     * before 3.17: no mana, no spell key, no spells.
     */
    public static boolean isCaster(Player player) {
        if (!enabled()) {
            return false;
        }
        return isWitch(player) || Ultimate.on() && (Powers.power(player) == Power.BILLY || heroSorcerer(player));
    }

    /** Billy's rank (1-10), or 0 for anyone else. */
    public static int billyRank(Player player) {
        return Powers.power(player) == Power.BILLY ? BillyRanks.rank(ModAttachments.BUILD_DATA.get(player)) : 0;
    }

    /**
     * Whether the Witches' Mindscape, Billy's place of meditation, is open to him: Billy himself (the Demiurge), and the
     * witches and sorcerers whose power is Billy's own gift ({@link MagicData#BILLYS}). Nobody else finds the way in.
     */
    public static boolean mindscapeOpen(Player player) {
        if (!isCaster(player)) {
            return false;
        }
        return Powers.power(player) == Power.BILLY || isWitch(player) && data(player).has(MagicData.BILLYS);
    }

    /** Whether he knows the spell (and may cast it now, mana and cooldown aside). */
    public static boolean knows(Player player, Spell spell) {
        if (!isCaster(player)) {
            return false;
        }
        if (spell == Spell.MINDSCAPE && !mindscapeOpen(player)) {
            return false;
        }
        if (isDemiurge(player)) {
            return true; // (every spell of every path and grimoire, and his own)
        }
        MagicData data = data(player);
        int grimoire = spell.group.grimoire();
        if (grimoire != 0) {
            return data.has(grimoire) && (isWitch(player) ? data.level() >= spell.level : true);
        }
        if (Powers.power(player) == Power.BILLY) {
            return spell.billyRank > 0 && billyRank(player) >= spell.billyRank;
        }
        if (heroSorcerer(player)) {
            return spell.group == Spell.Group.WITCH && spell.level <= 2;
        }
        WitchPath path = data.witchPath();
        if (path == null || data.level() < spell.level) {
            return false;
        }
        if (spell.group == Spell.Group.WITCH) {
            return true;
        }
        WitchPath of = spell.group.path();
        if (of == null) {
            return false;
        }
        // (the Spirit witch learns from every witch's path; a sorcerer only the mystic arts)
        return of == path || (path == WitchPath.SPIRIT && of.witch());
    }

    /** Every spell he knows, in their order (his quick keys take the first ten). */
    public static List<Spell> known(Player player) {
        List<Spell> list = new ArrayList<>();
        if (!isCaster(player)) {
            return list;
        }
        WitchPath path = path(player);
        // (a witch's own path first, so R G V B N are her own spells)
        if (path != null) {
            for (Spell spell : Spell.values()) {
                if (spell.group.path() == path && knows(player, spell)) {
                    list.add(spell);
                }
            }
        }
        for (Spell spell : Spell.values()) {
            if (!list.contains(spell) && knows(player, spell)) {
                list.add(spell);
            }
        }
        return list;
    }

    /** How strong his spells are: 1 for a first-level witch or an Apprentice Billy, more as they grow; the Demiurge 1.8. */
    public static float strength(Player player) {
        if (isDemiurge(player)) {
            return starForm(player) ? 2.2F : 1.8F;
        }
        if (Powers.power(player) == Power.BILLY) {
            return 1.0F + 0.08F * (billyRank(player) - 1);
        }
        if (isWitch(player)) {
            return 0.9F + 0.15F * (data(player).level() - 1);
        }
        return 1.0F;
    }

    /** His mana pool's top. */
    public static float maxMana(Player player) {
        if (isDemiurge(player)) {
            return 600.0F;
        }
        if (Powers.power(player) == Power.BILLY) {
            return 100.0F + 20.0F * (billyRank(player) - 1);
        }
        if (isWitch(player)) {
            return 80.0F + 30.0F * data(player).level();
        }
        return 100.0F;
    }

    /** Mana he gets back each second (twice as much near another caster: the coven, {@link #inCoven}). */
    public static float regen(Player player) {
        float base = loaded() ? WitchcraftConfig.MANA_REGEN.get().floatValue() : 3.0F;
        if (isDemiurge(player)) {
            base *= 8.0F;
        } else if (Powers.power(player) == Power.BILLY) {
            base += 0.5F * billyRank(player);
        } else if (isWitch(player)) {
            base += data(player).level();
        }
        return base;
    }

    /** Whether another caster stands near him: they share their magic (a coven), and both regain mana faster. */
    public static boolean inCoven(Player player) {
        double r = loaded() ? WitchcraftConfig.COVEN_RADIUS.get() : 16;
        for (Player other : player.level().players()) {
            if (other != player && other.isAlive() && !other.isSpectator() && other.distanceToSqr(player) <= r * r
                    && isCaster(other)) {
                return true;
            }
        }
        return false;
    }

    /** The colour of a spell as he casts it: darkened by the Darkhold's corruption. */
    public static int color(Player player, Spell spell) {
        return corrupt(player, spell.color);
    }

    /** The colour of his magic as a caster (his chant, his broom): Billy's blue, a witch's path, a hero's own colour. */
    public static int casterColor(Player player) {
        Power power = Powers.power(player);
        int base;
        if (power == Power.BILLY) {
            base = com.kolodkingal.enhanced.ability.BillyAbilities.MAIN;
        } else if (data(player).witchPath() != null && !power.hasPowers()) {
            base = data(player).witchPath().color;
        } else {
            base = power.color;
        }
        return corrupt(player, base);
    }

    /** The bright second colour of his magic. */
    public static int casterGlow(Player player) {
        Power power = Powers.power(player);
        if (power == Power.BILLY) {
            return com.kolodkingal.enhanced.ability.BillyAbilities.INNER;
        }
        if (data(player).witchPath() != null && !power.hasPowers()) {
            return data(player).witchPath().glow;
        }
        return power.glow;
    }

    /** A colour of his magic darkened by the Darkhold (toward its violet-black). */
    public static int corrupt(Player player, int color) {
        int corruption = data(player).corruption();
        if (corruption <= 0) {
            return color;
        }
        float f = Mth.clamp(corruption / 140.0F, 0.0F, 0.75F);
        return FastColor.ARGB32.lerp(f, color | 0xFF000000, 0xFF2A0A3A) & 0xFFFFFF;
    }

    /** How much more his spells hurt for the Darkhold's corruption (1: none). */
    public static float corruptionBonus(Player player) {
        return 1.0F + data(player).corruption() / 200.0F;
    }

    /** Whether a spell of his may hurt {@code target} (the rule every area attack follows). */
    public static boolean enemy(Player caster, LivingEntity target) {
        return target.isAlive() && AreaTargets.mayHurt(caster, target, false);
    }

    /** Whether {@code target} is one his spells of help may touch: himself, his pets, any player he would not hurt. */
    public static boolean friend(Player caster, LivingEntity target) {
        if (target == caster) {
            return true;
        }
        if (!target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target instanceof TamableAnimal pet && pet.isOwnedBy(caster)) {
            return true;
        }
        if (target instanceof Player other) {
            return caster.isAlliedTo(other) || !AreaTargets.mayHurt(caster, other, false) || caster.getTeam() == null;
        }
        return caster.isAlliedTo(target);
    }

    /** Mana he has now (Creative: always the top). */
    public static float mana(Player player) {
        return player.getAbilities().instabuild ? maxMana(player) : data(player).mana();
    }

    /** Spends mana; false (nothing spent) if he has too little. */
    public static boolean spend(ServerPlayer player, float amount) {
        if (player.getAbilities().instabuild || amount <= 0) {
            return true;
        }
        MagicData data = data(player);
        if (data.mana() < amount) {
            return false;
        }
        set(player, data.withMana(data.mana() - amount));
        return true;
    }
}

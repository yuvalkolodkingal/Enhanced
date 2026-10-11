package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

/**
 * Who an attack that hits everything in an area may hurt: the Phoenix Burst, Apocalypse's Sand Wave and Sandstorm, the
 * Titan and Gravity Slams, Ultron's Overcharge, a tornado. Without a rule they would kill the villagers, Iron Golems,
 * farm animals and pets round a base without a block changing, so each of them asks {@link #mayHurt} for every
 * creature in reach.
 *
 * <p>The rule: never the caster, a Creative or Spectator player, an armour stand, a teammate, the caster's own pets or
 * an ally some rule names ({@link #addAllyRule}: a creature the Phoenix has taken over, the fighters of a boss fight).
 * A player only if PvP is on and the caster may harm them. Hostile creatures always, and so is anything that is going
 * after the caster or their team, and the bosses and their minions. Every other creature (villagers, golems, animals,
 * others' pets) only if the caster's option for hurting passive creatures says so, which every hero has off.
 */
public final class AreaTargets {
    private static final List<BiPredicate<LivingEntity, LivingEntity>> ALLIES = new CopyOnWriteArrayList<>();

    /**
     * Adds a rule that names an ally of the caster: {@code rule.test(owner, target)} true means the area attack spares
     * the target. Heroes register theirs once, when the mod loads.
     */
    public static void addAllyRule(BiPredicate<LivingEntity, LivingEntity> rule) {
        ALLIES.add(rule);
    }

    /**
     * Whether an area attack of {@code owner} may hurt {@code target}.
     *
     * @param hurtsPassives whether the hero's option lets the attack hurt creatures that are neither hostile nor after
     *                      the caster (villagers, animals, golems, others' pets)
     */
    public static boolean mayHurt(LivingEntity owner, LivingEntity target, boolean hurtsPassives) {
        if (target == owner || target instanceof ArmorStand) {
            return false;
        }
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            if (owner instanceof Player attacker) {
                MinecraftServer server = player.getServer();
                return server != null && server.isPvpAllowed() && attacker.canHarmPlayer(player);
            }
            return true;
        }
        if (owner.isAlliedTo(target) || (target instanceof TamableAnimal pet && pet.isOwnedBy(owner))) {
            return false;
        }
        for (BiPredicate<LivingEntity, LivingEntity> ally : ALLIES) {
            if (ally.test(owner, target)) {
                return false;
            }
        }
        if (target instanceof Enemy || target.getType().is(ModTags.BOSSES) || target.getType().is(ModTags.BOSS_MINIONS)) {
            return true;
        }
        if (target instanceof Mob mob) {
            LivingEntity goal = mob.getTarget();
            if (goal != null && (goal == owner || owner.isAlliedTo(goal))) {
                return true;
            }
        }
        return hurtsPassives;
    }

    private AreaTargets() {
    }
}

package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * The warriors of Asgard defend it (3.5): Heimdall, Sif, Valkyrie, the Warriors Three, the Einherjar and the summoned
 * Einherjar choose, in this order, (1) the nearest of Hela's army ({@code #enhanced:hela_army}) within 32 blocks, (2) the
 * nearest monster within 16 that is after a player, (3) Hela herself, but only while none of her army is near. During a
 * fight with Hela, a warrior inside the arena or within 48 blocks of it looks for her army in the whole arena: Asgard's
 * warriors join the battle (they are not its fighters and win nothing), and walk home when it is over.
 *
 * <p>The choice is a pure function ({@link #pick}) of the candidates and two predicates, so it is tested without Hela's
 * lane; the predicates the game uses are {@link #army} and {@link #hela}.
 */
public class DefendAsgardGoal extends Goal {
    /** How far the warriors look for Hela's army. */
    public static final double ARMY_RANGE = 32.0;
    /** How far they look for monsters after a player. */
    public static final double MONSTER_RANGE = 16.0;
    /** How far from a Hela arena the warriors join her fight. */
    public static final double JOIN_RANGE = 48.0;

    private final AsgardianEntity npc;

    public DefendAsgardGoal(AsgardianEntity npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    /** One of Hela's army: her berserkers, Fenris and the rest of the tag (lane L4 fills it). */
    public static boolean army(Entity e) {
        return e.getType().is(ModTags.HELA_ARMY);
    }

    /** Hela herself: the boss of a Hela fight. */
    public static boolean hela(Entity e) {
        BossFight fight = BossFights.fightOf(e);
        return fight != null && fight.kind() == BossKind.HELA && e.getType().is(ModTags.BOSSES);
    }

    /** A monster that is after a player (not a boss: Hela is chosen as Hela, other bosses are the players' fight). */
    public static boolean hostileMonster(Entity e) {
        return e instanceof Mob mob && e.getType().getCategory() == MobCategory.MONSTER && mob.getTarget() instanceof Player
                && !e.getType().is(ModTags.BOSSES);
    }

    /**
     * Whom a warrior at {@code from} chooses among {@code candidates} (alive creatures near him): the nearest of the army
     * within {@code armyRange}; else the nearest monster after a player within {@link #MONSTER_RANGE}; else Hela, only if
     * no one of the army is within {@code armyRange}. Null if none.
     */
    @Nullable
    public static LivingEntity pick(Vec3 from, List<? extends LivingEntity> candidates, double armyRange,
                                    Predicate<Entity> isArmy, Predicate<Entity> isHela, Predicate<Entity> isMonster) {
        LivingEntity soldier = nearest(from, candidates, armyRange, e -> isArmy.test(e) && !isHela.test(e));
        if (soldier != null) {
            return soldier;
        }
        LivingEntity monster = nearest(from, candidates, MONSTER_RANGE, e -> isMonster.test(e) && !isHela.test(e));
        if (monster != null) {
            return monster;
        }
        return nearest(from, candidates, armyRange, isHela);
    }

    @Nullable
    private static LivingEntity nearest(Vec3 from, List<? extends LivingEntity> candidates, double range, Predicate<Entity> which) {
        LivingEntity best = null;
        double bestDistance = range * range;
        for (LivingEntity e : candidates) {
            if (!e.isAlive() || e instanceof AsgardianEntity || e instanceof Player || !which.test(e)) {
                continue;
            }
            double d = e.distanceToSqr(from);
            if (d <= bestDistance) {
                best = e;
                bestDistance = d;
            }
        }
        return best;
    }

    @Override
    public boolean canUse() {
        if (!(npc.role().warrior() || npc.summoned()) || npc.state().still() || npc.commanded() != null
                || (npc.tickCount + npc.getId()) % 20 != 0 || !(npc.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 from = npc.position();
        double range = ARMY_RANGE;
        AABB box = npc.getBoundingBox().inflate(ARMY_RANGE);
        BossFight fight = BossFights.active(level);
        if (fight != null && fight.kind() == BossKind.HELA && !npc.summoned()) {
            Vec3 center = fight.arena().center();
            double reach = fight.arena().radius() + JOIN_RANGE;
            if (from.distanceToSqr(center) <= reach * reach) {
                range = reach * 2;
                box = new AABB(center, center).inflate(fight.arena().radius() + 4, 24, fight.arena().radius() + 4).minmax(box);
            }
        }
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        LivingEntity chosen = pick(from, near, range, DefendAsgardGoal::army, DefendAsgardGoal::hela, DefendAsgardGoal::hostileMonster);
        LivingEntity current = npc.getTarget();
        if (chosen == null || chosen == current) {
            return false;
        }
        // A target of the army is never given up for a lesser one.
        if (current != null && current.isAlive() && army(current) && !army(chosen)) {
            return false;
        }
        npc.setTarget(chosen);
        return false;
    }
}

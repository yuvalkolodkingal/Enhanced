package com.kolodkingal.enhanced.effect;

import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Frightened (3.6, the Hulk's Roar): a creature that heard the roar runs from the one who roared (every few ticks it
 * picks a place away from him and runs there, faster than it walks) and does not turn on him ({@code HulkEvents}) while
 * it lasts. Who it fears is kept beside the effect, by creature.
 */
public class FrightenedEffect extends MobEffect {
    /** How far a frightened creature runs from him, and how fast. */
    public static final int RUN = 12;
    public static final double SPEED = 1.4;
    /** Who each frightened creature fears (forgotten with the creature). */
    private static final Map<LivingEntity, UUID> FEARED = new WeakHashMap<>();

    public FrightenedEffect() {
        super(MobEffectCategory.HARMFUL, 0x5BA33A);
    }

    /** The creature fears {@code who} from now on (while the effect lasts): it stops fighting at once. */
    public static void scare(LivingEntity creature, Entity who) {
        FEARED.put(creature, who.getUUID());
        if (creature instanceof Mob mob && mob.getTarget() == who) {
            mob.setTarget(null);
        }
        if (creature instanceof PathfinderMob mob) {
            flee(mob, who.position());
        }
    }

    /** Whether the creature is frightened of {@code who} right now. */
    public static boolean fears(LivingEntity creature, Entity who) {
        UUID feared = FEARED.get(creature);
        return feared != null && feared.equals(who.getUUID()) && creature.hasEffect(ModEffects.FRIGHTENED);
    }

    @Nullable
    private static Entity feared(LivingEntity creature) {
        UUID id = FEARED.get(creature);
        if (id == null || !(creature.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return null;
        }
        return level.getEntity(id);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity creature, int amplifier) {
        if (creature.level().isClientSide() || !(creature instanceof PathfinderMob mob)) {
            return true;
        }
        Entity from = feared(creature);
        if (from == null) {
            return true;
        }
        if (mob.getTarget() == from) {
            mob.setTarget(null);
        }
        if (mob.getNavigation().isDone() || mob.distanceTo(from) < RUN * 0.5) {
            flee(mob, from.position());
        }
        return true;
    }

    /** Sends the creature running to a place away from {@code from}. */
    static void flee(PathfinderMob mob, Vec3 from) {
        Vec3 away = DefaultRandomPos.getPosAway(mob, RUN, 7, from);
        if (away == null) {
            Vec3 dir = mob.position().subtract(from);
            dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(dir.x, 0, dir.z).normalize();
            away = mob.position().add(dir.scale(RUN * 0.5));
        }
        mob.getNavigation().moveTo(away.x, away.y, away.z, SPEED);
    }
}

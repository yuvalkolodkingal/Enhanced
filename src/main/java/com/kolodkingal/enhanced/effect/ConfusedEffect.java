package com.kolodkingal.enhanced.effect;

import com.kolodkingal.enhanced.item.InfinityStone;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * What Loki's Scepter does to a creature it hits. (In the films the Mind Stone takes over the mind of whoever it
 * touches; that is too strong, so here it only confuses.) A confused creature forgets its target and can't pick a new
 * one (see {@code CommonEvents.onChangeTarget}), and stumbles about aimlessly: it heads for a random spot nearby, a
 * little faster than it wanders when idle, and changes its mind every few ticks. Small blue-green sparks, the Mind
 * Stone's colours, swirl round its head. The bosses are immune (the {@code enhanced:confused_immune} tag).
 */
public class ConfusedEffect extends MobEffect {
    /** The Mind Stone's light in Loki's Scepter: a blue-green glow, from the bright core out to the deep blue. */
    public static final int CORE = 0xD2FFF8;
    public static final int MAIN = 0x45D8D0;
    public static final int BODY = 0x2E9BD6;

    /** How fast it stumbles, as a multiple of its walking speed (an idle wander is 1.0). */
    private static final double STUMBLE_SPEED = 1.2;
    /** It picks a new way to stumble when it gets there, or on average this many ticks anyway. */
    private static final int TURN_EVERY = 10;
    private static final int[] SPARKS = {CORE, MAIN, BODY};

    public ConfusedEffect() {
        super(MobEffectCategory.HARMFUL, MAIN);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            stumble(mob);
        }
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 2 == 0) {
            swirl(level, entity);
        }
        return true;
    }

    /** Sends the creature off towards a random spot nearby, over and over: never to anywhere in particular. */
    private static void stumble(Mob mob) {
        RandomSource random = mob.getRandom();
        if (mob.isPassenger() || (!mob.getNavigation().isDone() && random.nextInt(TURN_EVERY) != 0)) {
            return;
        }
        Vec3 spot = mob instanceof PathfinderMob pathfinder ? DefaultRandomPos.getPos(pathfinder, 6, 3) : null;
        if (spot == null) {
            spot = mob.position().add(random.nextGaussian() * 3.0, random.nextGaussian(), random.nextGaussian() * 3.0);
        }
        Brain<?> brain = mob.getBrain();
        if (brain.checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED)) {
            // Piglins, villagers, goats...: their brains walk where the walk target says.
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, (float) STUMBLE_SPEED, 0));
        } else if (!mob.getNavigation().moveTo(spot.x, spot.y, spot.z, STUMBLE_SPEED)) {
            // No path (it flies, swims or is boxed in): just push it that way.
            mob.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, STUMBLE_SPEED);
        }
        mob.getLookControl().setLookAt(spot.x, mob.getEyeY(), spot.z);
    }

    /** Two sparks chase each other round the head, rising and falling, trailing along their path. */
    private static void swirl(ServerLevel level, LivingEntity entity) {
        double radius = entity.getBbWidth() * 0.5 + 0.2;
        double t = entity.tickCount;
        for (int i = 0; i < 2; i++) {
            double angle = t * 0.5 + i * Math.PI;
            double r = radius + Math.sin(t * 0.2 + i) * 0.08;
            double y = entity.getEyeY() + 0.3 + Math.sin(t * 0.3 + i * 2.1) * 0.12;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            // Moving along the circle, so each spark leaves an arc.
            level.sendParticles(new EnergyParticleOptions(SPARKS[(entity.tickCount / 2 + i) % SPARKS.length], 0.7F),
                    entity.getX() + cos * r, y, entity.getZ() + sin * r, 0, -sin, 0.1, cos, 0.12);
        }
    }

    /**
     * The moment the Scepter lands: a small burst of the Mind Stone's light round the creature's head, and a chime with
     * the stone's own voice under it.
     */
    public static void burst(ServerLevel level, LivingEntity entity) {
        double x = entity.getX();
        double y = entity.getEyeY();
        double z = entity.getZ();
        for (int i = 0; i < 12; i++) {
            double angle = i * Mth.TWO_PI / 12;
            level.sendParticles(new EnergyParticleOptions(SPARKS[i % SPARKS.length], 1.1F), x + Math.cos(angle) * 0.3, y, z + Math.sin(angle) * 0.3,
                    0, Math.cos(angle), 0.2, Math.sin(angle), 0.18);
        }
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.4F);
        InfinityStone.MIND.use(level, x, y, z, 0.5F, 1.2F);
    }
}

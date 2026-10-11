package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What the boss and the hero share of the sand: who it may hurt, how it hurts, where the ground is, and how a creature
 * is swallowed in a cocoon. The boss's attacks and the hero's Wave and Entomb both call these, so what he does to you in
 * the fight is what you do after it. {@link SandHazards} holds the objects (waves, rings, tornadoes) that are built on
 * them.
 *
 * <p>Nothing here ever places or breaks a block: every effect of the sand is an entity, a particle or a picture the
 * client draws, so mobGriefing and the protection of an arena change nothing.
 */
public final class SandControl {
    /** The damage a cocoon never takes a creature below: two hearts. */
    public static final float ENTOMB_FLOOR = 4.0F;
    /** How long a creature's Entombed effect lasts when the cocoon does not refresh it: the cocoon renews it every tick. */
    public static final int EFFECT_TICKS = 6;

    /**
     * Whether a hazard of {@code owner} may hurt {@code victim}. Never the owner, Creative or Spectator players, armor
     * stands, or anything on the owner's side. <b>The boss's</b> hazards hurt players (whatever PvP says: he is a mob),
     * their pets and other creatures that belong to no fight, and never the boss, the Horsemen or the Husks of the fight.
     * <b>The hero's</b> hazards never hurt his own pets or team, or players unless PvP is on.
     */
    public static boolean canHurt(LivingEntity owner, LivingEntity victim) {
        if (!victim.isAlive() || victim.isSpectator()) {
            return false;
        }
        boolean villain = !(owner instanceof Player);
        return AreaTargets.mayHurt(owner, victim, villain);
    }

    /** The sand's damage: caused by {@code owner}, struck by it (so a Chaos Shield that stops a hazard counts it as one). */
    public static boolean hurt(ServerLevel level, @Nullable LivingEntity owner, LivingEntity victim, float amount) {
        return victim.hurt(ModDamageTypes.source(level, ModDamageTypes.SAND, owner, owner), amount);
    }

    /**
     * The top of the ground in the column of {@code (x, z)} found by looking down from {@code y}: where the sand rolls.
     * (Not the height map: under a ceiling or a canopy it is the ground under it, and in a closed test arena too.)
     */
    public static double groundAt(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(y), Math.floor(z));
        int bottom = level.getMinBuildHeight();
        int top = level.getMaxBuildHeight();
        // If the hint is inside the ground, climb out of it first.
        for (int i = 0; i < 16 && pos.getY() < top && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(); i++) {
            pos.move(0, 1, 0);
        }
        for (int i = 0; i < 96 && pos.getY() > bottom; i++, pos.move(0, -1, 0)) {
            var shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return pos.getY() + shape.max(Direction.Axis.Y);
            }
        }
        return bottom;
    }

    /** The unit vector of {@code from} to {@code to} on the ground plane (zero if they are above each other). */
    public static Vec3 flat(Vec3 from, Vec3 to) {
        Vec3 flat = new Vec3(to.x - from.x, 0.0, to.z - from.z);
        double length = flat.length();
        return length < 1.0E-6 ? Vec3.ZERO : flat.scale(1.0 / length);
    }

    /** A puff of sand where something of the sand ends. */
    public static void puff(ServerLevel level, Vec3 at, int count, double spread) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), at.x, at.y, at.z, count,
                spread, spread, spread, 0.08);
    }

    // ---- the Entomb ----

    /** Whether the player holds a complete shield of their own: a Chaos Shield or Apocalypse's, which the grasp skips. */
    public static boolean hasCompleteShield(Player player) {
        PowerData data = Powers.get(player);
        return (data.power() == Power.SCARLET_WITCH || data.power() == Power.APOCALYPSE) && data.chaosShieldUp(player.level().getGameTime());
    }

    /**
     * Why the sand cannot swallow {@code victim}, as the key of the message the owner or the victim reads, or null if it
     * can: a complete shield ("the shield protects"), a phased Vision, a Creative or Spectator player, a creature of the
     * bosses' tag, one already in a cocoon, or a dead one.
     */
    @Nullable
    public static String entombRefusal(LivingEntity victim) {
        if (!victim.isAlive() || victim.isSpectator() || victim.getType().is(ModTags.ENTOMB_IMMUNE)
                || victim.getType().is(ModTags.SPELLBOUND_IMMUNE) && !(victim instanceof Player)) {
            return "message.enhanced.entomb.slips";
        }
        if (victim instanceof Player player) {
            if (player.isCreative() || VisionAbilities.isPhased(player)) {
                return "message.enhanced.entomb.slips";
            }
            if (hasCompleteShield(player)) {
                return "message.enhanced.apocalypse.shield_protects";
            }
        }
        return SandCocoonEntity.cocoonOf(victim) != null ? "message.enhanced.entomb.slips" : null;
    }

    /**
     * Swallows {@code victim} in a cocoon of sand for {@code ticks}: it cannot move, jump, fly, attack, use items or use
     * its hero abilities, takes {@code damagePerSecond} a second but never below two hearts, and the cocoon (of {@code
     * health}) breaks when anything hits it. A boss's cocoon belongs to its fight (marked, discarded with it).
     *
     * @param owner the one who casts it, or null
     * @param fight the fight the cocoon belongs to, or null for a hero's
     * @return the cocoon, or null if the creature cannot be swallowed (see {@link #entombRefusal})
     */
    @Nullable
    public static SandCocoonEntity entomb(ServerLevel level, @Nullable LivingEntity owner, LivingEntity victim, int ticks,
                                          float damagePerSecond, float health, @Nullable BossFight fight) {
        if (entombRefusal(victim) != null) {
            return null;
        }
        SandCocoonEntity cocoon = new SandCocoonEntity(level, victim, owner, ticks, damagePerSecond, health);
        victim.stopRiding();
        victim.addEffect(new MobEffectInstance(ModEffects.ENTOMBED, EFFECT_TICKS, 0, false, false, true), owner);
        if (fight != null) {
            fight.spawn(cocoon);
        } else {
            level.addFreshEntity(cocoon);
        }
        return cocoon;
    }

    private SandControl() {
    }
}

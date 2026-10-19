package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.thanos.StonePowers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * What a stone's holder looks at (3.6), the same way the hero Thanos's stones see it: the first creature along his line
 * of sight that the stone may take, unless a block is nearer; and who a stone aimed at one creature may strike (not an
 * armour stand, a Creative player or an ally; a player only with PvP). Server only.
 */
public final class StoneAim {

    /** What the stone sees along his look: the creature in it, if any, and where the line ends. */
    public record Aim(@Nullable LivingEntity creature, Vec3 point, boolean block) {
    }

    /**
     * What he looks at within {@code range}: the first creature {@code wanted} accepts (its hitbox inflated by {@code
     * inflate}) unless a block is nearer, otherwise the first block, otherwise the end of the range.
     */
    public static Aim aim(ServerPlayer player, double range, double inflate, Predicate<LivingEntity> wanted) {
        EntityHitResult entity = Raycasts.entity(player, range, inflate, e -> {
            LivingEntity creature = creatureOf(e);
            return creature != null && creature != player && creature.isAlive() && !creature.isSpectator()
                    && wanted.test(creature);
        }, 1.0F);
        if (entity != null) {
            return new Aim(creatureOf(entity.getEntity()), entity.getLocation(), false);
        }
        BlockHitResult block = Raycasts.block(player, range, 1.0F);
        if (block.getType() != HitResult.Type.MISS) {
            return new Aim(null, block.getLocation(), true);
        }
        return new Aim(null, player.getEyePosition().add(player.getViewVector(1.0F).scale(range)), false);
    }

    /** The creature an entity is, or the creature a part of it belongs to (the Ender Dragon's body). */
    @Nullable
    public static LivingEntity creatureOf(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity parent ? parent : null;
    }

    /** A creature a single, aimed stone may strike: not untouchable or an ally; a player only with PvP. */
    public static boolean mayStrike(ServerPlayer holder, LivingEntity target) {
        return !StonePowers.untouchable(target) && AreaTargets.mayHurt(holder, target, true);
    }

    /** Whether a player is out of the stone's reach because PvP is off (or he is a friend): and says so. */
    public static boolean pvpRefuses(ServerPlayer holder, LivingEntity target) {
        if (target instanceof Player other && !other.isCreative() && !other.isSpectator()
                && !holder.canHarmPlayer(other)) {
            holder.displayClientMessage(Component.translatable("message.enhanced.thanos.pvp_off"), true);
            return true;
        }
        return false;
    }

    /** The middle of a creature's body, where a chain or a sphere goes. */
    public static Vec3 chestOf(Entity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.6, 0.0);
    }

    /** Where the stone is: the hand that holds it, a little ahead of him at the height of the chest. */
    public static Vec3 hand(Player player) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        if (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT) {
            right = right.scale(-1.0);
        }
        double size = player.getScale();
        return player.position().add(right.scale(0.35 * size)).add(forward.scale(0.4 * size))
                .add(0.0, player.getBbHeight() * 0.72, 0.0);
    }

    private StoneAim() {
    }
}

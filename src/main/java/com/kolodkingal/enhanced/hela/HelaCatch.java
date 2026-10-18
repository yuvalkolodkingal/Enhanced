package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.ability.MjolnirShatter;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Hela's hand against Thor's weapons (3.5), the moment of Ragnarok: a thrown Mjolnir that comes within reach of her hand
 * is caught and crushed ({@link MjolnirShatter#shatterCaught}: it bursts into fragments that strike Thor's enemies, and
 * comes whole into his hand again when the last one is home); a thrown Stormbreaker is not hers to break: the boss lets it
 * by, and the hero's Death's Grasp only stops it (it drops to the ground). Used by the boss's fight and the hero's grasp.
 */
public final class HelaCatch {

    /**
     * Catches and crushes a thrown Mjolnir within {@code reach} of {@code hand}, if there is one (the first found).
     *
     * @return whether one was caught
     */
    public static boolean catchNear(ServerLevel level, LivingEntity catcher, Vec3 hand, double reach) {
        for (ThrownWeaponEntity thrown : level.getEntitiesOfClass(ThrownWeaponEntity.class, new AABB(hand, hand).inflate(reach),
                e -> e.isAlive() && e.position().distanceToSqr(hand) <= reach * reach)) {
            if (ThorAbilities.isStormbreaker(thrown.weapon()) || !(thrown.getOwner() instanceof ServerPlayer thor) || thor == catcher) {
                continue;
            }
            crush(level, thrown, thor, hand);
            return true;
        }
        return false;
    }

    /** The hammer is in her hand: she closes it, and it bursts. */
    static void crush(ServerLevel level, ThrownWeaponEntity thrown, ServerPlayer thor, Vec3 hand) {
        ItemStack hammer = thrown.weapon().copy();
        forget(thor, thrown);
        thrown.discard();
        level.playSound(null, hand.x, hand.y, hand.z, ModSounds.MJOLNIR_CAUGHT.get(), SoundSource.HOSTILE, 2.5F, 1.0F);
        HelaFx.send(level, HelaFx.CRUSH, thrown.getId(), hand, 0);
        if (!MjolnirShatter.shatterCaught(thor, hammer, hand)) {
            // (Already in pieces: the hammer simply falls at her feet, Thor's still.)
            ThorAbilities.dropAt(level, hand, hammer);
        }
    }

    /**
     * Death's Grasp stops a thrown Stormbreaker: it drops to the ground before her. Returns whether one was within
     * {@code reach} of {@code hand}.
     */
    public static boolean stopNear(ServerLevel level, LivingEntity catcher, Vec3 hand, double reach) {
        for (ThrownWeaponEntity thrown : level.getEntitiesOfClass(ThrownWeaponEntity.class, new AABB(hand, hand).inflate(reach),
                e -> e.isAlive() && e.position().distanceToSqr(hand) <= reach * reach)) {
            if (!ThorAbilities.isStormbreaker(thrown.weapon()) || thrown.getOwner() == catcher) {
                continue;
            }
            if (thrown.getOwner() instanceof ServerPlayer thor) {
                forget(thor, thrown);
            }
            ItemStack axe = thrown.weapon().copy();
            Vec3 at = thrown.position();
            thrown.discard();
            ThorAbilities.dropAt(level, at, axe);
            HelaFx.send(level, HelaFx.CRUMBLE, catcher.getId(), at, 1);
            return true;
        }
        return false;
    }

    /** Thor no longer has a weapon in flight. */
    private static void forget(ServerPlayer thor, ThrownWeaponEntity thrown) {
        PowerData data = Powers.get(thor);
        if (data.hammerEntityId() == thrown.getId()) {
            data.setHammerEntityId(-1);
            Powers.syncIfDirty(thor);
        }
    }

    private HelaCatch() {
    }
}

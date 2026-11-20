package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.Demiurge;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What Billy the Demiurge looks like besides his starry suit (3.18.1): motes of the Demiurge's blue and white stars that
 * hang round him, and, while he uses any power (his own or the magic type's), stars streaming from his hands and twinkling
 * along a beam he holds. The stars inside the energy of every effect of his are {@code DemiurgeStars}'. Thinned by the
 * client's <i>Flight effects</i> option.
 */
public final class DemiurgeFx {
    private DemiurgeFx() {
    }

    /** Every client tick: every Demiurge in view. */
    public static void tick(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null) {
            return;
        }
        float density = EnhancedClientConfig.SPEC.isLoaded() && !EnhancedClientConfig.flightEffects() ? 0.35F : 1.0F;
        RandomSource random = level.random;
        for (Player player : level.players()) {
            PowerData data = Powers.get(player);
            if (!Demiurge.is(data) || player.isInvisible() || player.isSpectator() || player.distanceToSqr(mc.player) > 64 * 64) {
                continue;
            }
            boolean firstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
            // the aura: motes of blue rising round him, and now and then a star
            for (int i = 0; i < (firstPerson ? 1 : 3); i++) {
                if (random.nextFloat() > density) {
                    continue;
                }
                double a = random.nextDouble() * Math.PI * 2;
                double r = 0.45 + random.nextDouble() * 0.25;
                double y = player.getY() + random.nextDouble() * player.getBbHeight();
                int color = random.nextInt(4) == 0 ? Demiurge.STAR : random.nextInt(3) == 0 ? Demiurge.AURA : Demiurge.BLUE;
                level.addParticle(new EnergyParticleOptions(color, 0.5F + random.nextFloat() * 0.35F),
                        player.getX() + Math.cos(a) * r, y, player.getZ() + Math.sin(a) * r,
                        -Math.cos(a) * 0.01, 0.02 + random.nextDouble() * 0.02, -Math.sin(a) * 0.01);
            }
            if (!firstPerson && level.getGameTime() % 5 == 0 && random.nextFloat() < density) {
                double a = random.nextDouble() * Math.PI * 2;
                level.addParticle(ParticleTypes.END_ROD, player.getX() + Math.cos(a) * 0.6,
                        player.getY() + 0.3 + random.nextDouble() * 1.5, player.getZ() + Math.sin(a) * 0.6, 0, 0.005, 0);
            }
            if (!acting(data)) {
                continue;
            }
            // using a power: stars from his hands
            Vec3 look = player.getLookAngle();
            Vec3 hand = player.getEyePosition().add(0, -0.45, 0).add(look.scale(0.55));
            for (int i = 0; i < 2; i++) {
                if (random.nextFloat() > density) {
                    continue;
                }
                level.addParticle(ParticleTypes.END_ROD, hand.x + (random.nextDouble() - 0.5) * 0.5, hand.y + (random.nextDouble() - 0.5) * 0.4,
                        hand.z + (random.nextDouble() - 0.5) * 0.5, look.x * 0.03, look.y * 0.03 + 0.01, look.z * 0.03);
            }
            if (data.is(Live.BEAMING) || data.beamActive()) {
                // and twinkling along the beam he holds, out to what it strikes
                Vec3 eye = player.getEyePosition();
                HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(32.0)), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
                double length = hit.getLocation().distanceTo(eye);
                for (int i = 0; i < 4; i++) {
                    if (random.nextFloat() > density) {
                        continue;
                    }
                    Vec3 p = hand.add(look.scale(random.nextDouble() * Math.max(1.0, length - 0.6)));
                    level.addParticle(random.nextInt(3) == 0 ? new EnergyParticleOptions(Demiurge.STAR, 0.6F) : ParticleTypes.END_ROD,
                            p.x + (random.nextDouble() - 0.5) * 0.18, p.y + (random.nextDouble() - 0.5) * 0.18,
                            p.z + (random.nextDouble() - 0.5) * 0.18, 0, 0, 0);
                }
            }
        }
    }

    /** Whether he is using a power right now: casting, beaming, shielding, holding something, or any power's moment. */
    static boolean acting(PowerData data) {
        if (data.beamActive() || data.chaosShieldActive() || data.heldEntityId() != -1 || data.shrapnelActive()) {
            return true;
        }
        for (Live live : Live.values()) {
            if (live != Live.BILLY_DEMIURGE && live != Live.CROWNED && live != Live.SPEEDING && data.is(live)) {
                return true;
            }
        }
        return false;
    }
}

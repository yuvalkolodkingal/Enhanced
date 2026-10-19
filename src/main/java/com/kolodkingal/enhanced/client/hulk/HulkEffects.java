package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.HulkActing;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Hulk's effects on the client (3.6): the drawers of {@link HulkFxPayload} ({@link HulkFx} hands each kind here).
 * Shock rings of the ground's dust running out from the Smash and the landing, cracks of the ground (particles: no
 * texture changes), the Thunder Clap's air ripple (a cone of puffs racing forward), the leap's trail of dust, the
 * boulder's shattering, the charge's dust, the green shimmer of the transformation and the gamma flash of the accident,
 * and the camera's shake for a player near a blow (not with the client option {@code reduceMotion}). Every payload that
 * names the Hulk also plays his gesture ({@link HulkActing#onPayload}).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class HulkEffects {
    /** A ring of dust running out over the ground. */
    private record Ring(Vec3 center, float radius, long start, float speed, BlockState block) {
    }

    /** A wave of air racing forward in a cone. */
    private record Wave(Vec3 origin, Vec3 dir, float length, long start) {
    }

    /** A shimmer of green round a changing Hulk. */
    private record Shimmer(int entityId, long start, int ticks) {
    }

    private static final List<Ring> RINGS = new ArrayList<>();
    private static final List<Wave> WAVES = new ArrayList<>();
    private static final List<Shimmer> SHIMMERS = new ArrayList<>();
    /** The camera's shake (degrees) and the gamma flash (0-1), both fading. */
    private static float shake;
    private static float flash;

    /** Once, from the client's setup: the drawer of every kind. */
    public static void register() {
        for (int kind = HulkFxPayload.TRANSFORM_OUT; kind <= HulkFxPayload.GAMMA_BEAM; kind++) {
            HulkFx.register(kind, HulkEffects::receive);
        }
    }

    static void receive(HulkFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        HulkActing.onPayload(p);
        Vec3 at = p.points().isEmpty() ? null : p.points().get(0);
        Entity who = p.entityId() >= 0 ? level.getEntity(p.entityId()) : null;
        long now = level.getGameTime();
        switch (p.kind()) {
            case HulkFxPayload.SMASH -> {
                if (p.arg() > 0 && at != null) {
                    RINGS.add(new Ring(at, p.arg(), now, 1.0F, ground(level, at)));
                    cracks(level, at, 10);
                    shake(at, 2.5F);
                }
            }
            case HulkFxPayload.LAND -> {
                if (at != null) {
                    if (p.arg() > 0) {
                        RINGS.add(new Ring(at, p.arg() + 1.0F, now, 1.5F, ground(level, at)));
                        cracks(level, at, 8);
                        shake(at, 3.0F);
                    } else {
                        puff(level, at, ground(level, at), 14);
                        shake(at, 0.6F);
                    }
                }
            }
            case HulkFxPayload.CLAP -> {
                if (p.arg() > 0 && p.points().size() >= 2) {
                    Vec3 dir = p.points().get(1).subtract(at);
                    WAVES.add(new Wave(at, dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize(), p.arg(),
                            now));
                    level.addParticle(ParticleTypes.EXPLOSION, at.x + dir.x * 0.9, at.y, at.z + dir.z * 0.9, 0, 0, 0);
                    shake(at, 1.6F);
                }
            }
            case HulkFxPayload.ROAR -> {
                if (at != null) {
                    roar(level, at, who);
                    shake(at, 0.8F);
                }
            }
            case HulkFxPayload.LEAP, HulkFxPayload.BOUND -> {
                if (at != null) {
                    puff(level, at, ground(level, at), p.kind() == HulkFxPayload.LEAP ? 24 : 10);
                }
            }
            case HulkFxPayload.RIP -> {
                if (at != null) {
                    BlockState block = Block.stateById(p.arg());
                    cracks(level, at, 6);
                    puff(level, at, block.isAir() ? ground(level, at) : block, 20);
                }
            }
            case HulkFxPayload.BOULDER_HIT -> {
                if (at != null) {
                    BlockState block = Block.stateById(p.arg());
                    puff(level, at, block.isAir() ? Blocks.STONE.defaultBlockState() : block, 24);
                    shake(at, 1.0F);
                }
            }
            case HulkFxPayload.GRAB_SLAM -> {
                if (at != null && p.arg() == 0) {
                    puff(level, at, ground(level, at), 12);
                    shake(at, 1.2F);
                }
            }
            case HulkFxPayload.CHARGE -> {
                if (at != null) {
                    puff(level, at, ground(level, at), 10);
                }
            }
            case HulkFxPayload.TRANSFORM_OUT, HulkFxPayload.TRANSFORM_IN, HulkFxPayload.SWITCH -> {
                SHIMMERS.add(new Shimmer(p.entityId(), now, Math.max(10, p.arg())));
                if (p.kind() == HulkFxPayload.TRANSFORM_OUT && who == mc.player) {
                    flash = Math.max(flash, 0.35F);
                }
            }
            case HulkFxPayload.GAMMA_BEAM -> {
                if (p.arg() == 1 && who == mc.player) {
                    flash = 1.0F;
                }
            }
            default -> {
            }
        }
    }

    /** The block the ground is made of under {@code at}. */
    static BlockState ground(ClientLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at.x, at.y - 0.2, at.z);
        BlockState state = level.getBlockState(pos);
        return state.isAir() ? Blocks.DIRT.defaultBlockState() : state;
    }

    private static void puff(ClientLevel level, Vec3 at, BlockState block, int count) {
        RandomSource r = level.random;
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double s = 0.1 + r.nextDouble() * 0.25;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block), at.x, at.y + 0.2, at.z,
                    Math.cos(a) * s,
                    0.15 + r.nextDouble() * 0.25, Math.sin(a) * s);
        }
        for (int i = 0; i < count / 3; i++) {
            level.addParticle(ParticleTypes.POOF, at.x + (r.nextDouble() - 0.5) * 1.5, at.y + 0.2, at.z
                    + (r.nextDouble() - 0.5) * 1.5,
                    (r.nextDouble() - 0.5) * 0.1, 0.05, (r.nextDouble() - 0.5) * 0.1);
        }
    }

    /** Cracks running out over the ground: lines of the ground's crumbs. */
    private static void cracks(ClientLevel level, Vec3 at, int lines) {
        RandomSource r = level.random;
        BlockState block = ground(level, at);
        for (int i = 0; i < lines; i++) {
            double a = (i + r.nextDouble() * 0.5) * Math.PI * 2.0 / lines;
            double length = 2.0 + r.nextDouble() * 2.5;
            for (double d = 0.5; d < length; d += 0.35) {
                double wobble = (r.nextDouble() - 0.5) * 0.25;
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block), at.x + Math.cos(a + wobble) * d,
                        at.y + 0.05,
                        at.z + Math.sin(a + wobble) * d, 0, 0.02, 0);
            }
        }
    }

    private static void roar(ClientLevel level, Vec3 head, Entity who) {
        RandomSource r = level.random;
        Vec3 look = who != null ? who.getLookAngle() : new Vec3(0, 0, 1);
        for (int i = 0; i < 30; i++) {
            Vec3 d = look.add((r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 0.5, (r.nextDouble() - 0.5) * 0.9)
                    .normalize();
            level.addParticle(ParticleTypes.CLOUD, head.x + d.x, head.y + d.y * 0.5, head.z + d.z, d.x * 0.5, d.y * 0.2,
                    d.z * 0.5);
        }
        for (int i = 0; i < 12; i++) {
            level.addParticle(new EnergyParticleOptions(HulkAbilities.GAMMA, 0.6F), head.x, head.y, head.z,
                    (r.nextDouble() - 0.5) * 0.4,
                    (r.nextDouble() - 0.5) * 0.2, (r.nextDouble() - 0.5) * 0.4);
        }
    }

    /** The camera shakes for the local player within reach of a blow at {@code at}. */
    private static void shake(Vec3 at, float strength) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        double d = player.position().distanceTo(at);
        if (d > HulkAbilities.SHAKE_RANGE) {
            return;
        }
        shake = Math.max(shake, (float) (strength * (1.0 - d / HulkAbilities.SHAKE_RANGE)));
    }

    /** The flash's strength now (0 to 1), for the overlay. */
    public static float flash() {
        return flash;
    }

    /** Every client tick: the rings and waves run on, the trails of the leaping and charging Hulks, the shimmers. */
    public static void tick(ClientLevel level) {
        long now = level.getGameTime();
        RandomSource r = level.random;
        Iterator<Ring> rings = RINGS.iterator();
        while (rings.hasNext()) {
            Ring ring = rings.next();
            float radius = (now - ring.start()) * ring.speed();
            if (radius > ring.radius() || now < ring.start()) {
                rings.remove();
                continue;
            }
            int n = Math.max(8, (int) (radius * 7));
            for (int i = 0; i < n; i++) {
                double a = (i + r.nextDouble()) * Math.PI * 2.0 / n;
                double x = ring.center().x + Math.cos(a) * radius;
                double z = ring.center().z + Math.sin(a) * radius;
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ring.block()), x, ring.center().y + 0.1,
                        z,
                        Math.cos(a) * 0.15, 0.25 + r.nextDouble() * 0.15, Math.sin(a) * 0.15);
                if (i % 3 == 0) {
                    level.addParticle(ParticleTypes.POOF, x, ring.center().y + 0.2, z, Math.cos(a) * 0.05, 0.02,
                            Math.sin(a) * 0.05);
                }
            }
        }
        Iterator<Wave> waves = WAVES.iterator();
        while (waves.hasNext()) {
            Wave wave = waves.next();
            float front = (now - wave.start()) * 1.4F;
            if (front > wave.length()) {
                waves.remove();
                continue;
            }
            Vec3 side = new Vec3(-wave.dir().z, 0, wave.dir().x);
            double half = Math.tan(Math.toRadians(25.0)) * front;
            for (int i = -4; i <= 4; i++) {
                Vec3 p = wave.origin().add(wave.dir().scale(front)).add(side.scale(half * i / 4.0));
                level.addParticle(ParticleTypes.CLOUD, p.x, p.y + (r.nextDouble() - 0.5) * 1.2, p.z, wave.dir().x * 0.3,
                        0.0,
                        wave.dir().z * 0.3);
                if (i % 2 == 0) {
                    level.addParticle(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 0, 0, 0);
                }
            }
        }
        for (Player player : level.players()) {
            var data = Powers.get(player);
            if (data.is(Live.HULK_LEAPING) && !player.onGround() && now % 2 == 0) {
                level.addParticle(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.3, player.getZ(), 0, 0, 0);
            }
            if (data.is(Live.HULK_CHARGING)) {
                puff(level, player.position(), ground(level, player.position()), 4);
            }
            if (player instanceof AbstractClientPlayer p && HulkPlayerRenderer.drawsAsHulk(p) && p.isSprinting()
                    && p.onGround()
                    && p.walkAnimation.speed() > 0.6F && (p.tickCount % 6 == 0)) {
                // A ground thump and a small puff at every sprint step.
                puff(level, p.position(), ground(level, p.position()), 3);
            }
        }
        Iterator<Shimmer> shimmers = SHIMMERS.iterator();
        while (shimmers.hasNext()) {
            Shimmer s = shimmers.next();
            if (now - s.start() > s.ticks()) {
                shimmers.remove();
                continue;
            }
            Entity e = level.getEntity(s.entityId());
            if (e == null) {
                continue;
            }
            for (int i = 0; i < 6; i++) {
                double a = r.nextDouble() * Math.PI * 2.0;
                double rad = e.getBbWidth() * 0.8;
                level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? HulkAbilities.GREEN : HulkAbilities.GAMMA,
                        0.7F),
                        e.getX() + Math.cos(a) * rad, e.getY() + r.nextDouble() * e.getBbHeight(), e.getZ()
                                + Math.sin(a) * rad,
                        0, 0.03, 0);
            }
        }
        shake *= 0.82F;
        flash *= 0.93F;
        if (shake < 0.02F) {
            shake = 0.0F;
        }
        if (flash < 0.01F) {
            flash = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (shake <= 0.01F || EnhancedClientConfig.reducedMotion() || Minecraft.getInstance().player == null) {
            return;
        }
        float t = (float) event.getPartialTick() + Minecraft.getInstance().player.tickCount;
        event.setPitch(event.getPitch() + shake * Mth.sin(t * 2.6F));
        event.setRoll(event.getRoll() + shake * 0.7F * Mth.sin(t * 1.9F + 1.0F));
    }

    public static void clear() {
        RINGS.clear();
        WAVES.clear();
        SHIMMERS.clear();
        shake = 0.0F;
        flash = 0.0F;
    }

    private HulkEffects() {
    }
}

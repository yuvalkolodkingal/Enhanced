package com.kolodkingal.enhanced.client.redhulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.RedHulkActing;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.redhulk.Anger;
import com.kolodkingal.enhanced.redhulk.RedHulkAbilities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Red Hulk's effects on the client (3.8, lane L2): the drawers of {@link RedHulkFxPayload} ({@link RedHulkFx} hands
 * each kind here). The Ground Smash's ring of the ground's own crumbs running out over the blocks under it (no block
 * changes), the Heat Wave's cone of flame and shimmer, the Roar's breath, the jump's dust and the landing's ring, the
 * Anger Burst's ring of fire with its flash, the red light pulled into him while he absorbs, the steam of the change;
 * and on every Red Hulk in view: steam off his shoulders from anger 75 ({@code Live.RED_HULK_HEATED}), his hot breath,
 * embers when he is close to bursting. The loops: {@code red_hulk_steam} while he steams and {@code red_hulk_absorb}
 * while he absorbs. The camera shakes for a player near a blow (not with the client option {@code reduceMotion}).
 * Every payload that names him also plays his gesture ({@link RedHulkActing#onPayload}).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class RedHulkEffects {
    /** A ring running out over the ground: the smash's crumbs ({@code fire} false) or the burst's flames. */
    private record Ring(Vec3 center, float radius, long start, float speed, boolean fire) {
    }

    /** The heat wave rolling out in a cone. */
    private record Wave(Vec3 origin, Vec3 dir, float length, long start) {
    }

    /** Steam round a changing Red Hulk. */
    private record Steam(int entityId, long start, int ticks) {
    }

    /** Ember-orange, the flame's yellow and the absorb's red light. */
    static final int EMBER = 0xFF6A1A;
    static final int FLAME = 0xFFC040;
    static final int ABSORB = 0xFF2A2A;

    private static final List<Ring> RINGS = new ArrayList<>();
    private static final List<Wave> WAVES = new ArrayList<>();
    private static final List<Steam> STEAMS = new ArrayList<>();
    /** The loops playing, by the entity they follow. */
    private static final Map<Integer, Loop> STEAM_LOOPS = new HashMap<>();
    private static final Map<Integer, Loop> ABSORB_LOOPS = new HashMap<>();
    /** The camera's shake (degrees) and the burst's flash (0-1), both fading. */
    private static float shake;
    private static float flash;

    /** Once, from the client's setup: the drawer of every kind. */
    public static void register() {
        for (int kind = RedHulkFxPayload.SMASH; kind <= RedHulkFxPayload.TRANSFORM_OUT; kind++) {
            RedHulkFx.register(kind, RedHulkEffects::receive);
        }
    }

    static void receive(RedHulkFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        RedHulkActing.onPayload(p);
        Vec3 at = p.points().isEmpty() ? null : p.points().get(0);
        Entity who = p.entityId() >= 0 ? level.getEntity(p.entityId()) : null;
        long now = level.getGameTime();
        if (at == null) {
            return;
        }
        switch (p.kind()) {
            case RedHulkFxPayload.SMASH -> {
                if (p.arg() > 0) {
                    RINGS.add(new Ring(at, p.arg(), now, 1.0F, false));
                    cracks(level, at, 12);
                    shake(at, 2.8F);
                }
            }
            case RedHulkFxPayload.HEAT_WAVE -> {
                if (p.points().size() >= 2) {
                    Vec3 dir = p.points().get(1).subtract(at);
                    WAVES.add(new Wave(at, dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize(),
                            Math.max(2, p.arg()), now));
                    shake(at, 0.8F);
                }
            }
            case RedHulkFxPayload.ROAR -> {
                roar(level, at, who);
                shake(at, 1.0F);
            }
            case RedHulkFxPayload.JUMP_CHARGE -> puff(level, at, ground(level, at), 8);
            case RedHulkFxPayload.JUMP -> {
                puff(level, at, ground(level, at), 30);
                for (int i = 0; i < 2; i++) {
                    level.addParticle(ParticleTypes.EXPLOSION, at.x + (level.random.nextDouble() - 0.5) * 2.0,
                            at.y + 0.2, at.z + (level.random.nextDouble() - 0.5) * 2.0, 0, 0, 0);
                }
                shake(at, 1.5F);
            }
            case RedHulkFxPayload.LAND -> {
                RINGS.add(new Ring(at, Math.max(2, p.arg()) + 1.0F, now, 1.5F, false));
                cracks(level, at, 9);
                shake(at, 3.0F);
            }
            case RedHulkFxPayload.BURST -> {
                RINGS.add(new Ring(at, Math.max(2, p.arg()), now, Math.max(2, p.arg()) / 10.0F, true));
                level.addParticle(ParticleTypes.FLASH, at.x, at.y + 1.0, at.z, 0, 0, 0);
                level.addParticle(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5, at.z, 0, 0, 0);
                if (who == mc.player || mc.player != null && mc.player.position().distanceTo(at) < 8.0) {
                    flash = Math.max(flash, who == mc.player ? 0.45F : 0.25F);
                }
                shake(at, 4.0F);
            }
            case RedHulkFxPayload.ABSORB_HIT -> {
                for (int i = 0; i < 16; i++) {
                    Vec3 from = at.add((level.random.nextDouble() - 0.5) * 3.0, (level.random.nextDouble() - 0.5)
                            * 3.0, (level.random.nextDouble() - 0.5) * 3.0);
                    Vec3 v = at.subtract(from).scale(0.18);
                    level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? ABSORB : EMBER, 0.8F), from.x, from.y,
                            from.z, v.x, v.y, v.z);
                }
            }
            case RedHulkFxPayload.TRANSFORM_OUT, RedHulkFxPayload.TRANSFORM_IN ->
                    STEAMS.add(new Steam(p.entityId(), now, Math.max(10, p.arg())));
            case RedHulkFxPayload.GRAB -> puff(level, at, ground(level, at), 6);
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

    /** The block under a spot of a ring (its own crumbs), or null over air. */
    private static BlockState under(ClientLevel level, double x, double y, double z) {
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos pos = BlockPos.containing(x, y - 0.2 - dy, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                return state;
            }
        }
        return null;
    }

    private static void puff(ClientLevel level, Vec3 at, BlockState block, int count) {
        RandomSource r = level.random;
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double s = 0.1 + r.nextDouble() * 0.3;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block), at.x, at.y + 0.2, at.z,
                    Math.cos(a) * s, 0.2 + r.nextDouble() * 0.3, Math.sin(a) * s);
        }
        for (int i = 0; i < count / 3; i++) {
            level.addParticle(ParticleTypes.POOF, at.x + (r.nextDouble() - 0.5) * 2.0, at.y + 0.2,
                    at.z + (r.nextDouble() - 0.5) * 2.0, (r.nextDouble() - 0.5) * 0.1, 0.05,
                    (r.nextDouble() - 0.5) * 0.1);
        }
    }

    /** Cracks running out over the ground: lines of the ground's crumbs. */
    private static void cracks(ClientLevel level, Vec3 at, int lines) {
        RandomSource r = level.random;
        for (int i = 0; i < lines; i++) {
            double a = (i + r.nextDouble() * 0.5) * Math.PI * 2.0 / lines;
            double length = 2.5 + r.nextDouble() * 3.0;
            for (double d = 0.5; d < length; d += 0.35) {
                double wobble = (r.nextDouble() - 0.5) * 0.25;
                double x = at.x + Math.cos(a + wobble) * d;
                double z = at.z + Math.sin(a + wobble) * d;
                BlockState block = under(level, x, at.y, z);
                if (block != null) {
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block), x, at.y + 0.05, z, 0,
                            0.02, 0);
                }
            }
        }
    }

    private static void roar(ClientLevel level, Vec3 head, Entity who) {
        RandomSource r = level.random;
        Vec3 look = who != null ? who.getLookAngle() : new Vec3(0, 0, 1);
        for (int i = 0; i < 30; i++) {
            Vec3 d = look.add((r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 0.5, (r.nextDouble() - 0.5) * 0.9)
                    .normalize();
            level.addParticle(ParticleTypes.CLOUD, head.x + d.x, head.y + d.y * 0.5, head.z + d.z, d.x * 0.5,
                    d.y * 0.2, d.z * 0.5);
        }
        for (int i = 0; i < 14; i++) {
            level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? EMBER : RedHulkAbilities.RED, 0.6F), head.x,
                    head.y, head.z, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.2,
                    (r.nextDouble() - 0.5) * 0.4);
        }
    }

    /** The camera shakes for the local player within reach of a blow at {@code at}. */
    private static void shake(Vec3 at, float strength) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        double d = player.position().distanceTo(at);
        if (d > RedHulkAbilities.SHAKE_RANGE) {
            return;
        }
        shake = Math.max(shake, (float) (strength * (1.0 - d / RedHulkAbilities.SHAKE_RANGE)));
    }

    /** The burst's flash now (0 to 1), for the overlay. */
    public static float flash() {
        return flash;
    }

    /** Every client tick: the rings and waves run on, the steam and the embers of every Red Hulk in view. */
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
                double y = ring.center().y + 0.1;
                if (ring.fire()) {
                    level.addParticle(ParticleTypes.FLAME, x, y + r.nextDouble() * 0.6, z, Math.cos(a) * 0.12,
                            0.05 + r.nextDouble() * 0.08, Math.sin(a) * 0.12);
                    if (i % 2 == 0) {
                        level.addParticle(new EnergyParticleOptions(i % 4 == 0 ? EMBER : FLAME, 0.9F), x, y + 0.4,
                                z, Math.cos(a) * 0.05, 0.08, Math.sin(a) * 0.05);
                    }
                    if (i % 12 == 0) {
                        level.addParticle(ParticleTypes.SMOKE, x, y + 0.6, z, 0, 0.04, 0);
                    }
                } else {
                    BlockState block = under(level, x, ring.center().y, z);
                    if (block != null) {
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block), x, y, z,
                                Math.cos(a) * 0.15, 0.25 + r.nextDouble() * 0.2, Math.sin(a) * 0.15);
                    }
                    if (i % 3 == 0) {
                        level.addParticle(ParticleTypes.POOF, x, y + 0.1, z, Math.cos(a) * 0.05, 0.02,
                                Math.sin(a) * 0.05);
                    }
                }
            }
        }
        Iterator<Wave> waves = WAVES.iterator();
        while (waves.hasNext()) {
            Wave wave = waves.next();
            float front = (now - wave.start()) * 1.6F;
            if (front > wave.length()) {
                waves.remove();
                continue;
            }
            Vec3 side = new Vec3(-wave.dir().z, 0, wave.dir().x).normalize();
            double half = Math.tan(Math.toRadians(25.0)) * front;
            for (int i = -5; i <= 5; i++) {
                Vec3 p = wave.origin().add(wave.dir().scale(front)).add(side.scale(half * i / 5.0));
                double y = p.y + (r.nextDouble() - 0.5) * 1.4;
                level.addParticle(ParticleTypes.FLAME, p.x, y, p.z, wave.dir().x * 0.25, 0.02, wave.dir().z * 0.25);
                if (i % 2 == 0) {
                    level.addParticle(new EnergyParticleOptions(i % 4 == 0 ? EMBER : FLAME, 0.8F), p.x, y, p.z,
                            wave.dir().x * 0.2, 0.03, wave.dir().z * 0.2);
                }
                if (i % 3 == 0) {
                    level.addParticle(ParticleTypes.SMOKE, p.x, y + 0.3, p.z, 0, 0.03, 0);
                }
            }
        }
        Iterator<Steam> steams = STEAMS.iterator();
        while (steams.hasNext()) {
            Steam s = steams.next();
            if (now - s.start() > s.ticks()) {
                steams.remove();
                continue;
            }
            Entity e = level.getEntity(s.entityId());
            if (e == null) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                double a = r.nextDouble() * Math.PI * 2.0;
                double rad = e.getBbWidth() * 0.7;
                level.addParticle(i % 2 == 0 ? ParticleTypes.CLOUD : new EnergyParticleOptions(EMBER, 0.6F),
                        e.getX() + Math.cos(a) * rad, e.getY() + r.nextDouble() * e.getBbHeight(),
                        e.getZ() + Math.sin(a) * rad, 0, 0.04, 0);
            }
        }
        for (Player player : level.players()) {
            bodyTick(level, player, now, r);
        }
        STEAM_LOOPS.values().removeIf(Loop::isStopped);
        ABSORB_LOOPS.values().removeIf(Loop::isStopped);
        shake *= 0.82F;
        flash *= 0.90F;
        if (shake < 0.02F) {
            shake = 0.0F;
        }
        if (flash < 0.01F) {
            flash = 0.0F;
        }
    }

    /** The steam, breath and embers of a Red Hulk in view, and his loops. */
    private static void bodyTick(ClientLevel level, Player player, long now, RandomSource r) {
        PowerData data = Powers.get(player);
        if (!RedHulkAbilities.hasBody(data) || player.isInvisible()) {
            return;
        }
        float anger = Anger.of(player);
        double h = player.getBbHeight();
        double w = player.getBbWidth();
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        if (data.is(Live.RED_HULK_HEATED) && now % 2 == 0) {
            // Steam off his shoulders, drifting up.
            for (int side = -1; side <= 1; side += 2) {
                Vec3 s = player.position().add(right.scale(side * w * 0.42)).add(0, h * 0.82, 0);
                level.addParticle(ParticleTypes.CLOUD, s.x + (r.nextDouble() - 0.5) * 0.3, s.y,
                        s.z + (r.nextDouble() - 0.5) * 0.3, 0, 0.05 + r.nextDouble() * 0.03, 0);
            }
        }
        if (anger >= 40.0F && now % 20 == 0) {
            // His hot breath: a puff from the mouth.
            Vec3 look = player.getLookAngle();
            Vec3 mouth = player.getEyePosition().add(look.scale(w * 0.5)).subtract(0, h * 0.06, 0);
            level.addParticle(ParticleTypes.POOF, mouth.x, mouth.y, mouth.z, look.x * 0.05, 0.01, look.z * 0.05);
        }
        if (anger >= 90.0F && now % 3 == 0) {
            level.addParticle(new EnergyParticleOptions(EMBER, 0.5F), player.getX() + (r.nextDouble() - 0.5) * w,
                    player.getY() + r.nextDouble() * h, player.getZ() + (r.nextDouble() - 0.5) * w, 0, 0.06, 0);
        }
        if (data.is(Live.RED_HULK_ABSORBING) && now % 2 == 0) {
            Vec3 c = player.position().add(0, h * 0.6, 0);
            for (int i = 0; i < 3; i++) {
                Vec3 from = c.add((r.nextDouble() - 0.5) * 4.0, (r.nextDouble() - 0.5) * 3.0,
                        (r.nextDouble() - 0.5) * 4.0);
                Vec3 v = c.subtract(from).scale(0.15);
                level.addParticle(new EnergyParticleOptions(ABSORB, 0.7F), from.x, from.y, from.z, v.x, v.y, v.z);
            }
        }
        loop(STEAM_LOOPS, player, data.is(Live.RED_HULK_HEATED), ModSounds.RED_HULK_STEAM.get(), 0.6F);
        loop(ABSORB_LOOPS, player, data.is(Live.RED_HULK_ABSORBING), ModSounds.RED_HULK_ABSORB.get(), 0.9F);
    }

    /** Starts the loop for the player when {@code on} and none plays (it stops itself when its state ends). */
    private static void loop(Map<Integer, Loop> loops, Player player, boolean on, SoundEvent sound, float volume) {
        Loop playing = loops.get(player.getId());
        if (on && (playing == null || playing.isStopped())) {
            Loop loop = new Loop(sound, player, volume, loops == ABSORB_LOOPS);
            loops.put(player.getId(), loop);
            Minecraft.getInstance().getSoundManager().play(loop);
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
        STEAMS.clear();
        STEAM_LOOPS.values().forEach(Loop::end);
        ABSORB_LOOPS.values().forEach(Loop::end);
        STEAM_LOOPS.clear();
        ABSORB_LOOPS.clear();
        shake = 0.0F;
        flash = 0.0F;
    }

    /** A loop that follows a Red Hulk while his state lasts, fading in and out. */
    private static final class Loop extends AbstractTickableSoundInstance {
        private final Player player;
        private final float target;
        private final boolean absorb;

        Loop(SoundEvent sound, Player player, float target, boolean absorb) {
            super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.target = target;
            this.absorb = absorb;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.05F;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        void end() {
            stop();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !player.isAlive()) {
                stop();
                return;
            }
            PowerData data = Powers.get(player);
            boolean on = absorb ? data.is(Live.RED_HULK_ABSORBING) : data.is(Live.RED_HULK_HEATED);
            x = player.getX();
            y = player.getY() + player.getBbHeight() * 0.7;
            z = player.getZ();
            volume += ((on ? target : 0.0F) - volume) * 0.25F;
            if (!on && volume < 0.02F) {
                stop();
            }
        }
    }

    private RedHulkEffects() {
    }
}

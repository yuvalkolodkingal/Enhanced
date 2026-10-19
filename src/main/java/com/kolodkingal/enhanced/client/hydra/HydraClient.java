package com.kolodkingal.enhanced.client.hydra;

import com.kolodkingal.enhanced.hydra.HydraSoldierEntity;
import com.kolodkingal.enhanced.hydra.HydraSoldierEntity.Act;
import com.kolodkingal.enhanced.network.HydraFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The client's side of the Hydra soldiers (3.6, lane L8): the drawers of {@link HydraFxPayload} (a muzzle flash and
 * the shooter's recoil; the alarm's siren loop, which plays from the radio console while the server keeps telling of
 * the alarm, and the red glow of its lamps; a soldier turned, or let go), and the bookkeeping the model's poses ease
 * with (each soldier's last and previous {@link Act}). Registered from the client setup ({@link #setup}).
 */
public final class HydraClient {
    /** Ticks a change of pose takes. */
    private static final float EASE = 5.0F;
    /** Ticks a shot's kick lasts. */
    private static final float KICK = 4.0F;
    /** Ticks without news after which a siren stops by itself (the server tells every 80). */
    private static final long SIREN_SILENCE = 140L;

    private static final Map<HydraSoldierEntity, Track> TRACKS = new WeakHashMap<>();
    private static final Int2LongOpenHashMap SHOTS = new Int2LongOpenHashMap();
    private static final Map<BlockPos, Siren> SIRENS = new HashMap<>();

    /** What a soldier did and does, and since when (his own clock). */
    private static final class Track {
        Act act = Act.IDLE;
        Act previous = Act.IDLE;
        int since = -100;
    }

    public static void setup() {
        HydraFx.register(HydraFxPayload.MUZZLE, HydraClient::muzzle);
        HydraFx.register(HydraFxPayload.ALARM_ON, HydraClient::alarmOn);
        HydraFx.register(HydraFxPayload.ALARM_OFF, HydraClient::alarmOff);
        HydraFx.register(HydraFxPayload.TURNED, HydraClient::turned);
    }

    // ---- poses ----

    /** Notes the soldier's pose (called as he is drawn). */
    static void track(HydraSoldierEntity soldier) {
        Track t = TRACKS.computeIfAbsent(soldier, s -> new Track());
        Act now = soldier.act();
        if (now != t.act) {
            t.previous = t.act;
            t.act = now;
            t.since = soldier.tickCount;
        }
    }

    /** How far (0..1) the soldier has eased into his pose. */
    static float actWeight(HydraSoldierEntity soldier, float partialTick) {
        Track t = TRACKS.get(soldier);
        if (t == null) {
            return 1.0F;
        }
        float age = soldier.tickCount + partialTick - t.since;
        float w = Math.max(0.0F, Math.min(1.0F, age / EASE));
        return w * w * (3.0F - 2.0F * w);
    }

    /** The pose he eases out of. */
    static Act previousAct(HydraSoldierEntity soldier) {
        Track t = TRACKS.get(soldier);
        return t == null ? soldier.act() : t.previous;
    }

    /** The kick of his last shot (1 at the shot, fading to 0). */
    static float recoil(HydraSoldierEntity soldier, float partialTick) {
        long shot = SHOTS.getOrDefault(soldier.getId(), Long.MIN_VALUE);
        if (shot == Long.MIN_VALUE) {
            return 0.0F;
        }
        float age = soldier.tickCount + partialTick - shot;
        return age < 0.0F || age > KICK ? 0.0F : 1.0F - age / KICK;
    }

    // ---- the effects ----

    private static void muzzle(HydraFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.points().isEmpty()) {
            return;
        }
        Entity shooter = level.getEntity(payload.entityId());
        if (shooter != null) {
            if (SHOTS.size() > 512) {
                SHOTS.clear();
            }
            SHOTS.put(shooter.getId(), shooter.tickCount);
        }
        Vec3 at = payload.points().get(0);
        RandomSource random = level.random;
        for (int i = 0; i < 4; i++) {
            level.addParticle(new EnergyParticleOptions(0x4FB8FF, 0.6F), at.x, at.y, at.z,
                    (random.nextDouble() - 0.5) * 0.08, (random.nextDouble() - 0.5) * 0.08,
                    (random.nextDouble() - 0.5) * 0.08);
        }
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0.0, 0.02, 0.0);
    }

    private static void alarmOn(HydraFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.points().isEmpty()) {
            return;
        }
        Vec3 console = payload.points().get(0);
        BlockPos key = BlockPos.containing(console);
        Siren siren = SIRENS.get(key);
        if (siren == null || siren.isStopped()) {
            siren = new Siren(console, level);
            SIRENS.put(key, siren);
            Minecraft.getInstance().getSoundManager().play(siren);
        }
        siren.heard = level.getGameTime();
        DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 1.4F);
        for (int i = 1; i < payload.points().size(); i++) {
            Vec3 lamp = payload.points().get(i);
            for (int k = 0; k < 6; k++) {
                level.addParticle(red, lamp.x + (level.random.nextDouble() - 0.5) * 1.3,
                        lamp.y + (level.random.nextDouble() - 0.5) * 1.3,
                        lamp.z + (level.random.nextDouble() - 0.5) * 1.3, 0.0, 0.0, 0.0);
            }
        }
    }

    private static void alarmOff(HydraFxPayload payload) {
        if (payload.points().isEmpty()) {
            return;
        }
        Siren siren = SIRENS.remove(BlockPos.containing(payload.points().get(0)));
        if (siren != null) {
            siren.end();
        }
    }

    private static void turned(HydraFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.points().isEmpty()) {
            return;
        }
        Vec3 at = payload.points().get(0);
        boolean on = payload.arg() > 0;
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8.0;
            level.addParticle(on ? new EnergyParticleOptions(0x4FB8FF, 0.7F) : ParticleTypes.SMOKE,
                    at.x + Math.cos(a) * 0.45, at.y + (i % 4) * 0.1, at.z + Math.sin(a) * 0.45,
                    -Math.cos(a) * 0.03, 0.02, -Math.sin(a) * 0.03);
        }
    }

    /** The siren of an alarm, looping from its radio console (heard 48 blocks off) until told it is off. */
    private static final class Siren extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        long heard;

        Siren(Vec3 at, ClientLevel level) {
            super(ModSounds.HYDRA_ALARM.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.level = level;
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
            this.looping = true;
            this.delay = 0;
            this.volume = 3.0F;
            this.attenuation = Attenuation.LINEAR;
            this.heard = level.getGameTime();
        }

        @Override
        public void tick() {
            if (Minecraft.getInstance().level != level || level.getGameTime() - heard > SIREN_SILENCE) {
                stop();
            }
        }

        void end() {
            stop();
        }
    }

    private HydraClient() {
    }
}

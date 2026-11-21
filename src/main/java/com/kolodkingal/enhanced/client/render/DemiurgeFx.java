package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.Demiurge;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The light of Billy the Demiurge (3.18.1), as in the Demiurge's first design, drawn with the other energy effects
 * (additive): small stars turning round his hands while he casts; and while he flies or uses a power, the powered look's
 * light: rings of blue energy at his hands and the red star at his brow. How far the powered look is out ({@link #out})
 * eases in and back, with a burst of stars and a chime at the moment the suit changes ({@link #powered}, read by
 * {@code client.SuitSkins}). His red cloak is cloth, not light ({@link DemiurgeCloakLayer}, grown by {@link #out}), and his
 * crown is Billy's own, brighter ({@link BillyCrownLayer}). The stars inside the energy of every effect of his are
 * {@code DemiurgeStars}'.
 */
@Subscriber(side = Side.CLIENT)
public final class DemiurgeFx {
    static final int BLUE = 0x7FE6FF;
    static final int STAR = 0xEAF8FF;
    static final int CLOAK = 0xD01020;
    /** Ticks the powered look stays after he stops (so that it does not flicker between one spell and the next). */
    private static final int HOLD_TICKS = 10;

    /** Per player (entity id): how far the powered look is out (0 to 1), whether it is on. */
    private static final class Look {
        float amount;
        float amountO;
        boolean powered;
        int hold;
    }

    private static final Map<Integer, Look> LOOKS = new HashMap<>();

    private DemiurgeFx() {
    }

    /** Whether the Demiurge is in his powered look now: flying, or using a power. */
    static boolean poweredNow(Player player, PowerData data) {
        return player.getAbilities().flying || data.flying() || data.is(Live.CROWNED) || acting(data);
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

    /** Whether he wears the powered look now (the look follows {@link #poweredNow}, changing on the client tick). */
    public static boolean powered(Player player) {
        Look look = LOOKS.get(player.getId());
        return look != null && look.powered;
    }

    /** How far the powered look is out for him now (0 at rest to 1, eased): the cloak's layer grows the cloth by it. */
    public static float out(Player player) {
        Look look = LOOKS.get(player.getId());
        if (look == null) {
            return 0.0F;
        }
        float a = look.amount;
        return a * a * (3.0F - 2.0F * a);
    }

    public static void clear() {
        LOOKS.clear();
    }

    /** Every client tick: each Demiurge's look follows what he does; the change has its stars and chime. */
    public static void tick(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null) {
            return;
        }
        LOOKS.keySet().removeIf(id -> !(level.getEntity(id) instanceof Player p) || !Demiurge.is(p));
        for (Player player : level.players()) {
            PowerData data = Powers.get(player);
            if (!Demiurge.is(data)) {
                continue;
            }
            boolean fresh = !LOOKS.containsKey(player.getId());
            Look look = LOOKS.computeIfAbsent(player.getId(), id -> new Look());
            look.hold = poweredNow(player, data) ? HOLD_TICKS : Math.max(0, look.hold - 1);
            boolean now = look.hold > 0;
            if (now != look.powered && !fresh && !player.isInvisible()) {
                change(level, player, now);
            }
            look.powered = now;
            look.amountO = look.amount;
            look.amount = Mth.clamp(look.amount + (now ? 0.12F : -0.08F), 0.0F, 1.0F);
        }
    }

    /** The moment the suit changes: a burst of stars, and a chime as he powers up (a softer one as he comes to rest). */
    private static void change(ClientLevel level, Player player, boolean powered) {
        RandomSource random = level.random;
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (int i = 0; i < 26; i++) {
            Vec3 d = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize().scale(0.25);
            level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? STAR : powered ? BLUE : CLOAK, 0.6F + random.nextFloat() * 0.4F),
                    c.x + d.x, c.y + d.y * 2, c.z + d.z, d.x * 0.6, d.y * 0.6, d.z * 0.6);
        }
        level.playLocalSound(c.x, c.y, c.z, powered ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, powered ? 0.9F : 0.6F, powered ? 1.5F : 1.1F, false);
    }

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || LOOKS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        for (Player p : level.players()) {
            Look look = LOOKS.get(p.getId());
            if (look == null || !(p instanceof AbstractClientPlayer player) || player.isInvisible() || player.isSpectator()
                    || player.distanceToSqr(camera) > 96 * 96) {
                continue;
            }
            if (vc == null) {
                vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            }
            boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
            float amount = Mth.lerp(partial, look.amountO, look.amount);
            render(player, Powers.get(player), pose, vc, camera, partial, time, firstPerson, amount * amount * (3.0F - 2.0F * amount));
        }
        if (vc != null) {
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        }
    }

    private static void render(AbstractClientPlayer player, PowerData data, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                               float partialTick, float time, boolean firstPerson, float out) {
        Vec3 feet = player.getPosition(partialTick).subtract(camera);
        float h = player.getBbHeight();
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        boolean casting = data.is(Live.CASTING) || data.beamActive() || data.is(Live.BEAMING) || data.is(Live.FREEZING);
        for (int s = -1; s <= 1; s += 2) {
            Vec3 hand = feet.add(0, h * 0.42, 0).add(fwd.scale(0.25)).add(side.scale(0.42 * s));
            if (firstPerson) {
                hand = player.getEyePosition(partialTick).subtract(camera).add(player.getViewVector(partialTick).scale(0.7))
                        .add(side.scale(0.35 * s)).add(0, -0.35, 0);
            }
            if (casting) {
                handStars(pose, vc, hand, time, s, firstPerson ? 0.5F : 1.0F);
            }
            if (out > 0.02F && !firstPerson) {
                // the rings of blue energy at his hands (not over his own view)
                Vec3 normal = side.scale(s).add(fwd.scale(0.4));
                float pulse = 0.85F + 0.15F * Mth.sin(time * 0.3F + s);
                float a = out * 0.7F;
                EnergyGeometry.ring(pose, vc, hand, normal, 0.32F * pulse, 0.05F, 24, EnergyGeometry.argb(BLUE, a));
                EnergyGeometry.ring(pose, vc, hand, normal, 0.5F * pulse, 0.03F, 28, EnergyGeometry.argb(BLUE, 0.6F * a));
                EnergyGeometry.ring(pose, vc, hand, normal, 0.18F, 0.04F, 16, EnergyGeometry.argb(STAR, 0.8F * a));
            }
        }
        if (out > 0.02F && !firstPerson) {
            // the red star at his brow, in the hood's shadow (the cloak itself is cloth: DemiurgeCloakLayer)
            Vec3 head = feet.add(0, h * 0.9, 0).add(fwd.scale(0.3));
            twinkle(pose, vc, head, 0.14F * out, time * 0.3F, 0.8F * out);
        }
    }

    /** A small four-pointed star that twinkles. */
    static void twinkle(Matrix4f pose, VertexConsumer vc, Vec3 at, float size, float phase, float a) {
        float t = 0.55F + 0.45F * Mth.sin(phase);
        float s = size * (0.6F + 0.6F * t);
        int col = EnergyGeometry.argb(STAR, a * t);
        EnergyGeometry.ribbon(pose, vc, at.add(-s, 0, 0), at.add(s, 0, 0), s * 0.35F, col, col);
        EnergyGeometry.ribbon(pose, vc, at.add(0, -s, 0), at.add(0, s, 0), s * 0.35F, col, col);
        EnergyGeometry.ribbon(pose, vc, at.add(0, 0, -s), at.add(0, 0, s), s * 0.35F, col, col);
    }

    /** Small stars turning round a hand that casts (his abilities have stars in them). */
    private static void handStars(Matrix4f pose, VertexConsumer vc, Vec3 hand, float time, int s, float a) {
        for (int i = 0; i < 6; i++) {
            float ang = time * 0.25F * s + i * Mth.TWO_PI / 6;
            float rr = 0.22F + 0.05F * Mth.sin(time * 0.3F + i);
            Vec3 at = hand.add(Mth.cos(ang) * rr, 0.1F * Mth.sin(ang * 2), Mth.sin(ang) * rr);
            twinkle(pose, vc, at, 0.05F, time * 0.6F + i, a);
        }
    }
}

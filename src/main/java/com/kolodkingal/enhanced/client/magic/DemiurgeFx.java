package com.kolodkingal.enhanced.client.magic;

import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.ultimate.NexusBeing;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
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
 * The light of Ultimate Mode's two great forms (3.17), drawn with the other energy effects (additive):
 * <ul>
 *   <li>the Demiurge (Billy): a crown of bright blue starry energy over his head always; small stars turning round his hands
 *   while he casts; and while he flies or uses a power, the powered look: the great red hooded cloak flaring out behind
 *   and round him in long waving tongues, stars caught in it, and rings of blue energy at his hands. The cloak grows out
 *   and falls back with the change (idle to powered and back), with a burst of stars and a chime at the moment the suit
 *   changes ({@link #powered}, read by {@code client.SuitSkins});</li>
 *   <li>the Nexus Being (Wanda): a star field drifting round her and red chaos curling off her hands;</li>
 *   <li>a world the Nexus Being holds: the sky red, with turning rings of runes across it, and red fog.</li>
 * </ul>
 */
@Subscriber(side = Side.CLIENT)
public final class DemiurgeFx {
    static final int BLUE = 0x7FE6FF;
    static final int STAR = 0xEAF8FF;
    static final int CLOAK = 0xD01020;
    static final int CLOAK_DEEP = 0x700812;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    /** Per player (entity id): how far the powered look is out (0 to 1, eased), whether it was on, when it changed. */
    private static final class Look {
        float amount;
        boolean powered;
        long lastTick = Long.MIN_VALUE;
    }

    private static final Map<Integer, Look> LOOKS = new HashMap<>();
    /** How red the fog is (0 to 1, eased toward whether a Nexus Being holds this world). */
    private static float nexusFog;

    private DemiurgeFx() {
    }

    /** Whether the Demiurge is in his powered look now: flying, or using a power (both sides read the synced state). */
    static boolean poweredNow(Player player) {
        PowerData data = Powers.get(player);
        boolean flying = player instanceof LocalPlayer lp ? lp.getAbilities().flying : data.flying();
        return flying || data.is(Live.CROWNED) || data.is(Live.CASTING) || data.is(Live.FREEZING) || data.is(Live.CHANTING)
                || data.is(Live.ULTIMATE) || data.is(Live.ULTIMATE_AWAKENING) || data.beamActive() || data.chaosShieldActive();
    }

    /** Whether he wears the powered look (with its own moment of change: the look follows {@link #poweredNow}). */
    public static boolean powered(Player player) {
        Look look = LOOKS.get(player.getId());
        return look != null ? look.powered : poweredNow(player);
    }

    public static void clear() {
        LOOKS.clear();
    }

    // ==================================================================================================================
    // The Demiurge

    static void render(Minecraft mc, AbstractClientPlayer player, PowerData data, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                       float partialTick, float time, boolean firstPerson) {
        ClientLevel level = (ClientLevel) player.level();
        Look look = LOOKS.computeIfAbsent(player.getId(), id -> new Look());
        long tick = level.getGameTime();
        if (tick != look.lastTick) {
            boolean now = poweredNow(player);
            if (now != look.powered && look.lastTick != Long.MIN_VALUE) {
                change(level, player, now);
            }
            look.powered = now;
            look.amount = Mth.clamp(look.amount + (now ? 0.12F : -0.08F), 0.0F, 1.0F);
            look.lastTick = tick;
        }
        float out = look.amount * look.amount * (3.0F - 2.0F * look.amount);
        Vec3 feet = player.getPosition(partialTick).subtract(camera);
        float h = player.getBbHeight();
        float yaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        if (!firstPerson) {
            crown(pose, vc, feet.add(0, h + 0.18, 0), 0.32F, time, 1.0F);
        }
        boolean casting = data.is(Live.CASTING) || data.beamActive() || data.is(Live.FREEZING) || data.is(Live.CHANTING);
        for (int s = -1; s <= 1; s += 2) {
            Vec3 hand = feet.add(0, h * 0.42, 0).add(fwd.scale(0.25)).add(side.scale(0.42 * s));
            if (firstPerson) {
                hand = player.getEyePosition(partialTick).subtract(camera).add(player.getViewVector(partialTick).scale(0.7))
                        .add(side.scale(0.35 * s)).add(0, -0.35, 0);
            }
            if (casting) {
                handStars(pose, vc, hand, time, s, firstPerson ? 0.5F : 1.0F);
            }
            if (out > 0.02F) {
                // the rings of blue energy at his hands
                Vec3 normal = firstPerson ? player.getViewVector(partialTick) : side.scale(s).add(fwd.scale(0.4));
                float pulse = 0.85F + 0.15F * Mth.sin(time * 0.3F + s);
                float a = out * (firstPerson ? 0.35F : 0.9F);
                EnergyGeometry.ring(pose, vc, hand, normal, 0.32F * pulse, 0.05F, 24, EnergyGeometry.argb(BLUE, a));
                EnergyGeometry.ring(pose, vc, hand, normal, 0.5F * pulse, 0.03F, 28, EnergyGeometry.argb(BLUE, 0.6F * a));
                EnergyGeometry.ring(pose, vc, hand, normal, 0.18F, 0.04F, 16, EnergyGeometry.argb(STAR, 0.8F * a));
            }
        }
        if (out > 0.02F && !firstPerson) {
            cloak(pose, vc, feet, h, fwd, side, player.getDeltaMovement(), out, time, player.getId());
        }
    }

    /** The moment the suit changes: a burst of stars, and a chime as he powers up (a softer one as he comes to rest). */
    private static void change(ClientLevel level, AbstractClientPlayer player, boolean powered) {
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

    /** The crown of starry blue energy: a ring of points over the head, stars twinkling in it. */
    static void crown(Matrix4f pose, VertexConsumer vc, Vec3 at, float r, float time, float a) {
        EnergyGeometry.ring(pose, vc, at, UP, r, 0.05F, 24, EnergyGeometry.argb(BLUE, 0.8F * a));
        for (int i = 0; i < 7; i++) {
            float ang = i * Mth.TWO_PI / 7 + time * 0.02F;
            Vec3 base = at.add(Mth.cos(ang) * r, 0, Mth.sin(ang) * r);
            float tall = 0.22F + 0.06F * Mth.sin(time * 0.25F + i);
            Vec3 tip = base.add(Mth.cos(ang) * 0.05F, tall, Mth.sin(ang) * 0.05F);
            EnergyGeometry.ribbon(pose, vc, base, tip, 0.08F, 0.0F, EnergyGeometry.argb(BLUE, 0.85F * a),
                    EnergyGeometry.argb(STAR, 0.0F));
            twinkle(pose, vc, tip, 0.05F, time * 0.5F + i * 1.3F, a);
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

    /**
     * The great red hooded cloak: long tongues from his shoulders and the hood, flaring out behind and to the sides and
     * waving, deep red at the root and bright at the edges, stars caught in it.
     */
    private static void cloak(Matrix4f pose, VertexConsumer vc, Vec3 feet, float h, Vec3 fwd, Vec3 side, Vec3 vel, float out,
                              float time, int id) {
        Vec3 back = fwd.scale(-1);
        Vec3 stream = vel.lengthSqr() > 0.01 ? vel.normalize().scale(-1) : back;
        Vec3 neck = feet.add(0, h * 0.86, 0).add(back.scale(0.12));
        int tongues = 22;
        for (int i = 0; i < tongues; i++) {
            float f = (i + 0.5F) / tongues;               // across the cloak, one side to the other
            float across = (f - 0.5F) * 2.0F;
            float phase = time * 0.09F + i * 0.7F + id;
            Vec3 root = neck.add(side.scale(across * 0.28)).add(0, -Math.abs(across) * 0.08, 0);
            Vec3 dir = back.scale(0.6).add(stream.scale(0.4)).add(side.scale(across * 1.3)).add(0, 0.35 - 0.55 * Math.abs(across), 0);
            dir = dir.normalize();
            float len = (2.2F + 1.4F * (1.0F - Math.abs(across)) + 0.5F * Mth.sin(phase * 1.3F)) * out;
            Vec3 prev = root;
            int segs = 6;
            for (int k = 1; k <= segs; k++) {
                float t = (float) k / segs;
                Vec3 wave = side.scale(0.35 * t * Mth.sin(phase + t * 4.0F)).add(0, 0.3 * t * Mth.cos(phase * 0.8F + t * 3.0F), 0);
                Vec3 p = root.add(dir.scale(len * t)).add(wave).add(0, -0.6 * t * t * (1.0F - out * 0.5F), 0);
                float w0 = (0.42F - 0.25F * (t - 1.0F / segs)) * out;
                float w1 = (0.42F - 0.25F * t) * out;
                int c0 = EnergyGeometry.argb(k == 1 ? CLOAK_DEEP : CLOAK, 0.55F * out * (1.0F - 0.5F * (t - 1.0F / segs)));
                int c1 = EnergyGeometry.argb(CLOAK, 0.55F * out * (1.0F - 0.5F * t));
                EnergyGeometry.ribbon(pose, vc, prev, p, Math.max(0.02F, w0), Math.max(0.02F, w1), c0, c1);
                if (((i * 7 + k * 3) % 5) == 0) {
                    twinkle(pose, vc, p, 0.04F, time * 0.4F + i + k, 0.8F * out);
                }
                prev = p;
            }
        }
        // the hood: a red ring round the head
        Vec3 head = feet.add(0, h * 0.94, 0).add(back.scale(0.05));
        EnergyGeometry.ring(pose, vc, head, fwd, 0.36F, 0.09F, 20, EnergyGeometry.argb(CLOAK, 0.5F * out));
        // the red star at the brow
        twinkle(pose, vc, head.add(fwd.scale(0.3)), 0.18F * out, time * 0.3F, 0.8F * out);
    }

    // ==================================================================================================================
    // The Nexus Being

    /** The star field drifting round her, and red chaos curling off her hands. */
    static void nexus(AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick, float time,
                      boolean firstPerson) {
        Vec3 feet = player.getPosition(partialTick).subtract(camera);
        float h = player.getBbHeight();
        float a = firstPerson ? 0.35F : 1.0F;
        for (int i = 0; i < 24; i++) {
            float ang = i * 2.399F + time * 0.01F * (i % 2 == 0 ? 1 : -1);
            float y = ((i * 0.41F + time * 0.003F) % 1.0F) * h * 1.2F;
            float rr = 0.7F + 0.4F * Mth.sin(i * 1.3F);
            Vec3 at = feet.add(Mth.cos(ang) * rr, y, Mth.sin(ang) * rr);
            if (firstPerson && y > player.getEyeHeight() - 0.6F) {
                continue;
            }
            float t = 0.5F + 0.5F * Mth.sin(time * 0.35F + i);
            int col = EnergyGeometry.argb(i % 3 == 0 ? 0xFFFFFF : 0xFF8FA2, a * t);
            EnergyGeometry.ribbon(pose, vc, at.add(-0.05, 0, 0), at.add(0.05, 0, 0), 0.02F, col, col);
            EnergyGeometry.ribbon(pose, vc, at.add(0, -0.05, 0), at.add(0, 0.05, 0), 0.02F, col, col);
        }
        if (!firstPerson) {
            float yaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
            Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
            Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
            for (int s = -1; s <= 1; s += 2) {
                Vec3 hand = feet.add(0, h * 0.42, 0).add(fwd.scale(0.2)).add(side.scale(0.38 * s));
                for (int k = 0; k < 4; k++) {
                    float phase = time * 0.2F + k * 1.6F + s;
                    Vec3 mid = hand.add(side.scale(0.25 * s * Mth.cos(phase))).add(0, 0.25 + 0.1 * Mth.sin(phase * 1.5F), 0);
                    Vec3 tip = mid.add(fwd.scale(0.2 * Mth.sin(phase))).add(0, 0.25, 0);
                    EnergyGeometry.ribbon(pose, vc, hand, mid, 0.08F, 0.05F, EnergyGeometry.argb(NexusBeing.RED, 0.8F),
                            EnergyGeometry.argb(NexusBeing.RED, 0.5F));
                    EnergyGeometry.ribbon(pose, vc, mid, tip, 0.05F, 0.0F, EnergyGeometry.argb(NexusBeing.RED, 0.5F),
                            EnergyGeometry.argb(NexusBeing.GLOW, 0.0F));
                }
            }
        }
    }

    /** A world the Nexus Being holds: the sky washed red, and rings of runes turning across it. */
    static void nexusSky(Minecraft mc, Matrix4f pose, VertexConsumer vc, float partialTick, float time) {
        // (drawn round the camera, far out: the camera-relative origin is the camera itself)
        float r = 90.0F;
        for (int band = 0; band < 9; band++) {
            float lat = Mth.DEG_TO_RAD * (6 + band * 10);
            float y = Mth.sin(lat) * r;
            float rr = Mth.cos(lat) * r;
            EnergyGeometry.ring(pose, vc, new Vec3(0, y, 0), UP, rr, 15.0F, 48,
                    EnergyGeometry.argb(NexusBeing.RED, 0.16F));
        }
        for (int i = 0; i < 10; i++) {
            float ang = i * Mth.TWO_PI / 10 + time * 0.0015F;
            float lat = Mth.DEG_TO_RAD * (25 + (i % 3) * 18);
            Vec3 at = new Vec3(Mth.cos(ang) * Mth.cos(lat) * r * 0.95F, Mth.sin(lat) * r * 0.95F, Mth.sin(ang) * Mth.cos(lat) * r * 0.95F);
            Vec3 normal = at.scale(-1);
            float spin = time * 0.01F * (i % 2 == 0 ? 1 : -1);
            float size = 7.0F + 2.0F * (i % 3);
            EnergyGeometry.ring(pose, vc, at, normal, size, 0.5F, 36, EnergyGeometry.argb(0xFF3050, 0.55F));
            EnergyGeometry.ring(pose, vc, at, normal, size * 0.7F, 0.3F, 30, EnergyGeometry.argb(0xFF8090, 0.45F));
            // a rune: a star polygon inside the ring
            Vec3 u = normal.cross(UP).normalize();
            Vec3 v = normal.normalize().cross(u).normalize();
            int n = 5 + (i % 3);
            for (int k = 0; k < n; k++) {
                float a0 = spin + k * Mth.TWO_PI / n;
                float a1 = spin + (k + 2) * Mth.TWO_PI / n;
                Vec3 p0 = at.add(u.scale(Mth.cos(a0) * size * 0.68F)).add(v.scale(Mth.sin(a0) * size * 0.68F));
                Vec3 p1 = at.add(u.scale(Mth.cos(a1) * size * 0.68F)).add(v.scale(Mth.sin(a1) * size * 0.68F));
                EnergyGeometry.ribbon(pose, vc, p0, p1, 0.35F, EnergyGeometry.argb(0xFF4060, 0.5F),
                        EnergyGeometry.argb(0xFF4060, 0.5F));
            }
        }
    }

    /** The fog turns red in a world the Nexus Being holds. */
    @Subscribe
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        boolean held = mc.level != null && NexusBeing.heldOnClient(mc.level);
        nexusFog = Mth.clamp(nexusFog + (held ? 0.02F : -0.04F), 0.0F, 1.0F);
        if (nexusFog <= 0.001F) {
            return;
        }
        float k = nexusFog * 0.7F;
        event.setRed(Mth.lerp(k, event.getRed(), 0.62F));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.04F));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.12F));
    }
}

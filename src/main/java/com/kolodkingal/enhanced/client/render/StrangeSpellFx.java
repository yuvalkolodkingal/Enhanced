package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.strange.MirrorView;
import com.kolodkingal.enhanced.client.strange.StrangeFx;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.StrangeFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.strange.SeraphimShield;
import com.kolodkingal.enhanced.strange.StrangeAbilities;
import com.kolodkingal.enhanced.strange.StrangeSignals;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What everyone near Doctor Strange sees him cast (3.6), drawn from his kinds of {@link StrangeFxPayload} ({@link
 * StrangeSignals}): the Eldritch Whip, a chain of orange sparks from his hand that sways as it cracks; the Crimson Bands,
 * three red ribbons that close round their creature and turn; the Seraphim Dome, interlocking mandalas round him; the
 * Eye's green rings at an awakening and a Rewind. Every frame, too, the Shield of the Seraphim of whoever holds it up
 * (the synced {@link Live#STRANGE_SHIELD}): a layered mandala turning before his hands. Each kind also starts its gesture
 * on the caster. The mandalas are {@code textures/fx/mandala_*.png} (made by {@code gen_strange_fx.py}), tinted and
 * added to the scene; everything is interpolated with the partial tick.
 *
 * <p>It also keeps, per player, what his HUD needs of these (the lash of his combo, the Dome's and the portals' time
 * left), since the server does not sync them.
 */
@Subscriber(side = Side.CLIENT)
public final class StrangeSpellFx {
    public static final ResourceLocation SHIELD_TEX = Enhanced.id("textures/fx/mandala_shield.png");
    public static final ResourceLocation DOME_TEX = Enhanced.id("textures/fx/mandala_dome.png");
    public static final ResourceLocation RING_TEX = Enhanced.id("textures/fx/mandala_ring.png");
    public static final ResourceLocation EYE_TEX = Enhanced.id("textures/fx/mandala_eye.png");

    private static final int WHIP_TICKS = 10;
    private static final int WHIP_SEGMENTS = 18;
    /** The panels of a Dome: the directions of an icosahedron's faces, so the mandalas interlock round him. */
    private static final Vec3[] DOME_PANELS = icosahedron();

    private record Whip(int caster, Vec3 end, int step, long start) {
    }

    private record Bands(int target, long start, int ticks) {
    }

    private record Dome(int caster, Vec3 at, float radius, long start, int ticks) {
    }

    /** The Eye's green rings round a player: an awakening (big, slow) or a Rewind (fast, backward). */
    private record Rings(int player, Vec3 at, long start, int ticks, boolean backward) {
    }

    private static final List<Whip> WHIPS = new ArrayList<>();
    private static final List<Bands> BANDS = new ArrayList<>();
    private static final List<Dome> DOMES = new ArrayList<>();
    private static final List<Rings> RINGS = new ArrayList<>();

    /** What a player's HUD needs: his last lash and when, when his Dome and his portals end (his client's clock). */
    static final class Seen {
        int step;
        long stepAt = Long.MIN_VALUE / 2;
        long domeUntil;
        long portalsUntil;
        long shieldSince = -1;
    }

    private static final Map<Integer, Seen> SEEN = new HashMap<>();

    static Seen seen(int id) {
        return SEEN.computeIfAbsent(id, k -> new Seen());
    }

    /** Registers the drawers of his kinds (once, at client setup). */
    public static void register() {
        for (int kind = StrangeFxPayload.WHIP; kind <= StrangeFxPayload.MIRROR_OFF; kind++) {
            StrangeFx.register(kind, StrangeSpellFx::receive);
        }
    }

    static void receive(StrangeFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        Entity who = p.entityId() < 0 ? null : level.getEntity(p.entityId());
        List<Vec3> pts = p.points();
        Vec3 first = pts.isEmpty() ? (who == null ? Vec3.ZERO : who.position()) : pts.get(0);
        switch (p.kind()) {
            case StrangeFxPayload.WHIP -> {
                WHIPS.add(new Whip(p.entityId(), first, p.arg(), now));
                if (p.arg() == StrangeSignals.GRAPPLE) {
                    play(who, GestureId.STRANGE_GRAPPLE);
                } else {
                    Seen s = seen(p.entityId());
                    s.step = p.arg();
                    s.stepAt = now;
                    play(who, p.arg() == 2 ? GestureId.STRANGE_WHIP_2 : p.arg() == 3 ? GestureId.STRANGE_WHIP_3
                            : GestureId.STRANGE_WHIP_1);
                }
                sparks(level, first, StrangeAbilities.ORANGE, 8, 0.15);
            }
            case StrangeFxPayload.BANDS -> {
                int ticks = pts.size() > 1 ? (int) pts.get(1).x : 80;
                BANDS.add(new Bands(p.arg(), now, ticks));
                play(who, GestureId.STRANGE_BANDS);
                sparks(level, first.add(0, 1, 0), StrangeAbilities.CRIMSON, 14, 0.2);
            }
            case StrangeFxPayload.SHIELD -> {
                if (p.arg() == 2) {
                    // (The Eye's Rewind: the green rings run backward round him.)
                    RINGS.add(new Rings(p.entityId(), first, now, 24, true));
                    play(who, GestureId.STRANGE_REWIND);
                } else if (who != null) {
                    seen(who.getId()).shieldSince = p.arg() == 1 ? now : -1;
                }
            }
            case StrangeFxPayload.DOME -> {
                if (p.arg() < 0) {
                    RINGS.add(new Rings(p.entityId(), first, now, -p.arg(), false));
                    sparks(level, first, StrangeAbilities.TIME_GREEN, 30, 0.25);
                } else {
                    float radius = pts.size() > 1 ? (float) pts.get(1).x / 10.0F : 4.0F;
                    DOMES.add(new Dome(p.entityId(), first, radius, now, p.arg()));
                    seen(p.entityId()).domeUntil = now + p.arg();
                    play(who, GestureId.STRANGE_DOME);
                    sparks(level, first, StrangeAbilities.ORANGE, 30, 0.3);
                }
            }
            case StrangeFxPayload.PORTAL_OPEN -> {
                if (who != null) {
                    seen(who.getId()).portalsUntil = now + p.arg();
                    play(who, GestureId.STRANGE_SLING);
                }
                for (Vec3 at : pts) {
                    sparks(level, at, StrangeAbilities.ORANGE, 24, 0.25);
                }
            }
            case StrangeFxPayload.PORTAL_CLOSE -> {
                for (Vec3 at : pts) {
                    sparks(level, at, StrangeAbilities.GOLD, 12, 0.12);
                }
            }
            case StrangeFxPayload.ASTRAL_OUT, StrangeFxPayload.ASTRAL_IN ->
                    sparks(level, first.add(0, 1.0, 0), StrangeAbilities.ASTRAL_BLUE, 20, 0.12);
            case StrangeFxPayload.MIRROR_ON -> MirrorView.open(p.entityId(), first, p.arg() / 10.0F, now);
            case StrangeFxPayload.MIRROR_OFF -> MirrorView.close(p.entityId());
            default -> {
            }
        }
    }

    private static void play(Entity who, GestureId id) {
        if (who instanceof Player player) {
            HeroAnimator.play(player, id, 0.0F);
        }
    }

    /** Sparks thrown out from a point. */
    static void sparks(ClientLevel level, Vec3 at, int color, int count, double speed) {
        RandomSource r = level.random;
        for (int i = 0; i < count; i++) {
            int c = i % 3 == 0 ? StrangeAbilities.GOLD : color;
            level.addParticle(new EnergyParticleOptions(c, 0.6F), at.x, at.y, at.z, r.nextGaussian() * speed,
                    r.nextGaussian() * speed, r.nextGaussian() * speed);
        }
    }

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        WHIPS.removeIf(w -> now - w.start() > (w.step() == StrangeSignals.GRAPPLE ? 20 : WHIP_TICKS));
        BANDS.removeIf(b -> now - b.start() > b.ticks());
        DOMES.removeIf(d -> now - d.start() > d.ticks());
        RINGS.removeIf(r -> now - r.start() > r.ticks());
        // The Shield's sparks flying off its rim.
        for (AbstractClientPlayer p : level.players()) {
            if (shieldUp(p) && level.random.nextInt(2) == 0) {
                Vec3 look = p.getViewVector(1.0F);
                Vec3 c = SeraphimShield.discCenter(p.getEyePosition(1.0F), look);
                double a = level.random.nextDouble() * Math.PI * 2;
                Vec3[] uv = basis(look);
                Vec3 at = c.add(uv[0].scale(Math.cos(a) * SeraphimShield.DISC_RADIUS))
                        .add(uv[1].scale(Math.sin(a) * SeraphimShield.DISC_RADIUS));
                level.addParticle(new EnergyParticleOptions(colour(p, HeroAbility.STRANGE_SHIELDS, StrangeAbilities.ORANGE),
                        0.4F), at.x, at.y, at.z, look.x * 0.02, 0.01, look.z * 0.02);
            }
        }
    }

    static void clear() {
        WHIPS.clear();
        BANDS.clear();
        DOMES.clear();
        RINGS.clear();
        SEEN.clear();
    }

    /** Whether this player's Shield of the Seraphim stands (synced). */
    static boolean shieldUp(Player p) {
        PowerData data = Powers.get(p);
        return data.is(Live.STRANGE_SHIELD) && data.has(HeroAbility.STRANGE_SHIELDS);
    }

    /** The colour of one of his spells for this player: a custom hero's chosen colour, or Strange's own. */
    static int colour(Player p, HeroAbility row, int own) {
        return AbilityColors.color(p, row, own);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Drawing

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        List<AbstractClientPlayer> shielded = new ArrayList<>();
        for (AbstractClientPlayer p : level.players()) {
            if (shieldUp(p)) {
                shielded.add(p);
            }
        }
        if (WHIPS.isEmpty() && BANDS.isEmpty() && DOMES.isEmpty() && RINGS.isEmpty() && shielded.isEmpty()) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        if (!WHIPS.isEmpty() || !BANDS.isEmpty()) {
            VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            for (Whip w : WHIPS) {
                whip(mc, level, pose, vc, w, camera, time, partial);
            }
            for (Bands b : BANDS) {
                bands(level, pose, vc, b, camera, time, partial);
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        }
        if (!shielded.isEmpty()) {
            RenderType type = EnhancedRenderTypes.energyField(SHIELD_TEX);
            VertexConsumer vc = buffers.getBuffer(type);
            for (AbstractClientPlayer p : shielded) {
                shield(level, pose, vc, p, camera, time, partial);
            }
            buffers.endBatch(type);
        }
        if (!DOMES.isEmpty()) {
            RenderType type = EnhancedRenderTypes.energyField(DOME_TEX);
            VertexConsumer vc = buffers.getBuffer(type);
            for (Dome d : DOMES) {
                dome(level, pose, vc, d, camera, time, partial);
            }
            buffers.endBatch(type);
            VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
            for (Dome d : DOMES) {
                Vec3 c = domeCenter(level, d, partial).subtract(camera);
                float fade = fade(time - d.start(), d.ticks());
                EnergyGeometry.veil(pose, veil, c, d.radius(), 24, 12, StrangeAbilities.ORANGE, 0.03F * fade, 0.18F * fade);
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
        }
        if (!RINGS.isEmpty()) {
            RenderType type = EnhancedRenderTypes.energyField(EYE_TEX);
            VertexConsumer vc = buffers.getBuffer(type);
            for (Rings r : RINGS) {
                rings(level, pose, vc, r, camera, time, partial);
            }
            buffers.endBatch(type);
        }
    }

    /** A fade in over 3 ticks and out over the last 6 of {@code life}. */
    private static float fade(float age, float life) {
        return Mth.clamp(age / 3.0F, 0.0F, 1.0F) * Mth.clamp((life - age) / 6.0F, 0.0F, 1.0F);
    }

    /**
     * The whip: a chain of spark segments from his hand to where it cracks, swaying like a rope (a travelling sine that
     * dies away), its length thrown out over the first ticks and drawn back after.
     */
    private static void whip(Minecraft mc, ClientLevel level, Matrix4f pose, VertexConsumer vc, Whip w, Vec3 camera,
                             float time, float partial) {
        Entity caster = level.getEntity(w.caster());
        if (!(caster instanceof AbstractClientPlayer player)) {
            return;
        }
        float age = time - w.start();
        boolean grapple = w.step() == StrangeSignals.GRAPPLE;
        float life = grapple ? 20.0F : WHIP_TICKS;
        float out = Mth.clamp(age / 2.5F, 0.0F, 1.0F);
        float back = grapple ? 0.0F : Mth.clamp((age - 5.0F) / 5.0F, 0.0F, 1.0F);
        float reach = out * (1.0F - back);
        float alpha = Mth.clamp((life - age) / 4.0F, 0.0F, 1.0F);
        if (reach <= 0.01F || alpha <= 0.0F) {
            return;
        }
        Vec3 hand = WorldEffectsRenderer.handPosition(mc, player, partial);
        Vec3 end = w.end();
        Vec3 path = end.subtract(hand);
        double length = path.length();
        if (length < 0.1) {
            return;
        }
        Vec3 dir = path.scale(1.0 / length);
        Vec3[] uv = basis(dir);
        float sway = (grapple ? 0.15F : 0.55F) * Math.max(0.0F, 1.0F - age / life);
        List<Vec3> points = new ArrayList<>();
        int n = WHIP_SEGMENTS;
        for (int i = 0; i <= n; i++) {
            float f = (float) i / n * reach;
            double wave = Math.sin(f * Math.PI * 3.0 - age * 1.8 + w.step()) * sway * Math.sin(f * Math.PI);
            double droop = -Math.sin(f * Math.PI) * 0.35 * (1.0F - out) * length * 0.1;
            Vec3 p = hand.add(path.scale(f)).add(uv[0].scale(wave)).add(uv[1].scale(droop));
            points.add(p.subtract(camera));
        }
        int color = colour(player, HeroAbility.STRANGE_WHIP, StrangeAbilities.ORANGE);
        for (int i = 1; i < points.size(); i++) {
            // Segments of sparks: every other one a little brighter, as the film's chain of embers.
            float lit = (i % 2 == 0 ? 0.95F : 0.7F) * alpha;
            EnergyGeometry.ribbon(pose, vc, points.get(i - 1), points.get(i), 0.16F, EnergyGeometry.argb(color, lit),
                    EnergyGeometry.argb(color, lit));
            EnergyGeometry.ribbon(pose, vc, points.get(i - 1), points.get(i), 0.06F,
                    EnergyGeometry.argb(StrangeAbilities.GOLD, alpha), EnergyGeometry.argb(StrangeAbilities.GOLD, alpha));
        }
        if (age < 1.0F && reach >= 0.99F) {
            sparks(level, end, color, 3, 0.1);
        }
    }

    /** The Crimson Bands: three red ribbons closing on the creature and turning round it, at three heights. */
    private static void bands(ClientLevel level, Matrix4f pose, VertexConsumer vc, Bands b, Vec3 camera, float time,
                              float partial) {
        Entity target = level.getEntity(b.target());
        if (target == null) {
            return;
        }
        float age = time - b.start();
        float alpha = fade(age, b.ticks());
        AABB box = target.getBoundingBox().move(target.getPosition(partial).subtract(target.position()));
        double width = Math.max(box.getXsize(), box.getZsize()) * 0.5 + 0.12;
        double close = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        double radius = width + (1.4 - width) * (1.0 - close) * (1.0 - close);
        Vec3 base = new Vec3((box.minX + box.maxX) * 0.5, box.minY, (box.minZ + box.maxZ) * 0.5).subtract(camera);
        double h = box.getYsize();
        int segments = 24;
        for (int band = 0; band < 3; band++) {
            double y = h * (0.28 + 0.24 * band);
            double tilt = 0.12 * (band - 1);
            double spin = time * (0.12 + 0.03 * band) * (band % 2 == 0 ? 1 : -1);
            for (int s = 0; s < segments; s++) {
                double a0 = spin + Math.PI * 2 * s / segments;
                double a1 = spin + Math.PI * 2 * (s + 1) / segments;
                Vec3 p0 = base.add(Math.cos(a0) * radius, y + Math.sin(a0) * tilt * radius, Math.sin(a0) * radius);
                Vec3 p1 = base.add(Math.cos(a1) * radius, y + Math.sin(a1) * tilt * radius, Math.sin(a1) * radius);
                float glint = 0.6F + 0.4F * Mth.sin((float) (s * 0.9 + time * 0.4));
                EnergyGeometry.ribbon(pose, vc, p0, p1, 0.13F, EnergyGeometry.argb(StrangeAbilities.CRIMSON,
                        alpha * 0.85F * glint), EnergyGeometry.argb(StrangeAbilities.CRIMSON, alpha * 0.85F * glint));
                EnergyGeometry.ribbon(pose, vc, p0, p1, 0.04F, EnergyGeometry.argb(0xFF9A7A, alpha * 0.7F),
                        EnergyGeometry.argb(0xFF9A7A, alpha * 0.7F));
            }
        }
    }

    /** The Shield of the Seraphim: two mandalas before his hands, turning against each other, grown as it rises. */
    private static void shield(ClientLevel level, Matrix4f pose, VertexConsumer vc, AbstractClientPlayer p, Vec3 camera,
                               float time, float partial) {
        Vec3 look = p.getViewVector(partial);
        Vec3 center = SeraphimShield.discCenter(p.getEyePosition(partial), look).subtract(camera);
        Seen seen = seen(p.getId());
        float grown = seen.shieldSince < 0 ? 1.0F : Mth.clamp((time - seen.shieldSince) / 5.0F, 0.15F, 1.0F);
        float r = (float) SeraphimShield.DISC_RADIUS * (0.3F + 0.7F * grown * grown * (3 - 2 * grown));
        int color = colour(p, HeroAbility.STRANGE_SHIELDS, StrangeAbilities.ORANGE);
        float flicker = 0.85F + 0.15F * Mth.sin(time * 0.7F);
        disc(pose, vc, center, look, r, time * 0.05F, EnergyGeometry.argb(color, 0.95F * flicker));
        disc(pose, vc, center.add(look.scale(0.04)), look, r * 0.82F, -time * 0.08F,
                EnergyGeometry.argb(StrangeAbilities.GOLD, 0.6F * flicker));
    }

    /** The Dome's centre: its caster's middle as he moves (where he was cast if he is gone). */
    private static Vec3 domeCenter(ClientLevel level, Dome d, float partial) {
        Entity caster = level.getEntity(d.caster());
        return caster == null ? d.at() : caster.getPosition(partial).add(0, caster.getBbHeight() * 0.5, 0);
    }

    /** The Seraphim Dome: twenty mandala panels on a sphere, each turning, interlocking round him. */
    private static void dome(ClientLevel level, Matrix4f pose, VertexConsumer vc, Dome d, Vec3 camera, float time,
                             float partial) {
        float age = time - d.start();
        float alpha = fade(age, d.ticks());
        float grow = Mth.clamp(age / 5.0F, 0.0F, 1.0F);
        Vec3 c = domeCenter(level, d, partial).subtract(camera);
        float r = d.radius() * (0.4F + 0.6F * grow);
        float panel = r * 0.62F;
        Entity caster = level.getEntity(d.caster());
        int color = caster instanceof Player p ? colour(p, HeroAbility.STRANGE_SHIELDS, StrangeAbilities.ORANGE)
                : StrangeAbilities.ORANGE;
        for (int i = 0; i < DOME_PANELS.length; i++) {
            Vec3 n = DOME_PANELS[i];
            disc(pose, vc, c.add(n.scale(r)), n, panel, time * 0.04F * (i % 2 == 0 ? 1 : -1) + i,
                    EnergyGeometry.argb(color, 0.75F * alpha));
        }
    }

    /** The Eye's green rings: three time mandalas round him, flat, turning (backward for a Rewind). */
    private static void rings(ClientLevel level, Matrix4f pose, VertexConsumer vc, Rings r, Vec3 camera, float time,
                              float partial) {
        Entity who = level.getEntity(r.player());
        Vec3 at = who == null ? r.at() : who.getPosition(partial).add(0, 1.0, 0);
        float age = time - r.start();
        float alpha = fade(age, r.ticks());
        float dir = r.backward() ? -1.0F : 1.0F;
        Vec3 up = new Vec3(0, 1, 0);
        for (int i = 0; i < 3; i++) {
            float size = (r.backward() ? 0.9F : 1.3F) + 0.35F * i - (r.backward() ? 0 : 0.3F * Mth.clamp(age / r.ticks(),
                    0, 1));
            Vec3 c = at.add(0, -0.6 + 0.6 * i, 0).subtract(camera);
            disc(pose, vc, c, up, size, dir * time * (0.12F + 0.05F * i), EnergyGeometry.argb(StrangeAbilities.TIME_GREEN,
                    0.85F * alpha));
        }
    }

    // ---- geometry ----

    /** Two unit vectors across {@code n} (it need not be a unit vector). */
    static Vec3[] basis(Vec3 n) {
        Vec3 nn = n.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : n.normalize();
        Vec3 ref = Math.abs(nn.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = ref.cross(nn).normalize();
        Vec3 v = nn.cross(u);
        return new Vec3[]{u, v};
    }

    /**
     * A textured square of half-side {@code radius} at {@code c} facing {@code n}, turned by {@code angle} radians: one
     * mandala (the texture holds the disc).
     */
    static void disc(Matrix4f pose, VertexConsumer vc, Vec3 c, Vec3 n, float radius, float angle, int argb) {
        Vec3[] uv = basis(n);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Vec3 u = uv[0].scale(cos).add(uv[1].scale(sin)).scale(radius);
        Vec3 v = uv[1].scale(cos).subtract(uv[0].scale(sin)).scale(radius);
        vertex(pose, vc, c.subtract(u).subtract(v), 0, 0, argb);
        vertex(pose, vc, c.add(u).subtract(v), 1, 0, argb);
        vertex(pose, vc, c.add(u).add(v), 1, 1, argb);
        vertex(pose, vc, c.subtract(u).add(v), 0, 1, argb);
    }

    private static void vertex(Matrix4f pose, VertexConsumer vc, Vec3 p, float u, float v, int argb) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setUv(u, v).setColor(argb);
    }

    /** The twenty face directions of an icosahedron. */
    private static Vec3[] icosahedron() {
        double t = (1.0 + Math.sqrt(5.0)) / 2.0;
        Vec3[] v = {
                new Vec3(-1, t, 0), new Vec3(1, t, 0), new Vec3(-1, -t, 0), new Vec3(1, -t, 0),
                new Vec3(0, -1, t), new Vec3(0, 1, t), new Vec3(0, -1, -t), new Vec3(0, 1, -t),
                new Vec3(t, 0, -1), new Vec3(t, 0, 1), new Vec3(-t, 0, -1), new Vec3(-t, 0, 1)};
        int[][] f = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2},
                {10, 7, 6}, {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11},
                {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
        Vec3[] out = new Vec3[f.length];
        for (int i = 0; i < f.length; i++) {
            out[i] = v[f[i][0]].add(v[f[i][1]]).add(v[f[i][2]]).normalize();
        }
        return out;
    }

    /** Whether the reduced-motion option is on (the Mirror's warp and the big sways are calmer). */
    static boolean calm() {
        return EnhancedClientConfig.reducedMotion();
    }

    private StrangeSpellFx() {
    }
}

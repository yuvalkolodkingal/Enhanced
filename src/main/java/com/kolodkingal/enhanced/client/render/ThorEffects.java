package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.network.ThorFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Thor's lightning and the Bifrost, drawn in the world: lightning pouring from the sky into Mjolnir and leaping to its
 * target, arcs from creature to creature, the God of Thunder crackling with it, the Bifrost's rainbow beam and the
 * circle of runes it burns into the ground.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ThorEffects {
    public static final ResourceLocation BIFROST_MARK = Enhanced.id("textures/effect/bifrost_mark.png");
    /** MCU lightning: a white-hot core, electric blue glow and a deep blue halo. */
    private static final int CORE = 0xFFFFFF;
    private static final int BLUE = 0x4DB8FF;
    private static final int HALO = 0x1E6BFF;
    private static final int CHANNEL_TICKS = 10;
    private static final int ARC_TICKS = 6;
    private static final int ARRIVAL_TICKS = 24;
    private static final int MARK_TICKS = 1200;

    private record Effect(int kind, int entityId, List<Vec3> points, ResourceKey<Level> dimension, long start) {
        int length() {
            return switch (kind) {
                case ThorFxPayload.CHANNEL -> CHANNEL_TICKS;
                case ThorFxPayload.ARC -> ARC_TICKS;
                case ThorFxPayload.BIFROST_ARRIVAL -> ARRIVAL_TICKS;
                default -> MARK_TICKS;
            };
        }
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();

    public static void add(ThorFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && !payload.points().isEmpty()) {
            EFFECTS.add(new Effect(payload.kind(), payload.entityId(), payload.points(), level.dimension(), level.getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            EFFECTS.clear();
            return;
        }
        long now = level.getGameTime();
        EFFECTS.removeIf(e -> e.dimension() != level.dimension() || now - e.start() > e.length() || now < e.start());
        RandomSource random = level.random;
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (data.power() != Power.THOR || player.isInvisible()) {
                continue;
            }
            if (data.toggle(Toggle.GOD_OF_THUNDER) && random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, player.getRandomX(0.7), player.getRandomY(), player.getRandomZ(0.7),
                        (random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.2);
            }
            if (data.is(Live.BIFROST)) {
                for (int i = 0; i < 3; i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    level.addParticle(ParticleTypes.END_ROD, player.getX() + Math.cos(angle) * 1.4, player.getY() + random.nextDouble() * 2.5,
                            player.getZ() + Math.sin(angle) * 1.4, 0, 0.25, 0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // The rune circles first: flat on the ground, textured.
        VertexConsumer marks = buffers.getBuffer(EnhancedRenderTypes.energyField(BIFROST_MARK));
        for (Effect e : EFFECTS) {
            if (e.kind() == ThorFxPayload.BIFROST_MARK) {
                float age = (time - e.start()) / MARK_TICKS;
                // Burning bright at first, then slowly cooling and fading.
                float alpha = age < 0.02F ? age / 0.02F : 1.0F - Mth.clamp((age - 0.15F) / 0.85F, 0, 1);
                runeCircle(pose, marks, e.points().get(0).subtract(camera), 3.2F, alpha);
            }
        }
        buffers.endBatch(EnhancedRenderTypes.energyField(BIFROST_MARK));

        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Effect e : EFFECTS) {
            float age = time - e.start();
            switch (e.kind()) {
                case ThorFxPayload.CHANNEL -> {
                    Entity thor = level.getEntity(e.entityId());
                    if (thor instanceof AbstractClientPlayer player) {
                        Vec3 hammer = hammerAloft(mc, player, partialTick);
                        Vec3 sky = hammer.add(0, 40, 0);
                        float fade = 1.0F - age / CHANNEL_TICKS;
                        long seed = (long) (time / 2) * 7919L + e.start();
                        lightning(pose, vc, sky.subtract(camera), hammer.subtract(camera), 0.35F, fade, seed);
                        lightning(pose, vc, hammer.subtract(camera), e.points().get(0).subtract(camera), 0.3F, fade, seed + 13);
                    }
                }
                case ThorFxPayload.ARC -> {
                    float fade = 1.0F - age / ARC_TICKS;
                    long seed = (long) (time / 2) * 104729L + e.start();
                    for (int i = 0; i + 1 < e.points().size(); i++) {
                        lightning(pose, vc, e.points().get(i).subtract(camera), e.points().get(i + 1).subtract(camera), 0.2F, fade, seed + i);
                    }
                }
                case ThorFxPayload.BIFROST_ARRIVAL -> {
                    float fade = 1.0F - age / ARRIVAL_TICKS;
                    rainbowBeam(pose, vc, e.points().get(0).subtract(camera), 1.6F + 0.8F * fade, time, fade);
                }
                default -> {
                }
            }
        }
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (data.power() != Power.THOR || player.isInvisible()) {
                continue;
            }
            if (data.is(Live.BIFROST)) {
                Vec3 feet = player.getPosition(partialTick).subtract(camera);
                float pulse = 0.85F + 0.15F * Mth.sin(time * 0.9F);
                rainbowBeam(pose, vc, feet, 1.3F * pulse, time, 1.0F);
            }
            if (data.toggle(Toggle.GOD_OF_THUNDER)) {
                crackle(pose, vc, player, camera, partialTick, time);
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    /** Where Mjolnir is when Thor raises it to the sky: above his head, a little forward. */
    private static Vec3 hammerAloft(Minecraft mc, AbstractClientPlayer player, float partialTick) {
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        Vec3 eye = player.getEyePosition(partialTick);
        if (firstPerson) {
            return eye.add(player.getViewVector(partialTick).scale(1.2)).add(0, 1.4, 0);
        }
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double side = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? -0.38 : 0.38;
        return new Vec3(player.getX() + Math.cos(yaw) * side, eye.y + 1.1, player.getZ() + Math.sin(yaw) * side);
    }

    /** A bolt of MCU lightning: a wide deep-blue halo, an electric blue bolt and a white-hot core, flickering. */
    private static void lightning(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, float fade, long seed) {
        if (fade <= 0) {
            return;
        }
        float jitter = (float) Math.min(1.6, a.distanceTo(b) * 0.06 + 0.2);
        EnergyGeometry.bolt(pose, vc, a, b, width * 1.6F, jitter, EnergyGeometry.argb(HALO, 0.3F * fade), seed);
        EnergyGeometry.bolt(pose, vc, a, b, width * 0.7F, jitter, EnergyGeometry.argb(BLUE, 0.75F * fade), seed);
        EnergyGeometry.bolt(pose, vc, a, b, width * 0.25F, jitter, EnergyGeometry.argb(CORE, 0.95F * fade), seed);
        // A forked branch.
        Vec3 mid = a.lerp(b, 0.45);
        Vec3 fork = mid.add(new Vec3((seed % 7) - 3, -Math.abs(seed % 5), (seed % 3) - 1).normalize().scale(a.distanceTo(b) * 0.15));
        EnergyGeometry.bolt(pose, vc, mid, fork, width * 0.35F, jitter * 0.6F, EnergyGeometry.argb(BLUE, 0.5F * fade), seed + 5);
    }

    /** Short arcs of lightning crawling over the God of Thunder's body, changing every other tick. */
    private static void crackle(Matrix4f pose, VertexConsumer vc, AbstractClientPlayer player, Vec3 camera, float partialTick, float time) {
        RandomSource random = RandomSource.create((long) (time / 2) * 31L + player.getId());
        Vec3 base = player.getPosition(partialTick).subtract(camera);
        for (int i = 0; i < 4; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double y = 0.2 + random.nextDouble() * 1.6;
            Vec3 a = base.add(Math.cos(angle) * 0.38, y, Math.sin(angle) * 0.38);
            Vec3 b = a.add((random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.7, (random.nextDouble() - 0.5) * 0.6);
            EnergyGeometry.bolt(pose, vc, a, b, 0.03F, 0.12F, EnergyGeometry.argb(BLUE, 0.8F), random.nextLong());
            EnergyGeometry.bolt(pose, vc, a, b, 0.012F, 0.12F, EnergyGeometry.argb(CORE, 0.9F), random.nextLong());
        }
    }

    /** The Bifrost: a column of rainbow light, its colours swirling round, a white core, from the sky to {@code feet}. */
    private static void rainbowBeam(Matrix4f pose, VertexConsumer vc, Vec3 feet, float radius, float time, float fade) {
        int sides = 24;
        float height = 220F;
        for (int k = 0; k < sides; k++) {
            float a0 = Mth.TWO_PI * k / sides + time * 0.05F;
            float a1 = Mth.TWO_PI * (k + 1) / sides + time * 0.05F;
            int color = hue((float) k / sides + time * 0.01F);
            Vec3 p0 = feet.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
            Vec3 p1 = feet.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            quad(pose, vc, p0, p1, height, EnergyGeometry.argb(color, 0.42F * fade), EnergyGeometry.argb(color, 0.0F));
            // An inner, brighter sheath.
            Vec3 q0 = feet.add(Math.cos(a0) * radius * 0.45, 0, Math.sin(a0) * radius * 0.45);
            Vec3 q1 = feet.add(Math.cos(a1) * radius * 0.45, 0, Math.sin(a1) * radius * 0.45);
            quad(pose, vc, q0, q1, height, EnergyGeometry.argb(0xFFFFFF, 0.16F * fade), EnergyGeometry.argb(color, 0.04F * fade));
        }
    }

    /** A vertical quad from the ground edge {@code a}-{@code b} up {@code height} blocks, fading upward. */
    private static void quad(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float height, int bottom, int top) {
        vc.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(bottom);
        vc.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(bottom);
        vc.addVertex(pose, (float) b.x, (float) b.y + height, (float) b.z).setColor(top);
        vc.addVertex(pose, (float) a.x, (float) a.y + height, (float) a.z).setColor(top);
    }

    /** A bright, saturated colour around the colour wheel (0..1). */
    private static int hue(float h) {
        return Mth.hsvToRgb(((h % 1.0F) + 1.0F) % 1.0F, 0.75F, 1.0F);
    }

    /** The circle of runes: a textured square lying flat on the ground. */
    private static void runeCircle(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        int color = EnergyGeometry.argb(0xFFD9A0, alpha);
        float x = (float) center.x;
        float y = (float) center.y + 0.02F;
        float z = (float) center.z;
        vc.addVertex(pose, x - radius, y, z - radius).setUv(0, 0).setColor(color);
        vc.addVertex(pose, x + radius, y, z - radius).setUv(1, 0).setColor(color);
        vc.addVertex(pose, x + radius, y, z + radius).setUv(1, 1).setColor(color);
        vc.addVertex(pose, x - radius, y, z + radius).setUv(0, 1).setColor(color);
    }

    public static void clear() {
        EFFECTS.clear();
    }

    private ThorEffects() {
    }
}

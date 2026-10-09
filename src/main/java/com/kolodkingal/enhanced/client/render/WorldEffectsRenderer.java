package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Draws the energy effects that live in the world rather than on a body: the Hex walls, the Chaos Beam, the Chaos
 * Shield and the telekinetic tether. Everything is drawn for every player, including yourself in first person.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class WorldEffectsRenderer {
    public static final ResourceLocation HEX_TEXTURE = Enhanced.id("textures/entity/hex_wall.png");
    private static final Vec3 UP = new Vec3(0, 1, 0);

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
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float time = level.getGameTime() + partialTick;

        RenderType fieldType = EnhancedRenderTypes.energyField(HEX_TEXTURE);
        VertexConsumer field = buffers.getBuffer(fieldType);
        renderHexes(level, pose, field, camera, time, mc.options.getEffectiveRenderDistance() * 16.0);

        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (data.power() == Power.SCARLET_WITCH && data.chaosShieldActive() && !player.isInvisible()) {
                Vec3 center = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0).subtract(camera);
                float flicker = 0.9F + 0.1F * Mth.sin(time * 0.6F);
                EnergyGeometry.sphere(pose, field, center, (float) WandaAbilities.SHIELD_RADIUS, 32, 16, 6, 3, time * 0.01F,
                        Power.SCARLET_WITCH.glow, 0.10F * flicker, 0.8F * flicker);
            }
        }
        buffers.endBatch(fieldType);

        VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (data.power() == Power.SCARLET_WITCH && data.beamActive()) {
                renderChaosBeam(mc, player, pose, beams, camera, partialTick, time);
            }
            if (data.heldEntityId() != -1) {
                Entity held = level.getEntity(data.heldEntityId());
                if (held != null) {
                    renderTether(mc, player, held, data.power(), pose, beams, camera, partialTick, time);
                }
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    // ---- The Hex ----

    private static void renderHexes(ClientLevel level, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time, double renderDistance) {
        for (Hex hex : level.getData(ModAttachments.HEXES).hexes()) {
            float grow = Mth.clamp((time - hex.createdAt()) / 50F, 0F, 1F);
            grow = grow * grow * (3 - 2 * grow);
            float radius = hex.radius() * grow;
            if (radius < 0.5F) {
                continue;
            }
            Vec3 center = hex.center().subtract(camera);
            double distance = center.length();
            if (distance - radius > renderDistance + 32 || radius - distance > renderDistance + 32) {
                continue;
            }
            int segments = Mth.clamp((int) (radius * 2), 32, 128);
            float tilesU = Math.max(6, Math.round(Mth.TWO_PI * radius / 6F));
            float tilesV = Math.max(3, Math.round(Mth.PI * radius / 6F));
            float pulse = 0.85F + 0.15F * Mth.sin(time * 0.06F);
            // A wall that is still rising glows brighter.
            float rising = 1F + (1F - grow) * 1.5F;
            EnergyGeometry.sphere(pose, vc, center, radius, segments, segments / 2, tilesU, tilesV, time * 0.0015F,
                    Power.SCARLET_WITCH.color, 0.06F * pulse * rising, 0.6F * pulse * rising);
        }
    }

    // ---- Chaos Beam ----

    private static void renderChaosBeam(Minecraft mc, AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc,
                                        Vec3 camera, float partialTick, float time) {
        double range = EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.CHAOS_BEAM_RANGE.get() : 32;
        Vec3 end = Raycasts.any(player, range, WandaAbilities.BEAM_TARGET, partialTick).getLocation().subtract(camera);
        Vec3 start = handPosition(mc, player, partialTick).subtract(camera);

        float flicker = 0.85F + 0.15F * Mth.sin(time * 1.7F) * Mth.cos(time * 0.9F);
        int dark = 0x7A0010;
        int red = Power.SCARLET_WITCH.color;
        int hot = 0xFF9AAA;
        EnergyGeometry.ribbon(pose, vc, start, end, 0.60F * flicker, EnergyGeometry.argb(dark, 0.30F), EnergyGeometry.argb(dark, 0.18F));
        EnergyGeometry.ribbon(pose, vc, start, end, 0.30F * flicker, EnergyGeometry.argb(red, 0.65F), EnergyGeometry.argb(red, 0.45F));
        EnergyGeometry.ribbon(pose, vc, start, end, 0.10F, EnergyGeometry.argb(hot, 0.95F), EnergyGeometry.argb(hot, 0.8F));

        long seed = ((long) time / 2) * 341873128712L + player.getId() * 132897987541L;
        for (int k = 0; k < 3; k++) {
            EnergyGeometry.bolt(pose, vc, start, end, 0.04F, 0.28F, EnergyGeometry.argb(0xFF3355, 0.85F), seed + k * 7919L);
        }
        // A flare where the beam hits.
        float flare = 0.5F + 0.2F * Mth.sin(time * 2.3F);
        Vec3 toCamera = end.scale(-1).normalize();
        Vec3 side = toCamera.cross(UP);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(toCamera).normalize();
        EnergyGeometry.ribbon(pose, vc, end.subtract(side.scale(flare)), end.add(side.scale(flare)), flare, EnergyGeometry.argb(red, 0.7F), EnergyGeometry.argb(red, 0.7F));
        EnergyGeometry.ribbon(pose, vc, end.subtract(up.scale(flare)), end.add(up.scale(flare)), flare * 0.5F, EnergyGeometry.argb(hot, 0.6F), EnergyGeometry.argb(hot, 0.6F));
    }

    // ---- Telekinesis ----

    private static void renderTether(Minecraft mc, AbstractClientPlayer player, Entity held, Power power, Matrix4f pose,
                                     VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        Vec3 start = handPosition(mc, player, partialTick).subtract(camera);
        Vec3 end = held.getPosition(partialTick).add(0, held.getBbHeight() * 0.5, 0).subtract(camera);
        long seed = ((long) time / 3) * 6364136223846793005L + player.getId();
        EnergyGeometry.bolt(pose, vc, start, end, 0.05F, 0.35F, EnergyGeometry.argb(power.glow, 0.55F), seed);
        EnergyGeometry.bolt(pose, vc, start, end, 0.03F, 0.5F, EnergyGeometry.argb(power.color, 0.45F), seed + 31);
    }

    /** Where energy leaves the player's hands: in front of the chest, or bottom-right of the screen in first person. */
    public static Vec3 handPosition(Minecraft mc, AbstractClientPlayer player, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            Vec3 right = look.cross(UP);
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(look).normalize();
            return eye.add(look.scale(0.55)).add(right.scale(0.2)).add(up.scale(-0.22));
        }
        return eye.add(0, -0.3, 0).add(look.scale(0.75));
    }

    private WorldEffectsRenderer() {
    }
}

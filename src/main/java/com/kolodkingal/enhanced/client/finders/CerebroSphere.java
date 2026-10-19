package com.kolodkingal.enhanced.client.finders;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.CerebroView;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.finders.CerebroRoom;
import com.kolodkingal.enhanced.network.CerebroPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.world.SchoolLabPlan;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The lab's Cerebro on the client (3.7, lane L5; {@code CerebroView}'s sphere mode): while the amplified Cerebro
 * reports to this client ({@link CerebroView#amplified()}) and the player stands inside the sphere of a Cerebro
 * console, each mind it found is a glowing star on the sphere's inner surface in the direction of where that player
 * stands (the nearer, the higher; the far ones low over the walkway's level), in the hero's colour (a pale white for
 * the powerless), with the name; the minds of another dimension are a cluster on the sphere's floor under its name; the
 * stone sites are diamonds in their stone's colour; the sphere's bands of light pulse. Quads only: the world is not
 * changed. Outside the sphere the plain markers of {@link CerebroView} stay.
 *
 * <p>It also keeps the consoles that are on ({@code LabFxPayload.CONSOLE_ON|OFF}), with their hum ({@code
 * cerebro_amplified}, looped while on); lane L4's console renderer may ask {@link #consoleOn} to lift the helmet off
 * its stand.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class CerebroSphere {
    /** The stars stand this far in from the sphere's inner surface. */
    static final double INSET = 0.7;
    /** A console's light and hum stop by themselves this long after it went on (if the "off" never came). */
    static final int CONSOLE_MAX_TICKS = 20 * 60 * 2;

    /** The consoles that are on, and since when (game time). */
    private static final Map<BlockPos, Long> CONSOLES = new HashMap<>();
    /** The sphere the player stands in (its centre), or null. */
    @Nullable
    private static Vec3 center;
    private static long scannedAt;

    /** Whether the Cerebro console at {@code pos} is on (its helmet off the stand, on a telepath's head). */
    public static boolean consoleOn(BlockPos pos) {
        return CONSOLES.containsKey(pos);
    }

    /** Whether the player stands inside a Cerebro sphere while the amplified Cerebro reports to him. */
    public static boolean inside() {
        return center != null && CerebroView.amplified();
    }

    static void console(BlockPos pos, boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (on) {
            if (CONSOLES.put(pos.immutable(), mc.level == null ? 0L : mc.level.getGameTime()) == null) {
                mc.getSoundManager().play(new Hum(pos.immutable()));
            }
        } else {
            CONSOLES.remove(pos);
        }
    }

    static void clear() {
        CONSOLES.clear();
        center = null;
    }

    /** Every client tick: which sphere the player stands in (the consoles near, looked for once a second). */
    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            center = null;
            return;
        }
        long now = level.getGameTime();
        CONSOLES.values().removeIf(since -> now - since > CONSOLE_MAX_TICKS || since > now);
        if (!CerebroView.amplified()) {
            center = null;
            return;
        }
        if (center != null && now - scannedAt < 20) {
            return;
        }
        scannedAt = now;
        center = null;
        Vec3 eye = mc.player.getEyePosition();
        double r = SchoolLabPlan.CEREBRO_RADIUS + 0.5;
        for (BlockPos pos : CONSOLES.keySet()) {
            Vec3 c = sphere(level.getBlockState(pos), pos);
            if (c.distanceToSqr(eye) <= r * r) {
                center = c;
                return;
            }
        }
        // A console that went on before he came near: look round him for one.
        BlockPos at = mc.player.blockPosition();
        int reach = SchoolLabPlan.CEREBRO_RADIUS + CerebroRoom.CONSOLE_TO_CENTER + 1;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-reach, -4, -reach), at.offset(reach, 4, reach))) {
            if (level.getBlockState(pos).is(ModBlocks.CEREBRO_CONSOLE.get())) {
                Vec3 c = sphere(level.getBlockState(pos), pos);
                if (c.distanceToSqr(eye) <= r * r) {
                    center = c;
                    return;
                }
            }
        }
    }

    /** The centre of the sphere of a console (the block's middle at the platform's height). */
    static Vec3 sphere(BlockState state, BlockPos console) {
        BlockPos c = CerebroRoom.sphereCenter(state, console);
        return new Vec3(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Where the stars stand

    /**
     * The direction (unit) of a star for a mind at {@code offset} from the sphere's centre: its compass direction, and
     * an elevation from 70 degrees (beside him) down to 8 (ten thousand blocks off and more).
     */
    public static Vec3 starDirection(Vec3 offset) {
        double flat = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        double azimuth = Math.atan2(offset.z, offset.x);
        double near = 1.0 - Mth.clamp(Math.log10(flat + 1.0) / 4.0, 0.0, 1.0);
        double elevation = Math.toRadians(8.0 + 62.0 * near);
        return new Vec3(Math.cos(elevation) * Math.cos(azimuth), Math.sin(elevation),
                Math.cos(elevation) * Math.sin(azimuth));
    }

    /**
     * The direction of the cluster of another dimension's minds: low on the sphere's floor, round it by the
     * dimension's name, each mind a little apart by its own name.
     */
    public static Vec3 clusterDirection(String dimension, String name) {
        RandomSource random = RandomSource.create(dimension.hashCode());
        double azimuth = random.nextDouble() * Math.PI * 2.0;
        double elevation = Math.toRadians(-38.0);
        if (!name.isEmpty()) {
            RandomSource own = RandomSource.create(name.hashCode() * 31L + dimension.hashCode());
            azimuth += (own.nextDouble() - 0.5) * 0.45;
            elevation += Math.toRadians((own.nextDouble() - 0.5) * 18.0);
        }
        return new Vec3(Math.cos(elevation) * Math.cos(azimuth), Math.sin(elevation),
                Math.cos(elevation) * Math.sin(azimuth));
    }

    /** A dimension's name as players read it ({@code dimension.<namespace>.<path>}; else its path). */
    public static Component dimensionName(String dimension) {
        int colon = dimension.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : dimension.substring(0, colon);
        String path = dimension.substring(colon + 1);
        return Component.translatableWithFallback("dimension." + namespace + "." + path, path);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !inside()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = mc.level.getGameTime() + pt;
        float fade = Mth.clamp(CerebroView.ticksLeft() / 10.0F, 0.0F, 1.0F);
        Camera view = event.getCamera();
        Vec3 camera = view.getPosition();
        Vec3 middle = center;
        double radius = SchoolLabPlan.CEREBRO_RADIUS - INSET;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer glow = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        bands(glow, middle.subtract(camera), radius + 0.4, time, fade);
        Power[] powers = Power.values();
        List<CerebroPayload.Mind> minds = CerebroView.minds();
        Map<String, Vec3> clusters = new LinkedHashMap<>();
        record Label(Vec3 at, Component text, int rgb) {
        }
        java.util.List<Label> labels = new java.util.ArrayList<>();
        for (CerebroPayload.Mind mind : minds) {
            Power power = mind.power() >= 0 && mind.power() < powers.length ? powers[mind.power()] : Power.NONE;
            int rgb = power.hasPowers() ? power.titleColor() : 0xE8EEFF;
            Vec3 dir;
            if (mind.dimension().isEmpty()) {
                dir = starDirection(mind.pos().subtract(middle));
            } else {
                dir = clusterDirection(mind.dimension(), mind.name());
                clusters.putIfAbsent(mind.dimension(), clusterDirection(mind.dimension(), ""));
            }
            Vec3 at = middle.add(dir.scale(radius));
            float twinkle = 1.0F + 0.18F * Mth.sin(time * 0.21F + mind.name().hashCode());
            star(glow, view, at.subtract(camera), 0.22F * twinkle, rgb, 0.95F * fade, false);
            star(glow, view, at.subtract(camera), 0.6F * twinkle, rgb, 0.28F * fade, false);
            labels.add(new Label(at.add(0.0, -0.45, 0.0), Component.literal(mind.name()), rgb));
        }
        for (Map.Entry<String, Vec3> cluster : clusters.entrySet()) {
            Vec3 at = middle.add(cluster.getValue().scale(radius)).add(0.0, 1.1, 0.0);
            labels.add(new Label(at, dimensionName(cluster.getKey()), 0xC8D4FF));
        }
        StoneKind[] kinds = StoneKind.values();
        for (CerebroPayload.Site site : CerebroView.sites()) {
            StoneKind kind = kinds[Math.floorMod(site.kind(), kinds.length)];
            Vec3 offset = Vec3.atCenterOf(site.pos()).subtract(middle);
            Vec3 at = middle.add(starDirection(offset).scale(radius - 0.2));
            float pulse = 1.0F + 0.25F * Mth.sin(time * 0.15F + kind.ordinal());
            star(glow, view, at.subtract(camera), 0.34F * pulse, kind.color(), 0.95F * fade, true);
            star(glow, view, at.subtract(camera), 0.8F * pulse, kind.color(), 0.25F * fade, true);
            double dist = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            labels.add(new Label(at.add(0.0, -0.55, 0.0), Component.translatable("hud.enhanced.cerebro.site",
                    kind.displayName(), Math.round(dist)), kind.color()));
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        Font font = mc.font;
        for (Label label : labels) {
            Vec3 at = label.at().subtract(camera);
            PoseStack stack = new PoseStack();
            stack.translate(at.x, at.y, at.z);
            stack.mulPose(view.rotation());
            float scale = 0.016F;
            stack.scale(scale, -scale, scale);
            Matrix4f matrix = stack.last().pose();
            int a = Math.max(5, Math.round(235 * fade)) << 24;
            font.drawInBatch(label.text(), -font.width(label.text()) / 2.0F, 0.0F, a | label.rgb(), false, matrix,
                    buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
    }

    /** A star (a cross of light) or a diamond, facing the camera, at {@code at} (camera-relative). */
    private static void star(VertexConsumer out, Camera view, Vec3 at, float size, int rgb, float alpha,
                             boolean diamond) {
        PoseStack stack = new PoseStack();
        stack.translate(at.x, at.y, at.z);
        stack.mulPose(view.rotation());
        Matrix4f m = stack.last().pose();
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        int a = Mth.clamp(Math.round(alpha * 255), 0, 255);
        if (diamond) {
            quad(out, m, 0, -size, size, 0, 0, size, -size, 0, r, g, b, a);
            return;
        }
        float thin = size * 0.28F;
        quad(out, m, -size, -thin, size, -thin, size, thin, -size, thin, r, g, b, a);
        quad(out, m, -thin, -size, thin, -size, thin, size, -thin, size, r, g, b, a);
        quad(out, m, -thin * 1.6F, -thin * 1.6F, thin * 1.6F, -thin * 1.6F, thin * 1.6F, thin * 1.6F,
                -thin * 1.6F, thin * 1.6F, Math.min(255, r + 60), Math.min(255, g + 60), Math.min(255, b + 60), a);
    }

    private static void quad(VertexConsumer out, Matrix4f m, float x0, float y0, float x1, float y1, float x2,
                             float y2, float x3, float y3, int r, int g, int b, int a) {
        out.addVertex(m, x0, y0, 0.0F).setColor(r, g, b, a);
        out.addVertex(m, x1, y1, 0.0F).setColor(r, g, b, a);
        out.addVertex(m, x2, y2, 0.0F).setColor(r, g, b, a);
        out.addVertex(m, x3, y3, 0.0F).setColor(r, g, b, a);
    }

    /** The sphere's bands of light (the equator and two latitudes), pulsing slowly in Cerebro's blue. */
    private static void bands(VertexConsumer out, Vec3 middle, double radius, float time, float fade) {
        Matrix4f m = new PoseStack().last().pose();
        double[] latitudes = {0.0, Math.toRadians(28.0), Math.toRadians(-28.0)};
        int segments = 72;
        for (int band = 0; band < latitudes.length; band++) {
            double lat = latitudes[band];
            double ring = radius * Math.cos(lat);
            double y = radius * Math.sin(lat);
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.12F - band * 1.3F);
            int a = Math.round((band == 0 ? 70 : 40) * (0.4F + 0.6F * pulse) * fade);
            float half = band == 0 ? 0.07F : 0.04F;
            for (int i = 0; i < segments; i++) {
                double a0 = i * Math.PI * 2.0 / segments;
                double a1 = (i + 1) * Math.PI * 2.0 / segments;
                float x0 = (float) (middle.x + Math.cos(a0) * ring);
                float z0 = (float) (middle.z + Math.sin(a0) * ring);
                float x1 = (float) (middle.x + Math.cos(a1) * ring);
                float z1 = (float) (middle.z + Math.sin(a1) * ring);
                float yy = (float) (middle.y + y);
                out.addVertex(m, x0, yy - half, z0).setColor(0x9E, 0xB4, 0xFF, a);
                out.addVertex(m, x1, yy - half, z1).setColor(0x9E, 0xB4, 0xFF, a);
                out.addVertex(m, x1, yy + half, z1).setColor(0x9E, 0xB4, 0xFF, a);
                out.addVertex(m, x0, yy + half, z0).setColor(0x9E, 0xB4, 0xFF, a);
            }
        }
    }

    /** The console's hum, looped while it is on. */
    private static final class Hum extends AbstractTickableSoundInstance {
        private final BlockPos pos;

        Hum(BlockPos pos) {
            super(ModSounds.CEREBRO_AMPLIFIED.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.9F;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
        }

        @Override
        public void tick() {
            if (!consoleOn(pos)) {
                stop();
            }
        }
    }

    private CerebroSphere() {
    }
}

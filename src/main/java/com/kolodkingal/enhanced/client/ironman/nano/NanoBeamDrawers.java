package com.kolodkingal.enhanced.client.ironman.nano;

import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.IronManBeamFx;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * How the wide chest beams of lane L4 are drawn (3.8), as {@link BeamRenderer.Drawer}s on lane L3's straight Unibeam
 * ({@link IronManBeamFx#chestBeam}): the Mark 85's giant <b>chest laser</b> (white-cyan, 0.75 across) and the
 * Hulkbuster's heavy <b>Unibeam</b> (white-blue, 1.0 across, from the reactor of a 3.5× chest), both from the reactor
 * to the first block along his look (they go through creatures, as the server's beams do).
 */
public final class NanoBeamDrawers {
    /** The Unibeam's widths are lane L3's (0.35 across): these beams are drawn that many times wider. */
    static final float[] WIDTHS = {0.35F, 0.24F, 0.15F, 0.07F, 0.03F};
    static final float[] ALPHAS = {0.26F, 0.5F, 0.7F, 0.65F, 0.9F};

    public static final BeamRenderer.Drawer CHEST_LASER = drawer(0.75F / 0.35F);
    public static final BeamRenderer.Drawer HEAVY_UNIBEAM = drawer(1.0F / 0.35F);

    private static BeamRenderer.Drawer drawer(float scale) {
        return new BeamRenderer.Drawer() {
            @Override
            public Vec3 origin(Minecraft mc, AbstractClientPlayer player, BeamStyle style, float partialTick) {
                return from(mc, player, partialTick);
            }

            @Override
            public Vec3 end(AbstractClientPlayer player, BeamStyle style, float range, Vec3 crosshair,
                            float partialTick) {
                Vec3 from = from(Minecraft.getInstance(), player, partialTick);
                Vec3 aim = player.getEyePosition(partialTick).add(player.getViewVector(partialTick).scale(range));
                HitResult wall = player.level().clip(new ClipContext(from, aim, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
                return wall.getType() == HitResult.Type.MISS ? aim : wall.getLocation();
            }

            @Override
            public void draw(Minecraft mc, AbstractClientPlayer player, BeamStyle style, Matrix4f pose,
                             VertexConsumer vc, Vec3 a, Vec3 b, float partialTick, float time) {
                IronManBeamFx.chestBeam(pose, vc, a, b, style, time, WIDTHS, ALPHAS,
                        scale * Math.max(1.0F, player.getScale() * 0.5F));
            }
        };
    }

    /**
     * Where the beam leaves: the reactor; in the camera's own first-person view a little before it and lower (from the
     * reactor itself, its ring would fill the screen).
     */
    static Vec3 from(Minecraft mc, AbstractClientPlayer player, float partialTick) {
        Vec3 reactor = IronManBeamFx.reactor(player, partialTick);
        if (mc.getCameraEntity() == player && mc.options.getCameraType().isFirstPerson()) {
            return reactor.add(player.getViewVector(partialTick).scale(2.0 * player.getScale()))
                    .add(0.0, -0.35 * player.getScale(), 0.0);
        }
        return reactor;
    }

    private NanoBeamDrawers() {
    }
}

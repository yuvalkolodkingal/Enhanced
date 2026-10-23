package com.kolodkingal.enhanced.client.ironman.nano;

import com.kolodkingal.enhanced.ironman.nano.NanoDroneEntity;
import com.kolodkingal.enhanced.ironman.nano.WingFormation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a Mark 85 nanotech drone (3.8, lane L4; since 3.8.3 the Endgame wing unit): its two crescent blades ({@link
 * NanoDroneModel}) beside its Iron Man's arm, placed by {@link WingDraw} from him (not from the drone's own synced
 * place, so they never lag behind in flight), growing out of his arm when it forms, flickering white when something
 * hits it.
 */
public class NanoDroneRenderer extends EntityRenderer<NanoDroneEntity> {
    public NanoDroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        WingDraw.model = new NanoDroneModel(context.bakeLayer(NanoDroneModel.LAYER));
        shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(NanoDroneEntity drone, Frustum frustum, double camX, double camY, double camZ) {
        // The blades are drawn from him, wide of the drone's own little box: no frustum test on that box.
        return drone.owner() != null && drone.shouldRender(camX, camY, camZ);
    }

    @Override
    public void render(NanoDroneEntity drone, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        Player owner = drone.owner();
        if (owner == null) {
            return;
        }
        WingDraw.seen(owner, drone.slot());
        Vec3 origin = new Vec3(Mth.lerp(partialTick, drone.xo, drone.getX()),
                Mth.lerp(partialTick, drone.yo, drone.getY()), Mth.lerp(partialTick, drone.zo, drone.getZ()));
        float age = drone.tickCount + partialTick;
        // A hit: the metal flashes full bright every other half tick.
        boolean flash = drone.flicker(partialTick) > 0.0F && ((int) (age * 2.0F) & 1) == 0;
        int lit = flash ? LightTexture.FULL_BRIGHT : light;
        int side = WingFormation.side(drone.slot());
        for (int blade = 0; blade < WingFormation.BLADES; blade++) {
            int rank = WingFormation.rank(drone.slot(), blade);
            float shown = WingFormation.grown(rank, age);
            WingDraw.Pose pose = WingDraw.pose(owner, side, rank, partialTick, shown);
            WingDraw.draw(poseStack, buffer, origin, pose, shown, lit, 0xFFFFFFFF);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NanoDroneEntity drone) {
        return WingDraw.METAL;
    }
}

package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.entity.JimFireballEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Jim Hammond's fireball (3.17): a ball of living fire. A white-hot heart in an orange glow, wrapped in flame sheets that always face
 * the camera and lick outward and up, a tail of fire streaming back along its flight and thin smoke behind that; all of it
 * {@link FireSheet}s, whose flames rise continuously (no frames, no flicker), layered the way game fire is (an added-light core, see-through
 * coloured flames over it, dark smoke behind), with sparks shed from it as it flies. Bigger the longer it was charged.
 */
public class JimFireballRenderer extends EntityRenderer<JimFireballEntity> {
    private static final int CORE = 0xFFF2C0;
    private static final int HALO = 0xFF8A20;

    public JimFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(JimFireballEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float size = entity.size();
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
        Matrix4f m = poseStack.last().pose();
        // the heart: a white-hot core in an orange halo that breathes
        float pulse = 0.92F + 0.08F * Mth.sin(age * 0.9F);
        // (added light washes to white on a bright sky, so only the small heart is added; the orange body is see-through colour)
        VertexConsumer body = buffer.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        EnergyGeometry.veil(m, body, Vec3.ZERO, 0.30F * size * pulse, 14, 7, HALO, 0.75F, 0.15F);
        VertexConsumer glow = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        EnergyGeometry.veil(m, glow, Vec3.ZERO, 0.15F * size * pulse, 12, 6, CORE, 0.7F, 0.2F);
        // the flames: where it is (camera-relative) and which way it flies
        Matrix4f inverse = new Matrix4f(m).invert();
        Vector3f centre = m.transformPosition(new Vector3f());
        Vec3 motion = entity.getDeltaMovement();
        Vector3f back = new Vector3f((float) -motion.x, (float) -motion.y, (float) -motion.z);
        if (back.lengthSquared() < 1.0E-6F) {
            back.set(0, -1, 0);
        }
        back.normalize();
        Vector3f base = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f up = new Vector3f();
        float seed = entity.getId() * 0.37F;
        for (int pass = FireSheet.DECAL; pass <= FireSheet.GLOW; pass++) {
            VertexConsumer vc = buffer.getBuffer(FireSheet.type(pass));
            if (pass == FireSheet.DECAL) {
                // the smoke the fire leaves behind it: drawn first, so the flames lie over it
                Vector3f smoke = new Vector3f(back).add(0, 0.35F, 0).normalize(2.6F * size);
                FireSheet.frame(inverse, new Vector3f(centre).add(new Vector3f(back).mul(0.6F * size)), smoke, 0.5F * size, base, right, up);
                FireSheet.draw(FireSheet.SMOKE, m, vc, base, right, up, age, seed + 1.3F, 0.03F, 0.75F);
            }
            // the tail: long sheets streaming back from the ball, a little up (flames rise)
            Vector3f tail = new Vector3f(back).add(0, 0.25F, 0).normalize(1.9F * size);
            FireSheet.frame(inverse, new Vector3f(centre).sub(new Vector3f(back).mul(0.15F * size)), tail, 0.40F * size, base, right, up);
            FireSheet.draw(pass, m, vc, base, right, up, age, seed, 0.05F, 1.0F);
            Vector3f tail2 = new Vector3f(back).add(0, 0.45F, 0).normalize(1.3F * size);
            FireSheet.frame(inverse, new Vector3f(centre).sub(new Vector3f(back).mul(0.05F * size)), tail2, 0.42F * size, base, right, up);
            FireSheet.draw(pass, m, vc, base, right, up, age, seed + 0.4F, 0.035F, 0.8F);
            // the ball itself: flames licking up all round it
            Vector3f rise = new Vector3f(0, 1, 0).add(new Vector3f(back).mul(0.6F)).normalize(0.9F * size);
            FireSheet.frame(inverse, new Vector3f(centre).sub(0, 0.28F * size, 0), rise, 0.36F * size, base, right, up);
            FireSheet.draw(pass, m, vc, base, right, up, age, seed + 0.8F, 0.06F, 1.0F);
            // and a shorter, wider sheet low round the ball, so it reads as a ball of fire from every side
            FireSheet.frame(inverse, new Vector3f(centre).sub(0, 0.38F * size, 0), new Vector3f(0, 0.75F * size, 0), 0.46F * size, base, right, up);
            FireSheet.draw(pass, m, vc, base, right, up, age, seed + 1.9F, 0.07F, 0.9F);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(JimFireballEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

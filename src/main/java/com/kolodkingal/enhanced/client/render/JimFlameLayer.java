package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The fire of Jim Hammond (3.17), shared with Source Code Vision while he flies: while Jim flies in flames ({@link Live#JIM_FLAMING}), and brighter while he gathers the Nova Flame, his
 * own skin glows and he goes up in flames.
 * <ol>
 *   <li><b>The glow</b>: his current suit (the white android suit, the classic red one, any look) stays fully visible, drawn again
 *   full-bright so the world's light does not touch it, with a soft warm light over it that pulses.</li>
 *   <li><b>The sheath</b>: three larger copies of the posed model (head, torso, both arms, both legs), each in its own animated
 *   flame texture that rises at its own speed, mostly transparent so the suit shows through, with ragged cut-out edges, so the
 *   flames rise off him and the outline is thicker than the body all round.</li>
 *   <li><b>The tongues</b>: real flames, crossed quads anchored on the model's own parts (the top of the head, shoulders, arms,
 *   hands, back, legs), tallest on the head and shoulders. They are built in each part's frame, so they turn and tilt with the body
 *   in every pose; they lean away from his motion (swept back in flight, straight up when he hovers).</li>
 * </ol>
 * The flames are see-through and unlit: glowing fire, bright cores fading to transparent red edges, in fire's own colours against any sky. The streaming trail and
 * the embers are {@link JimFx}'s. In first person his own body is not drawn, so none of it can block the view.
 */
public class JimFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    static final int SHELL_FRAMES = 12;
    static final int TONGUE_FRAMES = 8;
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final ResourceLocation GLOW = tex("glow");

    static ResourceLocation tex(String name) {
        return Enhanced.id("textures/entity/jim/" + name + ".png");
    }

    /** Where a tongue stands on a part: pixels in the part's own frame (model space: y grows downward), its height and sway. */
    private record Tongue(float x, float y, float z, float height) {
    }

    private static final Tongue[] HEAD = {
            new Tongue(0, -8, 0, 20), new Tongue(-3F, -8, -1.5F, 15), new Tongue(3F, -8, -1.5F, 15),
            new Tongue(-2.5F, -8, 2.5F, 16), new Tongue(2.5F, -8, 2.5F, 16), new Tongue(0, -6, 4.4F, 13),
            new Tongue(-4.2F, -4, 0, 9), new Tongue(4.2F, -4, 0, 9)};
    private static final Tongue[] BODY = {
            new Tongue(-3F, 0, 2.7F, 15), new Tongue(3F, 0, 2.7F, 15), new Tongue(0, 3, 2.9F, 13), new Tongue(-2, 8, 2.9F, 11),
            new Tongue(2, 8, 2.9F, 11), new Tongue(-4.2F, 5, 0, 8), new Tongue(4.2F, 5, 0, 8)};
    private static final Tongue[] ARM = {
            new Tongue(0, -2, 0, 20), new Tongue(0, 3, 2.4F, 13), new Tongue(0, 3, -2.4F, 11), new Tongue(2.4F, 6, 0, 11),
            new Tongue(-2.4F, 6, 0, 11), new Tongue(0, 12, 0, 13)};
    private static final Tongue[] LEG = {
            new Tongue(0, 1, 2.4F, 12), new Tongue(0, 5, -2.4F, 10), new Tongue(2.2F, 7, 0, 10), new Tongue(-2.2F, 7, 0, 10),
            new Tongue(0, 12, 0, 11)};

    private final PlayerModel<AbstractClientPlayer> glow;

    public JimFlameLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                         PlayerModel<AbstractClientPlayer> glow) {
        super(parent);
        this.glow = glow;
    }

    /** Whether this player's body burns now. */
    static boolean burning(PowerData data, AbstractClientPlayer player) {
        if (data.is(Live.JIM_FLAMING) || data.is(Live.JIM_NOVA_CHARGING) || data.is(Live.JIM_NOVA_BLAST)) {
            return true;
        }
        // (3.17) Source Code Vision burns too while he flies: G on and in the air (his switch: vision.source_code_flight_fire)
        return data.power() == com.kolodkingal.enhanced.power.Power.VISION && "source_code".equals(data.variant()) && !data.disguise()
                && data.flying() && !player.onGround()
                && (!com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded() || com.kolodkingal.enhanced.EnhancedConfig.SOURCE_CODE_FLIGHT_FIRE.get());
    }

    private void pose(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                      float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.setAllVisible(true);
        // the second layers of the skin are not part of the fire
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        if (!burning(data, player) || player.isInvisible()) {
            return;
        }
        float t = player.tickCount + partialTicks;
        boolean nova = data.is(Live.JIM_NOVA_CHARGING) || data.is(Live.JIM_NOVA_BLAST);
        // 1. his own skin stays as it is (white stays white, red stays red); a faint warm light lies over it, mostly at the rim
        pose(glow, player, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        float pulse = 0.5F + 0.5F * Mth.sin(t * 0.21F);
        int glowAlpha = Math.round(255.0F * (nova ? 0.22F : 0.025F + 0.02F * pulse));
        glow.renderToBuffer(poseStack, buffer.getBuffer(RenderType.energySwirl(GLOW, (t * 0.003F) % 1.0F, (-t * 0.012F) % 1.0F)),
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glowAlpha << 24 | 0xFF9A30);
        PlayerModel<AbstractClientPlayer> body = getParentModel();
        // 2. the sheath: sheets of flame standing round every part, rising, in two layers at their own speeds
        sheets(poseStack, buffer, body.head, -4, 4, -8, 0, -4, 4, 6.0F, t, 0, nova);
        sheets(poseStack, buffer, body.body, -4, 4, 0, 12, -2, 2, 3.0F, t, 1, nova);
        sheets(poseStack, buffer, body.rightArm, -3, 1, -2, 10, -2, 2, 4.0F, t, 2, nova);
        sheets(poseStack, buffer, body.leftArm, -1, 3, -2, 10, -2, 2, 4.0F, t, 3, nova);
        sheets(poseStack, buffer, body.rightLeg, -2, 2, 0, 12, -2, 2, 2.0F, t, 4, nova);
        sheets(poseStack, buffer, body.leftLeg, -2, 2, 0, 12, -2, 2, 2.0F, t, 5, nova);
        // 3. tongues of flame standing on the parts
        Vec3 velocity = player.getDeltaMovement();
        Vector3f lean = new Vector3f((float) -velocity.x, (float) -velocity.y * 0.5F, (float) -velocity.z).mul(2.6F);
        float speed = Math.min(1.6F, (float) velocity.length() * 3.0F);
        if (lean.length() > 1.5F) {
            lean.normalize(1.5F);
        }
        tongues(poseStack, buffer, body.head, HEAD, t, lean, speed, 0, nova);
        tongues(poseStack, buffer, body.body, BODY, t, lean, speed, 6, nova);
        tongues(poseStack, buffer, body.rightArm, ARM, t, lean, speed, 12, nova);
        tongues(poseStack, buffer, body.leftArm, ARM, t, lean, speed, 18, nova);
        tongues(poseStack, buffer, body.rightLeg, LEG, t, lean, speed, 24, nova);
        tongues(poseStack, buffer, body.leftLeg, LEG, t, lean, speed, 28, nova);
    }

    /**
     * Sheets of flame round one part (its box in pixels of the part's own frame): four upright planes a little way out of its faces,
     * from the part's foot to a little above its top ({@code rise}), textured with the rising flame of {@link #SHELL} scrolling
     * upward; two layers, the outer one wider, slower and fainter. Drawn unshaded and see-through ({@link EnhancedRenderTypes#webDecal}: alpha-blended, no lighting,
     * no culling): glowing fire from every angle that stays orange against any sky, never a solid or dark shape.
     */
    private static void sheets(PoseStack poseStack, MultiBufferSource buffer, ModelPart part, float x0, float x1, float y0, float y1,
                               float z0, float z1, float rise, float t, int seed, boolean nova) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        Matrix4f m = poseStack.last().pose();
        for (int layer = 0; layer < 2; layer++) {
            float gap = layer == 0 ? 0.55F : 1.1F;
            float top = y0 - rise * (layer == 0 ? 1.0F : 1.6F);
            float speed = layer == 0 ? 0.034F : 0.021F;
            int frame = (Mth.floor(t * (layer == 0 ? 0.7F : 0.5F)) + seed * 3 + layer * 4) % SHELL_FRAMES;
            VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.webDecal(tex("shell_" + frame)));
            int alpha = Math.round((layer == 0 ? 0x2C : 0x1C) * (nova ? 1.4F : 1.0F));
            int color = Math.min(255, alpha) << 24 | 0xFFFFFF;
            float v0 = (t * speed + seed * 0.37F) % 1.0F;
            float[][] sides = {
                    {x0 - gap, z0 - gap, x1 + gap, z0 - gap}, {x1 + gap, z0 - gap, x1 + gap, z1 + gap},
                    {x1 + gap, z1 + gap, x0 - gap, z1 + gap}, {x0 - gap, z1 + gap, x0 - gap, z0 - gap}};
            for (int s = 0; s < 4; s++) {
                float[] e = sides[s];
                float u0 = s * 0.25F + layer * 0.5F;
                float u1 = u0 + 0.5F;
                float bottom = y1;
                put(vc, m, e[0], top, e[1], u0, v0, color);
                put(vc, m, e[2], top, e[3], u1, v0, color);
                put(vc, m, e[2], bottom, e[3], u1, v0 + 0.55F, color);
                put(vc, m, e[0], bottom, e[1], u0, v0 + 0.55F, color);
            }
        }
        poseStack.popPose();
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int color) {
        vc.addVertex(m, x / 16.0F, y / 16.0F, z / 16.0F).setUv(u, v).setColor(color);
    }

    private static void tongues(PoseStack poseStack, MultiBufferSource buffer, ModelPart part, Tongue[] list, float t, Vector3f lean,
                                float speed, int seed, boolean nova) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        Matrix4f m = poseStack.last().pose();
        // the world's up and the lean, in this part's own frame: the tongues turn and tilt with the body
        Matrix4f inverse = new Matrix4f(m).invert();
        for (int i = 0; i < list.length; i++) {
            Tongue tongue = list[i];
            float phase = t * 0.35F + (seed + i) * 1.7F;
            float sway = 0.22F * Mth.sin(phase) * (1.0F - 0.5F * speed);
            Vector3f world = new Vector3f(lean.x + sway, 1.0F, lean.z + 0.22F * Mth.cos(phase * 0.8F)).add(0, lean.y, 0).normalize();
            Vector3f dir = inverse.transformDirection(world, new Vector3f()).normalize();
            float flicker = 0.78F + 0.22F * Mth.sin(phase * 1.9F + 1.0F);
            float height = tongue.height() * flicker * (1.0F + 0.5F * speed + (nova ? 0.35F : 0.0F)) / 16.0F;
            Vector3f u = Math.abs(dir.y) < 0.9F ? new Vector3f(dir).cross(0, 1, 0) : new Vector3f(dir).cross(1, 0, 0);
            u.normalize();
            Vector3f v = new Vector3f(dir).cross(u).normalize();
            float half = Math.max(0.06F, height * 0.36F);
            int frame = (Mth.floor(t * 0.9F) + seed + i * 3) % TONGUE_FRAMES;
            VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.webDecal(tex("tongue_" + frame)));
            float bx = tongue.x() / 16.0F;
            float by = tongue.y() / 16.0F;
            float bz = tongue.z() / 16.0F;
            quad(m, vc, bx, by, bz, dir, u, half, height);
            quad(m, vc, bx, by, bz, dir, v, half, height);
        }
        poseStack.popPose();
    }

    /** One tongue's quad: its base at (bx, by, bz), up along {@code dir}, {@code half} wide each side along {@code side}. */
    private static void quad(Matrix4f m, VertexConsumer vc, float bx, float by, float bz, Vector3f dir, Vector3f side, float half,
                             float height) {
        float tx = bx + dir.x * height;
        float ty = by + dir.y * height;
        float tz = bz + dir.z * height;
        float[][] corners = {
                {bx - side.x * half, by - side.y * half, bz - side.z * half, 0, 1},
                {bx + side.x * half, by + side.y * half, bz + side.z * half, 1, 1},
                {tx + side.x * half, ty + side.y * half, tz + side.z * half, 1, 0},
                {tx - side.x * half, ty - side.y * half, tz - side.z * half, 0, 0}};
        for (float[] c : corners) {
            vc.addVertex(m, c[0], c[1], c[2]).setColor(0xD8FFFFFF).setUv(c[3], c[4]);
        }
    }
}

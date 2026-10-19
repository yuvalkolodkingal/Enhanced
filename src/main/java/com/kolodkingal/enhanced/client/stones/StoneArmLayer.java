package com.kolodkingal.enhanced.client.stones;

import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * What the stones draw on the arm that holds them (3.6, lane L2), in third person: the <b>Power Stone's veins</b>
 * ({@link Live#POWER_VEINS}: violet veins crawl up from the hand while the Purple Beam pours, as on Peter Quill's arm on
 * Xandar) and the <b>Time Stone's mandala</b> ({@link Live#TIME_SLOWING}: two rings of green light turn round the
 * wrist, one each way, with the ticks of a clock on them, while Slow Time runs). Both are light (additive, no
 * texture), drawn on the arm's own model part, so they follow every pose; they grow and turn with the frame's time.
 */
public class StoneArmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** How long the veins take to crawl from the hand to the shoulder (ticks). */
    private static final float VEINS_RISE_TICKS = 30.0F;
    private static final int VEIN = StoneKind.POWER.color();
    private static final int VEIN_CORE = StoneKind.POWER.core();
    private static final int TIME = StoneKind.TIME.color();
    private static final int TIME_CORE = 0xC8FFD8;
    private static final float PX = 1.0F / 16.0F;

    /** Player id -> the game time the veins began (they crawl up from then). */
    private static final java.util.Map<Integer, Float> VEINS_SINCE = new java.util.HashMap<>();

    public StoneArmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        PowerData data = Powers.get(player);
        boolean veins = data.is(Live.POWER_VEINS);
        boolean mandala = data.is(Live.TIME_SLOWING);
        if (!veins) {
            VEINS_SINCE.remove(player.getId());
        }
        if (!veins && !mandala || player.isInvisible()) {
            return;
        }
        HumanoidArm side = stoneArm(player);
        ModelPart arm = side == HumanoidArm.RIGHT ? getParentModel().rightArm : getParentModel().leftArm;
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        Matrix4f m = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        // The arm's box in its own space (pixels): x from {@code minX} to {@code maxX}, y -2 (shoulder) to 10 (hand).
        float width = slim ? 3.0F : 4.0F;
        float minX = side == HumanoidArm.RIGHT ? 1.0F - width : -1.0F;
        float maxX = minX + width;
        if (veins) {
            float since = VEINS_SINCE.computeIfAbsent(player.getId(), id -> ageInTicks);
            float grown = Mth.clamp((ageInTicks - since) / VEINS_RISE_TICKS, 0.15F, 1.0F);
            veins(vc, m, minX, maxX, grown, ageInTicks, player.getId());
        }
        if (mandala) {
            float cx = (minX + maxX) * 0.5F;
            rings(vc, m, cx, 8.6F, width * 0.5F + 1.6F, ageInTicks * 0.08F);
            rings(vc, m, cx, 6.4F, width * 0.5F + 2.4F, -ageInTicks * 0.05F);
        }
        poseStack.popPose();
    }

    /** The arm that holds the stone (bare, in a Gauntlet or in a vessel): the off arm if only it holds one. */
    static HumanoidArm stoneArm(AbstractClientPlayer player) {
        boolean main = holdsStone(player.getMainHandItem());
        boolean off = holdsStone(player.getOffhandItem());
        return off && !main ? player.getMainArm().getOpposite() : player.getMainArm();
    }

    private static boolean holdsStone(ItemStack stack) {
        if (stack.is(ModItems.INFINITY_GAUNTLET.get()) || stack.is(ModItems.LOKIS_SCEPTER.get())
                || stack.is(ModItems.TESSERACT.get()) || stack.is(ModItems.EYE_OF_AGAMOTTO.get())) {
            return true;
        }
        for (StoneKind kind : StoneKind.values()) {
            if (stack.is(ModItems.stone(kind))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The veins: on each of the arm's four faces two branching lines rise from the hand to {@code grown} of the way
     * up, a deeper violet with a pale core, flickering a little as the energy runs in them.
     */
    private static void veins(VertexConsumer vc, Matrix4f m, float minX, float maxX, float grown, float time, int seed) {
        float top = 10.0F - 12.0F * grown;
        float flicker = 0.75F + 0.25F * Mth.sin(time * 0.9F + seed);
        for (int face = 0; face < 4; face++) {
            for (int line = 0; line < 2; line++) {
                float across = line == 0 ? 0.3F : 0.7F;
                float prevY = 10.0F;
                float prevU = across;
                for (float y = 9.0F; y >= top - 0.01F; y -= 1.0F) {
                    float u = Mth.clamp(across + 0.18F * Mth.sin(y * 1.7F + face * 2.1F + line * 3.3F + seed),
                            0.08F, 0.92F);
                    float fade = Mth.clamp((y - top) / 3.0F + 0.35F, 0.0F, 1.0F) * flicker;
                    segment(vc, m, face, minX, maxX, prevU, prevY, u, y, 0.45F,
                            EnergyGeometry.argb(VEIN, 0.75F * fade));
                    segment(vc, m, face, minX, maxX, prevU, prevY, u, y, 0.18F,
                            EnergyGeometry.argb(VEIN_CORE, 0.85F * fade));
                    prevY = y;
                    prevU = u;
                }
            }
        }
    }

    /**
     * A short strip of vein on one face of the arm's box, from ({@code u0}, {@code y0}) to ({@code u1}, {@code y1});
     * {@code u} runs 0..1 across the face, {@code y} in pixels down the arm.
     */
    private static void segment(VertexConsumer vc, Matrix4f m, int face, float minX, float maxX, float u0, float y0,
                                float u1, float y1, float widthPx, int argb) {
        float half = widthPx * 0.5F;
        float lift = 0.06F;
        float[][] p = new float[4][];
        for (int i = 0; i < 4; i++) {
            float u = i < 2 ? u0 : u1;
            float y = i < 2 ? y0 : y1;
            float off = i == 0 || i == 3 ? -half : half;
            p[i] = switch (face) {
                // front (z -2), back (z 2): u runs along x
                case 0 -> new float[]{Mth.lerp(u, minX, maxX) + off, y, -2.0F - lift};
                case 1 -> new float[]{Mth.lerp(u, minX, maxX) + off, y, 2.0F + lift};
                // the two sides (x min, x max): u runs along z
                case 2 -> new float[]{minX - lift, y, Mth.lerp(u, -2.0F, 2.0F) + off};
                default -> new float[]{maxX + lift, y, Mth.lerp(u, -2.0F, 2.0F) + off};
            };
        }
        for (float[] v : p) {
            vc.addVertex(m, v[0] * PX, v[1] * PX, v[2] * PX).setColor(argb);
        }
    }

    /**
     * A ring of the Time Stone round the arm at height {@code y} (pixels), radius {@code r} (pixels), turned by {@code
     * angle}: a thin band with a brighter inner edge and twelve clock ticks standing out of it.
     */
    private static void rings(VertexConsumer vc, Matrix4f m, float cx, float y, float r, float angle) {
        int segments = 24;
        float inner = r - 0.35F;
        int band = EnergyGeometry.argb(TIME, 0.55F);
        int edge = EnergyGeometry.argb(TIME_CORE, 0.7F);
        for (int i = 0; i < segments; i++) {
            float a0 = angle + Mth.TWO_PI * i / segments;
            float a1 = angle + Mth.TWO_PI * (i + 1) / segments;
            flat(vc, m, cx, y, inner, r, a0, a1, band);
            flat(vc, m, cx, y, inner - 0.15F, inner, a0, a1, edge);
        }
        for (int i = 0; i < 12; i++) {
            float a = angle + Mth.TWO_PI * i / 12;
            float w = i % 3 == 0 ? 0.09F : 0.05F;
            flat(vc, m, cx, y, r, r + (i % 3 == 0 ? 1.1F : 0.6F), a - w, a + w, edge);
        }
    }

    /** A flat piece of an annulus round the arm (centre {@code cx}, 0) at height {@code y}, radii and angles given. */
    private static void flat(VertexConsumer vc, Matrix4f m, float cx, float y, float r0, float r1, float a0, float a1,
                             int argb) {
        vc.addVertex(m, (cx + Mth.cos(a0) * r0) * PX, y * PX, Mth.sin(a0) * r0 * PX).setColor(argb);
        vc.addVertex(m, (cx + Mth.cos(a0) * r1) * PX, y * PX, Mth.sin(a0) * r1 * PX).setColor(argb);
        vc.addVertex(m, (cx + Mth.cos(a1) * r1) * PX, y * PX, Mth.sin(a1) * r1 * PX).setColor(argb);
        vc.addVertex(m, (cx + Mth.cos(a1) * r0) * PX, y * PX, Mth.sin(a1) * r0 * PX).setColor(argb);
    }
}

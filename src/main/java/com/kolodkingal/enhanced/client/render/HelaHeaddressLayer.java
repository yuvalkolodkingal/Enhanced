package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hela's headdress (3.5, PLAN 7.3; redrawn in 3.5.1, it looked like a pineapple): her horned crown of the films, a fan of
 * long antler tines growing from the ridge of her cowl round the back of her head, spreading out like a dark halo and
 * sweeping back, each of three tapering segments that bend further back and out, the inner pairs with a short prong;
 * glossy black with a dark green sheen. One model of its own on the head ({@link #createLayer}, drawn by {@link #draw}
 * with the head's every turn), used by the hero (this layer: while her form is on, and in the look {@code goddess}
 * always), by the boss's renderer and by her shadows'. It grows from nothing in {@link #GROW_TICKS} ticks when the form
 * comes on and shrinks back when it goes, eased with the partial tick, from a time kept per player on the client.
 */
public class HelaHeaddressLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("hela"), "headdress");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/suit/hela_headdress.png");
    /** How long the headdress takes to grow (and to fade). */
    public static final float GROW_TICKS = 20.0F;
    /** Where the tines grow from, in the pixels of the head (its top at y = -8, its back at z = 4). */
    public static final float BASE_Y = -7.6F;
    public static final float BASE_Z = 1.0F;

    /** Player -> the time (client ticks) the form last came on or went, and whether it is on. */
    private static final Map<UUID, float[]> GROWTH = new HashMap<>();

    private final ModelPart headdress;

    public HelaHeaddressLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart headdress) {
        super(parent);
        this.headdress = headdress;
    }

    /**
     * The tines of one side, from the middle out: roll (degrees, out from the top round the back of the head), pitch
     * (degrees back), the three segments' lengths, the base thickness, and 1 for a side prong. Mirrored on the other side.
     */
    static final float[][] TINES = {
            {9.0F, 32.0F, 6.4F, 5.8F, 4.2F, 1.7F, 0},
            {28.0F, 30.0F, 6.2F, 5.4F, 3.9F, 1.6F, 1},
            {49.0F, 26.0F, 5.8F, 5.0F, 3.6F, 1.5F, 1},
            {70.0F, 20.0F, 5.0F, 4.2F, 3.0F, 1.3F, 0},
    };
    /** How much further back the second and third segments bend, and how they turn up toward the middle (degrees). */
    static final float[] BEND_PITCH = {12.0F, 14.0F};
    static final float[] BEND_ROLL = {-3.0F, -4.0F};
    /** The arc the tines grow from: its radius round the head's middle, behind it (pixels). */
    static final float ARC_R = 3.7F;
    static final float ARC_Z = 1.2F;
    static final float PRONG = 2.6F;

    /** The cowl's ridge round the top and back of the head, its keel, and four tines a side. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition crown = root.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-4.45F, -8.55F, -0.6F, 8.9F, 1.3F, 5.2F)
                .texOffs(0, 27).addBox(-1.0F, -9.3F, 0.0F, 2.0F, 1.0F, 3.6F), PartPose.ZERO);
        float rad = Mth.DEG_TO_RAD;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < TINES.length; i++) {
                float[] t = TINES[i];
                String name = (side < 0 ? "r" : "l") + i;
                float roll = t[0] * rad;
                float x = side * ARC_R * Mth.sin(roll);
                float y = -8.0F + 2.9F * (1.0F - Mth.cos(roll));
                float z = ARC_Z + 0.6F * (1.0F - Mth.cos(roll));
                float l1 = t[2];
                float l2 = t[3];
                float l3 = t[4];
                float w1 = t[5];
                float w2 = w1 * 0.72F;
                float w3 = w1 * 0.45F;
                PartDefinition seg1 = crown.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-w1 / 2, -l1, -w1 / 2, w1, l1, w1), PartPose.offsetAndRotation(x, y, z,
                        -t[1] * rad, 0.0F, side * roll));
                PartDefinition seg2 = seg1.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(8, 0)
                        .addBox(-w2 / 2, -l2, -w2 / 2, w2, l2, w2), PartPose.offsetAndRotation(0.0F, -l1 + 0.25F, 0.0F,
                        -BEND_PITCH[0] * rad, 0.0F, side * BEND_ROLL[0] * rad));
                seg2.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(16, 0)
                        .addBox(-w3 / 2, -l3, -w3 / 2, w3, l3, w3), PartPose.offsetAndRotation(0.0F, -l2 + 0.2F, 0.0F,
                        -BEND_PITCH[1] * rad, 0.0F, side * BEND_ROLL[1] * rad));
                if (t[6] > 0) {
                    // An antler's prong: from high on the first segment, out and a little forward.
                    seg1.addOrReplaceChild("prong", CubeListBuilder.create().texOffs(24, 0)
                            .addBox(-w3 / 2, -PRONG, -w3 / 2, w3, PRONG, w3), PartPose.offsetAndRotation(0.0F, -l1 * 0.75F, 0.0F,
                            20.0F * rad, 0.0F, side * 42.0F * rad));
                }
            }
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    /**
     * The headdress on a head at {@code grow} (0 nothing, 1 grown): scaled up from its root at the back of the crown.
     * {@code argb} tints it (white for its own colours; the alpha fades it, with a translucent buffer).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, ModelPart head, ModelPart headdress,
                            float grow, int argb) {
        if (grow <= 0.001F) {
            return;
        }
        float g = AnimMath.easeOutBack(Mth.clamp(grow, 0.0F, 1.0F));
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, BASE_Y / 16.0F, BASE_Z / 16.0F);
        poseStack.scale(g, g, g);
        poseStack.translate(0.0F, -BASE_Y / 16.0F, -BASE_Z / 16.0F);
        RenderType type = (argb >>> 24) < 0xFF ? RenderType.entityTranslucent(TEXTURE) : RenderType.entityCutoutNoCull(TEXTURE);
        headdress.render(poseStack, buffer.getBuffer(type), light, overlay, argb);
        poseStack.popPose();
    }

    /** Whether this player wears the headdress: Hela (or a custom hero with her form) with the form on, seen, no helmet. */
    static boolean wears(AbstractClientPlayer player, PowerData data) {
        return data.has(HeroAbility.HELA_GODDESS) && data.toggle(Toggle.HELA_FORM) && !player.isInvisible()
                && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
    }

    /**
     * How grown the player's headdress is now (0 to 1): it rises over {@link #GROW_TICKS} from the tick the form came on
     * and falls as long after it went.
     */
    static float growth(AbstractClientPlayer player, boolean on, float age) {
        float[] g = GROWTH.get(player.getUUID());
        if (g == null) {
            g = new float[]{age - GROW_TICKS, on ? 1.0F : 0.0F};
            GROWTH.put(player.getUUID(), g);
        }
        if ((g[1] > 0.5F) != on) {
            // It turned: start from where it is now, so a quick on-off does not jump.
            float now = current(g, age);
            g[1] = on ? 1.0F : 0.0F;
            g[0] = on ? age - now * GROW_TICKS : age - (1.0F - now) * GROW_TICKS;
        }
        return current(g, age);
    }

    private static float current(float[] g, float age) {
        float k = Mth.clamp((age - g[0]) / GROW_TICKS, 0.0F, 1.0F);
        return g[1] > 0.5F ? k : 1.0F - k;
    }

    /** Forgets the growth of everyone (leaving the world). */
    public static void clear() {
        GROWTH.clear();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        if (!data.acts(Power.HELA) && !data.has(HeroAbility.HELA_GODDESS)) {
            return;
        }
        boolean on = wears(player, data);
        float grow = growth(player, on, ageInTicks);
        if (grow <= 0.001F || player.isInvisible()) {
            return;
        }
        draw(poseStack, buffer, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), getParentModel().head, headdress, grow, 0xFFFFFFFF);
    }

    /** The look of a hero Hela (for the arrival look: the cowl's hair is swept back, there is no headdress until the form). */
    static boolean arrival(PowerData data) {
        return data.power() == Power.HELA && "arrival".equals(Variants.current(data).id());
    }
}

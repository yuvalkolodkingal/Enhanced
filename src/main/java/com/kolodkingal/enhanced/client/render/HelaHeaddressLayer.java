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
 * Hela's headdress (3.5, PLAN 7.3): eight antler tines of shiny dark green that grow out of the back of her cowl and sweep
 * back and up, the two in the middle tallest, each side fanning out lower, every tine three tapering segments curling
 * back. A model of its own on the head ({@link #createLayer}, drawn by {@link #draw} with the head's every turn), used by
 * the hero (this layer: while her form is on, and in the look {@code goddess} always) and by the boss's renderer. It grows
 * from nothing in {@link #GROW_TICKS} ticks when the form comes on and shrinks back when it goes, eased with the partial
 * tick, from a time kept per player on the client.
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

    /** The cowl band round the back of the head, and four tines a side. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition crown = root.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-4.6F, -8.6F, -1.2F, 9.2F, 2.0F, 5.8F)
                .texOffs(0, 28).addBox(-1.0F, -9.4F, -0.6F, 2.0F, 1.0F, 4.0F), PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 4; i++) {
                String name = (side < 0 ? "r" : "l") + i;
                float x = side * (0.8F + 0.95F * i);
                float z = BASE_Z + 0.55F * i;
                float l1 = 6.5F - 1.1F * i;
                float l2 = 5.5F - 0.9F * i;
                float l3 = 3.2F - 0.4F * i;
                PartDefinition seg1 = crown.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.8F, -l1, -0.8F, 1.6F, l1, 1.6F), PartPose.offsetAndRotation(x, BASE_Y, z,
                        -0.22F - 0.13F * i, 0.0F, -side * (0.10F + 0.32F * i)));
                PartDefinition seg2 = seg1.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(8, 0)
                        .addBox(-0.55F, -l2, -0.55F, 1.1F, l2, 1.1F), PartPose.offsetAndRotation(0.0F, -l1 + 0.3F, 0.0F,
                        -0.24F, 0.0F, -side * 0.14F));
                seg2.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(16, 0)
                        .addBox(-0.3F, -l3, -0.3F, 0.6F, l3, 0.6F), PartPose.offsetAndRotation(0.0F, -l2 + 0.2F, 0.0F,
                        -0.3F, 0.0F, -side * 0.16F));
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

package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.CyclopsGlasses;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The ruby-quartz glasses on the head of whoever wears them (3.3): a band round the head at the eyes, a little outside the
 * hat layer of the skin, that follows the head. On Cyclops it is the visor of his look (the film's dark wrap-around visor,
 * or the comics' yellow-framed one); on Cyclops in his own skin, or anyone else, the red sunglasses Scott wears out of
 * uniform. The lens glows: a little always, brightly while he holds the Optic Blast (with the weight of its stance, so it
 * swells and fades with his hand at the visor and never blinks). The glasses' armour texture is empty: this layer is how
 * they are seen.
 */
public class CyclopsVisorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("cyclops_visor"), "main");
    /** The band: 8 x 2 x 8 round the head at the eyes, 0.6 out (the hat layer is 0.5 out). Its texture is drawn at twice the density. */
    public static final float INFLATE = 0.6F;

    public static final ResourceLocation VISOR_APOCALYPSE = texture("cyclops_visor_apocalypse");
    public static final ResourceLocation VISOR_CLASSIC = texture("cyclops_visor_classic");
    public static final ResourceLocation GLASSES = texture("ruby_quartz_glasses_worn");
    private static final RenderType GLOW_APOCALYPSE = RenderType.eyes(texture("cyclops_visor_apocalypse_glow"));
    private static final RenderType GLOW_CLASSIC = RenderType.eyes(texture("cyclops_visor_classic_glow"));
    private static final RenderType GLOW_GLASSES = RenderType.eyes(texture("ruby_quartz_glasses_worn_glow"));
    /** How brightly the lens glows at rest, and at the height of the Optic Blast. */
    private static final float IDLE_GLOW = 0.16F;

    private final ModelPart visor;

    public CyclopsVisorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root) {
        super(parent);
        this.visor = root.getChild("visor");
    }

    private static ResourceLocation texture(String name) {
        return Enhanced.id("textures/entity/" + name + ".png");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("visor", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -4.5F, -4.0F, 8, 2, 8, new CubeDeformation(INFLATE)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }

    /** The texture of the glasses on this wearer: the visor of Cyclops's look, else the sunglasses. */
    public static ResourceLocation textureFor(PowerData data) {
        if (data.power() != Power.CYCLOPS) {
            return GLASSES;
        }
        return switch (Variants.current(data).id()) {
            case "apocalypse" -> VISOR_APOCALYPSE;
            case "classic" -> VISOR_CLASSIC;
            default -> GLASSES;
        };
    }

    private static RenderType glowFor(ResourceLocation texture) {
        return texture == VISOR_APOCALYPSE ? GLOW_APOCALYPSE : texture == VISOR_CLASSIC ? GLOW_CLASSIC : GLOW_GLASSES;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !CyclopsGlasses.wears(player)) {
            return;
        }
        PowerData data = Powers.get(player);
        ResourceLocation texture = textureFor(data);
        visor.copyFrom(getParentModel().head);
        visor.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, OverlayTexture.NO_OVERLAY);
        if (data.power() != Power.CYCLOPS || data.is(Live.CYCLOPS_UNLEASHED)) {
            return;
        }
        HeroAnimState state = HeroAnimator.peek(player);
        float blast = state == null ? (data.is(Live.BEAMING) ? 1.0F : 0.0F) : state.stance(Stance.CYCLOPS_BLAST);
        float glow = IDLE_GLOW + (1.0F - IDLE_GLOW) * blast * (0.9F + 0.1F * Mth.sin(ageInTicks * 0.9F));
        int g = Mth.clamp((int) (glow * 255.0F), 0, 255);
        visor.render(poseStack, buffer.getBuffer(glowFor(texture)), 0xF00000, OverlayTexture.NO_OVERLAY, 0xFF000000 | g << 16 | g << 8 | g);
    }
}

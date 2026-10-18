package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Spider-Man's eyes (DESIGN section 14): while the Black Eyes are on, the lenses of his suit are drawn black with a
 * dark red glint; while Instant Kill is on, in the Iron Spider look, they close to two red slits that glow. Each is a
 * head-only overlay, a texture the size of the skin of which only the pixels of the lenses are painted (one for each
 * look, because the lenses sit differently on each suit), drawn over the head with the model's own pose.
 *
 * <p>The black is opaque, drawn the way the skin is; the slits are drawn twice, first black and then again, glowing,
 * with the eyes' render type.
 */
public class BlackEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The black lenses of each look: {@code spider_eyes_black_<name>.png}. */
    private static final ResourceLocation BLACK_HOMEMADE = texture("black_homemade");
    private static final ResourceLocation BLACK_IRON = texture("black_iron");
    private static final ResourceLocation BLACK_BND = texture("black_bnd");
    private static final ResourceLocation BLACK_OWN = texture("black_own");
    /** The red slits of Instant Kill (the Iron Spider only). */
    private static final ResourceLocation KILL_IRON = texture("kill_iron");

    public BlackEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static ResourceLocation texture(String name) {
        return Enhanced.id("textures/entity/spider_eyes_" + name + ".png");
    }

    /** The black lenses a look has when the Black Eyes are on (a look added later is a texture and a line here). */
    public static ResourceLocation black(String look) {
        return switch (look) {
            case "homemade" -> BLACK_HOMEMADE;
            case "iron_spider" -> BLACK_IRON;
            case "brand_new_day" -> BLACK_BND;
            default -> BLACK_OWN;
        };
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        // Spider-Man's own lenses, painted for his looks. (3.4) A custom hero with the Black Eyes or the legs' Instant Kill
        // gets them on the generic eye mask instead: CustomLooksLayer draws them, by data.has(SPIDER_BLACK_EYES/LEGS).
        if (data.power() != Power.SPIDER_MAN || player.isInvisible()) {
            return;
        }
        String look = Variants.current(data).id();
        boolean kill = data.is(Live.INSTANT_KILL) && "iron_spider".equals(look);
        if (kill) {
            // The lenses close to two slits: black, and over it the red, glowing.
            getParentModel().head.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(KILL_IRON)), packedLight, OverlayTexture.NO_OVERLAY);
            getParentModel().head.render(poseStack, buffer.getBuffer(RenderType.eyes(KILL_IRON)), 0xF00000, OverlayTexture.NO_OVERLAY);
        } else if (data.is(Live.BLACK_EYES)) {
            getParentModel().head.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(black(look))), packedLight, OverlayTexture.NO_OVERLAY);
        }
    }
}

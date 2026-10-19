package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.GauntletLayer;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The renderer of a player who is the Hulk (3.6): the vanilla player renderer (its layers: what he holds, arrows, the
 * parrot; his armour is not drawn while he is the Hulk, {@code HeroArmour}), with the player model swapped for {@link
 * HulkModel} and the Hulk's 128 x 128 skin of his look. {@link HulkRenderEvents} draws him with it instead of the
 * player renderer (unless the client option {@code hulkModel} is off: then the player model wears the fallback skin of
 * his look). Made with the other renderers at every resource reload ({@code ClientSetup}), with the mod's layers that
 * make sense on him added there. The scale comes from the attribute, as for any creature.
 */
public class HulkPlayerRenderer extends PlayerRenderer {
    public static final ResourceLocation RAGNAROK = Enhanced.id("textures/entity/hulk/ragnarok.png");
    public static final ResourceLocation BRAND_NEW_DAY = Enhanced.id("textures/entity/hulk/brand_new_day.png");

    /** The one made at the last resource reload, or null before it. */
    @Nullable
    private static HulkPlayerRenderer instance;

    public HulkPlayerRenderer(EntityRendererProvider.Context context) {
        super(context, false);
        this.model = new HulkModel(context.bakeLayer(HulkModel.LAYER));
        this.shadowRadius = 0.75F;
    }

    public static void set(HulkPlayerRenderer renderer) {
        instance = renderer;
    }

    @Nullable
    public static HulkPlayerRenderer get() {
        return instance;
    }

    /** The Hulk's skin of this look. */
    public static ResourceLocation skin(String look) {
        return HulkAbilities.BRAND_NEW_DAY.equals(look) ? BRAND_NEW_DAY : RAGNAROK;
    }

    /** Whether this player is drawn as the Hulk's body right now: the hero in a Hulk look, the option on. */
    public static boolean drawsAsHulk(Player player) {
        if (instance == null || !EnhancedClientConfig.hulkModel()) {
            return false;
        }
        PowerData data = Powers.get(player);
        return data.power() == Power.HULK && HulkAbilities.isHulkLook(HulkAbilities.look(data));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
        return skin(HulkAbilities.look(Powers.get(player)));
    }

    /**
     * His arm in first person: the Hulk's huge forearm, in his skin (the vanilla hand would wear the player's skin), a
     * little further in so it stays in view. The Gauntlet of a Hulk who wields one is drawn on it as on a player's.
     */
    public void renderFirstPersonArm(PoseStack poseStack, MultiBufferSource buffer, int light,
            AbstractClientPlayer player, HumanoidArm side) {
        HulkModel hulk = (HulkModel) getModel();
        hulk.attackTime = 0.0F;
        hulk.crouching = false;
        hulk.swimAmount = 0.0F;
        hulk.setAllVisible(true);
        hulk.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        boolean right = side == HumanoidArm.RIGHT;
        ModelPart arm = right ? hulk.rightArm : hulk.leftArm;
        ModelPart sleeve = right ? hulk.rightSleeve : hulk.leftSleeve;
        poseStack.pushPose();
        // (The shoulder is 3.5 pixels further out than a player's and the arm a third longer: back into view.)
        poseStack.translate((right ? 1.0F : -1.0F) * 3.0F / 16.0F, -0.14F, 0.0F);
        arm.xRot = 0.0F;
        sleeve.xRot = 0.0F;
        ResourceLocation texture = getTextureLocation(player);
        arm.render(poseStack, buffer.getBuffer(RenderType.entitySolid(texture)), light, OverlayTexture.NO_OVERLAY);
        sleeve.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(texture)), light,
                OverlayTexture.NO_OVERLAY);
        HulkLayers.firstPersonExtras(poseStack, buffer, light, player, arm, right);
        if (right) {
            GauntletLayer.renderFirstPerson(poseStack, buffer, light, player, arm, this);
        }
        poseStack.popPose();
    }
}

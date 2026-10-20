package com.kolodkingal.enhanced.client.redhulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.hulk.HulkModel;
import com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.redhulk.Anger;
import com.kolodkingal.enhanced.redhulk.RedHulkAbilities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Red Hulk's heat (3.8, PLAN 4.3): emissive cracks over his skin that glow with his anger, from nothing when he is
 * calm to white-hot ember lines at 100, pulsing from 90 (his anger is synced to everyone who sees him, so everyone sees
 * it). On the Hulk's renderer (his own body: a mask on the Hulk's 128 x 128 atlas) and on the player renderers (the
 * look {@code own}, or the {@code hulkModel} option off: a 64 x 64 mask), and on his forearm in first person. His eyes
 * ({@link #eyes}) burn yellow-orange from anger 50 and while he absorbs. The masks are drawn additively (the eyes'
 * render type), so a dim heat is a faint glow and a full one bright. Ross has no heat.
 */
public class HeatLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation HEAT = Enhanced.id("textures/entity/red_hulk/heat.png");
    public static final ResourceLocation HEAT_PLAYER = Enhanced.id("textures/entity/red_hulk/heat_player.png");
    private static final RenderType EYES = RenderType.eyes(Enhanced.id("textures/entity/red_hulk/eyes.png"));
    private static final RenderType EYES_PLAYER =
            RenderType.eyes(Enhanced.id("textures/entity/red_hulk/eyes_player.png"));

    public HeatLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** How bright his heat is (0-1) at this anger, {@code age} the frame's ticks (the pulse near the top). */
    public static float heat(float anger, boolean absorbing, float age) {
        float h = Mth.clamp((anger - 15.0F) / 85.0F, 0.0F, 1.0F);
        h = h * h * (3.0F - 2.0F * h) * 0.9F + (absorbing ? 0.25F : 0.0F);
        if (anger >= 90.0F) {
            h *= 0.82F + 0.18F * Mth.sin(age * 0.6F);
        } else {
            h *= 0.94F + 0.06F * Mth.sin(age * 0.12F);
        }
        return Mth.clamp(h, 0.0F, 1.0F);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        PowerData data = Powers.get(player);
        if (player.isInvisible() || !RedHulkAbilities.hasBody(data)) {
            return;
        }
        float h = heat(Anger.of(player), data.is(Live.RED_HULK_ABSORBING), ageInTicks);
        if (h < 0.02F) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        ResourceLocation mask = model instanceof HulkModel ? HEAT : HEAT_PLAYER;
        int c = (int) (h * 255.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(mask)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(255, c, c, c));
    }

    /** His forearm's heat in first person (the Hulk's forearm, {@code HulkLayers.firstPersonExtras}). */
    public static void firstPerson(PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player,
            ModelPart arm) {
        PowerData data = Powers.get(player);
        if (!RedHulkAbilities.hasBody(data)) {
            return;
        }
        float h = heat(Anger.of(player), data.is(Live.RED_HULK_ABSORBING), player.tickCount);
        if (h < 0.02F) {
            return;
        }
        int c = (int) (h * 255.0F);
        int color = FastColor.ARGB32.color(255, c, c, c);
        arm.render(poseStack, buffer.getBuffer(RenderType.eyes(HEAT)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, color);
    }

    /** His burning eyes (from anger 50, or while he absorbs), on his body's skin or the player model's, or null. */
    @Nullable
    public static RenderType eyes(AbstractClientPlayer player, PowerData data) {
        if (!RedHulkAbilities.hasBody(data)
                || Anger.of(player) < 50.0F && !data.is(Live.RED_HULK_ABSORBING)) {
            return null;
        }
        return HulkPlayerRenderer.drawsAsHulk(player) ? EYES : EYES_PLAYER;
    }
}

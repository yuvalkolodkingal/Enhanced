package com.kolodkingal.enhanced.client.death;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Rio's Death look (3.7.2, Eilon's request), worn only on Death's screen ({@link DeathMeetingScreen}): at home in the
 * cottage she stays the gentle gardener of {@code rio.png}; meeting the dead, she is the Green Witch as Death, as in
 * <i>Agatha All Along</i>: a dark green-black gown with leaves and veins of decay in the cloth, a long hood framing a
 * pale face with dark lips ({@code rio_death.png}), and over the hood a crown of thin dark branches with a few dead
 * leaves ({@link #headgear}: a small model of its own, {@code rio_death_crown.png}). No flower anywhere (3.8.2, Eilon:
 * in the series Death wore none; the crown's pale tips, which read as blossoms, are dead twig ends now, and the pale
 * specks of the gown are moss). The screen's Rio is marked by a persistent tag ({@link #wear}); the Asgardians' renderer
 * asks {@link #wears} for her skin, and draws the crown as her headgear (registered for the role RIO, drawn only on the
 * one who wears the look).
 */
public final class DeathLook {
    /** The tag on the screen's own Rio (never on a Rio of the world). */
    public static final String TAG = "enhanced:death_look";
    public static final ResourceLocation SKIN = Enhanced.id("textures/entity/asgardian/rio_death.png");
    public static final ResourceLocation CROWN = Enhanced.id("textures/entity/asgardian/rio_death_crown.png");

    @Nullable
    private static ModelPart crown;

    /** Dresses {@code rio} (the screen's own, not in any level) in Death's look. */
    public static void wear(AsgardianEntity rio) {
        rio.getPersistentData().putBoolean(TAG, true);
    }

    /** Whether {@code npc} wears Death's look (only the Rio of Death's screen). */
    public static boolean wears(AsgardianEntity npc) {
        return npc.getPersistentData().getBoolean(TAG);
    }

    /** The crown of branches over the hood, drawn in the head's space (as a headgear of {@code AsgardianRenderer}). */
    public static void headgear(AsgardianEntity npc, PlayerModel<AsgardianEntity> model, PoseStack pose,
            MultiBufferSource buffers,
                                int light, float partialTick) {
        if (!wears(npc)) {
            return;
        }
        if (crown == null) {
            crown = crown();
        }
        crown.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CROWN)), light, OverlayTexture.NO_OVERLAY);
    }

    /**
     * The crown, in the head's pixels (the hood's top is at y -8.5, its front at z -4.5): a band of twigs across the
     * brow, then branches rising from the hood: the tallest in the middle of the front, lower and leaning outward
     * toward the sides, short ones round the back; a few leaves on the branches.
     */
    private static ModelPart crown() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 12).addBox(-4.6F, -9.1F, -4.7F, 9.2F, 0.7F,
                0.6F)
                .texOffs(0, 12).addBox(-4.7F, -9.1F, -4.6F, 0.6F, 0.7F, 9.2F)
                .texOffs(0, 12).addBox(4.1F, -9.1F, -4.6F, 0.6F, 0.7F, 9.2F), PartPose.ZERO);
        // {x, z, height, lean (z rotation, outward), tilt (x rotation, back)}
        float[][] branches = {
                {0.0F, -4.3F, 5.5F, 0.0F, -0.12F},
                {-1.8F, -4.3F, 3.6F, 0.22F, -0.1F}, {1.8F, -4.3F, 3.6F, -0.22F, -0.1F},
                {-3.6F, -3.9F, 2.8F, 0.48F, 0.0F}, {3.6F, -3.9F, 2.8F, -0.48F, 0.0F},
                {-4.3F, -1.2F, 3.0F, 0.42F, 0.1F}, {4.3F, -1.2F, 3.0F, -0.42F, 0.1F},
                {-4.2F, 2.0F, 2.2F, 0.38F, 0.25F}, {4.2F, 2.0F, 2.2F, -0.38F, 0.25F},
                {-1.6F, 4.2F, 1.8F, 0.1F, 0.4F}, {1.6F, 4.2F, 1.8F, -0.1F, 0.4F}};
        for (int i = 0; i < branches.length; i++) {
            float[] b = branches[i];
            CubeListBuilder cubes = CubeListBuilder.create().texOffs(4 * (i % 3), 0).addBox(-0.5F, -b[2], -0.5F, 1.0F,
                    b[2], 1.0F);
            if (b[2] >= 3.0F) {
                // a twig off the branch, with a leaf at its end
                float side = b[3] > 0.0F ? -1.0F : 1.0F;
                cubes.texOffs(0, 0).addBox(side > 0 ? 0.5F : -1.5F, -b[2] * 0.6F, -0.4F, 1.0F, 0.8F, 0.8F)
                        .texOffs(8, 8).addBox(side > 0 ? 1.3F : -2.3F, -b[2] * 0.6F - 1.0F, -0.5F, 1.0F, 1.0F, 1.0F);
            }
            root.addOrReplaceChild("branch_" + i, cubes, PartPose.offsetAndRotation(b[0], -8.5F, b[1], b[4], 0.0F,
                    b[3]));
        }
        return LayerDefinition.create(mesh, 32, 16).bakeRoot();
    }

    private DeathLook() {
    }
}

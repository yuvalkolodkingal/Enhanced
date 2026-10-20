package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Every suit's model (3.8, PLAN 7): {@link #get} is what lanes L3 and L4 draw. The 3.8 skeleton's suits are plain
 * coloured boxes over the player's limbs (one colour per suit, on a white texture); lane L7 replaces {@link #get}'s
 * body with its six models ({@link #register}) and keeps the signature.
 */
public final class SuitModels {
    private static final Map<SuitKind, SuitModel> MODELS = new EnumMap<>(SuitKind.class);
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    /** The model of a suit (lane L7's when registered, else the skeleton's coloured box). */
    public static SuitModel get(SuitKind kind) {
        return MODELS.computeIfAbsent(kind, k -> new BoxSuit(color(k)));
    }

    /** Lane L7's model of a suit (from the client setup). */
    public static void register(SuitKind kind, SuitModel model) {
        MODELS.put(kind, model);
    }

    private static int color(SuitKind kind) {
        return switch (kind) {
            case MARK_1 -> 0xFF6E6E6E;
            case MARK_3 -> 0xFFB3121B;
            case MARK_5 -> 0xFFC9CED6;
            case MARK_50 -> 0xFF9E1A1A;
            case MARK_85 -> 0xFF8C1418;
            case HULKBUSTER -> 0xFFC0301E;
        };
    }

    /** The skeleton's stand-in: a box a little larger than each limb, in the suit's colour. */
    private static final class BoxSuit implements SuitModel {
        private final int color;
        private final Map<SuitParts, ModelPart> parts = new EnumMap<>(SuitParts.class);

        BoxSuit(int color) {
            this.color = color;
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation grow = new CubeDeformation(0.6F);
            for (SuitParts part : SuitParts.VALUES) {
                root.addOrReplaceChild(part.name().toLowerCase(java.util.Locale.ROOT), box(part, grow), PartPose.ZERO);
            }
            ModelPart baked = LayerDefinition.create(mesh, 16, 16).bakeRoot();
            for (SuitParts part : SuitParts.VALUES) {
                parts.put(part, baked.getChild(part.name().toLowerCase(java.util.Locale.ROOT)));
            }
        }

        /** Each part's box in its limb's space (a player model's pixels). */
        private static CubeListBuilder box(SuitParts part, CubeDeformation grow) {
            CubeListBuilder b = CubeListBuilder.create();
            return switch (part) {
                case HELMET -> b.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, grow);
                case FACEPLATE -> b.addBox(-3.0F, -6.0F, -4.6F, 6, 5, 1, grow);
                case CHEST -> b.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, grow);
                case BACK -> b.addBox(-3.0F, 1.0F, 2.0F, 6, 8, 1, grow);
                case RIGHT_UPPER_ARM, LEFT_UPPER_ARM -> b.addBox(-1.5F, -2.0F, -2.0F, 4, 5, 4, grow);
                case RIGHT_FOREARM, LEFT_FOREARM -> b.addBox(-1.5F, 3.0F, -2.0F, 4, 5, 4, grow);
                case RIGHT_GAUNTLET, LEFT_GAUNTLET -> b.addBox(-1.5F, 8.0F, -2.0F, 4, 2, 4, grow);
                case RIGHT_THIGH, LEFT_THIGH -> b.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, grow);
                case RIGHT_SHIN, LEFT_SHIN -> b.addBox(-2.0F, 6.0F, -2.0F, 4, 4, 4, grow);
                case RIGHT_BOOT, LEFT_BOOT -> b.addBox(-2.0F, 10.0F, -2.0F, 4, 2, 4, grow);
            };
        }

        @Override
        public ResourceLocation texture() {
            return WHITE;
        }

        @Nullable
        @Override
        public ResourceLocation glowTexture() {
            return null;
        }

        @Override
        public void renderPart(SuitParts part, PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               int tint) {
            int a = (tint >>> 24) * (color >>> 24) / 255;
            int r = ((tint >> 16) & 0xFF) * ((color >> 16) & 0xFF) / 255;
            int g = ((tint >> 8) & 0xFF) * ((color >> 8) & 0xFF) / 255;
            int bl = (tint & 0xFF) * (color & 0xFF) / 255;
            parts.get(part).render(pose, buffer, light, overlay, (a << 24) | (r << 16) | (g << 8) | bl);
        }
    }

    private SuitModels() {
    }
}

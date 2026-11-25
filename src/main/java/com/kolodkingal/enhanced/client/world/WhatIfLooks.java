package com.kolodkingal.enhanced.client.world;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.multiverse.Multiverse;
import com.kolodkingal.enhanced.multiverse.WhatIfPeople;
import com.kolodkingal.enhanced.platform.client.ClientRegistrar;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * How the creatures of two What If universes look (3.19.1), drawn over each creature's own model by {@link WhatIfLayer}
 * (put on every creature's renderer at start), so every kind, the game's and the mods', gets the look without a model of
 * its own:
 * <ul>
 *   <li><b>Earth-2149</b>: every creature that has no zombie of its own (a zombie villager, a zombified piglin, a zoglin
 *   and a zombie horse are made instead where there is one: {@code ZombieWorld}) rots: a grey-green dead hide over its
 *   own, dark rotten patches, raw red flesh and bone showing through.</li>
 *   <li><b>The Gamma Bomb</b>: every creature is a Hulk of its kind: a second, swollen hide over the first, green and
 *   veined, its muscles bulging past its own shape, and the veins glowing gamma green, pulsing with its heartbeat.</li>
 * </ul>
 * Players, armour stands and the mod's people (the Asgardians, and the universes' own: {@link WhatIfPeople}) keep
 * their own look.
 */
public final class WhatIfLooks {
    static final ResourceLocation ROT = Enhanced.id("textures/entity/whatif/rot.png");
    static final ResourceLocation GAMMA_HIDE = Enhanced.id("textures/entity/whatif/gamma_hide.png");
    static final ResourceLocation GAMMA_VEINS = Enhanced.id("textures/entity/whatif/gamma_veins.png");
    /** How much the Hulk's second hide swells past the creature's own shape (across, and up). */
    static final float SWELL_WIDE = 1.09F;
    static final float SWELL_HIGH = 1.04F;

    public enum Look { NONE, ROT, GAMMA }

    private WhatIfLooks() {
    }

    /** Puts the look's layer on every creature's renderer (the players' keep theirs). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(ClientRegistrar.LayerContext layers) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND) {
                continue;
            }
            Object renderer;
            try {
                renderer = layers.anyRenderer(type);
            } catch (RuntimeException e) {
                continue;
            }
            if (renderer instanceof LivingEntityRenderer living) {
                living.addLayer(new WhatIfLayer(living));
            }
        }
    }

    /** The look of a creature in its universe. */
    public static Look look(LivingEntity entity) {
        if (entity instanceof Player || entity instanceof ArmorStand
                || entity instanceof com.kolodkingal.enhanced.asgard.npc.AsgardianEntity) {
            return Look.NONE;
        }
        ResourceKey<Level> dimension = entity.level().dimension();
        if (dimension == Multiverse.ZOMBIES_KEY) {
            return entity.getType().is(EntityTypeTags.UNDEAD) ? Look.NONE : Look.ROT;
        }
        return dimension == Multiverse.GAMMA_KEY ? Look.GAMMA : Look.NONE;
    }

    /** Draws the look over the model the creature was just drawn with (in the model's own space). */
    @SuppressWarnings({"rawtypes"})
    public static void draw(Model model, PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity, float ageInTicks) {
        Look look = look(entity);
        if (look == Look.NONE || entity.isInvisible()) {
            return;
        }
        if (look == Look.ROT) {
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(ROT)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            return;
        }
        // The Hulk's hide: swollen about the middle of the body (the model's space: y down, the feet at 1.501).
        float height = entity.getBbHeight() / Math.max(0.05F, entity.getScale());
        float middle = 1.501F - height * 0.5F;
        poseStack.pushPose();
        poseStack.translate(0.0F, middle, 0.0F);
        poseStack.scale(SWELL_WIDE, SWELL_HIGH, SWELL_WIDE);
        poseStack.translate(0.0F, -middle, 0.0F);
        model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(GAMMA_HIDE)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        float beat = 0.55F + 0.45F * Mth.sin((ageInTicks + entity.getId() * 7) * 0.18F);
        int g = Mth.clamp((int) (255 * beat), 0, 255);
        int color = 0xFF000000 | (int) (g * 0.45F) << 16 | g << 8 | (int) (g * 0.2F);
        model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(GAMMA_VEINS)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
    }
}

package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.client.render.AgathaHatLayer;
import com.kolodkingal.enhanced.client.render.BillyCrownLayer;
import com.kolodkingal.enhanced.client.render.CyclopsVisorLayer;
import com.kolodkingal.enhanced.client.render.GlowingEyesLayer;
import com.kolodkingal.enhanced.client.render.PhoenixFlameLayer;
import com.kolodkingal.enhanced.client.render.PowerAuraLayer;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.LookCatalog;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the hat, visor, glowing eyes and aura of a custom hero's look (3.4; the cape goes through the vanilla cape layer,
 * {@link CustomLooks#withCape}), each with the drawing code of the layer it borrows from: Agatha's hat, Billy's crown,
 * Cerebro's helmet, the armour model of Magneto's helmet, the Scarlet Witch's crown and Quicksilver's goggles (drawn, never
 * worn: no item is equipped, so nothing awakens), Cyclops's visors, the glowing eyes (a generic mask: two eyes of 2 x 1
 * pixels on one of four rows of the face, so they sit on any skin), the Scarlet Witch's and Vision's swirl, the Phoenix's
 * flames, the sand of Apocalypse. The eyes and the aura that show only while a power is on, or in flight, ease in and
 * out. A hat or visor is not drawn while something is worn on the head.
 *
 * <p>It also draws the eyes a custom hero's borrowed abilities light (3.4, R6): Thor's while the God of Thunder is
 * unleashed or the lightning called, the Scarlet Witch's red while she wields chaos magic, the Phoenix's, Agatha's,
 * Cyclops unleashed, Spider-Man's black eyes and the red slits of his Instant Kill, on the generic mask (the heroes'
 * own masks are painted for their own skins).
 */
public final class CustomLooksLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final RenderType[] EYES = new RenderType[LookCatalog.EYE_ROWS];
    private static final ResourceLocation[] EYE_MASKS = new ResourceLocation[LookCatalog.EYE_ROWS];
    /** How fast a layer that comes and goes eases (per tick). */
    private static final float FADE = 1.5F;

    static {
        List<String> masks = LookCatalog.textures(CustomLooks.EYES, "glow");
        for (int i = 0; i < LookCatalog.EYE_ROWS; i++) {
            EYE_MASKS[i] = CustomLooks.texture(masks.get(i));
            EYES[i] = RenderType.eyes(EYE_MASKS[i]);
        }
    }

    /** Per player: how much of the eyes and of the aura shows, the clock of the last frame, the tick of the last sparks. */
    private static final Map<Integer, float[]> SHOWN = new HashMap<>();

    private final PlayerModel<AbstractClientPlayer> model;
    private final ModelPart witchHat;
    private final ModelPart cerebro;
    private final ModelPart visor;
    private final HumanoidModel<AbstractClientPlayer> armor;

    /**
     * @param model    a player model a little larger than the skin (the auras and the masks)
     * @param witchHat Agatha's hat ({@code AgathaHatLayer.LAYER})
     * @param cerebro  Cerebro's helmet ({@code XavierChair.HELMET_LAYER})
     * @param visor    the visor band ({@code CyclopsVisorLayer.LAYER})
     * @param armor    the outer armour model (Magneto's helmet, the crown, the goggles)
     */
    public CustomLooksLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                            PlayerModel<AbstractClientPlayer> model, ModelPart witchHat, ModelPart cerebro, ModelPart visor,
                            HumanoidModel<AbstractClientPlayer> armor) {
        super(parent);
        this.model = model;
        this.witchHat = witchHat;
        this.cerebro = cerebro;
        this.visor = visor.getChild("visor");
        this.armor = armor;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        // (In Vision's disguise, his ability, he looks like himself: no layers.)
        if (data.power() != Power.CUSTOM || player.isInvisible() || CustomLooks.disguised(player)) {
            SHOWN.remove(player.getId());
            return;
        }
        CustomHero.Layers layers = CustomHeroes.hero(player).layers();
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        boolean headFree = player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();

        float t = player.tickCount + partialTick;
        float[] shown = SHOWN.computeIfAbsent(player.getId(), id -> new float[]{0.0F, 0.0F, t, -1.0F});
        float dt = Mth.clamp(t - shown[2], 0.0F, 4.0F);
        shown[2] = t;
        shown[0] = ease(shown[0], CustomLooks.shows(player, layers.eyes()), dt);
        shown[1] = ease(shown[1], CustomLooks.shows(player, layers.aura()), dt);

        if (headFree && !layers.hat().isNone()) {
            hat(poseStack, buffers, light, overlay, player, layers.hat(), ageInTicks);
        }
        if (headFree && !layers.visor().isNone()) {
            visor(poseStack, buffers, light, overlay, player, data, layers.visor(), ageInTicks);
        }
        eyes(poseStack, buffers, light, player, data, layers.eyes(), shown[0]);
        if (!layers.aura().isNone() && shown[1] > 0.01F) {
            aura(poseStack, buffers, light, player, layers.aura(), shown[1], t, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw,
                    headPitch);
            if (!(player instanceof PreviewPuppet) && shown[1] > 0.5F && player.tickCount != (int) shown[3]) {
                shown[3] = player.tickCount;
                particles(player, layers.aura());
            }
        }
    }

    private static float ease(float now, boolean on, float dt) {
        float target = on ? 1.0F : 0.0F;
        float step = dt / FADE * 0.35F;
        return now < target ? Math.min(target, now + step) : Math.max(target, now - step);
    }

    private static int argb(float alpha, int rgb) {
        return Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | (rgb & 0xFFFFFF);
    }

    // ---- the hat ----

    private void hat(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, AbstractClientPlayer player,
                     CustomHero.Layer hat, float ageInTicks) {
        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        int rgb = CustomLooks.color(player, CustomLooks.HAT, hat);
        switch (hat.style()) {
            case "witch_hat" -> AgathaHatLayer.drawHat(poseStack, buffers, light, overlay, parent.head, witchHat,
                    CustomLooks.texture(LookCatalog.textures(CustomLooks.HAT, "witch_hat").get(0)), argb(1.0F, rgb));
            case "billy_crown" -> BillyCrownLayer.drawCrown(poseStack, buffers, parent.head, ageInTicks, 1.0F, 1.0F, rgb);
            case "cerebro" -> {
                poseStack.pushPose();
                parent.head.translateAndRotate(poseStack);
                cerebro.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(
                        CustomLooks.texture(LookCatalog.textures(CustomLooks.HAT, "cerebro").get(0)))), LightTexture.FULL_BRIGHT, overlay);
                poseStack.popPose();
            }
            default -> armour(poseStack, buffers, light, LookCatalog.textures(CustomLooks.HAT, hat.style()));
        }
    }

    /** The head of the outer armour model in a helmet's armour texture. */
    private void armour(PoseStack poseStack, MultiBufferSource buffers, int light, List<String> textures) {
        if (textures.isEmpty()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        armor.setAllVisible(false);
        armor.head.visible = true;
        armor.hat.visible = true;
        armor.head.copyFrom(parent.head);
        armor.hat.copyFrom(parent.hat);
        armor.renderToBuffer(poseStack, buffers.getBuffer(RenderType.armorCutoutNoCull(CustomLooks.texture(textures.get(0)))), light,
                OverlayTexture.NO_OVERLAY);
    }

    // ---- the visor ----

    private void visor(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, AbstractClientPlayer player, PowerData data,
                       CustomHero.Layer layer, float ageInTicks) {
        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        switch (layer.style()) {
            case "cyclops_classic", "cyclops_apocalypse", "ruby_quartz" -> {
                ResourceLocation texture = layer.style().equals("cyclops_classic") ? CyclopsVisorLayer.VISOR_CLASSIC
                        : layer.style().equals("cyclops_apocalypse") ? CyclopsVisorLayer.VISOR_APOCALYPSE : CyclopsVisorLayer.GLASSES;
                // Its lens glows with Cyclops's Optic Blast when he has it, a little otherwise.
                float glow = data.has(HeroAbility.CYCLOPS_OPTIC_BLAST) ? CyclopsVisorLayer.lensGlow(player, data, ageInTicks) : 0.16F;
                CyclopsVisorLayer.drawVisor(poseStack, buffers, light, visor, parent.head, texture, glow);
            }
            case "quicksilver_goggles" -> armour(poseStack, buffers, light, LookCatalog.textures(CustomLooks.VISOR, layer.style()));
            default -> {
                // A mask: on the skin's hat layer, a little outside it.
                List<String> textures = LookCatalog.textures(CustomLooks.VISOR, layer.style());
                parent.copyPropertiesTo(model);
                model.hat.copyFrom(parent.head);
                int rgb = CustomLooks.color(player, CustomLooks.VISOR, layer);
                model.hat.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(CustomLooks.texture(textures.get(0)))), light,
                        overlay, argb(1.0F, rgb));
                if (textures.size() > 1) {
                    model.hat.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(CustomLooks.texture(textures.get(1)))), light,
                            overlay, 0xFFFFFFFF);
                }
            }
        }
    }

    // ---- the eyes ----

    private void eyes(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, PowerData data,
                      CustomHero.Layer layer, float shown) {
        int row = Mth.clamp(layer.row(), 0, LookCatalog.EYE_ROWS - 1);
        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        // Spider-Man's black eyes and the slits of the Instant Kill come first: they close the eyes.
        if (data.has(HeroAbility.SPIDER_LEGS) && data.is(Live.INSTANT_KILL)) {
            GlowingEyesLayer.drawEyes(poseStack, buffers, parent, EYES[row], 0xFFFF2020);
            return;
        }
        if (data.has(HeroAbility.SPIDER_BLACK_EYES) && data.is(Live.BLACK_EYES)) {
            dark(poseStack, buffers, light, parent, row, 1.0F);
            return;
        }
        int ability = abilityEyes(player, data);
        if (ability >= 0) {
            GlowingEyesLayer.drawEyes(poseStack, buffers, parent, EYES[row], 0xFF000000 | ability);
            return;
        }
        if (layer.isNone() || shown <= 0.01F) {
            return;
        }
        int rgb = CustomLooks.color(player, CustomLooks.EYES, layer);
        if ("black".equals(layer.style())) {
            dark(poseStack, buffers, light, parent, row, shown);
        } else {
            // (The eyes render type adds its colour: a dimmer colour is a fainter glow.)
            GlowingEyesLayer.drawEyes(poseStack, buffers, parent, EYES[row], 0xFF000000 | scale(rgb, shown));
        }
    }

    private static void dark(PoseStack poseStack, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> model, int row,
                             float alpha) {
        model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(EYE_MASKS[row])), light, OverlayTexture.NO_OVERLAY,
                argb(alpha, 0x000000));
    }

    private static int scale(int rgb, float f) {
        int r = Math.round(((rgb >> 16) & 0xFF) * f);
        int g = Math.round(((rgb >> 8) & 0xFF) * f);
        int b = Math.round((rgb & 0xFF) * f);
        return r << 16 | g << 8 | b;
    }

    /**
     * The colour the eyes glow because of a borrowed ability that is on now (the colours of the heroes' own eyes), or -1.
     */
    public static int abilityEyes(PowerData data) {
        return abilityEyes(null, data);
    }

    /** {@link #abilityEyes(PowerData)}, 3.5: in the colour the player's pick of the ability that lights them chose. */
    public static int abilityEyes(@Nullable Player player, PowerData data) {
        if (data.has(HeroAbility.THOR_LIGHTNING) && data.is(Live.CALLING_LIGHTNING)) {
            return AbilityColors.shade(player, HeroAbility.THOR_LIGHTNING, 0xCFEFFF);
        }
        if (data.has(HeroAbility.THOR_THUNDER) && data.toggle(Toggle.GOD_OF_THUNDER)) {
            return AbilityColors.shade(player, HeroAbility.THOR_THUNDER, 0xCFEFFF);
        }
        if (data.has(HeroAbility.WANDA_CHAOS_BEAM) && data.beamActive()) {
            return AbilityColors.shade(player, HeroAbility.WANDA_CHAOS_BEAM, 0xFF2A4A);
        }
        if (data.has(HeroAbility.WANDA_CHAOS_SHIELD) && data.chaosShieldActive()) {
            return AbilityColors.shade(player, HeroAbility.WANDA_CHAOS_SHIELD, 0xFF2A4A);
        }
        if (data.has(HeroAbility.WANDA_TELEKINESIS) && data.heldEntityId() != -1) {
            return AbilityColors.shade(player, HeroAbility.WANDA_TELEKINESIS, 0xFF2A4A);
        }
        if (data.has(HeroAbility.PHOENIX_MIND) && data.toggle(Toggle.MIND_READING)) {
            return AbilityColors.shade(player, HeroAbility.PHOENIX_MIND, 0xFFA43A);
        }
        if (data.has(HeroAbility.PHOENIX_BURST) && data.is(Live.PHOENIX_BLAZE)) {
            return AbilityColors.shade(player, HeroAbility.PHOENIX_BURST, 0xFFA43A);
        }
        if (data.has(HeroAbility.AGATHA_PURPLE_BEAM) && data.is(Live.BEAMING)) {
            return AbilityColors.shade(player, HeroAbility.AGATHA_PURPLE_BEAM, 0xB565FF);
        }
        if (data.has(HeroAbility.CYCLOPS_GLASSES) && data.is(Live.CYCLOPS_UNLEASHED)) {
            return AbilityColors.shade(player, HeroAbility.CYCLOPS_OPEN_EYES, 0xFF2A2A);
        }
        return -1;
    }

    // ---- the aura ----

    private void aura(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, CustomHero.Layer layer,
                      float shown, float t, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                      float headPitch) {
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTick);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.setAllVisible(true);
        int rgb = CustomLooks.color(player, CustomLooks.AURA, layer);
        ResourceLocation texture = CustomLooks.texture(LookCatalog.textures(CustomLooks.AURA, layer.style()).get(0));
        switch (layer.style()) {
            case "flames" -> PhoenixFlameLayer.drawFlames(poseStack, buffers, model, t, shown * (0.85F + 0.15F * Mth.sin(0.5F * t)),
                    argb(0.47F, rgb), argb(0.25F, CustomHeroes.hero(player).glow()));
            case "sand" -> PowerAuraLayer.drawSwirl(poseStack, buffers, light, model, texture, t, 0.5F, argb(0.55F * shown, 0xFFFFFF));
            case "sparks" -> PowerAuraLayer.drawSwirl(poseStack, buffers, light, model, texture, t, 2.5F, argb(0.3F * shown, rgb));
            case "mist" -> PowerAuraLayer.drawSwirl(poseStack, buffers, light, model, texture, t, 0.35F, argb(0.35F * shown, rgb));
            default -> PowerAuraLayer.drawSwirl(poseStack, buffers, light, model, texture, t, 1.0F, argb(0.56F * shown, rgb));
        }
    }

    /** Once a tick while the aura shows: its sparks (electric) or its mist (drifting dust in his colour). */
    private static void particles(AbstractClientPlayer player, CustomHero.Layer layer) {
        RandomSource random = player.getRandom();
        double x = player.getX() + (random.nextDouble() - 0.5) * 0.9;
        double y = player.getY() + random.nextDouble() * player.getBbHeight();
        double z = player.getZ() + (random.nextDouble() - 0.5) * 0.9;
        if ("sparks".equals(layer.style()) && random.nextInt(2) == 0) {
            player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0, 0.0, 0.0);
        } else if ("mist".equals(layer.style()) && random.nextInt(3) == 0) {
            int rgb = layer.color() >= 0 ? layer.color() : CustomHeroes.hero(player).color();
            Vector3f c = new Vector3f(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
            player.level().addParticle(new DustParticleOptions(c, 1.4F), x, y, z, 0.0, 0.01, 0.0);
        }
    }

    /** Forget every player (leaving the world). */
    public static void clear() {
        SHOWN.clear();
    }
}

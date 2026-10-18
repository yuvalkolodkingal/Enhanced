package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.LookCatalog;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The layers of a custom hero's look (3.4) as the client draws them: which of the five are on now (their "when": always,
 * while one of his powers is on, while he flies), their colours, and the textures made by tinting a grey template (the
 * plain cape: the vanilla cape layer needs a texture of its own colour, made once per colour). The cape reaches the game
 * through the hero's skin ({@code SuitSkins}); the rest is drawn by {@link CustomLooksLayer}. The editor's preview puppet
 * shows every layer whatever its "when", so a choice is seen at once.
 */
public final class CustomLooks {
    /** The five kinds, in the order of {@link CustomHero.Layers#KINDS}. */
    public static final int CAPE = 0;
    public static final int HAT = 1;
    public static final int VISOR = 2;
    public static final int EYES = 3;
    public static final int AURA = 4;

    private static final Map<Integer, ResourceLocation> PLAIN_CAPES = new HashMap<>();
    private static final Map<String, PlayerSkin> WITH_CAPE = new HashMap<>();

    /** The layers of the player's look, or none when he is not a custom hero. */
    public static CustomHero.Layers of(Player player) {
        return Powers.power(player) == Power.CUSTOM ? CustomHeroes.hero(player).layers() : CustomHero.Layers.NONE_ALL;
    }

    /** Whether a layer shows now (its "when": always, while a power is on, while flying). */
    public static boolean shows(Player player, CustomHero.Layer layer) {
        if (layer.isNone()) {
            return false;
        }
        if (player instanceof PreviewPuppet) {
            return true;
        }
        return switch (layer.when()) {
            case "powers" -> powersOn(player);
            case "flying" -> flying(player);
            default -> true;
        };
    }

    /** Whether he flies (with his powers, creative flight or an elytra). */
    public static boolean flying(Player player) {
        return Powers.get(player).flying() || player.getAbilities().flying || player.isFallFlying();
    }

    /**
     * Whether one of his powers is on now: a power switched on or held (a generic one, or a hero's ability that says so),
     * a beam, a shield, something held by telekinesis, a field.
     */
    public static boolean powersOn(Player player) {
        PowerData data = Powers.get(player);
        if (data.beamActive() || data.chaosShieldActive() || data.heldEntityId() != -1 || data.is(Live.BEAMING)) {
            return true;
        }
        CustomState state = CustomHeroes.state(player);
        if (state.onBits() != 0 || state.field()) {
            return true;
        }
        HeroKit kit = data.kit();
        if (kit != null) {
            for (HeroAbility a : kit.rows()) {
                if (a.isOn(data)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The colour a layer is drawn in: its own, the hero's (-1), or the style's fixed one (an eye style). */
    public static int color(Player player, int kind, CustomHero.Layer layer) {
        if (kind == EYES) {
            int fixed = LookCatalog.eyeColor(layer.style());
            if (fixed >= 0) {
                return fixed;
            }
        }
        if (!LookCatalog.TINTED.contains(layer.style())) {
            return 0xFFFFFF;
        }
        return layer.color() >= 0 ? layer.color() : CustomHeroes.hero(player).color();
    }

    /** A texture of the catalogue ({@code textures/...} under the mod's assets). */
    public static ResourceLocation texture(String path) {
        return Enhanced.id(path);
    }

    // ---- the cape ----

    /** The texture of the hero's cape, or null when he has none. */
    @Nullable
    public static ResourceLocation cape(Player player) {
        CustomHero.Layer cape = of(player).cape();
        if (cape.isNone()) {
            return null;
        }
        if ("plain".equals(cape.style())) {
            return plainCape(color(player, CAPE, cape));
        }
        return texture(LookCatalog.textures(CAPE, cape.style()).get(0));
    }

    /** {@code skin} with the hero's cape (the same skin when he has none). */
    public static PlayerSkin withCape(Player player, PlayerSkin skin) {
        ResourceLocation cape = cape(player);
        if (cape == null || cape.equals(skin.capeTexture())) {
            return skin;
        }
        String key = skin.texture() + "|" + skin.model() + "|" + cape;
        return WITH_CAPE.computeIfAbsent(key, k -> new PlayerSkin(skin.texture(), skin.textureUrl(), cape, skin.elytraTexture(),
                skin.model(), skin.secure()));
    }

    /** The grey cape template tinted to {@code rgb}, made the first time a colour is asked for. */
    private static ResourceLocation plainCape(int rgb) {
        ResourceLocation made = PLAIN_CAPES.get(rgb);
        if (made != null) {
            return made;
        }
        ResourceLocation template = texture(LookCatalog.textures(CAPE, "plain").get(0));
        ResourceLocation id = Enhanced.id(String.format("custom_cape/%06x", rgb));
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(template)) {
            NativeImage image = NativeImage.read(in);
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int grey = abgr & 0xFF;
                    int r = ((rgb >> 16) & 0xFF) * grey / 255;
                    int g = ((rgb >> 8) & 0xFF) * grey / 255;
                    int b = (rgb & 0xFF) * grey / 255;
                    image.setPixelRGBA(x, y, (abgr & 0xFF000000) | b << 16 | g << 8 | r);
                }
            }
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        } catch (Exception e) {
            Enhanced.LOGGER.warn("The plain cape could not be tinted", e);
            id = template;
        }
        PLAIN_CAPES.put(rgb, id);
        return id;
    }

    /** The client left the world: the tinted capes go. */
    public static void clear() {
        for (ResourceLocation id : PLAIN_CAPES.values()) {
            if (id.getPath().startsWith("custom_cape/")) {
                Minecraft.getInstance().getTextureManager().release(id);
            }
        }
        PLAIN_CAPES.clear();
        WITH_CAPE.clear();
    }

    private CustomLooks() {
    }
}

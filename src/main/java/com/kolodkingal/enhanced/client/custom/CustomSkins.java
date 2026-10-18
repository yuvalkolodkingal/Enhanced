package com.kolodkingal.enhanced.client.custom;

import com.google.common.hash.Hashing;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroLooks;
import com.kolodkingal.enhanced.custom.SkinCheck;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The skins of custom heroes on this client (3.3). A hero's PNG reaches every client with his synced definition; here it is
 * checked again ({@link SkinCheck}, then decoded), made a texture once per different picture (by its SHA-1, so the same
 * skin on two heroes, or a definition sent again, costs nothing), and released half a minute after nobody wears it, and
 * all at once when the client leaves the world. A hero who wears one of the mod's looks is drawn in that look's suit; one
 * who wears his own skin, in his own. A picture that fails the checks is drawn as the player's own skin.
 */
public final class CustomSkins {
    /** How long a texture nobody wears is kept (ms), in case the hero comes back into view. */
    private static final long KEEP_MS = 30_000L;

    private static final class Entry {
        @Nullable
        final ResourceLocation texture;
        long lastUsed;

        Entry(@Nullable ResourceLocation texture) {
            this.texture = texture;
        }
    }

    private static final Map<String, Entry> BY_HASH = new HashMap<>();
    private static final Map<String, PlayerSkin> SKINS = new HashMap<>();

    /** The suit a custom hero is drawn in, or null for his own skin. */
    @Nullable
    public static PlayerSkin suitFor(AbstractClientPlayer player) {
        CustomHero hero = CustomHeroes.hero(player);
        if (hero.wearsSkin()) {
            ResourceLocation texture = texture(hero.skin());
            if (texture == null) {
                return null;
            }
            String key = texture + (hero.slim() ? "#slim" : "#wide");
            return SKINS.computeIfAbsent(key, k -> new PlayerSkin(texture, null, null, null,
                    hero.slim() ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, true));
        }
        HeroLooks.Look look = HeroLooks.byRef(hero.look());
        if (look == null) {
            return null;
        }
        return SKINS.computeIfAbsent(look.ref(), k -> new PlayerSkin(look.skin(), null, null, null,
                look.slim() ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, true));
    }

    /** The texture of a skin's bytes (made the first time they are seen), or null if they are not a skin. */
    @Nullable
    public static ResourceLocation texture(byte[] png) {
        String hash = Hashing.sha1().hashBytes(png).toString();
        Entry entry = BY_HASH.get(hash);
        if (entry == null) {
            entry = new Entry(make(hash, png));
            BY_HASH.put(hash, entry);
        }
        entry.lastUsed = Util.getMillis();
        return entry.texture;
    }

    @Nullable
    private static ResourceLocation make(String hash, byte[] png) {
        if (SkinCheck.check(png, CustomHero.MAX_SKIN_WIRE) != SkinCheck.Verdict.OK) {
            return null;
        }
        try {
            NativeImage image = NativeImage.read(png);
            if (image.getWidth() != SkinCheck.SIZE || image.getHeight() != SkinCheck.SIZE) {
                image.close();
                return null;
            }
            // As the game does with a downloaded skin: the inner layer has no holes (only the outer layer may be see-through).
            noAlpha(image, 0, 0, 32, 16);
            noAlpha(image, 0, 16, 64, 32);
            noAlpha(image, 16, 48, 48, 64);
            ResourceLocation id = Enhanced.id("custom_skin/" + hash);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            return id;
        } catch (Exception e) {
            Enhanced.LOGGER.warn("A custom hero's skin could not be read ({} bytes)", png.length, e);
            return null;
        }
    }

    private static void noAlpha(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) | 0xFF000000);
            }
        }
    }

    /** Once a second or so: the textures nobody has worn for {@link #KEEP_MS} are released. */
    public static void tick() {
        long now = Util.getMillis();
        Set<ResourceLocation> released = new HashSet<>();
        Iterator<Map.Entry<String, Entry>> it = BY_HASH.entrySet().iterator();
        while (it.hasNext()) {
            Entry entry = it.next().getValue();
            if (now - entry.lastUsed > KEEP_MS) {
                if (entry.texture != null) {
                    Minecraft.getInstance().getTextureManager().release(entry.texture);
                    released.add(entry.texture);
                }
                it.remove();
            }
        }
        if (!released.isEmpty()) {
            SKINS.values().removeIf(skin -> released.contains(skin.texture()));
        }
    }

    /** How many skin textures are held now (for the tests of the cache). */
    public static int held() {
        return BY_HASH.size();
    }

    /** The client left the world: every texture goes. */
    public static void clear() {
        for (Entry entry : BY_HASH.values()) {
            if (entry.texture != null) {
                Minecraft.getInstance().getTextureManager().release(entry.texture);
            }
        }
        BY_HASH.clear();
        SKINS.clear();
    }

    private CustomSkins() {
    }
}

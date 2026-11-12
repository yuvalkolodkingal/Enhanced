package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.11 only: from 1.21.9 a skin's textures are client assets (an id and the texture's path), not texture ids; these
 * make and read the mod's skins as 1.21.1's (texture, url, cape, elytra, model, secure).
 */
public final class Skins {
    private Skins() {
    }

    @Nullable
    private static ClientAsset.Texture asset(@Nullable Identifier texture) {
        return texture == null ? null : new ClientAsset.ResourceTexture(texture, texture);
    }

    public static PlayerSkin of(Identifier texture, @Nullable String url, @Nullable Identifier cape, @Nullable Identifier elytra,
                                PlayerModelType model, boolean secure) {
        ClientAsset.Texture body = url == null ? asset(texture) : new ClientAsset.DownloadedTexture(texture, url);
        return new PlayerSkin(body, asset(cape), asset(elytra), model, secure);
    }

    @Nullable
    public static Identifier cape(PlayerSkin skin) {
        return skin.cape() == null ? null : skin.cape().texturePath();
    }

    @Nullable
    public static Identifier elytra(PlayerSkin skin) {
        return skin.elytra() == null ? null : skin.elytra().texturePath();
    }

    @Nullable
    public static String url(PlayerSkin skin) {
        return skin.body() instanceof ClientAsset.DownloadedTexture d ? d.url() : null;
    }
}

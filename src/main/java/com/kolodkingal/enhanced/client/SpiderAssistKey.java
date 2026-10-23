package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.platform.client.ClientRegistrar;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
// TODO(port seam) S1: a common key conflict context (notes/l6.md); NeoForge's until it lands.
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * The Web Assist key (3.3, default J): Spider-Man switches his assisted swing on and off (the client option
 * {@code spider_man.webAssist}, which the config screen shows too). Registered by the Spider-Man lane itself, next to
 * the mod's other keys.
 */
@Subscriber(side = Side.CLIENT)
public final class SpiderAssistKey {
    public static final KeyMapping WEB_ASSIST = new KeyMapping("key.enhanced.web_assist", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J, KeyBindings.CATEGORY);

    /** The key's registration (an event of the mod's own bus, kept apart from the game's). */
    public static final class Registration {
        /** Port P0: through the platform's client registrar (see ClientSetup). */
        public static void register(ClientRegistrar registrar) {
            registrar.keyMapping(WEB_ASSIST);
        }

        private Registration() {
        }
    }

    @Subscribe
    public static void onClientTick(ClientTickEvent.Post event) {
        boolean clicked = false;
        while (WEB_ASSIST.consumeClick()) {
            clicked = true;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (clicked && player != null && mc.screen == null && !player.isSpectator() && Powers.get(player).has(HeroAbility.SPIDER_ZIP)) {
            SpiderMotion.toggleAssist(player);
        }
    }

    private SpiderAssistKey() {
    }
}

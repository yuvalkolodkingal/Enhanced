package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * The Web Assist key (3.3, default J): Spider-Man switches his assisted swing on and off (the client option
 * {@code spider_man.webAssist}, which the config screen shows too). Registered by the Spider-Man lane itself, next to
 * the mod's other keys.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class SpiderAssistKey {
    public static final KeyMapping WEB_ASSIST = new KeyMapping("key.enhanced.web_assist", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J, KeyBindings.CATEGORY);

    /** The key's registration (an event of the mod's own bus, kept apart from the game's). */
    @EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(WEB_ASSIST);
        }

        private Registration() {
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        boolean clicked = false;
        while (WEB_ASSIST.consumeClick()) {
            clicked = true;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (clicked && player != null && mc.screen == null && !player.isSpectator() && Powers.get(player).power() == Power.SPIDER_MAN) {
            SpiderMotion.toggleAssist(player);
        }
    }

    private SpiderAssistKey() {
    }
}

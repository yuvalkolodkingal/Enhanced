package com.kolodkingal.enhanced.client.serum;

import com.kolodkingal.enhanced.network.SerumChoicePayload;
import com.kolodkingal.enhanced.network.SerumScreenPayload;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Super Soldier Serum's choice (3.8, PLAN 2.1): Captain America, Red Hulk or Falcon (Falcon locked without the
 * Exo-Wings). The 3.8 skeleton's screen is the plainest that works end to end (three buttons and Cancel; a choice is
 * sent, the server answers): lane L1 replaces it with the three painted cards, the keys 1/2/3 and Enter, and keeps
 * {@link #open}. Escape or Cancel sends nothing (the serum is kept).
 */
public class SerumScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int GAP = 6;

    private final boolean falconUnlocked;

    public SerumScreen(boolean falconUnlocked) {
        super(Component.translatable("screen.enhanced.serum.title"));
        this.falconUnlocked = falconUnlocked;
    }

    /** Opens the screen (the server says he used a serum). */
    public static void open(SerumScreenPayload payload) {
        Minecraft.getInstance().setScreen(new SerumScreen(payload.falconUnlocked()));
    }

    @Override
    protected void init() {
        int left = width / 2 - BUTTON_WIDTH / 2;
        int top = height / 2 - 36;
        addRenderableWidget(choice(Power.CAPTAIN_AMERICA, left, top));
        addRenderableWidget(choice(Power.RED_HULK, left, top + 20 + GAP));
        Button falcon = choice(Power.FALCON, left, top + 2 * (20 + GAP));
        falcon.active = falconUnlocked;
        if (!falconUnlocked) {
            falcon.setTooltip(Tooltip.create(Component.translatable("screen.enhanced.serum.falcon_locked")));
        }
        addRenderableWidget(falcon);
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.serum.cancel"), b -> onClose())
                .bounds(width / 2 - 50, top + 3 * (20 + GAP) + 8, 100, 20).build());
    }

    private Button choice(Power hero, int left, int top) {
        return Button.builder(hero.displayName(), b -> {
            PacketDistributor.sendToServer(new SerumChoicePayload(hero));
            onClose();
        }).bounds(left, top, BUTTON_WIDTH, 20).build();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = height / 2 - 36;
        graphics.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(0x6FB8FF).withBold(true)),
                width / 2, top - 26, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.enhanced.serum.subtitle")
                .withStyle(ChatFormatting.GRAY), width / 2, top - 14, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

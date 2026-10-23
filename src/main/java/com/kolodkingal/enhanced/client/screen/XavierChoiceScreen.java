package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.ability.XavierAbilities;
import com.kolodkingal.enhanced.network.XavierChoicePayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * "Which Charles are you?" (3.3): the screen that opens when a player awakens as Professor X. Two big cards, each with
 * the face of its skin: the young Charles of First Class and Days of Future Past (he walks: Hank's serum) and the old
 * professor in his wheelchair. A choice is sent to the server, which checks it ({@code XavierAbilities.choose});
 * "Later" closes the screen and keeps the young one (the Change Look key changes it at any time).
 */
public class XavierChoiceScreen extends Screen {
    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 62;
    private static final int GAP = 8;

    public XavierChoiceScreen() {
        super(Component.translatable("screen.enhanced.xavier.title"));
    }

    /** Opens the screen (the server says he has just awakened). */
    public static void open() {
        Minecraft.getInstance().setScreen(new XavierChoiceScreen());
    }

    @Override
    protected void init() {
        int top = height / 2 - CARD_HEIGHT / 2 - 6;
        int left = width / 2 - CARD_WIDTH - GAP / 2;
        addRenderableWidget(new Card(left, top, XavierAbilities.YOUNG));
        addRenderableWidget(new Card(left + CARD_WIDTH + GAP, top, XavierAbilities.OLD));
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.xavier.later"), b -> onClose())
                .bounds(width / 2 - 50, top + CARD_HEIGHT + 16, 100, 20).build());
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || Powers.power(mc.player) != Power.PROFESSOR_X) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = height / 2 - CARD_HEIGHT / 2 - 6;
        graphics.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(Power.PROFESSOR_X.glow).withBold(true)),
                width / 2, top - 22, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.enhanced.xavier.subtitle").withStyle(ChatFormatting.GRAY),
                width / 2, top - 10, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int darken(int rgb) {
        return (rgb >> 1) & 0x7F7F7F;
    }

    /** A big card for one version: the face of its skin, its name in its colour, two lines about it. */
    private final class Card extends AbstractButton {
        private final String version;
        private final Variant look;

        Card(int x, int y, String version) {
            super(x, y, CARD_WIDTH, CARD_HEIGHT, Component.translatable("screen.enhanced.xavier." + version));
            this.version = version;
            this.look = Variants.get(Power.PROFESSOR_X, version);
        }

        @Override
        public void onPress() {
            Net.sendToServer(new XavierChoicePayload(version));
            onClose();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = isHoveredOrFocused();
            int x = getX();
            int y = getY();
            int color = look.color();
            graphics.fill(x, y, x + width, y + height, lit ? 0xE0141E3A : 0xC00C1226);
            graphics.renderOutline(x, y, width, height, 0xFF000000 | (lit ? look.glow() : darken(color)));
            if (lit) {
                graphics.renderOutline(x + 1, y + 1, width - 2, height - 2, 0x80000000 | color);
            }
            ResourceLocation skin = look.skin();
            if (skin != null) {
                // The face, then the hat layer over it, at three times the size.
                graphics.blit(skin, x + 8, y + 15, 24, 24, 8, 8, 8, 8, 64, 64);
                graphics.blit(skin, x + 8, y + 15, 24, 24, 40, 8, 8, 8, 64, 64);
            }
            int text = x + 40 + (width - 40) / 2;
            Component name = getMessage().copy().withStyle(s -> s.withColor(look.glow()).withBold(true));
            graphics.drawCenteredString(font, name, text, y + 9, 0xFFFFFF);
            String key = "screen.enhanced.xavier." + version;
            graphics.drawCenteredString(font, Component.translatable(key + ".desc").withStyle(ChatFormatting.GRAY), text, y + 26, 0xFFFFFF);
            graphics.drawCenteredString(font, Component.translatable(key + ".desc2").withStyle(ChatFormatting.DARK_GRAY), text, y + 40, 0xFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

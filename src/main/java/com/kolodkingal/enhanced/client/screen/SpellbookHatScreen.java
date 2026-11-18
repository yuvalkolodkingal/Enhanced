package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.spellbook.Spellbook;
import com.kolodkingal.enhanced.spellbook.SpellbookHatPayload;
import com.kolodkingal.enhanced.spellbook.Spellbooks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Ethereal witch's hat (the Book of Ethereal Witchcraft, Sneak + use): twelve swatches of colour for Agatha's hat, the
 * hat off or on, and the book's magic given up. Every choice goes to the server ({@link SpellbookHatPayload}), which
 * changes her definition; the hat every player sees follows it.
 */
public class SpellbookHatScreen extends Screen {
    private static final int SWATCH = 26;
    private static final int GAP = 6;
    private static final int COLUMNS = 6;

    public SpellbookHatScreen() {
        super(Component.translatable("screen.enhanced.spellbook_hat.title"));
    }

    /** Opens the screen (the Ethereal witch used her book with Sneak). */
    public static void open() {
        Minecraft.getInstance().setScreen(new SpellbookHatScreen());
    }

    private int gridLeft() {
        return width / 2 - (COLUMNS * SWATCH + (COLUMNS - 1) * GAP) / 2;
    }

    private int gridTop() {
        return height / 2 - SWATCH - GAP / 2 - 14;
    }

    @Override
    protected void init() {
        int left = gridLeft();
        int top = gridTop();
        for (int i = 0; i < Spellbooks.HAT_COLORS.size(); i++) {
            int x = left + (i % COLUMNS) * (SWATCH + GAP);
            int y = top + (i / COLUMNS) * (SWATCH + GAP);
            addRenderableWidget(new Swatch(x, y, i, Spellbooks.HAT_COLORS.get(i)));
        }
        int below = top + 2 * SWATCH + GAP + 14;
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.spellbook_hat.toggle"),
                b -> Net.sendToServer(new SpellbookHatPayload(SpellbookHatPayload.TOGGLE, 0)))
                .bounds(width / 2 - 102, below, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.spellbook_hat.done"), b -> onClose())
                .bounds(width / 2 + 2, below, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.spellbook_hat.renounce")
                        .withStyle(ChatFormatting.RED), b -> {
                    Net.sendToServer(new SpellbookHatPayload(SpellbookHatPayload.RENOUNCE, 0));
                    onClose();
                })
                .bounds(width / 2 - 102, below + 26, 204, 20).build());
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || Spellbook.of(mc.player) != Spellbook.ETHEREAL) {
            onClose();
        }
    }

    /** The colour of the hat she wears now, or -1 if she wears none. */
    private static int worn() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return -1;
        }
        CustomHero.Layer hat = CustomHeroes.hero(mc.player).layers().hat();
        return hat.isNone() ? -1 : hat.color() & 0xFFFFFF;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = gridTop();
        graphics.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(Spellbook.ETHEREAL.glow).withBold(true)),
                width / 2, top - 26, 0xFFFFFF);
        Component state = Component.translatable(worn() < 0 ? "screen.enhanced.spellbook_hat.off" : "screen.enhanced.spellbook_hat.on")
                .withStyle(ChatFormatting.GRAY);
        graphics.drawCenteredString(font, state, width / 2, top - 13, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** One colour: a square of it, outlined in the book's glow when it is the hat's colour now. */
    private static final class Swatch extends AbstractButton {
        private final int index;
        private final int rgb;

        Swatch(int x, int y, int index, int rgb) {
            super(x, y, SWATCH, SWATCH, Component.translatable("screen.enhanced.spellbook_hat.color", index + 1));
            this.index = index;
            this.rgb = rgb;
        }

        @Override
        public void onPress() {
            Net.sendToServer(new SpellbookHatPayload(SpellbookHatPayload.COLOR, index));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            boolean chosen = worn() == rgb;
            boolean lit = isHoveredOrFocused();
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, chosen ? 0xFF000000 | Spellbook.ETHEREAL.glow : 0xFF101010);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000 | rgb);
            if (lit) {
                graphics.renderOutline(x, y, width, height, 0xFFFFFFFF);
            }
            if (chosen) {
                graphics.renderOutline(x + 2, y + 2, width - 4, height - 4, 0xC0FFFFFF);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

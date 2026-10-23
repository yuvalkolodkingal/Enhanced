package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.client.asgard.AsgardianRenderer;
import com.kolodkingal.enhanced.network.AsgardTalkPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The talk screen of the Asgardians (3.5): a panel at the bottom of the screen, the speaker's face from his skin in a
 * frame of his colour, his name, his line typed out at 40 letters a second (a click finishes it), and up to three answer
 * buttons above the panel; "Farewell" or Esc closes it. Choosing an answer sends it to the server
 * ({@code AsgardTalkPayload.Choose}), which answers with the next line or opens a trade; closing sends the empty answer
 * (the talk is over). In a right-to-left language (Hebrew) the face moves to the right and the text aligns right.
 */
public class AsgardTalkScreen extends Screen {
    /** Letters typed per tick (40 a second). */
    static final float TYPE_SPEED = 2.0F;
    static final int PANEL_HEIGHT = 84;
    static final int FACE = 40;
    static final int BUTTON_HEIGHT = 20;

    private final AsgardTalkPayload.Open talk;
    private final Role role;
    private final Component line;
    private final String text;
    private float typed;
    private boolean answered;
    private int waiting = -1;

    AsgardTalkScreen(AsgardTalkPayload.Open talk) {
        super(Component.translatable(roleOf(talk).nameKey()));
        this.talk = talk;
        this.role = roleOf(talk);
        Object[] args = new Object[talk.args().size()];
        for (int i = 0; i < args.length; i++) {
            String arg = talk.args().get(i);
            args[i] = arg.startsWith("@") ? Component.translatable(arg.substring(1)) : arg;
        }
        this.line = Component.translatable(talk.node(), args);
        this.text = line.getString();
    }

    private static Role roleOf(AsgardTalkPayload.Open talk) {
        Role[] all = Role.values();
        return talk.role() >= 0 && talk.role() < all.length ? all[talk.role()] : Role.CITIZEN;
    }

    /** The server opened a talk (or the next line of one). */
    public static void open(AsgardTalkPayload.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AsgardTalkScreen old) {
            old.answered = true; // replaced: the talk goes on
        }
        mc.setScreen(new AsgardTalkScreen(payload));
    }

    static boolean rightToLeft() {
        return Language.getInstance().isDefaultRightToLeft();
    }

    private int panelWidth() {
        return Math.min(380, width - 20);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return height - PANEL_HEIGHT - 12;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(300, panelWidth() - 40);
        int x = (width - buttonWidth) / 2;
        List<String> options = talk.options();
        int y = panelTop() - 6 - options.size() * (BUTTON_HEIGHT + 4);
        for (int i = 0; i < options.size(); i++) {
            addRenderableWidget(new Answer(x, y + i * (BUTTON_HEIGHT + 4), buttonWidth, i + 1, options.get(i)));
        }
        Component bye = Component.translatable("npc.enhanced.screen.farewell");
        int byeWidth = font.width(bye) + 12;
        int byeX = rightToLeft() ? panelLeft() + 6 : panelLeft() + panelWidth() - byeWidth - 6;
        addRenderableWidget(new Farewell(byeX, panelTop() + PANEL_HEIGHT - 16, byeWidth, bye));
    }

    @Override
    public void tick() {
        typed = Math.min(text.length(), typed + TYPE_SPEED);
        if (waiting >= 0 && ++waiting > 10) {
            // The server said nothing more: the talk ended with the answer.
            onClose();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.level != null && mc.level.getEntity(talk.entityId()) != null)) {
            onClose();
        }
    }

    private boolean doneTyping() {
        return typed >= text.length();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!doneTyping() && mouseY >= panelTop()) {
            typed = text.length();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The world stays visible: only the panel is drawn.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int w = panelWidth();
        int color = 0xFF000000 | role.color();
        graphics.fillGradient(left, top, left + w, top + PANEL_HEIGHT, 0xE0141A2C, 0xE00A0E18);
        graphics.renderOutline(left, top, w, PANEL_HEIGHT, color);
        graphics.fill(left + 1, top + 1, left + w - 1, top + 2, 0x60FFFFFF & color);
        boolean rtl = rightToLeft();
        int faceX = rtl ? left + w - FACE - 10 : left + 10;
        int faceY = top + 12;
        graphics.fill(faceX - 2, faceY - 2, faceX + FACE + 2, faceY + FACE + 2, color);
        graphics.fill(faceX - 1, faceY - 1, faceX + FACE + 1, faceY + FACE + 1, 0xFF000000);
        ResourceLocation skin = AsgardianRenderer.texture(role, variant());
        graphics.blit(skin, faceX, faceY, FACE, FACE, 8, 8, 8, 8, 64, 64);
        graphics.blit(skin, faceX, faceY, FACE, FACE, 40, 8, 8, 8, 64, 64);
        int textLeft = rtl ? left + 10 : faceX + FACE + 10;
        int textRight = rtl ? faceX - 10 : left + w - 10;
        int textWidth = textRight - textLeft;
        Component name = title.copy().withStyle(s -> s.withColor(role.color()).withBold(true));
        graphics.drawString(font, name, rtl ? textRight - font.width(name) : textLeft, top + 8, 0xFFFFFF, true);
        int shown = Mth.floor(typed);
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(FormattedText.of(text.substring(0, Math.min(shown, text.length()))),
                textWidth));
        int y = top + 22;
        for (FormattedCharSequence l : lines) {
            if (y > top + PANEL_HEIGHT - 20) {
                break;
            }
            graphics.drawString(font, l, rtl ? textRight - font.width(l) : textLeft, y, 0xF2EEDD, false);
            y += font.lineHeight + 1;
        }
        if (!doneTyping() && (Minecraft.getInstance().level != null && (Minecraft.getInstance().level.getGameTime() / 6) % 2 == 0)) {
            graphics.drawString(font, "▸", rtl ? left + 10 : left + w - 14, top + PANEL_HEIGHT - 14, color, false);
        }
        for (var child : children()) {
            if (child instanceof Answer answer) {
                answer.visible = doneTyping();
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** The speaker's skin variant (the Einherjar's and citizens'), from his entity on this client. */
    private int variant() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getEntity(talk.entityId()) instanceof AsgardianEntity npc && npc.role() == role ? npc.variant() : 0;
    }

    private void answer(String option) {
        if (answered) {
            return;
        }
        answered = true;
        Net.sendToServer(new AsgardTalkPayload.Choose(talk.entityId(), option));
        waiting = 0;
        for (var child : children()) {
            if (child instanceof AbstractButton button) {
                button.active = false;
            }
        }
    }

    @Override
    public void removed() {
        if (!answered) {
            answered = true;
            Net.sendToServer(new AsgardTalkPayload.Choose(talk.entityId(), ""));
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** An answer: a dark button with a gold edge when hovered, its number and its words. */
    private final class Answer extends AbstractButton {
        private final String option;
        private final int number;

        Answer(int x, int y, int width, int number, String option) {
            super(x, y, width, BUTTON_HEIGHT, Component.translatable("npc.enhanced.option." + option));
            this.option = option;
            this.number = number;
        }

        @Override
        public void onPress() {
            answer(option);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = isHoveredOrFocused() && active;
            int color = 0xFF000000 | role.color();
            graphics.fill(getX(), getY(), getX() + width, getY() + height, lit ? 0xE0283048 : 0xD0121828);
            graphics.renderOutline(getX(), getY(), width, height, lit ? color : 0xFF3A4258);
            boolean rtl = rightToLeft();
            String n = number + ".";
            Component words = getMessage();
            int textY = getY() + (height - 8) / 2;
            int ink = active ? (lit ? 0xFFFFFF : 0xE6E0CC) : 0x808080;
            if (rtl) {
                graphics.drawString(font, n, getX() + width - 6 - font.width(n), textY, color, false);
                graphics.drawString(font, words, getX() + width - 12 - font.width(n) - font.width(words), textY, ink, false);
            } else {
                graphics.drawString(font, n, getX() + 6, textY, color, false);
                graphics.drawString(font, words, getX() + 12 + font.width(n), textY, ink, false);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** "Farewell": closes the talk. */
    private final class Farewell extends AbstractButton {
        Farewell(int x, int y, int width, Component message) {
            super(x, y, width, 12, message);
        }

        @Override
        public void onPress() {
            onClose();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int ink = isHoveredOrFocused() ? 0xFFFFFF : 0x9A9A9A;
            graphics.drawString(font, getMessage(), getX() + 6, getY() + 2, ink, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** Number keys 1-3 choose the answers. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int index = keyCode - 49; // GLFW_KEY_1
        if (index >= 0 && index < talk.options().size() && doneTyping()) {
            answer(talk.options().get(index));
            return true;
        }
        if (!doneTyping() && (keyCode == 32 || keyCode == 257)) { // space, enter
            typed = text.length();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

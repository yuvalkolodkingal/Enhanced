package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.network.MindTalkPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A talk in Vision's mind (3.12): who speaks, his line (typed out), and the answers. A forced talk (Ultron's, the
 * Human Torch's) has no way out but its answers; the others close with Escape or "Goodbye".
 */
public class MindTalkScreen extends Screen {
    private static final int PANEL_HEIGHT = 74;
    private static final int TYPE_SPEED = 3;

    private final MindTalkPayload.Open talk;
    private final String text;
    private int typed;
    private boolean answered;

    private MindTalkScreen(MindTalkPayload.Open talk) {
        super(Component.translatable("mind.enhanced.speaker." + talk.speaker()));
        this.talk = talk;
        this.text = Component.translatable("mind.enhanced." + talk.line()).getString();
    }

    /** The server's line: shows it (or, an empty line, closes the talk). */
    public static void open(MindTalkPayload.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.line().isEmpty()) {
            if (mc.screen instanceof MindTalkScreen screen) {
                screen.answered = true;
                screen.onClose();
            }
            return;
        }
        if (mc.screen instanceof MindTalkScreen old) {
            old.answered = true;
        }
        mc.setScreen(new MindTalkScreen(payload));
    }

    private Component optionText(String option) {
        if (talk.line().startsWith("elevator.")) {
            return Component.translatable("mind.enhanced.elevator." + option);
        }
        if (option.equals("bye")) {
            return Component.translatable("mind.enhanced.bye");
        }
        return Component.translatable("mind.enhanced." + talk.line() + "." + option);
    }

    private int panelWidth() {
        return Math.min(400, width - 20);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    /** Tall enough for the whole line. */
    private int panelHeight() {
        return Math.max(PANEL_HEIGHT, 28 + font.split(Component.literal(text), panelWidth() - 16).size() * 10);
    }

    private int panelTop() {
        return height - panelHeight() - 12;
    }

    @Override
    protected void init() {
        List<String> options = talk.options();
        int buttonWidth = Math.min(340, panelWidth() - 20);
        int x = (width - buttonWidth) / 2;
        int y = panelTop() - 6 - options.size() * 24;
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            Component label = Component.literal((i + 1) + ". ").append(optionText(option));
            addRenderableWidget(Button.builder(label, b -> answer(option)).bounds(x, y + i * 24, buttonWidth, 20).build());
        }
    }

    private void answer(String option) {
        if (typed < text.length()) {
            typed = text.length();
            return;
        }
        answered = true;
        Net.sendToServer(new MindTalkPayload.Choose(option));
    }

    @Override
    public void tick() {
        typed = Math.min(text.length(), typed + TYPE_SPEED);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int n = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1;
        if (n >= 0 && n < talk.options().size()) {
            answer(talk.options().get(n));
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && talk.forced()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !talk.forced();
    }

    @Override
    public void removed() {
        if (!answered) {
            Net.sendToServer(new MindTalkPayload.Choose(""));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // the world stays in view
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int left = panelLeft();
        int top = panelTop();
        int w = panelWidth();
        int h = panelHeight();
        g.fill(left, top, left + w, top + h, 0xD0101018);
        g.renderOutline(left, top, w, h, 0xFFE6C35A);
        boolean rtl = Language.getInstance().isDefaultRightToLeft();
        Component name = getTitle();
        int nameX = rtl ? left + w - 8 - font.width(name) : left + 8;
        g.drawString(font, name, nameX, top + 6, 0xFFFFD86A, true);
        String shown = text.substring(0, Math.min(typed, text.length()));
        List<FormattedCharSequence> lines = font.split(Component.literal(shown), w - 16);
        int y = top + 20;
        for (FormattedCharSequence line : lines) {
            int lx = rtl ? left + w - 8 - font.width(line) : left + 8;
            g.drawString(font, line, lx, y, 0xFFF2EEE6, false);
            y += 10;
        }
    }
}

package com.kolodkingal.enhanced.client.screen.editor;

import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.custom.PreviewPuppet;
import com.kolodkingal.enhanced.client.screen.HeroEditorScreen;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.PowerModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * The colour of one power of the hero being made (3.5, the creator lane): opened by the colour button of a power's card
 * (Powers tab) or of a key's box (Keys tab). Swatches, a hue and a brightness slider, a hex code to type, and "Hero
 * colour" (the power's own colours, or the hero's: {@code Pick.color} -1). On the left the editor's 3D preview casts
 * the power again and again, and the colour is drawn round it the way the power draws it (a beam, a shield, a burst, an
 * aura), so the choice is seen before it is kept. Done keeps it, Cancel (or Esc) puts the old colour back.
 */
public final class ColorPickerScreen extends Screen {
    /** Ticks between two casts on the preview. */
    private static final int CAST_EVERY = 50;

    private final HeroEditorScreen editor;
    private final CatalogEntry entry;
    private final int before;
    private int color;
    private int hue;
    private int bright;
    private int ticks;
    private float yaw = -25.0F;
    private boolean typing;

    public ColorPickerScreen(HeroEditorScreen editor, CatalogEntry entry) {
        super(Component.translatable("screen.enhanced.hero_creator.color.title", CreatorUi.name(entry)));
        this.editor = editor;
        this.entry = entry;
        CustomHero.Pick pick = editor.draft().pick(entry);
        this.before = pick == null ? -1 : pick.color();
        this.color = before;
        int[] hb = CreatorUi.hueBright(shown());
        this.hue = hb[0];
        this.bright = hb[1];
    }

    /** Opens the picker for a picked power of the editor's draft (nothing when it is not picked). */
    public static void open(HeroEditorScreen editor, CatalogEntry entry) {
        if (editor.draft().pick(entry) != null) {
            Minecraft.getInstance().setScreen(new ColorPickerScreen(editor, entry));
        }
    }

    /** The colour the power's effects are drawn in now: the one chosen, or the hero's. */
    private int shown() {
        return color < 0 ? editor.draft().color() : color;
    }

    private void set(int rgb) {
        color = rgb < 0 ? -1 : rgb & 0xFFFFFF;
        CustomHero.Pick pick = editor.draft().pick(entry);
        if (pick != null) {
            editor.update(editor.draft().with(pick.withColor(color)));
        }
    }

    // ---- layout ----

    private int left() {
        return Mth.clamp(width / 3, 92, 170);
    }

    @Override
    protected void init() {
        int x = left() + 10;
        int w = width - x - 8;
        int y = 30;
        int size = 14;
        int per = Math.max(1, (w + 3) / (size + 3));
        int[] palette = HeroEditorScreen.PALETTE;
        for (int i = 0; i < palette.length; i++) {
            int c = palette[i];
            addRenderableWidget(new CreatorUi.Swatch(x + (i % per) * (size + 3), y + (i / per) * (size + 3), size, c,
                    chosen -> {
                set(c);
                rebuild();
            }, () -> color));
        }
        y += ((palette.length + per - 1) / per) * (size + 3) + 4;
        addRenderableWidget(new CreatorUi.IntSlider(x, y, w, 18, 0, 359, hue,
                v -> Component.translatable("screen.enhanced.hero_creator.color.hue", v), v -> {
            hue = v;
            set(CreatorUi.hsv(hue, bright));
        }));
        y += 21;
        addRenderableWidget(new CreatorUi.IntSlider(x, y, w, 18, 5, 100, Math.max(5, bright),
                v -> Component.translatable("screen.enhanced.hero_creator.color.brightness", v), v -> {
            bright = v;
            set(CreatorUi.hsv(hue, bright));
        }));
        y += 24;
        EditBox hex = addRenderableWidget(new EditBox(font, x + 1, y, Math.min(80, w / 2), 16,
                Component.translatable("screen.enhanced.hero_creator.color.hex")));
        hex.setMaxLength(7);
        hex.setValue(String.format(Locale.ROOT, "#%06X", shown()));
        hex.setResponder(text -> {
            String t = text.startsWith("#") ? text.substring(1) : text;
            if (t.length() == 6) {
                try {
                    int c = Integer.parseInt(t, 16);
                    if (c != color) {
                        set(c);
                        int[] hb = CreatorUi.hueBright(c);
                        hue = hb[0];
                        bright = hb[1];
                        typing = true;
                        rebuild();
                    }
                } catch (NumberFormatException ignored) {
                    // (not a colour yet: keep typing)
                }
            }
        });
        if (typing) {
            setFocused(hex);
            hex.setFocused(true);
            hex.moveCursorToEnd(false);
        }
        int hw = w - Math.min(80, w / 2) - 6;
        addRenderableWidget(new CreatorUi.BigButton(x + Math.min(80, w / 2) + 6, y - 2, hw, 20,
                Component.translatable("screen.enhanced.hero_creator.color.hero"), null, editor.draft().color(), () -> {
            set(-1);
            int[] hb = CreatorUi.hueBright(shown());
            hue = hb[0];
            bright = hb[1];
            rebuild();
        }).selected(color < 0));
        int by = height - 28;
        int bw = (w - 6) / 2;
        addRenderableWidget(new CreatorUi.BigButton(x, by, bw, 20,
                Component.translatable("screen.enhanced.hero_creator.color.done"), null,
                0x7CFC7C, this::done));
        addRenderableWidget(new CreatorUi.BigButton(x + bw + 6, by, bw, 20,
                Component.translatable("screen.enhanced.hero_creator.color.cancel"),
                null, 0xFF7070, this::cancel));
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private void done() {
        editor.setDraft(editor.draft());
        Minecraft.getInstance().setScreen(editor);
    }

    private void cancel() {
        set(before);
        done();
    }

    @Override
    public void onClose() {
        cancel();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            done();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        // The editor's preview lives on while the picker is open: it ticks, wears the draft, and casts the power again.
        editor.tick();
        if (ticks++ % CAST_EVERY == 0 && editor.puppet() != null) {
            editor.tryEntry(entry);
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (mx < left()) {
            yaw = Mth.wrapDegrees(yaw - (float) dx * 2.0F);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rgb = shown();
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(rgb).withBold(true)), width / 2, 8,
                0xFFFFFF);
        int x = left() + 10;
        int w = width - x - 8;
        // The chosen colour, large, and what it means.
        int sy = height - 66;
        g.fill(x, sy, x + 28, sy + 28, 0xFF000000 | rgb);
        g.renderOutline(x, sy, 28, 28, 0xFFFFFFFF);
        Component what = color < 0 ? Component.translatable("screen.enhanced.hero_creator.color.is_hero")
                : Component.literal(String.format(Locale.ROOT, "#%06X", color));
        g.drawString(font, what.copy().withStyle(ChatFormatting.WHITE), x + 34, sy + 2, 0xFFFFFF, true);
        List<FormattedCharSequence> hint = font.split(Component.translatable("screen.enhanced.hero_creator.color.hint")
                .withStyle(ChatFormatting.GRAY), Math.max(40, w - 34));
        for (int i = 0; i < Math.min(2, hint.size()); i++) {
            g.drawString(font, hint.get(i), x + 34, sy + 12 + i * 9, 0xFFFFFF, true);
        }
        renderPreview(g, partialTick, rgb);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left() + 4, 24, width - 2, height - 2, 0x50000000);
    }

    /** The preview: the puppet casting, and the power's colour drawn round it as the power draws it. */
    private void renderPreview(GuiGraphics g, float partialTick, int rgb) {
        int l = left();
        int top = 24;
        int bottom = height - 8;
        g.fill(2, top, l, bottom, 0x70000000);
        g.renderOutline(2, top, l - 2, bottom - top, 0xFF000000 | CreatorUi.darken(rgb));
        g.drawCenteredString(font,
                Component.translatable("screen.enhanced.hero_creator.color.preview").withStyle(ChatFormatting.GRAY),
                (l + 2) / 2, top + 4, 0xFFFFFF);
        int cx = (l + 2) / 2;
        int feet = bottom - 14;
        float size = Math.min((bottom - top - 30) / 2.6F, (l - 8) / 1.2F);
        float t = (ticks + partialTick) / 20.0F;
        g.enableScissor(3, top + 1, l - 1, bottom - 1);
        castBehind(g, cx, feet, size, t, rgb);
        PreviewPuppet puppet = editor.puppet();
        boolean drawn = puppet != null && puppet.draw(g, cx, feet, size, yaw, partialTick);
        if (!drawn) {
            int scale = Math.max(1, Math.min(4, (bottom - top) / 34));
            CreatorUi.paperDoll(g, CreatorUi.lookOf(editor.draft()), cx - 8 * scale, top + 20, scale);
        }
        castInFront(g, cx, feet, size, t, rgb);
        g.disableScissor();
    }

    /** What the power is like, for its picture: a beam, a shield, a lasting glow, or a burst. */
    private enum Shape {
        BEAM, SHIELD, AURA, BURST
    }

    private Shape shape() {
        CatalogEntry.Family family = entry.family();
        if (family == CatalogEntry.Family.SHIELD || entry == PowerModule.FORCE_FIELD) {
            return Shape.SHIELD;
        }
        if (family == CatalogEntry.Family.BEAM) {
            return Shape.BEAM;
        }
        CustomHero.Pick pick = editor.draft().pick(entry);
        PowerModule.Act act = pick == null ? entry.act("") : pick.act();
        return switch (act) {
            case HOLD -> Shape.BEAM;
            case TOGGLE, NONE -> Shape.AURA;
            default -> entry instanceof HeroAbility a && a.isVariant() && a.act() == PowerModule.Act.HOLD
                    ? Shape.SHIELD : Shape.BURST;
        };
    }

    /** The parts drawn behind the puppet (a dome's back, an aura). */
    private void castBehind(GuiGraphics g, int cx, int feet, float size, float t, int rgb) {
        Shape shape = shape();
        int mid = feet - Math.round(size * 1.0F);
        if (shape == Shape.SHIELD || shape == Shape.AURA) {
            float r = size * (shape == Shape.SHIELD ? 1.25F : 0.75F) * (0.95F + 0.05F * Mth.sin(t * 4.0F));
            disc(g, cx, mid, r, argb(rgb, shape == Shape.SHIELD ? 0.18F : 0.12F));
        }
    }

    /** The parts drawn in front of the puppet (the beam, the dome's rim, the burst's rings and sparks). */
    private void castInFront(GuiGraphics g, int cx, int feet, float size, float t, int rgb) {
        int glow = AbilityColors.glowOf(rgb);
        int mid = feet - Math.round(size * 1.0F);
        int hand = feet - Math.round(size * 1.3F);
        float phase = (t * 20.0F % CAST_EVERY) / CAST_EVERY;
        switch (shape()) {
            case BEAM -> {
                int from = cx + Math.round(size * 0.35F);
                int to = left() - 4;
                float wobble = Mth.sin(t * 9.0F);
                for (int x = from; x < to; x++) {
                    int y = hand + Math.round(Mth.sin((x - from) * 0.25F + t * 12.0F) * 1.2F * wobble);
                    g.fill(x, y - 3, x + 1, y + 4, argb(rgb, 0.45F));
                    g.fill(x, y - 1, x + 1, y + 2, argb(glow, 0.9F));
                }
            }
            case SHIELD -> ring(g, cx, mid, size * 1.25F, 2, argb(glow, 0.8F));
            case AURA -> {
                for (int i = 0; i < 10; i++) {
                    float a = i * Mth.TWO_PI / 10.0F + t * 1.5F;
                    float up = (t * 0.6F + i * 0.1F) % 1.0F;
                    int px = cx + Math.round(Mth.cos(a) * size * 0.45F);
                    int py = feet - Math.round(up * size * 2.0F);
                    g.fill(px - 1, py - 1, px + 2, py + 2, argb(i % 2 == 0 ? rgb : glow, 1.0F - up));
                }
            }
            case BURST -> {
                float k = Math.min(1.0F, phase * 2.5F);
                if (k < 1.0F) {
                    int bx = cx + Math.round(size * 0.35F);
                    ring(g, bx, hand, 3 + k * size * 0.9F, 2, argb(rgb, 1.0F - k));
                    ring(g, bx, hand, 2 + k * size * 0.5F, 1, argb(glow, 1.0F - k));
                    for (int i = 0; i < 8; i++) {
                        float a = i * Mth.TWO_PI / 8.0F;
                        int px = bx + Math.round(Mth.cos(a) * k * size * 1.1F);
                        int py = hand + Math.round(Mth.sin(a) * k * size * 1.1F);
                        g.fill(px - 1, py - 1, px + 2, py + 2, argb(glow, 1.0F - k));
                    }
                }
            }
        }
    }

    private static void ring(GuiGraphics g, int cx, int cy, float r, int thick, int argb) {
        int steps = Math.max(24, Math.round(r * 4));
        for (int i = 0; i < steps; i++) {
            float a = i * Mth.TWO_PI / steps;
            int x = cx + Math.round(Mth.cos(a) * r);
            int y = cy + Math.round(Mth.sin(a) * r);
            g.fill(x, y, x + thick, y + thick, argb);
        }
    }

    private static void disc(GuiGraphics g, int cx, int cy, float r, int argb) {
        int ri = Math.round(r);
        for (int dy = -ri; dy <= ri; dy++) {
            int half = Math.round((float) Math.sqrt(Math.max(0.0F, r * r - dy * dy)));
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, argb);
        }
    }

    private static int argb(int rgb, float alpha) {
        return Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | rgb & 0xFFFFFF;
    }
}

package com.kolodkingal.enhanced.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * The powers panel in its selectable styles (the client option {@code hudStyle}): what each style draws from a
 * {@link Model}, and the small animations they share (a slide in when the panel's owner changes, a nudge on a press,
 * a glint when a cooldown ends, a breathing edge on what is switched on). Everything is drawn with rectangles and
 * text, so it needs no textures; an ability may bring its own icon ({@link Icon}).
 *
 * <p>This file is the same in Enhanced and Enlightened but for its package: it knows neither mod. Each mod builds
 * the {@link Model} from its own data and calls {@link #draw}.
 */
public final class PowerPanels {
    /** The styles a player can choose. {@link #CLASSIC} is the text panel both mods had before; it is drawn by the mod. */
    public enum Style {
        CLASSIC, STACK, DIAL, COMIC, SCROLL, CLUSTER;

        public Style next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Draws an ability's icon in a square of {@code size} pixels at x, y. */
    @FunctionalInterface
    public interface Icon {
        void draw(GuiGraphics graphics, int x, int y, int size);
    }

    /**
     * One ability as the panel shows it.
     *
     * @param name         its name
     * @param status       what the classic panel writes after it (a cooldown, ON, "active"), or null
     * @param cooldownLeft seconds until it can be used again (0: ready)
     * @param on           a switch that is on, or an ability at work now
     * @param level        its level for the stars (-1: no levels)
     */
    public record Slot(Component name, @Nullable Component status, float cooldownLeft, boolean on, int level,
                       int maxLevel, @Nullable Icon icon) {
        /** This ability with an icon. */
        public Slot withIcon(@Nullable Icon icon) {
            return new Slot(name, status, cooldownLeft, on, level, maxLevel, icon);
        }
    }

    /** A key: the ability on it, and the one on it with Sneak (null: none). */
    public record Row(Component key, @Nullable Slot plain, @Nullable Slot sneak) {
        /** The ability to draw for this key now; a key with nothing on it is drawn empty. */
        Slot shown(boolean sneaking) {
            Slot slot = sneaking && sneak != null ? sneak : plain != null ? plain : sneak;
            return slot != null ? slot : EMPTY;
        }
    }

    /** Everything a style draws. */
    public static final class Model {
        /** Changes when the panel belongs to someone or something else (a new hero, a new look): the panel slides in. */
        public String identity = "";
        public Component title = Component.empty();
        /** The owner's colour, 0xRRGGBB. */
        public int color = 0xFFFFFF;
        public final List<Row> rows = new ArrayList<>();
        /** The lines under the abilities (points, a meter). */
        public final List<Component> notes = new ArrayList<>();
        /** Beads lit under the scroll (Enlightened's chakras), of {@link #beadsMax}; -1: none. */
        public int beads = -1;
        public int beadsMax;
        public boolean sneaking;
    }

    /** A key with no ability on it. */
    private static final Slot EMPTY = new Slot(Component.literal("-"), null, 0.0F, false, -1, 0, null);

    private static final int MAX = 32;
    private static final float[] LAST_LEFT = new float[MAX];
    private static final float[] TOTAL = new float[MAX];
    private static final boolean[] LAST_ON = new boolean[MAX];
    private static final long[] PRESS_AT = new long[MAX];
    private static final long[] READY_AT = new long[MAX];
    private static String identity = "";
    private static long shownAt;
    private static long activeAt;
    private static int lastPressed = -1;
    private static boolean lastSneak;
    private static long sneakAt;

    private PowerPanels() {
    }

    /** The slot whose state was watched as {@code row * 2 + (sneak ? 1 : 0)}. */
    private static int id(int row, boolean sneak) {
        return Math.min(MAX - 1, row * 2 + (sneak ? 1 : 0));
    }

    /** Notices what changed since the last frame: a cooldown that began or ended, a switch that flipped. */
    private static void watch(Model model, long now) {
        if (!model.identity.equals(identity)) {
            identity = model.identity;
            shownAt = now;
            activeAt = now;
            lastPressed = -1;
            java.util.Arrays.fill(PRESS_AT, 0L);
            java.util.Arrays.fill(READY_AT, 0L);
            java.util.Arrays.fill(LAST_LEFT, 0.0F);
        }
        if (model.sneaking != lastSneak) {
            lastSneak = model.sneaking;
            sneakAt = now;
        }
        for (int r = 0; r < model.rows.size(); r++) {
            for (int layer = 0; layer < 2; layer++) {
                Slot slot = layer == 0 ? model.rows.get(r).plain : model.rows.get(r).sneak;
                if (slot == null) {
                    continue;
                }
                int i = id(r, layer == 1);
                float left = slot.cooldownLeft;
                if (left > LAST_LEFT[i] + 0.25F) {
                    TOTAL[i] = left;
                    PRESS_AT[i] = now;
                    lastPressed = i;
                    activeAt = now;
                } else if (left <= 0.0F && LAST_LEFT[i] > 0.0F) {
                    READY_AT[i] = now;
                    activeAt = now;
                }
                if (slot.on != LAST_ON[i]) {
                    PRESS_AT[i] = now;
                    lastPressed = i;
                    activeAt = now;
                }
                LAST_LEFT[i] = left;
                LAST_ON[i] = slot.on;
            }
        }
    }

    private static float fraction(int i, Slot slot) {
        return slot.cooldownLeft <= 0.0F || TOTAL[i] <= 0.0F ? 0.0F : Mth.clamp(slot.cooldownLeft / TOTAL[i], 0.0F, 1.0F);
    }

    /** 0 to 1 over {@code millis} since {@code since}, eased out. */
    private static float ease(long now, long since, float millis, boolean calm) {
        if (calm || since == 0L) {
            return 1.0F;
        }
        float t = Mth.clamp((now - since) / millis, 0.0F, 1.0F);
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    /** 1 at {@code since}, falling to 0 over {@code millis}. */
    private static float fade(long now, long since, float millis, boolean calm) {
        if (calm || since == 0L) {
            return 0.0F;
        }
        return 1.0F - Mth.clamp((now - since) / millis, 0.0F, 1.0F);
    }

    private static int alpha(int rgb, float a) {
        return (Mth.clamp(Math.round(a * 255.0F), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int shade(int rgb, float f) {
        int r = Mth.clamp(Math.round(((rgb >> 16) & 0xFF) * f), 0, 255);
        int g = Mth.clamp(Math.round(((rgb >> 8) & 0xFF) * f), 0, 255);
        int b = Mth.clamp(Math.round((rgb & 0xFF) * f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    /** What stands for an ability that has no icon: the first letters of its first two words. */
    private static String initials(Component name) {
        String text = name.getString().trim();
        if (text.isEmpty() || text.equals("-")) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        boolean start = true;
        for (int i = 0; i < text.length() && out.codePointCount(0, out.length()) < 2; ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isLetterOrDigit(cp)) {
                if (cp == '(') {
                    break;
                }
                start = true;
            } else if (start) {
                out.appendCodePoint(cp);
                start = false;
            }
        }
        return out.toString();
    }

    private static String seconds(float left) {
        return left >= 10.0F ? Integer.toString(Mth.ceil(left)) : String.format(java.util.Locale.ROOT, "%.1f", left);
    }

    /**
     * Draws the panel in a new style with its top at {@code top}, against the right edge of the screen (or the left
     * one); returns its height, so that what comes under it knows where to start.
     *
     * @param calm reduced motion: every animation is a plain change of state
     */
    public static int draw(Style style, GuiGraphics graphics, Font font, Model model, int screenWidth, int top,
                           boolean right, boolean calm) {
        long now = Util.getMillis();
        watch(model, now);
        return switch (style) {
            case STACK -> stack(graphics, font, model, screenWidth, top, right, calm, now);
            case DIAL -> dial(graphics, font, model, screenWidth, top, right, calm, now);
            case COMIC -> comic(graphics, font, model, screenWidth, top, right, calm, now);
            case SCROLL -> scroll(graphics, font, model, screenWidth, top, right, calm, now);
            case CLUSTER -> cluster(graphics, font, model, screenWidth, top, right, calm, now);
            case CLASSIC -> 0;
        };
    }

    // ---- shared pieces ----

    /** A key cap: the key's name in a small raised box; returns its width. */
    private static int cap(GuiGraphics g, Font font, Component key, int x, int y, boolean dark) {
        int w = Math.max(9, font.width(key) + 4);
        g.fill(x, y, x + w, y + 10, dark ? 0xFF3A2A14 : 0xFF2A2F3A);
        g.fill(x, y + 9, x + w, y + 10, dark ? 0xFF1E1408 : 0xFF11141A);
        g.drawString(font, key, x + (w - font.width(key)) / 2 + 1, y + 1, dark ? 0xFFF1E2BD : 0xFFFFCF5A, false);
        return w;
    }

    /** A square tile of an ability: its icon (or first letter), its cooldown as a shade from the top, its state. */
    private static void tile(GuiGraphics g, Font font, Model model, int i, Slot slot, @Nullable Component key, int x,
                             int y, int size, boolean calm, long now) {
        float press = fade(now, PRESS_AT[i], 220.0F, calm);
        int grow = Math.round(press * 2.0F);
        int x0 = x - grow;
        int y0 = y - grow;
        int s = size + grow * 2;
        g.fill(x0 + 1, y0 + 1, x0 + s + 1, y0 + s + 1, 0x80000000);
        g.fill(x0, y0, x0 + s, y0 + s, 0xFF0B0D12);
        g.fill(x0 + 1, y0 + 1, x0 + s - 1, y0 + s - 1, 0xFF1D2330);
        int edge = slot.on ? 0xFF6FE08A : alpha(model.color, 0.75F);
        if (slot.on && !calm) {
            edge = alpha(0x6FE08A, 0.6F + 0.4F * Mth.sin(now / 250.0F));
        }
        g.renderOutline(x0 + 1, y0 + 1, s - 2, s - 2, edge);
        if (slot.icon != null) {
            slot.icon.draw(g, x0 + 3, y0 + 3, s - 6);
        } else {
            String letter = initials(slot.name);
            g.drawString(font, letter, x0 + (s - font.width(letter)) / 2 + 1, y0 + (s - 8) / 2, alpha(shade(model.color, 1.25F), 1.0F), true);
        }
        float left = fraction(i, slot);
        if (left > 0.0F) {
            g.fill(x0 + 2, y0 + 2, x0 + s - 2, y0 + 2 + Math.round((s - 4) * left), 0xB0000000);
            String n = seconds(slot.cooldownLeft);
            g.drawString(font, n, x0 + (s - font.width(n)) / 2 + 1, y0 + (s - 8) / 2, 0xFFFFFFFF, true);
        }
        float glint = fade(now, READY_AT[i], 500.0F, calm);
        if (glint > 0.0F) {
            g.fill(x0 + 1, y0 + 1, x0 + s - 1, y0 + s - 1, alpha(0xFFFFFF, glint * 0.7F));
        }
        if (key != null) {
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(x0 + 1.5F, y0 + 1.0F, 0.0F);
            pose.scale(0.5F, 0.5F, 1.0F);
            g.drawString(font, key, 0, 0, 0xFFFFCF5A, true);
            pose.popPose();
        }
    }

    private static int slideOffset(int width, int index, boolean right, boolean calm, long now) {
        float t = ease(now, shownAt + index * 45L, 350.0F, calm);
        if (now < shownAt + index * 45L && !calm) {
            t = 0.0F;
        }
        return Math.round((1.0F - t) * (width + 8)) * (right ? 1 : -1);
    }

    // ---- A: the key stack ----

    private static int stack(GuiGraphics g, Font font, Model model, int sw, int top, boolean right, boolean calm, long now) {
        boolean compact = !calm && now - activeAt > 5000L;
        float fold = compact ? ease(now, activeAt + 5000L, 300.0F, false) : 0.0F;
        int textWidth = 0;
        for (int r = 0; r < model.rows.size(); r++) {
            Slot slot = model.rows.get(r).shown(model.sneaking);
            if (slot != null) {
                int w = font.width(slot.name) + (slot.status != null ? font.width(slot.status) + 8 : 0);
                textWidth = Math.max(textWidth, w);
            }
        }
        int capWidth = 14;
        int full = capWidth + 6 + textWidth + 6;
        int width = Math.round(Mth.lerp(fold, full, capWidth + 4));
        int y = top;
        // the title
        int tw = font.width(model.title) + 10;
        int tx = (right ? sw - 4 - tw : 4) + slideOffset(tw, 0, right, calm, now);
        g.fill(tx, y, tx + tw, y + 12, 0xA0000000);
        g.fill(right ? tx + tw - 2 : tx, y, right ? tx + tw : tx + 2, y + 12, alpha(model.color, 1.0F));
        g.drawString(font, model.title, tx + 5, y + 2, 0xFFFFFFFF, true);
        y += 14;
        for (int r = 0; r < model.rows.size(); r++) {
            Row row = model.rows.get(r);
            Slot slot = row.shown(model.sneaking);
            if (slot == null) {
                continue;
            }
            int i = id(r, slot == row.sneak);
            int nudge = Math.round(fade(now, PRESS_AT[i], 220.0F, calm) * 4.0F) * (right ? -1 : 1);
            int x = (right ? sw - 4 - width : 4) + slideOffset(width, r + 1, right, calm, now) + nudge;
            g.fill(x, y, x + width, y + 12, 0x90000000);
            float left = fraction(i, slot);
            if (left > 0.0F) {
                int fw = Math.round(width * left);
                g.fill(right ? x + width - fw : x, y, right ? x + width : x + fw, y + 12, alpha(model.color, 0.3F));
            }
            if (slot.on) {
                float a = calm ? 1.0F : 0.6F + 0.4F * Mth.sin(now / 250.0F);
                g.fill(right ? x + width - 2 : x, y, right ? x + width : x + 2, y + 12, alpha(0x6FE08A, a));
            }
            // (a right-to-left language reads from the key on the right to the state on the left)
            boolean rtl = font.isBidirectional();
            cap(g, font, row.key, rtl ? x + width - 2 - Math.max(9, font.width(row.key) + 4) : x + 2, y + 1, false);
            if (fold < 0.5F) {
                g.drawString(font, slot.name, rtl ? x + width - capWidth - 6 - font.width(slot.name) : x + capWidth + 6, y + 2, 0xFFFFFFFF, true);
                if (slot.status != null) {
                    g.drawString(font, slot.status, rtl ? x + 4 : x + width - 4 - font.width(slot.status), y + 2, 0xFFFFFFFF, true);
                }
            }
            float glint = fade(now, READY_AT[i], 500.0F, calm);
            if (glint > 0.0F) {
                g.fill(x, y, x + width, y + 12, alpha(0xFFFFFF, glint * 0.6F));
            }
            y += 13;
        }
        y = notes(g, font, model, sw, y, right, fold < 0.5F);
        return y - top;
    }

    private static int notes(GuiGraphics g, Font font, Model model, int sw, int y, boolean right, boolean show) {
        if (!show) {
            return y;
        }
        for (Component note : model.notes) {
            int w = font.width(note) + 6;
            int x = right ? sw - 4 - w : 4;
            g.fill(x, y, x + w, y + 11, 0x70000000);
            g.drawString(font, note, x + 3, y + 2, 0xFFFFFFFF, true);
            y += 11;
        }
        return y;
    }

    // ---- B: the dial ----

    private static int dial(GuiGraphics g, Font font, Model model, int sw, int top, boolean right, boolean calm, long now) {
        int core = 24;
        int size = 18;
        int radius = 58;
        int ox = right ? sw - 4 - core / 2 : 4 + core / 2;
        int oy = top + core / 2;
        float in = ease(now, shownAt, 400.0F, calm);
        int n = model.rows.size();
        for (int r = 0; r < n; r++) {
            Row row = model.rows.get(r);
            Slot slot = row.shown(model.sneaking);
            if (slot == null) {
                continue;
            }
            double angle = Math.toRadians(4.0 + (n <= 1 ? 0.0 : r * 82.0 / (n - 1)));
            int dx = (int) Math.round(Math.cos(angle) * radius * in);
            int dy = (int) Math.round(Math.sin(angle) * radius * in);
            int x = (right ? ox - dx : ox + dx) - size / 2;
            int y = oy + dy - size / 2;
            tile(g, font, model, id(r, slot == row.sneak), slot, row.key, x, y, size, calm, now);
        }
        // the emblem
        g.fill(ox - core / 2, oy - core / 2, ox + core / 2, oy + core / 2, 0xFF0B0D12);
        g.fill(ox - core / 2 + 2, oy - core / 2 + 2, ox + core / 2 - 2, oy + core / 2 - 2, alpha(model.color, 1.0F));
        g.fill(ox - core / 2 + 2, oy, ox + core / 2 - 2, oy + core / 2 - 2, alpha(shade(model.color, 0.75F), 1.0F));
        String letter = model.title.getString();
        letter = letter.isEmpty() ? "?" : letter.substring(0, letter.offsetByCodePoints(0, 1));
        g.drawString(font, letter, ox - font.width(letter) / 2, oy - 4, 0xFF10131A, false);
        // the name of what was pressed last, for two seconds
        int height = core + radius - core / 2 + size / 2 + 2;
        if (lastPressed >= 0 && lastPressed / 2 < n && now - Math.max(PRESS_AT[lastPressed], 1L) < 2000L) {
            Row row = model.rows.get(lastPressed / 2);
            Slot slot = lastPressed % 2 == 1 ? row.sneak : row.plain;
            if (slot != null) {
                int w = font.width(slot.name) + 6;
                int x = right ? sw - 4 - w : 4;
                g.fill(x, top + height, x + w, top + height + 11, 0xA0000000);
                g.drawString(font, slot.name, x + 3, top + height + 2, 0xFFFFFFFF, true);
            }
        }
        height += 12;
        return notes(g, font, model, sw, top + height, right, true) - top;
    }

    // ---- C: the comic panels ----

    private static int comic(GuiGraphics g, Font font, Model model, int sw, int top, boolean right, boolean calm, long now) {
        int ink = 0xFF0D1016;
        int paper = 0xFFF5F0DF;
        PoseStack pose = g.pose();
        // The whole strip leans like a comic panel: x moves with y.
        pose.pushPose();
        int pivotX = right ? sw - 6 : 6;
        pose.translate(pivotX, top, 0.0F);
        pose.mulPose(new Matrix4f().m10(-0.18F));
        pose.translate(-pivotX, -top, 0.0F);
        String name = model.title.getString().toUpperCase(java.util.Locale.ROOT);
        int bw = font.width(name) + 12;
        int bx = (right ? sw - 8 - bw : 8) + slideOffset(bw, 0, right, calm, now);
        g.fill(bx + 2, top + 2, bx + bw + 2, top + 15, ink);
        g.fill(bx, top, bx + bw, top + 13, ink);
        g.fill(bx + 1, top + 1, bx + bw - 1, top + 12, alpha(model.color, 1.0F));
        g.drawString(font, name, bx + 6, top + 3, ink, false);
        int cw = 20;
        int ch = 24;
        int n = model.rows.size();
        int total = n * (cw + 2) - 2;
        int y = top + 17;
        for (int r = 0; r < n; r++) {
            Row row = model.rows.get(r);
            Slot slot = row.shown(model.sneaking);
            if (slot == null) {
                continue;
            }
            int i = id(r, slot == row.sneak);
            int grow = Math.round(fade(now, PRESS_AT[i], 220.0F, calm) * 2.0F);
            int x = (right ? sw - 8 - total : 8) + r * (cw + 2) + slideOffset(total, r + 1, right, calm, now);
            g.fill(x + 2 - grow, y + 2 - grow, x + cw + 2 + grow, y + ch + 2 + grow, ink);
            g.fill(x - grow, y - grow, x + cw + grow, y + ch + grow, slot.on ? 0xFF6FE08A : ink);
            g.fill(x + 1 - grow, y + 1 - grow, x + cw - 1 + grow, y + ch - 1 + grow, paper);
            float left = fraction(i, slot);
            int fillTop = y + 1 + Math.round((ch - 2) * left);
            g.fill(x + 1 - grow, fillTop, x + cw - 1 + grow, y + ch - 1 + grow, alpha(model.color, 1.0F));
            // halftone dots over the ink
            for (int dy = fillTop + 1; dy < y + ch - 2; dy += 3) {
                for (int dx = x + 2 + ((dy / 3) & 1); dx < x + cw - 2; dx += 3) {
                    g.fill(dx, dy, dx + 1, dy + 1, 0x300D1016);
                }
            }
            if (slot.icon != null) {
                slot.icon.draw(g, x + 4, y + 7, cw - 8);
            } else {
                String letter = initials(slot.name);
                g.drawString(font, letter, x + (cw - font.width(letter)) / 2 + 1, y + 10, ink, false);
            }
            pose.pushPose();
            pose.translate(x + 2.0F, y + 2.0F, 0.0F);
            pose.scale(0.5F, 0.5F, 1.0F);
            g.drawString(font, row.key, 0, 0, ink, false);
            pose.popPose();
            if (left > 0.0F) {
                String s = seconds(slot.cooldownLeft);
                g.drawString(font, s, x + cw - 2 - font.width(s), y + ch - 9, ink, false);
            }
        }
        y += ch + 4;
        pose.popPose();
        // the caption: what was used last
        if (lastPressed >= 0 && lastPressed / 2 < n && now - Math.max(PRESS_AT[lastPressed], 1L) < 2500L) {
            Row row = model.rows.get(lastPressed / 2);
            Slot slot = lastPressed % 2 == 1 ? row.sneak : row.plain;
            if (slot != null) {
                String text = slot.name.getString() + (slot.status != null ? "  " + slot.status.getString() : "");
                int w = font.width(text) + 6;
                int x = right ? sw - 8 - w : 8;
                g.fill(x - 1, y - 1, x + w + 1, y + 11, ink);
                g.fill(x, y, x + w, y + 10, paper);
                g.drawString(font, text, x + 3, y + 1, ink, false);
            }
        }
        y += 12;
        return notes(g, font, model, sw, y, right, true) - top;
    }

    // ---- D: the scroll ----

    private static int scroll(GuiGraphics g, Font font, Model model, int sw, int top, boolean right, boolean calm, long now) {
        int inkColor = 0xFF3A2A14;
        int paperTop = 0xFFF1E2BD;
        int paperBottom = 0xFFE2CC98;
        int rowH = 12;
        int textWidth = font.width(model.title) + 30;
        for (int r = 0; r < model.rows.size(); r++) {
            Slot slot = model.rows.get(r).shown(model.sneaking);
            if (slot != null) {
                int w = 16 + font.width(slot.name.getString()) + 6
                        + (slot.level >= 0 ? slot.maxLevel * 5 + 4 : 0)
                        + (slot.status != null ? font.width(slot.status.getString()) + 6 : 0);
                textWidth = Math.max(textWidth, w);
            }
        }
        for (Component note : model.notes) {
            textWidth = Math.max(textWidth, font.width(note.getString()));
        }
        int width = textWidth + 10;
        int bodyH = 16 + model.rows.size() * rowH + 3 + (model.beads >= 0 ? 10 : 0) + model.notes.size() * 10 + 2;
        float open = ease(now, shownAt, 650.0F, calm);
        int shown = Math.round(bodyH * open);
        int x = right ? sw - 8 - width : 8;
        int y = top;
        rod(g, x - 3, y, width + 6);
        y += 4;
        g.enableScissor(x - 4, y, x + width + 4, y + shown);
        g.fillGradient(x, y, x + width, y + bodyH, paperTop, paperBottom);
        g.fill(x + width, y + 1, x + width + 1, y + bodyH, 0x60000000);
        // the seal and the title
        // (a right-to-left language is laid out from the right: the seal, the keys and the names there, the states left)
        boolean rtl = font.isBidirectional();
        int sealX = rtl ? x + width - 14 : x + 3;
        g.fill(sealX, y + 2, sealX + 11, y + 13, alpha(model.color, 1.0F));
        g.renderOutline(sealX, y + 2, 11, 11, alpha(shade(model.color, 0.6F), 1.0F));
        String seal = model.title.getString();
        seal = seal.isEmpty() ? "?" : seal.substring(0, seal.offsetByCodePoints(0, 1));
        g.drawString(font, seal, sealX + 6 - font.width(seal) / 2, y + 4, 0xFFFFFFFF, false);
        String titleText = model.title.getString();
        g.drawString(font, titleText, rtl ? sealX - 4 - font.width(titleText) : x + 18, y + 4, inkColor, false);
        int ry = y + 16;
        float flip = calm ? 1.0F : Mth.clamp((now - sneakAt) / 180.0F, 0.0F, 1.0F);
        for (int r = 0; r < model.rows.size(); r++) {
            Row row = model.rows.get(r);
            Slot slot = row.shown(model.sneaking);
            if (slot == null) {
                ry += rowH;
                continue;
            }
            int i = id(r, slot == row.sneak);
            int nudge = Math.round(fade(now, PRESS_AT[i], 220.0F, calm) * 3.0F) * (right ? -1 : 1);
            int kw = Math.max(9, font.width(row.key) + 4);
            int rx = (rtl ? x + width - 3 - kw : x + 3) + nudge;
            g.fill(x + 3, ry + rowH - 1, x + width - 3, ry + rowH, 0x303A2A14);
            cap(g, font, row.key, rx, ry, true);
            // a line turning over to its Sneak ability is squeezed flat and opens again
            float squeeze = Math.abs(flip * 2.0F - 1.0F);
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(0.0F, ry + 5.0F, 0.0F);
            pose.scale(1.0F, Math.max(0.1F, squeeze), 1.0F);
            pose.translate(0.0F, -(ry + 5.0F), 0.0F);
            int nameColor = slot.on ? alpha(shade(model.color, 0.8F), 1.0F) : inkColor;
            String nameText = slot.name.getString();
            int dir = rtl ? -1 : 1;
            int tx = rtl ? rx - 3 : rx + kw + 3;
            if (slot.icon != null) {
                slot.icon.draw(g, rtl ? tx - 10 : tx, ry, 10);
                tx += 12 * dir;
            }
            g.drawString(font, nameText, rtl ? tx - font.width(nameText) : tx, ry + 1, nameColor, false);
            tx += (font.width(nameText) + 4) * dir;
            if (slot.level >= 0) {
                for (int s = 0; s < slot.maxLevel; s++) {
                    int sx = rtl ? tx - s * 5 - 4 : tx + s * 5;
                    g.fill(sx, ry + 3, sx + 4, ry + 7, s < slot.level ? 0xFFA8741A : 0x40A8741A);
                }
            }
            if (slot.status != null) {
                String status = slot.status.getString();
                g.drawString(font, status, rtl ? x + 4 : x + width - 4 - font.width(status), ry + 1, slot.on ? 0xFF2E7D32 : 0xFF6B5A3A, false);
            }
            pose.popPose();
            // the brush stroke: the cooldown, wiped away
            float left = fraction(i, slot);
            if (left > 0.0F) {
                int bw = Math.round((width - 6) * left);
                g.fill(right ? x + width - 3 - bw : x + 3, ry + rowH - 2, right ? x + width - 3 : x + 3 + bw, ry + rowH, alpha(model.color, 0.9F));
            }
            float glint = fade(now, READY_AT[i], 500.0F, calm);
            if (glint > 0.0F) {
                g.fill(x + 1, ry, x + width - 1, ry + rowH, alpha(0xFFFFFF, glint * 0.6F));
            }
            ry += rowH;
        }
        ry += 3;
        if (model.beads >= 0) {
            for (int b = 0; b < model.beadsMax; b++) {
                int bx = x + 4 + b * 8;
                g.fill(bx, ry, bx + 6, ry + 6, inkColor);
                g.fill(bx + 1, ry + 1, bx + 5, ry + 5, b < model.beads ? alpha(model.color, 1.0F) : paperBottom);
            }
            ry += 10;
        }
        for (Component note : model.notes) {
            String text = note.getString();
            g.drawString(font, text, rtl ? x + width - 4 - font.width(text) : x + 4, ry, 0xFF5A4424, false);
            ry += 10;
        }
        g.disableScissor();
        rod(g, x - 3, y + shown, width + 6);
        return 4 + shown + 4;
    }

    private static void rod(GuiGraphics g, int x, int y, int width) {
        g.fill(x, y, x + width, y + 4, 0xFF5B3A18);
        g.fill(x, y, x + width, y + 2, 0xFF8A5A2B);
        g.fill(x, y + 3, x + width, y + 4, 0xFF3A2410);
    }

    // ---- E: the cluster ----

    private static int cluster(GuiGraphics g, Font font, Model model, int sw, int top, boolean right, boolean calm, long now) {
        int size = 20;
        int n = model.rows.size();
        // bricks: rows of two, one, two, ... each row half a tile to the side of the last
        int perRow = 2;
        int y = top;
        int r = 0;
        int line = 0;
        while (r < n) {
            int count = line % 2 == 0 ? perRow : perRow - 1;
            count = Math.min(count, n - r);
            int rowWidth = count * (size + 1) - 1;
            int fullWidth = perRow * (size + 1) - 1;
            int x0 = (right ? sw - 4 - fullWidth : 4) + (fullWidth - rowWidth) / 2;
            for (int c = 0; c < count; c++, r++) {
                Row row = model.rows.get(r);
                Slot slot = row.shown(model.sneaking);
                if (slot == null) {
                    continue;
                }
                int x = x0 + c * (size + 1) + slideOffset(fullWidth, r, right, calm, now);
                tile(g, font, model, id(r, slot == row.sneak), slot, row.key, x, y, size, calm, now);
            }
            y += size + 1;
            line++;
        }
        return notes(g, font, model, sw, y + 2, right, true) - top;
    }
}

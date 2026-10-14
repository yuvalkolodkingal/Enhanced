package com.kolodkingal.enhanced.client.build;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildHints;
import com.kolodkingal.enhanced.build.Spell;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.SubStringSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The small things Build Magic draws on the screen (D-B 6.3 to 6.7), shared by the badge, the hint strip and the tips:
 * the pictures of an input (a mouse with a button lit, the wheel, a key cap with the key's own name, an eye), a spell's
 * picture and a block's, rings and thin lines in any direction, and text in the reading order of the language.
 *
 * <p><b>Right to left.</b> Minecraft orders a line of a right-to-left language (Hebrew) by its first strong letter:
 * a line that begins with a key's Latin name ("%s מחזיר אחורה", "Ctrl + Z takes it back") would be laid out left to
 * right, its Hebrew read last. {@link #visual} orders every line of a right-to-left language as a right-to-left
 * paragraph, as the language itself is read, whatever letter comes first; numbers, sizes and key names keep their own
 * order inside it.
 *
 * <p>Only ever loaded on a client.
 */
final class HudDraw {
    /** The size of a mouse picture. */
    static final int MOUSE_W = 9;
    static final int MOUSE_H = 12;
    /** The height of a key cap (its width follows the key's name). */
    static final int KEY_H = 12;
    /** How long a texture's presence is trusted before it is looked up again (a resource pack may bring it). */
    private static final int TEXTURE_RECHECK_TICKS = 200;

    private static final int OUTLINE = 0xE0E0E0;
    private static final int BODY = 0x2A2A2A;
    private static final int KEY_EDGE = 0x9A9A9A;
    private static final int KEY_FACE = 0x343434;
    private static final int KEY_SHADE = 0x161616;

    /** Which ART pictures are there ({@code textures/gui/build/<spell>.png}), and when that was last looked at. */
    private static final Map<ResourceLocation, Boolean> TEXTURES = new HashMap<>();
    private static long texturesAt = Long.MIN_VALUE / 2;

    /** The picture of an input, in a hint row or a tip. */
    enum Picture {
        LEFT, LEFT_TWICE, RIGHT, RIGHT_HELD, WHEEL, MIDDLE, MORE_WHEEL, LOOK, MAGIC_KEY, UNDO_KEY, FLIP_KEY;

        /** The picture of a hint row's input. */
        static Picture of(BuildHints.Input input) {
            return switch (input) {
                case LEFT -> LEFT;
                case LEFT_TWICE -> LEFT_TWICE;
                case RIGHT -> RIGHT;
                case RIGHT_HELD -> RIGHT_HELD;
                case WHEEL -> WHEEL;
                case MORE_WHEEL -> MORE_WHEEL;
                case LOOK -> LOOK;
                case MAGIC_KEY -> MAGIC_KEY;
                case UNDO_KEY -> UNDO_KEY;
                case FLIP_KEY -> FLIP_KEY;
            };
        }

        /** The key whose name the picture shows (null for the mouse and the eye). */
        KeyMapping key() {
            return switch (this) {
                case MORE_WHEEL -> BuildKeys.MORE;
                case MAGIC_KEY -> BuildKeys.MAGIC;
                case UNDO_KEY -> BuildKeys.UNDO;
                case FLIP_KEY -> BuildKeys.FLIP;
                default -> null;
            };
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Text

    /** Whether the language is read from right to left (Hebrew). */
    static boolean rtl() {
        return Language.getInstance().isDefaultRightToLeft();
    }

    /**
     * A line in the order it is drawn: as Minecraft orders it in a left-to-right language; in a right-to-left one, as a
     * right-to-left paragraph whatever its first letter (the bidi of {@code FormattedBidiReorder}, with the paragraph's
     * direction given rather than guessed).
     */
    static FormattedCharSequence visual(FormattedText text) {
        Language language = Language.getInstance();
        if (!language.isDefaultRightToLeft()) {
            return language.getVisualOrder(text);
        }
        SubStringSource source = SubStringSource.create(text, UCharacter::getMirror, HudDraw::shape);
        Bidi bidi = new Bidi(source.getPlainText(), Bidi.DIRECTION_RIGHT_TO_LEFT);
        bidi.setReorderingMode(Bidi.REORDER_DEFAULT);
        List<FormattedCharSequence> runs = new ArrayList<>();
        for (int i = 0; i < bidi.countRuns(); i++) {
            BidiRun run = bidi.getVisualRun(i);
            runs.addAll(source.substring(run.getStart(), run.getLength(), run.isOddRun()));
        }
        return FormattedCharSequence.composite(runs);
    }

    private static String shape(String text) {
        try {
            return new ArabicShaping(ArabicShaping.LETTERS_SHAPE).shape(text);
        } catch (Exception odd) {
            return text;
        }
    }

    /** A whole number with separators: {@code 1,240,000}. */
    static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /**
     * Draws a line ordered by {@link #visual} with its left edge at {@code x}, in {@code argb} (nothing when it is
     * almost transparent: the font draws an alpha below 4 opaque). Returns its width.
     */
    static int text(GuiGraphics g, Font font, FormattedCharSequence line, int x, int y, int argb, boolean shadow) {
        if ((argb >>> 24) >= 8) {
            g.drawString(font, line, x, y, argb, shadow);
        }
        return font.width(line);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Shapes

    /** {@code rgb} with {@code alpha} (0 to 1). */
    static int argb(int rgb, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** {@code argb} with its alpha scaled by {@code fade}. */
    static int faded(int argb, float fade) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(fade, 0.0F, 1.0F));
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** A rectangle in {@code rgb} at {@code alpha}. */
    static void box(GuiGraphics g, int x0, int y0, int x1, int y1, int rgb, float alpha) {
        if (alpha > 0.0F && x1 > x0 && y1 > y0) {
            g.fill(x0, y0, x1, y1, argb(rgb, alpha));
        }
    }

    /**
     * A quad in GUI coordinates, each corner in its own colour; wound the way the GUI's culling keeps (the corners may
     * come in either order around it).
     */
    static void quad(VertexConsumer vc, Matrix4f pose, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                     int c0, int c1, int c2, int c3) {
        float area = (x0 * y1 - x1 * y0) + (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3);
        if (area > 0.0F) {
            vc.addVertex(pose, x3, y3, 0.0F).setColor(c3);
            vc.addVertex(pose, x2, y2, 0.0F).setColor(c2);
            vc.addVertex(pose, x1, y1, 0.0F).setColor(c1);
            vc.addVertex(pose, x0, y0, 0.0F).setColor(c0);
        } else {
            vc.addVertex(pose, x0, y0, 0.0F).setColor(c0);
            vc.addVertex(pose, x1, y1, 0.0F).setColor(c1);
            vc.addVertex(pose, x2, y2, 0.0F).setColor(c2);
            vc.addVertex(pose, x3, y3, 0.0F).setColor(c3);
        }
    }

    /**
     * Part of a ring round ({@code cx}, {@code cy}) between two radii, from {@code from} to {@code to} turns (0 at the
     * top, clockwise), in {@code argb}.
     */
    static void ring(GuiGraphics g, float cx, float cy, float inner, float outer, float from, float to, int argb) {
        if (to <= from || (argb >>> 24) == 0) {
            return;
        }
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f pose = g.pose().last().pose();
        int steps = Math.max(2, Mth.ceil((to - from) * 48.0F));
        for (int i = 0; i < steps; i++) {
            double a0 = Math.PI * 2.0 * (from + (to - from) * i / steps);
            double a1 = Math.PI * 2.0 * (from + (to - from) * (i + 1) / steps);
            float s0 = (float) Math.sin(a0);
            float c0 = (float) -Math.cos(a0);
            float s1 = (float) Math.sin(a1);
            float c1 = (float) -Math.cos(a1);
            quad(vc, pose, cx + s0 * inner, cy + c0 * inner, cx + s0 * outer, cy + c0 * outer, cx + s1 * outer, cy + c1 * outer,
                    cx + s1 * inner, cy + c1 * inner, argb, argb, argb, argb);
        }
        g.flush();
    }

    /** A straight line {@code width} wide from one point to another (not flushed: call {@link GuiGraphics#flush}). */
    static void line(GuiGraphics g, float x0, float y0, float x1, float y1, float width, int argb) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            return;
        }
        float nx = -dy / length * width / 2.0F;
        float ny = dx / length * width / 2.0F;
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        quad(vc, g.pose().last().pose(), x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, argb, argb, argb, argb);
    }

    /** A rectangle whose colour runs from {@code left} at its left edge to {@code right} at its right edge. */
    static void sideways(GuiGraphics g, float x0, float y0, float x1, float y1, int left, int right) {
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        quad(vc, g.pose().last().pose(), x0, y0, x0, y1, x1, y1, x1, y0, left, left, right, right);
        g.flush();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The pictures of the inputs

    /** How wide {@code picture} is drawn. */
    static int width(Font font, Picture picture) {
        return switch (picture) {
            case LEFT, RIGHT, MIDDLE -> MOUSE_W;
            case LEFT_TWICE -> MOUSE_W + 1 + font.width("×2");
            case RIGHT_HELD -> MOUSE_W + 8;
            case WHEEL -> MOUSE_W + 7;
            case MORE_WHEEL -> keyWidth(font, BuildKeys.MORE) + 3 + font.width("+") + 3 + MOUSE_W + 7;
            case LOOK -> 11;
            case MAGIC_KEY, UNDO_KEY, FLIP_KEY -> keyWidth(font, picture.key());
        };
    }

    /**
     * Draws {@code picture} with its top left at ({@code x}, {@code y}), 12 pixels high: the mouse with a button lit in
     * {@code lit} (or not, {@code on} false, while a tip blinks it), the key caps with the keys' own names ({@code
     * pressed}: lit and pushed down, as a tip blinks them), the eye. {@code fade} scales every alpha.
     */
    static void picture(GuiGraphics g, Font font, Picture picture, int x, int y, int lit, boolean on, boolean pressed, float fade) {
        switch (picture) {
            case LEFT -> mouse(g, x, y, Button.LEFT, lit, on, fade);
            case RIGHT -> mouse(g, x, y, Button.RIGHT, lit, on, fade);
            case MIDDLE -> mouse(g, x, y, Button.MIDDLE, lit, on, fade);
            case LEFT_TWICE -> {
                mouse(g, x, y, Button.LEFT, lit, on, fade);
                // Part of the picture: never reordered.
                text(g, font, FormattedCharSequence.forward("×2", Style.EMPTY), x + MOUSE_W + 1, y + 4, faded(0xFFFFFFFF, fade), true);
            }
            case RIGHT_HELD -> {
                mouse(g, x, y, Button.RIGHT, lit, on, fade);
                // A small ring three quarters full: hold until the ring fills.
                ring(g, x + MOUSE_W + 4.5F, y + 5.5F, 1.5F, 3.5F, 0.0F, 1.0F, faded(argb(BODY, 0.9F), fade));
                ring(g, x + MOUSE_W + 4.5F, y + 5.5F, 1.5F, 3.5F, 0.0F, on ? 0.75F : 0.4F, faded(argb(lit, 1.0F), fade));
            }
            case WHEEL -> {
                mouse(g, x, y, Button.MIDDLE, lit, on, fade);
                arrows(g, x + MOUSE_W + 2, y + 1, faded(0xFFFFFFFF, fade));
            }
            case MORE_WHEEL -> {
                int key = keyCap(g, font, BuildKeys.MORE, x, y, pressed, lit, fade);
                int plus = x + key + 3;
                text(g, font, FormattedCharSequence.forward("+", Style.EMPTY), plus, y + 2, faded(0xFFFFFFFF, fade), true);
                int at = plus + font.width("+") + 3;
                mouse(g, at, y, Button.MIDDLE, lit, on, fade);
                arrows(g, at + MOUSE_W + 2, y + 1, faded(0xFFFFFFFF, fade));
            }
            case LOOK -> eye(g, x, y + 2, lit, fade);
            case MAGIC_KEY, UNDO_KEY, FLIP_KEY -> keyCap(g, font, picture.key(), x, y, pressed, lit, fade);
        }
    }

    private enum Button { LEFT, RIGHT, MIDDLE }

    /** A mouse, 9 by 12 pixels: a rounded body, two buttons and the wheel, one of them lit. */
    private static void mouse(GuiGraphics g, int x, int y, Button button, int lit, boolean on, float fade) {
        int edge = faded(argb(OUTLINE, 1.0F), fade);
        int body = faded(argb(BODY, 0.95F), fade);
        int glow = faded(argb(lit, 1.0F), fade);
        // The body, its corners cut.
        g.fill(x + 1, y + 1, x + 8, y + 11, body);
        // The outline.
        g.fill(x + 2, y, x + 7, y + 1, edge);
        g.fill(x + 2, y + 11, x + 7, y + 12, edge);
        g.fill(x, y + 2, x + 1, y + 10, edge);
        g.fill(x + 8, y + 2, x + 9, y + 10, edge);
        g.fill(x + 1, y + 1, x + 2, y + 2, edge);
        g.fill(x + 7, y + 1, x + 8, y + 2, edge);
        g.fill(x + 1, y + 10, x + 2, y + 11, edge);
        g.fill(x + 7, y + 10, x + 8, y + 11, edge);
        // The buttons' line, and between them the wheel.
        g.fill(x + 1, y + 5, x + 8, y + 6, edge);
        if (on && button == Button.LEFT) {
            g.fill(x + 2, y + 1, x + 4, y + 2, glow);
            g.fill(x + 1, y + 2, x + 4, y + 5, glow);
        }
        if (on && button == Button.RIGHT) {
            g.fill(x + 5, y + 1, x + 7, y + 2, glow);
            g.fill(x + 5, y + 2, x + 8, y + 5, glow);
        }
        g.fill(x + 4, y + 1, x + 5, y + 5, on && button == Button.MIDDLE ? glow : edge);
    }

    /** Two small arrows, up and down, 5 pixels wide, beside a mouse whose wheel turns. */
    private static void arrows(GuiGraphics g, int x, int y, int argb) {
        g.fill(x + 2, y, x + 3, y + 1, argb);
        g.fill(x + 1, y + 1, x + 4, y + 2, argb);
        g.fill(x, y + 2, x + 5, y + 3, argb);
        g.fill(x + 2, y + 3, x + 3, y + 7, argb);
        g.fill(x, y + 7, x + 5, y + 8, argb);
        g.fill(x + 1, y + 8, x + 4, y + 9, argb);
        g.fill(x + 2, y + 9, x + 3, y + 10, argb);
    }

    /** An eye, 11 by 7 pixels, its iris in {@code lit}: where the builder looks. */
    private static void eye(GuiGraphics g, int x, int y, int lit, float fade) {
        int edge = faded(argb(OUTLINE, 1.0F), fade);
        int white = faded(argb(0xFFFFFF, 0.85F), fade);
        int iris = faded(argb(lit, 1.0F), fade);
        g.fill(x + 3, y, x + 8, y + 1, edge);
        g.fill(x + 1, y + 1, x + 3, y + 2, edge);
        g.fill(x + 8, y + 1, x + 10, y + 2, edge);
        g.fill(x, y + 2, x + 1, y + 5, edge);
        g.fill(x + 10, y + 2, x + 11, y + 5, edge);
        g.fill(x + 1, y + 5, x + 3, y + 6, edge);
        g.fill(x + 8, y + 5, x + 10, y + 6, edge);
        g.fill(x + 3, y + 6, x + 8, y + 7, edge);
        g.fill(x + 3, y + 1, x + 8, y + 6, white);
        g.fill(x + 1, y + 2, x + 10, y + 5, white);
        g.fill(x + 4, y + 2, x + 7, y + 5, iris);
        g.fill(x + 5, y + 3, x + 6, y + 4, faded(0xFF000000, fade));
    }

    /** How wide the cap of {@code key} is: its name and a margin. */
    static int keyWidth(Font font, KeyMapping key) {
        return font.width(visual(key.getTranslatedKeyMessage())) + 6;
    }

    /**
     * A key cap with the key's own name (a rebound key shows its new name), 12 pixels high; pressed (lit, one pixel
     * lower) while a tip blinks it. Returns its width.
     */
    static int keyCap(GuiGraphics g, Font font, KeyMapping key, int x, int y, boolean pressed, int lit, float fade) {
        FormattedCharSequence name = visual(key.getTranslatedKeyMessage());
        int w = font.width(name) + 6;
        int down = pressed ? 1 : 0;
        g.fill(x, y + down, x + w, y + KEY_H, faded(argb(KEY_EDGE, 1.0F), fade));
        g.fill(x + 1, y + 1 + down, x + w - 1, y + KEY_H - 1, faded(pressed ? argb(darker(lit), 1.0F) : argb(KEY_FACE, 1.0F), fade));
        g.fill(x + 1, y + KEY_H - 2, x + w - 1, y + KEY_H - 1, faded(argb(KEY_SHADE, 1.0F), fade));
        text(g, font, name, x + 3, y + 2 + down, faded(0xFFFFFFFF, fade), false);
        return w;
    }

    /** {@code rgb} at half its brightness. */
    static int darker(int rgb) {
        return ((rgb >> 1) & 0x7F7F7F);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Spells and blocks

    /**
     * A spell's picture, 16 pixels: ART's {@code textures/gui/build/<spell>.png} once it is there, until then a
     * vanilla item that stands in for it (D-B 6.5: bricks, a feather, a brush, a painting, a piston, a snowball, a
     * filled map, a minecart, a chain, a grass block, a flowering azalea).
     */
    static void spell(GuiGraphics g, Spell spell, int x, int y) {
        ResourceLocation texture = Enhanced.id("textures/gui/build/" + spell.name().toLowerCase(Locale.ROOT) + ".png");
        if (present(texture)) {
            g.blit(texture, x, y, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
            return;
        }
        g.renderItem(new ItemStack(standIn(spell)), x, y);
    }

    /** The vanilla item that stands in for a spell's picture. */
    static Item standIn(Spell spell) {
        return switch (spell) {
            case FILL -> Items.BRICKS;
            case VANISH -> Items.FEATHER;
            case CHANGE -> Items.BRUSH;
            case PAINT -> Items.PAINTING;
            case PULL -> Items.PISTON;
            case SHAPE -> Items.SNOWBALL;
            case COPY -> Items.FILLED_MAP;
            case MOVE -> Items.MINECART;
            case REPEAT -> Items.CHAIN;
            case LAND -> Items.GRASS_BLOCK;
            case GROW -> Items.FLOWERING_AZALEA;
        };
    }

    /** The item that shows a block: its own, a fluid's bucket, or nothing (empty) for a block no item shows. */
    static ItemStack item(BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                item = fluid.getType().getBucket();
            }
        }
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Whether a texture is there (looked up again every {@value #TEXTURE_RECHECK_TICKS} ticks). */
    private static boolean present(ResourceLocation texture) {
        long now = BuildClient.now();
        if (now - texturesAt > TEXTURE_RECHECK_TICKS || now < texturesAt) {
            TEXTURES.clear();
            texturesAt = now;
        }
        return TEXTURES.computeIfAbsent(texture, t -> Minecraft.getInstance().getResourceManager().getResource(t).isPresent());
    }

    private HudDraw() {
    }
}

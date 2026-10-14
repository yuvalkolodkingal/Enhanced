package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildGrammar;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.build.ShapeKind;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Spell Wheel (D-B 6.5, 2.2): hold Y and it opens; every spell is a big picture on a ring round the screen's centre,
 * so a spell is chosen by pointing, by the mouse wheel or by a number, with nothing to read.
 *
 * <ul>
 *     <li><b>The ring</b>: the spells in the order of the spec, clockwise from the top, each in a 40 × 40 tile. The
 *     cursor more than the centre disc away from the middle points at the sector it is in (the hovered tile grows a fifth
 *     and its name and one line about it show inside the ring); the chosen spell has the hero's outline. Letting go of Y
 *     while pointing at a sector chooses it and closes the wheel; letting go with the cursor still in the middle leaves
 *     the wheel open to be clicked; Esc, or Y again, closes it unchanged. A click chooses a spell and keeps the wheel open
 *     (the style row and the blocks follow), a click on the chosen one closes it; <b>1-9</b> and <b>0</b> choose by number
 *     and close; the mouse wheel steps through the spells.</li>
 *     <li><b>The centre disc</b> (a door): stops building, or starts it again.</li>
 *     <li><b>Left</b>: Undo and Redo, with how many there are (grey at 0). <b>Right</b>: the style row of the chosen
 *     spell, as pictures ({@link StylePictures}); for Shapes also Solid and Hollow. <b>Under the ring</b>: the hotbar's
 *     slots (a click makes that slot's block the material and turns Mix off), the dice (Mix) and the palette
 *     ({@link BlockPaletteScreen}).</li>
 *     <li><b>Over the ring</b>: Wanda "Chaos Magic ∞"; Billy "Billy's Magic" with his stars, rank, meter and how far the
 *     next rank is.</li>
 * </ul>
 *
 * <p>Everything is drawn in the units of a 340 high design and scaled to fit the window (the pictures and the places
 * shrink, the words stay readable); a Hebrew line is ordered right to left ({@link HudDraw#visual}). Reduced motion: the
 * hovered tile does not grow. Non-pausing, like every screen of the mod.
 *
 * <p>Only ever loaded on a client.
 */
public final class SpellWheelScreen extends Screen {
    /** Whether Grow (the last spell, phase 2) is on the wheel. */
    private static final boolean WITH_GROW = true;
    /** The spells on the ring, in the spec's order. */
    private static final Spell[] SPELLS = WITH_GROW ? Spell.values()
            : java.util.Arrays.stream(Spell.values()).filter(s -> s != Spell.GROW).toArray(Spell[]::new);

    /** Radius of the ring of tiles, and the side of a tile, in design pixels (D-B 6.5). */
    private static final float RING = 110.0F;
    private static final float TILE = 40.0F;
    /** The hovered tile's growth. */
    private static final float GROWN = 1.2F;
    /** The centre disc's radius. */
    private static final float DISC = 28.0F;
    /** Where the sectors end: tiles, grown, and a margin. */
    private static final float ZONE = RING + 26.0F;
    /** How far the rows of the window may reach from the centre at most (the design's half height). */
    private static final float HALF_HEIGHT = 171.0F;
    private static final float HALF_WIDTH = 214.0F;
    private static final int INFO_ROWS = 11;

    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;
    private static final int DARK_GREY = 0x6E6E6E;
    private static final int GOLD = 0xFFD54A;
    private static final int ORANGE = 0xFFAA33;
    private static final int PANEL = 0x0B1220;

    /** What a rectangle on the wheel is. */
    private enum Kind {
        UNDO, REDO, MIX, BLOCKS, SLOT, STYLE, SOLID, HOLLOW
    }

    private record Hit(Kind kind, int index, int x0, int y0, int x1, int y1) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /** What the middle of the ring says about what the cursor is on. */
    private record Caption(Component title, @Nullable Component text, int rgb) {
    }

    /** Whether Y has been let go since the wheel opened: a key repeat of the hold must not close it. */
    private boolean released;
    private int cx;
    private int cy;
    private float k = 1.0F;
    private final List<Hit> hits = new ArrayList<>();
    private int hoveredSector = -1;
    private double mouseX;
    private double mouseY;

    /** The wheel; opened by a hold of Y ({@code byHold}), Y is still down and its release is part of the choice. */
    private SpellWheelScreen(boolean byHold) {
        super(Component.translatable("key.enhanced.build_magic"));
        this.released = !byHold;
    }

    /** Opens the wheel after a hold of Y (build mode is on by then). */
    static void open() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        mc.setScreen(new SpellWheelScreen(true));
        BuildFeedback.wheel(Powers.power(player), 1.0F);
    }

    /** The wheel again, from the palette page's way back. */
    static void reopen() {
        Minecraft.getInstance().setScreen(new SpellWheelScreen(false));
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        mouseX = mc.mouseHandler.xpos() * width / (double) mc.getWindow().getScreenWidth();
        mouseY = mc.mouseHandler.ypos() * height / (double) mc.getWindow().getScreenHeight();
        layout();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || !BuildClient.isBuilder(Powers.power(player))) {
            onClose();
        }
    }

    /** Nothing blurs the world: it is what the builder is about to change. A soft veil only. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x40000000, 0x70000000);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Layout

    /** The centre, the scale, and the rectangles that can be clicked, for this size of window and the chosen spell. */
    private void layout() {
        cx = width / 2;
        cy = height / 2;
        k = Mth.clamp(Math.min((height / 2.0F - 6.0F) / HALF_HEIGHT, (width / 2.0F - 4.0F) / HALF_WIDTH), 0.5F, 1.0F);
        hits.clear();
        Minecraft mc = Minecraft.getInstance();
        BuildGrammar grammar = BuildClient.grammar();

        // Left: Undo and Redo.
        int zone = Math.round(ZONE * k);
        int right = cx - zone - 8;
        int left = Math.max(3, right - 70);
        hits.add(new Hit(Kind.UNDO, 0, left, cy - 34, right, cy - 4));
        hits.add(new Hit(Kind.REDO, 0, left, cy + 4, right, cy + 34));

        // Right: the style row, two tiles wide; Solid and Hollow under the shapes.
        int styles = grammar.spell().styles();
        if (styles > 1) {
            int t = Math.max(24, Math.round(30.0F * k));
            int x0 = Math.min(cx + zone + 8, width - 3 - (2 * t + 2));
            int rows = (styles + 1) / 2;
            boolean shape = grammar.spell() == Spell.SHAPE;
            int total = rows * (t + 2) + (shape ? t + 6 : 0);
            int y0 = Math.max(3, cy - total / 2);
            for (int i = 0; i < styles; i++) {
                int x = x0 + (i % 2) * (t + 2);
                int y = y0 + (i / 2) * (t + 2);
                hits.add(new Hit(Kind.STYLE, i, x, y, x + t, y + t));
            }
            if (shape) {
                int y = y0 + rows * (t + 2) + 4;
                hits.add(new Hit(Kind.SOLID, 0, x0, y, x0 + t, y + t));
                hits.add(new Hit(Kind.HOLLOW, 1, x0 + t + 2, y, x0 + 2 * t + 2, y + t));
            }
        }

        // Under the ring: the dice, the nine hotbar slots, the palette.
        int slot = Math.max(18, Math.round(22.0F * k));
        int pitch = slot + 2;
        int side = Math.max(slot + 6, Math.round(34.0F * k));
        int total = side + 4 + 9 * pitch - 2 + 4 + side;
        int x = cx - total / 2;
        int y = Math.min(cy + Math.round((RING + TILE * 0.6F) * k) + 8, height - slot - 3);
        hits.add(new Hit(Kind.MIX, 0, x, y, x + side, y + slot));
        for (int i = 0; i < 9; i++) {
            int sx = x + side + 4 + i * pitch;
            hits.add(new Hit(Kind.SLOT, i, sx, y, sx + slot, y + slot));
        }
        int bx = x + side + 4 + 9 * pitch - 2 + 4;
        hits.add(new Hit(Kind.BLOCKS, 0, bx, y, bx + side, y + slot));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Where the cursor is

    /** The distance from the centre, in GUI pixels. */
    private double distance(double mx, double my) {
        return Math.sqrt((mx - cx) * (mx - cx) + (my - cy) * (my - cy));
    }

    /** The spell whose sector contains the point, or -1 in the middle disc and beyond the ring. */
    private int sectorAt(double mx, double my) {
        double d = distance(mx, my);
        if (d <= DISC * k || d > ZONE * k) {
            return -1;
        }
        double angle = Math.atan2(mx - cx, -(my - cy));
        if (angle < 0.0) {
            angle += Math.PI * 2.0;
        }
        return (int) Math.round(angle / (Math.PI * 2.0 / SPELLS.length)) % SPELLS.length;
    }

    private @Nullable Hit hitAt(double mx, double my) {
        for (Hit hit : hits) {
            if (hit.contains(mx, my)) {
                return hit;
            }
        }
        return null;
    }

    private boolean onDisc(double mx, double my) {
        return distance(mx, my) <= DISC * k;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        mouseX = mx;
        mouseY = my;
        layout();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        Font font = mc.font;
        super.render(g, mx, my, partialTick);
        Power hero = Powers.power(player);
        int rgb = hero.color;
        BuildGrammar grammar = BuildClient.grammar();
        BuildData data = player.getData(ModAttachments.BUILD_DATA);
        boolean calm = com.kolodkingal.enhanced.client.EnhancedClientConfig.REDUCED_MOTION.get();

        int sector = sectorAt(mx, my);
        if (sector != hoveredSector) {
            hoveredSector = sector;
            if (sector >= 0) {
                BuildFeedback.wheel(hero, 0.9F + 0.05F * sector);
            }
        }
        Hit hit = hitAt(mx, my);
        boolean disc = hit == null && onDisc(mx, my);

        // The ring: a dark band, the sector pointed at lit.
        float inner = DISC * k + 3.0F;
        float outer = ZONE * k;
        HudDraw.ring(g, cx, cy, inner, outer, 0.0F, 1.0F, HudDraw.argb(PANEL, 0.62F));
        HudDraw.ring(g, cx, cy, outer - 1.0F, outer, 0.0F, 1.0F, HudDraw.argb(rgb, 0.55F));
        HudDraw.ring(g, cx, cy, inner - 1.0F, inner, 0.0F, 1.0F, HudDraw.argb(rgb, 0.35F));
        float turn = 1.0F / SPELLS.length;
        int chosen = indexOf(grammar.spell());
        if (chosen >= 0) {
            HudDraw.ring(g, cx, cy, inner, outer, (chosen - 0.5F) * turn, (chosen + 0.5F) * turn, HudDraw.argb(rgb, 0.16F));
        }
        if (hit == null && sector >= 0) {
            HudDraw.ring(g, cx, cy, inner, outer, (sector - 0.5F) * turn, (sector + 0.5F) * turn, HudDraw.argb(rgb, 0.34F));
        }

        // The spells' tiles.
        for (int i = 0; i < SPELLS.length; i++) {
            double angle = Math.PI * 2.0 * i / SPELLS.length;
            float tx = cx + (float) Math.sin(angle) * RING * k;
            float ty = cy - (float) Math.cos(angle) * RING * k;
            boolean lit = hit == null && i == sector;
            spellTile(g, font, SPELLS[i], i, tx, ty, lit && !calm ? GROWN : 1.0F, lit, i == chosen, rgb);
        }

        // The middle: the door.
        door(g, hero, disc, BuildClient.buildMode());

        // What the cursor is on, in words.
        Caption caption = caption(player, grammar, data, hit, sector, disc, hero);
        captionText(g, font, caption);

        // Over the ring: who is building.
        info(g, font, player, hero, data);

        // The columns and the row.
        undoRedo(g, font, data, hit, rgb);
        styleRow(g, grammar, hit, rgb);
        hotbar(g, font, player, grammar, hit, rgb);
        if (hit != null && hit.kind() == Kind.SLOT) {
            ItemStack stack = player.getInventory().getItem(hit.index());
            if (!stack.isEmpty()) {
                g.renderTooltip(font, stack, mx, my);
            }
        }
    }

    private static int indexOf(Spell spell) {
        for (int i = 0; i < SPELLS.length; i++) {
            if (SPELLS[i] == spell) {
                return i;
            }
        }
        return -1;
    }

    /** A spell's picture on a dark tile, the number of its key in a corner; the chosen one outlined in the hero's colour. */
    private void spellTile(GuiGraphics g, Font font, Spell spell, int index, float x, float y, float grow, boolean lit, boolean chosen,
                           int rgb) {
        float size = TILE * k * grow;
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        int half = Math.round(size / 2.0F);
        g.fill(-half, -half, half, half, HudDraw.argb(chosen ? HudDraw.darker(rgb) : 0x101826, chosen ? 0.92F : 0.88F));
        int edge = chosen ? rgb : lit ? 0xFFFFFF : 0x4A5668;
        g.renderOutline(-half, -half, 2 * half, 2 * half, 0xFF000000 | edge);
        if (chosen) {
            g.renderOutline(-half + 1, -half + 1, 2 * half - 2, 2 * half - 2, HudDraw.argb(rgb, 0.7F));
        }
        float scale = size * 0.8F / 16.0F;
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0F);
        HudDraw.spell(g, spell, -8, -8);
        g.pose().popPose();
        if (index < 10) {
            String digit = String.valueOf((index + 1) % 10);
            g.drawString(font, digit, -half + 3, -half + 2, HudDraw.argb(lit || chosen ? WHITE : GREY, 1.0F), true);
        }
        g.pose().popPose();
    }

    /** The disc in the middle: a door, and a ring in the hero's colour that lights when it is pointed at. */
    private void door(GuiGraphics g, Power hero, boolean lit, boolean on) {
        float r = DISC * k;
        HudDraw.ring(g, cx, cy, 0.0F, r, 0.0F, 1.0F, HudDraw.argb(lit ? HudDraw.darker(hero.color) : 0x101826, 0.95F));
        HudDraw.ring(g, cx, cy, r - 1.5F, r, 0.0F, 1.0F, HudDraw.argb(lit ? WHITE : hero.color, 0.9F));
        float scale = r * 1.2F / 16.0F;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        if (!HudDraw.art(g, "door", -8, -8, 16, 16)) {
            g.renderItem(new ItemStack(Items.OAK_DOOR), -8, -8);
        }
        g.pose().popPose();
        if (!on) {
            // Build mode is off: the door is shut, and a little tick says what a click does.
            HudDraw.ring(g, cx, cy, r + 2.0F, r + 3.5F, 0.0F, 1.0F, HudDraw.argb(ORANGE, 0.6F));
        }
    }

    private Caption caption(LocalPlayer player, BuildGrammar grammar, BuildData data, @Nullable Hit hit, int sector, boolean disc,
                            Power hero) {
        int rgb = hero.color;
        if (hit != null) {
            switch (hit.kind()) {
                case UNDO:
                    return new Caption(Component.translatable("screen.enhanced.build.undo", data.undo()), null, WHITE);
                case REDO:
                    return new Caption(Component.translatable("screen.enhanced.build.redo", data.redo()), null, WHITE);
                case MIX:
                    return new Caption(Component.translatable("screen.enhanced.build.mix"), null, WHITE);
                case BLOCKS:
                    return new Caption(Component.translatable("screen.enhanced.build.blocks"), null, WHITE);
                case SLOT:
                    ItemStack stack = player.getInventory().getItem(hit.index());
                    if (!stack.isEmpty()) {
                        return new Caption(stack.getHoverName(), null, WHITE);
                    }
                    break;
                case STYLE:
                    return new Caption(styleName(grammar.spell(), hit.index()), null, WHITE);
                case SOLID:
                    return new Caption(Component.translatable("screen.enhanced.build.solid"), null, WHITE);
                case HOLLOW:
                    return new Caption(Component.translatable("screen.enhanced.build.hollow"), null, WHITE);
                default:
                    break;
            }
        }
        if (disc) {
            return new Caption(Component.translatable(BuildClient.buildMode() ? "screen.enhanced.build.stop" : "screen.enhanced.build.start"),
                    null, WHITE);
        }
        Spell spell = sector >= 0 ? SPELLS[sector] : grammar.spell();
        return new Caption(Component.translatable(spell.key()), Component.translatable(spell.key() + ".desc"), rgb);
    }

    /** The name of a style, or of a shape, of a spell. */
    static Component styleName(Spell spell, int style) {
        return Component.translatable(switch (spell) {
            case FILL -> com.kolodkingal.enhanced.build.FillStyle.byId(style).key();
            case VANISH -> com.kolodkingal.enhanced.build.VanishStyle.byId(style).key();
            case CHANGE -> com.kolodkingal.enhanced.build.ChangeStyle.byId(style).key();
            case SHAPE -> ShapeKind.byId(style).key();
            case LAND -> com.kolodkingal.enhanced.build.LandStyle.byId(style).key();
            case GROW -> com.kolodkingal.enhanced.build.GrowStyle.byId(style).key();
            default -> spell.key();
        });
    }

    /** The name, bold, and a line about it under the disc, inside the ring; shrunk to fit a small window. */
    private void captionText(GuiGraphics g, Font font, Caption caption) {
        float scale = k < 0.9F ? 0.8F : 1.0F;
        int width = Math.round((90.0F * k + 10.0F) / scale);
        int y = Math.round((DISC + 6.0F) * k);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        int line = 0;
        for (FormattedCharSequence row : font.split(caption.title().copy().withStyle(s -> s.withBold(true)), width)) {
            g.drawString(font, row, -font.width(row) / 2, Math.round(y / scale) + line * 10, HudDraw.argb(caption.rgb(), 1.0F), true);
            line++;
            if (line >= 2) {
                break;
            }
        }
        if (caption.text() != null) {
            int rows = 0;
            for (FormattedCharSequence row : font.split(caption.text(), width)) {
                g.drawString(font, row, -font.width(row) / 2, Math.round(y / scale) + line * 10 + 1, HudDraw.argb(GREY, 1.0F), true);
                line++;
                if (++rows >= 3) {
                    break;
                }
            }
        }
        g.pose().popPose();
    }

    /** Over the ring: Wanda's name and "∞"; Billy's name, stars, rank, meter and the way to the next rank. */
    private void info(GuiGraphics g, Font font, LocalPlayer player, Power hero, BuildData data) {
        int rgb = hero.color;
        boolean billy = hero == Power.BILLY;
        MutableComponent title = Component.translatable("screen.enhanced.build.title." + hero.name().toLowerCase(Locale.ROOT));
        title = title.withStyle(s -> s.withBold(true).withColor(rgb));
        if (!billy) {
            title.append(Component.literal(" ∞"));
        }
        List<FormattedCharSequence> rows = new ArrayList<>();
        rows.add(HudDraw.visual(title));
        int rank = BillyRanks.rank(data);
        if (billy) {
            Limits limits = Limits.of(player);
            int top = BillyRanks.meter(rank);
            MutableComponent stars = Component.empty();
            for (int i = 1; i <= BillyRanks.maxRank(); i++) {
                int star = i;
                stars.append(Component.literal(i <= rank ? "★" : "☆").withStyle(s -> s.withColor(star <= rank ? GOLD : DARK_GREY)));
            }
            stars.append(Component.literal(" ")).append(Component.translatable(BillyRanks.nameKey(rank)).withStyle(s -> s.withColor(WHITE)));
            rows.add(HudDraw.visual(stars));
            if (limits == null || limits.limited()) {
                rows.add(null);
                int next = rank < BillyRanks.maxRank() ? rank + 1 : 0;
                Component line = next > 0
                        ? Component.translatable("screen.enhanced.build.next_rank", HudDraw.number(data.practice()),
                                HudDraw.number(BillyRanks.practiceFor(next)), Component.translatable(BillyRanks.nameKey(next)))
                        : Component.translatable("screen.enhanced.build.top_rank");
                rows.add(HudDraw.visual(line.copy().withStyle(s -> s.withColor(GREY))));
            }
        }
        int height = rows.size() * INFO_ROWS;
        int bottom = cy - Math.round((RING + TILE * 0.6F) * k) - 4;
        int y = bottom - height;
        boolean corner = y < 2;
        // No room over the ring (a small window): the same lines in the top right corner, clear of the hero panel.
        if (corner) {
            y = 4;
        }
        for (FormattedCharSequence row : rows) {
            if (row == null) {
                meter(g, font, player, data, rank, cx, y + 1, corner, rgb);
            } else if (corner) {
                g.drawString(font, row, width - 6 - font.width(row), y, WHITE | 0xFF000000, true);
            } else {
                g.drawString(font, row, cx - font.width(row) / 2, y, WHITE | 0xFF000000, true);
            }
            y += INFO_ROWS;
        }
    }

    /** Billy's meter: a bar and "✦ 372/512", refilling as it does outside the wheel. */
    private void meter(GuiGraphics g, Font font, LocalPlayer player, BuildData data, int rank, int centre, int y, boolean corner, int rgb) {
        int top = BillyRanks.meter(rank);
        double now = BillyRanks.meterNow(data, player.level().getGameTime());
        FormattedCharSequence text = HudDraw.visual(Component.translatable("hud.enhanced.build.meter", HudDraw.number((long) now),
                HudDraw.number(top)));
        int bar = 80;
        int total = bar + 4 + font.width(text);
        int x = corner ? width - 6 - total : centre - total / 2;
        g.fill(x - 1, y, x + bar + 1, y + 7, 0xD9000000);
        g.fill(x, y + 1, x + bar, y + 6, 0xFF1C2833);
        int has = top <= 0 ? 0 : Mth.clamp(Mth.floor(now / top * bar), 0, bar);
        g.fill(x, y + 1, x + has, y + 6, 0xFF000000 | rgb);
        if (data.momNear()) {
            g.fill(x, y + 1, x + bar, y + 2, 0xFF000000 | Power.SCARLET_WITCH.color);
        }
        g.drawString(font, text, x + bar + 4, y, WHITE | 0xFF000000, true);
    }

    /** Undo and Redo, big, with how many there are; grey at 0. */
    private void undoRedo(GuiGraphics g, Font font, BuildData data, @Nullable Hit hit, int rgb) {
        for (Hit h : hits) {
            if (h.kind() != Kind.UNDO && h.kind() != Kind.REDO) {
                continue;
            }
            boolean undo = h.kind() == Kind.UNDO;
            int count = undo ? data.undo() : data.redo();
            boolean lit = hit == h && count > 0;
            button(g, h, lit, false, count > 0, rgb);
            int colour = count > 0 ? WHITE : DARK_GREY;
            g.pose().pushPose();
            g.pose().translate(h.x0() + 6, h.y0() + 5, 0.0F);
            g.pose().scale(1.8F, 1.8F, 1.0F);
            g.drawString(font, undo ? "↶" : "↷", 0, 0, 0xFF000000 | (count > 0 ? lit ? WHITE : rgb : colour), false);
            g.pose().popPose();
            FormattedCharSequence text = HudDraw.visual(Component.translatable(undo ? "screen.enhanced.build.undo" : "screen.enhanced.build.redo", count));
            int room = h.x1() - h.x0() - 28;
            float scale = Math.min(1.0F, room / (float) Math.max(1, font.width(text)));
            g.pose().pushPose();
            g.pose().translate(h.x0() + 26, h.y0() + (h.y1() - h.y0()) / 2.0F - 4.0F * scale, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, text, 0, 0, 0xFF000000 | colour, true);
            g.pose().popPose();
        }
    }

    /** A button's body: a dark plate with an outline, lit when pointed at or switched on. */
    private void button(GuiGraphics g, Hit h, boolean lit, boolean on, boolean live, int rgb) {
        g.fill(h.x0(), h.y0(), h.x1(), h.y1(), HudDraw.argb(on ? HudDraw.darker(rgb) : lit ? 0x1B2840 : 0x101826, 0.9F));
        int edge = on ? rgb : lit ? WHITE : live ? 0x5A6A82 : 0x333B48;
        g.renderOutline(h.x0(), h.y0(), h.x1() - h.x0(), h.y1() - h.y0(), 0xFF000000 | edge);
    }

    /** The current spell's styles, as pictures, and Solid or Hollow under the shapes. */
    private void styleRow(GuiGraphics g, BuildGrammar grammar, @Nullable Hit hit, int rgb) {
        Spell spell = grammar.spell();
        for (Hit h : hits) {
            int w = h.x1() - h.x0();
            int px = Math.max(1, (w - 4) / StylePictures.SIZE);
            int size = px * StylePictures.SIZE;
            int ix = h.x0() + (w - size) / 2;
            int iy = h.y0() + (w - size) / 2;
            switch (h.kind()) {
                case STYLE -> {
                    boolean chosen = h.index() == grammar.style();
                    button(g, h, hit == h, chosen, true, rgb);
                    StylePictures.draw(g, spell, h.index(), spell == Spell.SHAPE && grammar.hollow(), ix, iy, px, rgb);
                }
                case SOLID, HOLLOW -> {
                    boolean hollow = h.kind() == Kind.HOLLOW;
                    boolean chosen = hollow == grammar.hollow();
                    button(g, h, hit == h, chosen, true, rgb);
                    float r = w * 0.28F;
                    float ccx = h.x0() + w / 2.0F;
                    float ccy = h.y0() + (h.y1() - h.y0()) / 2.0F;
                    HudDraw.ring(g, ccx, ccy, hollow ? r - 2.5F : 0.0F, r, 0.0F, 1.0F, 0xFF000000 | (chosen ? WHITE : rgb));
                }
                default -> {
                }
            }
        }
    }

    /** The dice, the hotbar's nine slots and the palette's button, under the ring. */
    private void hotbar(GuiGraphics g, Font font, LocalPlayer player, BuildGrammar grammar, @Nullable Hit hit, int rgb) {
        Inventory inventory = player.getInventory();
        boolean mix = grammar.mix();
        boolean hand = !mix && grammar.material() == SpellRecipe.HAND;
        for (Hit h : hits) {
            switch (h.kind()) {
                case MIX -> {
                    button(g, h, hit == h, mix, true, rgb);
                    die(g, (h.x0() + h.x1()) / 2, (h.y0() + h.y1()) / 2, mix ? WHITE : rgb);
                }
                case BLOCKS -> {
                    button(g, h, hit == h, false, true, rgb);
                    palette(g, (h.x0() + h.x1()) / 2, (h.y0() + h.y1()) / 2);
                }
                case SLOT -> {
                    ItemStack stack = inventory.getItem(h.index());
                    boolean chosen = hand && h.index() == inventory.selected;
                    boolean block = stack.getItem() instanceof BlockItem;
                    button(g, h, hit == h, chosen, true, rgb);
                    int ix = (h.x0() + h.x1()) / 2 - 8;
                    int iy = (h.y0() + h.y1()) / 2 - 8;
                    if (!stack.isEmpty()) {
                        g.renderItem(stack, ix, iy);
                        g.renderItemDecorations(font, stack, ix, iy);
                        if (!block) {
                            g.fill(h.x0() + 1, h.y0() + 1, h.x1() - 1, h.y1() - 1, 0x90101826);
                        } else if (mix) {
                            g.renderOutline(h.x0() + 1, h.y0() + 1, h.x1() - h.x0() - 2, h.y1() - h.y0() - 2, HudDraw.argb(rgb, 0.8F));
                        }
                    }
                }
                default -> {
                }
            }
        }
    }

    /** A die, five pips (the dice of Mix). */
    private void die(GuiGraphics g, int x, int y, int rgb) {
        if (HudDraw.art(g, "dice", x - 8, y - 8, 16, 16)) {
            return;
        }
        g.fill(x - 7, y - 7, x + 7, y + 7, 0xFF000000 | rgb);
        g.fill(x - 6, y - 6, x + 6, y + 6, 0xFFF4F4F4);
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dy = -1; dy <= 1; dy += 2) {
                g.fill(x + dx * 3 - 1, y + dy * 3 - 1, x + dx * 3 + 1, y + dy * 3 + 1, 0xFF202830);
            }
        }
        g.fill(x - 1, y - 1, x + 1, y + 1, 0xFF202830);
    }

    /** Four coloured blocks (the palette page's button). */
    private void palette(GuiGraphics g, int x, int y) {
        if (HudDraw.art(g, "blocks", x - 8, y - 8, 16, 16)) {
            return;
        }
        g.fill(x - 7, y - 7, x - 1, y - 1, 0xFF8A8A8A);
        g.fill(x + 1, y - 7, x + 7, y - 1, 0xFFB5523B);
        g.fill(x - 7, y + 1, x - 1, y + 7, 0xFF58B84A);
        g.fill(x + 1, y + 1, x + 7, y + 7, 0xFFB08850);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Choosing

    /** Chooses a spell (build mode on first, if it was off). */
    private void choose(Spell spell) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (!BuildClient.buildMode()) {
            BuildClient.setMode(true);
        }
        if (BuildClient.grammar().spell() != spell) {
            BuildFeedback.clicked(1.2F);
        }
        BuildClient.choose(player, spell);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        layout();
        BuildGrammar grammar = BuildClient.grammar();
        Hit hit = hitAt(mx, my);
        if (hit != null) {
            switch (hit.kind()) {
                case UNDO -> {
                    if (player.getData(ModAttachments.BUILD_DATA).undo() > 0) {
                        BuildFeedback.clicked(1.0F);
                        BuildClient.undo();
                    }
                }
                case REDO -> {
                    if (player.getData(ModAttachments.BUILD_DATA).redo() > 0) {
                        BuildFeedback.clicked(1.0F);
                        BuildClient.redo();
                    }
                }
                case MIX -> {
                    BuildFeedback.clicked(grammar.mix() ? 0.9F : 1.3F);
                    BuildClient.setMix(player, !grammar.mix());
                }
                case BLOCKS -> {
                    BuildFeedback.clicked(1.1F);
                    Minecraft.getInstance().setScreen(new BlockPaletteScreen());
                }
                case SLOT -> {
                    BuildFeedback.clicked(1.1F);
                    BuildClient.useSlot(player, hit.index());
                }
                case STYLE -> {
                    BuildFeedback.clicked(0.9F + 0.1F * hit.index());
                    BuildClient.chooseStyle(player, hit.index());
                }
                case SOLID, HOLLOW -> {
                    BuildFeedback.clicked(hit.kind() == Kind.HOLLOW ? 1.3F : 0.9F);
                    BuildClient.chooseHollow(player, hit.kind() == Kind.HOLLOW);
                }
            }
            return true;
        }
        if (onDisc(mx, my)) {
            BuildClient.setMode(!BuildClient.buildMode());
            BuildFeedback.clicked(BuildClient.buildMode() ? 1.4F : 0.8F);
            if (!BuildClient.buildMode()) {
                onClose();
            }
            return true;
        }
        int sector = sectorAt(mx, my);
        if (sector >= 0) {
            if (SPELLS[sector] == grammar.spell() && BuildClient.buildMode()) {
                onClose();
            } else {
                choose(SPELLS[sector]);
            }
            return true;
        }
        // Anywhere else: the wheel is put away.
        onClose();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int steps = (int) Math.signum(scrollY);
        if (steps == 0) {
            return false;
        }
        int now = Math.max(0, indexOf(BuildClient.grammar().spell()));
        // Scrolling up goes round the ring clockwise, as the numbers go.
        choose(SPELLS[Math.floorMod(now - steps, SPELLS.length)]);
        BuildFeedback.wheel(Powers.power(Minecraft.getInstance().player), 1.1F);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (isMagicKey(key, scan)) {
            // The repeats of the hold that opened the wheel are not a second press of Y.
            if (released) {
                onClose();
            }
            return true;
        }
        int number = numberOf(key);
        if (number >= 0 && number < SPELLS.length) {
            choose(SPELLS[number]);
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        if (isMagicKey(key, scan) && !released) {
            released = true;
            // Let go on a picture: that spell, and the wheel closes. Let go in the middle: it stays to be clicked.
            layout();
            if (hitAt(mouseX, mouseY) == null) {
                int sector = sectorAt(mouseX, mouseY);
                if (sector >= 0) {
                    choose(SPELLS[sector]);
                    onClose();
                }
            }
            return true;
        }
        return super.keyReleased(key, scan, modifiers);
    }

    private static boolean isMagicKey(int key, int scan) {
        return InputConstants.getKey(key, scan).equals(BuildKeys.MAGIC.getKey());
    }

    /** 0 for the key 1 up to 8 for 9, 9 for 0; -1 for any other key (the number pad counts too). */
    private static int numberOf(int key) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            return key - GLFW.GLFW_KEY_1;
        }
        if (key >= GLFW.GLFW_KEY_KP_1 && key <= GLFW.GLFW_KEY_KP_9) {
            return key - GLFW.GLFW_KEY_KP_1;
        }
        return key == GLFW.GLFW_KEY_0 || key == GLFW.GLFW_KEY_KP_0 ? 9 : -1;
    }
}

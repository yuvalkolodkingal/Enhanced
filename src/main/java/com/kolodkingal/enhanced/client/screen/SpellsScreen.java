package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.spellbook.SpellLevels;
import com.kolodkingal.enhanced.spellbook.Spellbook;
import com.kolodkingal.enhanced.spellbook.WitchLevelView;
import com.kolodkingal.enhanced.spellbook.WitchLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * 3.18.1: the Book of Spells, the witch's levels (a caster's plain use of her book; the Ethereal witch opens it from her
 * hat's screen; a witch of the Demiurge's gift by using any spellbook): her five levels, each with its name and the spells
 * it opens on their keys, what is open already, the next level and the experience still missing, and the bar of her way
 * to it. Reads only what the server told the client ({@link WitchLevelView}).
 */
public class SpellsScreen extends Screen {
    private static final int W = 300;
    private static final int ROW = 30;

    public SpellsScreen() {
        super(Component.translatable("screen.enhanced.spells.title"));
    }

    /** Opens the screen (client only). */
    public static void open() {
        Minecraft.getInstance().setScreen(new SpellsScreen());
    }

    private int left() {
        return width / 2 - W / 2;
    }

    private int top() {
        return Math.max(8, height / 2 - (54 + ROW * SpellLevels.MAX + 34) / 2);
    }

    @Override
    protected void init() {
        int y = top() + 54 + ROW * SpellLevels.MAX + 10;
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.spellbook_hat.done"), b -> onClose())
                .bounds(width / 2 - 50, y, 100, 20).build());
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !WitchLevelView.active()) {
            onClose();
        }
    }

    /** The colour of her magic: her book's, or her hero's. */
    private static int color() {
        int c = WitchLevelView.color();
        Minecraft mc = Minecraft.getInstance();
        return c >= 0 || mc.player == null ? Math.max(0, c) : CustomHeroes.hero(mc.player).color();
    }

    private static int glow() {
        int c = WitchLevelView.glow();
        Minecraft mc = Minecraft.getInstance();
        return c >= 0 || mc.player == null ? Math.max(0, c) : CustomHeroes.hero(mc.player).glow();
    }

    /** The name of her source: her book's caster ("Ethereal Witch"), or the gift's hero. */
    private static Component who() {
        int s = WitchLevelView.source();
        if (s >= 1 && s <= 3) {
            return Spellbook.values()[s - 1].title();
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? Component.empty() : CustomHeroes.name(mc.player);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int color = color();
        int glow = glow();
        int x = left();
        int y = top();
        int level = WitchLevelView.level();
        int max = WitchLevelView.maxLevel();
        graphics.fill(x - 6, y - 6, x + W + 6, y + 54 + ROW * SpellLevels.MAX + 4, 0xC0101014);
        graphics.fill(x - 6, y - 6, x + W + 6, y - 4, 0xFF000000 | color);
        MutableComponent head = title.copy().append(Component.literal(" · ")).append(who());
        graphics.drawCenteredString(font, head.withStyle(s -> s.withColor(glow).withBold(true)), width / 2, y, 0xFFFFFF);
        Component state = Component.translatable("screen.enhanced.spells.level", level,
                Component.translatable(WitchLevels.levelTitleKey(WitchLevelView.source(), level))).withStyle(s -> s.withColor(color));
        graphics.drawCenteredString(font, state, width / 2, y + 13, 0xFFFFFF);
        Component xp = WitchLevelView.missing() > 0
                ? Component.translatable("screen.enhanced.spells.xp", WitchLevelView.xp(), SpellLevels.threshold(level + 1),
                WitchLevelView.missing()).withStyle(ChatFormatting.GRAY)
                : Component.translatable(level >= SpellLevels.MAX ? "screen.enhanced.spells.mastered" : "screen.enhanced.spells.server_max")
                .withStyle(ChatFormatting.GRAY);
        graphics.drawCenteredString(font, xp, width / 2, y + 25, 0xFFFFFF);
        // the bar of her way to the next level
        int bx = x + 20;
        int bw = W - 40;
        int by = y + 38;
        graphics.fill(bx - 1, by - 1, bx + bw + 1, by + 5, 0xFF000000);
        graphics.fill(bx, by, bx + bw, by + 4, 0xFF2A2A30);
        graphics.fill(bx, by, bx + Math.round(bw * WitchLevelView.progress()), by + 4, 0xFF000000 | color);
        int ry = y + 52;
        for (int l = 1; l <= SpellLevels.MAX; l++) {
            boolean open = l <= level;
            boolean next = l == level + 1 && l <= max;
            boolean beyond = l > max;
            int bg = open ? 0x50000000 | (color & 0xFFFFFF) : next ? 0x40FFFFFF : 0x30000000;
            graphics.fill(x, ry, x + W, ry + ROW - 3, bg & 0x7FFFFFFF | 0x20000000);
            if (next) {
                graphics.renderOutline(x, ry, W, ROW - 3, 0xFF000000 | glow);
            }
            int textColor = open ? 0xFFFFFF : next ? 0xE0E0E0 : 0x808080;
            Component name = Component.translatable("screen.enhanced.spells.row", l,
                    Component.translatable(WitchLevels.levelTitleKey(WitchLevelView.source(), l)));
            graphics.drawString(font, name.copy().withStyle(s -> s.withColor(open ? glow : textColor).withBold(open)), x + 6, ry + 4,
                    0xFFFFFFFF, true);
            Component mark = open ? Component.translatable("screen.enhanced.spells.open").withStyle(s -> s.withColor(color))
                    : next ? Component.translatable("screen.enhanced.spells.next", SpellLevels.threshold(l)).withStyle(ChatFormatting.YELLOW)
                    : beyond ? Component.translatable("screen.enhanced.spells.beyond").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.translatable("screen.enhanced.spells.locked", SpellLevels.threshold(l)).withStyle(ChatFormatting.DARK_GRAY);
            graphics.drawString(font, mark, x + W - 6 - font.width(mark), ry + 4, 0xFFFFFFFF, true);
            graphics.drawString(font, opens(l, textColor), x + 12, ry + 15, 0xFFFFFFFF, false);
            ry += ROW;
        }
    }

    /** What a level opens: each spell with its key ("[G] Flight"), a passive power as "always on". */
    private Component opens(int level, int textColor) {
        List<String[]> cells = WitchLevelView.opens(level);
        MutableComponent line = Component.empty();
        if (cells.isEmpty()) {
            return Component.translatable("screen.enhanced.spells.stronger").withStyle(s -> s.withColor(textColor).withItalic(true));
        }
        boolean first = true;
        for (String[] c : cells) {
            if (!first) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;
            int column = Integer.parseInt(c[0]);
            if (column > 0) {
                Component key = KeyBindings.ability(column).getTranslatedKeyMessage();
                Component label = "1".equals(c[1]) ? Component.translatable("hud.enhanced.custom.sneak", key) : key;
                line.append(Component.literal("[").append(label).append("] ").withStyle(s -> s.withColor(0xA0A0A0)));
            } else {
                line.append(Component.translatable("screen.enhanced.spells.passive").withStyle(s -> s.withColor(0xA0A0A0)));
            }
            line.append(SpellLevels.label(c[2]).withStyle(s -> s.withColor(textColor)));
        }
        return line;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

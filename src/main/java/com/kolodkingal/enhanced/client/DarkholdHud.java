package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A small hint panel at the top right while the Darkhold is in hand (3.2): its name in purple, what use does, what Sneak + use
 * does, and that magic used on the holder poisons the attacker; green while the beam burns. For someone who cannot read the book
 * it only says so; for someone with no power it says that holding use makes them Agatha (3.3). Nothing is drawn with the
 * book away, or with the GUI hidden.
 */
public class DarkholdHud implements LayeredDraw.Layer {
    private static final int PURPLE = 0xB45CFF;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || !DarkholdBook.holds(player)) {
            return;
        }
        Font font = mc.font;
        boolean firing = Powers.get(player).is(Live.BEAMING);
        List<Component> lines = DarkholdBook.awakens(player)
                ? List.of(Component.translatable("item.enhanced.darkhold").withStyle(s -> s.withBold(true).withColor(PURPLE)),
                        Component.translatable("hud.enhanced.darkhold.awaken").withStyle(ChatFormatting.LIGHT_PURPLE))
                : DarkholdBook.mayUse(player)
                ? List.of(Component.translatable("item.enhanced.darkhold").withStyle(s -> s.withBold(true).withColor(PURPLE)),
                        Component.translatable(firing ? "hud.enhanced.darkhold.beam_firing" : "hud.enhanced.darkhold.beam")
                                .withStyle(firing ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                        Component.translatable("hud.enhanced.darkhold.marks").withStyle(ChatFormatting.GRAY),
                        Component.translatable("hud.enhanced.darkhold.retaliation").withStyle(ChatFormatting.DARK_PURPLE))
                : List.of(Component.translatable("item.enhanced.darkhold").withStyle(s -> s.withBold(true).withColor(PURPLE)),
                        Component.translatable("hud.enhanced.darkhold.locked").withStyle(ChatFormatting.GRAY));
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int x = graphics.guiWidth() - width - 10;
        int y = 6;
        graphics.fill(x - 4, y - 3, x + width + 4, y + lines.size() * 10 + 1, 0x80000000);
        for (Component line : lines) {
            graphics.drawString(font, line, x, y, 0xFFFFFF);
            y += 10;
        }
    }
}

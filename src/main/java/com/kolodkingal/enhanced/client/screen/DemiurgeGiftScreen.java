package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.ability.Demiurge;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.ultimate.DemiurgeGifts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Billy the Demiurge's gift of magic (3.18.1, Sneak + G as the Demiurge): the player to give it to (any player online; only
 * one who has no hero can take it), and the magic to give, picked from the Hero Creator's catalogue, magic only
 * ({@link DemiurgeGifts#CATALOG}: the spellbooks' magic and flight, then the Scarlet Witch's, Billy's, Agatha's, Doctor
 * Strange's and Loki's abilities). "Give" sends {@code /demiurge give <player> <picks>}, "Take back" {@code /demiurge take
 * <player>}: the server judges both.
 */
public class DemiurgeGiftScreen extends Screen {
    private static final int COLUMNS = 3;
    private static final int ROWS = 8;
    private static final int PER_PAGE = COLUMNS * ROWS;
    private static final int CELL_W = 132;
    private static final int CELL_H = 15;

    private final Set<Integer> picks = new LinkedHashSet<>();
    private final List<UUID> players = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private int target;
    private int page;

    public DemiurgeGiftScreen() {
        super(Component.translatable("screen.enhanced.demiurge_gift.title"));
    }

    /** Opens the screen (the Demiurge pressed Sneak + G). */
    public static void open() {
        Minecraft.getInstance().setScreen(new DemiurgeGiftScreen());
    }

    private static int pages() {
        return (DemiurgeGifts.CATALOG.size() + PER_PAGE - 1) / PER_PAGE;
    }

    private int gridLeft() {
        return width / 2 - COLUMNS * CELL_W / 2;
    }

    private int gridTop() {
        return 48;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        players.clear();
        names.clear();
        if (mc.getConnection() != null && mc.player != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                if (!info.getProfile().getId().equals(mc.player.getUUID())) {
                    players.add(info.getProfile().getId());
                    names.add(info.getProfile().getName());
                }
            }
        }
        target = Math.max(0, Math.min(target, players.size() - 1));
        clearWidgets();
        int mid = width / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> step(-1)).bounds(mid - 110, 26, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> step(1)).bounds(mid + 90, 26, 20, 16).build());
        int left = gridLeft();
        int top = gridTop();
        for (int slot = 0; slot < PER_PAGE; slot++) {
            int index = page * PER_PAGE + slot;
            if (index >= DemiurgeGifts.CATALOG.size()) {
                break;
            }
            int x = left + (slot / ROWS) * CELL_W;
            int y = top + (slot % ROWS) * CELL_H;
            addRenderableWidget(new Pick(x + 1, y, CELL_W - 2, CELL_H - 1, index));
        }
        int below = top + ROWS * CELL_H + 4;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(mid - 60, below, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(mid + 40, below, 20, 16).build());
        int buttons = below + 22;
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.demiurge_gift.give")
                        .withStyle(s -> s.withColor(Demiurge.AURA)), b -> give())
                .bounds(mid - 154, buttons, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.demiurge_gift.take"), b -> take())
                .bounds(mid - 50, buttons, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.demiurge_gift.done"), b -> onClose())
                .bounds(mid + 54, buttons, 100, 20).build());
    }

    private void step(int by) {
        if (!players.isEmpty()) {
            target = (target + by + players.size()) % players.size();
        }
    }

    private void turn(int by) {
        page = (page + by + pages()) % pages();
        init();
    }

    private String targetName() {
        return players.isEmpty() ? "" : names.get(target);
    }

    private void give() {
        Minecraft mc = Minecraft.getInstance();
        if (players.isEmpty() || picks.isEmpty() || mc.getConnection() == null) {
            return;
        }
        mc.getConnection().sendCommand("demiurge give " + targetName() + " " + DemiurgeGifts.format(new ArrayList<>(picks)));
        onClose();
    }

    private void take() {
        Minecraft mc = Minecraft.getInstance();
        if (players.isEmpty() || mc.getConnection() == null) {
            return;
        }
        mc.getConnection().sendCommand("demiurge take " + targetName());
        onClose();
    }

    /** Picks or unpicks an entry; of one generic power only one option may be picked. */
    private void toggle(int index) {
        if (picks.remove(index)) {
            return;
        }
        String id = DemiurgeGifts.CATALOG.get(index).id();
        picks.removeIf(i -> DemiurgeGifts.CATALOG.get(i).id().equals(id));
        picks.add(index);
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Demiurge.is(Powers.get(mc.player))) {
            onClose();
        }
    }

    /** What the screen knows of the chosen player's hero: none, a hero, or nothing (he is too far to be seen). */
    private Component targetState() {
        Minecraft mc = Minecraft.getInstance();
        if (players.isEmpty() || mc.level == null) {
            return Component.translatable("screen.enhanced.demiurge_gift.nobody").withStyle(ChatFormatting.GRAY);
        }
        Player seen = mc.level.getPlayerByUUID(players.get(target));
        if (seen == null) {
            return Component.translatable("screen.enhanced.demiurge_gift.unseen").withStyle(ChatFormatting.GRAY);
        }
        Power power = Powers.power(seen);
        if (DemiurgeGifts.gifted(seen)) {
            return Component.translatable("screen.enhanced.demiurge_gift.gifted").withStyle(s -> s.withColor(Demiurge.AURA));
        }
        return power == Power.NONE ? Component.translatable("screen.enhanced.demiurge_gift.ready").withStyle(ChatFormatting.GREEN)
                : Component.translatable("screen.enhanced.demiurge_gift.has_hero", power.displayName()).withStyle(ChatFormatting.RED);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int mid = width / 2;
        graphics.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(Demiurge.AURA).withBold(true)), mid, 8, 0xFFFFFF);
        Component who = players.isEmpty() ? Component.translatable("screen.enhanced.demiurge_gift.nobody")
                : Component.literal(targetName()).withStyle(ChatFormatting.WHITE);
        graphics.drawCenteredString(font, who, mid, 27, 0xFFFFFF);
        graphics.drawCenteredString(font, targetState(), mid, 37, 0xFFFFFF);
        int below = gridTop() + ROWS * CELL_H + 4;
        graphics.drawCenteredString(font, Component.translatable("screen.enhanced.demiurge_gift.page", page + 1, pages(), picks.size())
                .withStyle(ChatFormatting.GRAY), mid, below + 4, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** One power: its name in its hero's colour, lit when picked. */
    private final class Pick extends AbstractButton {
        private final int index;

        Pick(int x, int y, int w, int h, int index) {
            super(x, y, w, h, DemiurgeGifts.CATALOG.get(index).name());
            this.index = index;
        }

        @Override
        public void onPress() {
            toggle(index);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            boolean on = picks.contains(index);
            DemiurgeGifts.Gift gift = DemiurgeGifts.CATALOG.get(index);
            Power hero = gift.hero();
            int color = hero == null ? 0xE6E6FF : hero.titleColor();
            graphics.fill(x, y, x + width, y + height, on ? 0xC0103A78 : 0x90000000);
            if (on) {
                graphics.renderOutline(x, y, width, height, 0xFF000000 | Demiurge.AURA);
            } else if (isHoveredOrFocused()) {
                graphics.renderOutline(x, y, width, height, 0xFFFFFFFF);
            }
            graphics.fill(x + 2, y + 3, x + 5, y + height - 3, 0xFF000000 | color);
            Component name = gift.name();
            String text = font.plainSubstrByWidth(name.getString(), width - 10);
            graphics.drawString(font, Component.literal(text).withStyle(s -> s.withColor(on ? 0xFFFFFF : 0xD0D0D0)), x + 8, y + 3, 0xFFFFFFFF, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

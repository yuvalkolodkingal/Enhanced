package com.kolodkingal.enhanced.client.eternity;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.eternity.Wishes;
import com.kolodkingal.enhanced.network.EternityScreenPayload;
import com.kolodkingal.enhanced.network.EternityWishPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Eternity's wish screen (3.8, PLAN 10.5): opened by the server ({@link EternityScreenPayload}) when Eternity speaks.
 * Eternity's words at the top; two big choices, "Ask for anything" and "Become Love"; and "Leave" (home now).
 *
 * <ul>
 * <li><b>Ask for anything</b>: a search box (the item's name in the player's language, or its id), a scrolling grid of
 * every item in the game (the creative tabs' order, then the rest by id; the ones Eternity never grants hidden: {@link
 * Wishes#forbidden}), each item's tooltip, the locked ones greyed with their reason (the payload's locks: the stones'
 * ledger, Gungnir, Mjolnir), and "Wish for it".</li>
 * <li><b>Become Love</b>: her card (her portrait drawn from her skin, what she is) and "Wish for it".</li>
 * </ul>
 * A player who has had his wish sees only Eternity's line and "Leave". The server checks everything again; Escape
 * closes the screen and leaves him standing on the mirror (the wishing stone opens it again).
 */
public class WishScreen extends Screen {
    private static final ResourceLocation LOVE_SKIN = Enhanced.id("textures/entity/suit/love.png");
    private static final int GOLD = 0xFFE59A;
    private static final int LILAC = 0xC08CFF;
    private static final int COLUMNS_MAX = 12;

    private enum Mode { CHOOSE, PICKER, LOVE }

    private final EternityScreenPayload payload;
    private final Map<ResourceLocation, String> locks = new HashMap<>();
    private Mode mode = Mode.CHOOSE;
    /** Every item the picker can show, and those matching the search. */
    private final List<ItemStack> all = new ArrayList<>();
    private List<ItemStack> shown = new ArrayList<>();
    private String search = "";
    private int scroll;
    private ItemStack selected = ItemStack.EMPTY;
    private EditBox box;
    private Button wishButton;
    // The grid's layout.
    private int gridX;
    private int gridY;
    private int columns;
    private int rows;
    private int pitch;

    public WishScreen(EternityScreenPayload payload) {
        super(Component.translatable("screen.enhanced.wish.title"));
        this.payload = payload;
        for (EternityScreenPayload.Lock lock : payload.locks()) {
            locks.put(lock.item(), lock.reasonKey());
        }
    }

    /** Opens it (the server says Eternity speaks). */
    public static void open(EternityScreenPayload payload) {
        Minecraft.getInstance().setScreen(new WishScreen(payload));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        clearWidgets();
        int cx = width / 2;
        switch (mode) {
            case CHOOSE -> {
                int cardW = Mth.clamp(width / 3, 120, 180);
                int cardH = 64;
                int top = height / 2 - 18;
                if (payload.mayWish()) {
                    boolean love = payload.loveAllowed();
                    int x0 = love ? cx - cardW - 6 : cx - cardW / 2;
                    addRenderableWidget(new Card(x0, top, cardW, cardH,
                            Component.translatable("screen.enhanced.wish.anything"),
                            Component.translatable("screen.enhanced.wish.anything_desc"), GOLD,
                                    () -> show(Mode.PICKER)));
                    if (love) {
                        addRenderableWidget(new Card(cx + 6, top, cardW, cardH,
                                Component.translatable("screen.enhanced.wish.love"),
                                Component.translatable("screen.enhanced.wish.love_short"), LILAC,
                                        () -> show(Mode.LOVE)));
                    }
                }
                addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.wish.leave"), b -> leave())
                        .bounds(cx - 50, Math.min(height - 26, top + cardH + 16), 100, 20).build());
            }
            case PICKER -> initPicker();
            case LOVE -> {
                int bottom = height - 28;
                addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.wish.back"),
                        b -> show(Mode.CHOOSE)).bounds(cx - 106, bottom, 100, 20).build());
                addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.wish.become_love")
                        .withStyle(s -> s.withColor(LILAC)), b -> wish(EternityWishPayload.LOVE, null))
                        .bounds(cx + 6, bottom, 100, 20).build());
            }
        }
    }

    private void show(Mode next) {
        mode = next;
        init();
    }

    // ---- the picker ----

    private void initPicker() {
        if (all.isEmpty()) {
            collect();
        }
        pitch = 20;
        columns = Mth.clamp((width - 40) / pitch, 6, COLUMNS_MAX);
        gridX = (width - columns * pitch) / 2;
        gridY = 58;
        rows = Math.max(2, (height - 34 - gridY - 18) / pitch);
        int fieldW = Math.min(200, columns * pitch);
        box = new EditBox(font, (width - fieldW) / 2, 36, fieldW, 16,
                Component.translatable("screen.enhanced.wish.search"));
        box.setHint(Component.translatable("screen.enhanced.wish.search").withStyle(ChatFormatting.DARK_GRAY));
        box.setValue(search);
        box.setResponder(text -> {
            search = text.trim().toLowerCase(Locale.ROOT);
            scroll = 0;
            filter();
        });
        addRenderableWidget(box);
        setInitialFocus(box);
        int bottom = height - 26;
        int cx = width / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.wish.back"), b -> show(Mode.CHOOSE))
                .bounds(cx - 106, bottom, 100, 20).build());
        wishButton = addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.wish.wish")
                .withStyle(s -> s.withColor(GOLD)), b -> wishSelected()).bounds(cx + 6, bottom, 100, 20).build());
        filter();
    }

    /** Every item Eternity may grant: the creative tabs' order, then the rest of the registry by id. */
    private void collect() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        FeatureFlagSet flags = player.connection.enabledFeatures();
        Set<Item> seen = new LinkedHashSet<>();
        try {
            CreativeModeTabs.tryRebuildTabContents(flags, false, player.level().registryAccess());
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
                    continue;
                }
                for (ItemStack stack : tab.getDisplayItems()) {
                    seen.add(stack.getItem());
                }
            }
        } catch (RuntimeException e) {
            Enhanced.LOGGER.warn("Eternity's picker could not read the creative tabs; every item by id", e);
        }
        List<Item> rest = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!seen.contains(item)) {
                rest.add(item);
            }
        }
        rest.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        seen.addAll(rest);
        for (Item item : seen) {
            if (!Wishes.forbidden(item, flags)) {
                all.add(new ItemStack(item));
            }
        }
    }

    private void filter() {
        if (search.isEmpty()) {
            shown = all;
        } else {
            List<ItemStack> found = new ArrayList<>();
            for (ItemStack stack : all) {
                String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (name.contains(search) || id.contains(search)) {
                    found.add(stack);
                }
            }
            shown = found;
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
        updateWishButton();
    }

    private int maxScroll() {
        int lines = (shown.size() + columns - 1) / Math.max(1, columns);
        return Math.max(0, lines - rows);
    }

    private void updateWishButton() {
        if (wishButton != null) {
            wishButton.active = !selected.isEmpty() && lockOf(selected) == null;
        }
    }

    private String lockOf(ItemStack stack) {
        return locks.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private void wishSelected() {
        if (!selected.isEmpty() && lockOf(selected) == null) {
            wish(EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(selected.getItem()));
        }
    }

    private void wish(byte kind, ResourceLocation item) {
        PacketDistributor.sendToServer(new EternityWishPayload(kind, item == null
                ? ResourceLocation.withDefaultNamespace("air") : item));
        onClose();
    }

    private void leave() {
        wish(EternityWishPayload.LEAVE, null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (mode == Mode.PICKER) {
            scroll = Mth.clamp(scroll - (int) Math.signum(dy), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, dx, dy);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mode == Mode.PICKER) {
            int index = slotAt(mouseX, mouseY);
            if (index >= 0) {
                selected = shown.get(index);
                updateWishButton();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The index in {@link #shown} of the slot under the mouse, or -1. */
    private int slotAt(double mouseX, double mouseY) {
        if (mouseX < gridX || mouseY < gridY) {
            return -1;
        }
        int col = (int) ((mouseX - gridX) / pitch);
        int row = (int) ((mouseY - gridY) / pitch);
        if (col >= columns || row >= rows) {
            return -1;
        }
        int index = (row + scroll) * columns + col;
        return index < shown.size() ? index : -1;
    }

    // ---- drawing ----

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // A soft white veil with a violet heart: Eternity's light over the realm.
        g.fillGradient(0, 0, width, height, 0x60F4F2FF, 0xA0201838);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(LILAC).withBold(true)), cx, 8, 0xFFFFFF);
        switch (mode) {
            case CHOOSE -> {
                Component line = Component.translatable(payload.mayWish() ? "eternity.enhanced.line.welcome"
                        : "message.enhanced.eternity.had_wish").withStyle(s -> s.withColor(0xF0ECFF).withItalic(true));
                int y = height / 2 - 52;
                for (FormattedCharSequence part : font.split(line, Math.min(320, width - 40))) {
                    g.drawCenteredString(font, part, cx, y, 0xFFFFFF);
                    y += 11;
                }
            }
            case PICKER -> renderPicker(g, mouseX, mouseY);
            case LOVE -> renderLove(g);
        }
    }

    private void renderPicker(GuiGraphics g, int mouseX, int mouseY) {
        g.drawCenteredString(font, Component.translatable("screen.enhanced.wish.anything")
                .withStyle(s -> s.withColor(GOLD)),
                width / 2, 22, 0xFFFFFF);
        g.fill(gridX - 3, gridY - 3, gridX + columns * pitch + 3, gridY + rows * pitch + 3, 0x90100C20);
        ItemStack hovered = ItemStack.EMPTY;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = (row + scroll) * columns + col;
                if (index >= shown.size()) {
                    break;
                }
                ItemStack stack = shown.get(index);
                int x = gridX + col * pitch;
                int y = gridY + row * pitch;
                boolean locked = lockOf(stack) != null;
                boolean over = mouseX >= x && mouseX < x + pitch && mouseY >= y && mouseY < y + pitch;
                if (ItemStack.isSameItem(stack, selected)) {
                    g.fill(x, y, x + pitch - 1, y + pitch - 1, 0xC0FFE59A);
                } else if (over) {
                    g.fill(x, y, x + pitch - 1, y + pitch - 1, 0x60FFFFFF);
                }
                g.renderItem(stack, x + 2, y + 2);
                if (locked) {
                    g.fill(x + 1, y + 1, x + pitch - 2, y + pitch - 2, 0xA0202020);
                    g.drawString(font, "✖", x + pitch - 8, y + 1, 0xFFE05050, true);
                }
                if (over) {
                    hovered = stack;
                }
            }
        }
        if (maxScroll() > 0) {
            int barH = rows * pitch;
            int knob = Math.max(10, barH * rows / ((shown.size() + columns - 1) / columns));
            int top = gridY + (barH - knob) * scroll / maxScroll();
            g.fill(gridX + columns * pitch + 4, top, gridX + columns * pitch + 7, top + knob, 0xC0FFE59A);
        }
        String count = shown.size() + " / " + all.size();
        g.drawString(font, count, gridX, gridY + rows * pitch + 5, 0xFFB8B0D0, false);
        if (!selected.isEmpty()) {
            Component name = selected.getHoverName();
            String lock = lockOf(selected);
            Component line = lock != null ? Component.translatable(lock, name).withStyle(ChatFormatting.RED)
                    : Component.translatable("screen.enhanced.wish.chosen", name).withStyle(s -> s.withColor(GOLD));
            g.drawCenteredString(font, line, width / 2, gridY + rows * pitch + 5, 0xFFFFFF);
        }
        if (!hovered.isEmpty()) {
            List<Component> lines = new ArrayList<>(getTooltipFromItem(minecraft, hovered));
            String lock = lockOf(hovered);
            if (lock != null) {
                lines.add(Component.translatable(lock, hovered.getHoverName()).withStyle(ChatFormatting.RED));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void renderLove(GuiGraphics g) {
        int cx = width / 2;
        g.drawCenteredString(font, Component.translatable("power.enhanced.love").withStyle(s -> s.withColor(LILAC)
                .withBold(true)), cx, 24, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("power.enhanced.love.awaken")
                .withStyle(s -> s.withColor(GOLD)),
                cx, 36, 0xFFFFFF);
        int scale = Mth.clamp((height - 130) / 32, 2, 4);
        int dollX = cx - 8 * scale;
        int dollY = 50;
        paperDoll(g, dollX, dollY, scale);
        int y = dollY + 32 * scale + 8;
        for (FormattedCharSequence part : font.split(Component.translatable("screen.enhanced.wish.love_desc")
                .withStyle(ChatFormatting.WHITE), Math.min(300, width - 40))) {
            g.drawCenteredString(font, part, cx, y, 0xFFFFFF);
            y += 11;
        }
    }

    /**
     * Her portrait: the front of her skin (head, body, arms, legs and their outer layers), {@code s} pixels a texel.
     */
    private static void paperDoll(GuiGraphics g, int x, int y, int s) {
        g.fill(x - 3 * s, y - s, x + 19 * s, y + 33 * s, 0x60FFFFFF);
        // head
        part(g, x + 4 * s, y, s, 8, 8, 8, 8, 40, 8);
        // body, arms, legs (the left limbs from the 1.8 skin's lower half)
        part(g, x + 4 * s, y + 8 * s, s, 20, 20, 8, 12, 20, 36);
        part(g, x, y + 8 * s, s, 44, 20, 4, 12, 44, 36);
        part(g, x + 12 * s, y + 8 * s, s, 36, 52, 4, 12, 52, 52);
        part(g, x + 4 * s, y + 20 * s, s, 4, 20, 4, 12, 4, 36);
        part(g, x + 8 * s, y + 20 * s, s, 20, 52, 4, 12, 4, 52);
    }

    private static void part(GuiGraphics g, int x, int y, int s, int u, int v, int w, int h, int ou, int ov) {
        g.blit(LOVE_SKIN, x, y, w * s, h * s, u, v, w, h, 64, 64);
        g.blit(LOVE_SKIN, x, y, w * s, h * s, ou, ov, w, h, 64, 64);
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive()) {
            onClose();
        }
    }

    /** A big choice: its name in its colour and a line about it. */
    private final class Card extends Button {
        private final Component desc;
        private final int color;

        Card(int x, int y, int w, int h, Component name, Component desc, int color, Runnable press) {
            super(x, y, w, h, name, b -> press.run(), DEFAULT_NARRATION);
            this.desc = desc;
            this.color = color;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean lit = isHoveredOrFocused();
            int x = getX();
            int y = getY();
            g.fill(x, y, x + width, y + height, lit ? 0xE0281E48 : 0xC0181030);
            g.renderOutline(x, y, width, height, 0xFF000000 | (lit ? color : (color >> 1) & 0x7F7F7F));
            g.drawCenteredString(font, getMessage().copy().withStyle(s -> s.withColor(color).withBold(true)),
                    x + width / 2, y + 10, 0xFFFFFF);
            int line = y + 26;
            for (FormattedCharSequence part : font.split(desc.copy().withStyle(ChatFormatting.GRAY), width - 12)) {
                g.drawCenteredString(font, part, x + width / 2, line, 0xFFFFFF);
                line += 10;
            }
        }
    }
}

package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The "Blocks" page of the Spell Wheel (D-B 5.4): a grid of big block icons taken from the game's own creative tabs
 * (Building Blocks, Coloured Blocks, Natural Blocks, Functional Blocks), a row of the last {@value #RECENT_MAX} blocks
 * used this session, and a search field nobody needs. Only blocks magic may make are shown (not air, nothing from
 * {@code #enhanced:build_unconjurable}, no block of two cells, no TNT, lava or fire for Billy); with
 * {@code requireMaterials} on, a player in Survival sees only the blocks the inventory holds. A click picks the block as
 * the material ({@code PICK_BLOCK}: the server checks the id and the tags again) and puts the page away; this is how
 * Survival magic conjures a block the kid does not carry.
 *
 * <p>Drawn in the units of a 250 high design scaled to the window, like the wheel. Wheel: scrolls the grid. Esc closes
 * it, the arrow at the top left goes back to the wheel.
 *
 * <p>Only ever loaded on a client.
 */
public final class BlockPaletteScreen extends Screen {
    /** How many blocks the Recent row keeps (D-B 5.4). */
    public static final int RECENT_MAX = 16;
    /** The creative tabs the grid takes its blocks from, in this order. */
    private static final List<ResourceKey<CreativeModeTab>> TABS = List.of(CreativeModeTabs.BUILDING_BLOCKS,
            CreativeModeTabs.COLORED_BLOCKS, CreativeModeTabs.NATURAL_BLOCKS, CreativeModeTabs.FUNCTIONAL_BLOCKS);
    private static final int COLUMNS = 12;
    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;
    private static final int PANEL = 0x0B1220;

    /** The blocks used this session, the newest first. */
    private static final List<Block> RECENT = new ArrayList<>();

    private final List<List<Block>> tabBlocks = new ArrayList<>();
    private final List<CreativeModeTab> tabs = new ArrayList<>();
    private int tab;
    private int scroll;
    private int rows = 4;
    private String search = "";
    /** What the grid shows: the tab's blocks, or the blocks of every tab whose name holds the search (made when either changes). */
    private List<Block> shown = List.of();
    /** Each block's lower-case name, made once. */
    private final Map<Block, String> names = new java.util.HashMap<>();
    private @Nullable EditBox box;
    private boolean dragging;

    private float k = 1.0F;
    private int gridX;
    private int gridY;
    private int pitch;
    private int cell;
    private int recentX;
    private int recentY;
    private int recentPitch;
    private int tabY;
    private int tabX;
    private int tabW;
    private int tabH;
    private int backX;
    private int backY;

    public BlockPaletteScreen() {
        super(Component.translatable("screen.enhanced.build.blocks.title"));
    }

    /** A block was picked (the palette, or a middle click): it heads the Recent row. */
    static void used(Block block) {
        RECENT.remove(block);
        RECENT.add(0, block);
        while (RECENT.size() > RECENT_MAX) {
            RECENT.remove(RECENT.size() - 1);
        }
    }

    /** Forgets the Recent row (at logout: "kept for the session"). */
    static void clear() {
        RECENT.clear();
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && tabBlocks.isEmpty()) {
            CreativeModeTabs.tryRebuildTabContents(player.connection.enabledFeatures(), false, player.level().registryAccess());
            for (ResourceKey<CreativeModeTab> key : TABS) {
                CreativeModeTab found = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
                if (found != null) {
                    tabs.add(found);
                    tabBlocks.add(blocksOf(player, found));
                }
            }
        }
        refresh();
        layout();
        int fieldW = Math.min(180, pitch * COLUMNS);
        box = new EditBox(font, (width - fieldW) / 2, height - 22, fieldW, 16, Component.translatable("screen.enhanced.build.blocks.search"));
        box.setHint(Component.translatable("screen.enhanced.build.blocks.search"));
        box.setValue(search);
        box.setResponder(text -> {
            search = text.trim().toLowerCase(Locale.ROOT);
            scroll = 0;
            refresh();
        });
        addRenderableWidget(box);
    }

    /** The blocks of a tab that magic may make, once each, in the tab's order. */
    private static List<Block> blocksOf(LocalPlayer player, CreativeModeTab found) {
        Set<Block> blocks = new LinkedHashSet<>();
        for (ItemStack stack : found.getDisplayItems()) {
            if (stack.getItem() instanceof BlockItem item && allowed(player, item.getBlock())) {
                blocks.add(item.getBlock());
            }
        }
        return new ArrayList<>(blocks);
    }

    /** Whether the palette shows a block: magic may make it, and with {@code requireMaterials} a Survival player has it. */
    private static boolean allowed(LocalPlayer player, Block block) {
        if (!BuildClient.conjurable(block.defaultBlockState())) {
            return false;
        }
        boolean materials = EnhancedConfig.SPEC.isLoaded() && BuildConfig.REQUIRE_MATERIALS.get() && !player.isCreative();
        return !materials || player.getInventory().contains(new ItemStack(block));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || !BuildClient.isBuilder(BuildClient.hero(player))) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x50000000, 0x80000000);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Layout

    private void layout() {
        k = Mth.clamp(Math.min((height - 10.0F) / 250.0F, (width - 10.0F) / 340.0F), 0.7F, 1.0F);
        pitch = Math.max(18, Math.round(26.0F * k));
        cell = pitch - 2;
        int gridW = COLUMNS * pitch;
        gridX = (width - gridW - 10) / 2;
        backX = 6;
        backY = 5;
        recentPitch = Math.max(14, Math.min(22, gridW / RECENT_MAX));
        recentX = gridX;
        recentY = 34;
        tabH = 24;
        tabW = 40;
        tabY = recentY + recentPitch + 8;
        tabX = (width - (TABS.size() * (tabW + 4) - 4)) / 2;
        gridY = tabY + tabH + 4;
        int bottom = height - 28;
        rows = Math.max(2, (bottom - gridY) / pitch);
    }

    /** Makes the list the grid shows: the tab's blocks, or, with something typed, every block of every tab whose name has it. */
    private void refresh() {
        if (search.isEmpty()) {
            shown = tab < tabBlocks.size() ? tabBlocks.get(tab) : List.of();
            return;
        }
        Set<Block> found = new LinkedHashSet<>();
        for (List<Block> list : tabBlocks) {
            for (Block block : list) {
                if (names.computeIfAbsent(block, b -> b.getName().getString().toLowerCase(Locale.ROOT)).contains(search)) {
                    found.add(block);
                }
            }
        }
        shown = new ArrayList<>(found);
    }

    private List<Block> shown() {
        return shown;
    }

    private int maxScroll(int count) {
        return Math.max(0, (count + COLUMNS - 1) / COLUMNS - rows);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        layout();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        Font font = mc.font;
        super.render(g, mx, my, partialTick);
        Power hero = BuildClient.hero(player);
        int rgb = hero.color;
        List<Block> blocks = shown();
        scroll = Mth.clamp(scroll, 0, maxScroll(blocks.size()));
        @Nullable ItemStack tooltip = null;

        // Title and the way back.
        Component title = this.title.copy().withStyle(style -> style.withBold(true).withColor(rgb));
        g.drawString(font, HudDraw.visual(title), (width - font.width(HudDraw.visual(title))) / 2, 9, 0xFFFFFFFF, true);
        boolean backLit = inBack(mx, my);
        g.fill(backX, backY, backX + 22, backY + 18, HudDraw.argb(backLit ? 0x1B2840 : 0x101826, 0.9F));
        g.renderOutline(backX, backY, 22, 18, 0xFF000000 | (backLit ? WHITE : 0x5A6A82));
        g.drawString(font, "←", backX + 6, backY + 5, 0xFFFFFFFF, true);

        // Recent.
        g.drawString(font, HudDraw.visual(Component.translatable("screen.enhanced.build.blocks.recent")), recentX, recentY - 11,
                0xFF000000 | GREY, true);
        for (int i = 0; i < RECENT_MAX; i++) {
            int x = recentX + i * recentPitch;
            g.fill(x, recentY, x + recentPitch - 1, recentY + recentPitch - 1, HudDraw.argb(PANEL, 0.7F));
            if (i < RECENT.size()) {
                boolean lit = mx >= x && mx < x + recentPitch - 1 && my >= recentY && my < recentY + recentPitch - 1;
                if (lit) {
                    g.renderOutline(x, recentY, recentPitch - 1, recentPitch - 1, 0xFFFFFFFF);
                    tooltip = new ItemStack(RECENT.get(i));
                }
                itemIn(g, new ItemStack(RECENT.get(i)), x, recentY, recentPitch - 1);
            } else {
                g.renderOutline(x, recentY, recentPitch - 1, recentPitch - 1, 0x33FFFFFF);
            }
        }

        // The four tabs.
        for (int i = 0; i < tabs.size(); i++) {
            int x = tabX + i * (tabW + 4);
            boolean chosen = i == tab && search.isEmpty();
            boolean lit = mx >= x && mx < x + tabW && my >= tabY && my < tabY + tabH;
            g.fill(x, tabY, x + tabW, tabY + tabH, HudDraw.argb(chosen ? HudDraw.darker(rgb) : lit ? 0x1B2840 : 0x101826, 0.92F));
            g.renderOutline(x, tabY, tabW, tabH, 0xFF000000 | (chosen ? rgb : lit ? WHITE : 0x4A5668));
            g.renderItem(tabs.get(i).getIconItem(), x + (tabW - 16) / 2, tabY + (tabH - 16) / 2);
        }

        // The grid.
        int gridW = COLUMNS * pitch;
        g.fill(gridX - 2, gridY - 2, gridX + gridW + 12, gridY + rows * pitch + 1, HudDraw.argb(PANEL, 0.6F));
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (row + scroll) * COLUMNS + col;
                int x = gridX + col * pitch;
                int y = gridY + row * pitch;
                g.fill(x, y, x + cell, y + cell, HudDraw.argb(0x16202F, 0.9F));
                if (index >= blocks.size()) {
                    continue;
                }
                ItemStack stack = new ItemStack(blocks.get(index));
                boolean lit = mx >= x && mx < x + cell && my >= y && my < y + cell;
                if (lit) {
                    g.fill(x, y, x + cell, y + cell, HudDraw.argb(rgb, 0.3F));
                    g.renderOutline(x, y, cell, cell, 0xFFFFFFFF);
                    tooltip = stack;
                }
                itemIn(g, stack, x, y, cell);
            }
        }
        scrollbar(g, blocks.size(), rgb);
        if (tooltip != null) {
            g.renderTooltip(font, tooltip, mx, my);
        }
        for (int i = 0; i < tabs.size(); i++) {
            int x = tabX + i * (tabW + 4);
            if (mx >= x && mx < x + tabW && my >= tabY && my < tabY + tabH) {
                g.renderTooltip(font, tabs.get(i).getDisplayName(), mx, my);
            }
        }
    }

    /** An item filling a square of {@code size} pixels (the 16 pixel model, scaled up in the grid: "big block icons"). */
    private static void itemIn(GuiGraphics g, ItemStack stack, int x, int y, int size) {
        float scale = (size - 2) / 16.0F;
        g.pose().pushPose();
        g.pose().translate(x + 1, y + 1, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    private void scrollbar(GuiGraphics g, int count, int rgb) {
        int x = gridX + COLUMNS * pitch + 3;
        int h = rows * pitch - 2;
        g.fill(x, gridY, x + 6, gridY + h, 0x66000000);
        int max = maxScroll(count);
        if (max <= 0) {
            return;
        }
        int total = (count + COLUMNS - 1) / COLUMNS;
        int thumb = Math.max(10, h * rows / total);
        int y = gridY + (h - thumb) * scroll / max;
        g.fill(x, y, x + 6, y + thumb, 0xFF000000 | (dragging ? WHITE : rgb));
    }

    private boolean inBack(double mx, double my) {
        return mx >= backX && mx < backX + 22 && my >= backY && my < backY + 18;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Choosing

    private void pick(Block block) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (!BuildClient.buildMode()) {
            BuildClient.setMode(true);
        }
        BuildFeedback.clicked(1.3F);
        BuildClient.pickBlock(player, block);
        onClose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (inBack(mx, my)) {
            BuildFeedback.clicked(0.9F);
            SpellWheelScreen.reopen();
            return true;
        }
        for (int i = 0; i < RECENT.size(); i++) {
            int x = recentX + i * recentPitch;
            if (mx >= x && mx < x + recentPitch - 1 && my >= recentY && my < recentY + recentPitch - 1) {
                pick(RECENT.get(i));
                return true;
            }
        }
        for (int i = 0; i < tabs.size(); i++) {
            int x = tabX + i * (tabW + 4);
            if (mx >= x && mx < x + tabW && my >= tabY && my < tabY + tabH) {
                BuildFeedback.clicked(1.0F + 0.1F * i);
                tab = i;
                scroll = 0;
                if (box != null && !search.isEmpty()) {
                    box.setValue("");
                }
                refresh();
                return true;
            }
        }
        List<Block> blocks = shown();
        int barX = gridX + COLUMNS * pitch + 3;
        if (mx >= barX && mx < barX + 6 && my >= gridY && my < gridY + rows * pitch) {
            dragging = true;
            dragTo(my, blocks.size());
            return true;
        }
        if (mx >= gridX && mx < gridX + COLUMNS * pitch && my >= gridY && my < gridY + rows * pitch) {
            int col = (int) ((mx - gridX) / pitch);
            int row = (int) ((my - gridY) / pitch);
            int index = (row + scroll) * COLUMNS + col;
            if (mx - gridX - col * pitch < cell && my - gridY - row * pitch < cell && index < blocks.size()) {
                pick(blocks.get(index));
                return true;
            }
        }
        return false;
    }

    private void dragTo(double my, int count) {
        int max = maxScroll(count);
        int h = rows * pitch - 2;
        scroll = Mth.clamp((int) Math.round((my - gridY) / h * max), 0, max);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            dragTo(my, shown().size());
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int max = maxScroll(shown().size());
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, max);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (box != null && box.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                box.setFocused(false);
                setFocused(null);
                return true;
            }
            return box.keyPressed(key, scan, modifiers) || box.canConsumeInput();
        }
        return super.keyPressed(key, scan, modifiers);
    }
}

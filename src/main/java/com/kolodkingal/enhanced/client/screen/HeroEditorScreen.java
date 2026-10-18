package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.custom.HeroFiles;
import com.kolodkingal.enhanced.client.custom.PreviewPuppet;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.client.screen.editor.EditorTab;
import com.kolodkingal.enhanced.client.screen.editor.KeysTab;
import com.kolodkingal.enhanced.client.screen.editor.LookTab;
import com.kolodkingal.enhanced.client.screen.editor.MovesTab;
import com.kolodkingal.enhanced.client.screen.editor.Popup;
import com.kolodkingal.enhanced.client.screen.editor.PowersTab;
import com.kolodkingal.enhanced.client.screen.editor.SaveTab;
import com.kolodkingal.enhanced.client.screen.editor.StartTab;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroJson;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.KeyLayout;
import com.kolodkingal.enhanced.custom.PowerModule;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Create my hero" (3.3, rewritten in 3.4): the editor of a custom hero, for an eleven-year-old, in English and Hebrew.
 * On the left a live 3D preview of the hero (drag it to turn it; under it Stand, Walk, Run, Fly and Try), on top the six
 * tabs and the big budget bar (green, yellow near the end, red and shaking over it):
 * <ul>
 *     <li><b>Start</b>: start from a hero ("Thor-like"...) or an archetype, or empty ({@link StartTab});</li>
 *     <li><b>Powers</b>: the whole catalogue by kind, the heroes' faces, a search box, and the power clicked
 *     ({@link PowersTab});</li>
 *     <li><b>Keys</b>: five keys, pressed and with Sneak, and Auto ({@link KeysTab});</li>
 *     <li><b>Moves</b>: how the hero flies, runs and stands, and the move of each key ({@link MovesTab});</li>
 *     <li><b>Look</b>: the skin, the name and colours, the cape, hat, visor, eyes and aura ({@link LookTab});</li>
 *     <li><b>Save</b>: become the hero, the files, the clipboard ({@link SaveTab}).</li>
 * </ul>
 * The editor runs the server's rules ({@link CustomHeroRules}) with the limits the server synced, so what it shows is
 * what the server will accept; the server judges again. Esc closes a pop-up first, then goes back.
 */
public class HeroEditorScreen extends Screen {
    /** The tabs, in order. */
    public enum Tab {
        START, POWERS, KEYS, MOVES, LOOK, SAVE
    }

    /** The palette of the colour pickers. */
    public static final int[] PALETTE = {0xE53935, 0xFB8C00, 0xF5A623, 0xFDD835, 0x7CB342, 0x43A047, 0x00897B, 0x00ACC1,
            0x3FA9F5, 0x1E88E5, 0x3949AB, 0x8E24AA, 0xD81B60, 0xF5F5F5, 0x9E9E9E, 0x212121};

    private static final int TABS_Y = 16;
    private static final int BUDGET_Y = 37;
    private static final int TOP = 52;
    private static final long SHAKE_MS = 700L;

    private final Screen parent;
    private CustomHero draft;
    private Tab tab;
    private final Map<Tab, EditorTab> tabs = new HashMap<>();
    private final Map<String, Integer> pages = new HashMap<>();
    @Nullable
    private Popup popup;
    @Nullable
    private Component status;
    private int statusColor = 0xFFFFFF;
    private long statusUntil;
    private boolean wasOver;
    private long shakeUntil;

    @Nullable
    private PreviewPuppet puppet;
    private boolean puppetTried;
    private float yaw = -25.0F;
    private boolean turning;
    private int tryingUntil;
    @Nullable
    private CatalogEntry holding;

    public HeroEditorScreen(Screen parent, CustomHero start) {
        super(Component.translatable("screen.enhanced.hero_creator.editor"));
        this.parent = parent;
        this.draft = start;
        this.tab = start.isEmpty() ? Tab.START : Tab.POWERS;
        tabs.put(Tab.START, new StartTab(this));
        tabs.put(Tab.POWERS, new PowersTab(this));
        tabs.put(Tab.KEYS, new KeysTab(this));
        tabs.put(Tab.MOVES, new MovesTab(this));
        tabs.put(Tab.LOOK, new LookTab(this));
        tabs.put(Tab.SAVE, new SaveTab(this));
        this.wasOver = CustomHeroRules.cost(start) > limits().budget();
    }

    // ---- what the tabs share ----

    public CustomHero draft() {
        return draft;
    }

    /** Changes the draft and lays the page out again (the budget bar shakes when it goes over). */
    public void setDraft(CustomHero hero) {
        draft = hero;
        boolean over = CustomHeroRules.cost(hero) > limits().budget();
        if (over && !wasOver) {
            shakeUntil = Util.getMillis() + SHAKE_MS;
            say(Component.translatable("screen.enhanced.hero_creator.over_budget"), false);
        }
        wasOver = over;
        rebuild();
    }

    /** Changes the draft without laying the page out again (a slider being dragged). */
    public void update(CustomHero hero) {
        draft = hero;
        boolean over = CustomHeroRules.cost(hero) > limits().budget();
        if (over && !wasOver) {
            shakeUntil = Util.getMillis() + SHAKE_MS;
            say(Component.translatable("screen.enhanced.hero_creator.over_budget"), false);
        }
        wasOver = over;
    }

    /** The limits the preview wears the draft with: the server's, but every power picked shows, even over the budget. */
    public CustomHeroRules.Limits previewLimits() {
        CustomHeroRules.Limits l = limits();
        return new CustomHeroRules.Limits(Integer.MAX_VALUE, Integer.MAX_VALUE, l.maxStrength(), l.maxSkinBytes(), l.disabled(),
                l.heroModules(), l.worldPowers(), l.layers(), l.spread());
    }

    public CustomHeroRules.Limits limits() {
        return CustomHeroRules.Limits.current();
    }

    public void rebuild() {
        clearWidgets();
        init();
    }

    public <T extends AbstractWidget> T add(T widget) {
        return addRenderableWidget(widget);
    }

    public void say(Component message, boolean good) {
        status = message;
        statusColor = good ? 0x7CFC7C : 0xFF8070;
        statusUntil = Util.getMillis() + 6000;
    }

    public void show(Tab tab) {
        this.tab = tab;
        popup = null;
        rebuild();
    }

    public Tab tab() {
        return tab;
    }

    /** Opens a choice over the page. */
    public void ask(Popup popup) {
        this.popup = popup;
        pages.remove("popup");
        rebuild();
    }

    public void closePopup() {
        popup = null;
        rebuild();
    }

    /** The page a list is on (kept within its {@code count} pages). */
    public int page(String key, int count) {
        int p = Mth.clamp(pages.getOrDefault(key, 0), 0, Math.max(0, count - 1));
        pages.put(key, p);
        return p;
    }

    public void turn(String key, int count, int by) {
        pages.put(key, Mth.clamp(pages.getOrDefault(key, 0) + by, 0, Math.max(0, count - 1)));
        rebuild();
    }

    public void resetPage(String key) {
        pages.remove(key);
    }

    @Nullable
    public PreviewPuppet puppet() {
        return puppet;
    }

    /** Shows the preview moving this way. */
    public void preview(HeroAnimator.PreviewMotion motion) {
        if (puppet != null) {
            puppet.motion(motion);
        }
    }

    /** Plays a binding on the preview (after it has worn the draft as it is now). */
    public void tryBinding(int column, boolean sneak) {
        if (puppet != null) {
            puppet.wear(draft, previewLimits());
            puppet.motion(HeroAnimator.PreviewMotion.IDLE);
            puppet.tryBinding(column, sneak);
        }
    }

    /**
     * Tries a power on the preview, picked or not: a held or switched-on power raises its stance for three seconds, any
     * other plays its move (as if it were on its key; the preview keeps wearing that for as long as it plays).
     */
    public void tryEntry(CatalogEntry entry) {
        if (puppet == null) {
            return;
        }
        CustomHero hero = draft;
        if (!hero.has(entry)) {
            String option = entry instanceof PowerModule m ? m.defaultOption().id() : HeroAbility.BOTH;
            hero = hero.with(new CustomHero.Pick(entry.id(), CustomHeroJson.DEFAULT_STRENGTH, option, -1));
        }
        CustomHero.Pick pick = hero.pick(entry);
        int column = 0;
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            if (entry.id().equals(hero.binding(c, false))) {
                column = c;
            }
        }
        if (column == 0) {
            column = KeyLayout.preferred(entry);
            hero = hero.withKey(column, entry.id());
        }
        puppet.wear(hero, previewLimits());
        puppet.motion(HeroAnimator.PreviewMotion.IDLE);
        tryingUntil = puppet.tickCount + 60;
        PowerModule.Act act = pick == null ? entry.act("") : pick.act();
        if (act == PowerModule.Act.HOLD || act == PowerModule.Act.TOGGLE) {
            puppet.tryHold(entry, true);
            holding = entry;
        } else {
            puppet.tryBinding(column, false);
        }
    }

    // ---- layout ----

    private int left() {
        return Mth.clamp(width / 3, 92, 160);
    }

    @Override
    protected void init() {
        if (!puppetTried) {
            puppetTried = true;
            puppet = PreviewPuppet.create();
        }
        int x0 = left() + 6;
        int right = width - 6;
        // Back, over the preview.
        add(new CreatorUi.BigButton(4, TABS_Y, left() - 6, 18, Component.translatable("screen.enhanced.hero_creator.back"), null, 0xB0B0B0,
                this::back));
        if (popup != null) {
            initPopup(x0, right);
            return;
        }
        Tab[] all = Tab.values();
        int tabW = (right - x0 - (all.length - 1) * 2) / all.length;
        for (int i = 0; i < all.length; i++) {
            Tab t = all[i];
            CreatorUi.BigButton b = add(new CreatorUi.BigButton(x0 + i * (tabW + 2), TABS_Y, tabW, 18,
                    Component.translatable("screen.enhanced.hero_creator.tab." + t.name().toLowerCase(Locale.ROOT)), null,
                    t == tab ? draft.color() : 0xA0A0A0, () -> show(t)).selected(t == tab));
            b.setTooltip(Tooltip.create(Component.translatable("screen.enhanced.hero_creator.tab." + t.name().toLowerCase(Locale.ROOT) + ".desc")));
        }
        initPreviewButtons();
        tabs.get(tab).init(x0, TOP, right - x0, height - 22 - TOP);
    }

    private void initPreviewButtons() {
        int w = left() - 6;
        int y = height - 42;
        HeroAnimator.PreviewMotion now = puppet == null ? HeroAnimator.PreviewMotion.IDLE : puppet.motion();
        String[] names = {"stand", "walk", "run", "fly"};
        HeroAnimator.PreviewMotion[] motions = {HeroAnimator.PreviewMotion.IDLE, HeroAnimator.PreviewMotion.WALK, HeroAnimator.PreviewMotion.RUN,
                HeroAnimator.PreviewMotion.FLY_CRUISE};
        int bw = (w - 3 * 2) / 4;
        for (int i = 0; i < names.length; i++) {
            HeroAnimator.PreviewMotion m = motions[i];
            CreatorUi.BigButton b = add(new CreatorUi.BigButton(4 + i * (bw + 2), y, bw, 18,
                    Component.translatable("screen.enhanced.hero_creator.preview." + names[i]), null, draft.glow(), () -> {
                preview(m);
                rebuild();
            }).selected(now == m || m == HeroAnimator.PreviewMotion.RUN && now == HeroAnimator.PreviewMotion.SPEED_RUN
                    || m == HeroAnimator.PreviewMotion.FLY_CRUISE && (now == HeroAnimator.PreviewMotion.FLY_HOVER
                    || now == HeroAnimator.PreviewMotion.FLY_SPRINT)));
            b.active = puppet != null;
        }
    }

    private void initPopup(int x0, int right) {
        Popup p = popup;
        int w = right - x0;
        int top = TOP - 14;
        int h = height - 22 - top;
        EditorTab helper = new EditorTab(this) {
            @Override
            public void init(int x, int y, int ww, int hh) {
                List<Card> cards = p.items().stream().map(item -> new Card(item.label(), item.sub(), item.icon(), item.face(), item.color(),
                        item.selected(), true, null, () -> {
                    popup = null;
                    item.action().run();
                    rebuild();
                })).toList();
                cards("popup", cards, x, y, ww, hh, 96, 22);
            }
        };
        helper.init(x0, top + 14, w, h - 40);
        add(new CreatorUi.BigButton(x0 + w / 4, height - 46, w / 2, 20, Component.translatable("screen.enhanced.hero_creator.close"), null,
                0xB0B0B0, this::closePopup));
    }

    private void releasePuppet() {
        if (puppet != null) {
            puppet.release();
            puppet = null;
        }
    }

    private void back() {
        if (popup != null) {
            closePopup();
        } else {
            onClose();
        }
    }

    /** Closes every screen of the creator (the hero was sent to the server). */
    public void closeAll() {
        releasePuppet();
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        releasePuppet();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && popup != null) {
            closePopup();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (puppet != null) {
            if (puppet.tickCount >= tryingUntil) {
                if (holding != null) {
                    puppet.tryHold(holding, false);
                    holding = null;
                }
                puppet.wear(draft, previewLimits());
            }
            puppet.tickPreview();
        }
    }

    // ---- the preview: drag to turn ----

    private boolean inPreview(double mx, double my) {
        return mx >= 2 && mx < left() && my >= TABS_Y + 20 && my < height - 46;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (inPreview(mx, my) && popup == null) {
            turning = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        turning = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (turning) {
            yaw = Mth.wrapDegrees(yaw - (float) dx * 2.0F);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    // ---- files dropped on the window ----

    @Override
    public void onFilesDrop(List<Path> dropped) {
        List<Path> pngs = HeroFiles.pngs(dropped);
        if (!pngs.isEmpty()) {
            show(Tab.LOOK);
            ((LookTab) tabs.get(Tab.LOOK)).loadSkin(pngs.get(0));
        } else if (!dropped.isEmpty() && dropped.get(0).getFileName().toString().endsWith(".json")) {
            ((SaveTab) tabs.get(Tab.SAVE)).load(HeroFiles.read(dropped.get(0)));
        }
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(HeroCreatorScreen.GOLD).withBold(true)), width / 2, 4, 0xFFFFFF);
        renderPreview(g, partialTick);
        int x0 = left() + 6;
        int right = width - 6;
        if (popup != null) {
            // (The pop-up's question stands where the budget bar does.)
            CreatorUi.drawFitted(g, font, popup.title().copy().withStyle(s -> s.withColor(draft.color()).withBold(true)), x0, BUDGET_Y + 2,
                    right - x0, 0xFFFFFF);
        } else {
            renderBudget(g, x0, right);
            tabs.get(tab).render(g, x0, TOP, right - x0, height - 22 - TOP, mouseX, mouseY, partialTick);
        }
        if (status != null && Util.getMillis() < statusUntil) {
            CreatorUi.drawFitted(g, font, status, x0, height - 14, right - x0, statusColor);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        // The pages' panel, and the preview's.
        g.fill(left() + 2, TABS_Y - 2, width - 2, height - 2, 0x50000000);
    }

    /** The budget bar: "45 / 90 points", green, then yellow from three quarters, red over (and it shakes). */
    private void renderBudget(GuiGraphics g, int x0, int right) {
        int cost = CustomHeroRules.cost(draft);
        int budget = Math.max(1, limits().budget());
        float f = cost / (float) budget;
        int color = f > 1.0F ? 0xE53935 : f >= 0.75F ? 0xFDD835 : 0x43C04A;
        int shake = Util.getMillis() < shakeUntil ? Math.round(Mth.sin(Util.getMillis() / 25.0F) * 3.0F) : 0;
        Component text = Component.translatable("screen.enhanced.hero_creator.budget", cost, budget).withStyle(s -> s.withColor(color).withBold(true));
        int tw = Math.min(font.width(text), (right - x0) / 3);
        CreatorUi.drawFitted(g, font, text, x0 + shake, BUDGET_Y + 2, tw, 0xFFFFFF);
        Component count = Component.translatable("screen.enhanced.hero_creator.powers_short", draft.powers().size(), limits().maxPowers())
                .withStyle(draft.powers().size() > limits().maxPowers() ? ChatFormatting.RED : ChatFormatting.GRAY);
        int cw = Math.min(font.width(count), 60);
        CreatorUi.drawFitted(g, font, count, right - cw, BUDGET_Y + 2, cw, 0xFFFFFF);
        int bx = x0 + tw + 6 + shake;
        int bw = right - cw - 6 - bx;
        if (bw > 10) {
            g.fill(bx, BUDGET_Y + 1, bx + bw, BUDGET_Y + 11, 0xFF202020);
            g.fill(bx, BUDGET_Y + 1, bx + Math.round(bw * Math.min(1.0F, f)), BUDGET_Y + 11, 0xFF000000 | color);
            g.renderOutline(bx - 1, BUDGET_Y, bw + 2, 12, 0xFF000000 | CreatorUi.darken(draft.color()));
            // A mark at three quarters.
            int q = bx + Math.round(bw * 0.75F);
            g.fill(q, BUDGET_Y + 1, q + 1, BUDGET_Y + 11, 0x80FFFFFF);
        }
    }

    /** The live preview: the puppet (or the paper doll), and the hero's name under it. */
    private void renderPreview(GuiGraphics g, float partialTick) {
        int l = left();
        int top = TABS_Y + 20;
        int bottom = height - 46;
        g.fill(2, top, l, bottom, 0x70000000);
        g.renderOutline(2, top, l - 2, bottom - top, 0xFF000000 | CreatorUi.darken(draft.color()));
        String name = CustomHeroRules.cleanName(draft.name());
        Component shown = Component.literal(name.isEmpty() ? CustomHeroRules.DEFAULT_NAME : name)
                .withStyle(s -> s.withColor(draft.color()).withBold(true));
        int w = Math.min(font.width(shown), l - 8);
        CreatorUi.drawFitted(g, font, shown, (l + 2) / 2 - w / 2, top + 4, l - 8, 0xFFFFFF);
        int roomH = bottom - top - 22;
        // (Room above the head for a hat.)
        float size = Math.min(roomH / 2.6F, (l - 8) / 1.2F);
        boolean drawn = false;
        if (puppet != null) {
            g.enableScissor(3, top + 1, l - 1, bottom - 1);
            drawn = puppet.draw(g, (l + 2) / 2, bottom - 8 - Math.round(size * 0.15F), size, yaw, partialTick);
            g.disableScissor();
        }
        if (!drawn) {
            int scale = Math.max(1, Math.min(4, roomH / 34));
            CreatorUi.paperDoll(g, CreatorUi.lookOf(draft), (l + 2) / 2 - 8 * scale, top + 16, scale);
        } else {
            CreatorUi.drawFitted(g, font, Component.translatable("screen.enhanced.hero_creator.preview.drag").withStyle(ChatFormatting.DARK_GRAY),
                    6, bottom - 10, l - 10, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Whether the draft has anything in it worth asking before it is replaced. */
    public boolean hasWork() {
        return !draft.powers().isEmpty();
    }

    /** A hero read from a file, the clipboard or a preset, as a fresh draft (kept as it was written). */
    public void replaceDraft(CustomHero hero) {
        wasOver = CustomHeroRules.cost(hero) > limits().budget();
        draft = hero;
        pages.clear();
        rebuild();
    }

    /** The JSON of the draft as the server would accept it (for the files and the clipboard). */
    public String json() {
        return CustomHeroJson.write(CustomHeroRules.sanitize(draft, limits()).hero());
    }
}

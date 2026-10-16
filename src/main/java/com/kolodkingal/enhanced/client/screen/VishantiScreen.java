package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.config.VishantiConfig;
import com.kolodkingal.enhanced.network.VishantiActionPayload;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
import com.kolodkingal.enhanced.vishanti.WardStrength;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * The Book of the Vishanti, opened by using it as a sorcerer: the way to the first base, every place saved with the book
 * (in any dimension; the bases marked), a field to save the spot the player stands on, as a place or as a base with a
 * ward, and how far the ward of this sorcerer reaches. Reads the places from the player's synced data and rebuilds itself
 * when the server confirms a change; the server checks everything again.
 */
public class VishantiScreen extends Screen {
    private static final int ROW = 22;
    /** Where the list of places starts, and how much room the controls under it take. */
    private static final int LIST_TOP = 70;
    private static final int BOTTOM = 98;
    private static final int WIDTH = 260;

    private int page;
    private BifrostData shown = BifrostData.EMPTY;
    private boolean shownReady;
    private int shownRadius;
    /** What the player has typed in the name field, kept while the screen rebuilds itself. */
    private String typed = "";
    private EditBox nameBox;

    public VishantiScreen() {
        super(Component.translatable("screen.enhanced.vishanti.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new VishantiScreen());
    }

    private static BifrostData data() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? BifrostData.EMPTY : PlaceBook.Kind.VISHANTI.data(player);
    }

    private static boolean ready() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && PlaceBook.cooldown(player, PlaceBook.Kind.VISHANTI) == 0;
    }

    private static int radius() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0 : WardStrength.radiusOf(player);
    }

    private int perPage() {
        return Math.max(1, (height - BOTTOM - LIST_TOP) / ROW);
    }

    @Override
    protected void init() {
        BifrostData data = data();
        shown = data;
        shownReady = ready();
        shownRadius = radius();
        int left = width / 2 - WIDTH / 2;
        int y = 44;

        Button base = Button.builder(Component.translatable("screen.enhanced.vishanti.base"),
                        b -> travel(VishantiActionPayload.GO_BASE, -1, ""))
                .bounds(left, y, WIDTH, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.enhanced.vishanti.base.tooltip")))
                .build();
        base.active = shownReady && data.bases() > 0;
        addRenderableWidget(base);
        y = LIST_TOP;

        List<BifrostData.Place> places = data.places();
        int pages = Math.max(1, (places.size() + perPage() - 1) / perPage());
        page = Math.min(page, pages - 1);
        int from = page * perPage();
        for (int i = from; i < Math.min(places.size(), from + perPage()); i++) {
            BifrostData.Place place = places.get(i);
            int index = i;
            Button go = Button.builder(label(place), b -> travel(VishantiActionPayload.TRAVEL, index, place.name()))
                    .bounds(left, y, WIDTH - 24, 20)
                    .tooltip(Tooltip.create(where(place)))
                    .build();
            go.active = shownReady;
            addRenderableWidget(go);
            addRenderableWidget(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED),
                            b -> send(VishantiActionPayload.FORGET, index, place.name()))
                    .bounds(left + WIDTH - 20, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable(place.base() ? "screen.enhanced.vishanti.forget_base"
                            : "screen.enhanced.vishanti.forget", place.name())))
                    .build());
            y += ROW;
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left, height - BOTTOM + 2, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + WIDTH - 20, height - BOTTOM + 2, 20, 20).build());
        }

        nameBox = new EditBox(font, left, height - 74, WIDTH, 20, Component.translatable("screen.enhanced.vishanti.name"));
        nameBox.setMaxLength(BifrostData.MAX_NAME_LENGTH);
        nameBox.setHint(Component.translatable("screen.enhanced.vishanti.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(typed);
        nameBox.setResponder(value -> typed = value);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.vishanti.save"), b -> save(false))
                .bounds(left, height - 50, WIDTH / 2 - 2, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.enhanced.vishanti.save.tooltip")))
                .build());
        Button saveBase = Button.builder(Component.translatable("screen.enhanced.vishanti.save_base"), b -> save(true))
                .bounds(left + WIDTH / 2 + 2, height - 50, WIDTH / 2 - 2, 20)
                .tooltip(Tooltip.create(baseTooltip(data)))
                .build();
        saveBase.active = shownRadius > 0;
        addRenderableWidget(saveBase);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(left, height - 26, WIDTH, 20).build());
    }

    private Component baseTooltip(BifrostData data) {
        if (shownRadius <= 0) {
            return Component.translatable("screen.enhanced.vishanti.save_base.off");
        }
        int max = EnhancedConfig.SPEC.isLoaded() ? VishantiConfig.MAX_WARDS.get() : 2;
        return Component.translatable("screen.enhanced.vishanti.save_base.tooltip", shownRadius, data.bases(), max);
    }

    /** The place's name (a base marked), then (in grey) how far away it is, or which world it is in. */
    private static Component label(BifrostData.Place place) {
        LocalPlayer player = Minecraft.getInstance().player;
        Component far;
        if (player != null && player.level().dimension() == place.dimension()) {
            far = Component.translatable("screen.enhanced.bifrost.distance", Math.round(player.position().distanceTo(place.pos())));
        } else {
            far = dimensionName(place.dimension());
        }
        Component name = place.base()
                ? Component.translatable("screen.enhanced.vishanti.base_mark", place.name()).withStyle(ChatFormatting.AQUA)
                : Component.literal(place.name());
        return name.copy().append(Component.literal("  ")).append(far.copy().withStyle(ChatFormatting.GRAY));
    }

    private static Component where(BifrostData.Place place) {
        Vec3 pos = place.pos();
        return dimensionName(place.dimension()).copy().append(Component.literal(String.format(Locale.ROOT, "  %d, %d, %d",
                (int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z))).withStyle(ChatFormatting.GRAY));
    }

    /** "Overworld", "The Nether", "The End", or the id of a dimension added by another mod. */
    private static Component dimensionName(ResourceKey<Level> dimension) {
        String key = "dimension." + dimension.location().getNamespace() + "." + dimension.location().getPath();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(BifrostData.dimensionId(dimension));
    }

    private void travel(int action, int index, String name) {
        send(action, index, name);
        onClose();
    }

    private void save(boolean base) {
        send(base ? VishantiActionPayload.SAVE_BASE : VishantiActionPayload.SAVE, -1, typed);
        typed = "";
        nameBox.setValue("");
    }

    private static void send(int action, int index, String name) {
        PacketDistributor.sendToServer(new VishantiActionPayload(action, index, name));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            save(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive() || !VishantiBook.holds(player)) {
            // The book answers when it is held: put it away and the screen goes too.
            onClose();
            return;
        }
        if (!data().equals(shown) || ready() != shownReady || radius() != shownRadius) {
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        LocalPlayer player = Minecraft.getInstance().player;
        int color = player == null ? Power.NONE.color : Powers.power(player).color;
        graphics.drawCenteredString(font, title.copy().withStyle(s -> s.withBold(true).withColor(0xE8B940)), width / 2, 8, 0xFFFFFF);
        Component ward = shownRadius > 0
                ? Component.translatable("screen.enhanced.vishanti.ward", shownRadius).withStyle(s -> s.withColor(color))
                : Component.translatable("screen.enhanced.vishanti.no_ward").withStyle(ChatFormatting.GRAY);
        graphics.drawCenteredString(font, ward, width / 2, 21, 0xFFFFFF);
        if (shown.places().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.enhanced.vishanti.empty").withStyle(ChatFormatting.GRAY),
                    width / 2, LIST_TOP + 8, 0xFFFFFF);
        }
        long wait = player == null ? 0 : PlaceBook.cooldown(player, PlaceBook.Kind.VISHANTI);
        Component hint = wait > 0
                ? Component.translatable("screen.enhanced.vishanti.cooldown", (wait + 19) / 20).withStyle(ChatFormatting.GOLD)
                : Component.translatable("screen.enhanced.vishanti.hint").withStyle(ChatFormatting.DARK_GRAY);
        graphics.drawCenteredString(font, hint, width / 2, 33, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

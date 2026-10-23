package com.kolodkingal.enhanced.client.strange;

import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.network.SlingRingActionPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.strange.SlingRingActions;
import com.kolodkingal.enhanced.strange.StrangeAbilities;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * The Sling Ring's screen (3.6), opened by using the ring as a sorcerer: the places it keeps (in any world; how far or
 * which world), each a button that opens a pair of portals to it, a cross to forget one, and a field to save the spot he
 * stands on. The Book of the Vishanti's screen in the ring's skin: a slow ring of orange sparks turns behind the list.
 * Reads the places from the player's synced data and rebuilds itself when they change; the server checks everything
 * again ({@code SlingRingActions}).
 */
public class SlingRingScreen extends Screen {
    private static final int ROW = 22;
    private static final int LIST_TOP = 58;
    private static final int BOTTOM = 98;
    private static final int WIDTH = 260;
    private static final int SPARKS = 72;

    private int page;
    private BifrostData shown = BifrostData.EMPTY;
    private boolean shownReady;
    private String typed = "";
    private EditBox nameBox;

    public SlingRingScreen() {
        super(Component.translatable("screen.enhanced.sling_ring.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new SlingRingScreen());
    }

    private static BifrostData data() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? BifrostData.EMPTY : PlaceBook.Kind.SLING_RING.data(player);
    }

    private static boolean ready() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && PlaceBook.cooldown(player, PlaceBook.Kind.SLING_RING) == 0;
    }

    private int perPage() {
        return Math.max(1, (height - BOTTOM - LIST_TOP) / ROW);
    }

    @Override
    protected void init() {
        BifrostData data = data();
        shown = data;
        shownReady = ready();
        int left = width / 2 - WIDTH / 2;
        int y = LIST_TOP;
        List<BifrostData.Place> places = data.places();
        int pages = Math.max(1, (places.size() + perPage() - 1) / perPage());
        page = Math.min(page, pages - 1);
        int from = page * perPage();
        for (int i = from; i < Math.min(places.size(), from + perPage()); i++) {
            BifrostData.Place place = places.get(i);
            int index = i;
            Button go = Button.builder(label(place), b -> {
                        send(SlingRingActionPayload.TRAVEL, index, place.name());
                        onClose();
                    })
                    .bounds(left, y, WIDTH - 24, 20)
                    .tooltip(Tooltip.create(where(place)))
                    .build();
            go.active = shownReady;
            addRenderableWidget(go);
            addRenderableWidget(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED),
                            b -> send(SlingRingActionPayload.FORGET, index, place.name()))
                    .bounds(left + WIDTH - 20, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable("screen.enhanced.sling_ring.forget", place.name())))
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
        nameBox = new EditBox(font, left, height - 74, WIDTH, 20, Component.translatable("screen.enhanced.sling_ring.name"));
        nameBox.setMaxLength(BifrostData.MAX_NAME_LENGTH);
        nameBox.setHint(Component.translatable("screen.enhanced.sling_ring.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(typed);
        nameBox.setResponder(value -> typed = value);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.sling_ring.save"), b -> save())
                .bounds(left, height - 50, WIDTH, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.enhanced.sling_ring.save.tooltip",
                        SlingRingActions.places())))
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(left, height - 26, WIDTH, 20).build());
    }

    /** The place's name, then (in grey) how far away it is, or which world it is in. */
    private static Component label(BifrostData.Place place) {
        LocalPlayer player = Minecraft.getInstance().player;
        Component far = player != null && player.level().dimension() == place.dimension()
                ? Component.translatable("screen.enhanced.bifrost.distance", Math.round(player.position().distanceTo(place.pos())))
                : dimensionName(place.dimension());
        return Component.literal(place.name()).append(Component.literal("  ")).append(far.copy().withStyle(ChatFormatting.GRAY));
    }

    private static Component where(BifrostData.Place place) {
        Vec3 pos = place.pos();
        return dimensionName(place.dimension()).copy().append(Component.literal(String.format(Locale.ROOT, "  %d, %d, %d",
                (int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z))).withStyle(ChatFormatting.GRAY));
    }

    private static Component dimensionName(ResourceKey<Level> dimension) {
        String key = "dimension." + dimension.location().getNamespace() + "." + dimension.location().getPath();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(BifrostData.dimensionId(dimension));
    }

    private void save() {
        send(SlingRingActionPayload.SAVE, -1, typed);
        typed = "";
        nameBox.setValue("");
    }

    private static void send(int action, int index, String name) {
        Net.sendToServer(new SlingRingActionPayload(action, index, name));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive() || !SlingRingActions.carries(player)) {
            onClose();
            return;
        }
        if (!data().equals(shown) || ready() != shownReady) {
            rebuildWidgets();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        sparkRing(graphics, partialTick);
    }

    /** A slow ring of orange sparks behind the list, the ring's own skin (drawn in the GUI, no texture). */
    private void sparkRing(GuiGraphics graphics, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        float time = (player == null ? 0 : player.tickCount) + partialTick;
        int cx = width / 2;
        int cy = (LIST_TOP + height - BOTTOM) / 2;
        float r = Math.min(WIDTH * 0.55F, (height - BOTTOM - LIST_TOP) * 0.6F + 30.0F);
        for (int i = 0; i < SPARKS; i++) {
            float a = (float) (i * Math.PI * 2 / SPARKS + time * 0.02F);
            float wobble = 1.0F + 0.04F * Mth.sin(time * 0.2F + i * 1.7F);
            int x = cx + Math.round(Mth.cos(a) * r * wobble);
            int y = cy + Math.round(Mth.sin(a) * r * wobble);
            int alpha = 0x60 + (int) (0x60 * (0.5F + 0.5F * Mth.sin(time * 0.3F + i)));
            int color = (alpha << 24) | (i % 4 == 0 ? StrangeAbilities.GOLD : StrangeAbilities.ORANGE);
            graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title.copy().withStyle(s -> s.withBold(true).withColor(StrangeAbilities.ORANGE)),
                width / 2, 10, 0xFFFFFF);
        LocalPlayer player = Minecraft.getInstance().player;
        long wait = player == null ? 0 : PlaceBook.cooldown(player, PlaceBook.Kind.SLING_RING);
        Component hint = wait > 0
                ? Component.translatable("screen.enhanced.sling_ring.cooldown", (wait + 19) / 20).withStyle(ChatFormatting.GOLD)
                : Component.translatable("screen.enhanced.sling_ring.hint").withStyle(ChatFormatting.GRAY);
        graphics.drawCenteredString(font, hint, width / 2, 24, 0xFFFFFF);
        Component count = Component.translatable("screen.enhanced.sling_ring.count", shown.places().size(),
                SlingRingActions.places()).withStyle(ChatFormatting.DARK_GRAY);
        graphics.drawCenteredString(font, count, width / 2, 36, 0xFFFFFF);
        if (shown.places().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.enhanced.sling_ring.empty")
                    .withStyle(ChatFormatting.GRAY), width / 2, LIST_TOP + 8, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

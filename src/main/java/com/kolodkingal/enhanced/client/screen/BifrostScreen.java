package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.network.BifrostActionPayload;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * The Bifrost, opened by using Mjolnir or Stormbreaker (or with Thor's Bifrost key): home, every place the player has
 * saved, in any dimension, and a field to save the spot they stand on. Reads the places straight from the player's
 * synced data and rebuilds itself whenever the server confirms a change; the server checks everything again.
 */
public class BifrostScreen extends Screen {
    private static final int ROW = 24;
    private static final int WIDTH = 260;
    /** The colours of the Bifrost, for the title. */
    private static final int[] RAINBOW = {0xFF6B6B, 0xFFB45C, 0xFFF07A, 0x86FF8F, 0x6FD6FF, 0x8C86FF, 0xE38CFF};

    private int page;
    private BifrostData shown = BifrostData.EMPTY;
    private boolean shownReady;
    /** What the player has typed in the name field, kept while the screen rebuilds itself. */
    private String typed = "";
    private EditBox nameBox;

    public BifrostScreen() {
        super(Component.translatable("screen.enhanced.bifrost.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new BifrostScreen());
    }

    private static BifrostData data() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? BifrostData.EMPTY : player.getData(ModAttachments.BIFROST);
    }

    /** The Bifrost can open now: its strength gathered again, and not already taking the player. */
    private static boolean ready() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && data().cooldown(player.level().getGameTime()) == 0 && !Powers.get(player).is(Live.BIFROST);
    }

    private int perPage() {
        return Math.max(1, (height - 170) / ROW);
    }

    @Override
    protected void init() {
        BifrostData data = data();
        shown = data;
        shownReady = ready();
        int left = width / 2 - WIDTH / 2;
        int y = 34;

        Button home = Button.builder(Component.translatable("screen.enhanced.bifrost.home"),
                        b -> travel(BifrostActionPayload.HOME, -1, ""))
                .bounds(left, y, WIDTH, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.enhanced.bifrost.home.tooltip")))
                .build();
        home.active = shownReady;
        addRenderableWidget(home);
        y += ROW + 8;

        List<BifrostData.Place> places = data.places();
        int pages = Math.max(1, (places.size() + perPage() - 1) / perPage());
        page = Math.min(page, pages - 1);
        int from = page * perPage();
        for (int i = from; i < Math.min(places.size(), from + perPage()); i++) {
            BifrostData.Place place = places.get(i);
            int index = i;
            Button go = Button.builder(label(place), b -> travel(BifrostActionPayload.TRAVEL, index, place.name()))
                    .bounds(left, y, WIDTH - 24, 20)
                    .tooltip(Tooltip.create(where(place)))
                    .build();
            go.active = shownReady;
            addRenderableWidget(go);
            addRenderableWidget(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED),
                            b -> send(BifrostActionPayload.FORGET, index, place.name()))
                    .bounds(left + WIDTH - 20, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable("screen.enhanced.bifrost.forget", place.name())))
                    .build());
            y += ROW;
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left, height - 86, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + WIDTH - 20, height - 86, 20, 20).build());
        }

        nameBox = new EditBox(font, left, height - 56, WIDTH - 86, 20, Component.translatable("screen.enhanced.bifrost.name"));
        nameBox.setMaxLength(BifrostData.MAX_NAME_LENGTH);
        nameBox.setHint(Component.translatable("screen.enhanced.bifrost.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(typed);
        nameBox.setResponder(value -> typed = value);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.bifrost.save"), b -> save())
                .bounds(left + WIDTH - 80, height - 56, 80, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.enhanced.bifrost.save.tooltip")))
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(left, height - 30, WIDTH, 20).build());
    }

    /** The place's name, then (in grey) how far away it is, or which world it is in. */
    private static Component label(BifrostData.Place place) {
        LocalPlayer player = Minecraft.getInstance().player;
        Component far;
        if (player != null && player.level().dimension() == place.dimension()) {
            far = Component.translatable("screen.enhanced.bifrost.distance", Math.round(player.position().distanceTo(place.pos())));
        } else {
            far = dimensionName(place.dimension());
        }
        return Component.literal(place.name()).append(Component.literal("  ")).append(far.copy().withStyle(ChatFormatting.GRAY));
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

    private void save() {
        send(BifrostActionPayload.SAVE, -1, typed);
        typed = "";
        nameBox.setValue("");
    }

    private static void send(int action, int index, String name) {
        PacketDistributor.sendToServer(new BifrostActionPayload(action, index, name));
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
        if (player == null || !player.isAlive() || !ThorAbilities.holdsWeapon(player)) {
            // The Bifrost answers the weapon: put it away and the screen goes too.
            onClose();
            return;
        }
        if (!data().equals(shown) || ready() != shownReady) {
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, rainbow(title.getString()), width / 2, 14, 0xFFFFFF);
        if (shown.places().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.enhanced.bifrost.empty").withStyle(ChatFormatting.GRAY),
                    width / 2, 34 + ROW + 14, 0xFFFFFF);
        }
        LocalPlayer player = Minecraft.getInstance().player;
        long wait = player == null ? 0 : shown.cooldown(player.level().getGameTime());
        Component hint = wait > 0
                ? Component.translatable("screen.enhanced.bifrost.cooldown", (wait + 19) / 20).withStyle(ChatFormatting.GOLD)
                : Component.translatable("screen.enhanced.bifrost.hint").withStyle(ChatFormatting.DARK_GRAY);
        graphics.drawCenteredString(font, hint, width / 2, height - 72, 0xFFFFFF);
    }

    private static MutableComponent rainbow(String text) {
        MutableComponent out = Component.empty().withStyle(ChatFormatting.BOLD);
        int[] i = {0};
        text.codePoints().forEach(c -> out.append(Component.literal(Character.toString(c))
                .withStyle(s -> s.withColor(RAINBOW[i[0]++ % RAINBOW.length]))));
        return out;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

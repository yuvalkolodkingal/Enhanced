package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.bifrost.BifrostMenu;
import com.kolodkingal.enhanced.config.HeimdallConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * The Bifrost, opened by using Mjolnir, Stormbreaker or Hofund (or with Thor's or Heimdall's Bifrost key): first the realms
 * (3.5: Asgard, only from outside it while it is open; Home, "Home (Midgard)" in Asgard), then every place the player has
 * saved, in any dimension, and a field to save the spot they stand on. Reads the places straight from the player's synced
 * data and rebuilds itself whenever the server confirms a change; the server checks everything again. Which entries it
 * shows and what each sends is {@link BifrostMenu}.
 *
 * <p>3.5, Heimdall's <b>Send mode</b> (Sneak + N, the Bifrost for a friend): the same realms and Heimdall's own places, and
 * choosing one sends the friend there ({@link BifrostActionPayload#SEND}); nothing is saved or forgotten in it.
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
    /** Send mode: the entity id of the player Heimdall sends, or -1 for a journey of one's own. */
    private final int sendTarget;

    public BifrostScreen() {
        this(-1);
    }

    private BifrostScreen(int sendTarget) {
        super(Component.translatable("screen.enhanced.bifrost.title"));
        this.sendTarget = sendTarget;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new BifrostScreen());
    }

    /** 3.5: Heimdall's Send mode, for the player whose entity id is {@code target} (the server chose him). */
    public static void openSend(int target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(target) instanceof Player) {
            mc.setScreen(new BifrostScreen(target));
        }
    }

    private boolean sending() {
        return sendTarget >= 0;
    }

    private static BifrostData data() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? BifrostData.EMPTY : player.getData(ModAttachments.BIFROST);
    }

    /**
     * The Bifrost can open now: its strength gathered again, and not already taking the player (Send mode: Heimdall's
     * Bifrost Strike has recharged).
     */
    private boolean ready() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        long now = player.level().getGameTime();
        if (sending()) {
            return Powers.get(player).isReady(HeroAbility.HEIMDALL_STRIKE, now);
        }
        return data().cooldown(now) == 0 && !Powers.get(player).is(Live.BIFROST);
    }

    /** The player Heimdall sends, while he is still there to be sent. */
    private Player friend() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getEntity(sendTarget) instanceof Player p ? p : null;
    }

    private int perPage() {
        return Math.max(1, (height - 170 - ROW) / ROW);
    }

    @Override
    protected void init() {
        BifrostData data = data();
        shown = data;
        shownReady = ready();
        int left = width / 2 - WIDTH / 2;
        int y = 34;
        Minecraft mc = Minecraft.getInstance();
        boolean inAsgard = mc.level != null && mc.level.dimension() == AsgardDimension.KEY;
        List<BifrostMenu.Entry> entries = BifrostMenu.entries(inAsgard, Bifrost.asgardEnabled(), data.places());

        // The realms, side by side.
        List<BifrostMenu.Entry> realms = entries.stream().filter(BifrostMenu.Entry::realm).toList();
        int realmWidth = (WIDTH - 4 * (realms.size() - 1)) / realms.size();
        for (int i = 0; i < realms.size(); i++) {
            BifrostMenu.Entry entry = realms.get(i);
            Component label = Component.translatable(entry.labelKey());
            if (entry.kind() == BifrostMenu.Kind.ASGARD) {
                label = rainbow(label.getString());
            }
            Button button = Button.builder(label, b -> travel(entry))
                    .bounds(left + i * (realmWidth + 4), y, realmWidth, 20)
                    .tooltip(Tooltip.create(Component.translatable(entry.kind() == BifrostMenu.Kind.ASGARD
                            ? "screen.enhanced.bifrost.asgard.tooltip" : "screen.enhanced.bifrost.home.tooltip")))
                    .build();
            button.active = shownReady;
            addRenderableWidget(button);
        }
        y += ROW + 8;

        List<BifrostMenu.Entry> places = entries.stream().filter(e -> !e.realm()).toList();
        int pages = Math.max(1, (places.size() + perPage() - 1) / perPage());
        page = Math.min(page, pages - 1);
        int from = page * perPage();
        for (int i = from; i < Math.min(places.size(), from + perPage()); i++) {
            BifrostMenu.Entry entry = places.get(i);
            BifrostData.Place place = data.places().get(entry.index());
            Button go = Button.builder(label(place), b -> travel(entry))
                    .bounds(left, y, sending() ? WIDTH : WIDTH - 24, 20)
                    .tooltip(Tooltip.create(where(place)))
                    .build();
            go.active = shownReady;
            addRenderableWidget(go);
            if (!sending()) {
                addRenderableWidget(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED),
                                b -> send(new BifrostActionPayload(BifrostActionPayload.FORGET, entry.index(), entry.name())))
                        .bounds(left + WIDTH - 20, y, 20, 20)
                        .tooltip(Tooltip.create(Component.translatable("screen.enhanced.bifrost.forget", place.name())))
                        .build());
            }
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

        if (!sending()) {
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
        } else {
            nameBox = null;
        }
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

    /** "Overworld", "The Nether", "The End", "Asgard", or the id of a dimension added by another mod. */
    private static Component dimensionName(ResourceKey<Level> dimension) {
        String key = "dimension." + dimension.location().getNamespace() + "." + dimension.location().getPath();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(BifrostData.dimensionId(dimension));
    }

    private void travel(BifrostMenu.Entry entry) {
        send(BifrostMenu.action(entry, sendTarget));
        onClose();
    }

    private void save() {
        send(new BifrostActionPayload(BifrostActionPayload.SAVE, -1, typed));
        typed = "";
        nameBox.setValue("");
    }

    private static void send(BifrostActionPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive()) {
            onClose();
            return;
        }
        if (sending()) {
            // Heimdall's Send mode: while he has the Strike and his friend is still within reach.
            Entity friend = friend();
            double range = EnhancedConfig.SPEC.isLoaded() ? HeimdallConfig.SEND_RANGE.get() : 24;
            if (!Powers.get(player).has(HeroAbility.HEIMDALL_STRIKE) || friend == null || friend.distanceTo(player) > range + 2) {
                onClose();
                return;
            }
        } else if (!ThorAbilities.callsBifrost(player)) {
            // The Bifrost answers the weapon (3.4: or a custom hero's Bifrost): put it away and the screen goes too.
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
        Player friend = sending() ? friend() : null;
        Component heading = friend != null
                ? rainbow(Component.translatable("screen.enhanced.bifrost.send_title", friend.getDisplayName()).getString())
                : rainbow(title.getString());
        graphics.drawCenteredString(font, heading, width / 2, 14, 0xFFFFFF);
        if (shown.places().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable(sending() ? "screen.enhanced.bifrost.send_empty" : "screen.enhanced.bifrost.empty")
                    .withStyle(ChatFormatting.GRAY), width / 2, 34 + ROW + 14, 0xFFFFFF);
        }
        LocalPlayer player = Minecraft.getInstance().player;
        long wait = player == null ? 0 : shown.cooldown(player.level().getGameTime());
        Component hint;
        if (sending()) {
            hint = Component.translatable("screen.enhanced.bifrost.send_hint").withStyle(ChatFormatting.DARK_GRAY);
        } else if (wait > 0) {
            hint = Component.translatable("screen.enhanced.bifrost.cooldown", (wait + 19) / 20).withStyle(ChatFormatting.GOLD);
        } else {
            hint = Component.translatable("screen.enhanced.bifrost.hint").withStyle(ChatFormatting.DARK_GRAY);
        }
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

package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.network.HexActionPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Wanda's Hex Control: choose who may cross the wall, resize it, or dissolve it, and whether the Vision her Hex raised
 * follows her or holds his post. Reads the Hex straight from the synced level data and rebuilds itself whenever the
 * server confirms a change.
 */
public class HexScreen extends Screen {
    private static final int ROW = 24;
    private static final int WIDTH = 220;

    private int page;
    private int lastState;

    public HexScreen() {
        super(Component.translatable("screen.enhanced.hex.title"));
    }

    private Optional<Hex> hex() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return Optional.empty();
        }
        return mc.level.getData(ModAttachments.HEXES).byOwner(mc.player.getUUID());
    }

    private List<PlayerInfo> otherPlayers() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            return List.of();
        }
        UUID self = mc.player.getUUID();
        List<PlayerInfo> list = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            if (!info.getProfile().getId().equals(self)) {
                list.add(info);
            }
        }
        list.sort(Comparator.comparing(info -> info.getProfile().getName(), String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private int perPage() {
        return Math.max(1, (height - 150 - 2 * ROW) / ROW);
    }

    @Override
    protected void init() {
        Optional<Hex> found = hex();
        if (found.isEmpty()) {
            return;
        }
        Hex hex = found.get();
        lastState = stateHash(hex);
        int left = width / 2 - WIDTH / 2;
        int y = 36;

        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(HexAction.SET_RADIUS, null, hex.radius() - 8))
                .bounds(left, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(HexAction.SET_RADIUS, null, hex.radius() + 8))
                .bounds(left + WIDTH - 20, y, 20, 20).build());
        y += ROW;

        Component creatures = Component.translatable("screen.enhanced.hex.creatures",
                hex.allowCreatures() ? allowed() : blocked());
        addRenderableWidget(Button.builder(creatures, b -> send(HexAction.SET_CREATURES, null, hex.allowCreatures() ? 0 : 1))
                .bounds(left, y, WIDTH, 20).build());
        y += ROW;

        Component visionOrder = Component.translatable("screen.enhanced.hex.vision",
                Component.translatable(hex.visionStays() ? "screen.enhanced.hex.vision_stays" : "screen.enhanced.hex.vision_follows"));
        Component keys = KeyBindings.VISION_COMMAND.getTranslatedKeyMessage();
        addRenderableWidget(Button.builder(visionOrder, b -> send(HexAction.SET_VISION_STAYS, null, hex.visionStays() ? 0 : 1))
                .tooltip(Tooltip.create(Component.translatable("screen.enhanced.hex.vision_tooltip", keys, keys)))
                .bounds(left, y, WIDTH, 20).build());
        y += ROW;

        // 3.3: the Hex Runes on the four walls
        Component runes = Component.translatable("screen.enhanced.hex.runes", hex.runes()
                ? Component.translatable("screen.enhanced.hex.runes_on").withStyle(ChatFormatting.RED)
                : Component.translatable("screen.enhanced.hex.runes_off").withStyle(ChatFormatting.GRAY));
        addRenderableWidget(Button.builder(runes, b -> send(HexAction.SET_RUNES, null, hex.runes() ? 0 : 1))
                .tooltip(Tooltip.create(Component.translatable("screen.enhanced.hex.runes_tooltip")))
                .bounds(left, y, WIDTH, 20).build());
        y += ROW + 8;

        List<PlayerInfo> players = otherPlayers();
        int pages = Math.max(1, (players.size() + perPage() - 1) / perPage());
        page = Math.min(page, pages - 1);
        int from = page * perPage();
        for (int i = from; i < Math.min(players.size(), from + perPage()); i++) {
            PlayerInfo info = players.get(i);
            UUID id = info.getProfile().getId();
            boolean isAllowed = hex.allowed().contains(id);
            Component label = Component.literal(info.getProfile().getName() + ": ").append(isAllowed ? allowed() : blocked());
            addRenderableWidget(Button.builder(label, b -> send(HexAction.SET_PLAYER, id, isAllowed ? 0 : 1))
                    .bounds(left, y, WIDTH, 20).build());
            y += ROW;
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left, height - 56, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + WIDTH - 20, height - 56, 20, 20).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.hex.dissolve").withStyle(ChatFormatting.RED), b -> {
            send(HexAction.DISSOLVE, null, 0);
            onClose();
        }).bounds(left, height - 30, WIDTH / 2 - 4, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + WIDTH / 2 + 4, height - 30, WIDTH / 2 - 4, 20).build());
    }

    private static Component allowed() {
        return Component.translatable("screen.enhanced.hex.allowed").withStyle(ChatFormatting.GREEN);
    }

    private static Component blocked() {
        return Component.translatable("screen.enhanced.hex.blocked").withStyle(ChatFormatting.RED);
    }

    private void send(HexAction action, UUID target, int value) {
        PacketDistributor.sendToServer(new HexActionPayload(action, Optional.ofNullable(target), value));
    }

    private int stateHash(Hex hex) {
        return Objects.hash(hex.radius(), hex.allowCreatures(), hex.visionStays(), hex.runes(), hex.allowed(), otherPlayers().size());
    }

    @Override
    public void tick() {
        Optional<Hex> found = hex();
        if (found.isEmpty()) {
            onClose();
            return;
        }
        if (stateHash(found.get()) != lastState) {
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(Power.SCARLET_WITCH.color).withBold(true)), width / 2, 14, 0xFFFFFF);
        hex().ifPresent(hex -> {
            graphics.drawCenteredString(font, Component.translatable("screen.enhanced.hex.radius", hex.radius()), width / 2, 42, 0xFFFFFF);
            if (otherPlayers().isEmpty()) {
                graphics.drawCenteredString(font, Component.translatable("screen.enhanced.hex.no_players").withStyle(ChatFormatting.GRAY),
                        width / 2, 36 + ROW * 4 + 14, 0xFFFFFF);
            }
        });
        long deathPercent = EnhancedConfig.SPEC.isLoaded() ? Math.round(EnhancedConfig.HEX_DEATH_CHANCE.get() * 100) : 95;
        graphics.drawCenteredString(font, Component.translatable("screen.enhanced.hex.hint", deathPercent).withStyle(ChatFormatting.DARK_GRAY),
                width / 2, height - 72, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

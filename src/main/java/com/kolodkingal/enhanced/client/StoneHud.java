package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.item.InfinityStoneItem;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.stones.Snap;
import com.kolodkingal.enhanced.stones.StoneTouch;
import com.kolodkingal.enhanced.stones.StoneUse;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * The Infinity Stones' panel (3.6, the plan's 3.0; lane L1 draws it, lane L2 feeds it): while a stone, a crafted
 * Gauntlet with stones or a stone's vessel (Loki's Scepter, the Tesseract, the Eye of Agamotto) is in a hand, its
 * stones as gems to the right of the hotbar (the Gauntlet's selected one larger, framed in its colour), each with its
 * cooldown sweep (the stone item's cooldown, which lane L2 sets); above them the lines and bars the stones' powers
 * register for the stone in use ({@link #registerLine}, {@link #registerBar}) and the Snap's readiness. A bare stone
 * that burns its carrier shows its grace counting down in red above the hotbar ("The Power Stone burns! 7…"), as the
 * server counts it ({@link StoneTouch}, the same rule run here). The white flash of a Snap seen near. Nothing with the
 * GUI hidden.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public class StoneHud implements LayeredDraw.Layer {
    private static final int GAP = 18;
    private static final Map<StoneKind, List<Function<Player, Component>>> LINES = new EnumMap<>(StoneKind.class);
    private static final Map<StoneKind, List<ToDoubleFunction<Player>>> BARS = new EnumMap<>(StoneKind.class);
    /** The local player's grace per stone, counted as the server counts it. */
    private static final Map<StoneKind, Integer> GRACE = new EnumMap<>(StoneKind.class);
    /** The white flash of a Snap (1 at its start, fading). */
    private static float flash;
    /** The red edge of a rejection (1 at its start, fading). */
    private static float burn;

    /**
     * A line under the stone's gem while it is the one in use (lane L2: "Slow Time: on", "×3 bubbles"); the function
     * gives null when there is nothing to say.
     */
    public static synchronized void registerLine(StoneKind kind, Function<Player, Component> line) {
        LINES.computeIfAbsent(kind, k -> new ArrayList<>()).add(line);
    }

    /** A bar (0 to 1; below 0: hidden) under the stone's gem while it is the one in use (Rewind history, beam heat). */
    public static synchronized void registerBar(StoneKind kind, ToDoubleFunction<Player> bar) {
        BARS.computeIfAbsent(kind, k -> new ArrayList<>()).add(bar);
    }

    /** A Snap near the local player: the white flash. */
    public static void flash(float strength) {
        flash = Math.max(flash, Mth.clamp(strength, 0.0F, 1.0F));
    }

    /** The local player was rejected by a stone: the red edge. */
    public static void burn() {
        burn = 1.0F;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        flash = Math.max(0.0F, flash - 0.08F);
        burn = Math.max(0.0F, burn - 0.05F);
        if (mc.player == null || mc.isPaused()) {
            if (mc.player == null) {
                GRACE.clear();
            }
            return;
        }
        StoneTouch.advance(mc.player, GRACE);
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) {
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        if (flash > 0.01F) {
            graphics.fill(0, 0, w, h, (int) (flash * 200.0F) << 24 | 0xFFFFFF);
        }
        if (burn > 0.01F) {
            int edge = (int) (burn * 140.0F) << 24 | 0xC01020;
            graphics.fillGradient(0, 0, w, 24, edge, edge & 0xFFFFFF);
            graphics.fillGradient(0, h - 24, w, h, edge & 0xFFFFFF, edge);
        }
        Font font = mc.font;
        float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
        drawGrace(graphics, font, player, w, h);
        drawPanel(graphics, font, player, w, h, partial);
    }

    /** The grace of every stone that burns him, in red above the hotbar. */
    private static void drawGrace(GuiGraphics graphics, Font font, LocalPlayer player, int w, int h) {
        int y = h - 74;
        for (Map.Entry<StoneKind, Integer> entry : GRACE.entrySet()) {
            StoneKind kind = entry.getKey();
            int left = StoneTouch.secondsLeft(entry.getValue());
            Component line = Component.translatable("hud.enhanced.stones.burns", kind.displayName(), left)
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            int width = font.width(line) + 22;
            int x = (w - width) / 2;
            float pulse = 0.5F + 0.5F * Mth.sin(player.tickCount * 0.6F);
            int edge = (int) (120 + 100 * pulse) << 24 | 0xD01818;
            graphics.fill(x - 3, y - 3, x + width + 3, y + 14, 0x90000000);
            graphics.renderOutline(x - 3, y - 3, width + 6, 17, edge);
            graphics.renderItem(new ItemStack(ModItems.stone(kind)), x, y - 2);
            graphics.drawString(font, line, x + 20, y + 2, 0xFFFFFF);
            y -= 20;
        }
    }

    /** The gems of what is in his hands, to the right of the hotbar, with their lines. */
    private static void drawPanel(GuiGraphics graphics, Font font, LocalPlayer player, int w, int h, float partial) {
        Panel panel = panel(player);
        if (panel == null) {
            return;
        }
        int x = w / 2 + 91 + 8;
        if (player.getMainArm() == HumanoidArm.LEFT && !player.getOffhandItem().isEmpty()) {
            x += 29;
        }
        int y = h - 20;
        int width = 0;
        for (StoneKind kind : panel.stones) {
            width += kind == panel.selected ? GAP + 2 : GAP - 2;
        }
        graphics.fill(x - 3, y - 3, x + width + 1, y + 19, 0x70000000);
        int at = x;
        for (StoneKind kind : panel.stones) {
            ItemStack gem = new ItemStack(ModItems.stone(kind));
            boolean big = kind == panel.selected;
            if (big) {
                graphics.renderOutline(at - 1, y - 1, 18, 18, 0xFF000000 | kind.color());
                graphics.renderItem(gem, at, y);
                graphics.renderItemDecorations(font, gem, at, y);
                at += GAP + 2;
            } else {
                graphics.pose().pushPose();
                graphics.pose().translate(at, y + 4, 0.0F);
                graphics.pose().scale(0.75F, 0.75F, 1.0F);
                graphics.renderItem(gem, 0, 0);
                graphics.renderItemDecorations(font, gem, 0, 0);
                graphics.pose().popPose();
                at += GAP - 2;
            }
        }
        List<Component> lines = new ArrayList<>();
        List<Double> bars = new ArrayList<>();
        if (panel.selected != null) {
            synchronized (StoneHud.class) {
                for (Function<Player, Component> line : LINES.getOrDefault(panel.selected, List.of())) {
                    Component said = line.apply(player);
                    if (said != null) {
                        lines.add(said);
                    }
                }
                for (ToDoubleFunction<Player> bar : BARS.getOrDefault(panel.selected, List.of())) {
                    double value = bar.applyAsDouble(player);
                    if (value >= 0.0) {
                        bars.add(Math.min(1.0, value));
                    }
                }
            }
        }
        if (panel.snap) {
            long wait = Snap.waitLeft(player);
            lines.add(wait <= 0 ? Component.translatable("hud.enhanced.stones.snap_ready")
                    .withStyle(ChatFormatting.WHITE) : Component.translatable("hud.enhanced.stones.snap_wait",
                    clock(wait)).withStyle(ChatFormatting.GRAY));
        }
        int ty = y - 6;
        for (double bar : bars) {
            int color = panel.selected == null ? 0xFFFFFF : panel.selected.color();
            graphics.fill(x - 1, ty, x + width - 1, ty + 3, 0x90000000);
            graphics.fill(x - 1, ty, x - 1 + (int) ((width) * bar), ty + 3, 0xFF000000 | color);
            ty -= 5;
        }
        for (Component line : lines) {
            ty -= 9;
            int lx = Math.max(2, Math.min(x, w - font.width(line) - 4));
            graphics.drawString(font, line, lx, ty, 0xFFFFFF);
        }
    }

    /** Minutes and seconds of game ticks ("12:05"). */
    static String clock(long ticks) {
        long seconds = (ticks + 19) / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /** What the panel shows: the stones (in socket order), the one in use, and whether the Snap line shows. */
    private record Panel(List<StoneKind> stones, @Nullable StoneKind selected, boolean snap) {
    }

    @Nullable
    private static Panel panel(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (InfinityGauntletItem.crafted(stack)) {
                GauntletContents contents = InfinityGauntletItem.contents(stack);
                if (contents.count() == 0) {
                    continue;
                }
                List<StoneKind> stones = new ArrayList<>();
                for (StoneKind kind : StoneKind.values()) {
                    if (contents.has(kind)) {
                        stones.add(kind);
                    }
                }
                return new Panel(stones, InfinityGauntletItem.selectedOrFirst(contents), contents.full());
            }
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof InfinityStoneItem stone) {
                return new Panel(List.of(stone.kind), stone.kind, false);
            }
            for (StoneKind kind : StoneKind.values()) {
                Item vessel = StoneUse.vessel(kind);
                if (vessel != null && stack.is(vessel)) {
                    return new Panel(List.of(kind), kind, false);
                }
            }
        }
        return null;
    }
}

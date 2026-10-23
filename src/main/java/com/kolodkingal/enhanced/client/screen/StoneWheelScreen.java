package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.network.StoneActionPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.stones.StoneWheel;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Stone Wheel (3.6, the plan's 2.1): held J with a crafted Infinity Gauntlet in a hand opens a ring of its stones
 * round the crosshair; the stone the mouse points at is lit and named in the middle, and letting go of J (or a click)
 * picks it ({@code StoneActionPayload(SELECT, ordinal)}). A tap of J picks the next stone without the wheel ({@link
 * #tick}). The game goes on behind it (it does not pause).
 */
public class StoneWheelScreen extends Screen {
    /** J held this long (ticks) opens the wheel; let go sooner, it is a tap. */
    public static final int HOLD_TICKS = 6;
    private static final int RADIUS = 52;
    private static final int DEAD_ZONE = 12;

    private static boolean wasDown;
    private static int downTicks;
    private static boolean opened;

    private final List<StoneKind> stones;
    @Nullable
    private final StoneKind selected;
    @Nullable
    private StoneKind hovered;
    private boolean chosen;

    public StoneWheelScreen(List<StoneKind> stones, @Nullable StoneKind selected) {
        super(Component.translatable("key.enhanced.stone_wheel"));
        this.stones = stones;
        this.selected = selected;
        this.hovered = selected;
    }

    /** Every client tick: the J key's taps and holds. */
    public static void tick(Minecraft mc) {
        boolean down = mc.screen == null && KeyBindings.STONE_WHEEL.isDown();
        if (mc.player == null) {
            wasDown = false;
            return;
        }
        if (down) {
            downTicks = wasDown ? downTicks + 1 : 0;
            if (!wasDown) {
                opened = false;
            }
            if (!opened && downTicks >= HOLD_TICKS) {
                ItemStack gauntlet = StoneWheel.gauntletInHand(mc.player);
                opened = true;
                if (gauntlet != null) {
                    GauntletContents contents = InfinityGauntletItem.contents(gauntlet);
                    List<StoneKind> stones = new ArrayList<>();
                    for (StoneKind kind : StoneKind.values()) {
                        if (contents.has(kind)) {
                            stones.add(kind);
                        }
                    }
                    Net.sendToServer(new StoneActionPayload(StoneActionPayload.WHEEL_OPEN, 0));
                    mc.setScreen(new StoneWheelScreen(stones, InfinityGauntletItem.selectedOrFirst(contents)));
                }
            }
        } else if (wasDown && !opened && StoneWheel.gauntletInHand(mc.player) != null) {
            Net.sendToServer(new StoneActionPayload(StoneActionPayload.SELECT, -1));
        }
        wasDown = down;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || StoneWheel.gauntletInHand(mc.player) == null) {
            onClose();
            return;
        }
        InputConstants.Key key = KeyBindings.STONE_WHEEL.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM && !InputConstants.isKeyDown(mc.getWindow().getWindow(),
                key.getValue())) {
            choose();
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.STONE_WHEEL.matches(keyCode, scanCode)) {
            choose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        hovered = at(mouseX, mouseY);
        choose();
        return true;
    }

    /** Picks the stone pointed at (none: the wheel just closes) and closes. */
    private void choose() {
        if (chosen) {
            return;
        }
        chosen = true;
        if (hovered != null && hovered != selected) {
            Net.sendToServer(new StoneActionPayload(StoneActionPayload.SELECT, hovered.ordinal()));
        }
        onClose();
    }

    /** The stone whose sector holds the point, or null in the middle. */
    @Nullable
    private StoneKind at(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        if (stones.isEmpty() || dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) {
            return null;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0;
        double sector = Mth.TWO_PI / stones.size();
        int index = Math.floorMod((int) Math.floor((angle + sector / 2.0) / sector), stones.size());
        return stones.get(index);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        StoneKind pointed = at(mouseX, mouseY);
        if (pointed != null) {
            hovered = pointed;
        }
        int cx = width / 2;
        int cy = height / 2;
        ring(graphics, cx, cy, RADIUS + 18, RADIUS - 18, 0x88000000);
        double sector = Mth.TWO_PI / Math.max(1, stones.size());
        for (int i = 0; i < stones.size(); i++) {
            StoneKind kind = stones.get(i);
            double angle = i * sector - Math.PI / 2.0;
            int x = cx + (int) Math.round(Math.cos(angle) * RADIUS);
            int y = cy + (int) Math.round(Math.sin(angle) * RADIUS);
            boolean lit = kind == hovered;
            if (lit) {
                ring(graphics, x, y, 15, 0, 0x90000000 | kind.color());
            } else if (kind == selected) {
                graphics.renderOutline(x - 11, y - 11, 22, 22, 0xC0000000 | kind.color());
            }
            float scale = lit ? 1.6F : 1.2F;
            graphics.pose().pushPose();
            graphics.pose().translate(x - 8 * scale, y - 8 * scale, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.renderItem(new ItemStack(ModItems.stone(kind)), 0, 0);
            graphics.pose().popPose();
        }
        if (hovered != null) {
            Component name = hovered.displayName();
            graphics.drawCenteredString(font, name, cx, cy - 4, 0xFF000000 | hovered.color());
        }
    }

    /** A filled ring (or disc, inner 0) of square cells: cheap, and enough at this size. */
    private static void ring(GuiGraphics graphics, int cx, int cy, int outer, int inner, int argb) {
        for (int dy = -outer; dy <= outer; dy++) {
            int half = (int) Math.sqrt(Math.max(0, outer * outer - dy * dy));
            int hole = Math.abs(dy) < inner ? (int) Math.sqrt(Math.max(0, inner * inner - dy * dy)) : 0;
            if (hole == 0) {
                graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, argb);
            } else {
                graphics.fill(cx - half, cy + dy, cx - hole, cy + dy + 1, argb);
                graphics.fill(cx + hole, cy + dy, cx + half, cy + dy + 1, argb);
            }
        }
    }
}

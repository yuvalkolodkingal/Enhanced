package com.kolodkingal.enhanced.client.serum;

import com.kolodkingal.enhanced.captain.CaptainState;
import com.kolodkingal.enhanced.client.custom.PreviewPuppet;
import com.kolodkingal.enhanced.network.SerumChoicePayload;
import com.kolodkingal.enhanced.network.SerumScreenPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The Super Soldier Serum's choice (3.8, PLAN 2.1): a dark-blue panel, "Super Soldier Serum", and three tall cards side
 * by side, Captain America, Red Hulk and Falcon, each with the hero standing in his look (the Hero Creator's {@link
 * PreviewPuppet}, turning a little) on a backdrop of his colours, his name, one line, and a Choose button. Falcon's
 * card is grey with a padlock while he does not carry the Exo-Wings ("Needs the Falcon Exo-Wings..."). Keys 1, 2, 3
 * pick a card and Enter takes it; Escape or Cancel closes and sends nothing (the serum is kept). The server checks the
 * answer again. The layout shrinks to fit any GUI scale (1-4); in Hebrew the text reads right to left and the cards
 * keep their order.
 */
public class SerumScreen extends Screen {
    /** The serum's heroes, in the order of their cards. */
    static final Power[] HEROES = {Power.CAPTAIN_AMERICA, Power.RED_HULK, Power.FALCON};
    /** Each card's looks (the puppet's) and its backdrop's two colours (top, bottom). */
    static final String[] LOOKS = {"classic", "red_hulk", "endgame"};
    static final int[][] BACKDROPS = {{0xFF2C55B0, 0xFF0D1B40}, {0xFFA02A1E, 0xFF3A0B07}, {0xFF5A6476, 0xFF1C2028}};
    private static final int GAP = 8;
    private static final int TITLE_H = 34;

    private final boolean falconUnlocked;
    private final PreviewPuppet[] puppets = new PreviewPuppet[HEROES.length];
    private final Button[] choose = new Button[HEROES.length];
    private int selected = -1;
    private int cardW;
    private int cardH;
    private int portraitH;
    private int left;
    private int top;
    private float clock;

    public SerumScreen(boolean falconUnlocked) {
        super(Component.translatable("screen.enhanced.serum.title"));
        this.falconUnlocked = falconUnlocked;
    }

    /** Opens the screen (the server says he used a serum). */
    public static void open(SerumScreenPayload payload) {
        Minecraft.getInstance().setScreen(new SerumScreen(payload.falconUnlocked()));
    }

    /** Whether a card may be chosen (Falcon's only with the wings). */
    boolean open(int card) {
        return HEROES[card] != Power.FALCON || falconUnlocked;
    }

    @Override
    protected void init() {
        cardW = Mth.clamp((width - 24 - 2 * GAP) / 3, 70, 112);
        int panelW = 3 * cardW + 2 * GAP;
        int available = height - TITLE_H - 34;
        cardH = Mth.clamp(available, 96, 200);
        portraitH = Math.max(36, cardH - 84);
        left = (width - panelW) / 2;
        top = Math.max(4, (height - (TITLE_H + cardH + 30)) / 2);
        for (int i = 0; i < HEROES.length; i++) {
            int card = i;
            int x = cardX(i);
            Button b = Button.builder(Component.translatable("screen.enhanced.serum.choose"), btn -> send(card))
                    .bounds(x + 4, top + TITLE_H + cardH - 24, cardW - 8, 20).build();
            if (!open(i)) {
                b.active = false;
                b.setTooltip(Tooltip.create(Component.translatable("screen.enhanced.serum.falcon_locked")));
            }
            choose[i] = addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.serum.cancel"), b -> onClose())
                .bounds(width / 2 - 50, top + TITLE_H + cardH + 6, 100, 20).build());
        for (int i = 0; i < HEROES.length; i++) {
            if (puppets[i] == null) {
                puppets[i] = puppet(i);
            }
        }
    }

    /** The hero of a card, standing in his look, his shield on his back, Falcon's wings on (this client only). */
    @Nullable
    private PreviewPuppet puppet(int card) {
        PreviewPuppet puppet = PreviewPuppet.create();
        if (puppet == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("power", HEROES[card].getSerializedName());
        json.addProperty("variant", LOOKS[card]);
        puppet.setData(ModAttachments.POWER, PowerData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        if (HEROES[card] == Power.CAPTAIN_AMERICA) {
            puppet.setData(ModAttachments.CAPTAIN, CaptainState.EMPTY.with(CaptainState.BACK, true));
        } else if (HEROES[card] == Power.FALCON) {
            puppet.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.FALCON_EXO_WINGS.get()));
        }
        return puppet;
    }

    int cardX(int card) {
        return left + card * (cardW + GAP);
    }

    /** The answer: the hero of the card (the server checks it again). */
    void send(int card) {
        if (!open(card)) {
            return;
        }
        PacketDistributor.sendToServer(new SerumChoicePayload(HEROES[card]));
        onClose();
    }

    @Override
    public void tick() {
        clock++;
        for (PreviewPuppet puppet : puppets) {
            if (puppet != null) {
                puppet.tickPreview();
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_3) {
            int card = key - GLFW.GLFW_KEY_1;
            if (open(card)) {
                selected = card;
            }
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && selected >= 0) {
            send(selected);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        for (int i = 0; i < HEROES.length; i++) {
            int x = cardX(i);
            int y = top + TITLE_H;
            if (mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH && open(i)) {
                selected = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
        int panelW = 3 * cardW + 2 * GAP;
        int x0 = left - 8;
        int y0 = top - 4;
        int x1 = left + panelW + 8;
        int y1 = top + TITLE_H + cardH + 30;
        g.fillGradient(x0, y0, x1, y1, 0xEE14275A, 0xEE070E24);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, 0xFF6FA0FF);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(0x8CC4FF).withBold(true)), width / 2,
                top + 4, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.enhanced.serum.subtitle")
                .withStyle(ChatFormatting.GRAY), width / 2, top + 17, 0xFFFFFF);
        for (int i = 0; i < HEROES.length; i++) {
            card(g, i, mouseX, mouseY, partialTick);
        }
        // The buttons over the cards' faces.
        for (Button b : choose) {
            if (b != null) {
                b.render(g, mouseX, mouseY, partialTick);
            }
        }
    }

    private void card(GuiGraphics g, int card, int mouseX, int mouseY, float partialTick) {
        int x = cardX(card);
        int y = top + TITLE_H;
        boolean open = open(card);
        boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH;
        Power hero = HEROES[card];
        // The backdrop and the frame (gold when picked, light when the mouse is on it).
        g.fill(x, y, x + cardW, y + cardH, 0xFF0B1330);
        g.fillGradient(x + 2, y + 2, x + cardW - 2, y + 2 + portraitH, BACKDROPS[card][0], BACKDROPS[card][1]);
        int frame = card == selected ? 0xFFFFD86A : hover && open ? 0xFFB8D4FF : 0xFF3A5A9A;
        g.renderOutline(x, y, cardW, cardH, frame);
        if (card == selected) {
            g.renderOutline(x + 1, y + 1, cardW - 2, cardH - 2, frame);
        }
        // The hero, standing on his backdrop, turning a little.
        PreviewPuppet puppet = puppets[card];
        if (puppet != null) {
            float size = portraitH / 2.15F;
            float yaw = -22.0F + 14.0F * Mth.sin((clock + partialTick) * 0.04F + card);
            g.enableScissor(x + 2, y + 2, x + cardW - 2, y + 2 + portraitH);
            puppet.draw(g, x + cardW / 2, y + portraitH - 3, size, yaw, partialTick);
            g.disableScissor();
        }
        // The key that picks it.
        g.fill(x + 3, y + 3, x + 13, y + 13, 0xC0000000);
        g.drawCenteredString(font, String.valueOf(card + 1), x + 8, y + 4, open ? 0xFFFFFFFF : 0xFF808080);
        // The name and the line.
        int ty = y + portraitH + 6;
        Component name = hero.displayName().copy().withStyle(s -> s.withBold(true)
                .withColor(open ? hero.titleColor() : 0x9A9A9A));
        scaledCentered(g, name, x + cardW / 2, ty, Math.min(1.0F, (cardW - 6) / (float) font.width(name)),
                0xFFFFFFFF);
        Component line = open ? Component.translatable("screen.enhanced.serum." + hero.getSerializedName())
                : Component.translatable("screen.enhanced.serum.falcon_locked");
        // The line in the room left above the button; smaller when it would not fit whole.
        int room = Math.max(1, (cardH - portraitH - 42) / 9);
        float scale = 1.0F;
        List<FormattedCharSequence> lines = font.split(line, cardW - 8);
        while (lines.size() > (int) (room / scale) && scale > 0.55F) {
            scale -= 0.15F;
            lines = font.split(line, (int) ((cardW - 8) / scale));
        }
        int ly = ty + 12;
        int shown = Math.min(lines.size(), (int) (room / scale));
        for (int k = 0; k < shown; k++) {
            scaledCentered(g, lines.get(k), x + cardW / 2, ly + Math.round(k * 9 * scale), scale,
                    open ? 0xFFD8E0EE : 0xFFE0A050);
        }
        if (!open) {
            // Locked: greyed, a padlock over the portrait (in front of the puppet, which is drawn deep).
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 400.0F);
            g.fill(x + 2, y + 2, x + cardW - 2, y + 2 + portraitH, 0xA0202020);
            padlock(g, x + cardW / 2, y + 2 + portraitH / 2);
            g.pose().popPose();
        }
    }

    /** Text centred on {@code cx} at {@code y}, drawn at {@code scale}. */
    private void scaledCentered(GuiGraphics g, Component text, int cx, int y, float scale, int color) {
        scaledCentered(g, text.getVisualOrderText(), cx, y, scale, color);
    }

    private void scaledCentered(GuiGraphics g, FormattedCharSequence text, int cx, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, -font.width(text) / 2, 0, color, true);
        g.pose().popPose();
    }

    /** A padlock, drawn in pixels: the shackle and the body with its keyhole. */
    static void padlock(GuiGraphics g, int cx, int cy) {
        int gold = 0xFFE2B84A;
        int dark = 0xFF8A6A1E;
        // The shackle.
        g.fill(cx - 5, cy - 12, cx + 5, cy - 10, gold);
        g.fill(cx - 7, cy - 10, cx - 5, cy - 2, gold);
        g.fill(cx + 5, cy - 10, cx + 7, cy - 2, gold);
        // The body.
        g.fill(cx - 9, cy - 2, cx + 9, cy + 12, gold);
        g.fill(cx - 9, cy + 10, cx + 9, cy + 12, dark);
        // The keyhole.
        g.fill(cx - 1, cy + 2, cx + 2, cy + 5, 0xFF2A1E08);
        g.fill(cx, cy + 5, cx + 1, cy + 8, 0xFF2A1E08);
    }

    @Override
    public void removed() {
        for (PreviewPuppet puppet : puppets) {
            if (puppet != null) {
                puppet.release();
            }
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

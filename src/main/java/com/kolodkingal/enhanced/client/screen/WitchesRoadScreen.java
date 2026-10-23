package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.block.WitchesRoadDoorBlock;
import com.kolodkingal.enhanced.network.RoadChoicePayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.WitchesRoad;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * "Seekest thou the Road?": a Witches' Road door opened for a seeker holding a Scarlet Witch Gene. Either he is asked to walk the
 * Road ("Walk the Road": the trials, 3.2) or, once he has won them all (or where the trials are off), he chooses whose road to
 * walk, Billy's or Tommy's. The server checks everything again (still no powers, still a gene, still near the open door, and
 * which of the two he may answer). The screen closes by itself when the door does, or when the seeker walks away.
 *
 * <p>At the end of the Road ({@code ending}, 3.2) the same screen is shown inside the Road itself, after the last trial: Billy or
 * Tommy, "Not yet" (close it; the door in the hall or {@code /road choose} brings it back) and "Leave the Road" (back to the world, the
 * Gene kept). It closes when the player is no longer in the Road.
 */
public class WitchesRoadScreen extends Screen {
    private static final int CHOICE_WIDTH = 170;
    private static final int CHOICE_HEIGHT = 44;
    private static final int GAP = 10;
    /** How long the door may take to show as open on the client (ticks). */
    private static final int OPEN_GRACE_TICKS = 20;

    private final BlockPos door;
    private final boolean trials;
    private final int done;
    private final int total;
    private final boolean ending;
    /** Ticks the screen has been up. */
    private int age;

    public WitchesRoadScreen(BlockPos door, boolean trials, int done, int total, boolean ending) {
        super(Component.translatable("screen.enhanced.road.title"));
        this.door = door;
        this.trials = trials;
        this.done = done;
        this.total = total;
        this.ending = ending;
    }

    /** Opens the screen (called when the server says the door opened for us, or the Road asks at its end). */
    public static void open(BlockPos door, boolean trials, int done, int total, boolean ending) {
        Minecraft.getInstance().setScreen(new WitchesRoadScreen(door, trials, done, total, ending));
    }

    @Override
    protected void init() {
        int top = height / 2 - CHOICE_HEIGHT / 2 - 6;
        if (trials) {
            // Power.NONE is the answer "Walk the Road".
            addRenderableWidget(new Choice(width / 2 - CHOICE_WIDTH / 2, top, Power.NONE, WitchesRoad.LIGHT, "screen.enhanced.road.walk"));
        } else {
            int left = width / 2 - CHOICE_WIDTH - GAP / 2;
            addRenderableWidget(new Choice(left, top, Power.BILLY, Power.BILLY.color, "screen.enhanced.road.billy"));
            addRenderableWidget(new Choice(left + CHOICE_WIDTH + GAP, top, Power.TOMMY, Power.TOMMY.glow, "screen.enhanced.road.tommy"));
        }
        if (ending) {
            // Close it for now, or leave the Road with the Gene (the answer "none" at the end of the Road means leaving).
            addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.road.later"), b -> onClose())
                    .bounds(width / 2 - 105, top + CHOICE_HEIGHT + 16, 100, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.road.leave"), b -> choose(Power.NONE))
                    .bounds(width / 2 + 5, top + CHOICE_HEIGHT + 16, 100, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                    .bounds(width / 2 - 50, top + CHOICE_HEIGHT + 16, 100, 20).build());
        }
    }

    private void choose(Power power) {
        Net.sendToServer(new RoadChoicePayload(door, power));
        onClose();
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            onClose();
            return;
        }
        if (ending) {
            // At the end of the Road there is no open door to watch: the screen stays while he is in the Road.
            if (!RoadDimension.is(mc.level)) {
                onClose();
            }
            return;
        }
        boolean open = mc.level.getBlockState(door).getOptionalValue(WitchesRoadDoorBlock.OPEN).orElse(false);
        boolean near = mc.player.position().distanceToSqr(Vec3.atBottomCenterOf(door)) <= WitchesRoad.CHOICE_DISTANCE * WitchesRoad.CHOICE_DISTANCE;
        // The server sends this screen in the same tick it opens the door, and the door's new state can reach us a moment after the
        // screen does: a door that has not shown as open yet gets a second to do so.
        age++;
        if (!near || (!open && age > OPEN_GRACE_TICKS)) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = height / 2 - CHOICE_HEIGHT / 2 - 6;
        graphics.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE).withBold(true).withItalic(true)),
                width / 2, top - 22, 0xFFFFFF);
        if (trials) {
            graphics.drawCenteredString(font, Component.translatable("screen.enhanced.road.progress", Math.min(done + 1, total), total)
                    .withStyle(ChatFormatting.GRAY), width / 2, top - 10, 0xFFFFFF);
        } else if (ending) {
            graphics.drawCenteredString(font, Component.translatable("screen.enhanced.road.ending").withStyle(ChatFormatting.GRAY), width / 2, top - 10, 0xFFFFFF);
            graphics.drawCenteredString(font, Component.translatable("screen.enhanced.road.ending.gene").withStyle(ChatFormatting.DARK_GRAY),
                    width / 2, top + CHOICE_HEIGHT + 4, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int darken(int rgb) {
        return (rgb >> 1) & 0x7F7F7F;
    }

    /** A big button for one road: the hero's name in their colour, and one line about them. */
    private final class Choice extends AbstractButton {
        private final Power power;
        private final int color;
        private final Component description;

        Choice(int x, int y, Power power, int color, String key) {
            super(x, y, CHOICE_WIDTH, CHOICE_HEIGHT, Component.translatable(key));
            this.power = power;
            this.color = color;
            this.description = Component.translatable(key + ".desc");
        }

        @Override
        public void onPress() {
            choose(power);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = isHoveredOrFocused();
            int x = getX();
            int y = getY();
            graphics.fill(x, y, x + width, y + height, lit ? 0xE0182436 : 0xC0101824);
            graphics.renderOutline(x, y, width, height, 0xFF000000 | (lit ? color : darken(color)));
            if (lit) {
                graphics.renderOutline(x + 1, y + 1, width - 2, height - 2, 0x80000000 | color);
            }
            Component name = getMessage().copy().withStyle(s -> s.withColor(color).withBold(true));
            graphics.drawCenteredString(font, name, x + width / 2, y + 10, 0xFFFFFF);
            graphics.drawCenteredString(font, description.copy().withStyle(ChatFormatting.GRAY), x + width / 2, y + 26, 0xFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

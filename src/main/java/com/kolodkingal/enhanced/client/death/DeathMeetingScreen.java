package com.kolodkingal.enhanced.client.death;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.client.asgard.AsgardianRenderer;
import com.kolodkingal.enhanced.death.DeathMeeting;
import com.kolodkingal.enhanced.network.DeathMeetingPayload;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Death's screen (3.7, Eilon's request): in place of the plain death screen, the player meets Rio Vidal, the Green
 * Witch, in a misty green clearing: vines overhead, grass and small flowers underfoot, mist drifting by and leaves and
 * petals falling. She stands large on the left in her cottage skin (the {@code cottage_resident} entity as Rio, drawn
 * by its own renderer, her eyes following the mouse); on the right, the title, one short line of hers (a few, EN/HE),
 * how he died and his score, and his choice: <b>Start again</b> (vanilla's respawn) and <b>Start again in the
 * Nether</b> (when the server offers it), with vanilla's small <b>Title screen</b> under them. As vanilla's, its
 * buttons wake after a second, Esc does not close it, and it is a {@link DeathScreen} (so a respawn closes it as
 * vanilla's). Gentle and calm: nothing scary, nothing gory.
 */
public class DeathMeetingScreen extends DeathScreen {
    /** How many lines Rio has ({@code screen.enhanced.death_meeting.line.<n>}). */
    public static final int LINES = 6;

    static final ResourceLocation MIST = Enhanced.id("textures/gui/death/mist.png");
    static final ResourceLocation LEAVES = Enhanced.id("textures/gui/death/leaves.png");
    static final ResourceLocation GLOW = Enhanced.id("textures/gui/death/glow.png");
    static final ResourceLocation VINES = Enhanced.id("textures/gui/death/vines.png");
    static final ResourceLocation GRASS = Enhanced.id("textures/gui/death/grass.png");

    /** The ticks before the buttons wake (vanilla's second, so a click meant for the fight does not choose). */
    static final int WAKE_TICKS = 20;
    /** The ticks a chosen road waits for its respawn before the buttons wake again. */
    static final int RETRY_TICKS = 100;
    private static final int TITLE_COLOR = 0xD6F5CF;
    private static final int LINE_COLOR = 0xE8F8DC;
    private static final int NAME_COLOR = 0x9BE39A;
    private static final int NETHER_COLOR = 0xFFB46B;
    private static final int LEAF_COUNT = 34;

    @Nullable
    private final Component cause;
    private final boolean nether;
    private final int fireSeconds;
    private final int line;
    private final Component heading = Component.translatable("screen.enhanced.death_meeting.title");
    private final Component saying;
    private final Component name = Component.translatable("screen.enhanced.death_meeting.rio");
    private final List<Leaf> leaves = new ArrayList<>();
    private final List<Button> roads = new ArrayList<>();
    private Component score = CommonComponents.EMPTY;
    @Nullable
    private AsgardianEntity rio;
    private boolean rioTried;
    /** A road is taken: the buttons stay asleep (also after a resize) until the respawn closes the screen. */
    private boolean chosen;
    private int chosenAt;
    private int age;

    // The layout (set in init).
    private int columnX;
    private int columnWidth;
    private int rioX;
    private int rioFeet;
    private int rioScale;
    private int titleY;
    private int boxY;
    private int boxBottom;
    private List<FormattedCharSequence> sayingLines = List.of();
    private List<FormattedCharSequence> causeLines = List.of();
    private int causeY;
    private int scoreY;

    /** One falling leaf or petal: where it starts, how it falls, sways and turns, and which of the four it is. */
    private record Leaf(float x, float y, float fall, float sway, float phase, float spin, float turn, int kind,
            int size,
                        boolean near) {
    }

    public DeathMeetingScreen(@Nullable Component cause, boolean nether, int fireSeconds, int line) {
        // Vanilla's own fields get no cause: this screen draws it itself.
        super(null, false);
        this.cause = cause;
        this.nether = nether;
        this.fireSeconds = fireSeconds;
        this.line = Math.floorMod(line, LINES);
        this.saying = Component.translatable("screen.enhanced.death_meeting.line." + this.line)
                .withStyle(ChatFormatting.ITALIC);
        RandomSource random = RandomSource.create(4_2024L + this.line);
        for (int i = 0; i < LEAF_COUNT; i++) {
            boolean near = i % 3 == 0;
            leaves.add(new Leaf(random.nextFloat(), random.nextFloat(), 0.25F + random.nextFloat() * (near ? 0.45F
                    : 0.25F),
                    4.0F + random.nextFloat() * 10.0F, random.nextFloat() * Mth.TWO_PI, 0.6F
                            + random.nextFloat() * 1.8F,
                    random.nextFloat() * 360.0F, random.nextInt(4), near ? 9 + random.nextInt(4) : 6
                            + random.nextInt(3),
                    near));
        }
    }

    // ---- layout and buttons ----

    @Override
    protected void init() {
        roads.clear();
        columnWidth = Math.min(220, Math.max(160, (int) (width * 0.52F)));
        columnX = width - columnWidth - Math.max(6, width / 24);
        int centre = columnX + columnWidth / 2;
        rioX = Math.max(30, columnX / 2);
        rioFeet = height - 16;
        int tallest = Math.min((int) (height * 0.62F), (int) (columnX * 1.3F));
        rioScale = Math.max(20, (int) (tallest / 1.8F));

        titleY = Math.max(8, (int) (height * 0.06F));
        boxY = titleY + 26;
        sayingLines = font.split(saying, columnWidth - 20);
        boxBottom = boxY + 8 + sayingLines.size() * 10;
        causeLines = cause == null ? List.of() : font.split(cause, columnWidth);
        if (causeLines.size() > 2) {
            causeLines = causeLines.subList(0, 2);
        }
        causeY = boxBottom + 9;
        scoreY = causeY + causeLines.size() * 10 + 2;
        score = Component.translatable("deathScreen.score.value",
                Component.literal(Integer.toString(minecraft.player.getScore())).withStyle(ChatFormatting.YELLOW));

        int buttonWidth = Math.min(200, columnWidth);
        int buttonX = centre - buttonWidth / 2;
        int y = Math.max(scoreY + 16, Math.min(height - 80, (int) (height * 0.48F)));
        Button again = addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.death_meeting.again"),
                        button -> startAgain())
                .bounds(buttonX, y, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.enhanced.death_meeting.again.tooltip")))
                .build());
        roads.add(again);
        y += 24;
        if (nether) {
            Component road = Component.translatable("screen.enhanced.death_meeting.nether").withColor(NETHER_COLOR);
            Button hell = addRenderableWidget(Button.builder(road, button -> startInTheNether())
                    .bounds(buttonX, y, buttonWidth, 20)
                    .tooltip(Tooltip.create(fireSeconds > 0
                            ? Component.translatable("screen.enhanced.death_meeting.nether.tooltip", fireSeconds)
                            : Component.translatable("screen.enhanced.death_meeting.nether.tooltip_plain")))
                    .build());
            roads.add(hell);
            y += 24;
        }
        int small = Math.min(110, buttonWidth);
        Button title = addRenderableWidget(Button.builder(Component.translatable("deathScreen.titleScreen"),
                        button -> minecraft.getReportingContext().draftReportHandled(minecraft, this, this::toTitle,
                                true))
                .bounds(centre - small / 2, y + 4, small, 16)
                .build());
        roads.add(title);
        for (Button button : roads) {
            button.active = age >= WAKE_TICKS && !chosen;
        }
    }

    private void startAgain() {
        sleep();
        minecraft.player.respawn();
    }

    private void startInTheNether() {
        sleep();
        PacketDistributor.sendToServer(new DeathMeetingPayload.Choose(DeathMeeting.NETHER));
    }

    /** One road is taken: the buttons go quiet until the respawn closes the screen. */
    private void sleep() {
        chosen = true;
        chosenAt = age;
        for (Button button : roads) {
            button.active = false;
        }
    }

    /** Vanilla's way to the title screen: asked first ("respawn" answers it as vanilla does). */
    private void toTitle() {
        DeathScreen.TitleConfirmScreen confirm = new DeathScreen.TitleConfirmScreen(yes -> {
            if (yes) {
                if (minecraft.level != null) {
                    minecraft.level.disconnect();
                }
                minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
                minecraft.setScreen(new TitleScreen());
            } else {
                minecraft.player.respawn();
                minecraft.setScreen(null);
            }
        }, Component.translatable("deathScreen.quit.confirm"), CommonComponents.EMPTY,
                Component.translatable("deathScreen.titleScreen"), Component.translatable("deathScreen.respawn"));
        minecraft.setScreen(confirm);
        confirm.setDelay(20);
    }

    @Override
    public void tick() {
        age++;
        if (chosen && age - chosenAt > RETRY_TICKS) {
            // No respawn came (the answer was lost): the roads wake again.
            chosen = false;
        }
        if (age == WAKE_TICKS || age == chosenAt + RETRY_TICKS + 1) {
            for (Button button : roads) {
                button.active = !chosen;
            }
        }
        if (rio != null) {
            rio.tickCount++;
        }
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(heading, saying);
    }

    // ---- drawing ----

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float t = age + partialTick;
        float fade = Mth.clamp(t / 12.0F, 0.0F, 1.0F);
        int top = ((int) (0xC8 * fade) << 24) | 0x06140C;
        int bottom = ((int) (0xF0 * fade) << 24) | 0x12301C;
        graphics.fillGradient(0, 0, width, height, top, bottom);
        RenderSystem.enableBlend();
        mist(graphics, t, height * 0.18F, 0.18F, 0.30F * fade, 0.55F);
        glow(graphics, t, fade);
        tiled(graphics, VINES, 128, 40, false, 0.95F * fade);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float t = age + partialTick;
        float fade = Mth.clamp(t / 12.0F, 0.0F, 1.0F);
        renderBackground(graphics, mouseX, mouseY, partialTick);
        leaves(graphics, t, false, fade);
        drawRio(graphics, mouseX, mouseY, partialTick);
        RenderSystem.enableBlend();
        mist(graphics, t, height * 0.62F, -0.32F, 0.22F * fade, 0.35F);
        tiled(graphics, GRASS, 128, 24, true, fade);
        leaves(graphics, t, true, fade);
        graphics.drawCenteredString(font, name, rioX, height - 12, NAME_COLOR);
        drawColumn(graphics, fade);
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** The right-hand column: the title, Rio's line in its box, the cause and the score, on a soft dark panel. */
    private void drawColumn(GuiGraphics graphics, float fade) {
        int centre = columnX + columnWidth / 2;
        int alpha = (int) (0x60 * fade) << 24;
        graphics.fill(columnX - 6, titleY - 6, columnX + columnWidth + 6, height - 8, alpha | 0x04100A);
        graphics.pose().pushPose();
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.drawCenteredString(font, heading, centre / 2, titleY / 2, TITLE_COLOR);
        graphics.pose().popPose();
        // Rio's line in a bubble whose little tail points at her.
        int left = columnX + 2;
        int right = columnX + columnWidth - 2;
        graphics.fill(left, boxY, right, boxBottom, 0xC0143222);
        graphics.renderOutline(left, boxY, right - left, boxBottom - boxY, 0xFF5FA86A);
        int tailY = boxY + (boxBottom - boxY) / 2;
        for (int i = 1; i <= 4; i++) {
            graphics.fill(left - i, tailY - (4 - i), left - i + 1, tailY + (4 - i) + 1, 0xFF5FA86A);
        }
        int y = boxY + 5;
        for (FormattedCharSequence text : sayingLines) {
            graphics.drawCenteredString(font, text, centre, y, LINE_COLOR);
            y += 10;
        }
        y = causeY;
        for (FormattedCharSequence text : causeLines) {
            graphics.drawCenteredString(font, text, centre, y, 0xFFFFFF);
            y += 10;
        }
        graphics.drawCenteredString(font, score, centre, scoreY, 0xFFFFFF);
    }

    /** A band of mist across the screen, drifting sideways ({@code speed} pixels per tick, wrapping). */
    private void mist(GuiGraphics graphics, float t, float y, float speed, float alpha, float tall) {
        int w = (int) (width * 1.25F);
        int h = (int) (height * tall);
        float shift = Mth.positiveModulo(t * speed, w);
        graphics.setColor(0.80F, 1.0F, 0.84F, alpha);
        for (int i = -1; i <= 1; i++) {
            int x = (int) (i * w + shift);
            graphics.blit(MIST, x, (int) y - h / 2, w, h, 0.0F, 0.0F, 256, 64, 256, 64);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Rio's green light behind her, breathing slowly. */
    private void glow(GuiGraphics graphics, float t, float fade) {
        float breath = 0.85F + 0.15F * Mth.sin(t * 0.05F);
        int size = (int) (rioScale * 2.6F * breath);
        int cy = rioFeet - (int) (rioScale * 0.95F);
        graphics.setColor(0.45F, 0.95F, 0.55F, 0.42F * fade);
        graphics.blit(GLOW, rioX - size / 2, cy - size / 2, size, size, 0.0F, 0.0F, 64, 64, 64, 64);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** A strip (the vines at the top, the grass at the bottom) tiled across the width at twice its size. */
    private void tiled(GuiGraphics graphics, ResourceLocation texture, int tw, int th, boolean bottom, float alpha) {
        int w = tw * 2;
        int h = th * 2;
        int y = bottom ? height - h : 0;
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        for (int x = 0; x < width; x += w) {
            graphics.blit(texture, x, y, w, h, 0.0F, 0.0F, tw, th, tw, th);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The leaves and petals: the far ones small and dim behind Rio, the near ones bigger in front of her. */
    private void leaves(GuiGraphics graphics, float t, boolean near, float fade) {
        float span = height + 32.0F;
        for (Leaf leaf : leaves) {
            if (leaf.near() != near) {
                continue;
            }
            float y = Mth.positiveModulo(leaf.y() * span + t * leaf.fall(), span) - 16.0F;
            float x = leaf.x() * (width + 40.0F) - 20.0F + Mth.sin(t * 0.035F * leaf.spin()
                    + leaf.phase()) * leaf.sway()
                    + t * 0.08F;
            x = Mth.positiveModulo(x + 20.0F, width + 40.0F) - 20.0F;
            float angle = leaf.turn() + t * leaf.spin() * 2.0F;
            int size = leaf.size();
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
            graphics.setColor(1.0F, 1.0F, 1.0F, (near ? 0.95F : 0.6F) * fade);
            graphics.blit(LEAVES, -size / 2, -size / 2, size, size, leaf.kind() * 16.0F, 0.0F, 16, 16, 64, 16);
            graphics.pose().popPose();
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Rio herself: the cottage's Rio (the {@code cottage_resident} entity as {@link Role#RIO}) drawn large, standing,
     * breathing, turning a little toward the mouse. Without a level to make her in, her face from her skin.
     */
    private void drawRio(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        AsgardianEntity npc = rio();
        if (npc == null) {
            int size = rioScale;
            graphics.blit(AsgardianRenderer.texture(Role.RIO, 0), rioX - size / 2, rioFeet - size * 2, size, size,
                    8.0F, 8.0F, 8, 8, 64, 64);
            return;
        }
        float headY = rioFeet - rioScale * 1.5F;
        float turnX = (float) Math.atan((rioX - mouseX) / 80.0F);
        float turnY = (float) Math.atan((headY - mouseY) / 80.0F);
        float body = 180.0F + turnX * 12.0F;
        npc.yBodyRot = body;
        npc.yBodyRotO = body;
        npc.setYRot(180.0F + turnX * 30.0F);
        npc.yRotO = npc.getYRot();
        npc.yHeadRot = npc.getYRot();
        npc.yHeadRotO = npc.getYRot();
        npc.setXRot(-turnY * 18.0F);
        npc.xRotO = npc.getXRot();

        graphics.pose().pushPose();
        graphics.pose().translate(rioX, rioFeet, 50.0F);
        graphics.pose().scale(rioScale, rioScale, -rioScale);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf tilt = new Quaternionf().rotateX(-4.0F * Mth.DEG_TO_RAD);
        pose.mul(tilt);
        graphics.pose().mulPose(pose);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.overrideCameraOrientation(tilt.conjugate(new Quaternionf()).rotateY((float) Math.PI));
        dispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> dispatcher.render(npc, 0.0, 0.0, 0.0, 0.0F, partialTick, graphics.pose(),
                graphics.bufferSource(), 0xF000F0));
        graphics.flush();
        dispatcher.setRenderShadow(true);
        graphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    /** Rio, made once (client side only, never added to the level). */
    @Nullable
    private AsgardianEntity rio() {
        if (!rioTried) {
            rioTried = true;
            try {
                if (minecraft.level != null) {
                    rio = ModEntities.COTTAGE_RESIDENT.get().create(minecraft.level);
                    if (rio != null) {
                        rio.setRole(Role.RIO, 0);
                    }
                }
            } catch (RuntimeException e) {
                Enhanced.LOGGER.warn("Rio could not be drawn on Death's screen", e);
                rio = null;
            }
        }
        return rio;
    }
}

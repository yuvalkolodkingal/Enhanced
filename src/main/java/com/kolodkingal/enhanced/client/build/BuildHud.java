package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BillyMagic;
import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildGrammar;
import com.kolodkingal.enhanced.build.BuildHints;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellMath;
import com.kolodkingal.enhanced.build.SpellPlanner;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.build.HudDraw.Picture;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build Magic on the builder's screen (D-B 6.3 to 6.7), the layer {@code enhanced:build_magic} registered below the
 * debug overlay. The sixth line of the hero panel ({@code PowerHud}) is not drawn here.
 *
 * <ul>
 *     <li><b>The badge</b>, 16 pixels right of the crosshair (left of it when the screen is too narrow): the spell's
 *     picture and the material's (a pulsing "?" with none, a small eye on a picked block, two of the hotbar's blocks
 *     for a mix), the spell and its style over the size; Billy's meter (60 pixels, refilling smoothly, the cost a
 *     darker chunk at its end; orange with a clock and the seconds while it does not fit yet; flashing red when it
 *     never will), "cost / meter ✦", his stars and rank (a "+" sparkle and the new star growing at a rank-up), Mom's
 *     scarlet stripe and "½ Mom's help", "too far"; the Scarlet Witch's "∞" and the blocks her spell changes.</li>
 *     <li><b>The rings</b> round the crosshair: the hold ring (red for a removal, the hero's colour for a huge spell;
 *     let go early, it unwinds), and while a spell of the hero works, the progress ring with its percentage and the
 *     Scarlet Witch's waiting spells as dots under it (in build mode or out of it).</li>
 *     <li><b>The done pop</b>: "✓ 140" rises out of the badge for {@value #POP_TICKS} ticks after a spell.</li>
 *     <li><b>The hint strip</b> at the bottom right ({@link BuildHints#lines}; client option {@code hints}), over the
 *     subtitles while they show, over the food bar when it would run into the hotbar.</li>
 *     <li><b>The tips</b> ({@link BuildTips}; client option {@code tips}): a card over the action bar.</li>
 *     <li><b>The crosshair</b> becomes a small rune in the hero's colour (Billy's rune ring, the Scarlet Witch's
 *     hexagram), and <b>the screen's edges</b> take a faint tint (alpha 0.06) in it, so the mode is clear at a
 *     glance.</li>
 * </ul>
 *
 * <p>Reduced motion: nothing turns, pulses, blinks, bobs, flashes or rises. A right-to-left language (Hebrew) reads
 * every line right to left ({@link HudDraw#visual}); the hint strip and the tip card put the picture first, on the
 * right.
 *
 * <p>Only ever loaded on a client.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class BuildHud implements LayeredDraw.Layer {
    /** How long the done pop rises (D-B 6.3). */
    public static final int POP_TICKS = 40;
    /** How far right of the crosshair the badge starts (D-B 6.3). */
    private static final int BADGE_GAP = 16;
    private static final int PAD = 3;
    /** The action bar's line stands in the band from this far above the bottom (its text is centred 68 above it). */
    private static final int MESSAGE_BAND = 62;
    private static final int METER_W = 60;
    private static final int METER_H = 5;
    /** The edge tint's alpha at the very edge (D-B 6.3). */
    private static final float TINT = 0.06F;
    /** Ticks after a new player entity (a login, a respawn) before a higher rank is a rank-up rather than the sync. */
    private static final int RANK_SETTLE_TICKS = 100;
    private static final int RANK_UP_TICKS = 40;
    private static final int UNWIND_TICKS = 6;
    private static final int ROW_H = 14;
    /** The most of the Scarlet Witch's waiting spells drawn as dots; more say how many. */
    private static final int MAX_DOTS = 8;

    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;
    private static final int DARK_GREY = 0x6E6E6E;
    private static final int ORANGE = 0xFFAA33;
    private static final int RED = 0xFF5050;
    private static final int REMOVAL = 0xFF4A4A;
    private static final int GOLD = 0xFFD54A;

    // What the last tick saw.
    private static boolean wasBuilding;
    private static @Nullable Spell seenSpell;
    private static BuildHints.Stage seenStage = BuildHints.Stage.NOTHING;
    private static @Nullable BuildVerdictPayload seenVerdict;
    private static @Nullable LocalPlayer seenPlayer;
    private static long playerSince;
    private static int seenRank;
    private static long rankUpAt = Long.MIN_VALUE / 2;
    private static long popAt = Long.MIN_VALUE / 2;
    private static int popCount;
    private static float unwindFrom;
    private static boolean unwindHuge;
    private static long unwindAt = Long.MIN_VALUE / 2;
    private static boolean tooFar;
    /** Where the hint strip stood in the last frame (its left edge and top), or {@code Integer.MAX_VALUE} when it was not drawn. */
    private static int stripLeft = Integer.MAX_VALUE;
    private static int stripTop = Integer.MAX_VALUE;
    /** Whether the action bar's line was moved aside this frame (the pose to put back). */
    private static boolean messageShifted;

    private static final Subtitles SUBTITLES = new Subtitles();
    private static boolean listening;

    // ------------------------------------------------------------------------------------------------------------
    // Every tick

    /**
     * One client tick ({@link BuildInput} calls it after {@link BuildClient#tick}): the tips of a spell chosen, a box
     * ready or a spell done, the done pop, a rank-up, whether Billy looks at a block beyond his reach.
     */
    static void tick(LocalPlayer player) {
        long now = BuildClient.now();
        boolean building = BuildClient.buildMode();
        BuildGrammar g = BuildClient.grammar();
        BuildHints.Stage stage = building ? BuildClient.stage() : BuildHints.Stage.NOTHING;
        if (building && !wasBuilding) {
            BuildTips.started(g.spell());
        } else if (building && g.spell() != seenSpell) {
            BuildTips.chosen(g.spell());
        }
        if (building && ready(stage) && (seenStage == BuildHints.Stage.NOTHING || seenStage == BuildHints.Stage.CORNER)) {
            BuildTips.boxReady(g.spell());
        }
        BuildVerdictPayload verdict = BuildClient.lastVerdict().orElse(null);
        if (verdict != null && verdict != seenVerdict) {
            seenVerdict = verdict;
            if (done(verdict.code())) {
                popAt = now;
                popCount = verdict.changed();
                if (building && verdict.code() != BuildVerdictPayload.REDONE) {
                    BuildTips.done(g.spell());
                }
            }
        }
        wasBuilding = building;
        seenSpell = g.spell();
        seenStage = stage;

        BuildData data = player.getExistingData(ModAttachments.BUILD_DATA).orElse(BuildData.EMPTY);
        int rank = BillyRanks.rank(data);
        if (player != seenPlayer) {
            seenPlayer = player;
            playerSince = now;
        } else if (rank > seenRank && now - playerSince > RANK_SETTLE_TICKS) {
            rankUpAt = now;
        }
        seenRank = rank;

        BuildGrammar.Aim aim = BuildClient.aim();
        tooFar = building && aim != null && aim.tooFar();
        BuildTips.tick(building, stage);
    }

    /** The right button was let go before the ring was full: the ring unwinds from where it was. */
    static void unwound(float from, boolean huge) {
        unwindFrom = from;
        unwindHuge = huge;
        unwindAt = BuildClient.now();
    }

    /** Forgets the moment (at logout): the pop, the rank seen, the tips and their counts. */
    static void clear() {
        wasBuilding = false;
        seenSpell = null;
        seenStage = BuildHints.Stage.NOTHING;
        seenVerdict = null;
        seenPlayer = null;
        rankUpAt = Long.MIN_VALUE / 2;
        popAt = Long.MIN_VALUE / 2;
        unwindAt = Long.MIN_VALUE / 2;
        tooFar = false;
        BuildTips.clear();
    }

    private static boolean ready(BuildHints.Stage stage) {
        return stage == BuildHints.Stage.BOX || stage == BuildHints.Stage.BOX_HOLD;
    }

    private static boolean done(int code) {
        return code == BuildVerdictPayload.DONE || code == BuildVerdictPayload.DONE_PARTLY || code == BuildVerdictPayload.REDONE;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The crosshair

    /** In build mode, in first person, the rune stands where vanilla's crosshair would. */
    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName()) && runeShown(mc)) {
            event.setCanceled(true);
        } else if (VanillaGuiLayers.OVERLAY_MESSAGE.equals(event.getName())) {
            // The action bar's line (the words of a cast, an undo, a refusal) never runs under the hint strip: it stands aside.
            float shift = messageShift(mc, event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight());
            if (shift != 0.0F) {
                event.getGuiGraphics().pose().pushPose();
                event.getGuiGraphics().pose().translate(shift, 0.0F, 0.0F);
                messageShifted = true;
            }
        }
    }

    /** The action bar's layer is drawn: puts back the shift of its line. */
    @SubscribeEvent
    public static void onLayerDrawn(RenderGuiLayerEvent.Post event) {
        if (messageShifted && VanillaGuiLayers.OVERLAY_MESSAGE.equals(event.getName())) {
            event.getGuiGraphics().pose().popPose();
            messageShifted = false;
        }
    }

    /**
     * How far left the action bar's line goes so that it ends before the hint strip drawn last frame (0 when it is
     * clear of it, or when there is no line): the strip's top row stands at the line's height only when the strip is
     * four rows or more (the line is centred, 68 above the bottom). At most as far as keeps the line on the screen.
     */
    private static float messageShift(Minecraft mc, int width, int height) {
        Component line = mc.gui.overlayMessageString;
        if (line == null || mc.gui.overlayMessageTime <= 0 || stripLeft == Integer.MAX_VALUE || stripTop >= height - MESSAGE_BAND) {
            return 0.0F;
        }
        int w = mc.font.width(line) + 4;
        float right = width / 2.0F + w / 2.0F;
        float shift = Math.min(0.0F, stripLeft - 6 - right);
        // (A line wider than the room it has keeps the screen's edge, and runs under the strip's corner.)
        return Math.min(0.0F, Math.max(shift, 2.0F - (width / 2.0F - w / 2.0F)));
    }

    private static boolean runeShown(Minecraft mc) {
        return shown(mc) && BuildClient.buildMode() && mc.options.getCameraType().isFirstPerson();
    }

    /** Whether the layer draws at all: a builder alive in a world, the GUI not hidden, no debug screen. */
    private static boolean shown(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && mc.level != null && !mc.options.hideGui && !mc.getDebugOverlay().showDebugScreen() && !player.isDeadOrDying()
                && BuildClient.isBuilder(Powers.power(player));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!listening) {
            mc.getSoundManager().addListener(SUBTITLES);
            listening = true;
        }
        stripLeft = Integer.MAX_VALUE;
        stripTop = Integer.MAX_VALUE;
        if (!shown(mc)) {
            return;
        }
        LocalPlayer player = mc.player;
        Power power = Powers.power(player);
        float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
        float time = BuildClient.now() + partial;
        boolean calm = EnhancedClientConfig.reducedMotion();
        boolean building = BuildClient.buildMode();
        boolean screen = mc.screen != null && !(mc.screen instanceof ChatScreen);
        BuildData data = player.getExistingData(ModAttachments.BUILD_DATA).orElse(BuildData.EMPTY);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // Vanilla's crosshair is 15 pixels with its top left at ((w - 15) / 2, (h - 15) / 2).
        float cx = (width - 15) / 2 + 7.5F;
        float cy = (height - 15) / 2 + 7.5F;

        if (building) {
            edges(graphics, width, height, power.color);
            if (mc.options.getCameraType().isFirstPerson()) {
                rune(graphics, cx, cy, power, time, calm);
            }
        }
        rings(graphics, mc.font, cx, cy, power, data, building, time, calm);
        int[] badge = null;
        if (building && !screen) {
            badge = badge(graphics, mc, power, data, cx, cy, time, calm);
        }
        pop(graphics, mc.font, power, badge, cx, cy, time, calm);
        if (building && !screen && EnhancedClientConfig.buildHints()) {
            strip(graphics, mc, power, data, width, height);
        }
        if (building && !screen) {
            tip(graphics, mc, power, badge, width, height, partial, calm);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The rune and the edges

    /** The screen's edges in the hero's colour, alpha {@value #TINT} at the edge, nothing a fifth of the way in. */
    private static void edges(GuiGraphics g, int width, int height, int rgb) {
        int band = Math.max(24, height / 5);
        int edge = HudDraw.argb(rgb, TINT);
        int none = HudDraw.argb(rgb, 0.0F);
        g.fillGradient(0, 0, width, band, edge, none);
        g.fillGradient(0, height - band, width, height, none, edge);
        HudDraw.sideways(g, 0, 0, band, height, edge, none);
        HudDraw.sideways(g, width - band, 0, width, height, none, edge);
    }

    /**
     * The crosshair as a small rune in the hero's colour, over a dark shadow so it is seen on any ground: Billy's rune
     * ring (a ring and four turning marks), the Scarlet Witch's turning hexagram; a bright dot in the middle.
     */
    private static void rune(GuiGraphics g, float cx, float cy, Power power, float time, boolean calm) {
        int core = HudDraw.argb(power.color, 0.95F);
        int shade = HudDraw.argb(0x000000, 0.45F);
        if (power == Power.BILLY) {
            float turn = calm ? 0.0F : time / 160.0F;
            HudDraw.ring(g, cx, cy, 3.4F, 5.6F, 0.0F, 1.0F, shade);
            HudDraw.ring(g, cx, cy, 4.0F, 5.0F, 0.0F, 1.0F, core);
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2.0 * (turn + 0.125 + i * 0.25);
                float sx = (float) Math.sin(a);
                float sy = (float) -Math.cos(a);
                HudDraw.line(g, cx + sx * 5.6F, cy + sy * 5.6F, cx + sx * 7.8F, cy + sy * 7.8F, 2.2F, shade);
                HudDraw.line(g, cx + sx * 5.8F, cy + sy * 5.8F, cx + sx * 7.6F, cy + sy * 7.6F, 1.0F, core);
            }
        } else {
            float turn = calm ? 0.0F : time / 240.0F;
            for (int pass = 0; pass < 2; pass++) {
                for (int t = 0; t < 2; t++) {
                    for (int i = 0; i < 3; i++) {
                        double a0 = Math.PI * 2.0 * (turn + t / 6.0 + i / 3.0);
                        double a1 = Math.PI * 2.0 * (turn + t / 6.0 + (i + 1) / 3.0);
                        float r = 6.0F;
                        HudDraw.line(g, cx + (float) Math.sin(a0) * r, cy - (float) Math.cos(a0) * r, cx + (float) Math.sin(a1) * r,
                                cy - (float) Math.cos(a1) * r, pass == 0 ? 2.2F : 1.0F, pass == 0 ? shade : core);
                    }
                }
            }
        }
        g.flush();
        int x = Mth.floor(cx);
        int y = Mth.floor(cy);
        g.fill(x - 1, y - 1, x + 2, y + 2, shade);
        g.fill(x, y, x + 1, y + 1, HudDraw.argb(power.glow, 1.0F));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The rings

    /**
     * The hold ring (while the right button is held for a spell that needs it, and as it unwinds when let go early) and
     * the progress ring of the hero's own working spell, with its percentage and the waiting spells as dots.
     */
    private static void rings(GuiGraphics g, Font font, float cx, float cy, Power power, BuildData data, boolean building, float time,
                              boolean calm) {
        int track = HudDraw.argb(0x000000, 0.45F);
        float hold = building ? BuildClient.holdProgress() : -1.0F;
        if (hold >= 0.0F) {
            int colour = BuildClient.holdHuge() ? power.color : REMOVAL;
            HudDraw.ring(g, cx, cy, 8.5F, 11.0F, 0.0F, 1.0F, track);
            HudDraw.ring(g, cx, cy, 8.5F, 11.0F, 0.0F, hold, HudDraw.argb(colour, 0.95F));
        } else if (!calm && time - unwindAt < UNWIND_TICKS) {
            float left = unwindFrom * (1.0F - (time - unwindAt) / UNWIND_TICKS);
            HudDraw.ring(g, cx, cy, 8.5F, 11.0F, 0.0F, left, HudDraw.argb(unwindHuge ? power.color : REMOVAL, 0.6F));
        }
        if (!data.working()) {
            return;
        }
        float progress = Mth.clamp(data.progress() / 1000.0F, 0.0F, 1.0F);
        HudDraw.ring(g, cx, cy, 13.0F, 15.0F, 0.0F, 1.0F, track);
        HudDraw.ring(g, cx, cy, 13.0F, 15.0F, 0.0F, progress, HudDraw.argb(power.glow, 0.95F));
        FormattedCharSequence percent = HudDraw.visual(Component.translatable("hud.enhanced.build.progress", Math.round(progress * 100.0F)));
        int y = Mth.floor(cy) + 18;
        HudDraw.text(g, font, percent, Mth.floor(cx) - font.width(percent) / 2, y, HudDraw.argb(WHITE, 1.0F), true);
        int queued = data.queued();
        if (queued <= 0) {
            return;
        }
        y += 11;
        if (queued > MAX_DOTS) {
            FormattedCharSequence waiting = HudDraw.visual(Component.translatable("hud.enhanced.build.queued", queued));
            HudDraw.text(g, font, waiting, Mth.floor(cx) - font.width(waiting) / 2, y, HudDraw.argb(GREY, 1.0F), true);
            return;
        }
        int x = Mth.floor(cx) - (queued * 4 - 2) / 2;
        for (int i = 0; i < queued; i++) {
            g.fill(x + i * 4, y, x + i * 4 + 2, y + 2, HudDraw.argb(power.glow, 0.9F));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The badge

    /**
     * Draws the badge beside the crosshair and returns where it stands ({x, y, width, height}), for the done pop and
     * the tip card. Its rows: the spell's and the material's pictures, beside them the spell's name and style over the
     * size; Billy's meter and cost (the Scarlet Witch's "∞" and count); Billy's stars and rank; what the builder should
     * know (Mom's help, too far, no block in hand), when there is something.
     */
    private static int[] badge(GuiGraphics g, Minecraft mc, Power power, BuildData data, float cx, float cy, float time, boolean calm) {
        Font font = mc.font;
        BuildGrammar grammar = BuildClient.grammar();
        Spell spell = grammar.spell();
        Limits limits = BuildClient.limits();
        boolean limited = limits != null && limits.limited();
        long gameTime = mc.level.getGameTime();

        // The pictures, and beside them the spell's name over the size.
        boolean material = spell.needsMaterial();
        int pictures = 18 + (material ? 18 : 0);
        FormattedCharSequence name = HudDraw.visual(spellName(grammar));
        Component size = size(grammar);
        FormattedCharSequence sizeText = size == null ? null : HudDraw.visual(size);
        int head = pictures + Math.max(font.width(name), sizeText == null ? 0 : font.width(sizeText));

        // The meter and the cost, or "∞" and the count.
        int top = BillyRanks.meter(BillyRanks.rank(data));
        double meterNow = BillyRanks.meterNow(data, gameTime);
        long cost = limited && !spell.isBrush() ? BuildClient.cost(data) : -1;
        Component amount = amount(grammar, limited, cost, meterNow, top);
        FormattedCharSequence amountText = amount == null ? null : HudDraw.visual(amount);
        boolean waits = limited && cost > meterNow && cost <= top;
        FormattedCharSequence seconds = waits ? HudDraw.visual(Component.translatable("hud.enhanced.seconds",
                BillyMagic.secondsToPay(data, gameTime, (int) Math.min(Integer.MAX_VALUE, cost)))) : null;
        FormattedCharSequence infinity = HudDraw.visual(Component.literal("∞"));
        int meterRow = (limited ? METER_W + 2 : font.width(infinity)) + (amountText != null ? 4 + font.width(amountText) : 0)
                + (seconds != null ? 3 + 10 + font.width(seconds) : 0);

        // Billy's stars and rank.
        int stars = power == Power.BILLY ? BillyRanks.maxRank() : 0;
        FormattedCharSequence rankName = HudDraw.visual(Component.translatable(BillyRanks.nameKey(BillyRanks.rank(data))));
        int rankRow = stars > 0 ? stars * 8 + 4 + font.width(rankName) : 0;

        // What he should know: Mom's help, too far, no block in hand.
        MutableComponent notes = Component.empty();
        if (power == Power.BILLY && data.momNear()) {
            notes.append(Component.translatable("hud.enhanced.build.mom").withStyle(s -> s.withColor(Power.SCARLET_WITCH.glow)));
        }
        if (tooFar) {
            notes.append(notes.getSiblings().isEmpty() ? "" : "  ").append(Component.translatable("hud.enhanced.build.too_far")
                    .withStyle(s -> s.withColor(ORANGE)));
        }
        if (material && BuildClient.material() == null) {
            notes.append(notes.getSiblings().isEmpty() ? "" : "  ").append(Component.translatable("hud.enhanced.build.hand")
                    .withStyle(s -> s.withColor(GREY)));
        }
        FormattedCharSequence noteText = notes.getSiblings().isEmpty() ? null : HudDraw.visual(notes);
        int noteRow = noteText == null ? 0 : font.width(noteText);

        int innerW = Math.max(Math.max(head, meterRow), Math.max(rankRow, noteRow));
        int innerH = 20 + 11 + (stars > 0 ? 10 : 0) + (noteText != null ? 10 : 0);
        int w = innerW + 2 * PAD;
        int h = innerH + 2 * PAD;
        int x = Mth.floor(cx) + BADGE_GAP;
        if (x + w > g.guiWidth() - 4) {
            x = Mth.floor(cx) - BADGE_GAP - w;
        }
        x = Mth.clamp(x, 4, Math.max(4, g.guiWidth() - 4 - w));
        int y = Mth.clamp(Mth.floor(cy) - h / 2, 4, Math.max(4, g.guiHeight() - 4 - h));

        g.fill(x, y, x + w, y + h, HudDraw.argb(0x101010, 0.72F));
        g.fill(x, y, x + w, y + 1, HudDraw.argb(power.color, 0.8F));
        int left = x + PAD;
        int row = y + PAD + 1;

        HudDraw.spell(g, spell, left, row + 1);
        if (material) {
            materialSlot(g, font, grammar, left + 18, row + 1, time, calm);
        }
        HudDraw.text(g, font, name, left + pictures, row, HudDraw.argb(GREY, 1.0F), true);
        if (sizeText != null) {
            HudDraw.text(g, font, sizeText, left + pictures, row + 10, HudDraw.argb(WHITE, 1.0F), true);
        }

        row += 20;
        boolean never = limited && cost > top;
        boolean flash = never && !calm && Mth.floor(time / 4.0F) % 2 == 1;
        int at;
        if (limited) {
            meter(g, left, row + 2, power, data, meterNow, top, cost, never, flash);
            at = left + METER_W + 2;
        } else {
            at = left + HudDraw.text(g, font, infinity, left, row + 1, HudDraw.argb(power.glow, 1.0F), true);
        }
        if (amountText != null) {
            int colour = never ? (flash ? WHITE : RED) : waits ? ORANGE : WHITE;
            at += 4;
            at += HudDraw.text(g, font, amountText, at, row + 1, HudDraw.argb(colour, 1.0F), true);
        }
        if (seconds != null) {
            at += 3;
            g.pose().pushPose();
            g.pose().translate(at, row, 0.0F);
            g.pose().scale(0.5625F, 0.5625F, 1.0F);
            g.renderItem(new ItemStack(Items.CLOCK), 0, 0);
            g.pose().popPose();
            HudDraw.text(g, font, seconds, at + 10, row + 1, HudDraw.argb(ORANGE, 1.0F), true);
        }

        if (stars > 0) {
            row += 11;
            at = stars(g, font, left, row, BillyRanks.rank(data), stars, time, calm);
            HudDraw.text(g, font, rankName, at + 4, row + 1, HudDraw.argb(GREY, 1.0F), true);
        }
        if (noteText != null) {
            row += 10;
            HudDraw.text(g, font, noteText, left, row + 1, HudDraw.argb(WHITE, 1.0F), true);
        }
        return new int[]{x, y, w, h};
    }

    /** The spell's name, its style or shape (a shape's "●" solid or "○" hollow), and "mix" while the hotbar mixes. */
    private static Component spellName(BuildGrammar grammar) {
        Spell spell = grammar.spell();
        MutableComponent name = Component.translatable(spell.key());
        if (spell.styles() > 1) {
            name.append(" · ").append(BuildHints.styleName(spell, grammar.style()));
        }
        if (spell == Spell.SHAPE) {
            name.append(grammar.hollow() ? " ○" : " ●");
        }
        if (spell.needsMaterial() && grammar.mix()) {
            name.append(" · ").append(Component.translatable("hud.enhanced.build.mix"));
        }
        return name;
    }

    /**
     * The size of what the spell is about to make: the box (or the selected thing's bounds, a ghost's), Repeat's copies
     * and gap, a shape's radius and height, Pull's layers, a brush; null with nothing to measure.
     */
    private static @Nullable Component size(BuildGrammar grammar) {
        Spell spell = grammar.spell();
        switch (spell) {
            case PAINT, LAND, GROW -> {
                return Component.translatable("hud.enhanced.build.brush", HudDraw.number(grammar.brush()));
            }
            case SHAPE -> {
                MutableComponent text = Component.translatable("hud.enhanced.build.radius", HudDraw.number(grammar.radius()));
                if (grammar.shapeKind().hasHeight()) {
                    text.append(" · ").append(Component.translatable("hud.enhanced.build.height", HudDraw.number(grammar.height())));
                }
                return text;
            }
            case PULL -> {
                return Component.translatable("hud.enhanced.build.layers", HudDraw.number(Math.abs(grammar.layers())));
            }
            default -> {
            }
        }
        BoundingBox over;
        if (grammar.ghost()) {
            SpellPlanner.Plan plan = BuildClient.plan();
            over = plan != null && plan.prepared() && plan.refusal() == SpellPlanner.NONE ? plan.bounds() : null;
        } else {
            over = grammar.box() != null ? grammar.box() : grammar.maskBounds();
        }
        if (over == null) {
            return null;
        }
        MutableComponent text = Component.translatable("hud.enhanced.build.size", HudDraw.number(over.getXSpan()), HudDraw.number(over.getYSpan()),
                HudDraw.number(over.getZSpan()));
        if (spell == Spell.REPEAT) {
            text.append(" · ").append(Component.translatable("hud.enhanced.build.copies", HudDraw.number(grammar.copies())));
            if (grammar.gap() > 0) {
                text.append(" · ").append(Component.translatable("hud.enhanced.build.gap", HudDraw.number(grammar.gap())));
            }
        }
        return text;
    }

    /**
     * The badge's number: Billy's "cost / meter ✦" (his meter, "✦ 372/512", with nothing to pay), the Scarlet Witch's
     * blocks changed ("about" when some are in chunks not loaded); "counting..." while the client counts; null with
     * nothing planned.
     */
    private static @Nullable Component amount(BuildGrammar grammar, boolean limited, long cost, double meterNow, int top) {
        SpellPlanner.Plan plan = BuildClient.plan();
        boolean planned = !grammar.spell().isBrush() && plan != null && plan.prepared() && plan.refusal() == SpellPlanner.NONE && plan.writes();
        SpellPlanner.Count count = BuildClient.count();
        if (planned && count != null && !count.done()) {
            return Component.translatable("hud.enhanced.build.counting", HudDraw.number(count.walked()));
        }
        if (limited) {
            if (planned && cost >= 0) {
                return Component.translatable("hud.enhanced.build.cost", HudDraw.number(cost), HudDraw.number(Mth.floor(meterNow)));
            }
            return Component.translatable("hud.enhanced.build.meter", HudDraw.number(Mth.floor(meterNow)), HudDraw.number(top));
        }
        long changed = planned ? BuildClient.changed() : -1;
        if (changed < 0 || count == null) {
            return null;
        }
        BuildVerdictPayload preflight = BuildClient.preflight();
        boolean about = count.unloaded() > 0 || preflight != null && (preflight.flags() & BuildVerdictPayload.UNLOADED) != 0;
        return about ? Component.translatable("hud.enhanced.build.about", HudDraw.number(changed)) : Component.literal(HudDraw.number(changed));
    }

    /**
     * Billy's meter, 60 by 5 pixels: what he has now in his blue, refilling smoothly; the cost a darker chunk at its
     * end; more than he has now, all he has orange and what is missing faintly orange after it; more than his whole
     * meter, flashing red. Mom's help: a scarlet stripe along its top.
     */
    private static void meter(GuiGraphics g, int x, int y, Power power, BuildData data, double now, int top, long cost, boolean never,
                              boolean flash) {
        g.fill(x - 1, y - 1, x + METER_W + 1, y + METER_H + 1, HudDraw.argb(0x000000, 0.85F));
        g.fill(x, y, x + METER_W, y + METER_H, HudDraw.argb(0x1C2833, 1.0F));
        int has = top <= 0 ? 0 : Mth.clamp(Mth.floor(now / top * METER_W), 0, METER_W);
        if (cost <= 0) {
            HudDraw.box(g, x, y, x + has, y + METER_H, power.color, 1.0F);
        } else if (never) {
            HudDraw.box(g, x, y, x + METER_W, y + METER_H, RED, flash ? 0.25F : 0.55F);
            HudDraw.box(g, x, y, x + has, y + METER_H, RED, flash ? 0.6F : 1.0F);
        } else if (cost > now) {
            int needs = Mth.clamp(Mth.ceil((double) cost / top * METER_W), has, METER_W);
            HudDraw.box(g, x, y, x + has, y + METER_H, ORANGE, 1.0F);
            HudDraw.box(g, x + has, y, x + needs, y + METER_H, ORANGE, 0.35F);
        } else {
            int chunk = Mth.clamp(Mth.ceil((double) cost / top * METER_W), 1, Math.max(1, has));
            HudDraw.box(g, x, y, x + has - chunk, y + METER_H, power.color, 1.0F);
            HudDraw.box(g, x + has - chunk, y, x + has, y + METER_H, HudDraw.darker(power.color), 1.0F);
            g.fill(x + has - chunk, y, x + has - chunk + 1, y + METER_H, HudDraw.argb(WHITE, 0.6F));
        }
        if (data.momNear()) {
            g.fill(x, y, x + METER_W, y + 1, HudDraw.argb(Power.SCARLET_WITCH.color, 1.0F));
        }
    }

    /**
     * Billy's stars, one for each rank he can reach, filled up to his; at a rank-up the new star grows into place and a
     * "+" sparkles over it. Returns where the row goes on.
     */
    private static int stars(GuiGraphics g, Font font, int x, int y, int rank, int count, float time, boolean calm) {
        float since = time - rankUpAt;
        boolean rising = since >= 0.0F && since < RANK_UP_TICKS;
        for (int i = 1; i <= count; i++) {
            boolean filled = i <= rank;
            FormattedCharSequence star = HudDraw.visual(Component.literal(filled ? "★" : "☆"));
            int colour = HudDraw.argb(filled ? GOLD : DARK_GREY, 1.0F);
            int sx = x + (i - 1) * 8;
            if (rising && i == rank && !calm) {
                float grow = since < 10.0F ? since / 10.0F * 1.6F : 1.6F - 0.6F * Math.min(1.0F, (since - 10.0F) / 10.0F);
                g.pose().pushPose();
                g.pose().translate(sx + 3.5F, y + 5.0F, 0.0F);
                g.pose().scale(grow, grow, 1.0F);
                HudDraw.text(g, font, star, -3, -4, colour, true);
                g.pose().popPose();
            } else {
                HudDraw.text(g, font, star, sx, y + 1, colour, true);
            }
        }
        if (rising) {
            float fade = 1.0F - since / RANK_UP_TICKS;
            float lift = calm ? 0.0F : since / RANK_UP_TICKS * 6.0F;
            g.pose().pushPose();
            g.pose().translate(x + rank * 8 - 2, y - 4 - lift, 0.0F);
            HudDraw.text(g, font, HudDraw.visual(Component.literal("+")), 0, 0, HudDraw.argb(WHITE, fade), true);
            g.pose().popPose();
        }
        return x + count * 8;
    }

    /**
     * The material's slot: the block that will be made (a picked one with a small eye; a mix as two of its blocks), or
     * an empty slot with a pulsing "?" when the spell has none.
     */
    private static void materialSlot(GuiGraphics g, Font font, BuildGrammar grammar, int x, int y, float time, boolean calm) {
        Object key = BuildClient.materialKey();
        if (key instanceof BlockState state) {
            ItemStack item = HudDraw.item(state);
            if (!item.isEmpty()) {
                g.renderItem(item, x, y);
            } else {
                g.fill(x + 2, y + 2, x + 14, y + 14, HudDraw.argb(state.getBlock().defaultMapColor().col, 1.0F));
            }
            if (grammar.material() == SpellRecipe.PICKED) {
                // Over the item (drawn 150 up).
                g.pose().pushPose();
                g.pose().translate(0.0F, 0.0F, 200.0F);
                HudDraw.picture(g, font, Picture.LOOK, x + 6, y + 7, Power.BILLY.glow, true, false, 1.0F);
                g.pose().popPose();
            }
            return;
        }
        if (key instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof SpellMath.Mix mix) {
            List<SpellMath.Mix.Part> parts = mix.blocks();
            for (int i = Math.min(2, parts.size()) - 1; i >= 0; i--) {
                g.pose().pushPose();
                g.pose().translate(x + (i == 0 ? 0.0F : 5.0F), y + (i == 0 ? 0.0F : 5.0F), i == 0 ? 50.0F : 0.0F);
                g.pose().scale(0.6875F, 0.6875F, 1.0F);
                g.renderItem(HudDraw.item(parts.get(i).state()), 0, 0);
                g.pose().popPose();
            }
            return;
        }
        // No block: an empty slot asking for one.
        g.fill(x, y, x + 16, y + 16, HudDraw.argb(0x8A8A8A, 0.9F));
        g.fill(x + 1, y + 1, x + 15, y + 15, HudDraw.argb(0x1A1A1A, 1.0F));
        float pulse = calm ? 1.0F : 0.55F + 0.45F * (0.5F + 0.5F * Mth.sin(time * 0.3F));
        FormattedCharSequence question = HudDraw.visual(Component.literal("?"));
        HudDraw.text(g, font, question, x + 8 - font.width(question) / 2, y + 4, HudDraw.argb(ORANGE, pulse), true);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The done pop

    /** "✓ 140" rising out of the badge (beside the crosshair without one) {@value #POP_TICKS} ticks after a spell. */
    private static void pop(GuiGraphics g, Font font, Power power, int @Nullable [] badge, float cx, float cy, float time, boolean calm) {
        float since = time - popAt;
        if (since < 0.0F || since >= POP_TICKS) {
            return;
        }
        float fade = Math.min(1.0F, (POP_TICKS - since) / 15.0F);
        float rise = calm ? 0.0F : since / POP_TICKS * 14.0F;
        FormattedCharSequence text = HudDraw.visual(Component.literal("✓ " + HudDraw.number(popCount)));
        int x = badge != null ? badge[0] + PAD + 18 : Mth.floor(cx) + BADGE_GAP;
        int y = badge != null ? badge[1] - 10 : Mth.floor(cy) - 14;
        g.pose().pushPose();
        g.pose().translate(0.0F, -rise, 0.0F);
        HudDraw.text(g, font, text, x, y, HudDraw.argb(power.glow, fade), true);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The hint strip

    /**
     * The hint strip (D-B 6.4): three to five rows at the bottom right, each the picture of an input and its words; in
     * a right-to-left language the pictures stand on the right and the words end against them.
     */
    private static void strip(GuiGraphics g, Minecraft mc, Power power, BuildData data, int width, int height) {
        BuildGrammar grammar = BuildClient.grammar();
        List<BuildHints.Row> rows = BuildHints.lines(grammar.spell(), grammar.style(), BuildClient.stage(), power, data.working(), data.undo() > 0);
        if (rows.isEmpty()) {
            return;
        }
        Font font = mc.font;
        int pictures = 0;
        int words = 0;
        List<FormattedCharSequence> texts = new ArrayList<>(rows.size());
        for (BuildHints.Row row : rows) {
            FormattedCharSequence text = HudDraw.visual(row.text());
            texts.add(text);
            pictures = Math.max(pictures, HudDraw.width(font, Picture.of(row.input())));
            words = Math.max(words, font.width(text));
        }
        int gap = 5;
        int w = PAD + pictures + gap + words + PAD;
        int h = rows.size() * ROW_H + 2 * PAD - 2;
        int right = width - 4;
        int left = right - w;
        int bottom = height - 4;
        int subtitles = SUBTITLES.rows(mc);
        if (subtitles > 0) {
            // Over the subtitles' box: its rows are 10 apart, the lowest centred 35 above the bottom.
            bottom = Math.min(bottom, height - 35 - (subtitles - 1) * 10 - 5 - 3);
        }
        if (left < width / 2 + 91 + 4) {
            // It would run into the hotbar: over the food bar instead.
            bottom = Math.min(bottom, height - Math.max(22, mc.gui.rightHeight) - 3);
        }
        int top = bottom - h;
        stripLeft = left;
        stripTop = top;
        boolean rtl = HudDraw.rtl();
        g.fill(left, top, right, bottom, HudDraw.argb(0x000000, 0.45F));
        for (int i = 0; i < rows.size(); i++) {
            Picture picture = Picture.of(rows.get(i).input());
            int y = top + PAD + i * ROW_H;
            FormattedCharSequence text = texts.get(i);
            if (rtl) {
                HudDraw.picture(g, font, picture, right - PAD - pictures, y, power.color, true, false, 1.0F);
                HudDraw.text(g, font, text, right - PAD - pictures - gap - font.width(text), y + 2, HudDraw.argb(WHITE, 1.0F), true);
            } else {
                HudDraw.picture(g, font, picture, left + PAD + pictures - HudDraw.width(font, picture), y, power.color, true, false, 1.0F);
                HudDraw.text(g, font, text, left + PAD + pictures + gap, y + 2, HudDraw.argb(WHITE, 1.0F), true);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The tip card

    /**
     * The tip playing now, as a card over the action bar: the picture twice as big with its button blinking and an
     * arrow pointing at it, then the words; fading in and out. Where a short screen puts it beside the badge, it moves
     * left of it.
     */
    private static void tip(GuiGraphics g, Minecraft mc, Power power, int @Nullable [] badge, int width, int height, float partial,
                            boolean calm) {
        BuildTips.Tip tip = BuildTips.current();
        if (tip == null) {
            return;
        }
        float age = BuildTips.age(partial);
        float fade = Mth.clamp(Math.min(age / 4.0F, (BuildTips.TIP_TICKS - age) / 10.0F), 0.0F, 1.0F);
        if (fade <= 0.03F) {
            return;
        }
        Font font = mc.font;
        FormattedCharSequence text = HudDraw.visual(tip.text());
        int scale = 2;
        int pw = HudDraw.width(font, tip.picture()) * scale;
        int ph = HudDraw.MOUSE_H * scale;
        int pad = 5;
        int arrow = 5;
        int w = pad + pw + 8 + font.width(text) + pad + 1;
        int h = pad + arrow + ph + pad;
        // Over the action bar, which stands over the hearts and the food (or 68 above the bottom).
        int bottom = height - Math.max(68, Math.max(mc.gui.leftHeight, mc.gui.rightHeight) + 9) - 7;
        int left = width / 2 - w / 2;
        int top = bottom - h;
        if (badge != null && top < badge[1] + badge[3] && bottom > badge[1] && left < badge[0] + badge[2] && left + w > badge[0]) {
            left = Math.max(4, Math.min(left, badge[0] - 4 - w));
        }
        g.fill(left, top, left + w, bottom, HudDraw.faded(HudDraw.argb(0x101010, 0.82F), fade));
        g.renderOutline(left, top, w, h, HudDraw.faded(HudDraw.argb(power.color, 0.9F), fade));
        boolean rtl = HudDraw.rtl();
        int px = rtl ? left + w - pad - pw : left + pad;
        int py = top + pad + arrow;
        boolean on = calm || Mth.floor(age / 6.0F) % 2 == 0;
        g.pose().pushPose();
        g.pose().translate(px, py, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        HudDraw.picture(g, font, tip.picture(), 0, 0, power.color, on, on, fade);
        g.pose().popPose();
        // The arrow, pointing down at what to press, bobbing.
        int bob = calm ? 0 : Math.round(1.0F + Mth.sin(age * 0.5F));
        int ax = px + pointAt(font, tip.picture()) * scale + scale / 2;
        int ay = top + pad - 3 + bob;
        int white = HudDraw.faded(HudDraw.argb(WHITE, 1.0F), fade);
        g.fill(ax - 2, ay, ax + 3, ay + 1, white);
        g.fill(ax - 1, ay + 1, ax + 2, ay + 2, white);
        g.fill(ax, ay + 2, ax + 1, ay + 3, white);
        int tx = rtl ? px - 8 - font.width(text) : px + pw + 8;
        HudDraw.text(g, font, text, tx, py + ph / 2 - 4, HudDraw.faded(HudDraw.argb(WHITE, 1.0F), fade), true);
    }

    /** Where in a picture (unscaled) the tip's arrow points: the button lit, the wheel, the middle of a key. */
    private static int pointAt(Font font, Picture picture) {
        return switch (picture) {
            case LEFT, LEFT_TWICE -> 2;
            case RIGHT, RIGHT_HELD -> 6;
            case WHEEL, MIDDLE -> 4;
            case MORE_WHEEL -> HudDraw.keyWidth(font, BuildKeys.MORE) / 2;
            case LOOK -> 5;
            case MAGIC_KEY, UNDO_KEY, FLIP_KEY -> HudDraw.keyWidth(font, picture.key()) / 2;
        };
    }

    // ------------------------------------------------------------------------------------------------------------
    // The subtitles' box

    /**
     * How many rows the subtitles' box shows (vanilla keeps them to itself): every subtitle heard within the time it is
     * shown, from where it can still be heard, as {@code SubtitleOverlay} counts them.
     */
    private static final class Subtitles implements SoundEventListener {
        private record Heard(Vec3 at, float range, long millis) {
        }

        private final Map<Component, Heard> heard = new HashMap<>();

        @Override
        public void onPlaySound(SoundInstance sound, WeighedSoundEvents accessor, float range) {
            Component text = accessor.getSubtitle();
            if (text != null) {
                heard.put(text, new Heard(new Vec3(sound.getX(), sound.getY(), sound.getZ()), range, Util.getMillis()));
            }
        }

        int rows(Minecraft mc) {
            if (!mc.options.showSubtitles().get()) {
                heard.clear();
                return 0;
            }
            double shown = 3000.0 * mc.options.notificationDisplayTime().get();
            long now = Util.getMillis();
            heard.values().removeIf(h -> now - h.millis() > shown);
            Vec3 ear = mc.getSoundManager().getListenerTransform().position();
            int rows = 0;
            for (Heard h : heard.values()) {
                if (Float.isInfinite(h.range()) || ear.closerThan(h.at(), h.range())) {
                    rows++;
                }
            }
            return rows;
        }
    }
}

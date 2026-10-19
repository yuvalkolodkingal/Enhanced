package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.client.BossBarOverride;
import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.hulk.HulkForms;
import com.kolodkingal.enhanced.hulk.HulkHooks;
import com.kolodkingal.enhanced.hulk.HulkRage;
import com.kolodkingal.enhanced.hulk.HulkState;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The Hulk's HUD (3.6), under the powers' panel: the Hulk's <b>rage bar</b> (green, marks at 50 and 100, gamma-bright
 * and "Enraged" from 50) and, after his Snap, the burned arm's minutes; Bruce Banner's <b>heart rate</b> ("♥ 92 bpm",
 * rising to 200 with his anger, a red pulse near the top, the pulse watch of The Incredible Hulk) and "N: Hulk out".
 * Banner's heart is heard too: a loop that beats faster with his anger ({@code banner_heartbeat}, two beats at 75 a
 * minute at pitch 1). And the green flash of the gamma accident over the whole screen.
 */
public final class HulkHud implements LayeredDraw.Layer {
    /** The panel's height: the powers' panel above it is 12 + 5 lines of 10. */
    private static final int PANEL = 12 + 5 * 10 + 6;
    private static final int BAR_W = 100;
    @Nullable
    private static Heart heart;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        float flash = HulkEffects.flash();
        if (flash > 0.01F) {
            int a = (int) (flash * 170);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (a << 24) | 0x7CFF4A);
        }
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || player.isDeadOrDying()) {
            return;
        }
        PowerData data = Powers.get(player);
        if (data.power() != Power.HULK) {
            return;
        }
        Font font = mc.font;
        long now = mc.level.getGameTime();
        float pt = delta.getGameTimeDeltaPartialTick(false);
        HulkState state = HulkRage.get(player);
        int x = 4;
        int y = 4 + BossBarOverride.clearance(x + 160) + PANEL;
        if (HulkForms.isHuman(data)) {
            int bpm = HulkRage.bpm(state.anger());
            float beat = (now + pt) * bpm / 1200.0F;
            boolean pulse = (beat - (float) Math.floor(beat)) < 0.18F;
            int color = state.anger() >= 80 ? (pulse ? 0xFFFF3030 : 0xFFB02020) : (pulse ? 0xFFFF8080 : 0xFFE0E0E0);
            Component line = Component.translatable("hud.enhanced.hulk.bpm", bpm);
            g.fill(x - 2, y - 2, x + 150, y + 22, 0x70000000);
            g.drawString(font, line, x, y, color, true);
            Component hint = HulkRage.inhibited(player)
                    ? Component.translatable("hud.enhanced.hulk.inhibited").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("hud.enhanced.hulk.hulk_out", KeyBindings.ability(5)
                            .getTranslatedKeyMessage())
                            .withStyle(ChatFormatting.GREEN);
            g.drawString(font, hint, x, y + 11, 0xFFFFFFFF, true);
            return;
        }
        int rage = state.rage();
        boolean enraged = data.is(Live.HULK_ENRAGED);
        g.fill(x - 2, y - 2, x + BAR_W + 60, y + (HulkHooks.armBurned(player) ? 22 : 12), 0x70000000);
        Component label = Component.translatable("hud.enhanced.hulk.rage", rage);
        g.drawString(font, label, x, y, enraged ? 0xFFB6F27A : 0xFFFFFFFF, true);
        int bx = x + 54;
        int by = y + 1;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + 7, 0xFF101810);
        int filled = Math.round(BAR_W * rage / (float) HulkRage.MAX);
        int fill = enraged ? lerpColor(0xFF5BA33A, 0xFFB6F27A, 0.5F + 0.5F * Mth.sin((now + pt) * 0.4F)) : 0xFF4E8F33;
        g.fill(bx, by, bx + filled, by + 6, fill);
        g.fill(bx + BAR_W / 2, by - 1, bx + BAR_W / 2 + 1, by + 7, 0xFFFFFFFF);
        g.fill(bx + BAR_W - 1, by - 1, bx + BAR_W, by + 7, 0xFFFFFFFF);
        if (HulkHooks.armBurned(player)) {
            long left = (state.armBurnedUntil() - now) / 20;
            g.drawString(font, Component.translatable("hud.enhanced.hulk.arm", String.format("%d:%02d", left / 60,
                    left % 60)),
                    x, y + 11, 0xFFFFA040, true);
        }
    }

    private static int lerpColor(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int gg = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return 0xFF000000 | r << 16 | gg << 8 | bl;
    }

    /** Every client tick: Banner's heart is heard while he is angry. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        boolean angry = player != null && player.isAlive() && Powers.get(player).power() == Power.HULK
                && HulkForms.isHuman(player)
                && HulkRage.get(player).anger() >= 15;
        if (angry && (heart == null || heart.isStopped())) {
            heart = new Heart();
            mc.getSoundManager().play(heart);
        }
    }

    public static void clear() {
        if (heart != null) {
            heart.end();
            heart = null;
        }
    }

    /** Banner's heartbeat: a loop at the player, faster (higher) and louder with his anger. */
    private static final class Heart extends AbstractTickableSoundInstance {
        Heart() {
            super(ModSounds.BANNER_HEARTBEAT.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.volume = 0.05F;
            this.pitch = 1.0F;
        }

        void end() {
            stop();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || !player.isAlive() || Powers.get(player).power() != Power.HULK
                    || !HulkForms.isHuman(player)) {
                stop();
                return;
            }
            int anger = HulkRage.get(player).anger();
            float bpm = HulkRage.bpm(anger);
            float targetPitch = Mth.clamp(bpm / 75.0F, 0.9F, 2.0F);
            float targetVolume = anger < 15 ? 0.0F : Mth.clamp(0.25F + anger / 160.0F, 0.0F, 0.9F);
            pitch += (targetPitch - pitch) * 0.2F;
            volume += (targetVolume - volume) * 0.2F;
            if (anger < 15 && volume < 0.02F) {
                stop();
            }
        }
    }
}

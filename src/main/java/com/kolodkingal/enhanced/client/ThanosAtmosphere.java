package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.render.ThanosClientView;
import com.kolodkingal.enhanced.client.render.ThanosFx;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The air round the Mad Titan (DESIGN section 15.14.3 and 15.14.2): within 64 blocks of a living Thanos the fog turns
 * dark violet over three seconds and the edges of the screen darken, a thunder that is not weather rumbles far off
 * every so often, and a stone arriving flashes the screen white. The call has its own sky: while the Tesseract and the
 * Scepter are held up, and while the portal is open, the fog turns violet too. The effects on the player himself are
 * here as well: the grey of the Reality Stone's petrifying, the green of a Time Stone's lock, and the Soul World, an
 * orange sky over a still water with a gateway far ahead. The client options {@code bossAtmosphere} and reduced motion
 * are honoured.
 */
@Subscriber(side = Side.CLIENT)
public final class ThanosAtmosphere {
    /** The colour the fog turns. */
    private static final int FOG = 0x2A1A40;
    /** Within this many blocks of him the air is changed. */
    private static final double REACH = 64.0;
    private static final int GREY = 0x808890;
    private static final int GREEN = StoneKind.TIME.color();

    /** How far the fog has turned (0 to 1), eased toward where it should be. */
    private static float fog;
    /** How dark the edges of the screen are (0 to 1). */
    private static float edges;
    /** The Soul World: the tick it began and the tick it ends (a payload tells when; the effect also does). */
    private static long soulFrom = Long.MIN_VALUE;
    private static long soulUntil = Long.MIN_VALUE;
    /** The flash of a stone arriving: when it began and how long it lasts. */
    private static long flashFrom = Long.MIN_VALUE;
    private static int flashTicks;
    /** The tick until which a portal stands open in the sky (the call). */
    private static long portalUntil = Long.MIN_VALUE;
    private static int nextThunder = 400;

    /** The Soul World takes the player: the orange sky for {@code ticks}. */
    public static void soulWorld(long now, int ticks) {
        soulFrom = now;
        soulUntil = now + ticks;
    }

    /** A white flash of the screen. */
    public static void flash(long now, int ticks) {
        flashFrom = now;
        flashTicks = Math.max(1, ticks);
    }

    /** A portal is open in the sky for {@code ticks}: the air turns before he arrives. */
    public static void portalOpen(long now, int ticks) {
        portalUntil = Math.max(portalUntil, now + ticks);
    }

    public static void clear() {
        fog = 0.0F;
        edges = 0.0F;
        soulFrom = Long.MIN_VALUE;
        soulUntil = Long.MIN_VALUE;
        flashFrom = Long.MIN_VALUE;
        portalUntil = Long.MIN_VALUE;
    }

    /** Once per client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            clear();
            return;
        }
        long now = mc.level.getGameTime();
        ThanosEntity boss = ThanosClientView.nearestBoss(player.position());
        boolean near = boss != null && boss.isAlive() && boss.state() != ThanosState.DYING && boss.position().distanceToSqr(player.position()) < REACH * REACH;
        boolean allowed = EnhancedClientConfig.bossAtmosphere();
        float fogTarget = 0.0F;
        float edgeTarget = 0.0F;
        if (allowed) {
            if (near) {
                fogTarget = 1.0F;
                edgeTarget = 1.0F;
            }
            if (calling(player) || now < portalUntil) {
                fogTarget = Math.max(fogTarget, 0.7F);
            }
        }
        // Three seconds to turn.
        fog += Mth.clamp(fogTarget - fog, -1.0F / 60.0F, 1.0F / 60.0F);
        edges += Mth.clamp(edgeTarget - edges, -1.0F / 40.0F, 1.0F / 40.0F);
        // A distant thunder that is not weather, every twenty to forty seconds.
        if (near && allowed && --nextThunder <= 0) {
            nextThunder = 400 + mc.level.random.nextInt(400);
            mc.level.playLocalSound(player.getX(), player.getY() + 40.0, player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 0.3F,
                    0.55F + mc.level.random.nextFloat() * 0.2F, false);
        }
    }

    /** Whether the player is holding up the Tesseract and the Scepter together: the call. */
    private static boolean calling(LocalPlayer player) {
        if (!player.isUsingItem()) {
            return false;
        }
        ItemStack used = player.getUseItem();
        ItemStack other = player.getItemInHand(player.getUsedItemHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return used.is(ModItems.TESSERACT.get()) && other.is(ModItems.LOKIS_SCEPTER.get())
                || used.is(ModItems.LOKIS_SCEPTER.get()) && other.is(ModItems.TESSERACT.get());
    }

    @Subscribe
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (fog <= 0.001F) {
            return;
        }
        float k = fog * 0.75F;
        event.setRed(Mth.lerp(k, event.getRed(), ((FOG >> 16) & 0xFF) / 255.0F));
        event.setGreen(Mth.lerp(k, event.getGreen(), ((FOG >> 8) & 0xFF) / 255.0F));
        event.setBlue(Mth.lerp(k, event.getBlue(), (FOG & 0xFF) / 255.0F));
    }

    /** A landing and a blow near by shake the camera a little (not with reduced motion). */
    @Subscribe
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float shake = ThanosFx.shake();
        if (shake <= 0.01F || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        float t = (float) event.getPartialTick() + Minecraft.getInstance().player.tickCount;
        event.setPitch(event.getPitch() + shake * Mth.sin(t * 2.3F));
        event.setRoll(event.getRoll() + shake * 0.6F * Mth.sin(t * 1.7F + 1.0F));
    }

    // ---------------------------------------------------------------------------------------------------------
    // The screen

    /** The overlay: registered above the camera overlays by {@code ClientSetup}. */
    public static final class Overlay implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null || mc.options.hideGui) {
                return;
            }
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();
            long now = mc.level.getGameTime();
            float time = now + deltaTracker.getGameTimeDeltaPartialTick(false);
            if (edges > 0.01F) {
                vignette(graphics, width, height, FOG, 0.5F * edges);
            }
            if (player.hasEffect(ModEffects.PETRIFIED)) {
                graphics.fill(0, 0, width, height, argb(GREY, 0.12F));
                vignette(graphics, width, height, GREY, 0.6F);
            }
            if (player.hasEffect(ModEffects.TIME_LOCKED)) {
                graphics.fill(0, 0, width, height, argb(GREEN, 0.07F));
                vignette(graphics, width, height, GREEN, 0.35F);
            }
            float soul = soulStrength(player, time);
            if (soul > 0.01F) {
                soulWorld(graphics, width, height, time, soul);
            }
            if (flashFrom != Long.MIN_VALUE && !EnhancedClientConfig.reducedMotion()) {
                float k = 1.0F - (time - flashFrom) / flashTicks;
                if (k > 0.0F && k <= 1.0F) {
                    graphics.fill(0, 0, width, height, argb(0xFFFFFF, 0.2F * k));
                }
            }
        }

        /** How much of the Soul World is over the screen: it fades in over six ticks and out over ten. */
        private static float soulStrength(LocalPlayer player, float time) {
            boolean effect = player.hasEffect(ModEffects.SOULWORLD);
            boolean payload = time >= soulFrom && time <= soulUntil;
            if (!effect && !payload) {
                return 0.0F;
            }
            float in = soulFrom == Long.MIN_VALUE ? 1.0F : Mth.clamp((time - soulFrom) / 6.0F, 0.0F, 1.0F);
            float out = time <= soulUntil ? Mth.clamp((soulUntil - time) / 10.0F, 0.0F, 1.0F) : 1.0F;
            return Math.min(in, out);
        }

        /** The edges of the screen darkened in steps, strongest at the border. */
        private static void vignette(GuiGraphics graphics, int width, int height, int rgb, float strength) {
            int steps = 14;
            int depthX = width / 6;
            int depthY = height / 5;
            for (int i = 0; i < steps; i++) {
                float k = 1.0F - (float) i / steps;
                int color = argb(rgb, strength * k * k * 0.5F);
                int dx = depthX * i / steps;
                int dy = depthY * i / steps;
                int sx = Math.max(1, depthX / steps);
                int sy = Math.max(1, depthY / steps);
                graphics.fill(dx, 0, dx + sx, height, color);
                graphics.fill(width - dx - sx, 0, width - dx, height, color);
                graphics.fill(0, dy, width, dy + sy, color);
                graphics.fill(0, height - dy - sy, width, height - dy, color);
            }
        }

        /**
         * The Soul World of the film: an orange sky over a pale horizon, a still water under it that ripples, and a
         * dark gateway standing far ahead. It covers most of the screen while it lasts.
         */
        private static void soulWorld(GuiGraphics graphics, int width, int height, float time, float strength) {
            int horizon = (int) (height * 0.58F);
            graphics.fillGradient(0, 0, width, horizon, argb(0xFF8A1E, 0.85F * strength), argb(0xFFD6A0, 0.85F * strength));
            graphics.fillGradient(0, horizon, width, height, argb(0xE9A55B, 0.85F * strength), argb(0x8A3F12, 0.9F * strength));
            // The water: rows of light that slide and thin out toward the horizon.
            for (int row = 0; row < 14; row++) {
                float depth = (row + 1) / 14.0F;
                int y = horizon + (int) (depth * depth * (height - horizon));
                int span = (int) (width * (0.2F + 0.5F * depth));
                int x = (int) ((width - span) * 0.5F + Mth.sin(time * 0.05F + row * 1.3F) * 20.0F * depth);
                graphics.fill(x, y, x + span, y + Math.max(1, (int) (2.0F * depth)), argb(0xFFE9C2, 0.35F * strength * (1.0F - 0.4F * depth)));
            }
            // The gateway: two pillars and a lintel against the sky, far off.
            int cx = width / 2;
            int base = horizon;
            int pillar = Math.max(2, width / 160);
            int tall = height / 5;
            int gap = width / 22;
            int dark = argb(0x4A1E08, 0.9F * strength);
            graphics.fill(cx - gap - pillar, base - tall, cx - gap, base, dark);
            graphics.fill(cx + gap, base - tall, cx + gap + pillar, base, dark);
            graphics.fill(cx - gap - pillar * 2, base - tall - pillar, cx + gap + pillar * 2, base - tall, dark);
            graphics.fill(cx - gap - pillar, base - tall + pillar * 2, cx + gap + pillar, base - tall + pillar * 3, dark);
            vignette(graphics, width, height, 0x6A2A08, 0.7F * strength);
        }

        private static int argb(int rgb, float alpha) {
            return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
        }
    }

    private static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }

    private ThanosAtmosphere() {
    }
}

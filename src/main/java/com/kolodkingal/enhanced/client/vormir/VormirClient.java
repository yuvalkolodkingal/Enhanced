package com.kolodkingal.enhanced.client.vormir;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.stones.StoneFx;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.vormir.StonekeeperEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The Vormir Cliff on the client (3.6, lane L5): the orange glow over the screen of one given the Soul Stone ({@link
 * StoneFxPayload#SOUL_GIVEN}: full at once, as he wakes in the pool, then fading over its ticks), and the summit's wind
 * ({@code vormir_wind}), a loop that rises as he nears a Stonekeeper (within {@link #WIND_RANGE} blocks) and fades away
 * when he leaves.
 */
public final class VormirClient {
    /** How near (blocks) a Stonekeeper the summit's wind is heard. */
    public static final double WIND_RANGE = 48.0;

    private static long fadeStart = Long.MIN_VALUE;
    private static int fadeTicks;
    private static Wind wind;

    /** From the client setup: the drawer of the gift. */
    public static void setup() {
        StoneFx.register(StoneFxPayload.SOUL_GIVEN, VormirClient::soulGiven);
    }

    private static void soulGiven(StoneFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && payload.entityId() == mc.player.getId()) {
            fadeStart = mc.level.getGameTime();
            fadeTicks = Math.max(1, payload.arg());
        }
    }

    /** How strong the orange glow is now (0 to 1): full at once, fading to nothing. */
    static float fade(float time) {
        float t = (time - fadeStart) / fadeTicks;
        if (t < 0.0F || t >= 1.0F) {
            return 0.0F;
        }
        return t < 0.15F ? 1.0F : 1.0F - (t - 0.15F) / 0.85F;
    }

    private static void drawFade(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || fadeStart == Long.MIN_VALUE) {
            return;
        }
        float k = fade(mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(true));
        if (k <= 0.0F) {
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int middle = Mth.clamp((int) (k * 200.0F), 0, 255);
        int edge = Mth.clamp((int) (k * 240.0F), 0, 255);
        graphics.fill(0, 0, w, h, middle << 24 | 0xFF8A1E);
        graphics.fillGradient(0, 0, w, h / 3, edge << 24 | 0x7A2E08, 0x007A2E08);
        graphics.fillGradient(0, h - h / 3, w, h, 0x007A2E08, edge << 24 | 0x7A2E08);
    }

    // ---- the wind ----

    private static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (wind != null && wind.isStopped()) {
            wind = null;
        }
        if (wind == null && mc.level.getGameTime() % 10 == 0 && nearestKeeper(mc) < WIND_RANGE) {
            wind = new Wind();
            mc.getSoundManager().play(wind);
        }
    }

    /** How far (blocks) the nearest Stonekeeper is, or infinity. */
    static double nearestKeeper(Minecraft mc) {
        double best = Double.POSITIVE_INFINITY;
        if (mc.level == null || mc.player == null) {
            return best;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof StonekeeperEntity keeper && keeper.isAlive()) {
                best = Math.min(best, keeper.distanceTo(mc.player));
            }
        }
        return best;
    }

    /** The summit's wind: all round the listener, louder nearer the keeper, fading in and out. */
    private static final class Wind extends AbstractTickableSoundInstance {
        private int age;

        Wind() {
            super(ModSounds.VORMIR_WIND.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.01F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            double d = nearestKeeper(mc);
            float target = d < WIND_RANGE ? (float) (0.7 * (1.0 - d / WIND_RANGE)) + 0.1F : 0.0F;
            age++;
            volume += Mth.clamp(target - volume, -0.02F, 0.02F);
            if (target <= 0.0F && volume <= 0.01F && age > 20) {
                stop();
            }
        }
    }

    // ---- the events ----

    /** The client's ticks and its leaving a world (the game bus). */
    @EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            tick(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            if (wind != null) {
                Minecraft.getInstance().getSoundManager().stop(wind);
                wind = null;
            }
            fadeStart = Long.MIN_VALUE;
        }

        private Events() {
        }
    }

    /** The glow's layer over the screen (the mod bus). */
    @EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void onRegisterGui(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("vormir_gift"), VormirClient::drawFade);
        }

        private Setup() {
        }
    }

    private VormirClient() {
    }
}

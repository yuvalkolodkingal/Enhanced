package com.kolodkingal.enhanced.client.school;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.WeaponXPodBlockEntity;
import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * The client's side of the X-Men lower lab (3.7, lane L4): the lift's fade (the doors close and open on the rider's
 * screen, {@link LabFxPayload#LIFT_DOWN} / {@link LabFxPayload#LIFT_UP}), a puff of dust from a secret shelf, sparks at
 * the rack, the lab's soft machinery loop while the server says one is inside a lab ({@link LabFxPayload#AMBIENCE}, at
 * {@link #AMBIENCE_VOLUME}: the file is mastered loud), and each running Weapon X Pod's hum ({@link #HUM_VOLUME}) and
 * green bubbles.
 */
public final class LabClient {
    /** The lab ambience's volume ({@code ambient.school_lab} is mastered at -17.5 LUFS, louder than 3.6's). */
    static final float AMBIENCE_VOLUME = 0.5F;
    /** The pod's hum's volume ({@code weapon_x_pod_hum}, -15.9 LUFS). */
    static final float HUM_VOLUME = 0.8F;
    /** Ticks without news after which the ambience fades out (the server tells every 40). */
    static final int SILENCE = 130;
    /** The fade's length (ticks): black for {@link #FADE_HOLD}, then clearing. */
    static final int FADE_TICKS = 22;
    static final int FADE_HOLD = 5;

    private static long fadeAt = Long.MIN_VALUE;
    private static boolean insideLab;
    private static long heardAt;
    @Nullable
    private static Loop ambience;
    private static final Map<BlockPos, Loop> HUMS = new HashMap<>();

    /** From the client setup: the drawers of the lab's moments, and the pods' client tick. */
    public static void setup() {
        LabFx.register(LabFxPayload.LIFT_DOWN, LabClient::fade);
        LabFx.register(LabFxPayload.LIFT_UP, LabClient::fade);
        LabFx.register(LabFxPayload.SHELF_OPEN, LabClient::dust);
        LabFx.register(LabFxPayload.SHELF_CLOSE, LabClient::dust);
        LabFx.register(LabFxPayload.RACK_TAKE, LabClient::sparks);
        LabFx.register(LabFxPayload.AMBIENCE, LabClient::ambience);
        WeaponXPodBlockEntity.clientTick = LabClient::pod;
    }

    // ---- the lift's fade ----

    private static void fade(LabFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            fadeAt = mc.level.getGameTime();
        }
    }

    /** How dark the fade is now (0 clear, 1 black). */
    static float fadeAlpha(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || fadeAt == Long.MIN_VALUE) {
            return 0.0F;
        }
        float t = mc.level.getGameTime() - fadeAt + partialTick;
        if (t < 0 || t > FADE_TICKS) {
            return 0.0F;
        }
        if (t < FADE_HOLD) {
            return 1.0F;
        }
        return 1.0F - (t - FADE_HOLD) / (FADE_TICKS - FADE_HOLD);
    }

    /** The lift's fade on the screen: dark steel doors parting from the middle. */
    public static final class FadeLayer implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
            float a = fadeAlpha(deltaTracker.getGameTimeDeltaPartialTick(false));
            if (a <= 0.0F) {
                return;
            }
            int w = graphics.guiWidth();
            int h = graphics.guiHeight();
            // the two doors: each covers its half while black, sliding out as the fade clears
            int open = (int) ((1.0F - a) * (w / 2.0F));
            int door = 0xFF000000 | 0x22262B;
            int edge = 0xFF000000 | 0x6E7780;
            graphics.fill(0, 0, w / 2 - open, h, door);
            graphics.fill(w / 2 + open, 0, w, h, door);
            if (open < w / 2) {
                graphics.fill(w / 2 - open - 2, 0, w / 2 - open, h, edge);
                graphics.fill(w / 2 + open, 0, w / 2 + open + 2, h, edge);
            }
            int shade = (int) (a * 0x90) << 24;
            graphics.fill(0, 0, w, h, shade);
        }
    }

    // ---- the shelves' dust, the rack's sparks ----

    private static void dust(LabFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        BlockPos p = payload.pos();
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.WHITE_ASH, p.getX() + random.nextDouble(), p.getY() + random.nextDouble()
                    * 2.0, p.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
    }

    private static void sparks(LabFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        BlockPos p = payload.pos();
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.2 + random.nextDouble() * 0.6,
                    p.getY() + 0.5 + random.nextDouble() * 0.4, p.getZ() + 0.2 + random.nextDouble() * 0.6,
                    (random.nextDouble() - 0.5) * 0.2, 0.1, (random.nextDouble() - 0.5) * 0.2);
        }
    }

    // ---- the lab's ambience ----

    private static void ambience(LabFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        insideLab = payload.pos().getY() != 0;
        heardAt = mc.level.getGameTime();
    }

    // ---- the pods ----

    private static final DustParticleOptions BUBBLE = new DustParticleOptions(new Vector3f(0.45F, 1.0F, 0.55F), 0.6F);

    /** A pod's client tick: its hum while it runs; green bubbles rising (faster while it runs). */
    private static void pod(WeaponXPodBlockEntity pod) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        BlockPos pos = pod.getBlockPos();
        RandomSource random = level.random;
        if (random.nextInt(pod.running() ? 1 : 6) == 0) {
            level.addParticle(BUBBLE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.1,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.06, 0.0);
        }
        Loop hum = HUMS.get(pos);
        if (pod.running() && (hum == null || hum.isStopped())) {
            hum = new Loop(ModSounds.WEAPON_X_POD_HUM.get(), level, HUM_VOLUME, false);
            hum.at(pos);
            HUMS.put(pos.immutable(), hum);
            mc.getSoundManager().play(hum);
        }
        if (hum != null) {
            hum.fading = !pod.running() || pod.isRemoved();
        }
    }

    private static void tick(Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        if (insideLab && mc.level.getGameTime() - heardAt > SILENCE) {
            insideLab = false;
        }
        if (ambience != null && (ambience.isStopped() || ambience.level != mc.level)) {
            ambience = null;
        }
        if (ambience != null) {
            ambience.fading = !insideLab;
        } else if (insideLab) {
            ambience = new Loop(ModSounds.AMBIENT_SCHOOL_LAB.get(), mc.level, AMBIENCE_VOLUME, true);
            mc.getSoundManager().play(ambience);
        }
        HUMS.values().removeIf(hum -> hum.isStopped() || hum.level != mc.level);
    }

    /** A loop that fades in, and out once told to: all round the listener (the ambience) or at a pod. */
    private static final class Loop extends AbstractTickableSoundInstance {
        final ClientLevel level;
        final float full;
        boolean fading;

        Loop(SoundEvent sound, ClientLevel level, float full, boolean everywhere) {
            super(sound, everywhere ? SoundSource.AMBIENT : SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.level = level;
            this.full = full;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            if (everywhere) {
                this.relative = true;
                this.attenuation = SoundInstance.Attenuation.NONE;
            }
        }

        void at(BlockPos pos) {
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 1.0;
            this.z = pos.getZ() + 0.5;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (Minecraft.getInstance().level != level) {
                stop();
                return;
            }
            float target = fading ? 0.0F : full;
            volume = Mth.clamp(volume + Mth.clamp(target - volume, -0.02F, 0.02F), 0.0F, 1.0F);
            if (fading && volume <= 0.01F) {
                stop();
            }
        }
    }

    /** The client's ticks and its leaving a world (the game bus). */
    @EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            tick(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            Minecraft mc = Minecraft.getInstance();
            if (ambience != null) {
                mc.getSoundManager().stop(ambience);
                ambience = null;
            }
            HUMS.values().forEach(mc.getSoundManager()::stop);
            HUMS.clear();
            insideLab = false;
            fadeAt = Long.MIN_VALUE;
        }

        private Events() {
        }
    }

    private LabClient() {
    }
}

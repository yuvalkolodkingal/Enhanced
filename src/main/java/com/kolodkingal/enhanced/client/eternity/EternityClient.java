package com.kolodkingal.enhanced.client.eternity;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.love.LoveEyeBeamFx;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.LoveActing;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * The client of Eternity and Love (3.8): {@link #setup} registers the drawers of {@link EternityFxPayload} (the altar's
 * charge, the journey's white flash, the arrival, the wish's golden-white stars, the way home, Love's awakening, her
 * Thunder Strike's gesture), the wish screen's opener and Love's eye beams. In the realm: the white mist on the horizon
 * (fog from 48 to 200 blocks, never beyond what is drawn; client option {@code eternityFog}), and the realm's ambient
 * pad and soft rings of light round the walkers' feet on the still water; by an altar, the shrine's hum.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class EternityClient {
    /** The mist: its colour and where it begins and ends. */
    static final float MIST_R = 0.95F;
    static final float MIST_G = 0.96F;
    static final float MIST_B = 0.97F;
    static final float MIST_NEAR = 48.0F;
    static final float MIST_FAR = 200.0F;
    /** How long the white flash of a journey lasts (ticks). */
    static final float FLASH_TICKS = 24.0F;

    private static long flashAt = Long.MIN_VALUE;
    private static AmbientLoop ambient;
    /** Altars humming (by position) and their loops. */
    private static final Map<BlockPos, AmbientLoop> HUMS = new HashMap<>();

    /** Called from the client setup (ClientSetup's eternity region). */
    public static void setup() {
        EternityFx.screen(WishScreen::open);
        BeamRenderer.registerDrawer(AvengersBeams.LOVE_EYES, LoveEyeBeamFx.DRAWER);
        EternityFx.register(EternityFxPayload.ALTAR_CHARGE, EternityClient::charge);
        EternityFx.register(EternityFxPayload.ALTAR_GO, p -> flash());
        EternityFx.register(EternityFxPayload.ARRIVE, p -> flash());
        EternityFx.register(EternityFxPayload.LEAVE, p -> flash());
        EternityFx.register(EternityFxPayload.WISH_GRANTED, p -> burst(p, 0xFFF2C0, 0xFFFFFF, 90));
        EternityFx.register(EternityFxPayload.LOVE_AWAKEN, p -> burst(p, 0xC08CFF, 0xFFE59A, 70));
        EternityFx.register(EternityFxPayload.THUNDER_STRIKE, LoveActing::strike);
        EternityFx.register(EternityFxPayload.EYES_ON, p -> {
        });
        EternityFx.register(EternityFxPayload.EYES_OFF, p -> {
        });
    }

    // ---- the moments ----

    /** The altar begins to charge: a ring of violet sparks round it. */
    private static void charge(EternityFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(payload.pos()).add(0, 1.0, 0);
        for (int i = 0; i < 24; i++) {
            double a = Mth.TWO_PI * i / 24;
            level.addParticle(new EnergyParticleOptions(0xB57BFF, 1.0F), at.x + Math.cos(a) * 1.2, at.y,
                    at.z + Math.sin(a) * 1.2, -Math.cos(a) * 0.05, 0.06, -Math.sin(a) * 0.05);
        }
    }

    /** A white flash on the local player's screen (the journey to and from the realm). */
    private static void flash() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            flashAt = mc.level.getGameTime();
        }
    }

    /** A burst of stars round the player the payload names. */
    private static void burst(EternityFxPayload payload, int colour, int core, int count) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        Vec3 at = entity != null ? entity.position().add(0, 1.0, 0) : Vec3.atCenterOf(payload.pos());
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.8 + 0.3, random.nextGaussian())
                    .normalize().scale(0.15 + random.nextDouble() * 0.25);
            level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? core : colour, 1.2F), at.x, at.y, at.z, dir.x,
                    dir.y, dir.z);
            if (i % 4 == 0) {
                level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, dir.x * 0.6, dir.y * 0.6, dir.z * 0.6);
            }
        }
    }

    /** The white flash over the screen ({@code ClientSetup}'s GUI layers, eternity region). */
    public static final class FlashLayer implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics graphics, DeltaTracker delta) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || flashAt == Long.MIN_VALUE) {
                return;
            }
            float t = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false) - flashAt;
            if (t < 0 || t > FLASH_TICKS) {
                return;
            }
            float k = t < 4 ? t / 4 : 1.0F - (t - 4) / (FLASH_TICKS - 4);
            int alpha = (int) (Mth.clamp(k, 0, 1) * (EnhancedClientConfig.reducedMotion() ? 120 : 235));
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFFFFF);
        }
    }

    // ---- the mist ----

    @SubscribeEvent
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        if (inRealm()) {
            event.setRed(MIST_R);
            event.setGreen(MIST_G);
            event.setBlue(MIST_B);
        }
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (!inRealm() || event.getType() != FogType.NONE || !EnhancedClientConfig.eternityFog()) {
            return;
        }
        boolean sky = event.getMode() == FogRenderer.FogMode.FOG_SKY;
        float drawn = event.getFarPlaneDistance();
        float far = Math.min(MIST_FAR, Math.max(24.0F, drawn - 8.0F));
        float near = Math.min(MIST_NEAR, far * 0.35F);
        if (sky) {
            return; // (the sky dome is drawn without fog: its horizon is the mist's colour already)
        }
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }

    static boolean inRealm() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension() == EternityRealm.LEVEL;
    }

    // ---- the sounds ----

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (inRealm()) {
            if (ambient == null || ambient.isStopped()) {
                ambient = new AmbientLoop(ModSounds.ETERNITY_AMBIENT.get(), null, 0.7F, EternityClient::inRealm);
                mc.getSoundManager().play(ambient);
            }
        }
        HUMS.entrySet().removeIf(e -> e.getValue().isStopped());
        if (inRealm()) {
            ripples(mc);
        }
    }

    /** Footsteps on the still water: a soft ring of light spreads round each walker's feet every few steps. */
    private static void ripples(Minecraft mc) {
        ClientLevel level = mc.level;
        for (net.minecraft.world.entity.player.Player player : level.players()) {
            if (!player.onGround() || player.getY() > EternityRealm.MIRROR_Y + 1.1
                    || (player.tickCount + player.getId()) % 7 != 0) {
                continue;
            }
            Vec3 motion = player.position().subtract(player.xo, player.yo, player.zo);
            if (motion.horizontalDistanceSqr() < 1.0E-3) {
                continue;
            }
            double y = EternityRealm.MIRROR_Y + 1.02;
            for (int i = 0; i < 14; i++) {
                double a = Mth.TWO_PI * i / 14;
                double cos = Math.cos(a);
                double sin = Math.sin(a);
                level.addParticle(new EnergyParticleOptions(0xF4F6FF, 0.35F), player.getX() + cos * 0.3, y,
                        player.getZ() + sin * 0.3, cos * 0.045, 0.0, sin * 0.045);
            }
        }
    }

    /** An altar is drawn: it hums while the player is near (one loop per altar). */
    static void hum(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || HUMS.containsKey(pos) || mc.player.blockPosition().distSqr(pos) > 16 * 16) {
            return;
        }
        BlockPos at = pos.immutable();
        AmbientLoop loop = new AmbientLoop(ModSounds.SHRINE_HUM.get(), Vec3.atCenterOf(at), 0.6F,
                () -> mc.level != null && mc.player != null && mc.player.blockPosition().distSqr(at) <= 20 * 20
                        && mc.level.getBlockState(at)
                                .getBlock() instanceof com.kolodkingal.enhanced.block.EternityAltarBlock);
        HUMS.put(at, loop);
        mc.getSoundManager().play(loop);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HUMS.clear();
        ambient = null;
        flashAt = Long.MIN_VALUE;
    }

    /** A loop that plays while {@code active} holds (at a spot, or with the listener when {@code at} is null). */
    private static final class AmbientLoop extends AbstractTickableSoundInstance {
        private final BooleanSupplier active;
        private final float top;

        AmbientLoop(SoundEvent sound, Vec3 at, float top, BooleanSupplier active) {
            super(sound, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.active = active;
            this.top = top;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.02F;
            if (at == null) {
                this.relative = true;
                this.attenuation = Attenuation.NONE;
            } else {
                this.x = at.x;
                this.y = at.y;
                this.z = at.z;
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (!active.getAsBoolean()) {
                volume -= 0.05F;
                if (volume <= 0.0F) {
                    stop();
                }
                return;
            }
            volume = Math.min(top, volume + 0.03F);
        }
    }

    private EternityClient() {
    }
}

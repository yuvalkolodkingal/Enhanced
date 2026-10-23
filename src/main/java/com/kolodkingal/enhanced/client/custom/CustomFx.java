package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.modules.EnergyPowers;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * What every client draws of the custom heroes it sees (3.3): the Force Field (a sphere of his colours, as the Chaos
 * Shield is drawn), the trail he leaves while he runs at super speed, a glow of the energy he has absorbed, and, for the
 * local player only, Wall-Crawling (players move themselves, so the climb is the client's). The skins' cache is tidied
 * here too.
 */
@Subscriber(side = Side.CLIENT)
public final class CustomFx {
    private static final ResourceLocation LATTICE = Enhanced.id("textures/entity/hex_wall.png");

    /** Once per client tick, with a world (from the hero plug-in). */
    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            CustomSkins.tick();
        }
        if (!EnhancedClientConfig.flightEffects()) {
            return;
        }
        RandomSource random = level.random;
        for (AbstractClientPlayer player : level.players()) {
            if (!CustomHeroes.is(player) || player.isInvisible()) {
                continue;
            }
            CustomHero hero = CustomHeroes.hero(player);
            CustomState state = CustomHeroes.state(player);
            boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
            if (Powers.get(player).is(Live.SPEEDING)) {
                double dx = player.getX() - player.xOld;
                double dz = player.getZ() - player.zOld;
                if (dx * dx + dz * dz > 0.04) {
                    int color = hero.colorOf(hero.pick(PowerModule.SUPER_SPEED));
                    for (int i = 0; i < 3; i++) {
                        double back = 1.0 + random.nextDouble();
                        level.addParticle(new EnergyParticleOptions(i == 0 ? hero.glow() : color, 1.1F),
                                player.getX() - dx * back + (random.nextDouble() - 0.5) * 0.5, player.getY() + random.nextDouble() * 1.8,
                                player.getZ() - dz * back + (random.nextDouble() - 0.5) * 0.5, 0, 0, 0);
                    }
                }
            }
            CustomHero.Pick absorption = hero.pick(PowerModule.ABSORPTION);
            if (absorption != null && state.charge() > 0 && !firstPerson && level.getGameTime() % 4 == 0) {
                float full = state.charge() / ModuleStats.ABSORB_CAPACITY.value(absorption.strength());
                if (random.nextFloat() < 0.3F + full) {
                    level.addParticle(new EnergyParticleOptions(hero.colorOf(absorption), 0.7F + full * 0.6F),
                            player.getX() + (random.nextDouble() - 0.5) * 0.7, player.getY() + 0.3 + random.nextDouble() * 1.5,
                            player.getZ() + (random.nextDouble() - 0.5) * 0.7, 0, 0.02, 0);
                }
            }
        }
    }

    static void clear() {
    }

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (int pass = 0; pass < 2; pass++) {
            boolean lines = pass == 1;
            RenderType type = lines ? EnhancedRenderTypes.energyField(LATTICE) : EnhancedRenderTypes.ENERGY_VEIL;
            VertexConsumer vc = null;
            for (AbstractClientPlayer player : level.players()) {
                if (!CustomHeroes.is(player) || player.isInvisible()) {
                    continue;
                }
                CustomState state = CustomHeroes.state(player);
                if (!state.field()) {
                    continue;
                }
                CustomHero hero = CustomHeroes.hero(player);
                int color = hero.colorOf(hero.pick(PowerModule.FORCE_FIELD));
                float grown = state.fieldGrowth(time);
                float eased = grown * grown * (3F - 2F * grown);
                CustomHero.Pick pick = hero.pick(PowerModule.FORCE_FIELD);
                float max = pick == null ? 1F : ModuleStats.FIELD_STRENGTH.value(pick.strength());
                float weak = 1F - Mth.clamp(state.fieldStrength() / max / 0.35F, 0F, 1F);
                float flicker = (0.9F + 0.1F * Mth.sin(time * 0.6F)) * (0.4F + 0.6F * eased) * (1F - weak * 0.4F * (0.5F + 0.5F * Mth.sin(time * 2.2F)));
                float radius = (float) EnergyPowers.FIELD_RADIUS * (0.3F + 0.7F * eased);
                Vec3 center = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0).subtract(camera);
                if (vc == null) {
                    vc = buffers.getBuffer(type);
                }
                if (lines) {
                    EnergyGeometry.sphere(pose, vc, center, radius, 32, 16, 6, 3, time * 0.01F, hero.glow(), 0.35F * flicker, 0.65F * flicker);
                } else {
                    EnergyGeometry.veil(pose, vc, center, radius, 32, 16, color, 0.12F * flicker, 0.45F * flicker);
                }
            }
            if (vc != null) {
                buffers.endBatch(type);
            }
        }
    }

    /**
     * Wall-Crawling, for the local player: against a wall he climbs while he moves (Sneak holds him still on it), at the
     * speed of the power's strength ("fast" half again as fast).
     */
    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !CustomHeroes.isOn(player, PowerModule.WALL_CRAWL)
                || player.getAbilities().flying || player.isSpectator() || !player.horizontalCollision) {
            return;
        }
        CustomHero.Pick pick = CustomHeroes.pick(player, PowerModule.WALL_CRAWL);
        double speed = ModuleStats.CLIMB_SPEED.value(pick.strength()) * ("fast".equals(pick.option()) ? 1.5 : 1.0);
        Vec3 motion = player.getDeltaMovement();
        if (player.isShiftKeyDown()) {
            player.setDeltaMovement(motion.x, Math.max(motion.y, 0.0), motion.z);
        } else if (player.zza != 0 || player.xxa != 0 || player.input.jumping) {
            player.setDeltaMovement(motion.x, Math.max(motion.y, speed), motion.z);
        }
        player.resetFallDistance();
        player.minorHorizontalCollision = true;
    }

    @Subscribe
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CustomSkins.clear();
    }

    private CustomFx() {
    }
}

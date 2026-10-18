package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.PhoenixBurstPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the Phoenix draws in the world, for every player to see (the local player in first person too): the
 * firebird of ribbons that forms behind a charging Phoenix and the spirals of flame round her; the Phoenix Burst (a
 * flash, a shell of fire spreading from her at the wave's speed, the firebird flung open and flying out, streaks,
 * embers, ash, the scorch on the ground, a shake of the camera); the dome of the Psionic Field; and the violet shimmer
 * round the head of a Phoenix who reads minds. Modelled on {@link ThorEffects}: fixed-lifetime lists, {@link #add} for
 * the payload, {@link #clear} when the world goes.
 *
 * <p>Nothing here is sent by the server but the Burst itself ({@link PhoenixBurstPayload}); the charge, the field and
 * the reading are found from the synced {@link PowerData} of each player, as the other heroes' effects are.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class PhoenixFx {
    /** The texture of tongues of flame the shell and the body flame are drawn with. */
    public static final ResourceLocation FLAME_TEXTURE = Enhanced.id("textures/entity/phoenix_flame.png");
    private static final Vec3 UP = new Vec3(0, 1, 0);
    /** Nothing farther than this from the camera is drawn or sparked. */
    private static final double DRAW_DISTANCE = 192.0;
    private static final double PARTICLE_DISTANCE = 96.0;
    private static final int EMBERS = 150;
    private static final int FLAMES = 60;
    private static final int STREAKS = 48;
    private static final int SPIRALS = 3;
    private static final int SPIRAL_SAMPLES = 36;
    private static final int SHELL_SEGMENTS = 32;

    /** One Burst on its way: where, which way she faced, how charged, how far, when. */
    private record Burst(int caster, Vec3 centre, float yaw, float charge, float radius, long start, ResourceKey<Level> dimension,
                         double groundY, long seed) {
    }

    private static final List<Burst> BURSTS = new ArrayList<>();
    /** 3.5: the colour a custom hero's pick chose for what is being drawn now, or -1 (the Phoenix's fire and violet). */
    private static int tint = -1;
    /** 3.5: a grey copy of the flame lattice, tinted to a chosen colour. */
    private static final ResourceLocation FLAME_GREY = Enhanced.id("textures/entity/custom/phoenix_flame_grey.png");

    /** One of the Phoenix's colours, or it recoloured to the colour chosen for what is being drawn. */
    private static int c(int rgb) {
        return tint < 0 ? rgb : AbilityColors.recolor(rgb, tint);
    }

    /** The colour chosen for a Burst (its caster's pick of the Burst), or -1. */
    private static int tintOf(Burst burst) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? -1 : AbilityColors.chosen(level.getEntity(burst.caster()), HeroAbility.PHOENIX_BURST);
    }
    /** Phoenixes who charge now: the game time N went down (as the client saw the bit rise). */
    private static final Map<Integer, Long> CHARGING = new HashMap<>();
    /** The Psionic Fields that closed lately: the player and the game time. */
    private static final Map<Integer, Long> DOMES = new HashMap<>();
    /** Who had {@link Live#PHOENIX_PSIONIC} up on the last tick (to find it rising). */
    private static final Set<Integer> WAS_PSIONIC = new HashSet<>();

    // ------------------------------------------------------------------------------------------------------------
    // The payload

    /** The server says a Burst went off nearby. */
    public static void add(PhoenixBurstPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        // The floor under the Burst, for the scorch (she hovers 1.5 blocks up, the wave is a sphere).
        double ground = payload.centre().y - 2.4;
        Entity caster = level.getEntity(payload.caster());
        Entity looker = caster != null ? caster : mc.player;
        if (looker != null) {
            BlockHitResult hit = level.clip(new ClipContext(payload.centre(), payload.centre().add(0, -8, 0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, looker));
            if (hit.getType() != HitResult.Type.MISS) {
                ground = hit.getLocation().y;
            }
        }
        BURSTS.add(new Burst(payload.caster(), payload.centre(), payload.yaw(), payload.charge(), payload.radius(), payload.start(),
                level.dimension(), ground, payload.start() * 31L + payload.caster()));
        // The scream of the release, for everyone who sees her (the local Phoenix too: the poses are drawn for the camera).
        if (caster instanceof AbstractClientPlayer player) {
            HeroAnimator.play(player, GestureId.PHOENIX_BURST, 0.0F);
        }
    }

    public static void clear() {
        BURSTS.clear();
        CHARGING.clear();
        DOMES.clear();
        WAS_PSIONIC.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the HUD, the flame layer and the overlay ask

    /**
     * How charged this Phoenix's Burst is, 0 to 1, or -1 when she is not charging. (The client's own timer from the
     * moment the charge bit rose, over the ticks of a full charge.)
     */
    public static float chargeOf(AbstractClientPlayer player, float now) {
        Long start = CHARGING.get(player.getId());
        if (start == null || !Powers.get(player).is(Live.PHOENIX_CHARGING)) {
            return -1.0F;
        }
        return PhoenixMath.charge(now - start, PhoenixConfig.chargeTicks());
    }

    /** The gold on the local player's screen from the Bursts that went off round them, 0 to 1. */
    public static float screenFlash(Minecraft mc, float time) {
        if (mc.player == null || mc.level == null || EnhancedClientConfig.reducedMotion()) {
            return 0.0F;
        }
        float flash = 0.0F;
        Vec3 eye = mc.player.getEyePosition();
        for (Burst burst : BURSTS) {
            if (burst.dimension() == mc.level.dimension() && PhoenixMath.flashReaches(eye.distanceTo(burst.centre()), burst.radius())) {
                flash = Math.max(flash, PhoenixMath.screenFlash(time - burst.start()));
            }
        }
        return flash;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Every client tick: the edges of the synced data, and the particles

    /** Once a tick, from the plug-in's tick: finds a charge begin and a field close, and sparks the particles. */
    static void tick(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        long now = level.getGameTime();
        BURSTS.removeIf(b -> b.dimension() != level.dimension() || now - b.start() > PhoenixMath.BURST_TICKS + 2 || now < b.start() - 1);
        DOMES.values().removeIf(start -> now - start > PhoenixMath.DOME_TICKS + 2);
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        float thin = EnhancedClientConfig.flightEffects() ? 1.0F : 0.1F;
        RandomSource random = level.random;

        Set<Integer> seen = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            int id = player.getId();
            if (!data.acts(Power.PHOENIX)) {
                continue;
            }
            seen.add(id);
            if (data.is(Live.PHOENIX_CHARGING)) {
                CHARGING.putIfAbsent(id, now);
            } else {
                CHARGING.remove(id);
            }
            boolean psionic = data.is(Live.PHOENIX_PSIONIC);
            if (psionic && !WAS_PSIONIC.contains(id)) {
                DOMES.put(id, now);
            }
            if (psionic) {
                WAS_PSIONIC.add(id);
            } else {
                WAS_PSIONIC.remove(id);
            }
            if (player.isInvisible() || player.isSpectator() || player.position().distanceTo(camera) > PARTICLE_DISTANCE) {
                continue;
            }
            if (data.is(Live.PHOENIX_CHARGING)) {
                tint = AbilityColors.chosen(player, HeroAbility.PHOENIX_BURST);
                chargeParticles(level, camera, player, chargeOf(player, now), thin, random);
            }
            Long dome = DOMES.get(id);
            if (dome != null && now - dome <= PhoenixMath.DOME_GROW_TICKS) {
                tint = AbilityColors.chosen(player, HeroAbility.PHOENIX_PSIONIC_FIELD);
                domeParticles(level, camera, player, now - dome, thin, random);
            }
            if (data.toggle(Toggle.MIND_READING) && !data.is(Live.PHOENIX_CHARGING) && player.tickCount % 7 == player.getId() % 7
                    && random.nextFloat() < thin) {
                tint = AbilityColors.chosen(player, HeroAbility.PHOENIX_MIND);
                readingMote(level, camera, player, random);
            }
            tint = -1;
        }
        CHARGING.keySet().retainAll(seen);
        DOMES.keySet().retainAll(seen);
        WAS_PSIONIC.retainAll(seen);

        for (Burst burst : BURSTS) {
            tint = tintOf(burst);
            burstParticles(level, burst, now - burst.start(), camera, thin, random);
        }
        tint = -1;
    }

    /**
     * Embers rising from her shoulders and hands, and a flame from the hands now and then; more as the charge fills.
     */
    private static void chargeParticles(ClientLevel level, Vec3 camera, AbstractClientPlayer player, float charge, float thin, RandomSource random) {
        HumanoidArm[] sides = {HumanoidArm.RIGHT, HumanoidArm.LEFT};
        int count = Math.round((1 + 3 * Math.max(charge, 0.0F)) * thin);
        for (int i = 0; i < count; i++) {
            HumanoidArm side = sides[random.nextInt(2)];
            Vec3 at = random.nextBoolean() ? HandTracker.hand(player, side, 0.0F) : HandTracker.shoulder(player, side, 0.0F);
            if (at == null) {
                // The local player's body is not drawn: the embers rise from where her shoulders would be.
                double yaw = Math.toRadians(player.getYRot());
                double across = side == HumanoidArm.RIGHT ? 0.4 : -0.4;
                at = player.position().add(-Math.cos(yaw) * across, 1.4, -Math.sin(yaw) * across);
            }
            if (PhoenixMath.tooCloseToCamera(at.distanceToSqr(camera))) {
                continue; // her own shoulders, seen from inside her head
            }
            int colour = random.nextInt(3) == 0 ? c(PhoenixMath.FIRE_CORE) : random.nextBoolean() ? c(PhoenixMath.FIRE_GOLD) : c(PhoenixMath.FIRE_MAIN);
            level.addParticle(new EnergyParticleOptions(colour, 0.9F), at.x + (random.nextDouble() - 0.5) * 0.2, at.y,
                    at.z + (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.02, 0.05 + random.nextDouble() * 0.05,
                    (random.nextDouble() - 0.5) * 0.02);
        }
        if (player.tickCount % 4 == 0 && random.nextFloat() < thin) {
            Vec3 hand = HandTracker.hand(player, HumanoidArm.RIGHT, 0.0F);
            if (hand != null && !PhoenixMath.tooCloseToCamera(hand.distanceToSqr(camera))) {
                level.addParticle(ParticleTypes.FLAME, hand.x, hand.y, hand.z, 0.0, 0.03, 0.0);
            }
        }
    }

    /** A ring of violet motes at the edge of the dome as it grows. */
    private static void domeParticles(ClientLevel level, Vec3 camera, AbstractClientPlayer player, long age, float thin, RandomSource random) {
        float radius = PhoenixMath.domeRadius(PhoenixConfig.psionicRadius(), age);
        Vec3 centre = player.position().add(0, player.getBbHeight() * 0.5, 0);
        int count = Math.round(24 * thin);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double tilt = (random.nextDouble() - 0.5) * 0.6;
            double flat = Math.cos(tilt) * radius;
            int colour = random.nextInt(3) == 0 ? c(PhoenixAbilities.PSY_CORE) : c(PhoenixAbilities.PSY_MAIN);
            double x = centre.x + Math.cos(angle) * flat;
            double y = centre.y + Math.sin(tilt) * radius;
            double z = centre.z + Math.sin(angle) * flat;
            if (!PhoenixMath.tooCloseToCamera(camera.distanceToSqr(x, y, z))) {
                level.addParticle(new EnergyParticleOptions(colour, 1.0F), x, y, z, 0.0, 0.01, 0.0);
            }
        }
    }

    /** One violet mote drifting up round the head of a Phoenix who reads minds (about three a second). */
    private static void readingMote(ClientLevel level, Vec3 camera, AbstractClientPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double x = player.getX() + Math.cos(angle) * 0.4;
        double y = player.getEyeY() + (random.nextDouble() - 0.3) * 0.4;
        double z = player.getZ() + Math.sin(angle) * 0.4;
        if (PhoenixMath.tooCloseToCamera(camera.distanceToSqr(x, y, z))) {
            return; // her own head, seen from inside it
        }
        level.addParticle(new EnergyParticleOptions(random.nextBoolean() ? c(PhoenixAbilities.PSY_MAIN) : c(PhoenixAbilities.PSY_CORE), 0.7F), x, y, z,
                0.0, 0.015, 0.0);
    }

    /**
     * The Burst's particles: embers and small flames thrown off the shell as it spreads (about 150 and 60 in all,
     * radial and slowly falling), the firebird shedding embers as it dissolves, and a fall of ash at the end.
     */
    private static void burstParticles(ClientLevel level, Burst burst, long age, Vec3 camera, float thin, RandomSource random) {
        if (age < 0 || camera.distanceTo(burst.centre()) > PARTICLE_DISTANCE + burst.radius()) {
            return;
        }
        float speed = (float) PhoenixConfig.waveSpeed();
        float travel = Math.max(PhoenixMath.shellTicks(burst.radius(), speed), 6.0F);
        if (age <= travel) {
            float radius = PhoenixMath.shellRadius(burst.radius(), speed, age);
            int embers = Math.round(EMBERS / travel * thin);
            int flames = Math.round(FLAMES / travel * thin);
            // Nothing is sparked within three blocks of the camera: a mote that close to it is drawn as a
            // screen-filling square (and the shell is that close to whoever stands in the middle of it for its first
            // few ticks).
            for (int i = 0; i < embers; i++) {
                Vec3 dir = randomDirection(random);
                Vec3 at = burst.centre().add(dir.scale(radius));
                if (PhoenixMath.tooCloseToCamera(at.distanceToSqr(camera))) {
                    continue;
                }
                double push = 0.3 + random.nextDouble() * 0.3;
                int colour = random.nextInt(4) == 0 ? c(PhoenixMath.FIRE_CORE) : random.nextBoolean() ? c(PhoenixMath.FIRE_GOLD) : c(PhoenixMath.FIRE_MAIN);
                level.addParticle(new EnergyParticleOptions(colour, 1.0F + random.nextFloat() * 0.6F), at.x, at.y, at.z, dir.x * push,
                        dir.y * push - 0.02, dir.z * push);
            }
            for (int i = 0; i < flames; i++) {
                Vec3 dir = randomDirection(random);
                Vec3 at = burst.centre().add(dir.scale(radius));
                if (PhoenixMath.tooCloseToCamera(at.distanceToSqr(camera))) {
                    continue;
                }
                double push = 0.15 + random.nextDouble() * 0.2;
                level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, dir.x * push, dir.y * push, dir.z * push);
            }
        }
        // The firebird sheds embers as it dissolves.
        if (age >= PhoenixMath.BIRD_TICKS * 0.5F && age <= PhoenixMath.BIRD_TICKS) {
            List<Firebird.Strand> bird = bird(burst, age);
            int shed = Math.round(6 * thin);
            for (int i = 0; i < shed; i++) {
                Firebird.Strand strand = bird.get(random.nextInt(bird.size()));
                int k = random.nextInt(strand.points().length);
                Vec3 at = strand.points()[k];
                float t = k / (float) Math.max(1, strand.points().length - 1);
                level.addParticle(new EnergyParticleOptions(PhoenixMath.fireGradient(t), 1.0F), at.x, at.y, at.z, (random.nextDouble() - 0.5) * 0.1,
                        -0.02 + random.nextDouble() * 0.05, (random.nextDouble() - 0.5) * 0.1);
            }
        }
        // Ash falling at the end.
        if (age >= PhoenixMath.BURST_TICKS - 12) {
            int ash = Math.round(5 * thin);
            for (int i = 0; i < ash; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double r = Math.sqrt(random.nextDouble()) * burst.radius() * 0.8;
                level.addParticle(ParticleTypes.ASH, burst.centre().x + Math.cos(angle) * r, burst.centre().y + (random.nextDouble() - 0.3) * 4.0,
                        burst.centre().z + Math.sin(angle) * r, 0.0, -0.02, 0.0);
            }
        }
    }

    /** A direction on the sphere, uniformly. */
    private static Vec3 randomDirection(RandomSource random) {
        double z = random.nextDouble() * 2.0 - 1.0;
        double angle = random.nextDouble() * Mth.TWO_PI;
        double flat = Math.sqrt(1.0 - z * z);
        return new Vec3(Math.cos(angle) * flat, z, Math.sin(angle) * flat);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The camera

    /** A Burst shakes the camera of whoever is near: a quick roll and nod that die away. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (BURSTS.isEmpty() || mc.level == null || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        float time = mc.level.getGameTime() + (float) event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        float shake = 0.0F;
        for (Burst burst : BURSTS) {
            shake = Math.max(shake, PhoenixMath.shake(burst.charge(), camera.distanceTo(burst.centre()), time - burst.start()));
        }
        if (shake > 0.0F) {
            event.setRoll(event.getRoll() + shake * Mth.sin(time * 2.4F));
            event.setPitch(event.getPitch() + 0.6F * shake * Mth.cos(time * 3.1F));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        List<AbstractClientPlayer> charging = new ArrayList<>();
        List<AbstractClientPlayer> fields = new ArrayList<>();
        for (AbstractClientPlayer player : level.players()) {
            if (!Powers.get(player).acts(Power.PHOENIX) || player.isInvisible() || player.isSpectator()) {
                continue;
            }
            if (CHARGING.containsKey(player.getId()) && Powers.get(player).is(Live.PHOENIX_CHARGING)) {
                charging.add(player);
            }
            if (DOMES.containsKey(player.getId())) {
                fields.add(player);
            }
        }
        if (BURSTS.isEmpty() && charging.isEmpty() && fields.isEmpty()) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // Two passes for the domes and the shells: a see-through veil, then the glowing lines on top.
        for (int pass = 0; pass < 2; pass++) {
            boolean lines = pass == 1;
            RenderType type = lines ? EnhancedRenderTypes.energyField(FLAME_TEXTURE) : EnhancedRenderTypes.ENERGY_VEIL;
            VertexConsumer vc = buffers.getBuffer(type);
            for (Burst burst : BURSTS) {
                if (tintOf(burst) < 0) {
                    shell(pose, vc, camera, burst, time, lines);
                }
            }
            buffers.endBatch(type);
            // (3.5: a Burst in a chosen colour, on the grey lattice tinted.)
            RenderType grey = lines ? EnhancedRenderTypes.energyField(FLAME_GREY) : EnhancedRenderTypes.ENERGY_VEIL;
            VertexConsumer gc = buffers.getBuffer(grey);
            for (Burst burst : BURSTS) {
                tint = tintOf(burst);
                if (tint >= 0) {
                    shell(pose, gc, camera, burst, time, lines);
                }
            }
            tint = -1;
            buffers.endBatch(grey);
            RenderType domeType = lines ? EnhancedRenderTypes.energyField(WorldEffectsRenderer.HEX_TEXTURE) : EnhancedRenderTypes.ENERGY_VEIL;
            VertexConsumer dc = buffers.getBuffer(domeType);
            for (AbstractClientPlayer player : fields) {
                tint = AbilityColors.chosen(player, HeroAbility.PHOENIX_PSIONIC_FIELD);
                dome(pose, dc, camera, player, partialTick, time, lines);
            }
            tint = -1;
            buffers.endBatch(domeType);
        }

        // The ribbons in two batches, one render type after the other (asking the buffer source for the second ends the
        // first): the body of the flames alpha-blended, so that they stay orange against a bright sky, where light that
        // only adds up turns white; then the hot light on top, added.
        for (int pass = 0; pass < 2; pass++) {
            boolean glow = pass == 1;
            RenderType type = glow ? EnhancedRenderTypes.ENERGY_BEAM : EnhancedRenderTypes.ENERGY_VEIL;
            VertexConsumer vc = buffers.getBuffer(type);
            for (Burst burst : BURSTS) {
                tint = tintOf(burst);
                burstRibbons(pose, vc, glow, camera, burst, time);
            }
            for (AbstractClientPlayer player : charging) {
                tint = AbilityColors.chosen(player, HeroAbility.PHOENIX_BURST);
                charge(pose, vc, glow, camera, player, partialTick, time);
            }
            tint = -1;
            buffers.endBatch(type);
        }
    }

    /**
     * The expanding shell of fire: a veil sphere and, on top, lines of flame, both fading as the wave nears its radius.
     */
    private static void shell(Matrix4f pose, VertexConsumer vc, Vec3 camera, Burst burst, float time, boolean lines) {
        float age = time - burst.start();
        float speed = (float) PhoenixConfig.waveSpeed();
        float alpha = PhoenixMath.shellAlpha(burst.radius(), speed, age);
        if (alpha <= 0.0F || camera.distanceTo(burst.centre()) > DRAW_DISTANCE + burst.radius()) {
            return;
        }
        float radius = PhoenixMath.shellRadius(burst.radius(), speed, age);
        Vec3 centre = burst.centre().subtract(camera);
        float grain = 0.85F + 0.15F * Mth.sin(time * 0.9F);
        if (lines) {
            float tilesU = Math.max(6, Math.round(Mth.TWO_PI * radius / 4.0F));
            float tilesV = Math.max(3, Math.round(Mth.PI * radius / 4.0F));
            EnergyGeometry.sphere(pose, vc, centre, radius, SHELL_SEGMENTS, SHELL_SEGMENTS / 2, tilesU, tilesV, -time * 0.03F,
                    c(PhoenixMath.FIRE_MAIN), 0.40F * alpha * 2.0F * grain, 0.80F * alpha * 2.0F * grain);
        } else {
            EnergyGeometry.veil(pose, vc, centre, radius, SHELL_SEGMENTS, SHELL_SEGMENTS / 2, c(PhoenixMath.FIRE_EMBER), 0.14F * alpha * 2.0F,
                    0.70F * alpha * 2.0F);
        }
    }

    /**
     * The flash, the streaks, the firebird and the scorch of one Burst. {@code glow} says which batch this is: the
     * added light (the flash, the hot cores) or the alpha-blended body of the flames.
     */
    private static void burstRibbons(Matrix4f pose, VertexConsumer vc, boolean glow, Vec3 camera, Burst burst, float time) {
        float age = time - burst.start();
        if (age < 0.0F || camera.distanceTo(burst.centre()) > DRAW_DISTANCE + burst.radius()) {
            return;
        }
        float speed = (float) PhoenixConfig.waveSpeed();
        Vec3 centre = burst.centre().subtract(camera);

        float flash = PhoenixMath.flashRadius(burst.charge(), age);
        if (glow && flash > 0.0F) {
            flash(pose, vc, centre, flash, 1.0F - age / 3.0F);
        }

        // Streaks: forty-eight ribbons from the centre outward, gold at the front of the wave to red behind it.
        if (age <= PhoenixMath.STREAK_TICKS) {
            float fade = 1.0F - age / PhoenixMath.STREAK_TICKS;
            float reach = PhoenixMath.shellRadius(burst.radius(), speed, age);
            float length = Math.min(reach, 4.0F + 0.2F * burst.radius());
            RandomSource random = RandomSource.create(burst.seed());
            for (int i = 0; i < STREAKS; i++) {
                Vec3 dir = randomDirection(random);
                float jitter = 0.85F + 0.15F * random.nextFloat();
                Vec3 head = centre.add(dir.scale(reach * jitter));
                Vec3 tail = centre.add(dir.scale(Math.max(0.0F, reach * jitter - length)));
                if (glow) {
                    EnergyGeometry.ribbon(pose, vc, tail, head, 0.0F, 0.04F, EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.0F),
                            EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.6F * fade));
                } else {
                    int colour = PhoenixMath.mix(c(PhoenixMath.FIRE_MAIN), c(PhoenixMath.FIRE_GOLD), jitter * 2.0F - 1.0F);
                    EnergyGeometry.ribbon(pose, vc, tail, head, 0.01F, 0.10F, EnergyGeometry.argb(c(PhoenixMath.FIRE_EMBER), 0.0F),
                            EnergyGeometry.argb(colour, 0.85F * fade));
                }
            }
        }

        // The firebird, flung open and flying out along her facing.
        if (age <= PhoenixMath.BIRD_TICKS) {
            drawBird(pose, vc, glow, camera, bird(burst, age), PhoenixMath.burstBirdAlpha(age));
        }

        // The scorch: two rings on the ground, the wave's footprint.
        if (!glow && age <= PhoenixMath.BURST_TICKS) {
            float reach = PhoenixMath.shellRadius(burst.radius(), speed, age);
            double h = burst.centre().y - burst.groundY();
            float foot = reach > h ? (float) Math.sqrt(reach * reach - h * h) : 0.0F;
            float fade = 1.0F - age / PhoenixMath.BURST_TICKS;
            Vec3 ground = new Vec3(burst.centre().x, burst.groundY() + 0.05, burst.centre().z).subtract(camera);
            if (foot > 0.3F) {
                EnergyGeometry.ring(pose, vc, ground, UP, foot, 0.5F, 48, EnergyGeometry.argb(c(PhoenixMath.FIRE_DEEP), 0.55F * fade));
                EnergyGeometry.ring(pose, vc, ground, UP, foot * 0.65F, 0.35F, 40, EnergyGeometry.argb(c(PhoenixMath.FIRE_EMBER), 0.35F * fade));
            }
        }
    }

    /**
     * The firebird of a Burst at this age: scaled up, flown out along her facing, wings flung open and then rising
     * again.
     */
    private static List<Firebird.Strand> bird(Burst burst, float age) {
        Vec3 forward = new Vec3(-Mth.sin(burst.yaw() * Mth.DEG_TO_RAD), 0.0, Mth.cos(burst.yaw() * Mth.DEG_TO_RAD));
        Vec3 chest = burst.centre().add(forward.scale(0.2 + PhoenixMath.burstBirdDistance(burst.radius(), age)));
        return Firebird.build(chest, forward, UP, PhoenixMath.burstBirdScale(burst.radius(), age), PhoenixMath.burstWingAngle(age), age, true);
    }

    /** A white-gold flash facing the camera: a disc of rings and a cross of ribbons. */
    private static void flash(Matrix4f pose, VertexConsumer vc, Vec3 centre, float radius, float alpha) {
        Vec3 toCamera = centre.scale(-1);
        Vec3 side = toCamera.cross(UP);
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(toCamera.normalize()).normalize();
        EnergyGeometry.ring(pose, vc, centre, toCamera, radius * 0.7F, radius * 0.9F, 16, EnergyGeometry.argb(c(PhoenixMath.FIRE_GOLD), 0.35F * alpha));
        EnergyGeometry.ring(pose, vc, centre, toCamera, radius * 0.35F, radius * 0.7F, 14, EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.9F * alpha));
        EnergyGeometry.ribbon(pose, vc, centre.subtract(side.scale(radius * 1.6)), centre.add(side.scale(radius * 1.6)), radius * 0.2F,
                EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.8F * alpha), EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.8F * alpha));
        EnergyGeometry.ribbon(pose, vc, centre.subtract(up.scale(radius * 1.6)), centre.add(up.scale(radius * 1.6)), radius * 0.2F,
                EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.8F * alpha), EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.8F * alpha));
    }

    /**
     * A Phoenix who charges: the firebird forming behind and above her, growing as the charge fills (its wings come out
     * at a fifth), and three spirals of flame winding up her body.
     */
    private static void charge(Matrix4f pose, VertexConsumer vc, boolean glow, Vec3 camera, AbstractClientPlayer player, float partialTick, float time) {
        if (player.position().distanceTo(camera) > DRAW_DISTANCE) {
            return;
        }
        float f = Math.max(0.0F, PhoenixMath.charge(time - CHARGING.get(player.getId()), PhoenixConfig.chargeTicks()));
        float yaw = player.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 feet = player.getPosition(partialTick);
        float scale = PhoenixMath.chargeBirdScale(f);
        Vec3 chest = feet.subtract(forward.scale(1.0 + 1.5 * scale)).add(0.0, 1.2 + 0.6 * scale, 0.0);
        drawBird(pose, vc, glow, camera, Firebird.build(chest, forward, UP, scale, PhoenixMath.chargeWingAngle(time), time,
                PhoenixMath.chargeBirdHasWings(f)), 0.45F + 0.55F * f);

        // Three spirals of flame winding up her body, flowing upward and turning faster as the charge fills.
        float turn = time * (0.25F + 0.2F * f);
        float rise = (time * 0.15F / 2.4F) % 1.0F;
        for (int k = 0; k < SPIRALS; k++) {
            Vec3 previous = null;
            float previousFade = 0.0F;
            for (int i = 0; i <= SPIRAL_SAMPLES; i++) {
                float u = i / (float) SPIRAL_SAMPLES;
                float height = (u + rise) % 1.0F;
                float angle = Mth.TWO_PI * (3.0F * u) + turn + k * Mth.TWO_PI / SPIRALS;
                float radius = 0.8F + 0.8F * height;
                Vec3 at = feet.add(Math.cos(angle) * radius, height * 2.4F, Math.sin(angle) * radius).subtract(camera);
                float fade = Mth.sin(Mth.PI * height);
                // The wrap from the top of the column to its foot is not a ribbon.
                if (previous != null && at.y > previous.y - 1.0) {
                    float a = 0.85F * (0.4F + 0.6F * f);
                    // Thinner and fainter the nearer to the camera, so the spirals round her never fill her own screen.
                    float near = (float) Mth.clamp(at.length() / 3.0, 0.12, 1.0);
                    if (glow) {
                        EnergyGeometry.ribbon(pose, vc, previous, at, 0.025F * near, 0.025F * near, EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.7F * a * near * previousFade),
                                EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.7F * a * near * fade));
                    } else {
                        EnergyGeometry.ribbon(pose, vc, previous, at, 0.08F * near, 0.08F * near, EnergyGeometry.argb(c(PhoenixMath.FIRE_GOLD), a * near * previousFade),
                                EnergyGeometry.argb(c(PhoenixMath.FIRE_MAIN), a * near * fade));
                    }
                }
                previous = at;
                previousFade = fade;
            }
        }
    }

    /**
     * Draws a firebird: every strand from its root to its tip, white-hot at the root to ember red at the tip and
     * fainter toward the tip. Each strand twice over in the added batch ({@code glow}: a wide faint glow and a thin hot
     * core) and once in the alpha-blended one (the ribbon itself, which stays fire-coloured against a bright sky).
     */
    private static void drawBird(Matrix4f pose, VertexConsumer vc, boolean glow, Vec3 camera, List<Firebird.Strand> strands, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        for (Firebird.Strand strand : strands) {
            Vec3[] points = strand.points();
            int n = points.length;
            for (int k = 1; k < n; k++) {
                float t0 = (k - 1) / (float) (n - 1);
                float t1 = k / (float) (n - 1);
                Vec3 a = points[k - 1].subtract(camera);
                Vec3 b = points[k].subtract(camera);
                float w0 = strand.width() * (1.0F - 0.75F * t0);
                float w1 = strand.width() * (1.0F - 0.75F * t1);
                int c0 = PhoenixMath.fireGradient(t0);
                int c1 = PhoenixMath.fireGradient(t1);
                float a0 = PhoenixMath.fireAlpha(t0) * alpha;
                float a1 = PhoenixMath.fireAlpha(t1) * alpha;
                if (glow) {
                    EnergyGeometry.ribbon(pose, vc, a, b, w0 * 3.0F, w1 * 3.0F, EnergyGeometry.argb(c0, 0.25F * a0), EnergyGeometry.argb(c1, 0.25F * a1));
                    EnergyGeometry.ribbon(pose, vc, a, b, w0 * 0.5F, w1 * 0.5F, EnergyGeometry.argb(c(PhoenixMath.FIRE_CORE), 0.7F * a0),
                            EnergyGeometry.argb(PhoenixMath.mix(c(PhoenixMath.FIRE_CORE), c1, t1), 0.7F * a1));
                } else {
                    EnergyGeometry.ribbon(pose, vc, a, b, w0 * 1.6F, w1 * 1.6F, EnergyGeometry.argb(c0, a0), EnergyGeometry.argb(c1, a1));
                }
            }
        }
    }

    /** The dome of a Psionic Field: a violet veil and the lines of the hexagons over it, from her out to the radius. */
    private static void dome(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, float partialTick, float time, boolean lines) {
        float age = time - DOMES.get(player.getId());
        float alpha = PhoenixMath.domeAlpha(age);
        if (alpha <= 0.0F) {
            return;
        }
        float radius = PhoenixMath.domeRadius(PhoenixConfig.psionicRadius(), age);
        if (radius < 0.5F) {
            return;
        }
        Vec3 centre = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0).subtract(camera);
        int segments = Mth.clamp((int) (radius * 2), 32, 96);
        if (lines) {
            float tilesU = Math.max(6, Math.round(Mth.TWO_PI * radius / 6.0F));
            float tilesV = Math.max(3, Math.round(Mth.PI * radius / 6.0F));
            // The lines are the deeper violet, so that added to a bright sky they do not turn white.
            EnergyGeometry.sphere(pose, vc, centre, radius, segments, segments / 2, tilesU, tilesV, time * 0.0015F, c(PhoenixAbilities.PSY_BODY),
                    0.40F * alpha, 0.80F * alpha);
        } else {
            EnergyGeometry.veil(pose, vc, centre, radius, segments, segments / 2, c(PhoenixAbilities.PSY_FRINGE), 0.22F * alpha, 0.50F * alpha);
        }
    }

    private PhoenixFx() {
    }
}

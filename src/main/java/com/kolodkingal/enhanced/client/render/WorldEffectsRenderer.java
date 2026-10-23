package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.network.HexBoltPayload;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Draws the energy effects that live in the world rather than on a body: the Hex walls, the Chaos Beam, the Chaos
 * Shield, the telekinetic tether, Billy's Hex Bolts and the pulse and streams of Magneto's Magnetic Pull
 * ({@link MagnetStreams}). Everything is drawn for every player, including yourself in first person.
 */
@Subscriber(side = Side.CLIENT)
public final class WorldEffectsRenderer {
    public static final ResourceLocation HEX_TEXTURE = Enhanced.id("textures/entity/hex_wall.png");
    /** The Hex from outside, as in WandaVision: glowing CRT 'cathodic bricks' and scanlines. */
    public static final ResourceLocation HEX_BARRIER = Enhanced.id("textures/entity/hex_barrier.png");
    /** The angry red Hex (WandaVision episodes 5-9): raspberry scanlines over a dark magenta-red body. */
    private static final int HEX_LINES = 0xC03762;
    private static final int HEX_BODY = 0x6F0D28;
    private static final Vec3 UP = new Vec3(0, 1, 0);

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
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float time = level.getGameTime() + partialTick;

        double renderDistance = mc.options.getEffectiveRenderDistance() * 16.0;
        updateHexes(level, time);
        updateShields(level, time);
        // Two passes for every energy dome: a see-through red veil, then glowing lines on top (the Hex's scanlines and
        // bricks; the Chaos Shield's hexagons).
        for (int pass = 0; pass < 2; pass++) {
            boolean lines = pass == 1;
            RenderType hexType = lines ? EnhancedRenderTypes.energyField(HEX_BARRIER) : EnhancedRenderTypes.ENERGY_VEIL;
            renderHexes(pose, buffers.getBuffer(hexType), lines, camera, time, renderDistance);
            buffers.endBatch(hexType);
            // Every shield that is up, of whichever hero (the Chaos Shield, and the shields of the heroes of 3.0).
            Set<RenderType> drawn = new HashSet<>();
            for (AbstractClientPlayer player : level.players()) {
                PowerData data = Powers.get(player);
                ShieldStyle style =
                        ShieldStyles.of(player, data); // (3.4: a custom hero's shield too; 3.5: in its colour)
                if (style != null && data.chaosShieldActive() && !player.isInvisible()) {
                    RenderType type = lines ? EnhancedRenderTypes.energyField(style.texture()) : EnhancedRenderTypes.ENERGY_VEIL;
                    VertexConsumer vc = buffers.getBuffer(type);
                    drawn.add(type);
                    Vec3 center = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0).subtract(camera);
                    // It grows from the hands to its full size while it forms (and is fainter while it does).
                    ShieldStyle.Look look = style.look(data, time);
                    if (lines) {
                        EnergyGeometry.sphere(pose, vc, center, look.radius(), 32, 16, 6, 3, time * 0.01F,
                                style.lines(), 0.35F * look.flicker(), 0.65F * look.flicker());
                    } else {
                        EnergyGeometry.veil(pose, vc, center, look.radius(), 32, 16, style.veil(), 0.12F * look.flicker(),
                                0.45F * look.flicker());
                    }
                }
            }
            // The shields just lowered or broken, swelling a little as they fade.
            for (Map.Entry<Integer, Shield> going : SHIELDS_GOING.entrySet()) {
                Shield shield = going.getValue();
                if (!(level.getEntity(going.getKey()) instanceof AbstractClientPlayer player)) {
                    continue;
                }
                float u = Mth.clamp((time - shield.at()) / SHIELD_FADE_TICKS, 0.0F, 1.0F);
                float alpha = shield.flicker() * (1.0F - u) * (1.0F - u);
                float radius = shield.radius() * (1.0F + 0.2F * u);
                ShieldStyle style = shield.style();
                RenderType type = lines ? EnhancedRenderTypes.energyField(style.texture())
                        : EnhancedRenderTypes.ENERGY_VEIL;
                VertexConsumer vc = buffers.getBuffer(type);
                drawn.add(type);
                Vec3 center = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0).subtract(camera);
                if (lines) {
                    EnergyGeometry.sphere(pose, vc, center, radius, 32, 16, 6, 3, time * 0.01F, style.lines(),
                            0.35F * alpha, 0.65F * alpha);
                } else {
                    EnergyGeometry.veil(pose, vc, center, radius, 32, 16, style.veil(), 0.12F * alpha, 0.45F * alpha);
                }
            }
            for (RenderType type : drawn) {
                buffers.endBatch(type);
            }
        }

        VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (!OwnBeams.behind(mc, player)) {
                heroBeams(mc, player, data, pose, beams, camera, partialTick, time);
            }
            if (data.heldEntityId() != -1) {
                Entity held = level.getEntity(data.heldEntityId());
                if (held != null) {
                    renderTether(mc, player, held, data, pose, beams, camera, partialTick, time);
                }
            }
            if (data.has(HeroAbility.MAGNETO_SHIELD) && data.magneticShield() && !player.isInvisible()) {
                renderMagneticField(mc, player, pose, beams, camera, partialTick, time);
            }
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof HexVisionEntity vision && vision.beamTarget() != null) {
                renderVisionBeam(vision, vision.beamTarget(), pose, beams, camera, partialTick, time);
            }
        }
        renderHexBolts(mc, level, pose, beams, camera, partialTick, time);
        BeamRenderer.render(mc, level, pose, beams, camera, partialTick, time);
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        // 3.6: the camera's own beams seen from behind him, over his body (OwnBeams).
        if (mc.getCameraEntity() instanceof AbstractClientPlayer self && OwnBeams.behind(mc, self)) {
            VertexConsumer over = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM_OVER);
            heroBeams(mc, self, Powers.get(self), pose, over, camera, partialTick, time);
            BeamRenderer.renderOwn(mc, self, pose, over, camera, partialTick, time);
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM_OVER);
        }
        BarrierRenderer.render(mc, level, buffers, pose, camera, partialTick, time);
        // The Magnetic Pull: its pulse and lines of force, then the items flying to his hand (it ends its own batches).
        MagnetStreams.render(mc, level, buffers, camera, partialTick, level.getGameTime());
    }

    /** The Chaos Beam and Vision's Mind Beam of one player (3.4: by the ability, so a custom hero who has it fires it too). */
    private static void heroBeams(Minecraft mc, AbstractClientPlayer player, PowerData data, Matrix4f pose,
                                  VertexConsumer beams, Vec3 camera, float partialTick, float time) {
        if (data.has(HeroAbility.WANDA_CHAOS_BEAM) && data.beamActive()) {
            renderBeam(mc, player, handPosition(mc, player, partialTick), CHAOS.of(player, HeroAbility.WANDA_CHAOS_BEAM),
                    WandaAbilities.beamTargets(player), pose, beams, camera, partialTick, time);
        }
        if (data.has(HeroAbility.VISION_MIND_BEAM) && data.beamActive()) {
            Palette palette = "white_vision".equals(data.variant()) ? WHITE_VISION : MIND_STONE;
            renderBeam(mc, player, foreheadPosition(mc, player, partialTick),
                    palette.of(player, HeroAbility.VISION_MIND_BEAM), WandaAbilities.BEAM_TARGET, pose, beams, camera,
                    partialTick, time);
        }
    }

    // ---- Shields going down ----

    /** A shield as it was last drawn: its style, size and brightness,
            and when (the game time with the partial tick). */
    private record Shield(ShieldStyle style, float radius, float flicker, float at) {
    }

    /** How long a lowered or broken shield takes to fade away (3.3: it vanished from one frame to the next). */
    private static final float SHIELD_FADE_TICKS = 6.0F;
    private static final Map<Integer, Shield> SHIELDS_UP = new HashMap<>();
    private static final Map<Integer, Shield> SHIELDS_GOING = new HashMap<>();

    /** Remembers every shield that is up, and starts the fade of each one that has just gone down. */
    private static void updateShields(ClientLevel level, float time) {
        Set<Integer> up = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            ShieldStyle style = ShieldStyles.of(player, data); // (3.4: a custom hero's shield too; 3.5: in its colour)
            if (style != null && data.chaosShieldActive() && !player.isInvisible()) {
                ShieldStyle.Look look = style.look(data, time);
                SHIELDS_UP.put(player.getId(), new Shield(style, look.radius(), look.flicker(), time));
                SHIELDS_GOING.remove(player.getId());
                up.add(player.getId());
            }
        }
        SHIELDS_UP.entrySet().removeIf(entry -> {
            if (up.contains(entry.getKey())) {
                return false;
            }
            // (Not for one who turned invisible: his shield is not drawn either.)
            if (level.getEntity(entry.getKey()) instanceof AbstractClientPlayer player && !player.isInvisible()) {
                Shield last = entry.getValue();
                SHIELDS_GOING.put(entry.getKey(), new Shield(last.style(), last.radius(), last.flicker(), time));
            }
            return true;
        });
        SHIELDS_GOING.values().removeIf(shield -> time - shield.at() > SHIELD_FADE_TICKS || time < shield.at() - 1.0F);
    }

    public static void clearShields() {
        SHIELDS_UP.clear();
        SHIELDS_GOING.clear();
    }

    // ---- The Hex ----

    /**
     * What the client shows of each Hex (3.3): the radius it is drawn at, easing to the real one when the Scarlet Witch
     * resizes it (it jumped), and a Hex that was just dissolved, sinking and fading away for {@link #HEX_FADE_TICKS}
     * (it vanished at once). Only the picture lingers: the wall is gone on the server the moment it is dissolved.
     * 3.7.1: its runes follow it ({@link HexRunesRenderer}): the radius it is set to (how many runes each wall has), the
     * one it was set to before and when it changed (the runes of the old count fade into those of the new).
     */
    static final class ShownHex {
        Hex hex;
        float radius;
        float fade = 1.0F;
        boolean gone;
        int runeRadius;
        int runeRadiusBefore;
        float runeRadiusAt = Float.NEGATIVE_INFINITY;

        /** The radius its wall is drawn at now: rising when it was just raised, sinking when it was just dissolved. */
        float drawnRadius(float time) {
            float grow = Mth.clamp((time - hex.createdAt()) / 50F, 0F, 1F);
            grow = grow * grow * (3 - 2 * grow);
            return radius * grow * (0.85F + 0.15F * AnimMath.ss(0.0F, 1.0F, fade));
        }
    }

    private static final Map<UUID, ShownHex> SHOWN_HEXES = new HashMap<>();
    private static final float HEX_FADE_TICKS = 30.0F;
    private static float hexClock = Float.NaN;
    @Nullable
    private static ClientLevel hexLevel;

    /** The Hexes as shown this frame (3.7.1: {@link HexRunesRenderer} draws their runes on them). */
    static Collection<ShownHex> shownHexes() {
        return SHOWN_HEXES.values();
    }

    public static void clearHexes() {
        SHOWN_HEXES.clear();
        hexClock = Float.NaN;
        hexLevel = null;
    }

    /** Moves the shown Hexes on to this frame (once a frame, before the two passes draw them). */
    private static void updateHexes(ClientLevel level, float time) {
        if (level != hexLevel) {
            // Another world or dimension: its Hexes are its own.
            clearHexes();
            hexLevel = level;
        }
        float dt = Float.isNaN(hexClock) ? 0.0F : Mth.clamp(time - hexClock, 0.0F, 4.0F);
        hexClock = time;
        for (ShownHex shown : SHOWN_HEXES.values()) {
            shown.gone = true;
        }
        for (Hex hex : ModAttachments.HEXES.get(level).hexes()) {
            ShownHex shown = SHOWN_HEXES.computeIfAbsent(hex.id(), id -> new ShownHex());
            if (shown.hex == null) {
                shown.radius = hex.radius();
                shown.runeRadius = hex.radius();
                shown.runeRadiusBefore = hex.radius();
            } else if (hex.radius() != shown.runeRadius) {
                shown.runeRadiusBefore = shown.runeRadius;
                shown.runeRadius = hex.radius();
                shown.runeRadiusAt = time;
            }
            shown.hex = hex;
            shown.gone = false;
            shown.fade = 1.0F;
            shown.radius = AnimMath.ease(shown.radius, hex.radius(), dt, 5.0F);
        }
        SHOWN_HEXES.values().removeIf(shown -> shown.gone && (shown.fade -= dt / HEX_FADE_TICKS) <= 0.0F);
    }

    private static void renderHexes(Matrix4f pose, VertexConsumer vc, boolean lines, Vec3 camera, float time,
            double renderDistance) {
        for (ShownHex shown : SHOWN_HEXES.values()) {
            Hex hex = shown.hex;
            float grow = Mth.clamp((time - hex.createdAt()) / 50F, 0F, 1F);
            grow = grow * grow * (3 - 2 * grow);
            // A dissolved Hex sinks a little as it fades.
            float fade = AnimMath.ss(0.0F, 1.0F, shown.fade);
            float radius = shown.drawnRadius(time);
            if (radius < 0.5F || fade <= 0.0F) {
                continue;
            }
            Vec3 center = hex.center().subtract(camera);
            double distance = center.length();
            if (distance - radius > renderDistance + 32 || radius - distance > renderDistance + 32) {
                continue;
            }
            // (3.3) The mesh and the tiling of the bricks are those of the finished wall: counted from the growing
            // radius
            // they changed every few frames while it rose, and the bricks jumped.
            int segments = Mth.clamp(hex.radius() * 2, 32, 128);
            float pulse = (0.85F + 0.15F * Mth.sin(time * 0.06F)) * fade;
            // A wall that is still rising glows brighter.
            float rising = 1F + (1F - grow) * 1.5F;
            if (lines) {
                float tilesU = Math.max(6, Math.round(Mth.TWO_PI * hex.radius() / 6F));
                float tilesV = Math.max(3, Math.round(Mth.PI * hex.radius() / 6F));
                EnergyGeometry.sphere(pose, vc, center, radius, segments, segments / 2, tilesU, tilesV, time * 0.0015F,
                        hex.color() < 0 ? HEX_LINES : AbilityColors.recolor(HEX_LINES, hex.color()),
                                0.32F * pulse * rising,
                        0.68F * pulse * rising);
            } else {
                // Dense enough to tint the sky behind it raspberry red, as in the show.
                EnergyGeometry.veil(pose, vc, center, radius, segments, segments / 2,
                        hex.color() < 0 ? HEX_BODY : AbilityColors.recolor(HEX_BODY, hex.color()),
                        0.36F * pulse, 0.72F * pulse * rising);
            }
        }
    }

    // ---- Beams (Chaos Beam, Mind Stone beam) ----

    /** Colours of a beam: wide glow, inner glow, hot core, crackling bolts, impact flare. */
    private record Palette(int glow, int core, int bolt) {
        /**
         * 3.5: this palette recoloured to the colour the player's pick of {@code row} chose (as it is when none was).
         */
        Palette of(AbstractClientPlayer player, HeroAbility row) {
            if (AbilityColors.chosen(player, row) < 0) {
                return this;
            }
            return new Palette(AbilityColors.shade(player, row, glow), AbilityColors.shade(player, row, core),
                    AbilityColors.shade(player, row, bolt));
        }
    }

    /** Chaos magic: crimson, hot pink wisps, a near-white core (WandaVision, Multiverse of Madness). */
    private static final Palette CHAOS = new Palette(Power.SCARLET_WITCH.color, 0xFFE3E6, 0xF07C8D);
    /** The Mind Stone's beam: pale gold with a cream-white core (Age of Ultron). */
    private static final Palette MIND_STONE = new Palette(0xF5D77A, 0xFFF6D5, 0xF2C66A);
    /** White Vision's beam: pale cyan-white (WandaVision episode 9). */
    private static final Palette WHITE_VISION = new Palette(0x7EBADF, 0xE8FBFF, 0xA6E2F0);

    private static void renderBeam(Minecraft mc, AbstractClientPlayer player, Vec3 origin, Palette palette, Predicate<Entity> targets,
                                   Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        double range = EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.CHAOS_BEAM_RANGE.get() : 32;
        Vec3 target = Raycasts.any(player, range, targets, partialTick).getLocation();
        // 3.3: drawn to the wall that stops it, as the server stops it (a Chaos Shield, a Hex Barrier, a Hex's wall).
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(player.level(), player, player.getEyePosition(partialTick), target, partialTick);
        Vec3 end = (shield != null ? shield.point() : target).subtract(camera);
        drawBeam(origin.subtract(camera), end, palette, player.getId(), pose, vc, time);
    }

    /**
     * The Mind Stone beam of the Vision of a Hex: from his forehead to the creature he is fighting (a short beam; his
     * White Vision look does not exist, so it is always the stone's pale gold).
     */
    private static void renderVisionBeam(HexVisionEntity vision, LivingEntity target, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                                         float partialTick, float time) {
        Vec3 look = vision.getViewVector(partialTick);
        Vec3 start = vision.getEyePosition(partialTick).add(look.scale(0.28)).add(0, 0.18, 0);
        Vec3 end = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.6, 0);
        drawBeam(start.subtract(camera), end.subtract(camera), MIND_STONE, vision.getId(), pose, vc, time);
    }

    /** A beam from {@code start} to {@code end} (both camera-relative): glow, core, bolts and a flare where it ends. */
    private static void drawBeam(Vec3 start, Vec3 end, Palette palette, int seedId, Matrix4f pose, VertexConsumer vc, float time) {
        float flicker = 0.85F + 0.15F * Mth.sin(time * 1.7F) * Mth.cos(time * 0.9F);
        // The beam starts thin at its source and swells a little towards the target, so it never fills the screen in first
        // person; a source right in front of the camera is drawn thinner still.
        float near = (float) Mth.clamp(start.length() / 1.5, 0.25, 1.0);
        EnergyGeometry.ribbon(pose, vc, start, end, 0.12F * near, 0.55F * flicker, EnergyGeometry.argb(palette.glow, 0.10F), EnergyGeometry.argb(palette.glow, 0.30F));
        EnergyGeometry.ribbon(pose, vc, start, end, 0.06F * near, 0.26F * flicker, EnergyGeometry.argb(palette.glow, 0.45F), EnergyGeometry.argb(palette.glow, 0.55F));
        EnergyGeometry.ribbon(pose, vc, start, end, 0.02F * near, 0.08F, EnergyGeometry.argb(palette.core, 0.75F), EnergyGeometry.argb(palette.core, 0.85F));

        // (3.3) The bolts writhe from one shape to the next instead of jumping every other tick.
        long seed = seedId * 132897987541L;
        for (int k = 0; k < 3; k++) {
            EnergyGeometry.bolt(pose, vc, start, end, 0.03F, 0.28F, EnergyGeometry.argb(palette.bolt, 0.8F),
                    seed + k * 7919L, time / 2.0F);
        }
        // A flare where the beam hits.
        float flare = 0.5F + 0.2F * Mth.sin(time * 2.3F);
        Vec3 toCamera = end.scale(-1).normalize();
        Vec3 side = toCamera.cross(UP);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(toCamera).normalize();
        EnergyGeometry.ribbon(pose, vc, end.subtract(side.scale(flare)), end.add(side.scale(flare)), flare, EnergyGeometry.argb(palette.glow, 0.7F), EnergyGeometry.argb(palette.glow, 0.7F));
        EnergyGeometry.ribbon(pose, vc, end.subtract(up.scale(flare)), end.add(up.scale(flare)), flare * 0.5F, EnergyGeometry.argb(palette.core, 0.6F), EnergyGeometry.argb(palette.core, 0.6F));
    }

    // ---- Magnetic Shield ----

    /**
     * Magneto's field while the shield is on: faint looping field lines around him (like iron filings round a magnet),
     * with a glint running along each, and a shimmer around every projectile hanging in the field.
     */
    private static void renderMagneticField(Minecraft mc, AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                                            float partialTick, float time) {
        Vec3 center = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0);
        Vec3 c = center.subtract(camera);
        int lines = 7;
        int steps = 28;
        float[] shells = {2.4F, (float) MagnetoAbilities.SHIELD_RADIUS};
        int fieldGlow = AbilityColors.color(player, HeroAbility.MAGNETO_SHIELD,
                Power.MAGNETO.glow); // (3.5: its pick's colour)
        for (int shell = 0; shell < shells.length; shell++) {
            float l = shells[shell];
            for (int k = 0; k < lines; k++) {
                double phi = Mth.TWO_PI * k / lines + time * (shell == 0 ? 0.006 : -0.004);
                double cos = Math.cos(phi);
                double sin = Math.sin(phi);
                float glint = ((time * 0.015F + k * 0.37F + shell * 0.5F) % 1.0F);
                Vec3 prev = null;
                for (int i = 0; i <= steps; i++) {
                    // A dipole field line: r = L sin^2(theta), from near the top pole round to near the bottom one.
                    double theta = 0.12 * Math.PI + 0.76 * Math.PI * i / steps;
                    double r = l * Math.sin(theta) * Math.sin(theta);
                    Vec3 point = c.add(r * Math.sin(theta) * cos, r * Math.cos(theta), r * Math.sin(theta) * sin);
                    if (prev != null) {
                        float t = (float) i / steps;
                        float fade = Mth.sin(t * Mth.PI); // fades out towards the poles
                        float near = Math.max(0.0F, 1.0F - Math.abs(t - glint) * 9.0F);
                        int color = EnergyGeometry.argb(fieldGlow, (0.10F + 0.45F * near) * fade);
                        EnergyGeometry.ribbon(pose, vc, prev, point, 0.035F + 0.03F * near, color, color);
                    }
                    prev = point;
                }
            }
        }
        // Projectiles hanging in the field: a shimmering ring round each and a faint line of force to his hand.
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().inflate(MagnetoAbilities.SHIELD_RADIUS + 2.0),
                p -> p.isNoGravity() && p.getDeltaMovement().lengthSqr() < 1.0E-6 && MagnetoAbilities.isMetalProjectile(p))) {
            Vec3 at = projectile.getPosition(partialTick).add(0, projectile.getBbHeight() * 0.5, 0).subtract(camera);
            float pulse = 0.28F + 0.06F * Mth.sin(time * 0.5F + projectile.getId());
            EnergyGeometry.ring(pose, vc, at, at.scale(-1).normalize(), pulse, 0.03F, 16, EnergyGeometry.argb(fieldGlow, 0.7F));
            EnergyGeometry.bolt(pose, vc, handPosition(mc, player, partialTick).subtract(camera), at, 0.018F, 0.12F,
                    EnergyGeometry.argb(fieldGlow, 0.35F), projectile.getId() * 2862933555777941757L,
                            time / 3.0F);
        }
    }

    // ---- Billy's Hex Bolts ----

    /** How long a Hex Bolt stays on screen. */
    private static final int HEX_BOLT_TICKS = 6;

    private record HexBolt(int caster, Vec3 from, Vec3 to, List<Vec3> forks, long seed, long born) {
    }

    private static final List<HexBolt> HEX_BOLTS = new ArrayList<>();

    /** The server says a Hex Bolt was cast nearby. */
    public static void addHexBolt(HexBoltPayload bolt) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            HeroAnimator.onHexBolt(bolt.caster(), bolt.to());
            HEX_BOLTS.add(new HexBolt(bolt.caster(), bolt.from(), bolt.to(), List.copyOf(bolt.forks()), bolt.seed(), level.getGameTime()));
        }
    }

    public static void clearHexBolts() {
        HEX_BOLTS.clear();
    }

    /**
     * Every Hex Bolt still crackling: a jagged arc from Billy's hand to what it struck, with short branches, and an arc on
     * to each creature it forked to. It reshapes every other tick and fades out.
     */
    private static void renderHexBolts(Minecraft mc, ClientLevel level, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick,
                                       float time) {
        HEX_BOLTS.removeIf(b -> time - b.born() > HEX_BOLT_TICKS || time < b.born() - 1);
        for (HexBolt bolt : HEX_BOLTS) {
            float age = Math.max(0.0F, time - bolt.born());
            float fade = 1.0F - age / HEX_BOLT_TICKS;
            Entity caster = level.getEntity(bolt.caster());
            boltTint = AbilityColors.chosen(caster,
                    HeroAbility.BILLY_HEX_BEAM); // (3.5: the bolt in the colour its pick chose)
            Vec3 from = (caster instanceof AbstractClientPlayer player ? handPosition(mc, player, partialTick) : bolt.from()).subtract(camera);
            Vec3 to = bolt.to().subtract(camera);
            RandomSource random = RandomSource.create(bolt.seed() + (long) (age / 2) * 7919L);
            // Thinner where it leaves a hand right in front of the camera, so it never fills the screen.
            float near = (float) Mth.clamp(from.length() / 1.5, 0.3, 1.0);
            lightning(pose, vc, from, to, near, fade, random, 2 + random.nextInt(3));
            for (Vec3 fork : bolt.forks()) {
                lightning(pose, vc, to, fork.subtract(camera), 0.7F, fade, random, 1);
            }
            float flare = 0.35F * fade;
            EnergyGeometry.ring(pose, vc, to, to.scale(-1), flare, 0.12F, 12, EnergyGeometry.argb(bolt(BillyAbilities.MAIN), 0.5F * fade));
            EnergyGeometry.ring(pose, vc, to, to.scale(-1), flare * 0.4F, 0.08F, 8, EnergyGeometry.argb(bolt(BillyAbilities.CORE), 0.7F * fade));
        }
        boltTint = -1;
    }

    /** 3.5: the colour chosen for the bolt being drawn, or -1 (Billy's blues). */
    private static int boltTint = -1;

    private static int bolt(int blue) {
        return boltTint < 0 ? blue : AbilityColors.recolor(blue, boltTint);
    }

    /** One arc of Billy's lightning, with a few short branches crackling off it. */
    private static void lightning(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float scale, float fade, RandomSource random, int branches) {
        double length = a.distanceTo(b);
        if (length < 1.0E-3) {
            return;
        }
        List<Vec3> path = EnergyGeometry.lightningPath(a, b, Math.max(0.15, length * 0.08), random);
        boltLayers(pose, vc, path, scale, fade);
        Vec3 dir = b.subtract(a).scale(1.0 / length);
        for (int i = 0; i < branches && path.size() > 2; i++) {
            Vec3 start = path.get(1 + random.nextInt(path.size() - 2));
            double reach = 0.4 + random.nextDouble() * Math.min(1.8, length * 0.2);
            Vec3 side = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
            Vec3 end = start.add(dir.scale(reach * 0.6)).add(side.scale(reach * 0.8));
            boltLayers(pose, vc, EnergyGeometry.lightningPath(start, end, reach * 0.2, random), scale * 0.5F, fade * 0.8F);
        }
    }

    /** Billy's magic in layers: the dark blue fringe outside, then blue, light blue, and a white-hot core. */
    private static void boltLayers(Matrix4f pose, VertexConsumer vc, List<Vec3> path, float scale, float fade) {
        EnergyGeometry.polyline(pose, vc, path, 0.34F * scale, EnergyGeometry.argb(bolt(BillyAbilities.FRINGE), 0.22F * fade));
        EnergyGeometry.polyline(pose, vc, path, 0.18F * scale, EnergyGeometry.argb(bolt(BillyAbilities.BODY),
                0.45F * fade));
        EnergyGeometry.polyline(pose, vc, path, 0.09F * scale, EnergyGeometry.argb(bolt(BillyAbilities.MAIN),
                0.8F * fade));
        EnergyGeometry.polyline(pose, vc, path, 0.045F * scale, EnergyGeometry.argb(bolt(BillyAbilities.INNER),
                0.9F * fade));
        EnergyGeometry.polyline(pose, vc, path, 0.02F * scale, EnergyGeometry.argb(bolt(BillyAbilities.CORE), fade));
    }

    // ---- Telekinesis ----

    private static void renderTether(Minecraft mc, AbstractClientPlayer player, Entity held, PowerData data, Matrix4f pose,
                                     VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        Power power = data.power();
        Vec3 start = handPosition(mc, player, partialTick).subtract(camera);
        Vec3 end = held.getPosition(partialTick).add(0, held.getBbHeight() * 0.5, 0).subtract(camera);
        long seed = player.getId() * 6364136223846793005L;
        float phase = time / 3.0F;
        // The Phoenix's telekinesis is of the mind: violet, where the others' take their hero's colour.
        // (3.4: a custom hero's in his own colours, the Phoenix's grip in hers.)
        boolean mind = data.has(HeroAbility.PHOENIX_TELEKINESIS);
        int main = mind ? PhoenixAbilities.PSY_MAIN : CustomActing.tint(player, power.color, false);
        int glow = mind ? PhoenixAbilities.PSY_CORE : CustomActing.tint(player, power.glow, true);
        // 3.5: a hero's grip in the colour its pick chose.
        for (HeroAbility grip : GRIPS) {
            if (data.has(grip) && AbilityColors.chosen(player, grip) >= 0) {
                main = AbilityColors.shade(player, grip, main);
                glow = AbilityColors.shade(player, grip, glow);
                break;
            }
        }
        EnergyGeometry.bolt(pose, vc, start, end, 0.04F, 0.35F, EnergyGeometry.argb(main, 0.7F), seed, phase);
        EnergyGeometry.bolt(pose, vc, start, end, 0.025F, 0.5F, EnergyGeometry.argb(glow, 0.4F), seed + 31, phase);
    }

    /** The heroes' telekinesis abilities (one at most per custom hero: {@code Resource.GRIP}). */
    private static final HeroAbility[] GRIPS = {HeroAbility.MAGNETO_METAL, HeroAbility.WANDA_TELEKINESIS,
            HeroAbility.BILLY_TELEKINESIS,
            HeroAbility.PHOENIX_TELEKINESIS};

    /** Vision's Mind Stone, in the middle of his forehead (just above the screen centre in first person). */
    public static Vec3 foreheadPosition(Minecraft mc, AbstractClientPlayer player, float partialTick) {
        Vec3 tracked = HandTracker.forehead(player, partialTick);
        if (tracked != null) {
            return tracked;
        }
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            // A little way out from the top of the view, so the beam is seen pouring forward rather than end-on.
            Vec3 right = look.cross(UP);
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(look).normalize();
            return eye.add(look.scale(0.9)).add(up.scale(0.24));
        }
        return BodyLean.follow(player, eye, partialTick).add(look.scale(0.28)).add(0, 0.18, 0);
    }

    /** Where energy leaves the player's hands: in front of the chest, or bottom-right of the screen in first person. */
    public static Vec3 handPosition(Minecraft mc, AbstractClientPlayer player, float partialTick) {
        Vec3 tracked = HandTracker.hand(player, player.getMainArm(), partialTick);
        if (tracked != null) {
            return tracked;
        }
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            Vec3 right = look.cross(UP);
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(look).normalize();
            // Where the hand is drawn: its resting place, moved with the first-person gesture (the view-space offset is
            // about 0.8 blocks from the camera, so 0.4 of it is the offset in the world).
            float[] hand = new float[3];
            if (player == mc.player) {
                FirstPersonArms.mainHand(mc.player, hand);
            }
            double side = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0 : -1.0;
            return eye.add(look.scale(0.55 + 0.4 * hand[2])).add(right.scale(side * (0.2 + 0.4 * hand[0]))).add(up.scale(-0.22 + 0.4 * hand[1]));
        }
        return BodyLean.follow(player, eye, partialTick).add(0, -0.3, 0).add(look.scale(0.75));
    }

    private WorldEffectsRenderer() {
    }
}

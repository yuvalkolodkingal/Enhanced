package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The Scarlet Witch as the Sorceress Supreme (her looks {@code sorcerer_supreme} and {@code sorcerer_supreme_dark}): what
 * moves. Like Billy the Demiurge's cloak ({@link DemiurgeFx}), her Cloak of Levitation has a look at rest and a greater one
 * while she flies or uses a power: {@link #out} eases from 0 to 1 and back, and {@link SorcererCloakLayer} grows the cloth
 * by it (the train longer and fanned out, its edges lifted, the collar's two points larger). The change has a burst of
 * chaos magic and a chime; while it is out, rings of red energy turn at her hands, as on the comic's cover. {@link #ground}
 * tells the cloak whether she stands on something: there its train glides out over the ground instead of into it.
 * {@link #runes} is the light of the runes of her gloves: they are chaos energy, not paint, and show only while she uses
 * her magic (they come quicker than the cloak spreads, and fade when she stops); {@link SorcererGlowLayer} draws them.
 */
@Subscriber(side = Side.CLIENT)
public final class SorcererFx {
    /** The looks' ids begin with this (the dark-haired one is {@code sorcerer_supreme_dark}). */
    public static final String LOOK = "sorcerer_supreme";
    static final int RED = 0xFF2A44;
    static final int LIGHT = 0xFFD2D8;
    static final int GOLD = 0xF6C860;
    /** Ticks the greater look stays after she stops (so that it does not flicker between one spell and the next). */
    private static final int HOLD_TICKS = 12;

    /** Per player (entity id): how far the greater look is out (0 to 1), and how firmly she stands (0 in the air to 1). */
    private static final class Look {
        float amount;
        float amountO;
        float ground = 1.0F;
        float groundO = 1.0F;
        float runes;
        float runesO;
        boolean powered;
        int hold;
    }

    private static final Map<Integer, Look> LOOKS = new HashMap<>();

    private SorcererFx() {
    }

    /** Whether this player is the Scarlet Witch in a Sorceress Supreme look now (her crown on, not her own skin). */
    public static boolean worn(Player player) {
        PowerData data = Powers.get(player);
        if (com.kolodkingal.enhanced.ability.UltimateWanda.is(data)) {
            return true; // (3.19: Ultimate Wanda is always the Sorceress Supreme, her cloak, tiara and runes with her)
        }
        return data.power() == Power.SCARLET_WITCH && !data.ownSkin() && WandaAbilities.wearsCrown(player)
                && Variants.current(data).id().startsWith(LOOK);
    }

    /** Whether the greater look is called for now: she flies, or uses a power. */
    static boolean poweredNow(Player player, PowerData data) {
        return player.getAbilities().flying || data.flying() || DemiurgeFx.acting(data);
    }

    /** How far the greater look is out for her now (0 at rest to 1, eased). */
    public static float out(Player player, float partialTick) {
        Look look = LOOKS.get(player.getId());
        if (look == null) {
            return 0.0F;
        }
        float a = Mth.lerp(partialTick, look.amountO, look.amount);
        return a * a * (3.0F - 2.0F * a);
    }

    /** How firmly she stands (0 in the air to 1 on the ground, eased over a few ticks). */
    public static float ground(Player player, float partialTick) {
        Look look = LOOKS.get(player.getId());
        return look == null ? 1.0F : Mth.lerp(partialTick, look.groundO, look.ground);
    }

    /** How bright the runes of her gloves are now (0: none, she is not using her magic; to 1, eased). */
    public static float runes(Player player, float partialTick) {
        Look look = LOOKS.get(player.getId());
        if (look == null) {
            return 0.0F;
        }
        float a = Mth.lerp(partialTick, look.runesO, look.runes);
        return a * a * (3.0F - 2.0F * a);
    }

    public static void clear() {
        LOOKS.clear();
    }

    /** Every client tick: each Sorceress Supreme's cloak follows what she does; the change has its burst and chime. */
    public static void tick(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null) {
            return;
        }
        LOOKS.keySet().removeIf(id -> !(level.getEntity(id) instanceof Player p) || !worn(p));
        for (Player player : level.players()) {
            if (!worn(player)) {
                continue;
            }
            PowerData data = Powers.get(player);
            boolean fresh = !LOOKS.containsKey(player.getId());
            Look look = LOOKS.computeIfAbsent(player.getId(), id -> new Look());
            look.hold = poweredNow(player, data) ? HOLD_TICKS : Math.max(0, look.hold - 1);
            boolean now = look.hold > 0;
            if (now != look.powered && !fresh && !player.isInvisible()) {
                change(level, player, now);
            }
            look.powered = now;
            look.amountO = look.amount;
            // (the cloak takes about a second to spread, a little less to settle)
            look.amount = Mth.clamp(look.amount + (now ? 0.055F : -0.07F), 0.0F, 1.0F);
            // (the runes light in a third of a second as her magic begins, and fade in half a second after it ends)
            look.runesO = look.runes;
            look.runes = Mth.clamp(look.runes + (now ? 0.16F : -0.1F), 0.0F, 1.0F);
            look.groundO = look.ground;
            look.ground = Mth.clamp(look.ground + (player.onGround() ? 0.2F : -0.2F), 0.0F, 1.0F);
            if (fresh) {
                look.ground = look.groundO = player.onGround() ? 1.0F : 0.0F;
            }
        }
    }

    /** The moment the cloak spreads or settles: a burst of chaos magic round her waist, and a chime. */
    private static void change(ClientLevel level, Player player, boolean powered) {
        RandomSource random = level.random;
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.45, 0);
        int count = powered ? 22 : 10;
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double r = 0.45 + random.nextDouble() * 0.15;
            double speed = powered ? 0.16 : 0.06;
            level.addParticle(new EnergyParticleOptions(i % 4 == 0 ? LIGHT : i % 7 == 0 ? GOLD : RED, 0.5F + random.nextFloat() * 0.4F),
                    c.x + Math.cos(a) * r, c.y + (random.nextDouble() - 0.5) * 0.5, c.z + Math.sin(a) * r,
                    Math.cos(a) * speed, 0.02 + random.nextDouble() * 0.05, Math.sin(a) * speed);
        }
        level.playLocalSound(c.x, c.y, c.z, powered ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, powered ? 0.8F : 0.5F, powered ? 0.8F : 0.65F, false);
    }

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || LOOKS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        for (Player p : level.players()) {
            if (!LOOKS.containsKey(p.getId()) || !(p instanceof AbstractClientPlayer player) || player.isInvisible()
                    || player.isSpectator() || player.distanceToSqr(camera) > 96 * 96) {
                continue;
            }
            float out = out(player, partial);
            boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
            if (out <= 0.02F || firstPerson) {
                continue; // (the rings are not drawn over her own view)
            }
            if (vc == null) {
                vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            }
            rings(player, pose, vc, camera, partial, time, out);
        }
        if (vc != null) {
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        }
    }

    /** The rings of red chaos energy at her two hands: two turning rings, a bright core, and a needle of light through them. */
    private static void rings(AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick,
                              float time, float out) {
        Vec3 feet = player.getPosition(partialTick).subtract(camera);
        float h = player.getBbHeight();
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        for (int s = -1; s <= 1; s += 2) {
            // (the hand where it was drawn this frame; before the first frame, about where a hand hangs)
            Vec3 tracked = HandTracker.hand(player, s < 0 ? HumanoidArm.RIGHT : HumanoidArm.LEFT, partialTick);
            Vec3 hand = tracked != null ? tracked.subtract(camera)
                    : feet.add(0, h * 0.42, 0).add(fwd.scale(0.25)).add(side.scale(0.42 * s));
            Vec3 normal = side.scale(s).add(fwd.scale(0.5)); // (side is her left: -1 is her right hand)
            float pulse = 0.88F + 0.12F * Mth.sin(time * 0.28F + s);
            float a = out * 0.75F;
            EnergyGeometry.ring(pose, vc, hand, normal, 0.36F * pulse, 0.05F, 26, EnergyGeometry.argb(RED, a));
            EnergyGeometry.ring(pose, vc, hand, normal, 0.24F * pulse, 0.03F, 20, EnergyGeometry.argb(RED, 0.7F * a));
            EnergyGeometry.ring(pose, vc, hand, normal, 0.30F * pulse, 0.015F, 24, EnergyGeometry.argb(LIGHT, 0.85F * a));
            // the needle: a thin line of light through the rings, slowly turning
            float turn = time * 0.05F * s;
            Vec3 axis = new Vec3(0, Mth.cos(turn), 0).add(fwd.scale(Mth.sin(turn))).normalize().scale(0.62F * pulse);
            int tip = EnergyGeometry.argb(RED, 0.0F);
            int mid = EnergyGeometry.argb(LIGHT, 0.8F * a);
            EnergyGeometry.ribbon(pose, vc, hand, hand.add(axis), 0.03F, 0.0F, mid, tip);
            EnergyGeometry.ribbon(pose, vc, hand, hand.subtract(axis), 0.03F, 0.0F, mid, tip);
        }
    }
}

package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.SpeedSurfaces;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The look of a speedster running on a wall or a ceiling (3.3), besides the body turned onto it (that is the
 * animator's): the blur he leaves, laid along the body as it is turned (out from the wall, down from the ceiling) in his
 * colours, and, on his own screen, speed lines streaming out from the middle in his colours (silver for Quicksilver,
 * teal-green for Tommy), so a run up a wall reads in first person too. Both fade in and out.
 */
public final class SpeedSurfaceFx {
    /** 3.5: the wall-run ability of a custom hero (Quicksilver's or Tommy's), for its colour. */
    private static HeroAbility wallRow(Player player) {
        return AbilityColors.first(player, HeroAbility.QUICKSILVER_WALL_RUN, HeroAbility.TOMMY_WALL_RUN);
    }

    private SpeedSurfaceFx() {
    }

    /**
     * Lays the tick's blur of a runner on a surface, and says whether it did (else the ground's blur is laid). The
     * runner moved {@code (dx, dy, dz)} this tick.
     */
    static boolean trail(ClientLevel level, AbstractClientPlayer player, PowerData data, RandomSource random, double dx, double dy, double dz) {
        HeroAnimState s = HeroAnimator.peek(player);
        boolean onSurface = data.is(Live.SURFACE_WALL) || data.is(Live.SURFACE_CEILING)
                || player == Minecraft.getInstance().player && SpeedsterMotion.runner().surface.touches();
        if (s == null || s.surfaceKind == 0 || !onSurface || dx * dx + dy * dy + dz * dz < 0.04) {
            return false;
        }
        boolean tommy = data.power() == Power.TOMMY;
        // (3.4: a custom hero's blur is in his own colours.)
        boolean custom = data.power() == Power.CUSTOM;
        // The body's axis from the feet: out of the wall, or down from the ceiling.
        Vec3 axis = s.surfaceKind == 2 ? new Vec3(0, -1, 0) : s.surfaceNormal;
        Vec3 feet = s.surfaceKind == 2 ? player.position().add(0, player.getBbHeight(), 0) : player.position().add(axis.scale(-0.3)).add(0, 0.85, 0);
        int count = tommy ? 4 : 3;
        for (int i = 0; i < count; i++) {
            double back = 0.8 + random.nextDouble() * 1.4;
            double along = random.nextDouble() * (tommy ? random.nextDouble() : 1.0) * 1.7;
            int color = custom ? (i == 0 ? Powers.glow(player) : Powers.color(player))
                    : tommy ? (i == 0 ? 0xE6FFF5 : i == 1 ? Power.TOMMY.glow : Power.TOMMY.color)
                    : (i == 0 ? Power.QUICKSILVER.glow : Power.QUICKSILVER.color);
            color = AbilityColors.shade(player, wallRow(player), color); // (3.5: the wall run's own colour)
            level.addParticle(new EnergyParticleOptions(color, 1.0F + random.nextFloat() * 0.3F),
                    feet.x - dx * back + axis.x * along + (random.nextDouble() - 0.5) * 0.4,
                    feet.y - dy * back + axis.y * along + (random.nextDouble() - 0.5) * 0.4,
                    feet.z - dz * back + axis.z * along + (random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
        }
        return true;
    }

    /** The speed lines on the local runner's screen while he runs on a wall or a ceiling. */
    public static final class SpeedLines implements LayeredDraw.Layer {
        private static final int LINES = 28;
        private float strength;

        @Override
        public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            PowerData data = player == null ? null : Powers.get(player);
            boolean on = data != null && SpeedSurfaces.isSpeedster(data) && SpeedsterMotion.runner().surface.touches()
                    && EnhancedClientConfig.flightEffects();
            strength += ((on ? 1.0F : 0.0F) - strength) * Math.min(1.0F, deltaTracker.getRealtimeDeltaTicks() * 0.2F);
            if (strength < 0.01F || mc.options.hideGui || player == null) {
                return;
            }
            int rgb = data.power() == Power.CUSTOM ? Powers.glow(player) : data.power() == Power.TOMMY ? Power.TOMMY.glow : Power.QUICKSILVER.glow;
            rgb = AbilityColors.glow(player, wallRow(player), rgb);
            float time = player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false);
            int w = graphics.guiWidth();
            int h = graphics.guiHeight();
            float reach = (float) Math.hypot(w, h) / 2.0F;
            PoseStack pose = graphics.pose();
            for (int i = 0; i < LINES; i++) {
                // Each line has its own angle and pace (the same every frame), and streams outwards, fading in and out.
                float angle = i * 2.39996F * Mth.RAD_TO_DEG;
                float pace = 0.09F + 0.05F * ((i * 7) % 5) / 4.0F;
                float u = (time * pace + i * 0.37F) % 1.0F;
                float inner = reach * (0.35F + 0.6F * u);
                float length = reach * (0.10F + 0.08F * ((i * 3) % 4) / 3.0F);
                float fade = Mth.sin(u * Mth.PI) * strength;
                int alpha = (int) (110 * fade);
                if (alpha < 3) {
                    continue;
                }
                pose.pushPose();
                pose.translate(w / 2.0F, h / 2.0F, 0.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(angle));
                graphics.fill(Math.round(inner), 0, Math.round(inner + length), 1, alpha << 24 | rgb);
                pose.popPose();
            }
        }
    }
}

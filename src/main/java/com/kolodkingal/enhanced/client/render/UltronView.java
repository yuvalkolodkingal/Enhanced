package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.client.UltronLoops;
import com.kolodkingal.enhanced.client.UltronMusic;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What the local player sees of the hero Ultron besides the world: his five HUD lines (the plasma left, flight, the
 * pull's rest, the Upload's rest or "cut off", the drones he has), the eyes that glow (his look's colour, brighter while
 * he fires, charges or hacks), the once-a-tick upkeep of his client (the palm that gathers the Overcharge, the music and
 * loops of the fight) and the clearing of all of it when the world changes.
 */
final class UltronView {
    private static final String[] LOOKS = {"prime", "sentry", "human"};

    // ---- the HUD ----

    @Nullable
    static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        return switch (ability) {
            case 1 -> plasma(player, data, now);
            case 2 -> onOff(data.flying());
            case 3 -> PowerHud.cooldown(data, 3, now);
            case 4 -> isCut(player) ? Component.translatable("hud.enhanced.ultron.cut_off").withStyle(ChatFormatting.RED) : PowerHud.cooldown(data, 4, now);
            case 5 -> drones(player, data, now);
            default -> null;
        };
    }

    /** "Plasma 24/30" (red when it is dry), "charging" while the Overcharge builds, and the Overcharge's rest after it. */
    private static Component plasma(LocalPlayer player, PowerData data, long now) {
        int max = UltronConfig.BOLT_POOL.get();
        if (data.is(Live.ULTRON_CHARGING)) {
            return Component.translatable("hud.enhanced.ultron.charging").withStyle(ChatFormatting.GOLD);
        }
        int pool = player.getAbilities().instabuild ? max : Math.min(data.ironCharge(), max);
        ChatFormatting colour = pool <= 0 ? ChatFormatting.RED : pool * 4 <= max ? ChatFormatting.YELLOW : ChatFormatting.AQUA;
        Component line = Component.translatable("hud.enhanced.ultron.plasma", pool, max).withStyle(colour);
        Component rest = PowerHud.cooldown(data, 1, now);
        return rest == null ? line : line.copy().append(Component.literal("  ")).append(rest);
    }

    /** "Drones 3/6" and the Sentry Army's rest. */
    private static Component drones(LocalPlayer player, PowerData data, long now) {
        int count = UltronActing.dronesNear(player, 128.0);
        Component line = Component.translatable("hud.enhanced.ultron.drones", count, UltronConfig.DRONE_MAX.get())
                .withStyle(count > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY);
        Component rest = PowerHud.cooldown(data, 5, now);
        return rest == null ? line : line.copy().append(Component.literal("  ")).append(rest);
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    /** Whether a Vision stands near enough to cut this player's uploads (the same rule as the server's). */
    static boolean isCut(Player player) {
        double range = UltronConfig.VISION_CUT_RANGE.get();
        if (range <= 0.0) {
            return false;
        }
        for (Player other : player.level().players()) {
            if (other.isAlive() && !other.isSpectator() && Powers.is(other, Power.VISION) && other.position().distanceToSqr(player.position()) <= range * range) {
                return true;
            }
        }
        return false;
    }

    // ---- the eyes ----

    /** The masks of his glowing eyes (the pixels of the eyes in the skin): idle and hot, for each of his looks. */
    private static final RenderType[] IDLE = new RenderType[LOOKS.length];
    private static final RenderType[] HOT = new RenderType[LOOKS.length];

    static {
        for (int i = 0; i < LOOKS.length; i++) {
            IDLE[i] = RenderType.eyes(Enhanced.id("textures/entity/ultron_eyes_" + LOOKS[i] + ".png"));
            HOT[i] = RenderType.eyes(Enhanced.id("textures/entity/ultron_eyes_" + LOOKS[i] + "_hot.png"));
        }
    }

    /** His eyes always burn, in the colour of his look (the amber of Prime for his own skin), brighter while he works. */
    @Nullable
    static RenderType eyes(AbstractClientPlayer player, PowerData data) {
        String look = Powers.variant(player).id();
        int index = 0;
        for (int i = 0; i < LOOKS.length; i++) {
            if (LOOKS[i].equals(look)) {
                index = i;
            }
        }
        boolean hot = data.beamActive() || data.is(Live.ULTRON_CHARGING) || data.is(Live.CASTING);
        return hot ? HOT[index] : IDLE[index];
    }

    // ---- the client's life ----

    /** Once per client tick, with a world: the palm that gathers the Overcharge, and the music and loops of the fight. */
    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        UltronMusic.tick(mc);
        UltronLoops.tick(mc);
        for (Player player : level.players()) {
            if (player instanceof AbstractClientPlayer client && player.isAlive() && Powers.get(player).power() == Power.ULTRON
                    && Powers.get(player).is(Live.ULTRON_CHARGING)) {
                gather(level, mc, client);
            }
        }
    }

    /** Motes of the plasma gather into the raised palm: they run in from a wide ring and the palm itself glows. */
    private static void gather(ClientLevel level, Minecraft mc, AbstractClientPlayer player) {
        Vec3 palm = HandTracker.hand(player, player.getMainArm(), 0.0F);
        if (palm == null) {
            return;
        }
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        float size = firstPerson ? 0.5F : 1.0F;
        RandomSource random = level.random;
        for (int i = 0; i < (firstPerson ? 2 : 5); i++) {
            Vec3 away = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize().scale(0.9 + random.nextDouble() * 0.6);
            Vec3 from = palm.add(away);
            Vec3 in = away.scale(-0.12);
            int colour = random.nextInt(3) == 0 ? 0xFFF3D6 : random.nextInt(2) == 0 ? 0xFFD27A : 0xFFA23A;
            level.addParticle(new EnergyParticleOptions(colour, size), from.x, from.y, from.z, in.x, in.y, in.z);
        }
        level.addParticle(new EnergyParticleOptions(0xFFF3D6, firstPerson ? 0.7F : 2.0F), palm.x, palm.y, palm.z, 0.0, 0.0, 0.0);
    }

    static void clear() {
        UltronMusic.stop();
        UltronLoops.stopAll();
    }

    private UltronView() {
    }
}

package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.client.MindReadingOverlay;
import com.kolodkingal.enhanced.client.MindSense;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What the local player sees of the Phoenix besides the world: her five HUD lines (Mind Reading on or off with the Mind
 * Sense recharge, flight, "holding", the host she has taken, the Burst's charge), the eyes that glow (gold fire while
 * she charges or burns, violet while she uses her mind), and the once-a-tick upkeep of her client (the effects, the
 * Mind Sense ping, the look for her host).
 */
final class PhoenixView {
    /** How often the client looks for the creature she has taken over (ticks), and how far (blocks). */
    private static final int HOST_PERIOD = 10;
    private static final double HOST_RANGE = 32.0;

    /** The name of the creature the local Phoenix has taken, as of the last look. */
    @Nullable
    private static Component host;

    // ---- the HUD ----

    @Nullable
    static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        return switch (ability) {
            case 1 -> reading(data, now);
            case 2 -> onOff(data.flying());
            case 3 -> data.heldEntityId() != -1 ? Component.translatable("hud.enhanced.holding").withStyle(ChatFormatting.AQUA) : null;
            case 4 -> control(data, now);
            case 5 -> burst(player, data, now);
            default -> null;
        };
    }

    /** Mind Reading on or off, and (while Mind Sense is recovering) how long it still needs. */
    private static Component reading(PowerData data, long now) {
        Component status = onOff(data.toggle(Toggle.MIND_READING));
        long left = data.cooldownUntil(1) - now;
        if (left > 0) {
            status = status.copy().append(Component.literal("  ")).append(Component.translatable("hud.enhanced.phoenix_sense",
                    String.format(Locale.ROOT, "%.1fs", left / 20.0)).withStyle(ChatFormatting.GRAY));
        }
        return status;
    }

    /** The cooldown of Mind Control and the Psionic Field, and the name of the creature she has taken. */
    @Nullable
    private static Component control(PowerData data, long now) {
        Component cooldown = PowerHud.cooldown(data, 4, now);
        if (host == null) {
            return cooldown;
        }
        Component taken = Component.translatable("hud.enhanced.phoenix_host", host).withStyle(s -> s.withColor(PhoenixAbilities.PSY_MAIN));
        return cooldown == null ? taken : cooldown.copy().append(Component.literal("  ")).append(taken);
    }

    /** "charging 63%" in gold (orange above nine tenths) while the Burst gathers; else its recharge. */
    @Nullable
    private static Component burst(LocalPlayer player, PowerData data, long now) {
        float charge = PhoenixFx.chargeOf(player, now);
        if (charge < 0.0F) {
            return PowerHud.cooldown(data, 5, now);
        }
        return Component.translatable("hud.enhanced.phoenix_charging", PhoenixMath.percent(charge))
                .withStyle(s -> s.withColor(charge >= PhoenixMath.BRIGHT_CHARGE ? PhoenixMath.FIRE_MAIN : PhoenixMath.FIRE_GOLD));
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    // ---- the eyes ----

    /** The two masks of her glowing eyes (the pixels of the eyes in the skin, painted fire and violet). */
    private static final RenderType FIRE_EYES = RenderType.eyes(Enhanced.id("textures/entity/phoenix_eyes.png"));
    private static final RenderType PSYCHIC_EYES = RenderType.eyes(Enhanced.id("textures/entity/phoenix_eyes_psychic.png"));

    /** Gold fire while she charges or burns, else violet while she reads, holds, casts or senses, else none. */
    @Nullable
    static RenderType eyes(AbstractClientPlayer player, PowerData data) {
        if (data.is(Live.PHOENIX_CHARGING) || data.is(Live.PHOENIX_BLAZE)) {
            return FIRE_EYES;
        }
        boolean psychic = data.heldEntityId() != -1 || data.toggle(Toggle.MIND_READING) || data.is(Live.CASTING) || data.is(Live.MIND_SENSING)
                || data.is(Live.PHOENIX_PSIONIC);
        return psychic ? PSYCHIC_EYES : null;
    }

    // ---- the client's life ----

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        PhoenixFx.tick(level);
        MindSense.tick(mc);
        if (level.getGameTime() % HOST_PERIOD == 0) {
            lookForHost(mc, level);
        }
    }

    /** The creature within 32 blocks that the local Phoenix has taken over, if any (the HUD names it). */
    private static void lookForHost(Minecraft mc, ClientLevel level) {
        host = null;
        LocalPlayer me = mc.player;
        if (me == null || !me.isAlive() || Powers.get(me).power() != Power.PHOENIX) {
            return;
        }
        double best = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, me.getBoundingBox().inflate(HOST_RANGE),
                e -> e instanceof Mob && e.hasEffect(ModEffects.POSSESSED))) {
            double distance = e.distanceToSqr(me);
            if (distance <= HOST_RANGE * HOST_RANGE && distance < best) {
                best = distance;
                host = e.getDisplayName();
            }
        }
    }

    static void clear() {
        PhoenixFx.clear();
        MindSense.stop();
        MindReadingOverlay.clear();
        host = null;
    }

    private PhoenixView() {
    }
}

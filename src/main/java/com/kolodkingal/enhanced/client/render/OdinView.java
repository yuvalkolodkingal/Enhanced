package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.odin.OdinAbilities;
import com.kolodkingal.enhanced.odin.RavenEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What Odin sees and shows (3.5), for the local player: the HUD lines of his five keys (the spear in flight or missing, the
 * shield's strength, the ravens' count and report, the Odinsleep), and the sight of Huginn and Muninn: every living thing
 * within {@link RavenEntity#SIGHT} blocks of one of his ravens glows through walls for him ({@code EntityGlow} asks {@link
 * #reveals} and {@link #color}), and what the Ravens' Report saw keeps glowing for {@link #REPORT_TICKS} and is listed on
 * the HUD. The client reckons it from the ravens it sees: nothing is sent for it.
 */
public final class OdinView {
    /** How long what the Ravens' Report saw keeps glowing (and is listed), in ticks. */
    public static final int REPORT_TICKS = 600;
    /** The colours of the ravens' sight: players gold, monsters red, the Asgardians blue, animals green, the rest white. */
    public static final int PLAYER = 0xFFD54A;
    public static final int MONSTER = 0xFF4A3A;
    public static final int ASGARDIAN = 0x6FA8FF;
    public static final int ANIMAL = 0x7CFC7C;
    public static final int OTHER = 0xF2F2F2;

    /** Where the local player's ravens are this tick. */
    private static final List<Vec3> RAVENS = new ArrayList<>();
    /** What the Ravens' Report saw: entity id to the game time it stops glowing. */
    private static final Map<Integer, Long> REPORTED = new HashMap<>();
    /** The report's list: what it saw, by kind (monsters, animals) or by name (players), and how many. */
    private static final Map<String, Integer> REPORT = new LinkedHashMap<>();
    private static long reportUntil = Long.MIN_VALUE;
    /** How many living things the ravens see this tick. */
    private static int seen;
    /** When the local player's ravens went out (game time), and whether for the report. */
    private static long ravensSince = Long.MIN_VALUE;
    private static boolean reporting;

    // ---- the HUD ----

    /** The status after the name of an ability: the cooldown unless the ability means something else now. */
    @Nullable
    public static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        return switch (ability) {
            case 1 -> {
                if (data.is(Live.ODIN_THROWN)) {
                    yield Component.translatable("hud.enhanced.odin.thrown").withStyle(ChatFormatting.GOLD);
                }
                if (data.power() == Power.ODIN && !carries(player)) {
                    yield Component.translatable("hud.enhanced.odin.no_gungnir").withStyle(ChatFormatting.RED);
                }
                yield cooldown(data, ability, now);
            }
            case 2 -> {
                Component shield = ShieldStyles.hudLine(data, now, OdinAbilities.shieldStrength(), OdinAbilities.shieldMaxTicks());
                yield shield != null ? shield : cooldown(data, ability, now);
            }
            case 4 -> {
                if (data.is(Live.ODIN_RAVENS)) {
                    int seconds = reporting ? OdinAbilities.REPORT_SECONDS : OdinAbilities.ravensSeconds();
                    long left = Math.max(0, ravensSince + seconds * 20L - now);
                    yield Component.translatable("hud.enhanced.odin.ravens", seen, String.format(Locale.ROOT, "%.0f", Math.ceil(left / 20.0)))
                            .withStyle(ChatFormatting.GOLD);
                }
                if (now < reportUntil && !REPORT.isEmpty()) {
                    yield report();
                }
                yield cooldown(data, ability, now);
            }
            case 5 -> {
                if (data.toggle(Toggle.ODINSLEEP)) {
                    int share = Math.round(100.0F * player.getHealth() / Math.max(1.0F, player.getMaxHealth()));
                    yield Component.translatable("hud.enhanced.odin.sleep", share).withStyle(ChatFormatting.YELLOW);
                }
                yield cooldown(data, ability, now);
            }
            default -> cooldown(data, ability, now);
        };
    }

    /** What the report saw, as the HUD lists it (the first few kinds). */
    private static Component report() {
        MutableComponent line = Component.translatable("hud.enhanced.odin.report").withStyle(ChatFormatting.GOLD);
        int shown = 0;
        for (Map.Entry<String, Integer> e : REPORT.entrySet()) {
            if (shown == 4) {
                line.append(Component.literal(" …").withStyle(ChatFormatting.GRAY));
                break;
            }
            line.append(Component.literal((shown == 0 ? " " : ", ") + e.getKey() + (e.getValue() > 1 ? " ×" + e.getValue() : ""))
                    .withStyle(ChatFormatting.WHITE));
            shown++;
        }
        return line;
    }

    /** Whether he has a Gungnir about him (either hand or his inventory). */
    private static boolean carries(LocalPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (GungnirItem.isGungnir(stack)) {
                return true;
            }
        }
        return GungnirItem.isGungnir(player.getOffhandItem());
    }

    @Nullable
    private static Component cooldown(PowerData data, int ability, long now) {
        return PowerHud.cooldown(data, ability, now);
    }

    // ---- the ravens' sight ----

    /** Once per client tick: where the ravens are, what they see, what the report keeps. */
    static void tick(Minecraft mc) {
        RAVENS.clear();
        seen = 0;
        LocalPlayer self = mc.player;
        ClientLevel level = mc.level;
        if (self == null || level == null) {
            return;
        }
        PowerData data = Powers.get(self);
        long now = level.getGameTime();
        REPORTED.values().removeIf(until -> until <= now);
        if (!data.has(HeroAbility.ODIN_RAVENS)) {
            REPORTED.clear();
            REPORT.clear();
            ravensSince = Long.MIN_VALUE;
            return;
        }
        boolean report = false;
        for (Entity e : level.entitiesForRendering()) {
            if (e instanceof RavenEntity raven && raven.ownedBy(self)) {
                RAVENS.add(raven.position());
                report |= raven.report();
            }
        }
        if (!data.is(Live.ODIN_RAVENS) || RAVENS.isEmpty()) {
            ravensSince = Long.MIN_VALUE;
            return;
        }
        if (ravensSince == Long.MIN_VALUE) {
            ravensSince = now;
            reporting = report;
            if (report) {
                REPORT.clear();
            }
        }
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living) || e == self || e instanceof RavenEntity || !living.isAlive() || !nearRaven(e)) {
                continue;
            }
            seen++;
            if (report && REPORTED.put(e.getId(), now + REPORT_TICKS) == null) {
                String name = e instanceof Player p ? p.getGameProfile().getName() : e.getType().getDescription().getString();
                REPORT.merge(name, 1, Integer::sum);
            }
        }
        if (report) {
            reportUntil = now + REPORT_TICKS;
        }
    }

    private static boolean nearRaven(Entity e) {
        double reach = RavenEntity.SIGHT * RavenEntity.SIGHT;
        for (Vec3 raven : RAVENS) {
            if (e.position().distanceToSqr(raven) <= reach) {
                return true;
            }
        }
        return false;
    }

    /** Whether the entity glows for the local player because his ravens see it (or the report saw it). */
    public static boolean reveals(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof RavenEntity || entity == Minecraft.getInstance().player) {
            return false;
        }
        if (REPORTED.containsKey(entity.getId())) {
            return true;
        }
        return !RAVENS.isEmpty() && nearRaven(entity);
    }

    /** The colour it glows in. */
    public static int color(Entity entity) {
        if (entity instanceof Player) {
            return PLAYER;
        }
        if (entity instanceof Enemy) {
            return MONSTER;
        }
        if (entity.getType().is(ModTags.ASGARDIANS)) {
            return ASGARDIAN;
        }
        return entity instanceof Animal ? ANIMAL : OTHER;
    }

    /** The client left the world: nothing seen is kept. */
    static void clear() {
        RAVENS.clear();
        REPORTED.clear();
        REPORT.clear();
        reportUntil = Long.MIN_VALUE;
        ravensSince = Long.MIN_VALUE;
        seen = 0;
    }

    private OdinView() {
    }
}

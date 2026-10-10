package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.LokiForm;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/** Top-left panel: your hero, each ability with its key, and its current state (cooldown, on/off, ammo). */
public class PowerHud implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        PowerData data = Powers.get(player);
        Power power = data.power();
        if (!power.hasPowers()) {
            return;
        }
        Font font = mc.font;
        long now = mc.level.getGameTime();
        Variant look = Variants.current(data);
        // Heroes with several looks show the current one, and the key that changes it, after their name.
        int titleColor = Variants.hasChoice(power) ? look.color() : power.titleColor();
        MutableComponent title = power.displayName().copy().withStyle(s -> s.withColor(titleColor).withBold(true));
        if (Variants.hasChoice(power)) {
            title.append(Component.literal(" ").withStyle(s -> s.withBold(false)))
                    .append(Component.literal("[").append(KeyBindings.CHANGE_LOOK.getTranslatedKeyMessage()).append("] ")
                            .withStyle(s -> s.withBold(false).withColor(ChatFormatting.YELLOW)))
                    .append(look.displayName().copy().withStyle(s -> s.withBold(false).withColor(ChatFormatting.GRAY)));
        }

        Component[] lines = new Component[PowerData.ABILITIES];
        int width = font.width(title) + 4;
        for (int i = 1; i <= PowerData.ABILITIES; i++) {
            MutableComponent line = Component.literal("[").append(KeyBindings.ability(i).getTranslatedKeyMessage()).append("] ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable(power.abilityKey(i)).withStyle(ChatFormatting.WHITE));
            Component status = status(player, data, i, now);
            if (status != null) {
                line.append(Component.literal("  ")).append(status);
            }
            lines[i - 1] = line;
            width = Math.max(width, font.width(line));
        }

        int x = 4;
        int y = 4;
        int height = 12 + PowerData.ABILITIES * 10;
        graphics.fill(x - 2, y - 2, x + width + 4, y + height, 0x70000000);
        graphics.drawString(font, title, x, y, 0xFFFFFFFF, true);
        y += 12;
        for (Component line : lines) {
            graphics.drawString(font, line, x, y, 0xFFFFFFFF, true);
            y += 10;
        }
    }

    private static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        long cooldown = data.cooldownUntil(ability) - now;
        if (cooldown > 0) {
            return Component.literal(String.format(Locale.ROOT, "%.1fs", cooldown / 20.0)).withStyle(ChatFormatting.GRAY);
        }
        boolean holding = data.heldEntityId() != -1;
        return switch (data.power()) {
            case MAGNETO -> switch (ability) {
                case 1 -> holding ? active("hud.enhanced.holding") : null;
                case 2 -> Component.translatable("hud.enhanced.shards", MagnetoAbilities.countAmmo(player, data))
                        .withStyle(data.shrapnelActive() ? ChatFormatting.AQUA : ChatFormatting.GRAY);
                case 4 -> onOff(data.magneticShield());
                case 5 -> onOff(data.flying());
                default -> null;
            };
            case SCARLET_WITCH -> switch (ability) {
                case 1 -> data.beamActive() ? active("hud.enhanced.active") : null;
                case 2 -> onOff(data.flying());
                case 3 -> holding ? active("hud.enhanced.holding") : null;
                case 4 -> shield(data, now);
                case 5 -> player.level().getData(ModAttachments.HEXES).byOwner(player.getUUID())
                        .map(hex -> (Component) Component.translatable("hud.enhanced.hex_radius", hex.radius()).withStyle(ChatFormatting.RED))
                        .orElse(null);
                default -> null;
            };
            case QUICKSILVER -> switch (ability) {
                case 1 -> quicksilverMode(data);
                case 4 -> onOff(data.wallRun());
                case 5 -> onOff(data.walkman());
                default -> null;
            };
            case VISION -> switch (ability) {
                case 1 -> data.beamActive() ? active("hud.enhanced.active") : null;
                case 2 -> onOff(data.flying());
                case 3 -> onOff(data.visionPhased());
                case 4 -> onOff(data.dense());
                case 5 -> onOff(data.disguise());
                default -> null;
            };
            case BILLY -> billy(player, data, ability);
            case TOMMY -> tommy(player, data, ability);
            case LOKI -> loki(player, data, ability);
            case THOR -> thor(player, data, ability);
            case NONE -> null;
        };
    }

    /**
     * The Chaos Shield: "forming" while it grows, then what is left of its strength (green, then yellow, then red as it
     * wears down) and the seconds it can still be held. Once it has dropped, its cooldown shows instead (see
     * {@link #status}).
     */
    private static Component shield(PowerData data, long now) {
        if (!data.chaosShieldActive()) {
            return null;
        }
        if (!data.chaosShieldUp(now)) {
            return Component.translatable("hud.enhanced.shield_forming").withStyle(ChatFormatting.GOLD);
        }
        float max = WandaAbilities.maxShieldStrength();
        float left = data.shieldStrength();
        ChatFormatting color = left > max * 0.5F ? ChatFormatting.GREEN : left > max * 0.25F ? ChatFormatting.YELLOW : ChatFormatting.RED;
        double seconds = Math.max(0, data.shieldFormedAt() + WandaAbilities.maxShieldTicks() - now) / 20.0;
        return Component.translatable("hud.enhanced.shield_strength", (int) Math.ceil(left), (int) Math.ceil(max)).withStyle(color)
                .append(Component.literal(String.format(Locale.ROOT, "  %.1fs", seconds)).withStyle(ChatFormatting.GRAY));
    }

    /** Which of Quicksilver's modes is on ("Time" or "Speed", or OFF), and "running" while he runs in it. */
    private static Component quicksilverMode(PowerData data) {
        if (!data.quicksilverTime() && !data.superSpeed()) {
            return onOff(false);
        }
        MutableComponent mode = Component.translatable(data.quicksilverTime() ? "hud.enhanced.time" : "hud.enhanced.speed")
                .withStyle(ChatFormatting.GREEN);
        if (data.is(Live.SPEEDING)) {
            mode.append(Component.literal(" ")).append(active("hud.enhanced.running"));
        }
        return mode;
    }

    private static Component billy(LocalPlayer player, PowerData data, int ability) {
        return switch (ability) {
            case 2 -> onOff(data.flying());
            case 3 -> data.heldEntityId() != -1 ? active("hud.enhanced.holding") : null;
            case 5 -> onOff(data.toggle(Toggle.THE_NOISE));
            default -> null;
        };
    }

    private static Component tommy(LocalPlayer player, PowerData data, int ability) {
        return switch (ability) {
            case 1 -> onOff(data.toggle(Toggle.SUPER_SPEED));
            default -> null;
        };
    }

    private static Component loki(LocalPlayer player, PowerData data, int ability) {
        return switch (ability) {
            case 2 -> data.is(Live.HIDDEN) ? active("hud.enhanced.active") : null;
            case 3 -> LokiForm.of(data).map(form -> (Component) form.displayName().copy().withStyle(ChatFormatting.GREEN)).orElse(null);
            case 5 -> onOff(data.toggle(Toggle.FROST_GIANT));
            default -> null;
        };
    }

    private static Component thor(LocalPlayer player, PowerData data, int ability) {
        return switch (ability) {
            case 1 -> ThorAbilities.holdsWeapon(player) ? Component.translatable("hud.enhanced.thor_weapon.in_hand").withStyle(ChatFormatting.AQUA)
                    : data.hammerEntityId() != -1 ? Component.translatable(player.level().getEntity(data.hammerEntityId()) instanceof MjolnirFragmentEntity
                    ? "hud.enhanced.thor_weapon.shattered" : "hud.enhanced.thor_weapon.flying").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("hud.enhanced.thor_weapon.away").withStyle(ChatFormatting.GRAY);
            case 2 -> !ThorAbilities.holdsWeapon(player) && data.hammerEntityId() == -1
                    ? Component.translatable("hud.enhanced.thor_weapon.needed").withStyle(ChatFormatting.DARK_GRAY) : onOff(data.flying());
            case 3 -> data.is(Live.CALLING_LIGHTNING) ? active("hud.enhanced.active")
                    : ThorAbilities.holdsWeapon(player) ? Component.translatable("hud.enhanced.thor_beam.boosted").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("hud.enhanced.thor_beam.stronger").withStyle(ChatFormatting.DARK_GRAY);
            case 4 -> onOff(data.toggle(Toggle.GOD_OF_THUNDER));
            case 5 -> data.is(Live.BIFROST) ? active("hud.enhanced.active")
                    : !ThorAbilities.holdsWeapon(player) ? Component.translatable("hud.enhanced.thor_weapon.needed").withStyle(ChatFormatting.DARK_GRAY)
                    : null;
            default -> null;
        };
    }

    private static Component active(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.AQUA);
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }
}

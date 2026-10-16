package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.client.build.BuildClient;
import com.kolodkingal.enhanced.client.build.BuildKeys;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.ShieldStyles;
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

/**
 * Top-left panel: your hero, each ability with its key, and its current state (cooldown, on/off, ammo). The Scarlet Witch
 * and Billy have a sixth line, Build Magic's key and whether build mode is on (D-B 6.6).
 */
public class PowerHud implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        // (Nor on the death screen: the powers wait for the respawn, and the panel would sit over "You Died!".)
        if (player == null || mc.level == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || player.isDeadOrDying()) {
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

        Component[] lines = new Component[PowerData.ABILITIES + extraLines(power)];
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
        if (extraLines(power) > 0) {
            Component line = buildLine(player, power, now);
            lines[PowerData.ABILITIES] = line;
            width = Math.max(width, font.width(line));
        }

        int x = 4;
        // Under the boss bars, when a fight's bars and their names would run into the panel.
        int y = 4 + BossBarOverride.clearance(x + width + 4);
        int height = 12 + lines.length * 10;
        graphics.fill(x - 2, y - 2, x + width + 4, y + height, 0x70000000);
        graphics.drawString(font, title, x, y, 0xFFFFFFFF, true);
        y += 12;
        for (Component line : lines) {
            graphics.drawString(font, line, x, y, 0xFFFFFFFF, true);
            y += 10;
        }
    }

    private static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        // The heroes of 3.0 say for themselves (by default: the cooldown, as below).
        HeroClient hero = HeroClients.of(data.power());
        if (hero != null) {
            return hero.hudStatus(player, data, ability, now);
        }
        // (Magneto's R says which of its recharges runs: the Magnetic Pull's, never a bare cooldown.)
        Component cooldown = data.power() == Power.MAGNETO && ability == 1 ? null : cooldown(data, ability, now);
        if (cooldown != null) {
            return cooldown;
        }
        boolean holding = data.heldEntityId() != -1;
        return switch (data.power()) {
            case MAGNETO -> switch (ability) {
                case 1 -> holding ? active("hud.enhanced.holding") : pullCooldown(data, now);
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
                case 4 -> shield(data, now, WandaAbilities.maxShieldStrength(), WandaAbilities.maxShieldTicks());
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
            case PHOENIX, SPIDER_MAN, THANOS, ULTRON, APOCALYPSE, NONE -> null;
        };
    }

    /** How many lines the panel has after the five abilities: Build Magic's for the Scarlet Witch and Billy, else none. */
    public static int extraLines(Power power) {
        return BuildClient.isBuilder(power) ? 1 : 0;
    }

    /**
     * The sixth line of the Scarlet Witch and Billy (D-B 6.6): "[Y] Build Magic" with the key's current name and ON or OFF
     * as flight shows it; for Billy then his magic meter as it refills ("✦ 372/512", the top his rank's).
     */
    private static Component buildLine(LocalPlayer player, Power power, long now) {
        MutableComponent line = Component.literal("[").append(BuildKeys.MAGIC.getTranslatedKeyMessage()).append("] ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("hud.enhanced.build").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  ")).append(onOff(BuildClient.buildMode()));
        if (power == Power.BILLY) {
            BuildData build = player.getExistingData(ModAttachments.BUILD_DATA).orElse(BuildData.EMPTY);
            int meter = (int) Math.floor(BillyRanks.meterNow(build, now));
            line.append(Component.literal("  ")).append(Component.translatable("hud.enhanced.build.meter", meter,
                    BillyRanks.meter(BillyRanks.rank(build))).withStyle(s -> s.withColor(Power.BILLY.color)));
        }
        return line;
    }

    /**
     * Magneto's R while he holds nothing (D-M 8.3): "pull 4.9s" in grey while the Magnetic Pull recharges (the cooldown of
     * ability 1), else nothing.
     */
    private static Component pullCooldown(PowerData data, long now) {
        Component left = cooldown(data, 1, now);
        return left == null ? null : Component.translatable("hud.enhanced.pull_cooldown", left).withStyle(ChatFormatting.GRAY);
    }

    /**
     * The ability's cooldown as the HUD shows it (grey seconds), or null when it is ready. The unit comes from the language
     * ({@code hud.enhanced.seconds}: "4.9s"; in Hebrew the number and then the Hebrew abbreviation): a Latin "s" after the
     * number on a Hebrew line was drawn apart from it, at the far end of the line.
     */
    public static Component cooldown(PowerData data, int ability, long now) {
        long left = data.cooldownUntil(ability) - now;
        return left > 0 ? Component.translatable("hud.enhanced.seconds", String.format(Locale.ROOT, "%.1f", left / 20.0))
                .withStyle(ChatFormatting.GRAY) : null;
    }

    /**
     * A hero's energy shield (the Chaos Shield; Apocalypse's has its own numbers): "forming" while it grows, then what
     * is left of its strength (green, then yellow, then red as it wears down) and the seconds it can still be held.
     * Once it has dropped, its cooldown shows instead (see {@link #status}).
     *
     * @param max      the strength a fresh shield of this hero has
     * @param maxTicks how long a complete shield of this hero can be held
     */
    public static Component shield(PowerData data, long now, float max, int maxTicks) {
        return ShieldStyles.hudLine(data, now, max, maxTicks);
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
            case 1 -> BillyAbilities.beamEnabled() ? hexBeam(player, data) : null;
            case 2 -> onOff(data.flying());
            case 3 -> data.heldEntityId() != -1 ? active("hud.enhanced.holding") : null;
            case 5 -> onOff(data.toggle(Toggle.THE_NOISE));
            default -> null;
        };
    }

    /**
     * The Hex Beam's line (3.2): what it does at his rank now, in grey, and green while it fires ("6.0/s, 20 blocks": damage a
     * second and range). The numbers grow with his Build Magic rank.
     */
    private static Component hexBeam(LocalPlayer player, PowerData data) {
        BuildData build = player.getExistingData(ModAttachments.BUILD_DATA).orElse(BuildData.EMPTY);
        String perSecond = String.format(Locale.ROOT, "%.1f", BillyAbilities.beamDamagePerSecond(build));
        int range = (int) Math.round(BillyAbilities.beamRange(build));
        boolean firing = data.is(Live.BEAMING);
        return Component.translatable(firing ? "hud.enhanced.billy_beam_firing" : "hud.enhanced.billy_beam", perSecond, range)
                .withStyle(firing ? ChatFormatting.GREEN : ChatFormatting.GRAY);
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

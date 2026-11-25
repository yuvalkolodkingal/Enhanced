package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.hud.PowerPanels;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.BillyBarrier;
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
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Top-left panel: your hero, each ability with its key, and its current state (cooldown, on/off, ammo). The Scarlet Witch
 * and Billy have a sixth line, Build Magic's key and whether build mode is on (D-B 6.6).
 */
public class PowerHud implements LayeredDraw.Layer {
    /**
     * The width the panel is drawn at, and until when it holds (3.3): it widens at once but narrows only after a
     * second,
     * so a cooldown counting down ("10.0s", "9.9s") does not make its right edge twitch every tick.
     */
    private static int shownWidth;
    private static long shownUntil;

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
        if (!power.hasPowers() || power == Power.CUSTOM) {
            return; // (3.3: a custom hero's panel is drawn in his own colours by CustomHeroHud)
        }
        Font font = mc.font;
        long now = mc.level.getGameTime();
        if (!Coexist.active()) {
            // A bender of Enlightened too, and switched to the bending: one line under its panel says how to come back.
            int[] other = Coexist.otherHudBottom();
            Component line = Component.literal("[").append(KeyBindings.SWITCH_POWERS.getTranslatedKeyMessage()).append("] ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(power.displayName().copy().withStyle(s -> s.withColor(power.titleColor())));
            int w = font.width(line) + 6;
            int ly = other == null ? 4 : other[0] + 2;
            int lx = other != null && other[1] == 1 ? graphics.guiWidth() - 4 - w : 4;
            graphics.fill(lx, ly, lx + w, ly + 11, 0x70000000);
            graphics.drawString(font, line, lx + 3, ly + 2, 0xFFFFFFFF, true);
            return;
        }
        PowerPanels.Style style = EnhancedClientConfig.hudStyle();
        if (style != PowerPanels.Style.CLASSIC) {
            PowerPanels.Model model = model(mc);
            if (model != null) {
                boolean right = EnhancedClientConfig.hudRight();
                int top = 4 + Math.max(BossBarOverride.clearance(4 + 150), right ? effectsClearance(player) : 0);
                int height = PowerPanels.draw(style, graphics, font, model, graphics.guiWidth(), top, right,
                        EnhancedClientConfig.reducedMotion());
                Coexist.hudBottom(top + height, right);
            }
            return;
        }
        Variant look = Variants.current(data);
        // Heroes with several looks show the current one, and the key that changes it, after their name.
        // (3.18) Cosmic Spider-Man's panel is in the Enigma Force's colours: a white-blue name, keys and frame
        // (3.18.1) Billy the Demiurge's panel too: his name in the Demiurge's blue, the magic type on H, a starry frame
        boolean demiurge = com.kolodkingal.enhanced.ability.Demiurge.is(data);
        boolean cosmic = com.kolodkingal.enhanced.ability.CosmicSpider.cosmicKeys(data) || demiurge;
        int aura = demiurge ? com.kolodkingal.enhanced.ability.Demiurge.AURA : com.kolodkingal.enhanced.ability.CosmicSpider.AURA;
        boolean ultimateWanda = com.kolodkingal.enhanced.ability.UltimateWanda.is(data); // (3.19: her name in her red, her menu on H)
        int titleColor = ultimateWanda ? com.kolodkingal.enhanced.ability.UltimateWanda.RED
                : cosmic ? aura : Variants.hasChoice(power) ? look.color() : power.titleColor();
        MutableComponent title = (ultimateWanda ? Component.translatable("hud.enhanced.ultimate_wanda.title")
                : demiurge ? Component.translatable("hud.enhanced.demiurge.title")
                : cosmic ? Component.translatable("hud.enhanced.cosmic_spider.title")
                : com.kolodkingal.enhanced.power.Characters.name(data).copy()).withStyle(s -> s.withColor(titleColor).withBold(true));
        if (ultimateWanda) {
            title.append(Component.literal(" ").withStyle(s -> s.withBold(false)))
                    .append(Component.literal("[").append(KeyBindings.CHANGE_LOOK.getTranslatedKeyMessage()).append("] ")
                            .withStyle(s -> s.withBold(false).withColor(com.kolodkingal.enhanced.ability.UltimateWanda.GLOW)))
                    .append(Component.translatable("hud.enhanced.ultimate_wanda.menu", com.kolodkingal.enhanced.ability.UltimateWanda.menuName(data.ironCharge()))
                            .withStyle(s -> s.withBold(false).withColor(0xFFE0E6)));
        } else if (demiurge) {
            title.append(Component.literal(" ").withStyle(s -> s.withBold(false)))
                    .append(Component.literal("[").append(KeyBindings.CHANGE_LOOK.getTranslatedKeyMessage()).append("] ")
                            .withStyle(s -> s.withBold(false).withColor(aura)))
                    .append(Component.translatable("hud.enhanced.demiurge.magic", com.kolodkingal.enhanced.ability.Demiurge.typeName(
                            com.kolodkingal.enhanced.ability.Demiurge.type(data))).withStyle(s -> s.withBold(false).withColor(0xE6F8FF)));
        } else if (Variants.hasChoice(power)) {
            title.append(Component.literal(" ").withStyle(s -> s.withBold(false)))
                    .append(Component.literal("[").append(KeyBindings.CHANGE_LOOK.getTranslatedKeyMessage()).append("] ")
                            .withStyle(s -> s.withBold(false).withColor(ChatFormatting.YELLOW)))
                    .append((cosmic ? Component.translatable(com.kolodkingal.enhanced.ability.CosmicSpider.universe(data)
                            ? "hud.enhanced.cosmic_spider.look.universe" : "hud.enhanced.cosmic_spider.look.starfield")
                            : look.displayName().copy()).withStyle(s -> s.withBold(false).withColor(cosmic ? 0xD8EEFF : 0xAAAAAA)));
        }

        Power builder = BuildClient.hero(player); // (3.4: whose Build Magic he has, the line's owner)
        // (3.6: a hero whose Y is not Build Magic, Doctor Strange's Eye, has a sixth line of his own: yLine.)
        boolean yLine = extraLines(builder) == 0 && com.kolodkingal.enhanced.custom.HeroKeys.yRow(data) != null;
        Component[] lines = new Component[PowerData.ABILITIES + extraLines(builder) + (yLine ? 1 : 0)];
        int width = font.width(title) + 4;
        for (int i = 1; i <= PowerData.ABILITIES; i++) {
            MutableComponent line = Component.literal("[").append(KeyBindings.ability(i).getTranslatedKeyMessage()).append("] ")
                    .withStyle(cosmic ? net.minecraft.network.chat.Style.EMPTY.withColor(aura)
                            : net.minecraft.network.chat.Style.EMPTY.withColor(ChatFormatting.YELLOW))
                    .append(Component.translatable(abilityKey(player, data, power, i)).withStyle(cosmic
                            ? net.minecraft.network.chat.Style.EMPTY.withColor(0xEAF6FF) : net.minecraft.network.chat.Style.EMPTY.withColor(ChatFormatting.WHITE)));
            Component status = status(player, data, i, now);
            if (status != null) {
                line.append(Component.literal("  ")).append(status);
            }
            lines[i - 1] = line;
            width = Math.max(width, font.width(line));
        }
        if (extraLines(builder) > 0) {
            Component line = buildLine(player, builder, now);
            lines[PowerData.ABILITIES] = line;
            width = Math.max(width, font.width(line));
        }
        if (yLine) {
            MutableComponent line = Component.literal("[").append(BuildKeys.MAGIC.getTranslatedKeyMessage()).append("] ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable(power.abilityKey(com.kolodkingal.enhanced.custom.HeroKeys.Y))
                            .withStyle(ChatFormatting.WHITE));
            Component status = status(player, data, com.kolodkingal.enhanced.custom.HeroKeys.Y, now);
            if (status != null) {
                line.append(Component.literal("  ")).append(status);
            }
            lines[PowerData.ABILITIES] = line;
            width = Math.max(width, font.width(line));
        }

        if (width >= shownWidth || now > shownUntil || now < shownUntil - 20) {
            if (width != shownWidth) {
                shownUntil = now + 20;
            }
            shownWidth = width;
        }
        width = shownWidth;
        int x = 4;
        // Under the boss bars, when a fight's bars and their names would run into the panel.
        int y = 4 + BossBarOverride.clearance(x + width + 4);
        int height = 12 + lines.length * 10;
        Coexist.hudBottom(y + height, false);
        graphics.fill(x - 2, y - 2, x + width + 4, y + height, cosmic ? 0x80101A3A : 0x70000000);
        if (cosmic) {
            // a thin glowing frame of the Enigma Force round the panel
            int frame = 0xC0000000 | aura;
            graphics.fill(x - 2, y - 2, x + width + 4, y - 1, frame);
            graphics.fill(x - 2, y + height - 1, x + width + 4, y + height, frame);
            graphics.fill(x - 2, y - 2, x - 1, y + height, frame);
            graphics.fill(x + width + 3, y - 2, x + width + 4, y + height, frame);
            if (demiurge) {
                // (small stars twinkling on the Demiurge's frame)
                long t = now;
                for (int k = 0; k < 6; k++) {
                    int sx = x + (int) ((k * 37L + t / 3) % (width + 4)) - 1;
                    int sy = k % 2 == 0 ? y - 2 : y + height - 1;
                    graphics.fill(sx, sy, sx + 1, sy + 1, 0xFFFFFFFF);
                }
            }
        }
        graphics.drawString(font, title, x, y, 0xFFFFFFFF, true);
        y += 12;
        for (Component line : lines) {
            graphics.drawString(font, line, x, y, 0xFFFFFFFF, true);
            y += 10;
        }
    }

    /** How far down a panel in the top-right corner starts so as not to cover the status effect icons there. */
    public static int effectsClearance(LocalPlayer player) {
        boolean good = false;
        boolean bad = false;
        for (net.minecraft.world.effect.MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.showIcon()) {
                if (effect.getEffect().value().isBeneficial()) {
                    good = true;
                } else {
                    bad = true;
                }
            }
        }
        return bad ? 52 : good ? 26 : 0;
    }

    /** What the new panel styles and the wheel draw: the classic panel's content, as data (null: no hero to show). */
    @org.jetbrains.annotations.Nullable
    public static PowerPanels.Model model(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return null;
        }
        PowerData data = Powers.get(player);
        Power power = data.power();
        if (!power.hasPowers() || power == Power.CUSTOM) {
            return null;
        }
        long now = mc.level.getGameTime();
        Variant look = Variants.current(data);
        PowerPanels.Model model = new PowerPanels.Model();
        boolean cosmicPanel = com.kolodkingal.enhanced.ability.CosmicSpider.cosmicKeys(data);
        boolean demiurgePanel = com.kolodkingal.enhanced.ability.Demiurge.is(data); // (3.18.1)
        model.color = demiurgePanel ? com.kolodkingal.enhanced.ability.Demiurge.AURA : cosmicPanel ? com.kolodkingal.enhanced.ability.CosmicSpider.AURA
                : Variants.hasChoice(power) ? look.color() : power.titleColor();
        boolean ultimatePanel = com.kolodkingal.enhanced.ability.UltimateWanda.is(data); // (3.19)
        if (ultimatePanel) {
            model.color = com.kolodkingal.enhanced.ability.UltimateWanda.RED;
        }
        model.title = ultimatePanel ? Component.translatable("hud.enhanced.ultimate_wanda.title")
                : demiurgePanel ? Component.translatable("hud.enhanced.demiurge.title")
                : cosmicPanel ? Component.translatable("hud.enhanced.cosmic_spider.title") : com.kolodkingal.enhanced.power.Characters.name(data);
        model.identity = power.getSerializedName() + "/" + look.displayName().getString();
        model.sneaking = false;
        for (int i = 1; i <= PowerData.ABILITIES; i++) {
            model.rows.add(new PowerPanels.Row(KeyBindings.ability(i).getTranslatedKeyMessage(),
                    panelSlot(Component.translatable(abilityKey(player, data, power, i)), status(player, data, i, now), data, i, now)
                            .withIcon(symbol(com.kolodkingal.enhanced.ability.Demiurge.keysOf(data).abilityKey(i))), null));
        }
        Power builder = BuildClient.hero(player);
        if (extraLines(builder) > 0) {
            model.notes.add(buildLine(player, builder, now));
        } else if (com.kolodkingal.enhanced.custom.HeroKeys.yRow(data) != null) {
            int y = com.kolodkingal.enhanced.custom.HeroKeys.Y;
            model.rows.add(new PowerPanels.Row(BuildKeys.MAGIC.getTranslatedKeyMessage(),
                    panelSlot(Component.translatable(power.abilityKey(y)), status(player, data, y, now), data, y, now), null));
        }
        if (ultimatePanel) {
            model.notes.add(Component.literal("[").append(KeyBindings.CHANGE_LOOK.getTranslatedKeyMessage()).append("] ")
                    .withStyle(s -> s.withColor(com.kolodkingal.enhanced.ability.UltimateWanda.GLOW))
                    .append(Component.translatable("hud.enhanced.ultimate_wanda.menu", com.kolodkingal.enhanced.ability.UltimateWanda.menuName(data.ironCharge()))
                            .withStyle(ChatFormatting.WHITE)));
        } else if (demiurgePanel) {
            model.notes.add(Component.literal("[").append(KeyBindings.CHANGE_LOOK.getTranslatedKeyMessage()).append("] ")
                    .withStyle(s -> s.withColor(com.kolodkingal.enhanced.ability.Demiurge.AURA))
                    .append(Component.translatable("hud.enhanced.demiurge.magic", com.kolodkingal.enhanced.ability.Demiurge.typeName(
                            com.kolodkingal.enhanced.ability.Demiurge.type(data))).withStyle(ChatFormatting.WHITE)));
        } else if (Variants.hasChoice(power)) {
            model.notes.add(Component.literal("[").append(KeyBindings.CHANGE_LOOK.getTranslatedKeyMessage()).append("] ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(look.displayName().copy().withStyle(ChatFormatting.GRAY)));
        }
        return model;
    }

    /**
     * The symbol of an ability (3.10): textures/gui/ability/&lt;hero&gt;_&lt;n&gt;.png, drawn by tools/symbols.py from the motif
     * tools/symbols.json gives its translation key ("ability.enhanced.thor.3" is thor_3).
     */
    public static PowerPanels.Icon symbol(String abilityKey) {
        // (3.18.1: the path is the witches' icons' too: WitchIcons.texture)
        net.minecraft.resources.ResourceLocation texture = com.kolodkingal.enhanced.Enhanced.id(
                com.kolodkingal.enhanced.spellbook.WitchIcons.texture(abilityKey));
        return (g, x, y, size) -> {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            g.blit(texture, x, y, size, size, 0, 0, 32, 32, 32, 32);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        };
    }

    /** An ability for the new styles: on when the classic panel writes its state in green or aqua (ON, active, holding). */
    private static PowerPanels.Slot panelSlot(Component name, @org.jetbrains.annotations.Nullable Component status, PowerData data,
                                               int ability, long now) {
        long left = data.cooldownUntil(ability) - now;
        net.minecraft.network.chat.TextColor color = status == null ? null : status.getStyle().getColor();
        boolean on = color != null && (color.equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.GREEN))
                || color.equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.AQUA)));
        return new PowerPanels.Slot(name, status, left > 0 ? left / 20.0F : 0.0F, on, -1, 0, null);
    }

    /** The name of a key's ability: the hero's, or (3.16) Solar Jewel Vision's own on R and N. */
    private static String abilityKey(LocalPlayer player, PowerData data, Power power, int ability) {
        // (3.18.1) Billy the Demiurge: the magic type's names on its keys; his own N says how to leave, a plain Billy with
        // Ultimate how to become him (without Ultimate nothing of the Demiurge is shown)
        if (com.kolodkingal.enhanced.ability.Demiurge.is(data)) {
            Power kit = com.kolodkingal.enhanced.ability.Demiurge.keysOf(data);
            if (kit != Power.BILLY) {
                return kit.abilityKey(ability);
            }
            if (ability == 5) {
                return "ability.enhanced.billy.demiurge.5";
            }
        } else if (power == Power.BILLY && ability == 5 && com.kolodkingal.enhanced.ability.Demiurge.allowedHolder(player)) {
            return "ability.enhanced.billy.demiurge_ready.5";
        }
        // (3.19) Ultimate Wanda: her own R, V and B in her Ultimate menu; G says how to leave the Ultimate, a plain Scarlet Witch
        // with Ultimate how to become her
        if (com.kolodkingal.enhanced.ability.UltimateWanda.is(data)) {
            if (com.kolodkingal.enhanced.ability.UltimateWanda.ultimateMenu(data) && (ability == 1 || ability == 3 || ability == 4 || ability == 5)) {
                return "ability.enhanced.scarlet_witch.ultimate." + ability;
            }
            if (ability == 2) {
                return "ability.enhanced.scarlet_witch.ultimate.2";
            }
            if (com.kolodkingal.enhanced.ability.UltimateWanda.multiverseMenu(data) && (ability == 1 || ability == 3)) {
                return "ability.enhanced.scarlet_witch.multiverse." + ability;
            }
            if (ability == 1 && !com.kolodkingal.enhanced.ability.UltimateWanda.ultimateMenu(data)
                    && !com.kolodkingal.enhanced.ability.UltimateWanda.multiverseMenu(data)) {
                return "ability.enhanced.scarlet_witch.ultimate_beam.1";
            }
        } else if (power == Power.SCARLET_WITCH && ability == 2 && com.kolodkingal.enhanced.ability.UltimateWanda.allowedHolder(player)) {
            return "ability.enhanced.scarlet_witch.ultimate_ready.2";
        }
        if ((ability == 1 || ability == 5) && com.kolodkingal.enhanced.power.VisionQuest.solar(data)) {
            return "ability.enhanced.vision.solar." + ability;
        }
        // (3.19) Red Skull's own R, G and V; his and Captain Hydra's "Hail Hydra" on B
        if (power == Power.CAPTAIN_AMERICA && com.kolodkingal.enhanced.power.Characters.hydra(data)) {
            if (com.kolodkingal.enhanced.captain.RedSkull.is(data) && ability >= 1 && ability <= 3) {
                return "ability.enhanced.captain_america.red_skull." + ability;
            }
            if (ability == 4) {
                return "ability.enhanced.captain_america.hydra.4";
            }
        }
        // (3.18) Cosmic Spider-Man's own R, G (webs, not swings: he flies), B and N
        if ((ability == 1 || ability == 2 || ability == 4 || ability == 5) && com.kolodkingal.enhanced.ability.CosmicSpider.cosmicKeys(data)) {
            return "ability.enhanced.spider_man.cosmic." + ability;
        }
        return power.abilityKey(ability);
    }

    private static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        // (3.17) a charged beam shows its charge on its key's line
        if (data.is(com.kolodkingal.enhanced.power.Live.BEAM_CHARGING) && ability == com.kolodkingal.enhanced.client.render.ChargedBeamFx.hudKey(data.power())) {
            return com.kolodkingal.enhanced.client.render.ChargedBeamFx.hudLine(player, data, now);
        }
        // (3.16) Solar Jewel Vision's R shows the light stored in his jewel
        if (ability == 1 && com.kolodkingal.enhanced.power.VisionQuest.solar(data) && com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded()) {
            int percent = Math.round(100.0F * data.ironCharge()
                    / Math.max(1, com.kolodkingal.enhanced.config.SolarJewelConfig.MAX_CHARGE.get()));
            return Component.translatable("hud.enhanced.solar.charge", percent)
                    .withStyle(percent >= 40 ? ChatFormatting.GOLD : ChatFormatting.RED);
        }
        // The heroes of 3.0 say for themselves (by default: the cooldown, as below).
        // (3.18.1: the Demiurge's keys say what the magic type's would)
        Power shown = com.kolodkingal.enhanced.ability.Demiurge.keysOf(data);
        HeroClient hero = HeroClients.of(shown);
        if (hero != null) {
            return hero.hudStatus(player, data, ability, now);
        }
        // (Magneto's R says which of its recharges runs: the Magnetic Pull's, never a bare cooldown.)
        Component cooldown = shown == Power.MAGNETO && ability == 1 ? null : cooldown(data, ability, now);
        if (cooldown != null) {
            return cooldown;
        }
        boolean holding = data.heldEntityId() != -1;
        return switch (shown) {
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
                case 5 -> ModAttachments.HEXES.get(player.level()).byOwner(player.getUUID())
                        .map(hex -> (Component) Component.translatable("hud.enhanced.hex_radius", hex.radius()).withStyle(ChatFormatting.RED))
                        .orElse(null);
                default -> null;
            };
            case QUICKSILVER -> switch (ability) {
                case 1 -> slowTime(data.quicksilverTime(), data.walkman());
                case 2 -> running(data, data.superSpeed());
                case 4 -> onOff(data.wallRun());
                default -> null;
            };
            case VISION -> switch (ability) {
                case 1 -> data.beamActive() ? active("hud.enhanced.active") : null;
                case 2 -> onOff(data.flying());
                case 3 -> onOff(data.visionPhased());
                case 4 -> null;
                case 5 -> onOff(data.disguise());
                default -> null;
            };
            case BILLY -> billy(player, data, ability);
            case TOMMY -> tommy(player, data, ability);
            case LOKI -> loki(player, data, ability);
            case THOR -> thor(player, data, ability);
            case PHOENIX, SPIDER_MAN, THANOS, ULTRON, APOCALYPSE, AGATHA, CYCLOPS, PROFESSOR_X, CUSTOM, HEIMDALL, ODIN,
                    HELA, DOCTOR_STRANGE, HULK, WOLVERINE, DEADPOOL, CAPTAIN_AMERICA, RED_HULK, IRON_MAN, FALCON, LOVE,
                    JIM_HAMMOND, NONE -> null;
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
            BuildData build = ModAttachments.BUILD_DATA.getExisting(player).orElse(BuildData.EMPTY);
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
    /** A speedster's slow time (3.3): ON or OFF, and Quicksilver's Walkman note while his song can play. */
    private static Component slowTime(boolean on, boolean walkman) {
        MutableComponent line = onOff(on).copy();
        if (on && walkman) {
            line.append(Component.literal(" ")).append(Component.translatable("hud.enhanced.walkman_note").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return line;
    }

    /** A speedster's running (3.3): ON or OFF, "running" while he runs, and the wall or the ceiling he runs on. */
    private static Component running(PowerData data, boolean on) {
        MutableComponent line = onOff(on).copy();
        if (data.is(Live.SURFACE_WALL) || data.is(Live.SURFACE_CEILING)) {
            line.append(Component.literal(" ")).append(active(data.is(Live.SURFACE_CEILING) ? "hud.enhanced.on_ceiling" : "hud.enhanced.on_wall"));
        } else if (data.is(Live.SPEEDING)) {
            line.append(Component.literal(" ")).append(active("hud.enhanced.running"));
        }
        return line;
    }

    private static Component billy(LocalPlayer player, PowerData data, int ability) {
        return switch (ability) {
            case 1 -> BillyAbilities.beamEnabled() ? hexBeam(player, data) : null;
            case 4 -> BillyBarrier.enabled() && data.chaosShieldActive() ? hexBarrier(player, data) : null;
            case 2 -> onOff(data.flying());
            case 3 -> data.heldEntityId() != -1 ? active("hud.enhanced.holding") : null;
            case 5 -> onOff(data.toggle(Toggle.THE_NOISE));
            default -> null;
        };
    }

    /**
     * The Hex Barrier's line while it is up (3.3): "forming", then its health as a bar of ten cells and in numbers ("21/30"),
     * green, then yellow below half and red below a quarter. The big bar under the crosshair is {@link BarrierHud}'s.
     */
    public static Component hexBarrier(LocalPlayer player, PowerData data) {
        if (!data.chaosShieldUp(player.level().getGameTime())) {
            return Component.translatable("hud.enhanced.shield_forming").withStyle(ChatFormatting.GOLD);
        }
        float max = BillyBarrier.maxHealth(player);
        float left = Math.min(data.shieldStrength(), max);
        float fraction = max <= 0 ? 0 : left / max;
        ChatFormatting color = fraction > 0.5F ? ChatFormatting.GREEN : fraction > 0.25F ? ChatFormatting.YELLOW : ChatFormatting.RED;
        int cells = Mth.clamp(Math.round(fraction * 10), left > 0 ? 1 : 0, 10);
        String bar = "\u25AE".repeat(cells) + "\u25AF".repeat(10 - cells);
        return Component.translatable("hud.enhanced.barrier_health", bar, (int) Math.ceil(left), (int) Math.ceil(max)).withStyle(color);
    }

    /**
     * The Hex Beam's line (3.2): what it does at his rank now, in grey, and green while it fires ("6.0/s, 20 blocks": damage a
     * second and range). The numbers grow with his Build Magic rank.
     */
    private static Component hexBeam(LocalPlayer player, PowerData data) {
        BuildData build = ModAttachments.BUILD_DATA.getExisting(player).orElse(BuildData.EMPTY);
        String perSecond = String.format(Locale.ROOT, "%.1f", BillyAbilities.beamDamagePerSecond(build));
        int range = (int) Math.round(BillyAbilities.beamRange(build));
        boolean firing = data.is(Live.BEAMING);
        return Component.translatable(firing ? "hud.enhanced.billy_beam_firing" : "hud.enhanced.billy_beam", perSecond, range)
                .withStyle(firing ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static Component tommy(LocalPlayer player, PowerData data, int ability) {
        return switch (ability) {
            case 1 -> {
                MutableComponent line = slowTime(data.toggle(Toggle.TOMMY_TIME), false).copy();
                if (data.toggle(Toggle.TOMMY_WALL_RUN_OFF)) {
                    line.append(Component.literal(" ")).append(Component.translatable("hud.enhanced.walls_off").withStyle(ChatFormatting.DARK_GRAY));
                }
                yield line;
            }
            case 2 -> running(data, data.toggle(Toggle.SUPER_SPEED));
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

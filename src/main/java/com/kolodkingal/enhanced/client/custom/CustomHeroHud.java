package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.ability.AgathaShield;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.client.BossBarOverride;
import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The top-left panel of a custom hero (3.3), in his colours: his name, then each key with the power he put on it and its
 * state (its cooldown, on or off, the strength left in the Force Field, the energy absorbed), then the powers that need no
 * key. It stands where the panel of the other heroes ({@code PowerHud}) does, which leaves it to this one. 3.4: a line
 * for each Sneak binding under its key ({@code [⇧R]}: a hero's Sneak variant, another option of a power, or a power of
 * its own; the key's cooldown is shared, so only the key's line shows it), the heroes' abilities by their names with their
 * state (on, off, active), and the keys' labels in his glow colour.
 */
public class CustomHeroHud implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || player.isDeadOrDying()) {
            return;
        }
        PowerData data = Powers.get(player);
        if (data.power() != Power.CUSTOM || data.kit() == null) {
            return;
        }
        CustomHero hero = CustomHeroes.hero(player);
        CustomState state = CustomHeroes.state(player);
        HeroKit kit = data.kit();
        Font font = mc.font;
        long now = mc.level.getGameTime();

        Component title = CustomHeroes.name(player).copy().withStyle(s -> s.withColor(hero.color()).withBold(true));
        List<Component> lines = new ArrayList<>();
        for (int column = 1; column <= CustomHero.KEYS; column++) {
            Component key = KeyBindings.ability(column).getTranslatedKeyMessage();
            HeroKit.Binding plain = kit.plain(column);
            MutableComponent line = label(Component.literal("[").append(key).append("] "), hero);
            if (plain == null) {
                line.append(Component.translatable("hud.enhanced.custom.free").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                line.append(name(plain).withStyle(s -> s.withColor(colorOf(hero, plain.picked()))));
                Component status = status(player, data, state, hero, plain, column, now, true);
                if (status != null) {
                    line.append(Component.literal("  ")).append(status);
                }
            }
            lines.add(line);
            HeroKit.Binding sneak = kit.sneak(column);
            if (sneak != null) {
                MutableComponent sub = label(Component.literal(" [").append(Component.translatable("hud.enhanced.custom.sneak", key))
                        .append("] "), hero);
                sub.append(name(sneak).withStyle(s -> s.withColor(colorOf(hero, sneak.picked()))));
                Component status = status(player, data, state, hero, sneak, column, now, false);
                if (status != null) {
                    sub.append(Component.literal("  ")).append(status);
                }
                lines.add(sub);
            }
        }
        List<Component> passive = new ArrayList<>();
        for (CustomHero.Pick pick : hero.powers()) {
            CatalogEntry entry = pick.entry();
            if (entry != null && onNoKey(kit, entry)) {
                // (3.5: in the colour its pick chose, else the hero's glow)
                passive.add(CreatorUi.name(entry).withStyle(s -> s.withColor(pick.color() < 0 ? hero.glow()
                        : pick.color())));
            }
        }
        if (!passive.isEmpty()) {
            MutableComponent line = Component.translatable("hud.enhanced.custom.always").withStyle(ChatFormatting.GRAY).append(" ");
            for (int i = 0; i < passive.size(); i++) {
                if (i > 0) {
                    line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                }
                line.append(passive.get(i).copy());
            }
            lines.add(line);
        }

        int width = font.width(title) + 4;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int x = 6;
        int y = 4 + BossBarOverride.clearance(x + width + 4);
        int height = 12 + lines.size() * 10;
        graphics.fill(x - 4, y - 2, x + width + 4, y + height, 0x70000000);
        // A stripe of his two colours down the side of the panel, and a thin line of his colour under the name.
        graphics.fill(x - 4, y - 2, x - 2, y + height / 2, 0xFF000000 | hero.color());
        graphics.fill(x - 4, y + height / 2, x - 2, y + height, 0xFF000000 | hero.glow());
        graphics.fill(x, y + 9, x + font.width(title), y + 10, 0x90000000 | hero.color());
        graphics.drawString(font, title, x, y, 0xFFFFFFFF, true);
        y += 12;
        for (Component line : lines) {
            graphics.drawString(font, line, x, y, 0xFFFFFFFF, true);
            y += 10;
        }
    }

    private static MutableComponent label(MutableComponent text, CustomHero hero) {
        return text.withStyle(s -> s.withColor(hero.glow()));
    }

    /** Whether a picked entry is on none of the ten bindings (an always-on power, or a hero's ability left off the keys). */
    private static boolean onNoKey(HeroKit kit, CatalogEntry entry) {
        for (int column = 1; column <= CustomHero.KEYS; column++) {
            for (HeroKit.Binding b : new HeroKit.Binding[]{kit.plain(column), kit.sneak(column)}) {
                if (b != null && (b.entry() == entry || b.picked() == entry)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The name of what a binding does: a hero's ability or its Sneak variant, a power or one of its options. */
    public static MutableComponent name(HeroKit.Binding b) {
        if (b.picked() instanceof HeroAbility v && v.isVariant()) {
            return CreatorUi.name(v); // (3.5: a variant row, by its own name)
        }
        if (b.entry() instanceof HeroAbility a) {
            return b.isAlt() ? CreatorUi.altName(a) : CreatorUi.name(a);
        }
        PowerModule m = b.module();
        if (m != null && !HeroKit.MAIN.equals(b.action())) {
            return Component.translatable(m.optionKey(b.action()));
        }
        return CreatorUi.name(b.entry());
    }

    private static int colorOf(CustomHero hero, CatalogEntry entry) {
        return hero.colorOf(hero.pick(entry));
    }

    /** The state after a binding: the key's cooldown (on the key's own line), else what it is doing. */
    @Nullable
    private static Component status(LocalPlayer player, PowerData data, CustomState state, CustomHero hero, HeroKit.Binding b, int column,
                                    long now, boolean keyLine) {
        if (b.entry() instanceof HeroAbility a) {
            Component shield = shield(player, data, a, b.isAlt(), now);
            if (shield != null) {
                return shield;
            }
            Component cooldown = keyLine ? PowerHud.cooldown(data, column, now) : null;
            if (cooldown != null) {
                return cooldown;
            }
            if (b.picked() instanceof HeroAbility v && v.isVariant()) {
                a = v; // (3.5: a variant row's own state)
            } else if (!a.hasOnState() || b.isAlt()) {
                return null;
            }
            if (!a.hasOnState()) {
                return null;
            }
            boolean on = a.isOn(data);
            return a.act() == PowerModule.Act.TOGGLE ? onOff(on)
                    : on ? Component.translatable("hud.enhanced.active").withStyle(ChatFormatting.AQUA) : null;
        }
        PowerModule module = b.module();
        CustomHero.Pick pick = hero.pick(module);
        if (pick == null) {
            return null;
        }
        if (module == PowerModule.FORCE_FIELD && state.field()) {
            return Component.translatable("hud.enhanced.custom.field", (int) Math.ceil(state.fieldStrength() / 2F))
                    .withStyle(ChatFormatting.GREEN);
        }
        Component cooldown = keyLine ? PowerHud.cooldown(data, column, now) : null;
        if (cooldown != null) {
            return cooldown;
        }
        String option = HeroKit.MAIN.equals(b.action()) ? pick.option() : b.action();
        if (module == PowerModule.FLIGHT) {
            return onOff(data.flying());
        }
        if (module == PowerModule.ABSORPTION && "energy".equals(option)) {
            int max = Math.round(ModuleStats.ABSORB_CAPACITY.value(pick.strength()) / 2F);
            return Component.translatable("hud.enhanced.custom.charge", Math.round(state.charge() / 2F), max)
                    .withStyle(state.charge() > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY);
        }
        if (module == PowerModule.TELEKINESIS) {
            return data.heldEntityId() != -1 ? Component.translatable("hud.enhanced.holding").withStyle(ChatFormatting.AQUA) : null;
        }
        if (module == PowerModule.SHAPESHIFT && "creature".equals(option)) {
            return data.form().isEmpty() ? null : Component.translatable("hud.enhanced.on").withStyle(ChatFormatting.GREEN);
        }
        return switch (module.act(option)) {
            case TOGGLE -> onOff(state.isOn(module));
            case HOLD -> state.isOn(module) ? Component.translatable("hud.enhanced.active").withStyle(ChatFormatting.AQUA) : null;
            default -> null;
        };
    }

    /**
     * The line of a borrowed shield while it is up, as its hero's HUD shows it: the Scarlet Witch's, Agatha's and
     * Apocalypse's shields (strength and time), Billy's Hex Barrier (its health). One shield at a time (the rules).
     */
    @Nullable
    private static Component shield(LocalPlayer player, PowerData data, HeroAbility a, boolean alt, long now) {
        if (!data.chaosShieldActive()) {
            return null;
        }
        return switch (a) {
            case WANDA_CHAOS_SHIELD -> PowerHud.shield(data, now, WandaAbilities.maxShieldStrength(), WandaAbilities.maxShieldTicks());
            case AGATHA_SHIELD -> PowerHud.shield(data, now, AgathaShield.maxStrength(), AgathaShield.maxTicks());
            case APOCALYPSE_SHIELD -> PowerHud.shield(data, now, ApocalypseGuard.maxStrength(), ApocalypseGuard.maxTicks());
            case BILLY_FREEZE -> alt && BillyBarrier.enabled() ? PowerHud.hexBarrier(player, data) : null;
            default -> null;
        };
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }
}

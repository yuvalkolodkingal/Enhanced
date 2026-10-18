package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.client.BossBarOverride;
import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
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
 * key. It stands where the panel of the other heroes ({@code PowerHud}) does, which leaves it to this one.
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
        if (data.power() != Power.CUSTOM) {
            return;
        }
        CustomHero hero = CustomHeroes.hero(player);
        CustomState state = CustomHeroes.state(player);
        Font font = mc.font;
        long now = mc.level.getGameTime();

        Component title = CustomHeroes.name(player).copy().withStyle(s -> s.withColor(hero.color()).withBold(true));
        List<Component> lines = new ArrayList<>();
        for (int slot = 1; slot <= CustomHero.KEYS; slot++) {
            MutableComponent line = Component.literal("[").append(KeyBindings.ability(slot).getTranslatedKeyMessage()).append("] ")
                    .withStyle(ChatFormatting.YELLOW);
            CustomHero.Pick pick = hero.onKey(slot);
            if (pick == null || pick.module() == null) {
                line.append(Component.translatable("hud.enhanced.custom.free").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                int color = hero.colorOf(pick);
                line.append(Component.translatable(pick.module().nameKey()).withStyle(s -> s.withColor(color)));
                Component status = status(player, data, state, pick, slot, now);
                if (status != null) {
                    line.append(Component.literal("  ")).append(status);
                }
            }
            lines.add(line);
        }
        List<Component> passive = new ArrayList<>();
        for (CustomHero.Pick pick : hero.powers()) {
            if (pick.module() != null && hero.keyOf(pick.module()) == 0) {
                passive.add(Component.translatable(pick.module().nameKey()));
            }
        }
        if (!passive.isEmpty()) {
            MutableComponent line = Component.translatable("hud.enhanced.custom.always").withStyle(ChatFormatting.GRAY).append(" ");
            for (int i = 0; i < passive.size(); i++) {
                if (i > 0) {
                    line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                }
                line.append(passive.get(i).copy().withStyle(s -> s.withColor(hero.glow())));
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
        // A stripe of his two colours down the side of the panel.
        graphics.fill(x - 4, y - 2, x - 2, y + height / 2, 0xFF000000 | hero.color());
        graphics.fill(x - 4, y + height / 2, x - 2, y + height, 0xFF000000 | hero.glow());
        graphics.drawString(font, title, x, y, 0xFFFFFFFF, true);
        y += 12;
        for (Component line : lines) {
            graphics.drawString(font, line, x, y, 0xFFFFFFFF, true);
            y += 10;
        }
    }

    /** The state after a key's power: its cooldown, else what it is doing. */
    @Nullable
    private static Component status(LocalPlayer player, PowerData data, CustomState state, CustomHero.Pick pick, int slot, long now) {
        PowerModule module = pick.module();
        if (module == PowerModule.FORCE_FIELD && state.field()) {
            return Component.translatable("hud.enhanced.custom.field", (int) Math.ceil(state.fieldStrength() / 2F))
                    .withStyle(ChatFormatting.GREEN);
        }
        Component cooldown = PowerHud.cooldown(data, slot, now);
        if (cooldown != null) {
            return cooldown;
        }
        if (module == PowerModule.FLIGHT) {
            return onOff(data.flying());
        }
        if (module == PowerModule.ABSORPTION && "energy".equals(pick.option())) {
            int max = Math.round(ModuleStats.ABSORB_CAPACITY.value(pick.strength()) / 2F);
            return Component.translatable("hud.enhanced.custom.charge", Math.round(state.charge() / 2F), max)
                    .withStyle(state.charge() > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY);
        }
        if (module == PowerModule.TELEKINESIS) {
            return data.heldEntityId() != -1 ? Component.translatable("hud.enhanced.holding").withStyle(ChatFormatting.AQUA) : null;
        }
        if (module == PowerModule.SHAPESHIFT && "creature".equals(pick.option())) {
            return data.form().isEmpty() ? null : Component.translatable("hud.enhanced.on").withStyle(ChatFormatting.GREEN);
        }
        return switch (pick.act()) {
            case TOGGLE -> onOff(state.isOn(module));
            case HOLD -> state.isOn(module) ? Component.translatable("hud.enhanced.active").withStyle(ChatFormatting.AQUA) : null;
            default -> null;
        };
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }
}

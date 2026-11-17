package com.kolodkingal.enhanced.client.magic;

import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.config.UltimateConfig;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.magic.MagicData;
import com.kolodkingal.enhanced.magic.Spell;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.ultimate.Ultimate;
import com.kolodkingal.enhanced.ultimate.UltimateForm;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The magic's panel (3.17), to the right of the hotbar: a caster's mana (a bar of his magic's colour, brighter in a coven),
 * the spell K casts with its key, the incantation's progress while he chants (its words fill as he speaks them), the
 * Darkhold's corruption when there is any; and, with Ultimate Mode on, the ultimate form: ready on O, or the time it
 * has left while it lasts.
 */
public class MagicHud implements LayeredDraw.Layer {
    private long chantStart = -1;
    private long formStart = -1;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.level == null || player.isSpectator()) {
            return;
        }
        Font font = mc.font;
        PowerData data = Powers.get(player);
        long now = mc.level.getGameTime();
        int x = graphics.guiWidth() / 2 + 98;
        int y = graphics.guiHeight() - 22;
        if (Magic.isCaster(player)) {
            MagicData magic = Magic.data(player);
            int color = Magic.casterColor(player);
            float max = Magic.maxMana(player);
            float mana = Magic.mana(player);
            int w = 82;
            graphics.fill(x - 1, y - 1, x + w + 1, y + 5, 0xC0000000);
            graphics.fill(x, y, x + Math.round(w * Math.min(1.0F, mana / Math.max(1.0F, max))), y + 4, 0xFF000000 | color);
            graphics.drawString(font, Component.translatable("hud.enhanced.mana", (int) mana, (int) max), x, y - 10, 0xFF000000 | Magic.casterGlow(player), true);
            Spell spell = Spell.byId(magic.selected());
            if (spell == null || !Magic.knows(player, spell)) {
                List<Spell> known = Magic.known(player);
                spell = known.isEmpty() ? null : known.get(0);
            }
            if (spell != null) {
                Component line = Component.translatable("hud.enhanced.spell", KeyBindings.SPELLS.getTranslatedKeyMessage(), spell.displayName());
                graphics.drawString(font, line, x, y - 20, 0xFF000000 | Magic.color(player, spell), true);
            }
            if (data.is(Live.CHANTING)) {
                if (chantStart < 0) {
                    chantStart = now;
                }
                float need = spell == null || spell.chant <= 0 ? 20 : spell.chant;
                float f = Math.min(1.0F, (now - chantStart) / need * (KeyBindings.SPELLS.isDown() ? 2.0F : 1.0F));
                int cw = 120;
                int cx = graphics.guiWidth() / 2 - cw / 2;
                int cy = graphics.guiHeight() - 72;
                graphics.fill(cx - 1, cy - 1, cx + cw + 1, cy + 4, 0xC0000000);
                graphics.fill(cx, cy, cx + Math.round(cw * f), cy + 3, 0xFF000000 | color);
                graphics.drawCenteredString(font, Component.translatable("hud.enhanced.chanting"), graphics.guiWidth() / 2, cy - 11,
                        0xFF000000 | Magic.casterGlow(player));
            } else {
                chantStart = -1;
            }
            if (magic.corruption() > 0) {
                graphics.drawString(font, Component.translatable("hud.enhanced.corruption", magic.corruption()), x, y + 7, 0xFFB45CFF, true);
            }
        }
        if (Ultimate.on()) {
            UltimateForm form = UltimateForm.of(player);
            if (Magic.isDemiurge(player)) {
                // (3.17) the Demiurge's magic type: whose abilities his keys have now (Sneak + O changes it)
                com.kolodkingal.enhanced.power.Power borrowed = com.kolodkingal.enhanced.ultimate.Demiurge.borrowed(player);
                Component type = borrowed == com.kolodkingal.enhanced.power.Power.NONE
                        ? Component.translatable("message.enhanced.demiurge.own") : borrowed.displayName();
                graphics.drawString(font, Component.translatable("hud.enhanced.demiurge.type", type,
                        KeyBindings.ULTIMATE.getTranslatedKeyMessage()), x, (Magic.isCaster(player) ? y - 40 : y - 20), 0xFF7FE0FF, true);
            }
            if (form != null) {
                int uy = Magic.isCaster(player) ? y - 30 : y - 10;
                int c = 0xFF000000 | form.color(player);
                if (data.is(Live.ULTIMATE)) {
                    if (formStart < 0) {
                        formStart = now;
                    }
                    if (form == UltimateForm.SCARLET_WITCH) {
                        // (the Nexus Being has no time limit: Sneak + O ends it)
                        graphics.drawString(font, Component.translatable("hud.enhanced.ultimate.untimed", form.displayName(),
                                KeyBindings.ULTIMATE.getTranslatedKeyMessage(), form.signatureName()), x, uy, c, true);
                    } else {
                        int total = EnhancedConfig.SPEC.isLoaded() ? UltimateConfig.DURATION_SECONDS.get() : 45;
                        int left = Math.max(0, total - (int) ((now - formStart) / 20));
                        graphics.drawString(font, Component.translatable("hud.enhanced.ultimate.active", form.displayName(), left,
                                KeyBindings.ULTIMATE.getTranslatedKeyMessage(), form.signatureName()), x, uy, c, true);
                    }
                } else {
                    formStart = -1;
                    graphics.drawString(font, Component.translatable("hud.enhanced.ultimate.ready", KeyBindings.ULTIMATE.getTranslatedKeyMessage(),
                            form.displayName()), x, uy, data.is(Live.ULTIMATE_AWAKENING) ? 0xFFFFFFFF : c, true);
                }
            }
        }
    }
}

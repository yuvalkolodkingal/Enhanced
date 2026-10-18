package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.network.CerebroPayload;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * What Professor X's powers lay over the screen (3.3), eased so nothing pops: the cold blue at the edges while his mind
 * is in another body, with a line saying whose, its health, the seconds left and the key that brings him back; the
 * violet at the edges of a player he steers, and a line telling them so; a red flash when a stronger telepath throws him
 * out; and the blue of Cerebro, with the list of the minds it found (name, hero, distance, a pointer to where they
 * are).
 */
public class XavierOverlay implements LayeredDraw.Layer {
    private static final int LINK_BLUE = 0x4A6CFF;
    private static final int CONTROL_VIOLET = 0x8A3CFF;
    private static final int THROWN_RED = 0xFF2840;
    private static final int CEREBRO_BLUE = 0x9EC2FF;
    /** The red flash lasts this long (ticks). */
    private static final float FLASH_TICKS = 16.0F;

    private float link;
    private float control;
    private float cerebro;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            link = 0.0F;
            control = 0.0F;
            cerebro = 0.0F;
            return;
        }
        float ease = Math.min(1.0F, deltaTracker.getRealtimeDeltaTicks() * 0.2F);
        link += ((XavierControl.inBody() ? 1.0F : 0.0F) - link) * ease;
        control += ((XavierControl.controlled() ? 1.0F : 0.0F) - control) * ease;
        cerebro += ((CerebroView.active() ? 1.0F : 0.0F) - cerebro) * ease;
        if (mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        if (cerebro > 0.01F) {
            shade(graphics, CEREBRO_BLUE, 0.35F * cerebro);
        }
        if (link > 0.01F) {
            shade(graphics, LINK_BLUE, 0.4F * link);
            Entity host = XavierControl.host();
            if (host != null && XavierControl.inBody()) {
                Component line = Component.translatable("hud.enhanced.xavier.in_body", host.getDisplayName(),
                        String.format(Locale.ROOT, "%.0f", XavierControl.ticksLeft() / 20.0), KeyBindings.ability(3).getTranslatedKeyMessage());
                graphics.drawCenteredString(font, line.copy().withStyle(s -> s.withColor(Power.PROFESSOR_X.glow)), w / 2, h - 96, 0xFFFFFF);
                if (host instanceof LivingEntity living) {
                    Component hp = Component.translatable("hud.enhanced.xavier.body_health",
                            String.format(Locale.ROOT, "%.0f", living.getHealth()), String.format(Locale.ROOT, "%.0f", living.getMaxHealth()));
                    graphics.drawCenteredString(font, hp.copy().withStyle(ChatFormatting.RED), w / 2, h - 86, 0xFFFFFF);
                }
            }
        }
        if (control > 0.01F) {
            shade(graphics, CONTROL_VIOLET, 0.45F * control);
            if (XavierControl.controlled()) {
                graphics.drawCenteredString(font, Component.translatable("hud.enhanced.xavier.controlled")
                        .withStyle(s -> s.withColor(0xD9B8FF).withBold(true)), w / 2, h / 2 + 24, 0xFFFFFF);
            }
        }
        float pt = deltaTracker.getGameTimeDeltaPartialTick(false);
        float flash = 1.0F - Mth.clamp((XavierControl.sinceThrown() + pt) / FLASH_TICKS, 0.0F, 1.0F);
        if (flash > 0.01F) {
            int a = Math.round(140 * flash * flash) << 24;
            graphics.fill(0, 0, w, h, a | THROWN_RED);
        }
        if (cerebro > 0.01F && CerebroView.active()) {
            list(graphics, font, mc, w);
        }
    }

    /** The minds Cerebro found, top right: an arrow toward each, the name, the hero, the distance. */
    private static void list(GuiGraphics graphics, Font font, Minecraft mc, int w) {
        List<CerebroPayload.Mind> minds = CerebroView.minds();
        int y = 6;
        Component title = Component.translatable("hud.enhanced.xavier.cerebro", minds.size(),
                String.format(Locale.ROOT, "%.0f", CerebroView.ticksLeft() / 20.0));
        graphics.drawString(font, title.copy().withStyle(s -> s.withColor(CEREBRO_BLUE).withBold(true)), w - font.width(title) - 8, y, 0xFFFFFF);
        y += 12;
        Power[] powers = Power.values();
        Vec3 eye = mc.player.getEyePosition();
        for (int i = 0; i < Math.min(8, minds.size()); i++) {
            CerebroPayload.Mind mind = minds.get(i);
            Power power = mind.power() >= 0 && mind.power() < powers.length ? powers[mind.power()] : Power.NONE;
            Vec3 d = mind.pos().subtract(eye);
            double angle = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-d.x, d.z)) - mc.player.getYRot());
            String arrow = arrow(angle);
            Component line = Component.literal(arrow + " ").append(power.hasPowers()
                    ? Component.translatable("hud.enhanced.xavier.cerebro.mind_hero", mind.name(), power.displayName(), Math.round(d.length()))
                    : Component.translatable("hud.enhanced.xavier.cerebro.mind", mind.name(), Math.round(d.length())));
            int rgb = power.hasPowers() ? power.titleColor() : 0xE8EEFF;
            graphics.drawString(font, line.copy().withStyle(s -> s.withColor(rgb)), w - font.width(line) - 8, y, 0xFFFFFF);
            y += 10;
        }
    }

    /** One of eight arrows for a direction relative to where he faces (0 ahead, 90 to the right). */
    static String arrow(double angle) {
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        int i = Math.floorMod((int) Math.round(angle / 45.0), 8);
        return arrows[i];
    }

    /** A shade at the edges of the screen, strongest at the edge, as the other overlays of the mod. */
    private static void shade(GuiGraphics graphics, int rgb, float alpha) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int edge = (Math.round(255 * alpha) & 0xFF) << 24 | rgb;
        int mid = (Math.round(255 * alpha * 0.15F) & 0xFF) << 24 | rgb;
        graphics.fillGradient(0, 0, w, h / 3, edge, mid);
        graphics.fill(0, h / 3, w, h - h / 3, mid);
        graphics.fillGradient(0, h - h / 3, w, h, mid, edge);
    }
}

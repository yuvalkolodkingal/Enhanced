package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.HeroLooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * The pieces the Hero Creator's screens are built of (3.3), made for an eleven-year-old: big buttons with a picture and a
 * coloured frame, colour swatches, a slider that says its value, and a paper-doll picture of a skin (its front, at any
 * size), which is the live preview of the hero being made.
 */
public final class CreatorUi {
    public static int darken(int rgb) {
        return (rgb >> 1) & 0x7F7F7F;
    }

    /**
     * A big button: a dark panel framed in {@code color}, a picture (an item, or a skin's face) on the left, the label in
     * bold colour and an optional line under it.
     */
    public static class BigButton extends AbstractButton {
        protected int color;
        @Nullable
        protected Component sub;
        @Nullable
        protected ItemStack icon;
        @Nullable
        protected ResourceLocation face;
        protected boolean selected;
        private final Runnable action;

        public BigButton(int x, int y, int width, int height, Component label, @Nullable Component sub, int color, Runnable action) {
            super(x, y, width, height, label);
            this.sub = sub;
            this.color = color;
            this.action = action;
        }

        public BigButton icon(@Nullable ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public BigButton face(@Nullable ResourceLocation face) {
            this.face = face;
            return this;
        }

        public BigButton selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        protected boolean stacked;

        /** (3.4) The picture on top in the middle and the words under it (a narrow box: the Keys tab's). */
        public BigButton stacked() {
            this.stacked = true;
            return this;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean lit = isHoveredOrFocused() && active;
            int x = getX();
            int y = getY();
            g.fill(x, y, x + width, y + height, selected ? 0xF0283A52 : lit ? 0xE0182436 : 0xC0101824);
            g.renderOutline(x, y, width, height, 0xFF000000 | (lit || selected ? color : darken(color)));
            if (lit || selected) {
                g.renderOutline(x + 1, y + 1, width - 2, height - 2, 0x80000000 | color);
            }
            if (stacked) {
                int top = y + 2;
                if (icon != null) {
                    g.renderFakeItem(icon, x + (width - 16) / 2, top);
                    top += 17;
                }
                int tc = active ? color : 0x707070;
                Component l = getMessage().copy().withStyle(s -> s.withColor(tc).withBold(true));
                int lw = Math.min(font.width(l), width - 4);
                drawFitted(g, font, l, x + (width - lw) / 2, top, width - 4, 0xFFFFFF);
                if (sub != null) {
                    Component sc = sub.copy().withStyle(s -> s.withColor(active ? 0xB0B0B0 : 0x606060));
                    int sw = Math.min(font.width(sc), width - 4);
                    drawFitted(g, font, sc, x + (width - sw) / 2, top + 9, width - 4, 0xFFFFFF);
                }
                return;
            }
            int textX = x + 6;
            int picture = Math.min(height - 6, 16);
            if (icon != null) {
                g.renderFakeItem(icon, x + 4, y + (height - 16) / 2);
                textX = x + 24;
            } else if (face != null) {
                PlayerFaceRenderer.draw(g, face, x + 4, y + (height - picture) / 2, picture);
                textX = x + 8 + picture;
            }
            int textColor = active ? color : 0x707070;
            Component label = getMessage().copy().withStyle(s -> s.withColor(textColor).withBold(true));
            int room = x + width - 4 - textX;
            if (sub == null) {
                drawFitted(g, font, label, textX, y + (height - 8) / 2, room, 0xFFFFFF);
            } else {
                drawFitted(g, font, label, textX, y + height / 2 - 9, room, 0xFFFFFF);
                drawFitted(g, font, sub.copy().withStyle(s -> s.withColor(active ? 0xB0B0B0 : 0x606060)), textX, y + height / 2 + 2, room, 0xFFFFFF);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** Text drawn at its size if it fits {@code room}, else squeezed to fit (never cut). */
    public static void drawFitted(GuiGraphics g, Font font, Component text, int x, int y, int room, int color) {
        int w = font.width(text);
        if (w <= room || room <= 0) {
            g.drawString(font, text, x, y, color, true);
            return;
        }
        float scale = (float) room / w;
        g.pose().pushPose();
        g.pose().translate(x, y + (1 - scale) * 4, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, true);
        g.pose().popPose();
    }

    /** A square of colour: a click picks it. */
    public static class Swatch extends AbstractButton {
        private final int rgb;
        private final IntConsumer pick;
        private final java.util.function.IntSupplier current;

        public Swatch(int x, int y, int size, int rgb, IntConsumer pick, java.util.function.IntSupplier current) {
            super(x, y, size, size, Component.literal(String.format("#%06X", rgb)));
            this.rgb = rgb;
            this.pick = pick;
            this.current = current;
        }

        @Override
        public void onPress() {
            pick.accept(rgb);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            boolean chosen = current.getAsInt() == rgb;
            g.fill(x, y, x + width, y + height, 0xFF000000 | rgb);
            g.renderOutline(x, y, width, height, chosen ? 0xFFFFFFFF : isHoveredOrFocused() ? 0xFFC0C0C0 : 0xFF202020);
            if (chosen) {
                g.renderOutline(x + 1, y + 1, width - 2, height - 2, 0xFF000000);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** A slider over whole numbers {@code min..max} that shows its value with {@code label}. */
    public static class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntFunction<Component> label;
        private final IntConsumer changed;

        public IntSlider(int x, int y, int width, int height, int min, int max, int value, IntFunction<Component> label, IntConsumer changed) {
            super(x, y, width, height, Component.empty(), max == min ? 0 : (double) (value - min) / (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.changed = changed;
            updateMessage();
        }

        public int intValue() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(label.apply(intValue()));
        }

        @Override
        protected void applyValue() {
            changed.accept(intValue());
        }
    }

    /** The colour of a hue (0-359) at a brightness (0-100), at full saturation (a little less at the brightest). */
    public static int hsv(int hue, int bright) {
        float b = Mth.clamp(bright / 100F, 0F, 1F);
        float s = b > 0.8F ? 1F - (b - 0.8F) * 3.5F : 1F;
        return Mth.hsvToRgb(hue / 360F, Math.max(0.15F, s), Math.min(1F, b * 1.1F + 0.05F)) & 0xFFFFFF;
    }

    /** The hue (0-359) and brightness (0-100) of a colour, roughly. */
    public static int[] hueBright(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float hue = 0F;
        if (d > 1.0E-5F) {
            hue = max == r ? ((g - b) / d) % 6F : max == g ? (b - r) / d + 2F : (r - g) / d + 4F;
            hue *= 60F;
            if (hue < 0) {
                hue += 360F;
            }
        }
        return new int[]{Math.round(hue) % 360, Math.round(max * 100)};
    }

    // ---- the paper doll ----

    /** The texture and arm width of the skin a hero (being made) wears, for the preview. */
    public record Look(ResourceLocation texture, boolean slim) {
    }

    /** What the hero being made looks like: his PNG, the hero look he took, or the player's own skin. */
    public static Look lookOf(CustomHero hero) {
        if (hero.wearsSkin()) {
            ResourceLocation texture = CustomSkins.texture(hero.skin());
            if (texture != null) {
                return new Look(texture, hero.slim());
            }
        }
        HeroLooks.Look look = HeroLooks.byRef(hero.look());
        if (look != null) {
            return new Look(look.skin(), look.slim());
        }
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin own = mc.getSkinManager().getInsecureSkin(mc.getGameProfile());
        return new Look(own.texture(), own.model() == PlayerSkin.Model.SLIM);
    }

    /**
     * The front of a skin as a paper doll, {@code scale} screen pixels to a skin pixel (16 x 32 skin pixels: the head, the
     * body, both arms and legs, each with its outer layer).
     */
    public static void paperDoll(GuiGraphics g, Look look, int x, int y, int scale) {
        ResourceLocation t = look.texture();
        int arm = look.slim() ? 3 : 4;
        int s = scale;
        // head and hat
        part(g, t, x + 4 * s, y, 8, 8, 8, 8, s);
        part(g, t, x + 4 * s, y, 40, 8, 8, 8, s);
        // body and jacket
        part(g, t, x + 4 * s, y + 8 * s, 20, 20, 8, 12, s);
        part(g, t, x + 4 * s, y + 8 * s, 20, 36, 8, 12, s);
        // the right arm (on the left of the picture) and its sleeve; the left arm and its sleeve
        part(g, t, x + (4 - arm) * s, y + 8 * s, 44, 20, arm, 12, s);
        part(g, t, x + (4 - arm) * s, y + 8 * s, 44, 36, arm, 12, s);
        part(g, t, x + 12 * s, y + 8 * s, 36, 52, arm, 12, s);
        part(g, t, x + 12 * s, y + 8 * s, 52, 52, arm, 12, s);
        // legs and trousers
        part(g, t, x + 4 * s, y + 20 * s, 4, 20, 4, 12, s);
        part(g, t, x + 4 * s, y + 20 * s, 4, 36, 4, 12, s);
        part(g, t, x + 8 * s, y + 20 * s, 20, 52, 4, 12, s);
        part(g, t, x + 8 * s, y + 20 * s, 4, 52, 4, 12, s);
    }

    private static void part(GuiGraphics g, ResourceLocation t, int x, int y, int u, int v, int w, int h, int s) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(t, x, y, w * s, h * s, u, v, w, h, 64, 64);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    // ---- 3.4: the catalogue's words ----

    /** The name of a catalogue entry (a hero's ability falls back to its id's last word until its words exist). */
    public static MutableComponent name(CatalogEntry entry) {
        return Component.translatableWithFallback(entry.nameKey(), pretty(entry.id()));
    }

    /** The name of a hero ability's Sneak variant. */
    public static MutableComponent altName(HeroAbility a) {
        return Component.translatableWithFallback(a.altKey(), pretty(a.id()) + " (Sneak)");
    }

    /**
     * 3.6: the name of any entry's Sneak variant: a hero ability's, a stone row's Sneak use, or the entry's own name.
     */
    public static MutableComponent altName(com.kolodkingal.enhanced.custom.CatalogEntry entry) {
        if (entry instanceof HeroAbility a) {
            return altName(a);
        }
        if (entry instanceof com.kolodkingal.enhanced.custom.StoneRow row) {
            return Component.translatableWithFallback(row.altKey(), pretty(row.id()) + " (Sneak)");
        }
        return name(entry);
    }

    /** The one-line description of an entry. */
    public static MutableComponent desc(CatalogEntry entry) {
        return Component.translatableWithFallback(entry.descKey(), "");
    }

    /** {@code thor.lightning} as words: "Lightning". */
    public static String pretty(String id) {
        String last = id.substring(id.lastIndexOf('.') + 1).replace('_', ' ');
        return last.isEmpty() ? id : Character.toUpperCase(last.charAt(0)) + last.substring(1);
    }

    /** The badge of how a key acts: ▶ press, ✋ hold, ⏻ on/off, ★ always. */
    public static MutableComponent badge(PowerModule.Act act) {
        return Component.translatable("screen.enhanced.hero_creator.badge." + act.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** The item of a module's icon. */
    public static ItemStack icon(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        return id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    /** Does something with a value later on the render thread (a file dialog answers on its own thread). */
    public static <T> Consumer<T> onRenderThread(Consumer<T> action) {
        return value -> Minecraft.getInstance().execute(() -> action.accept(value));
    }

    private CreatorUi() {
    }
}

package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.client.render.PhoenixMath;
import com.kolodkingal.enhanced.network.MindReadPayload;
import com.kolodkingal.enhanced.power.LokiForm;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The Mind Reading panel: a dark violet frame beside the crosshair that lists what the creature the Phoenix looks at is
 * carrying and feeling. It is drawn only while Mind Reading is on and the last {@link MindReadPayload} is at most 15
 * ticks old (the server sends a snapshot on a new target, on a change and at least twice a second, so it goes out soon
 * after she looks away).
 *
 * <p>Rows, top to bottom: the name (violet, bold) and kind (grey); the mood as a coloured word; the health as hearts
 * and a thin bar (a player's food and level too); the seven slots it wears or holds (hands, head, chest, legs, feet,
 * body) with the names of what is in the hands; a player's pack as a 9 by 4 grid, or a creature's own inventory as a
 * row; and the strange truth about a shapeshifted Loki or a disguised Vision. A mind that cannot be read shows its
 * name and one line saying why. There is no cursor in a game, so the item names are written out instead of tooltips.
 */
public final class MindReadingOverlay implements LayeredDraw.Layer {
    private static final ResourceLocation HEART = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation FOOD = ResourceLocation.withDefaultNamespace("hud/food_full");
    private static final int PAD = 5;
    private static final int SLOT = 18;
    private static final int CELL = 12;
    private static final int WORN_SLOTS = 7;
    private static final int PACK_COLUMNS = 9;
    private static final int FRAME = 0xA01A1030;
    private static final int SLOT_BACK = 0x66000000;
    private static final int SLOT_EDGE = 0x55B79CFF;
    private static final int GREY = 0xFFAAAAAA;
    private static final int CALM = 0xFFB79CFF;
    private static final int ALERT = 0xFFFFD65A;
    private static final int HOSTILE = 0xFFFF5C8A;
    private static final int AFRAID = 0xFF9ED8FF;
    private static final int FADE_TICKS = 4;

    /** The last snapshot, when it came (game time), and since when it has been about the same creature. */
    @Nullable
    private static MindReadPayload latest;
    private static long receivedAt = Long.MIN_VALUE;
    private static int targetId = -1;
    private static long targetSince;

    /** The server's snapshot of what she is reading (called by the payload, on the client). */
    public static void accept(MindReadPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = mc.level.getGameTime();
        if (latest == null || payload.entityId() != targetId || !PhoenixMath.panelFresh(now, receivedAt)) {
            targetSince = now;
        }
        targetId = payload.entityId();
        latest = payload;
        receivedAt = now;
    }

    /** Forget the snapshot (the client left the world, or Mind Reading was turned off). */
    public static void clear() {
        latest = null;
        receivedAt = Long.MIN_VALUE;
        targetId = -1;
    }

    /** Whether the panel is up right now. */
    public static boolean visible(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || latest == null) {
            return false;
        }
        PowerData data = Powers.get(player);
        return data.power() == Power.PHOENIX && data.toggle(Toggle.MIND_READING) && PhoenixMath.panelFresh(mc.level.getGameTime(), receivedAt);
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || mc.screen != null || !visible(mc) || latest == null) {
            return;
        }
        MindReadPayload snapshot = latest;
        float time = mc.level.getGameTime() + deltaTracker.getGameTimeDeltaPartialTick(false);
        boolean calm = EnhancedClientConfig.reducedMotion();
        float fade = calm ? 1.0F : Mth.clamp((time - targetSince) / FADE_TICKS, 0.0F, 1.0F);

        // Measure first, then place and draw: the panel is as big as what it holds.
        Painter measure = new Painter(null, mc.font, 0, 0, 1.0F, 0.0F);
        measure.paint(snapshot);
        int width = measure.width + 2 * PAD;
        int height = measure.y + 2 * PAD;
        float scale = Math.min(1.0F, (graphics.guiHeight() - 2 * PhoenixMath.PANEL_MARGIN) / (float) height);
        int shownWidth = Math.round(width * scale);
        int shownHeight = Math.round(height * scale);
        int[] origin = PhoenixMath.panelOrigin(graphics.guiWidth(), graphics.guiHeight(), shownWidth, shownHeight, graphics.guiWidth() / 2,
                graphics.guiHeight() / 2);

        graphics.pose().pushPose();
        graphics.pose().translate(origin[0], origin[1], 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        // The frame: dark violet, a one pixel border, a little mark in each corner, and a scanline drifting down it.
        graphics.fill(0, 0, width, height, withAlpha(FRAME, fade));
        graphics.renderOutline(0, 0, width, height, withAlpha(0xFF000000 | PhoenixAbilities.PSY_MAIN, fade));
        int mark = withAlpha(0xFF000000 | PhoenixAbilities.PSY_CORE, fade);
        graphics.fill(0, 0, 5, 1, mark);
        graphics.fill(0, 0, 1, 5, mark);
        graphics.fill(width - 5, height - 1, width, height, mark);
        graphics.fill(width - 1, height - 5, width, height, mark);
        // A bright line drifting down the panel at 20 pixels a second (one a tick); still with reduced motion.
        int scan = calm ? height / 3 : (int) (time % Math.max(1, height - 2)) + 1;
        graphics.fill(1, scan, width - 1, scan + 1, withAlpha(0x30000000 | PhoenixAbilities.PSY_MAIN, fade));

        Painter painter = new Painter(graphics, mc.font, PAD, PAD, fade, time);
        painter.paint(snapshot);
        graphics.pose().popPose();
    }

    /** The colour with its alpha scaled by {@code fade}. */
    private static int withAlpha(int argb, float fade) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(fade, 0.0F, 1.0F));
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** The name of the kind of creature ("Zombie"; "Player" for a player). */
    private static Component kind(String species) {
        Optional<EntityType<?>> type = EntityType.byString(species);
        return type.map(EntityType::getDescription).orElse(Component.literal(species));
    }

    /** What the panel says about a mind it cannot read. */
    @Nullable
    private static String refusal(int state) {
        return switch (state) {
            case MindReadPayload.BLOCKED -> "screen.enhanced.mind_read.blocked";
            case MindReadPayload.ILLUSION -> "screen.enhanced.mind_read.illusion";
            case MindReadPayload.TOO_VAST -> "screen.enhanced.mind_read.vast";
            case MindReadPayload.PLAYERS_OFF -> "screen.enhanced.mind_read.players_off";
            default -> null;
        };
    }

    /**
     * Lays the panel out row by row. With no graphics it only measures (how wide and how tall the rows come to), so the
     * frame can be sized before anything is drawn.
     */
    private static final class Painter {
        @Nullable
        private final GuiGraphics g;
        private final Font font;
        private final int left;
        private final float fade;
        private final float time;
        int y;
        int width;

        Painter(@Nullable GuiGraphics g, Font font, int left, int top, float fade, float time) {
            this.g = g;
            this.font = font;
            this.left = left;
            this.y = top;
            this.fade = fade;
            this.time = time;
        }

        void paint(MindReadPayload p) {
            // The title: its name in bold violet, and what it is in grey.
            Component kind = kind(p.species());
            boolean named = !p.name().isEmpty() && !p.name().equals(kind.getString());
            MutableComponent title = (named ? Component.literal(p.name()) : kind.copy()).withStyle(s -> s.withBold(true).withColor(PhoenixAbilities.PSY_MAIN));
            int x = text(title, 0xFFFFFFFF, 0, true);
            if (named) {
                text(kind.copy().withStyle(ChatFormatting.GRAY), GREY, x + 4, false);
            }
            y += 12;
            String refusal = refusal(p.state());
            if (refusal != null) {
                for (var line : font.split(Component.translatable(refusal), 140)) {
                    textLine(line, p.state() == MindReadPayload.BLOCKED ? GREY : CALM);
                    y += 10;
                }
                return;
            }
            mood(p);
            health(p);
            worn(p);
            if (p.state() == MindReadPayload.OK && !p.carried().isEmpty()) {
                if (p.carried().size() > 8) {
                    pack(p.carried());
                } else {
                    creatureStacks(p);
                }
            }
            extra(p);
        }

        /** The mood as a coloured dot and word. */
        private void mood(MindReadPayload p) {
            int colour;
            Component word;
            switch (p.mood()) {
                case MindReadPayload.ALERT -> {
                    colour = ALERT;
                    word = Component.translatable("screen.enhanced.mind_read.mood.alert");
                }
                case MindReadPayload.HOSTILE -> {
                    colour = HOSTILE;
                    word = Component.translatable("screen.enhanced.mind_read.mood.hostile", p.hostileTo());
                }
                case MindReadPayload.AFRAID -> {
                    colour = AFRAID;
                    word = Component.translatable("screen.enhanced.mind_read.mood.afraid");
                }
                default -> {
                    colour = CALM;
                    word = Component.translatable("screen.enhanced.mind_read.mood.calm");
                }
            }
            if (g != null) {
                g.fill(left + 1, y + 2, left + 6, y + 7, withAlpha(0xFF000000 | colour, fade));
            }
            text(word, colour, 9, false);
            y += 11;
        }

        /** Hearts as a number and a thin bar; a player's food and level too. */
        private void health(MindReadPayload p) {
            if (g != null) {
                g.blitSprite(HEART, left, y, 9, 9);
            }
            String hearts = trim(p.health() / 2.0F) + " / " + trim(p.maxHealth() / 2.0F);
            int after = text(Component.literal(hearts), 0xFFFFFFFF, 12, false);
            int barX = after + 6;
            int barWidth = 40;
            if (g != null) {
                float f = p.maxHealth() <= 0.0F ? 0.0F : Mth.clamp(p.health() / p.maxHealth(), 0.0F, 1.0F);
                g.fill(left + barX, y + 3, left + barX + barWidth, y + 6, withAlpha(0x80000000, fade));
                g.fill(left + barX, y + 3, left + barX + Math.round(barWidth * f), y + 6, withAlpha(0xFFE0394A, fade));
            }
            width = Math.max(width, barX + barWidth);
            y += 12;
            if (p.food() > 0 || p.level() > 0) {
                if (g != null) {
                    g.blitSprite(FOOD, left, y, 9, 9);
                }
                int end = text(Component.literal(trim(p.food() / 2.0F) + " / 10"), 0xFFFFFFFF, 12, false);
                text(Component.translatable("screen.enhanced.mind_read.level", p.level()).withStyle(ChatFormatting.GREEN), 0xFF80FF20, end + 8, false);
                y += 12;
            }
        }

        /** What it wears or holds: seven slots, then the names of the hands. */
        private void worn(MindReadPayload p) {
            y += 1;
            text(Component.translatable("screen.enhanced.mind_read.carrying").withStyle(ChatFormatting.GRAY), GREY, 0, false);
            y += 10;
            List<ItemStack> worn = p.worn();
            for (int i = 0; i < WORN_SLOTS; i++) {
                slot(left + i * (SLOT + 1), y, i < worn.size() ? worn.get(i) : ItemStack.EMPTY);
            }
            width = Math.max(width, WORN_SLOTS * (SLOT + 1) - 1);
            y += SLOT + 3;
            for (int hand = 0; hand < 2 && hand < worn.size(); hand++) {
                ItemStack stack = worn.get(hand);
                if (!stack.isEmpty()) {
                    itemName(stack);
                    y += 10;
                }
            }
        }

        /** A player's whole pack: the hotbar row, then the three rows of the pack, as small icons. */
        private void pack(List<ItemStack> stacks) {
            y += 2;
            text(Component.translatable("screen.enhanced.mind_read.pack").withStyle(ChatFormatting.GRAY), GREY, 0, false);
            y += 10;
            int rows = (stacks.size() + PACK_COLUMNS - 1) / PACK_COLUMNS;
            for (int i = 0; i < stacks.size(); i++) {
                int row = i / PACK_COLUMNS;
                // The hotbar sits a little apart from the pack.
                int cy = y + row * CELL + (row > 0 ? 2 : 0);
                cell(left + (i % PACK_COLUMNS) * CELL, cy, stacks.get(i));
            }
            width = Math.max(width, PACK_COLUMNS * CELL);
            y += rows * CELL + 2 + 2;
        }

        /** A villager's, a piglin's or an Enderman's own things as a single row. */
        private void creatureStacks(MindReadPayload p) {
            y += 2;
            text(Component.translatable("screen.enhanced.mind_read.pack").withStyle(ChatFormatting.GRAY), GREY, 0, false);
            y += 10;
            List<ItemStack> stacks = p.carried();
            for (int i = 0; i < stacks.size(); i++) {
                slot(left + i * (SLOT + 1), y, stacks.get(i));
            }
            width = Math.max(width, stacks.size() * (SLOT + 1) - 1);
            y += SLOT + 3;
            if (stacks.size() == 1 && p.species().equals("minecraft:enderman")) {
                itemName(stacks.get(0));
                y += 10;
            }
        }

        /** "Really Loki, shaped as a Zombie" or "Vision (disguised)", in violet italics. */
        private void extra(MindReadPayload p) {
            Component line = null;
            if (p.extra().startsWith("shaped:")) {
                Component form = LokiForm.parse(p.extra().substring("shaped:".length())).map(LokiForm::displayName).orElse(Component.literal("?"));
                line = Component.translatable("screen.enhanced.mind_read.really", Power.LOKI.displayName(), form);
            } else if (p.extra().equals("disguised")) {
                line = Component.translatable("screen.enhanced.mind_read.disguised", Power.VISION.displayName());
            }
            if (line != null) {
                y += 2;
                for (var part : font.split(line.copy().withStyle(s -> s.withItalic(true).withColor(PhoenixAbilities.PSY_MAIN)), 140)) {
                    textLine(part, CALM);
                    y += 10;
                }
            }
        }

        // ---- the pieces ----

        /** The name of the item in the colour of its rarity, with a small star if it is enchanted. */
        private void itemName(ItemStack stack) {
            Integer rarity = stack.getRarity().color().getColor();
            int colour = 0xFF000000 | (rarity == null ? 0xFFFFFF : rarity);
            String name = stack.getHoverName().getString() + (stack.isEnchanted() ? " *" : "");
            text(Component.literal(font.plainSubstrByWidth(name, 150)), colour, 0, false);
        }

        /** One slot: a darker square with a faint edge and the item on it, with its count and its durability bar. */
        private void slot(int x, int top, ItemStack stack) {
            if (g != null) {
                g.fill(x, top, x + SLOT, top + SLOT, withAlpha(SLOT_BACK, fade));
                g.renderOutline(x, top, SLOT, SLOT, withAlpha(SLOT_EDGE, fade));
                if (!stack.isEmpty()) {
                    g.renderItem(stack, x + 1, top + 1);
                    g.renderItemDecorations(font, stack, x + 1, top + 1);
                }
            }
        }

        /** One small cell of the pack grid: the item at three quarters of its size. */
        private void cell(int x, int top, ItemStack stack) {
            if (g != null) {
                g.fill(x, top, x + CELL - 1, top + CELL - 1, withAlpha(SLOT_BACK, fade));
                if (!stack.isEmpty()) {
                    g.pose().pushPose();
                    g.pose().translate(x, top - 1, 0.0F);
                    g.pose().scale(0.75F, 0.75F, 1.0F);
                    g.renderItem(stack, 0, 0);
                    g.renderItemDecorations(font, stack, 0, 0);
                    g.pose().popPose();
                }
            }
        }

        /** A line of text at {@code dx} from the left; returns where it ends. */
        private int text(Component component, int colour, int dx, boolean shadow) {
            int w = font.width(component);
            if (g != null) {
                g.drawString(font, component, left + dx, y, withAlpha(colour, fade), shadow);
            }
            width = Math.max(width, dx + w);
            return dx + w;
        }

        /** A wrapped line from {@link Font#split}. */
        private void textLine(net.minecraft.util.FormattedCharSequence line, int colour) {
            if (g != null) {
                g.drawString(font, line, left, y, withAlpha(colour, fade), false);
            }
            width = Math.max(width, font.width(line));
        }

        /** Hearts and food with at most one decimal ("9.5", "10"). */
        private static String trim(float value) {
            return Math.abs(value - Math.round(value)) < 0.05F ? Integer.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.1f", value);
        }
    }
}

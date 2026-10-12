package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.render.ThanosClientView;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosDefence;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The boss bar of the Mad Titan (DESIGN section 15.14.1): his health in the colour of his newest stone, with a darker
 * bar behind it for what he has just lost; a gold tick on the bar where each plate of his armour will break, dark once
 * it has; and under it a row of six sockets, the Gauntlet's. A stone he holds is a lit gem; the next one is a ring that
 * fills while it counts down, with its name and the time; the sixth, the Mind Stone's, is a dark socket outlined in
 * yellow that is never filled. His name turns red when he is enraged. The bar knows what it shows from the Thanos the
 * client knows; for one it does not know (he is far off), it shows the health and the colour alone.
 */
public final class ThanosBossBar {
    /** The three names the bar is given, one for each phase of the fight. */
    public static final String[] KEYS = {"boss.enhanced.thanos", "boss.enhanced.thanos.five", "boss.enhanced.thanos.enraged"};
    private static final String ENRAGED = "boss.enhanced.thanos.enraged";
    private static final int WIDTH = 182;
    private static final int HEIGHT = 7;
    private static final int GEM = 9;
    private static final int GAP = 4;
    private static final int OUTLINE = 0xFF08060E;
    private static final int BACK = 0xFF1B1830;
    private static final int REAR = 0xFF5A1F2A;
    private static final int PLATE = 0xFFD9C48A;
    private static final int PLATE_GONE = 0xFF3A3326;
    private static final int MIND = 0xF5D77A;
    /**
     * The health at which each plate breaks, in the order of the pips: the right pauldron, the left, the breastplate,
     * the helmet.
     */
    private static final float[] PLATE_AT = {0.75F, 0.55F, 0.35F, 0.20F};
    private static final int[] PLATES = {ThanosDefence.R_PAULDRON, ThanosDefence.L_PAULDRON, ThanosDefence.BREASTPLATE, ThanosDefence.HELMET};

    /** What he lost last, so that it can drain slowly behind the bar: per bar. */
    private static final Map<UUID, Float> REAR_BARS = new HashMap<>();
    private static final Map<UUID, Long> REAR_TIMES = new HashMap<>();

    /** Registers the drawer for the three names of the bar. */
    public static void register() {
        for (String key : KEYS) {
            BossBarOverride.register(key, ThanosBossBar::draw);
        }
    }

    /** Forgets what the bars remembered (leaving the world). */
    public static void clear() {
        REAR_BARS.clear();
        REAR_TIMES.clear();
    }

    private static int draw(GuiGraphics graphics, LerpingBossEvent bar, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ThanosEntity boss = mc.player == null ? null : ThanosClientView.nearestBoss(mc.player.position());
        int stones = boss != null ? boss.stones() : stonesOf(bar.getColor());
        StoneKind newest = StoneKind.nth(stones) == null ? StoneKind.POWER : StoneKind.nth(stones);
        int color = newest.color() | 0xFF000000;
        boolean enraged = ENRAGED.equals(BossBarOverride.nameKey(bar.getName()));
        float progress = Mth.clamp(bar.getProgress(), 0.0F, 1.0F);
        float time = mc.level == null ? 0.0F : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);

        // The name, over the bar.
        Component name = bar.getName();
        graphics.drawCenteredString(font, name, x + WIDTH / 2, y - 9, enraged ? 0xFFFF4040 : 0xFFFFFFFF);

        // The frame, the lost health behind, the health in the stone's colour.
        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, OUTLINE);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, BACK);
        float rear = rearOf(bar.getId(), progress, mc);
        if (rear > progress) {
            graphics.fill(x + (int) (WIDTH * progress), y, x + (int) (WIDTH * rear), y + HEIGHT, REAR);
        }
        int filled = (int) (WIDTH * progress);
        if (filled > 0) {
            graphics.fillGradient(x, y, x + filled, y + HEIGHT, lighten(color, 0.35F), darken(color, 0.65F));
            graphics.fill(x, y, x + filled, y + 1, 0x70FFFFFF);
            // A glint that runs along the bar, a little faster the more stones he has.
            int glint = (int) ((time * (1.0F + stones * 0.35F)) % (WIDTH + 40)) - 20;
            int g0 = Math.max(x, x + glint);
            int g1 = Math.min(x + filled, x + glint + 14);
            if (g1 > g0) {
                graphics.fill(g0, y + 1, g1, y + 3, 0x30FFFFFF);
            }
        }
        // Where the plates break.
        int plates = boss != null ? boss.plates() : ThanosDefence.ALL_PLATES;
        for (int i = 0; i < PLATES.length; i++) {
            if ((plates & PLATES[i]) != 0) {
                int tick = x + (int) (WIDTH * PLATE_AT[i]);
                graphics.fill(tick, y - 2, tick + 1, y + HEIGHT + 2, PLATE);
            }
        }

        // The pips of the four plates.
        int pipsY = y + HEIGHT + 3;
        for (int i = 0; i < PLATES.length; i++) {
            boolean on = (plates & PLATES[i]) != 0;
            int px = x + i * 7;
            graphics.fill(px, pipsY, px + 5, pipsY + 5, on ? PLATE : PLATE_GONE);
            graphics.fill(px + 1, pipsY + 1, px + 3, pipsY + 2, on ? 0xFFF3E3B0 : 0xFF2A261C);
        }

        // The six sockets.
        int row = WIDTH / 2 - (6 * GEM + 5 * GAP) / 2;
        int socketsY = y + HEIGHT + 3;
        int nextIn = boss != null ? boss.nextStoneIn() : -1;
        StoneKind coming = stones < StoneKind.granted().size() ? StoneKind.nth(stones + 1) : null;
        for (int i = 0; i < 6; i++) {
            int gx = x + row + i * (GEM + GAP);
            if (i < stones && i < 5) {
                lit(graphics, gx, socketsY, StoneKind.granted().get(i), time + i * 3);
            } else if (i == stones && coming != null && nextIn >= 0) {
                counting(graphics, gx, socketsY, coming, 1.0F - nextIn / (float) Math.max(1, interval()), nextIn, time);
            } else if (i == 5) {
                mind(graphics, gx, socketsY, stones >= 5, time);
            } else {
                dark(graphics, gx, socketsY);
            }
        }
        // The countdown beside the sockets: "Space Stone in 0:42".
        if (coming != null && nextIn >= 0) {
            int seconds = (nextIn + 19) / 20;
            Component label = Component.translatable("boss.enhanced.thanos.next", coming.displayName(),
                    String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60));
            graphics.drawString(font, label, x + row + 6 * (GEM + GAP) + 2, socketsY + 1, coming.color() | 0xFF000000, true);
        }
        return 7 + HEIGHT + 9 + GEM + 8;
    }

    /** The bar's colour tells how many stones he has when the Thanos himself is not known. */
    private static int stonesOf(BossEvent.BossBarColor color) {
        for (StoneKind stone : StoneKind.granted()) {
            if (stone.barColor() == color) {
                return stone.ordinal() + 1;
            }
        }
        return 1;
    }

    private static int interval() {
        try {
            return ThanosBossConfig.STONE_INTERVAL_TICKS.get();
        } catch (IllegalStateException notLoaded) {
            return 1800;
        }
    }

    /**
     * What the lost health looks like: it stays where it was and drains toward the real health at a few percent a
     * second.
     */
    private static float rearOf(UUID id, float progress, Minecraft mc) {
        long now = System.nanoTime();
        float rear = REAR_BARS.getOrDefault(id, progress);
        long before = REAR_TIMES.getOrDefault(id, now);
        float seconds = Mth.clamp((now - before) / 1.0E9F, 0.0F, 0.5F);
        rear = rear > progress ? Math.max(progress, rear - 0.05F * seconds) : progress;
        REAR_BARS.put(id, rear);
        REAR_TIMES.put(id, now);
        return rear;
    }

    /** A stone he holds: a gem in its colour with a soft halo that breathes. */
    private static void lit(GuiGraphics graphics, int x, int y, StoneKind stone, float time) {
        int color = stone.color() | 0xFF000000;
        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.12F);
        graphics.fill(x - 2, y - 2, x + GEM + 2, y + GEM + 2, withAlpha(stone.color(), 0.10F + 0.10F * pulse));
        graphics.fill(x - 1, y - 1, x + GEM + 1, y + GEM + 1, OUTLINE);
        graphics.fillGradient(x, y, x + GEM, y + GEM, lighten(color, 0.4F), darken(color, 0.75F));
        graphics.fill(x + 2, y + 2, x + GEM - 3, y + GEM - 3, withAlpha(stone.core(), 0.55F + 0.3F * pulse));
        graphics.fill(x + 1, y + 1, x + 3, y + 2, 0xB0FFFFFF);
    }

    /** The next stone: a ring that fills from below as the time runs out, brighter in its last ten seconds. */
    private static void counting(GuiGraphics graphics, int x, int y, StoneKind stone, float filled, int nextIn, float time) {
        graphics.fill(x - 1, y - 1, x + GEM + 1, y + GEM + 1, OUTLINE);
        graphics.fill(x, y, x + GEM, y + GEM, 0xFF14121E);
        int height = (int) ((GEM - 2) * Mth.clamp(filled, 0.0F, 1.0F));
        if (height > 0) {
            graphics.fill(x + 1, y + GEM - 1 - height, x + GEM - 1, y + GEM - 1, withAlpha(stone.color(), 0.55F));
        }
        // The ring.
        float pulse = nextIn <= 200 ? 0.6F + 0.4F * Mth.sin(time * 0.35F) : 1.0F;
        int ring = withAlpha(stone.color(), 0.85F * pulse);
        graphics.fill(x, y, x + GEM, y + 1, ring);
        graphics.fill(x, y + GEM - 1, x + GEM, y + GEM, ring);
        graphics.fill(x, y, x + 1, y + GEM, ring);
        graphics.fill(x + GEM - 1, y, x + GEM, y + GEM, ring);
        if (nextIn <= 200) {
            graphics.fill(x - 2, y - 2, x + GEM + 2, y + GEM + 2, withAlpha(stone.color(), 0.14F * pulse));
        }
    }

    /** A socket that is not lit and is not next: a dark gem. */
    private static void dark(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + GEM + 1, y + GEM + 1, OUTLINE);
        graphics.fill(x, y, x + GEM, y + GEM, 0xFF15131C);
        graphics.fill(x + 2, y + 2, x + 4, y + 3, 0x30FFFFFF);
    }

    /** The Mind Stone's socket: dark, outlined in yellow, never filled; it glimmers now and then once he holds five. */
    private static void mind(GuiGraphics graphics, int x, int y, boolean five, float time) {
        float phase = time % 400.0F;
        float glimmer = five && phase < 20.0F ? Mth.sin(Mth.PI * phase / 20.0F) : 0.0F;
        graphics.fill(x - 1, y - 1, x + GEM + 1, y + GEM + 1, OUTLINE);
        graphics.fill(x, y, x + GEM, y + GEM, 0xFF15131C);
        int outline = withAlpha(MIND, 0.5F + 0.5F * glimmer);
        graphics.fill(x, y, x + GEM, y + 1, outline);
        graphics.fill(x, y + GEM - 1, x + GEM, y + GEM, outline);
        graphics.fill(x, y, x + 1, y + GEM, outline);
        graphics.fill(x + GEM - 1, y, x + GEM, y + GEM, outline);
        if (glimmer > 0.05F) {
            graphics.fill(x + 3, y + 3, x + GEM - 3, y + GEM - 3, withAlpha(MIND, 0.7F * glimmer));
            graphics.fill(x - 2, y - 2, x + GEM + 2, y + GEM + 2, withAlpha(MIND, 0.12F * glimmer));
        }
    }

    private static int withAlpha(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }

    private static int lighten(int argb, float amount) {
        return mix(argb, 0xFFFFFF, amount);
    }

    private static int darken(int argb, float amount) {
        return mix(argb, 0x000000, 1.0F - amount);
    }

    private static int mix(int argb, int target, float k) {
        int r = (int) Mth.lerp(k, (argb >> 16) & 0xFF, (target >> 16) & 0xFF);
        int g = (int) Mth.lerp(k, (argb >> 8) & 0xFF, (target >> 8) & 0xFF);
        int b = (int) Mth.lerp(k, argb & 0xFF, target & 0xFF);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private ThanosBossBar() {
    }
}

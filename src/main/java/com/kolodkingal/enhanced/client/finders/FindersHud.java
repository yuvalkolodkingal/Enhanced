package com.kolodkingal.enhanced.client.finders;

import com.kolodkingal.enhanced.client.CerebroView;
import com.kolodkingal.enhanced.finders.LocatorRoutes;
import com.kolodkingal.enhanced.finders.LocatorState;
import com.kolodkingal.enhanced.network.CerebroPayload;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The finders' HUD (3.7, lane L5; the plan's "finders region of the stone HUD", a layer of its own): while a Stone
 * Locator is in a hand, one line over the hotbar with the stone's gem and colour, "Soul Stone · 810 blocks ·
 * north-east", or what it says instead ("Searching...", "Too far: ...", "Someone else holds the Time Stone.", "You have
 * all six..."), and under it the realm's words when it points to the way to another realm ("beyond the Bifrost") or
 * spins. While the amplified Cerebro reports, the stone sites it marks are listed under the minds (top right).
 */
public class FindersHud implements LayeredDraw.Layer {
    /** The bottom of the line's box over the bottom of the screen (just above the action bar). */
    static final int BOTTOM = 76;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) {
            return;
        }
        ItemStack locator = held(player);
        if (locator != null) {
            drawLocator(graphics, mc.font, player, LocatorClient.state(locator));
        }
        if (CerebroView.amplified()) {
            drawSites(graphics, mc.font, player);
        }
    }

    @Nullable
    private static ItemStack held(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.STONE_LOCATOR.get())) {
                return stack;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Locator's line

    private static void drawLocator(GuiGraphics graphics, Font font, LocalPlayer player, LocatorState state) {
        StoneKind kind = state.pointing().orElse(state.filter().orElse(null));
        LocatorRoutes.Realm realm = LocatorRoutes.realm(player.level().dimension());
        Component line = line(state, kind, player);
        Component words = null;
        if (kind != null) {
            String key = LocatorRoutes.message(realm, kind, state.status());
            if (key != null && here(state, player) != null) {
                words = Component.translatable(key, kind.displayName());
            } else if (key != null) {
                line = Component.translatable(key, kind.displayName());
            }
        }
        int rgb = state.status() == LocatorState.HAS_ALL ? 0xFFFFFF : kind != null ? kind.color()
                : state.tuned().map(StoneKind::color).orElse(0xE8E0C8);
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int width = Math.max(font.width(line), words == null ? 0 : font.width(words)) + 22;
        int x = (w - width) / 2;
        int height = words == null ? 17 : 27;
        int y = h - BOTTOM - height + 3;
        graphics.fill(x - 3, y - 3, x + width + 3, y + height - 3, 0x90000000);
        graphics.renderOutline(x - 3, y - 3, width + 6, height, 0xA0000000 | rgb);
        ItemStack icon = kind != null ? new ItemStack(ModItems.stone(kind))
                : new ItemStack(ModItems.STONE_LOCATOR.get());
        graphics.renderItem(icon, x, y - 2);
        graphics.drawString(font, line, x + 20, y + 2, 0xFF000000 | rgb);
        if (words != null) {
            graphics.drawString(font, words.copy().withStyle(ChatFormatting.GRAY), x + 20, y + 12, 0xFFFFFFFF);
        }
    }

    /** The first line: where, how far, which way; or the status. */
    private static Component line(LocatorState state, @Nullable StoneKind kind, LocalPlayer player) {
        BlockPos target = here(state, player);
        if (target != null && kind != null) {
            return Component.translatable("hud.enhanced.locator.found", kind.displayName(), state.distance(),
                    Component.translatable("hud.enhanced.locator.dir." + direction(player.position(), target)));
        }
        return switch (state.status()) {
            case LocatorState.HAS_ALL -> Component.translatable("message.enhanced.locator.all_six");
            case LocatorState.SOMEONE_ELSE -> Component.translatable("message.enhanced.locator.someone_else",
                    kind == null ? Component.literal("?") : kind.displayName());
            case LocatorState.TOO_FAR -> Component.translatable("message.enhanced.locator.too_far", state.distance());
            case LocatorState.SEARCHING -> kind == null ? Component.translatable("hud.enhanced.locator.searching")
                    : Component.translatable("hud.enhanced.locator.searching_for", kind.displayName());
            default -> kind == null ? Component.translatable("item.enhanced.stone_locator") : kind.displayName();
        };
    }

    /** The target when it is in the player's realm, else null. */
    @Nullable
    private static BlockPos here(LocatorState state, LocalPlayer player) {
        return state.target().filter(t -> t.dimension() == player.level().dimension()).map(GlobalPos::pos)
                .orElse(null);
    }

    /** The compass word from {@code from} toward {@code to}: n, ne, e, se, s, sw, w, nw. */
    public static String direction(Vec3 from, BlockPos to) {
        double dx = to.getX() + 0.5 - from.x;
        double dz = to.getZ() + 0.5 - from.z;
        double degrees = Math.toDegrees(Math.atan2(dx, -dz)); // 0 north, 90 east
        String[] words = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return words[Math.floorMod((int) Math.round(degrees / 45.0), 8)];
    }

    // ------------------------------------------------------------------------------------------------------------
    // Cerebro's stone sites

    private static void drawSites(GuiGraphics graphics, Font font, LocalPlayer player) {
        List<CerebroPayload.Site> sites = CerebroView.sites();
        if (sites.isEmpty()) {
            return;
        }
        int w = graphics.guiWidth();
        int y = 6 + 12 + 10 * Math.min(8, CerebroView.minds().size()) + 6;
        Component title = Component.translatable("hud.enhanced.cerebro.sites").withStyle(ChatFormatting.BOLD);
        graphics.drawString(font, title, w - font.width(title) - 8, y, 0xFFE8EEFF);
        y += 12;
        StoneKind[] kinds = StoneKind.values();
        for (CerebroPayload.Site site : sites) {
            StoneKind kind = kinds[Math.floorMod(site.kind(), kinds.length)];
            Vec3 d = Vec3.atCenterOf(site.pos()).subtract(player.position());
            double angle = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-d.x, d.z)) - player.getYRot());
            String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
            String arrow = arrows[Math.floorMod((int) Math.round(angle / 45.0), 8)];
            Component line = Component.literal(arrow + " ◆ ").append(Component.translatable(
                    "hud.enhanced.cerebro.site", kind.displayName(), Math.round(Math.hypot(d.x, d.z))));
            graphics.drawString(font, line, w - font.width(line) - 8, y, 0xFF000000 | kind.color());
            y += 10;
        }
    }
}

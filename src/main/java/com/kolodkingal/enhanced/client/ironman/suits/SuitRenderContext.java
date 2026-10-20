package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ironman.SuitKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Which player's suit is being drawn right now (3.8, PLAN 7), so the suit models can draw what the plain {@code
 * SuitModel.renderPart} call cannot say: the nanite wave's segments, the extras the synced state calls for (blades,
 * shields, the cannon, the Mark 85's flaps, the Mark 3's pods). Set before every player is drawn (third person, the
 * Hulkbuster's renderer swap) and before his first-person arm, at the highest priority; it holds for the frame and that
 * player only, so a stale one never leaks into another draw.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class SuitRenderContext {
    @Nullable
    private static AbstractClientPlayer player;
    private static float partialTick;
    private static boolean firstPerson;
    private static long frame;
    private static long setAt = -1L;
    @Nullable
    private static SuitLook look;

    /** The look of the suit {@code kind} of the player being drawn now, or null when no player of that suit is. */
    @Nullable
    static SuitLook look(SuitKind kind) {
        if (player == null || setAt != frame) {
            return null;
        }
        if (look == null) {
            look = SuitLook.of(player, partialTick);
            if (look == null) {
                player = null;
                return null;
            }
        }
        return look.kind == kind ? look : null;
    }

    /** Whether the draw now is a first-person arm. */
    static boolean firstPerson() {
        return firstPerson && player != null && setAt == frame;
    }

    private static void begin(AbstractClientPlayer who, float pt, boolean arm) {
        firstPerson = arm;
        player = who;
        partialTick = pt;
        look = null;
        setAt = frame;
    }

    private static void end() {
        player = null;
        look = null;
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        frame++;
        end();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer p) {
            begin(p, event.getPartialTick(), false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPlayerPost(RenderPlayerEvent.Post event) {
        end();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArm(RenderArmEvent event) {
        begin(event.getPlayer(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), true);
    }

    private SuitRenderContext() {
    }
}

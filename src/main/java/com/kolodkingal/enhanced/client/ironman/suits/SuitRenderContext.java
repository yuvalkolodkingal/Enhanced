package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderArmEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderFrameEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderPlayerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Which player's suit is being drawn right now (3.8, PLAN 7), so the suit models can draw what the plain {@code
 * SuitModel.renderPart} call cannot say: the nanite wave's segments, the extras the synced state calls for (blades,
 * shields, the cannon, the Mark 85's flaps, the Mark 3's pods). Set before every player is drawn (third person, the
 * Hulkbuster's renderer swap) and before his first-person arm, at the highest priority; it holds for the frame and that
 * player only, so a stale one never leaks into another draw.
 */
@Subscriber(side = Side.CLIENT)
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

    @Subscribe
    public static void onFrame(RenderFrameEvent.Pre event) {
        frame++;
        end();
    }

    @Subscribe(priority = Priority.HIGHEST, receiveCanceled = true)
    public static void onPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer p) {
            begin(p, event.getPartialTick(), false);
        }
    }

    @Subscribe(priority = Priority.LOWEST, receiveCanceled = true)
    public static void onPlayerPost(RenderPlayerEvent.Post event) {
        end();
    }

    @Subscribe(priority = Priority.HIGHEST)
    public static void onArm(RenderArmEvent event) {
        begin(event.getPlayer(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), true);
    }

    private SuitRenderContext() {
    }
}

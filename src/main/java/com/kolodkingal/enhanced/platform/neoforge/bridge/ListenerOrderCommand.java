package com.kolodkingal.enhanced.platform.neoforge.bridge;

import com.kolodkingal.enhanced.platform.event.RegisterCommandsEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /enhanced debug listener_order}: records the order of the mod's listeners on this side into the recording
 * ({@link ListenerOrderRecorder}). Only in development runs (or with {@code -Denhanced.debug=true}), for operators.
 * (It is a {@code @Subscribe} listener itself: the common-event path from end to end.)
 */
@Subscriber
public final class ListenerOrderCommand {
    private ListenerOrderCommand() {
    }

    @Subscribe
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!ListenerOrderRecorder.enabled()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("enhanced")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("listener_order")
                                .executes(ctx -> {
                                    ListenerOrderRecorder.Result result =
                                            ListenerOrderRecorder.write(ListenerOrderRecorder.file());
                                    ctx.getSource().sendSuccess(() -> Component.literal("Recorded "
                                            + result.listeners() + " listeners of " + result.events() + " events ("
                                            + result.section() + ") to " + result.file()), false);
                                    return result.listeners();
                                }))));
    }
}

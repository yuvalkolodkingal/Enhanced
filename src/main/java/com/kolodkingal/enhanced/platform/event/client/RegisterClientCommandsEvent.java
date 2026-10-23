package com.kolodkingal.enhanced.platform.event.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

/** The client's own commands are being registered. */
public class RegisterClientCommandsEvent {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final CommandBuildContext buildContext;

    public RegisterClientCommandsEvent(
            CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        this.dispatcher = dispatcher;
        this.buildContext = buildContext;
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return dispatcher;
    }

    public CommandBuildContext getBuildContext() {
        return buildContext;
    }
}

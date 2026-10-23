package com.kolodkingal.enhanced.platform.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** The server's commands are being registered. */
public class RegisterCommandsEvent {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final Commands.CommandSelection commandSelection;
    private final CommandBuildContext buildContext;

    public RegisterCommandsEvent(
            CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection commandSelection,
            CommandBuildContext buildContext) {
        this.dispatcher = dispatcher;
        this.commandSelection = commandSelection;
        this.buildContext = buildContext;
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return dispatcher;
    }

    public Commands.CommandSelection getCommandSelection() {
        return commandSelection;
    }

    public CommandBuildContext getBuildContext() {
        return buildContext;
    }
}

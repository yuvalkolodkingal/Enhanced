package com.kolodkingal.enhanced.mixin.client;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The cause vanilla's death screen was given, which Death's own screen shows (DeathMeetingClient). */
@Mixin(DeathScreen.class)
public interface DeathScreenAccessor {
    @Accessor("causeOfDeath")
    Component enhanced$getCauseOfDeath();
}

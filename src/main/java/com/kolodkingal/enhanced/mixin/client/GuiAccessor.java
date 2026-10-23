package com.kolodkingal.enhanced.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The action bar's line and how long it still shows: Build Magic's hint strip moves aside, never under it. Was an
 * access transformer line before the port.
 */
@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("overlayMessageString")
    @Nullable
    Component enhanced$getOverlayMessageString();

    @Accessor("overlayMessageTime")
    int enhanced$getOverlayMessageTime();
}

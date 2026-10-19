package com.kolodkingal.enhanced.hydra;

import net.minecraft.server.level.ServerPlayer;

/**
 * A creature whose own weapon fires when Professor X steers it and clicks (3.6): {@code MindLink} asks this before its
 * generic strike, so a Hydra soldier he took over fires his rifle (lane L8 implements it on the soldiers). The body
 * already looks where Xavier looks.
 */
public interface SteeredShooter {
    /** Xavier, inside this body, clicked: it fires its own weapon where it looks. */
    void steeredAttack(ServerPlayer xavier);
}

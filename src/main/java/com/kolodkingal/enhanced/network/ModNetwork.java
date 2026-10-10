package com.kolodkingal.enhanced.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Client → server messages (ability keys, Change Look, Hex Control) and server → client messages (projectiles slowed
 * by Quicksilver Time); everything else the client must know (powers, Hexes) travels as synced data attachments.
 */
public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(AbilityKeyPayload.TYPE, AbilityKeyPayload.STREAM_CODEC, AbilityKeyPayload::handle);
        registrar.playToServer(HexActionPayload.TYPE, HexActionPayload.STREAM_CODEC, HexActionPayload::handle);
        registrar.playToServer(ChangeLookPayload.TYPE, ChangeLookPayload.STREAM_CODEC, ChangeLookPayload::handle);
        registrar.playToClient(SlowedProjectilePayload.TYPE, SlowedProjectilePayload.STREAM_CODEC, SlowedProjectilePayload::handle);
        registrar.playToClient(ThorFxPayload.TYPE, ThorFxPayload.STREAM_CODEC, ThorFxPayload::handle);
    }

    private ModNetwork() {
    }
}

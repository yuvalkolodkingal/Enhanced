package com.kolodkingal.enhanced.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Client → server messages (ability keys, Change Look, Hex Control, the Bifrost screen, the Witches' Road choice) and
 * server → client messages (projectiles slowed by Quicksilver Time, Thor's lightning and Bifrost, Hex Bolts, the Witches'
 * Road screen); everything else the client must know (powers, Hexes, Bifrost places) travels as synced data attachments.
 */
public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(AbilityKeyPayload.TYPE, AbilityKeyPayload.STREAM_CODEC, AbilityKeyPayload::handle);
        registrar.playToServer(HexActionPayload.TYPE, HexActionPayload.STREAM_CODEC, HexActionPayload::handle);
        registrar.playToServer(ChangeLookPayload.TYPE, ChangeLookPayload.STREAM_CODEC, ChangeLookPayload::handle);
        registrar.playToServer(BifrostActionPayload.TYPE, BifrostActionPayload.STREAM_CODEC, BifrostActionPayload::handle);
        registrar.playToServer(RoadChoicePayload.TYPE, RoadChoicePayload.STREAM_CODEC, RoadChoicePayload::handle);
        registrar.playToClient(SlowedProjectilePayload.TYPE, SlowedProjectilePayload.STREAM_CODEC, SlowedProjectilePayload::handle);
        registrar.playToClient(ThorFxPayload.TYPE, ThorFxPayload.STREAM_CODEC, ThorFxPayload::handle);
        registrar.playToClient(HexBoltPayload.TYPE, HexBoltPayload.STREAM_CODEC, HexBoltPayload::handle);
        registrar.playToClient(OpenRoadPayload.TYPE, OpenRoadPayload.STREAM_CODEC, OpenRoadPayload::handle);
    }

    private ModNetwork() {
    }
}

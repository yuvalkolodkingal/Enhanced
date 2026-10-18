package com.kolodkingal.enhanced.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Client → server messages (ability keys, Change Look, Hex Control, the Bifrost screen, the Witches' Road choice, Build
 * Magic's casts and actions) and server → client messages (projectiles slowed by Quicksilver Time, Thor's lightning and
 * Bifrost, Hex Bolts, the Witches' Road screen, the effects of the Phoenix, Spider-Man, Thanos, Ultron and Apocalypse,
 * Build Magic's verdicts, effects and pockets, and Magneto's Magnetic Pull); everything else the client must know (powers,
 * Hexes, Bifrost places, a builder's meter and box) travels as synced data attachments.
 */
public final class ModNetwork {
    /**
     * The version of the messages and synced data: a client and a server with different versions can't play together.
     * Bumped once for all of 3.1, once for 3.2 (the Witches' Road screen got a mode and the trials' progress), and once for
     * all of 3.3.
     */
    public static final String PROTOCOL_VERSION = "7";

    public static void register(RegisterPayloadHandlersEvent event) {
        // (2.2 renumbered Thor's effects and added the Chaos Shield's state to the power data; 2.3 added the order to
        // the Vision of the Hex to every Hex; 3.0 widened the momentary states of the power data to 64 bits and added
        // the messages of the five new heroes; 3.1 added Build Magic's five messages and two synced attachments, and the
        // Magnetic Pull's message.)
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(AbilityKeyPayload.TYPE, AbilityKeyPayload.STREAM_CODEC, AbilityKeyPayload::handle);
        registrar.playToServer(HexActionPayload.TYPE, HexActionPayload.STREAM_CODEC, HexActionPayload::handle);
        registrar.playToServer(ChangeLookPayload.TYPE, ChangeLookPayload.STREAM_CODEC, ChangeLookPayload::handle);
        registrar.playToServer(BifrostActionPayload.TYPE, BifrostActionPayload.STREAM_CODEC, BifrostActionPayload::handle);
        registrar.playToServer(RoadChoicePayload.TYPE, RoadChoicePayload.STREAM_CODEC, RoadChoicePayload::handle);
        registrar.playToClient(SlowedProjectilePayload.TYPE, SlowedProjectilePayload.STREAM_CODEC, SlowedProjectilePayload::handle);
        registrar.playToClient(ThorFxPayload.TYPE, ThorFxPayload.STREAM_CODEC, ThorFxPayload::handle);
        registrar.playToClient(HexBoltPayload.TYPE, HexBoltPayload.STREAM_CODEC, HexBoltPayload::handle);
        registrar.playToClient(OpenRoadPayload.TYPE, OpenRoadPayload.STREAM_CODEC, OpenRoadPayload::handle);
        // The heroes of 3.0 (their handlers are filled in by the lane that builds each hero).
        registrar.playToClient(MindReadPayload.TYPE, MindReadPayload.STREAM_CODEC, MindReadPayload::handle);
        registrar.playToClient(PhoenixBurstPayload.TYPE, PhoenixBurstPayload.STREAM_CODEC, PhoenixBurstPayload::handle);
        registrar.playToClient(SpiderFxPayload.TYPE, SpiderFxPayload.STREAM_CODEC, SpiderFxPayload::handle);
        registrar.playToClient(SpiderSensePayload.TYPE, SpiderSensePayload.STREAM_CODEC, SpiderSensePayload::handle);
        registrar.playToClient(ThanosFxPayload.TYPE, ThanosFxPayload.STREAM_CODEC, ThanosFxPayload::handle);
        registrar.playToClient(UltronFxPayload.TYPE, UltronFxPayload.STREAM_CODEC, UltronFxPayload::handle);
        registrar.playToClient(SokoviaLiftPayload.TYPE, SokoviaLiftPayload.STREAM_CODEC, SokoviaLiftPayload::handle);
        registrar.playToClient(ApocalypseFxPayload.TYPE, ApocalypseFxPayload.STREAM_CODEC, ApocalypseFxPayload::handle);
        // 3.1: Build Magic and the Magnetic Pull (their handlers call stubs until the lanes BS, BC and MAG fill them).
        registrar.playToServer(BuildCastPayload.TYPE, BuildCastPayload.STREAM_CODEC, BuildCastPayload::handle);
        registrar.playToServer(BuildActionPayload.TYPE, BuildActionPayload.STREAM_CODEC, BuildActionPayload::handle);
        registrar.playToClient(BuildVerdictPayload.TYPE, BuildVerdictPayload.STREAM_CODEC, BuildVerdictPayload::handle);
        registrar.playToClient(BuildFxPayload.TYPE, BuildFxPayload.STREAM_CODEC, BuildFxPayload::handle);
        registrar.playToClient(BuildPocketPayload.TYPE, BuildPocketPayload.STREAM_CODEC, BuildPocketPayload::handle);
        registrar.playToClient(MagnetPullPayload.TYPE, MagnetPullPayload.STREAM_CODEC, MagnetPullPayload::handle);
        // 3.2: the Book of the Vishanti's screen and the wards it raises.
        registrar.playToServer(VishantiActionPayload.TYPE, VishantiActionPayload.STREAM_CODEC, VishantiActionPayload::handle);
        registrar.playToClient(WardSyncPayload.TYPE, WardSyncPayload.STREAM_CODEC, WardSyncPayload::handle);
        // 3.2: continuous beams (Billy's Hex Beam, and any other source of one).
        registrar.playToClient(BeamPayload.TYPE, BeamPayload.STREAM_CODEC, BeamPayload::handle);
        // 3.3: the music of the Witches' Road (the stage the player is at).
        registrar.playToClient(RoadMusicPayload.TYPE, RoadMusicPayload.STREAM_CODEC, RoadMusicPayload::handle);
        // 3.3: Cyclops's blasts (his beams are BeamPayloads).
        registrar.playToClient(CyclopsFxPayload.TYPE, CyclopsFxPayload.STREAM_CODEC, CyclopsFxPayload::handle);
        // 3.3: the Hero Creator's buttons (the custom hero's definition and state travel as synced attachments).
        registrar.playToServer(HeroCreatorPayload.TYPE, HeroCreatorPayload.STREAM_CODEC, HeroCreatorPayload::handle);
        // 3.3: Professor X (his screen and his mind in another body, the thoughts he and Billy hear, Cerebro).
        registrar.playToClient(XavierPayload.TYPE, XavierPayload.STREAM_CODEC, XavierPayload::handle);
        registrar.playToServer(XavierInputPayload.TYPE, XavierInputPayload.STREAM_CODEC, XavierInputPayload::handle);
        registrar.playToServer(XavierChoicePayload.TYPE, XavierChoicePayload.STREAM_CODEC, XavierChoicePayload::handle);
        registrar.playToClient(ThoughtsPayload.TYPE, ThoughtsPayload.STREAM_CODEC, ThoughtsPayload::handle);
        registrar.playToClient(CerebroPayload.TYPE, CerebroPayload.STREAM_CODEC, CerebroPayload::handle);
    }

    private ModNetwork() {
    }
}

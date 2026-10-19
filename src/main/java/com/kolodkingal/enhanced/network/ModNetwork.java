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
     * Bumped once for all of 3.1, once for 3.2 (the Witches' Road screen got a mode and the trials' progress), once for
     * all of 3.3, once for 3.4 (a custom hero's definition, format 2, and his state's cast marker), once for 3.5
     * (Asgard), and once for 3.6 (the Infinity Stones, Doctor Strange, the Hulk and Hydra).
     */
    public static final String PROTOCOL_VERSION = "10";
    /**
     * 3.5: the version of the world's music messages, which have a channel of their own: optional, so a client without them
     * (an older version of the mod) plays with the server all the same and simply hears the built-in music; the server only
     * sends them to a client that has the channel. Bumped only when these three messages change.
     */
    public static final String MUSIC_VERSION = "1";

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
        // 3.3: Agatha's effects (the Rune Trap, the Power Drain, her shield breaking).
        registrar.playToClient(AgathaFxPayload.TYPE, AgathaFxPayload.STREAM_CODEC, AgathaFxPayload::handle);
        // 3.5 (Asgard): protocol 9. The Bifrost's actions 4 (Asgard) and 5 (Heimdall's Send) and ThorFxPayload's
        // BIFROST_DEPART travel in the payloads above.
        registrar.playToClient(AsgardTalkPayload.Open.TYPE, AsgardTalkPayload.Open.STREAM_CODEC, AsgardTalkPayload.Open::handle);
        registrar.playToServer(AsgardTalkPayload.Choose.TYPE, AsgardTalkPayload.Choose.STREAM_CODEC, AsgardTalkPayload.Choose::handle);
        registrar.playToClient(HeimdallSightPayload.TYPE, HeimdallSightPayload.STREAM_CODEC, HeimdallSightPayload::handle);
        registrar.playToClient(AsgardFxPayload.TYPE, AsgardFxPayload.STREAM_CODEC, AsgardFxPayload::handle);
        // 3.6 (the Infinity Stones, Doctor Strange, the Hulk and Hydra): protocol 10. The handlers call the lanes'
        // hooks.
        registrar.playToServer(StoneActionPayload.TYPE, StoneActionPayload.STREAM_CODEC, StoneActionPayload::handle);
        registrar.playToClient(RealmScreenPayload.TYPE, RealmScreenPayload.STREAM_CODEC, RealmScreenPayload::handle);
        registrar.playToClient(StoneFxPayload.TYPE, StoneFxPayload.STREAM_CODEC, StoneFxPayload::handle);
        registrar.playToClient(StrangeFxPayload.TYPE, StrangeFxPayload.STREAM_CODEC, StrangeFxPayload::handle);
        registrar.playToServer(SlingRingActionPayload.TYPE, SlingRingActionPayload.STREAM_CODEC,
                SlingRingActionPayload::handle);
        registrar.playToClient(HulkFxPayload.TYPE, HulkFxPayload.STREAM_CODEC, HulkFxPayload::handle);
        registrar.playToClient(HydraFxPayload.TYPE, HydraFxPayload.STREAM_CODEC, HydraFxPayload::handle);
        // 3.5: the world's own music (its manifest, a client's request for a song, and the song in pieces), optional.
        PayloadRegistrar music = event.registrar(MUSIC_VERSION).optional();
        music.playToClient(MusicManifestPayload.TYPE, MusicManifestPayload.STREAM_CODEC, MusicManifestPayload::handle);
        music.playToServer(MusicRequestPayload.TYPE, MusicRequestPayload.STREAM_CODEC, MusicRequestPayload::handle);
        music.playToClient(MusicChunkPayload.TYPE, MusicChunkPayload.STREAM_CODEC, MusicChunkPayload::handle);
    }

    private ModNetwork() {
    }
}

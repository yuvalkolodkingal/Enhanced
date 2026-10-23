package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.server.level.ServerPlayer;

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
     * (Asgard), once for 3.6 (the Infinity Stones, Doctor Strange, the Hulk and Hydra), once for 3.7 (Wolverine,
     * Deadpool, the healing factor, the X-Men lower lab, the amplified Cerebro and the Stone Locator), and once for 3.8
     * (the serum, Captain America, Red Hulk, Iron Man, Falcon, the tower's armoury, Eternity and Love; the momentary
     * states in two words).
     */
    public static final String PROTOCOL_VERSION = "13";
    /**
     * 3.5: the version of the world's music messages, which have a channel of their own: optional, so a client without them
     * (an older version of the mod) plays with the server all the same and simply hears the built-in music; the server only
     * sends them to a client that has the channel. Bumped only when these three messages change.
     */
    public static final String MUSIC_VERSION = "1";

    public static void register() {
        // (2.2 renumbered Thor's effects and added the Chaos Shield's state to the power data; 2.3 added the order to
        // the Vision of the Hex to every Hex; 3.0 widened the momentary states of the power data to 64 bits and added
        // the messages of the five new heroes; 3.1 added Build Magic's five messages and two synced attachments, and the
        // Magnetic Pull's message.)
        Net.Channel registrar = Net.channel(PROTOCOL_VERSION);
        registrar.serverbound(AbilityKeyPayload.TYPE, AbilityKeyPayload.STREAM_CODEC, AbilityKeyPayload::handle);
        registrar.serverbound(HexActionPayload.TYPE, HexActionPayload.STREAM_CODEC, HexActionPayload::handle);
        registrar.serverbound(ChangeLookPayload.TYPE, ChangeLookPayload.STREAM_CODEC, ChangeLookPayload::handle);
        registrar.serverbound(BifrostActionPayload.TYPE, BifrostActionPayload.STREAM_CODEC, BifrostActionPayload::handle);
        registrar.serverbound(RoadChoicePayload.TYPE, RoadChoicePayload.STREAM_CODEC, RoadChoicePayload::handle);
        registrar.clientbound(SlowedProjectilePayload.TYPE, SlowedProjectilePayload.STREAM_CODEC, SlowedProjectilePayload::handle);
        registrar.clientbound(ThorFxPayload.TYPE, ThorFxPayload.STREAM_CODEC, ThorFxPayload::handle);
        registrar.clientbound(HexBoltPayload.TYPE, HexBoltPayload.STREAM_CODEC, HexBoltPayload::handle);
        registrar.clientbound(OpenRoadPayload.TYPE, OpenRoadPayload.STREAM_CODEC, OpenRoadPayload::handle);
        // The heroes of 3.0 (their handlers are filled in by the lane that builds each hero).
        registrar.clientbound(MindReadPayload.TYPE, MindReadPayload.STREAM_CODEC, MindReadPayload::handle);
        registrar.clientbound(PhoenixBurstPayload.TYPE, PhoenixBurstPayload.STREAM_CODEC, PhoenixBurstPayload::handle);
        registrar.clientbound(SpiderFxPayload.TYPE, SpiderFxPayload.STREAM_CODEC, SpiderFxPayload::handle);
        registrar.clientbound(SpiderSensePayload.TYPE, SpiderSensePayload.STREAM_CODEC, SpiderSensePayload::handle);
        registrar.clientbound(ThanosFxPayload.TYPE, ThanosFxPayload.STREAM_CODEC, ThanosFxPayload::handle);
        registrar.clientbound(UltronFxPayload.TYPE, UltronFxPayload.STREAM_CODEC, UltronFxPayload::handle);
        registrar.clientbound(SokoviaLiftPayload.TYPE, SokoviaLiftPayload.STREAM_CODEC, SokoviaLiftPayload::handle);
        registrar.clientbound(ApocalypseFxPayload.TYPE, ApocalypseFxPayload.STREAM_CODEC, ApocalypseFxPayload::handle);
        // 3.1: Build Magic and the Magnetic Pull (their handlers call stubs until the lanes BS, BC and MAG fill them).
        registrar.serverbound(BuildCastPayload.TYPE, BuildCastPayload.STREAM_CODEC, BuildCastPayload::handle);
        registrar.serverbound(BuildActionPayload.TYPE, BuildActionPayload.STREAM_CODEC, BuildActionPayload::handle);
        registrar.clientbound(BuildVerdictPayload.TYPE, BuildVerdictPayload.STREAM_CODEC, BuildVerdictPayload::handle);
        registrar.clientbound(BuildFxPayload.TYPE, BuildFxPayload.STREAM_CODEC, BuildFxPayload::handle);
        registrar.clientbound(BuildPocketPayload.TYPE, BuildPocketPayload.STREAM_CODEC, BuildPocketPayload::handle);
        registrar.clientbound(MagnetPullPayload.TYPE, MagnetPullPayload.STREAM_CODEC, MagnetPullPayload::handle);
        // 3.2: the Book of the Vishanti's screen and the wards it raises.
        registrar.serverbound(VishantiActionPayload.TYPE, VishantiActionPayload.STREAM_CODEC, VishantiActionPayload::handle);
        registrar.clientbound(WardSyncPayload.TYPE, WardSyncPayload.STREAM_CODEC, WardSyncPayload::handle);
        // 3.2: continuous beams (Billy's Hex Beam, and any other source of one).
        registrar.clientbound(BeamPayload.TYPE, BeamPayload.STREAM_CODEC, BeamPayload::handle);
        // 3.3: the music of the Witches' Road (the stage the player is at).
        registrar.clientbound(RoadMusicPayload.TYPE, RoadMusicPayload.STREAM_CODEC, RoadMusicPayload::handle);
        // 3.3: Cyclops's blasts (his beams are BeamPayloads).
        registrar.clientbound(CyclopsFxPayload.TYPE, CyclopsFxPayload.STREAM_CODEC, CyclopsFxPayload::handle);
        // 3.3: the Hero Creator's buttons (the custom hero's definition and state travel as synced attachments).
        registrar.serverbound(HeroCreatorPayload.TYPE, HeroCreatorPayload.STREAM_CODEC, HeroCreatorPayload::handle);
        // 3.3: Professor X (his screen and his mind in another body, the thoughts he and Billy hear, Cerebro).
        registrar.clientbound(XavierPayload.TYPE, XavierPayload.STREAM_CODEC, XavierPayload::handle);
        registrar.serverbound(XavierInputPayload.TYPE, XavierInputPayload.STREAM_CODEC, XavierInputPayload::handle);
        registrar.serverbound(XavierChoicePayload.TYPE, XavierChoicePayload.STREAM_CODEC, XavierChoicePayload::handle);
        registrar.clientbound(ThoughtsPayload.TYPE, ThoughtsPayload.STREAM_CODEC, ThoughtsPayload::handle);
        registrar.clientbound(CerebroPayload.TYPE, CerebroPayload.STREAM_CODEC, CerebroPayload::handle);
        // 3.3: Agatha's effects (the Rune Trap, the Power Drain, her shield breaking).
        registrar.clientbound(AgathaFxPayload.TYPE, AgathaFxPayload.STREAM_CODEC, AgathaFxPayload::handle);
        // 3.5 (Asgard): protocol 9. The Bifrost's actions 4 (Asgard) and 5 (Heimdall's Send) and ThorFxPayload's
        // BIFROST_DEPART travel in the payloads above.
        registrar.clientbound(AsgardTalkPayload.Open.TYPE, AsgardTalkPayload.Open.STREAM_CODEC, AsgardTalkPayload.Open::handle);
        registrar.serverbound(AsgardTalkPayload.Choose.TYPE, AsgardTalkPayload.Choose.STREAM_CODEC, AsgardTalkPayload.Choose::handle);
        registrar.clientbound(HeimdallSightPayload.TYPE, HeimdallSightPayload.STREAM_CODEC, HeimdallSightPayload::handle);
        registrar.clientbound(AsgardFxPayload.TYPE, AsgardFxPayload.STREAM_CODEC, AsgardFxPayload::handle);
        // 3.6 (the Infinity Stones, Doctor Strange, the Hulk and Hydra): protocol 10. The handlers call the lanes'
        // hooks.
        registrar.serverbound(StoneActionPayload.TYPE, StoneActionPayload.STREAM_CODEC, StoneActionPayload::handle);
        registrar.clientbound(RealmScreenPayload.TYPE, RealmScreenPayload.STREAM_CODEC, RealmScreenPayload::handle);
        registrar.clientbound(StoneFxPayload.TYPE, StoneFxPayload.STREAM_CODEC, StoneFxPayload::handle);
        registrar.clientbound(StrangeFxPayload.TYPE, StrangeFxPayload.STREAM_CODEC, StrangeFxPayload::handle);
        registrar.serverbound(SlingRingActionPayload.TYPE, SlingRingActionPayload.STREAM_CODEC,
                SlingRingActionPayload::handle);
        registrar.clientbound(HulkFxPayload.TYPE, HulkFxPayload.STREAM_CODEC, HulkFxPayload::handle);
        registrar.clientbound(HydraFxPayload.TYPE, HydraFxPayload.STREAM_CODEC, HydraFxPayload::handle);
        // 3.7 (Wolverine, Deadpool, the lab, the Locator, the cottage): protocol 11. CerebroPayload gained the
        // dimension of each mind, the amplified flag and the stone sites; StoneFxPayload the kind LOCATOR_TRAIL. The
        // handlers call the lanes' hooks.
        registrar.clientbound(HealingFxPayload.TYPE, HealingFxPayload.STREAM_CODEC, HealingFxPayload::handle);
        registrar.clientbound(WolverineFxPayload.TYPE, WolverineFxPayload.STREAM_CODEC, WolverineFxPayload::handle);
        registrar.clientbound(ScentPayload.TYPE, ScentPayload.STREAM_CODEC, ScentPayload::handle);
        registrar.clientbound(DeadpoolFxPayload.TYPE, DeadpoolFxPayload.STREAM_CODEC, DeadpoolFxPayload::handle);
        registrar.clientbound(LabFxPayload.TYPE, LabFxPayload.STREAM_CODEC, LabFxPayload::handle);
        // 3.7 (lane L8): the meeting with Death (Rio's offer, and the dead player's choice of road).
        registrar.clientbound(DeathMeetingPayload.Offer.TYPE, DeathMeetingPayload.Offer.STREAM_CODEC,
                DeathMeetingPayload.Offer::handle);
        registrar.serverbound(DeathMeetingPayload.Choose.TYPE, DeathMeetingPayload.Choose.STREAM_CODEC,
                DeathMeetingPayload.Choose::handle);
        // 3.8 (the serum, Captain America, Red Hulk, Iron Man, Falcon, the tower, Eternity and Love): protocol 12. The
        // power data's momentary states travel in two words. The handlers call the lanes' hooks.
        registrar.clientbound(SerumScreenPayload.TYPE, SerumScreenPayload.STREAM_CODEC, SerumScreenPayload::handle);
        registrar.serverbound(SerumChoicePayload.TYPE, SerumChoicePayload.STREAM_CODEC, SerumChoicePayload::handle);
        registrar.clientbound(CaptainFxPayload.TYPE, CaptainFxPayload.STREAM_CODEC, CaptainFxPayload::handle);
        registrar.clientbound(RedHulkFxPayload.TYPE, RedHulkFxPayload.STREAM_CODEC, RedHulkFxPayload::handle);
        registrar.clientbound(IronFxPayload.TYPE, IronFxPayload.STREAM_CODEC, IronFxPayload::handle);
        registrar.clientbound(FalconFxPayload.TYPE, FalconFxPayload.STREAM_CODEC, FalconFxPayload::handle);
        registrar.clientbound(TowerFxPayload.TYPE, TowerFxPayload.STREAM_CODEC, TowerFxPayload::handle);
        registrar.clientbound(EternityScreenPayload.TYPE, EternityScreenPayload.STREAM_CODEC,
                EternityScreenPayload::handle);
        registrar.serverbound(EternityWishPayload.TYPE, EternityWishPayload.STREAM_CODEC, EternityWishPayload::handle);
        registrar.clientbound(EternityFxPayload.TYPE, EternityFxPayload.STREAM_CODEC, EternityFxPayload::handle);
        // 3.5: the world's own music (its manifest, a client's request for a song, and the song in pieces), optional.
        Net.Channel music = Net.optionalChannel(MUSIC_VERSION);
        music.clientbound(MusicManifestPayload.TYPE, MusicManifestPayload.STREAM_CODEC, MusicManifestPayload::handle);
        music.serverbound(MusicRequestPayload.TYPE, MusicRequestPayload.STREAM_CODEC, MusicRequestPayload::handle);
        music.clientbound(MusicChunkPayload.TYPE, MusicChunkPayload.STREAM_CODEC, MusicChunkPayload::handle);
    }

    /**
     * Whether the player's client has the optional music channel (a client of an older version of the mod has not): the
     * server sends the music messages only to a client that has it.
     */
    public static boolean hasMusic(ServerPlayer player) {
        return Net.hasChannel(player, MusicManifestPayload.TYPE);
    }

    private ModNetwork() {
    }
}

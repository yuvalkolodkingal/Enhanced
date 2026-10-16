package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Voice of the Road: short lines in the action bar at the great moments of a trial (three to choose from for each, in
 * {@code message.enhanced.road.voice.<event>.<1..3>}), and a title with the trial's name when it begins. Switched off by
 * {@code witches_road.voice}.
 */
public final class RoadVoice {
    /** How many versions of each line there are. */
    public static final int VARIANTS = 3;

    /** The events the Road speaks at. */
    public static final String ENTER = "enter";
    public static final String START = "start";
    public static final String CHECKPOINT = "checkpoint";
    public static final String FALL = "fall";
    public static final String WON = "won";
    public static final String LEAVE = "leave";
    public static final String BACK = "back";
    /** The end of the Road: he has walked it all; the farewell as the power awakens; the word as he steps back into the world. */
    public static final String ENDING = "ending";
    public static final String FAREWELL = "farewell";
    public static final String HOME = "home";

    /** The lang key of one version of a line. */
    public static String key(String event, int variant) {
        return "message.enhanced.road.voice." + event + "." + (1 + Math.floorMod(variant, VARIANTS));
    }

    /** The Road says something; {@code salt} picks the version (the same player hears them in turn). */
    public static void say(ServerPlayer player, String event, int salt) {
        if (WitchesRoadConfig.VOICE.get()) {
            player.displayClientMessage(Component.translatable(key(event, salt)).withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE).withItalic(true)), true);
        }
    }

    /** The big title of a trial that begins: its number and name, and its goal underneath (a title is drawn four times the size of text: keep it short). */
    public static void begin(ServerPlayer player, RoadChallenge challenge, int number) {
        Component title = Component.translatable("message.enhanced.road.trial_title", number, challenge.titleShort())
                .withStyle(s -> s.withColor(WitchesRoad.LIGHT).withBold(true));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("road.enhanced.challenge." + challenge.id() + ".goal").withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE))));
    }

    private RoadVoice() {
    }
}

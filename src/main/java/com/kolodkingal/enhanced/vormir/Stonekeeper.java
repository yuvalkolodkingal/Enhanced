package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.config.SoulStoneConfig;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What the Stonekeeper says (3.6, the plan's 6.3): his lines in chat, in his orange name colour, each with his breathy
 * voice. Right-clicked, he speaks three lines in turn ({@link #lines}): the welcome, the price, and a third for the one
 * who asks (one who carries a soul already; a world whose stone has been taken; a server that gives only pets, or
 * none; 3.6.1, no Bearer: it will wait for you on the pedestal); after a death at his cliff that gave nothing he tells
 * the seeker why ({@link Sacrifice.Outcome#line}, and 3.6.1's plain line, {@link Sacrifice#tell}).
 */
public final class Stonekeeper {
    /** His name's colour: the stone's orange, a shade darker. */
    public static final int NAME_COLOR = 0xE07A1F;
    /** His lines' colour. */
    public static final int LINE_COLOR = 0xF2D2B0;
    /** Ticks between his lines. */
    public static final int LINE_GAP = 40;

    public static final String WELCOME = "npc.enhanced.stonekeeper.welcome";
    public static final String PRICE = "npc.enhanced.stonekeeper.price";
    public static final String CHOOSE = "npc.enhanced.stonekeeper.choose";

    /** The three lines he says to {@code player} when asked (lang keys). */
    public static List<String> lines(ServerPlayer player) {
        return List.of(WELCOME, PRICE, third(player));
    }

    /** The third line: what is true for this player here, now. */
    static String third(ServerPlayer player) {
        Sacrifice.Outcome refused = refusal(player);
        return refused != null && refused.line() != null ? refused.line() : CHOOSE;
    }

    /**
     * Why this player could not be given the stone today whoever he pushed, or null (he could): the stone sleeps, he is
     * in Creative, already has one, the world's has been taken, or no creature he could push would count; last, 3.6.1,
     * {@code NOT_BEARER} for one who could, but may not hold it bare (only the keeper's third line: it will wait for
     * him).
     */
    @Nullable
    static Sacrifice.Outcome refusal(ServerPlayer player) {
        if (!Sacrifice.enabled()) {
            return Sacrifice.Outcome.DISABLED;
        }
        if (Sacrifice.onePer() == SoulStoneConfig.OnePer.WORLD && Sacrifice.refusal(player) != null) {
            return Sacrifice.Outcome.TAKEN;
        }
        if (player.isCreative() || player.isSpectator()) {
            return Sacrifice.Outcome.CREATIVE;
        }
        Sacrifice.Outcome owed = Sacrifice.refusal(player);
        if (owed != null) {
            return owed;
        }
        boolean friends = Sacrifice.playerSacrifice() && player.server.isPvpAllowed();
        if (!friends && !Sacrifice.petSacrifice()) {
            return Sacrifice.Outcome.DISABLED;
        }
        if (!friends) {
            return Sacrifice.Outcome.PLAYERS_OFF;
        }
        // (3.6.1: no refusal: one who may not hold the stone bare is told it will wait for him on the pedestal.)
        return StoneRules.mayHold(player, StoneKind.SOUL) ? null : Sacrifice.Outcome.NOT_BEARER;
    }

    /** He says the line ({@code key}; nothing for null) to the player. */
    public static void say(ServerPlayer player, @Nullable String key) {
        if (key == null) {
            return;
        }
        Component name = ModEntities.STONEKEEPER.get().getDescription().copy().withColor(NAME_COLOR);
        player.sendSystemMessage(Component.translatable("npc.enhanced.stonekeeper.say", name,
                Component.translatable(key).withColor(LINE_COLOR)));
        player.playNotifySound(ModSounds.STONEKEEPER_VOICE.get(), SoundSource.NEUTRAL, 0.8F, 1.0F);
    }

    private Stonekeeper() {
    }
}

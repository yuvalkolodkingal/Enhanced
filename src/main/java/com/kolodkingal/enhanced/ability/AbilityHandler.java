package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Routes an ability key press/release from a client to the right hero. */
public final class AbilityHandler {

    /**
     * A key from the client, with the client's own Sneak at the moment of the press ({@code sneak}). The server's
     * crouch lags behind it: the client sends its Sneak once a tick, after the input is read, so a quick Sneak + key can
     * reach the server as the plain key (3.5 QA: Heimdall's Sneak + N struck instead of sending). For the time the key is
     * handled the player's Sneak is the client's, so every hero (and every path that reads {@code isShiftKeyDown}, a
     * custom hero's binding included) sees the variant the player pressed; then the server's own state is put back.
     */
    public static void onKey(ServerPlayer player, int ability, boolean down, boolean sneak) {
        boolean crouch = player.isShiftKeyDown();
        if (crouch == sneak) {
            onKey(player, ability, down);
            return;
        }
        player.setShiftKeyDown(sneak);
        try {
            onKey(player, ability, down);
        } finally {
            player.setShiftKeyDown(crouch);
        }
    }

    /**
     * @param ability 1-5
     * @param down    true when the key was pressed, false when released (hold-to-use abilities need both)
     */
    public static void onKey(ServerPlayer player, int ability, boolean down) {
        // Releases always go through, so a hold ability can't get stuck on (e.g. when the player died holding the key).
        // (3.6: key 6 is the Y key of a hero whose Y row is not Build Magic: HeroKeys.pressY.)
        if (ability < 1 || ability > com.kolodkingal.enhanced.custom.HeroKeys.Y || (down && !player.isAlive())) {
            return;
        }
        // In a trial of the Witches' Road no power works (and only one without powers is let in).
        if (down && RoadTrials.isInside(player)) {
            return;
        }
        // Held in a cocoon of sand: the hands are covered. (A release still goes through, as above.)
        if (down && player.hasEffect(ModEffects.ENTOMBED)) {
            return;
        }
        // 3.3: Power Absorption has taken this hero's powers for a moment.
        if (down && com.kolodkingal.enhanced.custom.CustomHeroes.isSuppressed(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.enhanced.custom.suppressed")
                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return;
        }
        // 3.3: the runes on the walls of a Hex hold every other sorcerer inside it: no power answers.
        if (down && HexRunes.refuse(player)) {
            return;
        }
        // 3.3: a body Professor X steers is not its own: its keys do nothing until he lets go.
        if (down && MindLink.isHost(player)) {
            return;
        }
        // 3.7: a healer who is DOWN (healing/Downed) does nothing until he rises. (A release still goes through.)
        if (down && com.kolodkingal.enhanced.healing.HealingFactor.isDown(player)) {
            return;
        }
        // Drained by Agatha (3.3): the power is hers for a while. (A release still goes through.)
        if (down && player.hasEffect(ModEffects.DRAINED)) {
            player.displayClientMessage(Component.translatable("message.enhanced.agatha.you_are_drained"), true);
            return;
        }
        PowerData data = Powers.get(player);
        if (ability == com.kolodkingal.enhanced.custom.HeroKeys.Y) {
            com.kolodkingal.enhanced.custom.HeroKeys.pressY(player, data, down, player.isShiftKeyDown());
        } else {
            heroKey(data.power(), player, data, ability, down, player.isShiftKeyDown());
        }
        Powers.syncIfDirty(player);
    }

    /**
     * The key entry of hero {@code hero}'s code: the player's own power (with his Sneak as {@code alt}), or (3.4) one of a
     * custom hero's sources, with the key the ability has for that hero and the binding's Sneak variant
     * ({@code HeroKeys.press}).
     *
     * @param ability 1-5, the ability's key for {@code hero} (3.6: or 6, the Y key, {@code HeroKeys.Y})
     */
    public static void heroKey(Power hero, ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        switch (hero) {
            case MAGNETO -> MagnetoAbilities.onKey(player, data, ability, down, alt);
            case SCARLET_WITCH -> WandaAbilities.onKey(player, data, ability, down, alt);
            case QUICKSILVER -> QuicksilverAbilities.onKey(player, data, ability, down, alt);
            case VISION -> VisionAbilities.onKey(player, data, ability, down, alt);
            case BILLY -> BillyAbilities.onKey(player, data, ability, down, alt);
            case TOMMY -> TommyAbilities.onKey(player, data, ability, down, alt);
            case LOKI -> LokiAbilities.onKey(player, data, ability, down, alt);
            case THOR -> ThorAbilities.onKey(player, data, ability, down, alt);
            // The heroes of 3.0 are plug-ins (HeroServer): their keys are theirs.
            case PHOENIX, SPIDER_MAN, THANOS, ULTRON, APOCALYPSE, AGATHA, CYCLOPS, PROFESSOR_X, CUSTOM, HEIMDALL, ODIN,
                    HELA, DOCTOR_STRANGE, HULK, WOLVERINE, DEADPOOL -> HeroServers.onKey(hero, player, data, ability,
                            down,
                    alt);
            case NONE -> {
            }
        }
    }

    private AbilityHandler() {
    }
}

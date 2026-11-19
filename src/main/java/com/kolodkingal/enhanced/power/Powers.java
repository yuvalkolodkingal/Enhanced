package com.kolodkingal.enhanced.power;

import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Helpers for reading and changing a player's power. */
public final class Powers {

    /**
     * The player's power data. For a custom hero it carries his kit (3.4): built from his definition when it changed (the
     * definition is compared by identity, so this is cheap), kept in his {@code CustomState} and put back on a power data
     * the client has just received. Every reader of the power data comes through here.
     */
    public static PowerData get(Player player) {
        PowerData data = ModAttachments.POWER.get(player);
        if (data.power() == Power.CUSTOM) {
            com.kolodkingal.enhanced.custom.CustomState state = ModAttachments.CUSTOM_STATE.get(player);
            com.kolodkingal.enhanced.custom.HeroKit kit = state.kitFor(ModAttachments.CUSTOM_HERO.get(player));
            if (data.kit() != kit) {
                data.setKit(kit);
            }
        }
        return data;
    }

    public static Power power(Player player) {
        return get(player).power();
    }

    public static boolean is(Player player, Power power) {
        return power(player) == power;
    }

    /** Pushes the player's power data to every client that can see them, if anything changed. */
    public static void syncIfDirty(ServerPlayer player) {
        PowerData data = get(player);
        if (data.isDirty()) {
            data.clearDirty();
            ModAttachments.POWER.sync(player);
        }
    }

    /**
     * Gives (or removes, with {@link Power#NONE}) a power. Cleans up whatever the old power was doing.
     */
    public static void setPower(ServerPlayer player, Power power) {
        PowerData data = get(player);
        Power old = data.power();
        if (old == power) {
            return;
        }
        Telekinesis.release(player, false);
        PowerTicker.endPower(player, data);
        if (data.has(com.kolodkingal.enhanced.custom.HeroAbility.WANDA_HEX)) { // (3.4: or a custom hero with her Hex)
            HexManager.dissolveAllOwnedBy(player.server, player.getUUID());
        }
        data.setPowerRaw(power);
        PowerTicker.applyPassives(player, data);
        syncIfDirty(player);
        BuildMagic.onPowerChanged(player, old, power);
        // (3.5: another hero's helmet, goggles, glasses or crown comes off the head into the inventory.)
        com.kolodkingal.enhanced.custom.HeroItems.takeOffForeign(player, data);
    }

    /**
     * The dramatic moment a player becomes a hero: title on screen, a burst of energy, a sound everyone nearby
     * hears and a message to the whole server.
     */
    public static void awaken(ServerPlayer player, Power power) {
        awaken(player, power, "");
    }

    /** {@link #awaken(ServerPlayer, Power)} straight into one of the hero's looks (an unknown id means the default). */
    public static void awaken(ServerPlayer player, Power power, String variant) {
        awaken(player, power, variant, true);
    }

    /**
     * {@link #awaken(ServerPlayer, Power, String)}; {@code ownWay} false (3.8.1) when the hero was not reached his own
     * way but given by a command or the Hero Creator's "Choose a hero": the server is then told plainly that he became
     * the hero ("… has become Iron Man!"), not the story of his item ("… put an Arc Reactor in their chest …").
     */
    public static void awaken(ServerPlayer player, Power power, String variant, boolean ownWay) {
        setPower(player, power);
        Variant chosen = Variants.get(power, variant);
        setVariant(player, EarnedLooks.has(player, chosen) ? chosen : Variants.get(power, ""));
        ServerLevel level = player.serverLevel();

        Component title = power.awakeningTitle().copy().withStyle(s -> s.withColor(power.titleColor()).withBold(true));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        String lineKey = "power.enhanced." + power.getSerializedName() + ".awaken";
        Component line = Component.translatable(lineKey).withStyle(ChatFormatting.GRAY);
        if (fitsSubtitle(lineKey)) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(line));
        } else {
            // A subtitle is drawn at twice the size of text and never wrapped, so a long line ("Whosoever holds this
            // hammer...") is cut at both ends on most screens: it goes in the action bar, which shows it whole.
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
            player.displayClientMessage(line, true);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        Variant look = variant(player);
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), look.color(), 120, 1.6);
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), look.glow(), 60, 0.8);

        String key = ownWay ? "power.enhanced." + power.getSerializedName() + ".broadcast"
                : "power.enhanced.chosen.broadcast";
        Component told = ownWay ? Component.translatable(key, player.getDisplayName())
                : Component.translatable(key, player.getDisplayName(), power.displayName());
        player.server.getPlayerList().broadcastSystemMessage(
                told.copy().withStyle(s -> s.withColor(power.titleColor())), false);
    }

    /** The longest awakening line (in the server's English) that is shown as the title's subtitle. */
    public static final int SUBTITLE_CHARS = 32;

    /** Whether the awakening line of this key fits under the title (see {@link #SUBTITLE_CHARS}). */
    public static boolean fitsSubtitle(String lineKey) {
        return Language.getInstance().getOrDefault(lineKey).length() <= SUBTITLE_CHARS;
    }

    /** The player's current look (a custom hero's is his own colours, see {@code CustomHeroes.variant}). */
    public static Variant variant(Player player) {
        PowerData data = get(player);
        return data.power() == Power.CUSTOM ? com.kolodkingal.enhanced.custom.CustomHeroes.variant(player) : Variants.current(data);
    }

    /** (3.17) The look the player is DRAWN in: his own skin's look when he chose it, else {@link #variant}. */
    public static Variant drawnVariant(Player player) {
        PowerData data = get(player);
        return data.power() == Power.CUSTOM ? variant(player) : Variants.drawn(data);
    }

    /** Main energy colour of the player's hero, in their current look. */
    public static int color(Player player) {
        return variant(player).color();
    }

    /** Secondary glow colour of the player's hero, in their current look. */
    public static int glow(Player player) {
        return variant(player).glow();
    }

    /** Changes the player's look (no effects). */
    public static void setVariant(ServerPlayer player, Variant variant) {
        PowerData data = get(player);
        if (variant.power() != data.power() || Variants.current(data).equals(variant)) {
            return;
        }
        data.setVariantRaw(variant.id());
        syncIfDirty(player);
    }

    /**
     * (3.17) Wears the player's own Minecraft skin instead of the hero's suit, or the suit again: his choice for every hero,
     * built-in or custom, kept when he becomes another; powers, auras and effects are as they were. Synced, so everyone sees it.
     */
    public static void setOwnSkin(ServerPlayer player, boolean own) {
        PowerData data = get(player);
        if (data.ownSkin() == own) {
            return;
        }
        data.setOwnSkinRaw(own);
        syncIfDirty(player);
    }

    /**
     * The Change Look key: steps to the next (or previous) look in a shimmer of the new look's colours. (3.17: after the last look
     * of every hero comes "Your own skin", the player's own skin on the hero; heroes with only one suit, and custom heroes, have
     * that one step.) Returns false if the hero has no other looks.
     */
    public static boolean cycleVariant(ServerPlayer player, int step) {
        PowerData data = get(player);
        // (3.12.2: within the character the hero is: Source Code Vision and Nano Ultron have looks of their own only)
        java.util.List<Variant> looks = Characters.looks(data);
        // The steps: the looks (a hero without any has his one suit), then "your own skin" (a hero with an "own" look has it).
        boolean extra = looks.stream().noneMatch(Variant::ownSkin);
        int count = Math.max(1, looks.size());
        int total = count + (extra ? 1 : 0);
        if (total < 2) {
            return false;
        }
        int at = 0;
        for (int i = 0; i < looks.size(); i++) {
            if (looks.get(i).id().equals(data.variant())) {
                at = i;
            }
        }
        int pos = extra && data.ownSkin() ? count : at;
        int next = Math.floorMod(pos + step, total);
        // (3.12: a look that is earned is skipped until it is)
        while (next < looks.size() && !EarnedLooks.has(player, looks.get(next))) {
            next = Math.floorMod(next + step, total);
        }
        ServerLevel level = player.serverLevel();
        Variant shown;
        if (extra && next == count) {
            setOwnSkin(player, true);
            shown = Variants.ownLook(data.power());
        } else {
            setOwnSkin(player, false);
            shown = looks.isEmpty() ? Variants.get(data.power(), "") : looks.get(next);
            setVariant(player, shown);
        }
        Variant next1 = shown;
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), next1.color(), 40, 0.5);
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), next1.glow(), 20, 0.3);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 0.35F, 1.6F);
        player.displayClientMessage(Component.translatable("message.enhanced.look", next1.displayName())
                .withStyle(s -> s.withColor(next1.color())), true);
        return true;
    }

    /** A sphere of energy particles, sent to everyone nearby. */
    public static void burst(ServerLevel level, double x, double y, double z, int color, int count, double spread) {
        level.sendParticles(new EnergyParticleOptions(color, 1.6F), x, y, z, count, spread, spread, spread, 0.15);
    }

    private Powers() {
    }
}

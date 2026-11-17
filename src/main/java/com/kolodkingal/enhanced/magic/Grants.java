package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Giving magic (3.17): the Demiurge gives witchcraft (a path and a level) or a sorcerer's art to players with no hero
 * power, on his Grant Magic screen, and takes back what he gave. An operator does the same with {@code /enhanced witch};
 * the Coven's Road gives a first-level path to a walker who asks for it.
 */
public final class Grants {
    private Grants() {
    }

    /** The Demiurge opens his screen: every player online, with what magic each has. */
    static void openScreen(ServerPlayer demiurge) {
        List<String> names = new ArrayList<>();
        List<String> states = new ArrayList<>();
        for (ServerPlayer other : demiurge.level().getServer().getPlayerList().getPlayers()) {
            names.add(other.getGameProfile().getName());
            MagicData data = Magic.data(other);
            String state = Powers.power(other).hasPowers() ? "hero:" + Powers.power(other).getSerializedName()
                    : data.witchPath() == null ? "none" : data.witchPath().id() + ":" + data.level() + (data.has(MagicData.GRANTED) ? ":granted" : "");
            states.add(state);
        }
        Net.sendToPlayer(demiurge, new MagicScreenPayload(MagicScreenPayload.GRANT, names, states, 0));
    }

    /** Makes {@code target} a witch (or sorcerer) of {@code path} at {@code level}, with the gift's light and a word. */
    public static void give(ServerPlayer target, WitchPath path, int level, boolean granted) {
        MagicData data = Magic.data(target).withCraft(path, level, granted);
        Magic.set(target, data);
        Magic.set(target, data.withMana(Magic.maxMana(target)));
        ServerLevel world = target.serverLevel();
        MagicFx.send(world, MagicFx.AWAKEN, target.position(), java.util.List.of(), path.color, path.glow, 1.0F, 40);
        MagicFx.send(world, MagicFx.CIRCLE, target.position(), java.util.List.of(), path.color, path.glow, 3.0F, 60);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        target.displayClientMessage(Component.translatable("message.enhanced.witch.become", path.displayName(), level)
                .withStyle(s -> s.withColor(path.color).withBold(true)), false);
        target.displayClientMessage(Component.translatable("message.enhanced.witch.keys").withStyle(ChatFormatting.GRAY), false);
    }

    /** Takes witchcraft away. */
    public static void revoke(ServerPlayer target) {
        Casting.stop(target);
        MagicZones.endOf(target.server, target.getUUID());
        Magic.set(target, Magic.data(target).withCraft(null, 1, false).withMana(0));
        target.displayClientMessage(Component.translatable("message.enhanced.witch.revoked").withStyle(ChatFormatting.GRAY), false);
        target.serverLevel().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** The Demiurge's answer on his screen (checked again here). */
    public static void fromScreen(ServerPlayer demiurge, String name, String pathId, int level, boolean revoke) {
        if (!Magic.isDemiurge(demiurge)) {
            demiurge.displayClientMessage(Component.translatable("message.enhanced.witch.not_demiurge").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ServerPlayer target = demiurge.level().getServer().getPlayerList().getPlayerByName(name);
        if (target == null) {
            return;
        }
        if (revoke) {
            if (!Magic.data(target).hasCraft()) {
                return;
            }
            revoke(target);
            demiurge.displayClientMessage(Component.translatable("message.enhanced.witch.you_revoked", target.getDisplayName()), true);
            openScreen(demiurge);
            return;
        }
        WitchPath path = WitchPath.byId(pathId);
        if (path == null || level < 1 || level > WitchPath.MAX_LEVEL) {
            return;
        }
        if (Powers.power(target).hasPowers()) {
            demiurge.displayClientMessage(Component.translatable("message.enhanced.witch.has_power", target.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        give(target, path, level, true);
        MagicFx.send(demiurge.serverLevel(), MagicFx.BEAMS, demiurge.getEyePosition(), java.util.List.of(target.position().add(0, 1, 0)),
                0xFFFFFF, path.color, 1.0F, 20);
        demiurge.displayClientMessage(Component.translatable("message.enhanced.witch.you_granted", target.getDisplayName(), path.displayName(), level), true);
        openScreen(demiurge);
    }
}

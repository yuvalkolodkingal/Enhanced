package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.config.AsgardConfig;
import com.kolodkingal.enhanced.network.AsgardTalkPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The server side of a talk with an Asgardian (3.5). A right-click {@link #begin}s it: the node is picked by the rules of
 * {@link AsgardDialogue} and sent to the player's talk screen ({@code AsgardTalkPayload.Open}); the Asgardian stops and
 * turns to him. The answer he chooses ({@code AsgardTalkPayload.Choose}) comes to {@link #choose}: it must be one the
 * last node offered him, from no farther than {@code asgard.talkDistance}; then its action runs (another node, a trade,
 * the Bifrost...). An empty answer is the screen closing: the talk ends.
 */
public final class AsgardTalk {
    /** A talk going on: whom the player talks to, what he was offered. */
    private record Session(int entityId, Role role, String node, List<AsgardDialogue.Option> offered) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private AsgardTalk() {
    }

    /** How far a player may be to talk ({@code asgard.talkDistance}). */
    public static double talkDistance() {
        return AsgardConfig.TALK_DISTANCE.get();
    }

    /** The talk context of {@code player} with {@code speaker} of {@code role}. */
    public static AsgardDialogue.Ctx context(ServerPlayer player, Entity speaker, Role role) {
        AsgardProgress progress = player.getData(ModAttachments.ASGARD);
        return new AsgardDialogue.Ctx(player, speaker, role, progress, AsgardState.get(player.getServer()));
    }

    /**
     * The player right-clicked {@code npc}: opens the talk (returns false if the Asgardian cannot talk now: down, asleep is
     * answered by Frigga's line, busy with another player, fighting, too far).
     */
    public static boolean begin(ServerPlayer player, AsgardianEntity npc) {
        if (player.isSpectator() || !npc.isAlive() || player.distanceTo(npc) > talkDistance() + 0.5) {
            return false;
        }
        NpcState state = npc.state();
        if (state == NpcState.DOWN) {
            speak(npc, player, "npc.enhanced.common.down", List.of());
            return true;
        }
        if (state == NpcState.FIGHTING || state == NpcState.BOWING || npc.summoned()) {
            return false;
        }
        if (npc.talkingWith() != null && !npc.talkingWith().equals(player.getUUID())) {
            speak(npc, player, "npc.enhanced.common.busy", List.of());
            return true;
        }
        if (npc.getTradingPlayer() != null && npc.getTradingPlayer() != player) {
            speak(npc, player, "npc.enhanced.common.busy", List.of());
            return true;
        }
        if (state == NpcState.SLEEPING) {
            // Odin sleeps: Frigga's words beside him.
            SESSIONS.put(player.getUUID(), new Session(npc.getId(), Role.FRIGGA, "odin_asleep", List.of()));
            send(player, npc, Role.FRIGGA, "npc.enhanced.frigga.odin_asleep", List.of(), List.of());
            return true;
        }
        AsgardDialogue.Ctx ctx = context(player, npc, npc.role());
        AsgardDialogue.Node node = AsgardDialogue.pick(ctx);
        if (node == null) {
            return false;
        }
        npc.startTalk(player);
        Power power = ctx.power();
        if (power == Power.ODIN || power == Power.HEIMDALL && npc.role() != Role.HEIMDALL) {
            npc.setState(NpcState.BOWING, 30);
            npc.setPartner(player);
            npc.playSound(com.kolodkingal.enhanced.registry.ModSounds.NPC_BOW.get(), 0.8F, 1.0F);
        } else if (power == Power.HELA && npc.role() != Role.ODIN) {
            Vec3 away = npc.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                away = away.normalize().scale(0.4);
                npc.push(away.x, 0.05, away.z);
                npc.hurtMarked = true;
            }
        }
        show(ctx, node);
        remember(player, npc.role());
        return true;
    }

    /** The flags of who he has talked to (after the node was picked: the first greeting is said once). */
    private static void remember(ServerPlayer player, Role role) {
        int flag = switch (role) {
            case HEIMDALL -> AsgardProgress.MET_HEIMDALL;
            case FRIGGA -> AsgardProgress.TALKED_FRIGGA;
            case SIF -> AsgardProgress.TALKED_SIF;
            case VALKYRIE -> AsgardProgress.TALKED_VALKYRIE;
            case VOLSTAGG, FANDRAL, HOGUN -> AsgardProgress.TALKED_WARRIORS;
            default -> 0;
        };
        AsgardProgress progress = player.getData(ModAttachments.ASGARD);
        if (flag != 0 && !progress.has(flag)) {
            player.setData(ModAttachments.ASGARD, progress.with(flag));
        }
    }

    /** Shows the node {@code key} of the speaker's role (an option's next node); nothing if there is none. */
    public static void show(AsgardDialogue.Ctx ctx, String key) {
        AsgardDialogue.Node node = AsgardDialogue.find(ctx.role(), key);
        if (node != null) {
            show(ctx, node);
        }
    }

    /** Shows a node: its line (a variant at random, its arguments) and the answers it offers. */
    public static void show(AsgardDialogue.Ctx ctx, AsgardDialogue.Node node) {
        ServerPlayer player = ctx.player();
        String line = AsgardDialogue.variant(AsgardDialogue.lineKey(ctx.role(), node.key()), player.getRandom());
        List<AsgardDialogue.Option> offered = AsgardDialogue.offered(ctx, node);
        List<String> keys = new ArrayList<>();
        for (AsgardDialogue.Option option : offered) {
            keys.add(option.key());
        }
        SESSIONS.put(player.getUUID(), new Session(ctx.speaker().getId(), ctx.role(), node.key(), offered));
        if (ctx.speaker() instanceof AsgardianEntity npc) {
            npc.startTalk(player);
        }
        send(player, ctx.speaker(), ctx.role(), line, AsgardDialogue.argsOf(ctx, node.key()), keys);
    }

    private static void send(ServerPlayer player, Entity speaker, Role role, String line, List<String> args, List<String> options) {
        List<String> clipped = args.size() > AsgardTalkPayload.MAX_ARGS ? args.subList(0, AsgardTalkPayload.MAX_ARGS) : args;
        PacketDistributor.sendToPlayer(player, new AsgardTalkPayload.Open(speaker.getId(), role.ordinal(), line, List.copyOf(clipped),
                List.copyOf(options)));
        if (speaker instanceof AsgardianEntity npc) {
            npc.playSound(npc.voice(), 0.7F, 0.95F + player.getRandom().nextFloat() * 0.1F);
        }
    }

    /** The player chose {@code option} in his talk with the Asgardian {@code entityId}. */
    public static void choose(ServerPlayer player, int entityId, String option) {
        tryChoose(player, entityId, option);
    }

    /**
     * The same, saying whether the answer was taken: it must be one the talk with that Asgardian offered, from no farther
     * than {@code asgard.talkDistance}; the empty answer (the screen closed) ends the talk and is always taken.
     */
    public static boolean tryChoose(ServerPlayer player, int entityId, String option) {
        Session session = SESSIONS.get(player.getUUID());
        Entity speaker = player.level().getEntity(entityId);
        if (session == null || session.entityId() != entityId) {
            return false;
        }
        if (option.isEmpty()) {
            end(player);
            return true;
        }
        AsgardDialogue.Option chosen = null;
        for (AsgardDialogue.Option offered : session.offered()) {
            if (offered.key().equals(option)) {
                chosen = offered;
            }
        }
        if (chosen == null || speaker == null || !speaker.isAlive() || player.distanceTo(speaker) > talkDistance()
                || speaker instanceof AsgardianEntity npc && npc.isDown()) {
            return false;
        }
        AsgardDialogue.Ctx ctx = context(player, speaker, session.role());
        chosen.action().run(ctx);
        if (SESSIONS.get(player.getUUID()) == session) {
            end(player); // nothing more was said: the talk is over
        }
        return true;
    }

    /** Ends the player's talk (the Asgardian goes back to what he was doing). */
    public static void end(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null && player.level().getEntity(session.entityId()) instanceof AsgardianEntity npc
                && player.getUUID().equals(npc.talkingWith())) {
            npc.endTalk();
        }
    }

    /** The node the player was last shown (for the tests), or null. */
    @Nullable
    public static String currentNode(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session == null ? null : session.node();
    }

    /** The answers the player was last offered (for the tests). */
    public static List<String> offered(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        List<String> keys = new ArrayList<>();
        if (session != null) {
            for (AsgardDialogue.Option option : session.offered()) {
                keys.add(option.key());
            }
        }
        return keys;
    }

    /** An Asgardian says a line to one player (in his chat, with his name in his colour, and his murmur). */
    public static void speak(Entity speaker, ServerPlayer player, String lineKey, List<String> args) {
        Role role = speaker instanceof AsgardianEntity npc ? npc.role() : Role.CITIZEN;
        player.sendSystemMessage(said(role, lineKey, args));
        if (speaker instanceof AsgardianEntity npc) {
            npc.playSound(npc.voice(), 0.7F, 1.0F);
        }
    }

    /** "<Name> line": the name in the role's colour. */
    public static Component said(Role role, String lineKey, List<String> args) {
        MutableComponent name = Component.translatable(role.nameKey()).withStyle(s -> s.withColor(role.color()).withBold(true));
        Object[] parts = new Object[args.size()];
        for (int i = 0; i < parts.length; i++) {
            String arg = args.get(i);
            parts[i] = arg.startsWith("@") ? Component.translatable(arg.substring(1)) : arg;
        }
        return Component.empty().append(name).append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(lineKey, parts).withStyle(ChatFormatting.WHITE));
    }

    /** Says a line to every player within {@code range} of the speaker. */
    public static void announce(Entity speaker, String lineKey, double range) {
        if (speaker.level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(speaker) <= range * range) {
                    speak(speaker, player, lineKey, List.of());
                }
            }
        }
    }

    /** Forgets every talk (the server stops) or one player's (he leaves). */
    public static void clear() {
        SESSIONS.clear();
        AsgardLines.clear();
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
    }
}

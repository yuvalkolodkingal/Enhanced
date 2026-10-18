package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * What the Asgardians say (3.5): a table of dialogue nodes per role, in Java (no JSON). Right-clicking an Asgardian
 * picks the node of his role whose {@code when} holds with the highest priority; its options are the answers offered,
 * and choosing one runs its action. Lang keys: {@code npc.enhanced.<role>.<node>} (with {@code .<n>} for lines with
 * variants) and {@code npc.enhanced.option.<key>}.
 *
 * <p>The 3.5 skeleton writes the types and the table ({@link #register}, {@link #nodes}) so that lanes L2, L3 and L4
 * can add the nodes of Heimdall's Watch, Odin and Hela's warnings from their own classes; lane L5 owns this class (the
 * picking, the talk screen, every other role's lines).
 */
public final class AsgardDialogue {
    /** What a node or an option is judged by: who talks to whom, and how far he has come. */
    public record Ctx(ServerPlayer player, Entity speaker, Role role, AsgardProgress progress, AsgardState state) {
    }

    /** What choosing an option does (the next node, a trade, the Watch, the Bifrost...). */
    @FunctionalInterface
    public interface Action {
        void run(Ctx ctx);
    }

    /** An answer the player may choose: shown when {@code when} holds. */
    public record Option(String key, Predicate<Ctx> when, Action action) {
    }

    /** A line of a role: said when {@code when} holds and no node of a higher priority does. */
    public record Node(String key, Predicate<Ctx> when, int priority, List<Option> options) {
    }

    private static final Map<Role, List<Node>> NODES = new EnumMap<>(Role.class);

    /** Adds a node to a role's table (from a lane's own class, once, when it loads). */
    public static synchronized void register(Role role, Node node) {
        NODES.computeIfAbsent(role, r -> new ArrayList<>()).add(node);
    }

    /** The nodes of a role, in the order they were registered. */
    public static synchronized List<Node> nodes(Role role) {
        return Collections.unmodifiableList(new ArrayList<>(NODES.getOrDefault(role, List.of())));
    }

    private AsgardDialogue() {
    }
}

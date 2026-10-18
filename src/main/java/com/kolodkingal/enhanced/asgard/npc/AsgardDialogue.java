package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * What the Asgardians say (3.5): a table of dialogue nodes per role, in Java (no JSON). Right-clicking an Asgardian
 * picks the node of his role whose {@code when} holds with the highest priority ({@link #pick}); its options are the
 * answers offered (at most {@link #MAX_OPTIONS}), and choosing one runs its action ({@link AsgardTalk}).
 *
 * <p><b>Lang keys.</b> A node's line is {@code npc.enhanced.<role>.<node>}; a node key with a dot names a shared line
 * instead ({@code common.react_thor} → {@code npc.enhanced.common.react_thor}). A line with variants ({@link #variants})
 * is said as {@code <line>.<n>}, n from 1, one at random. An option is {@code npc.enhanced.option.<key>}. Arguments
 * ({@link #args}) fill the line's {@code %s}; an argument that starts with {@code @} is a lang key the client translates.
 *
 * <p><b>For the other lanes</b> (L2 the Watch, L3 Odin, L4 Hela's warnings), from their own classes, once, when they
 * load:
 * <pre>{@code
 * AsgardDialogue.node(Role.HEIMDALL, "watch")
 *         .when(ctx -> !ctx.has(AsgardProgress.WATCH_WON)).priority(5)
 *         .option("take_watch", ctx -> true, ctx -> GuardiansWatch.start(ctx.player()))
 *         .option(AsgardDialogue.Option.close("not_yet"))
 *         .register();
 * AsgardDialogue.addOption(Role.HEIMDALL, "greeting", AsgardDialogue.Option.to("watch_ask", "watch"));
 * }</pre>
 * {@link #addOption} puts an answer into a node another lane wrote (after its own answers; the first three that hold are
 * offered, so L5's nodes leave room: Heimdall's greetings offer two of their own). An action that does not show another
 * node ({@link AsgardTalk#show}) or open a screen ends the talk. Every key a node or option names must exist in
 * {@code en_us} and {@code he_il} ({@code AsgardianTests} scans the whole table).
 */
public final class AsgardDialogue {
    /** The most answers a node offers (the talk screen's buttons). */
    public static final int MAX_OPTIONS = 3;
    /** A node only shown by an option's action ({@link AsgardTalk#show}), never picked by itself. */
    public static final Predicate<Ctx> NEVER = ctx -> false;
    public static final Predicate<Ctx> ALWAYS = ctx -> true;

    /** What a node or an option is judged by: who talks to whom, and how far he has come. */
    public record Ctx(ServerPlayer player, Entity speaker, Role role, AsgardProgress progress, AsgardState state) {
        /** Whether the player has every bit of {@code flag} ({@link AsgardProgress}). */
        public boolean has(int flag) {
            return progress.has(flag);
        }

        /** The player's power (the hero he is). */
        public Power power() {
            return Powers.power(player);
        }

        /** The speaker as an Asgardian, or null (a lane's own speaker). */
        @Nullable
        public AsgardianEntity npc() {
            return speaker instanceof AsgardianEntity npc ? npc : null;
        }

        /** Whether a fight with Hela is on in the speaker's level. */
        public boolean helaFight() {
            if (!(speaker.level() instanceof ServerLevel level)) {
                return false;
            }
            BossFight fight = BossFights.active(level);
            return fight != null && fight.kind() == BossKind.HELA;
        }

        /** Whether the player is hurt (below his full health). */
        public boolean hurt() {
            return player.getHealth() < player.getMaxHealth() - 0.5F;
        }
    }

    /** What choosing an option does (the next node, a trade, the Watch, the Bifrost...). */
    @FunctionalInterface
    public interface Action {
        void run(Ctx ctx);
    }

    /** An answer the player may choose: shown when {@code when} holds. */
    public record Option(String key, Predicate<Ctx> when, Action action) {
        /** An answer that leads to another node of the same role. */
        public static Option to(String key, String node) {
            return new Option(key, ALWAYS, ctx -> AsgardTalk.show(ctx, node));
        }

        /** The same, offered only when {@code when} holds. */
        public static Option to(String key, Predicate<Ctx> when, String node) {
            return new Option(key, when, ctx -> AsgardTalk.show(ctx, node));
        }

        /** An answer that ends the talk. */
        public static Option close(String key) {
            return new Option(key, ALWAYS, ctx -> {
            });
        }
    }

    /** A line of a role: said when {@code when} holds and no node of a higher priority does. */
    public record Node(String key, Predicate<Ctx> when, int priority, List<Option> options) {
    }

    private static final Map<Role, List<Node>> NODES = new EnumMap<>(Role.class);
    private static final Map<String, List<Option>> EXTRA = new HashMap<>();
    private static final Map<String, Integer> VARIANTS = new HashMap<>();
    private static final Map<String, Function<Ctx, List<String>>> ARGS = new HashMap<>();
    /** Lang keys said outside nodes ({@link AsgardTalk#speak}): checked by the tests like the nodes' lines. */
    private static final Set<String> LINES = new LinkedHashSet<>();

    static {
        AsgardLines.registerAll();
    }

    /** Adds a node to a role's table (from a lane's own class, once, when it loads). */
    public static synchronized void register(Role role, Node node) {
        NODES.computeIfAbsent(role, r -> new ArrayList<>()).add(node);
    }

    /** The nodes of a role, in the order they were registered. */
    public static synchronized List<Node> nodes(Role role) {
        return Collections.unmodifiableList(new ArrayList<>(NODES.getOrDefault(role, List.of())));
    }

    /** The node of a role with this key, or null. */
    @Nullable
    public static synchronized Node find(Role role, String key) {
        for (Node node : NODES.getOrDefault(role, List.of())) {
            if (node.key().equals(key)) {
                return node;
            }
        }
        return null;
    }

    /** Adds an answer to a node of a role (a node of another lane: Heimdall's greeting gets the Watch). */
    public static synchronized void addOption(Role role, String node, Option option) {
        EXTRA.computeIfAbsent(role.name() + "/" + node, k -> new ArrayList<>()).add(option);
    }

    /** The answers of a node: its own, then those added to it. */
    public static synchronized List<Option> allOptions(Role role, Node node) {
        List<Option> all = new ArrayList<>(node.options());
        all.addAll(EXTRA.getOrDefault(role.name() + "/" + node.key(), List.of()));
        return all;
    }

    /** Says that the line {@code lineKey} has {@code count} variants ({@code <lineKey>.1} to {@code .<count>}). */
    public static synchronized void variants(String lineKey, int count) {
        VARIANTS.put(lineKey, count);
    }

    /** How many variants a line has (1: the key itself). */
    public static synchronized int variantCount(String lineKey) {
        return VARIANTS.getOrDefault(lineKey, 1);
    }

    /** The arguments of a node's line (its {@code %s}), worked out when it is said. */
    public static synchronized void args(Role role, String node, Function<Ctx, List<String>> args) {
        ARGS.put(role.name() + "/" + node, args);
    }

    /** Notes lang keys said outside the nodes (for the tests' scan). */
    public static synchronized void lines(String... keys) {
        Collections.addAll(LINES, keys);
    }

    /** Every lang key the table names: every line with its variants, every option, the extra lines. */
    public static synchronized Set<String> langKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (Map.Entry<Role, List<Node>> entry : NODES.entrySet()) {
            for (Node node : entry.getValue()) {
                keys.addAll(lineKeys(lineKey(entry.getKey(), node.key())));
                for (Option option : allOptions(entry.getKey(), node)) {
                    keys.add(optionKey(option.key()));
                }
            }
        }
        for (String line : LINES) {
            keys.addAll(lineKeys(line));
        }
        return keys;
    }

    private static List<String> lineKeys(String base) {
        int n = variantCount(base);
        if (n <= 1) {
            return List.of(base);
        }
        List<String> keys = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            keys.add(base + "." + i);
        }
        return keys;
    }

    /** The lang key of a node's line (without its variant). */
    public static String lineKey(Role role, String node) {
        return node.indexOf('.') >= 0 ? "npc.enhanced." + node : "npc.enhanced." + role.id() + "." + node;
    }

    public static String optionKey(String option) {
        return "npc.enhanced.option." + option;
    }

    /** One variant of a line, at random ({@code <base>.<n>}), or the line itself. */
    public static String variant(String base, RandomSource random) {
        int n = variantCount(base);
        return n <= 1 ? base : base + "." + (1 + random.nextInt(n));
    }

    /** A "mind your manners" line of a role (the named ones have their own, the others share four). */
    public static String mannersLine(Role role, RandomSource random) {
        return role.named() ? lineKey(role, "manners") : variant("npc.enhanced.common.manners", random);
    }

    /** The arguments of a node's line in this talk. */
    public static synchronized List<String> argsOf(Ctx ctx, String node) {
        Function<Ctx, List<String>> args = ARGS.get(ctx.role().name() + "/" + node);
        return args == null ? List.of() : args.apply(ctx);
    }

    /** The node to say first: of the nodes whose {@code when} holds, the one of the highest priority (the first). */
    @Nullable
    public static synchronized Node pick(Ctx ctx) {
        Node best = null;
        for (Node node : NODES.getOrDefault(ctx.role(), List.of())) {
            if ((best == null || node.priority() > best.priority()) && node.when().test(ctx)) {
                best = node;
            }
        }
        return best;
    }

    /** The answers a node offers in this talk: those whose {@code when} holds, the first {@link #MAX_OPTIONS}. */
    public static List<Option> offered(Ctx ctx, Node node) {
        List<Option> offered = new ArrayList<>();
        for (Option option : allOptions(ctx.role(), node)) {
            if (offered.size() < MAX_OPTIONS && option.when().test(ctx)) {
                offered.add(option);
            }
        }
        return offered;
    }

    /** The builder of a node ({@link #register()} adds it). */
    public static Builder node(Role role, String key) {
        return new Builder(role, key);
    }

    public static final class Builder {
        private final Role role;
        private final String key;
        private Predicate<Ctx> when = NEVER;
        private int priority;
        private final List<Option> options = new ArrayList<>();

        private Builder(Role role, String key) {
            this.role = role;
            this.key = key;
        }

        /** When the node is picked by itself (without: only shown by an option). */
        public Builder when(Predicate<Ctx> when) {
            this.when = when;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /** The line has {@code count} variants. */
        public Builder variants(int count) {
            AsgardDialogue.variants(lineKey(role, key), count);
            return this;
        }

        public Builder args(Function<Ctx, List<String>> args) {
            AsgardDialogue.args(role, key, args);
            return this;
        }

        public Builder option(Option option) {
            options.add(option);
            return this;
        }

        public Builder option(String key, Predicate<Ctx> when, Action action) {
            return option(new Option(key, when, action));
        }

        public Builder options(List<Option> more) {
            options.addAll(more);
            return this;
        }

        public Node register() {
            Node node = new Node(key, when, priority, List.copyOf(options));
            AsgardDialogue.register(role, node);
            return node;
        }
    }

    private AsgardDialogue() {
    }
}

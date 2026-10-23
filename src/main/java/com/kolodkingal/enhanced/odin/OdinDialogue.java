package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Ctx;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Node;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Option;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.network.AsgardTalkPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;

import java.util.List;
import java.util.function.Predicate;

/**
 * What Odin says (3.5): the nodes of the Asgardian whose role is {@link Role#ODIN}, added to lane L5's table ({@link
 * AsgardDialogue#register}) when the hero's plug-in loads. Asleep in his chamber (until Hela first falls) he does not answer;
 * awake, his words follow the speaker's way to the throne: to one who beat Hela, where Gungnir waits; to the bearer of
 * Gungnir, the throne; to the crowned, the weight of the realm; to his son and to Loki, a father's words; to Hela's heir,
 * a warning. Three questions may be asked of him (Gungnir, his ravens, the vault), each answered by a line of its own.
 *
 * <p>Lang keys: {@code npc.enhanced.odin.<node>} for the lines and {@code npc.enhanced.option.<key>} for the answers. Every
 * line is our own words.
 */
public final class OdinDialogue {
    /** The nodes Odin may say, highest priority first (the keys of their lines under {@code npc.enhanced.odin.}). */
    public static final List<String> NODES = List.of("asleep", "hela_heir", "crowned", "bearer", "victor", "son", "loki", "greet");
    /** The answers offered (keys under {@code npc.enhanced.option.}), and the lines that answer them. */
    public static final List<String> OPTIONS = List.of("odin_gungnir", "odin_ravens", "odin_vault");
    public static final List<String> ANSWERS = List.of("about_gungnir", "about_ravens", "about_vault");

    private static boolean registered;

    /** Adds Odin's nodes to the table (once). */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        Predicate<Ctx> awake = c -> c.state().odinAwake();
        List<Option> ask = List.of(
                new Option("odin_gungnir", c -> true, c -> answer(c, "about_gungnir")),
                new Option("odin_ravens", c -> true, c -> answer(c, "about_ravens")),
                new Option("odin_vault", c -> c.progress().has(AsgardProgress.HELA_VICTOR) && !c.progress().has(AsgardProgress.GUNGNIR_TAKEN),
                        c -> answer(c, "about_vault")));
        AsgardDialogue.register(Role.ODIN, new Node("asleep", c -> !c.state().odinAwake(), 100, List.of()));
        AsgardDialogue.register(Role.ODIN, new Node("hela_heir", awake.and(c -> Powers.power(c.player()) == Power.HELA), 90, List.of()));
        AsgardDialogue.register(Role.ODIN, new Node("crowned", awake.and(c -> c.progress().has(AsgardProgress.CROWNED)
                || Powers.power(c.player()) == Power.ODIN), 80, ask));
        AsgardDialogue.register(Role.ODIN, new Node("bearer", awake.and(c -> c.progress().has(AsgardProgress.GUNGNIR_TAKEN)), 70, ask));
        AsgardDialogue.register(Role.ODIN, new Node("victor", awake.and(c -> c.progress().has(AsgardProgress.HELA_VICTOR)), 60, ask));
        AsgardDialogue.register(Role.ODIN, new Node("son", awake.and(c -> Powers.power(c.player()) == Power.THOR), 50, ask));
        AsgardDialogue.register(Role.ODIN, new Node("loki", awake.and(c -> Powers.power(c.player()) == Power.LOKI), 50, ask));
        AsgardDialogue.register(Role.ODIN, new Node("greet", awake, 0, ask));
    }

    /** Odin answers a question: a line of his own, with nothing more to choose (the screen is closed with Esc). */
    static void answer(Ctx ctx, String line) {
        Net.sendToPlayer(ctx.player(), new AsgardTalkPayload.Open(ctx.speaker().getId(), Role.ODIN.ordinal(),
                "npc.enhanced.odin." + line, List.of(), List.of()));
    }

    private OdinDialogue() {
    }
}

package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Ctx;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Node;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Option;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.List;
import java.util.function.Predicate;

/**
 * What the Heimdall of the Observatory says (3.5): his nodes in the Asgardians' dialogue table ({@link AsgardDialogue},
 * the role {@link Role#HEIMDALL}). He greets the newcomer, offers the Guardian's Watch, tells the winner how Hofund makes a
 * Heimdall, gives a new Hofund to one who lost his, and sends anyone home or back where he came from by the Bifrost (no key,
 * no cooldown: {@link Bifrost.Source#NPC}). Each node offers at most three answers. Lines: {@code npc.enhanced.heimdall.<node>};
 * answers: {@code npc.enhanced.option.heimdall_<key>}.
 *
 * <p>The nodes, highest priority first: {@code watch_running} (his Watch runs), {@code post_kept} (3.5.1: he keeps
 * Heimdall's post, {@link HeimdallPost}), {@code handover} (he is Heimdall: the post is handed to him), {@code greeting}
 * (first meeting), {@code hold_hofund} (he won the Watch and has no power yet), {@code fellow} (he is Heimdall),
 * {@code keeper_away} (another player keeps the post: still the Watch, home and back), {@code watch_offer} (he may
 * take the Watch), {@code watch_rest} (he lost it and must rest), {@code watch_done} (won, and another power) and
 * {@code idle} (always).
 */
@EventBusSubscriber(modid = Enhanced.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class HeimdallTalk {
    private static boolean registered;

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        register();
    }

    // ---- the answers ----

    public static final String WATCH = "heimdall_watch";
    public static final String NOT_YET = "heimdall_not_yet";
    public static final String HOME = "heimdall_home";
    public static final String BACK = "heimdall_back";
    public static final String WHO = "heimdall_who";
    public static final String GIVE_UP = "heimdall_give_up";
    public static final String HOFUND = "heimdall_hofund";
    /** 3.5.1, Heimdall's post: the keeper's answers. */
    public static final String POST_THANKS = "heimdall_post_thanks";
    public static final String POST_RETURN = "heimdall_post_return";
    public static final String POST_KEEP = "heimdall_post_keep";

    private static boolean progress(Ctx c, int flag) {
        return c.progress().has(flag);
    }

    private static final Predicate<Ctx> ALWAYS = c -> true;
    private static final Predicate<Ctx> RUNNING = c -> GuardiansWatch.watching(c.player());
    private static final Predicate<Ctx> MAY_WATCH = c -> GuardiansWatch.refusal(c.player()) == null;
    private static final Predicate<Ctx> CAME_FROM = c -> c.progress().cameFrom() != null;
    private static final Predicate<Ctx> LOST_HOFUND = c -> progress(c, AsgardProgress.WATCH_WON) && !HofundBond.hasOwn(c.player());

    private static Option home() {
        return new Option(HOME, ALWAYS, c -> {
            met(c.player());
            Bifrost.open(c.player(), Bifrost.Destination.HOME, Bifrost.Source.NPC);
        });
    }

    private static Option back() {
        return new Option(BACK, CAME_FROM, c -> {
            met(c.player());
            Bifrost.open(c.player(), Bifrost.Destination.CAME_FROM, Bifrost.Source.NPC);
        });
    }

    private static Option watch() {
        return new Option(WATCH, MAY_WATCH, c -> {
            met(c.player());
            GuardiansWatch.start(c.player());
        });
    }

    private static Option notYet() {
        return new Option(NOT_YET, ALWAYS, c -> met(c.player()));
    }

    private static Option who() {
        return new Option(WHO, ALWAYS, c -> {
            met(c.player());
            say(c.player(), "npc.enhanced.heimdall.lore");
        });
    }

    private static Option giveUp() {
        return new Option(GIVE_UP, RUNNING, c -> GuardiansWatch.lose(c.player(), "message.enhanced.watch.given_up"));
    }

    private static Option hofund() {
        return new Option(HOFUND, LOST_HOFUND, c -> {
            HofundBond.give(c.player());
            say(c.player(), "npc.enhanced.heimdall.hofund_again");
        });
    }

    /** Adds Heimdall's nodes to the table (once). */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        node("watch_running", RUNNING, 100, giveUp());
        // 3.5.1, Heimdall's post: the keeper may give it back; a player who is Heimdall is handed it (the line says
        // "We are both guardians of Asgard"); others hear who keeps it.
        node("post_kept", c -> HeimdallPost.keeps(c.player()), 95,
                new Option(POST_RETURN, ALWAYS, c -> HeimdallPost.end(c.player(), true)), Option.close(POST_KEEP));
        node("handover", c -> c.npc() != null && !c.npc().summoned() && HeimdallPost.mayHandOver(c.player()), 85,
                Option.close(POST_THANKS));
        AsgardDialogue.onShow(Role.HEIMDALL, "handover", c -> {
            if (c.npc() != null) {
                HeimdallPost.handOver(c.player(), c.npc());
            }
        });
        node("keeper_away", c -> HeimdallPost.keptByAnother(c.player()), 55, watch(), home(), back());
        AsgardDialogue.args(Role.HEIMDALL, "keeper_away", c -> List.of(HeimdallPost.keeperName(c.player().server)));
        AsgardDialogue.lines("npc.enhanced.heimdall.post_return", "boss.enhanced.post", "boss.enhanced.post.time",
                "message.enhanced.post.begins", "message.enhanced.post.socket_nobody");
        node("greeting", c -> !progress(c, AsgardProgress.MET_HEIMDALL), 80, watch(), who(), home());
        node("hold_hofund", c -> progress(c, AsgardProgress.WATCH_WON) && Powers.get(c.player()).power() == Power.NONE, 70,
                hofund(), home(), back());
        node("fellow", c -> Powers.get(c.player()).power() == Power.HEIMDALL, 60, home(), back(), who());
        node("watch_offer", MAY_WATCH, 50, watch(), home(), back());
        node("watch_rest", c -> !progress(c, AsgardProgress.WATCH_WON) && c.player().server.overworld().getGameTime()
                < c.progress().watchReadyAt(), 40, home(), back(), who());
        node("watch_done", c -> progress(c, AsgardProgress.WATCH_WON), 30, hofund(), home(), back());
        node("idle", ALWAYS, 0, home(), back(), who());
    }

    private static void node(String key, Predicate<Ctx> when, int priority, Option... options) {
        AsgardDialogue.register(Role.HEIMDALL, new Node(key, when, priority, List.of(options)));
    }

    /** He has met Heimdall (the greeting is said once). */
    private static void met(ServerPlayer player) {
        AsgardProgress progress = ModAttachments.ASGARD.get(player);
        if (!progress.has(AsgardProgress.MET_HEIMDALL)) {
            ModAttachments.ASGARD.set(player, progress.with(AsgardProgress.MET_HEIMDALL));
        }
    }

    /** A line of Heimdall's in the chat, to the player alone. */
    private static void say(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("npc.enhanced.heimdall.says", Component.translatable(key))
                .withStyle(ChatFormatting.GOLD));
    }

    private HeimdallTalk() {
    }
}

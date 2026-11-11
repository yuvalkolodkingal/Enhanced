package com.kolodkingal.enhanced.mind;

import com.kolodkingal.enhanced.network.MindTalkPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The talks of Vision's mind (3.12): scenes of lines, each said by someone ({@code speaker}: a {@link
 * MindNpcEntity.Role}'s id, {@code you} or {@code narrator}) and answered by one of a few options. An option leads to
 * the next line, ends the talk ({@code ""}) or does something ({@code "!action"}, {@link MindQuest#act}). Lines are lang
 * keys {@code mind.enhanced.<scene>.<line>}, options {@code mind.enhanced.<scene>.<line>.<n>}. A forced talk (Ultron's,
 * the Human Torch's) cannot be closed: it goes on until its end.
 */
public final class MindTalk {
    record Option(String next) {
    }

    record Node(String speaker, List<Option> options) {
    }

    record Session(String scene, String node, int npcId, boolean forced) {
    }

    private static final Map<String, Map<String, Node>> SCENES = new HashMap<>();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private MindTalk() {
    }

    // ---- the scripts ----

    private static final class Script {
        final Map<String, Node> nodes = new LinkedHashMap<>();

        Script line(String id, String speaker, String... next) {
            List<Option> options = new ArrayList<>();
            for (String n : next) {
                options.add(new Option(n));
            }
            nodes.put(id, new Node(speaker, options));
            return this;
        }
    }

    private static void scene(String name, Script script) {
        SCENES.put(name, script.nodes);
    }

    static {
        // Dum-E, at the door: what the place is
        scene("dum_e", new Script()
                .line("start", "dum_e", "where", "floors", "leave", "")
                .line("where", "dum_e", "floors", "leave", "")
                .line("floors", "dum_e", "where", "leave", "")
                .line("leave", "dum_e", "where", "floors", ""));
        // E.D.I.T.H., in the kitchen
        scene("edith", new Script()
                .line("start", "edith", "who", "what", "")
                .line("who", "edith", "what", "")
                .line("what", "edith", "who", ""));
        // F.R.I.D.A.Y., across from the cinema
        scene("friday", new Script()
                .line("start", "friday", "cinema", "gift", "")
                .line("cinema", "friday", "gift", "")
                .line("gift", "friday", "cinema", ""));
        // the riddle robots on the top floor: three questions; one robot's answer and the gate stays shut
        scene("robot", new Script()
                .line("start", "robot", "q1")
                .line("q1", "robot", "fail", "q2")
                .line("q2", "robot", "fail", "q3")
                .line("q3", "robot", "fail", "pass")
                .line("fail", "robot", "")
                .line("pass", "robot", "!open_gate"));
        scene("robot_after", new Script().line("start", "robot", ""));
        // Ultron, on the sofa below: he cannot be sent away
        scene("ultron", new Script()
                .line("start", "narrator", "here")
                .line("here", "ultron", "why")
                .line("why", "ultron", "!down"));
        // the Human Torch and Dr. Phineas Horton, at the bottom
        String[][] torch = {
                {"start", "narrator"}, {"flames", "narrator"}, {"first", "torch"}, {"source", "torch"},
                {"horton", "horton"}, {"distract", "torch"}, {"forgive", "horton"}, {"understatement", "torch"},
                {"fearless", "horton"}, {"artificial", "torch"}, {"hero", "horton"}, {"burned", "torch"},
                {"something", "torch"}, {"have_it", "torch"}, {"where", "torch"}, {"question", "torch"},
                {"lie", "torch"}, {"afraid", "torch"}, {"gift", "torch"}, {"son", "torch"}, {"why", "torch"},
                {"why2", "torch"}, {"lost", "torch"}, {"now", "torch"}, {"healed", "torch"}};
        Script s = new Script();
        for (int i = 0; i < torch.length; i++) {
            String next = i + 1 < torch.length ? torch[i + 1][0] : "!source_code";
            // (two lines do something as they end: the flames go out, and Horton appears)
            if (torch[i][0].equals("flames")) {
                next = "!quench";
            } else if (torch[i][0].equals("source")) {
                next = "!horton";
            }
            s.line(torch[i][0], torch[i][1], next);
        }
        scene("torch", s);
        // Nano Ultron, at half his health, to Vision
        scene("nano_question", new Script().line("start", "ultron", "!nano_go_on", "!nano_peace"));
        scene("torch_after", new Script().line("start", "torch", ""));
        scene("horton_after", new Script().line("start", "horton", ""));
    }

    /** The line that follows an action, in the scenes where one goes on (the flames, Horton). */
    static String afterAction(String scene, String action) {
        if (!scene.equals("torch")) {
            return null;
        }
        return switch (action) {
            case "quench" -> "first";
            case "horton" -> "horton";
            default -> null;
        };
    }

    // ---- talking ----

    /** The player used someone of the mind. */
    public static void talkTo(ServerPlayer player, MindNpcEntity npc) {
        if (SESSIONS.containsKey(player.getUUID()) && SESSIONS.get(player.getUUID()).forced()) {
            return;
        }
        MindProgress progress = MindProgress.of(player);
        String scene = switch (npc.role()) {
            case DUM_E -> "dum_e";
            case EDITH -> "edith";
            case FRIDAY -> "friday";
            case ROBOT -> progress.gateOpen() ? "robot_after" : "robot";
            case ULTRON -> "ultron";
            case TORCH -> progress.torchDone() ? "torch_after" : "torch";
            case HORTON -> "horton_after";
        };
        start(player, scene, npc, scene.equals("ultron") || scene.equals("torch"));
    }

    /** Opens a scene at its start. */
    public static void start(ServerPlayer player, String scene, Entity npc, boolean forced) {
        Session session = new Session(scene, "start", npc == null ? -1 : npc.getId(), forced);
        SESSIONS.put(player.getUUID(), session);
        open(player, session);
    }

    static void open(ServerPlayer player, Session session) {
        Node node = SCENES.get(session.scene()).get(session.node());
        List<String> options = new ArrayList<>();
        for (int i = 0; i < node.options().size(); i++) {
            options.add(node.options().get(i).next().isEmpty() ? "bye" : String.valueOf(i));
        }
        Net.sendToPlayer(player, new MindTalkPayload.Open(session.npcId(), node.speaker(),
                session.scene() + "." + session.node(), options, session.forced()));
    }

    /** The player chose an option the screen offered ({@code ""}: closed the screen). */
    public static void choose(ServerPlayer player, String option) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (session.scene().equals("elevator")) {
            if (option.isEmpty()) {
                SESSIONS.remove(player.getUUID());
            } else {
                chooseFloor(player, option);
            }
            return;
        }
        Node node = SCENES.get(session.scene()).get(session.node());
        int index = -1;
        if (option.equals("bye")) {
            for (int i = 0; i < node.options().size(); i++) {
                if (node.options().get(i).next().isEmpty()) {
                    index = i;
                }
            }
        } else {
            try {
                index = Integer.parseInt(option);
            } catch (NumberFormatException e) {
                index = -1;
            }
        }
        if (index < 0 || index >= node.options().size()) {
            if (!session.forced()) {
                SESSIONS.remove(player.getUUID());
            } else {
                open(player, session);
            }
            return;
        }
        String next = node.options().get(index).next();
        if (next.isEmpty()) {
            SESSIONS.remove(player.getUUID());
            Net.sendToPlayer(player, MindTalkPayload.Open.close());
            return;
        }
        if (next.startsWith("!")) {
            String action = next.substring(1);
            String after = afterAction(session.scene(), action);
            Entity npc = player.serverLevel().getEntity(session.npcId());
            MindQuest.act(player, action, npc);
            if (after != null) {
                Session cont = new Session(session.scene(), after, session.npcId(), session.forced());
                SESSIONS.put(player.getUUID(), cont);
                open(player, cont);
            } else {
                SESSIONS.remove(player.getUUID());
                Net.sendToPlayer(player, MindTalkPayload.Open.close());
            }
            return;
        }
        Session cont = new Session(session.scene(), next, session.npcId(), session.forced());
        SESSIONS.put(player.getUUID(), cont);
        open(player, cont);
    }

    /** The elevator: its own little talk, the floors the player may go to now. */
    public static void elevator(ServerPlayer player) {
        List<String> floors = MindQuest.floors(player);
        SESSIONS.put(player.getUUID(), new Session("elevator", "start", -1, false));
        Net.sendToPlayer(player, new MindTalkPayload.Open(-1, "elevator", "elevator.start", floors, false));
    }

    /** The elevator's choice (a floor's id). */
    public static void chooseFloor(ServerPlayer player, String floor) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null || !session.scene().equals("elevator")) {
            return;
        }
        Net.sendToPlayer(player, MindTalkPayload.Open.close());
        MindQuest.ride(player, floor);
    }

    /** Every lang key the scripts show (lines, options, speakers), for the tests. */
    public static List<String> keys() {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Map<String, Node>> scene : SCENES.entrySet()) {
            for (Map.Entry<String, Node> node : scene.getValue().entrySet()) {
                String line = scene.getKey() + "." + node.getKey();
                keys.add("mind.enhanced." + line);
                keys.add("mind.enhanced.speaker." + node.getValue().speaker());
                for (int i = 0; i < node.getValue().options().size(); i++) {
                    keys.add(node.getValue().options().get(i).next().isEmpty() ? "mind.enhanced.bye" : "mind.enhanced." + line + "." + i);
                }
            }
        }
        return keys;
    }

    public static boolean talking(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static void clear() {
        SESSIONS.clear();
    }
}

package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Ctx;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Option;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The lines of every Asgardian but the nodes the other lanes add (Heimdall's whole table: L2; Odin's whole table: L3;
 * Hela's warnings: L4). Our own words, never the films'. The chain of hints: Frigga, Hogun, Valkyrie and the citizens
 * speak of Hela's Seal behind the throne and of her army; Sif and the Einherjar of Heimdall's Watch (and
 * Hofund); Frigga, Fandral, Hogun and the citizens of Gungnir in the vault, for the one who faces Hela.
 */
final class AsgardLines {
    /** How long Frigga waits before she heals the same player again (5 minutes). */
    static final int HEAL_COOLDOWN = 6000;
    private static final Map<UUID, Long> HEALED = new HashMap<>();

    /** The places the Einherjar give directions to: lang id and where. */
    record Place(String id, BlockPos pos) {
    }

    static final List<Place> PLACES = List.of(
            new Place("palace", CityPlan.THRONE_ROOM_CENTER),
            new Place("observatory", CityPlan.OBSERVATORY_CENTER),
            new Place("training", CityPlan.TRAINING_RING),
            new Place("garden", CityPlan.FRIGGA_GARDEN),
            new Place("feast", CityPlan.FEAST_HALL),
            new Place("stables", CityPlan.STABLES),
            new Place("bridge", CityPlan.BRIDGE_HEAD));

    private AsgardLines() {
    }

    static void registerAll() {
        common();
        frigga();
        sif();
        valkyrie();
        volstagg();
        fandral();
        hogun();
        einherjar();
        citizen();
    }

    // ---- shared ----

    private static Predicate<Ctx> power(Power power) {
        return ctx -> ctx.power() == power;
    }

    private static Predicate<Ctx> not(int flag) {
        return ctx -> !ctx.has(flag);
    }

    private static void common() {
        AsgardDialogue.variants("npc.enhanced.common.manners", 4);
        AsgardDialogue.lines("npc.enhanced.common.manners", "npc.enhanced.common.busy", "npc.enhanced.common.down",
                "npc.enhanced.sif.duel_score", "npc.enhanced.sif.duel_won", "npc.enhanced.sif.duel_lost", "npc.enhanced.sif.duel_draw",
                "npc.enhanced.frigga.odin_asleep", "npc.enhanced.valkyrie.horse",
                "npc.enhanced.screen.farewell", "item.enhanced.sif_token", "item.enhanced.asgard_mead",
                "item.enhanced.valkyrie_horse");
        for (Place place : PLACES) {
            AsgardDialogue.lines("npc.enhanced.place." + place.id());
        }
        for (String dir : List.of("n", "ne", "e", "se", "s", "sw", "w", "nw")) {
            AsgardDialogue.lines("npc.enhanced.dir." + dir);
        }
        for (Role role : Role.values()) {
            AsgardDialogue.lines(role.nameKey());
            if (role.named()) {
                AsgardDialogue.lines(AsgardDialogue.lineKey(role, "manners"));
            }
        }
    }

    /**
     * The nodes every role has: the fight with Hela (priority 50) and the reactions to the player's power (30), each with
     * the role's own line where it has one ({@code mine}), else the shared line; both offer the role's usual answers.
     */
    private static void reactions(Role role, List<Option> answers, List<Power> mine) {
        AsgardDialogue.node(role, role.warrior() ? "common.fight_warrior" : "common.fight_citizen")
                .when(Ctx::helaFight).priority(50).register();
        for (Power power : List.of(Power.THOR, Power.LOKI, Power.HELA, Power.ODIN, Power.HEIMDALL)) {
            String id = "react_" + power.getSerializedName();
            AsgardDialogue.node(role, mine.contains(power) ? id : "common." + id)
                    .when(power(power)).priority(30).options(answers).register();
        }
    }

    private static Option trade() {
        return new Option("trade", ctx -> ctx.npc() != null && ctx.role().trader(), ctx -> {
            if (ctx.npc() != null) {
                ctx.npc().openTrades(ctx.player());
            }
        });
    }

    // ---- Frigga ----

    private static boolean mayHeal(Ctx ctx) {
        Long last = HEALED.get(ctx.player().getUUID());
        return ctx.hurt() && (last == null || ctx.player().level().getGameTime() - last >= HEAL_COOLDOWN);
    }

    private static void frigga() {
        Role role = Role.FRIGGA;
        List<Option> answers = List.of(
                new Option("about_odin", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx, ctx.state().odinAwake() ? "odin_awake" : "odin_sleeps")),
                Option.to("about_seal", "seal"),
                new Option("heal_me", AsgardLines::mayHeal, ctx -> {
                    HEALED.put(ctx.player().getUUID(), ctx.player().level().getGameTime());
                    ctx.player().addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                    AsgardTalk.show(ctx, "heal");
                }));
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(3).options(answers).register();
        AsgardDialogue.node(role, "greeting_first").when(not(AsgardProgress.TALKED_FRIGGA)).priority(20).options(answers).register();
        AsgardDialogue.node(role, "after_hela").when(ctx -> ctx.state().helaDefeats() > 0 && !ctx.has(AsgardProgress.GUNGNIR_TAKEN))
                .priority(10).options(answers).register();
        AsgardDialogue.node(role, "odin_sleeps").option(Option.to("about_seal", "seal")).option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "odin_awake").option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "seal").option(Option.to("about_gungnir", "gungnir")).option(Option.close("i_promise")).register();
        AsgardDialogue.node(role, "gungnir").option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "heal").option(Option.close("thanks")).register();
        reactions(role, answers, List.of(Power.THOR, Power.LOKI, Power.HELA, Power.ODIN, Power.HEIMDALL));
    }

    /** Forgets Frigga's healing (the server stops). */
    static void clear() {
        HEALED.clear();
    }

    // ---- Sif ----

    private static void sif() {
        Role role = Role.SIF;
        List<Option> answers = List.of(Option.to("train", "train"), trade(), Option.to("ask_advice", "advice"));
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(3).options(answers).register();
        AsgardDialogue.node(role, "greeting_first").when(not(AsgardProgress.TALKED_SIF)).priority(20).options(answers).register();
        AsgardDialogue.node(role, "train")
                .option("ready", ctx -> ctx.npc() != null, ctx -> AsgardianGoals.SparPlayer.begin(ctx.npc(), ctx.player()))
                .option(Option.close("later"))
                .register();
        AsgardDialogue.node(role, "advice").option(Option.close("thanks")).register();
        reactions(role, answers, List.of(Power.THOR, Power.LOKI, Power.HELA));
    }

    // ---- Valkyrie ----

    private static void valkyrie() {
        Role role = Role.VALKYRIE;
        List<Option> answers = List.of(Option.to("about_army", "army"), trade(), Option.to("about_hela", "hela"));
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(3).options(answers).register();
        AsgardDialogue.node(role, "greeting_first").when(not(AsgardProgress.TALKED_VALKYRIE)).priority(20).options(answers).register();
        AsgardDialogue.node(role, "army").option(Option.to("advice_q", "bring_friends")).option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "bring_friends").option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "hela").option(Option.to("advice_q", "bring_friends")).option(Option.close("thanks")).register();
        reactions(role, answers, List.of(Power.THOR, Power.HELA));
    }

    // ---- the Warriors Three ----

    private static void volstagg() {
        Role role = Role.VOLSTAGG;
        List<Option> answers = List.of(trade(), Option.to("joke_please", "joke"), Option.to("about_warriors", "warriors"));
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(4).options(answers).register();
        AsgardDialogue.node(role, "greeting_first").when(not(AsgardProgress.TALKED_WARRIORS)).priority(20).options(answers).register();
        AsgardDialogue.node(role, "joke").variants(6).option(Option.to("another_joke", "joke")).option(Option.close("laugh")).register();
        AsgardDialogue.node(role, "warriors").option(Option.close("thanks")).register();
        reactions(role, answers, List.of(Power.THOR, Power.LOKI));
    }

    private static void fandral() {
        Role role = Role.FANDRAL;
        List<Option> answers = List.of(Option.to("about_city", "city"), Option.to("about_bridge", "bridge"),
                Option.to("about_gungnir", "gungnir"));
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(4).options(answers).register();
        AsgardDialogue.node(role, "greeting_first").when(not(AsgardProgress.TALKED_WARRIORS)).priority(20).options(answers).register();
        AsgardDialogue.node(role, "city").option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "bridge").option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "gungnir").option(Option.close("thanks")).register();
        reactions(role, answers, List.of(Power.THOR, Power.LOKI));
    }

    private static void hogun() {
        Role role = Role.HOGUN;
        List<Option> answers = List.of(Option.to("about_vault", "vault"), Option.to("about_hela", "hela"));
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(3).options(answers).register();
        AsgardDialogue.node(role, "greeting_first").when(not(AsgardProgress.TALKED_WARRIORS)).priority(20).options(answers).register();
        AsgardDialogue.node(role, "vault").option(Option.to("why_sealed", "vault_more")).option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "vault_more").option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "hela").option(Option.close("thanks")).register();
        reactions(role, answers, List.of(Power.THOR, Power.LOKI));
    }

    // ---- the Einherjar and the citizens ----

    private static void einherjar() {
        Role role = Role.EINHERJAR;
        List<Option> answers = List.of(Option.to("directions", "directions"), Option.to("any_news", "news"));
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(4).options(answers).register();
        AsgardDialogue.node(role, "directions").args(AsgardLines::directions).option(Option.to("any_news", "news"))
                .option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "news").variants(6).option(Option.close("thanks")).register();
        reactions(role, answers, List.of());
    }

    private static void citizen() {
        Role role = Role.CITIZEN;
        List<Option> answers = List.of(Option.to("any_rumours", "rumour"));
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(6).options(answers).register();
        AsgardDialogue.node(role, "rumour").variants(20).option(Option.to("another_rumour", "rumour")).option(Option.close("thanks"))
                .register();
        reactions(role, answers, List.of());
    }

    /**
     * The Einherjar's directions: the nearest of the city's places more than 12 blocks away (its name, the steps, the
     * direction) and the one after it.
     */
    static List<String> directions(Ctx ctx) {
        Vec3 at = ctx.player().position();
        List<Place> far = new ArrayList<>();
        for (Place place : PLACES) {
            if (Vec3.atBottomCenterOf(place.pos()).distanceToSqr(at) > 12 * 12) {
                far.add(place);
            }
        }
        far.sort(Comparator.comparingDouble(p -> Vec3.atBottomCenterOf(p.pos()).distanceToSqr(at)));
        Place first = far.get(0);
        Place second = far.size() > 1 ? far.get(1) : first;
        Vec3 to = Vec3.atBottomCenterOf(first.pos());
        int steps = (int) Math.round(Math.sqrt(to.subtract(at).horizontalDistanceSqr()));
        return List.of("@npc.enhanced.place." + first.id(), Integer.toString(steps),
                "@npc.enhanced.dir." + direction(to.x - at.x, to.z - at.z), "@npc.enhanced.place." + second.id());
    }

    /** The compass direction of a step (dx east, dz south): n, ne, e, se, s, sw, w, nw. */
    static String direction(double dx, double dz) {
        double angle = Mth.atan2(dx, -dz) * Mth.RAD_TO_DEG; // 0 = north, 90 = east
        int sector = Math.floorMod((int) Math.round(angle / 45.0), 8);
        return List.of("n", "ne", "e", "se", "s", "sw", "w", "nw").get(sector);
    }
}

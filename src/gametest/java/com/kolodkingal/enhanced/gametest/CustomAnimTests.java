package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.custom.CustomAnimRoutes;
import com.kolodkingal.enhanced.client.custom.HeroAbilityAnims;
import com.kolodkingal.enhanced.client.render.FirstPersonMoves;
import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.RunCharacter;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.custom.AnimCatalog;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The animation of the custom hero (3.4, lane L5), its pure tables: the defaults of the thirty modules (Appendix A of the
 * plan), the gestures and stances of every built-in hero's ability and that its hero's own routes play them, the gesture a
 * cast marker plays for a binding (the hero's, the module's, the definition's move), who owns each synced bit in a kit,
 * the kit filter, the flight styles, run characters, speedster runs and idles of the editor, and the first-person rows
 * of every move of the catalogue. No client class beyond the pure enums and tables.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CustomAnimTests {
    private static final String BATCH = "custom_anim";

    /** A kit of these entries (each at strength 5 in its default option, on no key), not judged by the rules. */
    private static HeroKit kit(String... ids) {
        CustomKitSupport.Def def = CustomKitSupport.def();
        for (String id : ids) {
            def.pick(id, 5);
        }
        return HeroKit.of(def.raw());
    }

    /** Every option of every module has a default move, and a held one a stance; each gesture has its keys. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everyModuleOptionHasItsDefaultMoveAndStance(GameTestHelper helper) {
        for (PowerModule module : PowerModule.values()) {
            for (PowerModule.Option option : module.options()) {
                String where = module.id + "#" + option.id();
                GestureId on = CustomAnimRoutes.defaultGesture(module, option.id(), true);
                GestureId off = CustomAnimRoutes.defaultGesture(module, option.id(), false);
                switch (option.act()) {
                    // (Telekinesis is acted by its grab and throw, Flight by the take-off of its style.)
                    case PRESS -> check(on != null || module == PowerModule.TELEKINESIS, where + " plays nothing when pressed");
                    case HOLD -> {
                        check(on != null, where + " plays nothing when held");
                        check(CustomAnimRoutes.defaultStance(module, option.id()) != null, where + " has no stance while held");
                    }
                    case TOGGLE -> check(on != null || module == PowerModule.FLIGHT, where + " plays nothing when switched on");
                    case NONE -> check(on == null && off == null, where + " is passive and plays nothing");
                }
            }
        }
        // Appendix A, a few rows exactly.
        check(CustomAnimRoutes.defaultGesture(PowerModule.SUPER_STRENGTH, "slam", true) == GestureId.THANOS_SLAM, "the slam");
        check(CustomAnimRoutes.defaultGesture(PowerModule.PHASING, "phase", false) == GestureId.VISION_PHASE_OUT, "phasing off");
        check(CustomAnimRoutes.defaultStance(PowerModule.FORCE_FIELD, "reflect") == Stance.SHIELD_WANDA, "the reflecting field");
        check(CustomAnimRoutes.defaultStance(PowerModule.FIRE, "aura") == Stance.THOR_UNLEASHED, "the fire aura");
        helper.succeed();
    }

    /**
     * Every built-in hero's ability has its row; what the cast marker plays for it is what its hero's cooldown plays, and
     * what its own routes play is what its hero's routes give for the bits it owns.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everyHeroAbilityIsAnimatedAsItsHeroAnimatesIt(GameTestHelper helper) {
        for (HeroAbility a : HeroAbility.values()) {
            check(HeroAbilityAnims.covers(a), a.id() + " has no animation row");
            Set<GestureId> byCooldown = EnumSet.noneOf(GestureId.class);
            if (a.home >= 1 && a.home <= 5) {
                for (long left = 1; left <= 3000; left++) {
                    GestureId id = GestureRoutes.cooldown(a.hero, a.home, left);
                    if (id != null) {
                        byCooldown.add(id);
                    }
                }
            }
            for (boolean alt : new boolean[]{false, true}) {
                GestureId cast = alt ? HeroAbilityAnims.castAlt(a) : HeroAbilityAnims.castMain(a);
                GestureId shown = HeroAbilityAnims.shown(a, alt);
                String where = a.id() + (alt ? " (its Sneak variant)" : "");
                if (cast != null) {
                    check(byCooldown.contains(cast), where + ": " + cast + " is not what its hero's cooldown plays");
                    check(cast == shown, where + ": the cast is the gesture shown");
                } else if (shown != null) {
                    check(routed(a, shown), where + ": " + shown + " is not what its hero's routes play for what it owns");
                }
                if (alt) {
                    check(shown == null || a.hasAlt(), where + ": a variant's gesture for an ability without one");
                }
            }
            for (GestureId id : HeroAbilityAnims.gestures(a)) {
                check(HeroAbilityAnims.allows(kit(a.id()), id), a.id() + ": " + id + " is filtered out of its own kit");
            }
            GestureId late = HeroAbilityAnims.late(a);
            check(late == null || byCooldown.contains(late), a.id() + ": its late gesture is its hero's cooldown's");
        }
        // The rows the plan names.
        check(HeroAbilityAnims.castMain(HeroAbility.THANOS_POWER) == GestureId.THANOS_BLAST
                && HeroAbilityAnims.castAlt(HeroAbility.THANOS_POWER) == GestureId.THANOS_SLAM, "Thanos's blast and slam");
        check(HeroAbilityAnims.castMain(HeroAbility.QUICKSILVER_BLITZ) == GestureId.QS_BLITZ
                && HeroAbilityAnims.castAlt(HeroAbility.QUICKSILVER_BLITZ) == GestureId.QS_SNATCH, "the blitz and the pickpocket");
        check(HeroAbilityAnims.castMain(HeroAbility.CYCLOPS_GLASSES) == GestureId.CYCLOPS_GLASSES_ON
                && HeroAbilityAnims.castAlt(HeroAbility.CYCLOPS_GLASSES) == GestureId.CYCLOPS_GLASSES_OFF, "the glasses");
        check(HeroAbilityAnims.castMain(HeroAbility.THOR_LIGHTNING) == null, "the lightning is acted by its state");
        check(HeroAbilityAnims.stances(HeroAbility.THOR_LIGHTNING).contains(Stance.THOR_BEAM), "the lightning's stance");
        helper.succeed();
    }

    /** Whether one of its hero's routes plays {@code id} for a bit the ability owns (or something else starts it). */
    private static boolean routed(HeroAbility a, GestureId id) {
        Power hero = a.hero;
        for (Toggle t : Toggle.values()) {
            if (HeroAbilityAnims.owners(t).contains(a) && (GestureRoutes.toggle(hero, t, true) == id || GestureRoutes.toggle(hero, t, false) == id)) {
                return true;
            }
        }
        for (GestureRoutes.Flag f : GestureRoutes.Flag.values()) {
            if (HeroAbilityAnims.owners(f).contains(a)
                    && (GestureRoutes.flag(hero, f, true, false, false) == id || GestureRoutes.flag(hero, f, false, false, false) == id)) {
                return true;
            }
        }
        for (Live l : Live.values()) {
            if (HeroAbilityAnims.owners(l).contains(a) && (GestureRoutes.live(hero, l, true) == id || GestureRoutes.live(hero, l, false) == id)) {
                return true;
            }
        }
        if (GestureRoutes.grab(hero) == id || GestureRoutes.startedElsewhere(id)) {
            return true;
        }
        for (GestureRoutes.Hammer h : GestureRoutes.Hammer.values()) {
            if (GestureRoutes.hammer(h) == id && a == HeroAbility.THOR_HAMMER) {
                return true;
            }
        }
        for (GestureRoutes.HexChange h : GestureRoutes.HexChange.values()) {
            if (GestureRoutes.hex(h) == id && a == HeroAbility.WANDA_HEX) {
                return true;
            }
        }
        return false;
    }

    /** The marker of a binding plays the hero's gesture for its main action and its variant, a module's default, or the move chosen. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theCastOfABindingIsItsHerosItsModulesOrItsMove(GameTestHelper helper) {
        CustomHero hero = CustomKitSupport.def()
                .pick("thanos.power", 5).key(1, "thanos.power")
                .pick("energy_blast", 5, "bolts").key(2, "energy_blast")
                .pick("phasing", 5).key(3, "phasing")
                .pick("thor.lightning", 5).key(4, "thor.lightning")
                .build();
        HeroKit kit = HeroKit.of(hero);
        check(kit.sneak(1) != null && kit.sneak(1).isAlt(), "the variant sits on Sneak + R by itself");
        check(CustomAnimRoutes.cast(hero, kit, 1, false) == GestureId.THANOS_BLAST, "R: the Power Blast");
        check(CustomAnimRoutes.cast(hero, kit, 1, true) == GestureId.THANOS_SLAM, "Sneak + R: the Titan Slam");
        check(CustomAnimRoutes.cast(hero, kit, 2, false) == GestureId.BILLY_BOLT, "G: the bolts' hand thrust");
        check(CustomAnimRoutes.cast(hero, kit, 3, false, true) == GestureId.VISION_PHASE_IN, "V on: phasing in");
        check(CustomAnimRoutes.cast(hero, kit, 3, false, false) == GestureId.VISION_PHASE_OUT, "V off: phasing out");
        check(CustomAnimRoutes.cast(hero, kit, 4, false) == null, "B: the lightning plays its own route");
        check(CustomAnimRoutes.cast(hero, kit, 5, false) == null, "N is free");
        // The moves of the definition win, "none" too.
        CustomHero moved = hero.withMove(1, false, "loki_cast").withMove(1, true, CustomHero.NONE).withMove(4, false, "thor_summon");
        HeroKit movedKit = HeroKit.of(moved);
        check(CustomAnimRoutes.cast(moved, movedKit, 1, false) == GestureId.LOKI_CAST, "R plays the move chosen");
        check(CustomAnimRoutes.cast(moved, movedKit, 1, true) == null, "Sneak + R plays none");
        check(CustomAnimRoutes.chosen(moved, 1, true) && !CustomAnimRoutes.chosen(moved, 2, false), "a chosen move is told apart from auto");
        check(CustomAnimRoutes.cast(moved, movedKit, 4, false) == GestureId.THOR_SUMMON, "a routed ability plays the move chosen");
        check(HeroAbilityAnims.allows(movedKit, GestureId.THOR_SUMMON), "a chosen move passes the kit filter");
        helper.succeed();
    }

    /** Every bit an ability owns resolves to it in a kit that has it, and to nothing in one that has not. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everyBitResolvesToItsOwnerInAKit(GameTestHelper helper) {
        HeroKit empty = kit("flight");
        for (Live live : Live.values()) {
            for (HeroAbility a : HeroAbilityAnims.owners(live)) {
                check(HeroAbilityAnims.ownerOf(live, kit(a.id())) == a, live + " belongs to " + a.id());
            }
            check(HeroAbilityAnims.ownerOf(live, empty) == null, live + " belongs to nobody in a kit of Flight");
        }
        for (Toggle toggle : Toggle.values()) {
            check(!HeroAbilityAnims.owners(toggle).isEmpty(), "the toggle " + toggle + " has an owner");
            for (HeroAbility a : HeroAbilityAnims.owners(toggle)) {
                check(HeroAbilityAnims.ownerOf(toggle, kit(a.id())) == a, toggle + " belongs to " + a.id());
                check(GestureRoutes.toggle(a.hero, toggle, true) != null || GestureRoutes.toggle(a.hero, toggle, false) != null,
                        toggle + " is routed for " + a.hero);
            }
            check(HeroAbilityAnims.ownerOf(toggle, empty) == null, toggle + " belongs to nobody in a kit of Flight");
        }
        for (GestureRoutes.Flag flag : GestureRoutes.Flag.values()) {
            check(!HeroAbilityAnims.owners(flag).isEmpty(), "the flag " + flag + " has an owner");
            for (HeroAbility a : HeroAbilityAnims.owners(flag)) {
                check(HeroAbilityAnims.ownerOf(flag, kit(a.id())) == a, flag + " belongs to " + a.id());
            }
        }
        // The shield of a kit is its owner's; the order of the list decides only between abilities the rules keep apart.
        check(HeroAbilityAnims.ownerOf(GestureRoutes.Flag.CHAOS_SHIELD, kit("agatha.shield")) == HeroAbility.AGATHA_SHIELD, "Agatha's shield");
        check(HeroAbilityAnims.ownerOf(Live.CASTING, kit("spider.web", "loki.vanish")) == HeroAbility.LOKI_VANISH, "the first caster");
        helper.succeed();
    }

    /** The kit filter: a kit acts its abilities' gestures, its modules' defaults and the body's, not other heroes'. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theKitFilterKeepsAHeroToHisOwnMoves(GameTestHelper helper) {
        HeroKit kit = kit("thor.lightning", "energy_blast");
        check(HeroAbilityAnims.allows(kit, GestureId.THOR_BEAM_KICK_BARE), "the lightning's kick");
        check(HeroAbilityAnims.allows(kit, GestureId.BILLY_BOLT), "the energy blast's cast");
        check(HeroAbilityAnims.allows(kit, GestureId.LAND_SOFT) && HeroAbilityAnims.allows(kit, GestureId.TAKEOFF_THOR), "the body's");
        check(!HeroAbilityAnims.allows(kit, GestureId.THANOS_SLAM), "not Thanos's slam");
        check(!HeroAbilityAnims.allows(kit, GestureId.SPIDER_LAND), "not Spider-Man's landing");
        check(HeroAbilityAnims.stancesOf(kit).contains(Stance.THOR_BEAM), "the lightning's stance");
        check(!HeroAbilityAnims.stancesOf(kit).contains(Stance.THOR_UNLEASHED), "not the God of Thunder's");
        check(HeroAbilityAnims.allows(kit("telekinesis"), GestureId.TK_GRAB_WANDA), "the generic grab");
        helper.succeed();
    }

    /** A pick's stance: its module's default, the one chosen, or none; and when it is up. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aHeldPowerStandsInItsChosenStance(GameTestHelper helper) {
        CustomHero.Pick beam = new CustomHero.Pick("energy_blast", 5, "beam", -1);
        check(CustomAnimRoutes.stanceOf(beam) == Stance.BEAM_HAND, "the beam's own");
        check(CustomAnimRoutes.stanceOf(beam.withStance("thor_beam")) == Stance.THOR_BEAM, "the one chosen");
        check(CustomAnimRoutes.stanceOf(beam.withStance(CustomHero.NONE)) == null, "none");
        check(CustomAnimRoutes.stanceOf(new CustomHero.Pick("thor.lightning", 5, "both", -1)) == null, "a hero's ability raises its own");
        PowerData data = CustomKitSupport.data(Power.CUSTOM);
        CustomState state = new CustomState();
        check(!CustomAnimRoutes.isActive(beam, data, state), "the beam is not held");
        state.set(PowerModule.ENERGY_BLAST, true);
        check(CustomAnimRoutes.isActive(beam, data, state), "the beam is held");
        check(!CustomAnimRoutes.isActive(new CustomHero.Pick("energy_blast", 5, "bolts", -1), data, state), "bolts have no stance");
        CustomHero.Pick field = new CustomHero.Pick("force_field", 5, "bubble", -1);
        check(!CustomAnimRoutes.isActive(field, data, state), "no field");
        state.raiseField(0L, 4.0F);
        check(CustomAnimRoutes.isActive(field, data, state), "the field is up");
        CustomHero hero = CustomKitSupport.def().pick("energy_blast", 5, "beam").pick("thor.lightning", 5).raw();
        check(CustomAnimRoutes.previewStance(hero, PowerModule.ENERGY_BLAST) == Stance.BEAM_HAND, "the preview holds the beam's");
        check(CustomAnimRoutes.previewStance(hero, HeroAbility.THOR_LIGHTNING) == Stance.THOR_BEAM, "the preview holds the lightning's");
        helper.succeed();
    }

    /** Every flight style is a profile, every run character a character, every speedster run and idle a style; and auto. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theStylesOfTheEditorAreTheHeroesOwn(GameTestHelper helper) {
        for (String style : AnimCatalog.FLIGHT_STYLES) {
            Power power = CustomAnimRoutes.flightPower(style);
            if (style.equals("plain")) {
                check(power == Power.NONE, "plain is the game's flight");
            } else {
                check(FlightProfile.of(power, CustomAnimRoutes.flightWhite(style)) != null, "the flight style " + style + " has no profile");
            }
        }
        check(FlightProfile.of(Power.VISION, true) != FlightProfile.of(Power.VISION, false), "White Vision flies his own way");
        for (String name : AnimCatalog.RUN_CHARACTERS) {
            check(RunCharacter.byName(name) != null, "the run character " + name + " is none");
        }
        for (String name : AnimCatalog.IDLES) {
            check((CustomAnimRoutes.idle(name) == null) == name.equals("none"), "the idle " + name);
        }
        HeroKit nothing = kit("durability");
        for (int i = 0; i < AnimCatalog.SPEED_RUNS.size(); i++) {
            CustomHero chosen = nothing.hero().withAnim(new CustomHero.Anim(CustomHero.AUTO, CustomHero.AUTO, AnimCatalog.SPEED_RUNS.get(i),
                    CustomHero.AUTO));
            check(CustomAnimRoutes.speedRun(chosen, nothing, false) == i + 1, "the speed run " + AnimCatalog.SPEED_RUNS.get(i));
        }
        // Auto.
        HeroKit thor = kit("thor.hammer", "flight");
        check(CustomAnimRoutes.flight(thor.hero(), thor).equals("thor"), "a kit of Thor's flies like Thor");
        check(CustomAnimRoutes.run(thor.hero(), thor).equals("thor"), "and walks like him");
        HeroKit jet = HeroKit.of(CustomKitSupport.def().pick("flight", 5, "jet").raw());
        check(CustomAnimRoutes.flight(jet.hero(), jet).equals("ultron"), "a jet flies like Ultron");
        check(CustomAnimRoutes.flight(nothing.hero(), nothing).equals("plain"), "nothing of a flier: the game's flight");
        check(CustomAnimRoutes.run(nothing.hero(), nothing).equals("default"), "and the default walk");
        HeroKit spider = kit("spider.web");
        check(CustomAnimRoutes.run(spider.hero(), spider).equals("spider"), "a kit of Spider-Man's walks like him");
        HeroKit tommy = kit("tommy.speed");
        check(CustomAnimRoutes.speedRun(tommy.hero(), tommy, false) == 3, "Tommy's speed runs as Tommy");
        check(CustomAnimRoutes.speedRun(nothing.hero(), nothing, true) == 1, "slowed time: Quicksilver's Time");
        check(CustomAnimRoutes.speedRun(nothing.hero(), nothing, false) == 2, "else his Super Speed");
        CustomHero chosen = thor.hero().withAnim(new CustomHero.Anim("phoenix", "loki", CustomHero.AUTO, "regal"));
        check(CustomAnimRoutes.flight(chosen, thor).equals("phoenix") && CustomAnimRoutes.run(chosen, thor).equals("loki"),
                "the choice of the definition wins");
        check(CustomAnimRoutes.idle(chosen) == Stance.MAG_FIELD, "regal is Magneto's field");
        helper.succeed();
    }

    /** Every move and stance of the catalogue is a constant, and every move moves the hands in first person. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theCatalogueIsMadeOfTheHeroesMovesAndShowsInFirstPerson(GameTestHelper helper) {
        for (String move : AnimCatalog.GESTURES) {
            GestureId id = CustomAnimRoutes.gesture(move);
            check(id != null, "the move " + move + " is no gesture");
            check(FirstPersonMoves.covers(id), "the move " + move + " has no first-person row");
        }
        for (List<String> stances : List.of(AnimCatalog.STANCES_HOLD, AnimCatalog.STANCES_TOGGLE)) {
            for (String stance : stances) {
                check(CustomAnimRoutes.stance(stance) != null, "the stance " + stance + " is none");
            }
        }
        check(CustomAnimRoutes.gesture(CustomHero.AUTO) == null && CustomAnimRoutes.gesture(CustomHero.NONE) == null, "auto and none");
        check(CustomAnimRoutes.gesture("not_a_move") == null, "an unknown move plays nothing");
        helper.succeed();
    }
}

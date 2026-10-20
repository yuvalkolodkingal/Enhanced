package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Toggle;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The animation of each built-in hero's ability (3.4), for a custom hero who has it: the gesture its main action and its
 * Sneak variant play when told by the cast marker (only those the hero acts by a cooldown: a strength that changes the
 * cooldown's length would fool the routes that tell two uses of a key apart by it), every gesture and stance the
 * ability may start, and which ability of a kit owns each synced bit (a {@link Live} state, a {@link Toggle}, a
 * {@link GestureRoutes.Flag}; the rules' resources make most of them unique, and the cast marker settles the rest). The
 * tables are those of section 2.1 of the plan, checked against the built-in routes by {@code CustomAnimTests}. Pure (no
 * client class), so the tests can read it.
 */
public final class HeroAbilityAnims {

    /** What one ability plays: its main action's gesture and its variant's, whether a cooldown tells each, and the rest. */
    private static final class Row {
        @Nullable
        GestureId main;
        boolean mainCd;
        @Nullable
        GestureId alt;
        boolean altCd;
        /** A gesture its cooldown tells later than its key (the Overcharge's blast flies when it is charged). */
        @Nullable
        GestureId late;
        final EnumSet<GestureId> gestures = EnumSet.noneOf(GestureId.class);
        final EnumSet<Stance> stances = EnumSet.noneOf(Stance.class);
    }

    private static final Map<HeroAbility, Row> ROWS = new EnumMap<>(HeroAbility.class);
    private static final Map<Live, List<HeroAbility>> LIVE_OWNERS = new EnumMap<>(Live.class);
    private static final Map<Toggle, List<HeroAbility>> TOGGLE_OWNERS = new EnumMap<>(Toggle.class);
    private static final Map<GestureRoutes.Flag, List<HeroAbility>> FLAG_OWNERS = new EnumMap<>(GestureRoutes.Flag.class);
    /**
     * The gestures that are no ability's but the body's (the take-offs, the landings, braking, the skid of a run, a look
     * changing, letting go of what telekinesis held): the flight style, the run and the animator start them for anyone.
     */
    public static final Set<GestureId> SHARED = Collections.unmodifiableSet(EnumSet.of(GestureId.LOOK_CHANGE, GestureId.TAKEOFF_MAGNETO,
            GestureId.TAKEOFF_WANDA, GestureId.TAKEOFF_VISION, GestureId.TAKEOFF_BILLY, GestureId.TAKEOFF_THOR, GestureId.TAKEOFF_PHOENIX,
            GestureId.TAKEOFF_ULTRON, GestureId.TAKEOFF_APOC, GestureId.TAKEOFF_AGATHA, GestureId.BRAKE, GestureId.BRAKE_THOR,
            GestureId.LAND_SOFT, GestureId.LAND_HERO, GestureId.RUN_SKID, GestureId.TK_RELEASE));

    static {
        // ===== magneto =====
        row(HeroAbility.MAGNETO_METAL).main(GestureId.TK_GRAB_MAGNETO).altCd(GestureId.MAG_PULL)
                .also(GestureId.TK_FLING_MAGNETO).stances(Stance.TK_HOLD, Stance.TK_STRAIN);
        row(HeroAbility.MAGNETO_SHRAPNEL).main(GestureId.MAG_SHRAPNEL_START).stances(Stance.MAG_SHRAPNEL);
        row(HeroAbility.MAGNETO_DISARM).mainCd(GestureId.MAG_DISARM);
        row(HeroAbility.MAGNETO_SHIELD).main(GestureId.MAG_SHIELD_ON).also(GestureId.MAG_SHIELD_OFF).stances(Stance.MAG_FIELD, Stance.MAG_CATCH);
        // ===== scarlet_witch =====
        row(HeroAbility.WANDA_CHAOS_BEAM).main(GestureId.WANDA_BEAM_KICK).stances(Stance.BEAM_WANDA, Stance.WANDA_READY);
        row(HeroAbility.WANDA_TELEKINESIS).main(GestureId.TK_GRAB_WANDA).also(GestureId.TK_FLING_WANDA)
                .stances(Stance.TK_HOLD, Stance.TK_STRAIN, Stance.WANDA_READY);
        row(HeroAbility.WANDA_CHAOS_SHIELD).main(GestureId.WANDA_SHIELD_RAISE).also(GestureId.WANDA_SHIELD_SNAP, GestureId.WANDA_SHIELD_DROP)
                .stances(Stance.SHIELD_WANDA, Stance.WANDA_READY);
        row(HeroAbility.WANDA_HEX).main(GestureId.WANDA_HEX_RISE).alt(GestureId.WANDA_HEX_DISSOLVE)
                .also(GestureId.WANDA_HEX_TWEAK, GestureId.WANDA_HEX_GROW, GestureId.WANDA_HEX_SHRINK).stances(Stance.WANDA_HEX_CONTROL);
        // ===== quicksilver =====
        row(HeroAbility.QUICKSILVER_TIME).main(GestureId.QS_GOGGLES).alt(GestureId.QS_WALKMAN);
        row(HeroAbility.QUICKSILVER_SPEED).main(GestureId.QS_START).altCd(GestureId.SPEED_DASH);
        row(HeroAbility.QUICKSILVER_BLITZ).mainCd(GestureId.QS_BLITZ).altCd(GestureId.QS_SNATCH);
        row(HeroAbility.QUICKSILVER_WALL_RUN);
        row(HeroAbility.QUICKSILVER_BARRAGE).mainCd(GestureId.QS_BARRAGE);
        // ===== vision =====
        row(HeroAbility.VISION_MIND_BEAM).main(GestureId.VISION_FOCUS).stances(Stance.VISION_BEAM);
        row(HeroAbility.VISION_PHASE).main(GestureId.VISION_PHASE_IN).also(GestureId.VISION_PHASE_OUT).stances(Stance.VISION_PHASED);
        row(HeroAbility.VISION_DENSITY).main(GestureId.VISION_DENSE_ON).also(GestureId.VISION_DENSE_OFF).stances(Stance.VISION_DENSE);
        row(HeroAbility.VISION_DISGUISE).main(GestureId.VISION_DISGUISE);
        row(HeroAbility.VISION_BODY); // (L2's row, 3.4: a passive body, nothing to act)
        // ===== billy =====
        // (The Hex Beam begins with the bolt's cast, and with billy.beam off R is the single bolt, told by its cooldown.)
        row(HeroAbility.BILLY_HEX_BEAM).mainCd(GestureId.BILLY_BOLT).stances(Stance.BEAM_HAND);
        row(HeroAbility.BILLY_TELEKINESIS).main(GestureId.TK_GRAB_BILLY).also(GestureId.TK_FLING_BILLY).stances(Stance.TK_HOLD, Stance.TK_STRAIN);
        // (The Hex Barrier, his Sneak variant, has a stance and no gesture: the plate simply comes up.)
        row(HeroAbility.BILLY_FREEZE).mainCd(GestureId.BILLY_FREEZE_CAST).stances(Stance.BILLY_FREEZE, Stance.BARRIER_BILLY);
        row(HeroAbility.BILLY_NOISE).main(GestureId.BILLY_NOISE_ON).also(GestureId.BILLY_NOISE_OFF).stances(Stance.NOISE);
        row(HeroAbility.BILLY_BUILD_MAGIC);
        // ===== tommy =====
        row(HeroAbility.TOMMY_TIME).main(GestureId.SPEED_TIME).alt(GestureId.TOMMY_READY);
        row(HeroAbility.TOMMY_SPEED).main(GestureId.TOMMY_READY).altCd(GestureId.SPEED_DASH);
        row(HeroAbility.TOMMY_WHIRLWIND).main(GestureId.TOMMY_WHIRL_START).alt(GestureId.TOMMY_BOOM).stances(Stance.TOMMY_WHIRL);
        row(HeroAbility.TOMMY_VIBRATE).mainCd(GestureId.TOMMY_VIBRATE).altCd(GestureId.TOMMY_BLITZ);
        row(HeroAbility.TOMMY_ACCELERATE).mainCd(GestureId.TOMMY_ACCEL);
        // ===== loki =====
        row(HeroAbility.LOKI_DAGGERS).mainCd(GestureId.LOKI_DAGGER).altCd(GestureId.LOKI_FAN);
        row(HeroAbility.LOKI_DUPLICATES);
        row(HeroAbility.LOKI_SHAPESHIFT).main(GestureId.LOKI_CAST);
        row(HeroAbility.LOKI_VANISH).main(GestureId.LOKI_CAST);
        row(HeroAbility.LOKI_FROST_GIANT).main(GestureId.LOKI_FROST_ON).also(GestureId.LOKI_FROST_OFF).stances(Stance.LOKI_FROST);
        // ===== thor =====
        row(HeroAbility.THOR_HAMMER).main(GestureId.THOR_THROW).alt(GestureId.THOR_SHATTER).also(GestureId.THOR_SUMMON,
                GestureId.THOR_RECALL_DRAW, GestureId.THOR_CATCH, GestureId.THOR_REFORM, GestureId.THOR_PIN).stances(Stance.THOR_WAIT);
        row(HeroAbility.THOR_LIGHTNING).main(GestureId.THOR_BEAM_KICK_BARE).also(GestureId.THOR_BEAM_KICK).stances(Stance.THOR_BEAM);
        row(HeroAbility.THOR_THUNDER).main(GestureId.THOR_UNLEASH_ON).also(GestureId.THOR_UNLEASH_OFF).stances(Stance.THOR_UNLEASHED);
        row(HeroAbility.THOR_BIFROST).also(GestureId.THOR_ARRIVAL).stances(Stance.THOR_BIFROST);
        row(HeroAbility.THOR_BODY);
        // ===== phoenix =====
        row(HeroAbility.PHOENIX_MIND).main(GestureId.PHOENIX_READ_ON).alt(GestureId.PHOENIX_SENSE).also(GestureId.PHOENIX_READ_OFF)
                .stances(Stance.PHOENIX_READ);
        row(HeroAbility.PHOENIX_TELEKINESIS).main(GestureId.TK_GRAB_PHOENIX).also(GestureId.TK_FLING_PHOENIX)
                .stances(Stance.TK_HOLD, Stance.TK_STRAIN);
        row(HeroAbility.PHOENIX_CONTROL).main(GestureId.PHOENIX_MIND_CAST).alt(GestureId.PHOENIX_PSIONIC_CAST);
        row(HeroAbility.PHOENIX_BURST).main(GestureId.PHOENIX_CHARGE_START).also(GestureId.PHOENIX_BURST).stances(Stance.PHOENIX_CHARGE);
        row(HeroAbility.PHOENIX_BODY);
        // ===== spider_man =====
        row(HeroAbility.SPIDER_WEB).main(GestureId.SPIDER_SHOOT).altCd(GestureId.SPIDER_BURST);
        row(HeroAbility.SPIDER_ZIP).main(GestureId.SPIDER_ZIP_START).alt(GestureId.SPIDER_SWING_START).stances(Stance.SPIDER_ZIP, Stance.SPIDER_SWING);
        row(HeroAbility.SPIDER_CRAWL).main(GestureId.SPIDER_CRAWL_ON).alt(GestureId.SPIDER_SENSE_TOGGLE).also(GestureId.SPIDER_CRAWL_OFF,
                GestureId.SPIDER_ALERT_START, GestureId.SPIDER_ARRIVE, GestureId.SPIDER_LEAP)
                .stances(Stance.SPIDER_CLING_WALL, Stance.SPIDER_CLING_CEILING, Stance.SPIDER_ALERT);
        row(HeroAbility.SPIDER_LEGS).main(GestureId.SPIDER_LEGS_OUT).alt(GestureId.SPIDER_KILL_ON).also(GestureId.SPIDER_LEGS_IN)
                .stances(Stance.SPIDER_KILL);
        row(HeroAbility.SPIDER_BLACK_EYES).main(GestureId.SPIDER_SURGE_ON).stances(Stance.SPIDER_SURGE);
        row(HeroAbility.SPIDER_BODY).also(GestureId.SPIDER_LAND, GestureId.SPIDER_LEAP);
        // ===== thanos =====
        row(HeroAbility.THANOS_POWER).mainCd(GestureId.THANOS_BLAST).altCd(GestureId.THANOS_SLAM);
        row(HeroAbility.THANOS_SPACE).mainCd(GestureId.THANOS_STEP).altCd(GestureId.THANOS_PULL);
        row(HeroAbility.THANOS_REALITY).mainCd(GestureId.THANOS_WARP).alt(GestureId.THANOS_VEIL_ON).also(GestureId.THANOS_VEIL_OFF)
                .stances(Stance.THANOS_VEIL);
        row(HeroAbility.THANOS_SOUL).main(GestureId.THANOS_DRAIN_CAST).alt(GestureId.THANOS_SIGHT).stances(Stance.THANOS_DRAIN);
        row(HeroAbility.THANOS_TIME).mainCd(GestureId.THANOS_REWIND).altCd(GestureId.THANOS_TIMELOCK);
        row(HeroAbility.THANOS_BODY);
        // ===== ultron =====
        row(HeroAbility.ULTRON_REPULSOR).main(GestureId.ULTRON_FIRE_START).alt(GestureId.ULTRON_CHARGE_START).late(GestureId.ULTRON_BLAST)
                .stances(Stance.ULTRON_FIRE, Stance.ULTRON_CHARGE);
        // (The Pull and the Slam are told by the payload of their effect, which names the caster: anyone's.)
        row(HeroAbility.ULTRON_GRAVITY).main(GestureId.ULTRON_PULL).alt(GestureId.ULTRON_SLAM);
        row(HeroAbility.ULTRON_UPLOAD).mainCd(GestureId.ULTRON_UPLOAD).alt(GestureId.ULTRON_HACK);
        row(HeroAbility.ULTRON_ARMY).mainCd(GestureId.ULTRON_ARMY).alt(GestureId.ULTRON_COMMAND);
        row(HeroAbility.ULTRON_BODY);
        // ===== apocalypse =====
        row(HeroAbility.APOCALYPSE_SAND).mainCd(GestureId.APOC_SAND_WAVE).altCd(GestureId.APOC_ENTOMB);
        row(HeroAbility.APOCALYPSE_PORTAL).mainCd(GestureId.APOC_PORTAL);
        row(HeroAbility.APOCALYPSE_SHIELD).main(GestureId.APOC_SHIELD_RAISE).also(GestureId.APOC_SHIELD_DROP).stances(Stance.APOC_SHIELD);
        row(HeroAbility.APOCALYPSE_BESTOW).mainCd(GestureId.APOC_BESTOW).alt(GestureId.APOC_STORM_START).stances(Stance.APOC_STORM);
        row(HeroAbility.APOCALYPSE_BODY);
        // ===== agatha =====
        row(HeroAbility.AGATHA_PURPLE_BEAM).main(GestureId.AGATHA_BEAM).alt(GestureId.AGATHA_HEX).stances(Stance.BEAM_HAND);
        row(HeroAbility.AGATHA_DRAIN).mainCd(GestureId.AGATHA_DRAIN);
        row(HeroAbility.AGATHA_SHIELD).main(GestureId.AGATHA_SHIELD_RAISE).also(GestureId.AGATHA_SHIELD_DROP).stances(Stance.AGATHA_SHIELD);
        row(HeroAbility.AGATHA_RUNES).mainCd(GestureId.AGATHA_RUNES);
        row(HeroAbility.AGATHA_BODY);
        // ===== cyclops =====
        // (The Optic Blast held is its stance, the hand easing up to the visor; the Ricochet is its cooldown.)
        row(HeroAbility.CYCLOPS_OPTIC_BLAST).altCd(GestureId.CYCLOPS_RICOCHET).stances(Stance.CYCLOPS_BLAST, Stance.BEAM_HAND);
        row(HeroAbility.CYCLOPS_CONCUSSION).mainCd(GestureId.CYCLOPS_CONCUSSION);
        row(HeroAbility.CYCLOPS_LEAP).mainCd(GestureId.CYCLOPS_LEAP);
        row(HeroAbility.CYCLOPS_RALLY).mainCd(GestureId.CYCLOPS_RALLY);
        row(HeroAbility.CYCLOPS_GLASSES).mainCd(GestureId.CYCLOPS_GLASSES_ON).altCd(GestureId.CYCLOPS_GLASSES_OFF)
                .stances(Stance.CYCLOPS_UNLEASHED);
        row(HeroAbility.CYCLOPS_BODY);
        // ===== professor_x =====
        row(HeroAbility.XAVIER_MENTAL_BLAST).mainCd(GestureId.XAVIER_BLAST);
        row(HeroAbility.XAVIER_THOUGHTS).main(GestureId.XAVIER_THOUGHTS_ON).also(GestureId.XAVIER_THOUGHTS_OFF);
        row(HeroAbility.XAVIER_TAKE_OVER).main(GestureId.XAVIER_LINK).stances(Stance.XAVIER_TRANCE);
        row(HeroAbility.XAVIER_FREEZE_MINDS).mainCd(GestureId.XAVIER_FREEZE);
        row(HeroAbility.XAVIER_CEREBRO).main(GestureId.XAVIER_CEREBRO).stances(Stance.XAVIER_CEREBRO);
        // ===== heimdall (3.5) =====
        // (The rows of 3.5 are written empty by the skeleton: each lane gives its hero's gestures and stances.)
        // (The blows are counted by HeimdallClient's edges from live:HEIMDALL_SLASHING, on whatever key the row is; the Lunge is
        // R's cooldown; the Call, the Strike, the Send and Hofund's return are started by the payloads of their light.)
        row(HeroAbility.HEIMDALL_HOFUND).main(GestureId.HEIMDALL_SLASH_1).altCd(GestureId.HEIMDALL_LUNGE)
                .also(GestureId.HEIMDALL_SLASH_2, GestureId.HEIMDALL_SLASH_3, GestureId.HEIMDALL_RETURN);
        row(HeroAbility.HEIMDALL_SIGHT).main(GestureId.HEIMDALL_SIGHT_ON).also(GestureId.HEIMDALL_SIGHT_OFF)
                .stances(Stance.HEIMDALL_FARSIGHT, Stance.HEIMDALL_WATCH);
        row(HeroAbility.HEIMDALL_BIFROST).also(GestureId.HEIMDALL_BIFROST).stances(Stance.THOR_BIFROST);
        row(HeroAbility.HEIMDALL_WARD).also(GestureId.HEIMDALL_CALL).stances(Stance.HEIMDALL_WARD);
        row(HeroAbility.HEIMDALL_STRIKE).also(GestureId.HEIMDALL_STRIKE, GestureId.HEIMDALL_SEND);
        row(HeroAbility.HEIMDALL_BODY);
        // ===== odin (3.5) =====
        row(HeroAbility.ODIN_GUNGNIR).mainCd(GestureId.ODIN_BLAST).alt(GestureId.ODIN_THROW).also(GestureId.ODIN_CATCH)
                .stances(Stance.ODIN_REGAL);
        row(HeroAbility.ODIN_ODINFORCE).main(GestureId.ODIN_SHIELD_UP).altCd(GestureId.ODIN_HEAL).also(GestureId.ODIN_SHIELD_DOWN)
                .stances(Stance.ODIN_SHIELD);
        row(HeroAbility.ODIN_BANISH).mainCd(GestureId.ODIN_BANISH).altCd(GestureId.ODIN_WAVE);
        row(HeroAbility.ODIN_RAVENS).main(GestureId.ODIN_RAVENS).alt(GestureId.ODIN_RAVENS);
        row(HeroAbility.ODIN_COMMAND).mainCd(GestureId.ODIN_COMMAND).alt(GestureId.ODIN_KNEEL).also(GestureId.ODIN_RISE)
                .stances(Stance.ODIN_SLEEP);
        row(HeroAbility.ODIN_BODY).also(GestureId.ODIN_CROWN);
        // ===== hela (3.5) =====
        // (Her blades, her dash and Fenris are told by the payload of their effect; the form by its toggle, the dead and the
        // grasp by their states.)
        row(HeroAbility.HELA_NECROSWORDS).main(GestureId.HELA_THROW).alt(GestureId.HELA_RAIN);
        row(HeroAbility.HELA_BLADE_WALL).main(GestureId.HELA_WALL).alt(GestureId.HELA_BURST);
        row(HeroAbility.HELA_GODDESS).main(GestureId.HELA_FORM_ON).also(GestureId.HELA_FORM_OFF).alt(GestureId.HELA_DASH)
                .stances(Stance.HELA_BLADES);
        row(HeroAbility.HELA_ARMY).main(GestureId.HELA_RAISE).alt(GestureId.HELA_FENRIS);
        row(HeroAbility.HELA_GRASP).main(GestureId.HELA_GRASP).stances(Stance.HELA_GRASP);
        row(HeroAbility.HELA_BODY);
        // ===== doctor_strange (3.6) =====
        // (The skeleton's rows have no moves yet: lane L3 gives them their gestures and stances.)
        // (Every gesture of his is started by a payload of its sparks, a switch or the Shield's state, never by a
        // cooldown: the cast marker plays none of them.)
        row(HeroAbility.STRANGE_WHIP).main(GestureId.STRANGE_WHIP_1).alt(GestureId.STRANGE_GRAPPLE)
                .also(GestureId.STRANGE_WHIP_2, GestureId.STRANGE_WHIP_3);
        row(HeroAbility.STRANGE_SHIELDS).main(GestureId.STRANGE_SHIELD_UP).alt(GestureId.STRANGE_DOME)
                .stances(Stance.STRANGE_SHIELD, Stance.STRANGE_CASTING);
        row(HeroAbility.STRANGE_BANDS).main(GestureId.STRANGE_BANDS).alt(GestureId.STRANGE_ASTRAL)
                .also(GestureId.STRANGE_ASTRAL_IN);
        row(HeroAbility.STRANGE_PORTAL).main(GestureId.STRANGE_SLING).alt(GestureId.STRANGE_MIRROR)
                .stances(Stance.STRANGE_CASTING);
        row(HeroAbility.STRANGE_EYE).main(GestureId.STRANGE_EYE_OPEN).alt(GestureId.STRANGE_REWIND)
                .stances(Stance.STRANGE_CASTING);
        row(HeroAbility.STRANGE_BODY);
        // ===== hulk (3.6) =====
        // (Every move of his is told by the payload of its effect; the Human Form by its toggle, the rest by his
        // stances.)
        row(HeroAbility.HULK_SMASH).main(GestureId.HULK_SMASH).alt(GestureId.HULK_GRAB_SMASH);
        row(HeroAbility.HULK_CLAP).main(GestureId.HULK_CLAP).alt(GestureId.HULK_ROAR);
        row(HeroAbility.HULK_LEAP).main(GestureId.HULK_LEAP_CROUCH).alt(GestureId.HULK_BOUND).also(GestureId.HULK_LAND)
                .stances(Stance.HULK_AIR);
        row(HeroAbility.HULK_BOULDER).main(GestureId.HULK_RIP).alt(GestureId.HULK_CHARGE).also(GestureId.HULK_THROW);
        row(HeroAbility.HULK_TRANSFORM).main(GestureId.HULK_CALM).also(GestureId.HULK_OUT, GestureId.HULK_SWITCH);
        row(HeroAbility.HULK_BODY).stances(Stance.HULK_IDLE, Stance.HULK_ENRAGED);
        // ===== wolverine (3.7) =====
        // (The skeleton's rows have no moves yet: lane L2 gives them their gestures and stances.)
        row(HeroAbility.WOLVERINE_SLASH).main(GestureId.WOLVERINE_SLASH_1).alt(GestureId.WOLVERINE_SPIN)
                .also(GestureId.WOLVERINE_SLASH_2, GestureId.WOLVERINE_SLASH_3);
        row(HeroAbility.WOLVERINE_CLAWS).main(GestureId.WOLVERINE_SNIKT).alt(GestureId.WOLVERINE_CLIMB)
                .also(GestureId.WOLVERINE_SHINK).stances(Stance.WOLVERINE_CLAWS, Stance.WOLVERINE_CLIMBING);
        row(HeroAbility.WOLVERINE_POUNCE).main(GestureId.WOLVERINE_POUNCE_CROUCH).alt(GestureId.WOLVERINE_FERAL_LEAP)
                .also(GestureId.WOLVERINE_POUNCE_AIR, GestureId.WOLVERINE_POUNCE_STAB);
        row(HeroAbility.WOLVERINE_BERSERKER).main(GestureId.WOLVERINE_BERSERK_ROAR)
                .stances(Stance.WOLVERINE_BERSERK);
        row(HeroAbility.WOLVERINE_SENSES).main(GestureId.WOLVERINE_SNIFF).alt(GestureId.WOLVERINE_TRACK);
        row(HeroAbility.WOLVERINE_BODY);
        // ===== deadpool (3.7) =====
        // (Every move of his is told by the effect of his move, which names him; the Parry and the drawn pistols by
        // their states' stances.)
        row(HeroAbility.DEADPOOL_KATANAS).main(GestureId.DEADPOOL_SLASH_1).alt(GestureId.DEADPOOL_PARRY)
                .also(GestureId.DEADPOOL_SLASH_2, GestureId.DEADPOOL_SLASH_3).stances(Stance.DEADPOOL_SWORDS);
        row(HeroAbility.DEADPOOL_PISTOLS).main(GestureId.DEADPOOL_SHOOT_R).alt(GestureId.DEADPOOL_TRICK_SHOT)
                .also(GestureId.DEADPOOL_SHOOT_L, GestureId.DEADPOOL_RELOAD).stances(Stance.DEADPOOL_GUNS);
        row(HeroAbility.DEADPOOL_BELT).main(GestureId.DEADPOOL_BLINK).alt(GestureId.DEADPOOL_DROP_IN);
        row(HeroAbility.DEADPOOL_TAUNT).main(GestureId.DEADPOOL_TAUNT_WAVE).alt(GestureId.DEADPOOL_EAT)
                .also(GestureId.DEADPOOL_TAUNT_GUNS, GestureId.DEADPOOL_TAUNT_HEART);
        row(HeroAbility.DEADPOOL_DOGPOOL).main(GestureId.DEADPOOL_WHISTLE).alt(GestureId.DEADPOOL_SMOKE);
        row(HeroAbility.DEADPOOL_BODY);
        live(Live.DEADPOOL_PARRYING, HeroAbility.DEADPOOL_KATANAS);
        live(Live.DEADPOOL_AIMING, HeroAbility.DEADPOOL_PISTOLS);
        // ===== captain (3.8) =====
        // (Lane L1: every move of his is told by the effect of his move, which names him; the guard behind the raised
        // shield is the throw's stance.)
        row(HeroAbility.CAPTAIN_SHIELD_THROW).main(GestureId.CAP_THROW).alt(GestureId.CAP_THROW)
                .also(GestureId.CAP_THROW_OFF, GestureId.CAP_CATCH, GestureId.CAP_CATCH_MAIN)
                .stances(Stance.CAP_GUARD);
        row(HeroAbility.CAPTAIN_SHIELD_SLAM).main(GestureId.CAP_SLAM_LEAP).alt(GestureId.CAP_LEAP)
                .also(GestureId.CAP_SLAM_DOWN);
        row(HeroAbility.CAPTAIN_SHIELD_CHARGE).main(GestureId.CAP_CHARGE).alt(GestureId.CAP_BASH);
        row(HeroAbility.CAPTAIN_ASSEMBLE).main(GestureId.CAP_ASSEMBLE).alt(GestureId.CAP_INSPIRE);
        row(HeroAbility.CAPTAIN_COMBO).main(GestureId.CAP_PUNCH_1).alt(GestureId.CAP_FLIP_KICK)
                .also(GestureId.CAP_PUNCH_2, GestureId.CAP_KICK);
        row(HeroAbility.CAPTAIN_BODY);
        live(Live.CAP_CHARGING, HeroAbility.CAPTAIN_SHIELD_CHARGE);
        live(Live.CAP_SLAMMING, HeroAbility.CAPTAIN_SHIELD_SLAM);
        // ===== red_hulk (3.8) =====
        // (Lane L2. What he shares with the Hulk plays the Hulk's gestures: the Smash, the Grab, the Roar, the Bound,
        // the landing.)
        row(HeroAbility.RED_HULK_SMASH).main(GestureId.HULK_SMASH).alt(GestureId.HULK_GRAB_SMASH);
        row(HeroAbility.RED_HULK_HEAT_WAVE).main(GestureId.RED_HULK_HEAT_WAVE).alt(GestureId.HULK_ROAR);
        row(HeroAbility.RED_HULK_SUPER_JUMP).main(GestureId.RED_HULK_JUMP_CROUCH).alt(GestureId.HULK_BOUND)
                .also(GestureId.RED_HULK_JUMP, GestureId.HULK_LAND)
                .stances(Stance.RED_HULK_CHARGING, Stance.RED_HULK_AIR);
        row(HeroAbility.RED_HULK_BURST).main(GestureId.RED_HULK_BURST).alt(GestureId.RED_HULK_ABSORB)
                .stances(Stance.RED_HULK_ABSORBING);
        row(HeroAbility.RED_HULK_TRANSFORM).main(GestureId.RED_HULK_TRANSFORM_IN)
                .also(GestureId.RED_HULK_TRANSFORM_OUT, GestureId.RED_HULK_ROSS);
        row(HeroAbility.RED_HULK_BODY).stances(Stance.HULK_IDLE, Stance.RED_HULK_HEATED);
        // ===== iron_man (3.8) =====
        // (Lane L3's gestures for the core moves; the Mark 85's own (the blades, the shield, the Refocuser, the
        // drones) are lane L4's to add as alts.)
        row(HeroAbility.IRON_MAN_REPULSOR).main(GestureId.IRON_REPULSOR_R)
                .also(GestureId.IRON_REPULSOR_L, GestureId.IRON_DOUBLE_BLAST);
        row(HeroAbility.IRON_MAN_UNIBEAM).main(GestureId.IRON_UNIBEAM).stances(Stance.IRON_UNIBEAM);
        row(HeroAbility.IRON_MAN_SHIELD);
        row(HeroAbility.IRON_MAN_MISSILES).main(GestureId.IRON_MISSILES);
        row(HeroAbility.IRON_MAN_SUIT);
        // ===== falcon (3.8) =====
        // (Every move of his is told by the effect of his move, which names him; the Dive and the Wing Shield by
        // their states' stances, the glide by the body's; Redwing's launch by its toggle.)
        row(HeroAbility.FALCON_BLASTERS).main(GestureId.FALCON_BLAST_L).alt(GestureId.FALCON_ROCKET)
                .also(GestureId.FALCON_BLAST_R);
        row(HeroAbility.FALCON_DIVE).main(GestureId.FALCON_DIVE).alt(GestureId.FALCON_CLIMB)
                .also(GestureId.FALCON_DIVE_HIT).stances(Stance.FALCON_DIVING);
        row(HeroAbility.FALCON_WING_SHIELD).main(GestureId.FALCON_SHIELD).alt(GestureId.FALCON_SLASH)
                .stances(Stance.FALCON_SHIELDING);
        row(HeroAbility.FALCON_REDWING).main(GestureId.FALCON_REDWING_LAUNCH).alt(GestureId.FALCON_REDWING_POINT);
        row(HeroAbility.FALCON_BODY).stances(Stance.FALCON_GLIDE);
        live(Live.FALCON_DIVING, HeroAbility.FALCON_DIVE);
        live(Live.FALCON_WING_SHIELD, HeroAbility.FALCON_WING_SHIELD);
        live(Live.FALCON_GLIDING, HeroAbility.FALCON_BODY);
        // ===== love (3.8) =====
        // (Her throw is the cooldown of R; her lightning and her eyes her states; the catch and the strike her
        // client's.)
        row(HeroAbility.LOVE_AXE).mainCd(GestureId.LOVE_THROW).alt(GestureId.LOVE_STRIKE).also(GestureId.LOVE_CATCH);
        row(HeroAbility.LOVE_LIGHTNING).main(GestureId.LOVE_LIGHTNING);
        row(HeroAbility.LOVE_EYE_LASER).main(GestureId.LOVE_EYE_LASER);
        row(HeroAbility.LOVE_BIFROST);
        row(HeroAbility.LOVE_BODY);
        // ===== variants (3.5, the creator lane) =====
        // (A variant row is bound as its parent's Sneak variant, so the parent's routes and cast marker act it. Its own
        // row plays the parent's variant gesture on the marker when a cooldown tells it, and may start all the parent's
        // moves.)
        row(HeroAbility.WANDA_BUILD_MAGIC);
        for (HeroAbility a : HeroAbility.values()) {
            HeroAbility parent = a.parent();
            if (parent == null || ROWS.containsKey(a)) {
                continue;
            }
            Row of = ROWS.get(parent);
            Builder b = row(a);
            if (of != null) {
                if (of.alt != null && of.altCd) {
                    b.mainCd(of.alt);
                }
                b.row.gestures.addAll(of.gestures);
                b.row.stances.addAll(of.stances);
            }
        }

        // Who owns each synced bit (the first of the kit's rows in this order; the cast marker settles a shared one).
        live(Live.SPEEDING, HeroAbility.QUICKSILVER_SPEED, HeroAbility.QUICKSILVER_TIME, HeroAbility.TOMMY_SPEED);
        live(Live.WHIRLWIND, HeroAbility.TOMMY_WHIRLWIND);
        live(Live.SONIC_BOOM, HeroAbility.TOMMY_WHIRLWIND);
        live(Live.CALLING_LIGHTNING, HeroAbility.THOR_LIGHTNING);
        live(Live.BIFROST, HeroAbility.THOR_BIFROST, HeroAbility.HEIMDALL_BIFROST);
        live(Live.HIDDEN, HeroAbility.LOKI_DUPLICATES);
        live(Live.THROWING, HeroAbility.LOKI_DAGGERS);
        live(Live.CASTING, HeroAbility.LOKI_SHAPESHIFT, HeroAbility.LOKI_VANISH, HeroAbility.PHOENIX_CONTROL, HeroAbility.SPIDER_WEB,
                HeroAbility.ULTRON_UPLOAD, HeroAbility.AGATHA_PURPLE_BEAM);
        live(Live.CROWNED, HeroAbility.BILLY_HEX_BEAM, HeroAbility.BILLY_FREEZE, HeroAbility.BILLY_NOISE, HeroAbility.BILLY_TELEKINESIS);
        live(Live.FREEZING, HeroAbility.BILLY_FREEZE);
        live(Live.PHOENIX_CHARGING, HeroAbility.PHOENIX_BURST);
        live(Live.PHOENIX_BLAZE, HeroAbility.PHOENIX_BURST, HeroAbility.PHOENIX_BODY);
        live(Live.MIND_SENSING, HeroAbility.PHOENIX_MIND);
        live(Live.PHOENIX_PSIONIC, HeroAbility.PHOENIX_CONTROL);
        live(Live.CLINGING_WALL, HeroAbility.SPIDER_CRAWL);
        live(Live.CLINGING_CEILING, HeroAbility.SPIDER_CRAWL);
        live(Live.ZIPPING, HeroAbility.SPIDER_ZIP);
        live(Live.SWINGING, HeroAbility.SPIDER_ZIP);
        live(Live.SENSING, HeroAbility.SPIDER_CRAWL);
        live(Live.BLACK_EYES, HeroAbility.SPIDER_BLACK_EYES);
        live(Live.INSTANT_KILL, HeroAbility.SPIDER_LEGS);
        live(Live.SOUL_DRAINING, HeroAbility.THANOS_SOUL);
        live(Live.SOUL_SIGHT, HeroAbility.THANOS_SOUL);
        live(Live.ULTRON_CHARGING, HeroAbility.ULTRON_REPULSOR);
        live(Live.SANDSTORM, HeroAbility.APOCALYPSE_BESTOW);
        live(Live.BEAMING, HeroAbility.BILLY_HEX_BEAM, HeroAbility.AGATHA_PURPLE_BEAM, HeroAbility.CYCLOPS_OPTIC_BLAST, HeroAbility.CYCLOPS_GLASSES);
        live(Live.CYCLOPS_UNLEASHED, HeroAbility.CYCLOPS_GLASSES);
        live(Live.MIND_LINKED, HeroAbility.XAVIER_TAKE_OVER);
        live(Live.XAVIER_FOCUS, HeroAbility.XAVIER_MENTAL_BLAST, HeroAbility.XAVIER_FREEZE_MINDS);
        live(Live.CEREBRO, HeroAbility.XAVIER_CEREBRO);
        live(Live.SURFACE_WALL, HeroAbility.QUICKSILVER_WALL_RUN, HeroAbility.TOMMY_TIME);
        live(Live.SURFACE_CEILING, HeroAbility.QUICKSILVER_WALL_RUN, HeroAbility.TOMMY_TIME);
        // 3.5
        live(Live.HEIMDALL_WARD, HeroAbility.HEIMDALL_WARD);
        live(Live.HEIMDALL_FARSIGHT, HeroAbility.HEIMDALL_SIGHT);
        live(Live.HEIMDALL_SLASHING, HeroAbility.HEIMDALL_HOFUND);
        live(Live.ODIN_THROWN, HeroAbility.ODIN_GUNGNIR);
        live(Live.ODIN_RAVENS, HeroAbility.ODIN_RAVENS);
        live(Live.HELA_GRASPING, HeroAbility.HELA_GRASP);
        live(Live.HELA_SUMMONING, HeroAbility.HELA_ARMY);
        // 3.6 (lane L3: Doctor Strange's)
        live(Live.STRANGE_SHIELD, HeroAbility.STRANGE_SHIELDS);
        live(Live.STRANGE_ASTRAL, HeroAbility.STRANGE_BANDS);
        live(Live.STRANGE_MIRROR, HeroAbility.STRANGE_PORTAL);
        // 3.6 (lane L7: the Hulk's)
        live(Live.HULK_TRANSFORMING, HeroAbility.HULK_TRANSFORM);
        live(Live.HULK_LEAPING, HeroAbility.HULK_LEAP);
        live(Live.HULK_GRABBING, HeroAbility.HULK_SMASH);
        live(Live.HULK_CHARGING, HeroAbility.HULK_BOULDER);
        live(Live.HULK_ENRAGED, HeroAbility.HULK_BODY);
        // 3.7 (lane L2: Wolverine's)
        live(Live.WOLVERINE_POUNCING, HeroAbility.WOLVERINE_POUNCE);
        live(Live.WOLVERINE_BERSERK, HeroAbility.WOLVERINE_BERSERKER);
        // 3.8 (lane L2: Red Hulk's)
        live(Live.RED_HULK_TRANSFORMING, HeroAbility.RED_HULK_TRANSFORM);
        live(Live.RED_HULK_JUMP_CHARGING, HeroAbility.RED_HULK_SUPER_JUMP);
        live(Live.RED_HULK_LEAPING, HeroAbility.RED_HULK_SUPER_JUMP);
        live(Live.RED_HULK_HEATED, HeroAbility.RED_HULK_BODY);
        live(Live.RED_HULK_ABSORBING, HeroAbility.RED_HULK_BURST);
        live(Live.RED_HULK_GRABBING, HeroAbility.RED_HULK_SMASH);
        // (PINNED, LIFTED and MIND_CONTROLLED are what is done to a player, not what he does: no ability owns them.)
        toggle(Toggle.FROST_GIANT, HeroAbility.LOKI_FROST_GIANT);
        toggle(Toggle.GOD_OF_THUNDER, HeroAbility.THOR_THUNDER);
        toggle(Toggle.SUPER_SPEED, HeroAbility.TOMMY_SPEED);
        toggle(Toggle.THE_NOISE, HeroAbility.BILLY_NOISE);
        toggle(Toggle.MIND_READING, HeroAbility.PHOENIX_MIND);
        toggle(Toggle.WALL_CRAWL, HeroAbility.SPIDER_CRAWL);
        toggle(Toggle.SPIDER_SENSE, HeroAbility.SPIDER_CRAWL);
        toggle(Toggle.SPIDER_LEGS, HeroAbility.SPIDER_LEGS);
        toggle(Toggle.REALITY_VEIL, HeroAbility.THANOS_REALITY);
        toggle(Toggle.HEAR_THOUGHTS, HeroAbility.XAVIER_THOUGHTS);
        toggle(Toggle.TOMMY_TIME, HeroAbility.TOMMY_TIME);
        toggle(Toggle.TOMMY_WALL_RUN_OFF, HeroAbility.TOMMY_TIME);
        // 3.5
        toggle(Toggle.HEIMDALL_SIGHT, HeroAbility.HEIMDALL_SIGHT);
        toggle(Toggle.ODINSLEEP, HeroAbility.ODIN_COMMAND);
        toggle(Toggle.HELA_FORM, HeroAbility.HELA_GODDESS);
        // 3.6 (the Astral Projection and the Mirror Dimension are the Sneak variants of the bands and the portal)
        toggle(Toggle.STRANGE_ASTRAL, HeroAbility.STRANGE_BANDS);
        toggle(Toggle.STRANGE_MIRROR, HeroAbility.STRANGE_PORTAL);
        toggle(Toggle.TIME_SLOW, HeroAbility.STRANGE_EYE);
        toggle(Toggle.HUMAN_FORM, HeroAbility.HULK_TRANSFORM);
        // 3.7 (the claws, their climb and Track; lane L2 gives them gestures of their own)
        toggle(Toggle.CLAWS_OUT, HeroAbility.WOLVERINE_CLAWS);
        toggle(Toggle.CLAW_CLIMB, HeroAbility.WOLVERINE_CLAWS);
        toggle(Toggle.SCENT_TRACK, HeroAbility.WOLVERINE_SENSES);
        // 3.8 (Ross's form and Redwing; lanes L2 and L5 give them gestures of their own)
        toggle(Toggle.ROSS_FORM, HeroAbility.RED_HULK_TRANSFORM);
        toggle(Toggle.REDWING_OUT, HeroAbility.FALCON_REDWING);
        flag(GestureRoutes.Flag.MAGNETIC_SHIELD, HeroAbility.MAGNETO_SHIELD);
        flag(GestureRoutes.Flag.SHRAPNEL, HeroAbility.MAGNETO_SHRAPNEL);
        flag(GestureRoutes.Flag.BEAM, HeroAbility.WANDA_CHAOS_BEAM, HeroAbility.VISION_MIND_BEAM, HeroAbility.ULTRON_REPULSOR);
        flag(GestureRoutes.Flag.CHAOS_SHIELD, HeroAbility.WANDA_CHAOS_SHIELD, HeroAbility.APOCALYPSE_SHIELD, HeroAbility.AGATHA_SHIELD,
                HeroAbility.BILLY_FREEZE, HeroAbility.ODIN_ODINFORCE);
        flag(GestureRoutes.Flag.VISION_PHASED, HeroAbility.VISION_PHASE);
        flag(GestureRoutes.Flag.DENSE, HeroAbility.VISION_DENSITY);
        flag(GestureRoutes.Flag.DISGUISE, HeroAbility.VISION_DISGUISE);
        flag(GestureRoutes.Flag.QUICKSILVER_TIME, HeroAbility.QUICKSILVER_TIME);
        flag(GestureRoutes.Flag.SUPER_SPEED, HeroAbility.QUICKSILVER_SPEED);
        flag(GestureRoutes.Flag.WALKMAN, HeroAbility.QUICKSILVER_TIME);
    }

    /** The gesture the ability's main action plays on a cast marker, or null (its own route plays it). */
    @Nullable
    public static GestureId castMain(HeroAbility a) {
        Row row = ROWS.get(a);
        return row != null && row.mainCd ? row.main : null;
    }

    /** The gesture the ability's Sneak variant plays on a cast marker, or null. */
    @Nullable
    public static GestureId castAlt(HeroAbility a) {
        Row row = ROWS.get(a);
        return row != null && row.altCd ? row.alt : null;
    }

    /**
     * The gesture that stands for the ability's main action (or its variant), however it is told: what the editor's
     * preview plays for "Try" of the key. Null for an ability that has none (a passive, a message, an invisible trick).
     */
    @Nullable
    public static GestureId shown(HeroAbility a, boolean alt) {
        Row row = ROWS.get(a);
        return row == null ? null : alt ? row.alt : row.main;
    }

    /** A gesture the ability's cooldown starts later than its key does (the Overcharge's blast), or null. */
    @Nullable
    public static GestureId late(HeroAbility a) {
        Row row = ROWS.get(a);
        return row == null ? null : row.late;
    }

    /** Whether the table has a row for the ability (every row of {@link HeroAbility} must). */
    public static boolean covers(HeroAbility a) {
        return ROWS.containsKey(a);
    }

    /** Every gesture the ability may start. */
    public static Set<GestureId> gestures(HeroAbility a) {
        Row row = ROWS.get(a);
        return row == null ? Set.of() : Collections.unmodifiableSet(row.gestures);
    }

    /** Every stance the ability may raise. */
    public static Set<Stance> stances(HeroAbility a) {
        Row row = ROWS.get(a);
        return row == null ? Set.of() : Collections.unmodifiableSet(row.stances);
    }

    /** The abilities a live state may belong to (in the order a kit's first one is taken). */
    public static List<HeroAbility> owners(Live live) {
        return LIVE_OWNERS.getOrDefault(live, List.of());
    }

    /** The abilities a toggle belongs to. */
    public static List<HeroAbility> owners(Toggle toggle) {
        return TOGGLE_OWNERS.getOrDefault(toggle, List.of());
    }

    /** The abilities a flag belongs to. */
    public static List<HeroAbility> owners(GestureRoutes.Flag flag) {
        return FLAG_OWNERS.getOrDefault(flag, List.of());
    }

    /** The ability of the kit a live state belongs to, or null. */
    @Nullable
    public static HeroAbility ownerOf(Live live, HeroKit kit) {
        return first(owners(live), kit);
    }

    /** The ability of the kit a toggle belongs to, or null. */
    @Nullable
    public static HeroAbility ownerOf(Toggle toggle, HeroKit kit) {
        return first(owners(toggle), kit);
    }

    /** The ability of the kit a flag belongs to, or null. */
    @Nullable
    public static HeroAbility ownerOf(GestureRoutes.Flag flag, HeroKit kit) {
        return first(owners(flag), kit);
    }

    @Nullable
    private static HeroAbility first(List<HeroAbility> rows, HeroKit kit) {
        for (HeroAbility a : rows) {
            if (kit.has(a)) {
                return a;
            }
        }
        return null;
    }

    // ---- the kit filter (kept for the last kit asked about: a kit is immutable, one per definition) ----

    @Nullable
    private static HeroKit cachedKit;
    private static Set<GestureId> cachedGestures = Set.of();
    private static Set<Stance> cachedStances = Set.of();

    /**
     * Whether a gesture belongs to the kit: a row's, a generic module's default or a move the definition chose, or one of
     * the body's ({@link #SHARED}: a take-off, a landing). A hero's plug-in that starts gestures of its own (its edges, its
     * routes) is run for the custom hero through this filter, so he only acts what his abilities do.
     */
    public static synchronized boolean allows(HeroKit kit, GestureId gesture) {
        cache(kit);
        return SHARED.contains(gesture) || cachedGestures.contains(gesture);
    }

    /** The stances the kit's abilities may raise (the filter of their heroes' stance targets). */
    public static synchronized Set<Stance> stancesOf(HeroKit kit) {
        cache(kit);
        return cachedStances;
    }

    private static void cache(HeroKit kit) {
        if (cachedKit == kit) {
            return;
        }
        EnumSet<GestureId> gestures = EnumSet.noneOf(GestureId.class);
        EnumSet<Stance> stances = EnumSet.noneOf(Stance.class);
        for (HeroAbility a : kit.rows()) {
            gestures.addAll(gestures(a));
            stances.addAll(stances(a));
        }
        CustomHero hero = kit.hero();
        for (CustomHero.Pick pick : hero.powers()) {
            PowerModule module = pick.module();
            if (module == null) {
                continue;
            }
            for (PowerModule.Option option : module.options()) {
                add(gestures, CustomAnimRoutes.defaultGesture(module, option.id(), true));
                add(gestures, CustomAnimRoutes.defaultGesture(module, option.id(), false));
            }
            if (module == PowerModule.TELEKINESIS) {
                gestures.add(GestureId.TK_GRAB_WANDA);
                gestures.add(GestureId.TK_FLING_WANDA);
            }
        }
        for (String move : hero.moves()) {
            add(gestures, CustomAnimRoutes.gesture(move));
        }
        cachedGestures = Collections.unmodifiableSet(gestures);
        cachedStances = Collections.unmodifiableSet(stances);
        cachedKit = kit;
    }

    private static void add(Set<GestureId> set, @Nullable GestureId id) {
        if (id != null) {
            set.add(id);
        }
    }

    // ---- building the tables ----

    private static Builder row(HeroAbility a) {
        Row row = new Row();
        ROWS.put(a, row);
        return new Builder(row);
    }

    private static void live(Live live, HeroAbility... rows) {
        LIVE_OWNERS.put(live, List.of(rows));
    }

    private static void toggle(Toggle toggle, HeroAbility... rows) {
        TOGGLE_OWNERS.put(toggle, List.of(rows));
    }

    private static void flag(GestureRoutes.Flag flag, HeroAbility... rows) {
        FLAG_OWNERS.put(flag, List.of(rows));
    }

    private record Builder(Row row) {
        Builder main(GestureId id) {
            row.main = id;
            row.gestures.add(id);
            return this;
        }

        Builder mainCd(GestureId id) {
            row.mainCd = true;
            return main(id);
        }

        Builder alt(GestureId id) {
            row.alt = id;
            row.gestures.add(id);
            return this;
        }

        Builder altCd(GestureId id) {
            row.altCd = true;
            return alt(id);
        }

        Builder late(GestureId id) {
            row.late = id;
            row.gestures.add(id);
            return this;
        }

        Builder also(GestureId... ids) {
            row.gestures.addAll(List.of(ids));
            return this;
        }

        Builder stances(Stance... stances) {
            row.stances.addAll(List.of(stances));
            return this;
        }
    }

    private HeroAbilityAnims() {
    }
}

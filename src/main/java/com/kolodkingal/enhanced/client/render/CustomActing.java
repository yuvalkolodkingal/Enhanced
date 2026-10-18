package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.custom.CustomAnimRoutes;
import com.kolodkingal.enhanced.client.custom.HeroAbilityAnims;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * How a custom hero acts (3.4): the animation of the Hero Creator's heroes, as good as the built-in heroes'. Everything
 * here runs only for {@code Power.CUSTOM}; a built-in hero's animation never comes through this class.
 * <ul>
 *     <li>His <b>identity</b> ({@link #identity}): whose flight profile, take-off and flight effects he flies with, whose
 *     walk he walks with, his speedster run and his idle stance, from his definition's {@code anim} block
 *     ({@link CustomAnimRoutes}); the heroes his abilities come from.</li>
 *     <li>His <b>gestures</b>: the cast marker of his state tells every client which binding acted
 *     ({@link #castEdge}); the bits of his data that a borrowed ability flips are routed by the hero that owns them in his
 *     kit ({@link #toggle}, {@link #flag}, {@link #live}, {@link #grab}); the edges of those heroes' plug-ins run for him
 *     through the kit filter ({@link #edges}), and a borrowed gesture keeps its owner's flourishes
 *     ({@link #gestureStarted}).</li>
 *     <li>His <b>stances</b> ({@link #stanceTargets}): the stance targets of the plug-ins his abilities come from (only
 *     their abilities' stances), the stance each held or switched-on power chose (or its module's default), and the idle
 *     one at rest; the rows of the plug-ins that own what is up are drawn ({@link #ambient}, {@link #aimed},
 *     {@link #lean}, {@link #drop}).</li>
 *     <li>His <b>hands</b> in first person ({@link #firstPerson}), and the gesture his own key starts before the server
 *     answers ({@link #predicted}).</li>
 * </ul>
 */
public final class CustomActing {
    /** A key's marker plays the gesture of the binding; a cooldown that rises within this many ticks of it is the same act. */
    private static final float MARKER_WINDOW = 2.0F;
    /** How long a hero ability's own routes keep quiet after its binding played a move of the definition's (ticks). */
    private static final float QUIET_TICKS = 6.0F;
    /** How long the last binding told is the one an ambiguous state (two abilities that cast) belongs to (ticks). */
    private static final float OWNER_WINDOW = 10.0F;
    private static final boolean[] SCRATCH = new boolean[Stance.COUNT];

    /** The kit of a custom hero (the one on his data, or built from his definition). */
    static HeroKit kit(Player player, PowerData data) {
        HeroKit kit = data.kit();
        return kit != null ? kit : CustomHeroes.state(player).kitFor(CustomHeroes.hero(player));
    }

    // ====================================================================================================================
    // Identity
    // ====================================================================================================================

    /**
     * Who the hero flies, walks and speeds as: himself for a built-in hero; for a custom hero what his definition says
     * (worked out again when the definition changes; that also reseeds his state, as a new power does).
     */
    static void identity(HeroAnimState s, AbstractClientPlayer p, PowerData data) {
        if (data.power() != Power.CUSTOM) {
            s.kitStamp = null;
            s.kitSources.clear();
            s.flightAs = data.power();
            s.flightWhite = "white_vision".equals(data.variant());
            s.runAs = "";
            s.idle = null;
            return;
        }
        CustomHero hero = CustomHeroes.hero(p);
        HeroKit kit = kit(p, data);
        if (s.kitStamp != hero) {
            if (s.kitStamp != null) {
                s.reseed(Power.CUSTOM);
            }
            s.kitStamp = hero;
            settle(s, hero, kit);
        }
        s.customFlightSpeed = EnhancedConfig.SPEC.isLoaded() ? CustomHeroes.flightSpeed(p) : 0.05F;
    }

    /** Works the identity out of a definition and its kit (the world's hero, or the editor's puppet). */
    static void settle(HeroAnimState s, CustomHero hero, HeroKit kit) {
        s.kitSources.clear();
        s.kitSources.addAll(kit.sources());
        String flight = CustomAnimRoutes.flight(hero, kit);
        s.flightAs = CustomAnimRoutes.flightPower(flight);
        s.flightWhite = CustomAnimRoutes.flightWhite(flight);
        s.runAs = CustomAnimRoutes.run(hero, kit);
        s.idle = CustomAnimRoutes.idle(hero);
    }

    /** The speedster run style while he speeds (see {@code HeroAnimState.styleKind}). */
    static int styleKind(HeroAnimState s, AbstractClientPlayer p, PowerData data) {
        if (!s.speeding) {
            return 0;
        }
        HeroKit kit = kit(p, data);
        return CustomAnimRoutes.speedRun(kit.hero(), kit, data.slowMo());
    }

    // ====================================================================================================================
    // Gestures
    // ====================================================================================================================

    /** The cast marker moved: the binding it names acted, and plays its gesture (its move, its ability's, its module's). */
    static void castEdge(HeroAnimState s, AbstractClientPlayer p, PowerData data, float age) {
        CustomState state = CustomHeroes.state(p);
        int seq = state.castSeq();
        if (s.lastCastSeq < 0 || seq == s.lastCastSeq) {
            return;
        }
        HeroKit kit = kit(p, data);
        CustomHero hero = kit.hero();
        int column = state.castColumn();
        boolean sneak = state.castSneak();
        HeroKit.Binding binding = sneak ? kit.sneak(column) : kit.plain(column);
        if (binding == null) {
            return;
        }
        HeroAbility row = binding.ability();
        s.lastCastRow = row;
        s.lastCastAge = age;
        if (row != null && CustomAnimRoutes.chosen(hero, column, sneak)) {
            s.rowQuietUntil[row.ordinal()] = age + QUIET_TICKS;
        }
        PowerModule module = binding.module();
        boolean on = module == null || (module == PowerModule.FORCE_FIELD ? state.field() : state.isOn(module));
        GestureTable.fire(s, p, CustomAnimRoutes.cast(hero, kit, column, sneak, on), 0.0F, age);
    }

    /**
     * A cooldown of his rose on key {@code column}: what the key's marker did not already play. Only a gesture a cooldown
     * tells later than the key is left (the Overcharge's blast flies when it has charged).
     */
    static void cooldownRose(HeroAnimState s, AbstractClientPlayer p, PowerData data, int column, float age) {
        if (age - s.lastCastAge <= MARKER_WINDOW) {
            return;
        }
        HeroKit kit = kit(p, data);
        for (HeroKit.Binding b : new HeroKit.Binding[]{kit.plain(column), kit.sneak(column)}) {
            HeroAbility row = b == null ? null : b.ability();
            GestureId late = row == null ? null : HeroAbilityAnims.late(row);
            if (late != null && !quiet(s, row, age)) {
                GestureTable.fire(s, p, late, 0.0F, age);
            }
        }
    }

    /** The marker's binding is remembered for the next tick. */
    static void remember(HeroAnimState s, Player p) {
        s.lastCastSeq = CustomHeroes.state(p).castSeq();
    }

    /** The gesture a toggle of his switching stands for: its owner's in his kit, or null. */
    @Nullable
    static GestureId toggle(HeroAnimState s, HeroKit kit, Toggle toggle, boolean on, float age) {
        HeroAbility row = owner(s, HeroAbilityAnims.owners(toggle), kit, age);
        return row == null || quiet(s, row, age) ? null : GestureRoutes.toggle(row.hero, toggle, on);
    }

    /** The gesture a flag of his flipping stands for: its owner's in his kit, or null. */
    @Nullable
    static GestureId flag(HeroAnimState s, HeroKit kit, GestureRoutes.Flag flag, boolean on, boolean phased, float age) {
        HeroAbility row = owner(s, HeroAbilityAnims.owners(flag), kit, age);
        return row == null || quiet(s, row, age) ? null : GestureRoutes.flag(row.hero, flag, on, false, phased);
    }

    /** The gesture a state of his beginning stands for: its owner's in his kit, or null (a generic power's is its marker's). */
    @Nullable
    static GestureId live(HeroAnimState s, HeroKit kit, Live live, boolean armed, float age) {
        HeroAbility row = owner(s, HeroAbilityAnims.owners(live), kit, age);
        return row == null || quiet(s, row, age) ? null : GestureRoutes.live(row.hero, live, armed);
    }

    /** The grab of his telekinesis: the style of the hero whose grip he has, or the Scarlet Witch's for the generic one. */
    @Nullable
    static GestureId grab(HeroKit kit) {
        HeroAbility row = grip(kit);
        return row != null ? GestureRoutes.grab(row.hero) : GestureId.TK_GRAB_WANDA;
    }

    /** The throw of what his telekinesis held, in the same style. */
    @Nullable
    static GestureId fling(HeroKit kit) {
        HeroAbility row = grip(kit);
        return row != null ? GestureRoutes.fling(row.hero) : GestureId.TK_FLING_WANDA;
    }

    @Nullable
    private static HeroAbility grip(HeroKit kit) {
        for (HeroAbility a : new HeroAbility[]{HeroAbility.MAGNETO_METAL, HeroAbility.WANDA_TELEKINESIS, HeroAbility.BILLY_TELEKINESIS,
                HeroAbility.PHOENIX_TELEKINESIS}) {
            if (kit.has(a)) {
                return a;
            }
        }
        return null;
    }

    /** The owner of a bit among its candidates: the ability the marker just told if it is one, else the kit's first. */
    @Nullable
    private static HeroAbility owner(HeroAnimState s, List<HeroAbility> candidates, HeroKit kit, float age) {
        if (s.lastCastRow != null && age - s.lastCastAge <= OWNER_WINDOW && candidates.contains(s.lastCastRow) && kit.has(s.lastCastRow)) {
            return s.lastCastRow;
        }
        for (HeroAbility a : candidates) {
            if (kit.has(a)) {
                return a;
            }
        }
        return null;
    }

    private static boolean quiet(HeroAnimState s, HeroAbility row, float age) {
        return age < s.rowQuietUntil[row.ordinal()];
    }

    /** Whether a gesture belongs to his kit (the filter every gesture the table starts for him passes). */
    static boolean allowed(Player p, PowerData data, GestureId id) {
        return HeroAbilityAnims.allows(kit(p, data), id);
    }

    /** The edges of the plug-ins his abilities come from, each gesture they start passing the kit filter. */
    public static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        HeroKit kit = kit(p, data);
        HeroKit before = HeroAnimator.startFilter;
        HeroAnimator.startFilter = kit;
        try {
            for (Power source : s.kitSources) {
                HeroClient hero = HeroClients.of(source);
                if (hero != null) {
                    hero.edges(s, p, data, now);
                }
            }
        } finally {
            HeroAnimator.startFilter = before;
        }
    }

    /** A gesture began: its owner's flourishes go with it (Thor's sparks, the Phoenix's motes...). */
    public static void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        HeroClient owner = Gestures.owner(id);
        if (owner != null && owner.power() != Power.CUSTOM) {
            owner.gestureStarted(player, state, id, param);
        }
    }

    /**
     * The gesture his own key press starts at once: a generic power's default (or the move he chose for it) when the key
     * is rested; a built-in hero's ability waits for the server's answer (one round trip).
     */
    @Nullable
    public static GestureId predicted(PowerData data, int column, long now, boolean sneaking, Player player) {
        HeroKit kit = kit(player, data);
        HeroKit.Binding binding = kit.binding(column, sneaking);
        if (binding == null || binding.module() == null || !data.isReady(column, now)) {
            return null;
        }
        CustomHero hero = kit.hero();
        PowerModule.Act act = binding.module().act(CustomAnimRoutes.option(hero, binding));
        if (act != PowerModule.Act.PRESS && act != PowerModule.Act.HOLD) {
            return null;
        }
        return CustomAnimRoutes.cast(hero, kit, column, binding.sneak(), true);
    }

    // ====================================================================================================================
    // Stances
    // ====================================================================================================================

    /**
     * His stances this tick, after the shared ones (the built-in heroes of before 3.0 raise theirs from the data, by the
     * abilities he has): the plug-ins' targets filtered by his kit, each pick's chosen stance, the idle one; then which
     * plug-ins draw rows for him.
     */
    public static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        HeroKit kit = kit(p, data);
        CustomHero hero = kit.hero();
        Set<Stance> allowed = HeroAbilityAnims.stancesOf(kit);
        for (Power source : s.kitSources) {
            HeroClient plugin = HeroClients.of(source);
            if (plugin == null || source == Power.CUSTOM) {
                continue;
            }
            java.util.Arrays.fill(SCRATCH, false);
            plugin.stanceTargets(s, p, data, SCRATCH, now);
            for (int i = 0; i < SCRATCH.length; i++) {
                if (SCRATCH[i] && allowed.contains(Stance.VALUES[i])) {
                    on[i] = true;
                }
            }
        }
        chosen(hero, data, CustomHeroes.state(p), on);
        idle(s, on);
        rowHeroes(s, on);
    }

    /** Each held or switched-on pick's stance: the one it chose in place of its own, or its module's default. */
    static void chosen(CustomHero hero, PowerData data, CustomState state, boolean[] on) {
        for (CustomHero.Pick pick : hero.powers()) {
            if (!CustomAnimRoutes.isActive(pick, data, state)) {
                continue;
            }
            Stance chosen = CustomAnimRoutes.stanceOf(pick);
            HeroAbility row = pick.ability();
            if (row != null) {
                if (CustomHero.AUTO.equals(pick.stance())) {
                    continue;
                }
                for (Stance own : HeroAbilityAnims.stances(row)) {
                    on[own.ordinal()] = false;
                }
            } else {
                PowerModule module = pick.module();
                Stance standard = module == null ? null : CustomAnimRoutes.defaultStance(module, pick.option());
                if (standard != null && standard != chosen) {
                    on[standard.ordinal()] = false;
                }
            }
            if (chosen != null) {
                on[chosen.ordinal()] = true;
            }
        }
    }

    /** At rest on the ground, nothing held up, he stands in his idle stance. */
    static void idle(HeroAnimState s, boolean[] on) {
        if (s.idle == null || !s.onGround || s.flyingEff || s.speeding) {
            return;
        }
        for (Stance stance : Stance.VALUES) {
            if (stance.aimed && on[stance.ordinal()]) {
                return;
            }
        }
        on[s.idle.ordinal()] = true;
    }

    /** The plug-ins that draw rows for him: those of his abilities, and the owners of the plug-in stances he has up. */
    static void rowHeroes(HeroAnimState s, boolean[] on) {
        s.rowHeroes.clear();
        s.rowHeroes.addAll(s.kitSources);
        for (Stance stance : Stance.VALUES) {
            if (on[stance.ordinal()] || s.stance[stance.ordinal()] > 0.001F) {
                Power owner = owner(stance);
                if (owner != null) {
                    s.rowHeroes.add(owner);
                }
            }
        }
        s.rowHeroes.remove(Power.CUSTOM);
    }

    /** The hero of 3.0 whose plug-in draws a stance (by its region of {@link Stance}), or null for the shared rows. */
    @Nullable
    public static Power owner(Stance stance) {
        String name = stance.name();
        if (name.startsWith("PHOENIX_")) {
            return Power.PHOENIX;
        } else if (name.startsWith("SPIDER_")) {
            return Power.SPIDER_MAN;
        } else if (name.startsWith("THANOS_")) {
            return Power.THANOS;
        } else if (name.startsWith("ULTRON_")) {
            return Power.ULTRON;
        } else if (name.startsWith("APOC_")) {
            return Power.APOCALYPSE;
        } else if (name.startsWith("CYCLOPS_")) {
            return Power.CYCLOPS;
        } else if (name.startsWith("XAVIER_")) {
            return Power.PROFESSOR_X;
        } else if (name.startsWith("AGATHA_")) {
            return Power.AGATHA;
        }
        return null;
    }

    /** The ambient rows of the plug-ins that draw for him. */
    static void ambient(PoseContext c) {
        for (Power power : c.st.rowHeroes) {
            HeroClient hero = HeroClients.of(power);
            if (hero != null) {
                hero.ambient(c);
            }
        }
    }

    /** The aimed rows of the plug-ins that draw for him. */
    static void aimed(PoseContext c) {
        for (Power power : c.st.rowHeroes) {
            HeroClient hero = HeroClients.of(power);
            if (hero != null) {
                hero.aimed(c);
            }
        }
    }

    /** The lean their stances add. */
    public static float lean(HeroAnimState st) {
        float lean = 0.0F;
        for (Power power : st.rowHeroes) {
            HeroClient hero = HeroClients.of(power);
            if (hero != null) {
                lean += hero.lean(st);
            }
        }
        return lean;
    }

    /** The dip their stances add. */
    public static float drop(HeroAnimState st) {
        float drop = 0.0F;
        for (Power power : st.rowHeroes) {
            HeroClient hero = HeroClients.of(power);
            if (hero != null) {
                drop += hero.drop(st);
            }
        }
        return drop;
    }

    /** His hands in first person: the rows of the plug-ins his abilities come from (the catalogue's are FirstPersonArms'). */
    public static void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        for (Power source : s.kitSources) {
            HeroClient hero = HeroClients.of(source);
            if (hero != null && source != Power.CUSTOM) {
                hero.firstPerson(s, player, main, off);
            }
        }
    }

    /** The dome of his shield: the style of the hero whose shield he has (the Force Field is drawn by CustomFx). */
    @Nullable
    public static ShieldStyle shieldStyle(PowerData data) {
        HeroKit kit = data.kit();
        HeroAbility row = kit == null ? null : HeroAbilityAnims.ownerOf(GestureRoutes.Flag.CHAOS_SHIELD, kit);
        return row == null || row == HeroAbility.BILLY_FREEZE ? null : ShieldStyles.of(row.hero);
    }

    /**
     * The colour of an effect of a borrowed ability: the built-in hero's own colour for him, the custom hero's own colours
     * for a custom hero ({@code glow}: his second colour), so a power he borrowed glows in his colours.
     */
    public static int tint(Player player, int builtIn, boolean glow) {
        if (Powers.get(player).power() != Power.CUSTOM) {
            return builtIn;
        }
        return glow ? Powers.glow(player) : Powers.color(player);
    }

    private CustomActing() {
    }
}

package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.thanos.StoneKind;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 3.6: the Infinity Stones as cards of the Hero Creator. A stone's ability is not a hero's: it belongs to the stone. A
 * custom hero may put a stone on a key (its use as the main action, its Sneak use as the variant), and the key works
 * only while he carries that stone and may use it (bare, in a Gauntlet he may wield, or in its vessel: {@code
 * StoneUse.carried}); else it says "You do not carry the Mind Stone." The Creator never makes a stone: it lets a hero
 * use the one he owns on a key. The key of a stone row goes to {@code stones/StoneRowKeys} (lane L2), never to a hero's
 * code.
 *
 * <p>Data only, written by the 3.6 skeleton from the plan's table (8.2); lane L2 fixes the data in place. Ids are
 * {@code stone.<stone>} ({@link #id}), names {@code hero_module.enhanced.stone.<stone>} (and {@code .alt}, {@code
 * .desc}). The server's {@code custom_hero.stoneRows} can take them all away.
 */
public enum StoneRow implements CatalogEntry {
    /** The Reality Stone: the bubbles that take what is shot at him (a switch) / the Transmute. */
    REALITY(StoneKind.REALITY, PowerModule.Act.TOGGLE, PowerModule.Act.PRESS, 2, PowerModule.Group.MIND, null, false),
    /** The Mind Stone: the Mind Beam (held) / the Mind Command. */
    MIND(StoneKind.MIND, PowerModule.Act.HOLD, PowerModule.Act.PRESS, 2, PowerModule.Group.MIND, Family.BEAM, false,
            Resource.CONT_BEAM),
    /** The Time Stone: Slow Time (a switch) / the Rewind; only for a sorcerer (its rule). */
    TIME(StoneKind.TIME, PowerModule.Act.TOGGLE, PowerModule.Act.PRESS, 2, PowerModule.Group.MIND, Family.TIME, false,
            Resource.SLOW_TIME),
    /** The Power Stone: the Purple Beam (held) / the Power Bolt. */
    POWER(StoneKind.POWER, PowerModule.Act.HOLD, PowerModule.Act.PRESS, 3, PowerModule.Group.ENERGY, Family.BEAM, false,
            Resource.CONT_BEAM),
    /** The Space Stone: the Portal Step / the Realm Picker (world-scale). */
    SPACE(StoneKind.SPACE, PowerModule.Act.PRESS, PowerModule.Act.PRESS, 2, PowerModule.Group.MOVE, null, true),
    /** The Soul Stone: the Soul Drain (held) / the Soul Sight. */
    SOUL(StoneKind.SOUL, PowerModule.Act.HOLD, PowerModule.Act.PRESS, 2, PowerModule.Group.MIND, null, false,
            Resource.SOUL_CHAIN);

    /** The stone this row puts on a key. */
    public final StoneKind kind;
    private final String id;
    private final PowerModule.Act act;
    private final PowerModule.Act alt;
    /** Points per point of strength. */
    public final int costPerPoint;
    private final PowerModule.Group group;
    @Nullable
    private final Family family;
    private final boolean world;
    private final Set<Resource> resources;

    StoneRow(StoneKind kind, PowerModule.Act act, PowerModule.Act alt, int costPerPoint, PowerModule.Group group,
            @Nullable Family family, boolean world, Resource... resources) {
        this.kind = kind;
        this.id = "stone." + kind.id();
        this.act = act;
        this.alt = alt;
        this.costPerPoint = costPerPoint;
        this.group = group;
        this.family = family;
        this.world = world;
        EnumSet<Resource> set = EnumSet.noneOf(Resource.class);
        set.addAll(List.of(resources));
        this.resources = Collections.unmodifiableSet(set);
    }

    /** {@code stone.mind}: the id a hero's JSON names it by. */
    @Override
    public String id() {
        return id;
    }

    /** {@code hero_module.enhanced.stone.mind}; {@code .alt} is its Sneak use's name, {@code .desc} its line. */
    @Override
    public String nameKey() {
        return "hero_module.enhanced." + id;
    }

    /** The lang key of its Sneak use's name. */
    public String altKey() {
        return nameKey() + ".alt";
    }

    @Override
    public String descKey() {
        return nameKey() + ".desc";
    }

    @Override
    @Nullable
    public Family family() {
        return family;
    }

    @Override
    public PowerModule.Group group() {
        return group;
    }

    /** The stone itself ({@code enhanced:mind_stone}). */
    @Override
    public String icon() {
        return "enhanced:" + kind.id() + "_stone";
    }

    /** Points for each point of strength (its damage and cooldown scale, as a hero's ability). */
    @Override
    public int cost(int strength) {
        return costPerPoint * Math.max(1, strength);
    }

    /** A stone row has no generic options: its choice is {@link HeroAbility#BOTH} or {@link HeroAbility#MAIN}. */
    @Override
    public List<PowerModule.Option> options() {
        return List.of();
    }

    @Override
    public PowerModule.Act act(String option) {
        return act;
    }

    public PowerModule.Act act() {
        return act;
    }

    /** How its Sneak use acts (every stone has one). */
    @Override
    public PowerModule.Act alt() {
        return alt;
    }

    /** The resources its main action drives (its Sneak use drives none of them). */
    @Override
    public Set<Resource> resources(String option) {
        return resources;
    }

    /** A stone is nobody's: no hero. */
    @Override
    @Nullable
    public Power hero() {
        return null;
    }

    @Override
    public boolean world() {
        return world;
    }

    @Override
    public boolean scales() {
        return true;
    }

    /** A stone gives no role (a Bearer is one by a god's row: {@link Trait#STONE_BEARER}). */
    @Override
    public Set<Trait> traits() {
        return Set.of();
    }

    @Override
    public StoneKind stone() {
        return kind;
    }

    /** The row of this id ({@code stone.mind}, any case), or null. */
    @Nullable
    public static StoneRow byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (StoneRow row : values()) {
            if (row.id.equals(wanted)) {
                return row;
            }
        }
        return null;
    }

    /** The row of a stone. */
    public static StoneRow of(StoneKind kind) {
        for (StoneRow row : values()) {
            if (row.kind == kind) {
                return row;
            }
        }
        throw new IllegalArgumentException(kind.name());
    }
}

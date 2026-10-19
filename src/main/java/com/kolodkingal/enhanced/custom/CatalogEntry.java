package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.thanos.StoneKind;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * One entry of the Hero Creator's catalogue (3.4): a generic {@link PowerModule} or a built-in hero's {@link
 * HeroAbility}, behind one face, so the rules, the kit, the editor and the HUD read them alike ({@link Catalog}); 3.6:
 * or an Infinity Stone's {@link StoneRow}.
 */
public interface CatalogEntry {
    /** The editor's cards that stack several entries of one kind (a card with a style switcher). */
    enum Family {
        TELEKINESIS, BEAM, SHIELD, SPEED, TIME, PHASE, SHAPE, WALL, MIND, BODY
    }

    /** The id a hero's JSON names it by ({@code flight}, {@code thor.lightning}). */
    String id();

    String nameKey();

    String descKey();

    /** Its family, or null when it is a card of its own. */
    @Nullable
    Family family();

    PowerModule.Group group();

    /** The item drawn on its button (an item id). */
    String icon();

    /** The power points it costs at {@code strength}. */
    int cost(int strength);

    /** A generic module's options (the first is the default); none for a hero's ability. */
    List<PowerModule.Option> options();

    /** How its key acts in {@code option} (NONE: a passive, it takes no key). */
    PowerModule.Act act(String option);

    /** How its Sneak variant acts (a hero's ability), or null. */
    @Nullable
    PowerModule.Act alt();

    /** The shared state it drives in {@code option} (see {@link Resource}). */
    Set<Resource> resources(String option);

    /** The hero whose ability it is, or null for a generic module. */
    @Nullable
    Power hero();

    /** A world-scale ability ({@code custom_hero.worldPowers}). */
    boolean world();

    /** Whether its strength slider changes anything (false: fixed at {@link HeroAbility#FIXED_STRENGTH}). */
    boolean scales();

    /** The roles it gives a custom hero ({@link Traits}). */
    Set<Trait> traits();

    /** 3.6: the Infinity Stone a stone row puts on a key ({@link StoneRow}), or null for any other entry. */
    @Nullable
    default StoneKind stone() {
        return null;
    }
}

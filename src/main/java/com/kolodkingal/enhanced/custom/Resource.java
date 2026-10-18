package com.kolodkingal.enhanced.custom;

/**
 * A piece of a player's state that only one hero's code may drive at a time (3.4): a custom hero may take abilities of
 * several heroes, but there is one telekinetic grip, one Chaos-Shield record, one beam flag, one continuous beam... per
 * player. Every entry of the catalogue says which it uses ({@link CatalogEntry#resources}); the rules
 * ({@link CustomHeroRules}) keep, for each resource, only the picks of one owner (one hero's abilities, or the generic
 * modules), dropping later picks that would break it. Abilities of the same hero may share a resource, as the built-in
 * hero does.
 *
 * <p>Append only per lane region: a lane that finds another shared field adds it at the end of its region.
 */
public enum Resource {
    /** {@code PowerData.heldEntityId}: the one thing held with telekinesis. */
    GRIP,
    /** The Chaos-Shield record of {@code PowerData} (Wanda's shield, Billy's barrier, Apocalypse's and Agatha's shields). */
    SHIELD,
    /** {@code PowerData.beamActive}: the beam flag (Wanda, Vision, Ultron). */
    BEAM_FLAG,
    /** One {@code ContinuousBeam} per player (Billy, Agatha, Cyclops; the generic beam, flamethrower, water jet). */
    CONT_BEAM,
    /** {@code PowerData.form}: the creature the player has become. */
    FORM,
    /** {@code Live.SPEEDING} and the run's speed modifiers. */
    SPEED_RUN,
    /** {@code PowerData.slowMo}: the world round the player runs slower. */
    SLOW_TIME,
    /** Passing through walls ({@code PowerData.visionPhased}, the generic phasing). */
    PHASE,
    /** Clinging to walls and ceilings. */
    WALL,
    /** The {@code Attributes.SCALE} of the player. */
    SCALE,
    /** {@code PowerData.hammerEntityId}: the thrown weapon. */
    WEAPON,
    /** The one Hex a player may own. */
    HEX,
    /** Ultron's drones and his Second Life. */
    DRONES,
    // (L4, 3.4) ----
    /** {@code PowerData.ironCharge}: Magneto's shrapnel charge, which is also Ultron's plasma pool. */
    IRON_CHARGE
}

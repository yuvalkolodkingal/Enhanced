package com.kolodkingal.enhanced.build;

import net.minecraft.core.BlockPos;

/**
 * The numbers {@link SpellRecipe#clamped} forces a recipe into: the hero's own (Billy's by his rank, from
 * {@link BillyRanks}; the Scarlet Witch's technical caps, from {@code [build_magic_wanda]}) and the world the caster stands
 * in. Lane BS's {@code Limits} implements it, read live from the config when a cast arrives.
 *
 * <p>Clamping only keeps every number in a range the planner can work with; whether a recipe is allowed at all (the reach
 * of its anchor, Billy's longest side, the size of a mask) is decided after it, with a refusal the builder is told
 * ({@code TOO_FAR}, {@code TOO_WIDE}, D-B 10.2 C7).
 */
public interface RecipeLimits {
    /** The longest side of anything the hero builds; also how many layers Pull &amp; Push adds or takes away at most. */
    int side();

    /** How far (blocks) the hero points; also how far a Copy's or a Move's ghost is lifted off its surface at most. */
    int reach();

    /** The biggest radius of a shape. */
    int shapeRadius();

    /** The tallest shape (the shapes with a height: Tower, Ring, Cone, Pyramid). */
    int shapeHeight();

    /** The most copies in a Repeat. */
    int repeatCopies();

    /** The biggest radius of a brush (Paint, Land, Grow). */
    int brush();

    /** The position nearest to {@code pos} that is inside the caster's world: its build height and its world border. */
    BlockPos inWorld(BlockPos pos);
}

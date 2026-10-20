package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ironman.SuitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;
import java.util.Map;

/**
 * The Avengers Tower's armoury (3.8, PLAN 9.2): its points in the generator's model frame (x east, z south, layer 2 the
 * ground; 33 wide, the top part's frame: the base and mid parts lie 4 in from its edges, see {@link #inTemplate}). The
 * 3.8 skeleton wrote the planned points; lane L6 corrected them to the templates its generator writes ({@code
 * SCRATCH/v38/art/l6/tower381.py}, {@code TowerDisplayTests}): the plan's points were those of a draft of the 3.6.1
 * workshop that never shipped (x 10 to 21); the shipped workshop is 9 wide (x 6 to 14, the shaft's east face at 15),
 * its five armour stands at x 6, 8, 10, 12, 14 and its beacon at (12, 63, 12). They map into the world through the
 * piece ({@code SitePoints.toWorld}).
 */
public final class TowerPlan {
    /** The base's and mid part's frame is the model's less this in x and z (the top part is the model's frame). */
    public static final int PAD = 4;

    /**
     * The suit displays of Iron Man's workshop (their lower halves; the upper half is the block above): the five along
     * the north wall where the 3.6.1 armour stands stood, Mark 1 in the west to Mark 85 in the east (glass panes
     * between them, each one's name on a sign over it), the Hulkbuster in its bay south of the desk.
     */
    public static final Map<SuitKind, BlockPos> SUIT_DISPLAYS = Map.of(
            SuitKind.MARK_1, new BlockPos(6, 62, 8),
            SuitKind.MARK_3, new BlockPos(8, 62, 8),
            SuitKind.MARK_5, new BlockPos(10, 62, 8),
            SuitKind.MARK_50, new BlockPos(12, 62, 8),
            SuitKind.MARK_85, new BlockPos(14, 62, 8),
            SuitKind.HULKBUSTER, new BlockPos(12, 62, 17));
    /** The Hulkbuster's bay: 3 x 5 x 3 kept clear round its display (it is drawn 1.75 times a man's size). */
    public static final BoundingBox HULKBUSTER_BAY = new BoundingBox(11, 62, 16, 13, 66, 18);
    /** The Arc Reactor's case on Tony's desk (the old beacon's place). */
    public static final BlockPos ARC_REACTOR_CASE = new BlockPos(12, 63, 12);
    /** The Nano Arc Reactor's case on the desk (a screen's place, east of the Arc Reactor). */
    public static final BlockPos NANO_REACTOR_CASE = new BlockPos(13, 63, 12);
    /** The lab's "Project Rebirth" case of the Super Soldier Serum, on the workbench. */
    public static final BlockPos SERUM_CASE = new BlockPos(12, 57, 11);
    /** Cap's shield case in his closet in the penthouse (the item frame's place). */
    public static final BlockPos SHIELD_CASE = new BlockPos(7, 83, 9);
    /** The Falcon Exo-Wings' tech display on the observation deck (the cartography table's place). */
    public static final BlockPos WINGS_CASE = new BlockPos(14, 90, 16);
    /** Where the observation deck's cartography table moves to. */
    public static final BlockPos CARTOGRAPHY_TABLE = new BlockPos(13, 90, 18);
    /** The lobby's reception sign (3.8: two more lines, "Iron Man's suits: the workshop"). */
    public static final BlockPos LOBBY_SIGN = new BlockPos(12, 6, 8);

    /** The rooms of the armoury: their insides (floor to ceiling, the building's inner span). */
    public static final BoundingBox LAB = new BoundingBox(5, 56, 5, 27, 60, 27);
    public static final BoundingBox WORKSHOP = new BoundingBox(5, 62, 5, 27, 72, 27);
    public static final BoundingBox PENTHOUSE = new BoundingBox(5, 83, 5, 27, 88, 27);
    public static final BoundingBox OBSERVATION = new BoundingBox(5, 90, 5, 27, 95, 27);

    /** Every case's point (the displays apart). */
    public static final List<BlockPos> CASES = List.of(ARC_REACTOR_CASE, NANO_REACTOR_CASE, SERUM_CASE, SHIELD_CASE,
            WINGS_CASE);

    /** What a case shows and gives, and whether only Captain America may take it. */
    public record Case(BlockPos pos, ResourceLocation item, boolean captainOnly) {
    }

    /** The five cases with their items (the templates' block entities hold the same). */
    public static final List<Case> CASE_ITEMS = List.of(
            new Case(ARC_REACTOR_CASE, Enhanced.id("arc_reactor"), false),
            new Case(NANO_REACTOR_CASE, Enhanced.id("nano_arc_reactor"), false),
            new Case(SERUM_CASE, Enhanced.id("special_soldier_serum"), false),
            new Case(SHIELD_CASE, Enhanced.id("vibranium_shield"), true),
            new Case(WINGS_CASE, Enhanced.id("falcon_exo_wings"), false));

    private static ResourceLocation enhanced(String path) {
        return ResourceLocation.fromNamespaceAndPath("enhanced", path);
    }

    /**
     * A point of the model frame in its template: which part ({@code 0} base, {@code 1} mid, {@code 2} top), the
     * template, and the point in the template's own frame.
     */
    public record InTemplate(int part, ResourceLocation template, BlockPos local) {
    }

    /** Where a model-frame point lies in the tower's three templates. */
    public static InTemplate inTemplate(BlockPos model) {
        int[] partY = StoneSitePlans.Tower.PART_Y;
        int part = model.getY() >= partY[2] ? 2 : model.getY() >= partY[1] ? 1 : 0;
        int pad = part == 2 ? 0 : PAD;
        return new InTemplate(part, StoneSitePlans.Tower.PARTS.get(part), model.offset(-pad, -partY[part], -pad));
    }

    /** A model-frame point in the whole tower's frame (the base's: {@code StoneSitePlans.Tower}'s). */
    public static BlockPos inTower(BlockPos model) {
        return model.offset(-PAD, 0, -PAD);
    }

    private TowerPlan() {
    }
}

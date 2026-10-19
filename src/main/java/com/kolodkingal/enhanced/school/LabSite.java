package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.world.SchoolLabPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

/**
 * Where a Mutant School lies in the world, and so its lower lab (3.7): the school piece's template position, its turn
 * and its pivot, exactly as the piece placed the school's template. A point of the school's template ({@link
 * SchoolLabPlan}'s study points) is {@link #school}, a point of the lab's template is {@link #lab} (the lab's origin
 * is the school-template point {@link SchoolLabPlan#LAB_ORIGIN}), both turned as the school was; a block of either
 * template is turned by {@link #turn}. Kept in the school's {@link SchoolLabs.Lab} record, so the lab can be built and
 * used without its structure start (and a test can make a school of its own).
 *
 * @param origin the school piece's template position (where the template's (0, 0, 0) went before turning)
 * @param turn   the school's turn
 * @param pivot  the turn's pivot, template-relative
 */
public record LabSite(BlockPos origin, Rotation turn, BlockPos pivot) {

    /** The site of a school piece. */
    public static LabSite of(TemplateStructurePiece piece) {
        return new LabSite(piece.templatePosition(), piece.placeSettings().getRotation(),
                piece.placeSettings().getRotationPivot());
    }

    /** Where the school's template point {@code templatePos} lies in the world. */
    public BlockPos school(BlockPos templatePos) {
        return StructureTemplate.transform(templatePos, Mirror.NONE, turn, pivot).offset(origin);
    }

    /** Where the lab's template point {@code local} lies in the world. */
    public BlockPos lab(BlockPos local) {
        return school(SchoolLabPlan.LAB_ORIGIN.offset(local));
    }

    public BlockPos lab(int x, int y, int z) {
        return school(new BlockPos(SchoolLabPlan.LAB_ORIGIN.getX() + x, SchoolLabPlan.LAB_ORIGIN.getY() + y,
                SchoolLabPlan.LAB_ORIGIN.getZ() + z));
    }

    /** A template block as the school's turn sets it down. */
    public BlockState turn(BlockState state) {
        return state.rotate(turn);
    }

    /** A template direction as the school's turn points it. */
    public Direction turn(Direction direction) {
        return turn.rotate(direction);
    }

    /** The lab's box in the world. */
    public BoundingBox box() {
        BlockPos a = lab(BlockPos.ZERO);
        BlockPos b = lab(SchoolLabPlan.LAB_SIZE.getX() - 1, SchoolLabPlan.LAB_SIZE.getY() - 1,
                SchoolLabPlan.LAB_SIZE.getZ() - 1);
        return BoundingBox.fromCorners(a, b);
    }

    /** Whether {@code pos} is inside the lab's box. */
    public boolean contains(BlockPos pos) {
        return box().isInside(pos);
    }

    /** The lab's box as an AABB (whole blocks). */
    public AABB bounds() {
        return AABB.of(box());
    }
}

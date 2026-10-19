package com.kolodkingal.enhanced.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * The fixed points of a built site (3.6): a point of a template ({@link StoneSitePlans}, template-relative and
 * unturned) where its piece put it in the world, turned as the piece was turned. Every 3.6 site is built of {@link
 * TemplateSitePiece}s; the Vormir Cliff is never turned (its points are its piece's corner plus the plan's offset), the
 * Sanctum, the tower and the End tunnel are.
 */
public final class SitePoints {

    /** Where the piece put the template's point {@code local}. */
    public static BlockPos toWorld(TemplateStructurePiece piece, BlockPos local) {
        return StructureTemplate.calculateRelativePosition(piece.placeSettings(), local)
                .offset(piece.templatePosition());
    }

    private SitePoints() {
    }
}

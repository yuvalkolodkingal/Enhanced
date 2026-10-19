package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Where the Vormir Cliffs are (3.6): the structure {@code enhanced:vormir_cliff} found through the level's structure
 * references (its first template piece gives the template's corner and turn), and the small cliffs the GameTests build
 * ({@link #add}, removed when their test ends).
 */
public final class VormirSites {
    /** The structure of the cliff (lane L4's template). */
    public static final ResourceKey<Structure> CLIFF = ResourceKey.create(Registries.STRUCTURE,
            Enhanced.id("vormir_cliff"));

    /** Sites that are not structures: the GameTests' small cliffs. */
    private static final List<VormirSite> EXTRA = new CopyOnWriteArrayList<>();

    /** A site that is not a structure (a GameTest's cliff), until {@link #remove}. */
    public static void add(VormirSite site) {
        EXTRA.add(site);
    }

    public static void remove(VormirSite site) {
        EXTRA.remove(site);
    }

    /** The sites that are not structures, in this level. */
    static List<VormirSite> extra(ServerLevel level) {
        return EXTRA.stream().filter(s -> s.dimension() == level.dimension()).toList();
    }

    /** The Vormir Cliff whose site holds {@code pos}, or null. Only loaded places are looked at. */
    @Nullable
    public static VormirSite at(ServerLevel level, BlockPos pos) {
        for (VormirSite site : EXTRA) {
            if (site.dimension() == level.dimension() && site.bounds().isInside(pos)) {
                return site;
            }
        }
        if (!level.isLoaded(pos)) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, h -> h.is(CLIFF));
        if (!start.isValid() || start.getPieces().isEmpty()) {
            return null;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof TemplateStructurePiece template) {
                StructurePlaceSettings settings = template.placeSettings();
                return VormirSite.of(level.dimension(), template.templatePosition(), settings.getMirror(),
                        settings.getRotation(), settings.getRotationPivot());
            }
        }
        // (A piece that is no template: its box's corner, unturned.)
        var box = start.getPieces().get(0).getBoundingBox();
        return VormirSite.of(level.dimension(), new BlockPos(box.minX(), box.minY(), box.minZ()));
    }

    private VormirSites() {
    }
}

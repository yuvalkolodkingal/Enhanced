package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.world.CraterPiece;
import com.kolodkingal.enhanced.world.CraterStructure;
import com.kolodkingal.enhanced.world.FoundationProcessor;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import com.kolodkingal.enhanced.world.TemplateSiteStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The kinds of world structures of 3.3 (the Hydra outposts, Mjolnir's landing sites and the Mutant School), and of 3.6
 * (the stones' sites: the Sanctum, Avengers Tower, the End tunnel, the Vormir Cliff). The structures themselves are
 * data (data/enhanced/worldgen/structure, structure_set, the biome tags and the templates); these are the code they are
 * built with.
 */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Enhanced.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Enhanced.MODID);
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Enhanced.MODID);

    /** One template set on fairly level ground, with a footing under it where the ground falls away. */
    public static final DeferredHolder<StructureType<?>, StructureType<TemplateSiteStructure>> TEMPLATE_SITE =
            STRUCTURE_TYPES.register("template_site", () -> () -> TemplateSiteStructure.CODEC);
    /** A crater struck from the sky, scorched round about, with something lying in its middle. */
    public static final DeferredHolder<StructureType<?>, StructureType<CraterStructure>> CRATER =
            STRUCTURE_TYPES.register("crater", () -> () -> CraterStructure.CODEC);

    // ===== stone_sites (3.6) =====
    // (The skeleton registers the three new kinds with stand-in bodies; lane L4 owns them and registers its own
    // pieces.)
    /** A site on the floor of a tall Nether cavern: the Vormir Cliff. */
    public static final DeferredHolder<StructureType<?>,
            StructureType<com.kolodkingal.enhanced.world.NetherSiteStructure>> NETHER_SITE =
            STRUCTURE_TYPES.register("nether_site",
                    () -> () -> com.kolodkingal.enhanced.world.NetherSiteStructure.CODEC);
    /** A site carved into an End island: the End tunnel. */
    public static final DeferredHolder<StructureType<?>,
            StructureType<com.kolodkingal.enhanced.world.CarvedSiteStructure>> CARVED_SITE =
            STRUCTURE_TYPES.register("carved_site",
                    () -> () -> com.kolodkingal.enhanced.world.CarvedSiteStructure.CODEC);
    /** Several templates stacked: Avengers Tower. */
    public static final DeferredHolder<StructureType<?>,
            StructureType<com.kolodkingal.enhanced.world.TowerSiteStructure>> TOWER_SITE =
            STRUCTURE_TYPES.register("tower_site", () -> () -> com.kolodkingal.enhanced.world.TowerSiteStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TEMPLATE_SITE_PIECE =
            STRUCTURE_PIECES.register("template_site", () -> TemplateSitePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CRATER_PIECE =
            STRUCTURE_PIECES.register("crater", () -> (StructurePieceType.ContextlessType) CraterPiece::new);

    // ===== stone_sites (3.6): the seal against fluids =====
    /** Seals a template's carved air against the world's lava and water (see {@code world.SealProcessor}). */
    public static final DeferredHolder<StructureProcessorType<?>,
            StructureProcessorType<com.kolodkingal.enhanced.world.SealProcessor>> SEAL =
            PROCESSORS.register("seal", () -> () -> com.kolodkingal.enhanced.world.SealProcessor.CODEC);

    /** Fills the ground in under a template where it falls away (see {@link FoundationProcessor}). */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<FoundationProcessor>> FOUNDATION =
            PROCESSORS.register("foundation", () -> () -> FoundationProcessor.CODEC);

    private ModStructures() {
    }
}

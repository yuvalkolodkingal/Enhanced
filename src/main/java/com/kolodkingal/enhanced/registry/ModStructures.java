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
 * The kinds of world structures of 3.3 (the Hydra outposts, Mjolnir's landing sites and the Mutant School). The
 * structures themselves are data (data/enhanced/worldgen/structure, structure_set, the biome tags and the templates);
 * these are the code they are built with.
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

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TEMPLATE_SITE_PIECE =
            STRUCTURE_PIECES.register("template_site", () -> TemplateSitePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CRATER_PIECE =
            STRUCTURE_PIECES.register("crater", () -> (StructurePieceType.ContextlessType) CraterPiece::new);

    /** Fills the ground in under a template where it falls away (see {@link FoundationProcessor}). */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<FoundationProcessor>> FOUNDATION =
            PROCESSORS.register("foundation", () -> () -> FoundationProcessor.CODEC);

    private ModStructures() {
    }
}

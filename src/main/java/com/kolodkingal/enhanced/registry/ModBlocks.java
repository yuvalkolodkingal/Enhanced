package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.asgard.GungnirStandBlock;
import com.kolodkingal.enhanced.block.asgard.HelaSealBlock;
import com.kolodkingal.enhanced.block.asgard.OdinThroneBlock;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.block.SpiderWebBlock;
import com.kolodkingal.enhanced.block.WitchesRoadDoorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Enhanced.MODID);

    /** Mjolnir at rest: unbreakable, unmovable, blast-proof. Its item is {@link ModItems#MJOLNIR}, not a block item. */
    public static final DeferredBlock<MjolnirBlock> MJOLNIR = BLOCKS.register("mjolnir",
            () -> new MjolnirBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .sound(SoundType.ANVIL)
                    .pushReaction(PushReaction.BLOCK)
                    .noOcclusion()));

    /**
     * The door to the Witches' Road, set flat in the floor. Wood (an axe breaks it fastest); pistons can't push it; its
     * blue-green outline glows faintly, and the open Road pours out light.
     */
    public static final DeferredBlock<WitchesRoadDoorBlock> WITCHES_ROAD_DOOR = BLOCKS.registerBlock("witches_road_door",
            WitchesRoadDoorBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> state.getValue(WitchesRoadDoorBlock.OPEN) ? 12 : 3)
                    .isValidSpawn(Blocks::never));

    // ===== spider =====
    /** The temporary web of Spider-Man: no item, no drop; a hand or a sword clears it (hardness 0.5, no tool needed). */
    public static final DeferredBlock<SpiderWebBlock> SPIDER_WEB = BLOCKS.registerBlock("spider_web",
            SpiderWebBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .sound(SoundType.COBWEB)
                    .forceSolidOn()
                    .noCollission()
                    .noOcclusion()
                    .strength(0.5F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable());

    // ===== asgard (3.5) =====
    // (The skeleton registers the three blocks whose behaviour is another lane's; lane L1 adds the realm's blocks here.)
    /** Hela's Seal behind the throne: unbreakable, no item; its click calls Hela (lane L4). */
    public static final DeferredBlock<HelaSealBlock> HELA_SEAL = BLOCKS.registerBlock("hela_seal",
            HelaSealBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 4)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()
                    .isValidSpawn(Blocks::never));
    /** The Throne of Asgard: unbreakable in the city; sitting on it is the coronation (lane L3). */
    public static final DeferredBlock<OdinThroneBlock> ODIN_THRONE = BLOCKS.registerBlock("odin_throne",
            OdinThroneBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable());
    /** Gungnir's stand in the vault: what it gives is lane L3's. */
    public static final DeferredBlock<GungnirStandBlock> GUNGNIR_STAND = BLOCKS.registerBlock("gungnir_stand",
            GungnirStandBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable());
    // ===== heimdall (3.5) =====
    // ===== odin (3.5) =====
    // ===== hela (3.5) =====
    // ===== asgardians (3.5) =====

    private ModBlocks() {
    }
}

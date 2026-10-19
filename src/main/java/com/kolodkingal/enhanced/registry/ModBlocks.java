package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.asgard.AsgardBenchBlock;
import com.kolodkingal.enhanced.block.asgard.AsgardBrazierBlock;
import com.kolodkingal.enhanced.block.asgard.AsgardMuralBlock;
import com.kolodkingal.enhanced.block.asgard.BifrostBridgeBlock;
import com.kolodkingal.enhanced.block.asgard.BifrostMechanismBlock;
import com.kolodkingal.enhanced.block.asgard.BifrostRuneStoneBlock;
import com.kolodkingal.enhanced.block.asgard.CosmicFallsBlock;
import com.kolodkingal.enhanced.block.asgard.GungnirStandBlock;
import com.kolodkingal.enhanced.block.asgard.HofundSocketBlock;
import com.kolodkingal.enhanced.block.asgard.HelaSealBlock;
import com.kolodkingal.enhanced.block.asgard.OdinThroneBlock;
import com.kolodkingal.enhanced.block.GammaEmitterBlock;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.block.SpiderWebBlock;
import com.kolodkingal.enhanced.block.StonePedestalBlock;
import com.kolodkingal.enhanced.block.TesseractCradleBlock;
import com.kolodkingal.enhanced.block.WitchesRoadDoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
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
    // The realm's own blocks (lane L1): the gold and the realm stone of the city, the bridge, the falls, the paintings of
    // the throne room, the Observatory's socket and mechanism, braziers, benches and the Overworld's rune stones.
    public static final DeferredBlock<Block> ASGARD_GOLD_PLATING = BLOCKS.registerSimpleBlock("asgard_gold_plating", gold());
    public static final DeferredBlock<Block> ASGARD_GOLD_TRIM = BLOCKS.registerSimpleBlock("asgard_gold_trim", gold());
    public static final DeferredBlock<RotatedPillarBlock> ASGARD_GOLD_PILLAR = BLOCKS.registerBlock("asgard_gold_pillar",
            RotatedPillarBlock::new, gold());
    public static final DeferredBlock<IronBarsBlock> ASGARD_GOLD_LATTICE = BLOCKS.registerBlock("asgard_gold_lattice",
            IronBarsBlock::new, gold().noOcclusion());
    public static final DeferredBlock<Block> REALM_STONE = BLOCKS.registerSimpleBlock("realm_stone", realmStone());
    public static final DeferredBlock<Block> REALM_STONE_BRICKS = BLOCKS.registerSimpleBlock("realm_stone_bricks", realmStone());
    public static final DeferredBlock<Block> POLISHED_REALM_STONE = BLOCKS.registerSimpleBlock("polished_realm_stone", realmStone());
    public static final DeferredBlock<RotatedPillarBlock> REALM_STONE_PILLAR = BLOCKS.registerBlock("realm_stone_pillar",
            RotatedPillarBlock::new, realmStone());
    public static final DeferredBlock<StairBlock> REALM_STONE_BRICK_STAIRS = BLOCKS.registerBlock("realm_stone_brick_stairs",
            p -> new StairBlock(REALM_STONE_BRICKS.get().defaultBlockState(), p), realmStone());
    public static final DeferredBlock<SlabBlock> REALM_STONE_BRICK_SLAB = BLOCKS.registerBlock("realm_stone_brick_slab",
            SlabBlock::new, realmStone());
    public static final DeferredBlock<WallBlock> REALM_STONE_BRICK_WALL = BLOCKS.registerBlock("realm_stone_brick_wall",
            WallBlock::new, realmStone().forceSolidOn());
    /** The Rainbow Bridge: glowing crystal, its light flowing along its axis. */
    public static final DeferredBlock<BifrostBridgeBlock> BIFROST_BRIDGE = BLOCKS.registerBlock("bifrost_bridge",
            BifrostBridgeBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 12)
                    .noOcclusion()
                    .isValidSpawn(Blocks::never)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));
    /** The cosmic falls: light-water falling into the void; no collision, no fluid, no ticks. */
    public static final DeferredBlock<CosmicFallsBlock> COSMIC_FALLS = BLOCKS.registerBlock("cosmic_falls",
            CosmicFallsBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .isValidSpawn(Blocks::never)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));
    /** A tile of the throne room's paintings (peace or conquest). */
    public static final DeferredBlock<AsgardMuralBlock> ASGARD_MURAL = BLOCKS.registerBlock("asgard_mural",
            AsgardMuralBlock::new, realmStone());
    /** The Hofund socket in the Observatory: unbreakable, no item. */
    public static final DeferredBlock<HofundSocketBlock> HOFUND_SOCKET = BLOCKS.registerBlock("hofund_socket",
            HofundSocketBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable());
    /** The Bifrost mechanism: the Observatory's spinning ring; unbreakable, no item. */
    public static final DeferredBlock<BifrostMechanismBlock> BIFROST_MECHANISM = BLOCKS.registerBlock("bifrost_mechanism",
            BifrostMechanismBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 7)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable());
    /** A golden brazier, always lit. */
    public static final DeferredBlock<AsgardBrazierBlock> ASGARD_BRAZIER = BLOCKS.registerBlock("asgard_brazier",
            AsgardBrazierBlock::new, gold().noOcclusion().lightLevel(state -> 15));
    /** A Bifrost rune stone, the heart of an Overworld Bifrost site: unbreakable, no item. */
    public static final DeferredBlock<BifrostRuneStoneBlock> BIFROST_RUNE_STONE = BLOCKS.registerBlock("bifrost_rune_stone",
            BifrostRuneStoneBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 5)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()
                    .isValidSpawn(Blocks::never));
    /** The golden leaves of Asgard's trees (they never fall). */
    public static final DeferredBlock<LeavesBlock> ASGARD_GOLDEN_LEAVES = BLOCKS.registerBlock("asgard_golden_leaves",
            LeavesBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.AZALEA_LEAVES)
                    .noOcclusion()
                    .lightLevel(state -> 3)
                    .isValidSpawn(Blocks::never)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((state, level, pos) -> false));
    /** A stone bench to sit on. */
    public static final DeferredBlock<AsgardBenchBlock> ASGARD_BENCH = BLOCKS.registerBlock("asgard_bench",
            AsgardBenchBlock::new, realmStone().noOcclusion());

    private static BlockBehaviour.Properties gold() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).instrument(NoteBlockInstrument.BELL)
                .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties realmStone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5F, 6.0F);
    }

    // ===== heimdall (3.5) =====
    // ===== odin (3.5) =====
    // ===== hela (3.5) =====
    // ===== asgardians (3.5) =====

    // ===== stones (3.6) =====
    // (The skeleton registers every block of 3.6, with placeholder looks; each lane owns its classes.)
    /** The stone pedestal of a site: unbreakable, no loot (lane L1). */
    public static final DeferredBlock<StonePedestalBlock> STONE_PEDESTAL = BLOCKS.registerBlock(
            "stone_pedestal", StonePedestalBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(StonePedestalBlock.STONE)
                            == StonePedestalBlock.Stone.NONE ? 0 : 10)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable());
    // ===== stone_sites (3.6) =====
    /** The Tesseract's cradle on Avengers Tower: unbreakable, no loot (lane L4). */
    public static final DeferredBlock<TesseractCradleBlock> TESSERACT_CRADLE = BLOCKS.registerBlock(
            "tesseract_cradle", TesseractCradleBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable());
    // ===== hulk (3.6) =====
    /** The Gamma Emitter: a crafted lab device; it drops itself (lane L7). */
    public static final DeferredBlock<GammaEmitterBlock> GAMMA_EMITTER = BLOCKS.registerBlock(
            "gamma_emitter", GammaEmitterBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 4));

    private ModBlocks() {
    }
}

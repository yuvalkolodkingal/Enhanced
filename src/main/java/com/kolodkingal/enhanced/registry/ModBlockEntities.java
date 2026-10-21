package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.MjolnirBlockEntity;
import com.kolodkingal.enhanced.block.asgard.BifrostMechanismBlockEntity;
import com.kolodkingal.enhanced.block.asgard.BifrostRuneStoneBlockEntity;
import com.kolodkingal.enhanced.block.asgard.GungnirStandBlockEntity;
import com.kolodkingal.enhanced.block.asgard.HofundSocketBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Enhanced.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MjolnirBlockEntity>> MJOLNIR = BLOCK_ENTITIES.register("mjolnir",
            () -> BlockEntityType.Builder.of(MjolnirBlockEntity::new, ModBlocks.MJOLNIR.get()).build(null));

    // ===== asgard (3.5) =====
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BifrostMechanismBlockEntity>> BIFROST_MECHANISM =
            BLOCK_ENTITIES.register("bifrost_mechanism", () -> BlockEntityType.Builder.of(BifrostMechanismBlockEntity::new,
                    ModBlocks.BIFROST_MECHANISM.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HofundSocketBlockEntity>> HOFUND_SOCKET =
            BLOCK_ENTITIES.register("hofund_socket", () -> BlockEntityType.Builder.of(HofundSocketBlockEntity::new,
                    ModBlocks.HOFUND_SOCKET.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GungnirStandBlockEntity>> GUNGNIR_STAND =
            BLOCK_ENTITIES.register("gungnir_stand", () -> BlockEntityType.Builder.of(GungnirStandBlockEntity::new,
                    ModBlocks.GUNGNIR_STAND.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BifrostRuneStoneBlockEntity>> BIFROST_RUNE_STONE =
            BLOCK_ENTITIES.register("bifrost_rune_stone", () -> BlockEntityType.Builder.of(BifrostRuneStoneBlockEntity::new,
                    ModBlocks.BIFROST_RUNE_STONE.get()).build(null));

    // ===== stones (3.6) =====
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.StonePedestalBlockEntity>> STONE_PEDESTAL =
            BLOCK_ENTITIES.register("stone_pedestal",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.StonePedestalBlockEntity::new,
                    ModBlocks.STONE_PEDESTAL.get()).build(null));
    // ===== stone_sites (3.6) =====
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.TesseractCradleBlockEntity>> TESSERACT_CRADLE =
            BLOCK_ENTITIES.register("tesseract_cradle",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.TesseractCradleBlockEntity::new,
                    ModBlocks.TESSERACT_CRADLE.get()).build(null));
    // ===== hulk (3.6) =====
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.GammaEmitterBlockEntity>> GAMMA_EMITTER =
            BLOCK_ENTITIES.register("gamma_emitter",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.GammaEmitterBlockEntity::new,
                    ModBlocks.GAMMA_EMITTER.get()).build(null));

    // ===== school_lab (3.7) =====
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.AdamantiumRackBlockEntity>> ADAMANTIUM_RACK =
            BLOCK_ENTITIES.register("adamantium_rack",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.AdamantiumRackBlockEntity::new,
                    ModBlocks.ADAMANTIUM_RACK.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.WeaponXPodBlockEntity>> WEAPON_X_POD =
            BLOCK_ENTITIES.register("weapon_x_pod",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.WeaponXPodBlockEntity::new,
                    ModBlocks.WEAPON_X_POD.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.CerebroConsoleBlockEntity>> CEREBRO_CONSOLE =
            BLOCK_ENTITIES.register("cerebro_console",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.CerebroConsoleBlockEntity::new,
                    ModBlocks.CEREBRO_CONSOLE.get()).build(null));

    // ===== tower (3.8) =====
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.SuitDisplayBlockEntity>> SUIT_DISPLAY =
            BLOCK_ENTITIES.register("suit_display",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.SuitDisplayBlockEntity::new,
                    ModBlocks.SUIT_DISPLAY.get()).build(null));
    /**
     * The display's upper half (3.8.1): an empty block entity, so that world generation, which gives every block of an
     * entity block a placeholder, finds one there instead of warning in the log.
     */
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.SuitDisplayBlock.Top>> SUIT_DISPLAY_TOP =
            BLOCK_ENTITIES.register("suit_display_top",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.SuitDisplayBlock.Top::new,
                    ModBlocks.SUIT_DISPLAY.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.TechCaseBlockEntity>> TECH_CASE =
            BLOCK_ENTITIES.register("tech_case",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.TechCaseBlockEntity::new,
                    ModBlocks.TECH_CASE.get()).build(null));
    // ===== eternity (3.8) =====
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.kolodkingal.enhanced.block.EternityAltarBlockEntity>> ETERNITY_ALTAR =
            BLOCK_ENTITIES.register("eternity_altar",
                    () -> BlockEntityType.Builder.of(com.kolodkingal.enhanced.block.EternityAltarBlockEntity::new,
                    ModBlocks.ETERNITY_ALTAR.get()).build(null));

    private ModBlockEntities() {
    }
}

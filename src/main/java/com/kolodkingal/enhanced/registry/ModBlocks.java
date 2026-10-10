package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    private ModBlocks() {
    }
}

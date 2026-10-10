package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
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

    private ModBlocks() {
    }
}

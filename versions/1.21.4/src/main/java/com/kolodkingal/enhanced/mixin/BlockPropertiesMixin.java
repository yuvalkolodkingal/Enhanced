package com.kolodkingal.enhanced.mixin;

import com.kolodkingal.enhanced.compat.Ids;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.4: properties made while the mod registers a block carry its id (see {@code compat.Ids}). */
@Mixin(BlockBehaviour.Properties.class)
public abstract class BlockPropertiesMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enhanced$id(CallbackInfo ci) {
        ResourceKey<Block> id = Ids.currentBlock();
        if (id != null) {
            ((BlockBehaviour.Properties) (Object) this).setId(id);
        }
    }
}

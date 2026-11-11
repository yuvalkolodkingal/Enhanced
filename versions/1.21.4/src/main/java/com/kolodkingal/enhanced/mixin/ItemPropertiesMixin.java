package com.kolodkingal.enhanced.mixin;

import com.kolodkingal.enhanced.compat.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.4: properties made while the mod registers an item carry its id (see {@code compat.Ids}). And their durability is
 * the one the mod gave: 1.21.4's armour items set the material's durability after the mod's own, and the mod's armour
 * materials have none (0, which would break the item at once), so for the mod's items the first durability given
 * stays and a 0 is ignored.
 */
@Mixin(Item.Properties.class)
public abstract class ItemPropertiesMixin {
    @Unique
    private boolean enhanced$durable;
    @Unique
    private boolean enhanced$attributes;

    /** Likewise the mod's attributes stay: 1.21.4's swords and axes set their material's after them. */
    @Inject(method = "attributes", at = @At("HEAD"), cancellable = true)
    private void enhanced$attributes(net.minecraft.world.item.component.ItemAttributeModifiers attributes,
                                     CallbackInfoReturnable<Item.Properties> cir) {
        if (Ids.currentItem() == null) {
            return;
        }
        if (enhanced$attributes) {
            cir.setReturnValue((Item.Properties) (Object) this);
            return;
        }
        enhanced$attributes = true;
    }

    @Inject(method = "durability", at = @At("HEAD"), cancellable = true)
    private void enhanced$durability(int durability, CallbackInfoReturnable<Item.Properties> cir) {
        if (Ids.currentItem() == null) {
            return;
        }
        if (durability <= 0 || enhanced$durable) {
            cir.setReturnValue((Item.Properties) (Object) this);
            return;
        }
        enhanced$durable = true;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void enhanced$id(CallbackInfo ci) {
        ResourceKey<Item> id = Ids.currentItem();
        if (id != null) {
            Item.Properties self = (Item.Properties) (Object) this;
            self.setId(id);
            // an item named like one of the mod's blocks is its block item, named as the block (as before 1.21.2)
            if (BuiltInRegistries.BLOCK.containsKey(id.location())) {
                self.useBlockDescriptionPrefix();
            }
        }
    }
}

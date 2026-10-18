package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Enhanced.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENHANCED = TABS.register("enhanced",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.enhanced"))
                    .icon(() -> ModItems.MAGNETO_HELMET.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.MAGNETO_HELMET.get());
                        output.accept(ModItems.SCARLET_WITCH_GENE.get());
                        output.accept(ModItems.LOKIS_SCEPTER.get());
                        output.accept(ModItems.TESSERACT.get());
                        output.accept(ModItems.WANDA_CROWN.get());
                        output.accept(ModItems.QUICKSILVER_GOGGLES.get());
                        output.accept(ModItems.SYNTHEZOID_CORE.get());
                        output.accept(ModItems.MJOLNIR.get());
                        output.accept(ModItems.STORMBREAKER.get());
                        output.accept(ModItems.BOOK_OF_THE_VISHANTI.get());
                        output.accept(ModItems.DARKHOLD.get());
                        output.accept(ModItems.WITCHES_ROAD_DOOR.get());
                        output.accept(ModItems.PHOENIX_EMBER.get());
                        output.accept(ModItems.RADIOACTIVE_SPIDER.get());
                        output.accept(ModItems.INFINITY_GAUNTLET.get());
                        output.accept(ModItems.ULTRON_PROGRAM.get());
                        output.accept(ModItems.ULTRON_CORE.get());
                        output.accept(ModItems.CAPSTONE.get());
                        output.accept(ModItems.ESSENCE.get());
                        // 3.3
                        output.accept(ModItems.RUBY_QUARTZ_GLASSES.get());
                    })
                    .build());

    private ModCreativeTab() {
    }
}

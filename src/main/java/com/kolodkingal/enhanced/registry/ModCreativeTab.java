package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.reg.RegEntry;
import com.kolodkingal.enhanced.platform.reg.Registrar;
import com.kolodkingal.enhanced.platform.reg.Registrars;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public final class ModCreativeTab {
    public static final Registrar<CreativeModeTab> TABS = Registrars.create(Registries.CREATIVE_MODE_TAB, Enhanced.MODID);

    public static final RegEntry<CreativeModeTab, CreativeModeTab> ENHANCED = TABS.register("enhanced",
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
                        output.accept(ModItems.HERO_CREATOR.get());
                        output.accept(ModItems.XAVIER_WHEELCHAIR.get());
                        // 3.3.1
                        output.accept(ModItems.MUTANT_CURE.get());
                        // 3.5
                        output.accept(ModItems.HOFUND.get());
                        output.accept(ModItems.GUNGNIR.get());
                        output.accept(ModItems.HELA_HEADDRESS.get());
                        ModItems.ASGARD_BLOCK_ITEMS.forEach(item -> output.accept(item.get()));
                        // 3.6
                        output.accept(ModItems.SPACE_STONE.get());
                        output.accept(ModItems.MIND_STONE.get());
                        output.accept(ModItems.REALITY_STONE.get());
                        output.accept(ModItems.POWER_STONE.get());
                        output.accept(ModItems.TIME_STONE.get());
                        output.accept(ModItems.SOUL_STONE.get());
                        output.accept(ModItems.THE_ORB.get());
                        output.accept(ModItems.EMPTY_SCEPTER.get());
                        output.accept(ModItems.EMPTY_TESSERACT.get());
                        output.accept(ModItems.STONE_PEDESTAL.get());
                        output.accept(ModItems.EYE_OF_AGAMOTTO.get());
                        output.accept(ModItems.SLING_RING.get());
                        output.accept(ModItems.TESSERACT_CRADLE.get());
                        output.accept(ModItems.GAMMA_EMITTER.get());
                        output.accept(ModItems.GAMMA_INHIBITOR.get());
                        output.accept(ModItems.HYDRA_ENERGY_CELL.get());
                        output.accept(ModItems.HYDRA_BLASTER.get());
                        output.accept(ModItems.HYDRA_SOLDIER_SPAWN_EGG.get());
                        output.accept(ModItems.HYDRA_OFFICER_SPAWN_EGG.get());
                        // 3.7
                        output.accept(ModItems.ADAMANTIUM_INJECTION.get());
                        output.accept(ModItems.CHIMICHANGA.get());
                        output.accept(ModItems.STONE_LOCATOR.get());
                        output.accept(ModItems.SECRET_BOOKSHELF.get());
                        output.accept(ModItems.ELEVATOR_PAD.get());
                        output.accept(ModItems.X_DOOR.get());
                        output.accept(ModItems.X_DOOR_ROUND.get());
                        output.accept(ModItems.ADAMANTIUM_RACK.get());
                        output.accept(ModItems.WEAPON_X_POD.get());
                        output.accept(ModItems.CURE_STATION.get());
                        output.accept(ModItems.CEREBRO_CONSOLE.get());
                        // 3.8
                        output.accept(ModItems.SPECIAL_SOLDIER_SERUM.get());
                        output.accept(ModItems.VIBRANIUM_SHIELD.get());
                        output.accept(ModItems.ARC_REACTOR.get());
                        output.accept(ModItems.NANO_ARC_REACTOR.get());
                        output.accept(ModItems.IRON_MAN_MARK_1.get());
                        output.accept(ModItems.IRON_MAN_MARK_3.get());
                        output.accept(ModItems.IRON_MAN_MARK_5.get());
                        output.accept(ModItems.IRON_MAN_MARK_50.get());
                        output.accept(ModItems.IRON_MAN_MARK_85.get());
                        output.accept(ModItems.IRON_MAN_HULKBUSTER.get());
                        output.accept(ModItems.FALCON_EXO_WINGS.get());
                        output.accept(ModItems.SUIT_DISPLAY.get());
                        output.accept(ModItems.TECH_CASE.get());
                        output.accept(ModItems.ETERNITY_ALTAR.get());
                        output.accept(ModItems.ETERNITY_MIRROR.get());
                    })
                    .build());

    private ModCreativeTab() {
    }
}

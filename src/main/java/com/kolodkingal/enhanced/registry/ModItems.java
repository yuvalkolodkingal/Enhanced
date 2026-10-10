package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.item.LokisScepterItem;
import com.kolodkingal.enhanced.item.MagnetoHelmetItem;
import com.kolodkingal.enhanced.item.MjolnirItem;
import com.kolodkingal.enhanced.item.QuicksilverGogglesItem;
import com.kolodkingal.enhanced.item.ScarletWitchGeneItem;
import com.kolodkingal.enhanced.item.SynthezoidCoreItem;
import com.kolodkingal.enhanced.item.TesseractItem;
import com.kolodkingal.enhanced.item.WandaCrownItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Enhanced.MODID);

    /** No durability component, so it never takes damage. */
    public static final DeferredItem<MagnetoHelmetItem> MAGNETO_HELMET = ITEMS.register("magneto_helmet",
            () -> new MagnetoHelmetItem(ModArmorMaterials.MAGNETO, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final DeferredItem<WandaCrownItem> WANDA_CROWN = ITEMS.register("wanda_crown",
            () -> new WandaCrownItem(ModArmorMaterials.WANDA_CROWN, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final DeferredItem<QuicksilverGogglesItem> QUICKSILVER_GOGGLES = ITEMS.register("quicksilver_goggles",
            () -> new QuicksilverGogglesItem(ModArmorMaterials.QUICKSILVER, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<SynthezoidCoreItem> SYNTHEZOID_CORE = ITEMS.register("synthezoid_core",
            () -> new SynthezoidCoreItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final DeferredItem<ScarletWitchGeneItem> SCARLET_WITCH_GENE = ITEMS.register("scarlet_witch_gene",
            () -> new ScarletWitchGeneItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<LokisScepterItem> LOKIS_SCEPTER = ITEMS.register("lokis_scepter",
            () -> new LokisScepterItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                    .attributes(LokisScepterItem.createAttributes())));

    /** Only the worthy can lift it: see {@link com.kolodkingal.enhanced.ability.Worthiness}. */
    public static final DeferredItem<MjolnirItem> MJOLNIR = ITEMS.register("mjolnir",
            () -> new MjolnirItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                    .attributes(MjolnirItem.createAttributes())));
    /** The Space Stone in its cube. Using it is how Loki escapes in 2012, so it makes you Loki. */
    public static final DeferredItem<TesseractItem> TESSERACT = ITEMS.register("tesseract",
            () -> new TesseractItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> WITCHES_ROAD_DOOR = ITEMS.registerSimpleBlockItem(ModBlocks.WITCHES_ROAD_DOOR,
            new Item.Properties().rarity(Rarity.RARE));

    /** Projectile sprites only; not in the creative tab. */
    public static final DeferredItem<Item> IRON_SHRAPNEL = ITEMS.registerSimpleItem("iron_shrapnel");
    public static final DeferredItem<Item> PHOTON_ORB = ITEMS.registerSimpleItem("photon_orb");
    public static final DeferredItem<Item> LOKI_DAGGER = ITEMS.registerSimpleItem("loki_dagger");

    private ModItems() {
    }
}

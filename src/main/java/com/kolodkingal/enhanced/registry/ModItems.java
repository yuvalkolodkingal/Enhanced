package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.item.CapstoneItem;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.item.HelaHeaddressItem;
import com.kolodkingal.enhanced.item.HofundItem;
import com.kolodkingal.enhanced.item.DarkholdItem;
import com.kolodkingal.enhanced.item.EssenceItem;
import com.kolodkingal.enhanced.item.HeroCreatorItem;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.item.LokisScepterItem;
import com.kolodkingal.enhanced.item.MagnetoHelmetItem;
import com.kolodkingal.enhanced.item.MjolnirItem;
import com.kolodkingal.enhanced.item.MutantCureItem;
import com.kolodkingal.enhanced.item.PhoenixEmberItem;
import com.kolodkingal.enhanced.item.QuicksilverGogglesItem;
import com.kolodkingal.enhanced.item.RadioactiveSpiderItem;
import com.kolodkingal.enhanced.item.RubyQuartzGlassesItem;
import com.kolodkingal.enhanced.item.ScarletWitchGeneItem;
import com.kolodkingal.enhanced.item.StormbreakerItem;
import com.kolodkingal.enhanced.item.SynthezoidCoreItem;
import com.kolodkingal.enhanced.item.TesseractItem;
import com.kolodkingal.enhanced.item.UltronCoreItem;
import com.kolodkingal.enhanced.item.UltronProgramItem;
import com.kolodkingal.enhanced.item.VishantiBookItem;
import com.kolodkingal.enhanced.item.WandaCrownItem;
import com.kolodkingal.enhanced.item.XavierWheelchairItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Unbreakable;
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

    /** Only its owner can lift it: see {@link com.kolodkingal.enhanced.ability.Worthiness}. */
    public static final DeferredItem<MjolnirItem> MJOLNIR = ITEMS.register("mjolnir",
            () -> new MjolnirItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                    .attributes(MjolnirItem.createAttributes())));

    /** Anyone can wield it. Never wears out: the Unbreakable component (hidden; the tooltip says it in its own words). */
    public static final DeferredItem<StormbreakerItem> STORMBREAKER = ITEMS.register("stormbreaker",
            () -> new StormbreakerItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                    .attributes(StormbreakerItem.createAttributes())));
    /** The Space Stone in its cube. Using it is how Loki escapes in 2012, so it makes you Loki. */
    public static final DeferredItem<TesseractItem> TESSERACT = ITEMS.register("tesseract",
            () -> new TesseractItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> WITCHES_ROAD_DOOR = ITEMS.registerSimpleBlockItem(ModBlocks.WITCHES_ROAD_DOOR,
            new Item.Properties().rarity(Rarity.RARE));

    /** The old tome of the sorcerers (3.2): only Loki, the Scarlet Witch or Billy can read it. Craftable. */
    public static final DeferredItem<VishantiBookItem> BOOK_OF_THE_VISHANTI = ITEMS.register("book_of_the_vishanti",
            () -> new VishantiBookItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    /** Agatha's book (3.2): a purple beam, a retaliation against magic, a hex mark and a mind glimpse for a sorcerer who holds it. Craftable, and costly. */
    public static final DeferredItem<DarkholdItem> DARKHOLD = ITEMS.register("darkhold",
            () -> new DarkholdItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    // The items of 3.0, in one region per hero (the // ===== lines). All stack to one, are fire-resistant and epic,
    // except the spider, which stacks to sixteen.

    // ===== phoenix =====
    /** Found in chests; hold right-click for three seconds with no power to become the Phoenix. */
    public static final DeferredItem<PhoenixEmberItem> PHOENIX_EMBER = ITEMS.register("phoenix_ember",
            () -> new PhoenixEmberItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    // ===== spider =====
    public static final DeferredItem<RadioactiveSpiderItem> RADIOACTIVE_SPIDER = ITEMS.register("radioactive_spider",
            () -> new RadioactiveSpiderItem(new Item.Properties().stacksTo(16).fireResistant().rarity(Rarity.EPIC)));

    // ===== thanos =====
    /** The reward of the Thanos fight; hold right-click for three seconds with no power to become Thanos. */
    public static final DeferredItem<InfinityGauntletItem> INFINITY_GAUNTLET = ITEMS.register("infinity_gauntlet",
            () -> new InfinityGauntletItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    // ===== ultron =====
    /** Starts the Ultron fight, with Loki's Scepter (see ScepterRituals). */
    public static final DeferredItem<UltronProgramItem> ULTRON_PROGRAM = ITEMS.register("ultron_program",
            () -> new UltronProgramItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    /** The reward of the Ultron fight; with Loki's Scepter it makes a player without powers Ultron. */
    public static final DeferredItem<UltronCoreItem> ULTRON_CORE = ITEMS.register("ultron_core",
            () -> new UltronCoreItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    // ===== apocalypse =====
    /** Planted on sand by day, it wakes the Apocalypse fight. */
    public static final DeferredItem<CapstoneItem> CAPSTONE = ITEMS.register("capstone",
            () -> new CapstoneItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    /** The reward of the Apocalypse fight; hold right-click for three seconds with no power to become Apocalypse. */
    public static final DeferredItem<EssenceItem> ESSENCE = ITEMS.register("essence",
            () -> new EssenceItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    // ===== cyclops (3.3) =====
    /**
     * Found in the Mutant School (its chest table names {@code enhanced:inject/ruby_quartz_glasses}), and rarely in a woodland
     * mansion or a stronghold library; put on with no power, they make Cyclops. No durability component: they never break.
     */
    public static final DeferredItem<RubyQuartzGlassesItem> RUBY_QUARTZ_GLASSES = ITEMS.register("ruby_quartz_glasses",
            () -> new RubyQuartzGlassesItem(ModArmorMaterials.RUBY_QUARTZ, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    // ===== professor_x (3.3) =====
    /**
     * Crafted (or found in the Mutant School); hold right-click for three seconds with no power to become Professor X, who
     * then chooses his version.
     */
    public static final DeferredItem<XavierWheelchairItem> XAVIER_WHEELCHAIR = ITEMS.register("xavier_wheelchair",
            () -> new XavierWheelchairItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    // ===== the Mutant Cure (3.3.1) =====
    /** Crafted; hold right-click for two seconds to take your power away (or another player's: an operator, or with PvP on). */
    public static final DeferredItem<MutantCureItem> MUTANT_CURE = ITEMS.register("mutant_cure",
            () -> new MutantCureItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    // ===== asgard (3.5) =====
    // (The skeleton registers the three items other lanes need by name; lane L1 adds the realm's block items here.)
    // ===== heimdall (3.5) =====
    /** Heimdall's sword, the key of the Bifrost for whoever holds it (lane L2). */
    public static final DeferredItem<HofundItem> HOFUND = ITEMS.register("hofund",
            () -> new HofundItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                    .attributes(HofundItem.createAttributes())));
    // ===== odin (3.5) =====
    /** The spear of the Allfather; its owner opens the Bifrost with it (lane L3). */
    public static final DeferredItem<GungnirItem> GUNGNIR = ITEMS.register("gungnir",
            () -> new GungnirItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                    .attributes(GungnirItem.createAttributes())));
    // ===== hela (3.5) =====
    /** The reward of the fight against Hela; held with no power, it makes Hela (lane L4). */
    public static final DeferredItem<HelaHeaddressItem> HELA_HEADDRESS = ITEMS.register("hela_headdress",
            () -> new HelaHeaddressItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    // ===== asgardians (3.5) =====

    /** Projectile sprites only; not in the creative tab. */
    /** 3.3: the Hero Creator, a Creative-only item (the Creative tab, no recipe, no loot). */
    public static final DeferredItem<HeroCreatorItem> HERO_CREATOR = ITEMS.register("hero_creator",
            () -> new HeroCreatorItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> IRON_SHRAPNEL = ITEMS.registerSimpleItem("iron_shrapnel");
    public static final DeferredItem<Item> LOKI_DAGGER = ITEMS.registerSimpleItem("loki_dagger");

    private ModItems() {
    }
}

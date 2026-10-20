package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.item.CapstoneItem;
import com.kolodkingal.enhanced.item.EmptyScepterItem;
import com.kolodkingal.enhanced.item.EmptyTesseractItem;
import com.kolodkingal.enhanced.item.EyeOfAgamottoItem;
import com.kolodkingal.enhanced.item.GammaInhibitorItem;
import com.kolodkingal.enhanced.item.InfinityStoneItem;
import com.kolodkingal.enhanced.item.SlingRingItem;
import com.kolodkingal.enhanced.item.TheOrbItem;
import com.kolodkingal.enhanced.hydra.HydraBlasterItem;
import com.kolodkingal.enhanced.hydra.HydraEnergyCellItem;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.thanos.StoneKind;
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
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

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
    /**
     * The reward of the Thanos fight; hold right-click for three seconds with no power to become Thanos. 3.6: its
     * default {@code enhanced:gauntlet} is the Titan's Gauntlet (every old one, and the boss's reward, is it); a
     * crafted one is empty.
     */
    public static final DeferredItem<InfinityGauntletItem> INFINITY_GAUNTLET = ITEMS.register("infinity_gauntlet",
            () -> new InfinityGauntletItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                    .component(ModDataComponents.GAUNTLET.get(), GauntletContents.TITAN)));

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
    /** The block items of the realm's decorative blocks (Creative only: the city's own, no recipes), in tab order. */
    public static final List<DeferredItem<BlockItem>> ASGARD_BLOCK_ITEMS = List.of(
            ITEMS.registerSimpleBlockItem(ModBlocks.ASGARD_GOLD_PLATING),
            ITEMS.registerSimpleBlockItem(ModBlocks.ASGARD_GOLD_TRIM),
            ITEMS.registerSimpleBlockItem(ModBlocks.ASGARD_GOLD_PILLAR),
            ITEMS.registerSimpleBlockItem(ModBlocks.ASGARD_GOLD_LATTICE),
            ITEMS.registerSimpleBlockItem(ModBlocks.REALM_STONE),
            ITEMS.registerSimpleBlockItem(ModBlocks.REALM_STONE_BRICKS),
            ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_REALM_STONE),
            ITEMS.registerSimpleBlockItem(ModBlocks.REALM_STONE_PILLAR),
            ITEMS.registerSimpleBlockItem(ModBlocks.REALM_STONE_BRICK_STAIRS),
            ITEMS.registerSimpleBlockItem(ModBlocks.REALM_STONE_BRICK_SLAB),
            ITEMS.registerSimpleBlockItem(ModBlocks.REALM_STONE_BRICK_WALL),
            ITEMS.registerSimpleBlockItem(ModBlocks.BIFROST_BRIDGE),
            ITEMS.registerSimpleBlockItem(ModBlocks.COSMIC_FALLS),
            ITEMS.registerSimpleBlockItem(ModBlocks.ASGARD_MURAL),
            ITEMS.registerSimpleBlockItem(ModBlocks.ASGARD_BRAZIER),
            ITEMS.registerSimpleBlockItem(ModBlocks.ASGARD_GOLDEN_LEAVES),
            ITEMS.registerSimpleBlockItem(ModBlocks.ASGARD_BENCH));
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

    // ===== stones (3.6) =====
    // (The skeleton registers every item of 3.6; each lane owns its classes.)
    /** The six Infinity Stones (lane L1 owns them; lane L2 their powers). Stack to one, epic, fire-proof. */
    public static final DeferredItem<InfinityStoneItem> SPACE_STONE = stone("space_stone", StoneKind.SPACE);
    public static final DeferredItem<InfinityStoneItem> MIND_STONE = stone("mind_stone", StoneKind.MIND);
    public static final DeferredItem<InfinityStoneItem> REALITY_STONE = stone("reality_stone", StoneKind.REALITY);
    public static final DeferredItem<InfinityStoneItem> POWER_STONE = stone("power_stone", StoneKind.POWER);
    public static final DeferredItem<InfinityStoneItem> TIME_STONE = stone("time_stone", StoneKind.TIME);
    public static final DeferredItem<InfinityStoneItem> SOUL_STONE = stone("soul_stone", StoneKind.SOUL);
    /** The Orb, the Power Stone's casing from the End tunnel (lane L1). */
    public static final DeferredItem<TheOrbItem> THE_ORB = ITEMS.register("the_orb",
            () -> new TheOrbItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    /** Loki's Scepter without the Mind Stone: a plain weapon (lane L1). */
    public static final DeferredItem<EmptyScepterItem> EMPTY_SCEPTER = ITEMS.register("empty_scepter",
            () -> new EmptyScepterItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .attributes(com.kolodkingal.enhanced.item.LokisScepterItem.createAttributes())));
    /** The Tesseract without the Space Stone: decoration (lane L1). */
    public static final DeferredItem<EmptyTesseractItem> EMPTY_TESSERACT = ITEMS.register("empty_tesseract",
            () -> new EmptyTesseractItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    /** The stone pedestal of a site (Creative pick: an empty one; lane L1). */
    public static final DeferredItem<BlockItem> STONE_PEDESTAL =
            ITEMS.registerSimpleBlockItem(ModBlocks.STONE_PEDESTAL);
    // ===== strange (3.6) =====
    /** The Eye of Agamotto, the Time Stone's vessel; Doctor Strange's awakening (lane L3). */
    public static final DeferredItem<EyeOfAgamottoItem> EYE_OF_AGAMOTTO = ITEMS.register("eye_of_agamotto",
            () -> new EyeOfAgamottoItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    /** A sorcerer's Sling Ring (lane L3). */
    public static final DeferredItem<SlingRingItem> SLING_RING = ITEMS.register("sling_ring",
            () -> new SlingRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    // ===== stone_sites (3.6) =====
    /** The Tesseract's cradle on Avengers Tower (lane L4). */
    public static final DeferredItem<BlockItem> TESSERACT_CRADLE =
            ITEMS.registerSimpleBlockItem(ModBlocks.TESSERACT_CRADLE);
    // ===== vormir (3.6) =====
    // ===== hulk (3.6) =====
    /** The Gamma Emitter (lane L7). */
    public static final DeferredItem<BlockItem> GAMMA_EMITTER = ITEMS.registerSimpleBlockItem(ModBlocks.GAMMA_EMITTER);
    /** Bruce Banner's Gamma Inhibitor (lane L7, P2). */
    public static final DeferredItem<GammaInhibitorItem> GAMMA_INHIBITOR = ITEMS.register("gamma_inhibitor",
            () -> new GammaInhibitorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    // ===== hydra (3.6) =====
    /** A Hydra energy cell (lane L8). */
    public static final DeferredItem<HydraEnergyCellItem> HYDRA_ENERGY_CELL = ITEMS.register("hydra_energy_cell",
            () -> new HydraEnergyCellItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** The Hydra blaster (lane L8): 96 uses. */
    public static final DeferredItem<HydraBlasterItem> HYDRA_BLASTER = ITEMS.register("hydra_blaster",
            () -> new HydraBlasterItem(new Item.Properties().stacksTo(1).durability(96).rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> HYDRA_SOLDIER_SPAWN_EGG = ITEMS.register(
            "hydra_soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.HYDRA_SOLDIER, 0x1C1C1E, 0xB3161C, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HYDRA_OFFICER_SPAWN_EGG = ITEMS.register(
            "hydra_officer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.HYDRA_OFFICER, 0x2A2A2E, 0xD8B23A, new Item.Properties()));

    // ===== wolverine (3.7) =====
    // (The skeleton registers every item of 3.7; each lane owns its classes.)
    /** The Adamantium Injection: Wolverine's awakening (lane L2). From the lab's rack; not crafted by default. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.AdamantiumInjectionItem> ADAMANTIUM_INJECTION =
            ITEMS.register("adamantium_injection", () -> new com.kolodkingal.enhanced.item.AdamantiumInjectionItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    // ===== deadpool (3.7) =====
    /** A chimichanga: food for anyone, Deadpool's favourite (lane L3). */
    public static final DeferredItem<com.kolodkingal.enhanced.item.ChimichangaItem> CHIMICHANGA =
            ITEMS.register("chimichanga", () -> new com.kolodkingal.enhanced.item.ChimichangaItem(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(8).saturationModifier(0.8F)
                            .build())));
    // ===== school_lab (3.7) =====
    /** The lab's blocks, for builders (Creative; lane L4). */
    public static final DeferredItem<BlockItem> SECRET_BOOKSHELF =
            ITEMS.registerSimpleBlockItem(ModBlocks.SECRET_BOOKSHELF);
    public static final DeferredItem<BlockItem> ELEVATOR_PAD = ITEMS.registerSimpleBlockItem(ModBlocks.ELEVATOR_PAD);
    public static final DeferredItem<BlockItem> X_DOOR = ITEMS.registerSimpleBlockItem(ModBlocks.X_DOOR);
    public static final DeferredItem<BlockItem> X_DOOR_ROUND = ITEMS.registerSimpleBlockItem(ModBlocks.X_DOOR_ROUND);
    public static final DeferredItem<BlockItem> ADAMANTIUM_RACK =
            ITEMS.registerSimpleBlockItem(ModBlocks.ADAMANTIUM_RACK);
    public static final DeferredItem<BlockItem> WEAPON_X_POD = ITEMS.registerSimpleBlockItem(ModBlocks.WEAPON_X_POD);
    public static final DeferredItem<BlockItem> CURE_STATION = ITEMS.registerSimpleBlockItem(ModBlocks.CURE_STATION);
    public static final DeferredItem<BlockItem> CEREBRO_CONSOLE =
            ITEMS.registerSimpleBlockItem(ModBlocks.CEREBRO_CONSOLE);
    // ===== finders (3.7) =====
    /** The Stone Locator (lane L5). */
    public static final DeferredItem<com.kolodkingal.enhanced.item.StoneLocatorItem> STONE_LOCATOR =
            ITEMS.register("stone_locator", () -> new com.kolodkingal.enhanced.item.StoneLocatorItem(
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                            .component(ModDataComponents.LOCATOR.get(),
                                    com.kolodkingal.enhanced.finders.LocatorState.EMPTY)));
    // ===== cottage (3.7) =====

    // ===== serum (3.8) =====
    // (The skeleton registers every item of 3.8; each lane owns its classes. Plain looks until lane L7's icons.)
    /** The Super Soldier Serum (lane L1): the tower lab's "Project Rebirth" case; not crafted by default. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.SuperSoldierSerumItem> SPECIAL_SOLDIER_SERUM =
            ITEMS.register("special_soldier_serum", () -> new com.kolodkingal.enhanced.item.SuperSoldierSerumItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    // ===== captain (3.8) =====
    /** The Vibranium Shield (lane L1): a shield that never wears out (the Unbreakable component). */
    public static final DeferredItem<com.kolodkingal.enhanced.item.VibraniumShieldItem> VIBRANIUM_SHIELD =
            ITEMS.register("vibranium_shield", () -> new com.kolodkingal.enhanced.item.VibraniumShieldItem(
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(false))));
    // ===== iron_man (3.8) =====
    /** The Arc Reactor (lane L3): Tony's desk in the tower; it makes you Iron Man and powers the classic suits. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.ArcReactorItem> ARC_REACTOR =
            ITEMS.register("arc_reactor", () -> new com.kolodkingal.enhanced.item.ArcReactorItem(
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE)));
    /** The Nano Arc Reactor (lane L3): a chest item, the nanotech suits' home. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.NanoArcReactorItem> NANO_ARC_REACTOR =
            ITEMS.register("nano_arc_reactor", () -> new com.kolodkingal.enhanced.item.NanoArcReactorItem(
                    ModArmorMaterials.NANO_REACTOR, new Item.Properties().stacksTo(1).fireResistant()
                            .rarity(Rarity.EPIC)));
    /** The Mark 1 (lane L3): the cave suit, worn on the chest. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.IronSuitItem> IRON_MAN_MARK_1 =
            suit("iron_man_mark_1", com.kolodkingal.enhanced.ironman.SuitKind.MARK_1, ModArmorMaterials.IRON_MARK_1,
                    825, Rarity.RARE);
    /** The Mark 3 (lane L3): red and gold, worn on the chest. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.IronSuitItem> IRON_MAN_MARK_3 =
            suit("iron_man_mark_3", com.kolodkingal.enhanced.ironman.SuitKind.MARK_3, ModArmorMaterials.IRON_MARK_3,
                    1100, Rarity.EPIC);
    /** The Mark 5 (lane L3): the suitcase suit; folded in the hand ({@code enhanced:suit}), unfolded on the chest. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.IronSuitItem> IRON_MAN_MARK_5 =
            ITEMS.register("iron_man_mark_5", () -> new com.kolodkingal.enhanced.item.IronSuitItem(
                    com.kolodkingal.enhanced.ironman.SuitKind.MARK_5, ModArmorMaterials.IRON_MARK_5,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC).durability(600)
                            .component(ModDataComponents.SUIT.get(),
                                    com.kolodkingal.enhanced.ironman.SuitItemState.FOLDED)));
    // ===== nanotech (3.8) =====
    /** The Mark 50's nanotech housing (lanes L3 and L4): carried; the Nano Arc Reactor deploys it. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.IronSuitItem> IRON_MAN_MARK_50 =
            suit("iron_man_mark_50", com.kolodkingal.enhanced.ironman.SuitKind.MARK_50, ModArmorMaterials.NANO_REACTOR,
                    1815, Rarity.EPIC);
    /** The Mark 85's nanotech housing (lanes L3 and L4). */
    public static final DeferredItem<com.kolodkingal.enhanced.item.IronSuitItem> IRON_MAN_MARK_85 =
            suit("iron_man_mark_85", com.kolodkingal.enhanced.ironman.SuitKind.MARK_85, ModArmorMaterials.NANO_REACTOR,
                    2035, Rarity.EPIC);
    /** The Hulkbuster (lane L4): used, Veronica drops its frame; or worn on the chest. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.IronSuitItem> IRON_MAN_HULKBUSTER =
            suit("iron_man_hulkbuster", com.kolodkingal.enhanced.ironman.SuitKind.HULKBUSTER,
                    ModArmorMaterials.HULKBUSTER, 3000, Rarity.EPIC);
    // ===== falcon (3.8) =====
    /** Falcon's Exo-Wings (lane L5): the top of the tower; not crafted. */
    public static final DeferredItem<com.kolodkingal.enhanced.item.FalconWingsItem> FALCON_EXO_WINGS =
            ITEMS.register("falcon_exo_wings", () -> new com.kolodkingal.enhanced.item.FalconWingsItem(
                    ModArmorMaterials.FALCON_WINGS, new Item.Properties().stacksTo(1).fireResistant()
                            .rarity(Rarity.EPIC).durability(900)));
    // ===== tower (3.8) =====
    /** The tower's suit display and tech case, for builders (Creative; lane L6). */
    public static final DeferredItem<net.minecraft.world.item.DoubleHighBlockItem> SUIT_DISPLAY =
            ITEMS.register("suit_display", () -> new net.minecraft.world.item.DoubleHighBlockItem(
                    ModBlocks.SUIT_DISPLAY.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> TECH_CASE = ITEMS.registerSimpleBlockItem(ModBlocks.TECH_CASE,
            new Item.Properties().rarity(Rarity.RARE));
    // ===== eternity (3.8) =====
    /** Eternity's altar and the realm's mirror, for builders (Creative only; lane L8). */
    public static final DeferredItem<BlockItem> ETERNITY_ALTAR = ITEMS.registerSimpleBlockItem(ModBlocks.ETERNITY_ALTAR,
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> ETERNITY_MIRROR =
            ITEMS.registerSimpleBlockItem(ModBlocks.ETERNITY_MIRROR, new Item.Properties().rarity(Rarity.EPIC));
    // ===== love (3.8) =====

    /** 3.8: a suit of Iron Man's as a chest item, with its durability (the plan's numbers). */
    private static DeferredItem<com.kolodkingal.enhanced.item.IronSuitItem> suit(String name,
            com.kolodkingal.enhanced.ironman.SuitKind kind,
                    net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial>
                    material, int durability, Rarity rarity) {
        return ITEMS.register(name, () -> new com.kolodkingal.enhanced.item.IronSuitItem(kind, material,
                new Item.Properties().stacksTo(1).fireResistant().rarity(rarity).durability(durability)));
    }

    /** 3.6: an Infinity Stone's item. */
    private static DeferredItem<InfinityStoneItem> stone(String name, StoneKind kind) {
        return ITEMS.register(name, () -> new InfinityStoneItem(kind, new Item.Properties().stacksTo(1).fireResistant()
                .rarity(Rarity.EPIC)));
    }

    /** 3.6: the item of a stone. */
    public static Item stone(StoneKind kind) {
        return switch (kind) {
            case SPACE -> SPACE_STONE.get();
            case MIND -> MIND_STONE.get();
            case REALITY -> REALITY_STONE.get();
            case POWER -> POWER_STONE.get();
            case TIME -> TIME_STONE.get();
            case SOUL -> SOUL_STONE.get();
        };
    }

    /** Projectile sprites only; not in the creative tab. */
    /** 3.3: the Hero Creator, a Creative-only item (the Creative tab, no recipe, no loot). */
    public static final DeferredItem<HeroCreatorItem> HERO_CREATOR = ITEMS.register("hero_creator",
            () -> new HeroCreatorItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> IRON_SHRAPNEL = ITEMS.registerSimpleItem("iron_shrapnel");
    public static final DeferredItem<Item> LOKI_DAGGER = ITEMS.registerSimpleItem("loki_dagger");

    private ModItems() {
    }
}

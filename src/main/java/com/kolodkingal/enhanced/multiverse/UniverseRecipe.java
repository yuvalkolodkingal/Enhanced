package com.kolodkingal.enhanced.multiverse;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a random universe is made of (3.19.1), as the game's April Fools update 20w14&infin; made a dimension of the words
 * in a book: the universe's number is the seed, and the seed picks everything, always the same for the same number:
 * <ul>
 *     <li>the <b>shape</b> of the land ({@link #shape()}): the Overworld's, amplified mountains, floating islands, one
 *     great cave, a hollow world under a roof (the Nether's shape), islands over the void (the End's) or huge biomes
 *     (each shape is one dimension of {@link Multiverse#SHAPES});</li>
 *     <li>the <b>stone</b> the land is made of, the <b>ground</b> on top of it and what fills the <b>seas</b>
 *     ({@link #stone()}, {@link #ground()}, {@link #sea()}; null keeps the game's own), swapped when the land is made
 *     ({@link UniverseChunkGenerator});</li>
 *     <li>the <b>biomes</b> ({@link #biome}): the game's own, all one biome, or every biome turned into another;</li>
 *     <li>the colour of the <b>sky and fog</b> ({@link #fog()}, -1: the game's own), drawn by the client;</li>
 *     <li>the <b>ores</b> ({@link #ore()}): every ore may be another block;</li>
 *     <li>the <b>creatures</b> ({@link #creature}): none change, about half of the kinds come as others, all of them
 *     do, or every creature is one kind; and they may all be giants or tiny ({@link #scale()}) ({@code MultiverseEvents});</li>
 *     <li>the <b>gravity</b> ({@link #gravity()}): light as the moon, or heavy, for the players and every creature.</li>
 * </ul>
 * Everything here is arithmetic on the number, so the client works out the same colours as the server. 3.19.1, second
 * round: the number is mixed first (neighbouring numbers no longer give alike universes), and each part is drawn on its
 * own, so names give clearly different worlds.
 */
public final class UniverseRecipe {
    /** What the land may be made of instead of stone. */
    private static final List<Block> STONES = List.of(Blocks.TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.ORANGE_TERRACOTTA,
            Blocks.WHITE_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPUR_BLOCK, Blocks.END_STONE, Blocks.NETHERRACK,
            Blocks.BLACKSTONE, Blocks.QUARTZ_BLOCK, Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.SANDSTONE,
            Blocks.RED_SANDSTONE, Blocks.PACKED_MUD, Blocks.MOSS_BLOCK, Blocks.CALCITE, Blocks.TUFF, Blocks.AMETHYST_BLOCK,
            Blocks.OBSIDIAN, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.CLAY, Blocks.BONE_BLOCK, Blocks.HONEYCOMB_BLOCK,
            Blocks.MAGENTA_WOOL, Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.PURPLE_CONCRETE, Blocks.SMOOTH_BASALT, Blocks.DRIPSTONE_BLOCK, Blocks.SCULK, Blocks.GLOWSTONE,
            Blocks.MUD_BRICKS, Blocks.BRICKS, Blocks.DEEPSLATE_TILES, Blocks.WARPED_WART_BLOCK, Blocks.NETHER_WART_BLOCK,
            Blocks.SHROOMLIGHT, Blocks.OCHRE_FROGLIGHT, Blocks.SPONGE);
    /** What may lie on top instead of grass. */
    private static final List<Block> GROUNDS = List.of(Blocks.MYCELIUM, Blocks.PODZOL, Blocks.MOSS_BLOCK,
            Blocks.CRIMSON_NYLIUM, Blocks.WARPED_NYLIUM, Blocks.SNOW_BLOCK, Blocks.SAND, Blocks.RED_SAND, Blocks.COARSE_DIRT,
            Blocks.WHITE_WOOL, Blocks.LIME_WOOL, Blocks.PINK_WOOL, Blocks.SOUL_SAND, Blocks.GRAVEL, Blocks.MUD,
            Blocks.ROOTED_DIRT, Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE_POWDER);
    /** What may fill the seas instead of water. */
    private static final List<Block> SEAS = List.of(Blocks.LAVA, Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE,
            Blocks.SLIME_BLOCK, Blocks.HONEY_BLOCK, Blocks.MAGMA_BLOCK, Blocks.LIGHT_BLUE_STAINED_GLASS,
            Blocks.PURPLE_STAINED_GLASS, Blocks.LAVA);
    /** The kinds of creature that may come as another kind, and the kinds they may come as. */
    private static final List<EntityType<?>> CREATURES = List.of(EntityType.COW, EntityType.PIG, EntityType.SHEEP,
            EntityType.CHICKEN, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
            EntityType.ENDERMAN, EntityType.SLIME, EntityType.WITCH, EntityType.BLAZE, EntityType.MAGMA_CUBE,
            EntityType.GOAT, EntityType.FOX, EntityType.WOLF, EntityType.RABBIT, EntityType.PANDA, EntityType.POLAR_BEAR,
            EntityType.LLAMA, EntityType.FROG, EntityType.PARROT, EntityType.BEE, EntityType.STRAY, EntityType.HUSK,
            EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.MOOSHROOM, EntityType.CAMEL, EntityType.SNIFFER,
            EntityType.CAVE_SPIDER, EntityType.HORSE, EntityType.DONKEY, EntityType.CAT, EntityType.OCELOT,
            EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.GHAST);
    /** What the ores may be instead. */
    private static final List<Block> ORES = List.of(Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.GOLD_BLOCK,
            Blocks.IRON_BLOCK, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.COAL_BLOCK, Blocks.GLOWSTONE,
            Blocks.SEA_LANTERN, Blocks.AMETHYST_BLOCK, Blocks.RAW_GOLD_BLOCK, Blocks.RAW_COPPER_BLOCK, Blocks.MELON,
            Blocks.PUMPKIN, Blocks.TNT, Blocks.HONEY_BLOCK, Blocks.SLIME_BLOCK, Blocks.CAKE, Blocks.BOOKSHELF,
            Blocks.ANCIENT_DEBRIS, Blocks.CRYING_OBSIDIAN, Blocks.SHROOMLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.MAGMA_BLOCK);
    /** How the creatures are: as they are, about half of the kinds as others, every kind as another, all one kind. */
    private static final int CREATURES_KEEP = 0;
    private static final int CREATURES_HALF = 1;
    private static final int CREATURES_ALL = 2;
    private static final int CREATURES_ONE = 3;
    /** How the biomes are: the game's own, one for all, or each turned into another. */
    private static final int BIOMES_KEEP = 0;
    private static final int BIOMES_ONE = 1;
    private static final int BIOMES_SHUFFLED = 2;

    private static final Map<Integer, UniverseRecipe> CACHE = new ConcurrentHashMap<>();

    private final int number;
    private final int shape;
    @Nullable
    private final BlockState stone;
    @Nullable
    private final BlockState ground;
    @Nullable
    private final BlockState sea;
    private final int biomes;
    private final long biomeSeed;
    private final int fog;
    private final int creatures;
    private final long creatureSeed;
    @Nullable
    private final BlockState ore;
    private final float scale;
    private final float gravity;
    /** Which of its parts the universe is named for (its title). */
    private final int titled;
    /** The biome map, made the first time the land asks (it needs the dimension's biomes). */
    private volatile Map<Holder<Biome>, Holder<Biome>> biomeMap;

    private UniverseRecipe(int number) {
        this.number = number;
        Random random = new Random(mix(number));
        float s = random.nextFloat();
        this.shape = s < 0.25F ? 0 : s < 0.38F ? 1 : s < 0.5F ? 2 : s < 0.62F ? 3 : s < 0.75F ? 4 : s < 0.87F ? 5 : 6;
        this.stone = random.nextFloat() < 0.7F ? pick(STONES, random).defaultBlockState() : skip(random);
        this.ground = random.nextFloat() < 0.6F ? pick(GROUNDS, random).defaultBlockState() : skip(random);
        this.sea = random.nextFloat() < 0.5F ? pick(SEAS, random).defaultBlockState() : skip(random);
        float b = random.nextFloat();
        this.biomes = b < 0.25F ? BIOMES_KEEP : b < 0.5F ? BIOMES_ONE : BIOMES_SHUFFLED;
        this.biomeSeed = random.nextLong();
        this.fog = random.nextFloat() < 0.75F ? hsv(random.nextFloat(), 0.35F + random.nextFloat() * 0.65F,
                0.45F + random.nextFloat() * 0.55F) : -1;
        float c = random.nextFloat();
        this.creatures = c < 0.25F ? CREATURES_KEEP : c < 0.55F ? CREATURES_HALF : c < 0.8F ? CREATURES_ALL : CREATURES_ONE;
        this.creatureSeed = random.nextLong();
        this.ore = random.nextFloat() < 0.4F ? pick(ORES, random).defaultBlockState() : skip(random);
        float z = random.nextFloat();
        this.scale = z < 0.15F ? 2.0F : z < 0.3F ? 0.5F : 1.0F;
        float g = random.nextFloat();
        this.gravity = g < 0.18F ? -0.65F : g < 0.3F ? 0.6F : 0.0F;
        this.titled = random.nextInt(1 << 16);
    }

    /** The number well mixed (splitmix64): neighbouring numbers give unrelated draws. */
    static long mix(long number) {
        long z = number * 0x9E3779B97F4A7C15L + 0x2545F4914F6CDD1DL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A pick that was not taken still draws, so that the picks after it stay the same. */
    @Nullable
    private static BlockState skip(Random random) {
        random.nextInt();
        return null;
    }

    private static Block pick(List<Block> from, Random random) {
        return from.get(random.nextInt(from.size()));
    }

    /** The recipe of a universe number. */
    public static UniverseRecipe of(int number) {
        UniverseRecipe recipe = CACHE.get(number);
        if (recipe == null) {
            if (CACHE.size() > 4096) {
                CACHE.clear();
            }
            recipe = new UniverseRecipe(number);
            CACHE.put(number, recipe);
        }
        return recipe;
    }

    public int number() {
        return number;
    }

    /**
     * 0 the Overworld's land, 1 amplified, 2 floating islands, 3 caves, 4 a hollow world under a roof, 5 islands over the
     * void, 6 huge biomes: the index into {@link Multiverse#SHAPES}.
     */
    public int shape() {
        return shape;
    }

    /** What the land is made of instead of stone (and deepslate), or null for stone. */
    @Nullable
    public BlockState stone() {
        return stone;
    }

    /** What lies on top instead of grass (and the dirt under it), or null for grass. */
    @Nullable
    public BlockState ground() {
        return ground;
    }

    /** What fills the seas instead of water, or null for water. */
    @Nullable
    public BlockState sea() {
        return sea;
    }

    /** The colour of the sky's haze and the fog (0xRRGGBB), or -1 for the game's own. */
    public int fog() {
        return fog;
    }

    /** What every ore is instead, or null for the ores. */
    @Nullable
    public BlockState ore() {
        return ore;
    }

    /** How big every creature is (1, 2 giants, 0.5 tiny). */
    public float scale() {
        return scale;
    }

    /** How much lighter or heavier everything falls (0 as at home, -0.65 the moon's, 0.6 heavy): of the gravity. */
    public float gravity() {
        return gravity;
    }

    /** Whether the land swaps anything when it is made. */
    public boolean swapsBlocks() {
        return stone != null || ground != null || sea != null || ore != null;
    }

    /** Whether the biomes are not the game's own. */
    public boolean swapsBiomes() {
        return biomes != BIOMES_KEEP;
    }

    /**
     * The biome that stands where the game would have put {@code biome}: itself, one biome for all, or another of the
     * dimension's biomes ({@code possible}, the only ones the land may hold: their features are the ones it knows).
     */
    public Holder<Biome> biome(Holder<Biome> biome, Set<Holder<Biome>> possible) {
        if (biomes == BIOMES_KEEP) {
            return biome;
        }
        Map<Holder<Biome>, Holder<Biome>> map = biomeMap;
        if (map == null) {
            map = makeBiomeMap(possible);
            biomeMap = map;
        }
        return map.getOrDefault(biome, biome);
    }

    private Map<Holder<Biome>, Holder<Biome>> makeBiomeMap(Set<Holder<Biome>> possible) {
        List<Holder<Biome>> all = new ArrayList<>(possible);
        all.sort(Comparator.comparing(Holder::getRegisteredName));
        Map<Holder<Biome>, Holder<Biome>> map = new HashMap<>();
        if (all.isEmpty()) {
            return map;
        }
        Random random = new Random(biomeSeed);
        Holder<Biome> one = all.get(random.nextInt(all.size()));
        for (Holder<Biome> from : all) {
            map.put(from, biomes == BIOMES_ONE ? one : all.get(random.nextInt(all.size())));
        }
        return map;
    }

    /** The kind a creature of kind {@code type} comes as here (itself if it does not change). */
    public EntityType<?> creature(EntityType<?> type) {
        if (creatures == CREATURES_KEEP) {
            return type;
        }
        int index = CREATURES.indexOf(type);
        if (index < 0) {
            return type;
        }
        if (creatures == CREATURES_ONE) {
            return CREATURES.get((int) Math.floorMod(creatureSeed, (long) CREATURES.size()));
        }
        long h = creatureSeed + index * 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        // About half the kinds change (all of them in a universe of changed creatures): the others stay as they are.
        if (creatures == CREATURES_HALF && (h & 1L) == 0L) {
            return type;
        }
        return CREATURES.get((int) Math.floorMod(h >>> 1, (long) CREATURES.size()));
    }

    /**
     * "What If... the World Were Made of Clay?": the thing a universe is best known by, one of the things that make it
     * itself, drawn from the number (so that not every universe is named for its stone).
     */
    public Component title() {
        List<Component> names = new ArrayList<>();
        if (stone != null) {
            names.add(Component.translatable("what_if.enhanced.made_of", stone.getBlock().getName()));
        }
        if (sea != null) {
            names.add(Component.translatable("what_if.enhanced.seas_of", sea.getBlock().getName()));
        }
        if (ground != null) {
            names.add(Component.translatable("what_if.enhanced.ground_of", ground.getBlock().getName()));
        }
        if (ore != null) {
            names.add(Component.translatable("what_if.enhanced.ores_of", ore.getBlock().getName()));
        }
        if (shape != 0) {
            names.add(Component.translatable("what_if.enhanced." + switch (shape) {
                case 1 -> "mountains";
                case 2 -> "islands";
                case 3 -> "caves";
                case 4 -> "hollow";
                case 5 -> "void";
                default -> "huge";
            }));
        }
        if (gravity != 0.0F) {
            names.add(Component.translatable(gravity < 0.0F ? "what_if.enhanced.light" : "what_if.enhanced.heavy"));
        }
        if (scale != 1.0F) {
            names.add(Component.translatable(scale > 1.0F ? "what_if.enhanced.giants" : "what_if.enhanced.tiny"));
        }
        if (creatures == CREATURES_ONE) {
            names.add(Component.translatable("what_if.enhanced.only", CREATURES.get((int) Math.floorMod(creatureSeed, (long) CREATURES.size())).getDescription()));
        }
        if (biomes != BIOMES_KEEP) {
            names.add(Component.translatable("what_if.enhanced.biomes"));
        }
        return names.isEmpty() ? Component.translatable("what_if.enhanced.nothing") : names.get(titled % names.size());
    }

    private static int hsv(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        int sector = (int) h;
        float f = h - sector;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - f * saturation);
        float t = value * (1.0F - (1.0F - f) * saturation);
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}

package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * The multiverse (3.19). Every place belongs to a universe, known by its number, as in the comics:
 * <ul>
 *     <li><b>616</b>, the main universe: the Overworld (and the Nether, the End and the mod's realms, which belong to it),
 *     where a new world starts, with every structure of the game and of the mod but the Mutant School;</li>
 *     <li><b>10005</b>, the X-Men's universe ({@code enhanced:universe_10005}): a normal world of large biomes with the
 *     game's own structures and the Mutant School, the only place it is built now;</li>
 *     <li><b>2149</b>, the zombie universe ({@code enhanced:universe_2149}): the same land as 616, overrun. Zombies with
 *     helmets walk by day and by night, four times as many as a normal night, and every village is abandoned;</li>
 *     <li><b>What If</b> universes ({@code enhanced:what_if}), as many as you like, as in the infinite dimensions of the
 *     game's April Fools update (20w14&infin;): four dimensions of four shapes of land (the Overworld's, amplified,
 *     floating islands, caves), each cut into square cells {@link #CELL} blocks wide, each cell a universe of its own with
 *     its own number. The number is a seed ({@link UniverseRecipe}): it picks the shape, the stone, the ground, the seas,
 *     the biomes, the colour of the sky and the creatures. Any number or any words name one;</li>
 *     <li>three <b>What If</b> universes of the series, each a world of its own: the Gamma Bomb went off
 *     ({@link #GAMMA}), Hydra won ({@link #HYDRA}) and Ultron won ({@link #ULTRON}).</li>
 * </ul>
 * Outside 616 there are none of the mod's structures (so no Infinity Stone sites): the stones are found in chests
 * instead, one of each per universe ({@link UniverseStones}), and a stone works only in the universe it came from.
 */
public final class Multiverse {
    public static final int MAIN = 616;
    public static final int XMEN = 10005;
    public static final int ZOMBIES = 2149;

    public static final ResourceKey<Level> XMEN_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("universe_10005"));
    public static final ResourceKey<Level> ZOMBIES_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("universe_2149"));
    public static final ResourceKey<Level> WHAT_IF_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if"));
    public static final ResourceKey<Level> AMPLIFIED_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if_amplified"));
    public static final ResourceKey<Level> ISLANDS_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if_islands"));
    public static final ResourceKey<Level> CAVES_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if_caves"));
    /** (3.19.1, second round) A hollow world under a roof (the Nether's shape), islands over the void (the End's), huge biomes. */
    public static final ResourceKey<Level> HOLLOW_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if_hollow"));
    public static final ResourceKey<Level> VOID_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if_void"));
    public static final ResourceKey<Level> HUGE_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if_huge"));
    /** The seven shapes of land of the random universes, in the order of {@link UniverseRecipe#shape()}. */
    public static final List<ResourceKey<Level>> SHAPES = List.of(WHAT_IF_KEY, AMPLIFIED_KEY, ISLANDS_KEY, CAVES_KEY,
            HOLLOW_KEY, VOID_KEY, HUGE_KEY);

    /** The three What If universes of the series (numbers no cell ever has). */
    public static final int GAMMA = 8_000_001;
    public static final int HYDRA = 8_000_002;
    public static final int ULTRON = 8_000_003;
    public static final ResourceKey<Level> GAMMA_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if_gamma"));
    public static final ResourceKey<Level> HYDRA_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if_hydra"));
    public static final ResourceKey<Level> ULTRON_KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("what_if_ultron"));

    /** The width of one What If universe (blocks). */
    public static final int CELL = 100_000;
    /** Cells from the centre to the edge, each way: the world border (30 million) keeps a cell whole. */
    public static final int HALF = 299;
    /** Cells along one side, and in all: the number of What If universes that are told apart. */
    public static final int SIDE = 2 * HALF + 1;
    public static final int CELLS = SIDE * SIDE;
    /** The shuffle of numbers onto cells (a multiplier coprime with {@link #CELLS}, and its inverse). */
    private static final long MUL = 104_729L;
    private static final long ADD = 7_919L;
    private static final long INV = BigInteger.valueOf(MUL).modInverse(BigInteger.valueOf(CELLS)).longValue();

    private Multiverse() {
    }

    // ---- which universe ----

    /** Whether the dimension is one of the multiverse's own (10005, 2149, the random ones, the series' three). */
    public static boolean isUniverseDimension(ResourceKey<Level> dimension) {
        return dimension == XMEN_KEY || dimension == ZOMBIES_KEY || isRandomDimension(dimension) || dimension == GAMMA_KEY
                || dimension == HYDRA_KEY || dimension == ULTRON_KEY;
    }

    /** Whether the dimension is one of the seven cut into the random universes' cells. */
    public static boolean isRandomDimension(ResourceKey<Level> dimension) {
        return SHAPES.contains(dimension);
    }

    /** Whether the universe is one of the series' three What Ifs (Gamma, Hydra, Ultron). */
    public static boolean isThemed(int universe) {
        return universe == GAMMA || universe == HYDRA || universe == ULTRON;
    }

    /** The universe of a place: the cell's number in What If, the dimension's otherwise (616 for every other one). */
    public static int universeAt(Level level, BlockPos pos) {
        return universeAt(level.dimension(), pos.getX(), pos.getZ());
    }

    public static int universeAt(ResourceKey<Level> dimension, double x, double z) {
        if (dimension == XMEN_KEY) {
            return XMEN;
        }
        if (dimension == ZOMBIES_KEY) {
            return ZOMBIES;
        }
        if (dimension == GAMMA_KEY) {
            return GAMMA;
        }
        if (dimension == HYDRA_KEY) {
            return HYDRA;
        }
        if (dimension == ULTRON_KEY) {
            return ULTRON;
        }
        if (isRandomDimension(dimension)) {
            return numberOfCell(cellOf(x), cellOf(z));
        }
        return MAIN;
    }

    /** The cell (along one axis) of a coordinate of What If. */
    public static int cellOf(double coordinate) {
        return Math.max(-HALF, Math.min(HALF, (int) Math.floor((coordinate + CELL / 2.0) / CELL)));
    }

    /**
     * The universe number of a What If cell. The three cells whose number is 616, 10005 or 2149 are never travelled to
     * (see {@link #fold}); someone who walks into one is in a universe numbered past {@link #CELLS}.
     */
    public static int numberOfCell(int cx, int cz) {
        long index = (long) (cx + HALF) + (long) (cz + HALF) * SIDE;
        int n = (int) Math.floorMod((index - ADD) * INV, (long) CELLS);
        n = n == 0 ? CELLS : n;
        return isWhatIf(n) ? n : n + CELLS;
    }

    /** The cell {x, z} of a What If universe number (1 to {@link #CELLS}). */
    public static int[] cellOf(int number) {
        long n = number == CELLS ? 0 : Math.floorMod(number, CELLS);
        long index = Math.floorMod(n * MUL + ADD, (long) CELLS);
        return new int[]{(int) (index % SIDE) - HALF, (int) (index / SIDE) - HALF};
    }

    /** The centre of a What If universe (block x, z). */
    public static BlockPos centreOf(int number) {
        int[] cell = cellOf(number);
        return new BlockPos(cell[0] * CELL, 64, cell[1] * CELL);
    }

    /** Whether the number is a random universe of the cells (not 616, 10005, 2149 or one of the series' What Ifs). */
    public static boolean isWhatIf(int universe) {
        return universe != MAIN && universe != XMEN && universe != ZOMBIES && !isThemed(universe);
    }

    /**
     * The universe that words name, as the April Fools books did: a number names itself (616, 10005, 2149, or a What If
     * universe, folded into 1 to {@link #CELLS}); anything else is turned into a number of its own, always the same.
     */
    public static int named(String words) {
        String text = words.trim().toLowerCase(Locale.ROOT);
        if (text.startsWith("earth-") || text.startsWith("earth ")) {
            text = text.substring(6).trim();
        }
        if (text.matches("\\d{1,9}")) {
            int n = Integer.parseInt(text);
            if (n == MAIN || n == XMEN || n == ZOMBIES) {
                return n;
            }
            return fold(n);
        }
        long h = 1125899906842597L;
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            h = 31 * h + b;
        }
        return fold(h);
    }

    /** Folds any number into a What If universe (1 to {@link #CELLS}, never 616, 10005 or 2149). */
    public static int fold(long n) {
        int folded = (int) (Math.floorMod(n - 1, (long) CELLS) + 1);
        return isWhatIf(folded) ? folded : folded + 1;
    }

    // ---- the worlds ----

    /** The level a universe is in (the Overworld for 616), or null if the server has none (a data pack is missing). */
    @Nullable
    public static ServerLevel levelOf(MinecraftServer server, int universe) {
        if (universe == MAIN) {
            return server.overworld();
        }
        if (universe == XMEN) {
            return server.getLevel(XMEN_KEY);
        }
        if (universe == ZOMBIES) {
            return server.getLevel(ZOMBIES_KEY);
        }
        if (universe == GAMMA) {
            return server.getLevel(GAMMA_KEY);
        }
        if (universe == HYDRA) {
            return server.getLevel(HYDRA_KEY);
        }
        if (universe == ULTRON) {
            return server.getLevel(ULTRON_KEY);
        }
        return server.getLevel(SHAPES.get(UniverseRecipe.of(universe).shape()));
    }

    /** The universe's name for players: "Earth-616 (the main universe)", "Earth-48213: What If... the World Were Made of Clay?". */
    public static Component name(int universe) {
        return switch (universe) {
            case MAIN -> Component.translatable("universe.enhanced.616");
            case XMEN -> Component.translatable("universe.enhanced.10005");
            case ZOMBIES -> Component.translatable("universe.enhanced.2149");
            case GAMMA -> Component.translatable("universe.enhanced.gamma");
            case HYDRA -> Component.translatable("universe.enhanced.hydra");
            case ULTRON -> Component.translatable("universe.enhanced.ultron");
            default -> Component.translatable("universe.enhanced.what_if", universe, UniverseRecipe.of(universe).title());
        };
    }
}

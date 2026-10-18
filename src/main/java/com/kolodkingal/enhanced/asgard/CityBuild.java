package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.asgard.build.CityBuilder;
import com.kolodkingal.enhanced.asgard.build.CityCanvas;
import com.kolodkingal.enhanced.asgard.build.CityTemplates;
import com.kolodkingal.enhanced.asgard.build.GoldenRoad;
import com.kolodkingal.enhanced.asgard.build.ObservatoryBase;
import com.kolodkingal.enhanced.asgard.build.PalaceSpires;
import com.kolodkingal.enhanced.asgard.build.Palette;
import com.kolodkingal.enhanced.asgard.build.RainbowBridge;
import com.kolodkingal.enhanced.asgard.build.Stairways;
import com.kolodkingal.enhanced.asgard.build.Terraces;
import com.kolodkingal.enhanced.asgard.build.Trees;
import com.kolodkingal.enhanced.asgard.build.Waterways;
import com.kolodkingal.enhanced.block.asgard.AsgardBenchBlock;
import com.kolodkingal.enhanced.block.asgard.AsgardMuralBlock;
import com.kolodkingal.enhanced.block.asgard.OdinThroneBlock;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The building of the city of Asgard (3.5), one chunk at a time during generation ({@link #place}): the procedural
 * builders, then the templates of the plan's pieces, the trees, then the fixed blocks the other lanes and the tests find
 * at the plan's fixed points (the throne, Hela's Seal, the Hofund socket and the Bifrost mechanism, the circle of runes
 * round them, Gungnir's stand, the benches). Deterministic: nothing of the world's seed or its randomness. And what the
 * city's own code changes later: the paintings of the throne room ({@link #setMurals}), the vault's door
 * ({@link #setVaultDoor}).
 */
public final class CityBuild {
    /** The builders, in the order they draw (a later one wins where two touch; the templates win over them all). */
    public static final List<CityBuilder> BUILDERS;

    static {
        List<CityBuilder> builders = new ArrayList<>();
        builders.add(Terraces.INSTANCE);
        builders.add(Stairways.INSTANCE);
        builders.add(GoldenRoad.INSTANCE);
        builders.add(Waterways.INSTANCE);
        builders.add(RainbowBridge.INSTANCE);
        builders.add(ObservatoryBase.INSTANCE);
        builders.addAll(PalaceSpires.SPIRES);
        BUILDERS = List.copyOf(builders);
    }

    /** Builds the city's part of {@code chunk} (its land is there already). {@code templates}: null leaves them out. */
    public static void place(ChunkAccess chunk, @Nullable StructureTemplateManager templates) {
        CityCanvas canvas = new CityCanvas(chunk);
        BoundingBox box = canvas.box();
        for (CityBuilder builder : BUILDERS) {
            if (builder.box().intersects(box)) {
                builder.place(canvas);
            }
        }
        CityTemplates.place(canvas, templates);
        Trees.INSTANCE.place(canvas);
        placeFixed(canvas);
    }

    /** The fixed blocks of the plan, where they fall in this chunk. */
    private static void placeFixed(CityCanvas canvas) {
        set(canvas, CityPlan.THRONE, ModBlocks.ODIN_THRONE.get().defaultBlockState().setValue(OdinThroneBlock.FACING, Direction.SOUTH));
        set(canvas, CityPlan.HELA_SEAL, ModBlocks.HELA_SEAL.get().defaultBlockState());
        set(canvas, CityPlan.OBSERVATORY_SOCKET, ModBlocks.HOFUND_SOCKET.get().defaultBlockState());
        set(canvas, CityPlan.MECHANISM, ModBlocks.BIFROST_MECHANISM.get().defaultBlockState());
        set(canvas, CityPlan.GUNGNIR_STAND, ModBlocks.GUNGNIR_STAND.get().defaultBlockState());
        for (BlockPos rune : runeCircle()) {
            set(canvas, rune, Palette.bridge(Direction.Axis.Z));
        }
        for (CityPlan.Bench bench : CityPlan.benches()) {
            set(canvas, bench.pos(), ModBlocks.ASGARD_BENCH.get().defaultBlockState().setValue(AsgardBenchBlock.FACING, bench.facing()));
        }
    }

    private static void set(CityCanvas canvas, BlockPos pos, BlockState state) {
        canvas.set(pos.getX(), pos.getY(), pos.getZ(), state);
    }

    /** The circle of runes in the Observatory's floor round the socket: bridge crystal. */
    public static List<BlockPos> runeCircle() {
        List<BlockPos> out = new ArrayList<>();
        BlockPos c = CityPlan.MECHANISM;
        int r = (int) Math.ceil(CityPlan.RUNE_CIRCLE_RADIUS);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(d - CityPlan.RUNE_CIRCLE_RADIUS) < 0.5) {
                    out.add(c.offset(dx, 0, dz));
                }
            }
        }
        return out;
    }

    /**
     * Turns the murals of the throne room to Hela's conquests ({@code conquest} true, when she arrives) or back to the
     * peaceful paintings (called by lane L4's fight). Records the state in {@link AsgardState} and swaps every painting
     * of the throne room that is loaded, with a crack of plaster (pitch 0.85 to 1.15).
     */
    public static void setMurals(ServerLevel level, boolean conquest) {
        AsgardState.get(level.getServer()).setMuralsConquest(conquest);
        BoundingBox room = CityPlan.THRONE_ROOM;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int swapped = 0;
        for (int x = room.minX(); x <= room.maxX(); x++) {
            for (int z = room.minZ(); z <= room.maxZ(); z++) {
                if (!level.isLoaded(at.set(x, room.minY(), z))) {
                    continue;
                }
                for (int y = room.minY(); y <= room.maxY(); y++) {
                    BlockState state = level.getBlockState(at.set(x, y, z));
                    if (state.getBlock() instanceof AsgardMuralBlock && state.getValue(AsgardMuralBlock.CONQUEST) != conquest) {
                        level.setBlock(at, state.setValue(AsgardMuralBlock.CONQUEST, conquest), Block.UPDATE_CLIENTS);
                        if (swapped++ % 16 == 0) {
                            level.playSound(null, at, ModSounds.MURAL_CRACK.get(), SoundSource.BLOCKS, 1.0F,
                                    0.85F + level.random.nextFloat() * 0.3F);
                        }
                    }
                }
            }
        }
    }

    /** Opens the door of Odin's vault (air) or closes it again (solid gold), where it is loaded (for lane L3's Vault). */
    public static void setVaultDoor(ServerLevel level, boolean open) {
        BoundingBox door = CityPlan.VAULT_DOOR;
        BlockState state = open ? Blocks.AIR.defaultBlockState() : ModBlocks.ASGARD_GOLD_PLATING.get().defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(door.minX(), door.minY(), door.minZ(), door.maxX(), door.maxY(), door.maxZ())) {
            if (level.isLoaded(pos)) {
                level.setBlock(pos, state, Block.UPDATE_ALL);
            }
        }
    }

    private CityBuild() {
    }
}

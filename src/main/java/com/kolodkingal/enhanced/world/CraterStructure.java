package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A crater struck from the sky (Mjolnir's landing site, as in the New Mexico desert of <i>Thor</i>): a bowl
 * {@code radius} blocks wide and {@code depth} deep, its floor burnt black and still smoking, a thrown-up rim, scorched
 * streaks running out across the ground, and {@code core} lying in its middle. Only where the ground is fairly level
 * (nine points across it at most {@code max_slope} apart) and dry (all nine). See {@link CraterPiece} for the shape.
 */
public class CraterStructure extends Structure {
    public static final MapCodec<CraterStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            IntProvider.codec(4, 24).fieldOf("radius").forGetter(s -> s.radius),
            IntProvider.codec(1, 12).fieldOf("depth").forGetter(s -> s.depth),
            Codec.intRange(0, 32).optionalFieldOf("max_slope", 5).forGetter(s -> s.maxSlope),
            BlockState.CODEC.fieldOf("core").forGetter(s -> s.core)
    ).apply(instance, CraterStructure::new));

    private final IntProvider radius;
    private final IntProvider depth;
    private final int maxSlope;
    private final BlockState core;

    public CraterStructure(StructureSettings settings, IntProvider radius, IntProvider depth, int maxSlope, BlockState core) {
        super(settings);
        this.radius = radius;
        this.depth = depth;
        this.maxSlope = maxSlope;
        this.core = core;
    }

    public BlockState core() {
        return core;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int r = radius.sample(context.random());
        int d = Math.min(depth.sample(context.random()), r - 2);
        long seed = context.random().nextLong();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int center = SiteGround.surface(context, x, z);
        int low = center;
        int high = center;
        int reach = Mth.ceil(r * 1.3);
        int[][] points = new int[8][3];
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            int px = x + (int) Math.round(Math.cos(a) * reach);
            int pz = z + (int) Math.round(Math.sin(a) * reach);
            int h = SiteGround.surface(context, px, pz);
            points[i] = new int[] {px, h, pz};
            low = Math.min(low, h);
            high = Math.max(high, h);
        }
        if (high - low > maxSlope || !SiteGround.dryLand(context, x, z, center)) {
            return Optional.empty();
        }
        for (int[] point : points) {
            if (!SiteGround.dryLand(context, point[0], point[2], point[1])) {
                return Optional.empty();
            }
        }
        BlockPos at = new BlockPos(x, center, z);
        return Optional.of(new GenerationStub(at.above(), builder -> builder.addPiece(new CraterPiece(at, r, d, seed, core))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CRATER.get();
    }
}

package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

/**
 * A site carved into an island of the End (3.6, {@code enhanced:carved_site}): the End tunnel, whose vault holds the
 * Orb. Its template's entrance is its north face ({@code z} 0, its layer {@code surface_layer} on the island's
 * surface), its {@code vault} a box of it that must lie deep in the island. Only on the outer islands (the chunk at
 * least {@code min_distance} from 0, 0) where the chunk's middle stands at least {@code min_surface} high (with
 * island ten under it); then, for each way the
 * template may be turned (the first at random), the island's edge that way: the first column within {@code reach} whose
 * surface lies more than {@code drop} under the middle's; the entrance is set on the last column before it, the tunnel
 * going in under the island; the whole is sunk (at most {@code max_sink}) until the island covers the vault by {@code
 * cover} layers, and taken only if the vault's box is nine tenths island (End stone) before it is carved. The
 * template's air carves the tunnel; outside its walls it is void. {@code enabled_by}: the {@code [stone_sites]} option
 * that stops it ({@link SiteSwitch}).
 */
public class CarvedSiteStructure extends Structure {
    private static final Holder<StructureProcessorList> NONE = Holder.direct(new StructureProcessorList(List.of()));
    private static final Rotation[] TURNS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180,
            Rotation.COUNTERCLOCKWISE_90};

    public static final MapCodec<CarvedSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            Codec.intRange(0, 64).optionalFieldOf("surface_layer", 8).forGetter(s -> s.surfaceLayer),
            BoundingBox.CODEC.optionalFieldOf("vault", new BoundingBox(2, 0, 35, 12, 6, 45)).forGetter(s -> s.vault),
            Codec.intRange(1, 64).optionalFieldOf("reach", 24).forGetter(s -> s.reach),
            Codec.intRange(1, 64).optionalFieldOf("drop", 8).forGetter(s -> s.drop),
            Codec.intRange(0, 64).optionalFieldOf("cover", 6).forGetter(s -> s.cover),
            Codec.intRange(0, 64).optionalFieldOf("max_sink", 4).forGetter(s -> s.maxSink),
            Codec.intRange(0, 100000).optionalFieldOf("min_distance", 1000).forGetter(s -> s.minDistance),
            Codec.intRange(-64, 320).optionalFieldOf("min_surface", 60).forGetter(s -> s.minSurface),
            SiteSwitch.OPTION.optionalFieldOf("enabled_by").forGetter(s -> s.enabledBy)
    ).apply(instance, CarvedSiteStructure::new));

    private final ResourceLocation template;
    private final int surfaceLayer;
    private final BoundingBox vault;
    private final int reach;
    private final int drop;
    private final int cover;
    private final int maxSink;
    private final int minDistance;
    private final int minSurface;
    private final Optional<String> enabledBy;

    public CarvedSiteStructure(StructureSettings settings, ResourceLocation template) {
        this(settings, template, 8, new BoundingBox(2, 0, 35, 12, 6, 45), 24, 8, 6, 4, 1000, 60, Optional.empty());
    }

    public CarvedSiteStructure(StructureSettings settings, ResourceLocation template, int surfaceLayer,
            BoundingBox vault, int reach, int drop, int cover, int maxSink, int minDistance, int minSurface,
            Optional<String> enabledBy) {
        super(settings);
        this.template = template;
        this.surfaceLayer = surfaceLayer;
        this.vault = vault;
        this.reach = reach;
        this.drop = drop;
        this.cover = cover;
        this.maxSink = maxSink;
        this.minDistance = minDistance;
        this.minSurface = minSurface;
        this.enabledBy = enabledBy;
    }

    public ResourceLocation template() {
        return template;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!SiteSwitch.enabled(enabledBy)) {
            return Optional.empty();
        }
        StructureTemplateManager manager = context.structureTemplateManager();
        Vec3i size = manager.getOrCreate(template).getSize();
        if (size.getX() == 0 || size.getZ() == 0) {
            return Optional.empty();
        }
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX();
        int cz = chunk.getMiddleBlockZ();
        if ((long) cx * cx + (long) cz * cz < (long) minDistance * minDistance) {
            return Optional.empty();
        }
        int middle = SiteGround.surface(context, cx, cz);
        if (middle < minSurface || !solid(column(context, cx, cz).getBlock(middle - 10))) {
            return Optional.empty();
        }
        int first = context.random().nextInt(4);
        for (int k = 0; k < 4; k++) {
            Rotation rotation = TURNS[(first + k) % 4];
            Direction out = rotation.rotate(Direction.NORTH);
            OptionalInt edge = edge(d -> SiteGround.surface(context, cx + out.getStepX() * d, cz + out.getStepZ() * d),
                    middle, reach, drop);
            if (edge.isEmpty()) {
                continue;
            }
            int last = edge.getAsInt();
            BlockPos entrance = new BlockPos(cx + out.getStepX() * last, 0, cz + out.getStepZ() * last);
            int surface = last == 0 ? middle : SiteGround.surface(context, entrance.getX(), entrance.getZ());
            // the template's middle (its pivot) lies half its length in from the entrance, the other way
            BlockPos level = entrance.relative(out.getOpposite(), size.getZ() / 2);
            BlockPos probe = level.atY(surface);
            TemplateSitePiece trial = piece(manager, rotation, probe, size);
            BlockPos vaultTop = SitePoints.toWorld(trial, new BlockPos(vault.getCenter().getX(), vault.maxY(),
                    vault.getCenter().getZ()));
            int over = SiteGround.surface(context, vaultTop.getX(), vaultTop.getZ());
            int sink = Math.max(0, vaultTop.getY() + cover - over);
            if (sink > maxSink) {
                continue;
            }
            TemplateSitePiece piece = piece(manager, rotation, probe.below(sink), size);
            if (islandShare(context, piece) < 0.9) {
                continue;
            }
            BlockPos at = probe.below(sink);
            return Optional.of(new GenerationStub(at, builder -> builder.addPiece(piece)));
        }
        return Optional.empty();
    }

    /** The site's one piece, turned by {@code rotation}, its middle at {@code center} on the entrance's layer. */
    public TemplateSitePiece piece(StructureTemplateManager manager, Rotation rotation, BlockPos center, Vec3i size) {
        return new TemplateSitePiece(manager, template, NONE, rotation, center, size, surfaceLayer);
    }

    /**
     * How far out from the middle (surface {@code middle}) the island goes before its edge: the last step d (0 to
     * reach - 1) before the first column whose surface lies more than {@code drop} lower; empty if there is none
     * within {@code reach}.
     */
    public static OptionalInt edge(IntUnaryOperator surfaceAt, int middle, int reach, int drop) {
        for (int d = 1; d <= reach; d++) {
            if (surfaceAt.applyAsInt(d) < middle - drop) {
                return OptionalInt.of(d - 1);
            }
        }
        return OptionalInt.empty();
    }

    /** The share of the vault's box that is island before it is carved (nine columns, every layer). */
    private double islandShare(GenerationContext context, TemplateSitePiece piece) {
        return islandShare(vault, local -> SitePoints.toWorld(piece, local),
                (x, z) -> column(context, x, z)::getBlock);
    }

    /**
     * The share of a template's {@code vault} box (template-relative) that is solid ground before the template is set
     * down: nine of its columns ({@code toWorld} places them), every layer, read from {@code columns} (x, z to the
     * block at each y).
     */
    public static double islandShare(BoundingBox vault, Function<BlockPos, BlockPos> toWorld,
            BiFunction<Integer, Integer, IntFunction<BlockState>> columns) {
        int solid = 0;
        int all = 0;
        for (int x : new int[]{vault.minX(), vault.getCenter().getX(), vault.maxX()}) {
            for (int z : new int[]{vault.minZ(), vault.getCenter().getZ(), vault.maxZ()}) {
                BlockPos corner = toWorld.apply(new BlockPos(x, vault.minY(), z));
                IntFunction<BlockState> column = columns.apply(corner.getX(), corner.getZ());
                for (int y = 0; y <= vault.maxY() - vault.minY(); y++) {
                    all++;
                    if (solid(column.apply(corner.getY() + y))) {
                        solid++;
                    }
                }
            }
        }
        return all == 0 ? 0 : (double) solid / all;
    }

    private static NoiseColumn column(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
    }

    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CARVED_SITE.get();
    }
}

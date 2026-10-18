package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.CityPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The templates of Asgard's city, ready to be drawn chunk by chunk (3.5): each piece of {@link CityPlan#pieces} is read
 * once from its template (the server's structure manager), its blocks turned into place in the world and sorted by
 * chunk, so that a chunk draws only its own blocks of the pieces that touch it ({@link #place}). A missing template is
 * told once in the log and left out (generation never fails for it). Made again when a server starts.
 */
public final class CityTemplates {
    /** One piece, its blocks in the world, by chunk. */
    public record Placed(CityPlan.Piece piece, int[] xs, int[] ys, int[] zs, BlockState[] states, CompoundTag[] data,
                         Map<Long, int[]> byChunk) {
    }

    private static volatile List<Placed> placed;
    private static final Set<String> MISSING = new HashSet<>();

    /** Every piece, compiled (on first use, from {@code templates}). */
    public static List<Placed> all(StructureTemplateManager templates) {
        List<Placed> p = placed;
        if (p == null) {
            synchronized (CityTemplates.class) {
                p = placed;
                if (p == null) {
                    p = compile(templates);
                    placed = p;
                }
            }
        }
        return p;
    }

    /** Forgets the compiled pieces (a new server: its data packs may differ). */
    public static synchronized void clear() {
        placed = null;
        MISSING.clear();
    }

    private static List<Placed> compile(StructureTemplateManager templates) {
        List<Placed> out = new ArrayList<>();
        for (CityPlan.Piece piece : CityPlan.pieces()) {
            StructureTemplate template = templates.get(piece.id()).orElse(null);
            if (template == null || template.getSize().getX() == 0) {
                if (MISSING.add(piece.template())) {
                    Enhanced.LOGGER.warn("Asgard: the template {} is missing; the city is built without it", piece.id());
                }
                continue;
            }
            out.add(place(piece, template.save(new CompoundTag())));
        }
        return List.copyOf(out);
    }

    /** One piece's blocks, from its template's saved form (palette + blocks), turned into the world. */
    public static Placed place(CityPlan.Piece piece, CompoundTag saved) {
        ListTag palette = saved.getList("palette", Tag.TAG_COMPOUND);
        BlockState[] states = new BlockState[palette.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(i))
                    .mirror(piece.mirror()).rotate(piece.rotation());
        }
        ListTag blocks = saved.getList("blocks", Tag.TAG_COMPOUND);
        int n = blocks.size();
        int[] xs = new int[n];
        int[] ys = new int[n];
        int[] zs = new int[n];
        BlockState[] st = new BlockState[n];
        CompoundTag[] data = new CompoundTag[n];
        Map<Long, List<Integer>> chunks = new HashMap<>();
        for (int i = 0; i < n; i++) {
            CompoundTag b = blocks.getCompound(i);
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            BlockPos world = piece.toWorld(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)));
            xs[i] = world.getX();
            ys[i] = world.getY();
            zs[i] = world.getZ();
            st[i] = states[b.getInt("state")];
            data[i] = b.contains("nbt") ? b.getCompound("nbt") : null;
            chunks.computeIfAbsent(ChunkPos.asLong(xs[i] >> 4, zs[i] >> 4), k -> new ArrayList<>()).add(i);
        }
        Map<Long, int[]> byChunk = new HashMap<>();
        chunks.forEach((k, v) -> byChunk.put(k, v.stream().mapToInt(Integer::intValue).toArray()));
        return new Placed(piece, xs, ys, zs, st, data, byChunk);
    }

    /** Draws every piece's part of the canvas's chunk, in the plan's order. */
    public static void place(CityCanvas canvas, @Nullable StructureTemplateManager templates) {
        if (templates == null) {
            return;
        }
        long key = canvas.chunk().getPos().toLong();
        for (Placed p : all(templates)) {
            int[] mine = p.byChunk().get(key);
            if (mine == null) {
                continue;
            }
            for (int i : mine) {
                canvas.set(p.xs()[i], p.ys()[i], p.zs()[i], p.states()[i], p.data()[i]);
            }
        }
    }

    private CityTemplates() {
    }
}

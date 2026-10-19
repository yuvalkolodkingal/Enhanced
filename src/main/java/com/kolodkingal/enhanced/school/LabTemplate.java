package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.world.SchoolLabPlan;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * The lower lab's template as the builder reads it (3.7): every position of the 45 x 28 x 42 box with its block (null:
 * structure void, where the builder keeps solid ground or sets stone) and the block entities' data (chests' loot
 * tables, signs). Read once per server from the structure manager ({@link SchoolLabPlan#TEMPLATE}).
 */
public final class LabTemplate {
    public final int sx;
    public final int sy;
    public final int sz;
    private final BlockState[] states;
    private final Map<Integer, CompoundTag> data = new HashMap<>();

    private static WeakReference<MinecraftServer> cachedFor = new WeakReference<>(null);
    private static LabTemplate cached;

    private LabTemplate(int sx, int sy, int sz) {
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.states = new BlockState[sx * sy * sz];
    }

    /** The template of this server (read the first time). */
    public static synchronized LabTemplate get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (cached == null || cachedFor.get() != server) {
            cached = read(level);
            cachedFor = new WeakReference<>(server);
        }
        return cached;
    }

    private static LabTemplate read(ServerLevel level) {
        StructureTemplate template = level.getStructureManager().getOrCreate(SchoolLabPlan.TEMPLATE);
        CompoundTag root = template.save(new CompoundTag());
        ListTag size = root.getList("size", Tag.TAG_INT);
        LabTemplate out = new LabTemplate(size.getInt(0), size.getInt(1), size.getInt(2));
        ListTag paletteTag = root.contains("palette") ? root.getList("palette", Tag.TAG_COMPOUND)
                : root.getList("palettes", Tag.TAG_LIST).getList(0);
        BlockState[] palette = new BlockState[paletteTag.size()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), paletteTag.getCompound(i));
        }
        for (Tag t : root.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            int x = pos.getInt(0);
            int y = pos.getInt(1);
            int z = pos.getInt(2);
            if (!out.inside(x, y, z)) {
                continue;
            }
            int i = out.index(x, y, z);
            out.states[i] = palette[b.getInt("state")];
            if (b.contains("nbt")) {
                out.data.put(i, b.getCompound("nbt"));
            }
        }
        return out;
    }

    public boolean inside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz;
    }

    private int index(int x, int y, int z) {
        return x + sx * (z + sz * y);
    }

    /** The template's block at a lab-local position, or null (void). */
    @Nullable
    public BlockState at(int x, int y, int z) {
        return states[index(x, y, z)];
    }

    /** The block entity data at a lab-local position, or null. */
    @Nullable
    public CompoundTag data(int x, int y, int z) {
        return data.get(index(x, y, z));
    }

    /** Whether a lab-local position is on the box's outer faces (the shell). */
    public boolean face(int x, int y, int z) {
        return x == 0 || y == 0 || z == 0 || x == sx - 1 || y == sy - 1 || z == sz - 1;
    }

    /** How many face positions the shell has (the shell step's length). */
    public int faceCount() {
        return sx * sy * sz - (sx - 2) * (sy - 2) * (sz - 2);
    }
}

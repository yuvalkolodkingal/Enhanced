package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import com.kolodkingal.enhanced.protect.Shelters;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * (3.19.2) Jean Grey's basic matter manipulation, on Sneak + N: she looks at a block and reshapes it with her mind.
 * With a block in her main hand the block she looks at (and the blocks of the same kind round it on that face, a 3 by
 * 3 patch) becomes that block; with none, matter takes its next natural shape (sand to glass, cobblestone to stone,
 * stone to stone bricks, a log to planks, lava to obsidian, water to ice...). Nothing precious is made out of nothing
 * (no ores, no blocks of diamond, gold, iron...), no block that holds something (a chest) is touched, and every rule of
 * breaking and placing applies (boss arenas, claims, spawn protection, a stranger's Hex).
 */
public final class PhoenixMatter {
    /** How far she reshapes. */
    public static final double RANGE = 24.0;
    /** Ticks between two reshapings. */
    public static final int COOLDOWN_TICKS = 8;
    /** The hunger one reshaped block costs. */
    private static final float EXHAUSTION = 0.25F;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** The words that mark a block too precious to be made from nothing. */
    private static final String[] PRECIOUS = {"ore", "diamond", "emerald", "netherite", "gold", "iron_block", "ancient_debris",
            "beacon", "spawner", "command", "bedrock", "barrier", "structure", "jigsaw", "portal", "dragon", "lapis_block",
            "redstone_block", "copper_block", "raw_", "amethyst_block", "budding", "reinforced", "trial", "vault",
            "infested", "conduit", "heavy_core", "enhanced:"};

    private PhoenixMatter() {
    }

    /** Sneak + N went down. */
    public static void use(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            return;
        }
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 end = Shelters.hexClip(level, player, eye, eye.add(player.getViewVector(1.0F).scale(RANGE)));
        // (water and lava sources count: she freezes and hardens them)
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.enhanced.phoenix_matter.nothing"), true);
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState from = level.getBlockState(pos);
        BlockState held = heldShape(player.getMainHandItem());
        BlockState to = held != null ? held : naturalShape(from);
        if (to == null || !mayChange(from) || from.getBlock() == to.getBlock()) {
            player.displayClientMessage(Component.translatable("message.enhanced.phoenix_matter.cannot"), true);
            return;
        }
        int changed = 0;
        for (BlockPos at : patch(pos, hit.getDirection())) {
            BlockState there = level.getBlockState(at);
            if (there.getBlock() != from.getBlock() || !mayChange(there)) {
                continue;
            }
            if (!BlockRights.mayBreak(player, level, at, there) || !BlockRights.mayPlace(player, level, at)) {
                continue;
            }
            if (level.setBlockAndUpdate(at, to)) {
                changed++;
                level.sendParticles(new EnergyParticleOptions(com.kolodkingal.enhanced.ability.PhoenixAbilities.TK_ORANGE, 0.9F),
                        at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 6, 0.35, 0.35, 0.35, 0.02);
            }
            if (held == null) {
                break; // matter's own next shape: one block at a time
            }
        }
        if (changed == 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.phoenix_matter.cannot"), true);
            return;
        }
        LAST.put(player.getUUID(), now);
        player.causeFoodExhaustion(EXHAUSTION * changed);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.3F);
        player.displayClientMessage(Component.translatable("message.enhanced.phoenix_matter.done", changed,
                to.getBlock().getName()), true);
    }

    /** The block of the main hand she may shape matter into, or null. */
    @Nullable
    public static BlockState heldShape(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) {
            return null;
        }
        Block block = item.getBlock();
        BlockState state = block.defaultBlockState();
        return precious(block) || state.hasBlockEntity() || state.isAir() ? null : state;
    }

    /** Whether a block is too precious to be made from nothing. */
    static boolean precious(Block block) {
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        for (String word : PRECIOUS) {
            if (id.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** Whether this block may be reshaped at all: not air, not unbreakable, holding nothing. */
    static boolean mayChange(BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && state.getBlock() != Blocks.BEDROCK
                && state.getBlock().defaultDestroyTime() >= 0.0F;
    }

    /** Matter's next natural shape, or null. */
    @Nullable
    static BlockState naturalShape(BlockState from) {
        Block b = from.getBlock();
        Block to = null;
        if (b == Blocks.SAND || b == Blocks.RED_SAND) {
            to = Blocks.GLASS;
        } else if (b == Blocks.COBBLESTONE) {
            to = Blocks.STONE;
        } else if (b == Blocks.STONE) {
            to = Blocks.STONE_BRICKS;
        } else if (b == Blocks.STONE_BRICKS) {
            to = Blocks.CHISELED_STONE_BRICKS;
        } else if (b == Blocks.GRAVEL) {
            to = Blocks.SAND;
        } else if (b == Blocks.DIRT || b == Blocks.COARSE_DIRT) {
            to = Blocks.GRASS_BLOCK;
        } else if (b == Blocks.CLAY) {
            to = Blocks.TERRACOTTA;
        } else if (b == Blocks.NETHERRACK) {
            to = Blocks.NETHER_BRICKS;
        } else if (b == Blocks.WATER) {
            to = Blocks.ICE;
        } else if (b == Blocks.ICE) {
            to = Blocks.PACKED_ICE;
        } else if (b == Blocks.LAVA) {
            to = Blocks.OBSIDIAN;
        } else if (b == Blocks.COBBLED_DEEPSLATE) {
            to = Blocks.DEEPSLATE;
        } else if (b == Blocks.DEEPSLATE) {
            to = Blocks.DEEPSLATE_BRICKS;
        } else {
            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
            if (id.endsWith("_log") || id.endsWith("_stem")) {
                String planks = id.substring(0, id.lastIndexOf('_')) + "_planks";
                planks = planks.replace("stripped_", "");
                for (Block candidate : BuiltInRegistries.BLOCK) {
                    if (BuiltInRegistries.BLOCK.getKey(candidate).toString().equals(planks)) {
                        to = candidate;
                        break;
                    }
                }
            }
        }
        return to == null ? null : to.defaultBlockState();
    }

    /** The 3 by 3 patch of the face she looks at, the block itself first. */
    static List<BlockPos> patch(BlockPos centre, Direction face) {
        List<BlockPos> cells = new ArrayList<>();
        cells.add(centre);
        Direction.Axis axis = face.getAxis();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                cells.add(switch (axis) {
                    case X -> centre.offset(0, a, b);
                    case Y -> centre.offset(a, 0, b);
                    case Z -> centre.offset(a, b, 0);
                });
            }
        }
        return cells;
    }

    /** Forget every cooldown (the server stopped). */
    public static void clear() {
        LAST.clear();
    }
}

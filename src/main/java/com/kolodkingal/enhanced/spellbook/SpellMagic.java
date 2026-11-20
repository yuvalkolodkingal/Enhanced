package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.custom.ModuleLogic;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The spells the spellbooks add to the custom powers' catalogue: Magic's "orb" (the Ethereal Orb), Earth's "throw" and
 * "wall", and Nature's "bloom" (its own module, {@link Nature}). The books' casters are custom heroes ({@link Spellbooks}),
 * so these are ordinary powers: a hero of the Hero Creator may pick them too. Nothing here breaks a block: the wall only
 * fills free air (where the caster may place a block) and crumbles by itself ({@link EarthWallBlock}); the growth only
 * does what bone meal does, and sets flowers and grass only into free air on grass.
 */
public final class SpellMagic {
    /** The wall's height (blocks). */
    public static final int WALL_HEIGHT = 2;
    /** How far in front of the caster the wall rises (blocks). */
    public static final int WALL_DISTANCE = 2;

    private static final List<Block> FLOWERS = List.of(Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY,
            Blocks.AZURE_BLUET, Blocks.ALLIUM, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.FERN);

    private SpellMagic() {
    }

    /** Registers the spellbooks' own module with the custom hero's server ({@code CustomHeroServer}). */
    public static void register(Map<PowerModule, ModuleLogic> logic) {
        logic.put(PowerModule.NATURE, new Nature());
        logic.put(PowerModule.CANTRIP, new Cantrips()); // (3.18.1: the witches' cantrips)
    }

    // ---- the Ethereal Orb (Magic, "orb") ----

    /** A ball of the caster's purple energy leaves his hand. */
    public static void orb(ModuleLogic.Ctx c) {
        SpellShots.launch(c.player, SpellShots.Kind.ORB, c.v(ModuleStats.ORB_DAMAGE), c.v(ModuleStats.ORB_RADIUS), c.color(), c.glow());
        c.sparks(Shots.hand(c.player), 10, 0.15, 1.2F);
        c.cooldown(ModuleStats.ORB_COOLDOWN);
    }

    // ---- the thrown earth (Earth, "throw") ----

    /** A clod of earth torn from nowhere is thrown where he looks. */
    public static void throwEarth(ModuleLogic.Ctx c) {
        SpellShots.launch(c.player, SpellShots.Kind.CLOD, c.v(ModuleStats.THROW_DAMAGE), c.v(ModuleStats.THROW_KNOCKBACK), c.color(),
                c.glow());
        c.cooldown(ModuleStats.THROW_COOLDOWN);
    }

    // ---- the Earth Wall (Earth, "wall") ----

    /**
     * A short wall of packed earth rises across his way, {@link #WALL_DISTANCE} blocks in front of him: as many columns as
     * the power's strength gives, {@link #WALL_HEIGHT} high, each standing on the ground there. Only free air is filled,
     * never where a creature stands, and only where he may place a block; each block crumbles when its time is up.
     *
     * @return the blocks it raised
     */
    public static List<BlockPos> wall(ModuleLogic.Ctx c) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level();
        int length = Math.max(1, Math.round(c.v(ModuleStats.WALL_LENGTH)));
        int ticks = c.ticks(ModuleStats.WALL_TIME);
        List<BlockPos> raised = raise(p, level, p.getDirection(), length, ticks);
        if (raised.isEmpty()) {
            c.refuse("message.enhanced.spellbook.wall_blocked");
            return raised;
        }
        for (BlockPos pos : raised) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.1);
            level.sendParticles(new EnergyParticleOptions(c.color(), 1.0F), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    2, 0.3, 0.3, 0.3, 0.0);
        }
        c.sound(SoundEvents.ROOTED_DIRT_PLACE, 1.4F, 0.6F);
        c.sound(SoundEvents.GRAVEL_PLACE, 1.0F, 0.7F);
        c.cooldown(ModuleStats.WALL_COOLDOWN);
        return raised;
    }

    /** Raises the wall in front of {@code player}, facing {@code facing}: the blocks it put in (each lives {@code ticks}). */
    public static List<BlockPos> raise(ServerPlayer player, ServerLevel level, Direction facing, int length, int ticks) {
        List<BlockPos> raised = new ArrayList<>();
        Direction across = facing.getClockWise();
        BlockPos middle = player.blockPosition().relative(facing, WALL_DISTANCE);
        Block wall = ModBlocks.EARTH_WALL.get();
        for (int i = 0; i < length; i++) {
            int offset = (i + 1) / 2 * (i % 2 == 0 ? 1 : -1);
            BlockPos base = ground(level, middle.relative(across, offset));
            if (base == null) {
                continue;
            }
            for (int h = 0; h < WALL_HEIGHT; h++) {
                BlockPos pos = base.above(h);
                if (!free(level, pos) || !BlockRights.mayPlace(player, level, pos)
                        || !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), LivingEntity::isAlive).isEmpty()) {
                    break;
                }
                level.scheduleTick(pos, wall, ticks);
                if (level.setBlock(pos, wall.defaultBlockState(), Block.UPDATE_ALL)) {
                    raised.add(pos.immutable());
                }
            }
        }
        return raised;
    }

    /**
     * Whether the wall may fill this cell: air, or what any block placed there replaces (grass, a snow layer, a vine), never
     * water or lava, never a block of anyone's and never a flower or a crop (the column is left out instead).
     */
    private static boolean free(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() && !state.hasBlockEntity();
    }

    /** Whether something stands solid in this cell (the wall can stand on it). */
    private static boolean solid(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /**
     * The lowest free cell of the column at {@code at} that stands on something solid (a step up or down at most), or null:
     * the column is left out where the ground there is not free (a flower, a fence, a wall too high) or falls away.
     */
    private static BlockPos ground(ServerLevel level, BlockPos at) {
        BlockPos pos = at;
        for (int up = 0; up < 2 && solid(level, pos); up++) {
            pos = pos.above();
        }
        if (!free(level, pos)) {
            return null;
        }
        for (int down = 0; down < 3 && free(level, pos.below()); down++) {
            pos = pos.below();
        }
        return solid(level, pos.below()) ? pos : null;
    }

    // ---- Nature ("bloom") ----

    /** The power of growth: what grows round the caster grows, as with bone meal, and flowers come up in the grass. */
    static final class Nature implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            if (c.option("roots")) {
                roots(c);
                return;
            }
            int grown = bloom(c.player, c.level(), c.v(ModuleStats.BLOOM_RADIUS));
            WitchLevels.practice(c.player, grown / 5); // (3.18.1: the Green witch's practice, a point for each 5 plants)
            Vec3 at = c.player.position();
            Shots.ring(c.level(), at, c.v(ModuleStats.BLOOM_RADIUS), c.color());
            c.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 1.0, at.z, 20, 1.2, 0.6, 1.2, 0.0);
            c.sparks(at.add(0, 1.0, 0), 16, 0.6, 1.1F);
            c.sound(SoundEvents.AZALEA_LEAVES_PLACE, 1.2F, 0.8F);
            c.sound(SoundEvents.BONE_MEAL_USE, 1.0F, 1.0F);
            if (grown == 0) {
                c.refuse("message.enhanced.spellbook.nothing_grows");
            }
            c.cooldown(ModuleStats.BLOOM_COOLDOWN);
        }
    }

    // ---- 3.18.1: the Root Grasp (Nature, "roots") ----

    /**
     * The Green witch's Root Grasp: roots burst from the ground where she looks and hold the enemies there for a moment
     * (2 seconds at her level 5). One step weaker than the growth it springs from (strength 4 beside her growth's 5),
     * and it holds for less than Agatha's rune trap, so it is never stronger than Agatha.
     *
     * @return the creatures it holds
     */
    public static List<LivingEntity> roots(ModuleLogic.Ctx c) {
        int s = Math.max(1, c.strength() - 1);
        ServerPlayer p = c.player;
        ServerLevel level = c.level();
        Shots.Aim aim = Shots.aim(p, ModuleStats.ROOTS_RANGE.value(s));
        Vec3 at = aim.end();
        double radius = ModuleStats.ROOTS_RADIUS.value(s);
        int ticks = Math.max(1, Math.round(ModuleStats.ROOTS_TIME.value(s)));
        List<LivingEntity> held = new ArrayList<>();
        for (LivingEntity e : Shots.area(p, at, radius + 0.5)) {
            if (e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof net.minecraft.world.entity.player.Player) {
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                        ticks, 9, false, true), p);
                e.setDeltaMovement(0.0, Math.min(0.0, e.getDeltaMovement().y), 0.0);
                e.hurtMarked = true;
                held.add(e);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                        e.getX(), e.getY() + 0.3, e.getZ(), 14, 0.3, 0.3, 0.3, 0.1);
            }
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), at.x, at.y + 0.2,
                at.z, 30, radius * 0.5, 0.2, radius * 0.5, 0.15);
        level.sendParticles(new EnergyParticleOptions(c.color(), 1.1F), at.x, at.y + 0.6, at.z, 16, radius * 0.4, 0.4,
                radius * 0.4, 0.0);
        Shots.line(level, Shots.hand(p), at, c.color(), c.glow());
        c.sound(SoundEvents.ROOTED_DIRT_BREAK, 1.4F, 0.6F);
        c.sound(SoundEvents.HANGING_ROOTS_PLACE, 1.2F, 0.7F);
        if (c.slot > 0) {
            c.data.startCooldown(c.slot, c.now(), Math.max(1, Math.round(ModuleStats.ROOTS_COOLDOWN.value(s))));
        }
        return held;
    }

    /**
     * Everything within {@code radius} that bone meal would make grow grows once (crops, saplings, stems, berries...), and
     * on a grass block with free air above, now and then, a flower or a tuft of grass comes up. Only where the caster may
     * place a block.
     *
     * @return how many blocks grew or flowered
     */
    public static int bloom(ServerPlayer player, ServerLevel level, double radius) {
        RandomSource random = level.random;
        BlockPos center = player.blockPosition();
        int r = (int) Math.ceil(radius);
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 2, r))) {
            if (pos.distSqr(center) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GrassBlock) {
                BlockPos above = pos.above();
                if (level.getBlockState(above).isAir() && random.nextInt(4) == 0 && BlockRights.mayPlace(player, level, above)) {
                    BlockState flower = FLOWERS.get(random.nextInt(FLOWERS.size())).defaultBlockState();
                    if (flower.canSurvive(level, above) && level.setBlock(above, flower, Block.UPDATE_ALL)) {
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, above.getX() + 0.5, above.getY() + 0.3, above.getZ() + 0.5,
                                3, 0.25, 0.2, 0.25, 0.0);
                        grown++;
                    }
                }
                continue;
            }
            if (grows(state) && state.getBlock() instanceof BonemealableBlock crop && crop.isValidBonemealTarget(level, pos, state)
                    && BlockRights.mayPlace(player, level, pos)) {
                crop.performBonemeal(level, random, pos, state);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
                grown++;
            }
        }
        return grown;
    }

    /**
     * Whether the growth touches this block: a plant that grows by itself (crops, saplings, stems, berries, cocoa), never one
     * whose bone meal spreads over the blocks round it (moss, nylium, grass: grass only gets flowers, above).
     */
    private static boolean grows(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || block instanceof StemBlock
                || block instanceof SweetBerryBushBlock || block instanceof CocoaBlock;
    }
}

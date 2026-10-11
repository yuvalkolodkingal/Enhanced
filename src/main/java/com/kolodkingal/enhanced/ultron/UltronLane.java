package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossLane;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * How Ultron is woken: the ritual's checks and the fight it makes. {@code BossFights.start} has already made the checks
 * that are the same for every boss (a lane, the difficulty, how many fights run); this lane checks what is Ultron's own,
 * all of it before anything is used up, and tells the starter the first thing that is wrong in the action bar:
 *
 * <ol>
 *     <li>he is switched off on this server ({@code ultron_boss.enabled});</li>
 *     <li>the starter does not hold the Ultron Program (an operator's forced start needs none);</li>
 *     <li>there is no ground to aim at: the arena is centred on the top face of the block the starter looks at (an
 *     operator's forced start: at their feet);</li>
 *     <li>the dimension has no open sky (the Nether, the End);</li>
 *     <li>the arena, with a margin, is not inside the world border;</li>
 *     <li>the ground is protected: spawn protection or a claim, asked at the middle and eight points of the island's rim
 *     (a simulated block break);</li>
 *     <li>the ground is too uneven or mostly not walkable (the Tremor's rules: the lift's own are {@link
 *     LiftHook#refusal}).</li>
 * </ol>
 *
 * <p>An operator's forced start skips the checks of the site (3 to 7) but not the switch and not the sky. Then the
 * Program is used up (not in Creative) and registered with the fight, which gives it back if the fight ends through no
 * fault of the players.
 */
public final class UltronLane implements BossLane {
    /** The least share of the island's columns that must be walkable ground. */
    public static final double MIN_WALKABLE = 0.60;
    /** How far past the island's rim the arena's margin must still be inside the world border. */
    public static final int BORDER_MARGIN = 14;

    /** How the lane decides a dimension has open sky; the tests replace it to fake the Nether. */
    private static volatile Predicate<ServerLevel> openSky = level -> level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();

    /**
     * Replaces the check that a dimension has open sky (a test pretends to be in the Nether); null puts the real one back.
     *
     * @return the check that was in force
     */
    public static Predicate<ServerLevel> skyCheck(@Nullable Predicate<ServerLevel> check) {
        Predicate<ServerLevel> old = openSky;
        openSky = check != null ? check : level -> level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
        return old;
    }

    @Nullable
    @Override
    public Component refusal(ServerPlayer starter, boolean force) {
        ServerLevel level = starter.serverLevel();
        if (!UltronBossConfig.ENABLED.get()) {
            return Component.translatable("message.enhanced.ultron.refuse.disabled");
        }
        if (!force && programHand(starter) == null) {
            return Component.translatable("message.enhanced.ultron.refuse.program");
        }
        Vec3 centre = centre(starter, force);
        if (centre == null) {
            return Component.translatable("message.enhanced.ultron_program.aim");
        }
        if (!openSky.test(level)) {
            return Component.translatable("message.enhanced.ultron.refuse.dimension");
        }
        if (force) {
            return null;
        }
        int radius = UltronBossConfig.ISLAND_RADIUS.get();
        for (Vec3 point : probes(centre, radius + BORDER_MARGIN)) {
            if (!level.getWorldBorder().isWithinBounds(point.x, point.z, 1.0)) {
                return Component.translatable("message.enhanced.ultron.refuse.border");
            }
        }
        for (Vec3 point : probes(centre, radius)) {
            if (isProtected(level, starter, BlockPos.containing(point.x, centre.y - 1.0, point.z))) {
                return Component.translatable("message.enhanced.ultron.refuse.protected");
            }
        }
        Component site = site(level, centre, radius);
        if (site != null) {
            return site;
        }
        return LiftHooks.get().refusal(level, BlockPos.containing(centre), radius);
    }

    @Nullable
    @Override
    public BossFight create(ServerPlayer starter, boolean force) {
        Vec3 centre = centre(starter, force);
        if (centre == null) {
            return null;
        }
        ServerLevel level = starter.serverLevel();
        UltronFight fight = new UltronFight(level, new UltronArena(level, centre, UltronBossConfig.ISLAND_RADIUS.get()));
        if (!force) {
            InteractionHand hand = programHand(starter);
            if (hand == null) {
                return null;
            }
            ItemStack program = starter.getItemInHand(hand);
            fight.refundOnFailure(program);
            program.consume(1, starter);
        }
        return fight;
    }

    // ---- the checks ----

    /** The hand that holds the Ultron Program, or null. */
    @Nullable
    static InteractionHand programHand(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(ModItems.ULTRON_PROGRAM.get())) {
                return hand;
            }
        }
        return null;
    }

    /**
     * The middle of the arena: the top face of the block the starter looks at within reach, or, for an operator's forced
     * start, the starter's feet. Null if there is no block to aim at.
     */
    @Nullable
    static Vec3 centre(ServerPlayer starter, boolean force) {
        if (force) {
            return starter.position();
        }
        BlockHitResult hit = Raycasts.block(starter, starter.blockInteractionRange(), 1.0F);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        var shape = starter.level().getBlockState(pos).getCollisionShape(starter.level(), pos);
        double top = shape.isEmpty() ? pos.getY() + 1.0 : pos.getY() + shape.max(Direction.Axis.Y);
        return new Vec3(pos.getX() + 0.5, top, pos.getZ() + 0.5);
    }

    /** The middle and eight points round it at {@code radius}: where the checks look. */
    private static Vec3[] probes(Vec3 centre, double radius) {
        Vec3[] points = new Vec3[9];
        points[0] = centre;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            points[i + 1] = new Vec3(centre.x + Math.cos(angle) * radius, centre.y, centre.z + Math.sin(angle) * radius);
        }
        return points;
    }

    /** Whether spawn protection or a claim keeps the starter from changing this block (a simulated break). */
    private static boolean isProtected(ServerLevel level, ServerPlayer starter, BlockPos pos) {
        if (!level.mayInteract(starter, pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, starter)).isCanceled();
    }

    /**
     * The ground of the island must be level enough and walkable: across its columns the ground rises and falls by at
     * most {@code maxRelief}, and at least 60 percent of them are ground one can stand on (solid, with room above and no
     * liquid). Every column of the disc is looked at.
     */
    @Nullable
    static Component site(ServerLevel level, Vec3 centre, int radius) {
        int walkable = 0;
        int total = 0;
        double low = Double.MAX_VALUE;
        double high = -Double.MAX_VALUE;
        BlockPos fluid = null;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                double x = centre.x + dx;
                double z = centre.z + dz;
                total++;
                if (!level.hasChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4)) {
                    continue;
                }
                double ground = UltronMotion.surface(level, x, centre.y + 4.0, z);
                low = Math.min(low, ground);
                high = Math.max(high, ground);
                if (standable(level, x, ground, z)) {
                    walkable++;
                } else if (fluid == null && wet(level, x, ground, z)) {
                    fluid = BlockPos.containing(x, ground, z);
                }
            }
        }
        if (fluid != null && walkable < total * MIN_WALKABLE) {
            return Component.translatable("message.enhanced.ultron.refuse.fluid", fluid.toShortString());
        }
        if (total == 0 || high - low > UltronBossConfig.MAX_RELIEF.get() || walkable < total * MIN_WALKABLE) {
            return Component.translatable("message.enhanced.ultron.refuse.relief");
        }
        return null;
    }

    /** Whether water or lava is at, above or just under the ground of this column. */
    private static boolean wet(ServerLevel level, double x, double ground, double z) {
        BlockPos feet = BlockPos.containing(x, ground + 0.01, z);
        return !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty() || !level.getFluidState(feet.below()).isEmpty();
    }

    private static boolean standable(ServerLevel level, double x, double ground, double z) {
        BlockPos feet = BlockPos.containing(x, ground + 0.01, z);
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() && level.getFluidState(feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty() && level.getFluidState(feet.above()).isEmpty()
                && level.getFluidState(feet.below()).isEmpty();
    }
}

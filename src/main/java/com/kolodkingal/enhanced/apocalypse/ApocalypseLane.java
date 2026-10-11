package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossLane;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
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

/**
 * How Apocalypse is woken: the ritual's checks and the fight it makes (spec 3.2). {@code BossFights.start} has already
 * made the checks that are the same for every boss (a lane, the difficulty, how many fights run, the rest); this lane
 * checks what is his own, all of it <em>before</em> anything is used up, and the first thing that is wrong is told to the
 * starter in the action bar:
 *
 * <ol>
 *     <li>he is switched off on this server ({@code apocalypse_boss.enabled});</li>
 *     <li>the starter holds no Capstone, or aims at no block (an operator's forced start needs neither);</li>
 *     <li>the dimension is not one of {@code ritualDimensions}; it is not day (unless {@code ritualDaylightOnly} is off),
 *     or the block does not see the sky;</li>
 *     <li>the block is not sand or sandstone ({@code enhanced:ritual_ground}, unless {@code ritualNeedsSand} is off);</li>
 *     <li>the circle of radius 8 round the click has no room: liquid in it, or more than 40 percent of its columns blocked
 *     within twelve blocks above;</li>
 *     <li>the arena (radius {@code R + 8}) is not inside the world border, or the ground is protected (spawn
 *     protection or a claim, asked at the click and eight points round it);</li>
 *     <li>no hero who can break his shield (a Scarlet Witch or a Phoenix, unless the server says more) stands within
 *     {@code breakerRange} of the click, which would make the fight unwinnable.</li>
 * </ol>
 *
 * <p>An operator's forced start ({@code /enhanced boss start apocalypse force}) skips 2 to 7 but not the switch. Then the
 * Capstone is used up (not in Creative) and registered with the fight, which gives it back if the fight ends through no
 * fault of the players, and gets a short cooldown.
 */
public final class ApocalypseLane implements BossLane {
    /** The least of the ritual circle's columns that may be blocked: 40 percent. */
    public static final double MAX_BLOCKED = 0.40;
    /** The radius of the circle that must have room, and the height it needs. */
    public static final int ROOM_RADIUS = 8;
    public static final int ROOM_HEIGHT = 12;
    /** The cooldown the Capstone gets when the ritual starts, in ticks. */
    public static final int CAPSTONE_COOLDOWN = 20;

    /** The block the Capstone was clicked on, set by the item for the length of the start; null otherwise. */
    @Nullable
    private static volatile BlockPos clicked;

    /** Called by the Capstone for the length of a start: the lane then centres the arena on the clicked block. */
    public static void startedBy(@Nullable BlockPos click) {
        clicked = click;
    }

    @Nullable
    @Override
    public Component refusal(ServerPlayer starter, boolean force) {
        ServerLevel level = starter.serverLevel();
        if (!ApocalypseBossConfig.ENABLED.get()) {
            return Component.translatable("message.enhanced.apocalypse.refuse.disabled");
        }
        if (force) {
            return null;
        }
        if (capstoneHand(starter) == null) {
            return Component.translatable("message.enhanced.apocalypse.capstone_use");
        }
        BlockPos click = clickedBlock(starter);
        if (click == null) {
            return Component.translatable("message.enhanced.apocalypse.capstone_use");
        }
        String dimension = level.dimension().location().toString();
        if (!ApocalypseBossConfig.RITUAL_DIMENSIONS.get().contains(dimension)) {
            return Component.translatable("message.enhanced.apocalypse.refuse.dimension");
        }
        if (ApocalypseBossConfig.RITUAL_DAYLIGHT_ONLY.get() && (!level.isDay() || !level.canSeeSky(click.above()))) {
            return Component.translatable("message.enhanced.apocalypse.refuse.sun");
        }
        if (ApocalypseBossConfig.RITUAL_NEEDS_SAND.get() && !level.getBlockState(click).is(ModTags.RITUAL_GROUND)) {
            return Component.translatable("message.enhanced.apocalypse.refuse.sand");
        }
        if (!hasRoom(level, click)) {
            return Component.translatable("message.enhanced.apocalypse.refuse.room");
        }
        Vec3 centre = centreOf(level, click);
        double reach = ApocalypseBossConfig.ARENA_RADIUS.get() + 8.0;
        for (Vec3 point : probes(centre, reach)) {
            if (!level.getWorldBorder().isWithinBounds(point.x, point.z, 1.0)) {
                return Component.translatable("message.enhanced.apocalypse.refuse.border");
            }
        }
        for (Vec3 point : probes(centre, ROOM_RADIUS)) {
            if (isProtected(level, starter, BlockPos.containing(point.x, centre.y - 1.0, point.z))) {
                return Component.translatable("message.enhanced.apocalypse.refuse.protected");
            }
        }
        if (ApocalypseBossConfig.REQUIRE_BREAKER.get() && !breakerNear(level, centre)) {
            return Component.translatable("message.enhanced.apocalypse.refuse.breaker");
        }
        return null;
    }

    @Nullable
    @Override
    public BossFight create(ServerPlayer starter, boolean force) {
        ServerLevel level = starter.serverLevel();
        Vec3 centre;
        if (force) {
            centre = starter.position();
        } else {
            BlockPos click = clickedBlock(starter);
            if (click == null) {
                return null;
            }
            centre = centreOf(level, click);
        }
        ApocalypseFight fight = new ApocalypseFight(level, ApocalypseArena.of(level, centre));
        if (!force) {
            InteractionHand hand = capstoneHand(starter);
            if (hand == null) {
                return null;
            }
            ItemStack capstone = starter.getItemInHand(hand);
            fight.refundOnFailure(capstone);
            starter.getCooldowns().addCooldown(capstone.getItem(), CAPSTONE_COOLDOWN);
            capstone.consume(1, starter);
        }
        return fight;
    }

    // ---- the checks ----

    /** The hand that holds the Capstone, or null. */
    @Nullable
    static InteractionHand capstoneHand(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(ModItems.CAPSTONE.get())) {
                return hand;
            }
        }
        return null;
    }

    /** The block the ritual is on: the one the Capstone was clicked on, else the one the starter looks at. */
    @Nullable
    static BlockPos clickedBlock(ServerPlayer starter) {
        BlockPos click = clicked;
        if (click != null) {
            return click;
        }
        BlockHitResult hit = Raycasts.block(starter, starter.blockInteractionRange(), 1.0F);
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    /** The middle of the arena: the top face of the clicked block. */
    static Vec3 centreOf(ServerLevel level, BlockPos click) {
        var shape = level.getBlockState(click).getCollisionShape(level, click);
        double top = shape.isEmpty() ? click.getY() + 1.0 : click.getY() + shape.max(Direction.Axis.Y);
        return new Vec3(click.getX() + 0.5, top, click.getZ() + 0.5);
    }

    /**
     * The circle of radius 8 round the click must have room for him: no liquid in it, and at most 40 percent of its
     * columns blocked (a block that stops the head, three to twelve blocks above the click: a roof, a tree, a hill).
     */
    static boolean hasRoom(ServerLevel level, BlockPos click) {
        int total = 0;
        int blocked = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -ROOM_RADIUS; dx <= ROOM_RADIUS; dx++) {
            for (int dz = -ROOM_RADIUS; dz <= ROOM_RADIUS; dz++) {
                if (dx * dx + dz * dz > ROOM_RADIUS * ROOM_RADIUS) {
                    continue;
                }
                total++;
                boolean columnBlocked = false;
                for (int dy = 0; dy <= ROOM_HEIGHT; dy++) {
                    pos.set(click.getX() + dx, click.getY() + 1 + dy, click.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty()) {
                        return false;
                    }
                    if (dy >= 2 && !state.getCollisionShape(level, pos).isEmpty()) {
                        columnBlocked = true;
                    }
                }
                if (columnBlocked) {
                    blocked++;
                }
            }
        }
        return blocked <= total * MAX_BLOCKED;
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

    /** Whether a hero who can break his shield stands within {@code breakerRange} of the centre (not Creative, not Spectator). */
    static boolean breakerNear(ServerLevel level, Vec3 centre) {
        double range = ApocalypseBossConfig.BREAKER_RANGE.get();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && !player.isCreative() && player.position().distanceToSqr(centre) <= range * range && MindPower.isBreaker(player)) {
                return true;
            }
        }
        return false;
    }
}

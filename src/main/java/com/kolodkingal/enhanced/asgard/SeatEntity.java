package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * An invisible seat (3.5): what a player (or an Asgardian) sits on when he sits on the Throne of Asgard or a bench. It
 * holds its rider where the seat is and goes away as soon as nobody sits on it, or the block it belongs to is gone.
 * Never saved.
 */
public class SeatEntity extends Entity {
    /** The block the seat belongs to (null: none, it stays while ridden). */
    @Nullable
    private BlockPos seatOf;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    /**
     * Seats {@code rider} at {@code at} (the seat's feet; the rider's own feet are a little above), belonging to the block
     * {@code seatOf}. Returns the seat, or null if he could not sit (he rides something already, or the seat is taken).
     */
    @Nullable
    public static SeatEntity sit(LivingEntity rider, Vec3 at, @Nullable BlockPos seatOf) {
        if (!(rider.level() instanceof ServerLevel level) || rider.isPassenger() || taken(level, at)) {
            return null;
        }
        SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
        seat.seatOf = seatOf == null ? null : seatOf.immutable();
        seat.moveTo(at.x, at.y, at.z, rider.getYRot(), 0.0F);
        level.addFreshEntity(seat);
        if (!rider.startRiding(seat, true)) {
            seat.discard();
            return null;
        }
        return seat;
    }

    /** Whether someone sits on a seat at {@code at} already. */
    public static boolean taken(ServerLevel level, Vec3 at) {
        return !level.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(0.3),
                s -> s.isVehicle()).isEmpty();
    }

    @Nullable
    public BlockPos seatOf() {
        return seatOf;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (tickCount > 2 && !isVehicle() || seatOf != null && level().getBlockState(seatOf).isAir()) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return position();
    }

    /** Getting up: in front of the seat if there is room there (or on any side), never inside the throne. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (seatOf == null) {
            return super.getDismountLocationForPassenger(passenger);
        }
        BlockState state = level().getBlockState(seatOf);
        List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction front = state.getValue(HorizontalDirectionalBlock.FACING);
            sides.remove(front);
            sides.add(0, front);
        }
        for (Direction side : sides) {
            Vec3 at = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), seatOf.relative(side), true);
            if (at != null) {
                return at;
            }
        }
        return Vec3.atBottomCenterOf(seatOf).add(0, 1.0, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

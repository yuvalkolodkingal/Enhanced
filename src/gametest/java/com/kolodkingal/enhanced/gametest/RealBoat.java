package com.kolodkingal.enhanced.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Drives a real {@link Boat} entity the way the game's own client drives the boat of a player, tick by tick: the steps of
 * {@code Boat.tick} that the rider's client runs ({@code getStatus}, {@code floatBoat}, {@code controlBoat}, {@code move}), called
 * in that order, with the velocity kept here (the server does not run a boat that has a player in it: it sets its velocity to zero
 * every tick). It is the test's stand-in for a player's client, and the reference the {@link IceBoatSim} is checked against.
 */
final class RealBoat {
    private static final Method GET_STATUS;
    private static final Method FLOAT_BOAT;
    private static final Method CONTROL_BOAT;
    private static final Field STATUS;
    private static final Field OLD_STATUS;
    private static final Field DELTA_ROTATION;

    static {
        try {
            GET_STATUS = Boat.class.getDeclaredMethod("getStatus");
            FLOAT_BOAT = Boat.class.getDeclaredMethod("floatBoat");
            CONTROL_BOAT = Boat.class.getDeclaredMethod("controlBoat");
            STATUS = Boat.class.getDeclaredField("status");
            OLD_STATUS = Boat.class.getDeclaredField("oldStatus");
            DELTA_ROTATION = Boat.class.getDeclaredField("deltaRotation");
            for (java.lang.reflect.AccessibleObject o : new java.lang.reflect.AccessibleObject[]{GET_STATUS, FLOAT_BOAT, CONTROL_BOAT, STATUS, OLD_STATUS, DELTA_ROTATION}) {
                o.setAccessible(true);
            }
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    final Boat boat;
    /** The rider (an armor stand: the boat only drives with somebody in it). */
    final ArmorStand rider;
    Vec3 velocity = Vec3.ZERO;

    RealBoat(ServerLevel level, Vec3 at, float yaw) {
        boat = EntityType.BOAT.create(level);
        boat.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        level.addFreshEntity(boat);
        rider = EntityType.ARMOR_STAND.create(level);
        rider.moveTo(at.x, at.y, at.z);
        level.addFreshEntity(rider);
        rider.startRiding(boat, true);
    }

    /** One tick of the client of the rider, with the keys held. */
    void step(boolean left, boolean right, boolean up, boolean down) {
        try {
            OLD_STATUS.set(boat, STATUS.get(boat));
            STATUS.set(boat, GET_STATUS.invoke(boat));
            boat.setDeltaMovement(velocity);
            boat.setInput(left, right, up, down);
            FLOAT_BOAT.invoke(boat);
            CONTROL_BOAT.invoke(boat);
            boat.move(MoverType.SELF, boat.getDeltaMovement());
            velocity = boat.getDeltaMovement();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    float deltaRotation() {
        try {
            return DELTA_ROTATION.getFloat(boat);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    void setDeltaRotation(float value) {
        try {
            DELTA_ROTATION.setFloat(boat, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Copies the boat's state into a simulation (in the simulation's coordinates: the world's minus the offset). */
    void copyTo(IceBoatSim sim, double offsetX, double offsetY, double offsetZ) {
        sim.x = boat.getX() - offsetX;
        sim.y = boat.getY() - offsetY;
        sim.z = boat.getZ() - offsetZ;
        sim.vx = velocity.x;
        sim.vy = velocity.y;
        sim.vz = velocity.z;
        sim.yaw = boat.getYRot();
        sim.deltaRotation = deltaRotation();
    }

    private RealBoat(Boat boat) {
        this.boat = boat;
        this.rider = null;
    }

    /** The boat that is there already (a trial's, with a player in it): driven from rest. */
    static RealBoat adopt(Boat boat) {
        return new RealBoat(boat);
    }

    void discard() {
        if (rider != null) {
            rider.discard();
        }
        boat.discard();
    }
}

package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Where the parts of a hero really are this frame: the hands, the feet, the shoulders, the forehead and the head of Thor's
 * weapon, as world positions. {@link HandTrackerLayer} (the last layer drawn) records them from the pose stack the body is
 * drawn with, so every particle, ribbon and bolt starts where the model is drawn, whatever the lean, the pose or the
 * weapon's whirl.
 *
 * <p>A point that was not drawn lately (the hero is out of view, culled, or is the local player in first person, whose
 * body is not drawn) is null: callers keep their own approximation then.
 */
public final class HandTracker {
    /** A point drawn longer ago than this many ticks is not where the body is now. */
    private static final float FRESH_TICKS = 2.0F;

    private static final class Points {
        double age = Double.NEGATIVE_INFINITY;
        Vec3 handRight;
        Vec3 handLeft;
        Vec3 footRight;
        Vec3 footLeft;
        Vec3 shoulderRight;
        Vec3 shoulderLeft;
        Vec3 forehead;
        Vec3 weapon;
        double weaponAge = Double.NEGATIVE_INFINITY;
    }

    private static final Map<Integer, Points> POINTS = new HashMap<>();

    /** The middle of the fist of this hand. */
    @Nullable
    public static Vec3 hand(Player player, HumanoidArm arm, float partialTick) {
        Points p = fresh(player, partialTick);
        return p == null ? null : arm == HumanoidArm.RIGHT ? p.handRight : p.handLeft;
    }

    /** The sole of this foot (the right foot is the right leg's). */
    @Nullable
    public static Vec3 foot(Player player, HumanoidArm side, float partialTick) {
        Points p = fresh(player, partialTick);
        return p == null ? null : side == HumanoidArm.RIGHT ? p.footRight : p.footLeft;
    }

    /** The top of this shoulder. */
    @Nullable
    public static Vec3 shoulder(Player player, HumanoidArm side, float partialTick) {
        Points p = fresh(player, partialTick);
        return p == null ? null : side == HumanoidArm.RIGHT ? p.shoulderRight : p.shoulderLeft;
    }

    /** The middle of the forehead, where Vision's Mind Stone sits. */
    @Nullable
    public static Vec3 forehead(Player player, float partialTick) {
        Points p = fresh(player, partialTick);
        return p == null ? null : p.forehead;
    }

    /** The centre of the head of the weapon in Thor's hand, while it is drawn there. */
    @Nullable
    public static Vec3 weaponHead(Player player, float partialTick) {
        Points p = POINTS.get(player.getId());
        if (p == null || Math.abs(player.tickCount + partialTick - p.weaponAge) > FRESH_TICKS) {
            return null;
        }
        return p.weapon;
    }

    @Nullable
    private static Points fresh(Player player, float partialTick) {
        Points p = POINTS.get(player.getId());
        if (p == null || Math.abs(player.tickCount + partialTick - p.age) > FRESH_TICKS) {
            return null;
        }
        return p;
    }

    /** Called by the layer: records the hands, feet, shoulders and forehead of the player drawn with this pose stack. */
    static void record(Player player, float age, Vec3 handRight, Vec3 handLeft, Vec3 footRight, Vec3 footLeft, Vec3 shoulderRight,
                       Vec3 shoulderLeft, Vec3 forehead) {
        Points p = POINTS.computeIfAbsent(player.getId(), id -> new Points());
        p.age = age;
        p.handRight = handRight;
        p.handLeft = handLeft;
        p.footRight = footRight;
        p.footLeft = footLeft;
        p.shoulderRight = shoulderRight;
        p.shoulderLeft = shoulderLeft;
        p.forehead = forehead;
    }

    /**
     * Records where the weapon's head is: a point of the item in centred item coordinates (pixels of the item's model, 8 is
     * the middle), through the pose stack the weapon is drawn with.
     */
    static void recordWeapon(Player player, float age, PoseStack poseStack, Vector3f itemPoint) {
        Points p = POINTS.computeIfAbsent(player.getId(), id -> new Points());
        p.weapon = world(poseStack, (itemPoint.x - 8.0F) / 16.0F, (itemPoint.y - 8.0F) / 16.0F, (itemPoint.z - 8.0F) / 16.0F);
        p.weaponAge = age;
    }

    /** A point of the pose stack's frame in the world (entities are drawn camera-relative with the world's axes). */
    static Vec3 world(PoseStack poseStack, float x, float y, float z) {
        Vector3f v = poseStack.last().pose().transformPosition(x, y, z, new Vector3f());
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return new Vec3(v.x + camera.x, v.y + camera.y, v.z + camera.z);
    }

    /** Forgets the points of players who left the world (a respawn is a new entity: the old points are never read again). */
    public static void prune(ClientLevel level) {
        POINTS.keySet().removeIf(id -> !(level.getEntity(id) instanceof Player));
    }

    public static void clear() {
        POINTS.clear();
    }

    private HandTracker() {
    }
}

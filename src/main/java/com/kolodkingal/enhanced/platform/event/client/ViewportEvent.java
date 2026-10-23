package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;

/** The camera's view (NeoForge's ViewportEvent). */
public abstract class ViewportEvent {
    private final Camera camera;
    private final double partialTick;

    protected ViewportEvent(Camera camera, double partialTick) {
        this.camera = camera;
        this.partialTick = partialTick;
    }

    public Camera getCamera() {
        return camera;
    }

    public double getPartialTick() {
        return partialTick;
    }

    /** The camera's angles: change them (shakes, tilts). */
    public static class ComputeCameraAngles extends ViewportEvent {
        private float yaw;
        private float pitch;
        private float roll;

        public ComputeCameraAngles(Camera camera, double partialTick, float yaw, float pitch, float roll) {
            super(camera, partialTick);
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }

        public float getYaw() {
            return yaw;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public float getRoll() {
            return roll;
        }

        public void setRoll(float roll) {
            this.roll = roll;
        }
    }

    /** The fog's colour: change it. */
    public static class ComputeFogColor extends ViewportEvent {
        private float red;
        private float green;
        private float blue;

        public ComputeFogColor(Camera camera, double partialTick, float red, float green, float blue) {
            super(camera, partialTick);
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public float getRed() {
            return red;
        }

        public void setRed(float red) {
            this.red = red;
        }

        public float getGreen() {
            return green;
        }

        public void setGreen(float green) {
            this.green = green;
        }

        public float getBlue() {
            return blue;
        }

        public void setBlue(float blue) {
            this.blue = blue;
        }
    }

    /** The fog's distances: change them and cancel the event for the change to apply (as on NeoForge). */
    public static class RenderFog extends ViewportEvent implements Cancellable {
        private final FogRenderer.FogMode mode;
        private final FogType type;
        private float farPlaneDistance;
        private float nearPlaneDistance;
        private FogShape fogShape;
        private boolean canceled;

        public RenderFog(
                Camera camera, double partialTick, FogRenderer.FogMode mode, FogType type, float farPlaneDistance,
                float nearPlaneDistance, FogShape fogShape) {
            super(camera, partialTick);
            this.mode = mode;
            this.type = type;
            this.farPlaneDistance = farPlaneDistance;
            this.nearPlaneDistance = nearPlaneDistance;
            this.fogShape = fogShape;
        }

        public FogRenderer.FogMode getMode() {
            return mode;
        }

        public FogType getType() {
            return type;
        }

        public float getFarPlaneDistance() {
            return farPlaneDistance;
        }

        public void setFarPlaneDistance(float farPlaneDistance) {
            this.farPlaneDistance = farPlaneDistance;
        }

        public float getNearPlaneDistance() {
            return nearPlaneDistance;
        }

        public void setNearPlaneDistance(float nearPlaneDistance) {
            this.nearPlaneDistance = nearPlaneDistance;
        }

        public FogShape getFogShape() {
            return fogShape;
        }

        public void setFogShape(FogShape fogShape) {
            this.fogShape = fogShape;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }
}

package com.kolodkingal.enhanced.platform.event.client;

import net.minecraft.client.Camera;

/** How far behind the player the third-person camera stands: change it. */
public class CalculateDetachedCameraDistanceEvent {
    private final Camera camera;
    private final boolean cameraFlipped;
    private final float entityScalingFactor;
    private float distance;

    public CalculateDetachedCameraDistanceEvent(
            Camera camera, boolean cameraFlipped, float entityScalingFactor, float distance) {
        this.camera = camera;
        this.cameraFlipped = cameraFlipped;
        this.entityScalingFactor = entityScalingFactor;
        this.distance = distance;
    }

    public Camera getCamera() {
        return camera;
    }

    public boolean isCameraFlipped() {
        return cameraFlipped;
    }

    public float getEntityScalingFactor() {
        return entityScalingFactor;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }
}

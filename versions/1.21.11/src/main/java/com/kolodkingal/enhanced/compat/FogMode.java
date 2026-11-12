package com.kolodkingal.enhanced.compat;
//#norenames

/**
 * 1.21.11 only: 1.21.1's {@code FogRenderer.FogMode}. From 1.21.6 the level's fog is set up once for the terrain and the
 * sky together (the sky's own end is a field of the fog's data), so the mod's fog event is always {@link #FOG_TERRAIN}.
 */
public enum FogMode {
    FOG_SKY,
    FOG_TERRAIN
}

package com.kolodkingal.enhanced.platform.event;

/**
 * Yes, no, or leave it to the game: NeoForge's {@code TriState}, for the events whose answer has three values
 * ({@code RightClickBlock#setUseItem}, {@code ItemEntityPickupEvent.Pre#setCanPickup}, ...).
 */
public enum TriState {
    TRUE,
    DEFAULT,
    FALSE;

    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isFalse() {
        return this == FALSE;
    }
}

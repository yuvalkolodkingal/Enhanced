package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;

/** The player's input (NeoForge's InputEvent). */
public abstract class InputEvent {
    protected InputEvent() {
    }

    /** A key was pressed, held or let go (GLFW codes). */
    public static class Key extends InputEvent {
        private final int key;
        private final int scanCode;
        private final int action;
        private final int modifiers;

        public Key(int key, int scanCode, int action, int modifiers) {
            super();
            this.key = key;
            this.scanCode = scanCode;
            this.action = action;
            this.modifiers = modifiers;
        }

        public int getKey() {
            return key;
        }

        public int getScanCode() {
            return scanCode;
        }

        public int getAction() {
            return action;
        }

        public int getModifiers() {
            return modifiers;
        }
    }

    /** A mouse button was pressed or let go. */
    public static abstract class MouseButton extends InputEvent {
        private final int button;
        private final int action;
        private final int modifiers;

        protected MouseButton(int button, int action, int modifiers) {
            super();
            this.button = button;
            this.action = action;
            this.modifiers = modifiers;
        }

        public int getButton() {
            return button;
        }

        public int getAction() {
            return action;
        }

        public int getModifiers() {
            return modifiers;
        }

        /** A mouse button, before the game handles it; cancelling swallows it. */
        public static class Pre extends InputEvent.MouseButton implements Cancellable {
            private boolean canceled;

            public Pre(int button, int action, int modifiers) {
                super(button, action, modifiers);
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

        /** A mouse button, after the game handled it. */
        public static class Post extends InputEvent.MouseButton {
            public Post(int button, int action, int modifiers) {
                super(button, action, modifiers);
            }
        }
    }

    /** The mouse wheel turned (in game); cancelling swallows it. */
    public static class MouseScrollingEvent extends InputEvent implements Cancellable {
        private final double scrollDeltaX;
        private final double scrollDeltaY;
        private final boolean leftDown;
        private final boolean middleDown;
        private final boolean rightDown;
        private final double mouseX;
        private final double mouseY;
        private boolean canceled;

        public MouseScrollingEvent(
                double scrollDeltaX, double scrollDeltaY, boolean leftDown, boolean middleDown, boolean rightDown,
                double mouseX, double mouseY) {
            super();
            this.scrollDeltaX = scrollDeltaX;
            this.scrollDeltaY = scrollDeltaY;
            this.leftDown = leftDown;
            this.middleDown = middleDown;
            this.rightDown = rightDown;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public double getScrollDeltaX() {
            return scrollDeltaX;
        }

        public double getScrollDeltaY() {
            return scrollDeltaY;
        }

        public boolean isLeftDown() {
            return leftDown;
        }

        public boolean isMiddleDown() {
            return middleDown;
        }

        public boolean isRightDown() {
            return rightDown;
        }

        public double getMouseX() {
            return mouseX;
        }

        public double getMouseY() {
            return mouseY;
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

    /** The attack, use or pick-block key is about to act; cancelling stops it (setSwingHand for the swing). */
    public static class InteractionKeyMappingTriggered extends InputEvent implements Cancellable {
        private final int button;
        private final KeyMapping keyMapping;
        private final InteractionHand hand;
        private boolean swingHand;
        private boolean canceled;

        public InteractionKeyMappingTriggered(int button, KeyMapping keyMapping, InteractionHand hand) {
            super();
            this.button = button;
            this.keyMapping = keyMapping;
            this.hand = hand;
            this.swingHand = true;
        }

        public int getButton() {
            return button;
        }

        public KeyMapping getKeyMapping() {
            return keyMapping;
        }

        public InteractionHand getHand() {
            return hand;
        }

        public boolean shouldSwingHand() {
            return swingHand;
        }

        public void setSwingHand(boolean swingHand) {
            this.swingHand = swingHand;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        public boolean isAttack() {
            return button == 0;
        }

        public boolean isUseItem() {
            return button == 1;
        }

        public boolean isPickBlock() {
            return button == 2;
        }
    }
}

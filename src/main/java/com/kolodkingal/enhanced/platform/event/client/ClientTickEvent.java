package com.kolodkingal.enhanced.platform.event.client;

/** The client's tick (NeoForge's ClientTickEvent). */
public abstract class ClientTickEvent {
    protected ClientTickEvent() {
    }

    /** Before the client's tick. */
    public static class Pre extends ClientTickEvent {
        public Pre() {
            super();
        }
    }

    /** After the client's tick. */
    public static class Post extends ClientTickEvent {
        public Post() {
            super();
        }
    }
}

package com.kolodkingal.enhanced.compat;
//#norenames

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 26.3 only: an entity has an id only once it is added to a level (1.21.1's had one when it was made). The entities the
 * client makes for drawing only (the Hero Creator's puppet, a shapeshifter's body) get one of their own here: negative,
 * so it is never a real entity's.
 */
public final class ClientIds {
    private static final AtomicInteger NEXT = new AtomicInteger(-1_000_000);

    private ClientIds() {
    }

    public static int next() {
        return NEXT.decrementAndGet();
    }
}

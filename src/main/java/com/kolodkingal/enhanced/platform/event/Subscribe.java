package com.kolodkingal.enhanced.platform.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code public static void} method of a {@link Subscriber} class as a listener of the common event its one
 * parameter is (a type of {@code platform.event} or {@code platform.event.client}). It replaces NeoForge's
 * {@code @SubscribeEvent}, with the same meaning: {@link #priority} as {@code EventPriority}, {@link #receiveCanceled}
 * as there (a listener of a {@link Cancellable} event that is already cancelled is skipped unless it asks for it).
 *
 * <p>Nothing scans for it at run time: the build's annotation processor ({@code processor/}) writes every listener into
 * {@code platform.event.generated.GeneratedSubscriptions}, in the order NeoForge called them in 3.8.2
 * ({@code docs/port/listener-order.txt}), and {@link Events#registerGenerated} registers them with their
 * {@link EventHub}s.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Subscribe {
    Priority priority() default Priority.NORMAL;

    boolean receiveCanceled() default false;
}

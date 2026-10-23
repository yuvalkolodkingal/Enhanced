package com.kolodkingal.enhanced.platform.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class whose {@link Subscribe} methods listen to common events: replaces NeoForge's {@code @EventBusSubscriber}.
 * {@link #side} says where it is registered ({@code value = Dist.CLIENT} becomes {@code side = Side.CLIENT}); there is
 * no bus to name, since every common event has its own {@link EventHub}.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Subscriber {
    Side side() default Side.BOTH;
}

package com.kolodkingal.enhanced.platform.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A GameTest of the mod (port P0, PLAN2 3.5): what {@code @GameTest} with the class's {@code @GameTestHolder} and
 * {@code @PrefixGameTestTemplate(false)} was. The method is {@code public static void name(GameTestHelper helper)}.
 * The test's name is the method's name in lower case; its structure is {@code enhanced:<template>}. {@link EnhancedTests}
 * turns every such method into a test on each loader.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnhancedTest {
    /** The structure, in the mod's namespace ({@code arena}, {@code sokovia_pad}...). */
    String template();

    /** The batch: tests of one batch run together, batches one after the other ("" is the default batch). */
    String batch() default "";

    int timeoutTicks() default 100;

    long setupTicks() default 0L;

    boolean required() default true;
}

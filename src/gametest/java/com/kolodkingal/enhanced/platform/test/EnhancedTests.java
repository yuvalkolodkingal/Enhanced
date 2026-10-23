package com.kolodkingal.enhanced.platform.test;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * The r1 (Minecraft 1.21.1) GameTest adapter (port P0, PLAN2 3.5): one {@link GameTestGenerator} that turns every
 * {@link EnhancedTest} of {@link TestIndex} into the {@link TestFunction} the game's {@code @GameTest} registration made
 * of it before the port (same name, structure, batch, timeout, order), keeps those {@link TestFilter} accepts, and
 * installs the {@link ParityReporter}. Each loader registers this class (NeoForge: {@code RegisterGameTestsEvent}).
 */
public final class EnhancedTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The game's name of the default batch ({@code @GameTest}'s default). */
    public static final String DEFAULT_BATCH = "defaultBatch";

    @GameTestGenerator
    public static Collection<TestFunction> enhancedTests() {
        List<TestIndex.Entry> entries = TestIndex.load();
        TestFilter filter = TestFilter.fromSystem();
        ParityReporter reporter = new ParityReporter(ParityReporter.folder(), TestHooks.get().loaderName());
        List<TestFunction> functions = new ArrayList<>();
        List<TestIndex.Entry> chosen = new ArrayList<>();
        for (TestIndex.Entry entry : entries) {
            if (filter.accepts(entry)) {
                TestFunction function = function(entry);
                functions.add(function);
                chosen.add(entry);
                reporter.add(function, entry.id());
            }
        }
        LOGGER.info("Enhanced GameTests: {} of {} tests (filter {})", functions.size(), entries.size(), filter);
        ParityReporter.writeIndex(ParityReporter.folder(), chosen);
        GlobalTestReporter.replaceWith(reporter);
        return functions;
    }

    /** A test's batch name in the game ("" is the default batch). */
    static String batch(EnhancedTest test) {
        return test.batch().isEmpty() ? DEFAULT_BATCH : test.batch();
    }

    /** What {@code GameTestRegistry#turnMethodIntoTestFunction} made of the test's {@code @GameTest}. */
    static TestFunction function(TestIndex.Entry entry) {
        EnhancedTest test = entry.test();
        String name = entry.name();
        String structure = Enhanced.MODID + ":" + (test.template().isEmpty() ? name : test.template());
        return new TestFunction(batch(test), name, structure, StructureUtils.getRotationForRotationSteps(0),
                test.timeoutTicks(), test.setupTicks(), test.required(), false, 1, 1, false, consumer(entry.method()));
    }

    private static Consumer<GameTestHelper> consumer(Method method) {
        return helper -> {
            try {
                method.invoke(null, helper);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new RuntimeException(e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private EnhancedTests() {
    }
}

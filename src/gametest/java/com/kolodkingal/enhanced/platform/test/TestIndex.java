package com.kolodkingal.enhanced.platform.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The mod's GameTests: every {@link EnhancedTest} method, in a fixed order (port P0, PLAN2 3.5).
 *
 * <p>The test classes come from {@code META-INF/enhanced-tests.json} ({@code {"classes": ["...", ...]}}, written at
 * build time once the processor does so) or else from the loader ({@link TestHooks#testClasses()}: NeoForge's scan).
 * The order of the tests is the one NeoForge's own {@code @GameTestHolder} registration gave them: every declared
 * method of every test class, class by class in that order, in one {@link HashSet} whose iteration order is the
 * tests' order. Batches of more than 50 tests are split in that order, so keeping it keeps every batch as it was.
 */
public final class TestIndex {
    /** One test: its class, method and annotation. */
    public record Entry(Class<?> testClass, Method method, EnhancedTest test) {
        /** {@code SimpleClassName.methodName}: the test's id in reports and filters. */
        public String id() {
            return testClass.getSimpleName() + "." + method.getName();
        }

        /** The test's name in the game (its method's name in lower case, as before). */
        public String name() {
            return method.getName().toLowerCase();
        }
    }

    /** The resource the processor may write (one day) with the test classes. */
    public static final String RESOURCE = "META-INF/enhanced-tests.json";

    /** Every test of the mod, in order. */
    public static List<Entry> load() {
        List<Class<?>> classes = classesFromResource();
        if (classes.isEmpty()) {
            classes = TestHooks.get().testClasses();
        }
        return of(classes);
    }

    /** Every test of {@code classes}, in order. */
    public static List<Entry> of(List<Class<?>> classes) {
        Set<Method> methods = new HashSet<>();
        for (Class<?> testClass : classes) {
            methods.addAll(Arrays.asList(testClass.getDeclaredMethods()));
        }
        List<Entry> entries = new ArrayList<>();
        for (Method method : methods) {
            EnhancedTest test = method.getAnnotation(EnhancedTest.class);
            if (test != null) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalStateException("A test must be static: " + method);
                }
                entries.add(new Entry(method.getDeclaringClass(), method, test));
            }
        }
        return entries;
    }

    private static List<Class<?>> classesFromResource() {
        List<Class<?>> classes = new ArrayList<>();
        try (InputStream in = TestIndex.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return classes;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray names = json.getAsJsonArray("classes");
                for (JsonElement name : names) {
                    classes.add(Class.forName(name.getAsString(), true, TestIndex.class.getClassLoader()));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
        return classes;
    }

    private TestIndex() {
    }
}

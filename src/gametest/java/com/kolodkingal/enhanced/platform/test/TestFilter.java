package com.kolodkingal.enhanced.platform.test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * {@code -Denhanced.tests=<globs>} (port P0, PLAN2 3.5): only the tests whose class or {@code Class.method} matches one
 * of the comma-separated globs run ({@code *} any text, {@code ?} one character; case does not matter), so a lane runs
 * just its own classes, such as {@code -Denhanced.tests=Captain*,FalconTests.redwing*}. With Gradle:
 * {@code ./gradlew runGameTestServer -Penhanced.tests=Captain*}. Unset or empty: every test.
 */
public final class TestFilter {
    public static final String PROPERTY = "enhanced.tests";

    private final List<String> globs = new ArrayList<>();
    private final List<Pattern> patterns = new ArrayList<>();

    private TestFilter(String globs) {
        for (String glob : globs.split(",")) {
            String trimmed = glob.trim();
            if (!trimmed.isEmpty()) {
                this.globs.add(trimmed);
                patterns.add(Pattern.compile(toRegex(trimmed), Pattern.CASE_INSENSITIVE));
            }
        }
    }

    /** The filter the system property asks for. */
    public static TestFilter fromSystem() {
        String globs = System.getProperty(PROPERTY);
        return new TestFilter(globs == null ? "" : globs);
    }

    public static TestFilter of(String globs) {
        return new TestFilter(globs);
    }

    /** Whether every test runs. */
    public boolean all() {
        return patterns.isEmpty();
    }

    public boolean accepts(TestIndex.Entry entry) {
        if (patterns.isEmpty()) {
            return true;
        }
        String className = entry.testClass().getSimpleName();
        String id = entry.id();
        for (Pattern pattern : patterns) {
            if (pattern.matcher(className).matches() || pattern.matcher(id).matches()
                    || pattern.matcher(entry.testClass().getName()).matches()) {
                return true;
            }
        }
        return false;
    }

    private static String toRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                default -> regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return regex.toString();
    }

    @Override
    public String toString() {
        return patterns.isEmpty() ? "all" : String.join(",", globs);
    }
}

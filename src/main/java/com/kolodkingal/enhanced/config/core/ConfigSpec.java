package com.kolodkingal.enhanced.config.core;

import com.kolodkingal.enhanced.Enhanced;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The mod's config specs (port P0, PLAN2 3.3): what a config file holds, in sections, each value with its comment,
 * default and range. The builder methods and value types are those of NeoForge's {@code ModConfigSpec}, which the
 * config classes were written for, and they behave as it did: the same comments ({@code # Default:},
 * {@code # Range:}, {@code Allowed Values:}), the same tests of a value and the same corrections (a number out of its
 * range is clamped, anything else wrong becomes the default), the same cache of the read value.
 *
 * <p>Since lane L7 the backend is the mod's own ({@link Toml}, {@link ConfigFile}, {@link Configs}): no loader keeps
 * the files. A value is read from the loaded file ({@link #isLoaded()}); before that {@code get()} throws, as
 * NeoForge's did, so code that may run first asks {@code SPEC.isLoaded()}.
 */
public final class ConfigSpec {
    /** The value specs ({@link ValueSpec}) and sections (maps), in the order they were defined. */
    private final Map<String, Object> spec;
    private final Map<List<String>, String> levelComments;
    private final Map<List<String>, String> levelTranslationKeys;
    private final List<ConfigValue<?>> values;
    /** The loaded file's values (null: not loaded). */
    @Nullable
    private volatile Toml.Table loaded;
    /** Where {@link #save()} writes (null: a config not read from a file, such as the server's on a client). */
    @Nullable
    private volatile ConfigFile file;

    private ConfigSpec(Map<String, Object> spec, Map<List<String>, String> levelComments,
                       Map<List<String>, String> levelTranslationKeys, List<ConfigValue<?>> values) {
        this.spec = spec;
        this.levelComments = levelComments;
        this.levelTranslationKeys = levelTranslationKeys;
        this.values = values;
    }

    /** Whether the config file has been read (before that, values are their defaults and must not be relied on). */
    public boolean isLoaded() {
        return loaded != null;
    }

    /** Writes the config file (its values as they are now). */
    public void save() {
        Toml.Table table = Objects.requireNonNull(loaded,
                "Cannot save config value without assigned Config object present");
        ConfigFile target = file;
        if (target != null) {
            target.save(table);
        }
    }

    /** The value specs by path (each a {@link ValueSpec}; {@code null} if none). */
    public Specs getSpec() {
        return new Specs();
    }

    /** Every value of this config, in the order they were defined. */
    public List<ConfigValue<?>> values() {
        return values;
    }

    /** The names of the top-level sections ({@code push}), in the spec's order. */
    public List<String> sections() {
        List<String> sections = new ArrayList<>();
        for (Map.Entry<String, Object> entry : spec.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?>) {
                sections.add(entry.getKey());
            }
        }
        return sections;
    }

    /** The entries (value specs and section maps) of the section at {@code path} ({@code List.of()}: the top). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> entries(List<String> path) {
        Map<String, Object> level = spec;
        for (String key : path) {
            if (!(level.get(key) instanceof Map<?, ?> sub)) {
                return Map.of();
            }
            level = (Map<String, Object>) sub;
        }
        return Collections.unmodifiableMap(level);
    }

    /** The translation key of the section at {@code path}, or null if it has none. */
    @Nullable
    public String getLevelTranslationKey(List<String> path) {
        return levelTranslationKeys.get(path);
    }

    /** The comment of the section at {@code path}, or null if it has none. */
    @Nullable
    public String getLevelComment(List<String> path) {
        return levelComments.get(path);
    }

    /** The value at {@code path}, or null. */
    @Nullable
    public ConfigValue<?> value(List<String> path) {
        for (ConfigValue<?> value : values) {
            if (value.path.equals(path)) {
                return value;
            }
        }
        return null;
    }

    // ---- loading (ConfigFile) ----

    /** The values now in use (null: not loaded); changed by {@link ConfigValue#set}. */
    @Nullable
    Toml.Table loaded() {
        return loaded;
    }

    /**
     * Puts {@code table} in use (null: unloads), as NeoForge's {@code acceptConfig}: a table that is not correct is
     * corrected (with a warning per value) and saved; every cached value is read again.
     */
    void accept(@Nullable Toml.Table table, @Nullable ConfigFile from) {
        this.file = from;
        this.loaded = table;
        if (table != null && !isCorrect(table)) {
            Enhanced.LOGGER.warn("Configuration {} is not correct. Correcting",
                    from == null ? "(synced)" : from.fileName());
            correct(table, ConfigFile::warnCorrected, null);
            save();
        }
        afterReload();
    }

    /** Forgets the read values of every option that needs no restart (they are read again when asked). */
    public void afterReload() {
        for (ConfigValue<?> value : values) {
            if (value.getSpec().restartType() == RestartType.NONE) {
                value.clearCache();
            }
        }
    }

    /** The default file's values: every value at its default, every comment from the spec. */
    public Toml.Table defaults() {
        Toml.Table table = new Toml.Table();
        correct(table);
        return table;
    }

    /** What a correction did to one value: added, replaced or removed it. */
    public enum CorrectionAction {
        ADD, REPLACE, REMOVE
    }

    /** Told of each value a correction changes ({@code path} is only valid during the call). */
    @FunctionalInterface
    public interface CorrectionListener {
        void onCorrect(CorrectionAction action, List<String> path, @Nullable Object incorrectValue,
                       @Nullable Object correctedValue);
    }

    /** Whether {@code table} has every value of the spec, each acceptable, with its comment, and nothing else. */
    public boolean isCorrect(Toml.Table table) {
        LinkedList<String> path = new LinkedList<>();
        return correct(spec, table, path, Collections.unmodifiableList(path), (a, p, i, c) -> {
        }, null, true) == 0;
    }

    /** Makes {@code table} correct: missing values added, wrong ones corrected, unknown ones removed, comments set. */
    public void correct(Toml.Table table) {
        correct(table, (a, p, i, c) -> {
        }, null);
    }

    /** The same, telling {@code listener} of each value and {@code commentListener} of each comment changed. */
    public int correct(Toml.Table table, CorrectionListener listener, @Nullable CorrectionListener commentListener) {
        LinkedList<String> path = new LinkedList<>();
        return correct(spec, table, path, Collections.unmodifiableList(path), listener, commentListener, false);
    }

    @SuppressWarnings("unchecked")
    private int correct(Map<String, Object> specLevel, Toml.Table table, LinkedList<String> parentPath,
                        List<String> parentPathView, CorrectionListener listener,
                        @Nullable CorrectionListener commentListener, boolean dryRun) {
        int count = 0;
        Map<String, Object> tableMap = table.values();
        for (Map.Entry<String, Object> specEntry : specLevel.entrySet()) {
            String key = specEntry.getKey();
            Object specValue = specEntry.getValue();
            Object tableValue = tableMap.get(key);
            CorrectionAction action = tableValue == null ? CorrectionAction.ADD : CorrectionAction.REPLACE;
            parentPath.addLast(key);
            if (specValue instanceof Map<?, ?> subSpec) {
                if (tableValue instanceof Toml.Table subTable) {
                    count += correct((Map<String, Object>) subSpec, subTable, parentPath, parentPathView, listener,
                            commentListener, dryRun);
                    if (count > 0 && dryRun) {
                        return count;
                    }
                } else if (dryRun) {
                    return 1;
                } else {
                    Toml.Table newTable = new Toml.Table();
                    tableMap.put(key, newTable);
                    listener.onCorrect(action, parentPathView, tableValue, newTable);
                    count++;
                    count += correct((Map<String, Object>) subSpec, newTable, parentPath, parentPathView, listener,
                            commentListener, false);
                }
                String newComment = levelComments.get(parentPath);
                String oldComment = table.comment(key);
                if (!commentsMatch(oldComment, newComment)) {
                    if (commentListener != null) {
                        commentListener.onCorrect(action, parentPathView, oldComment, newComment);
                    }
                    if (dryRun) {
                        return 1;
                    }
                    table.setComment(key, newComment);
                }
            } else {
                ValueSpec valueSpec = (ValueSpec) specValue;
                if (!valueSpec.test(tableValue)) {
                    if (dryRun) {
                        return 1;
                    }
                    Object newValue = valueSpec.correct(tableValue);
                    tableMap.put(key, newValue);
                    listener.onCorrect(action, parentPathView, tableValue, newValue);
                    count++;
                }
                String oldComment = table.comment(key);
                if (!commentsMatch(oldComment, valueSpec.getComment())) {
                    if (commentListener != null) {
                        commentListener.onCorrect(action, parentPathView, oldComment, valueSpec.getComment());
                    }
                    if (dryRun) {
                        return 1;
                    }
                    table.setComment(key, valueSpec.getComment());
                }
            }
            parentPath.removeLast();
        }
        // then the values the spec does not know are removed
        for (Iterator<Map.Entry<String, Object>> it = tableMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Object> entry = it.next();
            if (!specLevel.containsKey(entry.getKey())) {
                if (dryRun) {
                    return 1;
                }
                it.remove();
                parentPath.addLast(entry.getKey());
                listener.onCorrect(CorrectionAction.REMOVE, parentPathView, entry.getValue(), null);
                parentPath.removeLast();
                count++;
            }
        }
        return count;
    }

    private static boolean commentsMatch(@Nullable String a, @Nullable String b) {
        boolean blankA = a == null || a.isBlank();
        boolean blankB = b == null || b.isBlank();
        if (blankA != blankB) {
            return false;
        }
        if (blankA) {
            return true;
        }
        return a.replace("\r\n", "\n").equals(b.replace("\r\n", "\n"));
    }

    /** The value specs of this config. */
    public final class Specs {
        /** The spec of the value at {@code path}, or null (a section, or nothing). */
        @SuppressWarnings("unchecked")
        public <T> T get(List<String> path) {
            Object level = spec;
            for (String key : path) {
                if (!(level instanceof Map<?, ?> map)) {
                    return null;
                }
                level = map.get(key);
            }
            return (T) (level instanceof ValueSpec value ? value : null);
        }
    }

    /** When a change of a value takes effect: at once, when the world is loaded again, or when the game restarts. */
    public enum RestartType {
        NONE, WORLD, GAME;

        public RestartType with(RestartType other) {
            return other == NONE ? this : (other == GAME || this == GAME) ? GAME : WORLD;
        }
    }

    /** The bounds of a number. */
    public static final class Range<V extends Comparable<? super V>> implements Predicate<Object> {
        private final Class<? extends V> clazz;
        private final V min;
        private final V max;

        Range(Class<V> clazz, V min, V max) {
            this.clazz = clazz;
            this.min = min;
            this.max = max;
            if (min.compareTo(max) > 0) {
                throw new IllegalArgumentException("Range min must be less then max.");
            }
        }

        public static Range<Integer> of(int min, int max) {
            return new Range<>(Integer.class, min, max);
        }

        public Class<? extends V> getClazz() {
            return clazz;
        }

        public V getMin() {
            return min;
        }

        public V getMax() {
            return max;
        }

        private boolean isNumber(@Nullable Object other) {
            return Number.class.isAssignableFrom(clazz) && other instanceof Number;
        }

        @Override
        public boolean test(@Nullable Object t) {
            if (isNumber(t)) {
                double n = ((Number) t).doubleValue();
                return ((Number) min).doubleValue() <= n && n <= ((Number) max).doubleValue();
            }
            if (!clazz.isInstance(t)) {
                return false;
            }
            V c = clazz.cast(t);
            return c.compareTo(min) >= 0 && c.compareTo(max) <= 0;
        }

        public Object correct(@Nullable Object value, Object def) {
            if (isNumber(value)) {
                double n = ((Number) value).doubleValue();
                return n < ((Number) min).doubleValue() ? min : n > ((Number) max).doubleValue() ? max : value;
            }
            if (!clazz.isInstance(value)) {
                return def;
            }
            V c = clazz.cast(value);
            return c.compareTo(min) < 0 ? min : c.compareTo(max) > 0 ? max : value;
        }

        @Override
        public String toString() {
            if (clazz == Integer.class) {
                if (max.equals(Integer.MAX_VALUE)) {
                    return "> " + min;
                } else if (min.equals(Integer.MIN_VALUE)) {
                    return "< " + max;
                }
            }
            return min + " ~ " + max;
        }
    }

    /** What a value accepts, its comment and its default. */
    public static class ValueSpec {
        @Nullable
        private final String comment;
        @Nullable
        private final String langKey;
        @Nullable
        private final Range<?> range;
        @Nullable
        private final Class<?> clazz;
        private final Supplier<?> supplier;
        private final Predicate<Object> validator;
        private final RestartType restartType;

        ValueSpec(Supplier<?> supplier, Predicate<Object> validator, BuilderContext context, List<String> path) {
            Objects.requireNonNull(supplier, "Default supplier can not be null");
            Objects.requireNonNull(validator, "Validator can not be null");
            this.comment = context.hasComment() ? context.buildComment(path) : null;
            this.langKey = context.getTranslationKey();
            this.range = context.getRange();
            this.restartType = context.restartType();
            this.clazz = context.getClazz();
            this.supplier = supplier;
            this.validator = validator;
        }

        @Nullable
        public String getComment() {
            return comment;
        }

        @Nullable
        public String getTranslationKey() {
            return langKey;
        }

        @Nullable
        @SuppressWarnings("unchecked")
        public <V extends Comparable<? super V>> Range<V> getRange() {
            return (Range<V>) range;
        }

        public RestartType restartType() {
            return restartType;
        }

        @Nullable
        public Class<?> getClazz() {
            return clazz;
        }

        /** Whether {@code value} is acceptable (type and range). */
        public boolean test(@Nullable Object value) {
            return validator.test(value);
        }

        /** What {@code value} becomes when it is not acceptable: clamped into the range, else the default. */
        public Object correct(@Nullable Object value) {
            return range == null ? getDefault() : range.correct(value, getDefault());
        }

        public Object getDefault() {
            return supplier.get();
        }
    }

    /** The spec of a list: what each element accepts, how many there may be, and what a new one is. */
    public static class ListValueSpec extends ValueSpec {
        private static final Range<Integer> MAX_ELEMENTS = Range.of(0, Integer.MAX_VALUE);
        private static final Range<Integer> NON_EMPTY = Range.of(1, Integer.MAX_VALUE);
        @Nullable
        private final Supplier<?> newElementSupplier;
        private final Range<Integer> sizeRange;
        private final Predicate<Object> elementValidator;

        ListValueSpec(Supplier<?> supplier, @Nullable Supplier<?> newElementSupplier, Predicate<Object> listValidator,
                      Predicate<Object> elementValidator, BuilderContext context, List<String> path,
                      @Nullable Range<Integer> sizeRange) {
            super(supplier, listValidator, context, path);
            this.newElementSupplier = newElementSupplier;
            this.elementValidator = Objects.requireNonNull(elementValidator, "ElementValidator can not be null");
            this.sizeRange = Objects.requireNonNullElse(sizeRange, MAX_ELEMENTS);
        }

        @Nullable
        public Supplier<?> getNewElementSupplier() {
            return newElementSupplier;
        }

        public boolean testElement(Object value) {
            return elementValidator.test(value);
        }

        public Range<Integer> getSizeRange() {
            return sizeRange;
        }

        @Override
        public Object correct(@Nullable Object value) {
            if (!(value instanceof List<?> list) || !sizeRange.test(list.size())) {
                return getDefault();
            }
            List<Object> kept = new ArrayList<>(list);
            kept.removeIf(elementValidator.negate());
            if (kept.isEmpty()) {
                return getDefault();
            }
            return kept;
        }
    }

    // ---- values ----

    /** A config value: read with {@link #get()}. */
    public static class ConfigValue<T> implements Supplier<T> {
        private final List<String> path;
        private final Supplier<T> defaultSupplier;
        @Nullable
        private ConfigSpec spec;
        @Nullable
        private volatile T cachedValue;

        ConfigValue(Builder parent, List<String> path, Supplier<T> defaultSupplier) {
            this.path = path;
            this.defaultSupplier = defaultSupplier;
            parent.values.add(this);
        }

        /** The value (read once from the loaded file, then kept until the file is read again). */
        @Override
        public T get() {
            T value = cachedValue;
            if (value == null) {
                value = getRaw();
                cachedValue = value;
            }
            return value;
        }

        /** The value in the loaded file, read now (no cache). */
        public T getRaw() {
            ConfigSpec built = Objects.requireNonNull(spec, "Cannot get config value before spec is built");
            Toml.Table table = built.loaded;
            if (table == null) {
                throw new IllegalStateException("Cannot get config value before config is loaded.");
            }
            return read(table);
        }

        /** This value as read from {@code table} (the default if it is not there). */
        @SuppressWarnings("unchecked")
        public T read(Toml.Table table) {
            Object raw = table.get(path);
            return raw == null ? defaultSupplier.get() : (T) raw;
        }

        /** The value in {@code table} as it is stored (null if none). */
        @Nullable
        final Object stored(Toml.Table table) {
            return table.get(path);
        }

        public T getDefault() {
            return defaultSupplier.get();
        }

        /** Changes the value (not yet written to the file: {@link #save()}). */
        public void set(T newValue) {
            ConfigSpec built = Objects.requireNonNull(spec, "Cannot set config value before spec is built");
            Toml.Table table = Objects.requireNonNull(built.loaded,
                    "Cannot set config value without assigned Config object present");
            table.set(path, newValue);
            if (getSpec().restartType() == RestartType.NONE) {
                cachedValue = newValue;
            }
        }

        /** Writes the config file. */
        public void save() {
            ConfigSpec built = Objects.requireNonNull(spec, "Cannot save config value before spec is built");
            Objects.requireNonNull(built.loaded, "Cannot save config value without assigned Config object present");
            built.save();
        }

        public void clearCache() {
            cachedValue = null;
        }

        /** Its path in the file: its sections' names and its own. */
        public List<String> getPath() {
            return new ArrayList<>(path);
        }

        public ValueSpec getSpec() {
            ConfigSpec built = Objects.requireNonNull(spec, "Cannot get the spec of a value before it is built");
            return built.getSpec().get(path);
        }

        /** The spec this value belongs to. */
        public ConfigSpec owner() {
            return Objects.requireNonNull(spec, "Cannot get the spec of a value before it is built");
        }
    }

    public static class BooleanValue extends ConfigValue<Boolean> implements BooleanSupplier {
        BooleanValue(Builder parent, List<String> path, Supplier<Boolean> defaultSupplier) {
            super(parent, path, defaultSupplier);
        }

        /**
         * The value as read from {@code table}. "true" and "false" written as strings (which the spec accepts, as
         * NeoForge's did) are read as booleans; NeoForge's backend handed out the string and its reader failed.
         */
        @Override
        public Boolean read(Toml.Table table) {
            Object raw = stored(table);
            if (raw instanceof String string) {
                return Boolean.parseBoolean(string);
            }
            return raw == null ? getDefault() : (Boolean) raw;
        }

        @Override
        public boolean getAsBoolean() {
            return get();
        }

        public boolean isTrue() {
            return getAsBoolean();
        }

        public boolean isFalse() {
            return !getAsBoolean();
        }
    }

    public static class IntValue extends ConfigValue<Integer> implements IntSupplier {
        IntValue(Builder parent, List<String> path, Supplier<Integer> defaultSupplier) {
            super(parent, path, defaultSupplier);
        }

        @Override
        public Integer read(Toml.Table table) {
            Number n = (Number) stored(table);
            return n == null ? getDefault() : n.intValue();
        }

        @Override
        public int getAsInt() {
            return get();
        }
    }

    public static class LongValue extends ConfigValue<Long> implements LongSupplier {
        LongValue(Builder parent, List<String> path, Supplier<Long> defaultSupplier) {
            super(parent, path, defaultSupplier);
        }

        @Override
        public Long read(Toml.Table table) {
            Number n = (Number) stored(table);
            return n == null ? getDefault() : n.longValue();
        }

        @Override
        public long getAsLong() {
            return get();
        }
    }

    public static class DoubleValue extends ConfigValue<Double> implements DoubleSupplier {
        DoubleValue(Builder parent, List<String> path, Supplier<Double> defaultSupplier) {
            super(parent, path, defaultSupplier);
        }

        @Override
        public Double read(Toml.Table table) {
            Number n = (Number) stored(table);
            return n == null ? getDefault() : n.doubleValue();
        }

        @Override
        public double getAsDouble() {
            return get();
        }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        private final Class<T> clazz;

        EnumValue(Builder parent, List<String> path, Supplier<T> defaultSupplier, Class<T> clazz) {
            super(parent, path, defaultSupplier);
            this.clazz = clazz;
        }

        /** The constant named in {@code table} (in any case), or the default. */
        @Override
        public T read(Toml.Table table) {
            Object raw = stored(table);
            if (raw == null) {
                return getDefault();
            }
            if (clazz.isInstance(raw)) {
                return clazz.cast(raw);
            }
            if (raw instanceof String name) {
                for (T constant : clazz.getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(name)) {
                        return constant;
                    }
                }
                throw new IllegalArgumentException("No enum constant " + clazz.getCanonicalName() + "." + name);
            }
            throw new ClassCastException("Cannot convert a value of type " + raw.getClass().getCanonicalName()
                    + " to an Enum");
        }

        public Class<T> enumClass() {
            return clazz;
        }
    }

    // ---- the builder ----

    /** Builds a spec: sections ({@link #push}/{@link #pop}), comments, and values. */
    public static class Builder {
        private final Map<String, Object> spec = new LinkedHashMap<>();
        private BuilderContext context = new BuilderContext();
        private final Map<List<String>, String> levelComments = new HashMap<>();
        private final Map<List<String>, String> levelTranslationKeys = new HashMap<>();
        private final List<String> currentPath = new ArrayList<>();
        private final List<ConfigValue<?>> values = new ArrayList<>();

        public Builder comment(String comment) {
            context.addComment(comment);
            return this;
        }

        public Builder comment(String... comment) {
            for (int i = 0; i < comment.length; i++) {
                Objects.requireNonNull(comment[i], "Comment string at " + i + " is null.");
            }
            for (String line : comment) {
                context.addComment(line);
            }
            return this;
        }

        public Builder translation(String translationKey) {
            context.setTranslationKey(translationKey);
            return this;
        }

        public Builder worldRestart() {
            context.restartType = RestartType.WORLD;
            return this;
        }

        public Builder gameRestart() {
            context.restartType = RestartType.GAME;
            return this;
        }

        public Builder push(String path) {
            return push(split(path));
        }

        public Builder push(List<String> path) {
            currentPath.addAll(path);
            if (context.hasComment()) {
                levelComments.put(new ArrayList<>(currentPath), context.buildComment(path));
                context.clearComment();
            }
            if (context.getTranslationKey() != null) {
                levelTranslationKeys.put(new ArrayList<>(currentPath), context.getTranslationKey());
                context.setTranslationKey(null);
            }
            context.ensureEmpty();
            return this;
        }

        public Builder pop() {
            return pop(1);
        }

        public Builder pop(int count) {
            if (count > currentPath.size()) {
                throw new IllegalArgumentException("Attempted to pop " + count + " elements when we only had: "
                        + currentPath);
            }
            for (int x = 0; x < count; x++) {
                currentPath.remove(currentPath.size() - 1);
            }
            return this;
        }

        public ConfigSpec build() {
            context.ensureEmpty();
            ConfigSpec built = new ConfigSpec(spec, Collections.unmodifiableMap(levelComments),
                    Collections.unmodifiableMap(levelTranslationKeys), List.copyOf(values));
            values.forEach(v -> v.spec = built);
            return built;
        }

        /** The root of every define: puts the spec at the full path and makes a plain value for it. */
        @SuppressWarnings("unchecked")
        private <T> ConfigValue<T> define(List<String> path, ValueSpec value, Supplier<T> defaultSupplier) {
            List<String> full = path;
            if (!currentPath.isEmpty()) {
                full = new ArrayList<>(currentPath.size() + path.size());
                full.addAll(currentPath);
                full.addAll(path);
            }
            Map<String, Object> level = spec;
            for (int i = 0; i < full.size() - 1; i++) {
                Object sub = level.get(full.get(i));
                if (!(sub instanceof Map<?, ?>)) {
                    sub = new LinkedHashMap<String, Object>();
                    level.put(full.get(i), sub);
                }
                level = (Map<String, Object>) sub;
            }
            level.put(full.get(full.size() - 1), value);
            context = new BuilderContext();
            return new ConfigValue<>(this, full, defaultSupplier);
        }

        /** Defines the spec of a typed value: its full path, for the typed value made next. */
        private List<String> typedPath(ConfigValue<?> plain) {
            values.remove(plain);
            return plain.path;
        }

        // -- define --

        public <T> ConfigValue<T> define(String path, T defaultValue) {
            return define(split(path), defaultValue);
        }

        public <T> ConfigValue<T> define(List<String> path, T defaultValue) {
            return define(path, defaultValue, o -> o != null && defaultValue.getClass().isAssignableFrom(o.getClass()));
        }

        public <T> ConfigValue<T> define(String path, T defaultValue, Predicate<Object> validator) {
            return define(split(path), defaultValue, validator);
        }

        public <T> ConfigValue<T> define(List<String> path, T defaultValue, Predicate<Object> validator) {
            Objects.requireNonNull(defaultValue, "Default value can not be null");
            return define(path, () -> defaultValue, validator);
        }

        public <T> ConfigValue<T> define(String path, Supplier<T> defaultSupplier, Predicate<Object> validator) {
            return define(split(path), defaultSupplier, validator);
        }

        public <T> ConfigValue<T> define(List<String> path, Supplier<T> defaultSupplier, Predicate<Object> validator) {
            return define(path, defaultSupplier, validator, Object.class);
        }

        public <T> ConfigValue<T> define(List<String> path, Supplier<T> defaultSupplier, Predicate<Object> validator,
                                         Class<?> clazz) {
            context.setClazz(clazz);
            return define(path, new ValueSpec(defaultSupplier, validator, context, path), defaultSupplier);
        }

        public BooleanValue define(String path, boolean defaultValue) {
            return define(split(path), defaultValue);
        }

        public BooleanValue define(List<String> path, boolean defaultValue) {
            return define(path, (Supplier<Boolean>) () -> defaultValue);
        }

        public BooleanValue define(String path, Supplier<Boolean> defaultSupplier) {
            return define(split(path), defaultSupplier);
        }

        public BooleanValue define(List<String> path, Supplier<Boolean> defaultSupplier) {
            ConfigValue<Boolean> plain = define(path, defaultSupplier, o -> {
                if (o instanceof String string) {
                    return string.equalsIgnoreCase("true") || string.equalsIgnoreCase("false");
                }
                return o instanceof Boolean;
            }, Boolean.class);
            return new BooleanValue(this, typedPath(plain), defaultSupplier);
        }

        // -- defineInRange --

        public <V extends Comparable<? super V>> ConfigValue<V> defineInRange(String path, V defaultValue, V min, V max,
                                                                              Class<V> clazz) {
            return defineInRange(split(path), defaultValue, min, max, clazz);
        }

        public <V extends Comparable<? super V>> ConfigValue<V> defineInRange(List<String> path, V defaultValue,
                                                                              V min, V max, Class<V> clazz) {
            return defineInRange(path, (Supplier<V>) () -> defaultValue, min, max, clazz);
        }

        public <V extends Comparable<? super V>> ConfigValue<V> defineInRange(List<String> path,
                                                                              Supplier<V> defaultSupplier, V min,
                                                                              V max, Class<V> clazz) {
            Range<V> range = new Range<>(clazz, min, max);
            context.setRange(range);
            comment(" Default: " + defaultSupplier.get());
            comment(" Range: " + range);
            return define(path, defaultSupplier, range);
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            return defineInRange(split(path), defaultValue, min, max);
        }

        public DoubleValue defineInRange(List<String> path, double defaultValue, double min, double max) {
            return defineInRange(path, (Supplier<Double>) () -> defaultValue, min, max);
        }

        public DoubleValue defineInRange(String path, Supplier<Double> defaultSupplier, double min, double max) {
            return defineInRange(split(path), defaultSupplier, min, max);
        }

        public DoubleValue defineInRange(List<String> path, Supplier<Double> defaultSupplier, double min,
                                         double max) {
            ConfigValue<Double> plain = defineInRange(path, defaultSupplier, min, max, Double.class);
            return new DoubleValue(this, typedPath(plain), defaultSupplier);
        }

        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            return defineInRange(split(path), defaultValue, min, max);
        }

        public IntValue defineInRange(List<String> path, int defaultValue, int min, int max) {
            return defineInRange(path, (Supplier<Integer>) () -> defaultValue, min, max);
        }

        public IntValue defineInRange(String path, Supplier<Integer> defaultSupplier, int min, int max) {
            return defineInRange(split(path), defaultSupplier, min, max);
        }

        public IntValue defineInRange(List<String> path, Supplier<Integer> defaultSupplier, int min, int max) {
            ConfigValue<Integer> plain = defineInRange(path, defaultSupplier, min, max, Integer.class);
            return new IntValue(this, typedPath(plain), defaultSupplier);
        }

        public LongValue defineInRange(String path, long defaultValue, long min, long max) {
            return defineInRange(split(path), defaultValue, min, max);
        }

        public LongValue defineInRange(List<String> path, long defaultValue, long min, long max) {
            return defineInRange(path, (Supplier<Long>) () -> defaultValue, min, max);
        }

        public LongValue defineInRange(String path, Supplier<Long> defaultSupplier, long min, long max) {
            return defineInRange(split(path), defaultSupplier, min, max);
        }

        public LongValue defineInRange(List<String> path, Supplier<Long> defaultSupplier, long min, long max) {
            ConfigValue<Long> plain = defineInRange(path, defaultSupplier, min, max, Long.class);
            return new LongValue(this, typedPath(plain), defaultSupplier);
        }

        // -- defineInList, defineEnum --

        public <T> ConfigValue<T> defineInList(String path, T defaultValue, Collection<? extends T> acceptableValues) {
            return defineInList(split(path), defaultValue, acceptableValues);
        }

        public <T> ConfigValue<T> defineInList(List<String> path, T defaultValue,
                                               Collection<? extends T> acceptableValues) {
            return define(path, () -> defaultValue, acceptableValues::contains);
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue) {
            return defineEnum(split(path), defaultValue);
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(List<String> path, V defaultValue) {
            return defineEnum(path, defaultValue, Arrays.asList(defaultValue.getDeclaringClass().getEnumConstants()));
        }

        @SafeVarargs
        public final <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue, V... acceptableValues) {
            return defineEnum(split(path), defaultValue, Arrays.asList(acceptableValues));
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue,
                                                           Collection<V> acceptableValues) {
            return defineEnum(split(path), defaultValue, acceptableValues);
        }

        /** An enum, written by name; a name in any case is accepted (NeoForge's NAME_IGNORECASE). */
        public <V extends Enum<V>> EnumValue<V> defineEnum(List<String> path, V defaultValue,
                                                           Collection<V> acceptableValues) {
            Class<V> clazz = defaultValue.getDeclaringClass();
            return defineEnum(path, defaultValue, obj -> {
                if (obj instanceof Enum) {
                    return acceptableValues.contains(obj);
                }
                if (obj instanceof String name) {
                    for (V constant : clazz.getEnumConstants()) {
                        if (constant.name().equalsIgnoreCase(name)) {
                            return acceptableValues.contains(constant);
                        }
                    }
                }
                return false;
            });
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue, Predicate<Object> validator) {
            return defineEnum(split(path), defaultValue, validator);
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(List<String> path, V defaultValue,
                                                           Predicate<Object> validator) {
            Class<V> clazz = defaultValue.getDeclaringClass();
            Supplier<V> defaultSupplier = () -> defaultValue;
            context.setClazz(clazz);
            comment("Allowed Values: " + Arrays.stream(clazz.getEnumConstants()).filter(validator).map(Enum::name)
                    .collect(Collectors.joining(", ")));
            ConfigValue<V> plain = define(path, new ValueSpec(defaultSupplier, validator, context, path),
                    defaultSupplier);
            return new EnumValue<>(this, typedPath(plain), defaultSupplier, clazz);
        }

        // -- lists --

        public <T> ConfigValue<List<? extends T>> defineList(String path, List<? extends T> defaultValue,
                                                             Supplier<T> newElementSupplier,
                                                             Predicate<Object> elementValidator) {
            return defineList(split(path), defaultValue, newElementSupplier, elementValidator);
        }

        public <T> ConfigValue<List<? extends T>> defineList(List<String> path, List<? extends T> defaultValue,
                                                             Supplier<T> newElementSupplier,
                                                             Predicate<Object> elementValidator) {
            return defineList(path, () -> defaultValue, newElementSupplier, elementValidator,
                    ListValueSpec.NON_EMPTY);
        }

        public <T> ConfigValue<List<? extends T>> defineListAllowEmpty(String path, List<? extends T> defaultValue,
                                                                       Supplier<T> newElementSupplier,
                                                                       Predicate<Object> elementValidator) {
            return defineListAllowEmpty(split(path), defaultValue, newElementSupplier, elementValidator);
        }

        public <T> ConfigValue<List<? extends T>> defineListAllowEmpty(List<String> path,
                                                                       List<? extends T> defaultValue,
                                                                       Supplier<T> newElementSupplier,
                                                                       Predicate<Object> elementValidator) {
            return defineList(path, () -> defaultValue, newElementSupplier, elementValidator, null);
        }

        private <T> ConfigValue<List<? extends T>> defineList(List<String> path,
                                                              Supplier<List<? extends T>> defaultSupplier,
                                                              @Nullable Supplier<T> newElementSupplier,
                                                              Predicate<Object> elementValidator,
                                                              @Nullable Range<Integer> sizeRange) {
            context.setClazz(List.class);
            Predicate<Object> listValidator = x -> x instanceof List<?> list
                    && list.stream().allMatch(elementValidator);
            return define(path, new ListValueSpec(defaultSupplier, newElementSupplier, listValidator, elementValidator,
                    context, path, sizeRange), defaultSupplier);
        }
    }

    static List<String> split(String path) {
        return new ArrayList<>(Arrays.asList(path.split("\\.", -1)));
    }

    private static final class BuilderContext {
        private final List<String> comment = new ArrayList<>();
        @Nullable
        private String langKey;
        @Nullable
        private Range<?> range;
        private RestartType restartType = RestartType.NONE;
        @Nullable
        private Class<?> clazz;

        void addComment(String value) {
            comment.add(Objects.requireNonNull(value, "Passed in null value for comment"));
        }

        void clearComment() {
            comment.clear();
        }

        boolean hasComment() {
            return !comment.isEmpty();
        }

        String buildComment(List<String> path) {
            if (comment.stream().allMatch(String::isBlank)) {
                throw new IllegalStateException("Can not build comment for config option " + String.join(".", path)
                        + " as it comprises entirely of blank lines/whitespace.");
            }
            return String.join("\n", comment);
        }

        void setTranslationKey(@Nullable String value) {
            langKey = value;
        }

        @Nullable
        String getTranslationKey() {
            return langKey;
        }

        void setRange(Range<?> value) {
            range = value;
            clazz = value.getClazz();
        }

        @Nullable
        Range<?> getRange() {
            return range;
        }

        RestartType restartType() {
            return restartType;
        }

        void setClazz(Class<?> value) {
            clazz = value;
        }

        @Nullable
        Class<?> getClazz() {
            return clazz;
        }

        void ensureEmpty() {
            if (hasComment()) {
                throw new IllegalStateException("Non-empty comment when empty expected");
            }
            if (langKey != null) {
                throw new IllegalStateException("Non-null translation key when null expected");
            }
            if (range != null) {
                throw new IllegalStateException("Non-null range when null expected");
            }
            if (restartType != RestartType.NONE) {
                throw new IllegalStateException("Dangling restart value set to " + restartType);
            }
        }
    }
}

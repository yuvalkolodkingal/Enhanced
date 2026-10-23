package com.kolodkingal.enhanced.config.core;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The mod's config specs (port P0, PLAN2 3.3): the same builder methods and value types as NeoForge's
 * {@code ModConfigSpec}, whose names the mod's config classes use, so moving to it is an import and type-name swap.
 *
 * <p><b>P0: a facade that delegates to {@code ModConfigSpec}</b> (every value is NeoForge's value underneath; the
 * files, sync, reloads and the config screen are NeoForge's). Lane L7 replaces the backend with the mod's own TOML layer
 * behind this same API.
 */
public final class ConfigSpec {
    private final ModConfigSpec spec;

    private ConfigSpec(ModConfigSpec spec) {
        this.spec = spec;
    }

    /** Whether the config file has been read (before that, values are their defaults and must not be relied on). */
    public boolean isLoaded() {
        return spec.isLoaded();
    }

    /** Writes the config file. */
    public void save() {
        spec.save();
    }

    /** The value specs by path (each a {@link ValueSpec}; {@code null} if none). */
    public Specs getSpec() {
        return new Specs();
    }

    /** P0 only: NeoForge's spec underneath, for the loader's entry point to register. */
    public ModConfigSpec neoSpec() {
        return spec;
    }

    /** The value specs of this config. */
    public final class Specs {
        /** The spec of the value at {@code path}, or null (a section, or nothing). */
        @SuppressWarnings("unchecked")
        public <T> T get(List<String> path) {
            Object found = spec.getSpec().get(path);
            return (T) (found instanceof ModConfigSpec.ValueSpec value ? new ValueSpec(value) : null);
        }
    }

    /** What a value accepts and its comment. */
    public static final class ValueSpec {
        private final ModConfigSpec.ValueSpec spec;

        ValueSpec(ModConfigSpec.ValueSpec spec) {
            this.spec = spec;
        }

        public String getComment() {
            return spec.getComment();
        }

        public String getTranslationKey() {
            return spec.getTranslationKey();
        }

        /** Whether {@code value} is acceptable (type and range). */
        public boolean test(Object value) {
            return spec.test(value);
        }

        public Object correct(Object value) {
            return spec.correct(value);
        }

        public Object getDefault() {
            return spec.getDefault();
        }

        public Class<?> getClazz() {
            return spec.getClazz();
        }
    }

    // ---- values ----

    /** A config value: read with {@link #get()}. */
    public static class ConfigValue<T> implements Supplier<T> {
        final ModConfigSpec.ConfigValue<T> value;

        ConfigValue(ModConfigSpec.ConfigValue<T> value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value.get();
        }

        /** The value in the file, read now (no cache). */
        public T getRaw() {
            return value.getRaw();
        }

        public T getDefault() {
            return value.getDefault();
        }

        /** Changes the value (not yet written to the file: {@link #save()}). */
        public void set(T newValue) {
            value.set(newValue);
        }

        public void save() {
            value.save();
        }

        public void clearCache() {
            value.clearCache();
        }

        /** Its path in the file: its sections' names and its own. */
        public List<String> getPath() {
            return value.getPath();
        }

        public ValueSpec getSpec() {
            return new ValueSpec(value.getSpec());
        }
    }

    public static class BooleanValue extends ConfigValue<Boolean> implements BooleanSupplier {
        BooleanValue(ModConfigSpec.BooleanValue value) {
            super(value);
        }

        @Override
        public boolean getAsBoolean() {
            return ((ModConfigSpec.BooleanValue) value).getAsBoolean();
        }

        public boolean isTrue() {
            return getAsBoolean();
        }

        public boolean isFalse() {
            return !getAsBoolean();
        }
    }

    public static class IntValue extends ConfigValue<Integer> implements IntSupplier {
        IntValue(ModConfigSpec.IntValue value) {
            super(value);
        }

        @Override
        public int getAsInt() {
            return ((ModConfigSpec.IntValue) value).getAsInt();
        }
    }

    public static class LongValue extends ConfigValue<Long> implements LongSupplier {
        LongValue(ModConfigSpec.LongValue value) {
            super(value);
        }

        @Override
        public long getAsLong() {
            return ((ModConfigSpec.LongValue) value).getAsLong();
        }
    }

    public static class DoubleValue extends ConfigValue<Double> implements DoubleSupplier {
        DoubleValue(ModConfigSpec.DoubleValue value) {
            super(value);
        }

        @Override
        public double getAsDouble() {
            return ((ModConfigSpec.DoubleValue) value).getAsDouble();
        }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        EnumValue(ModConfigSpec.EnumValue<T> value) {
            super(value);
        }
    }

    // ---- the builder ----

    /** Builds a spec: sections ({@link #push}/{@link #pop}), comments, and values. */
    public static class Builder {
        private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        public Builder comment(String comment) {
            builder.comment(comment);
            return this;
        }

        public Builder comment(String... comment) {
            builder.comment(comment);
            return this;
        }

        public Builder translation(String translationKey) {
            builder.translation(translationKey);
            return this;
        }

        public Builder worldRestart() {
            builder.worldRestart();
            return this;
        }

        public Builder gameRestart() {
            builder.gameRestart();
            return this;
        }

        public Builder push(String path) {
            builder.push(path);
            return this;
        }

        public Builder push(List<String> path) {
            builder.push(path);
            return this;
        }

        public Builder pop() {
            builder.pop();
            return this;
        }

        public Builder pop(int count) {
            builder.pop(count);
            return this;
        }

        public ConfigSpec build() {
            return new ConfigSpec(builder.build());
        }

        // -- define --

        public <T> ConfigValue<T> define(String path, T defaultValue) {
            return new ConfigValue<>(builder.define(path, defaultValue));
        }

        public <T> ConfigValue<T> define(List<String> path, T defaultValue) {
            return new ConfigValue<>(builder.define(path, defaultValue));
        }

        public <T> ConfigValue<T> define(String path, T defaultValue, Predicate<Object> validator) {
            return new ConfigValue<>(builder.define(path, defaultValue, validator));
        }

        public <T> ConfigValue<T> define(List<String> path, T defaultValue, Predicate<Object> validator) {
            return new ConfigValue<>(builder.define(path, defaultValue, validator));
        }

        public <T> ConfigValue<T> define(String path, Supplier<T> defaultSupplier, Predicate<Object> validator) {
            return new ConfigValue<>(builder.define(path, defaultSupplier, validator));
        }

        public <T> ConfigValue<T> define(List<String> path, Supplier<T> defaultSupplier, Predicate<Object> validator) {
            return new ConfigValue<>(builder.define(path, defaultSupplier, validator));
        }

        public BooleanValue define(String path, boolean defaultValue) {
            return new BooleanValue(builder.define(path, defaultValue));
        }

        public BooleanValue define(List<String> path, boolean defaultValue) {
            return new BooleanValue(builder.define(path, defaultValue));
        }

        public BooleanValue define(String path, Supplier<Boolean> defaultSupplier) {
            return new BooleanValue(builder.define(path, defaultSupplier));
        }

        public BooleanValue define(List<String> path, Supplier<Boolean> defaultSupplier) {
            return new BooleanValue(builder.define(path, defaultSupplier));
        }

        // -- defineInRange --

        public <V extends Comparable<? super V>> ConfigValue<V> defineInRange(String path, V defaultValue, V min, V max,
                                                                              Class<V> clazz) {
            return new ConfigValue<>(builder.defineInRange(path, defaultValue, min, max, clazz));
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            return new DoubleValue(builder.defineInRange(path, defaultValue, min, max));
        }

        public DoubleValue defineInRange(List<String> path, double defaultValue, double min, double max) {
            return new DoubleValue(builder.defineInRange(path, defaultValue, min, max));
        }

        public DoubleValue defineInRange(String path, Supplier<Double> defaultSupplier, double min, double max) {
            return new DoubleValue(builder.defineInRange(path, defaultSupplier, min, max));
        }

        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            return new IntValue(builder.defineInRange(path, defaultValue, min, max));
        }

        public IntValue defineInRange(List<String> path, int defaultValue, int min, int max) {
            return new IntValue(builder.defineInRange(path, defaultValue, min, max));
        }

        public IntValue defineInRange(String path, Supplier<Integer> defaultSupplier, int min, int max) {
            return new IntValue(builder.defineInRange(path, defaultSupplier, min, max));
        }

        public LongValue defineInRange(String path, long defaultValue, long min, long max) {
            return new LongValue(builder.defineInRange(path, defaultValue, min, max));
        }

        public LongValue defineInRange(List<String> path, long defaultValue, long min, long max) {
            return new LongValue(builder.defineInRange(path, defaultValue, min, max));
        }

        public LongValue defineInRange(String path, Supplier<Long> defaultSupplier, long min, long max) {
            return new LongValue(builder.defineInRange(path, defaultSupplier, min, max));
        }

        // -- defineInList, defineEnum --

        public <T> ConfigValue<T> defineInList(String path, T defaultValue, Collection<? extends T> acceptableValues) {
            return new ConfigValue<>(builder.defineInList(path, defaultValue, acceptableValues));
        }

        public <T> ConfigValue<T> defineInList(List<String> path, T defaultValue,
                                               Collection<? extends T> acceptableValues) {
            return new ConfigValue<>(builder.defineInList(path, defaultValue, acceptableValues));
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue) {
            return new EnumValue<>(builder.defineEnum(path, defaultValue));
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(List<String> path, V defaultValue) {
            return new EnumValue<>(builder.defineEnum(path, defaultValue));
        }

        @SafeVarargs
        public final <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue, V... acceptableValues) {
            return new EnumValue<>(builder.defineEnum(path, defaultValue, acceptableValues));
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue, Collection<V> acceptableValues) {
            return new EnumValue<>(builder.defineEnum(path, defaultValue, acceptableValues));
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue, Predicate<Object> validator) {
            return new EnumValue<>(builder.defineEnum(path, defaultValue, validator));
        }

        // -- lists --

        public <T> ConfigValue<List<? extends T>> defineList(String path, List<? extends T> defaultValue,
                                                             Supplier<T> newElementSupplier,
                                                             Predicate<Object> elementValidator) {
            return new ConfigValue<>(builder.defineList(path, defaultValue, newElementSupplier, elementValidator));
        }

        public <T> ConfigValue<List<? extends T>> defineList(List<String> path, List<? extends T> defaultValue,
                                                             Supplier<T> newElementSupplier,
                                                             Predicate<Object> elementValidator) {
            return new ConfigValue<>(builder.defineList(path, defaultValue, newElementSupplier, elementValidator));
        }

        public <T> ConfigValue<List<? extends T>> defineListAllowEmpty(String path, List<? extends T> defaultValue,
                                                                       Supplier<T> newElementSupplier,
                                                                       Predicate<Object> elementValidator) {
            return new ConfigValue<>(builder.defineListAllowEmpty(path, defaultValue, newElementSupplier,
                    elementValidator));
        }

        public <T> ConfigValue<List<? extends T>> defineListAllowEmpty(List<String> path,
                                                                       List<? extends T> defaultValue,
                                                                       Supplier<T> newElementSupplier,
                                                                       Predicate<Object> elementValidator) {
            return new ConfigValue<>(builder.defineListAllowEmpty(path, defaultValue, newElementSupplier,
                    elementValidator));
        }
    }
}

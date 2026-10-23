package com.kolodkingal.enhanced.config.screen;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.core.ConfigFile;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * One section of a config file (port P0, lane L7; client only): a row per value (a switch, a slider, a number or text
 * box, a cycle of choices, or a button for a list) and per sub-section, with Undo, Reset and Done, as NeoForge's
 * configuration screen had. Changes take effect at once; the file is written when the screen of the whole file closes.
 */
public class ConfigSectionScreen extends OptionsSubScreen {
    /** Sliders for whole numbers with at most this many steps; number boxes for more. */
    private static final long MAX_SLIDER_SIZE = 256L;
    private static final MutableComponent EMPTY_LINE = Component.literal("\n\n");

    protected final ConfigFile file;
    protected final ConfigSpec spec;
    protected final List<String> path;
    private final Map<String, Screen> children = new HashMap<>();
    /** Whether a value of this section (or one below it) was changed: the file is written when the top closes. */
    boolean changed;
    private final Deque<List<Change>> undo = new ArrayDeque<>();
    @Nullable
    private Button undoButton;
    @Nullable
    private Button resetButton;

    /** One value changed: from what to what. */
    private record Change(ConfigSpec.ConfigValue<Object> value, Object before, Object after) {
    }

    public ConfigSectionScreen(Screen parent, ConfigFile file, List<String> path, Component title) {
        super(parent, Minecraft.getInstance().options, title);
        this.file = file;
        this.spec = file.spec();
        this.path = List.copyOf(path);
    }

    // ---- names ----

    static String sectionKey(ConfigSpec spec, List<String> path) {
        String key = spec.getLevelTranslationKey(path);
        return key != null ? key : Enhanced.MODID + ".configuration." + path.get(path.size() - 1);
    }

    static String valueKey(ConfigSpec.ValueSpec valueSpec, String name) {
        String key = valueSpec.getTranslationKey();
        return key != null ? key : Enhanced.MODID + ".configuration." + name;
    }

    /** The name of an entry: its translation, else its key in the file. */
    static MutableComponent name(String translationKey, String name) {
        return Component.translatableWithFallback(translationKey, name);
    }

    static Component tooltip(String translationKey, String name, @Nullable String comment,
                             @Nullable ConfigSpec.Range<?> range) {
        String tooltipKey = translationKey + ".tooltip";
        boolean translated = I18n.exists(tooltipKey);
        MutableComponent tooltip = Component.empty().append(name(translationKey, name).withStyle(ChatFormatting.BOLD));
        if (translated || comment != null && !comment.isBlank()) {
            tooltip.append(EMPTY_LINE).append(Component.translatableWithFallback(tooltipKey,
                    comment == null ? "" : comment));
        }
        if (translated && range != null) {
            tooltip.append(EMPTY_LINE).append(Component.translatable(ConfigScreen.UI + "range", range.toString())
                    .withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }

    // ---- the rows ----

    @Override
    protected void addOptions() {
        rebuild();
    }

    /** Fills the list again from the values as they are now. */
    protected void rebuild() {
        if (list == null) {
            return;
        }
        list.children().clear();
        boolean editable = false;
        for (Map.Entry<String, Object> entry : spec.entries(path).entrySet()) {
            String key = entry.getKey();
            List<String> full = child(key);
            if (entry.getValue() instanceof Map<?, ?> section) {
                if (!section.isEmpty()) {
                    sectionRow(key, full);
                }
                continue;
            }
            ConfigSpec.ValueSpec valueSpec = (ConfigSpec.ValueSpec) entry.getValue();
            ConfigSpec.ConfigValue<?> value = spec.value(full);
            if (value == null) {
                continue;
            }
            String translationKey = valueKey(valueSpec, key);
            Component tooltip = tooltip(translationKey, key, valueSpec.getComment(), null);
            AbstractWidget widget = valueWidget(key, translationKey, valueSpec, value);
            StringWidget label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT,
                    name(translationKey, key), font).alignLeft();
            label.setTooltip(Tooltip.create(tooltip));
            list.addSmall(label, widget);
            editable |= !(valueSpec instanceof ConfigSpec.ListValueSpec);
        }
        if (editable && undoButton == null) {
            undoButton = Button.builder(Component.translatable(ConfigScreen.UI + "undo"), b -> {
                undo();
                rebuild();
            }).tooltip(Tooltip.create(Component.translatable(ConfigScreen.UI + "undo.tooltip")))
                    .width(Button.SMALL_WIDTH).build();
            resetButton = Button.builder(Component.translatable(ConfigScreen.UI + "reset"), b -> {
                reset();
                rebuild();
            }).tooltip(Tooltip.create(Component.translatable(ConfigScreen.UI + "reset.tooltip")))
                    .width(Button.SMALL_WIDTH).build();
        }
    }

    private List<String> child(String key) {
        List<String> full = new ArrayList<>(path);
        full.add(key);
        return full;
    }

    private void sectionRow(String key, List<String> full) {
        String translationKey = sectionKey(spec, full);
        Component tooltip = tooltip(translationKey, key, spec.getLevelComment(full), null);
        MutableComponent sectionName = name(translationKey, key);
        Component buttonText = I18n.exists(translationKey + ".button")
                ? Component.translatable(translationKey + ".button")
                : Component.translatable(ConfigScreen.UI + "edit");
        Button button = Button.builder(buttonText, b -> minecraft.setScreen(children.computeIfAbsent(key,
                        k -> new ConfigSectionScreen(this, file, full, crumb(sectionName)))))
                .tooltip(Tooltip.create(tooltip)).width(Button.DEFAULT_WIDTH).build();
        StringWidget label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT,
                Component.translatable(ConfigScreen.UI + "section", sectionName), font).alignLeft();
        label.setTooltip(Tooltip.create(tooltip));
        list.addSmall(label, button);
    }

    /** The title of a screen opened from this one: this one's title, an arrow, its own name. */
    Component crumb(Component name) {
        return Component.empty().append(getTitle()).append(Component.literal(" > ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private AbstractWidget valueWidget(String key, String translationKey, ConfigSpec.ValueSpec valueSpec,
                                       ConfigSpec.ConfigValue<?> value) {
        Component name = name(translationKey, key);
        Component tooltip = tooltip(translationKey, key, valueSpec.getComment(), null);
        ConfigSpec.ConfigValue<Object> target = (ConfigSpec.ConfigValue<Object>) value;
        if (valueSpec instanceof ConfigSpec.ListValueSpec listSpec) {
            return Button.builder(Component.translatable(ConfigScreen.UI + "edit"),
                            b -> minecraft.setScreen(new ConfigListScreen(this,
                                    (ConfigSpec.ConfigValue<List<?>>) (Object) value, listSpec, crumb(name))))
                    .tooltip(Tooltip.create(tooltip)).width(Button.DEFAULT_WIDTH).build();
        }
        if (value instanceof ConfigSpec.BooleanValue booleanValue) {
            return CycleButton.onOffBuilder(booleanValue.getRaw()).displayOnlyValue()
                    .withTooltip(v -> Tooltip.create(tooltip))
                    .create(0, 0, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, name, (b, v) -> apply(target, v));
        }
        if (value instanceof ConfigSpec.EnumValue enumValue) {
            List<Enum<?>> choices = new ArrayList<>(Arrays.stream(enumValue.enumClass().getEnumConstants())
                    .filter(valueSpec::test).map(e -> (Enum<?>) e).toList());
            Enum<?> current = (Enum<?>) enumValue.getRaw();
            return CycleButton.<Enum<?>>builder(e -> Component.literal(e.name())).withValues(choices)
                    .withInitialValue(current).displayOnlyValue().withTooltip(v -> Tooltip.create(tooltip))
                    .create(0, 0, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, name, (b, v) -> apply(target, v));
        }
        if (value instanceof ConfigSpec.IntValue intValue) {
            ConfigSpec.Range<Integer> range = valueSpec.getRange();
            int min = range != null ? range.getMin() : 0;
            int max = range != null ? range.getMax() : Integer.MAX_VALUE;
            Component rangeTooltip = tooltip(translationKey, key, valueSpec.getComment(), range);
            if ((long) max - (long) min < MAX_SLIDER_SIZE) {
                IntSlider slider = new IntSlider(min, max, intValue.getRaw(), v -> apply(target, v));
                slider.setTooltip(Tooltip.create(rangeTooltip));
                return slider;
            }
            return numberBox(name, rangeTooltip, valueSpec, value, Integer::decode);
        }
        if (value instanceof ConfigSpec.LongValue) {
            return numberBox(name, tooltip(translationKey, key, valueSpec.getComment(), valueSpec.getRange()),
                    valueSpec, value, Long::decode);
        }
        if (value instanceof ConfigSpec.DoubleValue) {
            return numberBox(name, tooltip(translationKey, key, valueSpec.getComment(), valueSpec.getRange()),
                    valueSpec, value, Double::parseDouble);
        }
        if (valueSpec.getDefault() instanceof String) {
            return stringBox(name, tooltip, valueSpec, target);
        }
        StringWidget label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT,
                Component.literal(Objects.toString(value.getRaw())), font).alignLeft();
        label.setTooltip(Tooltip.create(Component.translatable(ConfigScreen.UI + "unsupported")
                .withStyle(ChatFormatting.RED)));
        return label;
    }

    @SuppressWarnings("unchecked")
    private EditBox numberBox(Component name, Component tooltip, ConfigSpec.ValueSpec valueSpec,
                              ConfigSpec.ConfigValue<?> value, Function<String, ? extends Number> parser) {
        ConfigSpec.ConfigValue<Object> target = (ConfigSpec.ConfigValue<Object>) value;
        EditBox box = new EditBox(font, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, name);
        box.setTooltip(Tooltip.create(tooltip));
        box.setValue(String.valueOf(value.getRaw()));
        box.setResponder(text -> {
            try {
                Number number = parser.apply(text.strip());
                if (valueSpec.test(number)) {
                    apply(target, number);
                    box.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                    return;
                }
            } catch (NumberFormatException e) {
                // (not a number yet: typing goes on)
            }
            box.setTextColor(0xFFFF0000);
        });
        return box;
    }

    private AbstractWidget stringBox(Component name, Component tooltip, ConfigSpec.ValueSpec valueSpec,
                                     ConfigSpec.ConfigValue<Object> target) {
        String current = String.valueOf(target.getRaw());
        if (current.length() > 192) {
            StringWidget label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT,
                    Component.literal(current.substring(0, 128)), font).alignLeft();
            label.setTooltip(Tooltip.create(Component.translatable(ConfigScreen.UI + "too_long")
                    .withStyle(ChatFormatting.RED)));
            return label;
        }
        EditBox box = new EditBox(font, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, name);
        box.setTooltip(Tooltip.create(tooltip));
        box.setMaxLength(Mth.clamp(current.length() + 5, 128, 192));
        box.setValue(current);
        box.setResponder(text -> {
            if (valueSpec.test(text)) {
                apply(target, text);
                box.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
            } else {
                box.setTextColor(0xFFFF0000);
            }
        });
        return box;
    }

    // ---- changes ----

    /** Sets a value (if it changes), as one step to undo. */
    void apply(ConfigSpec.ConfigValue<Object> value, Object newValue) {
        Object before = value.getRaw();
        if (Objects.equals(before, newValue)) {
            return;
        }
        value.set(newValue);
        undo.push(List.of(new Change(value, before, newValue)));
        changed = true;
    }

    private void undo() {
        List<Change> step = undo.poll();
        if (step == null) {
            return;
        }
        for (int i = step.size() - 1; i >= 0; i--) {
            step.get(i).value().set(step.get(i).before());
        }
        changed = true;
    }

    /** Every value of this section (not its lists) back to its default, as one step to undo. */
    @SuppressWarnings("unchecked")
    private void reset() {
        List<Change> step = new ArrayList<>();
        for (ConfigSpec.ConfigValue<?> value : sectionValues()) {
            Object before = value.getRaw();
            Object after = value.getSpec().correct(null);
            ((ConfigSpec.ConfigValue<Object>) value).set(after);
            step.add(new Change((ConfigSpec.ConfigValue<Object>) value, before, after));
        }
        if (!step.isEmpty()) {
            undo.push(step);
            changed = true;
        }
    }

    /** The values of this section that are not lists and not at their default. */
    private List<ConfigSpec.ConfigValue<?>> sectionValues() {
        List<ConfigSpec.ConfigValue<?>> values = new ArrayList<>();
        if (!spec.isLoaded()) {
            return values;
        }
        for (Map.Entry<String, Object> entry : spec.entries(path).entrySet()) {
            if (entry.getValue() instanceof ConfigSpec.ValueSpec valueSpec
                    && !(valueSpec instanceof ConfigSpec.ListValueSpec)) {
                ConfigSpec.ConfigValue<?> value = spec.value(child(entry.getKey()));
                if (value != null && !Objects.equals(value.getRaw(), value.getDefault())) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (undoButton != null) {
            undoButton.active = !undo.isEmpty();
        }
        if (resetButton != null) {
            resetButton.active = !sectionValues().isEmpty();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void addFooter() {
        if (undoButton == null) {
            super.addFooter();
            return;
        }
        LinearLayout row = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        row.addChild(undoButton);
        row.addChild(resetButton);
        row.addChild(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(Button.SMALL_WIDTH).build());
    }

    @Override
    public void onClose() {
        if (changed) {
            if (lastScreen instanceof ConfigSectionScreen parent) {
                // (the screen of the whole file writes it)
                parent.changed = true;
            } else if (spec.isLoaded()) {
                spec.save();
            }
        }
        super.onClose();
    }

    /** A slider over whole numbers. */
    private static final class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final Consumer<Integer> onChange;

        IntSlider(int min, int max, int current, Consumer<Integer> onChange) {
            super(0, 0, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, Component.empty(),
                    max == min ? 0.0 : (Mth.clamp(current, min, max) - (double) min) / ((double) max - min));
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        private int current() {
            return (int) Math.round(min + value * ((double) max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(Integer.toString(current())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(current());
        }
    }
}

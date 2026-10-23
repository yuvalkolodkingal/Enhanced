package com.kolodkingal.enhanced.config.screen;

import com.kolodkingal.enhanced.config.core.ConfigSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The entries of a list value (port P0, lane L7; client only): a text box per entry with a button to take it out, a
 * button to add one, Reset and Done. The list is set when the screen closes, if every entry is acceptable.
 */
public class ConfigListScreen extends OptionsSubScreen {
    private final ConfigSectionScreen section;
    private final ConfigSpec.ConfigValue<List<?>> value;
    private final ConfigSpec.ListValueSpec spec;
    private List<Object> entries;
    @Nullable
    private Button resetButton;

    public ConfigListScreen(ConfigSectionScreen section, ConfigSpec.ConfigValue<List<?>> value,
                            ConfigSpec.ListValueSpec spec, Component title) {
        super(section, Minecraft.getInstance().options, title);
        this.section = section;
        this.value = value;
        this.spec = spec;
        this.entries = new ArrayList<>(value.getRaw());
    }

    @Override
    protected void addOptions() {
        rebuild();
    }

    private void rebuild() {
        if (list == null) {
            return;
        }
        list.children().clear();
        for (int i = 0; i < entries.size(); i++) {
            int index = i;
            Button remove = Button.builder(Component.translatable(ConfigScreen.UI + "remove", i + 1), b -> {
                entries.remove(index);
                rebuild();
            }).width(Button.DEFAULT_WIDTH).build();
            remove.active = spec.getSizeRange().test(entries.size() - 1);
            Object entry = entries.get(i);
            if (entry instanceof String text) {
                EditBox box = new EditBox(font, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, getTitle());
                box.setMaxLength(Mth.clamp(text.length() + 5, 128, 192));
                box.setValue(text);
                box.setResponder(now -> {
                    boolean ok = spec.testElement(now);
                    if (ok) {
                        entries.set(index, now);
                    }
                    box.setTextColor(ok ? EditBox.DEFAULT_TEXT_COLOR : 0xFFFF0000);
                });
                list.addSmall(remove, box);
            } else {
                StringWidget label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT,
                        Component.literal(Objects.toString(entry)), font).alignLeft();
                label.setTooltip(Tooltip.create(Component.translatable(ConfigScreen.UI + "unsupported")
                        .withStyle(ChatFormatting.RED)));
                list.addSmall(remove, label);
            }
        }
        Supplier<?> newEntry = spec.getNewElementSupplier();
        if (newEntry != null && spec.getSizeRange().test(entries.size() + 1)) {
            list.addSmall(new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, Component.empty(), font),
                    Button.builder(Component.translatable(ConfigScreen.UI + "add"), b -> {
                        entries.add(newEntry.get());
                        rebuild();
                    }).width(Button.DEFAULT_WIDTH).build());
        }
        if (resetButton == null) {
            resetButton = Button.builder(Component.translatable(ConfigScreen.UI + "reset"), b -> {
                entries = new ArrayList<>(value.getDefault());
                rebuild();
            }).tooltip(Tooltip.create(Component.translatable(ConfigScreen.UI + "reset.tooltip")))
                    .width(Button.SMALL_WIDTH).build();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (resetButton != null) {
            resetButton.active = !entries.equals(value.getDefault());
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void addFooter() {
        LinearLayout row = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        if (resetButton != null) {
            row.addChild(resetButton);
        }
        row.addChild(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(Button.SMALL_WIDTH).build());
    }

    @Override
    public void onClose() {
        List<Object> now = new ArrayList<>(entries);
        if (value.owner().isLoaded() && !now.equals(value.getRaw()) && spec.test(now)) {
            @SuppressWarnings("unchecked")
            ConfigSpec.ConfigValue<Object> target = (ConfigSpec.ConfigValue<Object>) (Object) value;
            section.apply(target, now);
        }
        super.onClose();
    }
}

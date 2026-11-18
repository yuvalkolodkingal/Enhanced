package com.kolodkingal.enhanced.config.screen;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.core.ConfigFile;
import com.kolodkingal.enhanced.config.core.Configs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

/**
 * The mod's settings screen (port P0, lane L7; client only): a button for the client's settings and one for the
 * server's, each opening its sections ({@link ConfigSectionScreen}). It replaces NeoForge's {@code ConfigurationScreen}
 * with the same rules: the server's settings can be changed in a world of your own (not while it is open to LAN), are
 * read-only on another server and unavailable with no world loaded. The names come from the lang keys NeoForge's
 * screen used ({@code enhanced.configuration.*}, {@code config.enhanced.*}).
 */
public class ConfigScreen extends OptionsSubScreen {
    static final String UI = Enhanced.MODID + ".configuration.ui.";
    /** The width of a button that takes a whole row. */
    static final int WIDE = 310;
    private static final MutableComponent EMPTY_LINE = Component.literal("\n\n");

    public ConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable(Enhanced.MODID + ".configuration.title"));
    }

    @Override
    protected void addOptions() {
        for (ConfigFile.Type type : List.of(ConfigFile.Type.CLIENT, ConfigFile.Type.SERVER)) {
            List<ConfigFile> files = Configs.of(type);
            if (files.isEmpty()) {
                continue;
            }
            list.addSmall(new StringWidget(WIDE, Button.DEFAULT_HEIGHT,
                    Component.translatable(UI + type.extension()).withStyle(ChatFormatting.UNDERLINE), font)
                    .alignLeft(), null);
            for (ConfigFile file : files) {
                list.addSmall(fileButton(type, file), null);
            }
        }
    }

    private Button fileButton(ConfigFile.Type type, ConfigFile file) {
        String key = Enhanced.MODID + ".configuration.section." + file.fileName().replaceAll("[^a-zA-Z0-9]+", ".")
                .replaceFirst("^\\.", "").replaceFirst("\\.$", "").toLowerCase(Locale.ROOT);
        Component title = Component.translatableWithFallback(key + ".title", file.fileName());
        Button button = Button.builder(Component.translatable(UI + "section",
                        Component.translatableWithFallback(key, file.fileName())),
                b -> minecraft.setScreen(new ConfigSectionScreen(this, file, List.of(), title)))
                .width(WIDE).build();
        MutableComponent tooltip = Component.empty();
        if (!file.spec().isLoaded()) {
            tooltip.append(Component.translatable(UI + "not_loaded").withStyle(ChatFormatting.RED)).append(EMPTY_LINE);
            button.active = false;
        } else if (type == ConfigFile.Type.SERVER && minecraft.getCurrentServer() != null
                && !minecraft.isSingleplayer()) {
            tooltip.append(Component.translatable(UI + "not_online").withStyle(ChatFormatting.RED)).append(EMPTY_LINE);
            button.active = false;
        } else if (type == ConfigFile.Type.SERVER && minecraft.hasSingleplayerServer()
                && minecraft.getSingleplayerServer().isPublished()) {
            tooltip.append(Component.translatable(UI + "not_lan").withStyle(ChatFormatting.RED)).append(EMPTY_LINE);
            button.active = false;
        }
        tooltip.append(Component.translatable(UI + "file", file.fileName()).withStyle(ChatFormatting.GRAY));
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }
}

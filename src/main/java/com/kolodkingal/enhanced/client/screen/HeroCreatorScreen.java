package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.network.HeroCreatorPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Hero Creator's first screen (3.3): three big buttons. <i>Choose a hero</i> (any hero of the mod and a look, at
 * once: {@link ChooseHeroScreen}), <i>Create my hero</i> (or <i>Edit my hero</i> when the player already made one:
 * {@link HeroEditorScreen}), and <i>Stop being a hero</i>. The server checks every button again.
 */
public class HeroCreatorScreen extends Screen {
    /** The Hero Creator's own gold. */
    public static final int GOLD = Power.CUSTOM.color;

    public HeroCreatorScreen() {
        super(Component.translatable("screen.enhanced.hero_creator.title"));
    }

    /** Opens the creator (the item's use, on the client). */
    public static void open() {
        Minecraft.getInstance().setScreen(new HeroCreatorScreen());
    }

    /** Whether the server has the creator switched on (its synced config). */
    static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || CustomHeroConfig.ENABLED.get();
    }

    static boolean mayChoose() {
        return !EnhancedConfig.SPEC.isLoaded() || CustomHeroConfig.ALLOW_CHOOSE_EXISTING.get();
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        int w = Math.min(250, width - 20);
        int x = width / 2 - w / 2;
        int y = Math.max(40, height / 2 - 66);
        boolean on = enabled();
        CreatorUi.BigButton choose = new CreatorUi.BigButton(x, y, w, 40, Component.translatable("screen.enhanced.hero_creator.choose"),
                Component.translatable("screen.enhanced.hero_creator.choose.desc"), GOLD, () -> mc.setScreen(new ChooseHeroScreen(this)))
                .icon(new ItemStack(Items.NETHER_STAR));
        choose.active = on && mayChoose();
        addRenderableWidget(choose);

        CustomHero mine = mc.player == null ? CustomHero.EMPTY : CustomHeroes.hero(mc.player);
        boolean edit = !mine.isEmpty();
        int heroColor = edit ? mine.color() : 0x3FA9F5;
        CreatorUi.BigButton create = new CreatorUi.BigButton(x, y + 46, w, 40,
                Component.translatable(edit ? "screen.enhanced.hero_creator.edit" : "screen.enhanced.hero_creator.create"),
                Component.translatable("screen.enhanced.hero_creator.create.desc"), heroColor,
                () -> mc.setScreen(new HeroEditorScreen(this, edit ? mine : CustomHero.EMPTY)))
                .icon(new ItemStack(ModItems.HERO_CREATOR.get()));
        create.active = on;
        addRenderableWidget(create);

        boolean hero = mc.player != null && Powers.power(mc.player).hasPowers();
        CreatorUi.BigButton stop = new CreatorUi.BigButton(x + w / 4, y + 92, w / 2, 24, Component.translatable("screen.enhanced.hero_creator.stop"),
                null, 0x9E9E9E, () -> {
                    PacketDistributor.sendToServer(HeroCreatorPayload.choose(Power.NONE, ""));
                    onClose();
                });
        stop.active = on && hero;
        addRenderableWidget(stop);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 50, Math.min(height - 26, y + 124), 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int y = Math.max(40, height / 2 - 66);
        g.pose().pushPose();
        g.pose().translate(width / 2F, y - 30, 0);
        g.pose().scale(2F, 2F, 1F);
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(GOLD).withBold(true)), 0, 0, 0xFFFFFF);
        g.pose().popPose();
        if (!enabled()) {
            g.drawCenteredString(font, Component.translatable("message.enhanced.hero_creator.disabled").withStyle(ChatFormatting.RED),
                    width / 2, y - 10, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

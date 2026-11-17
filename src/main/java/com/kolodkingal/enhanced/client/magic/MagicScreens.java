package com.kolodkingal.enhanced.client.magic;

import com.kolodkingal.enhanced.magic.MagicActionPayload;
import com.kolodkingal.enhanced.magic.MagicScreenPayload;
import com.kolodkingal.enhanced.magic.WitchPath;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The screens of 3.17's magic, opened by the server ({@link MagicScreenPayload}): Billy's Clairvoyance (a name to seek),
 * the Demiurge's Grant Magic (who, what magic, how strong; and taking it back) and the Coven's Road's prizes. The server
 * checks every answer again.
 */
public final class MagicScreens {
    private MagicScreens() {
    }

    public static void open(MagicScreenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        switch (payload.screen()) {
            case MagicScreenPayload.CLAIRVOYANCE -> mc.setScreen(new ClairvoyanceScreen(payload.names()));
            case MagicScreenPayload.GRANT -> mc.setScreen(new GrantScreen(payload.names(), payload.extra()));
            case MagicScreenPayload.ROAD_PRIZE -> mc.setScreen(new RoadPrizeScreen(payload.names(), payload.extra()));
            default -> {
            }
        }
    }

    /** A dark panel with a coloured frame, as the mod's other choice screens. */
    static void panel(GuiGraphics g, int x0, int y0, int x1, int y1, int top, int bottom, int frame) {
        g.fillGradient(x0, y0, x1, y1, top, bottom);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, frame);
    }

    // ==================================================================================================================

    /** Clairvoyance: type a player's or an item's name (or click a player), and see where it is. */
    static final class ClairvoyanceScreen extends Screen {
        private static final int WIDTH = 220;
        private final List<String> players;
        private EditBox box;
        private String typed = "";

        ClairvoyanceScreen(List<String> players) {
            super(Component.translatable("screen.enhanced.clairvoyance.title"));
            this.players = players;
        }

        @Override
        protected void init() {
            int left = (width - WIDTH) / 2;
            int top = height / 2 - 70;
            box = new EditBox(font, left, top + 30, WIDTH, 20, Component.translatable("screen.enhanced.clairvoyance.name"));
            box.setMaxLength(64);
            box.setHint(Component.translatable("screen.enhanced.clairvoyance.hint").withStyle(ChatFormatting.DARK_GRAY));
            box.setValue(typed);
            box.setResponder(value -> typed = value);
            addRenderableWidget(box);
            setInitialFocus(box);
            int y = top + 56;
            int col = 0;
            for (String name : players.subList(0, Math.min(8, players.size()))) {
                addRenderableWidget(Button.builder(Component.literal(name), b -> seek(name))
                        .bounds(left + col * (WIDTH / 2 + 2), y, WIDTH / 2 - 2, 18).build());
                col++;
                if (col == 2) {
                    col = 0;
                    y += 20;
                }
            }
            addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.clairvoyance.seek"), b -> seek(typed))
                    .bounds(left, top + 146, WIDTH / 2 - 2, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                    .bounds(left + WIDTH / 2 + 2, top + 146, WIDTH / 2 - 2, 20).build());
        }

        private void seek(String name) {
            if (!name.isBlank()) {
                Net.sendToServer(new MagicActionPayload(MagicActionPayload.CLAIRVOYANCE, name, "", 0));
            }
            onClose();
        }

        @Override
        public boolean keyPressed(int key, int scan, int modifiers) {
            if ((key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) && !typed.isBlank()) {
                seek(typed);
                return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }

        @Override
        public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            renderTransparentBackground(g);
            int left = (width - WIDTH) / 2;
            int top = height / 2 - 70;
            panel(g, left - 8, top - 4, left + WIDTH + 8, top + 172, 0xEE0E2240, 0xEE050B1A, 0xFF5CC8FF);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            int top = height / 2 - 70;
            g.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(0x9FE0FF).withBold(true)), width / 2, top + 2, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.translatable("screen.enhanced.clairvoyance.subtitle").withStyle(ChatFormatting.GRAY),
                    width / 2, top + 15, 0xFFFFFFFF);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    // ==================================================================================================================

    /**
     * The Demiurge's Grant Magic: the players on the left (with the magic each has), the kind of magic (the paths and the
     * sorcerer's art) as cards on the right, the strength (level 1-5) below; Grant gives it, Take Back takes a gift away.
     */
    static final class GrantScreen extends Screen {
        private final List<String> names;
        private final List<String> states;
        private int chosenPlayer = -1;
        private WitchPath chosenPath = WitchPath.GREEN;
        private int level = 1;
        private int left;
        private int top;
        private int panelW;
        private int panelH;
        private Button grant;
        private Button revoke;

        GrantScreen(List<String> names, List<String> states) {
            super(Component.translatable("screen.enhanced.grant.title"));
            this.names = names;
            this.states = states;
        }

        @Override
        protected void init() {
            panelW = Math.min(380, width - 20);
            panelH = Math.min(240, height - 20);
            left = (width - panelW) / 2;
            top = (height - panelH) / 2;
            int listW = panelW * 2 / 5;
            int y = top + 30;
            for (int i = 0; i < Math.min(names.size(), (panelH - 70) / 20); i++) {
                int index = i;
                String state = states.get(i);
                boolean hero = state.startsWith("hero:");
                Button b = Button.builder(Component.literal(names.get(i)), btn -> {
                    chosenPlayer = index;
                    refresh();
                }).bounds(left + 6, y, listW - 8, 18).build();
                b.setTooltip(Tooltip.create(describe(state)));
                b.active = true;
                if (hero) {
                    b.setMessage(Component.literal(names.get(i)).withStyle(ChatFormatting.DARK_GRAY));
                }
                addRenderableWidget(b);
                y += 20;
            }
            int cardsX = left + listW + 4;
            int cardW = (panelW - listW - 12) / 2;
            int cy = top + 30;
            for (int k = 0; k < WitchPath.values().length; k++) {
                WitchPath path = WitchPath.values()[k];
                int col = k % 2;
                int row = k / 2;
                addRenderableWidget(Button.builder(path.displayName().copy().withStyle(s -> s.withColor(path.color)), btn -> {
                    chosenPath = path;
                    refresh();
                }).bounds(cardsX + col * (cardW + 2), cy + row * 26, cardW, 22).tooltip(Tooltip.create(path.description())).build());
            }
            int ly = cy + 3 * 26 + 8;
            addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
                level = Math.max(1, level - 1);
                refresh();
            }).bounds(cardsX, ly, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
                level = Math.min(WitchPath.MAX_LEVEL, level + 1);
                refresh();
            }).bounds(cardsX + 2 * cardW - 18, ly, 20, 20).build());
            grant = addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.grant.give"), btn -> {
                if (chosenPlayer >= 0) {
                    Net.sendToServer(new MagicActionPayload(MagicActionPayload.GRANT, names.get(chosenPlayer), chosenPath.id(), level));
                }
            }).bounds(cardsX, ly + 26, cardW, 20).build());
            revoke = addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.grant.revoke"), btn -> {
                if (chosenPlayer >= 0) {
                    Net.sendToServer(new MagicActionPayload(MagicActionPayload.REVOKE, names.get(chosenPlayer), "", 0));
                }
            }).bounds(cardsX + cardW + 2, ly + 26, cardW, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                    .bounds(left + panelW / 2 - 50, top + panelH - 24, 100, 20).build());
            refresh();
        }

        private void refresh() {
            if (grant == null) {
                return;
            }
            boolean picked = chosenPlayer >= 0 && chosenPlayer < states.size();
            String state = picked ? states.get(chosenPlayer) : "";
            grant.active = picked && !state.startsWith("hero:");
            revoke.active = picked && !state.equals("none") && !state.startsWith("hero:");
        }

        private static Component describe(String state) {
            if (state.equals("none")) {
                return Component.translatable("screen.enhanced.grant.no_magic");
            }
            if (state.startsWith("hero:")) {
                return Component.translatable("screen.enhanced.grant.hero", Component.translatable("power.enhanced." + state.substring(5)));
            }
            String[] p = state.split(":");
            WitchPath path = WitchPath.byId(p[0]);
            return Component.translatable("screen.enhanced.grant.has", path == null ? Component.literal(p[0]) : path.displayName(),
                    p.length > 1 ? p[1] : "1");
        }

        @Override
        public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            renderTransparentBackground(g);
            panel(g, left, top, left + panelW, top + panelH, 0xEE10183A, 0xEE04060F, 0xFFCFEFFF);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            g.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(0xFFFFFF).withBold(true)), width / 2, top + 6, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.translatable("screen.enhanced.grant.subtitle").withStyle(ChatFormatting.GRAY),
                    width / 2, top + 18, 0xFFFFFFFF);
            int listW = panelW * 2 / 5;
            int cardsX = left + listW + 4;
            int cardW = (panelW - listW - 12) / 2;
            int ly = top + 30 + 3 * 26 + 8;
            // the strength: five stars of the path's colour
            for (int i = 0; i < WitchPath.MAX_LEVEL; i++) {
                int sx = cardsX + 28 + i * ((2 * cardW - 56) / WitchPath.MAX_LEVEL);
                int color = i < level ? 0xFF000000 | chosenPath.color : 0xFF303848;
                g.fill(sx, ly + 4, sx + 10, ly + 14, color);
                g.renderOutline(sx - 1, ly + 3, 12, 12, 0xFF000000 | chosenPath.glow);
            }
            Component sum = chosenPlayer >= 0 ? Component.translatable("screen.enhanced.grant.summary", names.get(chosenPlayer),
                    chosenPath.displayName(), level) : Component.translatable("screen.enhanced.grant.pick");
            List<FormattedCharSequence> lines = font.split(sum, 2 * cardW);
            for (int k = 0; k < Math.min(3, lines.size()); k++) {
                g.drawString(font, lines.get(k), cardsX, ly + 50 + k * 10, 0xFFD8E0EE, true);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    // ==================================================================================================================

    /**
     * The Road's End: "What do you seek?" A card for each prize offered (what the walker has is not among them); a prize
     * that needs a name (the player to find, the fallen to bring back, the path of witchcraft) shows its choices below.
     */
    static final class RoadPrizeScreen extends Screen {
        private final List<String> prizes;
        private final List<String> extra;
        private String chosen;
        private int left;
        private int top;
        private int panelW;

        RoadPrizeScreen(List<String> prizes, List<String> extra) {
            super(Component.translatable("screen.enhanced.road_prize.title"));
            this.prizes = prizes;
            this.extra = extra;
        }

        @Override
        protected void init() {
            panelW = Math.min(360, width - 20);
            left = (width - panelW) / 2;
            top = Math.max(8, height / 2 - 100);
            int n = Math.max(1, prizes.size());
            int cardW = (panelW - 12 - (n - 1) * 4) / n;
            for (int i = 0; i < prizes.size(); i++) {
                String prize = prizes.get(i);
                addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.road_prize." + prize), b -> pick(prize))
                        .bounds(left + 6 + i * (cardW + 4), top + 34, cardW, 22)
                        .tooltip(Tooltip.create(Component.translatable("screen.enhanced.road_prize." + prize + ".desc"))).build());
            }
            if (chosen != null) {
                String prefix = switch (chosen) {
                    case "find" -> "find:";
                    case "revive" -> "revive:";
                    case "witchcraft" -> "path:";
                    default -> null;
                };
                if (prefix != null) {
                    List<String> options = new ArrayList<>();
                    for (String e : extra) {
                        if (e.startsWith(prefix)) {
                            options.add(e.substring(prefix.length()));
                        }
                    }
                    int y = top + 70;
                    int col = 0;
                    int optW = (panelW - 16) / 3;
                    for (String option : options.subList(0, Math.min(12, options.size()))) {
                        Component label = prefix.equals("path:") && WitchPath.byId(option) != null
                                ? WitchPath.byId(option).displayName().copy().withStyle(s -> s.withColor(WitchPath.byId(option).color))
                                : Component.literal(option);
                        Button.Builder builder = Button.builder(label, b -> send(chosen, option))
                                .bounds(left + 6 + col * (optW + 2), y, optW, 18);
                        if (prefix.equals("path:") && WitchPath.byId(option) != null) {
                            builder.tooltip(Tooltip.create(WitchPath.byId(option).description()));
                        }
                        addRenderableWidget(builder.build());
                        col++;
                        if (col == 3) {
                            col = 0;
                            y += 20;
                        }
                    }
                }
            }
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                    .bounds(left + panelW / 2 - 50, top + 180, 100, 20).build());
        }

        private void pick(String prize) {
            if (prize.equals("unbound") || prize.equals("blessing")) {
                send(prize, "");
                return;
            }
            chosen = prize;
            rebuildWidgets();
        }

        private void send(String prize, String arg) {
            Net.sendToServer(new MagicActionPayload(MagicActionPayload.ROAD_PRIZE, prize, arg, 0));
            onClose();
        }

        @Override
        public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            renderTransparentBackground(g);
            panel(g, left, top, left + panelW, top + 206, 0xEE0E3236, 0xEE041214, 0xFF45C6D6);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            float pulse = 0.85F + 0.15F * Mth.sin((Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) * 0.15F);
            int glow = 0xFF000000 | (int) (0xBF * pulse) << 16 | (int) (0xF5 * pulse) << 8 | (int) (0xF0 * pulse);
            g.drawCenteredString(font, title.copy().withStyle(s -> s.withBold(true)), width / 2, top + 6, glow);
            g.drawCenteredString(font, Component.translatable("screen.enhanced.road_prize.subtitle").withStyle(ChatFormatting.GRAY),
                    width / 2, top + 19, 0xFFFFFFFF);
            if (chosen != null) {
                g.drawCenteredString(font, Component.translatable("screen.enhanced.road_prize." + chosen + ".pick"), width / 2, top + 60, 0xFFBFF5F0);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}

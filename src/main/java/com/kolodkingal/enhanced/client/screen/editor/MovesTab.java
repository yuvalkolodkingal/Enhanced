package com.kolodkingal.enhanced.client.screen.editor;

import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.client.screen.HeroEditorScreen;
import com.kolodkingal.enhanced.custom.AnimCatalog;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroLooks;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The Moves tab (3.4), "How does my hero move?". Its first page: how he flies (Plain, or as one of the fliers, each
 * with that hero's face), how he walks and runs (any hero's way), how he runs at super speed, and how he stands. Its
 * second page: the move each of his ten keys plays (Automatic: the hero's own move for a hero's ability, the usual one
 * for a power; No move; or one of the catalogue's moves) and the stance of each power he holds or switches on. Every
 * choice plays on the preview at once.
 */
public final class MovesTab extends EditorTab {
    private boolean keysPage;
    /** Where each row's label goes (worked out by {@link #init}). */
    private final int[] rowY = new int[4];

    public MovesTab(HeroEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int x, int y, int w, int h) {
        int half = (w - GAP) / 2;
        add(new CreatorUi.BigButton(x, y, half, 16, Component.translatable("screen.enhanced.hero_creator.moves.how"), null, draft().color(), () -> {
            keysPage = false;
            screen.rebuild();
        }).selected(!keysPage));
        add(new CreatorUi.BigButton(x + half + GAP, y, half, 16, Component.translatable("screen.enhanced.hero_creator.moves.keys"), null,
                draft().color(), () -> {
            keysPage = true;
            screen.rebuild();
        }).selected(keysPage));
        if (keysPage) {
            initKeys(x, y + 20, w, h - 20);
        } else {
            initHow(x, y + 20, w, h - 20);
        }
    }

    // ---- how he moves ----

    private void initHow(int x, int y, int w, int h) {
        CustomHero.Anim anim = draft().anim();
        int namedW = Math.max(50, (w - 4 * GAP) / 5);
        int yy = y + 11;
        rowY[0] = y;
        yy = flow(faces(AnimCatalog.FLIGHT_STYLES, anim.flight(), "flight", MovesTab::flierFace,
                s -> new CustomHero.Anim(s, anim.run(), anim.speedRun(), anim.idle()), HeroAnimator.PreviewMotion.FLY_CRUISE), x, yy, w, 18, 18);
        rowY[1] = yy;
        yy = flow(faces(AnimCatalog.RUN_CHARACTERS, anim.run(), "run", MovesTab::runnerFace,
                s -> new CustomHero.Anim(anim.flight(), s, anim.speedRun(), anim.idle()), HeroAnimator.PreviewMotion.RUN), x, yy + 11, w, 18, 18);
        rowY[2] = yy;
        yy = flow(named(AnimCatalog.SPEED_RUNS, anim.speedRun(), "speed_run",
                s -> new CustomHero.Anim(anim.flight(), anim.run(), s, anim.idle()), HeroAnimator.PreviewMotion.SPEED_RUN), x, yy + 11, w, namedW, 16);
        rowY[3] = yy;
        flow(named(AnimCatalog.IDLES, anim.idle(), "idle", s -> new CustomHero.Anim(anim.flight(), anim.run(), anim.speedRun(), s),
                HeroAnimator.PreviewMotion.IDLE), x, yy + 11, w, namedW, 16);
    }

    /** Picture buttons for a list of choices (Automatic first), each with a hero's face. */
    private List<Card> faces(List<String> styles, String current, String kind, Function<String, ResourceLocation> face,
                             Function<String, CustomHero.Anim> with, HeroAnimator.PreviewMotion motion) {
        List<Card> cards = new ArrayList<>();
        cards.add(choice(CustomHero.AUTO, current, kind, with, motion).icon(CreatorUi.icon("minecraft:compass")));
        for (String s : styles) {
            Card c = choice(s, current, kind, with, motion);
            ResourceLocation f = face.apply(s);
            cards.add(f == null ? c.icon(CreatorUi.icon(kind.equals("flight") ? "minecraft:feather" : "minecraft:leather_boots")) : c.face(f));
        }
        return cards;
    }

    /** Named buttons for a list of choices (Automatic first). */
    private List<Card> named(List<String> styles, String current, String kind, Function<String, CustomHero.Anim> with,
                             HeroAnimator.PreviewMotion motion) {
        List<Card> cards = new ArrayList<>();
        Card auto = choice(CustomHero.AUTO, current, kind, with, motion);
        cards.add(new Card(auto.tooltip(), null, null, null, auto.color(), auto.selected(), true, null, auto.action()));
        for (String s : styles) {
            Card c = choice(s, current, kind, with, motion);
            cards.add(new Card(c.tooltip(), null, null, null, c.color(), c.selected(), true, null, c.action()));
        }
        return cards;
    }

    private Card choice(String s, String current, String kind, Function<String, CustomHero.Anim> with, HeroAnimator.PreviewMotion motion) {
        return new Card(Component.empty(), null, null, null, draft().glow(), s.equals(current), true, animName(kind, s), () -> {
            set(draft().withAnim(with.apply(s)));
            screen.preview(motion);
        });
    }

    /** The name of an animation choice ({@code anim.enhanced.<kind>.<id>}; Automatic for {@code auto}). */
    public static MutableComponent animName(String kind, String id) {
        return CustomHero.AUTO.equals(id) ? Component.translatable("screen.enhanced.hero_creator.automatic")
                : Component.translatable("anim.enhanced." + kind + "." + id);
    }

    @Nullable
    private static ResourceLocation flierFace(String style) {
        return switch (style) {
            case "plain" -> null;
            case "white_vision" -> look("vision/white_vision");
            default -> heroFace(style);
        };
    }

    @Nullable
    private static ResourceLocation runnerFace(String style) {
        return switch (style) {
            case "default" -> null;
            case "white_vision" -> look("vision/white_vision");
            case "xavier_seated" -> look("professor_x/old");
            case "spider_surge" -> heroFace("spider");
            default -> heroFace(style);
        };
    }

    @Nullable
    private static ResourceLocation look(String ref) {
        HeroLooks.Look look = HeroLooks.byRef(ref);
        return look == null ? null : look.skin();
    }

    @Nullable
    private static ResourceLocation heroFace(String style) {
        Power p = switch (style) {
            case "wanda" -> Power.SCARLET_WITCH;
            case "spider" -> Power.SPIDER_MAN;
            case "xavier" -> Power.PROFESSOR_X;
            default -> Power.byName(style);
        };
        return p == null ? null : StartTab.face(p);
    }

    // ---- the keys' moves and the stances ----

    private void initKeys(int x, int y, int w, int h) {
        CustomHero hero = draft();
        int labelW = Math.min(52, w / 6);
        int cellW = (w - labelW - 2 - 4 * GAP) / CustomHero.KEYS;
        int gx = x + labelW + 2;
        for (int row = 0; row < 2; row++) {
            boolean sneak = row == 1;
            for (int c = 1; c <= CustomHero.KEYS; c++) {
                int col = c;
                boolean bound = !hero.binding(c, sneak).isEmpty() || sneak && !hero.binding(c, false).isEmpty();
                String move = hero.move(c, sneak);
                CreatorUi.BigButton b = add(new CreatorUi.BigButton(gx + (c - 1) * (cellW + GAP), y + 11 + row * 21, cellW, 18,
                        moveName(move), null, CustomHero.AUTO.equals(move) ? 0x909090 : draft().glow(), () -> chooseMove(col, sneak)));
                b.active = bound;
            }
        }
        int yy = y + 11 + 2 * 21 + 14;
        List<Card> stances = new ArrayList<>();
        for (CustomHero.Pick p : hero.powers()) {
            CatalogEntry e = p.entry();
            PowerModule.Act act = e == null ? PowerModule.Act.NONE : stanceAct(e, p.option());
            if (act != PowerModule.Act.HOLD && act != PowerModule.Act.TOGGLE) {
                continue;
            }
            stances.add(new Card(CreatorUi.name(e), stanceName(p.stance()), CreatorUi.icon(e.icon()), null, hero.colorOf(p),
                    false, true, null, () -> chooseStance(p, act)));
        }
        if (!stances.isEmpty()) {
            cards("stances", stances, x, yy, w, y + h - yy, 100, 24);
        }
    }

    /** How an entry acts for its stance: held or switched on, by its main action or its Sneak move (as the rules say). */
    private static PowerModule.Act stanceAct(CatalogEntry e, String option) {
        PowerModule.Act act = e.act(option);
        if (act == PowerModule.Act.HOLD || act == PowerModule.Act.TOGGLE) {
            return act;
        }
        PowerModule.Act alt = e.alt();
        return alt != null && HeroAbility.BOTH.equals(option) ? alt : act;
    }

    private static MutableComponent moveName(String move) {
        return CustomHero.AUTO.equals(move) ? Component.translatable("screen.enhanced.hero_creator.automatic")
                : CustomHero.NONE.equals(move) ? Component.translatable("screen.enhanced.hero_creator.no_move")
                : Component.translatable("anim.enhanced.gesture." + move);
    }

    private static MutableComponent stanceName(String stance) {
        return CustomHero.AUTO.equals(stance) ? Component.translatable("screen.enhanced.hero_creator.automatic")
                : CustomHero.NONE.equals(stance) ? Component.translatable("screen.enhanced.hero_creator.no_stance")
                : Component.translatable("anim.enhanced.stance." + stance);
    }

    private void chooseMove(int column, boolean sneak) {
        String now = draft().move(column, sneak);
        List<Popup.Item> items = new ArrayList<>();
        List<String> all = new ArrayList<>(List.of(CustomHero.AUTO, CustomHero.NONE));
        all.addAll(AnimCatalog.GESTURES);
        for (String m : all) {
            items.add(Popup.Item.of(moveName(m), CustomHero.AUTO.equals(m) ? Component.translatable("screen.enhanced.hero_creator.automatic.desc") : null,
                    draft().glow(), m.equals(now), () -> {
                set(draft().withMove(column, sneak, m));
                screen.tryBinding(column, sneak);
            }));
        }
        Component key = KeyBindings.ability(column).getTranslatedKeyMessage();
        screen.ask(new Popup(Component.translatable("screen.enhanced.hero_creator.move.choose",
                sneak ? Component.translatable("hud.enhanced.custom.sneak", key) : key), items));
    }

    private void chooseStance(CustomHero.Pick pick, PowerModule.Act act) {
        List<Popup.Item> items = new ArrayList<>();
        List<String> all = new ArrayList<>(List.of(CustomHero.AUTO, CustomHero.NONE));
        all.addAll(act == PowerModule.Act.HOLD ? AnimCatalog.STANCES_HOLD : AnimCatalog.STANCES_TOGGLE);
        for (String s : all) {
            items.add(Popup.Item.of(stanceName(s), null, draft().glow(), s.equals(pick.stance()), () -> {
                CustomHero.Pick p = draft().pick(pick.entry());
                if (p != null) {
                    set(draft().with(p.withStance(s)));
                    screen.tryEntry(p.entry());
                }
            }));
        }
        screen.ask(new Popup(Component.translatable("screen.enhanced.hero_creator.stance.choose", CreatorUi.name(pick.entry())), items));
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
        if (keysPage) {
            text(g, Component.translatable("screen.enhanced.hero_creator.moves.keys.hint").withStyle(ChatFormatting.GRAY), x, y + 20, w, 0xFFFFFF);
            int labelW = Math.min(52, w / 6);
            text(g, Component.translatable("screen.enhanced.hero_creator.key.press").withStyle(ChatFormatting.YELLOW), x, y + 36, labelW, 0xFFFFFF);
            text(g, Component.translatable("screen.enhanced.hero_creator.key.sneak").withStyle(ChatFormatting.YELLOW), x, y + 57, labelW, 0xFFFFFF);
            text(g, Component.translatable("screen.enhanced.hero_creator.stances").withStyle(ChatFormatting.GRAY), x, y + 20 + 11 + 42 + 3, w,
                    0xFFFFFF);
            return;
        }
        CustomHero.Anim anim = draft().anim();
        String[] kinds = {"flight", "run", "speed_run", "idle"};
        String[] values = {anim.flight(), anim.run(), anim.speedRun(), anim.idle()};
        for (int i = 0; i < 4; i++) {
            Component line = Component.translatable("screen.enhanced.hero_creator.moves." + kinds[i], animName(kinds[i], values[i])
                    .withStyle(s -> s.withColor(draft().glow()).withBold(true))).withStyle(ChatFormatting.GRAY);
            text(g, line, x, rowY[i] + 1, w, 0xFFFFFF);
        }
    }
}

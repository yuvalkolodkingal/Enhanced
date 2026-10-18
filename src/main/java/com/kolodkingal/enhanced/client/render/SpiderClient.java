package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.SpiderClimb;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.client.SpiderMotion;
import com.kolodkingal.enhanced.client.SpiderSenseClient;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The client side of Spider-Man, as a {@link HeroClient} plug-in (DESIGN section 14): the five lines of his HUD, which
 * of his stances are on (clinging to a wall or a ceiling, zipping, swinging, the Black Eyes, Instant Kill, the
 * spider-sense) and what they add to the body, and the effects that last for as long as he is in view. The rest of his
 * client side has a class of its own: {@link com.kolodkingal.enhanced.client.SpiderMotion} moves him, {@link
 * SpiderEffects} draws his webs, {@link SpiderLegsLayer} and {@link BlackEyesLayer} his legs and eyes, {@link
 * SpiderPoses} his pose and {@link com.kolodkingal.enhanced.client.SpiderSenseClient} his sense.
 *
 * <p>His animation: the keys of his fifteen gestures are {@link SpiderGestures}; the routes that start them are the
 * lines of his own in {@link GestureRoutes} and the methods below; what the client adds (the key press that acts before
 * the server answers, the zip his own movement begins, the landing, the push off a wall and the grab of one, his hands
 * in first person, the flourishes of his gestures) is {@link SpiderActing}, with {@link SpiderEdges} for the rules and
 * {@link SpiderGestureFx} for the flourishes. He does not fly, so he has no flight profile; his walk and sprint have a
 * character of their own in {@code RunPoses}.
 */
public final class SpiderClient implements HeroClient {
    /** The look of the Iron Spider suit. */
    private static final String IRON = SpiderServer.IRON_SPIDER;
    /**
     * The nanites count as scattered when the suit's armour has been missing from the Iron Spider for this many ticks.
     */
    private static final int STRIPPED_TICKS = 5;
    /**
     * The attribute modifier of the suit's armour (applied by the server to an Iron Spider whose nanites are whole).
     */
    private static final ResourceLocation SUIT_ARMOR = Enhanced.id("spider_suit_armor");

    private static int missingArmour;

    /**
     * The player as a plain {@code Player}. The game tests load this class (with the registry of the heroes) on a
     * server, where a class of the client may not be loaded, and the verifier loads a client class to find out that it
     * is a {@code Player} when a {@code LocalPlayer} is passed on as one; passed on as an {@code Object}, it needs no
     * loading.
     */
    private static Player asPlayer(Object player) {
        return (Player) player;
    }

    @Override
    public Power power() {
        return Power.SPIDER_MAN;
    }

    // ---- the HUD ----

    /**
     * The five lines (DESIGN section 14, "HUD"): R shows the Web Burst's cooldown or that his webs are organic; G shows
     * a zip or a swing in progress (since 3.3, otherwise whether Web Assist is on); V shows Wall Crawl on or off, clinging, the sense off, and danger; B shows what the
     * Iron Spider's systems are doing; N shows the Black Eyes' seconds. His cooldowns mean the seconds a mode has left
     * for B and N while it lasts, so these lines are tried before the generic grey seconds.
     */
    @Nullable
    @Override
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return switch (ability) {
            case 1 -> {
                long left = data.cooldownUntil(1) - now;
                yield left > 0 ? Component.translatable("hud.enhanced.burst_cooldown", seconds(left)).withStyle(ChatFormatting.GRAY)
                        : SpiderServer.organic(asPlayer(player), data) ? Component.translatable("hud.enhanced.organic").withStyle(ChatFormatting.AQUA) : null;
            }
            case 2 -> data.is(Live.SWINGING) ? Component.translatable("hud.enhanced.swinging").withStyle(ChatFormatting.AQUA)
                    : data.is(Live.ZIPPING) ? Component.translatable("hud.enhanced.zipping").withStyle(ChatFormatting.AQUA)
                    // 3.3: whether his swing is assisted (Web Assist, the J key) or manual.
                    : SpiderMotion.assistOn() ? Component.translatable("hud.enhanced.web_assist").withStyle(ChatFormatting.DARK_AQUA)
                    : Component.translatable("hud.enhanced.web_manual").withStyle(ChatFormatting.DARK_GRAY);
            case 3 -> crawl(data, now);
            case 4 -> ironSystems(player, data, now);
            case 5 -> {
                long left = data.cooldownUntil(5) - now;
                yield data.is(Live.BLACK_EYES) ? Component.translatable("hud.enhanced.on_for", seconds(left)).withStyle(ChatFormatting.RED)
                        : left > 0 ? Component.translatable("hud.enhanced.recharging", seconds(left)).withStyle(ChatFormatting.GRAY) : null;
            }
            default -> null;
        };
    }

    private static String seconds(long ticks) {
        return String.format(Locale.ROOT, "%.1fs", Math.max(0L, ticks) / 20.0);
    }

    private static Component crawl(PowerData data, long now) {
        boolean on = data.toggle(Toggle.WALL_CRAWL);
        MutableComponent line = Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
        if (data.is(Live.CLINGING_WALL) || data.is(Live.CLINGING_CEILING)) {
            line.append(Component.literal(" ")).append(Component.translatable("hud.enhanced.clinging").withStyle(ChatFormatting.AQUA));
        }
        if (!data.toggle(Toggle.SPIDER_SENSE)) {
            line.append(Component.literal(" ")).append(Component.translatable("hud.enhanced.sense_off").withStyle(ChatFormatting.DARK_GRAY));
        } else if (SpiderSenseClient.danger(now)) {
            line.append(Component.literal(" ")).append(Component.translatable("hud.enhanced.danger").withStyle(ChatFormatting.RED));
        }
        return line;
    }

    private static Component ironSystems(LocalPlayer player, PowerData data, long now) {
        if (!IRON.equals(Variants.current(data).id())) {
            return Component.translatable("hud.enhanced.iron_only").withStyle(ChatFormatting.DARK_GRAY);
        }
        if (scattered()) {
            return Component.translatable("hud.enhanced.nanites_scattered").withStyle(ChatFormatting.RED);
        }
        long left = data.cooldownUntil(4) - now;
        if (data.is(Live.INSTANT_KILL)) {
            return Component.translatable("hud.enhanced.instant_kill", seconds(left)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        }
        if (SpiderMotion.confirmPending(now)) {
            return Component.translatable("hud.enhanced.confirm").withStyle(ChatFormatting.GOLD);
        }
        if (left > 0) {
            return Component.translatable("hud.enhanced.recharging", seconds(left)).withStyle(ChatFormatting.GRAY);
        }
        boolean out = data.toggle(Toggle.SPIDER_LEGS);
        return Component.translatable(out ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(out ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    /**
     * Whether Magneto has scattered the nanites of the local Iron Spider. The server does not say it in the data; the
     * suit's armour does: it is missing from his armour attribute for as long as they are scattered (counted over a few
     * ticks, so that changing look does not flash it).
     */
    static boolean scattered() {
        return missingArmour >= STRIPPED_TICKS;
    }

    // ---- the animation: which stances are on, and what they add to the body ----

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        // The local hero's own movement knows at once what he is doing (the server's flags come a tick or two later and
        // outlast a cling by a few ticks); everyone else's is read from the synced data.
        boolean local = p == Minecraft.getInstance().player;
        SpiderMotion.Mode mode = SpiderMotion.mode();
        SpiderClimb.Kind surface = SpiderMotion.surface().kind();
        on[Stance.SPIDER_CLING_WALL.ordinal()] = local ? surface == SpiderClimb.Kind.WALL : data.is(Live.CLINGING_WALL);
        on[Stance.SPIDER_CLING_CEILING.ordinal()] = local ? surface == SpiderClimb.Kind.CEILING : data.is(Live.CLINGING_CEILING);
        on[Stance.SPIDER_SURGE.ordinal()] = data.is(Live.BLACK_EYES);
        on[Stance.SPIDER_KILL.ordinal()] = data.is(Live.INSTANT_KILL);
        on[Stance.SPIDER_ALERT.ordinal()] = data.is(Live.SENSING);
        on[Stance.SPIDER_ZIP.ordinal()] = local ? mode == SpiderMotion.Mode.ZIP : data.is(Live.ZIPPING);
        on[Stance.SPIDER_SWING.ordinal()] = local ? mode == SpiderMotion.Mode.SWING : data.is(Live.SWINGING);
        SpiderPoses.sample(s, p, data);
    }

    @Override
    public float lean(HeroAnimState st) {
        return SpiderGestures.lean(st);
    }

    @Override
    public float drop(HeroAnimState st) {
        return SpiderGestures.drop(st);
    }

    @Override
    public void ambient(PoseContext c) {
        SpiderPoses.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        SpiderPoses.aimed(c);
    }

    @Override
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        SpiderActing.edges(s, p, data);
    }

    @Nullable
    @Override
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        return SpiderActing.predicted(s, data, ability, now, sneaking);
    }

    @Override
    public void registerGestures() {
        SpiderGestures.register();
    }

    // ---- which gesture stands for what ----

    /**
     * The Web Burst: R's cooldown is the burst's alone (the Web Shot has a timer of its own that the data never shows).
     */
    @Nullable
    @Override
    public GestureId cooldownGesture(int ability, long left) {
        return ability == 1 && left > 0 ? GestureId.SPIDER_BURST : null;
    }

    @Nullable
    @Override
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case CASTING -> GestureId.SPIDER_SHOOT;
            case ZIPPING -> GestureId.SPIDER_ZIP_START;
            case SWINGING -> GestureId.SPIDER_SWING_START;
            case SENSING -> GestureId.SPIDER_ALERT_START;
            case BLACK_EYES -> GestureId.SPIDER_SURGE_ON;
            case INSTANT_KILL -> GestureId.SPIDER_KILL_ON;
            default -> null;
        };
    }

    /**
     * The landing, the push off a surface and the grab of a wall are the animator's own edges ({@link SpiderActing}).
     */
    @Override
    public boolean startedElsewhere(GestureId id) {
        return id == GestureId.SPIDER_LAND || id == GestureId.SPIDER_LEAP || id == GestureId.SPIDER_ARRIVE;
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        SpiderActing.started(player, id);
    }

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        SpiderActing.firstPerson(s, player, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null) {
            return;
        }
        SpiderEffects.tick(level);
        SpiderGestureFx.tick(level);
        SpiderSenseClient.tick(mc);
        if (level.getGameTime() % 100 == 0) {
            SpiderLegsLayer.prune(level);
        }
        boolean missing = false;
        if (player != null) {
            PowerData data = Powers.get(asPlayer(player));
            AttributeInstance armour = player.getAttribute(Attributes.ARMOR);
            missing = data.power() == Power.SPIDER_MAN && IRON.equals(Variants.current(data).id()) && armour != null
                    && !armour.hasModifier(SUIT_ARMOR) && EnhancedConfig.SPEC.isLoaded() && SpiderConfig.IRON_SPIDER_ARMOR.get() > 0.0;
        }
        missingArmour = missing ? Math.min(STRIPPED_TICKS + 1, missingArmour + 1) : 0;
    }

    @Override
    public void clear() {
        SpiderEffects.clear();
        SpiderGestureFx.clear();
        SpiderActing.clear();
        SpiderSenseClient.clear();
        SpiderLegsLayer.clear();
        SpiderMotion.reset();
        missingArmour = 0;
    }
}

package com.kolodkingal.enhanced.cottage;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Ctx;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Option;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Asgardians;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.config.CottageConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What the family of Agatha's Cottage says (3.7, the plan's 9.7): nodes of the Asgardians' dialogue table ({@link
 * AsgardDialogue}) for the roles AGATHA, NICKY and RIO, lines {@code npc.enhanced.agatha|nicky|rio.<node>}, answers
 * {@code npc.enhanced.option.cottage_*}. Our own words, warm and gentle, in English and Hebrew.
 *
 * <ul>
 *   <li><b>Agatha</b>: a greeting (her first one once), a line for each hero who visits, her family, the Road in the
 *   cellar, her trades, and Señor Scratchy's quest when he has run off.</li>
 *   <li><b>Nicky</b>: a cheerful kid: play, where his mom is, Scratchy, Rio, and a gift once a day (a cookie, a flower
 *   or his drawing of you).</li>
 *   <li><b>Rio</b>: calm and kind: the garden, a blessing once a day (Regeneration), her trades, and, only if asked who
 *   she really is, her one gentle line about being Death.</li>
 * </ul>
 * One of each (3.5.1): while a player is Agatha, the cottage's Agatha is away, and the lines that say where she is name
 * the player instead ({@code mom_player}, {@code agatha_player}); the player who is Agatha is greeted as family. (These
 * are picked by the answers' actions, not by {@code AsgardDialogue.swap}: the 3.5.1 tests check that every swap names
 * the player who is Odin.)
 */
@EventBusSubscriber(modid = Enhanced.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CottageDialogue {
    private static boolean registered;

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CottageDialogue::register);
    }

    /** The heroes Agatha (and Nicky) have a line for: their power → the node. */
    static final Map<Power, String> AGATHA_HEROES = Map.of(Power.BILLY, "hero_billy", Power.SCARLET_WITCH, "hero_wanda",
            Power.TOMMY, "hero_tommy", Power.WOLVERINE, "hero_wolverine", Power.DEADPOOL, "hero_deadpool",
            Power.DOCTOR_STRANGE, "hero_strange", Power.HULK, "hero_hulk");
    static final Map<Power, String> NICKY_HEROES = Map.of(Power.WOLVERINE, "hero_wolverine", Power.DEADPOOL,
            "hero_deadpool", Power.HULK, "hero_hulk", Power.BILLY, "hero_billy");

    // ---- the options ----

    static boolean trades() {
        return !EnhancedConfig.SPEC.isLoaded() || CottageConfig.TRADES.get();
    }

    static int giftsPerDay() {
        return EnhancedConfig.SPEC.isLoaded() ? CottageConfig.GIFTS_PER_DAY.get() : 1;
    }

    static int blessingTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? CottageConfig.BLESSING_TICKS.get() : 600;
    }

    // ---- what a talk knows ----

    @Nullable
    static Cottages.Cottage cottage(Ctx ctx) {
        AsgardianEntity npc = ctx.npc();
        return npc != null && npc.level() instanceof ServerLevel level ? Cottages.get(level).of(npc) : null;
    }

    static CottageProgress progress(Ctx ctx) {
        return ModAttachments.COTTAGE.get(ctx.player());
    }

    static boolean has(Ctx ctx, int flag) {
        return progress(ctx).has(flag);
    }

    static long day(Ctx ctx) {
        return Math.floorDiv(ctx.player().level().getDayTime(), 24000L);
    }

    static boolean scratchyGone(Ctx ctx) {
        Cottages.Cottage cottage = cottage(ctx);
        return cottage != null && cottage.scratchyGone();
    }

    /** Whether the talker is Agatha. */
    static boolean isAgatha(Ctx ctx) {
        return ctx.power() == Power.AGATHA;
    }

    /** Whether someone else online is Agatha (the cottage's Agatha is away). */
    static boolean otherAgatha(Ctx ctx) {
        return !isAgatha(ctx) && !Asgardians.heroesPlaying(ctx.player().server, Power.AGATHA).isEmpty();
    }

    static List<String> agathaNames(Ctx ctx) {
        return List.of(Asgardians.heroNames(ctx.player().server, Power.AGATHA));
    }

    static boolean mayGift(Ctx ctx) {
        return progress(ctx).giftsOn(day(ctx)) < giftsPerDay();
    }

    static boolean mayBless(Ctx ctx) {
        return blessingTicks() > 0 && progress(ctx).lastBlessingDay() != day(ctx);
    }

    static void met(Ctx ctx, int flag) {
        CottageProgress progress = progress(ctx);
        if (!progress.has(flag)) {
            ModAttachments.COTTAGE.set(ctx.player(), progress.with(flag));
        }
    }

    static Option trade() {
        return new Option("trade", ctx -> trades() && ctx.npc() != null && ctx.role().trader(), ctx -> {
            if (ctx.npc() != null) {
                ctx.npc().openTrades(ctx.player());
            }
        });
    }

    // ---- the table ----

    /** Registers the three roles' nodes (once; at common setup, and by the tests). */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        agatha();
        nicky();
        rio();
        AsgardDialogue.lines("npc.enhanced.nicky.scratchy_found", "npc.enhanced.agatha.scratchy_reward",
                "npc.enhanced.rio.scratchy_reward", "npc.enhanced.agatha.asleep", "npc.enhanced.nicky.asleep",
                "npc.enhanced.rio.asleep", "message.enhanced.cottage.mom_home", "message.enhanced.cottage.two_moms",
                "message.enhanced.cottage.door_sleeps", "item.enhanced.nicky_drawing", "item.enhanced.toy_broom",
                "item.enhanced.wooden_spoon", "item.enhanced.agatha_soup", Scratchy.NAME);
    }

    private static void agatha() {
        Role role = Role.AGATHA;
        List<Option> answers = List.of(
                Option.to("cottage_scratchy_help",
                        ctx -> scratchyGone(ctx) && !has(ctx, CottageProgress.SCRATCHY_QUEST), "scratchy_gone"),
                Option.to("cottage_family", "family"),
                new Option("cottage_road", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx,
                        CottageKeeper.roadDoor() ? "road" : "road_closed")),
                trade());
        AsgardDialogue.Action meet = ctx -> {
            met(ctx, CottageProgress.MET_AGATHA);
            Cottages.Cottage cottage = cottage(ctx);
            if (cottage != null && !cottage.scratchyGone() && has(ctx, CottageProgress.SCRATCHY_QUEST)) {
                ModAttachments.COTTAGE.set(ctx.player(), progress(ctx).without(CottageProgress.SCRATCHY_QUEST));
            }
        };
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(3).onShow(meet).options(answers)
                .register();
        AsgardDialogue.node(role, "greeting_first").when(ctx -> !has(ctx, CottageProgress.MET_AGATHA)).priority(20)
                .onShow(meet).options(answers).register();
        for (Map.Entry<Power, String> hero : AGATHA_HEROES.entrySet()) {
            AsgardDialogue.node(role, hero.getValue()).when(ctx -> ctx.power() == hero.getKey()).priority(30)
                    .onShow(meet).options(answers).register();
        }
        AsgardDialogue.node(role, "scratchy_gone")
                .when(ctx -> scratchyGone(ctx) && !has(ctx, CottageProgress.SCRATCHY_QUEST)).priority(40).onShow(meet)
                .option("cottage_scratchy_yes", AsgardDialogue.ALWAYS, ctx -> {
                    acceptQuest(ctx);
                    AsgardTalk.show(ctx, "scratchy_thanks");
                })
                .option(Option.close("later")).register();
        AsgardDialogue.node(role, "scratchy_thanks").option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "scratchy_looking")
                .when(ctx -> scratchyGone(ctx) && has(ctx, CottageProgress.SCRATCHY_QUEST)).priority(35)
                .option(Option.close("thanks")).option(Option.to("cottage_family", "family")).option(trade())
                .register();
        AsgardDialogue.node(role, "family").option(Option.to("cottage_about_nicky", "nicky"))
                .option(Option.to("cottage_about_rio", "rio")).option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "nicky").option(Option.to("cottage_about_rio", "rio")).option(Option.close("thanks"))
                .register();
        AsgardDialogue.node(role, "rio").option(Option.to("cottage_about_nicky", "nicky"))
                .option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "road").option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "road_closed").option(Option.close("thanks")).register();
    }

    private static void nicky() {
        Role role = Role.NICKY;
        List<Option> answers = List.of(
                new Option("cottage_gift", AsgardDialogue.ALWAYS, CottageDialogue::gift),
                Option.to("cottage_play", "play"),
                new Option("cottage_about_mom", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx,
                        isAgatha(ctx) ? "mom_you" : otherAgatha(ctx) ? "mom_player" : "mom")),
                new Option("cottage_about_scratchy", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx,
                        scratchyGone(ctx) ? "scratchy_gone" : "scratchy")));
        AsgardDialogue.Action meet = ctx -> met(ctx, CottageProgress.MET_NICKY);
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(3).onShow(meet).options(answers)
                .register();
        AsgardDialogue.node(role, "greeting_first").when(ctx -> !has(ctx, CottageProgress.MET_NICKY)).priority(20)
                .onShow(meet).options(answers).register();
        AsgardDialogue.node(role, "greeting_mom").when(CottageDialogue::isAgatha).priority(40).onShow(meet)
                .options(answers).register();
        for (Map.Entry<Power, String> hero : NICKY_HEROES.entrySet()) {
            AsgardDialogue.node(role, hero.getValue()).when(ctx -> ctx.power() == hero.getKey()).priority(30)
                    .onShow(meet).options(answers).register();
        }
        List<Option> more = List.of(Option.to("cottage_about_rio", "rio"), Option.to("cottage_play", "play"),
                Option.close("cottage_bye"));
        AsgardDialogue.node(role, "play").option(new Option("cottage_about_scratchy", AsgardDialogue.ALWAYS,
                ctx -> AsgardTalk.show(ctx, scratchyGone(ctx) ? "scratchy_gone" : "scratchy")))
                .option(Option.to("cottage_about_rio", "rio")).option(Option.close("cottage_bye")).register();
        AsgardDialogue.node(role, "mom").options(more).register();
        AsgardDialogue.node(role, "mom_player").args(CottageDialogue::agathaNames).options(more).register();
        AsgardDialogue.node(role, "mom_you").options(more).register();
        AsgardDialogue.node(role, "gift").option(Option.close("thanks")).register();
        AsgardDialogue.node(role, "gift_done").option(Option.to("cottage_play", "play"))
                .option(Option.close("cottage_bye")).register();
        AsgardDialogue.node(role, "rio").option(Option.to("cottage_play", "play")).option(Option.close("cottage_bye"))
                .register();
        AsgardDialogue.node(role, "scratchy").option(Option.to("cottage_play", "play"))
                .option(Option.close("cottage_bye")).register();
        AsgardDialogue.node(role, "scratchy_gone").option("cottage_scratchy_yes", AsgardDialogue.ALWAYS,
                CottageDialogue::acceptQuest).option(Option.close("later")).register();
    }

    /** He will look for Señor Scratchy: he sees him glow (within 24 blocks) until he is home. */
    static void acceptQuest(Ctx ctx) {
        ModAttachments.COTTAGE.set(ctx.player(), progress(ctx).with(CottageProgress.SCRATCHY_QUEST));
    }

    /** Nicky's gift (once a day, {@code giftsPerDay}): a cookie, a flower, or his drawing of you. */
    static void gift(Ctx ctx) {
        ServerPlayer player = ctx.player();
        if (!mayGift(ctx)) {
            AsgardTalk.show(ctx, "gift_done");
            return;
        }
        ItemStack gift = switch (player.getRandom().nextInt(3)) {
            case 0 -> new ItemStack(Items.COOKIE, 3);
            case 1 -> new ItemStack(List.of(Items.POPPY, Items.DANDELION, Items.CORNFLOWER, Items.ALLIUM)
                    .get(player.getRandom().nextInt(4)));
            default -> CottageGoals.named(Items.PAPER, "item.enhanced.nicky_drawing");
        };
        ModAttachments.COTTAGE.set(player, progress(ctx).gifted(day(ctx)));
        Scratchy.give(player, gift);
        if (ctx.npc() != null && player.level() instanceof ServerLevel level) {
            AsgardianEntity nicky = ctx.npc();
            level.sendParticles(ParticleTypes.HEART, nicky.getX(), nicky.getY() + nicky.getBbHeight() + 0.3,
                    nicky.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
        }
        AsgardTalk.show(ctx, "gift");
    }

    private static void rio() {
        Role role = Role.RIO;
        List<Option> answers = List.of(
                new Option("cottage_blessing", CottageDialogue::mayBless, CottageDialogue::bless),
                trade(),
                Option.to("cottage_chat", "chat"));
        AsgardDialogue.Action meet = ctx -> met(ctx, CottageProgress.MET_RIO);
        AsgardDialogue.node(role, "greeting").when(AsgardDialogue.ALWAYS).variants(3).onShow(meet).options(answers)
                .register();
        AsgardDialogue.node(role, "greeting_first").when(ctx -> !has(ctx, CottageProgress.MET_RIO)).priority(20)
                .onShow(meet).options(answers).register();
        AsgardDialogue.node(role, "greeting_agatha").when(CottageDialogue::isAgatha).priority(40).onShow(meet)
                .options(answers).register();
        AsgardDialogue.node(role, "chat").option(Option.to("cottage_about_garden", "garden"))
                .option(new Option("cottage_about_agatha", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx,
                        isAgatha(ctx) ? "agatha_you" : otherAgatha(ctx) ? "agatha_player" : "agatha")))
                .option(Option.to("cottage_who", "who")).register();
        List<Option> back = List.of(Option.to("cottage_chat", "chat"), Option.close("thanks"));
        AsgardDialogue.node(role, "garden").options(back).register();
        AsgardDialogue.node(role, "agatha").options(back).register();
        AsgardDialogue.node(role, "agatha_player").args(CottageDialogue::agathaNames).options(back).register();
        AsgardDialogue.node(role, "agatha_you").options(back).register();
        AsgardDialogue.node(role, "who").options(back).register();
        AsgardDialogue.node(role, "blessing").option(Option.close("thanks")).register();
    }

    /** Rio's blessing (once a day): Regeneration I for {@code blessingTicks}, a green sparkle, a soft chime. */
    static void bless(Ctx ctx) {
        ServerPlayer player = ctx.player();
        if (!mayBless(ctx)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, blessingTicks(), 0));
        ModAttachments.COTTAGE.set(player, progress(ctx).withLastBlessingDay(day(ctx)));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 16,
                    0.4, 0.6, 0.4, 0.0);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RIO_BLESSING.get(),
                    player.getSoundSource(), 1.0F, 1.0F);
        }
        AsgardTalk.show(ctx, "blessing");
    }

    /** Every node key of a role (for the tests). */
    static List<String> nodes(Role role) {
        List<String> keys = new ArrayList<>();
        for (AsgardDialogue.Node node : AsgardDialogue.nodes(role)) {
            keys.add(node.key());
        }
        return keys;
    }

    private CottageDialogue() {
    }
}

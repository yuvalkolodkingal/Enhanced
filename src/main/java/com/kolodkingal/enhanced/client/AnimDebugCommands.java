package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

/**
 * Client-side tools for looking at the heroes' animation, under {@code /enhanced_anim}: pose a flight without flying that
 * fast, play a gesture and freeze it at an exact tick for a screenshot, and show the numbers behind the pose.
 *
 * <pre>
 * /enhanced_anim fly &lt;s&gt; &lt;u&gt; &lt;turn&gt;   pretend the motion of the local player: speed (1 cruise, 2 sprint), climb (-1..1), turn (degrees per tick)
 * /enhanced_anim fly off
 * /enhanced_anim play &lt;gesture&gt; [hold]  start a gesture on yourself, ignoring its trigger (and freeze it at that tick)
 * /enhanced_anim target &lt;name|self&gt;  act on another player in view instead of yourself (to photograph their frozen pose from every side)
 * /enhanced_anim stance &lt;stance&gt; on|off|auto   force a stance on or off for yourself (/enhanced_anim stance off: all back to the data)
 * /enhanced_anim hold &lt;ticks&gt;         jump every playing gesture to this tick and freeze the animation of the local player
 * /enhanced_anim release              let it run again
 * /enhanced_anim state                toggle the overlay of the numbers
 * /enhanced_anim become &lt;file&gt;        become the custom hero of enhanced/heroes/&lt;file&gt;.json
 * </pre>
 */
@Subscriber(side = Side.CLIENT)
public final class AnimDebugCommands {

    /** The command's registration (port P0: through the platform's client registrar, see ClientSetup). */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("enhanced_anim")
                .then(Commands.literal("fly")
                        .then(Commands.literal("off").executes(ctx -> {
                            HeroAnimator.debugFly(false, 0.0F, 0.0F, 0.0F);
                            return say(ctx.getSource(), "motion: measured");
                        }))
                        .then(Commands.argument("s", FloatArgumentType.floatArg(-1.0F, 3.0F))
                                .then(Commands.argument("u", FloatArgumentType.floatArg(-1.0F, 1.0F))
                                        .then(Commands.argument("turn", FloatArgumentType.floatArg(-20.0F, 20.0F)).executes(ctx -> {
                                            HeroAnimator.debugFly(true, FloatArgumentType.getFloat(ctx, "s"), FloatArgumentType.getFloat(ctx, "u"),
                                                    FloatArgumentType.getFloat(ctx, "turn"));
                                            return say(ctx.getSource(), "motion: pretended");
                                        })))))
                .then(Commands.literal("play")
                        .then(Commands.argument("gesture", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(GestureId.values()).map(Enum::name), builder))
                                .executes(ctx -> play(ctx.getSource(), StringArgumentType.getString(ctx, "gesture"), -1))
                                .then(Commands.argument("hold", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> play(ctx.getSource(), StringArgumentType.getString(ctx, "gesture"),
                                                IntegerArgumentType.getInteger(ctx, "hold"))))))
                .then(Commands.literal("target")
                        .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return say(ctx.getSource(), HeroAnimator.debugTarget(name) ? "tools act on " + name : "nobody called " + name + " in view");
                        })))
                .then(Commands.literal("stance")
                        .then(Commands.literal("off").executes(ctx -> {
                            HeroAnimator.debugStance(null, null);
                            return say(ctx.getSource(), "stances: from the data");
                        }))
                        .then(Commands.argument("stance", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Stance.VALUES).map(Enum::name), builder))
                                .then(Commands.literal("on").executes(ctx -> stance(ctx.getSource(), StringArgumentType.getString(ctx, "stance"), true)))
                                .then(Commands.literal("off").executes(ctx -> stance(ctx.getSource(), StringArgumentType.getString(ctx, "stance"), false)))
                                .then(Commands.literal("auto").executes(ctx -> stance(ctx.getSource(), StringArgumentType.getString(ctx, "stance"), null)))))
                .then(Commands.literal("hold")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                            HeroAnimator.debugHold(IntegerArgumentType.getInteger(ctx, "ticks"));
                            return say(ctx.getSource(), "held");
                        })))
                .then(Commands.literal("become")
                        .then(Commands.argument("file", StringArgumentType.word())
                                .executes(ctx -> become(ctx.getSource(), StringArgumentType.getString(ctx, "file")))))
                .then(Commands.literal("release").executes(ctx -> {
                    HeroAnimator.debugRelease();
                    return say(ctx.getSource(), "released");
                }))
                .then(Commands.literal("state").executes(ctx -> {
                    HeroAnimator.debugOverlay = !HeroAnimator.debugOverlay;
                    return say(ctx.getSource(), "overlay " + (HeroAnimator.debugOverlay ? "on" : "off"));
                })));
    }

    /** Starts a gesture on the local player and, with a tick, freezes it there at once (for an exact-frame screenshot). */
    private static int play(CommandSourceStack source, String name, int hold) {
        try {
            HeroAnimator.debugPlay(GestureId.valueOf(name));
        } catch (IllegalArgumentException e) {
            return say(source, "no gesture " + name);
        }
        if (hold >= 0) {
            HeroAnimator.debugHold(hold);
        }
        return say(source, "playing " + name + (hold >= 0 ? " held at " + hold : ""));
    }

    /** Forces a stance on or off (or back to the data) for the local player. */
    private static int stance(CommandSourceStack source, String name, Boolean on) {
        try {
            HeroAnimator.debugStance(Stance.valueOf(name), on);
        } catch (IllegalArgumentException e) {
            return say(source, "no stance " + name);
        }
        return say(source, name + (on == null ? ": from the data" : on ? ": forced on" : ": forced off"));
    }

    /**
     * 3.4: becomes the custom hero of {@code enhanced/heroes/<file>.json} at once (as the creator's "Become this hero!"
     * does: the server judges it and may refuse), to look at a custom hero's animation without clicking through the editor.
     */
    private static int become(CommandSourceStack source, String file) {
        java.nio.file.Path path = com.kolodkingal.enhanced.client.custom.HeroFiles.heroesDir().resolve(file + ".json");
        com.kolodkingal.enhanced.custom.CustomHeroJson.Read read = com.kolodkingal.enhanced.client.custom.HeroFiles.read(path);
        if (!read.ok()) {
            return say(source, "cannot read " + path + ": " + read.notes());
        }
        com.kolodkingal.enhanced.platform.net.Net.sendToServer(com.kolodkingal.enhanced.network.HeroCreatorPayload.apply(read.hero()));
        return say(source, "becoming " + read.hero().name());
    }

    private static int say(CommandSourceStack source, String text) {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[anim] " + text));
        return 1;
    }

    private AnimDebugCommands() {
    }
}

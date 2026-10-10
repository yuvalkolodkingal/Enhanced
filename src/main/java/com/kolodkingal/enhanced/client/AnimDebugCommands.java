package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Arrays;

/**
 * Client-side tools for looking at the heroes' animation, under {@code /enhanced_anim}: pose a flight without flying that
 * fast, play a gesture and freeze it at an exact tick for a screenshot, and show the numbers behind the pose.
 *
 * <pre>
 * /enhanced_anim fly &lt;s&gt; &lt;u&gt; &lt;turn&gt;   pretend the motion of the local player: speed (1 cruise, 2 sprint), climb (-1..1), turn (degrees per tick)
 * /enhanced_anim fly off
 * /enhanced_anim play &lt;gesture&gt; [hold]  start a gesture on yourself, ignoring its trigger (and freeze it at that tick)
 * /enhanced_anim hold &lt;ticks&gt;         jump every playing gesture to this tick and freeze the animation of the local player
 * /enhanced_anim release              let it run again
 * /enhanced_anim state                toggle the overlay of the numbers
 * </pre>
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class AnimDebugCommands {

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("enhanced_anim")
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
                .then(Commands.literal("hold")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                            HeroAnimator.debugHold(IntegerArgumentType.getInteger(ctx, "ticks"));
                            return say(ctx.getSource(), "held");
                        })))
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

    private static int say(CommandSourceStack source, String text) {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[anim] " + text));
        return 1;
    }

    private AnimDebugCommands() {
    }
}

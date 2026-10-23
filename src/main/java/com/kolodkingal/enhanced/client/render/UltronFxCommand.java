package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code /enhanced_ultron_fx <effect> [ticks]}: plays one effect of the Ultron fight on this client only, a few blocks in
 * front of you on the ground, as if the server had sent it (the way {@code /enhanced_model} shows a body without a
 * fight). For the people who draw and tune the tells: nothing is sent to a server and nothing in the world changes. The
 * effects that belong to a body ({@code beams_aim}, {@code beams_fire}, {@code glint}, {@code death}) use the body that
 * {@code /enhanced_model ultron} is showing, or the nearest one.
 */
@Subscriber(side = Side.CLIENT)
public final class UltronFxCommand {
    /** An effect the command can play: its kind, how long it lasts by default, and its radius. */
    private record Spec(int kind, int ticks, float radius, boolean fromBody) {
    }

    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        SPECS.put("assemble", new Spec(UltronFxPayload.ASSEMBLE, 50, 3.4F, false));
        SPECS.put("flash", new Spec(UltronFxPayload.FLASH, 14, 6.0F, false));
        SPECS.put("glint", new Spec(UltronFxPayload.GLINT, 6, 0.0F, true));
        SPECS.put("beams_aim", new Spec(UltronFxPayload.BEAMS_AIM, 5, 0.0F, true));
        SPECS.put("beams_fire", new Spec(UltronFxPayload.BEAMS_FIRE, 20, 0.0F, true));
        SPECS.put("dive_ring", new Spec(UltronFxPayload.DIVE_RING, 12, 4.0F, false));
        SPECS.put("debris_ring", new Spec(UltronFxPayload.DEBRIS_RING, 30, 3.0F, false));
        SPECS.put("pull_ring", new Spec(UltronFxPayload.PULL_RING, 24, 16.0F, false));
        SPECS.put("spawn_pillar", new Spec(UltronFxPayload.SPAWN_PILLAR, 30, 1.0F, false));
        SPECS.put("shockwave", new Spec(UltronFxPayload.SHOCKWAVE, 12, 6.0F, false));
        SPECS.put("data_line", new Spec(UltronFxPayload.DATA_LINE, 40, 0.0F, false));
        SPECS.put("wreck", new Spec(UltronFxPayload.WRECK, 200, 0.0F, false));
        SPECS.put("tremor", new Spec(UltronFxPayload.TREMOR, 120, 20.0F, false));
        SPECS.put("lift_ring", new Spec(UltronFxPayload.LIFT_RING, 200, 20.0F, false));
        SPECS.put("death", new Spec(UltronFxPayload.DEATH, 60, 0.0F, true));
        SPECS.put("pulse", new Spec(UltronFxPayload.PULSE, 20, 0.0F, true));
        SPECS.put("gravity_slam", new Spec(UltronFxPayload.GRAVITY_SLAM, 10, 7.0F, false));
        SPECS.put("gravity_pull", new Spec(UltronFxPayload.GRAVITY_PULL, 8, 0.0F, false));
        SPECS.put("overcharge", new Spec(UltronFxPayload.OVERCHARGE_FLASH, 6, 2.0F, false));
        SPECS.put("network", new Spec(UltronFxPayload.NETWORK_FLASH, 10, 2.0F, false));
        SPECS.put("drone", new Spec(UltronFxPayload.DRONE_ASSEMBLE, 20, 0.0F, false));
        SPECS.put("hack", new Spec(UltronFxPayload.HACK_BEAM, 20, 0.0F, false));
        SPECS.put("command", new Spec(UltronFxPayload.COMMAND_MARK, 10, 1.5F, false));
    }

    /** The command's registration (port P0: through the platform's client registrar, see ClientSetup). */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("enhanced_ultron_fx")
                .then(Commands.argument("effect", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SPECS.keySet(), builder))
                        .executes(ctx -> play(StringArgumentType.getString(ctx, "effect"), -1))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 2000))
                                .executes(ctx -> play(StringArgumentType.getString(ctx, "effect"), IntegerArgumentType.getInteger(ctx, "ticks"))))));
    }

    private static int play(String name, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        Spec spec = SPECS.get(name);
        if (spec == null || level == null || player == null) {
            say(spec == null ? "no effect called " + name + ": " + String.join(", ", SPECS.keySet()) : "not in a world");
            return 0;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 ground = new Vec3(player.getX() + flat.x * 7.0, player.getY(), player.getZ() + flat.z * 7.0);
        Entity body = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof UltronEntity && entity.distanceToSqr(player) < best) {
                best = entity.distanceToSqr(player);
                body = entity;
            }
        }
        if (spec.fromBody() && body == null) {
            say(name + " needs a body: show one with /enhanced_model ultron idle");
            return 0;
        }
        int length = ticks > 0 ? ticks : spec.ticks();
        Vec3 at = ground;
        Vec3 to = Vec3.ZERO;
        int entity = -1;
        if (spec.fromBody()) {
            entity = body.getId();
            at = switch (spec.kind()) {
                case UltronFxPayload.GLINT -> body.position().add(0, body.getBbHeight() * 0.62, 0);
                case UltronFxPayload.DEATH, UltronFxPayload.PULSE -> body.position();
                default -> player.position();
            };
            if (spec.kind() == UltronFxPayload.GLINT) {
                to = player.position().add(0, 1.1, 0);
            }
        } else {
            switch (spec.kind()) {
                case UltronFxPayload.DATA_LINE -> {
                    at = ground.add(0, 1.0, 0);
                    to = ground.add(flat.cross(new Vec3(0, 1, 0)).scale(6.0)).add(0, 2.0, 0);
                }
                case UltronFxPayload.GRAVITY_PULL -> {
                    at = player.getEyePosition().add(look.scale(0.6)).add(0, -0.3, 0);
                    to = ground.add(0, 1.0, 0);
                }
                case UltronFxPayload.HACK_BEAM -> {
                    at = player.getEyePosition().add(look.scale(0.6)).add(0, -0.3, 0);
                    to = ground.add(0, 1.0, 0);
                }
                case UltronFxPayload.OVERCHARGE_FLASH -> {
                    at = player.getEyePosition().add(look.scale(1.5));
                    to = look;
                }
                case UltronFxPayload.NETWORK_FLASH -> at = ground.add(0, 1.0, 0);
                case UltronFxPayload.COMMAND_MARK -> at = ground.add(0, 1.0, 0);
                default -> {
                }
            }
        }
        if (spec.kind() == UltronFxPayload.BEAMS_AIM || spec.kind() == UltronFxPayload.BEAMS_FIRE) {
            at = ground;
        }
        UltronEffects.receive(new UltronFxPayload(spec.kind(), entity, at, to, spec.radius(), length));
        say("playing " + name + " for " + length + " ticks");
        return 1;
    }

    private static void say(String text) {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[ultron fx] " + text));
    }

    private UltronFxCommand() {
    }
}

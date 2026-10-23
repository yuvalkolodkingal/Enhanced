package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.platform.event.LevelEvent;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The model viewer: a client-only copy of a boss, put in front of you and held in a pose, so that the art of a boss can
 * be looked at, state by state and tick by tick, without running a fight. Like {@code /enhanced_anim} it exists only on
 * the client, changes nothing on a server and is for the people who draw the models.
 *
 * <pre>
 * /enhanced_model list                         the bosses that can be shown, and their states
 * /enhanced_model &lt;boss&gt; &lt;state&gt; [tick]  show it in that state (frozen at the tick, or running from it)
 * /enhanced_model clear                        take it away
 * </pre>
 *
 * A boss lane registers a {@link Subject} for each of its models ({@code ThanosModel}, {@code UltronModel}, {@code
 * ApocalypseModel}, a Sentry): the entity type to show and how to put it in a state. The viewer makes the entity on
 * the client only, never adds it to a server, and keeps it where it was put (no AI, no gravity, no collision).
 */
@Subscriber(side = Side.CLIENT)
public final class ModelViewer {
    /** How far in front of the player the model stands, in blocks. */
    private static final double DISTANCE = 5.0;

    /** What a boss lane says about one model. */
    public interface Subject {
        /** The entity the model belongs to. */
        EntityType<?> type();

        /** The states the viewer offers, as the lane names them ("idle", "slam_tell", "dying"). */
        List<String> states();

        /**
         * Puts the entity in the state, as it would be {@code tick} ticks into it (the lane sets whatever the model
         * reads: its synced state, its tick count, its attack timer). Called again every client tick while shown.
         */
        void pose(Entity entity, String state, int tick);
    }

    private static final Map<String, Subject> SUBJECTS = new LinkedHashMap<>();

    @Nullable
    private static Entity shown;
    @Nullable
    private static Subject subject;
    private static String state = "";
    private static int tick;
    private static boolean frozen;

    /** Offers a model to the viewer under a name ({@code thanos}, {@code ultron_sentry}). */
    public static void register(String name, Subject subject) {
        SUBJECTS.put(name, subject);
    }

    /** The command's registration (port P0: through the platform's client registrar, see ClientSetup). */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("enhanced_model")
                .then(Commands.literal("list").executes(ctx -> list()))
                .then(Commands.literal("clear").executes(ctx -> {
                    clear();
                    return say("cleared");
                }))
                .then(Commands.argument("boss", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SUBJECTS.keySet(), builder))
                        .then(Commands.argument("state", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    Subject chosen = SUBJECTS.get(StringArgumentType.getString(ctx, "boss"));
                                    return SharedSuggestionProvider.suggest(chosen == null ? List.of() : chosen.states(), builder);
                                })
                                .executes(ctx -> show(StringArgumentType.getString(ctx, "boss"),
                                        StringArgumentType.getString(ctx, "state"), 0, false))
                                .then(Commands.argument("tick", IntegerArgumentType.integer(0, 2000))
                                        .executes(ctx -> show(StringArgumentType.getString(ctx, "boss"),
                                                StringArgumentType.getString(ctx, "state"), IntegerArgumentType.getInteger(ctx, "tick"), true))))));
    }

    private static int list() {
        if (SUBJECTS.isEmpty()) {
            return say("no model is registered yet (a boss lane registers its own with ModelViewer.register)");
        }
        SUBJECTS.forEach((name, subject) -> say(name + ": " + String.join(", ", subject.states())));
        return SUBJECTS.size();
    }

    private static int show(String name, String requested, int atTick, boolean freeze) {
        Subject chosen = SUBJECTS.get(name);
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (chosen == null || level == null || player == null) {
            return say(chosen == null ? "no model called " + name : "not in a world");
        }
        if (!chosen.states().contains(requested)) {
            return say(name + " has no state " + requested + ": " + String.join(", ", chosen.states()));
        }
        clear();
        Entity entity = chosen.type().create(level);
        if (entity == null) {
            return say("could not make " + name);
        }
        entity.noPhysics = true;
        entity.setNoGravity(true);
        entity.setInvulnerable(true);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
        subject = chosen;
        shown = entity;
        state = requested;
        tick = atTick;
        frozen = freeze;
        place(player, entity);
        chosen.pose(entity, state, tick);
        level.addEntity(entity);
        return say("showing " + name + " in " + requested + (freeze ? " frozen at tick " + atTick : ""));
    }

    /** In front of the player, turned to face them. */
    private static void place(LocalPlayer player, Entity entity) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 at = player.position().add(flat.scale(DISTANCE));
        float yaw = player.getYRot() + 180.0F;
        entity.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.yRotO = yaw;
    }

    private static void clear() {
        if (shown != null) {
            shown.discard();
        }
        shown = null;
        subject = null;
    }

    @Subscribe
    public static void onTick(ClientTickEvent.Post event) {
        if (shown == null) {
            return;
        }
        if (shown.isRemoved()) {
            clear();
            return;
        }
        if (!frozen) {
            tick++;
        }
        shown.tickCount = tick;
        subject.pose(shown, state, tick);
    }

    @Subscribe
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            shown = null;
            subject = null;
        }
    }

    private static int say(String text) {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[model] " + text));
        return 1;
    }

    private ModelViewer() {
    }
}

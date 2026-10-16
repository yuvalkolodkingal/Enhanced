package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.network.OpenRoadPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The end of the Road (3.2). The player who has won every trial steps through the last gate and is brought to {@link EndingHall};
 * there the Road asks "Seekest thou the Road?" once more and opens the choice screen inside the Road: Billy (Wiccan) or Tommy (Speed).
 * <ul>
 *     <li>The choice is checked here and nowhere else, whatever the client sends: he is in the hall, in the Road dimension, alive, has no
 *     power, his saved progress says he has won every trial and is inside, the hall has asked him, and he carries a Scarlet Witch Gene.
 *     Then the Gene is used up (one, and only now) and he awakens exactly as at the door ({@link Powers#awaken}).</li>
 *     <li>The farewell: for {@code endingFarewellSeconds} he is held where he stands in a rising column of light and the Road speaks its
 *     farewell. The Road's rules still hold (no ability key, no teleport out: the Vishanti, the Darkhold and the Bifrost are shut in this
 *     dimension, {@link RoadTrials#allowsTeleport} refuses the rest).</li>
 *     <li>Then he is sent out ({@link RoadTrials#leave}): to the place he entered by, or failing that the door he opened, or the world's
 *     spawn; his things come back, his hunger and health are full, every effect is cleared, the cell is taken down and the Road forgets
 *     his progress (the next Road he can walk, with another Gene and no power, is dealt other courses).</li>
 *     <li>He may close the screen: the door in the hall and {@code /road choose} ask again; the screen has a button to leave the Road
 *     without choosing (the Gene is kept; the door in the world then offers the choice).</li>
 *     <li>A logout or a crash in the hall: at the next login the hall is built again and the question comes back. A logout in the farewell:
 *     the power is his already, and the login sends him out ({@link RoadTrials#reconcile}).</li>
 * </ul>
 * All the state is in memory, about the hall that exists; what lasts is the player's progress ({@link RoadProgress}).
 */
public final class RoadEnding {
    /** How long the Road gathers its light before it asks (3 s). */
    public static final int ASK_DELAY_TICKS = 60;

    /** Where a player in the hall is. */
    public enum Stage {
        /** The light gathers; the question is not asked yet. */
        ARRIVING,
        /** The question has been asked; the player decides. */
        ASKING,
        /** He has chosen and awakened; the Road says its farewell. */
        FAREWELL
    }

    private static final class State {
        Stage stage = Stage.ARRIVING;
        final long since;
        long chosenAt;
        int total;
        Power chosen = Power.NONE;
        Vec3 anchor = Vec3.ZERO;
        float yaw;

        State(long since) {
            this.since = since;
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // Who is where

    /** Whether the player is in the hall (before the answer, or in the farewell). */
    public static boolean isAtEnding(ServerPlayer player) {
        return stage(player) != null;
    }

    /** Where the player is in the ending, or null if he is not in the hall. */
    @Nullable
    public static Stage stage(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        RoadTrial trial = RoadTrials.trialOf(player.getUUID());
        return state != null && trial != null && trial.challenge().isEnding() && RoadDimension.in(player) ? state.stage : null;
    }

    /** Whether the player has chosen and is being sent out (a power in the Road is no reason to take him out before that). */
    public static boolean isLeaving(ServerPlayer player) {
        return stage(player) == Stage.FAREWELL;
    }

    /** The hall of the player is taken down (or the server stopped): nothing is remembered about it. */
    static void forget(UUID player) {
        STATES.remove(player);
    }

    static void clear() {
        STATES.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // In the hall

    /** The player has been put in the hall (he came through the last gate, or logged back in): the Road speaks and the light gathers. */
    static void arrive(RoadTrial trial, ServerPlayer player) {
        ServerLevel level = trial.level();
        STATES.put(player.getUUID(), new State(level.getGameTime()));
        if (WitchesRoadConfig.VOICE.get()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.enhanced.road.ending.title")
                    .withStyle(s -> s.withColor(WitchesRoad.LIGHT).withBold(true))));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("screen.enhanced.road.title")
                    .withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE).withItalic(true))));
        }
        RoadVoice.say(player, RoadVoice.ENDING, trial.progress().started());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WITCHES_ROAD.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    /** Every tick the player is in the hall. */
    static void tick(RoadTrial trial, ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            arrive(trial, player);
            return;
        }
        ServerLevel level = trial.level();
        long now = level.getGameTime();
        if (state.stage != Stage.FAREWELL && trial.progress().completed() < RoadChallenges.count()) {
            // An operator took his trials back: he has not walked the Road after all.
            player.displayClientMessage(Component.translatable("message.enhanced.road.ending.undone").withStyle(ChatFormatting.GRAY), true);
            RoadTrials.leave(player, RoadChallenge.End.LEFT);
            return;
        }
        switch (state.stage) {
            case ARRIVING -> {
                glow(level, trial, now);
                if (now - state.since >= ASK_DELAY_TICKS) {
                    state.stage = Stage.ASKING;
                    ask(trial, player);
                }
            }
            case ASKING -> glow(level, trial, now);
            case FAREWELL -> farewell(trial, player, state, now);
        }
    }

    /** The light over the dais. */
    private static void glow(ServerLevel level, RoadTrial trial, long now) {
        if (now % 3 != 0) {
            return;
        }
        Vec3 dais = EndingHall.dais(trial);
        RandomSource random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * 1.8;
            level.sendParticles(new EnergyParticleOptions(i % 3 == 0 ? WitchesRoad.LIGHT_CORE : WitchesRoad.LIGHT, 1.2F),
                    dais.x + Math.cos(angle) * radius, dais.y + 0.1, dais.z + Math.sin(angle) * radius, 0, 0, 1, 0, 0.05 + random.nextDouble() * 0.1);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The question

    /**
     * Asks the player again (the screen opens again): from the door in the hall and from {@code /road choose}.
     *
     * @param tell whether to say why not when nothing happens
     * @return whether the screen was sent
     */
    public static boolean reopen(ServerPlayer player, boolean tell) {
        Stage stage = stage(player);
        RoadTrial trial = RoadTrials.trialOf(player.getUUID());
        if (stage == null || trial == null) {
            if (tell) {
                player.displayClientMessage(Component.translatable("message.enhanced.road.ending.not_here").withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        if (stage == Stage.ARRIVING) {
            if (tell) {
                player.displayClientMessage(Component.translatable("message.enhanced.road.ending.wait").withStyle(s -> s.withColor(WitchesRoad.LIGHT)), true);
            }
            return false;
        }
        if (stage == Stage.FAREWELL) {
            return false;
        }
        return ask(trial, player);
    }

    /** Sends the choice screen. @return whether it was sent (he needs a Gene to answer) */
    private static boolean ask(RoadTrial trial, ServerPlayer player) {
        if (findGene(player) == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.ending.no_gene").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int total = RoadChallenges.count();
        PacketDistributor.sendToPlayer(player, new OpenRoadPayload(trial.layout().exitDoor(), false, total, total, true));
        return true;
    }

    /** The stack of Genes the player carries (the hands first), or null. */
    @Nullable
    private static ItemStack findGene(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.SCARLET_WITCH_GENE.get())) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.SCARLET_WITCH_GENE.get())) {
                return stack;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The answer

    /**
     * The player's answer on the screen in the hall. Everything is checked again here (see the class comment); a client that sends the
     * answer without being in the hall, or twice, or after another power, gets nothing and loses nothing.
     *
     * @return whether he awakened
     */
    public static boolean choose(ServerPlayer player, Power choice) {
        if (choice != Power.BILLY && choice != Power.TOMMY) {
            return false;
        }
        UUID id = player.getUUID();
        State state = STATES.get(id);
        RoadTrial trial = RoadTrials.trialOf(id);
        if (state == null || trial == null || !trial.challenge().isEnding() || trial.phase() != RoadTrial.Phase.ACTIVE || state.stage != Stage.ASKING) {
            return false;
        }
        if (!player.isAlive() || player.isSpectator() || !RoadDimension.in(player) || Powers.power(player).hasPowers()) {
            return false;
        }
        RoadProgress progress = RoadSave.get(player.server).existing(id);
        if (progress == null || !progress.inside() || progress.completed() != RoadChallenges.count()) {
            return false;
        }
        ItemStack gene = findGene(player);
        if (gene == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.ending.no_gene").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        // The one place the Gene is used up (one of them), and he awakens as at the door.
        gene.consume(1, player);
        state.stage = Stage.FAREWELL;
        state.chosen = choice;
        state.chosenAt = trial.level().getGameTime();
        state.total = WitchesRoadConfig.ENDING_FAREWELL_SECONDS.get() * 20;
        state.anchor = player.position();
        state.yaw = player.getYRot();
        Powers.awaken(player, choice);
        if (state.total <= 0) {
            depart(player, state);
        }
        return true;
    }

    /** "Leave the Road" on the screen: back to where he entered, with the Gene; the door in the world offers the choice. */
    public static boolean leaveUnchosen(ServerPlayer player) {
        Stage stage = stage(player);
        if (stage == null || stage == Stage.FAREWELL) {
            return false;
        }
        RoadTrials.leave(player, RoadChallenge.End.LEFT);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The farewell

    private static void farewell(RoadTrial trial, ServerPlayer player, State state, long now) {
        ServerLevel level = trial.level();
        long age = now - state.chosenAt;
        // He stays where he stands (the speed of Tommy and the flight of Billy are not for the hall).
        if (player.position().distanceToSqr(state.anchor) > 2.25 || player.getY() < state.anchor.y - 0.5) {
            RoadTrials.moveWithin(player, state.anchor, state.yaw);
        }
        column(level, player.position(), state.chosen, age);
        if (age % 20 == 10) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F,
                    0.8F + Math.min(age, 100) / 100.0F * 0.8F);
        }
        if (age == Math.min(40, state.total / 2)) {
            RoadVoice.say(player, RoadVoice.FAREWELL, trial.progress().started());
        }
        if (age >= state.total) {
            depart(player, state);
        }
    }

    /** A column of light round the player that rises and tightens as the farewell goes on, in the Road's blue-green and his hero's colour. */
    private static void column(ServerLevel level, Vec3 at, Power power, long age) {
        RandomSource random = level.getRandom();
        int hero = power == Power.TOMMY ? Power.TOMMY.glow : Power.BILLY.color;
        for (int i = 0; i < 6; i++) {
            double angle = age * 0.35 + i * Math.PI / 3.0;
            double radius = Math.max(0.3, 1.2 - age * 0.01);
            int color = i % 3 == 0 ? WitchesRoad.LIGHT_CORE : i % 3 == 1 ? WitchesRoad.LIGHT : hero;
            level.sendParticles(new EnergyParticleOptions(color, 1.3F), at.x + Math.cos(angle) * radius, at.y + random.nextDouble() * 0.4,
                    at.z + Math.sin(angle) * radius, 0, 0, 1, 0, 0.12 + random.nextDouble() * 0.12);
        }
    }

    /**
     * The farewell is over: he is sent out of the Road. His effects are cleared; he comes out healed and fed (the Road's blessing, whatever
     * {@code healAtCheckpoint} says); {@link RoadTrials#leave} takes him to where he entered, takes the hall down and, because he has a power now,
     * makes the Road forget his progress.
     */
    private static void depart(ServerPlayer player, State state) {
        STATES.remove(player.getUUID());
        Power chosen = state.chosen;
        player.removeAllEffects();
        RoadTrials.leave(player, RoadChallenge.End.WON);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(10.0F);
        RoadVoice.say(player, RoadVoice.HOME, (int) (player.getUUID().getLeastSignificantBits() & 0xFF));
        player.displayClientMessage(Component.translatable("message.enhanced.road.ending.done", Component.translatable("power.enhanced." + chosen.getSerializedName()))
                .withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)), false);
    }

    private RoadEnding() {
    }
}

package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.healing.HealingHooks;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Deadpool's lines (3.7, PLAN 5.4): about forty jokes of our own, in English and Hebrew, kid-safe (no swearing, no film
 * quotes; snacks, his belt, the "audience", unicorns, naps, Dogpool). A line is a lang key {@code
 * message.enhanced.deadpool.line.<group>.<n>}; it is sent as a {@link DeadpoolFxPayload#TAUNT} effect, so every client
 * within {@link #BUBBLE_RANGE} writes it in its own language in a speech bubble over his head (the client's {@code
 * deadpoolLines} option hides them), and his own client also shows it in the action bar.
 *
 * <p>When he speaks: the Taunt (always, its own cooldown), when he goes DOWN and rises (the healing factor's {@link
 * HealingHooks}; both of two healers down near each other: "two_healers"), when the belt glitches, when Dogpool comes,
 * when he eats a chimichanga, when the poison switches his healing off, near a Wolverine (once in a while), now and then
 * to the "reader", and once when the Weapon X Pod makes him. Every line but the Taunt's is at most one per {@code
 * lineGapTicks} ({@code linesEnabled} off: none at all).
 */
public final class DeadpoolLines {
    /** How far (blocks) the bubble of a line is seen. */
    public static final double BUBBLE_RANGE = 24.0;
    /** How the effect's argument holds a line: {@code group * GROUP + n}, and the Taunt's pose times {@code POSE}. */
    public static final int GROUP = 64;
    public static final int POSE = 4096;
    /** How often (ticks) the quiet lines are thought of: the reader, a Wolverine near, the poison. */
    static final int THINK_EVERY = 20;
    /** The chance a quiet minute ends with a word to the reader. */
    static final int READER_EVERY = 1200;
    static final float READER_CHANCE = 0.35F;
    /** How near a Wolverine is to be talked to, and how long before the same one is talked to again. */
    static final double WOLVERINE_RANGE = 8.0;
    static final int WOLVERINE_AGAIN = 6000;
    /** How near two downed healers are to joke about it. */
    static final double TWO_HEALERS_RANGE = 16.0;

    /** The groups of lines, each with its count (the lang keys run 0..count-1). */
    public enum Group {
        TAUNT(8), RISE(4), DOWN(4), GLITCH(4), DOGPOOL(4), CHIMICHANGA(4), READER(4), WOLVERINE(4), TWO_HEALERS(2),
        POISONED(2), AWAKEN(2);

        public final int count;

        Group(int count) {
            this.count = count;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The lang key of its {@code n}th line. */
        public String key(int n) {
            return "message.enhanced.deadpool.line." + id() + "." + n;
        }
    }

    /** All the lines' keys (for the lang tests). */
    public static List<String> keys() {
        List<String> keys = new java.util.ArrayList<>();
        for (Group g : Group.values()) {
            for (int n = 0; n < g.count; n++) {
                keys.add(g.key(n));
            }
        }
        return keys;
    }

    /** The lang key of an effect's argument (the pose taken off), or null for a number that is no line. */
    public static String keyOf(int arg) {
        int line = arg % POSE;
        int group = line / GROUP;
        int n = line % GROUP;
        Group[] groups = Group.values();
        return group >= 0 && group < groups.length && n < groups[group].count ? groups[group].key(n) : null;
    }

    /** The pose of a Taunt's argument. */
    public static int poseOf(int arg) {
        return arg / POSE;
    }

    /** What each Deadpool said last, and the quiet ones' clocks. */
    private static final class Said {
        long lastLine = Long.MIN_VALUE / 2;
        long lastNoise = Long.MIN_VALUE / 2;
        boolean wasPoisoned;
        final Map<UUID, Long> wolverines = new HashMap<>();
    }

    private static final Map<UUID, Said> SAID = new HashMap<>();

    static {
        HealingHooks.register(new HealingHooks.Listener() {
            @Override
            public void down(ServerPlayer player) {
                if (Powers.get(player).power() != Power.DEADPOOL) {
                    return;
                }
                if (anotherDownNear(player)) {
                    say(player, Group.TWO_HEALERS, true);
                } else {
                    say(player, Group.DOWN, false);
                }
            }

            @Override
            public void rise(ServerPlayer player, boolean rescued) {
                if (Powers.get(player).power() == Power.DEADPOOL) {
                    say(player, Group.RISE, true);
                }
            }
        });
    }

    /** Makes sure the healing factor's hooks are listened to (they are registered when this class loads). */
    static void init() {
    }

    /** A random line of the group. */
    static int pick(Group group, RandomSource random) {
        return random.nextInt(group.count);
    }

    /**
     * A random line of the group, if lines are on and (unless {@code always}) his last line is {@code lineGapTicks}
     * behind. Returns whether he said it.
     */
    public static boolean say(ServerPlayer player, Group group, boolean always) {
        if (!enabled()) {
            return false;
        }
        long now = player.level().getGameTime();
        Said said = SAID.computeIfAbsent(player.getUUID(), k -> new Said());
        if (!always && now - said.lastLine < DeadpoolAbilities.cfg(DeadpoolConfig.LINE_GAP_TICKS, 200)) {
            return false;
        }
        sayLine(player, group, pick(group, player.getRandom()), 0);
        return true;
    }

    /** Says that very line (with the Taunt's pose), whatever the gap, if lines are on. */
    static void sayLine(ServerPlayer player, Group group, int n, int pose) {
        int arg = pose * POSE + group.ordinal() * GROUP + n;
        ServerLevel level = player.serverLevel();
        if (enabled()) {
            SAID.computeIfAbsent(player.getUUID(), k -> new Said()).lastLine = level.getGameTime();
            PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), BUBBLE_RANGE,
                    new DeadpoolFxPayload(DeadpoolFxPayload.TAUNT, player.getId(), List.of(player.position()), arg));
        } else if (group == Group.TAUNT) {
            // (no words, still the pose)
            PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), BUBBLE_RANGE,
                    new DeadpoolFxPayload(DeadpoolFxPayload.TAUNT, player.getId(), List.of(player.position()),
                            pose * POSE + GROUP * Group.values().length));
        }
    }

    /** The last game time he said a line (for the tests). */
    public static long lastLine(Player player) {
        Said said = SAID.get(player.getUUID());
        return said == null ? Long.MIN_VALUE / 2 : said.lastLine;
    }

    static boolean enabled() {
        return DeadpoolAbilities.cfg(DeadpoolConfig.LINES_ENABLED, true);
    }

    /** Every tick of Deadpool's: the quiet lines (the poison, a Wolverine near, the reader). */
    static void tick(ServerPlayer player, PowerData data, long now) {
        if ((now + player.getId()) % THINK_EVERY != 0 || !enabled()) {
            return;
        }
        Said said = SAID.computeIfAbsent(player.getUUID(), k -> new Said());
        boolean poisoned = HealingFactor.has(player) && HealingFactor.suppressed(player);
        if (poisoned && !said.wasPoisoned) {
            said.wasPoisoned = true;
            say(player, Group.POISONED, false);
            return;
        }
        said.wasPoisoned = poisoned;
        if (player.getLastHurtByMobTimestamp() > player.tickCount - 100 && player.getLastHurtByMob() != null) {
            said.lastNoise = now; // (in a fight: no chatting to the reader)
        }
        for (Player other : player.level().players()) {
            if (other != player && other.isAlive() && !other.isSpectator()
                    && Powers.get(other).power() == Power.WOLVERINE
                    && other.distanceToSqr(player) <= WOLVERINE_RANGE * WOLVERINE_RANGE) {
                Long last = said.wolverines.get(other.getUUID());
                if ((last == null || now - last > WOLVERINE_AGAIN) && say(player, Group.WOLVERINE, false)) {
                    said.wolverines.put(other.getUUID(), now);
                    return;
                }
            }
        }
        if (now - said.lastLine > READER_EVERY && now - said.lastNoise > READER_EVERY
                && (now / THINK_EVERY) % (READER_EVERY / THINK_EVERY) == 0) {
            if (player.getRandom().nextFloat() < READER_CHANCE) {
                say(player, Group.READER, false);
            } else {
                said.lastLine = now; // (and the next try is a quiet minute later)
            }
        }
    }

    /** Whether another healer lies DOWN within {@link #TWO_HEALERS_RANGE} of him. */
    static boolean anotherDownNear(ServerPlayer player) {
        for (Player other : player.level().players()) {
            if (other != player && other.distanceToSqr(player) <= TWO_HEALERS_RANGE * TWO_HEALERS_RANGE
                    && HealingFactor.isDown(other)) {
                return true;
            }
        }
        return false;
    }

    static void forget(UUID player) {
        SAID.remove(player);
    }

    static void clear() {
        SAID.clear();
    }

    private DeadpoolLines() {
    }
}

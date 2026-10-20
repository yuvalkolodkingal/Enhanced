package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Red Hulk's Anger (3.8, PLAN 4.3): 0 to 100 in {@link RedHulkState#anger()} (the attachment {@code RED_HULK}, synced to
 * everyone who sees him: his skin glows hotter with it). It fills with the fight, {@code angerPerDamageTaken} for each
 * health he loses (after armour) and {@code angerPerDamageDealt} for each he deals (his fists and every move of his),
 * and {@code angerOnRoar} with a Roar; it cools by {@code coolPerSecond} once he has not fought for {@code
 * coolDelayTicks}. At 100 it bursts by itself ({@link RedHulkBurst}) and is gone. Ross fills at {@code rossAngerFactor}
 * and, at 100, turns into Red Hulk (when there is room; without room his anger waits, full).
 */
public final class Anger {
    public static final float MAX = 100.0F;

    public static RedHulkState get(Player player) {
        return player.getData(ModAttachments.RED_HULK);
    }

    static void set(Player player, RedHulkState state) {
        if (!state.equals(get(player))) {
            player.setData(ModAttachments.RED_HULK, state);
        }
    }

    /** His anger now (0-100). Both sides. */
    public static float of(Player player) {
        return get(player).anger();
    }

    /** Sets his anger (a command's, a test's); {@code fight}: he is in a fight now (the cooling waits). */
    public static void setAnger(ServerPlayer player, float value, boolean fight) {
        RedHulkState s = get(player);
        set(player, s.withAnger(clamp(value), fight ? player.level().getGameTime() : s.lastFight()));
    }

    /** Adds {@code amount} to his anger (Ross's at his share); {@code fight}: it came from a fight. */
    public static void add(ServerPlayer player, PowerData data, float amount, boolean fight) {
        if (amount <= 0.0F || !data.has(HeroAbility.RED_HULK_BODY)) {
            return;
        }
        if (RedHulkAbilities.isRoss(data)) {
            amount *= (float) RedHulkAbilities.cfg(RedHulkConfig.ROSS_ANGER_FACTOR, 0.5);
        }
        RedHulkState s = get(player);
        set(player, s.withAnger(clamp(s.anger() + amount), fight ? player.level().getGameTime() : s.lastFight()));
    }

    /** He lost {@code health} (after armour). */
    static void hurt(ServerPlayer player, PowerData data, float health) {
        add(player, data, health * (float) RedHulkAbilities.cfg(RedHulkConfig.ANGER_PER_DAMAGE_TAKEN, 3.0), true);
    }

    /** He dealt {@code health} to someone (his burst's own blows do not count: they are the anger let out). */
    static void dealt(ServerPlayer player, PowerData data, float health) {
        RedHulkAbilities.State st = RedHulkAbilities.peek(player.getUUID());
        if (st != null && st.burst != null) {
            return;
        }
        add(player, data, health * (float) RedHulkAbilities.cfg(RedHulkConfig.ANGER_PER_DAMAGE_DEALT, 1.0), true);
    }

    /**
     * Every tick of a player with his body: at 100 Red Hulk bursts and Ross turns into Red Hulk; out of a fight the
     * anger cools (every 10 ticks, half a second's worth).
     */
    static void tick(ServerPlayer player, PowerData data, RedHulkAbilities.State st, long now) {
        RedHulkState s = get(player);
        if (s.anger() >= MAX && st.change == null) {
            if (RedHulkAbilities.isRoss(data)) {
                if (data.power() == Power.RED_HULK && RossForm.grow(player, data, st, false)) {
                    set(player, s.withAnger(MAX * 0.5F, now));
                }
                return; // (no room: his anger waits, full)
            }
            if (st.burst == null && player.isAlive()) {
                RedHulkBurst.start(player, data, 1.0);
                return;
            }
        }
        if (now % 10 == 0 && s.anger() > 0.0F && st.burst == null
                && now - s.lastFight() >= RedHulkAbilities.cfg(RedHulkConfig.COOL_DELAY_TICKS, 100)) {
            float cool = (float) RedHulkAbilities.cfg(RedHulkConfig.COOL_PER_SECOND, 4.0) * 0.5F;
            set(player, s.withAnger(clamp(s.anger() - cool), s.lastFight()));
        }
    }

    static float clamp(float value) {
        return Math.max(0.0F, Math.min(MAX, value));
    }

    private Anger() {
    }
}

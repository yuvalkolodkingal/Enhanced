package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The Hulk's two meters (3.6), kept in {@link HulkState} (synced to the player alone):
 * <ul> <li><b>Rage</b> (0-100, the Hulk and a custom hero with his body): +4 per heart he takes, +2 per blow he lands,
 * +20 on a Roar; after 10 seconds out of a fight it falls 2 a second. At 50 or more he is enraged (his eyes glow,
 * Nausea and confusion slide off him) and every damage of his is up to {@code rageDamageBonus} more ({@link
 * #damageFactor}). It never turns him back into Banner (Ragnarok: two years as the Hulk); it is gone on a death and on
 * a change to Banner.</li> <li><b>Anger</b> (0-100, Bruce Banner): +{@code bannerAngerPerHeart} per heart he takes, and
 * 30 at once when his health drops below 4 ("the Hulk protects Banner"); it falls 2 a second. At 100 he Hulks out by
 * himself, unless he carries a Gamma Inhibitor (or there is no room: then it waits). The HUD shows it as his heart
 * rate, 70 to 200 beats a minute.</li> </ul>
 */
public final class HulkRage {
    /** Rage at which he is enraged. */
    public static final int ENRAGED = 50;
    /** The most of either meter. */
    public static final int MAX = 100;
    /** Rage a heart taken and a blow landed add; anger the "Hulk protects Banner" moment adds. */
    public static final int RAGE_PER_HEART = 4;
    public static final int RAGE_PER_HIT = 2;
    public static final int LOW_HEALTH_ANGER = 30;
    /** Below this health Banner's anger jumps. */
    public static final float LOW_HEALTH = 4.0F;
    /** Rage falls only after this many ticks out of a fight. */
    public static final int OUT_OF_FIGHT = 200;
    /** Banner's heart rate at rest and at the top. */
    public static final int BPM_REST = 70;
    public static final int BPM_TOP = 200;

    /** The player's state (an empty one for anyone who never was the Hulk). */
    public static HulkState get(Player player) {
        return player.getData(ModAttachments.HULK);
    }

    private static void set(Player player, HulkState state) {
        if (!state.equals(get(player))) {
            player.setData(ModAttachments.HULK, state);
        }
    }

    /** Whether the Hulk's rage is on in the config. */
    static boolean rageOn() {
        return HulkAbilities.cfg(HulkConfig.RAGE, true);
    }

    /** Whether he is enraged (rage at {@link #ENRAGED} or more, his body on). */
    public static boolean enraged(Player player) {
        return rageOn() && get(player).rage() >= ENRAGED && HulkAbilities.hasBody(Powers.get(player));
    }

    /** What his rage multiplies his damage by: 1 calm, up to 1 + {@code rageDamageBonus} at 100. */
    public static double damageFactor(Player player, PowerData data) {
        if (!rageOn() || !HulkAbilities.hasBody(data)) {
            return 1.0;
        }
        return 1.0 + get(player).rage() / (double) MAX * HulkAbilities.cfg(HulkConfig.RAGE_DAMAGE_BONUS, 0.3);
    }

    /** Banner's heart rate for this anger (beats a minute). */
    public static int bpm(int anger) {
        return BPM_REST + Math.round((BPM_TOP - BPM_REST) * Math.max(0, Math.min(MAX, anger)) / (float) MAX);
    }

    /** Adds rage (a Roar, a blow). */
    public static void add(ServerPlayer player, PowerData data, int amount) {
        if (rageOn() && HulkAbilities.hasBody(data)) {
            HulkState state = get(player);
            set(player, state.withRage(state.rage() + amount));
        }
    }

    /** A blow of his landed: rage, and he is in a fight. */
    static void landed(ServerPlayer player, PowerData data) {
        if (data.has(HeroAbility.HULK_BODY)) {
            HulkAbilities.state(player.getUUID()).lastFight = player.level().getGameTime();
            add(player, data, RAGE_PER_HIT);
        }
    }

    /**
     * He took {@code amount} damage (after armour): the Hulk's rage rises, Banner's anger rises (and jumps when his
     * health falls below 4).
     */
    static void hurt(ServerPlayer player, PowerData data, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        HulkAbilities.State st = HulkAbilities.state(player.getUUID());
        HulkAbilities.hurtNow(player);
        HulkState state = get(player);
        if (HulkAbilities.hasBody(data)) {
            if (rageOn()) {
                set(player, state.withRage(state.rage() + Math.round(amount / 2.0F * RAGE_PER_HEART)));
            }
            return;
        }
        if (data.power() != Power.HULK || !HulkForms.isHuman(data)) {
            return;
        }
        int anger = state.anger() + Math.round(amount / 2.0F * HulkAbilities.cfg(HulkConfig.BANNER_ANGER_PER_HEART,
                10));
        float after = player.getHealth() - amount;
        if (after < LOW_HEALTH && st.wellBefore && HulkAbilities.cfg(HulkConfig.BANNER_LOW_HEALTH_TRANSFORM, true)) {
            anger += LOW_HEALTH_ANGER;
            st.wellBefore = false;
        }
        set(player, state.withAnger(anger));
    }

    /** Every tick of a player with the Hulk's rows: the meters fall, and Banner's anger may Hulk him out. */
    static void tick(ServerPlayer player, PowerData data, HulkAbilities.State st, long now) {
        HulkState state = get(player);
        if (player.getHealth() >= LOW_HEALTH) {
            st.wellBefore = true;
        }
        // (Before the meters fall: at 100 he Hulks out; with no room his anger waits, it does not ebb.)
        if (data.power() == Power.HULK && HulkForms.isHuman(data) && state.anger() >= MAX
                && HulkAbilities.cfg(HulkConfig.BANNER_ANGER_TRANSFORM, true) && !inhibited(player)
                        && !HulkForms.changing(player)) {
            String to = HulkAbilities.isHulkLook(state.lastHulk()) ? state.lastHulk() : HulkState.FIRST_HULK;
            if (HulkForms.change(player, to, false)) {
                set(player, get(player).withAnger(0));
                return;
            }
            return; // (no room: his anger waits, full)
        }
        if (now % 10 == 0) {
            if (state.rage() > 0 && (now - st.lastFight >= OUT_OF_FIGHT || !rageOn())) {
                state = state.withRage(rageOn() ? state.rage() - 1 : 0);
            }
            if (state.anger() > 0) {
                state = state.withAnger(state.anger() - 1);
            }
            set(player, state);
        }
    }

    /** Whether Banner carries a Gamma Inhibitor (it keeps him human, however angry). */
    public static boolean inhibited(Player player) {
        return player.getInventory().hasAnyMatching(s -> s.is(ModItems.GAMMA_INHIBITOR.get()));
    }

    /** He calms down into Banner: the rage is gone. */
    static void calm(ServerPlayer player) {
        set(player, get(player).withRage(0));
    }

    /** A death: rage and anger are gone. */
    static void reset(ServerPlayer player) {
        HulkState state = get(player);
        set(player, state.withRage(0).withAnger(0));
    }

    private HulkRage() {
    }
}

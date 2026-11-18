package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * Jim Hammond's solar charge (3.17): the Solar Jewel's meter ({@link PowerData#ironCharge}, synced and saved) filled by the jewel's
 * own sunlight rule ({@link SolarJewel#absorbing}) and spent by his attacks. Only Jim himself (the power) has it: flight, fire
 * immunity, his body, regeneration and the burning cost nothing. With {@code solarCharge} off his attacks are free.
 */
public final class JimSolar {
    private JimSolar() {
    }

    /** Whether this player's attacks use the meter. */
    public static boolean active(ServerPlayer player, PowerData data) {
        return JimConfig.SOLAR_CHARGE.get() && data.power() == Power.JIM_HAMMOND && !player.getAbilities().instabuild;
    }

    public static int capacity() {
        return JimConfig.SOLAR_CAPACITY.get();
    }

    /** The meter, 0 to 1. */
    public static float fraction(PowerData data) {
        return Mth.clamp(data.ironCharge() / (float) Math.max(1, capacity()), 0.0F, 1.0F);
    }

    /** Every tick: the meter drinks in the sun by the jewel's rule (creative: always full). */
    static void tick(ServerPlayer player, PowerData data) {
        if (data.power() != Power.JIM_HAMMOND || !JimConfig.SOLAR_CHARGE.get()) {
            return;
        }
        int max = capacity();
        if (player.getAbilities().instabuild) {
            if (data.ironCharge() != max) {
                data.setIronCharge(max);
            }
            return;
        }
        if (data.ironCharge() > max) {
            data.setIronCharge(max);
            return;
        }
        int gain = SolarJewel.absorbing(player, JimConfig.SOLAR_FILL_SUN.get(), JimConfig.SOLAR_FILL_SHADE.get());
        if (gain > 0 && data.ironCharge() < max) {
            data.setIronCharge(Math.min(max, data.ironCharge() + gain));
        }
    }

    /** Whether he has at least {@code need}; if not, says so on the action bar. */
    static boolean has(ServerPlayer player, PowerData data, int need) {
        if (!active(player, data) || data.ironCharge() >= need) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.enhanced.jim.solar_low", need * 100 / Math.max(1, capacity()),
                Math.round(fraction(data) * 100)), true);
        return false;
    }

    /** Spends {@code amount} if he has it (free when the switch is off or he is a creative player). */
    static boolean spend(ServerPlayer player, PowerData data, int amount) {
        if (!active(player, data)) {
            return true;
        }
        if (data.ironCharge() < amount) {
            has(player, data, amount);
            return false;
        }
        data.setIronCharge(data.ironCharge() - amount);
        return true;
    }

    /** The Fire Beam's price: a tap's, rising to a full charge's. */
    static int beamCost(float charge) {
        return Math.round(Mth.lerp(charge, JimConfig.BEAM_TAP_COST.get(),
                Math.max(JimConfig.BEAM_TAP_COST.get(), JimConfig.BEAM_FULL_COST.get())));
    }
}

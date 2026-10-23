package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * N: the Combat Combo, and with Sneak the Flip Kick (3.8, PLAN 3.4). The combo is two punches and a spinning kick
 * ({@code comboDamage1/2/3}) on the first creature in a 60° cone before him, at least {@code comboGap} ticks apart; a
 * pause of {@code comboReset} starts it again from the first punch; the kick knocks back. The flip kick is a backflip:
 * the creature in front is kicked up and away and he lands three blocks back. Both deal {@code enhanced:cap_combo}
 * (3.8.1; a blow of the hand, {@code #enhanced:melee_blows}), which a custom hero's strength scales ({@code
 * HeroDamage}).
 */
public final class CaptainCombo {
    static final double REACH = 3.5;
    static final double CONE = 60.0;
    /** The spinning kick's knock, and the flip kick's (up, back). */
    static final double KICK_KNOCK = 1.0;
    static final double FLIP_UP = 0.6;
    static final double FLIP_BACK = 1.2;
    /** His backflip: back and up (he lands about three blocks back). */
    static final double FLIP_SELF_BACK = 0.42;
    static final double FLIP_SELF_UP = 0.5;

    /** A blow of the combo. Returns the step struck (0-2), or -1 when none was (too soon). */
    public static int punch(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.CAPTAIN_COMBO) || !data.isReady(HeroAbility.CAPTAIN_COMBO, now)) {
            return -1;
        }
        CaptainState cs = ModAttachments.CAPTAIN.get(player);
        int reset = CaptainAbilities.cfg(CaptainConfig.COMBO_RESET, 30);
        int step = now - cs.comboAt() > reset || cs.comboStep() > 2 ? 0 : cs.comboStep();
        data.startCooldown(HeroAbility.CAPTAIN_COMBO, now, CaptainAbilities.cfg(CaptainConfig.COMBO_GAP, 8));
        ModAttachments.CAPTAIN.set(player, cs.withCombo((step + 1) % 3, now));
        float damage = (float) switch (step) {
            case 0 -> CaptainAbilities.cfg(CaptainConfig.COMBO_DAMAGE_1, 4.0);
            case 1 -> CaptainAbilities.cfg(CaptainConfig.COMBO_DAMAGE_2, 4.0);
            default -> CaptainAbilities.cfg(CaptainConfig.COMBO_DAMAGE_3, 7.0);
        };
        ServerLevel level = player.serverLevel();
        player.swing(step == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, true);
        LivingEntity target = CaptainAbilities.inFront(player, REACH, CONE);
        if (target != null && CaptainAbilities.hurt(player, target, ModDamageTypes.CAP_COMBO, damage, null)) {
            if (step == 2) {
                CaptainAbilities.push(target, player.position(), KICK_KNOCK, 0.2, player);
            }
            CaptainAbilities.sound(level, target.position(), ModSounds.CAP_PUNCH.get(), 1.0F, step == 2 ? 0.8F
                    : 1.0F + 0.1F * step);
        }
        CaptainAbilities.send(level, player, step == 2 ? CaptainFxPayload.KICK : CaptainFxPayload.PUNCH,
                target == null ? List.of() : List.of(target.getBoundingBox().getCenter()), step);
        return step;
    }

    /** The flip kick. Returns whether it struck a creature. */
    public static boolean flipKick(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.CAPTAIN_FLIP_KICK) || !data.isReady(HeroAbility.CAPTAIN_FLIP_KICK, now)) {
            return false;
        }
        data.startCooldown(HeroAbility.CAPTAIN_FLIP_KICK, now, CaptainAbilities.cfg(CaptainConfig.FLIP_COOLDOWN, 40));
        ServerLevel level = player.serverLevel();
        LivingEntity target = CaptainAbilities.inFront(player, REACH, CONE);
        boolean struck = false;
        if (target != null && CaptainAbilities.hurt(player, target, ModDamageTypes.CAP_COMBO,
                (float) CaptainAbilities.cfg(CaptainConfig.FLIP_DAMAGE, 5.0), null)) {
            CaptainAbilities.push(target, player.position(), FLIP_BACK, FLIP_UP, player);
            struck = true;
        }
        Vec3 back = CaptainAbilities.flat(player.getLookAngle()).scale(-FLIP_SELF_BACK);
        player.setDeltaMovement(back.x, FLIP_SELF_UP, back.z);
        player.hurtMarked = true;
        CaptainAbilities.guardFall(player, 30);
        CaptainAbilities.sound(level, player.position(), ModSounds.CAP_FLIP.get(), 1.0F, 1.0F);
        CaptainAbilities.send(level, player, CaptainFxPayload.FLIP, target == null ? List.of()
                : List.of(target.getBoundingBox().getCenter()), struck ? 1 : 0);
        return struck;
    }

    private CaptainCombo() {
    }
}

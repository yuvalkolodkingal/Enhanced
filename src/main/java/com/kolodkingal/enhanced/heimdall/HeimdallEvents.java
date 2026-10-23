package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.item.HofundItem;
import com.kolodkingal.enhanced.platform.event.LivingFallEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;

/**
 * Heimdall's events (3.5): the Shadows of Hela of a Guardian's Watch, which only their watcher can strike; the harm the
 * Guardian's Ward takes off; the bonus of Hofund in Heimdall's hand; and his body's sure footing.
 */
@Subscriber
public final class HeimdallEvents {

    /** A shadow of a Watch: its watcher's blow strikes it, anyone else's (and anything else) does nothing. */
    @Subscribe(priority = Priority.HIGHEST)
    public static void onShadowHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || GuardiansWatch.watcherOf(event.getEntity()) == null) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return; // (the void, /kill: the Watch hides it again)
        }
        GuardiansWatch.strike(event.getEntity(), source.getEntity());
        event.setCanceled(true);
    }

    /**
     * Under the Guardian's Ward he takes {@link HeimdallAbilities#WARD_REDUCTION} less; Hofund in his hand strikes
     * {@code heimdall.hofundBonus} harder (his own blows: the Slash and the Lunge count it themselves).
     */
    @Subscribe
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (HeimdallAbilities.warded(event.getEntity()) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setAmount(event.getAmount() * (1.0F - HeimdallAbilities.WARD_REDUCTION));
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof Player attacker
                && HofundItem.isHofund(attacker.getMainHandItem()) && Powers.get(attacker).has(HeroAbility.HEIMDALL_HOFUND)) {
            event.setAmount(event.getAmount() * (float) (1.0 + HeimdallAbilities.hofundBonus()));
        }
    }

    /** His body: a fall shorter than {@link HeimdallAbilities#SAFE_FALL} blocks does him no harm. */
    @Subscribe
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()
                && Powers.get(player).has(HeroAbility.HEIMDALL_BODY) && event.getDistance() < HeimdallAbilities.SAFE_FALL) {
            event.setDamageMultiplier(0.0F);
        }
    }

    private HeimdallEvents() {
    }
}

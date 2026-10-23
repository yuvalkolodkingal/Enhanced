package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.ArenaLoans;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.platform.event.EntityJoinLevelEvent;
import com.kolodkingal.enhanced.platform.event.LevelEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/**
 * The game-bus events of telekinesis in 3.1 (every form: Wanda, Billy, the Phoenix, Magneto), in a class of their own so
 * that they compose with the 3.0 chain of {@code CommonEvents} by priority (spec 5.6): the landing guard, the saves of the
 * world that the journals of the arenas' loans and the notes of Ultron's islands wait for, and the will a vanilla boss
 * gets back if he was saved while a grip had taken it.
 */
@Subscriber
public final class TelekinesisEvents {

    /**
     * The landing guard (D-M 3.8, R16): whatever a grip let go of that its holder may not hurt (a player with PvP off, a
     * teammate, another player's pet, drone or Hex's Vision) takes no fall, no wall and no telekinetic slam until it lands
     * ({@link Telekinesis#isSpared}), while {@code magneto.telekinesisSparesFriends} is on. {@code HIGH}: after the lift's
     * carry immunity ({@code HIGHEST}), before the 3.0 chain ({@code NORMAL}); and for every creature, not players only.
     */
    @Subscribe(priority = Priority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || !Telekinesis.isSpared(event.getEntity())
                || !EnhancedConfig.TELEKINESIS_SPARES_FRIENDS.get()) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL) || source.is(ModDamageTypes.TELEKINETIC_SLAM)) {
            event.setCanceled(true);
        }
    }

    /**
     * A dimension was saved (R48): its chunks are queued to the disk before this event, so a repair of an arena's ground made
     * before the save before this one is on the disk now, and its journal may forget it ({@link ArenaLoans#onSave}, which
     * also counts the save); so may the note of an island of Ultron's that is gone ({@link SokoviaJournal#onSave}).
     */
    @Subscribe
    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ArenaLoans.onSave(level);
            SokoviaJournal.onSave(level);
        }
    }

    /**
     * A vanilla boss saved while a grip had turned his {@code NoAI} on (a crash, a chunk unloaded under a throw) joins a
     * level: the flag is never left saved, so it goes off with its mark ({@link BossGrip#onJoin}, R37).
     */
    @Subscribe
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            BossGrip.onJoin(event.getEntity());
        }
    }

    private TelekinesisEvents() {
    }
}

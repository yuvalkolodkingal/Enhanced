package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The call: the Tesseract in one hand and Loki's Scepter in the other, held up together for five seconds. A spiral of
 * the Space Stone's blue rises from the one and of the Mind Stone's gold from the other, a drone swells, a bell tolls
 * every second and the action bar counts the stones. Letting go, swapping a slot, losing either item or dying cancels
 * it; neither item is used up. When the five seconds end the shared rules are asked again and the fight begins (see
 * {@link ThanosLane}).
 *
 * <p>It is the Thanos lane's row of {@link ScepterRituals}: once it is registered, a refusal tells the player why and
 * nothing else happens (the Scepter does not explode, the Tesseract does not take its holder away).
 */
public final class ThanosSummon implements ScepterRituals.Hold {
    private static final int MIND_GOLD = 0xF5D77A;

    /** Players whose charge already completed early (a charge shorter than the five seconds the items can be held). */
    private static final Set<UUID> DONE = new HashSet<>();

    /**
     * The summoners who have not let go of the use key since their call completed, and the last tick it asked again. A
     * held key asks again every four ticks; those asks are the same press, and are not answered with "He is already
     * here" over the portal's own line.
     */
    private static final Map<UUID, Long> STILL_HELD = new HashMap<>();
    /** More than this many ticks between two asks: the key was let go of in between. */
    static final int HELD_GAP = 10;

    /** Registers the Thanos fight and its summoning, once, when the mod loads. */
    public static void register() {
        BossFights.register(BossKind.THANOS, new ThanosLane());
        ScepterRituals.registerThanosHold(new ThanosSummon());
    }

    /** Forgets every charge (the server has stopped). */
    public static void clear() {
        DONE.clear();
        STILL_HELD.clear();
    }

    /**
     * Why the Mad Titan cannot be called by {@code player} now, or null: the server's rules, in the words the player
     * reads. The rest of the shared rules (another fight, the difficulty) are asked again by the fight's start.
     */
    @Nullable
    public static Component refusal(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (player.isSpectator() || player.isDeadOrDying()) {
            return Component.translatable("message.enhanced.thanos.summon.disabled");
        }
        if (!ThanosBossConfig.SUMMON_ENABLED.get()) {
            return Component.translatable("message.enhanced.thanos.summon.disabled");
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return Component.translatable("message.enhanced.thanos.summon.peaceful");
        }
        if (MjolnirPin.isPinned(player)) {
            return Component.translatable("message.enhanced.mjolnir.pinned_cannot");
        }
        BossFight active = BossFights.active(level);
        if (active != null) {
            return active.kind() == BossKind.THANOS ? Component.translatable("message.enhanced.thanos.summon.active")
                    : Component.translatable("message.enhanced.boss.busy", active.kind().displayName(), (int) active.arena().center().x, (int) active.arena().center().z);
        }
        long resting = BossFights.restLeft(level.getServer(), BossKind.THANOS);
        if (resting > 0) {
            long seconds = (resting + 19) / 20;
            return Component.translatable("message.enhanced.thanos.summon.resting", String.format("%d:%02d", seconds / 60, seconds % 60));
        }
        return ThanosLane.siteRefusal(player);
    }

    @Override
    public boolean begin(ServerPlayer player, InteractionHand hand) {
        Component refusal = refusal(player);
        if (refusal != null) {
            if (!stillHeld(player)) {
                player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        DONE.remove(player.getUUID());
        STILL_HELD.remove(player.getUUID());
        return true;
    }

    /**
     * Whether this use is the summoner's key still held down after the call completed (the game asks again every four
     * ticks while it is): it is answered with silence, and keeps the silence going for as long as it is held.
     */
    private static boolean stillHeld(ServerPlayer player) {
        Long last = STILL_HELD.get(player.getUUID());
        long now = player.serverLevel().getGameTime();
        if (last != null && now - last <= HELD_GAP) {
            STILL_HELD.put(player.getUUID(), now);
            return true;
        }
        STILL_HELD.remove(player.getUUID());
        return false;
    }

    /** Whether the two hands hold the Tesseract and the Scepter, one each. */
    static boolean holdsBoth(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(ModItems.TESSERACT.get()) && off.is(ModItems.LOKIS_SCEPTER.get())
                || main.is(ModItems.LOKIS_SCEPTER.get()) && off.is(ModItems.TESSERACT.get());
    }

    @Override
    public void tick(ServerPlayer player, ItemStack stack, int remaining) {
        int elapsed = ScepterRituals.HOLD_TICKS - remaining;
        if (!holdsBoth(player)) {
            player.stopUsingItem();
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.summon.need_both"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        charge(level, player, elapsed);
        if (elapsed % 20 == 0 && elapsed > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.summon.charging", elapsed / 20).withColor(StoneKind.SPACE.color()), true);
            ThanosSounds.SUMMON_BELL.play(level, player.position(), 1.0F, 0.8F + elapsed / 20 * 0.1F);
        }
        if (elapsed == 1) {
            ThanosSounds.SUMMON.play(level, player.position());
        }
        int charge = ThanosBossConfig.SUMMON_CHARGE_TICKS.get();
        if (charge < ScepterRituals.HOLD_TICKS && elapsed >= charge && DONE.add(player.getUUID())) {
            complete(player);
            player.stopUsingItem();
        }
    }

    /** The two spirals rising: the Space Stone's blue and the Mind Stone's gold, turning together. */
    private static void charge(ServerLevel level, ServerPlayer player, int elapsed) {
        double rise = (elapsed % 40) / 40.0 * 3.0;
        for (int i = 0; i < 2; i++) {
            double angle = elapsed * 0.35 + i * Math.PI;
            Vec3 at = player.position().add(Math.cos(angle) * 0.9, 0.2 + rise, Math.sin(angle) * 0.9);
            level.sendParticles(new EnergyParticleOptions(i == 0 ? StoneKind.SPACE.color() : MIND_GOLD, 1.2F), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void release(ServerPlayer player, ItemStack stack) {
        DONE.remove(player.getUUID());
    }

    @Override
    public void finish(ServerPlayer player, ItemStack stack) {
        if (DONE.remove(player.getUUID())) {
            return;
        }
        if (holdsBoth(player)) {
            complete(player);
        }
    }

    /** The five seconds are up: the rules are asked once more and the portal opens. */
    private static void complete(ServerPlayer player) {
        if (BossFights.start(BossKind.THANOS, player, false)) {
            STILL_HELD.put(player.getUUID(), player.serverLevel().getGameTime());
        }
    }
}

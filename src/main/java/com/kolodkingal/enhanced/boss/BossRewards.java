package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.config.BossConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Who a boss fight rewards and how the reward reaches them. One rule for the three bosses:
 *
 * <ul>
 *     <li>A participant earns it by being present for at least {@code rewardMinSeconds} <em>and</em> having done at
 *     least the fight's minimum damage ({@link BossFight#rewardMinDamage}: anything for Ultron and Apocalypse, 3
 *     percent of his health for Thanos), <em>or</em> by landing the last blow, whatever their time and damage.</li>
 *     <li>A Creative player never does, unless {@code creativeGetsReward} says so; a Spectator never does.</li>
 *     <li>It goes straight into the inventory; if that is full it drops at the player's feet for them alone, for good;
 *     if the player is dead or offline it waits as a gift ({@link BossGifts}) and arrives at their next login, respawn
 *     or dimension change.</li>
 *     <li>Each rewarded player who is there also gets {@code xpRewardPerPlayer} experience points.</li>
 * </ul>
 */
public final class BossRewards {

    /** The participants of {@code fight} who earn the reward, in the order they joined. */
    public static List<BossFight.Participant> eligible(BossFight fight) {
        List<BossFight.Participant> eligible = new ArrayList<>();
        for (BossFight.Participant participant : fight.participants()) {
            if (earns(fight, participant)) {
                eligible.add(participant);
            }
        }
        return eligible;
    }

    private static boolean earns(BossFight fight, BossFight.Participant participant) {
        if (participant.wasCreative() && !BossConfig.CREATIVE_GETS_REWARD.get()) {
            return false;
        }
        MinecraftServer server = fight.server();
        ServerPlayer online = server.getPlayerList().getPlayer(participant.id());
        if (online != null && online.isSpectator()) {
            return false;
        }
        if (participant.struckLastBlow()) {
            return true;
        }
        float damage = 0.0F;
        for (String counter : fight.rewardCounters()) {
            damage += participant.counter(counter);
        }
        return participant.secondsPresent() >= BossConfig.REWARD_MIN_SECONDS.get() && damage > 0.0F && damage >= fight.rewardMinDamage();
    }

    /**
     * Rewards everyone who qualifies: the lane's stacks and the experience. Called once, when the fight is won.
     *
     * @return the participants who were rewarded
     */
    static List<BossFight.Participant> reward(BossFight fight) {
        MinecraftServer server = fight.server();
        List<BossFight.Participant> rewarded = eligible(fight);
        for (BossFight.Participant participant : rewarded) {
            for (ItemStack stack : fight.rewards(participant)) {
                hand(server, participant.id(), stack, true);
            }
            ServerPlayer player = server.getPlayerList().getPlayer(participant.id());
            if (player != null && player.isAlive()) {
                int xp = BossConfig.XP_REWARD_PER_PLAYER.get();
                if (xp > 0) {
                    player.giveExperiencePoints(xp);
                }
                player.displayClientMessage(Component.translatable("message.enhanced.boss.reward", fight.kind().displayName()), true);
            }
        }
        return rewarded;
    }

    /**
     * Gives a player a stack: now, if they are online and alive and {@code now} is true, otherwise as a gift that waits
     * for them.
     */
    public static void hand(MinecraftServer server, UUID who, ItemStack stack, boolean now) {
        ServerPlayer player = server.getPlayerList().getPlayer(who);
        if (now && player != null && player.isAlive()) {
            give(player, stack);
        } else {
            BossGifts.add(server, who, stack);
        }
    }

    /**
     * Puts the stack in the player's inventory; what does not fit drops at their feet and only they can pick it up, and
     * it never despawns.
     */
    public static void give(ServerPlayer player, ItemStack stack) {
        ItemStack rest = stack.copy();
        player.getInventory().add(rest);
        if (!rest.isEmpty()) {
            ItemEntity drop = player.drop(rest, false);
            if (drop != null) {
                drop.setTarget(player.getUUID());
                drop.setUnlimitedLifetime();
                drop.setInvulnerable(true);
                drop.setNoPickUpDelay();
            }
        }
    }

    private BossRewards() {
    }
}

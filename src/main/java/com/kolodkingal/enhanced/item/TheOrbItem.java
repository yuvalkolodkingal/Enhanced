package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * The Orb (3.6): a silver sphere that holds the Power Stone (Guardians of the Galaxy), found in the vault chest of an
 * End tunnel. Anyone may carry it safely (it is a container: the stone in it does not burn). Used by one who may hold
 * the Power Stone (a Bearer), it opens: the Orb is gone and the Power Stone is in his hand (the ledger counted it when
 * the chest gave the Orb; the name of who took it goes with it). Anyone else: "The Orb will not open for you."
 */
public class TheOrbItem extends Item {
    public TheOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack orb = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.consume(orb);
        }
        if (!StoneRules.mayHold(server, StoneKind.POWER)) {
            server.displayClientMessage(Component.translatable("message.enhanced.stones.orb_closed")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResultHolder.fail(orb);
        }
        return InteractionResultHolder.sidedSuccess(open(server, orb), false);
    }

    /** Opens the Orb in the player's hand: the Power Stone (with the Orb's taker written on it) takes its place. */
    public static ItemStack open(ServerPlayer player, ItemStack orb) {
        ItemStack stone = new ItemStack(ModItems.POWER_STONE.get());
        UUID taker = StoneLedger.taker(orb);
        if (taker != null) {
            StoneLedger.stamp(stone, taker);
        }
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(0.0, player.getBbHeight() * 0.6, 0.0).add(player.getLookAngle().scale(0.5));
        level.playSound(null, at.x, at.y, at.z, ModSounds.ORB_OPEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        StoneKind.POWER.voice().use(level, at, InfinityStone.UNDER, 1.0F);
        level.sendParticles(new EnergyParticleOptions(StoneKind.POWER.color(), 1.3F), at.x, at.y, at.z, 30, 0.25, 0.25,
                0.25, 0.1);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new StoneFxPayload(StoneFxPayload.PEDESTAL_TAKE, player.getId(), List.of(at),
                        StoneKind.POWER.ordinal()));
        return stone;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.the_orb.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.the_orb.desc2").withStyle(ChatFormatting.DARK_PURPLE));
    }
}

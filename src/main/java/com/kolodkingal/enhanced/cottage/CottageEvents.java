package com.kolodkingal.enhanced.cottage;

import com.kolodkingal.enhanced.asgard.SeatEntity;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.NpcState;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.platform.event.EntityJoinLevelEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.road.WitchesRoad;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Agatha's Cottage's part of the game's events (3.7): the keeper's beat on every player's tick, the refusal of
 * duplicates as they join (a resident or a Señor Scratchy whose cottage records another, the Agatha of a cottage
 * while a player is Agatha, anyone while {@code residents} is off), a sleeper's "Zzz..." instead of a talk, the
 * cottage's chairs and benches to sit on, and the cellar door asleep while {@code roadDoor} is off.
 */
@Subscriber
public final class CottageEvents {

    private CottageEvents() {
    }

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CottageKeeper.tick(player);
        }
    }

    @Subscribe
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && refuses(level, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /**
     * Whether a joining entity must be refused: a cottage's resident (or Scratchy) whose cottage records someone else
     * (no duplicates, ever: one that walked into an unloaded chunk comes back after his replacement was made), the
     * Agatha of a cottage while a player is Agatha, or any resident while {@code residents} is off. One of a cottage
     * that records nobody in his place is adopted.
     */
    public static boolean refuses(ServerLevel level, Entity entity) {
        boolean resident = entity.getType() == ModEntities.COTTAGE_RESIDENT.get() && entity instanceof AsgardianEntity;
        if (!resident && !Scratchy.is(entity)) {
            return false;
        }
        long key = resident ? Cottages.keyOf(((AsgardianEntity) entity).slot()) : Scratchy.cottageOf(entity);
        Cottages.Cottage cottage = key == Long.MIN_VALUE ? null : Cottages.get(level).byKey(key);
        if (cottage == null) {
            return false; // a stray (a command, a test): nobody's
        }
        if (resident) {
            Role role = ((AsgardianEntity) entity).role();
            if (!CottageKeeper.residents()
                    || role == Role.AGATHA && CottageKeeper.agathaAway(level.getServer())) {
                return true;
            }
            UUID recorded = cottage.resident(role);
            if (recorded == null) {
                cottage.setResident(role, entity.getUUID());
                return false;
            }
            return !recorded.equals(entity.getUUID());
        }
        UUID recorded = cottage.scratchy();
        if (recorded == null) {
            cottage.setScratchy(entity.getUUID());
            return false;
        }
        return !recorded.equals(entity.getUUID());
    }

    /** A sleeping resident is not woken by a talk: he murmurs in his sleep. */
    @Subscribe
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getTarget() instanceof AsgardianEntity npc)
                || !npc.role().cottage() || npc.state() != NpcState.SLEEPING) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            AsgardTalk.speak(npc, player, "npc.enhanced." + npc.role().id() + ".asleep", List.of());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** The cottage's chairs and benches seat a player (an empty hand, not sneaking); its door sleeps while off. */
    @Subscribe
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        if (!(world instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean door = WitchesRoad.isDoor(state);
        boolean chair = state.getBlock() instanceof StairBlock;
        if (!door && !chair || level.dimension() != Level.OVERWORLD) {
            return;
        }
        Cottages.Cottage cottage = Cottages.get(level).at(pos, 0);
        if (cottage == null) {
            return;
        }
        Cottages.Points points = cottage.points();
        if (door && pos.equals(points.roadDoor()) && !CottageKeeper.roadDoor()) {
            player.displayClientMessage(Component.translatable("message.enhanced.cottage.door_sleeps")
                    .withStyle(ChatFormatting.GRAY), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (chair && player.getMainHandItem().isEmpty() && !player.isShiftKeyDown()
                && (points.seats().contains(pos) || points.porch().contains(pos) || points.bench().contains(pos))) {
            Vec3 seat = Vec3.atBottomCenterOf(pos).add(0.0, 0.15, 0.0);
            if (SeatEntity.sit(player, seat, pos) != null) {
                player.setYRot(state.getValue(StairBlock.FACING).getOpposite().toYRot());
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CottageKeeper.forget(event.getEntity().getUUID());
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        CottageKeeper.clear();
    }
}

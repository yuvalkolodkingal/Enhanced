package com.kolodkingal.enhanced.mind;

import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What happens in Vision's mind (3.12): the elevator's floors and rides, and the talks' actions: the robots' gate,
 * the way down after Ultron, the Human Torch's flames going out, Dr. Horton appearing, and the Source Code, after which
 * the floors below are gone for good.
 */
public final class MindQuest {
    private MindQuest() {
    }

    private static BlockPos origin(ServerPlayer player) {
        return MindRealm.origin(MindRealm.plotOf(player));
    }

    /** The floors the elevator offers the player now. */
    static List<String> floors(ServerPlayer player) {
        List<String> floors = new ArrayList<>(List.of("ground", "upper", "top"));
        if (!MindProgress.of(player).torchDone()) {
            floors.add("down");
        }
        floors.add("leave");
        return floors;
    }

    /** The elevator takes the player to a floor (or, {@code leave}, out of the mind). */
    static void ride(ServerPlayer player, String floor) {
        if (!MindRealm.in(player)) {
            return;
        }
        BlockPos o = origin(player);
        int level = switch (floor) {
            case "ground" -> MindBuilder.GROUND;
            case "upper" -> MindBuilder.UPPER;
            case "top" -> MindBuilder.TOP;
            case "down" -> MindBuilder.BELOW;
            default -> Integer.MIN_VALUE;
        };
        if (floor.equals("leave")) {
            MindRealm.leave(player);
            return;
        }
        if (level == Integer.MIN_VALUE || floor.equals("down") && MindProgress.of(player).torchDone()) {
            return;
        }
        move(player, MindBuilder.liftStop(o, level), 270.0F);
        if (floor.equals("down")) {
            // Ultron waits on the sofa: the talk starts at once and cannot be left
            Entity ultron = find(player, MindNpcEntity.Role.ULTRON);
            MindTalk.start(player, "ultron", ultron, true);
        }
    }

    static void move(ServerPlayer player, BlockPos to, float yaw) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6F, 0.8F);
        Vec3 at = Vec3.atBottomCenterOf(to);
        player.teleportTo(level, at.x, at.y, at.z, yaw, 0.0F);
        player.resetFallDistance();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
    }

    static Entity find(ServerPlayer player, MindNpcEntity.Role role) {
        BlockPos o = origin(player);
        for (MindNpcEntity npc : player.serverLevel().getEntitiesOfClass(MindNpcEntity.class, new AABB(o).inflate(70, 80, 70))) {
            if (npc.role() == role) {
                return npc;
            }
        }
        return null;
    }

    /** An action at the end of a line. */
    static void act(ServerPlayer player, String action, Entity npc) {
        if (action.startsWith("nano_")) {
            if (npc instanceof com.kolodkingal.enhanced.nano.NanoUltronEntity boss) {
                if (action.equals("nano_peace")) {
                    boss.makePeace(player);
                } else {
                    boss.goOn();
                }
            }
            return;
        }
        BlockPos o = origin(player);
        ServerLevel level = player.serverLevel();
        MindProgress progress = MindProgress.of(player);
        switch (action) {
            case "open_gate" -> {
                progress.setGateOpen(true);
                MindBuilder.openGate(level, o);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            case "down" -> {
                move(player, MindBuilder.bottomArrival(o), 90.0F);
                player.displayClientMessage(Component.translatable("message.enhanced.mind.bottom"), true);
            }
            case "quench" -> {
                if (npc instanceof MindNpcEntity torch) {
                    torch.setAblaze(false);
                    torch.clearFire();
                    Vec3 chair = MindBuilder.armchair(o);
                    torch.moveTo(chair.x, chair.y + 0.1, chair.z, 90.0F, 0.0F);
                    level.playSound(null, torch.getX(), torch.getY(), torch.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 0.8F);
                    MindFx.burst(level, torch.position().add(0, 1, 0));
                }
            }
            case "horton" -> {
                if (find(player, MindNpcEntity.Role.HORTON) == null) {
                    MindNpcEntity horton = ModEntities.MIND_NPC.get().create(level);
                    if (horton != null) {
                        horton.setRole(MindNpcEntity.Role.HORTON);
                        horton.setPlot(MindRealm.plotOf(player));
                        Vec3 at = MindBuilder.easel(o);
                        horton.moveTo(at.x, at.y, at.z, 270.0F, 0.0F);
                        level.addFreshEntity(horton);
                        MindFx.burst(level, at.add(0, 1, 0));
                    }
                }
            }
            case "source_code" -> finish(player, progress, o);
            default -> {
            }
        }
    }

    /** The Human Torch hands over the Source Code; the elevator goes up, and below there is nothing any more. */
    private static void finish(ServerPlayer player, MindProgress progress, BlockPos o) {
        ItemStack code = new ItemStack(ModItems.SOURCE_CODE.get());
        if (!player.getInventory().add(code)) {
            player.drop(code, false);
        }
        progress.setTorchDone(true);
        player.displayClientMessage(Component.translatable("message.enhanced.mind.source_code"), false);
        move(player, MindBuilder.liftStop(o, MindBuilder.GROUND), 270.0F);
        ServerLevel level = player.serverLevel();
        MindBuilder.removeBelow(level, o);
        MindRealm.keepPeople(level, MindRealm.plotOf(player), progress);
        for (MindNpcEntity npc : level.getEntitiesOfClass(MindNpcEntity.class, new AABB(o).inflate(70, 80, 70))) {
            if (npc.role() == MindNpcEntity.Role.HORTON) {
                npc.discard();
            }
        }
    }

    /** Every tick inside: walking up to the Human Torch starts his talk. */
    static void tick(ServerPlayer player) {
        if (player.tickCount % 10 != 0 || MindTalk.talking(player) || MindProgress.of(player).torchDone()) {
            return;
        }
        Vec3 torch = MindBuilder.torch(origin(player));
        if (player.position().distanceToSqr(torch) < 5.5 * 5.5) {
            MindTalk.start(player, "torch", find(player, MindNpcEntity.Role.TORCH), true);
        }
    }
}

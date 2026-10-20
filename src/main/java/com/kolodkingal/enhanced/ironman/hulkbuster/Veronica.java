package com.kolodkingal.enhanced.ironman.hulkbuster;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.nano.NanoTech;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Veronica, the Hulkbuster's satellite (3.8, lane L4, PLAN 6.4): she drops the frame for the suit-up ({@link
 * Hulkbuster}'s listener) and, on N, <b>a new arm</b> from orbit that repairs the suit by {@code veronicaRepair} 50 %
 * of its durability ({@code veronicaCooldown} 1200). Both need the sky above him ({@code needsSky}: no block that stops
 * a falling frame within {@code dropHeight} 40 above): "Veronica can't reach you in here."
 */
public final class Veronica {
    /** The ticks the arm falls before it lands on him (the repair comes with it). */
    public static final int ARM_FALL_TICKS = 20;

    /** When each wearer's new arm lands (game time). */
    private static final Map<UUID, Long> ARMS = new HashMap<>();

    public static int dropHeight() {
        return NanoTech.cfg(IronManConfig.HULKBUSTER_DROP_HEIGHT, 40);
    }

    /**
     * Whether the sky is open above {@code from} for {@code height} blocks: no block a falling frame would stop at
     * (anything with a collision shape: glass, leaves and barriers too). Above the world's top it is open.
     */
    public static boolean skyAbove(Level level, BlockPos from, int height) {
        BlockPos.MutableBlockPos pos = from.mutable();
        for (int i = 0; i < height; i++) {
            if (pos.getY() >= level.getMaxBuildHeight()) {
                return true;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) {
                return false;
            }
            pos.move(0, 1, 0);
        }
        return true;
    }

    /**
     * N in the Hulkbuster: Veronica sends a new arm (refused indoors, with the line).
     *
     * @return whether the arm is on its way
     */
    public static boolean callArm(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.IRON_MAN_MISSILES, now) || ARMS.containsKey(player.getUUID())) {
            return false;
        }
        if (NanoTech.cfg(IronManConfig.HULKBUSTER_NEEDS_SKY, true)
                && !skyAbove(player.level(), BlockPos.containing(player.getEyePosition()).above(), dropHeight())) {
            player.displayClientMessage(Component.translatable("message.enhanced.nanotech.veronica_indoors")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        data.startCooldown(HeroAbility.IRON_MAN_MISSILES, now, NanoTech.cfg(IronManConfig.HULKBUSTER_VERONICA_COOLDOWN,
                1200));
        ARMS.put(player.getUUID(), now + ARM_FALL_TICKS);
        ServerLevel level = player.serverLevel();
        Vec3 top = player.position().add(0.0, player.getBbHeight() + dropHeight(), 0.0);
        NanoTech.send(level, player, IronFxPayload.VERONICA, List.of(top, player.position()
                .add(0.0, player.getBbHeight() * 0.6, 0.0)), ARM_FALL_TICKS);
        NanoTech.sound(level, player.position(), ModSounds.IRON_THRUSTERS.get(), 1.0F, 1.4F);
        return true;
    }

    /** Every tick of a Hulkbuster wearer: the arm that lands repairs the suit. */
    static void tick(ServerPlayer player, long now) {
        Long lands = ARMS.get(player.getUUID());
        if (lands == null || now < lands) {
            return;
        }
        ARMS.remove(player.getUUID());
        ItemStack suit = player.getItemBySlot(EquipmentSlot.CHEST);
        if (IronSuits.kind(suit) != com.kolodkingal.enhanced.ironman.SuitKind.HULKBUSTER || !suit.isDamageableItem()) {
            return;
        }
        int repair = (int) Math.round(suit.getMaxDamage() * NanoTech.cfg(IronManConfig.HULKBUSTER_VERONICA_REPAIR,
                0.5));
        suit.setDamageValue(Math.max(0, suit.getDamageValue() - repair));
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(0.0, player.getBbHeight() * 0.6, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 20, 0.6, 0.6, 0.6, 0.3);
        NanoTech.sound(level, at, ModSounds.VERONICA_ARM.get(), 1.5F, 1.0F);
    }

    /** The arm still falling is lost (the suit came off, he left). */
    static void cancel(UUID id) {
        ARMS.remove(id);
    }

    static void clear() {
        ARMS.clear();
    }

    private Veronica() {
    }
}

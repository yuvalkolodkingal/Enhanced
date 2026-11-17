package com.kolodkingal.enhanced.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Billy's Clairvoyance (3.17): he names a player or an item and sees where it is. A player anywhere: his world and his
 * place; in this world a trail of light runs from Billy toward him and he glows. An item: the nearest one of that name
 * lying on the ground, in a frame, or in a player's hands or pack (within 160 blocks).
 */
public final class Clairvoyance {
    private static final double ITEM_RANGE = 160;

    private Clairvoyance() {
    }

    /** The name the caster typed in the box. */
    public static void seek(ServerPlayer caster, String name) {
        String wanted = name.trim();
        if (wanted.isEmpty()) {
            return;
        }
        ServerPlayer target = caster.server.getPlayerList().getPlayerByName(wanted);
        if (target != null && target != caster) {
            reveal(caster, target);
            return;
        }
        Entity holder = findItem(caster, wanted.toLowerCase(Locale.ROOT));
        if (holder == null) {
            caster.displayClientMessage(Component.translatable("message.enhanced.spell.clairvoyance.none", wanted).withStyle(ChatFormatting.GRAY), false);
            return;
        }
        trail(caster, holder.position());
        holder.setGlowingTag(true);
        caster.displayClientMessage(Component.translatable("message.enhanced.spell.clairvoyance.item", wanted, holder.blockPosition().getX(),
                holder.blockPosition().getY(), holder.blockPosition().getZ()).withStyle(ChatFormatting.AQUA), false);
    }

    /** Shows the caster where a player is. */
    public static void reveal(ServerPlayer caster, ServerPlayer target) {
        caster.displayClientMessage(Component.translatable("message.enhanced.spell.clairvoyance.player", target.getDisplayName(),
                target.level().dimension().location().toString(), target.blockPosition().getX(), target.blockPosition().getY(),
                target.blockPosition().getZ()).withStyle(ChatFormatting.AQUA), false);
        if (target.level() == caster.level()) {
            trail(caster, target.position());
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false), caster);
        }
        caster.serverLevel().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /** A trail of light on the ground from the caster toward {@code to} (its first 48 blocks). */
    private static void trail(ServerPlayer caster, Vec3 to) {
        ServerLevel level = caster.serverLevel();
        Vec3 from = caster.position();
        Vec3 d = to.subtract(from);
        double len = Math.min(48, d.length());
        if (len < 0.5) {
            return;
        }
        Vec3 dir = d.normalize();
        List<Vec3> points = new ArrayList<>();
        for (double t = 0; t <= len; t += 2.0) {
            Vec3 p = from.add(dir.scale(t));
            Vec3 ground = Portals.surface(level, p);
            points.add(Math.abs(ground.y - p.y) < 6 ? ground : p);
        }
        MagicFx.send(level, MagicFx.TRAIL, from, points, 0xCFEFFF, 0x5CC8FF, 1.0F, 400);
    }

    @Nullable
    private static Entity findItem(ServerPlayer caster, String wanted) {
        ServerLevel level = caster.serverLevel();
        Entity best = null;
        double bestD = ITEM_RANGE * ITEM_RANGE;
        for (Entity e : level.getEntitiesOfClass(Entity.class, caster.getBoundingBox().inflate(ITEM_RANGE),
                e -> e instanceof ItemEntity || e instanceof ItemFrame || e instanceof ServerPlayer)) {
            if (e == caster) {
                continue;
            }
            boolean match = false;
            if (e instanceof ItemEntity item) {
                match = named(item.getItem(), wanted);
            } else if (e instanceof ItemFrame frame) {
                match = named(frame.getItem(), wanted);
            } else if (e instanceof ServerPlayer other) {
                for (int i = 0; i < other.getInventory().getContainerSize() && !match; i++) {
                    match = named(other.getInventory().getItem(i), wanted);
                }
            }
            double d = e.distanceToSqr(caster);
            if (match && d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private static boolean named(ItemStack stack, String wanted) {
        return !stack.isEmpty() && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(wanted);
    }
}

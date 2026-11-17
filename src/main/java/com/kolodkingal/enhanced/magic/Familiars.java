package com.kolodkingal.enhanced.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A witch's familiar (3.17), come at Billy's call for a while: a black cat (from his second rank), a crow (from his
 * fourth, it flies ahead) or a spectral hound (from his sixth, it fights). Each is tamed to him, glows faintly, and scouts:
 * when something hostile comes near it, or takes aim at him, the familiar warns him (a line, a sound, the foe glows).
 */
public final class Familiars {
    private record Bond(UUID owner, long until, long lastWarn) {
    }

    private static final Map<UUID, Bond> FAMILIARS = new HashMap<>();

    private Familiars() {
    }

    static boolean summon(ServerPlayer p, int ticks) {
        ServerLevel level = p.serverLevel();
        int rank = Math.max(Magic.billyRank(p), Magic.isDemiurge(p) ? 10 : Magic.isWitch(p) ? 2 * Magic.data(p).level() : 2);
        List<EntityType<? extends TamableAnimal>> kinds = new ArrayList<>();
        kinds.add(EntityType.CAT);
        if (rank >= 4) {
            kinds.add(EntityType.PARROT);
        }
        if (rank >= 6) {
            kinds.add(EntityType.WOLF);
        }
        EntityType<? extends TamableAnimal> kind = kinds.get(p.getRandom().nextInt(kinds.size()));
        TamableAnimal pet = kind.create(level);
        if (pet == null) {
            return false;
        }
        Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
        pet.moveTo(at.x, p.getY(), at.z, p.getYRot(), 0.0F);
        pet.tame(p);
        pet.setPersistenceRequired();
        pet.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
        if (kind == EntityType.WOLF) {
            pet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1, false, false));
            pet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1, false, false));
        }
        pet.setCustomName(Component.translatable("entity.enhanced.familiar", p.getDisplayName()).withStyle(ChatFormatting.AQUA));
        pet.addTag("enhanced_familiar");
        level.addFreshEntity(pet);
        FAMILIARS.put(pet.getUUID(), new Bond(p.getUUID(), level.getGameTime() + ticks, 0));
        level.sendParticles(ParticleTypes.WITCH, at.x, p.getY() + 0.5, at.z, 30, 0.4, 0.5, 0.4, 0.05);
        MagicFx.send(level, MagicFx.CIRCLE, at, List.of(), Magic.casterColor(p), 0xFFFFFF, 1.2F, 30);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.8F, 1.6F);
        p.displayClientMessage(Component.translatable("message.enhanced.spell.familiar", pet.getType().getDescription()), true);
        return true;
    }

    public static boolean isFamiliar(Entity e) {
        return FAMILIARS.containsKey(e.getUUID()) || e.getTags().contains("enhanced_familiar");
    }

    @Nullable
    public static UUID ownerOf(Entity e) {
        Bond bond = FAMILIARS.get(e.getUUID());
        return bond == null ? null : bond.owner();
    }

    /** Every second: the familiars scout, warn, and go home when their time is up. */
    public static void tick(MinecraftServer server) {
        if (FAMILIARS.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Bond>> it = FAMILIARS.entrySet().iterator();
        Map<UUID, Bond> updates = new HashMap<>();
        while (it.hasNext()) {
            Map.Entry<UUID, Bond> e = it.next();
            Entity pet = null;
            for (ServerLevel level : server.getAllLevels()) {
                pet = level.getEntity(e.getKey());
                if (pet != null) {
                    break;
                }
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(e.getValue().owner());
            if (pet == null || !pet.isAlive()) {
                it.remove();
                continue;
            }
            if (owner == null || now >= e.getValue().until()) {
                ((ServerLevel) pet.level()).sendParticles(ParticleTypes.WITCH, pet.getX(), pet.getY() + 0.5, pet.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
                pet.discard();
                it.remove();
                continue;
            }
            if (now - e.getValue().lastWarn() < 200) {
                continue;
            }
            for (Mob foe : pet.level().getEntitiesOfClass(Mob.class, pet.getBoundingBox().inflate(14),
                    m -> m.isAlive() && (m instanceof Enemy || m.getTarget() == owner))) {
                if (foe.getTarget() == owner || foe.distanceToSqr(owner) < 20 * 20) {
                    owner.displayClientMessage(Component.translatable("message.enhanced.spell.familiar_warns", pet.getType().getDescription(),
                            foe.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
                    owner.playNotifySound(SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 0.6F, 1.4F);
                    foe.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                    updates.put(e.getKey(), new Bond(e.getValue().owner(), e.getValue().until(), now));
                    break;
                }
            }
        }
        FAMILIARS.putAll(updates);
    }

    public static void clear() {
        FAMILIARS.clear();
    }
}

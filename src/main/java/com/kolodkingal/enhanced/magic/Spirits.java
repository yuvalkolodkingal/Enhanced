package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.hela.ArmyMob;
import com.kolodkingal.enhanced.hela.HelaArmy;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Necromancy and its undoing (3.17): Billy raises the spirit of a creature that died near him a moment ago (it rises
 * glowing, a ghost of itself, and fights for him; with no fresh death a spectral berserker answers), and the Banishment
 * sends every summoned thing back where it came from: these spirits, Hela's army, an evoker's vexes, the familiars.
 */
public final class Spirits {
    private record Death(EntityType<?> type, ResourceKey<Level> dim, Vec3 at, long time) {
    }

    private record Bond(UUID owner, long until) {
    }

    /** The last deaths (the spirits that may be raised). */
    private static final Deque<Death> DEATHS = new ArrayDeque<>();
    private static final Map<UUID, Bond> SPIRITS = new HashMap<>();
    /** How long after a death its spirit may still be raised (30 s). */
    private static final int FRESH_TICKS = 600;

    private Spirits() {
    }

    /** A creature died: its spirit lingers a while. */
    public static void died(LivingEntity e) {
        if (e instanceof Player || e.getType().is(ModTags.BOSSES) || e.getType().is(ModTags.BOSS_MINIONS) || SPIRITS.containsKey(e.getUUID())
                || !(e instanceof Mob)) {
            return;
        }
        DEATHS.addFirst(new Death(e.getType(), e.level().dimension(), e.position(), e.level().getGameTime()));
        while (DEATHS.size() > 64) {
            DEATHS.removeLast();
        }
    }

    /** Raises the freshest spirit near Billy (or a spectral berserker). */
    static boolean raise(ServerPlayer p, int ticks, int color) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        Death found = null;
        for (Death d : DEATHS) {
            if (d.dim() == level.dimension() && now - d.time() <= FRESH_TICKS && d.at().distanceToSqr(p.position()) <= 24 * 24) {
                found = d;
                break;
            }
        }
        if (found != null) {
            DEATHS.remove(found);
            Entity e = found.type().create(level);
            if (e instanceof Mob mob) {
                mob.moveTo(found.at().x, found.at().y, found.at().z, p.getYRot(), 0.0F);
                mob.setPersistenceRequired();
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1, false, false));
                mob.setCustomName(Component.translatable("entity.enhanced.spirit_of", mob.getType().getDescription()).withStyle(ChatFormatting.AQUA));
                mob.addTag("enhanced_spirit");
                level.addFreshEntity(mob);
                SPIRITS.put(mob.getUUID(), new Bond(p.getUUID(), now + ticks));
                level.sendParticles(ParticleTypes.SOUL, found.at().x, found.at().y + 0.5, found.at().z, 30, 0.4, 0.6, 0.4, 0.05);
                MagicFx.send(level, MagicFx.CIRCLE, found.at(), List.of(), color, 0xFFFFFF, 1.5F, 40);
                MagicFx.send(level, MagicFx.AWAKEN, found.at(), List.of(), color, 0xFFFFFF, 1.0F, 30);
                level.playSound(null, found.at().x, found.at().y, found.at().z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5F, 0.6F);
                p.displayClientMessage(Component.translatable("message.enhanced.spell.spirit_raised", mob.getType().getDescription()), true);
                return true;
            }
        }
        HelaArmy.raise(p, 1, ticks);
        p.displayClientMessage(Component.translatable("message.enhanced.spell.spirit_berserker"), true);
        return true;
    }

    /** Whether a creature was summoned by magic (and may be banished). */
    public static boolean summoned(Entity e) {
        return SPIRITS.containsKey(e.getUUID()) || e instanceof Vex || e instanceof ArmyMob army && army.ownerId() != null
                || Familiars.isFamiliar(e) || e.getTags().contains("enhanced_spirit");
    }

    /** Whether it is this player's own. */
    public static boolean ownedBy(Entity e, Player p) {
        Bond bond = SPIRITS.get(e.getUUID());
        if (bond != null) {
            return bond.owner().equals(p.getUUID());
        }
        if (e instanceof ArmyMob army) {
            return p.getUUID().equals(army.ownerId());
        }
        return Familiars.ownerOf(e) != null && p.getUUID().equals(Familiars.ownerOf(e));
    }

    /** Every 10 ticks: the spirits fight their master's foes (and never him or his friends), and fade when their time is up. */
    public static void tick(MinecraftServer server) {
        if (SPIRITS.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Bond>> it = SPIRITS.entrySet().iterator();
        List<UUID> gone = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<UUID, Bond> e = it.next();
            Entity spirit = null;
            for (ServerLevel level : server.getAllLevels()) {
                spirit = level.getEntity(e.getKey());
                if (spirit != null) {
                    break;
                }
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(e.getValue().owner());
            if (!(spirit instanceof Mob mob) || !mob.isAlive()) {
                gone.add(e.getKey());
                continue;
            }
            if (owner == null || now >= e.getValue().until() || owner.level() != mob.level()) {
                ((ServerLevel) mob.level()).sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
                mob.discard();
                gone.add(e.getKey());
                continue;
            }
            LivingEntity target = mob.getTarget();
            if (target != null && (target == owner || Magic.friend(owner, target))) {
                mob.setTarget(null);
                target = null;
            }
            if (target == null || !target.isAlive()) {
                LivingEntity best = null;
                double bestD = 16 * 16;
                for (LivingEntity c : mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16),
                        c -> c != mob && c != owner && !SPIRITS.containsKey(c.getUUID()) && Magic.enemy(owner, c))) {
                    double d = c.distanceToSqr(mob);
                    if (d < bestD) {
                        bestD = d;
                        best = c;
                    }
                }
                mob.setTarget(best);
                if (best == null && mob.distanceToSqr(owner) > 64) {
                    mob.getNavigation().moveTo(owner, 1.2);
                }
            }
        }
        gone.forEach(SPIRITS::remove);
    }

    public static void clear() {
        DEATHS.clear();
        SPIRITS.clear();
    }
}

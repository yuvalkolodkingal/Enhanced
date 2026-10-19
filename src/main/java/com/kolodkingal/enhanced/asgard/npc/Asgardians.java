package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What other lanes ask of the Asgardians (3.5): Heimdall's Gatekeeper's Call (L2) summons Einherjar, Odin's command (L3)
 * sends the warriors at a foe and his coronation makes the hall bow, and anyone may have a named Asgardian say a line.
 */
public final class Asgardians {
    /** How far Odin's command reaches. */
    public static final double COMMAND_RANGE = 64.0;
    /** How far the Asgardians who bow can be from the one they bow to. */
    public static final double BOW_RANGE = 32.0;
    /** How long a bow lasts (ticks). */
    public static final int BOW_TICKS = 60;

    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.35F), 1.4F);

    /**
     * Summons {@code count} Einherjar for {@code owner} for {@code ticks} (they come in a flash of gold round him, follow
     * him, fight what he fights or what attacks him, and vanish in gold when the time is up). Returns them.
     */
    public static List<Entity> summonEinherjar(ServerPlayer owner, int count, int ticks) {
        List<Entity> summoned = new ArrayList<>();
        if (!(owner.level() instanceof ServerLevel level)) {
            return summoned;
        }
        for (int i = 0; i < count; i++) {
            AsgardianEntity einherjar = ModEntities.ASGARDIAN.get().create(level);
            if (einherjar == null) {
                continue;
            }
            einherjar.setRole(Role.EINHERJAR, owner.getRandom().nextInt(Role.EINHERJAR.skins()));
            double angle = owner.getYRot() * Mth.DEG_TO_RAD + Math.PI * 2 * i / Math.max(1, count);
            Vec3 at = owner.position().add(-Math.sin(angle) * 2.0, 0.0, Math.cos(angle) * 2.0);
            BlockPos stand = standOn(level, BlockPos.containing(at));
            einherjar.moveTo(at.x, stand != null ? stand.getY() : owner.getY(), at.z, owner.getYRot(), 0.0F);
            einherjar.summonFor(owner, ticks);
            if (level.addFreshEntity(einherjar)) {
                summoned.add(einherjar);
                level.sendParticles(GOLD, einherjar.getX(), einherjar.getY() + 1.0, einherjar.getZ(), 24, 0.35, 0.8, 0.35, 0.0);
            }
        }
        if (!summoned.isEmpty()) {
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.EINHERJAR_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return summoned;
    }

    /** The nearest place within 3 blocks up or down where a body of two blocks stands on something, or null. */
    @Nullable
    public static BlockPos standOn(ServerLevel level, BlockPos at) {
        for (int dy = 0; dy <= 3; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = at.above(dy * sign);
                if (!level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()
                        && level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                        && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * The Allfather's command: the Einherjar and warriors within {@link #COMMAND_RANGE} of {@code owner} attack {@code
     * target} (or follow him, if null) for {@code ticks}. Returns how many answered.
     */
    public static int command(ServerPlayer owner, @Nullable LivingEntity target, int ticks) {
        int answered = 0;
        for (AsgardianEntity npc : owner.level().getEntitiesOfClass(AsgardianEntity.class, owner.getBoundingBox().inflate(COMMAND_RANGE),
                e -> e.isAlive() && e.role().warrior() && !e.isDown())) {
            if (target != null) {
                if (target instanceof AsgardianEntity || target == owner) {
                    continue;
                }
                npc.endTalk();
                npc.command(target, ticks);
            } else {
                npc.endTalk();
                npc.followFor(owner, ticks);
            }
            answered++;
        }
        return answered;
    }

    /** The Asgardians who can see {@code player} turn to him and bow (the coronation, a god's arrival). */
    public static void bowTo(ServerPlayer player) {
        for (AsgardianEntity npc : player.level().getEntitiesOfClass(AsgardianEntity.class, player.getBoundingBox().inflate(BOW_RANGE),
                e -> e.isAlive() && !e.state().still() && e.state() != NpcState.FIGHTING && e.hasLineOfSight(player))) {
            npc.getNavigation().stop();
            npc.setState(NpcState.BOWING, BOW_TICKS);
            npc.setPartner(player);
            npc.getLookControl().setLookAt(player, 90.0F, 90.0F);
            AsgardianGoals.face(npc, player.position());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.NPC_BOW.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    /** The named Asgardian of a role says one of his lines ({@code npc.enhanced.<role>.<lineKey>}) to those near him. */
    public static void say(ServerLevel level, Role role, String lineKey) {
        AsgardianEntity npc = fixture(level, role);
        if (npc != null) {
            AsgardTalk.announce(npc, AsgardDialogue.variant(AsgardDialogue.lineKey(role, lineKey), npc.getRandom()), 24.0);
        }
    }

    /** The named Asgardian of a role in the level (the keeper's, else any loaded one), or null. */
    @Nullable
    public static AsgardianEntity fixture(ServerLevel level, Role role) {
        UUID id = AsgardPopulation.residents(level) != null ? AsgardPopulation.residents(level).resident(role.name()) : null;
        if (id != null && level.getEntity(id) instanceof AsgardianEntity npc && npc.isAlive()) {
            return npc;
        }
        for (Entity e : level.getAllEntities()) {
            if (e instanceof AsgardianEntity npc && npc.isAlive() && npc.role() == role && !npc.summoned()) {
                return npc;
            }
        }
        return null;
    }

    /** Every Asgardian within {@code range} of {@code at}. */
    public static List<AsgardianEntity> near(ServerLevel level, Vec3 at, double range) {
        return level.getEntitiesOfClass(AsgardianEntity.class, new AABB(at, at).inflate(range), Entity::isAlive);
    }

    // ---- one of each character (3.5.1) ----

    /** The players online who are {@code power} now (by their power, not a custom hero's rows), in the server's order. */
    public static List<ServerPlayer> heroesPlaying(MinecraftServer server, Power power) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (Powers.power(player) == power) {
                out.add(player);
            }
        }
        return out;
    }

    /** Their names, joined ("Eilon" or "Eilon, Peter"), or "" when nobody is {@code power}. */
    public static String heroNames(MinecraftServer server, Power power) {
        StringBuilder names = new StringBuilder();
        for (ServerPlayer player : heroesPlaying(server, power)) {
            if (!names.isEmpty()) {
                names.append(", ");
            }
            names.append(player.getGameProfile().getName());
        }
        return names.toString();
    }

    /** Whether the Asgardian of {@code role} is away now: a player online is its hero ({@link Role#yieldsToHero()}). */
    public static boolean yields(MinecraftServer server, Role role) {
        return role.yieldsToHero() && !heroesPlaying(server, role.hero()).isEmpty();
    }

    /** Whether Odin is awake (Hela has fallen once): his NPC stands in the throne room. */
    static boolean odinAwake(ServerLevel level) {
        return AsgardState.get(level.getServer()).odinAwake();
    }

    private Asgardians() {
    }
}

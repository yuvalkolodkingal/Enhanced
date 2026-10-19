package com.kolodkingal.enhanced.cottage;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.config.CottageConfig;
import com.kolodkingal.enhanced.hydra.HydraGarrison;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Señor Scratchy (3.7, the plan's 9.8; WandaVision): Agatha's brown rabbit, a vanilla rabbit named "Señor Scratchy",
 * persistent, tagged with his cottage's key ({@link #TAG}). The keeper keeps one per cottage at the hutch (made again,
 * unseen, if lost). Each morning, with chance {@code [agatha_cottage] scratchyRunsOff}, he runs off: the keeper moves
 * him, unseen, to a grass spot 30 to 60 blocks from the cottage. Agatha asks the players to find him (whoever says yes
 * sees him glow within 24 blocks); brought within 4 blocks of the hutch (he follows a carrot, or a lead), he hops in,
 * Nicky cheers, and Agatha gives each finder 3 emeralds and a potion of Luck, once per run-off.
 */
public final class Scratchy {
    /** The rabbit's tag: the key of his cottage. */
    public static final String TAG = Enhanced.MODID + ":cottage";
    /** The lang key of his name. */
    public static final String NAME = "entity.enhanced.senor_scratchy";
    /** Within this of the hutch he is home. */
    public static final double HOME_RANGE = 4.0;
    /** How far from the hutch he runs (blocks). */
    public static final int RUN_MIN = 30;
    public static final int RUN_MAX = 60;
    /** A run-off rabbit nobody can find this long (ticks) found his own way home: he is made again at the hutch. */
    public static final long LOST_TICKS = 6000;
    /** Within this of the hutch, the players who looked for him are thanked. */
    public static final double REWARD_RANGE = 16.0;
    /** The players within this of the hutch hear Nicky cheer. */
    public static final double CHEER_RANGE = 24.0;
    /** How far the finder sees him glow (the client's source reads this). */
    public static final double GLOW_RANGE = 24.0;

    static double runsOff() {
        return EnhancedConfig.SPEC.isLoaded() ? CottageConfig.SCRATCHY_RUNS_OFF.get() : 0.3;
    }

    /** Whether an entity is a cottage's Scratchy. */
    public static boolean is(Entity entity) {
        return entity instanceof Rabbit && entity.getPersistentData().contains(TAG);
    }

    /** The key of the cottage a Scratchy belongs to. */
    public static long cottageOf(Entity entity) {
        return entity.getPersistentData().getLong(TAG);
    }

    /** The cottage's Scratchy, if he is loaded and alive. */
    @Nullable
    public static Rabbit find(ServerLevel level, Cottages.Cottage cottage) {
        UUID id = cottage.scratchy();
        return id != null && level.getEntity(id) instanceof Rabbit rabbit && rabbit.isAlive() ? rabbit : null;
    }

    /**
     * The keeper's part: there is one Scratchy per cottage. A missing one is made at the hutch (the first in sight, the
     * next ones unseen); one who ran off and is not loaded is waited for {@link #LOST_TICKS}, then he came home by
     * himself. Returns the rabbit made, or null.
     */
    @Nullable
    static Rabbit keep(ServerLevel level, Cottages.Cottage cottage) {
        if (find(level, cottage) != null) {
            cottage.missingSince = -1L;
            return null;
        }
        BlockPos hutch = cottage.points().hutch();
        if (cottage.scratchyGone()) {
            long now = level.getGameTime();
            if (cottage.missingSince < 0) {
                cottage.missingSince = now;
                return null;
            }
            if (now - cottage.missingSince < LOST_TICKS) {
                return null;
            }
            cottage.setGone(Cottages.HOME);
        }
        if (!CottageKeeper.loaded(level, hutch)) {
            return null;
        }
        boolean first = !cottage.has(Cottages.MADE_SCRATCHY);
        if (!first && !HydraGarrison.unseen(level, Vec3.atBottomCenterOf(hutch), CottageKeeper.NOT_WITHIN)) {
            return null;
        }
        Rabbit rabbit = make(level, cottage);
        if (rabbit == null) {
            return null;
        }
        UUID before = cottage.scratchy();
        cottage.setScratchy(rabbit.getUUID());
        if (!level.addFreshEntity(rabbit)) {
            cottage.setScratchy(before);
            return null;
        }
        cottage.set(Cottages.MADE_SCRATCHY, true);
        return rabbit;
    }

    /** A new Scratchy at the hutch (not yet in the level). */
    @Nullable
    static Rabbit make(ServerLevel level, Cottages.Cottage cottage) {
        Rabbit rabbit = EntityType.RABBIT.create(level);
        if (rabbit == null) {
            return null;
        }
        BlockPos hutch = cottage.points().hutch();
        rabbit.setVariant(Rabbit.Variant.BROWN);
        rabbit.setCustomName(Component.translatable(NAME));
        rabbit.setPersistenceRequired();
        rabbit.getPersistentData().putLong(TAG, cottage.key());
        rabbit.moveTo(hutch.getX() + 0.5, hutch.getY(), hutch.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F,
                0.0F);
        return rabbit;
    }

    // ---- running off ----

    /** The keeper's morning: {@link #morning(ServerLevel, Cottages.Cottage, long, RandomSource, double, boolean)}. */
    static boolean morning(ServerLevel level, Cottages.Cottage cottage, long dayTime, RandomSource random) {
        return morning(level, cottage, dayTime, random, runsOff(), true);
    }

    /**
     * The morning's question (once per in-game day, before 6000): does he run off today? With the chance {@code
     * chance} ({@code scratchyRunsOff}); a yes is kept until he can go ({@code unseen}: when nobody sees him; the
     * keeper tries again at each look). Returns whether he ran off now.
     */
    public static boolean morning(ServerLevel level, Cottages.Cottage cottage, long dayTime, RandomSource random,
                                  double chance, boolean unseen) {
        long day = Math.floorDiv(dayTime, 24000L);
        long time = Math.floorMod(dayTime, 24000L);
        if (cottage.scratchyGone()) {
            return false;
        }
        if (time < 6000L && day != cottage.rollDay()) {
            cottage.setRollDay(day);
            cottage.pendingRun = random.nextDouble() < chance;
        }
        if (!cottage.pendingRun || time >= 6000L) {
            cottage.pendingRun = false;
            return false;
        }
        Rabbit rabbit = find(level, cottage);
        if (rabbit == null || !runOff(level, cottage, rabbit, random, unseen)) {
            return false;
        }
        cottage.pendingRun = false;
        return true;
    }

    /**
     * He runs off: to a grass spot (or, failing that, any dry ground) 30 to 60 blocks from the hutch, in a loaded chunk
     * and (when {@code unseen}) where nobody sees him leave or arrive. Returns whether a spot was found.
     */
    public static boolean runOff(ServerLevel level, Cottages.Cottage cottage, Rabbit rabbit, RandomSource random,
                                 boolean unseen) {
        if (unseen && !HydraGarrison.unseen(level, rabbit.position(), CottageKeeper.NOT_WITHIN)) {
            return false;
        }
        BlockPos spot = spot(level, cottage, random, unseen);
        if (spot == null) {
            return false;
        }
        rabbit.stopRiding();
        rabbit.getNavigation().stop();
        rabbit.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        cottage.setGone(Math.max(1L, level.getGameTime()));
        return true;
    }

    @Nullable
    static BlockPos spot(ServerLevel level, Cottages.Cottage cottage, RandomSource random, boolean unseen) {
        BlockPos hutch = cottage.points().hutch();
        BlockPos fallback = null;
        for (int i = 0; i < 48; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = RUN_MIN + random.nextDouble() * (RUN_MAX - RUN_MIN);
            int x = hutch.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = hutch.getZ() + (int) Math.round(Math.sin(angle) * dist);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            BlockState ground = level.getBlockState(at.below());
            if (!level.getFluidState(at).isEmpty() || !level.getFluidState(at.below()).isEmpty()
                    || ground.getCollisionShape(level, at.below()).isEmpty()
                    || !level.getBlockState(at).getCollisionShape(level, at).isEmpty()
                    || Cottages.get(level).at(at, 2) != null) {
                continue;
            }
            if (unseen && !HydraGarrison.unseen(level, Vec3.atBottomCenterOf(at), CottageKeeper.NOT_WITHIN)) {
                continue;
            }
            if (ground.is(BlockTags.DIRT)) {
                return at;
            }
            if (fallback == null) {
                fallback = at;
            }
        }
        return fallback;
    }

    // ---- coming home ----

    /** Every quick look: a run-off Scratchy within {@link #HOME_RANGE} of his hutch is home. */
    static boolean quick(ServerLevel level, Cottages.Cottage cottage) {
        if (!cottage.scratchyGone()) {
            return false;
        }
        Rabbit rabbit = find(level, cottage);
        BlockPos hutch = cottage.points().hutch();
        if (rabbit == null || rabbit.distanceToSqr(Vec3.atBottomCenterOf(hutch)) > HOME_RANGE * HOME_RANGE) {
            return false;
        }
        comeHome(level, cottage, rabbit);
        return true;
    }

    /**
     * He hops into the hutch: Nicky cheers, and Agatha (Rio while a player is Agatha) thanks the finders: every player
     * within {@link #REWARD_RANGE} of the hutch who said he would look, leads him on a lead or holds a carrot, once per
     * run-off. Returns the players thanked.
     */
    public static List<ServerPlayer> comeHome(ServerLevel level, Cottages.Cottage cottage, Rabbit rabbit) {
        long run = cottage.gone();
        BlockPos hutch = cottage.points().hutch();
        Vec3 home = Vec3.atBottomCenterOf(hutch);
        Entity leader = rabbit.getLeashHolder();
        if (rabbit.isLeashed()) {
            rabbit.dropLeash(true, true);
        }
        rabbit.teleportTo(home.x, home.y, home.z);
        rabbit.getNavigation().stop();
        cottage.setGone(Cottages.HOME);
        level.sendParticles(ParticleTypes.HEART, home.x, home.y + 0.6, home.z, 6, 0.4, 0.3, 0.4, 0.0);
        level.playSound(null, home.x, home.y, home.z, ModSounds.SCRATCHY_FOUND.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        AsgardianEntity nicky = CottageKeeper.resident(level, cottage, Role.NICKY);
        AsgardianEntity thanker = CottageKeeper.resident(level, cottage, Role.AGATHA);
        if (thanker == null) {
            thanker = CottageKeeper.resident(level, cottage, Role.RIO);
        }
        if (nicky != null) {
            nicky.playSound(ModSounds.NICKY_CHEER.get(), 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.HEART, nicky.getX(), nicky.getY() + 2.0, nicky.getZ(), 3, 0.3, 0.2, 0.3,
                    0.0);
        }
        List<ServerPlayer> thanked = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            double d = player.distanceToSqr(home);
            if (nicky != null && d <= CHEER_RANGE * CHEER_RANGE) {
                AsgardTalk.speak(nicky, player, "npc.enhanced.nicky.scratchy_found", List.of());
            }
            if (d > REWARD_RANGE * REWARD_RANGE || player.isSpectator()) {
                continue;
            }
            CottageProgress progress = player.getData(ModAttachments.COTTAGE);
            boolean helped = progress.has(CottageProgress.SCRATCHY_QUEST) || leader == player
                    || player.getMainHandItem().is(Items.CARROT) || player.getOffhandItem().is(Items.CARROT)
                    || player.getMainHandItem().is(Items.GOLDEN_CARROT);
            if (!helped || progress.scratchyRun() == run) {
                continue;
            }
            player.setData(ModAttachments.COTTAGE,
                    progress.without(CottageProgress.SCRATCHY_QUEST).withScratchyRun(run));
            give(player, new ItemStack(Items.EMERALD, 3));
            give(player, PotionContents.createItemStack(Items.POTION, Potions.LUCK));
            if (thanker != null) {
                AsgardTalk.speak(thanker, player, "npc.enhanced." + thanker.role().id() + ".scratchy_reward",
                        List.of());
            }
            thanked.add(player);
        }
        return thanked;
    }

    static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private Scratchy() {
    }
}

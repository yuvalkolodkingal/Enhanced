package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.block.MultiverseBridgeBlock;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.EntityJoinLevelEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.stones.SpaceRealms;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The life of the universes (3.19): the dead of 2149 (and of the What If universes where the dead rose), the twists of
 * the What If universes on their players, the stones in the chests, the bridge's ring and energy, and the name of each
 * universe said to whoever comes into it.
 */
@Subscriber
public final class MultiverseEvents {
    /** How many zombies walk round a player in 2149 (about four times a normal night), in a What If universe of the dead, and in the Darkhold's. */
    static final int ZOMBIE_CAP = 28;
    static final int WHAT_IF_ZOMBIE_CAP = 14;
    static final int DARKHOLD_ZOMBIE_CAP = 8;
    /** How near a player (blocks) the dead are counted, and where new ones rise (between the two). */
    static final int COUNT_RANGE = 56;
    static final int RISE_MIN = 24;
    static final int RISE_MAX = 46;

    /** Player -> the universe he was last told he is in. */
    private static final Map<UUID, Integer> TOLD = new HashMap<>();

    private MultiverseEvents() {
    }

    /** The universe was announced to the player (by a journey): no second announcement. */
    static void announced(ServerPlayer player, int universe) {
        TOLD.put(player.getUUID(), universe);
    }

    /** How many zombies the place keeps round a player (0: none of its own). */
    static int zombieCap(Level level, BlockPos pos) {
        int universe = Multiverse.universeAt(level, pos);
        if (universe == Multiverse.ZOMBIES) {
            return ZOMBIE_CAP;
        }
        if (level.dimension() == Multiverse.WHAT_IF_KEY) {
            WhatIf twist = WhatIf.of(universe);
            return twist == WhatIf.ZOMBIES ? WHAT_IF_ZOMBIE_CAP : twist == WhatIf.DARKHOLD ? DARKHOLD_ZOMBIE_CAP : 0;
        }
        return 0;
    }

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        UniverseTravel.tick(player);
        if (player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        int universe = Multiverse.universeAt(level, pos);
        UniverseStones.check(player);
        Integer told = TOLD.get(player.getUUID());
        if (told == null) {
            TOLD.put(player.getUUID(), universe);
        } else if (told != universe) {
            TOLD.put(player.getUUID(), universe);
            player.displayClientMessage(Component.translatable("message.enhanced.multiverse.now_in", Multiverse.name(universe))
                    .withStyle(ChatFormatting.AQUA), true);
        }
        if (player.tickCount % 100 == 0) {
            UniverseTravel.remember(player);
        }
        if (level.dimension() == Multiverse.WHAT_IF_KEY) {
            twist(player, WhatIf.of(universe));
        }
        if (player.tickCount % 40 == 0 && !player.isSpectator()) {
            int cap = zombieCap(level, pos);
            if (cap > 0) {
                raiseTheDead(level, player, cap);
            }
        }
    }

    /** The twist of a What If universe on a player in it. */
    static void twist(ServerPlayer player, WhatIf twist) {
        switch (twist) {
            case GRAVITY -> {
                effect(player, MobEffects.JUMP, 2);
                effect(player, MobEffects.SLOW_FALLING, 0);
            }
            case SPEED -> effect(player, MobEffects.MOVEMENT_SPEED, 2);
            case GAMMA -> {
                effect(player, MobEffects.DAMAGE_BOOST, 0);
                effect(player, MobEffects.DIG_SPEED, 1);
            }
            case DARKHOLD -> {
                if (player.getRandom().nextInt(30) == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0, true, false, true));
                }
            }
            default -> {
            }
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, 70, amplifier, true, false, true));
    }

    /** New zombies rise round the player, by day as by night, until there are as many as the place keeps. */
    static void raiseTheDead(ServerLevel level, ServerPlayer player, int cap) {
        List<Zombie> near = level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(COUNT_RANGE));
        for (Zombie zombie : near) {
            helmet(zombie);
        }
        int missing = cap - near.size();
        for (int i = 0; i < Math.min(3, missing); i++) {
            double angle = player.getRandom().nextDouble() * Mth.TWO_PI;
            double distance = RISE_MIN + player.getRandom().nextDouble() * (RISE_MAX - RISE_MIN);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            BlockPos column = new BlockPos(x, 0, z);
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!SpaceRealms.safe(level, feet) || level.getNearestPlayer(feet.getX(), feet.getY(), feet.getZ(), RISE_MIN - 4, false) != null) {
                continue;
            }
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) {
                continue;
            }
            zombie.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, player.getRandom().nextFloat() * 360.0F, 0.0F);
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), MobSpawnType.NATURAL, null);
            helmet(zombie);
            level.addFreshEntity(zombie);
        }
    }

    /** A zombie of a universe of the dead wears a helmet that the sun never wears out, and never drops it. */
    static void helmet(Zombie zombie) {
        ItemStack head = zombie.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(zombie.getRandom().nextInt(3) == 0 ? Items.IRON_HELMET
                    : Items.LEATHER_HELMET));
            zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);
        } else if (head.isDamageableItem() && head.getDamageValue() > 0) {
            head.setDamageValue(0);
        }
    }

    @Subscribe
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        Level level = event.getLevel();
        if (entity instanceof Zombie zombie && zombieCap(level, entity.blockPosition()) > 0) {
            helmet(zombie);
        }
        if (!event.loadedFromDisk() && entity instanceof Enemy && level.dimension() == Multiverse.WHAT_IF_KEY
                && WhatIf.of(Multiverse.universeAt(level, entity.blockPosition())) == WhatIf.PEACE) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Level level = player.level();
        if (Multiverse.universeAt(level, event.getPos()) == Multiverse.MAIN) {
            return;
        }
        BlockEntity entity = level.getBlockEntity(event.getPos());
        if (entity != null) {
            UniverseStones.opened(player, entity);
        }
    }

    @Subscribe
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()
                && event.getState().is(ModBlocks.MULTIVERSE_BRIDGE.get())) {
            MultiverseBridge.tearDown(level, event.getPos(), event.getState().getValue(MultiverseBridgeBlock.FACING));
        }
    }

    @Subscribe
    public static void onServerTick(ServerTickEvent.Post event) {
        MultiverseBridge.serverTick(event.getServer());
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        MultiverseBridge.clear();
        TOLD.clear();
    }
}

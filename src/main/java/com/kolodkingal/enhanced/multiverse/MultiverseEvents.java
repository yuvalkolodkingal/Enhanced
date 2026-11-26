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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The life of the universes (3.19): the dead of 2149, the creatures of the random universes that come as other kinds,
 * the series' What Ifs (the Gamma Bomb's, Hydra's city, Ultron's dead world), the stones in the chests, the bridge's ring
 * and energy, and the name of each universe said to whoever comes into it.
 */
@Subscriber
public final class MultiverseEvents {
    /** How many zombies walk round a player in 2149 (about four times a normal night). */
    static final int ZOMBIE_CAP = 28;
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
        return Multiverse.universeAt(level, pos) == Multiverse.ZOMBIES ? ZOMBIE_CAP : 0;
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
        physics(player, Multiverse.isRandomDimension(level.dimension()) ? UniverseRecipe.of(universe) : null);
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
        if (level.dimension() == Multiverse.GAMMA_KEY) {
            GammaWorld.tick(player);
        } else if (level.dimension() == Multiverse.HYDRA_KEY) {
            HydraCity.tick(player);
        } else if (level.dimension() == Multiverse.ULTRON_KEY) {
            UltronWorld.tick(player);
        }
        WhatIfPeople.tick(player);
        Bunkers.tick(player);
        if (level.dimension() == Multiverse.ZOMBIES_KEY && !player.isSpectator()) {
            ZombieWorld.tick(player);
        }
        if (player.tickCount % 40 == 0 && !player.isSpectator()) {
            int cap = zombieCap(level, pos);
            if (cap > 0) {
                raiseTheDead(level, player, cap);
            }
        }
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
        if (level.dimension() == Multiverse.ZOMBIES_KEY && entity instanceof Mob mob && level instanceof ServerLevel server) {
            EntityType<?> zombie = ZombieWorld.onJoin(mob);
            if (zombie != null && SWAPS.size() < 64) {
                SWAPS.add(new Swap(server, zombie, mob.getX(), mob.getY(), mob.getZ(), mob.getYRot()));
                event.setCanceled(true);
                return;
            }
        }
        if (Multiverse.isRandomDimension(level.dimension())) {
            if (!event.loadedFromDisk() && entity instanceof Mob mob && level instanceof ServerLevel server && swap(server, mob)) {
                event.setCanceled(true);
                return;
            }
            if (entity instanceof Mob mob) {
                physics(mob, UniverseRecipe.of(Multiverse.universeAt(level, mob.blockPosition())));
            }
        } else if (level.dimension() == Multiverse.GAMMA_KEY) {
            GammaWorld.hulkify(entity);
        } else if (level.dimension() == Multiverse.HYDRA_KEY) {
            if (HydraCity.onJoin(entity, event.loadedFromDisk())) {
                event.setCanceled(true);
            }
        }
    }

    /** The modifiers of a random universe's gravity (on everyone) and of its creatures' size (not on the players). */
    static final net.minecraft.resources.ResourceLocation GRAVITY = com.kolodkingal.enhanced.Enhanced.id("universe_gravity");
    static final net.minecraft.resources.ResourceLocation SIZE = com.kolodkingal.enhanced.Enhanced.id("universe_size");

    /**
     * The gravity of the universe the entity is in (3.19.1: light as the moon, or heavy), and for a creature its size
     * (giants, or tiny). {@code recipe} null: out of the random universes, as at home.
     */
    static void physics(net.minecraft.world.entity.LivingEntity entity, @org.jetbrains.annotations.Nullable UniverseRecipe recipe) {
        float gravity = recipe == null ? 0.0F : recipe.gravity();
        modifier(entity, net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY, GRAVITY, gravity);
        modifier(entity, net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE, GRAVITY, gravity < 0.0F ? 6.0 : 0.0);
        if (!(entity instanceof ServerPlayer)) {
            float scale = recipe == null ? 1.0F : recipe.scale();
            modifier(entity, net.minecraft.world.entity.ai.attributes.Attributes.SCALE, SIZE, scale - 1.0F);
        }
    }

    private static void modifier(net.minecraft.world.entity.LivingEntity entity,
                                 net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                 net.minecraft.resources.ResourceLocation id, double amount) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        net.minecraft.world.entity.ai.attributes.AttributeModifier existing = instance.getModifier(id);
        if (amount == 0.0) {
            if (existing != null) {
                instance.removeModifier(id);
            }
            return;
        }
        // SAFE_FALL_DISTANCE adds; the gravity and the size multiply.
        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation = attribute == net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE
                ? net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                : net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        if (existing == null || existing.amount() != amount) {
            instance.removeModifier(id);
            instance.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, amount, operation));
        }
    }

    /** A creature that comes to be in a random universe as another kind ({@link UniverseRecipe#creature}), made next tick. */
    private record Swap(ServerLevel level, EntityType<?> type, double x, double y, double z, float yaw) {
    }

    private static final List<Swap> SWAPS = new ArrayList<>();
    /** The tag of a creature that already came as the kind its universe wanted (it is never swapped again). */
    static final String SWAPPED = "enhanced_swapped";

    /** Whether the new creature comes as another kind here: it is not added, and the other kind is made in its place. */
    static boolean swap(ServerLevel level, Mob mob) {
        if (mob.getTags().contains(SWAPPED) || mob.isPersistenceRequired() || mob.isPassenger() || mob.isVehicle()) {
            return false;
        }
        EntityType<?> to = UniverseRecipe.of(Multiverse.universeAt(level, mob.blockPosition())).creature(mob.getType());
        if (to == mob.getType() || SWAPS.size() >= 64) {
            return false;
        }
        SWAPS.add(new Swap(level, to, mob.getX(), mob.getY(), mob.getZ(), mob.getYRot()));
        return true;
    }

    private static void makeSwaps() {
        if (SWAPS.isEmpty()) {
            return;
        }
        List<Swap> now = new ArrayList<>(SWAPS);
        SWAPS.clear();
        for (Swap swap : now) {
            BlockPos at = BlockPos.containing(swap.x(), swap.y(), swap.z());
            if (!swap.level().isLoaded(at)) {
                continue;
            }
            Entity made = swap.type().create(swap.level());
            if (!(made instanceof Mob mob)) {
                continue;
            }
            mob.moveTo(swap.x(), swap.y(), swap.z(), swap.yaw(), 0.0F);
            mob.addTag(SWAPPED);
            mob.finalizeSpawn(swap.level(), swap.level().getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
            swap.level().addFreshEntity(mob);
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
        makeSwaps();
        HydraCity.serverTick(event.getServer());
        WhatIfPeople.serverTick(event.getServer());
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        MultiverseBridge.clear();
        TOLD.clear();
        SWAPS.clear();
        HydraCity.clear();
        UltronWorld.clear();
    }
}

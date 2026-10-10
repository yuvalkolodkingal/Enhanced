package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.block.MjolnirBlockEntity;
import com.kolodkingal.enhanced.entity.ThrownMjolnirEntity;
import com.kolodkingal.enhanced.network.ThorFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thor, the God of Thunder. His power is his own, but Mjolnir focuses it: he throws it and calls it back, flies by
 * spinning it, and calls lightning down through it. Whoever lifts Mjolnir first, if worthy and without another power,
 * becomes Thor.
 */
public final class ThorAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("thor_body_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("thor_body_armor");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("thor_body_knockback");
    private static final ResourceLocation THUNDER_DAMAGE = Enhanced.id("thor_god_of_thunder_damage");
    private static final ResourceLocation THUNDER_TOUGHNESS = Enhanced.id("thor_god_of_thunder_toughness");

    public static final int THROW_COOLDOWN = 10;
    public static final int LIGHTNING_COOLDOWN = 30;
    /** Creatures this close to a strike are hit too (at reduced damage). */
    public static final double STRIKE_RADIUS = 3.0;
    public static final float ARC_DAMAGE = 4.0F;
    public static final double ARC_RANGE = 6.0;
    public static final int ARC_JUMPS = 2;
    /** Ticks the Bifrost's light gathers around Thor before it takes him. */
    public static final int BIFROST_CHARGE = 30;
    public static final int BIFROST_LOOK_RANGE = 512;

    /** Thor → the Bifrost that is about to take him. */
    private static final Map<UUID, Bifrost> BIFROSTS = new HashMap<>();
    /** Thor → game time until which he holds Mjolnir up to the sky (pose). */
    private static final Map<UUID, Long> CALLING = new HashMap<>();

    private record Bifrost(ResourceKey<Level> dimension, Vec3 to, float yaw, int[] ticksLeft) {
    }

    // ---- input ----

    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        if (!down || player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> throwOrRecall(player, data);
            case 2 -> {
                if (!player.mayFly()) {
                    player.displayClientMessage(Component.translatable("message.enhanced.thor.flight_needs_mjolnir"), true);
                } else {
                    PowerTicker.toggleFlight(player);
                }
            }
            case 3 -> callLightning(player, data);
            case 4 -> {
                boolean on = !data.toggle(Toggle.GOD_OF_THUNDER);
                if (on && !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0) {
                    player.displayClientMessage(Component.translatable("message.enhanced.thor.too_hungry"), true);
                    return;
                }
                data.setToggle(Toggle.GOD_OF_THUNDER, on);
                if (on) {
                    ServerLevel level = player.serverLevel();
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.3F);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 1.0, 0.5, 0.4);
                }
                player.displayClientMessage(Component.translatable(on ? "message.enhanced.god_of_thunder.on" : "message.enhanced.god_of_thunder.off"), true);
            }
            case 5 -> bifrost(player, data);
            default -> {
            }
        }
    }

    // ---- Mjolnir and the worthy (runs for every player) ----

    public static boolean isMjolnir(ItemStack stack) {
        return stack.is(ModItems.MJOLNIR.get());
    }

    /** Mjolnir in either hand. */
    public static boolean holdsMjolnir(Player player) {
        return isMjolnir(player.getMainHandItem()) || isMjolnir(player.getOffhandItem());
    }

    /**
     * The worthy who lift Mjolnir become Thor (if they have no other power); in the hands of the unworthy it is far too
     * heavy, and drops to the ground wherever they are.
     */
    public static void mjolnirTick(ServerPlayer player, PowerData data) {
        if (player.isSpectator()) {
            return;
        }
        boolean worthy = Worthiness.isWorthy(player);
        if (!worthy) {
            dropFromUnworthy(player);
            return;
        }
        if (data.power() == Power.NONE && holdsMjolnir(player)) {
            awaken(player);
        }
    }

    /** Every Mjolnir the unworthy player carries (inventory, or on the cursor) crashes to the ground. */
    private static void dropFromUnworthy(ServerPlayer player) {
        boolean dropped = false;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isMjolnir(stack)) {
                inventory.setItem(slot, ItemStack.EMPTY);
                fall(player, stack);
                dropped = true;
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (isMjolnir(carried)) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            fall(player, carried);
            dropped = true;
        }
        if (dropped) {
            player.displayClientMessage(Component.translatable("message.enhanced.mjolnir.too_heavy").withStyle(ChatFormatting.GRAY), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }

    /** Mjolnir drops straight down at the player's feet. */
    private static void fall(ServerPlayer player, ItemStack stack) {
        ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getY() + 0.3, player.getZ(), stack.copy());
        item.setDeltaMovement(0, -0.2, 0);
        item.setPickUpDelay(40);
        player.level().addFreshEntity(item);
    }

    /** The worthy lift Mjolnir: lightning answers, and a new God of Thunder stands there. */
    private static void awaken(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Powers.awaken(player, Power.THOR);
    }

    // ---- passives ----

    public static void tick(ServerPlayer player, PowerData data) {
        // The hammer in flight may have been caught, dropped or unloaded.
        if (data.hammerEntityId() != -1 && !(player.serverLevel().getEntity(data.hammerEntityId()) instanceof ThrownMjolnirEntity)) {
            data.setHammerEntityId(-1);
        }
        if (data.toggle(Toggle.GOD_OF_THUNDER)) {
            boolean starving = !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
            if (starving) {
                data.setToggle(Toggle.GOD_OF_THUNDER, false);
                player.displayClientMessage(Component.translatable("message.enhanced.god_of_thunder.off"), true);
            } else {
                player.causeFoodExhaustion(EnhancedConfig.GOD_OF_THUNDER_EXHAUSTION.get().floatValue());
            }
        }
        Long calling = CALLING.get(player.getUUID());
        boolean isCalling = calling != null && player.level().getGameTime() < calling;
        if (calling != null && !isCalling) {
            CALLING.remove(player.getUUID());
        }
        data.set(Live.CALLING_LIGHTNING, isCalling);
        bifrostTick(player, data);
    }

    /** Thor flies by spinning Mjolnir: only while he holds it (or it is on its way back to him). */
    public static boolean canFly(ServerPlayer player, PowerData data) {
        return holdsMjolnir(player) || data.hammerEntityId() != -1;
    }

    /** Asgardian body (30 health, 6 armor, hard to knock back); the God of Thunder hits harder and takes less. */
    public static void applyBody(ServerPlayer player, PowerData data) {
        boolean thor = data.power() == Power.THOR;
        boolean unleashed = thor && data.toggle(Toggle.GOD_OF_THUNDER);
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, 10.0, thor);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, 6.0, thor);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK, 0.5, thor);
        PowerTicker.setModifier(player, Attributes.ATTACK_DAMAGE, THUNDER_DAMAGE, 4.0, unleashed);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, THUNDER_TOUGHNESS, 6.0, unleashed);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static boolean isThor(Entity entity) {
        return entity instanceof Player player && Powers.is(player, Power.THOR);
    }

    // ---- Ability 1: throw Mjolnir, or call it back ----

    private static void throwOrRecall(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(1, now)) {
            return;
        }
        InteractionHand hand = isMjolnir(player.getMainHandItem()) ? InteractionHand.MAIN_HAND
                : isMjolnir(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        if (hand != null) {
            ItemStack hammer = player.getItemInHand(hand);
            ThrownMjolnirEntity thrown = new ThrownMjolnirEntity(level, player, hammer);
            player.setItemInHand(hand, ItemStack.EMPTY);
            level.addFreshEntity(thrown);
            data.setHammerEntityId(thrown.getId());
            data.startCooldown(1, now, THROW_COOLDOWN);
            player.swing(hand, true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.2F, 0.6F);
            return;
        }
        if (recall(player, data)) {
            data.startCooldown(1, now, THROW_COOLDOWN);
        } else {
            player.displayClientMessage(Component.translatable("message.enhanced.mjolnir.out_of_reach"), true);
        }
    }

    /**
     * Calls Mjolnir home, from wherever it is nearest: the rest of his inventory, the air, the ground (dropped, or
     * resting as a block). Returns false if there is no Mjolnir within reach.
     */
    public static boolean recall(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        // Already his: in the inventory (straight to hand) or flying (turn it around).
        int slot = findInInventory(player);
        if (slot >= 0) {
            ItemStack inHand = player.getMainHandItem();
            Inventory inventory = player.getInventory();
            player.setItemInHand(InteractionHand.MAIN_HAND, inventory.getItem(slot));
            inventory.setItem(slot, inHand);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
            return true;
        }
        if (level.getEntity(data.hammerEntityId()) instanceof ThrownMjolnirEntity flying) {
            flying.setReturning(true);
            return true;
        }
        double range = EnhancedConfig.MJOLNIR_RECALL_RANGE.get();
        Vec3 eye = player.getEyePosition();
        // Lying on the ground as an item.
        ItemEntity nearestItem = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(range),
                        e -> isMjolnir(e.getItem()) && e.distanceToSqr(player) <= range * range).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
        // Resting as a block.
        BlockPos nearestBlock = findBlock(level, player.blockPosition(), range);
        Vec3 from;
        ItemStack hammer;
        if (nearestItem != null && (nearestBlock == null || nearestItem.distanceToSqr(eye) <= nearestBlock.getCenter().distanceToSqr(eye))) {
            from = nearestItem.position().add(0, 0.25, 0);
            hammer = nearestItem.getItem().split(1);
            if (nearestItem.getItem().isEmpty()) {
                nearestItem.discard();
            }
        } else if (nearestBlock != null) {
            from = nearestBlock.getCenter();
            hammer = MjolnirBlock.lift(level, nearestBlock);
        } else {
            return false;
        }
        ThrownMjolnirEntity returning = ThrownMjolnirEntity.recalled(level, player, hammer, from);
        level.addFreshEntity(returning);
        data.setHammerEntityId(returning.getId());
        level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    private static int findInInventory(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isMjolnir(inventory.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    /** The nearest Mjolnir block within range, among the loaded chunks around the player. */
    @Nullable
    private static BlockPos findBlock(ServerLevel level, BlockPos center, double range) {
        int chunks = (int) Math.ceil(range / 16.0);
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        BlockPos best = null;
        double bestDist = range * range;
        for (int x = cx - chunks; x <= cx + chunks; x++) {
            for (int z = cz - chunks; z <= cz + chunks; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof MjolnirBlockEntity) {
                        double d = be.getBlockPos().distSqr(center);
                        if (d <= bestDist) {
                            bestDist = d;
                            best = be.getBlockPos();
                        }
                    }
                }
            }
        }
        return best;
    }

    // ---- Ability 3: lightning ----

    private static void callLightning(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(3, now)) {
            return;
        }
        int range = EnhancedConfig.THOR_LIGHTNING_RANGE.get();
        HitResult hit = Raycasts.any(player, range, e -> (e instanceof LivingEntity || e instanceof EndCrystal) && e != player
                && e.isAlive() && !e.isSpectator(), 1.0F);
        Vec3 target;
        if (hit instanceof EntityHitResult entityHit) {
            target = entityHit.getEntity().position();
        } else {
            target = ground(level, hit.getLocation());
        }
        data.startCooldown(3, now, LIGHTNING_COOLDOWN);
        CALLING.put(player.getUUID(), now + 14);
        data.set(Live.CALLING_LIGHTNING, true);
        strike(level, player, target, true);
        if (data.toggle(Toggle.GOD_OF_THUNDER)) {
            // Unleashed, the storm answers twice more around the target.
            for (int i = 0; i < 2; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                Vec3 around = target.add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                strike(level, player, ground(level, around.add(0, 3, 0)), false);
            }
        }
        // Everyone nearby sees the lightning come down into Mjolnir and leap from it to the target.
        if (holdsMjolnir(player)) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new ThorFxPayload(ThorFxPayload.CHANNEL, player.getId(), List.of(target)));
        }
    }

    /** The first solid ground at or below a point (up to 32 blocks down). */
    private static Vec3 ground(ServerLevel level, Vec3 point) {
        BlockHitResult down = level.clip(new ClipContext(point, point.subtract(0, 32, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        return down.getType() == HitResult.Type.MISS ? point : down.getLocation();
    }

    /**
     * A bolt of lightning at {@code target}: everything within 3 blocks is struck (full damage at the centre), with
     * the usual effects of lightning on creatures (creepers charge, pigs turn into zombified piglins...), but no fire
     * on blocks. Thor himself is never hurt by lightning.
     */
    public static void strike(ServerLevel level, ServerPlayer thor, Vec3 target, boolean main) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(target.x, target.y, target.z);
        bolt.setVisualOnly(true);
        bolt.setCause(thor);
        level.addFreshEntity(bolt);
        float damage = EnhancedConfig.THOR_LIGHTNING_DAMAGE.get().floatValue() * (main ? 1.0F : 0.75F);
        AABB area = new AABB(target, target).inflate(STRIKE_RADIUS, STRIKE_RADIUS + 1, STRIKE_RADIUS);
        for (Entity e : level.getEntities(thor, area, e -> (e instanceof LivingEntity || e instanceof EndCrystal) && e.isAlive()
                && !e.isSpectator() && !isThor(e))) {
            double distance = e.position().distanceTo(target);
            if (distance > STRIKE_RADIUS + 0.5) {
                continue;
            }
            float falloff = distance < 1.5 ? 1.0F : 0.6F;
            e.hurt(ModDamageTypes.source(level, DamageTypes.LIGHTNING_BOLT, thor, bolt), damage * falloff);
            // Vanilla's reactions to lightning (its own damage is absorbed by the hit above).
            e.thunderHit(level, bolt);
        }
    }

    /**
     * The God of Thunder's lightning leaps from a hit to the target and on to the next creatures nearby, up to two
     * jumps. Seen by everyone around.
     */
    public static void arc(ServerLevel level, ServerPlayer thor, Vec3 from, Entity first) {
        List<Vec3> points = new ArrayList<>();
        points.add(from);
        List<Entity> struck = new ArrayList<>();
        Entity current = first;
        for (int jump = 0; current != null && jump <= ARC_JUMPS; jump++) {
            struck.add(current);
            points.add(current.position().add(0, current.getBbHeight() * 0.6, 0));
            current.hurt(ModDamageTypes.source(level, DamageTypes.LIGHTNING_BOLT, thor, thor), ARC_DAMAGE);
            Entity from2 = current;
            current = level.getEntities(thor, current.getBoundingBox().inflate(ARC_RANGE), e -> e instanceof LivingEntity && e.isAlive()
                            && !e.isSpectator() && !struck.contains(e) && !isThor(e)).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(from2))).orElse(null);
        }
        level.playSound(null, first.getX(), first.getY(), first.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.5F, 1.6F);
        PacketDistributor.sendToPlayersNear(level, null, from.x, from.y, from.z, 64,
                new ThorFxPayload(ThorFxPayload.ARC, thor.getId(), points));
    }

    /** A melee hit by the God of Thunder arcs lightning (called for every hit Thor lands). */
    public static void onMeleeHit(ServerPlayer thor, LivingEntity target) {
        PowerData data = Powers.get(thor);
        if (data.power() == Power.THOR && data.toggle(Toggle.GOD_OF_THUNDER)) {
            arc(thor.serverLevel(), thor, thor.getEyePosition().subtract(0, 0.4, 0), target);
        }
    }

    // ---- Ability 5: the Bifrost ----

    /**
     * "Heimdall! Open the Bifrost!": a beam of rainbow light gathers around Thor and, a moment later, takes him home to
     * his respawn point (bed or respawn anchor, otherwise the world spawn), even in another dimension. Sneak: to the
     * spot he looks at instead, up to 512 blocks away. The Bifrost burns its circle of runes into the ground where it
     * lands and where it left.
     */
    private static void bifrost(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(5, now) || BIFROSTS.containsKey(player.getUUID())) {
            return;
        }
        ResourceKey<Level> dimension;
        Vec3 to;
        if (player.isShiftKeyDown()) {
            BlockHitResult hit = Raycasts.block(player, BIFROST_LOOK_RANGE, 1.0F);
            if (hit.getType() == HitResult.Type.MISS) {
                player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_ground"), true);
                return;
            }
            dimension = level.dimension();
            to = Vec3.atBottomCenterOf(hit.getBlockPos().above());
        } else {
            DimensionTransition home = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
            dimension = home.newLevel().dimension();
            to = home.pos();
        }
        data.startCooldown(5, now, EnhancedConfig.BIFROST_COOLDOWN.get());
        BIFROSTS.put(player.getUUID(), new Bifrost(dimension, to, player.getYRot(), new int[]{BIFROST_CHARGE}));
        data.set(Live.BIFROST, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    private static void bifrostTick(ServerPlayer player, PowerData data) {
        Bifrost bifrost = BIFROSTS.get(player.getUUID());
        if (bifrost == null) {
            data.set(Live.BIFROST, false);
            return;
        }
        // Held in the beam: rising slightly, unable to walk away.
        player.setDeltaMovement(0, 0.02, 0);
        player.hurtMarked = true;
        player.resetFallDistance();
        if (--bifrost.ticksLeft()[0] > 0) {
            return;
        }
        BIFROSTS.remove(player.getUUID());
        data.set(Live.BIFROST, false);
        ServerLevel from = player.serverLevel();
        ServerLevel destination = player.server.getLevel(bifrost.dimension());
        if (destination == null) {
            return;
        }
        Vec3 start = player.position();
        mark(from, start);
        player.teleportTo(destination, bifrost.to().x, bifrost.to().y, bifrost.to().z, bifrost.yaw(), player.getXRot());
        player.resetFallDistance();
        destination.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.4F);
        PacketDistributor.sendToPlayersNear(destination, null, player.getX(), player.getY(), player.getZ(), 128,
                new ThorFxPayload(ThorFxPayload.BIFROST_ARRIVAL, player.getId(), List.of(player.position())));
        mark(destination, player.position());
    }

    /** The Bifrost's circle of runes, burned into the ground (seen by everyone nearby, fading after a minute). */
    private static void mark(ServerLevel level, Vec3 at) {
        Vec3 ground = ground(level, at.add(0, 0.5, 0));
        PacketDistributor.sendToPlayersNear(level, null, ground.x, ground.y, ground.z, 128,
                new ThorFxPayload(ThorFxPayload.BIFROST_MARK, -1, List.of(ground)));
    }

    public static boolean inBifrost(Player player) {
        return BIFROSTS.containsKey(player.getUUID());
    }

    // ---- cleanup ----

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
        BIFROSTS.remove(player.getUUID());
        CALLING.remove(player.getUUID());
        data.set(Live.BIFROST, false);
        data.set(Live.CALLING_LIGHTNING, false);
        data.setToggle(Toggle.GOD_OF_THUNDER, false);
        // A hammer in flight finds no Thor to return to, and drops by itself.
    }

    public static void clear() {
        BIFROSTS.clear();
        CALLING.clear();
    }

    private ThorAbilities() {
    }
}

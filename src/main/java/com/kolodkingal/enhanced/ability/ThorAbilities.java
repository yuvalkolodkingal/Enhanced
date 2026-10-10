package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.block.MjolnirBlockEntity;
import com.kolodkingal.enhanced.client.screen.BifrostScreen;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.entity.MjolnirPinEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.network.ThorFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
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
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Thor, the God of Thunder. His power is his own, but his weapon focuses it, Mjolnir or Stormbreaker: he throws it and
 * calls it back, flies by spinning it, and fires lightning from his hand, twice as strong through the weapon. Mjolnir
 * can also shatter into fragments that strike his enemies ({@link MjolnirShatter}) and be set down on someone to pin
 * them in place ({@link MjolnirPin}). Whoever claims Mjolnir first (the first worthy hand to lift it), if they have no
 * other power, becomes Thor; after that only its owner can lift it.
 * <p>
 * Either weapon also calls the Bifrost, for Thor or anyone else who holds it: home, to where they look, or to a place
 * they saved, in any dimension (the places and the cooldown live in {@link BifrostData}).
 */
public final class ThorAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("thor_body_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("thor_body_armor");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("thor_body_knockback");
    private static final ResourceLocation THUNDER_DAMAGE = Enhanced.id("thor_god_of_thunder_damage");
    private static final ResourceLocation THUNDER_TOUGHNESS = Enhanced.id("thor_god_of_thunder_toughness");

    public static final int THROW_COOLDOWN = 10;
    public static final float ARC_DAMAGE = 4.0F;
    public static final double ARC_RANGE = 6.0;
    public static final int ARC_JUMPS = 2;
    /** Ticks the Bifrost's light gathers around its traveller before it takes them. */
    public static final int BIFROST_CHARGE = 30;
    public static final int BIFROST_LOOK_RANGE = 512;
    /** If a saved place has been built over, the Bifrost sets its traveller down on the first free spot up to this high. */
    private static final int ARRIVAL_SEARCH = 24;

    /**
     * What Thor's lightning beam can hit: creatures (other Thors excepted: lightning never hurts them), the parts of
     * the Ender Dragon and End Crystals. Shared by the server (who is hit) and the client (where the beam is drawn to).
     */
    public static final Predicate<Entity> BEAM_TARGET = e -> e.isPickable() && !e.isSpectator() && e.isAlive()
            && (e instanceof LivingEntity || e instanceof PartEntity<?> || e instanceof EndCrystal) && !isThor(e);

    /**
     * What this Thor's own beam can hit: {@link #BEAM_TARGET}, but never the steed he rides, nor whoever rides with
     * him.
     */
    public static Predicate<Entity> beamTargets(Entity thor) {
        return e -> BEAM_TARGET.test(e) && !thor.isPassengerOfSameVehicle(e);
    }

    /** Traveller → the Bifrost that is about to take them. */
    private static final Map<UUID, Journey> JOURNEYS = new HashMap<>();
    /** Thor → his lightning beam while V is held. */
    private static final Map<UUID, Beam> BEAMS = new HashMap<>();

    /**
     * Thor's beam: when it may hit next (kept between holds, so that letting go and pressing again can't hit more often
     * than the interval), and the creatures it has hit since V went down (they get lightning's effects once).
     */
    private static final class Beam {
        long nextHit;
        final IntSet struck = new IntOpenHashSet();
    }

    /** Where the Bifrost is taking someone, and the game time it takes them. */
    private record Journey(ResourceKey<Level> dimension, Vec3 to, float yaw, long arrivesAt) {
    }

    // ---- input ----

    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        // The lightning beam is held: letting go must always get through, even for a Thor who just became a spectator.
        if (ability == 3) {
            lightningBeam(player, data, down);
            return;
        }
        if (!down || player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                InteractionHand hand = weaponHand(player);
                if (player.isShiftKeyDown() && hand != null && isMjolnir(player.getItemInHand(hand))) {
                    // Sneak + R: Mjolnir shatters (Stormbreaker doesn't: it is thrown as usual).
                    MjolnirShatter.shatter(player, data, hand);
                } else {
                    throwOrRecall(player, data);
                }
            }
            case 2 -> {
                if (!player.mayFly()) {
                    player.displayClientMessage(Component.translatable("message.enhanced.thor.flight_needs_weapon"), true);
                } else {
                    PowerTicker.toggleFlight(player);
                }
            }
            case 4 -> {
                boolean on = !data.toggle(Toggle.GOD_OF_THUNDER);
                if (on && starving(player)) {
                    player.displayClientMessage(Component.translatable("message.enhanced.thor.too_hungry"), true);
                    return;
                }
                data.setToggle(Toggle.GOD_OF_THUNDER, on);
                if (on) {
                    ServerLevel level = player.serverLevel();
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GOD_OF_THUNDER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 1.0, 0.5, 0.4);
                }
                player.displayClientMessage(Component.translatable(on ? "message.enhanced.god_of_thunder.on" : "message.enhanced.god_of_thunder.off"), true);
            }
            // Without Sneak the client opens the Bifrost screen instead; a bare key press goes home.
            case 5 -> {
                if (player.isShiftKeyDown()) {
                    bifrostToLook(player);
                } else {
                    bifrostHome(player);
                }
            }
            default -> {
            }
        }
    }

    // ---- the weapons, the worthy and the Bifrost (runs for every player) ----

    public static boolean isMjolnir(ItemStack stack) {
        return stack.is(ModItems.MJOLNIR.get());
    }

    public static boolean isStormbreaker(ItemStack stack) {
        return stack.is(ModItems.STORMBREAKER.get());
    }

    /** Mjolnir or Stormbreaker. */
    public static boolean isWeapon(ItemStack stack) {
        return isMjolnir(stack) || isStormbreaker(stack);
    }

    /** Mjolnir or Stormbreaker in either hand. */
    public static boolean holdsWeapon(LivingEntity entity) {
        return weaponHand(entity) != null;
    }

    /** The hand holding Mjolnir or Stormbreaker (the main hand first), or null. */
    @Nullable
    public static InteractionHand weaponHand(LivingEntity entity) {
        return isWeapon(entity.getMainHandItem()) ? InteractionHand.MAIN_HAND
                : isWeapon(entity.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
    }

    /**
     * Every player: the first worthy hand to carry an unclaimed Mjolnir claims it; one they may not lift (they are not
     * worthy, or it belongs to someone else) is far too heavy, and drops to the ground wherever they are. The worthy
     * who hold their own become Thor (if they have no other power). And whoever the Bifrost is about to take, Thor or
     * not, is held in its light.
     */
    public static void everyPlayerTick(ServerPlayer player, PowerData data) {
        bifrostTick(player, data);
        MjolnirShatter.tick(player);
        MjolnirPin.tickPlayer(player);
        if (player.isSpectator()) {
            return;
        }
        settleMjolnirs(player);
        if (data.power() == Power.NONE && holdsOwnMjolnir(player)) {
            awaken(player);
        }
    }

    /** Mjolnir in either hand, and theirs to hold as Thor (see {@link Worthiness#isTheirs}). */
    private static boolean holdsOwnMjolnir(Player player) {
        if (!Worthiness.isWorthy(player)) {
            return false;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isMjolnir(stack) && Worthiness.isTheirs(player, stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every Mjolnir the player carries (inventory, or on the cursor): one nobody owns yet is claimed by them if they
     * are worthy, and one they may not lift crashes to the ground.
     */
    private static void settleMjolnirs(ServerPlayer player) {
        boolean dropped = false;
        MjolnirOwner heavyFor = null;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isMjolnir(stack)) {
                continue;
            }
            if (Worthiness.canLift(player, stack)) {
                keep(player, stack);
            } else {
                inventory.setItem(slot, ItemStack.EMPTY);
                fall(player, stack);
                heavyFor = Worthiness.ownerKeepingFrom(player, stack);
                dropped = true;
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (isMjolnir(carried)) {
            if (Worthiness.canLift(player, carried)) {
                keep(player, carried);
            } else {
                player.containerMenu.setCarried(ItemStack.EMPTY);
                fall(player, carried);
                heavyFor = Worthiness.ownerKeepingFrom(player, carried);
                dropped = true;
            }
        }
        if (dropped) {
            player.displayClientMessage((heavyFor != null ? Component.translatable("message.enhanced.mjolnir.slips", heavyFor.name())
                    : Component.translatable("message.enhanced.mjolnir.too_heavy")).withStyle(ChatFormatting.GRAY), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }

    /** A Mjolnir the player may carry: it is theirs now if nobody owned it, and it shows their current name. */
    private static void keep(ServerPlayer player, ItemStack mjolnir) {
        if (Worthiness.claim(player, mjolnir)) {
            return;
        }
        MjolnirOwner owner = Worthiness.ownerOf(mjolnir);
        if (owner != null && owner.is(player) && !owner.name().equals(player.getGameProfile().getName())) {
            mjolnir.set(ModDataComponents.OWNER.get(), MjolnirOwner.of(player));
        }
    }

    /** Mjolnir drops straight down at the player's feet. */
    private static void fall(ServerPlayer player, ItemStack stack) {
        dropAt(player.serverLevel(), player.position(), stack.copy());
    }

    /** A hammer (or anything) drops straight down at a point; Mjolnir comes to rest as a block where it lands. */
    public static void dropAt(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, at.x, at.y + 0.3, at.z, stack);
        item.setDeltaMovement(0, -0.2, 0);
        item.setPickUpDelay(40);
        level.addFreshEntity(item);
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
        // The weapon in flight (or the fragments of the shattered one) may have been caught, dropped or unloaded.
        if (data.hammerEntityId() != -1 && !(player.serverLevel().getEntity(data.hammerEntityId()) instanceof ThrownWeaponEntity)
                && !(player.serverLevel().getEntity(data.hammerEntityId()) instanceof MjolnirFragmentEntity)) {
            data.setHammerEntityId(-1);
        }
        if (data.toggle(Toggle.GOD_OF_THUNDER)) {
            if (starving(player)) {
                data.setToggle(Toggle.GOD_OF_THUNDER, false);
                player.displayClientMessage(Component.translatable("message.enhanced.god_of_thunder.off"), true);
            } else {
                player.causeFoodExhaustion(EnhancedConfig.GOD_OF_THUNDER_EXHAUSTION.get().floatValue());
            }
        }
        if (data.is(Live.CALLING_LIGHTNING)) {
            beamTick(player, data);
        }
    }

    /** With an empty hunger bar there is nothing left to fuel the thunder (not in Creative). */
    private static boolean starving(Player player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    /** Thor flies by spinning his weapon: only while he holds it (or it is on its way back to him). */
    public static boolean canFly(ServerPlayer player, PowerData data) {
        return holdsWeapon(player) || data.hammerEntityId() != -1;
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

    // ---- Ability 1: throw the weapon, or call it back ----

    private static void throwOrRecall(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(1, now)) {
            return;
        }
        InteractionHand hand = weaponHand(player);
        if (hand != null) {
            ItemStack weapon = player.getItemInHand(hand);
            ThrownWeaponEntity thrown = new ThrownWeaponEntity(level, player, weapon);
            player.setItemInHand(hand, ItemStack.EMPTY);
            level.addFreshEntity(thrown);
            data.setHammerEntityId(thrown.getId());
            data.startCooldown(1, now, THROW_COOLDOWN);
            player.swing(hand, true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MJOLNIR_THROW.get(), SoundSource.PLAYERS, 1.2F, isStormbreaker(weapon) ? 0.85F : 1.0F);
            return;
        }
        if (recall(player, data)) {
            data.startCooldown(1, now, THROW_COOLDOWN);
        } else {
            player.displayClientMessage(Component.translatable("message.enhanced.thor.weapon_out_of_reach"), true);
        }
    }

    /**
     * Calls his weapon home, from wherever the nearest one is: the rest of his inventory, the air, the ground (dropped,
     * or Mjolnir resting as a block), or the hands of a creature that picked it up. Returns false if there is no Mjolnir
     * or Stormbreaker within reach. (Never from another player's hands: theirs is theirs. And never another player's
     * Mjolnir, wherever it lies, even if he is in Creative: only Stormbreaker, his own Mjolnir, or one nobody owns yet,
     * which he claims when it reaches him.)
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
        if (level.getEntity(data.hammerEntityId()) instanceof ThrownWeaponEntity flying) {
            flying.setReturning(true);
            return true;
        }
        // In pieces: every fragment turns home at once.
        if (MjolnirShatter.callBack(player)) {
            return true;
        }
        double range = EnhancedConfig.MJOLNIR_RECALL_RANGE.get();
        Vec3 eye = player.getEyePosition();
        AABB around = player.getBoundingBox().inflate(range);
        // Lying on the ground as an item.
        ItemEntity nearestItem = level.getEntitiesOfClass(ItemEntity.class, around,
                        e -> canCall(player, e.getItem()) && e.distanceToSqr(player) <= range * range).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
        // Resting as a block.
        BlockPos nearestBlock = findBlock(level, player, range);
        // In a creature's hands.
        LivingEntity nearestHolder = level.getEntitiesOfClass(LivingEntity.class, around,
                        e -> !(e instanceof Player) && e.isAlive() && callableHand(player, e) != null
                                && e.distanceToSqr(player) <= range * range).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
        // Lying on someone it pins down (never the one it pins: they cannot call it off themselves).
        MjolnirPinEntity nearestPin = level.getEntitiesOfClass(MjolnirPinEntity.class, around,
                        p -> !p.targetUuid().equals(player.getUUID()) && canCall(player, p.hammer()) && p.distanceToSqr(player) <= range * range).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
        double toItem = nearestItem == null ? Double.MAX_VALUE : nearestItem.distanceToSqr(eye);
        double toBlock = nearestBlock == null ? Double.MAX_VALUE : nearestBlock.getCenter().distanceToSqr(eye);
        double toPin = nearestPin == null ? Double.MAX_VALUE : nearestPin.distanceToSqr(eye);
        double toHolder = nearestHolder == null ? Double.MAX_VALUE : nearestHolder.distanceToSqr(eye);
        double nearest = Math.min(Math.min(toItem, toBlock), Math.min(toPin, toHolder));
        Vec3 from;
        ItemStack weapon;
        if (nearestItem != null && toItem == nearest) {
            from = nearestItem.position().add(0, 0.25, 0);
            weapon = nearestItem.getItem().split(1);
            if (nearestItem.getItem().isEmpty()) {
                nearestItem.discard();
            }
        } else if (nearestBlock != null && toBlock == nearest) {
            from = nearestBlock.getCenter();
            weapon = MjolnirBlock.lift(level, nearestBlock);
        } else if (nearestPin != null && toPin == nearest) {
            from = nearestPin.position();
            weapon = MjolnirPin.takeHammer(level, nearestPin);
        } else if (nearestHolder != null) {
            InteractionHand hand = callableHand(player, nearestHolder);
            from = nearestHolder.getEyePosition().subtract(0, 0.4, 0);
            weapon = nearestHolder.getItemInHand(hand).split(1);
            nearestHolder.setItemInHand(hand, ItemStack.EMPTY);
        } else {
            return false;
        }
        ThrownWeaponEntity returning = ThrownWeaponEntity.recalled(level, player, weapon, from);
        level.addFreshEntity(returning);
        data.setHammerEntityId(returning.getId());
        level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    private static int findInInventory(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (canCall(player, inventory.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    /** Whether Thor can call this weapon: Stormbreaker, or a Mjolnir that is his or nobody's. */
    private static boolean canCall(Player thor, ItemStack weapon) {
        return isStormbreaker(weapon) || (isMjolnir(weapon) && Worthiness.canCall(thor, weapon));
    }

    /** The hand in which the creature holds a weapon Thor can call, or null. */
    @Nullable
    private static InteractionHand callableHand(Player thor, LivingEntity holder) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (canCall(thor, holder.getItemInHand(hand))) {
                return hand;
            }
        }
        return null;
    }

    /** The nearest Mjolnir block within range that Thor can call, among the loaded chunks around him. */
    @Nullable
    private static BlockPos findBlock(ServerLevel level, Player thor, double range) {
        BlockPos center = thor.blockPosition();
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
                    if (be instanceof MjolnirBlockEntity mjolnir && Worthiness.canCall(thor, mjolnir.hammer())) {
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

    // ---- Ability 3: the lightning beam (hold) ----

    /**
     * V down: lightning pours from his hand, or from the weapon in it, to whatever he looks at, for as long as he holds
     * it (see {@link #beamTick}); V up: it stops. {@link Live#CALLING_LIGHTNING} is what every client draws it from.
     */
    private static void lightningBeam(ServerPlayer player, PowerData data, boolean down) {
        if (!down || player.isSpectator()) {
            data.set(Live.CALLING_LIGHTNING, false);
            return;
        }
        if (data.is(Live.CALLING_LIGHTNING)) {
            return;
        }
        if (starving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thor.too_hungry"), true);
            return;
        }
        BEAMS.computeIfAbsent(player.getUUID(), id -> new Beam()).struck.clear();
        data.set(Live.CALLING_LIGHTNING, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    /**
     * While the beam is fired: every few ticks (config) it hits the first creature in the line of sight, up to its
     * range (24 blocks, 48 with Mjolnir or Stormbreaker in hand), for 2 damage (twice that with the weapon, and 1.5
     * times that again for the God of Thunder, whose every hit also arcs to the next creatures nearby). With the weapon
     * in hand the first hit on each creature has the usual effects of lightning on it (creepers charge, pigs turn into
     * zombified piglins). It burns a little hunger; where it ends, sparks fly, but nothing burns.
     */
    private static void beamTick(ServerPlayer thor, PowerData data) {
        if (thor.isSpectator() || starving(thor)) {
            data.set(Live.CALLING_LIGHTNING, false);
            return;
        }
        ServerLevel level = thor.serverLevel();
        Beam beam = BEAMS.computeIfAbsent(thor.getUUID(), id -> new Beam());
        boolean weapon = holdsWeapon(thor);
        boolean thunder = data.toggle(Toggle.GOD_OF_THUNDER);
        thor.causeFoodExhaustion(EnhancedConfig.THOR_BEAM_EXHAUSTION.get().floatValue());
        HitResult hit = Raycasts.any(thor, beamRange(weapon), beamTargets(thor), 1.0F);
        Vec3 end = hit.getLocation();
        long now = level.getGameTime();
        // A complete Chaos Shield in the way stops the beam at its surface, and pays for it with its strength.
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, thor, thor.getEyePosition(), end, 1.0F);
        if (shield != null) {
            if (now >= beam.nextHit) {
                beam.nextHit = now + EnhancedConfig.THOR_BEAM_INTERVAL.get();
                beamShielded(level, thor, shield, beamDamage(weapon, thunder));
            }
            return;
        }
        if (hit instanceof EntityHitResult entityHit && now >= beam.nextHit) {
            beam.nextHit = now + EnhancedConfig.THOR_BEAM_INTERVAL.get();
            beamHit(level, thor, beam, entityHit, weapon, thunder);
        }
        // Sparks and a crackle where it lands, on a block or a creature (not in the empty air).
        if (hit.getType() != HitResult.Type.MISS) {
            if (now % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 3, 0.12, 0.12, 0.12, 0.25);
            }
            if (now % 6 == 0) {
                level.playSound(null, end.x, end.y, end.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.25F, 1.9F);
            }
        }
    }

    /**
     * The beam meets a Scarlet Witch's Chaos Shield: it spends itself on the sphere (red sparks, the crackle of the
     * beam where it ends) and takes {@code damage} points off the shield's strength. Whoever is inside is not touched,
     * and nothing happens to creatures beyond (no lightning effects, no arcs).
     */
    private static void beamShielded(ServerLevel level, ServerPlayer thor, WandaAbilities.ShieldStop shield, float damage) {
        Vec3 at = shield.point();
        level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.2F), at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.12);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.3);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.25F, 1.9F);
        // (With PvP off the beam would not have hurt her: it costs the shield nothing either.)
        if (shield.owner() instanceof ServerPlayer wanda && thor.canHarmPlayer(wanda)) {
            WandaAbilities.wear(wanda, damage);
        }
    }

    private static void beamHit(ServerLevel level, ServerPlayer thor, Beam beam, EntityHitResult hit, boolean weapon, boolean thunder) {
        Entity target = hit.getEntity();
        LivingEntity living = target instanceof LivingEntity l ? l
                : target instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity l ? l : null;
        // (Not on players: lightning has nothing to do to them but a flinch, and PvP rules are not for it to bypass.)
        if (weapon && living != null && !(living instanceof Player) && beam.struck.add(living.getId())) {
            thunderStruck(level, thor, living);
        }
        target.hurt(ModDamageTypes.source(level, ModDamageTypes.LIGHTNING_BEAM, thor, thor), beamDamage(weapon, thunder));
        Vec3 at = hit.getLocation();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.3);
        if (thunder) {
            arc(level, thor, at, living != null ? living : target, false);
        }
    }

    /**
     * Lightning's own effect on a creature (a creeper is charged, a pig turns into a zombified piglin, a villager into
     * a witch...), without its damage or its fire.
     */
    private static void thunderStruck(ServerLevel level, ServerPlayer thor, LivingEntity target) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(target.getX(), target.getY(), target.getZ());
        bolt.setVisualOnly(true);
        bolt.setCause(thor);
        bolt.setDamage(0.0F);
        int fire = target.getRemainingFireTicks();
        target.thunderHit(level, bolt);
        target.setRemainingFireTicks(fire);
    }

    /**
     * Lets the next hit of Thor's beam land at once, whenever the last was (it is forgotten when he loses the power or
     * logs out; a test that fires the beam several times within one tick calls it in between).
     */
    public static void forgetBeamPace(ServerPlayer thor) {
        BEAMS.remove(thor.getUUID());
    }

    /**
     * Damage of one hit of the beam, for whether Thor holds Mjolnir or Stormbreaker and whether the God of Thunder is
     * unleashed.
     */
    public static float beamDamage(boolean weapon, boolean thunder) {
        double damage = EnhancedConfig.THOR_BEAM_DAMAGE.get();
        if (weapon) {
            damage *= EnhancedConfig.THOR_BEAM_WEAPON_MULTIPLIER.get();
        }
        if (thunder) {
            damage *= EnhancedConfig.THOR_BEAM_THUNDER_MULTIPLIER.get();
        }
        return (float) damage;
    }

    /** How far the beam reaches (blocks); on a client before the server's config has arrived, the defaults. */
    public static double beamRange(boolean weapon) {
        if (!EnhancedConfig.SPEC.isLoaded()) {
            return weapon ? 48 : 24;
        }
        return weapon ? EnhancedConfig.THOR_BEAM_WEAPON_RANGE.get() : EnhancedConfig.THOR_BEAM_RANGE.get();
    }

    /** The first solid ground at or below a point (up to 32 blocks down). */
    private static Vec3 ground(ServerLevel level, Vec3 point) {
        BlockHitResult down = level.clip(new ClipContext(point, point.subtract(0, 32, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        return down.getType() == HitResult.Type.MISS ? point : down.getLocation();
    }

    /**
     * The God of Thunder's lightning leaps from a hit to the target and on to the next creatures nearby, up to two
     * jumps. Seen by everyone around.
     */
    public static void arc(ServerLevel level, ServerPlayer thor, Vec3 from, Entity first) {
        arc(level, thor, from, first, true);
    }

    /**
     * {@link #arc(ServerLevel, ServerPlayer, Vec3, Entity)}, where the first creature is the one that was struck
     * already (the beam's) and is only the start of the arc if {@code hurtFirst} is false.
     */
    private static void arc(ServerLevel level, ServerPlayer thor, Vec3 from, Entity first, boolean hurtFirst) {
        List<Vec3> points = new ArrayList<>();
        points.add(from);
        List<Entity> struck = new ArrayList<>();
        Entity current = first;
        for (int jump = 0; current != null && jump <= ARC_JUMPS; jump++) {
            struck.add(current);
            points.add(current.position().add(0, current.getBbHeight() * 0.6, 0));
            if (jump > 0 || hurtFirst) {
                current.hurt(ModDamageTypes.source(level, DamageTypes.LIGHTNING_BOLT, thor, thor), ARC_DAMAGE);
            }
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

    // ---- the Bifrost: Thor's ability 5, and anyone's who holds Mjolnir or Stormbreaker ----

    /**
     * Using Mjolnir or Stormbreaker (right-click): the Bifrost screen opens on the client (home, the saved places, save
     * this one); with Sneak, the Bifrost opens to where the player looks.
     */
    public static InteractionResultHolder<ItemStack> useWeapon(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer server) {
                bifrostToLook(server);
            }
        } else if (level.isClientSide) {
            // Client only: this branch never runs on a dedicated server, so the screen class is never loaded there.
            BifrostScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * "Heimdall! Open the Bifrost!": a beam of rainbow light gathers around the traveller and, a moment later, takes
     * them home to their respawn point (bed or respawn anchor, otherwise the world spawn), even in another dimension.
     * The Bifrost burns its circle of runes into the ground where it lands and where it left.
     */
    public static void bifrostHome(ServerPlayer player) {
        if (mayOpenBifrost(player)) {
            DimensionTransition home = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
            openBifrost(player, home.newLevel().dimension(), home.pos(), player.getYRot());
        }
    }

    /** The Bifrost to the spot the player looks at, up to 512 blocks away. */
    public static void bifrostToLook(ServerPlayer player) {
        if (!mayOpenBifrost(player)) {
            return;
        }
        BlockHitResult hit = Raycasts.block(player, BIFROST_LOOK_RANGE, 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_ground"), true);
            return;
        }
        openBifrost(player, player.level().dimension(), Vec3.atBottomCenterOf(hit.getBlockPos().above()), player.getYRot());
    }

    /**
     * The Bifrost to the saved place at {@code index}, in whatever dimension it is, if it is still the one called
     * {@code name} (the list may have changed since the screen showed it).
     */
    public static void bifrostToPlace(ServerPlayer player, int index, String name) {
        BifrostData.Place place = placeAt(player, index, name);
        if (place == null || !mayOpenBifrost(player)) {
            return;
        }
        if (player.server.getLevel(place.dimension()) == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_world"), true);
            return;
        }
        openBifrost(player, place.dimension(), place.pos(), place.yaw());
    }

    /**
     * Marks where the player stands, facing the way they face, as a place the Bifrost can take them back to. Saving a
     * name again moves that place; the Bifrost's runes flare at their feet.
     */
    public static void savePlace(ServerPlayer player, String typed) {
        if (player.isSpectator()) {
            return;
        }
        if (!holdsWeapon(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.needs_weapon"), true);
            return;
        }
        BifrostData bifrost = player.getData(ModAttachments.BIFROST);
        BlockPos at = player.blockPosition();
        String name = BifrostData.cleanName(typed, at.getX() + ", " + at.getY() + ", " + at.getZ());
        BifrostData updated = bifrost.with(new BifrostData.Place(name, player.level().dimension(), player.position(), player.getYRot()));
        if (updated == bifrost) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.full", BifrostData.MAX_PLACES), true);
            return;
        }
        player.setData(ModAttachments.BIFROST, updated);
        ServerLevel level = player.serverLevel();
        mark(level, player.position());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.2F);
        player.displayClientMessage(Component.translatable("message.enhanced.bifrost.saved", name), true);
    }

    /** Forgets the saved place at {@code index}, if it is still the one called {@code name}. */
    public static void forgetPlace(ServerPlayer player, int index, String name) {
        BifrostData.Place place = placeAt(player, index, name);
        if (place != null) {
            player.setData(ModAttachments.BIFROST, player.getData(ModAttachments.BIFROST).without(index));
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.forgotten", place.name()), true);
        }
    }

    @Nullable
    private static BifrostData.Place placeAt(ServerPlayer player, int index, String name) {
        List<BifrostData.Place> places = player.getData(ModAttachments.BIFROST).places();
        return index >= 0 && index < places.size() && places.get(index).name().equals(name) ? places.get(index) : null;
    }

    /** The Bifrost answers whoever holds Mjolnir or Stormbreaker, once it has gathered its strength again. */
    private static boolean mayOpenBifrost(ServerPlayer player) {
        if (player.isSpectator() || JOURNEYS.containsKey(player.getUUID())) {
            return false;
        }
        if (MjolnirPin.isPinned(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.mjolnir.pinned_cannot"), true);
            return false;
        }
        if (!holdsWeapon(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.needs_weapon"), true);
            return false;
        }
        long wait = player.getData(ModAttachments.BIFROST).cooldown(player.level().getGameTime());
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.not_ready", (wait + 19) / 20), true);
            return false;
        }
        return true;
    }

    private static void openBifrost(ServerPlayer player, ResourceKey<Level> dimension, Vec3 to, float yaw) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        int cooldown = EnhancedConfig.BIFROST_COOLDOWN.get();
        player.setData(ModAttachments.BIFROST, player.getData(ModAttachments.BIFROST).readyAt(now + cooldown));
        PowerData data = Powers.get(player);
        if (data.power() == Power.THOR) {
            // The same cooldown, shown on Thor's HUD.
            data.startCooldown(5, now, cooldown);
        }
        JOURNEYS.put(player.getUUID(), new Journey(dimension, to, yaw, now + BIFROST_CHARGE));
        data.set(Live.BIFROST, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BIFROST_CHARGE.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
    }

    private static void bifrostTick(ServerPlayer player, PowerData data) {
        Journey journey = JOURNEYS.get(player.getUUID());
        if (journey == null) {
            data.set(Live.BIFROST, false);
            return;
        }
        long now = player.level().getGameTime();
        if (now < journey.arrivesAt()) {
            // Held in the beam: rising slightly, unable to walk away.
            player.setDeltaMovement(0, 0.02, 0);
            player.hurtMarked = true;
            player.resetFallDistance();
            return;
        }
        JOURNEYS.remove(player.getUUID());
        data.set(Live.BIFROST, false);
        ServerLevel destination = player.server.getLevel(journey.dimension());
        if (destination == null || now > journey.arrivesAt() + 5) {
            // The world is gone, or the traveller wasn't there to be taken (dead, or away): the light went out.
            return;
        }
        Vec3 to = safeArrival(destination, player, journey.to());
        if (!HexManager.onTeleport(destination, player, to)) {
            return;
        }
        ServerLevel from = player.serverLevel();
        mark(from, player.position());
        player.teleportTo(destination, to.x, to.y, to.z, journey.yaw(), player.getXRot());
        player.resetFallDistance();
        destination.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BIFROST_ARRIVE.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        PacketDistributor.sendToPlayersNear(destination, null, player.getX(), player.getY(), player.getZ(), 128,
                new ThorFxPayload(ThorFxPayload.BIFROST_ARRIVAL, player.getId(), List.of(player.position())));
        mark(destination, player.position());
    }

    /** {@code to}, or the first spot above it where the traveller fits (a saved place may have been built over since). */
    private static Vec3 safeArrival(ServerLevel level, ServerPlayer player, Vec3 to) {
        // Collision checks treat unloaded chunks as empty air: load the landing chunk first.
        level.getChunkAt(BlockPos.containing(to));
        AABB body = player.getDimensions(Pose.STANDING).makeBoundingBox(to);
        for (int up = 0; up <= ARRIVAL_SEARCH && to.y + up < level.getMaxBuildHeight(); up++) {
            if (level.noCollision(player, body.move(0, up, 0))) {
                return to.add(0, up, 0);
            }
        }
        return to;
    }

    /** The Bifrost's circle of runes, burned into the ground (seen by everyone nearby, fading after a minute). */
    private static void mark(ServerLevel level, Vec3 at) {
        Vec3 ground = ground(level, at.add(0, 0.5, 0));
        PacketDistributor.sendToPlayersNear(level, null, ground.x, ground.y, ground.z, 128,
                new ThorFxPayload(ThorFxPayload.BIFROST_MARK, -1, List.of(ground)));
    }

    public static boolean inBifrost(Player player) {
        return JOURNEYS.containsKey(player.getUUID());
    }

    // ---- cleanup ----

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
        forgetBeamPace(player);
        data.set(Live.CALLING_LIGHTNING, false);
        data.setToggle(Toggle.GOD_OF_THUNDER, false);
        // A shattered Mjolnir comes together where he stands and falls. A weapon in flight finds no Thor to return to,
        // and drops by itself. A Bifrost already gathering still takes its traveller: it answers the weapon, not the
        // power.
        MjolnirShatter.onGone(player);
    }

    /** Thor logs out: his beam stops (a Mjolnir in pieces falls: see {@link MjolnirShatter#onGone}). */
    public static void onLogout(ServerPlayer player) {
        forgetBeamPace(player);
        Powers.get(player).set(Live.CALLING_LIGHTNING, false);
    }

    public static void clear() {
        JOURNEYS.clear();
        BEAMS.clear();
        MjolnirShatter.clear();
        MjolnirPin.clear();
    }

    private ThorAbilities() {
    }
}

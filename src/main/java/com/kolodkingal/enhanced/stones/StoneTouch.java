package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.item.InfinityStone;
import com.kolodkingal.enhanced.item.InfinityStoneItem;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What a bare Infinity Stone does to one who may not hold it (3.6, the plan's 1.5; who may is {@link StoneRules}):
 *
 * <ol> <li><b>On the ground</b> it slips away from his hand: he cannot pick it up; once a second it hops a little away
 * with a spark of its colour and the action bar says so. Hoppers and allays still take it.</li> <li><b>In his
 * inventory</b> (from a chest, the crafting output, a friend's hand, a command, or a Bearer who lost his power) a
 * <b>grace</b> of {@code [stones] graceSeconds} begins, {@code handGraceFactor} times faster while it is in a hand; the
 * HUD counts it down. At its end the stone <b>rejects</b> him: a small zap ({@code rejectDamage}, {@code stone_reject},
 * never below one heart of health), a little knockback, its voice low, a burst of its colour, and it jumps out of his
 * inventory 1.5 blocks in front of him.</li> <li><b>Containers are safe</b>: a chest, a shulker box, the Infinity
 * Gauntlet, the Orb. A bundle in the inventory is not: the grace looks into bundles.</li> </ol>
 *
 * The grace is kept per player and per stone in a map that is not saved (a relog starts it again); moving the stone
 * from slot to slot does not. Lost stones (killed or fallen out of the world) go back to the ledger ({@link
 * StoneLedger}).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class StoneTouch {
    /** Ticks between two slips of the same player (the hop, the spark, the line). */
    private static final int SLIP_EVERY = 20;
    /** How far in front of him a rejected stone lands, and how long before anyone may pick it up. */
    private static final double THROW_DISTANCE = 1.5;
    private static final int THROW_PICKUP_DELAY = 40;
    private static final double KNOCKBACK = 0.4;

    /** Each player's grace per stone (in grace units: one per tick, {@code handGraceFactor} in a hand). */
    private static final Map<UUID, Map<StoneKind, Integer>> GRACE = new HashMap<>();
    /** When each player last saw a stone slip away (game time). */
    private static final Map<UUID, Long> LAST_SLIP = new HashMap<>();

    /** Where a stone is carried that is not safe: a slot of the inventory, held in a hand or not, maybe in a bundle. */
    public record Carried(StoneKind kind, int slot, boolean inHand, boolean inBundle) {
    }

    // ---- the rule, on both sides (the HUD counts the same grace) ----

    /** How many grace units a stone may gather before it rejects its carrier. */
    public static int graceUnits() {
        int seconds = EnhancedConfig.SPEC.isLoaded() ? StonesConfig.GRACE_SECONDS.get() : 10;
        return seconds * 20;
    }

    /** The grace units a tick adds: one, or {@code handGraceFactor} for a stone in a hand. */
    public static int step(boolean inHand) {
        return inHand ? (EnhancedConfig.SPEC.isLoaded() ? StonesConfig.HAND_GRACE_FACTOR.get() : 3) : 1;
    }

    /**
     * The stones the player carries bare (in the inventory, a hand or a bundle in the inventory) that he may not hold,
     * one entry per kind (the one most in a hand first). Creative players and spectators carry none.
     */
    public static List<Carried> unsafe(Player player) {
        List<Carried> out = new ArrayList<>();
        if (player.isSpectator() || player.isCreative()) {
            return out;
        }
        Inventory inventory = player.getInventory();
        int size = inventory.getContainerSize();
        Map<StoneKind, Carried> best = new EnumMap<>(StoneKind.class);
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            boolean inHand = slot == inventory.selected || slot == Inventory.SLOT_OFFHAND;
            if (stack.getItem() instanceof InfinityStoneItem stone) {
                consider(player, best, new Carried(stone.kind, slot, inHand, false));
            }
            BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundle != null) {
                for (ItemStack inside : bundle.items()) {
                    if (inside.getItem() instanceof InfinityStoneItem stone) {
                        consider(player, best, new Carried(stone.kind, slot, false, true));
                    }
                }
            }
        }
        out.addAll(best.values());
        return out;
    }

    private static void consider(Player player, Map<StoneKind, Carried> best, Carried carried) {
        if (StoneRules.mayHold(player, carried.kind())) {
            return;
        }
        Carried before = best.get(carried.kind());
        if (before == null || carried.inHand() && !before.inHand()) {
            best.put(carried.kind(), carried);
        }
    }

    /**
     * Advances a player's grace by one tick in {@code grace} (kinds no longer carried are forgotten).
     *
     * @return the stones whose grace has run out this tick
     */
    public static List<Carried> advance(Player player, Map<StoneKind, Integer> grace) {
        List<Carried> carried = unsafe(player);
        List<Carried> due = new ArrayList<>();
        grace.keySet().removeIf(kind -> carried.stream().noneMatch(c -> c.kind() == kind));
        int limit = graceUnits();
        for (Carried c : carried) {
            int units = grace.merge(c.kind(), step(c.inHand()), Integer::sum);
            if (units >= limit) {
                due.add(c);
            }
        }
        return due;
    }

    /** Whole seconds left of a grace of {@code units} (rounded up; at least 0). */
    public static int secondsLeft(int units) {
        return Math.max(0, (graceUnits() - units + 19) / 20);
    }

    // ---- the server ----

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    /** Every tick of every player on the server: his grace, and the rejection when it runs out. */
    public static void tick(ServerPlayer player) {
        if (!player.isAlive()) {
            GRACE.remove(player.getUUID());
            return;
        }
        Map<StoneKind, Integer> grace = GRACE.computeIfAbsent(player.getUUID(), k -> new EnumMap<>(StoneKind.class));
        Map<StoneKind, Integer> before = new EnumMap<>(grace);
        List<Carried> due = advance(player, grace);
        for (Map.Entry<StoneKind, Integer> entry : grace.entrySet()) {
            int was = secondsLeft(before.getOrDefault(entry.getKey(), 0));
            int now = secondsLeft(entry.getValue());
            if (now != was && now > 0) {
                player.playNotifySound(ModSounds.STONE_GRACE_TICK.get(), SoundSource.PLAYERS, 0.6F,
                        0.9F + 0.05F * (10 - Math.min(10, now)));
            }
        }
        for (Carried c : due) {
            reject(player, c);
            grace.remove(c.kind());
        }
        if (grace.isEmpty()) {
            GRACE.remove(player.getUUID());
        }
    }

    /** A player's grace of a stone now (grace units; 0 when none): the tests read it. */
    public static int grace(ServerPlayer player, StoneKind kind) {
        Map<StoneKind, Integer> grace = GRACE.get(player.getUUID());
        return grace == null ? 0 : grace.getOrDefault(kind, 0);
    }

    /**
     * The stone rejects the one who may not hold it: the zap (never below one heart), the knockback, its voice low, a
     * burst of its colour, and it jumps out of his inventory (or his bundle) to land in front of him.
     */
    static void reject(ServerPlayer player, Carried carried) {
        ItemStack stone = takeOut(player, carried);
        if (stone.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        StoneKind kind = carried.kind();
        float damage = EnhancedConfig.SPEC.isLoaded() ? StonesConfig.REJECT_DAMAGE.get().floatValue() : 2.0F;
        float amount = Math.min(damage, player.getHealth() - 1.0F);
        if (amount > 0.0F) {
            player.hurt(ModDamageTypes.source(level, ModDamageTypes.STONE_REJECT), amount);
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        player.knockback(KNOCKBACK, flat.x, flat.z);
        player.hurtMarked = true;
        Vec3 hand = player.position().add(0.0, player.getBbHeight() * 0.6, 0.0);
        level.playSound(null, hand.x, hand.y, hand.z, ModSounds.STONE_REJECT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        kind.voice().use(level, hand, InfinityStone.UNDER, 0.6F);
        level.sendParticles(new EnergyParticleOptions(kind.color(), 1.2F), hand.x, hand.y, hand.z, 24, 0.3, 0.3,
                0.3, 0.12);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new StoneFxPayload(StoneFxPayload.REJECT, player.getId(), List.of(hand), kind.ordinal()));
        player.displayClientMessage(Component.translatable("message.enhanced.stones.too_strong", kind.displayName())
                .withStyle(ChatFormatting.RED), true);
        throwOut(player, stone, flat);
    }

    /** Takes the rejected stone out of its slot (or out of the bundle in that slot). */
    private static ItemStack takeOut(ServerPlayer player, Carried carried) {
        Inventory inventory = player.getInventory();
        ItemStack holder = inventory.getItem(carried.slot());
        if (!carried.inBundle()) {
            if (holder.getItem() instanceof InfinityStoneItem stone && stone.kind == carried.kind()) {
                inventory.setItem(carried.slot(), ItemStack.EMPTY);
                return holder;
            }
            return ItemStack.EMPTY;
        }
        BundleContents bundle = holder.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle == null) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> kept = new ArrayList<>();
        ItemStack out = ItemStack.EMPTY;
        for (ItemStack inside : bundle.items()) {
            if (out.isEmpty() && inside.getItem() instanceof InfinityStoneItem stone && stone.kind == carried.kind()) {
                out = inside.copy();
            } else {
                kept.add(inside.copy());
            }
        }
        holder.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(kept));
        return out;
    }

    /** The stone lands 1.5 blocks in front of him (at his feet if a wall is there), not to be picked up for a while. */
    private static void throwOut(ServerPlayer player, ItemStack stone, Vec3 flat) {
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(flat.scale(THROW_DISTANCE)).add(0.0, 0.6, 0.0);
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stone);
        if (!level.noCollision(item)) {
            item.setPos(player.getX(), player.getY() + 0.6, player.getZ());
        }
        item.setDeltaMovement(flat.x * 0.15, 0.25, flat.z * 0.15);
        item.setPickUpDelay(THROW_PICKUP_DELAY);
        item.setThrower(player);
        level.addFreshEntity(item);
    }

    /** A stone on the ground slips away from the hand of one who may not hold it. */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity item = event.getItemEntity();
        if (!(item.getItem().getItem() instanceof InfinityStoneItem stone)) {
            return;
        }
        Player player = event.getPlayer();
        if (StoneRules.mayHold(player, stone.kind)) {
            return;
        }
        event.setCanPickup(TriState.FALSE);
        if (player instanceof ServerPlayer server) {
            slip(server, item, stone.kind);
        }
    }

    /** The slip, once a second per player: the hop away, a spark, the line, the sound. */
    static void slip(ServerPlayer player, ItemEntity item, StoneKind kind) {
        long now = player.level().getGameTime();
        Long last = LAST_SLIP.get(player.getUUID());
        if (last != null && now - last < SLIP_EVERY && now >= last) {
            return;
        }
        LAST_SLIP.put(player.getUUID(), now);
        Vec3 away = item.position().subtract(player.position());
        away = new Vec3(away.x, 0.0, away.z);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
        item.setDeltaMovement(away.x * 0.3, 0.25, away.z * 0.3);
        item.hurtMarked = true;
        ServerLevel level = player.serverLevel();
        level.sendParticles(new EnergyParticleOptions(kind.color(), 0.9F), item.getX(), item.getY() + 0.25,
                item.getZ(), 10, 0.15, 0.15, 0.15, 0.08);
        level.playSound(null, item.getX(), item.getY(), item.getZ(), ModSounds.STONE_SLIP.get(), SoundSource.PLAYERS,
                0.8F, 1.0F);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new StoneFxPayload(StoneFxPayload.SLIP, item.getId(), List.of(item.position()), kind.ordinal()));
        player.displayClientMessage(Component.translatable("message.enhanced.stones.slips", kind.displayName())
                .withStyle(ChatFormatting.GOLD), true);
    }

    /** A stone killed (a command) or fallen out of the world is lost: the ledger returns it to its sites. */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)) {
            return;
        }
        if (lost(item)) {
            StoneLedger.destroyed(item.getItem());
        }
    }

    /** Whether an item entity that leaves its level is lost for good (killed, or gone below the world). */
    static boolean lost(ItemEntity item) {
        Entity.RemovalReason reason = item.getRemovalReason();
        if (reason == Entity.RemovalReason.KILLED) {
            return true;
        }
        return reason == Entity.RemovalReason.DISCARDED && item.getY() < item.level().getMinBuildHeight() - 64;
    }

    /** A player who comes back gets the stones lost while he was away (player mode: his bits cleared). */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoneLedger.progress(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GRACE.remove(event.getEntity().getUUID());
        LAST_SLIP.remove(event.getEntity().getUUID());
    }

    /** Forgets a player's grace (a test's clean start). */
    public static void forget(@Nullable UUID player) {
        if (player == null) {
            GRACE.clear();
            LAST_SLIP.clear();
        } else {
            GRACE.remove(player);
            LAST_SLIP.remove(player);
        }
    }

    private StoneTouch() {
    }
}

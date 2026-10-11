package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.network.MindReadPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.power.LokiForm;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Phoenix reading minds: what she finds in whoever she looks at (the server finds the target and builds the
 * snapshot; the client shows it in a panel beside her crosshair), and the count of minds a Mind Sense ping finds.
 *
 * <p>Reading changes nothing about its target. It needs a line of sight (Mind Sense is the one through walls), works
 * through the Hex wall, on a pinned target and in Quicksilver Time, and with PvP off. Creative and Spectator players,
 * armour stands and Jean herself are not minds; a Loki's duplicate is an illusion; Magneto's helmet shields the mind of
 * whoever wears it; the bosses' minds are too vast to read.
 *
 * <p>Every stack in a snapshot is a stripped copy (see {@link #strip}) and the whole payload stays under {@link
 * #MAX_BYTES}: a clientbound payload over 1 MiB would disconnect the reader, and a pack full of full shulker boxes is
 * over that.
 */
public final class MindReading {
    /** How often the server looks again at what the Phoenix looks at (ticks). */
    public static final int PERIOD = 5;
    /**
     * The snapshot is sent again at least this often (twice a second): the client's panel gives up 15 ticks after the
     * last one, so it stays up while she looks and goes soon after she looks away.
     */
    public static final int RESEND_TICKS = 10;
    /** A player who is read is told so at most once in this long (10 s). */
    public static final int NOTICE_TICKS = 200;
    /** The most a snapshot may weigh once encoded (64 KiB). */
    public static final int MAX_BYTES = 64 * 1024;
    /** How many stacks a creature's own inventory shows (a villager's has 8). */
    private static final int CREATURE_STACKS = 8;
    /** How much of a custom name survives (the panel shows names, not hover events). */
    private static final int NAME_LENGTH = 64;
    /** A mob hurt this recently (5 s) that is not aggressive is afraid. */
    private static final int AFRAID_TICKS = 100;
    /** The six slots of what a creature wears, in the order of a snapshot's {@code worn} list. */
    private static final EquipmentSlot[] WORN = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY};

    /** What one Phoenix is looking at, and what the client was last told of it. */
    private static final class Reading {
        int target = -1;
        long sentAt = Long.MIN_VALUE;
        @Nullable
        MindReadPayload sent;
    }

    private static final Map<UUID, Reading> READINGS = new HashMap<>();
    /** Whom the Phoenix told "you feel a presence", and when (game time). */
    private static final Map<UUID, Long> NOTICES = new HashMap<>();

    /** The minds a Mind Sense ping found: all of them, and how many were players, hostile, or silent. */
    public record Sensed(List<LivingEntity> minds, int players, int hostile, int silences) {
    }

    // ------------------------------------------------------------------------------------------------------------
    // Finding and reading a mind

    /** What Mind Reading can look at: a living creature that is not Jean, a spectator, an armour stand or a Creative player. */
    public static boolean lookable(ServerPlayer reader, Entity e) {
        return e instanceof LivingEntity living && living != reader && living.isAlive() && !living.isSpectator()
                && !(living instanceof ArmorStand) && !(living instanceof Player player && player.isCreative());
    }

    /** The first creature the Phoenix looks at, within her reading range and not behind a block. */
    @Nullable
    public static LivingEntity target(ServerPlayer reader) {
        EntityHitResult hit = Raycasts.entity(reader, PhoenixConfig.MIND_READ_RANGE.get(), 0.3, e -> lookable(reader, e), 1.0F);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /**
     * While Mind Reading is on: every {@link #PERIOD} ticks the Phoenix looks again at what she is looking at.
     */
    public static void tick(ServerPlayer reader, PowerData data) {
        long now = reader.level().getGameTime();
        if (now % PERIOD == 0) {
            look(reader, now);
        }
    }

    /**
     * One look: finds her target and, when it changed or its snapshot did (or soon enough anyway), tells her client.
     * Also the motes that say "she is looking into him". {@code now} is the game time (a test passes its own).
     */
    public static void look(ServerPlayer reader, long now) {
        Reading reading = READINGS.computeIfAbsent(reader.getUUID(), id -> new Reading());
        LivingEntity target = target(reader);
        int id = target == null ? -1 : target.getId();
        boolean fresh = id != reading.target;
        if (fresh && target != null) {
            // A new mind: the listeners hear of it before anything refuses it (a boss may feel the glance).
            PhoenixPower.touched(reader, target, PhoenixPower.Kind.MIND_READ, 0.0F);
            reader.playNotifySound(ModSounds.MIND_READ.get(), SoundSource.PLAYERS, 0.25F, 1.2F);
        }
        reading.target = id;
        if (target == null) {
            reading.sent = null;
            return;
        }
        MindReadPayload snapshot = snapshot(reader, target);
        if (fresh || reading.sent == null || now - reading.sentAt >= RESEND_TICKS || !sameContent(reading.sent, snapshot)) {
            PacketDistributor.sendToPlayer(reader, snapshot);
            reading.sent = snapshot;
            reading.sentAt = now;
        }
        if (snapshot.state() == MindReadPayload.OK && target instanceof ServerPlayer read) {
            notice(read, now);
        }
        if (now % (PERIOD * 2) == 0 && reader.level() instanceof ServerLevel level) {
            motes(level, reader, target);
        }
    }

    /** Forgets what the Phoenix was looking at (Mind Reading was switched off, or she is gone). */
    public static void stop(ServerPlayer reader) {
        READINGS.remove(reader.getUUID());
    }

    /** Forget everything (the server stopped). */
    public static void clear() {
        READINGS.clear();
        NOTICES.clear();
    }

    /** "You feel a presence in your mind", once in a while, if the server says a read player is told. */
    private static void notice(ServerPlayer read, long now) {
        if (!PhoenixConfig.MIND_READ_NOTIFIES.get()) {
            return;
        }
        Long last = NOTICES.get(read.getUUID());
        if (last == null || now - last >= NOTICE_TICKS) {
            NOTICES.put(read.getUUID(), now);
            read.displayClientMessage(Component.translatable("message.enhanced.mind_read.felt"), true);
        }
    }

    /** Six violet motes along the line from her forehead to the target's head, and a few at the target's head. */
    private static void motes(ServerLevel level, ServerPlayer reader, LivingEntity target) {
        Vec3 from = reader.getEyePosition();
        Vec3 to = target.getEyePosition();
        EnergyParticleOptions mote = new EnergyParticleOptions(PhoenixAbilities.PSY_MAIN, 0.7F);
        for (int i = 1; i <= 6; i++) {
            Vec3 at = from.lerp(to, i / 7.0);
            level.sendParticles(mote, at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        level.sendParticles(new EnergyParticleOptions(PhoenixAbilities.PSY_CORE, 0.8F), to.x, to.y + 0.3, to.z, 3, 0.2, 0.1, 0.2, 0.01);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The snapshot

    /** Why a mind cannot be read, or {@link MindReadPayload#OK}. */
    public static int stateOf(LivingEntity target) {
        if (target instanceof LokiIllusionEntity) {
            return MindReadPayload.ILLUSION;
        }
        if (target.getType().is(ModTags.TELEPATHY_IMMUNE)) {
            return MindReadPayload.TOO_VAST;
        }
        if (target instanceof Player && !PhoenixConfig.MIND_READ_PLAYERS.get()) {
            return MindReadPayload.PLAYERS_OFF;
        }
        if (PhoenixAbilities.isShielded(target)) {
            return MindReadPayload.BLOCKED;
        }
        return MindReadPayload.OK;
    }

    /**
     * What her Mind Reading finds in {@code target}: its name and kind always; and, if its mind can be read, how it
     * feels, how well it is, what it wears and what it carries (a player's whole pack unless the server keeps it
     * private). Pure: it changes nothing and sends nothing.
     */
    public static MindReadPayload snapshot(ServerPlayer reader, LivingEntity target) {
        int state = stateOf(target);
        String name = target instanceof Player || target.hasCustomName() ? target.getName().getString() : "";
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        if (state != MindReadPayload.OK) {
            return new MindReadPayload(target.getId(), name, species, state, MindReadPayload.CALM, "", 0.0F, 0.0F, 0, 0,
                    List.of(), List.of(), "");
        }
        int mood = MindReadPayload.CALM;
        String hostileTo = "";
        if (target instanceof Mob mob) {
            LivingEntity goal = mob.getTarget();
            if (goal != null && goal.isAlive()) {
                mood = MindReadPayload.HOSTILE;
                hostileTo = goal.getName().getString();
            } else if (mob.isAggressive()) {
                mood = MindReadPayload.ALERT;
            } else if (mob.getLastHurtByMob() != null && mob.tickCount - mob.getLastHurtByMobTimestamp() < AFRAID_TICKS) {
                mood = MindReadPayload.AFRAID;
            }
        }
        List<ItemStack> worn = new ArrayList<>(WORN.length);
        for (EquipmentSlot slot : WORN) {
            worn.add(strip(target.getItemBySlot(slot)));
        }
        int food = 0;
        int level = 0;
        List<ItemStack> carried = new ArrayList<>();
        if (target instanceof Player player) {
            food = player.getFoodData().getFoodLevel();
            level = player.experienceLevel;
            if (PhoenixConfig.MIND_READ_INVENTORY.get()) {
                for (ItemStack stack : player.getInventory().items) {
                    carried.add(strip(stack));
                }
            }
        } else if (target instanceof InventoryCarrier carrier) {
            for (int i = 0; i < carrier.getInventory().getContainerSize() && carried.size() < CREATURE_STACKS; i++) {
                ItemStack stack = carrier.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    carried.add(strip(stack));
                }
            }
        } else if (target instanceof EnderMan enderman) {
            BlockState block = enderman.getCarriedBlock();
            if (block != null && !block.isAir() && !block.getBlock().asItem().getDefaultInstance().isEmpty()) {
                carried.add(new ItemStack(block.getBlock().asItem()));
            }
        }
        MindReadPayload payload = new MindReadPayload(target.getId(), name, species, state, mood, hostileTo, target.getHealth(),
                target.getMaxHealth(), food, level, worn, carried, extra(target));
        return bounded(reader, payload);
    }

    /**
     * What is behind the face: {@code "shaped:<form>"} for a Loki in another shape (the form as {@link LokiForm}
     * saves it, so the client names it in its own language), {@code "disguised"} for a Vision in his human disguise,
     * else empty.
     */
    private static String extra(LivingEntity target) {
        if (target instanceof Player player) {
            PowerData data = Powers.get(player);
            if (data.power() == Power.LOKI && !data.form().isEmpty()) {
                return "shaped:" + data.form();
            }
            if (data.power() == Power.VISION && data.disguise()) {
                return "disguised";
            }
        }
        return "";
    }

    /**
     * A copy of the stack with only what the panel shows: the item, the count, the damage, the enchantments and a
     * plain custom name. No container contents, no written-book pages, no custom data, no hover events: a pack of full
     * shulker boxes would be megabytes.
     */
    public static ItemStack strip(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = new ItemStack(stack.getItem(), stack.getCount());
        if (stack.isDamaged()) {
            copy.setDamageValue(stack.getDamageValue());
        }
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            copy.set(DataComponents.ENCHANTMENTS, enchantments);
        }
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            copy.set(DataComponents.STORED_ENCHANTMENTS, stored);
        }
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            String plain = name.getString();
            copy.set(DataComponents.CUSTOM_NAME, Component.literal(plain.length() > NAME_LENGTH ? plain.substring(0, NAME_LENGTH) : plain));
        }
        return copy;
    }

    /** The size of the payload once encoded, in bytes. */
    public static int encodedSize(ServerPlayer reader, MindReadPayload payload) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), reader.registryAccess());
        try {
            MindReadPayload.STREAM_CODEC.encode(buf, payload);
            return buf.readableBytes();
        } finally {
            buf.release();
        }
    }

    /** The payload with the last stacks of what it carries dropped until it fits in {@link #MAX_BYTES}. */
    private static MindReadPayload bounded(ServerPlayer reader, MindReadPayload payload) {
        MindReadPayload bounded = payload;
        while (encodedSize(reader, bounded) > MAX_BYTES && !bounded.carried().isEmpty()) {
            List<ItemStack> fewer = new ArrayList<>(bounded.carried());
            fewer.remove(fewer.size() - 1);
            bounded = new MindReadPayload(bounded.entityId(), bounded.name(), bounded.species(), bounded.state(), bounded.mood(),
                    bounded.hostileTo(), bounded.health(), bounded.maxHealth(), bounded.food(), bounded.level(), bounded.worn(),
                    fewer, bounded.extra());
        }
        return bounded;
    }

    /** Whether two snapshots say the same (stacks are compared by what they hold, not by identity). */
    public static boolean sameContent(MindReadPayload a, MindReadPayload b) {
        return a.entityId() == b.entityId() && a.state() == b.state() && a.mood() == b.mood() && a.name().equals(b.name())
                && a.species().equals(b.species()) && a.hostileTo().equals(b.hostileTo()) && a.health() == b.health()
                && a.maxHealth() == b.maxHealth() && a.food() == b.food() && a.level() == b.level() && a.extra().equals(b.extra())
                && ItemStack.listMatches(a.worn(), b.worn()) && ItemStack.listMatches(a.carried(), b.carried());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Mind Sense

    /** Every creature within {@code range} of the Phoenix that has a mind (not hers), and how many of them are what. */
    public static Sensed sense(ServerPlayer phoenix, double range) {
        ServerLevel level = phoenix.serverLevel();
        List<LivingEntity> minds = new ArrayList<>();
        int players = 0;
        int hostile = 0;
        int silences = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, phoenix.getBoundingBox().inflate(range),
                e -> e != phoenix && PhoenixAbilities.hasMind(e) && e.distanceToSqr(phoenix) <= range * range)) {
            if (PhoenixAbilities.isShielded(e)) {
                silences++;
                continue;
            }
            minds.add(e);
            if (e instanceof Player) {
                players++;
            }
            if (e instanceof Enemy) {
                hostile++;
            }
        }
        return new Sensed(minds, players, hostile, silences);
    }

    private MindReading() {
    }
}

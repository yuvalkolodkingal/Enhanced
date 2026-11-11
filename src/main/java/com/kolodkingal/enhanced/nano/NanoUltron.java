package com.kolodkingal.enhanced.nano;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.EarnedLooks;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ultron in nanotech (3.12), the look a player who is Ultron earns at the Horton manor's storage ({@code ultron:nano}):
 * harder to hurt, harder hitting, faster in flight; his Sentry Army calls up to {@value #ARMY_MAX} people the nanotech
 * has taken instead of Sentries; his lasers burn hotter; and Sneak + his flight key turns him into pure nanotech, which
 * no blow, arrow or other physical thing can touch, for as long as he holds it.
 */
public final class NanoUltron {
    public static final int ARMY_MAX = 12;
    private static final int ARMY_CALL = 4;
    private static final int ARMY_LIFE = 20 * 90;
    private static final ResourceLocation ARMOR = Enhanced.id("ultron_nano_armor");
    private static final ResourceLocation TOUGHNESS = Enhanced.id("ultron_nano_toughness");
    private static final ResourceLocation DAMAGE = Enhanced.id("ultron_nano_damage");
    private static final Set<UUID> NANO_FORM = new HashSet<>();

    private NanoUltron() {
    }

    /** Whether the player is Ultron in nanotech now. */
    public static boolean is(Player player) {
        return Powers.is(player, Power.ULTRON) && "nano".equals(Powers.get(player).variant());
    }

    /** The storage touched by a player who is Ultron: he earns (and puts on) the nanotech body. */
    static void upgrade(ServerPlayer player) {
        Variant nano = Variants.get(Power.ULTRON, "nano");
        boolean first = EarnedLooks.earn(player, nano);
        Powers.setVariant(player, nano);
        ServerLevel level = player.serverLevel();
        Powers.burst(level, player.getX(), player.getY() + 1.0, player.getZ(), nano.color(), 90, 0.7);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 0.7F);
        player.displayClientMessage(Component.translatable(first ? "message.enhanced.nano_ultron.become" : "message.enhanced.look",
                nano.displayName()).withStyle(s -> s.withColor(nano.color())), false);
    }

    /** Every tick for Ultron: the nanotech body's strength (and the nano form ends when he is no longer it). */
    public static void applyBody(ServerPlayer player) {
        boolean nano = is(player);
        set(player, Attributes.ARMOR, ARMOR, 8.0, nano);
        set(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS, 6.0, nano);
        set(player, Attributes.ATTACK_DAMAGE, DAMAGE, 6.0, nano);
        if (!nano) {
            NANO_FORM.remove(player.getUUID());
        } else if (NANO_FORM.contains(player.getUUID()) && player.tickCount % 2 == 0) {
            player.serverLevel().sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.1F), 1.1F),
                    player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.35, 0.7, 0.35, 0.02);
        }
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (on && existing == null) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        } else if (!on && existing != null) {
            instance.removeModifier(id);
        }
    }

    /** Sneak + flight key: pure nanotech on or off. Returns whether the key was his (he is Ultron in nanotech). */
    public static boolean toggleForm(ServerPlayer player) {
        if (!is(player)) {
            return false;
        }
        boolean on = NANO_FORM.add(player.getUUID());
        if (!on) {
            NANO_FORM.remove(player.getUUID());
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                on ? SoundEvents.SCULK_CLICKING : SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.nano_ultron.form.on"
                : "message.enhanced.nano_ultron.form.off"), true);
        return true;
    }

    public static boolean inForm(Player player) {
        return NANO_FORM.contains(player.getUUID());
    }

    /** Whether a hit on this player is stopped by his nano form. */
    public static boolean shrugsOff(Player player, DamageSource source) {
        return inForm(player) && NanoUltronEntity.physical(source);
    }

    /** His army: people the nanotech has taken, {@value #ARMY_CALL} at a time, at most {@value #ARMY_MAX}. */
    public static boolean army(ServerPlayer player, PowerData data) {
        if (!is(player)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        List<NanoInfectedEntity> mine = level.getEntitiesOfClass(NanoInfectedEntity.class, player.getBoundingBox().inflate(96),
                e -> player.getUUID().equals(e.owner()));
        int room = ARMY_MAX - mine.size();
        if (room <= 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.nano_ultron.army.max", ARMY_MAX), true);
            return true;
        }
        for (int i = 0; i < Math.min(ARMY_CALL, room); i++) {
            NanoInfectedEntity infected = ModEntities.NANO_INFECTED.get().create(level);
            if (infected == null) {
                continue;
            }
            double a = player.getRandom().nextDouble() * Math.PI * 2;
            Vec3 at = player.position().add(Math.cos(a) * 2.5, 0.0, Math.sin(a) * 2.5);
            infected.moveTo(at.x, at.y, at.z, player.getYRot(), 0.0F);
            infected.serve(player.getUUID(), ARMY_LIFE);
            level.addFreshEntity(infected);
            level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.1F), 1.3F), at.x, at.y + 1, at.z, 20, 0.3, 0.6, 0.3, 0.02);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }

    public static void forget(Player player) {
        NANO_FORM.remove(player.getUUID());
    }
}

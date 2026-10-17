package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The server side of one power module (3.3): what it does every tick, on its key, to the hero's body, and when it ends.
 * One instance serves every custom hero; whatever it keeps per player is in {@link CustomState} (synced) or in a map of
 * its own (cleared by {@link #forget} and {@link #clear}). Every method gets a {@link Ctx}: the hero, his pick of the
 * module and its numbers at its strength.
 */
public interface ModuleLogic {
    /** Every tick of a hero who has this power (alive). */
    default void tick(Ctx c) {
    }

    /** Its key (only when the power is on a key): down and up. */
    default void key(Ctx c, boolean down) {
    }

    /**
     * The hero's body (attribute modifiers): {@code pick} is the module's pick while the player is a custom hero with it,
     * null otherwise (take every modifier off). Called for every player whenever the passives are applied.
     */
    default void body(ServerPlayer player, @Nullable CustomHero.Pick pick) {
    }

    /** The power goes away (the hero lost it, was replaced, died or left): end whatever it left in the world. */
    default void end(Ctx c) {
    }

    /** The player left the server: forget what is kept for him. */
    default void forget(ServerPlayer player) {
    }

    /** The server stopped: forget everything. */
    default void clear() {
    }

    /** One hero's use of one module, and the numbers of it. */
    final class Ctx {
        public final ServerPlayer player;
        public final PowerData data;
        public final CustomHero hero;
        public final CustomState state;
        public final CustomHero.Pick pick;
        public final PowerModule module;
        /** The key the module is on (1-5), or 0. */
        public final int slot;

        public Ctx(ServerPlayer player, PowerData data, CustomHero hero, CustomState state, CustomHero.Pick pick, PowerModule module, int slot) {
            this.player = player;
            this.data = data;
            this.hero = hero;
            this.state = state;
            this.pick = pick;
            this.module = module;
            this.slot = slot;
        }

        public ServerLevel level() {
            return player.serverLevel();
        }

        public long now() {
            return level().getGameTime();
        }

        /** The strength of the power, 1 to 10 (the editor's slider). */
        public int strength() {
            return Math.max(1, Math.min(PowerModule.MAX_STRENGTH, pick.strength()));
        }

        public boolean option(String id) {
            return module.option(pick.option()) == null ? module.defaultOption().id().equals(id) : pick.option().equals(id);
        }

        /** The power's colour (its own, or the hero's). */
        public int color() {
            return hero.colorOf(pick);
        }

        public int glow() {
            return hero.glow();
        }

        /** One of the power's numbers at its strength, scaled by the server's options ({@link ModuleStats}). */
        public float v(ModuleStats.Stat stat) {
            return stat.value(strength());
        }

        /** A number of ticks (a duration, a cooldown) at the power's strength. */
        public int ticks(ModuleStats.Stat stat) {
            return Math.max(1, Math.round(stat.value(strength())));
        }

        /** Whether the key's cooldown has run out. */
        public boolean ready() {
            return slot == 0 || data.isReady(slot, now());
        }

        /** Starts the key's cooldown: the stat's ticks at the power's strength (already times {@code cooldownScale}). */
        public void cooldown(ModuleStats.Stat stat) {
            if (slot > 0) {
                data.startCooldown(slot, now(), ticks(stat));
            }
        }

        /** Costs hunger (times {@code custom_hero.exhaustionScale}; nothing in Creative). */
        public void exhaust(float amount) {
            if (!player.getAbilities().instabuild) {
                player.causeFoodExhaustion(amount * (float) scale(CustomHeroConfig.EXHAUSTION_SCALE, 1.0));
            }
        }

        /** Whether the hero has the food for a power that needs some (a toggle that drains it ends when he is starving). */
        public boolean fed() {
            return player.getAbilities().instabuild || player.getFoodData().getFoodLevel() > 0 || scale(CustomHeroConfig.EXHAUSTION_SCALE, 1.0) == 0.0;
        }

        public void sound(SoundEvent sound, float volume, float pitch) {
            level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
        }

        public void sound(Holder<SoundEvent> sound, float volume, float pitch) {
            sound(sound.value(), volume, pitch);
        }

        /** Particles of the power's colour at a point, seen by everyone near. */
        public void sparks(Vec3 at, int count, double spread, float size) {
            level().sendParticles(new EnergyParticleOptions(color(), size), at.x, at.y, at.z, count, spread, spread, spread, 0.05);
        }

        /** The action bar line, in the power's colour. */
        public void say(String key, Object... args) {
            player.displayClientMessage(Component.translatable(key, args).withStyle(s -> s.withColor(color())), true);
        }

        /** The action bar line saying a power cannot be used (grey). */
        public void refuse(String key, Object... args) {
            player.displayClientMessage(Component.translatable(key, args).withStyle(net.minecraft.ChatFormatting.GRAY), true);
        }

        public Vec3 eyes() {
            return player.getEyePosition();
        }

        public Vec3 look() {
            return player.getLookAngle();
        }

        /** Marks the power data and the state for the clients (sent at the end of the tick). */
        public void changed() {
            data.markDirty();
        }

        /** Whether the power data says the hero is the one who has it (a held key's power is still his). */
        public boolean stillHis() {
            return Powers.get(player).power() == com.kolodkingal.enhanced.power.Power.CUSTOM;
        }
    }

    /** A server option, or its default when the config is not loaded (a client's own copy before it joins). */
    static double scale(net.neoforged.neoforge.common.ModConfigSpec.DoubleValue value, double fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    /** Adds or removes a permanent attribute modifier (as {@code PowerTicker} does for the other heroes). */
    static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present,
                         AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount || existing.operation() != operation) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present) {
        modifier(player, attribute, id, amount, present, AttributeModifier.Operation.ADD_VALUE);
    }
}

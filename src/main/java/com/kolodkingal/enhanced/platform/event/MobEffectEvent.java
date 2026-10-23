package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** An event about a living entity's effects (NeoForge's MobEffectEvent). */
public abstract class MobEffectEvent extends LivingEvent {
    private final @Nullable MobEffectInstance effectInstance;

    protected MobEffectEvent(LivingEntity entity, @Nullable MobEffectInstance effectInstance) {
        super(entity);
        this.effectInstance = effectInstance;
    }

    @Nullable
    public MobEffectInstance getEffectInstance() {
        return effectInstance;
    }

    /** An effect was added to a living entity (or replaced the old one). */
    public static class Added extends MobEffectEvent {
        private final @Nullable MobEffectInstance oldEffectInstance;
        private final @Nullable Entity effectSource;

        public Added(
                LivingEntity entity, @Nullable MobEffectInstance effectInstance,
                @Nullable MobEffectInstance oldEffectInstance, @Nullable Entity effectSource) {
            super(entity, effectInstance);
            this.oldEffectInstance = oldEffectInstance;
            this.effectSource = effectSource;
        }

        @Nullable
        public MobEffectInstance getOldEffectInstance() {
            return oldEffectInstance;
        }

        @Nullable
        public Entity getEffectSource() {
            return effectSource;
        }
    }

    /** May an effect be added to a living entity? setResult(DO_NOT_APPLY) refuses it. */
    public static class Applicable extends MobEffectEvent {
        private final @Nullable Entity effectSource;
        private Result result;

        public Applicable(
                LivingEntity entity, @Nullable MobEffectInstance effectInstance, @Nullable Entity effectSource) {
            super(entity, effectInstance);
            this.effectSource = effectSource;
            this.result = Result.DEFAULT;
        }

        @Nullable
        public Entity getEffectSource() {
            return effectSource;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        /** Whether the effect is applied: as the result says, or as the game would by default. */
        public boolean getApplicationResult() {
            MobEffectInstance effect = getEffectInstance();
            return getResult() == Result.APPLY
                    || getResult() == Result.DEFAULT && effect != null && getEntity().canBeAffected(effect);
        }

        /** Apply the effect, refuse it, or leave it to the game. */
        public enum Result {
            APPLY,
            DO_NOT_APPLY,
            DEFAULT
        }
    }

    /** An effect of a living entity ran out. */
    public static class Expired extends MobEffectEvent implements Cancellable {
        private boolean canceled;

        public Expired(LivingEntity entity, @Nullable MobEffectInstance effectInstance) {
            super(entity, effectInstance);
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    /** An effect is about to be removed (milk, a command...); cancelling keeps it. */
    public static class Remove extends MobEffectEvent implements Cancellable {
        private final Holder<MobEffect> effect;
        private boolean canceled;

        public Remove(LivingEntity entity, @Nullable MobEffectInstance effectInstance, Holder<MobEffect> effect) {
            super(entity, effectInstance);
            this.effect = effect;
        }

        public Holder<MobEffect> getEffect() {
            return effect;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }
}

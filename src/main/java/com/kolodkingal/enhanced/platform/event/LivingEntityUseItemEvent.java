package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** A living entity uses an item over time (NeoForge's LivingEntityUseItemEvent). */
public abstract class LivingEntityUseItemEvent extends LivingEvent {
    private final ItemStack item;
    private final InteractionHand hand;
    private int duration;

    protected LivingEntityUseItemEvent(LivingEntity entity, ItemStack item, InteractionHand hand, int duration) {
        super(entity);
        this.item = item;
        this.hand = hand;
        this.duration = duration;
    }

    public ItemStack getItem() {
        return item;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    /** A living entity starts to use an item (eat, draw a bow...); cancelling stops it. */
    public static class Start extends LivingEntityUseItemEvent implements Cancellable {
        private boolean canceled;

        public Start(LivingEntity entity, ItemStack item, InteractionHand hand, int duration) {
            super(entity, item, hand, duration);
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

    /** A tick of a living entity using an item; cancelling stops the use. */
    public static class Tick extends LivingEntityUseItemEvent implements Cancellable {
        private boolean canceled;

        public Tick(LivingEntity entity, ItemStack item, InteractionHand hand, int duration) {
            super(entity, item, hand, duration);
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

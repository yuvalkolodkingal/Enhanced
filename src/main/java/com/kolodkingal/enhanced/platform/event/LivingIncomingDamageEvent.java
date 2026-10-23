package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A living entity is about to be hurt, before armour and the other reductions: change the amount, cancel the blow, or
 * change a reduction.
 */
public class LivingIncomingDamageEvent extends LivingEvent implements Cancellable {
    private final DamageSource source;
    private float amount;
    private int invulnerabilityTicks;
    private final List<ReductionEntry> reductionModifiers;
    private boolean canceled;

    public LivingIncomingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
        super(entity);
        this.source = source;
        this.amount = amount;
        this.invulnerabilityTicks = 20;
        this.reductionModifiers = new ArrayList<>();
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public int getInvulnerabilityTicks() {
        return invulnerabilityTicks;
    }

    public void setInvulnerabilityTicks(int invulnerabilityTicks) {
        this.invulnerabilityTicks = invulnerabilityTicks;
    }

    public List<ReductionEntry> getReductionModifiers() {
        return reductionModifiers;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Changes how much one of the game's reductions takes off the blow, later in the damage pipeline (on NeoForge,
     * DamageContainer#addModifier): the modifier gets this event and the reduction the game worked out, and returns
     * the reduction to use.
     */
    public void addReductionModifier(Reduction type, ReductionModifier modifier) {
        reductionModifiers.add(new ReductionEntry(type, modifier));
    }

    /** The game's reductions of a blow, as NeoForge's DamageContainer.Reduction. */
    public enum Reduction {
        ARMOR,
        ENCHANTMENTS,
        MOB_EFFECTS,
        ABSORPTION
    }

    /** A change of a reduction: (the event, the reduction worked out) to the reduction used. */
    @FunctionalInterface
    public interface ReductionModifier {
        float modify(LivingIncomingDamageEvent event, float reduction);
    }

    public record ReductionEntry(Reduction type, ReductionModifier modifier) {
    }
}

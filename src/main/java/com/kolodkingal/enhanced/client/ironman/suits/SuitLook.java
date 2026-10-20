package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a suit looks like at one moment (3.8, PLAN 7): which suit, how far a phase has got (interpolated with the
 * partial tick), how lit its eyes and reactor are, and how far each of its moving extras is out (all eased between
 * ticks by {@link SuitFx}). Built from the synced {@link IronManState} and {@code Live} bits of a player ({@link #of}),
 * or still for a display ({@link #still}).
 */
public final class SuitLook {
    public SuitKind kind;
    /** The phase's progress (0-1) and direction; {@link #inPhase} false: the suit is simply on. */
    public float progress = 1.0F;
    public boolean up = true;
    public boolean inPhase;
    /** How lit the eyes, reactor, repulsors and lights are (0 dark, 1 on). */
    public float lit = 1.0F;
    /** How far out: the blades (Mark 50's nano blade, Mark 85's energy blades), the shields, the cannon. */
    public float blades;
    public float shield;
    public float cannon;
    /** The Mark 85's back flaps (0 flat, 1 fanned out) and the Mark 3's shoulder pods (0 shut, 1 open). */
    public float flaps;
    public float pods;
    /** The Hulkbuster's front shells (0 shut, 1 open: its frame). */
    public float frameOpen;
    public float partialTick;
    /** Game ticks plus the partial tick (for the slow lights). */
    public float time;
    public boolean slim;
    @Nullable
    public AbstractClientPlayer player;

    /** A suit standing still, on and lit (the displays). */
    public static SuitLook still(SuitKind kind, float time) {
        SuitLook look = new SuitLook();
        look.kind = kind;
        look.time = time;
        return look;
    }

    /** The suit a player shows now ({@link SuitRender#shownSuit}), or null when he shows none. */
    @Nullable
    public static SuitLook of(AbstractClientPlayer player, float partialTick) {
        SuitKind kind = SuitRender.shownSuit(player);
        if (kind == null) {
            return null;
        }
        IronManState state = IronSuits.state(player);
        SuitLook look = new SuitLook();
        look.kind = kind;
        look.player = player;
        look.partialTick = partialTick;
        look.time = player.tickCount + partialTick;
        look.slim = SuitRender.slim(player);
        if (state.inPhase() && state.phaseKind() == kind) {
            look.inPhase = true;
            look.up = state.phase() == IronManState.UP;
            look.progress = SuitTimelines.progress(player.level().getGameTime(), state.phaseStart(),
                    state.phaseTicks(), partialTick);
            look.lit = SuitTimelines.lit(kind, look.progress, look.up);
        } else if (broken(player, kind)) {
            look.lit = 0.0F;
        }
        look.blades = SuitFx.eased(player, SuitFx.BLADES, partialTick);
        look.shield = SuitFx.eased(player, SuitFx.SHIELD, partialTick);
        look.cannon = SuitFx.eased(player, SuitFx.CANNON, partialTick);
        look.flaps = SuitFx.eased(player, SuitFx.FLAPS, partialTick);
        look.pods = SuitFx.eased(player, SuitFx.PODS, partialTick);
        return look;
    }

    /** A worn classic suit at durability 0 is offline: its lights go dark (the armour still shows). */
    private static boolean broken(AbstractClientPlayer player, SuitKind kind) {
        if (kind.nanotech()) {
            return false;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.isDamageableItem() && chest.getDamageValue() >= chest.getMaxDamage() - 1
                && IronSuits.kind(chest) == kind;
    }
}

package com.kolodkingal.enhanced.ironman.hulkbuster;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.hulk.HulkForms;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitPhases;
import com.kolodkingal.enhanced.ironman.nano.NanoTech;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Hulkbuster (Mark 44, Age of Ultron; 3.8, lane L4, PLAN 6.4): its body while it is worn, and its suit-up and
 * take-off as a {@link SuitPhases.Listener} on lane L3's phases.
 *
 * <p><b>Putting it on.</b> Using the item outdoors (lane L3's {@code SuitControl.putOn}) begins the 80-tick UP: here
 * the listener first checks the sky ({@link Veronica#skyAbove}: "Veronica can't reach you in here.") and the room for a
 * 3.5× body ({@link HulkForms#roomFor}: "No room for the Hulkbuster here."); then Veronica's frame ({@link
 * HulkbusterFrameEntity}) falls from {@code dropHeight} 40 above a spot 2 in front of him (30 ticks), lands with dust
 * and a shake, opens (10), he is drawn into it (10), it closes round him (15) and he grows 1 → 3.5 (15), the room
 * checked every tick: blocked, the phase is aborted (the frame opens and lifts away, he is his size again, out of any
 * block). Put on the chest slot by hand (indoors too) the short version plays: the frame folds in round him (25) and he
 * grows (15), {@link #BY_HAND_TICKS} in all.
 *
 * <p><b>Taking it off</b> (H, DOWN 40): he shrinks (15), the frame stands open round him (10), he steps out 2 blocks
 * back (5) and it lifts off (10); lane L3 puts the item in the inventory.
 *
 * <p><b>The body</b> while it is on (an Iron Man's or a Creative player's): {@code SCALE} +{@code scaleBonus} 2.5 (the
 * attribute: every tracker sees him at 3.5×), step height +1, entity reach +2, both with the growth; explosion
 * immunity, no riding and no bed ({@link HulkbusterEvents}). The armour, full knockback resistance, walk speed and slow
 * flight are lane L3's (the item's attributes, {@code IronSuits}).
 */
public final class Hulkbuster implements SuitPhases.Listener {
    public static final Hulkbuster INSTANCE = new Hulkbuster();
    /** The suit-up's length when the item is put on the chest slot by hand. */
    public static final int BY_HAND_TICKS = 40;
    /** The UP phase's stages, as shares of its length (80: fall 30, open 10, step in 10, close 15, grow 15). */
    public static final float UP_LANDED = 30.0F / 80.0F;
    public static final float UP_STEP_IN = 40.0F / 80.0F;
    public static final float UP_CLOSE = 50.0F / 80.0F;
    public static final float UP_GROW = 65.0F / 80.0F;
    /** The by-hand version's: the frame folds in (25 of 40), then the growth. */
    public static final float HAND_GROW = 25.0F / 40.0F;
    /** The DOWN phase's (40: shrink 15, open 10, step out 5, lift off 10). */
    public static final float DOWN_OPEN = 15.0F / 40.0F;
    public static final float DOWN_OUT = 25.0F / 40.0F;
    public static final float DOWN_LIFT = 30.0F / 40.0F;
    /** How far in front of him the frame lands, and how far back he steps out of it. */
    public static final double DROP_AHEAD = 2.0;
    public static final double STEP_OUT = 2.0;
    /** A shake is felt this far from the landing. */
    public static final int SHAKE_RADIUS = 12;

    private static final ResourceLocation SCALE = Enhanced.id("hulkbuster_scale");
    private static final ResourceLocation STEP = Enhanced.id("hulkbuster_step");
    private static final ResourceLocation REACH = Enhanced.id("hulkbuster_reach");
    static final double STEP_BONUS = 1.0;
    static final double REACH_BONUS = 2.0;

    /** Each wearer's frame entity (its id) while a phase shows one, and whether the last suit-up was aborted. */
    private static final Map<UUID, Integer> FRAMES = new HashMap<>();
    private static final Map<UUID, Boolean> NO_ROOM = new HashMap<>();

    // ---- the numbers ----

    public static double scaleBonus() {
        return NanoTech.cfg(IronManConfig.HULKBUSTER_SCALE_BONUS, 2.5);
    }

    /** Whether the Hulkbuster's item is on his chest slot. */
    public static boolean onChest(Player player) {
        return IronSuits.kind(player.getItemBySlot(EquipmentSlot.CHEST)) == SuitKind.HULKBUSTER;
    }

    /** Whether he may have its body (Iron Man, or a Creative builder). */
    static boolean mayHaveBody(Player player) {
        return player.isCreative() || Powers.power(player) == Power.IRON_MAN;
    }

    /**
     * How far grown into the Hulkbuster he is (0 his own size, 1 the full 3.5×) at {@code gameTime + partialTick}, from
     * the synced state (both sides: the renderer swap asks it too). Outside a phase: 1 while it is worn.
     */
    public static float growth(Player player, IronManState state, long gameTime, float partialTick) {
        if (state.inPhase()) {
            if (state.phaseKind() != SuitKind.HULKBUSTER) {
                return 0.0F;
            }
            float t = phaseProgress(state, gameTime, partialTick);
            if (state.phase() == IronManState.UP) {
                float from = state.phaseTicks() <= BY_HAND_TICKS ? HAND_GROW : UP_GROW;
                return Mth.clamp((t - from) / (1.0F - from), 0.0F, 1.0F);
            }
            return 1.0F - Mth.clamp(t / DOWN_OPEN, 0.0F, 1.0F);
        }
        return onChest(player) && mayHaveBody(player) ? 1.0F : 0.0F;
    }

    /** The phase's progress (0-1). */
    public static float phaseProgress(IronManState state, long gameTime, float partialTick) {
        return state.phaseTicks() <= 0 ? 1.0F : Mth.clamp((gameTime - state.phaseStart() + partialTick)
                / state.phaseTicks(), 0.0F, 1.0F);
    }

    /**
     * Whether he is drawn as the Hulkbuster now (the client's renderer swap): it is worn and grown, or a phase has
     * shut the frame round him and not yet opened it.
     */
    public static boolean drawnAsHulkbuster(Player player, IronManState state, long gameTime, float partialTick) {
        if (state.inPhase()) {
            if (state.phaseKind() != SuitKind.HULKBUSTER) {
                return false;
            }
            float t = phaseProgress(state, gameTime, partialTick);
            if (state.phase() == IronManState.UP) {
                return t >= (state.phaseTicks() <= BY_HAND_TICKS ? HAND_GROW : UP_GROW);
            }
            return t < DOWN_OPEN;
        }
        return onChest(player) && mayHaveBody(player);
    }

    // ---- the body ----

    /**
     * Every tick of every player: the Hulkbuster's size (with the phase's growth), step and reach while it is on, none
     * of it otherwise. Worn with no phase and not yet grown (put on while a phase could not begin, after a respawn or a
     * login): it grows at once if there is room, else it comes off into the inventory.
     */
    public static void applyBody(ServerPlayer player) {
        IronManState state = IronSuits.state(player);
        boolean hulkbusterPhase = state.inPhase() && state.phaseKind() == SuitKind.HULKBUSTER;
        if (!hulkbusterPhase && !onChest(player) && current(player) == 0.0) {
            return; // (nobody's Hulkbuster: nothing to put on or take off)
        }
        double full = scaleBonus();
        if (!hulkbusterPhase && onChest(player) && mayHaveBody(player) && player.isAlive()
                && current(player) < full - 1.0E-3) {
            if (!HulkForms.roomFor(player, full)) {
                popOff(player);
                return;
            }
        }
        float g = player.isAlive() ? growth(player, state, player.level().getGameTime(), 0.0F) : 0.0F;
        set(player, Attributes.SCALE, SCALE, full * g, g > 1.0E-4F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.STEP_HEIGHT, STEP, STEP_BONUS * g, g > 1.0E-4F, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ENTITY_INTERACTION_RANGE, REACH, REACH_BONUS * g, g > 1.0E-4F,
                AttributeModifier.Operation.ADD_VALUE);
    }

    /** His Hulkbuster's scale bonus now (0 when none). */
    public static double current(Player player) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        AttributeModifier modifier = scale == null ? null : scale.getModifier(SCALE);
        return modifier == null ? 0.0 : modifier.amount();
    }

    /** He is his own size at once (death, the power lost). */
    public static void shrinkNow(Player player) {
        set(player, Attributes.SCALE, SCALE, 0.0, false, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.STEP_HEIGHT, STEP, 0.0, false, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ENTITY_INTERACTION_RANGE, REACH, 0.0, false, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void set(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount,
            boolean present, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || Math.abs(existing.amount() - amount) > 1.0E-6) {
                instance.removeModifier(id);
                instance.addTransientModifier(new AttributeModifier(id, amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    /** The Hulkbuster cannot stand here: it comes off into the inventory (or drops), "No room for the Hulkbuster". */
    static void popOff(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (IronSuits.kind(chest) == SuitKind.HULKBUSTER) {
            ItemStack suit = chest.copy();
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            if (!player.getInventory().add(suit) && !suit.isEmpty()) {
                player.drop(suit, false);
            }
        }
        shrinkNow(player);
        noRoom(player);
    }

    static void noRoom(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.enhanced.nanotech.no_room")
                .withStyle(ChatFormatting.GRAY), true);
    }

    /** If he stands in blocks (an aborted growth), he is moved to the nearest spot with room for him. */
    static void freeSpot(ServerPlayer player) {
        if (HulkForms.roomFor(player, 0.0)) {
            return;
        }
        BlockPos at = player.blockPosition();
        for (int r = 1; r <= 4; r++) {
            for (int dy = 0; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r && dy != r) {
                            continue;
                        }
                        Vec3 spot = Vec3.atBottomCenterOf(at.offset(dx, dy, dz));
                        Vec3 old = player.position();
                        player.setPos(spot.x, spot.y, spot.z);
                        boolean room = HulkForms.roomFor(player, 0.0)
                                && !player.level().getBlockState(at.offset(dx, dy - 1, dz)).isAir();
                        player.setPos(old.x, old.y, old.z);
                        if (room) {
                            player.teleportTo(spot.x, spot.y, spot.z);
                            return;
                        }
                    }
                }
            }
        }
    }

    // ---- the phases (lane L3's framework, this suit's listener) ----

    @Override
    public boolean mayBegin(ServerPlayer player, SuitKind kind, boolean up, boolean byHand) {
        if (!up) {
            return true;
        }
        if (!byHand && NanoTech.cfg(IronManConfig.HULKBUSTER_NEEDS_SKY, true)
                && !(Veronica.skyAbove(player.level(), player.blockPosition().above(), Veronica.dropHeight())
                && Veronica.skyAbove(player.level(), BlockPos.containing(dropSpot(player)).above(),
                Veronica.dropHeight()))) {
            player.displayClientMessage(Component.translatable("message.enhanced.nanotech.veronica_indoors")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!HulkForms.roomFor(player, scaleBonus())) {
            noRoom(player);
            return false;
        }
        return true;
    }

    @Override
    public void begun(ServerPlayer player, SuitKind kind, boolean up) {
        NO_ROOM.remove(player.getUUID());
        if (!up) {
            return;
        }
        ServerLevel level = player.serverLevel();
        HulkbusterFrameEntity frame;
        if (SuitPhases.byHand(player)) {
            // The short version: the phase is BY_HAND_TICKS long; the frame folds in round him.
            IronManState state = IronSuits.state(player);
            if (state.phaseTicks() != BY_HAND_TICKS) {
                IronSuits.setState(player, state.withPhase(state.phase(), kind, state.phaseStart(), BY_HAND_TICKS));
            }
            frame = HulkbusterFrameEntity.standing(player, HulkbusterFrameEntity.ASSEMBLING);
            NanoTech.sound(level, player.position(), ModSounds.HULKBUSTER_CLOSE.get(), 1.0F, 1.0F);
        } else {
            Vec3 landing = dropSpot(player);
            frame = HulkbusterFrameEntity.dropping(player, landing.add(0.0, Veronica.dropHeight(), 0.0), landing);
            NanoTech.sound(level, landing, ModSounds.IRON_THRUSTERS.get(), 1.5F, 0.6F);
        }
        level.addFreshEntity(frame);
        FRAMES.put(player.getUUID(), frame.getId());
    }

    @Override
    public void ticked(ServerPlayer player, SuitKind kind, boolean up, int tick, int ticks) {
        ServerLevel level = player.serverLevel();
        HulkbusterFrameEntity frame = frame(player);
        if (up) {
            boolean byHand = ticks <= BY_HAND_TICKS;
            int grow = Math.round((byHand ? HAND_GROW : UP_GROW) * ticks);
            if (!byHand && frame != null) {
                int landed = Math.round(UP_LANDED * ticks);
                int stepIn = Math.round(UP_STEP_IN * ticks);
                int close = Math.round(UP_CLOSE * ticks);
                if (tick == landed) {
                    frame.landed(level);
                    frame.setStage(HulkbusterFrameEntity.OPENING);
                    NanoTech.send(level, frame, IronFxPayload.FRAME_LAND, List.of(frame.landing()), SHAKE_RADIUS);
                    NanoTech.sound(level, frame.landing(), ModSounds.HULKBUSTER_DROP.get(), 2.0F, 1.0F);
                    NanoTech.sound(level, frame.landing(), ModSounds.HULKBUSTER_OPEN.get(), 1.0F, 1.0F);
                } else if (tick > stepIn && tick <= close) {
                    // He is drawn into the open frame, a fifth of a block a tick (the rest at the last step).
                    Vec3 to = frame.landing();
                    Vec3 at = player.position();
                    Vec3 d = to.subtract(at);
                    Vec3 next = tick == close || d.length() <= 0.2 ? to : at.add(d.normalize().scale(0.2));
                    player.teleportTo(next.x, next.y, next.z);
                    if (tick == close) {
                        frame.setStage(HulkbusterFrameEntity.CLOSING);
                        NanoTech.sound(level, to, ModSounds.HULKBUSTER_CLOSE.get(), 1.2F, 1.0F);
                    }
                }
            }
            if (tick == grow && frame != null) {
                // The frame has shut round him: from now on he is the Hulkbuster.
                frame.discard();
                FRAMES.remove(player.getUUID());
            }
            if (tick >= grow) {
                float next = Math.min(1.0F, (float) (tick + 1 - grow) / Math.max(1, ticks - grow));
                if (!HulkForms.roomFor(player, scaleBonus() * next)) {
                    NO_ROOM.put(player.getUUID(), true);
                    SuitPhases.abort(player);
                }
            }
            return;
        }
        int open = Math.round(DOWN_OPEN * ticks);
        int out = Math.round(DOWN_OUT * ticks);
        int lift = Math.round(DOWN_LIFT * ticks);
        if (tick == open) {
            frame = HulkbusterFrameEntity.standing(player, HulkbusterFrameEntity.OPENING);
            level.addFreshEntity(frame);
            FRAMES.put(player.getUUID(), frame.getId());
            NanoTech.sound(level, player.position(), ModSounds.HULKBUSTER_OPEN.get(), 1.0F, 1.0F);
        } else if (tick > out && tick <= lift && frame != null) {
            Vec3 back = Vec3.directionFromRotation(0.0F, frame.getYRot() + 180.0F)
                    .scale(STEP_OUT / Math.max(1, lift - out));
            Vec3 next = player.position().add(back);
            Vec3 old = player.position();
            player.setPos(next.x, next.y, next.z);
            boolean room = HulkForms.roomFor(player, 0.0);
            player.setPos(old.x, old.y, old.z);
            if (room) {
                player.teleportTo(next.x, next.y, next.z);
            }
            NanoTech.sound(level, player.position(), ModSounds.HULKBUSTER_STEP.get(), 0.6F, 1.2F);
        }
        if (tick == lift && frame != null) {
            frame.liftOff();
            FRAMES.remove(player.getUUID());
        }
    }

    @Override
    public void ended(ServerPlayer player, SuitKind kind, boolean up, boolean completed) {
        HulkbusterFrameEntity frame = frame(player);
        FRAMES.remove(player.getUUID());
        boolean noRoom = NO_ROOM.remove(player.getUUID()) != null;
        if (up && completed) {
            if (frame != null) {
                frame.discard();
            }
            if (onChest(player) && !HulkForms.roomFor(player, scaleBonus())) {
                popOff(player);
                freeSpot(player);
                return;
            }
            applyBody(player);
            return;
        }
        // Aborted, or the take-off's end: the frame (if any) lifts away and he is his own size.
        if (frame != null) {
            frame.liftOff();
        }
        shrinkNow(player);
        if (up) {
            if (noRoom) {
                noRoom(player);
            }
            freeSpot(player);
        }
    }

    /** Where the frame lands: 2 blocks in front of him, on the ground there. */
    static Vec3 dropSpot(Player player) {
        Vec3 ahead = player.position().add(Vec3.directionFromRotation(0.0F, player.getYRot()).scale(DROP_AHEAD));
        Level level = player.level();
        BlockPos pos = BlockPos.containing(ahead.x, player.getY() + 1.0, ahead.z);
        for (int i = 0; i < 6; i++) {
            if (!level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                return new Vec3(ahead.x, pos.getY(), ahead.z);
            }
            pos = pos.below();
        }
        return new Vec3(ahead.x, player.getY(), ahead.z);
    }

    /** His frame, if one stands now. */
    @Nullable
    static HulkbusterFrameEntity frame(ServerPlayer player) {
        Integer id = FRAMES.get(player.getUUID());
        return id != null && player.level().getEntity(id) instanceof HulkbusterFrameEntity frame ? frame : null;
    }

    static void forget(UUID id) {
        FRAMES.remove(id);
        NO_ROOM.remove(id);
    }

    static void clear() {
        FRAMES.clear();
        NO_ROOM.clear();
    }

    private Hulkbuster() {
    }
}

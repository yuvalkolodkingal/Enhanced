package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.CyclopsConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.item.RubyQuartzGlassesItem;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Cyclops's ruby-quartz glasses, and what pours out of his eyes without them (3.3). The glasses are worn in the head slot;
 * every tick {@link #tick} compares what he wears with what he wore, so the glasses coming off any way at all (Sneak + N,
 * dragged out of the slot, knocked off by a command) lets the <b>giant beam</b> out: a wide red beam from his eyes where he
 * looks, which hurts and shoves everything along it and does not stop until they are back on (N, a right-click, the slot).
 *
 * <p>It is made safe: it never hurts Cyclops himself, costs him no hunger (an uncontrolled power must not starve him), breaks
 * no block unless {@code cyclops.unleashedBreaksBlocks} (and then never round his own feet, in an arena or where he may not
 * break), pauses while sand holds his face (Entombed), and ends when he dies, leaves, loses the power or becomes a spectator.
 * Coming back (a respawn, a login) never starts it: only the glasses coming off does. With no glasses left anywhere on him,
 * N shuts his eyes (the beam ends; he still needs the glasses to aim).
 *
 * <p>Taking them off and putting them on start a short cooldown on N that is only the signal of its gesture for the
 * clients ({@link #OFF_TICKS}, {@link #ON_TICKS}: the lengths tell the two apart); N never waits for it.
 */
public final class CyclopsGlasses {
    /** The cooldown N shows when the glasses come off, and when they go on (the gestures' signals). */
    public static final int OFF_TICKS = 24;
    public static final int ON_TICKS = 10;
    /** The giant beam never breaks a block this close to his feet (he would fall into his own hole). */
    public static final double SAFE_RADIUS = 2.5;

    // ---- the glasses ----

    /** Whether this creature wears the ruby-quartz glasses (in its head slot). */
    public static boolean wears(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.RUBY_QUARTZ_GLASSES.get());
    }

    private static boolean isGlasses(ItemStack stack) {
        return stack.is(ModItems.RUBY_QUARTZ_GLASSES.get());
    }

    /**
     * Every tick of a Cyclops: the glasses coming off lets the giant beam out, and going back on stops it. The first look
     * after a login, a respawn or a new power only notes what he wears.
     */
    static void tick(ServerPlayer player, PowerData data) {
        CyclopsAbilities.State state = CyclopsAbilities.state(player.getUUID());
        boolean wearing = wears(player);
        if (state.wore == null) {
            state.wore = wearing;
            if (wearing && data.is(Live.CYCLOPS_UNLEASHED)) {
                end(player, data, false);
            }
            return;
        }
        if (state.wore && !wearing) {
            cameOff(player, data);
        } else if (!state.wore && wearing) {
            wentOn(player, data);
        } else if (wearing && data.is(Live.CYCLOPS_UNLEASHED)) {
            end(player, data, false);
        }
        state.wore = wearing;
    }

    /**
     * Sneak + N: he takes the glasses off (into his hand, else his inventory, else the off hand, else at his feet). A custom
     * hero with the key (3.4) wears no glasses: he opens his eyes wide, and the giant beam pours out.
     */
    static void takeOff(ServerPlayer player, PowerData data) {
        if (!CyclopsAbilities.needsGlasses(data)) {
            if (data.is(Live.CYCLOPS_UNLEASHED)) {
                player.displayClientMessage(Component.translatable("message.enhanced.cyclops.eyes_already_open"), true);
            } else {
                cameOff(player, data);
            }
            return;
        }
        if (!wears(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.cyclops.not_wearing"), true);
            return;
        }
        ItemStack glasses = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        stow(player, glasses);
        CyclopsAbilities.state(player.getUUID()).wore = false;
        cameOff(player, data);
    }

    /** Where glasses taken off go: the main hand if it is empty, else the inventory, else the off hand, else dropped at his feet. */
    private static void stow(ServerPlayer player, ItemStack glasses) {
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, glasses);
        } else if (!player.getInventory().add(glasses)) {
            if (player.getOffhandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.OFF_HAND, glasses);
            } else {
                player.drop(glasses, false, true);
            }
        }
    }

    /**
     * N: he puts the glasses back on, from his hands or anywhere in his inventory (whatever was on his head takes their
     * place). With none left anywhere on him and the beam pouring out, he shuts his eyes and it ends.
     */
    static void putOn(ServerPlayer player, PowerData data) {
        if (!CyclopsAbilities.needsGlasses(data)) {
            // (A custom hero's key: he shuts his eyes, with the gesture of the glasses going on.)
            if (data.is(Live.CYCLOPS_UNLEASHED)) {
                end(player, data, true);
                data.startCooldown(HeroAbility.CYCLOPS_GLASSES, player.level().getGameTime(), ON_TICKS);
            } else {
                player.displayClientMessage(Component.translatable("message.enhanced.cyclops.eyes_already_shut"), true);
            }
            return;
        }
        if (wears(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.cyclops.already_on"), true);
            return;
        }
        Inventory inventory = player.getInventory();
        int slot = -1;
        if (isGlasses(player.getMainHandItem())) {
            slot = inventory.selected;
        } else {
            for (int i = 0; i < inventory.items.size(); i++) {
                if (isGlasses(inventory.items.get(i))) {
                    slot = i;
                    break;
                }
            }
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        if (slot >= 0) {
            ItemStack glasses = inventory.items.get(slot).split(1);
            if (inventory.items.get(slot).isEmpty()) {
                inventory.items.set(slot, head);
            } else if (!head.isEmpty() && !inventory.add(head)) {
                player.drop(head, false, true);
            }
            player.setItemSlot(EquipmentSlot.HEAD, glasses);
        } else if (isGlasses(player.getOffhandItem())) {
            ItemStack glasses = player.getOffhandItem().split(1);
            player.setItemInHand(InteractionHand.OFF_HAND, head);
            player.setItemSlot(EquipmentSlot.HEAD, glasses);
        } else {
            if (data.is(Live.CYCLOPS_UNLEASHED)) {
                end(player, data, true);
            } else {
                player.displayClientMessage(Component.translatable("message.enhanced.cyclops.no_glasses_left"), true);
            }
            return;
        }
        CyclopsAbilities.state(player.getUUID()).wore = true;
        wentOn(player, data);
    }

    // ---- off and on ----

    /** The glasses came off: the giant beam pours from his eyes (unless he is dead or a spectator). */
    private static void cameOff(ServerPlayer player, PowerData data) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (ContinuousBeam.isFiring(player)) {
            ContinuousBeam.stop(player); // (the optic blast he was holding)
        }
        data.set(Live.CYCLOPS_UNLEASHED, true);
        data.startCooldown(HeroAbility.CYCLOPS_GLASSES, level.getGameTime(), OFF_TICKS);
        ContinuousBeam.start(player, RubyQuartzGlassesItem.UNLEASHED, CyclopsConfig.UNLEASHED_RANGE.get());
        player.displayClientMessage(Component.translatable(CyclopsAbilities.needsGlasses(data) ? "message.enhanced.cyclops.unleashed"
                : "message.enhanced.cyclops.eyes_opened"), true);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.CYCLOPS_UNLEASHED_START.get(), SoundSource.PLAYERS,
                1.4F, 1.0F);
    }

    /** The glasses went on: the giant beam stops, with the click of the visor. */
    private static void wentOn(ServerPlayer player, PowerData data) {
        boolean pouring = data.is(Live.CYCLOPS_UNLEASHED);
        end(player, data, false);
        ServerLevel level = player.serverLevel();
        data.startCooldown(HeroAbility.CYCLOPS_GLASSES, level.getGameTime(), ON_TICKS);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.CYCLOPS_VISOR.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        if (pouring) {
            player.displayClientMessage(Component.translatable("message.enhanced.cyclops.glasses_on"), true);
        }
    }

    /** The giant beam ends (the glasses are on, his eyes are shut, he died, left or lost the power). */
    static void end(ServerPlayer player, PowerData data, boolean shut) {
        if (!data.is(Live.CYCLOPS_UNLEASHED)) {
            return;
        }
        data.set(Live.CYCLOPS_UNLEASHED, false);
        if (ContinuousBeam.isFiring(player)) {
            ContinuousBeam.stop(player);
        }
        if (shut) {
            player.displayClientMessage(Component.translatable("message.enhanced.cyclops.eyes_shut"), true);
        }
    }

    // ---- the giant beam ----

    /** The numbers of a tick of the giant beam, from the config: no hunger, and every hit shoves along the beam. */
    public static BeamSpec unleashedSpec() {
        double push = CyclopsConfig.UNLEASHED_PUSH.get();
        return new BeamSpec(RubyQuartzGlassesItem.UNLEASHED, CyclopsConfig.UNLEASHED_RANGE.get(), CyclopsConfig.UNLEASHED_DAMAGE.get().floatValue(),
                CyclopsConfig.UNLEASHED_INTERVAL.get(), 0.0F, ModDamageTypes.OPTIC_BLAST,
                (shooter, target) -> CyclopsAbilities.shove(shooter, target, shooter.getLookAngle(), push, 0.08));
    }

    /**
     * One tick of the giant beam: the shared beam hits what the crosshair is on, and every {@code unleashedInterval} ticks
     * everything else within {@code unleashedRadius} of its line is hit and shoved too, up to the block (or the Chaos Shield)
     * that stops it; that block, with {@code unleashedBreaksBlocks}, breaks.
     */
    static void unleashedTick(ServerPlayer player, PowerData data) {
        if (player.isSpectator()) {
            end(player, data, false);
            return;
        }
        if (player.hasEffect(ModEffects.ENTOMBED)) {
            if (ContinuousBeam.isFiring(player)) {
                ContinuousBeam.stop(player); // (sand over his face holds it back while it lasts)
            }
            return;
        }
        if (ContinuousBeam.isFiring(player) && !data.is(Live.BEAMING)) {
            ContinuousBeam.stop(player); // (something that lets go of every held ability took the flag away: it starts again)
        }
        if (!ContinuousBeam.isFiring(player)) {
            ContinuousBeam.start(player, RubyQuartzGlassesItem.UNLEASHED, CyclopsConfig.UNLEASHED_RANGE.get());
        }
        ContinuousBeam.tick(player, unleashedSpec());
        CyclopsAbilities.State state = CyclopsAbilities.state(player.getUUID());
        long now = player.level().getGameTime();
        int interval = CyclopsConfig.UNLEASHED_INTERVAL.get();
        if (now - state.lastSweep >= interval || now < state.lastSweep) {
            state.lastSweep = now;
            sweep(player);
        }
    }

    /** Where the giant beam ends: the first block along his look within its range (or that range), or a Chaos Shield before it. */
    public static Vec3 unleashedEnd(ServerPlayer player, @Nullable BlockHitResult[] blockOut) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getLookAngle().scale(CyclopsConfig.UNLEASHED_RANGE.get()));
        BlockHitResult block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, eye, end, 1.0F);
        if (blockOut != null) {
            blockOut[0] = shield == null && block.getType() == HitResult.Type.BLOCK ? block : null;
        }
        return shield != null ? shield.point() : end;
    }

    /** Everything along the giant beam but what the crosshair is on (the shared beam hits that one) is hit and shoved. */
    private static void sweep(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockHitResult[] block = new BlockHitResult[1];
        Vec3 eye = player.getEyePosition();
        Vec3 end = unleashedEnd(player, block);
        HitResult crosshair = Raycasts.any(player, CyclopsConfig.UNLEASHED_RANGE.get(), ContinuousBeam.targets(player), 1.0F);
        Entity aimed = crosshair instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        if (aimed instanceof PartEntity<?> part) {
            aimed = part.getParent();
        }
        float damage = CyclopsConfig.UNLEASHED_DAMAGE.get().floatValue();
        double push = CyclopsConfig.UNLEASHED_PUSH.get();
        Vec3 look = player.getLookAngle();
        for (LivingEntity victim : CyclopsAbilities.along(level, player, eye, end, CyclopsConfig.UNLEASHED_RADIUS.get())) {
            if (victim == aimed || !AreaTargets.mayHurt(player, victim, true)) {
                continue;
            }
            victim.hurt(ModDamageTypes.source(level, ModDamageTypes.OPTIC_BLAST, player, player), damage);
            CyclopsAbilities.shove(player, victim, look, push, 0.08);
        }
        if (block[0] != null && CyclopsConfig.UNLEASHED_BREAKS_BLOCKS.get()) {
            breakAround(player, level, block[0].getBlockPos());
        }
    }

    /**
     * The giant beam breaks what it strikes (only with {@code unleashedBreaksBlocks}): the block and those round it within the
     * beam's radius, no harder than {@code unleashedMaxHardness}, never within {@link #SAFE_RADIUS} of his feet, and only
     * where he may break ({@link BlockRights#mayBreak}: no boss arena, the game's own rules and protections).
     *
     * @return how many blocks broke
     */
    public static int breakAround(ServerPlayer player, ServerLevel level, BlockPos center) {
        int reach = (int) Math.ceil(CyclopsConfig.UNLEASHED_RADIUS.get());
        double max = CyclopsConfig.UNLEASHED_MAX_HARDNESS.get();
        double radius = CyclopsConfig.UNLEASHED_RADIUS.get();
        int broke = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            if (pos.distSqr(center) > radius * radius + 0.01) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness < 0.0F || hardness > max || Vec3.atCenterOf(pos).distanceTo(player.position()) <= SAFE_RADIUS) {
                continue;
            }
            BlockPos cell = pos.immutable();
            if (BlockRights.mayBreak(player, level, cell, state) && level.destroyBlock(cell, true, player)) {
                broke++;
            }
        }
        return broke;
    }

    private CyclopsGlasses() {
    }
}

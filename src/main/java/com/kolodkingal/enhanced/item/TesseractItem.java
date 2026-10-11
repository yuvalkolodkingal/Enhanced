package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Tesseract, with the Space Stone inside. In Avengers: Endgame the 2012 Loki grabs it and vanishes in a blue swirl:
 * that escape is where the Loki of the TVA comes from. Used by someone without powers, it does the same: a swirl, a
 * flash, and they are Loki somewhere far away; unless they hold a Scarlet Witch Gene in their other hand: then it
 * awakens them as the Scarlet Witch instead, exactly as Loki's Scepter does. A hero who uses it is simply taken to the
 * block they look at. The cube itself is never used up. The Space Stone's voice (see {@link InfinityStone}) sounds
 * whenever it is used, under the portal's own sound; and a soft hum of it is heard while someone holds the cube.
 */
public class TesseractItem extends Item {
    /** The Space Stone palette: dim, mid, bright, core. */
    public static final int DIM = 0x028EB4;
    public static final int MID = 0x1FB6CF;
    public static final int BRIGHT = 0x5DCCE1;
    public static final int CORE = 0xA0DBE8;

    public static final int ESCAPE_MIN = 100;
    public static final int ESCAPE_MAX = 250;
    public static final int ESCAPE_TRIES = 12;
    public static final int JUMP_RANGE = 48;
    public static final int JUMP_COOLDOWN = 100;

    public TesseractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (Powers.power(serverPlayer).hasPowers()) {
                if (!jump(serverPlayer)) {
                    return InteractionResultHolder.fail(stack);
                }
                player.getCooldowns().addCooldown(this, JUMP_COOLDOWN);
            } else {
                InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                ItemStack gene = player.getItemInHand(otherHand);
                if (gene.is(ModItems.SCARLET_WITCH_GENE.get())) {
                    // No portal here, but the Space Stone still answers the gene.
                    InfinityStone.SPACE.use(serverPlayer.serverLevel(), serverPlayer.position(), InfinityStone.UNDER, 1.0F);
                    ScarletWitchGeneItem.awaken(serverPlayer, gene);
                } else if (MjolnirPin.isPinned(serverPlayer)) {
                    // Loki's escape takes him a hundred blocks away: not with Mjolnir lying on him. (A hero's jump is a
                    // teleport like any other: HexManager.onTeleport says no.)
                    serverPlayer.displayClientMessage(Component.translatable("message.enhanced.mjolnir.pinned_cannot"), true);
                    return InteractionResultHolder.fail(stack);
                } else {
                    escape(serverPlayer);
                }
                player.getCooldowns().addCooldown(this, 20);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Loki's escape: the Space Stone swirls round the player, they vanish in a blue flash and reappear 100-250 blocks
     * away on safe ground (or stay where they are, if none is found), and awaken as Loki.
     */
    public static void escape(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        swirl(level, player.position());
        Vec3 from = player.position();
        Vec3 to = Teleports.farAway(player, player.getRandom(), ESCAPE_MIN, ESCAPE_MAX, ESCAPE_TRIES);
        portalSound(level, from, 1.0F, 1.0F);
        if (to != null) {
            flash(level, from);
            float yaw = (float) (Mth.atan2(-(to.x - from.x), to.z - from.z) * Mth.RAD_TO_DEG);
            player.teleportTo(level, to.x, to.y, to.z, yaw, 0.0F);
            player.resetFallDistance();
            flash(level, to);
            portalSound(level, to, 0.7F, 1.2F);
        }
        Powers.awaken(player, Power.LOKI);
    }

    /**
     * A hero's jump: the Space Stone takes them to the block they look at (up to 48 blocks away), on top of it if
     * there is room. The Hex wall still stops them.
     *
     * @return false if nothing happened (nothing in reach, no room, or the Hex refused)
     */
    public static boolean jump(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockHitResult hit = Raycasts.block(player, JUMP_RANGE, 1.0F);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.enhanced.tesseract.no_block", JUMP_RANGE), true);
            return false;
        }
        Vec3 to = Teleports.onto(player, hit);
        if (to == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.teleport.no_room"), true);
            return false;
        }
        if (!HexManager.onTeleport(level, player, to)) {
            return false;
        }
        Vec3 from = player.position();
        flash(level, from);
        portalSound(level, from, 0.8F, 1.3F);
        player.teleportTo(level, to.x, to.y, to.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        flash(level, to);
        portalSound(level, to, 0.6F, 1.5F);
        return true;
    }

    /** One end of a teleport: the portal opening, with the Space Stone's voice swelling underneath it. */
    private static void portalSound(ServerLevel level, Vec3 at, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, ModSounds.TESSERACT.get(), SoundSource.PLAYERS, volume, pitch);
        InfinityStone.SPACE.use(level, at, volume * InfinityStone.UNDER, pitch);
    }

    /** A rising spiral of Space Stone light round someone about to vanish. */
    private static void swirl(ServerLevel level, Vec3 at) {
        int[] colors = {DIM, MID, BRIGHT, CORE};
        for (int i = 0; i < 72; i++) {
            double t = i / 72.0;
            double angle = t * Mth.TWO_PI * 3;
            double radius = 1.1 - t * 0.5;
            double x = at.x + Math.cos(angle) * radius;
            double z = at.z + Math.sin(angle) * radius;
            // Moving along the spiral, so it turns.
            level.sendParticles(new EnergyParticleOptions(colors[i % colors.length], 1.3F), x, at.y + t * 2.2, z, 0,
                    -Math.sin(angle), 0.15, Math.cos(angle), 0.18);
        }
    }

    /** A blue flash where someone vanished or appeared. */
    private static void flash(ServerLevel level, Vec3 at) {
        level.sendParticles(new EnergyParticleOptions(MID, 2.0F), at.x, at.y + 1.0, at.z, 40, 0.35, 0.7, 0.35, 0.1);
        level.sendParticles(new EnergyParticleOptions(CORE, 2.6F), at.x, at.y + 1.0, at.z, 16, 0.15, 0.4, 0.15, 0.04);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.tesseract.desc").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.enhanced.tesseract.desc2").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("item.enhanced.tesseract.desc3").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}

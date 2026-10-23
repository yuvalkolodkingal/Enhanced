package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.ext.ItemExt;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.stones.Snap;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.stones.StoneUse;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Infinity Gauntlet. Its component {@code enhanced:gauntlet} ({@link GauntletContents}) makes it one of two things
 * (3.6, the plan's 2.1):
 *
 * <ul> <li><b>Thanos's Gauntlet</b> ({@code titan}: the item's default, so every Gauntlet of an older world, the reward
 * of the Thanos fight and "Choose a hero → Thanos" are it): exactly as before, hold right-click for three seconds with
 * no power to become Thanos. While it is held the five stones light in turn, each with its voice, from Power to Time;
 * when the hold is done a bolt of light (it hurts nobody) strikes where he stands and the item is used up. (DESIGN
 * section 15.) Its five stones are the Titan's own: none goes in or out.</li> <li><b>The Infinity Gauntlet</b>
 * (crafted, its sockets empty): real stones go in (a stone clicked onto it, or it onto a stone, in an inventory screen,
 * as with a bundle; anyone may put one in: it is a safe container) and the selected one comes out (right-click it with
 * an empty cursor; only for one who may hold that stone). One who may wield it ({@link StoneRules#mayWield}) uses the
 * selected stone (use, Sneak + use: its variant) through {@link StoneUse}; the Stone Wheel (J) chooses it. With all six
 * in, Sneak and hold use for three seconds: {@link Snap}. A tap of Sneak + use is still the selected stone's
 * variant.</li> </ul>
 */
public class InfinityGauntletItem extends AwakeningItem implements ItemExt.StopUsing {
    /** A crafted Gauntlet's use is held at most this long (a held power ends itself sooner). */
    private static final int HOLD = 72000;
    /** Whether each player's use of a crafted Gauntlet started as its variant (Sneak), until it stops. Server only. */
    private static final Map<UUID, Boolean> HELD_ALT = new HashMap<>();

    public InfinityGauntletItem(Properties properties) {
        super(properties);
    }

    /** What a Gauntlet holds (the Titan's, when the stack says nothing). */
    public static GauntletContents contents(ItemStack stack) {
        GauntletContents contents = stack.get(ModDataComponents.GAUNTLET.get());
        return contents == null ? GauntletContents.TITAN : contents;
    }

    /** Whether the stack is a crafted Gauntlet (not Thanos's). */
    public static boolean crafted(ItemStack stack) {
        return stack.is(ModItems.INFINITY_GAUNTLET.get()) && !contents(stack).titan();
    }

    /** A new crafted Gauntlet with no stones. */
    public static ItemStack empty() {
        ItemStack stack = new ItemStack(ModItems.INFINITY_GAUNTLET.get());
        stack.set(ModDataComponents.GAUNTLET.get(), GauntletContents.EMPTY);
        return stack;
    }

    @Override
    public Power power() {
        return Power.THANOS;
    }

    @Override
    protected Component alreadyPowered() {
        return Component.translatable("message.enhanced.thanos.already_powered");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GauntletContents contents = contents(stack);
        if (contents.titan()) {
            return super.use(level, player, hand);
        }
        StoneKind selected = selectedOrFirst(contents);
        if (selected == null) {
            tell(player, "message.enhanced.gauntlet.empty", ChatFormatting.GRAY);
            return InteractionResultHolder.pass(stack);
        }
        if (!StoneRules.mayWield(player)) {
            tell(player, "message.enhanced.gauntlet.not_yours", ChatFormatting.LIGHT_PURPLE);
            return InteractionResultHolder.consume(stack);
        }
        boolean alt = player.isShiftKeyDown();
        if (player instanceof ServerPlayer server) {
            if (selected != contents.selectedStone()) {
                stack.set(ModDataComponents.GAUNTLET.get(), contents.withSelected(selected));
            }
            if (alt && Snap.mayCharge(server, contents)) {
                // A tap is still the variant (on release); a hold of three seconds is the Snap.
                Snap.begin(server);
            } else if (StoneUse.use(server, selected, StoneUse.Source.GAUNTLET, alt, true)) {
                HELD_ALT.put(server.getUUID(), alt);
            } else {
                return InteractionResultHolder.pass(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** The selected stone, or the first stone in it when the selected socket is empty (null: none at all). */
    @Nullable
    public static StoneKind selectedOrFirst(GauntletContents contents) {
        StoneKind selected = contents.selectedStone();
        if (selected != null) {
            return selected;
        }
        for (StoneKind kind : StoneKind.values()) {
            if (contents.has(kind)) {
                return kind;
            }
        }
        return null;
    }

    private static void tell(Player player, String key, ChatFormatting color) {
        if (player instanceof ServerPlayer server) {
            server.displayClientMessage(Component.translatable(key).withStyle(color), true);
        }
    }

    /** Three seconds by default; {@code gauntletRitualTicks} in the {@code [thanos_boss]} config. */
    @Override
    public int holdTicks() {
        return ThanosBossConfig.GAUNTLET_RITUAL_TICKS.get();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return contents(stack).titan() ? holdTicks() : HOLD;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return contents(stack).titan() ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return contents(stack).titan() ? super.finishUsingItem(stack, level, entity) : stack;
    }

    /**
     * The Titan's ceremony: the five stones light one after the other over the length of the hold, each with its voice
     * and a little of its colour round the left hand. A crafted Gauntlet: the Snap's charge.
     */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!contents(stack).titan()) {
            if (Snap.charging(player) && Snap.chargeTick(player, getUseDuration(stack, entity) - remaining)) {
                player.stopUsingItem();
            }
            return;
        }
        int hold = Math.max(1, holdTicks());
        int used = hold - remaining;
        int step = Math.max(1, hold / StoneKind.granted().size());
        if (used % step != 0 || used / step >= StoneKind.granted().size()) {
            return;
        }
        StoneKind stone = StoneKind.nth(used / step + 1);
        Vec3 hand = player.position().add(0.0, player.getBbHeight() * 0.7, 0.0);
        stone.voice().use(level, hand, 0.7F, 1.0F);
        ((ServerLevel) level).sendParticles(new EnergyParticleOptions(stone.color(), 1.3F), hand.x, hand.y, hand.z, 14,
                0.3, 0.3, 0.3, 0.06);
    }

    /**
     * The use of a crafted Gauntlet stopped (let go, another item, death): a Snap's charge let go at once is the
     * selected stone's variant (a tap), later it is nothing; a stone's held power is told the key came up.
     */
    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (!(entity instanceof ServerPlayer player) || contents(stack).titan()) {
            return;
        }
        StoneKind selected = selectedOrFirst(contents(stack));
        if (Snap.charging(player)) {
            Snap.end(player);
            int used = getUseDuration(stack, entity) - count;
            if (used < Snap.TAP_TICKS && selected != null && StoneRules.mayWield(player)) {
                StoneUse.use(player, selected, StoneUse.Source.GAUNTLET, true, true);
                StoneUse.use(player, selected, StoneUse.Source.GAUNTLET, true, false);
            }
            return;
        }
        Boolean alt = HELD_ALT.remove(player.getUUID());
        if (alt != null && selected != null) {
            StoneUse.use(player, selected, StoneUse.Source.GAUNTLET, alt, false);
        }
    }

    /** The moment of the awakening: a bolt of light, then the power (the same as a hero whom Mjolnir finds worthy). */
    @Override
    protected void awaken(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        super.awaken(player);
    }

    // ---- stones in and out (in an inventory screen, as with a bundle) ----

    /** The Gauntlet on the cursor, clicked on a slot that holds a stone: the stone goes in. */
    @Override
    public boolean overrideStackedOnOther(ItemStack gauntlet, Slot slot, ClickAction action, Player player) {
        ItemStack other = slot.getItem();
        if (gauntlet.getCount() != 1 || !(other.getItem() instanceof InfinityStoneItem)) {
            return false;
        }
        if (!slot.allowModification(player)) {
            return false;
        }
        ItemStack stone = insert(gauntlet, other, player);
        if (stone == null) {
            return contents(gauntlet).titan();
        }
        slot.set(stone);
        return true;
    }

    /**
     * Something on the cursor clicked on the Gauntlet in a slot: a stone goes in; an empty cursor with the right button
     * takes the selected stone out (for one who may hold it).
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack gauntlet, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess access) {
        if (gauntlet.getCount() != 1 || !slot.allowModification(player)) {
            return false;
        }
        if (other.getItem() instanceof InfinityStoneItem) {
            ItemStack left = insert(gauntlet, other, player);
            if (left == null) {
                return contents(gauntlet).titan();
            }
            access.set(left);
            return true;
        }
        if (!other.isEmpty() || action != ClickAction.SECONDARY) {
            return false;
        }
        GauntletContents contents = contents(gauntlet);
        if (contents.titan()) {
            tell(player, "message.enhanced.gauntlet.titans_own", ChatFormatting.GOLD);
            return true;
        }
        StoneKind selected = selectedOrFirst(contents);
        if (selected == null) {
            return false;
        }
        ItemStack out = takeOut(gauntlet, selected, player);
        if (out.isEmpty()) {
            return true;
        }
        access.set(out);
        return true;
    }

    /**
     * Puts the stone {@code stone} into the Gauntlet.
     *
     * @return what is left where the stone was (empty), or null when it does not go in (Thanos's Gauntlet, said so; a
     * socket already full: the click does what it always did)
     */
    @Nullable
    public static ItemStack insert(ItemStack gauntlet, ItemStack stone, Player player) {
        if (!(stone.getItem() instanceof InfinityStoneItem item) || stone.getCount() != 1) {
            return null;
        }
        GauntletContents contents = contents(gauntlet);
        if (contents.titan()) {
            tell(player, "message.enhanced.gauntlet.titans_own", ChatFormatting.GOLD);
            return null;
        }
        if (contents.has(item.kind)) {
            return null;
        }
        GauntletContents next = contents.with(item.kind);
        if (contents.selectedStone() == null) {
            next = next.withSelected(item.kind);
        }
        gauntlet.set(ModDataComponents.GAUNTLET.get(), next);
        StoneLedger.putInGauntletOf(gauntlet, item.kind, StoneLedger.taker(stone));
        player.playSound(ModSounds.GAUNTLET_INSERT.get(), 0.8F, 1.0F);
        return ItemStack.EMPTY;
    }

    /**
     * Takes the stone {@code kind} out of the Gauntlet for {@code player} (who must be allowed to hold it: else he is
     * told, and nothing moves).
     *
     * @return the stone (with its taker's name), or empty
     */
    public static ItemStack takeOut(ItemStack gauntlet, StoneKind kind, Player player) {
        GauntletContents contents = contents(gauntlet);
        if (contents.titan() || !contents.has(kind)) {
            return ItemStack.EMPTY;
        }
        if (!StoneRules.mayHold(player, kind)) {
            tell(player, "message.enhanced.gauntlet.stays", ChatFormatting.LIGHT_PURPLE);
            return ItemStack.EMPTY;
        }
        ItemStack stone = new ItemStack(ModItems.stone(kind));
        UUID taker = StoneLedger.gauntletTakerOf(gauntlet, kind);
        if (taker != null) {
            StoneLedger.stamp(stone, taker);
        }
        StoneLedger.putInGauntletOf(gauntlet, kind, null);
        GauntletContents left = contents.without(kind);
        StoneKind next = selectedOrFirst(left);
        gauntlet.set(ModDataComponents.GAUNTLET.get(), next == null ? left : left.withSelected(next));
        player.playSound(ModSounds.GAUNTLET_INSERT.get(), 0.8F, 0.8F);
        return stone;
    }

    // ---- the tooltip ----

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GauntletContents contents = contents(stack);
        if (contents.titan()) {
            tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.titan").withStyle(ChatFormatting.GOLD,
                    ChatFormatting.BOLD));
            tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.desc")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.desc2").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.desc3").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (contents.count() == 0) {
            tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.empty").withStyle(ChatFormatting.GRAY));
        } else {
            StoneKind selected = selectedOrFirst(contents);
            for (StoneKind kind : StoneKind.values()) {
                if (contents.has(kind)) {
                    MutableComponent line = Component.literal(kind == selected ? "▶ " : "  ")
                            .append(kind.displayName()).withStyle(s -> s.withColor(kind.color()));
                    tooltip.add(line);
                }
            }
        }
        tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.insert")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.wheel")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(contents.full() ? "item.enhanced.infinity_gauntlet.snap"
                : "item.enhanced.infinity_gauntlet.wield").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Thanos's Gauntlet glints as before; a crafted one does not (its sockets glow by themselves). */
    @Override
    public boolean isFoil(ItemStack stack) {
        return contents(stack).titan();
    }
}

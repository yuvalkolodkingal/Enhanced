package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.ext.ItemExt;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.stones.StoneUse;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One of the six Infinity Stones as an item (3.6): {@code enhanced:space_stone}, {@code mind_stone}, {@code
 * reality_stone}, {@code power_stone}, {@code time_stone}, {@code soul_stone}, keyed by {@link StoneKind} (its colour
 * and voice). Stacks to one, epic, fire-proof; on the ground it never despawns, nothing hurts it (fire, lava,
 * explosions, cactus) and it glints in its colour. Who may hold it is {@link StoneRules}; what it does to one who may
 * not is {@code StoneTouch} (it slips away, it burns after a grace).
 *
 * <p>Use (Sneak + use: its variant) goes to {@link StoneUse} (lane L2's powers): the press when the use starts, the
 * release when it stops (a held power, such as a beam, runs between the two). One who may hold the stone but not use it
 * (Thor and the Time Stone) is told so and nothing happens.
 */
public class InfinityStoneItem extends Item implements ItemExt.StopUsing, ItemExt.EntityItemUpdate {
    /** How often (ticks) a stone on the ground glints. */
    private static final int GLINT_EVERY = 8;
    /** A use is held at most this long (a held power ends itself sooner). */
    private static final int HOLD = 72000;
    /** Whether each player's use of a bare stone started as its variant (Sneak), until it stops. Server only. */
    private static final Map<UUID, Boolean> HELD_ALT = new HashMap<>();

    /** The stone this item is. */
    public final StoneKind kind;

    public InfinityStoneItem(StoneKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    /** The Time Stone taken back out of the Eye of Agamotto keeps who took it from its site (3.6, integration). */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (kind == StoneKind.TIME) {
            StoneLedger.carryTaker(stack, player, s -> s.is(ModItems.EYE_OF_AGAMOTTO.get()));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!StoneRules.mayHold(player, kind)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!StoneRules.mayUse(player, kind)) {
            if (player instanceof ServerPlayer server) {
                server.displayClientMessage(Component.translatable("message.enhanced.stones.quiet", kind.displayName())
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (player instanceof ServerPlayer server) {
            boolean alt = player.isShiftKeyDown();
            if (!StoneUse.use(server, kind, StoneUse.Source.BARE, alt, true)) {
                return InteractionResultHolder.pass(stack);
            }
            HELD_ALT.put(server.getUUID(), alt);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** The use stopped (let go, another item, death): the stone's power is told the key came up. */
    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity instanceof ServerPlayer player) {
            Boolean alt = HELD_ALT.remove(player.getUUID());
            if (alt != null) {
                StoneUse.use(player, kind, StoneUse.Source.BARE, alt, false);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return HOLD;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** On the ground: it never despawns, and it glints in its colour. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (!level.isClientSide()) {
            if (entity.getAge() != -32768) {
                entity.setUnlimitedLifetime();
            }
        } else if (entity.tickCount % GLINT_EVERY == 0) {
            level.addParticle(new EnergyParticleOptions(kind.color(), 0.6F), entity.getX(), entity.getY() + 0.3,
                    entity.getZ(), (level.random.nextDouble() - 0.5) * 0.02, 0.03, (level.random.nextDouble() - 0.5)
                            * 0.02);
        }
        return false;
    }

    /** Nothing in the world destroys a stone: not fire, lava, an explosion or a cactus. */
    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "item.enhanced." + kind.id() + "_stone";
        tooltip.add(Component.translatable(key + ".desc").withStyle(s -> s.withColor(kind.color())));
        tooltip.add(Component.translatable(key + ".uses").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(key + ".desc2").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** No enchantment glint: the gem glows by itself (its model is full bright), and the glint would tint it. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}

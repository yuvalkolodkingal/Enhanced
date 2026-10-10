package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Does nothing on its own. Hold it in one hand while a powerless player uses Loki's Scepter (touching the Mind Stone)
 * or the Tesseract in the other to awaken as the Scarlet Witch, or while opening a Witches' Road door to become Billy
 * or Tommy.
 */
public class ScarletWitchGeneItem extends Item {
    public ScarletWitchGeneItem(Properties properties) {
        super(properties);
    }

    /**
     * The Scarlet Witch's awakening, the same for everything that can wake her (Loki's Scepter, the Tesseract): the
     * gene fuses with the stone and is used up, lightning strikes (for show), the player awakens as the Scarlet Witch
     * (title, burst, sound, a message to the whole server) and Wanda's Crown appears in their inventory.
     *
     * @param gene the gene stack the player holds (one is used up, unless they are in Creative)
     */
    public static void awaken(ServerPlayer player, ItemStack gene) {
        ServerLevel level = player.serverLevel();
        gene.consume(1, player);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Powers.awaken(player, Power.SCARLET_WITCH);
        ItemStack crown = new ItemStack(ModItems.WANDA_CROWN.get());
        if (!player.getInventory().add(crown)) {
            player.drop(crown, false);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.scarlet_witch_gene.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.scarlet_witch_gene.desc2").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.enhanced.scarlet_witch_gene.desc3").withStyle(ChatFormatting.AQUA));
    }
}

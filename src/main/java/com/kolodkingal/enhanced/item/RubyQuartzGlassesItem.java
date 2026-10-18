package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Scott Summers's ruby-quartz glasses (3.3), found in the Mutant School. Worn in the head slot (a helmet, so a right-click
 * puts them on), they make whoever first puts them on without other powers Cyclops, and they are what holds his eyes back:
 * while he wears them he fires a controlled optic blast; when they come off, the giant beam pours out until they are back on
 * (see {@code CyclopsGlasses}). On his head they are drawn as the visor of his look, on anyone else's as red sunglasses, by
 * their own layer (the armour texture of their material is empty).
 */
public class RubyQuartzGlassesItem extends ArmorItem {
    /**
     * The look of his two beams, registered here so that the server and every client know them as soon as the item is made:
     * the controlled optic blast, ruby red with a hot pink-white core, and the giant beam, a deeper red with a wide dark
     * fringe. Both leave from his eyes (the clients draw them straight, not as the jagged bolt of the magic beams).
     */
    public static final BeamStyle OPTIC = BeamStyles.register(new BeamStyle("cyclops_optic", "beam.enhanced.cyclops_optic",
            0xFFF4EE, 0xFFB3A8, 0xFF3B46, 0xC8102A, 0x5A0712, false));
    public static final BeamStyle UNLEASHED = BeamStyles.register(new BeamStyle("cyclops_unleashed", "beam.enhanced.cyclops_unleashed",
            0xFFEDE4, 0xFF9C8A, 0xFF2A30, 0xB0081C, 0x48040C, false));

    public RubyQuartzGlassesItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.ruby_quartz_glasses.desc").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.enhanced.ruby_quartz_glasses.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}

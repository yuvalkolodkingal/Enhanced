package com.kolodkingal.enhanced.road.copy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Floating text and ghost blocks of the Copy the Structure trial, made with the game's display entities (no new entity type, no
 * client code): the texts say what the Check and Hint buttons are and show the result over the plot; a ghost is the real block, as it
 * has to be, drawn with a glowing outline.
 */
final class CopyDisplays {

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float value : values) {
            list.add(FloatTag.valueOf(value));
        }
        return list;
    }

    private static CompoundTag transformation(float scale, float translation) {
        CompoundTag tag = new CompoundTag();
        tag.put("left_rotation", floats(0, 0, 0, 1));
        tag.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("scale", floats(scale, scale, scale));
        tag.put("translation", floats(translation, translation, translation));
        return tag;
    }

    /** The saved form of a text that always faces the viewer. */
    static CompoundTag textTag(ServerLevel level, Component text, float scale) {
        CompoundTag tag = new CompoundTag();
        tag.putString("text", Component.Serializer.toJson(text, level.registryAccess()));
        tag.putString("billboard", "center");
        tag.putInt("background", 0x80102028);
        tag.putInt("line_width", 260);
        tag.putString("alignment", "center");
        tag.putBoolean("shadow", true);
        tag.put("transformation", transformation(scale, 0.0F));
        tag.putFloat("view_range", 2.0F);
        return tag;
    }

    /** A floating text at a place (the middle of the text). */
    static Display.TextDisplay text(ServerLevel level, double x, double y, double z, Component text, float scale) {
        Display.TextDisplay display = EntityType.TEXT_DISPLAY.create(level);
        display.load(textTag(level, text, scale));
        display.moveTo(x, y, z);
        level.addFreshEntity(display);
        return display;
    }

    /** Changes what a floating text says. */
    static void retext(ServerLevel level, Display.TextDisplay display, Component text, float scale) {
        double x = display.getX();
        double y = display.getY();
        double z = display.getZ();
        display.load(textTag(level, text, scale));
        display.moveTo(x, y, z);
    }

    /**
     * A ghost of a block: the block as it should be, a little smaller than the cell (or a little bigger when something else is there
     * already, so that it shows round it), outlined in a glow.
     */
    static Display.BlockDisplay ghost(ServerLevel level, BlockPos pos, BlockState state, boolean around, int glow) {
        Display.BlockDisplay display = EntityType.BLOCK_DISPLAY.create(level);
        CompoundTag tag = new CompoundTag();
        tag.put("block_state", NbtUtils.writeBlockState(state));
        float scale = around ? 1.06F : 0.86F;
        tag.put("transformation", transformation(scale, (1.0F - scale) / 2.0F));
        CompoundTag light = new CompoundTag();
        light.putInt("block", 15);
        light.putInt("sky", 15);
        tag.put("brightness", light);
        tag.putInt("glow_color_override", glow);
        tag.putBoolean("Glowing", true);
        tag.putFloat("view_range", 2.0F);
        display.load(tag);
        display.moveTo(pos.getX(), pos.getY(), pos.getZ());
        level.addFreshEntity(display);
        return display;
    }

    private CopyDisplays() {
    }
}

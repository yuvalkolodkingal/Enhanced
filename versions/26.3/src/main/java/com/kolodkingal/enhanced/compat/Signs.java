package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;

import java.lang.reflect.Field;

/**
 * 26.3 only: a sign written while its chunk is generated. 26.3's {@code setText} and {@code setWaxed} always tell the
 * level the block changed, and a block entity of a chunk still being made has no level yet (a crash in "Feature
 * placement"). Without a level the text and the wax are set as loading them would set them.
 */
public final class Signs {
    private Signs() {
    }

    public static void front(SignBlockEntity sign, SignText text) {
        if (sign.getLevel() != null) {
            sign.setText(text, SignTextSlot.FRONT);
        } else {
            set(sign, "frontText", text);
        }
    }

    public static void waxed(SignBlockEntity sign) {
        if (sign.getLevel() != null) {
            sign.setWaxed(true);
        } else {
            set(sign, "isWaxed", true);
        }
    }

    private static void set(SignBlockEntity sign, String name, Object value) {
        try {
            Field field = SignBlockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(sign, value);
        } catch (ReflectiveOperationException e) {
            com.kolodkingal.enhanced.Enhanced.LOGGER.warn("A generated sign could not be written ({})", name, e);
        }
    }
}

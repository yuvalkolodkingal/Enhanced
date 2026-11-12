package com.kolodkingal.enhanced.power;

import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/**
 * Looks that are earned, not chosen (3.12): Vision's Source Code (from the Human Torch, in Vision's mind) and Ultron's
 * nanotech body (from the Horton manor's storage). The Change Look key skips them until the player has earned them;
 * once earned they stay, with the player's own data (it survives death).
 */
public final class EarnedLooks {
    private static final String TAG = "enhanced_earned_looks";
    private static final Set<String> EARNED = Set.of("vision:source_code", "ultron:nano", "ultron:nano_human");

    private EarnedLooks() {
    }

    private static String key(Variant variant) {
        return variant.power().getSerializedName() + ":" + variant.id();
    }

    /** Whether this look must be earned. */
    public static boolean earnable(Variant variant) {
        return EARNED.contains(key(variant));
    }

    /** Whether the player may wear the look (always, for one that is not earned). */
    public static boolean has(Player player, Variant variant) {
        if (!earnable(variant)) {
            return true;
        }
        ListTag list = ModAttachments.PERSISTENT_DATA.get(player).getList(TAG, Tag.TAG_STRING);
        String key = key(variant);
        for (int i = 0; i < list.size(); i++) {
            if (key.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    /** The player earns the look (no effect if already). Returns whether it is new. */
    public static boolean earn(Player player, Variant variant) {
        if (has(player, variant)) {
            return false;
        }
        CompoundTag data = ModAttachments.PERSISTENT_DATA.get(player);
        ListTag list = data.getList(TAG, Tag.TAG_STRING);
        list.add(StringTag.valueOf(key(variant)));
        data.put(TAG, list);
        return true;
    }
}

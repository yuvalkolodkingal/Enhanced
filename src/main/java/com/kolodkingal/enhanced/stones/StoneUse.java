package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.ThanosAbilities;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The single entry of a stone's use (3.6): the bare stone in a hand, the selected stone of a Gauntlet in a hand, a
 * vessel (Loki's Scepter, the Tesseract, the Eye of Agamotto) or a custom hero's stone row all come here. The 3.6
 * skeleton writes the entry and the questions; lane L2 writes the body ({@link #use}) and every stone's power.
 */
public final class StoneUse {
    /** Where the stone that is used is. */
    public enum Source {
        /** The bare stone, in a hand. */
        BARE,
        /** The selected stone of a filled Gauntlet in a hand. */
        GAUNTLET,
        /** Its vessel: Loki's Scepter (Mind), the Tesseract (Space), the Eye of Agamotto (Time). */
        VESSEL,
        /** A custom hero's stone row ({@code StoneRowKeys}). */
        ROW
    }

    /**
     * The player uses the stone ({@code alt}: its Sneak use; {@code down}: the key or the use went down, or came up for
     * a held power).
     *
     * @return whether a stone power took the use (it acted, or refused and said why); false when there is none (the
     * skeleton: the caller then does what it did before 3.6, so the Scepter's and the Tesseract's old rows are
     * unchanged until lane L2)
     */
    public static boolean use(ServerPlayer player, StoneKind kind, Source source, boolean alt, boolean down) {
        return false;
    }

    /**
     * Whether a restrained hand cannot use a stone: held by Billy's spell, petrified, in the Soul World, time-locked,
     * pinned by Mjolnir (Thanos's rule).
     */
    public static boolean restrained(LivingEntity entity) {
        return ThanosAbilities.held(entity) || entity.hasEffect(ModEffects.TIME_LOCKED) || MjolnirPin.isPinned(entity);
    }

    /**
     * Whether the player carries the stone and may use it (a stone row's condition): the bare stone in his inventory,
     * in a filled Gauntlet he may wield, or its vessel (Loki's Scepter for the Mind Stone, the Tesseract for the Space
     * Stone, the Eye of Agamotto for the Time Stone, which only a sorcerer uses).
     */
    public static boolean carried(Player player, StoneKind kind) {
        if (!StoneRules.mayUse(player, kind) && vessel(kind) == null) {
            return false;
        }
        Item vessel = vessel(kind);
        for (ItemStack stack : player.getInventory().items) {
            if (carries(player, stack, kind, vessel)) {
                return true;
            }
        }
        return carries(player, player.getOffhandItem(), kind, vessel);
    }

    private static boolean carries(Player player, ItemStack stack, StoneKind kind, @Nullable Item vessel) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ModItems.stone(kind))) {
            return StoneRules.mayUse(player, kind);
        }
        if (vessel != null && stack.is(vessel)) {
            return kind != StoneKind.TIME || StoneRules.mayUse(player, kind);
        }
        GauntletContents gauntlet = stack.get(ModDataComponents.GAUNTLET.get());
        return stack.is(ModItems.INFINITY_GAUNTLET.get()) && gauntlet != null && !gauntlet.titan() && gauntlet.has(kind)
                && StoneRules.mayWield(player);
    }

    /**
     * The vessel of a stone (the item that holds it and lets anyone use it), or null (Reality, Power, Soul have none).
     */
    @Nullable
    public static Item vessel(StoneKind kind) {
        return switch (kind) {
            case MIND -> ModItems.LOKIS_SCEPTER.get();
            case SPACE -> ModItems.TESSERACT.get();
            case TIME -> ModItems.EYE_OF_AGAMOTTO.get();
            default -> null;
        };
    }

    private StoneUse() {
    }
}

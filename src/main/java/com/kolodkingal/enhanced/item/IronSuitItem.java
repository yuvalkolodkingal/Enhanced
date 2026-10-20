package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.ironman.SuitKind;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/**
 * One of Iron Man's suits (3.8, PLAN 5.3): a classic suit is one chestplate item that is the whole suit (its armour on
 * the chest slot; its armour texture empty: {@code SuitLayer} draws it); the Mark 5 is its suitcase in the hand ({@code
 * enhanced:suit} folded); the Mark 50's and Mark 85's are nanotech housings ({@link #nanotech}) carried in the
 * inventory. Registered by the 3.8 skeleton as plain chest items; lane L3 owns the class (who may wear it, the
 * suitcase, the wear), lane L4 the housings' rebuilding.
 */
public class IronSuitItem extends ArmorItem {
    private final SuitKind kind;

    public IronSuitItem(SuitKind kind, Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
        this.kind = kind;
    }

    /** Which suit it is. */
    public SuitKind kind() {
        return kind;
    }

    /** A nanotech housing (Mark 50, Mark 85), not a suit worn as an item. */
    public boolean nanotech() {
        return kind.nanotech();
    }
}

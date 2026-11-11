package com.kolodkingal.enhanced.compat;

import net.minecraft.network.chat.Component;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 1.21.5: an item's tooltip lines go to a consumer; the mod's items still add to a list, which hands each one on. */
public final class TipList extends AbstractList<Component> {
    private final Consumer<Component> out;
    private final List<Component> seen = new ArrayList<>();

    private TipList(Consumer<Component> out) {
        this.out = out;
    }

    public static List<Component> of(Consumer<Component> out) {
        return new TipList(out);
    }

    @Override
    public boolean add(Component line) {
        seen.add(line);
        out.accept(line);
        return true;
    }

    @Override
    public Component get(int index) {
        return seen.get(index);
    }

    @Override
    public int size() {
        return seen.size();
    }
}

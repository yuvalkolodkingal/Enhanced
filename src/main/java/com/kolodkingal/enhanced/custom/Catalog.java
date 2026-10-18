package com.kolodkingal.enhanced.custom;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Hero Creator's whole catalogue (3.4): the thirty generic {@link PowerModule}s, then every built-in hero's
 * {@link HeroAbility}. The one list the rules, the editor and the HUD read.
 */
public final class Catalog {
    private static final List<CatalogEntry> ALL;

    static {
        List<CatalogEntry> all = new ArrayList<>(List.of(PowerModule.values()));
        all.addAll(List.of(HeroAbility.values()));
        ALL = Collections.unmodifiableList(all);
    }

    /** Every entry, generic modules first, then the heroes' abilities in the order of the heroes. */
    public static List<CatalogEntry> all() {
        return ALL;
    }

    /** The entry of this id (a module's {@code flight} or a hero's {@code thor.lightning}, any case), or null. */
    @Nullable
    public static CatalogEntry byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        PowerModule module = PowerModule.byId(id);
        return module != null ? module : HeroAbility.byId(id);
    }

    /** The entries of one family, in catalogue order. */
    public static List<CatalogEntry> family(CatalogEntry.Family family) {
        List<CatalogEntry> list = new ArrayList<>();
        for (CatalogEntry e : ALL) {
            if (e.family() == family) {
                list.add(e);
            }
        }
        return list;
    }

    private Catalog() {
    }
}

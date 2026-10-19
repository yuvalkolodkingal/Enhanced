package com.kolodkingal.enhanced.client.hydra;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hydra.HydraSoldierEntity;
import com.kolodkingal.enhanced.hydra.Rank;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What each Hydra soldier looks like (3.6, the plan's 15.8): his skin and the pieces of {@link HydraGearModel} he
 * wears. Riflemen: a black helmet with a dark visor (0), a balaclava and goggles (1), a cap with the red band (2), bare
 * head with a headset (3); all with the vest (the emblem on the chest), pouches, belt, radio and the energy rifle.
 * Enforcers: the riot helmet (with its face shield, or without), riot armour, shoulder pads, the riot shield with the
 * emblem and the stun baton. The sniper: a cap, the vest, the long scoped rifle. The officer: a black field jacket with
 * red shoulder boards (his skin), a headset, the pistol; grey hair, no helmet.
 */
public final class HydraLooks {
    private static final List<String> KIT = List.of("vest", "pouch_r", "pouch_m", "pouch_l", "belt", "radio");
    private static final List<String> RIFLE = List.of("rifle", "rifle_barrel", "rifle_mag", "rifle_sight",
            "rifle_cell");
    private static final List<String> HEADSET = List.of("headset", "headset_ear_r", "headset_ear_l", "headset_mic");
    private static final List<String> RIOT = List.of("riot_helmet", "riot_chest", "belt", "pad_r", "pad_l", "shield",
            "baton", "baton_tip");

    /** Every piece of the gear that a look may show or hide. */
    public static final List<String> ALL_PIECES = List.of("helmet", "visor", "cap", "cap_brim", "headset",
            "headset_ear_r", "headset_ear_l", "headset_mic", "goggles", "riot_helmet", "riot_shield", "eyes",
            "eyes_visor", "vest", "pouch_r", "pouch_m", "pouch_l", "belt", "radio", "riot_chest", "pad_r", "pad_l",
            "shield", "rifle", "rifle_barrel", "rifle_mag", "rifle_sight", "rifle_cell", "long_rifle", "long_barrel",
            "long_scope", "long_cell", "baton", "baton_tip", "pistol", "pistol_cell");

    /** The skin of a soldier. */
    public static ResourceLocation skin(HydraSoldierEntity soldier) {
        String name = soldier.isOfficer() ? "officer_" + Math.floorMod(soldier.variant(), 2)
                : soldier.rank().id() + "_" + Math.floorMod(soldier.variant(), soldier.rank().skins());
        return Enhanced.id("textures/entity/hydra/" + name + ".png");
    }

    /** Whether his eyes are behind a visor (the turned blue eyes are drawn on it). */
    public static boolean visored(HydraSoldierEntity soldier) {
        return !soldier.isOfficer() && (soldier.rank() == Rank.ENFORCER && soldier.variant() == 0
                || soldier.rank() == Rank.RIFLEMAN && soldier.variant() == 0);
    }

    /** The pieces he wears (without the eyes). */
    public static Set<String> pieces(HydraSoldierEntity soldier) {
        List<String> out = new ArrayList<>();
        if (soldier.isOfficer()) {
            out.addAll(HEADSET);
            out.add("belt");
            out.add("pistol");
            out.add("pistol_cell");
            return Set.copyOf(out);
        }
        switch (soldier.rank()) {
            case ENFORCER -> {
                out.addAll(RIOT);
                if (soldier.variant() == 0) {
                    out.add("riot_shield");
                }
            }
            case SNIPER -> out.addAll(List.of("cap", "cap_brim", "vest", "belt", "long_rifle", "long_barrel",
                    "long_scope", "long_cell"));
            default -> {
                out.addAll(KIT);
                out.addAll(RIFLE);
                switch (Math.floorMod(soldier.variant(), 4)) {
                    case 0 -> out.addAll(List.of("helmet", "visor"));
                    case 1 -> out.add("goggles");
                    case 2 -> out.addAll(List.of("cap", "cap_brim"));
                    default -> out.addAll(HEADSET);
                }
            }
        }
        return Set.copyOf(out);
    }

    /** The glowing pieces he wears (energy cells, the baton's tip), and his eyes while he is turned. */
    public static Set<String> glowing(HydraSoldierEntity soldier) {
        List<String> out = new ArrayList<>();
        for (String piece : pieces(soldier)) {
            if (piece.endsWith("_cell") || piece.equals("baton_tip")) {
                out.add(piece);
            }
        }
        if (soldier.isTurned()) {
            out.add(visored(soldier) ? "eyes_visor" : "eyes");
        }
        return Set.copyOf(out);
    }

    private HydraLooks() {
    }
}

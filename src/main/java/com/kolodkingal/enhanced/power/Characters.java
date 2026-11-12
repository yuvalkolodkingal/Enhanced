package com.kolodkingal.enhanced.power;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The characters made from another hero's powers (3.12.2): Source Code Vision (Vision once he has taken in the Source
 * Code) and Nano Ultron (Ultron in the Horton nanotech). Each is a hero of its own: its own name, and only its own
 * looks, which Change Look steps through (Nano Ultron: the nanotech body and the human form; Source Code Vision has one,
 * and his human disguise). The hero it is made from never steps into them: one becomes them at the Source Code and
 * the nanotech storage, and leaves them only by becoming another hero.
 */
public final class Characters {
    public static final String SOURCE_CODE_VISION = "source_code_vision";
    public static final String NANO_ULTRON = "nano_ultron";

    private record Made(Power power, String id, List<String> looks) {
    }

    private static final List<Made> MADE = List.of(
            new Made(Power.VISION, SOURCE_CODE_VISION, List.of("source_code")),
            new Made(Power.ULTRON, NANO_ULTRON, List.of("nano", "nano_human")));

    private Characters() {
    }

    /** The character this hero is, or null for the hero itself. */
    @Nullable
    public static String of(PowerData data) {
        for (Made made : MADE) {
            if (data.power() == made.power && made.looks.contains(data.variant())) {
                return made.id;
            }
        }
        return null;
    }

    public static boolean is(PowerData data, String character) {
        return character.equals(of(data));
    }

    /** The name the hero goes by: the character's, or the hero's own. */
    public static Component name(PowerData data) {
        String id = of(data);
        return id == null ? data.power().displayName() : Component.translatable("character.enhanced." + id);
    }

    /** The looks Change Look steps through: the character's own, or the hero's without those of its characters. */
    public static List<Variant> looks(PowerData data) {
        List<Variant> all = Variants.of(data.power());
        String id = of(data);
        List<Variant> out = new ArrayList<>();
        for (Variant v : all) {
            boolean characters = false;
            boolean mine = false;
            for (Made made : MADE) {
                if (made.power == data.power() && made.looks.contains(v.id())) {
                    characters = true;
                    mine = made.id.equals(id);
                }
            }
            if (id == null ? !characters : mine) {
                out.add(v);
            }
        }
        return out;
    }
}

package com.kolodkingal.enhanced.asgard.npc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What other lanes ask of the Asgardians (3.5). The 3.5 skeleton writes these calls so that Heimdall's Gatekeeper's
 * Call (L2), Odin's command and coronation (L3) and the others compile before lane L5, who owns this class, fills them;
 * until then nobody answers (no Einherjar come, nobody bows).
 */
public final class Asgardians {

    /** Summons {@code count} Einherjar for {@code owner} for {@code ticks} (they follow and defend him). Returns them. */
    public static List<Entity> summonEinherjar(ServerPlayer owner, int count, int ticks) {
        return List.of();
    }

    /**
     * The Allfather's command: the Einherjar and warriors near {@code owner} attack {@code target} (or follow him if
     * null) for {@code ticks}. Returns how many answered.
     */
    public static int command(ServerPlayer owner, @Nullable LivingEntity target, int ticks) {
        return 0;
    }

    /** The Asgardians who can see {@code player} turn to him and bow (the coronation). */
    public static void bowTo(ServerPlayer player) {
    }

    /** The named Asgardian of a role says one of his lines ({@code npc.enhanced.<role>.<lineKey>}) to those near him. */
    public static void say(ServerLevel level, Role role, String lineKey) {
    }

    /** The named Asgardian of a role in the level, or null. */
    @Nullable
    public static Entity fixture(ServerLevel level, Role role) {
        return null;
    }

    private Asgardians() {
    }
}

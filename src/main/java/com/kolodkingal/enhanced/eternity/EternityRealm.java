package com.kolodkingal.enhanced.eternity;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Eternity's realm, {@code enhanced:eternity} (3.8, PLAN 10.3-10.5): the way in from the shrine's altar, the way home,
 * and Eternity kept in it. The 3.8 skeleton writes the realm's key and points and refuses the journey ("not yet"); lane
 * L8 fills {@link #enter} and {@link #returnHome} (the return point on {@code ModAttachments.ETERNITY}, Eternity made
 * once, the wish screen) and keeps the signatures.
 */
public final class EternityRealm {
    /** The realm's level. */
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Enhanced.id("eternity"));
    /** Where a traveller arrives, facing north toward Eternity. */
    public static final Vec3 ARRIVAL = new Vec3(0.5, 64.0, 24.5);
    /** Where Eternity sits, facing south. */
    public static final BlockPos ETERNITY = new BlockPos(0, 64, -48);
    /** The wishing stone (a second altar) two blocks north of the arrival. */
    public static final BlockPos WISHING_STONE = BlockPos.containing(ARRIVAL).north(2);
    /** The mirror's layer: the realm's one layer of {@code eternity_mirror}. */
    public static final int MIRROR_Y = 63;

    /** Whether the player is in Eternity's realm. */
    public static boolean in(ServerPlayer player) {
        return player.level().dimension() == LEVEL;
    }

    /**
     * Takes the player to the realm from the altar at {@code altar} (his return point saved first). Skeleton: refused.
     *
     * @return whether he was taken
     */
    public static boolean enter(ServerPlayer player, BlockPos altar) {
        player.displayClientMessage(Component.translatable("message.enhanced.eternity.not_yet"), true);
        return false;
    }

    /**
     * The wish screen's answer ({@code EternityWishPayload}: an item, Love, or leave), every rule checked here, never
     * trusting the client. Skeleton: "not yet".
     */
    public static void wish(ServerPlayer player, byte kind, ResourceLocation item) {
        player.displayClientMessage(Component.translatable("message.enhanced.eternity.not_yet"), true);
    }

    /** Sends him home to where he came from (or the nearest safe spot, or his respawn point). Skeleton: nothing. */
    public static void returnHome(ServerPlayer player) {
    }

    private EternityRealm() {
    }
}

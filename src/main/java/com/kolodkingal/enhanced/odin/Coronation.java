package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.npc.Asgardians;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The coronation on the Throne of Asgard (3.5): a powerless player who sits on the throne holding his own Gungnir becomes
 * Odin. The throne is lane L1's block ({@code odin_throne}); it calls {@link #onSit} when a player sits down. Golden light
 * pours from the throne for {@link #TICKS} ticks while he stays on it with the spear in hand; then he is crowned: the title
 * "Odin" (the line "The Allfather takes his throne"), the flag {@code AsgardProgress.CROWNED}, and the Asgardians who can
 * see him turn and bow ({@link Asgardians#bowTo}). Rising, letting go of the spear or another power on him puts the light
 * out. Sitting on the throne otherwise is just sitting (Odin himself, awake and near, asks whether it is comfortable).
 */
public final class Coronation {
    /** How long the golden light pours before the crown (3 s). */
    public static final int TICKS = 60;
    /** How far from the throne a seated player's seat can be (the seat's offset, and a little more). */
    public static final double SEAT_REACH = 2.5;
    /**
     * How far from the throne a player who was not seated when he sat down may stand (a throne that takes a click for
     * sitting down, before the realm's seat exists): his reach.
     */
    public static final double CLICK_REACH = 5.0;
    /** How far Odin (the Asgardian) hears someone sit on his throne. */
    private static final double ODIN_HEARS = 16.0;

    /** The players being crowned: where the throne is, and when it began. */
    private static final Map<UUID, Crowning> CROWNINGS = new HashMap<>();

    /** {@code seated}: whether he sat on a seat (he must stay on it), or only clicked the throne (he must stay near). */
    private record Crowning(BlockPos throne, long start, boolean seated) {
    }

    /** What sitting down on the throne began. */
    public enum Sat {
        /** The golden light: he will be crowned if he stays. */
        CROWNING,
        /** He holds his own Gungnir, but another power is on him. */
        HAS_POWER,
        /** Just sitting. */
        SITTING
    }

    /** A player has sat down on the throne at {@code pos}. */
    public static void onSit(ServerPlayer player, BlockPos pos) {
        sit(player, pos);
    }

    /** The server's side of sitting down on the throne. */
    public static Sat sit(ServerPlayer player, BlockPos pos) {
        boolean spear = holdsOwnGungnir(player);
        Power power = Powers.power(player);
        if (spear && power == Power.NONE) {
            if (!CROWNINGS.containsKey(player.getUUID())) {
                ServerLevel level = player.serverLevel();
                CROWNINGS.put(player.getUUID(), new Crowning(pos.immutable(), level.getGameTime(), player.isPassenger()));
                Vec3 at = Vec3.atBottomCenterOf(pos);
                level.playSound(null, at.x, at.y, at.z, ModSounds.CORONATION.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
                OdinAbilities.send(level, player, OdinAbilities.FX_CORONATION, List.of(at), TICKS);
                player.displayClientMessage(Component.translatable("message.enhanced.odin.coronation_begins").withStyle(ChatFormatting.GOLD), true);
            }
            return Sat.CROWNING;
        }
        if (spear && power != Power.ODIN) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.coronation_power"), true);
            return Sat.HAS_POWER;
        }
        ServerLevel level = player.serverLevel();
        Entity odin = Asgardians.fixture(level, Role.ODIN);
        if (odin != null && odin.distanceTo(player) <= ODIN_HEARS && AsgardState.get(level.getServer()).odinAwake()) {
            Asgardians.say(level, Role.ODIN, "comfortable");
        }
        return Sat.SITTING;
    }

    /** Whether he holds a Gungnir of his own in either hand. */
    public static boolean holdsOwnGungnir(ServerPlayer player) {
        return GungnirItem.isOwnGungnir(player, player.getMainHandItem()) || GungnirItem.isOwnGungnir(player, player.getOffhandItem());
    }

    /** Whether the player is being crowned now. */
    public static boolean crowning(ServerPlayer player) {
        return CROWNINGS.containsKey(player.getUUID());
    }

    /** Every player, every tick: the light pours while he stays; at its end, the crown. */
    static void tick(ServerPlayer player, PowerData data) {
        Crowning crowning = CROWNINGS.get(player.getUUID());
        if (crowning == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 seat = Vec3.atBottomCenterOf(crowning.throne());
        boolean near = crowning.seated() ? player.getVehicle() != null && player.getVehicle().position().distanceTo(seat) <= SEAT_REACH
                : player.position().distanceTo(seat) <= CLICK_REACH;
        boolean stays = player.isAlive() && data.power() == Power.NONE && holdsOwnGungnir(player) && near;
        if (!stays) {
            CROWNINGS.remove(player.getUUID());
            if (data.power() != Power.ODIN) {
                player.displayClientMessage(Component.translatable("message.enhanced.odin.coronation_fades"), true);
            }
            return;
        }
        long t = level.getGameTime() - crowning.start();
        if (t % 4 == 0) {
            level.sendParticles(new EnergyParticleOptions(OdinAbilities.GOLD, 1.3F), seat.x, seat.y + 1.0, seat.z, 6, 0.6, 1.0, 0.6, 0.05);
        }
        if (t >= TICKS) {
            CROWNINGS.remove(player.getUUID());
            crown(player);
        }
    }

    /** He is Odin. */
    static void crown(ServerPlayer player) {
        Powers.awaken(player, Power.ODIN);
        ModAttachments.ASGARD.set(player, ModAttachments.ASGARD.get(player).with(AsgardProgress.CROWNED));
        ServerLevel level = player.serverLevel();
        OdinAbilities.send(level, player, OdinAbilities.FX_CROWNED, List.of(player.position()), 0);
        Asgardians.bowTo(player);
        if (com.kolodkingal.enhanced.asgard.AsgardDimension.is(level)) {
            // One of each character (3.5.1): the Allfather of the hall leaves in gold as the new one is crowned.
            com.kolodkingal.enhanced.asgard.npc.AsgardPopulation.yieldToHeroes(level);
        }
        player.displayClientMessage(Component.translatable("message.enhanced.odin.crowned").withStyle(ChatFormatting.GOLD), false);
    }

    /** He left, died or changed realm: no coronation goes on without him. */
    static void forget(ServerPlayer player) {
        CROWNINGS.remove(player.getUUID());
    }

    static void clear() {
        CROWNINGS.clear();
    }

    private Coronation() {
    }
}

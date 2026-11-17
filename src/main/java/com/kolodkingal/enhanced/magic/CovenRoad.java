package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.WitchcraftConfig;
import com.kolodkingal.enhanced.mind.MindRealm;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.road.WitchesRoad;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Coven's Road (3.17): the Witches' Road walked together. A Green Witch (or Billy, or the Demiurge) opens a portal to
 * the Road; everyone who steps into it while it is open walks the Road with the others: a winding path of the Road's
 * blue-green light over the void, lanterns along it, to the Road's End, a round hall with an amethyst dais. There each
 * walker, on the dais, chooses a prize of his own ({@link Prize}): to be unbound, to find a player, to bring back the dead
 * (a hardcore world's fallen), witchcraft of his own (someone with no power), or the Road's blessing. A prize he already
 * has is not offered. Then the Road sends him home. (The Road of the trials, {@link WitchesRoad}, is the one to walk
 * alone, with a Scarlet Witch Gene.)
 */
public final class CovenRoad {
    /** What the Road's End may give. */
    public enum Prize {
        /** Freed of every curse, mind control and the Darkhold's corruption, and none holds him again until he dies. */
        UNBOUND,
        /** Where a player is: the world and the place, and a trail of light to him if he is in this one. */
        FIND,
        /** A fallen player of a hardcore world rises again. */
        REVIVE,
        /** Witchcraft of his own: a path of the four (someone with no power and no craft). */
        WITCHCRAFT,
        /** The Road's blessing: strength, luck and health for a long while. */
        BLESSING;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Prize byId(String id) {
            for (Prize p : values()) {
                if (p.id().equals(id)) {
                    return p;
                }
            }
            return null;
        }
    }

    /** One walk of the Road: its place, who walks it, who has chosen. */
    private static final class Walk {
        final int slot;
        final Set<UUID> walkers = new LinkedHashSet<>();
        final Set<UUID> chosen = new HashSet<>();
        final Set<UUID> asked = new HashSet<>();

        Walk(int slot) {
            this.slot = slot;
        }
    }

    private static final Map<Integer, Walk> WALKS = new HashMap<>();
    private static final Map<UUID, Integer> WALKER_OF = new HashMap<>();
    private static int nextSlot;
    private static final int LENGTH = 36;

    private CovenRoad() {
    }

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    /** The start of walk {@code slot}'s road. */
    static BlockPos start(int slot) {
        return new BlockPos(slot * 256, MindRealm.GROUND, Realms.ROAD_Z);
    }

    /** The middle of the Road's End of walk {@code slot}. */
    static BlockPos end(int slot) {
        return start(slot).offset(0, 0, -LENGTH - 8);
    }

    public static boolean onRoad(ServerPlayer player) {
        return WALKER_OF.containsKey(player.getUUID()) && player.level().dimension() == MindRealm.LEVEL;
    }

    // ---- the portal ----

    /** Opens the portal to the Road at the caster's feet (the Green Witch's spell, Billy's). */
    static boolean openPortal(ServerPlayer caster, int color) {
        if (caster.level().dimension() == MindRealm.LEVEL) {
            caster.displayClientMessage(Component.translatable("message.enhanced.spell.mind_world").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel mind = MindRealm.level(caster.server);
        if (mind == null) {
            return false;
        }
        int slot = nextSlot++ % 64;
        Walk walk = new Walk(slot);
        WALKS.put(slot, walk);
        build(mind, slot);
        int seconds = loaded() ? WitchcraftConfig.ROAD_PORTAL_SECONDS.get() : 15;
        Vec3 look = caster.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 at = caster.position().add(look.scale(2.0));
        MagicZones.Zone zone = Portals.openAction(caster, at, look, seconds * 20, WitchesRoad.LIGHT, () -> {
        });
        zone.startDistance = slot; // (the walk this portal leads to)
        caster.serverLevel().playSound(null, at.x, at.y, at.z, com.kolodkingal.enhanced.registry.ModSounds.WITCHES_ROAD.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        for (ServerPlayer near : caster.serverLevel().players()) {
            if (near.distanceToSqr(caster) < 32 * 32) {
                near.displayClientMessage(Component.translatable("message.enhanced.road.coven_open", caster.getDisplayName(), seconds)
                        .withStyle(s -> s.withColor(WitchesRoad.LIGHT)), false);
            }
        }
        return true;
    }

    /** Someone steps into the portal: he walks the Road with the coven. */
    static void enterThrough(ServerPlayer player, MagicZones.Zone portal) {
        int slot = (int) portal.startDistance;
        Walk walk = WALKS.get(slot);
        if (walk == null || player.isSpectator() || WALKER_OF.containsKey(player.getUUID())) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(start(slot).above()).add((walk.walkers.size() % 3) - 1, 0, 0);
        if (!Realms.enter(player, at, 180.0F)) {
            return;
        }
        walk.walkers.add(player.getUUID());
        WALKER_OF.put(player.getUUID(), slot);
        player.displayClientMessage(Component.translatable("message.enhanced.road.coven_walk").withStyle(s -> s.withColor(WitchesRoad.LIGHT)), false);
        player.serverLevel().playSound(null, at.x, at.y, at.z, com.kolodkingal.enhanced.registry.ModSounds.ROAD_TRAVEL.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ---- the Road itself ----

    /** A winding path of light over the void, lanterns along it, and the round hall of the Road's End. */
    private static void build(ServerLevel mind, int slot) {
        BlockPos s = start(slot);
        mind.getChunk(s);
        if (mind.getBlockState(s.below()).is(Blocks.CALCITE)) {
            return;
        }
        for (int i = 0; i <= LENGTH; i++) {
            int wiggle = (int) Math.round(Math.sin(i * 0.28) * 3);
            for (int w = -1; w <= 1; w++) {
                BlockPos at = s.offset(wiggle + w, 0, -i);
                mind.getChunk(at);
                mind.setBlock(at, w == 0 && i % 4 == 0 ? Blocks.SEA_LANTERN.defaultBlockState()
                        : (i + w) % 3 == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 1; y <= 4; y++) {
                    mind.setBlock(at.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            if (i % 6 == 3) {
                for (int side = -1; side <= 1; side += 2) {
                    BlockPos post = s.offset(wiggle + side * 2, 0, -i);
                    mind.setBlock(post, Blocks.DARK_OAK_PLANKS.defaultBlockState(), Block.UPDATE_CLIENTS);
                    mind.setBlock(post.above(), Blocks.DARK_OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    mind.setBlock(post.above(2), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), Block.UPDATE_CLIENTS);
                }
            }
        }
        BlockPos e = end(slot);
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > 8.5) {
                    continue;
                }
                BlockPos at = e.offset(x, 0, z);
                mind.getChunk(at);
                BlockState floor = d > 7 ? Blocks.DARK_OAK_LOG.defaultBlockState() : d <= 2.6 || (d >= 5 && d <= 6)
                        ? Blocks.AMETHYST_BLOCK.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState();
                mind.setBlock(at, floor, Block.UPDATE_CLIENTS);
                for (int y = 1; y <= 5; y++) {
                    mind.setBlock(at.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        mind.setBlock(e, Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(30 + 60 * k);
            BlockPos p = e.offset((int) Math.round(Math.cos(a) * 7), 1, (int) Math.round(Math.sin(a) * 7));
            mind.setBlock(p, Blocks.AMETHYST_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            mind.setBlock(p.above(), Blocks.AMETHYST_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            mind.setBlock(p.above(2), Blocks.SOUL_LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        mind.setBlock(s.below(), Blocks.CALCITE.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Every tick of a walker: the Road's light, and on the dais, his choice. */
    public static void tick(ServerPlayer player) {
        Integer slot = WALKER_OF.get(player.getUUID());
        if (slot == null) {
            return;
        }
        if (player.level().dimension() != MindRealm.LEVEL) {
            leaveWalk(player);
            return;
        }
        Walk walk = WALKS.get(slot);
        if (walk == null) {
            leaveWalk(player);
            return;
        }
        ServerLevel mind = player.serverLevel();
        if (player.tickCount % 10 == 0) {
            MagicFx.burst(mind, player.position().add(0, 0.2, 0), WitchesRoad.LIGHT, 2, 0.4, 0.8F);
        }
        Vec3 dais = Vec3.atBottomCenterOf(end(slot).above());
        if (player.position().distanceToSqr(dais) < 3.0 * 3.0 && !walk.chosen.contains(player.getUUID())
                && !walk.asked.contains(player.getUUID())) {
            walk.asked.add(player.getUUID());
            ask(player);
        } else if (player.position().distanceToSqr(dais) > 4.5 * 4.5) {
            walk.asked.remove(player.getUUID());
        }
    }

    /** The prizes he may choose here: what he already has is not among them. */
    public static List<Prize> offered(ServerPlayer player) {
        List<Prize> list = new ArrayList<>();
        if (!Magic.data(player).has(MagicData.ROAD_UNBOUND)) {
            list.add(Prize.UNBOUND);
        }
        if (player.server.getPlayerList().getPlayerCount() > 1) {
            list.add(Prize.FIND);
        }
        if (!dead(player.server).isEmpty() && (!loaded() || WitchcraftConfig.ROAD_REVIVES.get())) {
            list.add(Prize.REVIVE);
        }
        if (!Powers.power(player).hasPowers() && !Magic.data(player).hasCraft() && (!loaded() || WitchcraftConfig.ROAD_OFFERS_WITCHCRAFT.get())) {
            list.add(Prize.WITCHCRAFT);
        }
        list.add(Prize.BLESSING);
        return list;
    }

    /** The fallen of a hardcore world, waiting as spectators. */
    static List<ServerPlayer> dead(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        if (!server.isHardcore()) {
            return out;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() && !WALKER_OF.containsKey(p.getUUID())) {
                out.add(p);
            }
        }
        return out;
    }

    private static void ask(ServerPlayer player) {
        List<String> prizes = new ArrayList<>();
        for (Prize p : offered(player)) {
            prizes.add(p.id());
        }
        List<String> extra = new ArrayList<>();
        for (String name : Realms.onlineNames(player)) {
            extra.add("find:" + name);
        }
        for (ServerPlayer d : dead(player.server)) {
            extra.add("revive:" + d.getGameProfile().getName());
        }
        for (WitchPath path : WitchPath.values()) {
            if (!path.rare) {
                extra.add("path:" + path.id());
            }
        }
        Net.sendToPlayer(player, new MagicScreenPayload(MagicScreenPayload.ROAD_PRIZE, prizes, extra, 0));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** His choice on the dais (checked here, whatever the client sent). */
    public static void choose(ServerPlayer player, String prizeId, String arg) {
        Integer slot = WALKER_OF.get(player.getUUID());
        Walk walk = slot == null ? null : WALKS.get(slot);
        Prize prize = Prize.byId(prizeId);
        if (walk == null || prize == null || walk.chosen.contains(player.getUUID()) || !offered(player).contains(prize)
                || player.position().distanceToSqr(Vec3.atBottomCenterOf(end(slot).above())) > 6 * 6) {
            return;
        }
        boolean given = give(player, prize, arg);
        if (!given) {
            return;
        }
        walk.chosen.add(player.getUUID());
        MagicFx.send(player.serverLevel(), MagicFx.AWAKEN, player.position(), List.of(), WitchesRoad.LIGHT, WitchesRoad.LIGHT_CORE, 1.0F, 40);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), com.kolodkingal.enhanced.registry.ModSounds.ROAD_TRAVEL.get(),
                SoundSource.PLAYERS, 1.0F, 1.2F);
        // the Road sends him home a moment later
        MagicZones.mark(player, "road_home", 60);
    }

    private static boolean give(ServerPlayer player, Prize prize, String arg) {
        switch (prize) {
            case UNBOUND -> {
                SpellEffects.unbind(player);
                SpellEffects.cure(player, true);
                Magic.set(player, Magic.data(player).withCorruption(0).with(MagicData.ROAD_UNBOUND));
                player.displayClientMessage(Component.translatable("message.enhanced.road.prize.unbound").withStyle(s -> s.withColor(WitchesRoad.LIGHT)), false);
            }
            case FIND -> {
                ServerPlayer found = player.server.getPlayerList().getPlayerByName(arg);
                if (found == null || found == player) {
                    return false;
                }
                Clairvoyance.reveal(player, found);
            }
            case REVIVE -> {
                ServerPlayer fallen = player.server.getPlayerList().getPlayerByName(arg);
                if (fallen == null || !dead(player.server).contains(fallen)) {
                    return false;
                }
                revive(player, fallen);
            }
            case WITCHCRAFT -> {
                WitchPath path = WitchPath.byId(arg);
                if (path == null || path.rare) {
                    return false;
                }
                Grants.give(player, path, 1, false);
            }
            case BLESSING -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 24000, 0, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 24000, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 24000, 1, false, true, true));
                player.heal(20.0F);
                player.displayClientMessage(Component.translatable("message.enhanced.road.prize.blessing").withStyle(s -> s.withColor(WitchesRoad.LIGHT)), false);
            }
        }
        return true;
    }

    /** A fallen player of a hardcore world rises: in Survival, whole, beside whoever brought him back once he is home. */
    private static void revive(ServerPlayer reviver, ServerPlayer fallen) {
        fallen.setGameMode(GameType.SURVIVAL);
        fallen.setHealth(fallen.getMaxHealth());
        fallen.getFoodData().setFoodLevel(20);
        com.kolodkingal.enhanced.mind.MindProgress back = com.kolodkingal.enhanced.mind.MindProgress.of(reviver);
        if (back.hasReturn()) {
            net.minecraft.nbt.CompoundTag r = back.returnPoint();
            ServerLevel home = reviver.level().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(r.getString("Dim"))));
            if (home != null) {
                fallen.teleportTo(home, r.getDouble("X"), r.getDouble("Y"), r.getDouble("Z"), r.getFloat("Yaw"), 0.0F);
            }
        }
        fallen.displayClientMessage(Component.translatable("message.enhanced.road.prize.revived", reviver.getDisplayName())
                .withStyle(s -> s.withColor(WitchesRoad.LIGHT)), false);
        reviver.level().getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.enhanced.road.prize.revive_all",
                reviver.getDisplayName(), fallen.getDisplayName()).withStyle(s -> s.withColor(WitchesRoad.LIGHT)), false);
    }

    /** Every tick: walkers who chose go home after their farewell. */
    public static void tickHome(ServerPlayer player) {
        if (WALKER_OF.containsKey(player.getUUID()) && !MagicZones.has(player, "road_home") && WALKS.containsKey(WALKER_OF.get(player.getUUID()))
                && WALKS.get(WALKER_OF.get(player.getUUID())).chosen.contains(player.getUUID())) {
            leaveWalk(player);
            if (player.level().dimension() == MindRealm.LEVEL) {
                MindRealm.leave(player);
            }
        }
    }

    private static void leaveWalk(ServerPlayer player) {
        Integer slot = WALKER_OF.remove(player.getUUID());
        if (slot == null) {
            return;
        }
        Walk walk = WALKS.get(slot);
        if (walk != null) {
            walk.walkers.remove(player.getUUID());
            if (walk.walkers.isEmpty()) {
                WALKS.remove(slot);
            }
        }
    }

    public static void logout(ServerPlayer player) {
        leaveWalk(player);
    }

    public static void clear() {
        WALKS.clear();
        WALKER_OF.clear();
        nextSlot = 0;
    }
}

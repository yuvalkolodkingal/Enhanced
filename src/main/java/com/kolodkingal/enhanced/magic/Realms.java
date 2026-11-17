package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.mind.MindProgress;
import com.kolodkingal.enhanced.mind.MindRealm;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The places of 3.17's magic, built far out in Vision's mind ({@link MindRealm#LEVEL}: its rules hold there too: whoever
 * enters remembers where he came from, {@link MindRealm#leave} takes him back, a login inside sends him home):
 * <ul>
 *   <li>the Witches' Mindscape: one round hall for every witch and sorcerer (and Billy, Wanda, Agatha, Doctor Strange),
 *   where they meet and talk unheard ({@code witchcraft.mindscapePrivateChat});</li>
 *   <li>Billy's pocket dimension: a hidden room for each caster, with chests for his things (and his coven's: a Sneak
 *   cast takes his friends near in with him);</li>
 *   <li>the dreams: the Dream Binding's trance, a misty floor in the void where a bound player is held, and the caster
 *   with him if he likes;</li>
 *   <li>the Coven's Road ({@link CovenRoad}).</li>
 * </ul>
 */
public final class Realms {
    public static final int HUB_Z = -100000;
    public static final int POCKET_Z = -120000;
    public static final int DREAM_Z = -140000;
    public static final int ROAD_Z = -160000;
    private static final int GROUND = MindRealm.GROUND;
    private static final int HUB_RADIUS = 12;

    /** Who dreams where, and until when. */
    private record Dream(int slot, long until, UUID binder) {
    }

    private static final Map<UUID, Dream> DREAMS = new HashMap<>();
    private static int nextDream;
    /** Dreamwalkers (the Darkhold): the game mode to give back, where the body stands, and when. */
    private record Walk(GameType mode, Vec3 body, long until) {
    }

    private static final Map<UUID, Walk> WALKS = new HashMap<>();
    private static boolean hubBuilt;

    private Realms() {
    }

    // ---- where things are ----

    public static BlockPos hubCenter() {
        return new BlockPos(0, GROUND, HUB_Z);
    }

    /** Whether he stands in the Witches' Mindscape. */
    public static boolean inMindscape(Player player) {
        return player.level().dimension() == MindRealm.LEVEL && Math.abs(player.getZ() - HUB_Z) < 64 && Math.abs(player.getX()) < 64;
    }

    /** Whether he is in a pocket dimension. */
    public static boolean inPocket(Player player) {
        return player.level().dimension() == MindRealm.LEVEL && Math.abs(player.getZ() - POCKET_Z) < 32;
    }

    public static boolean dreaming(Player player) {
        return DREAMS.containsKey(player.getUUID());
    }

    // ---- going in and out ----

    /** Takes him into the mind's world at {@code at} (remembering where he came from), or false if he can't go. */
    static boolean enter(ServerPlayer player, Vec3 at, float yaw) {
        ServerLevel mind = MindRealm.level(player.server);
        if (mind == null || !player.isAlive() || player.isSpectator()) {
            if (mind == null) {
                player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_world"), true);
            }
            return false;
        }
        MindProgress progress = MindProgress.of(player);
        if (player.level().dimension() != MindRealm.LEVEL || !progress.hasReturn()) {
            progress.setReturn(player.level().dimension().location().toString(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot());
        }
        player.stopRiding();
        player.teleportTo(mind, at.x, at.y, at.z, yaw, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        mind.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    /** The Mindscape spell: in, or (from inside) back out. */
    public static boolean toggleMindscape(ServerPlayer player) {
        if (inMindscape(player)) {
            MindRealm.leave(player);
            return true;
        }
        if (!Magic.mindscapeOpen(player)) {
            // (only Billy, and those whose magic is his gift, find the way in)
            player.displayClientMessage(Component.translatable("message.enhanced.mindscape.closed").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (player.level().dimension() == MindRealm.LEVEL) {
            player.displayClientMessage(Component.translatable("message.enhanced.spell.mind_world").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel mind = MindRealm.level(player.server);
        if (mind == null) {
            return false;
        }
        buildHub(mind);
        double a = player.getRandom().nextDouble() * Math.PI * 2;
        Vec3 at = Vec3.atBottomCenterOf(hubCenter()).add(Math.cos(a) * 5, 1, Math.sin(a) * 5);
        if (!enter(player, at, 0.0F)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("message.enhanced.mindscape.enter").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        for (ServerPlayer other : mind.players()) {
            if (other != player && inMindscape(other)) {
                other.displayClientMessage(Component.translatable("message.enhanced.mindscape.arrives", player.getDisplayName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
        }
        return true;
    }

    /** The hall of the Mindscape: a round floor over the starry void, a ring of candles, a glowing well in the middle. */
    private static void buildHub(ServerLevel mind) {
        BlockPos c = hubCenter();
        if (hubBuilt && mind.getBlockState(c).is(Blocks.SEA_LANTERN)) {
            return;
        }
        mind.getChunk(c);
        for (int x = -HUB_RADIUS; x <= HUB_RADIUS; x++) {
            for (int z = -HUB_RADIUS; z <= HUB_RADIUS; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > HUB_RADIUS + 0.5) {
                    continue;
                }
                BlockPos at = c.offset(x, 0, z);
                mind.getChunk(at);
                boolean rim = d > HUB_RADIUS - 1.5;
                boolean ring = d >= 6.5 && d <= 7.5;
                BlockState floor = rim ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : ring ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                        : (x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
                mind.setBlock(at, floor, Block.UPDATE_CLIENTS);
                mind.setBlock(at.below(), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 1; y <= 6; y++) {
                    mind.setBlock(at.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        mind.setBlock(c, Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            BlockPos pillar = c.offset((int) Math.round(Math.cos(a) * (HUB_RADIUS - 2)), 1, (int) Math.round(Math.sin(a) * (HUB_RADIUS - 2)));
            mind.setBlock(pillar, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            mind.setBlock(pillar.above(), Blocks.PURPLE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3)
                    .setValue(CandleBlock.LIT, true), Block.UPDATE_CLIENTS);
        }
        // the way out: a lantern on the floor at the north edge
        mind.setBlock(c.offset(0, 0, -(HUB_RADIUS - 3)), Blocks.CRYING_OBSIDIAN.defaultBlockState(), Block.UPDATE_CLIENTS);
        hubBuilt = true;
    }

    // ---- the pocket dimension ----

    static boolean togglePocket(ServerPlayer player, boolean bringFriends) {
        if (inPocket(player)) {
            MindRealm.leave(player);
            return true;
        }
        if (player.level().dimension() == MindRealm.LEVEL) {
            player.displayClientMessage(Component.translatable("message.enhanced.spell.mind_world").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel mind = MindRealm.level(player.server);
        if (mind == null) {
            return false;
        }
        int slot = Rooms.get(player.server).slotOf(player.getUUID());
        BlockPos origin = new BlockPos(slot * 64, GROUND, POCKET_Z);
        buildPocket(mind, origin);
        List<ServerPlayer> squad = new ArrayList<>();
        if (bringFriends) {
            for (ServerPlayer other : player.serverLevel().players()) {
                if (other != player && other.distanceToSqr(player) <= 8 * 8 && Magic.friend(player, other) && !other.isSpectator()) {
                    squad.add(other);
                }
            }
        }
        MagicFx.send(player.serverLevel(), MagicFx.PORTAL, player.position(), List.of(player.getLookAngle()), 0x6FA8FF, 0xFFFFFF, 1.3F, 20);
        if (!enter(player, Vec3.atBottomCenterOf(origin.above()), 180.0F)) {
            return false;
        }
        for (ServerPlayer friend : squad) {
            enter(friend, Vec3.atBottomCenterOf(origin.above()).add(1, 0, 0), 180.0F);
        }
        player.displayClientMessage(Component.translatable("message.enhanced.spell.pocket").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** A room of nine by nine, walled and roofed, with four chests and two barrels along its back wall; built once. */
    private static void buildPocket(ServerLevel mind, BlockPos o) {
        mind.getChunk(o);
        if (mind.getBlockState(o.below()).is(Blocks.CALCITE)) {
            return;
        }
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = -1; y <= 5; y++) {
                    BlockPos at = o.offset(x, y, z);
                    boolean wall = Math.abs(x) == 5 || Math.abs(z) == 5;
                    BlockState state = y == -1 || y == 5 || wall
                            ? (y == -1 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState())
                            : y == 0 ? ((x + z) % 2 == 0 ? Blocks.POLISHED_DIORITE.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState())
                            : Blocks.AIR.defaultBlockState();
                    mind.setBlock(at, state, Block.UPDATE_CLIENTS);
                }
            }
        }
        mind.setBlock(o.below(), Blocks.CALCITE.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (int x = -3; x <= 3; x += 2) {
            mind.setBlock(o.offset(x, 1, -4), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        }
        mind.setBlock(o.offset(-4, 1, -4), Blocks.BARREL.defaultBlockState(), Block.UPDATE_ALL);
        mind.setBlock(o.offset(4, 1, -4), Blocks.BARREL.defaultBlockState(), Block.UPDATE_ALL);
        mind.setBlock(o.offset(-4, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        mind.setBlock(o.offset(4, 1, 2), Blocks.ENCHANTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        for (int[] l : new int[][]{{-3, 3}, {3, 3}, {-3, -2}, {3, -2}}) {
            mind.setBlock(o.offset(l[0], 4, l[1]), Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // ---- dreams ----

    /** The Dream Binding: a player is carried into a trance (the caster with him when {@code together}); a creature dozes. */
    static boolean dreamBind(ServerPlayer caster, LivingEntity target, int ticks, boolean together, int color) {
        ServerLevel level = caster.serverLevel();
        if (!(target instanceof ServerPlayer victim)) {
            target.addEffect(new MobEffectInstance(com.kolodkingal.enhanced.registry.ModEffects.SPELLBOUND, ticks, 0, false, false, true), caster);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0, false, false, true), caster);
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
            MagicZones.mark(target, "dream_bound", ticks);
            MagicFx.send(level, MagicFx.BIND, target.position(), List.of(target.position()), color, 0xF0E8FF, target.getBbHeight(), ticks);
            return true;
        }
        if (victim.level().dimension() == MindRealm.LEVEL || Magic.unbound(victim)) {
            caster.displayClientMessage(Component.translatable("message.enhanced.spell.dream_resisted"), true);
            return false;
        }
        ServerLevel mind = MindRealm.level(caster.server);
        if (mind == null) {
            return false;
        }
        int slot = nextDream++ % 256;
        BlockPos o = new BlockPos(slot * 64, GROUND, DREAM_Z);
        buildDream(mind, o);
        long until = level.getGameTime() + ticks;
        if (!enter(victim, Vec3.atBottomCenterOf(o.above()), 0.0F)) {
            return false;
        }
        DREAMS.put(victim.getUUID(), new Dream(slot, until, caster.getUUID()));
        victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, false));
        victim.displayClientMessage(Component.translatable("message.enhanced.spell.dream.you").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        if (together) {
            enter(caster, Vec3.atBottomCenterOf(o.above()).add(0, 0, 4), 180.0F);
            DREAMS.put(caster.getUUID(), new Dream(slot, until, caster.getUUID()));
        }
        caster.displayClientMessage(Component.translatable("message.enhanced.spell.dream", victim.getDisplayName()), true);
        return true;
    }

    /** A pale floor in a mist of purple, a bed in the middle: the trance's room. */
    private static void buildDream(ServerLevel mind, BlockPos o) {
        mind.getChunk(o);
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (x * x + z * z > 52) {
                    continue;
                }
                mind.setBlock(o.offset(x, 0, z), (x + z) % 3 == 0 ? Blocks.PURPLE_STAINED_GLASS.defaultBlockState()
                        : Blocks.WHITE_CONCRETE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 1; y <= 4; y++) {
                    mind.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        mind.setBlock(o.offset(0, 1, -5), Blocks.AMETHYST_CLUSTER.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Wakes a dreamer (his time is up, or he was unbound). */
    public static void wake(ServerPlayer player) {
        Dream dream = DREAMS.remove(player.getUUID());
        if (dream != null && player.level().dimension() == MindRealm.LEVEL) {
            MindRealm.leave(player);
            player.displayClientMessage(Component.translatable("message.enhanced.spell.dream.wake").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    // ---- the Darkhold's dreamwalk ----

    /** His spirit walks out of his body: through walls, unseen, for a while; then it snaps back to where the body stood. */
    static boolean dreamwalk(ServerPlayer player, int ticks) {
        if (WALKS.containsKey(player.getUUID()) || player.isCreative() || player.isSpectator()) {
            return false;
        }
        WALKS.put(player.getUUID(), new Walk(player.gameMode.getGameModeForPlayer(), player.position(), player.level().getGameTime() + ticks));
        MagicFx.burst(player.serverLevel(), player.position().add(0, 1, 0), 0x6A2FB0, 30, 0.4, 1.2F);
        player.setGameMode(GameType.SPECTATOR);
        player.displayClientMessage(Component.translatable("message.enhanced.spell.dreamwalk", ticks / 20).withStyle(ChatFormatting.DARK_PURPLE), false);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2F, 0.5F);
        return true;
    }

    private static void endWalk(ServerPlayer player) {
        Walk walk = WALKS.remove(player.getUUID());
        if (walk == null) {
            return;
        }
        player.setGameMode(walk.mode() == GameType.SPECTATOR ? GameType.SURVIVAL : walk.mode());
        player.teleportTo(walk.body().x, walk.body().y, walk.body().z);
        player.resetFallDistance();
        player.displayClientMessage(Component.translatable("message.enhanced.spell.dreamwalk.back").withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    // ---- every tick ----

    /** Every tick of every player. */
    public static void tick(ServerPlayer player) {
        long now = player.level().getGameTime();
        Dream dream = DREAMS.get(player.getUUID());
        if (dream != null && (now >= dream.until() || player.level().dimension() != MindRealm.LEVEL)) {
            wake(player);
        }
        Walk walk = WALKS.get(player.getUUID());
        if (walk != null && now >= walk.until()) {
            endWalk(player);
        }
        if (player.tickCount % 10 == 0 && inMindscape(player)) {
            BlockPos exit = hubCenter().offset(0, 0, -(HUB_RADIUS - 3));
            if (player.blockPosition().below().equals(exit) || player.blockPosition().equals(exit.above())) {
                MindRealm.leave(player);
            }
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
        }
    }

    /** He left the game: a dreamwalker's spirit goes back to his body first. */
    public static void logout(ServerPlayer player) {
        endWalk(player);
        DREAMS.remove(player.getUUID());
    }

    /** The names of the players online (for the clairvoyance's box), but his own. */
    static List<String> onlineNames(ServerPlayer player) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player) {
                names.add(other.getGameProfile().getName());
            }
        }
        return names;
    }

    public static void clear(MinecraftServer server) {
        if (server != null) {
            for (UUID id : new ArrayList<>(WALKS.keySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    endWalk(player);
                }
            }
        }
        WALKS.clear();
        DREAMS.clear();
        hubBuilt = false;
        nextDream = 0;
    }

    /** The rooms handed out (kept with the Overworld's data). */
    public static final class Rooms extends SavedData {
        public static final String NAME = "enhanced_magic_rooms";
        private final Map<UUID, Integer> slots = new HashMap<>();
        private int next;

        public static SavedData.Factory<Rooms> factory() {
            return new SavedData.Factory<>(Rooms::new, Rooms::load, null);
        }

        static Rooms get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
        }

        private static Rooms load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            Rooms rooms = new Rooms();
            rooms.next = tag.getInt("Next");
            CompoundTag map = tag.getCompound("Slots");
            for (String key : map.getAllKeys()) {
                try {
                    rooms.slots.put(UUID.fromString(key), map.getInt(key));
                } catch (IllegalArgumentException ignored) {
                    // not a player's id
                }
            }
            return rooms;
        }

        int slotOf(UUID id) {
            Integer slot = slots.get(id);
            if (slot == null) {
                slot = next++;
                slots.put(id, slot);
                setDirty();
            }
            return slot;
        }

        @Override
        public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            tag.putInt("Next", next);
            CompoundTag map = new CompoundTag();
            slots.forEach((id, slot) -> map.putInt(id.toString(), slot));
            tag.put("Slots", map);
            return tag;
        }
    }
}

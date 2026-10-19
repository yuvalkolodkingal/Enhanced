package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SoulStoneConfig;
import com.kolodkingal.enhanced.item.InfinityStone;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneProgress;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Soul Stone's sacrifice at the Vormir Cliff (3.6, the plan's 6.1 and 6.2; the user's answer of 18/10: not by the
 * friend's choice — the seeker pushes him, or his pet, off the cliff).
 *
 * <p>A death is judged when it happens ({@link #judge}): a fall ({@code fall} or {@code fly_into_wall}) that ends in a
 * cliff's chasm ({@code PIT_BOX}), begun on its edge within {@code deathWindowTicks}, at least {@code minFall} deep,
 * and a player's push (his hit, his shot, his pet's bite, his power's shove, his telekinetic drop) within {@code
 * pushWindowTicks} before the victim left the edge. Then the rules ({@link #rules}): the stone is on, the seeker no
 * Creative or Spectator, a Bearer, the victim a player (with {@code playerSacrifice}, where PvP is on and the seeker
 * may hurt him; with {@code friendRule = team} a team mate) or the seeker's own tamed pet (with {@code petSacrifice});
 * and {@code onePer} lets him have one. Anything less is an ordinary death; the Stonekeeper tells a seeker who pushed
 * why nothing was given.
 *
 * <p>A sacrifice: the victim player loses every item (inventory, armour, off hand, the carried stack, the crafting
 * grid; even under {@code keepInventory}; his ender chest is never touched), every stone among them returns to its site
 * ({@link StoneLedger#destroyed}), his experience is lost and his power taken ({@code destroyItems}, {@code
 * removePower}); he respawns as normal and is told. A pet dies with no drops and no experience, and its soul rises to
 * the seeker. The seeker wakes in the cliff's pool with the Soul Stone.
 */
public final class Sacrifice {
    /** The orange of the stone's light (its colour). */
    public static final int ORANGE = StoneKind.SOUL.color();
    /** How long (ticks) the seeker's screen glows orange. */
    public static final int FADE_TICKS = 40;
    /** How long (ticks) a pet's soul takes to rise to its master. */
    public static final int WISP_TICKS = 60;
    /** The advancement of the gift. */
    public static final ResourceLocation ADVANCEMENT = Enhanced.id("vormir/a_soul_for_a_soul");

    /** What a death at the cliff comes to (the Stonekeeper's line for each, null where he says nothing). */
    public enum Outcome {
        /** The stone is given. */
        SACRIFICE("message.enhanced.vormir.soul_for_soul"),
        /** No fall from a cliff's edge into its chasm: an ordinary death, nothing said. */
        NOT_A_FALL(null),
        /** Nobody pushed him (he jumped, he fell, a creature pushed him, the push is too old): nothing said. */
        NO_PUSH(null),
        /** {@code enabled} is off. */
        DISABLED("npc.enhanced.stonekeeper.asleep"),
        /** The seeker is in Creative or Spectator. */
        CREATIVE("npc.enhanced.stonekeeper.creative"),
        /** The seeker may not hold the Soul Stone. */
        NOT_BEARER("npc.enhanced.stonekeeper.not_bearer"),
        /** A player victim, {@code playerSacrifice} off. */
        PLAYERS_OFF("npc.enhanced.stonekeeper.pets_only"),
        /** A pet victim, {@code petSacrifice} off. */
        PETS_OFF("npc.enhanced.stonekeeper.friends_only"),
        /** A player victim where no player may hurt another (PvP off, the same team without friendly fire). */
        PVP_OFF("npc.enhanced.stonekeeper.no_harm"),
        /** Someone else's pet, a wild creature, not a team mate under {@code friendRule = team}. */
        NOT_LOVED("npc.enhanced.stonekeeper.not_loved"),
        /** {@code onePer = player}: he already has one. */
        ALREADY_HAS("npc.enhanced.stonekeeper.has_one"),
        /** {@code onePer = world}: the world's stone has been given. */
        TAKEN("npc.enhanced.stonekeeper.taken");

        @Nullable
        private final String line;

        Outcome(@Nullable String line) {
            this.line = line;
        }

        /** The lang key of the Stonekeeper's line to the seeker, or null. */
        @Nullable
        public String line() {
            return line;
        }
    }

    /** A death judged: what it comes to, the seeker who pushed (null: none) and the cliff (null: none). */
    public record Judgment(Outcome outcome, @Nullable ServerPlayer seeker, @Nullable VormirSite site) {
    }

    /** Sacrificed players and pets whose drops and experience are swallowed (until their drops are handled). */
    private static final Set<UUID> SWALLOWED = new HashSet<>();
    /** Sacrificed players who have not respawned yet (their message waits). */
    private static final Set<UUID> GIVEN = new HashSet<>();
    /** The souls of sacrificed pets on their way to their masters. */
    private static final List<Wisp> WISPS = new ArrayList<>();

    private record Wisp(ServerLevel level, Vec3 from, UUID master, long start) {
    }

    // ---- the judgment ----

    /** What the death of {@code victim} by {@code source} comes to (read when it dies, before anything is dropped). */
    public static Judgment judge(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return new Judgment(Outcome.NOT_A_FALL, null, null);
        }
        VormirTracker.Departure departure = VormirTracker.departure(victim);
        if (departure == null || !(source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL))) {
            return new Judgment(Outcome.NOT_A_FALL, null, null);
        }
        long now = level.getGameTime();
        VormirSite site = departure.site();
        double fall = Math.max(victim.fallDistance, departure.y() - victim.getY());
        if (now < departure.tick() || now - departure.tick() > VormirTracker.deathWindowTicks() || !site.inPit(victim)
                || fall < minFall()) {
            return new Judgment(Outcome.NOT_A_FALL, null, null);
        }
        VormirTracker.Credit credit = departure.credit();
        ServerPlayer seeker = credit == null || credit.pusher() == null ? null
                : level.getServer().getPlayerList().getPlayer(credit.pusher());
        if (seeker == null || seeker == victim) {
            return new Judgment(Outcome.NO_PUSH, null, site);
        }
        return new Judgment(rules(seeker, victim), seeker, site);
    }

    /**
     * Whether {@code seeker} would be given the Soul Stone for {@code victim} pushed off the cliff (the rules after the
     * fall and the push: the plan's 6.1, conditions 3 to 6).
     */
    public static Outcome rules(ServerPlayer seeker, LivingEntity victim) {
        if (!enabled()) {
            return Outcome.DISABLED;
        }
        if (seeker.isCreative() || seeker.isSpectator()) {
            return Outcome.CREATIVE;
        }
        if (!StoneRules.mayHold(seeker, StoneKind.SOUL)) {
            return Outcome.NOT_BEARER;
        }
        if (victim instanceof ServerPlayer friend) {
            if (!playerSacrifice()) {
                return Outcome.PLAYERS_OFF;
            }
            if (!seeker.server.isPvpAllowed()) {
                return Outcome.PVP_OFF;
            }
            if (friendRule() == SoulStoneConfig.FriendRule.TEAM
                    && (seeker.getTeam() == null || !seeker.isAlliedTo(friend))) {
                return Outcome.NOT_LOVED;
            }
            if (!seeker.canHarmPlayer(friend)) {
                return Outcome.PVP_OFF;
            }
        } else if (isPetOf(victim, seeker)) {
            if (!petSacrifice()) {
                return Outcome.PETS_OFF;
            }
        } else {
            return Outcome.NOT_LOVED;
        }
        Outcome owed = refusal(seeker);
        return owed != null ? owed : Outcome.SACRIFICE;
    }

    /**
     * Why {@code onePer} gives the seeker no stone now ({@link Outcome#ALREADY_HAS}, {@link Outcome#TAKEN}), or null.
     * The stones' ledger counts the Soul Stone by {@code [soul_stone] onePer} ({@link StoneLedger#mayTake}): in {@code
     * player} mode one who has taken his (the bit of his progress) or carries one has it already; in {@code world}
     * mode the world's stone has a holder until it is lost.
     */
    @Nullable
    public static Outcome refusal(ServerPlayer seeker) {
        boolean ledger = StoneLedger.mayTake(seeker, StoneKind.SOUL);
        return switch (onePer()) {
            case UNLIMITED -> null;
            case WORLD -> ledger ? null : Outcome.TAKEN;
            case PLAYER -> !ledger || seeker.getData(ModAttachments.STONES).hasTaken(StoneKind.SOUL)
                    || seeker.getInventory().contains(s -> s.is(ModItems.SOUL_STONE.get()))
                    ? Outcome.ALREADY_HAS : null;
        };
    }

    /** Whether {@code victim} is the seeker's own tamed pet (a tamed animal or a tamed horse, donkey, mule, llama). */
    public static boolean isPetOf(LivingEntity victim, ServerPlayer seeker) {
        boolean tamed = victim instanceof TamableAnimal animal && animal.isTame()
                || victim instanceof AbstractHorse horse && horse.isTamed();
        return tamed && victim instanceof OwnableEntity pet && seeker.getUUID().equals(pet.getOwnerUUID());
    }

    // ---- the death ----

    /** A creature died (not saved: the event was not cancelled): the cliff's judgment, and what follows it. */
    static void onDeath(LivingEntity victim, DamageSource source) {
        Judgment judgment = judge(victim, source);
        VormirTracker.forget(victim.getUUID());
        ServerPlayer seeker = judgment.seeker();
        if (seeker == null) {
            return;
        }
        if (judgment.outcome() != Outcome.SACRIFICE) {
            Stonekeeper.say(seeker, judgment.outcome().line());
            return;
        }
        if (victim instanceof ServerPlayer friend) {
            takeFrom(friend);
        } else {
            SWALLOWED.add(victim.getUUID());
            Vec3 from = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
            WISPS.add(new Wisp((ServerLevel) victim.level(), from, seeker.getUUID(), victim.level().getGameTime()));
        }
        give(seeker, judgment.site());
    }

    /** What the victim player loses: his things, his experience, his power. */
    private static void takeFrom(ServerPlayer friend) {
        if (destroyItems()) {
            destroyEverything(friend);
            SWALLOWED.add(friend.getUUID());
        }
        if (removePower() && Powers.power(friend).hasPowers()) {
            Powers.setPower(friend, Power.NONE);
        }
        friend.setData(ModAttachments.STONES, friend.getData(ModAttachments.STONES).with(StoneProgress.WAS_SACRIFICED));
        GIVEN.add(friend.getUUID());
    }

    /**
     * Every item of the player gone for good (the inventory, armour and off hand, the stack on his cursor, his crafting
     * grid), each through {@link StoneLedger#destroyed} first, and his experience. His ender chest is not touched.
     */
    public static void destroyEverything(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            destroy(inventory.getItem(i));
        }
        inventory.clearContent();
        destroy(player.containerMenu.getCarried());
        player.containerMenu.setCarried(ItemStack.EMPTY);
        CraftingContainer grid = player.inventoryMenu.getCraftSlots();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            destroy(grid.getItem(i));
        }
        grid.clearContent();
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
    }

    private static void destroy(ItemStack stack) {
        if (!stack.isEmpty()) {
            StoneLedger.destroyed(stack);
        }
    }

    /** Whether the creature's drops and experience are swallowed (it was given); asked as they would fall. */
    static boolean swallows(LivingEntity entity) {
        return SWALLOWED.contains(entity.getUUID());
    }

    /** Its drops have been handled. */
    static void swallowed(LivingEntity entity) {
        SWALLOWED.remove(entity.getUUID());
    }

    /** A respawned player who was given hears what happened. */
    static void onRespawn(ServerPlayer player) {
        if (GIVEN.remove(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.enhanced.vormir.given").withColor(ORANGE));
            player.sendSystemMessage(Component.translatable("message.enhanced.vormir.ender_chest")
                    .withColor(0xC9A27A));
        }
    }

    // ---- the gift ----

    /**
     * The seeker wakes in the cliff's pool holding the Soul Stone (in his hand if it is free, else in his inventory,
     * else at his feet): the keeper's line, an orange glow over his screen, the title, the sounds; the ledger and his
     * progress mark it, and the advancement.
     */
    static void give(ServerPlayer seeker, @Nullable VormirSite site) {
        MinecraftServer server = seeker.server;
        ServerLevel level = site == null ? seeker.serverLevel() : server.getLevel(site.dimension());
        if (level == null) {
            level = seeker.serverLevel();
        }
        if (site != null) {
            Vec3 at = standingPlace(level, site.pool());
            seeker.stopRiding();
            seeker.teleportTo(level, at.x, at.y, at.z, seeker.getYRot(), seeker.getXRot());
            seeker.resetFallDistance();
        }
        ItemStack stone = new ItemStack(ModItems.SOUL_STONE.get());
        StoneLedger.stamp(stone, seeker.getUUID());
        if (seeker.getMainHandItem().isEmpty()) {
            seeker.setItemInHand(InteractionHand.MAIN_HAND, stone);
        } else if (!seeker.getInventory().add(stone)) {
            seeker.drop(stone, false);
        }
        StoneProgress progress = seeker.getData(ModAttachments.STONES).with(StoneProgress.SACRIFICED_FOR);
        // (The ledger's player mode keeps the same bit: set here too, so it holds whatever the ledger's state.)
        seeker.setData(ModAttachments.STONES, onePer() == SoulStoneConfig.OnePer.PLAYER
                ? progress.withTaken(StoneKind.SOUL, true) : progress);
        StoneLedger.taken(seeker, StoneKind.SOUL);
        Stonekeeper.say(seeker, Outcome.SACRIFICE.line());
        seeker.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        seeker.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("title.enhanced.vormir.soul_stone").withColor(ORANGE)));
        seeker.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("title.enhanced.vormir.soul_stone.subtitle").withColor(0xFFD6A0)));
        PacketDistributor.sendToPlayer(seeker, new StoneFxPayload(StoneFxPayload.SOUL_GIVEN, seeker.getId(), List.of(),
                FADE_TICKS));
        ServerLevel at = seeker.serverLevel();
        at.playSound(null, seeker.getX(), seeker.getY(), seeker.getZ(), ModSounds.SOUL_SACRIFICE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        InfinityStone.SOUL.use(at, seeker.position(), InfinityStone.UNDER, 1.0F);
        Powers.burst(at, seeker.getX(), seeker.getY() + 0.3, seeker.getZ(), ORANGE, 40, 0.9);
        AdvancementHolder advancement = server.getAdvancements().get(ADVANCEMENT);
        if (advancement != null) {
            seeker.getAdvancements().award(advancement, "sacrifice");
        }
    }

    /** The first place at or above the pool where a player fits (the pool's glass is a floor). */
    static Vec3 standingPlace(ServerLevel level, BlockPos pool) {
        for (int up = 0; up <= 3; up++) {
            BlockPos feet = pool.above(up);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                return Vec3.atBottomCenterOf(feet);
            }
        }
        return Vec3.atBottomCenterOf(pool.above());
    }

    // ---- the pets' souls ----

    /** Every tick: each soul rises from the chasm toward its master's hand, an orange wisp, and fades into him. */
    static void tickWisps(ServerLevel level) {
        long now = level.getGameTime();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(1.0F, 0.54F, 0.12F), 1.2F);
        for (Iterator<Wisp> it = WISPS.iterator(); it.hasNext(); ) {
            Wisp wisp = it.next();
            if (wisp.level() != level) {
                continue;
            }
            ServerPlayer master = level.getServer().getPlayerList().getPlayer(wisp.master());
            float t = (now - wisp.start()) / (float) WISP_TICKS;
            if (master == null || master.level() != level || t >= 1.0F || t < 0.0F) {
                if (master != null && master.level() == level) {
                    level.sendParticles(ParticleTypes.SOUL, master.getX(), master.getY() + 1.0, master.getZ(), 12,
                            0.3, 0.4, 0.3, 0.02);
                }
                it.remove();
                continue;
            }
            Vec3 hand = master.position().add(0.0, master.getBbHeight() * 0.6, 0.0);
            double ease = t * t * (3.0 - 2.0 * t);
            Vec3 at = wisp.from().lerp(hand, ease);
            double angle = t * 12.0;
            double r = 0.4 * (1.0 - t);
            level.sendParticles(dust, at.x + Math.cos(angle) * r, at.y, at.z + Math.sin(angle) * r, 2, 0.05, 0.05, 0.05,
                    0.0);
            if ((now - wisp.start()) % 4 == 0) {
                level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    /** The server stopped: everything waiting is forgotten. */
    static void clear() {
        SWALLOWED.clear();
        GIVEN.clear();
        WISPS.clear();
    }

    /** Whether a respawn message waits for the player (tests). */
    public static boolean awaitsRespawn(UUID player) {
        return GIVEN.contains(player);
    }

    // ---- the config (read when asked; its defaults where it is not loaded) ----

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    public static boolean enabled() {
        return !loaded() || SoulStoneConfig.ENABLED.get();
    }

    public static boolean playerSacrifice() {
        return !loaded() || SoulStoneConfig.PLAYER_SACRIFICE.get();
    }

    public static boolean petSacrifice() {
        return !loaded() || SoulStoneConfig.PET_SACRIFICE.get();
    }

    static boolean destroyItems() {
        return !loaded() || SoulStoneConfig.DESTROY_ITEMS.get();
    }

    static boolean removePower() {
        return !loaded() || SoulStoneConfig.REMOVE_POWER.get();
    }

    static SoulStoneConfig.FriendRule friendRule() {
        return loaded() ? SoulStoneConfig.FRIEND_RULE.get() : SoulStoneConfig.FriendRule.ANY;
    }

    public static SoulStoneConfig.OnePer onePer() {
        return loaded() ? SoulStoneConfig.ONE_PER.get() : SoulStoneConfig.OnePer.PLAYER;
    }

    static int minFall() {
        return loaded() ? SoulStoneConfig.MIN_FALL.get() : 30;
    }

    private Sacrifice() {
    }
}

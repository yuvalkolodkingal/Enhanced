package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SoulStoneConfig;
import com.kolodkingal.enhanced.item.InfinityStone;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
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
import net.minecraft.world.entity.player.Player;
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
 * Creative or Spectator (3.6.1: not necessarily a Bearer), the victim a player (with {@code playerSacrifice}, where PvP is on and the seeker
 * may hurt him; with {@code friendRule = team} a team mate) or the seeker's own tamed pet (with {@code petSacrifice});
 * and {@code onePer} lets him have one. Anything less is an ordinary death; the Stonekeeper tells a seeker who pushed
 * why nothing was given.
 *
 * <p>A sacrifice: the victim player loses every item (inventory, armour, off hand, the carried stack, the crafting
 * grid; even under {@code keepInventory}; his ender chest is never touched), every stone among them returns to its site
 * ({@link StoneLedger#destroyed}), his experience is lost and his power taken ({@code destroyItems}, {@code
 * removePower}); he respawns as normal and is told. A pet dies with no drops and no experience, and its soul rises to
 * the seeker. The seeker wakes in the cliff's pool with the Soul Stone.
 *
 * <p>3.6.1 (found in a real client on a generated cliff): one who left the edge with a push and lands alive on the
 * pit's floor (a cat never takes fall damage, a horse takes half, a hero may take none) is judged there ({@link
 * #onLanding}) and, if it counts, the fall takes it; a seeker who may not hold the stone bare earns it too, on a
 * pedestal by the pool ({@link HeldSoulStone}); and a seeker whose sacrifice did not count is told why in plain words
 * ({@link #tell}).
 */
public final class Sacrifice {
    /** The orange of the stone's light (its colour). */
    public static final int ORANGE = StoneKind.SOUL.color();
    /** How long (ticks) the seeker's screen glows orange. */
    public static final int FADE_TICKS = 40;
    /** The colour of the plain line that says why a sacrifice did not count (3.6.1). */
    public static final int WHY_COLOR = 0xE8B07A;
    /** How long (ticks) a pet's soul takes to rise to its master. */
    public static final int WISP_TICKS = 60;
    /** The advancement of the gift. */
    public static final ResourceLocation ADVANCEMENT = Enhanced.id("vormir/a_soul_for_a_soul");

    /**
     * What a death (or a landing) at the cliff comes to: the Stonekeeper's line to the seeker (null where he says
     * nothing) and, 3.6.1, a short plain line that says why it did not count (null for none).
     */
    public enum Outcome {
        /** The stone is given. */
        SACRIFICE("message.enhanced.vormir.soul_for_soul", null),
        /** No fall from a cliff's edge into its chasm: an ordinary death, nothing said. */
        NOT_A_FALL(null, null),
        /** Nobody pushed him (he jumped, he fell, a creature pushed him): nothing said. */
        NO_PUSH(null, null),
        /** {@code enabled} is off. */
        DISABLED("npc.enhanced.stonekeeper.asleep", "message.enhanced.vormir.why.disabled"),
        /** The seeker is in Creative or Spectator. */
        CREATIVE("npc.enhanced.stonekeeper.creative", "message.enhanced.vormir.why.creative"),
        /**
         * The seeker may not hold the Soul Stone bare: since 3.6.1 no refusal (the stone waits for him on a pedestal),
         * only the keeper's third line.
         */
        NOT_BEARER("npc.enhanced.stonekeeper.held", null),
        /** A player victim, {@code playerSacrifice} off. */
        PLAYERS_OFF("npc.enhanced.stonekeeper.pets_only", "message.enhanced.vormir.why.players_off"),
        /** A pet victim, {@code petSacrifice} off. */
        PETS_OFF("npc.enhanced.stonekeeper.friends_only", "message.enhanced.vormir.why.pets_off"),
        /** A player victim where no player may hurt another (PvP off, the same team without friendly fire). */
        PVP_OFF("npc.enhanced.stonekeeper.no_harm", "message.enhanced.vormir.why.pvp_off"),
        /** Someone else's pet, a wild creature, not a team mate under {@code friendRule = team}. */
        NOT_LOVED("npc.enhanced.stonekeeper.not_loved", "message.enhanced.vormir.why.not_your_pet"),
        /** {@code onePer = player}: he already has one. */
        ALREADY_HAS("npc.enhanced.stonekeeper.has_one", "message.enhanced.vormir.why.has_one"),
        /** {@code onePer = world}: the world's stone has been given. */
        TAKEN("npc.enhanced.stonekeeper.taken", "message.enhanced.vormir.why.taken"),
        /** 3.6.1: pushed into the chasm, but not from the edge's strip (no departure from it). */
        NOT_FROM_EDGE(null, "message.enhanced.vormir.why.not_from_edge"),
        /** 3.6.1: his push was older than {@code pushWindowTicks} when the victim left the edge. */
        PUSH_TOO_OLD(null, "message.enhanced.vormir.why.too_old"),
        /** 3.6.1: the victim landed in the pit and lived (a totem of undying, or nothing could hurt it). */
        SURVIVED(null, "message.enhanced.vormir.why.survived");

        @Nullable
        private final String line;
        @Nullable
        private final String why;

        Outcome(@Nullable String line, @Nullable String why) {
            this.line = line;
            this.why = why;
        }

        /** The lang key of the Stonekeeper's line to the seeker, or null. */
        @Nullable
        public String line() {
            return line;
        }

        /** The lang key of the plain line that tells the seeker why it did not count, or null (3.6.1). */
        @Nullable
        public String why() {
            return why;
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
        return judge(victim, source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(ModDamageTypes.VORMIR_FALL));
    }

    /**
     * What the death (or, with {@code fell} and the creature alive, the landing on the pit's floor) of {@code victim}
     * comes to. A seeker is named for every outcome he is told of: the player whose push counts, else (3.6.1) the one
     * whose blow did not land (PvP off), whose push was too old, or who pushed it into the chasm from elsewhere.
     */
    public static Judgment judge(LivingEntity victim, boolean fell) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return new Judgment(Outcome.NOT_A_FALL, null, null);
        }
        long now = level.getGameTime();
        VormirTracker.Departure departure = VormirTracker.departure(victim);
        if (departure == null) {
            if (fell) {
                VormirSite site = VormirSites.at(level, victim.blockPosition());
                VormirTracker.Credit last = VormirTracker.lastCredit(victim);
                ServerPlayer pusher = site == null || !site.inPit(victim) || last == null
                        || now - last.tick() > VormirTracker.pushWindowTicks() + VormirTracker.deathWindowTicks()
                        ? null : online(level, last.pusher(), victim);
                if (pusher != null) {
                    return new Judgment(Outcome.NOT_FROM_EDGE, pusher, site);
                }
            }
            return new Judgment(Outcome.NOT_A_FALL, null, null);
        }
        VormirSite site = departure.site();
        if (!fell || now < departure.tick() || now - departure.tick() > VormirTracker.deathWindowTicks()
                || !site.inPit(victim) || VormirTracker.fallOf(victim, departure) < minFall()) {
            return new Judgment(Outcome.NOT_A_FALL, null, null);
        }
        VormirTracker.Credit credit = departure.credit();
        ServerPlayer seeker = credit == null ? null : online(level, credit.pusher(), victim);
        if (seeker != null) {
            return new Judgment(rules(seeker, victim), seeker, site);
        }
        if (credit == null) {
            // (Why there is no push: a player's push that was too old, or a blow that never landed.)
            VormirTracker.Credit last = VormirTracker.lastCredit(victim);
            ServerPlayer old = last == null || last.tick() > departure.tick() ? null
                    : online(level, last.pusher(), victim);
            if (old != null) {
                return new Judgment(Outcome.PUSH_TOO_OLD, old, site);
            }
            VormirTracker.Credit tried = VormirTracker.lastAttempt(victim);
            ServerPlayer attacker = tried == null || tried.tick() > departure.tick()
                    || departure.tick() - tried.tick() > VormirTracker.pushWindowTicks()
                    || last != null && last.tick() >= tried.tick() ? null : online(level, tried.pusher(), victim);
            if (attacker != null) {
                Outcome outcome = rules(attacker, victim);
                if (outcome != Outcome.SACRIFICE) {
                    return new Judgment(outcome, attacker, site);
                }
            }
        }
        return new Judgment(Outcome.NO_PUSH, null, site);
    }

    /** The online player {@code id} if he is not the victim himself, or null. */
    @Nullable
    private static ServerPlayer online(ServerLevel level, @Nullable UUID id, LivingEntity victim) {
        if (id == null || id.equals(victim.getUUID())) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(id);
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
        // (3.6.1: one who may not hold the stone bare earns it too: it waits for him on a pedestal by the pool.)
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
            tell(seeker, judgment.outcome(), victim);
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

    /**
     * The creature left the edge with a push and has landed alive on the pit's floor from the chasm's height (3.6.1: a
     * cat never takes fall damage, a horse takes half, a hero may take none, a friend in good armour lives). If it
     * counts, the fall takes it: it dies of the fall ({@code enhanced:vormir_fall}, which neither armour nor effects
     * soften; a totem of undying still saves). If it does not, the seeker is told why. Judged once.
     */
    static void onLanding(LivingEntity victim) {
        Judgment judgment = judge(victim, true);
        ServerPlayer seeker = judgment.seeker();
        if (judgment.outcome() == Outcome.SACRIFICE) {
            victim.invulnerableTime = 0;
            victim.hurt(ModDamageTypes.source(victim.level(), ModDamageTypes.VORMIR_FALL), Float.MAX_VALUE);
            if (victim.isAlive() && !victim.isDeadOrDying() && seeker != null) {
                tell(seeker, Outcome.SURVIVED, victim);
            }
        } else if (seeker != null) {
            tell(seeker, judgment.outcome(), victim);
        }
        VormirTracker.landed(victim.getUUID());
    }

    /**
     * The seeker hears why it did not count: the Stonekeeper's line (if he has one for it), then a short plain line
     * (3.6.1).
     */
    public static void tell(ServerPlayer seeker, Outcome outcome, LivingEntity victim) {
        Stonekeeper.say(seeker, outcome.line());
        String why = outcome.why();
        if (outcome == Outcome.NOT_LOVED && victim instanceof Player) {
            why = "message.enhanced.vormir.why.not_team_mate";
        }
        if (why != null) {
            Component line = outcome == Outcome.PUSH_TOO_OLD
                    ? Component.translatable(why, VormirTracker.pushWindowTicks() / 20)
                    : Component.translatable(why);
            seeker.sendSystemMessage(line.copy().withColor(WHY_COLOR));
        }
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
     * else at his feet; 3.6.1: one who may not hold it bare finds it on a pedestal by the pool, held for him, {@link
     * HeldSoulStone}): the keeper's line, an orange glow over his screen, the title, the sounds; the ledger and his
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
        BlockPos held = site == null || StoneRules.mayHold(seeker, StoneKind.SOUL) ? null
                : HeldSoulStone.place(level, site, seeker);
        if (held == null) {
            ItemStack stone = new ItemStack(ModItems.SOUL_STONE.get());
            StoneLedger.stamp(stone, seeker.getUUID());
            if (seeker.getMainHandItem().isEmpty()) {
                seeker.setItemInHand(InteractionHand.MAIN_HAND, stone);
            } else if (!seeker.getInventory().add(stone)) {
                seeker.drop(stone, false);
            }
        }
        StoneProgress progress = seeker.getData(ModAttachments.STONES).with(StoneProgress.SACRIFICED_FOR);
        // (The ledger's player mode keeps the same bit: set here too, so it holds whatever the ledger's state.)
        seeker.setData(ModAttachments.STONES, onePer() == SoulStoneConfig.OnePer.PLAYER
                ? progress.withTaken(StoneKind.SOUL, true) : progress);
        StoneLedger.taken(seeker, StoneKind.SOUL);
        Stonekeeper.say(seeker, Outcome.SACRIFICE.line());
        if (held != null) {
            // (3.6.1: he earned it, but may not carry it bare: it waits for him on the pedestal by the pool.)
            seeker.sendSystemMessage(Component.translatable("message.enhanced.vormir.held").withColor(ORANGE));
            seeker.sendSystemMessage(Component.translatable("message.enhanced.vormir.held_how").withColor(WHY_COLOR));
        }
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

    public static int minFall() {
        return loaded() ? SoulStoneConfig.MIN_FALL.get() : 30;
    }

    private Sacrifice() {
    }
}

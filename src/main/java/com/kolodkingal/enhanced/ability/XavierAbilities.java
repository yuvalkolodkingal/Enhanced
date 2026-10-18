package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.config.XavierConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.network.CerebroPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Professor X, Charles Xavier (3.3, lane XAVIER). He awakens in his wheelchair ({@code XavierWheelchairItem}) and
 * chooses his version, young (he walks: Hank's serum) or old (he sits in his chair); his powers are the same in both.
 *
 * <ul>
 *     <li><b>R, Mental Blast</b>: a blow of the mind at whoever he looks at: damage that ignores armour, dizziness and a
 *     slowing ({@code [professor_x].blast*}).</li>
 *     <li><b>G, Hear Thoughts</b>: Billy's The Noise, his way: every mind in earshot glows in its mood, whispers, and its
 *     thoughts are written over its head ({@link Thoughts}).</li>
 *     <li><b>V, Take Over a Body</b>: his mind goes into the creature (or, with PvP, the player) he looks at, and steers
 *     it; V again brings him back ({@link MindLink}).</li>
 *     <li><b>B, Freeze Minds</b>: two fingers to his temple and everyone around him stands still (X-Men: First Class, Days
 *     of Future Past), players only with PvP on.</li>
 *     <li><b>N, Cerebro</b>: the helmet; for ten seconds he sees every mind of a player far around him, through the world
 *     (X2).</li>
 * </ul>
 *
 * <p>No power of his touches a mind closed to him ({@link Telepathy}): Magneto's helmet, the bosses, a telepath; and
 * when he reaches into a stronger one, it throws him out.
 */
public final class XavierAbilities {
    /** The blue-white of Cerebro: the core, the main light, the body of a stream, its fringe. */
    public static final int CORE = 0xF2F6FF;
    public static final int MAIN = 0x9EB4FF;
    public static final int BODY = 0x6F8CFF;
    public static final int FRINGE = 0x3D4FB8;

    /** How long the two fingers stay at his temple after a Mental Blast or Freeze Minds. */
    public static final int FOCUS_TICKS = 16;
    /** Cerebro tells him what it finds this often. */
    static final int CEREBRO_PING = 20;

    /** What one Professor X is doing that lasts (game times). */
    private static final class State {
        long focusUntil;
        long cerebroUntil;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    // ------------------------------------------------------------------------------------------------------------
    // The version

    /** The ids of his two versions, in the order of the screen. */
    public static final String YOUNG = "young";
    public static final String OLD = "old";

    /**
     * He chose his version on the screen that opens when he awakens: the look is put on (a shimmer, as the Change Look
     * key gives). Anything but one of his looks, or anyone but Professor X, changes nothing.
     */
    public static void choose(ServerPlayer player, String version) {
        if (!player.isAlive() || Powers.power(player) != Power.PROFESSOR_X) {
            return;
        }
        Variant look = null;
        for (Variant v : Variants.of(Power.PROFESSOR_X)) {
            if (v.id().equals(version)) {
                look = v;
            }
        }
        if (look == null) {
            return;
        }
        Powers.setVariant(player, look);
        ServerLevel level = player.serverLevel();
        Powers.burst(level, player.getX(), player.getY() + 1.0, player.getZ(), look.color(), 40, 0.5);
        Powers.burst(level, player.getX(), player.getY() + 1.0, player.getZ(), look.glow(), 20, 0.3);
        player.displayClientMessage(Component.translatable("message.enhanced.look", look.displayName())
                .withStyle(s -> s.withColor(Power.PROFESSOR_X.color)), true);
    }

    /** Whether this player is the old Professor X, who sits in his wheelchair. */
    public static boolean seated(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.PROFESSOR_X && OLD.equals(Variants.current(data).id());
    }

    /** Whether his chair is drawn under him: not while he swims, sleeps, glides, rides or is a spectator. */
    public static boolean chairShown(Player player) {
        net.minecraft.world.entity.Pose pose = player.getPose();
        return !player.isSpectator() && !player.isPassenger() && !player.isFallFlying() && pose != net.minecraft.world.entity.Pose.SWIMMING
                && pose != net.minecraft.world.entity.Pose.SLEEPING && pose != net.minecraft.world.entity.Pose.SPIN_ATTACK
                && pose != net.minecraft.world.entity.Pose.DYING;
    }

    /** Whether this player sits in his chair now: the old Professor X, where the chair is shown. */
    public static boolean sits(Player player) {
        return seated(player) && chairShown(player);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input

    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        if (!down) {
            return;
        }
        if (MindLink.isLinked(player)) {
            // Away in another body: V brings him back, and his own body can do nothing else.
            if (ability == 3) {
                MindLink.end(player.server, player.getUUID(), MindLink.End.LET_GO);
            } else {
                player.displayClientMessage(Component.translatable("message.enhanced.xavier.away"), true);
            }
            return;
        }
        switch (ability) {
            case 1 -> mentalBlast(player, data);
            case 2 -> toggleThoughts(player, data);
            case 3 -> takeOver(player, data);
            case 4 -> freezeMinds(player, data);
            case 5 -> cerebro(player, data);
            default -> {
            }
        }
    }

    /** His own tick (alive): the link, the fingers at his temple, Cerebro. */
    public static void tick(ServerPlayer player, PowerData data) {
        MindLink.tick(player);
        long now = player.level().getGameTime();
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (data.is(Live.XAVIER_FOCUS) && now >= state.focusUntil) {
            data.set(Live.XAVIER_FOCUS, false);
        }
        if (data.is(Live.CEREBRO)) {
            if (now >= state.cerebroUntil) {
                data.set(Live.CEREBRO, false);
                player.connection.send(new CerebroPayload(List.of(), 0));
            } else if ((state.cerebroUntil - now) % CEREBRO_PING == 0) {
                ping(player, (int) (state.cerebroUntil - now));
            }
        }
    }

    /** Every player, every tick: the thoughts heard (Billy's too), and nobody left stuck by a link. */
    static void everyPlayerTick(ServerPlayer player, PowerData data) {
        Thoughts.tick(player, data);
        MindLink.watchdog(player, data);
    }

    /** The power is being taken away: he comes back to his body, the thoughts go quiet, Cerebro goes dark. */
    static void endAll(ServerPlayer player, PowerData data) {
        MindLink.end(player.server, player.getUUID(), MindLink.End.POWER_LOST);
        data.setToggle(Toggle.HEAR_THOUGHTS, false);
        data.set(Live.XAVIER_FOCUS, false);
        data.set(Live.CEREBRO, false);
        data.set(Live.MIND_LINKED, false);
        STATES.remove(player.getUUID());
    }

    /** He left, died or changed dimension: the link ends (whichever side of it he was on), Cerebro goes dark. */
    static void onGone(ServerPlayer player) {
        MindLink.onGone(player);
        PowerData data = Powers.get(player);
        data.set(Live.XAVIER_FOCUS, false);
        data.set(Live.CEREBRO, false);
        STATES.remove(player.getUUID());
    }

    /** Forget everything (the server stopped). */
    static void clear() {
        STATES.clear();
        MindLink.clear();
    }

    private static void focus(ServerPlayer player, PowerData data) {
        state(player).focusUntil = player.level().getGameTime() + FOCUS_TICKS;
        data.set(Live.XAVIER_FOCUS, true);
    }

    /** A living creature he can aim at: not himself, an armour stand, an illusion or a spectator. */
    private static boolean aimable(ServerPlayer player, net.minecraft.world.entity.Entity e) {
        return e instanceof LivingEntity living && living != player && living.isAlive() && !living.isSpectator()
                && !(living instanceof ArmorStand);
    }

    /** Why this mind does not answer him (a lang key), or null when it is open; a closed mind costs nothing. */
    static String closed(ServerPlayer player, LivingEntity target) {
        if (target instanceof LokiIllusionEntity) {
            return "message.enhanced.xavier.illusion";
        }
        return switch (Telepathy.reach(player, target)) {
            case OPEN -> null;
            case SHIELDED -> "message.enhanced.xavier.shielded";
            case TELEPATH, THROWS_OUT -> "message.enhanced.xavier.telepath";
        };
    }

    /** Whether he may hurt or hold a player with a power of the mind: PvP on, and he may harm them. */
    static boolean mayReach(ServerPlayer player, Player other) {
        return !other.isCreative() && !other.isSpectator() && player.server.isPvpAllowed() && player.canHarmPlayer(other);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 1: the Mental Blast

    static void mentalBlast(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.XAVIER_MENTAL_BLAST, now)) {
            return;
        }
        EntityHitResult hit = Raycasts.entity(player, XavierConfig.BLAST_RANGE.get(), 0.3, e -> aimable(player, e), 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.xavier.nothing"), true);
            return;
        }
        LivingEntity target = (LivingEntity) hit.getEntity();
        String refusal = closed(player, target);
        if (refusal == null && target instanceof Player other && !mayReach(player, other)) {
            refusal = "message.enhanced.xavier.no_pvp";
        }
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal), true);
            return;
        }
        float damage = XavierConfig.BLAST_DAMAGE.get().floatValue();
        if (damage > 0.0F) {
            target.hurt(ModDamageTypes.source(level, ModDamageTypes.MIND_BLAST, player, player), damage);
        }
        int dizzy = XavierConfig.BLAST_DIZZY_TICKS.get();
        if (dizzy > 0 && target.isAlive()) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, dizzy + 40, 0, false, true, true), player);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dizzy, 1, false, true, true), player);
        }
        data.startCooldown(HeroAbility.XAVIER_MENTAL_BLAST, now, XavierConfig.BLAST_COOLDOWN.get());
        focus(player, data);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.XAVIER_BLAST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        ripple(level, player, target);
    }

    /** The blast's ripple: rings of motes travelling from his temple to the target, and a flash at its head. */
    private static void ripple(ServerLevel level, ServerPlayer from, LivingEntity to) {
        Vec3 start = from.getEyePosition().add(from.getLookAngle().scale(0.4));
        Vec3 end = to.getEyePosition();
        Vec3 delta = end.subtract(start);
        int steps = (int) Math.min(30, delta.length() * 1.5);
        for (int i = 1; i <= steps; i++) {
            Vec3 p = start.add(delta.scale(i / (double) steps));
            level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(from, HeroAbility.XAVIER_MENTAL_BLAST, i % 3 == 0 ? CORE : MAIN), 0.9F),
                    p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.0);
        }
        level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(from, HeroAbility.XAVIER_MENTAL_BLAST, CORE),
                1.4F), end.x, end.y + 0.2, end.z,
                14, 0.3, 0.2, 0.3, 0.08);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 2: Hear Thoughts

    private static void toggleThoughts(ServerPlayer player, PowerData data) {
        boolean on = !data.toggle(Toggle.HEAR_THOUGHTS);
        data.setToggle(Toggle.HEAR_THOUGHTS, on);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.xavier.thoughts.on" : "message.enhanced.xavier.thoughts.off"), true);
        if (on) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.XAVIER_THOUGHTS.get(),
                    SoundSource.PLAYERS, 0.7F, 1.0F);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 3: Take Over a Body

    private static void takeOver(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.XAVIER_TAKE_OVER, now)) {
            return;
        }
        EntityHitResult hit = Raycasts.entity(player, XavierConfig.LINK_RANGE.get(), 0.3, e -> aimable(player, e), 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.xavier.nothing"), true);
            return;
        }
        MindLink.take(player, (LivingEntity) hit.getEntity());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 4: Freeze Minds

    /**
     * Everything with an open mind within {@code freezeRadius} (through walls: it is the mind) is held still for {@code
     * freezeTicks} (the Spellbound of Billy's Freeze). Not held: his pets, his team, a closed mind, the creatures too big
     * for a spell, and players without PvP. If nobody is held it costs nothing.
     */
    static int freezeMinds(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.XAVIER_FREEZE_MINDS, now)) {
            return 0;
        }
        double radius = XavierConfig.FREEZE_RADIUS.get();
        int ticks = XavierConfig.FREEZE_TICKS.get();
        int held = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> aimable(player, e) && e.distanceToSqr(player) <= radius * radius)) {
            if (!freezes(player, e)) {
                continue;
            }
            if (!e.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, ticks, 0, false, false, true), player)) {
                continue;
            }
            held++;
            if (e instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(player, HeroAbility.XAVIER_FREEZE_MINDS, CORE), 1.0F), e.getX(),
                    e.getEyeY() + 0.3, e.getZ(), 6, 0.25, 0.15, 0.25, 0.02);
        }
        if (held == 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.xavier.freeze.nothing"), true);
            return 0;
        }
        data.startCooldown(HeroAbility.XAVIER_FREEZE_MINDS, now, XavierConfig.FREEZE_COOLDOWN.get());
        focus(player, data);
        player.displayClientMessage(Component.translatable("message.enhanced.xavier.freeze", held), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.XAVIER_FREEZE.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.XAVIER_FREEZE_MINDS, MAIN), 1.4F), player.getX(),
                player.getEyeY(), player.getZ(), 30, 0.6, 0.4, 0.6, 0.25);
        return held;
    }

    /** Whether Freeze Minds holds this one. */
    static boolean freezes(ServerPlayer player, LivingEntity e) {
        if (e instanceof LokiIllusionEntity || player.isAlliedTo(e) || e.getType().is(ModTags.SPELLBOUND_IMMUNE)
                || !Telepathy.isOpen(player, e) || MindLink.isHost(e)) {
            return false;
        }
        if (e instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID())) {
            return false;
        }
        return !(e instanceof Player other) || mayReach(player, other);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 5: Cerebro

    /**
     * The helmet goes on and Cerebro searches: every player in his dimension within {@code cerebroRange} whose mind is
     * not shut by Magneto's helmet is found (a telepath too: Cerebro amplifies), marked on his screen through the world for
     * {@code cerebroTicks}, and feels it ("Cerebro has found you").
     */
    static void cerebro(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.XAVIER_CEREBRO, now)) {
            return;
        }
        int ticks = XavierConfig.CEREBRO_TICKS.get();
        State state = state(player);
        state.cerebroUntil = now + ticks;
        data.set(Live.CEREBRO, true);
        data.startCooldown(HeroAbility.XAVIER_CEREBRO, now, XavierConfig.CEREBRO_COOLDOWN.get());
        List<CerebroPayload.Mind> found = ping(player, ticks);
        for (ServerPlayer other : level.players()) {
            if (other != player && found.stream().anyMatch(m -> m.name().equals(other.getGameProfile().getName()))) {
                other.displayClientMessage(Component.translatable("message.enhanced.xavier.cerebro.found_you", player.getDisplayName()), true);
            }
        }
        player.displayClientMessage(Component.translatable("message.enhanced.xavier.cerebro", found.size()), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.XAVIER_CEREBRO.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
    }

    /** Who Cerebro finds now (the nearest first), sent to him. */
    static List<CerebroPayload.Mind> ping(ServerPlayer player, int ticksLeft) {
        List<CerebroPayload.Mind> minds = found(player);
        player.connection.send(new CerebroPayload(minds, ticksLeft));
        return minds;
    }

    /** The minds Cerebro finds around him: players in his dimension, in range, not Spectators, not under Magneto's helmet. */
    public static List<CerebroPayload.Mind> found(ServerPlayer player) {
        double range = XavierConfig.CEREBRO_RANGE.get();
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && other.isAlive() && !other.isSpectator() && !Telepathy.isShielded(other)
                    && other.distanceToSqr(player) <= range * range) {
                near.add(other);
            }
        }
        near.sort(Comparator.comparingDouble(o -> o.distanceToSqr(player)));
        List<CerebroPayload.Mind> minds = new ArrayList<>();
        for (ServerPlayer other : near) {
            if (minds.size() >= CerebroPayload.MAX) {
                break;
            }
            minds.add(new CerebroPayload.Mind(other.getGameProfile().getName(), Powers.power(other).ordinal(), other.position()));
        }
        return minds;
    }

    private XavierAbilities() {
    }
}

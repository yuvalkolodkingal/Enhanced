package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossBars;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * The fight against the Mad Titan: a portal opens in the sky and a column of blue light marks where he will land, he
 * arrives when the countdown ends, and the fight is his clock of stones (see {@link ThanosEntity}) until he falls,
 * everyone is gone or the framework ends it. The framework ({@link BossFight}) keeps the participants, the ledger, the
 * wipe, the abandonment, the rest, the rewards and the bars; this class is what is Thanos's own:
 *
 * <ul>
 *     <li><b>The call.</b> Everyone within 160 blocks is told with a title; the countdown marks the landing; then he
 *     is made and falls through the portal.</li>
 *     <li><b>The bar.</b> His health, in the colour of his newest stone; its name turns to "Only the Mind Stone remains"
 *     at five stones and to "Enraged" when his damage grows. The client draws the six sockets from the entity.</li>
 *     <li><b>The end.</b> Beaten, he is rewarded at once (one Infinity Gauntlet for each fighter who did 3 percent of his
 *     health, or struck the last blow) while he kneels and turns to dust; any other end sends him through a ring of Space
 *     and rests him for half a time. The ritual consumed nothing, so there is nothing to refund.</li>
 * </ul>
 */
public final class ThanosFight extends BossFight {
    /** What the fight is doing. */
    public enum Phase {
        /** The portal is open and the countdown runs. */
        COUNTDOWN,
        /** He is on his way down or rising from the landing. */
        ARRIVING,
        /** The fight. */
        FIGHT,
        /** He fell: the dust and the reward. */
        FALLEN,
        /** He has left (nobody was left to fight, or the fight was ended). */
        LEAVING
    }

    /** How long he takes to go through the ring of Space when he leaves. */
    private static final int LEAVING_TICKS = 30;
    /** He is put back when he is this far under the centre of his arena (the void hurts far below, in every dimension). */
    private static final double VOID_DEPTH = 24.0;
    /** Brought back, he stands at most this far inside his radius. */
    private static final double BRING_BACK_INSIDE = 4.0;
    /** How high above the landing the portal hangs. */
    private static final double PORTAL_HEIGHT = 30.0;

    private final Vec3 landing;
    private final double fallHeight;
    private Phase phase = Phase.COUNTDOWN;
    private int countdown;
    private int leaving;
    private boolean fiveShown;
    @Nullable
    private ThanosEntity boss;

    private ThanosFight(ServerLevel level, BossArena arena, Vec3 landing, int countdown, double fallHeight) {
        super(level, arena, new BossBars(Component.translatable("boss.enhanced.thanos"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS));
        this.landing = landing;
        this.countdown = countdown;
        this.fallHeight = fallHeight;
    }

    /**
     * A fight round {@code center}: no containment, no protection of blocks (nothing he does changes one), the clock and
     * the leash reaching 24 blocks beyond the arena.
     *
     * @param landing   where he lands
     * @param countdown the ticks from the portal opening to his arrival (0: he is made at once)
     * @param fallHeight how far above the landing he is made (0: standing on it)
     */
    public static ThanosFight create(ServerLevel level, Vec3 center, Vec3 landing, int countdown, double fallHeight) {
        double radius = ThanosBossConfig.ARENA_RADIUS.get();
        BossArena arena = new BossArena(level, center, radius, BossArena.Containment.NONE, false)
                .joinWithin(radius + 24.0).abandonBeyond(radius + 24.0).tractorDepth(0.0);
        return new ThanosFight(level, arena, landing, countdown, fallHeight);
    }

    /**
     * Starts a fight at once with Thanos made at {@code pos}, standing: no portal, no countdown (what the tests and
     * {@code /enhanced boss start thanos force} on a roofed place need). He still arrives (60 ticks, invulnerable).
     */
    @VisibleForTesting
    public static ThanosFight spawnAt(ServerLevel level, Vec3 pos, ServerPlayer summoner) {
        ThanosFight fight = create(level, pos, pos, 0, 0.0);
        BossFights.begin(fight, summoner);
        return fight;
    }

    @Override
    public BossKind kind() {
        return BossKind.THANOS;
    }

    public Phase phase() {
        return phase;
    }

    /** Thanos, once he has been made (null before the countdown ends). */
    @Nullable
    public ThanosEntity boss() {
        return boss;
    }

    public Vec3 landing() {
        return landing;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The call

    @Override
    protected void onBegin(ServerPlayer starter) {
        bars().health().setVisible(false);
        ServerLevel level = level();
        Vec3 center = arena().center();
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(center) <= 160.0 * 160.0) {
                title(player, Component.translatable("message.enhanced.thanos.summon.title").withColor(StoneKind.POWER.color()).withStyle(ChatFormatting.BOLD),
                        Component.translatable("message.enhanced.thanos.summon.subtitle").withStyle(ChatFormatting.GRAY));
            }
        }
        ThanosSounds.SUMMON.play(level, center);
        if (countdown > 0) {
            ThanosEffects.portalSky(level, landing, countdown);
            tell(Component.translatable("message.enhanced.thanos.coming").withColor(StoneKind.SPACE.color()), true);
        } else {
            makeBoss();
        }
    }

    /** A title and its subtitle for one player. */
    static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** Makes him: above the landing, falling through the portal. */
    private void makeBoss() {
        ThanosEntity thanos = new ThanosEntity(ModEntities.THANOS.get(), level());
        thanos.moveTo(landing.x, landing.y + fallHeight, landing.z, 0.0F, 0.0F);
        thanos.attach(this);
        adopt(thanos);
        level().addFreshEntity(thanos);
        thanos.beginArrival();
        boss = thanos;
        phase = Phase.ARRIVING;
        if (fallHeight > 0.0) {
            ThanosEffects.portal(level(), landing.add(0, fallHeight, 0), 4.0F, 30);
        }
        bars().health().setVisible(true);
    }

    /** The landing is over and he has risen: the fight begins. */
    void arrived() {
        phase = Phase.FIGHT;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The fight

    @Override
    protected void onTick() {
        if (phase == Phase.COUNTDOWN) {
            if (--countdown <= 0) {
                makeBoss();
            } else if (countdown % 20 == 0) {
                ThanosEffects.ringTell(level(), landing, 3.0F, 20, StoneKind.SPACE.color());
            }
            return;
        }
        if (boss == null) {
            return;
        }
        if (boss.isRemoved() || !boss.isAlive() && boss.state() != ThanosState.DYING) {
            // Killed from outside (/kill), or removed: no victory.
            end(BossEnd.ABORT);
            return;
        }
        if (boss.getY() < arena().center().y - VOID_DEPTH && boss.state() != ThanosState.DYING) {
            // Thrown off the edge of the world's ground (an End island): he steps back before the void can kill him.
            BossGrip.forget(boss);
            bringBack(boss);
        }
        updateBars(boss);
    }

    // ---- telekinesis (3.1, D-T 6.2.1) ----

    /** {@code [thanos_boss].holdTicks}. */
    @Override
    public int holdTicks() {
        return ThanosBossConfig.HOLD_TICKS.get();
    }

    /**
     * Thanos thrown out of his arena, or fallen too far under it, steps back through a Space portal: rings at both ends,
     * and he stands on the ground at most {@link #BRING_BACK_INSIDE} blocks inside his radius (where he was, if he was
     * already that far in), stopped.
     */
    @Override
    protected void bringBack(Entity entity) {
        if (!(entity instanceof ThanosEntity thanos)) {
            return;
        }
        Vec3 c = arena().center();
        double dx = thanos.getX() - c.x;
        double dz = thanos.getZ() - c.z;
        double flat = Math.hypot(dx, dz);
        double reach = Math.max(1.0, arena().radius() - BRING_BACK_INSIDE);
        double scale = flat > reach ? reach / flat : 1.0;
        double x = c.x + dx * scale;
        double z = c.z + dz * scale;
        Vec3 spot = groundUnder(thanos, x, z);
        Vec3 from = thanos.position();
        ThanosEffects.portal(level(), from.add(0, 0.2, 0), 1.6F, 20);
        ThanosEffects.portal(level(), spot.add(0, 0.2, 0), 1.6F, 20);
        thanos.moveTo(spot.x, spot.y, spot.z, thanos.getYRot(), thanos.getXRot());
        thanos.setDeltaMovement(Vec3.ZERO);
        thanos.resetFallDistance();
        thanos.getNavigation().stop();
        ThanosSounds.PORTAL.play(level(), spot, 1.0F, 1.2F);
    }

    /** Where he stands in the column of {@code (x, z)}: the first block he can stand on, looking down from above the centre. */
    private Vec3 groundUnder(ThanosEntity thanos, double x, double z) {
        double top = arena().center().y + 6.0;
        BlockHitResult hit = level().clip(new ClipContext(new Vec3(x, top, z), new Vec3(x, top - 46.0, z), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, thanos));
        Vec3 spot = hit.getType() == HitResult.Type.MISS ? new Vec3(x, arena().center().y, z) : hit.getLocation();
        return Teleports.fits(thanos, spot) ? spot : spot.add(0, 1.0, 0);
    }

    /** The bar shows his health in the colour of his newest stone, and says what he has become. */
    private void updateBars(ThanosEntity thanos) {
        ServerBossEvent bar = bars().health();
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, thanos.getHealth() / thanos.getMaxHealth())));
        BossEvent.BossBarColor color = thanos.newestStone().barColor();
        if (bar.getColor() != color) {
            bar.setColor(color);
        }
        String key = thanos.flag(ThanosEntity.FLAG_ENRAGED) ? "boss.enhanced.thanos.enraged"
                : thanos.stones() >= StoneKind.granted().size() ? "boss.enhanced.thanos.five" : "boss.enhanced.thanos";
        if (!(bar.getName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name) || !name.getKey().equals(key)) {
            bar.setName(Component.translatable(key));
        }
    }

    /** A new stone has fused: a title for everyone in the arena, in the stone's colour. */
    void stoneArrived(StoneKind stone, ThanosEntity thanos) {
        for (ServerPlayer player : present()) {
            title(player, Component.translatable("stone.enhanced." + stone.id()).withColor(stone.color()).withStyle(ChatFormatting.BOLD),
                    Component.translatable("message.enhanced.thanos.stone.subtitle").withStyle(ChatFormatting.GRAY));
        }
        if (thanos.stones() >= StoneKind.granted().size() && !fiveShown) {
            fiveShown = true;
            tell(Component.translatable("boss.enhanced.thanos.five").withStyle(ChatFormatting.YELLOW), false);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The end

    /** He is down: the last blow is the one that did it, and the fight is won. */
    void bossFell(@Nullable ServerPlayer lastBlow) {
        phase = Phase.FALLEN;
        if (lastBlow != null) {
            lastBlow(lastBlow);
        }
        end(BossEnd.VICTORY);
    }

    /** He died some other way (an operator's kill): the fight is over without a winner. */
    void bossLost() {
        end(BossEnd.ABORT);
    }

    /** The dust has settled. */
    void bossGone() {
        phase = Phase.LEAVING;
    }

    @Override
    protected List<ItemStack> rewards(Participant participant) {
        ServerPlayer player = server().getPlayerList().getPlayer(participant.id());
        if (player != null && player.isAlive()) {
            title(player, Component.translatable("message.enhanced.thanos.reward").withColor(StoneKind.POWER.color()).withStyle(ChatFormatting.BOLD),
                    Component.translatable("item.enhanced.infinity_gauntlet").withStyle(ChatFormatting.GRAY));
        }
        return List.of(new ItemStack(ModItems.INFINITY_GAUNTLET.get()));
    }

    /** Three percent of his health: what earns a Gauntlet together with the time in the fight. */
    @Override
    protected float rewardMinDamage() {
        // (The ledger is in the bar's own points, so the share is of the bar.)
        return (float) (ThanosEntity.REAL_HEALTH * ThanosBossConfig.REWARD_MIN_SHARE.get());
    }

    /** The rest: a full one after a victory, half after he leaves for lack of fighters. */
    @Override
    protected long restTicks(BossEnd why) {
        long rest = ThanosBossConfig.SUMMON_COOLDOWN_TICKS.get();
        return switch (why) {
            case VICTORY -> rest;
            case WIPE, ABANDON -> rest / 2;
            default -> 0L;
        };
    }

    @Override
    protected void onEnd(BossEnd why) {
        if (why == BossEnd.VICTORY || boss == null || boss.isRemoved()) {
            return;
        }
        // He goes through a ring of Space.
        phase = Phase.LEAVING;
        leaving = 0;
        ThanosEffects.portal(level(), boss.position().add(0, 1.5, 0), 3.0F, LEAVING_TICKS);
        ThanosSounds.PORTAL.play(level(), boss.position(), 1.2F, 0.8F);
        boss.interruptForLeaving();
        if (why == BossEnd.ABANDON) {
            tell(Component.translatable("message.enhanced.thanos.left").withStyle(ChatFormatting.GRAY), false);
        }
    }

    @Override
    protected boolean windDown(int ticks) {
        if (phase == Phase.COUNTDOWN || boss == null) {
            return true;
        }
        if (result() == BossEnd.VICTORY) {
            return boss.isRemoved();
        }
        return ++leaving >= LEAVING_TICKS || boss.isRemoved();
    }

    @Override
    protected void onFinished(boolean forced) {
        if (boss != null && !boss.isRemoved()) {
            boss.discard();
        }
    }

    @Override
    protected String status() {
        if (isOver()) {
            return "ending (" + result() + ")";
        }
        return switch (phase) {
            case COUNTDOWN -> "portal open, he lands in " + countdown / 20 + " s";
            case ARRIVING -> "arriving";
            case FIGHT, FALLEN, LEAVING -> boss == null ? "fighting" : "stones " + boss.stones() + ", health "
                    + (int) boss.points() + "/" + (int) boss.maxPoints() + ", " + boss.state().name().toLowerCase(java.util.Locale.ROOT);
        };
    }
}

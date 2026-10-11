package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossBars;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.BossLane;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The stand-ins for the boss lanes: a {@link BossFight} that does nothing but write down what the framework did to it,
 * an arena whose ground is the test floor, and a {@link BossLane} that makes them. With these the framework's fights,
 * wipes, rewards, gifts and arena rules are tested without a real boss.
 */
final class BossTestSupport {

    /** A fight that records every call the framework makes, and lets a test choose what the lane would answer. */
    static class TestFight extends BossFight {
        private final BossKind kind;
        final List<String> log = new ArrayList<>();
        /** How many ticks the ending takes (the lane's wind-down). */
        int windDownNeeded;
        int ticks;
        int finishedCalls;
        boolean forced;
        boolean creditsEvents = true;
        boolean pressHandled;
        boolean launchHandled;
        int pressed;
        int launched;
        float minDamage = Float.MIN_VALUE;
        Set<String> counters = Set.of(DAMAGE);
        long rest;
        List<ItemStack> reward = List.of(new ItemStack(Items.DIAMOND));
        @Nullable
        BossEnd ended;

        TestFight(ServerLevel level, BossKind kind, BossArena arena) {
            super(level, arena, new BossBars(Component.translatable("boss.enhanced." + kind.id()), BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.NOTCHED_6));
            this.kind = kind;
        }

        @Override
        public BossKind kind() {
            return kind;
        }

        @Override
        protected void onBegin(ServerPlayer starter) {
            log.add("begin");
        }

        @Override
        protected void onTick() {
            ticks++;
        }

        @Override
        protected List<ItemStack> rewards(Participant participant) {
            log.add("reward:" + participant.name());
            return reward.stream().map(ItemStack::copy).toList();
        }

        @Override
        protected float rewardMinDamage() {
            return minDamage;
        }

        @Override
        protected Set<String> rewardCounters() {
            return counters;
        }

        @Override
        protected boolean creditsDamageEvents() {
            return creditsEvents;
        }

        @Override
        protected long restTicks(BossEnd why) {
            return rest;
        }

        @Override
        protected void onEnd(BossEnd why) {
            ended = why;
            log.add("end:" + why);
        }

        @Override
        protected boolean windDown(int ticks) {
            return ticks >= windDownNeeded;
        }

        @Override
        protected void onFinished(boolean forced) {
            finishedCalls++;
            this.forced = forced;
            log.add("finished:" + forced);
        }

        @Override
        protected boolean onTelekinesisPress(ServerPlayer wanda) {
            pressed++;
            return pressHandled;
        }

        @Override
        protected boolean onTelekinesisLaunch(ServerPlayer wanda, Entity held) {
            launched++;
            return launchHandled;
        }

        @Override
        protected String status() {
            return "test " + (ended == null ? "running" : ended);
        }
    }

    /**
     * An arena whose ground is the floor of the test, for the tractor and the field: the real one looks for the sky.
     */
    static final class TestArena extends BossArena {
        TestArena(ServerLevel level, Vec3 center, double radius, Containment containment, boolean protectBlocks) {
            super(level, center, radius, containment, protectBlocks);
        }

        @Override
        protected Vec3 surfaceSpot(ServerPlayer player, double x, double z) {
            return new Vec3(x, center().y, z);
        }
    }

    /** A lane that makes {@link TestFight}s, counts its calls and can refuse. */
    static final class TestLane implements BossLane {
        final BossKind kind;
        final Function<ServerPlayer, TestFight> maker;
        @Nullable
        Component refusal;
        boolean returnsNull;
        int refusals;
        int creates;
        boolean lastForce;
        final List<TestFight> made = new ArrayList<>();

        TestLane(BossKind kind, Function<ServerPlayer, TestFight> maker) {
            this.kind = kind;
            this.maker = maker;
        }

        @Nullable
        @Override
        public Component refusal(ServerPlayer starter, boolean force) {
            refusals++;
            lastForce = force;
            return refusal;
        }

        @Nullable
        @Override
        public BossFight create(ServerPlayer starter, boolean force) {
            creates++;
            lastForce = force;
            if (returnsNull) {
                return null;
            }
            TestFight fight = maker.apply(starter);
            made.add(fight);
            return fight;
        }
    }

    /** The translation key of the last action-bar line the player was sent since the last call, or null. */
    @Nullable
    static String lastActionBarKey(ServerPlayer player) {
        String key = null;
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay() && chat.content().getContents() instanceof TranslatableContents translatable) {
                key = translatable.getKey();
            }
        }
        return key;
    }

    private BossTestSupport() {
    }
}

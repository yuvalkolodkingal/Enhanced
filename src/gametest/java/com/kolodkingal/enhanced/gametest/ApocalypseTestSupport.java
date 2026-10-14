package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What the Apocalypse tests share: a fixture that shrinks the arena to fit the 21-wide test floor and keeps the server
 * quiet, a way to start a fight at the middle of the floor and jump to a stage of it, and the small things every test of
 * the boss needs (a player the test cannot kill by accident, a boss that does nothing until told, what a player was sent).
 */
final class ApocalypseTestSupport {
    /** The middle of the test floor: the fight is centred there. */
    static final Vec3 MIDDLE = new Vec3(10.5, FLOOR, 10.5);

    /**
     * A boss fixture whose arena has a radius of 9 (so the wall at 13 and the protected cylinder at 17 are far outside the
     * 21-wide floor), with the announcements off and the breaker rule off (a lone Survival player can start the ritual).
     */
    static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.config(ApocalypseBossConfig.ARENA_RADIUS, 9);
        return f;
    }

    /**
     * Sets the three factors of the critical hits ({@code critShieldFactor}, {@code critBodyFactor}, {@code
     * othersBodyFactor}) to 1.0, so that every blow counts as in 3.0: for a test whose subject is another rule (his
     * armour, explosions, the window's quarter, the floors, the cap, the bars, his healing, the Escape, the strains
     * counted once a cast; spec 8a.2). The test needs a batch of its own.
     */
    static void noCrits(TestKit.Fixture f) {
        f.config(ApocalypseShieldConfig.CRIT_SHIELD_FACTOR, 1.0);
        f.config(ApocalypseShieldConfig.CRIT_BODY_FACTOR, 1.0);
        f.config(ApocalypseShieldConfig.OTHERS_BODY_FACTOR, 1.0);
    }

    /** Starts the fight by an operator's command: it is centred at the starter's feet, and no Capstone is used. */
    static ApocalypseFight start(TestKit.Fixture f, ServerPlayer starter) {
        check(BossFights.start(BossKind.APOCALYPSE, starter, true), "the Apocalypse fight starts");
        return fightOf(f);
    }

    /** The Apocalypse fight that runs in the test's level. */
    static ApocalypseFight fightOf(TestKit.Fixture f) {
        check(BossFights.active(f.level()) instanceof ApocalypseFight, "an Apocalypse fight runs");
        return (ApocalypseFight) BossFights.active(f.level());
    }

    /** Starts the fight and jumps to a phase of it: he is there, his shield up, his health where the phase puts it. */
    static ApocalypseFight inPhase(TestKit.Fixture f, ServerPlayer starter, int phase) {
        ApocalypseFight fight = start(f, starter);
        fight.forceState(ApocalypseFight.Stage.FIGHT, phase);
        calm(fight);
        return fight;
    }

    static ApocalypseEntity boss(ApocalypseFight fight) {
        ApocalypseEntity boss = fight.boss();
        check(boss != null, "the boss is there");
        return boss;
    }

    /**
     * Runs {@code step} once a tick for ticks 1 to {@code lastTick}; returning true ends the test successfully, and if it
     * has not by the last tick the test fails. Unlike {@link TestKit.Fixture#everyTick} it keeps one entry at a time in the
     * test's schedule (each tick schedules the next): the schedule is a hash map that is iterated while its entries run, and
     * a callback that adds many entries to it, or adds one when it is just full, breaks the iteration (the server crashes
     * with a NullPointerException inside {@code GameTestInfo.tickInternal}). So a test that polls for a long time and also
     * schedules from inside its steps uses this one.
     */
    static void every(GameTestHelper helper, int lastTick, IntPredicate step) {
        schedule(helper, 1, lastTick, step);
    }

    private static void schedule(GameTestHelper helper, int tick, int lastTick, IntPredicate step) {
        helper.runAfterDelay(1, () -> {
            if (step.test(tick)) {
                helper.succeed();
            } else if (tick >= lastTick) {
                throw new GameTestAssertException("Test did not finish within " + lastTick + " ticks");
            } else {
                schedule(helper, tick + 1, lastTick, step);
            }
        });
    }

    /**
     * Runs {@code then} in the first tick in which {@code ready} is true (it is asked once a tick), and fails the test if that
     * has not happened in {@code maxTicks}: for what the server learns from another thread (the light engine knows of a new
     * roof only when its own thread has had its turn, and a test server ticks as fast as it can, so a fixed number of ticks
     * can be over in a few milliseconds and is a flake). Each tick that finds it not ready gives the other threads ten
     * milliseconds of the server thread's time.
     */
    static void whenReady(GameTestHelper helper, int maxTicks, BooleanSupplier ready, String what, Runnable then) {
        awaitStep(helper, 1, maxTicks, ready, what, then);
    }

    private static void awaitStep(GameTestHelper helper, int tick, int maxTicks, BooleanSupplier ready, String what, Runnable then) {
        helper.runAfterDelay(1, () -> {
            if (ready.getAsBoolean()) {
                then.run();
            } else if (tick >= maxTicks) {
                throw new GameTestAssertException("Not " + what + " within " + maxTicks + " ticks");
            } else {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                awaitStep(helper, tick + 1, maxTicks, ready, what, then);
            }
        });
    }

    /** Makes a player unlikely to die of what a test does to them (a thousand health). */
    static void tank(ServerPlayer player) {
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        player.setHealth(1000.0F);
    }

    /**
     * He chooses no attack by himself and does not walk: only what a test forces happens. (His speed is nothing; a change of
     * form puts it back, so a test that changes it calls this again.)
     */
    static void calm(ApocalypseFight fight) {
        ApocalypseEntity boss = fight.boss();
        if (boss != null) {
            boss.brain().setChoosing(false);
            boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        }
    }

    /** A hero, the fight and the boss: the boss calm at the middle, the hero seven blocks west of him looking at him. */
    record Duel(TestKit.Fixture f, ServerPlayer hero, ApocalypseFight fight, ApocalypseEntity boss) {
        ServerLevel level() {
            return f.level();
        }

        /** A damage source of this type, the hero the cause of it. */
        DamageSource by(ResourceKey<DamageType> type) {
            return ModDamageTypes.source(level(), type, hero, hero);
        }

        /** Hurts him as the hero would, with no invulnerability frames in the way. */
        boolean hit(ResourceKey<DamageType> type, float amount) {
            boss.invulnerableTime = 0;
            return boss.hurt(by(type), amount);
        }
    }

    /** A duel in the given phase (the shield up at full strength, health where the phase puts it) with a hero of this power. */
    static Duel duel(GameTestHelper helper, int phase, Power power) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer hero = f.player(MIDDLE.x, FLOOR, MIDDLE.z, power);
        tank(hero);
        ApocalypseFight fight = inPhase(f, hero, phase);
        ApocalypseEntity boss = boss(fight);
        put(f, hero, 3.5, FLOOR, 10.5);
        TestKit.lookAt(hero, boss);
        return new Duel(f, hero, fight, boss);
    }

    /** Breaks the boss's shield with one blow of mind power from the hero (who must be a breaker). */
    static void breakShield(Duel duel) {
        check(duel.boss().shield().isUp(), "the shield is up to be broken");
        duel.hit(ModDamageTypes.CHAOS_BEAM, duel.boss().shield().strength() + 1.0F);
        check(!duel.boss().shield().isUp(), "and one blow broke it: " + duel.boss().shield().state());
    }

    /** Puts an entity at a spot of the test (relative coordinates), at rest. */
    static void put(TestKit.Fixture f, Entity entity, double x, double y, double z) {
        Vec3 at = f.helper().absoluteVec(new Vec3(x, y, z));
        entity.moveTo(at.x, at.y, at.z, entity.getYRot(), entity.getXRot());
        entity.setDeltaMovement(Vec3.ZERO);
    }

    /** Where an entity is, in the test's relative coordinates. */
    static Vec3 rel(TestKit.Fixture f, Entity entity) {
        return f.helper().relativeVec(entity.position());
    }

    /** A source that is a plain blow of this kind of damage by nobody, for the shield's "everything else" tests. */
    static DamageSource fire(TestKit.Fixture f) {
        return f.level().damageSources().onFire();
    }

    /**
     * What a player has been sent, kept across ticks (the fake connection hands each packet over once): the chat lines
     * and sounds the tests look for.
     */
    static final class Log {
        private final ServerPlayer player;
        private final List<Object> packets = new ArrayList<>();

        Log(ServerPlayer player) {
            this.player = player;
        }

        /** Takes what has been sent since the last look. */
        Log drain() {
            packets.addAll(TestKit.sentPackets(player));
            return this;
        }

        /** Whether a chat line (not the action bar) with this translation key, in its text or in any part of it, was sent. */
        boolean said(String key) {
            return timesSaid(key) > 0;
        }

        /** How many chat lines (not the action bar) with this translation key, in their text or in any part of it, were sent. */
        int timesSaid(String key) {
            int times = 0;
            for (Object packet : packets) {
                if (packet instanceof ClientboundSystemChatPacket chat && !chat.overlay() && mentions(chat.content(), key)) {
                    times++;
                }
            }
            return times;
        }

        /** Whether an action-bar line with this translation key was sent. */
        boolean toldOnActionBar(String key) {
            for (Object packet : packets) {
                if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay() && mentions(chat.content(), key)) {
                    return true;
                }
            }
            return false;
        }

        /** The translation keys of the action-bar lines that were sent, oldest first (for the messages of a failure). */
        List<String> actionBarKeys() {
            List<String> keys = new ArrayList<>();
            for (Object packet : packets) {
                if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay()) {
                    keys.add(chat.content().getContents() instanceof TranslatableContents translatable ? translatable.getKey() : chat.content().getString());
                }
            }
            return keys;
        }

        /** Whether a sound of this event was sent. */
        boolean heard(SoundEvent event) {
            return TestKit.soundsIn(packets).stream().anyMatch(s -> s.id().equals(event.getLocation()));
        }

        /** Whether the server told the client to stop the sound of this event. */
        boolean soundStopped(SoundEvent event) {
            return packets.stream().anyMatch(p -> p instanceof ClientboundStopSoundPacket stop && event.getLocation().equals(stop.getName()));
        }

        /** Whether a title with this translation key (in its text or in any part of it) was shown. */
        boolean titled(String key) {
            return packets.stream().anyMatch(p -> p instanceof ClientboundSetTitleTextPacket title && mentions(title.text(), key));
        }

        /** The particle bursts that were sent. */
        List<ClientboundLevelParticlesPacket> particles() {
            return TestKit.particlesIn(packets);
        }

        /** Every effect payload of the Apocalypse fight that was sent, of the given kind. */
        List<ApocalypseFxPayload> effects(byte kind) {
            List<ApocalypseFxPayload> found = new ArrayList<>();
            for (Object packet : packets) {
                if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof ApocalypseFxPayload fx && fx.kind() == kind) {
                    found.add(fx);
                }
            }
            return found;
        }

        void clear() {
            packets.clear();
        }
    }

    /** Whether the component, or any part of it, is the translatable text of this key. */
    static boolean mentions(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            if (translatable.getKey().equals(key)) {
                return true;
            }
            for (Object argument : translatable.getArgs()) {
                if (argument instanceof Component part && mentions(part, key)) {
                    return true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (mentions(sibling, key)) {
                return true;
            }
        }
        return false;
    }

    private ApocalypseTestSupport() {
    }
}

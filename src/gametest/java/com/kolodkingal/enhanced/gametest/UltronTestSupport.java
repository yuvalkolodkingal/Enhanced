package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.ultron.LiftHook;
import com.kolodkingal.enhanced.ultron.LiftHooks;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What the Ultron tests share: a fixture that shrinks the island to fit the 21-wide arena and keeps the server quiet, a
 * way to start a fight at the middle of the floor and skip the cinematic, and the small things every test of the boss
 * needs (a player who cannot be killed by the test, the boss and his Sentries of a fight).
 */
final class UltronTestSupport {
    /** The middle of the test floor: the fight is centred there. */
    static final Vec3 MIDDLE = new Vec3(10.5, FLOOR, 10.5);

    /**
     * A boss fixture whose island is 9 blocks across, so that the whole fight (the ring of Sentries at 6, the field at
     * R + 12 = 21) happens in the one test arena; the server's announcements are off. The island does not rise in these
     * tests (the arena has a ceiling, which the Sky Island's rules refuse): the lift is the Tremor, as it was before
     * the Sokovia lift existed. The tests of the lift itself ({@code SokoviaTests}) install the real one on the Sokovia
     * pad.
     */
    static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        LiftHook lift = LiftHooks.install(null);
        f.onClose(() -> LiftHooks.install(lift));
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.config(UltronBossConfig.ISLAND_RADIUS, 9);
        f.config(UltronBossConfig.LIFT_HEIGHT, 16);
        f.config(UltronBossConfig.CONE_DEPTH, 6);
        f.config(UltronBossConfig.SKIN_DEPTH, 4);
        f.config(UltronBossConfig.LIFT_TICKS, 40);
        f.config(UltronBossConfig.EDITS_PER_TICK, 1500);
        return f;
    }

    /** Starts the fight by an operator's command: it is centred at the starter's feet. */
    static UltronFight start(TestKit.Fixture f, ServerPlayer starter) {
        check(BossFights.start(BossKind.ULTRON, starter, true), "the Ultron fight starts");
        return fightOf(f);
    }

    /** The Ultron fight that runs in the test's level. */
    static UltronFight fightOf(TestKit.Fixture f) {
        check(BossFights.active(f.level()) instanceof UltronFight, "an Ultron fight runs");
        return (UltronFight) BossFights.active(f.level());
    }

    /** Starts the fight and skips the cinematic: Prime and the Sentries are there, and the fight is on. */
    static UltronFight onTheGround(TestKit.Fixture f, ServerPlayer starter) {
        UltronFight fight = start(f, starter);
        fight.forceState(UltronFight.Phase.GROUND);
        return fight;
    }

    /** Skips to the island (or the Tremor's ground at the end of it): bodies can fall. */
    static UltronFight onTheIsland(TestKit.Fixture f, ServerPlayer starter) {
        UltronFight fight = start(f, starter);
        fight.forceState(UltronFight.Phase.ISLAND);
        return fight;
    }

    static UltronEntity boss(UltronFight fight) {
        UltronEntity boss = fight.boss();
        check(boss != null, "Ultron's body is there");
        return boss;
    }

    /** Makes a player unlikely to die of what the test does to them (a thousand health). */
    static void tank(ServerPlayer player) {
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        player.setHealth(1000.0F);
    }

    /**
     * Nothing in the fight acts on its own: the body does not choose attacks (only {@code brain().force} starts one), the
     * Sentries are held, and no wave is due. What a test forces is all that happens.
     */
    static void calm(UltronFight fight) {
        UltronEntity boss = fight.boss();
        if (boss != null) {
            boss.brain().setChoosing(false);
        }
        for (UltronSentryEntity sentry : fight.sentries()) {
            sentry.setHeld(true);
        }
        fight.setNextWaveAt(Long.MAX_VALUE / 2);
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

    /** A Sentry of the fight, assembled and at rest, at a spot of the test floor. */
    static UltronSentryEntity sentryAt(TestKit.Fixture f, UltronFight fight, double x, double z) {
        Vec3 at = f.helper().absoluteVec(new Vec3(x, FLOOR + 1.0, z));
        UltronSentryEntity sentry = UltronSentryEntity.army(f.level(), at, false);
        fight.spawn(sentry);
        fight.refresh();
        return sentry;
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
            for (Object packet : packets) {
                if (packet instanceof ClientboundSystemChatPacket chat && !chat.overlay() && mentions(chat.content(), key)) {
                    return true;
                }
            }
            return false;
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

        /** How many action-bar lines with this translation key were sent. */
        int actionBarCount(String key) {
            int count = 0;
            for (Object packet : packets) {
                if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay() && mentions(chat.content(), key)) {
                    count++;
                }
            }
            return count;
        }

        /** The arguments of the last action-bar line with this key, or null. */
        Object[] lastActionBarArgs(String key) {
            Object[] args = null;
            for (Object packet : packets) {
                if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay() && chat.content().getContents() instanceof TranslatableContents t
                        && t.getKey().equals(key)) {
                    args = t.getArgs();
                }
            }
            return args;
        }

        /** Whether a sound of this event was sent. */
        boolean heard(SoundEvent event) {
            return TestKit.soundsIn(packets).stream().anyMatch(s -> s.id().equals(event.getLocation()));
        }

        /** Every effect payload of the Ultron fight that was sent, of the given kind. */
        List<UltronFxPayload> effects(int kind) {
            List<UltronFxPayload> found = new ArrayList<>();
            for (Object packet : packets) {
                if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof UltronFxPayload fx && fx.kind() == kind) {
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

    /** Runs a command as the player, with operator rights; returns what it returned. */
    static int run(ServerPlayer player, String command) throws CommandSyntaxException {
        CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        return player.getServer().getCommands().getDispatcher().execute(command, source);
    }

    private UltronTestSupport() {
    }
}

package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.gametest.UltronTestSupport.Log;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.ultron.UltronAttack;
import com.kolodkingal.enhanced.ultron.UltronBodies;
import com.kolodkingal.enhanced.ultron.UltronBrain;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.MIDDLE;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.boss;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.calm;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.fixture;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.onTheGround;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.put;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.tank;

/**
 * The boss's body and his army, without the fight's story: what his bodies are, what hurts them and what does not, the
 * six attacks (every one tells before it hurts, every aim is fixed at its moment), how the next attack is chosen, and
 * how the heroes' powers meet the machines.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class UltronBossTests {
    /** Tests that start no fight and keep no server-wide state share one batch. */
    private static final String QUICK = "ultron";

    // ------------------------------------------------------------------------------------------------------------
    // The bodies

    @GameTest(template = ARENA, batch = QUICK)
    public static void bodyStats(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();

        // One fighter: 600, then 80 percent of the last, never less than a quarter of the first.
        double[] one = {600.0, 480.0, 384.0, 307.2, 245.76, 196.608, 157.2864, 150.0, 150.0, 150.0};
        for (int k = 0; k < one.length; k++) {
            assertNear(UltronBodies.health(k, 1), one[k], 0.01, "the health of body " + k + " for one fighter");
        }
        // More fighters, more health: 35 percent of the first body for each after the first, at most six count.
        assertNear(UltronBodies.firstBodyHealth(2), 810.0, 0.01, "two fighters");
        assertNear(UltronBodies.firstBodyHealth(3), 1020.0, 0.01, "three fighters");
        assertNear(UltronBodies.firstBodyHealth(6), 1650.0, 0.01, "six fighters");
        assertNear(UltronBodies.firstBodyHealth(9), 1650.0, 0.01, "nine count as six");
        assertNear(UltronBodies.health(1, 3), 816.0, 0.01, "the second body of three fighters keeps the decay");
        assertNear(UltronBodies.health(7, 3), 255.0, 0.01, "and the floor");
        check(UltronBodies.counted(0) == 1 && UltronBodies.counted(1) == 1 && UltronBodies.counted(4) == 4 && UltronBodies.counted(6) == 6
                && UltronBodies.counted(40) == 6, "at least one and at most six fighters are counted");

        // Armour, toughness and size shrink with every body.
        double[] armor = {8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 2.0};
        double[] toughness = {4.0, 4.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0};
        double[] scale = {1.9, 1.7, 1.5, 1.4, 1.3, 1.2, 1.1, 1.1};
        for (int k = 0; k < armor.length; k++) {
            UltronBodies.Body body = UltronBodies.of(k);
            assertNear(body.armor(), armor[k], 1.0E-6, "the armour of body " + k);
            assertNear(body.toughness(), toughness[k], 1.0E-6, "the toughness of body " + k);
            assertNear(body.scale(), scale[k], 1.0E-4, "the scale of body " + k);
            assertNear(body.hitboxScale(), scale[k] / 1.9, 1.0E-4, "and so the hitbox of body " + k);
        }
        check(UltronBodies.of(0).form() == 0 && UltronBodies.of(1).form() == 1 && UltronBodies.of(2).form() == 2 && UltronBodies.of(9).form() == 2,
                "Prime, the Hybrid, then the Sentry-Ultrons");

        // The entity wears them.
        UltronEntity entity = ModEntities.ULTRON.get().create(level);
        entity.applyBody(0, 1);
        assertNear(entity.getMaxHealth(), 600.0, 0.01, "Prime has 600 health");
        assertNear(entity.getHealth(), 600.0, 0.01, "all of it");
        assertNear(entity.getAttributeValue(Attributes.ARMOR), 8.0, 1.0E-6, "armour 8");
        assertNear(entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 4.0, 1.0E-6, "toughness 4");
        assertNear(entity.getBbHeight(), 3.4, 0.01, "and is 3.4 blocks tall");
        float prime = entity.getBbHeight();
        entity.applyBody(1, 1);
        assertNear(entity.getMaxHealth(), 480.0, 0.01, "the Hybrid has 480");
        assertNear(entity.getBbHeight(), 3.4 * 1.7 / 1.9, 0.01, "and is smaller");
        entity.applyBody(2, 1);
        assertNear(entity.getMaxHealth(), 384.0, 0.01, "the first Sentry-Ultron has 384");
        assertNear(entity.getAttributeValue(Attributes.ARMOR), 6.0, 1.0E-6, "armour 6");
        assertNear(entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 2.0, 1.0E-6, "toughness 2");
        check(entity.getBbHeight() < prime * 0.8F, "and well under Prime's height: " + entity.getBbHeight());
        entity.applyBody(7, 1);
        assertNear(entity.getMaxHealth(), 150.0, 0.01, "the seventh Sentry-Ultron has the floor");
        assertNear(entity.getBbHeight(), 3.4 * 1.1 / 1.9, 0.01, "and the smallest size");

        // The bosses' difficulty multiplies his health.
        f.config(BossConfig.DIFFICULTY_SCALE, 1.5);
        entity.applyBody(0, 1);
        assertNear(entity.getMaxHealth(), 900.0, 0.01, "[boss].difficultyScale 1.5 makes Prime 900");
        f.config(BossConfig.DIFFICULTY_SCALE, 1.0);

        // A fight that grew keeps the fraction he has left.
        entity.applyBody(0, 1);
        entity.setHealth(300.0F);
        entity.rescaleHealth(3);
        assertNear(entity.getMaxHealth(), 1020.0, 0.01, "three fighters make Prime 1020");
        assertNear(entity.getHealth(), 510.0, 0.01, "at the same half");

        // More than 1024 is more than a creature can hold: six fighters ask for 1650.
        entity.applyBody(0, 6);
        assertNear(entity.designHealth(), 1650.0, 0.01, "six fighters make Prime 1650 in the design");
        assertNear(entity.getMaxHealth(), 1024.0, 0.01, "of which the attribute holds 1024");
        assertNear(entity.getHealth(), 1024.0, 0.01, "all of it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "ultron_hits")
    public static void hitCapAndExplosionsAndEffects(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        boss.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0);
        DamageSource generic = level.damageSources().generic();
        DamageSource explosion = level.damageSources().explosion(null, null);
        assertNear(boss.getMaxHealth(), 600.0, 0.01, "Prime, 600 health");

        // No single hit takes more than 15 percent of his health.
        assertNear(hit(boss, generic, 1000.0F), 90.0, 0.01, "a thousand points take only 90");
        assertNear(hit(boss, generic, 30.0F), 30.0, 0.01, "a small hit takes what it says");
        assertNear(hit(boss, generic, 90.0F), 90.0, 0.01, "exactly the cap takes the cap");
        boss.setHealth(boss.getMaxHealth());

        // Explosions are halved (before the cap).
        assertNear(hit(boss, explosion, 100.0F), 50.0, 0.01, "an explosion of 100 takes 50");
        assertNear(hit(boss, explosion, 1000.0F), 90.0, 0.01, "an explosion of a thousand still stops at the cap");
        boss.setHealth(boss.getMaxHealth());

        // A staggered body takes 30 percent more, the cap still holds.
        boss.setState(UltronState.STAGGER);
        assertNear(hit(boss, generic, 40.0F), 52.0, 0.01, "a staggered body takes 40 as 52");
        assertNear(hit(boss, generic, 100.0F), 90.0, 0.01, "and the cap is still the cap");
        boss.setState(UltronState.IDLE);
        boss.setHealth(boss.getMaxHealth());

        // The cap is a fraction of the body's own health.
        boss.applyBody(2, 1);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        boss.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0);
        assertNear(hit(boss, generic, 1000.0F), 384.0 * 0.15, 0.01, "a smaller body's cap is smaller");
        boss.applyBody(0, 1);

        // Six fighters ask for more health than a creature can hold: the cap is a share of the design's, and every blow is shrunk to fit.
        boss.applyBody(0, 6);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        boss.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0);
        double factor = 1024.0 / 1650.0;
        assertNear(hit(boss, generic, 1000.0F), 1650.0 * 0.15 * factor, 0.05, "with a design of 1650 the cap is 15 percent of it, shrunk to what the body holds");
        assertNear(hit(boss, generic, 100.0F), 100.0 * factor, 0.05, "and a small blow is shrunk the same");
        boss.applyBody(0, 1);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        boss.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0);

        // It is a machine: no effect takes hold but Instant Damage and Instant Health.
        for (var effect : List.of(MobEffects.POISON, MobEffects.WITHER, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.BLINDNESS,
                MobEffects.LEVITATION, MobEffects.GLOWING, MobEffects.REGENERATION, MobEffects.DAMAGE_RESISTANCE, MobEffects.CONFUSION)) {
            MobEffectInstance instance = new MobEffectInstance(effect, 200, 1);
            check(!boss.canBeAffected(instance) && !boss.addEffect(instance), effect.getRegisteredName() + " does not take hold");
        }
        check(boss.getActiveEffects().isEmpty(), "no effect is on him");
        check(boss.canBeAffected(new MobEffectInstance(MobEffects.HARM, 1, 0)) && boss.canBeAffected(new MobEffectInstance(MobEffects.HEAL, 1, 0)),
                "Instant Damage and Instant Health are not refused");

        // Fire, drowning and the Wither do nothing.
        float before = boss.getHealth();
        for (DamageSource source : List.of(level.damageSources().inFire(), level.damageSources().onFire(), level.damageSources().lava(),
                level.damageSources().drown(), level.damageSources().wither())) {
            boss.invulnerableTime = 0;
            check(!boss.hurt(source, 20.0F), source.getMsgId() + " is refused");
        }
        assertNear(boss.getHealth(), before, 1.0E-6, "his health did not move");

        // Nothing at all hurts a shielded body.
        boss.setShielded(true);
        boss.invulnerableTime = 0;
        check(!boss.hurt(generic, 20.0F) && !boss.hurt(explosion, 20.0F), "a shielded body takes nothing");
        assertNear(boss.getHealth(), before, 1.0E-6, "still the same");
        boss.setShielded(false);
        helper.succeed();
    }

    /** Hits the body with a source and returns the health it lost. */
    private static double hit(UltronEntity boss, DamageSource source, float amount) {
        boss.invulnerableTime = 0;
        float before = boss.getHealth();
        boss.hurt(source, amount);
        return before - boss.getHealth();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void immunityTags(GameTestHelper helper) {
        // The body is a boss: nothing that works on people works on it.
        EntityType<?> ultron = ModEntities.ULTRON.get();
        for (TagKey<EntityType<?>> tag : List.of(ModTags.BOSSES, ModTags.ULTRON_ROBOTS, ModTags.HEX_RESISTANT, ModTags.PIN_IMMUNE,
                ModTags.TELEPATHY_IMMUNE, ModTags.CONFUSED_IMMUNE, ModTags.SHAPESHIFT_IMMUNE, ModTags.SPELLBOUND_IMMUNE, ModTags.TELEKINESIS_IMMUNE,
                ModTags.INSTANT_KILL_IMMUNE, ModTags.WEB_IMMUNE)) {
            check(ultron.is(tag), "Ultron is in " + tag.location());
        }
        check(!ultron.is(ModTags.BOSS_MINIONS) && !ultron.is(ModTags.MAGNETIC_ENTITIES), "and is neither a minion nor magnetic");

        // The Sentry is his minion: a robot of metal that a hero can still lift, pin or confuse.
        EntityType<?> sentry = ModEntities.ULTRON_SENTRY.get();
        for (TagKey<EntityType<?>> tag : List.of(ModTags.BOSS_MINIONS, ModTags.ULTRON_ROBOTS, ModTags.MAGNETIC_ENTITIES, ModTags.INSTANT_KILL_IMMUNE)) {
            check(sentry.is(tag), "a Sentry is in " + tag.location());
        }
        for (TagKey<EntityType<?>> tag : List.of(ModTags.BOSSES, ModTags.TELEKINESIS_IMMUNE, ModTags.PIN_IMMUNE, ModTags.CONFUSED_IMMUNE,
                ModTags.SPELLBOUND_IMMUNE, ModTags.HEX_RESISTANT, ModTags.WEB_IMMUNE)) {
            check(!sentry.is(tag), "a Sentry is not in " + tag.location());
        }

        // The bolt and the key belong to no list of creatures.
        check(!ModEntities.ULTRON_BOLT.get().is(ModTags.BOSSES) && !ModEntities.VIBRANIUM_KEY.get().is(ModTags.BOSSES), "neither is a boss");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The attacks

    /** One scripted attack: where the body and the target stand, and what the test expects of the attack. */
    private record Case(UltronAttack attack, double bossZ, double bossY, double playerX, double playerZ, int tellKind) {
    }

    /**
     * The pin of the whole boss: every attack tells before it hurts. The attacks are forced one after the other on a
     * target who cannot be killed, and the first tick on which the target is hurt (or the first Sentry emerges) must
     * not come before the attack's tell is over, and the tell's own effect must have been sent before it.
     */
    @GameTest(template = ARENA, batch = "ultron_tells", timeoutTicks = 1200)
    public static void tellsComeBeforeDamage(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        UltronBrain brain = boss.brain();
        Log log = new Log(player);

        List<Case> cases = List.of(
                new Case(UltronAttack.VOLLEY, 4.5, FLOOR + 3.0, 10.5, 12.5, UltronFxPayload.GLINT),
                new Case(UltronAttack.BEAMS, 3.5, FLOOR + 3.0, 10.5, 12.5, UltronFxPayload.BEAMS_AIM),
                new Case(UltronAttack.DIVE, 3.5, FLOOR + 3.0, 10.5, 12.5, UltronFxPayload.DIVE_RING),
                new Case(UltronAttack.PULL, 10.5, FLOOR + 3.0, 10.5, 14.5, UltronFxPayload.PULL_RING),
                new Case(UltronAttack.PUNCH, 8.5, FLOOR + 1.0, 10.5, 11.0, -1),
                new Case(UltronAttack.CALL, 10.5, FLOOR + 3.0, 10.5, 16.5, UltronFxPayload.SPAWN_PILLAR));
        int[] index = {0};
        int[] since = {0};
        int[] firstTell = {-1};
        int[] firstHarm = {-1};
        f.everyTick(1100, tick -> {
            Case c = cases.get(index[0]);
            if (since[0] == 0) {
                player.setHealth(1000.0F);
                player.invulnerableTime = 0;
                put(f, player, c.playerX(), FLOOR, c.playerZ());
                put(f, boss, 10.5, c.bossY(), c.bossZ());
                firstTell[0] = -1;
                firstHarm[0] = -1;
                log.clear();
                check(brain.force(boss, fight, c.attack()), c.attack() + " could be started");
            }
            since[0]++;
            int tellsBefore = c.tellKind() >= 0 ? log.effects(c.tellKind()).size() : 0;
            log.drain();
            if (firstTell[0] < 0 && c.tellKind() >= 0 && log.effects(c.tellKind()).size() > tellsBefore) {
                firstTell[0] = since[0];
            }
            boolean harm = c.attack() == UltronAttack.CALL ? !fight.sentries().isEmpty() : player.getHealth() < 999.99F;
            if (firstHarm[0] < 0 && harm) {
                firstHarm[0] = since[0];
            }
            if (since[0] >= 4 && brain.current() == null) {
                check(firstHarm[0] > 0, c.attack() + " did what it does");
                check(firstHarm[0] >= c.attack().tell(), c.attack() + " came at tick " + firstHarm[0] + ", before its tell of " + c.attack().tell() + " was over");
                if (c.tellKind() >= 0) {
                    check(firstTell[0] > 0 && firstTell[0] <= firstHarm[0], c.attack() + " showed its tell (tick " + firstTell[0] + ") before it hurt (tick " + firstHarm[0] + ")");
                }
                index[0]++;
                since[0] = 0;
                return index[0] == cases.size();
            }
            check(since[0] < 220, c.attack() + " never ended");
            return false;
        });
    }

    /**
     * Once the tell is over the aim is fixed: the Beams are locked on the point the target stood at, the Dive lands in the
     * ring that appeared at their feet, the Punch goes along the line it began with. A target who moves after that is not
     * followed, and so is not hurt.
     */
    @GameTest(template = ARENA, batch = "ultron_aim", timeoutTicks = 900)
    public static void aimIsFixedAfterTheTell(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        UltronBrain brain = boss.brain();
        Vec3 p0 = f.helper().absoluteVec(new Vec3(14.5, FLOOR, 12.5));
        Vec3 p1 = f.helper().absoluteVec(new Vec3(10.5, FLOOR, 12.5));
        Vec3 p2 = f.helper().absoluteVec(new Vec3(18.5, FLOOR, 6.5));

        // 0 = Beams, 1 = Dive, 2 = Punch
        int[] stage = {0};
        int[] since = {0};
        boolean[] struck = {false};
        Vec3[] locked = {null};
        f.everyTick(800, tick -> {
            if (since[0] == 0) {
                player.setHealth(1000.0F);
                player.invulnerableTime = 0;
                struck[0] = false;
                switch (stage[0]) {
                    case 0 -> {
                        put(f, player, 14.5, FLOOR, 12.5);
                        put(f, boss, 10.5, FLOOR + 3.0, 3.5);
                        check(brain.force(boss, fight, UltronAttack.BEAMS), "the Beams start");
                    }
                    case 1 -> {
                        put(f, player, 10.5, FLOOR, 12.5);
                        put(f, boss, 10.5, FLOOR + 3.0, 3.5);
                        check(brain.force(boss, fight, UltronAttack.DIVE), "the Dive starts");
                    }
                    default -> {
                        put(f, player, 10.5, FLOOR, 11.0);
                        put(f, boss, 10.5, FLOOR + 1.0, 8.5);
                        check(brain.force(boss, fight, UltronAttack.PUNCH), "the Punch starts");
                    }
                }
            }
            since[0]++;
            int s = since[0];
            switch (stage[0]) {
                case 0 -> {
                    if (s == 10) {
                        put(f, player, 10.5, FLOOR, 12.5); // still in the tell: the aim follows
                    }
                    if (s == 25) {
                        check(brain.beamsLock().distanceTo(p1) < 0.5, "while he tells, the aim follows the target: " + brain.beamsLock() + " vs " + p1);
                    }
                    if (s == 40 && player.getHealth() < 999.99F) {
                        struck[0] = true;
                    }
                    if (s == 40) {
                        check(struck[0], "the Beams hit the one they were locked on");
                        locked[0] = brain.beamsLock();
                        check(locked[0].distanceTo(p1) < 0.5, "locked where he stood when the tell ended: " + locked[0]);
                        put(f, player, 18.5, FLOOR, 6.5); // out of the fan, after the lock
                        player.setHealth(1000.0F);
                        player.invulnerableTime = 0;
                    }
                    if (s > 40) {
                        check(brain.beamsLock().distanceTo(locked[0]) < 1.0E-6, "the lock does not move");
                        check(player.getHealth() > 999.99F, "the one who left the line is not followed: " + player.getHealth());
                    }
                    if (s > 5 && brain.current() == null) {
                        check(s >= 50, "the Beams ran their time: " + s);
                        stage[0]++;
                        since[0] = 0;
                    }
                }
                case 1 -> {
                    if (s == 16) {
                        // The ring has been on the ground since tick 12; he is not followed from here on.
                        check(brain.diveRing().distanceTo(p1) < 0.5, "the ring appeared at the target's feet: " + brain.diveRing());
                        put(f, player, 18.5, FLOOR, 12.5);
                    }
                    if (s > 16) {
                        check(brain.diveRing().distanceTo(p1) < 0.5, "the ring does not move");
                    }
                    if (s > 5 && brain.current() == null) {
                        check(boss.position().distanceTo(p1) < 0.6, "he landed in the ring, not on the one who left it: " + boss.position());
                        check(player.getHealth() > 999.99F, "and the one who left it was not hurt: " + player.getHealth());
                        check(boss.isStaggered(), "and he is staggered");
                        stage[0]++;
                        since[0] = 0;
                    }
                }
                default -> {
                    if (s == 3) {
                        put(f, player, 13.5, FLOOR, 8.5); // beside him, still within his reach, but off the line of the fist
                    }
                    if (s > 5 && brain.current() == null) {
                        check(player.getHealth() > 999.99F, "the fist went along the line it began with: " + player.getHealth());
                        return true;
                    }
                }
            }
            check(s < 200, "stage " + stage[0] + " never ended");
            return false;
        });
        // (p0 and p2 only document the geometry: the first is where the Beams' target begins, the second is off the fan.)
        check(p0.distanceTo(p2) > 0.0, "the spots differ");
    }

    @GameTest(template = ARENA, batch = "ultron_stagger", timeoutTicks = 500)
    public static void staggerAfterTheDive(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        boss.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0);
        UltronBrain brain = boss.brain();
        put(f, player, 10.5, FLOOR, 12.5);
        put(f, boss, 10.5, FLOOR + 3.0, 3.5);
        check(brain.force(boss, fight, UltronAttack.DIVE), "the Dive starts");
        int[] staggerStart = {-1};
        int[] nextStart = {-1};
        UltronAttack[] next = {null};
        Vec3[] resting = {null};
        f.everyTick(400, tick -> {
            if (staggerStart[0] < 0) {
                if (boss.isStaggered()) {
                    staggerStart[0] = tick;
                    check(brain.staggerLeft() > 0 && brain.staggerLeft() <= UltronBrain.STAGGER_TICKS, "the stagger has its time: " + brain.staggerLeft());
                    check(brain.current() == null, "he does nothing else");
                    check(UltronBrain.STAGGER_TICKS == 50, "which is fifty ticks");
                    resting[0] = boss.position();
                    // A blow now does a third more (the cap not reached).
                    double lost = hit(boss, boss.level().damageSources().generic(), 40.0F);
                    assertNear(lost, 52.0, 0.01, "a staggered body takes 40 as 52");
                    boss.setHealth(boss.getMaxHealth());
                }
                return false;
            }
            if (boss.isStaggered()) {
                check(boss.position().distanceTo(resting[0]) < 0.05, "he stays where he fell");
                check(brain.current() == null, "and attacks no one");
                return false;
            }
            if (nextStart[0] < 0) {
                nextStart[0] = tick;
                int length = tick - staggerStart[0];
                check(length >= 48 && length <= 53, "the stagger lasted " + length + " ticks");
                check(brain.afterStagger(), "and the body knows the next attack is the Volley");
                brain.setChoosing(true); // from here he chooses by himself again
            }
            if (brain.current() != null && next[0] == null) {
                next[0] = brain.current();
                check(next[0] == UltronAttack.VOLLEY, "the first attack after the stagger is the Volley, not " + next[0]);
                return true;
            }
            check(tick - nextStart[0] < 60, "no attack followed the stagger");
            return false;
        });
    }

    /**
     * Left to himself he hovers over the ground inside the island, keeps his distance from the one he hunts, and attacks
     * by the rule: never the same twice, the Beams and the Dive apart, a Volley after the stagger, each from its range.
     */
    @GameTest(template = ARENA, batch = "ultron_hover", timeoutTicks = 900)
    public static void theBodyHoversAndHunts(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheGround(f, player);
        UltronEntity boss = boss(fight);
        UltronBrain brain = boss.brain();
        put(f, player, 10.5, FLOOR, 16.5);
        Vec3 centre = fight.island().center();
        double reach = fight.islandRadius() - 2.0 + 0.7;
        List<UltronAttack> started = new java.util.ArrayList<>();
        List<Integer> startedAt = new java.util.ArrayList<>();
        UltronAttack[] running = {null};
        int[] idleTicks = {0};
        int[] idleRun = {0};
        f.everyTick(850, tick -> {
            if (!boss.isAlive()) {
                return false;
            }
            check(Math.hypot(boss.getX() - centre.x, boss.getZ() - centre.z) <= reach, "he stays inside the island: " + boss.position());
            UltronAttack now = brain.current();
            if (now != null && running[0] == null) {
                started.add(now);
                startedAt.add(tick);
                LivingEntity target = brain.target(level);
                check(target == player, "he hunts the fighter");
                double d = boss.distanceTo(player);
                boolean afterDive = started.size() > 1 && started.get(started.size() - 2) == UltronAttack.DIVE;
                // (the Volley after a stagger comes whatever the distance: he has just landed beside the one he dived at)
                check(afterDive || (d >= now.minRange() - 0.8 && d <= Math.min(now.maxRange(), 40.0) + 0.8), now + " was chosen at " + d + " blocks");
            }
            running[0] = now;
            if (now == null && brain.staggerLeft() == 0) {
                idleTicks[0]++;
                idleRun[0]++;
                if (idleRun[0] > 30) {
                    // (after a Dive he is on the ground and rises: give him a second and a half)
                    double ground = boss.getY() - fight.groundAt(boss.getX(), boss.getZ());
                    check(ground > 0.9 && ground < 6.0, "he hovers over the ground, not in it and not far above: " + ground);
                }
            } else {
                idleRun[0] = 0;
            }
            if (tick >= 800) {
                check(started.size() >= 4, "he attacked at least four times in 800 ticks: " + started);
                check(idleTicks[0] > 100, "and spent time hovering");
                for (int i = 1; i < started.size(); i++) {
                    UltronAttack a = started.get(i - 1);
                    UltronAttack b = started.get(i);
                    check(a != b, "never the same twice in a row: " + started);
                    if (a == UltronAttack.DIVE) {
                        check(b == UltronAttack.VOLLEY, "a Volley after the stagger: " + started);
                    }
                    if ((a == UltronAttack.DIVE && b == UltronAttack.BEAMS) || (a == UltronAttack.BEAMS && b == UltronAttack.DIVE)) {
                        check(startedAt.get(i) - startedAt.get(i - 1) >= UltronAttack.HEAVY_GAP, "the Beams and the Dive are apart: " + started);
                    }
                }
                return true;
            }
            return false;
        });
    }

    /** The rule that picks the next attack, as pure numbers. */
    @GameTest(template = ARENA, batch = QUICK)
    public static void attackChoice(GameTestHelper helper) {
        RandomSource random = RandomSource.create(20240611L);
        long now = 10_000L;
        long[] ready = new long[UltronAttack.values().length];

        // Straight after a stagger it is the Volley, whatever else is ready.
        for (int i = 0; i < 200; i++) {
            check(UltronAttack.choose(situation(0, 10.0, now, null, -1000, -1000, true, true, true, ready), random) == UltronAttack.VOLLEY,
                    "after a stagger: the Volley");
        }

        // The same attack is never chosen twice in a row.
        for (UltronAttack last : UltronAttack.values()) {
            for (int i = 0; i < 400; i++) {
                check(UltronAttack.choose(situation(0, 12.0, now, last, -1000, -1000, false, true, false, ready), random) != last,
                        last + " twice in a row");
            }
        }

        // What rests is not chosen; what has rested is.
        long[] resting = ready.clone();
        resting[UltronAttack.VOLLEY.ordinal()] = now + 1;
        resting[UltronAttack.DIVE.ordinal()] = now + 1;
        resting[UltronAttack.PULL.ordinal()] = now + 1;
        for (int i = 0; i < 400; i++) {
            UltronAttack chosen = UltronAttack.choose(situation(0, 10.0, now, null, -1000, -1000, false, false, false, resting), random);
            check(chosen == UltronAttack.BEAMS, "with the rest resting, only the Beams are left at ten blocks, not " + chosen);
        }
        resting[UltronAttack.VOLLEY.ordinal()] = now;
        check(UltronAttack.choose(situation(0, 10.0, now, UltronAttack.BEAMS, -1000, -1000, false, false, false, resting), random) == UltronAttack.VOLLEY,
                "an attack whose rest ends this very tick can be chosen");

        // The Beams and the Dive never come within 80 ticks of each other.
        for (int i = 0; i < 400; i++) {
            UltronAttack afterDive = UltronAttack.choose(situation(0, 12.0, now, null, -1000, now - 79, false, false, false, ready), random);
            check(afterDive != UltronAttack.BEAMS, "Beams within 80 ticks of a Dive");
            UltronAttack afterBeams = UltronAttack.choose(situation(0, 12.0, now, null, now - 79, -1000, false, false, false, ready), random);
            check(afterBeams != UltronAttack.DIVE, "a Dive within 80 ticks of the Beams");
        }
        boolean beams = false;
        boolean dive = false;
        for (int i = 0; i < 400; i++) {
            beams |= UltronAttack.choose(situation(0, 12.0, now, null, -1000, now - 80, false, false, false, ready), random) == UltronAttack.BEAMS;
            dive |= UltronAttack.choose(situation(0, 12.0, now, null, now - 80, -1000, false, false, false, ready), random) == UltronAttack.DIVE;
        }
        check(beams && dive, "eighty ticks later they are back");

        // Range: the Punch only within 3.2 blocks, the Dive from 5, the Volley and the Pull from 6, the Beams from 8.
        check(UltronAttack.choose(situation(0, 3.3, now, null, -1000, -1000, false, false, false, ready), random) == null,
                "at 3.3 blocks nothing suits: the Punch is out of reach and the rest too near");
        check(UltronAttack.choose(situation(0, 4.9, now, null, -1000, -1000, false, false, false, ready), random) == null, "nor at 4.9");
        for (int i = 0; i < 100; i++) {
            check(UltronAttack.choose(situation(0, 2.0, now, null, -1000, -1000, false, false, false, ready), random) == UltronAttack.PUNCH, "within reach, only the Punch");
            check(UltronAttack.choose(situation(0, 3.2, now, null, -1000, -1000, false, false, false, ready), random) == UltronAttack.PUNCH, "3.2 is still within reach");
            check(UltronAttack.choose(situation(0, 5.0, now, null, -1000, -1000, false, false, false, ready), random) == UltronAttack.DIVE, "at five blocks only the Dive");
            check(UltronAttack.choose(situation(0, 31.0, now, null, -1000, -1000, false, false, false, ready), random) == UltronAttack.BEAMS, "at 31 only the Beams reach");
            check(UltronAttack.choose(situation(0, 33.0, now, null, -1000, -1000, false, false, false, ready), random) == null, "at 33 nothing does");
        }
        Map<UltronAttack, Integer> six = new EnumMap<>(UltronAttack.class);
        for (int i = 0; i < 600; i++) {
            six.merge(UltronAttack.choose(situation(0, 6.0, now, null, -1000, -1000, false, false, false, ready), random), 1, Integer::sum);
        }
        check(six.keySet().equals(java.util.Set.of(UltronAttack.VOLLEY, UltronAttack.DIVE, UltronAttack.PULL)), "at six blocks: the Volley, the Dive and the Pull: " + six);

        // Which bodies have which attacks.
        Map<UltronAttack, Integer> hybrid = new EnumMap<>(UltronAttack.class);
        Map<UltronAttack, Integer> sentryUltron = new EnumMap<>(UltronAttack.class);
        for (int i = 0; i < 3000; i++) {
            UltronAttack a = UltronAttack.choose(situation(1, 12.0, now, null, -1000, -1000, false, true, false, ready), random);
            hybrid.merge(a, 1, Integer::sum);
            UltronAttack b = UltronAttack.choose(situation(2, 12.0, now, null, -1000, -1000, false, true, false, ready), random);
            sentryUltron.merge(b, 1, Integer::sum);
        }
        check(!hybrid.containsKey(UltronAttack.BEAMS) && hybrid.containsKey(UltronAttack.PULL) && hybrid.containsKey(UltronAttack.DIVE), "the Hybrid has no Beams: " + hybrid);
        check(!sentryUltron.containsKey(UltronAttack.BEAMS) && !sentryUltron.containsKey(UltronAttack.PULL)
                && sentryUltron.containsKey(UltronAttack.VOLLEY) && sentryUltron.containsKey(UltronAttack.DIVE), "a Sentry-Ultron has no Beams and no Pull: " + sentryUltron);
        check(UltronAttack.BEAMS.allowedFor(0) && !UltronAttack.BEAMS.allowedFor(1) && UltronAttack.PULL.allowedFor(1) && !UltronAttack.PULL.allowedFor(2),
                "allowedFor says the same");

        // Calling the Sentries needs room and budget, and is four times as likely when a wave is due.
        for (int i = 0; i < 400; i++) {
            check(UltronAttack.choose(situation(0, 12.0, now, null, -1000, -1000, false, false, false, ready), random) != UltronAttack.CALL, "a Call with no room");
        }
        long[] onlyCallOrPunch = ready.clone();
        for (UltronAttack attack : List.of(UltronAttack.VOLLEY, UltronAttack.BEAMS, UltronAttack.DIVE, UltronAttack.PULL)) {
            onlyCallOrPunch[attack.ordinal()] = now + 1000;
        }
        int calm = 0;
        int urgent = 0;
        int samples = 6000;
        for (int i = 0; i < samples; i++) {
            calm += UltronAttack.choose(situation(0, 2.0, now, null, -1000, -1000, false, true, false, onlyCallOrPunch), random) == UltronAttack.CALL ? 1 : 0;
            urgent += UltronAttack.choose(situation(0, 2.0, now, null, -1000, -1000, false, true, true, onlyCallOrPunch), random) == UltronAttack.CALL ? 1 : 0;
        }
        assertNear(calm / (double) samples, 30.0 / 90.0, 0.04, "a Call among a Punch: 30 of 90");
        assertNear(urgent / (double) samples, 120.0 / 180.0, 0.04, "and four times the weight when a wave is due: 120 of 180");

        // By weight among those that suit: 40, 25, 20 and 20.
        Map<UltronAttack, Integer> counts = new EnumMap<>(UltronAttack.class);
        for (int i = 0; i < 8000; i++) {
            counts.merge(UltronAttack.choose(situation(0, 12.0, now, null, -1000, -1000, false, false, false, ready), random), 1, Integer::sum);
        }
        assertNear(counts.getOrDefault(UltronAttack.VOLLEY, 0) / 8000.0, 40.0 / 105.0, 0.03, "the Volley, 40 of 105");
        assertNear(counts.getOrDefault(UltronAttack.BEAMS, 0) / 8000.0, 25.0 / 105.0, 0.03, "the Beams, 25 of 105");
        assertNear(counts.getOrDefault(UltronAttack.DIVE, 0) / 8000.0, 20.0 / 105.0, 0.03, "the Dive, 20 of 105");
        assertNear(counts.getOrDefault(UltronAttack.PULL, 0) / 8000.0, 20.0 / 105.0, 0.03, "the Pull, 20 of 105");

        // And the numbers of the table itself.
        check(UltronAttack.VOLLEY.tell() == 14 && UltronAttack.BEAMS.tell() == 30 && UltronAttack.DIVE.tell() == 24 && UltronAttack.PULL.tell() == 24
                && UltronAttack.CALL.tell() == 30 && UltronAttack.PUNCH.tell() == 8, "the tells");
        check(UltronAttack.VOLLEY.cooldown() == 70 && UltronAttack.BEAMS.cooldown() == 160 && UltronAttack.DIVE.cooldown() == 240
                && UltronAttack.PULL.cooldown() == 300 && UltronAttack.CALL.cooldown() == 500 && UltronAttack.PUNCH.cooldown() == 25, "the rests");
        helper.succeed();
    }

    private static UltronAttack.Situation situation(int body, double distance, long now, UltronAttack last, long lastBeams, long lastDive,
                                                    boolean afterStagger, boolean callAllowed, boolean callUrgent, long[] readyAt) {
        return new UltronAttack.Situation(body, distance, now, last, lastBeams, lastDive, afterStagger, callAllowed, callUrgent, readyAt);
    }

    private UltronBossTests() {
    }
}

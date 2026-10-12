package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.PhoenixBurst;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.apocalypse.ApocalypseAttack;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.Horsemen;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.entity.SandLanceEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosPhantomEntity;
import com.kolodkingal.enhanced.thanos.ThanosState;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The 3.0 heroes and bosses meet for the first time (integration.md 5.10): every hero against the bosses' defences and
 * minions, through the same keys a player presses, in a real fight. The five lanes were built in parallel against stubs of
 * one another, so these tests look for the seams. Each test starts a boss fight or changes a server-wide value, so each has
 * a batch of its own; whatever flies is kept short, so it stays in its own arena.
 *
 * <p>The quick rows of the matrix (the rituals, the rewards, the village, one fight at a time, the gifts, a restart and the
 * lift) are in {@link CrossHeroTests}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CrossHeroBossTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A key pressed and let go, as a client sends it. */
    static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    /** A key pressed and let go with Sneak held. */
    static void sneakPress(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        press(player, ability);
        player.setShiftKeyDown(false);
    }

    /** A key held down (it stays down until {@link #letGo}). */
    static void hold(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
    }

    static void letGo(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, false);
    }

    /** Makes a player unlikely to die of what the test does to them. */
    static ServerPlayer tank(ServerPlayer player) {
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        player.setHealth(1000.0F);
        return player;
    }

    /** Puts on a look of the player's power, as the look key does. */
    static void wear(ServerPlayer player, String look) {
        Powers.setVariant(player, Variants.get(Powers.power(player), look));
        PowerTicker.tick(player);
    }

    /** Where an entity is, in the test's relative coordinates. */
    static Vec3 rel(GameTestHelper helper, Entity entity) {
        return helper.relativeVec(entity.position());
    }

    /** Puts an entity at a spot of the test (relative coordinates), at rest. */
    static void put(GameTestHelper helper, Entity entity, double x, double y, double z) {
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        entity.moveTo(at.x, at.y, at.z, entity.getYRot(), entity.getXRot());
        entity.setDeltaMovement(Vec3.ZERO);
    }

    /** What a boss of Apocalypse keeps: his health and what is left of his shield, to compare later. */
    private record Guarded(float health, float shield) {
        static Guarded of(ApocalypseEntity boss) {
            return new Guarded(boss.getHealth(), boss.shield().strength());
        }

        /** The boss lost nothing, neither health nor shield, to {@code what}. */
        void unchanged(ApocalypseEntity boss, String what) {
            check(boss.isAlive(), what + ": he is alive");
            check(boss.getHealth() == health, what + " took none of his health: " + health + " -> " + boss.getHealth());
            check(boss.shield().isUp() && boss.shield().strength() == shield,
                    what + " did not wear his shield: " + shield + " -> " + boss.shield().strength() + " (" + boss.shield().state() + ")");
        }
    }

    /**
     * Counts the ticks an entity really ticks (Quicksilver Time skips the others: {@code EntityTickEvent.Post} follows only a
     * tick that ran).
     */
    private static final class TickCounter implements Consumer<EntityTickEvent.Post> {
        private final Map<Integer, Integer> counts = new HashMap<>();

        static TickCounter install(TestKit.Fixture f) {
            TickCounter counter = new TickCounter();
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityTickEvent.Post.class, counter);
            f.onClose(() -> NeoForge.EVENT_BUS.unregister(counter));
            return counter;
        }

        void watch(Entity entity) {
            counts.put(entity.getId(), 0);
        }

        int ticks(Entity entity) {
            return counts.getOrDefault(entity.getId(), -1);
        }

        void reset() {
            counts.replaceAll((id, n) -> 0);
        }

        @Override
        public void accept(EntityTickEvent.Post event) {
            if (!event.getEntity().level().isClientSide) {
                counts.computeIfPresent(event.getEntity().getId(), (id, n) -> n + 1);
            }
        }
    }

    /** How often the boss's shield has rung (it turned something away) in all that was sent to the player so far. */
    private static int ripples(ApocalypseTestSupport.Log log) {
        return log.drain().effects(ApocalypseFxPayload.SHIELD_HIT).size();
    }

    /** A Burst that fits in its arena: a charge of 10 ticks, radii of 4 to 9, no fire and no push, no long rest. */
    private static void smallBursts(TestKit.Fixture f) {
        f.config(PhoenixConfig.BURST_CHARGE_TICKS, 10);
        f.config(PhoenixConfig.BURST_MIN_CHARGE_TICKS, 0);
        f.config(PhoenixConfig.BURST_MIN_RADIUS, 4);
        f.config(PhoenixConfig.BURST_MAX_RADIUS, 9);
        f.config(PhoenixConfig.BURST_FIRE_SECONDS, 0);
        f.config(PhoenixConfig.BURST_KNOCKBACK, 0.0);
        f.config(PhoenixConfig.BURST_COOLDOWN, 0);
    }

    // ============================================================================================================
    // 2. The Phoenix Burst against a Chaos Shield and against Apocalypse's shield

    /**
     * A Burst that reaches a Scarlet Witch behind her complete Chaos Shield wears the shield down first (phoenix.md 4.5): a
     * Burst stronger than the shield shatters it and only the rest reaches her. The whole of it: Wanda raises her shield
     * with B, the Phoenix charges N to the full and the wave goes out. An Apocalypse behind his own shield in the same wave:
     * the Burst is mind power, so it wears his shield five times over and what is left of it gets through
     * (apocalypse.md 4.4).
     */
    @GameTest(template = ARENA, batch = "cross_burst_chaos_shield", timeoutTicks = 140)
    public static void aBurstWearsAChaosShieldDownAndOnlyTheRestGetsThrough(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        smallBursts(f);
        // A player takes half the Burst: 80 at the centre is 40 for her, more than the 30 of her shield.
        f.config(PhoenixConfig.BURST_MAX_DAMAGE, 80.0);
        ServerPlayer jean = f.player(5.5, FLOOR, 10.5, Power.PHOENIX);
        ServerPlayer wanda = f.player(8.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        wanda.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.WANDA_CROWN.get()));
        PowerTicker.tick(wanda);
        wanda.setHealth(wanda.getMaxHealth());
        PowerData shield = Powers.get(wanda);
        float health = wanda.getHealth();
        float strength = WandaAbilities.maxShieldStrength();
        ServerPlayer apocalypse = tank(f.player(2.5, FLOOR, 10.5, Power.APOCALYPSE));
        PowerTicker.tick(apocalypse);
        apocalypse.setHealth(apocalypse.getMaxHealth());
        float apocalypseHealth = apocalypse.getHealth();
        float[] told = {-1.0F};
        float[] toldHim = {-1.0F};
        PhoenixPower.Listener listener = (phoenix, target, kind, amount) -> {
            if (phoenix == jean && kind == PhoenixPower.Kind.BURST) {
                if (target == wanda) {
                    told[0] = amount;
                } else if (target == apocalypse) {
                    toldHim[0] = amount;
                }
            }
        };
        PhoenixPower.addListener(listener);
        f.onClose(() -> PhoenixPower.removeListener(listener));

        hold(wanda, 4);
        hold(apocalypse, 4);
        int[] charging = {-1};
        f.everyTick(120, tick -> {
            PowerTicker.tick(wanda);
            PowerTicker.tick(apocalypse);
            PowerTicker.tick(jean);
            if (charging[0] < 0 && WandaAbilities.hasShieldUp(wanda) && ApocalypseGuard.isUp(apocalypse)) {
                check(shield.shieldStrength() == strength, "her shield is complete and whole: " + shield.shieldStrength());
                check(Powers.get(apocalypse).shieldStrength() == ApocalypseGuard.maxStrength(), "and his");
                hold(jean, 5);
                check(Powers.get(jean).is(Live.PHOENIX_CHARGING), "the Phoenix charges");
                charging[0] = tick;
            }
            if (charging[0] < 0 || told[0] < 0.0F || toldHim[0] < 0.0F || PhoenixBurst.active() > 0) {
                return false;
            }
            assertNear(told[0], 40.0, 0.5, "the wave reaches Wanda with half of the 80 at the centre");
            check(!shield.chaosShieldActive() && shield.shieldStrength() == 0.0F, "the Burst shattered her Chaos Shield");
            assertNear(health - wanda.getHealth(), told[0] - strength, 0.5,
                    "and only what the shield could not take got through to her (health " + health + " -> " + wanda.getHealth() + ")");
            PowerData his = Powers.get(apocalypse);
            float factor = ApocalypseShieldConfig.PHOENIX_BURST_FACTOR.get().floatValue();
            check(toldHim[0] * factor > ApocalypseGuard.maxStrength(), "the Burst is more than his shield can take: " + toldHim[0] + " x " + factor);
            check(!his.chaosShieldActive() && his.shieldStrength() == 0.0F, "it shattered the Apocalypse's shield too");
            assertNear(apocalypseHealth - apocalypse.getHealth(), (toldHim[0] * factor - ApocalypseGuard.maxStrength()) / factor, 0.5,
                    "and what his shield could not take got through to him (health " + apocalypseHealth + " -> " + apocalypse.getHealth() + ")");
            return true;
        });
    }

    /**
     * The Burst is Phoenix mind power: it wears Apocalypse's shield by five times its damage and never his health
     * (apocalypse.md 3.5.2): a weak Burst takes five times what it did off the shield; a full one (30, so 150) breaks shield 1
     * of a lone fighter, and what is left over is thrown away.
     */
    @GameTest(template = ARENA, batch = "cross_burst_apocalypse", timeoutTicks = 160)
    public static void burstsWearApocalypsesShieldByFiveTimesAndAFullOneBreaksIt(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.PHOENIX);
        smallBursts(d.f());
        ServerPlayer jean = d.hero();
        ApocalypseEntity boss = d.boss();
        ApocalypseTestSupport.put(d.f(), jean, 7.5, FLOOR, 10.5);
        lookAt(jean, boss);
        float health = boss.getHealth();
        float max = boss.shield().strength();
        check(max == 150.0F, "shield 1 of one fighter has 150 points: " + max);
        List<Float> told = new ArrayList<>();
        PhoenixPower.Listener listener = (phoenix, target, kind, amount) -> {
            if (phoenix == jean && target == boss && kind == PhoenixPower.Kind.BURST) {
                told.add(amount);
            }
        };
        PhoenixPower.addListener(listener);
        d.f().onClose(() -> PhoenixPower.removeListener(listener));

        // A weak one: one tick of charge, then let go.
        hold(jean, 5);
        PowerTicker.tick(jean);
        letGo(jean, 5);
        check(PhoenixBurst.active() == 1, "a weak Burst went out");
        int[] second = {-1};
        d.f().everyTick(140, tick -> {
            PowerTicker.tick(jean);
            if (second[0] < 0) {
                if (told.isEmpty() || PhoenixBurst.active() > 0) {
                    return false;
                }
                float weak = told.get(0);
                check(weak > 0.0F && weak < 20.0F, "the weak Burst reached him with little: " + weak);
                assertNear(max - boss.shield().strength(), weak * 5.0, 1.0E-3, "and took five times that off his shield");
                check(boss.getHealth() == health && boss.shield().isUp(), "but nothing of his health, and the shield stands");
                // (The shield counts one mind blow from her in ten ticks.)
                if (tick < 14) {
                    return false;
                }
                Powers.get(jean).startCooldown(5, helper.getLevel().getGameTime(), 0);
                lookAt(jean, boss);
                hold(jean, 5);
                second[0] = tick;
                return false;
            }
            if (told.size() < 2 || PhoenixBurst.active() > 0) {
                return false;
            }
            assertNear(told.get(1), 30.0, 0.01, "the full Burst reached him with all of its 30");
            check(boss.shield().state() == ApocalypseShield.State.BROKEN, "and broke shield 1 (150): " + boss.shield().state());
            check(boss.getHealth() == health, "with what was left over thrown away, not taken from his health");
            assertNear(d.fight().participant(jean.getUUID()).counter("shield"), max, 1.0E-3,
                    "the Phoenix is credited with the whole shield she wore away");
            return true;
        });
    }

    /**
     * The Phoenix's Mind Control and Psionic Field are not damage but a strain on Apocalypse's shield, even when his mind
     * refuses them (apocalypse.md 3.5.4: 25 and 60 points): once per cast. A cast he refuses starts no recharge of hers
     * (phoenix.md 4.4), so pressing the key again at once is not a second cast: the shield feels each of her touches once
     * per its recharge.
     */
    @GameTest(template = ARENA, batch = "cross_phoenix_strain", timeoutTicks = 100)
    public static void thePhoenixsMindStrainsApocalypsesShieldOncePerCast(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.PHOENIX);
        ServerPlayer jean = d.hero();
        ApocalypseEntity boss = d.boss();
        float health = boss.getHealth();
        float control = ApocalypseShieldConfig.PHOENIX_MIND_CONTROL_STRAIN.get().floatValue();
        float field = ApocalypseShieldConfig.PHOENIX_PSIONIC_STRAIN.get().floatValue();
        float max = boss.shield().strength();
        lookAt(jean, boss);
        press(jean, 4);
        check(Powers.get(jean).isReady(4, helper.getLevel().getGameTime()), "his mind refused her: her Mind Control did not start its recharge");
        assertNear(max - boss.shield().strength(), control, 1.0E-3, "but the attempt strained his shield by " + control);
        for (int i = 0; i < 5; i++) {
            lookAt(jean, boss);
            press(jean, 4);
        }
        assertNear(max - boss.shield().strength(), control, 1.0E-3, "pressing B again at once is the same cast: no more strain ("
                + (max - boss.shield().strength()) + ")");
        sneakPress(jean, 4);
        assertNear(max - boss.shield().strength(), control + field, 1.0E-3, "her Psionic Field round him strains it by " + field);
        for (int i = 0; i < 5; i++) {
            sneakPress(jean, 4);
        }
        assertNear(max - boss.shield().strength(), control + field, 1.0E-3, "and again at once, no more (" + (max - boss.shield().strength()) + ")");
        check(boss.shield().isUp() && boss.getHealth() == health, "the shield stands, and his health is untouched");
        assertNear(d.fight().participant(jean.getUUID()).counter("shield"), control + field, 1.0E-3, "she is credited with the strain");
        helper.succeed();
    }

    /**
     * A hero Apocalypse behind his own shield is strained by the Phoenix's Mind Control and Psionic Field as the boss is
     * (apocalypse.md 4.4: "A Phoenix's Mind Control and Psionic Field strain it by 25 and 60"), once per cast, where PvP lets
     * her reach him.
     */
    @GameTest(template = ARENA, batch = "cross_phoenix_strain_hero", timeoutTicks = 100)
    public static void thePhoenixsMindStrainsAHeroApocalypsesShieldToo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer jean = f.player(4.5, FLOOR, 10.5, Power.PHOENIX);
        float control = ApocalypseShieldConfig.PHOENIX_MIND_CONTROL_STRAIN.get().floatValue();
        float field = ApocalypseShieldConfig.PHOENIX_PSIONIC_STRAIN.get().floatValue();
        ServerPlayer hero = tank(f.player(10.5, FLOOR, 10.5, Power.APOCALYPSE));
        PowerTicker.tick(hero);
        hold(hero, 4);
        f.everyTick(60, tick -> {
            PowerTicker.tick(hero);
            if (!ApocalypseGuard.isUp(hero)) {
                return false;
            }
            PowerData his = Powers.get(hero);
            float full = his.shieldStrength();
            lookAt(jean, hero);
            press(jean, 4);
            assertNear(full - his.shieldStrength(), control, 1.0E-3, "her Mind Control strains the hero's shield by " + control);
            Powers.get(jean).startCooldown(4, helper.getLevel().getGameTime(), 0);
            lookAt(jean, hero);
            press(jean, 4);
            assertNear(full - his.shieldStrength(), control, 1.0E-3, "a second Mind Control within her recharge strains it no more");
            Powers.get(jean).startCooldown(4, helper.getLevel().getGameTime(), 0);
            float before = his.shieldStrength();
            sneakPress(jean, 4);
            check(before - field <= 0.0F ? !his.chaosShieldActive() : Math.abs(before - his.shieldStrength() - field) < 1.0E-3,
                    "and her Psionic Field by " + field + ": " + before + " -> " + his.shieldStrength() + " (up " + his.chaosShieldActive() + ")");
            letGo(hero, 4);
            return true;
        });
    }

    // ============================================================================================================
    // 3. Only Wanda's and the Phoenix's mind power break Apocalypse's shield

    /**
     * Billy's Hex Bolt is magic but not mind power (apocalypse.md 3.5.2, question 1: a server may add it): the shield turns it
     * away, whole. Wanda's Chaos Beam, held on him, wears it five points a hit and never touches his health.
     */
    @GameTest(template = ARENA, batch = "cross_apocalypse_witches", timeoutTicks = 140)
    public static void billysHexBoltLeavesApocalypsesShieldWholeAndWandasBeamWearsIt(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        ApocalypseEntity boss = d.boss();
        ServerPlayer wanda = d.hero();
        ServerPlayer billy = tank(d.f().player(3.5, FLOOR, 13.5, Power.BILLY));
        lookAt(billy, boss);
        Guarded before = Guarded.of(boss);
        ApocalypseTestSupport.Log log = new ApocalypseTestSupport.Log(billy);
        press(billy, 1);
        check(ripples(log) >= 1, "Billy's Hex Bolt reached him: his shield rang");
        before.unchanged(boss, "Billy's Hex Bolt");
        int[] beamFrom = {-1};
        d.f().everyTick(120, tick -> {
            PowerTicker.tick(billy);
            PowerTicker.tick(wanda);
            if (tick < 30) {
                if (Powers.get(billy).isReady(1, helper.getLevel().getGameTime())) {
                    lookAt(billy, boss);
                    press(billy, 1);
                }
                before.unchanged(boss, "Billy's Hex Bolts (tick " + tick + ")");
                return false;
            }
            if (beamFrom[0] < 0) {
                beamFrom[0] = tick;
                lookAt(wanda, boss);
                hold(wanda, 1);
                return false;
            }
            lookAt(wanda, boss);
            check(boss.getHealth() == before.health(), "the Chaos Beam never touches his health");
            if (tick - beamFrom[0] >= 25) {
                float worn = before.shield() - boss.shield().strength();
                check(worn >= 10.0F - 1.0E-3F && worn % 5.0F < 1.0E-3F, "the Chaos Beam wore his shield, five a hit: " + worn);
                letGo(wanda, 1);
                return true;
            }
            return false;
        });
    }

    /**
     * Thor's strongest attacks are not mind power: the lightning beam, held on him, and a thrown Mjolnir leave the shield and
     * his health whole, and the hammer is turned back to Thor, never destroyed (apocalypse.md 3.5.3).
     */
    @GameTest(template = ARENA, batch = "cross_apocalypse_thor", timeoutTicks = 220)
    public static void thorsLightningBeamAndThrownMjolnirLeaveApocalypsesShieldWhole(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.THOR);
        d.f().config(EnhancedConfig.MJOLNIR_THROW_RANGE, 12);
        ApocalypseEntity boss = d.boss();
        ServerPlayer thor = d.hero();
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        lookAt(thor, boss);
        Guarded before = Guarded.of(boss);
        ApocalypseTestSupport.Log log = new ApocalypseTestSupport.Log(thor);
        ThorAbilities.forgetBeamPace(thor);
        hold(thor, 3);
        int[] thrownAt = {-1};
        int[] rang = {0};
        boolean[] flew = {false};
        d.f().everyTick(200, tick -> {
            PowerTicker.tick(thor);
            before.unchanged(boss, "Thor (tick " + tick + ")");
            if (tick < 30) {
                lookAt(thor, boss);
                check(Powers.get(thor).is(Live.CALLING_LIGHTNING), "the lightning beam is on him");
                return false;
            }
            if (tick == 30) {
                rang[0] = ripples(log);
                check(rang[0] >= 1, "the lightning beam reached him: his shield rang");
                letGo(thor, 3);
                lookAt(thor, boss);
                press(thor, 1);
                check(!thor.getMainHandItem().is(ModItems.MJOLNIR.get()), "Mjolnir is thrown");
                thrownAt[0] = tick;
                return false;
            }
            Entity hammer = helper.getLevel().getEntity(Powers.get(thor).hammerEntityId());
            if (hammer instanceof ThrownWeaponEntity weapon) {
                flew[0] = true;
                check(!weapon.isRemoved(), "the hammer is never destroyed");
            }
            if (thor.getMainHandItem().is(ModItems.MJOLNIR.get())) {
                check(flew[0], "the hammer flew");
                check(ripples(log) > rang[0], "and reached him: his shield turned it back");
                return true;
            }
            return false;
        });
    }

    /**
     * Spider-Man's webs and the Iron Spider's Instant Kill are not mind power: a web shot makes no web on him (he is
     * {@code web_immune}) and wears nothing, and the legs in Instant Kill strike him for their own five, which the shield
     * turns away like everything else.
     */
    @GameTest(template = ARENA, batch = "cross_apocalypse_spider", timeoutTicks = 100)
    public static void spiderMansWebsAndInstantKillLeaveApocalypsesShieldWhole(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SPIDER_MAN);
        ApocalypseEntity boss = d.boss();
        ServerPlayer spider = d.hero();
        lookAt(spider, boss);
        Guarded before = Guarded.of(boss);
        press(spider, 1);
        check(!boss.hasEffect(ModEffects.WEBBED), "the web does not take hold of him");
        before.unchanged(boss, "a web shot");

        wear(spider, "iron_spider");
        press(spider, 4);
        check(Powers.get(spider).toggle(Toggle.SPIDER_LEGS), "the Iron Spider's legs are out");
        ApocalypseTestSupport.put(d.f(), spider, 7.5, FLOOR, 10.5);
        sneakPress(spider, 4);
        sneakPress(spider, 4);
        check(Powers.get(spider).is(Live.INSTANT_KILL), "Instant Kill is on");
        check(SpiderServer.isThreat(spider, boss), "and he is a threat the legs go for");
        ApocalypseTestSupport.Log log = new ApocalypseTestSupport.Log(spider);
        d.f().everyTick(60, tick -> {
            PowerTicker.tick(spider);
            before.unchanged(boss, "the legs of Instant Kill (tick " + tick + ")");
            if (tick < 60) {
                return false;
            }
            check(ripples(log) >= 1, "the legs struck him: his shield rang");
            return true;
        });
    }

    /**
     * Magneto and Vision: shrapnel (metal) is stopped before it hits, Metal Telekinesis cannot lift him (he is not metal and
     * too powerful), the strip outside a window takes nothing, and the Mind Stone's beam is not mind power by default.
     */
    @GameTest(template = ARENA, batch = "cross_apocalypse_magneto_vision", timeoutTicks = 140)
    public static void magnetoAndVisionLeaveApocalypsesShieldWhole(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.MAGNETO);
        ApocalypseEntity boss = d.boss();
        ServerPlayer magneto = d.hero();
        for (int i = 0; i < 4; i++) {
            magneto.getInventory().add(new ItemStack(Items.IRON_INGOT, 64));
        }
        ServerPlayer vision = tank(d.f().player(3.5, FLOOR, 13.5, Power.VISION));
        Guarded before = Guarded.of(boss);
        lookAt(magneto, boss);
        press(magneto, 1);
        check(!Telekinesis.isHolding(magneto), "Metal Telekinesis cannot lift him");
        press(magneto, 3);
        before.unchanged(boss, "Magneto's strip outside a window");
        lookAt(magneto, boss);
        ApocalypseTestSupport.Log log = new ApocalypseTestSupport.Log(magneto);
        hold(magneto, 2);
        int[] rang = {0};
        d.f().everyTick(120, tick -> {
            PowerTicker.tick(magneto);
            PowerTicker.tick(vision);
            before.unchanged(boss, "Magneto's shrapnel and Vision's beam (tick " + tick + ")");
            if (tick < 40) {
                lookAt(magneto, boss);
                return false;
            }
            if (tick == 40) {
                rang[0] = ripples(log);
                check(rang[0] >= 1, "Magneto's shrapnel reached him: his shield turned it to dust");
                letGo(magneto, 2);
                lookAt(vision, boss);
                hold(vision, 1);
                return false;
            }
            lookAt(vision, boss);
            if (tick >= 80) {
                check(Powers.get(vision).beamActive(), "Vision's beam was on him all along");
                check(ripples(log) > rang[0], "and reached him: his shield rang");
                letGo(vision, 1);
                return true;
            }
            return false;
        });
    }

    /**
     * The villains as heroes are not mind power either: the Ultron hero's repulsor stream and Overcharge, his Gravity Pull and
     * Slam, and the Thanos hero's Power Blast, Titan Slam and Soul Drain leave the shield and his health whole.
     */
    @GameTest(template = ARENA, batch = "cross_apocalypse_villains", timeoutTicks = 220)
    public static void theUltronAndThanosHeroesLeaveApocalypsesShieldWhole(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.ULTRON);
        ApocalypseEntity boss = d.boss();
        ServerPlayer ultron = d.hero();
        ServerPlayer thanos = tank(d.f().player(5.5, FLOOR, 14.5, Power.THANOS));
        Guarded before = Guarded.of(boss);
        ApocalypseTestSupport.Log log = new ApocalypseTestSupport.Log(ultron);
        lookAt(ultron, boss);
        hold(ultron, 1);
        int[] rang = {0};
        d.f().everyTick(200, tick -> {
            PowerTicker.tick(ultron);
            PowerTicker.tick(thanos);
            before.unchanged(boss, "the villains' powers (tick " + tick + ")");
            if (tick == 25 || tick == 69 || tick == 89 || tick == 129 || tick == 180) {
                int now = ripples(log);
                check(now > rang[0], "each of them reached him and his shield rang (tick " + tick + ", " + now + " rings)");
                rang[0] = now;
            }
            switch (tick) {
                case 25 -> {
                    letGo(ultron, 1);
                    lookAt(ultron, boss);
                    sneakPress(ultron, 1);
                    check(Powers.get(ultron).is(Live.ULTRON_CHARGING), "the Overcharge builds");
                }
                case 70 -> {
                    lookAt(ultron, boss);
                    press(ultron, 3);
                    sneakPress(ultron, 3);
                }
                case 80 -> {
                    lookAt(thanos, boss);
                    press(thanos, 1);
                }
                case 90 -> {
                    // (The Blast and the Slam share the key's rest.)
                    Powers.get(thanos).startCooldown(1, helper.getLevel().getGameTime(), 0);
                    ApocalypseTestSupport.put(d.f(), thanos, 6.5, FLOOR, 10.5);
                    sneakPress(thanos, 1);
                }
                case 130 -> {
                    lookAt(thanos, boss);
                    hold(thanos, 4);
                }
                case 180 -> {
                    letGo(thanos, 4);
                    return true;
                }
                default -> {
                    if (tick > 130) {
                        lookAt(thanos, boss);
                    }
                }
            }
            return false;
        });
    }

    // ============================================================================================================
    // 4. Apocalypse's sand lances: caught by Billy, stopped by a Chaos Shield

    /**
     * Billy catches one of the five lances of a real volley in flight with his telekinesis (apocalypse.md 3.14) and throws it
     * back: while the shield is up it is turned away like anything else, and once the shield is broken the next one he throws
     * back hurts the boss (it is his sand, thrown by Billy).
     */
    @GameTest(template = ARENA, batch = "cross_apocalypse_lances_billy", timeoutTicks = 260)
    public static void billyCatchesASandLanceAndThrowsItBack(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.BILLY);
        ServerLevel level = d.level();
        ApocalypseEntity boss = d.boss();
        ApocalypseFight fight = d.fight();
        ServerPlayer billy = d.hero();
        ServerPlayer wanda = tank(d.f().player(3.5, FLOOR, 17.5, Power.SCARLET_WITCH));
        Guarded before = Guarded.of(boss);
        int[] step = {0};
        int[] stepAt = {0};
        float[] health = {boss.getHealth()};
        d.f().everyTick(240, tick -> {
            PowerTicker.tick(billy);
            switch (step[0]) {
                case 0, 3 -> {
                    // A volley of lances at Billy.
                    if (!boss.brain().isBusy() && boss.brain().force(boss, fight, ApocalypseAttack.LANCES)) {
                        step[0]++;
                        stepAt[0] = tick;
                    }
                    check(tick - stepAt[0] < 60, "he would not throw his lances (step " + step[0] + ")");
                }
                case 1, 4 -> {
                    // Billy catches the nearest lance in the air.
                    if (Telekinesis.isHolding(billy)) {
                        Entity held = level.getEntity(Powers.get(billy).heldEntityId());
                        check(held instanceof SandLanceEntity, "what Billy holds is a sand lance: " + held);
                        step[0]++;
                        stepAt[0] = tick;
                        return false;
                    }
                    List<SandLanceEntity> lances = level.getEntitiesOfClass(SandLanceEntity.class, new AABB(billy.blockPosition()).inflate(12.0),
                            lance -> !lance.isRemoved());
                    lances.sort(Comparator.comparingDouble(lance -> lance.distanceToSqr(billy)));
                    if (!lances.isEmpty() && lances.get(0).distanceTo(billy) < 6.0) {
                        lookAt(billy, lances.get(0));
                        press(billy, 3);
                    }
                    check(tick - stepAt[0] < 40, "Billy never caught a lance (step " + step[0] + ")");
                }
                case 2 -> {
                    // Thrown back while the shield is up: turned away like everything else.
                    if (tick - stepAt[0] == 2) {
                        lookAt(billy, boss);
                        press(billy, 3);
                        check(!Telekinesis.isHolding(billy), "Billy threw the lance");
                    }
                    before.unchanged(boss, "a lance thrown back at the shield");
                    if (tick - stepAt[0] >= 20) {
                        step[0]++;
                        stepAt[0] = tick;
                    }
                }
                case 5 -> {
                    // Holding the second lance: Wanda breaks the shield, then Billy throws it.
                    if (boss.shield().isUp()) {
                        boss.invulnerableTime = 0;
                        boss.hurt(ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, wanda, wanda), boss.shield().strength() + 1.0F);
                        check(boss.shield().state() == ApocalypseShield.State.BROKEN, "Wanda broke the shield: " + boss.shield().state());
                        health[0] = boss.getHealth();
                        lookAt(billy, boss);
                        press(billy, 3);
                        check(!Telekinesis.isHolding(billy), "Billy threw the second lance");
                        stepAt[0] = tick;
                        return false;
                    }
                    if (boss.getHealth() < health[0]) {
                        check(boss.getLastDamageSource() != null && boss.getLastDamageSource().is(ModDamageTypes.SAND)
                                && boss.getLastDamageSource().getEntity() == billy, "it was the lance, and Billy threw it");
                        return true;
                    }
                    check(tick - stepAt[0] < 30, "the lance Billy threw into the window never hurt him (health " + boss.getHealth() + ")");
                }
                default -> {
                }
            }
            return false;
        });
    }

    /**
     * A complete Chaos Shield stops every lance of a real volley at its surface, pays six for each (their damage) and keeps
     * the Scarlet Witch untouched (apocalypse.md 3.14; the lance is a {@code ShieldCost}).
     */
    @GameTest(template = ARENA, batch = "cross_apocalypse_lances_shield", timeoutTicks = 140)
    public static void aChaosShieldStopsAVolleyOfSandLances(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().config(EnhancedConfig.CHAOS_SHIELD_STRENGTH, 200.0);
        ApocalypseEntity boss = d.boss();
        ServerPlayer wanda = d.hero();
        PowerData data = Powers.get(wanda);
        float health = wanda.getHealth();
        hold(wanda, 4);
        int[] firedAt = {-1};
        d.f().everyTick(120, tick -> {
            PowerTicker.tick(wanda);
            if (firedAt[0] < 0) {
                if (WandaAbilities.hasShieldUp(wanda) && boss.brain().force(boss, d.fight(), ApocalypseAttack.LANCES)) {
                    firedAt[0] = tick;
                }
                return false;
            }
            check(wanda.getHealth() == health, "no lance reaches Wanda (tick " + tick + ")");
            float paid = 200.0F - data.shieldStrength();
            if (tick - firedAt[0] < 40) {
                return false;
            }
            check(paid >= 6.0F && Math.abs(paid / 6.0F - Math.round(paid / 6.0F)) < 1.0E-3F, "the shield paid six for each lance: " + paid);
            check(Math.round(paid / 6.0F) >= 3, "and stopped most of the volley: " + paid / 6.0F + " lances");
            check(WandaAbilities.hasShieldUp(wanda), "and still stands");
            letGo(wanda, 4);
            return true;
        });
    }

    // ============================================================================================================
    // 5. Spider-Man's mechanical legs against a Zombie and against an Ultron Sentry

    /**
     * The Iron Spider's legs guard him against a Zombie and against a Sentry of a real Ultron fight: each is a threat and is
     * jabbed for the legs' five (spider-man.md 4.3.4; ultron.md 3.14 "they take the legs' 5 damage"). In Instant Kill the
     * Zombie dies at the first strike, and the Sentry, a boss's minion, still takes only the legs' damage.
     */
    @GameTest(template = ARENA, batch = "cross_spider_legs_ultron", timeoutTicks = 160)
    public static void spiderLegsJabAZombieAndASentryButKillOnlyTheZombie(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        ServerPlayer spider = tank(f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SPIDER_MAN));
        UltronFight fight = UltronTestSupport.onTheGround(f, spider);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, body, 18.5, FLOOR, 18.5);
        for (UltronSentryEntity other : fight.sentries()) {
            UltronTestSupport.put(f, other, 2.5, FLOOR + 1.0, 2.5);
        }
        UltronSentryEntity sentry = UltronTestSupport.sentryAt(f, fight, 12.5, 10.5);
        sentry.setHeld(true);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(8.5, FLOOR, 10.5));
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        zombie.setHealth(100.0F);
        wear(spider, "iron_spider");
        press(spider, 4);
        check(Powers.get(spider).toggle(Toggle.SPIDER_LEGS), "the legs are out");
        check(SpiderServer.isThreat(spider, zombie), "a Zombie is a threat");
        check(SpiderServer.isThreat(spider, sentry), "and so is a Sentry of Ultron's army, whoever it hunts");
        float legs = SpiderConfig.LEGS_DAMAGE.get().floatValue();
        float sentryHealth = sentry.getHealth();
        float zombieHealth = zombie.getHealth();
        int[] killFrom = {-1};
        f.everyTick(140, tick -> {
            PowerTicker.tick(spider);
            if (killFrom[0] < 0) {
                if (tick < 45) {
                    return false;
                }
                // Two or three jabs in 45 ticks, at the nearest threat each time: both were jabbed.
                check(zombie.getHealth() < zombieHealth, "the legs jabbed the Zombie: " + zombie.getHealth());
                check(sentry.getHealth() < sentryHealth, "and the Sentry: " + sentry.getHealth() + " of " + sentryHealth);
                check(sentryHealth - sentry.getHealth() <= 2 * legs + 0.01F, "for the legs' damage: " + (sentryHealth - sentry.getHealth()));
                check(zombie.isAlive() && sentry.isAlive(), "a jab is a stab, not a kill");
                sneakPress(spider, 4);
                sneakPress(spider, 4);
                check(Powers.get(spider).is(Live.INSTANT_KILL), "Instant Kill is on");
                killFrom[0] = tick;
                return false;
            }
            float before = sentry.getHealth();
            if (tick - killFrom[0] < 40) {
                return false;
            }
            check(!zombie.isAlive(), "Instant Kill killed the Zombie");
            check(sentry.isAlive(), "and not the Sentry, a minion of the fight: " + sentry.getHealth());
            check(sentry.getHealth() < before || sentry.getHealth() < sentryHealth - legs * 0.5F, "which the legs still strike");
            check(fight.sentries().contains(sentry), "and which still counts for the fight");
            return true;
        });
    }

    // ============================================================================================================
    // 6. A web shot at Thanos locks the Gauntlet

    /**
     * A Spider-Man's web shot at the Mad Titan does not web him (he is {@code web_immune}) but reaches the Gauntlet: it is
     * locked for {@code webLockTicks}, at most once per {@code webLockCooldown}, and a web in the clench of a stone move breaks
     * it and staggers him (spider-man.md 4.6.1, thanos.md 3.8).
     */
    @GameTest(template = ARENA, batch = "cross_spider_thanos", timeoutTicks = 140)
    public static void aWebShotAtThanosLocksTheGauntlet(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        f.config(ThanosBossConfig.WEB_LOCK_TICKS, 30);
        f.config(ThanosBossConfig.WEB_LOCK_COOLDOWN, 60);
        f.config(SpiderConfig.WEB_SHOT_COOLDOWN, 0);
        ServerPlayer spider = ThanosMoveSupport.fighter(f, -6, 0, Power.SPIDER_MAN);
        ThanosEntity boss = ThanosBossTests.boss(f, spider);
        boss.forceStones(2);
        lookAt(spider, boss);
        press(spider, 1);
        check(boss.gauntletLocked() && boss.flag(ThanosEntity.FLAG_GAUNTLET_LOCKED), "the web locked the Gauntlet");
        check(!boss.hasEffect(ModEffects.WEBBED), "and did not web him");
        int[] clenchAt = {-1};
        f.everyTick(120, tick -> {
            if (tick == 15) {
                check(boss.gauntletLocked(), "still locked");
                lookAt(spider, boss);
                press(spider, 1);
            }
            if (tick == 35) {
                check(!boss.gauntletLocked(), "the lock wore off after its 30 ticks, and a second web inside the cooldown did not renew it");
            }
            if (tick == 70) {
                boss.forceMove(ThanosMove.POWER_BLAST, spider);
                return false;
            }
            if (tick > 70 && clenchAt[0] < 0 && boss.clenching()) {
                clenchAt[0] = tick;
                lookAt(spider, boss);
                press(spider, 1);
                check(boss.state() == ThanosState.STAGGERED, "a web in the clench of a stone move breaks it: he staggers (" + boss.state() + ")");
                check(boss.gauntletLocked(), "with the Gauntlet locked");
                return true;
            }
            check(tick < 110, "the Power Blast never clenched: " + boss.state());
            return false;
        });
    }

    // ============================================================================================================
    // 7. Magneto against an Iron Spider, a Sentry and an Ultron body

    /**
     * The Iron Spider is a nanotech iron suit (spider-man.md 4.7): Magneto's Metal Telekinesis lifts him, and his strip
     * scatters the nanites (the suit's armour and the legs are off for a while, Instant Kill ends). In the homemade suit he is
     * cloth: Magneto cannot lift him.
     */
    @GameTest(template = ARENA, batch = "cross_magneto_spider", timeoutTicks = 60)
    public static void magnetoLiftsAndStripsAnIronSpiderButNotAHomemadeOne(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer magneto = f.player(4.5, FLOOR, 10.5, Power.MAGNETO);
        ServerPlayer spider = tank(f.player(9.5, FLOOR, 10.5, Power.SPIDER_MAN));
        PowerTicker.tick(spider);
        lookAt(magneto, spider);
        press(magneto, 1);
        check(!Telekinesis.isHolding(magneto), "a Spider-Man in the homemade suit is not metal");

        wear(spider, "iron_spider");
        double armour = spider.getAttributeValue(Attributes.ARMOR);
        check(armour >= 10.0, "the Iron Spider suit protects: " + armour);
        press(spider, 4);
        sneakPress(spider, 4);
        sneakPress(spider, 4);
        check(Powers.get(spider).is(Live.INSTANT_KILL), "Instant Kill is on");
        lookAt(magneto, spider);
        press(magneto, 1);
        check(Telekinesis.isHolding(magneto) && Powers.get(magneto).heldEntityId() == spider.getId(), "Magneto lifts the Iron Spider");
        sneakPress(magneto, 1);
        check(!Telekinesis.isHolding(magneto), "and sets him down");

        lookAt(magneto, spider);
        press(magneto, 3);
        PowerTicker.tick(spider);
        check(!Powers.get(spider).toggle(Toggle.SPIDER_LEGS), "the strip folds the legs");
        check(!Powers.get(spider).is(Live.INSTANT_KILL), "and ends Instant Kill");
        check(spider.getAttributeValue(Attributes.ARMOR) < armour, "and the suit's armour is gone for a while: " + spider.getAttributeValue(Attributes.ARMOR));
        check(Powers.get(magneto).cooldownUntil(3) > helper.getLevel().getGameTime(), "the strip found metal: it rests");
        helper.succeed();
    }

    /**
     * The Sentries are Stark's drone metal ({@code magnetic_entities}): Magneto's Metal Telekinesis lifts one, and his strip
     * finds nothing to take from it. Ultron's bodies are vibranium, not magnetic, and too powerful to lift: neither works on
     * them (ultron.md 3.14).
     */
    @GameTest(template = ARENA, batch = "cross_magneto_ultron", timeoutTicks = 60)
    public static void magnetoLiftsASentryButNotAnUltronBody(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        ServerPlayer magneto = tank(f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.MAGNETO));
        UltronFight fight = UltronTestSupport.onTheGround(f, magneto);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, magneto, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 9.5, FLOOR, 4.5);
        for (UltronSentryEntity other : fight.sentries()) {
            UltronTestSupport.put(f, other, 18.5, FLOOR + 1.0, 18.5);
        }
        UltronSentryEntity sentry = UltronTestSupport.sentryAt(f, fight, 9.5, 10.5);
        sentry.setHeld(true);

        lookAt(magneto, body);
        press(magneto, 1);
        check(!Telekinesis.isHolding(magneto), "Ultron's vibranium body cannot be lifted");
        float bodyHealth = body.getHealth();
        press(magneto, 3);
        check(body.isAlive() && body.getHealth() == bodyHealth, "and the strip does nothing to him");

        lookAt(magneto, sentry);
        float sentryHealth = sentry.getHealth();
        press(magneto, 3);
        check(Powers.get(magneto).isReady(3, helper.getLevel().getGameTime()), "a Sentry carries nothing to strip: the strip finds no metal to take");
        check(sentry.getHealth() == sentryHealth && sentry.isAlive(), "and does nothing to it");
        lookAt(magneto, sentry);
        press(magneto, 1);
        check(Telekinesis.isHolding(magneto) && Powers.get(magneto).heldEntityId() == sentry.getId(), "but Magneto lifts the Sentry");
        f.everyTick(20, tick -> {
            PowerTicker.tick(magneto);
            check(Telekinesis.isHolding(magneto), "and holds it (tick " + tick + ")");
            return tick >= 20;
        });
    }

    // ============================================================================================================
    // 8. Quicksilver against each boss

    /** Quicksilver Time with a small zone (the zone of 32 would reach the arenas round this one). */
    private static void smallZone(TestKit.Fixture f) {
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 8);
    }

    /**
     * Quicksilver Time slows the Mad Titan to one half (not one fifth) while he has no Time Stone and not at all with it
     * (thanos.md 3.9: {@code slowFactor}); Super Speed slows nothing.
     */
    @GameTest(template = ARENA, batch = "cross_quicksilver_thanos", timeoutTicks = 160)
    public static void quicksilverTimeSlowsThanosToHalfUntilTheTimeStone(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        smallZone(f);
        ServerPlayer peter = ThanosMoveSupport.fighter(f, -4, 0, Power.QUICKSILVER);
        ThanosEntity boss = ThanosBossTests.boss(f, peter);
        TickCounter counter = TickCounter.install(f);
        counter.watch(boss);
        press(peter, 1);
        f.everyTick(140, tick -> {
            PowerTicker.tick(peter);
            if (tick == 1) {
                counter.reset();
            }
            if (tick == 41) {
                check(Powers.get(peter).slowMo(), "Quicksilver Time is on");
                check(Math.abs(counter.ticks(boss) - 20) <= 2, "he ticks every second tick, not every fifth: " + counter.ticks(boss) + " of 40");
                boss.forceStones(5);
                check(boss.hasTimeStone(), "now he holds the Time Stone");
                counter.reset();
            }
            if (tick == 81) {
                check(counter.ticks(boss) >= 39, "and Quicksilver Time slows him no more: " + counter.ticks(boss) + " of 40");
                press(peter, 1);
                sneakPress(peter, 1);
                check(Powers.get(peter).superSpeed() && !Powers.get(peter).quicksilverTime(), "Super Speed instead");
                boss.forceStones(1);
                counter.reset();
            }
            if (tick == 121) {
                check(!Powers.get(peter).slowMo(), "Super Speed slows nothing");
                check(counter.ticks(boss) >= 39, "not even him: " + counter.ticks(boss) + " of 40");
                return true;
            }
            return false;
        });
    }

    /**
     * Ultron's bodies are not slowed by Quicksilver Time at all, his Sentries are, five times (ultron.md 3.14).
     */
    @GameTest(template = ARENA, batch = "cross_quicksilver_ultron", timeoutTicks = 100)
    public static void quicksilverTimeSlowsTheSentriesButNotUltron(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        smallZone(f);
        ServerPlayer peter = tank(f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.QUICKSILVER));
        UltronFight fight = UltronTestSupport.onTheGround(f, peter);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, body, 13.5, FLOOR, 10.5);
        UltronSentryEntity sentry = UltronTestSupport.sentryAt(f, fight, 7.5, 10.5);
        TickCounter counter = TickCounter.install(f);
        counter.watch(body);
        counter.watch(sentry);
        press(peter, 1);
        f.everyTick(60, tick -> {
            PowerTicker.tick(peter);
            if (tick == 1) {
                counter.reset();
            }
            if (tick == 51) {
                check(Powers.get(peter).slowMo(), "Quicksilver Time is on");
                check(counter.ticks(body) >= 48, "Ultron's body is not slowed: " + counter.ticks(body) + " of 50");
                check(counter.ticks(sentry) <= 12, "his Sentry is, five times: " + counter.ticks(sentry) + " of 50");
                return true;
            }
            return false;
        });
    }

    /**
     * Apocalypse adapts (apocalypse.md 3.14): Quicksilver Time slows him fully for {@code adaptTicks}, then he is immune to it
     * for a while and answers with an Entomb at the speedster's feet.
     */
    @GameTest(template = ARENA, batch = "cross_quicksilver_apocalypse", timeoutTicks = 200)
    public static void apocalypseAdaptsToQuicksilverTimeAndEntombsTheSpeedster(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.QUICKSILVER);
        smallZone(d.f());
        ServerPlayer peter = d.hero();
        ApocalypseEntity boss = d.boss();
        ApocalypseTestSupport.put(d.f(), peter, 6.5, FLOOR, 10.5);
        int adapt = ApocalypseBossConfig.ADAPT_TICKS.get();
        TickCounter counter = TickCounter.install(d.f());
        counter.watch(boss);
        press(peter, 1);
        int[] adaptedAt = {-1};
        d.f().everyTick(adapt + 80, tick -> {
            PowerTicker.tick(peter);
            if (tick == 1) {
                counter.reset();
            }
            if (tick == 41) {
                check(counter.ticks(boss) <= 10, "at first he is slowed fully, five times: " + counter.ticks(boss) + " of 40");
                check(!boss.hasAdapted(), "and has not adapted yet");
            }
            if (adaptedAt[0] < 0 && boss.hasAdapted()) {
                adaptedAt[0] = tick;
                check(tick >= adapt - 5, "he adapts after " + adapt + " ticks of it, not " + tick);
                counter.reset();
            }
            if (adaptedAt[0] > 0 && tick == adaptedAt[0] + 30) {
                check(counter.ticks(boss) >= 29, "then Quicksilver Time no longer slows him: " + counter.ticks(boss) + " of 30");
                check(peter.hasEffect(ModEffects.ENTOMBED), "and he answers with an Entomb at the speedster's feet");
                return true;
            }
            return false;
        });
    }

    // ============================================================================================================
    // 9. The Hex against each boss and each minion

    /** A Hex of radius 8 round Wanda in the middle of the floor, and a wall that never spares a creature without powers. */
    private static Hex hex(TestKit.Fixture f, ServerPlayer wanda) {
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        Hex hex = HexManager.create(wanda);
        f.onClose(() -> HexManager.dissolveAllOwnedBy(f.level().getServer(), wanda.getUUID()));
        return hex;
    }

    /**
     * Moves a creature across the wall of the Hex, from {@code inside} to {@code outside} (relative coordinates), one tick
     * apart, as if it walked through it: the wall sees it on one side and then on the other.
     */
    private static void crossOut(GameTestHelper helper, Entity entity, Vec3 inside, Vec3 outside, int atTick, int tick) {
        if (tick == atTick) {
            put(helper, entity, inside.x, inside.y, inside.z);
        } else if (tick == atTick + 2) {
            put(helper, entity, outside.x, outside.y, outside.z);
        }
    }

    private static boolean inside(Hex hex, Entity entity) {
        return hex.contains(entity.position().add(0, entity.getBbHeight() * 0.5, 0));
    }

    /**
     * The Mad Titan is {@code hex_resistant}: the wall bounces him back, alive, whichever way he goes; his phantoms are
     * creatures without powers and die at it (thanos.md 3.9: "a Hex is a perfect arena").
     */
    @GameTest(template = ARENA, batch = "cross_hex_thanos", timeoutTicks = 120)
    public static void theHexBouncesThanosAndUnmakesHisPhantoms(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        ServerPlayer wanda = ThanosMoveSupport.fighter(f, 0, 3, Power.SCARLET_WITCH);
        ThanosEntity boss = ThanosBossTests.boss(f, wanda);
        Hex hex = hex(f, wanda);
        ServerLevel level = f.level();
        ThanosPhantomEntity phantom = com.kolodkingal.enhanced.registry.ModEntities.THANOS_PHANTOM.get().create(level);
        put(helper, phantom, 12.5, FLOOR, 13.5);
        phantom.setNoAi(true);
        boss.fight().spawn(phantom);
        f.everyTick(100, tick -> {
            crossOut(helper, boss, new Vec3(14.5, FLOOR, 13.5), new Vec3(19.5, FLOOR, 13.5), 5, tick);
            crossOut(helper, phantom, new Vec3(12.5, FLOOR, 13.5), new Vec3(19.5, FLOOR, 18.5), 5, tick);
            if (tick < 20) {
                return false;
            }
            check(boss.isAlive() && inside(hex, boss), "Thanos was thrown back inside the Hex, alive: at " + rel(helper, boss));
            check(!phantom.isAlive() || phantom.isRemoved(), "his phantom did not survive the wall: at " + rel(helper, phantom));
            return true;
        });
    }

    /**
     * Ultron's bodies are {@code hex_resistant} (bounced, never killed) and his Sentries die at the wall (ultron.md 3.14),
     * and a Sentry that dies at the wall leaves the fight's count.
     */
    @GameTest(template = ARENA, batch = "cross_hex_ultron", timeoutTicks = 120)
    public static void theHexBouncesUltronAndUnmakesHisSentries(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        ServerPlayer wanda = tank(f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH));
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        for (UltronSentryEntity other : fight.sentries()) {
            UltronTestSupport.put(f, other, 10.5, FLOOR + 1.0, 4.5);
        }
        UltronSentryEntity sentry = UltronTestSupport.sentryAt(f, fight, 12.5, 13.5);
        sentry.setHeld(true);
        Hex hex = hex(f, wanda);
        f.everyTick(100, tick -> {
            crossOut(helper, body, new Vec3(14.5, FLOOR, 13.5), new Vec3(19.5, FLOOR, 13.5), 5, tick);
            crossOut(helper, sentry, new Vec3(12.5, FLOOR + 1.0, 13.5), new Vec3(19.5, FLOOR + 1.0, 18.5), 5, tick);
            if (tick < 20) {
                return false;
            }
            check(body.isAlive() && inside(hex, body), "Ultron's body was thrown back inside, alive: at " + rel(helper, body));
            check(!sentry.isAlive(), "the Sentry did not survive the wall: at " + rel(helper, sentry));
            fight.refresh();
            check(!fight.sentries().contains(sentry), "and is no longer one of the fight's");
            return true;
        });
    }

    /**
     * Apocalypse is {@code hex_resistant}: bounced, never killed; the Horsemen are Husks, creatures without powers, and die at
     * the wall (apocalypse.md 3.14).
     */
    @GameTest(template = ARENA, batch = "cross_hex_apocalypse", timeoutTicks = 120)
    public static void theHexBouncesApocalypseAndUnmakesHisHorsemen(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        ServerPlayer wanda = d.hero();
        ApocalypseEntity boss = d.boss();
        ApocalypseTestSupport.put(d.f(), wanda, 10.5, FLOOR, 8.5);
        Husk war = d.fight().horsemen().spawn(d.level(), Horsemen.Kind.WAR, helper.absoluteVec(new Vec3(12.5, FLOOR, 13.5)));
        war.setNoAi(true);
        check(Horsemen.kindOf(war) == Horsemen.Kind.WAR, "War rides for him");
        Hex hex = hex(d.f(), wanda);
        d.f().everyTick(100, tick -> {
            crossOut(helper, boss, new Vec3(14.5, FLOOR, 13.5), new Vec3(19.5, FLOOR, 13.5), 5, tick);
            crossOut(helper, war, new Vec3(12.5, FLOOR, 13.5), new Vec3(19.5, FLOOR, 18.5), 5, tick);
            if (tick < 20) {
                return false;
            }
            check(boss.isAlive() && inside(hex, boss), "Apocalypse was thrown back inside, alive: at " + rel(helper, boss));
            check(!war.isAlive(), "War did not survive the wall: at " + rel(helper, war));
            check(d.fight().horsemenAlive().stream().noneMatch(h -> h == war), "and rides for him no more");
            return true;
        });
    }

    private CrossHeroBossTests() {
    }
}

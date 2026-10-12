package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PhoenixBurst;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.network.PhoenixBurstPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.PhoenixTests.actionBarKeys;
import static com.kolodkingal.enhanced.gametest.PhoenixTests.jean;
import static com.kolodkingal.enhanced.gametest.PhoenixTests.payloads;
import static com.kolodkingal.enhanced.gametest.PhoenixTests.press;
import static com.kolodkingal.enhanced.gametest.PhoenixTests.rested;
import static com.kolodkingal.enhanced.gametest.PhoenixTests.sneakPress;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The Phoenix Burst: the charge, the waves, what they do and to whom. A wave is server-wide state that reaches whatever
 * stands in its sphere, and the settings a Burst test changes are server-wide too, so each test that lets a wave go has
 * a batch of its own (and a radius that fits in its arena, 9 blocks from the middle). The tests that only hurt one
 * creature with the Burst's damage type are in the quick batch of {@link PhoenixTests}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class PhoenixBurstTests {
    /** Where the Phoenix stands in the tests that spread a wave: the targets are placed along +X from here. */
    private static final double X = 5.5;
    private static final double Z = 10.5;

    /**
     * A charge of 10 ticks that fires by itself, radii of 4 to 9 blocks (so that a wave stays in its arena), no fire on
     * the creatures it hits and no push, unless a test says otherwise.
     */
    private static void smallBursts(TestKit.Fixture f) {
        f.config(PhoenixConfig.BURST_CHARGE_TICKS, 10);
        f.config(PhoenixConfig.BURST_MIN_CHARGE_TICKS, 0);
        f.config(PhoenixConfig.BURST_MIN_RADIUS, 4);
        f.config(PhoenixConfig.BURST_MAX_RADIUS, 9);
        f.config(PhoenixConfig.BURST_FIRE_SECONDS, 0);
        f.config(PhoenixConfig.BURST_KNOCKBACK, 0.0);
    }

    /** A zombie with 200 health that stands still, so that what the Burst does to it can be measured. */
    private static Zombie sturdy(GameTestHelper helper, double x, double z) {
        Zombie zombie = PhoenixTests.zombie(helper, x, z);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        zombie.setHealth(200.0F);
        return zombie;
    }

    /** What a creature at this distance takes of a Burst whose damage at the centre is {@code damage}. */
    private static double expected(double damage, double distance, double radius) {
        double half = radius * 0.5;
        double falloff = distance <= half ? 1.0 : 1.0 - 0.4 * (distance - half) / half;
        return damage * Math.max(0.6, falloff);
    }

    private static double distance(ServerPlayer jean, LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(jean.position().add(0, jean.getBbHeight() * 0.5, 0));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The charge

    @GameTest(template = ARENA, batch = "phoenix_burst_charge")
    public static void burstChargesAndRisesAndHoldsHerStill(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 10.5, 10.5);
        PowerData data = Powers.get(jean);
        AttributeInstance speed = jean.getAttribute(Attributes.MOVEMENT_SPEED);
        float exhaustion = jean.getFoodData().getExhaustionLevel();
        long now = level.getGameTime();

        AbilityHandler.onKey(jean, 5, true);
        check(data.is(Live.PHOENIX_CHARGING) && data.is(Live.PHOENIX_BLAZE), "N starts the charge and the flames");
        PowerTicker.tick(jean);
        check(jean.getAbilities().flying, "the charge lifts her (she hovers)");
        assertNear(jean.getDeltaMovement().y, 0.12, 1.0E-6, "rising at 0.12 blocks per tick");
        check(jean.hurtMarked, "and the client is told");
        assertNear(speed.getValue(), speed.getBaseValue() * 0.4, 1.0E-4, "her walking speed is cut to 40 percent");
        // She hovers while she charges, and a hovering flier moves at her flying speed, not at the walking speed
        // (found in game: she charged at three times a walk): so that is cut too, every tick the charge lasts.
        float flying = Power.PHOENIX.flightSpeed();
        assertNear(jean.getAbilities().getFlyingSpeed(), flying * 0.4, 1.0E-4, "and so is her flying speed");
        PowerTicker.tick(jean);
        assertNear(jean.getAbilities().getFlyingSpeed(), flying * 0.4, 1.0E-4, "tick after tick");

        // The flames that spiral up her are for the others to see: sent to her, a spark that spawns a block from her
        // camera is a blob over her whole screen (found in game). She sees her own from the client.
        ServerPlayer watcher = f.player(16.5, FLOOR, 10.5);
        TestKit.sentPackets(jean);
        TestKit.sentPackets(watcher);
        PowerTicker.tick(jean);
        check(TestKit.particlesIn(TestKit.sentPackets(jean)).isEmpty(), "the server sends her none of her own flames");
        check(!TestKit.particlesIn(TestKit.sentPackets(watcher)).isEmpty(), "but whoever is near sees them");

        // 1.5 blocks above the ground she stops rising and hangs there.
        jean.setPos(jean.getX(), jean.getY() + 1.6, jean.getZ());
        jean.setDeltaMovement(Vec3.ZERO);
        PowerTicker.tick(jean);
        assertNear(jean.getDeltaMovement().y, 0.0, 1.0E-6, "at 1.6 blocks she is high enough");
        jean.setPos(jean.getX(), jean.getY() - 0.8, jean.getZ());
        PowerTicker.tick(jean);
        assertNear(jean.getDeltaMovement().y, 0.12, 1.0E-6, "at 0.8 she rises again");

        // Letting go too soon (the Burst needs 12 ticks): the charge slips away.
        actionBarKeys(jean);
        AbilityHandler.onKey(jean, 5, false);
        check(!data.is(Live.PHOENIX_CHARGING) && !data.is(Live.PHOENIX_BLAZE), "let go too soon, the charge and its flames are gone");
        check(PhoenixBurst.active() == 0, "no wave goes out");
        check(data.cooldownUntil(5) == now + 40, "she rests a short while (2 seconds): " + (data.cooldownUntil(5) - now));
        check(actionBarKeys(jean).contains("message.enhanced.phoenix.cancelled"), "and is told it slipped away");
        assertNear(jean.getFoodData().getExhaustionLevel() - exhaustion, 0.0, 1.0E-6, "it cost no hunger");
        check(!jean.getAbilities().flying, "she is put down again, since the charge lifted her");
        PowerTicker.tick(jean);
        check(!speed.hasModifier(Enhanced.id("phoenix_charge_slow")), "and her speed is her own");
        assertNear(jean.getAbilities().getFlyingSpeed(), flying, 1.0E-4, "her flying speed too");

        AbilityHandler.onKey(jean, 5, true);
        check(!data.is(Live.PHOENIX_CHARGING) && actionBarKeys(jean).contains("message.enhanced.phoenix.not_ready"),
                "she cannot charge again until she has rested");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_cost", timeoutTicks = 60)
    public static void burstCostsHungerAndStartsTheCooldown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(PhoenixConfig.BURST_CHARGE_TICKS, 10);
        f.config(PhoenixConfig.BURST_MAX_RADIUS, 9);
        f.config(PhoenixConfig.BURST_MIN_RADIUS, 4);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 10.5, 10.5);
        ServerPlayer creative = f.player(2.5, FLOOR, 2.5, GameType.CREATIVE);
        Powers.setPower(creative, Power.PHOENIX);
        ServerPlayer hungry = f.player(18.5, FLOOR, 2.5, Power.PHOENIX);
        hungry.getFoodData().setFoodLevel(5);
        PowerData data = Powers.get(jean);
        float exhaustion = jean.getFoodData().getExhaustionLevel();
        float creativeExhaustion = creative.getFoodData().getExhaustionLevel();

        AbilityHandler.onKey(hungry, 5, true);
        check(!Powers.get(hungry).is(Live.PHOENIX_CHARGING) && actionBarKeys(hungry).contains("message.enhanced.phoenix.too_hungry"),
                "with less food than the Burst costs she cannot even charge");
        AbilityHandler.onKey(jean, 5, true);
        AbilityHandler.onKey(creative, 5, true);

        f.everyTick(30, tick -> {
            PowerTicker.tick(jean);
            PowerTicker.tick(creative);
            if (tick < 12) {
                return false;
            }
            long now = level.getGameTime();
            check(!data.is(Live.PHOENIX_CHARGING), "a full charge fires by itself");
            assertNear(jean.getFoodData().getExhaustionLevel() - exhaustion, 24.0, 0.01, "a full Burst costs 6 food points (24 of exhaustion)");
            assertNear(creative.getFoodData().getExhaustionLevel() - creativeExhaustion, 0.0, 1.0E-6, "in Creative it costs nothing");
            check(!data.isReady(5, now) && data.cooldownUntil(5) - now > 1790 && data.cooldownUntil(5) - now <= 1800, "she rests 90 seconds");
            MobEffectInstance weak = jean.getEffect(MobEffects.WEAKNESS);
            MobEffectInstance slow = jean.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            check(weak != null && slow != null && weak.getDuration() == 100 && slow.getDuration() == 100 && weak.getAmplifier() == 0,
                    "and is drained for 5 seconds: Weakness I and Slowness I");
            check(creative.getEffect(MobEffects.WEAKNESS) == null && creative.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null, "not in Creative");

            List<Object> sent = TestKit.sentPackets(jean);
            check(!payloads(sent, PhoenixBurstPayload.class).isEmpty(), "everyone nearby is told of the Burst");
            AbilityHandler.onKey(jean, 5, true);
            check(!data.is(Live.PHOENIX_CHARGING) && actionBarKeys(jean).contains("message.enhanced.phoenix.not_ready"), "she must rest before the next");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_cleanse", timeoutTicks = 60)
    public static void burstCleansesSpellsOnJean(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        f.config(PhoenixConfig.BURST_DRAIN_TICKS, 0);
        ServerPlayer jean = jean(f, 10.5, 10.5);
        jean.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 200));
        jean.addEffect(new MobEffectInstance(ModEffects.CONFUSED, 200));
        jean.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200));
        jean.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200));
        jean.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
        jean.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200));
        jean.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(30, tick -> {
            PowerTicker.tick(jean);
            if (tick < 11) {
                return false;
            }
            check(PhoenixBurst.active() > 0 || tick > 12, "the Burst went off");
            for (var gone : List.of(ModEffects.SPELLBOUND, ModEffects.CONFUSED, MobEffects.CONFUSION, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.BLINDNESS,
                    MobEffects.DARKNESS)) {
                check(!jean.hasEffect(gone), "the fire burns off " + gone.getRegisteredName());
            }
            check(jean.hasEffect(MobEffects.MOVEMENT_SPEED), "but not a gift like Speed");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The wave

    @GameTest(template = ARENA, batch = "phoenix_burst", timeoutTicks = 80)
    public static void burstScalesWithTheChargeAndSpreadsAtWaveSpeed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, X, Z);
        double[] offsets = {2.5, 5.5, 8.5, 9.6};
        Zombie[] zombies = new Zombie[offsets.length];
        double[] distances = new double[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            zombies[i] = sturdy(helper, X + offsets[i], Z);
            distances[i] = distance(jean, zombies[i]);
        }
        List<String> told = new ArrayList<>();
        PhoenixPower.Listener listener = (phoenix, target, kind, amount) -> {
            if (phoenix == jean && kind == PhoenixPower.Kind.BURST) {
                told.add(target.getId() + ":" + amount);
            }
        };
        PhoenixPower.addListener(listener);
        f.onClose(() -> PhoenixPower.removeListener(listener));
        TestKit.sentPackets(jean);

        int[] hitAt = {-1, -1, -1, -1};
        int[] firedAt = {-1};
        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(50, tick -> {
            PowerTicker.tick(jean);
            if (firedAt[0] < 0 && PhoenixBurst.active() > 0) {
                firedAt[0] = tick;
                List<PhoenixBurstPayload> bursts = payloads(TestKit.sentPackets(jean), PhoenixBurstPayload.class);
                check(bursts.size() == 1 && bursts.get(0).caster() == jean.getId(), "the Burst is announced once, by her");
                assertNear(bursts.get(0).charge(), 1.0, 1.0E-6, "at full charge");
                assertNear(bursts.get(0).radius(), 9.0, 1.0E-6, "with its radius (a full charge: 9 here)");
                check(bursts.get(0).centre().distanceTo(jean.position().add(0, jean.getBbHeight() * 0.5, 0)) < 1.0E-6, "from where she hovers");
            }
            for (int i = 0; i < zombies.length; i++) {
                if (hitAt[i] < 0 && zombies[i].getHealth() < 200.0F) {
                    hitAt[i] = tick;
                }
            }
            if (firedAt[0] < 0 || PhoenixBurst.active() > 0 || tick < firedAt[0] + 10) {
                return false;
            }
            check(firedAt[0] == 10, "a full charge of 10 ticks fires by itself on the tenth");
            check(hitAt[0] > 0 && hitAt[1] > hitAt[0] && hitAt[2] > hitAt[1], "the wave reaches the near ones first: " + List.of(hitAt[0], hitAt[1], hitAt[2]));
            check(Math.abs((hitAt[1] - hitAt[0]) - 3) <= 1 && Math.abs((hitAt[2] - hitAt[1]) - 3) <= 1,
                    "one block per tick: 3 blocks further, 3 ticks later " + List.of(hitAt[0], hitAt[1], hitAt[2]));
            check(zombies[3].getHealth() == 200.0F, "a creature beyond the radius (9.6 of 9) is never hurt");
            for (int i = 0; i < 3; i++) {
                double taken = 200.0 - zombies[i].getHealth();
                double want = expected(30.0, distances[i], 9.0);
                assertNear(taken, want, 0.3, "the zombie " + offsets[i] + " blocks away takes the Burst once (30 at the centre, 60 percent at the edge)");
            }
            assertNear(200.0 - zombies[0].getHealth(), 30.0, 0.1, "30 at the centre of a full Burst");
            check(told.size() == 3, "the listeners hear of each creature it reaches once: " + told);
            for (int i = 0; i < 3; i++) {
                int id = zombies[i].getId();
                float said = told.stream().filter(entry -> entry.startsWith(id + ":")).map(entry -> Float.parseFloat(entry.substring(entry.indexOf(':') + 1)))
                        .findFirst().orElse(-1.0F);
                assertNear(said, 200.0 - zombies[i].getHealth(), 0.01, "and told what it did to zombie " + i);
            }
            DamageSource last = zombies[2].getLastDamageSource();
            check(last != null && last.is(ModDamageTypes.PHOENIX_BURST) && last.getEntity() == jean, "Jean gets the credit");
            check(PhoenixPower.isPhoenixMindPower(last), "and the damage is her mind power");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_half", timeoutTicks = 80)
    public static void aHalfChargedBurstReachesLessFarHurtsLessAndBurnsLess(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        f.config(PhoenixConfig.BURST_FIRE_SECONDS, 10);
        ServerPlayer jean = jean(f, X, Z);
        Zombie near = sturdy(helper, X + 3.0, Z);
        Zombie out = sturdy(helper, X + 7.5, Z);
        check(Math.abs(PhoenixBurst.radius(0.5F) - 6.5F) < 1.0E-4 && Math.abs(PhoenixBurst.damage(0.5F) - 19.0F) < 1.0E-4,
                "half a charge: a radius of 6.5 and a damage of 19 here");
        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(50, tick -> {
            PowerTicker.tick(jean);
            if (tick == 5) {
                AbilityHandler.onKey(jean, 5, false);
            }
            if (tick < 20) {
                return false;
            }
            check(PhoenixBurst.active() == 0, "the wave is over");
            check(out.getHealth() == 200.0F && !out.isOnFire(), "a creature 7.5 blocks away is out of a half Burst's reach");
            assertNear(200.0 - near.getHealth(), 19.0, 2.5, "the near one takes about 19 (and a point or two of burning)");
            check(near.isOnFire() && near.getRemainingFireTicks() > 60 && near.getRemainingFireTicks() <= 140,
                    "and burns for about 7 seconds (a weaker Burst burns 40 to 100 percent as long): " + near.getRemainingFireTicks());
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_allies", timeoutTicks = 80)
    public static void burstSparesHerAlliesAndHurtsThePlayersItMayWithPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        f.pvp(true);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, X, Z);
        Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(X + 3.0, FLOOR, Z));
        pet.tame(jean);
        ServerPlayer teammate = f.player(X + 3.0, FLOOR, Z + 3.0);
        ServerPlayer creative = f.player(X + 3.0, FLOOR, Z - 3.0, GameType.CREATIVE);
        ServerPlayer stranger = f.player(X + 4.0, FLOOR, Z + 1.0);
        ServerPlayer magneto = f.player(X + 4.0, FLOOR, Z - 1.0, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        Zombie foe = sturdy(helper, X + 2.0, Z + 5.0);
        Zombie host = sturdy(helper, X + 1.0, Z - 4.0);
        lookAt(jean, host);
        press(jean, 4);
        check(host.hasEffect(ModEffects.POSSESSED), "one zombie is her host");
        Scoreboard scoreboard = level.getServer().getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("phoenix_test_" + UUID.randomUUID().toString().substring(0, 8));
        scoreboard.addPlayerToTeam(jean.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(teammate.getScoreboardName(), team);
        f.onClose(() -> scoreboard.removePlayerTeam(team));

        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(50, tick -> {
            PowerTicker.tick(jean);
            if (tick < 22) {
                return false;
            }
            check(PhoenixBurst.active() == 0, "the wave is over");
            check(jean.getHealth() == jean.getMaxHealth(), "it never hurts her");
            check(pet.getHealth() == pet.getMaxHealth(), "nor her pet");
            check(teammate.getHealth() == teammate.getMaxHealth(), "nor her teammate");
            check(host.getHealth() == 200.0F, "nor the creature she has taken over");
            check(creative.getHealth() == creative.getMaxHealth(), "nor a Creative player");
            assertNear(stranger.getMaxHealth() - stranger.getHealth(), 30.0 * 0.5 * (expected(1.0, distance(jean, stranger), 9.0)), 0.3,
                    "a stranger takes half of it, with PvP on");
            check(magneto.getHealth() < magneto.getMaxHealth(), "so does Magneto in his helmet (it blocks the mind, not the fire)");
            check(foe.getHealth() < 200.0F, "and a zombie after her");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_passives", timeoutTicks = 160)
    public static void burstSparesVillagersAndAnimalsByDefault(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        f.config(PhoenixConfig.BURST_COOLDOWN, 0);
        f.config(PhoenixConfig.BURST_DRAIN_TICKS, 0);
        f.config(PhoenixConfig.BURST_FIRE_SECONDS, 10);
        f.config(PhoenixConfig.BURST_MIN_DAMAGE, 5.0);
        f.config(PhoenixConfig.BURST_MAX_DAMAGE, 5.0);
        ServerPlayer jean = jean(f, X, Z);
        ServerPlayer owner = f.player(2.5, FLOOR, 18.5);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(X + 3.0, FLOOR, Z + 2.0));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(X + 3.0, FLOOR, Z - 2.0));
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(X + 4.0, FLOOR, Z + 4.0));
        Wolf strangers = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(X + 4.0, FLOOR, Z - 4.0));
        strangers.tame(owner);
        Zombie zombie = sturdy(helper, X + 2.0, Z + 3.0);
        Ghast ghast = helper.spawnWithNoFreeWill(EntityType.GHAST, new Vec3(X + 6.0, FLOOR + 1.0, Z));
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(X + 3.0, FLOOR, Z - 5.0));
        List<LivingEntity> passive = List.of(villager, golem, cow, strangers);
        List<LivingEntity> hostile = List.of(zombie, ghast, warden);

        int[] phase = {1};
        int[] since = {0};
        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(150, tick -> {
            PowerTicker.tick(jean);
            since[0]++;
            if (since[0] < 24) {
                return false;
            }
            if (phase[0] == 1) {
                for (LivingEntity spared : passive) {
                    check(spared.getHealth() == spared.getMaxHealth() && !spared.isOnFire(), spared.getType().getDescription().getString()
                            + " is spared (and not set alight)");
                }
                for (LivingEntity hurt : hostile) {
                    check(hurt.getHealth() < hurt.getMaxHealth(), hurt.getType().getDescription().getString() + " is hurt (a boss counts as hostile)");
                }
                // The option for the villages that were meant to burn.
                f.config(PhoenixConfig.BURST_HURTS_PASSIVES, true);
                rested(jean, 5);
                AbilityHandler.onKey(jean, 5, true);
                phase[0] = 2;
                since[0] = 0;
                return false;
            }
            for (LivingEntity burned : passive) {
                check(burned.getHealth() < burned.getMaxHealth(), burned.getType().getDescription().getString() + " is hurt with burstHurtsPassives on");
            }
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_knock", timeoutTicks = 80)
    public static void burstKnockbackSkipsBosses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        f.config(PhoenixConfig.BURST_KNOCKBACK, 1.2);
        ServerPlayer jean = jean(f, X, Z);
        Zombie zombie = sturdy(helper, X + 3.0, Z);
        // The Warden shrugs off a push anyway; take that away, so that only the tag keeps it still. Every hit still gives
        // a creature the small kick of vanilla damage (0.4), which the boss keeps; the Burst's own push (1.2 more) it does not get.
        Warden boss = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(X, FLOOR, Z + 3.0));
        boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
        float bossHealth = boss.getHealth();
        Vec3 zombieStart = zombie.position();
        Vec3 bossStart = boss.position();
        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(50, tick -> {
            PowerTicker.tick(jean);
            if (tick < 22) {
                return false;
            }
            double zombiePush = zombie.position().subtract(zombieStart).horizontalDistance();
            double bossPush = boss.position().subtract(bossStart).horizontalDistance();
            check(zombie.position().subtract(zombieStart).x > 1.0, "a zombie is thrown away from the centre: " + zombiePush);
            check(boss.getHealth() < bossHealth, "a boss is hurt");
            check(bossPush < zombiePush * 0.6, "but the boss is not thrown with it: " + bossPush + " against " + zombiePush);
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_cover", timeoutTicks = 80)
    public static void burstDoesNotGetThroughStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        f.config(PhoenixConfig.BURST_FIRE_SECONDS, 10);
        ServerPlayer jean = jean(f, X, Z);
        // A wall of stone, one block thick, four high and seven wide, between her and the zombie behind it.
        for (int z = 7; z <= 13; z++) {
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(7, y, z), Blocks.STONE);
            }
        }
        Zombie behind = sturdy(helper, X + 3.0, Z);
        Zombie clear = sturdy(helper, X, Z + 4.0);
        double clearDistance = distance(jean, clear);
        List<LivingEntity> told = new ArrayList<>();
        PhoenixPower.Listener listener = (phoenix, target, kind, amount) -> {
            if (phoenix == jean && kind == PhoenixPower.Kind.BURST) {
                told.add(target);
            }
        };
        PhoenixPower.addListener(listener);
        f.onClose(() -> PhoenixPower.removeListener(listener));
        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(50, tick -> {
            PowerTicker.tick(jean);
            if (tick < 22) {
                return false;
            }
            check(PhoenixBurst.active() == 0, "the wave is over");
            check(behind.getHealth() == 200.0F && !behind.isOnFire(), "a creature behind a wall of stone takes nothing and is not set alight: " + behind.getHealth());
            check(!told.contains(behind), "and nobody is told the Burst touched it");
            assertNear(200.0 - clear.getHealth(), expected(30.0, clearDistance, 9.0), 0.3, "one in the open the same distance away takes the Burst");
            check(clear.isOnFire() && told.contains(clear), "and burns");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_border", timeoutTicks = 100)
    public static void burstLightsNoFireWhereTheWorldWillNotLetHerBuild(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        f.config(PhoenixConfig.BURST_MAX_FIRE_BLOCKS, 400);
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.OAK_PLANKS);
            }
        }
        ServerPlayer jean = jean(f, 10.5, 10.5);
        // The world border stands 5 blocks from her on every side (a place nobody may build is a place nobody may burn).
        WorldBorder border = level.getWorldBorder();
        double centreX = border.getCenterX();
        double centreZ = border.getCenterZ();
        double size = border.getSize();
        f.onClose(() -> {
            border.setCenter(centreX, centreZ);
            border.setSize(size);
        });
        Vec3 middle = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        border.setCenter(middle.x, middle.z);
        border.setSize(10.0);
        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(70, tick -> {
            PowerTicker.tick(jean);
            if (tick < 24) {
                return false;
            }
            int inside = 0;
            int outside = 0;
            for (int x = 0; x < 21; x++) {
                for (int z = 0; z < 21; z++) {
                    BlockPos pos = helper.absolutePos(new BlockPos(x, 2, z));
                    if (level.getBlockState(pos).is(Blocks.FIRE)) {
                        if (border.isWithinBounds(pos)) {
                            inside++;
                        } else {
                            outside++;
                        }
                    }
                }
            }
            check(inside > 0, "(control: the wave does light fires where the world lets her build)");
            check(outside == 0, "but none outside the world border: " + outside);
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_nopvp", timeoutTicks = 80)
    public static void pvpOffProtectsPlayersFromAllHarmfulAbilities(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        f.config(PhoenixConfig.BURST_KNOCKBACK, 1.2);
        f.config(PhoenixConfig.BURST_FIRE_SECONDS, 10);
        f.config(PhoenixConfig.PSIONIC_RADIUS, 8);
        f.pvp(false);
        ServerPlayer jean = jean(f, X, Z);
        ServerPlayer other = f.player(X + 3.0, FLOOR, Z + 3.0);
        Zombie zombie = sturdy(helper, X + 3.0, Z - 3.0);
        PowerTicker.tick(jean);

        // Mind Reading still reads, and tells.
        lookAt(jean, other);
        actionBarKeys(other);
        press(jean, 1);
        com.kolodkingal.enhanced.ability.MindReading.look(jean, 1000);
        check(actionBarKeys(other).contains("message.enhanced.mind_read.felt"), "Mind Reading still reads a player, and the player feels it");
        // Mind Control does not reach a player.
        press(jean, 4);
        check(!other.hasEffect(ModEffects.SPELLBOUND) && actionBarKeys(jean).contains("message.enhanced.mind_control.no_pvp"), "Mind Control cannot hold a player");
        // The Psionic Field holds the zombie and not the player.
        sneakPress(jean, 4);
        check(zombie.hasEffect(ModEffects.SPELLBOUND) && !other.hasEffect(ModEffects.SPELLBOUND), "the Psionic Field holds the mob and not the player");

        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(50, tick -> {
            PowerTicker.tick(jean);
            if (tick < 22) {
                return false;
            }
            check(other.getHealth() == other.getMaxHealth() && !other.isOnFire() && other.getDeltaMovement().horizontalDistance() < 0.1,
                    "the Burst leaves the player alone: no damage, no fire, no push");
            check(zombie.getHealth() < 200.0F, "though it hurts the zombie");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The ground

    @GameTest(template = ARENA, batch = "phoenix_burst_fire", timeoutTicks = 260)
    public static void burstNeverBreaksBlocksAndSetsFireOnlyWithMobGriefing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        smallBursts(f);
        f.config(PhoenixConfig.BURST_COOLDOWN, 0);
        f.config(PhoenixConfig.BURST_DRAIN_TICKS, 0);
        f.config(PhoenixConfig.BURST_MAX_FIRE_BLOCKS, 5);
        ServerLevel level = helper.getLevel();
        // A floor of planks (and the stone floor round it).
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), x >= 3 && x <= 18 && z >= 3 && z <= 18 ? Blocks.OAK_PLANKS : Blocks.STONE);
            }
        }
        ServerPlayer jean = jean(f, 10.5, 10.5);
        boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        f.onClose(() -> level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(griefing, level.getServer()));

        int[] phase = {1};
        int[] since = {0};
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, level.getServer());
        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(240, tick -> {
            PowerTicker.tick(jean);
            since[0]++;
            if (since[0] < 24) {
                return false;
            }
            int fires = countFires(helper);
            int planks = countBlocks(helper, Blocks.OAK_PLANKS);
            check(planks == 16 * 16 && countBlocks(helper, Blocks.STONE) == 21 * 21 - 16 * 16, "no block was broken in phase " + phase[0]);
            if (phase[0] == 1) {
                check(fires == 0, "with mobGriefing off the wave lights nothing: " + fires);
                level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, level.getServer());
                f.config(PhoenixConfig.BURST_SETS_FIRE, false);
                rested(jean, 5);
                AbilityHandler.onKey(jean, 5, true);
                phase[0] = 2;
                since[0] = 0;
                return false;
            }
            if (phase[0] == 2) {
                check(fires == 0, "with burstSetsFire off neither: " + fires);
                f.config(PhoenixConfig.BURST_SETS_FIRE, true);
                rested(jean, 5);
                AbilityHandler.onKey(jean, 5, true);
                phase[0] = 3;
                since[0] = 0;
                return false;
            }
            check(fires == 5, "with both on it lights fires, at most burstMaxFireBlocks (5): " + fires);
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_burst_arena", timeoutTicks = 100)
    public static void burstLightsNoFireInAProtectedBossArena(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        smallBursts(f);
        f.config(PhoenixConfig.BURST_MAX_FIRE_BLOCKS, 400);
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.OAK_PLANKS);
            }
        }
        ServerPlayer jean = jean(f, 10.5, 10.5);
        // A fight whose arena protects the blocks within 4 of the middle; the Burst reaches 9.
        BossArena arena = new BossTestSupport.TestArena(level, helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)), 4.0, BossArena.Containment.NONE, true)
                .containAt(3.0).joinWithin(5.0).tractorDepth(0.0);
        BossFrameworkTests.start(f, BossKind.ULTRON, jean, new BossTestSupport.TestFight(level, BossKind.ULTRON, arena));
        check(BossFights.protects(level, helper.absolutePos(new BlockPos(10, 2, 12))) && !BossFights.protects(level, helper.absolutePos(new BlockPos(10, 2, 16))),
                "the arena protects the ground within 4 blocks of the middle, and not the rest");
        AbilityHandler.onKey(jean, 5, true);
        f.everyTick(70, tick -> {
            PowerTicker.tick(jean);
            if (tick < 24) {
                return false;
            }
            int inside = 0;
            int outside = 0;
            for (int x = 0; x < 21; x++) {
                for (int z = 0; z < 21; z++) {
                    if (level.getBlockState(helper.absolutePos(new BlockPos(x, 2, z))).is(Blocks.FIRE)) {
                        double d = Math.hypot(x + 0.5 - 10.5, z + 0.5 - 10.5);
                        if (d < 4.0) {
                            inside++;
                        } else {
                            outside++;
                        }
                    }
                }
            }
            check(outside > 0, "(control: the wave does light fires where nothing protects the ground)");
            check(inside == 0, "but none inside the arena: " + inside);
            return true;
        });
    }

    private static int countFires(GameTestHelper helper) {
        return countBlocks(helper, Blocks.FIRE);
    }

    private static int countBlocks(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        int count = 0;
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 21; x++) {
                for (int z = 0; z < 21; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(block)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The damage, directly (quick)

    @GameTest(template = ARENA, batch = PhoenixTests.PVP)
    public static void burstWearsWandasShieldDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 4.5, 10.5);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        wanda.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.WANDA_CROWN.get()));
        PowerTicker.tick(wanda);
        wanda.setHealth(wanda.getMaxHealth());
        PowerData data = Powers.get(wanda);
        float health = wanda.getHealth();
        check(health >= 40.0F, "Wanda in her crown has 40 health");
        DamageSource burst = ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, jean, jean);

        data.raiseChaosShield(level.getGameTime(), 0, 30.0F);
        check(WandaAbilities.hasShieldUp(wanda), "her Chaos Shield is up, with 30 points");
        check(!wanda.hurt(burst, 20.0F) && wanda.getHealth() == health, "a Burst of 20 hurts nothing but the shield");
        assertNear(data.shieldStrength(), 10.0, 1.0E-4, "which loses 20 points");
        check(data.chaosShieldActive(), "and stands");

        wanda.invulnerableTime = 0;
        check(wanda.hurt(burst, 15.0F), "a Burst of 15 against 10 points of shield");
        check(!data.chaosShieldActive() && data.shieldStrength() == 0.0F, "shatters it");
        assertNear(health - wanda.getHealth(), 5.0, 0.01, "and the 5 points left get through");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PhoenixTests.PVP)
    public static void burstDoesNotBypassAPhasedVisionButPassesArmourAndFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 4.5, 10.5);
        DamageSource burst = ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, jean, jean);

        ServerPlayer vision = f.player(10.5, FLOOR, 10.5, Power.VISION);
        Powers.get(vision).setVisionPhased(true);
        PowerTicker.tick(vision);
        float health = vision.getHealth();
        check(!vision.hurt(burst, 10.0F) && vision.getHealth() == health, "a phased Vision is intangible: the Burst's fire passes through him");

        Zombie armoured = PhoenixTests.zombie(helper, 14.5, 10.5);
        armoured.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        armoured.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        armoured.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        armoured.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        float before = armoured.getHealth();
        check(armoured.hurt(burst, 10.0F), "armour does not stop it");
        assertNear(before - armoured.getHealth(), 10.0, 0.01, "all of it gets through netherite");

        Blaze blaze = helper.spawnWithNoFreeWill(EntityType.BLAZE, new Vec3(14.5, FLOOR, 14.5));
        float blazeBefore = blaze.getHealth();
        check(!blaze.hurt(level.damageSources().inFire(), 6.0F), "(control: fire does nothing to a Blaze)");
        check(blaze.hurt(burst, 6.0F), "but the Burst is not fire");
        assertNear(blazeBefore - blaze.getHealth(), 6.0, 0.01, "a Blaze takes it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PhoenixTests.QUICK)
    public static void aMobKilledByTheBurstLeavesGoldenAsh(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 4.5, 10.5);
        Zombie doomed = PhoenixTests.zombie(helper, 10.5, 10.5);
        TestKit.sentPackets(jean);
        doomed.hurt(ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, jean, jean), 100.0F);
        check(!doomed.isAlive(), "it died");
        long ash = TestKit.particlesIn(TestKit.sentPackets(jean)).stream().filter(p -> p.getParticle() == net.minecraft.core.particles.ParticleTypes.ASH).count();
        check(ash > 0, "in a puff of ash");
        helper.succeed();
    }

    private PhoenixBurstTests() {
    }
}

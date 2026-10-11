package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.UltronAbilities;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Ultron, the hero (DESIGN section 16): his body and what it shrugs off, the stream of plasma and the pool behind it,
 * the Overcharge, flight, the Gravity Pull and the Gravity Slam. His drones and the network (Upload, the Hack, Second
 * Life, the Sentry Army, Command) are {@code UltronNetworkTests}. Tests that fire bolts keep them inside the arena: its
 * barrier walls spend them.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class UltronTests {
    /** Quick tests that keep no creature alive between ticks share one batch. */
    private static final String QUICK = "ultron_hero";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    private static ServerPlayer hero(TestKit.Fixture f, double x, double z) {
        ServerPlayer hero = f.player(x, FLOOR, z, Power.ULTRON);
        look(hero, EAST, 0.0F);
        return hero;
    }

    private static void key(ServerPlayer player, int ability, boolean down) {
        AbilityHandler.onKey(player, ability, down);
    }

    private static void tick(ServerPlayer player) {
        PowerTicker.tick(player);
    }

    /** A zombie that stands still, takes its damage plainly (no armour) and does not die of it. */
    private static Zombie dummy(GameTestHelper helper, double x, double z) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        zombie.setHealth(400.0F);
        // (A zombie burns in the daylight, a point now and then.)
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100000, 0, false, false));
        return zombie;
    }

    /** What the creature has lost of its 400 health. */
    private static double lost(LivingEntity creature) {
        return 400.0 - creature.getHealth();
    }

    private static List<BlockState> blocks(GameTestHelper helper) {
        List<BlockState> states = new ArrayList<>();
        AABB bounds = helper.getBounds();
        BlockPos.betweenClosedStream(bounds.contract(0.5, 0.5, 0.5)).forEach(pos -> states.add(helper.getLevel().getBlockState(pos)));
        return states;
    }

    private static UltronSentryEntity drone(TestKit.Fixture f, ServerPlayer hero, double x, double y, double z) {
        UltronSentryEntity drone = UltronSentryEntity.drone(f.level(), f.helper().absoluteVec(new Vec3(x, y, z)), hero.getUUID(), false, false);
        drone.setHeld(true);
        f.level().addFreshEntity(drone);
        return drone;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The body

    @GameTest(template = ARENA, batch = QUICK)
    public static void bodyAndPassives(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 10.5, 10.5);
        ServerPlayer plain = f.player(4.5, FLOOR, 4.5);
        tick(hero);
        tick(plain);
        assertNear(hero.getMaxHealth(), 30.0, 1.0E-4, "10 more health than a player's 20");
        assertNear(hero.getArmorValue(), 8.0, 1.0E-4, "8 armour");
        assertNear(hero.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.3, 1.0E-4, "0.3 knockback resistance");
        check(plain.getMaxHealth() == 20.0F && plain.getArmorValue() == 0, "and nobody else has any of it");

        // A machine is never hungry and never out of breath.
        hero.getFoodData().setFoodLevel(3);
        hero.setAirSupply(10);
        tick(hero);
        check(hero.getFoodData().getFoodLevel() == 20, "the food bar stays full");
        check(hero.getAirSupply() == hero.getMaxAirSupply(), "so does the air");

        // No Poison, no Wither; a player takes both.
        check(!hero.addEffect(new MobEffectInstance(MobEffects.POISON, 200)) && !hero.hasEffect(MobEffects.POISON), "no Poison");
        check(!hero.addEffect(new MobEffectInstance(MobEffects.WITHER, 200)) && !hero.hasEffect(MobEffects.WITHER), "no Wither");
        check(plain.addEffect(new MobEffectInstance(MobEffects.POISON, 200)), "a player takes Poison");
        check(hero.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200)), "but the rest of the effects land");

        // The palms catch him: no fall damage; the void still kills.
        hero.setHealth(30.0F);
        check(!hero.hurt(level.damageSources().fall(), 12.0F) && hero.getHealth() == 30.0F, "a fall of 12 hurts him not at all");
        plain.setHealth(20.0F);
        check(plain.hurt(level.damageSources().fall(), 4.0F) && plain.getHealth() < 20.0F, "but it hurts a player");
        hero.invulnerableTime = 0;
        check(hero.hurt(level.damageSources().fellOutOfWorld(), 4.0F) && hero.getHealth() < 30.0F, "the void is not a fall");

        // Electronics: lightning does 1.25 times (before the armour).
        hero.setHealth(30.0F);
        hero.invulnerableTime = 0;
        DamageSource lightning = level.damageSources().lightningBolt();
        float expected = CombatRules.getDamageAfterAbsorb(hero, 5.0F, lightning, hero.getArmorValue(), (float) hero.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        hero.hurt(lightning, 4.0F);
        assertNear(30.0 - hero.getHealth(), expected, 0.01, "lightning does 4 x 1.25 = 5, less his armour");

        // And all of it goes with the power.
        Powers.setPower(hero, Power.NONE);
        tick(hero);
        check(hero.getMaxHealth() == 20.0F && hero.getArmorValue() == 0 && hero.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.0,
                "the body is gone with the power");
        check(hero.addEffect(new MobEffectInstance(MobEffects.POISON, 200)), "and so is his immunity");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 1: Repulsor Fire

    @GameTest(template = ARENA, batch = "ultron_fire", timeoutTicks = 200)
    public static void repulsorFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 3.5, 10.5);
        PowerData data = Powers.get(hero);
        Zombie target = dummy(helper, 9.5, 10.5);
        Zombie beside = dummy(helper, 10.3, 11.0);
        Zombie far = dummy(helper, 9.5, 14.5);
        ServerPlayer creative = f.player(10.3, FLOOR, 10.0, GameType.CREATIVE);
        ServerPlayer bystander = f.player(10.3, FLOOR, 9.4);
        UltronSentryEntity drone = drone(f, hero, 10.6, FLOOR, 10.2);
        float bystanderHealth = bystander.getHealth();
        List<BlockState> before = blocks(helper);
        tick(hero);
        check(data.ironCharge() == 30, "the pool starts full: " + data.ironCharge());

        key(hero, 1, true);
        check(data.beamActive(), "R down: he is firing");
        Set<Integer> seen = new HashSet<>();
        List<Double> sides = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        f.everyTick(70, t -> {
            if (t <= 40) {
                tick(hero);
                for (UltronBoltEntity bolt : level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds())) {
                    if (bolt.getOwner() == hero && seen.add(bolt.getId())) {
                        sides.add(bolt.getZ() - helper.absoluteVec(new Vec3(0, 0, 10.5)).z);
                        times.add(level.getGameTime());
                        check(bolt.damage() == 4.0F && bolt.palette() == UltronBoltEntity.Palette.PRIME, "a bolt of 4, in Prime's amber");
                    }
                }
            }
            if (t == 40) {
                key(hero, 1, false);
                check(!data.beamActive(), "R up: he stops");
                check(seen.size() == 10, "five a second: ten bolts in forty ticks, not " + seen.size());
                for (int i = 1; i < times.size(); i++) {
                    check(times.get(i) - times.get(i - 1) == 4, "four ticks apart");
                }
                for (int i = 0; i < sides.size(); i++) {
                    assertNear(sides.get(i), i % 2 == 0 ? 0.35 : -0.35, 0.08, "bolt " + i + " leaves the " + (i % 2 == 0 ? "right" : "left") + " hand");
                }
                check(data.ironCharge() == 20, "a charge a bolt: " + data.ironCharge());
            }
            if (t == 44) {
                // Every bolt landed: no hit cooldown. The first creature takes 4, the one beside it half of that.
                assertNear(lost(target), 40.0, 0.01, "ten bolts of 4 on the first creature in the line");
                assertNear(lost(beside), 20.0, 0.01, "and 2 of each on the one within 1.2 blocks");
                assertNear(lost(far), 0.0, 0.01, "none on the one 4 blocks away");
                check(creative.getHealth() == 20.0F && bystander.getHealth() == bystanderHealth, "never a Creative player, nor a player with PvP off");
                check(drone.getHealth() == drone.getMaxHealth(), "nor his drones");
            }
            if (t == 46) {
                check(data.ironCharge() == 20, "the pool does not refill for half a second after the last bolt");
            }
            if (t == 70) {
                int pool = data.ironCharge();
                check(pool >= 23 && pool <= 25, "and then refills four a second: " + pool);
                check(blocks(helper).equals(before), "no block changed");
                check(level.getEntitiesOfClass(Projectile.class, helper.getBounds()).isEmpty(), "and every bolt is spent");
                return true;
            }
            if (t > 40) {
                tick(hero);
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "ultron_pool", timeoutTicks = 200)
    public static void thePoolRunsDryAndNeedsAFewChargesToRestart(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 3.5, 10.5);
        PowerData data = Powers.get(hero);
        tick(hero);
        data.setIronCharge(2);
        key(hero, 1, true);
        Set<Integer> seen = new HashSet<>();
        List<Integer> shotAt = new ArrayList<>();
        f.everyTick(60, t -> {
            tick(hero);
            for (UltronBoltEntity bolt : level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds())) {
                if (bolt.getOwner() == hero && seen.add(bolt.getId())) {
                    shotAt.add(t);
                }
            }
            if (t == 8) {
                check(shotAt.equals(List.of(1, 5)) && data.ironCharge() == 0, "two charges, two bolts, and he is dry: " + shotAt + " " + data.ironCharge());
                check(!data.beamActive(), "the stream stops with the plasma");
                check("message.enhanced.ultron.pool_empty".equals(BossTestSupport.lastActionBarKey(hero)), "he is told");
            }
            if (t == 40) {
                check(shotAt.size() == 2, "empty, R held: nothing fires while the pool refills");
                check(data.ironCharge() >= 4 && data.ironCharge() < 6, "it refills four a second after the idle half second: " + data.ironCharge());
            }
            if (t == 58) {
                check(shotAt.size() >= 3 && shotAt.get(2) >= 40, "with six charges R works again (and, held, fires): " + shotAt);
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void creativeHasAnEndlessPoolAndHisLookColoursTheBolt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer creative = f.player(3.5, FLOOR, 10.5, GameType.CREATIVE);
        Powers.setPower(creative, Power.ULTRON);
        look(creative, EAST, 0.0F);
        PowerData data = Powers.get(creative);
        tick(creative);
        key(creative, 1, true);
        for (int i = 0; i < 30; i++) {
            level.getGameTime();
            tick(creative);
        }
        check(data.ironCharge() == 30, "Creative's pool is never spent: " + data.ironCharge());
        key(creative, 1, false);
        for (UltronBoltEntity bolt : level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds())) {
            bolt.discard();
        }

        // The look chooses the plasma.
        ServerPlayer hero = hero(f, 3.5, 5.5);
        for (String look : List.of("sentry", "human", "prime")) {
            Powers.setVariant(hero, Variants.get(Power.ULTRON, look));
            check(UltronAbilities.palette(hero) == switch (look) {
                case "sentry" -> UltronBoltEntity.Palette.SENTRY;
                case "human" -> UltronBoltEntity.Palette.HUMAN;
                default -> UltronBoltEntity.Palette.PRIME;
            }, "the " + look + " look has its own plasma");
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sneak + R: the Overcharge

    @GameTest(template = ARENA, batch = "ultron_overcharge", timeoutTicks = 200)
    public static void overcharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 3.5, 10.5);
        PowerData data = Powers.get(hero);
        Zombie target = dummy(helper, 9.5, 10.5);
        Zombie beside = dummy(helper, 11.5, 10.5);
        Zombie outside = dummy(helper, 9.5, 14.5);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
        tick(hero);

        // With too little plasma it does not start.
        data.setIronCharge(9);
        hero.setShiftKeyDown(true);
        key(hero, 1, true);
        check(!data.is(Live.ULTRON_CHARGING) && data.ironCharge() == 9, "nine charges are not enough for ten");
        data.setIronCharge(30);
        double walking = hero.getAttributeValue(Attributes.MOVEMENT_SPEED);

        key(hero, 1, true);
        long start = level.getGameTime();
        check(data.is(Live.ULTRON_CHARGING) && data.ironCharge() == 20, "Sneak + R: charging, ten charges gone: " + data.ironCharge());
        hero.setShiftKeyDown(false);
        f.everyTick(60, t -> {
            tick(hero);
            if (t == 3) {
                assertNear(hero.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 0.4, 1.0E-4, "slowed to 40 percent");
                key(hero, 1, true);
                check(!data.beamActive(), "and he cannot fire R while he builds it");
                hero.invulnerableTime = 0;
                hero.hurt(level.damageSources().generic(), 1.0F);
                check(data.is(Live.ULTRON_CHARGING), "a blow does not break it");
                key(hero, 1, false);
            }
            if (t == 14) {
                check(data.is(Live.ULTRON_CHARGING) && level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds()).isEmpty(), "fifteen ticks: not yet");
            }
            if (t == 15) {
                check(!data.is(Live.ULTRON_CHARGING), "the charge is over");
                List<UltronBoltEntity> blasts = level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds());
                check(blasts.size() == 1 && blasts.get(0).isHeavy() && blasts.get(0).damage() == 14.0F, "one heavy blast of 14");
                assertNear(data.cooldownUntil(1) - level.getGameTime(), 160.0, 1.0, "and the cooldown starts: eight seconds");
                key(hero, 1, true);
                hero.setShiftKeyDown(true);
                key(hero, 1, true);
                check(!data.is(Live.ULTRON_CHARGING) && data.ironCharge() == 20, "and cannot be charged again until it is over");
                hero.setShiftKeyDown(false);
                key(hero, 1, false);
            }
            if (t == 25) {
                assertNear(lost(target), 14.0, 0.01, "14 on what it hits");
                assertNear(lost(beside), 8.0, 0.01, "8 on what stands within 3.5 blocks");
                assertNear(lost(outside), 0.0, 0.01, "none on what is further");
                check(target.getX() > helper.absoluteVec(new Vec3(9.5, 0, 0)).x + 0.3, "and a push: the blow carries it back");
                check(hero.getAttributeValue(Attributes.MOVEMENT_SPEED) > walking * 0.99, "he walks at his speed again");
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 2: Anti-Gravity Flight

    @GameTest(template = ARENA, batch = QUICK)
    public static void flight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = hero(f, 10.5, 10.5);
        tick(hero);
        check(Power.ULTRON.canFly() && Power.ULTRON.flightAbility() == 2, "G flies");
        check(hero.mayFly(), "he may fly");
        assertNear(hero.getAbilities().getFlyingSpeed(), 0.10, 1.0E-4, "at 0.10");
        key(hero, 2, true);
        check(hero.getAbilities().flying, "G takes off");
        key(hero, 2, true);
        check(!hero.getAbilities().flying, "and G lands");
        f.config(UltronConfig.FLIGHT_SPEED, 0.2);
        tick(hero);
        assertNear(hero.getAbilities().getFlyingSpeed(), 0.2, 1.0E-4, "the speed is the config's");

        // Flying is no hunger, and the landing is soft.
        key(hero, 2, true);
        hero.getFoodData().setFoodLevel(2);
        tick(hero);
        check(hero.getFoodData().getFoodLevel() == 20, "no hunger");
        hero.fallDistance = 40.0F;
        float health = hero.getHealth();
        hero.causeFallDamage(40.0F, 1.0F, f.level().damageSources().fall());
        check(hero.getHealth() == health, "no fall damage");

        // A spellbound hero can't fly (nor use any key).
        hero.getAbilities().flying = false;
        hero.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100, 0));
        key(hero, 2, true);
        check(!hero.getAbilities().flying, "Billy's Freeze holds him on the ground");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 3: Gravity Pull

    @GameTest(template = ARENA, batch = "ultron_pull", timeoutTicks = 200)
    public static void gravityPull(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 3.5, 10.5);
        PowerData data = Powers.get(hero);
        Zombie zombie = dummy(helper, 14.5, 10.5);
        tick(hero);
        look(hero, EAST, 0.0F);
        lookAt(hero, zombie);

        key(hero, 3, true);
        long start = level.getGameTime();
        assertNear(data.cooldownUntil(3) - start, 80.0, 0.5, "the cooldown is four seconds");
        Zombie second = dummy(helper, 9.5, 10.5);
        f.everyTick(60, t -> {
            tick(hero);
            if (t == 1) {
                check(zombie.getDeltaMovement().x < -0.5, "it is yanked toward him at once: " + zombie.getDeltaMovement());
                key(hero, 3, true);
                check(second.getDeltaMovement().x == 0.0, "a second pull during the cooldown does nothing");
            }
            if (t == 30) {
                Vec3 in = hero.position().add(2.0, 0.0, 0.0);
                check(zombie.position().distanceTo(in) < 1.3, "it stands two blocks in front of him: " + zombie.position() + " vs " + in);
                assertNear(lost(zombie), 3.0, 0.01, "the weight of it: 3 damage");
                MobEffectInstance slow = zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                check(slow != null && slow.getAmplifier() == 1, "and Slowness II");
                check(zombie.getDeltaMovement().length() < 0.2, "it is at rest");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "ultron_pull_targets", timeoutTicks = 200)
    public static void gravityPullTargets(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 3.5, 10.5);
        PowerData data = Powers.get(hero);
        tick(hero);
        long now = level.getGameTime();

        // A Creative player in the way is looked through; a player with PvP off is no target.
        ServerPlayer creative = f.player(8.5, FLOOR, 10.5, GameType.CREATIVE);
        ServerPlayer player = f.player(11.5, FLOOR, 10.5);
        lookAt(hero, player);
        key(hero, 3, true);
        check(data.isReady(3, now) && player.getDeltaMovement().x == 0.0, "no player is pulled with PvP off");
        f.pvp(true);
        key(hero, 3, true);
        check(!data.isReady(3, now), "with PvP on a player is a target");
        creative.setPos(creative.position().add(0, 0, 5.0));
        data.startCooldown(3, now, 0);

        // One of his own drones is not.
        UltronSentryEntity drone = drone(f, hero, 6.5, FLOOR + 1.0, 10.5);
        player.setPos(player.position().add(0, 0, 6.0));
        look(hero, EAST, 0.0F);
        lookAt(hero, drone);
        key(hero, 3, true);
        check(data.isReady(3, level.getGameTime()) && drone.getDeltaMovement().x == 0.0, "never one of his drones");

        // An arrow in flight is caught and falls; a dropped item comes to his feet.
        Arrow arrow = EntityType.ARROW.create(level);
        Vec3 at = helper.absoluteVec(new Vec3(12.5, FLOOR + 1.0, 10.5));
        arrow.setPos(at);
        arrow.setDeltaMovement(0.6, 0.0, 0.0);
        level.addFreshEntity(arrow);
        ItemEntity item = helper.spawnItem(Items.DIAMOND, new Vec3(14.5, FLOOR, 8.5));
        lookAt(hero, arrow);
        key(hero, 3, true);
        check(!data.isReady(3, level.getGameTime()), "an arrow in flight is a target");
        f.everyTick(40, t -> {
            tick(hero);
            if (t == 12) {
                check(arrow.getDeltaMovement().x < 0.3 && arrow.getX() < hero.getX() + 3.5, "the arrow is caught: " + arrow.position() + " " + arrow.getDeltaMovement());
                data.startCooldown(3, level.getGameTime(), 0);
                lookAt(hero, item);
                key(hero, 3, true);
                check(!data.isReady(3, level.getGameTime()), "an item is a target");
            }
            if (t == 36) {
                check(item.position().distanceTo(hero.position()) < 2.0, "and it lies at his feet: " + item.position());
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sneak + V: Gravity Slam

    @GameTest(template = ARENA, batch = "ultron_slam", timeoutTicks = 200)
    public static void gravitySlam(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 10.5, 10.5);
        PowerData data = Powers.get(hero);
        f.pvp(false);
        Zombie close = dummy(helper, 13.5, 10.5);
        Zombie edge = dummy(helper, 10.5, 16.5);
        Zombie outside = dummy(helper, 10.5, 18.5);
        Zombie flier = dummy(helper, 7.5, 10.5);
        flier.setNoGravity(true);
        flier.setPos(flier.getX(), flier.getY() + 3.0, flier.getZ());
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(10.5, FLOOR, 7.5));
        pig.setHealth(pig.getMaxHealth());
        ServerPlayer creative = f.player(10.5, FLOOR, 8.5, GameType.CREATIVE);
        ServerPlayer player = f.player(8.5, FLOOR, 12.5);
        UltronSentryEntity drone = drone(f, hero, 12.5, FLOOR + 1.0, 12.5);
        float playerHealth = player.getHealth();
        tick(hero);

        helper.runAfterDelay(3, () -> {
            hero.setShiftKeyDown(true);
            key(hero, 3, true);
            long now = level.getGameTime();
            assertNear(data.cooldownUntil(3) - now, 160.0, 0.5, "eight seconds");
            assertNear(lost(close), 6.0, 0.01, "6 to what is near");
            assertNear(lost(edge), 6.0, 0.01, "and 6 six blocks away");
            assertNear(lost(outside), 0.0, 0.01, "none eight blocks away");
            assertNear(lost(flier), 6.0, 0.01, "6 to what flies");
            check(close.getDeltaMovement().x > 1.0 && close.getDeltaMovement().y == 0.0, "thrown outward along the ground: " + close.getDeltaMovement());
            check(edge.getDeltaMovement().z > 1.0, "outward from where he stands, whichever way");
            assertNear(flier.getDeltaMovement().y, -1.2, 0.01, "what flies is slammed into the ground");
            MobEffectInstance slow = close.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            check(slow != null && slow.getAmplifier() == 0 && slow.getDuration() > 50, "Slowness I for three seconds");
            check(pig.getHealth() == pig.getMaxHealth(), "a pig is none of his business (the area rule)");
            check(creative.getHealth() == 20.0F && player.getHealth() == playerHealth, "not a Creative player, nor one with PvP off");
            check(drone.getHealth() == drone.getMaxHealth() && drone.getDeltaMovement().length() == 0.0, "and not his drones");
            hero.setShiftKeyDown(false);

            // Ready again: the same blow lands on a player when PvP allows it, and then the cooldown is the point.
            key(hero, 3, true);
            check(data.cooldownUntil(3) == now + 160, "a second Slam in the cooldown does nothing");
            f.pvp(true);
            data.startCooldown(3, now, 0);
            hero.setShiftKeyDown(true);
            key(hero, 3, true);
            check(player.getHealth() < playerHealth, "with PvP on it hurts a player: " + player.getHealth());
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Scepter

    @GameTest(template = ARENA, batch = QUICK)
    public static void theStoneKnowsItsCreation(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = hero(f, 10.5, 10.5);
        hero.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        float health = hero.getHealth();
        ScepterRituals.useScepter(hero, InteractionHand.MAIN_HAND, false);
        check(hero.isAlive() && hero.getHealth() == health && Powers.is(hero, Power.ULTRON), "the Mind Stone knows Ultron: no explosion");
        check("message.enhanced.mind_stone.recognizes".equals(BossTestSupport.lastActionBarKey(hero)), "it says so");
        helper.succeed();
    }
}

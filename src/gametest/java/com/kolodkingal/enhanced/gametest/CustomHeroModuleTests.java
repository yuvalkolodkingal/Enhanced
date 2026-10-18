package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.custom.CustomEvents;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.modules.MindPowers;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The thirty power modules of a custom hero on the server (3.3, F2, F5), each with the behaviour of its key and its
 * passives, and the Hero Creator's two buttons (who may use them, the config, choosing one of the mod's heroes). Every
 * test makes a hero of one power on key R, strength 10 unless it says otherwise, against creatures with no AI (so nothing
 * moves by itself).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CustomHeroModuleTests {
    private static final String BATCH = "custom_modules";

    /** A Survival player, a custom hero with {@code module} in {@code option} at {@code strength} on key R. */
    static ServerPlayer hero(TestKit.Fixture f, double x, double z, PowerModule module, String option, int strength) {
        ServerPlayer player = f.player(x, FLOOR, z);
        become(player, module, option, strength);
        return player;
    }

    static void become(ServerPlayer player, PowerModule module, String option, int strength) {
        CustomHero hero = CustomHero.EMPTY.withName("Tester").withColors(0x3FA9F5, 0xE0F4FF)
                .withPowers(List.of(new CustomHero.Pick(module.id, strength, option, -1))).withKeys(List.of(module.id, "", "", "", ""));
        CustomHeroRules.Result result = CustomHeroRules.sanitize(hero, CustomHeroRules.Limits.DEFAULT);
        CustomHeroes.become(player, result.hero());
    }

    static void press(ServerPlayer player) {
        AbilityHandler.onKey(player, 1, true);
        AbilityHandler.onKey(player, 1, false);
    }

    static Zombie zombie(GameTestHelper helper, double x, double z) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        return zombie;
    }

    static void tick(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            player.tickCount++;
            PowerTicker.tick(player);
        }
    }

    /** The player aims at the creature. */
    static void aim(ServerPlayer player, Mob mob) {
        TestKit.lookAt(player, mob);
    }

    // ---- the hero itself ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void becomingAHeroSavesTheDefinitionAndSyncsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.FLIGHT, "jet", 5);
        check(Powers.power(player) == Power.CUSTOM, "he is a custom hero");
        CustomHero hero = player.getData(ModAttachments.CUSTOM_HERO);
        check(hero.name().equals("Tester") && hero.has(PowerModule.FLIGHT), "his definition is on him");
        check(Powers.color(player) == 0x3FA9F5 && Powers.glow(player) == 0xE0F4FF, "his colours are his look's");
        check(player.mayFly(), "Flight lets him fly");
        assertNear(player.getAbilities().getFlyingSpeed(), ModuleStats.FLIGHT_SPEED.value(5), 1.0E-4, "at the speed of its strength");
        press(player);
        check(player.getAbilities().flying, "R takes off");
        Powers.setPower(player, Power.NONE);
        check(!player.mayFly() && player.getData(ModAttachments.CUSTOM_HERO).has(PowerModule.FLIGHT), "losing the power keeps the definition");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aFreeKeyOrASuppressedHeroDoesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.SUPER_SPEED, "run", 5);
        AbilityHandler.onKey(player, 3, true);
        check(!CustomHeroes.state(player).anyOn(), "a free key does nothing");
        CustomHeroes.suppress(player, 100);
        press(player);
        check(!CustomHeroes.state(player).isOn(PowerModule.SUPER_SPEED), "Power Absorption took his powers: R does nothing");
        helper.succeed();
    }

    // ---- the body ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void superStrengthPunchesSlamsAndLeaps(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.SUPER_STRENGTH, "punch", 10);
        check(player.getAttributeValue(Attributes.ATTACK_DAMAGE) >= 1.0 + ModuleStats.STRENGTH_MELEE.value(10) - 1.0E-3, "harder blows");
        Zombie z = zombie(helper, 7.5, 10.5);
        aim(player, z);
        float before = z.getHealth();
        press(player);
        check(z.getHealth() < before && z.getDeltaMovement().horizontalDistance() > 0.1, "the punch hurts and throws");
        check(!Powers.get(player).isReady(1, helper.getLevel().getGameTime()), "and its cooldown runs");
        become(player, PowerModule.SUPER_STRENGTH, "slam", 10);
        Zombie a = zombie(helper, 3.5, 10.5);
        Zombie b = zombie(helper, 5.5, 13.5);
        press(player);
        check(a.getHealth() < a.getMaxHealth() && b.getHealth() < b.getMaxHealth(), "the slam hits all round");
        become(player, PowerModule.SUPER_STRENGTH, "leap", 10);
        TestKit.look(player, 0, -20);
        press(player);
        check(player.getDeltaMovement().y > 0.5 && CustomHeroes.fallGuarded(player), "the leap throws him up, and he will land softly");
        Powers.setPower(player, Power.NONE);
        assertNear(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 1.0, 1.0E-3, "the strength goes with the power");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void durabilityArmoursOrAddsHealth(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.DURABILITY, "armor", 10);
        assertNear(player.getAttributeValue(Attributes.ARMOR), ModuleStats.ARMOR.value(10), 1.0E-3, "armour of its strength");
        check(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) > 0.5, "hard to push");
        become(player, PowerModule.DURABILITY, "health", 1);
        assertNear(player.getMaxHealth(), 20 + ModuleStats.EXTRA_HEALTH.value(1), 1.0E-3, "more hearts at strength 1");
        assertNear(player.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-3, "and no armour");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theHealingFactorHealsSteadilyOrAtOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.HEALING, "steady", 10);
        player.setHealth(6);
        tick(player, 40);
        check(player.getHealth() >= 6 + 2 * ModuleStats.REGEN.value(10) - 0.01, "two seconds heal twice: " + player.getHealth());
        become(player, PowerModule.HEALING, "burst", 10);
        player.setHealth(4);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 200));
        press(player);
        check(player.getHealth() >= Math.min(player.getMaxHealth(), 4 + ModuleStats.BURST_HEAL.value(10)) - 0.01
                && !player.hasEffect(MobEffects.POISON), "a burst heals and cures: " + player.getHealth());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void shapeshiftingGrowsShrinksAndTakesACreaturesShape(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.SHAPESHIFT, "grow", 4);
        press(player);
        assertNear(player.getScale(), ModuleStats.GROW_SCALE.value(4), 1.0E-3, "a giant");
        press(player);
        assertNear(player.getScale(), 1.0, 1.0E-3, "and back");
        become(player, PowerModule.SHAPESHIFT, "shrink", 10);
        press(player);
        assertNear(player.getScale(), ModuleStats.SHRINK_SCALE.value(10), 1.0E-3, "tiny");
        become(player, PowerModule.SHAPESHIFT, "creature", 10);
        assertNear(player.getScale(), 1.0, 1.0E-3, "a new definition ends the old shape");
        Zombie z = zombie(helper, 13.5, 10.5);
        aim(player, z);
        press(player);
        check(Powers.get(player).form().equals("minecraft:zombie"), "the zombie's shape: " + Powers.get(player).form());
        press(player);
        check(Powers.get(player).form().isEmpty(), "and back to himself");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void invisibilityHidesHimFromEyesAndMonsters(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.INVISIBILITY, "full", 10);
        Zombie z = zombie(helper, 13.5, 10.5);
        z.setTarget(player);
        press(player);
        check(player.hasEffect(MobEffects.INVISIBILITY) && CustomHeroes.isOn(player, PowerModule.INVISIBILITY), "invisible");
        check(z.getTarget() == null, "the monster lost him");
        z.setTarget(player);
        check(z.getTarget() == null, "and cannot pick him again");
        press(player);
        check(!player.hasEffect(MobEffects.INVISIBILITY), "seen again");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void phasingPassesThroughWallsOrLetsBlowsThrough(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.PHASING, "phase", 10);
        press(player);
        check(VisionAbilities.isPhased(player) && player.mayFly() && player.getAbilities().flying, "Vision's phasing: he floats through walls");
        check(!player.hurt(helper.getLevel().damageSources().generic(), 5) || player.getHealth() == player.getMaxHealth(), "nothing hurts him");
        press(player);
        check(!VisionAbilities.isPhased(player), "solid again");
        become(player, PowerModule.PHASING, "ghost", 10);
        press(player);
        check(!VisionAbilities.isPhased(player), "a ghost does not pass walls");
        check(CustomEvents.spares(player, helper.getLevel().damageSources().generic()), "but every blow passes through him");
        helper.succeed();
    }

    // ---- moving ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void superSpeedRunsAndBlitzes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 3.5, 10.5, PowerModule.SUPER_SPEED, "run", 10);
        double base = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        press(player);
        tick(player, 1);
        assertNear(player.getAttributeValue(Attributes.MOVEMENT_SPEED), base * (1 + ModuleStats.RUN_SPEED.value(10)), 1.0E-4, "faster");
        press(player);
        tick(player, 1);
        assertNear(player.getAttributeValue(Attributes.MOVEMENT_SPEED), base, 1.0E-4, "and back");
        become(player, PowerModule.SUPER_SPEED, "blitz", 3);
        Zombie z = zombie(helper, 7.5, 10.5);
        TestKit.look(player, TestKit.EAST, 0);
        Vec3 from = player.position();
        press(player);
        check(player.position().x - from.x > 4.0, "he dashed east: " + player.position().subtract(from));
        check(z.getHealth() < z.getMaxHealth(), "striking the zombie on the way");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void teleportationBlinksAndAppearsBehind(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 3.5, 10.5, PowerModule.TELEPORT, "blink", 2);
        TestKit.lookAt(player, helper.absoluteVec(new Vec3(9.5, FLOOR - 0.5, 10.5)));
        press(player);
        check(player.position().distanceTo(helper.absoluteVec(new Vec3(9.5, FLOOR, 10.5))) < 1.5, "he stands where he looked: " + player.position());
        become(player, PowerModule.TELEPORT, "behind", 10);
        Zombie z = zombie(helper, 14.5, 10.5);
        z.setYBodyRot(90); // facing west, towards him
        aim(player, z);
        press(player);
        check(player.position().x > z.getX() && player.distanceTo(z) < 2.5, "behind the zombie: " + player.position());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void wallCrawlingStartsOnAndForgivesTheFall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        helper.setBlock(new BlockPos(11, 2, 10), Blocks.STONE);
        helper.setBlock(new BlockPos(11, 3, 10), Blocks.STONE);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.WALL_CRAWL, "steady", 5);
        check(CustomHeroes.isOn(player, PowerModule.WALL_CRAWL), "on from the start");
        player.moveTo(helper.absoluteVec(new Vec3(10.65, FLOOR, 10.5)));
        check(CustomEvents.spares(player, helper.getLevel().damageSources().fall()), "a fall by a wall does not hurt");
        press(player);
        check(!CustomHeroes.isOn(player, PowerModule.WALL_CRAWL), "R switches it off");
        helper.succeed();
    }

    // ---- energy ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void energyBlastBoltsBurstsAndBeams(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 3.5, 10.5, PowerModule.ENERGY_BLAST, "bolts", 10);
        Zombie z = zombie(helper, 12.5, 10.5);
        aim(player, z);
        press(player);
        check(z.getHealth() < z.getMaxHealth(), "a bolt hits");
        become(player, PowerModule.ENERGY_BLAST, "burst", 10);
        Zombie near = zombie(helper, 5.5, 10.5);
        press(player);
        check(near.getHealth() < near.getMaxHealth(), "a burst hits what is near");
        become(player, PowerModule.ENERGY_BLAST, "beam", 10);
        AbilityHandler.onKey(player, 1, true);
        check(ContinuousBeam.isFiring(player) && Powers.get(player).is(Live.BEAMING), "holding R fires the shared beam");
        AbilityHandler.onKey(player, 1, false);
        check(!ContinuousBeam.isFiring(player) && !Powers.get(player).is(Live.BEAMING), "letting go stops it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theForceFieldTakesHitsAndBreaks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.FORCE_FIELD, "bubble", 1);
        Zombie z = zombie(helper, 12.5, 10.5);
        AbilityHandler.onKey(player, 1, true);
        CustomState state = CustomHeroes.state(player);
        check(state.field(), "holding R raises it");
        float strength = state.fieldStrength();
        assertNear(strength, ModuleStats.FIELD_STRENGTH.value(1), 1.0E-3, "as strong as its strength");
        player.hurt(helper.getLevel().damageSources().mobAttack(z), 4);
        check(player.getHealth() == player.getMaxHealth() && state.fieldStrength() < strength, "a blow wears the field, not him");
        player.invulnerableTime = 0;
        player.hurt(helper.getLevel().damageSources().mobAttack(z), 50);
        check(!state.field() && !Powers.get(player).isReady(1, helper.getLevel().getGameTime()), "too much breaks it, and it recharges");
        AbilityHandler.onKey(player, 1, false);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void magicBoltsWardsAndCurses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 3.5, 10.5, PowerModule.MAGIC, "bolts", 10);
        Zombie z = zombie(helper, 12.5, 10.5);
        aim(player, z);
        press(player);
        check(z.getHealth() < z.getMaxHealth(), "a mystic bolt hits");
        become(player, PowerModule.MAGIC, "curse", 10);
        aim(player, z);
        press(player);
        check(z.hasEffect(MobEffects.WEAKNESS) && z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "cursed");
        become(player, PowerModule.MAGIC, "ward", 10);
        press(player);
        tick(player, 10);
        check(player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the ward heals and guards him");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void absorptionStoresEnergyAndLetsItOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.ABSORPTION, "energy", 2);
        float cap = ModuleStats.ABSORB_CAPACITY.value(2);
        player.hurt(helper.getLevel().damageSources().magic(), 5);
        check(player.getHealth() == player.getMaxHealth(), "magic is soaked up");
        assertNear(CustomHeroes.state(player).charge(), 5, 1.0E-3, "and stored");
        player.invulnerableTime = 0;
        player.hurt(helper.getLevel().damageSources().magic(), 12);
        assertNear(CustomHeroes.state(player).charge(), cap, 1.0E-3, "up to its capacity");
        check(player.getHealth() < player.getMaxHealth(), "the rest hurts");
        Zombie z = zombie(helper, 12.5, 10.5);
        press(player);
        check(z.getHealth() < z.getMaxHealth() && CustomHeroes.state(player).charge() == 0, "released all round him");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "custom_modules_pvp")
    public static void powerAbsorptionTakesAHerosPowers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer player = hero(f, 8.5, 10.5, PowerModule.ABSORPTION, "power", 10);
        ServerPlayer thor = f.player(11.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        TestKit.lookAt(player, thor);
        press(player);
        check(thor.getHealth() < thor.getMaxHealth() && CustomHeroes.isSuppressed(thor), "the hero is drained and his powers taken");
        AbilityHandler.onKey(thor, 2, true);
        check(!thor.getAbilities().flying, "his keys do nothing for now");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSonicScreamConeAndRing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.SONIC_SCREAM, "cone", 10);
        Zombie front = zombie(helper, 9.5, 10.5);
        Zombie back = zombie(helper, 2.5, 10.5);
        TestKit.look(player, TestKit.EAST, 0);
        press(player);
        check(front.getHealth() < front.getMaxHealth() && back.getHealth() == back.getMaxHealth(), "the cone hits only in front");
        check(front.hasEffect(MobEffects.CONFUSION), "and dazes");
        become(player, PowerModule.SONIC_SCREAM, "ring", 10);
        press(player);
        check(back.getHealth() < back.getMaxHealth(), "the ring hits behind too");
        helper.succeed();
    }

    // ---- the elements ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void fireBurnsOthersAndNeverHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 3.5, 10.5, PowerModule.FIRE, "fireball", 10);
        Zombie z = zombie(helper, 10.5, 10.5);
        aim(player, z);
        press(player);
        check(z.getHealth() < z.getMaxHealth() && z.getRemainingFireTicks() > 0, "a fireball sets it alight");
        check(CustomEvents.spares(player, helper.getLevel().damageSources().inFire()), "fire never burns him");
        become(player, PowerModule.FIRE, "aura", 10);
        Zombie near = zombie(helper, 4.5, 10.5);
        press(player);
        tick(player, 10);
        check(near.getRemainingFireTicks() > 0, "the aura burns what is near");
        become(player, PowerModule.FIRE, "flamethrower", 10);
        AbilityHandler.onKey(player, 1, true);
        check(CustomHeroes.isOn(player, PowerModule.FIRE), "the flamethrower burns while R is held");
        AbilityHandler.onKey(player, 1, false);
        check(!CustomHeroes.isOn(player, PowerModule.FIRE), "and stops");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void iceFreezesAndMakesAPath(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 3.5, 10.5, PowerModule.ICE, "freeze_ray", 10);
        Zombie z = zombie(helper, 10.5, 10.5);
        aim(player, z);
        press(player);
        check(z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && z.getTicksFrozen() > 0 && z.getHealth() < z.getMaxHealth(), "frozen");
        check(CustomEvents.spares(player, helper.getLevel().damageSources().freeze()), "the cold never hurts him");
        become(player, PowerModule.ICE, "ice_path", 10);
        BlockPos water = new BlockPos(16, 1, 10);
        helper.setBlock(water, Blocks.WATER);
        player.moveTo(helper.absoluteVec(new Vec3(15.5, FLOOR, 10.5)));
        player.setOnGround(true);
        press(player);
        tick(player, 2);
        helper.assertBlockPresent(Blocks.FROSTED_ICE, water);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void lightningStrikesAndChains(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 3.5, 10.5, PowerModule.LIGHTNING, "chain", 10);
        Zombie a = zombie(helper, 9.5, 10.5);
        Zombie b = zombie(helper, 12.5, 10.5);
        aim(player, a);
        press(player);
        check(a.getHealth() < a.getMaxHealth() && b.getHealth() < b.getMaxHealth(), "the lightning jumps to the next");
        check(CustomEvents.spares(player, helper.getLevel().damageSources().lightningBolt()), "lightning never hurts him");
        become(player, PowerModule.LIGHTNING, "strike", 10);
        Zombie c = zombie(helper, 6.5, 14.5);
        aim(player, c);
        press(player);
        check(c.getHealth() < c.getMaxHealth(), "a strike where he looks");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void waterPushesWavesAndBreathes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.WATER, "wave", 10);
        Zombie z = zombie(helper, 7.5, 10.5);
        z.igniteForSeconds(5);
        press(player);
        check(z.getHealth() < z.getMaxHealth() && z.getRemainingFireTicks() <= 0, "the wave hits and puts out fire");
        check(CustomEvents.spares(player, helper.getLevel().damageSources().drown()), "he never drowns");
        become(player, PowerModule.WATER, "jet", 10);
        Zombie y = zombie(helper, 9.5, 10.5);
        aim(player, y);
        AbilityHandler.onKey(player, 1, true);
        tick(player, 2);
        check(y.getDeltaMovement().x > 0.05, "the jet pushes: " + y.getDeltaMovement());
        AbilityHandler.onKey(player, 1, false);
        check(!CustomHeroes.isOn(player, PowerModule.WATER), "and stops");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void earthShakesAndThrowsSpikesWithoutChangingABlock(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.EARTH, "tremor", 10);
        Zombie z = zombie(helper, 7.5, 10.5);
        z.setOnGround(true);
        press(player);
        check(z.getHealth() < z.getMaxHealth() && z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the tremor");
        become(player, PowerModule.EARTH, "spikes", 10);
        Zombie y = zombie(helper, 12.5, 10.5);
        TestKit.look(player, TestKit.EAST, 0);
        press(player);
        check(y.getHealth() < y.getMaxHealth() && y.getDeltaMovement().y > 0.5, "the spikes throw it up");
        helper.assertBlockPresent(Blocks.SMOOTH_STONE, new BlockPos(12, 1, 10));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void windGustsLiftsAndSoftensFalls(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.WIND, "gust", 10);
        Zombie z = zombie(helper, 8.5, 10.5);
        TestKit.look(player, TestKit.EAST, 0);
        press(player);
        check(z.getDeltaMovement().x > 1.0, "the gust throws: " + z.getDeltaMovement());
        become(player, PowerModule.WIND, "updraft", 10);
        press(player);
        check(player.getDeltaMovement().y >= ModuleStats.UPDRAFT_LIFT.value(10) - 1.0E-3 && CustomHeroes.fallGuarded(player), "up on the wind");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void weatherControlCallsAStormOnTheMonsters(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.WEATHER, "storm", 10);
        Zombie z = zombie(helper, 9.5, 10.5);
        press(player);
        tick(player, 20);
        check(z.getHealth() < z.getMaxHealth(), "lightning strikes the monster near him");
        helper.succeed();
    }

    /** Weather "cycle" turns the world's weather (a batch of its own: it changes the whole world). */
    @GameTest(template = ARENA, batch = "custom_modules_weather")
    public static void weatherControlCyclesTheWorldsWeather(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        boolean raining = level.isRaining();
        boolean thundering = level.isThundering();
        f.onClose(() -> level.setWeatherParameters(raining ? 0 : 12000, raining ? 6000 : 0, raining, thundering));
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.WEATHER, "cycle", 10);
        level.setWeatherParameters(12000, 0, false, false);
        press(player);
        check(level.getLevelData().isRaining(), "clear turns to rain");
        f.config(CustomHeroConfig.WEATHER_CHANGES_WORLD, false);
        Powers.get(player).startCooldown(1, 0, 0);
        level.setWeatherParameters(12000, 0, false, false);
        press(player);
        check(!level.getLevelData().isRaining(), "the server may forbid it");
        helper.succeed();
    }

    // ---- the mind ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void superSensesSeeInTheDarkAndThroughWalls(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.SUPER_SENSES, "night", 10);
        tick(player, 20);
        check(player.hasEffect(MobEffects.NIGHT_VISION), "night vision");
        become(player, PowerModule.SUPER_SENSES, "xray", 10);
        press(player);
        check(CustomHeroes.isOn(player, PowerModule.SUPER_SENSES), "x-ray is a switch (his client draws the glow)");
        become(player, PowerModule.SUPER_SENSES, "danger", 10);
        TestKit.heardSounds(player);
        Zombie z = zombie(helper, 9.5, 10.5);
        MindPowers.danger(player, z);
        check(TestKit.heardSounds(player).stream().anyMatch(s -> s.id().getPath().contains("note_block.bell")), "a warning rings");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void telepathySensesAndReadsMinds(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.TELEPATHY, "sense", 10);
        press(player);
        check(CustomHeroes.isOn(player, PowerModule.TELEPATHY), "sensing minds (his client draws them)");
        become(player, PowerModule.TELEPATHY, "read", 10);
        Zombie z = zombie(helper, 9.5, 10.5);
        aim(player, z);
        press(player);
        check(!Powers.get(player).isReady(1, helper.getLevel().getGameTime()), "a mind was read");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "custom_modules_tk")
    public static void telekinesisIsTheSharedOneInHisColour(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.TELEKINESIS, "creatures", 10);
        Zombie z = zombie(helper, 9.5, 10.5);
        aim(player, z);
        press(player);
        check(Telekinesis.holds(player, z), "the zombie is held");
        check(Telekinesis.Mode.CUSTOM.colorOf(player) == 0x3FA9F5, "in his colour");
        Powers.setPower(player, Power.NONE);
        check(!Telekinesis.isHolding(player), "let go with the power");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void mindControlMakesAllyAndCalmsMonsters(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.MIND_CONTROL, "ally", 10);
        Zombie ally = zombie(helper, 8.5, 10.5);
        Zombie foe = zombie(helper, 11.5, 10.5);
        aim(player, ally);
        press(player);
        check(MindPowers.control(ally) != null && MindPowers.control(ally).kind() == MindPowers.Kind.ALLY, "the zombie is his");
        ally.setTarget(player);
        check(ally.getTarget() != player, "it never turns on him");
        tick(player, 5);
        check(ally.getTarget() == foe, "it goes for the other monster: " + ally.getTarget());
        become(player, PowerModule.MIND_CONTROL, "calm", 10);
        check(MindPowers.controlled(player.getUUID()) == 0, "a new definition lets them go");
        foe.setTarget(player);
        press(player);
        check(foe.getTarget() == null && MindPowers.control(foe).kind() == MindPowers.Kind.CALM, "the monsters forget him");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void animalControlMakesAPack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.ANIMAL_CONTROL, "pack", 10);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, FLOOR, 12.5));
        Zombie z = zombie(helper, 10.5, 10.5);
        aim(player, z);
        press(player);
        MindPowers.Control control = MindPowers.control(cow);
        check(control != null && control.kind() == MindPowers.Kind.PACK && z.getUUID().equals(control.victim()), "the cow goes for the zombie");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void duplicationConjuresCopiesThatFight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, PowerModule.DUPLICATION, "clones", 10);
        press(player);
        List<LokiIllusionEntity> copies = LokiIllusionEntity.ownedBy(helper.getLevel(), player.getUUID());
        check(copies.size() == Math.round(ModuleStats.CLONES.value(10)), "copies: " + copies.size());
        check(copies.stream().allMatch(c -> c.mode() == LokiIllusionEntity.Mode.FIGHTER), "that fight");
        Powers.setPower(player, Power.NONE);
        check(LokiIllusionEntity.ownedBy(helper.getLevel(), player.getUUID()).isEmpty(), "and vanish with the power");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void technopathyFlipsDoorsAndLeversAndCommandsGolems(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos door = new BlockPos(7, 2, 10);
        BlockPos lever = new BlockPos(5, 2, 13);
        helper.setBlock(door, Blocks.OAK_DOOR.defaultBlockState());
        helper.setBlock(door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        helper.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.TECHNOPATHY, "hack", 10);
        press(player);
        check(helper.getBlockState(door).getValue(BlockStateProperties.OPEN), "the door opened");
        check(helper.getBlockState(lever).getValue(BlockStateProperties.POWERED), "the lever flipped");
        become(player, PowerModule.TECHNOPATHY, "command", 10);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new Vec3(12.5, FLOOR, 12.5));
        press(player);
        check(MindPowers.control(golem) != null && MindPowers.control(golem).kind() == MindPowers.Kind.ALLY, "the golem obeys");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void timeSlowsStopsAndRewinds(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.TIME, "stop", 10);
        Zombie z = zombie(helper, 9.5, 10.5);
        aim(player, z);
        press(player);
        check(z.hasEffect(ModEffects.SPELLBOUND), "frozen in time (Billy's spell)");
        become(player, PowerModule.TIME, "slow", 10);
        Zombie y = zombie(helper, 7.5, 12.5);
        press(player);
        tick(player, 10);
        check(y.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the world slows round him");
        become(player, PowerModule.TIME, "rewind", 1);
        Vec3 start = player.position();
        tick(player, 70);
        player.teleportTo(start.x + 6, start.y, start.z);
        player.setHealth(5);
        tick(player, 5);
        Powers.get(player).startCooldown(1, 0, 0);
        press(player);
        check(player.position().distanceTo(start) < 1.0 && player.getHealth() == player.getMaxHealth(), "back where and as he was");
        helper.succeed();
    }

    // ---- the creator's buttons ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void onlyCreativePlayersAndOperatorsMayUseTheCreator(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer survivor = f.player(5.5, FLOOR, 5.5);
        check(CustomHeroes.refusal(survivor) != null && !CustomHeroes.apply(survivor, CustomHeroDataTests.sample()), "Survival: refused");
        check(Powers.power(survivor) == Power.NONE, "and nothing happened");
        ServerPlayer creative = f.player(8.5, FLOOR, 5.5, GameType.CREATIVE);
        check(CustomHeroes.apply(creative, CustomHeroDataTests.sample()), "Creative: allowed");
        check(Powers.power(creative) == Power.CUSTOM && CustomHeroes.hero(creative).name().equals("Storm Girl"), "he is Storm Girl");
        check(!CustomHeroes.apply(creative, CustomHeroDataTests.sample()), "a second hero at once must wait");
        ServerPlayer chooser = f.player(11.5, FLOOR, 5.5, GameType.CREATIVE);
        check(CustomHeroes.choose(chooser, Power.THOR, "ragnarok"), "choosing one of the mod's heroes");
        check(Powers.power(chooser) == Power.THOR && Powers.get(chooser).variant().equals("ragnarok"), "Thor in his Ragnarok look");
        check(!CustomHeroes.choose(chooser, Power.CUSTOM, ""), "a custom hero is never chosen without its definition");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "custom_modules_config")
    public static void theServerCanSwitchTheCreatorOffAndLimitIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(CustomHeroConfig.ENABLED, false);
        ServerPlayer creative = f.player(5.5, FLOOR, 5.5, GameType.CREATIVE);
        check(!CustomHeroes.apply(creative, CustomHeroDataTests.sample()) && Powers.power(creative) == Power.NONE, "switched off");
        f.config(CustomHeroConfig.ENABLED, true);
        f.config(CustomHeroConfig.OPERATORS_ONLY, true);
        check(CustomHeroes.refusal(creative) != null, "operators only");
        f.config(CustomHeroConfig.OPERATORS_ONLY, false);
        f.config(CustomHeroConfig.ALLOW_CHOOSE_EXISTING, false);
        check(!CustomHeroes.choose(creative, Power.THOR, ""), "choosing switched off");
        f.config(CustomHeroConfig.BUDGET, 10);
        f.config(CustomHeroConfig.MAX_STRENGTH, 3);
        f.config(CustomHeroConfig.APPLY_COOLDOWN, 0);
        check(CustomHeroes.apply(creative, CustomHeroDataTests.sample()), "a hero over the limits is still made");
        CustomHero made = CustomHeroes.hero(creative);
        check(CustomHeroRules.cost(made) <= 10 && made.powers().stream().allMatch(p -> p.strength() <= 3), "within them: " + made.powers());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void deathEndsHeldPowersAndLogoutForgetsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 5.5, 10.5, PowerModule.FORCE_FIELD, "reflect", 5);
        AbilityHandler.onKey(player, 1, true);
        check(CustomHeroes.state(player).field(), "the field is up");
        player.hurt(helper.getLevel().damageSources().source(DamageTypes.GENERIC_KILL), Float.MAX_VALUE);
        check(!CustomHeroes.state(player).field(), "death drops it");
        helper.succeed();
    }

    private CustomHeroModuleTests() {
    }
}

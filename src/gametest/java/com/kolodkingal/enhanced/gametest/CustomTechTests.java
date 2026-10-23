package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.CyclopsAbilities;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.MindLink;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.ability.Thoughts;
import com.kolodkingal.enhanced.apocalypse.SandHazards;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.config.CyclopsConfig;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.config.XavierConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Resource;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronDrones;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The abilities of Spider-Man, Ultron, Apocalypse, Cyclops and Professor X in a custom hero's hands (3.4, lane L4): each row
 * on a key that is not its hero's own (so its cooldown is the custom column's), its Sneak variant through the Sneak cell, the
 * parts of a hero's upkeep that belong to one row only, the bodies only with their rows, the items a custom hero does not
 * need (Cyclops's glasses, the Iron Spider's look), the ends that leave nothing behind, and the names of the rows in both
 * languages. A test that keeps server-wide state (the sand that rolls, a beam, the drones, a link) has a batch of its own
 * and {@link TestKit#bossFixture}.
 */
public final class CustomTechTests {
    private static final String QUICK = "custom_tech";
    private static final String PVP = "custom_tech_pvp";
    private static final String SPIDER = "custom_tech_spider";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A custom hero with the given rows (strength 5), each on the given column (0: on no key), judged by the rules. */
    private static ServerPlayer hero(TestKit.Fixture f, double x, double z, Object... idsAndColumns) {
        CustomKitSupport.Def def = CustomKitSupport.def();
        for (int i = 0; i < idsAndColumns.length; i += 2) {
            String id = (String) idsAndColumns[i];
            int column = (Integer) idsAndColumns[i + 1];
            def.pick(id, 5);
            if (column > 0) {
                def.key(column, id);
            }
        }
        ServerPlayer player = CustomKitSupport.hero(f, x, z, def.build());
        player.setHealth(player.getMaxHealth());
        f.onClose(() -> {
            ContinuousBeam.forget(player);
            HeroLifecycle.onLogout(player);
        });
        return player;
    }

    private static PowerData data(ServerPlayer player) {
        return Powers.get(player);
    }

    private static long now(ServerPlayer player) {
        return player.level().getGameTime();
    }

    /** How long the cooldown of a column has left. */
    private static long left(ServerPlayer player, int column) {
        return data(player).cooldownUntil(column) - now(player);
    }

    /** Only column {@code column} is cooling down (the custom column, not the hero's own key). */
    private static void onlyColumnCools(ServerPlayer player, int column, String what) {
        for (int c = 1; c <= PowerData.ABILITIES; c++) {
            boolean cooling = left(player, c) > 0;
            check(cooling == (c == column), what + ": column " + c + (cooling ? " cools down" : " is ready"));
        }
    }

    /** Lets every column's cooldown run out at once. */
    private static void rest(ServerPlayer player) {
        for (int c = 1; c <= PowerData.ABILITIES; c++) {
            data(player).startCooldown(c, now(player), 0);
        }
    }

    /** A husk with no armour and 200 health that stands where it is put. */
    private static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    /** A husk that stands where it is put and does not burn in the sun. */
    private static Zombie zombie(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
    }

    /** A husk with its own will (it walks when steered). */
    private static Husk walker(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawn(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.setPersistenceRequired();
        return husk;
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    private static Vec3 abs(GameTestHelper helper, double x, double y, double z) {
        return helper.absoluteVec(new Vec3(x, y, z));
    }

    /** Offers a blow to the damage chain as it would come to the target: whether it was stopped. */
    private static boolean stopped(ServerPlayer target, DamageSource source, float amount) {
        LivingIncomingDamageEvent event = new LivingIncomingDamageEvent(target, new DamageContainer(source, amount));
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Spider-Man

    /** spider.web on B: the shot webs the creature looked at; Sneak + B is the Web Burst, its cooldown on B. */
    @EnhancedTest(template = ARENA, batch = SPIDER, timeoutTicks = 60)
    public static void spiderWebOnB(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hero(f, 10.5, 6.5, "spider.web", 4);
        Zombie zombie = zombie(helper, 10.5, 12.5);
        lookAt(p, zombie);
        PowerTicker.tick(p);
        CustomKitSupport.tap(p, 4, false);
        MobEffectInstance webbed = zombie.getEffect(ModEffects.WEBBED);
        check(webbed != null && webbed.getDuration() == SpiderConfig.WEBBED_TICKS.get(), "the zombie is Webbed: " + webbed);
        check(helper.getBlockState(new BlockPos(10, 2, 12)).is(ModBlocks.SPIDER_WEB.get()), "with a web block at its feet");
        check(data(p).is(Live.CASTING), "his hand is thrust out");
        f.everyTick(10, tick -> {
            PowerTicker.tick(p);
            if (tick < 8) {
                return false;
            }
            Zombie near = zombie(helper, 12.5, 6.5);
            CustomKitSupport.tap(p, 4, true);
            check(near.hasEffect(ModEffects.WEBBED), "Sneak + B: the Web Burst webs who is near");
            onlyColumnCools(p, 4, "the burst's cooldown");
            assertNear(left(p, 4), SpiderConfig.WEB_BURST_COOLDOWN.get(), 1.0, "its length at strength 5");
            check(!data(p).toggle(Toggle.WALL_CRAWL), "no wall-crawl of Spider-Man's without its row");
            return true;
        });
    }

    /** spider.zip on R: the zip starts and ends at the block; Sneak + R held is a swing that ends when R is let go. */
    @EnhancedTest(template = ARENA, batch = SPIDER, timeoutTicks = 60)
    public static void spiderZipAndSwingOnR(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.ZIP_COOLDOWN, 0);
        ServerPlayer p = hero(f, 10.5, 6.5, "spider.zip", 1);
        fill(helper, 10, 2, 16, 10, 6, 16, Blocks.STONE);
        lookAt(p, abs(helper, 10.5, 3.5, 16.0));
        PowerTicker.tick(p);
        CustomKitSupport.tap(p, 1, false);
        check(data(p).is(Live.ZIPPING) && SpiderServer.zipStand(p) != null, "R: he zips");
        Vec3 stand = SpiderServer.zipStand(p);
        for (int i = 0; i < 8 && data(p).is(Live.ZIPPING); i++) {
            Vec3 to = stand.subtract(p.position());
            Vec3 next = to.length() <= 2.5 ? stand : p.position().add(to.normalize().scale(2.5));
            p.absMoveTo(next.x, next.y, next.z);
            PowerTicker.tick(p);
        }
        check(!data(p).is(Live.ZIPPING) && p.position().distanceTo(stand) < 0.1, "the zip moves him to the wall and ends there");

        ServerPlayer s = hero(f, 4.5, 4.5, "spider.zip", 1);
        fill(helper, 3, 6, 8, 5, 8, 8, Blocks.STONE);
        Vec3 air = abs(helper, 4.5, 5.0, 4.5);
        s.absMoveTo(air.x, air.y, air.z);
        lookAt(s, abs(helper, 4.5, 6.5, 8.0));
        PowerTicker.tick(s);
        CustomKitSupport.press(s, 1, true, true);
        check(data(s).is(Live.SWINGING) && SpiderServer.swingAnchor(s) != null, "Sneak + R held: a swing");
        CustomKitSupport.press(s, 1, false, false);
        check(!data(s).is(Live.SWINGING), "R let go (Sneak or not): the swing ends");
        helper.succeed();
    }

    /** spider.crawl on N: crawl and the sense start on; N switches the crawl, Sneak + N the sense; it clings with the row only. */
    @EnhancedTest(template = ARENA, batch = SPIDER)
    public static void spiderCrawlOnN(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hero(f, 4.5, 4.5, "spider.crawl", 5);
        PowerData data = data(p);
        check(data.toggle(Toggle.WALL_CRAWL) && data.toggle(Toggle.SPIDER_SENSE), "both start on, as for Spider-Man");
        CustomKitSupport.tap(p, 5, false);
        check(!data.toggle(Toggle.WALL_CRAWL) && data.toggle(Toggle.SPIDER_SENSE), "N: the crawl goes off");
        CustomKitSupport.tap(p, 5, true);
        check(!data.toggle(Toggle.SPIDER_SENSE), "Sneak + N: the sense goes off");
        CustomKitSupport.tap(p, 5, false);
        CustomKitSupport.tap(p, 5, true);
        check(data.toggle(Toggle.WALL_CRAWL) && data.toggle(Toggle.SPIDER_SENSE), "and both back on");
        check(CustomKitSupport.kit(p).has(HeroAbility.SPIDER_CRAWL) && SpiderServer.isSpiderMan(p), "his webs are his: web blocks let him through");
        // His client says he clings to the wall beside him: the server believes it (the cling report is for him too).
        fill(helper, 5, 2, 3, 5, 5, 5, Blocks.STONE);
        Vec3 at = abs(helper, 4.69, 3.0, 4.5);
        p.absMoveTo(at.x, at.y, at.z);
        SpiderServer.onClingReport(p, com.kolodkingal.enhanced.network.SpiderClingPayload.WALL, net.minecraft.core.Direction.WEST.get3DDataValue());
        PowerTicker.tick(p);
        check(data.is(Live.CLINGING_WALL), "he clings to the wall");
        // Without the row, the report is not believed.
        ServerPlayer other = hero(f, 14.5, 4.5, "spider.web", 1);
        SpiderServer.onClingReport(other, com.kolodkingal.enhanced.network.SpiderClingPayload.WALL, net.minecraft.core.Direction.WEST.get3DDataValue());
        PowerTicker.tick(other);
        check(!data(other).is(Live.CLINGING_WALL) && !data(other).toggle(Toggle.WALL_CRAWL), "a custom hero without the crawl does not cling");
        helper.succeed();
    }

    /** spider.legs on R in his own look: the legs come out (reach, no fall damage); Sneak + R twice is Instant Kill. */
    @EnhancedTest(template = ARENA, batch = SPIDER)
    public static void spiderLegsInAnyLook(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hero(f, 4.5, 4.5, "spider.legs", 1);
        PowerData data = data(p);
        double reach = p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        CustomKitSupport.tap(p, 1, false);
        PowerTicker.tick(p);
        check(data.toggle(Toggle.SPIDER_LEGS), "R: the legs come out in his own look");
        assertNear(p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), reach + SpiderConfig.LEGS_REACH_BONUS.get(), 1.0E-6, "a longer reach");
        assertNear(p.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 0.0, 1.0E-6, "and no fall damage");
        check(SpiderServer.isIronSpider(p), "an Iron Spider (metal for Magneto)");
        CustomKitSupport.tap(p, 1, true);
        check(!data.is(Live.INSTANT_KILL), "Sneak + R once: it asks to be confirmed");
        CustomKitSupport.tap(p, 1, true);
        check(data.is(Live.INSTANT_KILL), "twice: Instant Kill");
        onlyColumnCools(p, 1, "Instant Kill's clock");
        CustomKitSupport.tap(p, 1, true);
        check(!data.is(Live.INSTANT_KILL), "again: it ends");
        CustomKitSupport.tap(p, 1, false);
        PowerTicker.tick(p);
        check(!data.toggle(Toggle.SPIDER_LEGS), "R: the legs go in");
        assertNear(p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), reach, 1.0E-6, "the reach back");
        helper.succeed();
    }

    /** spider.black_eyes on G: the surge with its speed; G again calms it, with half the cooldown on G. */
    @EnhancedTest(template = ARENA, batch = SPIDER)
    public static void spiderBlackEyesOnG(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hero(f, 4.5, 4.5, "spider.black_eyes", 2);
        double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        CustomKitSupport.tap(p, 2, false);
        PowerTicker.tick(p);
        check(data(p).is(Live.BLACK_EYES), "G: the Black Eyes");
        check(p.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed * 1.1, "faster");
        CustomKitSupport.tap(p, 2, false);
        PowerTicker.tick(p);
        check(!data(p).is(Live.BLACK_EYES), "G again calms it");
        onlyColumnCools(p, 2, "its cooldown");
        assertNear(left(p, 2), SpiderConfig.BLACK_EYES_COOLDOWN.get() / 2, 1.0, "half, calmed");
        helper.succeed();
    }

    /** The body of Spider-Man comes with spider.body only, scaled by its strength; another of his rows gives none of it. */
    @EnhancedTest(template = ARENA, batch = SPIDER)
    public static void spiderBodyOnlyWithItsRow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer web = hero(f, 4.5, 4.5, "spider.web", 1);
        PowerTicker.tick(web);
        check(web.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Enhanced.id("spider_speed")) == null, "no body with the web only");
        assertNear(web.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 1.0, 1.0E-6, "his falls are anyone's");
        ServerPlayer body = hero(f, 8.5, 4.5, "spider.body", 0);
        PowerTicker.tick(body);
        assertNear(body.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.1 * (1.0 + SpiderConfig.SPEED_BONUS.get()), 1.0E-6, "spider.body 5: his speed");
        assertNear(body.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 1.0 - SpiderConfig.FALL_DAMAGE_REDUCTION.get(), 1.0E-6, "and his falls");
        ServerPlayer strong = CustomKitSupport.hero(f, 12.5, 4.5, CustomKitSupport.def().pick("spider.body", 10).build());
        f.onClose(() -> HeroLifecycle.onLogout(strong));
        PowerTicker.tick(strong);
        assertNear(strong.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.1 * (1.0 + SpiderConfig.SPEED_BONUS.get() * 1.5), 1.0E-6,
                "strength 10: half as much again");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ultron

    /** ultron.repulsor on V: held, plasma bolts fly; let go, they stop. Sneak + V: the Overcharge, its cooldown on V. */
    @EnhancedTest(template = ARENA, batch = "custom_tech_ultron", timeoutTicks = 100)
    public static void ultronRepulsorOnV(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer p = hero(f, 3.5, 10.5, "ultron.repulsor", 3);
        TestKit.look(p, TestKit.EAST, 0.0F);
        PowerData data = data(p);
        PowerTicker.tick(p);
        check(data.ironCharge() == UltronConfig.BOLT_POOL.get(), "the pool starts full");
        CustomKitSupport.press(p, 3, false, true);
        check(data.beamActive(), "V held: he fires");
        f.everyTick(60, tick -> {
            PowerTicker.tick(p);
            if (tick == 12) {
                CustomKitSupport.press(p, 3, false, false);
                check(!data.beamActive(), "let go: he stops");
                check(data.ironCharge() < UltronConfig.BOLT_POOL.get(), "the bolts took plasma: " + data.ironCharge());
                check(level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds()).stream().anyMatch(b -> b.getOwner() == p),
                        "his bolts fly");
                data.setIronCharge(UltronConfig.BOLT_POOL.get());
                CustomKitSupport.press(p, 3, true, true);
                p.setShiftKeyDown(false);
                check(data.is(Live.ULTRON_CHARGING), "Sneak + V: the Overcharge builds");
            }
            if (tick == 14) {
                assertNear(p.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.1 * (1.0 - UltronConfig.OVERCHARGE_SLOW.get()), 1.0E-4,
                        "slowed while it builds");
            }
            if (tick == 12 + UltronConfig.OVERCHARGE_CHARGE_TICKS.get() + 1) {
                check(!data.is(Live.ULTRON_CHARGING), "the blast flies");
                onlyColumnCools(p, 3, "the Overcharge's cooldown");
                assertNear(left(p, 3), UltronConfig.OVERCHARGE_COOLDOWN.get(), 1.0, "its length at strength 5");
                return true;
            }
            return false;
        });
    }

    /** ultron.gravity on R pulls a creature, its cooldown on R; Sneak + R slams; ultron.body: the machine. */
    @EnhancedTest(template = ARENA, batch = "custom_tech_ultron_pull", timeoutTicks = 60)
    public static void ultronGravityOnRAndHisBody(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer p = hero(f, 3.5, 10.5, "ultron.gravity", 1, "ultron.body", 0);
        Husk husk = husk(helper, 10.5, 10.5);
        lookAt(p, husk);
        double before = p.distanceTo(husk);
        CustomKitSupport.tap(p, 1, false);
        onlyColumnCools(p, 1, "the pull's cooldown");
        assertNear(p.getMaxHealth(), 20.0 + UltronConfig.BODY_HEALTH.get(), 1.0E-4, "ultron.body: his health");
        p.getFoodData().setFoodLevel(5);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        check(!p.hasEffect(MobEffects.POISON), "a machine takes no poison");
        f.everyTick(30, tick -> {
            PowerTicker.tick(p);
            if (tick == 20) {
                check(p.distanceTo(husk) < before - 3.0, "the husk was pulled to him: " + p.distanceTo(husk));
                check(p.getFoodData().getFoodLevel() == 20, "and he is never hungry");
                rest(p);
                CustomKitSupport.tap(p, 1, true);
                onlyColumnCools(p, 1, "Sneak + R: the slam's cooldown");
                assertNear(left(p, 1), UltronConfig.SLAM_COOLDOWN.get(), 1.0, "the slam's length");
                return true;
            }
            return false;
        });
    }

    /** ultron.army on G: Sentries of his own; ultron.upload on B jumps into one; losing his powers takes the army away. */
    @EnhancedTest(template = ARENA, batch = "custom_tech_ultron_army", timeoutTicks = 100)
    public static void ultronArmyAndUploadForACustomOwner(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer p = hero(f, 10.5, 10.5, "ultron.army", 2, "ultron.upload", 4);
        CustomKitSupport.tap(p, 2, false);
        List<UltronSentryEntity> drones = UltronDrones.drones(p);
        check(drones.size() == UltronConfig.DRONE_COUNT.get(), "G: his Sentries assemble: " + drones.size());
        onlyColumnCools(p, 2, "the army's cooldown");
        f.everyTick(60, tick -> {
            PowerTicker.tick(p);
            if (tick < 40) {
                return false;
            }
            List<Vec3> at = UltronDrones.drones(p).stream().map(UltronSentryEntity::position).toList();
            CustomKitSupport.tap(p, 4, false);
            check(at.stream().anyMatch(d -> p.position().distanceTo(d) < 2.0) && UltronDrones.drones(p).size() == at.size() - 1,
                    "B: he uploads into one of his drones");
            check(left(p, 4) > 0 && left(p, 2) > 0, "B rests too");
            // Second Life is the upload's: a deadly blow with a drone near and B ready, and he lives on.
            rest(p);
            p.invulnerableTime = 0;
            p.hurt(f.level().damageSources().generic(), 1000.0F);
            check(p.isAlive() && p.getHealth() > 0.0F, "Second Life saves him");
            check(left(p, 4) > 0, "and B rests");
            CustomHeroes.become(p, CustomHero.EMPTY);
            check(UltronDrones.drones(p).isEmpty(), "his army goes with his powers");
            return true;
        });
    }

    /** magneto.shrapnel and ultron.repulsor use the one iron charge: the later one is left out with a note. */
    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void theIronChargeIsOneOwners(GameTestHelper helper) {
        CustomHero hero = CustomKitSupport.def().pick("magneto.shrapnel", 5).pick("ultron.repulsor", 5).raw();
        CustomHeroRules.Result result = CustomHeroRules.sanitize(hero, CustomHeroRules.Limits.DEFAULT);
        check(result.hero().pick(HeroAbility.MAGNETO_SHRAPNEL) != null && result.hero().pick(HeroAbility.ULTRON_REPULSOR) == null,
                "the repulsors are left out");
        check(result.notes().stream().anyMatch(n -> n.key().endsWith("rule.resource")), "with a note: " + result.notes());
        check(HeroAbility.ULTRON_REPULSOR.resources(HeroAbility.BOTH).contains(Resource.IRON_CHARGE), "Ultron's pool is the charge");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Apocalypse

    /** apocalypse.shield on R: held, it forms and stops a blow; let go, it drops with its cooldown on R. */
    @EnhancedTest(template = ARENA, batch = "custom_tech_apoc_shield", timeoutTicks = 100)
    public static void apocalypseShieldOnR(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer p = hero(f, 10.5, 10.5, "apocalypse.shield", 1);
        PowerData data = data(p);
        CustomKitSupport.press(p, 1, false, true);
        check(data.chaosShieldActive() && !ApocalypseGuard.isUp(p), "R held: it forms");
        int windUp = ApocalypseConfig.SHIELD_WIND_UP_TICKS.get();
        f.everyTick(windUp + 4, tick -> {
            PowerTicker.tick(p);
            if (tick < windUp + 2) {
                return false;
            }
            check(ApocalypseGuard.isUp(p), "complete after the wind-up");
            check(stopped(p, f.level().damageSources().magic(), 6.0F), "it stops a blow");
            CustomKitSupport.press(p, 1, false, false);
            check(!data.chaosShieldActive() && !ApocalypseGuard.isUp(p), "R let go: it drops");
            onlyColumnCools(p, 1, "the shield's recharge");
            assertNear(left(p, 1), ApocalypseConfig.SHIELD_COOLDOWN.get(), 1.0, "its length");
            return true;
        });
    }

    /** apocalypse.sand on G: a wave rolls, its cooldown on G; Sneak + G entombs. Nothing of his body without its row. */
    @EnhancedTest(template = ARENA, batch = "custom_tech_apoc_sand", timeoutTicks = 120)
    public static void apocalypseSandOnG(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer p = hero(f, 1.5, 10.5, "apocalypse.sand", 2);
        TestKit.look(p, TestKit.EAST, 0.0F);
        Husk husk = husk(helper, 6.5, 10.5);
        CustomKitSupport.tap(p, 2, false);
        check(SandHazards.count() == 1, "G: a wave rolls");
        onlyColumnCools(p, 2, "the wave's cooldown");
        assertNear(left(p, 2), ApocalypseConfig.WAVE_COOLDOWN.get(), 1.0, "its length");
        assertNear(p.getMaxHealth(), 20.0, 1.0E-4, "no body of Apocalypse's without its row");
        f.everyTick(100, tick -> {
            PowerTicker.tick(p);
            if (SandHazards.count() > 0) {
                return false;
            }
            check(husk.getHealth() < 200.0F, "the husk in its path is hurt");
            rest(p);
            lookAt(p, husk);
            CustomKitSupport.tap(p, 2, true);
            check(husk.hasEffect(ModEffects.ENTOMBED), "Sneak + G: entombed");
            onlyColumnCools(p, 2, "the entomb's cooldown");
            return true;
        });
    }

    /** apocalypse.bestow on V (Sneak: the storm) and apocalypse.portal on N; apocalypse.body: his body and healing. */
    @EnhancedTest(template = ARENA, batch = "custom_tech_apoc_bestow", timeoutTicks = 100)
    public static void apocalypseBestowPortalAndBody(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer p = hero(f, 4.5, 10.5, "apocalypse.bestow", 3, "apocalypse.portal", 5, "apocalypse.body", 0);
        ServerPlayer ally = f.player(8.5, FLOOR, 10.5);
        assertNear(p.getMaxHealth(), 20.0 + ApocalypseConfig.BODY_HEALTH_BONUS.get(), 1.0E-4, "apocalypse.body 5: his health");
        lookAt(p, ally);
        CustomKitSupport.tap(p, 3, false);
        check(ally.hasEffect(ModEffects.EMPOWERED), "V: the ally is empowered");
        onlyColumnCools(p, 3, "the bestowal's cooldown");
        rest(p);
        CustomKitSupport.tap(p, 3, true);
        check(data(p).is(Live.SANDSTORM), "Sneak + V: the sandstorm");
        TestKit.look(p, TestKit.EAST, 30.0F);
        Vec3 from = p.position();
        CustomKitSupport.tap(p, 5, false);
        check(p.position().distanceTo(from) > 2.0, "N: through the portal");
        check(left(p, 5) > 0 && left(p, 3) > 0, "N rests too");
        CustomHeroes.become(p, CustomHero.EMPTY);
        check(!data(p).is(Live.SANDSTORM) && !ally.hasEffect(ModEffects.EMPOWERED), "losing his powers ends the storm and the gift");
        assertNear(p.getMaxHealth(), 20.0, 1.0E-4, "and his body");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Cyclops

    /** cyclops.optic_blast on G without glasses: held, it burns; let go, it stops; Sneak + G: the Ricochet, its cooldown on G. */
    @EnhancedTest(template = ARENA, batch = "custom_tech_optic", timeoutTicks = 60)
    public static void cyclopsOpticBlastNeedsNoGlasses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer p = hero(f, 3.5, 10.5, "cyclops.optic_blast", 2);
        Husk husk = husk(helper, 9.5, 10.5);
        lookAt(p, husk);
        CustomKitSupport.press(p, 2, false, true);
        check(data(p).is(Live.BEAMING) && CyclopsAbilities.opticFiring(p), "G held, no glasses: the optic blast burns");
        f.everyTick(30, tick -> {
            lookAt(p, husk);
            PowerTicker.tick(p);
            if (tick < 20) {
                return false;
            }
            check(husk.getHealth() < 200.0F, "the husk is hurt");
            CustomKitSupport.press(p, 2, false, false);
            check(!data(p).is(Live.BEAMING) && !ContinuousBeam.isFiring(p), "let go: it stops");
            float health = husk.getHealth();
            CustomKitSupport.tap(p, 2, true);
            check(husk.getHealth() < health, "Sneak + G: the Ricochet hits");
            onlyColumnCools(p, 2, "the Ricochet's cooldown");
            assertNear(left(p, 2), CyclopsConfig.RICOCHET_COOLDOWN.get(), 1.0, "its length");
            return true;
        });
    }

    /** cyclops.glasses on R: Sneak + R opens his eyes (the giant beam pours out); R shuts them and it stops. */
    @EnhancedTest(template = ARENA, batch = "custom_tech_unleashed", timeoutTicks = 60)
    public static void cyclopsEyesOpenAndShut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer p = hero(f, 3.5, 10.5, "cyclops.glasses", 1);
        Husk husk = husk(helper, 9.5, 10.5);
        lookAt(p, husk);
        PowerData data = data(p);
        CustomKitSupport.tap(p, 1, true);
        check(data.is(Live.CYCLOPS_UNLEASHED) && ContinuousBeam.isFiring(p), "Sneak + R: the giant beam pours out");
        onlyColumnCools(p, 1, "the gesture's signal");
        f.everyTick(30, tick -> {
            lookAt(p, husk);
            PowerTicker.tick(p);
            check(data.is(Live.CYCLOPS_UNLEASHED), "it does not stop by itself (tick " + tick + ")");
            if (tick < 20) {
                return false;
            }
            check(husk.getHealth() < 200.0F, "it hurts what it meets");
            CustomKitSupport.tap(p, 1, false);
            check(!data.is(Live.CYCLOPS_UNLEASHED) && !ContinuousBeam.isFiring(p), "R: his eyes shut, it stops");
            PowerTicker.tick(p);
            check(!data.is(Live.CYCLOPS_UNLEASHED), "and stays stopped (no glasses are watched for him)");
            return true;
        });
    }

    /** cyclops.concussion on N, cyclops.leap on B, cyclops.rally on V: each on its column; cyclops.body only with its row. */
    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void cyclopsBlastsLeapAndRally(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hero(f, 4.5, 10.5, "cyclops.concussion", 5, "cyclops.leap", 4, "cyclops.rally", 3);
        Husk husk = husk(helper, 7.5, 10.5);
        lookAt(p, husk);
        CustomKitSupport.tap(p, 5, false);
        check(husk.getHealth() < 200.0F, "N: the concussion blast hits, without glasses");
        onlyColumnCools(p, 5, "the blast's cooldown");
        rest(p);
        CustomKitSupport.tap(p, 4, false);
        check(p.getDeltaMovement().y > 0.3 && CyclopsAbilities.leaping(p), "B: the Optic Leap");
        onlyColumnCools(p, 4, "the leap's cooldown");
        rest(p);
        CustomKitSupport.tap(p, 3, false);
        check(p.hasEffect(MobEffects.DAMAGE_BOOST), "V: the Rally");
        onlyColumnCools(p, 3, "the rally's cooldown");
        assertNear(p.getMaxHealth(), 20.0, 1.0E-4, "no body of Cyclops's without its row");
        ServerPlayer body = hero(f, 14.5, 10.5, "cyclops.body", 0);
        assertNear(body.getMaxHealth(), 20.0 + CyclopsConfig.BODY_HEALTH_BONUS.get(), 1.0E-4, "cyclops.body: his health");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Professor X

    /** xavier.mental_blast on N and xavier.freeze_minds on G, each on its column; thoughts on V; Cerebro on B. */
    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void xavierBlastFreezeThoughtsAndCerebro(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hero(f, 4.5, 10.5, "xavier.mental_blast", 5, "xavier.freeze_minds", 2, "xavier.thoughts", 3,
                "xavier.cerebro", 4);
        PowerData data = data(p);
        Zombie husk = zombie(helper, 9.5, 10.5);
        float before = husk.getHealth();
        lookAt(p, husk);
        CustomKitSupport.tap(p, 5, false);
        check(husk.getHealth() < before && husk.hasEffect(MobEffects.CONFUSION), "N: the Mental Blast");
        onlyColumnCools(p, 5, "the blast's cooldown");
        assertNear(left(p, 5), XavierConfig.BLAST_COOLDOWN.get(), 1.0, "its length");
        rest(p);
        CustomKitSupport.tap(p, 2, false);
        check(husk.hasEffect(ModEffects.SPELLBOUND), "G: Freeze Minds");
        onlyColumnCools(p, 2, "the freeze's cooldown");
        CustomKitSupport.tap(p, 3, false);
        check(data.toggle(Toggle.HEAR_THOUGHTS) && Thoughts.listens(p, data), "V: he hears thoughts");
        CustomKitSupport.tap(p, 4, false);
        check(data.is(Live.CEREBRO), "B: Cerebro");
        check(left(p, 4) > 0, "B rests");
        CustomHeroes.become(p, CustomHero.EMPTY);
        check(!data(p).toggle(Toggle.HEAR_THOUGHTS) && !data(p).is(Live.CEREBRO), "his powers gone: quiet");
        helper.succeed();
    }

    /** xavier.take_over on R: R takes the creature looked at, R again brings him back, and R rests. */
    @EnhancedTest(template = ARENA, batch = "custom_tech_link", timeoutTicks = 60)
    public static void xavierTakeOverReturnsOnItsColumn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer p = hero(f, 3.5, 10.5, "xavier.take_over", 1);
        f.onClose(() -> MindLink.end(p.server, p.getUUID(), MindLink.End.SERVER));
        Husk husk = walker(helper, 6.5, 10.5);
        lookAt(p, husk);
        CustomKitSupport.tap(p, 1, false);
        check(MindLink.isLinked(p) && MindLink.isHost(husk) && data(p).is(Live.MIND_LINKED), "R: he is in the husk");
        f.everyTick(20, tick -> {
            PowerTicker.tick(p);
            check(MindLink.isLinked(p), "the link holds (tick " + tick + ")");
            if (tick < 10) {
                return false;
            }
            CustomKitSupport.tap(p, 1, false);
            check(!MindLink.isLinked(p) && !MindLink.isHost(husk) && !data(p).is(Live.MIND_LINKED), "R again: he is back");
            onlyColumnCools(p, 1, "the link's cooldown");
            return true;
        });
    }

    /** With PvP on, a custom Mental Blast hurts a player through armour, scaled by its strength (2 against 9). */
    @EnhancedTest(template = ARENA, batch = PVP)
    public static void theMentalBlastScalesByStrength(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        float[] dealt = new float[2];
        int[] strengths = {2, 9};
        for (int i = 0; i < 2; i++) {
            ServerPlayer p = CustomKitSupport.hero(f, 3.5 + i * 8, 4.5,
                    CustomKitSupport.def().pick("xavier.mental_blast", strengths[i]).key(1, "xavier.mental_blast").build());
            f.onClose(() -> HeroLifecycle.onLogout(p));
            ServerPlayer target = f.player(3.5 + i * 8, FLOOR, 9.5);
            lookAt(p, target);
            CustomKitSupport.tap(p, 1, false);
            dealt[i] = target.getMaxHealth() - target.getHealth();
            check(target.getLastDamageSource() != null && target.getLastDamageSource().is(ModDamageTypes.MIND_BLAST), "a Mental Blast");
        }
        float base = XavierConfig.BLAST_DAMAGE.get().floatValue();
        assertNear(dealt[0], Math.min(20.0F, base * 0.7F), 0.05, "strength 2: 0.7 of it");
        assertNear(dealt[1], Math.min(20.0F, base * 1.4F), 0.05, "strength 9: 1.4 of it");
        check(dealt[0] < dealt[1], "the stronger blast hurts more");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The generic Wall Crawl, the names

    /** The generic Wall Crawl, on, raises the cling flag Spider-Man's stances are read from; off or on the ground, none. */
    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void theGenericWallCrawlSetsTheClingFlags(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = CustomKitSupport.hero(f, 4.5, 4.5, CustomKitSupport.def().pick("wall_crawl", 5).key(1, "wall_crawl").build());
        fill(helper, 5, 2, 3, 5, 6, 5, Blocks.STONE);
        check(CustomHeroes.state(p).isOn(PowerModule.WALL_CRAWL), "the crawl starts on");
        Vec3 wall = abs(helper, 4.69, 3.5, 4.5);
        p.absMoveTo(wall.x, wall.y, wall.z);
        p.setOnGround(false);
        CustomKitSupport.tickN(p, 1);
        check(data(p).is(Live.CLINGING_WALL) && !data(p).is(Live.CLINGING_CEILING), "on the wall in the air: the wall flag");
        p.setOnGround(true);
        CustomKitSupport.tickN(p, 1);
        check(!data(p).is(Live.CLINGING_WALL), "on the ground: none");
        p.setOnGround(false);
        CustomKitSupport.tap(p, 1, false);
        CustomKitSupport.tickN(p, 1);
        check(!data(p).is(Live.CLINGING_WALL), "switched off: none");
        helper.succeed();
    }

    /** Every row of the five heroes has its name and its line (and its Sneak variant's name) in English and Hebrew. */
    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void everyRowOfTheFiveHasItsNames(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        List<String> missing = new ArrayList<>();
        for (String name : List.of("en_us", "he_il")) {
            Path file = mod.findResource("assets", Enhanced.MODID, "lang", name + ".json");
            JsonObject lang = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Power hero : List.of(Power.SPIDER_MAN, Power.ULTRON, Power.APOCALYPSE, Power.CYCLOPS, Power.PROFESSOR_X)) {
                for (HeroAbility a : HeroAbility.of(hero)) {
                    List<String> keys = new ArrayList<>(List.of(a.nameKey(), a.descKey()));
                    if (a.hasAlt()) {
                        keys.add(a.altKey());
                    }
                    for (String key : keys) {
                        if (!lang.has(key)) {
                            missing.add(name + ": " + key);
                        }
                    }
                }
            }
            if (!lang.has(CustomHeroRules.resourceKey(Resource.IRON_CHARGE))) {
                missing.add(name + ": the iron charge");
            }
        }
        check(missing.isEmpty(), "missing: " + missing);
        helper.succeed();
    }

    private CustomTechTests() {
    }
}

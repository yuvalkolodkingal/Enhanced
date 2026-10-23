package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.nano.NanoBeams;
import com.kolodkingal.enhanced.ironman.nano.NanoDroneEntity;
import com.kolodkingal.enhanced.ironman.nano.NanoDrones;
import com.kolodkingal.enhanced.ironman.nano.NanoTech;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.IronCoreTests.step;
import static com.kolodkingal.enhanced.gametest.IronCoreTests.suited;
import static com.kolodkingal.enhanced.gametest.IronCoreTests.until;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * 3.8.2, Eilon's fix: Wanda's Chaos Shield stops every beam and blast of Iron Man, as it stops Thor's beam. Wanda
 * stands with her shield up and Iron Man fires at her from outside it: the classic repulsor and the Unibeam (Mark 3),
 * the Laser Burst (Mark 5), the Mark 50's Unibeam and nano cannon, the Mark 85's repulsors, giant chest laser,
 * Lightning Refocuser and drones, and the Hulkbuster's heavy Unibeam. Each time she takes no damage, the beam ends on
 * the surface of the sphere (the point the server and the client both stop it at), nothing beyond it is hit, and the
 * shield pays what the hit would have done. Fired from inside the sphere, Iron Man's beams still never touch her.
 * Each test has a batch of its own (they need PvP on, and the tests of a batch run side by side).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ChaosShieldIronManTests {
    private static final double WANDA_X = 12.5;
    private static final double WANDA_Z = 10.5;
    private static final float FULL = 30.0F;

    // ---- helpers ----

    /** Wanda (the Scarlet Witch) in the middle of the arena, PvP on, her shield's strength 30 and limited. */
    private static ServerPlayer wanda(TestKit.Fixture f) {
        f.pvp(true);
        return f.player(WANDA_X, FLOOR, WANDA_Z, Power.SCARLET_WITCH);
    }

    /** Presses B; when the shield is complete runs {@code then}. */
    private static void raise(TestKit.Fixture f, ServerPlayer wanda, Runnable then) {
        AbilityHandler.onKey(wanda, 4, true);
        check(Powers.get(wanda).chaosShieldActive(), "B starts the Chaos Shield");
        f.helper().runAfterDelay(EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get() + 1, () -> {
            check(WandaAbilities.hasShieldUp(wanda), "the Chaos Shield is complete");
            assertNear(strength(wanda), FULL, 1.0E-4, "at full strength");
            then.run();
        });
    }

    private static float strength(ServerPlayer wanda) {
        return Powers.get(wanda).shieldStrength();
    }

    private static Vec3 centre(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0);
    }

    /** She is not hurt at all, and still holds her shield. */
    private static void untouched(ServerPlayer wanda, String what) {
        assertNear(wanda.getMaxHealth() - wanda.getHealth(), 0.0, 1.0E-4, what + " doesn't hurt Wanda");
        check(WandaAbilities.hasShieldUp(wanda), what + ": the shield still stands");
    }

    /** {@code end} lies on the surface of her shield, on the side of {@code from}. */
    private static void onTheShield(ServerPlayer wanda, Vec3 end, Vec3 from, String what) {
        Vec3 c = centre(wanda);
        assertNear(end.distanceTo(c), WandaAbilities.SHIELD_RADIUS, 1.0E-3, what + " ends on the shield's surface");
        check(end.distanceTo(from) < c.distanceTo(from), what + " ends on the near side of the shield");
    }

    /** A husk with no armour and 200 health that stands where it is put. */
    private static Husk husk(GameTestHelper helper, double x, double z) {
        return NanoTechTests.husk(helper, x, z);
    }

    // ---- the classic suits ----

    /** Mark 3, R: the repulsor's ray ends on the shield; no damage, no boom there, the shield pays its 6. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_repulsor_pvp", timeoutTicks = 100)
    public static void theChaosShieldStopsTheClassicRepulsor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = suited(f, 3.5, WANDA_Z, SuitKind.MARK_3);
        Husk behind = husk(helper, 16.5, WANDA_Z);
        // Beside the spot where the ray meets the shield (outside it): a boom there would splash it.
        Husk beside = husk(helper, 10.5, 11.6);
        raise(f, wanda, () -> {
            lookAt(tony, wanda);
            IronMarkTests.press(tony, 1, false);
            untouched(wanda, "the repulsor");
            float damage = (float) IronManConfig.MARK3_REPULSOR_DAMAGE.get().doubleValue();
            assertNear(FULL - strength(wanda), damage, 1.0E-3, "the shield pays the repulsor's damage");
            check(IronMarkTests.lost(behind) == 0.0F, "nothing beyond the shield is hit");
            check(IronMarkTests.lost(beside) == 0.0F, "and no boom goes off at the shield");
            NanoTech.Ray ray = NanoTech.ray(tony, 32, e -> e instanceof LivingEntity
                    && ContinuousBeam.targets(tony).test(e));
            check(ray.blocked() && ray.wall().owner() == wanda && ray.target() == null, "the ray runs into her shield");
            onTheShield(wanda, ray.end(), tony.getEyePosition(), "the repulsor's ray");
            helper.succeed();
        });
    }

    /** Mark 3, V: the Unibeam ends on the shield and wears it down 4 every 10 ticks; she is never hurt. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_unibeam_pvp", timeoutTicks = 100)
    public static void theChaosShieldStopsTheUnibeam(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = suited(f, 3.5, WANDA_Z, SuitKind.MARK_3);
        Husk behind = husk(helper, 16.5, WANDA_Z);
        raise(f, wanda, () -> {
            lookAt(tony, wanda);
            IronMarkTests.down(tony, 3, false);
            check(Powers.get(tony).is(Live.BEAMING), "the Unibeam burns");
            step(helper, 25, () -> {
                untouched(wanda, "the Unibeam");
                float damage = (float) IronManConfig.MARK3_UNIBEAM_DAMAGE.get().doubleValue();
                assertNear(FULL - strength(wanda), 3 * damage, 1.0E-3, "three hits of the Unibeam, paid by the shield");
                check(IronMarkTests.lost(behind) == 0.0F, "nothing beyond the shield is hit");
                // Where the beam is drawn to (the client's own call: the crosshair, stopped at a wall).
                Vec3 crosshair = Raycasts.any(tony, 24, ContinuousBeam.targets(tony), 1.0F).getLocation();
                WandaAbilities.ShieldStop stop = WandaAbilities.shieldAcross(helper.getLevel(), tony,
                        tony.getEyePosition(), crosshair, 1.0F);
                check(stop != null && stop.owner() == wanda, "the Unibeam runs into her shield");
                onTheShield(wanda, stop.point(), tony.getEyePosition(), "the Unibeam");
                IronMarkTests.up(tony, 3);
                helper.succeed();
            }, tony);
        });
    }

    /** Mark 5, N: the Laser Burst's lasers end on the shield, for her and for the husk inside it. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_laser_burst_pvp", timeoutTicks = 100)
    public static void theChaosShieldStopsTheLaserBurst(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = suited(f, 8.5, WANDA_Z, SuitKind.MARK_5);
        Husk inside = husk(helper, WANDA_X, 11.7);
        Husk outside = husk(helper, 4.5, WANDA_Z);
        raise(f, wanda, () -> {
            check(centre(inside).distanceTo(centre(wanda)) < WandaAbilities.SHIELD_RADIUS,
                    "sanity: a husk in the shield");
            IronMarkTests.press(tony, 5, false);
            untouched(wanda, "the Laser Burst");
            float damage = (float) IronManConfig.MARK5_LASER_BURST_DAMAGE.get().doubleValue();
            check(IronMarkTests.lost(inside) == 0.0F, "the husk inside her shield is not cut");
            assertNear(IronMarkTests.lost(outside), damage, 1.0E-3, "the husk outside it is");
            assertNear(FULL - strength(wanda), 2 * damage, 1.0E-3, "the shield pays both lasers that met it");
            Vec3 from = tony.position().add(0, tony.getBbHeight() * 0.65, 0);
            WandaAbilities.ShieldStop stop = NanoTech.wallAcross(tony, from, wanda.position().add(0,
                    wanda.getBbHeight() * 0.6, 0));
            check(stop != null && stop.owner() == wanda, "the laser runs into her shield");
            onTheShield(wanda, stop.point(), from, "the laser");
            helper.succeed();
        });
    }

    /** From inside her shield Iron Man's beams are not stopped at the sphere, but never touch her either. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_point_blank_pvp", timeoutTicks = 100)
    public static void ironMansBeamsFromInsideTheShieldStillNeverHurtHer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = suited(f, WANDA_X - 1.3, WANDA_Z, SuitKind.MARK_5);
        raise(f, wanda, () -> {
            check(tony.position().add(0, tony.getBbHeight() * 0.65, 0).distanceTo(centre(wanda))
                    < WandaAbilities.SHIELD_RADIUS, "sanity: Iron Man stands inside her shield");
            IronMarkTests.press(tony, 5, false);
            untouched(wanda, "the Laser Burst from inside the shield");
            float damage = (float) IronManConfig.MARK5_LASER_BURST_DAMAGE.get().doubleValue();
            assertNear(FULL - strength(wanda), damage, 1.0E-3, "and the shield pays it");
            // Every damage type of his beams is one the shield stops on her.
            for (var type : List.of(ModDamageTypes.REPULSOR, ModDamageTypes.UNIBEAM, ModDamageTypes.DRONE_LASER,
                    ModDamageTypes.REFOCUSER_LIGHTNING)) {
                float before = strength(wanda);
                wanda.invulnerableTime = 0;
                wanda.hurt(ModDamageTypes.source(helper.getLevel(), type, tony, tony), 2.0F);
                untouched(wanda, type.location().getPath());
                assertNear(before - strength(wanda), 2.0, 1.0E-3, type.location().getPath() + " wears the shield");
            }
            helper.succeed();
        });
    }

    // ---- the nanotech suits ----

    /** Mark 85, R: both palms' repulsors end on the shield; the shield pays 9 for each. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_nano_repulsor_pvp", timeoutTicks = 100)
    public static void theChaosShieldStopsTheNanoRepulsors(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = NanoTechTests.deployed(f, 3.5, WANDA_Z, SuitKind.MARK_85);
        Husk behind = husk(helper, 16.5, WANDA_Z);
        raise(f, wanda, () -> {
            lookAt(tony, wanda);
            NanoTechTests.press(tony, 1, false);
            float damage = (float) IronManConfig.MARK85_REPULSOR_DAMAGE.get().doubleValue();
            untouched(wanda, "the first repulsor");
            assertNear(FULL - strength(wanda), damage, 1.0E-3, "the shield pays the first blast");
            NanoTech.Ray ray = NanoTech.ray(tony, 32);
            check(ray.blocked() && ray.wall().owner() == wanda, "the ray runs into her shield");
            onTheShield(wanda, ray.end(), tony.getEyePosition(), "the repulsor's ray");
            step(helper, 6, () -> {
                untouched(wanda, "the second repulsor");
                assertNear(FULL - strength(wanda), 2 * damage, 1.0E-3, "and the second");
                check(NanoTechTests.lost(behind) == 0.0F, "nothing beyond the shield is hit");
                helper.succeed();
            }, tony);
        });
    }

    /** Mark 85, V: the giant chest laser ends on the shield, and does not go through to the husk behind her. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_chest_laser_pvp", timeoutTicks = 100)
    public static void theChaosShieldStopsTheChestLaser(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = NanoTechTests.deployed(f, 3.5, WANDA_Z, SuitKind.MARK_85);
        Husk before = husk(helper, 7.5, WANDA_Z + 0.4);
        Husk behind = husk(helper, 16.5, WANDA_Z);
        raise(f, wanda, () -> {
            lookAt(tony, wanda);
            NanoTechTests.hold(tony, 3, false);
            check(Powers.get(tony).is(Live.BEAMING), "the laser burns");
            step(helper, 2, () -> {
                float damage = (float) IronManConfig.MARK85_LASER_DAMAGE.get().doubleValue();
                untouched(wanda, "the chest laser");
                assertNear(NanoTechTests.lost(before), damage, 1.0E-3, "the husk before the shield is burned");
                check(NanoTechTests.lost(behind) == 0.0F, "the husk behind it is not");
                assertNear(FULL - strength(wanda), damage, 1.0E-3, "the shield pays the laser");
                // Where it is drawn to (the client's call) and where the server stopped it.
                Vec3 from = NanoTech.chest(tony);
                Vec3 aim = tony.getEyePosition().add(tony.getLookAngle().scale(32));
                onTheShield(wanda, NanoBeams.reach(helper.getLevel(), tony, from, aim, 1.0F), from, "the chest laser");
                NanoTechTests.release(tony, 3);
                helper.succeed();
            }, tony);
        });
    }

    /** Mark 50, V: its Unibeam ends on the shield too. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_mark50_unibeam_pvp", timeoutTicks = 100)
    public static void theChaosShieldStopsTheMark50Unibeam(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = NanoTechTests.deployed(f, 3.5, WANDA_Z, SuitKind.MARK_50);
        Husk behind = husk(helper, 16.5, WANDA_Z);
        raise(f, wanda, () -> {
            lookAt(tony, wanda);
            NanoTechTests.hold(tony, 3, false);
            check(Powers.get(tony).is(Live.BEAMING), "the Unibeam burns");
            step(helper, 15, () -> {
                float damage = (float) IronManConfig.MARK50_UNIBEAM_DAMAGE.get().doubleValue();
                untouched(wanda, "the Mark 50's Unibeam");
                assertNear(FULL - strength(wanda), 2 * damage, 1.0E-3, "two hits, paid by the shield");
                check(NanoTechTests.lost(behind) == 0.0F, "nothing beyond the shield is hit");
                NanoTechTests.release(tony, 3);
                helper.succeed();
            }, tony);
        });
    }

    /** Mark 50, N held and let go: the nano cannon's blast ends on the shield, with no boom there. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_cannon_pvp", timeoutTicks = 100)
    public static void theChaosShieldStopsTheNanoCannon(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = NanoTechTests.deployed(f, 3.5, WANDA_Z, SuitKind.MARK_50);
        Husk beside = husk(helper, 10.5, 11.8);
        raise(f, wanda, () -> {
            lookAt(tony, wanda);
            NanoTechTests.hold(tony, 5, false);
            step(helper, 22, () -> {
                lookAt(tony, wanda);
                NanoTechTests.release(tony, 5);
                untouched(wanda, "the nano cannon");
                float damage = (float) IronManConfig.MARK50_CANNON_DAMAGE.get().doubleValue();
                assertNear(FULL - strength(wanda), damage, 1.0E-3, "the shield pays the cannon's blast");
                check(NanoTechTests.lost(beside) == 0.0F, "no boom goes off at the shield");
                helper.succeed();
            }, tony);
        });
    }

    /**
     * Mark 85, Sneak + B: aimed at her, the Refocuser's bolt strikes the shield and the chain ends there; aimed at a
     * husk outside it, the chain leaps to her and stops at the shield (the husk behind her is never reached).
     */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_refocuser_pvp", timeoutTicks = 100)
    public static void theChaosShieldStopsTheRefocusersLightning(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = NanoTechTests.deployed(f, 3.5, WANDA_Z, SuitKind.MARK_85);
        Husk behind = husk(helper, 15.5, WANDA_Z);
        raise(f, wanda, () -> {
            lookAt(tony, wanda);
            NanoTechTests.press(tony, 4, true);
            float damage = (float) IronManConfig.MARK85_REFOCUSER_DAMAGE.get().doubleValue();
            untouched(wanda, "the Refocuser's bolt");
            assertNear(FULL - strength(wanda), damage, 1.0E-3, "the shield pays the bolt");
            check(NanoTechTests.lost(behind) == 0.0F, "the chain does not leap past the shield");
            check(SuitEnergy.get(tony) < 100.0F, "the bolt was fired (its energy spent)");

            // Aimed at a husk outside the shield: the chain leaps from it to her, and stops at the sphere.
            Husk near = husk(helper, 9.0, WANDA_Z);
            NanoTechTests.ready(tony);
            SuitEnergy.set(tony, 100.0);
            lookAt(tony, near);
            NanoTechTests.press(tony, 4, true);
            assertNear(NanoTechTests.lost(near), damage, 1.0E-3, "the husk is struck");
            untouched(wanda, "the chain's next link");
            assertNear(FULL - strength(wanda), damage + damage * 0.8, 1.0E-3, "the shield pays the second link");
            check(NanoTechTests.lost(behind) == 0.0F, "and the chain ends there");
            WandaAbilities.ShieldStop stop = NanoTech.wallAcross(tony, near.position().add(0,
                    near.getBbHeight() * 0.6, 0), wanda.position().add(0, wanda.getBbHeight() * 0.6, 0));
            check(stop != null && stop.owner() == wanda, "the link runs into her shield");
            onTheShield(wanda, stop.point(), near.position(), "the link");
            helper.succeed();
        });
    }

    /** Mark 85, Sneak + N: the drones shoot at Wanda, who struck him; their lasers end on the shield. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_drones_pvp", timeoutTicks = 160)
    public static void theChaosShieldStopsTheDronesLasers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        ServerPlayer tony = NanoTechTests.deployed(f, 6.5, WANDA_Z, SuitKind.MARK_85);
        raise(f, wanda, () -> {
            tony.setLastHurtByMob(wanda);
            NanoTechTests.press(tony, 5, true);
            List<NanoDroneEntity> drones = NanoDrones.of(tony);
            check(drones.size() == 2, "two drones: " + drones.size());
            until(helper, 60, () -> strength(wanda) < FULL, () -> {
                float damage = (float) IronManConfig.MARK85_DRONE_DAMAGE.get().doubleValue();
                untouched(wanda, "the drones' lasers");
                float paid = FULL - strength(wanda);
                check(paid >= damage - 1.0E-3F && Math.abs(paid / damage - Math.round(paid / damage)) < 1.0E-3,
                        "each laser costs the shield a shot: " + paid);
                NanoDroneEntity drone = drones.get(0);
                Vec3 from = drone.muzzle(tony, 0);
                WandaAbilities.ShieldStop stop = NanoTech.wallAcross(tony, from, centre(wanda));
                check(stop != null && stop.owner() == wanda, "the laser runs into her shield");
                onTheShield(wanda, stop.point(), from, "the drone's laser");
                helper.succeed();
            }, tony);
        });
    }

    // ---- the Hulkbuster ----

    /** The Hulkbuster, V: the heavy Unibeam ends on the shield, and does not go through to the husk behind her. */
    @GameTest(template = ARENA, batch = "chaos_shield_iron_hulkbuster_pvp", timeoutTicks = 120)
    public static void theChaosShieldStopsTheHulkbustersUnibeam(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = wanda(f);
        Husk behind = husk(helper, 17.5, WANDA_Z);
        HulkbusterTests.hulkbuster(helper, f, 2.5, WANDA_Z, tony -> raise(f, wanda, () -> {
            lookAt(tony, wanda);
            AbilityHandler.onKey(tony, 3, true);
            check(Powers.get(tony).is(Live.BEAMING), "the heavy Unibeam burns");
            step(helper, 2, () -> {
                float damage = (float) IronManConfig.HULKBUSTER_UNIBEAM_DAMAGE.get().doubleValue();
                untouched(wanda, "the heavy Unibeam");
                check(NanoTechTests.lost(behind) == 0.0F, "the husk behind her is not burned");
                assertNear(FULL - strength(wanda), damage, 1.0E-3, "the shield pays the beam");
                Vec3 from = NanoTech.chest(tony);
                Vec3 aim = tony.getEyePosition().add(tony.getLookAngle().scale(32));
                onTheShield(wanda, NanoBeams.reach(helper.getLevel(), tony, from, aim, 1.0F), from,
                        "the heavy Unibeam");
                AbilityHandler.onKey(tony, 3, false);
                helper.succeed();
            }, tony);
        }));
    }

    private ChaosShieldIronManTests() {
    }
}

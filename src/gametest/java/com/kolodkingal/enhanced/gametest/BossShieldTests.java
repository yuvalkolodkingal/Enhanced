package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The refactor of Wanda's Chaos Shield that the bosses build on ({@code neutralize}, {@code disintegrate} with a
 * colour, the {@link ShieldCost} interface): the shield does for Mjolnir, Stormbreaker and arrows exactly what it did,
 * and does for a projectile that says what it costs, and whether it is one of a kind, what it says.
 */
public final class BossShieldTests {

    /** A bolt that wears the shield by 7 and is destroyed by it. */
    private static final class Bolt extends Snowball implements ShieldCost {
        Bolt(Level level) {
            super(EntityType.SNOWBALL, level);
        }

        @Override
        public float shieldCost() {
            return 7.0F;
        }
    }

    /** A sword that wears the shield by 5 and is one of a kind: the shield turns it back instead. */
    private static final class Sword extends Snowball implements ShieldCost {
        boolean home;

        Sword(Level level) {
            super(EntityType.SNOWBALL, level);
        }

        @Override
        public float shieldCost() {
            return 5.0F;
        }

        @Override
        public boolean oneOfAKind() {
            return true;
        }

        @Override
        public void turnHome() {
            home = true;
        }

        @Override
        public boolean returning() {
            return home;
        }
    }

    /**
     * The pin of the refactor: the Chaos Shield still throws Mjolnir back to Thor, and still pays what the throw would
     * have done.
     */
    @EnhancedTest(template = ARENA, batch = "boss_shield_mjolnir", timeoutTicks = 200)
    public static void chaosShieldStillTurnsMjolnirHome(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(thor);
        AbilityHandler.onKey(wanda, 4, true);
        boolean[] thrown = {false};
        f.everyTick(100, tick -> {
            PowerTicker.tick(thor);
            PowerTicker.tick(wanda);
            if (!thrown[0]) {
                if (WandaAbilities.hasShieldUp(wanda)) {
                    lookAt(thor, wanda);
                    AbilityHandler.onKey(thor, 1, true);
                    AbilityHandler.onKey(thor, 1, false);
                    check(thor.getMainHandItem().isEmpty(), "Mjolnir is in flight");
                    thrown[0] = true;
                }
                return false;
            }
            if (thor.getMainHandItem().is(ModItems.MJOLNIR.get())) {
                assertNear(wanda.getHealth(), 20.0, 1.0E-4, "the hammer never touched her");
                assertNear(Powers.get(wanda).shieldStrength(), 30.0 - EnhancedConfig.MJOLNIR_THROW_DAMAGE.get(), 1.0E-4,
                        "the shield paid what the throw would have done");
                check(Powers.get(wanda).chaosShieldActive(), "and held");
                check(helper.getEntities(ModEntities.THROWN_WEAPON.get()).isEmpty(), "nothing is left in flight");
                return true;
            }
            check(tick < 90, "Mjolnir was destroyed by the shield instead of coming back to Thor");
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "boss_shield_neutralize")
    public static void neutralizeTurnsAWeaponHomeAndDisintegratesTheRestInTheColourItIsGiven(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer thor = f.player(4.5, FLOOR, 10.5, Power.THOR);
        ServerPlayer watcher = f.player(8.5, FLOOR, 10.5);
        Vec3 at = helper.absoluteVec(new Vec3(8.5, FLOOR + 1.0, 10.5));

        ThrownWeaponEntity hammer = new ThrownWeaponEntity(level, thor, new ItemStack(ModItems.MJOLNIR.get()));
        hammer.moveTo(at);
        level.addFreshEntity(hammer);
        TestKit.sentPackets(watcher);
        WandaAbilities.neutralize(level, hammer);
        check(hammer.isReturning() && !hammer.isRemoved(), "Mjolnir is turned home, never destroyed");
        check(particleColours(watcher).contains(Power.SCARLET_WITCH.color), "in the Scarlet Witch's red");

        Snowball snowball = new Snowball(level, thor);
        snowball.moveTo(at);
        level.addFreshEntity(snowball);
        TestKit.sentPackets(watcher);
        WandaAbilities.neutralize(level, snowball, 0xF2B84B);
        check(snowball.isRemoved(), "a snowball is destroyed");
        List<Integer> colours = particleColours(watcher);
        check(colours.contains(0xF2B84B) && !colours.contains(Power.SCARLET_WITCH.color), "in the colour of the shield that stopped it");
        hammer.discard();
        helper.succeed();
    }

    private static List<Integer> particleColours(ServerPlayer player) {
        return TestKit.particlesIn(TestKit.sentPackets(player)).stream().map(ClientboundLevelParticlesPacket::getParticle)
                .filter(EnergyParticleOptions.class::isInstance).map(particle -> ((EnergyParticleOptions) particle).color()).toList();
    }

    @EnhancedTest(template = ARENA, batch = "boss_shield_cost")
    public static void aProjectileSaysWhatItCostsTheShieldAndWhetherItIsOneOfAKind(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer shooter = f.player(3.5, FLOOR, 10.5);
        Vec3 near = helper.absoluteVec(new Vec3(11.5, FLOOR + 1.0, 10.5));
        PowerData data = Powers.get(wanda);

        Bolt bolt = new Bolt(level);
        bolt.setOwner(shooter);
        bolt.moveTo(near);
        level.addFreshEntity(bolt);
        data.raiseChaosShield(level.getGameTime(), 0, 30.0F);
        WandaAbilities.block(wanda, bolt);
        check(bolt.isRemoved(), "a bolt is destroyed");
        assertNear(data.shieldStrength(), 23.0, 1.0E-4, "and costs what it says: 7");

        Sword sword = new Sword(level);
        sword.setOwner(shooter);
        sword.moveTo(near);
        level.addFreshEntity(sword);
        WandaAbilities.block(wanda, sword);
        check(!sword.isRemoved() && sword.home, "a sword is turned back toward whoever sent it, not destroyed");
        assertNear(data.shieldStrength(), 18.0, 1.0E-4, "and costs 5");

        // It is on its way home now: the shield's own scan lets it by, for nothing.
        PowerTicker.tick(wanda);
        assertNear(data.shieldStrength(), 18.0, 1.0E-4, "a sword on its way home costs the shield nothing");
        sword.home = false;
        sword.setDeltaMovement(wanda.position().subtract(sword.position()).normalize().scale(0.5));
        PowerTicker.tick(wanda);
        check(sword.home && !sword.isRemoved(), "one that is not is turned home again by the scan");
        assertNear(data.shieldStrength(), 13.0, 1.0E-4, "for 5 more");
        sword.discard();
        data.setChaosShieldActive(false);
        helper.succeed();
    }

    private BossShieldTests() {
    }
}

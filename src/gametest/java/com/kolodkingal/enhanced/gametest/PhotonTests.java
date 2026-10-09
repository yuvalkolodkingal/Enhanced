package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.entity.PhotonBlastEntity;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/** Photon, the Monica Rambeau effect. */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class PhotonTests {

    @GameTest(template = ARENA)
    public static void photonAbsorbsHalfOfFireDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer photon = f.player(5.5, FLOOR, 10.5, Power.PHOTON);
        ServerPlayer normal = f.player(15.5, FLOOR, 10.5);

        photon.hurt(helper.getLevel().damageSources().inFire(), 4.0F);
        normal.hurt(helper.getLevel().damageSources().inFire(), 4.0F);

        assertNear(20.0F - normal.getHealth(), 4.0, 1.0E-4, "fire damage on a normal player");
        assertNear(20.0F - photon.getHealth(), 2.0, 1.0E-4, "Photon absorbs half of the energy damage");
        assertNear(Powers.get(photon).energy(), 4.0, 1.0E-4, "and stores it (2 energy per absorbed point)");

        // Ordinary damage is not energy and is not absorbed.
        float health = photon.getHealth();
        photon.invulnerableTime = 0;
        photon.hurt(helper.getLevel().damageSources().generic(), 3.0F);
        assertNear(health - photon.getHealth(), 3.0, 1.0E-4, "non-energy damage is taken in full");
        assertNear(Powers.get(photon).energy(), 4.0, 1.0E-4, "non-energy damage stores nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void photonPhaseShiftCancelsDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer photon = f.player(5.5, FLOOR, 10.5, Power.PHOTON);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(7.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));

        AbilityHandler.onKey(photon, 4, true);
        check(Powers.get(photon).isPhased(), "B starts Phase Shift");
        photon.hurt(helper.getLevel().damageSources().mobAttack(zombie), 6.0F);
        photon.hurt(helper.getLevel().damageSources().generic(), 6.0F);
        check(photon.getHealth() == 20.0F, "phased: no damage, got health " + photon.getHealth());
        float zombieHealth = zombie.getHealth();
        photon.attack(zombie);
        check(zombie.getHealth() == zombieHealth, "a phased Photon can't attack");

        // It wears off after 3 seconds.
        for (int i = 0; i < 60; i++) {
            PowerTicker.tick(photon);
        }
        check(!Powers.get(photon).isPhased(), "Phase Shift ends after 60 ticks");
        photon.hurt(helper.getLevel().damageSources().generic(), 6.0F);
        check(photon.getHealth() < 20.0F, "solid again: damage applies");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void photonEnergyReleaseNeedsTenEnergy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer photon = f.player(8.5, FLOOR, 10.5, Power.PHOTON);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        PowerData data = Powers.get(photon);
        float zombieMax = zombie.getHealth();

        data.setEnergy(5.0F);
        AbilityHandler.onKey(photon, 3, true);
        assertNear(data.energy(), 5.0, 1.0E-4, "too little energy: nothing released");
        check(zombie.getHealth() == zombieMax, "no shockwave below 10 energy");
        check(data.isReady(3, helper.getLevel().getGameTime()), "no cooldown for a failed release");

        data.setEnergy(40.0F);
        AbilityHandler.onKey(photon, 3, true);
        assertNear(data.energy(), 0.0, 1.0E-4, "the release uses all stored energy");
        check(zombie.getHealth() < zombieMax, "the shockwave hurts creatures within 6 blocks");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void photonSpectrumVisionTogglesNightVision(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer photon = f.player(5.5, FLOOR, 10.5, Power.PHOTON);
        ServerPlayer potion = f.player(15.5, FLOOR, 10.5, Power.PHOTON);

        AbilityHandler.onKey(photon, 5, true);
        PowerTicker.tick(photon);
        check(Powers.get(photon).spectrumVision() && photon.hasEffect(MobEffects.NIGHT_VISION), "N turns Spectrum Vision on");
        AbilityHandler.onKey(photon, 5, true);
        PowerTicker.tick(photon);
        check(!photon.hasEffect(MobEffects.NIGHT_VISION), "N again turns it off");

        // A real Night Vision potion survives turning the vision on and off.
        potion.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600));
        AbilityHandler.onKey(potion, 5, true);
        PowerTicker.tick(potion);
        AbilityHandler.onKey(potion, 5, true);
        PowerTicker.tick(potion);
        check(potion.hasEffect(MobEffects.NIGHT_VISION) && potion.getEffect(MobEffects.NIGHT_VISION).getDuration() > 3000,
                "the potion's Night Vision is kept");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void photonBlastSuperchargesWithEnergy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer photon = f.player(3.5, FLOOR, 10.5, Power.PHOTON);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        lookAt(photon, zombie);
        PowerData data = Powers.get(photon);
        data.setEnergy(15.0F);

        AbilityHandler.onKey(photon, 1, true);
        List<PhotonBlastEntity> bolts = helper.getEntities(ModEntities.PHOTON_BLAST.get());
        check(bolts.size() == 1, "R fires one bolt");
        assertNear(data.energy(), 5.0, 1.0E-4, "a supercharged bolt costs 10 energy");
        AbilityHandler.onKey(photon, 1, true);
        check(helper.getEntities(ModEntities.PHOTON_BLAST.get()).size() == 1, "0.6 s cooldown between bolts");

        float max = zombie.getHealth();
        f.everyTick(40, tick -> !zombie.isAlive() || zombie.getHealth() < max);
    }

    private PhotonTests() {
    }
}

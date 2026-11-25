package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.captain.HydraShield;
import com.kolodkingal.enhanced.captain.RedSkull;
import com.kolodkingal.enhanced.hydra.HydraBoltEntity;
import com.kolodkingal.enhanced.hydra.HydraLoyalty;
import com.kolodkingal.enhanced.hydra.HydraSoldierEntity;
import com.kolodkingal.enhanced.hydra.Rank;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Characters;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * (3.19) Red Skull and Captain Hydra, heroes of their own: chosen like any hero (no longer earned); their shield is the
 * Hydra Shield; B is "Hail Hydra", whom Hydra's soldiers answer, and Hydra never take them for intruders; Red Skull has his
 * own powers on R, G and V (the Tesseract's blast and jump, the Dust of Death, his Luger).
 */
public final class HydraHeroTests {
    private static final String BATCH = "hydra_heroes";

    private static void press(ServerPlayer player, int key, boolean sneak) {
        AbilityHandler.onKey(player, key, true, sneak);
        AbilityHandler.onKey(player, key, false, sneak);
    }

    private static ServerPlayer hero(TestKit.Fixture f, double x, double z, String look) {
        ServerPlayer player = f.player(x, FLOOR, z, Power.NONE);
        Powers.awaken(player, Power.CAPTAIN_AMERICA, look, false);
        PowerTicker.tick(player);
        return player;
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theyAreChosenLikeAnyHero(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer skull = hero(f, 6.5, 6.5, "red_skull");
        ServerPlayer hydra = hero(f, 12.5, 12.5, "captain_hydra");
        check(Characters.is(Powers.get(skull), Characters.RED_SKULL), "Red Skull, with nothing earned");
        check(Characters.is(Powers.get(hydra), Characters.CAPTAIN_HYDRA), "Captain Hydra, with nothing earned");
        check(Characters.playable(Characters.RED_SKULL) != null && Characters.playable(Characters.CAPTAIN_HYDRA) != null,
                "both have their own card and command name");
        check(Characters.characterLook(Power.CAPTAIN_AMERICA, "captain_hydra") && !Characters.characterLook(Power.CAPTAIN_AMERICA, "classic"),
                "their looks are theirs, not Cap's");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theirShieldIsHydrasAndCapsIsHis(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, "captain_hydra");
        ItemStack shield = new ItemStack(ModItems.VIBRANIUM_SHIELD.get());
        player.getInventory().add(shield);
        player.tickCount = 20;
        HydraShield.tick(player);
        ItemStack held = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.VIBRANIUM_SHIELD.get())) {
                held = player.getInventory().getItem(i);
            }
        }
        check(HydraShield.is(held), "Captain Hydra's shield is the Hydra Shield");
        Powers.setVariant(player, com.kolodkingal.enhanced.power.Variants.get(Power.CAPTAIN_AMERICA, "classic"));
        player.tickCount = 30;
        HydraShield.tick(player);
        check(!HydraShield.is(held), "Captain America's is his own again");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hailHydraAndHydraSaluteThem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 8.5, 8.5, "captain_hydra");
        HydraSoldierEntity soldier = ModEntities.HYDRA_SOLDIER.get().create(f.level());
        soldier.setRank(Rank.values()[0], 0);
        Vec3 at = helper.absoluteVec(new Vec3(11.5, FLOOR, 8.5));
        soldier.moveTo(at.x, at.y, at.z, 90.0F, 0.0F);
        soldier.setPersistenceRequired();
        f.level().addFreshEntity(soldier);
        check(HydraLoyalty.sameSide(soldier, player) && soldier.isAlliedTo(player), "Hydra's soldiers are on his side");
        press(player, 4, false);
        check(soldier.hasEffect(MobEffects.DAMAGE_BOOST), "Hail Hydra: the soldier answers his call");
        check(player.hasEffect(MobEffects.DAMAGE_BOOST), "and he too");
        ServerPlayer cap = hero(f, 3.5, 3.5, "classic");
        check(!HydraLoyalty.sameSide(soldier, cap), "Captain America is no friend of theirs");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void redSkullsOwnPowers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 4.5, 10.5, "red_skull");
        PowerData data = Powers.get(player);
        check(RedSkull.is(data), "he is Red Skull");
        // (not an undead: the dust's poison and wither don't take on those, its harm does)
        Pillager husk = TestKit.spawnWithNoFreeWill(helper, EntityType.PILLAGER, new Vec3(7.5, FLOOR, 10.5));
        TestKit.lookAt(player, husk);
        AABB around = player.getBoundingBox().inflate(4.0);
        press(player, 1, false);
        check(!f.level().getEntitiesOfClass(HydraBoltEntity.class, around, b -> b.kind() == HydraBoltEntity.SNIPER).isEmpty(),
                "R: a blast of the Tesseract");
        press(player, 1, true);
        check(!f.level().getEntitiesOfClass(HydraBoltEntity.class, around, b -> b.kind() == HydraBoltEntity.PISTOL).isEmpty(),
                "Sneak + R: his Luger");
        press(player, 2, false);
        check(husk.hasEffect(MobEffects.WITHER) && husk.hasEffect(MobEffects.POISON), "G: the Dust of Death withers and poisons");
        Vec3 before = player.position();
        TestKit.lookAt(player, helper.absoluteVec(new Vec3(4.5, FLOOR + 1.6, 18.5)));
        press(player, 3, false);
        check(player.position().distanceTo(before) > 3.0, "V: the Tesseract carries him: " + player.position().distanceTo(before));
        helper.succeed();
    }
}

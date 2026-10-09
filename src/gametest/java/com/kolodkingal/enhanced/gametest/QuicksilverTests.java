package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Quicksilver. Tests that start Quicksilver Time get their own batch and a small slow-motion radius, because the
 * slow motion would otherwise reach into the neighbouring test arenas.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class QuicksilverTests {

    /** Makes the player sprint and runs his tick, as a connected client's tick would. */
    private static void run(ServerPlayer player, boolean sprinting) {
        player.setSprinting(sprinting);
        PowerTicker.tick(player);
    }

    @GameTest(template = ARENA)
    public static void gogglesAwakenQuicksilver(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.QUICKSILVER_GOGGLES.get()));
        player.doTick();
        check(Powers.is(player, Power.QUICKSILVER), "wearing the goggles makes you Quicksilver");
        assertNear(player.getAttributeValue(Attributes.ARMOR), 2.0, 1.0E-6, "the goggles protect like an iron helmet");
        check(!player.mayFly(), "Quicksilver runs, he doesn't fly");

        // Someone with another power only gets a pair of goggles.
        ServerPlayer magneto = f.player(4.5, FLOOR, 4.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.QUICKSILVER_GOGGLES.get()));
        magneto.doTick();
        check(Powers.is(magneto, Power.MAGNETO), "powers never stack");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "quicksilver_speed")
    public static void sprintingStartsQuicksilverTime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 5);
        ServerPlayer peter = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        double walking = peter.getAttributeValue(Attributes.MOVEMENT_SPEED);

        run(peter, true);
        PowerData data = Powers.get(peter);
        check(data.slowMo(), "sprinting starts Quicksilver Time");
        double bonus = EnhancedConfig.QUICKSILVER_SPEED_BONUS.get();
        // Sprinting itself multiplies speed by 1.3; Quicksilver Time multiplies it again by (1 + bonus).
        assertNear(peter.getAttributeValue(Attributes.MOVEMENT_SPEED), walking * 1.3 * (1 + bonus), 1.0E-4, "super speed");
        check(peter.getAttributeValue(Attributes.STEP_HEIGHT) >= 1.2, "he runs up whole blocks");

        peter.fallDistance = 10;
        float health = peter.getHealth();
        peter.hurt(helper.getLevel().damageSources().fall(), 6.0F);
        assertNear(peter.getHealth(), health, 1.0E-4, "no fall damage while running");

        run(peter, false);
        check(!data.slowMo(), "stopping ends Quicksilver Time");
        assertNear(peter.getAttributeValue(Attributes.MOVEMENT_SPEED), walking, 1.0E-4, "normal speed again");

        // Ability 1 turns the slow motion off, so he can sprint normally.
        AbilityHandler.onKey(peter, 1, true);
        AbilityHandler.onKey(peter, 1, false);
        run(peter, true);
        check(!data.slowMo(), "Quicksilver Time can be switched off");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "quicksilver_slow_blocks", timeoutTicks = 100)
    public static void quicksilverTimeSlowsTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 6);
        ServerPlayer peter = f.player(2.5, FLOOR, 10.5, Power.QUICKSILVER);
        run(peter, true);

        // Two blocks of sand dropped from the same height: one next to him, one far outside the slow motion.
        Vec3 nearPos = helper.absoluteVec(new Vec3(5.5, 8.0, 10.5));
        Vec3 farPos = helper.absoluteVec(new Vec3(19.5, 8.0, 10.5));
        FallingBlockEntity near = fallingSand(helper, nearPos);
        FallingBlockEntity far = fallingSand(helper, farPos);
        // Another player standing next to him is slowed down instead.
        ServerPlayer friend = f.player(4.5, FLOOR, 12.5);

        f.everyTick(15, tick -> {
            run(peter, true);
            if (tick < 15) {
                return false;
            }
            double nearFell = nearPos.y - near.getY();
            double farFell = farPos.y - far.getY();
            check(farFell > 2.5, "the far block falls normally (fell " + farFell + ")");
            check(nearFell < farFell * 0.4, "the block next to Quicksilver falls in slow motion (fell " + nearFell + " vs " + farFell + ")");
            MobEffectInstance slowness = friend.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            check(slowness != null && slowness.getAmplifier() >= 3, "other players nearby are slowed down");
            return true;
        });
    }

    private static FallingBlockEntity fallingSand(GameTestHelper helper, Vec3 pos) {
        FallingBlockEntity sand = EntityType.FALLING_BLOCK.create(helper.getLevel());
        check(sand != null, "falling block");
        sand.moveTo(pos.x, pos.y, pos.z);
        sand.setNoGravity(false);
        sand.dropItem = false;
        helper.getLevel().addFreshEntity(sand);
        // FallingBlockEntity defaults to sand when created this way.
        return sand;
    }

    @GameTest(template = ARENA, batch = "quicksilver_arrows", timeoutTicks = 60)
    public static void arrowsHangInTheAir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 8);
        ServerPlayer peter = f.player(2.5, FLOOR, 10.5, Power.QUICKSILVER);
        run(peter, true);

        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        Vec3 start = helper.absoluteVec(new Vec3(4.5, 6.0, 4.5));
        arrow.moveTo(start.x, start.y, start.z);
        arrow.setDeltaMovement(0, 0, 1.0);
        double baseDamage = arrow.getBaseDamage();
        helper.getLevel().addFreshEntity(arrow);
        int factor = EnhancedConfig.SLOW_MOTION_FACTOR.get();

        f.everyTick(30, tick -> {
            if (tick <= 3) {
                run(peter, true);
                if (tick == 3) {
                    assertNear(arrow.getDeltaMovement().z, 1.0 / factor, 0.05, "the arrow crawls at 1/" + factor + " speed");
                    check(arrow.isNoGravity(), "a slowed arrow doesn't drop out of the air");
                    assertNear(arrow.getBaseDamage(), baseDamage * factor, 1.0E-6, "it still hits as hard as before");
                }
                return false;
            }
            run(peter, false);
            if (tick == 6) {
                check(arrow.getDeltaMovement().z > 0.7, "the arrow speeds up again when Quicksilver stops (" + arrow.getDeltaMovement().z + ")");
                check(!arrow.isNoGravity(), "and falls normally again");
                assertNear(arrow.getBaseDamage(), baseDamage, 1.0E-6, "and has its normal damage back");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA)
    public static void speedBlitzAndPickpocket(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(2.5, FLOOR, 10.5, Power.QUICKSILVER);

        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        lookAt(peter, zombie);

        // Ability 3: the sword is in his hands before the zombie knows it.
        AbilityHandler.onKey(peter, 3, true);
        check(zombie.getMainHandItem().isEmpty(), "Pickpocket empties the zombie's hand");
        check(peter.getInventory().contains(new ItemStack(Items.IRON_SWORD)), "and the sword is in Quicksilver's inventory");

        // Ability 2: next to the zombie in an instant, six fast punches.
        float health = zombie.getHealth();
        AbilityHandler.onKey(peter, 2, true);
        check(peter.distanceTo(zombie) < 2.5, "Speed Blitz puts him right next to the target (" + peter.distanceTo(zombie) + ")");
        check(health - zombie.getHealth() > 9.0F, "and lands every punch (took " + (health - zombie.getHealth()) + ")");
        check(!Powers.get(peter).isReady(2, helper.getLevel().getGameTime()), "then it needs to cool down");
        helper.succeed();
    }
}

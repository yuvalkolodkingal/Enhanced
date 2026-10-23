package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/** Magneto: the helmet, his suit's protection and his five abilities. */
public final class MagnetoTests {

    // ---- the helmet ----

    @EnhancedTest(template = ARENA)
    public static void helmetAwakensMagnetoAndProtects(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200));
        check(player.hasEffect(MobEffects.CONFUSION), "an ordinary player can get Nausea");

        ItemStack helmet = new ItemStack(ModItems.MAGNETO_HELMET.get());
        check(!helmet.isDamageableItem(), "the helmet must never take damage");
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.doTick(); // one full player tick, as for a connected player: equipment bonuses + the power ticker

        check(Powers.is(player, Power.MAGNETO), "equipping the helmet awakens Magneto, got " + Powers.power(player));
        assertNear(player.getAttributeValue(Attributes.ARMOR), 20.0, 1.0E-6, "armor (full Netherite set)");
        assertNear(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 12.0, 1.0E-6, "armor toughness (full Netherite set)");
        assertNear(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.4, 1.0E-6, "knockback resistance");
        check(!player.hasEffect(MobEffects.CONFUSION), "Nausea he already had is removed when the helmet goes on");

        boolean applied = player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200));
        check(!applied && !player.hasEffect(MobEffects.CONFUSION), "Nausea can't be applied while the helmet is worn");
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200));
        check(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the helmet only blocks Nausea, not every effect");

        // Taking the helmet off keeps the power (it is permanent) and removes the armor.
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.doTick();
        check(Powers.is(player, Power.MAGNETO), "Magneto's power is permanent");
        assertNear(player.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-6, "armor without the helmet");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "solo_chaos_beam_still_hurts_magneto_but_no_nausea")
    public static void chaosBeamStillHurtsMagnetoButNoNausea(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true); // the GameTest server has PvP off; real servers have it on
        ServerPlayer magneto = f.player(14.5, FLOOR, 10.5);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        magneto.doTick();
        check(Powers.is(magneto, Power.MAGNETO), "helmet awakens Magneto");

        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        lookAt(wanda, magneto);
        float before = magneto.getHealth();
        AbilityHandler.onKey(wanda, 1, true);
        check(Powers.get(wanda).beamActive(), "R starts the Chaos Beam");
        PowerTicker.tick(wanda);

        float taken = before - magneto.getHealth();
        // The newest concept: the helmet blocks the Nausea, but Magneto takes the beam's FULL damage (his armor doesn't soften it).
        assertNear(taken, EnhancedConfig.CHAOS_BEAM_DAMAGE.get().floatValue(), 0.01, "beam damage on Magneto (full, armor ignored)");
        check(!magneto.hasEffect(MobEffects.CONFUSION), "the helmet blocks the beam's Nausea");

        AbilityHandler.onKey(wanda, 1, false);
        check(!Powers.get(wanda).beamActive(), "releasing R stops the beam");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "solo_telekinetic_slam_from_wanda_ignores_magnetos_armor")
    public static void telekineticSlamFromWandaIgnoresMagnetosArmor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer magneto = f.player(14.5, FLOOR, 10.5);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        magneto.doTick();
        check(Powers.is(magneto, Power.MAGNETO), "helmet awakens Magneto");
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer other = f.player(8.5, FLOOR, 4.5, Power.MAGNETO);

        // Something Wanda hurled at him (a block, a creature, or Magneto himself thrown into a wall).
        float before = magneto.getHealth();
        magneto.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.TELEKINETIC_SLAM, wanda, wanda), 8.0F);
        assertNear(before - magneto.getHealth(), 8.0, 0.01, "Wanda's telekinesis hits Magneto in full (armor ignored)");

        // The same slam from anyone else is softened by his armor as usual.
        magneto.invulnerableTime = 0;
        before = magneto.getHealth();
        magneto.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.TELEKINETIC_SLAM, other, other), 8.0F);
        check(before - magneto.getHealth() < 7.0F, "other telekinetic slams are softened by his armor (took " + (before - magneto.getHealth()) + ")");
        helper.succeed();
    }

    // ---- Ability 3: Armor & Weapon Strip ----

    @EnhancedTest(template = ARENA)
    public static void disarmRipsMetalOffAZombie(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(4.5, FLOOR, 10.5, Power.MAGNETO);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        lookAt(magneto, zombie);

        AbilityHandler.onKey(magneto, 3, true);

        check(zombie.getMainHandItem().isEmpty(), "iron sword ripped out of the zombie's hand");
        check(zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "iron helmet ripped off");
        check(zombie.getItemBySlot(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE), "leather isn't metal and stays on");
        check(countItems(helper, Items.IRON_SWORD) == 1, "the sword drops as an item");
        check(countItems(helper, Items.IRON_HELMET) == 1, "the helmet drops as an item");
        check(!Powers.get(magneto).isReady(3, helper.getLevel().getGameTime()), "a successful strip starts the cooldown");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void disarmIgnoresCreaturesWithoutMetal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(4.5, FLOOR, 10.5, Power.MAGNETO);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        lookAt(magneto, zombie);

        AbilityHandler.onKey(magneto, 3, true);

        check(zombie.getMainHandItem().is(Items.WOODEN_SWORD), "wooden sword stays");
        check(zombie.getItemBySlot(EquipmentSlot.HEAD).is(Items.LEATHER_HELMET), "leather helmet stays");
        check(helper.getEntities(EntityType.ITEM).isEmpty(), "nothing drops");
        check(Powers.get(magneto).isReady(3, helper.getLevel().getGameTime()), "no cooldown when there was nothing to strip");
        helper.succeed();
    }

    // ---- Ability 2: Iron Shrapnel Storm ----

    @EnhancedTest(template = ARENA)
    public static void shrapnelMeltsIronIntoShards(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        ItemStack enchanted = new ItemStack(Items.IRON_SWORD);
        enchanted.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS), 1);
        magneto.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 2));
        magneto.getInventory().setItem(1, enchanted);
        PowerData data = Powers.get(magneto);

        AbilityHandler.onKey(magneto, 2, true);
        check(data.shrapnelActive(), "holding G starts the storm");

        fire(magneto);
        check(magneto.getInventory().getItem(0).getCount() == 1, "the first shard melts one iron ingot");
        check(data.ironCharge() == 8, "an ingot is worth 9 shards (1 fired, 8 left), got " + data.ironCharge());
        check(shards(helper) == 1, "one shard flies");

        for (int i = 0; i < 17; i++) {
            fire(magneto);
        }
        check(magneto.getInventory().getItem(0).isEmpty(), "2 ingots = 18 shards");
        check(data.ironCharge() == 0, "charge used up");
        check(shards(helper) == 18, "18 shards fired, got " + shards(helper));
        check(data.shrapnelActive(), "still firing");

        fire(magneto);
        check(!data.shrapnelActive(), "out of iron: the storm stops");
        check(shards(helper) == 18, "no shard without iron");
        check(magneto.getInventory().getItem(1).is(Items.IRON_SWORD) && magneto.getInventory().getItem(1).isEnchanted(),
                "enchanted iron gear is never melted");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void shrapnelStormHurtsTarget(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        magneto.getInventory().setItem(0, new ItemStack(Items.IRON_BLOCK));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no sunburn
        lookAt(magneto, zombie);
        AbilityHandler.onKey(magneto, 2, true);
        float max = zombie.getMaxHealth();
        f.everyTick(40, tick -> {
            lookAt(magneto, zombie);
            PowerTicker.tick(magneto);
            return max - zombie.getHealth() >= 10.0F || !zombie.isAlive();
        });
    }

    private static void fire(ServerPlayer magneto) {
        magneto.tickCount = 0; // the storm fires on every SHRAPNEL_FIRE_INTERVAL-th tick
        PowerTicker.tick(magneto);
    }

    private static int shards(GameTestHelper helper) {
        return helper.getEntities(ModEntities.IRON_SHRAPNEL.get()).size();
    }

    private static int countItems(GameTestHelper helper, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemEntity e : helper.getEntities(EntityType.ITEM)) {
            if (e.getItem().is(item)) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    // ---- Ability 1: Metal Telekinesis ----

    @EnhancedTest(template = ARENA)
    public static void metalTelekinesisOnlyLiftsMetal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(2.5, FLOOR, 10.5, Power.MAGNETO);
        Zombie plain = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 10.5));
        Zombie armored = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 17.5));
        armored.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(9.5, FLOOR, 3.5));
        PowerData data = Powers.get(magneto);

        lookAt(magneto, plain);
        AbilityHandler.onKey(magneto, 1, true);
        check(!Telekinesis.isHolding(magneto), "a zombie without metal armor can't be lifted by Magneto");

        lookAt(magneto, armored);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto) && data.heldEntityId() == armored.getId(), "a zombie in an iron chestplate can be lifted");
        magneto.setShiftKeyDown(true);
        AbilityHandler.onKey(magneto, 1, true); // Sneak + R: put it down gently
        magneto.setShiftKeyDown(false);
        check(!Telekinesis.isHolding(magneto) && data.heldEntityId() == -1, "Sneak + R lets go");

        lookAt(magneto, golem);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto) && data.heldEntityId() == golem.getId(), "Magneto controls Iron Golems");
        Telekinesis.release(magneto, false);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void heldGolemFollowsMagnetosGaze(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(2.5, FLOOR, 10.5, Power.MAGNETO);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(8.5, FLOOR, 10.5));
        lookAt(magneto, golem);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto), "grabbed the golem");
        double startY = golem.getY();
        f.everyTick(30, tick -> {
            TestKit.look(magneto, TestKit.EAST, -25.0F); // look up: the golem rises
            PowerTicker.tick(magneto);
            check(Telekinesis.isHolding(magneto), "still holding the golem at tick " + tick);
            return golem.getY() > startY + 2.0;
        });
    }

    @EnhancedTest(template = ARENA)
    public static void thrownGolemIsAWreckingBall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(2.5, FLOOR, 10.5, Power.MAGNETO);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(6.5, FLOOR, 10.5));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no sunburn
        lookAt(magneto, golem);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto), "grabbed the golem");
        float golemMax = golem.getMaxHealth();
        float zombieMax = zombie.getMaxHealth();

        f.everyTick(40, tick -> {
            TestKit.look(magneto, TestKit.EAST, tick < 4 ? -8.0F : 0.0F);
            PowerTicker.tick(magneto);
            if (tick == 4) {
                AbilityHandler.onKey(magneto, 1, true); // launch it at the zombie
                check(!Telekinesis.isHolding(magneto), "launched");
            }
            check(golem.getHealth() == golemMax, "a thrown golem takes no damage itself");
            check(golem.getTarget() != magneto, "the golem never turns on Magneto");
            return tick > 4 && (!zombie.isAlive() || zombieMax - zombie.getHealth() >= 10.0F);
        });
    }

    // ---- Ability 4: Magnetic Shield ----

    /** Has its own batch: it forces the (server-wide) reflect chance to 100%. */
    @EnhancedTest(template = ARENA, batch = "shield_reflect")
    public static void magneticShieldThrowsArrowsBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SHIELD_REFLECT_CHANCE, 1.0);
        ServerPlayer magneto = f.player(14.5, FLOOR, 10.5, Power.MAGNETO);
        AbilityHandler.onKey(magneto, 4, true);
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(3.5, FLOOR, 10.5));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        skeleton.setNoAi(true);
        Arrow arrow = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), null);
        Vec3 dir = magneto.position().add(0, 0.9, 0).subtract(arrow.position());
        arrow.shoot(dir.x, dir.y, dir.z, 2.5F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
        float magnetoMax = magneto.getHealth();
        float skeletonMax = skeleton.getHealth();

        f.everyTick(40, tick -> {
            PowerTicker.tick(magneto);
            check(magneto.getHealth() >= magnetoMax, "the arrow must not hurt Magneto");
            return skeleton.getHealth() < skeletonMax || !skeleton.isAlive();
        });
    }

    /** Like the bullets in the films: a caught arrow hangs still in mid-air, then drops or flies back. */
    @EnhancedTest(template = ARENA, timeoutTicks = 60, batch = "solo_magnetic_shield_holds_arrows_in_mid_air")
    public static void magneticShieldHoldsArrowsInMidAir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SHIELD_REFLECT_CHANCE, 0.0);
        ServerPlayer magneto = f.player(14.5, FLOOR, 10.5, Power.MAGNETO);
        AbilityHandler.onKey(magneto, 4, true);
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(3.5, FLOOR, 10.5));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        skeleton.setNoAi(true);
        Arrow arrow = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), null);
        Vec3 dir = magneto.position().add(0, 0.9, 0).subtract(arrow.position());
        arrow.shoot(dir.x, dir.y, dir.z, 2.5F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
        float max = magneto.getHealth();
        Vec3[] caughtAt = new Vec3[1];
        int[] caughtTick = {-1};

        f.everyTick(50, tick -> {
            PowerTicker.tick(magneto);
            check(magneto.getHealth() >= max, "the arrow must not hurt Magneto");
            if (caughtTick[0] < 0) {
                if (MagnetoAbilities.isHeld(arrow)) {
                    caughtTick[0] = tick;
                    caughtAt[0] = arrow.position();
                    check(arrow.distanceTo(magneto) > 1.0, "it is caught before it reaches him");
                }
                return false;
            }
            int held = tick - caughtTick[0];
            if (held < MagnetoAbilities.HOLD_TICKS - 1) {
                check(arrow.position().distanceTo(caughtAt[0]) < 0.01 && arrow.getDeltaMovement().lengthSqr() < 1.0E-6,
                        "the arrow hangs perfectly still in the air (tick " + held + ")");
                return false;
            }
            if (held > MagnetoAbilities.HOLD_TICKS + 3) {
                check(!MagnetoAbilities.isHeld(arrow) && !arrow.isNoGravity(), "then it is let go and falls");
                check(arrow.getY() < caughtAt[0].y, "it drops to the ground");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA)
    public static void magneticShieldIgnoresNonMetalProjectiles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(12.5, FLOOR, 10.5, Power.MAGNETO);
        AbilityHandler.onKey(magneto, 4, true);
        Snowball snowball = new Snowball(helper.getLevel(), magneto.getX() - 6, magneto.getY() + 1.0, magneto.getZ());
        snowball.shoot(1.0, -0.05, 0.0, 1.5F, 0.0F);
        helper.getLevel().addFreshEntity(snowball);
        Vec3 start = snowball.position();
        f.everyTick(20, tick -> {
            PowerTicker.tick(magneto);
            if (snowball.isRemoved()) {
                return true; // it reached Magneto (or something) without being caught
            }
            check(snowball.getDeltaMovement().x > 0.5 || snowball.position().distanceTo(start) > 5.0,
                    "a snowball isn't metal: the Magnetic Shield must not stop it");
            return false;
        });
    }

    @EnhancedTest(template = ARENA)
    public static void magneticShieldStopsArrows(GameTestHelper helper) {
        shieldArrowTest(helper, true, 3.5, true);
    }

    /**
     * An arrow shot from point-blank range (and with the shield's per-tick field scan not running) must still be
     * stopped when it hits: the impact and damage hooks are the shield's last line of defence.
     */
    @EnhancedTest(template = ARENA)
    public static void magneticShieldStopsPointBlankArrows(GameTestHelper helper) {
        shieldArrowTest(helper, true, 10.0, false);
    }

    /** Control for the tests above: the same arrow does hurt Magneto when his shield is off. */
    @EnhancedTest(template = ARENA)
    public static void arrowHurtsMagnetoWithoutShield(GameTestHelper helper) {
        shieldArrowTest(helper, false, 3.5, true);
    }

    private static void shieldArrowTest(GameTestHelper helper, boolean shield, double shooterX, boolean tickMagneto) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(12.5, FLOOR, 10.5, Power.MAGNETO);
        if (shield) {
            AbilityHandler.onKey(magneto, 4, true);
            check(Powers.get(magneto).magneticShield(), "B turns the shield on");
        }
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(shooterX, FLOOR, 10.5));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no sunburn
        skeleton.setNoAi(true); // only the arrow may hurt Magneto (equipping a skeleton re-adds its melee goal)
        Arrow arrow = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), null);
        Vec3 target = magneto.position().add(0, magneto.getBbHeight() * 0.5, 0);
        Vec3 dir = target.subtract(arrow.position());
        arrow.shoot(dir.x, dir.y, dir.z, 2.5F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
        float max = magneto.getHealth();

        f.everyTick(30, tick -> {
            if (tickMagneto) {
                PowerTicker.tick(magneto);
            }
            if (shield) {
                if (magneto.getHealth() < max) {
                    DamageSource last = magneto.getLastDamageSource();
                    check(false, "an arrow hurt Magneto through his Magnetic Shield (" + (max - magneto.getHealth()) + " damage at tick " + tick
                            + (last == null ? "" : ", " + last.typeHolder().getRegisteredName() + " from " + last.getDirectEntity()
                            + " owned by " + last.getEntity()) + ")");
                }
                return tick >= 30;
            }
            return magneto.getHealth() < max;
        });
    }

    // ---- Ability 5: Magnetic Flight ----

    @EnhancedTest(template = ARENA)
    public static void flightSpeedsAndKeys(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer nobody = f.player(2.5, FLOOR, 2.5);
        ServerPlayer magneto = f.player(6.5, FLOOR, 2.5, Power.MAGNETO);
        ServerPlayer wanda = f.player(10.5, FLOOR, 2.5, Power.SCARLET_WITCH);
        ServerPlayer vision = f.player(14.5, FLOOR, 2.5, Power.VISION);
        for (ServerPlayer p : new ServerPlayer[]{nobody, magneto, wanda, vision}) {
            PowerTicker.tick(p);
        }
        check(!nobody.mayFly(), "a player without powers can't fly");
        check(magneto.mayFly() && wanda.mayFly() && vision.mayFly(), "every hero can fly");
        assertNear(magneto.getAbilities().getFlyingSpeed(), 0.075, 1.0E-4, "Magneto's flight speed");
        assertNear(wanda.getAbilities().getFlyingSpeed(), 0.09, 1.0E-4, "Wanda's flight speed");
        assertNear(vision.getAbilities().getFlyingSpeed(), 0.08, 1.0E-4, "Vision's flight speed");
        check(magneto.getAbilities().getFlyingSpeed() < wanda.getAbilities().getFlyingSpeed(), "Magneto flies a bit slower than Wanda");

        AbilityHandler.onKey(magneto, 5, true);
        check(magneto.getAbilities().flying, "N takes Magneto into the air");
        AbilityHandler.onKey(wanda, 2, true);
        check(wanda.getAbilities().flying, "G takes Wanda into the air");
        AbilityHandler.onKey(vision, 2, true);
        check(vision.getAbilities().flying, "G takes Vision into the air");
        AbilityHandler.onKey(magneto, 5, true);
        check(!magneto.getAbilities().flying, "N again lands");

        Powers.setPower(magneto, Power.NONE);
        check(!magneto.mayFly() && !magneto.getAbilities().flying, "losing the power takes flight away");
        helper.succeed();
    }

    private MagnetoTests() {
    }
}

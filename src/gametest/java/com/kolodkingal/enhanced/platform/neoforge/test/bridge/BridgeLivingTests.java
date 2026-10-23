package com.kolodkingal.enhanced.platform.neoforge.test.bridge;

import com.kolodkingal.enhanced.gametest.TestKit;
import com.kolodkingal.enhanced.platform.neoforge.test.bridge.BridgeTestSupport.Bridged;
import com.kolodkingal.enhanced.platform.event.FinalizeSpawnEvent;
import com.kolodkingal.enhanced.platform.event.LivingChangeTargetEvent;
import com.kolodkingal.enhanced.platform.event.LivingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingDestroyBlockEvent;
import com.kolodkingal.enhanced.platform.event.LivingDropsEvent;
import com.kolodkingal.enhanced.platform.event.LivingEntityUseItemEvent;
import com.kolodkingal.enhanced.platform.event.LivingEquipmentChangeEvent;
import com.kolodkingal.enhanced.platform.event.LivingExperienceDropEvent;
import com.kolodkingal.enhanced.platform.event.LivingFallEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingKnockBackEvent;
import com.kolodkingal.enhanced.platform.event.LivingShieldBlockEvent;
import com.kolodkingal.enhanced.platform.event.MobEffectEvent;
import com.kolodkingal.enhanced.platform.event.MobSpawnEvent;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.platform.neoforge.test.bridge.BridgeTestSupport.BATCH;
import static com.kolodkingal.enhanced.platform.neoforge.test.bridge.BridgeTestSupport.bridge;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/** BridgeTests of the living-entity events (port P0, S-A; {@link BridgeTestSupport}). */
public final class BridgeLivingTests {
    private BridgeLivingTests() {
    }

    private static Zombie zombie(GameTestHelper helper) {
        return TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 8.5));
    }

    private static DamageSource generic(GameTestHelper helper) {
        return helper.getLevel().damageSources().generic();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingIncomingDamage(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        DamageSource source = generic(helper);
        Bridged<LivingIncomingDamageEvent, net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> b =
                bridge(LivingIncomingDamageEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(zombie,
                                new DamageContainer(source, 5.0F)));
        check(b.common().getAmount() == 5.0F && b.common().getSource() == source, "the amount and the source");
        b.common().setAmount(2.0F);
        check(b.neo().getAmount() == 2.0F && b.common().getAmount() == 2.0F, "setAmount reaches NeoForge");
        b.common().setInvulnerabilityTicks(7);
        check(b.neo().getContainer().getPostAttackInvulnerabilityTicks() == 7, "setInvulnerabilityTicks too");
        b.common().addReductionModifier(LivingIncomingDamageEvent.Reduction.ARMOR, (event, reduction) -> {
            check(event == b.common(), "the modifier gets the common event");
            return reduction / 2.0F;
        });
        b.neo().getContainer().setReduction(DamageContainer.Reduction.ARMOR, 3.0F);
        check(b.neo().getContainer().getReduction(DamageContainer.Reduction.ARMOR) == 1.5F,
                "a reduction modifier runs in NeoForge's damage container");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingDamagePre(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        DamageContainer container = new DamageContainer(generic(helper), 6.0F);
        Bridged<LivingDamageEvent.Pre, net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre> b =
                bridge(LivingDamageEvent.Pre.class,
                        () -> new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(zombie, container));
        b.common().setNewDamage(1.0F);
        check(b.neo().getNewDamage() == 1.0F && container.getNewDamage() == 1.0F, "setNewDamage reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingDamagePost(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        DamageContainer container = new DamageContainer(generic(helper), 6.0F);
        container.setNewDamage(4.0F);
        Bridged<LivingDamageEvent.Post, net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> b =
                bridge(LivingDamageEvent.Post.class,
                        () -> new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post(zombie, container));
        check(b.common().getNewDamage() == 4.0F && b.common().getOriginalDamage() == 6.0F, "the damage taken");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingDeath(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        DamageSource source = generic(helper);
        bridge(LivingDeathEvent.class, () -> new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(zombie,
                source));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingChangeTarget(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Zombie other = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(4.5, FLOOR, 4.5));
        Bridged<LivingChangeTargetEvent, net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent> b =
                bridge(LivingChangeTargetEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent(zombie, other,
                                net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType
                                        .MOB_TARGET));
        check(b.common().getNewAboutToBeSetTarget() == other, "the new target");
        b.common().setNewAboutToBeSetTarget(null);
        check(b.neo().getNewAboutToBeSetTarget() == null, "setNewAboutToBeSetTarget reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingDestroyBlock(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(8, 1, 8));
        bridge(LivingDestroyBlockEvent.class,
                () -> new net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent(zombie, pos,
                        Blocks.STONE.defaultBlockState()));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingDrops(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        DamageSource source = generic(helper);
        List<ItemEntity> drops = new ArrayList<>();
        Bridged<LivingDropsEvent, net.neoforged.neoforge.event.entity.living.LivingDropsEvent> b =
                bridge(LivingDropsEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(zombie, source, drops,
                                true));
        check(b.common().getDrops() == drops, "the drops are NeoForge's own collection");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingUseItemStart(GameTestHelper helper) {
        ServerPlayer p = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        ItemStack apple = new ItemStack(Items.APPLE);
        Bridged<LivingEntityUseItemEvent.Start, net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent
                .Start> b = bridge(LivingEntityUseItemEvent.Start.class,
                () -> new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start(p, apple,
                        InteractionHand.MAIN_HAND, 32));
        b.common().setDuration(5);
        check(b.neo().getDuration() == 5, "setDuration reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingUseItemTick(GameTestHelper helper) {
        ServerPlayer p = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        ItemStack apple = new ItemStack(Items.APPLE);
        Bridged<LivingEntityUseItemEvent.Tick, net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent
                .Tick> b = bridge(LivingEntityUseItemEvent.Tick.class,
                () -> new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick(p, apple, 20));
        b.common().setDuration(3);
        check(b.neo().getDuration() == 3, "setDuration reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingEquipmentChange(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        bridge(LivingEquipmentChangeEvent.class,
                () -> new net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent(zombie,
                        EquipmentSlot.HEAD, ItemStack.EMPTY, helmet));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingExperienceDrop(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        ServerPlayer p = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        Bridged<LivingExperienceDropEvent, net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent> b =
                bridge(LivingExperienceDropEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent(zombie, p, 5));
        b.common().setDroppedExperience(9);
        check(b.neo().getDroppedExperience() == 9, "setDroppedExperience reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingFall(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Bridged<LivingFallEvent, net.neoforged.neoforge.event.entity.living.LivingFallEvent> b =
                bridge(LivingFallEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.living.LivingFallEvent(zombie, 3.0F, 1.0F));
        b.common().setDistance(1.0F);
        b.common().setDamageMultiplier(0.5F);
        check(b.neo().getDistance() == 1.0F && b.neo().getDamageMultiplier() == 0.5F, "the setters reach NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingKnockBack(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Bridged<LivingKnockBackEvent, net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent> b =
                bridge(LivingKnockBackEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent(zombie, 0.4F, 1.0,
                                0.5));
        b.common().setStrength(0.1F);
        b.common().setRatioX(2.0);
        b.common().setRatioZ(-1.0);
        check(b.neo().getStrength() == 0.1F && b.neo().getRatioX() == 2.0 && b.neo().getRatioZ() == -1.0,
                "the setters reach NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void livingShieldBlock(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        DamageContainer container = new DamageContainer(generic(helper), 4.0F);
        Bridged<LivingShieldBlockEvent, net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent> b =
                bridge(LivingShieldBlockEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent(zombie, container,
                                true));
        b.common().setBlocked(false);
        check(!b.neo().getBlocked(), "setBlocked reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void mobEffectAdded(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        MobEffectInstance speed = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100);
        bridge(MobEffectEvent.Added.class, () -> new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added(
                zombie, null, speed, null));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void mobEffectApplicable(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        MobEffectInstance speed = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100);
        Bridged<MobEffectEvent.Applicable, net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable> b =
                bridge(MobEffectEvent.Applicable.class,
                        () -> new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable(zombie, speed,
                                null));
        b.common().setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        check(b.neo().getResult() == net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result
                .DO_NOT_APPLY, "setResult reaches NeoForge");
        check(!b.common().getApplicationResult(), "and the effect is not applied");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void mobEffectExpired(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        MobEffectInstance speed = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100);
        bridge(MobEffectEvent.Expired.class,
                () -> new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired(zombie, speed));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void mobEffectRemove(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100));
        Bridged<MobEffectEvent.Remove, net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove> b =
                bridge(MobEffectEvent.Remove.class,
                        () -> new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove(zombie,
                                MobEffects.MOVEMENT_SPEED, null));
        check(b.common().getEffect() == MobEffects.MOVEMENT_SPEED, "the effect removed");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void mobSpawnPositionCheck(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Bridged<MobSpawnEvent.PositionCheck, net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck> b =
                bridge(MobSpawnEvent.PositionCheck.class,
                        () -> new net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck(zombie,
                                helper.getLevel(), MobSpawnType.NATURAL, null));
        b.common().setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        check(b.neo().getResult() == net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL,
                "setResult reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void mobSpawnPlacementCheck(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(6, 2, 6));
        Bridged<MobSpawnEvent.SpawnPlacementCheck,
                net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck> b =
                bridge(MobSpawnEvent.SpawnPlacementCheck.class,
                        () -> new net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck(
                                EntityType.ZOMBIE, helper.getLevel(), MobSpawnType.NATURAL, pos,
                                helper.getLevel().getRandom(), true));
        b.common().setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
        check(b.neo().getResult() == net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck.Result
                .SUCCEED, "setResult reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void finalizeSpawn(GameTestHelper helper) {
        // A mob not yet in the level: NeoForge refuses setSpawnCancelled once it is.
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(8, 2, 8));
        zombie.moveTo(Vec3.atBottomCenterOf(pos));
        Bridged<FinalizeSpawnEvent, net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent> b =
                bridge(FinalizeSpawnEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent(zombie,
                                helper.getLevel(), zombie.getX(), zombie.getY(), zombie.getZ(),
                                helper.getLevel().getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null));
        b.common().setSpawnCancelled(true);
        check(b.neo().isSpawnCancelled(), "setSpawnCancelled reaches NeoForge");
        helper.succeed();
    }
}

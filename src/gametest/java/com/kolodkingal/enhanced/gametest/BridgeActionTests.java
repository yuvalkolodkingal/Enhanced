package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.gametest.BridgeActionSupport.Probe;
import com.kolodkingal.enhanced.platform.event.AttackEntityEvent;
import com.kolodkingal.enhanced.platform.event.BlockDropsEvent;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.EntityJoinLevelEvent;
import com.kolodkingal.enhanced.platform.event.EntityLeaveLevelEvent;
import com.kolodkingal.enhanced.platform.event.EntityMobGriefingEvent;
import com.kolodkingal.enhanced.platform.event.EntityMountEvent;
import com.kolodkingal.enhanced.platform.event.EntityTeleportEvent;
import com.kolodkingal.enhanced.platform.event.EntityTickEvent;
import com.kolodkingal.enhanced.platform.event.EntityTravelToDimensionEvent;
import com.kolodkingal.enhanced.platform.event.ExplosionEvent;
import com.kolodkingal.enhanced.platform.event.ExplosionKnockbackEvent;
import com.kolodkingal.enhanced.platform.event.FinalizeSpawnEvent;
import com.kolodkingal.enhanced.platform.event.ItemEntityPickupEvent;
import com.kolodkingal.enhanced.platform.event.ItemTooltipEvent;
import com.kolodkingal.enhanced.platform.event.ItemTossEvent;
import com.kolodkingal.enhanced.platform.event.LevelTickEvent;
import com.kolodkingal.enhanced.platform.event.LivingChangeTargetEvent;
import com.kolodkingal.enhanced.platform.event.LivingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingDropsEvent;
import com.kolodkingal.enhanced.platform.event.LivingEntityUseItemEvent;
import com.kolodkingal.enhanced.platform.event.LivingEquipmentChangeEvent;
import com.kolodkingal.enhanced.platform.event.LivingExperienceDropEvent;
import com.kolodkingal.enhanced.platform.event.LivingFallEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingKnockBackEvent;
import com.kolodkingal.enhanced.platform.event.MobEffectEvent;
import com.kolodkingal.enhanced.platform.event.PistonEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.PlayerSetSpawnEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.ProjectileImpactEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.kolodkingal.enhanced.platform.event.TriState;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.BridgeActionSupport.BATCH;
import static com.kolodkingal.enhanced.gametest.BridgeActionSupport.cancel;
import static com.kolodkingal.enhanced.gametest.BridgeActionSupport.probe;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * BridgeTests with real vanilla actions (port P0, L8; PLAN2 3.5): a player breaks a block, places one, throws a
 * snowball and a pearl, hits a mob; an arrow flies into a pig; a creature is hurt, falls, dies, drops its loot, gets
 * and loses an effect, is knocked back, mounted, caught in an explosion... and each time the common event reaches the
 * mod's listeners once, with the action's data, in priority order, and what a listener changes or cancels changes what
 * the game does. The tests post nothing themselves: they prove that the loader's side (NeoForge's events through the
 * bridge today, Fabric's callbacks and hook mixins later) fires what the mod's 600 listeners rely on.
 *
 * <p>Everything here is vanilla and loader-neutral; players have no power and the creatures are plain, so the mod's own
 * listeners leave the events alone.
 */
public final class BridgeActionTests {
    private BridgeActionTests() {
    }

    private static BlockPos at(GameTestHelper helper, int x, int y, int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    private static Pig pig(GameTestHelper helper, double x, double z) {
        return TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(x, FLOOR, z));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Blocks

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void breakingABlockPostsItsBreakAndItsDrops(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        BlockPos pos = at(helper, 6, 2, 6);
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        Probe<BlockEvent.BreakEvent> breaks = probe(f, BlockEvent.BreakEvent.class, e -> e.getPos().equals(pos));
        Probe<BlockDropsEvent> drops = probe(f, BlockDropsEvent.class, e -> e.getPos().equals(pos));
        check(player.gameMode.destroyBlock(pos), "the player breaks the dirt");
        BlockEvent.BreakEvent broken = breaks.once();
        check(broken.getPlayer() == player && broken.getState().is(Blocks.DIRT) && broken.getLevel() == level,
                "the break names the player, the dirt and the level");
        BlockDropsEvent dropped = drops.once();
        check(dropped.getBreaker() == player && dropped.getDrops().stream().anyMatch(i -> i.getItem().is(Items.DIRT)),
                "the drops are the dirt, broken by the player: " + dropped.getDrops());
        check(level.getBlockState(pos).isAir(), "the dirt is gone");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCancelledBreakKeepsTheBlock(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        BlockPos pos = at(helper, 6, 2, 6);
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        Probe<BlockEvent.BreakEvent> breaks = probe(f, BlockEvent.BreakEvent.class, e -> e.getPos().equals(pos),
                BridgeActionSupport::cancel);
        Probe<BlockDropsEvent> drops = probe(f, BlockDropsEvent.class, e -> e.getPos().equals(pos));
        check(!player.gameMode.destroyBlock(pos), "a cancelled break breaks nothing");
        breaks.once();
        drops.none("for a block that was not broken");
        check(level.getBlockState(pos).is(Blocks.DIRT), "the dirt stays");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aLeftClickOnABlockStartsItsBreakingUnlessCancelled(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer builder = f.player(5.5, FLOOR, 5.5, GameType.CREATIVE);
        ServerPlayer refused = f.player(7.5, FLOOR, 5.5, GameType.CREATIVE);
        BlockPos first = at(helper, 5, 2, 7);
        BlockPos second = at(helper, 7, 2, 7);
        level.setBlockAndUpdate(first, Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(second, Blocks.DIRT.defaultBlockState());
        Probe<PlayerInteractEvent.LeftClickBlock> clicks = probe(f, PlayerInteractEvent.LeftClickBlock.class,
                e -> e.getEntity() == builder || e.getEntity() == refused, e -> {
                    if (e.getEntity() == refused) {
                        cancel(e);
                    }
                });
        int top = level.getMaxBuildHeight();
        builder.gameMode.handleBlockBreakAction(first, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.UP, top, 1);
        refused.gameMode.handleBlockBreakAction(second, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.UP, top, 2);
        check(clicks.count() == 2, "one left click each: " + clicks.order());
        PlayerInteractEvent.LeftClickBlock click = clicks.seen().get(0);
        check(click.getPos().equals(first) && click.getFace() == Direction.UP
                && click.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START, "the click names its block, "
                + "face and action: " + click.getPos() + " " + click.getFace() + " " + click.getAction());
        check(clicks.order().equals(List.of("HIGH", "LOW", "LOWEST", "HIGH", "LOWEST")), "in priority order, the "
                + "cancelled one skipping LOW: " + clicks.order());
        check(level.getBlockState(first).isAir(), "a creative player's click breaks the block");
        check(level.getBlockState(second).is(Blocks.DIRT), "a cancelled click breaks nothing");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void placingABlockPostsTheRightClickAndThePlacement(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        ItemStack stone = new ItemStack(Items.STONE, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        BlockPos floor = at(helper, 6, 1, 6);
        Probe<PlayerInteractEvent.RightClickBlock> clicks = probe(f, PlayerInteractEvent.RightClickBlock.class,
                e -> e.getEntity() == player);
        Probe<BlockEvent.EntityPlaceEvent> places = probe(f, BlockEvent.EntityPlaceEvent.class,
                e -> e.getEntity() == player);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        InteractionResult result = player.gameMode.useItemOn(player, level, stone, InteractionHand.MAIN_HAND, hit);
        PlayerInteractEvent.RightClickBlock click = clicks.once();
        check(click.getPos().equals(floor) && click.getFace() == Direction.UP
                && click.getHand() == InteractionHand.MAIN_HAND
                && click.getItemStack() == stone, "the right click names the floor, its top, the hand and the stone");
        BlockEvent.EntityPlaceEvent placed = places.once();
        check(placed.getPos().equals(floor.above()) && placed.getPlacedBlock().is(Blocks.STONE)
                && placed.getPlacedAgainst().is(Blocks.SMOOTH_STONE),
                        "the placement names the new block and the floor: "
                + placed.getPos() + " " + placed.getPlacedBlock() + " on " + placed.getPlacedAgainst());
        check(result.consumesAction() && level.getBlockState(floor.above()).is(Blocks.STONE) && stone.getCount() == 3,
                "the stone is placed: " + result);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCancelledPlacementPutsNothingDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        ItemStack stone = new ItemStack(Items.STONE, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        BlockPos floor = at(helper, 6, 1, 6);
        Probe<BlockEvent.EntityPlaceEvent> places = probe(f, BlockEvent.EntityPlaceEvent.class,
                e -> e.getEntity() == player, BridgeActionSupport::cancel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        player.gameMode.useItemOn(player, level, stone, InteractionHand.MAIN_HAND, hit);
        places.once();
        check(level.getBlockState(floor.above()).isAir() && stone.getCount() == 4, "nothing is placed, nothing spent");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void farmlandTrampledUnlessTheTrampleIsCancelled(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer jumper = f.player(4.5, FLOOR, 4.5);
        ServerPlayer careful = f.player(8.5, FLOOR, 4.5);
        BlockPos field = at(helper, 4, 1, 4);
        BlockPos garden = at(helper, 8, 1, 4);
        level.setBlockAndUpdate(field, Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(garden, Blocks.FARMLAND.defaultBlockState());
        Probe<BlockEvent.FarmlandTrampleEvent> tramples = probe(f, BlockEvent.FarmlandTrampleEvent.class,
                e -> e.getEntity() == jumper || e.getEntity() == careful, e -> {
                    if (e.getEntity() == careful) {
                        cancel(e);
                    }
                });
        BlockState farmland = level.getBlockState(field);
        farmland.getBlock().fallOn(level, farmland, field, jumper, 10.0F);
        farmland.getBlock().fallOn(level, farmland, garden, careful, 10.0F);
        check(tramples.count() == 2 && tramples.seen().get(0).getPos().equals(field)
                && tramples.seen().get(0).getFallDistance() == 10.0F, "a trample each, with its fall: "
                + tramples.order());
        check(level.getBlockState(field).is(Blocks.DIRT), "the trampled farmland turns to dirt");
        check(level.getBlockState(garden).is(Blocks.FARMLAND), "a cancelled trample keeps the farmland");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBreakSpeedIsTheListenersToChange(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        float plain = player.getDestroySpeed(dirt);
        Probe<PlayerEvent.BreakSpeed> speeds = probe(f, PlayerEvent.BreakSpeed.class, e -> e.getEntity() == player,
                e -> e.setNewSpeed(e.getOriginalSpeed() * 3.0F));
        float changed = player.getDestroySpeed(dirt);
        PlayerEvent.BreakSpeed speed = speeds.once();
        check(speed.getState() == dirt && speed.getOriginalSpeed() == plain, "the event has the block and the speed");
        check(changed == plain * 3.0F, "the listener's speed is the player's: " + changed + " for " + plain);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Items

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void throwingASnowballPostsTheRightClickAndTheSnowballJoins(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer thrower = f.player(4.5, FLOOR, 5.5);
        ServerPlayer refused = f.player(8.5, FLOOR, 5.5);
        ItemStack snow = new ItemStack(Items.SNOWBALL, 4);
        ItemStack kept = new ItemStack(Items.SNOWBALL, 4);
        thrower.setItemInHand(InteractionHand.MAIN_HAND, snow);
        refused.setItemInHand(InteractionHand.MAIN_HAND, kept);
        Probe<PlayerInteractEvent.RightClickItem> uses = probe(f, PlayerInteractEvent.RightClickItem.class,
                e -> e.getEntity() == thrower || e.getEntity() == refused, e -> {
                    if (e.getEntity() == refused) {
                        cancel(e);
                    }
                });
        Probe<EntityJoinLevelEvent> joins = probe(f, EntityJoinLevelEvent.class,
                e -> e.getEntity() instanceof Snowball s && (s.getOwner() == thrower || s.getOwner() == refused));
        thrower.gameMode.useItem(thrower, level, snow, InteractionHand.MAIN_HAND);
        refused.gameMode.useItem(refused, level, kept, InteractionHand.MAIN_HAND);
        check(uses.count() == 2 && uses.seen().get(0).getItemStack() == snow
                && uses.seen().get(0).getHand() == InteractionHand.MAIN_HAND, "a right click each: " + uses.order());
        EntityJoinLevelEvent joined = joins.once();
        check(((Snowball) joined.getEntity()).getOwner() == thrower && joined.getLevel() == level,
                "the thrower's snowball joins the level");
        check(snow.getCount() == 3 && kept.getCount() == 4, "the thrower spent one; the cancelled use spent none");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void tossingAnItemPostsTheTossUnlessCancelled(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tosser = f.player(4.5, FLOOR, 5.5);
        ServerPlayer refused = f.player(8.5, FLOOR, 5.5);
        Probe<ItemTossEvent> tosses = probe(f, ItemTossEvent.class,
                e -> e.getPlayer() == tosser || e.getPlayer() == refused, e -> {
                    if (e.getPlayer() == refused) {
                        cancel(e);
                    }
                });
        ItemEntity tossed = tosser.drop(new ItemStack(Items.STICK, 2), true);
        ItemEntity none = refused.drop(new ItemStack(Items.STICK, 2), true);
        check(tosses.count() == 2 && tosses.seen().get(0).getEntity() == tossed
                && tosses.seen().get(0).getEntity().getItem().is(Items.STICK), "a toss each, with its item entity");
        check(tossed != null && tossed.isAddedToLevel() && none == null, "the tossed sticks are in the world; the "
                + "cancelled toss made nothing");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void pickingUpAnItemAsksTheListeners(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer taker = f.player(4.5, FLOOR, 5.5);
        ServerPlayer refused = f.player(8.5, FLOOR, 5.5);
        Vec3 here = helper.absoluteVec(new Vec3(4.5, FLOOR, 5.5));
        Vec3 there = helper.absoluteVec(new Vec3(8.5, FLOOR, 5.5));
        ItemEntity taken = new ItemEntity(level, here.x, here.y, here.z, new ItemStack(Items.STICK, 3));
        ItemEntity left = new ItemEntity(level, there.x, there.y, there.z, new ItemStack(Items.STICK, 3));
        for (ItemEntity item : List.of(taken, left)) {
            item.setNoPickUpDelay();
            level.addFreshEntity(item);
        }
        Probe<ItemEntityPickupEvent.Pre> pickups = probe(f, ItemEntityPickupEvent.Pre.class,
                e -> e.getItemEntity() == taken || e.getItemEntity() == left, e -> {
                    if (e.getItemEntity() == left) {
                        e.setCanPickup(TriState.FALSE);
                    }
                });
        taken.playerTouch(taker);
        left.playerTouch(refused);
        check(pickups.count() == 2 && pickups.seen().get(0).getPlayer() == taker, "a pickup each: " + pickups.order());
        check(taken.isRemoved() && taker.getInventory().countItem(Items.STICK) == 3, "the sticks are picked up");
        check(!left.isRemoved() && refused.getInventory().countItem(Items.STICK) == 0, "a refused pickup leaves them");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void startingToUseAnItemIsTheListenersToStop(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer archer = f.player(4.5, FLOOR, 5.5);
        ServerPlayer refused = f.player(8.5, FLOOR, 5.5);
        archer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        refused.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        Probe<LivingEntityUseItemEvent.Start> starts = probe(f, LivingEntityUseItemEvent.Start.class,
                e -> e.getEntity() == archer || e.getEntity() == refused, e -> {
                    if (e.getEntity() == refused) {
                        cancel(e);
                    }
                });
        archer.startUsingItem(InteractionHand.MAIN_HAND);
        refused.startUsingItem(InteractionHand.MAIN_HAND);
        LivingEntityUseItemEvent.Start start = starts.seen().get(0);
        check(starts.count() == 2 && start.getItem().is(Items.BOW) && start.getHand() == InteractionHand.MAIN_HAND
                && start.getDuration() == 72000, "a start each, with the bow and its time: " + starts.order());
        check(archer.isUsingItem() && !refused.isUsingItem(), "the archer draws; the cancelled start does not");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aTooltipIsTheListenersToAddTo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        ItemStack stick = new ItemStack(Items.STICK);
        Probe<ItemTooltipEvent> tooltips = probe(f, ItemTooltipEvent.class, e -> e.getItemStack() == stick,
                e -> e.getToolTip().add(Component.literal("bridge")));
        List<Component> lines = stick.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), player,
                TooltipFlag.Default.NORMAL);
        ItemTooltipEvent tooltip = tooltips.once();
        check(tooltip.getEntity() == player && tooltip.getFlags() == TooltipFlag.Default.NORMAL,
                "the tooltip's player and flags");
        check(lines.stream().anyMatch(c -> c.getString().equals("bridge")), "the listener's line is in it: " + lines);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Fights and damage

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aPlayersHitPostsTheAttackAndTheDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        Pig pig = pig(helper, 6.5, 5.5);
        Probe<AttackEntityEvent> attacks = probe(f, AttackEntityEvent.class, e -> e.getEntity() == player);
        Probe<LivingIncomingDamageEvent> incoming = probe(f, LivingIncomingDamageEvent.class,
                e -> e.getEntity() == pig);
        Probe<LivingDamageEvent.Pre> pre = probe(f, LivingDamageEvent.Pre.class, e -> e.getEntity() == pig);
        Probe<LivingDamageEvent.Post> post = probe(f, LivingDamageEvent.Post.class, e -> e.getEntity() == pig);
        TestKit.chargeAttack(player);
        player.attack(pig);
        check(attacks.once().getTarget() == pig, "the attack names the pig");
        LivingIncomingDamageEvent hit = incoming.once();
        check(hit.getSource().getEntity() == player && hit.getAmount() == 1.0F, "the pig is hit by the player's fist: "
                + hit.getAmount());
        check(pre.once().getNewDamage() == 1.0F && post.once().getNewDamage() == 1.0F, "and takes the one point");
        check(pig.getHealth() == pig.getMaxHealth() - 1.0F, "the pig lost it: " + pig.getHealth());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCancelledAttackHurtsNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        Pig pig = pig(helper, 6.5, 5.5);
        Probe<AttackEntityEvent> attacks = probe(f, AttackEntityEvent.class, e -> e.getEntity() == player,
                BridgeActionSupport::cancel);
        Probe<LivingIncomingDamageEvent> incoming = probe(f, LivingIncomingDamageEvent.class,
                e -> e.getEntity() == pig);
        TestKit.chargeAttack(player);
        player.attack(pig);
        attacks.once();
        incoming.none("for a cancelled attack");
        check(pig.getHealth() == pig.getMaxHealth(), "the pig is not hurt");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void incomingDamageIsTheListenersToChangeOrCancel(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Pig softened = pig(helper, 4.5, 5.5);
        Pig spared = pig(helper, 8.5, 5.5);
        Probe<LivingIncomingDamageEvent> incoming = probe(f, LivingIncomingDamageEvent.class,
                e -> e.getEntity() == softened || e.getEntity() == spared, e -> {
                    if (e.getEntity() == spared) {
                        cancel(e);
                    } else {
                        e.setAmount(1.0F);
                    }
                });
        Probe<LivingDamageEvent.Post> post = probe(f, LivingDamageEvent.Post.class,
                e -> e.getEntity() == softened || e.getEntity() == spared);
        check(softened.hurt(level.damageSources().generic(), 4.0F), "the first pig is hurt");
        check(!spared.hurt(level.damageSources().generic(), 4.0F), "the second is not");
        check(incoming.count() == 2 && incoming.seen().get(0).getAmount() == 1.0F, "an incoming damage each: "
                + incoming.order());
        LivingDamageEvent.Post taken = post.once();
        check(taken.getEntity() == softened && taken.getOriginalDamage() == 4.0F && taken.getNewDamage() == 1.0F,
                "the damage taken is the listener's amount (of the original four): " + taken.getNewDamage());
        check(softened.getHealth() == softened.getMaxHealth() - 1.0F && spared.getHealth() == spared.getMaxHealth(),
                "health: " + softened.getHealth() + ", " + spared.getHealth());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void anArrowIntoAPigPostsTheImpactAndItsDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer archer = f.player(2.5, FLOOR, 2.5);
        Pig pig = pig(helper, 8.5, 5.5);
        Vec3 from = helper.absoluteVec(new Vec3(4.5, FLOOR + 0.5, 5.5));
        Arrow arrow = new Arrow(level, from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
        arrow.setOwner(archer);
        arrow.shoot(1.0, 0.0, 0.0, 1.5F, 0.0F);
        Probe<ProjectileImpactEvent> impacts = probe(f, ProjectileImpactEvent.class, e -> e.getProjectile() == arrow);
        Probe<LivingIncomingDamageEvent> incoming = probe(f, LivingIncomingDamageEvent.class,
                e -> e.getEntity() == pig);
        Probe<EntityJoinLevelEvent> joins = probe(f, EntityJoinLevelEvent.class, e -> e.getEntity() == arrow);
        level.addFreshEntity(arrow);
        joins.once();
        helper.succeedWhen(() -> {
            check(pig.getHealth() < pig.getMaxHealth(), "the arrow has not hit the pig yet");
            ProjectileImpactEvent impact = impacts.once();
            check(impact.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() == pig,
                    "the impact is on the pig");
            LivingIncomingDamageEvent damage = incoming.once();
            check(damage.getSource().getDirectEntity() == arrow && damage.getSource().getEntity() == archer,
                    "the damage is the archer's arrow");
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void aCancelledImpactLetsTheArrowFlyOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Pig pig = pig(helper, 8.5, 5.5);
        Vec3 from = helper.absoluteVec(new Vec3(4.5, FLOOR + 0.5, 5.5));
        Arrow arrow = new Arrow(level, from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
        arrow.shoot(1.0, 0.0, 0.0, 1.5F, 0.0F);
        Probe<ProjectileImpactEvent> impacts = probe(f, ProjectileImpactEvent.class,
                e -> e.getProjectile() == arrow && e.getRayTraceResult() instanceof EntityHitResult hit
                        && hit.getEntity() == pig, BridgeActionSupport::cancel);
        level.addFreshEntity(arrow);
        helper.runAfterDelay(20, () -> {
            check(impacts.count() >= 1, "the arrow reached the pig");
            check(impacts.order().stream().noneMatch("LOW"::equals), "every impact was cancelled at HIGH: "
                    + impacts.order());
            check(pig.getHealth() == pig.getMaxHealth(), "a cancelled impact hurts nothing");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aFallIsTheListenersToSoften(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Pig faller = pig(helper, 4.5, 5.5);
        Pig floater = pig(helper, 8.5, 5.5);
        Probe<LivingFallEvent> falls = probe(f, LivingFallEvent.class,
                e -> e.getEntity() == faller || e.getEntity() == floater, e -> {
                    if (e.getEntity() == floater) {
                        e.setDistance(0.0F);
                    }
                });
        faller.causeFallDamage(6.0F, 1.0F, level.damageSources().fall());
        floater.causeFallDamage(6.0F, 1.0F, level.damageSources().fall());
        check(falls.count() == 2 && falls.seen().get(0).getDistance() == 6.0F
                && falls.seen().get(0).getDamageMultiplier() == 1.0F, "a fall each: " + falls.order());
        check(faller.getHealth() == faller.getMaxHealth() - 3.0F, "six blocks cost three points: "
                + faller.getHealth());
        check(floater.getHealth() == floater.getMaxHealth(), "a fall made zero blocks costs nothing");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void knockbackIsTheListenersToChange(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Pig pushed = pig(helper, 4.5, 5.5);
        Pig held = pig(helper, 8.5, 5.5);
        Probe<LivingKnockBackEvent> knocks = probe(f, LivingKnockBackEvent.class,
                e -> e.getEntity() == pushed || e.getEntity() == held, e -> {
                    if (e.getEntity() == held) {
                        cancel(e);
                    }
                });
        pushed.setDeltaMovement(Vec3.ZERO);
        held.setDeltaMovement(Vec3.ZERO);
        pushed.knockback(0.5, 1.0, 0.0);
        held.knockback(0.5, 1.0, 0.0);
        LivingKnockBackEvent knock = knocks.seen().get(0);
        check(knocks.count() == 2 && knock.getStrength() == 0.5F && knock.getRatioX() == 1.0
                && knock.getRatioZ() == 0.0,
                "a knockback each, with its strength and direction: " + knocks.order());
        check(pushed.getDeltaMovement().x < 0.0, "the pig is pushed away from +x: " + pushed.getDeltaMovement());
        check(held.getDeltaMovement().x == 0.0, "a cancelled knockback moves nothing: " + held.getDeltaMovement());
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Death, loot and experience

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aKilledPigPostsItsDeathAndItsDrops(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Pig pig = pig(helper, 6.5, 5.5);
        Probe<LivingDeathEvent> deaths = probe(f, LivingDeathEvent.class, e -> e.getEntity() == pig);
        Probe<LivingDropsEvent> drops = probe(f, LivingDropsEvent.class, e -> e.getEntity() == pig);
        pig.hurt(level.damageSources().generic(), 100.0F);
        check(deaths.once().getSource().is(net.minecraft.world.damagesource.DamageTypes.GENERIC), "the death's source");
        LivingDropsEvent dropped = drops.once();
        check(!dropped.isRecentlyHit() && dropped.getDrops().stream().anyMatch(i -> i.getItem().is(Items.PORKCHOP)),
                "the pig's drops: " + dropped.getDrops());
        check(pig.isDeadOrDying(), "the pig is dead");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCancelledDeathLeavesTheCreatureAlive(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Pig pig = pig(helper, 6.5, 5.5);
        Probe<LivingDeathEvent> deaths = probe(f, LivingDeathEvent.class, e -> e.getEntity() == pig, e -> {
            cancel(e);
            pig.setHealth(1.0F);
        });
        Probe<LivingDropsEvent> drops = probe(f, LivingDropsEvent.class, e -> e.getEntity() == pig);
        pig.hurt(level.damageSources().generic(), 100.0F);
        deaths.once();
        drops.none("for a creature that did not die");
        check(pig.isAlive() && pig.getHealth() == 1.0F, "the pig lives on: " + pig.getHealth());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void experienceOfAKillIsTheListenersToChange(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(7.5, FLOOR, 5.5));
        Probe<LivingExperienceDropEvent> drops = probe(f, LivingExperienceDropEvent.class,
                e -> e.getEntity() == zombie, e -> e.setDroppedExperience(0));
        zombie.hurt(level.damageSources().playerAttack(player), 100.0F);
        helper.succeedWhen(() -> {
            LivingExperienceDropEvent drop = drops.once();
            check(drop.getAttackingPlayer() == player && drop.getOriginalExperience() == 5,
                    "the zombie's five points, for the player: " + drop.getOriginalExperience());
            check(level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,
                    zombie.getBoundingBox().inflate(3.0)).isEmpty(), "no orb: the listener made it zero");
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Effects

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anEffectIsAskedForAddedAndRemoved(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Pig pig = pig(helper, 6.5, 5.5);
        Probe<MobEffectEvent.Applicable> asks = probe(f, MobEffectEvent.Applicable.class, e -> e.getEntity() == pig);
        Probe<MobEffectEvent.Added> adds = probe(f, MobEffectEvent.Added.class, e -> e.getEntity() == pig);
        Probe<MobEffectEvent.Remove> removes = probe(f, MobEffectEvent.Remove.class, e -> e.getEntity() == pig,
                BridgeActionSupport::cancel);
        check(pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200)), "the pig gets Speed");
        check(asks.once().getEffectInstance().getEffect().equals(MobEffects.MOVEMENT_SPEED), "asked about Speed");
        MobEffectEvent.Added added = adds.once();
        check(added.getEffectInstance().getDuration() == 200 && added.getOldEffectInstance() == null,
                "added, new: " + added.getEffectInstance());
        check(!pig.removeEffect(MobEffects.MOVEMENT_SPEED), "a cancelled removal removes nothing");
        check(removes.once().getEffect().equals(MobEffects.MOVEMENT_SPEED) && pig.hasEffect(MobEffects.MOVEMENT_SPEED),
                "Speed stays");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anEffectTheListenersRefuseIsNotAdded(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Pig pig = pig(helper, 6.5, 5.5);
        Probe<MobEffectEvent.Applicable> asks = probe(f, MobEffectEvent.Applicable.class, e -> e.getEntity() == pig,
                e -> e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY));
        Probe<MobEffectEvent.Added> adds = probe(f, MobEffectEvent.Added.class, e -> e.getEntity() == pig);
        check(!pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200)), "a refused effect is not added");
        asks.once();
        adds.none("for a refused effect");
        check(!pig.hasEffect(MobEffects.MOVEMENT_SPEED), "the pig has no Speed");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void anEffectThatRunsOutPostsItsExpiry(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Pig pig = pig(helper, 6.5, 5.5);
        Probe<MobEffectEvent.Expired> expiries = probe(f, MobEffectEvent.Expired.class, e -> e.getEntity() == pig);
        pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3));
        helper.succeedWhen(() -> {
            check(!pig.hasEffect(MobEffects.MOVEMENT_SPEED), "Speed has not run out yet");
            check(expiries.once().getEffectInstance().getEffect().equals(MobEffects.MOVEMENT_SPEED), "Speed expired");
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Creatures and entities

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anEntityJoiningTheLevelCanBeTurnedAway(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Pig welcome = EntityType.PIG.create(level);
        Pig turnedAway = EntityType.PIG.create(level);
        Vec3 here = helper.absoluteVec(new Vec3(4.5, FLOOR, 5.5));
        Vec3 there = helper.absoluteVec(new Vec3(8.5, FLOOR, 5.5));
        welcome.moveTo(here.x, here.y, here.z, 0.0F, 0.0F);
        turnedAway.moveTo(there.x, there.y, there.z, 0.0F, 0.0F);
        welcome.setNoAi(true);
        turnedAway.setNoAi(true);
        Probe<EntityJoinLevelEvent> joins = probe(f, EntityJoinLevelEvent.class,
                e -> e.getEntity() == welcome || e.getEntity() == turnedAway, e -> {
                    if (e.getEntity() == turnedAway) {
                        cancel(e);
                    }
                });
        check(level.addFreshEntity(welcome), "the first pig joins");
        check(!level.addFreshEntity(turnedAway), "the second is turned away");
        check(joins.count() == 2 && joins.seen().get(0).getLevel() == level && !joins.seen().get(0).loadedFromDisk(),
                "a join each, new in this level: " + joins.order());
        check(welcome.isAddedToLevel() && !turnedAway.isAddedToLevel(), "only the first is in the level");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void anEntityLeavingTheLevelPostsItsLeave(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Pig pig = pig(helper, 6.5, 5.5);
        Probe<EntityLeaveLevelEvent> leaves = probe(f, EntityLeaveLevelEvent.class, e -> e.getEntity() == pig);
        pig.discard();
        helper.succeedWhen(() -> check(leaves.once().getLevel() == helper.getLevel(), "the pig left this level"));
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aMobsNewTargetIsTheListenersToRefuse(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        Zombie hunter = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(3.5, FLOOR, 8.5));
        Zombie calm = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 8.5));
        Probe<LivingChangeTargetEvent> targets = probe(f, LivingChangeTargetEvent.class,
                e -> e.getEntity() == hunter || e.getEntity() == calm, e -> {
                    if (e.getEntity() == calm) {
                        cancel(e);
                    }
                });
        hunter.setTarget(player);
        calm.setTarget(player);
        LivingChangeTargetEvent target = targets.seen().get(0);
        check(targets.count() == 2 && target.getOriginalAboutToBeSetTarget() == player
                && target.getNewAboutToBeSetTarget() == player, "a target change each: " + targets.order());
        check(hunter.getTarget() == player && calm.getTarget() == null, "only the first zombie targets the player");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void newGearPostsAnEquipmentChange(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(6.5, FLOOR, 5.5));
        Probe<LivingEquipmentChangeEvent> changes = probe(f, LivingEquipmentChangeEvent.class,
                e -> e.getEntity() == zombie && e.getSlot() == EquipmentSlot.MAINHAND);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        helper.succeedWhen(() -> {
            LivingEquipmentChangeEvent change = changes.once();
            check(change.getFrom().isEmpty() && change.getTo().is(Items.IRON_SWORD), "from nothing to the sword");
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void mountingIsTheListenersToRefuse(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer rider = f.player(4.5, FLOOR, 5.5);
        ServerPlayer refused = f.player(8.5, FLOOR, 5.5);
        Pig ridden = pig(helper, 4.5, 6.5);
        Pig free = pig(helper, 8.5, 6.5);
        Probe<EntityMountEvent> mounts = probe(f, EntityMountEvent.class,
                e -> e.getEntityMounting() == rider || e.getEntityMounting() == refused, e -> {
                    if (e.getEntityMounting() == refused) {
                        cancel(e);
                    }
                });
        check(rider.startRiding(ridden, true), "the rider gets on");
        check(!refused.startRiding(free, true), "the refused rider does not");
        EntityMountEvent mount = mounts.seen().get(0);
        check(mounts.count() == 2 && mount.getEntityBeingMounted() == ridden && mount.isMounting()
                && mount.getLevel() == helper.getLevel(), "a mount each: " + mounts.order());
        rider.stopRiding();
        check(mounts.count() == 3 && mounts.seen().get(2).isDismounting(), "getting off is a dismount");
        check(!rider.isPassenger() && !refused.isPassenger(), "nobody rides now");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void aCancelledEntityTickSkipsTheTick(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Pig ticking = pig(helper, 4.5, 5.5);
        Pig frozen = pig(helper, 8.5, 5.5);
        ticking.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
        frozen.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
        Probe<EntityTickEvent.Pre> pre = probe(f, EntityTickEvent.Pre.class,
                e -> e.getEntity() == ticking || e.getEntity() == frozen, e -> {
                    if (e.getEntity() == frozen) {
                        cancel(e);
                    }
                });
        Probe<EntityTickEvent.Post> post = probe(f, EntityTickEvent.Post.class, e -> e.getEntity() == frozen);
        helper.runAfterDelay(5, () -> {
            check(pre.seen().stream().filter(e -> e.getEntity() == frozen).count() >= 4, "the frozen pig was asked "
                    + "every tick");
            post.none("after a cancelled tick");
            check(frozen.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() == 200, "nothing of its tick ran");
            check(ticking.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() < 200, "the other pig's effect ticks");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aPlayersTickPostsBeforeAndAfter(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        List<String> ticks = new java.util.ArrayList<>();
        Probe<PlayerTickEvent.Pre> pre = probe(f, PlayerTickEvent.Pre.class, e -> e.getEntity() == player,
                e -> ticks.add("pre"));
        Probe<PlayerTickEvent.Post> post = probe(f, PlayerTickEvent.Post.class, e -> e.getEntity() == player,
                e -> ticks.add("post"));
        player.doTick();
        pre.once();
        post.once();
        check(ticks.equals(List.of("pre", "post")), "before, then after: " + ticks);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 40)
    public static void theLevelTicksPostTheirEvents(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Probe<LevelTickEvent.Pre> pre = probe(f, LevelTickEvent.Pre.class, e -> e.getLevel() == level);
        Probe<LevelTickEvent.Post> post = probe(f, LevelTickEvent.Post.class, e -> e.getLevel() == level);
        helper.succeedWhen(() -> {
            check(pre.count() >= 2 && post.count() >= 2, "two ticks of this level: " + pre.count() + ", "
                    + post.count());
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aSpawnedMobIsFinalisedUnlessTheSpawnIsCancelled(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos here = at(helper, 4, 2, 5);
        BlockPos there = at(helper, 8, 2, 5);
        Probe<FinalizeSpawnEvent> spawns = probe(f, FinalizeSpawnEvent.class,
                e -> e.getEntity().blockPosition().equals(here) || e.getEntity().blockPosition().equals(there), e -> {
                    if (e.getEntity().blockPosition().equals(there)) {
                        e.setSpawnCancelled(true);
                    }
                });
        Pig spawned = EntityType.PIG.spawn(level, here, MobSpawnType.MOB_SUMMONED);
        Pig refused = EntityType.PIG.spawn(level, there, MobSpawnType.MOB_SUMMONED);
        check(spawns.count() == 2 && spawns.seen().get(0).getSpawnType() == MobSpawnType.MOB_SUMMONED,
                "a finalised spawn each: " + spawns.order());
        check(spawned != null && spawned.isAddedToLevel(), "the first pig is spawned");
        check(refused == null || !refused.isAddedToLevel(), "a cancelled spawn adds no pig");
        if (spawned != null) {
            spawned.discard();
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void milkingACowPostsTheEntityInteraction(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer milker = f.player(4.5, FLOOR, 5.5);
        ServerPlayer refused = f.player(8.5, FLOOR, 5.5);
        Cow cow = TestKit.spawnWithNoFreeWill(helper, EntityType.COW, new Vec3(4.5, FLOOR, 7.5));
        Cow other = TestKit.spawnWithNoFreeWill(helper, EntityType.COW, new Vec3(8.5, FLOOR, 7.5));
        milker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        refused.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        Probe<PlayerInteractEvent.EntityInteract> uses = probe(f, PlayerInteractEvent.EntityInteract.class,
                e -> e.getEntity() == milker || e.getEntity() == refused, e -> {
                    if (e.getEntity() == refused) {
                        cancel(e);
                        e.setCancellationResult(InteractionResult.FAIL);
                    }
                });
        milker.interactOn(cow, InteractionHand.MAIN_HAND);
        InteractionResult refusedResult = refused.interactOn(other, InteractionHand.MAIN_HAND);
        check(uses.count() == 2 && uses.seen().get(0).getTarget() == cow, "an interaction each: " + uses.order());
        check(milker.getMainHandItem().is(Items.MILK_BUCKET), "the cow is milked");
        check(refused.getMainHandItem().is(Items.BUCKET) && refusedResult == InteractionResult.FAIL,
                "a cancelled interaction milks nothing and answers the listener's result");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Teleports and explosions

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void aThrownPearlPostsItsTeleport(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        ThrownEnderpearl pearl = new ThrownEnderpearl(level, player);
        pearl.shoot(0.0, -1.0, 0.0, 1.0F, 0.0F);
        Probe<EntityTeleportEvent.EnderPearl> teleports = probe(f, EntityTeleportEvent.EnderPearl.class,
                e -> e.getPlayer() == player, e -> e.setAttackDamage(2.0F));
        level.addFreshEntity(pearl);
        helper.succeedWhen(() -> {
            EntityTeleportEvent.EnderPearl teleport = teleports.once();
            check(teleport.getPearlEntity() == pearl && teleport.getEntity() == player, "the pearl and its thrower");
            check(player.getHealth() == player.getMaxHealth() - 2.0F, "the listener's damage: " + player.getHealth());
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anExplosionsVictimsAreTheListenersToSpare(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Pig spared = pig(helper, 5.5, 5.5);
        Pig hit = pig(helper, 7.5, 5.5);
        Vec3 blast = helper.absoluteVec(new Vec3(6.5, FLOOR + 0.5, 6.5));
        Probe<ExplosionEvent.Detonate> blasts = probe(f, ExplosionEvent.Detonate.class,
                e -> e.getExplosion().center().distanceToSqr(blast) < 1.0E-6,
                        e -> e.getAffectedEntities().remove(spared));
        Probe<ExplosionKnockbackEvent> knocks = probe(f, ExplosionKnockbackEvent.class,
                e -> e.getAffectedEntity() == spared || e.getAffectedEntity() == hit);
        level.explode(null, blast.x, blast.y, blast.z, 2.0F, Level.ExplosionInteraction.NONE);
        ExplosionEvent.Detonate detonate = blasts.once();
        check(detonate.getLevel() == level && detonate.getAffectedEntities().contains(hit)
                && !detonate.getAffectedEntities().contains(spared), "the blast and its victims, but the spared pig");
        check(knocks.once().getAffectedEntity() == hit, "only the victim is knocked back");
        check(spared.getHealth() == spared.getMaxHealth() && hit.getHealth() < hit.getMaxHealth(),
                "the spared pig is unhurt, the other is not: " + spared.getHealth() + ", " + hit.getHealth());
        check(level.getBlockState(at(helper, 6, 1, 6)).is(Blocks.SMOOTH_STONE), "the blast breaks no block");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCreepersBlastAsksWhetherMobsMayGrief(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Creeper creeper = TestKit.spawnWithNoFreeWill(helper, EntityType.CREEPER, new Vec3(6.5, FLOOR, 6.5));
        Probe<EntityMobGriefingEvent> asks = probe(f, EntityMobGriefingEvent.class, e -> e.getEntity() == creeper,
                e -> e.setCanGrief(false));
        level.explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(), 1.0F, Level.ExplosionInteraction.MOB);
        EntityMobGriefingEvent ask = asks.once();
        check(ask.getLevel() == level && !ask.canGrief(), "the creeper was asked about, and refused");
        check(level.getBlockState(at(helper, 6, 1, 6)).is(Blocks.SMOOTH_STONE), "a refused mob breaks no block");
        creeper.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // More of the world: tools, pistons, neighbours, spawn points, game modes, dimensions, ticks

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void strippingALogIsTheListenersToRefuse(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer woodcutter = f.player(4.5, FLOOR, 5.5);
        ServerPlayer refused = f.player(8.5, FLOOR, 5.5);
        BlockPos log = at(helper, 4, 2, 7);
        BlockPos kept = at(helper, 8, 2, 7);
        level.setBlockAndUpdate(log, Blocks.OAK_LOG.defaultBlockState());
        level.setBlockAndUpdate(kept, Blocks.OAK_LOG.defaultBlockState());
        Probe<BlockEvent.BlockToolModificationEvent> strips = probe(f, BlockEvent.BlockToolModificationEvent.class,
                e -> e.getPos().equals(log) || e.getPos().equals(kept), e -> {
                    if (e.getPos().equals(kept)) {
                        cancel(e);
                    }
                });
        for (ServerPlayer player : List.of(woodcutter, refused)) {
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            BlockPos target = player == woodcutter ? log : kept;
            player.gameMode.useItemOn(player, level, axe, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(target), Direction.NORTH, target, false));
        }
        BlockEvent.BlockToolModificationEvent strip = strips.seen().get(0);
        check(strip.getPos().equals(log) && strip.getPlayer() == woodcutter && strip.getHeldItemStack().is(Items.IRON_AXE)
                && !strip.isSimulated(), "the strip names the log, the player and the axe");
        check(strips.seen().stream().anyMatch(e -> e.getPos().equals(kept)), "the refused player's axe asked too");
        check(level.getBlockState(log).is(Blocks.STRIPPED_OAK_LOG), "the log is stripped");
        check(level.getBlockState(kept).is(Blocks.OAK_LOG), "a cancelled strip keeps the bark");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void aPoweredPistonPostsItsPushUnlessCancelled(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pushing = at(helper, 4, 2, 5);
        BlockPos stuck = at(helper, 8, 2, 5);
        Probe<PistonEvent.Pre> pushes = probe(f, PistonEvent.Pre.class,
                e -> e.getPos().equals(pushing) || e.getPos().equals(stuck), e -> {
                    if (e.getPos().equals(stuck)) {
                        cancel(e);
                    }
                });
        for (BlockPos piston : List.of(pushing, stuck)) {
            level.setBlockAndUpdate(piston, Blocks.PISTON.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING, Direction.UP));
            level.setBlockAndUpdate(piston.above(), Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(piston.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        }
        helper.succeedWhen(() -> {
            check(level.getBlockState(pushing.above(2)).is(Blocks.DIRT), "the piston has not pushed yet");
            PistonEvent.Pre push = pushes.seen().stream().filter(e -> e.getPos().equals(pushing)).findFirst()
                    .orElseThrow();
            check(push.getDirection() == Direction.UP && push.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND
                    && push.getFaceOffsetPos().equals(pushing.above()), "the push names its way");
            check(pushes.seen().stream().anyMatch(e -> e.getPos().equals(stuck)), "the other piston was asked too");
            check(level.getBlockState(stuck.above()).is(Blocks.DIRT), "a cancelled push moves nothing");
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aNewBlockNotifiesItsNeighbours(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = at(helper, 6, 2, 6);
        Probe<BlockEvent.NeighborNotifyEvent> notes = probe(f, BlockEvent.NeighborNotifyEvent.class,
                e -> e.getPos().equals(pos));
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        BlockEvent.NeighborNotifyEvent note = notes.once();
        check(note.getState().is(Blocks.STONE) && note.getNotifiedSides().size() == 6 && note.getLevel() == level,
                "the stone tells its six neighbours: " + note.getNotifiedSides());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aNewSpawnPointIsTheListenersToRefuse(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer settled = f.player(4.5, FLOOR, 5.5);
        ServerPlayer refused = f.player(8.5, FLOOR, 5.5);
        BlockPos home = at(helper, 4, 2, 5);
        Probe<PlayerSetSpawnEvent> spawns = probe(f, PlayerSetSpawnEvent.class,
                e -> e.getEntity() == settled || e.getEntity() == refused, e -> {
                    if (e.getEntity() == refused) {
                        cancel(e);
                    }
                });
        settled.setRespawnPosition(Level.OVERWORLD, home, 0.0F, true, false);
        refused.setRespawnPosition(Level.OVERWORLD, home, 0.0F, true, false);
        PlayerSetSpawnEvent spawn = spawns.seen().get(0);
        check(spawns.count() == 2 && spawn.getNewSpawn().equals(home) && spawn.isForced()
                && spawn.getSpawnLevel() == Level.OVERWORLD, "a spawn point each: " + spawns.order());
        check(home.equals(settled.getRespawnPosition()) && refused.getRespawnPosition() == null,
                "only the first player's spawn point is set");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aGameModeChangeIsTheListenersToChange(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        Probe<PlayerEvent.PlayerChangeGameModeEvent> changes = probe(f, PlayerEvent.PlayerChangeGameModeEvent.class,
                e -> e.getEntity() == player, e -> e.setNewGameMode(GameType.ADVENTURE));
        player.setGameMode(GameType.CREATIVE);
        PlayerEvent.PlayerChangeGameModeEvent change = changes.once();
        check(change.getCurrentGameMode() == GameType.SURVIVAL, "from survival");
        check(player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE, "the listener's mode wins: "
                + player.gameMode.getGameModeForPlayer());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void travelToAnotherDimensionIsTheListenersToStop(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Pig pig = pig(helper, 6.5, 5.5);
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        Probe<EntityTravelToDimensionEvent> travels = probe(f, EntityTravelToDimensionEvent.class,
                e -> e.getEntity() == pig, BridgeActionSupport::cancel);
        check(nether != null, "the server has a Nether");
        Object moved = pig.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(nether,
                pig.position(), Vec3.ZERO, 0.0F, 0.0F,
                        net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        check(travels.once().getDimension() == Level.NETHER, "the travel names the Nether");
        check(moved == null && pig.level() == level && !pig.isRemoved(), "a stopped travel leaves the pig here");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 40)
    public static void theServerTicksPostTheirEvents(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        Probe<ServerTickEvent.Pre> pre = probe(f, ServerTickEvent.Pre.class, e -> e.getServer() == server);
        Probe<ServerTickEvent.Post> post = probe(f, ServerTickEvent.Post.class, e -> e.getServer() == server);
        helper.succeedWhen(() -> check(pre.count() >= 2 && post.count() >= 2, "two server ticks: " + pre.count()
                + ", " + post.count()));
    }
}

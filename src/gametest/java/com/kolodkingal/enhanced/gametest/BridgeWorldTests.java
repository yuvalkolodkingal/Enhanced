package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.gametest.BridgeTestSupport.Bridged;
import com.kolodkingal.enhanced.platform.event.BlockDropsEvent;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.EntityJoinLevelEvent;
import com.kolodkingal.enhanced.platform.event.EntityLeaveLevelEvent;
import com.kolodkingal.enhanced.platform.event.EntityMobGriefingEvent;
import com.kolodkingal.enhanced.platform.event.EntityMountEvent;
import com.kolodkingal.enhanced.platform.event.EntityStruckByLightningEvent;
import com.kolodkingal.enhanced.platform.event.EntityTeleportEvent;
import com.kolodkingal.enhanced.platform.event.EntityTickEvent;
import com.kolodkingal.enhanced.platform.event.EntityTravelToDimensionEvent;
import com.kolodkingal.enhanced.platform.event.ExplosionEvent;
import com.kolodkingal.enhanced.platform.event.ExplosionKnockbackEvent;
import com.kolodkingal.enhanced.platform.event.FMLCommonSetupEvent;
import com.kolodkingal.enhanced.platform.event.ItemAttributeModifierEvent;
import com.kolodkingal.enhanced.platform.event.LevelEvent;
import com.kolodkingal.enhanced.platform.event.LevelTickEvent;
import com.kolodkingal.enhanced.platform.event.OnDatapackSyncEvent;
import com.kolodkingal.enhanced.platform.event.PistonEvent;
import com.kolodkingal.enhanced.platform.event.ProjectileImpactEvent;
import com.kolodkingal.enhanced.platform.event.RegisterCommandsEvent;
import com.kolodkingal.enhanced.platform.event.ServerStartedEvent;
import com.kolodkingal.enhanced.platform.event.ServerStartingEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppingEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.kolodkingal.enhanced.gametest.BridgeTestSupport.BATCH;
import static com.kolodkingal.enhanced.gametest.BridgeTestSupport.adapted;
import static com.kolodkingal.enhanced.gametest.BridgeTestSupport.bridge;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * BridgeTests of the entity, tick, server, level and block events (port P0, S-A; {@link BridgeTestSupport}). The
 * server's lifecycle and ticks, a level's tick and unloading, and the mod's setup are checked through the adapter only:
 * posting them on the live server would run the mod's (and NeoForge's) real work for them.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BridgeWorldTests {
    private BridgeWorldTests() {
    }

    private static Zombie zombie(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 8.5));
    }

    private static BlockPos at(GameTestHelper helper, int x, int y, int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    // ---- entities

    @GameTest(template = ARENA, batch = BATCH)
    public static void entityJoinLevel(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Bridged<EntityJoinLevelEvent, net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> b =
                bridge(EntityJoinLevelEvent.class, () -> new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(
                        zombie, helper.getLevel(), true));
        check(b.common().loadedFromDisk(), "loaded from disk");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void entityLeaveLevel(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        bridge(EntityLeaveLevelEvent.class, () -> new net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent(zombie,
                helper.getLevel()));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void entityMobGriefing(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Bridged<EntityMobGriefingEvent, net.neoforged.neoforge.event.entity.EntityMobGriefingEvent> b =
                bridge(EntityMobGriefingEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.EntityMobGriefingEvent(helper.getLevel(), zombie));
        b.common().setCanGrief(false);
        check(!b.neo().canGrief(), "setCanGrief reaches NeoForge");
        b.common().setCanGrief(true);
        check(b.neo().canGrief(), "both ways");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void entityMount(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Zombie horse = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(4.5, FLOOR, 4.5));
        bridge(EntityMountEvent.class, () -> new net.neoforged.neoforge.event.entity.EntityMountEvent(zombie, horse,
                helper.getLevel(), true));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void entityStruckByLightning(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        bridge(EntityStruckByLightningEvent.class,
                () -> new net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent(zombie, bolt));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void chorusFruitTeleport(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Vec3 to = zombie.position().add(1.0, 0.0, 1.0);
        Bridged<EntityTeleportEvent.ChorusFruit, net.neoforged.neoforge.event.entity.EntityTeleportEvent.ChorusFruit> b =
                bridge(EntityTeleportEvent.ChorusFruit.class,
                        () -> new net.neoforged.neoforge.event.entity.EntityTeleportEvent.ChorusFruit(zombie, to.x,
                                to.y, to.z));
        b.common().setTargetX(to.x + 2.0);
        check(b.neo().getTargetX() == to.x + 2.0, "setTargetX reaches NeoForge");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void enderEntityTeleport(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Vec3 to = zombie.position().add(1.0, 0.0, 1.0);
        Bridged<EntityTeleportEvent.EnderEntity, net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity> b =
                bridge(EntityTeleportEvent.EnderEntity.class,
                        () -> new net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity(zombie, to.x,
                                to.y, to.z));
        b.common().setTargetZ(to.z - 2.0);
        check(b.neo().getTargetZ() == to.z - 2.0, "setTargetZ reaches NeoForge");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void enderPearlTeleport(GameTestHelper helper) {
        ServerPlayer p = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        ThrownEnderpearl pearl = new ThrownEnderpearl(helper.getLevel(), p);
        Vec3 to = p.position().add(2.0, 0.0, 0.0);
        Bridged<EntityTeleportEvent.EnderPearl, net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl> b =
                bridge(EntityTeleportEvent.EnderPearl.class,
                        () -> new net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl(p, to.x, to.y,
                                to.z, pearl, 5.0F, null));
        check(b.common().getPlayer() == p && b.common().getPearlEntity() == pearl, "the player and the pearl");
        b.common().setAttackDamage(0.0F);
        check(b.neo().getAttackDamage() == 0.0F, "setAttackDamage reaches NeoForge");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void entityTravelToDimension(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        bridge(EntityTravelToDimensionEvent.class,
                () -> new net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent(zombie, Level.NETHER));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void projectileImpact(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        arrow.setPos(zombie.position().add(0.0, 1.0, -1.0));
        EntityHitResult hit = new EntityHitResult(zombie);
        Bridged<ProjectileImpactEvent, net.neoforged.neoforge.event.entity.ProjectileImpactEvent> b =
                bridge(ProjectileImpactEvent.class, () -> new net.neoforged.neoforge.event.entity.ProjectileImpactEvent(
                        arrow, hit));
        check(b.common().getProjectile() == arrow && b.common().getRayTraceResult() == hit, "the arrow and the hit");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void entityTickPre(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        bridge(EntityTickEvent.Pre.class, () -> new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(zombie));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void entityTickPost(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        bridge(EntityTickEvent.Post.class, () -> new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(zombie));
        helper.succeed();
    }

    // ---- ticks and the server (the adapter only)

    @GameTest(template = ARENA, batch = BATCH)
    public static void levelTicks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Bridged<LevelTickEvent.Pre, ?> pre = adapted(LevelTickEvent.Pre.class,
                new net.neoforged.neoforge.event.tick.LevelTickEvent.Pre(() -> true, level));
        check(pre.common().getLevel() == level && pre.common().hasTime(), "the level, with time left");
        Bridged<LevelTickEvent.Post, ?> post = adapted(LevelTickEvent.Post.class,
                new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(() -> false, level));
        check(post.common().getLevel() == level && !post.common().hasTime(), "the level, without time left");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void serverTicks(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Bridged<ServerTickEvent.Pre, ?> pre = adapted(ServerTickEvent.Pre.class,
                new net.neoforged.neoforge.event.tick.ServerTickEvent.Pre(() -> true, server));
        check(pre.common().getServer() == server && pre.common().hasTime(), "the server, with time left");
        Bridged<ServerTickEvent.Post, ?> post = adapted(ServerTickEvent.Post.class,
                new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(() -> false, server));
        check(post.common().getServer() == server && !post.common().hasTime(), "the server, without time left");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void serverLifecycle(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        check(adapted(ServerStartingEvent.class, new net.neoforged.neoforge.event.server.ServerStartingEvent(server))
                .common().getServer() == server, "starting");
        check(adapted(ServerStartedEvent.class, new net.neoforged.neoforge.event.server.ServerStartedEvent(server))
                .common().getServer() == server, "started");
        check(adapted(ServerStoppingEvent.class, new net.neoforged.neoforge.event.server.ServerStoppingEvent(server))
                .common().getServer() == server, "stopping");
        check(adapted(ServerStoppedEvent.class, new net.neoforged.neoforge.event.server.ServerStoppedEvent(server))
                .common().getServer() == server, "stopped");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void commonSetup(GameTestHelper helper) {
        net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent neo = new net.neoforged.fml.event.lifecycle
                .FMLCommonSetupEvent(ModList.get().getModContainerById(Enhanced.MODID).orElseThrow(),
                new DeferredWorkQueue("bridge test"));
        Bridged<FMLCommonSetupEvent, ?> b = adapted(FMLCommonSetupEvent.class, neo, "*none*");
        AtomicBoolean ran = new AtomicBoolean();
        check(b.common().enqueueWork(() -> ran.set(true)) != null, "enqueueWork goes to NeoForge's queue");
        check(!ran.get(), "which runs it later, not now");
        helper.succeed();
    }

    // ---- levels

    @GameTest(template = ARENA, batch = BATCH)
    public static void levelLoad(GameTestHelper helper) {
        bridge(LevelEvent.Load.class, () -> new net.neoforged.neoforge.event.level.LevelEvent.Load(helper.getLevel()));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void levelSave(GameTestHelper helper) {
        bridge(LevelEvent.Save.class, () -> new net.neoforged.neoforge.event.level.LevelEvent.Save(helper.getLevel()));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void levelUnload(GameTestHelper helper) {
        check(adapted(LevelEvent.Unload.class, new net.neoforged.neoforge.event.level.LevelEvent.Unload(
                helper.getLevel())).common().getLevel() == helper.getLevel(), "the level");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void explosionDetonate(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Vec3 c = zombie.position().add(1.0, 0.0, 0.0);
        Explosion explosion = new Explosion(helper.getLevel(), null, c.x, c.y, c.z, 2.0F, false,
                Explosion.BlockInteraction.KEEP);
        List<Entity> hit = new ArrayList<>(List.of(zombie));
        Bridged<ExplosionEvent.Detonate, net.neoforged.neoforge.event.level.ExplosionEvent.Detonate> b =
                bridge(ExplosionEvent.Detonate.class, () -> new net.neoforged.neoforge.event.level.ExplosionEvent
                        .Detonate(helper.getLevel(), explosion, hit));
        check(b.common().getAffectedEntities() == hit, "the entities are NeoForge's own list");
        check(b.common().getAffectedBlocks() == explosion.getToBlow(), "the blocks are the explosion's own list");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void explosionKnockback(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        Vec3 c = zombie.position().add(1.0, 0.0, 0.0);
        Explosion explosion = new Explosion(helper.getLevel(), null, c.x, c.y, c.z, 2.0F, false,
                Explosion.BlockInteraction.KEEP);
        Bridged<ExplosionKnockbackEvent, net.neoforged.neoforge.event.level.ExplosionKnockbackEvent> b =
                bridge(ExplosionKnockbackEvent.class, () -> new net.neoforged.neoforge.event.level
                        .ExplosionKnockbackEvent(helper.getLevel(), explosion, zombie, new Vec3(-1.0, 0.2, 0.0)));
        b.common().setKnockbackVelocity(Vec3.ZERO);
        check(b.neo().getKnockbackVelocity().equals(Vec3.ZERO), "setKnockbackVelocity reaches NeoForge");
        helper.succeed();
    }

    // ---- blocks

    @GameTest(template = ARENA, batch = BATCH)
    public static void blockBreak(GameTestHelper helper) {
        ServerPlayer p = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        BlockPos pos = at(helper, 6, 1, 6);
        bridge(BlockEvent.BreakEvent.class, () -> new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), p));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void blockEntityPlace(GameTestHelper helper) {
        ServerPlayer p = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        BlockPos pos = at(helper, 6, 2, 6);
        BlockState floor = helper.getLevel().getBlockState(pos.below());
        Bridged<BlockEvent.EntityPlaceEvent, net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> b =
                bridge(BlockEvent.EntityPlaceEvent.class,
                        () -> new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(BlockSnapshot.create(
                                helper.getLevel().dimension(), helper.getLevel(), pos), floor, p));
        check(b.common().getPlacedAgainst() == floor && b.common().getEntity() == p, "against what, by whom");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void farmlandTrample(GameTestHelper helper) {
        Zombie zombie = zombie(helper);
        BlockPos pos = at(helper, 8, 1, 8);
        bridge(BlockEvent.FarmlandTrampleEvent.class,
                () -> new net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent(helper.getLevel(), pos,
                        Blocks.FARMLAND.defaultBlockState(), 2.0F, zombie));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void fluidPlaceBlock(GameTestHelper helper) {
        BlockPos pos = at(helper, 6, 2, 6);
        Bridged<BlockEvent.FluidPlaceBlockEvent, net.neoforged.neoforge.event.level.BlockEvent.FluidPlaceBlockEvent> b =
                bridge(BlockEvent.FluidPlaceBlockEvent.class,
                        () -> new net.neoforged.neoforge.event.level.BlockEvent.FluidPlaceBlockEvent(helper.getLevel(),
                                pos, pos.above(), Blocks.COBBLESTONE.defaultBlockState()));
        b.common().setNewState(Blocks.STONE.defaultBlockState());
        check(b.neo().getNewState().is(Blocks.STONE), "setNewState reaches NeoForge");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void neighborNotify(GameTestHelper helper) {
        BlockPos pos = at(helper, 6, 1, 6);
        bridge(BlockEvent.NeighborNotifyEvent.class,
                () -> new net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent(helper.getLevel(), pos,
                        helper.getLevel().getBlockState(pos), EnumSet.of(Direction.UP, Direction.DOWN), false));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void portalSpawn(GameTestHelper helper) {
        BlockPos pos = at(helper, 6, 2, 6);
        PortalShape shape = new PortalShape(helper.getLevel(), pos, Direction.Axis.X);
        bridge(BlockEvent.PortalSpawnEvent.class,
                () -> new net.neoforged.neoforge.event.level.BlockEvent.PortalSpawnEvent(helper.getLevel(), pos,
                        Blocks.AIR.defaultBlockState(), shape));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void blockToolModification(GameTestHelper helper) {
        ServerPlayer p = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        BlockPos pos = at(helper, 6, 1, 6);
        UseOnContext context = new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        Bridged<BlockEvent.BlockToolModificationEvent,
                net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent> b =
                bridge(BlockEvent.BlockToolModificationEvent.class,
                        () -> new net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent(log, context,
                                ItemAbilities.AXE_STRIP, false));
        check(b.common().getPlayer() == p && b.common().getHeldItemStack().is(Items.IRON_AXE), "who, with what");
        b.common().setFinalState(Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        check(b.neo().getFinalState().is(Blocks.STRIPPED_OAK_LOG), "setFinalState reaches NeoForge");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void blockDrops(GameTestHelper helper) {
        ServerPlayer p = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        BlockPos pos = at(helper, 6, 1, 6);
        Bridged<BlockDropsEvent, net.neoforged.neoforge.event.level.BlockDropsEvent> b =
                bridge(BlockDropsEvent.class, () -> new net.neoforged.neoforge.event.level.BlockDropsEvent(
                        helper.getLevel(), pos, helper.getLevel().getBlockState(pos), null, new ArrayList<>(), p,
                        ItemStack.EMPTY));
        b.common().setDroppedExperience(4);
        check(b.neo().getDroppedExperience() == 4, "setDroppedExperience reaches NeoForge");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void pistonPre(GameTestHelper helper) {
        BlockPos pos = at(helper, 6, 2, 6);
        Bridged<PistonEvent.Pre, net.neoforged.neoforge.event.level.PistonEvent.Pre> b =
                bridge(PistonEvent.Pre.class, () -> new net.neoforged.neoforge.event.level.PistonEvent.Pre(
                        helper.getLevel(), pos, Direction.UP,
                        net.neoforged.neoforge.event.level.PistonEvent.PistonMoveType.EXTEND));
        check(b.common().getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND, "it extends");
        helper.succeed();
    }

    // ---- the rest

    @GameTest(template = ARENA, batch = BATCH)
    public static void registerCommands(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommandBuildContext context = CommandBuildContext.simple(server.registryAccess(),
                server.getWorldData().enabledFeatures());
        Bridged<RegisterCommandsEvent, net.neoforged.neoforge.event.RegisterCommandsEvent> b =
                bridge(RegisterCommandsEvent.class, () -> new net.neoforged.neoforge.event.RegisterCommandsEvent(
                        new CommandDispatcher<CommandSourceStack>(), Commands.CommandSelection.DEDICATED, context));
        check(b.common().getCommandSelection() == Commands.CommandSelection.DEDICATED, "the selection");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void datapackSync(GameTestHelper helper) {
        ServerPlayer p = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        MinecraftServer server = helper.getLevel().getServer();
        Bridged<OnDatapackSyncEvent, net.neoforged.neoforge.event.OnDatapackSyncEvent> b =
                bridge(OnDatapackSyncEvent.class, () -> new net.neoforged.neoforge.event.OnDatapackSyncEvent(
                        server.getPlayerList(), p));
        check(b.common().getRelevantPlayers().toList().equals(List.of(p)), "only the player who joined");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void itemAttributeModifiers(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemAttributeModifiers defaults = sword.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY);
        Bridged<ItemAttributeModifierEvent, net.neoforged.neoforge.event.ItemAttributeModifierEvent> b =
                bridge(ItemAttributeModifierEvent.class,
                        () -> new net.neoforged.neoforge.event.ItemAttributeModifierEvent(sword, defaults));
        AttributeModifier extra = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Enhanced.MODID,
                "bridge_test"), 3.0, AttributeModifier.Operation.ADD_VALUE);
        b.common().replaceModifier(Attributes.ATTACK_DAMAGE, extra, EquipmentSlotGroup.MAINHAND);
        check(b.neo().getModifiers().stream().anyMatch(e -> e.modifier().equals(extra)),
                "replaceModifier reaches NeoForge");
        helper.succeed();
    }
}

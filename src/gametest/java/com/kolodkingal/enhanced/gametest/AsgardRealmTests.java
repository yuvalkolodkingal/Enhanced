package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.asgard.AsgardChunkGenerator;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardRules;
import com.kolodkingal.enhanced.asgard.AsgardSky;
import com.kolodkingal.enhanced.asgard.AsgardTerrain;
import com.kolodkingal.enhanced.asgard.CityBuild;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.asgard.FeastHall;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.asgard.RuneStones;
import com.kolodkingal.enhanced.asgard.SeatEntity;
import com.kolodkingal.enhanced.block.asgard.AsgardMuralBlock;
import com.kolodkingal.enhanced.block.asgard.BifrostMechanismBlockEntity;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.Events;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The realm of Asgard (3.5, lane L1): the land ({@link AsgardTerrain}), the plan of the city and its templates, the
 * generated city at its fixed points, the sky's colours (batch {@code asgard_realm}); and the rules: the protected city,
 * the void catch, no monsters by nature, no spawn point, the inventory kept, the soft edge, no Nether portal, the
 * rune stones (batch {@code asgard_rules}).
 */
public final class AsgardRealmTests {
    static final String REALM = "asgard_realm";
    static final String RULES = "asgard_rules";

    /** Loads (generating if need be) the chunk holding {@code pos}. */
    private static BlockState at(ServerLevel level, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        return level.getBlockState(pos);
    }

    // ---- the land ----

    @EnhancedTest(template = ARENA, batch = REALM)
    public static void theLandTheSeaTheVoidAndTheUnderside(GameTestHelper helper) {
        AsgardTerrain.Column middle = AsgardTerrain.column(0, 0);
        check(middle.kind() == AsgardTerrain.Kind.LAND && middle.top() == 77 && middle.pad() != null, "land at (0, 0), the plaza at 77: " + middle);
        check(AsgardTerrain.column(0, -100).top() == 81, "the palace's pad at 81");
        AsgardTerrain.Column sea = AsgardTerrain.column(300, 0);
        check(sea.kind() == AsgardTerrain.Kind.SEA && sea.top() < CityPlan.SEA_LEVEL, "the sea at r 300: " + sea);
        check(AsgardChunkGenerator.block(sea, 300, CityPlan.SEA_LEVEL, 0, 0).is(Blocks.WATER)
                && AsgardChunkGenerator.block(sea, 300, CityPlan.SEA_LEVEL + 1, 0, 0) == null, "water up to y 62, air above");
        check(AsgardTerrain.column(500, 0).kind() == AsgardTerrain.Kind.VOID, "the void at r 500");
        check(AsgardTerrain.column(0, -379).kind() == AsgardTerrain.Kind.LIP, "the stone lip at r 379");
        AsgardTerrain.Column falls = AsgardTerrain.column(0, -382);
        check(falls.kind() == AsgardTerrain.Kind.FALLS && falls.top() == CityPlan.SEA_LEVEL && falls.bottom() < 30, "the falls at r 381: " + falls);
        check(middle.bottom() < 0 && AsgardTerrain.column(0, -100).bottom() < 0, "the underside under the palace goes below 0");
        check(AsgardTerrain.underside(370, 0) > 20, "and thins to the rim");
        check(AsgardTerrain.column((int) AsgardTerrain.CHASM_X, (int) AsgardTerrain.CHASM_Z).kind() == AsgardTerrain.Kind.VOID,
                "the chasm west of the palace goes through the land");
        check(AsgardTerrain.column(-170, 170).kind() == AsgardTerrain.Kind.LAND && AsgardTerrain.column(-170, 170).top() == 65,
                "the land reaches out to the stables (r 240)");
        helper.succeed();
    }

    // ---- the plan ----

    @EnhancedTest(template = ARENA, batch = REALM)
    public static void thePiecesOfThePlanStandOnTheLandAndTheirTemplatesLoad(GameTestHelper helper) {
        List<CityPlan.Piece> pieces = CityPlan.pieces();
        check(pieces.size() >= 100, "about a hundred and more pieces: " + pieces.size());
        Set<String> used = new HashSet<>();
        for (CityPlan.Piece p : pieces) {
            used.add(p.template());
            BoundingBox b = p.box();
            boolean overVoidOrSea = p.template().equals("observatory") || p.template().equals("docks") || p.template().startsWith("skiff");
            if (!overVoidOrSea) {
                for (int[] c : new int[][]{{b.minX(), b.minZ()}, {b.maxX(), b.minZ()}, {b.minX(), b.maxZ()}, {b.maxX(), b.maxZ()}}) {
                    check(CityPlan.padAt(c[0], c[1]) != null, p.template() + " stands on a district: corner " + c[0] + ", " + c[1]);
                }
            }
        }
        check(used.containsAll(CityPlan.TEMPLATE_SIZES.keySet()), "every template is used");
        // no two pieces fill the same space (the vault's stair runs under the steps, its box only)
        for (int i = 0; i < pieces.size(); i++) {
            for (int j = i + 1; j < pieces.size(); j++) {
                CityPlan.Piece a = pieces.get(i);
                CityPlan.Piece b = pieces.get(j);
                if (a.template().equals("vault_stair") || b.template().equals("vault_stair")) {
                    continue;
                }
                check(!a.box().intersects(b.box()), a.template() + " and " + b.template() + " overlap: " + a.box() + " " + b.box());
            }
        }
        for (var e : CityPlan.TEMPLATE_SIZES.entrySet()) {
            Optional<StructureTemplate> t = helper.getLevel().getStructureManager().get(Enhanced.id("asgard/" + e.getKey()));
            check(t.isPresent(), "the template " + e.getKey() + " exists");
            Vec3i size = t.get().getSize();
            check(size.equals(e.getValue()), e.getKey() + " is " + e.getValue() + ": " + size);
        }
        check(helper.getLevel().getStructureManager().get(Enhanced.id("bifrost_site/site")).map(StructureTemplate::getSize)
                .filter(s -> s.equals(new Vec3i(15, 6, 15))).isPresent(), "the rune site's template is 15 x 6 x 15");
        check(CityPlan.homes().size() == 8 + CityPlan.EINHERJAR_COUNT + CityPlan.CITIZEN_COUNT, "a home for every Asgardian");
        check(CityPlan.hidingSpots().size() == 24 && new HashSet<>(CityPlan.hidingSpots()).size() == 24, "24 hiding places");
        check(CityPlan.isProtected(CityPlan.THRONE) && CityPlan.isProtected(new BlockPos(0, 72, 300))
                && !CityPlan.isProtected(new BlockPos(150, 70, -150)), "the throne and the bridge are protected, the wild land is not");
        helper.succeed();
    }

    // ---- the city, generated ----

    @EnhancedTest(template = ARENA, batch = REALM, timeoutTicks = 400)
    public static void theGeneratedCityHasItsFixedPoints(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel asgard = AsgardTestSupport.withArrival(server);
        check(at(asgard, CityPlan.THRONE).is(ModBlocks.ODIN_THRONE.get())
                && at(asgard, CityPlan.THRONE).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == Direction.SOUTH,
                "the throne at its place, facing the hall: " + at(asgard, CityPlan.THRONE));
        check(at(asgard, CityPlan.HELA_SEAL).is(ModBlocks.HELA_SEAL.get()), "Hela's Seal behind it");
        check(at(asgard, CityPlan.OBSERVATORY_SOCKET).is(ModBlocks.HOFUND_SOCKET.get()), "the Hofund socket in the Observatory");
        check(at(asgard, CityPlan.MECHANISM).is(ModBlocks.BIFROST_MECHANISM.get()), "the mechanism under it");
        check(at(asgard, CityPlan.GUNGNIR_STAND).is(ModBlocks.GUNGNIR_STAND.get()), "Gungnir's stand in the vault");
        check(at(asgard, CityPlan.VAULT_DOOR.getCenter().offset(0, 0, 0)).is(ModBlocks.ASGARD_GOLD_PLATING.get()), "the vault's door is shut");
        BlockPos arrival = BlockPos.containing(CityPlan.ARRIVAL);
        check(!at(asgard, arrival.below()).isAir() && at(asgard, arrival).isAir() && at(asgard, arrival.above()).isAir(), "the landing is clear");
        for (int z = 210; z <= 420; z += 30) {
            check(at(asgard, new BlockPos(0, CityPlan.BRIDGE_Y, z)).is(ModBlocks.BIFROST_BRIDGE.get()), "the bridge at z " + z);
        }
        check(at(asgard, new BlockPos(0, 50, -382)).is(ModBlocks.COSMIC_FALLS.get()), "the falls at r 381");
        check(at(asgard, CityPlan.THRONE_ROOM_CENTER).getCollisionShape(asgard, CityPlan.THRONE_ROOM_CENTER).max(Direction.Axis.Y) < 0.1
                && at(asgard, CityPlan.THRONE_ROOM_CENTER.above()).isAir() && !at(asgard, CityPlan.THRONE_ROOM_CENTER.below()).isAir(),
                "the throne room's floor at 82");
        // (integration: lane L4's dais, where Hela forms and channels, stands on lane L1's dais before the throne, with room)
        net.minecraft.world.phys.Vec3 dais = com.kolodkingal.enhanced.hela.HelaLane.dais(asgard);
        check(dais.y > CityPlan.THRONE_ROOM_FLOOR && dais.y <= CityPlan.THRONE.getY() + 1 && dais.z < CityPlan.THRONE.getZ() + 6
                && asgard.noCollision(new net.minecraft.world.phys.AABB(dais.x - 0.4, dais.y + 0.01, dais.z - 0.4,
                        dais.x + 0.4, dais.y + 2.5, dais.z + 0.4)), "Hela's place on the throne's dais: " + dais);
        BifrostMechanismBlockEntity mechanism = (BifrostMechanismBlockEntity) asgard.getBlockEntity(CityPlan.MECHANISM);
        check(AsgardRules.spinNear(asgard, CityPlan.ARRIVAL) && mechanism.spinStart() == asgard.getGameTime(), "the Bifrost spins the mechanism");
        check(!AsgardRules.spinNear(asgard, CityPlan.ARRIVAL.add(0, 0, -60)), "but not from far away");
        // the murals swap to Hela's conquests and back
        int peace = murals(asgard, false);
        check(peace >= 100, "the throne room is painted: " + peace);
        CityBuild.setMurals(asgard, true);
        check(murals(asgard, true) == peace && murals(asgard, false) == 0, "all to the conquest");
        CityBuild.setMurals(asgard, false);
        check(murals(asgard, false) == peace, "and back to peace");
        CityBuild.setVaultDoor(asgard, true);
        check(asgard.getBlockState(new BlockPos(0, 72, -106)).isAir(), "the vault opens");
        CityBuild.setVaultDoor(asgard, false);
        check(asgard.getBlockState(new BlockPos(0, 72, -106)).is(ModBlocks.ASGARD_GOLD_PLATING.get()), "and shuts");
        helper.succeed();
    }

    private static int murals(ServerLevel level, boolean conquest) {
        int n = 0;
        BoundingBox room = CityPlan.THRONE_ROOM;
        for (BlockPos pos : BlockPos.betweenClosed(room.minX(), room.minY(), room.minZ(), room.maxX(), room.maxY(), room.maxZ())) {
            BlockState s = at(level, pos);
            if (s.getBlock() instanceof AsgardMuralBlock && s.getValue(AsgardMuralBlock.CONQUEST) == conquest) {
                n++;
            }
        }
        return n;
    }

    @EnhancedTest(template = ARENA, batch = REALM)
    public static void theSkysColoursByTheTimeOfDay(GameTestHelper helper) {
        check(AsgardSky.dayColour(0.0F) == AsgardSky.DAY && AsgardSky.dayColour(0.5F) == AsgardSky.NIGHT, "blue at noon, deep space at midnight");
        check(Math.abs(AsgardSky.nebulaAlpha(0.0F) - 0.35F) < 1e-4 && Math.abs(AsgardSky.nebulaAlpha(0.5F) - 1.0F) < 1e-4,
                "the nebula faint by day, full at night");
        check(AsgardSky.nightness(0.0F) == 0.0F && AsgardSky.nightness(0.5F) == 1.0F
                && AsgardSky.nightness(0.22F) > 0 && AsgardSky.nightness(0.22F) < 1, "dusk between");
        check(BifrostMechanismBlockEntity.angle(0, 30) != BifrostMechanismBlockEntity.angle(0, 31)
                && Math.abs(BifrostMechanismBlockEntity.angle(-1000, 100.5) - BifrostMechanismBlockEntity.angle(-1000, 100.0)) < 1.0,
                "the ring turns smoothly, faster while it spins");
        helper.succeed();
    }

    // ---- the rules ----

    @EnhancedTest(template = ARENA, batch = RULES)
    public static void theCityIsProtectedTheWildLandIsNot(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerLevel asgard = AsgardTestSupport.ensureAsgard(helper.getLevel().getServer());
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        BlockPos city = new BlockPos(0, 77, 10);
        BlockPos wild = new BlockPos(150, 70, -150);
        at(asgard, city);
        at(asgard, wild);
        check(AsgardRules.protectsCity(player, asgard, city) && !AsgardRules.protectsCity(player, asgard, wild), "the plaza is the city");
        check(!BlockRights.mayBreak(player, asgard, city, asgard.getBlockState(city)) && !BlockRights.mayPlace(player, asgard, city.above()),
                "no breaking or placing in the city");
        check(BlockRights.massEditRefuses(player, asgard, city), "Build Magic and the Magnetic Pull are refused there");
        check(!AsgardRules.protectsCity(player, helper.getLevel(), city), "only in Asgard");
        ServerPlayer op = f.player(7.5, FLOOR, 5.5, GameType.CREATIVE);
        MinecraftServer server = helper.getLevel().getServer();
        server.getPlayerList().getOps().add(new ServerOpListEntry(op.getGameProfile(), 2, false));
        f.onClose(() -> server.getPlayerList().getOps().remove(op.getGameProfile()));
        check(!AsgardRules.protectsCity(op, asgard, city), "an operator in Creative may edit");
        check(AsgardRules.protectsCity(null, asgard, city), "explosions and creatures may not");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = RULES, timeoutTicks = 200)
    public static void heimdallCatchesAFallIntoTheVoidAndTheEdgePushesBack(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel asgard = AsgardTestSupport.withArrival(server);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        server.getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), 2, false));
        player.teleportTo(asgard, 500.5, -60, 0.5, 0, 0);
        server.getPlayerList().getOps().remove(player.getGameProfile());
        check(AsgardDimension.in(player), "he is in Asgard, falling under the void's line");
        float health = player.getHealth();
        player.fallDistance = 100;
        Events.post(new PlayerTickEvent.Post(player));
        check(player.position().distanceTo(CityPlan.ARRIVAL) < 0.01 && player.fallDistance == 0 && player.getHealth() == health,
                "Heimdall caught him: in the Observatory, unhurt: " + player.position());
        check("message.enhanced.asgard.rescued".equals(BossTestSupport.lastActionBarKey(player)), "and he is told");
        player.teleportTo(asgard, 700.5, 120, 0.5, 0, 0);
        check(AsgardRules.pushFromEdge(player) && player.getDeltaMovement().x < 0, "beyond the edge he is pushed back toward the city");
        player.teleportTo(asgard, 100.5, 120, 0.5, 0, 0);
        check(!AsgardRules.pushFromEdge(player), "not inside it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = RULES)
    public static void noMonstersByNatureNoSpawnPointNoPortal(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerLevel asgard = AsgardTestSupport.withArrival(helper.getLevel().getServer());
        Zombie natural = new Zombie(EntityType.ZOMBIE, asgard);
        natural.moveTo(CityPlan.ARRIVAL);
        Platform.get().hooks().finalizeMobSpawn(natural, asgard, asgard.getCurrentDifficultyAt(natural.blockPosition()), MobSpawnType.NATURAL, null);
        check(natural.isSpawnCancelled(), "no zombie is born there by nature");
        Zombie egg = new Zombie(EntityType.ZOMBIE, asgard);
        egg.moveTo(CityPlan.ARRIVAL);
        Platform.get().hooks().finalizeMobSpawn(egg, asgard, asgard.getCurrentDifficultyAt(egg.blockPosition()), MobSpawnType.SPAWN_EGG, null);
        check(!egg.isSpawnCancelled(), "a spawn egg still works");
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        player.setRespawnPosition(AsgardDimension.KEY, BlockPos.containing(CityPlan.ARRIVAL), 0, false, false);
        check(player.getRespawnDimension() != AsgardDimension.KEY, "a bed never sets the spawn in Asgard");
        player.setRespawnPosition(AsgardDimension.KEY, BlockPos.containing(CityPlan.ARRIVAL), 0, true, false);
        check(player.getRespawnDimension() == AsgardDimension.KEY, "a command (forced) still does");
        player.setRespawnPosition(helper.getLevel().dimension(), null, 0, true, false);
        // a Nether portal frame of obsidian, out over the void
        BlockPos base = new BlockPos(2, 100, 470);
        at(asgard, base);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 5; y++) {
                if (x == 0 || x == 3 || y == 0 || y == 4) {
                    asgard.setBlock(base.offset(x, y, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
                }
            }
        }
        f.onClose(() -> {
            for (BlockPos p : BlockPos.betweenClosed(base, base.offset(3, 4, 0))) {
                asgard.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        });
        Optional<PortalShape> shape = PortalShape.findEmptyPortalShape(asgard, base.offset(1, 1, 0), Direction.Axis.X);
        check(shape.isPresent(), "a good frame");
        BlockPos inFrame = base.offset(1, 1, 0);
        check(Events.post(new BlockEvent.PortalSpawnEvent(asgard, inFrame, asgard.getBlockState(inFrame), shape.get())).isCanceled(),
                "but no portal is lit in Asgard");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = RULES)
    public static void whoDiesInAsgardKeepsHisThings(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel asgard = AsgardTestSupport.withArrival(server);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        server.getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), 2, false));
        player.teleportTo(asgard, CityPlan.ARRIVAL.x, CityPlan.ARRIVAL.y, CityPlan.ARRIVAL.z, 0, 0);
        server.getPlayerList().getOps().remove(player.getGameProfile());
        player.getInventory().setItem(3, new ItemStack(Items.DIAMOND, 7));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HOFUND.get()));
        player.experienceLevel = 5;
        player.die(asgard.damageSources().generic());
        int dropped = asgard.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(8)).size();
        check(dropped == 0, "nothing falls to the ground: " + dropped);
        ServerPlayer back = f.player(5.5, FLOOR, 7.5);
        Events.post(new PlayerEvent.Clone(back, player, true));
        check(back.getInventory().getItem(3).is(Items.DIAMOND) && back.getInventory().getItem(3).getCount() == 7
                && back.getInventory().countItem(ModItems.HOFUND.get()) == 1 && back.experienceLevel == 5, "he comes back with all of it");
        // (with asgard.keepInventory off, only these stay)
        check(AsgardRules.alwaysKept(new ItemStack(ModItems.HOFUND.get())) && AsgardRules.alwaysKept(new ItemStack(ModItems.MJOLNIR.get()))
                && !AsgardRules.alwaysKept(new ItemStack(Items.DIAMOND)), "the keys are never lost");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = RULES, timeoutTicks = 300)
    public static void theRuneStoneTakesTheCircleToAsgardAndRests(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerLevel asgard = AsgardTestSupport.withArrival(helper.getLevel().getServer());
        BlockPos stone = helper.absolutePos(new BlockPos(10, 2, 10));
        helper.getLevel().setBlock(stone, ModBlocks.BIFROST_RUNE_STONE.get().defaultBlockState(), 3);
        ServerPlayer caller = f.player(10.5, FLOOR, 8.5);
        ServerPlayer friend = f.player(12.5, FLOOR, 8.5);
        check(RuneStones.use(caller, stone), "the stone calls the Bifrost");
        check(!RuneStones.use(caller, stone), "and then rests");
        check("message.enhanced.asgard.rune_resting".equals(BossTestSupport.lastActionBarKey(caller)), "he is told it rests");
        f.everyTick(200, tick -> {
            PowerTicker.tick(caller);
            PowerTicker.tick(friend);
            if (!AsgardDimension.in(caller)) {
                return false;
            }
            check(AsgardDimension.in(friend), "his friend in the circle comes along");
            check(caller.position().distanceTo(CityPlan.ARRIVAL) < 1.5, "to the Observatory: " + caller.position());
            AsgardProgress progress = ModAttachments.ASGARD.get(caller);
            check(progress.has(AsgardProgress.VISITED) && progress.cameFrom() != null
                    && progress.cameFrom().pos().closerThan(stone, 4), "he has visited, and is remembered where he came from");
            check(ModAttachments.ASGARD.get(friend).has(AsgardProgress.VISITED), "so is his friend");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = RULES)
    public static void theThroneSeatsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos throne = helper.absolutePos(new BlockPos(10, 2, 10));
        helper.getLevel().setBlock(throne, ModBlocks.ODIN_THRONE.get().defaultBlockState(), 3);
        ServerPlayer player = f.player(10.5, FLOOR, 12.5);
        com.kolodkingal.enhanced.block.asgard.OdinThroneBlock.sit(player, throne);
        check(player.getVehicle() instanceof SeatEntity seat && throne.equals(seat.seatOf()), "he sits on the throne");
        player.stopRiding();
        check(!player.isPassenger() && !helper.getLevel().getBlockState(BlockPos.containing(player.position())).is(ModBlocks.ODIN_THRONE.get()),
                "and gets up beside it, not inside it");
        helper.succeed();
    }

    // ---- the Hall of Feasts (3.7.2: "fix Asgard's dining hall") ----

    /** Asgard with every chunk of the Hall of Feasts generated and loaded. */
    private static ServerLevel hall(MinecraftServer server) {
        ServerLevel asgard = AsgardTestSupport.ensureAsgard(server);
        BoundingBox b = FeastHall.box();
        for (int cx = (b.minX() >> 4) - 1; cx <= (b.maxX() >> 4) + 1; cx++) {
            for (int cz = (b.minZ() >> 4) - 1; cz <= (b.maxZ() >> 4) + 1; cz++) {
                asgard.getChunk(cx, cz);
            }
        }
        return asgard;
    }

    private static double top(ServerLevel level, BlockPos pos) {
        var shape = level.getBlockState(pos).getCollisionShape(level, pos);
        return shape.isEmpty() ? 0.0 : shape.max(Direction.Axis.Y);
    }

    /**
     * Every seat of the hall is a bench facing its table, with the table a block high right before it and room to sit;
     * the high table has its six; nothing floats (the dishes stand on the boards, the lanterns hang from the ceiling),
     * no fence arms reach into the air, the doors (three at the front, two to the kitchen at the back) are open.
     */
    @EnhancedTest(template = ARENA, batch = REALM, timeoutTicks = 400)
    public static void theHallOfFeastsHasItsTablesSeatsAndLightsInPlace(GameTestHelper helper) {
        ServerLevel asgard = hall(helper.getLevel().getServer());
        List<CityPlan.Bench> seats = FeastHall.seats();
        check(seats.size() == 6 + 112
                && new HashSet<>(seats.stream().map(CityPlan.Bench::pos).toList()).size() == seats.size(),
                "118 seats, each its own: " + seats.size());
        for (CityPlan.Bench seat : seats) {
            BlockState bench = asgard.getBlockState(seat.pos());
            check(bench.is(ModBlocks.ASGARD_BENCH.get())
                    && bench.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                    == seat.facing(), "a bench facing its table at " + seat + ": " + bench);
            BlockPos front = seat.pos().relative(seat.facing());
            check(FeastHall.isTable(asgard.getBlockState(front)) && top(asgard, front) == 1.0
                    && FeastHall.diningSeat(asgard, seat.pos()),
                    "a table a block high right before " + seat + ": " + asgard.getBlockState(front));
            check(top(asgard, seat.pos().below()) == 1.0 && top(asgard, seat.pos()) == 0.5,
                    "the bench stands on the floor: " + seat);
            check(asgard.getBlockState(seat.pos().above()).getCollisionShape(asgard, seat.pos().above()).isEmpty()
                    && asgard.getBlockState(seat.pos().above(2)).getCollisionShape(asgard,
                            seat.pos().above(2)).isEmpty(),
                    "room to sit at " + seat);
        }
        BoundingBox box = FeastHall.box();
        int lanterns = 0;
        int dishes = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY() + 1, box.minZ(), box.maxX(), box.minY() + 13,
                box.maxZ())) {
            BlockState state = asgard.getBlockState(pos);
            check(!(state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock),
                    "no fence posts with arms into the air: " + pos);
            if (state.is(Blocks.CAKE) || state.is(Blocks.CANDLE) || state.is(Blocks.FLOWER_POT)) {
                dishes++;
                check(FeastHall.isTable(asgard.getBlockState(pos.below())) && top(asgard, pos.below()) == 1.0
                        && state.canSurvive(asgard, pos),
                        "the dish stands on a table's board, not over it: " + pos + " " + state);
            }
            if (state.is(Blocks.LANTERN)) {
                lanterns++;
                check(state.getValue(net.minecraft.world.level.block.LanternBlock.HANGING) && state.canSurvive(asgard,
                        pos)
                        && net.minecraft.world.level.block.Block.canSupportCenter(asgard, pos.above(), Direction.DOWN),
                        "the lantern hangs from the ceiling: " + pos);
            }
        }
        check(lanterns == 12 && dishes >= 30, "12 lanterns, the tables laid: " + lanterns + " " + dishes);
        // the doors: the front's three arches (west, the template's south) and the back's two to the kitchen (east)
        CityPlan.Piece p = FeastHall.piece();
        for (int[] door : new int[][]{{10, 30}, {20, 30}, {30, 30}, {10, 0}, {30, 0}}) {
            for (int y = 1; y <= 2; y++) {
                BlockPos at = p.toWorld(new BlockPos(door[0], y, door[1]));
                check(asgard.getBlockState(at).isAir(), "the door is open at " + at);
            }
        }
        helper.succeed();
    }

    /**
     * An Asgardian finds his way from the hall's front door to every one of its seats, and from the kitchen to the high
     * table, and never over a table or a dish on one.
     */
    @EnhancedTest(template = ARENA, batch = REALM, timeoutTicks = 400)
    public static void everySeatOfTheHallIsReachedAndNeverOverATable(GameTestHelper helper) {
        ServerLevel asgard = hall(helper.getLevel().getServer());
        AsgardianEntity walker = com.kolodkingal.enhanced.registry.ModEntities.ASGARDIAN.get().create(asgard);
        walker.setRole(Role.SIF, 0);
        List<String> lost = new java.util.ArrayList<>();
        for (BlockPos from : List.of(FeastHall.DOOR, FeastHall.piece().toWorld(new BlockPos(10, 1, -3)))) {
            List<CityPlan.Bench> to = from.equals(FeastHall.DOOR) ? FeastHall.seats() : FeastHall.highTable();
            for (CityPlan.Bench seat : to) {
                // as a walker does: a path as far as his search reaches, then on from where it ended (at most three
                // legs)
                BlockPos at = from;
                boolean reached = false;
                for (int leg = 0; leg < 3 && !reached; leg++) {
                    walker.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
                    walker.setOnGround(true);
                    net.minecraft.world.level.pathfinder.Path path = walker.getNavigation().createPath(seat.pos(), 1);
                    if (path == null || path.getNodeCount() == 0) {
                        break;
                    }
                    for (int i = 0; i < path.getNodeCount(); i++) {
                        net.minecraft.world.level.pathfinder.Node n = path.getNode(i);
                        check(!FeastHall.overTable(asgard, n.x, n.y, n.z), "the way to " + seat
                                + " goes over a table at " + n.asBlockPos());
                    }
                    reached = path.canReach();
                    at = path.getEndNode().asBlockPos();
                }
                if (!reached) {
                    lost.add(from.toShortString() + "->" + seat.pos().toShortString());
                }
            }
        }
        check(lost.isEmpty(), lost.size() + " seats out of reach, the first: " + (lost.isEmpty() ? "" : lost.get(0)));
        walker.discard();
        helper.succeed();
    }

    /**
     * A city built before 3.7.2 has the hall of 3.5: the first time all of it is loaded it is mended once, block by
     * block, to the new hall, leaving alone a block a player changed (and the banners come with their colours).
     */
    @EnhancedTest(template = ARENA, batch = "asgard_feast_mend", timeoutTicks = 400)
    public static void aHallOfFeastsBuiltBeforeIsMendedOnce(GameTestHelper helper) {
        ServerLevel asgard = hall(helper.getLevel().getServer());
        var templates = asgard.getStructureManager();
        FeastHall.Diff diff = FeastHall.diff(templates.get(FeastHall.LEGACY).orElseThrow(),
                templates.get(FeastHall.piece().id()).orElseThrow());
        int n = diff.positions().size();
        check(n > 300, "the two halls differ in many blocks: " + n);
        int flags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
        for (BlockPos pos : diff.positions()) {
            asgard.setBlock(pos, diff.was(pos), flags);
        }
        // the hall of 3.5: a cake over Volstagg's table floats half a block over the board
        CityPlan.Bench volstagg = FeastHall.seatOf(Role.VOLSTAGG);
        BlockPos changed = volstagg.pos().relative(volstagg.facing());
        check(asgard.getBlockState(changed).is(ModBlocks.POLISHED_REALM_STONE.get()), "the old high table: "
                + asgard.getBlockState(changed));
        asgard.setBlock(changed, Blocks.GOLD_BLOCK.defaultBlockState(), flags);
        AsgardState state = AsgardState.get(helper.getLevel().getServer());
        state.setFeastHallMended(false);
        FeastHall.Mend done = FeastHall.mendOnce(asgard);
        check(done != null && done.repaired() == n - 1 && done.skipped() == 1 && done.alreadyRight() == 0, "mended: "
                + done);
        check(state.feastHallMended() && FeastHall.mendOnce(asgard) == null, "once");
        for (BlockPos pos : diff.positions()) {
            if (!pos.equals(changed)) {
                check(FeastHall.same(asgard.getBlockState(pos), diff.now(pos)), "the new hall at " + pos + ": "
                        + asgard.getBlockState(pos));
            }
        }
        check(asgard.getBlockState(changed).is(Blocks.GOLD_BLOCK), "the player's block is left alone");
        int banners = 0;
        for (BlockPos pos : diff.positions()) {
            if (asgard.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BannerBlockEntity banner) {
                banners++;
                check(banner.getPatterns().layers().size() == 3, "a banner of Asgard with its pattern: " + pos);
            }
        }
        check(banners >= 4, "the banners moved onto the pillars: " + banners);
        FeastHall.Mend again = FeastHall.mend(asgard);
        check(again.repaired() == 0 && again.alreadyRight() == n - 1 && again.skipped() == 1,
                "a second mending changes nothing: " + again);
        asgard.setBlock(changed, diff.now(changed), flags);
        helper.succeed();
    }
}

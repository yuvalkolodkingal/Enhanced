package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.block.CerebroConsoleBlock;
import com.kolodkingal.enhanced.finders.CerebroRoom;
import com.kolodkingal.enhanced.finders.SiteSearch;
import com.kolodkingal.enhanced.network.CerebroPayload;
import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.world.SchoolLabPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The lab's Cerebro (3.7, lane L5; PLAN 7 and 13): Professor X at the console finds a player in the Nether (amplified,
 * the mind marked with its dimension) and, away from it, his plain N does not (Cerebro unchanged: every {@code
 * XavierTests} green); Magneto's helmet blocks it both ways; a non-telepath is refused; the Phoenix may use it; the
 * stone sites are listed (one of each kind, a stand-in Sanctum in the flat test world); the cooldown is shared; the
 * report reaches whoever stands in the sphere.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CerebroRoomTests {
    static final String BATCH = "cerebro_room";

    /** A console at the arena's (x, 2, z) facing north (as the lab's), its absolute position. */
    static BlockPos console(GameTestHelper helper, int x, int z) {
        BlockPos rel = new BlockPos(x, 2, z);
        helper.setBlock(rel, ModBlocks.CEREBRO_CONSOLE.get().defaultBlockState()
                .setValue(CerebroConsoleBlock.FACING, Direction.NORTH));
        return helper.absolutePos(rel);
    }

    static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    static void ends(TestKit.Fixture f, ServerPlayer player) {
        f.onClose(() -> CerebroRoom.forget(player));
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void atTheConsoleProfessorXFindsTheNether(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        BlockPos console = console(helper, 4, 4);
        ServerPlayer near = f.player(4.5, FLOOR, 6.5, Power.PROFESSOR_X);
        ServerPlayer away = f.player(16.5, FLOOR, 16.5, Power.PROFESSOR_X);
        ServerPlayer thor = f.player(10.5, FLOOR, 10.5, Power.THOR);
        ServerPlayer hidden = f.player(12.5, FLOOR, 10.5);
        ServerLevel nether = server.getLevel(Level.NETHER);
        for (ServerPlayer traveller : List.of(thor, hidden)) {
            server.getPlayerList().getOps().add(new ServerOpListEntry(traveller.getGameProfile(), 2, false));
            traveller.teleportTo(nether, 0.5, 80.0, 0.5, 0, 0);
            server.getPlayerList().getOps().remove(traveller.getGameProfile());
        }
        hidden.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        ends(f, near);
        TestKit.sentPackets(near);
        TestKit.sentPackets(away);
        press(away, 5);
        List<CerebroPayload> plain = LocatorTests.payloads(TestKit.sentPackets(away), CerebroPayload.class);
        check(!plain.isEmpty() && !plain.get(plain.size() - 1).amplified(), "away from the console: the plain Cerebro");
        check(plain.get(plain.size() - 1).minds().stream().noneMatch(m -> m.name().equals(name(thor))),
                "which does not reach the Nether");
        check(!CerebroRoom.amplifying(away), "not amplified");
        press(near, 5);
        check(CerebroRoom.amplifying(near) && CerebroRoom.console(near).map(g -> g.pos().equals(console)).orElse(false),
                "N within 4 of the console: amplified");
        check(Powers.get(near).is(Live.CEREBRO), "the helmet on his head");
        check(!Powers.get(near).isReady(5, helper.getLevel().getGameTime()), "Cerebro's cooldown (shared)");
        List<CerebroPayload> sent = LocatorTests.payloads(TestKit.sentPackets(near), CerebroPayload.class);
        check(!sent.isEmpty(), "his client hears it");
        CerebroPayload report = sent.get(sent.size() - 1);
        check(report.amplified() && report.ticksLeft() == 600, "amplified, 30 seconds: " + report.ticksLeft());
        check(report.minds().stream().anyMatch(m -> m.name().equals(name(thor))
                        && m.dimension().equals("minecraft:the_nether") && m.power() == Power.THOR.ordinal()),
                "Thor, in the Nether, as Thor");
        check(report.minds().stream().noneMatch(m -> m.name().equals(name(hidden))), "Magneto's helmet hides a mind");
        check(report.minds().stream().filter(m -> m.name().equals(name(away))).allMatch(m -> m.dimension().isEmpty()),
                "a mind of his own dimension has no dimension written");
        check(LocatorTests.actionBarKeys(TestKit.sentPackets(thor))
                .contains("message.enhanced.xavier.cerebro.found_you"), "Thor feels it");
        CerebroRoom.forget(near);
        check(!CerebroRoom.amplifying(near) && !Powers.get(near).is(Live.CEREBRO), "the helmet comes off");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void whoMayUseTheConsole(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos console = console(helper, 10, 10);
        ServerPlayer thor = f.player(10.5, FLOOR, 12.5, Power.THOR);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer jean = f.player(8.5, FLOOR, 10.5, Power.PHOENIX);
        ends(f, jean);
        ends(f, wanda);
        TestKit.sentPackets(thor);
        check(CerebroRoom.useConsole(thor, console) == InteractionResult.CONSUME
                && LocatorTests.actionBarKeys(TestKit.sentPackets(thor)).contains(
                "message.enhanced.school_lab.not_telepath") && !CerebroRoom.amplifying(thor),
                "Thor: \"Cerebro needs a telepath's mind.\"");
        wanda.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        TestKit.sentPackets(wanda);
        check(CerebroRoom.useConsole(wanda, console) == InteractionResult.CONSUME
                && LocatorTests.actionBarKeys(TestKit.sentPackets(wanda))
                .contains("message.enhanced.school_lab.helmet"),
                "Wanda in Magneto's helmet: \"The helmet blocks Cerebro.\"");
        TestKit.sentPackets(jean);
        check(CerebroRoom.useConsole(jean, console) == InteractionResult.SUCCESS && CerebroRoom.amplifying(jean),
                "the Phoenix may (Jean did)");
        check(LocatorTests.payloads(TestKit.sentPackets(jean), CerebroPayload.class).stream()
                .anyMatch(CerebroPayload::amplified), "and her client hears the amplified Cerebro");
        wanda.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        TestKit.sentPackets(wanda);
        check(CerebroRoom.useConsole(wanda, console) == InteractionResult.CONSUME && !CerebroRoom.amplifying(wanda)
                && LocatorTests.actionBarKeys(TestKit.sentPackets(wanda)).contains("message.enhanced.cerebro.busy"),
                "one helmet: Wanda waits while Jean wears it");
        check(CerebroRoom.useConsole(jean, console) == InteractionResult.SUCCESS && !CerebroRoom.amplifying(jean),
                "a second right-click takes the helmet off");
        TestKit.sentPackets(jean);
        check(CerebroRoom.useConsole(jean, console) == InteractionResult.CONSUME
                && LocatorTests.actionBarKeys(TestKit.sentPackets(jean)).contains("message.enhanced.cerebro.resting"),
                "and it rests (Cerebro's cooldown)");
        check(CerebroRoom.useConsole(jean, console.above()) == InteractionResult.PASS, "no console there: nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theSitesAreListedAndTheRoomSeesThem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos console = console(helper, 16, 4);
        ServerPlayer charles = f.player(16.5, FLOOR, 6.5, Power.PROFESSOR_X);
        // A visitor inside the sphere (its centre two blocks north of the console), and a stand-in Sanctum.
        ServerPlayer peter = f.player(16.5, FLOOR, 1.5);
        ends(f, charles);
        ChunkPos at = charles.chunkPosition();
        LocatorTests.FakeGrid sanctum = new LocatorTests.FakeGrid(level, 8);
        ChunkPos site = sanctum.potential(Math.floorDiv(at.x, 8) + 1, Math.floorDiv(at.z, 8));
        sanctum.starts.add(site.toLong());
        SiteSearch.prober((l, tag) -> tag == ModTags.STONE_SITE_TIME ? List.of(sanctum) : List.of());
        f.onClose(() -> SiteSearch.prober(null));
        check(CerebroRoom.sphereCenter(level.getBlockState(console), console).equals(console.north(2))
                && SchoolLabPlan.CEREBRO_CONSOLE.relative(Direction.NORTH, CerebroRoom.CONSOLE_TO_CENTER)
                .equals(SchoolLabPlan.CEREBRO_CENTER), "the sphere's centre, as the lab's plan has it");
        TestKit.sentPackets(charles);
        TestKit.sentPackets(peter);
        check(CerebroRoom.useConsole(charles, console) == InteractionResult.SUCCESS, "Professor X at the console");
        f.everyTick(150, t -> {
            List<CerebroPayload> sent = LocatorTests.payloads(TestKit.sentPackets(charles), CerebroPayload.class);
            for (CerebroPayload report : sent) {
                if (report.amplified() && !report.sites().isEmpty()) {
                    check(report.sites().size() <= CerebroPayload.MAX_SITES
                            && report.sites().stream().allMatch(s -> s.kind() == StoneKind.TIME.ordinal()
                            && s.pos().equals(sanctum.locate(site))), "the Sanctum's diamond: " + report.sites());
                    List<CerebroPayload> seen = LocatorTests.payloads(TestKit.sentPackets(peter),
                            CerebroPayload.class);
                    check(seen.stream().anyMatch(p -> p.amplified() && p.ticksLeft() <= CerebroRoom.VISITOR_TICKS),
                            "the visitor in the sphere sees the room's stars");
                    return true;
                }
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theConsoleTellsThePlayersNear(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos console = console(helper, 4, 16);
        ServerPlayer billy = f.player(4.5, FLOOR, 18.5, Power.BILLY);
        TestKit.sentPackets(billy);
        check(CerebroRoom.useConsole(billy, console) == InteractionResult.SUCCESS, "Billy is a telepath too");
        check(LocatorTests.payloads(TestKit.sentPackets(billy), LabFxPayload.class).stream()
                .anyMatch(p -> p.kind() == LabFxPayload.CONSOLE_ON && p.pos().equals(console)),
                "the console lights up");
        helper.destroyBlock(new BlockPos(4, 2, 16));
        f.everyTick(5, t -> {
            if (CerebroRoom.amplifying(billy)) {
                return false;
            }
            check(LocatorTests.payloads(TestKit.sentPackets(billy), LabFxPayload.class).stream()
                    .anyMatch(p -> p.kind() == LabFxPayload.CONSOLE_OFF), "the console gone: Cerebro goes dark");
            return true;
        });
    }

    static String name(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    private CerebroRoomTests() {
    }
}

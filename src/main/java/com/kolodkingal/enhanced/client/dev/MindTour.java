package com.kolodkingal.enhanced.client.dev;

import com.kolodkingal.enhanced.mind.MindBuilder;
import com.kolodkingal.enhanced.mind.MindPaintings;
import com.kolodkingal.enhanced.mind.MindProgress;
import com.kolodkingal.enhanced.mind.MindQuest;
import com.kolodkingal.enhanced.mind.MindRealm;
import com.kolodkingal.enhanced.mind.MindTalk;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A development tool (3.12, {@code -Denhanced.mindtour=true}): makes a flat creative world, walks through Vision's mind
 * floor by floor, plays its talks, then wakes Nano Ultron at a Horton manor and makes Ultron nanotech, saving a
 * screenshot at each step ({@code run/screenshots/m*.png}), and quits.
 */
@Subscriber(side = Side.CLIENT)
public final class MindTour {
    private static final boolean ENABLED = Boolean.getBoolean("enhanced.mindtour");

    private record Step(int delay, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static boolean asked;
    private static boolean scripted;
    private static int settle;
    private static int wait;
    private static int index;

    private MindTour() {
    }

    @Subscribe
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!asked && mc.screen instanceof TitleScreen) {
                asked = true;
                mc.options.renderDistance().set(6);
                mc.options.chatOpacity().set(0.0);
                LevelSettings settings = new LevelSettings("mindtour", GameType.CREATIVE, false, Difficulty.EASY, true, new GameRules(),
                        WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("mindtour", settings, new WorldOptions(4242L, true, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value()
                                .createWorldDimensions(), mc.screen);
            }
            return;
        }
        if (settle++ < 100) {
            return;
        }
        if (!scripted) {
            scripted = true;
            // (-Denhanced.mindtour.clean=true: no HUD, for pictures to share)
            mc.options.hideGui = Boolean.getBoolean("enhanced.mindtour.clean");
            script();
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (index >= STEPS.size()) {
            mc.stop();
            return;
        }
        Step step = STEPS.get(index++);
        step.action().run();
        wait = step.delay();
    }

    private static void server(Consumer<ServerPlayer> action, int delay) {
        STEPS.add(new Step(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            var server = mc.getSingleplayerServer();
            if (server != null && mc.player != null) {
                java.util.UUID id = mc.player.getUUID();
                server.execute(() -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(id);
                    if (p != null) {
                        try {
                            action.accept(p);
                        } catch (RuntimeException e) {
                            com.kolodkingal.enhanced.Enhanced.LOGGER.error("Mind tour step failed", e);
                        }
                    }
                });
            }
        }));
    }

    private static void shot(String name, int delay) {
        STEPS.add(new Step(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {
            });
        }));
    }

    private static void camera(CameraType type) {
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.setCameraType(type)));
    }

    private static BlockPos o(ServerPlayer p) {
        return MindRealm.origin(MindProgress.of(p).plot());
    }

    /** Puts the player at a place relative to his mind's origin, looking a way. */
    private static void at(int x, int y, int z, float yaw, float pitch, int delay) {
        server(p -> {
            BlockPos at = o(p).offset(x, y, z);
            p.teleportTo(p.serverLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, pitch);
        }, delay);
    }

    private static void choose(String option, int delay) {
        server(p -> MindTalk.choose(p, option), delay);
    }

    private static void script() {
        camera(CameraType.FIRST_PERSON);
        server(p -> {
            p.setGameMode(GameType.CREATIVE);
            Powers.awaken(p, Power.VISION, "white_vision", false);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }, 40);
        server(MindRealm::enter, 120);
        shot("m01_arrival", 10);
        at(0, 0, -30, 0.0F, -10.0F, 60);
        shot("m02_house", 10);
        at(0, 0, -6, 180.0F, 10.0F, 40);
        shot("m03_hall_dume", 10);
        server(p -> MindTalk.start(p, "dum_e", MindQuest_find(p, "dum_e"), false), 30);
        shot("m04_dume_talk", 10);
        choose("1", 30);
        shot("m05_dume_floors", 10);
        choose("bye", 20);
        at(7, 0, -4, 135.0F, 10.0F, 40);
        shot("m06_kitchen", 10);
        at(-4, 0, 7, 90.0F, 0.0F, 40);
        server(p -> MindPaintings.place(p.serverLevel(), MindBuilder.screen(o(p)), Direction.EAST, MindPaintings.CINEMA), 30);
        shot("m07_cinema", 10);
        at(-1, 0, 5, 270.0F, 0.0F, 30);
        shot("m08_friday", 10);
        server(MindTalk::elevator, 30);
        shot("m09_elevator", 10);
        choose("upper", 40);
        at(2, MindBuilder.UPPER, 6, 135.0F, 10.0F, 40);
        shot("m10_ultron_room", 10);
        at(-6, MindBuilder.UPPER, -6, 0.0F, 0.0F, 30);
        shot("m11_balcony", 10);
        server(MindTalk::elevator, 20);
        choose("top", 40);
        at(-6, MindBuilder.TOP, 8, 180.0F, 10.0F, 40);
        shot("m12_robots", 10);
        server(p -> MindTalk.start(p, "robot", MindQuest_find(p, "robot"), false), 30);
        choose("0", 15);
        choose("1", 15);
        shot("m13_riddle", 10);
        choose("1", 15);
        choose("1", 15);
        shot("m14_riddle_pass", 10);
        choose("0", 30);
        at(-6, MindBuilder.TOP, -3, 0.0F, 10.0F, 30);
        shot("m15_library", 10);
        server(MindTalk::elevator, 20);
        choose("down", 40);
        shot("m16_ultron_talk", 10);
        choose("0", 20);
        choose("0", 20);
        shot("m17_family", 10);
        choose("0", 40);
        shot("m18_corridor", 10);
        at(-11, MindBuilder.BOTTOM, 5, 90.0F, 5.0F, 60);
        shot("m19_torch", 20);
        choose("0", 25);
        shot("m20_flames", 10);
        choose("0", 40);
        shot("m21_first", 10);
        choose("0", 20);
        choose("0", 30);
        shot("m22_horton", 10);
        for (int i = 0; i < 20; i++) {
            choose("0", 8);
        }
        shot("m23_healed", 10);
        choose("0", 60);
        shot("m24_back_up", 10);
        server(p -> {
            p.getInventory().clearContent();
            p.getInventory().add(new net.minecraft.world.item.ItemStack(com.kolodkingal.enhanced.registry.ModItems.SOURCE_CODE.get()));
            p.getInventory().selected = 0;
        }, 10);
        server(p -> com.kolodkingal.enhanced.registry.ModItems.SOURCE_CODE.get().use(p.level(), p, net.minecraft.world.InteractionHand.MAIN_HAND), 40);
        camera(CameraType.THIRD_PERSON_FRONT);
        shot("m25_source_code", 20);
        camera(CameraType.FIRST_PERSON);
        server(MindRealm::leave, 80);
        if ("manor".equals(System.getProperty("enhanced.mindtour.from"))) {
            // (-Denhanced.mindtour.from=manor: only the manor and Nano Ultron)
            STEPS.subList(2, STEPS.size()).clear();
        }
        // the Horton manor, built here by its own piece (its spot kept, so every view is placed from it)
        server(p -> {
            ServerLevel level = p.serverLevel();
            BlockPos base = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    p.blockPosition().offset(30, 0, 0));
            manor = base;
            for (int cx = (base.getX() - 20) >> 4; cx <= (base.getX() + 20) >> 4; cx++) {
                for (int cz = (base.getZ() - 20) >> 4; cz <= (base.getZ() + 20) >> 4; cz++) {
                    level.getChunk(cx, cz);
                }
            }
            com.kolodkingal.enhanced.nano.HortonManorPiece piece = new com.kolodkingal.enhanced.nano.HortonManorPiece(base);
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random,
                    piece.getBoundingBox(), new net.minecraft.world.level.ChunkPos(base), base);
            level.setDayTime(6000);
            p.teleportTo(level, base.getX() + 0.5, base.getY() + 1, base.getZ() - 34.5, 0.0F, -12.0F);
        }, 80);
        shot("m26_manor", 40);
        server(p -> p.teleportTo(p.serverLevel(), manor.getX() + 5.5, manor.getY(), manor.getZ() + 0.5, 270.0F, 25.0F), 40);
        shot("m27_storage", 40);
        server(p -> {
            p.setGameMode(GameType.SURVIVAL);
            p.setInvulnerable(true);
            BlockPos storage = manor.offset(8, 0, 0);
            var state = p.serverLevel().getBlockState(storage);
            state.useWithoutItem(p.serverLevel(), p, new net.minecraft.world.phys.BlockHitResult(storage.getCenter(),
                    Direction.UP, storage, false));
        }, 60);
        shot("m28_nano_ultron", 10);
        server(p -> {
            for (var boss : p.serverLevel().getEntitiesOfClass(com.kolodkingal.enhanced.nano.NanoUltronEntity.class,
                    p.getBoundingBox().inflate(40))) {
                boss.setHealth(boss.getMaxHealth() * 0.49F);
            }
        }, 60);
        shot("m29_question", 10);
        choose("1", 60);
        shot("m30_peace", 10);
        server(p -> {
            Powers.awaken(p, Power.ULTRON, "", false);
            BlockPos storage = manor.offset(8, 0, 0);
            p.serverLevel().getBlockState(storage).useWithoutItem(p.serverLevel(), p,
                    new net.minecraft.world.phys.BlockHitResult(storage.getCenter(), Direction.UP, storage, false));
        }, 60);
        camera(CameraType.THIRD_PERSON_FRONT);
        shot("m31_ultron_nano", 20);
        server(p -> com.kolodkingal.enhanced.nano.NanoUltron.army(p, Powers.get(p)), 60);
        camera(CameraType.THIRD_PERSON_BACK);
        shot("m32_nano_army", 20);
    }

    /** Where the tour built the Horton manor. */
    private static BlockPos manor = BlockPos.ZERO;

    private static net.minecraft.world.entity.Entity MindQuest_find(ServerPlayer p, String role) {
        for (var npc : p.serverLevel().getEntitiesOfClass(com.kolodkingal.enhanced.mind.MindNpcEntity.class,
                p.getBoundingBox().inflate(80))) {
            if (npc.role().id().equals(role)) {
                return npc;
            }
        }
        return null;
    }
}

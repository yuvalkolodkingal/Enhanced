package com.kolodkingal.enhanced.client.dev;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.21.11 only, a development tool ({@code -Denhanced.porttour=true}): a smoke test of the 1.21.11 copy's drawing. It
 * makes a flat creative world, wears every hero (front view), summons every creature and thing of the mod, places every
 * block with a block entity, opens the mod's screens, visits Asgard and Eternity, saving a screenshot at each step
 * ({@code screenshots/p*.png}), and quits. A step that fails is logged and the tour goes on.
 */
@Subscriber(side = Side.CLIENT)
public final class PortTour {
    private static final boolean ENABLED = Boolean.getBoolean("enhanced.porttour");

    private record Step(int delay, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static boolean asked;
    private static boolean scripted;
    private static int settle;
    private static int wait;
    private static int index;

    private PortTour() {
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
                LevelSettings settings = new LevelSettings("porttour", GameType.CREATIVE, false, Difficulty.EASY, true,
                        new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("porttour", settings, new WorldOptions(4242L, false, false),
                        access -> access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value()
                                .createWorldDimensions(), mc.screen);
            }
            return;
        }
        if (settle++ < 100) {
            return;
        }
        if (!scripted) {
            scripted = true;
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
        try {
            step.action().run();
        } catch (RuntimeException e) {
            Enhanced.LOGGER.error("Port tour step {} failed", index, e);
        }
        wait = step.delay();
    }

    private static void client(Runnable action, int delay) {
        STEPS.add(new Step(delay, action));
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
                            Enhanced.LOGGER.error("Port tour server step failed", e);
                        }
                    }
                });
            }
        }));
    }

    private static void shot(String name, int delay) {
        client(() -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, msg -> {
            });
        }, delay);
    }

    private static void camera(CameraType type) {
        client(() -> Minecraft.getInstance().options.setCameraType(type), 2);
    }

    /** {@code -Denhanced.porttour.only=heroes,mobs,blocks,screens,realms}: only those parts (all when unset). */
    private static boolean part(String name) {
        String only = System.getProperty("enhanced.porttour.only");
        return only == null || java.util.Arrays.asList(only.split(",")).contains(name);
    }

    private static void script() {
        server(p -> {
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(p.level(), 0.5, -50.0, 0.5, java.util.Set.of(), 180.0F, 10.0F, true);
        }, 40);
        // every hero, seen from the front
        camera(CameraType.THIRD_PERSON_FRONT);
        for (Power power : Power.values()) {
            if (!part("heroes") || power == Power.NONE || power == Power.CUSTOM) {
                continue;
            }
            server(p -> Powers.awaken(p, power, "", false), 40);
            shot("p_hero_" + power.name().toLowerCase(java.util.Locale.ROOT), 5);
        }
        server(p -> Powers.awaken(p, Power.NONE, "", false), 20);
        camera(CameraType.FIRST_PERSON);
        // every creature and thing, six at a time, in front of the player
        List<EntityType<?>> types = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(Enhanced.MODID)).toList();
        for (int i = 0; part("mobs") && i < types.size(); i += 6) {
            List<EntityType<?>> batch = types.subList(i, Math.min(types.size(), i + 6));
            int n = i / 6;
            server(p -> {
                ServerLevel level = p.level();
                for (Entity old : level.getEntities((Entity) null, p.getBoundingBox().inflate(30), e -> e != p
                        && !(e instanceof ServerPlayer))) {
                    old.discard();
                }
                int k = 0;
                for (EntityType<?> type : batch) {
                    try {
                        Entity e = type.create(level, EntitySpawnReason.COMMAND);
                        if (e == null) {
                            continue;
                        }
                        e.snapTo(p.getX() - 5.0 + 2.0 * k, p.getY(), p.getZ() - 6.0, 0.0F, 0.0F);
                        if (e instanceof Mob mob) {
                            mob.setNoAi(true);
                        }
                        e.setNoGravity(true);
                        level.addFreshEntity(e);
                    } catch (RuntimeException ex) {
                        Enhanced.LOGGER.error("Port tour: {} could not be summoned", BuiltInRegistries.ENTITY_TYPE.getKey(type), ex);
                    }
                    k++;
                }
                Enhanced.LOGGER.info("Port tour: batch {} {}", n, batch.stream().map(BuiltInRegistries.ENTITY_TYPE::getKey).toList());
            }, 40);
            shot("p_mobs_" + n, 5);
        }
        server(p -> {
            for (Entity old : p.level().getEntities((Entity) null, p.getBoundingBox().inflate(30), e -> !(e instanceof ServerPlayer))) {
                old.discard();
            }
        }, 10);
        // every block with a block entity, in a row
        if (part("blocks")) server(p -> {
            ServerLevel level = p.level();
            int k = 0;
            for (Block block : BuiltInRegistries.BLOCK) {
                if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Enhanced.MODID) || !(block instanceof EntityBlock)) {
                    continue;
                }
                BlockPos at = BlockPos.containing(p.getX() - 8 + 2 * (k % 9), p.getY() - 1, p.getZ() - 5 - 3 * (k / 9));
                BlockState state = block.defaultBlockState();
                level.setBlock(at, state, 3);
                Enhanced.LOGGER.info("Port tour: placed {}", BuiltInRegistries.BLOCK.getKey(block));
                k++;
            }
        }, 60);
        if (part("blocks")) shot("p_blocks", 5);
        // the screens
        if (part("screens")) screens();
        // the realms
        if (part("realms")) realms();
    }

    private static void screens() {
        client(() -> Minecraft.getInstance().setScreen(new com.kolodkingal.enhanced.client.screen.ChooseHeroScreen(null)), 40);
        shot("p_screen_choose", 5);
        client(() -> Minecraft.getInstance().setScreen(new com.kolodkingal.enhanced.client.screen.HeroEditorScreen(null, CustomHero.EMPTY)), 60);
        shot("p_screen_editor", 5);
        client(() -> Minecraft.getInstance().setScreen(new com.kolodkingal.enhanced.config.screen.ConfigScreen(null)), 40);
        shot("p_screen_config", 5);
        client(() -> Minecraft.getInstance().setScreen(new com.kolodkingal.enhanced.client.screen.StoneWheelScreen(
                Arrays.asList(StoneKind.values()), StoneKind.values()[0])), 40);
        shot("p_screen_stones", 5);
        client(() -> Minecraft.getInstance().setScreen(new com.kolodkingal.enhanced.client.build.BlockPaletteScreen()), 40);
        shot("p_screen_palette", 5);
        client(() -> Minecraft.getInstance().setScreen(null), 10);
    }

    private static void realms() {
        server(p -> {
            ServerLevel asgard = AsgardDimension.level(p.level().getServer());
            if (asgard != null) {
                p.teleportTo(asgard, 0.5, 150.0, 0.5, java.util.Set.of(), 0.0F, -20.0F, true);
            }
        }, 200);
        shot("p_asgard_sky", 5);
        client(() -> Minecraft.getInstance().player.setXRot(30.0F), 40);
        shot("p_asgard_land", 5);
        server(p -> {
            ServerLevel eternity = p.level().getServer().getLevel(EternityRealm.LEVEL);
            if (eternity != null) {
                p.teleportTo(eternity, 0.5, 70.0, 0.5, java.util.Set.of(), 0.0F, -10.0F, true);
            }
        }, 200);
        shot("p_eternity", 5);
    }
}

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
            // (flying, so a view from the air stays where it is put)
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(p.serverLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, pitch);
        }, delay);
    }

    private static void choose(String option, int delay) {
        server(p -> MindTalk.choose(p, option), delay);
    }

    private static final java.util.List<net.minecraft.world.entity.Entity> TOUR = new java.util.ArrayList<>();

    /** (3.19.1) A row of creatures and one of the universe's people in front of the player there; a picture; his talk. */
    private static void whatIf(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key, String name,
                               com.kolodkingal.enhanced.asgard.npc.Role role) {
        server(p -> {
            TOUR.forEach(net.minecraft.world.entity.Entity::discard);
            TOUR.clear();
            net.minecraft.server.level.ServerLevel level = p.getServer().getLevel(key);
            int x = key == com.kolodkingal.enhanced.multiverse.Multiverse.HYDRA_KEY ? 99 : 8;
            int z = key == com.kolodkingal.enhanced.multiverse.Multiverse.HYDRA_KEY ? 3 : 8;
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            level.setDayTime(6000);
            p.teleportTo(level, x + 0.5, y + 2.5, z - 7.5, 0.0F, 15.0F);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }, 60);
        server(p -> {
            net.minecraft.server.level.ServerLevel level = p.serverLevel();
            java.util.List<net.minecraft.world.entity.EntityType<?>> kinds = java.util.List.of(net.minecraft.world.entity.EntityType.COW,
                    net.minecraft.world.entity.EntityType.PIG, net.minecraft.world.entity.EntityType.SHEEP, net.minecraft.world.entity.EntityType.CHICKEN,
                    net.minecraft.world.entity.EntityType.WOLF, net.minecraft.world.entity.EntityType.CREEPER, net.minecraft.world.entity.EntityType.VILLAGER);
            double x0 = p.getX() - 6.0;
            int floor = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.getX()), (int) Math.floor(p.getZ()) + 6);
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = 2; dz <= 10; dz++) {
                    BlockPos at = BlockPos.containing(p.getX() + dx, floor - 1, p.getZ() + dz);
                    level.setBlockAndUpdate(at, net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int up = 0; up < 6; up++) {
                        level.setBlockAndUpdate(at.above(1 + up), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            }
            for (int i = 0; i < kinds.size(); i++) {
                net.minecraft.world.entity.Entity e = kinds.get(i).create(level);
                if (e instanceof net.minecraft.world.entity.Mob mob) {
                    double x = x0 + i * 2.0;
                    mob.moveTo(x, floor, p.getZ() + 6.0, 180.0F, 0.0F);
                    mob.setYHeadRot(180.0F);
                    mob.setNoAi(true);
                    level.addFreshEntity(mob);
                    TOUR.add(mob);
                }
            }
            com.kolodkingal.enhanced.asgard.npc.AsgardianEntity person = com.kolodkingal.enhanced.multiverse.WhatIfPeople.person(level, role,
                    p.getX() + 7.0, floor, p.getZ() + 3.5, null);
            person.setYRot(180.0F);
            level.addFreshEntity(person);
            TOUR.add(person);
            com.kolodkingal.enhanced.Enhanced.LOGGER.info("What If tour: {} creatures in {}", TOUR.size(), level.dimension().location());
        }, 80);
        shot(name, 10);
        server(p -> {
            for (net.minecraft.world.entity.Entity e : TOUR) {
                if (e instanceof com.kolodkingal.enhanced.asgard.npc.AsgardianEntity npc) {
                    p.teleportTo(p.serverLevel(), npc.getX(), npc.getY(), npc.getZ() - 2.5, 0.0F, 10.0F);
                    boolean opened = com.kolodkingal.enhanced.asgard.npc.AsgardTalk.begin(p, npc);
                    com.kolodkingal.enhanced.Enhanced.LOGGER.info("What If tour: talk opened {} at {}", opened,
                            com.kolodkingal.enhanced.asgard.npc.AsgardTalk.currentNode(p));
                }
            }
        }, 60);
        shot(name.replace("1_", "2_").replace("3_", "4_").replace("5_", "6_") + "_talk", 10);
        STEPS.add(new Step(10, () -> Minecraft.getInstance().setScreen(null)));
    }

    private static final BlockPos[] BUNKER = new BlockPos[1];

    /** (3.19.1) A bunker of a universe, as the land built it: the transit, the bunker from the air, its hall, a resident's talk. */
    private static void bunker(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key, int universe, String name) {
        server(p -> {
            TOUR.forEach(net.minecraft.world.entity.Entity::discard);
            TOUR.clear();
            BUNKER[0] = null;
            net.minecraft.server.level.ServerLevel level = p.getServer().getLevel(key);
            for (int cell = 0; cell < 6 && BUNKER[0] == null; cell++) {
                int cx = cell % 3 - 1;
                int cz = cell / 3;
                BlockPos site = com.kolodkingal.enhanced.multiverse.Bunkers.site(level.getSeed(), universe, cx, cz);
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        level.getChunk((site.getX() >> 4) + x, (site.getZ() >> 4) + z);
                    }
                }
                BlockPos lodestone = com.kolodkingal.enhanced.multiverse.Bunkers.find(level, site.offset(0, 0, -8));
                com.kolodkingal.enhanced.Enhanced.LOGGER.info("Bunker tour: cell {},{} site {} lodestone {}", cx, cz, site, lodestone);
                if (lodestone != null) {
                    BUNKER[0] = lodestone;
                    if (!level.getBlockState(lodestone.below()).is(net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS)) {
                        TOUR.addAll(com.kolodkingal.enhanced.multiverse.Bunkers.populate(level, lodestone, universe,
                                com.kolodkingal.enhanced.multiverse.Bunkers.siteSeed(level.getSeed(), universe, cx, cz)));
                    }
                }
            }
            BlockPos at = BUNKER[0] == null ? p.blockPosition() : BUNKER[0];
            level.setDayTime(5000);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(level, at.getX() + 0.5, at.getY() + 26.0, at.getZ() + 8 + 48.0, 180.0F, 28.0F);
        }, 60);
        shot(name, 200);
        STEPS.add(new Step(2, () -> com.kolodkingal.enhanced.client.world.UniverseTransit.play(universe)));
        shot(name + "_transit", 4);
        shot(name + "_transit2", 12);
        shot(name + "_transit3", 14);
        server(p -> {
            if (BUNKER[0] != null) {
                BlockPos l = BUNKER[0];
                p.teleportTo(p.serverLevel(), l.getX() + 0.5, l.getY() + 31.0, l.getZ() + 8 + 4.0, 180.0F, 70.0F);
            }
        }, 40);
        shot(name + "_top", 40);
        server(p -> {
            if (BUNKER[0] != null) {
                BlockPos l = BUNKER[0];
                p.teleportTo(p.serverLevel(), l.getX() + 0.5, l.getY() + 1.0, l.getZ() + 4.5, 180.0F, 5.0F);
            }
        }, 40);
        shot(name + "_hall", 40);
        server(p -> {
            for (net.minecraft.world.entity.Entity e : p.serverLevel().getEntitiesOfClass(com.kolodkingal.enhanced.asgard.npc.AsgardianEntity.class,
                    p.getBoundingBox().inflate(24.0))) {
                if (e instanceof com.kolodkingal.enhanced.asgard.npc.AsgardianEntity npc) {
                    p.teleportTo(p.serverLevel(), npc.getX(), npc.getY(), npc.getZ() + 2.5, 180.0F, 10.0F);
                    boolean opened = com.kolodkingal.enhanced.asgard.npc.AsgardTalk.begin(p, npc);
                    com.kolodkingal.enhanced.Enhanced.LOGGER.info("Bunker tour: talk opened {} at {}", opened,
                            com.kolodkingal.enhanced.asgard.npc.AsgardTalk.currentNode(p));
                    break;
                }
            }
        }, 40);
        shot(name + "_talk", 10);
        STEPS.add(new Step(10, () -> Minecraft.getInstance().setScreen(null)));
    }

    private static void script() {
        if ("config".equals(System.getProperty("enhanced.mindtour.mode"))) {
            // (3.16.1) the settings screen and its client section, in a world (a player's report: an empty screen)
            STEPS.add(new Step(40, () -> Minecraft.getInstance().setScreen(
                    new com.kolodkingal.enhanced.config.screen.ConfigScreen(null))));
            shot("c01_settings", 10);
            STEPS.add(new Step(40, () -> {
                var file = com.kolodkingal.enhanced.config.core.Configs.of(
                        com.kolodkingal.enhanced.config.core.ConfigFile.Type.CLIENT).get(0);
                Minecraft.getInstance().setScreen(new com.kolodkingal.enhanced.config.screen.ConfigSectionScreen(
                        new com.kolodkingal.enhanced.config.screen.ConfigScreen(null), file, java.util.List.of(),
                        net.minecraft.network.chat.Component.literal(file.fileName())));
            }));
            shot("c02_client", 10);
            // a section of values, filled again (as after a reset), which empties and refills its rows
            STEPS.add(new Step(40, () -> {
                var file = com.kolodkingal.enhanced.config.core.Configs.of(
                        com.kolodkingal.enhanced.config.core.ConfigFile.Type.CLIENT).get(0);
                var screen = new com.kolodkingal.enhanced.config.screen.ConfigSectionScreen(
                        new com.kolodkingal.enhanced.config.screen.ConfigScreen(null), file, java.util.List.of("animation"),
                        net.minecraft.network.chat.Component.literal("animation"));
                Minecraft.getInstance().setScreen(screen);
                try {
                    java.lang.reflect.Method rebuild = com.kolodkingal.enhanced.config.screen.ConfigSectionScreen.class
                            .getDeclaredMethod("rebuild");
                    rebuild.setAccessible(true);
                    rebuild.invoke(screen);
                    rebuild.invoke(screen);
                    com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: the section was filled again");
                } catch (ReflectiveOperationException e) {
                    com.kolodkingal.enhanced.Enhanced.LOGGER.error("Mind tour: refilling the section failed", e);
                }
            }));
            shot("c03_values", 10);
            STEPS.add(new Step(20, () -> Minecraft.getInstance().setScreen(null)));
            return;
        }
        if ("bunker".equals(System.getProperty("enhanced.mindtour.mode"))) {
            bunker(com.kolodkingal.enhanced.multiverse.Multiverse.GAMMA_KEY, com.kolodkingal.enhanced.multiverse.Multiverse.GAMMA, "b01_gamma");
            bunker(com.kolodkingal.enhanced.multiverse.Multiverse.ZOMBIES_KEY, com.kolodkingal.enhanced.multiverse.Multiverse.ZOMBIES, "b02_zombies");
            return;
        }
        if ("whatif".equals(System.getProperty("enhanced.mindtour.mode"))) {
            // (3.19.1) -Denhanced.mindtour.mode=whatif: the zombies' and the Gamma Bomb's creatures, and their people's talk
            whatIf(com.kolodkingal.enhanced.multiverse.Multiverse.ZOMBIES_KEY, "w01_zombies", com.kolodkingal.enhanced.asgard.npc.Role.SURVIVOR);
            whatIf(com.kolodkingal.enhanced.multiverse.Multiverse.GAMMA_KEY, "w03_gamma", com.kolodkingal.enhanced.asgard.npc.Role.SURVIVOR);
            whatIf(com.kolodkingal.enhanced.multiverse.Multiverse.HYDRA_KEY, "w05_hydra", com.kolodkingal.enhanced.asgard.npc.Role.HYDRA_CITIZEN);
            return;
        }
        if ("jean".equals(System.getProperty("enhanced.mindtour.mode"))) {
            // (3.19.2) -Denhanced.mindtour.mode=jean: Jean Grey's Brand New Day look, her telekinesis (orange, no lightning),
            // her psionic force field and her bigger Phoenix
            server(p -> {
                p.setGameMode(GameType.SURVIVAL);
                Powers.awaken(p, Power.PHOENIX, "brand_new_day", false);
                p.teleportTo(p.getX(), p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        (int) p.getX(), (int) p.getZ()), p.getZ());
                p.setYRot(0.0F);
                p.setXRot(0.0F);
            }, 40);
            camera(CameraType.THIRD_PERSON_FRONT);
            shot("j01_look_front", 30);
            camera(CameraType.THIRD_PERSON_BACK);
            shot("j02_look_back", 20);
            server(p -> {
                net.minecraft.world.entity.Mob pig = net.minecraft.world.entity.EntityType.PIG.create(p.level());
                if (pig != null) {
                    pig.moveTo(p.getX() + 3.0, p.getY(), p.getZ() + 3.0, 180.0F, 0.0F);
                    pig.setNoAi(true);
                    p.level().addFreshEntity(pig);
                }
            }, 10);
            server(p -> p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                    p.position().add(3.0, 0.5, 3.0)), 10);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, true, false), 10);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, false, false), 30);
            shot("j03_tk_back", 10);
            camera(CameraType.THIRD_PERSON_FRONT);
            shot("j04_tk_front", 15);
            camera(CameraType.FIRST_PERSON);
            shot("j05_tk_first", 15);
            server(p -> com.kolodkingal.enhanced.ability.Telekinesis.release(p, true), 10);
            server(p -> {
                com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 2, true, true);
                com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 2, false, true);
            }, 10);
            camera(CameraType.THIRD_PERSON_FRONT);
            shot("j06_shield_front", 20);
            camera(CameraType.FIRST_PERSON);
            shot("j07_shield_first", 15);
            server(p -> {
                com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 2, true, true);
                com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 2, false, true);
                p.getFoodData().setFoodLevel(20);
                p.setYRot(180.0F);
            }, 10);
            camera(CameraType.THIRD_PERSON_BACK);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 5, true, false), 10);
            shot("j08_charge", 45);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 5, false, false), 2);
            shot("j09_burst", 8);
            shot("j10_burst_late", 12);
            camera(CameraType.FIRST_PERSON);
            return;
        }
        if ("tk".equals(System.getProperty("enhanced.mindtour.mode"))) {
            // (3.16.1) -Denhanced.mindtour.mode=tk: the Scarlet Witch's telekinesis on a pig, then (Sneak) on a block
            server(p -> {
                p.setGameMode(GameType.SURVIVAL);
                Powers.awaken(p, Power.SCARLET_WITCH, "", false);
                // (her crown: the suit's skin draws it, so the item's must not show over it)
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                        new net.minecraft.world.item.ItemStack(com.kolodkingal.enhanced.registry.ModItems.WANDA_CROWN.get()));
                p.teleportTo(p.getX(), p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        (int) p.getX(), (int) p.getZ()), p.getZ());
                p.setYRot(0.0F);
                p.setXRot(10.0F);
            }, 40);
            server(p -> {
                net.minecraft.world.entity.Mob pig = net.minecraft.world.entity.EntityType.PIG.create(p.level());
                if (pig != null) {
                    pig.moveTo(p.getX(), p.getY(), p.getZ() + 4.0, 180.0F, 0.0F);
                    pig.setNoAi(true);
                    p.level().addFreshEntity(pig);
                }
            }, 30);
            server(p -> p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                    p.position().add(0, 0.5, 4.0)), 10);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, true, false), 30);
            server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: tk holds {} ({})",
                    Powers.get(p).heldEntityId(), p.level().getEntity(Powers.get(p).heldEntityId())), 5);
            shot("tk01_pig", 10);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, false, false), 10);
            // a second press throws it
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, true, false), 5);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, false, false), 30);
            server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: tk after the throw holds {}",
                    Powers.get(p).heldEntityId()), 10);
            // a block, with Sneak
            server(p -> {
                p.level().setBlockAndUpdate(net.minecraft.core.BlockPos.containing(p.getX(), p.getY(), p.getZ() + 3.0),
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                        net.minecraft.world.phys.Vec3.atCenterOf(net.minecraft.core.BlockPos.containing(p.getX(), p.getY(), p.getZ() + 3.0)));
            }, 20);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, true, true), 30);
            server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: tk block holds {} ({})",
                    Powers.get(p).heldEntityId(), p.level().getEntity(Powers.get(p).heldEntityId())), 5);
            shot("tk02_block", 10);
            // her glowing eyes: in the open air, firing her chaos beam, seen from the front
            server(p -> com.kolodkingal.enhanced.ability.Telekinesis.release(p, true), 10);
            server(p -> {
                p.setGameMode(GameType.CREATIVE);
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                p.teleportTo(p.getX(), p.getY() + 12.0, p.getZ() - 6.0);
                p.setYRot(180.0F);
                p.setXRot(0.0F);
            }, 30);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, true, false), 15);
            camera(CameraType.THIRD_PERSON_FRONT);
            shot("tk03_face", 10);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, false, false), 10);
            camera(CameraType.FIRST_PERSON);
            server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, false, true), 40);
            return;
        }
        if ("eternity".equals(System.getProperty("enhanced.mindtour.mode"))) {
            // (3.16) -Denhanced.mindtour.mode=eternity: Eternity's realm, and Eternity before you
            server(p -> p.setGameMode(GameType.CREATIVE), 40);
            server(p -> com.kolodkingal.enhanced.eternity.EternityRealm.enter(p, p.blockPosition()), 200);
            shot("e01_arrival", 20);
            server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: Eternity entities near: {}",
                    p.serverLevel().getEntitiesOfClass(com.kolodkingal.enhanced.eternity.EternityEntity.class,
                            p.getBoundingBox().inflate(256)).stream().map(e -> e.blockPosition().toShortString()).toList()), 20);
            camera(CameraType.THIRD_PERSON_FRONT);
            shot("e02_behind", 20);
            camera(CameraType.FIRST_PERSON);
            server(p -> p.setXRot(-25.0F), 20);
            shot("e03_up", 20);
            // from far off: walking up to him across the water
            STEPS.add(new Step(10, () -> Minecraft.getInstance().setScreen(null)));
            server(p -> p.teleportTo(0.5, 66.0, 40.5), 40);
            server(p -> { p.setYRot(180.0F); p.setXRot(-10.0F); }, 40);
            shot("e04_far", 20);
            server(p -> p.teleportTo(0.5, 66.0, 110.5), 40);
            server(p -> { p.setYRot(180.0F); p.setXRot(-10.0F); }, 40);
            shot("e05_farther", 20);
            return;
        }
        if ("palette".equals(System.getProperty("enhanced.mindtour.mode"))) {
            // (3.16) -Denhanced.mindtour.mode=palette: the Scarlet Witch's block palette held open a minute, for clicks from
            // outside (xdotool) to be tried on it (26.3's mouse buttons)
            server(p -> Powers.awaken(p, Power.SCARLET_WITCH, "", false), 40);
            STEPS.add(new Step(1200, () -> Minecraft.getInstance().setScreen(
                    new com.kolodkingal.enhanced.client.build.BlockPaletteScreen())));
            return;
        }
        camera(CameraType.FIRST_PERSON);
        server(p -> {
            p.setGameMode(GameType.CREATIVE);
            Powers.awaken(p, Power.VISION, "white_vision", false);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }, 40);
        server(MindRealm::enter, 120);
        shot("m01_arrival", 10);
        at(4, 20, -48, 0.0F, 8.0F, 60);
        shot("m02_house", 10);
        at(0, 0, -6, 180.0F, 10.0F, 40);
        shot("m03_hall_dume", 10);
        server(p -> MindTalk.start(p, "dum_e", MindQuest_find(p, "dum_e"), false), 30);
        shot("m04_dume_talk", 10);
        choose("1", 30);
        shot("m05_dume_floors", 10);
        choose("bye", 20);
        // (3.15) U at the door with Dum-E (JARVIS is gone since 3.16: his grave is in the mausoleum)
        at(1, 0, -5, 160.0F, 5.0F, 40);
        shot("m05d_dume_u", 10);
        at(7, 0, -4, 135.0F, 10.0F, 40);
        shot("m06_kitchen", 10);
        at(-4, 0, 7, 90.0F, 0.0F, 40);
        server(p -> {
            var picture = MindBuilder.showPicture(p.serverLevel(), o(p));
            com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: the cinema's picture at {}", picture == null ? null : picture.getBoundingBox());
        }, 30);
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
        // (3.16) Source Code Vision's powers in fire (the default), in the hall where there is room
        at(1, 0, -4, 90.0F, 2.0F, 30);
        camera(CameraType.THIRD_PERSON_BACK);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, true, false), 15);
        shot("m25b_fire_beam_back", 5);
        camera(CameraType.THIRD_PERSON_FRONT);
        shot("m25c_fire_beam_front", 5);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, false, false), 10);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 4, true, false), 25);
        shot("m25d_fire_density", 5);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 4, true, false), 10);
        // ...and with the Solar Jewel turned on (vision.solar_jewel.enabled)
        server(p -> com.kolodkingal.enhanced.config.SolarJewelConfig.ENABLED.set(true), 10);
        camera(CameraType.THIRD_PERSON_BACK);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, true, false), 15);
        shot("m25e_solar_beam", 5);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, false, false), 10);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 5, true, false), 15);
        shot("m25f_eye_beams", 5);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 5, false, false), 10);
        camera(CameraType.THIRD_PERSON_FRONT);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, true, true), 18);
        shot("m25g_overload_gather", 12);
        camera(CameraType.THIRD_PERSON_BACK);
        shot("m25h_overload_fire", 6);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, false, true), 40);
        server(p -> com.kolodkingal.enhanced.config.SolarJewelConfig.ENABLED.set(false), 10);
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
                    p.blockPosition().offset(60, 0, 0));
            manor = base;
            for (int cx = (base.getX() - 40) >> 4; cx <= (base.getX() + 40) >> 4; cx++) {
                for (int cz = (base.getZ() - 50) >> 4; cz <= (base.getZ() + 20) >> 4; cz++) {
                    level.getChunk(cx, cz);
                }
            }
            com.kolodkingal.enhanced.nano.HortonManorPiece piece = new com.kolodkingal.enhanced.nano.HortonManorPiece(base);
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random,
                    piece.getBoundingBox(), new net.minecraft.world.level.ChunkPos(base), base);
            level.setDayTime(6000);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(level, base.getX() + 0.5, base.getY() + 20, base.getZ() - 66.5, 0.0F, 9.0F);
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

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

    private static void client(Runnable action, int delay) {
        STEPS.add(new Step(delay, action));
    }

    /** A picture of the player from the front or the back, close (a narrow view), with or without the HUD. */
    private static void portrait(String name, CameraType type, boolean hud) {
        client(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = !hud;
            mc.options.setCameraType(type);
        }, 6);
        shot(name, 6);
    }

    private static void become(Power power, String variant, boolean flying) {
        server(p -> {
            p.setGameMode(GameType.CREATIVE);
            Powers.awaken(p, power, variant, false);
            if (!variant.isEmpty()) {
                Powers.setVariant(p, com.kolodkingal.enhanced.power.Variants.get(power, variant)); // (an earned look, given)
            }
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundClearTitlesPacket(true));
            p.getAbilities().flying = flying;
            p.onUpdateAbilities();
            if (!flying) {
                p.teleportTo(p.getX(), p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        (int) p.getX(), (int) p.getZ()), p.getZ());
            } else {
                p.teleportTo(p.getX(), p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        (int) p.getX(), (int) p.getZ()) + 6.0, p.getZ());
            }
            p.setYRot(0.0F);
            p.setXRot(0.0F);
        }, 50);
    }

    /**
     * (3.17) -Denhanced.mindtour.mode=ultimate: Ultimate Mode seen in the game. First with the mode off (an ordinary Scarlet
     * Witch, Billy and Source Code Vision: nothing of 3.17 on them, and the new keys' defaults checked against every other
     * key), then on: the Nexus Being front and back, her Chaos Burst and her red sky; the Demiurge at rest and powered, his
     * magic types, his Grant Magic screen, the Spell Wheel, the Mindscape; Source Code Vision in his fire.
     */
    private static void ultimateTour() {
        client(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.fov().set(40);
            // the new keys against every other key's default
            for (net.minecraft.client.KeyMapping mine : new net.minecraft.client.KeyMapping[]{
                    com.kolodkingal.enhanced.client.KeyBindings.SPELLS, com.kolodkingal.enhanced.client.KeyBindings.ULTIMATE,
                    com.kolodkingal.enhanced.client.KeyBindings.MAGIC_TYPES}) {
                List<String> same = new ArrayList<>();
                for (net.minecraft.client.KeyMapping other : mc.options.keyMappings) {
                    if (other != mine && other.getDefaultKey().equals(mine.getDefaultKey())) {
                        same.add(other.getName());
                    }
                }
                com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: key {} ({}) shares its default with {}", mine.getName(),
                        mine.getDefaultKey().getName(), same);
            }
            // (and, for the record, the letters the game itself has taken)
            for (net.minecraft.client.KeyMapping other : mc.options.keyMappings) {
                String key = other.getDefaultKey().getName();
                if (!other.getName().startsWith("key.enhanced") && (key.endsWith(".o") || key.endsWith(".k"))) {
                    com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: {} is the default of {}", key, other.getName());
                }
            }
        }, 10);
        server(p -> {
            p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "time set noon");
            p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "weather clear");
            com.kolodkingal.enhanced.ultimate.Ultimate.set(Boolean.FALSE);
        }, 20);
        // ---- Ultimate Mode off: the heroes as they were ----
        become(Power.SCARLET_WITCH, "", false);
        server(p -> p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(com.kolodkingal.enhanced.registry.ModItems.WANDA_CROWN.get())), 30);
        portrait("u01_off_wanda_front", CameraType.THIRD_PERSON_FRONT, true);
        portrait("u02_off_wanda_back", CameraType.THIRD_PERSON_BACK, true);
        server(p -> p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY), 10);
        become(Power.BILLY, "", true);
        portrait("u03_off_billy_flying_front", CameraType.THIRD_PERSON_FRONT, true);
        portrait("u04_off_billy_flying_back", CameraType.THIRD_PERSON_BACK, true);
        become(Power.VISION, "source_code", true);
        portrait("u05_off_source_code_flying_front", CameraType.THIRD_PERSON_FRONT, true);
        portrait("u06_off_source_code_flying_back", CameraType.THIRD_PERSON_BACK, true);
        // ---- Ultimate Mode on ----
        server(p -> {
            com.kolodkingal.enhanced.ultimate.Ultimate.set(Boolean.TRUE);
            com.kolodkingal.enhanced.platform.net.Net.sendToAllPlayers(com.kolodkingal.enhanced.ultimate.UltimateSyncPayload.now());
        }, 20);
        // Source Code Vision: his whole body in fire while he flies
        portrait("u10_vision_fire_front", CameraType.THIRD_PERSON_FRONT, false);
        portrait("u11_vision_fire_back", CameraType.THIRD_PERSON_BACK, false);
        client(() -> Minecraft.getInstance().player.setDeltaMovement(0.0, 0.0, 1.4), 2);
        client(() -> Minecraft.getInstance().player.setDeltaMovement(0.0, 0.0, 1.4), 2);
        shot("u12_vision_fire_moving", 6);
        // the Nexus Being
        become(Power.SCARLET_WITCH, "", false);
        server(p -> p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(com.kolodkingal.enhanced.registry.ModItems.WANDA_CROWN.get())), 10);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, false), 70);
        portrait("u20_nexus_front", CameraType.THIRD_PERSON_FRONT, false);
        portrait("u21_nexus_back", CameraType.THIRD_PERSON_BACK, false);
        portrait("u22_nexus_hud", CameraType.THIRD_PERSON_BACK, true);
        // her Chaos Burst over the land, seen from the air behind her, then her red sky from the ground
        server(p -> {
            for (int i = 0; i < 10; i++) {
                net.minecraft.world.entity.Mob pig = net.minecraft.world.entity.EntityType.ZOMBIE.create(p.level());
                if (pig != null) {
                    double a = i * 0.628;
                    double x = p.getX() + Math.cos(a) * (8 + i * 3);
                    double z = p.getZ() + Math.sin(a) * (8 + i * 3);
                    pig.moveTo(x, p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z), z, 0.0F, 0.0F);
                    pig.setNoAi(true);
                    p.level().addFreshEntity(pig);
                }
            }
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(p.getX(), p.getY() + 14.0, p.getZ());
            p.setXRot(25.0F);
        }, 30);
        client(() -> Minecraft.getInstance().options.fov().set(90), 4);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, false), 7);
        portrait("u23_chaos_burst", CameraType.THIRD_PERSON_BACK, false);
        server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: after the burst {} of the ten zombies near her live",
                p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(60),
                        e -> e.getType() == net.minecraft.world.entity.EntityType.ZOMBIE && e.isAlive()).size()), 30);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, false), 5);
        portrait("u24_chaos_burst_first_person", CameraType.FIRST_PERSON, false);
        server(p -> {
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.teleportTo(p.getX(), p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    (int) p.getX(), (int) p.getZ()), p.getZ());
            p.setXRot(-40.0F);
        }, 60);
        portrait("u25_red_sky", CameraType.FIRST_PERSON, false);
        client(() -> Minecraft.getInstance().options.fov().set(40), 4);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, true), 40);
        server(p -> p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY), 10);
        // the Demiurge: at rest, then powered (flying)
        become(Power.BILLY, "", false);
        portrait("u30_demiurge_idle_front", CameraType.THIRD_PERSON_FRONT, false);
        portrait("u31_demiurge_idle_back", CameraType.THIRD_PERSON_BACK, false);
        portrait("u32_demiurge_idle_hud", CameraType.THIRD_PERSON_BACK, true);
        become(Power.BILLY, "", true);
        client(() -> Minecraft.getInstance().options.fov().set(60), 30);
        portrait("u33_demiurge_powered_front", CameraType.THIRD_PERSON_FRONT, false);
        portrait("u34_demiurge_powered_back", CameraType.THIRD_PERSON_BACK, false);
        client(() -> Minecraft.getInstance().options.fov().set(30), 4);
        portrait("u35_demiurge_powered_close", CameraType.THIRD_PERSON_FRONT, false);
        client(() -> Minecraft.getInstance().options.fov().set(60), 4);
        client(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }, 6);
        client(() -> Minecraft.getInstance().player.setDeltaMovement(0.0, 0.0, 1.2), 3);
        client(() -> Minecraft.getInstance().player.setDeltaMovement(0.0, 0.0, 1.2), 3);
        shot("u36_demiurge_powered_flying", 6);
        // his magic types: the Scarlet Witch's keys are his, and her beam
        server(com.kolodkingal.enhanced.ultimate.Demiurge::cycle, 30);
        portrait("u37_magic_type_hud", CameraType.THIRD_PERSON_BACK, true);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, true, false), 15);
        portrait("u38_magic_type_wandas_beam", CameraType.THIRD_PERSON_BACK, true);
        server(p -> com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 1, false, false), 10);
        server(p -> com.kolodkingal.enhanced.ultimate.Demiurge.setType(p, 0), 20);
        // his screens
        client(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            mc.options.fov().set(70);
        }, 4);
        server(com.kolodkingal.enhanced.magic.Grants::openScreen, 30);
        shot("u40_grant_magic", 10);
        client(() -> Minecraft.getInstance().setScreen(null), 10);
        // the Mindscape
        server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: the Mindscape opens to the Demiurge: {}",
                com.kolodkingal.enhanced.magic.Realms.toggleMindscape(p)), 120);
        shot("u50_mindscape", 10);
        server(p -> { p.setYRot(120.0F); p.setXRot(10.0F); }, 20);
        shot("u51_mindscape_round", 10);
        portrait("u52_mindscape_demiurge", CameraType.THIRD_PERSON_FRONT, false);
        client(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
        }, 4);
        client(() -> com.kolodkingal.enhanced.client.magic.SpellWheelScreen.open(Minecraft.getInstance()), 30);
        shot("u41_spell_wheel", 10);
        client(() -> Minecraft.getInstance().setScreen(null), 10);
        server(p -> com.kolodkingal.enhanced.magic.Realms.toggleMindscape(p), 80);
        // ---- the three forms with rules of their own ----
        // Cosmic Spider-Man
        become(Power.SPIDER_MAN, "", false);
        client(() -> Minecraft.getInstance().options.fov().set(40), 4);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, false), 70);
        portrait("u60_cosmic_spider_front", CameraType.THIRD_PERSON_FRONT, false);
        portrait("u61_cosmic_spider_back", CameraType.THIRD_PERSON_BACK, false);
        portrait("u62_cosmic_spider_hud", CameraType.THIRD_PERSON_BACK, true);
        server(p -> {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(p.getX(), p.getY() + 8.0, p.getZ());
        }, 20);
        client(() -> Minecraft.getInstance().options.fov().set(70), 4);
        client(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }, 6);
        client(() -> Minecraft.getInstance().player.setDeltaMovement(0.0, 0.0, 1.8), 2);
        client(() -> Minecraft.getInstance().player.setDeltaMovement(0.0, 0.0, 1.8), 2);
        shot("u63_cosmic_spider_flying", 6);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, false), 5);
        shot("u64_cosmic_spider_blast", 6);
        server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: Cosmic Spider-Man: armour {}, attack {}, Uni-Power {}, flies {}",
                p.getArmorValue(), p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                com.kolodkingal.enhanced.ultimate.BespokeForms.cosmicEnergy(p), com.kolodkingal.enhanced.ultimate.BespokeForms.flies(p)), 10);
        // the Godkiller
        become(Power.IRON_MAN, "", false);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, false), 80);
        server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: the Godkiller: {} blocks tall, {} wide, armour {}, scale {}",
                p.getBbHeight(), p.getBbWidth(), p.getArmorValue(),
                p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE)), 10);
        portrait("u70_godkiller_front", CameraType.THIRD_PERSON_FRONT, false);
        portrait("u71_godkiller_back", CameraType.THIRD_PERSON_BACK, false);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, false), 6);
        shot("u72_godkiller_cannons", 6);
        // Ultimate Tommy
        become(Power.TOMMY, "", false);
        client(() -> Minecraft.getInstance().options.fov().set(60), 4);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, false), 70);
        portrait("u80_ultimate_tommy_front", CameraType.THIRD_PERSON_FRONT, false);
        server(p -> com.kolodkingal.enhanced.ultimate.UltimateForms.key(p, false), 16);
        portrait("u81_ultimate_tommy_tornado", CameraType.THIRD_PERSON_BACK, false);
        server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: Ultimate Tommy: armour {}, time-proof {}",
                p.getArmorValue(), com.kolodkingal.enhanced.ultimate.BespokeForms.timeProof(p)), 10);
        server(p -> com.kolodkingal.enhanced.ultimate.Ultimate.set(null), 20);
    }

    /**
     * (3.17) -Denhanced.mindtour.mode=keys: the keyboard itself. The tour waits twice while something outside presses real
     * keys (xdotool): with Ultimate Mode off, an ordinary Scarlet Witch whose new keys must do nothing and whose R must
     * fire her beam as ever; then, with it on, the Demiurge, whose Magic Types key and Spell Wheel (Sneak + the Spells key,
     * held) must answer.
     */
    private static void keysTour() {
        server(p -> {
            p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "time set noon");
            com.kolodkingal.enhanced.ultimate.Ultimate.set(Boolean.FALSE);
        }, 20);
        become(Power.SCARLET_WITCH, "", false);
        server(p -> p.setGameMode(GameType.SURVIVAL), 10);
        client(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: KEYS A");
        }, 110);
        server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: keys A: beam {}, ultimate {}, awakening {}, chanting {}, caster {}, screen {}",
                Powers.get(p).beamActive(), Powers.get(p).is(com.kolodkingal.enhanced.power.Live.ULTIMATE),
                Powers.get(p).is(com.kolodkingal.enhanced.power.Live.ULTIMATE_AWAKENING),
                Powers.get(p).is(com.kolodkingal.enhanced.power.Live.CHANTING), com.kolodkingal.enhanced.magic.Magic.isCaster(p),
                Minecraft.getInstance().screen), 2);
        shot("k01_off_wanda_real_keys", 60);
        server(p -> {
            com.kolodkingal.enhanced.ultimate.Ultimate.set(Boolean.TRUE);
            com.kolodkingal.enhanced.platform.net.Net.sendToAllPlayers(com.kolodkingal.enhanced.ultimate.UltimateSyncPayload.now());
        }, 20);
        become(Power.BILLY, "", true);
        client(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: KEYS B");
        }, 120);
        server(p -> com.kolodkingal.enhanced.Enhanced.LOGGER.info("Mind tour: keys B: magic type {}, screen {}",
                com.kolodkingal.enhanced.ultimate.Demiurge.borrowed(p), Minecraft.getInstance().screen), 2);
        shot("k02_spell_wheel_real_keys", 80);
        server(p -> com.kolodkingal.enhanced.ultimate.Ultimate.set(null), 20);
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
        if ("ultimate".equals(System.getProperty("enhanced.mindtour.mode"))) {
            ultimateTour();
            return;
        }
        if ("keys".equals(System.getProperty("enhanced.mindtour.mode"))) {
            keysTour();
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
        // ...and with the Solar Jewel turned on (Ultimate Mode, 3.17)
        server(p -> com.kolodkingal.enhanced.ultimate.Ultimate.set(true), 10);
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
        server(p -> com.kolodkingal.enhanced.ultimate.Ultimate.set(null), 10);
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

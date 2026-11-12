package com.kolodkingal.enhanced.client.dev;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * A development tool (3.14.1, {@code -Denhanced.flickerprobe=true}, off by default): measures the flight's flicker to
 * the standing pose. While the local player flies in third person it counts the frames whose body was not posed (the
 * poses bailed out, or the lean of the body was not applied) and logs why: the clause of the vanilla-owned body, the
 * gap between the model's age and the animation's clock, a partial tick of 1, a frame that did not move the clock on.
 * It also counts the frames drawn while the flight's weight is under a half (an upright body in the air), the flashes
 * (one frame whose drawn pose is unlike the frames around it) and, from 1.21.9, the armour drawn in vanilla's pose.
 * <p>
 * With it a client makes a world and flies scripted heroes in F5 (take-off, hovering, turning by key and as a mouse
 * turns, cruising, sprinting, stopping, climbing and diving, landing, over, under and out of water, out of a gap too
 * low to stand in), first as it is, then with the player's age set {@code -Denhanced.flickerprobe.age} ticks on
 * (default 2,000,000: days of play) and uneven frame times, then in survival; it logs a summary per hero and phase
 * ({@code Flicker probe:} lines) and quits. {@code -Denhanced.flickerprobe.rounds=1,3} picks rounds;
 * {@code -Denhanced.flickerprobe.drive=false} only measures (fly yourself).
 */
@Subscriber(side = Side.CLIENT)
public final class FlickerProbe {
    public static final boolean ENABLED = Boolean.getBoolean("enhanced.flickerprobe");
    private static final boolean DRIVE = ENABLED && !"false".equals(System.getProperty("enhanced.flickerprobe.drive"));
    private static final int AGE = Integer.getInteger("enhanced.flickerprobe.age", 2_000_000);
    private static final Power[] HEROES = {Power.IRON_MAN, Power.THOR, Power.VISION, Power.MAGNETO, Power.PHOENIX,
            Power.DOCTOR_STRANGE};
    /** The fliers of the survival round: those who fly with nothing in hand or on. */
    private static final Power[] SURVIVAL_HEROES = {Power.VISION, Power.MAGNETO, Power.SCARLET_WITCH, Power.BILLY, Power.PHOENIX,
            Power.ULTRON, Power.APOCALYPSE, Power.AGATHA, Power.DOCTOR_STRANGE};

    // ---- the frame being drawn (the local player's) ----
    private static int frameTick = Integer.MIN_VALUE;
    private static float framePt = Float.NaN;
    private static boolean counted;
    private static String anim = "-";
    /** What the animator did for the frame being tallied (anim moves on with the next frame's animator call). */
    private static String frameAnim = "-";
    private static boolean leaned;
    private static float wAir;
    private static int poses;
    private static String poseFail;
    private static String lastPose = "-";
    /** The pose drawn this frame: the body's lean and bank (degrees), then the model's body, arms and legs (radians). */
    private static final float[] DRAWN = new float[7];
    private static String diag = "";
    /** The last three frames in flight: what was drawn and why (for a flash: one frame unlike the frames around it). */
    private static final float[][] RING = new float[3][];
    private static final String[] RING_WHY = new String[3];
    private static final boolean[] RING_HERO = new boolean[3];
    private static int flashes;

    // ---- the tally of the phase ----
    private static String phase = "start";
    private static int frames;
    private static int unposed;
    private static int poseCalls;
    private static int armourDraws;
    private static int armourBare;
    private static int armourVanilla;
    private static int totalArmour;
    private static int totalArmourVanilla;
    private static int totalArmourBare;
    private static int weakAir;
    private static final Map<String, Integer> REASONS = new TreeMap<>();
    private static final List<String> DETAILS = new ArrayList<>();
    private static int totalFrames;
    private static int totalUnposed;
    private static int totalFlashes;
    private static boolean jitter;
    /** Turning as a mouse turns, every frame (degrees a tick; 0: none). */
    private static float mouseTurn;
    private static long lastTurnNanos;
    private static final java.util.Random RANDOM = new java.util.Random(7L);

    private FlickerProbe() {
    }

    private static boolean local(Player player) {
        return player == Minecraft.getInstance().player;
    }

    /** What {@code HeroAnimator.frame} did for this player's frame (the reason it returned where it did). */
    public static void anim(Player player, String what) {
        if (local(player)) {
            anim = what;
        }
    }

    /** The lean of the body: called once a frame per drawn player, after the animator's frame. */
    public static void lean(Player player, float pt, HeroAnimState state) {
        if (!local(player)) {
            return;
        }
        finishFrame();
        Minecraft mc = Minecraft.getInstance();
        frameTick = player.tickCount;
        framePt = pt;
        frameAnim = anim;
        counted = player instanceof LocalPlayer lp && lp.getAbilities().flying && !player.onGround()
                && !mc.options.getCameraType().isFirstPerson() && pt < 1.0F;
        leaned = state != null && state.wVanilla <= 0.0F;
        wAir = state == null ? -1.0F : state.wAir;
        DRAWN[0] = leaned ? state.renderPitch : 0.0F;
        DRAWN[1] = leaned ? state.renderRoll : 0.0F;
        diag = state == null ? "no state" : String.format("t %.3f pt %.3f %s wAir %.2f pitch %.1f roll %.1f s %.2f sH %.2f u %.2f flying %s ground %s",
                state.time, pt, anim, state.wAir, state.renderPitch, state.renderRoll, state.s, state.sH, state.u, state.flyingEff, state.onGround);
        poses = 0;
        poseFail = null;
        long now = System.nanoTime();
        if (DRIVE && mouseTurn != 0.0F && lastTurnNanos != 0L && player instanceof LocalPlayer lp) {
            // (Entity.turn takes mouse units: 0.15 degrees each)
            lp.turn(mouseTurn * (now - lastTurnNanos) / 5.0E7 / 0.15, 0.0);
        }
        lastTurnNanos = now;
        if (jitter && RANDOM.nextInt(3) == 0) {
            try {
                Thread.sleep(RANDOM.nextInt(45));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * An armour model of this player drawn (from 1.21.9): whether vanilla's pose of it already was the hero's pose of the
     * frame ({@code before}: what 3.14.0 drew) and whether it wears it once dressed.
     */
    public static void armour(Player player, boolean before, boolean dressed) {
        if (!local(player) || !counted) {
            return;
        }
        armourDraws++;
        if (!before) {
            armourVanilla++;
        }
        if (!dressed) {
            armourBare++;
            REASONS.merge("armour drawn in vanilla's pose", 1, Integer::sum);
        }
    }

    /** What {@code HeroPoses.apply} did for a model of this player ("posed", or why it left the vanilla pose). */
    public static void pose(Player player, float ageInTicks, String result, float body, float rightArm, float leftArm,
                            float rightLeg, float leftLeg) {
        if (!local(player)) {
            return;
        }
        DRAWN[2] = body;
        DRAWN[3] = rightArm;
        DRAWN[4] = leftArm;
        DRAWN[5] = rightLeg;
        DRAWN[6] = leftLeg;
        poses++;
        poseCalls++;
        lastPose = result;
        if (!result.equals("posed") && poseFail == null) {
            poseFail = result + String.format(" [age %.4f tick %d pt %.4f]", ageInTicks, player.tickCount, framePt);
        }
    }

    private static void finishFrame() {
        if (!counted) {
            return;
        }
        counted = false;
        frames++;
        totalFrames++;
        if (wAir >= 0.0F && wAir < 0.5F) {
            weakAir++;
        }
        RING[0] = RING[1];
        RING[1] = RING[2];
        RING[2] = DRAWN.clone();
        RING_WHY[0] = RING_WHY[1];
        RING_WHY[1] = RING_WHY[2];
        RING_WHY[2] = diag + " pose " + lastPose;
        RING_HERO[0] = RING_HERO[1];
        RING_HERO[1] = RING_HERO[2];
        RING_HERO[2] = leaned && poseFail == null && poses > 0;
        // (a flash among frames the hero's pose drew: vanilla's own poses, a crawl say, move as vanilla moves them)
        if (RING[0] != null && RING_HERO[0] && RING_HERO[2] && distance(RING[1], RING[0]) > 1.0F && distance(RING[1], RING[2]) > 1.0F
                && distance(RING[0], RING[2]) < 0.5F) {
            flashes++;
            totalFlashes++;
            String key = "flash: one frame unlike the frames around it";
            REASONS.merge(key, 1, Integer::sum);
            if (DETAILS.size() < 12) {
                DETAILS.add(String.format("flash: before %s | flash %s | after %s; drawn %s / %s / %s", RING_WHY[0], RING_WHY[1],
                        RING_WHY[2], java.util.Arrays.toString(RING[0]), java.util.Arrays.toString(RING[1]),
                        java.util.Arrays.toString(RING[2])));
            }
        }
        String why = null;
        if (!leaned) {
            why = "lean not applied: frame " + frameAnim;
        } else if (poses == 0) {
            why = "no pose call this frame (frame " + frameAnim + ")";
        } else if (poseFail != null) {
            why = "pose bailed: " + poseFail + " (frame " + frameAnim + ")";
        }
        if (why != null) {
            unposed++;
            totalUnposed++;
            String key = why.replaceAll("\\[.*?\\]", "").replaceAll("-?\\d+(\\.\\d+)?", "#");
            REASONS.merge(key, 1, Integer::sum);
            if (DETAILS.size() < 12) {
                DETAILS.add(why);
            }
        }
    }

    /** How different two drawn poses look: 10 degrees of lean or 0.3 radians of a limb count 1. */
    private static float distance(float[] a, float[] b) {
        float d = (Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1])) / 10.0F;
        for (int i = 2; i < a.length; i++) {
            d += Math.abs(a[i] - b[i]) / 0.3F;
        }
        return d;
    }

    private static void report(String next) {
        finishFrame();
        if (frames > 0 || unposed > 0) {
            Enhanced.LOGGER.info("Flicker probe: {}: {} frames in flight (F5), {} unposed, {} flashes, {} pose calls, {} with the flight's weight < 0.5{}",
                    phase, frames, unposed, flashes, poseCalls, weakAir,
                    armourDraws > 0 ? ", armour drawn " + armourDraws + " times: " + armourVanilla + " posed by vanilla unlike the body, "
                            + armourBare + " of them left so" : "");
            totalArmour += armourDraws;
            totalArmourVanilla += armourVanilla;
            totalArmourBare += armourBare;
            REASONS.forEach((k, v) -> Enhanced.LOGGER.info("Flicker probe:     {} x {}", v, k));
            DETAILS.forEach(d -> Enhanced.LOGGER.info("Flicker probe:     e.g. {}", d));
        }
        phase = next;
        frames = 0;
        unposed = 0;
        flashes = 0;
        java.util.Arrays.fill(RING, null);
        poseCalls = 0;
        armourDraws = 0;
        armourBare = 0;
        armourVanilla = 0;
        weakAir = 0;
        REASONS.clear();
        DETAILS.clear();
    }

    // =================================================================================================================
    // The scripted flight
    // =================================================================================================================

    private record Step(int ticks, String name, Consumer<LocalPlayer> each) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static boolean asked;
    private static boolean scripted;
    private static int settle;
    private static int index = -1;
    private static int left;
    private static boolean hideArmour = true;

    @Subscribe
    public static void onTick(ClientTickEvent.Post event) {
        if (!DRIVE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!asked && mc.screen instanceof TitleScreen) {
                asked = true;
                mc.options.chatOpacity().set(0.0);
                LevelSettings settings = new LevelSettings("flickerprobe", GameType.CREATIVE, false, Difficulty.EASY, true, new GameRules(),
                        WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("flickerprobe", settings, new WorldOptions(4242L, true, false),
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
            mc.options.framerateLimit().set(260);
            mc.options.enableVsync().set(false);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            // (the armour drawn on heroes too, to see it wear the hero's pose; put back at the end)
            hideArmour = com.kolodkingal.enhanced.client.EnhancedClientConfig.HIDE_HERO_ARMOUR.get();
            com.kolodkingal.enhanced.client.EnhancedClientConfig.HIDE_HERO_ARMOUR.set(false);
            script();
        }
        LocalPlayer player = mc.player;
        if (left <= 0) {
            release(mc);
            index++;
            if (index >= STEPS.size()) {
                report("end");
                com.kolodkingal.enhanced.client.EnhancedClientConfig.HIDE_HERO_ARMOUR.set(hideArmour);
                Enhanced.LOGGER.info("Flicker probe: total {} frames in flight (F5), {} unposed, {} flashes; armour drawn {} times, {} posed by vanilla unlike the body, {} left so",
                        totalFrames, totalUnposed, totalFlashes, totalArmour, totalArmourVanilla, totalArmourBare);
                mc.stop();
                return;
            }
            Step step = STEPS.get(index);
            report(step.name());
            left = step.ticks();
        }
        left--;
        try {
            STEPS.get(index).each().accept(player);
        } catch (RuntimeException e) {
            Enhanced.LOGGER.error("Flicker probe step {} failed", STEPS.get(index).name(), e);
        }
    }

    private static void release(Minecraft mc) {
        mc.options.keyUp.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
        mc.options.keyLeft.setDown(false);
    }

    private static void step(int ticks, String name, Consumer<LocalPlayer> each) {
        STEPS.add(new Step(ticks, name, each));
    }

    private static void once(String name, Consumer<LocalPlayer> action) {
        boolean[] done = {false};
        STEPS.add(new Step(1, name, p -> {
            if (!done[0]) {
                done[0] = true;
                action.accept(p);
            }
        }));
    }

    private static void server(String name, Consumer<ServerPlayer> action, int wait) {
        once(name, local -> {
            Minecraft mc = Minecraft.getInstance();
            var server = mc.getSingleplayerServer();
            if (server == null) {
                return;
            }
            java.util.UUID id = local.getUUID();
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    try {
                        action.accept(p);
                    } catch (RuntimeException e) {
                        Enhanced.LOGGER.error("Flicker probe server step failed", e);
                    }
                }
            });
        });
        step(wait, name + " (wait)", p -> {
        });
    }

    private static void keys(boolean up, boolean sprint, boolean jump, boolean shift) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.keyUp.setDown(up);
        mc.options.keySprint.setDown(sprint);
        mc.options.keyJump.setDown(jump);
        mc.options.keyShift.setDown(shift);
    }

    private static void fly(LocalPlayer p) {
        if (!p.getAbilities().flying && p.mayFly()) {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
    }

    private static void script() {
        String rounds = System.getProperty("enhanced.flickerprobe.rounds", "1,2,3");
        for (int round = 0; round < 3; round++) {
            boolean aged = round == 1;
            boolean survival = round == 2;
            if (!java.util.Arrays.asList(rounds.split(",")).contains(String.valueOf(round + 1))) {
                continue;
            }
            once("round " + round, p -> {
                jitter = aged;
                if (aged) {
                    p.tickCount += AGE;
                }
                Enhanced.LOGGER.info("Flicker probe: round {}: tickCount {}, uneven frames {}, survival {}", round(aged, survival),
                        p.tickCount, jitter, survival);
            });
            for (Power hero : survival ? SURVIVAL_HEROES : HEROES) {
                String h = hero.name().toLowerCase(java.util.Locale.ROOT) + (aged ? " aged" : survival ? " survival" : "");
                server(h + " awaken", p -> {
                    Powers.awaken(p, hero, "", false);
                    if (hero == Power.MAGNETO) {
                        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                                new net.minecraft.world.item.ItemStack(com.kolodkingal.enhanced.registry.ModItems.MAGNETO_HELMET.get()));
                    }
                    if (hero != Power.IRON_MAN) {
                        // (armour on: from 1.21.9 it is posed apart from the body)
                        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
                        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS));
                        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
                    }
                    p.setGameMode(survival ? GameType.SURVIVAL : GameType.CREATIVE);
                    p.getAbilities().invulnerable = true;
                    p.getAbilities().flying = true;
                    p.onUpdateAbilities();
                    p.teleportTo(p.getX(), 130.0, p.getZ());
                }, 40);
                step(30, h + " take-off", p -> {
                    fly(p);
                    keys(false, false, true, false);
                });
                step(60, h + " hover", p -> keys(false, false, false, false));
                turning(40, h + " hover turning (mouse)", 9.0F, p -> keys(false, false, false, false));
                step(60, h + " cruise", p -> keys(true, false, false, false));
                step(60, h + " cruise turning", p -> {
                    keys(true, false, false, false);
                    p.setYRot(p.getYRot() + 5.0F);
                });
                turning(60, h + " cruise turning (mouse)", -6.0F, p -> keys(true, false, false, false));
                step(60, h + " sprint", p -> keys(true, true, false, false));
                step(60, h + " sprint turning", p -> {
                    keys(true, true, false, false);
                    p.setYRot(p.getYRot() + (p.tickCount / 20 % 2 == 0 ? 7.0F : -7.0F));
                });
                turning(60, h + " sprint turning (mouse)", 8.0F, p -> keys(true, true, false, false));
                step(30, h + " stop and go", p -> keys(p.tickCount / 5 % 2 == 0, false, false, false));
                step(25, h + " climb", p -> keys(true, false, true, false));
                step(25, h + " dive", p -> keys(true, false, false, true));
                // landing: down to the ground, then up again
                server(h + " to the ground", p -> {
                    ServerLevel level = (ServerLevel) p.level();
                    BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, p.blockPosition());
                    p.teleportTo(p.getX(), top.getY() + 5.0, p.getZ());
                }, 10);
                step(60, h + " landing", p -> keys(false, false, false, !p.onGround()));
                step(20, h + " standing", p -> keys(false, false, false, false));
                step(30, h + " take-off from the ground", p -> {
                    fly(p);
                    keys(false, false, true, false);
                });
                if (survival) {
                    continue;
                }
                // water: a pool under the flight, skimmed, then swum through under water and flown out of
                server(h + " pool", p -> {
                    ServerLevel level = (ServerLevel) p.level();
                    BlockPos c = p.blockPosition();
                    int y = c.getY() + 20;
                    for (int dx = -6; dx <= 40; dx++) {
                        for (int dz = -12; dz <= 12; dz++) {
                            for (int dy = 0; dy < 4; dy++) {
                                level.setBlock(new BlockPos(c.getX() + dx, y + dy, c.getZ() + dz), Blocks.WATER.defaultBlockState(), 2);
                            }
                        }
                    }
                    p.teleportTo(c.getX() + 0.5, y + 4.6, c.getZ() + 0.5);
                    p.setYRot(-90.0F);
                    p.setYHeadRot(-90.0F);
                }, 20);
                step(40, h + " over water", p -> {
                    fly(p);
                    p.setYRot(-90.0F);
                    keys(true, false, false, false);
                });
                server(h + " under water", p -> p.teleportTo(p.getX() - 20.0, p.getY() - 4.4, p.getZ()), 5);
                step(40, h + " swimming under water", p -> {
                    p.setYRot(-90.0F);
                    keys(true, true, false, false);
                });
                step(25, h + " up out of the water", p -> keys(true, false, true, false));
                step(40, h + " flying on after the water", p -> keys(true, false, false, false));
                // a squeeze: a gap one block high (vanilla's crawl pose, which fades out over the next ticks), flown out of
                server(h + " squeeze", p -> {
                    ServerLevel level = (ServerLevel) p.level();
                    BlockPos c = p.blockPosition().above(12);
                    for (int dx = -2; dx <= 3; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            level.setBlock(c.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 2);
                            level.setBlock(c.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 2);
                            level.setBlock(c.offset(dx, 1, dz), Blocks.STONE.defaultBlockState(), 2);
                        }
                    }
                    p.teleportTo(c.getX() + 0.5, c.getY() + 0.05, c.getZ() + 0.5);
                    p.setYRot(-90.0F);
                    p.setYHeadRot(-90.0F);
                }, 30);
                step(40, h + " out of the squeeze", p -> {
                    fly(p);
                    p.setYRot(-90.0F);
                    keys(true, false, false, false);
                });
            }
        }
    }

    private static int round(boolean aged, boolean survival) {
        return aged ? 2 : survival ? 3 : 1;
    }

    /** A step that turns as a mouse does (every frame), {@code rate} degrees a tick. */
    private static void turning(int ticks, String name, float rate, Consumer<LocalPlayer> each) {
        boolean[] started = {false};
        step(ticks, name, p -> {
            if (!started[0]) {
                started[0] = true;
                mouseTurn = rate;
            }
            each.accept(p);
        });
        once(name + " (end)", p -> mouseTurn = 0.0F);
    }
}

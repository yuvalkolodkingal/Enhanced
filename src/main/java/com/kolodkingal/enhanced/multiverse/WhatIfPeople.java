package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Ctx;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.Option;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.hydra.HydraBossEntity;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.stones.SpaceRealms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The people of the What If universes (3.19.1), on the mod's own talk system (the Asgardians' {@link AsgardDialogue}: the
 * talk panel with the speaker's face, his words typed out and up to three answers). They are {@code enhanced:asgardian}
 * of two roles of their own:
 * <ul>
 *   <li>{@link Role#HYDRA_CITIZEN}: the people of Hydra's city. Under Hydra they whisper how they suffer and what
 *   happened, tell where the rulers are (and which of them has fallen), and thank whoever promises to free them with
 *   what little they have; once the city is free they celebrate and give a reward.</li>
 *   <li>{@link Role#SURVIVOR}: the last of the living in the zombies' Earth-2149, the Gamma Bomb's world and Ultron's
 *   dead one. Rare, hiding; each tells his universe's story, gives advice, and shares his supplies once.</li>
 * </ul>
 * Lines {@code npc.enhanced.hydra_citizen|survivor.<node>[.n]}, answers {@code npc.enhanced.option.whatif_*}. A person
 * is tagged {@link #PERSON}; far from every player he is gone (no world fills up with them).
 */
public final class WhatIfPeople {
    /** The tag of every person of a What If universe, and of one who already gave what he had. */
    public static final String PERSON = "enhanced_whatif_person";
    static final String GAVE = "enhanced_whatif_gave";
    /** How many survivors are round a player at most, how far they are counted, and where a new one comes. */
    static final int SURVIVORS = 2;
    static final int COUNT_RANGE = 96;
    static final int COME_MIN = 20;
    static final int COME_MAX = 40;
    /** How far from every player a person is let be before he is gone. */
    static final int GONE_RANGE = 128;

    private static boolean registered;

    private WhatIfPeople() {
    }

    // ---- the people ----

    /** A new person of {@code role} at the place, his skin at random, tagged as a What If person (and {@code tag}). */
    public static AsgardianEntity person(ServerLevel level, Role role, double x, double y, double z, @Nullable String tag) {
        AsgardianEntity npc = ModEntities.ASGARDIAN.get().create(level);
        npc.setRole(role, level.getRandom().nextInt(role.skins()));
        npc.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), MobSpawnType.NATURAL, null);
        npc.addTag(PERSON);
        if (tag != null) {
            npc.addTag(tag);
        }
        return npc;
    }

    /** Whether the entity is one of the universes' people (by its role, which the client knows too). */
    public static boolean isPerson(Entity entity) {
        return entity instanceof AsgardianEntity npc && npc.role().whatIf();
    }

    /** Whether survivors hide in this universe (the zombies', the Gamma Bomb's, Ultron's). */
    static boolean survivorsIn(ResourceKey<Level> dimension) {
        return dimension == Multiverse.ZOMBIES_KEY || dimension == Multiverse.GAMMA_KEY || dimension == Multiverse.ULTRON_KEY;
    }

    /** Every five seconds of a player in a universe of survivors: now and then one is found hiding near. */
    static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (player.isSpectator() || player.tickCount % 100 != 0 || !survivorsIn(level.dimension())) {
            return;
        }
        int near = level.getEntitiesOfClass(AsgardianEntity.class, player.getBoundingBox().inflate(COUNT_RANGE),
                npc -> npc.role() == Role.SURVIVOR).size();
        if (near >= SURVIVORS || player.getRandom().nextInt(3) != 0) {
            return;
        }
        for (int attempt = 0; attempt < 4; attempt++) {
            double angle = player.getRandom().nextDouble() * Mth.TWO_PI;
            double distance = COME_MIN + player.getRandom().nextDouble() * (COME_MAX - COME_MIN);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.isLoaded(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (level.dimension() == Multiverse.ULTRON_KEY ? y != CityChunkGenerator.GROUND + 1 : !SpaceRealms.safe(level, feet)) {
                continue;
            }
            level.addFreshEntity(person(level, Role.SURVIVOR, x + 0.5, y, z + 0.5, null));
            return;
        }
    }

    /** Every ten seconds: the people no player is near are gone. */
    static void serverTick(net.minecraft.server.MinecraftServer server) {
        if (server.getTickCount() % 200 != 0) {
            return;
        }
        for (ResourceKey<Level> key : List.of(Multiverse.HYDRA_KEY, Multiverse.ZOMBIES_KEY, Multiverse.GAMMA_KEY, Multiverse.ULTRON_KEY)) {
            ServerLevel level = server.getLevel(key);
            if (level == null) {
                continue;
            }
            List<Entity> gone = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (isPerson(entity) && level.getNearestPlayer(entity, GONE_RANGE) == null) {
                    gone.add(entity);
                }
            }
            gone.forEach(Entity::discard);
        }
    }

    // ---- what they say ----

    /** The universe a survivor's lines are of: "zombies", "gamma" or "ultron". */
    static String universe(Ctx ctx) {
        ResourceKey<Level> dimension = ctx.speaker().level().dimension();
        return dimension == Multiverse.ULTRON_KEY ? "ultron" : dimension == Multiverse.GAMMA_KEY ? "gamma" : "zombies";
    }

    static boolean freed(Ctx ctx) {
        return HydraCity.state(ctx.player().server).freed();
    }

    /** Gives what the speaker has, once: his things fall at the player's feet. Returns whether he still had them. */
    static boolean give(Ctx ctx, List<ItemStack> things) {
        Entity speaker = ctx.speaker();
        if (speaker.getTags().contains(GAVE)) {
            return false;
        }
        speaker.addTag(GAVE);
        ServerPlayer player = ctx.player();
        for (ItemStack thing : things) {
            if (!player.getInventory().add(thing.copy())) {
                player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), thing.copy()));
            }
        }
        if (speaker.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, speaker.getX(), speaker.getY() + 1.9, speaker.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        }
        return true;
    }

    /** Registers the two roles' nodes (once; at common setup from HydraEvents.Setup, and by the tests). */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        citizens();
        survivors();
    }

    private static void citizens() {
        Role role = Role.HYDRA_CITIZEN;
        Option history = Option.to("whatif_what_happened", "history");
        Option rulers = new Option("whatif_rulers", AsgardDialogue.ALWAYS, ctx -> {
            HydraCity.State state = HydraCity.state(ctx.player().server);
            boolean skull = state.down(HydraBossEntity.RED_SKULL);
            boolean cap = state.down(HydraBossEntity.CAPTAIN_HYDRA);
            AsgardTalk.show(ctx, skull ? "rulers_skull_down" : cap ? "rulers_cap_down" : "rulers");
        });
        Option promise = Option.to("whatif_free_you", "promise");
        Option bye = Option.close("whatif_bye");
        AsgardDialogue.node(role, "oppressed").when(ctx -> !freed(ctx)).priority(1).variants(12)
                .option(history).option(rulers).option(promise).register();
        AsgardDialogue.node(role, "history").variants(3).option(rulers).option(promise).option(bye).register();
        AsgardDialogue.node(role, "rulers").option(promise).option(history).option(bye).register();
        AsgardDialogue.node(role, "rulers_skull_down").option(promise).option(bye).register();
        AsgardDialogue.node(role, "rulers_cap_down").option(promise).option(bye).register();
        AsgardDialogue.node(role, "promise").variants(3).onShow(ctx -> give(ctx, List.of(new ItemStack(Items.BREAD, 2))))
                .option(rulers).option(bye).register();
        AsgardDialogue.node(role, "freed").when(WhatIfPeople::freed).priority(5).variants(6)
                .option(new Option("whatif_reward", AsgardDialogue.ALWAYS,
                        ctx -> AsgardTalk.show(ctx, give(ctx, List.of(new ItemStack(Items.EMERALD, 2 + ctx.player().getRandom().nextInt(3))))
                                ? "reward" : "reward_given")))
                .option(Option.to("whatif_future", "future")).option(bye).register();
        AsgardDialogue.node(role, "reward").option(Option.to("whatif_future", "future")).option(bye).register();
        AsgardDialogue.node(role, "reward_given").option(Option.to("whatif_future", "future")).option(bye).register();
        AsgardDialogue.node(role, "future").variants(3).option(bye).register();
    }

    private static void survivors() {
        Role role = Role.SURVIVOR;
        Option story = new Option("whatif_what_happened", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx, universe(ctx) + "_story"));
        Option advice = new Option("whatif_advice", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx, universe(ctx) + "_advice"));
        Option supplies = new Option("whatif_supplies", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx,
                give(ctx, supplies(universe(ctx))) ? "supplies" : "supplies_gone"));
        Option bye = Option.close("whatif_bye");
        for (String universe : List.of("zombies", "gamma", "ultron")) {
            AsgardDialogue.node(role, universe).when(ctx -> universe(ctx).equals(universe)).priority(1).variants(4)
                    .option(story).option(advice).option(supplies).register();
            AsgardDialogue.node(role, universe + "_story").variants(2).option(advice).option(supplies).option(bye).register();
            AsgardDialogue.node(role, universe + "_advice").variants(3).option(story).option(supplies).option(bye).register();
        }
        AsgardDialogue.node(role, "supplies").option(story).option(bye).register();
        AsgardDialogue.node(role, "supplies_gone").option(story).option(bye).register();
    }

    /** What a survivor of each universe has to share. */
    static List<ItemStack> supplies(String universe) {
        return switch (universe) {
            case "ultron" -> List.of(new ItemStack(Items.COOKED_BEEF, 3), new ItemStack(Items.REDSTONE, 8), new ItemStack(Items.IRON_INGOT, 3));
            case "gamma" -> List.of(new ItemStack(Items.GOLDEN_CARROT, 3), new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.SHIELD));
            default -> List.of(new ItemStack(Items.BREAD, 4), new ItemStack(Items.ARROW, 16), new ItemStack(Items.IRON_SWORD));
        };
    }
}

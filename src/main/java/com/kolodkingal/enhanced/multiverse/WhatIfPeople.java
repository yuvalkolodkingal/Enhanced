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
 * of two roles of their own, and each skin of a role is one person, with a name, a face and words of his own
 * ({@link Role#personaKey}):
 * <ul>
 *   <li>{@link Role#HYDRA_CITIZEN}: eight people of Hydra's city. Under Hydra each whispers his own life under it and
 *   his own memory of before; all of them tell where the rulers are and thank whoever promises to free them; once the
 *   city is free each celebrates in his own way, gives a reward and says what he will do now.</li>
 *   <li>{@link Role#SURVIVOR}: twenty of the last of the living, each with his own story, advice and supplies: eight in
 *   the zombies' Earth-2149 and eight in the Gamma Bomb's world, found only in the bunkers ({@link Bunkers}), and four
 *   hiding in Ultron's dead streets.</li>
 * </ul>
 * Lines {@code npc.enhanced.hydra_citizen|survivor.p<variant>[_<node>][.n]}, answers {@code npc.enhanced.option.whatif_*}.
 * A person is tagged {@link #PERSON}; far from every player he is gone (no world fills up with them), except the
 * bunkers' people, who live there.
 */
public final class WhatIfPeople {
    /** The tag of every person of a What If universe, and of one who already gave what he had. */
    public static final String PERSON = "enhanced_whatif_person";
    static final String GAVE = "enhanced_whatif_gave";
    /** The survivors of each universe: their skins (persons) are {@code first} to {@code first + count - 1}. */
    public static final int ZOMBIE_SURVIVORS = 0;
    public static final int GAMMA_SURVIVORS = 8;
    public static final int ULTRON_SURVIVORS = 16;
    public static final int SURVIVORS_EACH = 8;
    static final int ULTRON_COUNT = 4;
    /** How many survivors are round a player at most in Ultron's streets, how far they are counted, and where a new one comes. */
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

    /** A new person of {@code role} at the place, one of its people at random, tagged as a What If person (and {@code tag}). */
    public static AsgardianEntity person(ServerLevel level, Role role, double x, double y, double z, @Nullable String tag) {
        return person(level, role, level.getRandom().nextInt(role.skins()), x, y, z, tag);
    }

    /** A new person: the one of {@code role} that is skin {@code variant} (his name over his head, his own words). */
    public static AsgardianEntity person(ServerLevel level, Role role, int variant, double x, double y, double z, @Nullable String tag) {
        AsgardianEntity npc = ModEntities.ASGARDIAN.get().create(level);
        npc.setRole(role, variant);
        npc.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), MobSpawnType.NATURAL, null);
        npc.setCustomName(net.minecraft.network.chat.Component.translatable(role.personaKey(npc.variant())));
        npc.setCustomNameVisible(false);
        npc.addTag(PERSON);
        if (tag != null) {
            npc.addTag(tag);
        }
        return npc;
    }

    /** The person a speaker is (his skin), 0 for any other speaker. */
    static int variant(Ctx ctx) {
        return ctx.speaker() instanceof AsgardianEntity npc ? npc.variant() : 0;
    }

    /** Whether the entity is one of the universes' people (by its role, which the client knows too). */
    public static boolean isPerson(Entity entity) {
        return entity instanceof AsgardianEntity npc && npc.role().whatIf();
    }

    /**
     * Every five seconds of a player in Ultron's streets: now and then one of its survivors is found hiding near. (In
     * 2149 and the Gamma Bomb's world the survivors are only in the bunkers: {@link Bunkers}.)
     */
    static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (player.isSpectator() || player.tickCount % 100 != 0 || level.dimension() != Multiverse.ULTRON_KEY) {
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
            if (y != CityChunkGenerator.GROUND + 1) {
                continue;
            }
            int variant = ULTRON_SURVIVORS + player.getRandom().nextInt(ULTRON_COUNT);
            level.addFreshEntity(person(level, Role.SURVIVOR, variant, x + 0.5, y, z + 0.5, null));
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
                if (isPerson(entity) && !entity.getTags().contains(Bunkers.RESIDENT) && level.getNearestPlayer(entity, GONE_RANGE) == null) {
                    gone.add(entity);
                }
            }
            gone.forEach(Entity::discard);
        }
    }

    // ---- what they say ----

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
        Option history = new Option("whatif_your_story", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx, "p" + variant(ctx) + "_history"));
        Option future = new Option("whatif_future", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx, "p" + variant(ctx) + "_future"));
        Option rulers = new Option("whatif_rulers", AsgardDialogue.ALWAYS, ctx -> {
            HydraCity.State state = HydraCity.state(ctx.player().server);
            boolean skull = state.down(HydraBossEntity.RED_SKULL);
            boolean cap = state.down(HydraBossEntity.CAPTAIN_HYDRA);
            AsgardTalk.show(ctx, skull ? "rulers_skull_down" : cap ? "rulers_cap_down" : "rulers");
        });
        Option promise = Option.to("whatif_free_you", "promise");
        Option bye = Option.close("whatif_bye");
        Option reward = new Option("whatif_reward", AsgardDialogue.ALWAYS,
                ctx -> AsgardTalk.show(ctx, give(ctx, List.of(new ItemStack(Items.EMERALD, 2 + ctx.player().getRandom().nextInt(3))))
                        ? "reward" : "reward_given"));
        for (int v = 0; v < role.skins(); v++) {
            int person = v;
            AsgardDialogue.node(role, "p" + v).when(ctx -> !freed(ctx) && variant(ctx) == person).priority(1).variants(2)
                    .option(history).option(rulers).option(promise).register();
            AsgardDialogue.node(role, "p" + v + "_history").option(rulers).option(promise).option(bye).register();
            AsgardDialogue.node(role, "p" + v + "_free").when(ctx -> freed(ctx) && variant(ctx) == person).priority(5).variants(2)
                    .option(reward).option(future).option(bye).register();
            AsgardDialogue.node(role, "p" + v + "_future").option(bye).register();
        }
        AsgardDialogue.node(role, "rulers").option(promise).option(history).option(bye).register();
        AsgardDialogue.node(role, "rulers_skull_down").option(promise).option(bye).register();
        AsgardDialogue.node(role, "rulers_cap_down").option(promise).option(bye).register();
        AsgardDialogue.node(role, "promise").variants(3).onShow(ctx -> give(ctx, List.of(new ItemStack(Items.BREAD, 2))))
                .option(rulers).option(bye).register();
        AsgardDialogue.node(role, "reward").option(future).option(bye).register();
        AsgardDialogue.node(role, "reward_given").option(future).option(bye).register();
    }

    private static void survivors() {
        Role role = Role.SURVIVOR;
        Option who = new Option("whatif_who", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx, "p" + variant(ctx) + "_who"));
        Option story = new Option("whatif_your_story", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx, "p" + variant(ctx) + "_story"));
        Option advice = new Option("whatif_advice", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx, "p" + variant(ctx) + "_advice"));
        Option supplies = new Option("whatif_supplies", AsgardDialogue.ALWAYS, ctx -> AsgardTalk.show(ctx,
                "p" + variant(ctx) + (give(ctx, supplies(variant(ctx))) ? "_gift" : "_gone")));
        Option bye = Option.close("whatif_bye");
        for (int v = 0; v < role.skins(); v++) {
            int person = v;
            AsgardDialogue.node(role, "p" + v).when(ctx -> variant(ctx) == person).priority(1).variants(2)
                    .option(who).option(story).option(supplies).register();
            AsgardDialogue.node(role, "p" + v + "_who").option(story).option(advice).option(supplies).register();
            AsgardDialogue.node(role, "p" + v + "_story").option(who).option(advice).option(bye).register();
            AsgardDialogue.node(role, "p" + v + "_advice").option(supplies).option(story).option(bye).register();
            AsgardDialogue.node(role, "p" + v + "_gift").option(advice).option(bye).register();
            AsgardDialogue.node(role, "p" + v + "_gone").option(advice).option(bye).register();
        }
    }

    /**
     * What each survivor has to share (by his skin: 2149's, the Gamma Bomb's, Ultron's). Made on first use, not as the class
     * loads: in 26.3 an item stack cannot be made before the items' components are bound (the mod's loading).
     */
    private static final class Supplies {
        static final List<List<ItemStack>> ALL = List.of(
            List.of(new ItemStack(Items.GOLDEN_APPLE, 1), new ItemStack(Items.BREAD, 3)),
            List.of(new ItemStack(Items.COMPASS, 1), new ItemStack(Items.TORCH, 16)),
            List.of(new ItemStack(Items.WHEAT_SEEDS, 8), new ItemStack(Items.BREAD, 6)),
            List.of(new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.REDSTONE, 8)),
            List.of(new ItemStack(Items.BOW, 1), new ItemStack(Items.ARROW, 24)),
            List.of(new ItemStack(Items.GOLDEN_CARROT, 4)),
            List.of(new ItemStack(Items.CANDLE, 4), new ItemStack(Items.COOKED_BEEF, 3)),
            List.of(new ItemStack(Items.IRON_SWORD, 1), new ItemStack(Items.SHIELD, 1)),
            List.of(new ItemStack(Items.MILK_BUCKET, 1), new ItemStack(Items.GOLDEN_CARROT, 2)),
            List.of(new ItemStack(Items.COOKED_CHICKEN, 4), new ItemStack(Items.LEATHER, 4)),
            List.of(new ItemStack(Items.COOKED_BEEF, 6), new ItemStack(Items.HAY_BLOCK, 2)),
            List.of(new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.SHIELD, 1)),
            List.of(new ItemStack(Items.COAL, 12), new ItemStack(Items.COOKED_PORKCHOP, 4)),
            List.of(new ItemStack(Items.GOLDEN_APPLE, 1), new ItemStack(Items.MILK_BUCKET, 1)),
            List.of(new ItemStack(Items.BOOK, 3), new ItemStack(Items.BREAD, 4)),
            List.of(new ItemStack(Items.CROSSBOW, 1), new ItemStack(Items.ARROW, 16)),
            List.of(new ItemStack(Items.REDSTONE, 12), new ItemStack(Items.IRON_INGOT, 3)),
            List.of(new ItemStack(Items.REDSTONE_TORCH, 8), new ItemStack(Items.COOKED_BEEF, 3)),
            List.of(new ItemStack(Items.IRON_INGOT, 5), new ItemStack(Items.COPPER_INGOT, 8)),
            List.of(new ItemStack(Items.GOLDEN_APPLE, 1), new ItemStack(Items.BREAD, 4))
        );
    }

    static List<ItemStack> supplies(int variant) {
        return Supplies.ALL.get(Math.floorMod(variant, Supplies.ALL.size()));
    }
}

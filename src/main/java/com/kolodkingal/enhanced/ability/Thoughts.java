package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.XavierConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.network.ThoughtsPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The thoughts that are heard (3.3): Billy's The Noise (WandaVision 7, "it's so noisy in my head") and Professor X's
 * Hear Thoughts are one system. The client makes every creature in earshot glow in its mood and plays the whispers
 * ({@code TheNoise}); this class gives the whispers words, drawn over the heads ({@link ThoughtsPayload}, every {@link
 * #PERIOD} ticks, the nearest {@link ThoughtsPayload#MAX}).
 *
 * <p>A thought is made from what the creature is doing, so it is true: a player who is hurt, hungry, flying, swimming,
 * sneaking, armed, mining, a hero or out at night thinks of it; a creature that hunts names its prey, one that is hurt
 * wants to run, a baby wants its mother, a pet loves its owner, and each common creature has thoughts of its own (the
 * zombie, the creeper, the villager...). A mind closed to the listener ({@link Telepathy}: Magneto's helmet, a
 * telepath as strong or stronger) gives nothing but its silence. Lang keys {@code thought.enhanced.<kind>.<n>}.
 */
public final class Thoughts {
    /** Ticks between two messages of thoughts. */
    public static final int PERIOD = 40;
    /** A thought changes to another of its kind this often (ticks), so the same creature does not say the same thing. */
    static final int CHANGE = 200;

    /** The kinds of thoughts and how many lines each has ({@code thought.enhanced.<kind>.0} to {@code n - 1}). */
    public static final Map<String, Integer> KINDS = new LinkedHashMap<>();
    /** The creatures with thoughts of their own, by entity type path (fish share one kind). */
    private static final Map<String, String> SPECIES = new LinkedHashMap<>();

    static {
        // Players.
        KINDS.put("player_hurt", 2);
        KINDS.put("player_hungry", 2);
        KINDS.put("player_flying", 2);
        KINDS.put("player_swimming", 1);
        KINDS.put("player_sneaking", 1);
        KINDS.put("player_armed", 2);
        KINDS.put("player_mining", 2);
        KINDS.put("player_hero", 2);
        KINDS.put("player_night", 1);
        KINDS.put("player_idle", 3);
        // Any creature.
        KINDS.put("afraid", 2);
        KINDS.put("hunting", 2);
        KINDS.put("baby", 1);
        KINDS.put("pet", 1);
        KINDS.put("hostile", 2);
        KINDS.put("calm", 2);
        // A mind closed to the listener.
        KINDS.put("closed", 1);
        KINDS.put("shielded", 1);
        // Creatures with thoughts of their own.
        for (String species : new String[]{"zombie", "skeleton", "creeper", "spider", "enderman", "villager", "iron_golem", "cow",
                "sheep", "pig", "chicken", "wolf", "cat", "horse", "bee", "piglin", "witch", "slime", "fox", "fish", "squid", "parrot",
                "rabbit", "goat", "frog", "llama", "panda", "allay", "drowned", "husk"}) {
            KINDS.put(species, species.equals("zombie") || species.equals("creeper") || species.equals("villager") || species.equals("skeleton") ? 2 : 1);
            SPECIES.put(species, species);
        }
        for (String fish : new String[]{"cod", "salmon", "tropical_fish", "pufferfish"}) {
            SPECIES.put(fish, "fish");
        }
        SPECIES.put("glow_squid", "squid");
        SPECIES.put("zombie_villager", "zombie");
        SPECIES.put("stray", "skeleton");
        SPECIES.put("cave_spider", "spider");
        SPECIES.put("donkey", "horse");
        SPECIES.put("mule", "horse");
        SPECIES.put("trader_llama", "llama");
        SPECIES.put("mooshroom", "cow");
        SPECIES.put("wandering_trader", "villager");
        SPECIES.put("magma_cube", "slime");
        // 3.6: the Hydra soldiers and their officer (lane L8's).
        KINDS.put("hydra_soldier", 4);
        SPECIES.put("hydra_soldier", "hydra_soldier");
        KINDS.put("hydra_officer", 3);
        SPECIES.put("hydra_officer", "hydra_officer");
        // 3.7: Deadpool's silly thoughts (a player who is Deadpool: lane L3 picks them), Dogpool's, and the family of
        // Agatha's Cottage (by role: lane L6 picks them).
        KINDS.put("deadpool", 6);
        KINDS.put("dogpool", 3);
        SPECIES.put("dogpool", "dogpool");
        KINDS.put("agatha", 3);
        KINDS.put("nicky", 3);
        KINDS.put("rio", 3);
    }

    /**
     * Whether this player hears thoughts right now: Billy with The Noise on, Professor X with Hear Thoughts on (and not away in a
     * body); 3.4: a custom hero with either ability, on.
     */
    public static boolean listens(Player player, PowerData data) {
        return (data.has(HeroAbility.BILLY_NOISE) && data.toggle(Toggle.THE_NOISE))
                || (data.has(HeroAbility.XAVIER_THOUGHTS) && data.toggle(Toggle.HEAR_THOUGHTS) && !MindLink.isLinked(player));
    }

    /**
     * Whose hearing a listener has, for its range: Professor X's while his Hear Thoughts is on, else Billy's (The Noise). For the
     * two heroes their own power, as before.
     */
    public static Power hearsAs(PowerData data) {
        return data.has(HeroAbility.XAVIER_THOUGHTS) && data.toggle(Toggle.HEAR_THOUGHTS) ? Power.PROFESSOR_X : Power.BILLY;
    }

    /** How far the listener hears (the server's config; Billy's is The Noise's range). */
    public static double range(Power power) {
        if (!EnhancedConfig.SPEC.isLoaded()) {
            return power == Power.PROFESSOR_X ? 24 : 32;
        }
        return power == Power.PROFESSOR_X ? XavierConfig.THOUGHTS_RANGE.get() : EnhancedConfig.NOISE_RANGE.get();
    }

    /** Every player, every tick: a listener is sent the thoughts around him every {@link #PERIOD} ticks. */
    static void tick(ServerPlayer player, PowerData data) {
        if (!listens(player, data) || (player.tickCount + player.getId()) % PERIOD != 0) {
            return;
        }
        player.connection.send(new ThoughtsPayload(around(player, hearsAs(data))));
    }

    /** The thoughts of everything the listener hears, the nearest first. */
    public static List<ThoughtsPayload.Thought> around(ServerPlayer listener, Power power) {
        double range = range(power);
        List<LivingEntity> near = listener.level().getEntitiesOfClass(LivingEntity.class, listener.getBoundingBox().inflate(range),
                e -> e != listener && hasMind(e) && e.distanceToSqr(listener) <= range * range);
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(listener)));
        List<ThoughtsPayload.Thought> out = new ArrayList<>();
        long time = listener.level().getGameTime();
        for (LivingEntity e : near) {
            if (out.size() >= ThoughtsPayload.MAX) {
                break;
            }
            out.add(thought(listener, e, time));
        }
        return out;
    }

    /** Whether this has a mind that thinks: a living creature that is not an armour stand, an illusion or a spectator. */
    static boolean hasMind(LivingEntity e) {
        return e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof LokiIllusionEntity) && !e.isSpectator()
                && !(e instanceof Player p && p.isInvisible() && p.isCreative());
    }

    /** What {@code e} is thinking, as the listener hears it. */
    public static ThoughtsPayload.Thought thought(Player listener, LivingEntity e, long time) {
        Telepathy.Verdict verdict = Telepathy.reach(listener, e);
        if (verdict != Telepathy.Verdict.OPEN) {
            String kind = verdict == Telepathy.Verdict.SHIELDED ? "shielded" : "closed";
            return new ThoughtsPayload.Thought(e.getId(), line(kind, e, time), ThoughtsPayload.CLOSED);
        }
        if (e instanceof Player player) {
            return playerThought(player, time);
        }
        return creatureThought(e, time);
    }

    private static ThoughtsPayload.Thought playerThought(Player p, long time) {
        PowerData data = Powers.get(p);
        if (data.power() == Power.DEADPOOL) {
            // 3.7: a Deadpool's mind is open, and silly whatever he does (lane L3).
            return new ThoughtsPayload.Thought(p.getId(), line("deadpool", p, time, null), ThoughtsPayload.CALM);
        }
        ItemStack held = p.getMainHandItem();
        String kind;
        Object arg = null;
        int mood = ThoughtsPayload.CALM;
        if (p.getHealth() <= p.getMaxHealth() * 0.3F) {
            kind = "player_hurt";
            mood = ThoughtsPayload.AFRAID;
        } else if (p.getFoodData().getFoodLevel() <= 6) {
            kind = "player_hungry";
        } else if (p.getAbilities().flying || p.isFallFlying()) {
            kind = "player_flying";
        } else if (p.isInWater()) {
            kind = "player_swimming";
        } else if (p.isShiftKeyDown()) {
            kind = "player_sneaking";
        } else if (held.is(ItemTags.SWORDS) || held.is(ItemTags.AXES) || held.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem) {
            kind = "player_armed";
            mood = ThoughtsPayload.HOSTILE;
        } else if (held.is(ItemTags.PICKAXES) || held.is(ItemTags.SHOVELS)) {
            kind = "player_mining";
        } else if (data.power().hasPowers()) {
            kind = "player_hero";
            arg = data.power().displayName();
        } else if (p.level().isNight()) {
            kind = "player_night";
        } else {
            kind = "player_idle";
        }
        return new ThoughtsPayload.Thought(p.getId(), line(kind, p, time, arg), mood);
    }

    private static ThoughtsPayload.Thought creatureThought(LivingEntity e, long time) {
        String kind;
        Object arg = null;
        int mood = e instanceof Enemy ? ThoughtsPayload.HOSTILE : ThoughtsPayload.CALM;
        LivingEntity target = e instanceof Mob mob ? mob.getTarget() : null;
        String species = SPECIES.get(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
        if (e.getHealth() <= e.getMaxHealth() * 0.25F) {
            kind = "afraid";
            mood = ThoughtsPayload.AFRAID;
        } else if (target != null && target.isAlive()) {
            kind = "hunting";
            arg = target.getDisplayName();
            mood = ThoughtsPayload.HOSTILE;
        } else if (e instanceof AgeableMob ageable && ageable.isBaby()) {
            kind = "baby";
        } else if ("dogpool".equals(species)) {
            kind = species; // 3.7: Dogpool thinks his own thoughts, though he is a pet (lane L3)
        } else if (e instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() != null) {
            kind = "pet";
            arg = pet.getOwner().getDisplayName();
        } else if (e instanceof com.kolodkingal.enhanced.asgard.npc.AsgardianEntity npc && npc.role().cottage()) {
            kind = npc.role().id(); // (3.7: the family of Agatha's Cottage thinks its own thoughts)
        } else if (species != null) {
            kind = species;
        } else {
            kind = e instanceof Enemy ? "hostile" : "calm";
        }
        return new ThoughtsPayload.Thought(e.getId(), line(kind, e, time, arg), mood);
    }

    private static Component line(String kind, LivingEntity e, long time) {
        return line(kind, e, time, null);
    }

    /** One of the lines of a kind: which one changes every {@link #CHANGE} ticks, and differs from creature to creature. */
    static Component line(String kind, LivingEntity e, long time, Object arg) {
        int n = KINDS.getOrDefault(kind, 1);
        int i = (int) Math.floorMod(e.getId() * 7L + time / CHANGE, n);
        String key = "thought.enhanced." + kind + "." + i;
        return arg == null ? Component.translatable(key) : Component.translatable(key, arg);
    }

    private Thoughts() {
    }
}

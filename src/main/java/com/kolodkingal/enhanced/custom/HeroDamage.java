package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The strength of a custom hero's hero abilities in their damage (3.4): a hit of a built-in hero's damage type dealt by
 * a custom hero (by him, his projectile, his illusion or his drone) is multiplied by the strength factor of the ability of
 * his kit that deals it ({@link HeroKit#damageFactor}) and by {@code custom_hero.damageScale}. The generic modules scale
 * their own numbers ({@link ModuleStats}). Runs at {@link EventPriority#LOW}, after the rules that cancel or set the
 * damage (shields, wards, immunities) and before the ones that only read it.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class HeroDamage {
    /**
     * How the ability of a damage type is found in a kit: the first of the list it has, or the strongest of them. (3.6:
     * a row may be a stone's, {@link StoneRow}.)
     */
    private record Rule(List<CatalogEntry> rows, boolean strongest) {
    }

    private static final Map<ResourceKey<DamageType>, Rule> TYPES = new LinkedHashMap<>();

    static {
        first(ModDamageTypes.CHAOS_BEAM, HeroAbility.WANDA_CHAOS_BEAM);
        first(ModDamageTypes.SHRAPNEL, HeroAbility.MAGNETO_SHRAPNEL);
        first(ModDamageTypes.TELEKINETIC_SLAM, HeroAbility.MAGNETO_METAL, HeroAbility.WANDA_TELEKINESIS, HeroAbility.BILLY_TELEKINESIS,
                HeroAbility.PHOENIX_TELEKINESIS);
        first(ModDamageTypes.MIND_STONE, HeroAbility.VISION_MIND_BEAM);
        // (3.6: Vision's row first, then the Mind Stone's.)
        first(ModDamageTypes.MIND_STONE_BEAM, HeroAbility.VISION_MIND_BEAM, StoneRow.MIND);
        // (L2: every speed strike deals it, the blitzes, the dashes, the barrage, the Whirlwind and the Sonic Boom: the strongest.)
        TYPES.put(ModDamageTypes.SPEED_BLITZ, new Rule(List.<CatalogEntry>of(HeroAbility.QUICKSILVER_BLITZ,
                HeroAbility.QUICKSILVER_SPEED, HeroAbility.QUICKSILVER_BARRAGE, HeroAbility.TOMMY_SPEED,
                HeroAbility.TOMMY_WHIRLWIND, HeroAbility.TOMMY_VIBRATE), true));
        first(ModDamageTypes.MOLECULAR_ACCELERATION, HeroAbility.TOMMY_ACCELERATE);
        first(ModDamageTypes.MJOLNIR, HeroAbility.THOR_HAMMER);
        first(ModDamageTypes.STORMBREAKER, HeroAbility.THOR_HAMMER);
        first(ModDamageTypes.LOKI_DAGGER, HeroAbility.LOKI_DAGGERS);
        first(ModDamageTypes.HEX_BOLT, HeroAbility.BILLY_HEX_BEAM);
        first(ModDamageTypes.HEX_BEAM, HeroAbility.BILLY_HEX_BEAM);
        first(ModDamageTypes.LIGHTNING_BEAM, HeroAbility.THOR_LIGHTNING);
        first(ModDamageTypes.DARKHOLD_BEAM, HeroAbility.AGATHA_PURPLE_BEAM);
        first(ModDamageTypes.PHOENIX_BURST, HeroAbility.PHOENIX_BURST);
        first(ModDamageTypes.WEB_KICK, HeroAbility.SPIDER_BODY);
        first(ModDamageTypes.SPIDER_LEGS, HeroAbility.SPIDER_LEGS);
        List<CatalogEntry> thanos = List.copyOf(HeroAbility.of(Power.THANOS));
        for (ResourceKey<DamageType> type : List.of(ModDamageTypes.THANOS_BLOW, ModDamageTypes.THANOS_SWORD, ModDamageTypes.STONE_POWER,
                ModDamageTypes.STONE_BLAST)) {
            TYPES.put(type, new Rule(thanos, true));
        }
        // (3.6: the Soul Stone's own row deals it too.)
        List<CatalogEntry> soul = new java.util.ArrayList<>(thanos);
        soul.add(StoneRow.SOUL);
        TYPES.put(ModDamageTypes.STONE_SOUL, new Rule(List.copyOf(soul), true));
        first(ModDamageTypes.ULTRON_BOLT, HeroAbility.ULTRON_REPULSOR);
        first(ModDamageTypes.ULTRON_BEAM, HeroAbility.ULTRON_REPULSOR);
        first(ModDamageTypes.ULTRON_SLAM, HeroAbility.ULTRON_GRAVITY);
        first(ModDamageTypes.SAND, HeroAbility.APOCALYPSE_SAND);
        first(ModDamageTypes.ENTOMB, HeroAbility.APOCALYPSE_SAND);
        TYPES.put(ModDamageTypes.OPTIC_BLAST, new Rule(List.copyOf(HeroAbility.of(Power.CYCLOPS)), true));
        first(ModDamageTypes.MIND_BLAST, HeroAbility.XAVIER_MENTAL_BLAST);
        first(ModDamageTypes.AGATHA_DRAIN, HeroAbility.AGATHA_DRAIN);
        first(ModDamageTypes.AGATHA_RUNES, HeroAbility.AGATHA_RUNES);
        // 3.5 (the skeleton): the gods of Asgard.
        first(ModDamageTypes.HOFUND, HeroAbility.HEIMDALL_HOFUND);
        first(ModDamageTypes.BIFROST_STRIKE, HeroAbility.HEIMDALL_STRIKE);
        first(ModDamageTypes.GUNGNIR, HeroAbility.ODIN_GUNGNIR);
        first(ModDamageTypes.ODINFORCE, HeroAbility.ODIN_BANISH);
        first(ModDamageTypes.NECROSWORD, HeroAbility.HELA_NECROSWORDS);
        first(ModDamageTypes.BLADE_WALL, HeroAbility.HELA_BLADE_WALL);
        // 3.6 (the skeleton): Doctor Strange, the Hulk and the stones.
        first(ModDamageTypes.ELDRITCH_WHIP, HeroAbility.STRANGE_WHIP);
        first(ModDamageTypes.CRIMSON_BANDS, HeroAbility.STRANGE_BANDS);
        first(ModDamageTypes.POWER_STONE_BEAM, StoneRow.POWER);
        first(ModDamageTypes.HULK_SMASH, HeroAbility.HULK_SMASH);
        first(ModDamageTypes.HULK_GRAB, HeroAbility.HULK_GRAB_SMASH, HeroAbility.HULK_SMASH);
        first(ModDamageTypes.HULK_CLAP, HeroAbility.HULK_CLAP);
        first(ModDamageTypes.HULK_LEAP, HeroAbility.HULK_LEAP);
        first(ModDamageTypes.HULK_BOULDER, HeroAbility.HULK_BOULDER);
        first(ModDamageTypes.HULK_CHARGE, HeroAbility.HULK_GAMMA_CHARGE, HeroAbility.HULK_BOULDER);
        // 3.7 (the skeleton): Wolverine and Deadpool (the Spin Slash is the claws' too, the Drop-In the katanas', the
        // Trick Shot the pistols').
        first(ModDamageTypes.WOLVERINE_CLAWS, HeroAbility.WOLVERINE_SLASH, HeroAbility.WOLVERINE_SPIN_SLASH,
                HeroAbility.WOLVERINE_CLAWS);
        first(ModDamageTypes.WOLVERINE_POUNCE, HeroAbility.WOLVERINE_POUNCE);
        first(ModDamageTypes.DEADPOOL_KATANA, HeroAbility.DEADPOOL_KATANAS, HeroAbility.DEADPOOL_DROP_IN);
        first(ModDamageTypes.DEADPOOL_BULLET, HeroAbility.DEADPOOL_PISTOLS, HeroAbility.DEADPOOL_TRICK_SHOT);
        // 3.8 (the skeleton): Captain America, Red Hulk, Iron Man, Falcon and Love (the suits' own moves, the Mark 1's
        // flame and the Hulkbuster's punches, are no rows: PLAN 11.1).
        first(ModDamageTypes.CAP_SHIELD, HeroAbility.CAPTAIN_SHIELD_THROW, HeroAbility.CAPTAIN_RICOCHET);
        first(ModDamageTypes.CAP_SLAM, HeroAbility.CAPTAIN_SHIELD_SLAM, HeroAbility.CAPTAIN_SHIELD_CHARGE);
        // (3.8.1: his fists and kicks have their own type now, so that a custom hero's strength scales them.)
        first(ModDamageTypes.CAP_COMBO, HeroAbility.CAPTAIN_COMBO, HeroAbility.CAPTAIN_FLIP_KICK);
        first(ModDamageTypes.RED_HULK_SMASH, HeroAbility.RED_HULK_SMASH);
        first(ModDamageTypes.RED_HULK_BURST, HeroAbility.RED_HULK_BURST, HeroAbility.RED_HULK_HEAT_WAVE);
        first(ModDamageTypes.REPULSOR, HeroAbility.IRON_MAN_REPULSOR);
        first(ModDamageTypes.IRON_BLADE, HeroAbility.IRON_MAN_ENERGY_BLADES);
        first(ModDamageTypes.UNIBEAM, HeroAbility.IRON_MAN_UNIBEAM);
        first(ModDamageTypes.IRON_MISSILE, HeroAbility.IRON_MAN_MISSILES);
        first(ModDamageTypes.REFOCUSER_LIGHTNING, HeroAbility.IRON_MAN_LIGHTNING_REFOCUSER);
        first(ModDamageTypes.DRONE_LASER, HeroAbility.IRON_MAN_DRONES);
        first(ModDamageTypes.FALCON_DART, HeroAbility.FALCON_BLASTERS);
        first(ModDamageTypes.FALCON_DIVE, HeroAbility.FALCON_DIVE);
        first(ModDamageTypes.REDWING_LASER, HeroAbility.FALCON_REDWING);
        first(ModDamageTypes.LOVE_EYE_LASER, HeroAbility.LOVE_EYE_LASER);
        first(ModDamageTypes.LOVE_LIGHTNING, HeroAbility.LOVE_THUNDER_STRIKE);
    }

    private static void first(ResourceKey<DamageType> type, CatalogEntry... rows) {
        TYPES.put(type, new Rule(List.of(rows), false));
    }

    /**
     * The hero ability (3.6: or stone row) of {@code kit} that deals damage of {@code type}, or null (not a hero's
     * type, or not in the kit).
     */
    @Nullable
    public static CatalogEntry abilityOf(ResourceKey<DamageType> type, HeroKit kit) {
        Rule rule = TYPES.get(type);
        if (rule == null) {
            return null;
        }
        CatalogEntry best = null;
        for (CatalogEntry a : rule.rows) {
            if (has(kit, a)) {
                if (!rule.strongest) {
                    return a;
                }
                if (best == null || kit.strength(a) > kit.strength(best)) {
                    best = a;
                }
            }
        }
        return best;
    }

    private static boolean has(HeroKit kit, CatalogEntry e) {
        return e instanceof HeroAbility a ? kit.has(a) : e instanceof StoneRow r && kit.has(r);
    }

    /** The factor a hit of {@code type} by a custom hero with {@code kit} is multiplied by (1 when it is none of his). */
    public static float factor(ResourceKey<DamageType> type, HeroKit kit) {
        CatalogEntry a = abilityOf(type, kit);
        if (a == null) {
            return 1.0F;
        }
        double scale = EnhancedConfig.SPEC.isLoaded() ? CustomHeroConfig.DAMAGE_SCALE.get() : 1.0;
        float factor = a instanceof HeroAbility row ? kit.damageFactor(row) : kit.damageFactor((StoneRow) a);
        return factor * (float) scale;
    }

    /** The player behind a hit: its causing entity, the owner of a thrown thing, of an illusion or of a drone. */
    @Nullable
    static Player attacker(DamageSource source, Level level) {
        Entity e = source.getEntity();
        if (e instanceof LokiIllusionEntity illusion) {
            return illusion.owner();
        }
        if (e instanceof UltronSentryEntity drone && drone.getOwnerUUID() != null) {
            return level.getPlayerByUUID(drone.getOwnerUUID());
        }
        if (e instanceof TraceableEntity traced && !(e instanceof Player)) {
            e = traced.getOwner();
        }
        return e instanceof Player player ? player : null;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Player attacker = attacker(event.getSource(), event.getEntity().level());
        if (attacker == null) {
            return;
        }
        PowerData data = Powers.get(attacker);
        if (data.power() != Power.CUSTOM || data.kit() == null) {
            return;
        }
        ResourceKey<DamageType> type = event.getSource().typeHolder().unwrapKey().orElse(null);
        if (type == null) {
            return;
        }
        float factor = factor(type, data.kit());
        if (factor != 1.0F) {
            event.setAmount(event.getAmount() * factor);
        }
    }

    private HeroDamage() {
    }
}

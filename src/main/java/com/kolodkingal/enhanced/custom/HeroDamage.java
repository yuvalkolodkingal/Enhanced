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
    /** How the ability of a damage type is found in a kit: the first of the list it has, or the strongest of them. */
    private record Rule(List<HeroAbility> rows, boolean strongest) {
    }

    private static final Map<ResourceKey<DamageType>, Rule> TYPES = new LinkedHashMap<>();

    static {
        first(ModDamageTypes.CHAOS_BEAM, HeroAbility.WANDA_CHAOS_BEAM);
        first(ModDamageTypes.SHRAPNEL, HeroAbility.MAGNETO_SHRAPNEL);
        first(ModDamageTypes.TELEKINETIC_SLAM, HeroAbility.MAGNETO_METAL, HeroAbility.WANDA_TELEKINESIS, HeroAbility.BILLY_TELEKINESIS,
                HeroAbility.PHOENIX_TELEKINESIS);
        first(ModDamageTypes.MIND_STONE, HeroAbility.VISION_MIND_BEAM);
        first(ModDamageTypes.MIND_STONE_BEAM, HeroAbility.VISION_MIND_BEAM);
        // (L2: every speed strike deals it, the blitzes, the dashes, the barrage, the Whirlwind and the Sonic Boom: the strongest.)
        TYPES.put(ModDamageTypes.SPEED_BLITZ, new Rule(List.of(HeroAbility.QUICKSILVER_BLITZ, HeroAbility.QUICKSILVER_SPEED,
                HeroAbility.QUICKSILVER_BARRAGE, HeroAbility.TOMMY_SPEED, HeroAbility.TOMMY_WHIRLWIND, HeroAbility.TOMMY_VIBRATE), true));
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
        List<HeroAbility> thanos = HeroAbility.of(Power.THANOS);
        for (ResourceKey<DamageType> type : List.of(ModDamageTypes.THANOS_BLOW, ModDamageTypes.THANOS_SWORD, ModDamageTypes.STONE_POWER,
                ModDamageTypes.STONE_BLAST, ModDamageTypes.STONE_SOUL)) {
            TYPES.put(type, new Rule(thanos, true));
        }
        first(ModDamageTypes.ULTRON_BOLT, HeroAbility.ULTRON_REPULSOR);
        first(ModDamageTypes.ULTRON_BEAM, HeroAbility.ULTRON_REPULSOR);
        first(ModDamageTypes.ULTRON_SLAM, HeroAbility.ULTRON_GRAVITY);
        first(ModDamageTypes.SAND, HeroAbility.APOCALYPSE_SAND);
        first(ModDamageTypes.ENTOMB, HeroAbility.APOCALYPSE_SAND);
        TYPES.put(ModDamageTypes.OPTIC_BLAST, new Rule(HeroAbility.of(Power.CYCLOPS), true));
        first(ModDamageTypes.MIND_BLAST, HeroAbility.XAVIER_MENTAL_BLAST);
        first(ModDamageTypes.AGATHA_DRAIN, HeroAbility.AGATHA_DRAIN);
        first(ModDamageTypes.AGATHA_RUNES, HeroAbility.AGATHA_RUNES);
    }

    private static void first(ResourceKey<DamageType> type, HeroAbility... rows) {
        TYPES.put(type, new Rule(List.of(rows), false));
    }

    /** The hero ability of {@code kit} that deals damage of {@code type}, or null (not a hero's type, or not in the kit). */
    @Nullable
    public static HeroAbility abilityOf(ResourceKey<DamageType> type, HeroKit kit) {
        Rule rule = TYPES.get(type);
        if (rule == null) {
            return null;
        }
        HeroAbility best = null;
        for (HeroAbility a : rule.rows) {
            if (kit.has(a)) {
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

    /** The factor a hit of {@code type} by a custom hero with {@code kit} is multiplied by (1 when it is none of his). */
    public static float factor(ResourceKey<DamageType> type, HeroKit kit) {
        HeroAbility a = abilityOf(type, kit);
        if (a == null) {
            return 1.0F;
        }
        double scale = EnhancedConfig.SPEC.isLoaded() ? CustomHeroConfig.DAMAGE_SCALE.get() : 1.0;
        return kit.damageFactor(a) * (float) scale;
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

package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every number of every power module (3.3, F5), as a function of the power's strength (1 to 10, the slider of the
 * editor): a {@link Stat} has its value at strength 1 and at strength 10 and runs in a straight line between them (a
 * cooldown gets shorter as the power gets stronger, a damage bigger). The server's logic reads its numbers here, and the
 * editor's preview shows the very same ones ({@link #of}), so what the slider promises is what the power does. Damage is
 * multiplied by {@code custom_hero.damageScale}, cooldowns by {@code cooldownScale}, Flight's speed by {@code flightSpeed}
 * (its level-2 value at 0.08), the Force Field by {@code forceFieldStrength}, control times by {@code controlSeconds} and
 * Teleportation's reach by {@code teleportRange}.
 */
public final class ModuleStats {
    /** How a number reads, and what scales it. */
    public enum Kind {
        /** Points of damage (shown in hearts), times {@code damageScale}. */
        DAMAGE,
        /** Points of health (shown in hearts). */
        HEALTH,
        /** Ticks of a cooldown (shown in seconds), times {@code cooldownScale}. */
        COOLDOWN,
        /** Ticks of something lasting (shown in seconds). */
        TICKS,
        /** Blocks. */
        BLOCKS,
        /** A plain number. */
        NUMBER,
        /** A whole number. */
        COUNT,
        /** A fraction, shown as a percentage. */
        PERCENT
    }

    /** What a config option scales a stat by (besides its kind). */
    public enum Scale {
        NONE, FLIGHT, FIELD, CONTROL, TELEPORT, SUPPRESS
    }

    /**
     * One number of a power.
     *
     * @param id   the lang key's last part ({@code stat.enhanced.<id>})
     * @param at1  its value at strength 1
     * @param at10 its value at strength 10
     */
    public record Stat(String id, float at1, float at10, Kind kind, Scale scale) {
        /** The value at {@code strength} (1 to 10), before the config scales it. */
        public float raw(int strength) {
            float t = (Mth.clamp(strength, 1, PowerModule.MAX_STRENGTH) - 1) / (float) (PowerModule.MAX_STRENGTH - 1);
            float v = at1 + (at10 - at1) * t;
            return kind == Kind.COUNT ? Math.round(v) : v;
        }

        /** The value at {@code strength}, scaled by the server's options. */
        public float value(int strength) {
            float v = raw(strength) * (float) configScale();
            return switch (kind) {
                case COUNT -> Math.max(1, Math.round(v));
                case COOLDOWN, TICKS -> Math.max(1, Math.round(v));
                default -> v;
            };
        }

        private double configScale() {
            double s = switch (kind) {
                case DAMAGE -> opt(CustomHeroConfig.DAMAGE_SCALE, 1.0);
                case COOLDOWN -> opt(CustomHeroConfig.COOLDOWN_SCALE, 1.0);
                default -> 1.0;
            };
            return s * switch (scale) {
                case NONE -> 1.0;
                case FLIGHT -> opt(CustomHeroConfig.FLIGHT_SPEED, 0.08) / 0.08;
                case FIELD -> opt(CustomHeroConfig.FORCE_FIELD_STRENGTH, 35.0) / 35.0;
                case CONTROL -> EnhancedConfig.SPEC.isLoaded() ? CustomHeroConfig.CONTROL_SECONDS.get() / 20.0 : 1.0;
                case TELEPORT -> EnhancedConfig.SPEC.isLoaded() ? CustomHeroConfig.TELEPORT_RANGE.get() / 32.0 : 1.0;
                case SUPPRESS -> EnhancedConfig.SPEC.isLoaded() ? CustomHeroConfig.SUPPRESS_SECONDS.get() / 8.0 : 1.0;
            };
        }

        public String nameKey() {
            return "stat.enhanced." + id;
        }

        /** The value as the preview shows it, without its unit ("3.5" hearts, "1.2" seconds, "40%"). */
        public String shown(int strength) {
            float v = value(strength);
            return switch (kind) {
                case DAMAGE, HEALTH -> one(v / 2F);
                case COOLDOWN, TICKS -> one(v / 20F);
                case PERCENT -> Math.round(v * 100F) + "";
                case COUNT -> Math.round(v) + "";
                case NUMBER -> v < 1F ? String.format(Locale.ROOT, "%.2f", v) : one(v);
                case BLOCKS -> one(v);
            };
        }

        /** The lang key of the unit the preview puts after the value ({@code stat.enhanced.unit.<kind>}). */
        public String unitKey() {
            return "stat.enhanced.unit." + kind.name().toLowerCase(Locale.ROOT);
        }

        private static String one(float v) {
            return Math.abs(v - Math.round(v)) < 0.05F ? Math.round(v) + "" : String.format(Locale.ROOT, "%.1f", v);
        }
    }

    private static double opt(com.kolodkingal.enhanced.config.core.ConfigSpec.DoubleValue value, double fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    private static Stat s(String id, float at1, float at10, Kind kind) {
        return new Stat(id, at1, at10, kind, Scale.NONE);
    }

    private static Stat s(String id, float at1, float at10, Kind kind, Scale scale) {
        return new Stat(id, at1, at10, kind, scale);
    }

    // ---- Super Strength ----
    public static final Stat STRENGTH_MELEE = s("melee", 1, 8, Kind.DAMAGE);
    public static final Stat PUNCH_DAMAGE = s("damage", 6, 18, Kind.DAMAGE);
    public static final Stat PUNCH_KNOCKBACK = s("knockback", 1.5F, 4F, Kind.NUMBER);
    public static final Stat PUNCH_COOLDOWN = s("cooldown", 60, 20, Kind.COOLDOWN);
    public static final Stat SLAM_DAMAGE = s("damage", 4, 12, Kind.DAMAGE);
    public static final Stat SLAM_RADIUS = s("radius", 3, 7, Kind.BLOCKS);
    public static final Stat SLAM_COOLDOWN = s("cooldown", 100, 40, Kind.COOLDOWN);
    public static final Stat LEAP_POWER = s("leap", 1.0F, 2.6F, Kind.NUMBER);
    public static final Stat LEAP_COOLDOWN = s("cooldown", 60, 20, Kind.COOLDOWN);
    // ---- Durability ----
    public static final Stat ARMOR = s("armor", 3, 15, Kind.NUMBER);
    public static final Stat TOUGHNESS = s("toughness", 1, 8, Kind.NUMBER);
    public static final Stat KNOCKBACK_RESIST = s("knockback_resist", 0.1F, 0.6F, Kind.PERCENT);
    public static final Stat EXTRA_HEALTH = s("health", 4, 30, Kind.HEALTH);
    public static final Stat HEALTH_KNOCKBACK_RESIST = s("knockback_resist", 0.1F, 0.4F, Kind.PERCENT);
    // ---- Super Speed ----
    public static final Stat RUN_SPEED = s("speed", 0.25F, 1.6F, Kind.PERCENT);
    public static final Stat RUN_HUNGER = s("hunger", 0.4F, 0.1F, Kind.NUMBER);
    public static final Stat BLITZ_DISTANCE = s("distance", 6, 20, Kind.BLOCKS);
    public static final Stat BLITZ_DAMAGE = s("damage", 3, 10, Kind.DAMAGE);
    public static final Stat BLITZ_COOLDOWN = s("cooldown", 80, 30, Kind.COOLDOWN);
    // ---- Flight ----
    public static final Stat FLIGHT_SPEED = s("speed", 0.06F, 0.13F, Kind.NUMBER, Scale.FLIGHT);
    // ---- Healing Factor ----
    public static final Stat REGEN = s("regen", 0.25F, 2F, Kind.HEALTH);
    public static final Stat BURST_HEAL = s("heal", 4, 20, Kind.HEALTH);
    public static final Stat BURST_REGEN = s("regen", 0.1F, 0.5F, Kind.HEALTH);
    public static final Stat BURST_COOLDOWN = s("cooldown", 400, 160, Kind.COOLDOWN);
    // ---- Energy Blast ----
    public static final Stat BEAM_DAMAGE = s("damage", 1, 4, Kind.DAMAGE);
    public static final Stat BEAM_RANGE = s("range", 16, 40, Kind.BLOCKS);
    public static final Stat BEAM_HUNGER = s("hunger", 0.08F, 0.03F, Kind.NUMBER);
    public static final Stat BOLT_DAMAGE = s("damage", 3, 12, Kind.DAMAGE);
    public static final Stat BOLT_RANGE = s("range", 20, 48, Kind.BLOCKS);
    public static final Stat BOLT_COOLDOWN = s("cooldown", 16, 6, Kind.COOLDOWN);
    public static final Stat BURST_DAMAGE = s("damage", 4, 14, Kind.DAMAGE);
    public static final Stat BURST_RADIUS = s("radius", 3, 8, Kind.BLOCKS);
    public static final Stat BURST_COOL = s("cooldown", 120, 50, Kind.COOLDOWN);
    // ---- Super Senses ----
    public static final Stat DANGER_RANGE = s("range", 12, 40, Kind.BLOCKS);
    public static final Stat XRAY_RANGE = s("range", 12, 48, Kind.BLOCKS);
    // ---- Telepathy ----
    public static final Stat SENSE_RANGE = s("range", 12, 48, Kind.BLOCKS);
    public static final Stat READ_RANGE = s("range", 12, 40, Kind.BLOCKS);
    public static final Stat READ_COOLDOWN = s("cooldown", 40, 10, Kind.COOLDOWN);
    // ---- Teleportation ----
    public static final Stat BLINK_RANGE = s("range", 8, 48, Kind.BLOCKS, Scale.TELEPORT);
    public static final Stat BLINK_COOLDOWN = s("cooldown", 80, 16, Kind.COOLDOWN);
    public static final Stat BEHIND_RANGE = s("range", 8, 32, Kind.BLOCKS, Scale.TELEPORT);
    public static final Stat BEHIND_COOLDOWN = s("cooldown", 100, 30, Kind.COOLDOWN);
    // ---- Telekinesis ----
    public static final Stat TK_RANGE = s("range", 8, 32, Kind.BLOCKS);
    public static final Stat TK_THROW = s("throw", 0.6F, 1.6F, Kind.NUMBER);
    // ---- Wall-Crawling ----
    public static final Stat CLIMB_SPEED = s("climb", 0.12F, 0.35F, Kind.NUMBER);
    // ---- Force Field ----
    public static final Stat FIELD_STRENGTH = s("shield", 10, 80, Kind.HEALTH, Scale.FIELD);
    public static final Stat FIELD_TIME = s("duration", 100, 400, Kind.TICKS);
    public static final Stat FIELD_COOLDOWN = s("cooldown", 200, 60, Kind.COOLDOWN);
    // ---- Phasing ----
    public static final Stat PHASE_HUNGER = s("hunger", 0.5F, 0.1F, Kind.NUMBER);
    // ---- Animal Control ----
    public static final Stat ANIMAL_RADIUS = s("radius", 6, 20, Kind.BLOCKS);
    public static final Stat ANIMAL_TIME = s("duration", 100, 600, Kind.TICKS, Scale.CONTROL);
    public static final Stat ANIMAL_COOLDOWN = s("cooldown", 300, 120, Kind.COOLDOWN);
    // ---- Mind Control ----
    public static final Stat MIND_RANGE = s("range", 8, 24, Kind.BLOCKS);
    public static final Stat MIND_TIME = s("duration", 100, 600, Kind.TICKS, Scale.CONTROL);
    public static final Stat MIND_COOLDOWN = s("cooldown", 600, 200, Kind.COOLDOWN);
    // ---- Magic ----
    public static final Stat MYSTIC_DAMAGE = s("damage", 3, 11, Kind.DAMAGE);
    public static final Stat MYSTIC_RANGE = s("range", 20, 40, Kind.BLOCKS);
    public static final Stat MYSTIC_COOLDOWN = s("cooldown", 20, 8, Kind.COOLDOWN);
    public static final Stat WARD_RADIUS = s("radius", 3, 8, Kind.BLOCKS);
    public static final Stat WARD_TIME = s("duration", 100, 300, Kind.TICKS);
    public static final Stat WARD_COOLDOWN = s("cooldown", 600, 300, Kind.COOLDOWN);
    public static final Stat CURSE_TIME = s("duration", 100, 400, Kind.TICKS);
    public static final Stat CURSE_RANGE = s("range", 12, 32, Kind.BLOCKS);
    public static final Stat CURSE_COOLDOWN = s("cooldown", 300, 120, Kind.COOLDOWN);
    // ---- Ice ----
    public static final Stat FREEZE_DAMAGE = s("damage", 2, 8, Kind.DAMAGE);
    public static final Stat FREEZE_TIME = s("duration", 40, 200, Kind.TICKS);
    public static final Stat FREEZE_RANGE = s("range", 20, 40, Kind.BLOCKS);
    public static final Stat FREEZE_COOLDOWN = s("cooldown", 30, 10, Kind.COOLDOWN);
    public static final Stat ICE_PATH_RADIUS = s("radius", 1, 5, Kind.BLOCKS);
    public static final Stat BLIZZARD_DAMAGE = s("damage", 2, 8, Kind.DAMAGE);
    public static final Stat BLIZZARD_RADIUS = s("radius", 3, 9, Kind.BLOCKS);
    public static final Stat BLIZZARD_COOLDOWN = s("cooldown", 160, 60, Kind.COOLDOWN);
    // ---- Weather Control ----
    public static final Stat STORM_DAMAGE = s("damage", 4, 12, Kind.DAMAGE);
    public static final Stat STORM_TIME = s("duration", 100, 400, Kind.TICKS);
    public static final Stat STORM_RADIUS = s("radius", 10, 24, Kind.BLOCKS);
    public static final Stat STORM_COOLDOWN = s("cooldown", 1200, 400, Kind.COOLDOWN);
    public static final Stat CYCLE_COOLDOWN = s("cooldown", 600, 100, Kind.COOLDOWN);
    // ---- Lightning ----
    public static final Stat STRIKE_DAMAGE = s("damage", 5, 16, Kind.DAMAGE);
    public static final Stat STRIKE_RANGE = s("range", 20, 48, Kind.BLOCKS);
    public static final Stat STRIKE_COOLDOWN = s("cooldown", 60, 20, Kind.COOLDOWN);
    public static final Stat CHAIN_DAMAGE = s("damage", 4, 12, Kind.DAMAGE);
    public static final Stat CHAIN_JUMPS = s("jumps", 1, 5, Kind.COUNT);
    public static final Stat CHAIN_COOLDOWN = s("cooldown", 60, 24, Kind.COOLDOWN);
    // ---- Shapeshifting ----
    public static final Stat GROW_SCALE = s("scale", 1.25F, 3F, Kind.NUMBER);
    public static final Stat SHRINK_SCALE = s("scale", 0.75F, 0.25F, Kind.NUMBER);
    public static final Stat CREATURE_RANGE = s("range", 8, 24, Kind.BLOCKS);
    public static final Stat CREATURE_COOLDOWN = s("cooldown", 100, 40, Kind.COOLDOWN);
    // ---- Invisibility ----
    public static final Stat INVISIBLE_HUNGER = s("hunger", 0.6F, 0.1F, Kind.NUMBER);
    // ---- Absorption ----
    public static final Stat ABSORB_CAPACITY = s("capacity", 10, 60, Kind.HEALTH);
    public static final Stat ABSORB_RADIUS = s("radius", 3, 8, Kind.BLOCKS);
    public static final Stat ABSORB_COOLDOWN = s("cooldown", 40, 20, Kind.COOLDOWN);
    public static final Stat DRAIN_DAMAGE = s("damage", 3, 10, Kind.DAMAGE);
    public static final Stat DRAIN_RANGE = s("range", 4, 10, Kind.BLOCKS);
    public static final Stat DRAIN_SUPPRESS = s("suppress", 60, 240, Kind.TICKS, Scale.SUPPRESS);
    public static final Stat DRAIN_COOLDOWN = s("cooldown", 200, 80, Kind.COOLDOWN);
    // ---- Fire ----
    public static final Stat FIREBALL_DAMAGE = s("damage", 3, 10, Kind.DAMAGE);
    public static final Stat FIREBALL_BURN = s("burn", 60, 200, Kind.TICKS);
    public static final Stat FIREBALL_RANGE = s("range", 20, 40, Kind.BLOCKS);
    public static final Stat FIREBALL_COOLDOWN = s("cooldown", 20, 8, Kind.COOLDOWN);
    public static final Stat FLAME_DAMAGE = s("damage", 1, 3, Kind.DAMAGE);
    public static final Stat FLAME_RANGE = s("range", 4, 10, Kind.BLOCKS);
    public static final Stat AURA_DAMAGE = s("damage", 1, 3, Kind.DAMAGE);
    public static final Stat AURA_RADIUS = s("radius", 2, 5, Kind.BLOCKS);
    // ---- Duplication ----
    public static final Stat CLONES = s("count", 1, 4, Kind.COUNT);
    public static final Stat CLONE_TIME = s("duration", 160, 600, Kind.TICKS);
    public static final Stat CLONE_COOLDOWN = s("cooldown", 600, 240, Kind.COOLDOWN);
    public static final Stat DECOYS = s("count", 1, 5, Kind.COUNT);
    public static final Stat DECOY_TIME = s("duration", 120, 400, Kind.TICKS);
    public static final Stat DECOY_COOLDOWN = s("cooldown", 400, 160, Kind.COOLDOWN);
    // ---- Water ----
    public static final Stat JET_PUSH = s("push", 0.4F, 1.6F, Kind.NUMBER);
    public static final Stat JET_RANGE = s("range", 6, 16, Kind.BLOCKS);
    public static final Stat WAVE_DAMAGE = s("damage", 2, 8, Kind.DAMAGE);
    public static final Stat WAVE_RADIUS = s("radius", 3, 8, Kind.BLOCKS);
    public static final Stat WAVE_PUSH = s("push", 0.6F, 1.8F, Kind.NUMBER);
    public static final Stat WAVE_COOLDOWN = s("cooldown", 120, 50, Kind.COOLDOWN);
    // ---- Time ----
    public static final Stat SLOW_RADIUS = s("radius", 4, 14, Kind.BLOCKS);
    public static final Stat SLOW_HUNGER = s("hunger", 0.6F, 0.2F, Kind.NUMBER);
    public static final Stat STOP_TIME = s("duration", 40, 200, Kind.TICKS);
    public static final Stat STOP_RANGE = s("range", 10, 24, Kind.BLOCKS);
    public static final Stat STOP_COOLDOWN = s("cooldown", 400, 160, Kind.COOLDOWN);
    public static final Stat REWIND_TIME = s("rewind", 60, 200, Kind.TICKS);
    public static final Stat REWIND_COOLDOWN = s("cooldown", 600, 200, Kind.COOLDOWN);
    // ---- Technopathy ----
    public static final Stat HACK_RADIUS = s("radius", 3, 12, Kind.BLOCKS);
    public static final Stat HACK_COOLDOWN = s("cooldown", 60, 20, Kind.COOLDOWN);
    public static final Stat COMMAND_RADIUS = s("radius", 8, 24, Kind.BLOCKS);
    public static final Stat COMMAND_TIME = s("duration", 200, 600, Kind.TICKS, Scale.CONTROL);
    public static final Stat COMMAND_COOLDOWN = s("cooldown", 600, 200, Kind.COOLDOWN);
    // ---- Sonic Scream ----
    public static final Stat SCREAM_DAMAGE = s("damage", 3, 12, Kind.DAMAGE);
    public static final Stat SCREAM_RANGE = s("range", 6, 20, Kind.BLOCKS);
    public static final Stat SCREAM_COOLDOWN = s("cooldown", 120, 40, Kind.COOLDOWN);
    public static final Stat RING_DAMAGE = s("damage", 2, 10, Kind.DAMAGE);
    public static final Stat RING_RADIUS = s("radius", 4, 12, Kind.BLOCKS);
    public static final Stat RING_COOLDOWN = s("cooldown", 140, 50, Kind.COOLDOWN);
    // ---- Earth ----
    public static final Stat TREMOR_DAMAGE = s("damage", 3, 12, Kind.DAMAGE);
    public static final Stat TREMOR_RADIUS = s("radius", 3, 10, Kind.BLOCKS);
    public static final Stat TREMOR_COOLDOWN = s("cooldown", 120, 40, Kind.COOLDOWN);
    public static final Stat SPIKES_DAMAGE = s("damage", 4, 14, Kind.DAMAGE);
    public static final Stat SPIKES_LENGTH = s("distance", 6, 20, Kind.BLOCKS);
    public static final Stat SPIKES_COOLDOWN = s("cooldown", 100, 35, Kind.COOLDOWN);
    // ---- Wind ----
    public static final Stat GUST_PUSH = s("push", 0.8F, 3F, Kind.NUMBER);
    public static final Stat GUST_RANGE = s("range", 5, 14, Kind.BLOCKS);
    public static final Stat GUST_DAMAGE = s("damage", 0, 4, Kind.DAMAGE);
    public static final Stat GUST_COOLDOWN = s("cooldown", 60, 20, Kind.COOLDOWN);
    public static final Stat UPDRAFT_LIFT = s("lift", 0.7F, 2F, Kind.NUMBER);
    public static final Stat UPDRAFT_COOLDOWN = s("cooldown", 80, 30, Kind.COOLDOWN);
    public static final Stat FALL_GUARD = s("fall", 0.3F, 0.9F, Kind.PERCENT);

    /** The numbers of a module in one of its options, in the order the editor's preview lists them. */
    public static List<Stat> of(PowerModule module, String option) {
        List<Stat> list = new ArrayList<>();
        switch (module) {
            case SUPER_STRENGTH -> {
                list.add(STRENGTH_MELEE);
                switch (option) {
                    case "slam" -> list.addAll(List.of(SLAM_DAMAGE, SLAM_RADIUS, SLAM_COOLDOWN));
                    case "leap" -> list.addAll(List.of(LEAP_POWER, LEAP_COOLDOWN));
                    default -> list.addAll(List.of(PUNCH_DAMAGE, PUNCH_KNOCKBACK, PUNCH_COOLDOWN));
                }
            }
            case DURABILITY -> list.addAll(option.equals("health") ? List.of(EXTRA_HEALTH, HEALTH_KNOCKBACK_RESIST)
                    : List.of(ARMOR, TOUGHNESS, KNOCKBACK_RESIST));
            case SUPER_SPEED -> list.addAll(option.equals("blitz") ? List.of(BLITZ_DISTANCE, BLITZ_DAMAGE, BLITZ_COOLDOWN)
                    : List.of(RUN_SPEED, RUN_HUNGER));
            case FLIGHT -> list.add(FLIGHT_SPEED);
            case HEALING -> list.addAll(option.equals("burst") ? List.of(BURST_HEAL, BURST_REGEN, BURST_COOLDOWN) : List.of(REGEN));
            case ENERGY_BLAST -> list.addAll(switch (option) {
                case "bolts" -> List.of(BOLT_DAMAGE, BOLT_RANGE, BOLT_COOLDOWN);
                case "burst" -> List.of(BURST_DAMAGE, BURST_RADIUS, BURST_COOL);
                default -> List.of(BEAM_DAMAGE, BEAM_RANGE, BEAM_HUNGER);
            });
            case SUPER_SENSES -> list.add(option.equals("xray") ? XRAY_RANGE : DANGER_RANGE);
            case TELEPATHY -> list.addAll(option.equals("read") ? List.of(READ_RANGE, READ_COOLDOWN) : List.of(SENSE_RANGE));
            case TELEPORT -> list.addAll(option.equals("behind") ? List.of(BEHIND_RANGE, BEHIND_COOLDOWN) : List.of(BLINK_RANGE, BLINK_COOLDOWN));
            case TELEKINESIS -> list.addAll(List.of(TK_RANGE, TK_THROW));
            case WALL_CRAWL -> list.add(CLIMB_SPEED);
            case FORCE_FIELD -> list.addAll(List.of(FIELD_STRENGTH, FIELD_TIME, FIELD_COOLDOWN));
            case PHASING -> list.add(PHASE_HUNGER);
            case ANIMAL_CONTROL -> list.addAll(List.of(ANIMAL_RADIUS, ANIMAL_TIME, ANIMAL_COOLDOWN));
            case MIND_CONTROL -> list.addAll(List.of(MIND_RANGE, MIND_TIME, MIND_COOLDOWN));
            case MAGIC -> list.addAll(switch (option) {
                case "ward" -> List.of(WARD_RADIUS, WARD_TIME, WARD_COOLDOWN);
                case "curse" -> List.of(CURSE_RANGE, CURSE_TIME, CURSE_COOLDOWN);
                default -> List.of(MYSTIC_DAMAGE, MYSTIC_RANGE, MYSTIC_COOLDOWN);
            });
            case ICE -> list.addAll(switch (option) {
                case "ice_path" -> List.of(ICE_PATH_RADIUS);
                case "blizzard" -> List.of(BLIZZARD_DAMAGE, BLIZZARD_RADIUS, BLIZZARD_COOLDOWN);
                default -> List.of(FREEZE_DAMAGE, FREEZE_TIME, FREEZE_RANGE, FREEZE_COOLDOWN);
            });
            case WEATHER -> list.addAll(option.equals("cycle") ? List.of(CYCLE_COOLDOWN)
                    : List.of(STORM_DAMAGE, STORM_TIME, STORM_RADIUS, STORM_COOLDOWN));
            case LIGHTNING -> list.addAll(option.equals("chain") ? List.of(CHAIN_DAMAGE, CHAIN_JUMPS, CHAIN_COOLDOWN)
                    : List.of(STRIKE_DAMAGE, STRIKE_RANGE, STRIKE_COOLDOWN));
            case SHAPESHIFT -> list.addAll(switch (option) {
                case "shrink" -> List.of(SHRINK_SCALE);
                case "creature" -> List.of(CREATURE_RANGE, CREATURE_COOLDOWN);
                default -> List.of(GROW_SCALE);
            });
            case INVISIBILITY -> list.add(INVISIBLE_HUNGER);
            case ABSORPTION -> list.addAll(option.equals("power") ? List.of(DRAIN_DAMAGE, DRAIN_RANGE, DRAIN_SUPPRESS, DRAIN_COOLDOWN)
                    : List.of(ABSORB_CAPACITY, ABSORB_RADIUS, ABSORB_COOLDOWN));
            case FIRE -> list.addAll(switch (option) {
                case "flamethrower" -> List.of(FLAME_DAMAGE, FLAME_RANGE);
                case "aura" -> List.of(AURA_DAMAGE, AURA_RADIUS);
                default -> List.of(FIREBALL_DAMAGE, FIREBALL_BURN, FIREBALL_RANGE, FIREBALL_COOLDOWN);
            });
            case DUPLICATION -> list.addAll(option.equals("decoys") ? List.of(DECOYS, DECOY_TIME, DECOY_COOLDOWN)
                    : List.of(CLONES, CLONE_TIME, CLONE_COOLDOWN));
            case WATER -> list.addAll(option.equals("wave") ? List.of(WAVE_DAMAGE, WAVE_RADIUS, WAVE_PUSH, WAVE_COOLDOWN)
                    : List.of(JET_PUSH, JET_RANGE));
            case TIME -> list.addAll(switch (option) {
                case "stop" -> List.of(STOP_TIME, STOP_RANGE, STOP_COOLDOWN);
                case "rewind" -> List.of(REWIND_TIME, REWIND_COOLDOWN);
                default -> List.of(SLOW_RADIUS, SLOW_HUNGER);
            });
            case TECHNOPATHY -> list.addAll(option.equals("command") ? List.of(COMMAND_RADIUS, COMMAND_TIME, COMMAND_COOLDOWN)
                    : List.of(HACK_RADIUS, HACK_COOLDOWN));
            case SONIC_SCREAM -> list.addAll(option.equals("ring") ? List.of(RING_DAMAGE, RING_RADIUS, RING_COOLDOWN)
                    : List.of(SCREAM_DAMAGE, SCREAM_RANGE, SCREAM_COOLDOWN));
            case EARTH -> list.addAll(option.equals("spikes") ? List.of(SPIKES_DAMAGE, SPIKES_LENGTH, SPIKES_COOLDOWN)
                    : List.of(TREMOR_DAMAGE, TREMOR_RADIUS, TREMOR_COOLDOWN));
            case WIND -> {
                list.addAll(option.equals("updraft") ? List.of(UPDRAFT_LIFT, UPDRAFT_COOLDOWN) : List.of(GUST_PUSH, GUST_RANGE, GUST_DAMAGE, GUST_COOLDOWN));
                list.add(FALL_GUARD);
            }
        }
        return list;
    }

    private ModuleStats() {
    }
}

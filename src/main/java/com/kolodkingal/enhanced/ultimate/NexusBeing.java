package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.config.UltimateConfig;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.magic.MagicFx;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ultimate Wanda (3.17): the Nexus Being. With Ultimate Mode on, the Scarlet Witch's ultimate form is Wanda of the
 * films in the cosmic Nexus suit, deep scarlet with a star field, red chaos round her. While she is the Nexus Being:
 * <ul>
 *   <li>every hurt heals back within two seconds (while she lives through it);</li>
 *   <li>her abilities have no cooldowns and are a brighter red ({@code AbilityColors}), and hit harder;</li>
 *   <li>the whole world she stands in is hers, not a Hex's walls: its sky turns red and runes cover it, and those runes
 *   strip every other hero in it of his powers; she is held there until the form ends, and each world she goes to while
 *   it lasts is hers as well (she holds several at once);</li>
 *   <li>for each hero her runes strip she grows stronger: faster, tougher, her chaos magic harder;</li>
 *   <li>chaos flashes out of everything within {@code ultimate.nexusFlashRange} blocks of her at her enemies
 *   ({@code ultimate.nexusFlashDamage} each, never her);</li>
 *   <li>nobody can step inside her Chaos Shield.</li>
 * </ul>
 * The form lasts until she ends it (Sneak + the Ultimate Form key), dies, or Ultimate Mode goes off; it has no cooldown.
 */
public final class NexusBeing {
    /** Her red, bright: her abilities and her sky. */
    public static final int RED = 0xFF1E3C;
    public static final int GLOW = 0xFFB0C0;
    /** The red of her Chaos Burst: brighter still. */
    public static final int BRIGHT = 0xFF0A22;

    /** Per Nexus Being (server): the worlds she holds, and how many heroes her runes strip. */
    private static final class Hold {
        final Set<ResourceKey<Level>> worlds = new LinkedHashSet<>();
        int stripped;
        long nextFlash;
    }

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    /** The worlds held, as the client was told ({@link UltimateSyncPayload}). */
    private static volatile Set<String> clientHeld = Set.of();

    private NexusBeing() {
    }

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    /** Whether this player is the Nexus Being now (both sides: the Scarlet Witch in her ultimate form). */
    public static boolean is(Player player) {
        return Powers.power(player) == Power.SCARLET_WITCH && Ultimate.on() && Powers.get(player).is(Live.ULTIMATE);
    }

    // ---- the worlds she holds ----

    /** The ids of every world a Nexus Being holds now (server). */
    public static List<String> heldIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Hold hold : HOLDS.values()) {
            for (ResourceKey<Level> key : hold.worlds) {
                ids.add(key.location().toString());
            }
        }
        return new ArrayList<>(ids);
    }

    public static void setHeldOnClient(List<String> ids) {
        clientHeld = Set.copyOf(ids);
    }

    /** Whether a Nexus Being holds this world (the client asks: its sky is red). */
    public static boolean heldOnClient(Level level) {
        return !clientHeld.isEmpty() && clientHeld.contains(level.dimension().location().toString());
    }

    /** The Nexus Being who holds {@code level} (server), or null. */
    private static ServerPlayer holder(ServerLevel level) {
        for (Map.Entry<UUID, Hold> e : HOLDS.entrySet()) {
            if (e.getValue().worlds.contains(level.dimension())) {
                // (a Nexus Being who has left the server holds nothing: the next one's hold counts)
                ServerPlayer wanda = level.getServer().getPlayerList().getPlayer(e.getKey());
                if (wanda != null) {
                    return wanda;
                }
            }
        }
        return null;
    }

    /** Whether her runes strip this player of his powers now (server): a hero, not her, in a world she holds. */
    public static boolean strips(Player player) {
        if (HOLDS.isEmpty() || !(player.level() instanceof ServerLevel level) || player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (Powers.power(player) == Power.NONE && !Magic.isWitch(player)) {
            return false;
        }
        // (one Nexus Being's runes do not strip another: each holds the world for herself)
        if (HOLDS.containsKey(player.getUUID())) {
            return false;
        }
        ServerPlayer wanda = holder(level);
        return wanda != null && wanda != player && !Magic.unbound(player);
    }

    private static void sync(MinecraftServer server) {
        Net.sendToAllPlayers(UltimateSyncPayload.now());
    }

    // ---- the form ----

    /** Every tick of a Nexus Being (the form lasts; server). */
    static void tick(ServerPlayer wanda) {
        ServerLevel level = wanda.serverLevel();
        long now = level.getGameTime();
        Hold hold = HOLDS.computeIfAbsent(wanda.getUUID(), id -> new Hold());
        if (hold.worlds.add(level.dimension())) {
            claim(wanda, level);
            sync(level.getServer());
        }
        // every hurt heals back within two seconds
        if (wanda.isAlive() && wanda.getHealth() < wanda.getMaxHealth()) {
            wanda.heal(Math.max(0.5F, wanda.getMaxHealth() / 40.0F));
        }
        // no cooldowns
        PowerData data = Powers.get(wanda);
        data.shiftCooldowns(now, Long.MAX_VALUE / 4);
        // the runes strip every hero in her worlds; she grows stronger for each
        if (now % 10 == 0) {
            int stripped = 0;
            for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
                if (other != wanda && hold.worlds.contains(other.level().dimension()) && strips(other)) {
                    other.addEffect(new MobEffectInstance(ModEffects.DRAINED, 30, 0, false, false, true), wanda);
                    if (other.getAbilities().flying && !other.isCreative()) {
                        other.getAbilities().flying = false;
                        other.onUpdateAbilities();
                    }
                    stripped++;
                }
            }
            if (stripped > hold.stripped) {
                wanda.displayClientMessage(Component.translatable("message.enhanced.nexus.stronger", stripped)
                        .withStyle(s -> s.withColor(RED).withBold(true)), true);
            }
            hold.stripped = stripped;
            int n = Math.min(stripped, 4);
            wanda.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, Math.min(1 + n, 4), true, false, true));
            wanda.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, Math.min(1 + n / 2, 3), true, false, true));
            wanda.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, n, true, false, true));
        }
        // chaos flashes out of everything near at her enemies
        if (now >= hold.nextFlash) {
            hold.nextFlash = now + 40;
            burst(wanda, level, false);
        }
        // nobody steps inside her shield
        if (data.chaosShieldActive()) {
            keepOut(wanda, level);
        }
    }

    /** The first moment a world is hers: a great ring of red, the sky's runes forming, every player in it told. */
    private static void claim(ServerPlayer wanda, ServerLevel level) {
        MagicFx.send(level, MagicFx.RING, wanda.position(), List.of(), RED, GLOW, 48.0F, 40);
        MagicFx.send(level, MagicFx.STARBURST, wanda.position().add(0, 1.4, 0), List.of(), RED, GLOW, 6.0F, 30);
        level.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7F, 1.4F);
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.translatable("message.enhanced.nexus.claims", wanda.getDisplayName())
                    .withStyle(ChatFormatting.RED), false);
        }
    }

    private static double flashRange() {
        return loaded() ? UltimateConfig.NEXUS_FLASH_RANGE.get() : 50;
    }

    private static float flashDamage() {
        return (float) (loaded() ? UltimateConfig.NEXUS_FLASH_DAMAGE.get() : 15.0);
    }

    /**
     * The Chaos Burst: all the ground and all the sky within {@code ultimate.nexusFlashRange} blocks of her throw red energy
     * in every direction, from under everyone's feet and down out of the sky, and it strikes everyone there but her (those
     * a power of hers may hurt: {@code UltimateForms.targets}). It comes of itself every two seconds while she is the Nexus
     * Being; her signature move ({@code great}) is the same burst at once, twice as hard.
     */
    static void burst(ServerPlayer wanda, ServerLevel level, boolean great) {
        double r = flashRange();
        List<LivingEntity> targets = UltimateForms.targets(wanda, r);
        List<Vec3> struck = new ArrayList<>();
        float damage = flashDamage() * (great ? 2.0F : 1.0F);
        for (LivingEntity e : targets) {
            if (struck.size() < 48) {
                struck.add(e.position().add(0, e.getBbHeight() * 0.5, 0));
            }
            e.invulnerableTime = 0;
            e.hurt(ModDamageTypes.source(level, ModDamageTypes.ULTIMATE, wanda, wanda), damage);
        }
        MagicFx.send(level, MagicFx.CHAOS_BURST, wanda.position(), struck, BRIGHT, GLOW, (float) r, great ? 40 : 26);
        if (great) {
            MagicFx.send(level, MagicFx.RING, wanda.position(), List.of(), BRIGHT, GLOW, (float) r, 30);
            MagicFx.send(level, MagicFx.STARBURST, wanda.position().add(0, 1.4, 0), List.of(), BRIGHT, GLOW, 12.0F, 24);
        }
        level.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.PLAYERS, 1.6F, 0.5F);
        level.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, great ? 1.4F : 0.6F, 1.5F);
        if (great) {
            level.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 1.2F, 0.6F);
        }
    }

    /** Whatever comes inside her Chaos Shield is thrown out of it, and whatever is shot into it falls. */
    private static void keepOut(ServerPlayer wanda, ServerLevel level) {
        Vec3 center = wanda.position().add(0, wanda.getBbHeight() * 0.5, 0);
        double r = WandaAbilities.SHIELD_RADIUS + 0.3;
        for (Entity e : level.getEntities(wanda, wanda.getBoundingBox().inflate(r + 1.0))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(center);
            if (to.lengthSqr() > r * r) {
                continue;
            }
            if (e instanceof Projectile projectile) {
                projectile.discard();
                continue;
            }
            if (e instanceof LivingEntity || e instanceof net.minecraft.world.entity.item.ItemEntity) {
                Vec3 out = to.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : to.normalize();
                e.setDeltaMovement(out.scale(0.9).add(0, 0.25, 0));
                e.hurtMarked = true;
                if (e.position().subtract(center).lengthSqr() < (r - 0.4) * (r - 0.4)) {
                    Vec3 edge = center.add(out.scale(r + 0.3));
                    e.teleportTo(edge.x, Math.max(edge.y - e.getBbHeight() * 0.5, wanda.getY()), edge.z);
                }
            }
        }
    }

    /** The form ended (or she left): her worlds are free again. */
    static void release(ServerPlayer wanda) {
        Hold hold = HOLDS.remove(wanda.getUUID());
        if (hold != null && !hold.worlds.isEmpty()) {
            for (ResourceKey<Level> key : hold.worlds) {
                ServerLevel level = wanda.server.getLevel(key);
                if (level != null) {
                    for (ServerPlayer p : level.players()) {
                        p.displayClientMessage(Component.translatable("message.enhanced.nexus.released")
                                .withStyle(ChatFormatting.GRAY), true);
                    }
                }
            }
            sync(wanda.server);
        }
    }

    /** How many heroes her runes strip now (for her strength). */
    public static int stripped(Player wanda) {
        Hold hold = HOLDS.get(wanda.getUUID());
        return hold == null ? 0 : hold.stripped;
    }

    public static void clear() {
        HOLDS.clear();
        clientHeld = Set.of();
    }

    /** For the tests: the worlds she holds. */
    public static Set<ResourceKey<Level>> worlds(Player wanda) {
        Hold hold = HOLDS.get(wanda.getUUID());
        return hold == null ? Set.of() : new HashSet<>(hold.worlds);
    }
}

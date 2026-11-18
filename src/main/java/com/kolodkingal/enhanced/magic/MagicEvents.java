package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.ItemTossEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.ServerChatEvent;
import com.kolodkingal.enhanced.platform.event.ServerStartingEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppingEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.ultimate.Ultimate;
import com.kolodkingal.enhanced.ultimate.UltimateForms;
import com.kolodkingal.enhanced.ultimate.UltimateSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The game's side of 3.17's magic and Ultimate Mode: the ticks (mana, chants, forms, zones, conjured things, spirits,
 * familiars, the realms), the blows (the forms' and enchantments' strength; Foresight's dodge, Premonition, Reversal, the
 * Siphon, the circles' shelter, the Probability Hex, the Soul Binding, the Time Loop, a chant broken off), the deaths, the
 * Mindscape's private talk, and the lifecycle (Ultimate Mode read with the world, sent to each player; everything forgotten
 * when a player leaves or the server stops).
 */
@Subscriber
public final class MagicEvents {
    /** When an Unbinding's freedom ends, per player. */
    private static final Map<UUID, Long> UNBOUND_UNTIL = new HashMap<>();

    private MagicEvents() {
    }

    static void unboundUntil(ServerPlayer player, long until) {
        UNBOUND_UNTIL.put(player.getUUID(), until);
    }

    // ---- ticks ----

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        com.kolodkingal.enhanced.ultimate.Demiurge.tick(player);
        UltimateForms.tick(player);
        com.kolodkingal.enhanced.ultimate.BespokeForms.tick(player);
        Casting.tick(player);
        Broom.tick(player);
        Realms.tick(player);
        CovenRoad.tick(player);
        CovenRoad.tickHome(player);
        long now = player.level().getGameTime();
        if (player.tickCount % 10 == 0) {
            mana(player);
            freedom(player, now);
        }
        if (player.tickCount % 20 == 0) {
            items(player, now);
        }
        Powers.syncIfDirty(player);
    }

    /** Mana comes back (twice as fast in a coven). */
    private static void mana(ServerPlayer player) {
        if (!Magic.isCaster(player)) {
            return;
        }
        MagicData data = Magic.data(player);
        float max = Magic.maxMana(player);
        if (data.mana() > max) {
            Magic.set(player, data.withMana(max));
            return;
        }
        if (data.mana() >= max) {
            return;
        }
        float gain = Magic.regen(player) * 0.5F * (Magic.inCoven(player) ? 2.0F : 1.0F);
        Magic.set(player, data.withMana(Math.min(max, data.mana() + gain)));
    }

    /** The unbound: nothing holds them; an Unbinding's minute ends. */
    private static void freedom(ServerPlayer player, long now) {
        MagicData data = Magic.data(player);
        if (data.has(MagicData.UNBOUND)) {
            Long until = UNBOUND_UNTIL.get(player.getUUID());
            if (until == null || now >= until) {
                Magic.set(player, data.without(MagicData.UNBOUND));
                UNBOUND_UNTIL.remove(player.getUUID());
            }
        }
        if (Magic.unbound(player)) {
            if (player.hasEffect(ModEffects.SPELLBOUND)) {
                player.removeEffect(ModEffects.SPELLBOUND);
            }
            if (player.hasEffect(ModEffects.CONFUSED)) {
                player.removeEffect(ModEffects.CONFUSED);
            }
        }
    }

    /** Conjured things vanish when their time is up, enchantments fade, a cursed weapon burns its holder. */
    private static void items(ServerPlayer player, long now) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (Conjured.expired(stack, now)) {
                inv.setItem(i, ItemStack.EMPTY);
                MagicFx.burst(player.serverLevel(), player.position().add(0, 1, 0), 0x8FE0FF, 6, 0.3, 0.7F);
                continue;
            }
            Conjured.fade(stack, now);
        }
        if (MagicZones.has(player, "cursed_hand") && !player.getMainHandItem().isEmpty()) {
            player.igniteForSeconds(2);
        }
    }

    @Subscribe
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        MagicZones.tick(server);
        TempBlocks.tick(server);
        com.kolodkingal.enhanced.ultimate.BespokeForms.serverTick(server);
        long now = server.overworld().getGameTime();
        if (now % 10 == 0) {
            Spirits.tick(server);
        }
        if (now % 20 == 0) {
            Familiars.tick(server);
        }
    }

    // ---- blows ----

    @Subscribe
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        boolean magic = source.is(ModTags.MAGIC_DAMAGE);
        // the attacker's side: an ultimate form, an enchanted weapon, a hex of bad luck
        if (attacker instanceof LivingEntity living) {
            if (MagicZones.unlucky(living)) {
                event.setCanceled(true);
                MagicFx.burst((ServerLevel) victim.level(), victim.position().add(0, 1, 0), 0x7A5CFF, 4, 0.3, 0.6F);
                return;
            }
            float factor = UltimateForms.damageFactor(living);
            if (source.getDirectEntity() == living && Conjured.enchanted(living.getMainHandItem(), victim.level().getGameTime())) {
                factor *= 1.5F;
            }
            if (factor != 1.0F) {
                event.setAmount(event.getAmount() * factor);
            }
        }
        // Ultimate Mode's own forms: a tornado stops what is shot or cast into it; their weaknesses and strengths
        if (com.kolodkingal.enhanced.ultimate.BespokeForms.blockedByTornado(victim, source)) {
            event.setCanceled(true);
            MagicFx.burst((ServerLevel) victim.level(), victim.position().add(0, 1, 0), 0xA8F0D4, 8, 0.4, 0.8F);
            return;
        }
        float taken = com.kolodkingal.enhanced.ultimate.BespokeForms.taken(victim, source);
        if (taken != 1.0F) {
            event.setAmount(event.getAmount() * taken);
        }
        if (MagicZones.has(victim, "transmuted")) {
            event.setAmount(event.getAmount() * 1.3F);
        }
        // a circle of protection, the Vishanti's shield: magic from outside (or from an enemy) does not reach in
        if (magic && MagicZones.shields(victim, attacker) && !com.kolodkingal.enhanced.ultimate.BespokeForms.piercesShields(source)) {
            event.setCanceled(true);
            MagicFx.burst((ServerLevel) victim.level(), victim.position().add(0, 1, 0), 0xFFE2A8, 8, 0.4, 0.8F);
            return;
        }
        if (!(victim instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        // Foresight: a blow foreseen is a blow dodged (a third of them)
        if (MagicZones.has(player, "foresight") && attacker != null && player.getRandom().nextFloat() < 0.35F) {
            event.setCanceled(true);
            Vec3 side = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(player.getRandom().nextBoolean() ? 0.5 : -0.5);
            player.setDeltaMovement(player.getDeltaMovement().add(side));
            player.hurtMarked = true;
            MagicFx.burst(level, player.position().add(0, 1, 0), 0xC8B8FF, 8, 0.3, 0.8F);
            player.displayClientMessage(Component.translatable("message.enhanced.spell.dodged").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        // Premonition: the first blow is seen coming, dodged, and answered
        if (MagicZones.has(player, "premonition") && attacker instanceof LivingEntity foe) {
            MagicZones.unbuff(player, "premonition");
            event.setCanceled(true);
            foe.hurt(com.kolodkingal.enhanced.registry.ModDamageTypes.source(level, com.kolodkingal.enhanced.registry.ModDamageTypes.SPELL, player, player),
                    4.0F * Magic.strength(player));
            com.kolodkingal.enhanced.custom.Shots.push(foe, player.position(), 1.2, 0.4);
            player.displayClientMessage(Component.translatable("message.enhanced.spell.foreseen").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        // Reversal: a spell or an arrow comes back on whoever sent it
        if (MagicZones.has(player, "reversal") && attacker instanceof LivingEntity foe && (magic || source.getDirectEntity() instanceof Projectile)) {
            MagicZones.unbuff(player, "reversal");
            event.setCanceled(true);
            if (source.getDirectEntity() instanceof Projectile projectile) {
                MagicZones.reflect(level, projectile, foe);
            } else {
                foe.hurt(com.kolodkingal.enhanced.registry.ModDamageTypes.source(level, com.kolodkingal.enhanced.registry.ModDamageTypes.SPELL, player, player),
                        event.getAmount());
                MagicFx.send(level, MagicFx.BEAMS, player.position().add(0, 1, 0), List.of(foe.position().add(0, 1, 0)), 0xFFD27A, 0xFFFFFF, 1.0F, 10);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
            return;
        }
        // Billy's Siphon: magic aimed at him feeds him
        if (MagicZones.has(player, "siphon") && (magic || source.is(ModTags.ENERGY_DAMAGE))) {
            event.setCanceled(true);
            player.heal(event.getAmount() * 0.5F);
            Magic.set(player, Magic.data(player).withMana(Math.min(Magic.maxMana(player), Magic.data(player).mana() + event.getAmount() * 3)));
            MagicFx.burst(level, player.position().add(0, 1, 0), 0x9FE8FF, 10, 0.4, 1.0F);
            return;
        }
        // a soul bound to his: half of the blow is the bound one's
        MagicZones.Zone bond = MagicZones.soulBindOf(player);
        if (bond != null && bond.target != null && level.getEntity(bond.target) instanceof LivingEntity bound && bound.isAlive()) {
            float half = event.getAmount() * 0.5F;
            event.setAmount(event.getAmount() - half);
            bound.hurt(com.kolodkingal.enhanced.registry.ModDamageTypes.source(level, com.kolodkingal.enhanced.registry.ModDamageTypes.DARK_SPELL, player, player), half);
        }
        // the Time Loop: a blow that would kill him takes him back to the moment he marked
        if (event.getAmount() >= player.getHealth() + player.getAbsorptionAmount() && TimeLoop.saves(player)) {
            event.setCanceled(true);
            return;
        }
        Casting.hurt(player, event.getAmount());
    }

    @Subscribe
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide()) {
            return;
        }
        Spirits.died(e);
        if (e instanceof ServerPlayer player) {
            MagicData data = Magic.data(player);
            if (data.has(MagicData.ROAD_UNBOUND)) {
                Magic.set(player, data.without(MagicData.ROAD_UNBOUND));
            }
            Casting.stop(player);
            UltimateForms.end(player, true);
        }
    }

    @Subscribe
    public static void onToss(ItemTossEvent event) {
        if (Conjured.conjured(event.getEntity().getItem())) {
            event.getEntity().discard();
        }
    }

    @Subscribe
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && TempBlocks.isTemp(level, event.getPos())) {
            event.setCanceled(true);
            TempBlocks.broken(level, event.getPos());
            level.setBlock(event.getPos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }

    // ---- the Mindscape's private talk ----

    @Subscribe
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!Realms.inMindscape(player) || !Magic.enabled()
                || com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded() && !com.kolodkingal.enhanced.config.WitchcraftConfig.MINDSCAPE_PRIVATE_CHAT.get()) {
            return;
        }
        event.setCanceled(true);
        Component line = Component.translatable("chat.enhanced.mindscape", player.getDisplayName(), event.getMessage())
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (Realms.inMindscape(other)) {
                other.sendSystemMessage(line);
            }
        }
    }

    // ---- lifecycle ----

    @Subscribe
    public static void onServerStarting(ServerStartingEvent event) {
        Ultimate.load(event.getServer());
        if (Ultimate.worldSetting() != null) {
            Enhanced.LOGGER.info("Ultimate Mode: {} (the world's own setting)", Ultimate.on() ? "on" : "off");
        }
    }

    @Subscribe
    public static void onServerStopping(ServerStoppingEvent event) {
        TempBlocks.clearAll(event.getServer());
        Realms.clear(event.getServer());
        MagicZones.clear(event.getServer());
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        Ultimate.reset();
        Casting.clear();
        UltimateForms.clear();
        com.kolodkingal.enhanced.ultimate.BespokeForms.clear();
        Spirits.clear();
        Familiars.clear();
        TimeLoop.clear();
        CovenRoad.clear();
        UNBOUND_UNTIL.clear();
    }

    @Subscribe
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Net.sendToPlayer(player, UltimateSyncPayload.now());
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Realms.logout(player);
            CovenRoad.logout(player);
            Casting.forget(player);
            UltimateForms.forget(player);
            com.kolodkingal.enhanced.ultimate.BespokeForms.forget(player.getUUID());
            TimeLoop.forget(player.getUUID());
            MagicZones.endOf(player.server, player.getUUID());
            UNBOUND_UNTIL.remove(player.getUUID());
        }
    }
}

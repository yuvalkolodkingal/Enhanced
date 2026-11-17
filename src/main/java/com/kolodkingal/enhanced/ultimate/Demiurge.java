package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.magic.MagicData;
import com.kolodkingal.enhanced.magic.MagicFx;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Ultimate Billy (3.17): with Ultimate Mode on, Billy is the Demiurge all the time, not only in his star form.
 * <ul>
 *   <li>His body: twenty hearts (40 health), protection like a full set of Netherite armour (armour 20, toughness 12, some
 *   knockback resistance), on top of what he wears, and he heals faster (a heart back every two seconds).</li>
 *   <li>His barrier wraps his whole body ({@code BillyBarrier.covers}); his abilities are a brighter blue with stars in
 *   them ({@code AbilityColors}) and hit {@link #DAMAGE_FACTOR} times as hard ({@code UltimateForms.damageFactor}).</li>
 *   <li>Magic types (Sneak + O): his keys take on another magic user's abilities, in his blue with stars: the Scarlet
 *   Witch's, Doctor Strange's, Agatha's, Loki's; and back to his own. Kept in his magic ({@link MagicData}'s flags, so it is
 *   synced to everyone who sees him) and read into his {@link PowerData#borrowed} on both sides every tick.</li>
 *   <li>The Grant Magic spell and every spell of 3.17 (the Demiurge knows them all, {@code Magic.knows}).</li>
 * </ul>
 * He is drawn in the Demiurge's look (the starry suit, red scarf and cape; the star body in the great red hooded cloak
 * while he flies or casts: {@code client.SuitSkins}, {@code client.magic.DemiurgeFx}).
 */
public final class Demiurge {
    /** The magic users whose abilities the Demiurge can take on, in the order Sneak + O goes through them. */
    public static final List<Power> MAGIC_TYPES = List.of(Power.SCARLET_WITCH, Power.DOCTOR_STRANGE, Power.AGATHA, Power.LOKI);
    /** How many times as hard his abilities (and a borrowed magic user's) hit. */
    public static final float DAMAGE_FACTOR = 1.75F;
    /** The extra health (twenty hearts in all with a player's own ten), armour and toughness (Netherite's), knockback resistance. */
    static final double HEALTH = 20.0;
    static final double ARMOR = 20.0;
    static final double TOUGHNESS = 12.0;
    static final double KNOCKBACK = 0.4;

    private static final ResourceLocation HEALTH_ID = Enhanced.id("demiurge_health");
    private static final ResourceLocation ARMOR_ID = Enhanced.id("demiurge_armor");
    private static final ResourceLocation TOUGHNESS_ID = Enhanced.id("demiurge_toughness");
    private static final ResourceLocation KNOCKBACK_ID = Enhanced.id("demiurge_knockback");

    /** The flag bits of {@link MagicData} that hold the magic type: 0 his own, 1-4 {@link #MAGIC_TYPES}. */
    private static final int TYPE_SHIFT = 12;
    private static final int TYPE_MASK = 0xF << TYPE_SHIFT;

    private Demiurge() {
    }

    /** The magic user whose abilities he has taken on (both sides), or {@link Power#NONE}: his own. */
    public static Power borrowed(Player player) {
        if (!Magic.isDemiurge(player)) {
            return Power.NONE;
        }
        int type = (Magic.data(player).flags() & TYPE_MASK) >>> TYPE_SHIFT;
        return type >= 1 && type <= MAGIC_TYPES.size() ? MAGIC_TYPES.get(type - 1) : Power.NONE;
    }

    /** Sneak + O: on to the next magic type (after the last, his own again). */
    public static void cycle(ServerPlayer player) {
        if (!Magic.isDemiurge(player)) {
            return;
        }
        MagicData magic = Magic.data(player);
        int type = (magic.flags() & TYPE_MASK) >>> TYPE_SHIFT;
        int next = (type + 1) % (MAGIC_TYPES.size() + 1);
        setType(player, next);
    }

    /** Sets his magic type (0: his own), ending whatever the one before left in the world. */
    public static void setType(ServerPlayer player, int type) {
        PowerData data = Powers.get(player);
        Power before = borrowed(player);
        if (before != Power.NONE) {
            PowerTicker.heroEnd(before, player, data);
        }
        MagicData magic = Magic.data(player);
        int flags = magic.flags() & ~TYPE_MASK | (type << TYPE_SHIFT) & TYPE_MASK;
        Magic.set(player, new MagicData(magic.path(), magic.level(), flags, magic.corruption(), magic.selected(), magic.mana()));
        Power now = borrowed(player);
        data.setBorrowed(now);
        data.markDirty();
        Powers.syncIfDirty(player);
        ServerLevel level = player.serverLevel();
        MagicFx.sendOn(level, MagicFx.AWAKEN, player, List.of(), 0x7FE0FF, 0xFFFFFF, 1.0F, 16);
        MagicFx.send(level, MagicFx.STARBURST, player.position().add(0, 1.2, 0), List.of(), 0x7FE0FF, 0xFFFFFF, 2.0F, 14);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.8F);
        Component name = now == Power.NONE ? Component.translatable("message.enhanced.demiurge.own")
                : now.displayName();
        player.displayClientMessage(Component.translatable("message.enhanced.demiurge.magic_type", name)
                .withStyle(s -> s.withColor(0x7FE0FF).withBold(true)), true);
    }

    /** Every tick of every player (server): his body, his healing, and the magic type read into his power data. */
    public static void tick(ServerPlayer player) {
        boolean demiurge = Magic.isDemiurge(player);
        PowerData data = Powers.get(player);
        data.setBorrowed(demiurge ? borrowed(player) : Power.NONE);
        setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, HEALTH, demiurge);
        setModifier(player, Attributes.ARMOR, ARMOR_ID, ARMOR, demiurge);
        setModifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, TOUGHNESS, demiurge);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, KNOCKBACK, demiurge);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        if (!demiurge) {
            if ((Magic.data(player).flags() & TYPE_MASK) != 0 && Powers.power(player) != Power.BILLY) {
                MagicData magic = Magic.data(player); // (no longer Billy: the magic type is forgotten)
                Magic.set(player, new MagicData(magic.path(), magic.level(), magic.flags() & ~TYPE_MASK, magic.corruption(),
                        magic.selected(), magic.mana()));
            }
            return;
        }
        if (player.isAlive() && player.tickCount % 40 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(2.0F);
        }
    }

    private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount,
                                    boolean present) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }
}

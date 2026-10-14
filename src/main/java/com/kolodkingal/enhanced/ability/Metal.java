package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What is metal: the one rule every Magneto power asks (his R, his Armor &amp; Weapon Strip, the Magnetic Pull, the aim of
 * {@link Raycasts#metalBlock}), so they never disagree about what he can take (D-M 2).
 *
 * <p>Metal is everything with metal in it: an item in {@code enhanced:magnetic} (iron, gold, copper and netherite and
 * whatever is made with them) or {@code enhanced:magnetic_armor}; a block in {@code enhanced:magnetic_blocks}; Iron
 * Golems, every minecart, the Sentries and the Hex's Vision ({@code enhanced:magnetic_entities}); the metal bodies (Vision,
 * the Ultron hero, the Iron Spider); a boss whose {@link BossHooks#isMetal} says so; any creature with metal anywhere in its
 * equipment; a falling or lifted metal block; a metal projectile in flight. Never Mjolnir, the Gauntlet or Ultron's Core
 * ({@code enhanced:cannot_be_snatched}), the heroes' own gear (in no metal tag), a phased Vision, or a conjured body (Loki's
 * illusions, {@code enhanced:conjured_bodies}), whose gear is only a copy.
 */
public final class Metal {

    /**
     * Whether Magneto's telekinesis can take this entity (D-M 2.3), asked in this order: a conjured body never; a type in
     * {@code enhanced:magnetic_entities} always; a boss as its {@link BossHooks#isMetal} says; a phased Vision never; Vision,
     * the Ultron hero and the Iron Spider always (their metal is the body); a dropped metal item; a falling or lifted metal
     * block; a metal projectile in flight; and any other creature with a metal item in any slot (armour, either hand, horse
     * armour).
     */
    public static boolean isMagnetic(Entity e) {
        if (isConjured(e)) {
            return false; // what an illusion wears is a copy (D-M 2.5)
        }
        if (e.getType().is(ModTags.MAGNETIC_ENTITIES)) {
            return true;
        }
        if (e instanceof BossHooks boss) {
            return boss.isMetal(); // Ultron's vibranium, Thanos's gold armour and sword, Apocalypse until his true form
        }
        if (e instanceof Player player) {
            if (VisionAbilities.isPhased(player)) {
                return false; // intangible: there is nothing to grip
            }
            if (isMetalHero(player)) {
                return true;
            }
        }
        if (e instanceof ItemEntity item) {
            return isMetalItem(item.getItem());
        }
        if (e instanceof FallingBlockEntity falling) {
            return isMetalBlock(falling.getBlockState());
        }
        if (e instanceof TelekineticBlockEntity lifted) {
            return isMetalBlock(lifted.getBlockState());
        }
        if (e instanceof Projectile projectile) {
            return inFlight(projectile) && e.getType().is(ModTags.METALLIC_PROJECTILES);
        }
        if (e instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (isMetalItem(living.getItemBySlot(slot))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether a grip of Magneto's on this entity holds on, asked every tick of the grip (D-M 3.5): a metal projectile he
     * caught stays held once it is no longer in flight (an arrow carried against a wall turns {@code inGround} in its own
     * tick), and so does a projectile whose boss hook says it is metal (Thanos's sword, R9); anything else while it is
     * {@link #isMagnetic}.
     */
    public static boolean staysMagnetic(Entity e) {
        if (e instanceof BossHooks boss && boss.isMetal()) {
            return true;
        }
        if (e instanceof Projectile && e.getType().is(ModTags.METALLIC_PROJECTILES)) {
            return true;
        }
        return isMagnetic(e);
    }

    /** A projectile still flying: not stuck in the ground, not a trident flying home, not caught by Magneto. */
    public static boolean inFlight(Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow && (arrow.inGround || arrow.isNoPhysics())) {
            return false;
        }
        return !(projectile instanceof FishingHook) && !MagnetoAbilities.isHeld(projectile);
    }

    /**
     * Whether an item is metal: in {@code enhanced:magnetic} or {@code enhanced:magnetic_armor}, and never what no power
     * may take ({@code enhanced:cannot_be_snatched}: Mjolnir, the Gauntlet, Ultron's Core).
     */
    public static boolean isMetalItem(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(ModTags.CANNOT_BE_SNATCHED)
                && (stack.is(ModTags.MAGNETIC_ITEMS) || stack.is(ModTags.MAGNETIC_ARMOR));
    }

    /** Whether a block is metal: in {@code enhanced:magnetic_blocks} (the metal ores among them). */
    public static boolean isMetalBlock(BlockState state) {
        return state.is(ModTags.MAGNETIC_BLOCKS);
    }

    /**
     * Whether a metal block is metal the world made, not one people build with (D-M 2.2): an ore ({@code
     * enhanced:magnetic_ores}) or a raw-metal block (any {@code #c:storage_blocks/raw_*}, which grow in large ore veins;
     * other mods' raw tin or lead blocks too). The Magnetic Pull takes natural metal through rock and from all round him;
     * every other metal block is built, and comes only in sight and from where he looks.
     */
    public static boolean isNaturalMetal(BlockState state) {
        return isMetalBlock(state) && (state.is(ModTags.MAGNETIC_ORES) || state.getTags().anyMatch(Metal::isRawStorageTag));
    }

    /** A {@code c:storage_blocks/raw_*} tag: the blocks of raw metal. */
    private static boolean isRawStorageTag(TagKey<Block> tag) {
        return tag.location().getNamespace().equals("c") && tag.location().getPath().startsWith("storage_blocks/raw_");
    }

    /**
     * Whether the metal of this entity is its body, not only what it wears (an Iron Golem, a Sentry, the Hex's Vision,
     * Vision, the Ultron hero, the Iron Spider, a boss whose hook says it is metal): the strip and the pull tell it apart
     * from a creature in metal gear, and the strip says "lift it with R" at it. Minecarts are metal but no body; a phased
     * Vision is no metal at all.
     */
    public static boolean isMetalBody(Entity e) {
        if (isConjured(e)) {
            return false;
        }
        if (e.getType().is(ModTags.MAGNETIC_ENTITIES)) {
            return !(e instanceof AbstractMinecart);
        }
        if (e instanceof BossHooks boss) {
            return boss.isMetal();
        }
        return e instanceof Player player && !VisionAbilities.isPhased(player) && isMetalHero(player);
    }

    /** Vision and White Vision, the Ultron hero in every look, and the Iron Spider: heroes whose body is metal (D-M 2.3). */
    private static boolean isMetalHero(Player player) {
        Power power = Powers.get(player).power();
        return power == Power.VISION || power == Power.ULTRON || SpiderServer.isIronSpider(player);
    }

    /**
     * Whether this body is conjured ({@code enhanced:conjured_bodies}: Loki's illusions): its gear is a copy, so it is never
     * metal and nothing is ever taken from it; the strip and the pull dissolve it instead ({@link #dissolve}).
     */
    public static boolean isConjured(Entity e) {
        return e.getType().is(ModTags.CONJURED_BODIES);
    }

    /**
     * Whether what {@code entity} has in {@code slot} is real gear, which taking it gives (D-M 2.5): not on a conjured body,
     * and not on a mob whose drop chance for the slot is 0 or less (a Horseman's gear, a hand Tommy's Blitz emptied), so
     * that taking it never makes what the creature itself would never drop. Such a piece {@link #crumble}s when it is torn
     * off. Players, armour stands and every mob's ordinary gear are real.
     */
    public static boolean isRealGear(LivingEntity entity, EquipmentSlot slot) {
        if (isConjured(entity)) {
            return false;
        }
        return !(entity instanceof Mob mob) || mob.getEquipmentDropChance(slot) > 0.0F;
    }

    /**
     * Gear that was not real, torn off: it crumbles where it was worn, in its own item's fragments, with a low break
     * (D-M 2.5). Nothing drops and nothing is given.
     */
    public static void crumble(ServerLevel level, LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        double y = entity.getY() + entity.getBbHeight() * slotHeight(slot);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), entity.getX(), y, entity.getZ(), 12,
                entity.getBbWidth() * 0.3, 0.1, entity.getBbWidth() * 0.3, 0.08);
        level.playSound(null, entity.getX(), y, entity.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** Where on a body the slot is worn, as a share of its height. */
    private static double slotHeight(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0.9;
            case CHEST, BODY -> 0.65;
            case LEGS -> 0.4;
            case FEET -> 0.1;
            case MAINHAND, OFFHAND -> 0.5;
        };
    }

    /**
     * Magnetism saw through a conjured body: it dissolves (a Loki illusion into its warm-white light, with his green flash
     * where it was touched, {@code touched} or its middle), and nothing of what it wore is given.
     */
    public static void dissolve(Entity body, @Nullable Vec3 touched) {
        if (body instanceof LokiIllusionEntity illusion) {
            illusion.dissolve(touched);
        } else {
            body.discard();
        }
    }

    private Metal() {
    }
}

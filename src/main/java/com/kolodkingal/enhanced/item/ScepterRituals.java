package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.StoneUse;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * What Loki's Scepter (the Mind Stone) and the Tesseract (the Space Stone) do when they are used with another item in
 * the other hand: one table for both, read in order, so that the five behaviours the heroes of 3.0 hang on them never
 * become separate {@code if} chains in separate files.
 *
 * <pre> Scepter hand | other hand       | gesture                   | ritual Scepter      | Ultron's Core    | use
 * | ULTRON_CORE   (a powerless player becomes Ultron) Scepter      | Ultron Program   | Sneak + use on a block    |
 * ULTRON_FIGHT  (the Ultron boss wakes) Scepter      | Ultron Program   | use                       | ULTRON_HINT
 * ("aim at the ground", nothing else) Scepter      | Tesseract        | hold use (5 s)            | THANOS        (the
 * Mad Titan is called) Scepter      | Wanda, Vision or Ultron | use                | RECOGNISES    (the stone knows its
 * own) Scepter      | Scarlet W. Gene  | use                       | WANDA_GENE    (the Scarlet Witch awakens) Scepter
 * | Synthezoid Core  | use                       | VISION_CORE   (Vision awakens) Scepter      | nothing          |
 * Sneak + hold use          | MIND_BEAM     (3.6: the vessel, the Mind Stone's beam) Scepter      | anything else    |
 * use                       | EXPLODES      (the stone's energy tears you apart) Tesseract    | Scepter          | hold
 * use (5 s)            | THANOS Tesseract    | any              | Sneak + use               | REALMS        (3.6: the
 * vessel, the Realm Picker) Tesseract    | any, a hero      | use                       | JUMP          (to the block
 * you look at) Tesseract    | Scarlet W. Gene  | use                       | WANDA_GENE Tesseract    | anything else
 * | use                       | ESCAPE        (Loki's escape, in 2012) </pre>
 *
 * The rows are chosen by the pure functions {@link #scepter} and {@link #tesseract} (checked row by row by the game
 * tests); {@link #useScepter} and {@link #useTesseract} carry them out. The rows before 3.0 do exactly what the items
 * did before. The rows of the villains need their lanes: the Ultron fight is started through {@link BossFights}, which
 * declines until the Ultron lane registers its fight, and the Thanos summoning is declined until the Thanos lane
 * registers its hold ({@link #registerThanosHold}); the Scepter and Tesseract then do what they always did.
 *
 * <p>3.6: the two vessel rows go to the stones ({@code StoneUse}, lane L2), inserted before the old fallbacks, so every
 * old row is unchanged for the same inputs (a plain use with nothing in the other hand still {@code EXPLODES}). While
 * no stone power answers (the skeleton), a vessel row does what its old row did.
 */
public final class ScepterRituals {
    /** How long the Scepter and the Tesseract are held to call the Mad Titan: five seconds. */
    public static final int HOLD_TICKS = 100;

    /** The rows of the table. */
    public enum Ritual {
        RECOGNISES, WANDA_GENE, VISION_CORE, ULTRON_CORE, ULTRON_FIGHT, ULTRON_HINT, THANOS, EXPLODES, JUMP, ESCAPE,
        /**
         * 3.6: the Scepter as the Mind Stone's vessel: Sneak + hold use with an empty other hand fires the Mind Beam.
         */
        MIND_BEAM,
        /** 3.6: the Tesseract as the Space Stone's vessel: Sneak + use opens the Realm Picker. */
        REALMS
    }

    /**
     * The summoning of Thanos, held with the Scepter and the Tesseract in the two hands: the Thanos lane registers one
     * with {@link #registerThanosHold}. While there is none, the row is declined.
     */
    public interface Hold {
        /**
         * Whether the hold begins (the caller then starts using the item). {@code hand} is the hand of the item that is
         * being used (the Scepter's for a use of the Scepter, the Tesseract's for a use of the Tesseract); the other
         * hand holds the other item. A refusal answers false after telling the player why: nothing else happens (the
         * Scepter does not explode, the Tesseract does not jump or make a Loki).
         */
        boolean begin(ServerPlayer player, InteractionHand hand);

        /** Each tick of the hold; {@code remaining} counts down to 0. */
        void tick(ServerPlayer player, ItemStack stack, int remaining);

        /** The hold was let go of, or the item swapped out of the hand. */
        void release(ServerPlayer player, ItemStack stack);

        /** The five seconds are up. */
        void finish(ServerPlayer player, ItemStack stack);
    }

    private static final Map<BossKind, Hold> HOLDS = new EnumMap<>(BossKind.class);

    /**
     * Registers the Thanos lane's summoning (once, when the mod loads); null takes it away (a test that registered its
     * own puts the old one back). Once there is one, the rows of the Mad Titan are the lane's alone: when its
     * {@link Hold#begin} refuses, the player is told why and nothing else happens (the Scepter does not explode, the
     * Tesseract does not take them away).
     *
     * @return the hold that was registered before, or null
     */
    @Nullable
    public static Hold registerThanosHold(@Nullable Hold hold) {
        return hold == null ? HOLDS.remove(BossKind.THANOS) : HOLDS.put(BossKind.THANOS, hold);
    }

    @Nullable
    private static Hold thanos() {
        return HOLDS.get(BossKind.THANOS);
    }

    /** The summoning that is registered, or null (what a test saves and puts back). */
    @Nullable
    public static Hold thanosHold() {
        return thanos();
    }

    // ---- the table ----

    /**
     * The row for a Scepter used by a player with {@code power}, with {@code other} in the other hand.
     *
     * @param sneaking whether the player sneaks
     * @param onBlock  whether the use is on a block (the Scepter's {@code useOn}) rather than in the air
     */
    public static Ritual scepter(Power power, ItemStack other, boolean sneaking, boolean onBlock) {
        if (other.is(ModItems.ULTRON_CORE.get())) {
            return Ritual.ULTRON_CORE;
        }
        if (other.is(ModItems.ULTRON_PROGRAM.get())) {
            return sneaking && onBlock ? Ritual.ULTRON_FIGHT : Ritual.ULTRON_HINT;
        }
        if (other.is(ModItems.TESSERACT.get())) {
            return Ritual.THANOS;
        }
        // 3.6: the vessel (before the old fallbacks: a plain use with nothing in the other hand still explodes).
        if (sneaking && other.isEmpty()) {
            return Ritual.MIND_BEAM;
        }
        return beforeTheVillains(power, other);
    }

    /** The rows the Scepter had before the villains: what it still does where theirs are declined. */
    private static Ritual beforeTheVillains(Power power, ItemStack other) {
        if (power == Power.SCARLET_WITCH || power == Power.VISION || power == Power.ULTRON) {
            return Ritual.RECOGNISES;
        }
        if (other.is(ModItems.SCARLET_WITCH_GENE.get())) {
            return Ritual.WANDA_GENE;
        }
        if (other.is(ModItems.SYNTHEZOID_CORE.get())) {
            return Ritual.VISION_CORE;
        }
        return Ritual.EXPLODES;
    }

    /**
     * The row for a Tesseract used (not sneaking) by a player with {@code power}, with {@code other} in the other hand.
     */
    public static Ritual tesseract(Power power, ItemStack other) {
        return tesseract(power, other, false);
    }

    /** 3.6: the row for a Tesseract used by a player with {@code power}, with {@code other} in the other hand. */
    public static Ritual tesseract(Power power, ItemStack other, boolean sneaking) {
        if (other.is(ModItems.LOKIS_SCEPTER.get())) {
            return Ritual.THANOS;
        }
        // 3.6: the vessel (before the old fallbacks).
        if (sneaking) {
            return Ritual.REALMS;
        }
        if (power.hasPowers()) {
            return Ritual.JUMP;
        }
        return other.is(ModItems.SCARLET_WITCH_GENE.get()) ? Ritual.WANDA_GENE : Ritual.ESCAPE;
    }

    // ---- the Scepter ----

    /**
     * The player touches the Mind Stone with the Scepter in {@code scepterHand}; the other hand may hold what the stone
     * answers to.
     *
     * @param onBlock whether the use is on a block (a Sneak + use on open ground wakes Ultron)
     * @return true if a hold began (the caller starts using the Scepter); false for everything else
     */
    public static boolean useScepter(ServerPlayer player, InteractionHand scepterHand, boolean onBlock) {
        ServerLevel level = player.serverLevel();
        Power power = Powers.power(player);
        InteractionHand otherHand = other(scepterHand);
        ItemStack other = player.getItemInHand(otherHand);
        Ritual ritual = scepter(power, other, player.isShiftKeyDown(), onBlock);
        if (ritual == Ritual.THANOS) {
            Hold hold = thanos();
            if (hold != null) {
                // The lane answers for this row, also when it refuses (it tells the player why): never the explosion.
                return hold.begin(player, scepterHand);
            }
            // The Mad Titan cannot be called yet: the Scepter does what it always did with a Tesseract.
            ritual = beforeTheVillains(power, other);
        }
        if (ritual == Ritual.MIND_BEAM) {
            // 3.6: the Mind Stone's beam, held (the stones answer, lane L2); while nothing answers, the old row.
            if (StoneUse.use(player, StoneKind.MIND, StoneUse.Source.VESSEL, true, true)) {
                return true;
            }
            ritual = beforeTheVillains(power, other);
        }
        switch (ritual) {
            case RECOGNISES -> {
                player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.recognizes").withStyle(ChatFormatting.GOLD), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
                InfinityStone.MIND.use(level, player.position(), InfinityStone.UNDER, 1.0F);
            }
            case WANDA_GENE -> {
                if (refuseIfPowered(player, power)) {
                    return false;
                }
                InfinityStone.MIND.use(level, player.position(), InfinityStone.UNDER, 1.0F);
                ScarletWitchGeneItem.awaken(player, other);
            }
            case VISION_CORE -> {
                if (refuseIfPowered(player, power)) {
                    return false;
                }
                other.consume(1, player);
                InfinityStone.MIND.use(level, player.position(), InfinityStone.UNDER, 1.0F);
                lightning(level, player);
                Powers.awaken(player, Power.VISION);
            }
            case ULTRON_CORE -> {
                if (refuseIfPowered(player, power)) {
                    return false;
                }
                other.consume(1, player);
                InfinityStone.MIND.use(level, player.position(), InfinityStone.UNDER, 1.0F);
                lightning(level, player);
                Powers.awaken(player, Power.ULTRON);
            }
            case ULTRON_FIGHT -> {
                InfinityStone.MIND.use(level, player.position(), InfinityStone.UNDER, 1.0F);
                // BossFights tells the player why it cannot start (no lane yet, Peaceful, another fight, the site).
                BossFights.start(BossKind.ULTRON, player, false);
            }
            case ULTRON_HINT -> {
                // The stone is turned away, as it is for a gene: nothing is used up, nobody is hurt.
                InfinityStone.MIND.use(level, player.position(), InfinityStone.UNDER, 1.2F);
                player.displayClientMessage(Component.translatable("message.enhanced.ultron_program.aim").withStyle(ChatFormatting.GRAY), true);
            }
            default -> explode(player);
        }
        return false;
    }

    /** The Scepter's {@code useOn}: only the Sneak + use that wakes Ultron is a use on a block; false for any other. */
    public static boolean useScepterOnBlock(ServerPlayer player, InteractionHand scepterHand) {
        if (scepter(Powers.power(player), player.getItemInHand(other(scepterHand)), player.isShiftKeyDown(), true) != Ritual.ULTRON_FIGHT) {
            return false;
        }
        useScepter(player, scepterHand, true);
        return true;
    }

    /** A hero who is not Wanda or Vision, or any power at all, is told that the stone has nothing for them. */
    private static boolean refuseIfPowered(ServerPlayer player, Power power) {
        if (power.hasPowers()) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.already_powered").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        return false;
    }

    /**
     * No gene or synthezoid body: the Mind Stone's energy is too much for an ordinary body. The stone sings lower and
     * louder than it does for a friend.
     */
    private static void explode(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        InfinityStone.MIND.use(level, player.position(), 1.0F, 0.75F);
        level.explode(player, player.getX(), player.getY() + 1.0, player.getZ(), 3.0F, Level.ExplosionInteraction.NONE);
        if (player.isCreative() || player.isSpectator()) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.creative").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        player.hurt(ModDamageTypes.source(level, ModDamageTypes.MIND_STONE), 1.0E6F);
    }

    private static void lightning(ServerLevel level, ServerPlayer player) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    // ---- the Tesseract ----

    /**
     * Whether this use of the Tesseract is the call to the Mad Titan and the lane that answers it is built: the
     * Scepter in the other hand, and a registered {@link Hold}. Then the summoning is the whole answer, whether it
     * begins or refuses ({@link #useTesseract}); the jump, Loki's escape and the awakening of the Scarlet Witch, which
     * are the Tesseract's own, must not follow a refusal.
     */
    public static boolean callsThanos(ServerPlayer player, InteractionHand hand) {
        return thanos() != null && tesseract(Powers.power(player), player.getItemInHand(other(hand))) == Ritual.THANOS;
    }

    /**
     * 3.6: a Sneak + use of the Tesseract is the Space Stone's Realm Picker (the stones answer, lane L2), unless the
     * Scepter is in the other hand (the call to the Mad Titan).
     *
     * @return whether the stone took the use (then the Tesseract does nothing else); false while nothing answers (the
     * skeleton: the Tesseract does what it always did)
     */
    public static boolean useTesseractRealms(ServerPlayer player, InteractionHand hand) {
        if (tesseract(Powers.power(player), player.getItemInHand(other(hand)),
                player.isShiftKeyDown()) != Ritual.REALMS) {
            return false;
        }
        return StoneUse.use(player, StoneKind.SPACE, StoneUse.Source.VESSEL, true, true);
    }

    /**
     * The Tesseract is used for the call to the Mad Titan ({@link #callsThanos}). Only that hold is decided here;
     * everything else the Tesseract does (the jump, Loki's escape, the Scarlet Witch's awakening) is still its own.
     *
     * @return true if a hold began (the caller starts using the Tesseract); false if it did not, or if this is not the
     * call to Thanos
     */
    public static boolean useTesseract(ServerPlayer player, InteractionHand hand) {
        if (!callsThanos(player, hand)) {
            return false;
        }
        return thanos().begin(player, hand);
    }

    // ---- the hold: what the two items delegate ----

    /**
     * 3.6: whether the item held is the Mind Beam's (the {@code MIND_BEAM} row), not the call to the Mad Titan: the
     * stone runs that hold itself ({@code StoneUse}), so none of these goes to the summoning.
     */
    public static boolean mindBeamHold(LivingEntity user) {
        return StoneUse.holding(user, StoneKind.MIND);
    }

    public static void holdTick(LivingEntity user, ItemStack stack, int remaining) {
        Hold hold = thanos();
        if (hold != null && user instanceof ServerPlayer player && !mindBeamHold(player)) {
            hold.tick(player, stack, remaining);
        }
    }

    public static void holdReleased(LivingEntity user, ItemStack stack) {
        Hold hold = thanos();
        if (hold != null && user instanceof ServerPlayer player && !mindBeamHold(player)) {
            hold.release(player, stack);
        }
    }

    public static void holdFinished(LivingEntity user, ItemStack stack) {
        Hold hold = thanos();
        if (hold != null && user instanceof ServerPlayer player && !mindBeamHold(player)) {
            hold.finish(player, stack);
        }
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private ScepterRituals() {
    }
}

package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.StoneUse;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * What the Eye of Agamotto does, by who uses it (3.6, the plan's section 5): the item is {@code EyeOfAgamottoItem}.
 * <ul>
 * <li>A powerless player who holds it {@code awakenTicks} becomes Doctor Strange: the Eye opens, green rings spin round
 * him, and it is bound to him ({@code OWNER}); it is not used up.</li>
 * <li>Doctor Strange (or a custom hero with his Eye's row): Slow Time; Sneak, Rewind ({@link EyeTime}, as his Y).</li>
 * <li>Any other sorcerer (Billy, Agatha, Loki, the Scarlet Witch, Odin, a custom sorcerer): the Time Stone's use through
 * its vessel ({@code StoneUse.use(TIME, VESSEL)}, the stones' lane), or, while that lane has not taken it, the Eye's own
 * Slow Time and Rewind ({@link EyeTime}).</li>
 * <li>Anyone else, the gods who are not sorcerers and the Phoenix: "The Eye stays closed."</li>
 * </ul>
 * An owned Eye awakens nobody else while its owner is Doctor Strange ({@code eye_of_agamotto.ownerOnly}); when he loses
 * the power it is unowned again ({@link #release}), and an unowned Eye in Doctor Strange's inventory becomes his (the one
 * "Choose a hero" gives him).
 */
public final class EyeOfAgamotto {
    /** The green of its rings. */
    public static final int GREEN = StrangeAbilities.TIME_GREEN;

    public static int awakenTicks() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.EYE_AWAKEN_TICKS.get(), 60);
    }

    static boolean ownerOnly() {
        return StrangeAbilities.cfgBool(() -> StrangeConfig.EYE_OWNER_ONLY.get(), true);
    }

    static boolean nonSorcererMessage() {
        return StrangeAbilities.cfgBool(() -> StrangeConfig.EYE_NON_SORCERER_MESSAGE.get(), true);
    }

    // ---- who it answers ----

    /** What the Eye is to a player. */
    public enum Use {
        /** He has no power: holding it makes him Doctor Strange. */
        AWAKEN,
        /** He has no power, but the Eye is another Doctor Strange's. */
        TAKEN,
        /** Doctor Strange, or a custom hero with the Eye's row: his Y's powers. */
        STRANGE,
        /** A sorcerer: the Time Stone's powers. */
        SORCERER,
        /** A sorcerer the Hex Runes silence. */
        SILENCED,
        /** Anyone else: it stays closed. */
        CLOSED
    }

    /** What the Eye {@code stack} is to {@code player}. */
    public static Use use(Player player, ItemStack stack) {
        PowerData data = Powers.get(player);
        if (!data.power().hasPowers()) {
            return takenBy(player, stack) != null ? Use.TAKEN : Use.AWAKEN;
        }
        if (data.has(HeroAbility.STRANGE_EYE)) {
            return HexRunes.silences(player) ? Use.SILENCED : Use.STRANGE;
        }
        if (Traits.has(player, Trait.SORCERER)) {
            return HexRunes.silences(player) ? Use.SILENCED : Use.SORCERER;
        }
        return Use.CLOSED;
    }

    // ---- the owner ----

    /** The Eye's owner, or null. */
    @Nullable
    public static MjolnirOwner owner(ItemStack stack) {
        return stack.get(ModDataComponents.OWNER.get());
    }

    /**
     * The Doctor Strange this Eye is bound to, if he is not {@code player}, is on the server and is still Doctor Strange
     * (and {@code ownerOnly} is on); else null.
     */
    @Nullable
    static ServerPlayer takenBy(Player player, ItemStack stack) {
        MjolnirOwner owner = owner(stack);
        if (owner == null || owner.is(player) || !ownerOnly() || player.getServer() == null) {
            return null;
        }
        ServerPlayer holder = player.getServer().getPlayerList().getPlayer(owner.id());
        return holder != null && Powers.get(holder).power() == Power.DOCTOR_STRANGE ? holder : null;
    }

    /** Binds the Eye to {@code player}. */
    public static void bind(ItemStack stack, Player player) {
        stack.set(ModDataComponents.OWNER.get(), MjolnirOwner.of(player));
    }

    /**
     * Every tick of an Eye in an inventory (server): an unowned Eye in Doctor Strange's inventory becomes his; an Eye
     * whose owner is here but no longer Doctor Strange is unowned again.
     */
    public static void inventoryTick(ItemStack stack, ServerPlayer holder) {
        if (holder.tickCount % 20 != 0) {
            return;
        }
        MjolnirOwner owner = owner(stack);
        boolean strange = Powers.get(holder).power() == Power.DOCTOR_STRANGE;
        if (owner == null) {
            if (strange) {
                bind(stack, holder);
            }
            return;
        }
        ServerPlayer bound = holder.server.getPlayerList().getPlayer(owner.id());
        if (bound != null && Powers.get(bound).power() != Power.DOCTOR_STRANGE) {
            stack.remove(ModDataComponents.OWNER.get());
            if (strange && bound != holder) {
                bind(stack, holder);
            }
        }
    }

    /** Doctor Strange loses the power: every Eye bound to him that he carries is unowned again. */
    static void release(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            unbind(stack, player);
        }
        unbind(player.getOffhandItem(), player);
    }

    private static void unbind(ItemStack stack, Player player) {
        MjolnirOwner owner = owner(stack);
        if (stack.is(EyeTime.eye()) && owner != null && owner.is(player)) {
            stack.remove(ModDataComponents.OWNER.get());
        }
    }

    // ---- the uses ----

    /**
     * A use of the Eye that is not the awakening's hold (the item's {@code use}, server): Doctor Strange's time, a
     * sorcerer's, or a refusal. Returns whether it was taken (a refusal is taken too: it said why).
     */
    public static boolean use(ServerPlayer player, ItemStack stack, boolean alt) {
        PowerData data = Powers.get(player);
        switch (use(player, stack)) {
            case STRANGE -> {
                if (alt) {
                    EyeTime.rewind(player, data, HeroAbility.STRANGE_EYE, true);
                } else {
                    EyeTime.toggleSlow(player, data, HeroAbility.STRANGE_EYE, true);
                }
            }
            case SORCERER -> {
                if (!StoneUse.use(player, StoneKind.TIME, StoneUse.Source.VESSEL, alt, true)) {
                    if (alt) {
                        EyeTime.rewind(player, data, null, true);
                    } else {
                        EyeTime.toggleSlow(player, data, null, true);
                    }
                }
            }
            case SILENCED -> StrangeAbilities.say(player, "message.enhanced.eye.runes");
            case TAKEN -> StrangeAbilities.say(player, "message.enhanced.eye.taken");
            case CLOSED -> {
                if (nonSorcererMessage()) {
                    player.displayClientMessage(Component.translatable("message.enhanced.eye.closed")
                            .withStyle(ChatFormatting.GRAY), true);
                }
            }
            case AWAKEN -> {
                return false;
            }
        }
        return true;
    }

    /** A tick of the awakening's hold: green rings gather round him, faster as it nears its end. */
    public static void holdTick(ServerPlayer player, int held) {
        ServerLevel level = player.serverLevel();
        if (held % 2 != 0) {
            return;
        }
        float t = Math.min(1.0F, held / (float) awakenTicks());
        Vec3 c = player.position().add(0, 1.0, 0);
        double r = 1.4 - 0.6 * t;
        for (int i = 0; i < 2; i++) {
            double a = held * (0.25 + 0.25 * t) + i * Math.PI;
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.21F, 0.83F, 0.42F), 0.8F);
            level.sendParticles(dust, c.x + Math.cos(a) * r, c.y + 0.4 * Math.sin(a * 0.5), c.z + Math.sin(a) * r, 1,
                    0, 0, 0, 0);
        }
        if (held == 2) {
            level.playSound(null, c.x, c.y, c.z, ModSounds.EYE_OPEN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    /** The hold is complete: a powerless player becomes Doctor Strange, and the Eye is his. */
    public static void awaken(ServerPlayer player, ItemStack stack) {
        if (Powers.get(player).power().hasPowers() || takenBy(player, stack) != null) {
            return;
        }
        Powers.awaken(player, Power.DOCTOR_STRANGE);
        bind(stack, player);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.STRANGE_AWAKEN.get(),
                SoundSource.PLAYERS, 1.4F, 1.0F);
        Vec3 c = player.position().add(0, 1.0, 0);
        StrangeSignals.near(level, c, com.kolodkingal.enhanced.network.StrangeFxPayload.DOME, player,
                java.util.List.of(c, new Vec3(18, 0, 0)), -StrangeSignals.AWAKEN_TICKS);
    }

    private EyeOfAgamotto() {
    }
}

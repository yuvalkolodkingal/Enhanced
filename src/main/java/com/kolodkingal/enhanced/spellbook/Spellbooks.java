package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The server side of the spellbooks ({@link Spellbook}): a player with no power uses a book and becomes its caster; the
 * caster uses it again to give its magic up (Sneak + use; the Ethereal witch gives it up from her hat's screen, since her
 * plain use is the hat); a hero of the mod, or a hero of the player's own making, is refused and keeps his power. The
 * Ethereal witch's hat comes off and goes back on with each use, and its colour is chosen on the screen Sneak + use opens
 * ({@code SpellbookHatScreen}, which sends {@link SpellbookHatPayload}); the hat is a layer of her definition, so it is
 * synced to every player who sees her with the rest of it.
 *
 * <p>A player's own custom hero (made with the Hero Creator) is kept aside while he is a caster ({@link SpellbookData}) and
 * given back when he gives the book up.
 */
public final class Spellbooks {
    /** The colours of the hat's screen: Agatha's dark violet first, then eleven more. */
    public static final List<Integer> HAT_COLORS = List.of(SpellbookData.DEFAULT_HAT, 0x1E1B22, 0x7B2FBF, 0xC86BFF, 0xB8338F,
            0x9E1B32, 0xD9772B, 0xD9B23A, 0x2F7A3A, 0x1F7A7A, 0x2C4FA8, 0xE8E4F0);

    private Spellbooks() {
    }

    /** The book's use, on both sides (the client opens the hat's screen; the server does the rest). */
    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, Spellbook book) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            use(server, book, server.isShiftKeyDown());
        } else if (level.isClientSide() && player.isShiftKeyDown() && book == Spellbook.ETHEREAL && Spellbook.of(player) == book) {
            // Client only: this branch never runs on a dedicated server, so the screen class is never loaded there.
            com.kolodkingal.enhanced.client.screen.SpellbookHatScreen.open();
        } else if (level.isClientSide() && opensSpells(player, book, player.isShiftKeyDown())) {
            // (3.18.1) the Book of Spells: the caster's plain use (the Ethereal witch's is her hat: her hat's screen has
            // the button), or a witch of the Demiurge's gift using any book. Client only, as above.
            com.kolodkingal.enhanced.client.screen.SpellsScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 3.18.1: whether this use opens the Book of Spells on the client: the book's own caster's plain use (not the Ethereal
     * witch's: hers is the hat), or any use of a book by a witch of the Demiurge's gift.
     */
    public static boolean opensSpells(Player player, Spellbook book, boolean sneak) {
        Spellbook current = Spellbook.of(player);
        if (current == null) {
            return Powers.power(player) == Power.CUSTOM && WitchLevelView.gift();
        }
        return current == book && book != Spellbook.ETHEREAL && !sneak;
    }

    /** What using the book does on the server (with Sneak or not). */
    public static void use(ServerPlayer player, Spellbook book, boolean sneak) {
        Spellbook current = Spellbook.of(player);
        if (current == book) {
            if (book == Spellbook.ETHEREAL) {
                if (!sneak) {
                    toggleHat(player);
                }
                // (Sneak: the client has opened the hat's screen)
            } else if (sneak) {
                renounce(player);
            } else {
                // (3.18.1: the client has opened the Book of Spells; it is told her level afresh)
                WitchLevels.sync(player);
            }
            return;
        }
        WitchLevels.Source source = current == null ? WitchLevels.source(player) : null;
        if (source != null && source.kind() == WitchLevels.Kind.GIFT) {
            WitchLevels.sync(player); // (a witch of the Demiurge's gift: the client has opened her Book of Spells)
            return;
        }
        if (current == null && Powers.power(player) != Power.NONE) {
            // A hero keeps his own power: the book stays shut.
            player.displayClientMessage(Component.translatable("message.enhanced.spellbook.has_power", book.bookName())
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        become(player, book);
    }

    /**
     * Makes the player the book's caster: from no power, or from another book's caster. His own custom hero, if he made
     * one, is kept aside.
     */
    public static void become(ServerPlayer player, Spellbook book) {
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        CustomHero own = ModAttachments.CUSTOM_HERO.get(player);
        if (Spellbook.of(own) == null) {
            data = data.withStash(own); // (a book's definition left from before is never kept aside: his own already is)
        }
        // (3.18.1) a book begun now starts at level 1; a book she comes back to, at her level in it
        data = WitchLevels.begin(data, book.id);
        ModAttachments.SPELLBOOK.set(player, data);
        CustomHeroes.become(player, book.definition(WitchLevels.level(data, book.id), data.hatColor(), true), false);
        announce(player, book);
        WitchLevels.sync(player);
    }

    /** The caster gives the book's magic up: no power, and his own custom hero back. False if he is no caster. */
    public static boolean renounce(ServerPlayer player) {
        Spellbook book = Spellbook.of(player);
        if (book == null) {
            return false;
        }
        Powers.setPower(player, Power.NONE);
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        ModAttachments.CUSTOM_HERO.set(player, data.stash());
        ModAttachments.CUSTOM_HERO.sync(player);
        ModAttachments.SPELLBOOK.set(player, data.withStash(CustomHero.EMPTY));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS,
                1.0F, 0.7F);
        say(player, "message.enhanced.spellbook.renounced", book.bookName());
        WitchLevels.sync(player);
        return true;
    }

    // ---- the Ethereal witch's hat ----

    /** Whether the Ethereal witch wears her hat now. */
    public static boolean wearsHat(Player player) {
        return Spellbook.of(player) == Spellbook.ETHEREAL && "witch_hat".equals(CustomHeroes.hero(player).layers().hat().style());
    }

    /** The hat comes off, or goes back on. */
    public static void toggleHat(ServerPlayer player) {
        if (Spellbook.of(player) != Spellbook.ETHEREAL) {
            return;
        }
        boolean on = !wearsHat(player);
        setHat(player, on, ModAttachments.SPELLBOOK.get(player).hatColor());
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                SoundSource.PLAYERS, 1.0F, on ? 1.0F : 0.8F);
        say(player, on ? "message.enhanced.spellbook.hat_on" : "message.enhanced.spellbook.hat_off");
    }

    /** The hat in a new colour (it goes on if it was off). */
    public static void chooseHatColor(ServerPlayer player, int color) {
        if (Spellbook.of(player) != Spellbook.ETHEREAL) {
            return;
        }
        int rgb = color & 0xFFFFFF;
        ModAttachments.SPELLBOOK.set(player, ModAttachments.SPELLBOOK.get(player).withHatColor(rgb));
        setHat(player, true, rgb);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DYE_USE, SoundSource.PLAYERS,
                1.0F, 1.0F);
    }

    private static void setHat(ServerPlayer player, boolean on, int color) {
        // (3.18.1: at her level; the hat changes nothing else)
        CustomHero hero = Spellbook.ETHEREAL.definition(WitchLevels.level(ModAttachments.SPELLBOOK.get(player), Spellbook.ETHEREAL.id),
                color, on);
        ModAttachments.CUSTOM_HERO.set(player, hero);
        ModAttachments.CUSTOM_HERO.sync(player);
        Powers.syncIfDirty(player);
    }

    // ---- words ----

    private static void say(ServerPlayer player, String key, Object... args) {
        Spellbook book = Spellbook.of(player);
        int color = book == null ? 0xE0E0E0 : book.glow;
        player.displayClientMessage(Component.translatable(key, args).withStyle(s -> s.withColor(color)), true);
    }

    /** The awakening of a caster: the title, the book's sound, a burst in its colours and a word to the server. */
    private static void announce(ServerPlayer player, Spellbook book) {
        ServerLevel level = player.serverLevel();
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(book.title().copy().withStyle(s -> s.withColor(book.color).withBold(true))));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("spellbook.enhanced." + book.id + ".awaken")
                .withStyle(s -> s.withColor(book.glow))));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CUSTOM_AWAKEN.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.2F, 0.8F);
        Powers.burst(level, player.getX(), player.getY() + 1.0, player.getZ(), book.color, 120, 1.6);
        Powers.burst(level, player.getX(), player.getY() + 1.0, player.getZ(), book.glow, 60, 0.8);
        player.server.getPlayerList().broadcastSystemMessage(Component.translatable("spellbook.enhanced.broadcast",
                player.getDisplayName(), book.title()).withStyle(s -> s.withColor(book.color)), false);
    }

    /** The colour of a swatch of the hat's screen, or null if {@code index} is none of them. */
    @Nullable
    public static Integer hatColor(int index) {
        return index >= 0 && index < HAT_COLORS.size() ? HAT_COLORS.get(index) : null;
    }
}

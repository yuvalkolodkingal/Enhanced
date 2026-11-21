package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Demiurge;
import com.kolodkingal.enhanced.config.DemiurgeConfig;
import com.kolodkingal.enhanced.custom.Catalog;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Resource;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.spellbook.Spellbook;
import com.kolodkingal.enhanced.spellbook.SpellbookData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Demiurge's gift of magic (3.18.1), as the One Above All gives power: Billy the Demiurge gives any player who has no hero
 * now ({@link Power#NONE}) the magic he picks from the Hero Creator's catalogue, magic only ({@link #CATALOG}: the generic
 * magic of the spellbooks, flight, and every ability of the witches and sorcerers: the Scarlet Witch, Billy himself, Agatha,
 * Doctor Strange and Loki). The player becomes a custom hero of those powers, in his own skin, with no point budget
 * ({@code billy.demiurge.giftStrength} each), as a spellbook makes a caster: a hero of his own making is kept aside
 * ({@link SpellbookData#stash}) and given back when the Demiurge takes the gift back.
 *
 * <p>The Demiurge's screen (Sneak + G) sends {@code /demiurge give <player> <picks>}, the picks as indexes into
 * {@link #CATALOG} (the same list on both sides), so the gift needs no message of its own; an operator may give it with
 * {@code /enhanced demiurge gift} (Ultimate is still needed there).
 */
public final class DemiurgeGifts {
    /** The name of a gifted player's definition (what tells a gift from any other custom hero). */
    public static final String NAME = "Gift of the Demiurge";
    /** The witches and sorcerers whose abilities may be given, in the screen's order. */
    public static final List<Power> SORCERERS = List.of(Power.SCARLET_WITCH, Power.BILLY, Power.AGATHA, Power.DOCTOR_STRANGE, Power.LOKI);

    /** One power that may be given: a catalogue entry and the option it is given in. */
    public record Gift(String id, String option) {
        @Nullable
        public CatalogEntry entry() {
            return Catalog.byId(id);
        }

        /** Whether it goes on a key (a body takes none). */
        public boolean takesKey() {
            CatalogEntry e = entry();
            return e != null && e.act(option) != PowerModule.Act.NONE;
        }

        /** The hero whose ability it is, or null for the generic magic. */
        @Nullable
        public Power hero() {
            CatalogEntry e = entry();
            return e == null ? null : e.hero();
        }

        /** Its name in the player's language ("Mystic Bolts", "Chaos Beam"). */
        public Component name() {
            CatalogEntry e = entry();
            if (e instanceof PowerModule module) {
                return Component.translatable(module.optionKey(option));
            }
            return e == null ? Component.literal(id) : Component.translatable(e.nameKey());
        }
    }

    /** Every power the Demiurge may give, in a fixed order (the screen sends indexes into it). */
    public static final List<Gift> CATALOG = build();

    private static List<Gift> build() {
        List<Gift> list = new ArrayList<>();
        for (String option : List.of("bolts", "orb", "ward", "curse")) {
            list.add(new Gift(PowerModule.MAGIC.id(), option));
        }
        list.add(new Gift(PowerModule.EARTH.id(), "throw"));
        list.add(new Gift(PowerModule.EARTH.id(), "wall"));
        list.add(new Gift(PowerModule.NATURE.id(), "bloom"));
        list.add(new Gift(PowerModule.FLIGHT.id(), "hover"));
        for (Power hero : SORCERERS) {
            for (HeroAbility a : HeroAbility.values()) {
                if (a.hero == hero && magic(a)) {
                    list.add(new Gift(a.id(), HeroAbility.BOTH));
                }
            }
        }
        return Collections.unmodifiableList(list);
    }

    /** A witch's or sorcerer's ability that may be given: not on the Y key, not Build Magic, not the stone bearer's nature. */
    private static boolean magic(HeroAbility a) {
        return a.home != com.kolodkingal.enhanced.custom.HeroKeys.Y && !a.resources(HeroAbility.BOTH).contains(Resource.BUILD)
                && a != HeroAbility.WANDA_STONE_BEARER;
    }

    private DemiurgeGifts() {
    }

    // ---- the gift ----

    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || DemiurgeConfig.GIFTS.get();
    }

    private static int strength() {
        return EnhancedConfig.SPEC.isLoaded() ? DemiurgeConfig.GIFT_STRENGTH.get() : 8;
    }

    private static int maxPowers() {
        return EnhancedConfig.SPEC.isLoaded() ? DemiurgeConfig.GIFT_MAX_POWERS.get() : 10;
    }

    /** The limits a gift is judged by: every power allowed, no point budget. */
    private static CustomHeroRules.Limits limits() {
        return new CustomHeroRules.Limits(10_000, Math.max(20, maxPowers()), PowerModule.MAX_STRENGTH, 16384, Set.of());
    }

    /** Whether this definition is a Demiurge's gift. */
    public static boolean isGift(CustomHero hero) {
        return NAME.equals(hero.name());
    }

    /** Whether this player holds a Demiurge's gift now. */
    public static boolean gifted(net.minecraft.world.entity.player.Player player) {
        return Powers.power(player) == Power.CUSTOM && isGift(CustomHeroes.hero(player));
    }

    /** The picks of {@code "3,7,12"} (indexes into {@link #CATALOG}; anything else is left out), in order, once each. */
    public static List<Integer> parse(String text) {
        Set<Integer> picks = new LinkedHashSet<>();
        for (String part : text.split("[,;]")) {
            try {
                int i = Integer.parseInt(part.trim());
                if (i >= 0 && i < CATALOG.size()) {
                    picks.add(i);
                }
            } catch (NumberFormatException ignored) {
                // (left out)
            }
        }
        return new ArrayList<>(picks);
    }

    /** {@code picks} as the text the screen sends. */
    public static String format(List<Integer> picks) {
        StringBuilder sb = new StringBuilder();
        for (int i : picks) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(i);
        }
        return sb.toString();
    }

    /**
     * Why {@code giver} may not give magic to {@code target} now (a lang key), or null if he may. {@code giver} null: an
     * operator's command (only Ultimate and the target are checked).
     */
    @Nullable
    public static String refusal(@Nullable ServerPlayer giver, ServerPlayer target) {
        if (!enabled()) {
            return "message.enhanced.demiurge.gift.disabled";
        }
        if (giver != null) {
            if (!Ultimate.allowed(giver) || !Demiurge.enabled()) {
                return "message.enhanced.demiurge.gift.no_ultimate";
            }
            if (!Demiurge.is(giver)) {
                return "message.enhanced.demiurge.gift.not_demiurge";
            }
            if (giver == target) {
                return "message.enhanced.demiurge.gift.self";
            }
            double range = EnhancedConfig.SPEC.isLoaded() ? DemiurgeConfig.GIFT_RANGE.get() : 0.0;
            if (giver.level() != target.level() || range > 0.0 && giver.distanceTo(target) > range) {
                return "message.enhanced.demiurge.gift.too_far";
            }
        } else if (!Ultimate.on() && !Ultimate.granted(target)) {
            return "message.enhanced.demiurge.gift.no_ultimate";
        }
        if (Powers.power(target) != Power.NONE) {
            return "message.enhanced.demiurge.gift.has_hero";
        }
        return null;
    }

    /**
     * The definition of a gift of these picks: magic only, in the Demiurge's blue, his own skin, the powers that take a key on
     * R G V B N and then on Sneak (first beside a power with no Sneak variant of its own), one option of a generic power each.
     */
    public static CustomHero definition(List<Integer> picks) {
        List<CustomHero.Pick> powers = new ArrayList<>();
        List<String> keyed = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        int strength = strength();
        for (int i : picks) {
            if (i < 0 || i >= CATALOG.size() || powers.size() >= maxPowers()) {
                continue;
            }
            Gift gift = CATALOG.get(i);
            if (gift.entry() == null || !ids.add(gift.id())) {
                continue; // (one option of a generic power)
            }
            powers.add(new CustomHero.Pick(gift.id(), strength, gift.option(), -1));
            if (gift.takesKey()) {
                keyed.add(gift.id());
            }
        }
        String[] keys = {"", "", "", "", ""};
        String[] sneak = {"", "", "", "", ""};
        int k = 0;
        List<String> rest = new ArrayList<>();
        for (String id : keyed) {
            if (k < CustomHero.KEYS) {
                keys[k++] = id;
            } else {
                rest.add(id);
            }
        }
        // the rest on Sneak: first beside a power with no Sneak variant of its own, then anywhere left
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int c = 0; c < CustomHero.KEYS && !rest.isEmpty(); c++) {
                if (!sneak[c].isEmpty()) {
                    continue;
                }
                CatalogEntry on = Catalog.byId(keys[c]);
                boolean hasAlt = on instanceof HeroAbility a && a.hasAlt();
                if (pass == 0 && hasAlt) {
                    continue;
                }
                sneak[c] = rest.remove(0);
            }
        }
        CustomHero hero = CustomHero.EMPTY.withName(NAME).withColors(Demiurge.BLUE, Demiurge.STAR)
                .withLook(CustomHero.LOOK_OWN, false).withPowers(powers)
                .withKeys(List.of(keys)).withSneakKeys(List.of(sneak));
        return CustomHeroRules.sanitize(hero, limits()).hero();
    }

    /**
     * Gives the magic of {@code picks} to {@code target} (from the Demiurge {@code giver}, or an operator: null). Returns null
     * when it was given, else why not (a lang key; the giver is told).
     */
    @Nullable
    public static String give(@Nullable ServerPlayer giver, ServerPlayer target, List<Integer> picks) {
        String refused = refusal(giver, target);
        if (refused == null && picks.isEmpty()) {
            refused = "message.enhanced.demiurge.gift.empty";
        }
        CustomHero hero = refused == null ? definition(picks) : null;
        if (refused == null && hero.powers().isEmpty()) {
            refused = "message.enhanced.demiurge.gift.empty";
        }
        if (refused != null) {
            if (giver != null) {
                giver.displayClientMessage(Component.translatable(refused, target.getDisplayName())
                        .withStyle(s -> s.withColor(0xFF8080)), true);
            }
            return refused;
        }
        // (3.18.1) a witch of his gift grows by the witch levels: she starts at level 1 and her gifted powers open level by
        // level (level 5 is the whole gift); a hero of her own making is kept aside, as a spellbook does
        com.kolodkingal.enhanced.spellbook.WitchLevels.startGift(target, hero);
        announce(giver, target, hero);
        return null;
    }

    /** The Demiurge (or an operator) takes a gift back: no power, and the player's own custom hero back. */
    public static boolean takeBack(@Nullable ServerPlayer giver, ServerPlayer target) {
        if (giver != null && (!Demiurge.is(giver) || !Ultimate.allowed(giver))) {
            giver.displayClientMessage(Component.translatable(Ultimate.allowed(giver) ? "message.enhanced.demiurge.gift.not_demiurge"
                    : "message.enhanced.demiurge.gift.no_ultimate").withStyle(s -> s.withColor(0xFF8080)), true);
            return false;
        }
        if (!gifted(target)) {
            if (giver != null) {
                giver.displayClientMessage(Component.translatable("message.enhanced.demiurge.gift.not_gifted", target.getDisplayName())
                        .withStyle(s -> s.withColor(0xFF8080)), true);
            }
            return false;
        }
        Powers.setPower(target, Power.NONE);
        SpellbookData book = ModAttachments.SPELLBOOK.get(target);
        ModAttachments.CUSTOM_HERO.set(target, book.stash());
        ModAttachments.CUSTOM_HERO.sync(target);
        ModAttachments.SPELLBOOK.set(target, book.withStash(CustomHero.EMPTY).withGift(CustomHero.EMPTY));
        target.displayClientMessage(Component.translatable("message.enhanced.demiurge.gift.taken")
                .withStyle(s -> s.withColor(Demiurge.AURA)), true);
        if (giver != null) {
            giver.displayClientMessage(Component.translatable("message.enhanced.demiurge.gift.took", target.getDisplayName())
                    .withStyle(s -> s.withColor(Demiurge.AURA)), true);
        }
        return true;
    }

    private static void announce(@Nullable ServerPlayer giver, ServerPlayer target, CustomHero hero) {
        ServerLevel level = target.serverLevel();
        target.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        target.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.enhanced.demiurge.gift.title")
                .withStyle(s -> s.withColor(Demiurge.AURA).withBold(true))));
        target.connection.send(new ClientboundSetSubtitleTextPacket(giver == null
                ? Component.translatable("message.enhanced.demiurge.gift.subtitle_anon").withStyle(s -> s.withColor(Demiurge.STAR))
                : Component.translatable("message.enhanced.demiurge.gift.subtitle", giver.getDisplayName())
                .withStyle(s -> s.withColor(Demiurge.STAR))));
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.CUSTOM_AWAKEN.get(), SoundSource.PLAYERS, 1.2F, 1.2F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.5F);
        Demiurge.burst(level, target, 90);
        if (giver != null) {
            giver.displayClientMessage(Component.translatable("message.enhanced.demiurge.gift.given", target.getDisplayName(),
                    hero.powers().size()).withStyle(s -> s.withColor(Demiurge.AURA)), true);
        }
    }
}

package com.kolodkingal.enhanced.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the boss fights keep between fights, saved with the world: the rewards that could not be handed over yet (the
 * fighter was offline, or dead) and when each boss may be called again. A level attachment kept on the Overworld only,
 * so a gift is found from any dimension (see {@code ModAttachments.BOSS_DATA}). Nothing about a fight that is running
 * is saved: no fight survives a restart.
 *
 * <p>The record never changes: every {@code with...} method returns a new one, which {@link BossGifts} and {@link
 * BossFights} store back on the Overworld (which also marks it for saving).
 *
 * @param gifts the stacks waiting for each player, by their UUID
 * @param rest  the game time at which each boss ({@link BossKind#id}) may be called again
 */
public record BossData(Map<UUID, List<ItemStack>> gifts, Map<String, Long> rest) {
    public static final BossData EMPTY = new BossData(Map.of(), Map.of());

    public static final Codec<BossData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, ItemStack.CODEC.listOf()).optionalFieldOf("gifts", Map.of()).forGetter(BossData::gifts),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("rest", Map.of()).forGetter(BossData::rest)
    ).apply(i, BossData::new));

    /** The gifts of {@code who} (an empty list if there are none). */
    public List<ItemStack> giftsOf(UUID who) {
        return gifts.getOrDefault(who, List.of());
    }

    /** These data with one more stack waiting for {@code who}. */
    public BossData withGift(UUID who, ItemStack stack) {
        Map<UUID, List<ItemStack>> next = new HashMap<>(gifts);
        List<ItemStack> list = new ArrayList<>(giftsOf(who));
        list.add(stack.copy());
        next.put(who, List.copyOf(list));
        return new BossData(Map.copyOf(next), rest);
    }

    /** These data without the gifts of {@code who}. */
    public BossData withoutGifts(UUID who) {
        if (!gifts.containsKey(who)) {
            return this;
        }
        Map<UUID, List<ItemStack>> next = new HashMap<>(gifts);
        next.remove(who);
        return new BossData(Map.copyOf(next), rest);
    }

    /** The game time before which {@code kind} may not be called again (0 if it is not resting). */
    public long restUntil(BossKind kind) {
        return rest.getOrDefault(kind.id(), 0L);
    }

    /** These data with {@code kind} resting until the game time {@code until}. */
    public BossData withRest(BossKind kind, long until) {
        Map<String, Long> next = new HashMap<>(rest);
        next.put(kind.id(), until);
        return new BossData(gifts, Map.copyOf(next));
    }
}

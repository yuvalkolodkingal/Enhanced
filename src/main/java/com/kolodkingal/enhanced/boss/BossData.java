package com.kolodkingal.enhanced.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the boss fights keep between fights, saved with the world: the rewards that could not be handed over yet (the
 * fighter was offline, or their inventory was full) and when each boss may be called again. A level attachment kept
 * on the Overworld only, so a gift is found from any dimension (see {@code ModAttachments.BOSS_DATA}). Nothing about a
 * fight that is running is saved: no fight survives a restart.
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
}

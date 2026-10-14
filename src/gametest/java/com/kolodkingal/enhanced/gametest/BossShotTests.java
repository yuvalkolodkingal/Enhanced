package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.Metal;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StonePowers;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What a boss fight throws, in the hands of telekinesis (3.1, D-T 6.2.1 and 10): which of the bosses and their shots the
 * tags leave to telekinesis and which the villains' own pulls pass by, and how long a grip holds Thanos's sword.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BossShotTests {

    /**
     * T23 (D-T 10, R37): the three villains and Thanos's sword, meteor and orb left {@code telekinesis_immune} for {@code
     * pull_immune}, the tag the villain heroes' pulls read; the Wither, the Warden and the Elder Guardian are held as bosses
     * and the pulls still pass them by; what no telekinesis lifts (the Ender Dragon, a fragment of Mjolnir, the Hex's
     * Vision, Apocalypse's cocoon, the Vibranium Key) is in both. A Thanos stone's grasp still refuses every boss.
     */
    @GameTest(template = ARENA, batch = "boss_shot_tags")
    public static void bossTagsAreSplit(GameTestHelper helper) {
        for (EntityType<?> type : List.of(ModEntities.ULTRON.get(), ModEntities.THANOS.get(), ModEntities.APOCALYPSE.get(),
                ModEntities.THANOS_SWORD.get(), ModEntities.THANOS_METEOR.get(), ModEntities.THANOS_ORB.get())) {
            check(type.is(ModTags.PULL_IMMUNE), EntityType.getKey(type) + " is in pull_immune: the villains' pulls pass it by");
            check(!type.is(ModTags.TELEKINESIS_IMMUNE), EntityType.getKey(type) + " is not in telekinesis_immune any more");
            check(!type.is(ModTags.HELD_AS_BOSSES), EntityType.getKey(type) + " is not a vanilla boss");
        }
        for (EntityType<?> type : List.of(EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN)) {
            check(type.is(ModTags.HELD_AS_BOSSES), EntityType.getKey(type) + " is held as a boss");
            check(type.is(ModTags.PULL_IMMUNE), EntityType.getKey(type) + " is in pull_immune, as it was in 3.0");
            check(!type.is(ModTags.TELEKINESIS_IMMUNE), EntityType.getKey(type) + " is not in telekinesis_immune any more");
        }
        for (EntityType<?> type : List.of(EntityType.ENDER_DRAGON, ModEntities.MJOLNIR_FRAGMENT.get(), ModEntities.HEX_VISION.get(),
                ModEntities.SAND_COCOON.get(), ModEntities.VIBRANIUM_KEY.get())) {
            check(type.is(ModTags.TELEKINESIS_IMMUNE) && type.is(ModTags.PULL_IMMUNE),
                    EntityType.getKey(type) + " is in telekinesis_immune and in pull_immune");
            check(!type.is(ModTags.HELD_AS_BOSSES), EntityType.getKey(type) + " is not held as a boss");
        }

        // The stones' grasp (the Thanos hero's Space Pull, the boss's Choke and grabs) reads pull_immune: no boss.
        ServerLevel level = helper.getLevel();
        for (EntityType<? extends LivingEntity> type : List.<EntityType<? extends LivingEntity>>of(ModEntities.ULTRON.get(),
                ModEntities.THANOS.get(), ModEntities.APOCALYPSE.get(), EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN)) {
            LivingEntity boss = type.create(level);
            check(boss != null && !StonePowers.graspable(boss), EntityType.getKey(type) + " cannot be grasped by a stone");
        }
        LivingEntity zombie = EntityType.ZOMBIE.create(level);
        check(zombie != null && StonePowers.graspable(zombie), "a zombie can");
        helper.succeed();
    }

    /**
     * R8 (D-T 4.3, D-M 3.5): Thanos's Double-Edged Sword is metal to Magneto, and a grip holds it {@code
     * thanos_boss.swordHoldTicks} (40) at most, Billy's and the Phoenix's alike (Billy's factor is for the bosses only),
     * Magneto's 1.5 times as long (60): on metal, his grip is the stronger. A shot is no boss: nothing keeps it from the
     * next grip.
     */
    @GameTest(template = ARENA, batch = "boss_shot_hold")
    public static void magnetoHoldsTheSwordLonger(GameTestHelper helper) {
        ThanosSwordEntity sword = ModEntities.THANOS_SWORD.get().create(helper.getLevel());
        check(sword != null && Metal.isMagnetic(sword), "the Double-Edged Sword is metal to Magneto");
        check(BossGrip.holdLimit(sword, Telekinesis.Mode.BILLY) == 40 && BossGrip.holdLimit(sword, Telekinesis.Mode.JEAN) == 40,
                "Billy and the Phoenix hold it 40 ticks: " + BossGrip.holdLimit(sword, Telekinesis.Mode.BILLY));
        check(BossGrip.holdLimit(sword, Telekinesis.Mode.MAGNETO) == 60, "Magneto 60: " + BossGrip.holdLimit(sword, Telekinesis.Mode.MAGNETO));
        check(!BossGrip.isBoss(sword) && BossGrip.regripLeft(sword) == 0, "a shot has no regrip of its own");
        helper.succeed();
    }

    private BossShotTests() {
    }
}

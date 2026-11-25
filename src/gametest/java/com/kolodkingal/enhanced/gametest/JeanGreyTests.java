package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.MindLink;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.ability.PhoenixMatter;
import com.kolodkingal.enhanced.ability.PhoenixShield;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * (3.19.2) Jean Grey made stronger: a bigger, stronger Phoenix Burst by default, her telekinesis orange, five times a
 * player's healing, fire and lava never hurt her, basic matter manipulation (Sneak + N) and a psionic force field
 * (Sneak + G).
 */
public final class JeanGreyTests {
    private static final String BATCH = "jean_grey";

    private JeanGreyTests() {
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herBurstIsBiggerAndStrongerByDefault(GameTestHelper helper) {
        check(PhoenixConfig.BURST_MAX_RADIUS.getDefault().equals(40) && PhoenixConfig.BURST_MIN_RADIUS.getDefault().equals(12),
                "a full Burst reaches 40 blocks (it was 24), a weak one 12 (it was 8)");
        check(PhoenixConfig.BURST_MAX_DAMAGE.getDefault().equals(60.0) && PhoenixConfig.BURST_MIN_DAMAGE.getDefault().equals(16.0),
                "a full Burst does 60 (it was 30)");
        check(Telekinesis.Mode.JEAN.color == PhoenixAbilities.TK_ORANGE, "her telekinesis is orange, not violet");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void sheHealsFiveTimesAsFastAndFireNeverHurtsHer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = PhoenixTests.jean(f, 10.5, 10.5);
        jean.setHealth(4.0F);
        jean.getFoodData().setFoodLevel(10); // too hungry for a player's own healing
        float before = jean.getHealth();
        for (int i = 1; i <= PhoenixAbilities.REGEN_PERIOD * 4 + 1; i++) {
            helper.runAfterDelay(i, () -> PhoenixAbilities.tick(jean, Powers.get(jean)));
        }
        helper.runAfterDelay(PhoenixAbilities.REGEN_PERIOD * 4 + 2, () -> {
            check(jean.getHealth() >= before + 3.0F, "she healed at least 3 in 4 of her periods (20 ticks), a player 0: " + jean.getHealth());
            check(jean.hasEffect(MobEffects.FIRE_RESISTANCE), "she carries the game's fire resistance, unseen");
            float health = jean.getHealth();
            ServerLevel level = helper.getLevel();
            jean.hurt(level.damageSources().lava(), 6.0F);
            jean.hurt(level.damageSources().inFire(), 6.0F);
            check(jean.getHealth() == health, "lava and fire do nothing to her");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herForceFieldCutsBlowsAndIsDrawn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = PhoenixTests.jean(f, 10.5, 10.5);
        PhoenixTests.sneakPress(jean, 2);
        check(PhoenixShield.isUp(jean), "Sneak + G raises her force field");
        PhoenixAbilities.tick(jean, Powers.get(jean));
        check(Powers.get(jean).is(Live.PHOENIX_SHIELD), "and the clients are told to draw it");
        check(Math.abs(PhoenixShield.filter(jean, helper.getLevel().damageSources().generic(), 10.0F) - 3.0F) < 1.0E-4,
                "a blow of 10 reaches her as 3");
        DamageSource fall = helper.getLevel().damageSources().fall();
        check(PhoenixShield.filter(jean, fall, 10.0F) == 10.0F, "a fall is not stopped by it");
        PhoenixTests.sneakPress(jean, 2);
        check(!PhoenixShield.isUp(jean), "Sneak + G again lowers it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void sheReshapesMatter(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = PhoenixTests.jean(f, 10.5, 10.5);
        BlockPos sand = helper.absolutePos(new BlockPos(13, 2, 10));
        helper.getLevel().setBlockAndUpdate(sand, Blocks.SAND.defaultBlockState());
        TestKit.lookAt(jean, Vec3.atCenterOf(sand));
        PhoenixTests.sneakPress(jean, 5);
        check(helper.getLevel().getBlockState(sand).is(Blocks.GLASS), "Sneak + N with an empty hand turns sand to glass");
        check(!PhoenixAbilities.isCharging(jean), "and it charges no Burst");

        check(PhoenixMatter.heldShape(new ItemStack(Items.DIAMOND_BLOCK)) == null && PhoenixMatter.heldShape(new ItemStack(Items.IRON_ORE)) == null
                && PhoenixMatter.heldShape(new ItemStack(Items.CHEST)) == null, "nothing precious or holding things is made from nothing");
        check(PhoenixMatter.heldShape(new ItemStack(Items.OAK_PLANKS)) != null, "but planks are");
        helper.runAfterDelay(PhoenixMatter.COOLDOWN_TICKS + 1, () -> {
            jean.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            TestKit.lookAt(jean, Vec3.atCenterOf(sand));
            PhoenixTests.sneakPress(jean, 5);
            check(helper.getLevel().getBlockState(sand).is(Blocks.OAK_PLANKS), "with planks in her hand the glass becomes planks");
            jean.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herBodyIsArmouredLikeDiamondAndHealsFaster(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = PhoenixTests.jean(f, 10.5, 10.5);
        PhoenixAbilities.tick(jean, Powers.get(jean));
        check(jean.getAttributeValue(Attributes.ARMOR) >= 20.0 && jean.getAttributeValue(Attributes.ARMOR_TOUGHNESS) >= 8.0,
                "a full set of diamond armour's protection, with nothing on: " + jean.getAttributeValue(Attributes.ARMOR));
        check(PhoenixAbilities.REGEN_PERIOD <= 5, "half a heart every 5 ticks or faster");
        Powers.setPower(jean, Power.NONE);
        PowerTicker.tick(jean);
        check(jean.getAttributeValue(Attributes.ARMOR) < 1.0, "it goes with her power");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void sheTakesABodyForAsLongAsSheLikes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = PhoenixTests.jean(f, 3.5, 10.5);
        f.onClose(() -> MindLink.end(jean.server, jean.getUUID(), MindLink.End.LET_GO));
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(14.5, FLOOR, 10.5));
        TestKit.lookAt(jean, husk);
        PhoenixTests.sneakPress(jean, 3);
        check(MindLink.isHost(husk) && MindLink.isLinked(jean), "Sneak + V: her mind is in the husk, eleven blocks away");
        for (int i = 0; i < 5; i++) {
            PowerTicker.tick(jean);
        }
        check(Powers.get(jean).is(Live.PHOENIX_SHIELD), "her own body wears her force field meanwhile");
        float health = jean.getHealth();
        jean.hurt(helper.getLevel().damageSources().generic(), 10.0F);
        check(MindLink.isLinked(jean), "a blow to her body does not bring her back, and there is no time limit");
        check(jean.getHealth() >= health - 3.5F, "the field took most of the blow: " + (health - jean.getHealth()));
        PhoenixTests.press(jean, 3);
        check(!MindLink.isLinked(jean) && !MindLink.isHost(husk), "V: she is back");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void wandaBillyAndSpiderManAreImmune(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = PhoenixTests.jean(f, 3.5, 10.5);
        f.onClose(() -> MindLink.end(jean.server, jean.getUUID(), MindLink.End.LET_GO));
        Power[] immune = {Power.SCARLET_WITCH, Power.BILLY, Power.SPIDER_MAN};
        for (int i = 0; i < immune.length; i++) {
            Power power = immune[i];
            ServerPlayer other = f.player(8.5, FLOOR, 6.5 + 4.0 * i, power);
            TestKit.lookAt(jean, other);
            PhoenixTests.actionBarKeys(jean);
            PhoenixTests.sneakPress(jean, 3);
            check(!MindLink.isLinked(jean) && !MindLink.isHost(other), power + "'s mind is closed to her");
            check(PhoenixTests.actionBarKeys(jean).contains("message.enhanced.phoenix_body.immune"), "and she is told why (" + power + ")");
        }
        helper.succeed();
    }
}

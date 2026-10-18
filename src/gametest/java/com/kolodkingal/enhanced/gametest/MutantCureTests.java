package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.CureConfig;
import com.kolodkingal.enhanced.item.MutantCureItem;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Mutant Cure (3.3.1): it takes every hero's power away and leaves nothing of it behind (as an operator's {@code
 * power set none}), it is used up only when it cures, it turns away someone with no power, it works on another player
 * only for an operator or with PvP on and within reach, it can be switched off, and its recipe. The switched-off test has
 * a batch of its own (it changes a setting).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class MutantCureTests {
    private static final String BATCH = "mutant_cure";
    private static final String BATCH_OFF = "mutant_cure_off";

    private static ItemStack hold(ServerPlayer player, int count) {
        ItemStack cure = new ItemStack(ModItems.MUTANT_CURE.get(), count);
        player.setItemInHand(InteractionHand.MAIN_HAND, cure);
        return cure;
    }

    /** Holds right-click on himself to the end (the hold itself is the game's: the item's use, then its finish). */
    private static void injectSelf(GameTestHelper helper, ServerPlayer player, ItemStack cure) {
        cure.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        if (player.isUsingItem()) {
            cure.finishUsingItem(helper.getLevel(), player);
            player.stopUsingItem();
        }
    }

    private static boolean heard(ServerPlayer player, ResourceLocation sound) {
        return TestKit.heardSounds(player).stream().anyMatch(s -> s.id().equals(sound));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theCureTakesEveryHerosPowerAndLeavesNothingBehind(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // What a player who never had a power has: the baseline every cured hero must come back to.
        ServerPlayer plain = f.player(2.5, FLOOR, 2.5);
        PowerTicker.tick(plain);
        int n = 0;
        for (Power power : Power.values()) {
            if (!power.hasPowers()) {
                continue;
            }
            ServerPlayer hero = f.player(4.5 + (n % 6) * 2.5, FLOOR, 5.5 + (n / 6) * 3.0, power);
            n++;
            for (int i = 0; i < 3; i++) {
                PowerTicker.tick(hero);
            }
            if (hero.getAbilities().mayfly) {
                hero.getAbilities().flying = true;
            }
            check(Powers.power(hero) == power, power + ": a hero before the cure");
            ItemStack cure = hold(hero, 2);
            TestKit.sentPackets(hero);
            injectSelf(helper, hero, cure);
            PowerTicker.tick(hero);
            PowerData data = Powers.get(hero);
            check(data.power() == Power.NONE, power + ": the cure took the power: " + data.power());
            check(cure.getCount() == 1, power + ": one cure is used up: " + cure.getCount());
            for (Live live : Live.values()) {
                check(!data.is(live), power + ": nothing of the power goes on: " + live);
            }
            check(!hero.getAbilities().mayfly && !hero.getAbilities().flying, power + ": no flight left");
            for (Holder<Attribute> attribute : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
                AttributeInstance was = plain.getAttribute(attribute);
                AttributeInstance now = hero.getAttribute(attribute);
                if (was != null && now != null) {
                    check(Math.abs(was.getValue() - now.getValue()) < 1.0E-6 && now.getModifiers().size() == was.getModifiers().size(),
                            power + ": " + attribute.getRegisteredName() + " is a plain player's again: " + now.getValue() + " " + now.getModifiers());
                }
            }
            check(hero.getActiveEffects().size() == plain.getActiveEffects().size(), power + ": no effect of the power left: " + hero.getActiveEffects());
            check(hero.getHealth() <= hero.getMaxHealth(), power + ": health within the plain maximum");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theCureSaysSoAndIsKeptWhenItCannotCure(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer nobody = f.player(4.5, FLOOR, 4.5);
        ItemStack cure = hold(nobody, 1);
        TestKit.sentPackets(nobody);
        cure.use(helper.getLevel(), nobody, InteractionHand.MAIN_HAND);
        check(!nobody.isUsingItem(), "no hold for someone with no power");
        check("message.enhanced.mutant_cure.no_power".equals(lastActionBarKey(nobody)), "he is told he has no power to cure");
        check(cure.getCount() == 1, "nothing is used up");

        // A hero: the hold begins (two seconds), and its end cures him, with the hiss and the message.
        ServerPlayer thor = f.player(8.5, FLOOR, 4.5, Power.THOR);
        ItemStack his = hold(thor, 1);
        his.use(helper.getLevel(), thor, InteractionHand.MAIN_HAND);
        check(thor.isUsingItem() && thor.getUseItemRemainingTicks() == MutantCureItem.holdTicks(), "the hold begins: " + thor.getUseItemRemainingTicks());
        check(MutantCureItem.holdTicks() == 40, "two seconds by default");
        // Letting go early changes nothing.
        thor.releaseUsingItem();
        check(Powers.power(thor) == Power.THOR && his.getCount() == 1, "let go early: still Thor, the cure kept");
        TestKit.sentPackets(thor);
        injectSelf(helper, thor, his);
        check(Powers.power(thor) == Power.NONE, "cured");
        check(his.isEmpty(), "the cure is used up");
        check("message.enhanced.mutant_cure.cured".equals(lastActionBarKey(thor)), "he is told");
        ServerPlayer near = f.player(9.5, FLOOR, 6.5);
        TestKit.sentPackets(near);
        ServerPlayer again = f.player(12.5, FLOOR, 4.5, Power.LOKI);
        injectSelf(helper, again, hold(again, 1));
        check(heard(near, ModSounds.MUTANT_CURE.getId()), "the cure's sound is heard nearby");

        // In Creative the cure is kept.
        ServerPlayer creative = f.player(14.5, FLOOR, 8.5, Power.VISION);
        creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        ItemStack kept = hold(creative, 1);
        injectSelf(helper, creative, kept);
        check(Powers.power(creative) == Power.NONE && kept.getCount() == 1, "Creative: cured, the cure kept");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void anotherPlayerOnlyByAnOperatorOrWithPvpAndWithinReach(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer doctor = f.player(5.5, FLOOR, 5.5);
        ServerPlayer hero = f.player(7.5, FLOOR, 5.5, Power.MAGNETO);
        ItemStack cure = hold(doctor, 3);

        // PvP off, no operator: refused, nothing used up.
        f.pvp(false);
        TestKit.sentPackets(doctor);
        InteractionResult refused = cure.interactLivingEntity(doctor, hero, InteractionHand.MAIN_HAND);
        check(refused == InteractionResult.CONSUME && !doctor.isUsingItem(), "refused: no hold");
        check("message.enhanced.mutant_cure.not_allowed".equals(lastActionBarKey(doctor)), "he is told only an operator or PvP");
        check(Powers.power(hero) == Power.MAGNETO && cure.getCount() == 3, "still Magneto, nothing used up");

        // PvP on: the hold begins on him, and its end cures him, not the one who holds it.
        f.pvp(true);
        cure.interactLivingEntity(doctor, hero, InteractionHand.MAIN_HAND);
        check(doctor.isUsingItem(), "the hold begins on the other player");
        cure.finishUsingItem(helper.getLevel(), doctor);
        doctor.stopUsingItem();
        check(Powers.power(hero) == Power.NONE, "the other player is cured");
        check(cure.getCount() == 2, "one cure is used up");
        check("message.enhanced.mutant_cure.cured_other".equals(lastActionBarKey(doctor)), "the one who held it is told");

        // A player with no power: nothing to cure (right after the cure, right-click still held, it says nothing more).
        TestKit.sentPackets(doctor);
        cure.interactLivingEntity(doctor, hero, InteractionHand.MAIN_HAND);
        check(!doctor.isUsingItem() && lastActionBarKey(doctor) == null, "no power: no hold, and the cure's message stays");

        // Out of reach at the end: nothing happens.
        ServerPlayer runner = f.player(7.5, FLOOR, 7.5, Power.QUICKSILVER);
        cure.interactLivingEntity(doctor, runner, InteractionHand.MAIN_HAND);
        runner.teleportTo(runner.getX() + 8.0, runner.getY(), runner.getZ());
        cure.finishUsingItem(helper.getLevel(), doctor);
        doctor.stopUsingItem();
        check(Powers.power(runner) == Power.QUICKSILVER && cure.getCount() == 2, "too far: still Quicksilver, nothing used up");
        check("message.enhanced.mutant_cure.too_far".equals(lastActionBarKey(doctor)), "he is told it did not go in");

        // An operator may, PvP off.
        f.pvp(false);
        var ops = helper.getLevel().getServer().getPlayerList().getOps();
        ops.add(new ServerOpListEntry(doctor.getGameProfile(), 2, false));
        f.onClose(() -> ops.remove(doctor.getGameProfile()));
        ServerPlayer witch = f.player(5.5, FLOOR, 7.5, Power.SCARLET_WITCH);
        cure.interactLivingEntity(doctor, witch, InteractionHand.MAIN_HAND);
        cure.finishUsingItem(helper.getLevel(), doctor);
        doctor.stopUsingItem();
        check(Powers.power(witch) == Power.NONE && cure.getCount() == 1, "an operator cures another player with PvP off");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theCureIsCraftedFromAnEchoShard(GameTestHelper helper) {
        RecipeHolder<?> holder = helper.getLevel().getRecipeManager().byKey(Enhanced.id("mutant_cure")).orElse(null);
        check(holder != null && holder.value() instanceof CraftingRecipe, "the recipe exists: " + holder);
        CraftingRecipe recipe = (CraftingRecipe) holder.value();
        check(recipe.getResultItem(helper.getLevel().registryAccess()).is(ModItems.MUTANT_CURE.get()), "it makes the cure");
        List<Ingredient> ingredients = recipe.getIngredients();
        check(ingredients.size() == 4, "four ingredients: " + ingredients.size());
        for (var item : List.of(Items.GLASS_BOTTLE, Items.MILK_BUCKET, Items.ECHO_SHARD, Items.IRON_NUGGET)) {
            check(ingredients.stream().anyMatch(i -> i.test(new ItemStack(item))), "it needs " + item);
        }
        check(new ItemStack(ModItems.MUTANT_CURE.get()).getMaxStackSize() == 16, "a stack of 16");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH_OFF)
    public static void aServerCanSwitchTheCureOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(CureConfig.ENABLED, false);
        ServerPlayer hero = f.player(5.5, FLOOR, 5.5, Power.SPIDER_MAN);
        ItemStack cure = hold(hero, 1);
        TestKit.sentPackets(hero);
        injectSelf(helper, hero, cure);
        check(Powers.power(hero) == Power.SPIDER_MAN && cure.getCount() == 1, "switched off: nothing happens, nothing used up");
        check("message.enhanced.mutant_cure.disabled".equals(lastActionBarKey(hero)), "he is told it does not work here");
        f.config(CureConfig.ON_OTHERS, false);
        f.config(CureConfig.ENABLED, true);
        f.pvp(true);
        ServerPlayer other = f.player(7.5, FLOOR, 5.5);
        ItemStack its = hold(other, 1);
        its.interactLivingEntity(other, hero, InteractionHand.MAIN_HAND);
        check(!other.isUsingItem() && Powers.power(hero) == Power.SPIDER_MAN, "onOthers off: not on another player, even with PvP");
        helper.succeed();
    }

    private MutantCureTests() {
    }
}

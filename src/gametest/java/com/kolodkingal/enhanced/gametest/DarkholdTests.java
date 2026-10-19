package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.AgathaConfig;
import com.kolodkingal.enhanced.config.DarkholdConfig;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.network.BeamPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The Darkhold (3.2): who can read it, Agatha's purple beam (damage, hunger, the generic beam's rules), the retaliation (who
 * is poisoned), the hex mark and the mind glimpse. Quick tests share the batch {@code items_darkhold}; one that changes a
 * setting the others rely on has a batch of its own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class DarkholdTests {
    private static final String BATCH = "items_darkhold";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A player with a power holding the Darkhold in the main hand. */
    private static ServerPlayer holder(TestKit.Fixture f, double x, double z, Power power) {
        ServerPlayer player = f.player(x, FLOOR, z, power);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DARKHOLD.get()));
        return player;
    }

    private static boolean sorcerer(Power power) {
        // (3.3: Agatha Harkness is a sorcerer too; 3.6: and Doctor Strange.)
        return power == Power.LOKI || power == Power.SCARLET_WITCH || power == Power.BILLY || power == Power.AGATHA
                || power == Power.DOCTOR_STRANGE;
    }

    private static boolean says(Component text, String key) {
        return text.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key);
    }

    /** Use, as a right click: the beam begins (held), or with Sneak the mark or the glimpse. */
    private static void use(ServerPlayer player, boolean sneak) {
        player.setShiftKeyDown(sneak);
        InteractionHand hand = DarkholdBook.isBook(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        player.getItemInHand(hand).use(player.serverLevel(), player, hand);
        player.setShiftKeyDown(false);
    }

    /** What the game does every tick a held item is used (the fake player is not ticked by anyone). */
    private static void tickUse(ServerPlayer player) {
        PowerTicker.tick(player);
        if (player.isUsingItem()) {
            player.getUseItem().getItem().onUseTick(player.serverLevel(), player, player.getUseItem(), 100);
        }
    }

    private static Pig dummy(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(x, FLOOR, z));
    }

    private static List<BeamPayload> beamPayloads(ServerPlayer player) {
        List<BeamPayload> found = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof BeamPayload beam) {
                found.add(beam);
            }
        }
        return found;
    }

    private static DamageSource magicBy(ServerLevel level, LivingEntity attacker) {
        return ModDamageTypes.source(level, ModDamageTypes.HEX_BOLT, attacker, attacker);
    }

    private static MobEffectInstance poison(LivingEntity entity) {
        return entity.getEffect(MobEffects.POISON);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The item

    @GameTest(template = ARENA, batch = BATCH)
    public static void theDarkholdIsCraftedFromWitherAndEchoAndScrap(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("darkhold"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:darkhold exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack n = new ItemStack(Items.NETHERITE_SCRAP);
        ItemStack r = new ItemStack(Items.WITHER_ROSE);
        ItemStack e = new ItemStack(Items.ECHO_SHARD);
        ItemStack b = new ItemStack(Items.BOOK);
        ItemStack s = new ItemStack(Items.WITHER_SKELETON_SKULL);
        check(recipe.matches(CraftingInput.of(3, 3, List.of(n, r, n, e, b, e, n, s, n)), level), "N R N / E B E / N S N");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(n, new ItemStack(Items.POPPY), n, e, b, e, n, s, n)), level), "a Wither Rose, not a poppy");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(n, r, n, e, b, e, n, new ItemStack(Items.SKELETON_SKULL), n)), level),
                "a Wither Skeleton's skull, not a Skeleton's");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.DARKHOLD.get()), "it makes the Darkhold");
        check(level.getServer().getAdvancements().get(Enhanced.id("recipes/equipment/darkhold")) != null, "and its recipe is unlocked by an advancement");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theDarkholdSaysWhoCanReadIt(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.DARKHOLD.get());
        check(stack.getMaxStackSize() == 1, "one to a stack");
        boolean sorcerers = false;
        boolean beam = false;
        for (Component line : stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL)) {
            sorcerers |= says(line, "item.enhanced.darkhold.sorcerers");
            beam |= says(line, "item.enhanced.darkhold.beam");
        }
        check(sorcerers && beam, "the tooltip says only a sorcerer can read it, and what it does");
        BeamStyle style = BeamStyles.byId("darkhold");
        check(style != null && style.nameKey().equals("beam.enhanced.darkhold") && style.main() == 0xB45CFF && style.core() == 0xFFFFFF,
                "the purple beam style is registered: purple with a white core");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theMagicTagNamesTheMagicOfTheMod(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (ResourceKey<DamageType> key : List.of(ModDamageTypes.HEX_BOLT, ModDamageTypes.HEX_BEAM,
                ModDamageTypes.CHAOS_BEAM, ModDamageTypes.HEX_WALL, ModDamageTypes.TELEKINETIC_SLAM, ModDamageTypes.DARKHOLD_BEAM)) {
            check(ModDamageTypes.source(level, key, null, null).is(ModTags.MAGIC_DAMAGE), key.location() + " is magic");
        }
        check(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, Enhanced.id("entomb")))
                .map(h -> new DamageSource(h).is(ModTags.MAGIC_DAMAGE)).orElse(false), "Apocalypse's entomb is magic");
        check(!ModDamageTypes.source(level, ModDamageTypes.LIGHTNING_BEAM, null, null).is(ModTags.MAGIC_DAMAGE),
                "Thor's lightning is his power but not magic: it does not set the book off");
        check(level.damageSources().magic().is(ModTags.MAGIC_DAMAGE) && level.damageSources().indirectMagic(null, null).is(ModTags.MAGIC_DAMAGE),
                "so is the game's own");
        check(!level.damageSources().generic().is(ModTags.MAGIC_DAMAGE) && !level.damageSources().fall().is(ModTags.MAGIC_DAMAGE),
                "a blow or a fall is not");
        check(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ModDamageTypes.DARKHOLD_BEAM).isPresent(),
                "the beam's damage type exists");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theDarkholdDoesNothingInTheWitchesRoad(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ResourceKey<Level> road = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("enhanced:witches_road"));
        check(DarkholdBook.isTheRoad(road) && !DarkholdBook.isTheRoad(Level.OVERWORLD) && !DarkholdBook.isTheRoad(Level.NETHER),
                "the Road is the one world where the book is silent");
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        check(!DarkholdBook.inTheRoad(loki) && DarkholdBook.mayUse(loki), "elsewhere it works");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Who can use it

    @GameTest(template = ARENA, batch = BATCH)
    public static void onlyLokiTheScarletWitchAndBillyCanUseIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(!DarkholdConfig.ANYONE_CAN_USE.get(), "anyoneCanUse is off by default");
        for (Power power : Power.values()) {
            ServerPlayer player = holder(f, 10.5, 10.5, power);
            check(DarkholdBook.mayUse(player) == sorcerer(power), power + ": sorcerer = " + sorcerer(power));
            check(DarkholdBook.active(player) == sorcerer(power), power + ": the book is active in their hand only if they may use it");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void anyoneElseHearsOnlyWhispers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Pig pig = dummy(helper, 14.5, 10.5);
        // (3.3: a player with no power who reads it becomes Agatha instead: AgathaTests.)
        for (Power power : new Power[]{Power.TOMMY, Power.THOR, Power.VISION}) {
            ServerPlayer player = holder(f, 4.5, 10.5, power);
            lookAt(player, pig);
            TestKit.sentPackets(player);
            use(player, false);
            check("message.enhanced.darkhold.not_sorcerer".equals(lastActionBarKey(player)), power + ": 'only a sorcerer can read it'");
            check(!ContinuousBeam.isFiring(player) && !player.isUsingItem() && !DarkholdBook.firing(player), power + ": no beam");
            use(player, true);
            check(pig.getEffect(MobEffects.GLOWING) == null, power + ": no mark");
            DamageSource magic = magicBy(helper.getLevel(), pig);
            player.hurt(magic, 1.0F);
            check(poison(pig) == null, power + ": and no retaliation");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "items_darkhold_anyone")
    public static void anyoneCanUseItWhenTheConfigSaysSo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DarkholdConfig.ANYONE_CAN_USE, true);
        // (3.3: with agatha.darkholdAwakens on, a player with no power who reads it becomes Agatha instead.)
        f.config(AgathaConfig.DARKHOLD_AWAKENS, false);
        ServerPlayer nobody = holder(f, 4.5, 10.5, Power.NONE);
        Pig pig = dummy(helper, 14.5, 10.5);
        check(DarkholdBook.mayUse(nobody) && DarkholdBook.active(nobody), "with anyoneCanUse a player with no power may");
        lookAt(nobody, pig);
        use(nobody, false);
        check(ContinuousBeam.isFiring(nobody) && DarkholdBook.firing(nobody) && Powers.get(nobody).is(Live.BEAMING), "and fires the beam");
        float before = pig.getHealth();
        for (int i = 0; i < 12; i++) {
            tickUse(nobody);
        }
        check(pig.getHealth() < before, "which hurts what the crosshair is on");
        nobody.stopUsingItem();
        check(!ContinuousBeam.isFiring(nobody), "and stops when use is released");
        Pig attacker = dummy(helper, 16.5, 16.5);
        nobody.hurt(magicBy(helper.getLevel(), attacker), 1.0F);
        check(poison(attacker) != null, "the retaliation too");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The purple beam

    @GameTest(template = ARENA, batch = "items_darkhold_beam", timeoutTicks = 100)
    public static void theBeamHurtsWhatTheCrosshairIsOnForTheConfiguredDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = holder(f, 3.5, 10.5, Power.LOKI);
        PowerTicker.tick(loki);
        Pig target = dummy(helper, 10.5, 10.5);
        Pig aside = dummy(helper, 10.5, 14.5);
        lookAt(loki, target);
        TestKit.sentPackets(loki);
        use(loki, false);
        check(loki.isUsingItem() && ContinuousBeam.isFiring(loki) && Powers.get(loki).is(Live.BEAMING), "use held: the beam burns");
        float perHit = DarkholdConfig.BEAM_DAMAGE_PER_SECOND.get().floatValue() * DarkholdConfig.BEAM_INTERVAL.get() / 20.0F;
        assertNear(perHit, 2.0, 1.0E-4, "a hit of the default beam is 2.0 (8 a second, a hit every 5 ticks)");
        List<BeamPayload> told = beamPayloads(loki);
        check(!told.isEmpty() && told.get(0).on() && told.get(0).style().equals("darkhold") && told.get(0).shooter() == loki.getId(),
                "the clients are told, in the Darkhold's style: " + told);
        f.everyTick(70, tick -> {
            tickUse(loki);
            if (tick == 20) {
                float lost = target.getMaxHealth() - target.getHealth();
                double hits = lost / perHit;
                check(Math.abs(hits - Math.round(hits)) < 0.01 && hits >= 4 && hits <= 5, "four or five hits in a second: " + lost);
                check(aside.getHealth() == aside.getMaxHealth(), "what the crosshair is not on is not hurt");
                check(target.getLastDamageSource() != null && target.getLastDamageSource().is(ModDamageTypes.DARKHOLD_BEAM)
                        && target.getLastDamageSource().getEntity() == loki, "it is the Darkhold's damage and Loki gets the credit");
                helper.setBlock(7, (int) FLOOR + 1, 10, Blocks.STONE);
                helper.setBlock(7, (int) FLOOR, 10, Blocks.STONE);
                target.setHealth(target.getMaxHealth());
            }
            if (tick == 40) {
                check(target.getHealth() == target.getMaxHealth(), "a wall in the way takes it all");
                loki.stopUsingItem();
                check(!ContinuousBeam.isFiring(loki) && !Powers.get(loki).is(Live.BEAMING) && !DarkholdBook.firing(loki), "released, it stops");
                check(beamPayloads(loki).stream().anyMatch(p -> !p.on()), "and the clients are told it stopped");
                helper.setBlock(7, (int) FLOOR + 1, 10, Blocks.AIR);
                helper.setBlock(7, (int) FLOOR, 10, Blocks.AIR);
            }
            if (tick > 40 && tick < 70) {
                check(target.getHealth() == target.getMaxHealth(), "and hurts nothing after (tick " + tick + ")");
            }
            return tick == 70;
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theBeamCostsHungerNeedsFoodAndWorksFromTheOffHand(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        wanda.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.DARKHOLD.get()));
        wanda.getFoodData().setFoodLevel(0);
        use(wanda, false);
        check(!ContinuousBeam.isFiring(wanda) && "message.enhanced.darkhold.hungry".equals(lastActionBarKey(wanda)), "on an empty stomach it cannot start");
        wanda.stopUsingItem();
        wanda.getFoodData().setFoodLevel(20);
        use(wanda, false);
        check(ContinuousBeam.isFiring(wanda), "fed, it starts, from the off hand too");
        float exhaustion = wanda.getFoodData().getExhaustionLevel();
        for (int i = 0; i < 20; i++) {
            tickUse(wanda);
        }
        assertNear(wanda.getFoodData().getExhaustionLevel() - exhaustion, DarkholdConfig.BEAM_EXHAUSTION_PER_SECOND.get(), 0.2,
                "a second of beam costs its exhaustion");
        wanda.getFoodData().setFoodLevel(0);
        tickUse(wanda);
        check(!ContinuousBeam.isFiring(wanda) && !DarkholdBook.firing(wanda), "starving ends it");
        wanda.getFoodData().setFoodLevel(20);
        wanda.stopUsingItem();
        use(wanda, false);
        wanda.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        tickUse(wanda);
        check(!ContinuousBeam.isFiring(wanda), "putting the book away ends it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "items_darkhold_billy", timeoutTicks = 60)
    public static void billyWithTheDarkholdFiresOnlyItsBeam(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = holder(f, 3.5, 10.5, Power.BILLY);
        Pig target = dummy(helper, 10.5, 10.5);
        lookAt(billy, target);
        PowerTicker.tick(billy);
        use(billy, false);
        check(DarkholdBook.firing(billy), "Billy fires the Darkhold's beam");
        f.everyTick(21, tick -> {
            tickUse(billy);
            if (tick < 21) {
                return false;
            }
            float lost = target.getMaxHealth() - target.getHealth();
            double hits = lost / 2.0;
            check(Math.abs(hits - Math.round(hits)) < 0.01 && hits >= 4 && hits <= 5,
                    "every hit is the Darkhold's 2.0, his own beam does not run beside it: " + lost);
            check(target.getLastDamageSource() != null && target.getLastDamageSource().is(ModDamageTypes.DARKHOLD_BEAM), "the damage is the Darkhold's");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "items_darkhold_pvp", timeoutTicks = 80)
    public static void theBeamKeepsTheRulesOfPvP(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(false);
        ServerPlayer loki = holder(f, 3.5, 10.5, Power.LOKI);
        ServerPlayer other = f.player(10.5, FLOOR, 10.5);
        lookAt(loki, other);
        PowerTicker.tick(loki);
        use(loki, false);
        f.everyTick(55, tick -> {
            tickUse(loki);
            if (tick == 20) {
                check(other.getHealth() == other.getMaxHealth(), "with PvP off the beam does not hurt another player");
                loki.stopUsingItem();
                f.pvp(true);
                use(loki, false);
            }
            if (tick < 55) {
                return false;
            }
            check(other.getHealth() < other.getMaxHealth(), "with PvP on it does");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The retaliation

    @GameTest(template = ARENA, batch = BATCH)
    public static void magicUsedOnTheHolderPoisonsTheAttackerNotTheHolder(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        // (A pig: the undead shrug poison off.)
        Pig attacker = dummy(helper, 14.5, 10.5);
        check(DarkholdConfig.POISON_TARGET.get() == DarkholdConfig.PoisonTarget.ATTACKER && DarkholdConfig.POISON_TICKS.get() == 200,
                "by default the attacker, ten seconds");
        // A blow is not magic.
        loki.hurt(level.damageSources().mobAttack(attacker), 1.0F);
        check(poison(attacker) == null && poison(loki) == null, "a blow poisons nobody");
        // Magic is.
        loki.invulnerableTime = 0;
        loki.hurt(magicBy(level, attacker), 1.0F);
        MobEffectInstance poison = poison(attacker);
        check(poison != null && poison.getDuration() == 200 && poison.getAmplifier() == DarkholdConfig.POISON_AMPLIFIER.get(),
                "the attacker is poisoned for 10 seconds: " + poison);
        check(poison(loki) == null, "the holder is not");
        // Not again at once.
        attacker.removeEffect(MobEffects.POISON);
        loki.hurt(magicBy(level, attacker), 1.0F);
        check(poison(attacker) == null, "a second use at once is not answered (the short cooldown per attacker)");
        // The game's own magic damage counts, thrown by a creature.
        Pig another = dummy(helper, 14.5, 14.5);
        loki.hurt(level.damageSources().indirectMagic(another, another), 1.0F);
        check(poison(another) != null, "a witch's potion (indirect magic) too");
        // No book in hand: nothing.
        Pig third = dummy(helper, 16.5, 16.5);
        loki.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        loki.hurt(magicBy(level, third), 1.0F);
        check(poison(third) == null, "without the book in hand there is no retaliation");
        loki.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.DARKHOLD.get()));
        loki.hurt(magicBy(level, third), 1.0F);
        check(poison(third) != null, "the off hand will do");
        // Hurting oneself with magic is no one's attack.
        loki.removeEffect(MobEffects.POISON);
        loki.hurt(level.damageSources().magic(), 1.0F);
        check(poison(loki) == null, "magic with no attacker poisons nobody");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "items_darkhold_holder")
    public static void theConfigCanPoisonTheHolderInstead(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DarkholdConfig.POISON_TARGET, DarkholdConfig.PoisonTarget.HOLDER);
        f.config(DarkholdConfig.POISON_TICKS, 100);
        f.config(DarkholdConfig.POISON_AMPLIFIER, 1);
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        Pig attacker = dummy(helper, 14.5, 10.5);
        loki.hurt(magicBy(helper.getLevel(), attacker), 1.0F);
        MobEffectInstance poison = poison(loki);
        check(poison != null && poison.getDuration() <= 100 && poison.getDuration() > 90 && poison.getAmplifier() == 1, "the holder is poisoned: " + poison);
        check(poison(attacker) == null, "the attacker is not");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The hex mark and the mind glimpse

    @GameTest(template = ARENA, batch = "items_darkhold_marks", timeoutTicks = 80)
    public static void sneakAndUseMarksWhatYouLookAtAndItTakesMoreFromYou(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DarkholdConfig.MARK_TICKS, 30);
        f.config(DarkholdConfig.MARK_COOLDOWN, 60);
        ServerPlayer loki = holder(f, 3.5, 10.5, Power.LOKI);
        ServerPlayer other = holder(f, 3.5, 4.5, Power.BILLY);
        Pig target = dummy(helper, 10.5, 10.5);
        lookAt(loki, target);
        TestKit.sentPackets(loki);
        use(loki, true);
        check(target.getEffect(MobEffects.GLOWING) != null, "the creature glows");
        MobEffectInstance slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(slow != null && slow.getAmplifier() == DarkholdConfig.MARK_SLOW_AMPLIFIER.get(), "and is slowed: " + slow);
        check("message.enhanced.darkhold.marked".equals(lastActionBarKey(loki)), "Loki is told");
        long now = helper.getLevel().getGameTime();
        assertNear(DarkholdBook.markedFactor(target, loki), 1.5, 1.0E-6, "it takes half as much again from Loki");
        assertNear(DarkholdBook.markedFactor(target, other), 1.0, 1.0E-6, "but not from anyone else");
        assertNear(DarkholdBook.markedFactor(target, null), 1.0, 1.0E-6, "nor from nothing");
        target.hurt(helper.getLevel().damageSources().playerAttack(loki), 4.0F);
        assertNear(target.getMaxHealth() - target.getHealth(), 6.0, 1.0E-4, "4 damage from Loki is 6");
        target.setHealth(target.getMaxHealth());
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().playerAttack(other), 4.0F);
        assertNear(target.getMaxHealth() - target.getHealth(), 4.0, 1.0E-4, "but 4 from another is 4");
        // Again at once: the cooldown.
        target.invulnerableTime = 0;
        use(loki, true);
        check("message.enhanced.darkhold.not_ready".equals(lastActionBarKey(loki)), "a second mark at once is told to wait");
        f.everyTick(40, tick -> {
            if (tick < 35) {
                return false;
            }
            assertNear(DarkholdBook.markedFactor(target, loki), 1.0, 1.0E-6, "the mark is over after its configured time");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aPlayerCanOnlyBeMarkedWhereTheyCanBeHarmed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = holder(f, 3.5, 10.5, Power.LOKI);
        ServerPlayer other = f.player(10.5, FLOOR, 10.5);
        lookAt(loki, other);
        TestKit.sentPackets(loki);
        use(loki, true);
        check(other.getEffect(MobEffects.GLOWING) == null && "message.enhanced.darkhold.mark_refused".equals(lastActionBarKey(loki)),
                "with PvP off another player is not marked");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void sneakAndUseAtNothingGlimpsesTheCreaturesNear(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        Pig near = dummy(helper, 14.5, 10.5);
        Zombie nearer = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        nearer.setHealth(5.0F);
        TestKit.look(loki, 0.0F, -90.0F);
        TestKit.sentPackets(loki);
        use(loki, true);
        check(near.getEffect(MobEffects.GLOWING) != null && nearer.getEffect(MobEffects.GLOWING) != null, "every creature near glows");
        check(loki.getEffect(MobEffects.GLOWING) == null, "but not the holder");
        Component said = null;
        for (Object packet : TestKit.sentPackets(loki)) {
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat && chat.overlay()) {
                said = chat.content();
            }
        }
        check(said != null && says(said, "message.enhanced.darkhold.glimpse"), "the action bar says what the glimpse found");
        TranslatableContents contents = (TranslatableContents) said.getContents();
        check(contents.getArgs()[0].equals(2) && ((Component) contents.getArgs()[1]).getString().equals(nearer.getDisplayName().getString())
                && contents.getArgs()[2].equals("5.0"), "two creatures, the nearest the zombie, at 5.0 health: " + java.util.Arrays.toString(contents.getArgs()));
        use(loki, true);
        check("message.enhanced.darkhold.not_ready".equals(lastActionBarKey(loki)), "and it has a cooldown");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "items_darkhold_glimpse")
    public static void theGlimpseSeesNothingBeyondItsRangeAndSaysSoWhenNothingIsNear(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DarkholdConfig.GLIMPSE_RANGE, 5);
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        Pig far = dummy(helper, 18.5, 10.5);
        TestKit.look(loki, 0.0F, -90.0F);
        TestKit.sentPackets(loki);
        use(loki, true);
        check(far.getEffect(MobEffects.GLOWING) == null, "a creature beyond the range does not glow");
        check("message.enhanced.darkhold.glimpse_none".equals(lastActionBarKey(loki)), "and the book says nothing living is near");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Review fixes

    @GameTest(template = ARENA, batch = BATCH)
    public static void magicEffectsOnTheHolderAnswerToo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        Pig caster = dummy(helper, 14.5, 10.5);
        loki.addEffect(new MobEffectInstance(ModEffects.CONFUSED, 100, 0), caster);
        check(poison(caster) != null, "a spell of confusion laid on the holder poisons whoever laid it");
        Pig other = dummy(helper, 14.5, 14.5);
        loki.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100, 0), other);
        check(poison(other) != null, "so does a freeze");
        Pig harmless = dummy(helper, 16.5, 16.5);
        loki.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0), harmless);
        check(poison(harmless) == null, "an ordinary effect does not");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "items_darkhold_billy_r", timeoutTicks = 60)
    public static void releasingRDoesNotStopTheDarkholdsBeam(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = holder(f, 3.5, 10.5, Power.BILLY);
        PowerTicker.tick(billy);
        use(billy, false);
        check(DarkholdBook.firing(billy), "the Darkhold's beam burns");
        AbilityHandler.onKey(billy, 1, false);
        check(DarkholdBook.firing(billy) && ContinuousBeam.isFiring(billy), "R coming up does not put it out");
        AbilityHandler.onKey(billy, 1, true);
        AbilityHandler.onKey(billy, 1, false);
        check(DarkholdBook.firing(billy) && ContinuousBeam.isFiring(billy), "nor does a tap of R");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "items_darkhold_boss_mark")
    public static void aBossIsMarkedButNotSlowed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = holder(f, 3.5, 10.5, Power.LOKI);
        Mob wither = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(10.5, FLOOR, 10.5));
        lookAt(loki, wither);
        TestKit.sentPackets(loki);
        DarkholdBook.mark(loki, wither);
        check(wither.getEffect(MobEffects.GLOWING) != null, "the boss glows (" + wither.getActiveEffects() + ")");
        check(wither.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null, "but is not slowed");
        assertNear(DarkholdBook.markedFactor(wither, loki), 1.5, 1.0E-6, "and takes the marker's extra damage");
        check(DarkholdBook.bossLike(wither) && !DarkholdBook.bossLike(helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 12.5))), "bosses are told from the rest");
        wither.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theGlimpseDoesNotShowWhatHides(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        Pig seen = dummy(helper, 14.5, 10.5);
        Pig invisible = dummy(helper, 14.5, 12.5);
        invisible.setInvisible(true);
        ServerPlayer hiding = f.player(12.5, FLOOR, 14.5, Power.LOKI);
        Powers.get(hiding).set(Live.HIDDEN, true);
        check(DarkholdBook.hidden(invisible) && DarkholdBook.hidden(hiding) && !DarkholdBook.hidden(seen), "what is invisible or in Vanish hides");
        TestKit.look(loki, 0.0F, -90.0F);
        use(loki, true);
        check(seen.getEffect(MobEffects.GLOWING) != null, "the one in plain sight glows");
        check(invisible.getEffect(MobEffects.GLOWING) == null && hiding.getEffect(MobEffects.GLOWING) == null, "the hidden do not");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "items_darkhold_marks_prune", timeoutTicks = 80)
    public static void endedMarksAreDropped(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(DarkholdConfig.MARK_TICKS, 20);
        ServerPlayer loki = holder(f, 3.5, 10.5, Power.LOKI);
        Pig pig = dummy(helper, 10.5, 10.5);
        int before = DarkholdBook.markCount();
        DarkholdBook.mark(loki, pig);
        check(DarkholdBook.markCount() == before + 1, "the mark is kept");
        f.everyTick(60, tick -> {
            loki.tickCount = 3 + 20 * tick; // (the second of the prune)
            PowerTicker.tick(loki);
            if (tick < 40) {
                return false;
            }
            check(DarkholdBook.markCount() == before, "the mark that ran out is gone, though the pig was never hit again");
            return true;
        });
    }
}

package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.entity.LokiDaggerEntity;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.item.TesseractItem;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.LokiForm;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/** Loki: the Tesseract, his shared tricks and his Frost Giant form. */
public final class LokiTests {

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    /** The whole arena, with room above it. */
    private static AABB arena(GameTestHelper helper) {
        return new AABB(helper.absoluteVec(new Vec3(0, 0, 0)), helper.absoluteVec(new Vec3(21, 12, 21)));
    }

    /** How many times {@code listener} heard {@code sound} played inside {@code where} since it last asked. */
    private static long heard(ServerPlayer listener, AABB where, DeferredHolder<SoundEvent, SoundEvent> sound) {
        return TestKit.heardSounds(listener).stream().filter(s -> s.id().equals(sound.getId()) && where.contains(s.at())).count();
    }

    // ---- the Tesseract ----

    @EnhancedTest(template = ARENA)
    public static void tesseractMakesYouLokiFarAway(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        Vec3 start = player.position();
        TestKit.heardSounds(player);

        player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);

        check(Powers.is(player, Power.LOKI), "whoever escapes with the Tesseract becomes Loki");
        List<TestKit.HeardSound> sounds = TestKit.heardSounds(player).stream().filter(s -> s.id().equals(ModSounds.TESSERACT.getId())).toList();
        check(sounds.stream().anyMatch(s -> arena(helper).contains(s.at())), "the Tesseract opens with its own sound where they vanish");
        check(sounds.stream().anyMatch(s -> !arena(helper).contains(s.at())), "and again where they appear");
        double distance = player.position().subtract(start).multiply(1, 0, 1).length();
        check(distance >= TesseractItem.ESCAPE_MIN && distance <= TesseractItem.ESCAPE_MAX,
                "they reappear 100-250 blocks away, not " + distance);
        BlockPos feet = player.blockPosition();
        check(level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP), "on solid ground at " + feet);
        check(level.noCollision(player), "not inside blocks");
        check(!level.containsAnyLiquid(player.getBoundingBox()), "not in water or lava");
        check(player.getMainHandItem().is(ModItems.TESSERACT.get()) && player.getMainHandItem().getCount() == 1,
                "the Tesseract isn't used up");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void tesseractTakesAHeroWhereTheyLook(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        magneto.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        BlockPos block = new BlockPos(12, 2, 10);
        helper.setBlock(block, Blocks.STONE);
        lookAt(magneto, helper.absoluteVec(Vec3.atCenterOf(block)));
        TestKit.heardSounds(magneto);

        magneto.getMainHandItem().use(level, magneto, InteractionHand.MAIN_HAND);

        check(heard(magneto, arena(helper), ModSounds.TESSERACT) == 2, "the jump plays the Tesseract's sound at both ends");
        Vec3 rel = helper.relativeVec(magneto.position());
        check(Math.abs(rel.x - 12.5) < 0.01 && Math.abs(rel.z - 10.5) < 0.01 && Math.abs(rel.y - 3.0) < 0.01,
                "the Space Stone puts him on top of the block he looked at, not at " + rel);
        check(Powers.is(magneto, Power.MAGNETO), "a hero stays who he is");
        check(magneto.getCooldowns().isOnCooldown(ModItems.TESSERACT.get()), "and the cube needs a moment before the next jump");
        check(magneto.getMainHandItem().is(ModItems.TESSERACT.get()), "the Tesseract isn't used up");
        helper.succeed();
    }

    // ---- Ability 1: Daggers ----

    @EnhancedTest(template = ARENA)
    public static void daggerHurtsAndVanishes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = f.player(4.5, FLOOR, 10.5, Power.LOKI);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 10.5));
        // Out in the daylight it would burn, and a point of fire damage would look like the dagger's.
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200));
        lookAt(loki, zombie);
        float health = zombie.getHealth();

        press(loki, 1);
        check(level.getEntitiesOfClass(LokiDaggerEntity.class, arena(helper)).size() == 1, "R throws one dagger");
        check(!Powers.get(loki).isReady(1, level.getGameTime()), "with a short cooldown");

        f.everyTick(40, tick -> {
            if (zombie.getHealth() >= health) {
                return false;
            }
            check(health - zombie.getHealth() > 4.0F, "a dagger hits hard, not " + (health - zombie.getHealth()));
            check(level.getEntitiesOfClass(LokiDaggerEntity.class, arena(helper)).isEmpty(), "the dagger is gone the moment it hits");
            check(level.getEntitiesOfClass(ItemEntity.class, arena(helper)).isEmpty(), "and leaves nothing to pick up");
            return true;
        });
    }

    @EnhancedTest(template = ARENA)
    public static void sneakingThrowsAFanOfThree(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = f.player(4.5, FLOOR, 10.5, Power.LOKI);
        look(loki, EAST, 0.0F);
        loki.setShiftKeyDown(true);
        press(loki, 1);
        List<LokiDaggerEntity> daggers = level.getEntitiesOfClass(LokiDaggerEntity.class, arena(helper));
        check(daggers.size() == 3, "Sneak + R throws a fan of three, not " + daggers.size());
        check(Powers.get(loki).cooldownUntil(1) - level.getGameTime() == LokiAbilities.FAN_COOLDOWN, "a fan takes longer to conjure");
        helper.succeed();
    }

    // ---- Ability 2: Duplicates ----

    @EnhancedTest(template = ARENA, timeoutTicks = 260)
    public static void duplicatesDrawEnemiesAwayAndFade(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = f.player(10.5, FLOOR, 10.5, Power.LOKI);
        PowerData data = Powers.get(loki);
        Skeleton skeleton = TestKit.spawnWithNoFreeWill(helper, EntityType.SKELETON, new Vec3(17.5, FLOOR, 10.5));
        skeleton.setTarget(loki);
        check(skeleton.getTarget() == loki, "the skeleton is after Loki");

        press(loki, 2);

        check(loki.isInvisible() && loki.hasEffect(MobEffects.INVISIBILITY), "Surprise! Loki is invisible");
        MobEffectInstance invisibility = loki.getEffect(MobEffects.INVISIBILITY);
        check(invisibility != null && !invisibility.isVisible(), "without a trail of potion swirls");
        check(data.is(Live.HIDDEN), "his held items and name are hidden too");
        List<LokiIllusionEntity> copies = LokiIllusionEntity.ownedBy(level, loki.getUUID());
        check(copies.size() == LokiAbilities.DUPLICATES, "three duplicates appear, not " + copies.size());
        check(skeleton.getTarget() instanceof LokiIllusionEntity, "the skeleton goes after a duplicate instead");

        LokiIllusionEntity touched = copies.get(0);
        check(touched.hurt(level.damageSources().mobAttack(skeleton), 1.0F), "a duplicate can be hit");
        check(touched.isRemoved(), "and one hit dissolves it");
        check(LokiIllusionEntity.ownedBy(level, loki.getUUID()).size() == LokiAbilities.DUPLICATES - 1, "the others are still there");

        f.everyTick(LokiAbilities.DUPLICATE_TICKS + 20, tick -> {
            loki.doTick();
            if (tick == LokiAbilities.HIDE_TICKS + 5) {
                check(!loki.isInvisible() && !data.is(Live.HIDDEN), "after 5 seconds Loki can be seen again");
            }
            if (tick == LokiAbilities.DUPLICATE_TICKS / 2) {
                check(!LokiIllusionEntity.ownedBy(level, loki.getUUID()).isEmpty(), "the duplicates last a while");
            }
            return tick > LokiAbilities.DUPLICATE_TICKS && LokiIllusionEntity.ownedBy(level, loki.getUUID()).isEmpty();
        });
    }

    // ---- Ability 3: Shapeshift ----

    @EnhancedTest(template = ARENA, timeoutTicks = 160)
    public static void shapeshiftIntoAZombieAndBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(4.5, FLOOR, 10.5, Power.LOKI);
        PowerData data = Powers.get(loki);
        Zombie model = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 10.5));
        lookAt(loki, model);
        press(loki, 3);
        check(LokiForm.of(data).map(LokiForm::type).orElse(null) == EntityType.ZOMBIE, "Loki takes the zombie's shape: " + data.form());

        model.setTarget(loki);
        check(model.getTarget() == null, "a zombie never goes after one of its own");

        // Real target selection: a zombie leaves him alone, while a husk (another kind) comes for him.
        Zombie zombie = TestKit.spawn(helper, EntityType.ZOMBIE, new Vec3(4.5, FLOOR, 15.5));
        Husk husk = TestKit.spawn(helper, EntityType.HUSK, new Vec3(4.5, FLOOR, 5.5));
        f.everyTick(120, tick -> {
            check(zombie.getTarget() != loki, "the zombie doesn't pick Loki as its target");
            if (husk.getTarget() != loki) {
                return false;
            }
            // Looking at nothing (straight up at the sky), he is himself again.
            look(loki, EAST, -90.0F);
            press(loki, 3);
            check(data.form().isEmpty(), "pressing again while looking at nothing turns him back");
            model.setTarget(loki);
            check(model.getTarget() == loki, "and zombies can see him for what he is again");

            zombie.discard();
            husk.discard();
            ElderGuardian boss = TestKit.spawnWithNoFreeWill(helper, EntityType.ELDER_GUARDIAN, new Vec3(9.5, FLOOR + 3.0, 14.5));
            lookAt(loki, boss);
            press(loki, 3);
            check(data.form().isEmpty(), "a boss can't be copied");
            return true;
        });
    }

    // ---- Ability 4: Vanish ----

    @EnhancedTest(template = ARENA)
    public static void vanishPutsLokiBehindACreature(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(3.5, FLOOR, 10.5, Power.LOKI);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 10.5));
        // The zombie faces Loki, to the west.
        zombie.setYRot(90.0F);
        zombie.setYBodyRot(90.0F);
        zombie.setYHeadRot(90.0F);
        lookAt(loki, zombie);

        press(loki, 4);

        Vec3 rel = helper.relativeVec(loki.position());
        check(rel.x > 11.0 && rel.x < 12.6 && Math.abs(rel.z - 10.5) < 0.05, "Loki reappears right behind the zombie, not at " + rel);
        check(Math.abs(Mth.wrapDegrees(loki.getYRot() - 90.0F)) < 1.0F, "facing its back, not " + loki.getYRot());
        check(!Powers.get(loki).isReady(4, helper.getLevel().getGameTime()), "Vanish has a cooldown");

        // Looking at a block, he lands on top of it.
        ServerPlayer second = f.player(3.5, FLOOR, 4.5, Power.LOKI);
        BlockPos block = new BlockPos(14, 2, 4);
        helper.setBlock(block, Blocks.STONE);
        lookAt(second, helper.absoluteVec(Vec3.atCenterOf(block)));
        press(second, 4);
        Vec3 top = helper.relativeVec(second.position());
        check(Math.abs(top.x - 14.5) < 0.01 && Math.abs(top.y - 3.0) < 0.01 && Math.abs(top.z - 4.5) < 0.01,
                "looking at a block, he lands on top of it, not at " + top);
        helper.succeed();
    }

    // ---- Ability 5: Frost Giant ----

    @EnhancedTest(template = ARENA)
    public static void frostGiantIsColdAndHard(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(5.5, FLOOR, 10.5, Power.LOKI);
        PowerData data = Powers.get(loki);
        PowerTicker.tick(loki);
        assertNear(loki.getAttributeValue(Attributes.MAX_HEALTH), 30.0, 1.0E-6, "Loki is Asgardian-tough: 15 hearts (3.5)");
        double armor = loki.getAttributeValue(Attributes.ARMOR);
        assertNear(armor, 6.0, 1.0E-6, "and his body has 6 armor");
        assertNear(loki.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 2.0, 1.0E-6, "and 2 toughness");

        press(loki, 5);
        check(data.toggle(Toggle.FROST_GIANT), "N reveals his true Frost Giant form");
        assertNear(loki.getAttributeValue(Attributes.ARMOR) - armor, LokiAbilities.FROST_ARMOR_BONUS, 1.0E-6, "a Frost Giant's hide: +4 armor");

        float health = loki.getHealth();
        loki.hurt(loki.damageSources().freeze(), 4.0F);
        assertNear(loki.getHealth(), health, 1.0E-6, "the cold can't hurt a Frost Giant");
        loki.setTicksFrozen(300);
        PowerTicker.tick(loki);
        check(loki.getTicksFrozen() == 0, "and he never freezes");

        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(7.0, FLOOR, 10.5));
        loki.attack(zombie);
        MobEffectInstance slowness = zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(slowness != null && slowness.getAmplifier() == 1 && slowness.getDuration() >= LokiAbilities.FREEZE_TICKS - 1,
                "his blow slows the zombie (Slowness II for 3 seconds)");
        check(zombie.isFullyFrozen(), "and freezes it over");

        press(loki, 5);
        check(!data.toggle(Toggle.FROST_GIANT), "pressed again, he looks Asgardian");
        assertNear(loki.getAttributeValue(Attributes.ARMOR), armor, 1.0E-6, "the extra armor goes with the form");

        // Changing look while a Frost Giant keeps the form: it belongs to Loki, not to an outfit.
        press(loki, 5);
        check(data.toggle(Toggle.FROST_GIANT), "a Frost Giant again");
        Powers.setVariant(loki, Variants.get(Power.LOKI, "tva"));
        PowerTicker.tick(loki);
        check(data.toggle(Toggle.FROST_GIANT), "changing look doesn't end the Frost Giant form");
        assertNear(loki.getAttributeValue(Attributes.ARMOR) - armor, LokiAbilities.FROST_ARMOR_BONUS, 1.0E-6, "or its armor");
        helper.succeed();
    }

    /**
     * 3.5: Loki's Asgardian body is set by {@code [loki]} bodyHealthBonus (10), bodyArmor (6) and bodyToughness (2): as
     * tough as Thor's body, below the crowned Scarlet Witch; the Frost Giant's +4 armor comes on top. A change of the
     * config takes hold on the next tick, and his health never stays above the new maximum.
     */
    @EnhancedTest(template = ARENA, batch = "loki_body")
    public static void lokisBodyFollowsTheConfig(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(EnhancedConfig.LOKI_BODY_HEALTH.getDefault() == 10.0 && EnhancedConfig.LOKI_BODY_ARMOR.getDefault() == 6.0
                && EnhancedConfig.LOKI_BODY_TOUGHNESS.getDefault() == 2.0, "the defaults: +10 health, 6 armor, 2 toughness");
        ServerPlayer loki = f.player(5.5, FLOOR, 10.5, Power.LOKI);
        ServerPlayer thor = f.player(5.5, FLOOR, 4.5, Power.THOR);
        PowerTicker.tick(loki);
        PowerTicker.tick(thor);
        assertNear(loki.getMaxHealth(), thor.getMaxHealth(), 1.0E-6, "as much health as Thor");
        assertNear(loki.getAttributeValue(Attributes.ARMOR), thor.getAttributeValue(Attributes.ARMOR), 1.0E-6, "and as much armor");
        press(loki, 5);
        PowerTicker.tick(loki);
        assertNear(loki.getAttributeValue(Attributes.ARMOR), 10.0, 1.0E-6, "the Frost Giant: 6 + 4 armor");
        press(loki, 5);
        loki.setHealth(loki.getMaxHealth());
        f.config(EnhancedConfig.LOKI_BODY_HEALTH, 4.0);
        f.config(EnhancedConfig.LOKI_BODY_ARMOR, 0.0);
        f.config(EnhancedConfig.LOKI_BODY_TOUGHNESS, 5.0);
        PowerTicker.tick(loki);
        assertNear(loki.getMaxHealth(), 24.0, 1.0E-6, "the 3.4 body again: 24 health");
        assertNear(loki.getHealth(), 24.0, 1.0E-6, "his health comes down to it");
        assertNear(loki.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-6, "no armor");
        assertNear(loki.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 5.0, 1.0E-6, "5 toughness");
        Powers.setPower(loki, Power.NONE);
        assertNear(loki.getMaxHealth(), 20.0, 1.0E-6, "an ordinary body without the power");
        assertNear(loki.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 0.0, 1.0E-6, "and no toughness");
        helper.succeed();
    }

    /** No look has an ability of its own: ability 5 is the Frost Giant form whichever outfit Loki wears. */
    @EnhancedTest(template = ARENA)
    public static void frostGiantIsAbilityFiveInEveryLook(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(5.5, FLOOR, 10.5, Power.LOKI);
        PowerData data = Powers.get(loki);
        PowerTicker.tick(loki);
        double armor = loki.getAttributeValue(Attributes.ARMOR);

        for (String id : List.of("loki", "tva", "god_of_stories", "own")) {
            Powers.setVariant(loki, Variants.get(Power.LOKI, id));
            check(Variants.current(data).id().equals(id), "Loki wears the " + id + " look");

            press(loki, 5);
            check(data.toggle(Toggle.FROST_GIANT), "N reveals the Frost Giant in the " + id + " look");
            assertNear(loki.getAttributeValue(Attributes.ARMOR) - armor, LokiAbilities.FROST_ARMOR_BONUS, 1.0E-6,
                    "with its armor in the " + id + " look");
            Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(7.0, FLOOR, 10.5));
            loki.attack(zombie);
            check(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "and its blow slows in the " + id + " look");
            zombie.discard();

            press(loki, 5);
            check(!data.toggle(Toggle.FROST_GIANT), "N turns him Asgardian again in the " + id + " look");
            assertNear(loki.getAttributeValue(Attributes.ARMOR), armor, 1.0E-6, "and takes the armor away");
            loki.setHealth(loki.getMaxHealth());
        }
        helper.succeed();
    }

    // ---- Looks ----

    /** Only the main MCU Loki: his Asgardian look, the TVA's, the God of Stories' and the player's own skin. */
    @EnhancedTest(template = ARENA)
    public static void lokiHasOnlyTheMainLokisLooks(GameTestHelper helper) {
        List<String> ids = Variants.of(Power.LOKI).stream().map(Variant::id).toList();
        check(ids.equals(List.of("loki", "tva", "god_of_stories", "own")), "Loki's looks are the main Loki's, not " + ids);

        // A world saved with one of the looks that are gone loads as the default look.
        for (String gone : List.of("sylvie", "classic", "kid", "president")) {
            JsonObject saved = new JsonObject();
            saved.addProperty("power", "loki");
            saved.addProperty("variant", gone);
            PowerData data = PowerData.CODEC.parse(JsonOps.INSTANCE, saved).result().orElseThrow();
            check(Variants.current(data).id().equals("loki"), "a saved " + gone + " look falls back to the God of Mischief");
        }

        // The Change Look key goes round them and back.
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(10.5, FLOOR, 10.5, Power.LOKI);
        PowerData data = Powers.get(loki);
        for (String expected : List.of("tva", "god_of_stories", "own", "loki")) {
            check(Powers.cycleVariant(loki, 1), "the look changes");
            check(Variants.current(data).id().equals(expected), "the next look is " + expected + ", not " + Variants.current(data).id());
        }
        helper.succeed();
    }

    // ---- Sounds ----

    /** Every trick has Loki's own sound (not a vanilla stand-in), played where it happens. */
    @EnhancedTest(template = ARENA)
    public static void everyTrickHasItsOwnSound(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        AABB arena = arena(helper);
        ServerPlayer loki = f.player(4.5, FLOOR, 10.5, Power.LOKI);
        look(loki, EAST, 0.0F);
        TestKit.heardSounds(loki);

        press(loki, 1);
        check(heard(loki, arena, ModSounds.LOKI_DAGGER) == 1, "throwing a dagger plays loki_dagger");

        Zombie model = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 10.5));
        lookAt(loki, model);
        press(loki, 3);
        check(heard(loki, arena, ModSounds.LOKI_SHAPESHIFT) == 1, "taking a shape plays loki_shapeshift");
        look(loki, EAST, -90.0F);
        press(loki, 3);
        check(heard(loki, arena, ModSounds.LOKI_SHAPESHIFT) == 1, "and so does turning back");
        model.discard();

        BlockPos block = new BlockPos(14, 2, 10);
        helper.setBlock(block, Blocks.STONE);
        lookAt(loki, helper.absoluteVec(Vec3.atCenterOf(block)));
        press(loki, 4);
        check(heard(loki, arena, ModSounds.LOKI_VANISH) == 2, "vanishing plays loki_vanish where he goes and where he arrives");

        press(loki, 5);
        check(heard(loki, arena, ModSounds.FROST_GIANT) == 1, "becoming a Frost Giant plays frost_giant");
        press(loki, 5);
        check(heard(loki, arena, ModSounds.FROST_GIANT) == 0, "and turning Asgardian again doesn't");

        press(loki, 2);
        check(heard(loki, arena, ModSounds.LOKI_ILLUSION) == 1, "the duplicates appearing play loki_illusion");
        LokiIllusionEntity copy = LokiIllusionEntity.ownedBy(level, loki.getUUID()).get(0);
        copy.hurt(level.damageSources().generic(), 1.0F);
        check(copy.isRemoved() && heard(loki, arena, ModSounds.LOKI_DISSOLVE) == 1, "a duplicate breaking plays loki_dissolve");
        // The dagger thrown at the start is still in flight; it mustn't leave the arena.
        level.getEntitiesOfClass(LokiDaggerEntity.class, arena).forEach(LokiDaggerEntity::discard);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void losingThePowerEndsEveryTrick(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = f.player(10.5, FLOOR, 10.5, Power.LOKI);
        press(loki, 2);
        press(loki, 5);
        check(!LokiIllusionEntity.ownedBy(level, loki.getUUID()).isEmpty(), "duplicates are out");

        Powers.setPower(loki, Power.NONE);

        check(LokiIllusionEntity.ownedBy(level, loki.getUUID()).isEmpty(), "every duplicate dissolves with the power");
        check(!loki.hasEffect(MobEffects.INVISIBILITY), "Loki is visible again");
        assertNear(loki.getAttributeValue(Attributes.MAX_HEALTH), 20.0, 1.0E-6, "an ordinary body again");
        assertNear(loki.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-6, "with no armor of his body or the Frost Giant's");
        helper.succeed();
    }

    private LokiTests() {
    }
}

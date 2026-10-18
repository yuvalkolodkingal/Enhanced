package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.BifrostKeys;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.MjolnirShatter;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.ability.PhoenixBurst;
import com.kolodkingal.enhanced.ability.ThanosAbilities;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.entity.LokiDaggerEntity;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The gods' abilities in a custom hero's hands (3.4, lane L3): every row of Thor, Loki, the Phoenix and Thanos works on a
 * key that is not its home key (its cooldown on that key, by its strength), its Sneak variant through the binding (not the
 * player's Sneak), its upkeep only for the abilities picked (no Thor body with his lightning alone), its item rule (the
 * hammer needs the weapon in hand and makes him worthy; the Bifrost needs no weapon) and its end when the powers go.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CustomGodsTests {
    private static final String BATCH = "custom_gods";

    /** A husk that stands where it is, with no armour (so a damage reads as it is) and no burning in the sun. */
    private static Zombie husk(GameTestHelper helper, double x, double z) {
        Zombie husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        return husk;
    }

    private static float lost(LivingEntity creature) {
        return creature.getMaxHealth() - creature.getHealth();
    }

    private static long now(ServerPlayer player) {
        return player.level().getGameTime();
    }

    /** A custom hero with one ability at a strength on key {@code column}. */
    private static ServerPlayer one(TestKit.Fixture f, double x, double z, String id, int strength, int column) {
        return CustomKitSupport.hero(f, x, z, CustomKitSupport.def().pick(id, strength).key(column, id).build());
    }

    // ---- Thor ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void thorsLightningOnRIsBareHandedAndScalesWithItsStrength(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer weak = one(f, 3.5, 4.5, "thor.lightning", 2, 1);
        ServerPlayer strong = one(f, 3.5, 16.5, "thor.lightning", 9, 1);
        Zombie weakTarget = husk(helper, 10.5, 4.5);
        Zombie strongTarget = husk(helper, 10.5, 16.5);
        lookAt(weak, weakTarget);
        lookAt(strong, strongTarget);
        CustomKitSupport.press(weak, 1, false, true);
        CustomKitSupport.press(strong, 1, false, true);
        check(Powers.get(weak).is(Live.CALLING_LIGHTNING), "R (not V) calls the lightning, with nothing in hand");
        CustomKitSupport.tickN(weak, 1);
        CustomKitSupport.tickN(strong, 1);
        // One hit of 2 bare-handed, times 1 + 0.5 * (s - 5) / 5: 0.7 at strength 2, 1.4 at strength 9.
        assertNear(lost(weakTarget), 2.0 * 0.7, 0.01, "strength 2 hits for 1.4");
        assertNear(lost(strongTarget), 2.0 * 1.4, 0.01, "strength 9 hits for 2.8");
        CustomKitSupport.press(weak, 1, false, false);
        check(!Powers.get(weak).is(Live.CALLING_LIGHTNING), "letting go of R ends it");
        check(ThorAbilities.isThor(weak), "lightning does not hurt whoever calls it");
        check(!Worthiness.isWorthy(weak), "his lightning alone does not make him worthy of Mjolnir");
        check(weak.getMaxHealth() == 20.0F, "nor gives him Thor's body: " + weak.getMaxHealth());
        CustomKitSupport.press(strong, 1, false, false);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void thorsBodyComesWithItsRowByItsStrength(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer none = one(f, 3.5, 4.5, "thor.lightning", 5, 1);
        ServerPlayer five = CustomKitSupport.hero(f, 3.5, 10.5, CustomKitSupport.def().pick("thor.body", 5).build());
        ServerPlayer ten = CustomKitSupport.hero(f, 3.5, 16.5, CustomKitSupport.def().pick("thor.body", 10).build());
        double armour = none.getAttributeValue(Attributes.ARMOR);
        for (ServerPlayer p : List.of(none, five, ten)) {
            CustomKitSupport.tickN(p, 1);
        }
        check(none.getMaxHealth() == 20.0F, "no body row, no Asgardian body");
        assertNear(five.getMaxHealth(), 30.0, 1.0E-4, "the body at strength 5 is Thor's: +10 health");
        assertNear(five.getAttributeValue(Attributes.ARMOR) - armour, 6.0, 1.0E-4, "and +6 armour");
        assertNear(ten.getMaxHealth(), 35.0, 1.0E-4, "at strength 10 half again: +15 health");
        assertNear(ten.getAttributeValue(Attributes.ARMOR) - armour, 9.0, 1.0E-4, "and +9 armour");
        check(ThorAbilities.isThor(five) && !Worthiness.isWorthy(five), "the body makes him a god of thunder, not worthy");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theGodOfThunderOnRHitsHarderAndArcs(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 8.5, 10.5, "thor.thunder", 5, 1);
        Zombie first = husk(helper, 10.0, 10.5);
        Zombie second = husk(helper, 13.0, 10.5);
        PowerData data = Powers.get(p);
        CustomKitSupport.tap(p, 1, false);
        check(data.toggle(Toggle.GOD_OF_THUNDER), "R unleashes the God of Thunder");
        CustomKitSupport.tickN(p, 1);
        check(p.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(Enhanced.id("thor_god_of_thunder_damage")) != null,
                "he hits harder");
        check(p.getMaxHealth() == 20.0F, "with no Asgardian body");
        lookAt(p, first);
        p.attack(first);
        check(lost(second) > 0.0F, "his blow arcs to the next creature");
        CustomKitSupport.tap(p, 1, false);
        check(!data.toggle(Toggle.GOD_OF_THUNDER), "R again calms him");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void theBifrostOnRNeedsNoWeaponAndItsMainOpensTheScreen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 3.5, 10.5, "thor.bifrost", 5, 1);
        check(BifrostKeys.opensScreen(p, 1, false), "R opens the Bifrost screen (its main), with no weapon");
        check(!BifrostKeys.opensScreen(p, 1, true), "Sneak + R is its variant: to the server");
        check(!BifrostKeys.opensScreen(p, 5, false), "N is not his Bifrost");
        ServerPlayer thor = f.player(3.5, FLOOR, 4.5, Power.THOR);
        check(!BifrostKeys.opensScreen(thor, 5, false), "Thor without his weapon: no screen");
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        check(BifrostKeys.opensScreen(thor, 5, false) && !BifrostKeys.opensScreen(thor, 5, true), "Thor with it: N, not Sneak + N");

        BlockPos spot = new BlockPos(16, 1, 10);
        lookAt(p, helper.absoluteVec(Vec3.atCenterOf(spot).add(0, 0.49, 0)));
        long now = now(p);
        CustomKitSupport.tap(p, 1, true);
        PowerData data = Powers.get(p);
        check(data.is(Live.BIFROST), "Sneak + R: the Bifrost gathers round him");
        check(data.cooldownUntil(1) == now + EnhancedConfig.BIFROST_COOLDOWN.get(), "its cooldown on R");
        f.everyTick(100, tick -> {
            CustomKitSupport.tickN(p, 1);
            if (!data.is(Live.BIFROST)) {
                Vec3 rel = helper.relativeVec(p.position());
                check(Math.abs(rel.x - 16.5) < 0.6 && Math.abs(rel.z - 10.5) < 0.6, "he lands where he looked: " + rel);
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "custom_gods_hammer", timeoutTicks = 200)
    public static void thorsHammerOnGNeedsTheWeaponAndComesBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.MJOLNIR_THROW_RANGE, 12);
        ServerPlayer p = one(f, 3.5, 10.5, "thor.hammer", 5, 2);
        PowerData data = Powers.get(p);
        check(Worthiness.isWorthy(p) && Traits.has(p, Trait.WORTHY), "Thor's hammer makes him worthy");
        CustomKitSupport.tap(p, 2, false);
        check(data.hammerEntityId() == -1 && data.cooldownUntil(2) == 0, "with nothing in hand and none near, nothing is thrown");

        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        Zombie target = husk(helper, 12.5, 10.5);
        lookAt(p, target);
        long now = now(p);
        CustomKitSupport.tap(p, 2, false);
        check(p.getMainHandItem().isEmpty() && helper.getLevel().getEntity(data.hammerEntityId()) instanceof ThrownWeaponEntity,
                "G throws Mjolnir");
        check(data.cooldownUntil(2) == now + ThorAbilities.THROW_COOLDOWN && data.cooldownUntil(1) == 0, "its cooldown on G, not on R");
        boolean[] hit = {false};
        f.everyTick(150, tick -> {
            CustomKitSupport.tickN(p, 1);
            hit[0] |= lost(target) > 0.0F || !target.isAlive();
            if (p.getMainHandItem().is(ModItems.MJOLNIR.get())) {
                check(hit[0], "it hit on the way");
                check(Worthiness.ownerOf(p.getMainHandItem()) != null && Worthiness.ownerOf(p.getMainHandItem()).is(p),
                        "and came back to his hand, his now");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "custom_gods_shatter", timeoutTicks = 200)
    public static void sneakAndThorsHammerShattersMjolnirAndItComesBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 10.5, 10.5, "thor.hammer", 5, 4);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        CustomKitSupport.tap(p, 4, true);
        check(MjolnirShatter.isShattered(p) && p.getMainHandItem().isEmpty(), "Sneak + B (his variant) shatters Mjolnir");
        f.everyTick(180, tick -> {
            CustomKitSupport.tickN(p, 1);
            if (!MjolnirShatter.isShattered(p)) {
                check(p.getMainHandItem().is(ModItems.MJOLNIR.get()), "whole again, in his hand");
                return true;
            }
            return false;
        });
    }

    // ---- Loki ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void lokisDaggersOnVAndTheFanOnSneakV(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer one = one(f, 3.5, 4.5, "loki.daggers", 5, 3);
        ServerPlayer fan = one(f, 3.5, 16.5, "loki.daggers", 5, 3);
        TestKit.look(one, TestKit.EAST, 0.0F);
        TestKit.look(fan, TestKit.EAST, 0.0F);
        long now = now(one);
        CustomKitSupport.tap(one, 3, false);
        CustomKitSupport.tap(fan, 3, true);
        AABB arena = new AABB(helper.absoluteVec(Vec3.ZERO), helper.absoluteVec(new Vec3(21, 8, 21)));
        List<LokiDaggerEntity> daggers = helper.getLevel().getEntitiesOfClass(LokiDaggerEntity.class, arena);
        check(daggers.stream().filter(d -> d.getOwner() == one).count() == 1, "V throws one dagger");
        check(daggers.stream().filter(d -> d.getOwner() == fan).count() == 3, "Sneak + V a fan of three");
        check(Powers.get(one).cooldownUntil(3) == now + LokiAbilities.DAGGER_COOLDOWN, "the dagger's cooldown on V");
        check(Powers.get(fan).cooldownUntil(3) == now + LokiAbilities.FAN_COOLDOWN, "the fan's on V too");
        check(Traits.has(one, Trait.SORCERER), "Loki's tricks make him a sorcerer");
        daggers.forEach(LokiDaggerEntity::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theFrostGiantOnRArmoursHimWithoutLokisBody(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 5.5, 10.5, "loki.frost_giant", 5, 1);
        ServerPlayer other = one(f, 5.5, 4.5, "loki.daggers", 5, 1);
        CustomKitSupport.tickN(p, 1);
        double armour = p.getAttributeValue(Attributes.ARMOR);
        CustomKitSupport.tap(p, 1, false);
        CustomKitSupport.tickN(p, 1);
        check(Powers.get(p).toggle(Toggle.FROST_GIANT) && LokiAbilities.isFrostGiant(p), "R reveals the Frost Giant");
        assertNear(p.getAttributeValue(Attributes.ARMOR) - armour, LokiAbilities.FROST_ARMOR_BONUS, 1.0E-6, "with his hide");
        check(p.getMaxHealth() == 20.0F, "but not Loki's Asgardian health");
        check(!LokiAbilities.isFrostGiant(other), "a custom hero with Loki's daggers only is no Frost Giant");
        CustomKitSupport.tap(p, 1, false);
        CustomKitSupport.tickN(p, 1);
        check(!LokiAbilities.isFrostGiant(p), "R again: himself");
        assertNear(p.getAttributeValue(Attributes.ARMOR), armour, 1.0E-6, "the hide goes with it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void lokisDuplicatesOnBStayWhileHeHasThem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 10.5, 10.5, "loki.duplicates", 5, 4);
        ServerLevel level = helper.getLevel();
        long now = now(p);
        CustomKitSupport.tap(p, 4, false);
        check(Powers.get(p).is(Live.HIDDEN) && LokiAbilities.isHidden(p), "B: he is hidden");
        check(LokiIllusionEntity.ownedBy(level, p.getUUID()).size() == LokiAbilities.DUPLICATES, "behind three copies");
        check(Powers.get(p).cooldownUntil(4) == now + LokiAbilities.DUPLICATES_COOLDOWN, "the cooldown on B");
        f.everyTick(40, tick -> {
            CustomKitSupport.tickN(p, 1);
            if (tick < 25) {
                return false;
            }
            check(LokiIllusionEntity.ownedBy(level, p.getUUID()).size() == LokiAbilities.DUPLICATES,
                    "the copies hold: their caster has the trick");
            Powers.setPower(p, Power.NONE);
            check(!Powers.get(p).is(Live.HIDDEN), "losing the powers ends the trick");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void lokisShapeshiftOnGAndAnotherHerosPoseIsNotEnded(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = CustomKitSupport.hero(f, 4.5, 10.5, CustomKitSupport.def()
                .pick("loki.shapeshift", 5).key(2, "loki.shapeshift").pick("phoenix.mind", 5).key(4, "phoenix.mind").build());
        Zombie zombie = husk(helper, 9.5, 10.5);
        lookAt(p, zombie);
        CustomKitSupport.tap(p, 2, false);
        PowerData data = Powers.get(p);
        check(!data.form().isEmpty() && LokiAbilities.passesAs(p, EntityType.HUSK), "G: he takes the husk's shape");
        check(data.is(Live.CASTING), "with Loki's casting hand");
        CustomKitSupport.tickN(p, 1);
        check(data.is(Live.CASTING), "the Phoenix's tick does not end Loki's pose");
        TestKit.look(p, 0.0F, -90.0F);
        CustomKitSupport.tap(p, 2, false);
        check(data.form().isEmpty(), "looking at nothing, he is himself again");
        helper.succeed();
    }

    // ---- the Phoenix ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void phoenixMindOnBAndMindSenseOnSneakB(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 10.5, 10.5, "phoenix.mind", 5, 4);
        PowerData data = Powers.get(p);
        CustomKitSupport.tap(p, 4, false);
        check(data.toggle(Toggle.MIND_READING), "B: mind reading on");
        check(data.cooldownUntil(4) == 0, "a switch with no cooldown");
        long now = now(p);
        CustomKitSupport.tap(p, 4, true);
        check(data.is(Live.MIND_SENSING), "Sneak + B: Mind Sense");
        check(data.cooldownUntil(4) == now + PhoenixConfig.MIND_SENSE_COOLDOWN.get(), "its cooldown on B");
        check(data.toggle(Toggle.MIND_READING), "the variant leaves the reading as it was");
        check(Traits.has(p, Trait.TELEPATH_STRONG), "a strong telepath");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void phoenixControlOnGHoldsItsHost(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 4.5, 10.5, "phoenix.control", 5, 2);
        Zombie host = husk(helper, 9.5, 10.5);
        lookAt(p, host);
        long now = now(p);
        CustomKitSupport.tap(p, 2, false);
        PowerData data = Powers.get(p);
        check(host.hasEffect(ModEffects.POSSESSED) && PhoenixAbilities.hostsOf(p).equals(List.of(host)), "G takes over the husk");
        check(data.cooldownUntil(2) == now + PhoenixConfig.MIND_CONTROL_COOLDOWN.get() && data.cooldownUntil(4) == 0,
                "the cooldown on G, not on B");
        check(data.is(Live.CASTING), "she casts");
        f.everyTick(40, tick -> {
            CustomKitSupport.tickN(p, 1);
            if (tick < 25) {
                return false;
            }
            check(host.hasEffect(ModEffects.POSSESSED) && PhoenixAbilities.hostsOf(p).equals(List.of(host)),
                    "her steering keeps the husk: she has the ability");
            Powers.setPower(p, Power.NONE);
            check(PhoenixAbilities.hostsOf(p).isEmpty(), "losing the powers frees it");
            return true;
        });
    }

    /** (Alone in its batch: the field reaches 16 blocks, further than its arena.) */
    @GameTest(template = ARENA, batch = "custom_gods_field")
    public static void thePsionicFieldIsSneakOnPhoenixControlsKey(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 4.5, 10.5, "phoenix.control", 5, 2);
        Zombie held = husk(helper, 9.5, 10.5);
        long now = now(p);
        CustomKitSupport.tap(p, 2, true);
        PowerData data = Powers.get(p);
        check(data.is(Live.PHOENIX_PSIONIC) && held.hasEffect(ModEffects.SPELLBOUND), "Sneak + G: the Psionic Field holds the husk");
        check(!held.hasEffect(ModEffects.POSSESSED), "(not Mind Control)");
        check(data.cooldownUntil(2) == now + PhoenixConfig.PSIONIC_COOLDOWN.get(), "its own cooldown, on G");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void thePhoenixBodyKeepsFireOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer body = CustomKitSupport.hero(f, 5.5, 10.5, CustomKitSupport.def().pick("phoenix.body", 5).build());
        ServerPlayer mind = one(f, 15.5, 10.5, "phoenix.mind", 5, 1);
        check(PhoenixAbilities.isFireImmune(body) && Traits.has(body, Trait.FIRE_IMMUNE), "her body: fire cannot hurt him");
        check(!body.hurt(body.damageSources().inFire(), 4.0F), "fire does nothing");
        check(!PhoenixAbilities.isFireImmune(mind) && mind.hurt(mind.damageSources().inFire(), 4.0F), "her mind alone: it burns");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "custom_gods_burst", timeoutTicks = 60)
    public static void thePhoenixBurstOnRChargesHoversAtHisFlightAndGoesOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(PhoenixConfig.BURST_CHARGE_TICKS, 10);
        f.config(PhoenixConfig.BURST_MIN_CHARGE_TICKS, 0);
        f.config(PhoenixConfig.BURST_MIN_RADIUS, 4);
        f.config(PhoenixConfig.BURST_MAX_RADIUS, 6);
        f.config(PhoenixConfig.BURST_FIRE_SECONDS, 0);
        f.config(PhoenixConfig.BURST_KNOCKBACK, 0.0);
        ServerPlayer p = CustomKitSupport.hero(f, 10.5, 10.5, CustomKitSupport.def()
                .pick("phoenix.burst", 5).key(1, "phoenix.burst").pick("flight", 5, "jet").build());
        PowerData data = Powers.get(p);
        CustomKitSupport.tickN(p, 1);
        CustomKitSupport.press(p, 1, false, true);
        check(data.is(Live.PHOENIX_CHARGING) && PhoenixAbilities.isCharging(p), "R held: the charge");
        CustomKitSupport.tickN(p, 1);
        check(p.getAbilities().flying, "it lifts him");
        assertNear(p.getAbilities().getFlyingSpeed(), CustomHeroes.flightSpeed(p) * 0.4, 1.0E-4,
                "and slows his own flying speed (his Flight's), not the Phoenix's");
        CustomKitSupport.tickN(p, 2);
        long now = now(p);
        CustomKitSupport.press(p, 1, false, false);
        check(!data.is(Live.PHOENIX_CHARGING), "let go: it goes off");
        check(PhoenixBurst.active() > 0, "a wave spreads");
        check(data.cooldownUntil(1) == now + PhoenixConfig.BURST_COOLDOWN.get(), "the Burst's cooldown on R");
        helper.succeed();
    }

    // ---- Thanos ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void thePowerStoneOnGWithItsSlamOnSneakAndItsMainAloneOnASneakCell(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer both = one(f, 3.5, 4.5, "thanos.power", 5, 2);
        check(CustomKitSupport.kit(both).sneak(2) != null && CustomKitSupport.kit(both).sneak(2).isAlt(), "the Slam sits on Sneak + G");
        TestKit.look(both, TestKit.EAST, 0.0F);
        long now = now(both);
        CustomKitSupport.tap(both, 2, false);
        PowerData data = Powers.get(both);
        check(data.cooldownUntil(2) == now + ThanosConfig.POWER_BLAST_COOLDOWN.get(), "G: the blast, its cooldown on G");
        data.startCooldown(2, 0L, 0);
        CustomKitSupport.tap(both, 2, true);
        check(data.cooldownUntil(2) == now + ThanosConfig.SLAM_COOLDOWN.get(), "Sneak + G: the slam");

        ServerPlayer main = CustomKitSupport.hero(f, 3.5, 16.5, CustomKitSupport.def()
                .pick("thanos.power", 5, HeroAbility.MAIN).sneak(4, "thanos.power").build());
        TestKit.look(main, TestKit.EAST, 0.0F);
        Zombie target = husk(helper, 9.5, 16.5);
        lookAt(main, target);
        CustomKitSupport.tap(main, 4, true);
        PowerData mainData = Powers.get(main);
        check(mainData.cooldownUntil(4) == now + ThanosConfig.POWER_BLAST_COOLDOWN.get(),
                "the blast alone on Sneak + B: the blast, not the slam (Sneak is not its variant)");
        assertNear(lost(target), ThanosConfig.POWER_BLAST_DAMAGE.get(), 0.01, "the husk takes the blast");
        CustomKitSupport.tap(main, 4, false);
        check(mainData.cooldownUntil(4) == now + ThanosConfig.POWER_BLAST_COOLDOWN.get(), "plain B is free");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theTimeStoneOnRRewindsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 6.5, 6.5, "thanos.time", 5, 1);
        ThanosAbilities.remember(p, 100);
        Vec3 there = p.position();
        Vec3 elsewhere = helper.absoluteVec(new Vec3(15.5, FLOOR, 15.5));
        p.teleportTo(helper.getLevel(), elsewhere.x, elsewhere.y, elsewhere.z, 0.0F, 0.0F);
        long now = now(p);
        CustomKitSupport.tap(p, 1, false);
        assertNear(p.getX(), there.x, 0.05, "R rewinds him to where he was (x)");
        assertNear(p.getZ(), there.z, 0.05, "(z)");
        check(Powers.get(p).cooldownUntil(1) == now + ThanosConfig.REWIND_COOLDOWN.get(), "the Rewind's cooldown on R");
        check(ThanosAbilities.historyTicks(p) == 0, "the history is spent");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSoulStoneOnVDrainsWhileHeldAndTheTitansBodyIsItsRow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = one(f, 4.5, 10.5, "thanos.soul", 5, 3);
        ServerPlayer titan = CustomKitSupport.hero(f, 4.5, 16.5, CustomKitSupport.def().pick("thanos.body", 5).build());
        CustomKitSupport.tickN(p, 1);
        CustomKitSupport.tickN(titan, 1);
        check(p.getMaxHealth() == 20.0F, "a stone alone gives no Titan's body");
        assertNear(titan.getMaxHealth(), 20.0 + ThanosConfig.HEALTH_BONUS.get(), 1.0E-4, "his body row does");
        Zombie target = husk(helper, 9.5, 10.5);
        lookAt(p, target);
        CustomKitSupport.press(p, 3, false, true);
        PowerData data = Powers.get(p);
        check(data.is(Live.SOUL_DRAINING), "V held: the drain");
        long now = now(p);
        CustomKitSupport.press(p, 3, false, false);
        check(!data.is(Live.SOUL_DRAINING), "let go: it ends");
        check(data.cooldownUntil(3) == now + ThanosConfig.DRAIN_COOLDOWN.get(), "its cooldown on V");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void losingThePowersEndsEveryGodsState(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = CustomKitSupport.hero(f, 10.5, 10.5, CustomKitSupport.def()
                .pick("thor.thunder", 5).key(1, "thor.thunder").pick("loki.frost_giant", 5).key(2, "loki.frost_giant")
                .pick("phoenix.mind", 5).key(3, "phoenix.mind").pick("thanos.reality", 5).key(4, "thanos.reality").build());
        CustomKitSupport.tap(p, 1, false);
        CustomKitSupport.tap(p, 2, false);
        CustomKitSupport.tap(p, 3, false);
        CustomKitSupport.tap(p, 4, true);
        PowerData data = Powers.get(p);
        check(data.toggle(Toggle.GOD_OF_THUNDER) && data.toggle(Toggle.FROST_GIANT) && data.toggle(Toggle.MIND_READING)
                && data.toggle(Toggle.REALITY_VEIL), "four gods' switches on, one key each (the Veil on Sneak + B)");
        CustomKitSupport.tickN(p, 1);
        Powers.setPower(p, Power.NONE);
        check(!data.toggle(Toggle.GOD_OF_THUNDER) && !data.toggle(Toggle.FROST_GIANT) && !data.toggle(Toggle.MIND_READING)
                && !data.toggle(Toggle.REALITY_VEIL), "losing the powers turns them all off");
        helper.succeed();
    }

    // ---- with PvP ----

    @GameTest(template = ARENA, batch = "custom_gods_pvp")
    public static void withPvpTheLightningHitsAPlayerAndTheMindHoldsOne(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer caster = CustomKitSupport.hero(f, 3.5, 10.5, CustomKitSupport.def()
                .pick("thor.lightning", 5).key(1, "thor.lightning").pick("phoenix.control", 5).key(2, "phoenix.control").build());
        ServerPlayer victim = f.player(10.5, FLOOR, 10.5);
        lookAt(caster, victim);
        CustomKitSupport.press(caster, 1, false, true);
        CustomKitSupport.tickN(caster, 1);
        CustomKitSupport.press(caster, 1, false, false);
        check(victim.getHealth() < victim.getMaxHealth(), "the lightning hurts a player with PvP on");
        CustomKitSupport.tap(caster, 2, false);
        check(victim.hasEffect(ModEffects.SPELLBOUND), "and his mind control holds one still");
        helper.succeed();
    }

    private CustomGodsTests() {
    }
}

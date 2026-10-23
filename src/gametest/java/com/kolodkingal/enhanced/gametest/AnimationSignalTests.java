package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The signals in the synced data that the heroes' gestures are drawn from. The animation of a gesture starts when it sees
 * an ability's cooldown begin, a flag flip or a state rise, so these tests pin down the server's side of that bargain: an
 * ability starts its cooldown only when it worked (a hand that thrusts at nothing would be a lie), and flight, telekinesis and
 * Thor's weapon say what they are doing in the data every other client sees.
 */
public final class AnimationSignalTests {

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    private static boolean ready(ServerPlayer player, int ability) {
        return Powers.get(player).isReady(ability, player.level().getGameTime());
    }

    /** With nothing to act on, an ability does not start its cooldown (so no gesture plays for a move that did nothing). */
    @EnhancedTest(template = ARENA)
    public static void aMoveWithNothingToActOnStartsNoCooldown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // Alone in the arena, looking straight up: the tests of a batch run side by side, and the creatures of the arena next
        // door must not be in anyone's cone or line.
        ServerPlayer magneto = f.player(10.5, FLOOR, 10.5, Power.MAGNETO);
        ServerPlayer quicksilver = f.player(4.5, FLOOR, 4.5, Power.QUICKSILVER);
        ServerPlayer tommy = f.player(16.5, FLOOR, 16.5, Power.TOMMY);
        ServerPlayer billy = f.player(4.5, FLOOR, 16.5, Power.BILLY);
        ServerPlayer thor = f.player(16.5, FLOOR, 4.5, Power.THOR);
        look(magneto, 0.0F, -90.0F);
        press(magneto, 3);
        check(ready(magneto, 3), "Magneto's strip needs something to strip");
        look(quicksilver, 0.0F, -90.0F);
        press(quicksilver, 3);
        quicksilver.setShiftKeyDown(true);
        press(quicksilver, 3);
        quicksilver.setShiftKeyDown(false);
        check(ready(quicksilver, 3), "Quicksilver's blitz and pickpocket need a target");
        look(tommy, 0.0F, -90.0F);
        tommy.setShiftKeyDown(true);
        press(tommy, 4);
        tommy.setShiftKeyDown(false);
        check(ready(tommy, 4), "Tommy's blitz needs an armed creature near him");
        press(tommy, 5);
        press(tommy, 4);
        check(ready(tommy, 5), "Molecular acceleration needs something to touch");
        check(ready(tommy, 4), "Vibrate needs a wall to go through");
        look(billy, 0.0F, -90.0F);
        press(billy, 4);
        check(ready(billy, 4) && !Powers.get(billy).is(Live.FREEZING), "Freeze needs creatures to freeze");
        press(thor, 1);
        check(ready(thor, 1), "a call with no weapon anywhere starts no cooldown");
        check(Powers.get(thor).hammerEntityId() == -1, "and sends nothing flying");
        helper.succeed();
    }

    /** Moves that always work say so at once: the bolt, the whirlwind, Molecular Acceleration on a creature, the freeze on a crowd. */
    @EnhancedTest(template = ARENA)
    public static void aMoveThatWorksStartsItsCooldownAndRaisesItsState(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(4.5, FLOOR, 10.5, Power.BILLY);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 10.5));
        lookAt(billy, pig);
        press(billy, 1);
        PowerTicker.tick(billy);
        check(!ready(billy, 1) && Powers.get(billy).is(Live.CASTING), "the bolt starts its cooldown and the casting hand");
        press(billy, 4);
        PowerTicker.tick(billy);
        check(!ready(billy, 4) && Powers.get(billy).is(Live.FREEZING), "freezing a creature starts the cooldown and the freezing state");

        ServerPlayer tommy = f.player(4.5, FLOOR, 4.5, Power.TOMMY);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 4.5));
        lookAt(tommy, zombie);
        press(tommy, 5);
        check(!ready(tommy, 5), "Molecular Acceleration on a creature starts its cooldown");
        press(tommy, 3);
        check(!ready(tommy, 3) && Powers.get(tommy).is(Live.WHIRLWIND), "the whirlwind starts its cooldown and its state");
        helper.succeed();
    }

    /** Flight says so in the data on the next server tick, take-off and landing alike. */
    @EnhancedTest(template = ARENA)
    public static void flightIsSyncedOnTheNextTick(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerTicker.tick(wanda);
        PowerData data = Powers.get(wanda);
        check(!data.flying(), "on the ground at first");
        press(wanda, 2);
        check(wanda.getAbilities().flying, "G takes her up");
        PowerTicker.tick(wanda);
        check(data.flying(), "and the data says she flies by the next tick");
        press(wanda, 2);
        PowerTicker.tick(wanda);
        check(!data.flying(), "G again: the data says she has landed");
        helper.succeed();
    }

    /** Telekinesis names what it holds, and stops naming it when it lets go (by a launch or gently). */
    @EnhancedTest(template = ARENA)
    public static void telekinesisNamesWhatItHolds(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(9.5, FLOOR, 10.5));
        lookAt(wanda, pig);
        PowerData data = Powers.get(wanda);
        press(wanda, 3);
        check(data.heldEntityId() == pig.getId(), "she holds the pig");
        press(wanda, 3);
        check(data.heldEntityId() == -1, "pressing again throws it and lets go");
        check(pig.getDeltaMovement().lengthSqr() > 0.25, "the thrown pig is fast: the throw is told from a gentle release by its speed");
        lookAt(wanda, pig);
        pig.setDeltaMovement(Vec3.ZERO);
        press(wanda, 3);
        check(data.heldEntityId() == pig.getId(), "she holds it again");
        wanda.setShiftKeyDown(true);
        press(wanda, 3);
        wanda.setShiftKeyDown(false);
        check(data.heldEntityId() == -1 && pig.getDeltaMovement().lengthSqr() < 0.01, "sneaking sets it down gently: no speed");
        helper.succeed();
    }

    /**
     * Thor's weapon: thrown it is named in flight (and gone from his hand), drawn from the inventory it arrives in his hand,
     * caught it is unnamed. (A batch of its own, and a throw of 12 blocks, as in {@code ThorTests}: at its usual range
     * the weapon struck the arena's wall, which stands on the border of a chunk where nothing ticks in a test.)
     */
    @EnhancedTest(template = ARENA, timeoutTicks = 200, batch = "thor_weapon_signal")
    public static void thorsWeaponSaysWhereItIs(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.MJOLNIR_THROW_RANGE, 12);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        lookAt(thor, zombie);
        PowerData data = Powers.get(thor);
        press(thor, 1);
        check(!ready(thor, 1), "the throw starts its cooldown");
        check(data.hammerEntityId() != -1 && helper.getLevel().getEntity(data.hammerEntityId()) instanceof ThrownWeaponEntity, "the weapon in flight is named");
        check(!thor.getMainHandItem().is(ModItems.MJOLNIR.get()), "and gone from his hand");
        f.everyTick(150, tick -> {
            PowerTicker.tick(thor);
            if (thor.getMainHandItem().is(ModItems.MJOLNIR.get())) {
                check(data.hammerEntityId() == -1, "caught: nothing is named in flight any more");
                return true;
            }
            check(tick < 150, "the weapon never came back: in flight " + helper.getLevel().getEntity(data.hammerEntityId())
                    + ", Thor at " + thor.position() + ", alive " + thor.isAlive() + ", power " + Powers.power(thor));
            return false;
        });
    }

    /** Called from the inventory: the cooldown starts, the weapon is in his hand, and nothing flies. */
    @EnhancedTest(template = ARENA)
    public static void aWeaponDrawnFromTheInventoryStartsTheCooldown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(10.5, FLOOR, 10.5, Power.THOR);
        thor.getInventory().setItem(5, new ItemStack(ModItems.MJOLNIR.get()));
        PowerData data = Powers.get(thor);
        press(thor, 1);
        check(thor.getMainHandItem().is(ModItems.MJOLNIR.get()), "the weapon is in his hand");
        check(!ready(thor, 1), "the cooldown started");
        check(data.hammerEntityId() == -1, "nothing flew");
        helper.succeed();
    }

    /** Loki's two cooldowns tell the gesture apart: a single dagger is short, a fan long; both raise nothing else. */
    @EnhancedTest(template = ARENA)
    public static void lokisDaggersCooldownsTellTheGesturesApart(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(10.5, FLOOR, 10.5, Power.LOKI);
        PowerData data = Powers.get(loki);
        long now = helper.getLevel().getGameTime();
        press(loki, 1);
        check(data.cooldownUntil(1) - now == com.kolodkingal.enhanced.ability.LokiAbilities.DAGGER_COOLDOWN, "one dagger: the short cooldown");
        loki.setShiftKeyDown(true);
        data.startCooldown(1, now - 100, 0);
        press(loki, 1);
        loki.setShiftKeyDown(false);
        check(data.cooldownUntil(1) - now == com.kolodkingal.enhanced.ability.LokiAbilities.FAN_COOLDOWN, "a fan: the long one");
        check(com.kolodkingal.enhanced.ability.LokiAbilities.DAGGER_COOLDOWN <= 15 && com.kolodkingal.enhanced.ability.LokiAbilities.FAN_COOLDOWN > 15,
                "the animation tells them apart at 15 ticks");
        helper.succeed();
    }

    /**
     * 3.4: a custom hero's key that acts moves his cast marker and names its binding (the key and the Sneak cell), so every
     * client plays the gesture of that binding whatever the strength did to the cooldown's length; a press that does
     * nothing (the key cooling down) does not move it.
     */
    @EnhancedTest(template = ARENA)
    public static void aCustomHerosKeyNamesItsBindingInTheCastMarker(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = CustomKitSupport.hero(f, 10.5, 10.5, CustomKitSupport.def()
                .pick("energy_blast", 5, "bolts").key(1, "energy_blast").sneak(1, "energy_blast#burst")
                .pick("thanos.power", 5).key(2, "thanos.power")
                .build());
        look(hero, 0.0F, -90.0F);
        CustomState state = CustomHeroes.state(hero);
        int seq = state.castSeq();
        CustomKitSupport.tap(hero, 1, false);
        check(state.castSeq() == ((seq + 1) & 0xFF) && state.castColumn() == 1 && !state.castSneak(), "R's bolts named R");
        CustomKitSupport.tap(hero, 1, true);
        check(state.castSeq() == ((seq + 1) & 0xFF), "Sneak + R while R cools down does nothing and tells nothing");
        Powers.get(hero).startCooldown(1, 0L, 0);
        CustomKitSupport.tap(hero, 1, true);
        check(state.castSeq() == ((seq + 2) & 0xFF) && state.castColumn() == 1 && state.castSneak(), "Sneak + R's burst named its Sneak cell");
        helper.succeed();
    }

    private AnimationSignalTests() {
    }
}

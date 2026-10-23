package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.block.WitchesRoadDoorBlock;
import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.road.RoadDoors;
import com.kolodkingal.enhanced.road.WitchesRoad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Billy Maximoff (Wiccan) and the Witches' Road. The doors of a dimension all belong to one Road, so each test that walks
 * it has its own batch and starts from an empty Road.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BillyTests {

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    private static BlockPos placeDoor(GameTestHelper helper, int x, int z) {
        BlockPos rel = new BlockPos(x, 2, z);
        helper.setBlock(rel, ModBlocks.WITCHES_ROAD_DOOR.get());
        return helper.absolutePos(rel);
    }

    /** Right-clicks a block the way the game does, with whatever the player holds. */
    private static void use(ServerPlayer player, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, WitchesRoad.DOOR_HEIGHT, 0), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    private static boolean isOpen(ServerLevel level, BlockPos door) {
        BlockState state = level.getBlockState(door);
        return WitchesRoad.isDoor(state) && state.getValue(WitchesRoadDoorBlock.OPEN);
    }

    private static ItemStack genes(int count) {
        return new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), count);
    }

    /** Standing on top of a door, as someone coming up out of the Road does. */
    private static void checkOnDoor(ServerPlayer player, BlockPos door, String what) {
        Vec3 on = Vec3.atBottomCenterOf(door).add(0, WitchesRoad.DOOR_HEIGHT, 0);
        check(player.position().distanceTo(on) < 0.01, what + ": expected " + on + " but was " + player.position());
    }

    // ---- Becoming Billy (or Tommy): the Witches' Road Door ----

    // (3.2: with the trials on, the door asks to walk the Road and the choice comes at its end: these two tests pin the door as it is
    // when witches_road.enabled is off, which is the 3.1 door; RoadDoorTests pins the new one.)
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "witches_road_classic")
    public static void geneAndDoorAwakenBillyOrTommy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(WitchesRoadConfig.ENABLED, false);
        ServerLevel level = helper.getLevel();
        BlockPos door = placeDoor(helper, 10, 10);
        ServerPlayer seeker = f.player(10.5, FLOOR, 12.5);
        seeker.setItemInHand(InteractionHand.MAIN_HAND, genes(2));
        ServerPlayer twin = f.player(12.5, FLOOR, 10.5);
        twin.setItemInHand(InteractionHand.OFF_HAND, genes(1));

        check(!WitchesRoad.choose(seeker, door, Power.BILLY), "nobody can choose before the door opens");
        use(seeker, door);
        use(twin, door);
        check(!isOpen(level, door), "the ritual takes a moment");
        helper.runAfterDelay(WitchesRoad.RITUAL_TICKS + 2, () -> {
            check(isOpen(level, door), "the door opens when the ring of light has run round it");
            check(WitchesRoad.choose(seeker, door, Power.BILLY), "the seeker chooses Billy's road");
            check(Powers.is(seeker, Power.BILLY), "and becomes Billy");
            check(seeker.getMainHandItem().getCount() == 1, "one gene is used up");
            check(WitchesRoad.choose(twin, door, Power.TOMMY), "a gene in the other hand works too");
            check(Powers.is(twin, Power.TOMMY) && twin.getOffhandItem().isEmpty(), "Tommy, and the gene is gone");
            check(!WitchesRoad.choose(seeker, door, Power.TOMMY) && Powers.is(seeker, Power.BILLY), "a hero can't choose again");
            check(seeker.getMainHandItem().getCount() == 1, "nor lose another gene trying");
        });
        helper.runAfterDelay(WitchesRoad.RITUAL_TICKS + WitchesRoad.OPEN_TICKS + 3, () -> {
            check(!isOpen(level, door), "the door closes 5 seconds after it opened");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "witches_road_classic")
    public static void theRoadOpensOnlyForWitchBloodWithoutPowers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(WitchesRoadConfig.ENABLED, false);
        ServerLevel level = helper.getLevel();
        BlockPos door = placeDoor(helper, 10, 10);
        ServerPlayer nobody = f.player(10.5, FLOOR, 12.5);
        ServerPlayer magneto = f.player(8.5, FLOOR, 10.5, Power.MAGNETO);
        magneto.setItemInHand(InteractionHand.MAIN_HAND, genes(1));
        use(nobody, door);
        use(magneto, door);
        helper.runAfterDelay(WitchesRoad.RITUAL_TICKS + 5, () -> {
            check(!isOpen(level, door), "no gene, or a hero already: the door stays shut");

            WitchesRoad.open(level, door);
            check(!WitchesRoad.choose(nobody, door, Power.BILLY) && !Powers.power(nobody).hasPowers(), "without a gene nothing happens");
            check(!WitchesRoad.choose(magneto, door, Power.BILLY) && Powers.is(magneto, Power.MAGNETO), "a hero can't take another road");
            check(magneto.getMainHandItem().getCount() == 1, "and keeps the gene");

            ServerPlayer far = f.player(10.5, FLOOR, 19.5);
            far.setItemInHand(InteractionHand.MAIN_HAND, genes(1));
            check(!WitchesRoad.choose(far, door, Power.TOMMY) && !Powers.power(far).hasPowers(), "9 blocks from the door is too far");
            check(!WitchesRoad.choose(far, door, Power.MAGNETO), "only Billy's or Tommy's road");
            Vec3 near = helper.absoluteVec(new Vec3(10.5, FLOOR, 16.5));
            far.moveTo(near.x, near.y, near.z);
            check(!WitchesRoad.choose(far, door, Power.MAGNETO), "only Billy's or Tommy's road, even close by");
            check(WitchesRoad.choose(far, door, Power.TOMMY) && Powers.is(far, Power.TOMMY), "6 blocks away it works");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 140)
    public static void aDoorSetDownOpenClosesByItself(GameTestHelper helper) {
        BlockPos rel = new BlockPos(10, 2, 10);
        helper.setBlock(rel, ModBlocks.WITCHES_ROAD_DOOR.get().defaultBlockState().setValue(WitchesRoadDoorBlock.OPEN, true));
        BlockPos door = helper.absolutePos(rel);
        check(isOpen(helper.getLevel(), door), "placed open");
        helper.runAfterDelay(WitchesRoad.OPEN_TICKS + 2, () -> {
            check(!isOpen(helper.getLevel(), door), "a door is never left open");
            helper.succeed();
        });
    }

    // ---- Walking the Road ----

    @GameTest(template = ARENA, batch = "witches_road_walk")
    public static void wandaAndBillyWalkTheRoad(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ModAttachments.ROAD_DOORS.set(level, RoadDoors.EMPTY);
        BlockPos a = placeDoor(helper, 3, 3);
        BlockPos b = placeDoor(helper, 17, 3);
        BlockPos c = placeDoor(helper, 17, 17);
        check(WitchesRoad.doors(level).doors().equals(List.of(a, b, c)), "the doors join the Road in the order they are placed");

        ServerPlayer wanda = f.player(3.5, FLOOR, 5.5, Power.SCARLET_WITCH);
        use(wanda, a);
        checkOnDoor(wanda, b, "the Scarlet Witch comes up out of the next door");
        check(isOpen(level, a) && isOpen(level, b), "both doors open");

        ServerPlayer billy = f.player(15.5, FLOOR, 3.5, Power.BILLY);
        use(billy, b);
        checkOnDoor(billy, c, "Billy walks the Road too");

        ServerPlayer secondWitch = f.player(17.5, FLOOR, 15.5, Power.SCARLET_WITCH);
        use(secondWitch, c);
        checkOnDoor(secondWitch, a, "after the last door the Road goes round to the first");

        Vec3 before = wanda.position();
        use(wanda, b);
        check(wanda.position().equals(before), "3 seconds before she can walk it again");

        for (Power power : new Power[]{Power.NONE, Power.QUICKSILVER, Power.TOMMY}) {
            ServerPlayer other = f.player(5.5, FLOOR, 3.5, power);
            Vec3 at = other.position();
            use(other, a);
            check(other.position().equals(at), "the Road doesn't take " + power.getSerializedName());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "witches_road_lost_door")
    public static void aMissingDoorIsSkipped(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ModAttachments.ROAD_DOORS.set(level, RoadDoors.EMPTY);
        BlockPos a = placeDoor(helper, 3, 3);
        // Listed, but the door is gone (lost without the Road noticing).
        BlockPos lost = helper.absolutePos(new BlockPos(10, 2, 3));
        WitchesRoad.addDoor(level, lost);
        BlockPos b = placeDoor(helper, 17, 3);
        check(WitchesRoad.doors(level).doors().equals(List.of(a, lost, b)), "three doors listed");

        ServerPlayer billy = f.player(3.5, FLOOR, 5.5, Power.BILLY);
        use(billy, a);
        checkOnDoor(billy, b, "the missing door is skipped");
        check(WitchesRoad.doors(level).doors().equals(List.of(a, b)), "and dropped from the Road");

        helper.setBlock(new BlockPos(17, 2, 3), Blocks.AIR);
        check(WitchesRoad.doors(level).doors().equals(List.of(a)), "a broken door leaves the Road");
        ServerPlayer wanda = f.player(3.5, FLOOR, 5.5, Power.SCARLET_WITCH);
        Vec3 at = wanda.position();
        use(wanda, a);
        check(wanda.position().equals(at), "the only door leads nowhere");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theDoorIsCraftedAndDropsItself(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("witches_road_door"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:witches_road_door exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack a = new ItemStack(Items.AMETHYST_SHARD);
        ItemStack l = new ItemStack(Items.SOUL_LANTERN);
        ItemStack t = new ItemStack(Items.DARK_OAK_TRAPDOOR);
        ItemStack e = new ItemStack(Items.ENDER_EYE);
        check(recipe.matches(CraftingInput.of(3, 3, List.of(a, l, a, t, e, t, a, l, a)), level), "A L A / T E T / A L A");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.WITCHES_ROAD_DOOR.get()), "it makes a Witches' Road Door");

        BlockPos door = placeDoor(helper, 10, 10);
        BlockState state = level.getBlockState(door);
        check(state.getPistonPushReaction() == PushReaction.BLOCK, "pistons can't push it");
        assertNear(state.getCollisionShape(level, door).max(Direction.Axis.Y), WitchesRoad.DOOR_HEIGHT, 1.0E-6, "you stand on a low slab");
        level.destroyBlock(door, true);
        check(!level.getEntitiesOfClass(ItemEntity.class, new AABB(door).inflate(2),
                item -> item.getItem().is(ModItems.WITCHES_ROAD_DOOR.get())).isEmpty(), "a broken door drops itself");
        helper.succeed();
    }

    // ---- Ability 1: Hex Bolt ----

    @GameTest(template = ARENA)
    public static void hexBoltStrikesAndForks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(4.5, FLOOR, 10.5, Power.BILLY);
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 10.5));
        Pig near = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 13.5));
        Pig far = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 1.5));
        lookAt(billy, target);
        press(billy, 1);

        float damage = EnhancedConfig.HEX_BOLT_DAMAGE.get().floatValue();
        assertNear(target.getMaxHealth() - target.getHealth(), damage, 1.0E-3, "the bolt strikes for its full damage");
        assertNear(near.getMaxHealth() - near.getHealth(), damage * 0.5, 1.0E-3, "and forks to a creature 3 blocks away for half");
        check(far.getHealth() == far.getMaxHealth(), "but not to one 9 blocks away");
        check(target.getLastDamageSource() != null && target.getLastDamageSource().getEntity() == billy, "Billy gets the credit");
        check(!Powers.get(billy).isReady(1, helper.getLevel().getGameTime()), "a short cooldown");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hexBoltForksToTwoAtMost(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(4.5, FLOOR, 10.5, Power.BILLY);
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 10.5));
        List<Pig> around = List.of(
                helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 12.5)),
                helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 8.5)),
                helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(15.5, FLOOR, 10.5)));
        lookAt(billy, target);
        press(billy, 1);
        long hurt = around.stream().filter(p -> p.getHealth() < p.getMaxHealth()).count();
        check(hurt == BillyAbilities.FORK_TARGETS, "the bolt forks to " + BillyAbilities.FORK_TARGETS + " creatures, not " + hurt);
        check(around.get(2).getHealth() == around.get(2).getMaxHealth(), "the farthest one is spared");
        helper.succeed();
    }

    // ---- Ability 3: Telekinesis ----

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void telekinesisCatchesAnArrowInFlight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(4.5, FLOOR, 10.5, Power.BILLY);
        look(billy, EAST, 0.0F);
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        Vec3 start = helper.absoluteVec(new Vec3(14.5, FLOOR + 1.4, 10.5));
        arrow.moveTo(start.x, start.y, start.z);
        arrow.setDeltaMovement(-0.8, 0.0, 0.0);
        helper.getLevel().addFreshEntity(arrow);

        press(billy, 3);
        PowerData data = Powers.get(billy);
        check(Telekinesis.isHolding(billy) && data.heldEntityId() == arrow.getId(), "Billy catches the arrow flying at him");
        f.everyTick(20, tick -> {
            PowerTicker.tick(billy);
            if (tick < 15) {
                return false;
            }
            check(arrow.isAlive() && billy.getHealth() == billy.getMaxHealth(), "it never reaches him");
            check(arrow.getY() > helper.absoluteVec(new Vec3(0, FLOOR + 0.5, 0)).y, "it hangs in the air");
            double distance = arrow.position().distanceTo(billy.getEyePosition());
            check(distance > 2.5 && distance < 12.5, "in front of him, " + distance + " blocks away");
            check(arrow.getDeltaMovement().length() < 0.01, "still");

            press(billy, 3);
            check(!Telekinesis.isHolding(billy), "pressed again, it is launched");
            check(arrow.getDeltaMovement().x > 1.5, "where he looks: " + arrow.getDeltaMovement());
            check(arrow.getOwner() == billy, "and it flies for him now");
            check(!arrow.isNoGravity(), "with its weight back");
            return true;
        });
    }

    // ---- Ability 4: Freeze ----

    @GameTest(template = ARENA)
    public static void freezeSpellbindsTheConeInFront(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(4.5, FLOOR, 10.5, Power.BILLY);
        look(billy, EAST, 0.0F);
        Zombie ahead = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 10.5));
        Zombie aside = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 13.5));
        Zombie behind = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, FLOOR, 10.5));
        Zombie wide = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, FLOOR, 16.5));
        Zombie tooFar = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(19.5, FLOOR, 10.5));
        ServerPlayer soldier = f.player(9.5, FLOOR, 8.5);
        press(billy, 4);

        for (LivingEntity e : List.of(ahead, aside, soldier)) {
            check(e.hasEffect(ModEffects.SPELLBOUND), e.getName().getString() + " in front of him is spellbound");
        }
        assertNear(ahead.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.0, 1.0E-6, "a spellbound creature can't move");
        assertNear(soldier.getAttributeValue(Attributes.JUMP_STRENGTH), 0.0, 1.0E-6, "nor jump");
        assertNear(ahead.getAttributeValue(Attributes.ATTACK_DAMAGE), 0.0, 1.0E-6, "nor hurt anyone");
        check(!behind.hasEffect(ModEffects.SPELLBOUND), "the one behind him is not");
        check(!wide.hasEffect(ModEffects.SPELLBOUND), "nor one outside the 90 degree cone");
        check(!tooFar.hasEffect(ModEffects.SPELLBOUND), "nor one 15 blocks away");
        check(!billy.hasEffect(ModEffects.SPELLBOUND), "nor Billy himself");
        check(!Powers.get(billy).isReady(4, helper.getLevel().getGameTime()), "Freeze has a cooldown");
        helper.succeed();
    }

    /**
     * The spellbound zombie is watched every tick and must never move; the free one has until tick 90 to walk a block
     * (a zombie can take more than two seconds to set off after a target it was handed).
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void aSpellboundZombieDoesNotGoAfterItsTarget(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Zombie frozen = helper.spawn(EntityType.ZOMBIE, new Vec3(4.5, FLOOR, 6.5));
        Zombie free = helper.spawn(EntityType.ZOMBIE, new Vec3(4.5, FLOOR, 14.5));
        Villager frozenPrey = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(12.5, FLOOR, 6.5));
        Villager freePrey = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(12.5, FLOOR, 14.5));
        frozen.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 200));
        frozen.setTarget(frozenPrey);
        free.setTarget(freePrey);
        Vec3 frozenAt = frozen.position();
        Vec3 freeAt = free.position();
        f.everyTick(90, tick -> {
            double moved = frozen.position().subtract(frozenAt).horizontalDistance();
            check(moved < 0.2, "a spellbound zombie stays where it is (moved " + moved + " by tick " + tick + ")");
            if (tick < 40) {
                return false;
            }
            check(frozen.getTarget() == null, "a spellbound zombie forgets its target");
            double walked = free.position().subtract(freeAt).horizontalDistance();
            check(walked > 1.0 || tick < 90, "while a free zombie goes after its own (walked " + walked + ")");
            return walked > 1.0;
        });
    }

    // ---- Abilities 2 and 5, and the crown ----

    @GameTest(template = ARENA)
    public static void flightTheNoiseAndTheCrown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(10.5, FLOOR, 10.5, Power.BILLY);
        PowerData data = Powers.get(billy);
        PowerTicker.tick(billy);
        check(billy.mayFly() && !data.is(Live.CROWNED), "Billy can fly; no crown until he uses his magic");

        press(billy, 5);
        check(data.toggle(Toggle.THE_NOISE), "N: he hears the thoughts around him");
        PowerTicker.tick(billy);
        check(data.is(Live.CROWNED), "his crown forms when he uses his magic");
        press(billy, 5);
        check(!data.toggle(Toggle.THE_NOISE), "N again: quiet");

        press(billy, 2);
        check(billy.getAbilities().flying, "G: he takes off");
        press(billy, 2);
        check(!billy.getAbilities().flying, "G again: he lands");
        helper.succeed();
    }

    private BillyTests() {
    }
}

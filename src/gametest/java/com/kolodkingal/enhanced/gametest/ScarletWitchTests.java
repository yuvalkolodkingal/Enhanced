package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/** The Scarlet Witch: the Mind Stone awakening, her crown and her abilities (the Hex has its own tests). */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ScarletWitchTests {

    // ---- the Mind Stone ----

    /** Has its own batch: the Mind Stone's explosion shouldn't reach other tests. */
    @GameTest(template = ARENA, batch = "mind_stone_death")
    public static void mindStoneKillsPlayerWithoutGene(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));

        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        check(player.isDeadOrDying(), "touching the Mind Stone without the gene kills a Survival player");
        check(Powers.is(player, Power.NONE), "and gives no power");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "mind_stone_death")
    public static void mindStoneSparesCreativePlayers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5, net.minecraft.world.level.GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));

        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        check(player.isAlive() && !player.isDeadOrDying(), "Creative players are never killed by the Mind Stone");
        check(Powers.is(player, Power.NONE), "and get no power without the gene");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mindStoneWithGeneAwakensScarletWitch(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()));

        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        check(player.isAlive() && player.getHealth() == player.getMaxHealth(), "the gene protects the player");
        check(Powers.is(player, Power.SCARLET_WITCH), "gene + Mind Stone awakens the Scarlet Witch, got " + Powers.power(player));
        check(player.getOffhandItem().isEmpty(), "the gene is used up");
        check(player.getInventory().contains(new ItemStack(ModItems.WANDA_CROWN.get())), "Wanda's Crown appears in the inventory");
        check(player.getMainHandItem().is(ModItems.LOKIS_SCEPTER.get()), "the scepter is kept");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mindStoneIsSafeForTheScarletWitch(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        wanda.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));

        wanda.getOffhandItem().use(helper.getLevel(), wanda, InteractionHand.OFF_HAND);

        check(wanda.isAlive() && wanda.getHealth() == wanda.getMaxHealth(), "the Mind Stone recognizes the Scarlet Witch");
        check(Powers.is(wanda, Power.SCARLET_WITCH), "still the Scarlet Witch");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void geneDoesNotBondToMagneto(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(10.5, FLOOR, 10.5, Power.MAGNETO);
        magneto.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        magneto.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), 2));

        magneto.getMainHandItem().use(helper.getLevel(), magneto, InteractionHand.MAIN_HAND);

        check(magneto.isAlive() && magneto.getHealth() == magneto.getMaxHealth(), "nothing happens to Magneto");
        check(Powers.is(magneto, Power.MAGNETO), "powers never stack");
        check(magneto.getOffhandItem().getCount() == 2, "the gene isn't used up");
        check(!magneto.getInventory().contains(new ItemStack(ModItems.WANDA_CROWN.get())), "no crown");
        helper.succeed();
    }

    // ---- Wanda's Crown ----

    @GameTest(template = ARENA)
    public static void crownTransformsOnlyTheScarletWitch(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer other = f.player(15.5, FLOOR, 10.5);
        wanda.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.WANDA_CROWN.get()));
        other.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.WANDA_CROWN.get()));
        wanda.doTick();
        other.doTick();

        assertNear(wanda.getMaxHealth(), 40.0, 1.0E-6, "Wanda's max health with the crown");
        assertNear(wanda.getAttributeValue(Attributes.ARMOR), 20.0, 1.0E-6, "Wanda's armor with the crown (full Diamond)");
        assertNear(wanda.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 8.0, 1.0E-6, "Wanda's toughness with the crown (full Diamond)");
        check(!new ItemStack(ModItems.WANDA_CROWN.get()).isDamageableItem(), "the crown never breaks");

        assertNear(other.getMaxHealth(), 20.0, 1.0E-6, "on anyone else the crown is just a crown (health)");
        assertNear(other.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-6, "on anyone else the crown is just a crown (armor)");
        check(Powers.is(other, Power.NONE), "the crown gives no power");

        wanda.setHealth(40.0F);
        wanda.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        wanda.doTick();
        assertNear(wanda.getMaxHealth(), 20.0, 1.0E-6, "max health without the crown");
        assertNear(wanda.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-6, "armor without the crown");
        assertNear(wanda.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 0.0, 1.0E-6, "toughness without the crown");
        check(wanda.getHealth() <= 20.0F, "health drops to the new maximum");
        helper.succeed();
    }

    // ---- Ability 1: Chaos Energy Beam ----

    @GameTest(template = ARENA)
    public static void chaosBeamHurtsAndNauseates(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 10.5));
        lookAt(wanda, pig);
        float before = pig.getHealth();

        AbilityHandler.onKey(wanda, 1, true);
        PowerTicker.tick(wanda);

        assertNear(before - pig.getHealth(), 5.0, 1.0E-4, "one beam hit = 2.5 hearts");
        check(pig.hasEffect(MobEffects.CONFUSION), "the beam gives Nausea");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chaosBeamIsBlockedByWalls(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 10.5));
        for (int y = 2; y <= 6; y++) {
            for (int z = 8; z <= 12; z++) {
                helper.setBlock(new BlockPos(8, y, z), Blocks.STONE);
            }
        }
        lookAt(wanda, pig);
        float before = pig.getHealth();

        AbilityHandler.onKey(wanda, 1, true);
        for (int i = 0; i < 5; i++) {
            PowerTicker.tick(wanda);
        }

        check(pig.getHealth() == before, "the beam stops at the wall");
        check(!pig.hasEffect(MobEffects.CONFUSION), "no Nausea through a wall");
        helper.succeed();
    }

    // ---- Ability 3: Telekinesis ----

    @GameTest(template = ARENA)
    public static void wandaLiftsCreaturesAndBlocks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(9.5, FLOOR, 10.5));

        lookAt(wanda, pig);
        AbilityHandler.onKey(wanda, 3, true);
        check(Telekinesis.isHolding(wanda) && data.heldEntityId() == pig.getId(), "Wanda lifts any creature (no metal needed)");
        Telekinesis.release(wanda, false);

        BlockPos stone = new BlockPos(7, 2, 16);
        helper.setBlock(stone, Blocks.STONE);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(stone)));
        AbilityHandler.onKey(wanda, 3, true);
        helper.assertBlockNotPresent(Blocks.STONE, stone);
        List<TelekineticBlockEntity> lifted = helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get());
        check(lifted.size() == 1, "the block becomes one flying block, got " + lifted.size());
        check(lifted.get(0).getBlockState().is(Blocks.STONE), "the flying block is stone");
        check(Telekinesis.isHolding(wanda) && data.heldEntityId() == lifted.get(0).getId(), "Wanda holds the torn-out block");
        Telekinesis.release(wanda, false);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wandaCannotLiftObsidianOrBedrock(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        BlockPos obsidian = new BlockPos(8, 2, 7);
        BlockPos bedrock = new BlockPos(8, 2, 14);
        helper.setBlock(obsidian, Blocks.OBSIDIAN);
        helper.setBlock(bedrock, Blocks.BEDROCK);

        for (BlockPos pos : new BlockPos[]{obsidian, bedrock}) {
            lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(pos)));
            AbilityHandler.onKey(wanda, 3, true);
            check(!Telekinesis.isHolding(wanda), "can't lift " + helper.getBlockState(pos));
        }
        helper.assertBlockPresent(Blocks.OBSIDIAN, obsidian);
        helper.assertBlockPresent(Blocks.BEDROCK, bedrock);
        check(helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).isEmpty(), "nothing was torn out");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void thrownBlockShattersAgainstWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        BlockPos stone = new BlockPos(6, 2, 10);
        helper.setBlock(stone, Blocks.STONE);
        buildWall(helper, 15);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(stone)));
        AbilityHandler.onKey(wanda, 3, true);
        check(Telekinesis.isHolding(wanda), "lifted the stone");

        f.everyTick(40, tick -> {
            if (tick <= LAUNCH_TICK) {
                TestKit.look(wanda, TestKit.EAST, -25.0F); // raise it into the air
                PowerTicker.tick(wanda);
                check(helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).size() == 1, "still floating at tick " + tick);
                if (tick == LAUNCH_TICK) {
                    TestKit.look(wanda, TestKit.EAST, 0.0F);
                    AbilityHandler.onKey(wanda, 3, true); // launch at the wall
                    check(!Telekinesis.isHolding(wanda), "launched");
                }
                return false;
            }
            PowerTicker.tick(wanda);
            if (!helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).isEmpty()) {
                return false;
            }
            check(countItems(helper, Items.COBBLESTONE) == 1, "the shattered stone drops its normal loot (cobblestone)");
            helper.assertBlockNotPresent(Blocks.STONE, stone);
            for (int y = 2; y <= 6; y++) {
                helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(15, y, 10));
            }
            return true;
        });
    }

    @GameTest(template = ARENA)
    public static void thrownCreatureSlamsIntoWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(7.5, FLOOR, 10.5));
        buildWall(helper, 15);
        lookAt(wanda, pig);
        AbilityHandler.onKey(wanda, 3, true);
        check(Telekinesis.isHolding(wanda), "lifted the pig");
        float max = pig.getMaxHealth();

        f.everyTick(40, tick -> {
            if (tick < LAUNCH_TICK) {
                TestKit.look(wanda, TestKit.EAST, -25.0F); // raise it into the air
            }
            PowerTicker.tick(wanda);
            if (tick == LAUNCH_TICK) {
                check(Telekinesis.isHolding(wanda) && pig.getY() > helper.absoluteVec(new Vec3(0, FLOOR + 1.0, 0)).y, "the pig floats");
                TestKit.look(wanda, TestKit.EAST, 0.0F);
                AbilityHandler.onKey(wanda, 3, true);
                check(!Telekinesis.isHolding(wanda), "launched");
            }
            return tick > LAUNCH_TICK && (!pig.isAlive() || pig.getHealth() < max);
        });
    }

    private static final int LAUNCH_TICK = 6;

    @GameTest(template = ARENA)
    public static void heldBlockSetDownGentlyBecomesABlockAgain(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        BlockPos dirt = new BlockPos(7, 2, 10);
        helper.setBlock(dirt, Blocks.DIRT);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(dirt)));
        AbilityHandler.onKey(wanda, 3, true);
        check(Telekinesis.isHolding(wanda), "lifted the dirt");

        f.everyTick(60, tick -> {
            if (tick <= 4) {
                TestKit.look(wanda, TestKit.EAST, -20.0F);
                PowerTicker.tick(wanda);
                if (tick == 4) {
                    wanda.setShiftKeyDown(true);
                    AbilityHandler.onKey(wanda, 3, true); // Sneak + V: put it down
                    wanda.setShiftKeyDown(false);
                    check(!Telekinesis.isHolding(wanda), "let go");
                }
                return false;
            }
            if (!helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).isEmpty()) {
                return false;
            }
            int dirtBlocks = 0;
            for (int x = 0; x < 21; x++) {
                for (int z = 0; z < 21; z++) {
                    if (helper.getBlockState(new BlockPos(x, 2, z)).is(Blocks.DIRT)) {
                        dirtBlocks++;
                    }
                }
            }
            check(dirtBlocks == 1, "the block lands like falling sand and becomes a block again, found " + dirtBlocks);
            check(countItems(helper, Items.DIRT) == 0, "it didn't shatter");
            return true;
        });
    }

    private static void buildWall(GameTestHelper helper, int x) {
        for (int y = 2; y <= 8; y++) {
            for (int z = 6; z <= 14; z++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.OBSIDIAN);
            }
        }
    }

    private static int countItems(GameTestHelper helper, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemEntity e : helper.getEntities(EntityType.ITEM)) {
            if (e.getItem().is(item)) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    // ---- Ability 4: Chaos Shield ----

    @GameTest(template = ARENA)
    public static void chaosShieldStopsArrows(GameTestHelper helper) {
        chaosShieldArrowTest(helper, 3.5, true);
    }

    /** Point-blank, without the shield's per-tick scan: the impact/damage hooks must still stop the arrow. */
    @GameTest(template = ARENA)
    public static void chaosShieldStopsPointBlankArrows(GameTestHelper helper) {
        chaosShieldArrowTest(helper, 10.0, false);
    }

    private static void chaosShieldArrowTest(GameTestHelper helper, double shooterX, boolean tickWanda) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        AbilityHandler.onKey(wanda, 4, true);
        check(Powers.get(wanda).chaosShieldActive(), "holding B raises the Chaos Shield");
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(shooterX, FLOOR, 10.5));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no sunburn
        skeleton.setNoAi(true); // only the arrow may hurt Wanda (equipping a skeleton re-adds its melee goal)
        Arrow arrow = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), null);
        Vec3 dir = wanda.position().add(0, 0.9, 0).subtract(arrow.position());
        arrow.shoot(dir.x, dir.y, dir.z, 2.5F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
        float max = wanda.getHealth();

        f.everyTick(20, tick -> {
            if (tickWanda) {
                PowerTicker.tick(wanda);
            }
            check(wanda.getHealth() >= max, "an arrow got through the Chaos Shield");
            return tick >= 10 && arrow.isRemoved();
        });
    }

    @GameTest(template = ARENA)
    public static void chaosShieldReducesExplosionDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer shielded = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer open = f.player(15.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        AbilityHandler.onKey(shielded, 4, true);

        shielded.hurt(helper.getLevel().damageSources().explosion(null, null), 8.0F);
        open.hurt(helper.getLevel().damageSources().explosion(null, null), 8.0F);

        assertNear(20.0F - open.getHealth(), 8.0, 1.0E-4, "explosion damage without the shield");
        assertNear(20.0F - shielded.getHealth(), 2.0, 1.0E-4, "the Chaos Shield lets only 25% of explosion damage through");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chaosShieldNeedsFood(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        wanda.getFoodData().setFoodLevel(0);
        AbilityHandler.onKey(wanda, 4, true);
        check(!Powers.get(wanda).chaosShieldActive(), "no shield on an empty stomach");

        wanda.getFoodData().setFoodLevel(1);
        wanda.getFoodData().setSaturation(0.0F);
        AbilityHandler.onKey(wanda, 4, true);
        check(Powers.get(wanda).chaosShieldActive(), "shield up");
        f.everyTick(60, tick -> {
            PowerTicker.tick(wanda);
            wanda.getFoodData().tick(wanda); // what the player's own tick does with exhaustion
            return !Powers.get(wanda).chaosShieldActive();
        });
    }

    private ScarletWitchTests() {
    }
}

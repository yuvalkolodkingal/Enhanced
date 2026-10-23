package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.MagnetPull;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.Metal;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Magneto's metal (3.1, D-M 2, 3.5, 3.6, 5 and 10; spec 18.3): what is metal ({@link Metal}), what his R catches, what his
 * Armor &amp; Weapon Strip takes and what it refuses, and that a Loki illusion gives nothing to anyone. Batches
 * {@code magneto_metal*} and {@code magneto_loki}.
 */
public final class MagnetoMetalTests {
    private static final String METAL = "magneto_metal";
    private static final String LOKI = "magneto_loki";
    /** Where the creatures that are only asked whether they are metal stand. */
    private static final Vec3 CORNER = new Vec3(16.5, FLOOR, 4.5);

    // ------------------------------------------------------------------------------------------------------------
    // What is metal (M1, M2, M10)

    /**
     * M1. Everything with metal in it is metal: a golem, every minecart, a creature with metal in any slot (only a sword in
     * its hand, a netherite helmet, a crossbow, a trident, a shield on a stand), metal items on the ground, a falling or
     * lifted metal block, an arrow in flight; and nothing else (leather, diamond, wood, Mjolnir, the Gauntlet, Ultron's
     * Core, the heroes' own gear, falling sand, a stuck arrow, a snowball).
     */
    @EnhancedTest(template = ARENA, batch = METAL)
    public static void metalIsEverythingWithMetalInIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = f.player(10.5, FLOOR, 10.5, Power.MAGNETO);

        check(Metal.isMagnetic(helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(3.5, FLOOR, 3.5))), "an Iron Golem");
        for (EntityType<?> cart : List.of(EntityType.MINECART, EntityType.CHEST_MINECART, EntityType.FURNACE_MINECART,
                EntityType.HOPPER_MINECART, EntityType.TNT_MINECART, EntityType.SPAWNER_MINECART, EntityType.COMMAND_BLOCK_MINECART)) {
            check(Metal.isMagnetic(cart.create(level)), "a " + BuiltInRegistries.ENTITY_TYPE.getKey(cart));
        }
        check(Metal.isMagnetic(wearing(helper, EntityType.ZOMBIE, CORNER, EquipmentSlot.MAINHAND, Items.IRON_SWORD)),
                "a zombie with nothing but an iron sword in its hand");
        check(Metal.isMagnetic(wearing(helper, EntityType.SKELETON, CORNER, EquipmentSlot.HEAD, Items.NETHERITE_HELMET)), "a skeleton in a netherite helmet");
        check(Metal.isMagnetic(wearing(helper, EntityType.PIGLIN, CORNER, EquipmentSlot.MAINHAND, Items.GOLDEN_SWORD)), "a piglin with a golden sword");
        check(Metal.isMagnetic(wearing(helper, EntityType.PILLAGER, CORNER, EquipmentSlot.MAINHAND, Items.CROSSBOW)), "a pillager with a crossbow");
        check(Metal.isMagnetic(wearing(helper, EntityType.DROWNED, CORNER, EquipmentSlot.MAINHAND, Items.TRIDENT)), "a drowned with a trident");
        ArmorStand stand = new ArmorStand(level, 0, 0, 0);
        stand.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        check(Metal.isMagnetic(stand), "an armour stand holding a shield");
        for (Item item : List.of(Items.RAW_COPPER, Items.NETHERITE_SCRAP, Items.WAXED_OXIDIZED_CUT_COPPER_SLAB, Items.RAIL,
                Items.LANTERN, Items.GOLDEN_APPLE, ModItems.STORMBREAKER.get(), Items.IRON_ORE)) {
            check(Metal.isMagnetic(dropped(level, item)), "a dropped " + BuiltInRegistries.ITEM.getKey(item));
        }
        FallingBlockEntity anvil = FallingBlockEntity.fall(level, helper.absolutePos(new BlockPos(5, 8, 15)), Blocks.ANVIL.defaultBlockState());
        check(Metal.isMagnetic(anvil), "a falling anvil");
        TelekineticBlockEntity lifted = new TelekineticBlockEntity(level, helper.absolutePos(new BlockPos(6, 4, 6)),
                Blocks.IRON_BLOCK.defaultBlockState(), magneto);
        check(Metal.isMagnetic(lifted), "a lifted iron block");
        Arrow flying = flyingArrow(helper, new Vec3(15.5, FLOOR + 3.0, 15.5));
        check(Metal.isMagnetic(flying), "an arrow in flight");

        check(!Metal.isMagnetic(EntityType.ZOMBIE.create(level)), "not a bare zombie");
        Zombie dressed = EntityType.ZOMBIE.create(level);
        dressed.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        dressed.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        dressed.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        check(!Metal.isMagnetic(dressed), "not a zombie in leather and diamond with a wooden sword");
        for (Item item : List.of(ModItems.MJOLNIR.get(), ModItems.INFINITY_GAUNTLET.get(), ModItems.ULTRON_CORE.get(),
                ModItems.MAGNETO_HELMET.get(), ModItems.WANDA_CROWN.get(), Items.DIAMOND)) {
            check(!Metal.isMagnetic(dropped(level, item)), "not a dropped " + BuiltInRegistries.ITEM.getKey(item));
        }
        FallingBlockEntity sand = FallingBlockEntity.fall(level, helper.absolutePos(new BlockPos(15, 8, 5)), Blocks.SAND.defaultBlockState());
        check(!Metal.isMagnetic(sand), "not falling sand");
        Arrow stuck = flyingArrow(helper, new Vec3(3.5, FLOOR, 15.5));
        stuck.inGround = true;
        check(!Metal.isMagnetic(stuck), "not an arrow stuck in the floor");
        Snowball snowball = new Snowball(level, 0, 0, 0);
        snowball.setDeltaMovement(0.5, 0.0, 0.0);
        check(!Metal.isMagnetic(snowball), "not a snowball in flight");
        for (Entity e : List.of(anvil, sand, flying, stuck)) {
            e.discard();
        }
        helper.succeed();
    }

    /**
     * M2. The heroes of metal: Vision in both looks (unless he phased, when there is nothing to grip), the Ultron hero in
     * every look, Thor while he holds Stormbreaker (never Mjolnir); the heroes' own gear is costume, not metal.
     */
    @EnhancedTest(template = ARENA, batch = METAL)
    public static void heroesOfMetal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(3.5, FLOOR, 3.5, Power.VISION);
        check(Metal.isMagnetic(vision) && Metal.isMetalBody(vision), "Vision is metal, through and through");
        Powers.setVariant(vision, Variants.get(Power.VISION, "white_vision"));
        check(Metal.isMagnetic(vision), "and so is White Vision");
        Powers.get(vision).setVisionPhased(true);
        check(!Metal.isMagnetic(vision) && !Metal.isMetalBody(vision), "a phased Vision is not: there is nothing to grip");
        Powers.get(vision).setVisionPhased(false);

        ServerPlayer ultron = f.player(6.5, FLOOR, 3.5, Power.ULTRON);
        for (String look : List.of("prime", "sentry", "human")) {
            Powers.setVariant(ultron, Variants.get(Power.ULTRON, look));
            check(Metal.isMagnetic(ultron) && Metal.isMetalBody(ultron), "the Ultron hero is vibranium in the " + look + " look");
        }

        ServerPlayer thor = f.player(9.5, FLOOR, 3.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        check(Metal.isMagnetic(thor) && !Metal.isMetalBody(thor), "Thor holding Stormbreaker is metal (in his hand, not his body)");
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        check(!Metal.isMagnetic(thor), "Thor holding Mjolnir is not: Mjolnir answers to no magnetism");

        ServerPlayer wanda = f.player(12.5, FLOOR, 3.5, Power.SCARLET_WITCH);
        wanda.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.WANDA_CROWN.get()));
        check(!Metal.isMagnetic(wanda), "Wanda's Crown is not metal");
        ServerPlayer magneto = f.player(15.5, FLOOR, 3.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        check(!Metal.isMagnetic(magneto), "nor is Magneto's own Helmet");
        helper.succeed();
    }

    /**
     * M10. The tags hold what D-M 10 says: every metal block of 10.2 and the nine ores of 4.3, never a hanging sign, a
     * spawner or a vault; every metal item of 2.1 and none of its "not metal" list; gold through {@code
     * #minecraft:piglin_loved}; netherite armour; the Hex's Vision and the command block minecart.
     */
    @EnhancedTest(template = ARENA, batch = METAL)
    public static void theTagsHoldWhatTheDesignSays(GameTestHelper helper) {
        for (Block block : List.of(Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.COPPER_BLOCK, Blocks.NETHERITE_BLOCK,
                Blocks.RAW_IRON_BLOCK, Blocks.RAW_GOLD_BLOCK, Blocks.RAW_COPPER_BLOCK, Blocks.ANVIL, Blocks.CHIPPED_ANVIL,
                Blocks.DAMAGED_ANVIL, Blocks.CAULDRON, Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON,
                Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL, Blocks.CHAIN, Blocks.IRON_BARS,
                Blocks.IRON_DOOR, Blocks.IRON_TRAPDOOR, Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.BELL, Blocks.HOPPER,
                Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHTNING_ROD,
                Blocks.HEAVY_CORE, Blocks.LODESTONE, Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.TRIPWIRE_HOOK,
                Blocks.SMITHING_TABLE, Blocks.STONECUTTER, Blocks.BLAST_FURNACE, Blocks.CRAFTER)) {
            check(Metal.isMetalBlock(block.defaultBlockState()), BuiltInRegistries.BLOCK.getKey(block) + " is a metal block");
        }
        int copper = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id.getNamespace().equals("minecraft") && id.getPath().contains("copper")) {
                check(Metal.isMetalBlock(block.defaultBlockState()), id + " is a metal block, as every copper block");
                copper++;
            }
        }
        check(copper >= 75, "every copper block was asked (the 72 blocks, two ores and the raw block), not only " + copper);
        for (Block ore : List.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
                Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.NETHER_GOLD_ORE, Blocks.GILDED_BLACKSTONE, Blocks.ANCIENT_DEBRIS)) {
            check(ore.defaultBlockState().is(ModTags.MAGNETIC_ORES) && Metal.isMetalBlock(ore.defaultBlockState()),
                    BuiltInRegistries.BLOCK.getKey(ore) + " is a metal ore");
        }
        for (Block block : List.of(Blocks.OAK_HANGING_SIGN, Blocks.SPAWNER, Blocks.TRIAL_SPAWNER, Blocks.VAULT, Blocks.STONE,
                Blocks.DIAMOND_ORE, Blocks.PISTON_HEAD, Blocks.MOVING_PISTON, Blocks.BEACON, Blocks.RESPAWN_ANCHOR,
                Blocks.ENCHANTING_TABLE, Blocks.CONDUIT, Blocks.COMMAND_BLOCK)) {
            check(!Metal.isMetalBlock(block.defaultBlockState()), BuiltInRegistries.BLOCK.getKey(block) + " is not a metal block");
        }
        check(!Blocks.RAW_IRON_BLOCK.defaultBlockState().is(ModTags.MAGNETIC_ORES) && !Blocks.IRON_BLOCK.defaultBlockState().is(ModTags.MAGNETIC_ORES),
                "a metal block is not an ore");

        for (Item item : List.of(
                // iron
                Items.IRON_INGOT, Items.IRON_NUGGET, Items.RAW_IRON, Items.IRON_BLOCK, Items.RAW_IRON_BLOCK, Items.IRON_ORE,
                Items.DEEPSLATE_IRON_ORE, Items.CHAIN, Items.BUCKET, Items.WATER_BUCKET, Items.AXOLOTL_BUCKET, Items.COD_BUCKET,
                Items.POWDER_SNOW_BUCKET, Items.RAIL, Items.POWERED_RAIL, Items.ANVIL, Items.MINECART, Items.CHEST_MINECART,
                Items.COMMAND_BLOCK_MINECART, Items.COMPASS, Items.RECOVERY_COMPASS, Items.LANTERN, Items.SOUL_LANTERN,
                Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.CROSSBOW, Items.SHIELD, Items.TRIDENT, Items.MACE, Items.HEAVY_CORE,
                Items.PISTON, Items.STICKY_PISTON, Items.TRIPWIRE_HOOK, Items.SMITHING_TABLE, Items.STONECUTTER,
                Items.BLAST_FURNACE, Items.CRAFTER, Items.IRON_HORSE_ARMOR, Items.SHEARS,
                // gold
                Items.GOLDEN_SWORD, Items.GOLDEN_HELMET, Items.GOLD_ORE, Items.NETHER_GOLD_ORE, Items.RAW_GOLD,
                Items.RAW_GOLD_BLOCK, Items.GOLD_BLOCK, Items.GILDED_BLACKSTONE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.BELL,
                Items.CLOCK, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_CARROT, Items.GLISTERING_MELON_SLICE,
                Items.GOLD_NUGGET,
                // copper
                Items.COPPER_INGOT, Items.RAW_COPPER, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.COPPER_BLOCK,
                Items.RAW_COPPER_BLOCK, Items.EXPOSED_CUT_COPPER_STAIRS, Items.WAXED_OXIDIZED_COPPER_BULB, Items.COPPER_DOOR,
                Items.LIGHTNING_ROD, Items.SPYGLASS, Items.BRUSH,
                // netherite
                Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK, Items.NETHERITE_SWORD, Items.NETHERITE_AXE,
                Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_HELMET,
                Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.LODESTONE, Items.ANCIENT_DEBRIS,
                // the mod's own
                ModItems.STORMBREAKER.get(), ModItems.IRON_SHRAPNEL.get(), ModItems.SYNTHEZOID_CORE.get(), ModItems.LOKIS_SCEPTER.get())) {
            check(Metal.isMetalItem(new ItemStack(item)), BuiltInRegistries.ITEM.getKey(item) + " is metal");
        }
        for (Item item : List.of(ModItems.MJOLNIR.get(), ModItems.INFINITY_GAUNTLET.get(), ModItems.ULTRON_CORE.get(),
                ModItems.MAGNETO_HELMET.get(), ModItems.WANDA_CROWN.get(), ModItems.QUICKSILVER_GOGGLES.get(), Items.DIAMOND,
                Items.DIAMOND_SWORD, Items.DIAMOND_CHESTPLATE, Items.LEATHER_HELMET, Items.TURTLE_HELMET, Items.BOW,
                Items.FISHING_ROD, Items.SADDLE, Items.ELYTRA, Items.STONE, Items.WOODEN_SWORD)) {
            check(!Metal.isMetalItem(new ItemStack(item)), BuiltInRegistries.ITEM.getKey(item) + " is not metal");
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.is(net.minecraft.tags.ItemTags.PIGLIN_LOVED)) {
                check(stack.is(ModTags.MAGNETIC_ITEMS), BuiltInRegistries.ITEM.getKey(item) + " (what piglins love is gold) is in enhanced:magnetic");
            }
        }
        for (Item armour : List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS)) {
            check(new ItemStack(armour).is(ModTags.MAGNETIC_ARMOR), BuiltInRegistries.ITEM.getKey(armour) + " is metal armour");
        }
        check(ModEntities.HEX_VISION.get().is(ModTags.MAGNETIC_ENTITIES), "the Hex's Vision is a metal body");
        check(EntityType.COMMAND_BLOCK_MINECART.is(ModTags.MAGNETIC_ENTITIES), "every minecart is metal, the command block one too");
        check(EntityType.ARROW.is(ModTags.METALLIC_PROJECTILES) && EntityType.SPECTRAL_ARROW.is(ModTags.METALLIC_PROJECTILES),
                "arrows are metal projectiles");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // R catches metal in flight (M6, M7)

    /**
     * M6. R catches an arrow in flight: it stops dead and hangs weightless; carried against a wall it stays held although
     * the arrow's own tick may stick it there; R again throws it where he looks, his now, and nobody gains the right to pick
     * it up. A snowball in flight is no metal, and a stuck arrow is not caught.
     */
    @EnhancedTest(template = ARENA, batch = METAL, timeoutTicks = 100)
    public static void magnetoCatchesMetalInFlight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = f.player(10.5, FLOOR, 5.5, Power.MAGNETO);
        PowerData data = Powers.get(magneto);
        for (int z = 1; z <= 19; z++) {
            for (int y = 2; y <= 6; y++) {
                helper.setBlock(new BlockPos(12, y, z), Blocks.STONE); // a wall a block east of him
            }
        }

        Arrow arrow = flyingArrow(helper, new Vec3(10.5, FLOOR + 1.6, 12.5));
        AbstractArrow.Pickup pickup = arrow.pickup;
        lookAt(magneto, arrow);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto) && data.heldEntityId() == arrow.getId(), "R catches the arrow in flight");
        check(arrow.getDeltaMovement().lengthSqr() < 1.0E-6 && arrow.isNoGravity(), "it stops dead and hangs weightless");

        f.everyTick(40, tick -> {
            if (tick <= 10) {
                TestKit.look(magneto, TestKit.EAST, 0.0F); // he turns to the wall a block away
                PowerTicker.tick(magneto);
                check(Telekinesis.isHolding(magneto) && data.heldEntityId() == arrow.getId(),
                        "pressed against the wall it is still held at tick " + tick);
                return false;
            }
            if (tick == 11) {
                TestKit.look(magneto, TestKit.EAST, 40.0F); // into the wall and the floor, so it stays in the arena
                AbilityHandler.onKey(magneto, 1, true);
                Vec3 flight = arrow.getDeltaMovement().normalize();
                check(!Telekinesis.isHolding(magneto), "R again throws it");
                check(flight.dot(magneto.getLookAngle()) > 0.99, "along his look");
                check(arrow.getOwner() == magneto && arrow.pickup == pickup && !arrow.inGround,
                        "it flies for him, and its pickup rule is what it was");
                arrow.discard();

                Snowball snowball = new Snowball(level, 0, 0, 0);
                Vec3 at = helper.absoluteVec(new Vec3(10.5, FLOOR + 1.6, 14.5));
                snowball.setPos(at);
                snowball.setDeltaMovement(0.4, 0.0, 0.0);
                level.addFreshEntity(snowball);
                lookAt(magneto, snowball);
                BossTestSupport.lastActionBarKey(magneto);
                AbilityHandler.onKey(magneto, 1, true);
                check(!Telekinesis.isHolding(magneto), "a snowball is not caught");
                check("message.enhanced.telekinesis.no_metal".equals(BossTestSupport.lastActionBarKey(magneto)), "it is no metal");
                snowball.discard();

                Arrow stuck = flyingArrow(helper, new Vec3(10.5, FLOOR - 0.1, 12.5));
                stuck.setDeltaMovement(Vec3.ZERO);
                stuck.inGround = true;
                lookAt(magneto, stuck);
                AbilityHandler.onKey(magneto, 1, true);
                check(!Telekinesis.isHolding(magneto), "an arrow stuck in the floor is not caught");
                stuck.discard();
                return true;
            }
            return false;
        });
    }

    /**
     * M7. R takes falling and thrown metal blocks: a falling anvil; an iron block Wanda threw, which then glows in his grey;
     * but not a dirt block she threw.
     */
    @EnhancedTest(template = ARENA, batch = METAL)
    public static void magnetoCatchesFallingAndThrownMetalBlocks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = f.player(17.5, FLOOR, 10.5, Power.MAGNETO);
        PowerData data = Powers.get(magneto);

        FallingBlockEntity anvil = FallingBlockEntity.fall(level, helper.absolutePos(new BlockPos(13, 7, 10)), Blocks.ANVIL.defaultBlockState());
        lookAt(magneto, anvil);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto) && data.heldEntityId() == anvil.getId(), "R holds a falling anvil");
        Telekinesis.release(magneto, false);
        anvil.discard();

        ServerPlayer wanda = f.player(3.5, FLOOR, 4.5, Power.SCARLET_WITCH);
        TelekineticBlockEntity iron = thrownByWanda(helper, wanda, new BlockPos(5, 2, 7), Blocks.IRON_BLOCK);
        lookAt(magneto, iron);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto) && data.heldEntityId() == iron.getId(), "R takes the iron block Wanda threw");
        check(iron.getColor() == Power.MAGNETO.color, "and it glows in his iron grey now");
        Telekinesis.release(magneto, false);
        iron.discard();

        TelekineticBlockEntity dirt = thrownByWanda(helper, wanda, new BlockPos(5, 2, 7), Blocks.DIRT);
        lookAt(magneto, dirt);
        AbilityHandler.onKey(magneto, 1, true);
        check(!Telekinesis.isHolding(magneto), "but not a dirt block she threw");
        dirt.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The strip (M9, M45)

    /**
     * M9. Stormbreaker is metal: dropped, R holds it; in a zombie's hand, R holds the zombie; in a player's hand (PvP on),
     * V tears it off and drops it at his feet, unmarked, so he can take it back first.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_metal_pvp")
    public static void stormbreakerIsMetal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        PowerData data = Powers.get(magneto);

        ItemEntity axe = dropped(level, ModItems.STORMBREAKER.get());
        axe.setPos(helper.absoluteVec(new Vec3(9.5, FLOOR, 10.5)));
        level.addFreshEntity(axe);
        lookAt(magneto, axe);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto) && data.heldEntityId() == axe.getId(), "R holds a dropped Stormbreaker");
        Telekinesis.release(magneto, false);
        axe.discard();

        Zombie zombie = wearing(helper, EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 15.5), EquipmentSlot.MAINHAND, ModItems.STORMBREAKER.get());
        lookAt(magneto, zombie);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto) && data.heldEntityId() == zombie.getId(), "R holds a zombie by the Stormbreaker in its hand");
        Telekinesis.release(magneto, false);
        zombie.discard();

        ServerPlayer holder = f.player(9.5, FLOOR, 5.5);
        holder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        lookAt(magneto, holder);
        AbilityHandler.onKey(magneto, 3, true);
        check(holder.getMainHandItem().isEmpty(), "V tears Stormbreaker out of a player's hand");
        List<ItemEntity> fallen = items(helper, ModItems.STORMBREAKER.get());
        check(fallen.size() == 1 && !fallen.get(0).getPersistentData().contains(MagnetoAbilities.STRIPPED_BY_TAG),
                "it falls at his feet, unmarked: the player may take it back first");
        check(!data.isReady(3, level.getGameTime()), "the strip rests");
        helper.succeed();
    }

    /**
     * M45. Gear a creature never drops (drop chance 0) crumbles when V tears it off: no helmet anywhere, only its
     * fragments and the sound of it breaking; the sword at the ordinary chance falls, marked for his pull. The pull does
     * the same to a second such zombie: its sword is his, its helmet crumbles.
     */
    @EnhancedTest(template = ARENA, batch = METAL)
    public static void gearAMobNeverDropsCrumbles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        Zombie zombie = wearing(helper, EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 10.5), EquipmentSlot.HEAD, Items.IRON_HELMET);
        zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        lookAt(magneto, zombie);
        TestKit.sentPackets(magneto);

        AbilityHandler.onKey(magneto, 3, true);
        List<Object> packets = TestKit.sentPackets(magneto);
        check(zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && zombie.getMainHandItem().isEmpty(), "V empties its head and its hand");
        List<ItemEntity> swords = items(helper, Items.IRON_SWORD);
        check(swords.size() == 1, "the sword falls as an item");
        CompoundTag mark = swords.get(0).getPersistentData();
        check(mark.hasUUID(MagnetoAbilities.STRIPPED_BY_TAG) && magneto.getUUID().equals(mark.getUUID(MagnetoAbilities.STRIPPED_BY_TAG)),
                "marked as torn off a creature by him, for his pull");
        check(items(helper, Items.IRON_HELMET).isEmpty() && magneto.getInventory().countItem(Items.IRON_HELMET) == 0,
                "the helmet it would never drop is nowhere: it crumbled");
        check(TestKit.soundsIn(packets).stream().anyMatch(sound -> sound.id().equals(SoundEvents.ITEM_BREAK.getLocation())),
                "it was heard breaking");
        check(TestKit.particlesIn(packets).stream().anyMatch(p -> p.getParticle() instanceof ItemParticleOption fragments
                && fragments.getItem().is(Items.IRON_HELMET)), "into its own fragments");
        check(!Powers.get(magneto).isReady(3, helper.getLevel().getGameTime()), "the strip did something: it rests");

        swords.get(0).discard(); // the strip's sword, out of the way of the pull
        Zombie second = wearing(helper, EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 12.5), EquipmentSlot.HEAD, Items.IRON_HELMET);
        second.setDropChance(EquipmentSlot.HEAD, 0.0F);
        second.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        List<Object> pulled = new ArrayList<>();
        TestKit.sentPackets(magneto);
        MagnetPullTests.sneakR(magneto);
        f.everyTick(20, tick -> {
            PowerTicker.tick(magneto);
            pulled.addAll(TestKit.sentPackets(magneto));
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(second.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && second.getMainHandItem().isEmpty(), "the pull empties its head and its hand");
            check(magneto.getInventory().countItem(Items.IRON_SWORD) == 1, "the sword is his");
            check(items(helper, Items.IRON_HELMET).isEmpty() && magneto.getInventory().countItem(Items.IRON_HELMET) == 0,
                    "and no helmet is anywhere: it crumbled");
            check(TestKit.soundsIn(pulled).stream().anyMatch(sound -> sound.id().equals(SoundEvents.ITEM_BREAK.getLocation())),
                    "it was heard breaking");
            return true;
        });
    }

    /**
     * Spec 5.5 (V at someone's pet: "hostile only"). With PvP off, V at another player's tamed horse in iron horse armour
     * takes nothing: the armour stays on, nothing falls, V does not rest, and he reads that PvP is off. His own horse's
     * armour V tears off, as in 3.0.
     */
    @EnhancedTest(template = ARENA, batch = METAL)
    public static void anotherPlayersHorseKeepsItsArmour(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        PowerData data = Powers.get(magneto);
        ServerPlayer owner = f.player(18.5, FLOOR, 18.5);
        Horse theirs = helper.spawnWithNoFreeWill(EntityType.HORSE, new Vec3(10.5, FLOOR, 7.5));
        theirs.tameWithName(owner);
        theirs.setBodyArmorItem(new ItemStack(Items.IRON_HORSE_ARMOR));
        check(Metal.isRealGear(theirs, EquipmentSlot.BODY), "the horse's armour is real gear, which it would drop");

        lookAt(magneto, theirs);
        BossTestSupport.lastActionBarKey(magneto);
        AbilityHandler.onKey(magneto, 3, true);
        String said = BossTestSupport.lastActionBarKey(magneto);
        check("message.enhanced.disarm.no_pvp".equals(said), "V at another player's horse with PvP off says so, not " + said);
        check(theirs.getBodyArmorItem().is(Items.IRON_HORSE_ARMOR), "the horse keeps its armour");
        check(items(helper, Items.IRON_HORSE_ARMOR).isEmpty(), "and nothing falls");
        check(data.isReady(3, level.getGameTime()), "and V does not rest");

        Horse his = helper.spawnWithNoFreeWill(EntityType.HORSE, new Vec3(10.5, FLOOR, 13.5));
        his.tameWithName(magneto);
        his.setBodyArmorItem(new ItemStack(Items.IRON_HORSE_ARMOR));
        lookAt(magneto, his);
        AbilityHandler.onKey(magneto, 3, true);
        check(his.getBodyArmorItem().isEmpty() && items(helper, Items.IRON_HORSE_ARMOR).size() == 1, "his own horse's armour V tears off");
        check(!data.isReady(3, level.getGameTime()), "and then rests");
        check(theirs.getBodyArmorItem().is(Items.IRON_HORSE_ARMOR), "the other horse still wears its own");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Loki's illusions give nothing (M44, M58)

    /**
     * M44. Loki in netherite, holding his Scepter, casts Duplicates: no copy is metal and R holds none; V at each dissolves
     * it ({@code disarm.illusion}) and rests; afterwards no netherite piece and no Scepter lies anywhere or is in Magneto's
     * inventory, and Loki still wears and holds his own. Cast again, the three copies in reach of the pull dissolve the
     * same way, nothing of theirs is made real, and the pull rests: it did something.
     */
    @EnhancedTest(template = ARENA, batch = LOKI)
    public static void illusionsGiveNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = f.player(10.5, FLOOR, 17.5, Power.LOKI);
        List<Item> set = List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
        List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        for (int i = 0; i < set.size(); i++) {
            loki.setItemSlot(slots.get(i), new ItemStack(set.get(i)));
        }
        loki.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        AbilityHandler.onKey(loki, 2, true);
        List<LokiIllusionEntity> copies = LokiIllusionEntity.ownedBy(level, loki.getUUID());
        check(copies.size() == 3, "three copies stand round him, not " + copies.size());
        ServerPlayer magneto = f.player(10.5, FLOOR, 2.5, Power.MAGNETO);
        PowerData data = Powers.get(magneto);
        for (int i = 0; i < copies.size(); i++) {
            // Out in front of Magneto, so nothing else is in his line.
            copies.get(i).moveTo(helper.absoluteVec(new Vec3(6.5 + 4 * i, FLOOR, 9.5)));
            check(copies.get(i).getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE), "a copy wears his netherite");
            check(!Metal.isMagnetic(copies.get(i)), "but it is not metal");
        }

        lookAt(magneto, copies.get(0));
        AbilityHandler.onKey(magneto, 1, true);
        check(!Telekinesis.isHeld(copies.get(0)), "R holds no copy");
        Telekinesis.release(magneto, false);

        for (LokiIllusionEntity copy : copies) {
            data.startCooldown(3, level.getGameTime(), 0);
            lookAt(magneto, copy);
            BossTestSupport.lastActionBarKey(magneto);
            AbilityHandler.onKey(magneto, 3, true);
            check(copy.isRemoved(), "V dissolves the copy");
            check("message.enhanced.disarm.illusion".equals(BossTestSupport.lastActionBarKey(magneto)), "only an illusion: nothing to take");
            check(!data.isReady(3, level.getGameTime()), "and the strip rests: magnetism saw through the trick");
        }
        for (Item item : List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
                ModItems.LOKIS_SCEPTER.get())) {
            check(items(helper, item).isEmpty() && magneto.getInventory().countItem(item) == 0,
                    "no " + BuiltInRegistries.ITEM.getKey(item) + " was made out of an illusion");
        }
        for (int i = 0; i < set.size(); i++) {
            check(loki.getItemBySlot(slots.get(i)).is(set.get(i)), "Loki still wears his " + BuiltInRegistries.ITEM.getKey(set.get(i)));
        }
        check(loki.getMainHandItem().is(ModItems.LOKIS_SCEPTER.get()), "and holds his Scepter");

        Powers.get(loki).startCooldown(2, level.getGameTime(), 0);
        AbilityHandler.onKey(loki, 2, true);
        List<LokiIllusionEntity> again = LokiIllusionEntity.ownedBy(level, loki.getUUID());
        check(again.size() == 3, "Duplicates again: three new copies, not " + again.size());
        boolean[] pressed = {false};
        f.everyTick(60, tick -> {
            long now = level.getGameTime();
            if (!pressed[0]) {
                if (now - Telekinesis.lastGripEnd(magneto) < EnhancedConfig.TELEKINESIS_SNEAK_GRACE_TICKS.get()) {
                    return false; // R's press a moment ago: Sneak + R waits out the grace after a grip
                }
                for (int i = 0; i < again.size(); i++) {
                    again.get(i).moveTo(helper.absoluteVec(new Vec3(7.5 + 3 * i, FLOOR, 5.5 + (i % 2))));
                    check(again.get(i).getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE), "a copy wears his netherite again");
                }
                MagnetPullTests.sneakR(magneto);
                check(MagnetPull.isPulling(magneto), "Magneto pulls with the three copies in reach");
                pressed[0] = true;
                return false;
            }
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(again.stream().allMatch(Entity::isRemoved), "the pull dissolves every copy");
            for (Item item : List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
                    ModItems.LOKIS_SCEPTER.get())) {
                check(items(helper, item).isEmpty() && magneto.getInventory().countItem(item) == 0,
                        "the pull made no " + BuiltInRegistries.ITEM.getKey(item) + " out of an illusion");
            }
            check(loki.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE), "Loki keeps his own");
            check(!data.isReady(1, now), "and the pull rests: magnetism saw through the trick");
            return true;
        });
    }

    /**
     * M58. Tommy's Blitz passes a Loki illusion by (what it holds is a copy of Loki's): no sword reaches Tommy or the
     * ground; a zombie's sword still comes to him, as in 3.0.
     */
    @EnhancedTest(template = ARENA, batch = LOKI)
    public static void blitzOnAnIllusionGivesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(1.5, FLOOR, 1.5, Power.LOKI);
        loki.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        Vec3 at = helper.absoluteVec(new Vec3(14.5, FLOOR, 14.5));
        LokiIllusionEntity copy = LokiIllusionEntity.conjure(loki, at, at, LokiIllusionEntity.Mode.DECOY, LokiIllusionEntity.DEFAULT_LIFETIME);
        check(copy.getMainHandItem().is(Items.IRON_SWORD), "Loki's copy holds his iron sword");
        ServerPlayer tommy = f.player(17.5, FLOOR, 17.5, Power.TOMMY);
        check(tommy.distanceTo(loki) > 12.0, "the real Loki is out of the Blitz's reach");

        tommy.setShiftKeyDown(true); // (3.3: the Disarm Blitz is Sneak + B)
        AbilityHandler.onKey(tommy, 4, true);
        AbilityHandler.onKey(tommy, 4, false);
        check(tommy.getInventory().countItem(Items.IRON_SWORD) == 0, "the Blitz takes nothing from an illusion");
        check(countDropped(helper, Items.IRON_SWORD) == 0, "and no sword falls");
        check(!copy.isRemoved() && copy.getMainHandItem().is(Items.IRON_SWORD), "the illusion was passed by");

        Zombie zombie = wearing(helper, EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 17.5), EquipmentSlot.MAINHAND, Items.IRON_SWORD);
        AbilityHandler.onKey(tommy, 4, true);
        AbilityHandler.onKey(tommy, 4, false);
        tommy.setShiftKeyDown(false);
        check(zombie.getMainHandItem().isEmpty() && tommy.getInventory().countItem(Items.IRON_SWORD) == 1,
                "a zombie's sword still comes to Tommy");
        check(copy.getMainHandItem().is(Items.IRON_SWORD) && countDropped(helper, Items.IRON_SWORD) == 0,
                "and still nothing from the illusion");
        copy.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A creature of {@code type} spawned at {@code at} (no free will), holding or wearing {@code item} in {@code slot}. */
    private static <T extends Mob> T wearing(GameTestHelper helper, EntityType<T> type, Vec3 at, EquipmentSlot slot, Item item) {
        T mob = helper.spawnWithNoFreeWill(type, at);
        mob.setItemSlot(slot, new ItemStack(item));
        return mob;
    }

    /** An item entity of {@code item}, not added to the level. */
    private static ItemEntity dropped(ServerLevel level, Item item) {
        return new ItemEntity(level, 0, 0, 0, new ItemStack(item));
    }

    /** An arrow added to the level at {@code relative}, flying east (the tests ask about it before it moves). */
    private static Arrow flyingArrow(GameTestHelper helper, Vec3 relative) {
        Vec3 at = helper.absoluteVec(relative);
        Arrow arrow = new Arrow(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.ARROW), null);
        arrow.setDeltaMovement(0.4, 0.0, 0.0);
        helper.getLevel().addFreshEntity(arrow);
        return arrow;
    }

    /** Wanda tears the block at {@code pos} out (it is placed there first) and throws it toward the far side of the arena. */
    private static TelekineticBlockEntity thrownByWanda(GameTestHelper helper, ServerPlayer wanda, BlockPos pos, Block block) {
        helper.setBlock(pos, block);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(pos)));
        AbilityHandler.onKey(wanda, 3, true);
        check(Telekinesis.isHolding(wanda), "Wanda tears the " + BuiltInRegistries.BLOCK.getKey(block) + " out");
        Entity held = helper.getLevel().getEntity(Powers.get(wanda).heldEntityId());
        check(held instanceof TelekineticBlockEntity, "and holds it as a lifted block");
        lookAt(wanda, helper.absoluteVec(new Vec3(10.5, FLOOR, 18.5)));
        AbilityHandler.onKey(wanda, 3, true);
        check(!Telekinesis.isHolding(wanda) && held.getDeltaMovement().lengthSqr() > 0.1, "and throws it");
        return (TelekineticBlockEntity) held;
    }

    /** The item entities of {@code item} in the arena. */
    private static List<ItemEntity> items(GameTestHelper helper, Item item) {
        return helper.getEntities(EntityType.ITEM).stream().filter(e -> e.getItem().is(item)).toList();
    }

    private static int countDropped(GameTestHelper helper, Item item) {
        int n = 0;
        for (ItemEntity e : helper.getEntities(EntityType.ITEM)) {
            if (e.getItem().is(item)) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    private MagnetoMetalTests() {
    }
}

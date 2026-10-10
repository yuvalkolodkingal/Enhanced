package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
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

/**
 * The Scarlet Witch: the Mind Stone and Tesseract awakenings, Loki's Scepter in battle, her crown and her abilities
 * (the Hex has its own tests).
 */
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

    // ---- the Tesseract also makes the Scarlet Witch ----

    @GameTest(template = ARENA)
    public static void tesseractWithGeneAwakensScarletWitch(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()));
        Vec3 start = player.position();
        TestKit.heardSounds(player);

        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        check(Powers.is(player, Power.SCARLET_WITCH), "gene + Tesseract awakens the Scarlet Witch, got " + Powers.power(player));
        check(player.getOffhandItem().isEmpty(), "the gene is used up");
        check(player.getInventory().contains(new ItemStack(ModItems.WANDA_CROWN.get())), "Wanda's Crown appears in the inventory");
        check(player.getMainHandItem().is(ModItems.TESSERACT.get()) && player.getMainHandItem().getCount() == 1, "the Tesseract is kept");
        check(player.getCooldowns().isOnCooldown(ModItems.TESSERACT.get()), "and needs a moment, like the scepter");
        check(player.isAlive() && player.getHealth() == player.getMaxHealth(), "nothing hurts the new Scarlet Witch");
        check(player.position().distanceTo(start) < 1.0E-6, "she is not taken anywhere: this is no escape");
        List<TestKit.HeardSound> sounds = TestKit.heardSounds(player);
        check(sounds.stream().anyMatch(s -> s.id().equals(ModSounds.AWAKEN.getId())), "she awakens with the awakening's sound");
        check(sounds.stream().noneMatch(s -> s.id().equals(ModSounds.TESSERACT.getId())), "and the cube opens no portal");
        helper.succeed();
    }

    /** The gene can be in either hand: the Tesseract just has to be in the other one. */
    @GameTest(template = ARENA)
    public static void tesseractInTheOffHandWorksWithTheGeneInTheMainHand(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), 3));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TESSERACT.get()));

        player.getOffhandItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);

        check(Powers.is(player, Power.SCARLET_WITCH), "the Scarlet Witch awakens with the cube in the off hand");
        check(player.getMainHandItem().is(ModItems.SCARLET_WITCH_GENE.get()) && player.getMainHandItem().getCount() == 2,
                "exactly one gene is used up");
        check(player.getOffhandItem().is(ModItems.TESSERACT.get()), "the Tesseract is kept");
        helper.succeed();
    }

    /** Creative players keep their gene, exactly as with the scepter. */
    @GameTest(template = ARENA)
    public static void tesseractAwakeningUsesTheGeneTheSameWayAsTheScepter(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer creative = f.player(5.5, FLOOR, 10.5, net.minecraft.world.level.GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        creative.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()));
        ServerPlayer viaScepter = f.player(15.5, FLOOR, 10.5, net.minecraft.world.level.GameType.CREATIVE);
        viaScepter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        viaScepter.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()));

        creative.getMainHandItem().use(helper.getLevel(), creative, InteractionHand.MAIN_HAND);
        viaScepter.getMainHandItem().use(helper.getLevel(), viaScepter, InteractionHand.MAIN_HAND);

        check(Powers.is(creative, Power.SCARLET_WITCH) && Powers.is(viaScepter, Power.SCARLET_WITCH), "both awaken the Scarlet Witch");
        check(creative.getOffhandItem().getCount() == viaScepter.getOffhandItem().getCount(),
                "and Creative keeps the gene either way (" + creative.getOffhandItem().getCount() + " vs " + viaScepter.getOffhandItem().getCount() + ")");
        helper.succeed();
    }

    /** Without the gene (or with something else in the other hand) the Tesseract keeps making Loki. */
    @GameTest(template = ARENA)
    public static void tesseractWithoutTheGeneStillMakesLoki(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SYNTHEZOID_CORE.get()));

        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        check(Powers.is(player, Power.LOKI), "the Tesseract with no gene makes Loki, got " + Powers.power(player));
        check(player.getOffhandItem().is(ModItems.SYNTHEZOID_CORE.get()), "and uses up nothing else");
        check(player.getMainHandItem().is(ModItems.TESSERACT.get()), "the Tesseract is kept");
        check(!player.getInventory().contains(new ItemStack(ModItems.WANDA_CROWN.get())), "no crown");
        helper.succeed();
    }

    /** A hero who uses the Tesseract is taken where they look, as before, whatever they hold in the other hand. */
    @GameTest(template = ARENA)
    public static void tesseractStillJumpsAHeroHoldingAGene(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        magneto.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        magneto.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), 2));
        BlockPos block = new BlockPos(12, 2, 10);
        helper.setBlock(block, Blocks.STONE);
        lookAt(magneto, helper.absoluteVec(Vec3.atCenterOf(block)));

        magneto.getMainHandItem().use(helper.getLevel(), magneto, InteractionHand.MAIN_HAND);

        Vec3 rel = helper.relativeVec(magneto.position());
        check(Math.abs(rel.x - 12.5) < 0.01 && Math.abs(rel.z - 10.5) < 0.01, "he is taken to the block he looks at, not " + rel);
        check(Powers.is(magneto, Power.MAGNETO), "he stays Magneto: powers never stack");
        check(magneto.getOffhandItem().getCount() == 2, "the gene isn't used up");
        check(!magneto.getInventory().contains(new ItemStack(ModItems.WANDA_CROWN.get())), "no crown");
        helper.succeed();
    }

    // ---- Loki's Scepter confuses whoever it hits ----

    private static ServerPlayer withScepter(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = f.player(x, FLOOR, z);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        return player;
    }

    /**
     * A zombie that is after a player is hit by the scepter: it forgets the player, can't pick anyone (or the same
     * player) again for as long as the effect lasts, and stumbles about; then it goes back to hunting.
     */
    @GameTest(template = ARENA, batch = "scepter_confusion", timeoutTicks = 200)
    public static void scepterMakesAZombieForgetItsTargetAndStumble(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SCEPTER_CONFUSION_TICKS, 60);
        ServerPlayer attacker = withScepter(f, 10.5, 10.5);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no sunburn
        zombie.setTarget(attacker);
        check(zombie.getTarget() == attacker, "the zombie is after the player");

        attacker.attack(zombie);

        MobEffectInstance confused = zombie.getEffect(ModEffects.CONFUSED);
        check(confused != null, "a melee hit with the scepter confuses the zombie");
        check(confused.getDuration() >= 59 && confused.getDuration() <= 60, "for the configured time, not " + confused.getDuration());
        boolean[] wasFree = {false};
        double[] walked = {0.0};
        Vec3[] last = {zombie.position()};
        f.everyTick(150, tick -> {
            double step = zombie.position().subtract(last[0]).horizontalDistance();
            last[0] = zombie.position();
            if (tick >= 5 && zombie.hasEffect(ModEffects.CONFUSED)) {
                check(zombie.getTarget() == null, "a confused zombie has no target (tick " + tick + ")");
                if (tick > 8) {
                    walked[0] += step;
                }
                if (tick % 10 == 0) {
                    zombie.setTarget(attacker);
                    check(zombie.getTarget() == null, "and can't pick one: setTarget is refused (tick " + tick + ")");
                }
            }
            if (!zombie.hasEffect(ModEffects.CONFUSED) && !wasFree[0]) {
                wasFree[0] = true;
                check(tick >= 58 && tick <= 66, "the effect ended after about 3 seconds, at tick " + tick);
                check(walked[0] > 1.5, "it stumbled about while confused (walked " + walked[0] + " blocks)");
            }
            // Once free it is a zombie again, and goes after the player it sees.
            return wasFree[0] && zombie.getTarget() == attacker;
        });
    }

    /** A hit player gets Nausea for 8 seconds and Slowness I for 3; Magneto's helmet turns the Nausea away. */
    @GameTest(template = ARENA)
    public static void scepterGivesAPlayerNauseaAndSlowness(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer attacker = withScepter(f, 10.5, 10.5);
        ServerPlayer victim = f.player(11.5, FLOOR, 10.5);
        ServerPlayer helmeted = f.player(9.5, FLOOR, 10.5);
        helmeted.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        check(EnhancedConfig.SCEPTER_NAUSEA_TICKS.get() == 160 && EnhancedConfig.SCEPTER_SLOWNESS_TICKS.get() == 60
                && EnhancedConfig.SCEPTER_CONFUSION_TICKS.get() == 120, "8 seconds of Nausea, 3 of Slowness, 6 of confusion");

        attacker.attack(victim);

        MobEffectInstance nausea = victim.getEffect(MobEffects.CONFUSION);
        check(nausea != null && nausea.getDuration() >= 159 && nausea.getAmplifier() == 0, "the player gets Nausea for 8 seconds, got " + nausea);
        MobEffectInstance slow = victim.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(slow != null && slow.getDuration() >= 59 && slow.getDuration() <= 60 && slow.getAmplifier() == 0,
                "and Slowness I for 3 seconds, got " + slow);
        check(!victim.hasEffect(ModEffects.CONFUSED), "players don't get the creature effect");

        attacker.attack(helmeted);
        check(!helmeted.hasEffect(MobEffects.CONFUSION), "Magneto's helmet still turns Nausea away");
        check(helmeted.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "but not the Slowness");
        helper.succeed();
    }

    /** Each of the three durations can be set to 0, which turns that effect off. */
    @GameTest(template = ARENA, batch = "scepter_confusion_off")
    public static void scepterConfusionCanBeTurnedOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(EnhancedConfig.SCEPTER_CONFUSION_TICKS, 0);
        f.config(EnhancedConfig.SCEPTER_NAUSEA_TICKS, 0);
        f.config(EnhancedConfig.SCEPTER_SLOWNESS_TICKS, 0);
        ServerPlayer attacker = withScepter(f, 10.5, 10.5);
        ServerPlayer victim = f.player(11.5, FLOOR, 10.5);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 10.5));
        zombie.setNoAi(true);
        float health = zombie.getHealth();

        attacker.attack(victim);
        attacker.attack(zombie);

        check(zombie.getHealth() < health, "the scepter still hurts");
        check(victim.getActiveEffects().isEmpty(), "with 0 ticks a player gets neither Nausea nor Slowness, got " + victim.getActiveEffects());
        check(zombie.getActiveEffects().isEmpty(), "and a creature isn't confused, got " + zombie.getActiveEffects());
        helper.succeed();
    }

    /** A blow that kills leaves nothing behind: no effect on the corpse, no chime. */
    @GameTest(template = ARENA)
    public static void scepterLeavesAKilledCreatureAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer attacker = withScepter(f, 10.5, 10.5);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 10.5));
        zombie.setNoAi(true);
        zombie.setHealth(0.1F); // (the scepter's own damage only counts once the player has ticked with it in hand)
        TestKit.heardSounds(attacker);

        attacker.attack(zombie);

        check(!zombie.isAlive(), "the blow kills the zombie");
        check(!zombie.hasEffect(ModEffects.CONFUSED), "so it isn't confused");
        check(TestKit.heardSounds(attacker).stream().noneMatch(s -> s.id().equals(SoundEvents.AMETHYST_BLOCK_CHIME.getLocation())),
                "and the Mind Stone doesn't chime over it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scepterCannotConfuseTheBosses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (EntityType<?> boss : List.of(EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.WARDEN)) {
            check(boss.is(ModTags.CONFUSED_IMMUNE), BuiltInRegistries.ENTITY_TYPE.getKey(boss) + " is immune to confusion");
        }
        check(!EntityType.ZOMBIE.is(ModTags.CONFUSED_IMMUNE) && !EntityType.VILLAGER.is(ModTags.CONFUSED_IMMUNE), "ordinary creatures are not");

        ServerPlayer attacker = withScepter(f, 10.5, 10.5);
        Warden warden = helper.spawn(EntityType.WARDEN, new Vec3(11.5, FLOOR, 10.5));
        warden.setNoAi(true);
        float health = warden.getHealth();

        attacker.attack(warden);

        check(warden.getHealth() < health, "the scepter does hurt the warden");
        check(!warden.hasEffect(ModEffects.CONFUSED), "but a boss-type mob is not confused by it");
        check(!warden.addEffect(new MobEffectInstance(ModEffects.CONFUSED, 100)), "whatever applies the effect");
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

    /** The whole chain behind "Magneto takes Wanda's attacks in full": a block she hurls at him hits as if he had no armor. */
    @GameTest(template = ARENA)
    public static void wandaHurlsABlockAtMagneto(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer magneto = f.player(12.5, FLOOR, 10.5);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        magneto.doTick();
        check(Powers.is(magneto, Power.MAGNETO), "helmet awakens Magneto");
        check(magneto.getAttributeValue(Attributes.ARMOR) >= 20.0, "Magneto's suit protects like netherite");
        BlockPos stone = new BlockPos(6, 2, 10);
        helper.setBlock(stone, Blocks.STONE);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(stone)));
        AbilityHandler.onKey(wanda, 3, true);
        check(Telekinesis.isHolding(wanda), "lifted the stone");
        float before = magneto.getHealth();

        f.everyTick(40, tick -> {
            if (tick < LAUNCH_TICK) {
                TestKit.look(wanda, TestKit.EAST, -10.0F);
            }
            PowerTicker.tick(wanda);
            if (tick == LAUNCH_TICK) {
                lookAt(wanda, magneto);
                AbilityHandler.onKey(wanda, 3, true);
                check(!Telekinesis.isHolding(wanda), "launched at Magneto");
            }
            if (tick > LAUNCH_TICK && magneto.getHealth() < before) {
                DamageSource source = magneto.getLastDamageSource();
                check(source != null && source.is(ModDamageTypes.TELEKINETIC_SLAM) && source.getEntity() == wanda,
                        "the block hit him and the hit is blamed on Wanda (" + source + ")");
                // Netherite-level armor would cut it to about a quarter; Wanda's attacks ignore it.
                float taken = before - magneto.getHealth();
                check(taken >= TelekineticBlockEntity.BASE_DAMAGE * 0.75F - 0.01F, "the hit lands in full (took " + taken + ")");
                return true;
            }
            return false;
        });
    }

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

    /** Presses B; when the wind-up is over (and the shield complete) runs {@code then}. */
    private static void raiseShield(TestKit.Fixture f, ServerPlayer wanda, Runnable then) {
        AbilityHandler.onKey(wanda, 4, true);
        check(Powers.get(wanda).chaosShieldActive(), "pressing B starts the Chaos Shield");
        f.helper().runAfterDelay(EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get() + 1, () -> {
            check(WandaAbilities.hasShieldUp(wanda), "the Chaos Shield is complete when its wind-up is over");
            then.run();
        });
    }

    private static void explode(GameTestHelper helper, ServerPlayer victim, float damage) {
        victim.invulnerableTime = 0;
        victim.hurt(helper.getLevel().damageSources().explosion(null, null), damage);
    }

    /** An arrow's blow, as if it had struck {@code victim} (the arrow itself never exists). */
    private static void arrowBlow(GameTestHelper helper, Skeleton shooter, ServerPlayer victim, float damage) {
        Arrow arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
        victim.invulnerableTime = 0;
        victim.hurt(helper.getLevel().damageSources().arrow(arrow, shooter), damage);
    }

    private static Skeleton harmlessSkeleton(GameTestHelper helper, Vec3 at) {
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, at);
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no sunburn
        skeleton.setNoAi(true); // only the arrow may hurt Wanda (equipping a skeleton re-adds its melee goal)
        return skeleton;
    }

    private static long cooldownLeft(ServerPlayer wanda, GameTestHelper helper) {
        return Powers.get(wanda).cooldownUntil(4) - helper.getLevel().getGameTime();
    }

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
        raiseShield(f, wanda, () -> {
            Skeleton skeleton = harmlessSkeleton(helper, new Vec3(shooterX, FLOOR, 10.5));
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
                if (tick >= 10 && arrow.isRemoved()) {
                    float left = Powers.get(wanda).shieldStrength();
                    check(left < WandaAbilities.maxShieldStrength() && left > WandaAbilities.maxShieldStrength() - 10.0F,
                            "the arrow wore the shield down by what it would have done, not " + left + " left of 30");
                    check(Powers.get(wanda).chaosShieldActive(), "one arrow doesn't break it");
                    return true;
                }
                return false;
            });
        });
    }

    /**
     * Mjolnir and Stormbreaker are one of a kind (the hammer exists only inside the thrown entity): a Chaos Shield
     * that turned one to sparks would destroy it for good. It throws it back to Thor instead, and pays what the blow
     * would have cost.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void chaosShieldTurnsAThrownWeaponBackToThor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        PowerTicker.tick(thor);
        AbilityHandler.onKey(wanda, 4, true);
        boolean[] thrown = {false};
        f.everyTick(100, tick -> {
            PowerTicker.tick(thor);
            PowerTicker.tick(wanda);
            if (!thrown[0]) {
                if (WandaAbilities.hasShieldUp(wanda)) {
                    lookAt(thor, wanda);
                    AbilityHandler.onKey(thor, 1, true);
                    AbilityHandler.onKey(thor, 1, false);
                    check(thor.getMainHandItem().isEmpty(), "Stormbreaker is in flight");
                    thrown[0] = true;
                }
                return false;
            }
            if (thor.getMainHandItem().is(ModItems.STORMBREAKER.get())) {
                assertNear(wanda.getHealth(), 20.0, 1.0E-4, "the axe never touched her");
                assertNear(Powers.get(wanda).shieldStrength(), 30.0 - EnhancedConfig.STORMBREAKER_THROW_DAMAGE.get(), 1.0E-4,
                        "the shield paid what the throw would have done (Stormbreaker's, not Mjolnir's)");
                check(Powers.get(wanda).chaosShieldActive(), "and held");
                check(helper.getEntities(ModEntities.THROWN_WEAPON.get()).isEmpty(), "nothing is left in flight");
                return true;
            }
            check(tick < 90, "Stormbreaker was destroyed by the shield instead of coming back to Thor");
            return false;
        });
    }

    // ---- the Chaos Shield against Thor ----

    /** A husk (it does not burn in the sun) that takes damage as written: no armour to soften it. */
    private static Zombie bareHusk(GameTestHelper helper, double x, double z) {
        Zombie husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        return husk;
    }

    /** Thor holds V for a single tick (his beam is free to hit at once, whatever tick it is) and lets go. */
    private static void beamOnce(ServerPlayer thor) {
        ThorAbilities.forgetBeamPace(thor);
        AbilityHandler.onKey(thor, 3, true);
        PowerTicker.tick(thor);
        AbilityHandler.onKey(thor, 3, false);
    }

    /**
     * (These three have batches of their own: they need PvP on for several ticks, and the tests of a batch run side by
     * side, some of them switching PvP back off when they end.)
     *
     * A complete Chaos Shield is solid to Thor's lightning beam as it is to an arrow: the beam ends on the sphere, Wanda
     * is not touched, nothing beyond is hit and the shield pays the beam's damage (the weapon and the God of Thunder make
     * it more). A shield that is still forming stops nothing.
     */
    @GameTest(template = ARENA, batch = "chaos_shield_beam_pvp", timeoutTicks = 100)
    public static void chaosShieldStopsThorsLightningBeamAtItsSurfaceAndIsWornDownByIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        // Beyond her, a little to the side: the line to it passes through the sphere without touching Wanda herself.
        Zombie behind = bareHusk(helper, 17.5, 11.5);
        Zombie beside = bareHusk(helper, 12.5, 14.5);
        PowerData data = Powers.get(wanda);
        lookAt(thor, wanda);
        AbilityHandler.onKey(wanda, 4, true);
        beamOnce(thor);
        assertNear(20.0F - wanda.getHealth(), 2.0, 1.0E-4, "a shield that is still forming stops nothing: the beam hurts her");
        assertNear(data.shieldStrength(), 30.0, 1.0E-4, "and costs it nothing");
        wanda.setHealth(20.0F);

        helper.runAfterDelay(EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get() + 1, () -> {
            check(WandaAbilities.hasShieldUp(wanda), "the shield is complete");
            lookAt(thor, behind);
            beamOnce(thor);
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "the beam doesn't hurt her");
            assertNear(data.shieldStrength(), 28.0, 1.0E-4, "the beam's 2 points wore the shield down, though it never touched her");
            check(behind.getHealth() == behind.getMaxHealth(), "the creature beyond the sphere is not hit");
            check(data.chaosShieldActive(), "and one hit doesn't break it");
            lookAt(thor, wanda);

            // Where the beam ends (the same call the client draws it with): on the sphere, on the side Thor is on.
            Vec3 center = wanda.position().add(0, wanda.getBbHeight() * 0.5, 0);
            WandaAbilities.ShieldStop stop = WandaAbilities.shieldAcross(helper.getLevel(), thor, thor.getEyePosition(), center, 1.0F);
            check(stop != null && stop.owner() == wanda, "the beam runs into her shield");
            assertNear(stop.point().distanceTo(center), WandaAbilities.SHIELD_RADIUS, 1.0E-6, "and ends on its surface");
            check(stop.point().x < center.x, "on the side of Thor");
            check(WandaAbilities.shieldAcross(helper.getLevel(), thor, thor.getEyePosition(), center.subtract(5, 0, 0), 1.0F) == null,
                    "a beam that stops short of the shield doesn't touch it");
            check(WandaAbilities.shieldAcross(helper.getLevel(), wanda, thor.getEyePosition(), center, 1.0F) == null,
                    "and nobody's own shield stops their own beam");

            // The weapon doubles it, the God of Thunder adds half again: 6 points, and no lightning leaps past the shield.
            thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
            AbilityHandler.onKey(thor, 4, true);
            check(Powers.get(thor).toggle(Toggle.GOD_OF_THUNDER), "the God of Thunder is unleashed");
            beamOnce(thor);
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "still not a scratch");
            assertNear(data.shieldStrength(), 22.0, 1.0E-4, "Mjolnir and the God of Thunder: 2 x 2 x 1.5 = 6 points");
            check(beside.getHealth() == beside.getMaxHealth(), "no lightning arcs to the creature beside her");
            helper.succeed();
        });
    }

    /**
     * A beam from inside the sphere is not stopped at its surface, but what hits Wanda herself still never gets through,
     * and neither does the lightning that leaps on from what Thor strikes.
     */
    @GameTest(template = ARENA, batch = "chaos_shield_point_blank_pvp", timeoutTicks = 100)
    public static void chaosShieldStopsAPointBlankBeamFromInsideItToo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer thor = f.player(11.0, FLOOR, 10.5, Power.THOR);
        PowerData data = Powers.get(wanda);
        raiseShield(f, wanda, () -> {
            lookAt(thor, wanda);
            Vec3 center = wanda.position().add(0, wanda.getBbHeight() * 0.5, 0);
            check(thor.getEyePosition().distanceTo(center) < WandaAbilities.SHIELD_RADIUS, "sanity: Thor stands inside her shield");
            check(WandaAbilities.shieldAcross(helper.getLevel(), thor, thor.getEyePosition(), center, 1.0F) == null,
                    "so the beam is not stopped at the surface");
            beamOnce(thor);
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "but it doesn't hurt her");
            assertNear(data.shieldStrength(), 28.0, 1.0E-4, "and wears the shield down all the same");
            wanda.hurt(ModDamageTypes.source(helper.getLevel(), DamageTypes.LIGHTNING_BOLT, thor, thor), 4.0F);
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "lightning that leaps from Thor's blow is stopped too");
            assertNear(data.shieldStrength(), 24.0, 1.0E-4, "and costs 4 points");
            // The sky's own lightning is nobody's to stop.
            wanda.invulnerableTime = 0;
            wanda.hurt(helper.getLevel().damageSources().lightningBolt(), 5.0F);
            assertNear(20.0F - wanda.getHealth(), 5.0, 1.0E-4, "a bolt from the sky still hurts");
            helper.succeed();
        });
    }

    /** The beam that breaks the shield is stopped too; then the shield is gone, and the next beam hurts her. */
    @GameTest(template = ARENA, batch = "chaos_shield_strength_5_pvp", timeoutTicks = 100)
    public static void aShatteredChaosShieldLetsThorsBeamThrough(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(EnhancedConfig.CHAOS_SHIELD_STRENGTH, 5.0);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        raiseShield(f, wanda, () -> {
            lookAt(thor, wanda);
            beamOnce(thor);
            beamOnce(thor);
            assertNear(data.shieldStrength(), 1.0, 1.0E-4, "two beams took 2 points each off the 5");
            beamOnce(thor);
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "the beam that breaks it is stopped too");
            check(!data.chaosShieldActive() && !WandaAbilities.hasShieldUp(wanda), "the shield shattered");
            beamOnce(thor);
            assertNear(20.0F - wanda.getHealth(), 2.0, 1.0E-4, "and the next beam goes through to her");
            helper.succeed();
        });
    }

    /**
     * The fragments of a shattered Mjolnir are the hammer itself: the shield turns one that is sent at her round, as it
     * does a thrown Mjolnir, and pays what its blow would have cost; it does not shred it. A fragment that is already
     * flying home is let by.
     */
    @GameTest(template = ARENA)
    public static void chaosShieldTurnsAnOutgoingFragmentRoundAndLetsAHomewardOneBy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        PowerData data = Powers.get(wanda);
        raiseShield(f, wanda, () -> {
            Vec3 center = wanda.position().add(0, wanda.getBbHeight() * 0.5, 0);
            // Homeward first: it was sent for someone else, struck, and is flying back across the sphere.
            MjolnirFragmentEntity homeward = new MjolnirFragmentEntity(helper.getLevel(), thor, center.add(1.2, 0, 0), new Vec3(-0.5, 0, 0), null, center);
            homeward.setReturning();
            helper.getLevel().addFreshEntity(homeward);
            PowerTicker.tick(wanda);
            check(!homeward.isRemoved(), "a fragment flying home passes through the shield unharmed");
            assertNear(data.shieldStrength(), 30.0, 1.0E-4, "and costs it nothing");
            homeward.discard();

            // Outgoing, at her.
            MjolnirFragmentEntity outgoing = new MjolnirFragmentEntity(helper.getLevel(), thor, center.add(-1.2, 0, 0), new Vec3(0.5, 0, 0), wanda, center);
            helper.getLevel().addFreshEntity(outgoing);
            check(!outgoing.isReturning(), "sanity: it is on its way out");
            PowerTicker.tick(wanda);
            check(!outgoing.isRemoved() && outgoing.isReturning(), "the shield turns it round instead of destroying it");
            assertNear(data.shieldStrength(), 30.0 - EnhancedConfig.MJOLNIR_SHATTER_DAMAGE.get(), 1.0E-4, "and pays what the strike would have done");
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "she is not touched");
            outgoing.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void chaosShieldReducesExplosionDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer shielded = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer open = f.player(15.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        raiseShield(f, shielded, () -> {
            explode(helper, shielded, 8.0F);
            explode(helper, open, 8.0F);

            assertNear(20.0F - open.getHealth(), 8.0, 1.0E-4, "explosion damage without the shield");
            assertNear(20.0F - shielded.getHealth(), 2.0, 1.0E-4, "the Chaos Shield lets only 25% of explosion damage through");
            assertNear(Powers.get(shielded).shieldStrength(), 30.0 - 6.0, 1.0E-4, "the 6 points it took wear the shield down");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void chaosShieldNeedsFood(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        wanda.getFoodData().setFoodLevel(0);
        AbilityHandler.onKey(wanda, 4, true);
        check(!Powers.get(wanda).chaosShieldActive(), "no shield on an empty stomach");
        check(Powers.get(wanda).isReady(4, helper.getLevel().getGameTime()), "and no cooldown for a shield that never formed");

        wanda.getFoodData().setFoodLevel(1);
        wanda.getFoodData().setSaturation(0.0F);
        AbilityHandler.onKey(wanda, 4, true);
        check(Powers.get(wanda).chaosShieldActive(), "shield up");
        f.everyTick(60, tick -> {
            PowerTicker.tick(wanda);
            wanda.getFoodData().tick(wanda); // what the player's own tick does with exhaustion
            if (Powers.get(wanda).chaosShieldActive()) {
                return false;
            }
            long left = cooldownLeft(wanda, helper);
            check(left > 100 && left <= 120, "a shield that starved has to recharge too, with the short cooldown (6 seconds), not " + left);
            return true;
        });
    }

    /** Pressing B starts it; it grows for the wind-up and blocks nothing until then. */
    @GameTest(template = ARENA)
    public static void chaosShieldTakesTimeToFormAndBlocksNothingMeanwhile(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Skeleton skeleton = harmlessSkeleton(helper, new Vec3(15.5, FLOOR, 10.5));
        PowerData data = Powers.get(wanda);
        int windUp = EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get();
        check(windUp == 15, "it takes 3/4 of a second (15 ticks) to form");

        AbilityHandler.onKey(wanda, 4, true);
        check(data.chaosShieldActive() && !WandaAbilities.hasShieldUp(wanda), "just pressed: forming, not up");
        assertNear(data.chaosShieldProgress(helper.getLevel().getGameTime()), 0.0, 1.0E-6, "nothing grown yet");
        explode(helper, wanda, 8.0F);
        assertNear(20.0F - wanda.getHealth(), 8.0, 1.0E-4, "an explosion at once gets through at full strength");
        arrowBlow(helper, skeleton, wanda, 4.0F);
        assertNear(20.0F - wanda.getHealth(), 12.0, 1.0E-4, "and so does an arrow");
        assertNear(data.shieldStrength(), 30.0, 1.0E-4, "the shield took nothing: it blocked nothing");

        helper.runAfterDelay(windUp - 3, () -> {
            check(!WandaAbilities.hasShieldUp(wanda), "still forming just before it is complete");
            float grown = data.chaosShieldProgress(helper.getLevel().getGameTime());
            check(grown > 0.7F && grown < 1.0F, "it has grown most of the way by then, not " + grown);
            wanda.setHealth(20.0F);
            explode(helper, wanda, 8.0F);
            assertNear(20.0F - wanda.getHealth(), 8.0, 1.0E-4, "still nothing blocked while it forms");
        });
        helper.runAfterDelay(windUp + 1, () -> {
            check(WandaAbilities.hasShieldUp(wanda), "complete after the wind-up");
            assertNear(data.chaosShieldProgress(helper.getLevel().getGameTime()), 1.0, 1.0E-6, "fully grown");
            wanda.setHealth(20.0F);
            explode(helper, wanda, 8.0F);
            assertNear(20.0F - wanda.getHealth(), 2.0, 1.0E-4, "now it reduces explosions");
            wanda.setHealth(20.0F);
            arrowBlow(helper, skeleton, wanda, 4.0F);
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "and stops arrows");
            helper.succeed();
        });
    }

    /** What it stops wears it down by that much, and at 0 it shatters: shards, a glassy sound, a long wait. */
    @GameTest(template = ARENA, batch = "chaos_shield_strength_12")
    public static void chaosShieldWearsDownAndShatters(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.CHAOS_SHIELD_STRENGTH, 12.0);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Skeleton skeleton = harmlessSkeleton(helper, new Vec3(15.5, FLOOR, 10.5));
        PowerData data = Powers.get(wanda);
        raiseShield(f, wanda, () -> {
            assertNear(data.shieldStrength(), 12.0, 1.0E-4, "a new shield has the configured strength");
            arrowBlow(helper, skeleton, wanda, 5.0F);
            assertNear(data.shieldStrength(), 7.0, 1.0E-4, "an arrow of 5 takes 5 off");
            arrowBlow(helper, skeleton, wanda, 5.0F);
            assertNear(data.shieldStrength(), 2.0, 1.0E-4, "and another");
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "neither got through");
            TestKit.heardSounds(wanda);

            arrowBlow(helper, skeleton, wanda, 5.0F);
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "the arrow that breaks it is stopped too");
            check(!data.chaosShieldActive() && !WandaAbilities.hasShieldUp(wanda), "the shield shattered at 0");
            assertNear(data.shieldStrength(), 0.0, 1.0E-4, "nothing left");
            long left = cooldownLeft(wanda, helper);
            check(left >= 295 && left <= 300, "a shattered shield needs 15 seconds (300 ticks) before the next, not " + left);
            List<Object> packets = TestKit.sentPackets(wanda);
            List<TestKit.HeardSound> heard = TestKit.soundsIn(packets);
            check(heard.stream().anyMatch(s -> s.id().equals(SoundEvents.GLASS_BREAK.getLocation())),
                    "and it breaks with a glassy sound, heard " + heard.stream().map(TestKit.HeardSound::id).toList());
            long shards = TestKit.particlesIn(packets).stream()
                    .filter(p -> p.getParticle() instanceof ItemParticleOption chip && chip.getItem().is(Items.RED_DYE)).count();
            check(shards >= 40, "and a burst of red shards flies out of it, not just " + shards + " of them");

            explode(helper, wanda, 8.0F);
            assertNear(20.0F - wanda.getHealth(), 8.0, 1.0E-4, "without it, explosions hit in full");
            AbilityHandler.onKey(wanda, 4, true);
            check(!data.chaosShieldActive(), "and B does nothing while it recharges");
            helper.succeed();
        });
    }

    /** An explosion bigger than the strength that is left breaks the shield and lets the rest through. */
    @GameTest(template = ARENA, batch = "chaos_shield_strength_10")
    public static void chaosShieldShatteringExplosionLetsTheRestThrough(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.CHAOS_SHIELD_STRENGTH, 10.0);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        raiseShield(f, wanda, () -> {
            explode(helper, wanda, 8.0F);
            assertNear(data.shieldStrength(), 4.0, 1.0E-4, "10 - 75% of 8");
            assertNear(20.0F - wanda.getHealth(), 2.0, 1.0E-4, "the quarter that always gets through");

            wanda.setHealth(20.0F);
            explode(helper, wanda, 8.0F);
            assertNear(20.0F - wanda.getHealth(), 4.0, 1.0E-4, "the 4 points left take 4 of the 6 it would stop: 2 + 2 get through");
            check(!data.chaosShieldActive(), "and it shatters");
            helper.succeed();
        });
    }

    /** It holds for at most 8 seconds, then drops by itself; held to its limit, it needs 15 seconds to come back. */
    @GameTest(template = ARENA, batch = "chaos_shield_limit")
    public static void chaosShieldDropsAtItsLimitAndNeedsLongToRecharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(EnhancedConfig.CHAOS_SHIELD_MAX_TICKS.get() == 160, "the limit is 8 seconds (160 ticks)");
        f.config(EnhancedConfig.CHAOS_SHIELD_MAX_TICKS, 20);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        long start = helper.getLevel().getGameTime();
        AbilityHandler.onKey(wanda, 4, true);
        int windUp = EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get();
        long[] droppedAt = {-1};

        f.everyTick(windUp + 20 + 10, tick -> {
            PowerTicker.tick(wanda);
            long now = helper.getLevel().getGameTime();
            if (data.chaosShieldActive()) {
                check(now - start < windUp + 20 + 1, "the shield is still up " + (now - start) + " ticks after B, past its limit");
                return false;
            }
            if (droppedAt[0] < 0) {
                droppedAt[0] = now - start;
                check(droppedAt[0] >= windUp + 19 && droppedAt[0] <= windUp + 21,
                        "it dropped " + droppedAt[0] + " ticks after B: the wind-up plus the limit");
            }
            long left = cooldownLeft(wanda, helper);
            check(left > 280, "held to its limit it needs the long wait (300 ticks), not " + left);
            AbilityHandler.onKey(wanda, 4, false);
            AbilityHandler.onKey(wanda, 4, true);
            check(!data.chaosShieldActive(), "B can't raise it again until then, even if pressed again");
            return true;
        });
    }

    /** B released: it drops at once, with the short cooldown; it can be raised again when that is over. */
    @GameTest(template = ARENA, batch = "chaos_shield_cooldown")
    public static void chaosShieldDropsWhenReleasedAndRechargesBriefly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(EnhancedConfig.CHAOS_SHIELD_COOLDOWN.get() == 120, "a dropped shield needs 6 seconds (120 ticks)");
        f.config(EnhancedConfig.CHAOS_SHIELD_COOLDOWN, 30);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        raiseShield(f, wanda, () -> {
            AbilityHandler.onKey(wanda, 4, false);
            check(!data.chaosShieldActive() && !WandaAbilities.hasShieldUp(wanda), "released: it drops at once");
            long left = cooldownLeft(wanda, helper);
            check(left >= 28 && left <= 30, "and the short cooldown starts, not " + left);

            AbilityHandler.onKey(wanda, 4, true);
            check(!data.chaosShieldActive(), "B does nothing meanwhile");
            helper.runAfterDelay(31, () -> {
                AbilityHandler.onKey(wanda, 4, true);
                check(data.chaosShieldActive(), "when the cooldown is over B raises it again");
                // Let go before it forms: a shield that never formed still has to recharge.
                AbilityHandler.onKey(wanda, 4, false);
                check(!data.chaosShieldActive() && !WandaAbilities.hasShieldUp(wanda), "released during the wind-up: gone");
                check(cooldownLeft(wanda, helper) >= 28, "and the cooldown applies all the same");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA)
    public static void chaosShieldDefaultsAndItsStrengthIsSynced(GameTestHelper helper) {
        check(EnhancedConfig.CHAOS_SHIELD_STRENGTH.get() == 30.0, "30 points of strength");
        check(EnhancedConfig.CHAOS_SHIELD_BROKEN_COOLDOWN.get() == 300, "15 seconds after it shattered or was held to its limit");
        assertNear(EnhancedConfig.CHAOS_SHIELD_EXHAUSTION.get(), 0.1, 1.0E-9, "it still costs hunger while held");

        PowerData data = new PowerData();
        data.raiseChaosShield(1000L, 15, 22.5F);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        PowerData.STREAM_CODEC.encode(buf, data);
        PowerData synced = PowerData.STREAM_CODEC.decode(buf);
        check(synced.chaosShieldActive() && synced.shieldFormedAt() == 1015L && synced.shieldStrength() == 22.5F,
                "what a client needs to draw and show the shield reaches it");
        check(!synced.chaosShieldUp(1014L) && synced.chaosShieldUp(1015L), "and it knows when the shield is complete");
        assertNear(synced.chaosShieldProgress(1007.5F), 0.5, 1.0E-6, "and how far it has grown");
        check(buf.readableBytes() == 0, "nothing is left over in the packet");

        PowerData idle = new PowerData();
        RegistryFriendlyByteBuf buf2 = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        PowerData.STREAM_CODEC.encode(buf2, idle);
        check(!PowerData.STREAM_CODEC.decode(buf2).chaosShieldActive() && buf2.readableBytes() == 0, "no shield: nothing extra sent");
        helper.succeed();
    }


    private ScarletWitchTests() {
    }
}

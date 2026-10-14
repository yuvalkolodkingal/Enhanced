package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.ApocalypseAbilities;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.apocalypse.SandHazards;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Log;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.every;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.put;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.rel;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Apocalypse, the hero (spec 4 and 8, tests 43 to 53 and 57): the Essence that makes him, his looks and his body, the five
 * abilities with their Sneak variants, the shield that stops everything but the mind, and what he does to the other heroes
 * and they to him. The boss he is won from has its own classes. A test that keeps server-wide state (the sand that rolls, the
 * storm, the shield's memory) has a batch of its own, and takes {@link TestKit#bossFixture} so that the next one starts clean.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ApocalypseTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** An Apocalypse at full health (the body gives him forty, which a new player does not have yet). */
    private static ServerPlayer hero(TestKit.Fixture f, double x, double z) {
        ServerPlayer hero = f.player(x, FLOOR, z, Power.APOCALYPSE);
        hero.setHealth(hero.getMaxHealth());
        return hero;
    }

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
    }

    private static void release(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, false);
    }

    /** A press of the key with Sneak held. */
    private static void sneakPress(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        AbilityHandler.onKey(player, ability, true);
        player.setShiftKeyDown(false);
    }

    /** Makes the cooldown of one slot run out at once. */
    private static void ready(ServerPlayer player, int slot) {
        Powers.get(player).startCooldown(slot, player.level().getGameTime(), 0);
    }

    private static long cooldownLeft(ServerPlayer player, int slot) {
        return Powers.get(player).cooldownUntil(slot) - player.level().getGameTime();
    }

    /** A husk with no armour and 200 health that stands where it is put: it takes damage as written, and a lot of it. */
    private static Husk bareHusk(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    private static Skeleton harmlessSkeleton(GameTestHelper helper, double x, double z) {
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(x, FLOOR, z));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no sunburn
        skeleton.setNoAi(true);
        return skeleton;
    }

    /** What the damage hook did with a blow: stopped altogether, or the part of it that went through. */
    private record Blow(boolean stopped, float passed) {
    }

    /** Offers a blow of this size to the damage chain as it would come to the target, and reports what is left of it. */
    private static Blow blow(ServerPlayer target, DamageSource source, float amount) {
        LivingIncomingDamageEvent event = new LivingIncomingDamageEvent(target, new DamageContainer(source, amount));
        NeoForge.EVENT_BUS.post(event);
        return new Blow(event.isCanceled(), event.isCanceled() ? 0.0F : event.getAmount());
    }

    private static int run(ServerPlayer player, String command) throws CommandSyntaxException {
        CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        return player.getServer().getCommands().getDispatcher().execute(command, source);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Essence (test 43) and the looks (44, 45)

    @GameTest(template = ARENA, batch = "apocalypse_hero_essence", timeoutTicks = 100)
    public static void essenceAwakensAPowerlessPlayerAfterTheRitual(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        ItemStack essence = new ItemStack(ModItems.ESSENCE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, essence);
        int hold = ApocalypseConfig.ESSENCE_RITUAL_TICKS.get();
        check(hold == 60 && essence.getItem().getUseDuration(essence, player) == hold, "it is held for essenceRitualTicks, three seconds");
        float health = player.getHealth();
        Log log = new Log(player).drain();
        log.clear();

        // Let go after a second: the wisps came, the heartbeat sounded and was stopped, and nothing changed.
        check(essence.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction() && player.isUsingItem(), "the hold begins");
        for (int i = 0; i < 20; i++) {
            player.doTick();
        }
        log.drain();
        long wisps = log.particles().stream().filter(p -> p.getParticle() instanceof EnergyParticleOptions).count();
        check(wisps >= 20, "wisps spiral in every tick of the hold: " + wisps);
        check(log.heard(ModSounds.ESSENCE_TRANSFER.get()), "to a heartbeat");
        player.releaseUsingItem();
        log.drain();
        check(log.soundStopped(ModSounds.ESSENCE_TRANSFER.get()), "which stops when they let go");
        check(Powers.power(player) == Power.NONE && essence.getCount() == 1 && !player.isUsingItem(), "an early release changes nothing");

        // Held to the end: he is Apocalypse, the Essence is gone, a bolt that burns nothing struck, and nobody was hurt.
        check(essence.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "the hold begins again");
        int ticks = 0;
        while (player.isUsingItem() && ticks < 120) {
            player.doTick();
            ticks++;
        }
        check(ticks >= hold - 1 && ticks <= hold + 1, "the hold takes three seconds: " + ticks);
        check(Powers.power(player) == Power.APOCALYPSE, "and awakens Apocalypse: " + Powers.power(player));
        check(player.getMainHandItem().isEmpty(), "the Essence is used up");
        // (His healing factor may have mended a point in the very tick he awoke: the body is 40 health now and the wait for quiet is
        // counted from before the world began, so it is "no less", not "the same".)
        check(player.getHealth() >= health && !player.isOnFire(), "with no damage and no fire: health " + player.getHealth() + " of " + health + ", fire ticks " + player.getRemainingFireTicks());
        log.drain();
        check(log.titled("power.enhanced.apocalypse"), "the title is shown");
        check(log.said("power.enhanced.apocalypse.broadcast"), "and the whole server is told");
        check(!level.getEntities(EntityType.LIGHTNING_BOLT, e -> e.distanceTo(player) < 3.0F).isEmpty(), "a bolt of lightning struck where they stood");

        // He already is En Sabah Nur: told so, nothing used up.
        ItemStack another = new ItemStack(ModItems.ESSENCE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, another);
        log.clear();
        check(another.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction() && !player.isUsingItem(), "no hold begins for him (the click is spent on the refusal)");
        check(log.drain().toldOnActionBar("message.enhanced.essence.already_apocalypse") && another.getCount() == 1, "he is told he already is En Sabah Nur");

        // Another hero's body does not take it: refused with a word, and the Essence stays.
        for (Power power : List.of(Power.MAGNETO, Power.THOR)) {
            ServerPlayer hero = f.player(9.5, FLOOR, 5.5, power);
            ItemStack stack = new ItemStack(ModItems.ESSENCE.get());
            hero.setItemInHand(InteractionHand.MAIN_HAND, stack);
            Log heroLog = new Log(hero).drain();
            heroLog.clear();
            check(stack.getItem().use(level, hero, InteractionHand.MAIN_HAND).getResult().consumesAction() && !hero.isUsingItem(), power + " is turned away (the click is spent on the refusal)");
            check(heroLog.drain().toldOnActionBar("message.enhanced.essence.already_powered"), "with a word");
            check(stack.getCount() == 1 && Powers.power(hero) == power, "and nothing is used up");
        }

        // In Creative the Essence is kept.
        ServerPlayer creative = f.player(13.5, FLOOR, 5.5, GameType.CREATIVE);
        ItemStack kept = new ItemStack(ModItems.ESSENCE.get());
        creative.setItemInHand(InteractionHand.MAIN_HAND, kept);
        kept.getItem().use(level, creative, InteractionHand.MAIN_HAND);
        for (int i = 0; i < hold + 2 && creative.isUsingItem(); i++) {
            creative.doTick();
        }
        check(Powers.power(creative) == Power.APOCALYPSE && creative.getMainHandItem().getCount() == 1, "in Creative the Essence is kept");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_command", timeoutTicks = 100)
    public static void commandSetsApocalypseInALook(GameTestHelper helper) throws CommandSyntaxException {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer op = f.player(10.5, FLOOR, 10.5);
        run(op, "enhanced power set @s apocalypse en_sabah_nur");
        check(Powers.power(op) == Power.APOCALYPSE, "the command makes him Apocalypse");
        check("en_sabah_nur".equals(Powers.get(op).variant()), "in the look asked for: " + Powers.get(op).variant());
        run(op, "enhanced look @s own");
        check("own".equals(Powers.get(op).variant()), "and /enhanced look changes it");
        boolean refused = false;
        try {
            run(op, "enhanced power set @s apocalypse nonsense");
        } catch (CommandSyntaxException e) {
            refused = true;
        }
        check(refused, "an unknown look is refused");
        check("own".equals(Powers.get(op).variant()), "and changes nothing");
        run(op, "enhanced power set @s none");
        check(Powers.power(op) == Power.NONE, "an operator can take the power away");

        // The operator's way to wake him is the boss command: forced, it needs no Capstone, no sand and no breaker.
        run(op, "enhanced boss start apocalypse force");
        check(BossFights.active(level) instanceof ApocalypseFight, "/enhanced boss start apocalypse wakes him");
        ApocalypseFight fight = (ApocalypseFight) BossFights.active(level);
        run(op, "enhanced boss stop");
        check(fight.isOver(), "and /enhanced boss stop ends the fight");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_pure")
    public static void apocalypseLooksAreTwoAndYourOwn(GameTestHelper helper) {
        List<Variant> looks = Variants.of(Power.APOCALYPSE);
        check(looks.stream().map(Variant::id).toList().equals(List.of("apocalypse", "en_sabah_nur", "own")), "the three looks: " + looks.stream().map(Variant::id).toList());
        Variant first = looks.get(0);
        check(first.skin() != null && first.cape() != null && !first.slim(), "the first has a skin and a cape and wide arms");
        Variant second = looks.get(1);
        check(second.skin() != null && second.cape() == null && !second.slim(), "the second a skin and no cape");
        Variant own = looks.get(2);
        check(own.ownSkin() && own.cape() == null, "your own has neither");
        check(Variants.get(Power.APOCALYPSE, "") == first || Variants.get(Power.APOCALYPSE, "").equals(first), "and the first is the default");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The body (46) and levitation (48)

    @GameTest(template = ARENA, batch = "apocalypse_hero_body", timeoutTicks = 200)
    public static void bodyAndHealingFactor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 10.5, 10.5);
        check(hero.getMaxHealth() == 40.0F, "forty health: " + hero.getMaxHealth());
        assertNear(hero.getAttributeValue(Attributes.ARMOR), 10.0, 1.0E-6, "armour");
        assertNear(hero.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 4.0, 1.0E-6, "toughness");
        assertNear(hero.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.5, 1.0E-6, "knockback resistance");

        // The body goes with the power, and is never anyone else's.
        ServerPlayer lost = f.player(5.5, FLOOR, 5.5, Power.APOCALYPSE);
        Powers.setPower(lost, Power.NONE);
        check(lost.getMaxHealth() == 20.0F && lost.getAttributeValue(Attributes.ARMOR) == 0.0 && lost.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0.0
                && lost.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.0, "a player who is no longer Apocalypse has none of it");
        ServerPlayer other = f.player(15.5, FLOOR, 5.5, Power.THOR);
        PowerTicker.tick(other);
        check(other.getMaxHealth() == 30.0F && other.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0.0, "nor does another hero get any of it");

        // Healing factor: a second after a wait of 100 ticks without damage, and not before.
        hero.setHealth(20.0F);
        check(hero.hurt(level.damageSources().magic(), 4.0F), "he is hurt");
        float hurt = hero.getHealth();
        long at = level.getGameTime();
        every(helper, 150, tick -> {
            PowerTicker.tick(hero);
            long age = level.getGameTime() - at;
            if (age < 100) {
                check(hero.getHealth() == hurt, "no healing before 100 ticks without damage (" + age + "): " + hero.getHealth());
            } else if (age < 120) {
                check(hero.getHealth() == hurt + 1.0F, "one health at 100 ticks, then a second to the next (" + age + "): " + hero.getHealth());
            } else {
                check(hero.getHealth() == hurt + 2.0F, "and one more at 120 (" + age + "): " + hero.getHealth());
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_fly", timeoutTicks = 100)
    public static void levitationIsFlightAtItsSpeed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        check(Power.APOCALYPSE.canFly() && Power.APOCALYPSE.flightAbility() == 2, "he flies, on G");
        assertNear(Power.APOCALYPSE.flightSpeed(), 0.08, 1.0E-6, "at 0.08");
        ServerPlayer hero = hero(f, 10.5, 10.5);
        PowerTicker.tick(hero);
        check(hero.mayFly(), "he may fly");
        assertNear(hero.getAbilities().getFlyingSpeed(), 0.08, 1.0E-6, "at his speed");
        press(hero, 2);
        check(hero.getAbilities().flying, "G takes off");
        press(hero, 2);
        check(!hero.getAbilities().flying, "and lands");
        Powers.setPower(hero, Power.NONE);
        check(!hero.mayFly() && hero.getAbilities().getFlyingSpeed() == 0.05F, "the flight goes with the power");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 1: the Sand Wave and the Entomb (47)

    @GameTest(template = ARENA, batch = "apocalypse_hero_wave", timeoutTicks = 120)
    public static void sandWaveRollsEighteenBlocksHitsEachOnceAndSparesTheRest(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 1.5, 10.5);
        TestKit.look(hero, TestKit.EAST, 0.0F);
        Husk near = bareHusk(helper, 5.5, 10.5);
        Husk middle = bareHusk(helper, 11.5, 10.5);
        Husk far = bareHusk(helper, 18.5, 10.5);
        Husk behind = bareHusk(helper, 0.5, 10.5);
        Husk aside = bareHusk(helper, 8.5, 14.5);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(8.5, FLOOR, 10.5));
        ServerPlayer bystander = f.player(14.5, FLOOR, 10.5);
        Log log = new Log(hero).drain();
        log.clear();
        float exhaustion = hero.getFoodData().getExhaustionLevel();

        press(hero, 1);
        long cooldown = cooldownLeft(hero, 1);
        check(cooldown >= 59 && cooldown <= 60, "three seconds of cooldown: " + cooldown);
        assertNear(hero.getFoodData().getExhaustionLevel() - exhaustion, 1.0, 1.0E-4, "and one exhaustion");
        check(SandHazards.count() == 1, "a wave rolls");
        log.drain();
        check(log.heard(ModSounds.SAND_WAVE.get()), "with its sound");
        List<ApocalypseFxPayload> fx = log.effects(ApocalypseFxPayload.WAVE);
        check(fx.size() == 1 && fx.get(0).c() == 90 && fx.get(0).d() == 18, "and its picture, 0.9 blocks a tick for 18: " + fx);
        every(helper, 100, tick -> {
            if (SandHazards.count() > 0) {
                return false;
            }
            for (Husk hit : List.of(near, middle, far)) {
                assertNear(200.0F - hit.getHealth(), 8.0, 1.0E-3, "a creature in its path takes 8, once");
                MobEffectInstance slow = hit.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                check(slow != null && slow.getAmplifier() == 1 && slow.getDuration() > 0 && slow.getDuration() <= 40, "and Slowness II for up to 2 seconds");
            }
            check(behind.getHealth() == 200.0F && aside.getHealth() == 200.0F, "nothing behind him, or off to the side");
            check(villager.getHealth() == villager.getMaxHealth(), "a villager in its path is spared");
            check(bystander.getHealth() == bystander.getMaxHealth(), "and so is a player, with PvP off");
            check(hero.getHealth() == hero.getMaxHealth(), "and he never hurts himself");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_wave_pvp", timeoutTicks = 100)
    public static void sandWaveHurtsAPlayerOnlyWithPvpOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(true);
        ServerPlayer hero = hero(f, 1.5, 10.5);
        TestKit.look(hero, TestKit.EAST, 0.0F);
        ServerPlayer victim = f.player(8.5, FLOOR, 10.5);
        ServerPlayer creative = f.player(12.5, FLOOR, 10.5, GameType.CREATIVE);
        press(hero, 1);
        every(helper, 80, tick -> {
            if (SandHazards.count() > 0) {
                return false;
            }
            assertNear(victim.getMaxHealth() - victim.getHealth(), 8.0, 1.0E-3, "with PvP on a player in its path takes 8, once");
            check(creative.getHealth() == creative.getMaxHealth() && creative.isCreative(), "a Creative player is never touched");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_wave_ground", timeoutTicks = 60)
    public static void sandWaveNeedsGroundWithinSixBlocks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        TestKit.look(hero, TestKit.EAST, 0.0F);
        Log log = new Log(hero).drain();
        log.clear();
        // Too high: there is no ground to pull sand from, and it costs nothing.
        put(f, hero, 3.5, FLOOR + 7.0, 10.5);
        float exhaustion = hero.getFoodData().getExhaustionLevel();
        press(hero, 1);
        check(log.drain().toldOnActionBar("message.enhanced.sand.no_ground"), "\"No ground to pull sand from\"");
        check(cooldownLeft(hero, 1) <= 0 && SandHazards.count() == 0 && hero.getFoodData().getExhaustionLevel() == exhaustion,
                "with no cooldown, no sand and no hunger");
        // A levitating Apocalypse casts it at the ground beneath.
        put(f, hero, 3.5, FLOOR + 5.0, 10.5);
        press(hero, 1);
        check(cooldownLeft(hero, 1) > 0 && SandHazards.count() == 1, "within six blocks of the ground it rolls");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_entomb", timeoutTicks = 100)
    public static void entombSwallowsWhomItShouldAndRefusesTheRest(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 3.5, 10.5);
        Log log = new Log(hero).drain();
        log.clear();
        Husk husk = bareHusk(helper, 10.5, 10.5);
        lookAt(hero, husk);
        float exhaustion = hero.getFoodData().getExhaustionLevel();

        sneakPress(hero, 1);
        SandCocoonEntity cocoon = SandCocoonEntity.cocoonOf(husk);
        check(cocoon != null && husk.hasEffect(ModEffects.ENTOMBED), "Sneak + R swallows it in a cocoon");
        check(cocoon.isHeroes() && cocoon.health() == 16.0F && cocoon.ticksLeft() == 80, "of 16 health, for 4 seconds");
        long cooldown = cooldownLeft(hero, 1);
        check(cooldown >= 239 && cooldown <= 240, "twelve seconds of cooldown: " + cooldown);
        assertNear(hero.getFoodData().getExhaustionLevel() - exhaustion, 3.0, 1.0E-4, "and three exhaustion");
        log.drain();
        check(log.heard(ModSounds.ENTOMB.get()) && log.effects(ApocalypseFxPayload.ENTOMB).size() == 1, "with its sound and its picture");
        // The two share the key: the Wave waits for the Entomb's cooldown.
        press(hero, 1);
        check(SandHazards.count() == 0, "R is not ready while the Entomb recharges");
        // A friend frees it by hitting the cocoon.
        cocoon.hurt(level.damageSources().generic(), 16.0F);
        check(cocoon.isRemoved() && !husk.hasEffect(ModEffects.ENTOMBED), "a hit of 16 breaks it and the effect goes with it");

        // Whom the sand cannot hold: nothing in sight, bosses, a Creative player, a phased Vision. No cooldown for any of them.
        ready(hero, 1);
        TestKit.look(hero, 0.0F, -80.0F);
        log.clear();
        sneakPress(hero, 1);
        check(log.drain().toldOnActionBar("message.enhanced.entomb.nothing") && cooldownLeft(hero, 1) <= 0, "nobody in reach");

        Warden boss = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(6.5, FLOOR, 16.5));
        lookAt(hero, boss);
        log.clear();
        sneakPress(hero, 1);
        check(log.drain().toldOnActionBar("message.enhanced.entomb.slips") && SandCocoonEntity.cocoonOf(boss) == null && cooldownLeft(hero, 1) <= 0,
                "the Warden slips through the sand (entomb_immune)");

        ServerPlayer creative = f.player(10.5, FLOOR, 15.5, GameType.CREATIVE);
        lookAt(hero, creative);
        sneakPress(hero, 1);
        check(log.drain().toldOnActionBar("message.enhanced.entomb.slips") && !creative.hasEffect(ModEffects.ENTOMBED), "and so does a Creative player");

        ServerPlayer vision = f.player(10.5, FLOOR, 7.5, Power.VISION);
        Powers.get(vision).setVisionPhased(true);
        lookAt(hero, vision);
        sneakPress(hero, 1);
        check(log.drain().toldOnActionBar("message.enhanced.entomb.slips") && !vision.hasEffect(ModEffects.ENTOMBED), "and a phased Vision");

        // A complete shield keeps the sand out, and says so.
        ServerPlayer wanda = f.player(8.5, FLOOR, 5.5, Power.SCARLET_WITCH);
        Powers.get(wanda).raiseChaosShield(level.getGameTime(), 0, 30.0F);
        lookAt(hero, wanda);
        sneakPress(hero, 1);
        check(log.drain().toldOnActionBar("message.enhanced.apocalypse.shield_protects") && !wanda.hasEffect(ModEffects.ENTOMBED) && cooldownLeft(hero, 1) <= 0,
                "\"The shield protects\": a complete Chaos Shield cannot be entombed");

        // A player is held only with PvP on.
        ServerPlayer victim = f.player(12.5, FLOOR, 12.5);
        lookAt(hero, victim);
        sneakPress(hero, 1);
        check(log.drain().toldOnActionBar("message.enhanced.entomb.nothing") && !victim.hasEffect(ModEffects.ENTOMBED) && cooldownLeft(hero, 1) <= 0,
                "with PvP off a player is not there for the sand");
        f.pvp(true);
        sneakPress(hero, 1);
        check(victim.hasEffect(ModEffects.ENTOMBED) && SandCocoonEntity.cocoonOf(victim) != null, "with PvP on they are held");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 3: the Portal (49)

    @GameTest(template = ARENA, batch = "apocalypse_hero_portal", timeoutTicks = 100)
    public static void portalTakesPetsAndSneakingAlliesAlong(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 4.5, 4.5);
        Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(5.5, FLOOR, 4.5));
        pet.tame(hero);
        Wolf stray = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(3.5, FLOOR, 4.5));
        ServerPlayer sneaker = f.player(4.5, FLOOR, 6.0);
        sneaker.setShiftKeyDown(true);
        ServerPlayer stander = f.player(3.5, FLOOR, 6.0);
        ServerPlayer distant = f.player(4.5, FLOOR, 10.5);
        distant.setShiftKeyDown(true);
        Vec3 standerAt = stander.position();
        Vec3 strayAt = stray.position();
        Vec3 distantAt = distant.position();
        Vec3 target = helper.absoluteVec(new Vec3(14.5, 2.0, 4.5));
        lookAt(hero, target);
        Log log = new Log(hero).drain();
        log.clear();
        float exhaustion = hero.getFoodData().getExhaustionLevel();

        press(hero, 3);
        check(hero.position().distanceTo(target) < 0.01, "he stands on the block he looked at: " + rel(f, hero));
        check(pet.position().distanceTo(hero.position()) <= 2.6 && sneaker.position().distanceTo(hero.position()) <= 2.6,
                "his wolf and the sneaking player arrive with him, within two blocks");
        check(stander.position().equals(standerAt) && stray.position().equals(strayAt) && distant.position().equals(distantAt),
                "a standing player, a wolf that is nobody's and a sneaker six blocks off stay where they were");
        long cooldown = cooldownLeft(hero, 3);
        check(cooldown >= 159 && cooldown <= 160, "eight seconds of cooldown: " + cooldown);
        assertNear(hero.getFoodData().getExhaustionLevel() - exhaustion, 2.0, 1.0E-4, "and two exhaustion");
        log.drain();
        check(log.heard(ModSounds.APOCALYPSE_PORTAL.get()), "with its sound");
        List<ApocalypseFxPayload> rings = log.effects(ApocalypseFxPayload.PORTAL);
        check(rings.size() == 1 && rings.get(0).c() == 20, "and its rings, open for 20 ticks: " + rings);

        // Aimed at nothing (no block within four blocks, say): a word and no cooldown.
        f.config(ApocalypseConfig.PORTAL_RANGE, 4);
        ready(hero, 3);
        TestKit.look(hero, TestKit.EAST, 0.0F);
        Vec3 where = hero.position();
        log.clear();
        press(hero, 3);
        check(log.drain().toldOnActionBar("message.enhanced.portal.nothing"), "\"Nothing in reach\"");
        check(cooldownLeft(hero, 3) <= 0 && hero.position().equals(where), "and it costs nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_portal_walls", timeoutTicks = 100)
    public static void portalRespectsTheHexAndPins(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(true);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 4);
        ServerPlayer hero = hero(f, 4.5, 4.5);
        ServerPlayer witch = f.player(14.5, FLOOR, 4.5, Power.SCARLET_WITCH);
        ServerPlayer friend = f.player(4.5, FLOOR, 6.0);
        friend.setShiftKeyDown(true);
        press(witch, 5);
        check(HexManager.ownedBy(f.level(), witch.getUUID()).isPresent(), "a Hex stands round the Scarlet Witch");
        Vec3 start = hero.position();
        lookAt(hero, helper.absoluteVec(new Vec3(14.5, 2.0, 4.5)));
        press(hero, 3);
        check(hero.position().equals(start) && cooldownLeft(hero, 3) <= 0, "the wall refuses his portal into the Hex: he stays, and it costs nothing");

        // Mjolnir lies on the sneaking friend: he cannot be taken through. And on the hero himself: no portal at all.
        ServerPlayer thor = f.player(8.5, FLOOR, 8.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, friend);
        check(MjolnirPin.isPinned(friend), "Mjolnir pins the friend");
        Vec3 friendAt = friend.position();
        lookAt(hero, helper.absoluteVec(new Vec3(4.5, 2.0, 14.5)));
        press(hero, 3);
        check(hero.position().distanceTo(helper.absoluteVec(new Vec3(4.5, 2.0, 14.5))) < 0.01, "outside the Hex the portal opens");
        check(friend.position().equals(friendAt), "and the pinned friend stays");

        ready(hero, 3);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, hero);
        check(MjolnirPin.isPinned(hero), "Mjolnir pins the hero");
        Vec3 held = hero.position();
        lookAt(hero, helper.absoluteVec(new Vec3(14.5, 2.0, 14.5)));
        press(hero, 3);
        check(hero.position().equals(held) && cooldownLeft(hero, 3) <= 0, "a pinned hero cannot open a portal");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 4: the Shield (50, 57)

    @GameTest(template = ARENA, batch = "apocalypse_hero_shield", timeoutTicks = 200)
    public static void heroShieldStopsEverythingButMindPower(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        f.pvp(false);
        ServerPlayer hero = hero(f, 10.5, 10.5);
        ServerPlayer wanda = f.player(5.5, FLOOR, 5.5, Power.SCARLET_WITCH);
        ServerPlayer phoenix = f.player(15.5, FLOOR, 5.5, Power.PHOENIX);
        ServerPlayer plain = f.player(5.5, FLOOR, 15.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        Skeleton skeleton = harmlessSkeleton(helper, 15.5, 15.5);
        PowerData data = Powers.get(hero);
        int windUp = ApocalypseConfig.SHIELD_WIND_UP_TICKS.get();

        press(hero, 4);
        check(data.chaosShieldActive() && !ApocalypseGuard.isUp(hero), "B starts it: forming, not up");
        Blow forming = blow(hero, level.damageSources().explosion(null, null), 8.0F);
        check(!forming.stopped() && forming.passed() == 8.0F && data.shieldStrength() == 60.0F, "while it forms it stops nothing and costs nothing");

        helper.runAfterDelay(windUp + 1, () -> {
            check(ApocalypseGuard.isUp(hero) && data.shieldStrength() == 60.0F, "complete after the wind-up, with 60 points");
            Arrow arrow = new Arrow(level, skeleton, new ItemStack(Items.ARROW), null);
            record Case(String what, DamageSource source, float amount) {
            }
            List<Case> stopped = List.of(
                    new Case("an arrow", level.damageSources().arrow(arrow, skeleton), 10.0F),
                    new Case("a blow", level.damageSources().mobAttack(zombie), 6.0F),
                    new Case("an explosion (all of it)", level.damageSources().explosion(null, null), 8.0F),
                    new Case("Thor's beam", ModDamageTypes.source(level, ModDamageTypes.LIGHTNING_BEAM, plain, plain), 6.0F),
                    new Case("a Blitz", ModDamageTypes.source(level, ModDamageTypes.SPEED_BLITZ, plain, plain), 12.0F),
                    new Case("fire", level.damageSources().onFire(), 2.0F),
                    new Case("lava", level.damageSources().lava(), 4.0F),
                    new Case("magic", level.damageSources().magic(), 6.0F),
                    new Case("lightning", level.damageSources().lightningBolt(), 5.0F),
                    new Case("a player's blow, whatever PvP says", level.damageSources().playerAttack(plain), 6.0F));
            float wear = 0.0F;
            for (Case c : stopped) {
                Blow result = blow(hero, c.source(), c.amount());
                check(result.stopped(), c.what() + " is stopped");
                wear += c.amount() * 0.5F;
            }
            assertNear(data.shieldStrength(), 60.0 - wear, 1.0E-4, "each cost the shield half of what it would have done");

            // What no shield can help: the world's own harm passes whole, and costs the shield nothing.
            float strength = data.shieldStrength();
            for (DamageSource harm : List.of(level.damageSources().fall(), level.damageSources().drown(), level.damageSources().inWall(),
                    level.damageSources().starve(), level.damageSources().cramming(), level.damageSources().fellOutOfWorld(),
                    level.damageSources().genericKill(), level.damageSources().outOfBorder())) {
                Blow result = blow(hero, harm, 3.0F);
                check(!result.stopped() && result.passed() == 3.0F, harm.type().msgId() + " is not stopped");
            }
            check(data.shieldStrength() == strength && ApocalypseGuard.isUp(hero), "and the shield is as it was");

            // Mind power wears it by its factor, once in a while per attacker; the rest of the beam is nothing.
            DamageSource beam = ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, wanda, wanda);
            Blow first = blow(hero, beam, 5.0F);
            check(first.stopped() && first.passed() == 0.0F, "a Scarlet Witch's beam is held");
            assertNear(data.shieldStrength(), strength - 5.0, 1.0E-4, "and wears the shield by 5");
            Blow second = blow(hero, beam, 5.0F);
            check(second.stopped(), "the next tick's hit is held too");
            assertNear(data.shieldStrength(), strength - 5.0, 1.0E-4, "without wearing it again: the shield counts one hit in ten ticks");
            Blow notMind = blow(hero, ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, plain, plain), 5.0F);
            check(notMind.stopped(), "the same beam from someone with no power is an ordinary blow");
            assertNear(data.shieldStrength(), strength - 5.0 - 2.5, 1.0E-4, "which costs half");

            helper.runAfterDelay(ApocalypseShieldConfig.MIND_HIT_COOLDOWN_TICKS.get() + 1, () -> {
                float before = data.shieldStrength();
                check(blow(hero, beam, 5.0F).stopped(), "ten ticks later the beam is counted again");
                assertNear(data.shieldStrength(), before - 5.0, 1.0E-4, "by 5");

                // The Phoenix's Burst x5; and when the strength is gone, the shield shatters and the rest goes through.
                data.setShieldStrength(12.0F);
                Log log = new Log(hero).drain();
                log.clear();
                DamageSource burst = ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, phoenix, phoenix);
                Blow shatter = blow(hero, burst, 5.0F);
                check(!shatter.stopped() && Math.abs(shatter.passed() - 2.6F) < 1.0E-3F,
                        "a Burst of 5 is 25 against 12: it shatters, and the 13 over go through as 2.6 damage: " + shatter);
                check(!data.chaosShieldActive() && !ApocalypseGuard.isUp(hero) && data.shieldStrength() == 0.0F, "the shield is gone");
                long recharge = cooldownLeft(hero, 4);
                check(recharge >= 399 && recharge <= 400, "twenty seconds to the next: " + recharge);
                log.drain();
                check(log.toldOnActionBar("message.enhanced.shield.shattered") && log.heard(ModSounds.APOCALYPSE_SHIELD_BREAK.get()), "with a word and a shatter");
                check(blow(hero, level.damageSources().magic(), 4.0F).passed() == 4.0F, "and without it everything gets through");
                log.clear();
                press(hero, 4);
                check(!data.chaosShieldActive() && log.drain().toldOnActionBar("message.enhanced.shield.recharging"), "B says it is recharging");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_shield_hold", timeoutTicks = 200)
    public static void heroShieldIsHeldAtMostItsTimeAndNeedsFood(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(ApocalypseConfig.SHIELD_MAX_TICKS, 20);
        ServerPlayer hero = hero(f, 10.5, 10.5);
        PowerData data = Powers.get(hero);
        int windUp = ApocalypseConfig.SHIELD_WIND_UP_TICKS.get();
        Log log = new Log(hero).drain();
        log.clear();

        // Let go: the short wait (8 seconds).
        press(hero, 4);
        release(hero, 4);
        check(!data.chaosShieldActive(), "letting go drops it");
        long wait = cooldownLeft(hero, 4);
        check(wait >= 159 && wait <= 160, "eight seconds to the next: " + wait);
        press(hero, 4);
        check(!data.chaosShieldActive() && log.drain().toldOnActionBar("message.enhanced.shield.recharging"), "which it says when B is pressed too soon");

        // Held to its limit: the long wait (20 seconds); it costs hunger for as long as it stands.
        ready(hero, 4);
        press(hero, 4);
        float exhaustion = hero.getFoodData().getExhaustionLevel();
        long start = f.level().getGameTime();
        every(helper, 80, tick -> {
            PowerTicker.tick(hero);
            long age = f.level().getGameTime() - start;
            if (age == 10) {
                assertNear(hero.getFoodData().getExhaustionLevel() - exhaustion, 1.0, 0.15, "a tenth of an exhaustion for every tick it stands: "
                        + (hero.getFoodData().getExhaustionLevel() - exhaustion));
            }
            if (age < windUp + 20) {
                check(data.chaosShieldActive(), "still held at " + age);
                return false;
            }
            if (data.chaosShieldActive()) {
                return false;
            }
            long left = cooldownLeft(hero, 4);
            check(left >= 395 && left <= 400, "held to its limit it needs twenty seconds: " + left);

            // An empty stomach: refused, and one that empties while it stands brings it down.
            ready(hero, 4);
            hero.getFoodData().setFoodLevel(0);
            log.clear();
            press(hero, 4);
            check(!data.chaosShieldActive() && log.drain().toldOnActionBar("message.enhanced.shield.hungry"), "with no food it will not rise");
            hero.getFoodData().setFoodLevel(10);
            press(hero, 4);
            check(data.chaosShieldActive(), "with food it does");
            hero.getFoodData().setFoodLevel(0);
            PowerTicker.tick(hero);
            check(!data.chaosShieldActive() && log.drain().toldOnActionBar("message.enhanced.shield.hungry"), "and it collapses when the food is gone");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_shield_arrows", timeoutTicks = 200)
    public static void heroShieldTurnsAwayArrowsAndSendsAThrownWeaponHomeToThor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 12.5, 10.5);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        PowerTicker.tick(thor);
        Skeleton skeleton = harmlessSkeleton(helper, 6.5, 10.5);
        PowerData data = Powers.get(hero);
        press(hero, 4);
        boolean[] stage = {false, false};
        float health = hero.getHealth();
        Arrow[] arrow = new Arrow[1];
        every(helper, 150, tick -> {
            PowerTicker.tick(hero);
            PowerTicker.tick(thor);
            if (!stage[0]) {
                if (ApocalypseGuard.isUp(hero)) {
                    arrow[0] = new Arrow(level, skeleton, new ItemStack(Items.ARROW), null);
                    Vec3 dir = hero.position().add(0, 0.9, 0).subtract(arrow[0].position());
                    arrow[0].shoot(dir.x, dir.y, dir.z, 2.5F, 0.0F);
                    level.addFreshEntity(arrow[0]);
                    stage[0] = true;
                }
                return false;
            }
            if (!stage[1]) {
                if (!arrow[0].isRemoved()) {
                    return false;
                }
                check(hero.getHealth() == health, "the arrow never touched him");
                float left = data.shieldStrength();
                check(left < 60.0F && left > 55.0F, "it wore the shield by half of what it would have done, not " + left + " left of 60");
                check(data.chaosShieldActive(), "one arrow does not break it");
                // Stormbreaker: turned back to Thor, never destroyed; the shield pays half of the throw's damage.
                data.setShieldStrength(60.0F);
                lookAt(thor, hero);
                press(thor, 1);
                release(thor, 1);
                check(thor.getMainHandItem().isEmpty(), "Stormbreaker is in flight");
                stage[1] = true;
                return false;
            }
            if (thor.getMainHandItem().is(ModItems.STORMBREAKER.get())) {
                check(hero.getHealth() == health, "the axe never touched him");
                assertNear(data.shieldStrength(), 60.0 - EnhancedConfig.STORMBREAKER_THROW_DAMAGE.get() * 0.5, 1.0E-3,
                        "the shield paid half of what the throw would have done");
                check(data.chaosShieldActive(), "and held");
                return true;
            }
            check(tick < 140, "Stormbreaker was destroyed by the shield instead of coming back to Thor");
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_shield_wear", timeoutTicks = 100)
    public static void heroShieldOtherWearAndBossLiteralRule(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 10.5, 10.5);
        ServerPlayer plain = f.player(5.5, FLOOR, 5.5);
        Skeleton skeleton = harmlessSkeleton(helper, 15.5, 15.5);
        PowerData data = Powers.get(hero);
        data.raiseChaosShield(level.getGameTime(), 0, 60.0F);
        check(ApocalypseGuard.isUp(hero), "the shield is up with 60 points");
        DamageSource arrow = level.damageSources().arrow(new Arrow(level, skeleton, new ItemStack(Items.ARROW), null), skeleton);

        // With shieldOtherWear at one half the 60 points stop 120 points of arrow damage, and the first beyond shatters it.
        for (int i = 1; i <= 17; i++) {
            check(blow(hero, arrow, 7.0F).stopped(), "arrow " + i + " is stopped");
        }
        assertNear(data.shieldStrength(), 0.5, 1.0E-4, "119 points stopped cost 59.5");
        Blow last = blow(hero, arrow, 7.0F);
        check(!last.stopped() && Math.abs(last.passed() - 6.0F) < 1.0E-3F, "the 18th shatters it, and 6 of its 7 go through: " + last);
        check(!data.chaosShieldActive(), "the shield is gone");

        // With 0 it is the literal rule: nothing but mind power ever wears it.
        f.config(ApocalypseConfig.SHIELD_OTHER_WEAR, 0.0);
        data.raiseChaosShield(level.getGameTime(), 0, 60.0F);
        for (int i = 0; i < 100; i++) {
            check(blow(hero, arrow, 50.0F).stopped(), "an arrow of 50 is stopped");
        }
        check(data.shieldStrength() == 60.0F && ApocalypseGuard.isUp(hero), "and costs the shield nothing, a hundred times over");
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        check(blow(hero, ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, wanda, wanda), 5.0F).stopped(), "mind power is stopped too");
        assertNear(data.shieldStrength(), 55.0, 1.0E-4, "and wears it by its amount");
        check(plain.isAlive(), "(a bystander)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_shield_boss", timeoutTicks = 100)
    public static void theBossShieldKeepsTheLiteralRuleWhateverTheOptionSays(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().config(ApocalypseConfig.SHIELD_OTHER_WEAR, 1.0);
        ServerLevel level = d.level();
        ServerPlayer hero = d.f().player(16.5, FLOOR, 16.5, Power.APOCALYPSE);
        PowerData data = Powers.get(hero);
        data.raiseChaosShield(level.getGameTime(), 0, 60.0F);
        Skeleton skeleton = harmlessSkeleton(helper, 3.5, 16.5);
        DamageSource arrow = level.damageSources().arrow(new Arrow(level, skeleton, new ItemStack(Items.ARROW), null), skeleton);
        float boss = d.boss().shield().strength();
        check(d.boss().shield().isUp(), "the boss's shield is up");

        d.boss().hurt(arrow, 50.0F);
        check(d.boss().shield().strength() == boss, "an arrow of 50 does nothing to the boss's shield");
        check(blow(hero, arrow, 50.0F).stopped() && data.shieldStrength() == 10.0F, "while it costs the hero's all the option says: 50 of 60");

        // Mind power wears both, at its factor; a Scarlet Witch's is critical on the boss's (3.1, x2), not the hero's.
        data.setShieldStrength(60.0F);
        d.hit(ModDamageTypes.CHAOS_BEAM, 5.0F);
        assertNear(d.boss().shield().strength(), boss - 10.0, 1.0E-3, "a beam of 5 wears his shield by 10");
        check(blow(hero, ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, d.hero(), d.hero()), 5.0F).stopped(), "and the hero's");
        assertNear(data.shieldStrength(), 55.0, 1.0E-4, "by 5");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 5: Power Bestowal and the Sandstorm (51, 52)

    @GameTest(template = ARENA, batch = "apocalypse_hero_bestow", timeoutTicks = 200)
    public static void bestowalEmpowersAlliesOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 3.5, 10.5);
        ServerPlayer friend = f.player(10.5, FLOOR, 10.5);
        Wolf wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(7.5, FLOOR, 7.5));
        wolf.tame(hero);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(10.5, FLOOR, 13.5));
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(7.5, FLOOR, 13.5));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(13.5, FLOOR, 5.5));
        Log log = new Log(hero).drain();
        log.clear();

        // Nothing in sight, then a hostile creature: a word each, and no cooldown.
        TestKit.look(hero, 0.0F, -80.0F);
        press(hero, 5);
        check(log.drain().toldOnActionBar("message.enhanced.bestow.nothing") && cooldownLeft(hero, 5) <= 0, "\"Look at someone to empower\", at no cost");
        lookAt(hero, zombie);
        press(hero, 5);
        check(log.drain().toldOnActionBar("message.enhanced.bestow.hostile") && !zombie.hasEffect(ModEffects.EMPOWERED) && cooldownLeft(hero, 5) <= 0,
                "\"You only enhance your own\": a zombie is refused");

        // A player: the attributes of the effect, and the hunger and the cooldown.
        float exhaustion = hero.getFoodData().getExhaustionLevel();
        double attack = friend.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double speed = friend.getAttributeValue(Attributes.MOVEMENT_SPEED);
        lookAt(hero, friend);
        press(hero, 5);
        MobEffectInstance empowered = friend.getEffect(ModEffects.EMPOWERED);
        check(empowered != null && empowered.getDuration() >= 1190 && empowered.getDuration() <= 1200, "a player is empowered for a minute");
        assertNear(friend.getAttributeValue(Attributes.ATTACK_DAMAGE), attack + 3.0, 1.0E-6, "three more attack damage");
        assertNear(friend.getAttributeValue(Attributes.ARMOR), 4.0, 1.0E-6, "four armour");
        assertNear(friend.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * 1.1, 1.0E-6, "a tenth quicker");
        long cooldown = cooldownLeft(hero, 5);
        check(cooldown >= 399 && cooldown <= 400, "twenty seconds of cooldown: " + cooldown);
        assertNear(hero.getFoodData().getExhaustionLevel() - exhaustion, 6.0, 1.0E-4, "and six exhaustion");
        check(log.drain().toldOnActionBar("message.enhanced.bestow.done") && log.heard(ModSounds.APOCALYPSE_BESTOW.get()), "he is told, and it is heard");

        // Health back, one every two seconds (the effect's own tick).
        var effect = ModEffects.EMPOWERED.get();
        check(effect.shouldApplyEffectTickThisTick(40, 0) && !effect.shouldApplyEffectTickThisTick(41, 0), "the effect heals every 40 ticks");
        friend.setHealth(10.0F);
        effect.applyEffectTick(friend, 0);
        check(friend.getHealth() == 11.0F, "one health at a time");

        // Pets, golems and villagers are his own; at most two at once: a third takes the place of the oldest.
        for (var own : List.of(wolf, villager, golem)) {
            ready(hero, 5);
            lookAt(hero, own);
            press(hero, 5);
            check(own.hasEffect(ModEffects.EMPOWERED), own.getType().getDescriptionId() + " is empowered");
        }
        check(!friend.hasEffect(ModEffects.EMPOWERED) && !wolf.hasEffect(ModEffects.EMPOWERED), "only the last two are: " + ApocalypseAbilities.bestowed(hero).size());
        check(villager.hasEffect(ModEffects.EMPOWERED) && golem.hasEffect(ModEffects.EMPOWERED) && ApocalypseAbilities.bestowed(hero).size() == 2, "the villager and the golem");

        // A hero's cooldowns run out one and a half times as fast, and everything ends with the power.
        ServerPlayer thor = f.player(7.5, FLOOR, 3.5, Power.THOR);
        ServerPlayer control = f.player(13.5, FLOOR, 3.5, Power.THOR);
        long now = level.getGameTime();
        Powers.get(thor).startCooldown(1, now, 200);
        Powers.get(control).startCooldown(1, now, 200);
        ready(hero, 5);
        lookAt(hero, thor);
        press(hero, 5);
        check(thor.hasEffect(ModEffects.EMPOWERED), "a Thor is empowered");
        every(helper, 60, tick -> {
            PowerTicker.tick(thor);
            PowerTicker.tick(control);
            if (tick < 40) {
                return false;
            }
            long age = level.getGameTime() - now;
            long thorLeft = Powers.get(thor).cooldownUntil(1) - level.getGameTime();
            long controlLeft = Powers.get(control).cooldownUntil(1) - level.getGameTime();
            check(Math.abs(controlLeft - (200 - age)) <= 1, "an ordinary cooldown runs at its own pace: " + controlLeft + " left after " + age);
            check(Math.abs(thorLeft - (200 - age * 1.5)) <= 2, "an empowered hero's runs one and a half times as fast: " + thorLeft + " left after " + age);
            Powers.setPower(hero, Power.NONE);
            check(!thor.hasEffect(ModEffects.EMPOWERED) && !villager.hasEffect(ModEffects.EMPOWERED) && !golem.hasEffect(ModEffects.EMPOWERED),
                    "and when he loses the power, those he empowered are no longer");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_storm", timeoutTicks = 250)
    public static void sandstormHurtsEnemiesOnlyAndSlowsThem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 10.5, 10.5);
        Husk enemy = bareHusk(helper, 13.5, 10.5);
        Husk distant = bareHusk(helper, 19.5, 17.5);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(10.5, FLOOR, 14.5));
        Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(8.5, FLOOR, 8.5));
        pet.tame(hero);
        ServerPlayer victim = f.player(10.5, FLOOR, 6.5);
        PowerData data = Powers.get(hero);
        Log log = new Log(hero).drain();
        log.clear();
        float exhaustion = hero.getFoodData().getExhaustionLevel();

        sneakPress(hero, 5);
        check(data.is(Live.SANDSTORM), "Sneak + N raises the storm");
        long cooldown = cooldownLeft(hero, 5);
        check(cooldown >= 599 && cooldown <= 600, "thirty seconds of cooldown: " + cooldown);
        assertNear(hero.getFoodData().getExhaustionLevel() - exhaustion, 8.0, 1.0E-4, "and eight exhaustion");
        log.drain();
        List<ApocalypseFxPayload> storms = log.effects(ApocalypseFxPayload.SANDSTORM);
        check(storms.size() == 1 && storms.get(0).c() == 120 && storms.get(0).d() == 70, "the clients are told: 120 ticks, radius 7: " + storms);
        long start = level.getGameTime();
        List<Long> pulses = new ArrayList<>();
        List<Float> victimLoss = new ArrayList<>();
        float[] last = {200.0F, victim.getHealth()};
        every(helper, 200, tick -> {
            victim.invulnerableTime = 0; // (nothing ticks a player without a client: the frames of the last hit would never run out)
            PowerTicker.tick(hero);
            long age = level.getGameTime() - start;
            if (age == 55) {
                f.pvp(true);
            }
            if (enemy.getHealth() < last[0]) {
                check(Math.abs(last[0] - enemy.getHealth() - 2.0F) < 1.0E-3F, "the storm does 2 at a time: " + (last[0] - enemy.getHealth()));
                pulses.add(age);
                last[0] = enemy.getHealth();
                MobEffectInstance slow = enemy.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                check(slow != null && slow.getAmplifier() == 0, "and slows it (Slowness I)");
            }
            if (victim.getHealth() < last[1]) {
                victimLoss.add(last[1] - victim.getHealth());
                last[1] = victim.getHealth();
                check(age > 55, "a player is hurt only once PvP is on (" + age + ")");
            }
            if (data.is(Live.SANDSTORM)) {
                return false;
            }
            check(age >= 119 && age <= 121, "the storm lasts six seconds: " + age);
            check(pulses.size() >= 10 && pulses.size() <= 12, "about eleven pulses: " + pulses);
            for (int i = 1; i < pulses.size(); i++) {
                check(pulses.get(i) - pulses.get(i - 1) == 10, "every ten ticks: " + pulses);
            }
            check(distant.getHealth() == 200.0F, "none to those beyond its seven blocks");
            check(villager.getHealth() == villager.getMaxHealth() && pet.getHealth() == pet.getMaxHealth(), "none to a villager or to his wolf");
            check(hero.getHealth() == hero.getMaxHealth(), "and never to him");
            check(victimLoss.size() >= 5 && victimLoss.size() <= 7 && victimLoss.stream().allMatch(l -> Math.abs(l - 2.0F) < 1.0E-3F),
                    "with PvP on (from tick 55) the player in it took 2 at a time, every ten ticks: " + victimLoss);
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_storm_arrows", timeoutTicks = 100)
    public static void sandstormSlowsProjectilesToHalfOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 10.5, 10.5);
        sneakPress(hero, 5);
        check(Powers.get(hero).is(Live.SANDSTORM), "the storm is up");
        Vec3 inside = helper.absoluteVec(new Vec3(13.5, FLOOR + 1.0, 10.5));
        Vec3 outside = helper.absoluteVec(new Vec3(19.5, FLOOR + 1.0, 10.5));
        Arrow theirs = new Arrow(level, inside.x, inside.y, inside.z, new ItemStack(Items.ARROW), null);
        Arrow far = new Arrow(level, outside.x, outside.y, outside.z, new ItemStack(Items.ARROW), null);
        Arrow his = new Arrow(level, hero, new ItemStack(Items.ARROW), null);
        his.setPos(inside.x, inside.y + 0.5, inside.z);
        for (Arrow arrow : List.of(theirs, far, his)) {
            arrow.setNoGravity(true);
            arrow.shoot(1.0, 0.0, 0.0, 1.0F, 0.0F);
            level.addFreshEntity(arrow);
        }
        PowerTicker.tick(hero);
        assertNear(theirs.getDeltaMovement().length(), 0.5, 0.01, "an arrow that enters the storm is slowed to half");
        check(theirs.getPersistentData().getBoolean(ApocalypseAbilities.SLOWED_TAG), "and marked");
        PowerTicker.tick(hero);
        assertNear(theirs.getDeltaMovement().length(), 0.5, 0.01, "once: it is not slowed again");
        assertNear(far.getDeltaMovement().length(), 1.0, 0.01, "an arrow beyond the storm is not slowed");
        assertNear(his.getDeltaMovement().length(), 1.0, 0.01, "nor is his own");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The other heroes (53) and the end of the power

    @GameTest(template = ARENA, batch = "apocalypse_hero_others", timeoutTicks = 100)
    public static void heroInteractions(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(true);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        ServerPlayer magneto = f.player(10.5, FLOOR, 10.5, Power.MAGNETO);

        // Magneto cannot lift flesh and bone, unless the hero wears iron.
        check(!Telekinesis.canLift(magneto, hero, Telekinesis.Mode.MAGNETO), "Magneto's Metal Telekinesis does nothing to him");
        hero.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        check(Telekinesis.canLift(magneto, hero, Telekinesis.Mode.MAGNETO), "unless he wears iron");
        hero.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

        // A pin stops levitation.
        ServerPlayer thor = f.player(8.5, FLOOR, 8.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, hero);
        check(MjolnirPin.isPinned(hero), "Mjolnir pins him");
        press(hero, 2);
        check(!hero.getAbilities().flying, "and he cannot levitate");
        press(hero, 4);
        check(Powers.get(hero).chaosShieldActive(), "but R, B and N still answer: the shield rises");
        release(hero, 4);

        // Billy's Freeze is an effect, not a pin: a frozen hero can still raise the shield.
        ServerPlayer frozen = hero(f, 8.5, 14.5);
        frozen.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100, 0));
        press(frozen, 4);
        check(Powers.get(frozen).chaosShieldActive(), "a frozen hero can still raise the shield with B");

        // A Spectator Apocalypse cannot act.
        ServerPlayer ghost = f.player(15.5, FLOOR, 15.5, GameType.SPECTATOR);
        Powers.setPower(ghost, Power.APOCALYPSE);
        TestKit.look(ghost, TestKit.EAST, 0.0F);
        for (int key = 1; key <= 5; key++) {
            press(ghost, key);
        }
        check(SandHazards.count() == 0 && !Powers.get(ghost).chaosShieldActive() && !Powers.get(ghost).is(Live.SANDSTORM), "no sand, no shield and no storm");
        for (int slot = 1; slot <= 5; slot++) {
            check(cooldownLeft(ghost, slot) <= 0, "and no cooldown on " + slot);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_end", timeoutTicks = 100)
    public static void losingThePowerEndsWhatHeLeftInTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(true);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        PowerData data = Powers.get(hero);
        ServerPlayer friend = f.player(10.5, FLOOR, 12.5);
        Husk husk = bareHusk(helper, 10.5, 8.5);

        lookAt(hero, friend);
        press(hero, 5);
        check(friend.hasEffect(ModEffects.EMPOWERED), "he empowered a friend");
        ready(hero, 1);
        TestKit.look(hero, TestKit.EAST, 0.0F);
        press(hero, 1);
        check(SandHazards.all().stream().anyMatch(h -> h.ownerId().equals(hero.getUUID())), "a wave rolls");
        ready(hero, 1);
        lookAt(hero, husk);
        sneakPress(hero, 1);
        SandCocoonEntity cocoon = SandCocoonEntity.cocoonOf(husk);
        check(cocoon != null && husk.hasEffect(ModEffects.ENTOMBED), "a husk is in a cocoon of his");
        press(hero, 4);
        check(data.chaosShieldActive(), "his shield is rising");
        ready(hero, 5);
        sneakPress(hero, 5);
        check(data.is(Live.SANDSTORM), "and the storm is up");

        Powers.setPower(hero, Power.NONE);
        check(!data.is(Live.SANDSTORM) && !data.chaosShieldActive(), "the storm and the shield are gone with the power");
        check(SandHazards.all().stream().noneMatch(h -> h.ownerId().equals(hero.getUUID())), "the sand still rolling is forgotten");
        check(cocoon.isRemoved() && !husk.hasEffect(ModEffects.ENTOMBED), "the cocoon lets go");
        check(!friend.hasEffect(ModEffects.EMPOWERED), "and the friend is no longer empowered");
        check(hero.getMaxHealth() == 20.0F, "nor is he");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_death", timeoutTicks = 100)
    public static void aFallenHeroStopsHoldingWhatHeHeld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 10.5, 10.5);
        PowerData data = Powers.get(hero);
        sneakPress(hero, 5);
        press(hero, 4);
        check(data.is(Live.SANDSTORM) && data.chaosShieldActive(), "the storm is up and the shield rises");
        hero.kill();
        check(!hero.isAlive(), "he falls");
        check(!data.is(Live.SANDSTORM) && !data.chaosShieldActive(), "his death ends the storm and the shield");
        // The plug-in's hold release clears the storm for anyone whose hands are held (a cocoon calls it every tick).
        Powers.get(hero).set(Live.SANDSTORM, true);
        PowerTicker.stopHoldAbilities(data);
        check(!data.is(Live.SANDSTORM), "and stopHold clears the storm");
        HeroLifecycle.onLogout(hero);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_hero_entombed", timeoutTicks = 100)
    public static void anEntombedHeroLosesTheStormAndTheShield(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 10.5, 10.5);
        PowerData data = Powers.get(hero);
        sneakPress(hero, 5);
        check(data.is(Live.SANDSTORM), "the storm is up");
        ServerPlayer caster = hero(f, 15.5, 15.5);
        SandCocoonEntity cocoon = SandControl.entomb(level, caster, hero, 100, 0.0F, 20.0F, null);
        check(cocoon != null, "the sand closes on him");
        PowerTicker.tick(hero);
        check(!data.is(Live.SANDSTORM), "his hands are covered: the storm stops");
        press(hero, 4);
        check(!data.chaosShieldActive(), "and the shield will not rise");
        cocoon.release();
        helper.succeed();
    }

    private ApocalypseTests() {
    }
}

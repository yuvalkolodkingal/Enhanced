package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.MjolnirShatter;
import com.kolodkingal.enhanced.config.HelaConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.hela.ArmyMob;
import com.kolodkingal.enhanced.hela.BerserkerEntity;
import com.kolodkingal.enhanced.hela.BladeSpikeEntity;
import com.kolodkingal.enhanced.hela.FenrisEntity;
import com.kolodkingal.enhanced.hela.HelaAbilities;
import com.kolodkingal.enhanced.hela.HelaArmy;
import com.kolodkingal.enhanced.hela.HelaEvents;
import com.kolodkingal.enhanced.hela.HelaFight;
import com.kolodkingal.enhanced.hela.HelaRules;
import com.kolodkingal.enhanced.hela.NecroswordEntity;
import com.kolodkingal.enhanced.item.HelaHeaddressItem;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.CustomKitSupport.press;
import static com.kolodkingal.enhanced.gametest.CustomKitSupport.tap;
import static com.kolodkingal.enhanced.gametest.CustomKitSupport.tickN;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Hela, the hero (3.5, PLAN 7 and 12): becoming her with the headdress (and the winner's mark on it), every key, her dead
 * on her side and gone with her, the strength of Asgard, Death's Grasp against arrows, Mjolnir and Stormbreaker, and her
 * rows on a custom hero (R1: on another column, R3: the variant from Sneak, the body only with its row).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class HelaHeroTests {
    private static final String BATCH = "hela";

    static ServerPlayer hela(TestKit.Fixture f, double x, double z) {
        ServerPlayer p = f.player(x, FLOOR, z, Power.HELA);
        TestKit.look(p, 0.0F, 0.0F);
        f.onClose(() -> HelaArmy.dismiss(p));
        return p;
    }

    private static <T extends net.minecraft.world.entity.Entity> List<T> near(TestKit.Fixture f, Class<T> type) {
        Vec3 c = f.helper().absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        return f.level().getEntitiesOfClass(type, new AABB(c, c).inflate(14));
    }

    // ---- becoming her ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theHeadressMakesAPowerlessPlayerHela(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(5.5, FLOOR, 5.5);
        ItemStack headdress = HelaFight.headdressFor(p.getUUID());
        check(headdress.getItem() instanceof HelaHeaddressItem item && item.power() == Power.HELA, "an awakening item of Hela");
        p.setItemInHand(InteractionHand.MAIN_HAND, headdress);
        headdress.getItem().inventoryTick(headdress, f.level(), p, 0, true);
        p.tickCount = 20;
        headdress.getItem().inventoryTick(headdress, f.level(), p, 0, true);
        check(p.getData(ModAttachments.ASGARD).has(AsgardProgress.HELA_VICTOR), "the winner's headdress gives him the right to Gungnir");
        headdress.finishUsingItem(f.level(), p);
        check(Powers.is(p, Power.HELA), "held, it made her Hela");
        ServerPlayer other = f.player(8.5, FLOOR, 8.5);
        ItemStack theirs = HelaFight.headdressFor(p.getUUID());
        other.tickCount = 20;
        theirs.getItem().inventoryTick(theirs, f.level(), other, 0, true);
        check(!other.getData(ModAttachments.ASGARD).has(AsgardProgress.HELA_VICTOR), "someone else's headdress gives no right");
        helper.succeed();
    }

    // ---- the keys ----

    /** Her V takes the Sneak the key was pressed with (the client's), not the server's crouch (3.5 integration). */
    @GameTest(template = ARENA, batch = BATCH)
    public static void herKeysTakeTheSneakTheyWerePressedWith(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hela(f, 10.5, 3.5);
        PowerData data = Powers.get(p);
        p.setShiftKeyDown(false);
        com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, true, true);
        com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, false, true);
        check(!data.toggle(Toggle.HELA_FORM), "Sneak + V is the dash, not the form");
        check(!p.isShiftKeyDown(), "the server's crouch is left as it was");
        p.setShiftKeyDown(true);
        com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, true, false);
        com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, false, false);
        check(data.toggle(Toggle.HELA_FORM), "a plain V takes the form though the server thought she crouched");
        p.setShiftKeyDown(false);
        com.kolodkingal.enhanced.ability.AbilityHandler.onKey(p, 3, true, false);
        check(!data.toggle(Toggle.HELA_FORM), "and lets it go");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theVolleyThrowsThreeAndFiveWithTheForm(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hela(f, 10.5, 3.5);
        tap(p, 1, false);
        List<NecroswordEntity> blades = near(f, NecroswordEntity.class);
        check(blades.size() == 3, "three blades: " + blades.size());
        check(blades.stream().allMatch(b -> b.getOwner() == p && Math.abs(b.damage() - HelaConfig.VOLLEY_DAMAGE.get()) < 1.0E-4), "hers, 7 each");
        PowerData data = Powers.get(p);
        check(!data.isReady(1, f.level().getGameTime()), "its cooldown runs");
        blades.forEach(NecroswordEntity::discard);
        tap(p, 3, false);
        check(data.toggle(Toggle.HELA_FORM) && p.hasEffect(MobEffects.DAMAGE_BOOST) && p.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the form: strength");
        data.startCooldown(1, 0, 0);
        tap(p, 1, false);
        check(near(f, NecroswordEntity.class).size() == 5, "five with the form");
        check(Math.abs(HelaEvents.factor(p, ModDamageTypes.source(f.level(), ModDamageTypes.NECROSWORD, p, p)) - HelaAbilities.FORM_FACTOR) < 1.0E-4,
                "her blades 30 percent stronger");
        check(Math.abs(HelaEvents.factor(p, p.damageSources().playerAttack(p)) - 1.0F) < 1.0E-4, "outside Asgard, her fist is her fist");
        tap(p, 3, false);
        check(!data.toggle(Toggle.HELA_FORM) && !p.hasEffect(MobEffects.DAMAGE_BOOST), "the form fades");
        near(f, NecroswordEntity.class).forEach(NecroswordEntity::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 80)
    public static void theRainFallsWhereSheLooks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hela(f, 10.5, 3.5);
        TestKit.look(p, 0.0F, 40.0F);
        tap(p, 1, true);
        List<NecroswordEntity> blades = near(f, NecroswordEntity.class);
        check(blades.size() == HelaAbilities.RAIN_BLADES && blades.stream().allMatch(b -> b.falling() && b.hang() > 0), "twelve blades hang");
        Zombie zombie = EntityType.ZOMBIE.create(f.level());
        Vec3 under = blades.get(0).position().subtract(0.0, 3.0, 0.0);
        zombie.moveTo(under.x, HelaRules.groundAt(f.level(), under.x, under.y + 3, under.z), under.z);
        f.level().addFreshEntity(zombie);
        f.everyTick(60, t -> {
            if (zombie.getHealth() < zombie.getMaxHealth()) {
                zombie.discard();
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theWallAndTheBurstRaiseSpikes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hela(f, 10.5, 3.5);
        tap(p, 2, false);
        List<BladeSpikeEntity> wall = near(f, BladeSpikeEntity.class);
        check(!wall.isEmpty() && wall.size() <= HelaAbilities.WALL_LENGTH, "a wall: " + wall.size());
        wall.forEach(BladeSpikeEntity::discard);
        Powers.get(p).startCooldown(2, 0, 0);
        tap(p, 2, true);
        check(near(f, BladeSpikeEntity.class).size() == HelaAbilities.BURST_SPIKES, "a ring");
        check(!Powers.get(p).isReady(2, f.level().getGameTime()), "one cooldown for both");
        near(f, BladeSpikeEntity.class).forEach(BladeSpikeEntity::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theDashCutsThroughCreatures(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hela(f, 10.5, 3.5);
        Zombie zombie = EntityType.ZOMBIE.create(f.level());
        Vec3 at = f.helper().absoluteVec(new Vec3(10.5, FLOOR, 7.5));
        zombie.moveTo(at.x, at.y, at.z);
        f.level().addFreshEntity(zombie);
        Vec3 from = p.position();
        tap(p, 3, true);
        check(p.position().distanceTo(from) > 4.0, "she moved: " + p.position().distanceTo(from));
        check(zombie.getHealth() < zombie.getMaxHealth(), "and cut what she passed");
        zombie.discard();
        helper.succeed();
    }

    // ---- her dead ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void herDeadServeHerAndCrumbleWithHer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hela(f, 10.5, 10.5);
        tap(p, 4, false);
        List<BerserkerEntity> risen = near(f, BerserkerEntity.class);
        check(risen.size() == HelaConfig.RAISE_COUNT.get(), "three rise: " + risen.size());
        check(risen.stream().allMatch(b -> p.getUUID().equals(b.ownerId()) && b.isAlly()), "hers");
        check(risen.stream().noneMatch(b -> b.canAttack(p)), "never against her");
        check(!HelaRules.canHurt(p, risen.get(0)), "and her blades spare them");
        PowerData data = Powers.get(p);
        tickN(p, 1);
        check(data.is(Live.HELA_SUMMONING), "the HUD's state");
        data.startCooldown(4, 0, 0);
        tap(p, 4, true);
        check(near(f, FenrisEntity.class).size() == 1, "Fenris comes");
        Powers.setPower(p, Power.NONE);
        helper.runAfterDelay(2, () -> {
            check(near(f, ArmyMob.class).isEmpty(), "they crumble when she loses her power");
            helper.succeed();
        });
    }

    // ---- Death's Grasp ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theGraspCrumblesArrowsAndCatchesMjolnir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hela(f, 10.5, 10.5);
        ServerPlayer thor = f.player(10.5, FLOOR, 18.5);
        ServerLevel level = f.level();
        press(p, 5, false, true);
        check(Powers.get(p).is(Live.HELA_GRASPING), "the grasp is held");
        Vec3 front = p.getEyePosition().add(p.getLookAngle().scale(1.5));
        Arrow arrow = new Arrow(EntityType.ARROW, level);
        arrow.setPos(front);
        level.addFreshEntity(arrow);
        ThrownWeaponEntity hammer = new ThrownWeaponEntity(level, thor, new ItemStack(ModItems.MJOLNIR.get()));
        hammer.setPos(front);
        hammer.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(hammer);
        ThrownWeaponEntity axe = new ThrownWeaponEntity(level, thor, new ItemStack(ModItems.STORMBREAKER.get()));
        axe.setPos(front.add(0.0, 0.3, 0.0));
        axe.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(axe);
        tickN(p, 1);
        check(arrow.isRemoved(), "the arrow crumbled");
        check(hammer.isRemoved() && MjolnirShatter.isShattered(thor), "Mjolnir caught and shattered");
        check(axe.isRemoved(), "Stormbreaker stopped (it lies on the ground)");
        press(p, 5, false, false);
        check(!Powers.get(p).is(Live.HELA_GRASPING) && !Powers.get(p).isReady(5, level.getGameTime()), "let go: the cooldown");
        MjolnirShatter.onGone(thor);
        helper.succeed();
    }

    // ---- her body ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void herBodyAndTheStrengthOfAsgard(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hela(f, 10.5, 10.5);
        HelaAbilities.applyBody(p, Powers.get(p));
        check(Math.abs(p.getMaxHealth() - 30.0F) < 0.01F, "30 health: " + p.getMaxHealth());
        PowerData data = Powers.get(p);
        check(Math.abs(HelaEvents.factor(data, true, p.damageSources().playerAttack(p)) - HelaConfig.ASGARD_FACTOR.get()) < 1.0E-4,
                "in Asgard, her blows times the strength of Asgard");
        check(Math.abs(HelaEvents.factor(data, false, p.damageSources().playerAttack(p)) - 1.0F) < 1.0E-4, "elsewhere not");
        check(Math.abs(HelaEvents.factor(new PowerData(), true, p.damageSources().playerAttack(p)) - 1.0F) < 1.0E-4,
                "and not for one without her body");
        Powers.setPower(p, Power.NONE);
        HelaAbilities.applyBody(p, Powers.get(p));
        check(Math.abs(p.getMaxHealth() - 20.0F) < 0.01F, "none of it once she is no longer Hela");
        helper.succeed();
    }

    // ---- a custom hero with her rows ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void herRowsWorkForACustomHero(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = CustomKitSupport.hero(f, 10.5, 3.5, CustomKitSupport.def()
                .pick("hela.necroswords", 5).key(3, "hela.necroswords")
                .pick("hela.army", 5).key(1, "hela.army")
                .build());
        f.onClose(() -> HelaArmy.dismiss(p));
        TestKit.look(p, 0.0F, 0.0F);
        tap(p, 3, false);
        check(near(f, NecroswordEntity.class).size() == 3, "R1: the volley on V");
        PowerData data = Powers.get(p);
        check(!data.isReady(3, f.level().getGameTime()) && data.isReady(1, f.level().getGameTime()), "its cooldown on its own column");
        near(f, NecroswordEntity.class).forEach(NecroswordEntity::discard);
        data.startCooldown(3, 0, 0);
        TestKit.look(p, 0.0F, 40.0F);
        tap(p, 3, true);
        check(near(f, NecroswordEntity.class).stream().allMatch(NecroswordEntity::falling) && !near(f, NecroswordEntity.class).isEmpty(),
                "R3: Sneak gives the rain");
        near(f, NecroswordEntity.class).forEach(NecroswordEntity::discard);
        tap(p, 1, false);
        check(near(f, BerserkerEntity.class).size() == HelaConfig.RAISE_COUNT.get(), "the army on R");
        HelaAbilities.applyBody(p, Powers.get(p));
        check(Math.abs(p.getMaxHealth() - 20.0F) < 0.01F, "no body without the body's row");
        check(!data.has(HeroAbility.HELA_GODDESS), "and no form");
        helper.succeed();
    }

    /** PvP off: her blades and her dead spare another player; a teammate is spared even with PvP on. */
    @GameTest(template = ARENA, batch = "hela_pvp")
    public static void herBladesSparePlayersWithoutPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = hela(f, 10.5, 10.5);
        ServerPlayer other = f.player(14.5, FLOOR, 10.5);
        f.pvp(false);
        check(!HelaRules.canHurt(p, other), "no PvP: spared");
        f.pvp(true);
        check(HelaRules.canHurt(p, other), "PvP: hurt");
        var board = f.level().getScoreboard();
        PlayerTeam team = board.addPlayerTeam("hela_test_team");
        team.setAllowFriendlyFire(false);
        board.addPlayerToTeam(p.getScoreboardName(), team);
        board.addPlayerToTeam(other.getScoreboardName(), team);
        f.onClose(() -> board.removePlayerTeam(team));
        check(!HelaRules.canHurt(p, other), "a teammate is spared");
        tap(p, 4, false);
        for (BerserkerEntity b : near(f, BerserkerEntity.class)) {
            check(!b.canAttack(other), "her dead spare her team");
        }
        helper.succeed();
    }
}

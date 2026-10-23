package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.bifrost.BifrostKeys;
import com.kolodkingal.enhanced.client.custom.HeroAbilityAnims;
import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.OdinGestures;
import com.kolodkingal.enhanced.client.render.OdinHeroMath;
import com.kolodkingal.enhanced.client.render.RunCharacter;
import com.kolodkingal.enhanced.client.render.ShieldStyles;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.block.asgard.GungnirStandBlockEntity;
import com.kolodkingal.enhanced.config.GungnirConfig;
import com.kolodkingal.enhanced.config.OdinConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroPresets;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.odin.Coronation;
import com.kolodkingal.enhanced.odin.GungnirEntity;
import com.kolodkingal.enhanced.odin.GungnirStand;
import com.kolodkingal.enhanced.odin.OdinAbilities;
import com.kolodkingal.enhanced.odin.OdinDialogue;
import com.kolodkingal.enhanced.odin.RavenEntity;
import com.kolodkingal.enhanced.odin.Vault;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Odin, the Allfather (3.5, lane L3): Gungnir from its stand (only to who beat Hela, once, and again only for a lost one),
 * the bond (a stale copy crumbles), the coronation on the throne (his own Gungnir, no power, staying), Gungnir as the
 * Bifrost's key for its owner, every key of the hero (the blast and the spear it needs, the throw that comes home and R
 * that calls it back, the shield that stops a blow and an arrow, the heal that heals allies and not foes, Banish that
 * removes a monster but not a named one and spares the mild, the wave, the ravens and their time, the command, the
 * Odinsleep that heals and holds him), his body, the custom hero's rows (on another key, the Sneak cell, the blast from
 * the hand), the vault's door, his words, and the pure animation tables. Tests that keep server-wide state (a config, PvP,
 * Asgard's state) have batches of their own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class OdinTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** An Odin at full health with his body, and with nothing in his hands. */
    private static ServerPlayer odin(TestKit.Fixture f, double x, double z) {
        ServerPlayer odin = f.player(x, FLOOR, z, Power.ODIN);
        PowerTicker.tick(odin);
        odin.setHealth(odin.getMaxHealth());
        f.onClose(() -> HeroLifecycle.onLogout(odin));
        return odin;
    }

    /** Gives the player a Gungnir of his own (his bond moves on to it) in his main hand. */
    private static ItemStack arm(ServerPlayer player) {
        ItemStack spear = GungnirItem.bound(player, OdinAbilities.nextBond(player));
        player.setItemInHand(InteractionHand.MAIN_HAND, spear);
        return spear;
    }

    private static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        player.setShiftKeyDown(false);
    }

    private static void release(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, false);
    }

    /** A husk with no armour and 200 health that stands where it is put. */
    private static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    private static boolean hurt(LivingEntity e) {
        return e.getHealth() < e.getMaxHealth();
    }

    private static AsgardProgress progress(ServerPlayer player) {
        return ModAttachments.ASGARD.get(player);
    }

    private static void flag(ServerPlayer player, int flag) {
        ModAttachments.ASGARD.set(player, progress(player).with(flag));
    }

    private static long now(ServerPlayer player) {
        return player.level().getGameTime();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Gungnir: the stand, the bond, the Bifrost

    /** The stand gives Gungnir only to one who beat Hela, once; again only to its owner who has lost his. */
    @GameTest(template = ARENA, batch = "odin")
    public static void theStandGivesGungnirOnlyToAHelaVictorOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        freeGungnir(f);
        BlockPos stand = helper.absolutePos(new BlockPos(10, 2, 10));
        ServerPlayer nobody = f.player(9.5, FLOOR, 9.5);
        check(GungnirStand.take(nobody, stand) == GungnirStand.Taken.UNWORTHY, "one who did not stand against Hela is refused");
        check(nobody.getInventory().countItem(ModItems.GUNGNIR.get()) == 0 && !progress(nobody).has(AsgardProgress.GUNGNIR_TAKEN),
                "and gets nothing");

        ServerPlayer victor = f.player(10.5, FLOOR, 9.5);
        flag(victor, AsgardProgress.HELA_VICTOR);
        check(GungnirStand.take(victor, stand) == GungnirStand.Taken.TAKEN, "the victor takes it");
        ItemStack spear = victor.getMainHandItem();
        check(GungnirItem.isOwnGungnir(victor, spear), "his own, in his hand: " + spear);
        check(progress(victor).has(AsgardProgress.GUNGNIR_TAKEN) && progress(victor).gungnirBond() == 1, "marked, bond 1");
        check(GungnirStand.take(victor, stand) == GungnirStand.Taken.ALREADY && victor.getInventory().countItem(ModItems.GUNGNIR.get()) == 1,
                "a second click gives nothing more");

        ItemStack lost = victor.getMainHandItem().copy();
        victor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        check(GungnirStand.take(victor, stand) == GungnirStand.Taken.RETURNED && GungnirItem.isOwnGungnir(victor, victor.getMainHandItem()),
                "having lost it, its owner takes a new one");
        check(progress(victor).gungnirBond() == 2 && GungnirItem.stale(lost, f.level()), "and the lost one's bond is broken");
        helper.succeed();
    }

    /** The world's Gungnir has no owner now and when the test ends (3.6.1: it is kept in Asgard's state). */
    private static void freeGungnir(TestKit.Fixture f) {
        MinecraftServer server = f.level().getServer();
        AsgardState.get(server).setGungnirOwner(null);
        f.onClose(() -> AsgardState.get(server).setGungnirOwner(null));
    }

    /**
     * 3.6.1, {@code [gungnir] onePerWorld} (on): the first victor over Hela who takes Gungnir is its one owner in the
     * world; the stand is empty for another victor (refused, told whose it is, and drawn empty for him), still gives
     * its owner a new one when he lost his, and gives one to a player in Creative without making him the owner. Off:
     * every victor takes his own.
     */
    @GameTest(template = ARENA, batch = "odin_gungnir")
    public static void onlyOnePlayerInTheWorldTakesGungnir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        freeGungnir(f);
        MinecraftServer server = f.level().getServer();
        BlockPos stand = helper.absolutePos(new BlockPos(10, 2, 10));
        helper.setBlock(new BlockPos(10, 2, 10), ModBlocks.GUNGNIR_STAND.get());
        GungnirStandBlockEntity block = (GungnirStandBlockEntity) f.level().getBlockEntity(stand);
        ServerPlayer first = f.player(9.5, FLOOR, 9.5);
        ServerPlayer second = f.player(11.5, FLOOR, 9.5);
        ServerPlayer builder = f.player(10.5, FLOOR, 11.5, GameType.CREATIVE);
        for (ServerPlayer victor : List.of(first, second, builder)) {
            flag(victor, AsgardProgress.HELA_VICTOR);
        }
        check(GungnirStand.onePerWorld(), "one per world is the default");
        check(viewOf(block, f).shows(second) && viewOf(block, f).shows(first), "before: the stand shows it to all");

        check(GungnirStand.take(first, stand) == GungnirStand.Taken.TAKEN, "the first victor takes it");
        check(first.getUUID().equals(AsgardState.get(server).gungnirOwner()), "and is the world's owner");
        check(GungnirStand.take(second, stand) == GungnirStand.Taken.CLAIMED
                && second.getInventory().countItem(ModItems.GUNGNIR.get()) == 0
                && !progress(second).has(AsgardProgress.GUNGNIR_TAKEN), "another victor gets nothing");
        GungnirStandBlockEntity view = viewOf(block, f);
        check(!view.shows(second) && view.shows(first) && view.shows(builder),
                "the stand is drawn empty for him (not for its owner, nor for Creative)");

        first.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        check(GungnirStand.take(first, stand) == GungnirStand.Taken.RETURNED && GungnirItem.isOwnGungnir(first,
                first.getMainHandItem()), "its owner, having lost it, takes a new one (the bond rule)");
        check(GungnirStand.take(builder, stand) == GungnirStand.Taken.TAKEN
                && builder.getInventory().countItem(ModItems.GUNGNIR.get()) == 1, "Creative is exempt");
        check(first.getUUID().equals(AsgardState.get(server).gungnirOwner()), "and changes nothing of the owner");

        f.config(GungnirConfig.ONE_PER_WORLD, false);
        check(GungnirStand.take(second, stand) == GungnirStand.Taken.TAKEN, "off: every victor takes his own");
        check(viewOf(block, f).shows(second), "and the stand shows it to all");
        helper.succeed();
    }

    /** What a client is told of the stand: a copy made from the update tag the server sends. */
    private static GungnirStandBlockEntity viewOf(GungnirStandBlockEntity stand, TestKit.Fixture f) {
        GungnirStandBlockEntity view = new GungnirStandBlockEntity(stand.getBlockPos(), stand.getBlockState());
        view.handleUpdateTag(stand.getUpdateTag(f.level().registryAccess()), f.level().registryAccess());
        return view;
    }

    /** A copy whose bond is broken crumbles in an inventory and where it lies; the newest and an unowned one never do. */
    @GameTest(template = ARENA, batch = "odin")
    public static void aStaleGungnirCrumbles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer owner = f.player(5.5, FLOOR, 5.5);
        ItemStack old = arm(owner).copy();
        ItemStack fresh = arm(owner);
        check(GungnirItem.stale(old, f.level()) && !GungnirItem.stale(fresh, f.level()), "the older bond is stale, the newer is not");
        check(!GungnirItem.stale(new ItemStack(ModItems.GUNGNIR.get()), f.level()), "an unowned Gungnir never crumbles");

        ServerPlayer holder = f.player(7.5, FLOOR, 5.5);
        holder.getInventory().setItem(3, old.copy());
        holder.tickCount = 10;
        ItemStack held = holder.getInventory().getItem(3);
        held.getItem().inventoryTick(held, f.level(), holder, 3, false);
        check(holder.getInventory().getItem(3).isEmpty(), "the stale copy crumbles in another's inventory");

        ItemEntity lying = new ItemEntity(f.level(), owner.getX(), owner.getY(), owner.getZ() + 2, old.copy());
        f.level().addFreshEntity(lying);
        lying.getItem().onEntityItemUpdate(lying);
        check(lying.isRemoved(), "and where it lies");
        helper.succeed();
    }

    /** Gungnir opens the Bifrost for its owner only; anyone may wield it. */
    @GameTest(template = ARENA, batch = "odin")
    public static void gungnirOpensTheBifrostForItsOwnerOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer owner = f.player(5.5, FLOOR, 5.5);
        ItemStack spear = arm(owner);
        check(BifrostKeys.holdsKey(owner) && BifrostKeys.mayOpen(owner), "its owner holds a key of the Bifrost");
        ServerPlayer other = f.player(8.5, FLOOR, 5.5);
        other.setItemInHand(InteractionHand.MAIN_HAND, spear.copy());
        check(!BifrostKeys.holdsKey(other) && !BifrostKeys.mayOpen(other), "someone else's Gungnir is not his key");
        check(spear.getAttributeModifiers().modifiers().stream().anyMatch(e -> e.attribute().is(Attributes.ENTITY_INTERACTION_RANGE)
                && e.modifier().amount() == 1.0), "a spear reaches a block further");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The coronation

    /** Only a powerless player holding his own Gungnir who stays on the throne is crowned. */
    @GameTest(template = ARENA, batch = "odin", timeoutTicks = 200)
    public static void theCoronationNeedsHisOwnGungnirNoPowerAndTheThrone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos throne = helper.absolutePos(new BlockPos(10, 2, 10));
        ServerPlayer lender = f.player(4.5, FLOOR, 4.5);
        ServerPlayer borrower = f.player(10.5, FLOOR, 11.5);
        borrower.setItemInHand(InteractionHand.MAIN_HAND, arm(lender).copy());
        check(Coronation.sit(borrower, throne) == Coronation.Sat.SITTING, "someone else's Gungnir crowns nobody");
        ServerPlayer thor = f.player(11.5, FLOOR, 10.5, Power.THOR);
        arm(thor);
        check(Coronation.sit(thor, throne) == Coronation.Sat.HAS_POWER, "nor one with another power");
        ServerPlayer leaves = f.player(9.5, FLOOR, 10.5);
        arm(leaves);
        check(Coronation.sit(leaves, throne) == Coronation.Sat.CROWNING, "his own Gungnir, no power: the light begins");
        ServerPlayer heir = f.player(10.5, FLOOR, 9.5);
        arm(heir);
        check(Coronation.sit(heir, throne) == Coronation.Sat.CROWNING && Coronation.crowning(heir), "and for the heir");
        f.everyTick(Coronation.TICKS + 10, tick -> {
            if (tick == 5) {
                leaves.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); // he lets go of the spear
            }
            for (ServerPlayer p : List.of(leaves, heir, borrower)) {
                PowerTicker.tick(p);
            }
            if (tick == 6) {
                check(!Coronation.crowning(leaves), "letting go of the spear puts the light out");
            }
            if (tick < Coronation.TICKS - 2) {
                check(Powers.power(heir) == Power.NONE, "not before the light has poured (tick " + tick + ")");
                return false;
            }
            if (Powers.power(heir) != Power.ODIN) {
                return false;
            }
            check(progress(heir).has(AsgardProgress.CROWNED), "crowned");
            check(Powers.power(leaves) == Power.NONE && Powers.power(borrower) == Power.NONE, "nobody else");
            return true;
        });
        f.onClose(() -> HeroLifecycle.onLogout(heir));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The keys of the hero

    /** R: the blast needs Gungnir in his hand; it strikes what he looks at; with none of his own anywhere, a new one comes. */
    @GameTest(template = ARENA, batch = "odin")
    public static void theBlastNeedsGungnirAndStrikes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 3.5, 10.5);
        Husk husk = husk(helper, 12.5, 10.5);
        lookAt(odin, husk);
        PowerData data = Powers.get(odin);

        press(odin, 1, false);
        check(GungnirItem.isOwnGungnir(odin, odin.getMainHandItem()) && progress(odin).gungnirBond() == 1,
                "with no Gungnir of his own anywhere, R calls a new one to his hand");
        check(!hurt(husk) && !data.isReady(HeroAbility.ODIN_GUNGNIR, now(odin)), "it takes the press (and its rest)");

        ServerPlayer second = odin(f, 3.5, 6.5);
        arm(second);
        ItemStack spear = second.getMainHandItem().copy();
        second.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        second.getInventory().setItem(5, spear);
        Husk other = husk(helper, 12.5, 6.5);
        lookAt(second, other);
        press(second, 1, false);
        check(!hurt(other) && progress(second).gungnirBond() == 1, "his own in his pack: nothing (take it in hand)");
        second.setItemInHand(InteractionHand.MAIN_HAND, second.getInventory().getItem(5).copy());
        second.getInventory().setItem(5, ItemStack.EMPTY);
        lookAt(second, other);
        press(second, 1, false);
        check(hurt(other) && other.getLastDamageSource().is(ModDamageTypes.GUNGNIR) && other.getLastDamageSource().getEntity() == second,
                "Gungnir in hand: the bolt strikes, with the credit");
        assertNear(other.getMaxHealth() - other.getHealth(), OdinConfig.BLAST_DAMAGE.get(), 1.0E-3, "for the blast's damage");
        check(!Powers.get(second).isReady(HeroAbility.ODIN_GUNGNIR, now(second)), "and R rests");
        helper.succeed();
    }

    /** Sneak + R: the spear flies, strikes, turns at the wall and comes home to his hand. */
    @GameTest(template = ARENA, batch = "odin", timeoutTicks = 200)
    public static void theThrowFliesAndComesHome(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 2.5, 10.5);
        arm(odin);
        Husk husk = husk(helper, 8.5, 10.5);
        lookAt(odin, husk);
        press(odin, 1, true);
        GungnirEntity spear = OdinAbilities.spear(odin);
        PowerData data = Powers.get(odin);
        check(spear != null && odin.getMainHandItem().isEmpty() && data.is(Live.ODIN_THROWN), "it leaves his hand");
        boolean[] struck = {false};
        f.everyTick(150, tick -> {
            PowerTicker.tick(odin);
            if (hurt(husk)) {
                struck[0] = true;
            }
            if (OdinAbilities.spear(odin) != null || !GungnirItem.isOwnGungnir(odin, odin.getMainHandItem())) {
                return false;
            }
            check(struck[0] && husk.getLastDamageSource().is(ModDamageTypes.GUNGNIR), "it struck on its way");
            check(!data.is(Live.ODIN_THROWN), "and is home in his hand");
            return true;
        });
    }

    /** R while the spear flies calls it back at once. */
    @GameTest(template = ARENA, batch = "odin")
    public static void rAgainCallsTheSpearBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 2.5, 10.5);
        arm(odin);
        TestKit.look(odin, TestKit.EAST, 0.0F);
        press(odin, 1, true);
        GungnirEntity spear = OdinAbilities.spear(odin);
        check(spear != null && !spear.isReturning(), "it flies out");
        press(odin, 1, false);
        check(spear.isReturning(), "R turns it home");
        helper.succeed();
    }

    /** G held: the shield forms, takes a blow off its strength and turns an arrow to sparks; released, it rests. */
    @GameTest(template = ARENA, batch = "odin_fight", timeoutTicks = 100)
    public static void theShieldStopsABlowAndAnArrow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 10.5, 10.5);
        PowerData data = Powers.get(odin);
        press(odin, 2, false);
        check(data.chaosShieldActive(), "G raises it");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        f.everyTick(40, tick -> {
            PowerTicker.tick(odin);
            if (tick == OdinAbilities.SHIELD_WIND_UP + 2) {
                check(OdinAbilities.shieldUp(odin), "complete after its wind-up");
                float health = odin.getHealth();
                float strength = data.shieldStrength();
                odin.hurt(odin.damageSources().mobAttack(zombie), 5.0F);
                check(odin.getHealth() == health, "the blow does not reach him");
                assertNear(data.shieldStrength(), strength - 5.0F, 1.0E-3, "it costs the shield");
                Arrow arrow = new Arrow(f.level(), odin.getX() + 3.0, odin.getY() + 1.0, odin.getZ(), new ItemStack(Items.ARROW), null);
                arrow.setDeltaMovement(-1.5, 0.0, 0.0);
                f.level().addFreshEntity(arrow);
                PowerTicker.tick(odin);
                check(arrow.isRemoved(), "the arrow turns to sparks before it hits");
            }
            if (tick == OdinAbilities.SHIELD_WIND_UP + 6) {
                release(odin, 2);
                check(!data.chaosShieldActive() && !data.isReady(HeroAbility.ODIN_ODINFORCE, now(odin)), "released: down, and resting");
                return true;
            }
            return false;
        });
    }

    /** Sneak + G heals him and his allies and takes their harmful effects away; a monster near is not healed. */
    @GameTest(template = ARENA, batch = "odin_fight")
    public static void theHealHealsAlliesNotFoes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 10.5, 10.5);
        ServerPlayer friend = f.player(12.5, FLOOR, 10.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 10.5));
        odin.setHealth(10.0F);
        friend.setHealth(6.0F);
        friend.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        zombie.setHealth(5.0F);
        List<LivingEntity> healed = OdinAbilities.heal(odin, Powers.get(odin));
        float heal = OdinConfig.HEAL.get().floatValue();
        assertNear(odin.getHealth(), 10.0F + heal, 1.0E-3, "he is healed");
        assertNear(friend.getHealth(), 6.0F + heal, 1.0E-3, "his friend is healed");
        check(!friend.hasEffect(MobEffects.POISON), "and the poison is gone");
        check(zombie.getHealth() == 5.0F && !healed.contains(zombie), "the monster is not");
        check(!Powers.get(odin).isReady(HeroAbility.ODIN_ODINFORCE, now(odin)), "G rests");
        helper.succeed();
    }

    /** V: a monster is banished (gone, nothing left); a named one resists and is hurt; a cow is spared and costs nothing. */
    @GameTest(template = ARENA, batch = "odin_fight")
    public static void banishRemovesAMonsterButNotANamedOne(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 3.5, 10.5);
        PowerData data = Powers.get(odin);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(9.5, FLOOR, 10.5));
        lookAt(odin, cow);
        check(OdinAbilities.banish(odin, data) == OdinAbilities.Banished.NONE && cow.isAlive(), "a cow means no harm: spared");
        check(data.isReady(HeroAbility.ODIN_BANISH, now(odin)), "and that costs nothing");
        cow.discard();

        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 10.5));
        lookAt(odin, zombie);
        check(OdinAbilities.banish(odin, data) == OdinAbilities.Banished.GONE && zombie.isRemoved(), "the zombie is banished");
        check(f.level().getEntitiesOfClass(ItemEntity.class, zombie.getBoundingBox().inflate(3)).isEmpty(), "leaving nothing");
        check(!data.isReady(HeroAbility.ODIN_BANISH, now(odin)), "V rests");

        ServerPlayer second = odin(f, 3.5, 5.5);
        Zombie named = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 5.5));
        named.setCustomName(Component.literal("Gorm"));
        lookAt(second, named);
        float before = named.getHealth();
        check(OdinAbilities.banish(second, Powers.get(second)) == OdinAbilities.Banished.RESISTED && named.isAlive(), "a named one resists");
        check(named.getHealth() < before, "and takes the blow instead");
        var thanos = ModEntities.THANOS.get().create(f.level());
        check(thanos != null && OdinAbilities.resists(thanos), "a boss resists too");
        thanos.discard();
        helper.succeed();
    }

    /** Sneak + V: the wave strikes and throws every foe round him, never a player with PvP off. */
    @GameTest(template = ARENA, batch = "odin_fight")
    public static void theWaveStrikesAndThrowsFoes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 10.5, 10.5);
        Husk east = husk(helper, 14.5, 10.5);
        Husk west = husk(helper, 6.5, 10.5);
        Husk far = husk(helper, 10.5, 20.0);
        ServerPlayer friend = f.player(10.5, FLOOR, 13.5);
        float friendHealth = friend.getHealth();
        int count = OdinAbilities.wave(odin, Powers.get(odin));
        check(count == 2 && hurt(east) && hurt(west) && !hurt(far), "the two within reach are struck: " + count);
        check(east.getDeltaMovement().x > 0.5 && west.getDeltaMovement().x < -0.5, "and thrown away from him");
        check(east.getLastDamageSource().is(ModDamageTypes.ODINFORCE), "by the Odinforce");
        check(friend.getHealth() == friendHealth, "a player with PvP off is spared");
        helper.succeed();
    }

    /** B: two ravens fly out and circle; he cannot hurt them; their time over, they leave and the state clears. */
    @GameTest(template = ARENA, batch = "odin_config", timeoutTicks = 100)
    public static void theRavensFlyAndLeave(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(OdinConfig.RAVENS_SECONDS, 1);
        ServerPlayer odin = odin(f, 10.5, 10.5);
        PowerData data = Powers.get(odin);
        TestKit.look(odin, TestKit.EAST, 30.0F);
        press(odin, 4, false);
        List<RavenEntity> ravens = OdinAbilities.ravensOf(odin);
        check(ravens.size() == 2 && data.is(Live.ODIN_RAVENS), "Huginn and Muninn fly");
        check(ravens.stream().allMatch(r -> r.ownedBy(odin) && !r.report()), "his, scouting");
        check(!data.isReady(HeroAbility.ODIN_RAVENS, now(odin)), "B rests");
        RavenEntity huginn = ravens.get(0);
        check(!huginn.hurt(odin.damageSources().playerAttack(odin), 4.0F), "he cannot hurt his own raven");
        f.everyTick(60, tick -> {
            PowerTicker.tick(odin);
            if (tick < 15) {
                return false;
            }
            if (ravens.stream().anyMatch(r -> !r.isRemoved())) {
                return false;
            }
            check(!data.is(Live.ODIN_RAVENS), "gone, and the state clears");
            return true;
        });
    }

    /** Sneak + N: the Odinsleep holds him still, refuses his other keys, heals him and ends when he wakes. */
    @GameTest(template = ARENA, batch = "odin", timeoutTicks = 100)
    public static void theOdinsleepHealsAndHoldsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 10.5, 10.5);
        PowerData data = Powers.get(odin);
        press(odin, 5, true);
        check(!data.toggle(Toggle.ODINSLEEP), "whole, he is not weary: refused");
        odin.setHealth(10.0F);
        press(odin, 5, true);
        check(data.toggle(Toggle.ODINSLEEP), "hurt, he sleeps");
        check(odin.getAttributeValue(Attributes.MOVEMENT_SPEED) < 1.0E-6, "his legs do not answer");
        arm(odin);
        Husk husk = husk(helper, 16.5, 10.5);
        lookAt(odin, husk);
        press(odin, 1, false);
        check(!hurt(husk), "asleep he does not blast");
        float start = odin.getHealth();
        f.everyTick(50, tick -> {
            PowerTicker.tick(odin);
            if (tick < 45) {
                return false;
            }
            check(odin.getHealth() > start + odin.getMaxHealth() * OdinAbilities.SLEEP_HEAL_SHARE, "he heals: " + odin.getHealth());
            check(odin.hasEffect(MobEffects.DAMAGE_RESISTANCE), "guarded");
            press(odin, 5, false);
            check(!data.toggle(Toggle.ODINSLEEP), "N wakes him");
            check(odin.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.05, "and he can walk");
            check(!data.isReady(HeroAbility.ODIN_COMMAND, now(odin)), "the Odinsleep rests after it ends");
            return true;
        });
    }

    /** N: the command is given (the warriors are the Asgardians'); its rest begins. */
    @GameTest(template = ARENA, batch = "odin")
    public static void theCommandIsGiven(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 10.5, 10.5);
        int answered = OdinAbilities.command(odin, Powers.get(odin));
        check(answered >= 0 && !Powers.get(odin).isReady(HeroAbility.ODIN_COMMAND, now(odin)), "the command rests: " + answered);
        helper.succeed();
    }

    /** His body: +12 health, +6 armour, +0.5 knockback resistance; a custom hero's by its strength; none on anyone else. */
    @GameTest(template = ARENA, batch = "odin")
    public static void theBodyIsByItsRow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 10.5, 10.5);
        assertNear(odin.getMaxHealth(), 20.0 + OdinAbilities.BODY_HEALTH_BONUS, 1.0E-4, "his health");
        assertNear(odin.getAttributeValue(Attributes.ARMOR), OdinAbilities.BODY_ARMOR_BONUS, 1.0E-4, "his armour");
        assertNear(odin.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), OdinAbilities.BODY_KNOCKBACK_RESISTANCE, 1.0E-4, "his footing");
        check(FlightProfile.of(Power.ODIN, false) == null && !Power.ODIN.canFly(), "he walks");
        ServerPlayer custom = CustomKitSupport.hero(f, 5.5, 5.5, CustomKitSupport.def().pick("odin.body", 5).pick("odin.ravens", 3)
                .key(1, "odin.ravens").build());
        PowerTicker.tick(custom);
        check(custom.getMaxHealth() > 20.0, "a custom hero with his body has more health: " + custom.getMaxHealth());
        ServerPlayer plain = CustomKitSupport.hero(f, 15.5, 5.5, CustomKitSupport.def().pick("odin.ravens", 3).key(1, "odin.ravens").build());
        PowerTicker.tick(plain);
        assertNear(plain.getMaxHealth(), 20.0, 1.0E-4, "without it, none");
        f.onClose(() -> {
            HeroLifecycle.onLogout(custom);
            HeroLifecycle.onLogout(plain);
        });
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The custom hero (3.4's rules)

    /** R1/R3/R7: on another key the blast leaves a custom hero's bare hand; its Sneak cell throws only a Gungnir he holds. */
    @GameTest(template = ARENA, batch = "odin", timeoutTicks = 120)
    public static void aCustomHeroBlastsFromHisHandAndThrowsOnlyGungnir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero def = CustomKitSupport.def().pick("odin.gungnir", 5).key(3, "odin.gungnir").build();
        ServerPlayer bare = CustomKitSupport.hero(f, 3.5, 4.5, def);
        f.onClose(() -> HeroLifecycle.onLogout(bare));
        check(CustomKitSupport.column(bare, HeroAbility.ODIN_GUNGNIR) == 3, "on V");
        Husk husk = husk(helper, 12.5, 4.5);
        lookAt(bare, husk);
        CustomKitSupport.tap(bare, 3, false);
        check(hurt(husk), "the blast leaves his bare hand");
        check(!Powers.get(bare).isReady(3, now(bare)) && Powers.get(bare).isReady(1, now(bare)), "the rest is on V's slot, not R's");

        ServerPlayer sneaky = CustomKitSupport.hero(f, 3.5, 10.5, def);
        f.onClose(() -> HeroLifecycle.onLogout(sneaky));
        Husk second = husk(helper, 12.5, 10.5);
        lookAt(sneaky, second);
        CustomKitSupport.tap(sneaky, 3, true);
        check(hurt(second) && OdinAbilities.spear(sneaky) == null, "Sneak with no Gungnir in hand: the blast, not a throw");

        ServerPlayer thrower = CustomKitSupport.hero(f, 3.5, 16.5, def);
        f.onClose(() -> HeroLifecycle.onLogout(thrower));
        arm(thrower);
        TestKit.look(thrower, TestKit.EAST, 0.0F);
        CustomKitSupport.tap(thrower, 3, true);
        check(OdinAbilities.spear(thrower) != null && Powers.get(thrower).is(Live.ODIN_THROWN), "with Gungnir in hand, Sneak throws it");
        f.everyTick(100, tick -> {
            PowerTicker.tick(thrower);
            return OdinAbilities.spear(thrower) == null && GungnirItem.isGungnir(thrower.getMainHandItem());
        });
    }

    /** R4: a custom hero with only the ravens keeps none of Odin's other upkeep (no shield, no sleep), and a body only by its row. */
    @GameTest(template = ARENA, batch = "odin")
    public static void aCustomHeroGetsOnlyHisRowsUpkeep(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = CustomKitSupport.hero(f, 10.5, 10.5, CustomKitSupport.def().pick("odin.odinforce", 3).key(2, "odin.odinforce").build());
        f.onClose(() -> HeroLifecycle.onLogout(hero));
        PowerData data = Powers.get(hero);
        CustomKitSupport.press(hero, 2, false, true);
        check(data.chaosShieldActive(), "G holds the Odinforce Shield for a custom hero too");
        check(ShieldStyles.of(data) != null && ShieldStyles.of(data) == ShieldStyles.of(Power.ODIN), "drawn in Odin's style");
        CustomKitSupport.press(hero, 2, false, false);
        check(!data.chaosShieldActive(), "released, it falls");
        hero.setHealth(8.0F);
        CustomKitSupport.tap(hero, 5, true);
        check(!data.toggle(Toggle.ODINSLEEP), "no Odinsleep without the command's row");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // PvP

    /** Banish spares a player while PvP is off; with it on, the Bifrost takes him home. */
    @GameTest(template = ARENA, batch = "odin_pvp")
    public static void banishSendsAPlayerHomeOnlyWithPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer odin = odin(f, 3.5, 10.5);
        ServerPlayer target = f.player(9.5, FLOOR, 10.5);
        lookAt(odin, target);
        f.pvp(false);
        check(OdinAbilities.banish(odin, Powers.get(odin)) == OdinAbilities.Banished.NONE && !Bifrost.inBifrost(target), "PvP off: spared");
        f.pvp(true);
        lookAt(odin, target);
        check(OdinAbilities.banish(odin, Powers.get(odin)) == OdinAbilities.Banished.SENT_HOME && Bifrost.inBifrost(target),
                "PvP on: the Bifrost gathers round him");
        f.onClose(() -> Bifrost.forget(target));
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The vault and his words

    /** The vault's door opens once (its blocks gone) and tells its listeners; a 3.6 place is kept empty in the vault. */
    @GameTest(template = ARENA, batch = "odin_realm", timeoutTicks = 200)
    public static void theVaultOpensOnce(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerLevel asgard = AsgardTestSupport.ensureAsgard(helper.getLevel().getServer());
        List<ServerLevel> told = new ArrayList<>();
        Consumer<ServerLevel> listener = told::add;
        Vault.OPENED.add(listener);
        f.onClose(() -> Vault.OPENED.remove(listener));
        for (BlockPos pos : BlockPos.betweenClosed(CityPlan.VAULT_DOOR.minX(), CityPlan.VAULT_DOOR.minY(), CityPlan.VAULT_DOOR.minZ(),
                CityPlan.VAULT_DOOR.maxX(), CityPlan.VAULT_DOOR.maxY(), CityPlan.VAULT_DOOR.maxZ())) {
            asgard.getChunk(pos);
        }
        // The door of the city (lane L1's), shut again afterwards: Asgard's level is shared by every test.
        com.kolodkingal.enhanced.asgard.CityBuild.setVaultDoor(asgard, false);
        f.onClose(() -> com.kolodkingal.enhanced.asgard.CityBuild.setVaultDoor(asgard, false));
        check(!Vault.isOpen(asgard), "shut");
        Vault.open(asgard);
        check(Vault.isOpen(asgard) && told.size() == 1, "open, and told once");
        Vault.open(asgard);
        check(told.size() == 1, "an open door is not opened again");
        check(CityPlan.VAULT.isInside(Vault.RESERVED_PEDESTAL) && !Vault.RESERVED_PEDESTAL.equals(CityPlan.GUNGNIR_STAND),
                "the place kept for 3.6 is in the vault, apart from Gungnir's stand");
        helper.succeed();
    }

    /** His words follow the way to the throne; every line and answer has its words in English and Hebrew. */
    @GameTest(template = ARENA, batch = "odin")
    public static void odinsWordsFollowTheWayToTheThrone(GameTestHelper helper) throws IOException {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        AsgardState asleep = new AsgardState();
        AsgardState awake = new AsgardState();
        awake.setOdinAwake(true);
        check(node(player, asleep).equals("asleep"), "asleep he does not answer");
        check(node(player, awake).equals("greet"), "awake he greets");
        flag(player, AsgardProgress.HELA_VICTOR);
        check(node(player, awake).equals("victor"), "to one who beat Hela: the vault");
        flag(player, AsgardProgress.GUNGNIR_TAKEN);
        check(node(player, awake).equals("bearer"), "to the bearer: the throne");
        flag(player, AsgardProgress.CROWNED);
        check(node(player, awake).equals("crowned"), "to the crowned: the realm");
        ServerPlayer hela = f.player(12.5, FLOOR, 10.5, Power.HELA);
        check(node(hela, awake).equals("hela_heir"), "to Hela's heir: a warning");

        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        List<String> keys = new ArrayList<>();
        for (String node : OdinDialogue.NODES) {
            keys.add("npc.enhanced.odin." + node);
        }
        for (String answer : OdinDialogue.ANSWERS) {
            keys.add("npc.enhanced.odin." + answer);
        }
        for (String option : OdinDialogue.OPTIONS) {
            keys.add("npc.enhanced.option." + option);
        }
        keys.add("npc.enhanced.odin.comfortable");
        for (String key : keys) {
            check(en.has(key) && he.has(key), "the words of " + key);
            check(en.get(key).getAsString().length() <= 140 && he.get(key).getAsString().length() <= 140, key + " fits a line");
        }
        for (AsgardDialogue.Node node : AsgardDialogue.nodes(Role.ODIN)) {
            check(OdinDialogue.NODES.contains(node.key()) && node.options().size() <= 3, "a node of his: " + node.key());
        }
        check(HeroPresets.HEROES.contains("odin"), "the Hero Creator offers an Odin-like hero");
        helper.succeed();
    }

    /** The node Odin says to this player (the highest priority whose condition holds). */
    private static String node(ServerPlayer player, AsgardState state) {
        AsgardDialogue.Ctx ctx = new AsgardDialogue.Ctx(player, player, Role.ODIN, progress(player), state);
        AsgardDialogue.Node best = null;
        for (AsgardDialogue.Node node : AsgardDialogue.nodes(Role.ODIN)) {
            if (node.when().test(ctx) && (best == null || node.priority() > best.priority())) {
                best = node;
            }
        }
        return best == null ? "" : best.key();
    }

    private static JsonObject lang(String name) throws IOException {
        var file = ModList.get().getModFileById(Enhanced.MODID).getFile();
        try (InputStream in = Files.newInputStream(file.findResource("assets", Enhanced.MODID, "lang", name + ".json"))) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The animation (pure)

    /** Every key is acted; each cooldown tells its use by its length; his states have gestures; the rows are animated. */
    @GameTest(template = ARENA, batch = "odin_pure")
    public static void everyKeyIsActedAndTheCooldownsTellTheirUses(GameTestHelper helper) {
        check(GestureRoutes.acting(Power.ODIN, 1).equals("cooldown") && GestureRoutes.acting(Power.ODIN, 2).equals("flag:CHAOS_SHIELD")
                && GestureRoutes.acting(Power.ODIN, 4).equals("live:ODIN_RAVENS"), "how his keys are acted");
        check(OdinHeroMath.cooldownGesture(1, OdinConfig.BLAST_COOLDOWN.get()) == GestureId.ODIN_BLAST, "R's short rest: the blast");
        check(OdinHeroMath.cooldownGesture(1, OdinConfig.THROW_COOLDOWN.get()) == null, "the throw is acted by its state");
        check(OdinHeroMath.cooldownGesture(1, OdinAbilities.RECALL_COOLDOWN) == GestureId.ODIN_CATCH, "a new Gungnir: the catch");
        check(OdinHeroMath.cooldownGesture(2, OdinConfig.HEAL_COOLDOWN.get()) == GestureId.ODIN_HEAL, "G's long rest: the heal");
        check(OdinHeroMath.cooldownGesture(2, OdinConfig.SHIELD_COOLDOWN.get()) == null, "the shield's rest: its flag acts it");
        check(OdinHeroMath.cooldownGesture(3, OdinConfig.BANISH_COOLDOWN.get()) == GestureId.ODIN_BANISH
                && OdinHeroMath.cooldownGesture(3, OdinConfig.WAVE_COOLDOWN.get()) == GestureId.ODIN_WAVE, "Banish and the Wave");
        check(OdinHeroMath.cooldownGesture(5, OdinConfig.COMMAND_COOLDOWN.get()) == GestureId.ODIN_COMMAND
                && OdinHeroMath.cooldownGesture(5, OdinConfig.ODINSLEEP_COOLDOWN.get()) == null, "the Command, not the Odinsleep's rest");
        check(GestureRoutes.toggle(Power.ODIN, Toggle.ODINSLEEP, true) == GestureId.ODIN_KNEEL
                && GestureRoutes.toggle(Power.ODIN, Toggle.ODINSLEEP, false) == GestureId.ODIN_RISE, "he kneels and rises");
        check(GestureRoutes.live(Power.ODIN, Live.ODIN_THROWN, true) == GestureId.ODIN_THROW
                && GestureRoutes.live(Power.ODIN, Live.ODIN_RAVENS, true) == GestureId.ODIN_RAVENS, "the throw and the ravens");
        check(GestureRoutes.reason(Live.ODIN_THROWN).contains("ODIN_THROW"), "the throw's state names its gesture");
        check(GestureRoutes.startedElsewhere(GestureId.ODIN_CROWN), "the coronation's gesture is its payload's");
        check(RunCharacter.of(Power.ODIN, false) == RunCharacter.ODIN, "his stride");
        check(Stance.ODIN_SHIELD.aimed && Stance.ODIN_SLEEP.aimed && !Stance.ODIN_REGAL.aimed, "the held stances and the bearing");
        for (HeroAbility a : List.of(HeroAbility.ODIN_GUNGNIR, HeroAbility.ODIN_ODINFORCE, HeroAbility.ODIN_BANISH, HeroAbility.ODIN_RAVENS,
                HeroAbility.ODIN_COMMAND, HeroAbility.ODIN_BODY)) {
            check(HeroAbilityAnims.covers(a), a.id() + " is animated");
        }
        check(HeroAbilityAnims.castMain(HeroAbility.ODIN_GUNGNIR) == GestureId.ODIN_BLAST
                && HeroAbilityAnims.castAlt(HeroAbility.ODIN_ODINFORCE) == GestureId.ODIN_HEAL, "the cast marker's gestures");
        check(HeroAbilityAnims.owners(GestureRoutes.Flag.CHAOS_SHIELD).contains(HeroAbility.ODIN_ODINFORCE), "the shield's flag is his too");
        helper.succeed();
    }

    /** His gestures are well formed; those that hand over to a stance end on its pose; the cocoon and the wave never jump. */
    @GameTest(template = ARENA, batch = "odin_pure")
    public static void theGesturesAreWellFormedAndHandOver(GameTestHelper helper) {
        List<GestureId> ids = List.of(GestureId.ODIN_BLAST, GestureId.ODIN_THROW, GestureId.ODIN_CATCH, GestureId.ODIN_SHIELD_UP,
                GestureId.ODIN_SHIELD_DOWN, GestureId.ODIN_HEAL, GestureId.ODIN_BANISH, GestureId.ODIN_WAVE, GestureId.ODIN_RAVENS,
                GestureId.ODIN_COMMAND, GestureId.ODIN_KNEEL, GestureId.ODIN_RISE, GestureId.ODIN_CROWN);
        for (GestureId id : ids) {
            Gestures.Def def = Gestures.get(id);
            check(def != null && def.problems().isEmpty(), id + ": " + (def == null ? "no keys" : def.problems()));
        }
        Gestures.ArmKey shield = Gestures.get(GestureId.ODIN_SHIELD_UP).m.get(Gestures.get(GestureId.ODIN_SHIELD_UP).m.size() - 1);
        check(shield.x == OdinGestures.SHIELD_M[0] && shield.y == OdinGestures.SHIELD_M[1] && shield.z == OdinGestures.SHIELD_M[2],
                "the shield's gesture ends on the stance's pose");
        Gestures.ArmKey kneel = Gestures.get(GestureId.ODIN_KNEEL).m.get(Gestures.get(GestureId.ODIN_KNEEL).m.size() - 1);
        check(kneel.x == OdinGestures.SLEEP_M[0] && kneel.z == OdinGestures.SLEEP_M[2], "the kneel ends on the Odinsleep's pose");
        float previous = OdinHeroMath.cocoonRadius(0.0F);
        for (float t = 0.05F; t < 400.0F; t += 0.05F) {
            float r = OdinHeroMath.cocoonRadius(t);
            check(Math.abs(r - previous) < 0.02F && r > 0.8F && r < 1.7F, "the cocoon breathes smoothly at " + t);
            previous = r;
        }
        float wave = 0.0F;
        for (float t = 0.0F; t <= 10.0F; t += 0.1F) {
            float r = OdinHeroMath.waveRadius(t, 8.0F);
            check(r >= wave - 1.0E-4F && r <= 8.0F + 1.0E-4F, "the wave only grows, to its radius");
            wave = r;
        }
        helper.succeed();
    }
}

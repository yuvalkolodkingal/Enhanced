package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.SoulStoneConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.MindStonePowers;
import com.kolodkingal.enhanced.stones.PowerBeam;
import com.kolodkingal.enhanced.stones.RealityBubbles;
import com.kolodkingal.enhanced.stones.SoulDrain;
import com.kolodkingal.enhanced.stones.SoulStonePowers;
import com.kolodkingal.enhanced.stones.SpaceRealms;
import com.kolodkingal.enhanced.stones.StoneCooldowns;
import com.kolodkingal.enhanced.stones.StoneEvents;
import com.kolodkingal.enhanced.stones.StoneUse;
import com.kolodkingal.enhanced.stones.StoneZones;
import com.kolodkingal.enhanced.stones.TimeRewind;
import com.kolodkingal.enhanced.stones.TimeStonePowers;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
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
 * The six stones' powers (3.6, lane L2): the Reality Stone's bubbles, Transmute and Illusion; the Mind Stone's beam,
 * through Loki's Scepter too, and its command; the Time Stone's Slow Time, Rewind and the crops (sorcerers only); the
 * Power Stone's Purple Beam, its craters and the bolt; the Space Stone's step and the Realm Picker; the Soul Stone's
 * drain and sight; and the custom heroes' stone rows. Non-gods cannot use Reality, Power or Soul at all.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class StonePowerTests {

    // ============================================================================================================
    // Helpers

    /** A player of {@code power} with the bare stone in his main hand. */
    static ServerPlayer holder(TestKit.Fixture f, double x, double z, Power power, StoneKind kind) {
        ServerPlayer player = f.player(x, FLOOR, z, power);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.stone(kind)));
        return player;
    }

    /** A husk that stands still and has no armour (the sun does not burn it). */
    static Zombie husk(GameTestHelper helper, double x, double z) {
        Zombie husk = helper.spawn(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.setNoAi(true);
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        return husk;
    }

    static float lost(LivingEntity creature) {
        return creature.getMaxHealth() - creature.getHealth();
    }

    /** Whether the player was told the line of {@code key} on his action bar since the last look. */
    static boolean told(ServerPlayer player, String key) {
        for (Object sent : TestKit.sentPackets(player)) {
            if (sent instanceof ClientboundSystemChatPacket chat && chat.overlay() && key.equals(key(chat.content()))) {
                return true;
            }
        }
        return false;
    }

    private static String key(Component component) {
        return component.getContents() instanceof TranslatableContents t ? t.getKey() : "";
    }

    /** The stone in his main hand is used (a right-click; Sneak for {@code alt}), as the game's use would. */
    static void use(ServerPlayer player, StoneKind kind, boolean alt) {
        StoneEvents.useInHand(player, InteractionHand.MAIN_HAND, kind, StoneUse.Source.BARE, alt);
    }

    static void tick(ServerPlayer player, int n) {
        for (int i = 0; i < n; i++) {
            player.tickCount++;
            StoneUse.tick(player);
        }
    }

    /**
     * The player's stones run for {@code n} real server ticks (the game time moves on, as the beams' and the drain's
     * rhythm need), then {@code then} runs. Nothing ticks a player without a client, so the test ticks his stones.
     */
    static void ticking(GameTestHelper helper, ServerPlayer player, int n, Runnable then) {
        helper.runAfterDelay(1, () -> {
            tick(player, 1);
            if (n <= 1) {
                then.run();
            } else {
                ticking(helper, player, n - 1, then);
            }
        });
    }

    /** A projectile at {@code from} flying at the player's chest. */
    static void aim(Projectile projectile, Vec3 from, ServerPlayer target, double speed) {
        projectile.setPos(from.x, from.y, from.z);
        Vec3 to = target.position().add(0.0, 1.0, 0.0).subtract(from).normalize().scale(speed);
        projectile.setDeltaMovement(to);
        target.level().addFreshEntity(projectile);
    }

    // ============================================================================================================
    // The Reality Stone

    /** An arrow, a ghast's fireball and an Ultron bolt flying at Thor become bubbles; his own arrow does not. */
    @GameTest(template = ARENA, batch = "stones_reality")
    public static void projectilesAtTheRealityStoneTurnToBubbles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer thor = holder(f, 10.5, 10.5, Power.THOR, StoneKind.REALITY);
        Vec3 at = thor.position();
        Arrow arrow = new Arrow(EntityType.ARROW, level);
        aim(arrow, at.add(3.0, 1.0, 0.0), thor, 1.5);
        LargeFireball fireball = new LargeFireball(EntityType.FIREBALL, level);
        aim(fireball, at.add(-3.0, 1.0, 0.0), thor, 0.5);
        // (A bolt with no fight and no player behind it is swept as an orphan: a hero Ultron shoots this one.)
        ServerPlayer ultron = f.player(10.5, FLOOR, 16.5);
        Projectile bolt = ModEntities.ULTRON_BOLT.get().create(level);
        bolt.setOwner(ultron);
        aim(bolt, at.add(0.0, 1.0, 3.0), thor, 1.0);
        Arrow own = new Arrow(EntityType.ARROW, level);
        own.setOwner(thor);
        aim(own, at.add(0.0, 1.0, -3.0), thor, 1.5);
        Projectile hammer = ModEntities.THROWN_WEAPON.get().create(level);
        aim(hammer, at.add(2.5, 1.0, 2.5), thor, 0.6);
        check(RealityBubbles.active(thor, Powers.get(thor)), "the stone in Thor's hand is awake");
        tick(thor, 1);
        check(arrow.isRemoved(), "the arrow pops");
        check(fireball.isRemoved(), "the fireball pops");
        check(bolt.isRemoved(), "the bolt pops");
        check(!own.isRemoved(), "his own arrow flies on");
        check(!hammer.isRemoved(), "a thrown Mjolnir is reality-proof");
        check(RealityBubbles.count(thor) == 3, "three bubbles counted: " + RealityBubbles.count(thor));
        helper.succeed();
    }

    /** Non-gods cannot use Reality, Power or Soul at all: a powerless player is told so and nothing happens. */
    @GameTest(template = ARENA, batch = "stones_reality")
    public static void aMortalCannotUseRealityPowerOrSoul(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer mortal = holder(f, 5.5, 5.5, Power.NONE, StoneKind.REALITY);
        check(!RealityBubbles.active(mortal, Powers.get(mortal)), "no bubbles for a mortal");
        Arrow arrow = new Arrow(EntityType.ARROW, f.level());
        aim(arrow, mortal.position().add(3.0, 1.0, 0.0), mortal, 1.5);
        tick(mortal, 1);
        check(!arrow.isRemoved(), "the arrow is not turned");
        TestKit.sentPackets(mortal);
        for (StoneKind kind : List.of(StoneKind.REALITY, StoneKind.POWER, StoneKind.SOUL)) {
            mortal.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.stone(kind)));
            use(mortal, kind, false);
            check(told(mortal, "message.enhanced.stones.too_strong"), kind + ": too strong for him");
            check(!StoneUse.holding(mortal) && StoneCooldowns.ready(mortal, kind, f.level().getGameTime()),
                    kind + ": nothing started");
        }
        ServerPlayer hulk = holder(f, 8.5, 5.5, Power.HULK, StoneKind.POWER);
        use(hulk, StoneKind.POWER, false);
        check(!StoneUse.holding(hulk), "the Hulk is no Bearer either");
        helper.succeed();
    }

    /** The Transmute turns the zombie Thor looks at to stone, and starts the cooldown. */
    @GameTest(template = ARENA, batch = "stones_reality")
    public static void theTransmuteTurnsAZombieToStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 3.5, 10.5, Power.THOR, StoneKind.REALITY);
        Zombie husk = husk(helper, 9.5, 10.5);
        lookAt(thor, husk);
        use(thor, StoneKind.REALITY, false);
        check(husk.hasEffect(ModEffects.PETRIFIED), "the husk is stone");
        long left = StoneCooldowns.remaining(thor, StoneKind.REALITY, f.level().getGameTime());
        check(left == StonesConfig.REALITY_TRANSMUTE_COOLDOWN.get(), "the cooldown runs: " + left);
        check(thor.getCooldowns().isOnCooldown(ModItems.REALITY_STONE.get()), "and the stone shows it");
        helper.succeed();
    }

    /** The Illusion: Thor vanishes, a red copy walks off, and the zombie that hunted him hunts it. */
    @GameTest(template = ARENA, batch = "stones_reality")
    public static void theIllusionDrawsTheZombieAway(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 10.5, 10.5, Power.THOR, StoneKind.REALITY);
        Zombie husk = husk(helper, 4.5, 10.5);
        husk.setTarget(thor);
        use(thor, StoneKind.REALITY, true);
        check(thor.hasEffect(MobEffects.INVISIBILITY), "Thor is invisible");
        check(husk.getTarget() instanceof LokiIllusionEntity copy && RealityBubbles.isStoneIllusion(copy)
                && copy.mode() == LokiIllusionEntity.Mode.REALITY,
                "the husk hunts the stone's copy: " + husk.getTarget());
        helper.succeed();
    }

    // ============================================================================================================
    // The Mind Stone

    /** The beam hurts the husk twice a second and confuses it. */
    @GameTest(template = ARENA, batch = "stones_mind")
    public static void theMindBeamHurtsAndConfuses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 3.5, 10.5, Power.THOR, StoneKind.MIND);
        Zombie husk = husk(helper, 8.5, 10.5);
        lookAt(thor, husk);
        use(thor, StoneKind.MIND, false);
        check(StoneUse.holding(thor, StoneKind.MIND) && thor.isUsingItem(), "the beam is held");
        check(ContinuousBeam.isFiring(thor), "and burns");
        ticking(helper, thor, 12, () -> {
            assertNear(lost(husk), 2 * StonesConfig.MIND_BEAM_DAMAGE.get(), 0.01, "two hits");
            check(husk.hasEffect(ModEffects.CONFUSED), "the husk is confused");
            thor.stopUsingItem();
            tick(thor, 1);
            check(!StoneUse.holding(thor) && !ContinuousBeam.isFiring(thor), "letting go ends it");
            check(StoneCooldowns.remaining(thor, StoneKind.MIND, f.level().getGameTime()) == 20, "a short cooldown");
            helper.succeed();
        });
    }

    /**
     * A powerless player fires the beam through Loki's Scepter with Sneak and an empty other hand; it does not
     * explode.
     */
    @GameTest(template = ARENA, batch = "stones_mind")
    public static void theScepterFiresTheMindBeamForAMortal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer mortal = f.player(3.5, FLOOR, 10.5);
        mortal.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        Zombie husk = husk(helper, 8.5, 10.5);
        lookAt(mortal, husk);
        mortal.setShiftKeyDown(true);
        check(ScepterRituals.useScepter(mortal, InteractionHand.MAIN_HAND, false), "a hold begins");
        check(ScepterRituals.mindBeamHold(mortal), "it is the Mind Beam's, not the Titan's");
        mortal.startUsingItem(InteractionHand.MAIN_HAND);
        check(mortal.getUseItemRemainingTicks() > ScepterRituals.HOLD_TICKS, "it lasts until let go");
        tick(mortal, 2);
        check(lost(husk) > 0.0F && mortal.isAlive() && mortal.getHealth() == mortal.getMaxHealth(),
                "the husk is hurt and the mortal is whole");
        mortal.setShiftKeyDown(false);
        mortal.releaseUsingItem();
        tick(mortal, 1);
        check(!StoneUse.holding(mortal), "letting go ends it");

        ServerPlayer creative = f.player(3.5, FLOOR, 4.5, GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        check(!ScepterRituals.useScepter(creative, InteractionHand.MAIN_HAND, false) && !StoneUse.holding(creative),
                "a plain use with nothing in the other hand is the old row (no beam)");
        helper.succeed();
    }

    /** The Mind Command makes the husk Thor's ally: it may not take him as its target. */
    @GameTest(template = ARENA, batch = "stones_mind")
    public static void theMindCommandMakesAZombieAnAlly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 3.5, 10.5, Power.THOR, StoneKind.MIND);
        Zombie husk = husk(helper, 8.5, 10.5);
        lookAt(thor, husk);
        use(thor, StoneKind.MIND, true);
        check(thor.getUUID().equals(MindStonePowers.commanderOf(husk)), "the husk is commanded");
        husk.setTarget(thor);
        check(husk.getTarget() != thor, "it will not target its commander");
        check(!com.kolodkingal.enhanced.ability.AreaTargets.mayHurt(thor, husk, false), "his attacks spare it");
        long left = StoneCooldowns.remaining(thor, StoneKind.MIND, f.level().getGameTime());
        check(left == StonesConfig.MIND_COMMAND_COOLDOWN.get(), "the cooldown: " + left);
        helper.succeed();
    }

    /** A player is never commanded: with PvP on he is only confused. */
    @GameTest(template = ARENA, batch = "stones_mind_pvp")
    public static void theMindCommandOnlyConfusesAPlayer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer thor = holder(f, 3.5, 10.5, Power.THOR, StoneKind.MIND);
        ServerPlayer other = f.player(8.5, FLOOR, 10.5);
        lookAt(thor, other);
        use(thor, StoneKind.MIND, true);
        check(other.hasEffect(MobEffects.CONFUSION) && other.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "the player is muddled");
        check(MindStonePowers.commanderOf(other) == null, "but not commanded");
        helper.succeed();
    }

    // ============================================================================================================
    // The Time Stone

    /** Thor may hold the Time Stone, but it will not answer him: he is no sorcerer. */
    @GameTest(template = ARENA, batch = "stones_time")
    public static void thorHoldsTheTimeStoneButCannotUseIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 5.5, 5.5, Power.THOR, StoneKind.TIME);
        TestKit.sentPackets(thor);
        use(thor, StoneKind.TIME, false);
        check(told(thor, "message.enhanced.stones.use.no_sorcerer"), "he is told he is no sorcerer");
        check(!TimeStonePowers.slowing(thor), "time runs on");
        helper.succeed();
    }

    /** Billy slows time round him: the husk is in the zone, at the Time Stone's factor; again, and it stops. */
    @GameTest(template = ARENA, batch = "stones_time")
    public static void billySlowsTimeRoundAZombie(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = holder(f, 5.5, 10.5, Power.BILLY, StoneKind.TIME);
        Zombie husk = husk(helper, 10.5, 10.5);
        use(billy, StoneKind.TIME, false);
        check(TimeStonePowers.slowing(billy) && Powers.get(billy).is(Live.TIME_SLOWING), "time slows");
        // (SlowMotion keeps the zones of a tick: the new one counts from the next.)
        ticking(helper, billy, 5, () -> {
            check(SlowMotion.isInZone(f.level(), husk.position()), "the husk is in the zone");
            check(StoneZones.factorFor(husk, SlowMotion.factor()) == StonesConfig.TIME_SLOW_FACTOR.get(),
                    "at the Time Stone's factor");
            check(!SlowMotion.skipTick(billy), "Billy himself is not slowed");
            use(billy, StoneKind.TIME, false);
            check(!TimeStonePowers.slowing(billy) && !Powers.get(billy).is(Live.TIME_SLOWING), "again: time runs on");
            long left = StoneCooldowns.remaining(billy, StoneKind.TIME, f.level().getGameTime());
            check(left == StonesConfig.TIME_SLOW_COOLDOWN.get(), "and the cooldown runs: " + left);
            helper.succeed();
        });
    }

    /** Slow Time ends by itself at its limit. */
    @GameTest(template = ARENA, batch = "stones_time_cfg")
    public static void slowTimeEndsAtItsLimit(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(StonesConfig.TIME_SLOW_MAX_TICKS, 20);
        ServerPlayer billy = holder(f, 5.5, 10.5, Power.BILLY, StoneKind.TIME);
        use(billy, StoneKind.TIME, false);
        helper.runAfterDelay(22, () -> {
            tick(billy, 1);
            check(!TimeStonePowers.slowing(billy), "the stone let go after its limit");
            helper.succeed();
        });
    }

    /** The Rewind takes Billy back to where he was, with the health he had then. */
    @GameTest(template = ARENA, batch = "stones_time")
    public static void theRewindReturnsPositionAndHealth(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = holder(f, 4.5, 10.5, Power.BILLY, StoneKind.TIME);
        Vec3 then = billy.position();
        TimeRewind.rememberAgo(billy, 60);
        billy.teleportTo(billy.getX() + 8.0, billy.getY(), billy.getZ());
        billy.setHealth(8.0F);
        use(billy, StoneKind.TIME, true);
        check(billy.position().distanceTo(then) < 1.0, "back where he was: " + billy.position() + " / " + then);
        check(billy.getHealth() > 8.0F, "with more health: " + billy.getHealth());
        long left = StoneCooldowns.remaining(billy, StoneKind.TIME, f.level().getGameTime());
        check(left == StonesConfig.TIME_REWIND_COOLDOWN.get(), "the cooldown: " + left);
        helper.succeed();
    }

    /** Mjolnir's pin refuses the Rewind. */
    @GameTest(template = ARENA, batch = "stones_time_pin")
    public static void theRewindIsRefusedWhenPinned(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer billy = holder(f, 4.5, 10.5, Power.BILLY, StoneKind.TIME);
        TimeRewind.rememberAgo(billy, 60);
        ServerPlayer thor = f.player(1.5, FLOOR, 1.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, billy);
        check(MjolnirPin.isPinned(billy), "Billy is pinned");
        use(billy, StoneKind.TIME, true);
        check(StoneCooldowns.ready(billy, StoneKind.TIME, f.level().getGameTime()), "nothing was rewound");
        helper.succeed();
    }

    /** The Time Stone on wheat grows it one stage; with Sneak, takes it back one. */
    @GameTest(template = ARENA, batch = "stones_time")
    public static void theTimeStoneGrowsAndUngrowsACrop(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = holder(f, 4.5, 10.5, Power.BILLY, StoneKind.TIME);
        BlockPos crop = new BlockPos(6, 2, 10);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));
        BlockPos at = helper.absolutePos(crop);
        check(TimeStonePowers.cropMagic(billy, StoneUse.Source.BARE, at, false), "it works on the crop");
        check(f.level().getBlockState(at).getValue(CropBlock.AGE) == 4, "one stage up");
        StoneCooldowns.start(billy, StoneKind.TIME, 0);
        check(TimeStonePowers.cropMagic(billy, StoneUse.Source.BARE, at, true), "and back");
        check(f.level().getBlockState(at).getValue(CropBlock.AGE) == 3, "one stage down");
        helper.succeed();
    }

    // ============================================================================================================
    // The Power Stone

    /**
     * The Purple Beam burns the husk it is on and bursts where it ends: the husk beside it is hurt, no block breaks.
     */
    @GameTest(template = ARENA, batch = "stones_power")
    public static void thePurpleBeamHurtsAndExplodes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 3.5, 10.5, Power.THOR, StoneKind.POWER);
        Zombie target = husk(helper, 9.5, 10.5);
        Zombie beside = husk(helper, 10.5, 12.0);
        lookAt(thor, target);
        use(thor, StoneKind.POWER, false);
        check(StoneUse.holding(thor, StoneKind.POWER) && Powers.get(thor).is(Live.POWER_VEINS), "the beam pours");
        tick(thor, 11);
        check(lost(target) >= StonesConfig.POWER_BEAM_DAMAGE.get(), "the target burns: " + lost(target));
        check(lost(beside) > 0.0F, "the burst hurts the one beside it");
        Vec3 floor = Vec3.atCenterOf(helper.absolutePos(new BlockPos(10, 1, 10)));
        check(PowerBeam.crater(thor, f.level(), floor) == 0, "no crater by default");
        check(f.level().getBlockState(helper.absolutePos(new BlockPos(10, 1, 10))).isSolid(), "the floor is whole");
        helper.succeed();
    }

    /** The beam overheats at its limit: the long cooldown. */
    @GameTest(template = ARENA, batch = "stones_power")
    public static void thePurpleBeamOverheats(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 3.5, 3.5, Power.THOR, StoneKind.POWER);
        use(thor, StoneKind.POWER, false);
        tick(thor, StonesConfig.POWER_BEAM_MAX_TICKS.get() + 2);
        check(!StoneUse.holding(thor) && !Powers.get(thor).is(Live.POWER_VEINS), "it stopped");
        long left = StoneCooldowns.remaining(thor, StoneKind.POWER, f.level().getGameTime());
        check(left == StonesConfig.POWER_OVERHEAT_COOLDOWN.get(), "the overheat cooldown: " + left);
        helper.succeed();
    }

    /** The Power Bolt: Thanos's blast with the stone's numbers. */
    @GameTest(template = ARENA, batch = "stones_power")
    public static void thePowerBoltStrikes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 3.5, 10.5, Power.THOR, StoneKind.POWER);
        Zombie husk = husk(helper, 10.5, 10.5);
        lookAt(thor, husk);
        use(thor, StoneKind.POWER, true);
        assertNear(lost(husk), StonesConfig.POWER_BOLT_DAMAGE.get(), 0.01, "the bolt's damage");
        long left = StoneCooldowns.remaining(thor, StoneKind.POWER, f.level().getGameTime());
        check(left == StonesConfig.POWER_BOLT_COOLDOWN.get(), "its cooldown: " + left);
        helper.succeed();
    }

    /** With terrainDamage and mobGriefing on, the burst leaves a small crater; with mobGriefing off, none. */
    @GameTest(template = ARENA, batch = "stones_power_cfg")
    public static void craterOnlyWithTheConfigAndMobGriefing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(StonesConfig.POWER_TERRAIN_DAMAGE, true);
        ServerLevel level = f.level();
        GameRules.BooleanValue griefing = level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean was = griefing.get();
        f.onClose(() -> griefing.set(was, level.getServer()));
        ServerPlayer thor = f.player(3.5, FLOOR, 3.5, Power.THOR);
        griefing.set(true, level.getServer());
        BlockPos hit = helper.absolutePos(new BlockPos(14, 1, 14));
        check(PowerBeam.crater(thor, level, Vec3.atCenterOf(hit)) > 0, "a crater");
        check(level.getBlockState(hit).isAir(), "where it burst");
        griefing.set(false, level.getServer());
        BlockPos other = helper.absolutePos(new BlockPos(6, 1, 14));
        check(PowerBeam.crater(thor, level, Vec3.atCenterOf(other)) == 0, "none with mobGriefing off");
        check(!level.getBlockState(other).isAir(), "the floor is whole");
        helper.succeed();
    }

    // ============================================================================================================
    // The Space Stone

    /** The Portal Step takes Thor to the wall he looks at. */
    @GameTest(template = ARENA, batch = "stones_space")
    public static void thePortalStepTakesHimWhereHeLooks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 2.5, 10.5, Power.THOR, StoneKind.SPACE);
        for (int y = 2; y <= 4; y++) {
            helper.setBlock(new BlockPos(14, y, 10), Blocks.STONE);
        }
        TestKit.look(thor, TestKit.EAST, 0.0F);
        Vec3 from = thor.position();
        use(thor, StoneKind.SPACE, false);
        check(thor.position().distanceTo(from) > 8.0, "he stepped far: " + thor.position());
        check(!StoneCooldowns.ready(thor, StoneKind.SPACE, f.level().getGameTime()), "the step's cooldown");
        helper.succeed();
    }

    /** The Tesseract: a plain use is the old one, Sneak + use opens the Realm Picker. */
    @GameTest(template = ARENA, batch = "stones_space")
    public static void theTesseractsSneakUseOpensThePicker(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer mortal = f.player(5.5, FLOOR, 5.5);
        mortal.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        check(!ScepterRituals.useTesseractRealms(mortal, InteractionHand.MAIN_HAND), "a plain use is not the picker");
        mortal.setShiftKeyDown(true);
        check(ScepterRituals.useTesseractRealms(mortal, InteractionHand.MAIN_HAND), "Sneak + use is");
        check(SpaceRealms.pickerOpen(mortal), "the picker is open");
        List<com.kolodkingal.enhanced.network.RealmScreenPayload.RealmOption> options = SpaceRealms.options(mortal);
        check(options.size() == 4 && options.get(SpaceRealms.OVERWORLD).allowed()
                && options.get(SpaceRealms.NETHER).allowed(), "four realms, the Nether open: " + options);
        helper.succeed();
    }

    /** With reachesAsgard off the picker greys Asgard out. */
    @GameTest(template = ARENA, batch = "stones_space_cfg")
    public static void reachesAsgardOffClosesAsgard(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(StonesConfig.SPACE_REACHES_ASGARD, false);
        ServerPlayer thor = holder(f, 5.5, 5.5, Power.THOR, StoneKind.SPACE);
        var asgard = SpaceRealms.options(thor).get(SpaceRealms.ASGARD);
        check(!asgard.allowed() && asgard.reason().equals("message.enhanced.stones.space.no_asgard"),
                "Asgard is closed to the stone: " + asgard);
        helper.succeed();
    }

    /** Mjolnir's pin refuses the Realm Picker. */
    @GameTest(template = ARENA, batch = "stones_space_pin")
    public static void thePickerIsRefusedWhenPinned(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer loki = holder(f, 5.5, 5.5, Power.LOKI, StoneKind.SPACE);
        ServerPlayer thor = f.player(1.5, FLOOR, 1.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, loki);
        use(loki, StoneKind.SPACE, true);
        check(!SpaceRealms.pickerOpen(loki), "no picker while pinned");
        helper.succeed();
    }

    /** The picker takes Thor to the Nether, onto safe ground, and the stone rests. */
    @GameTest(template = ARENA, batch = "stones_space_nether", timeoutTicks = 400)
    public static void theRealmPickerLandsSafelyInTheNether(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 5.5, 5.5, Power.THOR, StoneKind.SPACE);
        use(thor, StoneKind.SPACE, true);
        check(SpaceRealms.pickerOpen(thor), "the picker is open");
        SpaceRealms.choose(thor, SpaceRealms.NETHER);
        check(thor.level().dimension() == Level.NETHER, "Thor is in the Nether");
        check(SpaceRealms.safe(thor.serverLevel(), thor.blockPosition()), "on safe ground: " + thor.blockPosition());
        check(thor.hasEffect(MobEffects.DAMAGE_RESISTANCE), "with the landing's Resistance");
        long left = StoneCooldowns.remaining(thor, StoneKind.SPACE, thor.level().getGameTime());
        check(left > 0, "the stone rests");
        helper.succeed();
    }

    /** The picker takes Thor to the End's main island. */
    @GameTest(template = ARENA, batch = "stones_space_end", timeoutTicks = 400)
    public static void theRealmPickerLandsOnTheEndIsland(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 5.5, 5.5, Power.THOR, StoneKind.SPACE);
        use(thor, StoneKind.SPACE, true);
        SpaceRealms.choose(thor, SpaceRealms.END);
        check(thor.level().dimension() == Level.END, "Thor is in the End");
        check(SpaceRealms.safe(thor.serverLevel(), thor.blockPosition()), "on the island: " + thor.blockPosition());
        helper.succeed();
    }

    // ============================================================================================================
    // The Soul Stone

    /** The drain takes the husk's life and heals Thor with half of it; letting go ends it and starts the cooldown. */
    @GameTest(template = ARENA, batch = "stones_soul")
    public static void theSoulDrainHealsAndEndsOnRelease(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 3.5, 10.5, Power.THOR, StoneKind.SOUL);
        thor.setHealth(10.0F);
        Zombie husk = husk(helper, 8.5, 10.5);
        lookAt(thor, husk);
        use(thor, StoneKind.SOUL, false);
        check(StoneUse.holding(thor, StoneKind.SOUL) && SoulDrain.draining(thor), "the chain is on");
        ticking(helper, thor, SoulDrain.DRAIN_EVERY + 1, () -> {
            double bite = SoulStoneConfig.DRAIN_DAMAGE_PER_SECOND.get() * SoulDrain.DRAIN_EVERY / 20.0;
            assertNear(lost(husk), bite, 0.01, "one bite");
            assertNear(thor.getHealth(), 10.0 + bite * SoulStoneConfig.DRAIN_HEAL_FRACTION.get(), 0.01,
                    "half heals him");
            check(Powers.power(thor) == Power.THOR, "he does not become Thanos");
            StoneUse.use(thor, StoneKind.SOUL, StoneUse.Source.BARE, false, false);
            check(!StoneUse.holding(thor) && !SoulDrain.draining(thor), "letting go ends it");
            long left = StoneCooldowns.remaining(thor, StoneKind.SOUL, f.level().getGameTime());
            check(left == SoulStoneConfig.DRAIN_COOLDOWN.get(), "the cooldown: " + left);
            helper.succeed();
        });
    }

    /** A hard blow tears the chain. */
    @GameTest(template = ARENA, batch = "stones_soul")
    public static void aHardBlowTearsTheSoulDrain(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = holder(f, 3.5, 4.5, Power.THOR, StoneKind.SOUL);
        Zombie husk = husk(helper, 8.5, 4.5);
        lookAt(thor, husk);
        use(thor, StoneKind.SOUL, false);
        SoulStonePowers.hurt(thor, SoulDrain.TEAR_DAMAGE + 1.0F);
        tick(thor, 1);
        check(!SoulDrain.draining(thor) && !StoneUse.holding(thor), "the chain is torn");
        helper.succeed();
    }

    /** The Soul Sight opens for its time, then closes. */
    @GameTest(template = ARENA, batch = "stones_soul_cfg")
    public static void theSoulSightOpensAndCloses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SoulStoneConfig.SIGHT_TICKS, 20);
        ServerPlayer thor = holder(f, 5.5, 5.5, Power.THOR, StoneKind.SOUL);
        use(thor, StoneKind.SOUL, true);
        check(Powers.get(thor).is(Live.SOUL_SIGHT) && SoulStonePowers.seeing(thor), "the sight is open");
        helper.runAfterDelay(22, () -> {
            tick(thor, 1);
            check(!Powers.get(thor).is(Live.SOUL_SIGHT), "and closes");
            helper.succeed();
        });
    }

    // ============================================================================================================
    // The stone rows of the Hero Creator

    private static CustomHero rowHero(String stone) {
        return CustomKitSupport.def().pick("thor.body", 1).pick(stone, 1).key(1, stone).build();
    }

    /** A custom Bearer with stone.mind and the Mind Stone fires the beam on his key; letting go ends it. */
    @GameTest(template = ARENA, batch = "stones_rows")
    public static void aStoneRowFiresWithTheStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = CustomKitSupport.hero(f, 3.5, 10.5, rowHero("stone.mind"));
        hero.getInventory().add(new ItemStack(ModItems.MIND_STONE.get()));
        Zombie husk = husk(helper, 8.5, 10.5);
        lookAt(hero, husk);
        CustomKitSupport.press(hero, 1, false, true);
        check(StoneUse.holding(hero, StoneKind.MIND), "the beam burns");
        tick(hero, 2);
        check(lost(husk) > 0.0F, "and hurts");
        CustomKitSupport.press(hero, 1, false, false);
        check(!StoneUse.holding(hero), "letting go of the key ends it");
        helper.succeed();
    }

    /** Without the stone the row refuses. */
    @GameTest(template = ARENA, batch = "stones_rows")
    public static void aStoneRowWithoutTheStoneIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = CustomKitSupport.hero(f, 3.5, 4.5, rowHero("stone.mind"));
        TestKit.sentPackets(hero);
        CustomKitSupport.press(hero, 1, false, true);
        check(told(hero, "message.enhanced.stones.not_carried"), "he is told he does not carry it");
        check(!StoneUse.holding(hero), "nothing burns");
        helper.succeed();
    }

    /** stone.time needs a sorcerer: a custom Bearer who is not one is refused. */
    @GameTest(template = ARENA, batch = "stones_rows")
    public static void theTimeRowNeedsASorcerer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = CustomKitSupport.hero(f, 3.5, 16.5, rowHero("stone.time"));
        hero.getInventory().add(new ItemStack(ModItems.TIME_STONE.get()));
        TestKit.sentPackets(hero);
        CustomKitSupport.press(hero, 1, false, true);
        check(told(hero, "message.enhanced.stones.use.no_sorcerer"), "no sorcerer");
        check(!TimeStonePowers.slowing(hero), "time runs on");
        helper.succeed();
    }

    /** A hero's beam and a stone's beam drive the same resource: the rules keep one of them and say so. */
    @GameTest(template = ARENA, batch = "stones_rows")
    public static void twoBeamRowsKeepOneOwner(GameTestHelper helper) {
        CustomHero raw = CustomKitSupport.def().pick("billy.hex_beam", 1).pick("stone.mind", 1).raw();
        CustomHeroRules.Result result = CustomHeroRules.sanitize(raw, CustomHeroRules.Limits.DEFAULT);
        check(result.notes().stream().anyMatch(n -> n.key().endsWith("rule.resource")), "noted: " + result.notes());
        check(result.hero().powers().size() == 1, "one kept: " + result.hero().powers());
        helper.succeed();
    }

    private StonePowerTests() {
    }
}

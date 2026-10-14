package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.Telekinesis.Mode;
import com.kolodkingal.enhanced.apocalypse.ApocalypseArena;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.Horsemen;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosFight;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.IntPredicate;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * A boss in the grip of telekinesis (3.1, D-T 4), and what every telekinesis press says when it may not take what it is
 * aimed at (spec 5.2): one ray, nearest first, so a refused thing answers the press and nothing behind it is taken.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BossGripTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Presses the key of an ability, with Sneak held or not. */
    private static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /** A zombie that stands still and does not burn in the sun (the tests run by day). */
    private static Zombie stillZombie(GameTestHelper helper, double x, double z) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        return zombie;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The press says why not

    /**
     * Spec 5.2, the refusals of the press: a zombie someone else holds answers the press ({@code held_by_other}) and the
     * one behind it is not taken; a Spectator's press does nothing; Magneto's R names Mjolnir (dropped, or the only metal
     * in a hand) and a phased Vision, which the other forms do not; the Ender Dragon and its parts are too vast for every
     * form; a Wither gathering his strength or a Warden emerging cannot be held now ({@code boss_untouchable}), and the
     * press at the Wither takes nothing behind him. Magneto's R tears out a metal block and only a metal one.
     */
    @GameTest(template = ARENA, batch = "boss_grip_refusals", timeoutTicks = 40)
    public static void thePressSaysWhyItTakesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = helper.getLevel();

        // Held by someone else: the nearest thing answers, nothing behind it is taken.
        ServerPlayer wanda = f.player(1.5, FLOOR, 6.5, Power.SCARLET_WITCH);
        ServerPlayer billy = f.player(6.5, FLOOR, 9.5, Power.BILLY);
        Zombie front = stillZombie(helper, 6.5, 6.5);
        Zombie behind = stillZombie(helper, 10.5, 6.5);
        lookAt(billy, front);
        press(billy, 3, false);
        check(Telekinesis.holds(billy, front), "Billy holds the front zombie");
        lookAt(wanda, behind);
        lastActionBarKey(wanda);
        press(wanda, 3, false);
        check(Telekinesis.HELD_BY_OTHER.equals(lastActionBarKey(wanda)), "Wanda's press at it: someone else holds it");
        check(!Telekinesis.isHolding(wanda) && !Telekinesis.isHeld(behind) && Telekinesis.holds(billy, front),
                "she takes neither it nor the zombie behind it");
        Telekinesis.release(billy, false);

        // A Spectator's press, if one reaches the server, does nothing at all.
        ServerPlayer spectator = f.player(1.5, FLOOR, 4.5, GameType.SPECTATOR);
        Powers.setPower(spectator, Power.SCARLET_WITCH);
        lookAt(spectator, behind);
        lastActionBarKey(spectator);
        Telekinesis.press(spectator, Mode.WANDA);
        check(!Telekinesis.isHolding(spectator) && !Telekinesis.isHeld(behind) && lastActionBarKey(spectator) == null,
                "a Spectator's press takes nothing and says nothing");

        // Magneto's own refusals: Mjolnir answers no magnetism, a phased Vision has nothing to grip.
        ServerPlayer magneto = f.player(1.5, FLOOR, 13.5, Power.MAGNETO);
        Zombie thor = stillZombie(helper, 6.5, 13.5);
        thor.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MJOLNIR.get()));
        lookAt(magneto, thor);
        lastActionBarKey(magneto);
        press(magneto, 1, false);
        check(Telekinesis.MJOLNIR.equals(lastActionBarKey(magneto)) && !Telekinesis.isHolding(magneto),
                "Magneto's R at a zombie whose only metal is Mjolnir: Mjolnir answers no magnetism");
        check(Telekinesis.refusal(wanda, thor, Mode.WANDA) == null && Telekinesis.canLift(wanda, thor, Mode.WANDA),
                "Wanda's telekinesis takes that zombie");
        thor.discard();
        ItemEntity dropped = new ItemEntity(level, 0, 0, 0, new ItemStack(ModItems.MJOLNIR.get()));
        check(Telekinesis.MJOLNIR.equals(Telekinesis.refusal(magneto, dropped, Mode.MAGNETO)), "a dropped Mjolnir: the same");
        check(Telekinesis.refusal(wanda, dropped, Mode.WANDA) == null, "Wanda is told nothing of Mjolnir (she cannot lift it either)");

        ServerPlayer vision = f.player(6.5, FLOOR, 16.5, Power.VISION);
        Powers.get(vision).setVisionPhased(true);
        lookAt(magneto, vision);
        press(magneto, 1, false);
        check(Telekinesis.PHASED.equals(lastActionBarKey(magneto)) && !Telekinesis.isHolding(magneto),
                "Magneto's R at a phased Vision: there is nothing to grip");
        check(Telekinesis.refusal(wanda, vision, Mode.WANDA) == null, "only Magneto's metal is told so");

        // The Ender Dragon and each of its parts: too vast to hold, for every form.
        EnderDragon dragon = EntityType.ENDER_DRAGON.create(level);
        check(dragon != null, "a dragon is made");
        for (Mode mode : Mode.values()) {
            check(Telekinesis.DRAGON.equals(Telekinesis.refusal(wanda, dragon, mode)) && !Telekinesis.canLift(wanda, dragon, mode),
                    mode + ": the Ender Dragon is too vast to hold");
            for (EnderDragonPart part : dragon.getSubEntities()) {
                check(Telekinesis.DRAGON.equals(Telekinesis.refusal(wanda, part, mode)) && !Telekinesis.canLift(wanda, part, mode),
                        mode + ": and so is his " + part.name);
            }
        }

        // The vanilla bosses in their untouchable states.
        ServerPlayer phoenix = f.player(8.5, FLOOR, 19.5, Power.PHOENIX);
        WitherBoss wither = helper.spawnWithNoFreeWill(EntityType.WITHER, new Vec3(14.5, FLOOR, 19.5));
        wither.setInvulnerableTicks(100);
        Zombie past = stillZombie(helper, 18.5, 19.5);
        lookAt(phoenix, wither);
        lastActionBarKey(phoenix);
        press(phoenix, 3, false);
        check(BossGrip.UNTOUCHABLE.equals(lastActionBarKey(phoenix)), "a Wither gathering his strength cannot be held now");
        check(!Telekinesis.isHolding(phoenix) && !Telekinesis.isHeld(past), "and nothing behind him is taken");
        wither.discard();
        Warden warden = EntityType.WARDEN.create(level);
        check(warden != null, "a Warden is made");
        warden.setPose(Pose.EMERGING);
        check(BossGrip.UNTOUCHABLE.equals(Telekinesis.refusal(billy, warden, Mode.BILLY)), "nor a Warden coming out of the ground");
        warden.setPose(Pose.DIGGING);
        check(BossGrip.UNTOUCHABLE.equals(Telekinesis.refusal(billy, warden, Mode.BILLY)), "nor one going into it");
        warden.setPose(Pose.STANDING);
        check(Telekinesis.refusal(billy, warden, Mode.BILLY) == null, "a Warden standing has nothing that refuses a grip");

        // Magneto's R at a block: metal only.
        ServerPlayer metal = f.player(1.5, FLOOR, 1.5, Power.MAGNETO);
        BlockPos stone = new BlockPos(5, 2, 3);
        BlockPos iron = new BlockPos(5, 2, 1);
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(iron, Blocks.IRON_BLOCK);
        lookAt(metal, Vec3.atCenterOf(helper.absolutePos(stone)));
        lastActionBarKey(metal);
        press(metal, 1, false);
        check("message.enhanced.telekinesis.no_metal".equals(lastActionBarKey(metal)) && helper.getBlockState(stone).is(Blocks.STONE),
                "R at stone: no metal in sight, the stone stays");
        lookAt(metal, Vec3.atCenterOf(helper.absolutePos(iron)));
        press(metal, 1, false);
        TelekineticBlockEntity held = null;
        for (TelekineticBlockEntity block : helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get())) {
            if (Powers.get(metal).heldEntityId() == block.getId()) {
                held = block;
            }
        }
        check(held != null && held.getBlockState().is(Blocks.IRON_BLOCK) && helper.getBlockState(iron).isAir(),
                "R at an iron block tears it out of the ground");
        check(held.getColor() == Power.MAGNETO.color, "in his iron grey");
        Telekinesis.release(metal, false);
        held.discard();
        helper.succeed();
    }

    /**
     * Spec 5.2 step 2 and refusal step 5 (D-T 4.2, 4.6; R2): what a fight holds where it is (the Sokovia lift carries it)
     * uses no telekinesis and cannot be taken. A carried Wanda's V and Sneak + V, and a carried Magneto's Sneak + R, say
     * {@code telekinesis.carried} and do nothing else; Billy's V at a carried zombie says {@code boss_untouchable} and takes
     * nothing; let go by the fight, the zombie is held.
     */
    @GameTest(template = ARENA, batch = "boss_grip_locked", timeoutTicks = 40)
    public static void whatAFightHoldsIsNeitherTakenNorTakes(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer magneto = f.player(4.5, FLOOR, 14.5, Power.MAGNETO);
        ServerPlayer billy = f.player(16.5, FLOOR, 6.5, Power.BILLY);
        TestFight fight = BossFrameworkTests.start(f, BossKind.ULTRON, wanda,
                BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, false));
        Zombie carried = stillZombie(helper, 12.5, 6.5);
        fight.locked = e -> e == wanda || e == magneto || e == carried;
        BlockPos dirt = new BlockPos(8, 2, 10);
        helper.setBlock(dirt, Blocks.DIRT);
        Zombie zombie = stillZombie(helper, 8.5, 12.5);

        lookAt(wanda, zombie);
        lastActionBarKey(wanda);
        press(wanda, 3, false);
        check("message.enhanced.telekinesis.carried".equals(lastActionBarKey(wanda)) && !Telekinesis.isHolding(wanda),
                "a carried Wanda's V: the island carries her, no telekinesis");
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(dirt)));
        press(wanda, 3, true);
        check("message.enhanced.telekinesis.carried".equals(lastActionBarKey(wanda)) && helper.getBlockState(dirt).is(Blocks.DIRT),
                "nor Sneak + V: the dirt stays");
        lookAt(magneto, zombie);
        lastActionBarKey(magneto);
        press(magneto, 1, true);
        check("message.enhanced.telekinesis.carried".equals(lastActionBarKey(magneto)), "a carried Magneto's Sneak + R: the same");

        lookAt(billy, carried);
        lastActionBarKey(billy);
        press(billy, 3, false);
        check(BossGrip.UNTOUCHABLE.equals(lastActionBarKey(billy)) && !Telekinesis.isHolding(billy),
                "Billy's V at a carried zombie: it cannot be held now");
        check(!Telekinesis.canLift(billy, carried, Mode.BILLY), "it cannot be lifted");
        fight.locked = e -> false;
        press(billy, 3, false);
        check(Telekinesis.holds(billy, carried), "let go by the fight, Billy holds it");
        Telekinesis.release(billy, false);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Hold limit, struggle and regrip (D-T 4.3)

    /**
     * T7 (D-T 4.3, 4.4): Wanda holds Ultron's body on the ground. For 59 ticks it is held and does nothing (his brain
     * chooses, his cooldowns are reset, and still no attack begins), 20 ticks before the end she reads that he is breaking
     * free, and at the 60th tick he breaks free. Then nobody holds him for {@code boss.regripTicks} (40 here): Wanda's and
     * Billy's presses say so, the last one 39 ticks after the break; 40 ticks after it Billy holds him, and loses him after
     * his own hold of 30 ticks.
     */
    @GameTest(template = ARENA, batch = "boss_grip_free", timeoutTicks = 200)
    public static void aHeldBossBreaksFreeAndNobodyTakesHimAgainAtOnce(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(BossConfig.REGRIP_TICKS, 40);
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        ServerPlayer billy = f.player(10.5, FLOOR, 17.5, Power.BILLY);
        UltronTestSupport.tank(billy);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, wanda, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 9.5, FLOOR + 1.0, 10.5);
        lookAt(wanda, body);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, body) && BossGrip.GRABBED.equals(lastActionBarKey(wanda)), "Wanda takes hold of Ultron as a boss");
        body.brain().setChoosing(true);
        body.brain().resetCooldowns();
        int[] brokeAt = {-1};
        f.everyTick(180, tick -> {
            PowerTicker.tick(wanda);
            PowerTicker.tick(billy);
            if (brokeAt[0] < 0) {
                if (tick == 40) {
                    check(BossGrip.BREAKING.equals(lastActionBarKey(wanda)), "20 ticks before the end she reads that he is breaking free");
                }
                if (tick < 60) {
                    check(Telekinesis.holds(wanda, body), "he is held at tick " + tick);
                    check(body.brain().isIdle() && !body.state().isAttack(), "and does nothing at tick " + tick);
                    return false;
                }
                check(!Telekinesis.isHolding(wanda) && BossGrip.BROKE_FREE.equals(lastActionBarKey(wanda)), "at the 60th tick he broke free");
                brokeAt[0] = tick;
                UltronTestSupport.calm(fight);
                lookAt(wanda, body);
                press(wanda, 3, false);
                check(!Telekinesis.isHeld(body) && BossGrip.REGRIP.equals(lastActionBarKey(wanda)), "Wanda's press at once: he is shaking it off");
                lookAt(billy, body);
                press(billy, 3, false);
                check(!Telekinesis.isHeld(body) && BossGrip.REGRIP.equals(lastActionBarKey(billy)), "and Billy's: the regrip is the boss's, for everyone");
                return false;
            }
            int since = tick - brokeAt[0];
            if (since == 39) {
                lookAt(billy, body);
                press(billy, 3, false);
                check(!Telekinesis.isHeld(body) && BossGrip.REGRIP.equals(lastActionBarKey(billy)), "39 ticks after the break: still not");
            } else if (since == 40) {
                lookAt(billy, body);
                press(billy, 3, false);
                check(Telekinesis.holds(billy, body), "40 ticks after the break Billy holds him");
            } else if (since > 40 && since < 70) {
                check(Telekinesis.holds(billy, body), "Billy holds him " + (since - 40) + " ticks");
            } else if (since == 70) {
                check(!Telekinesis.isHolding(billy) && BossGrip.BROKE_FREE.equals(lastActionBarKey(billy)),
                        "and loses him after his own hold: 60 x 0.5 = 30 ticks");
                return true;
            }
            return false;
        });
    }

    /**
     * T11 (D-T 4.3): a boss who becomes untouchable while held slips out of the grip at once, and his holder is not told he
     * broke free. Wanda holds Ultron on the ground; the lift's warning begins (he is shielded): the next tick she holds
     * nothing.
     */
    @GameTest(template = ARENA, batch = "boss_grip_slip", timeoutTicks = 40)
    public static void aHeldBossSlipsWhenHeBecomesUntouchable(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, wanda, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 9.5, FLOOR + 1.0, 10.5);
        lookAt(wanda, body);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, body), "Wanda holds Ultron");
        f.everyTick(20, tick -> {
            PowerTicker.tick(wanda);
            if (tick < 5) {
                check(Telekinesis.holds(wanda, body), "held at tick " + tick);
                return false;
            }
            if (tick == 5) {
                lastActionBarKey(wanda);
                fight.forceState(UltronFight.Phase.LIFT_WARN);
                check(body.isShielded(), "the warning shields him");
                return false;
            }
            check(!Telekinesis.isHolding(wanda) && !Telekinesis.isHeld(body), "the tick after the warning began she holds nothing");
            check(!BossGrip.BROKE_FREE.equals(lastActionBarKey(wanda)), "he slipped away; he did not break free");
            return true;
        });
    }

    /**
     * T11, Apocalypse's part (D-T 4.3, 6.3.1): Wanda's beam breaks his shield, with a short window; when 10 ticks or less of it are
     * left, Billy takes hold of him (his hold, 30 ticks, outlasts the window). The window ends by time and the shield
     * re-forms: Billy holds nothing at most one tick after the shield first reads {@code REFORMING}, and was not told he
     * broke free.
     */
    @GameTest(template = ARENA, batch = "boss_grip_slip_window", timeoutTicks = 80)
    public static void aHeldApocalypseSlipsWhenHisShieldReforms(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.BILLY);
        d.f().onClose(Telekinesis::clear);
        d.f().config(ApocalypseBossConfig.WINDOW_TICKS, 40);
        ServerPlayer billy = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerPlayer wanda = d.f().player(10.5, FLOOR, 17.5, Power.SCARLET_WITCH);
        ApocalypseTestSupport.tank(wanda);
        boss.invulnerableTime = 0;
        boss.hurt(ModDamageTypes.source(d.level(), ModDamageTypes.CHAOS_BEAM, wanda, wanda), boss.shield().strength() + 1.0F);
        check(boss.shield().state() == ApocalypseShield.State.BROKEN && boss.shield().windowLeft() <= 40,
                "Wanda's beam breaks his shield, opening a short window: " + boss.shield().state());
        int[] reforming = {-1};
        boolean[] grabbed = {false};
        d.f().everyTick(60, tick -> {
            PowerTicker.tick(billy);
            if (!grabbed[0]) {
                if (boss.shield().state() == ApocalypseShield.State.BROKEN && boss.shield().windowLeft() <= 10) {
                    lookAt(billy, boss);
                    press(billy, 3, false);
                    check(Telekinesis.holds(billy, boss), "in the last 10 ticks of the window Billy takes hold of him");
                    lastActionBarKey(billy);
                    grabbed[0] = true;
                }
                return false;
            }
            if (reforming[0] < 0 && boss.shield().state() == ApocalypseShield.State.REFORMING) {
                reforming[0] = tick;
            }
            if (reforming[0] < 0) {
                check(Telekinesis.holds(billy, boss), "Billy holds him while the window lasts (tick " + tick + ")");
                return false;
            }
            if (Telekinesis.isHolding(billy)) {
                check(tick - reforming[0] < 1, "Billy still holds him " + (tick - reforming[0]) + " ticks after the shield began to re-form");
                return false;
            }
            check(!BossGrip.BROKE_FREE.equals(lastActionBarKey(billy)), "he slipped away as his shield re-formed; he did not break free");
            return true;
        });
    }

    /**
     * T17 (D-T 4.3, 4.6, 7): one holder per boss, and the regrip time is the boss's, for everyone. Wanda holds Thanos; the
     * Phoenix's press says someone else holds him; he breaks free after Wanda's 40 ticks, and the Phoenix's press at once
     * says he is still shaking it off: two holders cannot chain him.
     */
    @GameTest(template = ARENA, batch = "boss_grip_two", timeoutTicks = 80)
    public static void twoHoldersCannotChainABoss(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer wanda = ThanosMoveSupport.fighter(f, -7.0, 0.0, Power.SCARLET_WITCH);
        ServerPlayer phoenix = ThanosMoveSupport.fighter(f, 0.0, 7.0, Power.PHOENIX);
        ThanosEntity thanos = ThanosBossTests.boss(f, wanda);
        lookAt(wanda, thanos);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, thanos), "Wanda holds Thanos");
        lookAt(phoenix, thanos);
        press(phoenix, 3, false);
        check(Telekinesis.HELD_BY_OTHER.equals(lastActionBarKey(phoenix)) && Telekinesis.holds(wanda, thanos),
                "the Phoenix's press: someone else holds him");
        f.everyTick(60, tick -> {
            PowerTicker.tick(wanda);
            PowerTicker.tick(phoenix);
            if (Telekinesis.holds(wanda, thanos)) {
                check(tick < 40, "Wanda holds him at most 40 ticks");
                return false;
            }
            check(tick == 40 && BossGrip.BROKE_FREE.equals(lastActionBarKey(wanda)), "he broke free of Wanda at tick 40: " + tick);
            lookAt(phoenix, thanos);
            press(phoenix, 3, false);
            check(!Telekinesis.isHeld(thanos) && BossGrip.REGRIP.equals(lastActionBarKey(phoenix)),
                    "the Phoenix's press at once: he is still shaking off the last grip");
            return true;
        });
    }

    /**
     * T41 (D-T 4.3, the default config): a boss is still at most a third of a fight, however hard the heroes try. Magneto
     * and Wanda alternate on Ultron, Magneto first: whenever nobody holds him and the next one's press would not be
     * refused, that one takes hold of him, and throws him at the edge of the arena on the last tick before he would break
     * free. Over 1,200 ticks his brain runs at least 800 of them (the arithmetic of D-T 4.3 gives 820), he is grabbed at
     * least 4 times, and never less than {@code boss.regripTicks} (240) after the still of the last throw ended.
     */
    @GameTest(template = ARENA, batch = "boss_grip_duty", timeoutTicks = 1300)
    public static void aBossIsStillAtMostAThirdOfTheTime(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer magneto = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.MAGNETO);
        UltronTestSupport.tank(magneto);
        ServerPlayer wanda = f.player(10.5, FLOOR, 17.5, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        UltronFight fight = UltronTestSupport.onTheGround(f, magneto);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, magneto, 10.5, FLOOR, 3.5);
        ServerPlayer[] holders = {magneto, wanda};
        Mode[] modes = {Mode.MAGNETO, Mode.WANDA};
        int[] keys = {1, 3};
        long ticksBefore = body.brain().ticked();
        int[] next = {0};
        int[] current = {-1};
        int[] grabbedAt = {0};
        int[] grabs = {0};
        long[] stillEnded = {Long.MIN_VALUE};
        boolean[] wasStill = {false};
        Vec3 centre = fight.arena().center();
        f.everyTick(1200, tick -> {
            PowerTicker.tick(magneto);
            PowerTicker.tick(wanda);
            long now = helper.getLevel().getGameTime();
            boolean still = BossGrip.stills(body);
            if (wasStill[0] && !still) {
                stillEnded[0] = now;
            }
            wasStill[0] = still;
            if (current[0] >= 0 && Telekinesis.holds(holders[current[0]], body)) {
                ServerPlayer holder = holders[current[0]];
                if (tick - grabbedAt[0] == BossGrip.holdLimit(body, modes[current[0]]) - 1) {
                    // The last tick before he would break free: thrown at the edge of the arena, away from the middle.
                    Vec3 away = body.position().subtract(centre).multiply(1.0, 0.0, 1.0);
                    away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
                    lookAt(holder, body.position().add(away.scale(20.0)).add(0.0, 1.0, 0.0));
                    press(holder, keys[current[0]], false);
                    check(!Telekinesis.isHolding(holder), "the holder throws him at tick " + tick);
                    current[0] = -1;
                }
            } else if (!Telekinesis.isHeld(body)) {
                ServerPlayer holder = holders[next[0]];
                if (Telekinesis.refusal(holder, body, modes[next[0]]) == null && Telekinesis.canLift(holder, body, modes[next[0]])) {
                    lookAt(holder, body);
                    press(holder, keys[next[0]], false);
                    check(Telekinesis.holds(holder, body), "a press that is not refused takes hold of him (tick " + tick + ")");
                    long since = stillEnded[0] == Long.MIN_VALUE || still ? -1 : now - stillEnded[0];
                    check(grabs[0] == 0 || since >= BossConfig.REGRIP_TICKS.get(),
                            "no grab comes while he is still, nor within the regrip time of the last still's end: " + since);
                    grabs[0]++;
                    grabbedAt[0] = tick;
                    current[0] = next[0];
                    next[0] = 1 - next[0];
                }
            }
            if (tick < 1200) {
                return false;
            }
            long brainTicks = body.brain().ticked() - ticksBefore;
            check(brainTicks >= 800, "his brain ran at least two thirds of the time: " + brainTicks + " of 1200");
            check(grabs[0] >= 4, "and he was grabbed at least 4 times: " + grabs[0]);
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Entombed

    /**
     * T18 (D-T 6.3.1): an entombed hero lets go of what they hold, whatever entombed them. Wanda holds a zombie in the air
     * and Apocalypse's sand swallows her ({@code SandControl.entomb}): she holds nothing at once, the zombie falls, and a key
     * press does nothing. The Phoenix holds a zombie and is given the effect directly (as {@code /effect} would): the next
     * tick she holds nothing.
     */
    @GameTest(template = ARENA, batch = "boss_grip_entomb", timeoutTicks = 80)
    public static void anEntombedHeroLetsGo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(3.5, FLOOR, 6.5, Power.SCARLET_WITCH);
        ServerPlayer phoenix = f.player(3.5, FLOOR, 14.5, Power.PHOENIX);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 6.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        Zombie other = stillZombie(helper, 8.5, 14.5);
        Zombie spare = stillZombie(helper, 8.5, 10.5);
        lookAt(wanda, zombie);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, zombie), "Wanda holds a zombie");
        lookAt(phoenix, other);
        press(phoenix, 3, false);
        check(Telekinesis.holds(phoenix, other), "the Phoenix holds another");
        double[] liftedTo = {0.0};
        f.everyTick(60, tick -> {
            if (tick <= 10) {
                look(wanda, wanda.getYRot(), -25.0F); // she raises it into the air
                PowerTicker.tick(wanda);
                PowerTicker.tick(phoenix);
                return false;
            }
            if (tick == 11) {
                liftedTo[0] = zombie.getY();
                check(liftedTo[0] > helper.absoluteVec(new Vec3(0, FLOOR + 1.0, 0)).y, "the zombie is up in the air: " + liftedTo[0]);
                check(SandControl.entomb(level, null, wanda, 100, 0.0F, 10.0F, null) != null, "the sand swallows Wanda");
                check(!Telekinesis.isHolding(wanda) && !Telekinesis.isHeld(zombie), "she holds nothing at once");
                lookAt(wanda, spare);
                press(wanda, 3, false);
                check(!Telekinesis.isHolding(wanda) && !Telekinesis.isHeld(spare), "and a key press does nothing");
                phoenix.addEffect(new MobEffectInstance(ModEffects.ENTOMBED, 100, 0));
                check(Telekinesis.holds(phoenix, other), "the Phoenix, entombed by an effect alone, still holds hers until her tick");
                PowerTicker.tick(phoenix);
                check(!Telekinesis.isHolding(phoenix) && !Telekinesis.isHeld(other), "the next tick she holds nothing");
                return false;
            }
            return zombie.onGround() && zombie.getY() < liftedTo[0] - 1.0;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Carrying and throwing (D-T 4.5)

    /**
     * Walks {@code hero}, who holds {@code target}, outward from the centre of the arena along {@code (dx, dz)}, half a block
     * a tick, looking straight away from the centre, until he stands {@code end} blocks from it, then 20 ticks more. Every
     * tick the target's horizontal distance from the centre is never more than {@code limit}, and at some tick it is at
     * least {@code limit - 0.5}: the hero pulled it against the limit, and it stayed in. Then {@code then} runs.
     */
    private static void walkOut(TestKit.Fixture f, ServerPlayer hero, Entity target, Vec3 centre, double dx, double dz, double limit,
                                double end, String what, Runnable then) {
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        double[] at = {(hero.getX() - centre.x) * dx + (hero.getZ() - centre.z) * dz};
        double[] farthest = {0.0};
        int[] after = {0};
        chain(f.helper(), 200, tick -> {
            if (at[0] < end) {
                at[0] = Math.min(end, at[0] + 0.5);
            } else {
                after[0]++;
            }
            hero.moveTo(centre.x + dx * at[0], hero.getY(), centre.z + dz * at[0], yaw, 0.0F);
            look(hero, yaw, 0.0F);
            PowerTicker.tick(hero);
            check(Telekinesis.holds(hero, target), what + " is held all along (tick " + tick + ")");
            double distance = Math.hypot(target.getX() - centre.x, target.getZ() - centre.z);
            check(distance <= limit + 1.0E-3, what + " is never carried past " + limit + ": " + distance + " at tick " + tick);
            farthest[0] = Math.max(farthest[0], distance);
            if (after[0] < 20) {
                return false;
            }
            check(farthest[0] >= limit - 0.5, what + " was pulled against its limit " + limit + ": at most " + farthest[0]);
            Telekinesis.release(hero, false);
            then.run();
            return true;
        });
    }

    /**
     * Runs {@code step} once a tick, one schedule entry at a time (see {@link ApocalypseTestSupport#every}), until it answers
     * true; the test fails if it has not by {@code lastTick}. Unlike {@code every}, being done does not end the test: what
     * the step started goes on.
     */
    private static void chain(GameTestHelper helper, int lastTick, IntPredicate step) {
        chainFrom(helper, 1, lastTick, step);
    }

    private static void chainFrom(GameTestHelper helper, int tick, int lastTick, IntPredicate step) {
        helper.runAfterDelay(1, () -> {
            if (step.test(tick)) {
                return;
            }
            if (tick >= lastTick) {
                throw new GameTestAssertException("Not done within " + lastTick + " ticks");
            }
            chainFrom(helper, tick + 1, lastTick, step);
        });
    }

    /** Clears the air of the pad over its middle row, east to west, so that nothing carried there meets a tree or a hill. */
    private static void clearTheRow(GameTestHelper helper) {
        int middle = (int) SokoviaTestSupport.MIDDLE_Z;
        for (int x = 0; x < SokoviaTestSupport.WIDTH; x++) {
            for (int z = middle - 3; z <= middle + 3; z++) {
                for (int y = SokoviaTestSupport.SURFACE + 1; y <= SokoviaTestSupport.SURFACE + 12; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    /**
     * T12 (D-T 4.5, 6.1.2), Ultron's part: the hero takes hold of a body of Ultron 6 blocks from it, inside the field, and
     * walks outward until he stands 4 blocks past the field: the body is never carried more than {@code containRadius - 2}
     * from the centre (30 by default; 19 with the tests' island of 9), and it is carried right up to that limit; the same for
     * a Sentry of the fight. The holds are 600 ticks here, so nothing breaks free on the way.
     */
    @GameTest(template = SokoviaTestSupport.PAD, batch = "boss_grip_carry", timeoutTicks = 300)
    public static void aBossIsNeverCarriedOutOfHisArena(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(UltronBossConfig.HOLD_TICKS, 600);
        clearTheRow(helper);
        double ground = SokoviaTestSupport.GROUND;
        double mx = SokoviaTestSupport.MIDDLE_X;
        double mz = SokoviaTestSupport.MIDDLE_Z;
        ServerPlayer wanda = f.player(mx, ground, mz, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        Vec3 centre = fight.arena().center();
        double limit = fight.arena().containRadius() - 2.0;
        UltronTestSupport.put(f, wanda, mx + 4.0, ground, mz);
        UltronTestSupport.put(f, body, mx + 10.0, ground + 1.0, mz);
        lookAt(wanda, body);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, body), "Wanda holds Ultron");
        walkOut(f, wanda, body, centre, 1.0, 0.0, limit, fight.arena().containRadius() + 4.0, "Ultron's body", () -> {
            UltronSentryEntity sentry = UltronSentryEntity.army(f.level(), f.helper().absoluteVec(new Vec3(mx - 10.0, ground + 1.0, mz)), false);
            fight.spawn(sentry);
            fight.refresh();
            sentry.setHeld(true);
            UltronTestSupport.put(f, wanda, mx - 4.0, ground, mz);
            lookAt(wanda, sentry);
            press(wanda, 3, false);
            check(Telekinesis.holds(wanda, sentry), "Wanda holds a Sentry");
            walkOut(f, wanda, sentry, centre, -1.0, 0.0, limit, fight.arena().containRadius() + 4.0, "a Sentry", f.helper()::succeed);
        });
    }

    /**
     * T12, Apocalypse's part (D-T 6.3.1): he is never carried more than {@code fightRadius - 2} from the centre (22 by
     * default; 7 with the tests' radius of 9), inside the leash of his own, and a Horseman never more than {@code
     * containRadius - 2} (26 by default; 11 here), inside the wall.
     */
    @GameTest(template = SokoviaTestSupport.PAD, batch = "boss_grip_carry_apocalypse", timeoutTicks = 300)
    public static void apocalypseAndHisHorsemenAreNeverCarriedOutOfHisArena(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(ApocalypseBossConfig.HOLD_TICKS, 600);
        clearTheRow(helper);
        double ground = SokoviaTestSupport.GROUND;
        double mx = SokoviaTestSupport.MIDDLE_X;
        double mz = SokoviaTestSupport.MIDDLE_Z;
        ServerPlayer wanda = f.player(mx, ground, mz, Power.SCARLET_WITCH);
        ApocalypseTestSupport.tank(wanda);
        ApocalypseFight fight = ApocalypseTestSupport.inPhase(f, wanda, 1);
        ApocalypseEntity boss = ApocalypseTestSupport.boss(fight);
        ApocalypseArena arena = (ApocalypseArena) fight.arena();
        Vec3 centre = arena.center();
        ApocalypseTestSupport.put(f, boss, mx + 3.0, ground, mz);
        ApocalypseTestSupport.put(f, wanda, mx - 3.0, ground, mz);
        lookAt(wanda, boss);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, boss), "Wanda (mind power) holds Apocalypse through his shield");
        walkOut(f, wanda, boss, centre, 1.0, 0.0, arena.fightRadius() - 2.0, arena.containRadius() + 4.0, "Apocalypse", () -> {
            Husk horseman = fight.horsemen().spawn(f.level(), Horsemen.Kind.FAMINE, f.helper().absoluteVec(new Vec3(mx - 9.0, ground, mz)));
            horseman.setNoAi(true);
            UltronTestSupport.put(f, wanda, mx - 3.0, ground, mz);
            lookAt(wanda, horseman);
            press(wanda, 3, false);
            check(Telekinesis.holds(wanda, horseman), "Wanda holds a Horseman");
            walkOut(f, wanda, horseman, centre, -1.0, 0.0, arena.containRadius() - 2.0, arena.containRadius() + 4.0, "a Horseman",
                    f.helper()::succeed);
        });
    }

    /**
     * T12, Thanos's part (D-T 6.2.1): his arena is open (no field, no wall), and he is never carried more than {@code
     * radius - 2} from its centre (38 by default; 14 with the tests' radius of 16).
     */
    @GameTest(template = SokoviaTestSupport.PAD, batch = "boss_grip_carry_thanos", timeoutTicks = 200)
    public static void thanosIsNeverCarriedOutOfHisArena(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(ThanosBossConfig.HOLD_TICKS, 600);
        clearTheRow(helper);
        double ground = SokoviaTestSupport.GROUND;
        double mx = SokoviaTestSupport.MIDDLE_X;
        double mz = SokoviaTestSupport.MIDDLE_Z;
        ServerPlayer wanda = f.player(mx - 3.0, ground, mz, Power.SCARLET_WITCH);
        ThanosMoveSupport.tough(wanda);
        ThanosFight fight = ThanosFight.spawnAt(f.level(), f.helper().absoluteVec(new Vec3(mx, ground, mz)), wanda);
        ThanosEntity thanos = fight.boss();
        check(thanos != null, "the fight made him");
        thanos.forceArrived();
        thanos.setPassive(true);
        UltronTestSupport.put(f, thanos, mx + 3.0, ground, mz);
        lookAt(wanda, thanos);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, thanos), "Wanda holds Thanos");
        walkOut(f, wanda, thanos, fight.arena().center(), 1.0, 0.0, fight.arena().radius() - 2.0, fight.arena().radius() + 4.0, "Thanos",
                f.helper()::succeed);
    }

    /**
     * T45 (D-T 4.5): a Horseman thrown straight at Apocalypse's wall is stopped dead inside it: it is never beyond {@code
     * containRadius}, crashes there, and is brought back two blocks inside the wall, on the sand, at once.
     */
    @GameTest(template = ARENA, batch = "boss_grip_minion_edge", timeoutTicks = 60)
    public static void aThrownMinionStopsAtTheWallAndComesBack(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(ApocalypseBossConfig.ARENA_RADIUS, 5);
        f.config(ApocalypseBossConfig.ARENA_HEIGHT, 16);
        ServerPlayer starter = f.player(10.5, FLOOR, 10.5, Power.BILLY);
        ApocalypseTestSupport.tank(starter);
        ApocalypseFight fight = ApocalypseTestSupport.inPhase(f, starter, 1);
        ApocalypseTestSupport.put(f, ApocalypseTestSupport.boss(fight), 10.5, FLOOR, 4.5);
        BossArena arena = fight.arena();
        Vec3 centre = arena.center();
        double wall = arena.containRadius();
        check(wall == 9.0, "the wall stands at the radius plus four: " + wall);
        Husk horseman = fight.horsemen().spawn(f.level(), Horsemen.Kind.FAMINE, helper.absoluteVec(new Vec3(10.5 + wall - 4.0, FLOOR, 10.5)));
        horseman.setNoAi(true);
        float health = horseman.getHealth();
        ApocalypseTestSupport.put(f, starter, 8.5, FLOOR, 10.5);
        lookAt(starter, horseman);
        press(starter, 3, false);
        check(Telekinesis.holds(starter, horseman), "Billy holds a Horseman 4 blocks inside the wall");
        look(starter, EAST, 0.0F);
        press(starter, 3, false);
        check(!Telekinesis.isHolding(starter), "and throws it straight at the wall");
        double[] farthest = {0.0};
        f.everyTick(20, tick -> {
            double distance = arena.distance(horseman.position());
            farthest[0] = Math.max(farthest[0], distance);
            check(distance <= wall, "it is never beyond the wall: " + distance + " at tick " + tick);
            if (tick < 3) {
                return false;
            }
            check(horseman.getHealth() < health, "it crashed at the wall");
            assertNear(distance, wall - 2.0, 0.05, "it is back two blocks inside the wall");
            assertNear(horseman.getY(), centre.y, 0.05, "on the sand");
            check(horseman.getDeltaMovement().horizontalDistance() < 1.0E-3, "and it is not flying");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The holder's switches, PvP, a still flight, a holder who leaves, other dimensions

    /**
     * T42 (D-T 4.2, 9): with both switches off, Wanda's V at Ultron says he is too powerful to lift, holds nothing and tears
     * nothing out of the ground behind him; Thanos's sword cannot be caught; a boss bolt Billy catches and throws at a
     * Sentry keeps the fight's mark, so it spares the Sentry (3.0).
     */
    @GameTest(template = ARENA, batch = "boss_grip_switch", timeoutTicks = 60)
    public static void theBossGripsHaveSwitches(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(BossConfig.TELEKINESIS_HOLDS_BOSSES, false);
        f.config(BossConfig.TELEKINESIS_CATCHES_BOSS_SHOTS, false);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        ServerPlayer billy = f.player(3.5, FLOOR, 16.5, Power.BILLY);
        UltronTestSupport.tank(billy);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, wanda, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 10.5, FLOOR + 1.0, 10.5);
        BlockPos dirt = new BlockPos(16, 2, 10);
        helper.setBlock(dirt, Blocks.DIRT);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(dirt)));
        press(wanda, 3, false);
        check(BossGrip.TOO_POWERFUL.equals(lastActionBarKey(wanda)) && !Telekinesis.isHolding(wanda), "too powerful to lift, nothing held");
        check(helper.getBlockState(dirt).is(Blocks.DIRT), "and the ground behind him is not torn out");

        ThanosSwordEntity sword = ModEntities.THANOS_SWORD.get().create(level);
        check(sword != null && !Telekinesis.canLift(billy, sword, Mode.BILLY), "Thanos's sword cannot be caught");

        UltronSentryEntity sentry = UltronTestSupport.sentryAt(f, fight, 16.5, 16.5);
        float health = sentry.getHealth();
        UltronBoltEntity bolt = UltronBoltEntity.create(level, body, billy.position().add(4.0, 1.5, 0.0), new Vec3(-0.5, 0.0, 0.0), 4.0F,
                UltronBoltEntity.Palette.PRIME);
        fight.adopt(bolt);
        level.addFreshEntity(bolt);
        Telekinesis.hold(billy, bolt, Mode.BILLY);
        check(Telekinesis.holds(billy, bolt), "Billy catches a boss bolt");
        lookAt(billy, sentry);
        press(billy, 3, false);
        check(!Telekinesis.isHolding(billy), "and throws it at a Sentry");
        f.everyTick(30, tick -> {
            check(sentry.getHealth() == health, "the bolt spares its side's Sentry (tick " + tick + ")");
            if (tick == 1) {
                check(BossFights.fightOf(bolt) != null, "the bolt keeps the fight's mark");
            }
            return tick >= 30;
        });
    }

    /**
     * T43 (D-T 7): with PvP off, nothing thrown hurts a player. Ultron thrown by Wanda crashes into a zombie (15) and then
     * into a Survival player, whose health is unchanged; a boss bolt Billy caught and throws at the player leaves him whole;
     * two stones Wanda sets down onto the player's cells never become blocks (D-T 2.3 rule 1). (The meteor of D-T 14.2
     * is lane TK's step 7: a shot is not yet caught.)
     */
    @GameTest(template = ARENA, batch = "boss_grip_pvp", timeoutTicks = 160)
    public static void thrownBossesSparePlayersWithoutPvP(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.pvp(false);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(BossConfig.PROTECT_ARENA, false); // (Wanda tears stones out of the floor of his arena: real ones, step 5 lends)
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        ServerPlayer billy = f.player(3.5, FLOOR, 17.5, Power.BILLY);
        ServerPlayer friend = f.player(15.5, FLOOR, 10.5);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, wanda, 2.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 7.5, FLOOR + 1.0, 10.5);
        // He flies 2.6 blocks in his first tick and 2.37 in the next (2.6 x 0.91): the zombie, then the player, in his path.
        Zombie zombie = stillZombie(helper, 10.1, 10.5);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        zombie.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
        UltronTestSupport.put(f, friend, 12.47, FLOOR, 10.5);
        float friendHealth = friend.getHealth();
        lookAt(wanda, body);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, body), "Wanda holds Ultron");
        look(wanda, EAST, 0.0F);
        press(wanda, 3, false);
        check(!Telekinesis.isHolding(wanda), "and throws him along the row");
        f.everyTick(150, tick -> {
            PowerTicker.tick(wanda);
            PowerTicker.tick(billy);
            check(friend.getHealth() == friendHealth, "nothing thrown hurts the player (tick " + tick + ")");
            if (tick == 6) {
                assertNear(zombie.getMaxHealth() - zombie.getHealth(), 15.0, 0.01, "the zombie took 15 from the boss crashing into it");
                // A boss bolt Billy caught, thrown at the player.
                UltronTestSupport.put(f, friend, 10.5, FLOOR, 17.5);
                UltronBoltEntity bolt = UltronBoltEntity.create(level, body, billy.position().add(2.0, 1.5, 0.0), Vec3.ZERO, 4.0F,
                        UltronBoltEntity.Palette.PRIME);
                fight.adopt(bolt);
                level.addFreshEntity(bolt);
                Telekinesis.hold(billy, bolt, Mode.BILLY);
                lookAt(billy, friend);
                press(billy, 3, false);
                check(!Telekinesis.isHolding(billy), "Billy throws a caught bolt at the player");
                return false;
            }
            if (tick < 120) {
                setDownOnto(helper, wanda, friend, tick);
                return false;
            }
            return stonesDidNotStand(helper, friend);
        });
    }

    /**
     * Two stones Wanda sets down onto the player: each is torn out of the floor with Sneak + V (the block only), carried
     * over his head and let go there, where it falls into his cells.
     */
    private static void setDownOnto(GameTestHelper helper, ServerPlayer wanda, ServerPlayer friend, int tick) {
        BlockPos feet = friend.blockPosition();
        if (tick == 30 || tick == 62) {
            BlockPos stone = tick == 30 ? helper.absolutePos(new BlockPos(17, 2, 3)) : feet.offset(2, 0, 0);
            helper.getLevel().setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
            lookAt(wanda, Vec3.atCenterOf(stone));
            press(wanda, 3, true);
            check(Telekinesis.isHolding(wanda), "Wanda tears a stone out of the floor");
        } else if (tick == 38 || tick == 80) {
            TelekineticBlockEntity held = null;
            for (TelekineticBlockEntity block : helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get())) {
                if (Powers.get(wanda).heldEntityId() == block.getId()) {
                    held = block;
                }
            }
            check(held != null, "Wanda holds the stone");
            held.moveTo(feet.getX() + 0.5, friend.getY() + 3.0, feet.getZ() + 0.5);
            press(wanda, 3, true);
            check(!Telekinesis.isHolding(wanda), "and lets it go over the player's head");
        }
    }

    /** Neither stone stood in the player's cells: both fell as cobblestone. */
    private static boolean stonesDidNotStand(GameTestHelper helper, ServerPlayer friend) {
        if (!helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).isEmpty()) {
            return false;
        }
        BlockPos feet = friend.blockPosition();
        check(!helper.getLevel().getBlockState(feet).is(Blocks.STONE) && !helper.getLevel().getBlockState(feet.above()).is(Blocks.STONE),
                "no stone stands in the player's feet or head cell");
        int cobblestone = 0;
        for (ItemEntity item : helper.getEntities(EntityType.ITEM)) {
            if (item.getItem().is(Items.COBBLESTONE)) {
                cobblestone += item.getItem().getCount();
            }
        }
        check(cobblestone == 2, "both stones fell as their loot: " + cobblestone);
        return true;
    }

    /**
     * T44 (D-T 4.4): a thrown boss does nothing while his throw keeps him still (at most {@code boss.thrownStillTicks}):
     * Ultron, his cooldowns reset and a target in reach, starts no attack and his brain does not tick; once the still is
     * over he attacks again within 60 ticks.
     */
    @GameTest(template = ARENA, batch = "boss_grip_flight", timeoutTicks = 120)
    public static void aThrownBossActsOnlyWhenHisFlightIsOver(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, wanda, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 9.5, FLOOR + 1.0, 10.5);
        lookAt(wanda, body);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, body), "Wanda holds Ultron");
        body.brain().setChoosing(true);
        body.brain().resetCooldowns();
        lookAt(wanda, body.position().add(6.0, 3.0, 0.0));
        press(wanda, 3, false);
        check(!Telekinesis.isHolding(wanda) && BossGrip.stills(body), "she throws him at once: he is still");
        long[] ticked = {body.brain().ticked()};
        int[] still = {0};
        int[] stillEnded = {-1};
        f.everyTick(100, tick -> {
            PowerTicker.tick(wanda);
            if (stillEnded[0] < 0 && BossGrip.stills(body)) {
                still[0]++;
                check(still[0] <= BossConfig.THROWN_STILL_TICKS.get(), "he is still at most 20 ticks");
                check(!body.state().isAttack(), "while still he starts no attack (tick " + tick + ")");
                check(body.brain().ticked() == ticked[0], "and his brain does not tick (tick " + tick + ")");
                return false;
            }
            if (stillEnded[0] < 0) {
                stillEnded[0] = tick;
            }
            if (body.state().isAttack()) {
                return true;
            }
            check(tick - stillEnded[0] <= 60, "he attacks within 60 ticks of the end of his still");
            return false;
        });
    }

    /**
     * T46 (D-T 4.3, 7): a holder who leaves lets the boss go, and his regrip time starts then. Wanda holds Apocalypse in a
     * window and logs out: the next tick he is not held, his regrip is 39 or 40 of {@code boss.regripTicks} (40 here), and
     * nothing keeps him still.
     */
    @GameTest(template = ARENA, batch = "boss_grip_logout", timeoutTicks = 40)
    public static void aHolderWhoLeavesLetsTheBossGo(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        d.f().config(BossConfig.REGRIP_TICKS, 40);
        ApocalypseTestSupport.breakShield(d);
        lookAt(d.hero(), d.boss());
        press(d.hero(), 3, false);
        check(Telekinesis.holds(d.hero(), d.boss()) && BossGrip.stills(d.boss()), "Wanda holds Apocalypse in a window: he is still");
        Telekinesis.onLogout(d.hero());
        helper.runAfterDelay(1, () -> {
            check(!Telekinesis.isHeld(d.boss()), "the tick after she left he is not held");
            long left = BossGrip.regripLeft(d.boss());
            check(left >= 39 && left <= 40, "his regrip runs from then: " + left);
            check(!BossGrip.stills(d.boss()), "and nothing keeps him still");
            helper.succeed();
        });
    }

    /** T46, the second half: the same when the holder dies, Wanda holding Thanos. */
    @GameTest(template = ARENA, batch = "boss_grip_death", timeoutTicks = 40)
    public static void aHolderWhoDiesLetsTheBossGo(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(BossConfig.REGRIP_TICKS, 40);
        ServerPlayer wanda = ThanosMoveSupport.fighter(f, -7.0, 0.0, Power.SCARLET_WITCH);
        ThanosEntity thanos = ThanosBossTests.boss(f, wanda);
        lookAt(wanda, thanos);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, thanos), "Wanda holds Thanos");
        wanda.kill();
        helper.runAfterDelay(1, () -> {
            check(!Telekinesis.isHeld(thanos), "the tick after she died he is not held");
            long left = BossGrip.regripLeft(thanos);
            check(left >= 39 && left <= 40, "his regrip runs from then: " + left);
            check(!BossGrip.stills(thanos), "and nothing keeps him still");
            helper.succeed();
        });
    }

    /**
     * T47 (D-T 7): the bosses never go through a portal (their fights are in their dimension), while a block Wanda lifted
     * outside any arena does as any entity. (A borrowed block's part is lane TK's step 5: nothing is borrowed yet.)
     */
    @GameTest(template = ARENA, batch = "boss_grip_dimension", timeoutTicks = 40)
    public static void bossesAndBorrowedBlocksStayInTheirDimension(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        Level other = nether != null ? nether : level;
        UltronEntity ultron = ModEntities.ULTRON.get().create(level);
        ApocalypseEntity apocalypse = ModEntities.APOCALYPSE.get().create(level);
        ThanosEntity thanos = ModEntities.THANOS.get().create(level);
        check(ultron != null && !ultron.canChangeDimensions(level, other), "an Ultron body stays in its dimension");
        check(apocalypse != null && !apocalypse.canChangeDimensions(level, other), "Apocalypse too");
        check(thanos != null && !thanos.canChangeDimensions(level, other), "and Thanos");
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        BlockPos dirt = new BlockPos(8, 2, 10);
        helper.setBlock(dirt, Blocks.DIRT);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(dirt)));
        press(wanda, 3, false);
        TelekineticBlockEntity lifted = null;
        for (TelekineticBlockEntity block : helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get())) {
            lifted = block;
        }
        check(lifted != null && lifted.canChangeDimensions(level, other), "a block lifted outside any arena goes through as any entity");
        Telekinesis.release(wanda, false);
        lifted.discard();
        helper.succeed();
    }

    /**
     * T49 (D-T 4.5, 6.2.1): Thanos is never carried below his centre minus 8, and thrown off the edge of the world's ground
     * (moved 30 blocks under his centre) he steps back onto it within his radius at the next tick of his fight, unhurt.
     */
    @GameTest(template = ARENA, batch = "boss_grip_void", timeoutTicks = 40)
    public static void thanosNeverFallsOutOfTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        ServerPlayer starter = ThanosMoveSupport.fighter(f, -7.0, 0.0);
        ThanosEntity thanos = ThanosBossTests.boss(f, starter);
        ThanosFight fight = (ThanosFight) BossFights.fightOf(thanos);
        check(fight != null, "he has his fight");
        Vec3 centre = fight.arena().center();
        Vec3 deep = centre.subtract(0.0, 20.0, 0.0);
        assertNear(BossFights.carryLimit(thanos, deep).y, centre.y - 8.0, 1.0E-6, "he is never carried below his centre minus 8");
        float health = thanos.getHealth();
        thanos.moveTo(thanos.getX(), centre.y - 30.0, thanos.getZ(), thanos.getYRot(), 0.0F);
        helper.runAfterDelay(2, () -> {
            check(thanos.isAlive() && thanos.getHealth() == health, "he is alive and unhurt");
            check(fight.arena().distance(thanos.position()) <= fight.arena().radius() - 4.0 + 1.0E-3, "4 blocks inside his radius at least");
            assertNear(thanos.getY(), centre.y, 0.05, "on the ground of his arena");
            check(!BossGrip.stills(thanos), "and nothing keeps him still");
            helper.succeed();
        });
    }

    /**
     * T50 (D-T D2, 4.6): Sneak + V takes the ground even past a boss. Apocalypse's shield is up and a block of sand stands
     * behind him on each hero's line of sight: Billy's V says {@code boss_shield}, shows one ripple and leaves the sand;
     * Billy's, Wanda's and the Phoenix's Sneak + V each take the sand behind him, hold not him, and show no ripple. (With
     * {@code boss.protectArena} off the sand is the world's own: the arena's loans are lane TK's step 5.)
     */
    @GameTest(template = ARENA, batch = "telekinesis_sneak_boss", timeoutTicks = 40)
    public static void sneakTakesTheGroundEvenPastABoss(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.BILLY);
        d.f().onClose(Telekinesis::clear);
        d.f().config(BossConfig.PROTECT_ARENA, false);
        ApocalypseEntity boss = d.boss();
        ServerPlayer billy = d.hero();
        ServerPlayer wanda = d.f().player(10.5, FLOOR, 3.5, Power.SCARLET_WITCH);
        ServerPlayer phoenix = d.f().player(17.5, FLOOR, 10.5, Power.PHOENIX);
        ServerPlayer[] heroes = {billy, wanda, phoenix};
        BlockPos[] sand = {new BlockPos(16, 2, 10), new BlockPos(10, 2, 16), new BlockPos(5, 2, 10)};
        ApocalypseTestSupport.Log log = new ApocalypseTestSupport.Log(billy);
        for (BlockPos cell : sand) {
            helper.setBlock(cell, Blocks.SAND);
        }
        check(boss.shield().isUp(), "his shield is up");
        lookAt(billy, Vec3.atCenterOf(helper.absolutePos(sand[0])));
        press(billy, 3, false);
        log.drain();
        check(log.toldOnActionBar(BossGrip.SHIELD) && !Telekinesis.isHolding(billy), "Billy's V: his shield turns it away");
        check(helper.getBlockState(sand[0]).is(Blocks.SAND), "and the sand behind him stays");
        check(log.effects(ApocalypseFxPayload.SHIELD_HIT).size() == 1, "one golden ripple for the press");
        log.clear();
        for (int i = 0; i < heroes.length; i++) {
            lookAt(heroes[i], Vec3.atCenterOf(helper.absolutePos(sand[i])));
            press(heroes[i], 3, true);
            check(helper.getBlockState(sand[i]).isAir() && Telekinesis.isHolding(heroes[i]) && !Telekinesis.isHeld(boss),
                    heroes[i].getGameProfile().getName() + "'s Sneak + V takes the sand behind him, not him");
        }
        check(log.drain().effects(ApocalypseFxPayload.SHIELD_HIT).isEmpty(), "and no ripple");
        for (ServerPlayer hero : heroes) {
            Telekinesis.release(hero, false);
        }
        helper.succeed();
    }

    private BossGripTests() {
    }
}

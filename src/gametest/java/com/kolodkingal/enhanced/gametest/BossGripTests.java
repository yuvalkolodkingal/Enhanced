package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.Telekinesis.Mode;
import com.kolodkingal.enhanced.apocalypse.ApocalypseArena;
import com.kolodkingal.enhanced.apocalypse.ApocalypseAttack;
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
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.network.ThanosFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosFight;
import com.kolodkingal.enhanced.thanos.ThanosMeteorEntity;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import com.kolodkingal.enhanced.ultron.UltronAttack;
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
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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

    /** The lifted block the player holds now, or null. */
    private static TelekineticBlockEntity heldBlock(GameTestHelper helper, ServerPlayer player) {
        int held = Powers.get(player).heldEntityId();
        for (TelekineticBlockEntity block : helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get())) {
            if (block.getId() == held && Telekinesis.isHolding(player)) {
                return block;
            }
        }
        return null;
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
    // The bosses' own answers (D-T 4.2 rule 4, 6.1.1, 6.2.1, 6.3.1)

    /** Checks that no form of telekinesis may take {@code boss}, each told he cannot be held now. */
    private static void untouchable(ServerPlayer holder, Entity boss, String when) {
        for (Mode mode : Mode.values()) {
            check(BossGrip.UNTOUCHABLE.equals(Telekinesis.refusal(holder, boss, mode)) && !Telekinesis.canLift(holder, boss, mode),
                    mode + ": he cannot be held " + when);
        }
    }

    /** Checks that every form of telekinesis may take {@code boss}. */
    private static void holdable(ServerPlayer holder, Entity boss, String when) {
        for (Mode mode : Mode.values()) {
            check(Telekinesis.refusal(holder, boss, mode) == null && Telekinesis.canLift(holder, boss, mode), mode + ": he can be held " + when);
        }
    }

    /**
     * T9, Ultron's part (D-T 6.1.1): no form holds a body of Ultron while he awakens, from the lift's warning on, nor while
     * he changes bodies (or dies); on the ground and on the island in between, every form does (Magneto too: vibranium is
     * metal).
     */
    @GameTest(template = ARENA, batch = "boss_grip_states", timeoutTicks = 160)
    public static void nobodyHoldsABossInHisInvulnerableStates(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        UltronFight fight = UltronTestSupport.start(f, wanda);
        f.everyTick(140, tick -> {
            UltronEntity body = fight.boss();
            if (body == null) {
                check(fight.phase() == UltronFight.Phase.AWAKENING, "he appears while he awakens");
                return false;
            }
            untouchable(wanda, body, "while he awakens");
            fight.forceState(UltronFight.Phase.GROUND);
            UltronTestSupport.calm(fight);
            holdable(wanda, body, "on the ground");
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            untouchable(wanda, body, "from the lift's warning on");
            fight.forceState(UltronFight.Phase.ISLAND);
            UltronTestSupport.calm(fight);
            holdable(wanda, body, "on the island");
            check(fight.onBodyDestroyed(body, null, null) && (fight.isTransferring() || fight.isDying()), "his body is destroyed");
            untouchable(wanda, body, "while he changes bodies or dies");
            return true;
        });
    }

    /**
     * T9, Thanos's part (D-T 6.2.1): no form holds Thanos while he arrives, behind his Space Barrier, nor while he dies;
     * arrived and in between, every form does (Magneto too: his gold armour and the Gauntlet are metal).
     */
    @GameTest(template = ARENA, batch = "boss_grip_states_thanos", timeoutTicks = 200)
    public static void nobodyHoldsThanosInHisInvulnerableStates(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer wanda = ThanosMoveSupport.fighter(f, -7.0, 0.0, Power.SCARLET_WITCH);
        ThanosFight fight = ThanosFight.spawnAt(f.level(), f.helper().absoluteVec(ThanosBossTests.CENTRE), wanda);
        ThanosEntity thanos = fight.boss();
        check(thanos != null && thanos.state() == ThanosState.ARRIVING, "he arrives");
        untouchable(wanda, thanos, "while he arrives");
        thanos.forceArrived();
        thanos.setPassive(true);
        thanos.forceStones(2);
        holdable(wanda, thanos, "once he has arrived");
        thanos.forceMove(ThanosMove.SPACE_BARRIER, wanda);
        boolean[] barrier = {false};
        f.everyTick(180, tick -> {
            if (!barrier[0]) {
                if (thanos.flag(ThanosEntity.FLAG_BARRIER)) {
                    barrier[0] = true;
                    untouchable(wanda, thanos, "behind his Space Barrier");
                }
                return false;
            }
            if (thanos.flag(ThanosEntity.FLAG_BARRIER)) {
                return false;
            }
            holdable(wanda, thanos, "once his Barrier fell");
            thanos.setHealth(5.0F);
            ThanosBossTests.hit(thanos, wanda, 100.0F);
            check(thanos.state() == ThanosState.DYING, "he is beaten: " + thanos.state());
            untouchable(wanda, thanos, "while he dies");
            return true;
        });
    }

    /**
     * T9, Apocalypse's part (D-T 6.3.1): no form holds Apocalypse in a set piece (the first transition) nor while his
     * shield forms or re-forms; with his shield up, Wanda (mind power) does.
     */
    @GameTest(template = ARENA, batch = "boss_grip_states_apocalypse", timeoutTicks = 40)
    public static void nobodyHoldsApocalypseInHisInvulnerableStates(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        ServerPlayer wanda = d.hero();
        ApocalypseEntity boss = d.boss();
        check(Telekinesis.refusal(wanda, boss, Mode.WANDA) == null, "with his shield up, mind power holds him");
        d.fight().forceState(ApocalypseFight.Stage.TRANSITION_1, 1);
        untouchable(wanda, boss, "in his first transition");
        d.fight().forceState(ApocalypseFight.Stage.FIGHT, 1);
        boss.shield().reform(boss.shield().max());
        check(boss.shield().state() == ApocalypseShield.State.REFORMING, "his shield re-forms");
        untouchable(wanda, boss, "while his shield re-forms");
        boss.shield().form(boss.shield().max());
        untouchable(wanda, boss, "while his shield forms");
        helper.succeed();
    }

    /**
     * T48 (D-T 4.4, 6.1.1): a grip drops Ultron's attack but keeps his stagger after a Dive, which counts down in the grip.
     * Billy takes hold of him on the first tick of the stagger and lets go 20 ticks later: he is still staggered, 20 ticks
     * of it have passed, and it ends on the tick it would have ended without the grip.
     */
    @GameTest(template = ARENA, batch = "boss_grip_stagger", timeoutTicks = 300)
    public static void aGripKeepsUltronsStagger(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(BossConfig.REGRIP_TICKS, 0);
        ServerPlayer billy = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.BILLY);
        UltronTestSupport.tank(billy);
        UltronFight fight = UltronTestSupport.onTheGround(f, billy);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, billy, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 12.5, FLOOR + 3.0, 10.5);
        check(body.brain().force(body, fight, UltronAttack.DIVE), "he dives at Billy");
        int[] staggeredAt = {-1};
        int[] left = {0};
        f.everyTick(280, tick -> {
            PowerTicker.tick(billy);
            if (staggeredAt[0] < 0) {
                if (body.isStaggered()) {
                    staggeredAt[0] = tick;
                    left[0] = body.brain().staggerLeft();
                    check(left[0] > 20, "he is staggered after the Dive: " + left[0]);
                    lookAt(billy, body);
                    press(billy, 3, false);
                    check(Telekinesis.holds(billy, body), "Billy takes hold of him");
                }
                return false;
            }
            int since = tick - staggeredAt[0];
            if (since < 20) {
                check(Telekinesis.holds(billy, body) && body.isStaggered(), "held and still staggered (tick " + since + ")");
                return false;
            }
            if (since == 20) {
                Telekinesis.release(billy, false);
                check(body.isStaggered(), "let go after 20 ticks, he is still staggered");
                check(Math.abs(body.brain().staggerLeft() - (left[0] - 20)) <= 1,
                        "his stagger counted down in the grip: " + left[0] + " -> " + body.brain().staggerLeft());
                return false;
            }
            if (body.isStaggered()) {
                return false;
            }
            check(Math.abs(since - left[0]) <= 1, "the stagger ends when it would have without the grip: after " + since + " of " + left[0]);
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // A held boss does nothing (D-T 4.4, 6.1.1, 6.2.1, 6.3.1)

    /**
     * T8, Thanos's part (D-T 4.4, 6.2.1): a grip breaks his move off and lets go of what it held. Thanos chokes a player up
     * in the air; Wanda takes hold of him: the Choke is broken off, the player falls back to the floor and is hurt no more,
     * and no move begins while she holds him, though he chooses again (he is no longer passive). He breaks free after her
     * 40 ticks and acts again within 100: a move against the player beside him.
     */
    @GameTest(template = ARENA, batch = "boss_grip_still", timeoutTicks = 260)
    public static void aHeldBossDoesNothing(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer victim = ThanosMoveSupport.fighter(f, 0.0, 2.0);
        victim.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0)); // (let go up there, the fall alone hurts him)
        ServerPlayer wanda = ThanosMoveSupport.fighter(f, -7.0, 0.0, Power.SCARLET_WITCH);
        ThanosEntity thanos = ThanosBossTests.boss(f, victim);
        thanos.forceMove(ThanosMove.CHOKE, victim);
        double floor = helper.absoluteVec(new Vec3(0, FLOOR, 0)).y;
        int[] grabbedAt = {-1};
        int[] freeAt = {-1};
        float[] health = {0.0F};
        f.everyTick(240, tick -> {
            victim.doTick();
            PowerTicker.tick(wanda);
            if (grabbedAt[0] < 0) {
                if (tick < 24) {
                    return false;
                }
                check(thanos.currentMove() == ThanosMove.CHOKE && victim.getY() > floor + 0.3, "Thanos chokes the player, up off the floor");
                lookAt(wanda, thanos);
                press(wanda, 3, false);
                check(Telekinesis.holds(wanda, thanos), "Wanda takes hold of him");
                check(thanos.currentMove() == null, "his Choke is broken off");
                thanos.setPassive(false);
                grabbedAt[0] = tick;
                health[0] = victim.getHealth();
                return false;
            }
            if (Telekinesis.isHeld(thanos)) {
                check(thanos.currentMove() == null, "no move begins while he is held (tick " + (tick - grabbedAt[0]) + ")");
                check(victim.getHealth() == health[0], "the player is hurt no more");
                return false;
            }
            if (freeAt[0] < 0) {
                freeAt[0] = tick;
                check(tick - grabbedAt[0] == 40 && BossGrip.BROKE_FREE.equals(lastActionBarKey(wanda)), "he broke free after her 40 ticks");
                check(victim.getY() <= floor + 0.01, "the player he choked is back on the floor: " + (victim.getY() - floor));
            }
            if (thanos.currentMove() != null) {
                return true;
            }
            check(tick - freeAt[0] < 100, "he acts again within 100 ticks of the end of the grip");
            return false;
        });
    }

    /**
     * T8, Apocalypse's part (D-T 4.4, 6.3.1): in a window, with his brain busy (he has begun his Lances), Wanda takes hold of
     * him: his brain drops the attack, and begins none while she holds him, though it chooses again. Once he broke free
     * he acts again within 100 ticks. (The window opens with no stagger here, so his brain runs at once.)
     */
    @GameTest(template = ARENA, batch = "boss_grip_still_apocalypse", timeoutTicks = 200)
    public static void aHeldApocalypseDoesNothing(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        d.f().config(ApocalypseBossConfig.STAGGER_TICKS, 0);
        ServerPlayer wanda = d.hero();
        ApocalypseEntity boss = d.boss();
        ApocalypseTestSupport.breakShield(d);
        int[] freeAt = {-1};
        d.f().everyTick(180, tick -> {
            PowerTicker.tick(wanda);
            if (tick == 1) {
                return false; // (the fight counts its foes)
            }
            if (tick == 2) {
                boss.brain().clearCooldowns();
                check(boss.brain().force(boss, d.fight(), ApocalypseAttack.LANCES) && boss.brain().isBusy(), "in his window he begins his Lances");
                lookAt(wanda, boss);
                press(wanda, 3, false);
                check(Telekinesis.holds(wanda, boss) && !boss.brain().isBusy(), "Wanda takes hold of him: his brain drops the attack");
                boss.brain().setChoosing(true);
                return false;
            }
            if (Telekinesis.isHeld(boss)) {
                check(!boss.brain().isBusy(), "no attack begins while he is held (tick " + tick + ")");
                return false;
            }
            if (freeAt[0] < 0) {
                freeAt[0] = tick;
                check(boss.shield().state() == ApocalypseShield.State.BROKEN, "his window is still open");
                boss.brain().clearCooldowns();
            }
            if (boss.brain().isBusy()) {
                return true;
            }
            check(tick - freeAt[0] < 100, "he acts again within 100 ticks of the end of the grip");
            return false;
        });
    }

    /**
     * T8, Ultron's part (D-T 4.4, 6.1.1): Ultron begins a Volley at Wanda and she takes hold of him in its tell: the attack
     * is dropped, no bolt of his is fired while she holds him, and none begins, though his brain chooses again. Once he
     * broke free he attacks again within 100 ticks.
     */
    @GameTest(template = ARENA, batch = "boss_grip_still_ultron", timeoutTicks = 200)
    public static void aHeldUltronDoesNothing(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, wanda, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 12.5, FLOOR + 1.0, 10.5);
        check(body.brain().force(body, fight, UltronAttack.VOLLEY) && body.state().isAttack(), "he begins a Volley at Wanda");
        lookAt(wanda, body);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, body) && !body.state().isAttack(), "Wanda takes hold of him in its tell: the Volley is dropped");
        body.brain().setChoosing(true);
        body.brain().resetCooldowns();
        int[] freeAt = {-1};
        f.everyTick(180, tick -> {
            PowerTicker.tick(wanda);
            if (Telekinesis.isHeld(body)) {
                check(helper.getEntities(ModEntities.ULTRON_BOLT.get()).isEmpty(), "no bolt of his while he is held (tick " + tick + ")");
                check(!body.state().isAttack(), "and no attack begins");
                return false;
            }
            if (freeAt[0] < 0) {
                freeAt[0] = tick;
            }
            if (body.state().isAttack()) {
                return true;
            }
            check(tick - freeAt[0] < 100, "he attacks again within 100 ticks of the end of the grip");
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Apocalypse's shield (D-T 6.3, 6.3.1)

    /**
     * T10 (D-T 4.6, 6.3, 6.3.1): only mind power takes Apocalypse through his shield. With it up, Billy's and Magneto's
     * presses are turned away ({@code boss_shield}), each with exactly one golden ripple (10 ticks apart, past the ripple's
     * rate limit), and his Babel line is said once in all; a Horseman beside him is taken with no ripple, and one right
     * behind him on Billy's line is not taken through him. Wanda and the Phoenix hold him. In a window Billy holds him; in
     * his true form there is no metal on him for Magneto, and Billy holds him.
     */
    @GameTest(template = ARENA, batch = "boss_grip_shield", timeoutTicks = 60)
    public static void onlyMindPowerTakesApocalypseThroughHisShield(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.BILLY);
        d.f().onClose(Telekinesis::clear);
        d.f().config(BossConfig.REGRIP_TICKS, 0); // (the heroes take hold of him one after the other)
        ServerLevel level = d.level();
        ApocalypseEntity boss = d.boss();
        ServerPlayer billy = d.hero();
        ServerPlayer magneto = d.f().player(10.5, FLOOR, 3.5, Power.MAGNETO);
        ServerPlayer wanda = d.f().player(17.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer phoenix = d.f().player(16.5, FLOOR, 16.5, Power.PHOENIX);
        ApocalypseTestSupport.Log log = new ApocalypseTestSupport.Log(billy);
        check(boss.shield().isUp(), "his shield is up");
        ServerPlayer[] pressers = {billy, magneto, billy};
        int[] keys = {3, 1, 3};
        d.f().everyTick(40, tick -> {
            if (tick == 1 || tick == 11 || tick == 21) {
                int i = tick / 10;
                ServerPlayer hero = pressers[i];
                lookAt(hero, boss);
                if (hero == magneto) {
                    lastActionBarKey(magneto);
                }
                press(hero, keys[i], false);
                List<String> told = log.drain().actionBarKeys();
                String key = hero == magneto ? lastActionBarKey(magneto) : told.get(told.size() - 1);
                check(BossGrip.SHIELD.equals(key) && !Telekinesis.isHolding(hero) && !Telekinesis.isHeld(boss),
                        hero.getGameProfile().getName() + "'s press: his shield turns it away (" + key + ")");
                check(log.effects(ApocalypseFxPayload.SHIELD_HIT).size() == i + 1,
                        "exactly one golden ripple per press: " + log.effects(ApocalypseFxPayload.SHIELD_HIT).size() + " after " + (i + 1));
                return false;
            }
            if (tick < 31) {
                return false;
            }
            check(log.drain().timesSaid("speech.enhanced.apocalypse.babel") == 1, "his Babel line was said once in all");

            // A Horseman beside him is taken, with no ripple; one right behind him is not taken through him.
            Husk beside = d.fight().horsemen().spawn(level, Horsemen.Kind.FAMINE, helper.absoluteVec(new Vec3(10.5, FLOOR, 15.5)));
            beside.setNoAi(true);
            lookAt(billy, beside);
            press(billy, 3, false);
            check(Telekinesis.holds(billy, beside), "Billy's press at a Horseman beside him takes the Horseman");
            check(log.drain().effects(ApocalypseFxPayload.SHIELD_HIT).size() == 3, "and shows no ripple");
            Telekinesis.release(billy, false);
            beside.discard();
            Husk behind = d.fight().horsemen().spawn(level, Horsemen.Kind.FAMINE, helper.absoluteVec(new Vec3(15.5, FLOOR, 10.5)));
            behind.setNoAi(true);
            lookAt(billy, behind);
            press(billy, 3, false);
            List<String> told = log.drain().actionBarKeys();
            check(BossGrip.SHIELD.equals(told.get(told.size() - 1)) && !Telekinesis.isHolding(billy) && !Telekinesis.isHeld(behind),
                    "at a Horseman right behind him: his shield turns it away, and the Horseman is not taken through him");
            behind.discard();

            // Mind power holds him through it.
            for (ServerPlayer hero : new ServerPlayer[] {wanda, phoenix}) {
                lookAt(hero, boss);
                press(hero, 3, false);
                check(Telekinesis.holds(hero, boss), hero.getGameProfile().getName() + " (mind power) holds him through his shield");
                Telekinesis.release(hero, false);
            }

            // In a window, Billy holds him.
            boss.invulnerableTime = 0;
            boss.hurt(ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, wanda, wanda), boss.shield().strength() + 1.0F);
            check(boss.shield().state() == ApocalypseShield.State.BROKEN, "Wanda's beam breaks his shield");
            lookAt(billy, boss);
            press(billy, 3, false);
            check(Telekinesis.holds(billy, boss), "in his window Billy holds him");
            Telekinesis.release(billy, false);

            // In his true form there is no metal on him: Magneto has nothing to grip, Billy holds him.
            d.fight().forceState(ApocalypseFight.Stage.TRUE_FORM, 3);
            check(boss.isTrueForm() && !Telekinesis.isMagnetic(boss) && !Telekinesis.canLift(magneto, boss, Mode.MAGNETO),
                    "in his true form he wears no metal");
            lookAt(magneto, boss);
            lastActionBarKey(magneto);
            press(magneto, 1, false);
            check(!Telekinesis.isHolding(magneto) && "message.enhanced.telekinesis.no_metal".equals(lastActionBarKey(magneto)),
                    "Magneto's R at him: no metal in sight");
            lookAt(billy, boss);
            press(billy, 3, false);
            check(Telekinesis.holds(billy, boss), "Billy holds him");
            Telekinesis.release(billy, false);
            return true;
        });
    }

    /**
     * T10, the config's part (D-T 6.3.1): a server that names Billy in {@code apocalypse_shield.mindPowerHeroes} makes his
     * telekinesis mind power: he holds Apocalypse through his shield, and no ripple shows.
     */
    @GameTest(template = ARENA, batch = "boss_grip_shield_billy", timeoutTicks = 40)
    public static void billyNamedMindPowerHoldsApocalypseThroughHisShield(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.BILLY);
        d.f().onClose(Telekinesis::clear);
        d.f().config(ApocalypseShieldConfig.MIND_POWER_HEROES, List.of("scarlet_witch", "phoenix", "billy"));
        ServerPlayer billy = d.hero();
        ApocalypseEntity boss = d.boss();
        ApocalypseTestSupport.Log log = new ApocalypseTestSupport.Log(billy);
        check(boss.shield().isUp(), "his shield is up");
        lookAt(billy, boss);
        press(billy, 3, false);
        check(Telekinesis.holds(billy, boss), "Billy, named mind power, holds him through his shield");
        check(log.drain().effects(ApocalypseFxPayload.SHIELD_HIT).isEmpty(), "and no ripple shows");
        Telekinesis.release(billy, false);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ultron's heart and his Sentries (D-T 6.1.1, R40)

    /**
     * T15 (D-T 6.1.1): the heart rip stays Wanda's, and works on a body she holds. On the island: her empty-handed V at a
     * body at 15 percent tears the core out (as in 3.0); at 16 percent she holds the next body, and set to 14 percent while
     * she holds it her V tears its core out instead of throwing it. At 14 percent, her Sneak + V takes the block behind
     * him (a block his arena lends) and begins no upload (it never asks his fight). Billy at 14 percent holds him and throws
     * him: no rip.
     */
    @GameTest(template = ARENA, batch = "boss_grip_rip", timeoutTicks = 300)
    public static void theHeartRipStaysWandasAndWorksOnAHeldBody(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(BossConfig.REGRIP_TICKS, 0);
        f.config(BossConfig.THROWN_STILL_TICKS, 0);
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        ServerPlayer billy = f.player(16.5, FLOOR, 15.5, Power.BILLY);
        UltronTestSupport.tank(billy);
        UltronFight fight = UltronTestSupport.onTheIsland(f, wanda);
        UltronTestSupport.calm(fight);
        // Two bodies to upload into, far from the line of sight.
        UltronTestSupport.sentryAt(f, fight, 2.5, 2.5);
        UltronTestSupport.sentryAt(f, fight, 18.5, 2.5);
        UltronTestSupport.calm(fight);
        UltronTestSupport.put(f, wanda, 10.5, FLOOR, 15.5);
        BlockPos stone = new BlockPos(10, 3, 5);
        int[] step = {0};
        int[] waitFrom = {0};
        f.everyTick(280, tick -> {
            if (fight.isTransferring()) {
                return false;
            }
            UltronEntity body = UltronTestSupport.boss(fight);
            switch (step[0]) {
                case 0 -> {
                    UltronTestSupport.put(f, body, 10.5, FLOOR + 1.0, 9.5);
                    body.setHealth(body.getMaxHealth() * 0.149F);
                    lookAt(wanda, body);
                    press(wanda, 3, false);
                    check(fight.isTransferring() && fight.uploads() == 1 && !Telekinesis.isHolding(wanda),
                            "at 15 percent her empty-handed V tears the core out: he uploads");
                }
                case 1 -> {
                    UltronTestSupport.calm(fight);
                    UltronTestSupport.put(f, body, 10.5, FLOOR + 1.0, 9.5);
                    body.setHealth(body.getMaxHealth() * 0.16F);
                    lookAt(wanda, body);
                    press(wanda, 3, false);
                    check(Telekinesis.holds(wanda, body) && !fight.isTransferring(), "at 16 percent she holds his next body");
                    body.setHealth(body.getMaxHealth() * 0.14F);
                    press(wanda, 3, false);
                    check(fight.isTransferring() && fight.uploads() == 2, "set to 14 percent while she holds him, her V tears the core out");
                    check(!Telekinesis.isHolding(wanda) && !BossGrip.stills(body), "instead of throwing him");
                }
                case 2 -> {
                    UltronTestSupport.calm(fight);
                    UltronTestSupport.put(f, body, 10.5, FLOOR + 1.0, 9.5);
                    body.setHealth(body.getMaxHealth() * 0.14F);
                    helper.setBlock(stone, Blocks.STONE);
                    lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(stone)));
                    press(wanda, 3, true);
                    check(!fight.isTransferring() && fight.uploads() == 2, "her Sneak + V past him at 14 percent begins no upload");
                    TelekineticBlockEntity borrowed = heldBlock(helper, wanda);
                    check(borrowed != null && !Telekinesis.isHeld(body) && helper.getBlockState(stone).isAir(),
                            "it takes the block behind him, not him");
                    check(borrowed.isBorrowed(), "a block his arena lends");
                    for (TelekineticBlockEntity block : helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get())) {
                        block.discard();
                    }
                    Telekinesis.release(wanda, false);
                    waitFrom[0] = tick;
                }
                case 3 -> {
                    lookAt(billy, body);
                    press(billy, 3, false);
                    check(Telekinesis.holds(billy, body) && !fight.isTransferring(), "Billy at 14 percent holds him: no rip");
                    look(billy, billy.getYRot(), -60.0F);
                    press(billy, 3, false);
                    check(!Telekinesis.isHolding(billy) && !fight.isTransferring() && fight.uploads() == 2, "and his V throws him: no rip");
                    return true;
                }
                default -> {
                }
            }
            step[0]++;
            return false;
        });
    }

    /**
     * T16 (D-T 6.1.1, R40): the Sentry crush is Wanda's and Magneto's. A held Sentry of the fight thrown by Wanda dies at
     * once, what was left of it hers; thrown by Magneto it dies too, in 24 grey metal sparks with the clang of an anvil and
     * the snap of a chain, what was left of it his. Thrown by Billy or the Phoenix it flies and lives. Magneto's throw of a
     * body at 14 percent on the island tears no heart out (the rip is Wanda's alone).
     */
    @GameTest(template = ARENA, batch = "boss_grip_crush", timeoutTicks = 80)
    public static void theSentryCrushIsWandasAndMagnetos(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        ServerPlayer magneto = f.player(3.5, FLOOR, 7.5, Power.MAGNETO);
        ServerPlayer billy = f.player(3.5, FLOOR, 13.5, Power.BILLY);
        ServerPlayer phoenix = f.player(3.5, FLOOR, 17.5, Power.PHOENIX);
        for (ServerPlayer hero : new ServerPlayer[] {wanda, magneto, billy, phoenix}) {
            UltronTestSupport.tank(hero);
        }
        UltronFight fight = UltronTestSupport.onTheIsland(f, wanda);
        UltronTestSupport.calm(fight);
        UltronTestSupport.put(f, wanda, 3.5, FLOOR, 3.5);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, body, 16.5, FLOOR + 1.0, 7.5);
        UltronSentryEntity forWanda = UltronTestSupport.sentryAt(f, fight, 7.5, 3.5);
        UltronSentryEntity forMagneto = UltronTestSupport.sentryAt(f, fight, 7.5, 7.5);
        UltronSentryEntity forBilly = UltronTestSupport.sentryAt(f, fight, 7.5, 13.5);
        UltronSentryEntity forPhoenix = UltronTestSupport.sentryAt(f, fight, 7.5, 17.5);
        UltronTestSupport.calm(fight);
        f.everyTick(60, tick -> {
            if (tick == 1) {
                return false; // (the heroes join the fight)
            }
            if (tick == 2) {
                // Wanda's throw crushes it, as in 3.0.
                float health = forWanda.getHealth();
                lookAt(wanda, forWanda);
                press(wanda, 3, false);
                check(Telekinesis.holds(wanda, forWanda), "Wanda holds a Sentry");
                press(wanda, 3, false);
                check(!Telekinesis.isHolding(wanda) && forWanda.isDeadOrDying(), "her throw crushes it");
                assertNear(fight.participant(wanda.getUUID()).damage(), health, 0.01, "what was left of it is hers");

                // Magneto's throw crushes it too (R40), in grey metal sparks, an anvil's clang and a chain's snap.
                health = forMagneto.getHealth();
                lookAt(magneto, forMagneto);
                press(magneto, 1, false);
                check(Telekinesis.holds(magneto, forMagneto), "Magneto's R holds a Sentry");
                TestKit.sentPackets(magneto);
                press(magneto, 1, false);
                List<Object> sent = TestKit.sentPackets(magneto);
                check(!Telekinesis.isHolding(magneto) && forMagneto.isDeadOrDying(), "his throw crushes it");
                assertNear(fight.participant(magneto.getUUID()).damage(), health, 0.01, "what was left of it is his");
                boolean sparks = TestKit.particlesIn(sent).stream().anyMatch(p -> p.getCount() == 24
                        && p.getParticle() instanceof EnergyParticleOptions energy && energy.color() == Power.MAGNETO.color);
                check(sparks, "24 sparks of his metal grey burst from it");
                List<TestKit.HeardSound> heard = TestKit.soundsIn(sent);
                check(heard.stream().anyMatch(s -> s.id().equals(SoundEvents.ANVIL_LAND.getLocation()))
                        && heard.stream().anyMatch(s -> s.id().equals(SoundEvents.CHAIN_BREAK.getLocation())), "with an anvil's clang and a chain's snap");

                // Billy's and the Phoenix's throws send it flying, into the floor: it lives.
                for (ServerPlayer hero : new ServerPlayer[] {billy, phoenix}) {
                    UltronSentryEntity sentry = hero == billy ? forBilly : forPhoenix;
                    lookAt(hero, sentry);
                    press(hero, 3, false);
                    check(Telekinesis.holds(hero, sentry), hero.getGameProfile().getName() + " holds a Sentry");
                    look(hero, hero.getYRot(), 45.0F);
                    press(hero, 3, false);
                    check(!Telekinesis.isHolding(hero) && sentry.isAlive(), hero.getGameProfile().getName() + "'s throw crushes nothing");
                }
                return false;
            }
            if (tick < 30) {
                return false;
            }
            check(forBilly.isAlive() && forPhoenix.isAlive(), "the Sentries Billy and the Phoenix threw live on");

            // Magneto's throw of a body at 14 percent, on the island, tears no heart out.
            body.setHealth(body.getMaxHealth() * 0.14F);
            lookAt(magneto, body);
            press(magneto, 1, false);
            check(Telekinesis.holds(magneto, body), "Magneto holds his body (vibranium is metal)");
            look(magneto, EAST, 0.0F);
            press(magneto, 1, false);
            check(!Telekinesis.isHolding(magneto) && !fight.isTransferring() && fight.uploads() == 0 && BossGrip.stills(body),
                    "his throw throws him: no heart torn out");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // A cocoon (D-T 6.3)

    /**
     * T19 (D-T 6.3): taking hold of someone Apocalypse's sand entombed breaks the cocoon and frees them, whatever the form
     * that does it: Billy's grip frees a pig, Magneto's a zombie in an iron chestplate.
     */
    @GameTest(template = ARENA, batch = "boss_grip_cocoon", timeoutTicks = 40)
    public static void anyFormFreesAnEntombedFriend(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = helper.getLevel();
        ServerPlayer billy = f.player(3.5, FLOOR, 6.5, Power.BILLY);
        ServerPlayer magneto = f.player(3.5, FLOOR, 14.5, Power.MAGNETO);
        // The Apocalypse hero's sand: a hero's cocoon, which no fight owns and no sweep takes away.
        ServerPlayer caster = f.player(17.5, FLOOR, 10.5, Power.APOCALYPSE);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.5, FLOOR, 6.5));
        Zombie zombie = stillZombie(helper, 8.5, 14.5);
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        SandCocoonEntity pigCocoon = SandControl.entomb(level, caster, pig, 100, 0.0F, 20.0F, null);
        SandCocoonEntity zombieCocoon = SandControl.entomb(level, caster, zombie, 100, 0.0F, 20.0F, null);
        check(pigCocoon != null && zombieCocoon != null && !pigCocoon.isRemoved() && !zombieCocoon.isRemoved()
                && pig.hasEffect(ModEffects.ENTOMBED) && zombie.hasEffect(ModEffects.ENTOMBED), "a pig and a zombie in cocoons of sand");
        lookAt(billy, pig);
        press(billy, 3, false);
        check(Telekinesis.holds(billy, pig), "Billy takes hold of the pig");
        check(pigCocoon.isRemoved() && !pig.hasEffect(ModEffects.ENTOMBED), "and that breaks its cocoon");
        lookAt(magneto, zombie);
        press(magneto, 1, false);
        check(Telekinesis.holds(magneto, zombie), "Magneto's R takes hold of the zombie in iron");
        check(zombieCocoon.isRemoved() && !zombie.hasEffect(ModEffects.ENTOMBED), "and that breaks its cocoon");
        Telekinesis.release(billy, false);
        Telekinesis.release(magneto, false);
        helper.succeed();
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

    /** A wall of stone across the floor, {@code x} the column it stands in (test coordinates), 6 blocks high. */
    private static void wallAt(GameTestHelper helper, int x, int fromZ, int toZ) {
        for (int z = fromZ; z <= toZ; z++) {
            for (int y = 2; y < 8; y++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
    }

    /** The same, across the floor the other way: {@code z} the row it stands in. */
    private static void wallAlong(GameTestHelper helper, int z, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) {
            for (int y = 2; y < 8; y++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
    }

    /**
     * T13, Ultron's part (D-T 4.5, 6.1): a thrown boss takes the impact himself, through his own caps. Prime at 11 percent,
     * on the ground ({@code ultron_boss.liftAt} 0.1 here, so the lift waits), thrown by Wanda into a wall: his floor of a
     * tenth holds (before the island is up he cannot fall), and the blow is hers in the fight's ledger.
     */
    @GameTest(template = ARENA, batch = "boss_grip_throw", timeoutTicks = 60)
    public static void aThrownBossTakesItThroughHisOwnCaps(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(UltronBossConfig.LIFT_AT, 0.1); // (at 11 percent he stays on the ground: the lift waits for a tenth)
        ServerPlayer wanda = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, wanda, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 7.5, FLOOR + 1.0, 10.5);
        wallAt(helper, 11, 7, 14);
        float floor = body.getMaxHealth() * 0.10F;
        body.setHealth(body.getMaxHealth() * 0.11F);
        f.everyTick(50, tick -> {
            PowerTicker.tick(wanda);
            if (tick == 2) {
                lookAt(wanda, body);
                press(wanda, 3, false);
                check(Telekinesis.holds(wanda, body), "Wanda holds Prime at 11 percent");
                look(wanda, EAST, 0.0F);
                press(wanda, 3, false);
                check(!Telekinesis.isHolding(wanda), "and throws him into the wall");
                return false;
            }
            check(body.getHealth() >= floor - 0.01F, "his health never goes below a tenth: " + body.getHealth() + " at tick " + tick);
            return tick > 2 && fight.participant(wanda.getUUID()).damage() > 0.0F && !BossGrip.stills(body);
        });
    }

    /**
     * T13, Thanos's part (D-T 4.5, 6.2): Wanda throws Thanos into a wall, takes hold of him again as soon as she may (20
     * ticks after the throw, with {@code boss.regripTicks} 0: his still) and throws him into another wall, both blows within
     * 6 seconds: both reach his hurt as telekinetic slams, and exactly one plate is torn (once in 6 seconds).
     */
    @GameTest(template = ARENA, batch = "boss_grip_throw_thanos", timeoutTicks = 120)
    public static void aThrownThanosTakesItThroughHisOwnCaps(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(BossConfig.REGRIP_TICKS, 0);
        ServerPlayer wanda = ThanosMoveSupport.fighter(f, -6.0, 0.0, Power.SCARLET_WITCH);
        ThanosEntity thanos = ThanosBossTests.boss(f, wanda);
        wallAt(helper, 15, 6, 15);
        wallAlong(helper, 5, 6, 15);
        int plates = Integer.bitCount(thanos.plates());
        int[] slams = {0};
        Consumer<LivingIncomingDamageEvent> counter = event -> {
            if (event.getEntity() == thanos && event.getSource().is(ModDamageTypes.TELEKINETIC_SLAM)) {
                slams[0]++;
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, LivingIncomingDamageEvent.class, counter);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(counter));
        int[] thrownAt = {-1, -1};
        int[] slammedAt = {-1, -1};
        f.everyTick(110, tick -> {
            PowerTicker.tick(wanda);
            if (slams[0] > 0 && slammedAt[0] < 0) {
                slammedAt[0] = tick;
            }
            if (slams[0] > 1 && slammedAt[1] < 0) {
                slammedAt[1] = tick;
            }
            if (thrownAt[0] < 0) {
                lookAt(wanda, thanos);
                press(wanda, 3, false);
                check(Telekinesis.holds(wanda, thanos), "Wanda takes hold of Thanos");
                look(wanda, EAST, 0.0F);
                press(wanda, 3, false);
                thrownAt[0] = tick;
                return false;
            }
            if (thrownAt[1] < 0) {
                if (BossGrip.regripLeft(thanos) > 0) {
                    return false;
                }
                check(slams[0] == 1 && Integer.bitCount(thanos.plates()) == plates - 1, "the first blow tore a plate: " + slams[0]);
                check(tick - thrownAt[0] == 20, "she may take hold of him again 20 ticks after the throw: " + (tick - thrownAt[0]));
                lookAt(wanda, thanos);
                press(wanda, 3, false);
                check(Telekinesis.holds(wanda, thanos), "Wanda takes hold of him again as soon as she may");
                look(wanda, 180.0F, 0.0F); // north, at the second wall
                press(wanda, 3, false);
                thrownAt[1] = tick;
                return false;
            }
            if (slams[0] < 2) {
                return false;
            }
            check(slammedAt[1] - slammedAt[0] < 120, "both blows within 6 seconds: " + (slammedAt[1] - slammedAt[0]) + " ticks apart");
            check(Integer.bitCount(thanos.plates()) == plates - 1, "the second blow tore no plate: once in 6 seconds");
            return true;
        });
    }

    /**
     * T13, Apocalypse's part (D-T 4.5, 6.3): thrown by Wanda (mind power) with his shield up, he crashes into a zombie in
     * his path, which takes 15, and then into a wall: the blow wears his shield, not his health.
     */
    @GameTest(template = ARENA, batch = "boss_grip_throw_apocalypse", timeoutTicks = 60)
    public static void aThrownApocalypseTakesItThroughHisOwnCaps(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        ServerPlayer wanda = d.hero();
        ApocalypseEntity boss = d.boss();
        ApocalypseTestSupport.put(d.f(), wanda, 2.5, FLOOR, 10.5);
        ApocalypseTestSupport.put(d.f(), boss, 7.5, FLOOR, 10.5);
        // He flies 2.6 blocks in his first tick: the zombie is in his path, the wall after it.
        Zombie zombie = stillZombie(helper, 10.1, 10.5);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        zombie.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
        wallAt(helper, 15, 6, 15);
        float health = boss.getHealth();
        float strength = boss.shield().strength();
        lookAt(wanda, boss);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, boss), "Wanda (mind power) holds him through his shield");
        look(wanda, EAST, 0.0F);
        press(wanda, 3, false);
        check(!Telekinesis.isHolding(wanda), "and throws him at the wall");
        d.f().everyTick(50, tick -> {
            check(boss.getHealth() == health, "his health never changes (tick " + tick + ")");
            if (boss.shield().strength() >= strength) {
                return false;
            }
            assertNear(zombie.getMaxHealth() - zombie.getHealth(), 15.0, 0.01, "the zombie in his path took 15");
            check(boss.shield().isUp(), "the blow wore his shield: " + strength + " -> " + boss.shield().strength());
            return true;
        });
    }

    /**
     * T14, Ultron's part (D-T 4.5, 6.1.2): Ultron thrown out toward his field is stopped dead at it, and flashed back to the
     * ground three blocks inside it.
     */
    @GameTest(template = SokoviaTestSupport.PAD, batch = "boss_grip_home", timeoutTicks = 60)
    public static void aThrownBossComesBackInsideHisArena(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        clearTheRow(helper);
        double ground = SokoviaTestSupport.GROUND;
        double mx = SokoviaTestSupport.MIDDLE_X;
        double mz = SokoviaTestSupport.MIDDLE_Z;
        ServerPlayer wanda = f.player(mx, ground, mz, Power.SCARLET_WITCH);
        UltronTestSupport.tank(wanda);
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        BossArena arena = fight.arena();
        UltronTestSupport.put(f, wanda, mx + 8.0, ground, mz);
        UltronTestSupport.put(f, body, mx + 14.0, ground + 1.0, mz);
        lookAt(wanda, body);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, body), "Wanda holds Ultron");
        look(wanda, EAST, 0.0F);
        press(wanda, 3, false);
        check(!Telekinesis.isHolding(wanda), "and throws him toward the field");
        f.everyTick(50, tick -> {
            check(arena.distance(body.position()) <= arena.containRadius(), "he never crosses the field (tick " + tick + ")");
            if (BossGrip.stills(body)) {
                return false;
            }
            check(arena.distance(body.position()) <= arena.containRadius() - 3.0 + 0.01,
                    "he is back three blocks inside the field: " + arena.distance(body.position()));
            return true;
        });
    }

    /**
     * T14, Thanos's part (D-T 4.5, 6.2.1): his arena is open (no field, no wall): Thanos thrown against a wall past his
     * carry limit steps back through a Space portal, onto the ground 4 blocks inside his radius (8 here).
     */
    @GameTest(template = ARENA, batch = "boss_grip_home_thanos", timeoutTicks = 60)
    public static void aThrownThanosComesBackInsideHisArena(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(ThanosBossConfig.ARENA_RADIUS, 8);
        ServerPlayer wanda = ThanosMoveSupport.fighter(f, -5.0, 0.0, Power.SCARLET_WITCH);
        ThanosEntity thanos = ThanosBossTests.boss(f, wanda);
        ThanosFight fight = (ThanosFight) BossFights.fightOf(thanos);
        check(fight != null && fight.arena().radius() == 8.0, "his arena has a radius of 8");
        BossArena arena = fight.arena();
        ApocalypseTestSupport.put(f, thanos, ThanosBossTests.CENTRE.x + 3.0, FLOOR, ThanosBossTests.CENTRE.z);
        wallAt(helper, 19, 6, 15);
        lookAt(wanda, thanos);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, thanos), "Wanda holds Thanos");
        look(wanda, EAST, 0.0F);
        press(wanda, 3, false);
        check(!Telekinesis.isHolding(wanda), "and throws him at the wall, past his carry limit");
        double[] farthest = {0.0};
        f.everyTick(50, tick -> {
            farthest[0] = Math.max(farthest[0], arena.distance(thanos.position()));
            if (BossGrip.stills(thanos)) {
                return false;
            }
            check(farthest[0] > arena.radius() - 2.0, "he flew past his carry limit: " + farthest[0]);
            check(arena.distance(thanos.position()) <= arena.radius() - 4.0 + 0.01,
                    "he stepped back 4 blocks inside his radius: " + arena.distance(thanos.position()));
            assertNear(thanos.getY(), arena.center().y, 0.05, "on the ground");
            return true;
        });
    }

    /**
     * T14, Apocalypse's part (D-T 4.5, 6.3.1): thrown out toward his wall, he is stopped dead at it and comes back to the
     * middle of his arena, as his leash puts him.
     */
    @GameTest(template = ARENA, batch = "boss_grip_home_apocalypse", timeoutTicks = 60)
    public static void aThrownApocalypseComesBackToTheMiddle(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(ApocalypseBossConfig.ARENA_RADIUS, 5);
        f.config(ApocalypseBossConfig.ARENA_HEIGHT, 16);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ApocalypseTestSupport.tank(wanda);
        ApocalypseFight fight = ApocalypseTestSupport.inPhase(f, wanda, 1);
        ApocalypseEntity boss = ApocalypseTestSupport.boss(fight);
        BossArena arena = fight.arena();
        ApocalypseTestSupport.put(f, wanda, 5.5, FLOOR, 10.5);
        ApocalypseTestSupport.put(f, boss, 12.5, FLOOR, 10.5);
        lookAt(wanda, boss);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, boss), "Wanda holds Apocalypse");
        look(wanda, EAST, 0.0F);
        press(wanda, 3, false);
        check(!Telekinesis.isHolding(wanda), "and throws him toward his wall");
        f.everyTick(50, tick -> {
            check(arena.distance(boss.position()) <= arena.containRadius(), "he never crosses his wall (tick " + tick + ")");
            if (BossGrip.stills(boss)) {
                return false;
            }
            check(arena.distance(boss.position()) < 0.1, "he is back in the middle: " + arena.distance(boss.position()));
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
     * into a Survival player, whose health is unchanged; a boss bolt Billy caught and throws at the player flies through
     * him, leaving him whole, and hits the zombie behind him, for Billy; two stones Wanda sets down onto the player's cells
     * never become blocks (D-T 2.3 rule 1). (The meteor of D-T 14.2 needs a Thanos fight, and only one fight runs at a time:
     * {@link #aThrownMeteorSparesPlayersWithoutPvP}.)
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
        UltronBoltEntity[] bolt = {null};
        Zombie[] behind = {null};
        f.everyTick(150, tick -> {
            PowerTicker.tick(wanda);
            PowerTicker.tick(billy);
            check(friend.getHealth() == friendHealth, "nothing thrown hurts the player (tick " + tick + ")");
            if (tick == 6) {
                assertNear(zombie.getMaxHealth() - zombie.getHealth(), 15.0, 0.01, "the zombie took 15 from the boss crashing into it");
                // A boss bolt Billy caught, thrown at the player, with a zombie on its line just behind him.
                UltronTestSupport.put(f, friend, 10.5, FLOOR, 17.5);
                behind[0] = stillZombie(helper, 13.5, 17.5);
                bolt[0] = UltronBoltEntity.create(level, body, billy.position().add(2.0, 1.5, 0.0), Vec3.ZERO, 4.0F,
                        UltronBoltEntity.Palette.PRIME);
                fight.adopt(bolt[0]);
                level.addFreshEntity(bolt[0]);
                Telekinesis.hold(billy, bolt[0], Mode.BILLY);
                lookAt(billy, friend);
                press(billy, 3, false);
                check(!Telekinesis.isHolding(billy), "Billy throws a caught bolt at the player");
                return false;
            }
            if (behind[0] != null && bolt[0].isRemoved()) {
                Zombie hit = behind[0];
                check(hit.getHealth() < hit.getMaxHealth() && hit.getLastDamageSource() != null
                        && hit.getLastDamageSource().is(ModDamageTypes.ULTRON_BOLT) && hit.getLastDamageSource().getEntity() == billy,
                        "the bolt flew through the player and hit the zombie behind him, for Billy");
                hit.discard();
                behind[0] = null;
            }
            if (tick < 120) {
                setDownOnto(helper, wanda, friend, tick);
                return false;
            }
            check(behind[0] == null && bolt[0] != null, "the bolt reached the zombie behind the player");
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
     * T46 (D-T 4.3, 4.4, 7): a holder who leaves lets the boss go, and his regrip time starts then. Wanda holds Apocalypse
     * in a window (opened with no stagger here, so that nothing but the grip keeps his brain still) and logs out: the next
     * tick he is not held, his regrip is 39 or 40 of {@code boss.regripTicks} (40 here), nothing keeps him still, and his
     * brain runs again: he begins an attack within 100 ticks.
     */
    @GameTest(template = ARENA, batch = "boss_grip_logout", timeoutTicks = 140)
    public static void aHolderWhoLeavesLetsTheBossGo(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        d.f().config(BossConfig.REGRIP_TICKS, 40);
        d.f().config(ApocalypseBossConfig.STAGGER_TICKS, 0);
        ApocalypseEntity boss = d.boss();
        ApocalypseTestSupport.breakShield(d);
        int[] leftAt = {-1};
        d.f().everyTick(120, tick -> {
            if (tick == 1) {
                return false; // (the fight counts its foes)
            }
            if (tick == 2) {
                lookAt(d.hero(), boss);
                press(d.hero(), 3, false);
                check(Telekinesis.holds(d.hero(), boss) && BossGrip.stills(boss) && !boss.brain().isBusy(),
                        "Wanda holds Apocalypse in a window: he is still, and busy with nothing");
                boss.brain().setChoosing(true);
                boss.brain().clearCooldowns();
                Telekinesis.onLogout(d.hero());
                leftAt[0] = tick;
                return false;
            }
            if (tick == 3) {
                check(!Telekinesis.isHeld(boss), "the tick after she left he is not held");
                long left = BossGrip.regripLeft(boss);
                check(left >= 39 && left <= 40, "his regrip runs from then: " + left);
                check(!BossGrip.stills(boss), "and nothing keeps him still");
            }
            if (boss.brain().isBusy()) {
                return true;
            }
            check(tick - leftAt[0] < 100, "his brain runs again: he acts within 100 ticks of her leaving");
            return false;
        });
    }

    /**
     * T46, the second half: the same when the holder dies, Wanda holding Thanos (no longer passive). The tick after she
     * died he is not held, his regrip runs from then, nothing keeps him still, and within 100 ticks he begins a move at the
     * fighter beside him.
     */
    @GameTest(template = ARENA, batch = "boss_grip_death", timeoutTicks = 140)
    public static void aHolderWhoDiesLetsTheBossGo(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(BossConfig.REGRIP_TICKS, 40);
        ServerPlayer wanda = ThanosMoveSupport.fighter(f, -7.0, 0.0, Power.SCARLET_WITCH);
        ServerPlayer fighter = ThanosMoveSupport.fighter(f, 0.0, 2.0);
        ThanosEntity thanos = ThanosBossTests.boss(f, wanda);
        lookAt(wanda, thanos);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, thanos), "Wanda holds Thanos");
        thanos.setPassive(false);
        wanda.kill();
        f.everyTick(120, tick -> {
            if (tick == 1) {
                check(!Telekinesis.isHeld(thanos), "the tick after she died he is not held");
                long left = BossGrip.regripLeft(thanos);
                check(left >= 39 && left <= 40, "his regrip runs from then: " + left);
                check(!BossGrip.stills(thanos), "and nothing keeps him still");
            }
            if (thanos.currentMove() != null) {
                check(fighter.isAlive(), "(the fighter beside him is there to act on)");
                return true;
            }
            check(tick < 100, "he acts again within 100 ticks of her death");
            return false;
        });
    }

    /**
     * T47 (D-T 7): the bosses never go through a portal (their fights are in their dimension), while a block Wanda lifted
     * outside any arena does as any entity. A block a protected arena lent her stays in its dimension too: held in a nether
     * portal for 100 ticks, it is still in the overworld.
     */
    @GameTest(template = ARENA, batch = "boss_grip_dimension", timeoutTicks = 140)
    public static void bossesAndBorrowedBlocksStayInTheirDimension(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
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
        TelekineticBlockEntity lifted = heldBlock(helper, wanda);
        check(lifted != null && !lifted.isBorrowed() && lifted.canChangeDimensions(level, other), "a block lifted outside any arena goes through as any entity");
        Telekinesis.release(wanda, false);
        lifted.discard();

        // In a protected arena the block is borrowed, and held in a nether portal it goes nowhere.
        BossFrameworkTests.start(f, BossKind.ULTRON, wanda, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, true));
        BlockPos ground = new BlockPos(10, 2, 13);
        BlockPos portal = helper.absolutePos(new BlockPos(10, 3, 10));
        helper.setBlock(ground, Blocks.DIRT);
        level.setBlock(portal, Blocks.NETHER_PORTAL.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(ground)));
        press(wanda, 3, false);
        TelekineticBlockEntity borrowed = heldBlock(helper, wanda);
        check(borrowed != null && borrowed.isBorrowed() && !borrowed.canChangeDimensions(level, other), "a borrowed block stays in its dimension");
        boolean[] inThePortal = {false};
        f.everyTick(120, tick -> {
            lookAt(wanda, Vec3.atCenterOf(portal));
            PowerTicker.tick(wanda);
            check(borrowed.isAlive() && borrowed.level() == level && Telekinesis.holds(wanda, borrowed), "the borrowed block is held in the overworld");
            inThePortal[0] |= borrowed.getBoundingBox().intersects(new AABB(portal));
            if (tick < 105) {
                return false;
            }
            check(inThePortal[0] && level.getBlockState(portal).is(Blocks.NETHER_PORTAL), "it was held in the portal");
            Telekinesis.release(wanda, false);
            borrowed.crumble();
            return true;
        });
    }

    /**
     * T49 (D-T 4.5, 6.2.1): Thanos is never carried below his centre minus 8, and thrown off the edge of the world's ground
     * (moved 30 blocks under his centre) he steps back onto it within his radius at the next tick of his fight, unhurt.
     */
    @GameTest(template = ARENA, batch = "boss_grip_void", timeoutTicks = 40)
    public static void thanosNeverFallsOutOfTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        f.onClose(Telekinesis::clear);
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
     * Review r2 (spec 16, D-T 4.5, 6.2.1): Thanos is never left 24 blocks under his centre, and his step back puts him where
     * it is never needed again. He is called on a hill top 44 blocks over the floor (all of it above the test's barrier
     * ceiling). Moved down onto the open floor of a valley 27 blocks under his centre, with nothing over it, he steps back
     * at the next tick of his fight, once (a portal at each end), onto his landing (that column has no ground over his
     * carry floor), and stays there: no portal on the 20 ticks after. Moved into a cave 27 blocks under his centre, with
     * the hill's rock over him where the look for ground begins, he steps onto his landing too, never into the rock.
     */
    @GameTest(template = ARENA, batch = "boss_grip_valley", timeoutTicks = 80)
    public static void thanosFarDownAValleyStepsBackOnce(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = f.level();
        List<BlockPos> placed = new ArrayList<>();
        f.onClose(() -> placed.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)));
        stone(helper, placed, new BlockPos(9, 45, 9), new BlockPos(11, 45, 11)); // the hill top: he stands at y 46
        stone(helper, placed, new BlockPos(2, 18, 2), new BlockPos(4, 18, 4)); // the valley's floor, 27 under it
        stone(helper, placed, new BlockPos(16, 18, 16), new BlockPos(18, 18, 18)); // the cave's floor
        stone(helper, placed, new BlockPos(16, 48, 16), new BlockPos(18, 54, 18)); // its roof, through the centre + 6
        ServerPlayer starter = ThanosMoveSupport.fighter(f, -7.0, 0.0);
        ThanosFight fight = ThanosFight.spawnAt(level, helper.absoluteVec(new Vec3(10.5, 46.0, 10.5)), starter);
        ThanosEntity thanos = fight.boss();
        check(thanos != null, "the fight made him");
        thanos.forceArrived();
        thanos.setPassive(true);
        Vec3 landing = fight.landing();
        float health = thanos.getHealth();
        int[] seen = {0};
        f.everyTick(70, tick -> {
            seen[0] += portals(starter);
            Vec3 at = thanos.position();
            if (tick == 1 || tick == 31) {
                check(at.distanceTo(landing) < 0.05, "tick " + tick + ": he stands on the hill top: " + at);
                check(tick == 1 || seen[0] == 2, "no portal on the ticks after his step back: " + seen[0]);
                seen[0] = 0;
                thanos.moveTo(helper.absoluteVec(tick == 1 ? new Vec3(3.5, 19.0, 3.5) : new Vec3(17.5, 19.0, 17.5)));
                check(thanos.getY() < fight.arena().center().y - 24.0, "he is more than 24 blocks under his centre");
                return false;
            }
            if (tick >= 4) {
                String from = tick < 31 ? "the valley" : "the cave";
                check(at.distanceTo(landing) < 0.05, "tick " + tick + ": he stepped back from " + from + " onto his landing and stays: " + at);
                check(seen[0] == 2, "tick " + tick + ": once from " + from + ", a portal at each end: " + seen[0] + " portals");
                check(level.noCollision(thanos, thanos.getBoundingBox()), "he is not inside a block");
                check(thanos.isAlive() && thanos.getHealth() == health, "and unhurt");
            }
            return tick == 54;
        });
    }

    /** Stone in every cell from {@code from} to {@code to} (relative), noted in {@code placed} to be taken away at the end. */
    private static void stone(GameTestHelper helper, List<BlockPos> placed, BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            helper.setBlock(pos, Blocks.STONE);
            placed.add(helper.absolutePos(pos));
        }
    }

    /** How many of Thanos's portals the server has sent {@code player} since the last look. */
    private static int portals(ServerPlayer player) {
        return (int) ThanosSummonTests.fxKinds(TestKit.sentPackets(player)).stream().filter(kind -> kind == ThanosFxPayload.PORTAL).count();
    }

    /**
     * T50 (D-T D2, 4.6): Sneak + V takes the ground even past a boss. Apocalypse's shield is up and a block of sand stands
     * behind him on each hero's line of sight: Billy's V says {@code boss_shield}, shows one ripple and leaves the sand;
     * Billy's, Wanda's and the Phoenix's Sneak + V each borrow the sand behind him from his arena, hold not him, and show no
     * ripple.
     */
    @GameTest(template = ARENA, batch = "telekinesis_sneak_boss", timeoutTicks = 40)
    public static void sneakTakesTheGroundEvenPastABoss(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.BILLY);
        d.f().onClose(Telekinesis::clear);
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
            TelekineticBlockEntity borrowed = heldBlock(helper, heroes[i]);
            check(helper.getBlockState(sand[i]).isAir() && borrowed != null && borrowed.isBorrowed() && !Telekinesis.isHeld(boss),
                    heroes[i].getGameProfile().getName() + "'s Sneak + V borrows the sand behind him, not him");
        }
        check(log.drain().effects(ApocalypseFxPayload.SHIELD_HIT).isEmpty(), "and no ripple");
        for (ServerPlayer hero : heroes) {
            Telekinesis.release(hero, false);
        }
        helper.succeed();
    }

    /**
     * T43, the meteor's part (D-T 7; a Thanos fight of its own, PvP off): a meteor the Phoenix caught and throws at a
     * Survival player bursts on him and leaves him whole, while the zombie beside him takes the burst, her telekinetic
     * slam.
     */
    @GameTest(template = ARENA, batch = "boss_grip_pvp_meteor", timeoutTicks = 40)
    public static void aThrownMeteorSparesPlayersWithoutPvP(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.pvp(false);
        ServerPlayer phoenix = ThanosMoveSupport.fighter(f, -7.0, -4.0, Power.PHOENIX);
        ServerPlayer friend = ThanosMoveSupport.fighter(f, 5.0, -4.0);
        ThanosEntity thanos = ThanosBossTests.boss(f, phoenix);
        Zombie zombie = stillZombie(helper, 15.5, 8.0);
        ThanosMeteorEntity meteor = new ThanosMeteorEntity(ModEntities.THANOS_METEOR.get(), helper.getLevel());
        meteor.launch(thanos, helper.absoluteVec(new Vec3(7.5, FLOOR, 6.5)));
        Vec3 at = helper.absoluteVec(new Vec3(7.5, FLOOR + 1.0, 6.5));
        meteor.setPos(at.x, at.y, at.z);
        BossFights.fightOf(thanos).spawn(meteor);
        lookAt(phoenix, meteor);
        press(phoenix, 3, false);
        check(Telekinesis.holds(phoenix, meteor), "the Phoenix catches a meteor");
        lookAt(phoenix, friend);
        press(phoenix, 3, false);
        check(!Telekinesis.isHolding(phoenix) && meteor.isTurned(), "and throws it at the player");
        f.everyTick(30, tick -> {
            PowerTicker.tick(phoenix);
            check(friend.getHealth() == ThanosMoveSupport.MAX, "the player is never hurt (tick " + tick + ")");
            if (!meteor.isRemoved()) {
                return false;
            }
            check(zombie.getHealth() < zombie.getMaxHealth() && zombie.getLastDamageSource() != null
                    && zombie.getLastDamageSource().is(ModDamageTypes.TELEKINETIC_SLAM) && zombie.getLastDamageSource().getEntity() == phoenix,
                    "it burst on him: the zombie beside him took her telekinetic slam");
            zombie.discard();
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The vanilla bosses (R37)

    /** A player in Creative (no boss whose will is back goes for them) with a power. */
    private static ServerPlayer creative(TestKit.Fixture f, double x, double z, Power power) {
        ServerPlayer player = f.player(x, FLOOR, z, GameType.CREATIVE);
        Powers.setPower(player, power);
        return player;
    }

    /** A Wither standing on the floor, his spawn invulnerability over. */
    private static WitherBoss wither(GameTestHelper helper, double x, double z) {
        WitherBoss wither = helper.spawnWithNoFreeWill(EntityType.WITHER, new Vec3(x, FLOOR, z));
        wither.setInvulnerableTicks(0);
        return wither;
    }

    private static boolean marked(Entity e) {
        return e.getPersistentData().contains(BossGrip.NO_AI_MARK);
    }

    /**
     * T62 (R37, spec 5.2): the Wither, the Warden and the Elder Guardian are held as bosses, with no fight. The Ultron
     * hero's pull still passes the Wither by; Wanda's V holds him ({@code boss_grabbed}) without a will of his own ({@code
     * NoAI}, marked) for {@code boss.vanillaHoldTicks} (60); then he breaks free with his will back, and Billy's press at
     * once says he is still shaking it off. A Warden is held by Billy for half as long, and one coming out of the ground
     * cannot be held; an Elder Guardian is held by the Phoenix. A Wither whose {@code NoAI} was on before the grip keeps
     * it; a thrown one gets his will back on the first tick the still of his throw is over ({@code boss.thrownStillTicks}
     * 3 here, so that it ends while he still flies); a body that joins a level with the grip's mark gets its will back,
     * one with {@code NoAI} and no mark keeps it. The Ender Dragon and his parts are too vast for every form. The heroes
     * are in Creative, so that no boss whose will is back goes for them, and each boss is gone right after its part.
     */
    @GameTest(template = ARENA, batch = "vanilla_boss_grip", timeoutTicks = 200)
    public static void vanillaBossesAreHeldAsBosses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(BossConfig.THROWN_STILL_TICKS, 3);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = creative(f, 3.5, 10.5, Power.SCARLET_WITCH);
        ServerPlayer billy = creative(f, 3.5, 6.5, Power.BILLY);
        ServerPlayer phoenix = creative(f, 3.5, 14.5, Power.PHOENIX);
        ServerPlayer ultron = creative(f, 3.5, 17.5, Power.ULTRON);

        // The Ender Dragon and each of its parts: too vast to hold, for every form.
        EnderDragon dragon = EntityType.ENDER_DRAGON.create(level);
        check(dragon != null, "a dragon is made");
        for (Mode mode : Mode.values()) {
            check(Telekinesis.DRAGON.equals(Telekinesis.refusal(wanda, dragon, mode)) && !Telekinesis.canLift(wanda, dragon, mode),
                    mode + ": the Ender Dragon is too vast to hold");
            EnderDragonPart part = dragon.getSubEntities()[0];
            check(Telekinesis.DRAGON.equals(Telekinesis.refusal(wanda, part, mode)) && !Telekinesis.canLift(wanda, part, mode),
                    mode + ": and so is his " + part.name);
        }

        // The Ultron hero's Gravity Pull passes a Wither by (pull_immune): nothing is pulled, the key is not spent.
        WitherBoss wither = wither(helper, 9.5, 12.5);
        lookAt(ultron, wither);
        press(ultron, 3, false);
        check(Powers.get(ultron).isReady(3, level.getGameTime()), "the Ultron hero's pull takes nothing from the Wither");
        Zombie control = stillZombie(helper, 6.5, 17.5);
        lookAt(ultron, control);
        press(ultron, 3, false);
        check(!Powers.get(ultron).isReady(3, level.getGameTime()), "where a zombie is pulled");
        control.discard();

        // Wanda's V holds the Wither as a boss, without his will.
        lookAt(wanda, wither);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, wither) && BossGrip.GRABBED.equals(lastActionBarKey(wanda)), "Wanda takes hold of the Wither as a boss");
        check(wither.isNoAi() && marked(wither), "in her grip he has no will of his own, and the mark says why");

        int[] stage = {0};
        int[] stageAt = {0};
        Entity[] boss = {wither};
        f.everyTick(190, tick -> {
            PowerTicker.tick(wanda);
            PowerTicker.tick(billy);
            PowerTicker.tick(phoenix);
            int since = tick - stageAt[0];
            switch (stage[0]) {
                case 0 -> {
                    WitherBoss held = (WitherBoss) boss[0];
                    if (since < 60) {
                        check(Telekinesis.holds(wanda, held) && held.isNoAi(), "held without his will (tick " + since + ")");
                        return false;
                    }
                    check(!Telekinesis.isHolding(wanda) && BossGrip.BROKE_FREE.equals(lastActionBarKey(wanda)),
                            "after boss.vanillaHoldTicks he broke free");
                    check(!held.isNoAi() && !marked(held), "with his will back");
                    lookAt(billy, held);
                    press(billy, 3, false);
                    check(!Telekinesis.isHeld(held) && BossGrip.REGRIP.equals(lastActionBarKey(billy)),
                            "Billy's press at once: he is still shaking off the grip");
                    held.discard();

                    // A Wither gathering his strength cannot be held.
                    WitherBoss gathering = wither(helper, 9.5, 12.5);
                    gathering.setInvulnerableTicks(100);
                    lookAt(wanda, gathering);
                    press(wanda, 3, false);
                    check(!Telekinesis.isHolding(wanda) && BossGrip.UNTOUCHABLE.equals(lastActionBarKey(wanda)),
                            "a Wither gathering his strength cannot be held now");
                    gathering.discard();

                    // A Warden, held by Billy for half as long.
                    Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(9.5, FLOOR, 6.5));
                    lookAt(billy, warden);
                    press(billy, 3, false);
                    check(Telekinesis.holds(billy, warden) && BossGrip.GRABBED.equals(lastActionBarKey(billy)) && warden.isNoAi(),
                            "Billy takes hold of a Warden as a boss");
                    boss[0] = warden;
                    stage[0] = 1;
                    stageAt[0] = tick;
                }
                case 1 -> {
                    Warden warden = (Warden) boss[0];
                    if (since < 30) {
                        check(Telekinesis.holds(billy, warden) && warden.isNoAi(), "Billy holds the Warden (tick " + since + ")");
                        return false;
                    }
                    check(!Telekinesis.isHolding(billy) && BossGrip.BROKE_FREE.equals(lastActionBarKey(billy)) && !warden.isNoAi(),
                            "the Warden broke free of Billy after 60 x 0.5 = 30 ticks, his will back");
                    warden.setPose(Pose.EMERGING);
                    lookAt(billy, warden);
                    press(billy, 3, false);
                    check(!Telekinesis.isHolding(billy) && BossGrip.UNTOUCHABLE.equals(lastActionBarKey(billy)),
                            "a Warden coming out of the ground cannot be held now");
                    warden.discard();

                    // An Elder Guardian, held by the Phoenix.
                    ElderGuardian guardian = helper.spawnWithNoFreeWill(EntityType.ELDER_GUARDIAN, new Vec3(9.5, FLOOR, 15.5));
                    lookAt(phoenix, guardian);
                    press(phoenix, 3, false);
                    check(Telekinesis.holds(phoenix, guardian) && BossGrip.GRABBED.equals(lastActionBarKey(phoenix)) && guardian.isNoAi(),
                            "the Phoenix takes hold of an Elder Guardian as a boss");
                    press(phoenix, 3, true);
                    check(!Telekinesis.isHolding(phoenix) && !guardian.isNoAi() && !marked(guardian), "set down, he has his will back");
                    guardian.discard();

                    // A Wither whose NoAI was on before the grip keeps it after.
                    WitherBoss still = wither(helper, 9.5, 12.5);
                    still.setNoAi(true);
                    lookAt(wanda, still);
                    press(wanda, 3, false);
                    check(Telekinesis.holds(wanda, still) && still.isNoAi() && !marked(still), "Wanda holds a Wither who has no will anyway");
                    press(wanda, 3, true);
                    check(!Telekinesis.isHolding(wanda) && still.isNoAi(), "set down, he keeps the NoAI he had");
                    still.discard();

                    // A Wither thrown: no will while the still of his throw lasts, then his own.
                    WitherBoss thrown = wither(helper, 9.5, 12.5);
                    lookAt(wanda, thrown);
                    press(wanda, 3, false);
                    check(Telekinesis.holds(wanda, thrown), "Wanda takes hold of another Wither");
                    look(wanda, EAST, 0.0F);
                    press(wanda, 3, false);
                    check(!Telekinesis.isHolding(wanda) && BossGrip.stills(thrown) && thrown.isNoAi() && marked(thrown),
                            "and throws him: still, without his will");
                    boss[0] = thrown;
                    stage[0] = 2;
                    stageAt[0] = tick;
                }
                default -> {
                    WitherBoss thrown = (WitherBoss) boss[0];
                    if (BossGrip.stills(thrown)) {
                        check(since < 3 && thrown.isNoAi(), "no will while his throw keeps him still (tick " + since + ")");
                        return false;
                    }
                    check(since == 3 && !thrown.isNoAi() && !marked(thrown), "his will is back as the still ends (tick " + since + ")");
                    check(thrown.getDeltaMovement().horizontalDistance() > 0.5, "while his flight goes on: "
                            + thrown.getDeltaMovement().horizontalDistance());
                    thrown.discard();

                    // A body saved while a grip had its will: joining a level, it gets it back; one with no mark keeps its NoAI.
                    Vec3 at = helper.absoluteVec(new Vec3(9.5, FLOOR, 12.5));
                    Warden saved = EntityType.WARDEN.create(level);
                    Warden plain = EntityType.WARDEN.create(level);
                    check(saved != null && plain != null, "two Wardens are made");
                    saved.setNoAi(true);
                    saved.getPersistentData().putBoolean(BossGrip.NO_AI_MARK, true);
                    saved.moveTo(at.x, at.y, at.z);
                    plain.setNoAi(true);
                    plain.moveTo(at.x + 3.0, at.y, at.z);
                    level.addFreshEntity(saved);
                    level.addFreshEntity(plain);
                    check(!saved.isNoAi() && !marked(saved), "a body that joins with the grip's mark has its will back");
                    check(plain.isNoAi(), "one whose NoAI is its own keeps it");
                    saved.discard();
                    plain.discard();
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Spec 5.2, R37 (review fix): a vanilla boss thrown through a portal gets his will back on the other side. Wanda
     * throws a Warden and, in the still of the throw, a portal takes him to the Nether as it takes a creature (at once, the
     * chunk there held loaded as a portal holds it: {@link Entity#changeDimension}): the body that arrives has no {@code
     * NoAI} and no mark, his will his own again (nothing in the level he left could give it back to him there).
     */
    @GameTest(template = ARENA, batch = "vanilla_boss_grip_portal", timeoutTicks = 40)
    public static void aVanillaBossThrownThroughAPortalGetsHisWillBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        check(nether != null, "the server has a Nether");
        ServerPlayer wanda = creative(f, 3.5, 10.5, Power.SCARLET_WITCH);
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(9.5, FLOOR, 10.5));
        lookAt(wanda, warden);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, warden) && warden.isNoAi() && marked(warden), "Wanda holds a Warden without his will");
        look(wanda, EAST, 0.0F);
        press(wanda, 3, false);
        check(!Telekinesis.isHolding(wanda) && BossGrip.stills(warden) && warden.isNoAi(), "and throws him: still, without his will");

        // Over the Nether's roof: the chunk is held loaded as a portal holds the one it leads to.
        BlockPos there = new BlockPos(0, 130, 0);
        ChunkPos chunk = new ChunkPos(there);
        nether.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk, 3, there);
        f.onClose(() -> nether.getChunkSource().removeRegionTicket(TicketType.PORTAL, chunk, 3, there));
        nether.getChunk(chunk.x, chunk.z);
        Entity arrived = warden.changeDimension(new DimensionTransition(nether, Vec3.atBottomCenterOf(there), Vec3.ZERO, 0.0F, 0.0F,
                DimensionTransition.DO_NOTHING));
        check(arrived instanceof Warden && arrived.level() == nether && warden.isRemoved(), "a portal takes him to the Nether");
        Warden across = (Warden) arrived;
        across.discard(); // (looked at at once: he is gone before he could do anything in the Nether)
        check(!across.isNoAi() && !marked(across), "the body that arrives has his will back: no NoAI, no mark");
        helper.succeed();
    }

    /**
     * T62, with {@code boss.telekinesisHoldsVanillaBosses} off (R37): the Wither, the Warden and the Elder Guardian are too
     * powerful to lift, for Wanda, Billy and the Phoenix, as in 3.0; nothing is held and none loses his will.
     */
    @GameTest(template = ARENA, batch = "vanilla_boss_grip_off", timeoutTicks = 40)
    public static void vanillaBossesAreTooPowerfulWithTheSwitchOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(BossConfig.TELEKINESIS_HOLDS_VANILLA_BOSSES, false);
        ServerPlayer wanda = creative(f, 3.5, 10.5, Power.SCARLET_WITCH);
        ServerPlayer billy = creative(f, 3.5, 6.5, Power.BILLY);
        ServerPlayer phoenix = creative(f, 3.5, 14.5, Power.PHOENIX);
        WitherBoss wither = wither(helper, 9.5, 10.5);
        lookAt(wanda, wither);
        press(wanda, 3, false);
        check(!Telekinesis.isHolding(wanda) && BossGrip.TOO_POWERFUL.equals(lastActionBarKey(wanda)) && !wither.isNoAi(),
                "the Wither is too powerful to lift");
        check(!Telekinesis.canLift(wanda, wither, Mode.WANDA), "and cannot be lifted");
        wither.discard();
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(9.5, FLOOR, 6.5));
        lookAt(billy, warden);
        press(billy, 3, false);
        check(!Telekinesis.isHolding(billy) && BossGrip.TOO_POWERFUL.equals(lastActionBarKey(billy)) && !warden.isNoAi(),
                "so is the Warden");
        warden.discard();
        ElderGuardian guardian = helper.spawnWithNoFreeWill(EntityType.ELDER_GUARDIAN, new Vec3(9.5, FLOOR, 14.5));
        lookAt(phoenix, guardian);
        press(phoenix, 3, false);
        check(!Telekinesis.isHolding(phoenix) && BossGrip.TOO_POWERFUL.equals(lastActionBarKey(phoenix)) && !guardian.isNoAi(),
                "and the Elder Guardian");
        guardian.discard();
        helper.succeed();
    }

    private BossGripTests() {
    }
}

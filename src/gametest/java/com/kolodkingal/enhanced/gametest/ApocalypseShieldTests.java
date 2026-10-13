package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.apocalypse.ApocalypseAttack;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.apocalypse.MindPower;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.entity.SandLanceEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.boss;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.breakShield;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.duel;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.every;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.rel;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The heart of the fight (spec 3.5 and 8, tests 7 to 19): the one rule of what counts as mind power, the shield as a
 * state machine, and the boss that wears it: everything that is not mind power does exactly nothing, mind power wears the
 * shield and not his health, a broken shield is a window with its own numbers, the floors of the phases stop his health,
 * a window nobody uses is lost. Every test that touches a fight has a batch of its own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ApocalypseShieldTests {

    // ------------------------------------------------------------------------------------------------------------
    // Pure: the rule, the numbers, the machine

    @GameTest(template = ARENA, batch = "apocalypse_mind")
    public static void mindPowerIsExactlyWandaAndThePhoenix(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(3.5, FLOOR, 3.5, Power.SCARLET_WITCH);
        ServerPlayer phoenix = f.player(5.5, FLOOR, 3.5, Power.PHOENIX);
        ServerPlayer magneto = f.player(7.5, FLOOR, 3.5, Power.MAGNETO);
        ServerPlayer billy = f.player(9.5, FLOOR, 3.5, Power.BILLY);
        ServerPlayer plain = f.player(11.5, FLOOR, 3.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 14.5));

        // The Chaos Beam: Wanda's counts in full, no one else's does.
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.CHAOS_BEAM, wanda), 5.0F), 5.0, 1.0E-6, "a beam by a Scarlet Witch counts in full");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.CHAOS_BEAM, plain), 5.0F), 0.0, 1.0E-6, "a beam by a plain player is nothing");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.CHAOS_BEAM, magneto), 5.0F), 0.0, 1.0E-6, "nor by a Magneto");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.CHAOS_BEAM, billy), 5.0F), 0.0, 1.0E-6, "nor by a Billy");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.CHAOS_BEAM, phoenix), 5.0F), 5.0, 1.0E-6, "a Phoenix is a breaker whatever she throws");
        check(MindPower.shieldDamage(ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, null, null), 5.0F) == 0.0F, "a beam of nobody is nothing");
        check(MindPower.shieldDamage(ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, zombie, zombie), 5.0F) == 0.0F, "a beam of a Zombie is nothing");

        // The slam of what is thrown.
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.TELEKINETIC_SLAM, wanda), 7.0F), 7.0, 1.0E-6, "a slam by Wanda counts");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.TELEKINETIC_SLAM, phoenix), 7.0F), 7.0, 1.0E-6, "by the Phoenix too");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.TELEKINETIC_SLAM, magneto), 7.0F), 0.0, 1.0E-6, "a slam thrown by Magneto is not mind power");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.TELEKINETIC_SLAM, billy), 7.0F), 0.0, 1.0E-6, "nor Billy's");

        // The Burst: five times its damage.
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.PHOENIX_BURST, phoenix), 30.0F), 150.0, 1.0E-4, "a Burst of 30 removes 150");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.PHOENIX_BURST, phoenix), 8.0F), 40.0, 1.0E-4, "a Burst of 8 removes 40");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.PHOENIX_BURST, plain), 30.0F), 0.0, 1.0E-6, "a Burst by anyone else is nothing");

        // Everything else of their arsenal is not mind power.
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.HEX_BOLT, billy), 6.0F), 0.0, 1.0E-6, "a hex bolt is not in the tag");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.HEX_BOLT, wanda), 6.0F), 0.0, 1.0E-6, "even from Wanda");
        assertNear(MindPower.shieldDamage(level.damageSources().playerAttack(wanda), 6.0F), 0.0, 1.0E-6, "nor a punch of hers");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.CHAOS_BEAM, wanda), 0.0F), 0.0, 1.0E-6, "no damage, no wear");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.CHAOS_BEAM, wanda), -3.0F), 0.0, 1.0E-6, "and a healing is not wear");

        // The server's say: the list of heroes, and the factors.
        check(MindPower.breakers().equals(List.of(Power.SCARLET_WITCH, Power.PHOENIX)), "by default: a Scarlet Witch and a Phoenix: " + MindPower.breakers());
        f.config(ApocalypseShieldConfig.MIND_POWER_HEROES, List.of("billy"));
        check(MindPower.breakers().equals(List.of(Power.BILLY)), "the list is the server's: " + MindPower.breakers());
        check(MindPower.isBreaker(billy) && !MindPower.isBreaker(wanda) && !MindPower.isBreaker(Power.NONE), "now Billy is one and Wanda is not");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.TELEKINETIC_SLAM, billy), 7.0F), 7.0, 1.0E-6, "Billy's slam counts");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.TELEKINETIC_SLAM, wanda), 7.0F), 0.0, 1.0E-6, "and Wanda's no longer does");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.HEX_BOLT, billy), 7.0F), 0.0, 1.0E-6, "yet a hex bolt still needs the tag");
        f.config(ApocalypseShieldConfig.MIND_POWER_HEROES, List.of("scarlet_witch", "no_such_hero", "none"));
        check(MindPower.breakers().equals(List.of(Power.SCARLET_WITCH)), "a name that is no power is ignored: " + MindPower.breakers());
        f.config(ApocalypseShieldConfig.MIND_POWER_HEROES, List.of());
        check(MindPower.breakers().isEmpty() && !MindPower.isBreaker(wanda), "an empty list: nobody breaks it");
        f.config(ApocalypseShieldConfig.MIND_POWER_HEROES, List.of("scarlet_witch", "phoenix"));
        f.config(ApocalypseShieldConfig.CHAOS_BEAM_FACTOR, 2.0);
        f.config(ApocalypseShieldConfig.SLAM_FACTOR, 3.0);
        f.config(ApocalypseShieldConfig.PHOENIX_BURST_FACTOR, 4.0);
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.CHAOS_BEAM, wanda), 5.0F), 10.0, 1.0E-6, "the beam's factor is the server's");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.TELEKINETIC_SLAM, wanda), 5.0F), 15.0, 1.0E-6, "the slam's");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.PHOENIX_BURST, phoenix), 5.0F), 20.0, 1.0E-6, "the Burst's");
        helper.succeed();
    }

    private static DamageSource by(ServerLevel level, ResourceKey<DamageType> type, ServerPlayer player) {
        return ModDamageTypes.source(level, type, player, player);
    }

    @GameTest(template = ARENA, batch = "apocalypse_pure")
    public static void theMindPowerTagIsSharedWithThePhoenix(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer phoenix = f.player(5.5, FLOOR, 3.5, Power.PHOENIX);
        ServerPlayer billy = f.player(9.5, FLOOR, 3.5, Power.BILLY);
        var types = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (ResourceKey<DamageType> key : List.of(ModDamageTypes.CHAOS_BEAM, ModDamageTypes.TELEKINETIC_SLAM, ModDamageTypes.PHOENIX_BURST)) {
            check(types.getHolderOrThrow(key).is(ModTags.MIND_POWER), key.location() + " is mind power");
        }
        for (ResourceKey<DamageType> key : List.of(ModDamageTypes.HEX_BOLT, ModDamageTypes.SPEED_BLITZ, ModDamageTypes.SHRAPNEL, ModDamageTypes.SAND,
                ModDamageTypes.MJOLNIR, ModDamageTypes.LIGHTNING_BEAM, ModDamageTypes.HEX_WALL)) {
            check(!types.getHolderOrThrow(key).is(ModTags.MIND_POWER), key.location() + " is not");
        }
        // The Phoenix lane's own test of the same sentence, and ours, cannot drift apart.
        check(PhoenixPower.isPhoenixMindPower(by(level, ModDamageTypes.PHOENIX_BURST, phoenix)), "a Burst by the Phoenix is her mind power");
        check(PhoenixPower.isPhoenixMindPower(by(level, ModDamageTypes.TELEKINETIC_SLAM, phoenix)), "and so is her slam");
        check(!PhoenixPower.isPhoenixMindPower(by(level, ModDamageTypes.HEX_BOLT, phoenix)), "a hex bolt is not");
        check(!PhoenixPower.isPhoenixMindPower(by(level, ModDamageTypes.PHOENIX_BURST, billy)), "and the same damage by Billy is nobody's mind power");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.PHOENIX_BURST, phoenix), 10.0F), 50.0, 1.0E-4, "the Burst is worth its factor");
        assertNear(MindPower.shieldDamage(by(level, ModDamageTypes.PHOENIX_BURST, billy), 10.0F), 0.0, 1.0E-6, "and Billy's same damage is nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_pure")
    public static void shieldStrengthScalesWithPlayersAndPhase(GameTestHelper helper) {
        check(Math.round(ApocalypseShield.strengthFor(1, 1)) == 150, "shield 1, one fighter: 150");
        check(Math.round(ApocalypseShield.strengthFor(2, 1)) == 188, "shield 2: 187.5, shown 188: " + ApocalypseShield.strengthFor(2, 1));
        check(Math.round(ApocalypseShield.strengthFor(3, 1)) == 234, "shield 3: 234");
        check(Math.round(ApocalypseShield.strengthFor(1, 3)) == 255, "three fighters: 255");
        check(Math.round(ApocalypseShield.strengthFor(2, 3)) == 319, "319");
        check(Math.round(ApocalypseShield.strengthFor(3, 3)) == 398, "398");
        assertNear(ApocalypseShield.strengthFor(1, 0), 150.0, 1.0E-4, "no fighters counts as one");
        assertNear(ApocalypseShield.strengthFor(0, 1), 150.0, 1.0E-4, "and so does phase zero");
        check(ApocalypseShield.strengthFor(1, 6) > ApocalypseShield.strengthFor(1, 5), "every fighter adds");
        // His body is the same sentence.
        assertNear(ApocalypseEntity.designHealthFor(1), 900.0, 1.0E-3, "900 health for one");
        assertNear(ApocalypseEntity.designHealthFor(3), 1800.0, 1.0E-3, "1800 for three");
        assertNear(ApocalypseEntity.designHealthFor(6), 3150.0, 1.0E-3, "3150 for six");
        assertNear(ApocalypseEntity.designHealthFor(9), 3150.0, 1.0E-3, "and no more than six count");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_pure")
    public static void theShieldMachineKnitsWearsBreaksAndRegeneratesOnlyAfterTheDelay(GameTestHelper helper) {
        ApocalypseShield s = new ApocalypseShield();
        check(s.state() == ApocalypseShield.State.GONE && s.isVulnerable() && !s.isUp(), "before it forms there is no shield, and he is open");
        long now = 1000;
        s.form(100.0F);
        check(s.state() == ApocalypseShield.State.FORMING && !s.isVulnerable() && !s.isUp(), "forming: not up, and he cannot be hurt either");
        check(s.absorb(UUID.randomUUID(), 50.0F, now, 200) == ApocalypseShield.Result.IGNORED, "mind power does nothing to a shield that is still knitting");
        for (int i = 1; i < ApocalypseShield.FORM_TICKS; i++) {
            check(!s.tick(now++), "no window ends while it knits");
        }
        check(s.state() == ApocalypseShield.State.FORMING, "forty ticks to knit");
        assertNear(s.strength(), 100.0 * (ApocalypseShield.FORM_TICKS - 1) / ApocalypseShield.FORM_TICKS, 1.0E-3, "it grows as it knits");
        s.tick(now++);
        check(s.state() == ApocalypseShield.State.UP && s.strength() == 100.0F && s.fraction() == 1.0F, "then it is up, at full strength");

        // Wearing it: the same attacker is counted once per mindHitCooldownTicks, another at once.
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        check(s.absorb(a, 30.0F, now, 200) == ApocalypseShield.Result.WORN && s.strength() == 70.0F, "a hit of 30 wears it to 70");
        check(s.absorb(a, 30.0F, now + 5, 200) == ApocalypseShield.Result.IGNORED && s.strength() == 70.0F, "the same attacker again within the cooldown is not counted");
        check(s.absorb(b, 10.0F, now + 5, 200) == ApocalypseShield.Result.WORN && s.strength() == 60.0F, "another attacker is");
        check(s.absorb(a, 10.0F, now + 10, 200) == ApocalypseShield.Result.WORN && s.strength() == 50.0F, "the first one again after ten ticks");
        check(s.strain(10.0F, now + 11, 200) == ApocalypseShield.Result.WORN && s.strength() == 40.0F, "a strain has no cooldown");
        check(s.strain(10.0F, now + 11, 200) == ApocalypseShield.Result.WORN && s.strength() == 30.0F, "not even within the same tick");
        long last = s.lastMindHit();
        check(last == now + 11, "it remembers when it was last worn");

        // It does not regenerate until it has been left alone for the delay, then by a fraction a second.
        int delay = ApocalypseShieldConfig.SHIELD_REGEN_DELAY_TICKS.get();
        long t = last;
        for (int i = 1; i < delay; i++) {
            s.tick(++t);
        }
        check(s.strength() == 30.0F, "nothing back within " + delay + " ticks: " + s.strength());
        s.tick(++t);
        float perTick = (float) (100.0 * ApocalypseShieldConfig.SHIELD_REGEN_FRACTION.get() / 20.0);
        assertNear(s.strength(), 30.0F + perTick, 1.0E-4, "then it mends a little each tick");
        for (int i = 0; i < 19; i++) {
            s.tick(++t);
        }
        assertNear(s.strength(), 30.0F + 20 * perTick, 1.0E-3, "by its fraction a second");
        check(s.absorb(a, 1.0F, t, 200) == ApocalypseShield.Result.WORN, "wear starts the delay again");
        float after = s.strength();
        s.tick(++t);
        check(s.strength() == after, "no mending right after a hit");

        // Breaking: the last point breaks it, opens a window, and nothing wears a broken shield.
        check(s.absorb(b, 1000.0F, t + 20, 200) == ApocalypseShield.Result.BROKEN, "enough breaks it");
        check(s.state() == ApocalypseShield.State.BROKEN && s.isVulnerable() && s.windowLeft() == 200 && s.strength() == 0.0F, "a window of 200");
        check(s.absorb(b, 5.0F, t + 40, 200) == ApocalypseShield.Result.IGNORED, "a broken shield cannot be worn");
        check(s.extendWindow(100) && s.windowLeft() == 300 && s.isStripped(), "Magneto's strip lengthens the window");
        check(!s.extendWindow(100) && s.windowLeft() == 300, "once");
        boolean ended = false;
        for (int i = 1; i <= 300; i++) {
            boolean over = s.tick(++t);
            check(!ended, "nothing ticks after the window ended");
            if (over) {
                check(i == 300, "the window ends by time at its last tick (" + i + ")");
                ended = true;
            }
        }
        check(ended, "the window ended");

        // Knitting again, stronger or not, and gone for good.
        s.reform(150.0F);
        check(s.state() == ApocalypseShield.State.REFORMING && !s.isVulnerable() && s.strength() == 0.0F, "re-forming: closed again");
        for (int i = 0; i < 20; i++) {
            s.tick(++t);
        }
        assertNear(s.strength(), 75.0, 1.0E-3, "half way after twenty ticks");
        for (int i = 0; i < 20; i++) {
            s.tick(++t);
        }
        check(s.state() == ApocalypseShield.State.UP && s.strength() == 150.0F, "up at 150");
        s.shatter(0);
        check(s.state() == ApocalypseShield.State.GONE && s.isVulnerable(), "the third shield is gone for good: no window to wait for");
        s.raiseNow(40.0F);
        check(s.isUp() && s.strength() == 40.0F && s.max() == 40.0F, "raised at once for a test");
        s.remove();
        check(s.state() == ApocalypseShield.State.GONE && s.strength() == 0.0F, "taken away");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The boss that wears it

    @GameTest(template = ARENA, batch = "apocalypse_deflect", timeoutTicks = 160)
    public static void aShieldThatIsUpDeflectsEverythingElse(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ServerLevel level = d.level();
        ApocalypseEntity boss = d.boss();
        ServerPlayer thor = d.f().player(5.5, FLOOR, 16.5, Power.THOR);
        ServerPlayer magneto = d.f().player(5.5, FLOOR, 18.5, Power.MAGNETO);
        ServerPlayer speedster = d.f().player(7.5, FLOOR, 18.5, Power.QUICKSILVER);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 14.5));
        float health = boss.getHealth();
        float strength = boss.shield().strength();
        Vec3 motion = boss.getDeltaMovement();

        Arrow arrow = EntityType.ARROW.create(level);
        arrow.setOwner(thor);
        List<DamageSource> blows = new ArrayList<>(List.of(
                level.damageSources().arrow(arrow, thor),
                level.damageSources().explosion(null, null),
                level.damageSources().explosion(zombie, thor),
                level.damageSources().lightningBolt(),
                ModDamageTypes.source(level, ModDamageTypes.LIGHTNING_BEAM, thor, thor),
                ModDamageTypes.source(level, ModDamageTypes.MJOLNIR, thor, thor),
                ModDamageTypes.source(level, ModDamageTypes.STORMBREAKER, thor, thor),
                ModDamageTypes.source(level, ModDamageTypes.SHRAPNEL, magneto, magneto),
                ModDamageTypes.source(level, ModDamageTypes.SPEED_BLITZ, speedster, speedster),
                ModDamageTypes.source(level, ModDamageTypes.HEX_BOLT, d.hero(), d.hero()),
                ModDamageTypes.source(level, ModDamageTypes.SAND, zombie, zombie),
                ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, magneto, magneto),
                level.damageSources().onFire(),
                level.damageSources().inFire(),
                level.damageSources().lava(),
                level.damageSources().magic(),
                level.damageSources().generic(),
                level.damageSources().playerAttack(thor),
                level.damageSources().playerAttack(d.hero()),
                level.damageSources().mobAttack(zombie),
                level.damageSources().fall(),
                level.damageSources().wither(),
                level.damageSources().cactus(),
                level.damageSources().drown()));
        for (DamageSource blow : blows) {
            boss.invulnerableTime = 0;
            boolean hurt = boss.hurt(blow, 50.0F);
            String what = blow.getMsgId() + " by " + blow.getEntity();
            check(!hurt, "the shield turns away " + what);
            check(boss.getHealth() == health, "and he loses no health to " + what);
            check(boss.shield().strength() == strength, "nor the shield any strength to " + what);
            check(boss.invulnerableTime == 0 && boss.hurtTime == 0, "no invulnerability frames follow " + what);
            check(boss.getDeltaMovement().equals(motion), "no knockback from " + what);
        }
        check(d.fight().participant(d.hero().getUUID()).counter("shield") == 0.0F, "nothing was credited as shield worn");

        // What flies at him is turned away before it hits: arrows, shards and snowballs turn to dust, Mjolnir goes home.
        Arrow shot = EntityType.ARROW.create(level);
        shot.setOwner(thor);
        shot.pickup = AbstractArrow.Pickup.DISALLOWED;
        Vec3 near = new Vec3(boss.getX() + 4.0, boss.getY() + 1.4, boss.getZ());
        fly(level, shot, near);
        Snowball snowball = new Snowball(level, thor);
        fly(level, snowball, near.add(0.0, 0.0, 0.6));
        IronShrapnelEntity shard = new IronShrapnelEntity(level, magneto);
        shard.configure(6.0F, 1);
        fly(level, shard, near.add(0.0, 0.0, -0.6));
        ThrownWeaponEntity hammer = new ThrownWeaponEntity(level, thor, new ItemStack(ModItems.MJOLNIR.get()));
        fly(level, hammer, near.add(0.0, 0.5, 0.0));

        every(helper, 30, tick -> {
            check(boss.getHealth() == health && boss.shield().strength() == strength, "nothing that flew at him touched him or his shield");
            if (!shot.isRemoved() || !snowball.isRemoved() || !shard.isRemoved()) {
                return false;
            }
            check(hammer.isReturning() && !hammer.isRemoved(), "Mjolnir was turned back to Thor, not destroyed");
            hammer.discard();
            return true;
        });
    }

    /** Sets a projectile flying west from a point at one block a tick, toward the boss, and adds it to the level. */
    private static void fly(ServerLevel level, Projectile projectile, Vec3 from) {
        projectile.setPos(from);
        projectile.setDeltaMovement(-1.0, 0.0, 0.0);
        level.addFreshEntity(projectile);
    }

    /**
     * His line about arrows is for the first arrow too (found in the real game: an arrow turns to dust before it reaches
     * {@code hurt}, so only a later sword blow made him say it): "You can fire your arrows from the Tower of Babel, but you
     * can never strike God!", once.
     */
    @GameTest(template = ARENA, batch = "apocalypse_babel", timeoutTicks = 60)
    public static void theFirstArrowEarnsHisLineAboutBabel(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ServerLevel level = d.level();
        ApocalypseEntity boss = d.boss();
        ApocalypseTestSupport.Log log = new ApocalypseTestSupport.Log(d.hero());
        log.drain();
        check(!log.said("speech.enhanced.apocalypse.babel"), "nothing has touched the shield yet");
        Arrow shot = EntityType.ARROW.create(level);
        shot.setOwner(d.hero());
        shot.pickup = AbstractArrow.Pickup.DISALLOWED;
        fly(level, shot, new Vec3(boss.getX() + 5.0, boss.getY() + 1.4, boss.getZ()));
        float strength = boss.shield().strength();
        every(helper, 40, tick -> {
            if (!shot.isRemoved()) {
                return false;
            }
            check(boss.shield().strength() == strength, "the arrow wore nothing");
            check(log.drain().said("speech.enhanced.apocalypse.babel"), "the first arrow turned away makes him speak of Babel");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_beam", timeoutTicks = 120)
    public static void chaosBeamWearsTheShieldNotTheHealth(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ApocalypseEntity boss = d.boss();
        float health = boss.getHealth();
        float max = boss.shield().strength();
        AbilityHandler.onKey(d.hero(), 1, true);
        every(helper, 45, tick -> {
            lookAt(d.hero(), boss);
            PowerTicker.tick(d.hero());
            check(boss.getHealth() == health, "the beam never touches his health (tick " + tick + ")");
            // The shield keeps its own cooldown: five a hit, one hit in ten ticks, whatever the beam does.
            if (tick == 10 || tick == 20 || tick == 30 || tick == 40) {
                assertNear(max - boss.shield().strength(), 5.0 * (tick / 10), 1.0E-4, "five off the shield per ten ticks at tick " + tick);
            }
            if (tick == 40) {
                assertNear(d.fight().participant(d.hero().getUUID()).counter("shield"), 20.0, 1.0E-4, "and the beam's fighter is credited with the 20");
                check(boss.shield().isUp(), "still up");
                // The last point and more: it breaks, and what was left over is not taken from him.
                boss.shield().raiseNow(4.0F);
            }
            if (tick == 41) {
                check(boss.shield().state() == ApocalypseShield.State.BROKEN, "a hit of 5 against 4 breaks it");
                check(boss.getHealth() == health, "the excess is thrown away");
                check(boss.shield().windowLeft() > 150, "and a window of 200 opens");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_burst", timeoutTicks = 80)
    public static void phoenixBurstBreaksShieldOneInOneGo(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.PHOENIX);
        ApocalypseEntity boss = d.boss();
        float health = boss.getHealth();
        check(d.hit(ModDamageTypes.PHOENIX_BURST, 8.0F), "a Burst of 8 counts");
        assertNear(boss.shield().strength(), 110.0, 1.0E-3, "it removes 40 of the 150");
        check(boss.getHealth() == health && boss.shield().isUp(), "and nothing of his health");
        helper.runAfterDelay(12, () -> {
            check(d.hit(ModDamageTypes.PHOENIX_BURST, 30.0F), "a Burst of 30");
            check(boss.shield().state() == ApocalypseShield.State.BROKEN, "removes 150 and breaks even the whole of shield 1");
            check(boss.getHealth() == health, "with the excess thrown away");
            assertNear(d.fight().participant(d.hero().getUUID()).counter("shield"), 40.0 + 110.0, 1.0E-3,
                    "the Phoenix is credited with what the shield lost, no more");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_slam", timeoutTicks = 100)
    public static void telekineticThrowsWearTheShield(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ApocalypseEntity boss = d.boss();
        ServerPlayer magneto = d.f().player(5.5, FLOOR, 17.5, Power.MAGNETO);
        Zombie golem = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 14.5));
        float health = boss.getHealth();
        float max = boss.shield().strength();

        // A Magneto's slam is not mind power. (Nor is anyone's but a breaker's.)
        boss.invulnerableTime = 0;
        check(!boss.hurt(ModDamageTypes.source(d.level(), ModDamageTypes.TELEKINETIC_SLAM, magneto, golem), 9.0F), "the slam of a golem Magneto threw is turned away");
        check(boss.shield().strength() == max && boss.getHealth() == health, "and does nothing");

        // Wanda's wears it: the whole chain. She lifts a Zombie (not a block: nothing in the arena can be torn out of the
        // ground), throws it, and it is the shield that takes the blow, not his health.
        Zombie thrown = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(6.5, FLOOR, 10.5));
        lookAt(d.hero(), thrown);
        AbilityHandler.onKey(d.hero(), 3, true);
        check(Telekinesis.isHolding(d.hero()), "she holds the Zombie");
        float before = boss.shield().strength();
        every(helper, 60, tick -> {
            if (tick < 6) {
                TestKit.look(d.hero(), TestKit.EAST, -12.0F);
            }
            PowerTicker.tick(d.hero());
            if (tick == 6) {
                TestKit.look(d.hero(), TestKit.EAST, 0.0F);
                AbilityHandler.onKey(d.hero(), 3, true);
                check(!Telekinesis.isHolding(d.hero()), "she threw it");
            }
            if (tick > 6 && boss.shield().strength() < before) {
                check(before - boss.shield().strength() >= 3.0F, "the Zombie wore the shield by what it hit with: " + (before - boss.shield().strength()));
                check(boss.getHealth() == health, "and left him alone");
                return true;
            }
            check(tick < 58, "the Zombie never wore the shield: it is at " + rel(d.f(), thrown) + " (alive " + thrown.isAlive() + ") with speed "
                    + thrown.getDeltaMovement() + ", the boss at " + rel(d.f(), boss) + ", the shield " + boss.shield().strength() + "/" + max);
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_phoenix_touch", timeoutTicks = 200)
    public static void phoenixTouchesStrainTheShieldAndInterruptTheMindCrush(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.PHOENIX);
        ApocalypseEntity boss = d.boss();
        boss.shield().raiseNow(400.0F);
        PhoenixPower.touched(d.hero(), boss, PhoenixPower.Kind.BURST, 10.0F);
        PhoenixPower.touched(d.hero(), boss, PhoenixPower.Kind.SLAM, 10.0F);
        PhoenixPower.touched(d.hero(), boss, PhoenixPower.Kind.MIND_READ, 0.0F);
        check(boss.shield().strength() == 400.0F, "a Burst, a slam and a reading are not a strain: damage reaches the shield through hurt");
        PhoenixPower.touched(d.hero(), boss, PhoenixPower.Kind.MIND_CONTROL, 0.0F);
        assertNear(boss.shield().strength(), 375.0, 1.0E-4, "a Mind Control, even a refused one, takes 25");
        PhoenixPower.touched(d.hero(), boss, PhoenixPower.Kind.PSIONIC_FIELD, 0.0F);
        assertNear(boss.shield().strength(), 315.0, 1.0E-4, "a Psionic Field that reaches him takes 60");
        assertNear(d.fight().participant(d.hero().getUUID()).counter("shield"), 85.0, 1.0E-4, "and she is credited with the 85");
        check(boss.getHealth() == boss.designMax(), "none of it touches his health");
        PhoenixPower.touched(d.hero(), d.f().player(2.5, FLOOR, 2.5), PhoenixPower.Kind.MIND_CONTROL, 0.0F);
        assertNear(boss.shield().strength(), 315.0, 1.0E-4, "a touch on someone else is not his business");

        // The Mind Crush in its tell: either touch breaks the ring. (The fight has to have looked round once to know its foes.)
        d.hero().removeAllEffects();
        helper.runAfterDelay(2, () -> {
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.MIND_CRUSH) && boss.state() == ApocalypseState.TELL_MIND_CRUSH, "he begins the Mind Crush");
            helper.runAfterDelay(10, () -> {
                check(boss.brain().attack() == ApocalypseAttack.MIND_CRUSH, "mid-tell");
                PhoenixPower.touched(d.hero(), boss, PhoenixPower.Kind.MIND_CONTROL, 0.0F);
                helper.runAfterDelay(3, () -> {
                    check(boss.brain().attack() == null && boss.state() == ApocalypseState.IDLE, "a Mind Control interrupted it");
                    check(boss.brain().force(boss, d.fight(), ApocalypseAttack.MIND_CRUSH), "he tries again");
                    helper.runAfterDelay(10, () -> {
                        PhoenixPower.touched(d.hero(), boss, PhoenixPower.Kind.PSIONIC_FIELD, 0.0F);
                        helper.runAfterDelay(3, () -> {
                            check(boss.brain().attack() == null, "a Psionic Field interrupted that one");
                            check(!d.hero().hasEffect(MobEffects.CONFUSION) && !d.hero().hasEffect(MobEffects.BLINDNESS), "nobody felt either");
                            // A hero whose mind he cannot read leaves it to run its course.
                            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.MIND_CRUSH), "a third time");
                            helper.runAfterDelay(ApocalypseAttack.MIND_CRUSH.tellTicks() + 4, () -> {
                                check(d.hero().hasEffect(MobEffects.CONFUSION), "left alone, it lands");
                                helper.succeed();
                            });
                        });
                    });
                });
            });
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_phoenix_not_breaker", timeoutTicks = 60)
    public static void aPhoenixTheServerDoesNotListStrainsNothingButStillInterrupts(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.PHOENIX);
        d.f().config(ApocalypseShieldConfig.MIND_POWER_HEROES, List.of("scarlet_witch"));
        ApocalypseEntity boss = d.boss();
        float strength = boss.shield().strength();
        PhoenixPower.touched(d.hero(), boss, PhoenixPower.Kind.PSIONIC_FIELD, 0.0F);
        check(boss.shield().strength() == strength, "a mind the server does not count does not strain the shield");
        check(!d.hit(ModDamageTypes.PHOENIX_BURST, 30.0F) && boss.shield().strength() == strength, "and her Burst is turned away");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_window", timeoutTicks = 200)
    public static void aBrokenShieldIsAWindowWithItsOwnNumbers(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        boss.brain().setChoosing(true);
        boss.brain().clearCooldowns();
        breakShield(d);
        check(boss.shield().windowLeft() == ApocalypseBossConfig.WINDOW_TICKS.get(), "the window is windowTicks long");
        check(d.fight().staggerLeft() == ApocalypseBossConfig.STAGGER_TICKS.get() && boss.state() == ApocalypseState.STAGGERED, "he kneels, staggered");
        check(!d.fight().inWindow() && !d.fight().bossInvulnerable(), "open to blows, though he attacks nothing yet");
        check(boss.floorFraction() > 0.66 && boss.floorFraction() < 0.67, "the floor of the window is two thirds");

        // The numbers of a window, with his armour out of the way: a quarter more, a fraction at most, half of a blast.
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(18.5, FLOOR, 18.5));
        DamageSource blow = level.damageSources().mobAttack(zombie);
        boss.setArmourCut(true);
        float h0 = boss.getHealth();
        boss.invulnerableTime = 0;
        check(boss.hurt(blow, 8.0F), "an ordinary blow hurts in a window");
        assertNear(h0 - boss.getHealth(), 10.0, 1.0E-3, "a quarter more: 8 becomes 10");
        float h1 = boss.getHealth();
        boss.invulnerableTime = 0;
        boss.hurt(blow, 500.0F);
        assertNear(h1 - boss.getHealth(), 0.12 * boss.designMax(), 1.0E-3, "no blow takes more than 12 percent of his health");
        float h2 = boss.getHealth();
        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().explosion(null, null), 20.0F);
        assertNear(h2 - boss.getHealth(), 20.0 * 0.5 * 1.25, 1.0E-3, "a blast is halved, then the window's quarter");
        float h3 = boss.getHealth();
        boss.invulnerableTime = 0;
        check(!boss.hurt(level.damageSources().onFire(), 20.0F) && !boss.hurt(level.damageSources().drown(), 20.0F) && boss.getHealth() == h3,
                "fire and water still do nothing to him");
        boss.setArmourCut(false);

        // His armour counts again once it is back, and the quarter comes after it.
        float h4 = boss.getHealth();
        boss.invulnerableTime = 0;
        boss.hurt(blow, 8.0F);
        double armour = ApocalypseBossConfig.ARMOR.get();
        double toughness = ApocalypseBossConfig.ARMOR_TOUGHNESS.get();
        double effective = Math.min(20.0, Math.max(armour - 8.0 / (2.0 + toughness / 4.0), armour * 0.2));
        assertNear(h4 - boss.getHealth(), 8.0 * (1.0 - effective / 25.0) * 1.25, 1.0E-3, "with his armour on, 8 is what the armour lets through, a quarter more");

        // No attacks for the stagger; then the Lances come first.
        every(helper, 100, tick -> {
            if (tick <= ApocalypseBossConfig.STAGGER_TICKS.get() - 5) {
                check(!boss.brain().isBusy() && boss.state() == ApocalypseState.STAGGERED, "he attacks nothing while he kneels (tick " + tick + ")");
                check(level.getEntitiesOfClass(SandLanceEntity.class, boss.getBoundingBox().inflate(40.0)).isEmpty(), "no lance flies (tick " + tick + ")");
            }
            if (tick > ApocalypseBossConfig.STAGGER_TICKS.get() + 1 && boss.brain().isBusy()) {
                check(d.fight().staggerLeft() == 0 && d.fight().inWindow(), "the stagger is over and the window is his to be struck in");
                check(boss.brain().attack() == ApocalypseAttack.LANCES, "the first attack after a stagger is the Lances: " + boss.brain().attack());
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_floors", timeoutTicks = 200)
    public static void floorsStopTheHealthAndStartTheTransition(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ApocalypseEntity boss = d.boss();
        breakShield(d);
        boss.setArmourCut(true);
        for (int i = 0; i < 8; i++) {
            boss.invulnerableTime = 0;
            boss.hurt(d.level().damageSources().generic(), 500.0F);
        }
        float floor1 = (float) Math.ceil(900.0 * 2.0 / 3.0);
        assertNear(boss.getHealth(), floor1, 1.0E-3, "in window 1 no blow brings him below two thirds: " + boss.getHealth());
        check(boss.isAlive(), "and he lives");
        every(helper, 10, tick -> {
            if (d.fight().stage() != ApocalypseFight.Stage.TRANSITION_1) {
                return false;
            }
            check(boss.shield().state() == ApocalypseShield.State.GONE && d.fight().bossInvulnerable(), "the first transition begins: invulnerable");
            boss.invulnerableTime = 0;
            check(!boss.hurt(d.level().damageSources().generic(), 50.0F) && boss.getHealth() == floor1, "nothing hurts him in it");
            check(boss.state() == ApocalypseState.HOVER, "he hovers");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_floors2", timeoutTicks = 200)
    public static void theSecondWindowEndsAtOneThird(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.SCARLET_WITCH);
        ApocalypseEntity boss = d.boss();
        assertNear(boss.getHealth(), Math.ceil(900.0 * 2.0 / 3.0), 1.0E-3, "phase 2 begins at two thirds");
        assertNear(d.boss().shield().strength(), ApocalypseShield.strengthFor(2, 1), 1.0E-3, "behind a stronger shield");
        breakShield(d);
        boss.setArmourCut(true);
        for (int i = 0; i < 8; i++) {
            boss.invulnerableTime = 0;
            boss.hurt(d.level().damageSources().generic(), 500.0F);
        }
        float floor2 = (float) Math.ceil(900.0 / 3.0);
        assertNear(boss.getHealth(), floor2, 1.0E-3, "in window 2 no blow brings him below a third: " + boss.getHealth());
        every(helper, 10, tick -> {
            if (d.fight().stage() != ApocalypseFight.Stage.TRANSITION_2) {
                return false;
            }
            check(d.fight().bossInvulnerable() && boss.shield().state() == ApocalypseShield.State.GONE, "the second transition begins");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_reform", timeoutTicks = 260)
    public static void aFailedWindowReformsAtFullStrength(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        d.f().config(ApocalypseBossConfig.WINDOW_TICKS, 60);
        d.f().config(ApocalypseBossConfig.STAGGER_TICKS, 20);
        ApocalypseEntity boss = d.boss();
        float max = boss.shield().strength();
        breakShield(d);
        boss.setArmourCut(true);
        check(boss.shield().windowLeft() == 60, "a window of 60");
        every(helper, 200, tick -> {
            if (tick == 30) {
                check(boss.shield().state() == ApocalypseShield.State.BROKEN, "still open at tick 30");
            }
            if (tick == 70) {
                check(boss.shield().state() == ApocalypseShield.State.REFORMING, "the window ran out and the shield knits again: " + boss.shield().state());
                check(!boss.isArmourCut() && boss.floorFraction() == 0.0, "his armour is back and the floor is gone");
                check(boss.shield().strength() < max, "it is not whole yet");
                check(d.fight().stage() == ApocalypseFight.Stage.FIGHT, "he is still in the same phase");
                check(!d.fight().bossInvulnerable() && !boss.hurt(d.level().damageSources().generic(), 5.0F), "a shield that knits stops everything, though mind power cannot wear it yet");
            }
            if (tick == 110) {
                check(boss.shield().state() == ApocalypseShield.State.UP && boss.shield().strength() == max, "forty ticks later it is up at full strength");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_heal", timeoutTicks = 200)
    public static void heHealsWhileAShieldIsUpNeverAboveTheCeilingOfThePhase(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.SCARLET_WITCH);
        d.f().config(ApocalypseBossConfig.REGEN_PER_SECOND, 4.0); // 3.0's healing: since 3.1 his health never heals
        ApocalypseEntity boss = d.boss();
        // Phase 2's ceiling is two thirds: a little under it he mends 4 a second and stops there.
        float ceiling = (float) Math.ceil(900.0 * 2.0 / 3.0);
        boss.setHealth(ceiling - 6.0F);
        every(helper, 100, tick -> {
            if (tick == 1) {
                check(boss.getHealth() <= ceiling - 6.0F + 4.0F, "not instantly");
            }
            if (tick < 100) {
                check(boss.getHealth() <= ceiling + 1.0E-3F, "never above the ceiling of the phase (tick " + tick + "): " + boss.getHealth());
                return false;
            }
            assertNear(boss.getHealth(), ceiling, 1.0E-3, "he mended to it");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_heal1", timeoutTicks = 100)
    public static void heMendsFourASecondBehindShieldOne(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        d.f().config(ApocalypseBossConfig.REGEN_PER_SECOND, 4.0); // 3.0's healing: since 3.1 his health never heals
        ApocalypseEntity boss = d.boss();
        boss.setFraction(0.5);
        float start = boss.getHealth();
        every(helper, 60, tick -> {
            if (tick < 60) {
                return false;
            }
            assertNear(boss.getHealth() - start, 12.0, 1.0E-3, "sixty ticks, three seconds, 4 a second");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_immune", timeoutTicks = 60)
    public static void bossIsImmuneToEffectsAndHasEveryTag(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ApocalypseEntity boss = d.boss();
        for (var effect : List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.POISON, MobEffects.WITHER, MobEffects.CONFUSION,
                MobEffects.HARM, MobEffects.BLINDNESS, MobEffects.DIG_SLOWDOWN, MobEffects.LEVITATION, MobEffects.REGENERATION)) {
            boss.addEffect(new MobEffectInstance(effect, 200, 1));
            check(!boss.hasEffect(effect), effect.getRegisteredName() + " is refused");
        }
        boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        check(boss.hasEffect(MobEffects.GLOWING), "Glowing is allowed (the spyglass finds him)");
        var type = boss.getType();
        check(type.is(ModTags.HEX_RESISTANT), "hex_resistant");
        check(type.is(ModTags.PIN_IMMUNE), "pin_immune");
        check(type.is(ModTags.SPELLBOUND_IMMUNE), "spellbound_immune");
        check(type.is(ModTags.CONFUSED_IMMUNE), "confused_immune");
        check(type.is(ModTags.SHAPESHIFT_IMMUNE), "shapeshift_immune");
        check(type.is(ModTags.PULL_IMMUNE) && !type.is(ModTags.TELEKINESIS_IMMUNE), "pull_immune, and not telekinesis_immune (held as a boss)");
        check(type.is(ModTags.TELEPATHY_IMMUNE), "telepathy_immune");
        check(type.is(ModTags.BOSSES), "bosses");
        check(type.is(ModTags.ENTOMB_IMMUNE), "entomb_immune");

        // Wanda is mind power: her telekinesis takes hold of him through the shield (3.1), and lets go.
        check(boss.shield().isUp(), "his shield is up");
        lookAt(d.hero(), boss);
        AbilityHandler.onKey(d.hero(), 3, true);
        AbilityHandler.onKey(d.hero(), 3, false);
        check(Telekinesis.holds(d.hero(), boss), "Wanda's telekinesis takes hold of him through his shield");
        Telekinesis.release(d.hero(), false);
        check(!Telekinesis.isHeld(boss), "and lets go");
        helper.succeed();
    }

    private ApocalypseShieldTests() {
    }
}

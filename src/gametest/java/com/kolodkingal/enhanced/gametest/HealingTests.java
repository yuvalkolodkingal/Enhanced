package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.HealingConfig;
import com.kolodkingal.enhanced.healing.Downed;
import com.kolodkingal.enhanced.healing.HealingBurst;
import com.kolodkingal.enhanced.healing.HealingEvents;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.healing.HealingHooks;
import com.kolodkingal.enhanced.healing.HealingState;
import com.kolodkingal.enhanced.healing.Regeneration;
import com.kolodkingal.enhanced.healing.Rescue;
import com.kolodkingal.enhanced.healing.Suppression;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.vormir.VormirTracker;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The healing factor (3.7, lane L1; the plan's 2 and 13): Wolverine, Deadpool and a custom hero with Wolverine's body
 * row. A monster's blow never kills a healer: he goes DOWN at 1 health (the monster forgets him, he is untouchable and
 * still), and rises 100 ticks later with 8 health and Resistance; only the Infinity Stones, chaos magic, the Allfather's
 * power and the Phoenix get through (the Darkhold not by default), and the void and {@code /kill} always; a Totem of
 * Undying is never used by a blow he survives. Poison switches it off (no regeneration, a zombie kills him) and milk
 * turns it back. The regeneration (1.0/s, 3.0/s after 60 ticks out of harm, food paid, a quarter when starving), the
 * shrug, the Healing Burst; the rescue out of deep water and lava (or to the respawn point); the Road's rule first; a
 * boss fight is not wiped while a healer is DOWN; Vormir (pushed: the sacrifice takes him; alone: DOWN); the tip once,
 * the line of a defeat, the advancement, the hooks. Batches: {@code healing}, {@code healing_pvp} (PvP on), {@code
 * healing_boss} (a fight), and one per changed config ({@code healing_cfg*}).
 */
public final class HealingTests {
    private static final String BATCH = "healing";
    private static final String PVP = "healing_pvp";
    private static final String CFG = "healing_cfg";

    /** What the hooks heard, per player: "down", "rise", "rise:rescued". */
    private static final Map<UUID, List<String>> HEARD = new ConcurrentHashMap<>();

    static {
        HealingHooks.register(new HealingHooks.Listener() {
            @Override
            public void down(ServerPlayer player) {
                HEARD.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add("down");
            }

            @Override
            public void rise(ServerPlayer player, boolean rescued) {
                HEARD.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(rescued ? "rise:rescued" : "rise");
            }
        });
    }

    private HealingTests() {
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A healer of {@code power} at full health. */
    static ServerPlayer healer(TestKit.Fixture f, double x, double z, Power power) {
        ServerPlayer player = f.player(x, FLOOR, z, power);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    /** A custom hero with Wolverine's body row (and so the healing factor). */
    static ServerPlayer customHealer(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = CustomKitSupport.hero(f, x, z, CustomKitSupport.def().pick("wolverine.body", 5).build());
        player.setHealth(player.getMaxHealth());
        return player;
    }

    static DamageSource source(GameTestHelper helper, ResourceKey<DamageType> type) {
        return ModDamageTypes.source(helper.getLevel(), type);
    }

    /** A blow of {@code amount} from {@code source} (his short invulnerability after the last blow forgotten). */
    static boolean blow(ServerPlayer victim, DamageSource source, float amount) {
        victim.invulnerableTime = 0;
        return victim.hurt(source, amount);
    }

    /** A zombie's blow that would kill anyone. */
    static boolean zombieBlow(ServerPlayer victim, Zombie zombie) {
        return blow(victim, victim.damageSources().mobAttack(zombie), 1000.0F);
    }

    static Zombie zombie(GameTestHelper helper, double x, double z) {
        Zombie zombie = TestKit.spawn(helper, EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // (no burning in the sun)
        return zombie;
    }

    /**
     * Ticks the healers' factor once a server tick for {@code n} ticks ({@link HealingEvents#tick}: nothing ticks a
     * test player), then runs {@code then}; {@code full}: the whole player tick ({@code doTick}: the fluids, the fire).
     */
    static void ticks(GameTestHelper helper, int n, boolean full, Runnable then, ServerPlayer... players) {
        if (n <= 0) {
            then.run();
            return;
        }
        helper.runAfterDelay(1, () -> {
            for (ServerPlayer player : players) {
                if (player.isAlive()) {
                    if (full) {
                        player.doTick();
                    } else {
                        HealingEvents.tick(player);
                    }
                }
            }
            ticks(helper, n - 1, full, then, players);
        });
    }

    static boolean down(ServerPlayer player) {
        return HealingFactor.isDown(player) && Downed.downUntil(player) > 0L;
    }

    /** The chat lines (lang keys) the player was sent since the last look (not the action bar). */
    static List<String> chat(ServerPlayer player) {
        List<String> keys = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundSystemChatPacket line && !line.overlay()
                    && line.content().getContents() instanceof TranslatableContents said) {
                keys.add(said.getKey());
            }
        }
        return keys;
    }

    static List<String> heard(ServerPlayer player) {
        return HEARD.getOrDefault(player.getUUID(), List.of());
    }

    // ------------------------------------------------------------------------------------------------------------
    // DOWN and the rise

    /**
     * Wolverine, Deadpool and a custom hero with Wolverine's body: a zombie's blow that would kill leaves each at 1
     * health and DOWN (the bit everyone sees), the zombie forgets him and cannot take him again; still (no speed, no
     * jump); after 100 ticks he rises with 8 health and Resistance, the hooks heard both, "Back on Your Feet" is his.
     */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void aZombiesBlowNeverKillsAHealer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 4.5, 4.5, Power.WOLVERINE);
        ServerPlayer wade = healer(f, 10.5, 4.5, Power.DEADPOOL);
        ServerPlayer custom = customHealer(f, 16.5, 4.5);
        ServerPlayer[] all = {logan, wade, custom};
        Zombie zombie = zombie(helper, 10.5, 9.5);
        zombie.setTarget(wade);
        long start = helper.getLevel().getGameTime();
        for (ServerPlayer healer : all) {
            check(HealingFactor.has(healer), healer.getGameProfile().getName() + " is a healer");
            zombieBlow(healer, zombie);
            check(healer.isAlive() && healer.getHealth() == 1.0F, "the blow leaves him at 1 health: "
                    + healer.getHealth());
            check(down(healer) && Powers.get(healer).is(Live.HEALING_DOWN), "and DOWN");
            check(healer.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.0
                    && healer.getAttributeValue(Attributes.JUMP_STRENGTH) == 0.0, "he cannot walk or jump");
            check(heard(healer).contains("down"), "the hooks hear him go down");
        }
        check(zombie.getTarget() == null, "the zombie forgets him");
        zombie.setTarget(wade);
        check(zombie.getTarget() == null, "and cannot take him again while he is DOWN");
        zombie.discard(); // (so no bite lands on the healers as they rise)
        ticks(helper, Downed.downTicks() - 2, false, () -> {
            check(down(logan), "still DOWN two ticks before his time");
            ticks(helper, 3, false, () -> {
                for (ServerPlayer healer : all) {
                    check(!down(healer) && !Powers.get(healer).is(Live.HEALING_DOWN), "he rose at his time");
                    check(healer.getHealth() >= 8.0F && healer.getHealth() <= 9.5F, "with 8 health (and a step"
                            + " of healing at most): " + healer.getHealth());
                    check(healer.hasEffect(MobEffects.DAMAGE_RESISTANCE), "and a little Resistance");
                    check(healer.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0, "he can walk again");
                    check(heard(healer).contains("rise"), "the hooks hear him rise");
                    AdvancementHolder back = healer.server.getAdvancements().get(Enhanced.id("mutants/healing_factor"));
                    check(back != null && healer.getAdvancements().getOrStartProgress(back).isDone(),
                            "\"Back on Your Feet\" is his");
                }
                check(helper.getLevel().getGameTime() - start >= Downed.downTicks(), "after downTicks");
                helper.succeed();
            }, all);
        }, all);
    }

    /** While DOWN he is untouchable: an arrow and a zombie's bite do nothing; a chaos beam kills him. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void whileDownOnlyAKillerReachesHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        Zombie zombie = zombie(helper, 9.5, 6.5);
        zombieBlow(logan, zombie);
        check(down(logan), "DOWN");
        check(!blow(logan, ModDamageTypes.source(helper.getLevel(), DamageTypes.ARROW, zombie, null), 6.0F)
                && logan.getHealth() == 1.0F, "an arrow does nothing");
        check(!zombieBlow(logan, zombie) && logan.getHealth() == 1.0F, "nor a bite");
        check(!blow(logan, helper.getLevel().damageSources().fall(), 50.0F), "nor a fall");
        blow(logan, source(helper, ModDamageTypes.CHAOS_BEAM), 2.0F);
        check(logan.isDeadOrDying(), "chaos magic does: he is defeated");
        helper.succeed();
    }

    /** The Infinity Stones, chaos magic, the Allfather's power and the Phoenix defeat a healer; the Darkhold does not. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onlyTheTopPowersDefeatHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        List<ResourceKey<DamageType>> killers = List.of(ModDamageTypes.POWER_STONE_BEAM, ModDamageTypes.CHAOS_BEAM,
                ModDamageTypes.ODINFORCE, ModDamageTypes.PHOENIX_BURST);
        for (int i = 0; i < killers.size(); i++) {
            ServerPlayer healer = healer(f, 3.5 + 4 * i, 4.5, i % 2 == 0 ? Power.WOLVERINE : Power.DEADPOOL);
            blow(healer, source(helper, killers.get(i)), 1000.0F);
            check(healer.isDeadOrDying(), killers.get(i).location() + " defeats him");
        }
        ServerPlayer logan = healer(f, 10.5, 12.5, Power.WOLVERINE);
        blow(logan, source(helper, ModDamageTypes.DARKHOLD_BEAM), 1000.0F);
        check(logan.isAlive() && down(logan), "the Darkhold's beam does not (by default): he goes DOWN");
        helper.succeed();
    }

    /** The void and /kill are never a trap: they always kill. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theVoidAndKillAlwaysKill(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 4.5, 4.5, Power.WOLVERINE);
        ServerPlayer wade = healer(f, 12.5, 4.5, Power.DEADPOOL);
        logan.kill();
        check(logan.isDeadOrDying(), "/kill kills him");
        blow(wade, helper.getLevel().damageSources().fellOutOfWorld(), 1000.0F);
        check(wade.isDeadOrDying(), "the void kills him");
        helper.succeed();
    }

    /** A Totem of Undying is never used by a blow he survives (the death never happens). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aTotemIsNotUsedUp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        logan.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        zombieBlow(logan, zombie(helper, 9.5, 6.5));
        check(down(logan) && logan.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "DOWN, and the totem is still his");
        helper.succeed();
    }

    /**
     * The net for code that kills without a blow: a death with a plain source is refused (1 health, DOWN); a death by
     * chaos goes through. Absorption counts: a blow absorbed in part leaves exactly 1 health.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aDeathWithoutABlowIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 4.5, 4.5, Power.WOLVERINE);
        logan.setHealth(0.0F);
        logan.die(helper.getLevel().damageSources().generic());
        check(logan.isAlive() && logan.getHealth() == 1.0F && down(logan), "refused: 1 health and DOWN");
        ServerPlayer wade = healer(f, 10.5, 4.5, Power.DEADPOOL);
        wade.setHealth(6.0F);
        wade.setAbsorptionAmount(4.0F);
        blow(wade, helper.getLevel().damageSources().generic(), 30.0F);
        check(wade.getHealth() == 1.0F && wade.getAbsorptionAmount() == 0.0F && down(wade),
                "absorption spent, then cut to 1: " + wade.getHealth());
        ServerPlayer custom = customHealer(f, 16.5, 4.5);
        custom.setHealth(0.0F);
        custom.die(source(helper, ModDamageTypes.CHAOS_BEAM));
        check(!custom.isAlive() || custom.isDeadOrDying(), "a death by chaos goes through");
        helper.succeed();
    }

    /** A DOWN healer uses no item; logging out while DOWN, he logs in risen (8 health). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void downHeUsesNothingAndLogsInRisen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        logan.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        zombieBlow(logan, zombie(helper, 9.5, 6.5));
        PlayerInteractEvent.RightClickItem use = new PlayerInteractEvent.RightClickItem(logan, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(use);
        check(use.isCanceled(), "his items are refused");
        Downed.login(logan);
        check(!down(logan) && !Powers.get(logan).is(Live.HEALING_DOWN) && logan.getHealth() >= 8.0F
                && logan.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0, "logged in risen");
        helper.succeed();
    }

    /** The tip of his first DOWN is told once; the line of a defeat is heard by those near. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theTipOnceAndTheLineOfADefeat(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        ServerPlayer friend = f.player(12.5, FLOOR, 6.5);
        chat(logan);
        zombieBlow(logan, zombie(helper, 9.5, 9.5));
        check(chat(logan).contains("message.enhanced.healing.tip"), "the tip at his first DOWN");
        check(ModAttachments.HEALING.get(logan).has(HealingState.TIP_SHOWN), "remembered");
        Downed.rise(logan);
        zombieBlow(logan, zombie(helper, 9.5, 9.5));
        check(down(logan) && !chat(logan).contains("message.enhanced.healing.tip"), "not at the second");
        chat(friend);
        blow(logan, source(helper, ModDamageTypes.ODINFORCE), 1000.0F);
        check(logan.isDeadOrDying(), "the Odinforce defeats him");
        check(chat(friend).contains("message.enhanced.healing.defeated.odin"),
                "and his friend near hears: \"...could not stop the Allfather's power.\"");
        helper.succeed();
    }

    /** The Witches' Road's own rule goes first: in a trial he is put back whole, never DOWN. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRoadsRuleWins(GameTestHelper helper) throws ReflectiveOperationException {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        Field field = RoadTrials.class.getDeclaredField("ACTIVE");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> active = (Map<UUID, Object>) field.get(null);
        active.put(logan.getUUID(), null); // (in a trial for this tick only: no Road tick runs between)
        try {
            check(RoadTrials.isInside(logan), "he is in a trial");
            check(!zombieBlow(logan, zombie(helper, 9.5, 6.5)), "the Road turns the blow away");
            logan.setHealth(0.0F);
            logan.die(helper.getLevel().damageSources().generic());
            check(logan.isAlive() && logan.getHealth() == logan.getMaxHealth() && !down(logan),
                    "the Road saves him whole, not the healing factor");
        } finally {
            active.remove(logan.getUUID());
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Poison

    /**
     * Poison switches the healing off: the HUD's OFF (told once), no regeneration, no Burst, and a zombie kills him
     * ("...could not stop the poison"); milk turns it back on at once.
     */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void poisonSwitchesItOffAndMilkBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 4.5, 4.5, Power.WOLVERINE);
        ServerPlayer wade = healer(f, 12.5, 4.5, Power.DEADPOOL);
        logan.setHealth(10.0F);
        logan.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
        check(!HealingFactor.active(logan) && HealingFactor.has(logan), "off, though he has it");
        check(!HealingBurst.burst(logan, 0.5) && logan.getHealth() == 10.0F, "the Burst is refused");
        long now = helper.getLevel().getGameTime();
        check(Regeneration.rate(logan, now + 1000) == 0.0, "no regeneration");
        wade.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
        HealingEvents.tick(wade);
        check(Suppression.toldOff(wade), "he is told it is off");
        chat(wade);
        blow(wade, wade.damageSources().mobAttack(zombie(helper, 12.5, 8.5)), 1000.0F);
        check(wade.isDeadOrDying(), "a zombie kills a poisoned healer");
        ticks(helper, 40, false, () -> {
            check(logan.getHealth() <= 10.0F, "nothing healed while poisoned: " + logan.getHealth());
            logan.removeEffectsCuredBy(EffectCures.MILK);
            check(HealingFactor.active(logan), "milk turns it back on");
            HealingEvents.tick(logan);
            check(!Suppression.toldOff(logan), "and he is told");
            zombieBlow(logan, zombie(helper, 4.5, 8.5));
            check(down(logan), "a zombie's blow takes him DOWN again, no further");
            helper.succeed();
        }, logan);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Regeneration, the shrug, the Burst

    /**
     * 1.0 health a second after a blow, 3.0 once he has had 60 ticks out of harm (to the tick); food pays 0.05 for each
     * health; twice as fast in Wolverine's Berserker Rage; at food 0 a quarter.
     */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 160)
    public static void regenerationOneThenThree(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        logan.setHealth(4.0F);
        blow(logan, helper.getLevel().damageSources().generic(), 1.0F);
        long hurt = helper.getLevel().getGameTime();
        float h0 = logan.getHealth();
        float e0 = logan.getFoodData().getExhaustionLevel();
        check(Regeneration.rate(logan, hurt + 59) == 1.0 && Regeneration.rate(logan, hurt + 60) == 3.0,
                "1.0/s until 60 ticks out of harm, 3.0/s from the 60th");
        ticks(helper, 40, false, () -> {
            assertNear(logan.getHealth() - h0, 2.0, 1.0E-4, "1.0 a second for 40 ticks");
            assertNear(logan.getFoodData().getExhaustionLevel() - e0, 2.0 * 0.05, 1.0E-4, "paid with food");
            ticks(helper, 19, false, () -> {
                float h59 = logan.getHealth();
                ticks(helper, 40, false, () -> {
                    assertNear(logan.getHealth() - h59, 6.0, 1.0E-4, "3.0 a second out of harm");
                    long now = helper.getLevel().getGameTime();
                    double full = Regeneration.rate(logan, now);
                    Powers.get(logan).set(Live.WOLVERINE_BERSERK, true);
                    assertNear(Regeneration.rate(logan, now), full * 2.0, 1.0E-9, "twice in the Berserker Rage");
                    Powers.get(logan).set(Live.WOLVERINE_BERSERK, false);
                    logan.getFoodData().setFoodLevel(0);
                    assertNear(Regeneration.rate(logan, now), full * Regeneration.STARVING, 1.0E-9,
                            "a quarter when starving, never nothing");
                    helper.succeed();
                }, logan);
            }, logan);
        }, logan);
    }

    /** The shrug: Slowness lasts half as long; Poison and Spellbound do not; nothing is shrugged while poisoned. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theShrugHalvesTheGamesHarm(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        logan.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
        logan.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 200, 0));
        check(logan.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() == 100, "Slowness halved");
        check(logan.getEffect(ModEffects.SPELLBOUND).getDuration() == 200, "a spell of the mod is not shrugged");
        logan.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        check(logan.getEffect(MobEffects.POISON).getDuration() == 200, "Poison is not shrugged");
        logan.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        check(logan.getEffect(MobEffects.WEAKNESS).getDuration() == 200, "and while poisoned nothing is");
        ServerPlayer hulk = f.player(12.5, FLOOR, 6.5, Power.HULK);
        hulk.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
        check(hulk.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() == 200, "no healer, no shrug");
        helper.succeed();
    }

    /** The Healing Burst: half his health at once, the game's harm cleared (not a spell); refused while poisoned. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theHealingBurst(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        logan.setHealth(4.0F);
        logan.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 0));
        logan.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 400, 0));
        check(HealingBurst.burst(logan, HealingBurst.fraction()), "it heals");
        assertNear(logan.getHealth(), Math.min(logan.getMaxHealth(), 4.0F + logan.getMaxHealth() * 0.5F), 1.0E-4,
                "+50 % of his max health");
        check(!logan.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && logan.hasEffect(ModEffects.SPELLBOUND),
                "Slowness cleared, the spell not");
        ServerPlayer wade = healer(f, 12.5, 6.5, Power.DEADPOOL);
        wade.setHealth(4.0F);
        wade.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
        check(!HealingBurst.burst(wade, 1.0) && wade.getHealth() == 4.0F && wade.hasEffect(MobEffects.POISON),
                "refused while poisoned: nothing healed, the poison stays");
        ServerPlayer hulk = f.player(16.5, FLOOR, 6.5, Power.HULK);
        hulk.setHealth(4.0F);
        hulk.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
        check(HealingBurst.burst(hulk, 0.5) && hulk.getHealth() > 4.0F, "one with no healing factor is healed plainly");
        check(HealingBurst.cooldown() == 400, "its cooldown is burstCooldown");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The rescue

    /** DOWN in a deep tank of water: when he rises he stands on the dry floor beside it, his air full. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void downInDeepWaterHeRisesOnDryGround(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (int x = 8; x <= 12; x++) {
            for (int z = 8; z <= 12; z++) {
                for (int y = 2; y <= 6; y++) {
                    boolean wall = x == 8 || x == 12 || z == 8 || z == 12;
                    helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.GLASS : Blocks.WATER);
                }
            }
        }
        ServerPlayer logan = healer(f, 10.5, 10.5, Power.WOLVERINE);
        logan.doTick();
        check(Rescue.inHazard(logan), "under water without air is a hazard");
        blow(logan, helper.getLevel().damageSources().generic(), 1000.0F);
        check(down(logan), "DOWN in the tank");
        ticks(helper, Downed.downTicks() + 2, true, () -> {
            check(!down(logan) && logan.isAlive(), "he rose");
            check(!logan.isInWater() && Rescue.standable(helper.getLevel(), logan.blockPosition()),
                    "on a dry place to stand");
            check(Math.abs(logan.getY() - helper.absoluteVec(new Vec3(0, FLOOR, 0)).y) < 0.01,
                    "on the floor beside the tank (the nearest), not its rim");
            check(logan.position().distanceTo(helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5))) <= 32.0, "near");
            check(logan.getAirSupply() == logan.getMaxAirSupply(), "his air full");
            check(heard(logan).contains("rise:rescued"), "the hooks hear he was rescued");
            helper.succeed();
        }, logan);
    }

    /** DOWN in lava: he rises out of it, not burning. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void downInLavaHeRisesOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (int x = 8; x <= 12; x++) {
            for (int z = 8; z <= 12; z++) {
                boolean wall = x == 8 || x == 12 || z == 8 || z == 12;
                helper.setBlock(new BlockPos(x, 2, z), wall ? Blocks.STONE : Blocks.LAVA);
            }
        }
        ServerPlayer logan = healer(f, 10.5, 10.5, Power.WOLVERINE);
        logan.doTick();
        check(Rescue.inHazard(logan), "lava is a hazard");
        blow(logan, helper.getLevel().damageSources().lava(), 1000.0F);
        check(down(logan), "DOWN in the lava");
        ticks(helper, Downed.downTicks() + 2, true, () -> {
            check(!down(logan) && logan.isAlive(), "he rose");
            check(!logan.isInLava() && !logan.isOnFire() && logan.getRemainingFireTicks() <= 0,
                    "out of the lava, not burning");
            check(Rescue.standable(helper.getLevel(), logan.blockPosition()), "standing on something safe");
            helper.succeed();
        }, logan);
    }

    // ------------------------------------------------------------------------------------------------------------
    // PvP and Vormir

    /** With PvP on: a friend's blow takes him DOWN; then nothing a player strikes reaches him; Wanda's chaos does. */
    @EnhancedTest(template = ARENA, batch = PVP)
    public static void aFriendsBlowsTakeHimDownChaosDefeatsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        ServerPlayer wade = healer(f, 8.5, 6.5, Power.DEADPOOL);
        ServerPlayer wanda = f.player(12.5, FLOOR, 6.5, Power.SCARLET_WITCH);
        blow(logan, wade.damageSources().playerAttack(wade), 1000.0F);
        check(down(logan), "Deadpool's blow takes Wolverine DOWN (two healers cannot kill each other)");
        blow(wade, logan.damageSources().playerAttack(logan), 1000.0F);
        check(down(wade) && wade.isAlive(), "nor the other way");
        check(!blow(logan, wanda.damageSources().playerAttack(wanda), 50.0F) && logan.getHealth() == 1.0F,
                "a blow at a DOWN healer does nothing");
        blow(logan, ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM, wanda, wanda), 4.0F);
        check(logan.isDeadOrDying(), "the Scarlet Witch's chaos defeats him");
        helper.succeed();
    }

    /** Pushed off a Vormir Cliff, a healer is taken by the Soul Stone's price: he dies. */
    @EnhancedTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void pushedOffVormirAHealerIsTaken(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = f.player(5.5, 6.0, 12.5, Power.THOR);
        ServerPlayer logan = healer(f, 14.5, 14.5, Power.WOLVERINE);
        ServerPlayer friend = f.player(16.5, FLOOR, 14.5);
        chat(friend);
        cliff.standOnEdge(logan, 3);
        f.everyTick(12, tick -> {
            if (tick == 2) {
                check(VormirTracker.onEdge(logan), "the tracker saw him on the edge");
                logan.invulnerableTime = 0;
                check(logan.hurt(thor.damageSources().playerAttack(thor), 1.0F), "Thor's push lands");
                cliff.overThePit(logan, 2.0);
            }
            if (tick == 4) {
                check(cliff.departed(logan), "he left the edge");
                VormirTestSite.fallDie(logan);
                check(logan.isDeadOrDying(), "the Soul Stone's price takes even a healer");
                check(chat(friend).contains("message.enhanced.healing.defeated.vormir"), "and it is told");
                return true;
            }
            return false;
        });
    }

    /** A healer who falls into the chasm alone goes DOWN at its floor and rises (the ladder leads out). */
    @EnhancedTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aSoloFallIntoVormirIsOnlyDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer logan = healer(f, 14.5, 14.5, Power.WOLVERINE);
        cliff.standOnEdge(logan, 3);
        f.everyTick(12, tick -> {
            if (tick == 2) {
                cliff.overThePit(logan, 2.0);
            }
            if (tick == 4) {
                VormirTestSite.fallDie(logan);
                check(logan.isAlive() && down(logan), "alone, he is only DOWN");
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // A boss fight

    /** A boss fight is not wiped while a healer is DOWN: he counts as standing. */
    @EnhancedTest(template = ARENA, batch = "healing_boss", timeoutTicks = 120)
    public static void aBossFightIsNotWipedWhileAHealerIsDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.config(BossConfig.WIPE_SECONDS, 1);
        ServerPlayer logan = healer(f, 10.5, 10.5, Power.WOLVERINE);
        ServerPlayer friend = f.player(12.5, FLOOR, 10.5);
        BossTestSupport.TestFight fight = new BossTestSupport.TestFight(f.level(), BossKind.ULTRON,
                new BossTestSupport.TestArena(f.level(), helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)), 8.0,
                        Containment.NONE, false).containAt(6.0).joinWithin(9.0).tractorDepth(0.0));
        BossFights.register(BossKind.ULTRON, new BossTestSupport.TestLane(BossKind.ULTRON, p -> fight));
        check(BossFights.start(BossKind.ULTRON, friend, false), "the fight starts");
        helper.runAfterDelay(3, () -> {
            friend.kill();
            zombieBlow(logan, zombie(helper, 4.5, 4.5));
            check(down(logan), "the healer is DOWN, his friend dead");
        });
        ticks(helper, 60, false, () -> {
            check(!fight.isOver(), "no wipe while a healer is DOWN (wipeSeconds 1)");
            helper.succeed();
        }, logan);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The options (one batch each: they change the server's config)

    /** Each group of killers can be switched off (it spares him), and the Darkhold on. */
    @EnhancedTest(template = ARENA, batch = CFG)
    public static void eachKillerGroupIsAnOption(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Object[][] groups = {
                {HealingConfig.STONES_DEFEAT, ModDamageTypes.POWER_STONE_BEAM},
                {HealingConfig.CHAOS_DEFEATS, ModDamageTypes.CHAOS_BEAM},
                {HealingConfig.ODIN_DEFEATS, ModDamageTypes.ODINFORCE},
                {HealingConfig.PHOENIX_DEFEATS, ModDamageTypes.PHOENIX_BURST}};
        for (int i = 0; i < groups.length; i++) {
            @SuppressWarnings("unchecked")
            net.neoforged.neoforge.common.ModConfigSpec.BooleanValue option =
                    (net.neoforged.neoforge.common.ModConfigSpec.BooleanValue) groups[i][0];
            @SuppressWarnings("unchecked")
            ResourceKey<DamageType> type = (ResourceKey<DamageType>) groups[i][1];
            f.config(option, false);
            ServerPlayer healer = healer(f, 3.5 + 4 * i, 4.5, Power.WOLVERINE);
            blow(healer, source(helper, type), 1000.0F);
            check(healer.isAlive() && down(healer), option.getPath() + " off: " + type.location() + " spares him");
            f.config(option, true);
        }
        f.config(HealingConfig.DARKHOLD_DEFEATS, true);
        ServerPlayer logan = healer(f, 10.5, 12.5, Power.WOLVERINE);
        blow(logan, source(helper, ModDamageTypes.DARKHOLD_BEAM), 1000.0F);
        check(logan.isDeadOrDying(), "darkholdDefeats on: Agatha's beam defeats him");
        helper.succeed();
    }

    /** poisonStopsHealing off: poison changes nothing; customHeroes off: a custom healer dies; enabled off: anyone. */
    @EnhancedTest(template = ARENA, batch = "healing_cfg_who")
    public static void whoHasItAndWhetherPoisonStopsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(HealingConfig.POISON_STOPS_HEALING, false);
        ServerPlayer logan = healer(f, 3.5, 4.5, Power.WOLVERINE);
        logan.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        zombieBlow(logan, zombie(helper, 3.5, 8.5));
        check(logan.isAlive() && down(logan), "poisonStopsHealing off: poisoned, still only DOWN");
        f.config(HealingConfig.POISON_STOPS_HEALING, true);
        f.config(HealingConfig.CUSTOM_HEROES, false);
        ServerPlayer custom = customHealer(f, 9.5, 4.5);
        ServerPlayer wade = healer(f, 13.5, 4.5, Power.DEADPOOL);
        blow(custom, helper.getLevel().damageSources().generic(), 1000.0F);
        blow(wade, helper.getLevel().damageSources().generic(), 1000.0F);
        check(custom.isDeadOrDying() && wade.isAlive() && down(wade),
                "customHeroes off: the custom hero dies as anyone, Deadpool goes DOWN");
        f.config(HealingConfig.CUSTOM_HEROES, true);
        f.config(HealingConfig.ENABLED, false);
        ServerPlayer logan2 = healer(f, 17.5, 4.5, Power.WOLVERINE);
        blow(logan2, helper.getLevel().damageSources().generic(), 1000.0F);
        check(logan2.isDeadOrDying(), "enabled off: Wolverine dies as anyone");
        helper.succeed();
    }

    /**
     * The timing options: downTicks 20 (he rises after 20 ticks), riseHealth 12, no Resistance; downMobsForget off (the
     * zombie keeps him as its target); the tip on every DOWN with tipOnce off.
     */
    @EnhancedTest(template = ARENA, batch = "healing_cfg_time", timeoutTicks = 80)
    public static void theTimesOfDownAndRise(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(HealingConfig.DOWN_TICKS, 20);
        f.config(HealingConfig.RISE_HEALTH, 12.0);
        f.config(HealingConfig.RISE_RESISTANCE_TICKS, 0);
        f.config(HealingConfig.DOWN_MOBS_FORGET, false);
        f.config(HealingConfig.TIP_ONCE, false);
        ServerPlayer logan = healer(f, 6.5, 6.5, Power.WOLVERINE);
        Zombie zombie = zombie(helper, 9.5, 6.5);
        zombie.setTarget(logan);
        chat(logan);
        zombieBlow(logan, zombie);
        check(down(logan) && zombie.getTarget() == logan, "DOWN, and the zombie keeps him (downMobsForget off)");
        zombie.discard(); // (so no bite lands on him as he rises)
        ticks(helper, 21, false, () -> {
            check(!down(logan), "he rose after 20 ticks");
            check(logan.getHealth() >= 12.0F && !logan.hasEffect(MobEffects.DAMAGE_RESISTANCE), "12 health, no"
                    + " Resistance");
            zombieBlow(logan, zombie);
            List<String> said = chat(logan);
            check(said.stream().filter("message.enhanced.healing.tip"::equals).count() == 2,
                    "the tip at every DOWN (tipOnce off): " + said);
            helper.succeed();
        }, logan);
    }

    /** No dry place within rescueRadius (4) of a healer DOWN in water: he rises at his respawn point. */
    @EnhancedTest(template = ARENA, batch = "healing_cfg_rescue", timeoutTicks = 200)
    public static void noDryPlaceNearHeRisesAtHisRespawnPoint(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(HealingConfig.RESCUE_RADIUS, 4);
        for (int x = 5; x <= 15; x++) {
            for (int z = 5; z <= 15; z++) {
                for (int y = 2; y <= 8; y++) {
                    boolean wall = x == 5 || x == 15 || z == 5 || z == 15;
                    helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.GLASS : y == 8 ? Blocks.AIR : Blocks.WATER);
                }
            }
        }
        BlockPos home = helper.absolutePos(new BlockPos(2, 2, 2));
        ServerPlayer logan = healer(f, 10.5, 10.5, Power.WOLVERINE);
        logan.setRespawnPosition(helper.getLevel().dimension(), home, 0.0F, true, false);
        logan.doTick();
        blow(logan, helper.getLevel().damageSources().generic(), 1000.0F);
        check(down(logan) && Rescue.inHazard(logan), "DOWN under the water");
        check(Rescue.findSpot(helper.getLevel(), logan.blockPosition(), 4, p -> true) == null,
                "no dry place within 4");
        ticks(helper, Downed.downTicks() + 2, true, () -> {
            check(!down(logan), "he rose");
            check(logan.position().distanceTo(Vec3.atBottomCenterOf(home)) < 0.5,
                    "at his respawn point: " + logan.position() + " / " + home);
            helper.succeed();
        }, logan);
    }
}

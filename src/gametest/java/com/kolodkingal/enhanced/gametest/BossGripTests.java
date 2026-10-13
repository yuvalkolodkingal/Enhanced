package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.Telekinesis.Mode;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
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

    private BossGripTests() {
    }
}

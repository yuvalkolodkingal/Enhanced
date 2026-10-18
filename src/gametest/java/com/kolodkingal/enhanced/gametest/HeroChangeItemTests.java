package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * 3.5 (from the 3.4 QA): a hero's worn item (Magneto's helmet, Quicksilver's goggles, Cyclops's glasses, Wanda's crown)
 * comes off the head into the inventory when the player becomes another hero, so it no longer hides the new hero's look;
 * it is never deleted. It stays for its own hero, for a custom hero with that hero's abilities, and when the power is only
 * taken away.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class HeroChangeItemTests {

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    @GameTest(template = ARENA)
    public static void anotherHerosWornItemGoesIntoTheInventory(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Item glasses = ModItems.RUBY_QUARTZ_GLASSES.get();
        ServerPlayer scott = f.player(4.5, FLOOR, 4.5, Power.CYCLOPS);
        scott.setItemSlot(EquipmentSlot.HEAD, new ItemStack(glasses));
        // a custom hero without Cyclops's powers: the glasses come off, into the bag
        CustomKitSupport.become(scott, CustomKitSupport.def().pick("loki.daggers", 5).build());
        check(scott.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the glasses are off his head");
        check(count(scott, glasses) == 1, "and in his inventory, once");
        // a custom hero with the optic blast keeps them on
        scott.getInventory().items.replaceAll(s -> s.is(glasses) ? ItemStack.EMPTY : s);
        scott.setItemSlot(EquipmentSlot.HEAD, new ItemStack(glasses));
        CustomKitSupport.become(scott, CustomKitSupport.def().pick("cyclops.optic_blast", 5).build());
        check(scott.getItemBySlot(EquipmentSlot.HEAD).is(glasses), "a custom hero with Cyclops's optic blast keeps the glasses on");
        // ... until he is a custom hero without them (custom to custom)
        CustomKitSupport.become(scott, CustomKitSupport.def().pick("loki.daggers", 5).build());
        check(scott.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && count(scott, glasses) >= 1, "then they come off");

        // Magneto's helmet: kept when the power is only taken away, off when he becomes Thor
        ServerPlayer erik = f.player(16.5, FLOOR, 4.5, Power.MAGNETO);
        erik.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        Powers.setPower(erik, Power.NONE);
        check(erik.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MAGNETO_HELMET.get()), "no power: the helmet stays (it guards anyone's mind)");
        Powers.setPower(erik, Power.MAGNETO);
        check(erik.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MAGNETO_HELMET.get()), "Magneto again: still on");
        Powers.setPower(erik, Power.THOR);
        check(erik.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && count(erik, ModItems.MAGNETO_HELMET.get()) == 1,
                "Thor: the helmet is in the inventory");

        // an ordinary helmet is no hero's: it stays
        ServerPlayer steve = f.player(4.5, FLOOR, 16.5, Power.QUICKSILVER);
        steve.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Powers.setPower(steve, Power.THOR);
        check(steve.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "an iron helmet stays on");

        // a full inventory: the goggles fall at his feet, never lost
        ServerPlayer pietro = f.player(16.5, FLOOR, 16.5, Power.QUICKSILVER);
        pietro.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.QUICKSILVER_GOGGLES.get()));
        for (int i = 0; i < pietro.getInventory().items.size(); i++) {
            pietro.getInventory().items.set(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        Powers.setPower(pietro, Power.SPIDER_MAN);
        check(pietro.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the goggles are off");
        check(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, pietro.getBoundingBox().inflate(3),
                e -> e.getItem().is(ModItems.QUICKSILVER_GOGGLES.get())).isEmpty(), "and at his feet, the inventory being full");
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, pietro.getBoundingBox().inflate(3)).forEach(ItemEntity::discard);
        helper.succeed();
    }

    private HeroChangeItemTests() {
    }
}

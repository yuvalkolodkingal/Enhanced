package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.HeroServers;
import com.kolodkingal.enhanced.ability.MindReading;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.ability.PhoenixServer;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.item.PhoenixEmberItem;
import com.kolodkingal.enhanced.network.MindReadPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.LokiForm;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Jean Grey, the Phoenix: how she is awakened, what her mind does (reading, sensing, taking over, holding), her
 * telekinesis, how she ends, and how she meets the older heroes. The Burst and its waves are in {@link
 * PhoenixBurstTests}.
 *
 * <p>The tests of a batch run side by side and a setting is server-wide, so: the quick tests ({@link #QUICK}) change a
 * setting only if no other quick test reads it while it is changed, never switch PvP on, and keep what they do inside
 * their own arena (Mind Sense and the Psionic Field are given a radius that fits in it); the tests that need PvP are in
 * {@link #PVP}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class PhoenixTests {
    /** The quick tests: no waves, PvP stays off, no two of them change the same setting. */
    static final String QUICK = "phoenix";
    /** The tests that need PvP on (it is a server-wide setting). */
    static final String PVP = "phoenix_pvp";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers (also used by PhoenixBurstTests)

    static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    static void sneakPress(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        press(player, ability);
        player.setShiftKeyDown(false);
    }

    static ServerPlayer jean(TestKit.Fixture f, double x, double z) {
        return f.player(x, FLOOR, z, Power.PHOENIX);
    }

    /** A husk, which stands still and does not burn in the sun (a zombie does, in a test that runs by day). */
    static Zombie zombie(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
    }

    /** The action-bar lines the server has sent the player since the last call. */
    static List<Component> actionBar(ServerPlayer player) {
        List<Component> lines = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay()) {
                lines.add(chat.content());
            }
        }
        return lines;
    }

    /** The translation keys of the action-bar lines. */
    static List<String> actionBarKeys(ServerPlayer player) {
        return keys(actionBar(player));
    }

    private static List<String> keys(List<Component> lines) {
        List<String> keys = new ArrayList<>();
        for (Component line : lines) {
            if (line.getContents() instanceof TranslatableContents translatable) {
                keys.add(translatable.getKey());
            }
        }
        return keys;
    }

    /** The payloads of a type among the packets a player was sent (they may come in a bundle). */
    static <T extends CustomPacketPayload> List<T> payloads(List<Object> packets, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Object packet : packets) {
            collect(packet, type, found);
        }
        return found;
    }

    private static <T extends CustomPacketPayload> void collect(Object packet, Class<T> type, List<T> found) {
        if (packet instanceof ClientboundCustomPayloadPacket custom && type.isInstance(custom.payload())) {
            found.add(type.cast(custom.payload()));
        } else if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> inner : bundle.subPackets()) {
                collect(inner, type, found);
            }
        }
    }

    /** Whether one of these sounds is the given event. */
    private static boolean heard(List<TestKit.HeardSound> sounds, ResourceLocation event) {
        return sounds.stream().anyMatch(s -> s.id().equals(event));
    }

    private static ItemStack enchanted(Item item, ResourceKey<Enchantment> enchantment, int level, ServerLevel serverLevel) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), level);
        return stack;
    }

    /** Runs a command as the player (with operator rights). */
    private static int run(ServerPlayer player, String command) throws CommandSyntaxException {
        MinecraftServer server = player.getServer();
        CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        return server.getCommands().getDispatcher().execute(command, source);
    }

    /** Tells the game the next ability press finds the cooldown of the slot over. */
    static void rested(ServerPlayer player, int ability) {
        Powers.get(player).startCooldown(ability, player.level().getGameTime(), 0);
    }

    static MindReadPayload read(ServerPlayer jean, LivingEntity target) {
        return MindReading.snapshot(jean, target);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Awakening: the Ember

    @GameTest(template = ARENA, batch = QUICK)
    public static void emberAwakensAPowerlessPlayerAfterTheRitual(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        ItemStack stack = new ItemStack(ModItems.PHOENIX_EMBER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        PhoenixEmberItem ember = (PhoenixEmberItem) stack.getItem();
        check(ember.getUseDuration(stack, player) == PhoenixConfig.EMBER_RITUAL_TICKS.get() && ember.getUseDuration(stack, player) == 60,
                "the hold lasts three seconds");
        TestKit.sentPackets(player);

        check(ember.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction() && player.isUsingItem(), "the hold begins");
        ember.onUseTick(level, player, stack, 60);
        List<Object> held = TestKit.sentPackets(player);
        check(heard(TestKit.soundsIn(held), ModSounds.PHOENIX_CHARGE.getId()), "the Phoenix gathers as the hold begins");
        check(!TestKit.particlesIn(held).isEmpty(), "and flames spiral up the player");

        // Let go too soon: nothing changes, the flames sputter out and the roar is cut.
        ember.releaseUsing(stack, level, player, 30);
        List<Object> sputtered = TestKit.sentPackets(player);
        check(Powers.power(player) == Power.NONE && stack.getCount() == 1, "an early release changes nothing and uses nothing up");
        check(heard(TestKit.soundsIn(sputtered), SoundEvents.FIRE_EXTINGUISH.getLocation()), "the flames sputter out");
        check(sputtered.stream().anyMatch(p -> p instanceof ClientboundStopSoundPacket stop && ModSounds.PHOENIX_CHARGE.getId().equals(stop.getName())),
                "and the gathering roar is cut");

        // Held to the end.
        player.stopUsingItem();
        ember.use(level, player, InteractionHand.MAIN_HAND);
        ember.finishUsingItem(stack, level, player);
        check(Powers.power(player) == Power.PHOENIX, "the end of the hold awakens the Phoenix");
        check(stack.isEmpty(), "and the Ember is used up");
        check(Variants.current(Powers.get(player)).id().equals("apocalypse"), "in her first look");
        check(player.getHealth() == player.getMaxHealth(), "it did not hurt");
        List<Object> woke = TestKit.sentPackets(player);
        check(woke.stream().anyMatch(p -> p instanceof ClientboundSetTitleTextPacket), "the title is shown");
        check(woke.stream().anyMatch(p -> p instanceof ClientboundSystemChatPacket chat && !chat.overlay()
                && chat.content().getContents() instanceof TranslatableContents t && t.getKey().equals("power.enhanced.phoenix.broadcast")),
                "and the server is told");
        check(heard(TestKit.soundsIn(woke), ModSounds.PHOENIX_BURST.getId()) && heard(TestKit.soundsIn(woke), ModSounds.AWAKEN.getId()),
                "with the Burst's roar and the awakening");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void theEmberIsHeldAsLongAsTheServerSays(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        ItemStack stack = new ItemStack(ModItems.PHOENIX_EMBER.get());
        f.config(PhoenixConfig.EMBER_RITUAL_TICKS, 120);
        check(stack.getItem().getUseDuration(stack, player) == 120, "the hold follows emberRitualTicks");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void aHeroCannotBeAwakenedByTheEmber(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        for (Power hero : List.of(Power.MAGNETO, Power.THOR)) {
            ServerPlayer player = f.player(10.5, FLOOR, 10.5, hero);
            ItemStack stack = new ItemStack(ModItems.PHOENIX_EMBER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            TestKit.sentPackets(player);
            InteractionResultHolder<ItemStack> used = stack.getItem().use(level, player, InteractionHand.MAIN_HAND);
            check(!used.getResult().consumesAction() && !player.isUsingItem(), hero + ": no hold begins");
            check(actionBarKeys(player).contains("message.enhanced.phoenix_ember.already_powered"), hero + " is told their body already holds a power");
            stack.getItem().finishUsingItem(stack, level, player);
            check(Powers.power(player) == hero && stack.getCount() == 1, hero + " stays what they are and keeps the Ember");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void creativeKeepsTheEmber(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        ItemStack stack = new ItemStack(ModItems.PHOENIX_EMBER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(level, player, InteractionHand.MAIN_HAND);
        stack.getItem().finishUsingItem(stack, level, player);
        check(Powers.power(player) == Power.PHOENIX && stack.getCount() == 1, "a Creative player awakens and keeps the Ember");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void emberIsInTheChestsLikeTheTesseract(GameTestHelper helper) throws IOException {
        ServerLevel level = helper.getLevel();
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();

        // The data: six modifiers, each for its chest, with its chance, all listed in the global list.
        record Chest(String name, String table, double chance, ResourceKey<LootTable> key) {
        }
        List<Chest> chests = List.of(
                new Chest("end_city_treasure", "end_city_treasure", 0.06, BuiltInLootTables.END_CITY_TREASURE),
                new Chest("bastion_treasure", "bastion_treasure", 0.05, BuiltInLootTables.BASTION_TREASURE),
                new Chest("ancient_city", "ancient_city", 0.04, BuiltInLootTables.ANCIENT_CITY),
                new Chest("stronghold_library", "stronghold_library", 0.03, BuiltInLootTables.STRONGHOLD_LIBRARY),
                new Chest("nether_bridge", "nether_bridge", 0.03, BuiltInLootTables.NETHER_BRIDGE),
                new Chest("woodland_mansion", "woodland_mansion", 0.03, BuiltInLootTables.WOODLAND_MANSION));
        Path list = mod.findResource("data", "neoforge", "loot_modifiers", "global_loot_modifiers.json");
        JsonArray listed = JsonParser.parseString(Files.readString(list, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("entries");
        Set<String> names = new HashSet<>();
        listed.forEach(e -> names.add(e.getAsString()));
        for (Chest chest : chests) {
            check(names.contains("enhanced:ember_from_" + chest.name()), "the global list has ember_from_" + chest.name());
            Path file = mod.findResource("data", Enhanced.MODID, "loot_modifiers", "ember_from_" + chest.name() + ".json");
            JsonObject modifier = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            check(modifier.get("type").getAsString().equals("neoforge:add_table") && modifier.get("table").getAsString().equals("enhanced:inject/phoenix_ember"),
                    chest.name() + " adds the Ember's table");
            JsonArray conditions = modifier.getAsJsonArray("conditions");
            check(conditions.get(0).getAsJsonObject().get("loot_table_id").getAsString().equals("minecraft:chests/" + chest.table()),
                    chest.name() + " is for its own chest");
            assertNear(conditions.get(1).getAsJsonObject().get("chance").getAsDouble(), chest.chance(), 1.0E-9, chest.name() + "'s chance");
        }

        // The behaviour: the real chest tables give the Ember at about the chance the data says.
        int rolls = 4000;
        RandomSource random = RandomSource.create(1983L);
        for (Chest chest : chests) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(chest.key());
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(10.5, 2, 10.5)))
                    .create(LootContextParamSets.CHEST);
            int embers = 0;
            for (int i = 0; i < rolls; i++) {
                for (ItemStack stack : table.getRandomItems(params, random)) {
                    if (stack.is(ModItems.PHOENIX_EMBER.get())) {
                        embers += stack.getCount();
                    }
                }
            }
            double expected = rolls * chest.chance();
            double deviation = 5.5 * Math.sqrt(rolls * chest.chance() * (1.0 - chest.chance()));
            check(Math.abs(embers - expected) <= deviation + 1, chest.name() + ": the Ember is in about " + (int) (chest.chance() * 100)
                    + " percent of the chests, got " + embers + " of " + rolls);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void commandSetsPhoenixInALook(GameTestHelper helper) throws CommandSyntaxException {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        run(player, "enhanced power set @s phoenix brand_new_day");
        check(Powers.is(player, Power.PHOENIX) && Powers.get(player).variant().equals("brand_new_day"), "the Phoenix in her Brand New Day look");
        run(player, "enhanced look @s own");
        check(Powers.get(player).variant().equals("own"), "and in her own skin");
        boolean refused = false;
        try {
            run(player, "enhanced power set @s phoenix not_a_look");
        } catch (CommandSyntaxException expected) {
            refused = true;
        }
        check(refused && Powers.get(player).variant().equals("own"), "an unknown look is refused and changes nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void phoenixLooksAreTheTwoFilmsAndYourOwn(GameTestHelper helper) {
        List<Variant> looks = Variants.of(Power.PHOENIX);
        check(looks.stream().map(Variant::id).toList().equals(List.of("apocalypse", "brand_new_day", "own")), "the looks: " + looks);
        for (Variant look : looks.subList(0, 2)) {
            check(look.slim() && look.skin() != null && look.cape() == null, look.id() + " is a slim suit with no cape");
        }
        check(looks.get(2).ownSkin(), "the last look is the player's own skin");
        check(looks.get(0).color() == 0xFF8A1F && looks.get(0).glow() == 0xFFD65A, "the Apocalypse look burns gold and orange");
        check(looks.get(1).color() == 0xF2B632 && looks.get(1).glow() == 0xC4E88A, "the Brand New Day look is gold and pale green");
        check(HeroServers.of(Power.PHOENIX) instanceof PhoenixServer, "the Phoenix's server side is her own plug-in");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 1: Mind Reading

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindReadingShowsHandsArmourAndTheMood(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        Zombie zombie = zombie(helper, 10.5, 10.5);
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(15.5, FLOOR, 15.5));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        lookAt(jean, zombie);
        actionBar(jean);

        press(jean, 1);
        PowerData data = Powers.get(jean);
        check(data.toggle(Toggle.MIND_READING), "R switches Mind Reading on");
        check(actionBarKeys(jean).contains("message.enhanced.mind_reading.on"), "and says so");
        check(MindReading.target(jean) == zombie, "she looks at the zombie");
        MindReadPayload calm = read(jean, zombie);
        check(calm.state() == MindReadPayload.OK && calm.mood() == MindReadPayload.CALM && calm.hostileTo().isEmpty(), "an idle zombie is calm");
        check(calm.species().equals("minecraft:husk") && calm.entityId() == zombie.getId(), "it is a husk");
        check(calm.worn().size() == 7, "seven slots are worn");
        check(calm.worn().get(0).is(Items.IRON_SWORD) && calm.worn().get(2).is(Items.IRON_HELMET), "the sword in the hand, the helmet on the head");
        check(calm.worn().get(1).isEmpty() && calm.worn().get(3).isEmpty() && calm.worn().get(6).isEmpty(), "and nothing else");
        check(calm.carried().isEmpty(), "a zombie carries nothing");
        assertNear(calm.health(), zombie.getHealth(), 1.0E-4, "its health");

        zombie.setTarget(skeleton);
        MindReadPayload hostile = read(jean, zombie);
        check(hostile.mood() == MindReadPayload.HOSTILE && hostile.hostileTo().equals(skeleton.getName().getString()),
                "a zombie with a target means harm to it: " + hostile.hostileTo());

        // The server tells her client when the target or what it holds changes, or at least every 20 ticks.
        zombie.setTarget(null);
        TestKit.sentPackets(jean);
        MindReading.look(jean, 1000);
        check(payloads(TestKit.sentPackets(jean), MindReadPayload.class).size() == 1, "a new target is sent at once");
        MindReading.look(jean, 1005);
        check(payloads(TestKit.sentPackets(jean), MindReadPayload.class).isEmpty(), "the same thing is not sent again");
        zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        MindReading.look(jean, 1010);
        check(payloads(TestKit.sentPackets(jean), MindReadPayload.class).size() == 1, "what changed is sent");
        MindReading.look(jean, 1031);
        check(payloads(TestKit.sentPackets(jean), MindReadPayload.class).size() == 1, "and so is the same thing after 20 ticks");
        press(jean, 1);
        check(!data.toggle(Toggle.MIND_READING) && actionBarKeys(jean).contains("message.enhanced.mind_reading.off"), "R again switches it off");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindReadingListsAPlayersWholeInventory(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 4.5, 10.5);
        ServerPlayer other = f.player(10.5, FLOOR, 10.5);
        other.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        other.getInventory().setItem(8, new ItemStack(Items.APPLE, 16));
        other.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 64));
        other.getInventory().setItem(20, enchanted(Items.DIAMOND_PICKAXE, Enchantments.EFFICIENCY, 3, level));
        other.getInventory().setItem(35, new ItemStack(Items.EMERALD, 3));
        other.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        other.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        other.getFoodData().setFoodLevel(14);
        other.experienceLevel = 7;

        MindReadPayload all = read(jean, other);
        check(all.state() == MindReadPayload.OK && all.species().equals("minecraft:player"), "a player is read");
        check(all.name().equals(other.getGameProfile().getName()), "by name");
        check(all.carried().size() == 36, "all 36 slots come back, empty ones too: " + all.carried().size());
        check(all.carried().get(0).is(Items.DIAMOND_SWORD) && all.carried().get(8).is(Items.APPLE) && all.carried().get(8).getCount() == 16,
                "the hotbar first");
        check(all.carried().get(9).is(Items.IRON_INGOT) && all.carried().get(9).getCount() == 64 && all.carried().get(35).is(Items.EMERALD)
                && all.carried().get(35).getCount() == 3, "then the pack");
        check(all.carried().get(20).is(Items.DIAMOND_PICKAXE) && EnchantmentHelper.hasAnyEnchantments(all.carried().get(20)), "enchantments are kept");
        check(all.carried().get(1).isEmpty(), "an empty slot is empty");
        check(all.food() == 14 && all.level() == 7, "their food and level");
        check(all.worn().get(0).is(Items.DIAMOND_SWORD) && all.worn().get(3).is(Items.IRON_CHESTPLATE), "and what they hold and wear");

        f.config(PhoenixConfig.MIND_READ_INVENTORY, false);
        MindReadPayload handsOnly = read(jean, other);
        check(handsOnly.carried().isEmpty() && handsOnly.worn().get(0).is(Items.DIAMOND_SWORD) && handsOnly.worn().get(3).is(Items.IRON_CHESTPLATE),
                "with the pack private only the hands and the armour are shown");

        f.config(PhoenixConfig.MIND_READ_PLAYERS, false);
        MindReadPayload none = read(jean, other);
        check(none.state() == MindReadPayload.PLAYERS_OFF && none.worn().isEmpty() && none.carried().isEmpty(), "with players private, nothing at all");
        MindReadPayload zombie = read(jean, zombie(helper, 12.5, 12.5));
        check(zombie.state() == MindReadPayload.OK, "though a zombie's mind is still read");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindReadingNeedsLineOfSightAndRange(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        Zombie zombie = zombie(helper, 12.5, 10.5);
        lookAt(jean, zombie);
        check(MindReading.target(jean) == zombie, "a zombie 8 blocks away in plain sight is found");

        helper.setBlock(new BlockPos(7, 2, 10), Blocks.STONE);
        helper.setBlock(new BlockPos(7, 3, 10), Blocks.STONE);
        check(MindReading.target(jean) == null, "not behind a block");
        helper.setBlock(new BlockPos(7, 2, 10), Blocks.AIR);
        helper.setBlock(new BlockPos(7, 3, 10), Blocks.AIR);
        check(MindReading.target(jean) == zombie, "found again when the block is gone");

        f.config(PhoenixConfig.MIND_READ_RANGE, 4);
        check(MindReading.target(jean) == null, "and not beyond her range (4 blocks here)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindReadingRefusesTheUnreadable(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 10.5, 10.5);
        ServerPlayer creative = f.player(14.5, FLOOR, 10.5, GameType.CREATIVE);
        ServerPlayer spectator = f.player(6.5, FLOOR, 10.5, GameType.SPECTATOR);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(10.5, FLOOR, 14.5));
        ServerPlayer magneto = f.player(10.5, FLOOR, 6.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        ServerPlayer loki = f.player(1.5, FLOOR, 1.5, Power.LOKI);
        LokiIllusionEntity copy = LokiIllusionEntity.conjure(loki, helper.absoluteVec(new Vec3(13.5, FLOOR, 13.5)), null, LokiIllusionEntity.Mode.DECOY, 200);
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(7.5, FLOOR, 7.5));

        for (Entity unreadable : List.of(creative, spectator, stand)) {
            lookAt(jean, unreadable);
            check(MindReading.target(jean) == null, unreadable.getType().getDescription().getString() + " is no mind to read");
        }
        record Refusal(LivingEntity who, int state) {
        }
        for (Refusal refusal : List.of(new Refusal(magneto, MindReadPayload.BLOCKED), new Refusal(copy, MindReadPayload.ILLUSION),
                new Refusal(warden, MindReadPayload.TOO_VAST))) {
            lookAt(jean, refusal.who());
            check(MindReading.target(jean) == refusal.who(), refusal.who().getType().getDescription().getString() + " is looked at");
            MindReadPayload snapshot = read(jean, refusal.who());
            check(snapshot.state() == refusal.state(), "and its state is " + refusal.state() + ", not " + snapshot.state());
            check(snapshot.worn().isEmpty() && snapshot.carried().isEmpty() && snapshot.extra().isEmpty(), "nothing else of it is told");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindReadingSeesThroughLokisShape(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        ServerPlayer loki = f.player(10.5, FLOOR, 10.5, Power.LOKI);
        check(read(jean, loki).extra().isEmpty(), "Loki as himself shows nothing special");
        Powers.get(loki).setForm(LokiForm.creature(EntityType.ZOMBIE).serialize());
        check(read(jean, loki).extra().equals("shaped:minecraft:zombie"), "Loki as a zombie is seen for what he is: " + read(jean, loki).extra());
        UUID someone = UUID.randomUUID();
        Powers.get(loki).setForm(LokiForm.player(someone, "Peter").serialize());
        check(read(jean, loki).extra().equals("shaped:" + LokiForm.player(someone, "Peter").serialize()), "and so is Loki as a player");

        ServerPlayer vision = f.player(12.5, FLOOR, 12.5, Power.VISION);
        check(read(jean, vision).extra().isEmpty(), "Vision as himself shows nothing special");
        Powers.get(vision).setDisguise(true);
        check(read(jean, vision).extra().equals("disguised"), "Vision in his human disguise is found out");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void thePlayerWhoIsReadFeelsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        ServerPlayer other = f.player(10.5, FLOOR, 10.5);
        lookAt(jean, other);
        actionBar(other);
        MindReading.look(jean, 5000);
        check(actionBarKeys(other).contains("message.enhanced.mind_read.felt"), "the player who is read feels a presence");
        MindReading.look(jean, 5100);
        check(!actionBarKeys(other).contains("message.enhanced.mind_read.felt"), "but not again within 10 seconds");
        MindReading.look(jean, 5200);
        check(actionBarKeys(other).contains("message.enhanced.mind_read.felt"), "and again when they are over");

        // A mind that was not read (Magneto's helmet keeps it shut) feels nothing.
        ServerPlayer magneto = f.player(10.5, FLOOR, 16.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        lookAt(jean, magneto);
        actionBar(magneto);
        MindReading.look(jean, 7000);
        check(!actionBarKeys(magneto).contains("message.enhanced.mind_read.felt"), "a mind that stays shut feels no presence");
        lookAt(jean, other);

        f.config(PhoenixConfig.MIND_READ_NOTIFIES, false);
        MindReading.look(jean, 9000);
        check(!actionBarKeys(other).contains("message.enhanced.mind_read.felt"), "never, if the server says so");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindReadingCostsHunger(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 10.5, 10.5);
        ServerPlayer creative = f.player(2.5, FLOOR, 2.5, GameType.CREATIVE);
        Powers.setPower(creative, Power.PHOENIX);
        press(jean, 1);
        press(creative, 1);
        check(Powers.get(jean).toggle(Toggle.MIND_READING) && Powers.get(creative).toggle(Toggle.MIND_READING), "both are reading");
        float before = jean.getFoodData().getExhaustionLevel();
        float creativeBefore = creative.getFoodData().getExhaustionLevel();
        for (int i = 0; i < 20; i++) {
            PowerTicker.tick(jean);
            PowerTicker.tick(creative);
        }
        assertNear(jean.getFoodData().getExhaustionLevel() - before, 20 * 0.01, 1.0E-4, "20 ticks of reading cost 0.01 of exhaustion each");
        assertNear(creative.getFoodData().getExhaustionLevel() - creativeBefore, 0.0, 1.0E-6, "in Creative nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindReadPayloadRoundTrips(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ItemStack> worn = new ArrayList<>();
        List<ItemStack> carried = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            worn.add(enchanted(Items.DIAMOND_SWORD, Enchantments.SHARPNESS, 1 + i % 5, level));
        }
        for (int i = 0; i < 34; i++) {
            carried.add(i % 4 == 0 ? ItemStack.EMPTY : enchanted(Items.DIAMOND_PICKAXE, Enchantments.EFFICIENCY, 1 + i % 5, level));
        }
        MindReadPayload payload = new MindReadPayload(7, "Jean", "minecraft:player", MindReadPayload.OK, MindReadPayload.HOSTILE, "Peter", 17.5F, 20.0F, 14, 30,
                worn, carried, "shaped:minecraft:zombie");
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        MindReadPayload.STREAM_CODEC.encode(buf, payload);
        MindReadPayload back = MindReadPayload.STREAM_CODEC.decode(buf);
        check(MindReading.sameContent(payload, back), "everything comes back as it went");
        check(back.worn().size() == 7 && back.carried().size() == 34 && back.carried().get(0).isEmpty(), "with the same slots");
        check(back.name().equals("Jean") && back.hostileTo().equals("Peter") && back.extra().equals("shaped:minecraft:zombie"), "and the same words");
        assertNear(back.health(), 17.5, 1.0E-6, "and the same health");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindReadPayloadIsStrippedAndBounded(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 4.5, 10.5);
        ServerPlayer other = f.player(10.5, FLOOR, 10.5);

        // A shulker box full of shulker boxes full of books, and a signed book.
        ItemStack inner = new ItemStack(Items.SHULKER_BOX);
        List<ItemStack> books = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            book.set(DataComponents.CUSTOM_NAME, Component.literal("x".repeat(300)));
            books.add(book);
        }
        inner.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(books));
        ItemStack outer = new ItemStack(Items.SHULKER_BOX);
        outer.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Collections.nCopies(27, inner)));
        other.getInventory().setItem(10, outer);
        ItemStack signed = new ItemStack(Items.WRITTEN_BOOK);
        signed.set(DataComponents.CUSTOM_NAME, Component.literal("The Dark Phoenix Saga ".repeat(20)));
        other.getInventory().setItem(11, signed);
        check(MindReading.encodedSize(jean, new MindReadPayload(1, "", "", 0, 0, "", 0, 0, 0, 0, List.of(outer), List.of(), "")) > 100_000,
                "(control: the unstripped box is far over the limit)");

        MindReadPayload snapshot = read(jean, other);
        ItemStack stripped = snapshot.carried().get(10);
        check(stripped.is(Items.SHULKER_BOX) && !stripped.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems().iterator().hasNext(),
                "the box is shown but empty of contents");
        ItemStack strippedBook = snapshot.carried().get(11);
        check(strippedBook.is(Items.WRITTEN_BOOK) && !strippedBook.has(DataComponents.WRITTEN_BOOK_CONTENT), "the book keeps no pages");
        String name = strippedBook.get(DataComponents.CUSTOM_NAME).getString();
        check(name.length() <= 64 && strippedBook.get(DataComponents.CUSTOM_NAME).getContents() instanceof PlainTextContents,
                "and a plain, short name");
        int size = MindReading.encodedSize(jean, snapshot);
        check(size < MindReading.MAX_BYTES, "the whole snapshot is small: " + size + " bytes");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Mind Sense (Sneak + R)

    @GameTest(template = ARENA, batch = QUICK, timeoutTicks = 80)
    public static void mindSensePingFlagsAndCoolsDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // A radius that fits in the arena (the quick tests share the level), and a short ping.
        f.config(PhoenixConfig.MIND_SENSE_RANGE, 8);
        f.config(PhoenixConfig.MIND_SENSE_TICKS, 20);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 10.5, 10.5);
        zombie(helper, 13.5, 10.5);
        zombie(helper, 7.5, 10.5);
        helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(10.5, FLOOR, 14.5));
        f.player(8.5, FLOOR, 13.5);
        ServerPlayer magneto = f.player(10.5, FLOOR, 7.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        f.player(12.5, FLOOR, 12.5, GameType.CREATIVE);
        helper.spawn(EntityType.ARMOR_STAND, new Vec3(8.5, FLOOR, 8.5));
        ServerPlayer loki = f.player(1.5, FLOOR, 1.5, Power.LOKI);
        LokiIllusionEntity.conjure(loki, helper.absoluteVec(new Vec3(12.5, FLOOR, 8.5)), null, LokiIllusionEntity.Mode.DECOY, 200);

        PowerData data = Powers.get(jean);
        long now = level.getGameTime();
        float exhaustion = jean.getFoodData().getExhaustionLevel();
        actionBar(jean);
        sneakPress(jean, 1);
        check(!data.toggle(Toggle.MIND_READING), "Sneak + R is the ping, not the toggle");
        check(data.is(Live.MIND_SENSING), "the ping is on");
        check(data.cooldownUntil(1) == now + 400, "and the cooldown of ability 1 is 20 seconds: " + (data.cooldownUntil(1) - now));
        assertNear(jean.getFoodData().getExhaustionLevel() - exhaustion, 2.0, 1.0E-4, "it costs 2.0 of exhaustion");

        List<Component> told = actionBar(jean);
        check(keys(told).contains("message.enhanced.mind_sense.result"), "she is told what she found");
        TranslatableContents result = (TranslatableContents) told.stream().filter(c -> c.getContents() instanceof TranslatableContents t
                && t.getKey().equals("message.enhanced.mind_sense.result")).findFirst().orElseThrow().getContents();
        check(List.of(4, 1, 2).equals(List.of(result.getArgs())), "4 minds (two zombies, a pig, a player), 1 player, 2 hostile: " + List.of(result.getArgs()));
        Component line = told.get(told.size() - 1);
        check(line.getSiblings().stream().anyMatch(c -> c.getContents() instanceof TranslatableContents blank
                && blank.getKey().equals("message.enhanced.mind_sense.blank") && blank.getArgs()[0].equals(1)),
                "and of one silence where a mind should be (Magneto's helmet): " + line.getSiblings());

        sneakPress(jean, 1);
        check(actionBarKeys(jean).contains("message.enhanced.mind_sense.recovering"), "a second ping is refused while the first recovers");
        check(data.cooldownUntil(1) == now + 400, "and does not restart the cooldown");
        press(jean, 1);
        check(data.toggle(Toggle.MIND_READING), "though Mind Reading can still be switched on");

        f.everyTick(30, tick -> {
            PowerTicker.tick(jean);
            if (tick == 5) {
                check(data.is(Live.MIND_SENSING), "the ping is still on after 5 ticks");
            }
            return tick >= 25 && !data.is(Live.MIND_SENSING);
        });
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindSenseFindsAHiddenLoki(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 10.5, 10.5);
        ServerPlayer loki = f.player(14.5, FLOOR, 10.5, Power.LOKI);
        Powers.get(loki).set(Live.HIDDEN, true);
        loki.setInvisible(true);
        LokiIllusionEntity first = LokiIllusionEntity.conjure(loki, helper.absoluteVec(new Vec3(13.5, FLOOR, 12.5)), null, LokiIllusionEntity.Mode.DECOY, 200);
        LokiIllusionEntity second = LokiIllusionEntity.conjure(loki, helper.absoluteVec(new Vec3(7.5, FLOOR, 12.5)), null, LokiIllusionEntity.Mode.DECOY, 200);

        check(PhoenixAbilities.hasMind(loki), "Loki, invisible behind his duplicates, has a mind");
        check(!PhoenixAbilities.hasMind(first) && !PhoenixAbilities.hasMind(second), "his duplicates do not");
        MindReading.Sensed sensed = MindReading.sense(jean, 8.0);
        check(sensed.minds().contains(loki) && !sensed.minds().contains(first) && !sensed.minds().contains(second), "the ping finds him and not his copies");
        check(sensed.players() == 1 && sensed.hostile() == 0 && sensed.silences() == 0, "one player");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 2: Flight

    @GameTest(template = ARENA, batch = QUICK)
    public static void theKeyOfFlightTakesHerUpAndDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 10.5, 10.5);
        PowerTicker.tick(jean);
        check(jean.mayFly(), "the Phoenix can fly");
        assertNear(jean.getAbilities().getFlyingSpeed(), 0.09, 1.0E-4, "at 0.09");
        press(jean, 2);
        check(jean.getAbilities().flying, "G takes her up");
        press(jean, 2);
        check(!jean.getAbilities().flying, "G again lands her");
        f.config(PhoenixConfig.FLIGHT_SPEED, 0.2);
        PowerTicker.tick(jean);
        assertNear(jean.getAbilities().getFlyingSpeed(), 0.2, 1.0E-4, "the speed follows the server's setting");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 3: Telekinesis

    @GameTest(template = ARENA, batch = QUICK, timeoutTicks = 60)
    public static void phoenixTelekinesisLiftsAndLaunches(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 3.5, 10.5);
        PowerData data = Powers.get(jean);
        Zombie held = zombie(helper, 7.5, 10.5);
        Zombie victim = zombie(helper, 14.5, 10.5);
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        victim.setHealth(100.0F);
        List<String> touches = new ArrayList<>();
        PhoenixPower.Listener listener = (phoenix, target, kind, amount) -> {
            if (phoenix == jean && kind == PhoenixPower.Kind.SLAM) {
                touches.add(target.getId() + ":" + (amount > 0));
            }
        };
        PhoenixPower.addListener(listener);
        f.onClose(() -> PhoenixPower.removeListener(listener));

        lookAt(jean, held);
        press(jean, 3);
        check(Telekinesis.isHolding(jean) && data.heldEntityId() == held.getId(), "V lifts a zombie");
        look(jean, EAST, 0.0F);
        f.everyTick(40, tick -> {
            PowerTicker.tick(jean);
            if (tick == 4) {
                look(jean, EAST, 0.0F);
                press(jean, 3);
                check(!Telekinesis.isHolding(jean), "V again launches it");
            }
            if (tick > 4 && victim.getHealth() < 100.0F) {
                DamageSource source = victim.getLastDamageSource();
                check(source != null && source.is(ModDamageTypes.TELEKINETIC_SLAM) && source.getEntity() == jean,
                        "what it hits is hurt by a telekinetic slam, and Jean gets the blame: " + source);
                check(PhoenixPower.isPhoenixMindPower(source), "the slam is the Phoenix's mind power");
                check(touches.equals(List.of(victim.getId() + ":true")), "the listeners hear of the slam once: " + touches);
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = QUICK, timeoutTicks = 60)
    public static void phoenixTelekinesisCatchesArrowsItemsAndBlocksButNotBosses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 4.5, 10.5);
        PowerData data = Powers.get(jean);
        look(jean, EAST, 0.0F);

        // An arrow in flight hangs in the air, and flies again where she looks.
        Arrow arrow = new Arrow(EntityType.ARROW, level);
        Vec3 start = helper.absoluteVec(new Vec3(14.5, FLOOR + 1.4, 10.5));
        arrow.moveTo(start.x, start.y, start.z);
        arrow.setDeltaMovement(-0.8, 0.0, 0.0);
        level.addFreshEntity(arrow);
        press(jean, 3);
        check(Telekinesis.isHolding(jean) && data.heldEntityId() == arrow.getId(), "she catches the arrow flying at her");

        f.everyTick(20, tick -> {
            PowerTicker.tick(jean);
            if (tick < 10) {
                return false;
            }
            check(arrow.getDeltaMovement().length() < 0.01, "it hangs in the air");
            press(jean, 3);
            check(!Telekinesis.isHolding(jean) && arrow.getDeltaMovement().x > 1.5, "and flies again where she looks");
            arrow.discard();

            // A dropped item.
            ItemEntity drop = new ItemEntity(level, 0, 0, 0, new ItemStack(Items.DIAMOND));
            Vec3 at = helper.absoluteVec(new Vec3(8.5, FLOOR + 0.5, 10.5));
            drop.moveTo(at.x, at.y, at.z);
            level.addFreshEntity(drop);
            lookAt(jean, drop);
            press(jean, 3);
            check(Telekinesis.isHolding(jean) && data.heldEntityId() == drop.getId(), "a dropped item is lifted");
            Telekinesis.release(jean, false);
            drop.discard();

            // A block of stone (hardness at most 5): torn out, held, and violet.
            BlockPos stone = new BlockPos(9, 2, 10);
            helper.setBlock(stone, Blocks.STONE);
            lookAt(jean, Vec3.atCenterOf(helper.absolutePos(stone)));
            press(jean, 3);
            helper.assertBlockNotPresent(Blocks.STONE, stone);
            List<TelekineticBlockEntity> blocks = helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get());
            check(blocks.size() == 1 && Telekinesis.isHolding(jean) && data.heldEntityId() == blocks.get(0).getId(), "a block of stone is torn out and held");
            check(blocks.get(0).getColor() == Telekinesis.Mode.JEAN.color, "in the violet of her mind, not Wanda's red");
            Telekinesis.release(jean, false);
            blocks.get(0).discard();

            // Bosses are too strong, and so is a block that is too hard.
            Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(12.5, FLOOR, 12.5));
            check(!Telekinesis.canLift(jean, warden, Telekinesis.Mode.JEAN), "a Warden cannot be lifted");
            BlockPos obsidian = new BlockPos(9, 2, 7);
            helper.setBlock(obsidian, Blocks.OBSIDIAN);
            lookAt(jean, Vec3.atCenterOf(helper.absolutePos(obsidian)));
            press(jean, 3);
            helper.assertBlockPresent(Blocks.OBSIDIAN, obsidian);
            check(!Telekinesis.isHolding(jean), "obsidian is too hard to tear out");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void sneakAndVSetsDownAndLosingThePowerLetsGo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        PowerData data = Powers.get(jean);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(9.5, FLOOR, 10.5));
        lookAt(jean, pig);
        press(jean, 3);
        check(Telekinesis.isHolding(jean), "lifted");
        sneakPress(jean, 3);
        check(!Telekinesis.isHolding(jean) && data.heldEntityId() == -1, "Sneak + V sets it down");
        check(pig.getDeltaMovement().length() < 0.1, "gently, not thrown");

        lookAt(jean, pig);
        press(jean, 3);
        check(Telekinesis.isHolding(jean), "lifted again");
        Powers.setPower(jean, Power.NONE);
        check(!Telekinesis.isHolding(jean), "losing the power lets it go");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void phoenixTelekinesisUsesPsychicColours(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(Telekinesis.Mode.JEAN.color == PhoenixAbilities.PSY_MAIN, "Jean's telekinesis is violet");
        check(Telekinesis.Mode.WANDA.color == Power.SCARLET_WITCH.color && Telekinesis.Mode.BILLY.color == Power.BILLY.color
                && Telekinesis.Mode.MAGNETO.color == Power.MAGNETO.color, "the others keep the colour of their power");
        ServerPlayer jean = jean(f, 4.5, 10.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(9.5, FLOOR, 10.5));
        lookAt(jean, pig);
        TestKit.sentPackets(jean);
        press(jean, 3);
        PowerTicker.tick(jean);
        PowerTicker.tick(jean);
        List<Integer> colours = TestKit.particlesIn(TestKit.sentPackets(jean)).stream().map(ClientboundLevelParticlesPacket::getParticle)
                .filter(EnergyParticleOptions.class::isInstance).map(p -> ((EnergyParticleOptions) p).color()).toList();
        check(colours.contains(PhoenixAbilities.PSY_MAIN) && !colours.contains(Power.SCARLET_WITCH.color), "what she holds glows violet: " + colours);
        Telekinesis.release(jean, false);

        ServerPlayer wanda = f.player(4.5, FLOOR, 16.5, Power.SCARLET_WITCH);
        BlockPos dirt = new BlockPos(9, 2, 16);
        helper.setBlock(dirt, Blocks.DIRT);
        lookAt(wanda, Vec3.atCenterOf(helper.absolutePos(dirt)));
        press(wanda, 3);
        List<TelekineticBlockEntity> blocks = helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get());
        check(blocks.size() == 1 && blocks.get(0).getColor() == Power.SCARLET_WITCH.color, "Wanda's block is still red");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 4: Mind Control

    @GameTest(template = ARENA, batch = "phoenix_control", timeoutTicks = 80)
    public static void mindControlTakesOneCreatureThatFightsForHer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(PhoenixConfig.MIND_CONTROL_TICKS, 20);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 4.5, 10.5);
        PowerData data = Powers.get(jean);
        Zombie host = zombie(helper, 9.5, 10.5);
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(12.5, FLOOR, 14.5));
        lookAt(jean, host);
        float exhaustion = jean.getFoodData().getExhaustionLevel();
        long now = level.getGameTime();
        actionBar(jean);

        press(jean, 4);
        check(host.hasEffect(ModEffects.POSSESSED), "B takes over the zombie");
        check(host.getEffect(ModEffects.POSSESSED).getDuration() == 20, "for as long as the server says");
        check(PhoenixAbilities.hostsOf(jean).equals(List.of(host)), "it is her host");
        check(data.cooldownUntil(4) == now + 400, "ability 4 rests 20 seconds");
        assertNear(jean.getFoodData().getExhaustionLevel() - exhaustion, 6.0, 1.0E-4, "and it cost 6.0 of exhaustion");
        check(data.is(Live.CASTING), "she is casting");
        check(actionBarKeys(jean).contains("message.enhanced.mind_control.host"), "and is told whom she took");
        check(host.getTarget() == skeleton, "the zombie goes for the skeleton, the nearest hostile near her");
        host.setTarget(jean);
        check(host.getTarget() == skeleton, "and never takes her for a target");

        f.everyTick(40, tick -> {
            PowerTicker.tick(jean);
            if (tick < 30) {
                return false;
            }
            check(!host.hasEffect(ModEffects.POSSESSED), "the effect ends with its time");
            check(PhoenixAbilities.hostsOf(jean).isEmpty(), "the zombie is nobody's again");
            // (Freed, it is an ordinary husk: its own goals may well pick the nearest player. What she sent it after is forgotten.)
            check(host.getTarget() != skeleton, "and forgets what she sent it after: " + host.getTarget());
            check(!data.is(Live.CASTING), "and her casting pose is over");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void aHostGoesForWhatHurtOrWasHitByHerBeforeTheNearestHostile(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 5.5, 10.5);
        Zombie host = zombie(helper, 7.5, 10.5);
        Skeleton nearest = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(9.5, FLOOR, 10.5));
        Spider hit = helper.spawnWithNoFreeWill(EntityType.SPIDER, new Vec3(12.5, FLOOR, 12.5));
        Creeper hurter = helper.spawnWithNoFreeWill(EntityType.CREEPER, new Vec3(14.5, FLOOR, 6.5));
        Pig bystander = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, FLOOR, 6.5));
        lookAt(jean, host);
        press(jean, 4);
        check(host.hasEffect(ModEffects.POSSESSED), "possessed");
        check(host.getTarget() == nearest, "with nothing else to go for, the nearest hostile: " + host.getTarget());
        // A boss is never sent for, however near it stands.
        WitherBoss boss = helper.spawnWithNoFreeWill(EntityType.WITHER, new Vec3(7.5, FLOOR, 11.5));
        PhoenixAbilities.steer(host);
        check(host.getTarget() == nearest, "never a boss, though it stands nearer: " + host.getTarget());
        boss.discard();

        jean.setLastHurtMob(hit);
        PhoenixAbilities.steer(host);
        check(host.getTarget() == hit, "the creature she last hit comes first: " + host.getTarget());

        jean.setLastHurtByMob(hurter);
        PhoenixAbilities.steer(host);
        check(host.getTarget() == hurter, "and the one that last hurt her before that: " + host.getTarget());

        // Never her pets, her hosts or the passive.
        Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(8.5, FLOOR, 8.5));
        pet.tame(jean);
        jean.setLastHurtMob(pet);
        jean.setLastHurtByMob(null);
        PhoenixAbilities.steer(host);
        check(host.getTarget() == nearest, "not her pet, not a pig, not her: the nearest hostile again: " + host.getTarget());

        // A passive creature is a harmless puppet that follows her.
        rested(jean, 4);
        lookAt(jean, bystander);
        press(jean, 4);
        check(bystander.hasEffect(ModEffects.POSSESSED), "a pig can be taken too");
        check(!host.hasEffect(ModEffects.POSSESSED) && host.hasEffect(ModEffects.CONFUSED), "(and the zombie is freed, since she holds one)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void aSecondHostFreesTheFirstConfusedUnlessSheMayHoldTwo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        Zombie first = zombie(helper, 9.5, 8.5);
        Zombie second = zombie(helper, 9.5, 12.5);
        first.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100));
        lookAt(jean, first);
        press(jean, 4);
        check(first.hasEffect(ModEffects.POSSESSED), "the first is taken");
        check(!first.hasEffect(ModEffects.SPELLBOUND), "taking a creature over clears the spell that held it");

        rested(jean, 4);
        lookAt(jean, second);
        press(jean, 4);
        check(second.hasEffect(ModEffects.POSSESSED) && !first.hasEffect(ModEffects.POSSESSED), "a second cast frees the first");
        check(first.hasEffect(ModEffects.CONFUSED) && first.getEffect(ModEffects.CONFUSED).getDuration() <= 40, "which wakes confused for 2 seconds");
        check(PhoenixAbilities.hostsOf(jean).equals(List.of(second)), "she holds only the second");

        f.config(PhoenixConfig.MIND_CONTROL_MAX, 2);
        first.removeEffect(ModEffects.CONFUSED);
        rested(jean, 4);
        lookAt(jean, first);
        press(jean, 4);
        check(first.hasEffect(ModEffects.POSSESSED) && second.hasEffect(ModEffects.POSSESSED), "with two allowed, both stay");
        check(PhoenixAbilities.hostsOf(jean).equals(List.of(second, first)), "oldest first");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void mindControlRespectsRangeLineOfSightAndImmunity(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 10.5, 10.5);
        // Out of reach: 12 blocks.
        Zombie far = zombie(helper, 10.5 + 9.0, 10.5 + 8.0);
        lookAt(jean, far);
        actionBar(jean);
        press(jean, 4);
        check(!far.hasEffect(ModEffects.POSSESSED) && actionBarKeys(jean).contains("message.enhanced.mind_control.nothing"), "12 blocks is out of reach");
        far.discard();

        // Behind a wall.
        Zombie hidden = zombie(helper, 16.5, 10.5);
        helper.setBlock(new BlockPos(13, 2, 10), Blocks.STONE);
        helper.setBlock(new BlockPos(13, 3, 10), Blocks.STONE);
        lookAt(jean, hidden);
        press(jean, 4);
        check(!hidden.hasEffect(ModEffects.POSSESSED) && actionBarKeys(jean).contains("message.enhanced.mind_control.nothing"), "nor behind a wall");
        hidden.discard();

        ServerPlayer creative = f.player(10.5, FLOOR, 5.5, GameType.CREATIVE);
        ServerPlayer magneto = f.player(14.5, FLOOR, 6.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        ServerPlayer loki = f.player(1.5, FLOOR, 1.5, Power.LOKI);
        LokiIllusionEntity copy = LokiIllusionEntity.conjure(loki, helper.absoluteVec(new Vec3(14.5, FLOOR, 13.5)), null, LokiIllusionEntity.Mode.DECOY, 200);
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(6.5, FLOOR, 14.5));
        for (LivingEntity closed : List.of(creative, magneto, copy, warden)) {
            lookAt(jean, closed);
            actionBar(jean);
            press(jean, 4);
            check(!closed.hasEffect(ModEffects.POSSESSED) && !closed.hasEffect(ModEffects.SPELLBOUND), closed.getType().getDescription().getString() + " is not taken");
            check(actionBarKeys(jean).contains("message.enhanced.mind_control.cannot"), "and Jean is told that mind is closed to her");
            check(Powers.get(jean).isReady(4, helper.getLevel().getGameTime()), "a refused cast costs her nothing");
        }
        // The effect itself is refused by the Warden whoever gives it (a command, a potion), not only by Mind Control.
        check(!warden.addEffect(new MobEffectInstance(ModEffects.POSSESSED, 100)) && !warden.hasEffect(ModEffects.POSSESSED), "the Warden refuses the possessed effect from anyone");

        // A creature Mjolnir holds down.
        ServerPlayer thor = f.player(1.5, FLOOR, 18.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(thor);
        Zombie pinned = zombie(helper, 14.5, 8.5);
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, pinned);
        check(MjolnirPin.isPinned(pinned), "a zombie is pinned");
        lookAt(jean, pinned);
        actionBar(jean);
        press(jean, 4);
        check(!pinned.hasEffect(ModEffects.POSSESSED) && actionBarKeys(jean).contains("message.enhanced.mind_control.pinned"), "a pinned creature cannot be taken");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PVP)
    public static void mindControlOnAPlayerOnlyWithPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        ServerPlayer other = f.player(9.5, FLOOR, 10.5);
        ServerPlayer creative = f.player(9.5, FLOOR, 14.5, GameType.CREATIVE);
        PowerData data = Powers.get(jean);
        lookAt(jean, other);

        f.pvp(false);
        actionBar(jean);
        press(jean, 4);
        check(!other.hasEffect(ModEffects.SPELLBOUND) && actionBarKeys(jean).contains("message.enhanced.mind_control.no_pvp"),
                "PvP off: you can't reach their mind");
        check(data.isReady(4, helper.getLevel().getGameTime()), "and it costs nothing");

        f.pvp(true);
        press(jean, 4);
        check(other.hasEffect(ModEffects.SPELLBOUND) && other.getEffect(ModEffects.SPELLBOUND).getDuration() == 60, "PvP on: they are held for 3 seconds");
        check(!other.hasEffect(ModEffects.POSSESSED) && PhoenixAbilities.hostsOf(jean).isEmpty(), "(a player is held, not driven)");
        check(!data.isReady(4, helper.getLevel().getGameTime()), "and she must rest");

        rested(jean, 4);
        lookAt(jean, creative);
        press(jean, 4);
        check(!creative.hasEffect(ModEffects.SPELLBOUND), "a Creative player is unaffected");

        other.removeEffect(ModEffects.SPELLBOUND);
        rested(jean, 4);
        f.config(PhoenixConfig.MIND_CONTROL_PLAYER_TICKS, 0);
        lookAt(jean, other);
        press(jean, 4);
        check(!other.hasEffect(ModEffects.SPELLBOUND), "with the time set to 0, players are out of reach for good");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void aPossessedCreatureEndsWithItsOwner(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer dies = jean(f, 3.5, 3.5);
        ServerPlayer leaves = jean(f, 3.5, 7.5);
        ServerPlayer changes = jean(f, 3.5, 11.5);
        ServerPlayer loses = jean(f, 3.5, 15.5);
        ServerPlayer lives = jean(f, 3.5, 19.5);
        List<Mob> hosts = new ArrayList<>();
        double z = 3.5;
        for (ServerPlayer owner : List.of(dies, leaves, changes, loses, lives)) {
            Zombie host = zombie(helper, 8.5, z);
            lookAt(owner, host);
            press(owner, 4);
            check(host.hasEffect(ModEffects.POSSESSED), "taken");
            hosts.add(host);
            z += 4.0;
        }
        dies.hurt(helper.getLevel().damageSources().genericKill(), 1000.0F);
        check(!dies.isAlive() && !hosts.get(0).hasEffect(ModEffects.POSSESSED), "the host is freed when the Phoenix dies");
        HeroLifecycle.onLogout(leaves);
        check(!hosts.get(1).hasEffect(ModEffects.POSSESSED), "when she logs out");
        HeroLifecycle.onChangeDimension(changes);
        check(!hosts.get(2).hasEffect(ModEffects.POSSESSED), "when she changes dimension");
        Powers.setPower(loses, Power.NONE);
        check(!hosts.get(3).hasEffect(ModEffects.POSSESSED), "when she loses the power");
        check(hosts.get(4).hasEffect(ModEffects.POSSESSED) && PhoenixAbilities.hostsOf(lives).equals(List.of(hosts.get(4))), "(the one who stays keeps hers)");

        hosts.get(4).kill();
        check(PhoenixAbilities.hostsOf(lives).isEmpty(), "and a host that dies leaves her list");
        rested(lives, 4);
        Zombie another = zombie(helper, 8.5, 19.5);
        lookAt(lives, another);
        press(lives, 4);
        check(another.hasEffect(ModEffects.POSSESSED), "so that she can take the next");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK, timeoutTicks = 40)
    public static void aPossessedCreatureWhoseOwnerIsGoneWakesUp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Zombie orphan = zombie(helper, 10.5, 10.5);
        // What a saved world holds after a restart: the effect, and nobody who took it.
        orphan.addEffect(new MobEffectInstance(ModEffects.POSSESSED, 200, 0, false, true, true));
        check(!PhoenixAbilities.steer(orphan), "nobody holds it, so steering says the effect should end");
        orphan.tickCount = 20;
        check(!ModEffects.POSSESSED.get().applyEffectTick(orphan, 0), "and the effect's own tick ends it");
        f.everyTick(15, tick -> !orphan.hasEffect(ModEffects.POSSESSED));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Psionic Field

    @GameTest(template = ARENA, batch = PVP)
    public static void psionicFieldHoldsEveryoneInTheSphere(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(PhoenixConfig.PSIONIC_RADIUS, 8);
        f.config(PhoenixConfig.PSIONIC_TICKS, 40);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 10.5, 10.5);
        PowerData data = Powers.get(jean);

        Zombie held = zombie(helper, 14.5, 10.5);
        Zombie behindWall = zombie(helper, 10.5, 15.5);
        helper.setBlock(new BlockPos(10, 2, 13), Blocks.STONE);
        helper.setBlock(new BlockPos(10, 3, 13), Blocks.STONE);
        Zombie farOut = zombie(helper, 17.0, 17.0);
        Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(7.5, FLOOR, 10.5));
        pet.tame(jean);
        Zombie host = zombie(helper, 10.5, 6.5);
        lookAt(jean, host);
        press(jean, 4);
        check(host.hasEffect(ModEffects.POSSESSED), "(one zombie is her host)");
        rested(jean, 4);
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(6.5, FLOOR, 6.5));
        Skeleton mate = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(13.5, FLOOR, 6.5));
        ServerPlayer teammate = f.player(7.5, FLOOR, 13.5);
        ServerPlayer stranger = f.player(13.5, FLOOR, 13.5);
        ServerPlayer creative = f.player(6.5, FLOOR, 14.5, GameType.CREATIVE);
        ServerPlayer magneto = f.player(14.5, FLOOR, 14.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        Scoreboard scoreboard = level.getServer().getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("phoenix_test_" + UUID.randomUUID().toString().substring(0, 8));
        scoreboard.addPlayerToTeam(jean.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(teammate.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(mate.getScoreboardName(), team);
        f.onClose(() -> scoreboard.removePlayerTeam(team));
        List<LivingEntity> touched = new ArrayList<>();
        PhoenixPower.Listener listener = (phoenix, target, kind, amount) -> {
            if (phoenix == jean && kind == PhoenixPower.Kind.PSIONIC_FIELD) {
                touched.add(target);
            }
        };
        PhoenixPower.addListener(listener);
        f.onClose(() -> PhoenixPower.removeListener(listener));
        f.pvp(false);
        float exhaustion = jean.getFoodData().getExhaustionLevel();
        long now = level.getGameTime();
        actionBar(jean);

        sneakPress(jean, 4);
        check(held.hasEffect(ModEffects.SPELLBOUND) && held.getEffect(ModEffects.SPELLBOUND).getDuration() == 40, "a zombie in the sphere is held for the time the server says");
        check(behindWall.hasEffect(ModEffects.SPELLBOUND), "even through a wall (it is a field of the mind)");
        check(!farOut.hasEffect(ModEffects.SPELLBOUND), "but not one beyond the radius (in a corner of the cube that holds the sphere)");
        check(!pet.hasEffect(ModEffects.SPELLBOUND) && !host.hasEffect(ModEffects.SPELLBOUND) && !mate.hasEffect(ModEffects.SPELLBOUND),
                "not her pet, her host or her team");
        check(!warden.hasEffect(ModEffects.SPELLBOUND), "not the Warden, whose mind is too vast");
        check(!stranger.hasEffect(ModEffects.SPELLBOUND) && !teammate.hasEffect(ModEffects.SPELLBOUND), "with PvP off no player is held");
        check(!creative.hasEffect(ModEffects.SPELLBOUND) && !magneto.hasEffect(ModEffects.SPELLBOUND) && !jean.hasEffect(ModEffects.SPELLBOUND),
                "never a Creative player, a helmeted Magneto or herself");
        check(data.is(Live.PHOENIX_PSIONIC), "the arms-out pose is on");
        check(data.cooldownUntil(4) == now + 1200, "and ability 4 rests for 60 seconds");
        assertNear(jean.getFoodData().getExhaustionLevel() - exhaustion, 8.0, 1.0E-4, "at 8.0 of exhaustion");
        check(touched.contains(warden) && touched.contains(held) && touched.contains(stranger) && !touched.contains(jean) && !touched.contains(creative),
                "the listeners hear of every mind the field reached, held or not");
        press(jean, 4);
        check(!PhoenixAbilities.hostsOf(jean).isEmpty() && data.cooldownUntil(4) == now + 1200, "Mind Control shares the slot: it is still resting");

        // With PvP on the players are held too, but not her teammate.
        held.removeEffect(ModEffects.SPELLBOUND);
        rested(jean, 4);
        f.pvp(true);
        sneakPress(jean, 4);
        check(stranger.hasEffect(ModEffects.SPELLBOUND) && !teammate.hasEffect(ModEffects.SPELLBOUND), "with PvP on a stranger is held, a teammate is not");
        check(!creative.hasEffect(ModEffects.SPELLBOUND) && !magneto.hasEffect(ModEffects.SPELLBOUND), "never Creative or the helmet");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void anEmptyFieldCostsNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(PhoenixConfig.PSIONIC_RADIUS, 8);
        ServerPlayer jean = jean(f, 10.5, 10.5);
        PowerData data = Powers.get(jean);
        float exhaustion = jean.getFoodData().getExhaustionLevel();
        actionBar(jean);
        sneakPress(jean, 4);
        check(actionBarKeys(jean).contains("message.enhanced.psionic_field.nothing"), "Nobody in your field");
        check(data.isReady(4, helper.getLevel().getGameTime()) && !data.is(Live.PHOENIX_PSIONIC), "no cooldown, no pose");
        assertNear(jean.getFoodData().getExhaustionLevel() - exhaustion, 0.0, 1.0E-6, "and no cost");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The API for the Apocalypse lane

    @GameTest(template = ARENA, batch = QUICK)
    public static void phoenixMindPowerIsTheBurstAndHerSlams(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 4.5, 10.5);
        ServerPlayer wanda = f.player(8.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer plain = f.player(12.5, FLOOR, 10.5);
        Zombie zombie = zombie(helper, 16.5, 10.5);
        check(PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, jean, jean)), "the Burst of a Phoenix is her mind power");
        check(PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, jean, zombie)), "so is the slam of what she threw");
        check(!PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, wanda, zombie)), "the same slam by Wanda is not hers");
        check(!PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, plain, zombie)), "nor by a plain player");
        check(!PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, plain, plain)), "nor a stray Burst");

        List<String> told = new ArrayList<>();
        PhoenixPower.Listener listener = (phoenix, target, kind, amount) -> {
            if (phoenix == jean) {
                told.add(kind + ":" + target.getId() + ":" + amount);
            }
        };
        PhoenixPower.addListener(listener);
        f.onClose(() -> PhoenixPower.removeListener(listener));
        f.config(PhoenixConfig.PSIONIC_RADIUS, 8);
        lookAt(jean, wanda);
        press(jean, 4);
        sneakPress(jean, 4);
        check(told.contains(PhoenixPower.Kind.MIND_CONTROL + ":" + wanda.getId() + ":0.0"), "Mind Control is reported with no damage: " + told);
        check(told.contains(PhoenixPower.Kind.PSIONIC_FIELD + ":" + wanda.getId() + ":0.0"), "so is the Psionic Field: " + told);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void phoenixListenerSeesRefusedCasts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(PhoenixConfig.PSIONIC_RADIUS, 8);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(10.5, FLOOR, 10.5));
        List<String> told = new ArrayList<>();
        PhoenixPower.Listener listener = (phoenix, target, kind, amount) -> {
            if (phoenix == jean && target == warden) {
                told.add(kind.name());
            }
        };
        PhoenixPower.addListener(listener);
        f.onClose(() -> PhoenixPower.removeListener(listener));

        lookAt(jean, warden);
        actionBar(jean);
        press(jean, 4);
        check(!warden.hasEffect(ModEffects.POSSESSED) && actionBarKeys(jean).contains("message.enhanced.mind_control.cannot"), "Mind Control on a boss-sized mind is refused");
        check(told.equals(List.of("MIND_CONTROL")), "and still heard once, before the refusal: " + told);

        sneakPress(jean, 4);
        check(!warden.hasEffect(ModEffects.SPELLBOUND), "the Psionic Field does not hold the Warden");
        check(told.equals(List.of("MIND_CONTROL", "PSIONIC_FIELD")), "and still reports itself once: " + told);

        lookAt(jean, warden);
        MindReading.look(jean, 5000);
        check(told.equals(List.of("MIND_CONTROL", "PSIONIC_FIELD", "MIND_READ")), "a glance at it is reported too, though the mind is too vast to read: " + told);
        MindReading.look(jean, 5010);
        check(told.size() == 3, "and only the first time she turns to it: " + told);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Her body

    @GameTest(template = ARENA, batch = QUICK)
    public static void fireNeverHurtsThePhoenix(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer jean = jean(f, 10.5, 10.5);
        float health = jean.getHealth();
        for (DamageSource fire : List.of(level.damageSources().inFire(), level.damageSources().onFire(), level.damageSources().lava(),
                level.damageSources().hotFloor())) {
            check(!jean.hurt(fire, 4.0F) && jean.getHealth() == health, fire.getMsgId() + " does nothing to her");
        }
        jean.igniteForSeconds(8.0F);
        check(jean.getRemainingFireTicks() > 0, "(set alight)");
        PowerTicker.tick(jean);
        check(jean.getRemainingFireTicks() == 0, "the fire goes out at once");

        jean.invulnerableTime = 0;
        check(jean.hurt(level.damageSources().genericKill(), 3.0F) && jean.getHealth() < health, "damage nothing can stop still hurts her");
        jean.setHealth(health);
        jean.invulnerableTime = 0;
        f.config(PhoenixConfig.FIRE_IMMUNE, false);
        check(jean.hurt(level.damageSources().inFire(), 4.0F) && jean.getHealth() < health, "with the option off, fire hurts her like anyone");

        ServerPlayer other = f.player(4.5, FLOOR, 4.5, Power.MAGNETO);
        check(other.hurt(level.damageSources().inFire(), 2.0F), "(and fire still hurts the others)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void aSpectatorPhoenixUsesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = f.player(10.5, FLOOR, 10.5, GameType.SPECTATOR);
        Powers.setPower(jean, Power.PHOENIX);
        PowerData data = Powers.get(jean);
        for (int ability : new int[]{1, 3, 4, 5}) {
            press(jean, ability);
            sneakPress(jean, ability);
        }
        check(!data.toggle(Toggle.MIND_READING) && !data.is(Live.PHOENIX_CHARGING) && !data.is(Live.MIND_SENSING) && !data.is(Live.PHOENIX_PSIONIC),
                "a spectator reads, charges, senses and casts nothing");
        check(data.isReady(1, 0) && data.isReady(4, 0) && data.isReady(5, 0) && !Telekinesis.isHolding(jean), "and nothing rests");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void losingThePowerEndsEverythingSheLeft(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        PowerData data = Powers.get(jean);
        Zombie host = zombie(helper, 9.5, 10.5);
        lookAt(jean, host);
        press(jean, 4);
        press(jean, 1);
        sneakPress(jean, 1);
        AbilityHandler.onKey(jean, 5, true);
        PowerTicker.tick(jean);
        check(host.hasEffect(ModEffects.POSSESSED) && data.toggle(Toggle.MIND_READING) && data.is(Live.MIND_SENSING) && data.is(Live.PHOENIX_CHARGING)
                && data.is(Live.PHOENIX_BLAZE) && data.is(Live.CASTING), "she is busy with everything");
        check(jean.getAttribute(Attributes.MOVEMENT_SPEED)
                .hasModifier(Enhanced.id("phoenix_charge_slow")), "the charge slows her");
        Powers.setPower(jean, Power.NONE);
        check(!host.hasEffect(ModEffects.POSSESSED), "her host is free");
        for (Live live : Live.values()) {
            check(!data.is(live), "no state is left: " + live);
        }
        check(!data.toggle(Toggle.MIND_READING), "and Mind Reading is off");
        check(!jean.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Enhanced.id("phoenix_charge_slow")),
                "her speed is her own again");
        check(!jean.mayFly() && !jean.getAbilities().flying, "and she cannot fly");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void aDeadPhoenixLetsGoOfTheChargeAndEverythingElse(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        PowerData data = Powers.get(jean);
        AbilityHandler.onKey(jean, 5, true);
        PowerTicker.tick(jean);
        check(data.is(Live.PHOENIX_CHARGING) && data.is(Live.PHOENIX_BLAZE), "she is charging");
        jean.hurt(helper.getLevel().damageSources().genericKill(), 1000.0F);
        check(!jean.isAlive(), "she died");
        data.set(Live.PHOENIX_CHARGING, true);
        data.set(Live.PHOENIX_BLAZE, true);
        PowerTicker.tick(jean);
        check(!data.is(Live.PHOENIX_CHARGING) && !data.is(Live.PHOENIX_BLAZE), "the dead body charges nothing and burns with nothing");
        AbilityHandler.onKey(jean, 5, false);
        check(Powers.is(jean, Power.PHOENIX), "(she is still the Phoenix for when she is back)");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The older heroes and the rules

    @GameTest(template = ARENA, batch = PVP)
    public static void mjolnirPinStopsHerFlightButNotHerMind(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer jean = jean(f, 10.5, 10.5);
        ServerPlayer thor = f.player(4.5, FLOOR, 10.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(thor);
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, jean);
        check(MjolnirPin.isPinned(jean), "Jean is pinned");
        PowerData data = Powers.get(jean);

        press(jean, 2);
        check(!jean.getAbilities().flying, "a pinned Phoenix cannot fly");
        press(jean, 1);
        check(data.toggle(Toggle.MIND_READING), "but she can read minds");
        Zombie zombie = zombie(helper, 14.5, 10.5);
        lookAt(jean, zombie);
        check(MindReading.target(jean) == zombie, "and see who is there");
        press(jean, 4);
        check(zombie.hasEffect(ModEffects.POSSESSED), "and take them over");
        AbilityHandler.onKey(jean, 5, true);
        PowerTicker.tick(jean);
        check(data.is(Live.PHOENIX_CHARGING) && !jean.getAbilities().flying, "the charge works on the ground, in place");
        AbilityHandler.onKey(jean, 5, false);

        Skeleton pinnedMob = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(14.5, FLOOR, 14.5));
        ServerPlayer thor2 = f.player(2.5, FLOOR, 2.5, Power.THOR);
        thor2.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(thor2);
        MjolnirPin.pin(thor2, InteractionHand.MAIN_HAND, pinnedMob);
        check(MjolnirPin.isPinned(pinnedMob) && !Telekinesis.canLift(jean, pinnedMob, Telekinesis.Mode.JEAN), "and a pinned creature cannot be lifted by her");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "phoenix_hex")
    public static void hexWallBouncesThePhoenix(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        ServerPlayer wanda = HexTests.raiseHex(f);
        ServerPlayer jean = f.player(10.5, FLOOR, 2.0, Power.PHOENIX);
        ServerPlayer allowed = f.player(2.0, FLOOR, 10.5, Power.PHOENIX);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(allowed.getUUID()), 1);
        check(!HexTests.inside(f, wanda, jean), "Jean starts outside");

        f.everyTick(10, tick -> {
            if (tick == 2) {
                HexTests.walkTo(jean, HexTests.abs(helper, 10.5, FLOOR, 2.6));
                HexTests.walkTo(allowed, HexTests.abs(helper, 2.6, FLOOR, 10.5));
                check(HexTests.inside(f, wanda, jean) && HexTests.inside(f, wanda, allowed), "both step through the wall");
            }
            if (tick >= 4) {
                check(jean.isAlive() && Powers.is(jean, Power.PHOENIX), "the wall does not kill a hero");
                check(!HexTests.inside(f, wanda, jean), "Jean is bounced back out (tick " + tick + ")");
                check(allowed.isAlive() && HexTests.inside(f, wanda, allowed), "the one Wanda allowed passes");
                return tick >= 6;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "phoenix_helmet")
    public static void magnetoHelmetBlocksTelepathyButNotTelekinesis(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(PhoenixConfig.PSIONIC_RADIUS, 8);
        f.pvp(true);
        ServerPlayer jean = jean(f, 4.5, 10.5);
        ServerPlayer magneto = f.player(9.5, FLOOR, 10.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        lookAt(jean, magneto);

        check(MindReading.target(jean) == magneto && read(jean, magneto).state() == MindReadPayload.BLOCKED, "his mind is shielded from her reading");
        press(jean, 4);
        check(!magneto.hasEffect(ModEffects.SPELLBOUND) && Powers.get(jean).isReady(4, helper.getLevel().getGameTime()), "Mind Control cannot hold him");
        sneakPress(jean, 4);
        check(!magneto.hasEffect(ModEffects.SPELLBOUND), "nor the Psionic Field");
        check(MindReading.sense(jean, 8.0).silences() == 1 && MindReading.sense(jean, 8.0).minds().isEmpty(), "Mind Sense finds a silence where he is");
        press(jean, 3);
        check(Telekinesis.isHolding(jean) && Powers.get(jean).heldEntityId() == magneto.getId(), "but her telekinesis lifts him");
        helper.succeed();
    }

    private PhoenixTests() {
    }
}

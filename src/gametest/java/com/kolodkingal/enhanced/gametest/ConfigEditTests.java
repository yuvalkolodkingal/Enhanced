package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.config.core.ConfigEditPayload;
import com.kolodkingal.enhanced.config.core.ConfigEdits;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.config.core.ConfigValuePayload;
import com.kolodkingal.enhanced.config.core.Configs;
import com.kolodkingal.enhanced.config.core.Toml;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The server's settings changed from inside the game (3.17): {@code /enhanced config} and the packet an operator's
 * settings screen sends. One test, in a batch of its own: it changes the settings this very server runs on, one after
 * the other, and puts each back before it ends.
 */
public final class ConfigEditTests {
    private static final String BATCH = "config_edit";
    private static final String BUDGET = "custom_hero.budget";

    private ConfigEditTests() {
    }

    private static String file() {
        Path path = Configs.of(EnhancedConfig.SPEC).path();
        check(path != null, "the server's config has a file");
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /** Whether the file's [custom_hero] section says {@code budget = value}. */
    private static boolean fileSaysBudget(int value) {
        String text = file();
        int section = text.indexOf("[custom_hero]");
        check(section >= 0, "the file has the section");
        int next = text.indexOf("\n[", section + 1);
        String body = next < 0 ? text.substring(section) : text.substring(section, next);
        return body.matches("(?s).*\\n\\s*budget = " + value + "\\s*\\n.*");
    }

    /**
     * Writes the file as it was, by the config's own save (so the watcher does not read it again in the middle of other
     * tests): every value the file had otherwise is put in use for the moment of the save, then what was in use comes back.
     */
    @SuppressWarnings("unchecked")
    private static void restoreFile(String fileBefore) throws Exception {
        Toml.Table was = Toml.parse(fileBefore);
        List<Runnable> back = new java.util.ArrayList<>();
        for (ConfigSpec.ConfigValue<?> value : EnhancedConfig.SPEC.values()) {
            Object inUse = value.getRaw();
            Object inFile = was.get(value.getPath());
            if (inFile == null || ConfigEdits.text(inFile).equals(ConfigEdits.text(inUse))) {
                continue;
            }
            ((ConfigSpec.ConfigValue<Object>) value).set(ConfigEdits.parse(value, ConfigEdits.text(inFile)));
            back.add(() -> ((ConfigSpec.ConfigValue<Object>) value).set(inUse));
        }
        EnhancedConfig.SPEC.save();
        back.forEach(Runnable::run);
        EnhancedConfig.SPEC.afterReload();
    }

    private static int run(CommandSourceStack source, String command) throws CommandSyntaxException {
        return source.getServer().getCommands().getDispatcher().execute(command, source);
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anOperatorChangesTheServersSettingsInGame(GameTestHelper helper) throws Exception {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = f.player(10.5, FLOOR, 10.5, GameType.SURVIVAL);
        CommandSourceStack operator = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        CommandSourceStack nobody = player.createCommandSourceStack().withPermission(0).withSuppressedOutput();
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();

        // (the file as it is before the test: a change writes the whole file, with what the test fixture holds changed
        // in memory while a test runs, so the test puts the file itself back as well)
        String fileBefore = file();
        int before = CustomHeroConfig.BUDGET.get();
        int def = CustomHeroConfig.BUDGET.getDefault();
        int other = before == 77 ? 78 : 77;
        ConfigSpec.ConfigValue<?> found = ConfigEdits.find(BUDGET);
        check(found == CustomHeroConfig.BUDGET, "a setting's key is its sections and name joined by dots");
        try {
            // ---- every key is one setting, and Tab offers them ----
            List<String> keys = ConfigEdits.keys();
            Set<String> distinct = new HashSet<>(keys);
            check(distinct.size() == keys.size() && keys.size() > 100, "every setting has a key of its own: " + keys.size());
            for (String key : keys) {
                check(ConfigEdits.find(key) != null && !key.contains(" "), "the key finds its setting: " + key);
            }
            Set<String> offered = new HashSet<>();
            for (Suggestion s : dispatcher.getCompletionSuggestions(dispatcher.parse("enhanced config set custom_hero.b", operator)).get().getList()) {
                offered.add(s.getText());
            }
            check(offered.contains(BUDGET), "Tab completes the keys: " + offered);
            offered.clear();
            for (Suggestion s : dispatcher.getCompletionSuggestions(dispatcher.parse("enhanced config get ", operator)).get().getList()) {
                offered.add(s.getText());
            }
            check(offered.containsAll(distinct), "of every setting, " + offered.size() + " of " + distinct.size());

            // ---- set, get, reset ----
            check(run(operator, "enhanced config get " + BUDGET) == 1, "get answers");
            check(run(operator, "enhanced config set " + BUDGET + " " + other) == 1, "set is accepted");
            check(CustomHeroConfig.BUDGET.get() == other, "the value is in use at once");
            check(CustomHeroRules.Limits.current().budget() == other, "and the game plays by it: the heroes' budget is " + other);
            check(fileSaysBudget(other), "the config file was written again with it");
            check(run(operator, "enhanced config list") > 5, "list names the sections");
            check(run(operator, "enhanced config list custom_hero") > 3, "and a section's settings");
            check(run(operator, "enhanced config list no_such_section") == 0, "a section that is not there is said so");

            // ---- a value the config's rules refuse changes nothing ----
            ConfigSpec.Range<?> range = CustomHeroConfig.BUDGET.getSpec().getRange();
            check(range != null, "the budget has a range");
            for (String bad : List.of("999999999", "-5", "many", "1.5", "")) {
                int result = bad.isEmpty() ? 0 : run(operator, "enhanced config set " + BUDGET + " " + bad);
                if (bad.isEmpty()) {
                    boolean threw = false;
                    try {
                        ConfigEdits.change(BUDGET, bad);
                    } catch (ConfigEdits.Refused refused) {
                        threw = true;
                    }
                    check(threw, "an empty value is refused");
                }
                check(result == 0 && CustomHeroConfig.BUDGET.get() == other && fileSaysBudget(other),
                        "\"" + bad + "\" is refused and nothing changes");
            }
            check(run(operator, "enhanced config set no.such.setting 1") == 0, "a setting that is not there is refused");
            check(run(operator, "enhanced config get no.such.setting") == 0, "to read as well");

            // ---- other kinds of value: a switch, a choice ----
            boolean swirl = EnhancedConfig.HEX_VISION_ENABLED.get();
            String swirlKey = ConfigEdits.key(EnhancedConfig.HEX_VISION_ENABLED);
            check(run(operator, "enhanced config set " + swirlKey + " " + !swirl) == 1 && EnhancedConfig.HEX_VISION_ENABLED.get() == !swirl,
                    "a switch is set by true and false");
            check(run(operator, "enhanced config set " + swirlKey + " maybe") == 0 && EnhancedConfig.HEX_VISION_ENABLED.get() == !swirl,
                    "and by nothing else");
            check(run(operator, "enhanced config reset " + swirlKey) == 1
                    && EnhancedConfig.HEX_VISION_ENABLED.get() == EnhancedConfig.HEX_VISION_ENABLED.getDefault(), "reset puts the default back");
            ConfigEdits.change(swirlKey, String.valueOf(swirl));
            Object containers = BuildConfig.FULL_CONTAINERS.get();
            String containersKey = ConfigEdits.key(BuildConfig.FULL_CONTAINERS);
            for (Object choice : BuildConfig.FULL_CONTAINERS.get().getDeclaringClass().getEnumConstants()) {
                check(run(operator, "enhanced config set " + containersKey + " " + choice.toString().toLowerCase(java.util.Locale.ROOT)) == 1
                        && BuildConfig.FULL_CONTAINERS.get() == choice, "a choice is set by its name: " + choice);
            }
            check(run(operator, "enhanced config set " + containersKey + " NO_SUCH_CHOICE") == 0, "and by no other name");
            ConfigEdits.change(containersKey, ConfigEdits.text(containers));
            check(BuildConfig.FULL_CONTAINERS.get() == containers, "the choice is as it was");

            // ---- every setting's own value is one it accepts (so the screen and the command can always put it back) ----
            for (ConfigSpec.ConfigValue<?> value : ConfigEdits.values()) {
                Object now = value.getRaw();
                Object read = ConfigEdits.parse(value, ConfigEdits.text(now));
                check(java.util.Objects.equals(read, now), ConfigEdits.key(value) + " reads its own value back: " + now + " -> " + read);
                Object readDefault = ConfigEdits.parse(value, ConfigEdits.text(value.getDefault()));
                check(java.util.Objects.equals(readDefault, value.getDefault()), ConfigEdits.key(value) + " reads its default back");
            }

            // ---- who is not an operator changes nothing: by command ----
            check(!dispatcher.getRoot().getChild("enhanced").getRequirement().test(nobody), "/enhanced is not for him");
            boolean threw = false;
            try {
                run(nobody, "enhanced config set " + BUDGET + " " + (other + 1));
            } catch (CommandSyntaxException e) {
                threw = true;
            }
            check(threw && CustomHeroConfig.BUDGET.get() == other, "the command is no command for who is not an operator");

            // ---- nor by the screen's packet: the server looks up who asks ----
            check(!ConfigEdits.mayEdit(player), "a player is no operator until made one");
            check(!ConfigEdits.fromClient(player, BUDGET, String.valueOf(other + 1), false)
                    && CustomHeroConfig.BUDGET.get() == other && fileSaysBudget(other), "his packet changes nothing");
            check(!ConfigEdits.fromClient(player, BUDGET, "", true) && CustomHeroConfig.BUDGET.get() == other, "nor his reset");
            // an operator's packet is checked like the command
            check(!ConfigEdits.fromClient(player, true, BUDGET, "999999999", false) && CustomHeroConfig.BUDGET.get() == other,
                    "an operator's bad value is refused by the server, whatever his screen allowed");
            check(ConfigEdits.fromClient(player, true, BUDGET, String.valueOf(other + 1), false)
                    && CustomHeroConfig.BUDGET.get() == other + 1 && fileSaysBudget(other + 1)
                    && CustomHeroRules.Limits.current().budget() == other + 1, "his good one is made, written and in use");
            check(ConfigEdits.fromClient(player, true, BUDGET, "", true) && CustomHeroConfig.BUDGET.get() == def && fileSaysBudget(def),
                    "and his reset puts the default back");

            // ---- the packets themselves ----
            ByteBuf buf = Unpooled.buffer();
            ConfigEditPayload.STREAM_CODEC.encode(buf, new ConfigEditPayload(BUDGET, "12", false));
            ConfigEditPayload edit = ConfigEditPayload.STREAM_CODEC.decode(buf);
            check(edit.key().equals(BUDGET) && edit.value().equals("12") && !edit.reset(), "the screen's packet arrives whole");
            ConfigValuePayload.STREAM_CODEC.encode(buf, new ConfigValuePayload(BUDGET, "12"));
            ConfigValuePayload told = ConfigValuePayload.STREAM_CODEC.decode(buf);
            check(told.key().equals(BUDGET) && told.value().equals("12"), "and the server's");
            buf.release();

            // ---- reset by command ----
            ConfigEdits.change(BUDGET, String.valueOf(other));
            check(run(operator, "enhanced config reset " + BUDGET) == 1 && CustomHeroConfig.BUDGET.get() == def && fileSaysBudget(def),
                    "reset puts the default back, in the file too");
        } finally {
            ConfigEdits.change(BUDGET, String.valueOf(before));
            restoreFile(fileBefore);
        }
        check(CustomHeroConfig.BUDGET.get() == before && fileSaysBudget(before), "the budget is as the test found it");
        check(file().equals(fileBefore), "and so is the whole file");
        helper.succeed();
    }
}

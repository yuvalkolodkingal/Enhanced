package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.config.core.Toml;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.ConfigCompatTests.BATCH;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The mod's TOML reader and writer (port P0, lane L7): the forms a player may write, read as NightConfig read them
 * (the comment rules, the number types), files that are not TOML refused, and tables, lists and comments written as
 * NightConfig wrote them.
 */
public final class ConfigTomlTests {
    private ConfigTomlTests() {
    }

    private static Toml.Table parse(String text) {
        try {
            return Toml.parse(text);
        } catch (Toml.ParseException e) {
            throw new IllegalStateException("should read: " + text.replace("\n", "\\n") + ": " + e.getMessage());
        }
    }

    private static void refused(String text) {
        try {
            Toml.parse(text);
        } catch (Toml.ParseException e) {
            return;
        }
        throw new IllegalStateException("should not read: " + text.replace("\n", "\\n"));
    }

    private static Toml.Table table(Toml.Table root, String name) {
        return (Toml.Table) root.get(name);
    }

    /** Numbers keep the type NightConfig gave them: int if it fits, else long; a float with '.', 'e' or inf/nan. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void numbersAreReadAsNightConfigReadThem(GameTestHelper helper) {
        Toml.Table t = parse("a = 32\nb = 3000000000\nc = 2.5\nd = 1e3\ne = -7\nf = +7\ng = 0x1F\nh = 0o17\n"
                + "i = 0b101\nj = 1_000\nk = inf\nl = -inf\nm = nan\nn = 6.02E23\no = -0.0\n");
        check(t.get("a").equals(32), "int " + t.get("a"));
        check(t.get("b").equals(3000000000L), "long " + t.get("b"));
        check(t.get("c").equals(2.5) && t.get("d").equals(1000.0), "floats");
        check(t.get("e").equals(-7) && t.get("f").equals(7), "signs");
        check(t.get("g").equals(31) && t.get("h").equals(15) && t.get("i").equals(5), "bases");
        check(t.get("j").equals(1000), "underscores");
        check(t.get("k").equals(Double.POSITIVE_INFINITY) && t.get("l").equals(Double.NEGATIVE_INFINITY)
                && ((Double) t.get("m")).isNaN(), "inf and nan");
        check(t.get("n").equals(6.02E23) && t.get("o").equals(-0.0), "exponent and negative zero");
        for (String bad : List.of("a = 012", "a = 1__0", "a = _1", "a = 1_", "a = 1.", "a = .5", "a = 1e",
                "a = 0x", "a = 12a", "a = +-1", "a = 1.2.3")) {
            refused(bad + "\n");
        }
        helper.succeed();
    }

    /** Strings: basic with escapes, literal, multi-line (the first newline dropped, line-ending backslashes). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void stringsAreReadWithTheirEscapes(GameTestHelper helper) {
        Toml.Table t = parse("a = \"x\\ty\\n\\\"q\\\" \\\\ \\u00e9 \\U0001F600\"\nb = 'C:\\path'\n"
                + "c = \"\"\"\nline one\nline two\"\"\"\nd = '''\nraw \\n'''\ne = \"\"\"a \\\n    b\"\"\"\n"
                + "f = \"\"\"x\"\"\"\"\n");
        check(t.get("a").equals("x\ty\n\"q\" \\ \u00e9 \uD83D\uDE00"), "escapes: " + t.get("a"));
        check(t.get("b").equals("C:\\path"), "literal: " + t.get("b"));
        check(t.get("c").equals("line one\nline two"), "multi-line: " + t.get("c"));
        check(t.get("d").equals("raw \\n"), "multi-line literal: " + t.get("d"));
        check(t.get("e").equals("a b"), "line-ending backslash: " + t.get("e"));
        check(t.get("f").equals("x\""), "a quote before the end: " + t.get("f"));
        for (String bad : List.of("a = \"open\n", "a = 'open\n", "a = \"\\q\"\n", "a = \"\\u12\"\n",
                "a = \"\"\"never closed\n")) {
            refused(bad);
        }
        helper.succeed();
    }

    /** Comments belong to what follows them, then to the end of their own line; the last ones of a file are lost. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void commentsBelongToTheEntryBelowThem(GameTestHelper helper) {
        Toml.Table t = parse("#top\n# second\n[s]\n\t#one\n\ta = 1 #end\n\tb = 2\n#before t\n[t] #on t\n\tc = 3\n"
                + "#lost\n");
        check("top\n second".equals(t.comment("s")), "the first table's: " + t.comment("s"));
        check("one\nend".equals(table(t, "s").comment("a")), "a's: " + table(t, "s").comment("a"));
        check(table(t, "s").comment("b") == null, "b has none");
        check("before t\non t".equals(t.comment("t")), "t's: " + t.comment("t"));
        check(table(t, "t").comment("c") == null, "c has none (the comment after it is lost)");
        Toml.Table noNewline = parse("[s]\n#c\na = 1");
        check(table(noNewline, "s").comment("a") == null, "the last entry of a file with no end of line keeps none");
        refused("[s]\na = 1 junk\n");
        helper.succeed();
    }

    /** Tables, dotted keys, arrays of tables and inline tables; a table or key declared twice is refused. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void tablesAreReadAndRefusedAsNightConfigDid(GameTestHelper helper) {
        Toml.Table t = parse("x.y = 1\n[a.b]\nc = 2\n[a]\nd = 3\n[[e]]\nf = 4\n[[e]]\nf = 5\n[g]\n"
                + "h = {i = 6, j.k = 7}\nl = [1, \"two\", [3], ]\n\"quoted key\" = true\n");
        check(table(t, "x").get("y").equals(1), "a dotted key makes a table");
        check(table(table(t, "a"), "b").get("c").equals(2) && table(t, "a").get("d").equals(3), "a.b then a");
        List<?> e = (List<?>) t.get("e");
        check(e.size() == 2 && ((Toml.Table) e.get(1)).get("f").equals(5), "an array of tables");
        Toml.Table h = (Toml.Table) table(t, "g").get("h");
        check(h.get("i").equals(6) && table(h, "j").get("k").equals(7), "an inline table");
        check(table(t, "g").get("l").equals(List.of(1, "two", List.of(3))), "a mixed array with a trailing comma");
        check(table(t, "g").get("quoted key").equals(true), "a quoted key");
        // ("[a]" twice with nothing in the first is accepted, as NightConfig accepted it)
        for (String bad : List.of("a = 1\na = 2\n", "[a]\nb = 1\n[a]\nc = 2\n", "a = 1\n[a]\n",
                "a = {b = 1}\n[a.c]\n", "[a\n", "[]\n", "a = [1,,2]\n", "a = [,]\n", "a = {b = 1,}\n",
                "a b = 1\n", "a. = 1\n", "= 1\n", "a =\n", "a = tru\n", "a.b = 1\na.b.c = 2\n")) {
            refused(bad);
        }
        helper.succeed();
    }

    /** Written as NightConfig wrote: comments, tab indents, blank lines between tables, quoted keys, lists. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void filesAreWrittenAsNightConfigWroteThem(GameTestHelper helper) {
        Toml.Table root = new Toml.Table();
        Toml.Table s = new Toml.Table();
        root.values().put("s", s);
        root.setComment("s", "The section.\nTwo lines.");
        s.values().put("n", 3);
        s.setComment("n", " Default: 3\n Range: 0 ~ 5");
        s.values().put("x", 0.5);
        s.values().put("list", List.of("a", "b"));
        s.values().put("empty", List.of());
        s.values().put("text", "say \"hi\"\tnow\u0001");
        s.values().put("multi", "one\ntwo\nthree");
        s.values().put("odd key", Double.NaN);
        s.values().put("big", 3000000000L);
        s.values().put("none", new Toml.Table());
        Toml.Table sub = new Toml.Table();
        sub.values().put("on", true);
        s.values().put("sub", sub);
        Toml.Table t = new Toml.Table();
        t.values().put("e", "Entry");
        root.values().put("t", t);
        String text = Toml.write(root, "\n");
        String expected = "#The section.\n#Two lines.\n[s]\n\t# Default: 3\n\t# Range: 0 ~ 5\n\tn = 3\n\tx = 0.5\n"
                + "\tlist = [\"a\", \"b\"]\n\tempty = []\n\ttext = \"say \\\"hi\\\"\\tnow\\u0001\"\n"
                + "\tmulti = \"\"\"\none\ntwo\nthree\"\"\"\n\t\"odd key\" = nan\n\tbig = 3000000000\n\tnone = {}\n\n"
                + "\t[s.sub]\n\t\ton = true\n\n[t]\n\te = \"Entry\"\n";
        check(text.equals(expected), "written:\n" + text);
        Toml.Table back = parse(text);
        check(Toml.write(back, "\n").equals(text), "read back and written again unchanged");
        check(table(back, "s").get("multi").equals("one\ntwo\nthree") && table(back, "s").get("big")
                .equals(3000000000L), "the values read back");
        check(Toml.write(root, "\r\n").equals(expected.replace("\n", "\r\n")), "with Windows line ends");
        helper.succeed();
    }
}

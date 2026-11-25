package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * (3.19.2) The pages of the Darkhold and of the Book of the Vishanti (BookPagesScreen, a client screen: here only what it
 * reads) are written in English and in Hebrew, and the Vishanti's second page has words for every hero.
 */
public final class BookPagesTests {
    private static final String BATCH = "book_pages";

    private BookPagesTests() {
    }

    @EnhancedTest(template = TestKit.ARENA, batch = BATCH)
    public static void everyPageOfBothBooksIsWrittenInEnglishAndHebrew(GameTestHelper helper) throws IOException {
        JsonObject en = TestKit.lang("en_us");
        JsonObject he = TestKit.lang("he_il");
        List<String> keys = new ArrayList<>(List.of("key.enhanced.read_book", "item.enhanced.book.read", "screen.enhanced.book.read",
                "book.enhanced.darkhold.title", "book.enhanced.vishanti.title", "book.enhanced.vishanti.page1.title",
                "book.enhanced.vishanti.page1", "book.enhanced.vishanti.page2.title", "book.enhanced.vishanti.potential.other",
                "book.enhanced.vishanti.potential.ultimate_wanda", "book.enhanced.page_of"));
        for (int i = 1; i <= 4; i++) {
            keys.add("book.enhanced.darkhold.page" + i + ".title");
            keys.add("book.enhanced.darkhold.page" + i);
        }
        for (Power power : Power.values()) {
            keys.add("book.enhanced.vishanti.potential." + power.name().toLowerCase(Locale.ROOT));
        }
        for (String key : keys) {
            check(en.has(key) && !en.get(key).getAsString().isBlank(), key + " is written in English");
            check(he.has(key) && !he.get(key).getAsString().isBlank(), key + " is written in Hebrew");
        }
        check(he.get("book.enhanced.darkhold.page1").getAsString().startsWith("מכשפת השני אינה נולדת, היא מחושלת"),
                "the prophecy is in Yuval's words");
        helper.succeed();
    }
}

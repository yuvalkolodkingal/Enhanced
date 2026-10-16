<div dir="rtl">

# בנייה מקוד המקור

קוד המקור של המוד נמצא ב-[github.com/yuvalkolodkingal/enhanced](https://github.com/yuvalkolodkingal/enhanced). זה מוד של NeoForge 1.21.1 שכתוב בג'אווה 21 ונבנה עם Gradle.

## מה צריך

* **ג'אווה 21** (JDK).
* **Git**, כדי לשכפל את המאגר.
* שום דבר אחר: ה-Gradle wrapper (`./gradlew`) מוריד את Gradle, את NeoForge ואת המיפויים של מיינקראפט בפעם הראשונה.

## בנייה והרצה

<div dir="ltr">

```sh
git clone https://github.com/yuvalkolodkingal/enhanced.git
cd enhanced

./gradlew build              # the mod jar ends up in build/libs/
./gradlew runClient          # play in a development client
./gradlew runGameTestServer  # run the automated in-game tests
```

</div>

כדי להתקין את הבנייה שלכם, מעתיקים את `build/libs/enhanced-<version>.jar` לתיקיית `mods` של מופע NeoForge 1.21.1 (ראו [התקנה](Installation-HE)).

ה-GameTests (`./gradlew runGameTestServer`) מפעילים שרת, מריצים את הבדיקות של המוד שבתוך המשחק (כל הגיבורים, ההקס, דרך המכשפות, קובצי הנתונים וכן הלאה) ויוצאים עם שגיאה אם בדיקה חובה נכשלת (1,471 מהן ב-3.2). מוחקים את `run/world` ואת `run/config/enhanced-server.toml` לפני ריצה: קובץ שרת שנוצר בגרסה ישנה שומר על ברירות המחדל הישנות שלו ונכשל בבדיקות של המספרים החדשים.

## גרסאות

GitHub Actions (`.github/workflows/build.yml`) מריץ את אותה בנייה ואת ה-GameTests בכל דחיפה ובכל pull request, ומפרסם גרסה לכל דחיפה ל-`main` (או לתגית `v*`). ה-jar של הבנייה החדשה ביותר הוא זה שמסומן *Latest* בדף ה-[Releases](https://github.com/yuvalkolodkingal/enhanced/releases).

בקובץ ה-jar יש שיר מוגן בזכויות יוצרים, ולכן הבניות הן לשימוש אישי בלבד (ראו [קרדיטים](Credits-HE)).

## איפה כל דבר

| נתיב | מה יש בו |
|---|---|
| `src/main/java/com/kolodkingal/enhanced/` | המוד: יכולות, חפצים, בלוקים, יצורים, קוד הלקוח, הפקודות והגדרות השרת (`EnhancedConfig.java`). |
| `src/main/resources/` | טקסטורות, מודלים, צלילים, קובצי שפה (`lang/en_us.json`, `lang/he_il.json`), מתכונים, loot modifiers ותגיות. |
| `src/gametest/java/` | הבדיקות שבתוך המשחק. הן אף פעם לא נכנסות ל-jar של המוד. |
| `docs/DESIGN.md`, `docs/DESIGN.he.md` | העיצוב המלא, באנגלית ובעברית, עם כל החוקים והמספרים. |
| `docs/wiki/` | המקור של הוויקי הזה. |
| `docs/screenshots/` | צילומי המסך של ה-README ושל הוויקי הזה. |

## עריכת הוויקי הזה

את הוויקי הזה לא עורכים ב-GitHub. הדפים שלו הם קובצי Markdown בתיקייה **`docs/wiki/`** במאגר, וה-GitHub Action בשם *Publish the wiki* (`.github/workflows/wiki.yml`) מעתיק אותם לוויקי. עורכים את הקבצים שם ופותחים pull request או דוחפים ל-`main`. כל מה שישונה בוויקי עצמו נמחק בפרסום הבא.

* **שם הקובץ = שם הדף**, עם מקפים בין המילים: `docs/wiki/Becoming-a-Hero.md` הוא הדף *Becoming a Hero*. מקשרים לדף אחר בצורה `[טקסט](Page-Name)`, בלי `.md`.
* **לדפים בעברית** יש אותו שם עם `-HE` בסוף (`Thor-HE.md`), והם עטופים ב-`<div dir="rtl">`.
* **צילומי מסך** הם קובצי ה-PNG ב-`docs/screenshots/`. משתמשים בהם בצורה `![טקסט](images/<file>.png)`: ה-Action מעתיק אותם לתיקייה `images/` בוויקי.
* `_Sidebar.md` ו-`_Footer.md` הם סרגל הצד והכותרת התחתונה של הוויקי.
* ה-Action רץ בכל דחיפה ל-`main` ששינתה את `docs/wiki/`, את `docs/screenshots/` או את קובץ ה-workflow, ואפשר להריץ אותו ביד מהלשונית *Actions* (*Publish the wiki*, *Run workflow*).

### הכנת הוויקי (פעם אחת)

GitHub יוצר את מאגר ה-git של הוויקי רק כשקיים בו דף ראשון. לכן, פעם אחת:

1. בהגדרות המאגר, **Settings, General, Features**, מסמנים **Wikis**.
2. פותחים את הלשונית **Wiki** ולוחצים **Create the first page**. שומרים אותו עם כל טקסט: ה-Action מחליף אותו.
3. מריצים את ה-workflow בשם *Publish the wiki*, או דוחפים שינוי ל-`docs/wiki/`.

אם ה-Action נכשל עם "Could not clone the wiki", אחד משני הצעדים הראשונים חסר. ויקי של **מאגר פרטי** דורש תוכנית GitHub Pro, Team או Enterprise.

</div>

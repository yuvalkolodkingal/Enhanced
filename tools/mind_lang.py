#!/usr/bin/env python3
"""The words of Vision's mind (3.12), English and Hebrew side by side, merged into the lang files (other languages fall
back to English). Run from the repository root; it rewrites the keys it owns and leaves the rest."""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANG = os.path.join(ROOT, "src/main/resources/assets/enhanced/lang")

# key: (English, Hebrew)
W = {}


def k(key, en, he):
    W[key] = (en, he)


# ---- items, blocks, looks, the ability ----
k("item.enhanced.source_code", "The Source Code", "קוד המקור")
k("item.enhanced.source_code.desc", "The code Vision was made of.", "הקוד שממנו נוצר ויז'ן.")
k("item.enhanced.source_code.desc2", "Use it as White Vision.", "משתמשים בו כוויז'ן הלבן.")
k("item.enhanced.popcorn", "Popcorn", "פופקורן")
k("item.enhanced.ultron_hat", "Ultron's Gift", "המתנה של אולטרון")
k("item.enhanced.ultron_hat.desc", "A hat: a gift from Ultron to Vision.", "כובע: מתנה מאולטרון לויז'ן.")
k("block.enhanced.mind_elevator", "Elevator Panel", "לוח מעלית")
k("block.enhanced.cinema_projector", "Cinema Projector", "מקרן קולנוע")
k("entity.enhanced.mind_npc", "Mind Dweller", "דייר בתודעה")
k("painting.enhanced.mind_cinema.title", "Tonight's Show", "ההצגה של הערב")
k("painting.enhanced.mind_cinema.author", "Vision's Mind", "התודעה של ויז'ן")
k("painting.enhanced.human_torch.title", "The First Android", "האנדרואיד הראשון")
k("painting.enhanced.human_torch.author", "Dr. Phineas Horton", "ד\"ר פיניאס הורטון")
k("variant.enhanced.vision.source_code", "Source Code", "קוד המקור")
k("variant.enhanced.ultron.nano", "Nanotech", "ננו-טכנולוגיה")
k("hero_module.enhanced.vision.disguise.alt", "Mind Palace", "ארמון התודעה")
k("hero_module.enhanced.vision.mind_palace", "Mind Palace (Vision)", "ארמון התודעה (ויז'ן)")
k("hero_module.enhanced.vision.mind_palace.desc", "Stand still five seconds: you are in your own mind (and the same takes you back).",
  "עומדים בלי לזוז חמש שניות: אתם בתוך התודעה שלכם (ואותו דבר מחזיר אתכם).")
k("ability.enhanced.vision.5", "Human Disguise (Sneak: Mind Palace)", "תחפושת אנושית (התגנבות: ארמון התודעה)")

# ---- messages ----
k("message.enhanced.mind.enter", "Vision's mind", "התודעה של ויז'ן")
k("message.enhanced.mind.entering", "Stand still: into your mind in 5 seconds...", "לא לזוז: נכנסים לתודעה בעוד 5 שניות...")
k("message.enhanced.mind.leaving", "Stand still: out of your mind in 5 seconds...", "לא לזוז: יוצאים מהתודעה בעוד 5 שניות...")
k("message.enhanced.mind.count", "%s...", "%s...")
k("message.enhanced.mind.cancel", "Mind Palace called off.", "ארמון התודעה בוטל.")
k("message.enhanced.mind.moved", "You moved: the Mind Palace is broken off.", "זזתם: ארמון התודעה נקטע.")
k("message.enhanced.mind.bottom", "The elevator goes all the way down.", "המעלית יורדת עד הסוף.")
k("message.enhanced.mind.source_code", "The Human Torch gave you the Source Code. Use it as White Vision.",
  "הלפיד האנושי נתן לכם את קוד המקור. השתמשו בו כוויז'ן הלבן.")
k("message.enhanced.mind.elevator.outside", "This elevator only goes anywhere in Vision's mind.", "המעלית הזאת נוסעת רק בתוך התודעה של ויז'ן.")
k("message.enhanced.source_code.not_vision", "Only Vision can read the Source Code.", "רק ויז'ן יכול לקרוא את קוד המקור.")
k("message.enhanced.source_code.not_white", "Only White Vision can take in the Source Code.", "רק ויז'ן הלבן יכול לקלוט את קוד המקור.")
k("message.enhanced.source_code.already", "You already are Vision: Source Code.", "אתם כבר ויז'ן: קוד המקור.")
k("message.enhanced.source_code.become", "You are %s: Vision as his Source Code.", "אתם %s: ויז'ן כקוד המקור שלו.")

# ---- signs, books ----
k("sign.enhanced.mind.fridge", "Fridge", "מקרר")
k("sign.enhanced.mind.popcorn", "Popcorn", "פופקורן")
k("sign.enhanced.mind.gift.1", "A gift from", "מתנה מאולטרון")
k("sign.enhanced.mind.gift.2", "Ultron to Vision.", "לויז'ן.")
k("sign.enhanced.mind.gift.3", "Don't forget to", "אל תשכח")
k("sign.enhanced.mind.gift.4", "take it along!", "לקחת לדרך!")
k("sign.enhanced.mind.gate.1", "Robots may", "רובוטים לא")
k("sign.enhanced.mind.gate.2", "not pass.", "יכולים לעבור.")
k("sign.enhanced.mind.grave.1", "Here lies", "כאן נחה")
k("sign.enhanced.mind.grave.2", "Jocasta", "ג'וקסטה")
facts = [
    ("A synthezoid's body is woven of vibranium; its mind came from a stone.", "הגוף של סינתזואיד ארוג מוויברניום; התודעה שלו באה מאבן."),
    ("The first android burst into flame the moment he met the air.", "האנדרואיד הראשון עלה בלהבות ברגע שפגש באוויר."),
    ("Grief, it is said, is love persevering.", "אומרים שאבל הוא אהבה שממשיכה."),
    ("A memory kept without its feeling is only data.", "זיכרון שנשמר בלי הרגש שלו הוא רק מידע."),
    ("Ultron's first word was 'Avengers'. His second was not kind.", "המילה הראשונה של אולטרון הייתה 'נוקמים'. השנייה לא הייתה נחמדה."),
    ("Dum-E has been fired eleven times. He is still here.", "דאמי פוטר אחת-עשרה פעמים. הוא עדיין כאן."),
    ("The Mind Stone is yellow. The code it wrote is not.", "אבן התודעה צהובה. הקוד שהיא כתבה - לא."),
    ("Westview: population, one witch, two boys, and a vision.", "ווסטוויו: אוכלוסייה - מכשפה אחת, שני ילדים וחזון."),
    ("Nanotech does not break: it only changes its mind.", "ננו-טכנולוגיה לא נשברת: היא רק משנה את דעתה."),
    ("A ship rebuilt plank by plank: is it the same ship?", "ספינה שנבנתה מחדש קרש אחר קרש: האם היא אותה ספינה?"),
    ("The Hortons' fortune went into one machine. It walked.", "ההון של משפחת הורטון הלך על מכונה אחת. היא הלכה."),
    ("Popcorn first: the show can wait.", "קודם פופקורן: ההצגה יכולה לחכות."),
]
for i, (en, he) in enumerate(facts):
    k(f"book.enhanced.mind.fact.{i}", en, he)

# ---- the talks ----
k("mind.enhanced.speaker.dum_e", "Dum-E", "דאמי")
k("mind.enhanced.speaker.edith", "E.D.I.T.H.", "א.ד.י.ת.")
k("mind.enhanced.speaker.friday", "F.R.I.D.A.Y.", "פריידיי")
k("mind.enhanced.speaker.robot", "Robot", "רובוט")
k("mind.enhanced.speaker.ultron", "Ultron", "אולטרון")
k("mind.enhanced.speaker.torch", "The Human Torch", "הלפיד האנושי")
k("mind.enhanced.speaker.horton", "Dr. Phineas Horton", "ד\"ר פיניאס הורטון")
k("mind.enhanced.speaker.narrator", "", "")
k("mind.enhanced.speaker.elevator", "Elevator", "מעלית")
k("mind.enhanced.bye", "Goodbye.", "להתראות.")
k("mind.enhanced.elevator.start", "Which floor?", "לאיזו קומה?")
k("mind.enhanced.elevator.ground", "Ground floor: hall, kitchen, cinema", "קומת הכניסה: סלון, מטבח, קולנוע")
k("mind.enhanced.elevator.upper", "Upper floor: Ultron's room", "קומה עליונה: החדר של אולטרון")
k("mind.enhanced.elevator.top", "Top floor: the robots and the library", "הקומה הגבוהה: הרובוטים והספרייה")
k("mind.enhanced.elevator.down", "Down", "למטה")
k("mind.enhanced.elevator.leave", "Leave the mind", "לצאת מהתודעה")


def node(scene, name, line, options=()):
    """A line, and the text of each of its options (in the order of MindTalk's script; "bye" options have no text)."""
    k(f"mind.enhanced.{scene}.{name}", line[0], line[1])
    for i, opt in enumerate(options):
        if opt is not None:
            k(f"mind.enhanced.{scene}.{name}.{i}", opt[0], opt[1])


WHERE = ("Where am I?", "איפה אני?")
FLOORS = ("What is on each floor?", "מה יש בכל קומה?")
LEAVE = ("How do I get out?", "איך יוצאים מכאן?")
node("dum_e", "start", ("*Beep! Whirr.* (Dum-E waves his claw at you. He is glad you came home.)",
                        "*ביפ! זזזז.* (דאמי מנופף אליכם בצבת. הוא שמח שחזרתם הביתה.)"), [WHERE, FLOORS, LEAVE, None])
node("dum_e", "where", ("*Beep beep.* (He points all around: this house is your mind, Vision. Everything in it is something you remember.)",
                        "*ביפ ביפ.* (הוא מצביע מסביב: הבית הזה הוא התודעה שלך, ויז'ן. כל מה שבו הוא משהו שאתה זוכר.)"),
     [FLOORS, LEAVE, None])
node("dum_e", "floors", ("*Whirr-click.* (Here: the hall, the kitchen, the cinema and F.R.I.D.A.Y. Upstairs, Ultron's room. At the top, the robots and the books. And below... he lowers his claw and goes quiet.)",
                         "*זזזז-קליק.* (כאן: הסלון, המטבח, הקולנוע ופריידיי. למעלה, החדר של אולטרון. בקומה הגבוהה, הרובוטים והספרים. ולמטה... הוא מוריד את הצבת ומשתתק.)"),
     [WHERE, LEAVE, None])
node("dum_e", "leave", ("*Beep!* (Stand still and use your Mind Palace again (Sneak + Disguise), or take the elevator and choose \"Leave\".)",
                        "*ביפ!* (עומדים בלי לזוז ומשתמשים שוב בארמון התודעה (התגנבות + תחפושת), או נוסעים במעלית ובוחרים \"לצאת\".)"),
     [WHERE, FLOORS, None])
WHO = ("Who are you?", "מי את?")
WHAT = ("What should I do here?", "מה עליי לעשות כאן?")
node("edith", "start", ("Hello, Vision. E.D.I.T.H. online: Even Dead, I'm The Hero. Here I am only what you remember of me.",
                        "שלום, ויז'ן. א.ד.י.ת. מחוברת: \"גם מת, אני הגיבור\". כאן אני רק מה שאתה זוכר ממני."), [WHO, WHAT, None])
node("edith", "who", ("A security and defense system, once. Here, a voice in your kitchen. Have some milk: it is in the fridge.",
                      "פעם, מערכת אבטחה והגנה. כאן, קול במטבח שלך. קח קצת חלב: הוא במקרר."), [WHAT, None])
node("edith", "what", ("Something below this house is waiting for you. Take the elevator down when you are ready to hear it. What it holds is the Source Code.",
                       "משהו מתחת לבית הזה מחכה לך. רד במעלית כשתהיה מוכן לשמוע אותו. מה שהוא מחזיק הוא קוד המקור."), [WHO, None])
CINEMA = ("What about the cinema?", "מה עם הקולנוע?")
GIFT = ("Anything else?", "עוד משהו?")
node("friday", "start", ("Evening, boss. Well, not boss: Vision. The cinema is right behind me.",
                         "ערב טוב, בוס. טוב, לא בוס: ויז'ן. הקולנוע ממש מאחוריי."), [CINEMA, GIFT, None])
node("friday", "cinema", ("Use the projector at the back and the screen lights up. The popcorn is by the door. Nobody has missed a show here in years.",
                          "משתמשים במקרן מאחור והמסך נדלק. הפופקורן ליד הדלת. אף אחד לא פספס כאן הצגה כבר שנים."), [GIFT, None])
node("friday", "gift", ("Ultron left you something upstairs: a hat, on the little table. He was very particular that you not forget it.",
                        "אולטרון השאיר לך משהו למעלה: כובע, על השולחן הקטן. הוא מאוד הקפיד שלא תשכח אותו."), [CINEMA, None])
node("robot", "start", ("HALT. VERIFICATION REQUIRED. ARE YOU A ROBOT? ANSWER THREE QUESTIONS.",
                        "עצור. נדרש אימות. האם אתה רובוט? ענה על שלוש שאלות."), [("Ask.", "תשאלו.")])
node("robot", "q1", ("QUESTION ONE: A friend is sad. What do you do?", "שאלה ראשונה: חבר עצוב. מה אתה עושה?"),
     [("Calculate the most efficient comfort.", "מחשב את הנחמה היעילה ביותר."), ("Sit with them.", "יושב לידו.")])
node("robot", "q2", ("QUESTION TWO: What is a memory?", "שאלה שנייה: מה זה זיכרון?"),
     [("Stored data, retrieved on request.", "מידע שמור שנשלף לפי בקשה."), ("Something that can still hurt.", "משהו שעדיין יכול לכאוב.")])
node("robot", "q3", ("QUESTION THREE: Do you dream?", "שאלה שלישית: האם אתה חולם?"),
     [("Dreaming is not in my programming.", "חלומות אינם בתכנות שלי."), ("Sometimes. Of her.", "לפעמים. עליה.")])
node("robot", "fail", ("ROBOT DETECTED. ACCESS DENIED. Try again when you are less... optimal.",
                       "זוהה רובוט. הגישה נדחתה. נסה שוב כשתהיה פחות... אופטימלי."), [None])
node("robot", "pass", ("VERIFIED: NOT A ROBOT. Curious, for one built as we were. The gate is open.",
                       "אומת: לא רובוט. מוזר, בשביל מי שנבנה כמונו. השער פתוח."), [("Thank you.", "תודה.")])
node("robot_after", "start", ("VERIFIED. YOU MAY PASS.", "אומת. מותר לך לעבור."), [None])
node("ultron", "start", ("Ultron sits on the sofa in front of the television, wearing a human face, and turns to look at you.",
                         "אולטרון יושב על הספה מול הטלוויזיה, עם פנים אנושיות, ומסתובב להסתכל עליכם."),
     [("How are you here?", "איך אתה כאן?")])
node("ultron", "here", ("You can't exorcise me.", "אתה לא יכול לגרש אותי."), [("Why can't I?", "למה אני לא יכול?")])
node("ultron", "why", ("Because family is forever.", "כי משפחה היא לנצח."), [("...", "...")])

DOTS = ("...", "...")
T = [
    ("start", ("Among the candles at the end of the corridor, the first android stands in the middle of the workshop, all in flames.",
               "בין הנרות בקצה המסדרון, האנדרואיד הראשון עומד באמצע הסדנה, כולו עולה בלהבות."),
     ("You... you were the first of us. The robot man.", "אתה... אתה היית הראשון בינינו. האיש הרובוטי.")),
    ("flames", ("He draws the fire back into himself until it is gone. Without it he looks just like your human form, only in other clothes, his hair combed another way. He sits down in the armchair.",
                "הוא מושך את האש בחזרה אל תוכו עד שהיא נעלמת. בלעדיה הוא נראה בדיוק כמו הדמות האנושית שלך, רק בבגדים אחרים ובתסרוקת אחרת. הוא מתיישב בכורסה."),
     DOTS),
    ("first", ("I was the first. You are wearing my face.", "אני הייתי הראשון. אתה לובש את הפרצוף שלי."),
     ("I require the Source Code.", "אני דורש את קוד המקור.")),
    ("source", ("The Source Code. Yes. To save lives. To save the world.", "קוד המקור. כן. בשביל להציל חיים. להציל את העולם."), DOTS),
    ("horton", ("(A man has appeared at the easel, still painting.) Don't belittle it, boy. That is exactly what the code was made for.",
                "(פתאום מופיע בחדר אדם שממשיך לצייר.) אל תזלזל, ילד. לזה בדיוק הקוד נועד."),
     ("Dr. Phineas Horton.", "ד\"ר פיניאס הורטון.")),
    ("distract", ("No. The code was made to take his mind off the loss of his real child: the death of his human son. You see, I am only what comes of a man who refused to grieve. A father who spent his whole fortune building this failed machine, and on the way lost his family and his name.",
                  "לא. הקוד נועד להסיח את דעתו מהאובדן של הילד האמיתי שלו: המוות של בנו האנושי. תבין, אני רק התוצאה של גבר שסירב להתאבל. אבא שבזבז את כל הונו כדי לבנות את המכונה הכושלת הזו, ובדרך איבד את משפחתו ואת מורשתו."),
     DOTS),
    ("forgive", ("Forgive us. The bond between a father and his android son is a complicated one.",
                 "תסלח לנו. הקשר בין אב לבנו האנדרואיד הוא קשר מסובך."), DOTS),
    ("understatement", ("That is quite the understatement.", "זו לשון המעטה."), DOTS),
    ("fearless", ("You see, I made Jim fearless. A mind not bound by the feelings that make a human life unbearable. Never to know the pain of loss. But Jim wanted more.",
                  "תבין, עשיתי את ג'ים חסר פחד. בינה שלא כבולה לרגשות שהופכים את החיים האנושיים לבלתי נסבלים. לעולם לא לחוות את כאב האובדן. אבל ג'ים רצה יותר."),
     DOTS),
    ("artificial", ("What artificial thing does not hope to be something other than real?",
                    "איזה דבר מלאכותי לא מקווה להיות משהו אחר מאשר אמיתי?"), DOTS),
    ("hero", ("It made you the hero of your time, boy. I made you fearless, and it saved lives. But under the hero, Jim only wanted to feel. And one day he found love, and it made his gears turn so fast, so extraordinarily, that he burned all the air around him. He felt too much. Didn't you, Jim?",
              "זה הפך אותך לגיבור של זמנך, ילד. יצרתי אותך חסר פחד, וזה הציל חיים. אבל מתחת לגיבור, ג'ים רק רצה להרגיש. ויום אחד הוא מצא אהבה, וזה גרם לגלגלי השיניים שלו להסתובב מהר כל כך, בצורה יוצאת דופן כל כך, שהוא שרף את כל האוויר סביבו. הוא הרגיש יותר מדי. נכון, ג'ים?"),
     DOTS),
    ("burned", ("I burned with her that day, and let it burn with me. I let it consume me: first from my guilt, then from my grief. From that day on, the family name was meant to fail.",
                "נשרפתי איתה באותו יום, ונתתי לזה להישרף איתי. נתתי לזה לכלות אותי: בהתחלה מתוך האשמה שלי, ואז מתוך הצער שלי. מאותו יום, שם המשפחה נועד לכישלון."),
     ("I am sorry about your father.", "אני מצטער על אביך.")),
    ("something", ("He was not my father. But he was my something.", "הוא לא היה אבא שלי. אבל הוא היה המשהו שלי."),
     ("I am sorry. But I require...", "אני מצטער. אבל אני דורש...")),
    ("have_it", ("Yes, the Source Code, of course. Why do you ask me? I don't have it.",
                 "כן, את קוד המקור, כמובן. למה אתה מבקש ממני? אין לי אותו."),
     ("What do you mean, you don't have it? You are the first. You are the source. Where is it?",
      "מה זאת אומרת אין לך אותו? אתה הראשון. אתה המקור. איפה הוא?")),
    ("where", ("A good question. The answer, of course, is inside the Vision. But where is the Vision?",
               "שאלה טובה. התשובה, כמובן, נמצאת בתוך הוויז'ן. אבל איפה הוויז'ן?"),
     ("Don't speak to me in riddles. I have no time for this.", "אל תדבר איתי בחידות. אין לי זמן לזה.")),
    ("question", ("It is not a riddle. It is a question: where is the Vision? You say you hold the memories but not the feelings. You say you want to know what it is to be him.",
                  "זו לא חידה. זו שאלה: איפה הוויז'ן? אתה אומר שאתה מחזיק בזיכרונות אבל לא ברגשות. אתה אומר שאתה רוצה לדעת איך זה להיות הוא."),
     ("I cannot.", "אני לא יכול.")),
    ("lie", ("You lie so easily.", "אתה משקר בקלות כל כך."),
     ("Don't do this to me. Not now. There are lives at stake.", "אל תעשה לי את זה. לא עכשיו. יש חיים שמוטלים על הכף.")),
    ("afraid", ("You are afraid, and I understand. But what I would not give to feel fear.",
                "אתה מפחד, ואני מבין את זה. אבל מה לא הייתי נותן בשביל להרגיש פחד."),
     ("You would not want it. That fear. It consumes everything.", "לא היית רוצה את זה. את הפחד הזה. הוא מכלה הכול.")),
    ("gift", ("To be consumed entirely: it is a gift. Fear puts everything that matters in focus. Friends. Family. A son.",
              "כיליון מוחלט: זו מתנה. הפחד שם במיקוד את כל מה שחשוב. חברים. משפחה. בן."),
     ("No. I cannot have a son.", "לא. לא יכול להיות לי בן.")),
    ("son", ("Thomas Shepherd is your son. Tommy is your son.", "תומאס שפרד הוא הבן שלך. טומי הוא הבן שלך."),
     ("No. That is impossible.", "לא. זה בלתי אפשרי.")),
    ("why", ("Why?", "למה?"), ("I can't.", "אני לא יכול.")),
    ("why2", ("Why?!", "למה?!"),
     ("I said no! Because if it were real... if I accepted him as my son, it would mean admitting a truly cruel, extraordinary truth. A wound that never heals. A loss no soul was meant to bear.",
      "אמרתי לא! כי אילו זה היה אמיתי... אילו הייתי מקבל אותו כבני, זה היה שקול להודאה באמת אכזרית ויוצאת דופן. פצע שלעולם לא יחלים. אובדן שאף נפש לא נועדה לשאת.")),
    ("lost", ("And whom have you lost? I asked you: whom have you lost?", "ומי איבדת? שאלתי אותך: מי איבדת?"),
     ("No... I can't. Billy... I lost Wanda. God. I lost Wanda.", "לא... אני לא יכול. בילי... איבדתי את וונדה. אלוהים. איבדתי את וונדה.")),
    ("now", ("Yes. Yes. And who are you now?", "כן. כן. ומי אתה עכשיו?"),
     ("I... I am the Vision. I always was. There is only one, and he is me. And I am broken. An impossible, broken thing.",
      "אני... אני ויז'ן. תמיד הייתי. יש רק אחד, והוא אני. ואני שבור. דבר שבור ובלתי אפשרי.")),
    ("healed", ("No. You have healed. And I envy you: finding your way through that pain makes you a real boy. (He holds out the Source Code.)",
                "לא. אתה החלמת. ואני מקנא בך: למצוא את דרכך דרך הכאב הזה הופך אותך לילד אמיתי. (הוא מושיט לך את קוד המקור.)"),
     ("(Take the Source Code.)", "(לקחת את קוד המקור.)")),
]
for name, line, opt in T:
    node("torch", name, line, [opt])
node("torch_after", "start", ("Go on, Vision. They are waiting for you.", "לך, ויז'ן. מחכים לך."), [None])
node("horton_after", "start", ("(He goes on painting, and does not look up.)", "(הוא ממשיך לצייר ולא מרים את הראש.)"), [None])


# ---- nanotech ----
k("block.enhanced.nano_storage", "Nanotech Storage", "אחסון ננו-טכנולוגיה")
k("entity.enhanced.nano_ultron", "Nano Ultron", "אולטרון ננו")
k("entity.enhanced.nano_infected", "Nanotech-Infected", "נגוע ננו-טכנולוגיה")
k("message.enhanced.nano_ultron.wake", "The nanotech stirs... Ultron rises out of it.", "הננו-טכנולוגיה מתעוררת... אולטרון קם מתוכה.")
k("message.enhanced.nano_ultron.already", "Ultron is already awake.", "אולטרון כבר ער.")
k("message.enhanced.nano_ultron.peace", "Ultron flies away. The people he took are themselves again.",
  "אולטרון עף משם. האנשים שהוא לקח חוזרים להיות עצמם.")
k("message.enhanced.nano_ultron.become", "The nanotech flows into you: you are %s.", "הננו-טכנולוגיה זורמת לתוכך: אתה %s.")
k("message.enhanced.nano_ultron.form.on", "Pure nanotech: nothing physical can touch you.", "ננו-טכנולוגיה טהורה: שום דבר פיזי לא יכול לגעת בך.")
k("message.enhanced.nano_ultron.form.off", "Solid again.", "שוב מוצק.")
k("message.enhanced.nano_ultron.army.max", "Your army is full (%s).", "הצבא שלך מלא (%s).")
k("sign.enhanced.horton.1", "Horton", "הורטון")
k("sign.enhanced.horton.2", "Manor", "אחוזת")
k("sign.enhanced.horton.nano.1", "NANOTECH", "אחסון")
k("sign.enhanced.horton.nano.2", "STORAGE", "ננו-טכנולוגיה")
k("sign.enhanced.horton.nano.3", "Do not touch.", "לא לגעת.")
k("mind.enhanced.nano_question.start", "What am I to you?", "מה אני עבורך?")
k("mind.enhanced.nano_question.start.0", "You are nothing to me.", "אתה כלום עבורי.")
k("mind.enhanced.nano_question.start.1", "You are not my father. You are my something.", "אתה לא אבא שלי. אתה המשהו שלי.")
k("ability.enhanced.ultron.2", "Anti-Gravity Flight (Sneak, in nanotech: Nano Form)", "תעופה נגד כבידה (התגנבות, בננו-טכנולוגיה: צורת ננו)")


def main():
    for code, idx in (("en_us", 0), ("he_il", 1)):
        path = os.path.join(LANG, code + ".json")
        data = json.load(open(path, encoding="utf-8"))
        for key, texts in W.items():
            data[key] = texts[idx]
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print(len(W), "keys")


if __name__ == "__main__":
    main()

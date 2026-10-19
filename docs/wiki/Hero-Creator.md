# Hero Creator

The **Hero Creator** is a Creative-mode item that opens a screen where you can **become any hero of the mod** at once, in any look, or **make a hero of your own**: a name, up to ten powers, each with its own **strength**, on the keys you choose (plain or with **Sneak**), with your own moves, colours, **skin** and extras like a cape or glowing eyes. Since 3.4 the powers are not only **the thirty most common superpowers** of comics and films: **every ability of every hero of the mod** is there too, from Thor's Lightning Beam to Agatha's Rune Trap. A hero you make can be saved to a file and loaded again, on any server.

Hebrew: [עברית](Hero-Creator-HE)

## What's new in 3.6

* **Doctor Strange and the Hulk**: their twelve abilities are cards, and so are their Sneak variants (Whip Grapple, Seraphim Dome, Astral Projection, Mirror Dimension, the Eye's Rewind; Grab Smash, Roar, Bound, Gamma Charge). Ready-made heroes: *Doctor Strange-like* and *Hulk-like*.
* **The stone rows**: each [Infinity Stone](Infinity-Stones) is a card of its own, `stone.reality` to `stone.soul`, that works while your hero carries that stone. See [below](#the-stone-rows-36).
* **The Y key for the Eye**: Doctor Strange's Eye goes on **Y**, as Build Magic does. A hero may have one thing on **Y**: the Eye, or a Build Magic.
* **The Human Form**: with the Hulk's `hulk.transform`, **N** turns all your powers off and shows your own skin until you press it again.
* **Moves**: the flight style *Doctor Strange (the Cloak)* (and the Flight power's new option, **Cloak**), and the runs *Doctor Strange* and *The Hulk*.
* **Roles**: Doctor Strange's abilities make a hero a **sorcerer**; a god's ability (the Scarlet Witch's Hex, Loki's Frost Giant, Thor's, the Phoenix's, Heimdall's, Odin's or Hela's body) lets him **hold the Infinity Stones**.
* **Update the server and every player together:** 3.6 speaks network protocol **10**.

## What's new in 3.5

* **Every ability in its own colour.** Every power card has a **Colour** button, and every key box a little swatch, that opens a **colour picker**: swatches, a hue and a brightness slider, a colour code, and a preview of your hero casting the power in that colour. Everything the power draws takes it: beams, shields, sparks, bolts, auras and glow. See [Colours](#colours-every-power-in-its-own-colour-35).
* **Every ability is a card of its own.** Every **Sneak variant** of the sixteen heroes before 3.5 is now a card you can pick by itself and put on any key: Billy's **Hex Barrier**, Thor's **Shatter Mjolnir**, Spider-Man's **Spider-Sense**, Thanos's **Time Lock**, Cyclops's **Ricochet Shot** and 28 more; and the **Scarlet Witch's Build Magic** is a card beside Billy's. See [Heroes' Abilities](Hero-Abilities#the-sneak-variants-cards-of-their-own-35).
* **Heimdall, Odin and Hela**: their eighteen abilities are cards too, and they have ready-made heroes (*Heimdall-like*, *Odin-like*, *Hela-like*).
* **Unsaved changes are kept safe**: Esc or Back with changes you have not saved asks "Unsaved changes: discard them?" (**Discard** / **Keep editing**).
* **The old hero's hat comes off**: becoming another hero puts the old hero's helmet, goggles, glasses or crown in your inventory (see [below](#changing-hero-the-old-heros-headgear-35)).
* A power card's description now shows in full (up to six lines), and the status line ("Saved...", "Loaded...") shows at the top, where it covers no button.
* **Update the server and every player together:** 3.5 speaks network protocol **9**.

## What's new in 3.4

* **Every hero's abilities.** All **81 abilities** of the mod's sixteen heroes are powers you can pick, each with its **Sneak variant**, its look, its sounds and its animations, the same as the hero's own (it is the hero's own code). Mix them: Thor's lightning, Quicksilver's speed and Agatha's shield on one hero. The full list is on [Heroes' Abilities](Hero-Abilities).
* **Ten keys.** Each of **R G V B N** has two boxes: the key alone, and **Sneak +** the key, like the real heroes.
* **A strength slider for the heroes' abilities too**, a bigger budget (**90 points**, was 60) and up to **10 powers** (was 8).
* **Animations as good as the heroes'.** Every power plays a gesture, held and switched-on powers have a stance, and you choose how your hero **flies**, **runs**, **runs at super speed** and **stands**. Everyone else sees them.
* **Looks:** a **cape**, a **hat**, a **mask**, **glowing eyes** and an **aura**, on top of the skin.
* **A new editor** with six tabs, a **3D preview** that moves, **ready-made heroes** to start from, categories and a search box (in English and Hebrew).
* **Choose a Hero gives that hero's items**, as becoming him the usual way would: Thor gets his Mjolnir, Agatha her Darkhold.
* **Files from 3.3 still load:** they are turned into the new format by themselves.
* **Update the server and every player together:** 3.4 speaks network protocol **8**; a 3.3 client cannot join a 3.4 server, or the other way round.

## Getting it and who can use it

* It is only in the **Enhanced creative tab**. It has no recipe and is never found in a chest.
* It works for a player in **Creative mode**, or for an **operator** (in any mode). A server can allow operators only (`custom_hero.operatorsOnly`) or switch the creator off (`custom_hero.enabled`). The server checks every button again, so a changed client cannot get round it.
* Right-click it to open the screen. Its first page has three big buttons: **Choose a Hero**, **Create My Hero** (**Edit My Hero** once you are one) and **Stop Being a Hero**.

## Choose a Hero

A card for every hero of the mod, with the hero's face drawn from its skin (scroll for more). Click a hero to see its looks, pick one and press **Become!**: you are that hero at once, with the title and the announcement, as if you had earned it. A server can turn this page off (`custom_hero.allowChooseExisting`). Between two heroes made or chosen you wait 2 seconds (`custom_hero.applyCooldownTicks`).

### Changing hero: the old hero's headgear (3.5)

When you become another hero (here, by a command, by an awakening item or by the cure), a worn item of **another** hero, **Magneto's Helmet**, **Quicksilver's Goggles**, **Cyclops's Ruby-Quartz Glasses** or **Wanda's Crown**, comes off your head and goes into your inventory (at your feet if it is full; a line in the chat says where). It stays on if it belongs to your new hero, or if your custom hero has an ability of that hero, and nothing comes off when your power is only taken away. Nothing is lost, and nothing keeps working unseen on your head.

### The hero's items come with him (3.4)

Choosing a hero also gives you **the items that becoming him the usual way leaves you with**, so you can play him fully at once:

| Hero | What you get |
|---|---|
| Magneto | **Magneto's Helmet**, put on your head |
| Quicksilver | **Quicksilver's Goggles**, put on |
| Cyclops | **Ruby-Quartz Glasses**, put on (they hold his eyes back, as for the real Cyclops) |
| Thor | **Mjolnir**, already **claimed by you**; in the looks *Ragnarok*, *Love and Thunder* and *Love and Thunder, helmet* **Stormbreaker** instead |
| Loki | **Loki's Scepter** |
| Thanos | **Thanos's Gauntlet** (the Titan's Infinity Gauntlet) |
| Doctor Strange (3.6) | An **Eye of Agamotto**, already **his**, and a **Sling Ring** |
| Ultron | **Ultron's Core** |
| Professor X | **Professor X's Wheelchair** |
| Agatha | The **Darkhold** |
| The others | Nothing: the item that awakens them (the gene, the Synthezoid Core, the spider, the Phoenix Ember, the Essence...) is used up when they awaken |

* **Never twice.** If you already have that item anywhere (in your bag, worn or in a hand), you do not get another one. Choose Thor ten times and you still have one Mjolnir.
* Glasses, goggles and helmet go on your head only if it is free; otherwise they go in your bag. If your bag is full, the item drops at your feet.
* A server can switch this off: `custom_hero.chooseGivesItems` (on by default).

## Making your own hero

**Create My Hero** opens the editor. It has six tabs, **Start**, **Powers**, **Keys**, **Moves**, **Look** and **Save**, and on every tab a **3D preview** of your hero on the left: drag it to turn it, and press **Stand**, **Walk**, **Run** or **Fly** to watch it move, or **Try** to see a power's move. The **budget bar** at the top shows the points you have spent ("45 / 90 points"): green, yellow from three quarters, red when it is over, and then it shakes and says *Too many points! Take a power away or make one weaker.* You may go over the budget while you try things out (the bar only warns you), but the server leaves out the last powers past the budget when you become your hero, so bring it back under first. The number of powers is a hard limit: with the most powers you may have, **Add** is blocked until you remove one.

Your hero is saved with your player: it survives death and logging out, and if you become another hero for a while, **Edit My Hero** brings it back as you left it.

### 1. Start: from a hero, from a kind of hero, or from nothing

The **Start** tab has ready-made heroes to begin with. Pick one and change anything you like; your hero is replaced only after you say yes.

* **"Like" a hero** (21): *Magneto-like*, *Scarlet Witch-like*, *Quicksilver-like*, *Vision-like*, *Wiccan-like*, *Tommy-like*, *Loki-like*, *Thor-like*, *Phoenix-like*, *Spider-Man-like*, *Thanos-like*, *Ultron-like*, *Apocalypse-like*, *Agatha-like*, *Cyclops-like*, *Professor X-like* (3.5) *Heimdall-like*, *Odin-like* and *Hela-like*, and (3.6) *Doctor Strange-like* and *Hulk-like*: that hero's own abilities at strengths that fit, his colours, his look, his way of flying and running, and a fitting cape, hat, mask or aura (Thor-like has Thor's cape).
* **A kind of hero** (6): *Speedster*, *Flyer-Blaster*, *Tank*, *Mage*, *Mind Master* and *Elemental*, built from a mix of powers.
* **Empty Hero**: start with nothing and pick every power yourself.

Every ready-made hero costs **60 points or less**, so it fits on a server that still has the old budget of 60.

This is not the same as **Choose a Hero** on the first page: *Choose a Hero* makes you the real hero (with his items); *Thor-like* is your own hero that starts as a copy of Thor's powers, which you can then change.

### 2. Pick your powers

The **Powers** tab lists everything you can pick:

* the **thirty powers** of the catalogue ([table below](#the-thirty-powers)), and
* the **abilities of the heroes** ([Heroes' Abilities](Hero-Abilities)): 81 in 3.4, and since 3.5 Heimdall's, Odin's and Hela's eighteen, and **every Sneak variant as a card of its own** (33 of them, and the Scarlet Witch's Build Magic): 133 in all; and since 3.6 Doctor Strange's and the Hulk's twelve, with nine more Sneak-variant cards: **154**.
* the six **stone rows** (3.6), one for each Infinity Stone ([below](#the-stone-rows-36)).

Chips at the top filter them: **Body**, **Move**, **Energy**, **Elements**, **Mind** and **Heroes** (a face for each hero: click one to see only his abilities). The **search box** finds a power by its name, its description, its id or its hero's name, in English or in Hebrew. Powers that do the same kind of thing are stacked on one card (all the beams, all the shields, all the telekinesis...) with a switch to pick which one.

Click a card to see it: its name, what it does, how its key acts (▶ press, ✋ hold, ⏻ on / off, ★ always on), **Add** / **Remove** and **Try**. A power of the thirty has **options** (for example a beam, bolts or a burst). Every power has a **Colour** button (3.5, [below](#colours-every-power-in-its-own-colour-35)). A hero's ability has a switch for its **Sneak variant**, "Sneak: Shatter Mjolnir ✔": switch it off (✖) and the Sneak box of its key is free for something else. If a card cannot be added, it says why: the server switched it off, you have too many powers, it needs another power first, or it uses the same thing as a power you already have (see [step 5](#5-powers-that-cannot-go-together)).

A hero's ability in your hero **is that hero's ability**: the same code, effects, sounds and rules, in your colours where the hero's own colours were his (Thor's blue lightning, Wanda's Hex and other fixed looks stay as they are, unless you give the power a colour of its own). A few have rules of their own for a custom hero: see [the item rules](#abilities-and-items).

**A Sneak variant as a card (3.5).** Every Sneak variant of the heroes is also a card of its own, named "*ability* (*hero*)", for example **Hex Barrier (Billy)** or **Shatter Mjolnir (Thor)**. Picked **alone**, it works on **any** key, plain or Sneak: Billy's Hex Barrier on **B** with no Freeze at all. Picked **with the ability it belongs to**, that ability keeps only its main action, and the variant goes on the box you give it. It costs what its ability costs, and the two together count as **one** power. A few need their ability, as the hero does: Dissolve the Hex needs the Hex, the Walkman needs Quicksilver Time, Instant Kill needs the Iron Spider Legs, Command needs the Sentry Army, Open Eyes needs Shut Eyes. The full list is on [Heroes' Abilities](Hero-Abilities#the-sneak-variants-cards-of-their-own-35).

#### Colours: every power in its own colour (3.5)

Every power card has a **Colour** button, and on the **Keys** tab every filled box has a small swatch of its colour; both open the colour picker, "Colour of *power*":

* the editor's **16 swatches**, a **Hue** slider, a **Brightness** slider and a **Colour code** box (`#RRGGBB`);
* **Hero colour**: back to no colour of its own (the power's own colours, or your hero's);
* a **Cast preview** on the left: your hero casts the power every two and a half seconds, in the chosen colour;
* **Done** (Enter) and **Cancel** (Esc).

Everything that power draws takes the colour: its beams, bolts, shields and barriers, sparks and particles, auras and glow, the colour of its line on the HUD. A power and its Sneak variant share one colour. Colours a power cannot take: a body (more health, armour) has nothing to draw, so only its HUD line shows it; Loki's Frost Giant skin stays Jotun blue; the Infinity Gauntlet's stones keep their own colours (the fist's flare takes yours); webs, the Iron Spider's legs, the Sentries' eyes, Billy's crown and the Vision of the Hex keep their look, because they are things, not effects. Where an ability uses the game's own sparks or flames, which can't be coloured, a burst of energy in your colour is added. The built-in heroes themselves never change: only a custom hero's chosen colours do. Heimdall's and Hela's abilities keep their own colours (Heimdall's gold and the Bifrost's rainbow, Hela's black and green), and Odin's follow your hero's colour; of the three, only Odin's Odinforce shield takes a colour of its own.

### 3. Strength

Every power has a **strength from 1 to 10** (the server's highest: `custom_hero.maxStrength`), set with a slider.

* For the **thirty powers**, every number changes evenly from strength 1 to strength 10, and you see the real numbers beside the slider (damage, range, cooldown...). The [table](#the-thirty-powers) gives both ends.
* For the **heroes' abilities**, strength 5 is the hero himself. Higher makes the ability **hit harder** and **recharge faster**, lower the other way: at the server's default spread the damage is ×0.6 at strength 1 and ×1.5 at strength 10, and the wait between two uses is divided by the same number. A hero's **body** (more health, armour...) grows the same way. The slider shows these as **Damage ×**, **Wait ×** and **Body ×**.
* Some abilities have **nothing to make stronger** (a switch such as Vision's Phase, Quicksilver Time, the Frost Giant, Build Magic): they have **no slider**, and always cost as at strength 5.

### 4. The budget and the number of powers

Every power costs **points for each point of its strength**: 1, 2 or 3 points a point (the strong ones cost 3). An ability without a slider costs its points a point × 5. A hero has **90 points** to spend by default (`custom_hero.budget`) and at most **10 powers** (`custom_hero.maxPowers`). A hero's ability and its Sneak variant count as **one** power and cost nothing extra. A power of the thirty whose other option you put on a Sneak box ([step 6](#6-put-them-on-the-keys-plain-or-sneak)) costs **half again** (rounded up).

For example: Thor's Mjolnir Throw at 5 (10 points), his Lightning Beam at 6 (18), the God of Thunder at 4 (8), Flight at 5 (10), his Asgardian Body at 5 (5) and Energy Blast at 4 (8) spend 59 of the 90.

### 5. Powers that cannot go together

Some powers drive the same thing inside your hero: there is only one telekinetic grip, one shield, one beam, one shape. Two powers of **different heroes** (or a hero's ability and one of the thirty) that drive the same thing **cannot both be picked**. Abilities of the **same hero** can always share, as the hero does. The editor tells you on the card ("Uses the same shield as Witch's Shield. Remove that one first."), and a file that has both keeps the first one and leaves out the later one, with a note.

| What they share | Who drives it |
|---|---|
| **Telekinetic grip** (one thing held) | Magneto's Metal Telekinesis, Wanda's Chaos Telekinesis, Billy's Hex Telekinesis, the Phoenix's Telekinesis, the Telekinesis power |
| **Shield** | Wanda's Chaos Shield, Billy's Freeze (only with its Sneak variant, the Hex Barrier) and the Hex Barrier card, Apocalypse's Golden Shield, Agatha's Witch's Shield, Odin's Odinforce (3.5), Doctor Strange's Shield of the Seraphim (3.6) |
| **Beam** (the hero beam) | Wanda's Chaos Beam, Vision's Mind Stone Beam, Ultron's Repulsor Fire |
| **Beam** (the continuous beam) | Billy's Hex Beam, Agatha's Purple Beam, Cyclops's Optic Blast and Shut / Open Eyes, Energy Blast's Beam, Fire's Flamethrower, Water's Water Jet, the Mind Stone's and the Power Stone's rows (3.6) |
| **Metal charge** | Magneto's Iron Shrapnel Storm and Ultron's Repulsor Fire (Magneto's shrapnel charge is Ultron's plasma pool) |
| **Shape** | Loki's Shapeshift, Shapeshifting's Creature, the Hulk's Human Form (3.6) |
| **Size** | Shapeshifting's Grow and Shrink, the Hulk's body (3.6) |
| **Super speed** | Quicksilver Time, Quicksilver's and Tommy's Super Speed, Super Speed's Super Run |
| **Slow time** | Quicksilver Time, Tommy's Slow Time, Time's Slow Time, Doctor Strange's Eye, the Time Stone's row (3.6) |
| **Phasing** | Vision's Phase, the Phasing power, Doctor Strange's Astral Projection (3.6) |
| **Wall-crawling** | Spider-Man's Wall Crawl, the Wall-Crawling power |
| **Weapon, Hex, drones, spear, the dead** | Thor's Mjolnir Throw; Wanda's Hex; Ultron's Upload and Sentry Army; Odin's Gungnir; Hela's Raise the Dead (each only one hero, so they never clash) |
| **Build Magic** (3.5) | Billy's Build Magic, the Scarlet Witch's Build Magic: only one of the two |
| **Soul chain** (3.6) | Thanos's Soul Stone (his Soul Drain) and the Soul Stone's row |
| **The Y key** (3.6) | Billy's Build Magic, the Scarlet Witch's Build Magic, Doctor Strange's Eye: one thing on **Y** |

So Magneto's Iron Shrapnel Storm and Ultron's Repulsor Fire cannot be on the same hero, and neither can Quicksilver's Super Speed and Tommy's; Billy's Hex Beam and Agatha's Purple Beam cannot either. But Wanda's Chaos Beam with Billy's Hex Beam can: they are different beams.

Two more rules:

* **Some abilities need another.** Quicksilver's Wall & Water Run needs his Super Speed or Quicksilver Time; some Sneak-variant cards need their ability (above).
* **World powers.** Thirteen abilities change the world around you: Wanda's **Hex**, Thor's **Bifrost**, Thanos's **Time Stone**, Ultron's **Upload** and **Sentry Army**, Professor X's **Take Over a Body**, since 3.5 Heimdall's **Bifrost** and **Bifrost Strike**, Odin's **Banish** and **Allfather's Command**, and Hela's **Raise the Dead**, and since 3.6 Doctor Strange's **Sling-Ring Portal** and the **Space Stone**'s row. A server can switch them off for custom heroes (`custom_hero.worldPowers`), or switch off all the heroes' abilities (`custom_hero.heroModules`), or single ones (`custom_hero.disabledPowers`, for example `thor.lightning`, or `thor.*` for all of Thor's).

### 6. Put them on the keys: plain or Sneak

The **Keys** tab has **ten boxes**: the five keys **R G V B N** (with the names of your own key bindings), each once for **Press** and once for **Sneak +**. Click a box to choose what goes in it from a list of what fits; **Empty this box** clears it.

* **A hero's ability with a Sneak variant** fills **both boxes of its key** by itself: put Thor's Mjolnir Throw on **R**, and **Sneak + R** is Shatter Mjolnir. The two boxes are drawn joined. To put something else on that Sneak box, switch the variant off on the Powers tab.
* **Any power that takes a key** can sit in a Sneak box on its own, as long as it is not a hero's ability whose Sneak variant is on.
* **A power of the thirty** with several *press* options can have one option on the plain box and **another on its Sneak box** (for example Energy Blast's Bolts on **B** and its Burst on **Sneak + B**). That costs half the power again in points.
* **Sneak + a key whose Sneak box is empty** does what the key does alone.
* **One cooldown per key**, shared by its two boxes, as for every hero.
* **Always-on powers** (the bodies, Durability, Night Vision...) need no box: they are listed under *With no key*. **Build Magic** (Billy's, or since 3.5 the Scarlet Witch's) is used with **Y** and needs no box either, and so is Doctor Strange's **Eye** (3.6; its Sneak variant, the Rewind, is **Sneak + Y**).
* **A Sneak-variant card** (3.5) goes on any box, plain or Sneak; when its ability is on the same hero, that ability's own Sneak box is freed for it.
* **Auto** lays out every power like a hero's: the heroes' abilities on the keys they have for their own hero (Thor's Lightning Beam on **V**...), the rest by kind (energy and elements on **R**, movement on **G**, mind on **V**, body on **B**), and what finds no key on a free Sneak box.
* What you hold goes to the box that went down: let go of Sneak while holding a beam and the beam keeps going until you let go of the key.
* Some abilities use Sneak for a small extra of their own while you use them (setting a telekinetic grip down, Magneto's pull with nothing held): that works only while the Sneak box of their key is empty.

### 7. Animations: how your hero moves

The **Moves** tab chooses how your hero moves. **Automatic** is always there and is usually right; everything you choose shows on the preview at once. Other players see all of it.

* **Gestures.** Every power plays a move when you use it. A hero's ability plays **the hero's own move** (Thor's arm to the sky, Agatha's claw and pull, Cyclops's hand at the visor...); a power of the thirty plays a fitting one (Energy Blast's Bolts a hand blast, Earth's Tremor a two-fist stamp). For each of your ten boxes you can choose another one from the list, or **No move**: *Hand blast*, *Both palms push*, *Arm to the sky*, *Fist raised*, *Ground slam*, *Two-fist stamp*, *Fingers to the temple*, *Circle in the air*, *Flick*, *Spell hand*, *Scream*, *Wave of the arms*, *Claw and pull*, *Conductor*, *Trembling hand*, *Shoulder dash*, *Jabs*, *Rune circle*, *Thwip*, *Eye blast*, *Leap*, *Sonic boom*, *Gift*, *Psionic push*, *Pull back*, *Stamp of time*, *Rewind*, *Freeze*, *Mind grip*, *Overcharge*, *Push* and *Hex flick*. A borrowed move keeps its flourish: Thor's sparks, the Phoenix's motes, Thanos's ground ring.
* **Stances.** While a power is **held** (a beam, a shield) or **switched on** (Density, the God of Thunder), your hero stands like the hero it comes from. You can choose another: while held, *Hand forward*, *Wanda's two hands*, *Vision's brow*, *Thor's call*, *Repulsor palm*, *Hand at the visor*, *Drain grip*, *Wanda's shield*, *Apocalypse's guard*, *Agatha's shield*, *Billy's barrier*, *Freezing hands*, *Gathering fire*, *Storm arms* or *Straining*; while on, *Regal field*, *Floating ghost*, *Heavy*, *Frost giant*, *Crackling*, *Listening*, *Reading minds*, *Spider surge*, *Menacing* or *Reality veil*; or **No stance**.
* **Flight style**: *Plain* (the game's own flying), or the way a hero flies, with his take-off and his flight effects in your colours: *Magneto*, *Scarlet Witch*, *Vision*, *White Vision*, *Wiccan*, *Thor*, *Phoenix*, *Ultron*, *Apocalypse*, *Agatha* or (3.6) *Doctor Strange (the Cloak)*. Automatic takes the style of a flier whose ability you have (Thor, Wanda, Vision, Magneto, Billy, the Phoenix, Ultron, Apocalypse, Agatha, in that order), or else follows Flight's option (Hover flies like Vision, Jet like Ultron, Wings like the Phoenix, and since 3.6 Cloak like Doctor Strange). The style only changes how you look: your hero still flies only with the **Flight** power.
* **Run**: how your hero walks and sprints: *Ordinary*, *Magneto*, *Scarlet Witch*, *Vision*, *White Vision*, *Wiccan*, *Loki*, *Thor*, *Phoenix*, *Spider-Man*, *Thanos*, *Ultron*, *Apocalypse*, *Cyclops*, *Professor X*, *In a wheelchair*, *Agatha*, or (3.6) *Doctor Strange* or *The Hulk*. Automatic takes the run of a hero whose ability you have (the speedsters have no run of their own here: their running is the *Super run*).
* **Super run**: how your hero runs at super speed: *Quicksilver's Time*, *Quicksilver* or *Tommy*. Automatic runs like Tommy with Tommy's Super Speed, like Quicksilver in his Time while time is slowed, and like Quicksilver otherwise.
* **Standing**: a stance for when your hero stands still on the ground: *Ordinary*, *Calm*, *Ready*, *Regal*, *Heavy*, *Giant*, *Crackling* or *Menacing*.
* **First person.** You see your own hands do the moves too, in first person, even the moves you borrowed.

Every move and stance is smooth (no jumps or flicker), and *Reduced motion* in the client options calms them as for every hero.

### 8. Looks

The **Look** tab has seven parts: **Skin**, **Name** and colours, **Cape**, **Hat**, **Mask**, **Eyes** and **Aura**.

* **Skin**: **My Own Skin**, **A Hero's Skin** (any look of any hero, as a base), or **A Skin File**: drop a 64x64 PNG skin on the window, or press **Pick a File...**; **Open Skins Folder** opens `enhanced/skins/` in your game folder. **Paint a copy of this skin** writes the chosen hero skin into that folder so you can paint over it and pick it back. **Arms: Slim / Wide** chooses the arm width.
* **Name and colours**: your hero's name (up to 24 characters; colour codes are removed, and a hero with no name is called "My Hero"), the **main colour** (beams, particles, the HUD, and the effects of the heroes' abilities) and the **glow colour**.
* **Extras on top of the skin** (all optional; a server can switch them off with `custom_hero.layers`):

| Extra | Styles | Your colour? |
|---|---|---|
| **Cape** | *Plain cape* (in any colour), or a hero's cape: Magneto, Scarlet Witch, Thor, Thor (Ragnarok), Mighty Thor, Thor (Love and Thunder), Loki, Loki (God of Stories), Vision, White Vision, Wiccan, Wiccan (comics), Billy (Halloween), Apocalypse | the plain cape |
| **Hat** | Witch's hat, Wiccan's crown, Cerebro's helmet, Magneto's helmet, Wanda's crown | the witch's hat and Wiccan's crown |
| **Mask** | Cyclops visor (comics), Cyclops visor (film), Ruby glasses, Quicksilver's goggles, Eye mask, Full mask | the two masks |
| **Eyes** | Glowing, Lightning, Fire, Optic red, Machine, All black; on one of four rows of the face | Glowing |
| **Aura** | Energy, Flames, Sand, Sparks, Mist | all but Sand |

* **When does it show?** The eyes and the aura can show **Always**, only while a **power is on**, or only while **flying**.
* The hat and the mask hide while you wear something on your head. Magneto's helmet and Wanda's crown are only drawn: they are not the real items, so wearing them awakens nothing.
* Everyone sees your extras, and they show on the preview. While Vision's Human Disguise is on, your hero looks like an ordinary person with no extras.

### 9. Become, save, share

Since 3.5, leaving the editor (Esc or Back) with changes you have not saved or become asks first: "Unsaved changes: discard them?" **Discard** goes back to the Hero Creator, **Keep editing** stays. **Become this hero!** never asks: your hero is sent, not lost.

The **Save** tab has **Become this hero!**, **Save to File** (also copies it), **Copy**, **Import from Clipboard**, and the list of your saved heroes in `enhanced/heroes/` (click one to load it; **Open Heroes Folder** opens the folder). You can also drop a hero `.json` file on the window. Before you become your hero it tells you what the server would change ("The server will change this: ..."), or that everything fits.

## Abilities and items

Most of the heroes' abilities need nothing but the ability. A few follow the rules of their hero's items:

* **Thor's Mjolnir Throw needs Mjolnir or Stormbreaker in your hand** to throw, call back or shatter, as for Thor. The ability also makes you **worthy**: you can lift Mjolnir (unless someone else owns it). Stormbreaker can be crafted by anyone.
* **Thor's Lightning Beam works bare-handed**; with Mjolnir or Stormbreaker in hand it reaches further and hits harder, as Thor's does. With Mjolnir Throw, the Lightning Beam, the God of Thunder or the Asgardian Body, lightning cannot hurt you, as it cannot hurt Thor.
* **Thor's Bifrost works without a weapon** for a custom hero: the key opens the Bifrost screen (Asgard since 3.5, home, your saved places, save this one) and **Sneak** takes you where you look. Its wait follows its strength. **Heimdall's Bifrost** (3.5) does the same. Either one takes everyone in its circle, as every Bifrost does.
* **Heimdall's Hofund Slash works with any sword** (3.5), and 30% harder with a Hofund. **Odin's Gungnir Blast** fires from your bare hand; his Throw needs a Gungnir in your hand. **Hela's** abilities need no item.
* **Cyclops's abilities need no glasses** for a custom hero. *Shut Eyes* / *Open Eyes* does what taking the glasses off and putting them back on does for Cyclops: **Sneak** opens your eyes and the giant beam pours out; the key without Sneak shuts them.
* **Spider-Man's Iron Spider Legs work in any look**, not only in the Iron Spider suit, and they make you an Iron Spider (metal) for Magneto. **J** switches Web Assist when you have his Web Zip.
* **Wanda's Hex**: pressing its key again opens Hex Control, **Sneak** dissolves it, and **Z** gives orders to its Vision. It dissolves when you die, lose the power or remake your hero without it.
* **Doctor Strange's abilities** (3.6) need nothing but **the Eye**: his Eye (on **Y**) works only with an **Eye of Agamotto** in your inventory, and shares the Time Stone's numbers. His Sling-Ring Portal (**N**) needs no ring. Choosing Doctor Strange gives you an Eye of your own and a Sling Ring.
* **The Hulk's abilities** (3.6) work in any look. His **Human Form** (`hulk.transform`) is your **N**: every power off and your own skin, until **N** again (a custom hero has no Bruce Banner and no second Hulk). His body makes you bigger.
* **Billy's Build Magic** is used with **Y**, by Billy's rules (the meter and the rank), without Wanda's Instant. **The Scarlet Witch's Build Magic** (3.5) is used with **Y** by hers: no meter, and her **Instant** switch. A hero may have only one of the two.

**Roles come with the abilities.** A custom hero with an ability of the Scarlet Witch, Billy, Loki or Agatha (or the Magic power) is a **sorcerer**: he may read the Book of the Vishanti and the Darkhold, and a Hex's Runes hold him. Professor X's abilities (and the Telepathy and Mind Control powers) make him a **telepath** of the Professor's rank; any ability of Wanda or Billy, and the Phoenix's Mind Reading and Mind Control, a stronger one. Vision's Density and body and Ultron's body make him **metal** for Magneto. Agatha's and (3.5) Hela's abilities are **dark magic**; Odin's Odinforce makes him a **sorcerer**, and so (3.6) do Doctor Strange's abilities. A god's ability (3.6: the Scarlet Witch's Hex, Loki's Frost Giant, or Thor's, the Phoenix's, Heimdall's, Odin's or Hela's body) makes him a **Bearer** of the [Infinity Stones](Infinity-Stones#who-may-hold-a-stone): he may hold them bare (a server can switch that off: `stones.customBearers`). The Phoenix's Body of Fire (and the Fire power) keep fire off him.

### The stone rows (3.6)

Each Infinity Stone is a card on the Powers tab: **Reality Stone** `stone.reality`, **Mind Stone** `stone.mind`, **Time Stone** `stone.time`, **Power Stone** `stone.power`, **Space Stone** `stone.space` and **Soul Stone** `stone.soul` (the full table is on [Heroes' Abilities](Hero-Abilities#the-infinity-stones-36)). Put one on a key: the key is its first power, **Sneak +** the key its second, as the bare stone's right-click and Sneak + right-click.

* **It works only while your hero carries that stone** and may use it: the stone itself in his inventory (a god's ability lets him hold the Reality, Power and Soul Stones; a sorcerer may hold the Time Stone), a Gauntlet with it that he may wield, or its vessel (Loki's Scepter for the Mind Stone, the Tesseract for the Space Stone, the Eye of Agamotto for the Time Stone). Otherwise: "You do not carry the Mind Stone." The card shows **Needs the stone**.
* **The Time Stone answers only a sorcerer**, as always.
* Its numbers are the stone's (the `[mind_stone]`... sections), changed by its strength and the server's `custom_hero.cooldownScale`. Its cooldown is the stone's own: the bare stone and the row share it.
* The Space Stone's row is a world power. The Mind and Power Stones' beams share the continuous beam, the Time Stone the slow time, and the Soul Stone Thanos's soul chain ([step 5](#5-powers-that-cannot-go-together)).
* A server can switch the stone rows off: `custom_hero.stoneRows`.

**When the power ends.** When your hero dies, logs out, loses his power or is remade, every ability ends cleanly: beams, shields, switches, drones, the Hex.

## The thirty powers

The research behind the list (which powers turn up most often in comics and films) counted the abilities of the most published Marvel and DC characters. Close variants are one power with options: Healing Factor and Regeneration, Phasing and Intangibility, Shapeshifting and Size Change, Energy and Power Absorption. The **first option is the default**. *Press*: once per press, then a cooldown. *Hold*: while the key is down. *On / off*: a switch. *Always on*: no key. Every number grows (or, for cooldowns and hunger, shrinks) evenly from strength 1 to strength 10; the table gives both ends. Damage is in points (2 points = one heart).

| # | Power (id) | Points a strength | Options | From strength 1 to 10 |
|---|---|---|---|---|
| 1 | Super Strength (`super_strength`) | 2 | Mighty Punch (press), Ground Slam (press), Super Leap (press) | Blows +1 to +8. Punch 6 to 18 damage, cooldown 3 to 1 s; Slam 4 to 12 damage round you, radius 3 to 7; Leap 1.0 to 2.6, cooldown 3 to 1 s |
| 2 | Durability (`durability`) | 1 | Armour Skin (always on), More Health (always on) | Armour 3 to 15 and toughness 1 to 8, knockback resistance 10% to 60%; or 4 to 30 more health |
| 3 | Super Speed (`super_speed`) | 2 | Super Run (on / off), Blitz Dash (press) | Run 25% to 160% faster; Dash 6 to 20 blocks, 3 to 10 damage to all in the way, cooldown 4 to 1.5 s |
| 4 | Flight (`flight`) | 2 | Hover, Jet, Wings (on / off) | Speed 0.06 to 0.13 (a ring of light, a trail of fire, or wings of light) |
| 5 | Healing Factor (`healing`) | 2 | Regeneration (always on), Healing Burst (press) | 0.25 to 2 health a second; or a burst of 4 to 20 health, cooldown 20 to 8 s |
| 6 | Energy Blast (`energy_blast`) | 2 | Beam (hold), Bolts (press), Burst (press) | Beam 1 to 4 a hit, 16 to 40 blocks; Bolts 3 to 12 damage, 20 to 48 blocks; Burst 4 to 14 damage round you, radius 3 to 8 |
| 7 | Super Senses (`super_senses`) | 1 | Night Vision (always on), Danger Sense (always on), X-Ray Vision (on / off) | Danger felt 12 to 40 blocks away; X-ray 12 to 48 blocks |
| 8 | Telepathy (`telepathy`) | 1 | Sense Minds (on / off), Read a Mind (press) | Minds glow through walls 12 to 48 blocks away; read a creature 12 to 40 blocks away |
| 9 | Teleportation (`teleport`) | 2 | Blink (press), Behind You (press) | Blink 8 to 48 blocks, cooldown 4 to 0.8 s; behind a creature 8 to 32 blocks away |
| 10 | Telekinesis (`telekinesis`) | 2 | Creatures (press), Everything (press, blocks too) | Reach 8 to 32 blocks, throws 0.6 to 1.6 times as hard |
| 11 | Wall-Crawling (`wall_crawl`) | 1 | Steady, Fast (on / off) | Climbs at 0.12 to 0.35 (Fast: half again) |
| 12 | Force Field (`force_field`) | 2 | Bubble (hold), Reflect (hold: shots go back) | Takes 10 to 80 damage before it breaks, lasts 5 to 20 s, cooldown 10 to 3 s |
| 13 | Phasing (`phasing`) | 3 | Through Walls, Ghost (on / off) | Hunger 0.5 to 0.1 a second |
| 14 | Animal Control (`animal_control`) | 1 | My Pack (press), Calm (press) | Radius 6 to 20, 5 to 30 s, cooldown 15 to 6 s |
| 15 | Mind Control (`mind_control`) | 3 | Ally (press), Calm (press) | Reach 8 to 24 blocks, 5 to 30 s, cooldown 30 to 10 s |
| 16 | Magic (`magic`) | 2 | Mystic Bolts (press), Healing Ward (press), Curse (press) | Bolts 3 to 11 damage; a ward of radius 3 to 8 for 5 to 15 s; a curse 5 to 20 s |
| 17 | Ice (`ice`) | 2 | Freeze Ray (press), Ice Path (on / off), Blizzard (press) | Ray 2 to 8 damage and frozen 2 to 10 s; path radius 1 to 5; blizzard 2 to 8 damage, radius 3 to 9 |
| 18 | Weather Control (`weather`) | 2 | Storm (press), Change the Weather (press) | Storm 4 to 12 damage a strike, radius 10 to 24, 5 to 20 s; weather cooldown 30 to 5 s |
| 19 | Lightning (`lightning`) | 2 | Strike (press), Chain Lightning (press) | Strike 5 to 16 damage, 20 to 48 blocks; chain 4 to 12 damage, 1 to 5 jumps |
| 20 | Shapeshifting (`shapeshift`) | 1 | Grow (on / off), Shrink (on / off), Creature (press) | Grow 1.25 to 3 times your size; shrink 0.75 to 0.25; take a creature's shape 8 to 24 blocks away |
| 21 | Invisibility (`invisibility`) | 2 | Full, Shimmer (on / off) | Hunger 0.6 to 0.1 a second (Full: even monsters lose you) |
| 22 | Absorption (`absorption`) | 2 | Energy (press), Powers (press) | Store 10 to 60 of fire, lightning and blasts and let it out (radius 3 to 8); or drain 3 to 10 damage and take a hero's powers for 3 to 12 s (PvP only) |
| 23 | Fire (`fire`) | 2 | Fireball (press), Flamethrower (hold), Fire Aura (on / off) | Fireball 3 to 10 damage, burns 3 to 10 s; flame 1 to 3 damage, 4 to 10 blocks; aura 1 to 3 damage, radius 2 to 5. Fire never burns you |
| 24 | Duplication (`duplication`) | 2 | Clones (press), Decoys (press) | 1 to 4 clones that fight, 8 to 30 s; or 1 to 5 decoys, 6 to 20 s |
| 25 | Water (`water`) | 1 | Water Jet (hold), Wave (press) | Jet pushes 0.4 to 1.6, 6 to 16 blocks; wave 2 to 8 damage, radius 3 to 8. You never drown |
| 26 | Time (`time`) | 3 | Slow Time (on / off), Stop Time (press), Rewind (press) | Slow radius 4 to 14; a creature stands still 2 to 10 s; back to where you were 3 to 10 s ago |
| 27 | Technopathy (`technopathy`) | 1 | Hack (press), Command Golems (press) | Doors, gates and levers flip within 3 to 12 blocks; golems fight for you 10 to 30 s |
| 28 | Sonic Scream (`sonic_scream`) | 2 | Scream (press), Shockwave (press) | Cone 3 to 12 damage, 6 to 20 blocks; ring 2 to 10 damage, radius 4 to 12 |
| 29 | Earth (`earth`) | 2 | Tremor (press), Spikes (press) | Tremor 3 to 12 damage, radius 3 to 10; spikes 4 to 14 damage in a line 6 to 20 blocks long |
| 30 | Wind (`wind`) | 1 | Gust (press), Updraft (press) | Gust pushes 0.8 to 3, 5 to 14 blocks; updraft lifts 0.7 to 2. Softer falls |

The powers reuse the mod's own: the Energy Blast beam is the beam of Billy and the Darkhold in your colours, Telekinesis is Wanda's, Phasing is Vision's, the Creature shape is Loki's, the Clones are Loki's duplicates that fight, Stop Time is Billy's Freeze, and Super Run gives you Quicksilver's trails. No power of a custom hero crosses a Hex wall or a Chaos Shield, area powers spare what the other heroes' area attacks spare (allies, and players when PvP is off), and teleports respect arenas, the Witches' Road and the Hex like every other teleport. Earth changes no block, and the Sonic Scream breaks glass only if the server allows it (`custom_hero.blockChanges`).

## Your skin, seen by everyone

A custom skin must be a **PNG of exactly 64x64 pixels**, at most **16 KB** (`custom_hero.maxSkinBytes`, up to 24 KB). The server checks it and **sends it to every player**, so everyone sees your hero as you drew it. A skin that is too big, not a PNG or the wrong size is refused, and your hero wears your own skin instead.

## Saving, sharing and loading: the JSON file

**Save to File** writes your hero to `enhanced/heroes/<name>.json` in your game folder and copies the same text to the clipboard. **Copy** only copies it. Paste it anywhere to share your hero; a friend loads it with **Import from Clipboard** or by putting the file in their own `enhanced/heroes/` folder. It works on any server: that server's own limits decide what is kept.

Since 3.4 the file is **version 2**:

```json
{
  "format": "enhanced:custom_hero",
  "version": 2,
  "name": "Storm Girl",
  "color": "#3FA9F5",
  "glow": "#E0F4FF",
  "look": {
    "base": "thor/ragnarok", "slim": false,
    "cape": {"style": "plain", "color": "#B01E23"},
    "eyes": {"style": "glow", "color": "#9FE8FF", "when": "powers"},
    "aura": {"style": "energy", "when": "flying"}
  },
  "powers": [
    {"id": "thor.hammer", "strength": 5, "option": "both"},
    {"id": "flight", "strength": 5, "option": "jet"},
    {"id": "thor.lightning", "strength": 6, "option": "both", "stance": "auto"},
    {"id": "energy_blast", "strength": 4, "option": "bolts", "color": "#FF4040"},
    {"id": "thor.body", "strength": 5, "option": "both"}
  ],
  "keys":      ["thor.hammer", "flight", "thor.lightning", "energy_blast", ""],
  "sneakKeys": ["thor.hammer#alt", "", "", "energy_blast#burst", ""],
  "moves": {"b": "billy_bolt", "sneak_b": "phoenix_psionic_cast"},
  "anim": {"flight": "thor", "run": "thor", "speedRun": "auto", "idle": "crackle"}
}
```

| Field | Meaning |
|---|---|
| `format`, `version` | Always `enhanced:custom_hero` and `2`. A file from a newer version is read as far as it is understood ("some of it may be left out"). |
| `name` | The hero's name (up to 24 characters). |
| `color`, `glow` | The main and glow colours, as `#RRGGBB`. |
| `look` | `base` is `own` (your own skin), `skin` (the skin in the file) or a hero's look as `<hero>/<look>`, for example `thor/ragnarok` or `agatha/all_along` (Magneto, the Scarlet Witch and Quicksilver: `magneto/suit`). `slim` is the arm width. Then, only when they are not none: `cape`, `hat`, `visor` (the mask), `eyes` and `aura`, each with a `style` (the ids below), a `color` (`#RRGGBB`; left out: the hero's colour), for the eyes a `row` (0 to 3), and for the eyes and the aura a `when` (`always`, `powers` or `flying`). |
| `skin` | Only with `"base": "skin"`: the 64x64 PNG, written as base64. |
| `powers` | Each power: its `id` (a power of the thirty, such as `flight`, or a hero's ability, such as `thor.lightning`: [the list](Hero-Abilities)), its `strength` (1 to 10; a hand-written `"level": 1`, `2` or `3` reads as 3, 6 or 10; none at all reads as 5), its `option` (a power of the thirty: one of its options; a hero's ability: `both`, or `main` to switch its Sneak variant off), an optional `color` of its own (`#RRGGBB`; since 3.5 any power may have one, left out for the hero colour), and an optional `stance` for a held or switched-on power (`auto`, `none` or a stance id). |
| `keys` | Five ids, for **R G V B N** in that order; `""` leaves a key empty. |
| `sneakKeys` | The five **Sneak +** boxes, in the same order: `"<id>#alt"` is a hero's ability's Sneak variant (put there by itself), `"<id>"` a power on its own, `"<power>#<option>"` another press option of the power on the plain key. |
| `moves` | The move of each box that is not automatic: keys `r`, `g`, `v`, `b`, `n` and `sneak_r` ... `sneak_n`; values `none` or a move id (`billy_bolt`, `thor_summon`, `thanos_slam`...). |
| `anim` | `flight` (`plain`, `magneto`, `wanda`, `vision`, `white_vision`, `billy`, `thor`, `phoenix`, `ultron`, `apocalypse`, `agatha`), `run` (`default`, a hero such as `thor` or `spider`, `xavier_seated`...), `speedRun` (`qs_time`, `qs_speed`, `tommy`) and `idle` (`none`, `calm`, `ready`, `regal`, `heavy`, `jotun`, `crackle`, `menacing`); `auto` for any of them. |

The look ids: capes `plain`, `magneto`, `scarlet_witch`, `thor`, `thor_ragnarok`, `thor_mighty`, `thor_love_and_thunder`, `loki`, `loki_god_of_stories`, `vision`, `white_vision`, `billy_wiccan`, `billy_comics`, `billy_halloween`, `apocalypse`; hats `witch_hat`, `billy_crown`, `cerebro`, `magneto_helmet`, `wanda_crown`; masks `cyclops_classic`, `cyclops_apocalypse`, `ruby_quartz`, `quicksilver_goggles`, `domino_mask`, `full_mask`; eyes `glow`, `lightning`, `fire`, `optic`, `machine`, `black`; auras `energy`, `flames`, `sand`, `sparks`, `mist`.

**Files from 3.3 (version 1) load by themselves.** A 3.3 file, a 3.3 hero copied to the clipboard, and the hero saved with your player in a 3.3 world are all turned into version 2 when they are read: the powers, the keys and the look stay; the Sneak boxes are empty, every move and animation is automatic, and there are no extras; the next save writes version 2. The other way round, 3.3 can read the core of a 3.4 file (name, colours, skin, the thirty powers and the plain keys) and leaves out what it does not know.

## What the server checks

When a hero is made, chosen or loaded, the server judges it by its own rules. It never refuses a hero outright: it makes the best hero the rules allow and tells you what it changed. In this order:

1. The name is cleaned and the colours are made solid.
2. Unknown powers, powers the server switched off (`custom_hero.disabledPowers`), the heroes' abilities if `custom_hero.heroModules` is off, the world powers if `custom_hero.worldPowers` is off, and repeated powers are left out.
3. Strengths are kept between 1 and the server's highest (an ability with no slider is always 5); an unknown option becomes the default; a stance a power cannot take becomes automatic.
4. An ability whose needed power is missing is left out ("left out: it needs ...").
5. A power that drives what a power of another hero already drives is left out ("left out: it uses the same shield as ...").
6. Powers past the number, then past the budget, are left out from the end (the last picked go first).
7. The keys are checked: a box whose power is missing, always on, or not allowed there is emptied; a hero's ability's Sneak variant goes on the Sneak box of its key.
8. Moves and animations that name nothing known become automatic; extras that are not allowed (`custom_hero.layers`) or not known are taken off.
9. A bad skin, or an unknown hero look, falls back to your own skin.

Unknown fields are ignored, so a hand-written file loads as far as it can. The editor runs the same rules with the server's limits, so what you see is what you get.

## Playing your hero

* Your keys do what you put on them; each key has its own cooldown, shared by its plain and Sneak boxes. The top-left panel shows your hero's name in your colours, each key's power, a `[⇧R]` line under a key for its Sneak box, whether each switch is on, your shields' strength, and an *Always on* line for the powers that need no key.
* Beams, particles and the HUD use your colours, or a power's own colour if you gave it one (since 3.5 any power, a hero's ability too); the heroes' abilities use your colours where the hero used his own.
* A custom hero flies only with the **Flight** power (on its key, or double-tap **Space**). Vision's **Phase** also lets you float while it is on, and Vision's **Density** stops you flying while it is on.
* **Stop Being a Hero** takes your power away; your hero stays saved for later.
* An operator can also give it with `/enhanced power set <player> custom`: the player becomes the hero they last made (an empty hero if they never made one). See [Commands](Commands).

## For servers

Section `[custom_hero]`: switch the creator off, allow operators only, turn off Choose a Hero (and, since 3.4, the items it gives), change the budget (90), the number of powers (10), the highest strength, the skin size, the powers that are not allowed (a power of the thirty, a hero's ability such as `thor.lightning`, or a whole hero such as `thor.*`), the wait between heroes, whether the heroes' abilities, the world powers and the look extras may be used, how much strength changes a hero's ability (`heroStrengthSpread`), and scale every custom power's damage, cooldowns and hunger (and the flight speed, the Force Field, the control times, the teleport reach and how long Power Absorption takes a hero's powers), whether custom powers may change blocks and whether Weather Control changes the world's weather. Every option is read when it is used; a hero already made is judged by the new limits the next time it is saved. A world whose server file was written by 3.3 keeps its `budget = 60` and `maxPowers = 8` (the game never rewrites a value already in the file): set 90 and 10, or delete the two lines, to get the new defaults. All the ready-made heroes fit in 60. See [Server Configuration](Server-Configuration#the-hero-creator).

## Related

[Heroes' Abilities](Hero-Abilities) · [Becoming a Hero](Becoming-a-Hero) · [Looks](Looks) · [Controls](Controls) · [Items and Crafting](Items-and-Crafting#hero-creator)

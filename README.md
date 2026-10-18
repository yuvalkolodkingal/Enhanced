# Enhanced

A Minecraft mod where you can become one of the *Enhanced*:
**Magneto**, the Master of Magnetism; **Wanda Maximoff, the Scarlet Witch**;
**Quicksilver**, who slows the world down around him (to *Sweet Dreams*) or just outruns it;
**Vision**, the Mind Stone in a synthetic body (or **White Vision**);
Wanda's sons **Tommy (Speed)** and **Billy (Wiccan)**; **Loki**, the God of Mischief;
**Jean Grey, the Phoenix**, who reads minds and burns with cosmic fire;
**Peter Parker, Spider-Man**, who goes everywhere on a web;
or, if you are worthy, **Thor**, with **Mjolnir** and **Stormbreaker**;
**Agatha Harkness**, whom the Darkhold makes; **Cyclops**, whose eyes only his ruby-quartz glasses hold back;
or **Professor X**, young or old, who hears every mind and takes over bodies. In Creative mode the **Hero Creator**
turns you into any hero, or into a hero of your own.
Three villains are bosses, and their powers are won only by beating them: **Thanos**, the Mad Titan,
who gathers up to five Infinity Stones as he fights; **Ultron**, the AI of the Mind Stone, who lifts the ground
into the sky and whose Core makes you Ultron; and En Sabah Nur, **Apocalypse**, a boss you beat to earn his Essence.

Every power's look and colour follows the MCU films and shows, and Vision, Tommy, Billy, Loki
(in his Asgardian clothes, the TVA's or the God of Stories'), Thor, the Phoenix, Spider-Man, Thanos, Ultron, Apocalypse, Agatha, Cyclops and Professor X each have several looks
(press **H**).

**New in 3.3:** three new heroes: [Agatha](docs/wiki/Agatha.md) (read the Darkhold with no power: its purple beam, flight, the Power Drain, a witch's shield and a Rune Trap), [Cyclops](docs/wiki/Cyclops.md) (put on his ruby-quartz glasses: the Optic Blast, and a giant beam whenever they come off) and [Professor X](docs/wiki/Professor-X.md) (craft his wheelchair: hear thoughts, take over bodies, Cerebro; young or old); the [Hero Creator](docs/wiki/Hero-Creator.md) (Creative: become any hero, or make your own from thirty powers with a strength each, your keys, colours and skin, saved as JSON); [Hydra outposts, Mjolnir's craters and the Mutant School](docs/wiki/Structures.md) in the world (Loki's Scepter is now found only in the outposts' vaults); Billy's Hex Barrier stops everything from the front, with 30 health and no time limit, and chaos magic shatters it; Wanda's Hex lets in only those she approves and no power crosses its wall, and her **Hex Runes** silence every other sorcerer inside; the ward of the Book of the Vishanti keeps out monsters, bosses, what they shoot and dark magic; the Witches' Road plays an original ballad in a new arrangement at each stage; Quicksilver and Tommy each get separate **slow time** (R) and **super speed** (G) keys, **run up walls and along ceilings**, and new attacks (Dash Strike, Pebble Barrage, Sonic Boom); Spider-Man's **Web Assist** makes swinging easy in forests and open land (J switches it); and smoother animations everywhere.

**New in 3.2:** the Witches' Road has three **trials** (a parkour over the void, a structure to copy block by block, an ice-boat race; see [The Witches' Road](docs/wiki/Witches-Road.md)) and you choose Billy or Tommy at the end; Billy fires a continuous **Hex Beam** (hold **R**), raises a **Hex Barrier** (Sneak + **B**) and ranks up to **ten** ranks; Wanda's Chaos Shield is **unlimited** and she can build **instantly** (Ctrl + I); sorcerers can craft the **Book of the Vishanti** (saved places, and a ward round the base that no monster can enter) and the **Darkhold** (Agatha's purple beam; see [The Sorcerers' Books](docs/wiki/Sorcerers-Books.md)); and Apocalypse is weaker (one master factor, `apocalypse_boss.powerFactor`).

**New in 3.1:** Wanda and Billy build with **Build Magic** (press **Y**; eleven spells, and **Ctrl + Z** takes it back, even tomorrow; see the [Build Magic page](docs/wiki/Build-Magic.md)), every telekinesis works in the boss fights and Billy moves blocks, Magneto pulls the metal out of everything around him, and Apocalypse never heals while Wanda and the Phoenix hit him critically.

Original hero concepts by Eilon Kolodkin-Gal. The full design, with every rule and
number, is in [docs/DESIGN.md](docs/DESIGN.md) (Hebrew: [docs/DESIGN.he.md](docs/DESIGN.he.md)).

**Wiki:** the player's guide, in English and Hebrew, is on the [wiki](../../wiki). Its pages live in
[docs/wiki](docs/wiki) and a GitHub Action publishes them, so edit them there.

**Download:** every push to `main` builds the mod, runs the in-game tests and publishes the jar
on the [Releases](../../releases) page. The newest build is always marked *Latest*.

![The heroes](docs/screenshots/all-heroes.png)

| Magneto | Scarlet Witch | The Hex |
|---|---|---|
| ![Magneto](docs/screenshots/magneto.png) | ![Scarlet Witch](docs/screenshots/scarlet-witch.png) | ![The Hex](docs/screenshots/hex.png) |

| Quicksilver | Vision | Quicksilver Time |
|---|---|---|
| ![Quicksilver](docs/screenshots/quicksilver.png) | ![Vision](docs/screenshots/vision.png) | ![Quicksilver Time](docs/screenshots/quicksilver-run.png) |

* **Minecraft** 1.21.1
* **Mod loader** NeoForge 21.1.x
* **Install it on** the client, plus the server for multiplayer

## Becoming a hero

| Hero | How |
|---|---|
| **Magneto** | Craft **Magneto's Helmet** from 5 Netherite Blocks (helmet shape) and put it on. |
| **Scarlet Witch** | Find the very rare **Scarlet Witch Gene** (mansions, strongholds, fortresses, bastions, ancient and End cities, jungle temples, desert pyramids, witches, evokers). Hold it in one hand and use **Loki's Scepter** (in the vault of a Hydra outpost) with the other to touch the Mind Stone (**without the gene, you explode**), or use the **Tesseract** with the other hand instead. |
| **Quicksilver** | Craft **Quicksilver's Goggles** (sugar, iron, glass panes, a Block of Diamond) and put them on. |
| **Vision** | Craft a **Synthezoid Core** (redstone blocks, netherite ingots, a Nether Star). Hold it in one hand and use **Loki's Scepter** with the other. |
| **Tommy** or **Billy** | Craft the **Witches' Road Door** (amethyst shards, soul lanterns, dark oak trapdoors, an Eye of Ender) and place it in the floor. Open it holding a **Scarlet Witch Gene** (with no power), walk the three trials of the Road, and choose which of Wanda's sons you become. |
| **Loki** | Find the **Tesseract** (End City, Stronghold library, Ancient City and Woodland Mansion chests) and use it: you vanish in a blue flash and reappear far away as Loki. (With a **Scarlet Witch Gene** in your other hand it awakens the Scarlet Witch instead.) |
| **Thor** | Forge **Mjolnir** (netherite ingots, a Nether Star, a Heavy Core, a Breeze Rod), or find one in a **Mjolnir's crater**. The first **worthy** player to lift it (Heroes of the Village: save a village from a raid) claims it, and lightning makes you Thor. From then on only its owner can lift it. |
| **Phoenix** | Find the very rare **Phoenix Ember** (End City and bastion treasure, Ancient City, stronghold library, Nether fortress and woodland mansion chests), hold it with no power and **hold right-click for 3 seconds**. Mind Reading (**R**) shows the items of whoever you look at, **Sneak + R** senses every mind through the walls, **B** takes over a creature, **Sneak + B** freezes everyone around you, and **N**, held, charges the Phoenix Burst: a wave of cosmic fire in every direction. |
| **Spider-Man** | Craft a **Radioactive Spider** (Fermented Spider Eyes, Glow Ink Sacs and a Slime Block) or find one in a mineshaft, dungeon, Trial Chambers or Ancient City chest, and **right-click with it** with no power: you are sick for 7 seconds and awaken. **R** webs what you look at (**Sneak + R**: a burst of 24 strands), **G** zips you to the block you look at (**Sneak + G**, held: swing), jump at a wall and push to climb it, **V** switches Wall Crawl, and a spider-sense warns of danger. The Iron Spider suit has **B**, four mechanical legs (twice **Sneak + B**: Instant Kill), and **N** turns his eyes black. |
| **Thanos** | Hold **the Tesseract and Loki's Scepter** together for five seconds in open country to call the **Mad Titan**, beat him (he takes a new Infinity Stone every 90 seconds) and hold the **Infinity Gauntlet** he rewards for three seconds with no power to become **Thanos**: five Infinity Stones on five keys (Power Blast, Portal Step, Transmute, Soul Drain, Rewind, and a Sneak ability each), no flight and no Snap. |
| **Ultron** | Beat the **Ultron** boss and take his **Core**. Craft the **Ultron Program** (a Nether Star, 4 Blocks of Iron, 2 Echo Shards, 2 Blocks of Redstone), hold it in one hand and **Sneak + right-click** open, level ground with **Loki's Scepter** in the other: he rises among six Sentries, then the ground you stand on lifts into the sky. Everyone who fights for a minute and hurts him or his Sentries gets a **Core**; hold it in one hand and use the Scepter with the other (with no power) and you become Ultron. |
| **Apocalypse** | Craft a **Capstone** (a Nether Star, five Gold Blocks, a Lapis Block) and Sneak + right-click it on sand by day with a Scarlet Witch or a Phoenix within 64 blocks to wake **En Sabah Nur**: a boss in a golden shield that stops everything but mind power, with sand, portals and his Horsemen. Every fighter who stayed a minute and hurt him or his shield gets the **Essence**; hold it for three seconds with no power to become **Apocalypse**: sand waves and entombing, levitation, a portal, a golden shield of your own and power bestowal (Sneak: a sandstorm). |
| **Agatha** | Craft the **Darkhold** (netherite scrap, a wither rose, echo shards, a book, a wither skeleton skull) and, with no power, **hold right-click on it for 3 seconds**: the book's purple beam (**R**; Sneak: hex mark or mind glimpse), flight, 40 health, the **Power Drain** (**V**: a hero's powers gone for 10 seconds), the **Witch's Shield** (**B**, hold) and the **Rune Trap** (**N**). Magic used on her poisons whoever used it. |
| **Cyclops** | Find his **Ruby-Quartz Glasses** (Scott Summers' room in the **Mutant School**; rarely woodland mansions and stronghold libraries) and **put them on** with no power: the **Optic Blast** (hold **R**; Sneak: Ricochet Shot), Concussion Blast, Optic Leap, Rally. Take them off (**Sneak + N**) and a giant beam pours out until you put them back on (**N**). |
| **Professor X** | **Craft his Wheelchair** (3 leather, an Eye of Ender, 2 iron ingots) or find it in the **Mutant School**, and with no power **hold right-click on it for 3 seconds**; choose the young or the old Charles (who sits in the chair): the Mental Blast, **Hear Thoughts**, **Take Over a Body** (not a telepath: the Scarlet Witch, the Phoenix and Billy throw him out), Freeze Minds and **Cerebro**. |
| **Your own hero** | In Creative mode (or as an operator), use the **Hero Creator**: become any hero at once, or make your own from a catalogue of thirty powers, each with a strength from 1 to 10 within a budget, on the keys you choose, in your colours and your own 64x64 skin (everyone sees it). Save it to `enhanced/heroes/<name>.json` and load it anywhere. |

**Loki's Scepter** is a weapon too (8 damage), and a hit confuses whoever it lands on: a creature forgets its
target and stumbles about for 6 seconds (the Ender Dragon, the Wither and the Warden are immune), and a player gets
Nausea for 8 seconds and Slowness for 3.

The **Infinity Stones** have voices of their own: the Space Stone in the Tesseract is a deep, swelling cosmic hum, and
the Mind Stone in Loki's Scepter and in Vision's forehead a bright, shimmering, slightly eerie chime-hum. They sound
under the portal, the awakening or the chime they come with, and hum softly for as long as you hold the Tesseract or
the scepter (the *Players* volume slider turns them down). The Power, Reality, Soul and Time Stones have voices too:
the Mad Titan sounds each one as he gathers it, and a Thanos player hears them at every use of a stone.

The **Book of the Vishanti** (amethyst shards, an Eye of Ender, gold ingots, a book and a lapis lazuli) lets a sorcerer (Loki, the Scarlet Witch, Billy or Agatha) save places and teleport to them (use it; Sneak + use goes to your base) and raise a **ward** round a base that keeps out every monster and boss, what they shoot, and dark magic, as large as the sorcerer is strong. The **Darkhold** (netherite scrap, a wither rose, echo shards, a book and a wither skeleton skull) gives a sorcerer Agatha's purple beam (hold use), poisons whoever uses magic on its holder for 10 seconds, and has a hex mark and a mind glimpse (Sneak + use); a player with no power who reads it becomes Agatha.

**Stormbreaker** (3 netherite ingots, a Nether Star and any log, shaped like an axe) can be wielded by
anyone: the hardest-hitting axe there is, a fifth stronger than Mjolnir in everything they share (the blow, the
throw, the lightning beam). Both are drawn big, as in the films. Whoever holds Mjolnir or Stormbreaker can **call the Bifrost**:
right-click for the Bifrost screen, which takes you home or to any place you saved, **in any
dimension** (save up to 24 places, by name, where you stand), or Sneak + right-click to go where you
look. In Thor's hands either weapon is thrown, called back and flown with, and makes his lightning beam
twice as strong through Mjolnir, 2.4 times through Stormbreaker. Mjolnir can also **shatter** into fragments that strike his enemies (Sneak + R), and its
owner can **pin someone down** with it (Sneak + right-click on a creature or player).

## Controls

| Key | Magneto | Scarlet Witch | Quicksilver | Vision |
|---|---|---|---|---|
| **R** | Metal Telekinesis (Sneak: Magnetic Pull) | Chaos Energy Beam (hold) | Quicksilver Time on/off (Sneak: Walkman) | Mind Stone Beam (hold) |
| **G** | Iron Shrapnel Storm (hold) | Enhanced Flight | Super Speed on/off (Sneak: Dash Strike) | Flight |
| **V** | Armor & Weapon Strip | Telekinesis (creatures, items, blocks; Sneak: a block) | Speed Blitz (Sneak: Pickpocket) | Phase through walls (toggle) |
| **B** | Magnetic Shield (toggle) | Chaos Shield (hold; unlimited) | Wall & Water Run: walls, ceilings, water (toggle) | Density (toggle) |
| **N** | Magnetic Flight | The Hex (again: Hex Control, Sneak: dissolve) | Pebble Barrage | Human disguise (toggle) |
| **Y** | | Build Magic (tap: on or off; hold: the Spell Wheel; Ctrl + I: Instant) | | |

| Key | Tommy | Billy | Loki | Thor |
|---|---|---|---|---|
| **R** | Slow Time on/off (Sneak: Wall Run on/off) | Hex Beam (hold) | Daggers (Sneak: three) | Throw / call back (Sneak: shatter Mjolnir) |
| **G** | Super Speed on/off (Sneak: Dash Strike) | Flight | Duplicates | Flight (weapon in hand) |
| **V** | Whirlwind (Sneak: Sonic Boom) | Telekinesis (creatures, items, arrows, blocks; Sneak: a block) | Shapeshift | Lightning beam (hold) |
| **B** | Vibrate Through Walls (Sneak: Disarm Blitz) | Freeze (Sneak, hold: Hex Barrier) | Vanish | God of Thunder (toggle) |
| **N** | Molecular Acceleration | The Noise (toggle) | Frost Giant | Bifrost screen: home, saved places, save this one (Sneak: where you look) |
| **Y** | | Build Magic (tap: on or off; hold: the Spell Wheel) | | |
| **H** | Change look | Change look | Change look | Change look |

| Key | Phoenix |
|---|---|
| **R** | Mind Reading on/off (Sneak: Mind Sense through walls) |
| **G** | Flight |
| **V** | Telekinesis (creatures, items, arrows, blocks; Sneak: a block) |
| **B** | Mind Control (Sneak: Psionic Field) |
| **N** | Phoenix Burst (hold to charge, let go to unleash) |
| **H** | Change look |

| Key | Spider-Man |
|---|---|
| **R** | Web Shot (Sneak: Web Burst) |
| **G** | Web Zip (Sneak, hold: Web Swing) |
| **V** | Wall Crawl on/off (Sneak: Spider-Sense on/off) |
| **B** | Spider Legs (Iron Spider only; Sneak, twice: Instant Kill) |
| **N** | Black Eyes |
| **H** | Change suit |

| Key | Thanos | Ultron | Apocalypse |
|---|---|---|---|
| **R** | Power Blast (Sneak: Titan Slam) | Repulsor Fire (hold; Sneak: Overcharge) | Sand Wave (Sneak: Entomb) |
| **G** | Portal Step (Sneak: Space Pull) | Anti-Gravity Flight (toggle) | Levitation (toggle) |
| **V** | Transmute to stone (Sneak: Reality Veil, toggle) | Gravity Pull (Sneak: Gravity Slam) | Portal |
| **B** | Soul Drain (hold; Sneak: Soul Sight) | Upload (Sneak: Hack; Second Life) | Shield (hold) |
| **N** | Rewind (Sneak: Time Lock) | Sentry Army (Sneak: Command) | Power Bestowal (Sneak: Sandstorm) |
| **H** | Change look | Change look | Change look |

| Key | Agatha | Cyclops | Professor X |
|---|---|---|---|
| **R** | Purple Beam (hold; Sneak: Hex Mark or mind glimpse) | Optic Blast (hold; Sneak: Ricochet Shot) | Mental Blast |
| **G** | Flight | Concussion Blast | Hear Thoughts (toggle) |
| **V** | Power Drain | Optic Leap | Take Over a Body (again: return) |
| **B** | Witch's Shield (hold) | Rally | Freeze Minds |
| **N** | Rune Trap | Glasses on (Sneak: take them off) | Cerebro |
| **H** | Change look | Change look | Young or old Charles |

A custom hero of the Hero Creator has the powers you chose on **R G V B N**.

**Quicksilver Time:** as Quicksilver, press **R**. Everything within 32 blocks slows to a fifth of its speed
(arrows hang in the air) and the music starts, whether you run or just stand there. **G** is **Super Speed**, a
separate power: sprint, and you run four times faster. Use one, the other, or both (the world slowed while you run
through it). While either is on you also mine, attack, eat, shoot a bow and swim four times as fast. Tommy has the
same two: **R** Slow Time (teal) and **G** Super Speed. With super speed on, both **run up walls and along
ceilings** when they sprint into a wall.

**Web Assist:** Spider-Man's **Sneak + G**, held, swings on a web that finds its own anchor in trees, houses and
cliffs, never snags on the ground or the leaves, and sends the next web while **G** is held. **J** switches it to the
manual swing.

**The Hex's Vision:** when the Scarlet Witch raises a Hex, a Vision appears at her side and fights for her
while it stands (he can never leave it). Look at a creature and press **Z** to send him at it, at nothing to
call him back, **Sneak + Z** to make him hold his post. Vision himself, inside a standing Hex, can't leave it
either: he comes apart a little at the wall and is thrown back.

Every hero except Quicksilver, Tommy, Loki, Spider-Man, Thanos, Cyclops and Professor X (and a custom hero without Flight) can also fly by double-tapping **Space** (Thor only with
Mjolnir or Stormbreaker in hand). With telekinesis, press the key again
to launch what you hold, or **Sneak** + the key to put it down gently. Keys can be changed
under *Options → Controls → Enhanced Powers*. The top-left corner of the screen always
shows your abilities, cooldowns and resources.

**Client options** (*Mods → Enhanced → Config*, or `config/enhanced-client.toml`): *Hero animations*
(off: vanilla poses only), *Reduced motion* (calmer hover, no trembling or camera shake), *Flight
effects* (off: no trails, rings or wind) and *Hide heroes' armour* (on by default: heroes are drawn
without their armour, which still protects them).

## Screenshots

![The Scarlet Witch](docs/screenshots/scarlet-witch-sheet.png)

| | |
|---|---|
| ![Lifting an Iron Golem](docs/screenshots/magneto-telekinesis.png) Magneto lifting an Iron Golem | ![Disarm](docs/screenshots/magneto-disarm.png) Ripping a zombie's iron gear off |
| ![Chaos Beam](docs/screenshots/chaos-beam.png) The Chaos Beam | ![Chaos Shield](docs/screenshots/chaos-shield.png) The Chaos Shield |
| ![Awakening](docs/screenshots/mind-stone-awakening.png) Touching the Mind Stone with the gene | ![Hex Control](docs/screenshots/hex-control.png) Wanda's Hex Control screen |
| ![Flight](docs/screenshots/scarlet-witch-flight.png) Red energy around Wanda's hands in flight | ![Quicksilver Time](docs/screenshots/quicksilver-time.png) Quicksilver Time: sand and arrows hang in the air |
| ![Running past](docs/screenshots/quicksilver-run.png) Quicksilver running past, as another player sees him | ![Mind Stone beam](docs/screenshots/mind-stone-beam.png) Vision's Mind Stone beam |
| ![Phasing](docs/screenshots/vision-phase.png) Vision phasing through a wall | ![Disguise](docs/screenshots/vision-disguise.png) Vision and his WandaVision human disguise |

## Quicksilver's song

Quicksilver's Walkman plays *Sweet Dreams (Are Made of This)* by Eurythmics, which is built into
the mod (`src/main/resources/assets/enhanced/sounds/quicksilver/sweet_dreams.ogg`).

The recording is copyrighted, so this repository and the mods built from it are for personal use
only. Keep the repository private and don't share the built jar publicly.

To play a different song, start the game once (this creates the folder `config/enhanced/music/` in
your Minecraft folder) and put an `.mp3`, `.ogg` or `.wav` file there. The first file in
alphabetical order is played; remove it to go back to *Sweet Dreams*.

## Building

You need Java 21.

GitHub Actions (`.github/workflows/build.yml`) runs the same build and the GameTests on every
push and pull request, and publishes a release for every push to `main` (or a `v*` tag).

```sh
./gradlew build              # the mod jar ends up in build/libs/
./gradlew runClient          # play in a development client
./gradlew runGameTestServer  # run the automated in-game tests
```

To install, copy `build/libs/enhanced-<version>.jar` into the `mods` folder of a NeoForge
1.21.1 instance.

How the heroes and bosses of 3.0 hang on the shared code (the plug-ins, the event priorities, the
boss framework and its rules) is in [docs/HEROES-AND-BOSSES.md](docs/HEROES-AND-BOSSES.md).

## Credits

* Hero concepts: Eilon Kolodkin-Gal.
* Scarlet Witch skin: the community skin
  ["Scarlet Witch"](https://www.minecraftskins.com/skin/18465369/scarlet-witch/) from minecraftskins.com.
* *Sweet Dreams (Are Made of This)*: Eurythmics. Bundled for personal use only.

## For server owners

* `/enhanced power get|set …` (`none`, `magneto`, `scarlet_witch`, `quicksilver`,
  `vision`, `tommy`, `billy`, `loki`, `thor`, `phoenix`, `spider_man`, `thanos`, `ultron`, `apocalypse`, `agatha`, `cyclops`, `professor_x`, `custom`, optionally followed by a look), `/enhanced look <players> <look>`
  (for example `/enhanced look @a tva`), `/enhanced hex list|clear` and `/enhanced boss start <thanos|ultron|apocalypse> [force]`,
  `/enhanced boss stop`, `/enhanced boss list` and `/enhanced boss cleanup <center> <radius>`, which start, end, list and clean up
  boss fights, `/enhanced ward list|remove <player>` for the wards of the Book of the Vishanti, and `/enhanced road status|enter|leave|reset|skip|set|layout|cells` for the Witches' Road (operators only; everyone can type `/road leave` and `/road status`).
* Balance numbers (beam damage, Hex death chance and radius, the Vision of the Hex, flight speeds, shrapnel,
  how much Quicksilver Time slows the world, the Phoenix's Mind Reading, Mind Control and Burst, Spider-Man's webs, zip, swing, crawl, suits, legs, Instant Kill and Black Eyes, the rules every boss fight shares (`[boss]`), the Mad Titan's clock of stones and every move, Ultron's bodies, Sentries and the Sokovia lift, Apocalypse's shield, attacks and Horsemen and his master factor, and the three bosses' heroes, Billy's Hex Beam and Hex Barrier, the ten Build Magic ranks and Wanda's Instant level, the Book of the Vishanti and its wards, the Darkhold, the Witches' Road trials, and in 3.3 Agatha, Cyclops, Professor X, the Hex Runes and the Hero Creator's budget and limits…) are in `world/serverconfig/enhanced-server.toml`. The three structures of 3.3 are tuned by data packs (structure sets and biome tags).
* Item, block and entity tags under `data/enhanced/tags/` decide what counts as
  metal, what can be lifted, what the shield repels and which bosses resist the Hex,
  so datapacks can change them.

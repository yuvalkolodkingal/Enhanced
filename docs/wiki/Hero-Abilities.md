# Heroes' Abilities

Since 3.4 every ability of every hero of the mod is a power a [custom hero](Hero-Creator) can pick in the Hero Creator: **81 abilities** of sixteen heroes, since 3.5 **99** of nineteen (Heimdall, Odin and Hela added), since 3.6 **111** of twenty-one (Doctor Strange and the Hulk added), since 3.7 **123** of twenty-three (Wolverine and Deadpool added), and since 3.8 **150** of twenty-eight (Captain America, Red Hulk, Iron Man, Falcon and Love added), with every Sneak variant also [a card of its own](#the-sneak-variants-cards-of-their-own-35): **221 cards** in all. Since 3.6 there are also six [stone rows](#the-infinity-stones-36), one for each Infinity Stone. This page lists them all, hero by hero.

Hebrew: [עברית](Hero-Abilities-HE)

![All the heroes in one roster](images/000-enhanced-heroes-roster.png)

## How to read the tables

* **Ability (id)**: its name in the game, and the id a hero file uses (`thor.lightning`; the Scarlet Witch's are `wanda.*`, Spider-Man's `spider.*`, Professor X's `xavier.*`).
* **Hero's key**: the key it has for the hero himself, and how it acts there: *press* (once per press, then a cooldown), *hold* (while the key is down), *on/off* (a switch), or *always on* (a hero's body: no key). On your hero you may put it on **any** key, or on a Sneak box; **Auto** puts it on this key when it is free.
* **Sneak variant**: what **Sneak +** its key does. When you put the ability on a key, its variant fills that key's Sneak box by itself (switch the variant off on the Powers tab if you want the box for something else). The variant shares the key's cooldown.
* **Points**: what one point of strength costs. An ability with **no slider** (a switch with nothing to make stronger) always costs as at strength 5: the number shown.
* **Strength** (1 to 10, 5 is the hero himself) makes an ability hit harder and recharge faster, or a body give more; see [Strength](Hero-Creator#3-strength).
* A **world power** may be switched off by a server (`custom_hero.worldPowers`).
* Two abilities of **different heroes** that drive the same thing (one grip, one shield, one beam...) cannot be on the same hero; see [Powers that cannot go together](Hero-Creator#5-powers-that-cannot-go-together).

The flying keys of the fliers (Magneto's, the Scarlet Witch's, Vision's, Billy's, Thor's, the Phoenix's, Ultron's, Apocalypse's and Agatha's) are not abilities here: your hero flies with the **Flight** power, and flies **like** one of them with the [flight style](Hero-Creator#7-animations-how-your-hero-moves). Billy's Build Magic is here as his (his rules); since 3.5 the Scarlet Witch's, with her rules, is [a card of its own](#the-sneak-variants-cards-of-their-own-35). Doctor Strange's Cloak of Levitation is not an ability either: a custom hero flies with **Flight**, in the flight style *Doctor Strange (the Cloak)*. The same for the fliers of 3.8: Iron Man (*Iron Man (the thrusters)*), Falcon (*Falcon (the wings)*) and Love fly with **Flight**.

## The abilities

### Magneto (4)

See [Magneto](Magneto) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Metal Telekinesis** `magneto.metal` | **R** (press) | Magnetic Pull (press) | 2 a point | Magneto's grip: lift metal things and creatures in metal armour, press again to throw. Sneak with nothing held: pull the metal around you to your hand. |
| **Iron Shrapnel Storm** `magneto.shrapnel` | **G** (hold) | — | 2 a point | Hold: fire shards of the iron in your pockets. |
| **Armor & Weapon Strip** `magneto.disarm` | **V** (press) | — | 2 a point | Tear the metal armour and weapons off whoever you look at. |
| **Magnetic Shield** `magneto.shield` | **B** (on/off) | — | 10 (no slider) | A switch: arrows and metal shots stop in the air before they reach you. |

### Scarlet Witch (4)

See [Scarlet Witch](Scarlet-Witch) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Chaos Beam** `wanda.chaos_beam` | **R** (hold) | — | 2 a point | Hold: a red beam of chaos magic that burns and dizzies what it hits. |
| **Chaos Telekinesis** `wanda.telekinesis` | **V** (press) | — | 2 a point | Grab a creature or a loose block in red magic and throw it; Sneak tears a block out of the ground or sets it down. |
| **Chaos Shield** `wanda.chaos_shield` | **B** (hold) | — | 2 a point | Hold: a red dome round you that stops arrows, blows and beams until it wears out. |
| **The Hex** `wanda.hex` | **N** (press) | Dissolve the Hex (press) | 3 a point | Raise a Hex over the land that only those you let in may cross; press again for Hex Control, Z orders its Vision. A world power. |

### Quicksilver (5)

See [Quicksilver](Quicksilver) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Quicksilver Time** `quicksilver.time` | **R** (on/off) | Walkman (on/off) | 15 (no slider) | A switch: the world around you slows right down. Sneak: the Walkman plays Quicksilver's music. |
| **Super Speed** `quicksilver.speed` | **G** (on/off) | Dash Strike (press) | 2 a point | A switch: sprint at Quicksilver's speed and do everything faster. Sneak: dash through enemies. |
| **Speed Blitz** `quicksilver.blitz` | **V** (press) | Pickpocket (press) | 2 a point | Dash to a creature and land six quick hits. Sneak: snatch what it holds. |
| **Wall & Water Run** `quicksilver.wall_run` | **B** (on/off) | — | 5 (no slider) | A switch: at full speed, run up walls, along ceilings and over water. Needs Quicksilver's Super Speed or Quicksilver Time. |
| **Pebble Barrage** `quicksilver.barrage` | **N** (press) | — | 2 a point | Fling a handful of pebbles at super speed. |

### Vision (5)

See [Vision](Vision) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Mind Stone Beam** `vision.mind_beam` | **R** (hold) | — | 2 a point | Hold: a golden beam from your forehead. |
| **Phase** `vision.phase` | **V** (on/off) | — | 15 (no slider) | A switch: float through walls, floors and attacks like a ghost (you float even without Flight). |
| **Density** `vision.density` | **B** (on/off) | — | 5 (no slider) | A switch: heavy as a building: hard to hurt, you hit hard, and you cannot fly. |
| **Human Disguise** `vision.disguise` | **N** (on/off) | — | 5 (no slider) | A switch: look like an ordinary person. |
| **Synthetic Body** `vision.body` | no key (always on) | — | 1 a point | Always: more health and armour, never hungry, never out of breath, made of metal (Magneto can lift you). New in 3.4. |

### Billy (5)

See [Billy](Billy-and-the-Witches-Road) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Hex Beam** `billy.hex_beam` | **R** (hold) | — | 2 a point | Hold: crackling blue lightning from your hand. |
| **Hex Telekinesis** `billy.telekinesis` | **V** (press) | — | 2 a point | Grab a creature or a loose block in blue magic and throw it; Sneak tears a block out of the ground. |
| **Freeze** `billy.freeze` | **B** (press) | Hex Barrier (hold) | 3 a point | Freeze everyone in front of you for a while. Sneak (hold): the Hex Barrier, a blue plate that stops everything from the front. |
| **The Noise** `billy.noise` | **N** (on/off) | — | 5 (no slider) | A switch: hear the thoughts of everything around you; they glow through walls in the colour of their mood. |
| **Build Magic** `billy.build_magic` | **Y** (press) | — | 10 (no slider) | The Y key: build, copy and change the world with magic, by Billy's rules (the meter and his rank). Needs none of the five keys. |

### Tommy (5)

See [Tommy](Tommy) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Slow Time** `tommy.time` | **R** (on/off) | Wall & Water Run on/off (on/off) | 15 (no slider) | A switch: the world around you slows down, and you run up walls and over water. Sneak switches the wall and water run off and on. |
| **Super Speed (Tommy)** `tommy.speed` | **G** (on/off) | Dash Strike (press) | 2 a point | A switch, on from the start: sprint at Tommy's speed. Sneak: dash through enemies. |
| **Whirlwind** `tommy.whirlwind` | **V** (press) | Sonic Boom (press) | 2 a point | Run in circles until everything near spins in the air. Sneak: stop dead with a shockwave. |
| **Vibrate Through Walls** `tommy.vibrate` | **B** (press) | Disarm Blitz (press) | 2 a point | Pass through the wall in front of you. Sneak: take the weapons of everyone around. |
| **Molecular Acceleration** `tommy.accelerate` | **N** (press) | — | 2 a point | Shake a block until it breaks, or blast a creature back. |

### Loki (5)

See [Loki](Loki) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Daggers** `loki.daggers` | **R** (press) | Dagger Fan (press) | 2 a point | Throw a conjured dagger. Sneak: a fan of three. |
| **Duplicates** `loki.duplicates` | **G** (press) | — | 2 a point | Turn invisible while three copies of you lure enemies away. |
| **Shapeshift** `loki.shapeshift` | **V** (press) | — | 5 (no slider) | Take the shape of the creature you look at. |
| **Vanish** `loki.vanish` | **B** (press) | — | 2 a point | Teleport to where you look, or behind a creature. |
| **Frost Giant** `loki.frost_giant` | **N** (on/off) | — | 5 (no slider) | A switch: the Jotun form: armour, freezing blows and ice under your feet (it paints your own skin blue). |

### Thor (5)

See [Thor](Thor) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Mjolnir Throw** `thor.hammer` | **R** (press) | Shatter Mjolnir (press) | 2 a point | Throw Mjolnir or Stormbreaker and call it back. Needs the weapon in your hand. Makes you worthy of Mjolnir. Sneak: shatter Mjolnir. |
| **Lightning Beam** `thor.lightning` | **V** (hold) | — | 3 a point | Hold: pour lightning from your hand. Works bare-handed; with Mjolnir or Stormbreaker in hand it reaches further and hits harder. |
| **God of Thunder** `thor.thunder` | **B** (on/off) | — | 2 a point | A switch: hit harder, take less, and your blows arc lightning. |
| **Bifrost** `thor.bifrost` | **N** (press) | Bifrost to Where You Look (press) | 2 a point | Open the rainbow bridge home or to a saved place; Sneak: to where you look. No weapon needed. A world power. |
| **Asgardian Body** `thor.body` | no key (always on) | — | 1 a point | Always: more health and armour, hard to knock back. |

### The Phoenix (5)

See [The Phoenix](Phoenix) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Mind Reading** `phoenix.mind` | **R** (on/off) | Mind Sense (press) | 1 a point | A switch: read the mind (and the items) of whoever you look at. Sneak: sense every mind through the walls. |
| **Phoenix Telekinesis** `phoenix.telekinesis` | **V** (press) | — | 2 a point | Grab and throw creatures and blocks. |
| **Mind Control** `phoenix.control` | **B** (press) | Psionic Field (press) | 3 a point | Take over the creature you look at. Sneak: the Psionic Field freezes everyone around you. |
| **Phoenix Burst** `phoenix.burst` | **N** (hold) | — | 3 a point | Hold to charge, let go to send a wave of cosmic fire (you hover while charging only if you have Flight). |
| **Body of Fire** `phoenix.body` | no key (always on) | — | 5 (no slider) | Always: fire and lava cannot hurt you. |

### Spider-Man (6)

See [Spider-Man](Spider-Man) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Web Shot** `spider.web` | **R** (press) | Web Burst (press) | 2 a point | Wrap the creature you look at in webbing. Sneak: web everyone around you. |
| **Web Zip** `spider.zip` | **G** (press) | Web Swing (hold) | 2 a point | A web pulls you to the block you look at. Sneak (hold): swing on a web. J switches Web Assist. |
| **Wall Crawl** `spider.crawl` | **V** (on/off) | Spider-Sense (on/off) | 5 (no slider) | A switch: stick to walls and ceilings. Sneak: switch the Spider-Sense that warns of danger. Both start on. |
| **Iron Spider Legs** `spider.legs` | **B** (on/off) | Instant Kill (press) | 2 a point | A switch: four metal legs come out of your back and strike at threats, in any look. Sneak, twice: Instant Kill. |
| **Black Eyes** `spider.black_eyes` | **N** (on/off) | — | 2 a point | A surge of the spider in you: faster, stronger, sharper senses, then a crash. |
| **Spider Body** `spider.body` | no key (always on) | — | 1 a point | Always: quicker, jump higher, hit harder and land softly. |

### Thanos (6)

See [Thanos](Thanos) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Power Stone** `thanos.power` | **R** (press) | Titan Slam (press) | 3 a point | A violet blast that throws what it hits. Sneak: the Titan Slam. |
| **Space Stone** `thanos.space` | **G** (press) | Space Pull (press) | 2 a point | Step through a portal to where you look. Sneak: pull the creature you look at to you. |
| **Reality Stone** `thanos.reality` | **V** (press) | Reality Veil (on/off) | 2 a point | Turn a creature to stone for a while. Sneak: the Reality Veil, a switch that turns arrows and shots into harmless bubbles. |
| **Soul Stone** `thanos.soul` | **B** (hold) | Soul Sight (press) | 3 a point | Hold: drain a creature's life into yours. Sneak: Soul Sight, creatures near you glow through walls and Loki's tricks come apart. |
| **Time Stone** `thanos.time` | **N** (press) | Time Lock (press) | 3 a point | Rewind yourself a few seconds back. Sneak: the Time Lock, a dome where everything but you is slow. A world power. |
| **Titan Body** `thanos.body` | no key (always on) | — | 1 a point | Always: taller and tougher, with long arms and a healing factor. |

### Ultron (5)

See [Ultron](Ultron) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Repulsor Fire** `ultron.repulsor` | **R** (hold) | Overcharge (press) | 2 a point | Hold: fire plasma bolts from a pool that refills. Sneak: charge one heavy blast. |
| **Gravity Pull** `ultron.gravity` | **V** (press) | Gravity Slam (press) | 2 a point | Yank what you look at to you. Sneak: slam everything near you down. |
| **Upload** `ultron.upload` | **B** (press) | Hack (press) | 3 a point | Jump into one of your drones, healed; a drone near you saves you from death. Sneak: hack an enemy Sentry. A world power. |
| **Sentry Army** `ultron.army` | **N** (press) | Command (press) | 3 a point | Sentries of your own assemble round you and fight for you. Sneak: send them at what you look at. A world power. |
| **Vibranium Body** `ultron.body` | no key (always on) | — | 1 a point | Always: more health and armour, never hungry or out of breath, no poison, no fall damage; lightning hurts more. |

### Apocalypse (5)

See [Apocalypse](Apocalypse) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Sand Wave** `apocalypse.sand` | **R** (press) | Entomb (press) | 2 a point | A wave of sand rolls along the ground the way you face. Sneak: swallow a creature in a cocoon of sand. |
| **Portal** `apocalypse.portal` | **V** (press) | — | 2 a point | Step through a violet portal to where you look, with your pets and friends. |
| **Golden Shield** `apocalypse.shield` | **B** (hold) | — | 2 a point | Hold: Apocalypse's shield, which stops everything but the power of the mind. |
| **Power Bestowal** `apocalypse.bestow` | **N** (press) | Sandstorm (press) | 2 a point | Make the ally you look at stronger for a while. Sneak: a storm of sand rises round you. |
| **First Mutant's Body** `apocalypse.body` | no key (always on) | — | 1 a point | Always: much more health and armour, hard to knock back, heals when left alone. |

### Agatha (5)

See [Agatha](Agatha) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Purple Beam** `agatha.purple_beam` | **R** (hold) | Hex Mark (press) | 2 a point | Hold: the Darkhold's purple beam of dark magic. Sneak: mark the creature you look at, or glimpse a mind. |
| **Power Drain** `agatha.drain` | **V** (press) | — | 3 a point | Draw the power out of a hero for a while (or the life out of a creature) and heal. |
| **Witch's Shield** `agatha.shield` | **B** (hold) | — | 2 a point | Hold: a violet sphere round you that stops blows and turns projectiles to sparks until it shatters. |
| **Rune Trap** `agatha.runes` | **N** (press) | — | 2 a point | A circle of glowing runes on the ground that binds and burns what stands in it. |
| **Agatha's Dark Magic** `agatha.body` | no key (always on) | — | 1 a point | Always: more health, and magic used on you poisons whoever used it. |

### Cyclops (6)

See [Cyclops](Cyclops) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Optic Blast** `cyclops.optic_blast` | **R** (hold) | Ricochet Shot (press) | 2 a point | Hold: a red beam of force from your eyes. Sneak: a bolt that bounces off blocks. No glasses needed. |
| **Concussion Blast** `cyclops.concussion` | **G** (press) | — | 2 a point | A short, wide blast from your eyes that throws everything in front of you back. |
| **Optic Leap** `cyclops.leap` | **V** (press) | — | 1 a point | Blast the ground to throw yourself up and forward, with no fall damage. |
| **Rally** `cyclops.rally` | **B** (press) | — | 1 a point | "To me!" You and your allies around you get speed, strength and resistance. |
| **Shut Eyes** `cyclops.glasses` | **N** (press) | Open Eyes (press) | 3 a point | Sneak (Open Eyes): a giant beam pours out of your eyes until you press the key without Sneak (Shut Eyes). No glasses needed. |
| **Soldier's Body** `cyclops.body` | no key (always on) | — | 1 a point | Always: more health and armour. |

### Professor X (5)

See [Professor X](Professor-X) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Mental Blast** `xavier.mental_blast` | **R** (press) | — | 2 a point | A blow of the mind at whoever you look at: damage that ignores armour, and dizziness. |
| **Hear Thoughts** `xavier.thoughts` | **G** (on/off) | — | 5 (no slider) | A switch: hear the thoughts of every mind around you, and see them over their heads. |
| **Take Over a Body** `xavier.take_over` | **V** (press) | — | 3 a point | Your mind goes into the creature you look at and steers it; press again to come back. A world power. |
| **Freeze Minds** `xavier.freeze_minds` | **B** (press) | — | 3 a point | Everyone with an open mind around you stands still for a few seconds. |
| **Cerebro** `xavier.cerebro` | **N** (press) | — | 2 a point | Put on Cerebro's helmet and find the minds of the players far around you. |

### Heimdall (6)

New in 3.5. See [Heimdall](Heimdall) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Hofund** `heimdall.hofund` | **R** (press) | Lunge (press) | 2 a point | A three-blow golden sword combo (any sword for a custom hero; Hofund hits 30% harder). Sneak: a dash and a thrust. |
| **All-Seeing Sight** `heimdall.sight` | **G** (on/off) | Far Sight (hold) | 2 a point | Creatures glow through walls and far players are marked. Sneak, held: zoom far. |
| **Bifrost** `heimdall.bifrost` | **V** (press) | Bifrost where you look (press) | 2 a point | Opens the Bifrost with no item: home, saved places, Asgard. A world power. |
| **Guardian's Ward** `heimdall.ward` | **B** (hold) | Gatekeeper's Call (press) | 2 a point | Hold: a golden dome that stops projectiles and keeps monsters out. Sneak: Einherjar come to your side. |
| **Bifrost Strike** `heimdall.strike` | **N** (press) | Bifrost for a friend (press) | 3 a point | A column of rainbow light strikes where you look. Sneak: send the player you look at. A world power. |
| **Heimdall's body** `heimdall.body` | no key (always on) | — | 1 a point | More health and armour, steady footing, no damage from short falls. |

### Odin (6)

New in 3.5. See [Odin](Odin) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Gungnir** `odin.gungnir` | **R** (press) | Throw (press) | 3 a point | A golden bolt from the spear (or the hand). Sneak: throw Gungnir (in your hand), which returns. |
| **Odinforce** `odin.odinforce` | **G** (hold) | Heal (press) | 3 a point | Hold: a golden dome shield (the one shield). Sneak: heal yourself and your allies. |
| **Banish** `odin.banish` | **V** (press) | Odinforce Wave (press) | 3 a point | Banishes the creature you look at. Sneak: a golden shock wave. A world power. |
| **Huginn and Muninn** `odin.ravens` | **B** (press) | Ravens' Report (press) | 2 a point | Two ravens circle where you look and show every creature near them. |
| **Allfather's Command** `odin.command` | **N** (press) | Odinsleep (on/off) | 3 a point | Warriors fight your target. Sneak: the Odinsleep heals you while you kneel. A world power. |
| **Odin's body** `odin.body` | no key (always on) | — | 1 a point | More health and armour, steady footing, regeneration when hurt. |

### Hela (6)

New in 3.5. See [Hela](Hela#becoming-hela-the-hero) for the full story of each ability. Her abilities are dark magic.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Necroswords** `hela.necroswords` | **R** (press) | Blade Rain (press) | 2 a point | Throws a fan of black blades. Sneak: blades rain on a circle. |
| **Blade Wall** `hela.blade_wall` | **G** (press) | Blade Burst (press) | 2 a point | A line of spikes erupts from the ground. Sneak: a ring of spikes round you. |
| **Goddess of Death** `hela.goddess` | **V** (on/off) | Death Dash (press) | 2 a point | The headdress grows: strength and stronger blades. Sneak: a smoke dash. |
| **Raise the Dead** `hela.army` | **B** (press) | Fenris (press) | 3 a point | Berserkers rise to fight for you. Sneak: Fenris the wolf. A world power. |
| **Death's Grasp** `hela.grasp` | **N** (hold) | — | 2 a point | Hold: projectiles crumble before your hand; a thrown Mjolnir is caught. |
| **Hela's body** `hela.body` | no key (always on) | — | 1 a point | More health and armour; in Asgard, regeneration and stronger blows. |

### Doctor Strange (6)

New in 3.6. See [Doctor Strange](Doctor-Strange) for the full story of each ability. They make a hero a **sorcerer**.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Eldritch Whip** `strange.whip` | **R** (press) | Whip Grapple (press) | 2 a point | A whip of orange sparks lashes the creature in front and pulls it near: three lashes, the third lifts. Sneak: it pulls you to a block. |
| **Shield of the Seraphim** `strange.shields` | **V** (hold) | Seraphim Dome (press) | 2 a point | Hold: a round mandala shield in front of you stops shots and beams. Sneak: a dome of mandalas round you and your friends. Shares the one shield. |
| **Crimson Bands of Cyttorak** `strange.bands` | **B** (press) | Astral Projection (on/off) | 3 a point | Red bands bind the creature you look at. Sneak: your spirit steps out of your body and passes through walls. |
| **Sling-Ring Portal** `strange.portal` | **N** (press) | Mirror Dimension (on/off) | 3 a point | A ring of sparks opens a portal here and its twin where you look. Sneak: the Mirror Dimension, where nothing breaks. A world power. |
| **The Eye: Slow Time** `strange.eye` | **Y** (on/off) | The Eye: Rewind (press) | 3 a point | On **Y**, with the Eye of Agamotto in your inventory: time slows round you. Sneak: turn your own time back. Shares the slow time. |
| **Strange's body** `strange.body` | no key (always on) | — | 1 a point | More health and armour; the Cloak catches your falls; spells cost less hunger. |

### The Hulk (6)

New in 3.6. See [Hulk](Hulk) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Hulk Smash** `hulk.smash` | **R** (press) | Grab Smash (press) | 2 a point | Both fists into the ground: a ring hurts everyone on the ground near you. Sneak: grab one and slam it left, right, left. |
| **Thunder Clap** `hulk.clap` | **G** (press) | Roar (press) | 2 a point | A clap sends a shockwave that throws creatures far and puts out fire. Sneak: a roar that frightens them. |
| **Super Leap** `hulk.leap` | **V** (press) | Bound (press) | 2 a point | A giant leap to where you look, landing with a blow. Sneak: a long low bound. |
| **Boulder Throw** `hulk.boulder` | **B** (press) | Gamma Charge (press) | 2 a point | Tear up a boulder and hurl it. Sneak: charge shoulder first through everything in your way. |
| **Human Form** `hulk.transform` | **N** (press) | Switch Hulk (press; the Hulk's own) | 5 (no slider) | For a custom hero: **N** turns every power off and shows your own skin, until **N** again. (The Hulk himself changes between Bruce Banner and the Hulk, and Sneak switches his two Hulks.) Shares the one shape. |
| **The Hulk's body** `hulk.body` | no key (always on) | — | 2 a point | Huge (it changes your size), strong and hard to hurt; Rage makes the blows stronger. Shares the size. |

### Wolverine (6)

New in 3.7. See [Wolverine](Wolverine) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Claw Slash** `wolverine.slash` | **R** (press) | Spin Slash (press) | 2 a point | Three quick slashes of adamantium claws, the third a cross-slash. Sneak: a full turn that slashes everyone round you. |
| **Claws** `wolverine.claws` | **G** (on/off) | Claw Climb (on/off) | 1 a point | Snikt! The claws come out (your empty hand hits harder) or go back in. Sneak: climb walls by your claws. |
| **Pounce** `wolverine.pounce` | **V** (press) | Feral Leap (press) | 2 a point | Leap onto the creature you look at and stab it, pinning it for a moment. Sneak: a long low running leap. |
| **Berserker Rage** `wolverine.berserker` | **B** (press) | Healing Burst (press) | 3 a point | Ten seconds of rage: stronger, faster, healing twice as fast, but too angry to use items. Sneak: heal half your health at once. |
| **Sniff** `wolverine.senses` | **N** (press) | Track (on/off) | 2 a point | Smell every creature near you through walls, and their scent trails. Sneak: follow one scent for a minute. |
| **Wolverine's body** `wolverine.body` | no key (always on) | — | 3 a point | Adamantium bones: more health and armour, faster and stronger; you sink in water and Magneto can lift you. The [healing factor](Healing-Factor): only the Infinity Stones, chaos magic, the Allfather's power, the Phoenix or poison defeat you. |

### Deadpool (6)

New in 3.7. See [Deadpool](Deadpool) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Katanas** `deadpool.katanas` | **R** (press) | Parry (hold) | 2 a point | Three quick cuts of two swords. Sneak, held: the swords spin in front of you and bounce arrows back. The Parry shares the one shield. |
| **Pistols** `deadpool.pistols` | **G** (press) | Trick Shot (press) | 2 a point | Two cartoon pistols fire bright shots, then reload. Sneak: a shot that bounces off a wall at a foe. |
| **Teleport Belt** `deadpool.belt` | **V** (press) | Drop-In (press) | 2 a point | Blink to where you look (the belt sometimes lands you a little off). Sneak: drop onto a foe from above. |
| **Taunt** `deadpool.taunt` | **B** (press) | Chimichanga Break (press) | 1 a point | A joke and a pose: monsters near turn on you instead of your friends. Sneak: a healing break, better with a chimichanga. |
| **Dogpool** `deadpool.dogpool` | **N** (press) | Smoke Vanish (press) | 3 a point | Whistle for Dogpool, who fights beside you for two minutes. Sneak: vanish in a puff of smoke. |
| **Deadpool's body** `deadpool.body` | no key (always on) | — | 3 a point | More health, faster and stronger. The [healing factor](Healing-Factor). |

### Captain America (6)

New in 3.8. See [Captain America](Captain-America) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Shield Throw** `captain.shield_throw` | **R** (press) | Ricochet Throw (press) | 2 a point | Throw your shield: it hits and flies back to your hand. Needs a shield in hand: any shield flies and comes back, only the Vibranium Shield ricochets. Sneak: it bounces from foe to foe. |
| **Shield Slam** `captain.shield_slam` | **G** (press) | Super-Soldier Leap (press) | 2 a point | Leap and drive the shield down: a shockwave round you. Sneak: a long running leap. |
| **Shield Charge** `captain.shield_charge` | **V** (press) | Shield Bash (press) | 2 a point | Charge behind your raised shield and knock foes away. Sneak: a bash that stuns. |
| **Assemble** `captain.assemble` | **B** (press) | Inspire (press) | 2 a point | Call your allies: they get stronger and faster. Sneak: heal your allies. |
| **Combat Combo** `captain.combo` | **N** (press) | Flip Kick (press) | 1 a point | Two punches and a spinning kick. Sneak: a backflip kick. |
| **Super-Soldier** `captain.body` | no key (always on) | — | 2 a point | Speed II, Strength II and Resistance I, more health; **worthy of Mjolnir**. |

### Red Hulk (6)

New in 3.8. See [Red Hulk](Red-Hulk) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Ground Smash** `red_hulk.smash` | **R** (press) | Grab & Slam (press) | 2 a point | Smash the ground and throw everyone round you up. Sneak: grab a creature and slam it. |
| **Heat Wave** `red_hulk.heat_wave` | **G** (press) | Roar (press) | 2 a point | A cone of heat that sets foes alight. Sneak: a roar that frightens monsters. |
| **Super Jump** `red_hulk.super_jump` | **V** (hold, let go) | Bound (press) | 2 a point | Hold to charge a huge jump straight up (no fall damage). Sneak: a long low leap. |
| **Anger Burst** `red_hulk.burst` | **B** (press) | Energy Absorb (hold) | 3 a point | Let your anger out in a ring of fire. Sneak, held: soak up fire, lightning and beams. The Absorb shares the one shield. |
| **Transform** `red_hulk.transform` | **N** (press) | — | 5 (no slider) | For a custom hero, a human form: **N** turns every power off and shows your own skin until **N** again. (Red Hulk himself turns into Ross.) Shares the one shape. |
| **Red Hulk's Body** `red_hulk.body` | no key (always on) | — | 3 a point | Huge, strong and fire-proof, a little slow; anger builds up as you fight and bursts at 100. Shares the size. |

### Iron Man (5)

New in 3.8. A custom hero wears no suit: these cards are the **Mark 85**'s moves, and the body card is his suit. See [Iron Man](Iron-Man) for the full story.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Repulsor** `iron_man.repulsor` | **R** (press) | Energy Blades (on/off) | 2 a point | Blasts of energy from your palms. Sneak: energy blades come out of your hands. |
| **Unibeam** `iron_man.unibeam` | **V** (hold) | — | 2 a point | Hold: a big laser beam from your chest (the Mark 85's giant chest laser). |
| **Energy Shield** `iron_man.shield` | **B** (hold) | Lightning Refocuser (press) | 2 a point | Hold up a giant energy shield. Sneak: chain lightning that jumps between foes. Shares the one shield. |
| **Homing Missiles** `iron_man.missiles` | **N** (press) | Nanotech Drones (press) | 2 a point | Tiny missiles that chase foes and pop. Sneak: glowing wing blades that fire lasers for you. |
| **Suit** `iron_man.suit` | no key (always on) | — | 3 a point | Armour all over (+8 armour, +4 toughness) and a **suit energy** meter that powers the moves. |

### Falcon (5)

New in 3.8. A custom hero with his cards needs no wings item: Falcon's wings are drawn on him. See [Falcon](Falcon) for the full story.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Wrist Blasters** `falcon.blasters` | **R** (press) | Wrist Rocket (press) | 2 a point | Bright energy darts from both wrists. Sneak: a small rocket. |
| **Dive** `falcon.dive` | **V** (press) | Boost Climb (press) | 2 a point | Fold your wings and dive onto a foe, from the air. Sneak: shoot straight up. |
| **Wing Shield** `falcon.wing_shield` | **B** (hold) | Wing Slash (press) | 2 a point | Hold to wrap your wings in front of you. Sneak: spin with your wings out. Shares the one shield. |
| **Redwing** `falcon.redwing` | **N** (press) | Redwing Strike (press) | 2 a point | Your drone marks monsters and zaps them. Sneak: it rams a foe. |
| **Falcon's Body** `falcon.body` | no key (always on) | — | 2 a point | A little more health and speed, and Falcon's wings (with the Exo-Wings on your chest, you glide). |

### Love (5)

New in 3.8. See [Love](Love) for the full story of each ability.

| Ability (id) | Hero's key | Sneak variant | Points | What it does |
|---|---|---|---|---|
| **Axe Throw** `love.axe` | **R** (press) | Thunder Strike (press) | 2 a point | Throw Stormbreaker (or Mjolnir) and it flies back to you. Needs the weapon in your hand. Sneak: lightning strikes where you look. |
| **Lightning** `love.lightning` | **V** (hold) | — | 3 a point | Hold: a beam of lightning from your hand. |
| **Eye Laser** `love.eye_laser` | **B** (hold) | — | 2 a point | Hold: a purple beam from your eyes that burns. |
| **Bifrost** `love.bifrost` | **N** (press) | Bifrost to where you look (press) | 2 a point | Open the Bifrost with your axe in hand. Sneak: straight to where you look. A world power. |
| **Daughter of Thor** `love.body` | no key (always on) | — | 2 a point | 16 hearts; **worthy of Mjolnir**. |

## The Infinity Stones (3.6)

![The Infinity Stones](images/114-infinity-stones.png)

Each Infinity Stone is a **stone row**: put it on any key, and Sneak + that key is its second power. A stone row works **only while your hero carries that stone and may use it**: the stone itself (only a god's ability lets a hero hold the Reality, Power and Soul Stones: see [Infinity Stones](Infinity-Stones#who-may-hold-a-stone)), a Gauntlet with it that he may wield, or its vessel (Loki's Scepter, the Tesseract, the Eye of Agamotto). Without it the key says "You do not carry the Mind Stone." The Time Stone answers only a sorcerer. The card shows **Needs the stone**. The numbers follow the stone's own (`[mind_stone]`...), its strength and `custom_hero.cooldownScale`. A server can switch them off (`custom_hero.stoneRows`).

| Stone row (id) | Press | Sneak | Points | What it does |
|---|---|---|---|---|
| **Reality Stone** `stone.reality` | on/off | Transmute (press) | 2 a point | A switch: shots at you turn into bubbles. Sneak: the creature you look at turns to stone. |
| **Mind Stone** `stone.mind` | hold | Mind Command (press) | 2 a point | Hold: the Mind Beam. Sneak: the creature you look at fights for you. Shares the continuous beam. |
| **Time Stone** `stone.time` | on/off | Rewind (press) | 2 a point | Sorcerers only: time slows round you. Sneak: back 5 seconds. Shares the slow time. |
| **Power Stone** `stone.power` | hold | Power Bolt (press) | 3 a point | Hold: the Purple Beam, with explosions. Sneak: a bolt that explodes. Shares the continuous beam. |
| **Space Stone** `stone.space` | press | Realm Picker (press) | 2 a point | Step through a portal to where you look. Sneak: choose a world. A world power. |
| **Soul Stone** `stone.soul` | hold | Soul Sight (press) | 2 a point | Hold: drain a soul. Sneak: see every soul near you. Shares the soul chain with Thanos's Soul Stone. |

## The Sneak variants: cards of their own (3.5)

Since 3.5 every Sneak variant of the heroes is **a card of its own** too, so you can pick it without its ability ("all the abilities, like Billy's shield", as Eilon asked). Picked alone, it works on **any** key, plain or Sneak (it presses its ability's Sneak half for you). Picked with its ability, that ability keeps only its main action. It costs what its ability costs (and the two together count as one power), shares its colour, and is a world power when its ability is one. **Needs**: the ability it can't work without.

| Card (id) | The ability it belongs to | Needs | Points | What it does |
|---|---|---|---|---|
| **Magnetic Pull (Magneto)** `magneto.magnetic_pull` | `magneto.metal` | — | 2 a point | Pull every loose bit of metal around you to your hand, all at once. |
| **Dissolve the Hex (Scarlet Witch)** `wanda.hex_dissolve` | `wanda.hex` | `wanda.hex` | 3 a point | Bring your Hex down: the red dome melts away. A world power. |
| **Walkman (Quicksilver)** `quicksilver.walkman` | `quicksilver.time` | `quicksilver.time` | 15 (no slider) | A switch: Quicksilver's music plays while his slowed time is on. |
| **Dash Strike (Quicksilver)** `quicksilver.dash` | `quicksilver.speed` | — | 2 a point | Dash straight ahead in a silver blur, hitting every enemy in the way. |
| **Pickpocket (Quicksilver)** `quicksilver.pickpocket` | `quicksilver.blitz` | — | 2 a point | Zip to the creature you look at and snatch what it holds. |
| **Hex Barrier (Billy)** `billy.hex_barrier` | `billy.freeze` | — | 3 a point | Hold: a blue plate of magic in front of you stops everything that comes from the front. Shares the one shield. |
| **Wall & Water Run (Tommy)** `tommy.wall_run` | `tommy.time` | one of the speedsters' speeds or slow times | 15 (no slider) | A switch: run up walls, along ceilings and over water while you run at super speed. |
| **Dash Strike (Tommy)** `tommy.dash` | `tommy.speed` | — | 2 a point | Dash straight ahead in a white-green blur, hitting every enemy in the way. |
| **Sonic Boom (Tommy)** `tommy.sonic_boom` | `tommy.whirlwind` | — | 2 a point | Stop dead from top speed: a shockwave throws everything near you back. |
| **Disarm Blitz (Tommy)** `tommy.disarm_blitz` | `tommy.vibrate` | — | 2 a point | In a blink, take the weapons out of the hands of everyone around you. |
| **Dagger Fan (Loki)** `loki.dagger_fan` | `loki.daggers` | — | 2 a point | Throw a fan of conjured daggers at once. |
| **Shatter Mjolnir (Thor)** `thor.shatter` | `thor.hammer` | — | 2 a point | With Mjolnir in hand: it bursts into flying shards that strike around you, then comes together again. |
| **Bifrost to Where You Look (Thor)** `thor.bifrost_look` | `thor.bifrost` | — | 2 a point | The rainbow bridge carries you to the place you look at. A world power. |
| **Mind Sense (Jean Grey)** `phoenix.mind_sense` | `phoenix.mind` | — | 1 a point | Feel every mind around you for a moment, even through walls. |
| **Psionic Field (Jean Grey)** `phoenix.psionic_field` | `phoenix.control` | — | 3 a point | A burst of mind power pushes and stuns everything around you. |
| **Web Burst (Spider-Man)** `spider.web_burst` | `spider.web` | — | 2 a point | Webs fly out all around you and wrap everyone near. |
| **Web Swing (Spider-Man)** `spider.web_swing` | `spider.zip` | — | 2 a point | Hold to swing on a web from the block above you, let go to fly on (J switches Web Assist). |
| **Spider-Sense (Spider-Man)** `spider.sense` | `spider.crawl` | — | 5 (no slider) | A switch, on from the start: a tingle warns you of danger coming. |
| **Instant Kill (Spider-Man)** `spider.instant_kill` | `spider.legs` | `spider.legs` | 2 a point | Press twice with the spider legs out: they strike with all their force. |
| **Titan Slam (Thanos)** `thanos.titan_slam` | `thanos.power` | — | 3 a point | Punch the ground: a ring of violet force throws everything near you. |
| **Space Pull (Thanos)** `thanos.space_pull` | `thanos.space` | — | 2 a point | A blue portal pulls the creature you look at to you. |
| **Reality Veil (Thanos)** `thanos.reality_veil` | `thanos.reality` | — | 2 a point | A switch: a red veil of reality hides you from those around you. |
| **Soul Sight (Thanos)** `thanos.soul_sight` | `thanos.soul` | — | 3 a point | See every living soul around you glow, even through walls. |
| **Time Lock (Thanos)** `thanos.time_lock` | `thanos.time` | — | 3 a point | Freeze the creatures in front of you in time for a moment. A world power. |
| **Overcharge (Ultron)** `ultron.overcharge` | `ultron.repulsor` | — | 2 a point | Charge one heavy plasma blast in your palm, then let it fly. |
| **Gravity Slam (Ultron)** `ultron.slam` | `ultron.gravity` | — | 2 a point | Slam everything near you down into the ground. |
| **Hack (Ultron)** `ultron.hack` | `ultron.upload` | — | 3 a point | Hack the enemy Sentry you look at: it fights for you now. A world power. |
| **Command (Ultron)** `ultron.command` | `ultron.army` | `ultron.army` | 3 a point | Send your Sentries at what you look at. A world power. |
| **Entomb (Apocalypse)** `apocalypse.entomb` | `apocalypse.sand` | — | 2 a point | Swallow the creature you look at in a cocoon of sand. |
| **Sandstorm (Apocalypse)** `apocalypse.sandstorm` | `apocalypse.bestow` | — | 2 a point | A storm of sand rises round you and blinds your enemies. |
| **Hex Mark (Agatha)** `agatha.hex_mark` | `agatha.purple_beam` | — | 2 a point | Mark the creature you look at with a purple rune, or glimpse a mind. |
| **Ricochet Shot (Cyclops)** `cyclops.ricochet` | `cyclops.optic_blast` | — | 2 a point | A red bolt from your eyes that bounces off blocks to hit around corners. |
| **Open Eyes (Cyclops)** `cyclops.open_eyes` | `cyclops.glasses` | `cyclops.glasses` | 3 a point | Open your eyes wide: a giant beam pours out until you shut them. Shares the continuous beam. |
| **Whip Grapple (Doctor Strange)** `strange.whip_grapple` | `strange.whip` | — | 2 a point | The Eldritch Whip catches the block you look at and pulls you there. (3.6) |
| **Seraphim Dome (Doctor Strange)** `strange.seraphim_dome` | `strange.shields` | — | 2 a point | A sphere of mandalas guards you and everyone near you. Shares the one shield. (3.6) |
| **Astral Projection (Doctor Strange)** `strange.astral` | `strange.bands` | — | 3 a point | Your spirit leaves the body and passes through walls; press again to return. Shares the phasing. (3.6) |
| **Mirror Dimension (Doctor Strange)** `strange.mirror` | `strange.portal` | — | 3 a point | A bubble where nothing in the real world breaks and your foes cannot leave. A world power. (3.6) |
| **The Eye: Rewind (Doctor Strange)** `strange.eye_rewind` | `strange.eye` | — | 3 a point | With the Eye of Agamotto: turn your own time back a few seconds. (3.6) |
| **Grab Smash (the Hulk)** `hulk.grab_smash` | `hulk.smash` | — | 2 a point | Grab the creature in front by the legs and slam it left, right, left. (3.6) |
| **Roar (the Hulk)** `hulk.roar` | `hulk.clap` | — | 2 a point | A roar that frightens and weakens the creatures round you. (3.6) |
| **Bound (the Hulk)** `hulk.bound` | `hulk.leap` | — | 2 a point | A long low leap forward, again and again. (3.6) |
| **Gamma Charge (the Hulk)** `hulk.gamma_charge` | `hulk.boulder` | — | 2 a point | Charge shoulder first and throw aside everything in your path. (3.6) |
| **Spin Slash (Wolverine)** `wolverine.spin_slash` | `wolverine.slash` | — | 2 a point | A full turn, both claws out: every creature round you is slashed. (3.7) |
| **Claw Climb (Wolverine)** `wolverine.claw_climb` | `wolverine.claws` | `wolverine.claws` | 1 a point | Climb walls by your claws (needs the Claws). (3.7) |
| **Feral Leap (Wolverine)** `wolverine.feral_leap` | `wolverine.pounce` | — | 2 a point | A long, low running leap. (3.7) |
| **Healing Burst (Wolverine)** `wolverine.healing_burst` | `wolverine.berserker` | — | 3 a point | Heal half your health at once and shake off bad effects (not poison). Works without the healing factor too. (3.7) |
| **Track (Wolverine)** `wolverine.track` | `wolverine.senses` | — | 2 a point | Follow the scent of the creature you look at for a minute. (3.7) |
| **Parry (Deadpool)** `deadpool.parry` | `deadpool.katanas` | — | 2 a point | Held: two spinning swords bounce arrows back and soften blows from the front. Shares the one shield. (3.7) |
| **Trick Shot (Deadpool)** `deadpool.trick_shot` | `deadpool.pistols` | — | 2 a point | A shot that bounces off a wall at the nearest foe. (3.7) |
| **Drop-In (Deadpool)** `deadpool.drop_in` | `deadpool.belt` | — | 2 a point | Teleport above a foe and drop onto it sword first. (3.7) |
| **Chimichanga Break (Deadpool)** `deadpool.chimichanga` | `deadpool.taunt` | — | 1 a point | A healing break; with a chimichanga in your bag, a bigger one. Works without the healing factor too. (3.7) |
| **Smoke Vanish (Deadpool)** `deadpool.smoke` | `deadpool.dogpool` | — | 3 a point | A smoke bomb: you turn invisible and monsters lose you. (3.7) |
| **Ricochet Throw (Captain America)** `captain.ricochet` | `captain.shield_throw` | — | 2 a point | The shield bounces from foe to foe before it flies home. (3.8) |
| **Super-Soldier Leap (Captain America)** `captain.super_leap` | `captain.shield_slam` | — | 2 a point | A long running leap, with a safe landing. (3.8) |
| **Shield Bash (Captain America)** `captain.shield_bash` | `captain.shield_charge` | — | 2 a point | A close bash with the shield that stuns. (3.8) |
| **Inspire (Captain America)** `captain.inspire` | `captain.assemble` | — | 2 a point | Heal your allies around you. (3.8) |
| **Flip Kick (Captain America)** `captain.flip_kick` | `captain.combo` | — | 1 a point | A backflip kick that throws a foe up. (3.8) |
| **Grab & Slam (Red Hulk)** `red_hulk.grab_slam` | `red_hulk.smash` | — | 2 a point | Grab a creature and slam it down. (3.8) |
| **Roar (Red Hulk)** `red_hulk.roar` | `red_hulk.heat_wave` | — | 2 a point | A roar that frightens monsters away. (3.8) |
| **Bound (Red Hulk)** `red_hulk.bound` | `red_hulk.super_jump` | — | 2 a point | A long, low leap forward. (3.8) |
| **Energy Absorb (Red Hulk)** `red_hulk.absorb` | `red_hulk.burst` | — | 3 a point | Hold to soak up fire, lightning and beams into your anger. Shares the one shield. (3.8) |
| **Energy Blades (Iron Man)** `iron_man.energy_blades` | `iron_man.repulsor` | — | 2 a point | Energy blades come out of your hands. (3.8) |
| **Lightning Refocuser (Iron Man)** `iron_man.lightning_refocuser` | `iron_man.shield` | — | 2 a point | Chain lightning that jumps between foes. (3.8) |
| **Nanotech Drones (Iron Man)** `iron_man.drones` | `iron_man.missiles` | — | 2 a point | Four glowing wing blades grow out of your arms, two on each side, and fire lasers. (3.8; the wings 3.8.3) |
| **Wrist Rocket (Falcon)** `falcon.wrist_rocket` | `falcon.blasters` | — | 2 a point | A small rocket with a small boom. (3.8) |
| **Boost Climb (Falcon)** `falcon.boost_climb` | `falcon.dive` | — | 2 a point | Shoot straight up into the sky. (3.8) |
| **Wing Slash (Falcon)** `falcon.wing_slash` | `falcon.wing_shield` | — | 2 a point | Spin with your wings out. (3.8) |
| **Redwing Strike (Falcon)** `falcon.redwing_strike` | `falcon.redwing` | — | 2 a point | Redwing rams the foe you look at. (3.8) |
| **Thunder Strike (Love)** `love.thunder_strike` | `love.axe` | — | 2 a point | Lightning strikes where you look. (3.8) |
| **Bifrost to where you look (Love)** `love.bifrost_look` | `love.bifrost` | — | 2 a point | The Bifrost takes you straight to where you look. A world power. (3.8) |

**The Scarlet Witch's Build Magic** is a card of its own too (3.5): **Build Magic (Scarlet Witch)** `wanda.build_magic`, on **Y**, 10 points (no slider): build, copy and change the world with magic by the Scarlet Witch's rules (no meter, and her **Instant** switch). A hero may have hers or Billy's (`billy.build_magic`), not both.

Heimdall's, Odin's and Hela's Sneak variants are not cards of their own: they come with their abilities (the tables above). Doctor Strange's and the Hulk's are (3.6), except the Hulk's **Switch Hulk**, which is only the real Hulk's; and so are all of Wolverine's and Deadpool's (3.7), and all of the 3.8 heroes' (Captain America's five, Red Hulk's four, Iron Man's three, Falcon's four and Love's two). The suits' other moves (the Mark 1's flamethrower, the Mark 3's flares, the Mark 5's dash punch, the Mark 50's nano cannon, the Hulkbuster's punches and Veronica...) are not cards: a custom hero wears no suit.

## Related

[Hero Creator](Hero-Creator) · [Controls](Controls) · [Server Configuration](Server-Configuration#the-hero-creator)

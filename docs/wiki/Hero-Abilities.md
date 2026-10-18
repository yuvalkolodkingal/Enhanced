# Heroes' Abilities

Since 3.4 every ability of every hero of the mod is a power a [custom hero](Hero-Creator) can pick in the Hero Creator: **81 abilities** of sixteen heroes. This page lists them all, hero by hero.

Hebrew: [עברית](Hero-Abilities-HE)

## How to read the tables

* **Ability (id)**: its name in the game, and the id a hero file uses (`thor.lightning`; the Scarlet Witch's are `wanda.*`, Spider-Man's `spider.*`, Professor X's `xavier.*`).
* **Hero's key**: the key it has for the hero himself, and how it acts there: *press* (once per press, then a cooldown), *hold* (while the key is down), *on/off* (a switch), or *always on* (a hero's body: no key). On your hero you may put it on **any** key, or on a Sneak box; **Auto** puts it on this key when it is free.
* **Sneak variant**: what **Sneak +** its key does. When you put the ability on a key, its variant fills that key's Sneak box by itself (switch the variant off on the Powers tab if you want the box for something else). The variant shares the key's cooldown.
* **Points**: what one point of strength costs. An ability with **no slider** (a switch with nothing to make stronger) always costs as at strength 5: the number shown.
* **Strength** (1 to 10, 5 is the hero himself) makes an ability hit harder and recharge faster, or a body give more; see [Strength](Hero-Creator#3-strength).
* A **world power** may be switched off by a server (`custom_hero.worldPowers`).
* Two abilities of **different heroes** that drive the same thing (one grip, one shield, one beam...) cannot be on the same hero; see [Powers that cannot go together](Hero-Creator#5-powers-that-cannot-go-together).

The flying keys of the fliers (Magneto's, the Scarlet Witch's, Vision's, Billy's, Thor's, the Phoenix's, Ultron's, Apocalypse's and Agatha's) are not abilities here: your hero flies with the **Flight** power, and flies **like** one of them with the [flight style](Hero-Creator#7-animations-how-your-hero-moves). Wanda's and Billy's Build Magic is here once, as Billy's (his rules).

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
| **Slow Time** `tommy.time` | **R** (on/off) | Wall Run on/off (on/off) | 15 (no slider) | A switch: the world around you slows down, and you run up walls. Sneak switches the wall run off and on. |
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

## Related

[Hero Creator](Hero-Creator) · [Controls](Controls) · [Server Configuration](Server-Configuration#the-hero-creator)

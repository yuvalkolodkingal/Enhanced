# Enhanced — Full Design

**Enhanced** is a Minecraft mod (NeoForge, Minecraft 1.21.1) that lets players become
one of the *Enhanced*:

* **Magneto**, the Master of Magnetism
* **Wanda Maximoff, the Scarlet Witch**
* **Quicksilver (Peter Maximoff)**, the fastest man alive
* **Vision**, the synthezoid with the Mind Stone in his forehead, and **White Vision** **(added)**
* **Tommy Maximoff (Speed)**, Wanda's speedster son **(added)**
* **Billy Maximoff (Wiccan)**, Wanda's son, the witch who made the Witches' Road real **(added)**
* **Loki**, the God of Mischief, in his Asgardian clothes, as a TVA Variant and as the God of Stories **(added)**
* **Thor**, the God of Thunder, with **Mjolnir** and **Stormbreaker**, and the **Bifrost** **(added)**

This document completes the original concepts by Eilon Kolodkin-Gal (Magneto, the
Scarlet Witch, and Quicksilver's slow-motion run). Everything the concepts said is kept.
Where they left a gap (where an item comes from, what key does what, exact numbers), the
gap is filled in here, and every addition is marked **(added)**. The player's guide to all of
it (recipes, keys, every server option), in English and Hebrew, is on the
[wiki](../../../wiki); its pages are in [docs/wiki](wiki/).

---

## 0. Rules for every power

| Rule | Detail |
|---|---|
| One power per player | A player has exactly one of: *none*, Magneto, Scarlet Witch, Quicksilver, Vision, Tommy, Billy, Loki, Thor. Powers never stack. |
| Permanent | Once unlocked, a power survives death, logging out and changing dimension. Only an operator command can remove it (`/enhanced power set <player> none`). |
| Ability keys **(added)** | Ability 1–5 are bound to **R, G, V, B, N** by default (rebind under *Options → Controls → Enhanced Powers*). Each key triggers the ability with the same number in that hero's list. |
| Flight | Flying works like Creative mode: double-tap **Space**, or press the hero's flight key. Quicksilver doesn't fly, he runs. |
| Flight and run animation **(added)** | A flying hero leans into the flight like a superhero: upright while hovering, up to about 60° forward at full speed (Thor up to about 80°, flying behind his weapon), tilting a little further in a dive and back when climbing, and banking up to about 15° into turns. Quicksilver in Quicksilver Time and Tommy at super speed lean forward about 20° and swing their arms and legs bigger and faster. It is all smoothed, so nothing snaps, and other players see it too. |
| HUD **(added)** | The top-left corner shows your hero, each ability with its key, cooldowns, toggles and resources. |
| Looks **(added)** | Vision and the newer heroes have several looks (variants), each with its own skin, cape and energy colours: White Vision, the TVA Loki, Mighty Thor... Press **H** to change look (**Sneak + H** goes back). A look changes only how the hero looks and the colour of his energy: the abilities are the same in every look. Every look of the newer heroes includes *Your own skin*: the hero's powers in your own skin. |
| MCU colours **(added)** | Every power's light and colour follows the films and shows (section 9). |
| Creative / Spectator **(added)** | Creative and Spectator players are never killed by the Mind Stone or the Hex wall, the same way vanilla treats them as invulnerable. |

---

## 1. Magneto — the Master of Magnetism

### 1.1 Awakening

* **Magneto's Helmet** is crafted from **exactly 5 Netherite Blocks** in the normal helmet shape:

  ```
  N N N
  N . N
  ```
* **Equipping** the helmet for the first time permanently unlocks all of Magneto's
  abilities (if the player has no other power).
* While the helmet is worn by Magneto, the player's whole skin turns into **Magneto's
  classic suit** (red suit, purple gloves, boots and cape).

### 1.2 Suit & protection (while the helmet is worn)

* **Armor:** the helmet alone gives the protection of a **full Netherite set**:
  20 armor, 12 armor toughness, 40% knockback resistance.
* **Infinite durability:** the helmet never takes damage and never breaks.
* **Mental immunity:** 100% immune to **Nausea** (the dizziness from Wanda's Chaos
  Beam, pufferfish, etc.). Any Nausea he already has is removed the moment the
  helmet goes on.
* **No damage immunity:** Magneto still takes the **full physical damage** from the
  Chaos Beam and all of Wanda's other attacks. Even the helmet's armor doesn't soften them.
  Only the Nausea is blocked.

### 1.3 Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R | **Metal Telekinesis** | Look at a target up to 32 blocks away and press **R** to lift it. While held, it floats where you look. Press **R** again to **launch** it. **Sneak + R** sets it down gently. |
| 2 | G (hold) | **Iron Shrapnel Storm** | Hold **G** to fire a machine-gun stream of iron shrapnel (10 shards/second), paid for with iron from your inventory. |
| 3 | V | **Armor & Weapon Strip** | Look at an enemy and press **V**. Every metal weapon and armor piece is ripped off and drops to the ground. |
| 4 | B (toggle) | **Magnetic Shield** | An invisible magnetic field that repels all metal projectiles. |
| 5 | N (toggle) | **Magnetic Flight** | Smooth, steady flight. |

#### Ability 1 — Metal Telekinesis

* **Valid targets:** any mob or player wearing at least one piece of **Iron, Chainmail
  or Gold armor**, and **Iron Golems**. **(added)** Minecarts and dropped metal items
  also count, because they are made of metal.
* **Iron Golem control:** Magneto can lift any Iron Golem and throw it at enemies.
  A thrown golem hits like a wrecking ball (up to 15 damage to whatever it slams into),
  takes no damage itself, never turns on Magneto, and starts fighting the monster it hit.
* **Launch:** launched targets fly at high speed. When they slam into a wall or the
  floor they take heavy impact damage (up to 16) and hurt any creature they crash into
  (up to 6 damage).
* One target at a time. The hold breaks automatically if the target dies, leaves the
  dimension or gets more than 48 blocks away. A held *player* is released after
  30 seconds **(added — so nobody is trapped forever)**.

#### Ability 2 — Iron Shrapnel Storm

* Requires iron in the inventory. Shards are paid for from an internal *iron charge*.
  When the charge runs out, one more iron item is melted into shards. Plain iron is
  used first, smallest first, and tools and armor only when no plain iron is left:

  | Item | Shards |
  |---|---|
  | Iron Nugget | 1 |
  | Iron Ingot, Raw Iron | 9 |
  | Block of Iron, Block of Raw Iron | 81 |
  | Iron tools, weapons and armor (and other items tagged `enhanced:shrapnel_ammo`) | 18 |

  **(added)** Enchanted items are never melted, so a Mending pickaxe is safe.
* Each shard does **4 damage**, **pierces through up to 3 targets**, and **ignores the
  normal hit cooldown**, so the full stream deals about 40 damage per second before
  armor.
* Creative players need no iron.

#### Ability 3 — Armor & Weapon Strip (Disarm)

* Works on players, zombies, skeletons, piglins and every other creature, up to 32 blocks.
* Rips off every metal item in a hand or armor slot (iron, chainmail and gold armor,
  iron and gold weapons and tools, shears, flint & steel, metal horse armor). The items
  fly out and land on the ground as normal items.
* Cooldown: 3 seconds.

#### Ability 4 — Magnetic Shield

* Toggle on and off. While it is on it works automatically.
* **Metal projectiles** (arrows, spectral arrows, crossbow bolts, tridents and
  other Magnetos' shrapnel) coming at Magneto are **caught in mid-air** about 4.5 blocks
  out, like the bullets and missiles he stops in the X-Men films. Each one hangs there,
  trembling, for almost a second, while Magneto's hand points at it. Then it either
  **drops to the floor** or **flies straight back at whoever shot it** (50/50).
* **(added)** You can see the field while it's on: faint, colourless field lines loop around
  him like iron filings round a magnet, and every caught projectile shimmers with a ring
  and a line of force to his hand.
* A metal projectile that would hit Magneto while the shield is on never deals damage.
* It doesn't stop fireballs, potions or snowballs, because they aren't metal.

#### Ability 5 — Magnetic Flight

* Survival flight, steady and fully controllable.
* Flying speed is **0.075** (Creative is 0.05), a bit slower than Wanda's 0.09.

---

## 2. Wanda Maximoff — the Scarlet Witch

### 2.1 Awakening

**Step 1: find the gene.** The **Scarlet Witch Gene** is a very rare item. On its own
it does **nothing at all**. It can be found:

| Source | Chance |
|---|---|
| Woodland Mansion chest | 5% |
| Stronghold library chest | 4% |
| Stronghold corridor / crossing chest | 2% |
| Nether Fortress chest | 3% |
| Bastion treasure chest | 5% |
| Ancient City chest | 4% |
| End City treasure chest | 5% |
| Jungle Temple chest | 3% |
| Desert Pyramid chest | 2% |
| Witch, killed by a player | 1% (+0.5% per Looting level) |
| Evoker, killed by a player | 2% (+1% per Looting level) |

**Step 2: touch the Mind Stone.** The Mind Stone sits inside **Loki's Scepter** (the
blue gem in its golden blade). **(added)** Loki's Scepter is a relic found in chests:

| Source | Chance |
|---|---|
| End City treasure chest | 10% |
| Stronghold library chest | 5% |
| Bastion treasure chest | 4% |
| Ancient City chest | 3% |
| Woodland Mansion chest | 3% |

To touch the stone, hold the scepter and **right-click** (use it):

* **Holding the gene** in your other hand: the gene and the stone's cosmic energy fuse.
  The gene is used up and the player **awakens as the Scarlet Witch**, with all of Wanda's
  powers. Wanda's Crown appears in their inventory **(added)**.
* **Not holding the gene:** the Mind Stone's energy tears the player apart. They
  **explode and die instantly** (the explosion doesn't break blocks).
* **(added)** **Holding a Synthezoid Core** instead: the Mind Stone brings the synthetic
  body to life and the player **becomes Vision** (section 4).
* **(added)** If you are already the Scarlet Witch or Vision, touching the stone is safe.
  If you have a different power, the gene or core can't bond. Nothing happens, the item
  isn't used up, and holding it still protects you from the stone.
* **(added)** **The Tesseract works too.** A player with no power who holds the gene in one
  hand and uses the **Tesseract** (section 7) with the other awakens as the Scarlet Witch
  instead of becoming Loki: the same awakening as with the scepter, with the same effects,
  title and server-wide message, the gene used up in the same way and Wanda's Crown in the
  inventory. The Tesseract itself is not used up, and nobody is taken anywhere. Without the
  gene it keeps making Loki, and a hero who uses it is simply taken where they look (even
  with a gene in the other hand).
* **(added)** The scepter is also a strong melee weapon (8 attack damage, never breaks). In
  the films it takes over the mind of whoever it touches; that would be too strong, so a
  melee hit only **confuses**:
  * A **player** gets **Nausea** for 8 seconds and **Slowness I** for 3 (Magneto's helmet
    still turns the Nausea away).
  * Any other **creature** gets the **Confused** effect for 6 seconds: it forgets its target
    and can't pick a new one, and it stumbles about aimlessly: it heads for random spots
    nearby, a little faster than it wanders when idle, and changes its mind every few
    ticks. Small blue-green sparks, the Mind Stone's colour, swirl round its head while it
    lasts.
  * The **Ender Dragon, the Wither and the Warden are immune** (the
    `enhanced:confused_immune` tag).

### 2.2 Wanda's Crown — the transformation

* The crown is given when you awaken. **(added)** If it's lost, craft a new one:

  ```
  G R G      G = Gold Ingot
  G . G      R = Block of Redstone
  ```
* When the Scarlet Witch wears the crown she **transforms**:
  * Her skin becomes **the Scarlet Witch's suit**: copper hair, glowing red eyes, the
    burgundy suit and her crown, with a burgundy cape. **(added)** The skin is the community
    skin ["Scarlet Witch"](https://www.minecraftskins.com/skin/18465369/scarlet-witch/) from
    minecraftskins.com; it draws its own crown, so the crown model is hidden while she is
    transformed.
  * **Max health 40** (20 hearts instead of 10).
  * **Armor equal to a full Diamond set** (20 armor, 8 toughness).
  * Never breaks.
* On anyone else the crown is just a crown.

### 2.3 Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (hold) | **Chaos Energy Beam** | A glowing, crackling red beam from Wanda's hand. |
| 2 | G (toggle) | **Enhanced Flight** | Fast, smooth flight with red energy around both hands. |
| 3 | V | **Telekinesis** | Lift creatures, dropped items **or blocks**, move them with your gaze, then launch them. |
| 4 | B (hold) | **Chaos Shield** | A red glowing sphere that stops projectiles and softens explosions. It takes time to form, wears out and can shatter. |
| 5 | N | **The Hex** | A giant red energy dome that only Wanda controls. |

#### Ability 1 — Chaos Energy Beam

* **Look:** a bright, shimmering crimson beam with hot pink wisps, a near-white core,
  a crackling outer glow and sparks where it hits. It's a real rendered beam, not ordinary
  redstone particles. **(added)** While she wields chaos magic (beam, shield, telekinesis)
  her eyes glow red, as in the WandaVision finale.
* **Range:** 32 blocks. The beam stops at the first creature or block.
* **Damage:** **2.5 hearts (5 HP) per hit** for as long as the beam stays on the target.
  Minecraft lets a creature be hurt at most twice per second, so a target takes about
  5 hearts per second. The Ender Dragon (100 hearts) survives about 20 seconds of beam if
  every hit lands on its head, and roughly twice as long on its body or wings (it takes less
  damage there): strong, but not a one-shot.
* **Debuff:** every creature it hits gets **Nausea** for 5 seconds (Magneto's helmet
  blocks this, but not the damage).

#### Ability 2 — Enhanced Flight

* Survival flight. Fast (0.09, almost twice Creative's speed) but always controllable.
* **Look:** while she flies, two glowing red energy auras swirl around Wanda's hands,
  trailing red sparks.

#### Ability 3 — Telekinesis

* **Targets:** any monster, animal, villager, player or dropped item within 32 blocks.
  Bosses (Ender Dragon, Wither, Warden, Elder Guardian) are too powerful to lift.
* **Control:** while held, the target floats where Wanda looks. Press **V** again to
  **launch** it at high speed. When it crashes into a wall or the floor it takes heavy
  impact damage, and it hurts any creature it slams into.
* **Blocks:** look at a block (up to 24 blocks away) and press **V** to tear it out of
  the ground. Throw it at enemies like a shell (8+ damage). It shatters on impact and
  drops its normal item.
  * Liftable: any block with hardness 5 or less (dirt, stone, logs, ores, iron blocks…)
    that has no inventory or other block data and isn't on the
    `enhanced:telekinesis_immovable` tag. Bedrock, obsidian, portals, chests and
    spawners can't be lifted.
  * Lifting respects spawn protection, Adventure mode and claim mods.
* **Sneak + V** sets the target down gently. A held block placed this way lands like
  falling sand.

#### Ability 4 — Chaos Shield

* Hold **B**. A red, glowing sphere of chaos energy grows round Wanda. **(added)** It is a
  commitment, not a reflex:
  * **It takes 3/4 of a second (15 ticks) to form.** It grows from her hands to its full
    size and **blocks nothing until it is complete.**
  * **It holds for at most 8 seconds** (160 ticks, counted from when it is complete), then
    it drops.
  * **It has 30 points of strength.** Everything it stops wears it down by that much (see
    below). At 0 it **shatters**: a burst of red shards and a glassy sound.
  * **It has to recharge** before it can be raised again: **6 seconds** after it drops (B
    released, or hunger), **15 seconds** if it shattered or was held to its limit. The HUD
    counts it down, and B does nothing meanwhile.
* **Blocks every projectile** it meets while it is up: arrows, tridents, Ghast and Blaze
  fireballs, Wither skulls, shulker bullets, potions and snowballs. Projectiles disintegrate
  on the sphere. A projectile costs the shield what it would have done to Wanda: an arrow its
  damage at its speed (about 4 to 6), a trident 8, a fireball 6, a thrown Mjolnir,
  Stormbreaker, dagger or iron shard its own damage, and 2 for anything else. The projectile
  that breaks the shield is still stopped. A thrown Mjolnir or Stormbreaker is one of a kind,
  so it is not destroyed: the shield turns it round and it flies home to Thor.
* **Explosion damage is reduced by 75%.** The 75% the shield takes away is what wears it
  down; if less strength is left than that, the shield shatters and the rest gets through.
  Melee blows are not stopped.
* **Cost:** it burns hunger quickly while held (about 1 food point every 2 seconds), while it
  forms too. When the hunger bar is empty the shield collapses (and has to recharge).
* **HUD:** "forming" while it grows; once it is up, what is left of its strength (green, then
  yellow, then red) and the seconds it can still be held. The sphere flickers as it weakens
  or when its time is nearly up. Her arms rise as it forms.

#### Ability 5 — The Hex

* Press **N** to raise the Hex: a **giant red energy dome** (radius 32 blocks,
  adjustable from 8 to 96) centred on where Wanda stands. It rises over a couple of
  seconds. **(added)** It looks like the Westview Hex from outside: a raspberry-red wall
  of glowing TV-static bricks and scanlines that tints the sky behind it.
* **Only Wanda decides who may enter or leave.** Press **N** again to open the
  **Hex Control** screen:
  * Allow or block each online player.
  * Allow or block creatures (mobs and animals).
  * Make the Hex bigger or smaller.
  * Dissolve the Hex (or press **Sneak + N**).
* One Hex per Wanda. It is saved with the world and collapses if Wanda dies.
* **The price of the Hex wall.** When any creature or player **without powers**
  crosses the wall without Wanda's permission:
  * **95%:** instant death ("was rewritten by the Hex"). Armor, Totems of Undying and
    effects don't help.
  * **5%:** they survive and simply make it through.
* **Crossing includes teleporting:** Ender Pearls, Chorus Fruit and Endermen
  teleporting across the wall face the same 95% / 5% roll.
* **(added)** Anyone **with powers** (Magneto, Quicksilver, Vision, another Scarlet Witch) and
  bosses can't be killed by the wall. They're thrown back as if they hit glass, unless
  Wanda allowed them in.
* **(added)** Pets owned by an allowed player pass through safely.

---

## 3. Quicksilver — the fastest man alive

> The concept: "The X-Men movies' Quicksilver. When he runs, everything slows down, he
> speeds up, and the world starts playing *Sweet Dreams (Are Made of This)*."

### 3.1 Becoming Quicksilver **(added)**

* Craft **Quicksilver's Goggles** and put them on (like Magneto's Helmet, the first time
  you wear them you become Quicksilver for good, if you have no other power):

  ```
  S I S      S = Sugar, I = Iron Ingot
  G D G      G = Glass Pane, D = Block of Diamond
  ```
* While he wears the goggles, Quicksilver's skin becomes Peter Maximoff's outfit from the
  movies: silver hair, a shiny silver jacket over a grey T-shirt, headphones and jeans.
* The goggles protect like an iron helmet and never break.

### 3.2 Quicksilver Time (the main power)

**Press R and time slows down around him, whether he runs or not:** **(changed)** it used to start only while he
sprinted. Now he can stand still, walk, mine, build, fight or eat in the middle of it.

* Everything within **32 blocks** runs **5 times slower**: monsters, animals, villagers,
  falling blocks, TNT fuses, dropped items, smoke, sparks and splashes, and **arrows and thrown things, which hang in
  the air** and crawl forward. **(added)** An arrow that finally lands still hits as hard as it
  would have.
* Other players in range are slowed down heavily (a strong Slowness).
* **Quicksilver speeds up when he sprints**: he runs **4 times faster** than normal and steps up whole
  blocks without jumping. He takes no fall damage while running. Walking, he moves at his normal speed, so he can
  still do precise work.
* **The world starts playing his song**: everyone near him hears the theme from his Walkman for as long as
  Quicksilver Time is on, running or not (if his Walkman is on). It fades in when he turns Quicksilver Time on, keeps
  playing from where it was if he turns it off and on again within 10 seconds, and fades out when he turns it off.
  * The song is *Sweet Dreams (Are Made of This)* by Eurythmics, built into the mod.
  * **(added)** To play a different song, put an `.mp3`, `.ogg` or `.wav` file into
    `config/enhanced/music/` in the Minecraft folder (the mod creates that folder and a short README
    the first time it starts). The first file in alphabetical order is used. Remove it to go back
    to *Sweet Dreams*.
* A cool grey shade at the edges of the screen shows that time is slowed, and while he runs Quicksilver
  leaves a grey blur and fading afterimages behind him (in the X-Men films his speed has no
  coloured glow).
* Cost: it burns hunger the whole time it is on, **half** as fast while he stands or walks and at the full rate
  (0.03 exhaustion a tick) while he runs. When the hunger bar is empty, Quicksilver Time switches itself off (with a
  message) and can't be switched on again until he has eaten. Creative players never run out.

### 3.3 Super Speed (Sneak + R) **(added)**

A second mode, for when only **he** should be fast: Sneak + R turns Super Speed on or off.

* **The world is not slowed at all**: no slow motion, no song, no grey shade. Everyone else lives at the normal pace.
* When he sprints he runs at the **same super speed** as in Quicksilver Time (4 times faster, steps up whole blocks,
  no fall damage), with the same grey blur and afterimages, and Wall & Water Run works too.
* Cost: hunger only while he runs, at the full rate. With an empty hunger bar he can't run, but Super Speed stays on.
* **One mode at a time**: turning one on turns the other off. Both start off, and the choice is saved with the player.

### 3.4 Doing everything faster (in both modes) **(added)**

While Quicksilver Time **or** Super Speed is on, Quicksilver does everything **4 times as fast** (config:
`quicksilver.actionSpeed`), whether he runs or not:

| What | How it is faster |
|---|---|
| Mining | His block break speed is multiplied by 4: stone with a pickaxe takes a quarter of the time. |
| Attacking | His attack speed is multiplied by 4, so the attack cooldown recharges 4 times faster. |
| Eating, drinking, bows, crossbows and everything else you hold the use button for | The use runs 4 times as fast: a steak takes 8 ticks instead of 32, a bow is fully drawn in 5 ticks instead of 20, a crossbow is loaded in 7 instead of 25. Food is still eaten and arrows still fly at full strength. (A spyglass, a brush and a goat horn keep their own pace: there is nothing in them to wait for.) |
| Swimming | His swim speed is multiplied by 4. |
| Placing and breaking blocks one after another | On his own screen, the game's built-in pause between one block and the next (after a right click, and after a block is broken) is cut by the same factor, so holding the button places or breaks blocks without waiting. |

The movement speed bonus and the step-up stay tied to **sprinting**, so he can still walk normally and precisely.

### 3.5 Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (toggle) | **Quicksilver Time** | Turn Quicksilver Time on or off: the world around him is slowed, whether he runs or not. Off by default. **Sneak + R: Super Speed** **(added)**, only he is fast (see 4.3). |
| 2 | G | **Speed Blitz** **(added)** | Look at a creature up to 24 blocks away: Quicksilver is next to it in an instant and lands 6 rapid punches (2 damage each, ignoring the hit cooldown), then knocks it back. Cooldown 4 s. |
| 3 | V | **Pickpocket** **(added)** | Snatch whatever the creature or player you look at (up to 24 blocks) holds in its hand, straight into your inventory. Cooldown 5 s. |
| 4 | B (toggle) | **Wall & Water Run** **(added)** | While running (in Quicksilver Time or Super Speed), run straight up walls you run into, and across water as long as you keep running. |
| 5 | N (toggle) | **Walkman** **(added)** | Turn the theme music on or off. |

## 4. Vision — the Mind Stone in a synthetic body **(added)**

> Wanda's concept mentions the Mind Stone in Loki's Scepter. In the films that same stone gives
> Vision life, so the mod adds him as the second way to use it.

### 4.1 Becoming Vision

* Craft a **Synthezoid Core** (a synthetic body waiting for a mind):

  ```
  R N R      R = Block of Redstone
  N S N      N = Netherite Ingot
  R N R      S = Nether Star
  ```
* Hold **Loki's Scepter** in one hand and the core in the other, and use the scepter. The Mind
  Stone moves into the body: the core is used up and you **become Vision** for good.
* Holding the core also protects you from the stone, exactly like the gene does for Wanda.

### 4.2 The synthetic body

* Vision's skin becomes the MCU Vision: red synthetic face and hands, the golden **Mind Stone in
  his forehead** (it glows in the dark), a green suit with gold trim and a yellow cape.
* **(added)** His second look is **White Vision** (WandaVision episode 9): the same body drained of
  colour, chalk white with glowing pale cyan eyes and no Mind Stone, only an empty hexagonal
  socket of copper circuitry in his forehead. His beam is pale cyan-white instead of gold, and
  he phases in a white aura. Press **H** to switch.
* **Max health 30** (15 hearts) and **8 armor** from his vibranium body.
* He never gets hungry and never runs out of air.

### 4.3 Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (hold) | **Mind Stone Beam** | A pale gold beam from the stone in his forehead, 32 blocks long: 6 damage per hit (two hits per second). Hits creatures, players, the Ender Dragon and End Crystals. |
| 2 | G (toggle) | **Flight** | Survival flight at 0.08, calm and composed. |
| 3 | V (toggle) | **Phase** | Become intangible: walk and float **through walls, floors and creatures**. Nothing can hurt you and projectiles pass through you, but you can't attack or fire the beam. You float instead of falling. You can't turn solid again inside a block (if you're only a little sunk into the floor, you rise out of it). Phasing doesn't get you through Wanda's Hex wall: it is a wall of reality, not of stone. |
| 4 | B (toggle) | **Density** | Become as hard as diamond: +12 armor, no knockback, +6 punch damage with heavy knockback. Too heavy to fly, 30% slower, and he falls faster. Phasing ends density, and you can't turn dense while phased. |
| 5 | N (toggle) | **Human Disguise** | Take on the human look from *WandaVision*: a 1950s sitcom husband in a shirt, tie and cardigan. Only the looks change. |

---

## 5. Tommy Maximoff (Speed) **(added)**

> Wanda's son from *WandaVision*. His speed showed up on Halloween night ("Chip off the old
> Maximoff block! You've got super speed!"), and in the Battle of Westview he crossed the
> street as a teal-green ghost and disarmed a whole S.W.O.R.D. squad.

### 5.1 Becoming Tommy

Through the Witches' Road, with a Scarlet Witch Gene, choosing Tommy (section 6.1).

### 5.2 Abilities

Tommy's speed doesn't slow the world down (that is Quicksilver's trick): he leaves a translucent
teal-green ghost trail and afterimages, low along the ground, like in the Battle of Westview.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (toggle) | **Super Speed** | On from the start: sprinting is super speed, three and a half times as fast, stepping up whole blocks, with no fall damage while running. It burns hunger slowly; with an empty hunger bar he only runs normally. Turn it off to sprint normally. |
| 2 | G | **Disarm Blitz** | In a blink he circles every armed creature within 12 blocks (up to 8 of them) and is back before they notice, their weapons in his arms: creatures lose whatever they hold, players only their weapon (swords, axes, bows, crossbows, tridents, maces, Stormbreaker). Everyone disarmed is slowed for 2 seconds. Mjolnir is far too heavy to snatch. Cooldown 8 seconds. |
| 3 | V | **Whirlwind** | For 3 seconds he runs circles around himself: everything within 8 blocks (creatures, players, arrows in flight) is pulled onto a spinning ring, lifted up to 3 blocks, and flung away when the whirlwind ends. Ghosts of Tommy race round the ring. Cooldown 10 seconds. |
| 4 | B | **Vibrate Through Walls** | He vibrates his molecules and passes straight through the wall in front of him, up to 4 blocks thick (*VisionQuest* trailer; the comics). Not through bedrock or other unbreakable blocks, nor through a Hex he isn't allowed into. Cooldown 2 seconds. |
| 5 | N | **Molecular Acceleration** | *(comics)* He vibrates whatever he touches (up to 5 blocks away) until it shatters: a block breaks and drops its item (anything up to the hardness of obsidian, never bedrock; it respects spawn protection and claims), a creature takes 6 damage and is blasted back. Cooldown 1 second. |

### 5.3 Looks

*Halloween* (his WandaVision costume, a mini-Quicksilver: silver hair teased up, a blue tee with
a silver lightning bolt), *Battle of Westview* (the green track jacket), *Speed* (the comics
costume, green and white with amber goggles), and *Your own skin*.

---

## 6. Billy Maximoff (Wiccan) and the Witches' Road **(added)**

> Wanda's other son. In *WandaVision* he hears every thought around him and freezes a squad of
> S.W.O.R.D. soldiers; in *Agatha All Along* his own chaos magic makes the Witches' Road real.

### 6.1 The Witches' Road Door

* **Crafting:**

  ```
  A L A      A = Amethyst Shard
  T E T      L = Soul Lantern
  A L A      T = Dark Oak Trapdoor, E = Eye of Ender
  ```
* A pair of weathered cellar doors set flat in the floor (you can stand on it), outlined in a
  faint blue-green glow, like the door in the basement in *Agatha All Along*. Wood: an axe breaks
  it fastest; pistons can't push it.
* **Seeking the Road.** A player **with no power** right-clicks the door **holding a Scarlet Witch
  Gene**. A ring of blue-green light runs around the door for two seconds, the doors open onto a
  spiral stair among glowing roots, and the Road asks: **"Seekest thou the Road?"** Choose
  **Billy: Wiccan** or **Tommy: Speed** (section 5). One gene is used up. Anyone else holding a gene
  can join the ritual and choose too. The door closes again five seconds after it opens.
  * Without a gene the door won't open ("The Road only opens for witch blood").
  * A hero already "walks their own road".
* **Walking the Road.** The Scarlet Witch and Billy (whose magic made it) can travel the Road:
  right-clicking a door without a gene opens it and takes them to the **next Witches' Road door**
  in the same dimension (in the order the doors were placed, going round), arriving on it in a
  column of light. A door that is gone is skipped. Whatever they hold with telekinesis stays
  behind, and the Hex wall is respected. Cooldown 3 seconds.

### 6.2 Abilities

Billy's magic is blue with a white-hot core, crackling and branching like a Tesla coil.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R | **Hex Bolt** | A crackling blue arc from his hand to the first creature he looks at within 24 blocks: 6 damage, and it forks on to the two nearest creatures within 5 blocks of it (3 damage each). Everyone around sees the bolts. Cooldown half a second. |
| 2 | G (toggle) | **Flight** | Calm, weightless flight with open hands, like his mother's (speed 0.08). |
| 3 | V | **Telekinesis** | Wanda's telekinesis, in his blue: creatures, dropped items, minecarts, boats, primed TNT, falling blocks, **and projectiles in flight**: an arrow he catches hangs in front of him, pointing where he looks, and flies off at his command. He doesn't tear blocks out of the ground (that is Wanda's). Press again to launch, **Sneak** + the key to put down gently. |
| 4 | B | **Freeze** | Every creature and player within 12 blocks in a 90° cone in front of him, in his line of sight, is **spellbound** for 5 seconds: they can't move, jump or attack, can't pick a new target, and blue sparks circle their heads. The Ender Dragon and the Wither are too strong for it. Cooldown 12 seconds. |
| 5 | N (toggle) | **The Noise** | He hears every mind within 32 blocks: every creature glows through walls, **red** if it is hostile and about to attack, **blue** otherwise. Whispers crowd his head, louder the more minds are near. |

**The crown.** While he flies, and for three seconds after each spell, a glowing chrome crown
forms on his head, in shades from ice-white to deep blue (the *Wiccan* look already has it).

### 6.3 Looks

*Wiccan* (*Agatha All Along*), *Teen* (on the Witches' Road), *Halloween* (his *WandaVision*
costume), *Wiccan (comics)*, and *Your own skin*.

---

## 7. Loki — the God of Mischief **(added)**

> "I am Loki of Asgard, and I am burdened with glorious purpose." In *Avengers: Endgame* the
> Loki of 2012 grabs the Tesseract and vanishes in a swirl of blue light: the escape that
> starts the whole story of the TVA.

### 7.1 Becoming Loki

* **The Tesseract.** The cube with the Space Stone inside. It has no recipe: it is a very rare
  find in chests:

  | Source | Chance |
  |---|---|
  | End City treasure chest | 8% |
  | Stronghold library chest | 6% |
  | Woodland Mansion chest | 5% |
  | Ancient City chest | 4% |

  It is fireproof, doesn't stack and is never used up.
* **The escape.** A player **with no power** uses it (right click). The Space Stone swirls
  round them in a rising spiral of blue light, they vanish in a flash and reappear **100 to 250
  blocks away**, in the same dimension, on safe ground (solid, with room for the whole body,
  no liquid, nothing burning, and no Hex wall in between; it tries 12 random spots, and if none
  is safe they stay where they were). They **awaken as Loki**: a title on screen, a burst of green
  light and a message to the whole server. The cube needs one second before it can be used again.
* **(added)** **With a Scarlet Witch Gene in the other hand,** a player with no power awakens
  as the **Scarlet Witch** instead (section 2.1), exactly as with Loki's Scepter: no swirl, no
  escape, the gene used up, Wanda's Crown in the inventory. The cube is not used up and needs
  one second before it can be used again.
* **A hero's jump.** Someone who already has a power (Loki himself too) isn't changed by the
  cube: the Space Stone takes them to the block they look at, up to **48 blocks** away, on top of
  it if there is room. The Hex wall still stops them. Cooldown 5 seconds.

### 7.2 Abilities

Loki is Asgardian-tough: **24 health** (12 hearts). His magic is green. Every look has the same
five abilities.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R | **Daggers** | He throws a conjured dagger from his hand, fast and almost flat, for up to 3 seconds of flight: **5 damage** (`daggerDamage` in the server config). It was never real: whatever it hits, or when its flight ends, it breaks up into green sparks, so it can never be picked up, and it passes through his own duplicates. Cooldown 0.4 seconds. **Sneak + R** throws a **fan of three**, 10° apart. Cooldown 1.5 seconds. |
| 2 | G | **Duplicates** | *"Surprise!"* He turns invisible for 5 seconds (his held items and name are hidden too) and **three** illusory copies of him, looking exactly like him and holding what he holds, walk off in different directions, 12 blocks each. Every creature within 24 blocks that was after him goes after the nearest copy instead. A copy dissolves in one hit into warm-white streaks of light with a flash of his green, deals no damage, drops nothing and fades after 8 seconds anyway; they all dissolve if he loses his power. Drinking milk brings him back into view early. Cooldown 15 seconds. |
| 3 | V | **Shapeshift** | He takes the shape of the creature or player he looks at, up to 16 blocks away, in a flash of green light, and every creature of that kind within 48 blocks lets go of him: creatures never pick one of their own kind as a target (a zombie leaves him alone, a husk still comes for him). A player is copied with their skin and their name. Looking at nothing turns him back. The Ender Dragon, the Wither, the Warden, the Elder Guardian and his own duplicates can't be copied. No cooldown. |
| 4 | B | **Vanish** | Gone in a green flash and back where he looks, up to 20 blocks away: on top of the block he looks at, or right behind the creature, facing its back; with nothing in reach, in mid-air where he looks. Not where there is no room, and not through a Hex wall. Cooldown 3 seconds. |
| 5 | N (toggle) | **Frost Giant** | His true Jotun form: blue-grey skin with raised ridges and burning red eyes (painted onto whichever look he wears, his own skin included). **+4 armor**, he never freezes and the cold can't hurt him, still water freezes into frosted ice where he walks (within 3 blocks, like Frost Walker), and every blow he lands slows the creature (**Slowness II for 3 seconds**) and freezes it over. Creatures that are immune to the cold aren't affected. Press again to look Asgardian again. |

### 7.3 Looks

*God of Mischief* (the default: black and green leathers with gold, and the cape), *TVA Variant*
(the beige TVA jacket, no cape; his magic turns sand and amber), *God of Stories* (green and gold,
with a cape; olive and gold magic), and *Your own skin*. A look changes only how Loki looks and the
colour of his magic: the abilities are the same in every look, the Frost Giant included.

---

## 8. Thor — the God of Thunder **(added)**

> "Whosoever holds this hammer, if he be worthy, shall possess the power of Thor."

### 8.1 Mjolnir, Stormbreaker and the worthy

* **Forging Mjolnir.** "Forged in the heart of a dying star, from the sacred metal of Uru":

  ```
  N S N      N = Netherite Ingot (Uru)
  N H N      S = Nether Star (the heart of a dying star)
    R        H = Heavy Core, R = Breeze Rod (the handle)
  ```
* **The worthy.** Only the worthy can lift a Mjolnir nobody owns yet: Thor himself, **Vision**
  (who lifted it in *Age of Ultron*), players in Creative or Spectator mode, and anyone who has
  proven themselves a hero by **saving a village from a raid** (the *Hero of the Village*
  advancement). The server config can make everyone worthy.
* **One hammer, one owner.** A freshly forged Mjolnir (or one from the Creative tab) belongs to
  nobody. The first worthy player to lift it **claims it, for good**: when it comes into their
  inventory or hand, when they lift it off the ground, when they forge it, or when Thor calls it
  to himself. The hammer remembers its owner (an `enhanced:owner` item component, kept when it is
  thrown, dropped or set down), and its tooltip says so: *Bound to Steve*, or *Unclaimed* until
  then. From then on **only its owner can lift it**: not another Hero of the Village, not Vision,
  not another Thor. Players in Creative or Spectator mode can still lift any Mjolnir, so
  operators can move it, but holding someone else's never changes its owner. The server config
  (`mjolnirOwnerOnly`) can bring back the old rule, where any worthy player can lift any Mjolnir.
* **Everyone else can't hold it.** A player who is not worthy, or a worthy one who is not its
  owner, finds Mjolnir far too heavy (crafted, taken from a chest, anywhere in their inventory):
  it slips to the ground at their feet, and the message names its owner. They can't pick it up
  either.
* **Wherever Mjolnir comes to rest, it stays.** A dropped Mjolnir lands and stands on its
  head, handle up, like in the crater in New Mexico: a block that **nothing can break, blow up
  or push**, and that remembers whose it is. Only its owner can lift it (right click); anyone else
  gets *"It won't budge. Only Steve can lift it."* Thor can set it down himself with
  **Sneak + right click** on a block.
* **Becoming Thor.** The first time a worthy player with no other power lifts (and so claims) a
  Mjolnir nobody owned, or lifts their own, lightning strikes and they **become Thor**. Vision can
  claim it and fight with it, but stays Vision.
* Mjolnir hits hard (10 attack damage, slow like an axe), never breaks, and fire and lava
  can't destroy it. Speedsters can't snatch it and telekinesis can't lift it.
* **Stormbreaker.** Forged in Nidavellir in the heart of a dying star, its handle grown from
  Groot's arm:

  ```
  N N        N = Netherite Ingot (Uru)
  N S        S = Nether Star (the dying star)
    L        L = any log (Groot's arm)
  ```

  Stormbreaker carries no enchantment of worthiness: **anyone can wield it**. It is an axe (it
  chops wood, strips logs and knocks shields aside) that hits harder than any other (12 attack
  damage), never breaks and can't burn. It doesn't make anyone Thor, but whoever holds it can call
  **the Bifrost** (8.4). In Thor's hands it does everything Mjolnir does: he throws it, calls it
  back, flies with it and calls lightning down through it. Unlike Mjolnir, a speedster can snatch
  it out of a hand.

### 8.2 The Asgardian body

* **Max health 30** (15 hearts), **6 armor**, half knockback.
* Lightning never hurts or burns Thor. With Mjolnir or Stormbreaker in hand he takes no fall damage.

### 8.3 Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R | **Throw / Call Back** | With Mjolnir or Stormbreaker in hand: throw it. It flies straight, tumbling, smashing everything in its path (Mjolnir 10 damage, Stormbreaker 12, and heavy knockback), until it hits a wall or has flown 40 blocks, then turns back and flies home to his hand, straight through walls, hitting everything on the way back too. With neither in hand: **call the nearest one back**, from his inventory, the air, the ground anywhere within 64 blocks (dropped, or Mjolnir resting as a block), or the hands of a creature that picked it up (never from another player, and never another player's Mjolnir: he calls his own, one nobody owns yet, or Stormbreaker). |
| 2 | G (toggle) | **Flight** | Thor flies by spinning his weapon: only with Mjolnir or Stormbreaker in hand (or while it flies back to him). Fast: 0.11. Hovering, he holds it up to the sky; at speed it leads the way. |
| 3 | V | **Lightning** | He raises his weapon and lightning pours down into it from the sky and leaps to what he looks at, up to 64 blocks away: 8 damage there and less to everything within 3 blocks, with the usual effects of lightning on creatures (creepers become charged, pigs turn into zombified piglins), but no fire on blocks. Without a weapon he can still call it, straight from the sky. Cooldown 1.5 seconds. |
| 4 | B (toggle) | **God of Thunder** | *Thor: Ragnarok*: his eyes blaze white-blue and lightning crawls over his body. +4 attack damage, +6 armor toughness, every blow he lands arcs lightning to the next two creatures nearby (4 damage each), a thrown weapon arcs too, and each Lightning strikes three times. It burns hunger; with an empty hunger bar the thunder calms. |
| 5 | N | **Bifrost** | "Heimdall! Open the Bifrost!" With Mjolnir or Stormbreaker in hand: **N** opens the Bifrost screen (home, his saved places, save this one), **Sneak + N** takes him to the spot he looks at, up to 512 blocks away. See 9.4. |

All of Thor's lightning is the MCU's: a white-hot core, electric blue, a deep blue halo.

### 8.4 The Bifrost and saved places

* **The Bifrost answers the weapon, not the power.** Thor calls it with **N**; anyone else
  holding **Mjolnir or Stormbreaker** calls it by **using the weapon** (right click). With
  neither in hand it doesn't come.
* **The Bifrost screen** shows:
  * **Home**: the respawn point (bed or respawn anchor, otherwise the world spawn), in whatever
    dimension it is;
  * every **saved place**, with how far away it is, or which world it is in (the Overworld, the
    Nether, the End, or a dimension added by another mod); **✕** forgets one;
  * a field to **save the place you stand on**, under a name of up to 32 characters, facing the
    way you face. Saving a name again moves that place. Up to 24 places.
* **Sneak + N** (or Sneak + right click with the weapon): to the spot you look at, up to 512
  blocks away.
* A column of swirling rainbow light gathers around the traveller for a second and a half,
  holding them in place, then takes them, **across dimensions if need be**. They land exactly
  on the saved spot, facing the saved way; if it has been built over since, on the first free
  spot above it. It burns its circle of runes into the ground where it lands and where it left,
  glowing gold and fading over a minute.
* Arriving inside a Hex counts as crossing its wall: the Hex judges the traveller as it judges
  everyone else.
* **Cooldown 60 seconds**, the same for everyone, shown on the screen (and on Thor's HUD).
  The places and the cooldown are saved with the player and kept on death, and only that player
  sees them.

### 8.5 Looks

*Thor* (2011/*The Avengers*: black and silver armour with the six chest discs, red cape),
*Ragnarok* (the God of Thunder at the end of the film: short hair, eyepatch), *Love and Thunder*
(gold, electric blue and crimson armour), *Mighty Thor* (Jane Foster), and *Your own skin*.

---

## 9. MCU colours **(added)**

Every power's light follows the films and shows:

| Hero | On screen | In the mod |
|---|---|---|
| Scarlet Witch | Chaos magic: a crimson tangle of energy with hot pink highlights; her eyes glow red | Crimson beam with pink wisps and a near-white core; red glowing eyes while she wields magic |
| The Hex | From outside, a translucent wall of glowing raspberry-red "cathodic" bricks and scanlines | Raspberry-red CRT bricks and scanlines that tint the sky |
| Magneto | No glow at all in the X-Men films: metal just moves | Colourless field lines, iron-grey particles |
| Quicksilver | Fox Quicksilver: no coloured glow | A grey blur and afterimages |
| Vision | Pale gold Mind Stone beam; a greenish-yellow aura while phasing | Pale gold beam and amber stone; greenish-yellow phasing aura |
| White Vision | A pale cyan-white beam; no Mind Stone | Cyan-white beam; an empty hexagonal socket of copper circuitry; a white phasing aura |
| Tommy | A teal-green ghost trail | Teal-green trail, ghosts and mist |
| Billy | Blue magic with a white core, like a Tesla coil | Blue arcs and glows |
| Loki | Green seidr; the blue skin and red eyes of a Frost Giant | Green magic (sand and amber in the TVA look, olive and gold in the God of Stories'); a blue-grey Frost Giant with burning red eyes and a cold light |
| Thor | Blue-white lightning; rainbow Bifrost | White-hot core, electric blue, deep blue halo; rainbow Bifrost |

---

## 10. Operator commands **(added)**

| Command | Effect |
|---|---|
| `/enhanced power get [player]` | Show a player's power. |
| `/enhanced power set <players> <none\|magneto\|scarlet_witch\|quicksilver\|vision\|tommy\|billy\|loki\|thor> [look]` | Give or remove a power, optionally straight into one of its looks. |
| `/enhanced look <players> <look>` | Change the look of every given player whose hero has that look (e.g. `tva`, `white_vision`, `mighty_thor`). |
| `/enhanced hex list` | List all Hexes in the current dimension. |
| `/enhanced hex clear` | Dissolve every Hex in the current dimension. |

## 11. Server configuration **(added)**

All the main numbers (beam damage, Hex death chance, Hex radius, flight speeds,
shrapnel damage, shield reflect chance, the Chaos Shield's wind-up, time limit, strength and cooldowns, how much
Quicksilver Time slows the world, how much faster Quicksilver does everything, Tommy's speed, Loki's dagger damage, how
long Loki's Scepter confuses, Mjolnir's and Stormbreaker's damage and range, who is worthy of
Mjolnir and whether it belongs to the first worthy player who lifts it (`mjolnirOwnerOnly`), the Bifrost's
cooldown…) can be changed in `world/serverconfig/enhanced-server.toml`.

# Enhanced — Full Design

**Enhanced** is a Minecraft mod (NeoForge, Minecraft 1.21.1) that lets players become
one of the *Enhanced*:

* **Magneto**, the Master of Magnetism
* **Wanda Maximoff, the Scarlet Witch**
* **Photon (Monica Rambeau)**, the rare survivor of Wanda's Hex
* **Quicksilver (Peter Maximoff)**, the fastest man alive
* **Vision**, the synthezoid with the Mind Stone in his forehead **(added)**

This document completes the original concepts by Eilon Kolodkin-Gal (Magneto, the
Scarlet Witch, and Quicksilver's slow-motion run). Everything the concepts said is kept.
Where they left a gap (where an item comes from, what key does what, exact numbers), the
gap is filled in here, and every addition is marked **(added)**.

---

## 0. Rules for every power

| Rule | Detail |
|---|---|
| One power per player | A player has exactly one of: *none*, Magneto, Scarlet Witch, Photon, Quicksilver, Vision. Powers never stack. |
| Permanent | Once unlocked, a power survives death, logging out and changing dimension. Only an operator command can remove it (`/enhanced power set <player> none`). |
| Ability keys **(added)** | Ability 1–5 are bound to **R, G, V, B, N** by default (rebind under *Options → Controls → Enhanced Powers*). Each key triggers the ability with the same number in that hero's list. |
| Flight | Flying works like Creative mode: double-tap **Space**, or press the hero's flight key. Quicksilver doesn't fly, he runs. |
| HUD **(added)** | The top-left corner shows your hero, each ability with its key, cooldowns, toggles and resources. |
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
  other Magnetos' shrapnel) coming at Magneto are caught by the field. Each one
  either **loses its momentum and drops to the floor** or **flies straight back at
  whoever shot it** (50/50).
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
  body to life and the player **becomes Vision** (section 5).
* **(added)** If you are already the Scarlet Witch or Vision, touching the stone is safe.
  If you have a different power, the gene or core can't bond. Nothing happens, the item
  isn't used up, and holding it still protects you from the stone.
* **(added)** The scepter is also a strong melee weapon (8 attack damage, never breaks).

### 2.2 Wanda's Crown — the transformation

* The crown is given when you awaken. **(added)** If it's lost, craft a new one:

  ```
  G R G      G = Gold Ingot
  G . G      R = Block of Redstone
  ```
* When the Scarlet Witch wears the crown she **transforms**:
  * Her skin becomes **Wanda's full suit from *WandaVision*** (crimson and black
    suit and cape).
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
| 4 | B (hold) | **Chaos Shield** | A red glowing sphere that stops all projectiles. |
| 5 | N | **The Hex** | A giant red energy dome that only Wanda controls. |

#### Ability 1 — Chaos Energy Beam

* **Look:** a bright, shimmering red energy beam with a crackling outer glow and
  sparks where it hits. It's a real rendered beam, not ordinary redstone particles.
* **Range:** 32 blocks. The beam stops at the first creature or block.
* **Damage:** **2.5 hearts (5 HP) per hit** for as long as the beam stays on the target.
  Minecraft lets a creature be hurt at most twice per second, so a target takes about
  5 hearts per second. The Ender Dragon (100 hearts) survives about 20 seconds of beam:
  strong, but not a one-shot.
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

* Hold **B**. A red, glowing sphere of chaos energy surrounds Wanda.
* **Blocks 100% of projectiles**: arrows, tridents, Ghast and Blaze fireballs, Wither
  skulls, shulker bullets, potions and snowballs. Projectiles disintegrate on the sphere.
* **Explosion damage is reduced by 75%.**
* **Cost:** it burns hunger quickly while held (about 1 food point every 2 seconds).
  When the hunger bar is empty the shield collapses.

#### Ability 5 — The Hex

* Press **N** to raise the Hex: a **giant red energy dome** (radius 32 blocks,
  adjustable from 8 to 96) centred on where Wanda stands. The wall shimmers with a
  hexagon pattern and rises over a couple of seconds.
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
  * **5%, the Monica Rambeau effect:** they survive. A player who survives has their
    **DNA rewritten** and permanently becomes **Photon** (see section 3). A surviving
    mob simply makes it through.
* **Crossing includes teleporting:** Ender Pearls, Chorus Fruit and Endermen
  teleporting across the wall face the same 95% / 5% roll.
* **(added)** Anyone **with powers** (Magneto, Photon, Quicksilver, Vision, another Scarlet Witch) and
  bosses can't be killed by the wall. They're thrown back as if they hit glass, unless
  Wanda allowed them in.
* **(added)** Pets owned by an allowed player pass through safely.

---

## 3. Photon — the Monica Rambeau effect **(added)**

The concept says a 5% survivor of the Hex wall "permanently unlocks strong
energy-based superhero abilities." This section defines them, based on
Monica Rambeau after she crossed the Hex.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R | **Photon Blast** | Fire a bolt of pure light (6 damage). If you have stored energy, the bolt is supercharged (+6 damage, costs 10 energy). Cooldown 0.6 s. |
| 2 | G (toggle) | **Photon Flight** | Survival flight at 0.085 with a trail of light. |
| 3 | V | **Energy Absorption** | **Passive:** absorbs **50% of all energy damage** (fire, lava, lightning, explosions, magic, the Chaos Beam, other Photons) and stores it (max 100). **Press V:** release the stored energy as a shockwave that damages and knocks back everything within 6 blocks (needs at least 10 energy). |
| 4 | B | **Phase Shift** | Become intangible for 3 seconds. You take no damage and projectiles pass through you, but you can't attack (no melee, no Photon Blast, no Energy Release). A white energy shimmer covers your body. Cooldown 12 s. |
| 5 | N (toggle) | **Spectrum Vision** | See in the dark and see every creature within 48 blocks through walls. Your eyes glow. |

---

## 4. Quicksilver — the fastest man alive

> The concept: "The X-Men movies' Quicksilver. When he runs, everything slows down, he
> speeds up, and the world starts playing *Sweet Dreams (Are Made of This)*."

### 4.1 Becoming Quicksilver **(added)**

* Craft **Quicksilver's Goggles** and put them on (like Magneto's Helmet, the first time
  you wear them you become Quicksilver for good, if you have no other power):

  ```
  S I S      S = Sugar, I = Iron Ingot
  G D G      G = Glass Pane, D = Block of Diamond
  ```
* While he wears the goggles, Quicksilver's skin becomes Peter Maximoff's outfit from the
  movies: silver hair, a shiny silver jacket over a grey T-shirt, headphones and jeans.
* The goggles protect like an iron helmet and never break.

### 4.2 Quicksilver Time (the main power)

**When Quicksilver sprints, time slows down around him:**

* Everything within **32 blocks** runs **5 times slower**: monsters, animals, villagers,
  falling blocks, TNT fuses, dropped items, and **arrows and thrown things, which hang in
  the air** and crawl forward. **(added)** An arrow that finally lands still hits as hard as it
  would have.
* Other players in range are slowed down heavily (a strong Slowness).
* **Quicksilver speeds up**: he runs **4 times faster** than normal and steps up whole
  blocks without jumping. He takes no fall damage while running.
* **The world starts playing his song**: everyone near him hears the theme from his Walkman.
  It fades in when he starts running, keeps playing from where it was if he stops and starts
  again within 10 seconds, and fades out when he stops.
  * The song is *Sweet Dreams (Are Made of This)*. The recording is copyrighted, so it is not in
    this repository. Each player plays **their own copy** of it: put an `.mp3`, `.ogg` or `.wav`
    file into `config/enhanced/music/` in the Minecraft folder (the mod creates that folder and a
    short README the first time it starts). The first file in alphabetical order is used.
  * A personal build can also carry the song inside the jar: put it at
    `personal/quicksilver_theme.mp3` (or `.ogg` / `.wav`) before building. The `personal/` folder is
    ignored by git, so the song never ends up in the repository.
  * With no song anywhere, the mod plays an original synth tune instead.
* A blue tint at the edges of the screen shows that time is slowed, and Quicksilver leaves a
  silver-blue streak behind him.
* Cost: it burns hunger while he runs. With an empty hunger bar, he can only run normally.

### 4.3 Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (toggle) | **Quicksilver Time** | On by default: sprinting starts Quicksilver Time. Turn it off to sprint normally. |
| 2 | G | **Speed Blitz** **(added)** | Look at a creature up to 24 blocks away: Quicksilver is next to it in an instant and lands 6 rapid punches (2 damage each, ignoring the hit cooldown), then knocks it back. Cooldown 4 s. |
| 3 | V | **Pickpocket** **(added)** | Snatch whatever the creature or player you look at (up to 24 blocks) holds in its hand, straight into your inventory. Cooldown 5 s. |
| 4 | B (toggle) | **Wall & Water Run** **(added)** | During Quicksilver Time, run straight up walls you run into, and across water as long as you keep running. |
| 5 | N (toggle) | **Walkman** **(added)** | Turn the theme music on or off. |

## 5. Vision — the Mind Stone in a synthetic body **(added)**

> Wanda's concept mentions the Mind Stone in Loki's Scepter. In the films that same stone gives
> Vision life, so the mod adds him as the second way to use it.

### 5.1 Becoming Vision

* Craft a **Synthezoid Core** (a synthetic body waiting for a mind):

  ```
  R N R      R = Block of Redstone
  N S N      N = Netherite Ingot
  R N R      S = Nether Star
  ```
* Hold **Loki's Scepter** in one hand and the core in the other, and use the scepter. The Mind
  Stone moves into the body: the core is used up and you **become Vision** for good.
* Holding the core also protects you from the stone, exactly like the gene does for Wanda.

### 5.2 The synthetic body

* Vision's skin becomes the MCU Vision: red synthetic face and hands, the golden **Mind Stone in
  his forehead** (it glows in the dark), a green suit with gold trim and a yellow cape.
* **Max health 30** (15 hearts) and **8 armor** from his vibranium body.
* He never gets hungry and never runs out of air.

### 5.3 Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (hold) | **Mind Stone Beam** | A golden beam from the stone in his forehead, 32 blocks long: 6 damage per hit (two hits per second). Hits creatures, players, the Ender Dragon and End Crystals. |
| 2 | G (toggle) | **Flight** | Survival flight at 0.08, upright and calm. |
| 3 | V (toggle) | **Phase** | Become intangible: walk and float **through walls, floors and creatures**. Nothing can hurt you and projectiles pass through you, but you can't attack or fire the beam. You float instead of falling. You can't turn solid again inside a block (if you're only a little sunk into the floor, you rise out of it). Phasing doesn't get you through Wanda's Hex wall: it is a wall of reality, not of stone. |
| 4 | B (toggle) | **Density** | Become as hard as diamond: +12 armor, no knockback, +6 punch damage with heavy knockback. Too heavy to fly, 30% slower, and he falls faster. Phasing ends density, and you can't turn dense while phased. |
| 5 | N (toggle) | **Human Disguise** | Take on the human look from *WandaVision*: a 1950s sitcom husband in a shirt, tie and cardigan. Only the looks change. |

---

## 6. Operator commands **(added)**

| Command | Effect |
|---|---|
| `/enhanced power get [player]` | Show a player's power. |
| `/enhanced power set <players> <none\|magneto\|scarlet_witch\|photon\|quicksilver\|vision>` | Give or remove a power. |
| `/enhanced hex list` | List all Hexes in the current dimension. |
| `/enhanced hex clear` | Dissolve every Hex in the current dimension. |

## 7. Server configuration **(added)**

All the main numbers (beam damage, Hex death chance, Hex radius, flight speeds,
shrapnel damage, shield reflect chance, how much Quicksilver Time slows the world…) can be changed in
`world/serverconfig/enhanced-server.toml`.

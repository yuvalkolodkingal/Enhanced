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
* **Jean Grey, the Phoenix**, the telepath who reads minds and burns with cosmic fire, with the **Phoenix Burst** **(added in 3.0)**
* **Peter Parker, Spider-Man**, who goes everywhere on a web, sticks to walls and ceilings, feels danger coming and wears the Homemade Suit, the Iron Spider and the Brand New Day suit **(added in 3.0)**
* **Thanos, the Mad Titan**: a **boss** who gathers up to five Infinity Stones as he fights, never the sixth, and, for those who beat him, the **Infinity Gauntlet** and a **hero** of five stones **(added in 3.0)**
* **Ultron**, the artificial intelligence of the Mind Stone: a **boss** with an army of Sentries who lifts the ground into the sky and moves into a new body when one is destroyed, and, for those who beat him, a **hero** of plasma, anti-gravity, uploads and Sentries **(added in 3.0)**
* **Apocalypse (En Sabah Nur)**, the first mutant: a **boss** whose golden shield stops everything except mind power, and, for those who beat him, a **hero** of sand, levitation, portals and a shield of his own **(added in 3.0)**, turned down by one master factor **(3.2)**
* **The Witches' Road trials**: three challenges (a parkour, a structure to copy, an ice-boat race) in a dimension of their own that lead to the choice of Billy's or Tommy's power **(added in 3.2)**
* **The Book of the Vishanti**, with which a sorcerer saves places, teleports and raises a ward no monster can enter, and **the Darkhold**, Agatha's book with a purple beam and a retaliation against magic **(added in 3.2)**
* **Agatha Harkness**, whom the Darkhold makes of a player with no power; **Cyclops**, whose ruby-quartz glasses hold back his eyes; and **Professor X**, young or old, who hears thoughts and takes over bodies **(added in 3.3)**
* **The Hero Creator**: any hero at once, or a hero of the player's own from thirty powers with a strength each, saved as JSON **(added in 3.3)**
* **The Hero Creator as good as the heroes**: all 81 abilities of the heroes for a custom hero, ten keys with Sneak, the heroes' animations, look layers, a new editor, and Choose a Hero giving the hero's items **(added in 3.4)**
* **Hydra outposts, Mjolnir's craters and the Mutant School** in the world, the **music of the Witches' Road**, walls that really keep powers out (Billy's barrier, Wanda's Hex and its **Hex Runes**, the ward) and animations without flashing **(added in 3.3)**
* **Quicksilver and Tommy** with separate slow-time and running keys, running up walls and along ceilings, and new strikes; **Spider-Man's Web Assist**, easy swinging in forests and open land **(added in 3.3)**
* **Asgard**, the Realm Eternal, a dimension reached only by the Bifrost (which now takes everyone in its circle), with **Heimdall**, **Odin** and **Hela** (a boss, then a hero), the **Asgardians** who live there, a world music folder and new Walkman themes, colours for every ability of a custom hero, and every Sneak variant a card **(added in 3.5)**
* **The six Infinity Stones** as real items (each with two powers, only for the gods, the Scarlet Witch and the Phoenix, with three vessels for the others), the crafted **Infinity Gauntlet**, the Stone Wheel and the **Snap**; the **Sanctum Sanctorum**, **Avengers Tower**, the **End tunnels** and the **Vormir Cliffs** with its sacrifice; **Doctor Strange**, the **Hulk**, and the **Hydra soldiers** of the outposts **(added in 3.6)**
* **Wolverine** and **Deadpool**, with a shared **healing factor** that only the Infinity Stones, chaos magic, the Allfather's power, the Phoenix or poison defeat; the **X-Men lower lab** under every Mutant School, with its secret entrance, the Cure Station, the Weapon X rack and pod, and the amplified **Cerebro**; the **Stone Locator**; **Agatha's Cottage**, where Agatha, Nicky and Rio live happily; and **meeting Death** when you die **(added in 3.7)**

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
| One power per player | A player has exactly one of: *none*, Magneto, Scarlet Witch, Quicksilver, Vision, Tommy, Billy, Loki, Thor, Phoenix, Spider-Man, Thanos, Ultron, Apocalypse. Powers never stack. |
| Permanent | Once unlocked, a power survives death, logging out and changing dimension. Only an operator command can remove it (`/enhanced power set <player> none`). |
| Ability keys **(added)** | Ability 1–5 are bound to **R, G, V, B, N** by default (rebind under *Options → Controls → Enhanced Powers*). Each key triggers the ability with the same number in that hero's list. One more key, **Z** (*Vision, That One!*), belongs to the Scarlet Witch alone: it is her order to the Vision her Hex raises (section 2). **(added in 3.1)** **Y** is the one key outside R to N, H and Z: *Build Magic*, for the Scarlet Witch and Billy alone (section 18). **(added in 3.2)** **Ctrl + I** (*Build Magic: Instant*) is the Scarlet Witch's Instant switch (18.14), Sneak + **B** is Billy's Hex Barrier (6.5), and the two books of section 20 are used, not bound to a key. |
| Flight | Flying works like Creative mode: double-tap **Space**, or press the hero's flight key. Quicksilver doesn't fly, he runs; Spider-Man doesn't fly either: he zips and swings on webs (section 14), and Thanos walks and steps through portals (section 15). |
| Flight and run animation **(added)** | Every hero's pose is blended from the situation instead of switched, so nothing snaps and other players see it too. Each flier has a flight of his own. Magneto glides with his arms spread, leaning about 40° at full speed. Wanda floats upright with her palms open and stretches out, arms ahead, about 60° forward in a sprint. Vision is a straight body with arms at his sides, leaning about 65° at speed. Billy flies like a kid, arms wide, then ahead at a sprint. Thor leans about 90° behind his weapon, which really is in his fist in every pose (Mjolnir's handle and Stormbreaker's pass through it), whirls on its strap with a swoosh while he takes off and hovers, and is thrust ahead of him at speed; he overshoots a little and settles like a heavy hero. Every flier takes off with a gesture of his own, banks into turns, tips over in a dive, leans back when he climbs and brakes when he stops (Thor lands as a hero, planting the weapon). Quicksilver (in Quicksilver Time and in Super Speed) and Tommy at super speed run with their own stride, with ghosts of the stride behind them and a skid when they stop, and everyone else sprints and walks with a character of his own. Every flier leaves a trail of his own along the path he flew (crimson threads from Wanda's hands, golden threads from Vision's shoulders, field lines from Magneto's, a double helix from Billy's, lightning from Thor's boots), with white wind streaks at speed, a ring at take-off, a shockwave when a hero lands and a sonic boom when Thor or Vision break the sound barrier. Three client options (in `config/enhanced-client.toml`, section `[animation]`, and behind the *Config* button of Enhanced in the Mods screen; nothing there is synced) change it: *Hero animations* turns the hero animation off (vanilla poses), *Reduced motion* tones it down (hover bob and drift at a third, no trembling hands, jitter or camera shake), and *Flight effects* turns off the trails, wind and rings. |
| Ability animation **(added)** | Every ability is acted, not just switched on, and other players see it too. **Magneto** draws his field out around him with both arms, closes a claw on the metal and wrenches it away, and holds what he lifts with a steady open hand. The **Scarlet Witch** gathers her arms across her chest and throws them open to raise the Chaos Shield, thrusts both hands out for the beam, raises both hands overhead to grow the Hex and moves them apart or together to make it bigger or smaller; her lifting hand is a raised, shaking claw. **Quicksilver** taps his goggles, starts like a sprinter, jabs a flurry of fists in his blitz and darts a hand out to snatch. **Vision** puts his fingers to his temple before the beam, floats his arms wide to let go of the world and braces to become dense. **Tommy** shakes his arms out, spins twice in his blitz, shivers before he goes through a wall and holds out a vibrating palm. **Billy** thrusts his hand at the target of the Hex Bolt, throws both hands up to freeze and presses them to his ears for the Noise. **Loki** flicks his daggers from over the shoulder, raises a spread hand to cast and roars as the Frost Giant; his Scepter jabs instead of chopping. **Thor** hauls back and whips his arm to throw the weapon, holds out his hand for it, is slapped by it as it comes home (with sparks), kicks back as the lightning leaves his hand and holds his arm out along the beam while he fires it, thrusts Mjolnir up as it bursts into fragments and closes his fist as they come together, brings it down on whoever he pins, and roars as the God of Thunder. Your own gesture starts the moment you press the key, without waiting for the server. In first person your hands move too (hands in and up for the shield, a palm thrust out for the bolt, the dagger flicked forward, the weapon whirling at the edge of the screen) and the beam, the tether and the bolt leave the hand where it is drawn. |
| HUD **(added)** | The top-left corner shows your hero, each ability with its key, cooldowns, toggles and resources. |
| Looks **(added)** | Vision and the newer heroes have several looks (variants), each with its own skin, cape and energy colours: White Vision, the TVA Loki, Mighty Thor, the Phoenix, Spider-Man, Thanos, Ultron, Apocalypse... Press **H** to change look (**Sneak + H** goes back). A look changes only how the hero looks and the colour of his energy: the abilities are the same in every look (the one exception is Spider-Man, whose suits also differ in protection, and only the Iron Spider has the legs: section 14). Every look of the newer heroes includes *Your own skin*: the hero's powers in your own skin. |
| Heroes' armour **(added)** | Players with a power are drawn without their armour (helmet, chestplate, leggings and boots, with their trims, dye and glint), in third person, to other players and in the inventory preview: it only hides the characters. It still protects as usual, and a player with no power looks as they always did. The elytra (flight gear), a carved pumpkin or a mob head (a costume the player chose, not armour) and the mod's own hero gear (Magneto's Helmet, Wanda's Crown, Quicksilver's Goggles: part of the costume, and what puts the suit on) are always drawn. The client option *Hide heroes' armour* (on by default) turns it off. |
| MCU colours **(added)** | Every power's light and colour follows the films and shows (section 9). |
| Creative / Spectator **(added)** | Creative and Spectator players are never killed by the Mind Stone or the Hex wall, and the Hex never holds a Creative or Spectator Vision, the same way vanilla treats them as invulnerable. |

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
| 1 | R | **Metal Telekinesis** | Look at a target up to 32 blocks away and press **R** to lift it. While held, it floats where you look. Press **R** again to **launch** it. **Sneak + R** sets it down gently, and **with nothing held, Sneak + R is the Magnetic Pull** (1.3a): everything metal around you flies into your inventory. |
| 2 | G (hold) | **Iron Shrapnel Storm** | Hold **G** to fire a machine-gun stream of iron shrapnel (10 shards/second), paid for with iron from your inventory. |
| 3 | V | **Armor & Weapon Strip** | Look at an enemy and press **V**. Every metal weapon and armor piece is ripped off and drops to the ground. |
| 4 | B (toggle) | **Magnetic Shield** | An invisible magnetic field that repels all metal projectiles. |
| 5 | N (toggle) | **Magnetic Flight** | Smooth, steady flight. |

#### Ability 1 — Metal Telekinesis

* **Valid targets (3.1): everything with metal in it** (one rule, `ability/Metal`, for every power of Magneto): any
  creature or player with iron, gold, copper or netherite **anywhere in its equipment** (armor, either hand, horse armor),
  **Iron Golems**, every minecart, Ultron's Sentries, **Vision and White Vision** (unless phased: then he slips out of the
  grip, `slipped`), **the Ultron hero**, the Iron Spider, **the Vision of a Hex** (only Magneto can lift him), dropped metal
  items, a falling anvil or a block someone threw, and **metal arrows and tridents in flight** (catch them, then throw them
  back). **Never** Mjolnir (it answers only to a worthy hand), the Infinity Gauntlet, Ultron's Core, the heroes' own gear,
  Loki's daggers or a copy of Loki (no real metal), or a phased Vision.
* **Metal blocks (3.1).** Look at a metal block up to `magneto.blockLiftRange` (24) away and press **R** when nothing else
  is in the way: the blocks of `enhanced:magnetic_blocks` (iron, gold, copper and netherite blocks, ores, anvils, rails,
  chains, bars, lanterns, doors, plates, pistons, lightning rods...), up to hardness `blockLiftMaxHardness` (50); rails and
  plates can be aimed at; **a door comes out whole** and lands as a door; a hanging lantern that cannot hang stands; a
  piston is held and set down retracted; gold blocks anger piglins. A block that holds things (hopper, blast furnace, crafter,
  bell) stays: `fixed_metal` says the pull would take it, `fixed_metal_full` that it holds things. Spawn protection, Adventure
  mode and claims are respected, and in a boss arena the block is only borrowed (section 2.3).
* **Iron Golem control:** Magneto can lift any Iron Golem and throw it at enemies.
  A thrown golem hits like a wrecking ball (up to 15 damage to whatever it slams into),
  takes no damage itself, never turns on Magneto, and starts fighting the monster it hit. **(3.1)** A Sentry he throws
  is crushed (grey metal sparks, an anvil's clang), as when Wanda throws one.
* **Launch:** launched targets fly at high speed. When they slam into a wall or the
  floor they take heavy impact damage (up to 16) and hurt any creature they crash into
  (up to 6 damage).
* One target at a time. The hold breaks automatically if the target dies, leaves the
  dimension or gets more than 48 blocks away (or a held Vision phases). A held *player* is released after
  30 seconds **(added — so nobody is trapped forever)**. **(3.1) With PvP off (or a teammate), a player or someone's
  pet that any telekinesis lets go of or throws takes no fall, wall or slam damage until it lands**
  (`telekinesisSparesFriends`), and the same holder cannot grip the same player again for 3 seconds
  (`telekinesisPlayerRegripTicks`); for a second after any grip ends, Sneak + the key with nothing held does nothing
  (`telekinesisSneakGraceTicks`).
* **Bosses (3.1).** Ultron, Thanos and Apocalypse are held as bosses, **1.5 times as long as Wanda** (`boss.magnetoHoldFactor`):
  4.5 s for an Ultron body (vibranium is metal), 3 s for Thanos, 4.5 s for Apocalypse in a window until his true form,
  never through his shield; then 12 s before anyone can hold him again. Thanos's sword in flight is caught and tears free
  after 3 s. The Wither, the Warden, the Elder Guardian and the Ender Dragon are not metal.

#### 1.3a The Magnetic Pull (Sneak + R holding nothing) **(added in 3.1)**

**Sneak + R when R holds nothing** (whatever is in his hand) is a pull; it never grabs. Nothing happens within
`telekinesisSneakGraceTicks` (20) of a grip ending, while he rides, for a Spectator, or while the pull recharges
(`pull.recharging`) or he is too hungry (`pull.too_hungry`). One pull a time, sliced over the server's ticks.

* **Reach.** A sphere of `pullRadius` (8) round where he pressed. **Natural metal** (`enhanced:magnetic_ores` and raw-metal blocks)
  comes **through rock**; **built metal** (every other block of `magnetic_blocks`) only if he **sees** it
  (`pullBuiltNeedsSight`) **and** it lies within the **cone he faces**, `pullBuiltCone` (120 degrees; 360 = all round), so his own
  base behind him stays. Only loaded chunks, never the block under his feet. Never a cell that is protected by
  `BlockRights.massEditRefuses` (spawn, claims, the border, Adventure, `build_magic.allArenasProtected`: every running boss
  arena, Thanos's included, the island of Sokovia, the ground it hides, a Hex that is not his when `build_magic.hexIsAClaim`),
  never a block a villager has claimed (a blast furnace, smithing table, stonecutter or bell in use), never a block or minecart that **holds
  items** (an empty one comes), never an extended or powered piston.
* **What each thing gives.** An **ore** turns into its ground (stone, deepslate, netherrack, blackstone) and gives its metal:
  iron ore 1 raw iron, gold ore 1 raw gold, copper ore 2 to 5 raw copper, nether gold ore 2 to 6 gold nuggets, gilded blackstone 2 to 5 gold
  nuggets, ancient debris 1 netherite scrap (`pullAncientDebris`; it becomes netherrack). A **built block** comes whole,
  as Silk Touch mining gives it (a door as one door; a hopper spills what it held, which is why it comes only when empty).
  **Loose metal** (items he has room for, stuck arrows and tridents, falling blocks, empty minecarts) comes from all round.
  **Hostile creatures** lose every metal piece of gear (`pullTakesGear`; a boss's strip hook answers first; a Horseman's crumbles; a
  Loki copy dissolves). **A player, with PvP on and only one he can see**: one piece into **his inventory**
  (`pullPlayerGearToFeet` drops it at the player's feet instead), or an Iron Spider's nanites scatter, or a metal body is
  tugged; then `pullPlayerGraceTicks` (200) of grace. **Metal bodies** (Iron Golem, Vision, the Ultron hero, the Vision of a Hex; never a
  fight's Sentries, a creature another player owns, or a participant of a fight) are **tugged** toward him, at most `pullTug`
  (1) blocks a tick, unhurt.
* **Storage.** What is inside chests, barrels, shulker boxes, item frames and armor stands is never taken, unless
  `pullFromStorage` (default false): then the **metal stacks** of what he sees come (claims asked per block, never a
  villager's block, pets never), and the container, frame or stand stays.
* **Into the inventory.** Each thing goes straight into his inventory; what does not fit drops at his feet (`pull.full`).
  Taking is sliced: the 64 nearest things (`pullMaxTargets`), 2 a tick (`pullPerTick`), 8,192 cells searched a tick
  (`pullScanPerTick`); each is checked again when its turn comes. The first thing affected starts the **cooldown**
  `pullCooldown` (100 ticks, 5 s) and the cost `pullExhaustion` (4.0 = one hunger point); a pull that finds nothing costs
  nothing (`pullRetryTicks` 10). It ends at once if he dies, logs out, changes dimension, loses the power, becomes a Spectator
  or moves more than the radius from where he pressed.
* **Seen and heard.** The gesture (arms wide, fists to the chest), a colourless ring growing to the radius, every thing
  flying to his hand along a line of force as its item (8 to 26 ticks), `magnet_pull` at the start, `CHAIN_BREAK`,
  soft pickup clinks, `CHAIN_PLACE` at the end; the HUD shows `pull 4.9s` on R's line while it recharges.

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

* Works on zombies, skeletons, piglins and every other creature, up to 32 blocks. **(3.1) On players only with PvP on**
  (never a teammate or a Creative player: `disarm.no_pvp`, `disarm.not_allowed`).
* Rips off every metal item in a hand or armor slot (iron, chainmail, gold, **copper and netherite** armor, metal weapons
  and tools, shields, crossbows, tridents, Stormbreaker, shears, flint & steel, metal horse armor). The items
  fly out and land on the ground as normal items (a player's piece drops at his feet).
* **(3.1)** If the metal is the body itself (a golem, a Sentry, Vision, Ultron): `disarm.metal_body`, "lift it with R", no
  cooldown. A copy of Loki **dissolves** (`disarm.illusion`). Gear a creature never drops (the Horsemen's iron) **crumbles**.
  On Thanos it wrenches off **one plate** (once in 6 s, shared with slams; never while he arrives or dies).
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
| 3 | V | **Telekinesis** | Lift creatures, dropped items **or blocks**, move them with your gaze, then launch them. **(3.1)** Sneak + V with nothing held tears out the block you look at; the three villains and the Wither, the Warden and the Elder Guardian are held as bosses. |
| (3.1) | Y | **Build Magic** | Tap: build mode on or off; hold: the Spell Wheel. Her magic has no limit: section 18. |
| 4 | B (hold) | **Chaos Shield** | A red glowing sphere that stops projectiles and melee blows and softens explosions. It takes time to form; since 3.2 it is **unlimited** (ability 4). |
| 5 | N | **The Hex** | A giant red energy dome that only Wanda controls. |

#### Ability 1 — Chaos Energy Beam

* **Look:** a bright, shimmering crimson beam with hot pink wisps, a near-white core,
  a crackling outer glow and sparks where it hits. It's a real rendered beam, not ordinary
  redstone particles. **(added)** While she wields chaos magic (beam, shield, telekinesis)
  her eyes glow red, as in the WandaVision finale. The glow sits exactly on the eyes of the
  skin that is drawn: on the suit, whose eyes are two rows lower than a usual Minecraft
  skin's, and on her own skin, where they are on the usual row.
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
  **(3.1) Ultron, Thanos and Apocalypse, and the Wither, the Warden and the Elder Guardian, are held as
  bosses**: for 3 seconds, then they break free, and nobody can take hold of them again for 12 seconds (see each boss's
  section); the Ender Dragon is too vast to hold (`telekinesis.dragon`). Apocalypse's shield turns every power but mind
  power away, hers included: she holds him through it. Every form works the same way (3.1: Wanda, Billy, the Phoenix and Magneto),
  and a boss's shots (Thanos's sword, meteors and orbs; Ultron's bolts; Apocalypse's lances and boulders) are caught by whoever
  catches projectiles, and are the thrower's when thrown back.
* **Control:** while held, the target floats where Wanda looks. Press **V** again to
  **launch** it at high speed. When it crashes into a wall or the floor it takes heavy
  impact damage, and it hurts any creature it slams into.
* **Blocks:** when there is nothing to take, **V** tears out the block you look at (up to 24 blocks away);
  **(3.1) Sneak + V with nothing held tears it out even past a creature, an item or a boss** (never in the second after a
  grip ended: `telekinesisSneakGraceTicks`). Throw it at enemies like a shell (8+ damage). It shatters on impact and
  drops its normal item.
  * Liftable: any block with hardness 5 or less (dirt, stone, logs, ores, iron blocks…)
    that has no inventory or other block data and isn't on the
    `enhanced:telekinesis_immovable` tag. Bedrock, obsidian, portals, chests and
    spawners can't be lifted.
  * Lifting respects spawn protection, Adventure mode and claim mods.
* **Sneak + V** sets the target down gently. A held block placed this way **(3.1) becomes a block again where it lands**,
  under the placing rules (room, arenas, claims, spawn protection, never for a holder who left the game, never where someone
  stands); refused, it drops its loot, or itself when its loot is empty (glass, ice).
* **Arena blocks are borrowed (3.1).** In a protected arena (Ultron's and Apocalypse's) a block lifted by any telekinesis is
  *borrowed*: its hole mends after 5 s (`arenaMendTicks` 100), a set-down borrowed block crumbles after 5 s
  (`borrowedBlockTicks` 100; at most `maxStandingPerPlayer` 3 stand, never on one another or next to anyone), it never drops
  anything, a thrown one turns to dust, a fight holds at most `maxBorrowedBlocks` (48), and everything goes back when the fight ends; a
  journal written off the server thread restores the ground after a hard crash, and forgets a repair only two world saves
  later. Sand hanging over a gap, a block with a painting, frame or lead on it, and the ground under the island are not
  lent. The island in the sky is protected whatever `boss.protectArena` says. Thanos's arena is not protected: real blocks.
* **Bosses in the grip (3.1).** A held boss does nothing, cannot be carried out of his arena, is never held in his
  invulnerable states (Thanos arriving or dying, Ultron shielded or changing bodies, the lift's lock, Apocalypse's shield forming), breaks
  free after `holdTicks` (60 / 40 / 60, the vanilla bosses `boss.vanillaHoldTicks` 60) times the form's factor (Billy 0.5,
  Magneto 1.5 on metal), then nobody can hold him again for `boss.regripTicks` (240) counted from when he can act; a thrown
  boss does nothing for at most `thrownStillTicks` (20), takes the impact through his own caps and crashes where he comes down.
  A vanilla boss is held with his AI off (never saved that way).
* **Critical against Apocalypse (3.1).** Her mind power hits the boss critically: section 17.3.

#### Ability 4 — Chaos Shield

* Hold **B**. A red, glowing sphere of chaos energy grows round Wanda. **(added)** It is a
  commitment, not a reflex:
  * **It takes 3/4 of a second (15 ticks) to form.** It grows from her hands to its full
    size and **blocks nothing until it is complete.**
    * **(3.2) It is unlimited** (`scarlet_witch.shieldUnlimited`, default true). It costs no hunger, has no strength to wear down, no time limit, never shatters and needs no recharge after it drops: `WandaAbilities.wear` returns the whole amount and changes nothing, and she can hold B as long as she likes and raise it again at once. The HUD shows
    "∞ unlimited" (`hud.enhanced.shield_unlimited`) where it showed "30/30 8.0s"; the sphere keeps its shape on the client (no guttering). It still takes `chaosShieldWindUpTicks` (15) to form and stops exactly what it stopped before (everything below). The boss rules are as before: the Mind Crush still passes, the Entomb skips a complete shield, anything that `BYPASSES_INVULNERABILITY` still passes. `scarlet_witch.shieldUnlimitedVsBosses` (default true) keeps it unlimited against bosses too; with false, damage that `WandaAbilities.fromBoss(cause)` calls a boss's (an entity of the tag `enhanced:bosses`, a fight entity or member, a projectile whose owner is one) wears it by the old strength rule (30 points, shatters, `chaosShieldBrokenCooldown`) even while `shieldUnlimited` is true, and ordinary mobs and players still cannot (`WandaAbilities.wear(player, amount, cause)`; the two-argument overload means "cause unknown"). The time limit and the hunger stay off.
  * **The old rules** (`shieldUnlimited` false): it holds for at most 8 seconds (`chaosShieldMaxTicks` 160, counted from when it is complete), has 30 points of strength (`chaosShieldStrength`) that everything it stops wears down (see below; at 0 it **shatters**: a burst of red shards and a glassy sound), costs hunger (`chaosShieldExhaustion` 0.1 a tick) and has to recharge: **6 seconds** after it drops
    (`chaosShieldCooldown` 120), **15 seconds** if it shattered or was held to its limit (`chaosShieldBrokenCooldown` 300); the HUD counts it down and B does nothing meanwhile. Every "pays" and "costs" below is about these rules. `TestKit.fixture()` switches the option off for every test, so the 52 older shield tests test the limits; `AbwShieldTests` (5, batches `abw_shield_*`) cover both modes.
* **Blocks every projectile** it meets while it is up: arrows, tridents, Ghast and Blaze
  fireballs, Wither skulls, shulker bullets, potions and snowballs. Projectiles disintegrate
  on the sphere. A projectile costs the shield what it would have done to Wanda: an arrow its
  damage at its speed (about 4 to 6), a trident 8, a fireball 6, a thrown Mjolnir,
  Stormbreaker, dagger or iron shard its own damage, and 2 for anything else. The projectile
  that breaks the shield is still stopped. A thrown Mjolnir or Stormbreaker is one of a kind,
  so it is not destroyed: the shield turns it round and it flies home to Thor. So does a
  fragment of a shattered Mjolnir that is sent at her (the shield pays what its strike would
  have done, 6), while one that is already flying home passes through.
* **Stops Thor's lightning beam.** The sphere is solid to a beam as it is to an arrow: the beam
  ends on its surface (that is where it is drawn to), Wanda is not touched, nothing beyond the
  sphere is hit (and no lightning arcs on from it), and the shield pays the beam's damage for
  every hit: 2 bare-handed, 4 with Mjolnir or Stormbreaker, 6 with the God of Thunder on top.
  The beam that breaks the shield is stopped too. Lightning that Thor sends at Wanda from
  inside the sphere, or that leaps from a blow of his, is stopped and paid for in the same way.
  A shield that is still forming stops nothing, and on a server with PvP off the beam costs
  the shield nothing. (Lightning from the sky, by contrast, still hurts.)
* **Stops melee blows too.** Close in is no way round it. A creature's or a player's own
  blow (a swung sword or a fist, a zombie's or a Warden's swipe, a spider's bite, a Ravager's
  charge, a bee's sting, Quicksilver's blitz) never touches Wanda: the sphere takes it like an
  arrow. It costs the shield what it would have done to her (the blow that breaks the shield
  is stopped too), red sparks fly where it struck, the sphere rings, and the attacker is
  thrown back out of the dome (a creature that can't be knocked back, like the Warden, stays,
  and whoever Mjolnir pins is held where it is). A shield that is still forming stops nothing.
  Thor's blows are stopped like anyone's and the God of Thunder's lightning does not leap from
  them. With PvP off a player's blow can't hurt her anyway and costs the shield nothing. Her
  own blows and her beam work out of the dome. What is not a blow still gets through:
  falling, hunger, fire, lava, drowning, the void, `/kill`, magic and potions, a Guardian's
  beam, thorns and a Warden's sonic boom.
* **Explosion damage is reduced by 75%.** The 75% the shield takes away is what wears it
  down; if less strength is left than that, the shield shatters and the rest gets through.
* **Cost:** none while it is unlimited. With the old rules it burns hunger quickly while held (about 1 food point every 2 seconds), while it forms too. When the hunger bar is empty the shield collapses (and has to recharge).
* **HUD:** "forming" while it grows; once it is up, "∞ unlimited" (with the old rules: what is left of its strength, green, then yellow, then red, and the seconds it can still be held). The sphere flickers as it weakens
  or when its time is nearly up. Her arms gather across her chest as it forms and are thrown
  open at the very moment it is complete (also with another wind-up in the config: the arms
  keep time with it); let go of before it forms, she only lowers them.

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
* **(added)** **Vision can't leave a standing Hex.** Wanda is in control of it, and in *WandaVision* Vision
  starts to come apart at the edge of Westview. A player with Vision's power (White Vision too) who is
  inside a Hex can't leave it, whether or not Wanda allowed him in. Walking, flying, phasing, portals and
  every teleport out (Ender Pearls, Chorus Fruit, the Bifrost, Loki's Tesseract, the Witches' Road,
  Tommy's Vibrate) are refused at the wall. Where he meets it he starts to come apart: pieces of him flake
  off in orange and yellow with a crackling sound, and he is thrown back inside and told "Wanda's Hex
  holds you: only she can let you go". It hurts him a little: 1 heart the first time, and 1 heart more for
  every push within 3 seconds of the one before, up to 4 hearts (`hexHoldDamage`, `hexHoldDamageMax`); however
  hard he leans on the wall it bites at most twice a second. It never kills him (the last half heart is
  never taken), and it starts over after 3 seconds without pushing. White Vision is held the same way but
  doesn't come apart: he is only thrown back, like a hero walking into glass, and is not hurt. **He can
  escape by hurting Wanda**: at or below 30% of her health (`hexHoldReleaseHealth`) her Hex can no longer
  hold him and he walks out, allowed in or not, and her death dissolves the Hex. He is also free when the
  Hex dissolves, when Wanda shrinks it past him (the wall then runs inside him, and he is not dragged
  back), when Wanda is gone from the world, and if he is in Creative or Spectator. Getting **into** a Hex
  keeps the old rules, for both looks: Wanda's permission lets him in, and without it he is thrown back
  like any hero.
* **(added)** **The Vision of the Hex** ("when Wanda raises the Hex, Vision appears and attacks whoever she
  tells him to"):
  * When Wanda raises a Hex, a Vision appears beside her. He is a creature of his own, drawn like Vision:
    the player model with Vision's skin and yellow cape and the Mind Stone glowing in his forehead
    (brighter while he fires the beam). He floats and flies, with poses of his own for hovering, flying
    (leaning into it, arms swept back, cape streaming), standing guard and punching. He belongs to the
    Hex: he follows Wanda inside it, can never leave it (the wall holds him exactly as it holds Vision,
    and a Vision of another Hex is thrown back at this one) and is there as long as it stands. One per
    Hex.
  * He protects her like a tamed wolf. He goes for whatever attacks her, whatever she attacks and whatever
    hits him, and for whatever she points him at. Up close he fights with dense punches (12 damage, about
    one a second) and from a way off with a short Mind Stone beam (4 damage a hit, two hits a second, 16
    blocks). He has 80 health (an Iron Golem has 100) and 8 armor. All of it is in the `hex_vision`
    section of the config, which can also turn him off.
  * He never harms Wanda, players she allowed into the Hex, the pets of either, other Visions, or Creative
    and Spectator players, and no player at all on a server with PvP off. Wanda's Chaos Beam and blows pass
    him by, and no telekinesis lifts him, except Magneto's: he is vibranium. A Hex still holds him in, and with PvP off no throw or fall hurts him.
  * **Orders.** The **Z** key (*Vision, That One!*, rebindable under *Options → Controls → Enhanced
    Powers*): look at a creature and press it, and he goes for that one, whatever else he is doing;
    look at nothing and press it, and he comes back to her side; **Sneak + Z** makes him hold his post or
    follow her again. The Hex Control screen has the last as a button, *Vision: follows you / holds his
    post*, and the Hex remembers it.
  * If he falls he fades to nothing, and Wanda rebuilds him 60 seconds later (`hex_vision.rebuildTicks`)
    as long as the Hex stands. He fades away when the Hex dissolves, and when Wanda leaves (logs out or
    changes dimension; her Hex raises him again when she is back). Nothing of him is saved.

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

**3.3:** Quicksilver Time (R) and Super Speed (G) are separate powers, used alone or together; he runs at super speed only with Super Speed on, and runs up walls and along ceilings. See 31. The text below is the 3.2 design; where they differ, 31 is the rule.

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
* **Max health 30** (15 hearts) and **8 armor** from his vibranium body. **(3.1)** Vibranium is metal: Magneto can lift and throw him, unless he is phased: then he slips out of the grip.
* He never gets hungry and never runs out of air.
* **(added)** Inside a standing Hex he can't leave it, in either look, whether or not Wanda let him in:
  he comes apart at its wall and is thrown back (White Vision is only thrown back), until he hurts Wanda
  badly or defeats her (section 2, ability 5). Wanda's Hex raises a Vision of
  its own to fight at her side (section 2).

### 4.3 Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (hold) | **Mind Stone Beam** | A pale gold beam from the stone in his forehead, 32 blocks long: 6 damage per hit (two hits per second). Hits creatures, players, the Ender Dragon and End Crystals. |
| 2 | G (toggle) | **Flight** | Survival flight at 0.08, calm and composed. |
| 3 | V (toggle) | **Phase** | Become intangible: walk and float **through walls, floors and creatures**. Nothing can hurt you and projectiles pass through you, but you can't attack or fire the beam. You float instead of falling. You can't turn solid again inside a block (if you're only a little sunk into the floor, you rise out of it). Phasing doesn't get you through Wanda's Hex wall: it is a wall of reality, not of stone, and inside a standing Hex it doesn't get you out either. |
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

**3.3:** Tommy's keys changed (R Slow Time, G Super Speed, Sneak keys for Wall Run, Dash Strike, Sonic Boom and Disarm Blitz): see 31.

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
  spiral stair among glowing roots, and the Road asks: **"Seekest thou the Road?"** **(3.2)** The
  Road no longer asks for the choice at once: the screen offers one button, **Walk the Road**, and the player walks the three trials of section 19; only at the end does he choose **Billy: Wiccan** or **Tommy: Speed** (section 5), and only then is one gene used up. The door closes again five seconds after it opens. With `witches_road.enabled` false the door is the 3.1 door: the instant choice, and anyone else holding a gene can join the ritual and choose too.
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
| 1 | R (hold) | **Hex Beam** | **(3.2)** A continuous beam of blue magic from his hand along his look, to be aimed: 6 damage a second and 20 blocks at rank 1, up to 12 and 40 at the top rank, four hits a second, 0.8 exhaustion a second. Section 6.4. With `billy.beam` false it is the 3.1 **Hex Bolt**: a crackling blue arc to the first creature he looks at within 24 blocks, 6 damage, forking on to the two nearest creatures within 5 blocks of it (3 damage each), cooldown half a second. |
| 2 | G (toggle) | **Flight** | Calm, weightless flight with open hands, like his mother's (speed 0.08). |
| 3 | V | **Telekinesis** | Wanda's telekinesis, in his blue: creatures, dropped items, minecarts, boats, primed TNT, falling blocks, **and projectiles in flight**: an arrow he catches hangs in front of him, pointing where he looks, and flies off at his command. **(3.1) He tears blocks out of the ground too**: within 16 blocks, hardness 3 at most (stone, ores, planks; not iron blocks), one block a second (`billy.blockLifting`, `blockLiftRange`, `blockLiftMaxHardness`, `blockLiftCooldown`); **Sneak + V** with nothing held takes the block he looks at, even past a creature. He holds the villains and the vanilla bosses as bosses, half as long as Wanda (`boss.billyHoldFactor`); Apocalypse only in a window. Press again to launch, **Sneak** + the key to put down gently (a block becomes a block again where it lands). |
| (3.1) | Y | **Build Magic** | Tap: build mode on or off; hold: the Spell Wheel. An apprentice who learns: a magic meter, ten ranks from Apprentice to Chaos Master (3.2; four in 3.1), half the cost near a Scarlet Witch: section 18. |
| 4 | B | **Freeze** | Every creature and player within 12 blocks in a 90° cone in front of him, in his line of sight, is **spellbound** for 5 seconds: they can't move, jump or attack, can't pick a new target, and blue sparks circle their heads. The Ender Dragon and the Wither are too strong for it. Cooldown 12 seconds. **(3.2) Sneak + B, held, raises the Hex Barrier**: a directional plate of blue magic that stops what comes from the front, for a limited strength and time (section 6.5). |
| 5 | N (toggle) | **The Noise** | He hears every mind within 32 blocks: every creature glows through walls, **red** if it is hostile and about to attack, **blue** otherwise. Whispers crowd his head, louder the more minds are near. |

**The crown.** While he flies, and for three seconds after each spell, a glowing chrome crown
forms on his head, in shades from ice-white to deep blue (the *Wiccan* look already has it).

### 6.3 Looks

*Wiccan* (*Agatha All Along*), *Teen* (on the Witches' Road), *Halloween* (his *WandaVision*
costume), *Wiccan (comics)*, and *Your own skin*.

### 6.4 The Hex Beam (R, held) **(added in 3.2)**

Eilon asked for "a continuous laser beam instead of a single shot, still requiring the player to aim". **R is held**: a beam leaves his hand along his look (`ContinuousBeam`, section 21; style `billy_hex`: blue `0x5CC8FF`, a white-hot core, a dark blue fringe, strands and forks, flares at both ends) and hurts **what the crosshair is on**: the first creature, dragon part or
End crystal the line meets within range (a block stops it).

| Number | Option | At rank 1 | At the top rank |
|---|---|---|---|
| Damage a second | `beamDamagePerSecond` 6.0 × (1 + `beamRankDamageBonus` 1.0 × *p*) | 6.0 (1.5 a hit) | 12 (3.0 a hit) |
| Range | `beamRange` 20 × (1 + `beamRankRangeBonus` 1.0 × *p*) | 20 blocks | 40 blocks |
| Hits | `beamInterval` 5 ticks (four a second); per hit = damage a second × interval ÷ 20 | | |
| Hunger | `beamExhaustionPerSecond` 0.8 (a food point every 5 s) | | |

The rank progress *p* = (rank − 1) ÷ (`maxRank` − 1), read only through `BillyRanks.rank(BuildData)` and `BillyRanks.maxRank()` (0 when `maxRank` is 1). The damage type is `enhanced:hex_beam`, in the tags `minecraft:bypasses_cooldown`, `no_knockback` and `panic_causes`, so every hit lands (as Thor's beam); death messages in both languages.
A complete Chaos Shield stops the beam at its surface and pays strength as it does Thor's beam (with PvP rule `canHarmPlayer`); PvP and Creative as `hurt` does; bosses take it through their normal damage rules (**Apocalypse's shield takes nothing from it** and wears none). It stops on release, on starvation ("Too hungry to fire the Hex Beam", `message.enhanced.billy_beam.hungry`),
as a Spectator, on logout and on losing the power. While the Darkhold's beam is the one burning, his own does not run (20.3). The arm stays out along the look (`Stance.BEAM_HAND`); the start gesture is `BILLY_BOLT`; the loop is the Chaos Beam sound at pitch 1.3; the HUD line is `[R] Hex Beam  6.0 dmg/s, 20 blocks` (green `firing: ...` while held). The ability name is "Hex Beam" / "קרן כישוף".
**The fallback:** `billy.beam` false brings back the 3.1 Hex Bolt (6 damage, forking to the two nearest creatures within 5 blocks for half, cooldown half a second, no hunger). Differences from the bolt, on purpose: it costs hunger (the lead asked for "like the bolt"; the bolt cost none), the forks are dropped for the beam (the visual branches stay), there is no overheat, and per-tick damage is
per 5 ticks because creatures' damage cooldown is bypassed by the damage type. A payload (`BeamPayload`) goes out every 8 ticks while held. **(Review fixes)** With `billy.beam` false the ability's name is `ability.enhanced.billy.1.bolt` ("Hex Bolt" / "ברק כישוף") and `GestureRoutes.acting(BILLY, 1)` is "cooldown" again (the bolt's predicted gesture is back); the cooldown that starts when the Hex Barrier comes down does not play the Freeze gesture (`GestureTable.edges`). Tests: `abw_beam_*`.

### 6.5 The Hex Barrier (Sneak + B, held) **(added in 3.2)**

**3.3 changed it** (23.2): it stops everything from the front, has **health** (`barrierHealth` 60 since 3.3.1) instead of strength and time, no time limit and no hunger, mends while lowered, and only chaos and dark magic wear it down (3.3.1). The text below is the 3.2 design; where they differ, 23.2 is the rule.

**Sneak + B held** raises a **directional plate** of blue magic (plain B is still Freeze; no new key mapping, so the Controls screen is unchanged; the key is read on the server and the barrier stays up until B is released, even if Sneak is let go). B's recharge is shared with Freeze (cooldown slot 4: 240 ticks after Freeze, `barrierCooldown` 200 after the barrier is lowered or times out,
`barrierBrokenCooldown` 400 after it breaks; none if B is released before it formed).

* **Shape.** A tall translucent plate `barrierWidth` 1.4 × `barrierHeight` 2.4, held `barrierDistance` 1.6 in front of his body, slightly curved, following his look (pitch too), drawn a little to the off-arm side. It forms in `barrierWindUpTicks` 8 and stops nothing until it is complete. He walks at `barrierSpeedFactor` 0.6 (a movement-speed modifier, removed when it comes down) and it costs `barrierExhaustionPerSecond` 0.4 (not on an empty stomach: "Too hungry to raise the Hex Barrier").
* **Direction.** It covers a cone of `barrierArc` 60° either side of his look, measured from his body to the attacker, projectile or explosion source; blocks outside the cone pass whole, and an explosion with no source entity has no direction and passes whole.
* **It stops** (the same predicates as the Chaos Shield): projectiles (the price is `WandaAbilities.projectilePower`; they burst into blue sparks; also scanned where they cross the plate's plane), the blows of creatures and players (the attacker is thrown back 0.6), Thor's lightning, and explosions (`barrierExplosionFactor` 0.5 still gets through). It stops nothing that `BYPASSES_INVULNERABILITY`, nothing the Chaos Shield does not stop (the Mind Crush; the Entomb stays possible: `SandControl.entombRefusal` checks only a complete Chaos Shield).
* **Limits (unlike Wanda's shield).** Strength `barrierStrength` 20 × (1 + `barrierRankStrengthBonus` 2.0 × *p*) = 20 to 60; time `barrierDuration` 100 ticks × (1 + `barrierRankDurationBonus` 1.0 × *p*) = 100 to 200 (5 to 10 s). Everything it stops wears it down; at 0 it breaks ("The Hex Barrier broke!").
* **State** reuses the synced Chaos Shield fields of `PowerData` (`chaosShieldActive`, formed-at, strength), because Billy has no other use of them; everyone sees it from those. **HUD:** `[B] Freeze (Sneak: Hex Barrier)`, "forming", `20/20 3.1s`, the recharge.
* **Look.** Two generated textures (`billy_barrier.png` 128 × 224: engraved nested contours with a wavy top and rounded feet; `billy_seal.png` 128 × 128: a ring of 36 triangular teeth, a dotted ring, a hexagram, a diamond, two arrows), additive blue and white glow on a curved 14 × 20 grid with a translucent veil under it; the seal turns slowly and flickers; a hit flashes, runs a ring across the plate and crackles blue lightning; white cracks grow as strength drops and it gutters below 30%; the first-person plate is fainter (40%).
  Animation: `Stance.BARRIER_BILLY` (the off forearm raised forward across the chest, the main arm braced back, the body turned 8°, feet apart; first person: the left arm raised), eased in 3 ticks. Sounds are reused (beacon select and deactivate, amethyst, shield block, glass break).
  Messages: "The Hex Barrier needs N more seconds", "Too hungry to raise the Hex Barrier", "The Hex Barrier broke!". Tests: `abw_barrier_*` (polling one schedule entry at a time).


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
| 2 | G | **Duplicates** | *"Surprise!"* He turns invisible for 5 seconds (his held items and name are hidden too) and **three** illusory copies of him, looking exactly like him and holding what he holds, walk off in different directions, 12 blocks each. Every creature within 24 blocks that was after him goes after the nearest copy instead. A copy dissolves in one hit into warm-white streaks of light with a flash of his green, deals no damage, drops nothing and fades after 8 seconds anyway; **(3.1) it has no real metal, so Magneto's strip or pull dissolves it and Tommy's Disarm Blitz takes nothing from it** (`enhanced:conjured_bodies`); they all dissolve if he loses his power. Drinking milk brings him back into view early. Cooldown 15 seconds. |
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
  can't destroy it. Speedsters can't snatch it and telekinesis can't lift it, not even Magneto's. **(3.1)** Stormbreaker, by contrast, is metal: Magneto can lift it, tear it from a hand and pull it.
* **Shattering Mjolnir** (*Thor: Love and Thunder*, where Jane's Mjolnir bursts into dozens of glowing
  fragments that each strike an enemy, then fly back and reassemble). **Sneak + R** with Mjolnir in
  Thor's hand (with Stormbreaker, Sneak + R just throws it as R does): it bursts into glowing,
  crackling fragments, one for each enemy he can see within 16 blocks: hostile monsters, and any
  creature or player that is going after him or has just hurt him (the nearest 12), or a ring of
  twelve if nobody is around. Players PvP rules protect, and players in Creative or Spectator mode,
  are never targets. Each fragment curves in on its enemy, strikes it (6 damage and knockback; with
  the God of Thunder, lightning arcs from it too) and flies back, and when the last one is home Mjolnir
  is whole again in his hand (in his inventory if his hand is busy), still bound to its owner and
  with everything else it carried. While it is in pieces it counts as a weapon in flight, so he keeps
  flying, and **R** calls the fragments home at once. If Thor dies, logs out, changes dimension or
  loses his power meanwhile, it comes together where he was and falls to the ground, its owner
  kept. (While it is in pieces the hammer is kept in Thor's own saved data, so it can't be lost or
  copied, even if the world closes.) However it goes, the pieces are home and Mjolnir is whole again
  after 10 seconds at the latest. Cooldown 15 seconds.
* **Pinning someone down** (*The Avengers*, where Thor sets Mjolnir on Loki's chest and Loki can't get
  up). Its owner (or any worthy player, with `mjolnirOwnerOnly` off) **Sneak + right-clicks** a
  creature or player with Mjolnir in hand: Mjolnir is set down on them, lying across their chest.
  While it lies there they **can't move, jump, fly, swim up, ride, teleport** (Ender Pearls,
  chorus fruit, Endermen, and the mod's own teleports and dashes: the Bifrost, Vanish, Vibrate,
  the Blitz, the Witches' Road, the Tesseract) **or glide on an elytra**, and knockback doesn't
  move them and no telekinesis can lift them; they can still look around. **Only its owner can lift
  it again**: right-click the pinned one, or call it back with **R**. Nobody else can, the pinned one
  included (*"It won't budge. Only Steve can lift it."*). It lets go by itself after **60 seconds**
  (config), or when its owner logs out, dies or changes dimension, or when the pinned one dies or
  leaves: then it drops beside them, its owner kept. The bosses (the Ender Dragon, the Wither and
  the Warden: the `enhanced:pin_immune` tag) can't be pinned, and neither can a player if the server
  has PvP off (or one in Creative or Spectator mode).
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
  **the Bifrost** (8.4). In Thor's hands it does most of what Mjolnir does: he throws it, calls it
  back, flies with it and fires his lightning beam through it (it doesn't shatter, and it can't pin
  anyone). Unlike Mjolnir, a speedster can snatch it out of a hand.

  Both weapons are drawn big, as in the films: Mjolnir's head is about as long as Thor's head is
  wide, and Stormbreaker is a big axe with a long handle, the bigger of the two. They keep that size
  wherever they are seen: in Thor's fist (the handle passes through it at rest, thrust ahead in flight
  and while the weapon whirls on its strap), in first person, thrown, set down on the ground, lying on
  someone it pins, and as a dropped item (the inventory icon is as it was).

* **Mjolnir or Stormbreaker?** In the films the axe hit a bit harder than the hammer, and so it does
  here: **Stormbreaker is a fifth stronger than Mjolnir in everything they share**. What only
  Mjolnir can do stays Mjolnir's.

  | | Mjolnir | Stormbreaker |
  |---|---|---|
  | Who can wield it | only the worthy, and only its owner | anyone |
  | A blow in the hand | 10 damage (5 hearts) | **12** damage (6 hearts) |
  | Thrown (both ways) | 10 damage | **12** damage |
  | Lightning beam, each hit | 4 damage (2 hearts) | **4.8** damage |
  | With the God of Thunder | 6 damage | **7.2** damage |
  | Lightning beam, how far | 48 blocks | **58** blocks |
  | The Chaos Shield pays for the beam | the beam's damage | the beam's damage (a fifth more) |
  | Shatter into fragments (Sneak + R) | yes | no |
  | Pin someone down (Sneak + right click) | yes | no |
  | Throw, call back, flight, the Bifrost | yes | yes (the same speed, range and cooldown) |
  | A speedster can snatch it | no | yes |
  | Its size in the hand | the head as long as Thor's head is wide | a big axe, taller than the hammer |

  Every number above is a server option (`thor.*`, section 11).

### 8.2 The Asgardian body

* **Max health 30** (15 hearts), **6 armor**, half knockback.
* Lightning never hurts or burns Thor. With Mjolnir or Stormbreaker in hand he takes no fall damage.

### 8.3 Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R | **Throw / Call Back** (Sneak: shatter Mjolnir) | With Mjolnir or Stormbreaker in hand: throw it. It flies straight, tumbling, smashing everything in its path (Mjolnir 10 damage, Stormbreaker 12, and heavy knockback), until it hits a wall or has flown 40 blocks, then turns back and flies home to his hand, straight through walls, hitting everything on the way back too. With neither in hand: **call the nearest one back**, from his inventory, the air, the ground anywhere within 64 blocks (dropped, or Mjolnir resting as a block), or the hands of a creature that picked it up (never from another player, and never another player's Mjolnir: he calls his own, one nobody owns yet, or Stormbreaker), or off someone it pins down (never off himself). **Sneak + R** with Mjolnir in hand: it shatters into fragments that strike his enemies (see 8.1). |
| 2 | G (toggle) | **Flight** | Thor flies by spinning his weapon: only with Mjolnir or Stormbreaker in hand (or while it flies back to him). Fast: 0.11. Hovering, he holds it up to the sky; at speed it leads the way. |
| 3 | V (hold) | **Lightning Beam** | While he holds **V**, a crackling beam of lightning pours from his hand, or from the head of the weapon in it (the one in his main hand, if he holds two), to what he looks at, for everyone nearby to see: a white-hot core, electric blue and a deep blue halo, jagged and flickering. Every 5 ticks it hits the first creature in the line (not through walls): bare-handed, 2 damage (one heart) up to 24 blocks away (8 damage a second). With **Mjolnir** in hand it reaches 48 blocks and hits twice as hard (4 damage); with **Stormbreaker**, a fifth more of both (58 blocks, 4.8 damage). Through either the beam is thicker and brighter, and the first time it hits each creature it also has the usual effects of lightning on it (creepers become charged, pigs turn into zombified piglins, villagers into witches). The **God of Thunder** on top hits 1.5 times as hard, and every hit arcs to the next two creatures nearby. The beam leaves no invulnerability frames, so every hit lands. It burns a little hunger while he fires it (with an empty hunger bar it stops, and it won't start), and sparks fly where it ends (no fire, no block damage). A Scarlet Witch's complete **Chaos Shield** stops it at its surface and loses the beam's damage from its strength (see 2.3, ability 4). |
| 4 | B (toggle) | **God of Thunder** | *Thor: Ragnarok*: his eyes blaze white-blue and lightning crawls over his body. +4 attack damage, +6 armor toughness, every blow he lands arcs lightning to the next two creatures nearby (4 damage each), a thrown weapon and every fragment of a shattered Mjolnir arc too, and his lightning beam hits 1.5 times as hard. It burns hunger; with an empty hunger bar the thunder calms. |
| 5 | N | **Bifrost** | "Heimdall! Open the Bifrost!" With Mjolnir or Stormbreaker in hand: **N** opens the Bifrost screen (home, his saved places, save this one), **Sneak + N** takes him to the spot he looks at, up to 512 blocks away. See 8.4. |

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
  blocks away. (Aimed at a creature or player, Sneak + right click with Mjolnir pins them instead, 8.1.)
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
(the suit of most of that film: navy, gold and black armour with a gold Y on the chest, bare arms,
red cape; long ash-blond hair swept back from a bare forehead, falling down his back and in locks
in front of his shoulders, a full brown beard), *Love and Thunder, helmet* (the same suit with the
winged helmet of the fight at New Asgard, drawn in the hat layer: a gold shell with blue stripes,
wing panels at the sides, a bird's head over the brow and a face guard with a nose ridge that leaves
his eyes free, so they still glow as the God of Thunder; the film shows it only briefly, with the
gold-sleeved suit, and it is mostly computer-made), *Mighty Thor* (Jane Foster), and *Your own skin*.
Both Love and Thunder looks share one cape (a bright red one with a padded roll at the shoulders,
and his hair lying over it).

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
| Phoenix | A hooded telepath's mental power has no colour in *Brand New Day*; the fire of the Phoenix in *X-Men: Apocalypse* is "bird-shaped flames" (the colour is not in the sources) | Two palettes so a glance tells them apart: **fire** (the Burst, the charge, the Ember, flight embers) is gold and orange, core `#FFF4C2`, `#FFD65A`, `#FF8A1F`, `#E2531A`, `#8C2410`; **the mind** (reading, Mind Sense, Mind Control, the Psionic Field, telekinesis) is violet, `#F4EEFF`, `#B79CFF`, `#7C5CE0`, `#3F2B96`. Eyes violet when she uses her mind, burning gold when she charges. The Brand New Day look's own sparks are gold `#F2B632` with a pale green `#C4E88A`. |
| Spider-Man | The webs of the MCU films are plain white-silver; the Iron Spider is crimson and gold nanotech with white lenses; in *Brand New Day* his eyes turn fully black | Webs silver-white `#F2F6FA` with a shimmer `#BFD3E6` (organic: creamier, `#F4EFE3`); spider-sense a white-blue ring `#DDEEFF` / `#8EC5FF`; the Black Eyes black `#0A0305` with a dark red glint `#8A1020`; Instant Kill red `#FF2A2A` with gold at the leg tips `#FFC247`; the Iron Spider's look-change burst is gold `#C9A66B` with a pale gold `#E7D8A8`; the Radioactive Spider glows `#7CFC00`. The hero's red is `#DC2B2F`, with accents `#C9232D` (Homemade), `#C9A66B` (Iron Spider) and `#D9381E` with blue `#5BB6D6` (Brand New Day) |
| Thanos | The stones of *Infinity War*: Power purple, Space blue, Reality red, Soul orange, Time green; gold armour and the Gauntlet | **Power** `#9B4DEB` (core `#E6D2FF`), **Space** `#2F9BFF` (`#BFE6FF`), **Reality** `#E5253B` (`#FFB3BB`), **Soul** `#FF8A1E` (`#FFD6A0`), **Time** `#35D46A` (`#B8FFC9`); the Mind Stone's `#F5D77A` only outlines the empty socket. The boss bar takes the colour of his newest stone, and the Infinity Beam is white with the five colours as a banded halo (section 15.13.4) |
| Ultron | Amber-gold beams from the fingertips; red eyes and a red light in the torso; the Sentries blue at rest and red when he is in them; the Chitauri anti-gravity pale blue | Plasma amber-gold with a white core and a red edge (blue in the Sentry look, red in the human one); red eyes; Sentries blue, red when they hunt; anti-gravity a pale blue shimmer (the flight, the Pull, the Slam, the island's rim); the net white and red square motes; the Vibranium Key steel with `#7FD4FF` seams (section 16.10) |
| Apocalypse | A yellow-hued energy shield (fan wikis; the film's recap only says he obliterates the projectiles); "an immense sphere of violet light" for his teleport; warm ochre sand in the Cairo scenes; a sapphire-blue "soul-glow" in the transference ritual; the Phoenix's orange fire when she kills him | **Shield gold** (the boss's shield, the hero's shield and the Bestowal): a sphere of a hexagonal web, `#FFF2B0`, `#F5C84A`, `#E39B1B`, `#8A5A0E`. **Teleport violet** (the portals, the Mind Crush): `#F0D4FF`, `#B04FD8`, `#7A2EC8`, `#3F1A7A`. **Sand ochre** (the wave, spikes, lances, boulders, tornadoes, the cocoon, the sandstorm, the amber sky of the arena): `#E6CB86`, `#D8B76A`, `#B8934A`, `#8C6B2F`, haze `#C9A862`. **Soul-glow** (the Essence, the cap's glow): `#3FA0FF`, `#D8EEFF`, gold vein `#F5C84A`. His death uses the Phoenix's fire palette. His title is steel blue `#4A9BC4`. |

---

## 10. Operator commands **(added)**

| Command | Effect |
|---|---|
| `/enhanced power get [player]` | Show a player's power. |
| `/enhanced power set <players> <none\|magneto\|scarlet_witch\|quicksilver\|vision\|tommy\|billy\|loki\|thor\|phoenix\|spider_man\|thanos\|ultron\|apocalypse> [look]` | Give or remove a power, optionally straight into one of its looks. |
| `/enhanced look <players> <look>` | Change the look of every given player whose hero has that look (e.g. `tva`, `white_vision`, `mighty_thor`, `brand_new_day`, `iron_spider`, `endgame`, `sentry`, `en_sabah_nur`). |
| `/enhanced hex list` | List all Hexes in the current dimension. |
| `/enhanced hex clear` | Dissolve every Hex in the current dimension. |
| `/enhanced boss start thanos [force]` | Call the Mad Titan where you stand, as the ritual does (a portal, and six seconds later he lands 14 to 20 blocks away); `force` skips the site, the Overworld rule, `summonEnabled` and the rest (section 15.2). |
| `/enhanced boss start ultron [force]` | Without `force`, the ritual without the Scepter: the Program in hand is used up and the arena is the block looked at, after every check; with `force`, at your feet with no Program, skipping the site rules but not the switch or the sky (section 16.10). |
| `/enhanced boss start apocalypse [force]` | With `force`, wakes En Sabah Nur at your feet, with no Capstone, and skips the checks of the ground, the sky and the need for a Scarlet Witch or a Phoenix (never the switch that turns him off, Peaceful or the one-fight-at-a-time rule); without `force` the command is the ritual itself and needs a Capstone in hand and a block in sight (section 17.11). |
| `/enhanced boss stop` | End the running boss fight (a used Program or Capstone is given back, the island of Sokovia is put back); a second stop while a boss is leaving ends it at once. |
| `/enhanced boss list` | List the running boss fights. |
| `/enhanced build undo <player> [steps]` | **(3.1)** Undo that player's last Build Magic steps (1 by default, 1 to 1000), newest first, each as a queued job with the same safe rule as Ctrl + Z. **(3.2)** The player may be offline: a word argument resolves his name (online, the profile cache) or UUID, null-safe, `UndoFiles.ensureLoaded` reads his history from the disk once a run, and the job is `BuildJob.replay(profile, ...)`, acted by a fake player of his profile with Wanda's unlimited limits and the safe-undo rule. |
| `/enhanced build stop [<player>\|all]` | Stop the working spells where they are (undoable) and drop the queues: a player's, or everybody's with `all` or no argument. |
| `/enhanced build list` | The spells working or waiting: player, spell, dimension, percent, blocks. |
| `/enhanced build history <player>` | That player's steps, oldest first and numbered from 1, then the undone ones (the redo list): "1. Fill, 140 blocks, 2 min ago, overworld". **(3.2)** Also for an offline player (read from the disk). |
| `/enhanced build rank <player> [1-10]` | Billy's rank and practice; with a number, set the rank (practice becomes at least its threshold, exactly it when the rank goes down; the meter fills). |
| `/enhanced build meter <player> <value>` | Set Billy's magic meter (0 to its top). |
| `/enhanced build forget <player>` | Clear that player's history and pocket (memory and disk). |
| `/enhanced ward list` | **(3.2)** List every ward of the Book of the Vishanti: whose, its name, where and how far it reaches (section 20.2). |
| `/enhanced ward remove <profile>` | **(3.2)** Take away every ward of that player, online or not (a selector, a name or an id; the wards go by id alone); their bases stay as plain places. |
| `/enhanced road status [player]` | **(3.2)** Trials of the Witches' Road won, whether on the Road, falls (section 19). |
| `/enhanced road enter <players> [trial]` | **(3.2)** Send players onto the Road at once, with no Gene; `trial` from 1. |
| `/enhanced road leave <players>` | **(3.2)** Take players off the Road, keeping Gene and progress. |
| `/enhanced road reset <players> [newcourse]` | **(3.2)** Forget their Road progress; `newcourse` also deals new courses. |
| `/enhanced road skip <players>` | **(3.2)** Win the trial they are in (or count the next as won). |
| `/enhanced road set <players> <won>` | **(3.2)** Set how many trials they have won (0 to 99). |
| `/enhanced road layout [player]` | **(3.2)** Where the checkpoints, the finish and the gate of his trial are. |
| `/enhanced road cells` | **(3.2)** Road places in use or being cleared, and players on the Road. |
| `/road leave`, `/road status`, `/road choose` | **(3.2)** For everyone (a second root command): leave the Road keeping the Gene; how far you are; at the end of the Road, ask the choice again. |

| `/enhanced boss cleanup <center> <radius>` | Remove every entity of a boss's kind within the radius (the bosses, the Sentries, Thanos's sword, meteors, orbs and illusions, the Vibranium Key, the bolts, lances, boulders and cocoons), whether a fight owns it or not; no block is touched. |

**(added)** `/enhanced_anim` is a client-only tool (no operator rights, nothing changes for anyone else) for looking at the
animation: `play <gesture>` starts any gesture on yourself (and can freeze it at a tick, with `hold` and `release`), `fly <s> <u> <turn>`
pretends a flight speed, climb and turn so a pose can be seen without flying that fast, `stance <stance> on|off|auto` forces a held
pose, `target <name>` acts on another player in view, and `state` shows the numbers behind the pose.

**(added in 3.0)** `/enhanced_model <subject> <state> [tick]` is another client-only tool, for the people who draw the bosses: it
shows a copy, on your side only, of a boss or of something of his fight (`thanos`, `thanos_phantom`, `thanos_sword`, `thanos_meteor`,
`thanos_orb`, `ultron`, `ultron_sentry`, `vibranium_key`, `apocalypse`, `sand_cocoon`, `sand_lance`, `sand_boulder`) in a state, frozen
at a tick; `list` shows what can be shown and `clear` takes the copy away. The states are in each boss's section.

## 11. Server configuration **(added)**

All the main numbers (beam damage, Hex death chance, Hex radius, flight speeds,
shrapnel damage, shield reflect chance, the Chaos Shield's wind-up, time limit, strength and cooldowns, how much
Quicksilver Time slows the world, how much faster Quicksilver does everything, Tommy's speed, Loki's dagger damage, how
long Loki's Scepter confuses, Mjolnir's and Stormbreaker's damage and range, Thor's lightning beam (damage, range, its
strength with each weapon and as the God of Thunder, its hunger cost), the cooldown of shattering Mjolnir and the damage of
its fragments, how hard the Hex wall bites a Vision who pushes against it, the Vision of the Hex (health, punch,
beam, how long Wanda needs to rebuild him, or turning him off), how long it pins someone down, who is worthy of Mjolnir and whether it belongs to the first worthy player
who lifts it (`mjolnirOwnerOnly`), the Bifrost's
cooldown, how far the Phoenix's Mind Reading, Mind Sense and Mind Control reach and what they cost, how many creatures she can take over and for how long, the Psionic Field, and every number of the Phoenix Burst (charge time, radius, damage, what a player takes, wave speed, fire, knockback, whether it sets fires or hurts villagers and animals, hunger, cooldowns and the drain afterwards), Spider-Man's speed, jump, strength and fall damage, how long the Radioactive Spider's bite takes, how far spider-sense reaches and how often it scans, every number of the Web Shot, the Web Burst, the Web Zip (including the kick), the Web Swing and the Wall Crawl, how long webs and web blocks last and how many he can have, organic webs, each suit's protection and how long Magneto's strip lasts, the Iron Spider's legs, every number of Instant Kill (including whether it may strike players), and every number of the Black Eyes (what they give, what they cost, how long they last, the crash, and whether low health sets them off), the rules every boss fight shares (`[boss]`: how many fights at once, the wipe, abandon and time limits, the reward's minimum time, Creative players, the experience, the announcements, the arena's protection and a difficulty scale), every number of the Mad Titan (the call and its site, his health, the clock of stones, the surge and the enrage, the damage and cooldown of every move, the durations, the plates, poise and caps, and the share that earns the Gauntlet) and of the Thanos hero (his body and each stone's damage, range, duration, cooldown and hunger), every number of the Ultron fight (the bodies, the attacks, the Sentries and their waves, the lift: its mode, radius, height, depth and speed, and the field) and of the Ultron hero (his body, the plasma pool, every ability, the drones, the range of Vision's cut and lightning), what counts as mind power for En Sabah Nur's shield and how much of each kind it takes, his health, the strength and growth of his three shields, the windows and floors, the Escape, the damage and number of every attack, the Horsemen, the arena and its wall, and the hero's body, flight speed, shield, Wave, Entomb, Portal, Bestowal and Sandstorm (damage, range, duration, cooldown and cost)…) can be changed in `world/serverconfig/enhanced-server.toml`. The four client options
(*Hero animations*, *Reduced motion*, *Flight effects*, *Hide heroes' armour*) are in `config/enhanced-client.toml` of the player's own game,
with two of Thanos's (the music and the atmosphere of his fight, in its `[thanos]` section).

**(added in 3.1)** The server file has three new sections for Build Magic, `[build_magic]` (the time and speed budget, the hold-to-confirm rules, where spells may work, the undo and pocket caps), `[build_magic_wanda]` (her reach and her technical caps: her magic has no gameplay limit) and `[build_magic_billy]` (his meter, refill, side, reach, undos, the three practice numbers, Mom's help); new rows in `[billy]` (block lifting), `[magneto]` (the block reach and hardness, and every number of the Magnetic Pull), `[boss]` (telekinesis in the boss fights and the borrowed arena blocks), the three boss sections (how long each is held), and `[apocalypse_shield]` (the critical hits); and the two healing defaults of `[apocalypse_boss]` are now 0. The client file gains a `[build_magic]` section (*Build Magic hints*, *tips*, *preview detail*, *see the box through walls*, *other builders*).

**(added in 3.2)** `[apocalypse_boss]` gains the master factor `powerFactor` (0.65) and new defaults (shield 100, growth 1.2, 0.3 for each more fighter, a 12-second window, a 4-second stagger, Backhand 12, Boulder 9, ring 10, Entomb 80 ticks, Mind Crush nausea 80, Horsemen at 1.5 times the health and +3 damage), `[apocalypse_shield]` mends 2 percent a second after 200 ticks; `[scarlet_witch]` gains `shieldUnlimited` (true) and `shieldUnlimitedVsBosses` (true: with false her unlimited shield tires against bosses as in 3.1);
`[billy]` gains 22 options for the Hex Beam (`beam`, `beamDamagePerSecond`, `beamRange`, `beamInterval`, `beamExhaustionPerSecond`, `beamRankDamageBonus`, `beamRankRangeBonus`) and the Hex Barrier (`barrier` and 14 more); `[build_magic_wanda]` gains `instantMaxBlocks` (250,000), `instantMaxMillis` (5,000) and `instantTickMillis` (2,000, the share of one tick that all Instant spells together may use), and `[build_magic_billy]` the practice numbers `rank5Practice` to `rank10Practice` and `maxRank` 1 to 10 (10; an existing 3.1 `enhanced-server.toml` keeps `maxRank = 4` until the operator changes it, so a 3.1 server has four ranks until then).
Three new sections: `[vishanti]` (16 options: the teleport, the wards and their radii), `[darkhold]` (24) and `[witches_road]` (37: the Road with its farewell, the Parkour, the boat race with its fair factor and the structure copy). The wiki's *Server Configuration* page lists every option with its default and range; two new datapack tags, `enhanced:ward_immune` and `enhanced:magic_damage`, and the damage types `enhanced:hex_beam` and `enhanced:darkhold_beam`.

## 12. Sounds: the voices of the Infinity Stones **(added)**

Every Infinity Stone has a voice of its own, so the stones can be told apart by ear. They are synthesised from
scratch like all the mod's sounds (no samples), and mastered under the mod's other sounds, so that a stone sounds
**under** the portal, the awakening or the chime it comes with, never on top of it. Each stone has a short *use*
sound (two or three takes of it, so it is never quite the same twice) and a quiet *hum* that loops.

| Stone | Its voice | Where it sounds in the mod today |
|---|---|---|
| Space (the Tesseract) | A deep, swelling cosmic hum | Under the portal at both ends of Loki's escape and of a hero's jump, and when the Tesseract and the gene awaken the Scarlet Witch (there is no portal then); a soft hum while anyone holds the Tesseract. The Mad Titan and the Thanos hero use it for their Space Stone too (section 15.15). |
| Mind (Loki's Scepter, Vision's forehead) | A bright, shimmering, slightly eerie chime-hum | When the scepter's stone is touched (the awakening of the Scarlet Witch or of Vision, a hero it recognises, the touch that kills); under the chime of a hit that confuses (not over a creature the blow killed); as Vision's beam starts; a soft hum while anyone holds the scepter. |
| Power | A heavy violet roar | The Mad Titan's (section 15.15): when the stone arrives, quietly ten seconds before, at the tell of each of its moves and in the Infinity Beam's charge; and when a Thanos hero uses it, and in the Gauntlet's ceremony. |
| Reality | A liquid, bending, backwards-running swell | The same. |
| Soul | A breathy, mournful choir tone | The same. |
| Time | A ticking, rushing, rewinding shimmer | The same. |

The hum of the Space and Mind Stones is a quiet client-side loop for as long as the Tesseract or the scepter is in
either hand of a player: everyone near hears it, it follows the player, fades in and out with the item and stops when
it is put away (the *Players* volume slider of the game turns it down). The Thanos boss and hero use the other four
voices (and the Space Stone's) through `StoneKind`: the sound events `power_stone`, `reality_stone`, `soul_stone` and `time_stone` (and
their `_hum` loops) are registered, in `sounds.json` and in `ModSounds`, and `InfinityStone` lists all six.

---

## 13. Jean Grey, the Phoenix **(added in 3.0)**

> In *X-Men: Apocalypse* (2016) Xavier asks Jean to let go of the full strength of her power: she screams and **bird-shaped
> flames** burn En Sabah Nur, layer by layer, to ash. In *Spider-Man: Brand New Day* (2026) a hooded teenage telepath reads
> minds, jumps into the people within about 33 feet of her and freezes a whole district in a bubble of the mind. Both are
> played here, with one set of abilities. (The film does not show the Phoenix in *Brand New Day*, and does not say what
> colour her power has in either film: telepathy is violet and the fire is gold and orange so that they can be told
> apart, a design choice.)

### 13.1 Becoming the Phoenix

* **The Phoenix Ember.** A spark of the cosmic fire. It has no recipe: it is a very rare chest find, wired like the
  Tesseract (a pool in `data/enhanced/loot_table/inject/phoenix_ember.json` and six `neoforge:add_table` modifiers):

  | Source | Chance |
  |---|---|
  | End City treasure chest | 6% |
  | Bastion treasure chest | 5% |
  | Ancient City chest | 4% |
  | Stronghold library chest | 3% |
  | Nether Fortress chest | 3% |
  | Woodland Mansion chest | 3% |

  It doesn't stack, is fireproof, epic and has a foil.
* **The ritual.** A player **with no power** holds the Ember in either hand and **holds right-click for 3 seconds**
  (`phoenix.emberRitualTicks`, 60 ticks; it is the shared `AwakeningItem`, like the Essence of En Sabah Nur and the Infinity
  Gauntlet). While held, golden-orange particles spiral up the player and a few flames rise, and the sound `phoenix_charge`
  plays from the first tick. At the end **one Ember is used up** (not in Creative), the player awakens (the title, a burst of
  fire, `phoenix_burst` at low volume, a message to the whole server) and nobody is hurt. Let go early: the flames sputter out
  (`FIRE_EXTINGUISH`), the roar is cut and nothing is used up.
* **A hero** who uses it is refused at the first click with "Your body already holds a power", and nothing is used up:
  a player has one power at a time. Creative players can use it and keep it, and `/enhanced power set <player> phoenix [look]`
  works. A Spectator can't use items.
* **Not the Mind Stone.** Neither Loki's Scepter nor the Scarlet Witch Gene awakens the Phoenix: the Ember is her way in.

### 13.2 Abilities

Keys **R G V B N** are abilities 1 to 5. Every number is an option in the `[phoenix]` section of the server config.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (toggle) | **Mind Reading** | `Toggle.MIND_READING`. Every 5 ticks the server finds the first living thing along her view that has a mind (line of sight, `mindReadRange` 24) and sends her a snapshot (13.3); the client shows it in a panel beside the crosshair. Costs `mindReadExhaustion` (0.01) a tick. The read player is told once per 10 seconds (`mindReadNotifies`). **Sneak + R: Mind Sense** (below). |
| 2 | G (toggle) | **Flight** | Survival flight, speed `flightSpeed` (0.09). Pinned: no. A Hex wall bounces her like every hero. |
| 3 | V | **Telekinesis** | The new `Telekinesis.Mode.JEAN` (violet): Wanda's and Billy's rules, so creatures, players, dropped items, vehicles, primed TNT, falling blocks and projectiles in flight, and blocks (hardness up to 5, no block entities, not `telekinesis_immovable`, spawn protection and claims respected). Press again to launch, Sneak + V to set down; **(3.1) Sneak + V with nothing held tears out the block she looks at**, and the villains and the vanilla bosses can be held as bosses (the Ender Dragon is too vast), Apocalypse even through his shield. What she throws is `enhanced:telekinetic_slam` with her as the causing entity: Phoenix mind power (13.7). Range, speed and block limits are the shared `magneto.*` and `scarlet_witch.blockLift*` options. |
| 4 | B | **Mind Control** | The creature in her line of sight within `mindControlRange` (10) gets the effect `enhanced:possessed` for `mindControlTicks` (300). A host never targets her, her pets or her team (a strict rule: a possessed mob takes no target but the ones she sends it after; a creature with a brain, a hoglin or a piglin, writes its quarrels straight into its memory, so `PhoenixAbilities.guardBrain` forgets one with her side every tick), and every 10 ticks it is steered: the creature that last hurt her (24 blocks), the one she last hit (5 seconds), the nearest hostile within 16 blocks of her, else it walks back to within 4 blocks and stands guard. `mindControlMax` (1) hosts at once: a new one frees the oldest, who wakes `CONFUSED` for 2 seconds. A player is only held (`SPELLBOUND`, `mindControlPlayerTicks` 60) and only with PvP on. Refused: `enhanced:telepathy_immune`, Creative and Spectator players, Magneto's helmet, Loki's duplicates, armour stands, a creature pinned by Mjolnir, a stranger's pet with PvP off. Cooldown `mindControlCooldown` (400), cost `mindControlExhaustion` (6.0). **Sneak + B: the Psionic Field**, everything living within `psionicRadius` (16) of her, through walls, is `SPELLBOUND` for `psionicTicks` (60); not herself, her pets and team, her host, Creative and Spectator players, `spellbound_immune` and `telepathy_immune` creatures or whoever wears Magneto's helmet; players only with PvP on. Cooldown `psionicCooldown` (1200) on the same slot, cost `psionicExhaustion` (8.0), none of either if nobody is held. |
| 5 | N (hold) | **Phoenix Burst** | 13.4. |

**Mind Sense** (Sneak + R). A press that sets `Live.MIND_SENSING` for `mindSenseTicks` (120): a ring of violet motes spreads from her at 2 blocks a tick up to `mindSenseRange` (48), everyone sees it, and **only she** gets the glow through walls (an `EntityGlow` source): players `#E9E0FF`, hostile `#FF5C8A`, everyone else `#A98BFF`. Every mind counts (a hidden Loki, not his duplicates), and so do the helmeted as "silences". The action bar says "Minds nearby: 7 (players: 2, hostile: 3)". Cooldown of slot 1: `mindSenseCooldown` (400), cost `mindSenseExhaustion` (2.0).

**Passive: the Phoenix is not burned.** Damage of the `minecraft:is_fire` tag is cancelled (HIGH `LivingIncomingDamageEvent`) and she is put out each tick, if `fireImmune` (true). Unstoppable damage still hurts. Her own Burst never hurts her.

### 13.3 Mind Reading

* **The snapshot** (`ability/MindReading`, sent in a `MindReadPayload` to her alone, when the target changes or the content does, and at least every 10 ticks while she reads; the panel hides 15 ticks after the last one): the name and the entity type, the state (`ILLUSION`, `TOO_VAST`, `PLAYERS_OFF`, `BLOCKED` or `OK`, decided in that order), the mood (`CALM`, `ALERT`, `HOSTILE` with the name of its target, `AFRAID`), health, seven worn slots (main hand, off hand, head, chest, legs, feet, body), what it carries (a player's 36 slots; a creature's inventory, at most 8 stacks; an Enderman's block) and an extra line for a shapeshifted Loki or a disguised Vision.
* **Bounded.** Every stack is a stripped copy (item, count, damage, enchantments, custom name: no container contents, no book pages, no custom data) and the payload stays under 64 KiB, because vanilla disconnects a client that gets a clientbound payload over 1 MiB.
* **Rules.** Reading changes nothing about its target and works through the Hex wall, on a pinned creature, in Quicksilver Time and with PvP off. Not minds: armour stands, herself, Loki's duplicates (`ILLUSION`) and Creative and Spectator players. Magneto's helmet shields the wearer (`BLOCKED`), `enhanced:telepathy_immune` is `TOO_VAST`, and `mindReadPlayers` and `mindReadInventory` are server options.

### 13.4 The Phoenix Burst

Hold **N**. Not a Spectator, ability 5 rested, not already charging and at least `burstFoodCost` (6) food points, else "The Phoenix is still resting (12.3 s)" or "Too hungry to hold the Phoenix".

1. **Charging.** `Live.PHOENIX_CHARGING` and `PHOENIX_BLAZE` are set; she rises at 0.12 blocks a tick to 1.5 blocks above the ground and hovers (a pinned Phoenix does not rise, fall damage is reset), her walking speed is cut by 60 percent (the modifier `enhanced:phoenix_charge_slow`) and so is her flying speed, which is what a hovering player moves at (set again every tick of the charge by `PhoenixAbilities.applyBody`; the passives restore it the tick after), and she is **not invulnerable**. Flames spiral up her body; `phoenix_charge` plays.
2. **Release** (or full charge, `burstChargeTicks` 60: it fires by itself). Fewer than `burstMinChargeTicks` (12) ticks: cancelled, no damage, no hunger, the cooldown `burstCancelCooldown` (40). Otherwise the Burst, with the charge `f` (0 to 1).

| Value | Formula | Default |
|---|---|---|
| Radius | `lerp(burstMinRadius, burstMaxRadius, f)` | 8 to 24 blocks |
| Damage at the centre | `lerp(burstMinDamage, burstMaxDamage, f)` | 8 to 30 |
| Falloff | 1.0 to half the radius, linearly to 0.6 at the edge | |
| Cover | multiplied by `Explosion.getSeenPercent` from the centre; none reaches = nothing | stone protects |
| Players | multiplied by `burstPlayerFactor`, and only with PvP on | 0.5 |
| Burning | `lerp(0.4, 1.0, f) * burstFireSeconds` | 4 to 10 s |
| Knockback | `burstKnockback` away from the centre, scaled by cover, and 0.25 up; none for `pin_immune` | 1.2 |
| Cost | exhaustion `4 * burstFoodCost * max(0.3, f)` | 6 food points at full charge |
| Cooldown | `burstCooldown` | 1800 ticks |
| After | Weakness I and Slowness I for `burstDrainTicks`, none in Creative | 100 ticks |

* **The wave** is a sphere that grows at `burstWaveSpeed` (1 block a tick) from where she hovered, ticked on `LevelTickEvent.Post`; each creature is hurt once. **The damage type** is `enhanced:phoenix_burst`: it goes through armour (`bypasses_armor`), is **not** fire (a Blaze takes it), is not `bypasses_invulnerability` (a phased Vision takes nothing) and is a member of `enhanced:mind_power`, so it is Phoenix mind power.
* **Who.** The wave is an unaimed area attack: whoever the shared rule `AreaTargets.mayHurt` allows (hostile creatures, creatures after her or her team, the bosses, players with PvP on), and passive creatures (villagers, golems, animals, other players' pets) only with `burstHurtsPassives` (false). Never herself, her pets, her team, her hosts, Creative or Spectator players.
* **Blocks.** Never broken. With `mobGriefing` and `burstSetsFire`, up to `burstMaxFireBlocks` (40) fires on flammable ground where the wave passes, never where `BossFights.protects`, spawn protection, a claim or the world border says no.
* **Cleansing.** On release she loses `SPELLBOUND`, `CONFUSED`, Nausea, Slowness, Blindness and Darkness. Wanda's Chaos Shield is worn down by it (the rest passes through), like an explosion.
* A creature it kills leaves a puff of golden ash.

### 13.5 Interactions

| Rule or hero | What happens |
|---|---|
| One power at a time | The Ember refuses a hero. Losing the power ends the charge, the possessions and Mind Sense. |
| The Hex (Wanda) | Jean is bounced at the wall like every hero. The mind powers ignore the wall; the Burst's wave ignores it too. |
| The Chaos Shield | The Burst wears it down. The mind powers reach Wanda inside it. |
| Magneto | His helmet blocks reading, sensing, taking over and the field, not telekinesis and not the Burst. She is not metal. |
| Mjolnir's pin | A pinned Phoenix can't fly or rise, but reads, takes over and freezes; a pinned creature can't be lifted or taken. |
| Billy and Loki | The Burst burns Spellbound, Confusion and Nausea off her. Taking over a confused or spellbound creature frees it first. A hidden Loki is found by Mind Sense; a shapeshifted Loki is seen for what he is. |
| Quicksilver Time | Slowed creatures are read, taken, held and burned; the wave runs on the server clock. |
| PvP off | Reading still works (the player is told); taking over, the field and the Burst don't touch any player. Creatures are always affected. |
| mobGriefing off | The Burst lights no fire. |
| Bosses | `enhanced:telepathy_immune` (the Ender Dragon, the Wither, the Warden and the 3.0 bosses) can't be taken, held or read; the Burst hurts them and their minions are ordinary creatures. |
| Vision | A phased Vision is intangible and takes nothing from the Burst. |

### 13.6 What the others see, and the HUD

Her head tilts and violet motes drift from her forehead to the one she reads; her eyes glow violet while she reads, senses, takes over or freezes, and burn gold while she charges; Mind Control is a hand to the temple with a stream of motes and a ring of sparks round the host; the Psionic Field is both arms out and a violet dome with hex lines that grows to its radius in 8 ticks, holds 4 and fades over 12; Mind Sense is a spreading ring. The charge is a V of trembling arms, a flame swirl over her body (`PowerAuraLayer`-like, drawn by `PhoenixFlameLayer`) and a firebird of flame ribbons behind her that grows with the charge; the Burst throws the V open, flings the firebird out, spreads a shell of fire with streaks, embers and scorch rings, and flashes the screen of everyone in range gold (a client option, *Reduced motion*, tones all of it down). Flying she leaves embers, with gold streams from the shoulders at speed. HUD: R `ON`/`OFF` and `sense 12.3s`, G `ON`/`OFF`, V `holding`, B the cooldown and `host: <name>`, N `charging 63%` or the cooldown.

### 13.7 For the Apocalypse boss: what counts as Phoenix mind power

`ability/PhoenixPower`: `isPhoenix(entity)`; `isPhoenixMindPower(source)`, exactly `source.is(enhanced:mind_power)` with a Phoenix as the causing entity (the Burst and a slam of something she threw); `addListener(listener)`, called on the server for every touch, **before any refusal**, once per cast: `BURST` (per creature, with the damage), `SLAM`, `MIND_CONTROL`, `PSIONIC_FIELD` (once per mind in the sphere) and `MIND_READ` (once when she starts looking at a new target). Mind Control, the Psionic Field and Mind Reading are not damage; a boss that is `telepathy_immune` still hears of the attempt.

**(3.1) Critical hits.** Her mind power (the Burst, a slam, and the strains of Mind Control and the Psionic Field) hits the boss Apocalypse **critically**, as the Scarlet Witch's does: 17.3.

### 13.8 Looks, colours, sounds

*X-Men: Apocalypse* (default: the blue uniform of the last scene with grey side panels, yellow stripes and collar, a black belt, red hair in big waves), *Spider-Man: Brand New Day* (the hooded jacket: an olive hood with a mustard lining, a mustard hoodie, a dark leather jacket with an orange topstitch, copper hair with a ragged fringe) and *Your own skin*, all with slim arms and no cape. Fire is core `#FFF4C2`, gold `#FFD65A`, main `#FF8A1F`, ember `#E2531A`, deep `#8C2410`; the mind is core `#F4EEFF`, main `#B79CFF`, body `#7C5CE0`, fringe `#3F2B96`. The Brand New Day look's generic energy (awakening, look change, HUD title) is gold `#F2B632` with a pale green `#C4E88A`. Seven sounds, synthesised from scratch like the others (`sfx/phoenix_*`, `mind_*`, `psionic_field`): `phoenix_charge` (a rising fire roar with a voice-like cry and a hard cut), `phoenix_burst` (a boom, the screech of the Phoenix and a flame whoosh with a hall tail), `phoenix_blaze_loop` (a flame roar, a loop that follows her while she burns), `mind_read` (a breath, a two-note glass chime and whispers), `mind_sense` (a sonar ping and a ring), `mind_control` (a ring-modulated bell, a reversed whisper and a click) and `psionic_field` (a deep swell, a glass pad and a freezing tail), each with English and Hebrew subtitles.

---

## 14. Spider-Man **(added in 3.0)**

> In *Spider-Man: Homecoming* (2017) a fifteen-year-old "gained spider-like abilities after being bitten by a radioactive
> spider" and goes out in the suit he made himself; in *Infinity War* and *Endgame* Tony's Iron Spider gives him four
> mechanical legs and an Instant Kill mode; in *Spider-Man: Brand New Day* (2026) he sews a new suit, makes webs of his
> own body, and his eyes turn black when the spider in him takes over. All three suits and the black-eyed surge are
> played here, with one set of abilities. He cannot fly: he goes everywhere by web. (The film does not say how long
> the surge lasts or what it costs: both are a design choice, and the colour of the webs is ours.)

### 14.1 Becoming Spider-Man

* **The Radioactive Spider** (`radioactive_spider`): a small spider in a jar, glowing sickly green (`0x7CFC00`).
  Crafted (shaped, `data/enhanced/recipe/radioactive_spider.json`) and found:

  ```
  F G F      F = Fermented Spider Eye
  G B G      G = Glow Ink Sac
  F G F      B = Slime Block
  ```

  | Source | Chance |
  |---|---|
  | Abandoned Mineshaft chest | 4% |
  | Dungeon chest | 5% |
  | Trial Chambers reward chest | 3% |
  | Ancient City chest | 2% |
  | Cave Spider killed by a player | 0.4% (+0.3% per Looting level) |

  Five `neoforge:add_table` modifiers in the global list (table `enhanced:inject/radioactive_spider`) and a recipe
  advancement unlocked by a Glow Ink Sac. Stack 16, epic, foil; used up by nothing but the bite.
* **The bite** (`RadioactiveSpiderItem.use`, only a player with no power, not already sick): one point of damage (never the
  last), `biteDelayTicks` (140 = 7 s) of the effect `enhanced:radioactive_bite` with Nausea, Weakness I and Slowness I,
  and Poison I for the last 3 seconds (it never kills); sound `spider_bite`, green motes. One spider is used up (not in
  Creative). When the effect **expires** (`MobEffectEvent.Expired` in `SpiderEvents`) and the player is still online,
  alive and without a power, `Powers.awaken(SPIDER_MAN)` (his name as the title and "With great power comes great responsibility" in the action bar, too long for a subtitle, the
  shared awakening sound and burst, a message to the server). He wakes in the Homemade Suit.
* **Cancelled** by milk or anything that removes the effect, by death, or by another power meanwhile ("The bite
  fades"). The effect is saved with the player, so a logout does not lose it. A hero is turned away with "The spider's
  bite changes nothing: your power does not mix" and keeps the item; a player already bitten with "You are still sick
  from the last bite". `/enhanced power set <player> spider_man [look]` works as for every hero.

### 14.2 The three suits

Looks are the `H` key's variants; the first is the default. Spider-Man is the **one exception to "the abilities are the
same in every look"** (section 0): the suits differ in **armour**, and **B** (legs, Instant Kill) works in the Iron
Spider look only. All three are slim-armed, with no cape.

| Look | Id | Armour | Toughness | Knockback resistance | Skin |
|---|---|---|---|---|---|
| Homemade Suit (Homecoming) | `homemade` | 0 | 0 | 0 | red knit mask with round goggles, red hooded vest with a black spider and a brass zipper pull, blue-grey sweatshirt, fingerless gloves with brass web-shooters, baggy sweatpants, red socks and shoes |
| Iron Spider | `iron_spider` | **14** | 6 | 0.2 | crimson mesh, big white lenses, gunmetal bands from the shoulders into a gold-outlined hexagon, gold trim, a gold circle on the back where the legs come from |
| Brand New Day | `brand_new_day` | **8** | 2 | 0 | orange-red and turquoise-leaning blue, raised silver web lines, a big black spider with long thin legs, tear-shaped lenses, a silver plate on each forearm |
| Your own skin | `own` | 0 | 0 | 0 | the player's skin |

The protection is **by look**, as permanent attribute modifiers (`enhanced:spider_suit_armor`, `..._toughness`,
`..._knockback`, idempotent, set by `SpiderServer.applyBody` for everyone and removed from anyone who is not a
Spider-Man): nothing is worn, so the rule that hides vanilla armour on heroes does not matter. A look change in the air
or mid-zip takes effect at once. The numbers are the options `homemadeArmor`, `brandNewDayArmor`, `brandNewDayToughness`,
`ironSpiderArmor`, `ironSpiderToughness`, `ironSpiderKnockbackResistance`. Spider-Man keeps the normal 20 health.

### 14.3 The body

| Attribute | Modifier | Amount | Option |
|---|---|---|---|
| `MOVEMENT_SPEED` | `enhanced:spider_speed` | +0.15 (ADD_MULTIPLIED_BASE) | `speedBonus` |
| `JUMP_STRENGTH` | `enhanced:spider_jump` | +0.30 (a jump of about 2.1 blocks) | `jumpBonus` |
| `ATTACK_DAMAGE` | `enhanced:spider_strength` | +2.0 (fists hit for 3) | `strengthBonus` |
| `FALL_DAMAGE_MULTIPLIER` | `enhanced:spider_fall` | -0.5 | `fallDamageReduction` |
| `ENTITY_INTERACTION_RANGE` | `enhanced:spider_legs_reach` | +2.0, legs out only | `legsReachBonus` |
| `FALL_DAMAGE_MULTIPLIER` | `enhanced:spider_legs_fall` | -1.0, legs out only (no fall damage) | |

Fall distance is reset every tick he clings, zips or swings. **Immune to webs:** a Spider-Man is never Webbed
(`MobEffectEvent.Applicable` at HIGH priority) and never held by a Spider Web block; a vanilla cobweb still holds him.
**Organic webs** (`SpiderServer.organic`): the look is `brand_new_day`, or `Live.BLACK_EYES` is on. His webs then last
`organicWebFactor` (1.5) times as long, look thicker and creamier, cost `organicWebCostFactor` (2.0) times the hunger and
need a non-empty stomach ("Too hungry to make webs"). Hunger is the only resource: `player.causeFoodExhaustion`, 4.0 = one
food point, nothing in Creative. New shared state, all appended: `Toggle.WALL_CRAWL` and `SPIDER_SENSE` (on at start)
and `SPIDER_LEGS` (off); `Live.CLINGING_WALL`, `CLINGING_CEILING`, `ZIPPING`, `SWINGING`, `SENSING`, `BLACK_EYES`,
`INSTANT_KILL` and `CASTING` for 6 ticks after a shot. No new `PowerData` field: `cooldownUntil(1)` is the burst's
cooldown, `(2)` the zip's, `(4)` and `(5)` the end of Instant Kill and of the Black Eyes while they last and then their
cooldown.

### 14.4 Abilities

Keys **R G V B N** are abilities 1 to 5. Only **G** needs the release of its key. Every number is an option in the
`[spider_man]` section of the server config.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R | **Web Shot** / **Sneak: Web Burst** | 14.5. |
| 2 | G | **Web Zip** (chainable) / **Sneak, held: Web Swing** | 14.6. |
| 3 | V (toggle) | **Wall Crawl** / **Sneak: Spider-Sense** | 14.7 and 14.8. |
| 4 | B (toggle) | **Spider Legs** / **Sneak, twice: Instant Kill** | Iron Spider look only, 14.9. |
| 5 | N (toggle) | **Black Eyes** | 14.10. |

A `blocked` Spider-Man (a spectator, riding, pinned by Mjolnir, held by Spellbound or by telekinesis, flying in
Creative or gliding) ignores every key, and every web, zip and swing he had ends; a cling ends too, except under
Billy's Spellbound, which holds him where he is on the wall. A dead player's tick and `PowerTicker.stopHoldAbilities`
clear the seven `Live` bits (`SpiderServer.stopHold`).

### 14.5 Webs

**Web Shot (R).** Needs the hidden half-second timer (`webShotCooldown` 10 ticks) and, if organic, food. The aim is the
first creature on the look ray within `webShotRange` (24) not behind a block, else the first block. Nothing to hit:
"Nothing to web", nothing spent. A complete Chaos Shield in the way takes the web (it pays 2: `WandaAbilities.wear`, only if
the Spider-Man may hurt its owner) and nothing beyond is touched. **A creature** (not in `enhanced:web_immune`, not a Creative
or Spectator player, not another Spider-Man, a player only with PvP on) gets **Webbed** for `webbedTicks` (120, organic
x1.5), a mob stops walking, and two web blocks are laid at its feet and at chest height. A web that reaches a `web_immune`
entity calls `BossHooks.onWebShot(spider)` on it (Thanos locks his Gauntlet) and applies nothing. **A block** gets a patch
of web blocks on the free air in front of the face, in its plane, within `webPatchRadius` (1: at most 3 x 3). Costs
`webShotExhaustion` (0.4, organic x2); sounds `web_shot` at him and `web_splat` at the impact; `Live.CASTING` 6 ticks;
a `SpiderFxPayload` SHOT to everyone within 64 blocks.

**Web Burst (Sneak + R).** Needs `isReady(1)` (`webBurstCooldown` 240) and a creature within `webBurstRange` (10) in
line of sight that the shared area rule allows (`AreaTargets.mayHurt` with passives off, no option: hostile creatures, creatures after him or his team, players with PvP on; villagers, golems, animals and pets are spared and a strand flies past them). `webBurstStrands` (24) rays leave his chest over a Fibonacci sphere turned by a random angle
(`y = 1 - 2(i + 0.5)/n`, azimuth `i * 2.399963`), each stopping at the first block or creature. A creature hit by any
strands is Webbed once for `webBurstWebbedTicks` (80, organic x1.5); a strand that hits a block lays one web block in front
of the face. Each strand that meets a complete Chaos Shield stops there and pays 0.5, at most 12 in all. Costs
`webBurstExhaustion` (2.0, organic x2); sound `web_burst`; a payload BURST with the strand ends.

**Webbed** (`WebbedEffect`, harmful, `0xF2F6FA`): `MOVEMENT_SPEED` and `FLYING_SPEED` -0.85 (ADD_MULTIPLIED_TOTAL), `JUMP_STRENGTH` -1.0,
silver motes round it. A blow of 6 or more takes 20 ticks off (`SpiderEvents`). **Immune:** `enhanced:web_immune`
(default `#enhanced:bosses`), any Spider-Man, Creative and Spectator players, and players when PvP is off.

**The Spider Web block** (`SpiderWebBlock`, `ModBlocks.SPIDER_WEB`, no item, no loot, hardness 0.5, destroyed by
pushing): a cobweb that does not hold a Spider-Man or a flying Creative player and **removes itself** by a scheduled tick
after `webBlockTicks` (200, organic x1.5), so the world never stays webbed and no `mobGriefing` gate is needed. Only into
free air (`isEmptyBlock`), never over anything else, with `mayInteract`, outside Adventure mode, inside the world border and
never where `BossFights.protects(level, pos)` says it is a boss arena. `webBlockCap` (64) live blocks per Spider-Man (the
oldest go first); `webBlocks` false turns the blocks off, the webs still wrap. Breaking sound: vanilla's cobweb; placing:
`web_splat`.

### 14.6 Zip and swing

**3.3:** the swing has **Web Assist** (on by default): see 32. The manual swing below is unchanged.

Both aim with a block-only ray (`Raycasts.block`, through fluids).

**Web Zip (G).** Needs `isReady(2)` (`zipCooldown` 6) and not `blocked`. The anchor is the point hit within `zipRange`
(40); no block, "Nothing to zip to". The **stand point** `T` (`SpiderMath.standAt`) is where his body ends up: 0.35 out
from a wall face with the anchor at chest height, hanging under a ceiling, standing on top of a block, else the first
free offset. If `Teleports.crossesHex(level, player, T)`, "The Hex stops your web" (the same rule as Vanish and the Bifrost);
a complete Chaos Shield in the way takes the web and pays 2; both spend nothing. Otherwise `Live.ZIPPING` is raised, the
cooldown and `zipExhaustion` (0.3, organic x2) are spent, a swing is let go of, and `SpiderFxPayload` ZIP_START goes to
everyone near and to him. **The pull is the client's** (`SpiderMotion`):
`v = min(zipSpeed * sqrt(speedFactor), 0.3 + 0.1 t, 0.2 + 0.12 |T - p|)` along the line to `T`, with `zipSpeed` 1.3
blocks a tick. It ends on arrival, on a collision after 3 ticks, after `zipMaxTicks` (60), on Space (he keeps his speed),
when the anchor is gone, on a jump of the position (a Hex bounce or a teleport) or when he is blocked. The server
validates every tick (the anchor block, the distance, the time, a position that jumps more than a hitch can explain)
and resets `connection.aboveGroundTickCount` and the fall distance. On arrival at a wall or a ceiling with Wall Crawl on he
clings at once (`state.surface`, a grace of 8 ticks); the client stops a little short of the stand point (it arrives within
0.45 of it), so for the first 8 ticks it draws him in to the surface at 0.2 a tick and loses nothing; on a floor he lands;
with Wall Crawl off he keeps 40% of his speed.
**G again at any time** is a new zip: the chain. A zip aimed at a **creature** ends in a kick (`zipKickDamage` 4.0,
`enhanced:web_kick`; only a creature the same rule allows is aimed at, a villager or an animal in the way is not, and a boss or a Creative player never; 0 turns it off). **A refused zip or swing** (the Hex, a shield,
an empty stomach, no anchor, a cooldown, a hold on him) is answered with a ZIP_END or SWING_END of reason 2 to him alone,
because his client has begun the movement on its own prediction and stops it when it hears so (it drops an answer that
never comes after 12 ticks).

**Web Swing (Sneak + G, held).** The same aim within `swingRange` (40); the rope is `L = clamp(distance, 4, swingRange)`.
While G is held (`HeroServer.stopHold` clears it when he dies) the client runs a pendulum, `SpiderMath.swingStep`:
`vel' = (vel + (0, -g, 0) + airControl) * 0.992`, clamped to `swingMaxSpeed` (1.8), with `g = swingGravity` (0.06); a
position past the rope length is pulled back onto the sphere, so the speed along the web is lost and the speed across it
kept. W reels the rope in (down to 3), S lets it out (to 1.25 times the first length). He lets go on release (keeping
his speed, plus a push if Space is held), after 10 ticks on the ground, if the anchor is gone, after `swingMaxTicks`
(300) or when blocked. A swing begun on the ground hops him off it and pushes him 0.3 towards the anchor (from rest the
pendulum's way down is into the floor, and Sneak keeps a player from stepping off a ledge). G without Sneak while swinging
zips to the new aim. Costs `swingExhaustion` (0.01) a tick.
A loop `web_swing` follows him (client, `PowerLoopSounds`). ZIP_END and SWING_END carry the reason: 0 arrived,
1 cancelled, 2 refused, 3 blocked, 4 timeout, 5 released, 6 anchor lost.

### 14.7 Wall Crawl

`Toggle.WALL_CRAWL`, on by default; "Wall Crawl on" or "off". A movement rule and a render transform: gravity is switched off
and his own keys move him along the surface, the player keeps the standing box, no pose is forced, so a cling can neither
suffocate nor get stuck. `SpiderClimb` has no client imports and is shared by the client (movement) and the server (the
flag others see).

* **Surfaces** (`SpiderClimb.detect`, reach 0.12): a wall on one of the four sides, or a ceiling above the head, made of a
  block with a collision shape that is not in `enhanced:cannot_cling`. A cobweb, a vine, a ladder never count. At a corner he
  picks the wall he presses into.
* **Entering:** Wall Crawl on, not blocked, not in a liquid, not riding or gliding, food above 0 (Creative excepted), 6
  ticks since a leap. A wall: he presses into it (`wish . -normal >= 0.3`) **in the air**, or on the ground holds Space.
  A ceiling: Space with his head against it, or a climb that meets one. A zip or swing that ends at a surface enters with
  no input.
* **Moving:** the velocity is set each tick: `v = speed * (F * forward + S * side) + (-normal) * 0.05`, with
  `speed = crawlSpeed (0.22) * speedFactor * (legs out ? 1.25 : 1)` and `speedFactor = clamp(MOVEMENT_SPEED /
  (sprinting ? 1.3 : 1) / 0.1, 0, 4)` (1.15 for Spider-Man, 1.84 with the Black Eyes). The attribute is the key: Slowness,
  Quicksilver's zone, Webbed, Spellbound (a factor of 0: frozen on the wall) and Loki's Scepter slow his crawl, zip and
  swing. On a wall `F = (a L + (b + 0.45) UP) / max(|w|, 0.25)` with `a` the look along the wall and `b` its height:
  looking along the wall W goes that way, at it or up W climbs, down more than about 27 degrees W climbs down, shrinking
  smoothly through the dead zone so there is no jitter. On a ceiling W walks the way he faces. The keys are read from the
  key bindings and cleared in `MovementInputUpdateEvent` while he clings.
* **Leaving:** a **leap** (a new press of Space at least 4 ticks after entering) along `normalize(look + normal * 0.35)` at
  `leapSpeed` (0.9) * sqrt(speedFactor); walking onto the ground for 2 ticks, V off, being pinned or held, the surface
  vanishing for 4 ticks, no food, water, riding, or a teleport. A **mantle** over the top of a block, and a flip from a wall
  to a ceiling and back, are handled at the edges. The optional corner climb is not built: he picks the wall he presses into.
* **Cost:** `crawlExhaustion` (0.005) a tick while he moves.
* **What the server sees:** the flag others see is raised from **what his own client says**: `SpiderClingPayload` (the
  lane's one client-to-server message, registered by `SpiderNetwork`) carries 0 nothing, 1 a wall or 2 a ceiling, and the
  way it faces, when the cling begins, changes or ends and every 8 ticks while it lasts. The server believes it for 12
  ticks, only for a Spider-Man and only while a block he may cling to is within 0.45 of that side of his body
  (`SpiderClimb.near`): a crawl down a wall looks like a fall from his positions alone, and a steady climb like the arc of
  a jump, so the server cannot guess them. The flag lasts 4 ticks after the last confirmed tick, and goes at once when the
  client says it let go. Until a client says anything (a test, a client without the lane's movement) the server guesses
  from his position, `onGround` and the vertical history (he is beside or under a surface, in the air, not falling, not on
  the arc of a jump). The client also sends a keepalive position while it hangs still, and the server zeroes the downward
  motion of its copy while the flag is up. While clinging, zipping or swinging the server resets `aboveGroundTickCount` and
  the fall distance every tick.

### 14.8 Spider-Sense

`Toggle.SPIDER_SENSE`, on by default, Sneak + V. A warning of danger only. `SpiderSense.threats` runs every
`spiderSenseInterval` (5) ticks within `spiderSenseRange` (12), by spherical distance, and finds: a projectile not shot by
him whose straight path comes within 1.2 blocks of his body in the next 25 ticks; a hostile mob within 6 blocks that is
aggressive and targets him (a neutral mob only if it does); a creeper swelling within 7 or TNT with 40 ticks or fewer within 8;
(PvP on) a player within 4 who looks at him and has swung in the last 10 ticks. A threat not warned of in the last 40 ticks sets
`Live.SENSING` for 12 ticks (the pose) and sends `SpiderSensePayload(ids, strong)` to him alone (`strong` when a hit is
less than 12 ticks away): the client outlines the threats through walls (`EntityGlow`, `0xDDEEFF`) for 1.5 s, flashes the
screen edge, draws ripples and an arrow to the strongest threat, and the tingle `spider_sense` plays to him alone. It costs
nothing and changes nothing; with the toggle off nothing is sent. With the Black Eyes the range is x`blackEyesSenseFactor`
(2), the interval 2 ticks, and every hostile in range is listed and outlined. `SpiderSense.warn(player, source)` is public so
a mind-reading threat could be added later.

### 14.9 Iron Spider systems

Look `iron_spider` only ("Only the Iron Spider suit has legs"; the HUD says `Iron Spider only`).

**Spider Legs (B, toggle).** Four gold-and-black legs unfold from the gold circle on his back (14 ticks, sound
`spider_legs`, folding is 8 ticks at a lower pitch). While out: no fall damage, `legsReachBonus` (2) more reach, hunger
`legsExhaustion` (0.01) a tick, a crawl 25% faster, and every `legsInterval` (20) ticks the nearest **threat** within `legsRange`
(4.0) is jabbed for `legsDamage` (5.0) with a little knockback (`enhanced:spider_legs`, in the `melee_blows` tag). They
fold when the look changes, when the nanites are scattered or when B is pressed again. The server forces the toggle off in
any other look.

**Instant Kill (Sneak + B).** Needs the legs out. **Pressed twice within `instantKillConfirmTicks` (60)**: the first press
says "Instant Kill: press Sneak + B again to confirm". It needs `cooldownUntil(4)` passed and a non-empty stomach. On:
`Live.INSTANT_KILL`, the lenses close to two red slits, sound `instant_kill_on`, and for up to `instantKillMaxTicks` (200) every
`instantKillInterval` (5) ticks each of the four legs strikes one threat within `instantKillRange` (5.0), nearest first, for
`instantKillDamage` (1000), with a red-gold spark and `leg_strike`. Hunger `instantKillExhaustion` (0.08) a tick (about 4 food
points in all). It ends by itself, with Sneak + B, when the legs fold, at 0 hunger or when blocked; then `instantKillCooldown`
(600). **A threat** (`SpiderServer.isThreat`): a hostile `Enemy` mob that is alive, not owned or tamed; a neutral mob only if
it targets him; never a villager, golem, pet, animal, boat or armour stand, a Loki illusion, nor a player unless
`instantKillPlayers` (default off) and PvP is on. A creature in `enhanced:instant_kill_immune` (default `#enhanced:bosses`
and `#enhanced:boss_minions`) or for which `BossFights.isMinion` is true takes only `legsDamage`: four legs would clear a
boss's finite army in seconds. A complete Chaos Shield between a leg and its victim stops the strike (it pays its damage,
capped at 5). **Magneto:** `Telekinesis.isMagnetic` answers true for an Iron Spider, and the Armor and Weapon Strip
(`MagnetoAbilities.disarm`, via `SpiderServer.onStripped`) scatters his nanites for `ironSpiderStripTicks` (200): the suit's
modifiers, the legs and Instant Kill are off and "The nanites are scattered".

### 14.10 Black Eyes

**N**, a toggle. Needs `now >= cooldownUntil(5)` and at least 6 food points (Creative excepted). On: black lenses with a dark
red glint, the heartbeat `black_eyes_on`, wisps. **N again calms it** (no crash, half the cooldown). While it lasts (at most
`blackEyesMaxTicks`, 600; the end shows in `cooldownUntil(5)`), as `enhanced:spider_eyes_*` modifiers: speed +60%
(`blackEyesSpeedBonus`, ADD_MULTIPLIED_TOTAL: 1.15 x 1.6 = 1.84 times the normal speed; climbing 1.6 times, zip and swing
about 1.26 times, as they use the square root), melee damage +4 (`blackEyesStrengthBonus`; bare fists with the +50% attack
speed hit 6 times a second for 7: 42 a second, level with Magneto's shrapnel, the 3.0 review lowered it from +6), attack
speed +50%, jump +25%, knockback resistance 0.3, another -0.25 on fall damage; organic webs in every look; a wider spider-sense;
hunger `blackEyesExhaustion` (0.05) a tick. **The price:** when it runs out or the stomach is empty he **crashes**: Slowness II
and Weakness I for `blackEyesCrashTicks` (200), "The surge passes. You are drained.", and `blackEyesCooldown` (600).
**Stress surge (off by default):** if `blackEyesPanicHealth` is above 0 and his health falls to that, the eyes switch on by
themselves and cannot be calmed for 5 seconds. It never switches on otherwise and gives no night vision.

### 14.11 Interactions

| Rule or hero | What happens |
|---|---|
| One power at a time | The spider turns a hero away. Losing the power (`SpiderServer.endAll`, death, logout, dimension change, server stop) drops every web, zip, swing, cling, mode and modifier; the power, the look and the three toggles survive death. Web blocks already placed vanish by their own tick. |
| The Hex (Wanda) | He bounces off the wall like every hero: a zip, swing or cling it throws ends. A zip across the wall is refused at the press; a shot or burst passes the wall. |
| The Chaos Shield | A complete shield is solid to his webs, zips and legs (2 for a shot, 0.5 for a strand, the damage of a strike capped at 5); one that is still forming stops nothing; with PvP off the web costs the shield nothing. He cannot cling to a shield. |
| Magneto | The Iron Spider is metal: he can be lifted (30 s for a player) and, **with PvP on**, his nanites scattered (14.9). The other suits are cloth. |
| Mjolnir's pin | Every key does nothing, every web ends. Mjolnir cannot be webbed. |
| Billy's Spellbound | `blocked`, speed factor 0: a zip or swing ends and a cling holds him in place. |
| Loki | His duplicates dissolve at a web or a leg and are never a threat; a shapeshifted Loki is webbed as himself. |
| Quicksilver Time | The slowed zone, Slowness, Webbed and Spellbound slow his crawl, zip and swing through the speed attribute. |
| Telekinesis | Held, he is `blocked`. Spider-sense warns of a held player's slam. |
| Vision | A phased Vision is nothing to web; a dense one can be webbed. |
| PvP off | Webs, bursts and kicks do not affect players; Instant Kill never targets one. |
| Bosses | `enhanced:web_immune` and `enhanced:instant_kill_immune` default to `#enhanced:bosses` (and the latter `#enhanced:boss_minions`); minions are webbed normally but take only `legsDamage`. No web block inside a protected boss arena. Wall Crawl and zips work on any block. |
| mobGriefing | Nothing he does breaks or changes a block except placing self-removing webs into free air. |
| Creative and Spectator | Creative: everything works and costs nothing. Spectator: every key is ignored. |

### 14.12 What others see, the HUD and the animation

His **client side** is `SpiderMotion` (his own movement, in `PlayerTickEvent.Pre`, with prediction: a zip or swing starts
at once on his own aim, the server's `ZIP_START` confirms it or its anchor takes over if it differs by more than a block,
an unanswered prediction is dropped after 12 ticks), `SpiderEffects` (the web lines, splats and the FOV kick), the layers
`SpiderLegsLayer` (`LegIk`, a two-bone solve, three rigid segments of 9, 10 and 7 pixels, planting on the wall, ceiling or
ground, striking on `LEG_STRIKE`) and `BlackEyesLayer` (`spider_eyes_black_<look>` and `spider_eyes_kill_iron`, the second
pass drawn glowing), and `SpiderSenseClient` with `SpiderSenseOverlay`. Everyone near sees the strand to a shot, the 24 strands
of a burst, the web from his hand to the anchor for as long as a zip or swing lasts (then it detaches, flies to the anchor and
fades), the splat decals (20 ticks) and, on a wall, a soft stick at each hand.

**HUD** (`SpiderClient.hudStatus`): R `burst 9.5s` (grey) or `organic` (aqua); G `zipping` or `swinging` (aqua); V `ON` or `OFF`,
then `clinging`, `sense OFF` and `DANGER!` for a second after a warning; B `Iron Spider only`, `nanites scattered`, the legs
`ON` or `OFF`, `confirm?`, `INSTANT KILL 7.5s` (bold red), `recharging 21.0s`; N `ON 18.4s` (red) or `recharging 24.0s`.

**Animation.** Seven stances (`SPIDER_CLING_WALL`, `_CLING_CEILING`, `_SURGE`, `_KILL`, `_ALERT`, `_ZIP`, `_SWING`) with their
rows in `SpiderPoses`: against the wall facing it, upside down on the ceiling (the body flips about a pivot 0.9 blocks up),
the hunched trembling surge, the menacing kill stance, the alert guard, the zip lean, the swing with the knees tucked; a
crawl cycle of one stride per 1.1 blocks. Fifteen gestures (`SpiderGestures`, `SpiderEdges`): shoot, burst, zip start,
swing start, crawl on and off, sense toggle, alert, legs out and in, kill on, surge on, the landing (a bend of the knees after a fall
of a few blocks, a three-point landing with a ring of motes after a long one), the leap off a wall and the arrival of a zip. His own key press acts before the server answers (`SpiderActing`). In first person the hands reach up for a wall, thrust
out for a shot, throw wide for the burst and shrug when the legs come out. Flourishes (`SpiderGestureFx`): motes at the
shooting hand, the burst ring, black-red wisps at the head for the surge, gold sparks at the leg roots, a red flicker for Instant
Kill, two pale rings for the sense; thinned by the client's *Flight effects* option, with *Reduced motion* the FOV kick and the
screen pulses are off. His walk and sprint have a character of their own and a lope while the surge lasts. He does not fly, so
he has no flight profile.

### 14.13 Colours, sounds and files

The webs are always silver-white (`0xF2F6FA`, shimmer `0xBFD3E6`; organic `0xF4EFE3`, `0xE7DCC4`); spider-sense is core
`0xDDEEFF`, ring `0x8EC5FF`; the Black Eyes are black `0x0A0305` with a glint `0x8A1020` and aura wisps `0x1A0608`; Instant Kill
is `0xFF2A2A` with gold `0xFFC247` at the leg tips; a look change bursts in the look's two accent colours (listed below); the Radioactive Spider glows `0x7CFC00`. `Power.SPIDER_MAN` is spider red `0xDC2B2F` with a web-silver glow
`0xDCE6F2`; the looks' accents are `0xC9232D` / `0xB9CDE8` (homemade), `0xC9A66B` / `0xE7D8A8` (Iron Spider) and `0xD9381E` /
`0x5BB6D6` (Brand New Day). Thirteen sounds, synthesised from scratch with the mod's toolkit (mono, 44.1 kHz): `web_shot` (the
"thwip": a sine glide from 2400 to 700 Hz with a click, an air layer and a thump), `web_zip` (a thwip, then a rising reel with a
whoosh, a string twang and a closing tck), `web_burst` (24 miniature thwips, a whump and a shower), `web_splat`, `web_stick`,
`web_swing` (the loop `web_swing_loop`: a taut-web creak), `spider_sense` (a trembling 1760 Hz tingle with a swell and pings),
`black_eyes_on` (an inhale, a double heartbeat and a growl) and `black_eyes_off` (an exhale), `spider_legs` (ratchet chirps and
a shing), `leg_strike`, `instant_kill_on` (a descending three-note alert and a sub-bass drop) and `spider_bite`; each with English
and Hebrew subtitles. Vanilla's cobweb break sound is used for the web block and the shared awakening sound for the awakening.

The damage types `web_kick` and `spider_legs` are in the tag `enhanced:melee_blows` (the Chaos Shield's melee rule, because the
legs are his hands). Entity tags `web_immune` and `instant_kill_immune`, block tag `cannot_cling` (empty). Textures: the three
suits (`entity/suit/spider_*`), the five eye overlays and the legs (`entity/spider_*`), the web splat (`effect`), the Spider Web
block, the animated jar of the item (4 frames, a green pulse) and the two effect icons.

---

## 15. Thanos — the Mad Titan **(added in 3.0)**

> "I am inevitable." The Thanos of *Avengers: Infinity War*: a giant in gold, the Double-Edged Sword in his right hand
> and the Infinity Gauntlet on his left, who takes the stones one by one. In the mod he is the hardest of the three bosses
> and the source of the **Infinity Gauntlet**, the item that makes a player Thanos (15.11).

Sections 15.1 to 15.10 are the boss; 15.11 is the hero. The boss's code is `thanos/` (`ThanosEntity`, `ThanosMoves`, `ThanosFight`, `ThanosLane`, `ThanosSummon`,
`ArenaCheck`, `ThanosDefence`, `ThanosClock`, `StonePowers`, the sword, meteor, orb and phantom entities); the shared
rules of every boss (the registry of fights, the participants and the ledger, the wipe and the abandonment, the rest, the
rewards and the gifts, the bars) are the `boss/` package, and its numbers are in `[boss]` of the server config; Thanos's
own are in `[thanos_boss]`.

### 15.1 The design on one page

* He is called by the **Tesseract and Loki's Scepter held together** for five seconds (15.2), in open country (15.3).
* He arrives from a portal in the sky, invulnerable for three seconds, and the fight is a **clock**: he starts with the
  **Power Stone** and takes **one more every 90 seconds of combat** (Space, Reality, Soul, Time), each of which gives him
  moves, an announcement and a power surge in which he kneels and takes half again the damage. He never has the sixth, the
  Mind Stone. The difficulty is the clock, not the health (15.4).
* Every move has a **tell** and a number; every stone move is a **clench** that enough blows break (15.5, 15.6).
* The world is **never changed**: no block is broken or placed unless the server turned `blockDamage` on, and then only
  by two small craters. There is no containment: nobody is held in and nobody is walled out.
* He is beaten once; **one Infinity Gauntlet for each fighter who did 3 percent of his health or struck the last blow**
  (15.10). The ritual consumes nothing, so there is nothing to refund.
* No fight is saved: the entities are never written to the world, and the framework sweeps strays.

### 15.2 Summoning

**The hold.** The Tesseract in one hand and Loki's Scepter in the other, either way round; right-click and hold for
`summonChargeTicks` (100, at most the 100 ticks the items can be held). While held, a spiral of the Space Stone's blue
and one of the Mind Stone's gold rise round the player, a drone swells, a bell tolls every second (a little higher each
time) and the action bar counts "The stones are calling... n/5". Letting go, moving an item out of a hand, or dying
cancels it. Nothing is used up. `ScepterRituals.Hold` carries it; `ThanosSummon` is the hold, registered when the mod loads.

**The refusals** are shown in red in the action bar when the hold would begin, and nothing else happens (the Scepter does
not explode and the Tesseract does not take the player away): the server turned it off or the player is a Spectator
(`summon.disabled`), Peaceful, Mjolnir lies on the player (`mjolnir.pinned_cannot`), he is already here (`summon.active`),
another boss fights (`boss.busy`, with its place), the rest is not over (`summon.resting`, `m:ss`), another dimension than the
Overworld unless `summonOverworldOnly` is false (`summon.wrong_dimension`), and the site (15.3). When the hold ends the
shared rules are asked again (`BossFights.start`: Peaceful, one fight at a time, the rest), then the lane's.

**The call.** A title for everyone within 160 blocks, a portal in the sky 30 blocks over the landing (less where the column is
not free), a column of light on the landing and a ring on it every second; the landing is **14 to 20 blocks** from the
summoner, where there is room (`ArenaCheck.arrivalSpot`), or the summoner's own ground. After `summonCountdownTicks` (120)
he is made above the landing and falls through the portal, lands (a ring that shoves) and rises: **60 ticks of arrival,
invulnerable**. The arena is a circle of `arenaRadius` (40) blocks round the summoner; the clock and the leash reach 24
blocks beyond it, and so does the count of fighters (`joinWithin`).

An operator calls him anywhere with `/enhanced boss start thanos [force]` (`force` skips the site, the Overworld rule, `summonEnabled` and the rest; Peaceful and one fight at a time still apply).

### 15.3 The site

`ArenaCheck.verdict` (a pure function over a `World` the real server and the game tests each supply):

1. **Border:** the summoner and four points at `arenaRadius` north, south, east and west are inside the world border.
2. **Ground:** solid ground under the feet, and no liquid at them.
3. **Sky:** open sky above (the motion-blocking height map) and `summonSkyBlocks` (24) free blocks over the head.
4. **Room:** 24 sample spots (8 on each ring of radius 8, 16 and 24); a spot counts if it has standing room (solid ground
   within three blocks up or down, three free blocks above, no liquid). At least `summonOpenFraction` (60 percent) must.

### 15.4 The body and the clock

* **Health.** `health` (3,000) for one fighter, plus `healthPerExtraPlayer` (50 percent) for each other, at most
  `maxFightersCounted` (4) counted: 7,500, times `[boss].difficultyScale`. Creative and Spectator players are not counted.
  The maximum is recomputed when a fighter joins or leaves, keeping the fraction of health he has left. **The vanilla
  health attribute cannot hold thousands, so the entity has 1,000 health and every number of this design (3,000, the
  6 percent cap, the poise) is converted by the factor between the two (`toBar`, `toPoints`); the ledger and the bar are
  in the entity's own health.**
* **The stones.** `stonesAt(combatTicks) = 1 + combatTicks / stoneIntervalTicks` (1,800: 90 seconds), at most `maxStones`
  (5) and never more than the five he can hold; the order is Power, Space, Reality, Soul, Time, and **there is no sixth**. A
  stone adds its moves to his choice, his voice, a title for the arena and a **surge**: for `surgeTicks` (60) he kneels,
  starts no move, and takes `surgeDamageFactor` (1.5) times the damage. When several stones are added at once (a test, a
  command) only the last surges. Ten seconds before a stone the arena is told its name.
* **Combat ticks** only run while at least one fighter who is not Creative or Spectator is in the arena; with nobody he heals
  `idleRegenPerMinute` (2 percent) after a minute alone.
* **Damage scale.** Everything he does is multiplied by `difficultyScale` × (1 + `damageScalePerStone` (0.06) × (stones − 1))
  × (1 + the enrage). The **enrage** begins `enrageAfterTicks` (3,600: three minutes) after his last stone and adds
  `enrageStepPerMinute` (0.04) per minute up to `enrageMax` (0.4); the bar then says he is enraged.
* **Rest between moves:** `max(10, 26 − 3 × stones)` ticks, `gapPerExtraPlayer` (2) fewer for each extra fighter, never below 4.
* **The bar** (`ThanosFight`): his health, in the colour of his newest stone (purple, blue, red, yellow, green), named
  "Thanos, the Mad Titan", "Only the Mind Stone remains" at five stones, "Thanos, Enraged" in the enrage.

### 15.5 The moves

A state machine (arriving, idle, tell, active, recover, surge, staggered, dying) chooses a move when he is idle: the
**reactive** moves first, by their triggers (the Power Pulse for a hiding fighter, the Rewind after much damage, the Space
Barrier after ranged hits, the Veil after ranged hits, the Soul Sight for a trick near him), then the **Infinity Beam** when
it is ready and he has five stones, then a **weighted choice** among what the situation allows (distance, line of sight, the
number of fighters, the cooldowns; a move he used among his last two weighs half, and the moves of a stone that just arrived
weigh 2.5 times for a while). Each move: the **tell** in ticks, the tick at which his **aim locks** (a player who sidesteps
after it dodges), the **after** ticks that follow, its **weight** and its **cooldown**. The damage is in the config
(`[thanos_boss]`), times the damage scale; a Chaos Shield that stops a beam, a meteor, an orb or the sword pays what it
would have done (the sword's melee blows `swordShieldFactor` (2) times, a beam once).

| Move | Stone | Tell | After | Weight | Cooldown | Damage | What it does |
|---|---|---|---|---|---|---|---|
| Sword Sweep | none | 16 | 14 | 5 | 40 | 30 `thanos_sword` | a cone of 160 degrees and 5.5 blocks; breaks a raised Shield (100 ticks), throws a rider off |
| Overhead Smash | none | 24 | 16 | 3 | 100 | 26 + ring 12 | blow at the point 4 ahead, radius 3.5; then a ring from 3.5 to 6 only the grounded meet |
| Gauntlet Jab | none | 8 | 14 | 4 | 30 | 14 × 2 `thanos_blow` | two blows 8 ticks apart in a cone of 50 degrees, 4 blocks |
| Sword Throw | none | 14 | 10 | 2 | 280 | 22 per pass | the sword flies out 28 blocks (or to a wall) at 1.3 blocks a tick and returns at 1.5 through blocks, once per creature per pass; no Sweep or Smash while it is away; the Chaos Shield turns it home |
| Choke | none | 14 | 50 | 1.5 | 400 | 16 over 40 ticks, + 8 | takes one player within 3.2 blocks in a cone of 40 degrees (Loki and Vision first; never Creative, a phased Vision, a pinned or telekinesis-immune creature), holds him 3.2 up for 40 ticks (4 every 10), throws him; 60 damage from others frees him and staggers Thanos |
| Leap | none | 18 | 26 | 2 (3 beyond 15) | 160 | 12 | to where the target stood, at most 18 blocks; the landing is a ring of 4 |
| Power Blast | Power | 22 | 8 | 3 | 120 | 20 + splash 8 (r 3) | a beam 30 long and 1.2 wide for 6 ticks; a block or Chaos Shield (costs 20) stops it; Stormbreaker cuts it |
| Overload | Power | 30 | 8 | 2.5 | 200 | 16 per sigil | sigils (radius 3) under up to 3 players in sight, fixed at tick 12, detonate at 30 (9 up, 1 down); a crater of radius 2 only with `blockDamage` |
| Titan Shockwave | Power | 20 | 16 | 2 | 240 | 12 | a ring to 9 blocks in 10 ticks; whoever's feet are more than 0.5 above the ground is spared |
| Power Pulse | Power | 40 | 8 | reactive | 80 | 12 | a shot through the walls at a fighter he can neither see nor walk to for `unreachableTicks` (160); a Chaos Shield costs 12 |
| Portal Step | Space | 16 | 14 | 3 | 160 | none | he appears behind the target (6 to 40 blocks away) and a Jab or Sweep follows at once; a Hex wall refuses it |
| Meteor Hail | Space | 30 | 40 | 2.5 | 440 | 16 each | a ring under every player and `meteorExtra` (4) random ones, at most 10; each falls 24 blocks in 24 ticks and bursts in radius 2.5; a Chaos Shield costs 16 |
| Gravity Pull | Space | 24 | 26 | 2 | 360 | 10 crush | draws everyone in sight (4 to 22 blocks) at 0.55 blocks a tick for 16 ticks, then crushes radius 3.5 |
| Space Barrier | Space | 10 | 6 | reactive | 300 | none | a dome for `barrierTicks` (60) that cancels ranged damage (arrows, daggers, Thor's lightning) but not that born of a stone (the Chaos Beam, Vision's beam, Stormbreaker) |
| Reality Veil | Reality | 12 | 6 | reactive | 280 | none | for `veilTicks` (120) projectiles within 12 blocks heading at him turn to bubbles, except those of the tag `reality_proof` |
| Phantoms | Reality | 24 | 10 | 1.5 | 560 | 3.5 each | three illusions of one health that walk at the players; the real one is invisible for 80 ticks, shown for 40 by any hit on him |
| Petrify Orb | Reality | 14 | 14 | 2.5 | 240 | none | an orb at 0.55 blocks a tick; the first creature it meets is petrified for 60 ticks (cannot move, takes half damage); the shield costs 2 |
| Soul Sight | Soul | 10 | 6 | reactive | 240 | none | dissolves Loki's duplicates within 40 blocks, ends invisibility and shapeshifts |
| Soul Drain | Soul | 26 | 86 | 3 | 320 | 3 every 10 ticks | chains to up to 2 players in sight; ignores armour and enchantments; he heals `drainHealFraction` (0.4) of it; snaps at no line of sight, 28 blocks, or 60 poise |
| Soul World | Soul | 30 | 8 | 1 | 800 | none | a pillar of radius 3 under a player (fixed at tick 10); whoever is in it at 30 is in the Soul World for 80 ticks (nothing hurts them), he heals 3 percent of his maximum for each |
| Rewind | Time | 30 | 8 | reactive | 600 | none | returns up to `rewindMaxHeal` (8 percent) of health, the plates and his place of `rewindSeconds` (6) ago; fails if he is staggered |
| Time Snare | Time | 24 | 8 | 2.5 | 360 | none | discs under 2 players (fixed at tick 10); inside at 24: `TIME_LOCKED` for 80 ticks (−60 percent speed) |
| Infinity Beam | Time | 50 | 36 | reactive | 900 | 40 | with five stones only: a beam 40 long and 3 wide sweeping 60 degrees in 24 ticks, once per creature; a block stops it, a Chaos Shield costs 40; Stormbreaker cuts it |

### 15.6 Defence, plates and poise

`ThanosDefence.damageTaken` takes the steps in this order: nothing while arriving or dying unless the damage bypasses
invulnerability; a creature that is not a player does `mobDamageFactor` (0.25); the Space Barrier cancels ranged damage that is
not stone-born; each intact plate takes `plateReduction` (10 percent) off; the surge multiplies by 1.5 and a stagger or a
dark Gauntlet by 1.25; Stormbreaker by `stormbreakerFactor` (2.5); no hit takes more than `maxHitFraction` (6 percent) of his
maximum and no second more than `maxDamagePerSecond` (15 percent).

* **Plates:** right pauldron below 75 percent of health, left pauldron below 55, breastplate below 35, helmet below 20.
  Mjolnir breaks the helmet if its blow lands; `plateTearSeconds` (3) of continuous Chaos Beam tear a plate; a Telekinetic
  Slam tears one (at most once in 120 ticks); a Rewind restores them.
* **Poise:** a clench is the wind-up of any move of a stone. Blows during it add `poiseHit` (2) per point of damage taken
  (three times for `stone_born` damage: the Chaos Beam, the Mind Stone beam and Stormbreaker); at `staggerPoise` (100) the move breaks and he is **staggered** for 40 ticks and his
  Gauntlet is dark for `lockTicks` (80): only the sword and fist moves. Soul Drain breaks at 60 poise, the Choke at 60
  damage. `lockGauntlet` (Spider-Man's web: `webLockTicks` 40, at most once in `webLockCooldown` 400) does the same to a
  clench in progress; at any other time it only locks the Gauntlet for those 40 ticks (no stone move, a quarter more damage).
* **A mob's blow** (a golem, a wolf) does a quarter, and any creature that hits him (a golem, a wolf, a tamed pet) is swatted dead with the back of his hand. The Vision of Wanda's Hex is not a mob here: he hits at full strength and is not swatted (`ThanosEntity.hurt`).

### 15.7 Targeting and movement

He targets a **Vision first**, then whoever dealt him the most in the last 10 seconds, then the nearest; never a Creative or
Spectator player, a dead one, or one in the Soul World; he keeps a target 60 ticks, and until he has the Soul Stone he goes for
the nearest of Loki's copies. He walks like a creature of 1.2 × 3.3 blocks with a step of 1.4, ignores cobwebs and powder snow,
and has these rules: the **stuck rule** (no progress toward a target 4.5 to 40 blocks away for 80 ticks: he leaps at him, twice at
most, then is moved to the nearest free ground beside him with a purple flash); the **unreachable rule** (the Power Pulse);
the clock needs a fighter inside `arenaRadius + 24`.

### 15.8 What he is immune to, and how the other heroes meet him

* **Immune:** knockback, fire, lava, drowning, falls, suffocation, cactus, lightning, Wither, Hunger, starvation, magic
  without a source; effects: Wither, Poison, Hunger, Nausea, Levitation, Mining Fatigue, Weakness, `PETRIFIED`, `SOULWORLD`,
  `TIME_LOCKED`, and Slowness above level II.
* **Tags:** he is in `bosses` (so webs and the Instant Kill do not take hold of him: a web reaches the Gauntlet through a hook
  instead), `hex_resistant` (the wall bounces him in and out), `pin_immune`, `telekinesis_immune`, `shapeshift_immune`,
  `spellbound_immune` (the Freeze, the Petrify, the Time Lock), `confused_immune` and `telepathy_immune`; **(3.1)** he is no longer in
  `telekinesis_immune`: he is held as a boss, and he and his sword, meteors and orb are in `pull_immune` (the villain heroes' own pulls
  work as in 3.0); his sword, meteors and orb are in `shapeshift_immune` and `reality_proof`; a phantom is in `shapeshift_immune` and `boss_minions`
  (Wanda can lift one).
* **Hooks (`BossHooks`):** Quicksilver Time slows him to one half and not at all with the Time Stone; **(3.1)** Magneto's strip or
  pull wrenches off one plate (once in 6 s, shared with slams; never while he arrives or dies); Spider-Man's web locks the Gauntlet; Thor's lightning is a hook of the framework.
* **Telekinesis (3.1).** Held only as a boss: `holdTicks` 40 (2 s; Billy 1 s, Magneto 3 s), then 12 s before anyone can hold him
  again; never while he arrives, leaves, dies or stands in his Space Barrier. Billy, the Phoenix and Magneto catch his **sword**
  (it tears free after `swordHoldTicks` 40, from Magneto after 60, and flies home; thrown, it strikes him), and Billy and the Phoenix his
  meteors and orbs (still in the grip; thrown back they are the thrower's: the meteor bursts as a slam and breaks no block, the
  orb staggers him). His arena is not protected, so block telekinesis lifts real blocks there, but Build Magic and the Magnetic Pull
  change nothing in it while he fights (`build_magic.allArenasProtected`). He never goes more than 24 blocks below where he was
  called: dragged down a valley, he steps back once through a portal.
* **Wanda:** the Chaos Beam tears plates; **a Telekinetic Slam by any player** (Wanda's, Billy's or the Phoenix's) tears one too; her shield stops everything of his that is a projectile or a
  beam. **Vision** is what he hunts first. **Loki's** duplicates lure him until the Soul Stone. **Thor:** Mjolnir breaks his
  helmet, Stormbreaker does 2.5 times and cuts a beam. **The Phoenix:** her Mind Control and Psionic Field do not take hold of
  him (`telepathy_immune`, `spellbound_immune`), and the Burst's damage lands (it is not fire).

### 15.9 Anti-cheese

A pillar, a hole, a bunker or a boat: the unreachable rule and the Pull, the Step and the Meteors that reach anywhere in the
arena. Sniping from outside: the clock needs a fighter inside `arenaRadius + 24`. Standing out of the arena: no fighter near him
for `abandonSeconds` (90) and he leaves. Everyone dead for `wipeSeconds` (15): he leaves. The world is never changed.

### 15.10 The end and the reward

* **He falls.** A blow that would kill leaves him one health and he kneels for 100 ticks, invulnerable, says his last words at
  tick 40 and turns to dust from tick 60; the fight is won at once. The experience (`xpReward`, 12,000) falls where he stood.
* **The reward** (`BossRewards`): each participant who was there `rewardMinSeconds` and did at least `rewardMinShare` (3 percent)
  of his maximum health, and whoever struck the last blow, gets **one Infinity Gauntlet** and `xpRewardPerPlayer` points. A
  Creative player does not, unless `[boss].creativeGetsReward` is on. It goes into the inventory; a full one drops it for that player alone; a dead or absent player's
  waits as a gift. The rest is `summonCooldownTicks` (12,000: ten minutes).
* **Any other end:** nobody is rewarded, and he steps through a ring of Space (30 ticks; when the server stops he is simply gone).
  After a wipe or `abandonSeconds` the rest is half as long (6,000: five minutes); after an operator's stop, Peaceful,
  `maxFightMinutes` or a kill from outside there is no rest at all.
* **The Gauntlet** (`InfinityGauntletItem`) is held for `gauntletRitualTicks` (60) by a player with no power to become Thanos
  (15.11); a hero who tries is told so and nothing is used up; in Creative it is kept.

### 15.11 The playable Thanos

> "Dread it. Run from it. Destiny arrives all the same." Whoever holds the Infinity Gauntlet for `gauntletRitualTicks`
> (60) with no power becomes Thanos: the five stones the Mad Titan held, one on each key, **no Mind Stone and no Snap**.
> He does not fly. He is a tank with a toolbox. Its code is `ability/ThanosAbilities` (the keys, the cooldowns, what lasts
> from tick to tick), hung on the `HeroServer` plug-in `ThanosServer`; what the stones do in the world is the boss's
> `thanos/StonePowers` (caster-agnostic: a beam, a ring, a splash, the Chaos Shield asked wherever a beam crosses it); the
> three stone effects are `effect/StoneEffect`; the events are `thanos/ThanosEvents`. Every number is `[thanos]` in the
> server config.

#### 15.11.1 The Gauntlet

An `AwakeningItem`: hold right-click for `gauntletRitualTicks`. While it is held the five stones light **one after the
other** (Power, Space, Reality, Soul, Time), each with its voice, and a little of its colour rises round the hand; when
the hold is done a **visual-only bolt** strikes where he stands (it hurts nobody) and `Powers.awaken` makes him Thanos in
the Infinity War look, with the usual title, burst and announcement. A hero who tries is told so and nothing is used up;
in Creative it is kept. The Gauntlet is in `cannot_be_snatched` and can be given to a friend.

#### 15.11.2 The keys

An ability that cannot do anything (no target, no room, a wall in the way, PvP off) says why and starts no cooldown. The
two on a key **share one cooldown slot** (the client tells them apart by its length). Hunger is exhaustion (4.0 = one food
point). With an empty hunger bar the stones do not answer ("Too hungry for the stones"); in Creative they cost nothing.

| Key | Stone | Press | Sneak + press | Cooldown (ticks) | Exhaustion |
|---|---|---|---|---|---|
| **R** | Power | **Power Blast**: a violet beam from the Gauntlet to the first creature or block (28 blocks): 12 `stone_power` and a shove; a burst of 2.5 blocks where it ends: 6 `stone_blast` to the hostile ones (the aimed creature excluded); no block is ever broken | **Titan Slam**: a ring from his feet at 0.9 blocks a tick to 7 blocks: 10 `stone_blast` once to whatever stands on the ground, thrown outward; a jump clears it | 70 / 160 | 2.0 / 4.0 |
| **G** | Space | **Portal Step**: onto the block he looks at, or right behind the creature (40 blocks), through two rings; the Hex, Mjolnir's pin and a boss's arena refuse it, and it needs room for his whole body and no water | **Space Pull**: the creature he looks at (24 blocks) is drawn over 6 ticks to two blocks ahead of him and dazed (Slowness II, 20 ticks); not a boss, a pinned creature or an armour stand; a player only with PvP; the Hex judges the landing | 100 / 220 | 1.0 / 2.0 |
| **V** | Reality | **Transmute**: a sphere flies 6 ticks to the creature (20 blocks) and it is stone (`PETRIFIED`) for 60: it cannot move, jump, attack or use anything and takes half damage; a boss shakes it off; a Chaos Shield stops the sphere and loses 2; a Loki's duplicate dissolves | **Reality Veil** (toggle, `Toggle.REALITY_VEIL`, saved): a projectile heading for him within 8 blocks is a bubble that pops (not Mjolnir, Stormbreaker or the stones' own; not his own); 0.01 exhaustion a tick; off at an empty bar | 240 / none | 2.0 / 0.01 a tick |
| **B** | Soul | **Soul Drain** (hold, `Live.SOUL_DRAINING`): he locks onto the creature (16 blocks) and every 10 ticks it takes 3 `stone_soul` (armour and enchantments ignored) and he heals half of what it lost; ends when the key is let go, a block comes between, it is past 20 blocks or dies, he starves or is held, or he takes 60 damage in a second; a Chaos Shield stops the chain and loses 12 | **Soul Sight** (`Live.SOUL_SIGHT`, 160 ticks): the client outlines the creatures within 32 blocks through walls; Loki's duplicates dissolve, a hidden Loki is seen, a shapeshifted Loki is himself again (`sightBreaksIllusions`); whoever is invisible glows for everyone for 5 seconds | 40 after it ends / 400 (none in Creative) | 0.02 a tick / 1.0 |
| **N** | Time | **Rewind**: back to where he was 5 seconds ago, his health up to what it was then (at most 20 more), the harmful effects that began in the window gone, the fire out; needs 2 seconds of history; a Hex or a boss's arena refuses the place (the rest still happens), Mjolnir's pin refuses it all | **Time Lock**: a dome of 6 blocks at his feet for 100 ticks: `TIME_LOCKED` on what is inside (not him, not his pets, a player only with PvP, never Creative); projectiles lose 65 percent of their speed a tick and fall under no gravity | 900 / 600 | 8.0 / 3.0 |

While he is held (`SPELLBOUND`, `PETRIFIED`, `SOULWORLD`) no key answers ("The stones will not answer while you are
held"): the film's flaw that a restrained Gauntlet hand cannot snap. A key's **release** always goes through.

#### 15.11.3 The body

Permanent attribute modifiers under `enhanced:thanos_*`, applied by `applyBody` to whoever is Thanos and taken off
everyone else: `healthBonus` (+20), `armorBonus` (+6), `toughnessBonus` (+2), `knockbackResistance` (+0.5),
`attackBonus` (+3), `scale` (+10 percent, `ADD_MULTIPLIED_BASE`), `reachBonus` (+1 `entity_interaction_range`),
`stepBonus` (+0.4), and half the fall damage. The **Titan healing factor**: `regenPerSecond` (0.5) after `regenDelayTicks`
(100) without being hurt (`ThanosEvents.onHeroHurt` tells him), for 0.01 exhaustion a tick, none when starving. Nothing
else is passive, and `Power.THANOS.canFly()` is false.

#### 15.11.4 What lasts, and what ends it

The server state is `ThanosAbilities`' map by player UUID: the history (a snapshot every 5 ticks for `rewindSeconds`),
the damage of the last second, the creatures he turned to stone, the sight's end, and the drain, the sphere, the vortex,
the ring and the dome. It ends with **his power** (`endAll`: the body comes off, the veil drops), **his life**, **his
session** and **his dimension** (`gone`: the dome opens and the projectiles it hung fall, the creatures it held are free,
the stone he made is flesh, the drain lets go, the history is dropped), and **the server** (`onServerStopped`). A hold
released (`stopHold`: a screen, a death) lets the chain go and his next tick finishes it.

#### 15.11.5 The other heroes and the rules

* **Wanda:** the Chaos Shield stops the Blast (12), the Drain (12) and the Transmute (2) at its surface and takes three
  quarters of the Slam as an explosion; the Hex bounces him like any hero and judges the Step, the Pull and the Rewind as
  teleports (`HexManager.onTeleport`).
* **Thor:** Mjolnir's pin refuses the Step, the Pull of the pinned and the Rewind (a pinned creature is not shoved either);
  the Blast and the Drain still work.
* **Billy, the Phoenix:** a Freeze, a Mind Control or a Psionic Field (all `SPELLBOUND`) holds his hand. **Apocalypse:** in a cocoon
  of sand (`ENTOMBED`), `AbilityHandler` drops his presses. **Loki:** the Soul Sight is the answer to the duplicates, the hiding and the shape.
* **Vision:** phased through everything he cannot be touched. **Magneto:** the Gauntlet is a model, not an item: nothing to
  take. **Quicksilver Time:** slows him like anyone; the Rewind and the Lock run on the game clock.
* **PvP off:** the Blast, Slam, Pull, Transmute, Drain and Lock do nothing to players and say so; Creative and Spectator
  players are never touched.
* **The Mad Titan:** the hero's Blast is a beam and the Space Barrier turns it away; the Drain is no projectile and passes
  it; the damage counts in the ledger; a boss cannot be pulled, turned to stone or locked (the tags) but is hurt by both (Apocalypse's golden shield
  turns both away while it is up: they are not mind power).
* **Blocks:** nothing of his changes a block, whatever the rules say.

#### 15.11.6 Tests

`ThanosTests` (the body and healing factor, the Blast, Slam, Step, Pull, Transmute and Veil, the refusals when held, hungry,
in Creative or as a Spectator, the looks) and `ThanosSoulTimeTests` (the Drain and the Sight, the Rewind and the Lock, what
ends with him, the Gauntlet's ceremony, and the hero beside the boss). The state-heavy tests have a batch each
(`thanos_hero_*`). Deliberate bugs in each important rule (the shield check of each stone, the Hex and the pin, the heal
and its cap, the history, the dome's end, the hold, the hunger) were each caught by the test meant for them.

### 15.12 How the playing Thanos moves

> The animation of the hero is **client only** (the server sends nothing for it) and is built on the mod's animation system
> in `client/render`: a state per hero in view, layers blended by eased weights (locomotion, ambient stances, aimed stances,
> one-shot gestures), `GestureId` (the timing), `Gestures` (the keys), `Stances` (the rows), `GestureRoutes` (which change in
> the synced data stands for which gesture), `GestureTable` (the edges) and `FirstPersonArms`. Thanos adds to it and changes none of
> its rules. Every row is a raw model angle in degrees for a right-handed player, and a left-handed one is mirrored by the
> pipeline. `M` is the main arm; **`O` is the arm the Gauntlet is on** (the off arm), so every gesture of his drives `O`. His
> plug-in is `ThanosClient`; the keys of the gestures are `ThanosGestures`, the rows of the stances `ThanosPoses`, the client's
> own part (the key press, the landing, the first-person hand) `ThanosActing` and the flourishes at the hand `ThanosGestureFx`.
> The boss is a model of his own and does not use this pipeline (15.14.4).

#### 15.12.1 The gestures

One gesture of the group `CAST` plays at a time. "Force" means the gesture replaces the swing of a punch on the arms it holds.

| Gesture | Ticks (total / attack / release) | Force | Started by | What it looks like |
|---|---|---|---|---|
| `THANOS_BLAST` | 10 / 1 / 5 | yes | R's cooldown starts, nearer to the Blast's | The Gauntlet is thrust at the aim and holds, trembling; the other arm balances; the chest turns with the arm |
| `THANOS_SLAM` | 22 / 2 / 8 | yes | R's cooldown starts, nearer to the Slam's | The Gauntlet goes overhead; at tick 12 it is driven down to the ground, he leans in and sinks 0.24 blocks, the legs forward |
| `THANOS_STEP` | 12 / 1 / 5 | no | G's cooldown starts, nearer to the Step's | The Gauntlet draws a circle in the air, a blue ring |
| `THANOS_PULL` | 16 / 2 / 6 | yes | G's cooldown starts, nearer to the Pull's | The palm out level, then dragged back; he leans away |
| `THANOS_WARP` | 14 / 1 / 5 | yes | V's cooldown starts (the Transmute) | A flick of the Gauntlet at the aim, fingers spread, trembling |
| `THANOS_VEIL_ON` / `THANOS_VEIL_OFF` | 16 / 2 / 6 and 10 / 1 / 4 | no | `Toggle.REALITY_VEIL` flips | Both arms out, closing round him into the Veil's stance; the opposite when it drops |
| `THANOS_DRAIN_CAST` | 12 / 1 / 5 | yes | `Live.SOUL_DRAINING` rises | The chain lashes out, and the gesture hands over to the Drain's stance |
| `THANOS_SIGHT` | 14 / 2 / 5 | no | `Live.SOUL_SIGHT` rises | The Gauntlet to the eye, then swept across the face |
| `THANOS_REWIND` | 22 / 2 / 8 | no | N's cooldown starts, nearer to the Rewind's | The palm up and out, trembling; drawn back, he leans away |
| `THANOS_TIMELOCK` | 20 / 2 / 8 | no | N's cooldown starts, nearer to the Lock's | Both palms up and then pressing down in a stamp; the body sinks with it |

**Which of two uses of a key it is.** The two uses of R, G and N start cooldowns of different lengths (70 and 160, 100 and
220, 900 and 600 by default). When the data arrives the ticks left tell them apart: the use whose **configured cooldown** is
nearer plays (`ThanosGestures.cooldown`; the client has the server's config), so changing a cooldown in the server config
changes nothing for the animation, and a tie plays the first. The Soul Stone's two uses need no such rule: the drain and the
sight have states of their own (`Live`), which also covers the Sight in Creative, where it starts no cooldown. The Veil is a
toggle. The Transmute is the only use of V that has a cooldown.

**Before the server answers.** The local Thanos's own key press starts the gesture at once where the client can tell the stone
will work: not while he is held (`SPELLBOUND`, `PETRIFIED`, `SOULWORLD`), starving or a spectator; the Step only with a block or a
creature in range, the Pull, the Transmute and the Drain only with a creature in his sight (and the Drain not while it is
already draining), the Rewind only with two seconds of history, the Veil, the Sight, the Lock, the Blast and the Slam whenever
they are rested. A refusal the client cannot see (a Hex wall, no room to stand, PvP) lets the gesture play out and costs nothing.
The change in the data that arrives a moment later is swallowed, so the gesture does not play twice.

#### 15.12.2 The stances

| Stance | tauIn, tauOut | Aimed | On while | Rows |
|---|---|---|---|---|
| `THANOS_VEIL` | 4, 5 | no | `Toggle.REALITY_VEIL` | The main arm spread 10 degrees more than whatever it is doing (so he can walk and fight with the Veil up), the Gauntlet hand held out and up (-30, 0, -22) as if it held a thin film, trembling 0.8 at 1.6, the head back 2 degrees, a slow sway of 1.5 degrees |
| `THANOS_DRAIN` | 2, 4 | yes | `Live.SOUL_DRAINING` | The Gauntlet arm along his look (the one he drains is where he looks), trembling 1.4 at 1.9; the main arm braced back and out (-12, 0, 8); the head down 3; the legs braced wide; the body 2 degrees back; the Gauntlet arm counts as occupied |

A gesture that hands over to a stance ends on its pose, so that nothing pops: `THANOS_VEIL_ON` ends with the Gauntlet arm at the
Veil's and the main arm level with the body, spread by the stance's own angle; `THANOS_DRAIN_CAST` ends with the Gauntlet aimed
at the look with no offset and gives the lean back to the stance. `ThanosGestures` holds the numbers both read.

#### 15.12.3 Walking, landing, no flight

* **No flight profile** (`FlightProfile.of` answers null for him): no take-off, banking or braking, and the animator never starts
  a flight gesture.
* **Walk and sprint** (`RunPoses.THANOS`): heavy and wide, between Thor and Magneto: the arms swing 0.9 of the walk and 1.15
  of the sprint, the legs 1.15, a lean of 9 degrees and a turn of 5 at a sprint, and a bob of 0.03 blocks that is the stride
  shaking the ground. No speedster cycle: his speed is human.
* **Landing.** The animator lands only the heroes who fly, so `ThanosActing.edges` lands a Titan: a fall of more than three blocks
  (the speed just before the ground is 0.6 blocks a tick or more, `ThanosGestures.HARD_LANDING`) plays the soft landing with a dip
  deeper than anyone's (the dip of Thor's soft landing) and the dust and ring of the ground. A short fall is nothing.

#### 15.12.4 First person

The Gauntlet is on the off hand, which vanilla draws only when something moves it: for Thanos the off hand is **always drawn**,
at the lower left of the screen, with the Gauntlet on it (the `PlayerRendererMixin` hook, 15.13.2), breathing a little. Every row
is added to that rest, in a right hand's signs (the left hand mirrors), and stays at the edge of the screen but for the thrust of
the Blast (four ticks) and the stamp of the Time Lock:

| Gesture or stance | The Gauntlet hand |
|---|---|
| `THANOS_BLAST` | thrust forward and up a little (dz +0.22) for four ticks, then a kick back |
| `THANOS_SLAM` | up for six ticks (dy +0.30), then down hard (dy -0.25, dz +0.10, rx +25) |
| `THANOS_STEP` | a circle over the twelve ticks (dx 0.08, dy 0.06) |
| `THANOS_PULL` | out (dz +0.20), then drawn back (dz -0.06) |
| `THANOS_WARP` | a flick forward (dz +0.18) |
| `THANOS_VEIL_ON` / `OFF` | both hands come up a little and gather in, or open out |
| `THANOS_DRAIN_CAST`, the Drain | lashed out, then held out along the look, trembling (dx +0.10, dy +0.12, dz +0.16, rx -10) |
| `THANOS_SIGHT` | the hand rises at the edge of the screen, toward the eye |
| `THANOS_REWIND` | the palm up (rz +25), trembling, then drawn back |
| `THANOS_TIMELOCK` | both hands pressed down (dy -0.10, dz +0.10) |

The beam, the chain and the sparks leave the Gauntlet where it is drawn: the tracked hand when he is seen by someone else, and in
first person the hand's resting place moved by the same offset the hand is drawn with. The camera is never touched, and with
*Reduced motion* every tremble is off.

#### 15.12.5 The flourishes

Each gesture starts a small effect at the places the model really has (`HandTracker`), thinned by the client's *Flight effects* option
and only for a Thanos in view: a violet flare and spiral at the Gauntlet as the Blast leaves it, grey chips and a dust ring at his
feet for the Slam, motes hanging where the Step's hand has been and a blue ring at the end of the circle, a blue spiral drawn in
to the palm of the Pull, red sparks at the fingers of the Transmute, red motes round the hand that closes the Veil, orange motes at
the eyes of the Sight, two green rings on the forearm of the Rewind and a quick green mandala under the feet of the Time Lock.

#### 15.12.6 Tests, and how it was checked

`ThanosAnimationTests` (batch `thanos_anim`): the timing of the eleven gestures; that every one drives the Gauntlet arm and the other only
balances or joins in; that no key asks an arm for an impossible angle; what each gesture looks like in numbers; the hand-overs to the
stances; the routes (every ability, toggle and state of his acted or explained, none pending); that the cooldown of each use of
a key reaches its own gesture with the config's numbers and a few ticks late, and follows the config; that every gesture can be reached
by something; every key press of the prediction; that he has no flight profile; and the speed that makes a landing a Titan's.
The shared checks cover him too: `AnimationMathTests` (every gesture has well-formed keys and the Veil's closing ends on its stance,
every ability, toggle and state is acted or explained, no gesture is started by nothing). What the poses, the hands and the flourishes
look like was checked by eye in a real game, from a second player (`/enhanced_anim target`, `play <gesture> <tick>`, which freeze each
pose at a tick) and in first person.

### 15.13 Looks and art

Everything of him that is a picture. Every texture is drawn pixel by pixel by a script (no sample, no traced image), so
that it can be drawn again; the scripts are in `scratchpad/art/thanos/` of the 3.0 work (`gen_skins.py`, `gen_boss.py`,
`gen_items.py`, `gen_effects.py`) and the textures in `assets/enhanced/textures/`. `ThanosAssetsTests` (batch
`thanos_assets`) reads the files back and holds them to the rules below.

#### 15.13.1 The two skins and "your own skin"

The playable Thanos has three looks (`H` cycles them, `Sneak + H` goes back): **Infinity War** (the default),
**Endgame: Battle Armor** and **Your own skin** (the player's own, with the Gauntlet and the powers). The two skins are
64x64 with the classic four-pixel arm (`entity/suit/thanos_infinity_war.png`, `thanos_endgame.png`). Armour is hidden on a
hero, so everything he wears is painted into the skin and what stands off the body is on the overlay layers.

| | Infinity War (after the Statesman) | Endgame (the battle armour of 2014) |
|---|---|---|
| Face | purple and bald, a heavy brow, blue irises, a furrowed grey beard in vertical stripes (the great chin stands off the face on the hat layer) | the same face, open |
| Head | bareheaded | a gold helmet on the hat layer: a bright crest down the middle, a brow band with a point between the eyes, cheek guards and a neck guard |
| Body | a navy quilted tunic with blue piping, a gold gorget that makes a Y over the chest, a gold belt with an angular buckle | a plum breastplate with gold trim and a raised ridge, a thick gold gorget, the belt |
| Arms | bare purple, a bicep, veins on the forearm, gold straps over the shoulders | bare purple under great gold pauldrons |
| Legs | olive trousers, gold knee plates, dark boots | olive trousers, gold knee plates and greaves, dark boots |

* **Palette** (lifted from the posters, which are dark, so that eight pixels read): skin `#8C7DB4` with a ramp from `#CDC1EA` to
  `#3C3258`; beard `#7A7490`; iris `#5C9CE4`, pupil `#1E1830`; aged brass `#A08540` (light `#E2CA80`, dark `#614D26`); navy
  `#222C62`, piping `#4266D8`; olive `#4A4528`; boots `#2F2A1E`; plum `#5A3F7E`. Light comes from the top left; the metal has a lit
  top and left edge, a dark bottom and right edge and a few scratches; the cloth is quilted and dull.
* **The rules** (the shared skin rules, tested): nothing is drawn outside the rectangles the player model reads; the base layer is
  opaque; an overlay pixel is fully opaque or fully clear; the arm is the classic width. **The eyes** are on the columns 9 to 10 and 13 to 14
  of the head front, rows 11 to 12, a pupil outside and an iris inside, and the Endgame helmet leaves those eight pixels and the
  face clear, so that a glow mask over the eyes works on every look.
* **Drawn in at least three rounds each** with the mod's skin kit (`skinkit.py`, a character grid per face and a colour ramp per material) and the
  box renderer of the art tools, each round looked at from the front, back, sides, three quarters, from above and at the sizes a
  player is seen at in the game, and compared with the stills of the films (the *Infinity War* poster, the *Endgame* helmet and
  shoulders).

#### 15.13.2 The Infinity Gauntlet

**On the arm.** A model layer (`GauntletLayer`) draws a cuff, an engraved forearm and a fist on the off-hand arm of every
playing Thanos, whatever his look, scaled by 0.6 to fit a player's arm; in first person the same cuboids are put on the hand by one
injection into the renderer of the hand. They are cut from the boss's own texture (15.13.3), so the Gauntlet of the hero and
of the boss are one drawing: aged brass with a lozenge on the back of the forearm of nested channels, a thick cuff, a back of the
hand with a large round socket.

**The six sockets** are drawn by code on the back of the hand, a row of four on the knuckles and one at the thumb:

| Stone | Place | Lit colour | Core |
|---|---|---|---|
| Power | knuckle, index | `#9B4DEB` | `#E6D2FF` |
| Space | knuckle, middle | `#2F9BFF` | `#BFE6FF` |
| Reality | knuckle, ring | `#E5253B` | `#FFB3BB` |
| Soul | knuckle, little finger | `#FF8A1E` | `#FFD6A0` |
| Time | the thumb | `#35D46A` | `#B8FFC9` |
| Mind (never lit) | the large round socket, 3 by 3 pixels | a thin yellow outline `#F5D77A` | none |

A stone that is not burning is a dark glassy gem (`#15131C`); a burning one is drawn full bright with a halo. A stone he holds
**glows softly**, **flares for eight ticks when it is used** and burns while a held effect lasts; the stone that arrives next
pulses in its last ten seconds. The engraved lines of the cuff and forearm glow in the colour of the stone he used last
(`entity/thanos_glow.png`, white on clear, tinted by code). At five stones the Mind socket glimmers yellow for a second every
twenty. The Gauntlet is hidden while the player is invisible, hidden or shapeshifted.

**The item** (`item/infinity_gauntlet.png`, 16x64, four frames of 16x16, `frametime` 3, interpolated): the Gauntlet seen from the back
of the hand, aged brass with a dark outline, four gems on the knuckles, the Time gem at the thumb and the large dark socket; the gems
pulse in turn over the four frames (the Power and Time gems, then Space, Reality, Soul) and a yellow glint comes and goes in the socket.

#### 15.13.3 The Mad Titan's model and texture

A custom humanoid (`ThanosModel`), not a scaled player: 34 pixels tall at a render scale of 1.55 (3.29 blocks), with a hitbox of
1.2 by 3.3 and an eye height of 2.95. His breadth is what he is: shoulders twice a player's, a big head and a long arm. It has
28 boxes: the head with the beard, the **helmet** (crown, crest, two cheek guards, a neck guard), the body with the gorget, the
**breastplate**, the belt and its buckle, two arms with the two **pauldrons** (a top and a low plate each) and the dark leather bracer of the
sword arm, the Gauntlet's cuff, forearm and fist, the legs with knee plates and boots. The four plates (the helmet, the two pauldrons,
the breastplate) are hidden when they break (the synced `plates`), and what shows then is the **undersuit**: the bald purple head, bare purple
arms and the navy quilted tunic with its piping.

* **The texture** (`entity/thanos.png`, 128x128) is cut for the model's boxes at their `texOffs`: the Endgame armour (scratched gold with a lit
  edge and a dark edge, rivets on the pauldrons, the neck guard in three plates) over the Infinity War undersuit, the breastplate plum with a
  gold border and a raised ridge, the face of 15.13.1 stretched to the head's nine pixels (the brow, the eyes, a frown), the beard
  furrowed, the belt dark leather with gold studs. Every face of every box is painted in full. **One face is clear on purpose**: the breastplate's back, which lies
  in the plane of the body's back (two opaque faces in one plane flicker); the body's back is plum armour with a gold border.
* **The glow** (`entity/thanos_glow.png`) is white on clear: the lattice of the engraved lozenge, the channels of the forearm and the
  bands of the cuff, and nothing else (no pixel of it lies on a pixel the skin leaves clear).
* **His illusions** use the same model and texture (with the plates and stones of the real one), and have no shadow.
* **The sword** (`ThanosSwordModel`, `entity/thanos_sword.png`, 64x32): two broad flat blades back to back on a short grip, a bronze
  guard in the middle, a groove down each blade with a bronze inlay, a ridge (the spine) behind each; steel `#666678`, bronze
  `#A08540`, wrapped dark leather on the grip. It is the model in his hand and the entity in flight.
* **The meteor** (`ThanosMeteorEntity`) is a tumbling cluster of cubes that are all `item/titan_debris.png` (16x16): slate grey-blue rock with the
  Space Stone's blue running through its cracks, lit from within; a blue halo and a streak follow it. **The orb** is a translucent red sphere
  with a pale heart and four motes circling it, made of energy shapes, so it has no texture.
* `/enhanced_model thanos <state or move> [tick]` shows the model in any state, frozen at a tick (his Sweep eight ticks in is
  `/enhanced_model thanos sword_sweep 8`), for the people who draw.

#### 15.13.4 Energy colours

What every effect of the fight is painted with, whatever the look: **Power** `#9B4DEB` (core `#E6D2FF`), **Space** `#2F9BFF` (`#BFE6FF`; the
portals use the Tesseract's own blues), **Reality** `#E5253B` (`#FFB3BB`), **Soul** `#FF8A1E` (`#FFD6A0`), **Time** `#35D46A`
(`#B8FFC9`), **Mind** `#F5D77A`, only the empty socket's outline. The Infinity Beam is white with the five colours as a banded halo.
The boss bar takes the colour of his newest stone (purple, blue, red, yellow for orange, green).

#### 15.13.5 The other pictures

| File | What |
|---|---|
| `mob_effect/petrified.png` | 18x18: a grey cracked stone with the Reality Stone's red in the cracks |
| `mob_effect/soulworld.png` | 18x18: an orange sun on a horizon over still water |
| `mob_effect/time_locked.png` | 18x18: a green dial with ticks inside a mandala ring |
| `effect/stone_mandala.png` | 128x128, white on clear, tinted by code: a double outer ring, a band of twelve runes, a six-pointed star, clock hands (two of them are drawn, turning against each other, under the Time Stone's discs and mandalas) |
| `effect/portal_ring.png` | 128x128, white on clear: a thick ring with a lit core and a bright edge, a ring of sparks, a faint three-armed spiral (the portals of the Space Stone and the sky portal of the call) |
| `item/titan_debris.png` | 16x16, the meteor's rock |

The boss bar and the chains of the Soul Drain are drawn by code (the bar from rectangles and the sockets' own geometry, the chain from ribbons),
so they have no picture of their own.

#### 15.13.6 How it was checked

`ThanosAssetsTests` read the files back (the skin rules, the eyes, a painted face on every box, the animated strip). The pictures were then looked at **in a real game** (a
server and a client, `/enhanced_model` for every state, the playing Thanos in both looks in the third person and the first, the Gauntlet item in the hotbar, the meteor, and the
fight itself with the bar, the air and the subtitles). That round found one thing: in the Dying pose his legs come up and the soles of the boots, drawn nearly black, showed as a black slab; they are
now worn leather with a welt and a tread, and drawn light because the game shades every downward face by half. The call's subtitle (*Dread it. Run from it. Destiny arrives all the same.*, as the specification gave it) was
wider than a window at GUI scale 2 and was clipped at its ends; the review of the lane (15.16) shortened it, and the one of the awakening, to
*Destiny arrives all the same.*, which fits at every scale, and the title of the reward from *THE GAUNTLET IS YOURS* to *YOUR GAUNTLET*, which was
clipped at the default scale of a 1080p window too; nothing else was out of place.

### 15.14 What is shown

The client's part of the fight. Nothing here changes the world, and nothing but the picture is decided on the client: the server tells
it what to show with one payload (`ThanosFxPayload`) and with the entity's synced fields (his stones, the clock to the next, his state, move and
tick, the plates, the flags, the aim).

#### 15.14.1 The boss bar

`ThanosBossBar` draws the bar in the place of the vanilla one (`BossBarOverride`, for the three names of the bar): his health in the colour of his newest stone, a
darker bar behind it for what he has just lost, **a gold tick where each plate will break** (dark once it has), and under it **a row of six sockets**:
a stone he holds is a lit gem with a soft glow, the next one is a ring that fills while it counts down with its name and the time ("Space Stone in 0:42"),
the sixth is a dark socket outlined in yellow that is never filled. His name turns red when he is enraged. For a Thanos the client does not know (he
is far away) the vanilla bar's health and colour are shown alone.

#### 15.14.2 The tells and the effects

`ThanosFx` draws what the server sends, at most 48 at a time, none beyond 96 blocks, each for a few ticks. The fifteen kinds: a **ring on the ground** that closes in on the
impact (`RING_TELL`: the melee, the leap, the sigils of the Overload, the meteors, the Soul World's pillar), a **line** from the hand
that whitens as the aim locks (`LINE_TELL`, through the walls for the Pulse), a **beam** (`BEAM`, white core with a halo and a spiral of
motes; the Infinity Beam's is banded), a **portal** (`PORTAL`, `PORTAL_SKY`), a **falling star** in the stone's colour that is the stone arriving (`STAR`), a
**ring that spreads** (`SHOCK_RING`), a **mandala** (`MANDALA`: thin at first, solid at the end for the Snare), a **dome** (`DOME`: the Barrier blue, the Veil red, the
Time Lock green; the Barrier and the Veil are also drawn from his flags, so that a late arrival sees them), a **red wave** (`WAVE`), **orange chains** from the Gauntlet
(`CHAINS`, tense while the drain lasts, with motes flowing back), a **pillar** of orange light (`PILLAR`), the **Soul World** for the one taken (`SOULWORLD`), the **illusion**
round a petrified creature (`ILLUSION`, nothing in the world changes) and a white **flash** (`FLASH`, not with *Reduced motion*). At most 12 meteor rings, 6 chains and 4
domes are drawn at once. Every tell has the colour of its stone: gold for melee, purple Power, blue Space, red Reality, orange Soul, green Time.

#### 15.14.3 The air

`ThanosAtmosphere`, within 64 blocks of a living Thanos (the client option `bossAtmosphere`): the fog turns dark violet `#2A1A40` over three seconds, the edges of the screen
darken, a thunder that is not weather rumbles far away every 20 to 40 seconds, and a stone arriving flashes the screen white for a second. The call has its own sky: the fog
turns violet while the two items are held up and while the portal is open. On the player: a grey vignette for `PETRIFIED`, a green tint for `TIME_LOCKED`, and in the Soul World
an orange sky over still water with a gateway far ahead and the screen tinted orange.

#### 15.14.4 The boss's poses

`ThanosBossPoses` is a table of keyed poses (a wind-up, a strike, a hold, a recovery, each with its easing) for his body: a stand, the sweep, the smash, the jab, the throw, the
choke, the leap, the point (the Gauntlet along the locked aim, for the Blast, the orb, the drain and the Overload), the Gauntlet raised (Shockwave, Meteor Hail, Rewind, Infinity
Beam), the slam, the palm out (Pull, Barrier), the step, the wide arms (Veil, Phantoms), the press (Time Snare), the sight, the infinity pose, and the poses of the states: falling
through the portal, the surge (a knee, the Gauntlet arm shaking), the stagger, the arrival, the death (one knee, then seated). Each tick of a move reads its pose from the synced
state, move and tick, blended over four ticks from the last one. The legs swing at 0.5 of a player's rate with the arms against them, he breathes, his head follows what he looks at, he
leans forward a little when he chases. The sword whirls by an angle carried from frame to frame, so a whirl that begins or ends never makes it jump.

#### 15.14.5 The hero's screen

The hero's panel has a line for each key (15.11.2): the cooldown seconds, `ON`/`OFF` for the Veil, `draining` while the chain holds, `Sight 6.2s` while it looks, and, when the Rewind is
ready, the seconds of history stored (counted on the client, since the server's snapshots are not synced). The Soul Sight outlines come from `SoulSight` through the game's own glow, so they
show through walls and in his colour. The Drain's chain hums (the `soul_drain` loop, 15.15). The Gauntlet's sockets burn for the stone just used.

### 15.15 Sounds and music

All of it original and synthesized with the mod's numpy toolkit (`sfx.py`: oscillators, noise, filters, modal synthesis, a synthetic reverb; the recipes are `thanos_sounds.py`), mono Ogg
Vorbis at 44.1 kHz. **The six voices of the Infinity Stones are not here:** they are the 2.3 sounds (`<stone>_stone` and `<stone>_stone_hum`), and are played at every stone's arrival (full), as a
soft hum ten seconds before, at every stone move's tell, in turn in the Infinity Beam's charge, at each hero ability and in a row at the Gauntlet's ceremony. The portals reuse
the Tesseract's sound (higher), a dissolving illusion Loki's, the shield turning the sword home Mjolnir's impact.

#### 15.15.1 The sounds

Each cue of the fight (`ThanosSounds`) is a sound event, with its subtitle in English and in Hebrew; the names start with the lane's prefixes (`thanos_`, `gauntlet_`, `soul_`, `time_`, `reality_`).

| Event | Files | Seconds | Where | How it is made |
|---|---|---|---|---|
| `thanos_summon` | 1 | 3.5 | the charge, and the portal opening | a sub-bass swell (a sine 38 to 92 Hz and a saw an octave up, +12 dB over 3 seconds), a noise riser (400 Hz to 3 kHz), the Tesseract's own sound reversed, and at 3.0 s his bell cluster over a deep boom |
| `thanos_bell` | 1 | 1.4 | each second of the charge | the bell cluster alone: partials at 220, 349, 528 and 880 Hz (1 : 1.58 : 2.4 : 4.0) with a boom |
| `thanos_arrive` | 1 | 2.0 | the landing | a reversed ring-whoosh into a saturated 55 Hz thump, a noise burst, a crack, a gravelly rumble and grains of ground |
| `thanos_roar` | 1 | 1.6 | the arrival, each stone, the enrage | a baritone: a fundamental from 78 up to 108 Hz and down, formants at 600, 1000 and 2500 Hz opening toward "ah", a 6 Hz tremolo, a growl (noise modulated at 40 Hz), a rasp, a sub, a long hall |
| `thanos_step` | 3 | 0.45 | his footfalls | a 55 Hz thump, 0.15 s of gravel, a tiny armour clink (pings at 2.4 and 3.1 kHz) |
| `thanos_swing` | 2 | 0.55 | the Sweep, the Smash, the Throw | a broad whoosh sweeping 500 to 1800 Hz with a Doppler bend, the hollow ring of a big blade (190 and 215 Hz) |
| `thanos_hit` | 2 | 0.5 | his blows landing, the Slam | a 70 Hz thud, a metal clang (640, 1630 and 2900 Hz), a slap of body |
| `thanos_hurt` | 3 | 0.7 | he is hit hard (at most once in 20 ticks) | a short "oh" with the pitch falling 90 to 70 Hz, breath, a touch of vocal fry |
| `thanos_death` | 1 | 5.0 | the death (100 ticks) | a long exhale (78 down to 46 Hz through "oo" formants), a hiss of dust, and five falling tones (700 to 200 Hz), one a second, for the sockets going dark |
| `gauntlet_clench` | 1 | 0.4 | every stone move's tell, the hero's presses | a leather-and-metal creak falling 900 to 400 Hz, a gem click at 3.8 kHz, a low hum |
| `thanos_stone_surge` | 1 | 1.5 | a stone fuses (under its voice) | a rising shimmer, a deep "whomp" (40 to 65 Hz) at 0.7 s, a crackle |
| `thanos_power_blast` | 1 | 1.0 | the Power Blast, boss and hero, at the release | a 0.12 s rising snap, a wide crack, a 60 Hz boom, a violet shimmer tail |
| `thanos_infinity_charge` | 1 | 2.5 | the Infinity Beam's tell | five rising tones in turn 0.4 s apart (D2 F2 A2 C3 E3), each held, over a growing swell; it plays once |
| `thanos_infinity_beam` | 1 | 1.6 | the beam | a full-spectrum roar: pink noise with a 7 Hz tremolo of 3 dB, saws at 55, 110 and 165 Hz, a band that sweeps with the beam |
| `time_rewind` | 1 | 1.6 | the Rewind (boss and hero) | 28 reversed clock ticks speeding up from 6 to 40 a second, a sweep 120 to 900 Hz, a bright shimmer, a soft chime |
| `soul_drain` | 1 (`soul_drain_loop`) | 2.0, a loop | the chains lashing out; the hero's drain, in `PowerLoopSounds` | a choir shimmer (four detuned voices on A3, "ooh", 5 Hz vibrato) with chain clicks, 18 a second, at random pitches |
| `soul_world` | 1 | 3.0 | the victim's client | a gentle wash: low wind, two drops of water, a distant wordless voice on E4 at a quarter of the level |
| `reality_pop` | 3 | 0.3 | a bubble pops, the Petrify | a resonant blip gliding 900 to 1900 Hz in 40 ms, a puff of noise |
| `thanos_meteor_whistle` | 1 | 1.2 | each meteor falling | a sine gliding 1800 down to 400 Hz with a noise bed that follows it and a growing rumble |
| `thanos_barrier_up` | 1 | 0.6 | the Space Barrier | a glassy dome (partials at 900, 1420 and 2100 Hz), a noise sweep, a soft whump |
| `thanos_theme_1` to `_5` | 5 | 40 each, loops, streamed | the music (15.15.2) | below |

Departures from the plan, where the code's timing decides: `thanos_power_blast` opens with a 0.12 s snap, not a 0.6 s charge (the cue plays at the
release; the wind-up is the clench and the stone's voice); `thanos_death` is five seconds, the length of the dying state; `thanos_bell` is
new (the plan's vanilla bell would have clashed). The six sounds that the plan named without a prefix take `thanos_` (the Soul World is `soul_world`).

#### 15.15.2 The music

`ThanosMusic`: **five stems of 40 seconds in D minor**, one for each stone he holds, that play **together**, so the music grows as he does; each is a loop that closes,
every frequency a multiple of 1/40 Hz and every filter, noise and reverb circular. A stem fades in over two seconds when its stone arrives and stays; all are at full volume within 48
blocks of him, silent at 96, and fade out over three seconds when he dies or leaves; the game's own music waits; it is music-volume sound (the slider controls it) and the client
option `bossMusic` switches it off.

| Stem | Stone | What it adds |
|---|---|---|
| `thanos_theme_1` | Power | a drone (D1, D2, A2) in a slow breathing saw pad, a dark wind, and a heartbeat at 60 bpm (two low thumps, the second softer) that never stops |
| `thanos_theme_2` | Space | low strings: an open fifth rising D2 to A2 and settling on F2 over C3, twice in the 40 seconds |
| `thanos_theme_3` | Reality | high choir clusters a semitone apart (D5 E-flat5 E5 over A4 B-flat4 B4), their vowel slowly changing, glass above |
| `thanos_theme_4` | Soul | a driving ostinato at 90 bpm in eighths on D2 A2 F2 C3 under a low drum on the beat |
| `thanos_theme_5` | Time | the bells of the call (A4 D5 C5, then a low D) over a deep boom, with a high shimmer |

The stems are mixed so that all five together are no louder than a loud effect (each is trimmed 6 to 8 dB under the effects' loudness).

#### 15.15.3 How it was checked

`ThanosAssetsTests`: every event is registered, has its `sounds.json` entry with the right files, its subtitle in both languages, mono at 44.1 kHz,
a sensible length (the themes 40 s, the drain loop 2 s), the themes streamed; every cue of `ThanosSounds` plays an event of the mod. Each recipe was looked at as a
spectrogram and a waveform and measured (the peak under -1 dBFS after encoding, the loudness, the energy above 12 kHz, the pitch and the timing of what it is
meant to do); the loops were tested at the seam. The themes' downbeats are on the loop's first sample, which the seam test counts as a jump in level: it is a drum or a bell
beginning on the beat.

### 15.16 The review of the lane

The lane was read once more against the specification and then played in a real game (a dedicated server and two clients): the call, a two-player fight up to the fourth stone with each
stone's arrival and its five tells, a Hex as the arena, Stormbreaker and the Scarlet Witch against him, the reward, the becoming and the hero's keys with the Gauntlet in first person, and a
fight of two players with all five stones to the end. It found four things; each was fixed and has a game test of its own (`ThanosReviewTests`, and the targeting test of `ThanosFightTests`), which was
checked to fail on the old code.

1. **Titles wider than a screen.** A title is drawn at four times the size of the text and a subtitle at twice, and the game's scale for a 1080p window makes the screen only 480 units wide: the title
   of the reward (*THE GAUNTLET IS YOURS*) and the subtitles of the call and of the awakening (*Dread it. Run from it. Destiny arrives all the same.*) were cut at both ends. They are now *YOUR GAUNTLET* and *Destiny arrives all the
   same.*, which fit at every scale; a test holds every title of the lane to 14 characters and every subtitle to 32, in both languages (15.13.6).
2. **The hold on a target counted idle ticks.** "He keeps a target for sixty ticks" was a count of the calls of his idle choice, and he is idle only for a moment between one move and the next: the first fighter he picked
   held him for minutes, whoever hurt him since. It is now sixty ticks of the game (`TARGET_HOLD_TICKS`).
3. **A Titan who never lands.** The arrival (he is invulnerable in it) ended when he stood on the ground; one who fell into a lake, a cobweb or a block put into the column during the countdown never
   "landed" and stood in the portal until the fight timed out. Water and lava count as landed, and so does `LAND_TIMEOUT_TICKS` (200) of falling.
4. **The stuck rule walked through the Hex.** When he makes no progress toward the one he hunts the rule frees him to a spot beside them, and it did not ask the wall: a Thanos inside a Hex whose prey stood
   outside was put outside, and one outside was put in. It is a teleport like any of his now (`mayTeleportTo`, which asks `HexManager.onTeleport`), and a refused step leaves him where he is.

**Still imperfect.** No sound could be listened to in the test setup (a server and clients with no sound device), so the mix of the themes and the effects was judged only by the measurements of 15.15.3. A Hex that is raised *before* the call, with the landing spot outside its wall, keeps him outside it (the rule is "one outside cannot enter"); it is not a bug, but the caller may be surprised. The projectiles that a hero's Time Lock hung are freed when the dome closes or when the Thanos who cast it dies, logs out (so also when the server stops), changes dimension or loses the power; after a crash they stay hung, like any `noGravity` that a power has set.

---

## 16. Ultron — the boss and the hero **(added in 3.0)**

> "I'm here to help." In *Avengers: Age of Ultron* Tony Stark and Bruce Banner configure the neural net in the Mind Stone
> of Loki's Scepter; what wakes decides that peace means the end of humanity, and lifts a city into the sky to drop it. In
> Enhanced he is a **boss** (the biggest fight in the mod) and, for those who beat him, a **hero**.

Ultron has two parts that share a name: the **boss fight** (16.1 to 16.6), which is the only way to get the power, and the
**hero** (16.7 to 16.9) a player becomes with the reward. Both are in the **Enhanced** creative tab.

### 16.1 The Ultron Program and the ritual

* **The Ultron Program** ("Stark's peacekeeping program, waiting for a mind"). Crafted, never found in a chest, so
  Ultron is never met by surprise: a Nether Star in the middle, two Echo Shards above and below it, a Block of Redstone
  to its left and right and a Block of Iron in each corner (a Wither, an Ancient City and 36 iron). The recipe unlocks on
  picking up a Nether Star. Stack size 1, fireproof, epic, with no glint (only the Core has one).
* **The Scepter.** Loki's Scepter in one hand, the Program in the other, **Sneak + right-click** on a block (any power
  may start it: a party is started by one of its heroes). The centre of the arena is the top face of that block. Without
  Sneak, or without the Program, the Scepter does what it did before; with the Program in the other hand the Mind Stone
  only hums at it (it does not kill the holder, as with the gene and the Synthezoid Core).
* **The checks**, all made before anything is used up; the first refusal is told to the starter in the action bar: the
  boss is switched on (`ultron_boss.enabled`); the difficulty is not Peaceful; fewer than `[boss].maxActiveFights` (1)
  fights run on the server; the starter holds the Program; there is ground to aim at; the dimension has sky light and no
  ceiling (so not the Nether or the End); the arena and a margin of 14 are inside the world border; spawn protection and
  claims allow a break at the middle and eight points of the island's rim; the ground is level (it rises and falls by at
  most `maxRelief`, 6) and at least 60% of the island's columns are dry, walkable ground; the chunks of the island and the
  sky above it are loaded; no item frame, painting, leash knot, armour stand, minecart, boat or End Crystal hangs in the
  island's circle; the sky above the island, with a margin of 6, is empty; spawn protection and claims allow a break at
  the nine points in the sky too. Nothing that is built on the ground is a reason to refuse.
* **The awakening, 6 seconds.** The Program is used up (not in Creative) and the Scepter rests 20 ticks. Tick 0: the gem
  flares, a thin blue-white beam (`#7FD4FF`, the Mind Stone's) runs into the ground, `ultron_awaken` begins. Ticks 0 to
  50: scrap spirals up and assembles a standing silhouette. Tick 50: a flash, **six Ultron Sentries** (20 ticks
  assembling) appear in a ring of radius 6 and **Ultron Prime** hovers in the middle, invulnerable. Tick 60 and 100: *"I'm
  here to help."*, *"Peace in our time."* (the Sentries' eyes turn red). Tick 120: the fight begins.
* **Never by surprise, never by command.** `ultron` and `ultron_sentry` cannot be `/summon`ed or spawned from an egg,
  and an Ultron or a Sentry of the boss that is loaded without a live fight of its own is discarded (after a crash, or
  from a copied chunk). Operators have `/enhanced boss start ultron [force]` (16.10).

### 16.2 The bodies

Ultron is **one creature with many bodies**, so the boss bar, the target and the state never reset. The model is a
hand-made biped of 54 cubes on a 128x128 skin, scaled per body (not the player model: antennae, pauldrons and a jaw do
not fit a 64x64 skin).

| Body | Form | Scale | Hitbox | Health (n players) | Armour / toughness | Attacks |
|---|---|---|---|---|---|---|
| 0 | **Prime** | 1.9 | 1.3 x 3.4 | `bodyHealth` (600) x (1 + 0.35 (n-1)) | 8 / 4 | all |
| 1 | **Hybrid** (a Sentry's right forearm, a cracked chest plate, a broken antenna) | 1.7 | 1.2 x 3.1 | 0.8 of that | 7 / 4 | no Finger Beams |
| k >= 2 | **Sentry-Ultron** (a Sentry body with his crest and orange-red eyes) | max(1.1, 1.5 - 0.1 (k-2)) | 1.0 x 2.6, shrinking | 0.8^k of Prime's, at least 25% | max(2, 8-k) / 2 | Volley (one burst), Dive, Call, Punch |

(For one player: 600, 480, 384, 307, 246, 197, 157, then 150.) A hit takes at most **15%** of the body's maximum health
(before armour), explosions do half, and all mob effects are refused except Instant Damage and Instant Health (inverted
for undead only: he is a machine, so Harming hurts him). He is immune to fire, drowning, poison and the Wither (not to suffocation),
cannot be slowed (Quicksilver Time slows the Sentries, never the bodies), frozen, pinned, confused, shapeshifted, mind-controlled
or mind-controlled (**3.1: telekinesis holds him only as a boss**: never while he is shielded, changing bodies or carried by the lift, 3 s at most (Billy 1.5, Magneto 4.5, vibranium is metal), then 12 s before anyone can hold him again; he does nothing while held or for the first second of a throw); the Hex bounces him and never kills him (from the lift's lock to the end of the fight it lets him, the Sentries and the fighters through). Prime cannot drop below 10% before the island is up.

### 16.3 The attacks and their tells

Every attack that can hurt has a **tell** (a pose, a glow, a sound), and after it the aim is fixed. Ultron hovers 10 to 14
blocks from the nearest fighter, 3 above the ground, strafing, and never breaks a block. He chooses by weight among the
attacks that are ready and in range; never the same twice in a row, the Beams and the Dive never within 4 seconds of each
other, a Volley after a stagger, a pause of 1 second between attacks (1.5 for the later bodies).

| Attack | Range | Cooldown | Tell | What it does |
|---|---|---|---|---|
| **Repulsor Volley** | 6 to 30 | 3.5 s | 0.7 s: arms forward, fingertips glow, a whine; amber glint lines for the last 0.3 s | 3 bursts of 3 bolts, 1.7 blocks a tick, led 35%, 2.5 degrees of spread: **6 damage** each |
| **Finger Beams** (Prime) | 8 to 32 | 8 s | 1.5 s: **ten dotted aim lines** converge on a point at the target's feet and lock at the end; an electric hum | for 1 s ten beams fan +-10 degrees round the locked line, a hit every 4 ticks, **6 damage** each (up to five) |
| **Thruster Dive** | 5 to 30 | 12 s | 1.2 s: he rises 4 blocks; a red ring (radius 4) closes at one player's feet and does not follow | dives at 1.6 blocks a tick (8 to anyone within 1.5 of the path), lands: radius 4.5, **18 damage**, Slowness II; then **staggered 2.5 s** (grounded, venting, +30% damage taken) |
| **Gravity Pull** (Prime, Hybrid) | 6 to 26 | 15 s | 1.2 s: a fist in a pale-blue vortex, a ring of motes closes in | drags everyone within 16 blocks in sight for 0.6 s, then slams: radius 6, **14 damage**; nothing to anyone 2 or more blocks above the ground |
| **Call Sentries** | any | 25 s | 1.5 s: arms spread, the core pulses; cracks and pillars of light at every spawn point | a wave of Sentries |
| **Punch** | 0 to 3.2 | 1.25 s | 0.4 s: the fist drawn back | **12 damage**, knockback |

The Chaos Shield stops every bolt and every Finger Beam hit (6 each against the shield), not the shockwaves.

### 16.4 The Ultron Sentries

His army. A flying creature of the same model at scale 0.95, hovering on thrusters; **40 health**, armour 4, knockback
resistance 0.2, no fall damage; immune to fire, drowning, poison and the Wither; **metal** (Magneto can lift and throw
one); drops nothing and gives no experience. Eyes **blue** while assembling, stunned or with no target, **red** while hunting.
A **plasma bolt** (range 4 to 22, every 2.5 to 3.5 s, a 0.5 s tell, 1.4 blocks a tick, **4 damage**), a **punch** (within 2.2,
**6 damage**); several round one target take different angles. **Lightning stuns for 3 s.**

**Waves.** Six at the start; a wave of 4 every 25 s and whenever he calls, while fewer than 8 (+3 per extra player) live
and the **budget** (20, +8 per extra player) is not spent; on the island waves are 1.5 times the size and 0.8 times the
interval. A Sentry he has uploaded into, or one a hero hacked, no longer counts. The second boss bar counts **alive + budget
left** and the fight is not over while it is above zero.

### 16.5 Sokovia: the lift

At **55%** Prime floats to the middle, invulnerable, and a **Vibranium Key** (the Doomsday Trigger: a steel gear drum with
three toothed spokes and a black tower of gears with cyan seams) drills out of the ground. A 10-second **warning** (a ring
of light on the ground at the rim; fighters outside are asked in and, at the lock, set at the rim by a tractor), then the
island **rises 40 blocks in 8 seconds**.

* **The Sky Island (default).** The island is a **copy built in the sky**: a wavy disc of radius 20 (the rim varies by up to
  3), the natural ground as it is (5 layers) with what grows on it (up to 16 blocks), and a rock cone 14 deep. Blocks that are not
  natural (chests, doors, walls, torches, fluids) are not copied. The real ground is **never edited**. Only the island is built
  (and cleared) in the sky, in two batches of about 2,500 blocks a tick. During the rise the clients draw a mesh of the island
  climbing out of its place and hide the original columns on their side (`hideGround`).
* **Everyone who fights is carried and locked** for the move; the server keeps the kick counter from firing; villagers,
  animals and items stay on the ground. Telekinesis, Magneto's holds and Mjolnir's pins are released at the lock (and nothing that is carried can be taken, nor can a carried hero use telekinesis) (a thrown
  hammer is not recalled: it comes back to its owner as after any throw).
* **On the island:** more Sentries, and every 5 s, for every two fighters, a **red ring** (radius 3) under one of them for
  1.5 s and a piece of Sokovia falls on it: **10 damage**, nothing breaks.
* **The field.** A fighter who strays more than 4 past the rim, 3 below the cone or 80 above is put back on the island,
  takes 2 damage that cannot kill, gets Slowness for 3 s, at most once in 5 s. On the ground the same field is at the rim + 12
  and also catches anyone more than 12 blocks underground. A fighter who died stands in the **pit** for 2 s to be carried up
  again (3 times).
* **The arena is protected** from the ritual to the end: no block placed or broken, no fluid, no piston, explosions break nothing,
  mobs break nothing, no natural spawn; the mod's own block-changing abilities refuse (**3.1: telekinesis only borrows a block**, the hole mends and the block crumbles, and from the warning until the island is up, and from the descent on, the ground lends nothing; the island in the sky is protected even where `boss.protectArena` is off; Build Magic and the Magnetic Pull change nothing there); teleports that would leave the cylinder
  (Ender Pearl, Chorus Fruit, the Bifrost, Vanish, the Tesseract, the Witches' Road, Vibrate, portals) are cancelled.
* **The descent** is the mirror: the clients capture the island into the mesh, the server clears it, everyone is set down
  on the real ground at the same place relative to the middle, and the hidden columns come back. A player who was in the
  air over the island (a jump, a knock) is let down onto what is under them, their whole body at once, stopped by blocks
  as their own body is: a ledge their body still overlaps holds them, as do powder snow under leather boots and
  scaffolding they are not sneaking down; water or lava stops them at its surface; they are let down 64 blocks at most
  (with nothing under them in that, they stay where they are). A player who stands on something (within half a block) or
  is in water or lava stays as they are, and a hero who is flying flies on at his height. A rise that is cut short sets
  everyone down where they were taken from, in the same way. A fighter the pit's tractor is still carrying up when the
  island starts down is put on it at once and comes down with it.
* **The Tremor.** If the lift is switched off, the site cannot hold the island at the warning, or `liftMode` is `TREMOR`,
  the same warning, Sentries and falling rock happen on the ground and nothing moves.
* **Failure is one small rule.** The only record is a header of a few hundred bytes (`data/enhanced/sokovia`), written
  before the island is built and deleted after it is cleared. On a clean stop the island is cleared at once and whoever was
  on it, carried by it or on the way up to it from the pit is set on the ground below, in the same way; once it is falling
  its blocks are gone already, so whoever it is not carrying (one who stands on the real ground under it, or jumps there)
  is left as they are. After a crash the
  next start clears the island's shape at altitude, sets players down on the ground, discards the boss's leftover entities and
  deletes the file. A lost file leaves at worst a floating island of ordinary blocks (to be taken down by hand: `/enhanced boss cleanup` removes entities only), never a hole
  in the ground. `CUT_GROUND` (really lifting the ground) is a setting kept for later and is not in this release: set, it acts as `TREMOR`, and `liftRequiresMobGriefing` has no effect.

### 16.6 Uploading, the end and the reward

* **Bodies.** When a body would die, Ultron is not destroyed. If a Sentry of the fight is free (alive, not assembling, not
  owned by a player) he **uploads** into the nearest: a 4-second transfer (2 s of the body shuddering and sagging, the Sentries
  frozen and speaking as one, red-and-white data lines), the Sentry is consumed, the new body wakes weaker and says a line.
  A **Vision** within 48 blocks cuts the net: only a Sentry within 12 of the body will do.
* **The finish rule.** Ultron is destroyed for good only when **his body dies, no Sentry is alive and none is left in the
  budget** (or after `maxUploads`, 12, the safety valve). Then the body falls open, the last Sentries explode and the island
  lowers.
* **Heart rip.** A Scarlet Witch using telekinesis on a body at or below 15% of its health tears out its core (**3.1: also by pressing V again while she holds him**): it is
  destroyed at once. Launching a Sentry she holds crushes it (**3.1: Magneto's throw crushes one too**).
* **Speech** (his colour, a robot voice): *"I'm here to help."*, *"Peace in our time."*, *"Everyone creates the thing they
  dread."* (40 s in), *"I think you're confusing 'peace' with 'quiet'."* (the first fighter falls), *"You, Avengers, you are my
  meteor."* (the warning), *"This is exactly what I wanted. All of you, against all of me."* (the island), the three transfers,
  *"I've moved beyond your mission. I'm free."* (a body at 25%), *"Stark asked for a savior, and settled for a slave."* (the end,
  Vision near), *"Life... life always decides."*, and on a wipe *"When the dust settles, the only thing living in this world
  will be metal."*
* **Bars.** The first, red with six notches, is Ultron's current body (it reads "Ultron: lifting Sokovia" and "Ultron:
  uploading..." in their phases); the second, white with twenty notches, is "Ultron Sentries: N".
* **The reward: Ultron's Core.** Every fighter present at least 60 s who hurt the fight's entities, and whoever struck the last
  blow, gets one Core and 1,500 experience points (never a Creative or Spectator player, unless `creativeGetsReward`). It goes
  into the inventory; if that is full it drops at their feet, owner-only and never despawning; a fighter who is dead or offline gets it as a gift at the
  next login or respawn. Sentries and bodies drop nothing.
* **When the fight fails.** Everyone dead for 15 s (a wipe), nobody within 64 blocks for 90 s, or 25 minutes: the bosses dissolve,
  the island lowers, no reward and **no refund**. An operator's stop, a change to Peaceful, or the server stopping: the island is put
  back and the Program is given back. `/kill` on the body ends the fight with no reward.

### 16.7 Becoming Ultron

**Ultron's Core** ("The heart Wanda tore out. It still hums."): stack 1, fireproof, epic, with the glint, no recipe, never in a chest.
A player with **no power** who holds the Core in one hand and Loki's Scepter in the other and uses the Scepter awakens as Ultron:
the Core is used up (not in Creative), a red title, *"I was meant to be new. I was meant to be beautiful."*, a burst and a message to
the server. A player with a power keeps the Core and is told "Your DNA is already altered. The gene won't bond."; the Core, like the gene, protects its
holder from the Mind Stone's touch. Operators: `/enhanced power set <player> ultron [look]`.

### 16.8 Abilities

Ultron has **30 health** (15 hearts), **+8 armour**, +30% knockback resistance, no fall damage, never hungry and never out of
breath, immune to Poison and the Wither; he is **metal** (vibranium: Magneto lifts him), and **lightning does 1.25 times** (he is electronics).

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (hold) | **Repulsor Fire** | A plasma bolt every 4 ticks, alternating hands, from a pool of **30 charges** (one per bolt) that refills 4 a second after 0.5 s without firing; empty, it needs 6 before firing again. A bolt (2.6 blocks a tick, 30 ticks) does **4 damage** to the first creature and **2 to every other within 1.2 blocks**, with no hit delay (about 20 damage a second). It breaks nothing and sets nothing alight. A Chaos Shield stops it. The pool is Magneto's synced charge field, reused. |
| 1 | Sneak + R | **Overcharge** | With at least 10 charges: a 15-tick charge (a palm raised, glowing, trembling; he moves at 40% and cannot fire), then one heavy bolt: **14 damage**, **8 to everything within 3.5 blocks**, knockback 1.5, a flash and a boom, no block damage. Costs 10 charges, cooldown 8 s; being hit does not break the charge. |
| 2 | G (toggle) | **Anti-Gravity Flight** | Survival flight at 0.10 (Creative's is 0.05), upright and quiet. A pale-blue shimmer at palms and soles, a ground ring within 6 blocks with dust of the block below, a low hum (the Sentries' thrusters, pitched down). No jets, no flame, no hunger. |
| 3 | V | **Gravity Pull** | The first creature, dropped item or flying projectile within 24 blocks that is not his is yanked to 2 blocks in front of him: 3 damage and Slowness II for 2 s; a projectile is caught, an item lands at his feet. Not the bosses, not anyone pinned; no players with PvP off. Cooldown 4 s. |
| 3 | Sneak + V | **Gravity Slam** | A ring of anti-gravity to 7 blocks: every creature in it that is not his and is not a boss takes **6 damage**, is thrown outward and pressed down, and is slowed for 3 s. Cooldown 8 s. |
| 4 | B | **Upload** | Teleports him into the place of the drone he looks at (or the nearest within 48 blocks); the drone bursts into data; he is healed 8 (4 hearts) and loses harmful effects and fire. Cooldown 25 s. |
| 4 | Sneak + B | **Hack** | A 1-second cast on an enemy Sentry within 32: it becomes his drone. At most 2 hacked at a time; never works on a body; in the boss fight the Sentry leaves the count. |
| 4 | passive | **Second Life** | A blow that would kill him (after a Totem of Undying), with ability 4 ready and a drone within 64 blocks: the death is cancelled, he is set to 25% inside the drone, Resistance IV for 3 s; ability 4 then recharges for **5 minutes**. Not against the void or `/kill`. |
| 5 | N | **Sentry Army** | Three drones assemble round him (1 s): 24 health, a 3-damage bolt every 2 s, red eyes. They follow him (catching up by teleport past 24 blocks), attack what hurt him or what he last hit and hostile mobs within 12, and never a player unless PvP is on and that player attacked. At most 6; each lasts 90 s and all fall apart if he dies, leaves, changes dimension or loses the power. Cooldown 20 s. |
| 5 | Sneak + N | **Command** | All drones attack the creature he looks at within 32, or gather round him. |

**Vision cuts the net:** within 48 blocks of a Vision (or White Vision), Upload, Hack and Second Life refuse ("Vision has cut
your connection") and the HUD says "cut off". **PvP off:** his bolts, Overcharge, Pull and Slam neither hurt nor move players and
his drones never target them. Creative and Spectator players are never touched by his abilities. **Rules:** the Hex bounces him like
any hero (his drones follow his permission, as pets do); Quicksilver Time slows him and hangs his bolts; Billy's Freeze, Loki's Scepter
and Mjolnir's pin work on him; he cannot lift Mjolnir; Magneto lifts and throws him and the strip finds nothing; nothing he does breaks a block.

### 16.9 Looks

Four looks, with **H** (Sneak + H goes back): **Ultron Prime (Age of Ultron)** (default: gunmetal plates, bronze pipes, a ridged
crest and antennae, red-orange slit eyes; amber-gold plasma with a red edge), **Ultron Sentry** (matte black, a domed head, a red cheek
light, a ribbed spine; blue plasma), **VisionQuest (the human avatar)** (long grey hair and beard, glasses, a long dark coat; red
plasma; his eyes do not glow) and **Your own skin**. The skins use the 64x64 wide-arm layout; armour is hidden, so every detail is in
the overlay layers. The eyes glow from masks of four slanted pixels (none at rest in the human look).

### 16.10 MCU colours, commands and configuration

| What | On screen | In the mod |
|---|---|---|
| Ultron's plasma | Amber-gold beams from the fingertips (the still from Seoul); his eyes and torso light glow red | Amber-gold with a white core and a red edge; red eyes and a red torso strip |
| The Sentries | Blue idle, red when he is in them | Blue eyes and bolts; red when hunting or driven |
| Anti-gravity | The Chitauri technology, pale blue | Pale blue shimmer, rings and motes (flight, Pull, Slam, the rim of the island) |
| The net | Data, white and red | White and red square motes with scanlines (Upload, transfers, the tractor) |
| The Key and Sokovia | Steel, cyan seams, cracked stone | Steel, `#7FD4FF` seams, tan rubble |

* **Commands (operators):** `/enhanced boss start ultron` is the ritual without the Scepter (the Program in hand is used up, the arena is the block looked at, every
  check is made); `/enhanced boss start ultron force` starts the fight at your feet with no Program and skips the site rules (not the
  switch and not the sky), `/enhanced boss stop` ends it and gives the Program back, `/enhanced boss list`, `/enhanced boss cleanup
  <center> <radius>` removes a boss's leftover creatures and projectiles (no blocks), `/enhanced power set <players> ultron [look]` and `/enhanced look <players> prime|sentry|human|own`.
  The model viewer `/enhanced_model ultron|ultron_sentry|vibranium_key <state> [tick]` and `/enhanced_ultron_fx` are client tools for the artists.
* **Configuration:** `[ultron_boss]` (the fight: the bodies' health, armour and decay, the damage of every attack, the Sentries, the lift: mode,
  radius, height, depth, speed, and the field), `[ultron]` (the hero: his body, every ability's damage, range, pool, cost and cooldown, the drones,
  Vision's range, lightning) and the shared `[boss]` (one fight at a time, the wipe, abandon and time limits, the reward). Every number in this
  section is one of them.

### 16.11 Sounds

All synthesised, no samples. `ultron_awaken` (servos, plate hits, a rising electrical hum and a 7 kHz whistle, a boot chord), `ultron_bolt` (three
FM zaps), `ultron_overcharge` (a rising whine and a boom), `ultron_servo`, `ultron_beam` (a detuned hum with crackle, a loop), `ultron_gravity` (a
whomp), `ultron_lift_start` (stone cracking and a metal groan), `ultron_rumble` (a loop), `ultron_upload` (bit-crushed chirps and a modem warble),
`ultron_speak` (three robot-voice chirps), `sentry_hover` (thrusters, a loop; the hero's flight plays it lower), `sentry_death`, `ultron_transfer`
(overload, a falling saw and a chorus), `ultron_death` (a long power-down) and the **theme**: an original music box figure over a pulsing sub-bass, 32
bars at 84 BPM in A minor (not Disney's "I've Got No Strings"), played while a body is within 96 blocks. Each has a subtitle.

---

## 17. Apocalypse (En Sabah Nur) — the boss and the hero **(added in 3.0)**

> "Only the strong will survive." In *X-Men: Apocalypse* the first mutant, worshipped as a god for five thousand years, is woken in
> Cairo in 1983 and sets out to remake the world; the only thing in the film that breaks his protection is the mind: Jean Grey's
> Phoenix. In Enhanced he is a **boss** whose golden shield **stops everything except mind power** (the Scarlet Witch's and the
> Phoenix's), and, for those who beat him, a **hero**.

Apocalypse has two parts that share a name: the **boss fight** (17.1 to 17.8), which is the only way to get the power, and the **hero**
(17.9 to 17.10) a player becomes with the reward. Both items are in the **Enhanced** creative tab. The boss is built on the shared boss
framework (`boss/`, `BossFights`): the one-fight-at-a-time rule, the arena protection, the rewards and the gifts are the framework's;
the entity, the shield, the attacks, the states and the speech are his.

### 17.1 The Capstone and the ritual

* **The Capstone** ("The golden tip of the pyramid. The sun feeds it."). Crafted, never found in a chest, so he is never met by
  surprise: a Nether Star at the top, a Lapis Block in the middle of the second row and Gold Blocks on either side of it and across the whole
  third row (a Wither, five Gold Blocks and a Lapis Block). Stack size 1, fireproof, epic, with the glint. A plain right-click only says "Plant it in the
  sand, in the sun".
* **Planting it.** **Sneak + right-click on a block** with the Capstone in either hand. The centre of the arena is the top face of that block.
* **The checks**, all made before anything is used up; the first refusal is told to the starter in the action bar: the boss is switched on
  (`apocalypse_boss.enabled`); the difficulty is not Peaceful; fewer than `[boss].maxActiveFights` (1) fights run on the server; the
  dimension is in `ritualDimensions` (the Overworld); it is day and the block has sky above it (`ritualDaylightOnly`); the block is in the
  tag `enhanced:ritual_ground`, sand and sandstone (`ritualNeedsSand`); the circle of 8 blocks round the click has 12 blocks of free air
  above it, no liquid and at most 40 percent of its columns blocked; the arena and a margin are inside the world border; spawn
  protection and claims allow a break at the middle and at the rim; and **a Scarlet Witch or a Phoenix is within 64 blocks**
  (`requireBreaker`, `breakerRange`; Creative and Spectator players do not count), because a shield nobody can break is a fight nobody can win.
  Nothing that is built on the ground is a reason to refuse: the arena is never edited.
* **The awakening, 8 seconds.** The Capstone is used up (not in Creative). A column of gold light drives into the ground and
  `apocalypse_awaken` swells; the sky and the air turn amber for everyone within 64 blocks; a **wall of sand**, 40 high, rises round a circle of 24
  blocks radius; a violet portal opens above the middle; a flash, and he steps out facing the starter. For 3 seconds he is untouchable, his
  shield knits from the ground up, his eyes are white. *"Who wakes the First One?"*, then *"Only the strong will survive."*
* **Never by surprise, never by command.** `apocalypse`, `sand_lance`, `sand_boulder` and `sand_cocoon` cannot be `/summon`ed or spawned from
  an egg (`noSummon`), none is saved with its chunk (`noSave`), and an entity of the fight that is loaded without a live fight of its own is
  discarded: after a crash nothing is left. Operators have `/enhanced boss start apocalypse [force]` (17.11); `force` skips the checks of the
  ground, the sky and the breaker, never the switch, Peaceful or the one-fight rule.
* **When it is given back.** If the fight ends for a reason that was not the players' doing (the server stops, Peaceful, an operator's
  stop, the withdrawal for want of a breaker), the Capstone is returned (as a gift at the next login if the server is stopping). After a wipe,
  an abandoned fight or the time limit it is not.

### 17.2 The body

One creature, one boss bar. The model is a hand-made biped of **24 cubes** on a **128 x 64** texture (`client/render/ApocalypseCubes`,
the one table the model, the texture painter and the tests read; the 10 that exist on both sides are listed once and drawn mirrored with the
same texels, so 34 boxes are drawn): a player-like skeleton (head, body, two arms, two legs) with the armour built on it as cubes of their own, which a player
model cannot do (the domed cap, the cables, the flared collar, the pauldrons, the cloak's strips). A cube is armour, cap, cloth or body; in
the **true form** the armour is gone and the rest stays.

| Form | Drawn at | Hitbox | Health | Armour / toughness | Speed |
|---|---|---|---|---|---|
| **Armoured** (phases 1 to 3) | 1.35 times | 0.9 x 2.9 | `bodyHealth` (900) x (1 + 0.5 (n-1)) for n counted fighters, up to 6 | 10 / 6 | walks |
| **True form** (after the third shield) | 1.2 times | 0.8 x 2.5 | the same pool | none | 28 percent faster (0.36) |

He **walks** (he does not fly), keeps 8 to 14 blocks from the nearest fighter, and when he has found no path to anyone for 3 seconds he
steps through a portal. He cannot be pushed, set on fire, poisoned, withered, drowned or suffocated; **every mob effect is refused** (except
Glowing); the Hex bounces him (never kills him); Mjolnir's pin, Billy's Freeze, Loki's Scepter, shapeshifting and the Phoenix's
reading and possession do nothing. **(3.1) While his shield is up only mind power can hold him with telekinesis** (the Scarlet Witch, the Phoenix); in a window anyone's telekinesis can, as a boss (`holdTicks` 60: 3 s, Billy 1.5, Magneto 4.5 while he wears his armour, never in his true form; then 12 s before anyone can hold him again; the grab does not end his stagger). A block of his own arena thrown at his shield wears it half as much as other thrown things (`borrowedSlamFactor`). A hit takes at most **12 percent** of his maximum health (`maxHitFraction`), explosions do half.
His health follows the number of fighters: it is recomputed at the start of every transition and every phase, and he keeps the fraction of health
he has.

### 17.3 The shield: the one rule

**Mind power wears the shield down. Nothing else touches him.** The test (`apocalypse/MindPower`) is a pure function of the damage: it counts if and
only if its damage type is in the data tag `enhanced:mind_power` (the Chaos Beam, a telekinetic slam, the Phoenix Burst), its causing
entity is a player whose power is in `mindPowerHeroes` (Scarlet Witch and Phoenix by default, so a slam thrown by Magneto or Billy is not mind
power), and it arrives as damage. The same function is the hero's shield's own rule (17.9).

**(3.1) Critical hits.** On the **boss** (never the hero's shield), a hit is *critical* when its damage type is in `enhanced:mind_power`, its
causing player's power is in the new `critHeroes` (Scarlet Witch and Phoenix, kept apart from `mindPowerHeroes`, so adding Billy to the breakers does
not give him crits) and, on the shield, it is mind power at all (`MindPower.isCritical`, `isCritHero`). The Phoenix's Mind Control and Psionic Field strains are critical too. Every critical hit whose factor is above 1 plays `apocalypse_crit` (`CritSound`, 0.5 s, a 3 ms crack, glass shards between 1 and 6.5 kHz and a low gong at 73 Hz, volume 1.4 so the whole arena hears it; subtitle "Critical hit on Apocalypse"; file `sounds/apocalypse_crit.ogg`), at most once per attacker every 10 ticks, at the hit point on the shield and at his chest on the body.

| Hit | While the shield is up |
|---|---|
| Chaos Beam | amount x `chaosBeamFactor` (1) x **`critShieldFactor` (2)**: 10 a hit (5 x 2), one hit per attacker every `mindHitCooldownTicks` (10): shield 1 (**100** since 3.2) takes **10** hits instead of 20 |
| A telekinetic throw (a creature, an item, a block) | amount x `slamFactor` (1) x `critShieldFactor` (2); a block borrowed from his arena first x `borrowedSlamFactor` (0.5) |
| Phoenix Burst | amount x `phoenixBurstFactor` (**5**) x `critShieldFactor` (2): a Burst of 30 takes 300, **breaking any of a lone fighter's three shields at once** (**100, 120, 144**; 130 for two) |
| Phoenix throws | as the Scarlet Witch's |
| Phoenix Mind Control, Psionic Field | a **strain** (25 and 60 points, x `critShieldFactor`: **50 and 120**) even if refused, once a cast: a Phoenix strains his shield with each of them at most once per her own recharge of it (`mindControlCooldown`, `psionicCooldown`), however often she presses the key |
| Everything else (swords, arrows, TNT, fire, potions, pets, Mjolnir, lightning, Magneto's shrapnel, Vision's beam, Billy's Hex Beam and Bolt, the Darkhold's purple beam, the stones, the other heroes) | **nothing**: no damage, no knockback, a golden ripple and a chime (`apocalypse_shield_hit`) |

The server owner can add `"billy"` or `"vision"` to `mindPowerHeroes` and they then count with the same damage types. The shield's strength is
`shieldStrength` (**100**; 150 in 3.1) x `shieldGrowth` (**1.2**; 1.25) ^ (phase - 1) x (1 + `shieldPerExtraPlayer` (**0.3**; 0.35) (n - 1)): **100, 120, 144** for one fighter, 130, 156, 187 for two,
160, 192, 230 for three. It knits in 40 ticks, 2 seconds (nothing hurts him then), mends 2 percent a second (`shieldRegenFraction`; 3 in 3.1) once 10 seconds (`shieldRegenDelayTicks` **200**; 160)
have passed with no mind power. **(3.1) His health never heals**: `regenPerSecond` (the heal while a shield is up, never above the part of the fight he is in)
and `trueFormRegenPerSecond` (the true form's) are both **0** by default; a potion of Healing does nothing to him (`heal` is a no-op; the fight's own `heal(points, ceiling)` and the Escape's return
use `setHealth`); the shield still knits and re-forms. The Apocalypse **hero's** healing factor is unchanged.
**(3.1) On his body**, when the shield is down (a window, the stagger of an interrupted Escape, the true form): an explosion x `explosionDamageFactor` (0.5), then **a critical hit x `critBodyFactor` (2), any other blow x `othersBodyFactor` (0.4)**, then his armour, then x `windowDamageFactor` (1.25) in a window, then the cap `maxHitFraction` (12%) and the phase's floor. So Wanda's beam of 5 takes 12.5 in a window (5 x 2 x 1.25) and a sword of 7 takes 3.5 (7 x 0.4 x 1.25); in the true form without the 1.25. A critical blow counts double toward `escapeInterruptFraction`. Every critical hit shows 20 `CRIT` and 10 `ENCHANTED_HIT` particles, plays `enhanced:apocalypse_crit` (stand-in: the vanilla critical sound at pitch 0.7 until its file lands) and tells the attacker "Critical hit! ×2" (at most once per attacker every 10 ticks). `FORMING`, `REFORMING`, set pieces and his invulnerable states take nothing. The tests are `ApocalypseCritTests`. The boss bar is blue (his health, with notches at the floors) and a second, yellow bar for the shield ("broken" with the seconds left,
"forming").

### 17.4 The fight, phase by phase

| Part | What happens |
|---|---|
| **Phase 1: The God Awakens** (100% to 66%) | Shield 1. Sand Lances, Sand Wave, Sand Spikes, Boulder Barrage, Portal Step, Backhand. |
| **Window 1** | The shield breaks: he kneels (staggered 4 s, `staggerTicks` 80, attacking nothing), 12 s (`windowTicks` 240) in which **everything can hurt him** (critical hits x2, the rest x0.4, then x1.25 for the window: `windowDamageFactor`), armour counting. His health cannot fall below **two thirds** (the floor); reaching it ends the window. If nobody does enough, the shield re-forms at full strength. After the kneel he fights on with Lances, Backhand and Spikes at half speed. |
| **Transition 1** (6 s) | Untouchable; **his Horsemen** (three at the default factor) step out of portals; shield 2 forms. *"Rise, my Horsemen."* |
| **Phase 2: The Horsemen** (66% to 33%) | Shield 2. **Entomb, Mind Crush and one Tornado** join. Window 2 (floor at one third). |
| **Transition 2: the Cairo Collapse** (8 s) | He floats up, the sky goes dark ochre, **three rings of sand** cross the arena (30 ticks apart); any Horseman still alive crumbles; shield 3 forms. *"Everything they've built will fall..."* |
| **Phase 3: Unmasked** (33% to 5%) | Shield 3. **Two Tornadoes at once** (three at `powerFactor` 1.0). Its break has **no window**: |
| **The armour falls** | The plates fly off (collar, pauldrons, chest, gauntlets, the crown of the cap), the shield is gone for good, he is in the **true form**, and **everything can hurt him** (critical hits x2, the rest x0.4), with no window; his health stops at 15% until the Escape has been tried, then at 5% (the kneel), so both always happen. He does not heal there either (`trueFormRegenPerSecond` 0). |
| **The Escape** (once, at 15%) | A hand rises; a violet portal grows over him for 100 ticks (`escapeChannelTicks`). **Interrupted by any lightning damage (Thor's), by `escapeInterruptFraction` (4%) of his health in damage during the channel, or by a Hex wall.** A critical blow counts double. Interrupted: staggered 6 s and never again. Not interrupted: he vanishes and returns 10 s later at `escapeHealTo` (40%) with a fresh shield 3. The blow that earns the Escape starts it before his healing can mend him. |
| **The end** (5%) | Untouchable and kneeling: *"All is revealed."* He dies **in the Phoenix's fire, layer by layer** if a Phoenix dealt Burst or throw damage in the last `phoenixFinishSeconds` (15), otherwise in a column of sand (`apocalypse_death`). He drops nothing. |

The fight is meant to last 6 to 10 minutes for two or three equipped players (a lone Scarlet Witch: 8 to 12, mostly dodging). The two
floors are why a fight can never be won early, whatever damage a group brings.

### 17.5 The attacks and their tells

Every attack that can hurt has a **tell** (a pose, a glow, a sound) and then a **fixed aim moment**; after it, it does not follow. His sand is
**sand, not metal** (Magneto's shield does not catch it). He chooses by weight among the attacks that are ready and in range, never the
same twice in a row; damage scales with difficulty like a mob's.

| Attack | From | Tell (and the lock) | What it does |
|---|---|---|---|
| **Sand Lances** | 1 | The right hand rises, the fingers pinch; five thin ochre lines from the palm to the chest (locked 6 ticks before) | `lanceCount` (5, times the factor, rounded up: **4**) in a flat fan, **3.9** each (6 × 0.65). Billy and the Phoenix can catch them and throw them back |
| **Sand Wave** | 1 | Both arms sweep low; a faint lane on the ground, 4 wide (locks just over halfway) | A ridge 1.1 blocks tall: **6.5** (10), knockback, Slowness II; lower than a jump's peak |
| **Sand Spikes** | 1 | A fist rises and slams; an amber ring under each of up to four players, tracking, then locked | Spikes in each ring: **5.85** (9), launched up |
| **Boulder Barrage** | 1 | Both arms rise; three blocks of sand tear out of the ground and orbit over his head with a growing hum; amber lines to up to three players | Three boulders: **5.85** (`boulderDamage` 9) each; can be caught and thrown back |
| **Portal Step** | all | Violet shards at his feet; a second ring opens at the exit, usually behind the player farthest from him | An arrival shockwave: **3.9** (6), knockback. A Hex wall refuses it |
| **Backhand** | all | The arm drawn across the chest (8 ticks) | **7.8** (`backhandDamage` 12), strong knockback |
| **Entomb** | 2 | His hand reaches to the player who wore the shield most; a stripe of sand crawls to their feet and locks 10 ticks before the hand closes | If still on the spot: a **cocoon** for `entombTicks` (**80**; 100 in 3.1): no movement, jumping, flight, attacks, items or powers; 0.98 a second (1.5 × 0.65), never below two hearts; `cocoonHealth` (20) to break it. A complete Chaos Shield skips it; a phased Vision is intangible; `enhanced:entomb_immune` |
| **Mind Crush** | 2 | Two fingers to the temple, his eyes flash white-violet, a violet ring contracts from 28 blocks to nothing (2 s) | Everyone within `mindCrushRange` (28): Nausea (`mindCrushNauseaTicks` **80**; 120), Slowness 3 s, Blindness 2 s. **Magneto's helmet turns it away.** A Phoenix who touches him with Mind Control or the Psionic Field during the tell interrupts it. The Chaos Shield does not stop it |
| **Tornado** | 2 (one), 3 (two) | Both arms rise; the sky above darkens; dust spirals up at one to three spots | Funnels for 160 ticks that drift toward the nearest player, slower than a walk: 1.3 (2 × 0.65) every half second, and they lift what is inside |
| **Cairo Collapse** | T2 | The ground ripples; he floats up, arms wide | Three rings: **6.5** (`ringDamage` 10), strong knockback |

**(3.2) The master factor** `powerFactor` (0.65; 0.1 to 1.0) multiplies each damage in this table (the figures in bold are after it, the option's own number in brackets), the counts of Lances, Horsemen and Tornadoes (`ApocalypseBossConfig.count`: rounded up, never below one, zero stays zero), and divides the pace (`slower`: the pause between two attacks, 30, 25 and 20 ticks by phase, and every attack's
cooldown, last 1 ÷ factor times as long, about 1.54 at 0.65). It does **not** touch his health, armour, shield, windows, the arena wall's `fieldDamage`, the tells and their lengths or the projectiles' speed, and the Apocalypse hero is unchanged. The Portal Step's shock and the Horsemen's tricks (Famine's bolt 3, Pestilence's leap 7 and Death's slam 9) are scaled too; War's Shard Volley (4 a shard) is not.

The Lances and Boulders are entities (`sand_lance`, `sand_boulder`, both of `AbstractSandProjectile`) that implement `ShieldCost`, so the Chaos Shield
stops them and pays for their damage, and a projectile that Billy or the Phoenix catches is thrown back like any other. His blows are the damage types `sand`, `apocalypse_blow` and `entomb`.
**No attack ever changes a block**: the sand wave, the spikes, the tornadoes and the wall are effects and entities, so mobGriefing changes nothing and there is
nothing to restore after the fight.

### 17.6 The Horsemen

At Transition 1 he enhances **Husks** (vanilla desert mobs): `min(count(horsemenMax (4)), 1 + fighters)`, **three** at the default factor (`ceil(4 × 0.65)`), in the order War, Famine, Pestilence, Death (when `powerFactor` cuts the Four to fewer in a fight big enough for four, Death takes the place of the third: `Horsemen.kindAt`, so War, Famine, Death). Each is
named, glows, has `horsemanHealthFactor` (**1.5**; 2 in 3.1) times a Husk's health, `horsemanDamageBonus` (**3**; 4) × the factor (**1.95**) more attack damage, a little more speed and armour,
and **drops nothing and gives no experience**. They live until killed or until Transition 2. They are ordinary creatures: Wanda can lift them,
a Phoenix can take one over, Billy can freeze them. A Magneto hears, once, "Join me, Erik." **(3.1)** War's and Pestilence's iron is metal: Magneto can lift them, and his strip or pull tears it off, and it **crumbles** (the Horsemen are never farmed); Famine and Death carry nothing.

| Horseman | Gear | Trick |
|---|---|---|
| **War** | iron helmet, chestplate, axe | **Shard Volley** every 5 s: three iron shards hover, then fly in a fan, 4 each (Magneto can catch them) |
| **Famine** | none | **Gale** every 6 s: white lightning on the hands, a bolt falls beside the target, a gust blows back (1.95; Thor takes no lightning damage but the wind still pushes) |
| **Pestilence** | iron sword | **Psi-Blade Dash** every 5 s: the sword glows violet, a six-block leap: 4.55 and Slowness |
| **Death** | none | **Dive** every 7 s: he springs up, hangs, dives with a slam: 5.85 in a radius of 2.5 |

### 17.7 The arena, and when it goes wrong

* **Protection** (from the planting to the end, a circle of the arena + 8): no block is placed or broken, no fluid, bucket, piston or TNT works,
  explosions break nothing, and the mod's own block-changing abilities refuse (**3.1:** telekinesis only borrows a block: the hole mends, the block crumbles). **The wall of sand** (4 outside the arena, 40 high) is solid to
  players and to what telekinesis throws; whoever touches it is pushed back once a second for `fieldDamage` (1) that cannot kill, and flight ends at
  its top. Ender Pearl, Chorus Fruit, the Bifrost, Vanish, the Tesseract, the Witches' Road and Vibrate cannot take anyone out of the circle, and nothing
  takes anyone in. Anyone more than 8 blocks underground is lifted back by a violet tractor of sand. A dead fighter's drops never despawn while the
  fight lasts and get a fresh five minutes after it.
* **Everyone dead** for 15 s: *"You are not worthy yet."* He withdraws, the Horsemen crumble; no reward, no refund.
* **Nobody within 64 blocks** for 90 s: the fight is abandoned. **25 minutes**: he withdraws (no refund).
* **No breaker**: if no Scarlet Witch or Phoenix is alive in the arena for `noBreakerSeconds` (60) while the shield is up, he withdraws and the
  Capstone is given back.
* **A crash** leaves nothing: entities are not saved; at the next load any entity of the fight is swept as an orphan.

### 17.8 The reward: the Essence of En Sabah Nur

"A sapphire soul in gold veins. It looks for a body." Never crafted, never in a chest; stack 1, fireproof, epic, with the glint.

* **Who gets one:** every fighter who stayed at least `[boss].rewardMinSeconds` (60) and dealt any damage to him **or to his shield** (so a Scarlet Witch who only wore
  the shield down is rewarded like the one who landed blows), and whoever landed the last blow whatever their time; never a Creative or Spectator player (unless
  `creativeGetsReward`); pets credit nobody. Each also gets 1,500 experience. It goes into the inventory, else drops at their feet for them alone, never despawning (no time limit); a
  dead or offline fighter gets it as a gift at the next login or respawn. `dropsPhoenixEmber` (off) also gives a Phoenix Ember to a fighter with no power.
* **Awakening.** A player with **no power** who **holds right-click for `essenceRitualTicks` (60, three seconds)** is moved into it (`AwakeningItem`): blue wisps
  with gold veins spiral in with a heartbeat (`essence_transfer`), the skin turns blue, a harmless lightning flash, and the Essence is used up (not in
  Creative): **Apocalypse**. Letting go early uses nothing. A player with a power is told "Your body already holds a power".

### 17.9 Becoming Apocalypse: the abilities

**40 health** (+20), **armour 10, toughness 4**, +50% knockback resistance, a **healing factor** of half a heart a second after 5 seconds without damage; his own
sand never hurts him. Keys **R G V B N** (Sneak for the other).

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R | **Sand Wave** | A ridge of sand from his feet along the ground, `waveLength` 18 long and 4 wide, 0.9 blocks a tick: **8 damage**, knockback, Slowness II for 2 s, each creature once. Needs ground within 6 blocks below. Cooldown 3 s. |
| 1 | Sneak + R | **Entomb** | A creature or player within 24 (`entombRange`): sand climbs its legs into a cocoon for **80 ticks**: no moving, jumping, flying, attacking, items or powers; 1 damage a second, never below two hearts; friends free it by hitting the cocoon (16 health). Not on bosses, a phased Vision, Creative or Spectator, a complete Chaos Shield; players only with PvP on. Cooldown 12 s. |
| 2 | G (toggle) | **Levitation** | Survival flight at **0.08** (Creative's is 0.05), upright and calm, with sand motes at his feet and a ring at take-off. |
| 3 | V | **Portal** | A violet portal to the spot he looks at within 40 (on top of the block or in front of it). He takes every tamed pet and teammate within 3 blocks and every other player within 3 who is sneaking (stepping in is consent). The Hex wall judges every traveller, a pinned one stays, and nobody leaves a boss arena. Cooldown 8 s. |
| 4 | B (hold) | **Apocalypse's Shield** | A golden hexagonal sphere (radius 2.4): 10 ticks to form, **stops nothing until complete**, then stops every kind of damage **except mind power** (and except falls, drowning, suffocation, starvation, the void and `/kill`). Projectiles are neutralised before they hit; Mjolnir and Stormbreaker are turned back to Thor. Held up to **6 s**, **60 points of strength**; what it stops wears it by `shieldOtherWear` (0.5) of the damage, **mind power in full** (the Burst five times over), and when it breaks the rest of that blow comes through. A Phoenix's Mind Control and Psionic Field strain it by 25 and 60, once a cast and only with PvP on (`ApocalypseGuard.strain`). Recharge 8 s (20 s after it shatters), a little food. |
| 5 | N | **Power Bestowal** | An ally within 24 (a player, a tamed pet, an Iron Golem or a villager; "You only enhance your own") is **Empowered** for 1,200 ticks: +3 attack damage, +4 armour, +10% speed, half a heart every 2 s, and a hero's ability cooldowns recharge 1.5 times as fast; a golden aura. At most 2 at once (a third replaces the oldest). Cooldown 20 s. |
| 5 | Sneak + N | **Sandstorm** | A funnel of sand round him for 120 ticks, radius 7, following him: every half second everything he may hurt takes **2** and Slowness, **projectiles that enter are slowed to half**, and it blots out the sight of those inside. Allies are unaffected. Cooldown 30 s. |

**Rules.** The Hex bounces him like any hero unless Wanda allowed him; Mjolnir's pin stops his levitation and portal; his sand and storm ignore a Hex
wall. Magneto's powers do nothing to him unless he wears or holds metal (he is flesh and sand is not metal). With **PvP off** his Wave, Entomb
and Sandstorm do not hurt, hold or slow any player (mobs always); the shield, portal and Bestowal still work. A Spectator Apocalypse can use nothing;
Creative players are never swept or entombed and pay no hunger. `apocalypse.shieldOtherWear` at 0 makes the hero's shield cost nothing (the literal
rule); **the boss's shield always uses the literal rule.**

### 17.10 Looks and art

Two looks and the player's own skin, chosen with **H** (Sneak + H goes back); the abilities are the same in every one. Skins are 64 x 64, classic
(4 px) arms, an opaque base layer and no semi-transparent pixel; the eyes are glow masks (`apocalypse_eyes*`).

| Look id | Look | What it is |
|---|---|---|
| `apocalypse` | **X-Men: Apocalypse (1983)** (default) | Teal-blue skin, blank white eyes, a domed gunmetal cap with a leaf engraved on the brow, ribbed black cables down the sides of the face, a wide pewter lace collar and shoulder plates, dark verdigris armour with a cyan glyph on the chest, black gloves, a hooded teal cloak. |
| `en_sabah_nur` | **En Sabah Nur (3600 BC)** | The god-king in gold: a falcon-beaked helm with red cabochon eyes, a broad collar of gold, teal, lapis and red, teal chevrons on gold armour, a pleated kilt and sandals. His eyes glow red. |
| (own) | **Your own skin** | His powers in your own skin. |

The boss has two textures on the 128 x 64 sheet (`apocalypse_boss`, armoured, and `apocalypse_boss_true`, with its gold cracks), each with a glow layer
(`..._eyes`: his eyes `#FFF6D8`, the glyph on the chest `#59C3CF` and the engraving of the cap `#3FA0FF`, drawn with the eyes render type); the effects have
their own (`sand_swirl`, an animated strip of four square frames for the wall and the tornadoes; `golden_web`, the shield's hexagonal lattice with knots of `#FFF2B0`;
`sand_cocoon` and `sand_cocoon_cracks`). The art is drawn by generators written for the lane (Python and PIL, with a renderer that
reproduces the cube table so the model can be looked at without the game), not by hand, so a change of colour is a change of a palette. The Capstone (three
frames) and the Essence (four) are animated.

### 17.11 MCU colours, commands and configuration

| What | On screen | In the mod |
|---|---|---|
| The shield | Fan wikis describe a yellow-hued energy shield that held against Storm, Cyclops and Magneto but "did not protect him from Phoenix's power for long" (the recap says only that he obliterates the projectiles; the colour is the wikis', the hexagonal web is borrowed from the web-like cocoon of the 3600 BC scene, which the artists built on microscopic bone tissue) | **Shield gold**: a sphere knit from a hexagonal web, core `#FFF2B0`, main `#F5C84A`, edge `#E39B1B`, deep `#8A5A0E`. The same for the hero's shield and the Bestowal. `Power.APOCALYPSE` is `0xF2B84B` / `0xFFF0B8` |
| His portals and the Mind Crush | "An immense sphere of violet light", a ring of pink-violet shards (the actor said the first stills made him look purple because of the portal's light) | **Teleport violet**: core `#F0D4FF`, main `#B04FD8`, body `#7A2EC8`, fringe `#3F1A7A`; the Mind Crush's ring and eyes white `#F4F4F4` and violet |
| His sand | Warm ochre and amber, the Cairo stills are sepia with dust haze | **Sand**: light `#E6CB86`, main `#D8B76A`, shade `#B8934A`, deep `#8C6B2F`; the haze of the arena's air `#C9A862` |
| The Essence, the transference and the cap | At the altar his soul is a sapphire-blue gaseous energy and golden liquid runs through circuit-like symbols; the capstone's energy feeds down through golden veins | **Soul-glow**: sapphire `#3FA0FF`, core `#D8EEFF`, gold vein `#F5C84A` |
| His death | Jean's orange phoenix; his body torn apart layer by layer, a grey skull with red muscle in a shower of orange-gold sparks | The Phoenix's fire palette (core `#FFF4C2`, gold `#FFD65A`, main `#FF8A1F`, ember `#E2531A`) and the dissolve layer by layer |
| His voice in chat and title | His skin, a steel blue | `#4A9BC4` (`Power.titleColor`) |

* **Commands (operators):** `/enhanced boss start apocalypse force` starts the fight at your feet (no Capstone; without `force` the command is the ritual and needs a Capstone in hand and a block in sight), `/enhanced boss stop` ends it and gives the Capstone
  back, `/enhanced boss list`, `/enhanced boss cleanup <center> <radius>` (removes strays of the boss framework in a radius; he edits no block, so there is never a site to restore), `/enhanced power set <players> apocalypse [look]` and
  `/enhanced look <players> apocalypse|en_sabah_nur|own`. The model viewer `/enhanced_model apocalypse <state> [tick]` is a client tool for the artists.
* **Configuration:** `[apocalypse_shield]` (what counts as mind power and how much each kind wears the shield), `[apocalypse_boss]` (the fight: health, the shield's
  numbers, the windows, the Escape, every attack's damage, the Horsemen, the arena), `[apocalypse]` (the hero: his body, every ability's damage, range,
  cooldown and cost) and the shared `[boss]`. Every number in this section is one of them.

### 17.12 Sounds

All synthesised, no samples, no part of the film's score. `apocalypse_awaken` (a deep rumble, grinding stone and a choir chord rising to a gong),
`apocalypse_portal` (a reversed crystalline shimmer and a soft thump), `sand_wave`, `sand_spike`, `sand_storm_loop` (a loop), `entomb`, `entomb_break`,
`apocalypse_shield_up` (gold chimes), `apocalypse_shield_hit` (a glassy tink), `apocalypse_shield_break` (glass over a sub-boom), `apocalypse_mind_crush`,
`apocalypse_speak` (a low breathy "ah" under every line he says), `apocalypse_bestow`, `essence_transfer` (a heartbeat at 60 per minute under a rising shimmer),
`apocalypse_death` and **two themes**, played while he is within 96 blocks (full volume within 64): an original loop of 6/8 in D Phrygian dominant, a drone, a frame drum, an
end-blown flute and a choir, 96 seconds (60 bars); and a fuller loop of 64 seconds (40 bars) for the third phase, with a second drum, rising strings and a
faster pulse. They share one bar so that their pulses agree; both loop without a seam. Every effect has a subtitle in English and Hebrew; the music does not.

### 17.13 Tests

122 game tests in eight classes: `ApocalypseRitualTests` (7: every refusal of the ritual and what is used up), `ApocalypseShieldTests` (17: the rule of mind
power, every kind of hit against the shield, the windows, the floors), `ApocalypseBossTests` (18: each attack and its tell, the Horsemen), `ApocalypseFightTests`
(22: the phases, transitions, the Escape, the end, the rewards, the failures), `ApocalypseTests` (23: the hero's five abilities, the body, the Essence),
`ApocalypseReviewTests` (5: what the review found: the end of the arena told to everyone, Creative players not counted as fighters, the Sandstorm slowing a lance,
Quicksilver Time slowing his tells and his Horsemen, a tornado kept in the arena), `ApocalypseAnimationTests` (14) and `ApocalypseClientTests` (16: the cube table, the poses, the tells, the music and the amber air, and the art). Any test that touches a fight has a
batch of its own (`apocalypse_...`), because a window nobody uses is lost; the pure ones share `apocalypse_pure`. The art is pinned by the tests too: no two regions of
the texture overlap, every texture is the size the model reads, and the glow masks light only a small part of the sheet. `DataTests` checks the lang in both languages, the subtitles and that every
`.ogg` exists.

---

## 18. Build Magic: Wanda's and Billy's building (added in 3.1)

Eilon asked for "WorldEdit/Axiom type abilities that are easy to use", limitless for Wanda and limited for Billy, with no actual
WorldEdit: **Build Magic**, our own code, taking from those tools only their ideas and their controls. The player's guide is the
wiki page *Build Magic*. Package `build/` (common and server code, nothing in it names a client class), `client/build/` for the
input and the pictures. The key rule of the page: **easy to use for an 11-year-old**: one key, one mouse grammar, everything seen before
it happens, undo always one key away.

### 18.1 The key, the grammar, the context

* **Y** (`key.enhanced.build_magic`), `IN_GAME`, for a Scarlet Witch or Billy with no screen open and not a Spectator; for anyone else it
  does nothing. **Tap** (let go within 6 ticks): build mode on or off. **Hold** 6 ticks: the **Spell Wheel** (`SpellWheelScreen`, a screen
  that does not pause the game); out of build mode it turns the mode on first. The eleven spells sit on a ring (tiles of 40 pixels at a radius
  of 110, clockwise from the top, in the order of the table in 18.3). Moving to a picture and letting go of Y chooses it and closes the wheel;
  letting go in the middle (a dead disc of 28 pixels) leaves it open; a click on another spell chooses it and keeps the wheel open (the style
  row, the hotbar and the Blocks page follow), a click on the chosen one closes it; **1** to **9** and **0** (the keypad too) choose the first
  ten spells by number and close it (Grow, the eleventh, has no digit); the mouse wheel steps through the spells (down = clockwise); Esc closes
  it unchanged, and so does a tap of Y, which is swallowed (`BuildInput.swallowMagic`) so build mode stays on; a digit chosen while Y is
  still held closes the wheel and the hold that goes on does not open it again. The wheel also carries the style row (27 pictures), Solid and Hollow for Shapes, the dice (Mix), the nine hotbar slots, the
  **Blocks** page, Undo and Redo with their counts, and the centre door (start or stop building). The sixth line of the hero panel is
  `[Y] Build Magic ON/OFF` (Billy's also `✦ 372/512`).
* **Build mode ends by itself** on death, respawn, a power change, Spectator or Adventure mode, logout, and when the server turns
  `build_magic.enabled` / `operatorsOnly` against the player. A change of dimension keeps the mode and lets go of the box.
* **One mouse grammar, no Sneak.** Left click = *where* (corners; a drag; double click = the whole thing; the sky = let go, or a corner on the
  floating aim cell 6 blocks ahead); right click = *do it* (vanilla when nothing is ready; on a creature always vanilla); scroll = the
  main number (Ctrl: 8 at a time; Repeat's copies and gap 4); Alt + scroll (Left Alt, `key.enhanced.build_more`) = which kind (style,
  shape, turn, gap); middle click = take that exact block; Ctrl + Z / Ctrl + Y undo and redo; Ctrl + R / Ctrl + F turn and flip a
  ghost; Ctrl + C / X / V copy, pick up, bring the pocket back; Delete vanishes the box.
* **Contexts.** `BUILD` (no screen, build mode on) holds turn, flip, copy, cut, paste, vanish and `build_more`; **`WITCH`** (no screen, the
  power is the Scarlet Witch or Billy, in build mode or out) holds undo and redo, so Ctrl + Z of a witch never reaches Wanda's Z, and the
  server accepts UNDO and REDO with build mode off. NeoForge's key lookup gives a Ctrl press only to the Ctrl mapping when one is active;
  the game's Controls screen still marks them (and the keys they share) in red: display only.
* **How clicks are taken.** Attack on a block, or anywhere while the grammar wants a click, is cancelled (nothing is ever broken, in Creative
  too); use becomes the spell's right button when something is ready; scroll is taken only when there is something to adjust.

### 18.2 Selecting

* The **aimed cell** is the block under the crosshair within the hero's reach (Billy 24 at rank 1, +8 a rank; Wanda `build_magic_wanda.reach` 256),
  found with the mod's block ray (grass and flowers are clicked through; a pick uses the outline ray). Billy's rubber band stops at his side limit
  (and says "Too big for you yet: at most N blocks on a side"); a click on a block beyond his reach, up to reach + 64 blocks of looking, marks
  nothing and says "Too far for your magic", where a true sky is still the floating cell. The click that fixes corner B never arms a drag.
* **Corners and the box.** Click A, then B (the box rubber-bands in between and is world-aligned, clamped to the build height and the border);
  a third click starts over; a drag makes the box at once. Looking at the box, **scroll up pushes that face out one, down pulls it in**
  (Ctrl 8), the looked face lit at 22% with an arrow; the box also shows faintly through terrain; a face cannot pass the opposite one.
* **Magic select** (double click): on a built block, every non-air, non-fluid, non-ground block connected through its **26** neighbours
  (a tree comes whole; a house on a meadow stops at the grass; replaceable plants such as grass tufts do not join); on a ground block
  (`enhanced:build_ground`), the 6-connected cells of that same block. Never beyond the hero's reach, never into an unloaded chunk;
  **over the limit nothing is selected** (Wanda 100,000, Billy the size of his meter; a sparse thing past 16,777,216 cells of bounds is
  refused). A double click on another thing adds it (up to 8 seeds). The result is a *mask*: Fill turns exactly those blocks into the
  material; Vanish, Change and Paint act only on them; Copy, Move and Repeat take only them. Pushing a face turns it back into a box.

### 18.3 The eleven spells

One spell is one undo step (a Paint or Land stroke from press to release is one; a Move is one for both ends). Every spell takes at least
`minSpellTicks` (10) so it is seen growing: fills rise from the ground, shapes swell from their heart shell by shell, vanishing melts from the
top. The cost (Billy) is the number of blocks it really changes; a block that already holds the result costs nothing and is not written.

| # | Spell | Styles and the details |
|---|---|---|
| 1 | **Fill** | Solid; Walls (4 sides); Room (walls, floor, roof, the inside emptied); Floor; Frame (the 12 edges); Only the empty (air and replaceable blocks). A 5×4×5 box: 100, 64, 82 placed + 18 emptied, 25, 40. |
| 2 | **Vanish** | Everything (Delete); Hollow out (all but the outer layer); Plants and trees (`enhanced:build_plants`); This kind (the kind under the picking ray). No drops, a puff of dust; a waterlogged block leaves its water. |
| 3 | **Change** | This kind; Everything (non-air); Only the outside (touching air). The **family rule**: if both blocks belong to vanilla block families, the old block's variant (stairs, slab, wall, fence, door, trapdoor, button, plate...) becomes the same variant of the new family (oak stairs become spruce stairs holding spruce planks or spruce stairs), and a variant the family lacks is left alone; otherwise the old block becomes the material. Properties are kept by name. Never makes air, never fills air. |
| 4 | **Paint** | A brush of radius 0 to `brushMax` round the block under the picking ray; every surface block under it (touching air) changes by the family rule. Billy's radius up to his side ÷ 8 (3 at rank 1), Wanda's to `build_magic_wanda.brushMax` (16). Hold the right button and sweep; one stroke is one step. |
| 5 | **Pull & Push** | The surface is the looked block and every block of the same block connected to it in the plane of the face whose front neighbour is air or replaceable (up to the magic-select cap). Scroll up pulls N layers out (copies moved N steps, only into air or replaceable cells); down pushes: the surface and the N−1 layers behind it vanish. |
| 6 | **Shapes** | Ball, Dome, Tower, Ring, Cone, Pyramid, Arch, Heart; solid or hollow (hollow by default for Ball, Dome, Tower; a Ring is always one block thick; shapes standing on the ground have no floor). The ghost sits on the looked surface; scroll = radius (Billy 1 up to (side−1)÷2: 11 at rank 1; Wanda up to `shapeMaxRadius` 512); a click plants it; for the shapes with a height the height then follows the look up and down (default Tower 2r+1, Ring 1, Cone 2r+1, Pyramid r+1). A hollow dome of radius 3 is 57 blocks, a hollow ball of radius 6 is 414 (fits Billy's first meter), radius 7 is 530 (does not). |
| 7 | **Copy** | The first right click copies the box or mask into the pocket (server side, one per player, saved in `pocket.bin` of the player's folder, travelling across dimensions); the ghost follows the crosshair; each right click stamps a copy from the ground up. A box copy brings its air, a mask copy only the thing. Never copies creatures. Scroll lifts, Alt + scroll or Ctrl + R turns a quarter, Ctrl + F flips. Billy's pocket is at most his side on every axis; every pocket at most `pocketMaxMb`. **Containers:** the pocket keeps a container's block-entity data (and so its items) only where the caster may break the block (`massEditRefuses`, `vanillaRights`, the break event once a section; a claimed vault is read as the bare block), and Billy's never (`copiesContents`). |
| 8 | **Move** | The first right click picks up, the second puts down: one spell. A **compare-and-swap per cell**: a source cell is moved only if it still holds what was captured (or what this Move wrote); a cell someone changed meanwhile is left alone and counted `kept_changed`. Block entities move with their full contents. **A full chest is spared at the target:** the target cells that are not part of the Move's own source region are checked as a Fill's are (P7: kept and counted, or spilled with `fullContainers` SPILL), and then the source stays too. **Contents are never in two places:** every moved cell that carried block-entity data is linked to its source (`UndoStep.link`); an undo gives the source its data back only if the target was undone in the same undo, a redo writes the target's data only if the source was cleared, and a redo compares the hash of the data the undo put back (`Walk.restoredHash`) with the cell's, or finds a bare block holding no items, else the cell is dropped (`kept_changed`), so a chest refilled after the undo is not wiped by the redo. With `moveCarriesCreatures`, creatures, frames, paintings and armor stands inside come along, but only if the cells they stand in and on were not kept, the caster may build there and there is room at the end. A Move is never a *lite* step (18.9): over the memory budget it is refused, held or not. |
| 9 | **Repeat** | Copies in a row in the direction looked: copy i sits at (size along the direction + gap) × i; scroll = copies (Billy up to 8 +4 a rank; Wanda `repeatMax` 0 = no count), Alt + scroll = gap 0 to 16. |
| 10 | **Land** | Raise, Lower, Smooth: a brush of pulses every 4 ticks (`REACH` 8). A column of the brush disc moves by a falloff (1 − (d/r)²)² of a block a pulse; the surface is a ground block with something loose on it; a column moves whole or not at all (trees and houses stand, never float); Lower sinks only into ground or empty cells; the filler of a rising column is the block under it when that is plain ground, never an ore; Smooth moves a column toward the average of the square round it by at most one block; creatures on a rising column are lifted. Billy pays per pulse; never paid with items. |
| 11 | **Grow** | Swell, Shrink, Bloom: a brush of pulses every 4 ticks like Land (hold the right button, sweep; scroll = radius, Billy's side ÷ 8, Wanda's `brushMax`; Alt + scroll = style; one stroke is one step). A pulse (`GrowBrush.Pulse`, a `BrushPulse` like Land's) is planned against the world as it is, so nothing cascades inside one (a rock swells one layer a pulse), into one-cell columns (a tall flower: two) that the job writes whole or not at all through Land's machinery (the write-ahead gate, the rules, the settle pass). The brush is the sphere d² ≤ r² + r, nearest the middle first; *solid* means a full block with no block entity that magic may touch, and a block entity is never swelled from or shrunk. **Swell:** every air or replaceable cell of the sphere that touches a solid block of the sphere becomes that block (the most common solid face neighbour, ties to the one below). **Shrink:** every solid cell of the sphere with air on two or more faces vanishes. **Bloom** (Westview's garden), on cells with an empty cell above: dirt, coarse dirt and rooted dirt become grass; on grass and moss, by a 5-bit hash, 1 in 32 a tall flower (two cells), 4 in 32 a small flower, 8 in 32 short grass; netherrack becomes crimson nylium (warped in a warped forest) with roots on 8 in 32; mycelium sprouts a mushroom on 8 in 32. Each cell's choice is a hash of (the stroke's seed, x, y, z), the seed fresh per stroke from the client, so the server grows what the ghost showed; a Bloom pulse that plans nothing says "Nothing grows here" once a stroke. Billy pays per pulse for the cells changed; never paid with items; no TNT, lava or fire count. |

### 18.4 The block, magic or materials

The block in the main hand (else the off hand) is the material, in the state it would be placed in by hand (`getStateForPlacement`
with the caster's look). Middle click takes exactly that block (the client sends only where; the server reads the state). **Mix** weights the
hotbar's blocks by stack size with a hash of position and a seed carried by the cast, so the ghost shows the mix that will be built. The **Blocks** page
of the wheel picks any block magic may make. A water bucket makes water, a lava bucket lava (Wanda only). Two-block things (doors, beds, tall
flowers) are never the material but are carried whole by Copy, Move and Repeat, and no spell cuts one in half. By default blocks come from magic;
with `requireMaterials` a Survival player pays one plain item per block (an unaffordable spell is refused whole: "You need 37 more Oak Planks"), vanished blocks
go into the inventory, undo gives back and takes back; Creative never pays; Land and Grow are never paid with items (they move and grow the ground).

### 18.5 What the builder sees and hears

* **The box**: 12 glowing ribbons (a wave for Wanda, a flickering arc for Billy), faintly drawn again through terrain (`seeThrough`), faces at 8%, the
  looked face at 22% with an arrow, corner runes, a label like `7 × 4 × 5 · 140` (`counting...` for a huge plan); the magic-select glow.
* **The ghost** is the result, in real block models, translucent (alpha 0.45) and tinted in the hero's colour, built off the main thread by the same
  pure planner as the server's; the detail cap `ghostDetail` (32,768) draws only the outside past it and a tinted box past four times that. **The
  server's marks** (preflight, at most 4 a second and 2 ms, **no event posted**): red cross = protected, yellow = a chest that stays, white = someone
  stands there, grey = not loaded; the ghost round them is grey; a plan the server refuses whole (an arena, or a plan whose every checked cell is left: the refusal `NOTHING_CAN_CHANGE` = 41, `Preview.nothingCanChange`, which needs the whole plan
  checked, not a sample) is a grey ghost and the right button fizzles before a job, a cast sound, a charge or a history step exist; the `left.*` line says what stayed.
* **The badge** next to the crosshair: material, size, spell and style, Billy's meter with the cost of the ghost as a darker chunk (orange with a clock when
  more than he has now, red and a grey ghost when more than his whole meter, a scarlet stripe and "½" with Mom), Wanda's ∞. **Rings**: the hold ring (20 ticks, or 40
  over a million blocks, red for removals) and the progress ring. A done pop `✓ 140` for 40 ticks. The crosshair becomes a rune; the screen's edges take a faint tint.
* **The lines** the game says (`message.enhanced.build.*`) are at most 56 characters, each `%s` counted as 6 (`DataTests.everyBuildMagicLineIsShortEnoughToReadWhole`), plural-safe (a count after a colon, or `block(s)`), so they read whole at 854×480; the action bar's line is moved left until it ends before the hint strip (`BuildHud` on the overlay message's layer events, with access-transformer entries for `Gui.overlayMessageString` and `overlayMessageTime`). The hold rows of the strip say the spell's own verbs (Repeat keeps copies and gap; raise, lower, smooth; grow, shrink, bloom), and a scroll gesture keeps the face it pushes (notches within 20 ticks, the look within about 5 degrees, the same box).
* **The hint strip** (bottom right): three to five rows of little pictures of the input and one or two words for the current spell and state
  (pure `BuildHints.lines`); **tips** (a three-second picture the first times a spell is chosen and when something does nothing; `tips`).
* **The wheel** (18.1): a ring of eleven 40×40 pictures clockwise from the top in the order of the table, the centre disc *Start/Stop building*, undo and redo with their
  counts, the hotbar row with Mix and Blocks, the style row, and over it Wanda's "Chaos Magic ∞" or Billy's name, rank, stars, meter and the way to the next rank. The **Blocks** page
  lists the blocks of four creative tabs (Building, Coloured, Natural, Functional) that magic may make (`BuildClient.conjurable`, the test the mix and the server use; with
  `requireMaterials` a Survival player sees only what the inventory holds), 24-pixel icons in a grid with a scrollbar, a Recent row (the last 16 used this session), a search box over
  all four tabs and tooltips; a click sends `PICK_BLOCK` with the registry id and the material lasts until the hotbar slot or the held item changes.
  counts, the hotbar row with Mix and Blocks, the style row, and over it Wanda's "Chaos Magic ∞" or Billy's name, rank, stars, meter and the way to the next rank.
* **Everyone else** sees Wanda's eyes glow and Billy's crown stay, hand wisps or sparks, the builder's thin box within 64 blocks (`showOthers`), the
  cast gesture (`WANDA_HEX_TWEAK`, or `WANDA_HEX_RISE` from 100,000 blocks; `BILLY_BOLT` with a Hex Bolt arc from his hand), Mom's scarlet thread, a frame and a ripple
  sweeping the spell in its work order, server particles (at most 64 a tick), the undo ripple backwards, and Billy's rank-up (the crown flares white, runes spin; within 32).
* **Sounds.** Eight events of Build Magic, each with a file of its own in `assets/enhanced/sounds/` (the top level; the 3.0 voices are in `sounds/sfx/`), synthesised from scratch like
  all the mod's sounds, registered in `ModSounds`, `sounds.json` and both lang files (the subtitles): `build_on_wanda` ("Reality bends", 1.2 s, a breathy rising A minor chord with a shimmer),
  `build_on_billy` ("A spellbook opens", 0.8 s, a paper flutter and two glassy notes), `build_cast_wanda` ("Chaos magic rewrites reality", 1.0 s), `build_cast_billy` ("A spell crackles", 0.8 s),
  `build_hum` ("Magic weaves", 1.0 s, every 20 ticks at the front of a working spell, pitched lower for Wanda and higher for Billy), `build_fizzle` ("A spell fizzles", 0.4 s, only the builder
  hears it), `build_undo` ("Reality snaps back", 0.8 s, the cast chord backwards) and `build_rank_up` ("Billy learns new magic", 1.6 s, five rising notes and a sparkle). A Scarlet Witch spell of
  `BuildSounds.HUGE_CAST` (100,000) blocks or more plays her `HEX_RISE` instead of `build_cast_wanda`; the cast, the hum and the undo are played at the caster (the hum also at the spell's front when that is more than 16 blocks from him); the done sound is the game's `AMETHYST_CLUSTER_PLACE`, and the builder's UI sounds
  (corners, clicks, the hold ring, the wheel) are the game's own. The ninth new file is not Build Magic's: `apocalypse_crit` ("Critical hit on Apocalypse", 0.5 s, a heavy glassy crack with a low gong under it),
  played by `CritSound`. All nine are heard in game with their subtitles. **Pictures:** 45 PNGs in `textures/gui/build/`: a 32×32 picture for each spell (`fill`, `vanish`, `change`, `paint`,
  `pull`, `shape`, `copy`, `move`, `repeat`, `land`, `grow`), one for each of the nineteen styles (`<spell>_<n>.png`, n = the style's ordinal: `fill_0` to `fill_5`, `vanish_0` to `vanish_3`, `change_0` to
  `change_2`, `land_0` to `land_2`, `grow_0` to `grow_2`), one for each of the eight shapes (`shape_ball`, `shape_dome`, `shape_tower`, `shape_ring`, `shape_cone`, `shape_pyramid`, `shape_arch`,
  `shape_heart`), and seven 16×16 ones (`mouse_left`, `mouse_right`, `mouse_middle`, `mouse_wheel`, `dice`, `door`, `blocks`). The client draws the mouse pictures of the hint strip itself, so the four
  `mouse_*` files are spare; where a picture is missing it draws a stand-in.

### 18.6 Billy: an apprentice who learns

A spell costs one meter point per block really changed (a Move one per block moved; copying and looking are free); the meter refills continuously (also logged out), stored as
`(value, game time)`. A spell he cannot pay **does not start**; the COUNT phase reserves the cost, the end refunds what protection, bodies or chests left unchanged; undo
gives back `cost × restored ÷ changed` and takes back that practice; redo pays again. Every block of magic is **practice**; the rank never goes down.

| Rank | Practice | Meter | Refill | Side | Reach | Undos | Repeat | Brush |
|---|---|---|---|---|---|---|---|---|
| 1 Apprentice | 0 | 512 | 16/s | 24 | 24 | 10 | 8 | 3 |
| 2 Spellcaster | 2,000 | 1,024 | 24/s | 32 | 32 | 12 | 12 | 4 |
| 3 Sorcerer | 8,000 | 2,048 | 32/s | 40 | 40 | 14 | 16 | 5 |
| 4 Wiccan | 30,000 | 4,096 | 40/s | 48 | 48 | 16 | 20 | 6 |
| 5 Enchanter | 60,000 | 6,144 | 56/s | 56 | 56 | 18 | 24 | 7 |
| 6 Mystic | 120,000 | 8,192 | 72/s | 64 | 64 | 20 | 28 | 8 |
| 7 Wizard | 220,000 | 12,288 | 96/s | 72 | 72 | 22 | 32 | 9 |
| 8 Archmage | 380,000 | 16,384 | 128/s | 80 | 80 | 24 | 36 | 10 |
| 9 Reality Weaver | 600,000 | 24,576 | 160/s | 88 | 88 | 26 | 40 | 11 |
| 10 Chaos Master | 1,000,000 | 32,768 | 192/s | 96 | 96 | 28 | 44 | 12 |


**(3.2) Ten ranks** (`BillyRanks.MAX_RANK` 10; `maxRank` 1 to 10, default 10; ranks 1 to 4 keep their 3.1 numbers exactly, so a rank-4 Billy with 3.1 practice continues smoothly; a server file written by 3.1 holds `maxRank` 4 until its owner changes it). The practice thresholds are the options `rank2Practice` to `rank10Practice` (`BuildConfig.BILLY_PRACTICE`). Ranks 5 to 10 continue by tables:
meter = `meter` × {1, 2, 4, 8, 12, 16, 24, 32, 48, 64}; refill = `refill` × {1, 1.5, 2, 2.5, 3.5, 4.5, 6, 8, 10, 12}; side = reach = base + 8 (rank − 1); undos = base + 2 (rank − 1); repeat = base + 4 (rank − 1); brush = side ÷ 8; magic select = the meter. Rank 10 is far below Wanda (a side of the whole world, 256 reach, 100,000 selection, no meter, brush 16);
a full meter at rank 10 refills in about 171 s. Rank-up effects, sound and message are rank-generic (`RANK_UP`, `rankUp(rank)`; names `hud.enhanced.build.rank.1` to `.10`: Apprentice, Spellcaster, Sorcerer, Wiccan, Enchanter, Mystic, Wizard, Archmage, Reality Weaver, Chaos Master; in Hebrew שוליה, מטיל לחשים, מכשף, ויקאן, קוסם, מיסטיקן, אשף, אשף-על, אורג המציאות, אדון הכאוס).
His Hex Beam and Hex Barrier (6.4, 6.5) and the radius of his ward (20.2) grow with the same rank. For ranks 1 to 4: meter = `meter` × 2^(rank−1); refill = `refill` × (1 + (rank−1)/2); side = `maxSide` + 8(rank−1); reach = `reach` + 8(rank−1); undos = `undoSteps` + 2(rank−1);
repeat = `repeatMax` + 4(rank−1); brush = side ÷ 8; magic select = the meter. `maxRank` caps it. In ranks 1 to 4 each rank is the same step up from the Apprentice's five numbers (the meter doubles, the refill gains half of the first, side and reach 8, undos 2, Repeat 4); an operator sets a rank with `/enhanced build rank` (18.12). A rank-up is a moment (the crown flares, runes, a fanfare to everyone near, the meter fills as a gift).
**Mom's help**: a Scarlet Witch (alive, not a Spectator, in his dimension) within `momRange` (16) when he casts halves the cost (`momCostFactor` 0.5, rounded up), with a scarlet thread and "½"; his practice counts the real blocks.
Limits and what he is told: the meter now (`refilling`), the whole meter (`too_big`), the side (`too_wide`), the reach (`too_far`), undo history, Repeat, one spell at a time with a pause of `castPause` (10 ticks), TNT, lava and fire never
(`volatile`), copies of containers come out empty (`copiesContents`), Creative changes nothing (`limitsInCreative`).

### 18.7 Wanda: limitless, and the safety under it

No meter, no cost, no ranks, no size limit, every spell and style, shapes to radius 512, `undoSteps` 0 (as many as the memory and disk caps hold), `repeatMax` 0 (no count), her copies keep what is inside chests,
lava; reach 256, and her box corners are not limited by reach. Up to `maxQueued` (4) spells wait behind the one at work and run in turn. Inside her own Hex her spells get `hexBoost` (2) shares of the tick. Only technical safety:
every spell is a **job sliced inside the level tick** under one budget for all spells (`millisPerTick` 8 ms, `blocksPerTick` 20,000 writes with reads counting a quarter, `sectionsPerTick` 16; all
halved while the average tick is over 45 ms, with a floor of 1 ms and 256 writes), fair round robin, a Hex-boosted job counting `hexBoost` times; a job touches only chunks **loaded when it was cast** (kept loaded by tickets, at most `maxTicketChunks`; none is ever generated); at most `volatilePerSpell` (4,096) TNT, lava
or fire; `brushMax` 16 for a technical reason (a pulse every 4 ticks of radius 16 is about 17,000 blocks, which the budget follows; at 32 it would be 137,000); `maxBlocks` (0 = none), `operatorsOnly`, `deniedDimensions`.
A server stopping finishes the spells whose blocks left add up to at most `finishOnStopBlocks` (200,000), the largest stopped first (undoable).

### 18.8 The rules every cell obeys

One rule book (`build/BuildRules`, `ability/BlockRights`) for every spell, undo and redo, for both heroes. **Once per cast**: the switches and `operatorsOnly`; a Scarlet Witch or Billy, alive, in build mode; not Entombed; not
Adventure; the dimension allowed; a request rate of `maxRequestsPerSecond` (40); anchors within reach + 2 and Billy's side; the material makeable (never `build_unconjurable`; never two blocks tall; Billy never `build_volatile`); no island being cleared after a crash; Billy's pause; Wanda's queue and `maxBlocks`;
the hold flags (`holdToConfirmBlocks` 64 → 20 ticks, `hugeSpellBlocks` 1,000,000 → 40 ticks); then the COUNT phase decides the meter, the materials and the volatile cap. **Per cell, cheapest first**: loaded; inside the world and the border; the new state differs;
not `build_untouchable` and not a block with negative hardness; **not in the protected arena** (`BossFights.protects`, and with `allArenasProtected` the cylinder of any running fight, and the ground the island hides, and with `hexIsAClaim` a Hex that is not the caster's); no body there that may not be lifted (another player with PvP off, a boss or fight entity,
a creature Mjolnir pins); the old block entity holds no items (`fullContainers` SKIP leaves it, SPILL drops them; a Move carries it from its own source region, and never lands on a full container, 18.3); vanilla rights (`mayBuild`, `mayInteract`, `blockActionRestricted`); the claim probe once per 16×16×16 section; the break and place events per block (`postEventsPerBlock`);
materials. The player is the caster, or a fake player of his profile when he is away. Writing: `setBlock` with clients told, no neighbour reaction, no drops; `Clearable.tryClear` before a block entity goes; a door, bed or tall plant is written in both halves; falling blocks a spell places are held where they were put (`holdGravityBlocks`); afterwards
the outside is **settled** once (shape updates, side effects recorded so an undo brings a fallen torch back). Creatures inside new solid blocks are lifted on top (`liftCreatures`; another player only with PvP on). With PvP off a cell where another player stands is also kept free of anything that
would hurt him: not only blocks with a collision shape but fluids, TNT, lava, fire, powder snow, cobweb, berry bushes, magma, wither roses and campfires. A neighbour with a block entity that would
become another block (a sign, a banner) is left alone by the settle pass (vanilla breaks it, with its drop, in its turn). A column walk gives up a whole unloaded chunk column at once; a plan
with no loaded chunk, or a box wider than a million columns of which under 1% is loaded, ends at once with every cell counted unloaded.
Copies keep the block-entity data of `build_copies_data` types, Billy's without `build_holds_items`, and only where the caster may break the block (18.3). Never touched: bedrock-like blocks, portals, spawners, vaults, Mjolnir as a block; never made: budding amethyst, the dragon egg, infested blocks, frosted ice, Spider-Man's web and a few more; the volatile cap applies to copies too.
Witches' Road doors are ordinary blocks to magic.

### 18.9 Undo, redo and the history

Ctrl + Z in order: drop Wanda's newest waiting spell; else stop and rewind a working one (it jumps the queue); else undo the newest step (partly undone ones first); else "Nothing to undo". Ctrl + Y redoes. **Safe undo**: a recorded cell is restored only if it still holds exactly what the spell put there (the same state and, with data, the same hash); changed cells are
left and dropped from the step; cells that became protected stay in the step, marked partly undone, to be tried again. A Move's cells are linked to their sources (18.3), and a redo checks content, so a block entity is never in two places. Undo cannot bring back what the world did itself afterwards (flowed water, redstone), wandering creatures, or spilled items.

**The files** (`build/UndoFiles`, `WriteAhead`): `<world>/data/enhanced/build/<uuid>/` holds `history.dat` (gzip NBT, written to a temporary file, forced and moved into place atomically; it lists the `done`, `redo` and `open` steps with file, counts, state and box),
`step-<file>.bin` for each step (a header, then blocks of `int length, int crc32, gzip NBT`) and `pocket.bin` (`Pocket.toTag`). A step records the spell, the dimension, the cost and practice, and per 16×16×16 section the old and new states (palette indices), the block-entity data, and the side effects.
**Written ahead**: while a spell works its file holds *records* (`rec` blocks: per section the cells, the old and new states, the old and new data). `WriteAhead` is a second cursor of the plan that reads the old state and the state the spell will put, up to 32,768 cells ahead of the
writing cursor and only in the loaded chunks of the job's set, in batches of 256 doubling to 4,096; `BuildJob.gate` runs before every write (the target cell, a door or bed partner, a Move's source, every cell of a Land or Grow column) and answers READY when the cell's batch is on disk, WAIT (the slice yields and
nothing is written) or CHANGED (the cell no longer holds what was read ahead: left and counted `kept_changed`). Compression is done on `Util.backgroundExecutor()`, appends on `Util.ioPool()`, one ordered chain for each file and each player. When the step is sealed (or an undo or redo ends) its file is replaced atomically by
the whole step (`sec` blocks and an `end` block with the state, Billy's cost, the side effects and the carried frames). `LevelEvent.Save` forces the open files and writes the indexes; so do the seal, a logout and `ServerStoppingEvent` (LOW, after the scheduler). A torn tail (a bad crc) is cut; an unreadable index, pocket or step file is set aside as `.corrupt`; a
state whose block no longer exists reads as "keep". **After a crash** the index lists the spell under `open`: at load its file is read as records into an INTERRUPTED, *recovered* step whose undo puts a cell back when it holds the block the spell was to put there, and the player gets "Your last spell stopped when the world closed" at his next login.
**Reading**: login reads the history and the pocket in the background and installs them on the main thread; a step is only *listed* until Ctrl + Z or Y needs it (loaded in the background, the key asked again); `/enhanced build undo` loads steps in order. `undoSurvivesRestart` off writes nothing (in tests nothing is written unless a test opens a folder); an absent player's memory-only history is dropped after 30 minutes.

**Caps** (`build/UndoCaps`, looked at once a second and after each seal): steps (Wanda `undoSteps`, Billy by rank); `undoMemoryMb` 64 and `undoDiskMb` 512 for one player, `serverUndoMemoryMb` 512 and `serverUndoDiskMb` 4,096 for all. A step can leave memory only when its file holds it as it is and no job works on it; past a memory cap the oldest steps leave memory (the heaviest player's first for the server's cap) and stay listed;
with nothing written the oldest are forgotten instead. Past a disk cap the oldest step is forgotten (the heaviest player's for the server's) and its player is told; the newest step of a history is never lost to a disk cap. **Room for a working step** (`UndoCaps.room`, asked in PREPARE when the size is known): the step is held *whole* (about 14 bytes a cell and 12 KiB a section; `FULL`) when that fits `undoMemoryMb` and half of `serverUndoMemoryMb`
together with the steps other jobs work on and the record fits `undoDiskMb` (0.6 byte a cell and 700 bytes a section); *lite* (`LITE`) when it does not but the history is written to disk and the record fits: the step then keeps one bit a cell and the hashes of block entities (`UndoStep.makeLite`), the settle pass walks the bits, and at the seal only an `end` block is appended, so
the file's records are the step, listed and read back as a recovered step when undone (about 1 KiB a section in memory: 61 million cells about 16 MB); otherwise `NONE`: "Too big to remember: this one can't be undone. Hold to cast it anyway" (`NO_UNDO_ROOM`), and with the long hold the spell runs with a lite step that is never kept (no journal, no file). A Move is never lite, because its compare-and-swap needs states: over the budget it is refused whatever is held. Move and Repeat pockets are capped by `pocketMaxMb` too. At the defaults a Wanda spell over
about 4 million cells is lite. A server stopping finishes what `finishOnStopBlocks` allows and seals the rest as INTERRUPTED (undoable, with a persisted login line).

### 18.10 Network, config and tags

Network protocol **"5"** in 3.1 (a 3.0 client could not join a 3.1 server; 3.2 raised it to "6", section 22): `BuildCastPayload` (the recipe: spell, style, region, corners, seeds, anchor, size, turn, flip, material kind, seed, stroke; at most about 140 bytes; the client never sends a block state), `BuildActionPayload`
(mode, undo, redo, pick, pick block, hand, stroke end, view, pocket), `BuildVerdictPayload`, `BuildFxPayload`, `BuildPocketPayload` (in parts of at most 31 KiB) and, for Magneto, `MagnetPullPayload`; two attachments, `BUILD_DATA` (saved: Billy's meter, rank, practice, the pocket) and
`BUILD_VIEW` (not saved: mode, spell and box for the people who see him). The server config sections are `[build_magic]`, `[build_magic_wanda]` and `[build_magic_billy]` (section 11 and the wiki's Server Configuration page; 58 options), the client `[build_magic]` has five. The datapack tags: `enhanced:build_untouchable`, `build_unconjurable`, `build_volatile`, `build_ground`, `build_plants` (blocks)
and `build_copies_data`, `build_holds_items` (block entity types).

### 18.11 What it does with everything else

* **Boss fights.** No spell and no pull changes a block of any running fight's arena (Thanos's too); a spell working when a fight starts stops; the island in the sky and the ground under it are never touched; an undo of an older spell is retried after the fight. Build Magic never borrows (that is telekinesis, section 2).
* **Players and creatures.** With PvP off nobody is buried or lifted by another's spell and a Move never carries a player; a creature Mjolnir pins is never lifted or carried; a lifted-by-telekinesis block is an entity, so spells pass it by; telekinesis never moves a creature a spell carries.
* **Spectator, Adventure, Entombed.** Spectator and Adventure end build mode and stop a working spell at its next slice; Entombed refuses new casts and lets working ones go on.
* **Hex.** Hex walls are not blocks; Wanda's own Hex speeds her spells; another Hex is a claim only with `hexIsAClaim` (which the Magnetic Pull reads too).
* **Logout and death.** Build mode turns off, the box is forgotten, a working spell finishes with the fake player, "while you were away" at the next login; the history, pocket and meter stay.
* **Performance.** Planning is O(1) on the cast (a lazy iterator); preflight at most 2 ms; vanilla sends every changed block to every watching client (about 4 bytes a block), so a hosted server lowers `blocksPerTick`.

### 18.12 The operator's commands

`BuildCommands.node()` is the `build` branch of `/enhanced`, which already needs permission level 2; every argument that names a player is an online player (`EntityArgument.player()`), and none of them needs him to be a builder at the moment.
`undo <player> [steps]` (1 to 1000) takes `UndoHistory.toUndo(steps)` (a partly undone step first, then the newest that can be undone, and so on) and queues each step as a replay job of the player behind what he is doing, with the safe-undo rule and without the build-mode check; a step that left memory is read back
in the background first (the next waits for it, so the order stays) and one whose file cannot be read is skipped; the answer is "Undid N spells of X". `stop [<player>|all]` is `BuildScheduler.stop` for each named player (everyone who has a job when none is named): the spells stop where they are and the queues are dropped; what was done stays and can be undone. `list` prints each job's player,
spell (or `undo`/`redo`), dimension, percent and the cells it has walked and has left. `history <player>` prints every step, the ones Ctrl + Z works on and then the ones Ctrl + Y does again, oldest first. `rank <player> [1-10]` and `meter <player> <value>` answer "is not Billy" for anyone else; `BillyMagic.setRank` clamps to 1 to `BillyRanks.MAX_RANK` (10), sets the practice to at least the threshold of the rank (exactly it when the rank goes down, so his next spell does not undo the change) and fills the meter;
`setMeter` clamps to 0 and the top of the rank that applies. `forget <player>` clears the history (memory and `data/enhanced/build/<uuid>/`) and the pocket. The texts are `commands.enhanced.build.*` in both lang files.

### 18.13 Tests and the checks in game

About 120 game tests in nine classes of the build package, each with batches of its own because jobs, histories and sessions are server-wide and `TestKit.bossFixture` clears them: `BuildMathTests` (the pure code: shapes, styles, the family rule, the mix, the planner, magic select, a recipe clamped to the hero's limits, Billy's ranks as arithmetic), `BuildLimitsTests` (Billy's meter and "not yet" and "never", Mom's help, Wanda's technical caps, `requireMaterials`), `BuildGrammarTests` (the mouse grammar and the hint strip, pure),
`BuildJobTests` (the engine: pace, sections, fair shares, Ctrl + Z on waiting and working spells, the stop at server shutdown), `BuildRulesTests` (the rule book of 18.8), `BuildSpellTests` (every spell and style as a hero casts it), `BuildBrushTests` (Land and Grow), `BuildUndoTests` (the files, the write-ahead, the caps, the pocket, the commands) and `BuildReviewTests` (what the reviews found: the Move and the chest,
the redo, the memory budget, a job that throws, a claimed vault, PvP-off hazards, a sign beside a vanished wall, unloaded columns). `V31CrossTests` crosses Build Magic with the Magnetic Pull, the island, Mjolnir, pinned creatures, held blocks and the boss telekinesis. The release was also played: a dedicated server and two clients (Billy and Wanda) went through the whole of D-B, every spell and style, a 200,000-block fill at 20 ticks a second
(a mean tick time of 1.7 ms rising to a peak of 12 ms), the undo of all of it, 101 undos in a row, a server restart with seven steps on disk, Hebrew, the sounds with their subtitles, and a Move while a sister emptied the chest in it.

### 18.14 Wanda's Instant level **(added in 3.2)**

Eilon asked for "an extra level where the structures she creates appear instantly, with no loading or waiting". **Instant** is a switch in build mode, for the Scarlet Witch alone: while it is on, a spell, an undo and a redo write **all their cells in the same tick** they are cast.

* **State.** `BuildSession.instant` (the server's truth), mirrored to the client in `BuildData.instant` (a moment-state field, not saved; a trailing boolean of the stream codec) so the HUD badge and the wheel button read one source. Changed by `BuildActionPayload.INSTANT` (9, value 1 or 0); refused unless Scarlet Witch, in build mode, with `build_magic_wanda.instantMaxBlocks` above 0
  (`Limits.instantMax()` is 0 for Billy: "Instant is the Scarlet Witch's magic alone"). It turns off when she leaves build mode or her power changes.
* **Controls.** The key `key.enhanced.build_instant` = **Ctrl + I** in the `BUILD` context (I for Instant; it shares no key with vanilla or Enhanced, so the Controls screen adds no red mark; the name is "Build Magic: Instant (Scarlet Witch)"), and an **Instant** wheel button under Redo (Wanda only, shown while `instantMaxBlocks` is above 0). The HUD notes row shows a gold badge "⚡ Instant".
* **Engine.** A spell cast with Instant on gets `BuildJob.instant`; its `pace()` is unbounded and the read-ahead reads a whole window a slice. `BuildScheduler.runInstant(job)` (called at the end of `BuildMagic.cast` and of `replay` when the job is at the head of her queue; a job behind another runs its instant turn when it becomes head, in `turn()`) steps the job with an unbounded budget (writes, sections, no `minSpellTicks`,
  no budget sharing) inside the calling tick. It sends no progress, sync or hum. The write-ahead journal still gates each cell (the loop waits for the writer thread, parking 100 µs, while the guard allows). Undo and redo are instant too when her switch is on (and the step has at most `instantMaxBlocks` cells).
* **Limits.** Above `instantMaxBlocks` (**250,000**; 0 to 100,000,000; 0 = no Instant level) the spell runs at normal sliced speed from the start and she is told ("Too big for Instant (N of M): normal speed"). A wall-clock guard `instantMaxMillis` (**5,000**; 100 to 50,000; never more than a third of a dedicated server's `max-tick-time`) drops the rest of a slow spell back to normal speed ("Too slow for Instant: the rest goes at normal speed"). **(Review fixes)** All Instant spells of one server tick share `instantTickMillis` (**2,000**; 100 to 50,000: a static counter reset in `BuildScheduler.startTick`; an Instant turn gets the least of its own allowance and what is left; when nothing is left the job goes to normal speed at once with the same message and writes nothing that tick), and a step that ends `waiting()` (a chunk of the spell's set, a Move's source or an undo's chunks are not loaded) calls `job.fellBehind()` at once instead of spinning: normal speed, the tickets load the chunks. A progress payload with a real order replaces the POP order of a run that fell behind, so the rest draws its ripple. Only the journal wait (the writer thread) still parks.
  Measured on the GameTest server (a fill of stone into air, loaded chunks): 8,000 cells 0.12 s; 125,000 cells about 0.3 s (undo in memory) or 0.44 s (undo on disk); **1,000,000 cells 2.1 s with the undo in memory and 4.1 s with `undoSurvivesRestart` (the default; the cost is the journal's read-ahead and recording on the main thread)**. The lead's default of 1,000,000 was lowered to 250,000 (about a second) by the rule "lower it if above about 2 s a million".
* **Effects.** `BuildFxPayload.POP` (order 4, START and DONE with no progress): the client flashes the box for 8 ticks and sparks fly off its surface instead of the ripple. The brush spells (Paint, Land, Grow) already keep up with the cursor and are not affected.
* **Rules unchanged.** Every cell still goes through `BuildRules` and `BlockRights`: arenas, claims (events per block), Hex claims, loaded chunks only, the undo caps and lite steps, and the hold for removing or replacing more than 64 blocks or `hugeSpellBlocks`.
* **Tests.** `BuildInstantTests` (batches `build_instant_cast`, `_limit`, `_undo`, `_rules`, `_billy`, `_time`).

---

## 19. The Witches' Road: three trials (added in 3.2)

Eilon asked for the Witches' Road to be a place with **three challenges** that open one another: a **parkour**, a **structure to copy** so that it looks exactly like the one shown, and a **huge ice-boat race**; at the end the
player chooses Billy's power or Tommy's and is taken back out. In 3.1 the Road was only a door with an instant choice (section 6.1). The player's guide is the wiki page *The Witches' Road*. Package `road/` (common
and server code; a few client classes for the screen), config `config/WitchesRoadConfig` (`[witches_road]`), the dimension in the data pack. Everything a trial needs is **built by code, for one player, and taken down again**,
in the style of the Sokovia island and the boss arenas.

### 19.1 The flow

1. A player with **no power**, in Survival, right-clicks a Witches' Road door **holding a Scarlet Witch Gene** (3.1 up to here: the ring of blue-green light runs for 2 s, the door opens).
2. The door asks "Seekest thou the Road?" with one big button, **Walk the Road**, and Cancel, and says which trial waits ("Trial 2 of 3"). The same screen (`WitchesRoadScreen`, mode `CHOOSE`) offers Billy and Tommy
   in the hall at the end of the Road, and at the door to a player whose progress says all trials are won (he left the hall without choosing). With `witches_road.enabled` false the door is the exact 3.1 door (instant choice).
3. **Walk the Road**: the server builds the player's trial in a private cell of the dimension `enhanced:witches_road` (a few ticks; the door pours light meanwhile), then moves him there. His entry (dimension, position,
   rotation, game mode) is saved. The Gene stays in his inventory the whole time (if he throws it, it jumps back, 19.5).
4. Trial 1 (**Parkour**) → a finish platform with a gate → stepping into the gate builds trial 2 in a **new cell**, moves him, and clears the old cell; trial 2 (**Copy the Structure**) → gate → trial 3 (**Ice-Boat Race**) → gate → the **hall at the end of the Road**, where he chooses Billy or Tommy (19.10).
5. Each success unlocks the next trial (`completed` in the saved progress). A trial can be left at any time (the exit door on the start platform, or `/road leave`): he returns to where he entered with his Gene, his progress is kept,
   and the next time he starts the trial he had reached, at his last checkpoint.
6. Nobody with a power walks the Road ("You already walk your own road"); the heroes' Walking the Road (a door without a Gene, 6.1) is untouched. Creative and Spectator players are refused ("The Road tests only those who can fall").

### 19.2 The place

* **The dimension** `enhanced:witches_road` (data pack `dimension/witches_road.json`: a flat generator with one layer of air, the biome `enhanced:witches_road`; `dimension_type/witches_road.json`: effects `minecraft:the_nether`, so a constant coloured fog
  and no sky, no skylight, ambient light 0.25, `min_y` 0, height 320). The biome has a dark teal fog `0x0B3A44`, no spawns, warped spores and the ambient, mood and additions sounds of the Soul Sand Valley.
* **Cells.** Per-player instance cells on a grid: cell *i* has its origin at x = (*i* mod 64) × 2,048, z = (*i* ÷ 64) × 2,048, y = 96. Neighbours are at least 1,400 blocks apart whatever the course (a course may use ±450 blocks round its origin;
  `RoadCanvas` refuses more), so nobody sees another cell. The grid fits 4,096 cells; `maxPlayers` (32) bounds the cells in use.
* **Built by code** at entry (`RoadChallenge.build` draws on a `RoadCanvas`; sliced by `buildBlocksPerTick` 20,000, a first touch of a chunk counts 300), removed at exit or logout (`RoadSweeper`, sliced by `removeBlocksPerTick`). The canvas records every chunk and the Y range
  it touches and that record is saved in the **cell ledger before the first block is placed**, so a crash leaves nothing unaccounted: at the next start every ledger cell is an orphan and is cleared. Chunks are held by region tickets
  (`ModTicketTypes.ENHANCED_ROAD`, keyed by the owner) from the moment the cell is planned until it is cleared (the pattern of `BossFight.begin`/`finish`). Entities are discarded **after** the blocks (a filled chest would spill its items otherwise) in a second pass 40 ticks later (immediately at a server stop), with the tickets held until then, because the entities of an unloaded chunk load asynchronously; a chunk that throws is logged and the rest go on, and a failed chunk is tried 3 times (a cell that cannot be cleared stays in the ledger and is never given out again; `RoadSweeper.injectFault` is the test hook).
* **One live instance per player** (two while he moves from one trial to the next). Nobody shares a cell: no friends in the trials.
* **Saved data** `enhanced_witches_road` on the overworld's data storage (`RoadSave`): progress by player UUID, and the cell ledger. Every mutation marks it dirty; it survives logout, restart and crash. **The Road's world border is its own** (`RoadTrials.ensureBorder`: centred on 0,0, 59,999,968 wide, set when a trial is planned and checked every second), because the overworld's `/worldborder` is copied to every dimension and would otherwise shut a far cell out. The sweeper keeps the ledger's cells while the Road's level is missing (a 3.1 world copy; the dimension appears by itself at the first start).

### 19.3 Progress

`RoadProgress`, per player UUID: `completed` (trials won, a prefix: the next to play is trial `completed`), `salt` (changes the course; 0), `checkpoint` (index in the current trial's layout; 0 = the start), `inside` (true from the teleport in to the
teleport out), the entry (dimension, x y z, yaw, pitch, game mode), counters (falls, trials started) and `data`, a `CompoundTag` per trial id for the trial's own memory (the boat race's best time). The entry also remembers the Road door he opened (`Entry.door`, optional in the codec). The structure trial's put-aside backpack is **not** here: it is the player attachment `enhanced:road_stash` (`ModAttachments.ROAD_STASH`, saved with the player, copied on death).
**The course seed** = mix(player UUID, trial id, `salt`, `courseSeed`): the same course for the same player every time (he can learn it), another for another player; `/enhanced road reset <p> newcourse` raises `salt`; so does closing the Road after the ending (`closeRoad`: progress forgotten, `salt` + 1).

### 19.4 Failure, death, abandon, login, crash

* **Falling.** Below the checkpoint's `failY` (the lowest part of the stretch minus `fallDepth` 12) he is put back at the last checkpoint: no damage, fall distance cleared, a short voice line, `falls` counted. A checkpoint heals him and fills his hunger (`healAtCheckpoint`).
* **Damage.** All damage is cancelled inside a trial; void damage, `/kill` and anything lethal become a reset to the last checkpoint (`LivingIncomingDamageEvent` first, `LivingDeathEvent` as the net: cancelled, health restored). He never dies on the Road and never respawns in the world;
  `PlayerRespawnEvent` is the last net (progress says inside → brought back). Hunger is full, air full, fire out.
* **Refused way out.** `RoadTrials.leave` returns a boolean: the whole way out runs as the Road's own move, and only a successful move retires the trial, clears `inside` and the entry; if another mod refuses the move, the message `cannot_leave` is shown and the trial is kept (the ending tries again every tick). Every pending or active player is ticked in its own try/catch (`gaveUp`: log, `leave`, else forget in memory and `inside` false), and a trial that cannot open gives up after 60 s with a message.
* **Abandon.** The exit door (a `WitchesRoadDoorBlock` set in the start platform: right-click twice within `leaveConfirmSeconds` 5), `/road leave` (any player) or an operator's `/enhanced road leave <players>`: back to the entry, game mode restored, Gene kept, progress kept.
* **Logout inside.** The instance is removed (tickets released), `inside` stays true. At login `RoadTrials.reconcile`: in the Road dimension with `inside` → `resumeOnLogin` (true) rebuilds the trial and puts him at his checkpoint (a trial with `resumable()` false, the structure copy,
  starts again from its beginning); false → returned to the entry. In the Road dimension without `inside` (an operator's /tp, lost data) → returned to the entry if known, else the world spawn. Not in the Road but `inside` → `inside` false.
* **Server stop or crash.** Instances vanish; ledger cells are cleared at the next start (in slices, tickets held), and "Clearing N cell(s) of the Witches' Road that the last run left behind" is logged.

### 19.5 The rules inside a trial

Only players with no power get in, so the rules are few and live in one subscriber (`RoadRules`):

1. **Powers.** Entry is refused with a power; a power gained inside takes him out (Gene kept); `AbilityHandler.onKey` ignores keys inside a trial (no ability, build mode, flight toggle, telekinesis or Hex). The Book of the Vishanti and the Darkhold are shut (section 20).
2. **Teleports.** Ender Pearls, Chorus Fruit, portals (`EntityTravelToDimensionEvent`) and every mod teleport (`HexManager.onTeleport` asks `RoadTrials.allowsTeleport`) are refused, except the Road's own moves (`RoadTrials.moving(player, action)` marks one). **Nobody outside a trial may teleport or travel INTO the Road** either (a Bifrost, a Tesseract, a portal: refused; operators are free; the Road is shut to the Vishanti book in both directions). Operators' `/tp` is free.
3. **Flying.** Forced to Survival (restored on exit), gliding stopped every tick, Levitation, Slow Falling and Jump Boost removed, and Ender Pearls, Chorus Fruit, Wind Charges and firework rockets cannot be used (a Java set in `RoadRules`).
4. **Blocks.** Nothing is placed or broken in the Road dimension except where the active trial says (`RoadChallenge.mayPlace/mayBreak`: the plot of trial 2). Explosions and fire cannot happen.
5. **The Gene** cannot be thrown away inside: `RoadRules.onToss` gives the stack back (cursor first, then `placeItemBackInInventory`, the menu resynced), so it can always be spent at the end. Other things can still be thrown. **Items that would break the trials cannot be used**: armor stands, boats, minecarts, paintings, item frames and spawn eggs (a boat is the race's own). Things crafted in the Road are marked as the trial's (`ItemCraftedEvent`) and the 2×2 crafting square is emptied when the trial's things are taken back.
6. **Other players' powers** cannot reach him: another dimension, 1,400+ blocks from any other cell.
7. **No mob spawns** (the biome has none); anything a trial spawns is tracked and removed with the cell.
8. **Operators** not in a trial are free of the block and item rules; an operator's /tp out of a trial ends it.

### 19.6 The framework

```
RoadChallenge (interface)   id(), title(), build(RoadBuild) -> RoadLayout; defaults: onStart, tick, mayPlace, mayBreak, onReset, onEnd, resumable
RoadBuild                   level, seed, canvas, origin
RoadCanvas                  set / fill / reserve / live: all tracked; refuses outside the cell; places itself in slices
RoadLayout                  start + yaw, checkpoints (zone AABB, respawn, failY), finish AABB, gate (centre, facing), timeLimitTicks, exit door
RoadTrial                   the live instance: phase PLACING/READY/ACTIVE/WON/CLEARING, checkpoint, succeed(), putBack(player, fell), track(entity), data()
RoadChallenges              the ordered registry; register(challenge) and register(challenge, order): parkour 10, structure copy 20, ice-boat race 30
RoadTrials                  the facade: enter, leave, advance, won, reconcile, onLogout, serverTick, useExitDoor, allowsTeleport, op tools
RoadPalette, RoadFx, RoadVoice   the look (dark oak, amethyst, soul lanterns, roots, glow lichen), effects (reused sounds and particles), the Voice of the Road
RoadSave / RoadProgress / RoadSweeper / RoadRules / RoadEvents / RoadCommands / RoadEnding
```

A new trial = implement `RoadChallenge` (stateless; one object serves every player), register it (the order of the registry is the order of the trials; the screen says "Trial n of N" by itself), add config keys, lang keys
`road.enhanced.challenge.<id>` (and `.goal`, a subtitle of at most 32 characters in both languages: a test checks it) and tests with `RoadTestSupport`. Randomness comes only from `new SplittableRandom(build.seed())`: the course is drawn again, identically, when a player logs back in.
The title of a trial is "<n>. <name>" (a title is drawn four times the size of text; `RoadChallenge.titleShort()` gives a shorter name when the full one does not fit the window: trial 2 is "2. The Copy" / "2. ההעתקה"). `RoadTrials.plan` tries up to 8 seeds (`seed(owner, id, salt + attempt × 7919)`, the same ones every time) before giving up, and `advance` remembers a failure for 3 s so a gate does not try every tick. The GameTest server has no Road level of its own, so `RoadTestSupport.ensureRoad` creates it (the JSON itself is asserted by `theRoadIsAVoidDimension`).

### 19.7 Trial 1: the Parkour

A square spiral winding outward from the start platform (legs of 12, 12, 36, 36, 60, 60 blocks...) so that it can never run into itself, with random jumps along the legs (`ParkourGenerator`, `JumpModel`). Dark oak stones; a lit amethyst **checkpoint** pad every `parkourCheckpointEvery` (6) jumps;
a finish platform with a sealed amethyst gate (`RoadPalette.gate`; the framework opens it when the trial is won). `parkourPads` 40 jumps (8 to 150), `parkourDifficulty` 2 (1 easy: gaps of 1 or 2 blocks and wide stones; 2 normal: gaps up to 3 late, steps up and down; 3 hard: smaller stones, 3-block gaps sooner),
`parkourTimeLimitSeconds` 0 (none). **No jump is ever beyond a running jump**: gaps never exceed 3 blocks of air, a step up comes only with a gap of 2 or less, stones shrink and gaps grow as it goes. The 40 default jumps stay within about 50 blocks of the start. The goal line is "Cross the void to the gate".

### 19.8 Trial 2: Copy the Structure

*The room.* A big dark oak platform (soul lanterns, roots under it) with the start platform and the exit door at the south edge; looking north, on the left (west) the **model** on a floor with an amethyst outline and on the right (east) the empty **plot**, the same size and the same way
round, three blocks away, on the same chequered floor with a sea-lantern outline; an aisle between them runs north to the sealed gate. Floating text ("The Model", "Your Plot / Build here, then press Check", which becomes "Correct 41/48" after a check and "Perfect!" at the end).
In front of the aisle the **console**: two amethyst pillars with a warped button (**Check**) and a cherry button (**Hint**), and the **supply chest** between them. The labels are display entities tracked by the trial.

*The library* (`StructureLibrary`, pure code, `generate(seed, tier[, theme])`): 5 themes and 3 sizes (tier 1 fits 5×5×5, tier 2 9×9×7, tier 3 11×11×9; `structureCopyTier` 2; 0 = a size per player from his seed). **Hut** (a gabled cottage, log corners, stair roof, glass windows, a doorway; from
tier 2 a plinth and a chimney), **tower** (stone shell with mossy patches, parapet, glass slits; tier 2/3 a plank roof walk, an upside-down slab cornice, a flagpole with a wool flag), **bridge** (a humpback arch of stairs rising 2/3/5 blocks over a tunnel with wall rails and, from tier 2, lanterns),
**temple** (rings of two wool colours, a pillar colonnade, a two-colour frieze, a slab roof with an open eye, an altar with a lantern; quartz, deepslate/basalt or purpur), **witch's cauldron altar** (magma under a cauldron, a mossy/stone-brick circle, steps, two carved pumpkins, dark-oak posts with log beams and hanging
soul lanterns). The seed picks the theme and every variable. Every model has 3 to 9 kinds of block (they fit the hotbar and one chest), every block has an item, every block touches another on the way down to the floor, lanterns stand on or hang from something, and no two stairs meet at a corner (the game would round them,
so two layings would look alike). Tested over 5 themes × 3 tiers × 150 seeds. Average size 60, 159 and 286 blocks (tiers 1 to 3), at most 80, 188 and 324.

*The comparison* (`StructureCompare`, pure): block by block over the footprint and the model's height + 2 layers (scaffolding room; an extra there still counts). A block is right when it is the same block and its **facing, half (stairs), slab type, axis (logs, pillars) and `hanging` (lanterns)** are the same.
Ignored: stair shape, fence/wall/pane joints, waterlogged, powered, anything else. The result is correct/total and lists of missing, wrong and extra cells; **perfect** = none of them. 47/48 is `percent 97`, never 100.

*Supply* (`StructureSupply`). On entry the player's own backpack (slots 0 to 35 except Scarlet Witch Genes) is put aside, saved as a player attachment (`enhanced:road_stash`, so it is in his player file even after a hard kill; `StructureCopyEvents.onLogin` always restores it), and he is given exactly the kinds of block of the model, `max(16, need × structureCopySupplyPercent ÷ 100)` (150) of each, the first stack of each kind on the hotbar
(most needed first), the rest in the backpack. Every block carries `custom_data {enhanced_road_supply:1b}` and cannot leave: the stash is returned and every marked stack removed when the trial ends in ANY way (leave, win and step through, time limit, logout (`onLogout` runs before the save),
operator, server stop), and at login if a crash left them. The supply chest holds 3 full marked stacks of each kind and refills every second; it is emptied before the room is taken down. Breaking a block on the plot is instant (a `BreakSpeed` rule on both sides) and gives the block's item back as marked supply
(2 for a double slab; glass too); nothing drops on the floor.

*Check and Hint* (`StructureCopyChallenge`, the right clicks in `StructureCopyEvents`). **Check** (cooldown 10 ticks): action bar `Correct n/total`, a chat line `N missing, N wrong, N extra. Look for the glowing marks.` and one of the Road's words (3 variants each for "try" and "close" = 85% or more), and outlines (the 8 corners
and 12 edge middles of the cell's cube) drawn for 8 s with particles for the player only: blue-green = missing, red dust = wrong, orange dust = extra; at most `structureCopyMarks` (40), the lowest first. A perfect plot wins. **Hint** (cooldown 20 ticks, free): glowing block displays (the Road's teal) of the real blocks that belong in the
missing (and wrong) cells, at most `structureCopyHintBlocks` (40), lowest first then nearest, for `structureCopyHintSeconds` (8), a wrong block with a slightly bigger ghost around it; off with `structureCopyHints` false. `structureCopyAutoCheck` (true): once a second the Road compares the plot itself and a perfect copy wins without Check.

*Rules in the room.* Place and break only inside the plot volume (its footprint, from the first layer to model height + 2) and only while the trial is active; the model, the floor under the plot, the console, the chest and the platform cannot be built on or broken (a short action-bar reminder only for a block that is refused).
*No failing:* no health, no timer except the soft `structureCopyTimeLimitSeconds` (5400 = 90 min, 0 none; at the end he is sent home as if he had left and the room is taken down; the framework's `timeLimitTicks` is not used, it would put him back at the start); a fall puts him on the start platform, the build stays. `resumable()` is false: after a logout he
starts a NEW room (his build is not kept; his things are given back at logout and put aside again at login). Goal line: "Build a copy of the model".

### 19.9 Trial 3: the Ice-Boat Race

*The physics of a boat on ice in 1.21.1* (read from `Boat`, measured on the real entity in GameTests). The rider's **client** moves the boat; the server validates ("moved wrongly" past 0.25 off the server's own collision move) and follows. On land each tick speed is multiplied by the plain friction of the block
under it (packed ice and ice 0.98, blue ice 0.989, snow block, planks, amethyst 0.6) and W adds 0.04 along the heading (A or D alone 0.005, S -0.005): the top speed is 0.04 ÷ (1 − f): **2.0 blocks a tick (40 blocks a second) on packed ice**, 3.6 (72) on blue ice, 0.1 on snow. A and D add ∓1 degree to a turning speed
multiplied by the same friction (up to 50 degrees a tick, so the boat spins on if a key is held): the boat turns as fast as anyone needs but cannot change the direction it travels (only the 0.04 push along the heading bends the path): the tightest curve at speed *v* has radius *v*² ÷ 0.04. **A boat climbs nothing** (step height 0: a slab stops it dead), so the course has no ramps up;
edges down are fine. In the air gravity is 0.04 and speed × 0.9 a tick; it hangs on an edge until its whole box (1.375 wide) is off; a gap is crossed by speed alone (5 blocks of air with the far side 1 lower needs about 0.85 blocks a tick, 17 blocks a second). A boat that lands after a fall of more than 3 blocks breaks, so the trial keeps its fall distance at 0 and makes it invulnerable, and the course has
no drop above one block except the void. Sneak (Shift) in a boat dismounts the rider (`Player.rideTick`): that is the "stuck? sneak" mechanic.

*The model* (`road/boat/`): `IceBoatModel` (the numbers above as pure functions: terminal speed, turn rate, coast distance, corner speed √(0.5 × 0.04 × R), `airReach`, `minJumpSpeed`, `speedProfile`), `IceTrack` (a course as plain data: lines, pieces, features, checkpoints, loops, and a raster of columns), `IceTrackGenerator` (seeded: straights, quarter turns, S-bends,
hairpins with a snow braking strip before, narrow stretches (5 wide), snow drifts, snow-wall chicanes, blue-ice stripes (3 wide, 28 long: blue ice's 72 blocks a second is beyond the server's "moved too quickly" check and a child), and loops with a shortcut: a straight across the loop with a 5-block gap, the far side one block lower) and `IceBoatChallenge` (the trial).
**Generator guarantees** (tested over many seeds): determinism; length within [L, L + 260]; every floor at least 3 wide (normal `boatRaceTrackWidth` 9, narrow 5); every curve a quarter circle with R ≥ 13 (hairpins) or 20 to 50 that a boat can follow at ≥ 10 blocks a second; a straight of ≥ 24 blocks before every curve (a hairpin is preceded by a 44-block straight with a full-width snow strip); every floor cell reachable from the start; inside 436 blocks
of the cell's middle (the cell allows 450); checkpoints on plain straights; a shortcut = 5-block gap, drop 1, run-up ≥ 70 blocks which gives ≥ 1.5 times the speed the jump needs, saving ≥ 60 blocks. **Winnability proof:** a driver using only W/A/D (`IceBot`: pure pursuit and the physics speed profile) finishes the long way of EVERY tested course (hundreds of seeds, three difficulties, widths 7 to 15, lengths 200 to 1,400) on a
second simulation of the vanilla boat (`IceBoatSim`, written from the game's code), also a "careless" driver (3-tick delay, 4% random keys), and takes the shortcut in about 99% of the courses where it tries. The simulation is checked against the real `Boat` entity tick by tick (agreement to 1e-5 blocks over 1,500 ticks of a whole course; 1e-3 when walls are rubbed).

*The trial.* The course is drawn on the canvas (packed ice on 2 layers, walls of packed ice 2 high (4 on the outside of curves), a stripped dark oak rail, sea lanterns in the walls, soul lanterns on posts, hanging soul lanterns under the floor, amethyst arches over the start line, the checkpoints and the finish, a purple glass barrier at the start). Start on the Road's usual platform; the boat waits 6 blocks ahead at the start
line (a checkered stripe), the barrier 5 blocks in front of it; he walks up to the boat (it takes him within 2.4 blocks when he is not sneaking; after he climbs out during the countdown it does not take him again until he has walked 3.5 blocks away). **Countdown** `boatRaceCountdownSeconds` 3 (title 3, 2, 1, "GO!", subtitle "Par time"), the barrier opens, the framework's clock (from the trial's start, so the countdown is part of the time) runs: `boatRaceTimeLimitSeconds` 240, **never shorter than par × `boatRaceFairFactor`** (3.0, 0 to 20, 0 = off; the limit is `max(boatRaceTimeLimitSeconds, par × factor)`, so a long, hard course is never an unfair race against the clock).
**Checkpoint 1 is the start line**; then every ~`boatRaceCheckpointEvery` (100) blocks on a plain straight (amethyst arch, zone 6 deep × the track wide). **Falling** (below the lowest floor ahead − `fallDepth`) or leaving the boat: the boat is REPLACED (a new entity: the only sure way to put the client's idea of the boat right) on the last checkpoint with the player in it. **Sneak out:** back to the checkpoint at most once per `boatRaceRespawnCooldownSeconds` (5);
an earlier sneak puts him back in the same boat; 4 s without moving: the action bar says how. **Time out** (framework): back to the start platform, boat parked, barrier shut, countdown again. **The finish line** (checkered, arch): the race decides itself (`finish` null): time, par and best; `trial.succeed()`; the boat is removed and he stands on the finish platform in front of the gate.
HUD: a boss bar (time left, notched, blue → yellow → red; "checkpoint a of b"), the speed in blocks a second in the action bar, snow motes round the boat and aurora motes (`EnergyParticle`, green/blue/violet) high above. **Par time** = `track.parTicks()` (the fastest way through every curve by the model plus a little for snow, walls and drops) × `boatRaceParFactor` (1.4). The best time (ticks since "go") is kept in his saved data (`ice_boat.best`).
`boatRaceLength` 800 (200 to 1,400), `boatRaceTrackWidth` 9 (7 to 15, odd), `boatRaceDifficulty` 2 (1 easy: wide curves, one hairpin, no snow walls; 3: tighter curves, three hairpins, more snow), `boatRaceShortcuts` 1 (0 to 3), `boatRaceObstacles`, `boatRaceBlueIce`. Goal line: "Sail the ice to the finish". A scripted drive on the real boat tests the whole race (`RoadBoatRunTests`).

### 19.10 The end of the Road

The stub `RoadEnding.finish` is gone. After the last trial's gate the Road builds the **hall** (`EndingHall`, a `RoadChallenge` with `isEnding()` true, id `road_end`, not in `RoadChallenges.all()`; `RoadChallenges.ending()` and `RoadChallenges.stage(n)` = trial *n* or, after the last trial, the hall): a round hall of radius 9 in dark oak and amethyst, a dais of radius 2.6 with a sea lantern, six amethyst pillars with soul lanterns, roots and lichen hanging under it, and the Road's door lying in the floor behind the player
(the exit door), in a cell of its own. It is held (tickets), saved in the cell ledger, resumed after a login and taken down exactly like a trial (`RoadTrials.advance` plans it when `RoadChallenges.stage(completed)` is the hall); it never counts as won (`RoadTrial.succeed()` ignores it, `skip` does nothing there), and the framework's title and voice lines are skipped for it.

* **`RoadEnding`** (state per player in memory: stage ARRIVING → ASKING → FAREWELL). On arrival: the title "The Road's End", the subtitle "Seekest thou the Road?", a voice line (`ending.1..3`) and the sound `witches_road`; the light gathers over the dais for `ASK_DELAY_TICKS` (60), then `OpenRoadPayload(door, trials=false, done, total, ending=true)` opens `WitchesRoadScreen`: Billy, Tommy, the note "One Gene will be used up.", and the buttons "Not yet" and "Leave the Road". A Gene anywhere in the inventory is needed; without one the Road says so and does not ask.
* **The choice** (`RoadEnding.choose`) is the only place the Gene is used up in the Road. `RoadChoicePayload` is handled by where the player IS, never by what the message says (in the hall → `RoadEnding`; elsewhere in the Road → ignored; in the world → the 3.1 door rules), and the answer is accepted only if he is in the hall (a live trial with `isEnding()`, phase ACTIVE, stage ASKING), in the Road dimension, alive, not a Spectator, with no power, his **saved** progress says `inside` and `completed == RoadChallenges.count()`,
  he carries a Gene and the answer is Billy or Tommy. Then one Gene is consumed and `Powers.awaken` runs exactly as at the door (the title with the awakening line, a burst, the sound `awaken`, the server broadcast; Billy: Build Magic rank 1, meter full, the `BuildData` default). A second answer (a double click, a crafted packet) finds stage FAREWELL and does nothing. `WitchesRoad.choose` (the door) is unchanged except that it also closes the Road's progress.
* **The farewell** (`endingFarewellSeconds`, 5, 0 to 30; 0 = at once): he is held where he stands in a rising helix of the Road's light mixed with his hero's colour, amethyst chimes rising in pitch, the Road's line (`farewell.1..3`) after 2 s. The Road's rules still apply (no ability key, teleports refused for the Bifrost, the Tesseract and heroes' leaps; the Vishanti and the Darkhold are shut; pearls, chorus and portals refused; damage and death cancelled); a power in the Road is no reason to throw him out only in this stage (`RoadEnding.isLeaving`).
* **The way home** (`RoadEnding.depart` → `RoadTrials.leave(End.WON)`): effects cleared; he goes to the saved entry (dimension, position, rotation, game mode; the door he opened is saved too, `RoadProgress.Entry.door`, optional in the codec). If that place is gone or unsafe (a block there now, no floor within 24 blocks below, lava or fire at the feet or as the floor, cactus, magma or campfire, outside the world border, or a dimension that does not exist or refuses) the ladder is: **the entry spot (and up to 6 blocks above it) → the door he opened, if it is still a Road door → the overworld's spawn on the surface** (`worldSpawn`: the heightmap).
  His things come back (the structure trial's backpack), the hall is taken down, he comes out healed and fed (`healAtCheckpoint`), XP and inventory untouched, with a voice line (`home.1..3`) and the chat line "You walked the whole Witches' Road, and you are now Billy (Wiccan)!". Because he has a power, `leave` calls `RoadTrials.closeRoad`: **progress forgotten and `salt` + 1** (the next Road he could ever walk, with another Gene and no power, has other courses); a player with a power never keeps trial state, so any `leave` of a powered player closes the Road.
* **Closing and leaving.** "Not yet" only closes the screen (nothing changes on the server); the door in the hall (`RoadTrials.useExitDoor` → `RoadEnding.reopen`) and the command `/road choose` open it again. "Leave the Road" (the answer none at the end) is `RoadEnding.leaveUnchosen`: back to the entry with the Gene, progress kept, and the door in the world then offers Billy and Tommy as before (`DoorMode.CHOOSE`; choosing there also calls `closeRoad`). `/road leave` works at any time.
* **Logout and crash.** In the hall the hall is taken down with the cell (`onLogout`); at login `reconcile` builds it again and the title and the question come back. In the farewell the power is already his (saved with the player): at login, in the Road dimension with a power, `reconcile` sends him home (`leave`, End.WON if he had walked every trial) and the Road closes. A refused way out (another mod cancels the change of dimension) keeps him in the hall and tries again every tick (`RoadTrials.leave` returns a boolean; only a successful move retires the trial; the message `cannot_leave`).
* **Death.** Nothing kills him in the Road; at the finish he is put back at the hall's start and the question stands; in the farewell the death is turned away and he still leaves.
* **Operators.** `/enhanced road enter <p> <number of trials + 1>` sends a player to the end (progress must say every trial is won, or the choice is refused: `set <p> 99` first); `skip` has nothing to win there; `set <p> n` with *n* below the number of trials sends him home from the hall within a tick ("The Road has taken your trials back"); `reset` takes him out and forgets. `/tp` uses the command source's dimension: from the console use `execute in enhanced:witches_road run tp ...`, because a plain `/tp` asks for the overworld and the Road refuses it.
* **Tests** `RoadEndingTests` (21: Billy and Tommy awakened and sent home with everything checked, no farewell, closing the screen, leaving without choosing, logout and death in the hall and the farewell, the ladder of safe places, a missing dimension, a crafted answer through the real payload handler, the operator commands, the whole Road of two small trials, the structure trial's backpack through to the end).

### 19.11 Commands, configuration, voice, tests

* **Commands.** `/enhanced road status [player]`, `enter <players> [trial]` (numbered from 1, no Gene needed), `leave <players>`, `reset <players> [newcourse]`, `skip <players>`, `set <players> <won>`, `layout [player]`, `cells`; and for everyone `/road leave`, `/road status` and, at the end, `/road choose` (a second root command, because `/enhanced` needs permission level 2 and the way out must be open to all). `/enhanced road enter <players> <trials + 1>` is the hall.
* **Configuration** `[witches_road]`, 37 options (the wiki's Server Configuration page): the Road (`enabled`, `maxPlayers` 32, `courseSeed` 0, `resumeOnLogin`, `resumeAtCheckpoint`, `announce`, `voice`, `healAtCheckpoint`, `fallDepth` 12, `leaveConfirmSeconds` 5, `buildBlocksPerTick` 20,000, `removeBlocksPerTick` 20,000, `endingFarewellSeconds` 5), the Parkour (4), the boat race (12, with `boatRaceFairFactor`) and the structure copy (8). Constants, not options: the cell spacing 2,048, the course radius 450, 64 cells a row.
* **The Voice of the Road** speaks in the action bar and the title (`message.enhanced.road.voice.<event>.<n>`, three variants for enter, start, checkpoint, fall, won, leave, back and the structure trial's start, try, close and hint; English and Hebrew). **Sounds** are reused: `witches_road` (a trial opens, go, finish), `road_travel` (in and out), amethyst chimes, note-block hat and pling, boat paddles.
* **Tests:** `RoadFrameworkTests`, `RoadParkourTests`, `RoadRulesTests`, `RoadLoginTests`, `RoadStopTests`, `RoadDoorTests`, `RoadConfigTests`, `RoadDataTests` (the framework, in batches `road_framework`, `road_parkour`, `road_rules`...), `RoadStructureModelTests`, `RoadStructureTests`, `RoadStructureOptionTests` (the library over 5 × 3 × 150 seeds, the comparison, supply and stash, Check and Hint, a crash),
  `RoadBoatTrackTests`, `RoadBoatPhysicsTests`, `RoadBoatTests`, `RoadBoatRunTests`, `RoadEndingTests` (21, 19.10) and `RoadReviewTests` (15: the Gene thrown, travel into the Road, a refused way out, a failing trial tick, the sweeper's retries, the stash, 8 seeds, the border, unusable items, the fair factor). A test that swaps the registry (`RoadChallenges.swap`) has a batch of its own, and a full run needs `run/world` and `run/config/enhanced-server.toml` deleted first. Findings that shaped it: the 3.1 door screen sometimes never showed (the server sent it in the tick the door opened;
  `OPEN_GRACE_TICKS` 20), a cleared cell with a filled chest spilled its items (entities are now discarded after blocks), and a boat cannot climb a slab (so no ramps).

---

## 20. The sorcerers' books (added in 3.2)

Eilon asked for two items: the **Book of the Vishanti** (a sorcerer can save a place and teleport to it, and raise a protective shield round the base that no monster can enter, larger the stronger the sorcerer) and the **Darkhold** (Agatha's abilities: her purple laser beam, and a retaliation: magic used on the holder poisons for 10 seconds). Both are craftable in Survival and in
the **Enhanced** creative tab. The player's guide is the wiki page *The Sorcerers' Books*.

### 20.1 Who is a sorcerer

`WardStrength.isSorcerer`: the power is **Loki, the Scarlet Witch or Billy** (**and Agatha since 3.3**, 24). Anyone else may hold the books but they only whisper ("The runes swim before your eyes. Only a sorcerer can read this book", "The Darkhold whispers, but only a sorcerer can read it"); `darkhold.anyoneCanUse` (false) lets anyone use the Darkhold.

### 20.2 The Book of the Vishanti

* **Item** `enhanced:book_of_the_vishanti` (`VishantiBookItem`): stack 1, epic, fireproof; an animated 16 × 16 texture (a leather cover, gold corners and clasp, a glowing three-rayed emblem in a ring that pulses; `gen_vishanti.py`, three render-and-inspect rounds). **Recipe** (shaped): `A E A / G B G / A L A` = 4 amethyst shards, an Eye of Ender, 2 gold ingots, a book, a lapis lazuli;
  the advancement `recipes/equipment/book_of_the_vishanti` unlocks it on getting an Eye of Ender.
* **Controls.** Use opens the book (`VishantiScreen`: name field, *Save here*, *Save as base*, *Go to my base*, a row per place with *go* and *x*, bases marked, the reader's ward radius; three places a page, up to 24 places); **Sneak + use** goes to the first base. Client → server `VishantiActionPayload`.
* **Places and the teleport** are the Bifrost's service generalised: `ability/PlaceBook` (`Kind.BIFROST | VISHANTI` = which attachment holds the list and which lang prefix: save, forget, find, safe arrival, cooldown, destination rules, journey: charge, then take); `ThorAbilities` delegates to it with its behaviour unchanged. The record `BifrostData.Place` gained an optional `base` boolean (codec optional, default false;
  one more BOOL in the stream codec). The Vishanti has its own attachment `vishanti_book` (own list and cooldown). Rules: any dimension; refused by boss arenas (`BossFights.allowsTeleport` asked when the way opens and again at arrival), the Hex wall judge, Mjolnir's pin (all via `HexManager.onTeleport`), `vishanti.deniedDimensions` (default `["enhanced:witches_road"]`; the Road is **always** shut, even if the list is emptied: no saving, no leaving, no entering).
  `teleportChargeTicks` 20, `teleportCooldown` 600 (refunded if the arrival is refused), `teleportExhaustion` 6.0 (Creative free). Vanilla sounds and particles only (a sorcerer-coloured spark ring).
* **The ward** is a sphere round a **base** place. `WardStore` (a `SavedData` per dimension, `enhanced_wards`) keeps owner, name, centre and radius; `WardManager` does the look and push, the spawn gate, the refresh and the sync; `WardEvents` the game-bus events; `WardCommands` `/enhanced ward list|remove <player>`. At most `maxWardsPerPlayer` (2) bases; raising or moving one costs `wardCooldown` 200 and `wardExhaustion` 8.0.
  **Radius** (`WardStrength.radius`, hard cap 128): Wanda `wardRadiusWanda` **48**; Billy lerp(`wardRadiusBillyMin` **12** → `wardRadiusBillyMax` **40**, (rank − 1) ÷ (`maxRank` − 1)) read only through `WardStrength.billyFraction` (`BillyRanks.rank` ÷ `maxRank`): 12, 15, 18, 21, 24, 28, 31, 34, 37, 40 for ranks 1 to 10; Loki `wardRadiusLoki` **24**. It is refreshed at login, every 5 s while the owner is online and on any save or forget.
* **Effect.** Monsters (`MobCategory.MONSTER`) are refused where a ward is: `MobSpawnEvent.SpawnPlacementCheck` FAIL and `FinalizeSpawnEvent` cancelled (`wardBlocksSpawns`; a spawn egg, command or dispenser spawn is not refused by the event but the look then pushes it out); every `wardCheckTicks` (5) each ward does **one** spatial lookup (`getEntitiesOfClass(Mob, box)`, staggered per ward, only when the ward's centre chunk is
  entity-ticking, at most 48 monsters a look) and a monster inside is thrown back (`wardPush` 0.8 blocks a tick) when it is within 3 blocks of the edge, else set down just outside on free ground (up to 6 blocks up or down; the throw is the fallback). **Not affected:** the tag `enhanced:ward_immune` (= `#enhanced:bosses` and `#enhanced:boss_minions`, so the Wither, the Ender Dragon, the Warden, the Elder Guardian, Thanos, Ultron, Apocalypse and their helpers),
  anything in a boss fight or arena, pets with an owner, riders and mounts, and every non-monster. A ward changes **no block** (claim-safe). **(Review fixes)** Also let through, in code: a monster with a custom name (`hasCustomName()`), a leashed one (`isLeashed()`), and a Zombie Villager being cured (`isConverting()`); raiders, piglins, hoglins and farm monsters are NOT let through (name the mob to keep it). `wardsOn()` (`vishanti.enabled` and `wardsEnabled`) guards the look, the protection test and the spawn gate, so turning wards off silences every ward, an offline owner's too; a changed radius or a new Billy rank reaches an offline owner's ward at their next login. Set-down candidates are rejected when the body or the block under it has lava, fire, magma, cactus, a sweet berry bush, a wither rose or a campfire; a walker needs a sturdy top face under its feet, a Guardian needs water. The look is gated by "a player within radius + 96" and each monster by its own `isPositionEntityTicking`. `WardManager.payload(level, viewer)` tells a client of the wards whose edge is within 256 blocks of it and of its own wherever they are, resent every 10 s per player. Saving a base's name again with plain "Save here" moves the base and its ward (with the ward's cost, cooldown and rules). No claim awareness and no minimum distance between wards (a ward can be raised inside another player's base, farm or the spawn: a "clear the monsters round me" button; a ward over a raided village stops the raid). `/enhanced ward remove` takes a game profile (selector, name or id, online or not; wards go by id alone) and login reconciles whenever the player has any ward, so a wiped book loses its wards. It cannot be raised in a boss arena, in the Road, or where it would reach into an arena.
* **3.3:** what a ward keeps out changed: every monster, boss and boss minion, what they launch, and dark magic; `enhanced:ward_immune` is empty (23.3).
* **Persistence (a decision).** A ward works whenever its chunks are loaded, **whoever is online and whether or not the book is held**; only raising or forgetting needs the book. The ward of an online owner who is no longer a sorcerer sleeps (radius 0, kept, not drawn) and wakes when they are one again. Forgetting a base removes its ward; `/enhanced ward remove` turns all of a player's bases back into plain places.
* **Look.** `WardSyncPayload` (the awake wards of the dimension; on change, login, dimension change, respawn) and `client/WardFx`: a faint veil and hexagon lines on a sphere in the sorcerer's colour (Wanda crimson, Billy blue, Loki green) and a ground ring, only when the edge is within 64 blocks of the camera; *Reduced motion* stops the drift and the breathing.
* **Config** `[vishanti]` (16 options) and the tag `enhanced:ward_immune`. **Tests** `ItemsTests` (28, batches `items_vishanti`, `items_ward`, `items_vishanti_shut`, `items_vishanti_cost`, `items_vishanti_boss`, `items_ward_spawns`). Found and fixed in the game: the screen showed one place a page at GUI scale 2 (now three); the arena's ceiling lets the sun in, so the zombies of the push test have Fire Resistance.

### 20.3 The Darkhold

* **Item** `enhanced:darkhold` (`DarkholdItem`; stack 1, epic, fireproof; an animated purple-and-black texture, `gen_darkhold.py`). **Recipe** (shaped, rare on purpose): `N R N / E B E / N S N` = 4 netherite scrap, a wither rose, 2 echo shards, a book, a wither skeleton skull (the rose needs a Wither's kill, the skull a fortress, the shards an ancient city); the advancement `recipes/equipment/darkhold` unlocks it on getting a wither rose.
* **Who and where.** A sorcerer holds it in either hand (`darkhold.enabled`; anyone with `anyoneCanUse`). None of its powers works in the dimension `enhanced:witches_road`. A small HUD panel (`client/DarkholdHud`) lists the powers while it is held.
* **The Purple Beam** (**hold use**; the use is held like a bow's, so the holder walks slowly: a vanilla rule of held uses; not R, so it never clashes with a hero's own R). The generic `ContinuousBeam` (21) with the style `DarkholdItem.STYLE` (`darkhold`: core `0xFFFFFF`, inner `0xF0D8FF`, main `0xB45CFF`, body `0x7A2FD6`, fringe `0x3A127A`; the lang name "Purple Beam"). Aimed along the look at the first creature
  the crosshair meets (blocks stop it); a complete Chaos Shield stops it at its surface; the game's PvP, Creative and boss rules as for Billy's beam. `beamDamagePerSecond` **8** (hits of 2.0 every `beamInterval` 5 ticks, no damage cooldown), `beamRange` 24, `beamExhaustionPerSecond` 1.0 (Creative free). Damage type `enhanced:darkhold_beam` (in `bypasses_cooldown`, `no_knockback`, `panic_causes`; death messages in both languages).
  It needs food ("Too hungry to fire the Purple Beam"); it ends on release, when the book is put away, on hunger, Spectator and logout. **Billy** holding it fires this beam, not his own (his Hex Beam tick does not run while the Darkhold's beam is the one burning). The Chaos Beam loop at pitch 0.75 is its sound.
* **Retaliation** (passive, held). When a hit of the damage tag `enhanced:magic_damage` (the mod's Hex Bolt and Beam, Chaos Beam, telekinetic slam, Mind Stone and its beam, the stones' powers, the Phoenix Burst, the purple beam, `enhanced:hex_wall`, `enhanced:entomb`, and the game's `magic` and `indirect_magic`: witch potions; **`enhanced:lightning_beam` left the tag in the review**: Thor's lightning is his power, not magic) lands on the holder, the **attacker** (`poisonTarget` `ATTACKER`, or `HOLDER`)
  gets Poison for `poisonTicks` **200** (10 s) at level `poisonAmplifier` 0; the same attacker is not poisoned again for `poisonCooldownTicks` 40. It answers the *use* of magic even if a Chaos Shield stops the hit; a hit with no living attacker, or the holder's own, answers nothing; undead are immune to poison by the game's own rule. **(Review fixes)** The Confused and Spellbound effects (Loki's Scepter, Billy's Freeze, the Phoenix's mind powers) laid on the holder by a LIVING creature poison that creature too (`MobEffectEvent.Added`, only when the effect's source is a living creature; the retaliation cooldown applies); shapeshifting and telekinetic grabs are not effects and do not answer. The tooltip says "poisoned for a while" (the time is `poisonTicks`). R released while a Darkhold beam burns no longer stops it (`BillyAbilities.hexBeamKey` checks `DarkholdBook.firing`), and the loops are chosen by the style of the beam the client was told (`BeamRenderer.styleOf`), not by the held item.
* **Hex mark** (**Sneak + use** at a creature within `markRange` 24): it glows, gets Slowness (`markSlowAmplifier` 1) and takes `markDamageBonus` 0.5 more (×1.5) damage from the one who marked it, for `markTicks` 200 (a boss, the tags `enhanced:bosses` and `enhanced:hex_resistant`, still glows and takes the ×1.5 but is not slowed); `markCooldown` 100, `markExhaustion` 2.0; another player only where the marker could harm them (PvP).
* **Mind glimpse** (**Sneak + use** at no creature): every living creature within `glimpseRange` 24 (the nearest `glimpseMax` 16) glows for `glimpseTicks` 100, and the action bar says how many and the nearest one's name and health ("The Darkhold shows creatures: 2. Nearest: Zombie, 5.0 of 20 health"; it skips invisible creatures, players in Loki's Vanish (`Live.HIDDEN`) and a phased Vision, for the glow and the "nearest" line; ended marks are pruned once a second); `glimpseCooldown` 200, `glimpseExhaustion` 1.0.
* **Config** `[darkhold]` (24 options), the tag `enhanced:magic_damage`. Server state (marks, cooldowns, who fires) is in static maps cleared on server stop (`DarkholdEvents`). **Tests** `DarkholdTests` (17, batches `items_darkhold`, `_anyone`, `_beam`, `_billy`, `_pvp`, `_holder`, `_marks`, `_glimpse`).

---

## 21. The beam system (added in 3.2)

Billy's Hex Beam and the Darkhold's Purple Beam share one infrastructure; a third beam needs no copy of the renderer, the payload or the server logic.

* `beam/BeamStyle` (record, common): `id` (wire name), `nameKey`, the palette `core`, `inner`, `main`, `body`, `fringe` (white-hot to dark fringe) and `offHand`. `beam/BeamStyles`: the registry (`BILLY` = `billy_hex`, blue `0x5CC8FF` with a white-hot core and a dark blue fringe; `DarkholdItem.STYLE`). A style is registered once in a static initialiser both sides load.
* `beam/BeamSpec` (record): `style, range, damage (per hit), interval (ticks between hits), exhaustion (per tick), damageType, onHit`. The source works it out every tick from its own config. Its damage type is in the tags `bypasses_cooldown`, `no_knockback` and `panic_causes`, so every hit lands.
* `beam/ContinuousBeam` (server; since the review the hit time is kept per shooter across stop and start (`LAST_HIT`, cleared at logout and server stop), so tapping the key cannot hit faster than `interval`): `start(player, style, range)` when the key goes down (sets `Live.BEAMING`, sends `BeamPayload`), `tick(player, spec)` every tick while held (hunger exhaustion, a crosshair raycast that hits creatures, dragon parts and crystals and is stopped by blocks, a complete Chaos Shield stopping it at its surface and paying, damage every `interval` ticks, sparks, a keep-alive payload every 8 ticks), `stop`, `isFiring`, `forget`, `clear`.
  The caller decides when to stop (key up, hunger, Spectator).
* `network/BeamPayload(shooter id, style id, range, on)` (server → clients) and the client `BeamRenderer`, which draws every active beam in its style's palette, with strands, forks and flares at both ends; clients find the end by looking along the shooter's view. Animation: `Live.BEAMING` drives `Stance.BEAM_HAND` (an arm out along the look, the other braced; first person too) for any power; `GestureRoutes.live` gives the start gesture. Sound: `PowerLoopSounds.Kind.HEX_BEAM` (the Chaos Beam sound at pitch 1.3) and the Darkhold's.
* **Billy's Hex Beam** (6.4) and **Hex Barrier** (6.5) are built on it and on the synced Chaos Shield fields of `PowerData`. Tests: `abw_beam_*`, `abw_barrier_*`, `AbwShieldTests`.

---

## 22. What else changed in 3.2, and how it was put together

* **Apocalypse was weakened** (17.3 to 17.6): one master factor `apocalypse_boss.powerFactor` (0.65) and retuned defaults; `ApocalypseBossConfig.power/dealt/count/slower`. Existing mechanics tests set `powerFactor` to 1.0 (`ApocalypseTestSupport.fixture`) so they keep the 3.1 pacing; `AbwApocalypseTests` (5, batches `abw_apocalypse_*`) test the new numbers.
* **Wanda's Chaos Shield is unlimited** (2.3 ability 4): `scarlet_witch.shieldUnlimited` (true); `TestKit.fixture()` switches it off for the 52 old shield tests (`AbwShieldTests`, 5 tests, cover both modes).
* **Build Magic**: Billy's ten ranks (18.6) and Wanda's Instant level (18.14).
* **Network protocol "6"** (a 3.1 client cannot join a 3.2 server): the new payloads `BeamPayload`, `VishantiActionPayload`, `WardSyncPayload`, the changed `OpenRoadPayload(door, trials, done, total)` and `RoadChoicePayload(door, Power.NONE)` ("Walk the Road"), `BuildActionPayload.INSTANT`, `BuildFxPayload.POP` and the extra field of `BuildData` (`instant`, not saved). `SkeletonTests.theProtocolIsSix` pins it.
* **Shared files** were edited additively by the lanes (`EnhancedConfig`, lang files, `ModNetwork`, `ModAttachments`, `ModTags`, `ModItems`, `ClientSetup`, `PowerHud`, `PowerTicker`, `HexManager.onTeleport` and `AbilityHandler.onKey` got one hook each for the Road), then merged in the order ABW → ITEMS → BUILD → ROAD.
* **Tests.** 3.1 shipped 1,315 game tests; the final tree of 3.2 (version 3.2.0, all lanes and their review fixes merged) passes **1,559** (`All 1559 required tests passed`, about 5.2 minutes). The lanes added: the Apocalypse, shield, beam and barrier tests (`Abw*Tests`), 6 for Build Magic (`BuildInstantTests`, and `V31SkeletonTests` now checks all ten ranks), the Book of the Vishanti and the Darkhold (`ItemsTests`, `DarkholdTests` and their review tests), and the Road's (the framework and the three trials, 21 for the ending, 15 for its review). A full run needs `run/world` and `run/config/enhanced-server.toml` deleted first (a generated server file from 3.1 keeps the old defaults).
  `theIslandIsSafeFromEveryFeature` (V31CrossTests) failed in a few early runs under load (the 2 ms preview of the cast timed out); its test now stands the preview's clock still.
* **Review fixes.** Each lane was reviewed once and fixed before the merge into 3.2: the Apocalypse (Death takes the place of the third Horseman, 17.6), Wanda's shield against bosses (`shieldUnlimitedVsBosses`, 2.3) and Instant's shared tick budget (`instantTickMillis`, 18.14), the wards and the Darkhold (20.2, 20.3: who a ward lets through, `ward remove` by profile, Thor's lightning out of the magic tag, no slow on bosses) and the Road (19: the Gene given back if thrown, no travel into the Road from outside, the Road's own world border, the sweeper's retries, the stash saved with the player, the boat race's fair factor, the shorter title of trial 2).
* **Known rough edges.** The javadoc of `ApocalypseShield.strengthFor` still quotes the 3.1 numbers (150, 188, 234 for one fighter; the real ones are scaled by `powerFactor`). A server that keeps its 3.1 `enhanced-server.toml` keeps the 3.1 defaults for every option that file already lists (for example `maxRank` 4 and the old Apocalypse numbers): a fresh file, or editing the values, gives the 3.2 ones.

---

## 23. Walls that keep powers out (added in 3.3)

Eilon: "before, many powers got through". 3.3 makes the three walls of the mod mean what they say: **Billy's Hex Barrier** stops everything from the front, **Wanda's Hex** lets in only those she approves and lets no power through from outside, and **the ward of the Book of the Vishanti** keeps out monsters, bosses, what they launch and dark magic, and nothing else. The player's guide is on the wiki pages *Billy and the Witches' Road*, *Scarlet Witch* and *The Sorcerers' Books*.

### 23.1 One place for every wall

The package `protect/` (common code) is asked by every power:

* `Shelters`: `maker(entity)` (the owner chain: a projectile, a lit TNT, fangs, a cloud, a Vex and a thrown block lead to whoever made them); `hexTrusts(hex, source)` (the maker may cross into the Hex: `HexManager.judge(...) == PASS`: Wanda, Creative, the players she allowed and their pets, creatures if allowed, her Vision; never a boss); `hostile(source)` (the monster category, `#enhanced:bosses`, `#enhanced:boss_minions`, fight members; never a player or a pet): what a ward keeps out; `keepsOut`, `shelters` (Billy's barrier too), `stops` (pays the barrier, sparks at the wall), `across` (the first wall a line of power runs into: a Chaos Shield, a Hex Barrier plate, a Hex wall, a ward), `wallAcross` (for moving things: only a Hex or a ward) and `hexClip` (every player ray of `Raycasts` asks it). **Rule: a wall keeps out; a power that starts inside a sphere is never stopped by it.**
* `ShelterEvents`: the universal guards. Damage (`LivingIncomingDamageEvent`, highest priority: harm whose source, attacker or explosion centre is outside a Hex that does not trust its maker, or hostile or dark harm from outside a ward, is cancelled; harm from nowhere and `BYPASSES_INVULNERABILITY` pass), explosions (`ExplosionEvent.Detonate`: the blocks and entities inside such a Hex or ward are taken out of it; `ExplosionKnockbackEvent`: no push through Billy's barrier from the front), effects (`MobEffectEvent.Applicable`), projectiles and lit TNT whose next move enters a Hex or ward that keeps them out (they stop at the wall in its colour; one-of-a-kind weapons turn home; TNT fizzles), projectiles, TNT, fangs and lightning made just inside by a maker outside (not added), lightning strikes, a creature arriving through a portal and players changing dimension or respawning inside (`HexManager.arrive`), and the Hex Runes tick.
* `Sorcery.isSorcerer(player)`: Loki, Billy, the Scarlet Witch, Agatha, or anyone holding the Darkhold or the Book of the Vishanti in either hand (the Hex Runes' list). `DarkMagic`: the damage type tag `#enhanced:dark_magic` (`darkhold_beam`, `agatha_drain`, `agatha_runes`), the entity type tag `#enhanced:dark_magic` (empty, for projectiles), the Darkhold's beam style, and `casts(entity)` (the Darkhold's beam or Agatha).
* Every power of every hero and boss was audited against the Hex wall, by category: projectiles, beams and lines, player aims, harm, effects, explosions, area attacks (`AreaTargets.mayHurt` asks the walls), grips, pulls and teleports of others (per site), blocks (`BlockRights`), teleports of self (`HexManager.onTeleport`) and perception (The Noise, Mind Sense, Soul Sight, the Darkhold's glimpse; Spider-Sense is not stopped: it warns of danger to himself). Not covered, on purpose: an operator's `/tp` and `/summon`, natural lightning, a falling block nobody moved.

### 23.2 Billy's Hex Barrier

* **Stops everything from the front** (`barrierArc` 60°): projectiles (plate scan and damage hook), beams (every beam's line ends at the plate: `Shelters.across` and `WandaAbilities.shieldAcross` include it), blows (the attacker thrown back), explosions (all their damage and push), lightning, every harm with a place of origin in front, and the powers that reach for him: telekinetic grips, Magneto's strip and pull, Ultron's and Thanos's pulls, the Phoenix's mind control and reading, Quicksilver's pickpocket, Mjolnir's pin, the Darkhold's mark, Entomb, Mind Crush, Thanos's choke and soul pillar, effects and curses. Not from the side or back; harm from nowhere passes.
* **No time limit, no hunger**; the slow walk (`barrierSpeedFactor` 0.6) stays.
* **Health** `billy.barrierHealth` 60 (3.3.1; `barrierRankHealthBonus` 0.0: the same at every rank unless a server sets it). 3.3.1: only chaos magic (`BillyBarrier.isChaos`, the `chaos_beam` damage type) and dark magic (`BillyBarrier.isDark`: `DarkMagic.is(source)`, `DarkMagic.is(entity)`, `DarkMagic.casts(cause)`) wear it; everything else is stopped whole and costs nothing. A wearing hit takes its damage off; a grip or pull that does no damage costs 0, curses and effects cost 2, Entomb its total damage (at least 4). At 0 it **shatters** (64 shards, a ring of light, a flash, glass, amethyst and door-break sounds, "The Hex Barrier broke!") and B waits `barrierShatterCooldown` 200 ticks. The health is kept while lowered and mends `barrierRegenPerSecond` 1.5 a second; with less than 1 it refuses to rise ("still mending"). A normal lowering waits `barrierLowerCooldown` 10 ticks. The options `barrierDuration`, `barrierRankDurationBonus`, `barrierExhaustionPerSecond`, `barrierExplosionFactor`, `barrierStrength` and `barrierBrokenCooldown` are gone.
* **Chaos magic**: any power whose maker is a Scarlet Witch wears it down by its damage (3.3.1; before, it shattered it at once); when chaos takes its last health it says "Chaos magic shattered the Hex Barrier!", red sparks in the ring.
* **HUD**: line 4 shows "forming", then a ten-cell bar and `41/60` (green, yellow under half, red under a quarter); `client/ProtectHud` draws a big bar under the crosshair that eases to its value and flashes on a hit; the plate's cracks follow health over peak (drawn at full alpha on his own first-person plate; the near-breaking gutter is a slow pulse).
* Code: `ability/BillyBarrier` (`health`, `keep`, `lower`, `absorb`, `stopPower`, `isChaos`, `wear`); `WandaAbilities.wear` dispatches to it for a Billy. Tests: `AbwBarrierTests` (rewritten), `ProtTests`.

### 23.3 The ward of the Book of the Vishanti

* Keeps out **monsters, bosses and boss minions** (`WardManager.keepsOut`: the monster category, `#enhanced:bosses`, `#enhanced:boss_minions`); a boss or fight member is never spared (a named or leashed monster, a pet, a rider and a curing zombie villager still are; a hero's own Ultron drones are pets). Their spawns are refused. `#enhanced:ward_immune` is empty (a data pack hook).
* **What they launch stops at the edge**: projectiles of a hostile maker (the tick guard), beams of a hostile firer (`across`), harm from a hostile source outside (the damage guard), explosions set off by one outside (the blocks and entities inside untouched), effects, area attacks, and bosses' grips and pulls.
* **Dark magic stops at the edge**: the Darkhold's purple beam (drawn to the edge on clients too), its harm, hex mark, glimpse and poison, and Agatha's beam, Drain and runes (24).
* **Nothing else**: players' powers pass, chaos magic, Vision's beam and Thor included. A ward still cannot be raised over an arena.

### 23.4 Wanda's Hex: entry, the wall, the Hex Runes

* **Entry.** Her allow list is the one rule for coming in, for players and creatures: the players she allowed and their pets, creatures if "creatures allowed", her Vision; never a boss, minion or fight member. Anyone else bounces (heroes, bosses) or faces `hexDeathChance` (0.95) and, surviving, is **thrown back out** (3.2 let survivors in). Going out is as in 3.2. Every player of the level is watched every tick (any speed); a creature first seen inside is judged by where it was the tick before. Ways in checked: walking, flying and riding (the rider dismounted), Ender Pearls, Chorus Fruit, Endermen, the Bifrost, Vanish, Vibrate, the Tesseract, Thanos's portal, rewind and vortex, Apocalypse's portal, Ultron's upload, the Book of the Vishanti (all through `HexManager.onTeleport`), Quicksilver's Speed Blitz dash (now judged as a teleport), portals and dimension changes (the Witches' Road return too) and respawns inside (`HexManager.arrive`: set down outside on the ground of the column past the wall), creatures through portals, Sokovia (the lift is refused where a Hex wall cuts its column, `SokoviaValidator.cutByHex`, and the fight goes on as the Tremor; a Hex cannot be raised round the arena of a running Ultron fight), things thrown or pulled in (they stop at the wall; a grip from outside lets go). A login is not judged.
* **No power through the wall from outside** (23.1), except the powers of those it trusts (Wanda, the players she allowed, creatures if allowed). The Build Magic of others: `BlockRights.massEditRefuses` refuses every cell of a Hex whose owner did not allow a caster who stands outside, whatever `build_magic.hexIsAClaim` says (that option extends it to casters inside); `mayBreak` and `mayPlace` (block lifting, the Phoenix's fire...) refuse the same.
* **Hex Runes.** The Hex Control screen has the button *Hex Runes: glowing / none* (`HexAction.SET_RUNES`; the `Hex` record carries `runes` and `runesAt`, saved and synced). Inscribing costs `hex_runes.exhaustion` 4.0 (Creative free; refused on an empty stomach) and waits `hex_runes.cooldown` 100 ticks; wiping is free. While they glow every **other** sorcerer inside (`Sorcery.isSorcerer`) is silenced: ability keys refused ("The runes of the Hex hold your magic"; the client predicts no gesture), and at the moment they are caught grips are released, beams stopped (the Darkhold's too), Billy's barrier lowered, `endPower` run (Loki's illusions dissolve, Vanish, shapeshift and Frost Giant end), held abilities released, the toggles The Noise, God of Thunder, Frost Giant, Mind Reading, Reality Veil and Spider Legs switched off, flight ended with 5 s of Slow Falling (power flight withheld while held); the Darkhold and the Vishanti book do nothing. Wanda keeps hers; a Spectator is never held; the runes hold whether or not she allowed them in. HUD: a red line above the action bar "The Hex Runes hold your powers"; leaving: "Your magic is yours again". Look (3.7.1, after the *WandaVision* finale): `client/render/HexRunesRenderer`, at `AFTER_WEATHER` (after the walls of `WorldEffectsRenderer`, and after the clouds, which were drawn over them), draws six line glyphs (`HexRuneGlyphs`: the film's branch and gate and four of ours) as strokes, each a soft halo under a neon tube, over a level mist with scanlines behind every rune (texture `textures/entity/hex_rune_glow.png`): a dark red one with normal blending that darkens a bright sky (`EnhancedRenderTypes.oneSidedShade`), then the red glow and the strokes, additive (`oneSidedGlow`); fullbright, culled, on the sphere 0.12 inside the wall when the camera is inside and 0.12 outside when it is outside, facing the camera's side so they read the right way round; no depth writes. The layout is `hex/HexRuneLayout` (common, allocation-free, tested in `ProtTests.theRunesCoverTheWallsAndGrowWithTheHex`): each wall a quarter of the dome, rows from the ground (the lowest foot 1 block up) to 60 degrees, columns sharing the quarter evenly; a rune is 0.2 of the radius tall (2 to 14 blocks), pitch 2.0 heights across and 1.7 up, at most 4 rows of 8; 2 runes a wall at radius 8, 5 at 32, 12 at 96, 32 at 256; every wall starts with the film's two. It is laid out every frame at the radius the wall is drawn at (`ShownHex.drawnRadius`: rising, easing to a new size, sinking), with the count of the radius the Hex is set to; after a resize the old count fades into the new over 20 ticks. Inscribing draws the strokes in over 40 ticks; wiping fades them over 30; each rune breathes on its own sine with a rare flicker, steady under Reduced motion. Config `[hex_runes]`.
* Decisions: the 95% death chance for the unapproved stays (survivors are thrown back); creatures-allowed lets creatures' attacks in too; a Hex can still be raised in a Thanos fight (bosses inside are thrown out by the entry rule). Tests: `ProtTests` (a test per category), `HexTests`, `BossHookTests`.

## 24. Agatha Harkness (added in 3.3)

* `Power.AGATHA` (colour `0x9A3FE0`, glow `0xE2C2FF`), after APOCALYPSE; a plug-in (`AgathaServer`, `AgathaAbilities`, `AgathaShield`, `AgathaEvents`; client `AgathaClient`, `AgathaFx`, `AgathaHatLayer`). Awakening title "Agatha", line "The Darkhold opens to you", broadcast "... it was Agatha Harkness all along!".
* **Becoming her.** A player with **no power** holds use on the Darkhold for `agatha.awakenTicks` 60: violet motes spiral in, the whispers (`agatha_awaken`) rise, then `Powers.awaken(AGATHA)`; the book is kept. Letting go early does nothing. A hero who is not a sorcerer is refused as in 3.2; a sorcerer uses the book as in 3.2; the book is silent on the Witches' Road. `agatha.darkholdAwakens` false gives the 3.2 refusal. Decision: with `darkhold.anyoneCanUse` and `darkholdAwakens` both on, a no-power reader is woken as Agatha.
* **Keys** (none needs the book): **R** held, the Darkhold's Purple Beam (`DarkholdItem.STYLE`, damage type `darkhold_beam`, her numbers `agatha.beam*`: 8 a second, 24 blocks, every 5 ticks, 1.0 exhaustion a second; if she fires the book's beam, R neither starts nor stops it); **Sneak + R** the Darkhold's hex mark on the creature looked at, else its mind glimpse (the `[darkhold]` numbers; `Live.CASTING` 10 ticks). **G** flight (`flightSpeed` 0.085). **V** the **Power Drain** (*WandaVision*): the creature or player under the crosshair within `drainRange` 16: a player with a power (only where PvP lets her hurt them) gets the effect **Drained** for `drainTicks` 200 (no ability key works: `AbilityHandler`; a flier comes down: `PowerTicker.applyPassives`; held things let go), a creature takes `drainDamage` 6 of `agatha_drain`; either way she heals `drainHeal` 4; cooldown 300, 2.0 exhaustion; refusals cost nothing (no power, PvP or ally, nothing in sight, a ward). Decision: any hero can be drained. **B** held, the **Witch's Shield**: a violet sphere (radius 1.9) with a rune lattice; wind-up 8, at most 200 ticks, strength 40 (each stopped blow takes its damage off; at 0 it shatters and the rest goes through), stops what Apocalypse's shield stops, turns projectiles to violet sparks, recharge 40 or 200 after shattering or the time limit, 0.03 exhaustion a tick; it reuses the Chaos Shield fields of `PowerData`. **N** the **Rune Trap**: a circle (radius 4) of glowing runes where she looks (24 blocks; at a wall, on the ground at its foot; at nothing, round her feet) for `runeTicks` 100: what stands in it and her area attacks may hurt is Spellbound (renewed every 10 ticks) and takes `runeDamagePerSecond` 2 of `agatha_runes`; bosses and `spellbound_immune` are burned, not bound; one trap per Agatha; cooldown 400, 3.0 exhaustion.
* **Passive.** Magic used on her (`#enhanced:magic_damage`, and the effects Confused, Spellbound and Drained laid on her) poisons whoever used it, with or without the book (`agatha.retaliates`; the `[darkhold]` poison numbers and the per-attacker wait of `DarkholdBook.retaliate`). Body +20 max health (`bodyHealthBonus`).
* **Sorcerer, dark magic, ward.** `WardStrength.isSorcerer` and `Sorcery.isSorcerer` name her (books, ward radius `vishanti.wardRadiusAgatha` 24, Hex Runes); `WitchesRoad.use` lets her walk door to door like Wanda and Billy. Not a telepath (26). Her damage types are in `#enhanced:dark_magic`, `magic_damage` and `no_knockback`; the Drain's effect, the runes' binding and laying the trap ask `AgathaAbilities.wardStops` (a ward at the point that she is not inside): "A ward of the Vishanti keeps your dark magic out".
* **Looks** `wandavision` (deep plum velvet dress, high black collar, a gold brooch with an amethyst, flared black cuffs, dark-brown waves), `all_along` (dark green velvet coat over a black dress, silver clasp, black belt, and the hat, a model of its own: black felt, green band, silver buckle, the point bent back; hidden by a helmet), `own`; slim arms. Eyes glow violet (`agatha_eyes.png`) while the beam burns, the shield is up, the hex is cast, and 24 ticks after a drain or the runes. Sounds (original, synthesised): `agatha_awaken`, `agatha_drain`, `agatha_runes`, `agatha_shield_up`, `agatha_shield_hit`, `agatha_shield_break`. Payload `AgathaFxPayload` (RUNES, RUNES_END, DRAIN, SHIELD_BREAK). HUD: R "active", G on/off, V cooldown, B the shared shield line, N "runes 3.2s" then the cooldown; the Darkhold panel of a no-power holder says "Hold use: read it and become Agatha". Config `[agatha]` (28 options). Tests `AgathaTests`, `AgathaAnimationTests`; three render rounds in a real client.

## 25. Cyclops (added in 3.3)

* `Power.CYCLOPS` (`0xE8323A` / `0xFFB3A8`), a plug-in (`CyclopsServer`, `CyclopsAbilities`, `CyclopsGlasses`; client `CyclopsClient`, `CyclopsView`, `CyclopsBeamFx`, `CyclopsFx`, `CyclopsVisorLayer`; `event/CyclopsEvents`). Not a flier. A mutant without telepathy.
* **The glasses**: `enhanced:ruby_quartz_glasses` (`RubyQuartzGlassesItem`, a helmet of material `enhanced:ruby_quartz`, 1 armour, no durability, epic, fireproof, stack 1). Worn in the head slot by a player with no power they make him Cyclops (look `apocalypse`); on another hero they are only glasses. Loot: the table `enhanced:inject/ruby_quartz_glasses`, used by the Mutant School (`chests/mutant_school/cyclops`, always one) and by the loot modifiers of `woodland_mansion` and `stronghold_library` (2% each). No recipe.
* **Body** +10 max health (30), armour 4.
* **Keys.** **R** held, the **Optic Blast**: a `ContinuousBeam` of style `cyclops_optic` from his eyes to what the crosshair is on; 2.0 every 5 ticks, 32 blocks, each hit shoves 0.35, 0.6 exhaustion a second; needs the glasses on and food. **Sneak + R** the **Ricochet Shot**: one bolt that bounces off blocks (3 bounces, 24 a stretch), 7 to each creature once; a Chaos Shield or a Hex Barrier stops it and is paid; cooldown 40. **G** the **Concussion Blast**: a cone of 40° half-angle, 7 blocks, in sight, 6 damage and a 1.6 throw, turns projectiles away; spares allies, PvP-off players and passive creatures; cooldown 100. **V** the **Optic Leap**: a blast at the ground (within 4 below) throws him up 1.1 and forward 0.9, a ground blast (radius 2.5, 3 damage), no fall damage until he lands; cooldown 80. **B** the **Rally** ("To me, X-Men!"): he and the players, tamed pets and golems within 16 get Speed I, Strength I and Resistance I for 300 ticks (glasses off too); cooldown 900. **Sneak + N** takes the glasses off, **N** puts them on (never waits).
* **The glasses off**: `CyclopsGlasses.tick` compares the head slot every tick; the glasses leaving it in **any** way (Sneak + N into the main hand, else the inventory, else the off hand, else dropped; dragged out; `/item replace`) starts `Live.CYCLOPS_UNLEASHED` and a beam of style `cyclops_unleashed` (range 48, 6 every 10 ticks, radius 1.2: everything within the radius of its line up to the first block is hit and shoved 0.9, through creatures). It stops only when the glasses are back in the head slot. Safety: never hurts him, no hunger, no block damage unless `unleashedBreaksBlocks` (then hardness up to `unleashedMaxHardness` 3, never within 2.5 of his feet, only where `BlockRights.mayBreak`), pauses while Entombed, ends at death, logout, power loss and Spectator; a respawn or login without the glasses never starts it (only a removal does). With no glasses anywhere, N shuts his eyes. Players only with PvP on; passive creatures are hurt (an uncontrolled beam); teammates and pets spared.
* **Damage type** `enhanced:optic_blast` (`bypasses_cooldown`, `no_knockback`, `panic_causes`; not magic). **Beams drawn**: the new hook `BeamRenderer.Drawer` (`registerDrawer(style, drawer)`) lets a style draw its own beam: straight layered beams from the eyes with pulses; the giant one with three spiralling strands, through creatures to the block; in first person two thin beams from just below the view meeting at the crosshair.
* **Looks** `apocalypse` (navy field suit, black side panels, quilted shoulders, steel-blue piping, a silver X buckle; a gunmetal wrap-around visor with one ruby lens), `classic` (blue bodysuit, yellow X harness, belt, gloves, boots, blue cowl; a yellow visor band with a red lens), `own` (red sunglasses). The skins have no visor: `CyclopsVisorLayer` draws the glasses (a band at the eyes, double-density textures, glow masks; the lens glows a little always and with the weight of the Optic Blast's stance). Sounds (sounds/cyclops/): optic start and loop, unleashed start and loop, ricochet, concussion, leap, rally, visor. Stances `CYCLOPS_BLAST`, `CYCLOPS_UNLEASHED`; gestures for the ricochet, concussion, leap, rally and the glasses. Payload `CyclopsFxPayload`. Config `[cyclops]` (35 options). Tests `CyclopsTests`, `CyclopsAnimationTests`; four render rounds with two clients.

## 26. Professor X (added in 3.3)

* `Power.PROFESSOR_X` (`0x6F8CFF` / `0xDCE4FF`), no flight; a plug-in (`XavierServer`, `XavierAbilities`, `MindLink`, `Thoughts`; client `XavierClient`, `XavierControl`, `XavierOverlay`, `XavierChair`, `ThoughtBubbles`).
* **The wheelchair**: `enhanced:xavier_wheelchair` (`XavierWheelchairItem`, an `AwakeningItem`: hold right-click `wheelchairRitualTicks` 60; used up, not in Creative). Recipe `L.. / LLE / I.I` (leather, an Eye of Ender, iron ingots), unlocked by an Eye of Ender; also in the Mutant School (`chests/mutant_school/study`, always one). On awakening the server sends `XavierPayload.CHOOSE` and `XavierChoiceScreen` asks "Which Charles are you?": `young` (default; *Days of Future Past*, walks) or `old` (Patrick Stewart, sits in his wheelchair); `XavierChoicePayload`, server-checked; the Change Look key changes it later.
* **Telepathy** (`ability/Telepathy`, the rule book for everyone): `rank` (PROFESSOR_X = TELEPATH; SCARLET_WITCH, PHOENIX, BILLY = STRONGER; everyone else NONE, Vision too, Agatha and Cyclops too), `isShielded` (Magneto's helmet or `#enhanced:telepathy_immune`), `reach(telepath, target)` = OPEN, SHIELDED, TELEPATH (the same rank keeps him out) or THROWS_OUT (stronger). A mind is open only if its rank is lower than the reader's.
* **Keys.** **R** the **Mental Blast** (24 blocks, in sight; 6 of `enhanced:mind_blast`, ignores armour, in `magic_damage`; Nausea and Slowness II for `blastDizzyTicks` 60; players only with PvP; cooldown 40). **G** **Hear Thoughts** (`Toggle.HEAR_THOUGHTS`): every 40 ticks a `ThoughtsPayload` of the 16 nearest minds within `thoughtsRange` 24, lines `thought.enhanced.<kind>.<n>` chosen from what the creature does (hurt, hungry, flying, armed, mining, a hero, hunting with its prey named, a baby, a pet, and thirty species); a closed mind gives "(a mind closed to you)"; `client/ThoughtBubbles` draws italic words over heads in mood colours; `client/TheNoise` runs for him too (glow in his colour, whispers). **Billy's The Noise shows the words too.** **V** **Take Over a Body** (24 blocks, in sight; again: return): a creature's goals lose their MOVE, LOOK, JUMP and TARGET flags while steered (re-disabled every tick), its brain forgets its walk and attack targets, his keys set its `MoveControl` (3-D for fliers and swimmers) and his look its `LookControl`, a click strikes (a creeper ignites, a ranged mob shoots, else `doHurtTarget`); a player (PvP on, `linkPlayerTicks` 160) gets `Live.MIND_CONTROLLED`: their client stops their keys and clicks, the server cancels their attacks, uses and ability presses and sends them a velocity each tick. His client's camera is the body's eyes, turned by his mouse; his input is a wish (`XavierInputPayload`, clamped, NaN-safe, never a position). It ends on V, time (`linkCreatureTicks` 600), distance over `linkMaxDistance` 48, another dimension, either body's death, removal, logout or dimension change, his own body hurt (`linkBreaksOnHurt`), the mind closing, PvP off, Creative or a Road trial (players), power loss and server stop (`MindLink.endAll`); a freed creature is Confused 2 s; a watchdog clears a stuck bit. Refused: a body already steered, a Mjolnir pin, a stranger's pet with PvP off, armour stands, illusions, closed minds. A stronger telepath throws him out: `backlashDamage` 4, Nausea, Slowness IV and Weakness for `backlashStunTicks` 60, pushed back, V rests, a red flash. **B** **Freeze Minds**: every open mind within `freezeRadius` 12, through walls, is Spellbound for `freezeTicks` 100 (not his pets or team, closed minds, creatures too big for a spell, players without PvP; nobody held costs nothing); cooldown 600. **N** **Cerebro**: the helmet on for `cerebroTicks` 200; every player of the dimension within `cerebroRange` 256 not under Magneto's helmet is found (telepaths too), marked through the world and listed with arrows (`CerebroPayload`), and told; cooldown 1200.
* **The chair** (a decision): the old Professor X sits in his wheelchair at all times, drawn as a player layer (an original model: steel frame, black leather, two big spoked wheels with tyres that roll by the distance each side moved, interpolated with the partial tick, casters, a footrest); not while swimming, sleeping, gliding, riding or spectating; stance `XAVIER_SEATED`. Not a rideable entity: nothing to dismount, no desync. The young one walks.
* **Looks** `young`, `old`, `own`; skins `xavier_young`, `xavier_old`; `cerebro_helmet`. Sounds `xavier_awaken`, `_blast`, `_thoughts`, `_link`, `_unlink`, `_backlash`, `_freeze`, `_cerebro`. Payloads `XavierPayload`, `XavierInputPayload`, `XavierChoicePayload`, `ThoughtsPayload`, `CerebroPayload`. Config `[professor_x]` (20 options). Tests `XavierTests`, `XavierAnimationTests`; three render rounds.

## 27. The Hero Creator (added in 3.3)

Asked for later in the same release (F1 to F5): a Creative-only item to become any hero, or make one. (3.4 changed the limits, the format and the editor, and added the heroes' abilities: see section 33.)

* **Item** `enhanced:hero_creator` (creative tab only, no recipe or loot, stack 1). It works for a player in Creative or an operator (`custom_hero.operatorsOnly`: operators only; `enabled`); the server checks every button again.
* **`Power.CUSTOM`** after PROFESSOR_X (enum colours `0xF5A623` / `0xFFE7A8`, the creator's gold; a custom hero's colours are its definition's: `Powers.variant` returns them). No code per hero: the definition is data.
* **Data**: the attachment `enhanced:custom_hero` (the definition, saved as its JSON string, copied on death, synced to everyone; kept when the player becomes another hero, so "Edit My Hero" brings it back) and `enhanced:custom_state` (switched-on powers, stored energy, the Force Field; synced, not saved). One client-to-server payload, `enhanced:hero_creator` (CHOOSE a power and look, APPLY a definition); the skin travels inside the synced definition (at most 24 KiB on the wire).
* **The catalogue** (`custom/PowerModule`): the thirty most common superpowers, researched with sources (a ranking of the most published Marvel and DC characters' abilities weighted by appearances; counts on SuperheroDB; the Kaggle / IEEE "Super Heroes Dataset"; Wikipedia), close variants merged as options (healing + regeneration, phasing + intangibility, shapeshifting + size, energy + power absorption): super strength, durability, super speed, flight, healing factor, energy blast, super senses, telepathy, teleportation, telekinesis, wall-crawling, force field, phasing, animal control, mind control, magic, ice, weather, lightning, shapeshifting, invisibility, absorption, fire, duplication, water, time, technopathy, sonic scream, earth, wind. Each has a cost per strength point (1 to 3), options (the first the default) and, per option, how its key acts (none, press, hold, toggle).
* **Strength** (F5): every picked power has a strength 1 to 10 (`maxStrength`); every number of a module is a `ModuleStats.Stat` with its value at 1 and at 10 and a straight line between (damage, range, radius, speed, duration up; cooldown and hunger down); the server reads exactly these, and the editor shows them beside the slider with the server's `damageScale` and `cooldownScale` applied. The cost is points per strength times strength; default budget 60, at most 8 powers.
* **Rules** (`CustomHeroRules.sanitize`, pure, run by the client's screen too): the name cleaned (formatting and control characters out, at most 24, "My Hero" if empty); unknown, disabled and repeated powers dropped; strengths clamped; unknown options to the default; past `maxPowers`, then past the budget, dropped from the end; keys naming a missing or passive power cleared; the skin a 64x64 PNG within `maxSkinBytes` (16384) or dropped; an unknown look or missing skin becomes the player's own. It never refuses a hero outright: it makes the best the rules allow and says what it changed.
* **JSON** (`CustomHeroJson`): `{"format":"enhanced:custom_hero","version":1,"name","color","glow","look":{"base","slim"},"skin":"<base64 PNG>","powers":[{"id","strength","option","color"}],"keys":[5 ids]}`; reading is lenient (unknown fields ignored, a missing field takes its default, a hand-written `level` 1 to 3 reads as 3, 6 or 10, no strength reads as 5, a newer version read as far as understood, each thing not understood noted). Files `enhanced/heroes/<name>.json` (and the clipboard) and `enhanced/skins/*.png` in the game folder; a PNG or a hero file can be dropped on the window.
* **Reuse**: flight is the creative-flight modifier of `PowerTicker`; Super Run sets `Live.SPEEDING`; the beam is a `ContinuousBeam` with a `BeamStyle` per colour pair (`custom_RRGGBB_RRGGBB`, made on a client from its name); telekinesis is the shared one (modes `CUSTOM` and `CUSTOM_BLOCKS`); phasing is Vision's; the creature shape is Loki's form; duplication is `LokiIllusionEntity` with a new `FIGHTER` mode; Stop Time is `SPELLBOUND`; the Force Field neutralises projectiles as Wanda's shield does; teleports go through `HexManager.onTeleport`; area attacks through `AreaTargets.mayHurt`; no custom power crosses a Hex wall (`Shots.hexCut`) or a Chaos Shield. Earth changes no block; the Sonic Scream breaks glass only with `blockChanges`. A custom hero flies in the vanilla pose.
* **Client**: `CustomSkins` (a `DynamicTexture` per skin, checked again, cached by SHA-1, released 30 s after nobody wears it); the hero's HUD panel in its colours. Screens (for an eleven-year-old: big buttons, few words, EN and HE): the start page (Choose a Hero, Create / Edit My Hero, Stop Being a Hero), Choose (a card per hero from `Power` and `Variants`, faces drawn from the skins, then looks, then Become!), and the editor's tabs Name, Powers, Keys, Look, Save, with a paper-doll preview on each.
* Damage types `enhanced:hero_power`, `enhanced:hero_beam`; twelve synthesised sounds. Config `[custom_hero]` (19 options; the scales `flightSpeed`, `forceFieldStrength`, `controlSeconds`, `teleportRange` and `suppressSeconds` multiply the strength-1 to strength-10 numbers by their value over the default). Tests `CustomHeroDataTests` (round trips, the rules, all thirty modules and their numbers, the words in both languages) and `CustomHeroModuleTests` (every module's server behaviour, the permissions, the config).

## 28. The world: structures and the Road's music (added in 3.3)

* **Three structures** (package `world`, `registry/ModStructures`), all data: `data/enhanced/worldgen/structure/*.json`, `structure_set/*.json`, the biome tags `has_structure/*`, processor lists, templates in `data/enhanced/structure/`. Overworld only; they generate in new chunks of old worlds; `/locate structure` works. Two structure types: `enhanced:template_site` (`TemplateSiteStructure` and `TemplateSitePiece`: one NBT template turned a random way; `floor` = the template layer at ground level, the layers below are cellars; `max_slope`; `processors`, saved with the piece) and `enhanced:crater` (`CraterStructure`, `CraterPiece`: `radius` 9 to 12, `depth` 4 to 6, `max_slope`, `core` = `enhanced:mjolnir`, all from the column and the crater's seed); the processor `enhanced:foundation` fills down under every column where the ground falls away (the last processor of a list). A site needs nine footprint points within `max_slope` of each other (outpost 4, school 6, crater 5), all on dry land at or above sea level. Decision: code types driven by data rather than jigsaw (jigsaw cannot sink a cellar, refuse slopes or follow the ground).
  * **Hydra outpost** (`hydra_outposts` 36 / 12, salt 330141; taigas, snowy plains and taiga, grove, forests, birch forests, plains, meadow, windswept forest; surface structures; pillagers spawn in it): template `hydra_outpost/base` (33 × 22 × 29, floor 5): a fenced compound with a gate and signs, a concrete bunker, banners, barracks, a command room, a roof control room with mast and dish, a cellar lab and **the vault**: a chest on a pedestal with `chests/hydra_outpost/vault`, **Loki's Scepter, exactly one, always**; also `lab`, `barracks`, `supply`. The five `scepter_from_*` loot modifiers and `inject/lokis_scepter` are removed: the outposts are the only source.
  * **Mjolnir's crater** (`mjolnir_craters` 32 / 10, salt 20110506; desert, badlands, savannas, plains, sunflower plains; top layer modification, after trees): a bowl of burnt rock, rubble, two campfires, a thrown-up rim, scorched streaks and ring (fused glass in sand), and in the middle on polished blackstone **the Mjolnir block** with an empty block entity (unowned): today's rules (the first worthy lifts and claims it; "It won't budge"). Mjolnir's recipe stays.
  * **Mutant School** (`mutant_schools` 40 / 14, salt 19630901; plains, sunflower plains, meadow; top layer modification; outposts and craters keep 6 chunks away): template `mutant_school/mansion` (47 × 34 × 44, floor 6): a weathered stone mansion with slate roofs, a tower and the school's banner, grounds, a gate sign (EN/HE), the hall, the **Professor's study** (`chests/mutant_school/study` with `.../wheelchair`: the wheelchair, exactly one), the classroom, a dining table, the library, two dormitories, **Scott Summers' room** (`.../cyclops` with `.../cyclops_glasses`: the glasses, exactly one), and Cerebro below. The two item tables carry `neoforge:item_exists` conditions.
* **The Road's music** (C4): an original ballad in 6/8, E minor with a Phrygian F, whose head is the coven's tune of the 3.1 door sound; 16 bars; not the show's song or melody. One arrangement per stage (mono 44.1 kHz Vorbis, `assets/enhanced/sounds/music/road_*.ogg`, `stream: true`): `entrance` (`enhanced:music.road.entrance`, 32 s: the coven hums over a drone, bells, a heartbeat drum), `parkour` (`music.road.parkour`, 48 s: lute arpeggios and harpsichord), `structure_copy` (`music.road.copy`, 57.6 s: music box over a choir), `ice_boat` (`music.road.boat`, 48 s: drums, driving strings, horns), `road_end` (`music.road.ending`, 51.2 s: full, the second half in E major). Server `road/RoadMusic` sends `RoadMusicPayload(stage)` when it changes (checked every 10 ticks): `entrance` from the ritual at a door while the seeker is within 16 blocks and while he is taken in, the trial's id in a trial, `road_end` in the hall, nothing elsewhere; a trial without its own arrangement plays the parkour's. Client `RoadMusicPlayer`: fade in 2 s, crossfade 2 s, fade out 3 s, the MUSIC category, the game's own music held back, stopped on logout. A resource pack replaces any of the five events with `"replace": true`. Tests `WorldStructureTests`, `RoadMusicTests`.

## 29. Animations without flashing (3.3)

Eilon: "make all the animations smooth with no flashing". Every animation and effect was audited in code and in a real client (a dedicated server and two clients, before and after, slow ticks so that what moves only on the tick shows as a jump). Client-only changes, no gameplay. Crackles that jumped to a new random shape every 1 to 3 ticks now flow (`EnergyGeometry.flowingPath`, `bolt(.., phase)`) or crossfade (`EnergyGeometry.crossfade`): every 3.2 continuous beam, Thor's arcs, beam, fragments and crackle, the Chaos and Mind Stone bolts, Magneto's field bolts, the telekinetic tether, the Hex Barrier's edge arcs, Thor's flight lightning, Ultron's bolt crown and Billy's Build Magic sparks. On-off blinks became smooth pulses (refused spell frames, the rubber-band label, the meter, Sentry eyes, failing shields at about 2 Hz instead of 6 to 7, Tommy's shiver). Popping became fades (the ward dome beyond 64 blocks, a lowered or broken shield, the lowered barrier, a resized or dissolved Hex, Wanda's flight aura and Vision's phase shimmer). Jumps were smoothed (the Hex mesh and tiling from the final radius, a thrown hammer's heading, Ultron's ritual silhouette, the mind-reading scanline, the HUD panel's width). Reduced motion still turns the throbs and crackles off. Known leftover: world effects take their clock as a float of the game time, which loses the partial tick on worlds older than about 58 hours of play.

## 30. How 3.3 was put together

* **Lanes** in their own worktrees and branches, merged into the integration branch `v33`: PROT (23), AGATHA (24), CYCLOPS (25), XAVIER (26), CUSTOM (27), WORLD (28), ANIM (29), SPEED (31), SPIDER (32) and DOCS. Shared files (`EnhancedConfig` and the config classes, both lang files, `ModNetwork`, `ModAttachments`, `ModTags`, `ModItems`, `Power`, `ClientSetup`, `PowerHud`, `TestKit`, `DataTests`) were edited minimally and additively, every key in both languages. The new powers come after APOCALYPSE in the fixed order **AGATHA, CYCLOPS, PROFESSOR_X, CUSTOM**; every power after Thor is a plug-in (`HeroServers`, `HeroClients`).
* **Network protocol "7"** (one bump for all of 3.3; `SkeletonTests.theProtocolIsSeven`): `AgathaFxPayload`, `CyclopsFxPayload`, the five payloads of Professor X, `enhanced:hero_creator`, `RoadMusicPayload`, `SurfaceRunPayload`, `SpiderSwingPayload`, the changed `Hex` record and `HexAction.SET_RUNES`. A 3.2 client cannot join a 3.3 server.
* **Config.** New sections `[agatha]`, `[cyclops]`, `[professor_x]`, `[custom_hero]`, `[hex_runes]`, `[speedsters]`, five Web Assist options in `[spider_man]` and two client options (`[spider_man]` in `enhanced-client.toml`), the option `vishanti.wardRadiusAgatha`, and Billy's barrier block rewritten (23.2). Billy's `barrierCooldown` is renamed `barrierLowerCooldown` (default 10), so a server file written by 3.2 does not keep its 200.
* **Tests.** New classes `ProtTests`, `AgathaTests`, `AgathaAnimationTests`, `CyclopsTests`, `CyclopsAnimationTests`, `XavierTests`, `XavierAnimationTests`, `CustomHeroDataTests`, `CustomHeroModuleTests`, `WorldStructureTests`, `RoadMusicTests`, `SpeedsterSurfaceTests`, `SpeedStrikeTests` and `SwingAssistTests`; `AbwBarrierTests` rewritten; `SkeletonTests` checks protocol 7, the order of the powers and that the heroes after Thor are plug-ins.

## 31. The speedsters: two powers each, walls and ceilings, new attacks (added in 3.3)

Eilon asked for three things for Quicksilver and Tommy (H1 to H4): run up walls and along ceilings, the speedster way; slow time and running as two separate abilities; and more attacks.

* **Two powers, two keys.** Quicksilver: **R** Quicksilver Time (slow time) and **G** Super Speed (running), independent. Tommy: **R** Slow Time (new, `Toggle.TOMMY_TIME`) and **G** Super Speed (on from the start). Slow time slows the world (`SlowMotion.slowsTime`: any speedster's slow time is a zone, with `quicksilver.slowMotionFactor` and `slowMotionRadius`); he runs at super speed only while running is on and he sprints. Quicksilver's "everything four times as fast" holds while either is on. Costs: running at the full rate; slow time alone at the 3.2 idle rate for Quicksilver (half of `quicksilver.exhaustion`), `speedsters.tommyTimeExhaustion` 0.01 a tick for Tommy (starving stops it); both on and running costs only the running. The slow-time shade and the world tint take the colour of the speedster whose zone holds you (`SlowMotion.zoneOwner`: blue-grey for Quicksilver, teal for Tommy, `shaders/post/slow_motion_tommy.json`). Tommy's Slow Time has no song.
* **Final keys.** Quicksilver: R Quicksilver Time | Sneak+R Walkman; G Super Speed | Sneak+G Dash Strike; V Speed Blitz | Sneak+V Pickpocket (shares V's cooldown); B Wall & Water Run (walls, ceilings, water); N Pebble Barrage. Tommy: R Slow Time | Sneak+R Wall Run on/off (`Toggle.TOMMY_WALL_RUN_OFF`: on unless switched off); G Super Speed | Sneak+G Dash Strike; V Whirlwind | Sneak+V Sonic Boom (shares V's cooldown); B Vibrate Through Walls | Sneak+B Disarm Blitz (shares B's cooldown); N Molecular Acceleration. The gesture of a shared key is told by the cooldown's length, or by `Live.SONIC_BOOM` (6 ticks).
* **Walls and ceilings.** The rules are pure (`ability/SurfaceRun`, shared by the client, the server and the tests). Start: super speed, sprinting, **W** pressing within 60° into a wall, horizontal collision: up the wall at `surfaceSpeedFactor` 0.8 of the ground speed (at most `surfaceMaxSpeed` 1.0 block a tick). A ceiling over the wall he runs up: along the ceiling, away from the wall, steered by the look (`ceilingRun`); a wall ahead on a ceiling: down it to the floor; the end of a ceiling with an outer face: round the lip and up it; the top of a wall: hop over onto it. A hole or the end of a wall in mid-air, or momentum under `surfaceMinSpeed` 0.22 (W let go, sprint lost, food gone): he drops off, pushed 0.15 away. Contact may be lost for 2 ticks (a one-block gap is crossed). The hitbox never changes and every move goes through vanilla collision. The client moves the local runner (`client/SpeedsterMotion`: the whole velocity each tick, the camera turned 180° at 30° a tick when the run reverses, **Space** leaps off) and tells the server (`SurfaceRunPayload`); the server (`ability/SpeedSurfaces`) believes it only for a speedster who may run, with a block within 0.6 on that side, raises `Live.SURFACE_WALL` / `SURFACE_CEILING` for everyone, resets the fly-kick counter, and cancels fall damage until he lands (at most 10 s, `event/SpeedEvents`). Like Spider-Man's cling, the movement is the client's and the server validates it. `wallRun` false: they stop at walls as in 3.2.
* **Animation.** `client/render/SpeedSurfacePoses`: one eased "surface pitch" (-90° up a wall facing it, -180° on a ceiling, +90° down a wall) about a pivot so the feet are on the surface, the body turned to face it, the run cycle fed with the speed along the surface; other players read the synced flags, the side of the block and his facing. `client/SpeedSurfaceFx`: the blur along the turned body in the hero's colours, and speed lines on his own screen (GUI layer `speed_lines`, off with *Flight effects*). HUD: slow time ON/OFF (♪ for the Walkman), running ON/OFF and `running` / `on the wall` / `on the ceiling`.
* **Strikes** (`ability/SpeedStrikes`, reusable with a `Style` of colours; players are hit only when PvP lets the striker harm them, and the Hex is respected): the **Dash Strike** (both, Sneak+G): a dash along the look of `dashRange` 12 blocks, the body swept along the path so it never ends in a block (it stops at the first wall), `dashDamage` 5 to everything in the path and a shove aside, `dashCooldown` 100. The **Pebble Barrage** (Quicksilver, N): `barragePebbles` 8 hitscan pebbles in a narrow cone, `barrageRange` 24, `barrageDamage` 1.5 each with no hit cooldown, `barrageCooldown` 120, grey streaks. The **Sonic Boom** (Tommy, Sneak+V): he stops and a shockwave of `sonicBoomRadius` 6 does `sonicBoomDamage` 4 (×1.5 when he was running, falling off to half at the edge), throws creatures away and deflects projectiles, `sonicBoomCooldown` 160 (shared with the Whirlwind), a teal ring. Kept: the Speed Blitz, the Pickpocket, the Disarm Blitz, the Whirlwind, Molecular Acceleration. Gestures `SPEED_DASH`, `SPEED_TIME`, `QS_BARRAGE`, `TOMMY_BOOM`.
* Config `[speedsters]` (16 options). Tests `SpeedsterSurfaceTests` (the rules on a real server player: up and over, ceiling and lip, down the far wall, drop when slow, tight corridors, slabs and stairs never inside a block, the Space leap, the server's checks, flags, kick counter, toggles, no fall damage) and `SpeedStrikeTests`; checked in a real client with two players (frame sequences of both heroes on walls and ceilings, first and third person).

## 32. Web Assist: easy web swinging (added in 3.3)

Eilon asked for swinging that an eleven-year-old can do anywhere, especially in forests (G1, G2). The research (games' swing assists, Minecraft swing mods) is in the lane's notes.

* **On by default.** Each Spider-Man switches it with the key **J** (`key.enhanced.web_assist`, in the Enhanced key category) or the client option `[spider_man] webAssist` (remembered in `enhanced-client.toml`); `webAssistPreview` shows the next anchor. The server can forbid it (`spider_man.webAssist` false: every swing manual). HUD: the G line says `assist` or `manual` when he is not zipping or swinging.
* **Swing:** Sneak + G, then keep G held (Sneak may be let go); G alone in mid-air at nothing is an assisted swing too. `ability/SwingAssist` (pure: choose, pivot, fly-through, reel, pump, release, steer, hop, ground) picks the best anchor ahead and above within `webAssistRange` 30 (never past `swingRange`): leaves and logs (tall trees first), houses, cliffs; among good ones the crosshair's block wins. Each candidate swing is flown through before it is taken, so it never runs into the ground (near the ground or water the web is reeled in), the leaves or a trunk. **W** pumps, **S** brakes (no reeling), **A/D** push sideways, the swing turns gently toward the look (3° a tick), it lets go by itself at the best moment with a small launch, and while G is held the next web goes out as he begins to fall (or a moment after landing on a roof or tree top). Between two webs the air drag is much lower for 2 seconds. Letting go of G stops; Space lets go with a leap.
* **Open land:** `webAssistOpenLand`: `sky` (default; a web to the open sky ahead and above, only with open sky over it, at most `skyAnchorMaxTicks` 60), `zip` (a web boost forward and up, `webBoostCooldown` 20), or `none`.
* **Preview:** `client/render/SpiderAssistPreview` draws a small white web (a ring with six spokes; pale blue for a sky point) where the next web would go, while he is in the air or sneaking.
* **Server-authoritative:** the client chooses and moves (as for every Spider-Man movement) and tells the server (`SpiderSwingPayload`: attach, release, boost); the server checks each assisted web as its own swing (allowed, power, not held, hunger, ready, range + 4 for lag, a solid block behind the anchor and in sight or open sky above a sky point, the Chaos Shield) and refuses the rest (the client stops at once).
* **Manual mode** (assist off) is the 3.2 swing exactly. **Known limit:** under a dense, low canopy (a dark forest, leaves 3 to 4 blocks up) there is nothing safe to swing from and no sky: the assist finds nothing there (manual swing, zip and climbing still work).
* Five server options in `[spider_man]` (`webAssist`, `webAssistOpenLand`, `webAssistRange`, `skyAnchorMaxTicks`, `webBoostCooldown`), two client options. Tests `SwingAssistTests` (anchor selection in forest, flat and city set-ups, the fly-through, release and chain).

## 33. The Hero Creator as good as the heroes (added in 3.4)

Eilon asked to "audit and change the whole Hero Creator so it can make high-quality heroes, with animations, as good as the normal heroes, with all abilities possible, and those the normal heroes have" (R1 to R7), and then that choosing an existing hero also gives that hero's items (R8). The player's guide is the wiki's [Hero Creator](wiki/Hero-Creator.md) and [Heroes' Abilities](wiki/Hero-Abilities.md).

### 33.1 The audit

A 3.3 custom hero was compared with the built-in heroes on every axis: abilities and how they feel, gestures, stances, flight, run, first-person hands, idle, effects, sounds, HUD, keys (Sneak variants, hold and toggle), passives, roles (sorcerer, telepath, worthy...), walls and bosses, looks and the editor. The gaps that mattered most: none of the heroes' abilities could be taken; five keys with no Sneak variants; every power acted with the vanilla pose; no layers on the skin; an editor of thirty cards. Everything below closes them.

### 33.2 Every hero's ability is a row (`custom/HeroAbility`)

* **81 rows**: 71 keys of the sixteen heroes, 9 bodies (Thor, Vision, the Phoenix, Spider-Man, Thanos, Ultron, Apocalypse, Agatha, Cyclops; Vision's `vision.body` is new in 3.4: +10 health, +8 armour, never hungry or out of breath, metal) and Billy's Build Magic (`billy.build_magic`, the **Y** key, Billy's rules). The fliers' flight keys are not rows: they are the flight styles (33.6). The id is `<hero>.<name>` (`wanda.*`, `spider.*`, `xavier.*` for the Scarlet Witch, Spider-Man, Professor X) and is what a file names; the names are `hero_module.enhanced.<id>` (+ `.alt`, `.desc`) in both languages.
* **A row is data**: the hero, its home key (1 to 5, 0 a body, 6 Y), how its key acts and how its Sneak variant acts (press, hold, toggle), its cost per strength point, its group (Body, Move, Energy, Elements, Mind), an icon, the resources it drives, the roles it gives, what its strength scales (damage, cooldown, body; `fixed`: nothing, no slider, cost as at 5), whether it is a world power, what it requires, an `on` predicate (for the HUD and the stance override) and its family (cards stacked in the editor). `CatalogEntry` and `Catalog` give the thirty `PowerModule`s and the 81 rows one face.
* **The code is the hero's own.** The router calls `HeroKeys.press(player, data, row, down, alt)` → `AbilityHandler.heroKey(hero, player, data, home, down, alt)`; every hero has a five-argument `onKey(..., boolean alt)` (the plug-ins override `HeroServer.onKey(p, d, ability, down, alt)`) and the four-argument form passes `player.isShiftKeyDown()`, so the built-in heroes behave exactly as before. Nothing of a hero is copied.

### 33.3 The kit: "has this ability", not "is this hero"

* `HeroKit` (built from the definition on both sides, cached per definition by `CustomState.kitFor`, put on the data by `Powers.get`) answers `data.has(HeroAbility)`, `data.acts(Power)` (any row of that hero), `data.slot(row)`, `startCooldown(row, now, ticks)` / `isReady(row, now)` (the hero's home key mapped to the custom hero's column, the length divided by the strength factor and multiplied by `custom_hero.cooldownScale`) and `bodyScale(row)`. For a built-in hero `has` is `power == hero`, the slot is the home key and the scale 1: the heroes are unchanged by construction.
* Every hero lane applied the same rules: cooldowns by row; `power == X` meaning "this ability" became `has(row)` and meaning "the hero's upkeep" `acts(X)`; each tick part is guarded by its row (a hero's tick now runs for every custom hero with any of his rows); bodies gate on their row and scale by `bodyScale`; colours of effects come from `Powers.color/glow(player)` (the hero's own for him, the custom hero's for a custom hero; fixed palettes such as Thor's blue lightning, White Vision and the Hex stay); each hero got an `endAll` that runs when a custom hero with any of its rows loses his powers, is remade, **dies** or logs out.
* **Roles** are asked through `Traits.has(player, Trait)` instead of lists of heroes: `SORCERER` (the Hex Runes hold him; the Book of the Vishanti and the Darkhold), `TELEPATH` (Professor X's rank), `TELEPATH_STRONG` (the Scarlet Witch's, Billy's, the Phoenix's), `WORTHY` (Mjolnir), `METAL_BODY` (Magneto's powers take hold), `IRON_LEGS`, `DARK_MAGIC`, `FIRE_IMMUNE`. A built-in hero has the traits the old lists gave him; a custom hero those of his rows (all Wanda, Billy, Loki and Agatha rows are sorcerers; Professor X's are telepaths; `thor.hammer` is worthy; `vision.density`, `vision.body` and `ultron.body` are metal; `spider.legs` gives the legs; Agatha's are dark magic; `phoenix.body` is fire-immune) and of the generic Magic (sorcerer), Telepathy and Mind Control (telepath) and Fire (fire-immune).
* **Decisions per hero for a custom hero.** `thor.hammer` still needs Mjolnir or Stormbreaker in hand (throw, call back, shatter, pin); `thor.lightning` works bare-handed; `thor.bifrost` calls the Bifrost without a weapon (`ThorAbilities.callsBifrost`; its wait is the row's scaled cooldown); lightning immunity with the hammer, lightning, thunder or body row. Cyclops: only the hero needs the glasses (`CyclopsAbilities.needsGlasses`); for a custom hero `cyclops.glasses` is *Shut Eyes* / *Open Eyes* (Sneak opens them and the giant beam pours out). Spider-Man: `spider.legs` grows the legs in any look (`mayGrowLegs`), web blocks let any custom hero with a Spider-Man row through, **J** works with `spider.zip`. Ultron: the drones obey a custom owner and dissolve when he loses his powers, dies or leaves; Second Life belongs to `ultron.upload`. Loki's Frost Giant drives no size (armour only) and paints the custom skin. The Phoenix's body is fire immunity only; her Burst hovers only with Flight. Quicksilver's Wall & Water Run requires his speed or time; Tommy's wall run belongs to the Sneak variant of `tommy.time`. The Hex of a custom hero dissolves on death, loss of the power or a redefinition without it (logout keeps it, as Wanda's). Build Magic for a custom hero is Billy's (his meter and rank, no Instant). Professor X's chair and versions stay the hero's; Cerebro's helmet shows on a custom hero with the row.

### 33.4 Ten bindings and the router

* **5 columns × 2**: `keys` (R G V B N) and `sneakKeys`. A hero row with a Sneak variant on plain K puts `<id>#alt` on K's Sneak cell (unless its option is `main`); a Sneak cell may otherwise hold a picked power on its own (not a hero row whose variant is on) or `<module>#<option>`, another **press** option of the generic module on plain K (refused if that option drives a resource a hero row of the kit drives). Each action sits on one cell at most.
* `CustomHeroServer.onKey`: on the way down the binding is the Sneak cell if Sneak is held and the cell is not empty, else the plain one, and it is remembered per column (`HELD`), so a release always reaches what went down. **One cooldown per column**, shared by plain and Sneak, as on every hero.
* **The cast marker** (`CustomState.castSeq` 0 to 255, `castColumn`, `castSneak`; 2 bytes on the wire): set when a key going down changed any cooldown, toggle, live bit, flag or generic switch, so every client knows which binding acted even though strength changes the cooldown lengths.

### 33.5 Strength, damage and the budget

* **The factor** of a hero row at strength *s* is `1 + spread × (s − 5) / 5` (`custom_hero.heroStrengthSpread`, default 0.5: ×0.6 at 1, ×1.5 at 10; strength 5 is the hero). Damage × factor (and × `custom_hero.damageScale`), cooldown ÷ factor, body amounts × factor (knockback resistance and fall reduction capped at 1).
* **Damage** (`custom/HeroDamage`, `LivingIncomingDamageEvent` at LOW): when the attacker (or a projectile's, an illusion's or a drone's owner) is a custom hero, the damage type finds its row in the kit (`TYPES`: chaos beam → `wanda.chaos_beam`, shrapnel → `magneto.shrapnel`, the speed strikes → the strongest speed row, Mjolnir and Stormbreaker → `thor.hammer`, the Thanos stones → the strongest stone, optic → the strongest Cyclops row...) and is multiplied. Two damage types are new: `enhanced:hero_magic` (generic Magic's bolts; tag `magic_damage`, so the Vishanti ward keeps them out) and `enhanced:hero_mind` (tag `mind_power`; no module deals it yet).
* **Cost**: points per strength × strength (a fixed row: × 5); a generic Sneak variant adds `ceil(cost / 2)`; a hero row and its variant are one power. Defaults **budget 90** (was 60) and **maxPowers 10** (was 8); every preset costs at most 60.

### 33.6 Resources and the rules

* **`Resource`** (14): `GRIP`, `SHIELD`, `BEAM_FLAG`, `CONT_BEAM`, `FORM`, `SPEED_RUN`, `SLOW_TIME`, `PHASE`, `WALL`, `SCALE`, `WEAPON`, `HEX`, `DRONES`, `IRON_CHARGE` (L4: Magneto's shrapnel charge is Ultron's plasma pool). The owner of a resource is a hero (any of his rows) or "the generic modules"; the rules keep one owner per resource and drop later picks that would break it. `billy.freeze` drives `SHIELD` only with its variant on. No hero row drives `SCALE` any more (Loki's Frost Giant lost it), so the generic Grow and Shrink clash with nothing today.
* **`CustomHeroRules.sanitize`** (pure; the editor runs it with the synced limits) in order: the name and colours; unknown ids, `disabledPowers` (an id, `<hero>.*`), `heroModules` off (all hero rows), `worldPowers` off (`wanda.hex`, `thor.bifrost`, `thanos.time`, `ultron.upload`, `ultron.army`, `xavier.take_over`), repeats; strengths (fixed rows 5), options (`both` / `main` for a hero row), stances; `requires`; resources; `maxPowers`, then the budget, dropping from the end; the bindings; moves and animation choices (unknown → `auto`); layers (`layers` off or unknown style → none); the skin and the look. Notes: `rule.resource`, `rule.requires`, `rule.world`, `rule.hero_modules`, `rule.layer`, besides those of 3.3.

### 33.7 Animation

* **Gestures.** On a cast-marker edge the binding's gesture plays for everyone: the binding's chosen move, else, for a hero row, the hero's own (`client/custom/HeroAbilityAnims`: main and alt gestures of all 81 rows, the gestures and stances each may start, the owner row of every shared `Live`, `Toggle` and flag bit), else for a generic module the default of its option (Energy Blast's bolts `BILLY_BOLT`, Earth's tremor `ULTRON_SLAM`...), or nothing for `none`. The routes of the source heroes' toggles, flags and live bits are followed through the owner row in the kit and filtered by the kit, and a borrowed gesture's flourish runs with it (`Gestures.owner`: Thor's sparks, the Phoenix's motes, Thanos's ring). The editor's list of moves (`AnimCatalog.GESTURES`) has 32.
* **Stances**: the source heroes' stance targets ANDed with the kit's rows, the picks' chosen stances (`STANCES_HOLD` 15, `STANCES_TOGGLE` 10) while held or on, and the **idle** (`IDLES`: none, calm, ready, regal, heavy, jotun, crackle, menacing) on the ground when nothing else is up.
* **Flight style** (`FLIGHT_STYLES`: plain and the ten fliers' profiles, take-offs and flight effects; automatic: a flier row of the kit in the order Thor, Wanda, Vision, Magneto, Billy, Phoenix, Ultron, Apocalypse, Agatha, else Flight's option hover → Vision, jet → Ultron, wings → Phoenix); **run** (`RUN_CHARACTERS`, 17); **speedster run** (`SPEED_RUNS`: Quicksilver's Time, Quicksilver, Tommy). Flight effects keep their shapes and take the custom hero's colours. The style is animation only: the hero flies with the Flight module.
* **First person**: the sources' first-person rows plus generic rows for every borrowed move and hold stance (`FirstPersonMoves`).
* Everything goes through `HeroAnimator` with the partial tick; reduced motion is respected. **Preview API** for the editor: `HeroAnimator.preview(puppet, motion, pt)`, `previewCast(puppet, column, sneak)`, `previewHold(puppet, entry, on)`, `previewEnd(puppet)`. Debug: `/enhanced_anim become <file>` (client, op or Creative) becomes a hero file.

### 33.8 Looks

Five optional layers inside the definition (no new payload), each a style of `LookCatalog` with a colour where it can be tinted, drawn by `client/custom/CustomLooksLayer` through small static entries of the existing layers (no drawing code copied): **cape** (`none`, `plain` tinted from a grey template into a `DynamicTexture` per colour, and the 14 hero capes; through the vanilla cape layer), **hat** (`witch_hat`, `billy_crown`, `cerebro`, `magneto_helmet`, `wanda_crown`; the last two drawn with their armour textures, no item equipped), **visor** (`cyclops_classic`, `cyclops_apocalypse`, `ruby_quartz`, `quicksilver_goggles`, `domino_mask`, `full_mask`), **eyes** (`glow`, `lightning`, `fire`, `optic`, `machine`, `black` on a generic eye mask, rows 0 to 3) and **aura** (`energy`, `flames`, `sand`, `sparks`, `mist`). Eyes and aura have a `when`: always, while a power is on, while flying. Hat and visor hide while the head slot holds something; Vision's disguise shows the plain skin with no layers. The heroes' ability-lit layers (Thor's eyes, Spider-Man's legs and black eyes, the Phoenix's flames, Apocalypse's aura, Cyclops's lens) are gated by the rows.

### 33.9 The editor

`client/screen/HeroEditorScreen` with six tabs (`client/screen/editor/`: Start, Powers, Keys, Moves, Look, Save), a budget bar ("45 / 90 points"; green, yellow from 75%, red and shaking over the budget) and a live 3D preview (a puppet player drawn with the real partial tick, drag to turn; Stand, Walk, Run, Fly and Try through the preview API). **Start**: 22 presets (`assets/enhanced/hero_presets/<id>.json`, `HeroPresets`): one "like" each of the sixteen heroes and six archetypes (Speedster, Flyer-Blaster, Tank, Mage, Mind Master, Elemental), all within 60 points, and Empty. **Powers**: chips Body, Move, Energy, Elements, Mind, Heroes (faces filter to one hero); a search (`CatalogSearch`) over the EN and HE names, descriptions, ids and hero names, case- and accent-insensitive; family cards with a style switcher; Add, Remove, Try; options or the Sneak switch; the strength slider with the numbers (hero rows: Damage ×, Wait ×, Body ×); the reason a card cannot be added. **Keys**: the 5 × 2 boxes with the player's key names, a pop-up of what fits (each candidate checked by the rules), linked Sneak cells drawn joined, Auto (`KeyLayout.auto`), Try, Empty. **Moves**: flight, run, super run, standing, then each box's move and each held or switched power's stance. **Look**: skin, name and colours, cape, hat, mask, eyes, aura. **Save**: as 3.3, with the rules' notes before becoming. The HUD (`CustomHeroHud`) gained a `⇧` line per Sneak cell, the hero rows' on/off state and the shields' strength.

### 33.10 Choose a Hero gives the hero's items (R8)

`custom/HeroItems`, called by `CustomHeroes.choose` after `Powers.awaken`: what the hero's own awakening leaves with him, or what he carries as his. Magneto: his helmet; Quicksilver: his goggles; Cyclops: the ruby-quartz glasses (each put on the head if it is free, else in the inventory); Thor: Mjolnir claimed by the player (`ModDataComponents.OWNER`), or Stormbreaker in the looks `ragnarok`, `love_and_thunder`, `love_and_thunder_helmet`; Loki: his Scepter; Thanos: the Infinity Gauntlet; Ultron: his Core; Professor X: his wheelchair; Agatha: the Darkhold. The others get nothing: their awakening item is used up. An item the player already has anywhere in the inventory is skipped; a full inventory drops it at the feet. `custom_hero.chooseGivesItems` (true) turns it off.

### 33.11 Format 2 and protocol 8

* **JSON version 2** (`CustomHeroJson`) is a superset of 1: `look` gains `cape`, `hat`, `visor`, `eyes`, `aura` (`style`, `color`, `row`, `when`; written only when not none), a pick gains `stance`, and the root gains `sneakKeys` (5), `moves` (an object of the non-automatic bindings, keys `r` ... `n`, `sneak_r` ... `sneak_n`) and `anim` (`flight`, `run`, `speedRun`, `idle`). A hero row's `option` is `both` or `main`.
* **Migration**: a version-1 text (a file, the clipboard, the string saved with a player) is read with the new fields at their defaults (no Sneak bindings, everything automatic, no layers) and written back as 2. 3.3 reads the core of a 3.4 file and drops what it does not know.
* **Network**: no new payload; `HeroCreatorPayload` APPLY carries the v2 definition, `CustomHero.STREAM_CODEC` the v2 fields in a fixed order (well under the 32 KiB limit with a 24 KiB skin), `CUSTOM_STATE` the cast marker. `ModNetwork.PROTOCOL_VERSION` is **"8"** (`SkeletonTests.theProtocolIsEight`): a 3.3 client cannot join a 3.4 server.

### 33.12 Config and tests

* `[custom_hero]`: `budget` 90, `maxPowers` 10, `disabledPowers` takes `thor.lightning` and `thor.*`; new `heroModules` (true), `worldPowers` (true), `heroStrengthSpread` (0.5, 0 to 1), `layers` (true), `chooseGivesItems` (true). `damageScale` and `cooldownScale` also scale the hero rows. A server file of 3.3 keeps `budget = 60` and `maxPowers = 8`.
* Tests: `CustomV2Tests` (the rows, the kit, the rules, the router, format 2 and the migration), one class per hero lane with every row on a key other than its home (`CustomWitchTests`, `CustomSpeedTests`, `CustomGodsTests`, `CustomTechTests`: Sneak variants through the Sneak cell, bodies only with their rows and scaled, damage at strength 2 against 9, the ends on death and loss of the power, the names in both languages), `CustomAnimTests`, `CustomLooksTests` (textures, presets, search, Auto, R8) and `BarrierMagicTests.aCustomHerosBarrierKeepsTheSameRule`. Checked in a real client with two players (the Thor-like hero's lightning stance, God of Thunder and flight as another player sees them, the speedster's run, a mage's slam and shield, the idle, first person; the editor in English and Hebrew; layers seen by the other player).

## 34. How 3.4 was put together

* **S0, the skeleton** (branch `v34-skel`), before the lanes: the 80 rows as data, `Catalog`, `Resource`, `Trait`/`Traits`, `HeroKit` and the `PowerData` calls, `HeroKeys` and the dispatch statics, the router and the cast marker, `HeroDamage`, `CustomHero` v2 and its JSON with the migration, the rules of 33.6, `AnimCatalog`, `LookCatalog`, the config options, the two damage types, the client stubs, and protocol 8.
* **Six lanes** from the skeleton, each in its own worktree: L1 the witches (Scarlet Witch, Billy, Agatha, Build Magic), L2 speed and metal (Quicksilver, Tommy, Vision, Magneto; `vision.body`, the 81st row), L3 the gods (Thor, Loki, the Phoenix, Thanos), L4 tech and mind (Spider-Man, Ultron, Apocalypse, Cyclops, Professor X; `Resource.IRON_CHARGE`, the generic Wall Crawl's cling flags), L5 animation, L6 looks, the editor and R8. Shared files (both lang files, the config classes, `HeroAbility`'s per-hero regions, `PowerTicker`, `ClientEvents`...) were edited minimally and additively; the integrator merged L1 to L5 and 3.3.1 into `v34` (full GameTests `All 1849 required tests passed`), then L6 merged that into `v34-looks` and became `v34` (`All 1859 required tests passed`).
* **For a new hero** from now on: give each key a row in `HeroAbility` (in a region of its own) and a body row if it has passives; write its `onKey(..., alt)`; ask `has(row)` / `acts(hero)` instead of `power == hero`; cooldowns by row; guard its tick parts; an `endAll`; colours from `Powers.color/glow`; its damage types in `HeroDamage.TYPES`; its roles in `Traits.of`; its shared state in a `Resource`; its gestures and stances in `HeroAbilityAnims`; the names `hero_module.enhanced.<id>` (+ `.alt`, `.desc`) in both languages; and the count in `CustomV2Tests.everyKeyOfEveryHeroIsARowOrAFlightKey`. See `docs/HEROES-AND-BOSSES.md` section 12.

## 35. Asgard and its heroes (added in 3.5)

Eilon asked for Asgard (A1 to A7): a whole dimension in the MCU's style, reached only by the Bifrost; Heimdall and Odin as heroes; Hela as a boss who becomes a hero once beaten; the other gods as people of the city. The player's guide is the wiki: [Asgard](wiki/Asgard.md), [Heimdall](wiki/Heimdall.md), [Odin](wiki/Odin.md), [Hela](wiki/Hela.md), [Asgardians](wiki/Asgardians.md).

### 35.1 The dimension

* `enhanced:asgard` (`data/dimension`, `dimension_type`, `worldgen/biome/asgard.json`): skylight, no ceiling, the Overworld's clock, monsters never spawn by light, beds work (the spawn is refused by an event), respawn anchors do not. Its own sky (`client/asgard/AsgardSkyEffects`: a static buffer of 1,500 stars on the whole sphere, six nebula quads, a ringed planet and two moons; no clouds, no rain), music (`music.asgard`: three original pieces) and ambience.
* **A code generator** (`asgard/AsgardChunkGenerator`, codec `enhanced:asgard`): the land (`AsgardTerrain`, pure, a fixed seed, not the world's: radius 235 with two lobes, the sea to 380 with water to y 62, a stone lip and the cosmic falls at 381, an inverted cone of rock underneath, a chasm west of the palace), then the city (`CityBuild.place`): procedural builders (`asgard/build/*`: palace spires to y 204, terraces, stairways, the Golden Road, waterways, the Rainbow Bridge, the Observatory's rock, trees) and 56 NBT templates placed by `CityPlan` (one fixed plan: 16 levelled pads, about 120 pieces), clipped to the chunk. The city is the same in every world; `CityPlan.VERSION` is kept in `AsgardState`.
* **Fixed points** (`CityPlan`): the arrival in the Observatory, facing north; the Hofund socket and the Bifrost mechanism; the Throne of Asgard (0, 86, −128) and Hela's Seal behind it; the throne room (x −16..16, z −136..−64, floor 82, ceiling 108); the vault below it with Gungnir's stand; Odin's bed; the training ring, Frigga's garden, the Hall of Feasts, the stables, the bridge head.
* **The rules** (`asgard/AsgardRules`): no monster by nature (`FinalizeSpawnEvent`); the city protected (`asgard.protectCity`: `CityPlan.isProtected`, asked by `BlockRights` and by break, place, bucket, fire, explosion, piston, trample, griefing events; operators in Creative may edit); the void catch below `asgard.voidCatchY` (the Bifrost sets the player down in the Observatory, unhurt); the soft edge at `asgard.edgeRadius`; no spawn point in Asgard; death keeps the inventory (`asgard.keepInventory`; off, the Bifrost keys and Hela's Headdress are still kept); a login while Asgard is closed goes to the respawn point.
* **The gate** (`asgard/AsgardGate`, `EntityTravelToDimensionEvent` at HIGHEST): into or out of Asgard only while `Bifrost.isMoving`, or a player of permission level 2; Nether portals cannot be lit there; the Book of the Vishanti is refused before anything is spent.
* **The Bifrost Rune Stones**: an Overworld structure `enhanced:bifrost_site` (`template_site`, spacing 48 / separation 16, plains, meadow, snowy plains, taiga, windswept hills, badlands); the stone opens the Bifrost to Asgard (`Source.STONE`, 60 ticks, no key, the stone's own rest `asgard.runeStoneCooldown`).
* Performance: terrain per column straight into the sections, builders only inside their boxes, templates compiled once and bucketed by chunk; `/enhanced asgard genbench` measures it.
* **The Hall of Feasts** (3.7.2, `asgard/FeastHall`): the template `asgard/feast_hall` (SCRATCH's `v372/art/feast_hall.py`) has tables a block high (top spruce slab boards, stripped spruce legs) with the dishes on them, each long table in two halves with a cross aisle from the middle door to the high table, six benches behind the high table facing the hall, lanterns hung from the ceiling, two back doors to the kitchen. 3.5's hall (boards 1.5 high on fence posts, dishes floating half a block over them, no seat at the high table, lanterns hanging from the air, no way to the kitchen; the long tables unbroken, too long a walk round for a 32-block pathfinding search) is kept as `asgard_legacy/feast_hall_3_5`: `FeastHall.tick` mends a hall built from it once (`AsgardState.feastHallMended`) the first time all its chunks are loaded, setting each block where the two templates differ to the new one where the world still holds the old one (a joining block by its block; a player's change is left alone and counted); `/enhanced asgard mendhall` runs it again. Seats: `FeastHall.seats()` (6 + 112, a bench with its table before it). The feast: dusk is day ticks 11000-13500; Volstagg, Sif, Fandral and Hogun have their places at the high table; `activeHome` is the hall for them while it is on (from far off, unseen, they arrive at the front door); `AsgardianGoals.Dine` walks to the seat (own, or the first free one), sits (state `DINING`, appended to `NpcState`: the riding pose lowered as sitting, a food in hand by role, a bite every 60 ticks) until the feast ends, Volstagg also at other hours; `Seated` (LOOK only) keeps every sitter facing his seat's front (the benches of the city too, which 3.5 sat facing anyhow). `AsgardianNavigation`: the Asgardians' walk never goes over a table or a dish on one (`FeastHall.overTable`).

### 35.2 The Bifrost, reworked

* Everything about the Bifrost left `ThorAbilities` for `bifrost/Bifrost` (Thor keeps one-line delegates). `Bifrost.open(player, Destination, Source)`: destinations `HOME`, `Place`, `LOOK`, `ASGARD`, `OBSERVATORY`, `CAME_FROM`; sources `KEY` (the cooldown `thor.bifrostCooldown`), `NPC`, `STONE`, `RESCUE` (instant), `HEIMDALL_SEND`.
* **Keys** (`bifrost/BifrostKeys`): Mjolnir or Stormbreaker in hand, a custom hero's Thor's Bifrost, any Hofund (`asgard.hofundOpensBifrost`), one's own Gungnir, Heimdall's Bifrost row. The screen has a row of realms (Asgard, outside Asgard; Home, "Home (Midgard)" inside) and Heimdall's Send mode.
* **Group travel** (the user's change during the skeleton step, for every Bifrost): at departure everyone whose feet are inside the circle (`bifrost.groupRadius`, 3.2 = the burnt rune circle; ±3 blocks up and down; nearest 64) goes along: players, creatures, mounts with riders, items, boats, minecarts. Bosses and fight entities, spectators, armour stands, pinned entities, those already travelling and those a gate refuses stay; a sneaking player stays only with `bifrost.sneakToStay`. Each lands at his offset round the traveller, moved ≤ 4 up or down to safe ground, else on the traveller's spot. Only the opener pays the cooldown.
* `AsgardProgress` (a player attachment: visited, met Heimdall, won the Watch, Hela's victor, Gungnir taken, crowned, talked to...; `cameFrom`, the Hofund and Gungnir bonds, the Watch's retry time) and `AsgardState` (saved on the Asgard level: Odin awake, Hela's defeats, the murals, the residents, the seal's rest).

### 35.3 Heimdall, Odin and Hela

* Three `Power` entries after CUSTOM (HEIMDALL, ODIN, HELA), plug-ins (`HeroServer`/`HeroClient`), two looks and "own" each, gestures, stances, first-person moves, HUD lines, sounds, and six Hero Creator rows each (18 rows: 99 in all), R1 to R7 from birth; presets *Heimdall-like*, *Odin-like*, *Hela-like*.
* **Heimdall** (`heimdall/`): becoming him by the **Guardian's Watch** (`GuardiansWatch`: 7 `hela_shadow` wisps at 7 of 24 hiding spots chosen from the player's UUID and his tries, 3 minutes, only the watcher's blow counts, a boss bar and `music.watch`; won: a bound Hofund, `WATCH_WON`; a powerless player holding his own Hofund awakens). Keys: Hofund Slash (9/11/14, Sneak: Lunge 12), All-Seeing Sight (toggle: glow within `sightRange`, far players by payload, other realms on the HUD; Sneak held: Far Sight ×6–×10), Bifrost (no item), Guardian's Ward (hold: dome r 5, −40% damage; Sneak: 3 or 4 Einherjar), Bifrost Strike (14 in r 3; Sneak: Send a friend, who may stay by Sneak or a step out). Body +8 health, +4 armour, +0.4 knockback resistance, no damage from falls under 8. Hofund is bound (OWNER + BOND); a stale copy crumbles.
* **Odin** (`odin/`): becoming him: Hela's victor takes Gungnir from the stand in the vault (opened at her first defeat), then a powerless owner who sits on the throne with it is crowned (60 ticks of light; NPCs bow). Keys: Gungnir Blast 12 (Sneak: Throw 16, returns), Odinforce Shield (the shared shield record, r 3.5; Sneak: Heal 8 within 8), Banish (hostile gone; bosses, fight entities and named creatures take 20; players with PvP and `banishPlayers` go home by the Bifrost; Sneak: Wave 10), Huginn and Muninn (ravens show creatures within 32; Sneak: the Report), Allfather's Command (warriors within 64, or 4 Einherjar outside Asgard; Sneak: Odinsleep toggle, Resistance IV, 5% health a second). Body +12 health, +6 armour, Regeneration I below half health. The vault keeps a reserved pedestal for 3.6.
* **Hela the boss** (`hela/`, `BossKind.HELA`): called by breaking her seal (two clicks within 5 s; refusals: `summonEnabled`, outside Asgard, Creative/Spectator, the seal healing; an operator's forced start needs only an Asgard). Arena: the throne room, radius 34, WALL. Health 600 × (1 + 0.35 × extra fighters), armour 10. Phase 1 *The Goddess* (volleys of 3 or 5 necroswords, blade walls of solid spikes, shadow step, blade melee); phase 2 at 70% *The Army of the Dead* (berserker waves of 4 + 2 per extra fighter, at most 10, a new wave at 2 left; Fenris with his own bar, once; blade rain; the Asgardians are commanded against the army); phase 3 at 35% *The Strength of Asgard* (every 20 s a 4-s channel on the dais: 6% of her health breaks it, stun 3 s at ×1.25 damage, else she heals 10%; the blade storm every 15 s). She catches a thrown Mjolnir from phase 2, once a minute (`MjolnirShatter.shatterCaught`); Stormbreaker passes. Victory: Hela's Headdress and `HELA_VICTOR` to every rewarded fighter (2% of her health, or the last blow), murals back, the army crumbles; the first time, Odin wakes and `Vault.open`. Three-layer music (`client/HelaMusic`), muted by `bossMusic`.
* **Hela the hero**: the Headdress (an `AwakeningItem`, 60 ticks). Keys: Necrosword Volley (3, 5 with the form; Sneak: Blade Rain), Blade Wall (10 long; Sneak: Blade Burst r 4), Goddess of Death (toggle: Strength `formStrength`, Resistance I, blades ×1.3, a hunger point every 10 s; Sneak: Death Dash 8 blocks), Raise the Dead (3, 5 in Asgard, 40 s; Sneak: Fenris 60 s), Death's Grasp (hold ≤ 6 s: projectiles crumble, a thrown Mjolnir is caught and shattered once a minute, Stormbreaker dropped). Body +10 health, +4 armour; in Asgard Regeneration I and damage × `asgardFactor`.

### 35.4 The Asgardians

One entity `enhanced:asgardian` with a `Role` (Heimdall, Odin, Frigga, Sif, Valkyrie, Volstagg, Fandral, Hogun, Einherjar, citizen), a skin variant, states (idle, talking, sparring, sitting, feasting, sleeping, bowing, down, fighting) and goals (posts, patrols, sparring pairs, feasting, sitting, talking in pairs, strolls, going home, defending Asgard). The population keeper (`AsgardPopulation`) keeps every slot of `CityPlan.homes()` filled exactly once (12 Einherjar, 14 citizens). Never hurt by a player or anything he owns (a manners line); other lethal damage leaves them DOWN for 400 ticks; only `#bypasses_invulnerability` kills. Talk (`AsgardDialogue`: nodes by priority with conditions on progress, power and state, up to three answers; `AsgardTalkScreen`, EN and HE), trades (vanilla merchant screen: Sif, Valkyrie, Volstagg), Sif's training duel, Frigga's heal. Warriors fight `#enhanced:hela_army` first, then monsters after a player, then Hela. Config `asgard.npcs`, `asgard.talkDistance`.

### 35.5 Music, the Hero Creator and the fixes of 3.5

* **Music**: Quicksilver's own theme *Silver Hours* (client `[music] quicksilverSong`: `theme` or `sweet_dreams`); Speed's Walkman with *Westview Blur* (while he runs with Super Speed or his Slow Time is on; 2 s of grace, then a 10-s silent tail); the Road's sixth version, `music.road.choice`; **the world's music** (`<world>/serverconfig/enhanced/music/`: `quicksilver.ogg`, `speed.ogg`, `witches_road.ogg`, `witches_road_<part>.ogg`, `music.ogg`; Ogg Vorbis checked by header, `music.maxFileKB`; sent by SHA-256 in 30-KB pieces, `music.chunksPerTick`, cached in `config/enhanced/music_cache/`, on an optional channel of its own (`MUSIC_VERSION` 1) so older clients still join; `/enhanced music reload|list`; client `ignoreServerMusic`). Precedence: the player's own song, then the world's, then the mod's.
* **The Hero Creator**: every Sneak variant of the built-in heroes is a row of its own (33, plus the Scarlet Witch's Build Magic: 133 rows), bound as its parent's Sneak half on any key; a parent picked with its variant keeps its main action. `custom/AbilityColors`: a chosen colour per row (the pick's, shared with its variant), recolouring palettes by hue and saturation, greyscale textures tinted, energy bursts where vanilla particles cannot be tinted; built-in heroes keep their exact colours. The colour picker (`ColorPickerScreen`). The editor asks before discarding unsaved changes.
* **Fixes**: `/enhanced structure repair_school [radius]` (the 76 blocks 3.3.1 changed, through the school's own processors); Loki's body (`loki.bodyHealthBonus` 10, `bodyArmor` 6, `bodyToughness` 2: 30 health); Agatha's drain only on sorcerers (`agatha.drainOnlySorcerers`: Loki, the Scarlet Witch, Billy, Agatha, or a custom hero with any of their rows); the Brand New Day skin redrawn from the film; a hero change moves another hero's worn item from the head into the inventory (`HeroItems.takeOffForeign`).

## 36. How 3.5 was put together

* **S0, the skeleton** (branch `v35-skel`): the three `Power` entries, the 18 rows, every config option, sound event, damage type, lang key, payload and registry entry the lanes share, `bifrost/Bifrost` moved out of Thor with group travel, `AsgardGate`, `AsgardProgress`, `AsgardState`, the stub classes each lane fills, and **protocol "9"** (`SkeletonTests.theProtocolIsNine`).
* **Lanes**: L1 the realm, L2 Heimdall (and the Bifrost after S0), L3 Odin, L4 Hela, L5 the Asgardians, L6 sound and music; beside them a fixes lane, a music lane and the creator lane. Each lane owned its packages and wrote into the shared files only in its own region (`// ===== heimdall (3.5) =====`...). The integrator merged them into `v35` in this order: S0, L2, L3, L6, fixes, music, L5, L1, creator, L4; then the version 3.5.0 (main at 18eba02).
* As before, delete `run/world` **and** `run/config/enhanced-server.toml` before a full GameTest run. Asgard's level in GameTests is made by `AsgardTestSupport.ensureAsgard` (the GameTest server has only the vanilla dimensions). Full run on `v35` at 3.5.0: `All 2003 required tests passed`.

## 37. The Infinity Stones, Doctor Strange, the Hulk and Hydra (added in 3.6)

Eilon asked for the six Infinity Stones (S1 to S6, with his answers of 18/10), Doctor Strange made by the Eye of Agamotto, the Hulk (Bruce Banner and two Hulks) and soldiers in the Hydra outposts. The player's guide is the wiki: [Infinity Stones](wiki/Infinity-Stones.md), [Vormir](wiki/Vormir.md), [Doctor Strange](wiki/Doctor-Strange.md), [Hulk](wiki/Hulk.md), [Hydra Soldiers](wiki/Hydra-Soldiers.md), [Structures](wiki/Structures.md).

### 37.1 The stones and who may hold them

* **Six items** (`item/InfinityStoneItem`, keyed by `thanos/StoneKind`): `space_stone`, `mind_stone`, `reality_stone`, `power_stone`, `time_stone`, `soul_stone`; epic, one per stack, fire-, lava-, explosion- and cactus-proof, never despawn; tag `#enhanced:infinity_stones`.
* **The rules** (`stones/StoneRules`, pure and player forms): `isBearer` = Thor, Loki, Heimdall, Odin, Hela, the Scarlet Witch, the Phoenix, or a custom hero with the trait `STONE_BEARER` (the rows `wanda.hex`, `loki.frost_giant`, `thor.body`, `phoenix.body`, `heimdall.body`, `odin.body`, `hela.body`; `stones.customBearers`). `mayHold` = Creative, a Bearer, a `SORCERER` for the Time Stone (`sorcerersHoldTime`), Vision for the Mind Stone (`visionHoldsMind`). `mayUse` = `mayHold`, and the Time Stone only for a sorcerer (Thor holds it quietly). `mayWield` (a filled Gauntlet) = a Bearer, the hero Thanos, or the Hulk as a Hulk (`hulk.wieldsGauntlet`). The Hex Runes silence a sorcerer's Time Stone and Eye.
* **The touch** (`stones/StoneTouch`): a stone on the ground refuses a non-holder's pickup (a hop of 0.3, a spark, `stone_slip`, once a second); one in his inventory runs a grace per player and per kind (1 unit a tick, `handGraceFactor` 3 in a hand; bundles are looked into; the Gauntlet, the Orb, shulker boxes are safe) and at `graceSeconds` × 20 the rejection: `stone_reject` damage min(`rejectDamage`, health − 1), knockback 0.4, the stone thrown 1.5 blocks out with a pickup delay of 40.
* **One entry for every use** (`stones/StoneUse.use(player, kind, source, alt, down)`; sources `BARE`, `GAUNTLET`, `VESSEL`, `ROW`): who may use from there, restraints (Thanos's held hand, the Time Lock, Mjolnir's pin), a switch that is on always turns off, one held use at a time (`Hold`: the Mind Beam, the Purple Beam, the Soul Drain), the stone's cooldown (`StoneCooldowns`: per player and kind, mirrored on the stone item's `ItemCooldowns` for the HUD, and on the Eye of Agamotto for the Time Stone), hunger. Vessels: Loki's Scepter (`ScepterRituals.MIND_BEAM`: Sneak + hold, the other hand empty; anyone), the Tesseract (`REALMS`: Sneak + use; anyone), the Eye of Agamotto (sorcerers). Reality, Power and Soul have no vessel.
* **The powers** (numbers: `[reality_stone]`, `[mind_stone]`, `[time_stone]`, `[power_stone]`, `[space_stone]`, `[soul_stone]`): Reality (`RealityBubbles`: bubbles of projectiles within 4 while in hand, not `#reality_proof`, one per `bossRate` ticks in an arena; Transmute 3 s; Illusion 6 s with a `LokiIllusionEntity` of mode REALITY); Mind (`MindStonePowers`: Vision's beam on `ContinuousBeam`, 4 every 10 ticks, confusion at most once per 40 ticks, 6 s; the Mind Command 20 s, a commanded mob an ally by `COMMANDED` and the `AreaTargets` ally rule, a Hydra soldier `HydraLoyalty.turn`; a player only muddled); Time (`TimeStonePowers`, `StoneZones`: Slow Time a zone of 12 at factor 3 (bosses half), `Toggle.TIME_SLOW` + `Live.TIME_SLOWING`; the Rewind = `TimeRewind` with 5 s; crop, sapling and baby-animal magic); Power (`PowerBeam`: the Purple Beam 4 every 10 ticks, a burst every 10 at its end through `StonePowers.splash`, overheat at 60 ticks; craters only with `terrainDamage`, mobGriefing and `BlockRights.mayBlast`; the Power Bolt = Thanos's blast, 12 + 6); Space (`SpaceRealms`: the Portal Step ≤ 64; the Realm Picker, `RealmScreenPayload` with reasons, the Overworld's respawn point, the Nether at x/8 z/8 on safe ground, the End at (`endArrivalX`, top, 0), Asgard's Observatory through `SpaceStone.moving`, the one exception of `AsgardGate` (`reachesAsgard`); Resistance V 2 s); Soul (`SoulDrain` with 6 a second, half back; Soul Sight 8 s).
* **The HUD** (`client/StoneHud`): a gem per carried stone right of the hotbar with its sweep, lines and heat bars fed by `StoneHudFeed`, the grace countdown, the Snap's state.
* The Mad Titan (`ThanosWants`) prefers a fighter who carries a real stone from his second stone on ("You carry something that is mine.").

### 37.2 The Gauntlet, the Snap and the ledger

* **One item, two states**: the component `enhanced:gauntlet` = `GauntletContents(stones, titan, selected)`, item default `TITAN` (stones 0b11111, titan true): every old Gauntlet and the boss's reward is **Thanos's Gauntlet**, unchanged (the awakening item). The crafted one (`GauntletRecipe`, type `enhanced:gauntlet_shaped`, only while `gauntlet.craftable`; GNG/GCG/GGG) has empty sockets: in by a click in the inventory (bundle-style), out by a right-click with an empty cursor (only for one who may hold that kind); 63 socket models; `GauntletLayer` draws it on the arm with its real sockets.
* **The Stone Wheel** (`KeyBindings.STONE_WHEEL`, J, its own conflict context so it shares J with Web Assist): a tap under 6 ticks = the next stone (`StoneActionPayload` SELECT), a hold = `StoneWheelScreen`.
* **The Snap** (`stones/Snap`): all six, a wielder, Sneak + hold use `snapHoldTicks` (a tap under 10 ticks is the selected stone's variant); refused in a boss arena, on the Road and in Asgard (`snapInAsgard`); `MONSTER` mobs within `snapRadius`, not bosses, minions, named or tamed; exactly ⌊n × `snapFraction`⌋ by id order, turned to ash (`snap_dust`, no drops); the cost: Thanos and Creative none, the Hulk `HulkHooks.onSnap` (the arm), others `snapCostHealth` + Weakness II 30 s; `snapReadyAt` = now + `snapCooldownTicks`; advancement `enhanced:stones/snap` "Half of Everything".
* **The ledger** (`stones/StoneLedger`): the four site stones are counted by `stones.uniqueness` (`PLAYER` a bit per kind in the attachment `STONES`, `WORLD` the SavedData `stone_ledger` on the Overworld, `UNLIMITED`), the Soul Stone by `soul_stone.onePer`; the taker is stamped on the stack (`custom_data.enhanced_stone_taker`, and per kind inside a Gauntlet); an Eye crafted round a site's Time Stone carries its taker (`carryTaker`). A destroyed stack (an item entity killed or fallen 64 below the world, a sacrificed player's things, nested to depth 3) frees its kind (`returnLost`). Mind and Space are not counted.
* **Recipes** (data): `mind_stone` / `space_stone` (type `enhanced:stone_extraction`: the vessel alone gives the stone and the empty vessel), `lokis_scepter_from_stone`, `tesseract_from_stone`, `eye_of_agamotto` (8 gold blocks round the Time Stone), `time_stone_from_eye`, `infinity_gauntlet`, `sling_ring`.

### 37.3 The four sites

Structure types of our own: `tower_site` (parts stacked 32 apart), `carved_site` (carved into an End island from its edge), `nether_site` (a cavern floor with headroom 12 and side room 12; never turned); the 3.3 `template_site` got `enabled_by` and `ground` (`median` or `highest`). `world/SealProcessor` walls the world's lava off the site's air. Switches: `[stone_sites]`.

| Site | Id | Size | Where | Set (spacing / separation) | Holds |
|---|---|---|---|---|---|
| Sanctum Sanctorum | `enhanced:sanctum` | 23 × 26 × 19 | plains, sunflower plains, meadow, forests, cherry grove, taiga | 48 / 16, 6 chunks from Hydra outposts | the Time Stone's pedestal on the top floor |
| Avengers Tower | `enhanced:avengers_tower` | 25 × 92 × 25 (three parts) | plains, sunflower plains, forests, meadow | 64 / 24, 8 chunks from Sanctums | the Tesseract's cradle on the roof |
| End tunnel | `enhanced:end_tunnel` | 15 × 14 × 46 | End highlands and midlands, 1,000+ from the centre | 28 / 10 | the vault chest with the Orb (`enhanced:site_stone` loot function) |
| Vormir Cliff | `enhanced:vormir_cliff` | 33 × 74 × 33 | soul sand valley, basalt deltas | 20 / 8 | the edge, the pool, the Stonekeeper |

The Reality Stone's pedestal is set in Odin's vault at `CityPlan.REALITY_PEDESTAL` when the vault opens (and at server start while Odin is awake), so an Asgard of 3.5 gets it too; Hogun warns of the Aether. `block/StonePedestalBlock` (property `stone`, unbreakable, no loot; a player-mode pedestal keeps its stone for the next player), `block/TesseractCradleBlock` (`full`; one Tesseract, then empty for good). `world/SiteAmbience` tells a player inside a Sanctum or an End tunnel which loop to play (`StoneFxPayload.SITE_AMBIENCE`); the client fades it in and out.

3.7.1: every `TemplateSitePiece` places with `world/HangingEntityProcessor` (`enhanced:hanging_entities`, NeoForge's `processEntity`), which writes an item frame's or painting's block (`TileX`/`TileY`/`TileZ`, the block of its placed position) into it: our templates had none, so each frame was read as hung at 0, 0, 0 and logged "Block-attached entity at invalid position" (13 for the tower, 6 for the Sanctum) before being moved into place. `StoneSiteTests.theTowersItemFramesHangWhereTheyArePut`.

### 37.4 Vormir: the sacrifice

* `vormir/VormirTracker.credit(victim, pusher)` is called by every shared push (`StonePowers.shove`, `Telekinesis` release and throw, the Hulk's pushes): the creditor is the player, a pet's or a summon's owner, a projectile's owner; a non-player pusher takes the credit away (the last push decides). Per tick, who stands on the cliff's `EDGE_BOX` and when he leaves it, with the credit he carried if it is at most `pushWindowTicks` old. Hits from `LivingIncomingDamageEvent` (so with PvP off a player's hit never counts), blasts from `ExplosionEvent.Detonate`.
* `Sacrifice.judge` at `LivingDeathEvent` (LOWEST, uncancelled: a totem never gets there): a fall into `PIT_BOX` within `deathWindowTicks` of the departure and at least `minFall`, a credited online player who is not the victim; then the rules (enabled, Creative, `mayHold(SOUL)`, players: `playerSacrifice`, PvP, `friendRule`; pets: his own `TamableAnimal`/`AbstractHorse`, `petSacrifice`; the ledger). Each outcome but "no fall"/"no push" has a Stonekeeper line.
* The gift: a player victim's every stack through `StoneLedger.destroyed` then cleared (also under keepInventory; never the ender chest), experience 0, `removePower`, the flag `WAS_SACRIFICED` and two lines at respawn; a pet leaves an orange wisp. The seeker is set in the pool with the stamped Soul Stone, the keeper's "A soul for a soul.", the title and `StoneFxPayload.SOUL_GIVEN` (an orange glow), advancement `enhanced:vormir/a_soul_for_a_soul`.
* `StonekeeperEntity`: invulnerable but to `/kill` and the void, never moved, put back at his home + 0.3 every tick, three lines on a right-click; `VormirKeeper` brings him back when a player is on the summit. Config `[soul_stone]`.

### 37.5 Doctor Strange

`Power.DOCTOR_STRANGE` (colour 0xF29A2E, glow 0xFFE0A8), trait `SORCERER`, flight on G at `flightSpeed` 0.06 (flight profile STRANGE, the Cloak), plug-in `strange/StrangeServer` → `StrangeAbilities`. Keys R Eldritch Whip (7/7/10, a 25° cone of 8; Sneak: Whip Grapple ≤ 24), V Shield of the Seraphim (its own state `Live.STRANGE_SHIELD`: a disc that stops projectiles and ends beams, −60% from the front, ≤ 10 s; Sneak: Seraphim Dome r 4, 6 s), B Crimson Bands (SPELLBOUND 4 s, bosses 1 s; Sneak: Astral Projection with an `AstralBodyEntity`, phasing, 30 s, 64 blocks), N Sling-Ring Portal (`SlingPortalEntity` pairs, 8 s, never across worlds, a chunk ticket `ENHANCED_SLING`; Sneak: Mirror Dimension r 14, 20 s: `MirrorDimension.protects` asked by `BlockRights`, those he hurt held in, +20%), **Y** the Eye (`HeroKeys.Y` = 6; `EyeTime` through `StoneUse(TIME, VESSEL)`). Body +6 health, +2 armour, falls under 12 caught, spells ×0.8 hunger. **The Eye** (`EyeOfAgamottoItem`, `strange/EyeOfAgamotto`): a powerless holder for `awakenTicks` becomes Strange and owns the Eye (`OWNER`; `ownerOnly`); a sorcerer uses the Time Stone through it (and its crop magic); others "closed". **The Sling Ring** (`SlingRingItem`, `PlaceBook.Kind.SLING_RING`, attachment `SLING_RING`): 8 places, portal pairs to them across worlds (never Asgard or the Road), cooldown 200. Looks `sorcerer_supreme`, `multiverse`, `defender`, own; `CloakLayer`, `EyeAmuletLayer`, `AstralLayer`, mandala effects; config `[doctor_strange]`, `[eye_of_agamotto]`, `[sling_ring]`.

### 37.6 The Hulk

`Power.HULK` (0x5BA33A / 0xB6F27A), no flight, no role. Looks `ragnarok` (scale +0.5), `brand_new_day` (+0.75, damage × `bndDamageFactor` 1.15), `banner` and `own` (human: `HulkForms.isHuman`, every key but N refused). Every change goes through `HulkForms.change` (40 ticks Banner ↔ Hulk, 20 Hulk ↔ Hulk; room to grow checked every tick). Body (`HulkAbilities.applyBody`): +20 health, +8 armour, +4 toughness, knockback resistance 0.9, attack +7, reach +1.5/+2, jump +0.5, no fall damage, step +0.6, speed +15%, break speed ×4, healing 0.35/s after 5 s unhurt, hunger ×1.5. `HulkRage`: rage (+4 a heart taken, +2 a blow, +20 the Roar, −2/s after 10 s; ≥ 50 enraged, effects refused; up to +30% damage) and Banner's anger (+10 a heart, +30 under 4 health; at 100 he Hulks out unless he carries a Gamma Inhibitor; the HUD's heart rate 70–200). Keys: Hulk Smash (7 to the grounded within 6; Sneak: Grab Smash 4+4+6, a boss one punch of 8), Thunder Clap (6, a cone of 14 × 50°, projectiles turned, fire out; Sneak: Roar, `FRIGHTENED`), Super Leap (≤ 48, 6 in r 4; Sneak: Bound 24), Boulder Throw (`HulkBoulderEntity`, 8 + 4; Sneak: Gamma Charge 12 blocks, 6), Transform (Sneak: Switch Hulk); a custom hero's `hulk.transform` is the Human Form (`Toggle.HUMAN_FORM`). Blocks: `HulkTerrain` (`hulk.blockBreaking` `NONE`/`SOFT`/`ALL`, tags `hulk_soft`, `hulk_smashable`, `hulk_unbreakable`, mobGriefing and `BlockRights`). **The Gamma Emitter** (`block/GammaEmitterBlock`, recipe type `enhanced:gamma_emitter`, only while `gamma.emitterCraftable`): warm-up 60, a beam of 6 for 60 ticks, rest 200; a powerless player in the whole beam becomes the Hulk (Banner, then the first change at once); a Hulk is healed 2/s. The Snap burns his arm for `armBurnedSeconds`. Client: his own model (`HulkModel`, 128 × 128 skins) unless `[hulk] hulkModel` is off. Config `[hulk]`, `[gamma]`.

### 37.7 The Hydra soldiers

`hydra_soldier` (ranks RIFLEMAN 24/6, ENFORCER 30/10, SNIPER 20/4) and `hydra_officer` (36/8), `#enhanced:hydra`, MONSTER. `HydraGarrison` fills each outpost's posts (9 by `garrisonSize`: 2 gate guards, the tower sniper, a yard patrol of 2, a perimeter patrol of 2, the cellar enforcer, the officer) while a player is within `activateRadius`, from `HydraOutposts` (SavedData per dimension, keyed by the structure start), for the outposts of 3.3 too; the outpost's monster spawns are now empty (no pillagers). `HydraSenses` (a 120° cone of `sightRange`, sneaking × `sneakFactor`, night × 0.75, hearing within 8; never Creative, Spectator, invisible, Loki's illusions, Strange's spirit). `HydraBoltEntity` (a `ThrowableProjectile`; the Reality Stone bubbles it). `HydraAlarm`: the officer at once, a soldier after 60 ticks of fighting if the officer lives and he reaches a console (or radios him); the siren, the lamps, `reinforcements` at the cellar (one fewer on Easy), at most `maxAlarmsPerDay`; a console held 3 s switches it off for the day. Respawn `NEVER`/`DAYS`/`ALWAYS` (`respawnDays` while nobody is within 64). `HydraLoyalty`: Loki's Scepter (`scepterTurns`, `scepterTurnTicks`, an officer half), the Mind Stone's Command, Xavier and the Phoenix make a traitor; turned soldiers never attack players. Drops (`HydraDrops`, a player's kill, never reinforcements): energy cells, the spyglass, the **Hydra Blaster** (96 uses, a cell repairs it; `blasterEnabled`), **Hydra Intel** (a map to `#on_avengers_tower_maps`). Config `[hydra]`.

### 37.8 The Hero Creator and the fixes of 3.6

* **Rows**: 154 `HeroAbility` rows (133 + Doctor Strange's 6 and 5 Sneak-variant rows + the Hulk's 6 and 4; Switch Hulk is the built-in Hulk's only) and 6 `custom/StoneRow`s (`stone.reality`... `stone.soul`), a catalogue kind of their own (`CatalogEntry.stone()`): a stone row's key goes to `StoneRowKeys.press` → `StoneUse.use(ROW)` and works only while `StoneUse.carried` (the stone, a wieldable Gauntlet, or its vessel); `custom_hero.stoneRows`. `CustomHeroRules`: at most one Y row (Build Magic or the Eye); the Human Form refuses every other key. Resource `SOUL_CHAIN` (Thanos's Soul Stone and the Soul Stone row). Presets *Doctor Strange-like*, *Hulk-like*; flight style "cloak", runs "doctor_strange" and "hulk".
* **The third-person back view** (`client/render/OwnBeams`): with the camera behind its shooter, a beam lies along the line of sight and ends at the screen's middle, behind his own head, and the beam render types test depth, so his body hid it (Thor's Lightning Beam too). His own beams are now drawn after the others into `EnhancedRenderTypes.ENERGY_BEAM_OVER` (no depth test, switched off and back by a layering shard, since vanilla's `NO_DEPTH_TEST` does not disable the test at AFTER_PARTICLES): `BeamRenderer.renderOwn`, `WorldEffectsRenderer.heroBeams`, `ThorEffects`.
* **The stone beam's arm**: a stone beam is held through an item in use, which `HeroPoses.yields` left to vanilla; now a stone beam (`HeroAnimator.stoneBeam`) keeps the arm and the BEAMING stance raises the stone's own arm, even for a player with no power (the Scepter's beam).
* **The Hulk's camera**: while Banner grows, the third-person camera keeps the full-grown distance.

## 38. How 3.6 was put together

* **S0, the skeleton** (branch `v36-skel`, from 3.5.0 with 3.5.1 merged in): `Power.DOCTOR_STRANGE` and `HULK`, every row (154) and stone row (6), every config option, the 83 sound events, damage types, tags, lang keys, payloads (`StoneActionPayload`, `RealmScreenPayload`, `StoneFxPayload`, `StrangeFxPayload`, `HulkFxPayload`, `HydraFxPayload`, `SlingRingActionPayload`), registries, the stubs each lane fills, the Y key, and **protocol "10"** (`StoneSkeletonTests.theBitsFitAndTheProtocolIsTen`).
* **Lanes**: L1 the stones' core (items, ledger, pedestals, Gauntlet, Snap, wheel), L2 the stones' powers, L3 Doctor Strange, L4 the four sites, L5 Vormir, L6 sound and art, L7 the Hulk, L8 the Hydra soldiers. Each owned its packages and wrote into the shared files only in its own region (`// ===== stones|strange|stone_sites|vormir|hulk|hydra (3.6) =====`). The integrator merged them into `v36` in this order: S0, L1, L4, L2, L5, L8, L7, L6, then L3; then the cross-lane joins (the Soul Stone stamped with its taker, the Mind Stone's Command turning a Hydra soldier, the site ambience as loops, the Eye carrying the Time Stone's taker), the back-view beam fix, and the version 3.6.0.
* As before, delete `run/world` **and** `run/config/enhanced-server.toml` before a full GameTest run. Full run on `v36` at 3.6.0 (1e4c5ec), twice: `All 2221 required tests passed`.

## 39. The healing factor (added in 3.7)

Eilon asked for a Wolverine "with infinite healing" whom only the Infinity Stones, poison and top-level magic (Odin's, chaos magic) defeat; Deadpool shares it. One mechanic in `healing/` (lane L1) for both heroes and any custom hero with their body rows. The player's guide is [Healing Factor](wiki/Healing-Factor.md).

* **Who**: `HealingFactor.has(player)` = `Traits.has(HEALING_FACTOR)` (from `Traits.of(WOLVERINE|DEADPOOL)` or the rows `wolverine.body`, `deadpool.body` while `healing_factor.customHeroes`) and `[healing_factor] enabled`. `active(player)` = `has` and not suppressed. Never a list of heroes.
* **Regeneration** (`Regeneration`, every 10 game ticks: rate × 0.5): `regenPerSecond` 1.0, `regenOutOfCombat` 3.0 after `outOfCombatTicks` 60 without damage (`HealingState.lastHarm`, set at `LivingDamageEvent.Post`), × `wolverine.berserkRegenFactor` while `Live.WOLVERINE_BERSERK`, × 0.25 at food 0; exhaustion `exhaustionPerHealth` 0.05 per health. 0 while DOWN or suppressed. Vanilla food regeneration is untouched.
* **The shrug** (`Suppression.shrugs`): on `MobEffectEvent.Added`, a vanilla (`minecraft:` namespace) HARMFUL effect not in `#enhanced:stops_healing` gets its duration × `shrugFactor` 0.5 (the instance's duration field by reflection; infinite effects untouched). The mod's own effects (Spellbound, Drained, Frightened, Confused...) last their full time.
* **The rule** (`HealingFactor.defeat`, pure form tested as a truth table): a non-healer or a suppressed healer → `ALWAYS`/`POISON`; the void or `/kill` (`#minecraft:bypasses_invulnerability` **minus the mod's own types**, which the mod lists there: `hex_wall`, `mind_stone`) → `ALWAYS`; then the groups by tag, each switched by its option: `#enhanced:defeats_healing/stones` (`stone_power`, `stone_blast`, `stone_soul`, `mind_stone`, `mind_stone_beam`, `power_stone_beam`, `power_stone_explosion`, `snap_dust`; `stonesDefeat`), `/chaos` (`chaos_beam`, `hex_wall`, `hex_bolt`, `hex_beam`; `chaosDefeats`), `/odin` (`odinforce`, `gungnir`; `odinDefeats`), `/phoenix` (`phoenix_burst`; `phoenixDefeats`), `/darkhold` (`darkhold_beam`, `agatha_drain`, `agatha_runes`; `darkholdDefeats`, **false**), and the Vormir sacrifice (`isSacrificeFall`: a credited departure from the edge by another, within `soul_stone.deathWindowTicks`, a FALL, FLY_INTO_WALL or `vormir_fall` source; `vormirDefeats`). Anything else (mobs, swords, falls, lava, Thor, the Hulk, Hela, Hydra, claws and katanas) → null: DOWN. `Defeat.messageKey()` = `message.enhanced.healing.defeated.<group>`.
* **The cut** (`LivingDamageEvent.Pre` LOWEST, so armour, enchantments and absorption have counted): when the loss would reach the health and `defeat` is null, the new damage = absorption + (health − 1), a CUT ring (`HealingFxPayload` CUT), and `Downed.down`. A totem is never consumed (the death never happens). `LivingDeathEvent` HIGHEST is the net for code that sets health to 0 without damage (cancelled, health 1, DOWN), and remembers the defeat group; `LivingDeathEvent` LOWEST (`receiveCanceled`) sends the gold line `defeated.<group>` to everyone within 64 before vanilla's death message.
* **DOWN** (`Downed`, `Live.HEALING_DOWN`, synced to everyone): `downUntil` = now + `downTicks` 100; transient modifiers `enhanced:healing_down` −100% movement speed and jump; `PowerTicker.stopHoldAbilities`, item use stopped, elytra and flight stopped; monsters within 64 forget him (target, brain `ATTACK_TARGET`, NeutralMob anger, last hurt-by) and again every 10 ticks, and `LivingChangeTargetEvent` HIGH refuses a new target (`downMobsForget`); `LivingIncomingDamageEvent` HIGHEST cancels every blow but a defeat (stepping aside for `RoadTrials.isInside`: the Road's own rule runs first); his keys (`AbilityHandler.onKey` gate), right/left clicks, entity interactions, attacks, item use and block breaking are refused on both sides. The tip `message.enhanced.healing.tip` (built from the current options with `.tip.<group>`, `.or`, `.comma`; `.none` when nothing but the void is left) once (`TIP_SHOWN`; every DOWN with `tipOnce` off). **Rise** at `downUntil` (or when the factor is gone): rescue if in a hazard, health max(health, `riseHealth` 8), Resistance I `riseResistanceTicks` 60, FX RISE, the action bar, the advancement `enhanced:mutants/healing_factor` ("Back on Your Feet"), `HealingHooks.rise(player, rescued)` (Wolverine growls with his claws out; Deadpool a line). Logged in while DOWN: risen at once, silently.
* **Rescue** (`Rescue`): in a hazard (eyes in a drownable fluid without water breathing, lava, powder snow, inside a wall, fire at the feet, below min build height + 1) the rise moves him to the nearest standable spot within `rescueRadius` 32 (rings outward, per column the height nearest his, loaded chunks only; standable = a sturdy safe floor and two clear blocks, no fluid, fire, powder snow, web, berry bush, wither rose or portal), else his respawn point; inside a boss arena only spots in the arena and no respawn fallback. Fire out, freeze 0, air full, FX RESCUE.
* **Poison** (`Suppression`): any effect in `#enhanced:stops_healing` (Poison) while `poisonStopsHealing` switches the factor off: no regeneration, shrug, Burst or protection. Edges per tick: FX OFF/ON, the action bar ("The poison stops your healing!" / "Healing factor: back ON!"), sounds `healing_off`/`healing_on`.
* **The Healing Burst** (`HealingBurst.burst(player, fraction)`; `fraction()` = `burstFraction` 0.5, `cooldown()` = `burstCooldown` 400): false if dead, DOWN or a poisoned healer (no cooldown spent); else heal fraction × max, remove the shrugged kinds of effects, FX BURST. A player without the factor is healed plainly (the two Burst rows on a custom hero).
* **Client** (`client/healing/`): `HealingClient` (the seven FX drawers; late viewers play HEALING_DOWN and hold it; glints every 3 ticks while down and every 8 while hurt; the local view tipped 16° in first person while down, hands hidden, not with `reducedMotion`), `HealingHud` (its own GUI layer under the powers' panel for every healer: icon and "Healing factor: ON (+1.0/s)", the red crossed "OFF — poisoned!", "Healing… 3", darkened edges and a 48-dot ring over the crosshair; `HealingHud.bottom(data)` for the panels under it), `HealingGestures` (HEALING_DOWN 44 ticks: kneel, lie on the side where the client holds it, push up; HEALING_RISE; HEALING_BURST, arms up in a V).
* **State**: attachment `HEALING` = `HealingState(downUntil, lastHarm, flags TIP_SHOWN|WAS_DOWN_IN_HAZARD)`, saved, not kept on death, synced. `HealingHooks.register(Listener)` for the heroes' lines and growls.
* **With the rest**: the Road trial's HIGHEST rule wins; Ultron's Second Life (HIGH, death) is only reached by a defeat; a DOWN healer counts as alive in a boss fight; the Vormir rule above; Agatha's Drain refuses him (not a sorcerer); the Mutant Cure removes the power and with it the trait; a healer whose death is cancelled never meets Death (44). Config `[healing_factor]`.

## 40. Wolverine and Deadpool (added in 3.7)

Lanes L2 and L3. The player's guide: [Wolverine](wiki/Wolverine.md), [Deadpool](wiki/Deadpool.md).

### 40.1 Wolverine

* `Power.WOLVERINE` (after HULK; colour 0xF2C230, glow 0xD9DEE3), no flight (`flightAbility` −1), traits `HEALING_FACTOR`, `METAL_BODY` (Magneto's telekinesis and Magnetic Pull take him through `Metal.isMetalHero`: no Magneto code changed). Not a telepath, not a sorcerer. Plug-in `wolverine/WolverineServer` → `WolverineAbilities` (R1-R7 from birth: `data.has` per row, cooldowns by row, `alt` explicit); client `client/render/WolverineClient`.
* **Body** (`× bodyScale`): +10 health, +4 armour, +2 toughness, knockback resistance 0.4, +20% speed, +2 attack, +0.6 attack speed, +0.15 jump, +4 safe fall, swim speed −50% while `heavy`. **Adamantium** (`Adamantium.sink`, both sides from `PlayerTickEvent.Pre`): in water or lava (not in a boat, flying or a bubble column) sprint-swim off and motion.y = min(y, `maxSwimUp` 0.02) − `sinkSpeed` 0.03.
* **Keys**: R `ClawSlash` (combo 6/6/9 `wolverine_claws` on the nearest creature in a 60° cone within 3.5, in sight; the third hits up to 3 and pushes 0.8; the gap `slashGap` 10, 6 in the Rage, kept in the hero's state, not a cooldown; reset after `comboReset` 30) / Sneak: Spin Slash (7 to everyone within 3 `AreaTargets.mayHurt`, push 0.6, arrows knocked down; 80). G `Claws` (`Toggle.CLAWS_OUT`; out for R, the pounce, the Rage and 2 s after the injection; the empty main hand's blow + `clawBonus` 4 × (0.2 + 0.8 s²) at `LivingIncomingDamageEvent` LOW, a full ground blow sweeping creatures within 1 of the struck one for 2) / Sneak: `ClawClimb` (`Toggle.CLAW_CLIMB`, up `climbSpeed` 0.12/tick against a wall not in `#enhanced:claw_slippery`; Sneak holds still; no fall damage while clinging). V `Pounce` (prey by `Raycasts.entity` within 16; crouch 4 ticks `Live.WOLVERINE_POUNCING`, a 12-tick flight that follows the prey, stab 12 `wolverine_pounce`, pin 20 ticks with Slowness IV, a boss damage only, set down beside it; no prey: a plain leap; 100) / Sneak: Feral Leap (10 ahead, 2.2 high, 14 ticks; 30). B `Berserker` (`WolverineState.berserkUntil` synced, 200 ticks: attack × 1.4, +25% speed, +0.5 knockback resistance, claws forced out, items refused at `LivingEntityUseItemEvent.Start` and `RightClickItem`, a growl every 3 s, then Weakness I 100; 900) / Sneak: the Healing Burst (B's slot is shared for the hero himself). N `Senses` (sniff: every living creature within 48, ≤ 64 nearest, in `ScentPayload` with its kind MONSTER, ANIMAL, VILLAGER, PLAYER, POISONED for 200 ticks, glowing through walls via `EntityGlow`; trails of players and monsters sampled every second within 64 of a Wolverine, `trailSeconds` 60, ≤ 8 × 16 points; 300) / Sneak: Track (`Toggle.SCENT_TRACK`, the creature looked at within 64 or the last attacker within 60 s; 1200 ticks; a trail every 20 ticks; the tracked player told; ends on death, dimension, beyond `trackRange` 256, time, or again; 300 after).
* **The Adamantium Injection** (`item/AdamantiumInjectionItem`): hold `holdTicks` 60 (SPEAR pose) → `Powers.awaken(WOLVERINE)`, silver flash (`WolverineFxPayload` SNIKT arg 1), claws out 2 s; used up but in Creative. A hero refused while `heroesRefused` (Deadpool his own line), nothing used. Only on yourself. Recipe `enhanced:adamantium_injection` (`InjectionRecipe`, `I N I / N E N / I N I`) matches only while `craftable` (false). Found in the lab's rack (41).
* **Client**: `WolverineGestures` (14), `WolverineActing` (stances WOLVERINE_CLAWS, WOLVERINE_BERSERK, WOLVERINE_CLIMBING; first-person forearms), `client/wolverine/ClawLayer` (three blades per empty hand, 0→1 over 3 ticks out, 2 in, smoothstep; first person on `RenderArmEvent` LOW), `WolverineCowlLayer` (the classic cowl's fins), `ScentView`, `WolverineHud` (the Rage's red edge, "Combo n/3", the claws, the Rage bar, Track's arrow). Run character WOLVERINE. Looks `classic`, `x_men_2000` (named in 0xE8A33A), `own`.

### 40.2 Deadpool

* `Power.DEADPOOL` (after WOLVERINE; 0xC8102E / 0xFF8A8A, title 0xD6262E), no flight, trait `HEALING_FACTOR`. Plug-in `deadpool/DeadpoolServer` → `DeadpoolAbilities`. **Kid-safe rules**: no blood, no swearing, no film quotes or catchphrases, cartoon pistols with gold tracers, no unmasking, no romance jokes; every line our own (EN/HE).
* **Body**: +10 health, +15% speed, +2 attack, +0.4 attack speed, +0.1 jump, +3 safe fall; not heavy. Kidpool: `Attributes.SCALE` −0.2 on the look.
* **Keys**: R `Katanas` (5/5/8 `deadpool_katana` in a 70° cone within 3.5, the X-cut sweeps 3; gap 8, reset 30; their own gap, not R's slot) / Sneak, held: Parry (`Live.DEADPOOL_PARRYING`, ≤ `parryMaxTicks` 80: projectiles from the front bounce the way he looks and become his, a front blow −60%; then R's slot 60). G `Pistols` (pairs of `deadpool_bullet` 4, speed 3.5, range 48, `DeadpoolBulletEntity`; magazine 16, automatic reload 40; `Live.DEADPOOL_AIMING` 40 ticks after a shot; bullets pass villagers and animals) / Sneak: Trick Shot (8, one bounce toward the nearest hurtable creature within 12 that the bounce point sees; G's slot 80). V `TeleportBelt` (≤ 24 onto a safe spot, else within 3; never a wall, fluid, or across `HexManager.onTeleport`'s edges; glitch `glitchChance` 0.10 lands 3-6 off with a line; nowhere: refused, no cooldown; 40) / Sneak: Drop-In (8 above the creature looked at within 32, a server-driven plunge, 8 on landing, fall guarded; 160). B `Taunt` (a pose and a line; hostile non-boss mobs within 16 turn on him for 100 ticks via `LivingChangeTargetEvent`; 200) / Sneak: Chimichanga Break (`HealingBurst.burst` with `chimichangaFraction` 1.0 if he carries a chimichanga, eaten, + Absorption II 30 s, else the burst's 0.5; its own 400 clock). N Dogpool (`DogpoolEntity extends Wolf`, tamed, 20 health, bite 4, the wolf's follow/owner-target goals, never a player `AreaTargets.mayHurt` spares; 120 s or N again; at 0 health it naps (a poof, no death); poofs when he leaves, dies, changes dimension or loses the row; nobody may sit, feed, breed or leash it; a DISCARDED removal is refused unless poofing, so a server without animals keeps it; 1200) / Sneak: Smoke Vanish (Invisibility 80, mobs within 16 lose him; its own 400 clock).
* **Lines** (`DeadpoolLines`, `message.enhanced.deadpool.line.<group>.<n>`, 42: taunt 8, rise 4, down 4, glitch 4, dogpool 4, chimichanga 4, reader 4, wolverine 4, two_healers 2, poisoned 2, awaken 2): a `DeadpoolFxPayload` TAUNT (arg = pose·4096 + group·64 + n) to everyone within 24; each client writes it in its own language in a speech bubble (`client/deadpool/LineBubbles`, drawn as a name tag) and his own in the action bar. Server `linesEnabled`, `lineGapTicks` 200 (not the Taunt's); client `[deadpool] deadpoolLines`. Thoughts `deadpool` (6) and `dogpool` (3).
* **The Weapon X Pod's treatment** (`PodTreatment.use(player, lowerPos)`, the pod block hands its right-click from either half): a powerless player within 0.9 of the middle of the lower block, feet at its floor; the hatch holds him (speed and jump −100%), `podTicks` 100 with bubbles, heartbeat and "Treatment… 5"; then `Powers.awaken(DEADPOOL, classic)`, a red flash, an awaken line. Sneak, stepping away, the pod gone or a power gained cancels; a hero refused; one player per pod; `podAwakens` off → "not ready". The pod's block entity runs while the treatment does (the hatch, light, bubbles and looped hum; not saved).
* **The chimichanga** (`item/ChimichangaItem`, food 8 / 0.8; shapeless bread + `#enhanced:chimichanga_fillings` + carrot → 2). Choose a Hero → Deadpool gives three (`HeroItems`).
* **Client**: `DeadpoolGestures` (16), `DeadpoolActing` (stances DEADPOOL_SWORDS, DEADPOOL_GUNS), `DeadpoolGearLayer` (katanas on the back and pistols at the hips, in the empty hands while used, spinning in the Parry; first person via `RenderArmEvent`), `KatanaModel`, `PistolModel`, `DeadpoolBulletRenderer`, `DeadpoolEffects`, `DeadpoolHud`, `DogpoolRenderer`. Run character DEADPOOL. Looks `classic`, `trainee`, `kidpool` (+ cape), `own`.

### 40.3 Shared

* `Live` 53 → **58** (HEALING_DOWN, WOLVERINE_BERSERK, WOLVERINE_POUNCING, DEADPOOL_PARRYING, DEADPOOL_AIMING), `Toggle` 19 → **22** (CLAWS_OUT, CLAW_CLIMB, SCENT_TRACK). Damage types `wolverine_claws` and `deadpool_katana` (in `#enhanced:melee_blows`), `wolverine_pounce`, `deadpool_bullet` (`is_projectile`), with death messages EN/HE; `HeroDamage.TYPES` maps them to their rows. Neither damage type defeats a healer: the two can't finish each other.
* **Hero Creator**: 6 + 5 rows each (`wolverine.slash|claws|pounce|berserker|senses|body`, `deadpool.katanas|pistols|belt|taunt|dogpool|body`, and the variants `wolverine.spin_slash|claw_climb|feral_leap|healing_burst|track`, `deadpool.parry|trick_shot|drop_in|chimichanga|smoke`): **176** rows + 6 stone rows. `deadpool.katanas` has `altRes(SHIELD)` and `deadpool.parry` family SHIELD; `wolverine.claw_climb` requires `wolverine.claws`. Presets `wolverine.json` (54 points), `deadpool.json` (53); `HeroPresets.HEROES` and `AnimCatalog.RUN_CHARACTERS` list them. Advancements tab `enhanced:mutants/`: root "Snikt!" (an injection in the inventory), `healing_factor`, `maximum_snacks`, `similar_energies`.
* Payloads `HealingFxPayload`, `WolverineFxPayload`, `ScentPayload`, `DeadpoolFxPayload`. Config `[wolverine]`, `[adamantium_injection]`, `[deadpool]`, `[dogpool]`.

## 41. The X-Men lower lab and Cerebro (added in 3.7)

Eilon asked for an entrance to Cerebro in the Mutant School, with Professor X's Cerebro tied to it, and for Wolverine to be found in a lower lab under the school. Lanes L4 (the lab) and L5 (Cerebro). The player's guide: [The X-Men lower lab](wiki/Mutant-School-Lab.md).

* **Placed at run time, never by world generation** (`school/LabBuilder`, the `HydraGarrison` pattern), so the schools of 3.3 to 3.6 get it too and `SchoolRepair` stays valid. Every 40 ticks per player in the Overworld, schools within `buildRadius` 96 are found by their structure references (cached per chunk 600 ticks) and recorded in SavedData `enhanced_school_labs` (`SchoolLabs.Lab`: key = start chunk, `State` NONE / BUILDING(step) / DONE / BLOCKED(reason), `version`, and the school piece's origin, turn, pivot and a cursor). The Overworld's tick builds a BUILDING lab when every chunk of its box and the study is FULL and no player stands in its box, within **1.5 ms of wall time per tick for all labs**, the clock read every 4 cells. Steps, each idempotent (a crash resumes): CHECK (`too_deep` if the lab's floor would fall under min build height + 4) → SHELL (the box's faces from the template, whatever was there) → INSIDE (cells top-down, air included, `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE`, chests with their loot table and seed `levelSeed ^ pos`, signs with their text) → SWEEP (a fluid that crept in replaced by the template) → ENTRANCE (its own tick) → DONE (`school_lab_ready` to players within 96, a log line). A real school builds in ~35-50 ticks under ground.
* **The template** `data/enhanced/structure/mutant_school/lower_lab.nbt`, **45 × 28 × 42**, at the school template's (1, −31, 1), turned as the school piece (`SitePoints.toWorld`, `school/LabSite`); three untouched layers between it and the old cellar. Upper level (standing 22): lift lobby (the pads at (3,22,15)/(3,22,16) on an iron dais), training corridor, medical bay (Cure Station (16,22,6), chest `chests/school_lab/medical`), briefing room, lockers (`lockers`); a stair tunnel; lower level (standing 12): the hall with Cerebro's round X-door (x 19, y 12..14, z 20..22), the Weapon X lab (the sealed glass tank `TANK_BOX`, rack (6,12,26), pod (10,12,38), chest `weapon_x`), Cerebro: a sphere of inner radius 11 round (31,12,21), a 3-wide iron walkway with two-high bar rails to a quartz platform of radius 3 and the console (31,12,23, facing north), a ladder up the platform's stem. Built by `SCRATCH/v37/art/l4/lower_lab.py` with the 3.3 toolkit; rooms lit ≥ 8, the sphere ≥ 7 where one stands. Fixed points in `world/SchoolLabPlan`.
* **The no-holes rule**: nothing above ground changes but the four study shelves, the two pad planks and the lectern's book, and only over the template's own blocks (`SchoolRepair.same`; otherwise BLOCKED("entrance") for the entrance alone until `force`); no shaft (the lift is a ride); the shell is set before the inside opens; every door framed; the old cellar and the 3 layers above the lab never touched. `school/NoHoles` counts shell holes, stray fluids and unframed doors (`LabTests`, `status`).
* **The entrance** (`school/SecretEntrance`): `STUDY_SHELVES` (the west wall's shelves north of the fireplace) → `secret_bookshelf` (facing east, a hinge; right-click: the group swings open for 100 ticks, closes when no living thing stands in it, rechecked every 20), `STUDY_PADS` → `elevator_pad`, the empty lectern → "The Professor's notes" (a written book of translatable pages). `mayOpen`: `openToAll`, operators, or `X_MEN` (Professor X, Cyclops, Wolverine, the Phoenix, Magneto, Quicksilver, Deadpool).
* **The lift** (`school/Lift`): Sneak held `liftSneakTicks` 20 on a pad (on the ground, not a passenger) rides once per press to the linked pad of a DONE lab, facing out; up opens the shelves first; a missing study pad sends him to the school's front door outside. `LabFxPayload` LIFT_DOWN/UP (the doors' fade), `lift_ride` + `lift_ding`.
* **The blocks** (S0 registered all; hardness −1 in a lab: unbreakable in Survival): `secret_bookshelf`, `elevator_pad`, `x_door` (FACING, HALF, OPEN, hinge) and `x_door_round` (PART 0-8), opened by `LabDoors` for a player within 2 (scanned every 4 ticks round each player; mobs never) and shut 20 ticks after; `adamantium_rack` + BE (takers by UUID, `injectionsPerPlayer` 1); `weapon_x_pod` + BE (hollow, `PodTreatment` is L3's, 40.2); `cure_station` (a glass bottle → a Mutant Cure, per player per `cureStationCooldown` 1200 kept in his persistent data, only while `cureStation` and `[mutant_cure] enabled`); `cerebro_console` + BE. Renderers `LabRenderers.Rack|Pod|Console`; `LabClient` (the lift's fade, the lab's ambience loop at 0.5 while inside, `LabFxPayload.AMBIENCE` 9, the pod's hum).
* **The command** (`school/LabCommand`, under `SchoolRepair.node()`): `/enhanced structure school_lab [radius]` (128; 16-256) builds the nearest school's lab now, `force` also the entrance, `status` the record and, when DONE, the holes.
* **Loot**: `chests/school_lab/medical` (healing and regeneration potions, paper, bottles, honey, milk; golden apple 10%, Mutant Cure 30%), `lockers` (black and yellow leather, bread, a book; spyglass 40%, "X-Men Trainee Banner" 30%), `weapon_x` (iron, "Adamantium Shavings", redstone, paper; netherite scrap 10%).
* **Cerebro** (`finders/CerebroRoom`, one seam in `XavierAbilities.cerebro`): the plain N is unchanged (256 blocks, his dimension, 10 s; every `XavierTests` green). `amplify(player)`: a console within `cerebroConsoleReach` 4 and not worn by another → the **amplified** Cerebro for `cerebroAmplifiedTicks` 600: every player of every dimension (`cerebroAmplifiedAllDimensions`) not a spectator nor shielded by Magneto's helmet, no range, his own dimension first; `CerebroPayload` (protocol "11") with `Mind.dimension`, `amplified` and up to 6 `Site(kind, pos)` from `SiteSearch.nearest` for the kinds whose route in his realm is SEARCH (`cerebroShowsStoneSites`). Any telepath (`TELEPATH` or `TELEPATH_STRONG`, `cerebroConsoleTelepathsOnly`) right-clicks the console (`useConsole`); N at the console amplifies only for whoever has the Cerebro key. A second click takes the helmet off; ends on time, death, leaving, more than reach + 6 from the console, the console gone, or no longer a telepath. Reports every 20 ticks to him and to every player inside the sphere (radius 12). Cooldown: Cerebro's 1200, on his Cerebro key or an own record for telepaths without one. `Live.CEREBRO` for Professor X while amplified. Client `client/finders/CerebroSphere`: the minds as additive stars on the inner surface at radius 10.3 (azimuth the direction, elevation 70° near to 8° at 10,000 blocks; another dimension's minds a labelled cluster low on the sphere), the sites as diamonds with "Time Stone 810m", three pulsing bands, dark halos so they read on the light panels; the console's helmet leaves its stand while on (`LabRenderers.Console.helmetOff = CerebroSphere::consoleOn`). `XavierOverlay` names a far mind's realm.
* Config `[school_lab]`, the five `professor_x.cerebro*` options.

## 42. The Stone Locator (added in 3.7)

Eilon: "You must already have at least one Infinity Stone. It reads that stone's energy and searches for similar energies"; his recipe of 4 netherite blocks, 3 diamond blocks and a wither skeleton skull, the ninth slot the stone. Lane L5, `finders/`. The player's guide: [Stone Locator](wiki/Stone-Locator.md).

* **The recipe** (`LocatorRecipe`, type `enhanced:stone_locator`, `NKN / DSD / NDN`, S = `#enhanced:locator_cores`, only while `craftable`): a real core (`StoneLocator.coreKind`: a stone; Loki's Scepter → Mind, the Tesseract → Space, the Eye of Agamotto → Time, the Orb → Power, a crafted Gauntlet with at least one stone → its selected or first stone; never the Titan's or an empty one). `assemble` tunes the result; `getRemainingItems` leaves **the very core** in the centre slot (`copyWithCount(1)`: taker stamp, Gauntlet contents, every component) while `returnsCore`. Recipe unlock: any locator core; advancement `mutants/similar_energies`.
* **The item** (`item/StoneLocatorItem`, epic, one per stack, fire-proof) with the component `enhanced:locator` = `LocatorState(tuned, filter, target GlobalPos, status, distance, pointing)`; statuses SEARCHING, FOUND, OTHER_REALM, SOMEONE_ELSE, TOO_FAR, HAS_ALL, HIDDEN. While in a hand, `StoneLocator.read` every 20 ticks, the component written **only when it changed** (no re-equip bob).
* **What it wants**: a kind is *got* if carried in any form (bare, a Gauntlet, its vessel, the Orb), or for a site stone if the ledger won't give it to him (player mode: taken before; world mode: he is the holder); a site stone another holds in world mode is SOMEONE_ELSE and skipped. Of the lacking kinds (or the filter's one: Sneak + use cycles every stone → each lacking kind; a kind got meanwhile is dropped), the nearest target wins; HAS_ALL points north.
* **Routes** (`LocatorRoutes`, pure, shared with the HUD): Overworld: Mind/Time/Space search `#enhanced:stone_site/mind|time|space` (`hydra_outpost`, `sanctum`, `avengers_tower`); Power → GATEWAY `#minecraft:eye_of_ender_located` (strongholds); Reality → GATEWAY `#enhanced:bifrost_sites` (vault shut: HIDDEN, still the Bifrost site); Soul spins ("comes from the Nether"). Nether: Soul searches `vormir_cliff`, the others spin. End: Power searches `end_tunnel`. Asgard: Reality → FIXED `CityPlan.REALITY_PEDESTAL` while `odinAwake`, else HIDDEN; the others "Not in Asgard.". The Road: everything spins. `pointsToGateways` off turns gateways into spins.
* **The search** (`SiteSearch`): random-spread grids (`getPotentialStructureChunk`, `checkStructurePresence` answers free; `getChunk(STRUCTURE_STARTS)` + `getStartForStructure` is the counted load) and stronghold rings (`getRingPositionsFor`), rings of cells to one past the first find, within `searchRadius` 4096 (Nether `netherSearchRadius` 1024). A server-wide queue, round-robin, at most `startChecksPerTick` 2 loads and 2048 cells a tick; loads shared between players; an answer kept `recheckTicks` 600, until he moves `moveRecheck` 64, or until its site is spent; searches stop 100 ticks after nobody asks. The target is the start chunk's locate position.
* **Spent sites** (`SpentSites`, SavedData `enhanced_stone_sites_spent` per level: kind → start chunks; `mark` finds the start by `getStructureWithPieceAt`): the Tesseract cradle's take (Space), `SiteStoneLootFunction.run` (the End vault's roll: Power), the loot function `enhanced:mark_spent {stone: mind}` on the Scepter entry of `chests/hydra_outpost/vault` (Mind). Sanctums, Vormir and Asgard are never spent (their pedestals decide per player).
* **Use** (`pulse`): `StoneFxPayload.LOCATOR_TRAIL` (12 points toward the target, or a ring with none) to the holder and his trackers, `locator_pulse`; `pulseCooldown` 100 in its own record. `locator_found` on a new site, `locator_tick` on a search start or a filter change.
* **Client**: `LocatorClient` (item property `enhanced:angle` = the vanilla compass function toward `target`; spins without one or in another dimension, sweeps slowly while searching; 32 needle models; the socket tinted by the tuned stone, white with all six or untuned; the trail drawer) and `FindersHud` (its own GUI layer: one line over the hotbar, the gem and "Soul Stone · 810 blocks · north-east" or the status, a grey second line for the realm's words; the sites under Cerebro's minds). Config `[stone_locator]`.

## 43. Agatha's Cottage (added in 3.7)

Eilon's mom asked for a house where Agatha, her son Nicky and Death (Rio Vidal) live together: a good ending in this multiverse. Lane L6, `cottage/`. Eilon then asked for the Darkhold in Agatha's chest. The player's guide: [Agatha's Cottage](wiki/Agathas-Cottage.md).

* **The structure** `enhanced:agatha_cottage` (`template_site`, `surface_structures`, floor 3, `max_slope` 4, `"ground": "highest"`, processors `enhanced:agatha_cottage` (10% mossy/cracked, foundation), `enabled_by` `agatha_cottage.enabled`, no monster spawns), biome tag `#enhanced:has_structure/agatha_cottage` (forest, birch forest, old growth birch forest, flower forest, dark forest), set `enhanced:agatha_cottages` (40 / 14, salt 20240918, 6 chunks from `mutant_schools`). Template `agatha_cottage/house` **27 × 18 × 25** (`SCRATCH/v37/art/l6/cottage.py`): fence and gate sign, porch with rocking chairs, kitchen with the family table (4 seats), living room, Agatha's room with the study corner, Nicky's and Rio's rooms, the cellar with the `witches_road_door`, Rio's garden, greenhouse and Señor Scratchy's hutch. Points in `world/CottagePlan` (homes, `POT`, `STOVE`, `HERBS`, `PORCH_CHAIRS`, `BENCH`, `GREENHOUSE`, `GARDEN`, `YARD`, `HUTCH`, `HUTCH_GATE`, `ROAD_DOOR`, `TABLE_SEATS`, `BEDS`, `GATE`).
* **The residents**: a second entity type `enhanced:cottage_resident` of the Asgardians' class (`AsgardianEntity`), with three appended roles **AGATHA** (30 health, trader, `hero()` = `Power.AGATHA`, yields), **NICKY** (20, scale 0.75), **RIO** (30, trader), `Role.cottage()`. So they get the talk, the merchant screen, the "never hurt by players" rule, voices and `Flee`/`ReturnHome` for free; `AsgardPopulation.refuses` judges only type `asgardian`, and the cottage type is not in `#enhanced:asgardians` (Hela's army and Odin's allies are Asgard's); it is in `#enhanced:grab_immune`. Skins `textures/entity/asgardian/agatha|nicky|rio.png` (Agatha at home, a boy with a toy witch's hat, Rio in a cloak of bark and leaves: no skull, no ribs).
* **The keeper** (`CottageKeeper`, SavedData `enhanced_cottages` on the Overworld, the `HydraGarrison` pattern; its own keep, since `AsgardPopulation.keep` makes `asgardian` and reads `asgard.npcs`): every 10 ticks per player cottages within 64 are found by their structure starts (cached per chunk 600); every 100 ticks one Agatha, Nicky and Rio each (slots `cottage/<key>/<ROLE>`), a lost one remade unseen, duplicates refused on join, `residents` off: nobody; the cellar door registered in `RoadDoors` once (`roadDoor` off: removed, "the door sleeps"); the morning run-off; the ambience `ambient.cottage` every 400 ticks at 0.5 to whoever is in the grounds.
* **One of each** (3.5.1): while a player is Agatha (and `agathaYields`) the cottage's Agatha leaves in a violet shimmer (`agatha_away`), none is made, one loading in is refused; she comes back the same way. Nicky runs to a player-Agatha within 16 ("Mom! You're home!", "Two Moms?!" for two), once per visit. The lines that name the player (`mom_player`, `agatha_player`, `mom_you`, `agatha_you`) are chosen by the answers' actions, **not** by `AsgardDialogue.swap` (`OneOfEachTests` checks every swap names the player who is Odin).
* **Their day** (`CottageGoals`: `Clock`, `Shelter`, `Seated`, `Life`; by `dayTime % 24000`: morning < 2000, day < 11000, dusk < 12500, night < 23000): herbs, the pot, reading, the porch; Nicky feeding Scratchy, playing with the toy broom, chasing Scratchy; Rio's greenhouse, beds, bench and a flower for Nicky; dinner at the table with hearts; beds. Rain keeps them in; monsters within 10 send them home; one stuck or lost is set down while nobody sees. Gates count `tickCount + id` (a mob asks its new goals only on ticks where that is even; the integrator fixed the Asgardians' own gates the same way).
* **Talk, trades, gifts** (`CottageDialogue`, nodes of `AsgardDialogue`; every line EN/HE ≤ 140): Agatha's greeting, family, the Road, heroes' lines (Billy, Wanda, Tommy, Wolverine, Deadpool, Strange, the Hulk), Scratchy's quest; Nicky's play, Mom, Rio, Scratchy, heroes' lines (Wolverine, Deadpool, the Hulk, Billy) and a gift once a day (`giftsPerDay`, counted in `CottageProgress.flags`): 3 cookies, a flower or "A drawing of you (it has a cape)"; Rio's garden, Agatha, Nicky, her blessing once a day (Regeneration I `blessingTicks` 600) and, only when asked "Who are you, really?", her one gentle line about being Death. `CottageTrades` (`AsgardTrades.USES` 6): Agatha 7 offers (Healing 2, Night Vision 3, Slow Falling 3, Fire Resistance 4, 6 purple candles 1, an enchanted book of a kind enchantment at level 1-2 for 10 emeralds + a book, "Agatha's Soup" 2: a stew with Regeneration 8 s), Rio 14 (six flowers ×4, three saplings ×2, wheat seeds ×8, pumpkin seeds ×4, bone meal ×8, glow berries ×6 for 2, moss ×4). Sleepers answer "Zzz..."; the chairs, porch and bench seat a player (`SeatEntity`).
* **Señor Scratchy** (`Scratchy`): a brown rabbit, persistent, tagged with his cottage, kept at the hutch; each morning (before 6000, once a day) with chance `scratchyRunsOff` 0.3 he is moved unseen 30-60 blocks to dry ground; not found for 6000 ticks, he is home again. Whoever accepts the quest sees him glow green within 24 (`client/cottage/CottageClient`, an `EntityGlow` source on the synced `COTTAGE` flag); within 4 of the hutch he is home: `nicky_cheer`, `scratchy_found`, and Agatha (Rio while a player is Agatha) gives each finder within 16 3 emeralds and a potion of Luck, once per run-off.
* **Chests**: `chests/agatha_cottage/kitchen` (chimichanga 10%), `cellar` (an enchanted book 30%; never the Darkhold), `nicky` (toy broom, drawing, a music disc 15%), and **`study`** in Agatha's room: always **the Darkhold** and 3-5 books of Protection, Unbreaking, Sharpness, Feather Falling, Efficiency or Fire Protection at levels 2-4 (Unbreaking 2-3). The Darkhold has no uniqueness rule (it is crafted), so the chest always holds one.
* Attachment `COTTAGE` (`CottageProgress(flags, lastGiftDay, lastBlessingDay, scratchyRun)`, kept on death). Config `[agatha_cottage]`.

## 44. Meeting Death (added in 3.7)

Eilon, the same evening: "Every time you die, you meet Death, and she lets you start again or start again in Hell." Death is Rio, the Green Witch (43), gentle and calm. Lane L8, `death/` and `client/death/`. The player's guide: [Meeting Death](wiki/Meeting-Death.md).

* **The offer** (`DeathMeeting`): `LivingDeathEvent` LOWEST (so a cancelled death never gets it: the healing factor's DOWN, the Road trial, Ultron's Second Life, a totem) sends `DeathMeetingPayload.Offer(nether, fireSeconds)` before vanilla's death packet, and again on login while dead. `nether` = `enabled`, `allowNether`, the Nether level present, not hardcore, not `doImmediateRespawn` (a hardcore world and `doImmediateRespawn` get no offer at all).
* **The choice** (`DeathMeetingPayload.Choose`, `DeathMeeting.choose(player, AGAIN|NETHER)`): refused unless he is dead (by the player list: a dead body is removed 20 ticks after death, which the real client found) and has not won the game; the respawn goes through vanilla's `handleClientCommand(PERFORM_RESPAWN)`, so every respawn handler runs as usual. For NETHER, `PlayerRespawnPositionEvent` LOW redirects to the Nether unless another handler already changed the transition (it wins) or it already goes there (a charged anchor); `PlayerRespawnEvent` HIGH adds Fire Resistance `fireResistanceSeconds` 10, the green chat line, `rio_blessing` and particles.
* **The landing** (`NetherLanding`): centre = the respawn position × the dimensions' scale (x/8, z/8) at y 64, clamped to the border; square rings out to radius 16 (budget 1089 columns), per column the safe feet nearest y 64 (`SpaceRealms.safe`: a solid floor that is not bedrock, air at feet and head; under the roof by 6; no lava, fire, magma or campfire within 1). None: a 3 × 3 obsidian floor in a 5 × 5 netherrack one, 5 × 5 × 3 of air, fluids, fire and falling blocks in the shell turned to netherrack.
* **The screen** (`client/death/DeathMeetingClient` swaps `DeathScreen` at `ScreenEvent.Opening` while the client option `[death_meeting] showDeathMeeting`; `DeathMeetingScreen extends DeathScreen`, so vanilla's respawn closes it): a misty green clearing (`textures/gui/death/mist|leaves|glow|vines|grass.png`), Rio on the left (the `cottage_resident` entity as RIO with its own renderer, eyes on the mouse; since 3.7.2 in her Death look, `client/death/DeathLook`: a persistent tag on the screen's own Rio makes `AsgardianRenderer` use `textures/entity/asgardian/rio_death.png`, the dark green-black gown and long hood, and draw the crown of branches, a small baked model with `rio_death_crown.png`, as RIO's headgear; the cottage's Rio is unchanged), the title "You met Death", one of six lines, the cause and score (read from vanilla's screen), **Start again** and **Start again in the Nether** (when offered) with their tooltips, and vanilla's small Title screen; the buttons wake after 20 ticks, Esc doesn't close it; `rio_blessing` once.
* Shared edits: one `define` line in `EnhancedConfig`, the client option, two payload registrations (protocol stays "11"), a lang block of 20 keys. Config `[death_meeting]`.

## 45. How 3.7 was put together

* **S0, the skeleton** (branch `v37-skel`, from 3.6.0 with **3.6.1 merged in**: the Soul Stone's sacrifice repaired, one Gungnir per world, Tommy on water, the new Avengers Tower): `Power.WOLVERINE` and `DEADPOOL`, the 22 rows (**176**), `Trait.HEALING_FACTOR`, the five `Live` and three `Toggle` bits, the `Role`s AGATHA, NICKY, RIO and the `cottage_resident` type, the eight lab blocks and three block entities, the items, every config option (six classes and `XavierConfig`'s five), the **63 sound events** with placeholders, damage types, tags, lang keys, payloads (`HealingFxPayload`, `WolverineFxPayload`, `ScentPayload`, `DeadpoolFxPayload`, `LabFxPayload`, `StoneFxPayload.LOCATOR_TRAIL`, the new `CerebroPayload` fields), attachments, the placeholder templates of the lab and the cottage with their fixed points (`SchoolLabPlan`, `CottagePlan`), the stubs each lane filled, mod_version 3.7.0 and **protocol "11"** (`MutantSkeletonTests.theBitsFitAndTheProtocolIsEleven`). Full run after the 3.6.1 merge: `All 2247 required tests passed`.
* **Lanes**: L1 the healing factor, L2 Wolverine, L3 Deadpool, L4 the lower lab, L5 the finders (the Stone Locator and the lab's Cerebro), L6 Agatha's Cottage, L7 sound (the 63 sounds, synthesised), and L8 meeting Death (Eilon's request of the same evening, from S0 + L1). Each owned its packages and wrote into the shared files only in its own region (`// ===== healing|wolverine|deadpool|school_lab|finders|cottage (3.7) =====`, and `wolverine (3.7)`, `deadpool (3.7)`, `variants (3.7)` in `HeroAbility`); lang blocks were added 30 keys before the end of the files (`add_lang37.py`) and merged by union. The integrator merged them into `v37` in this order: S0, L1, L4, L5, L2, L3, L8, L7, L6; then the joins (the Cerebro console's helmet leaves its stand while on; Wolverine's and Deadpool's presets and runs listed, the pod's hatch following the treatment; the Asgardians' "every N ticks" goal gates counting `tickCount + id`, found by the cottage lane; the client config's Deadpool section words; the lab builder reading its clock every 4 cells; the keepers' test areas held loaded).
* As before, delete `run/world` **and** `run/config/enhanced-server.toml` before a full GameTest run. Every lane checked its visuals in a real client (dedicated server, English and Hebrew clients, 3+ render rounds) as well as its GameTests (`HealingTests`, `WolverineTests`, `DeadpoolTests`, `LabTests`, `LocatorTests`, `CerebroRoomTests`, `CottageTests`, `DeathMeetingTests`, `SoundAssetTests`); the integrator's run of the lanes' batches with the shared tests: `All 432 required tests passed`.

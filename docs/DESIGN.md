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
* **Apocalypse (En Sabah Nur)**, the first mutant: a **boss** whose golden shield stops everything except mind power, and, for those who beat him, a **hero** of sand, levitation, portals and a shield of his own **(added in 3.0)**

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
| Ability keys **(added)** | Ability 1–5 are bound to **R, G, V, B, N** by default (rebind under *Options → Controls → Enhanced Powers*). Each key triggers the ability with the same number in that hero's list. One more key, **Z** (*Vision, That One!*), belongs to the Scarlet Witch alone: it is her order to the Vision her Hex raises (section 2). |
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
| 4 | B (hold) | **Chaos Shield** | A red glowing sphere that stops projectiles and melee blows and softens explosions. It takes time to form, wears out and can shatter. |
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
* **Cost:** it burns hunger quickly while held (about 1 food point every 2 seconds), while it
  forms too. When the hunger bar is empty the shield collapses (and has to recharge).
* **HUD:** "forming" while it grows; once it is up, what is left of its strength (green, then
  yellow, then red) and the seconds it can still be held. The sphere flickers as it weakens
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
    him by, and telekinesis can't lift him.
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
| 3 | V | **Telekinesis** | The new `Telekinesis.Mode.JEAN` (violet): Wanda's and Billy's rules, so creatures, players, dropped items, vehicles, primed TNT, falling blocks and projectiles in flight, and blocks (hardness up to 5, no block entities, not `telekinesis_immovable`, spawn protection and claims respected). Press again to launch, Sneak + V to set down. What she throws is `enhanced:telekinetic_slam` with her as the causing entity: Phoenix mind power (13.7). Range, speed and block limits are the shared `magneto.*` and `scarlet_witch.blockLift*` options. |
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
| Magneto | The Iron Spider is metal: he can be lifted (30 s for a player) and his nanites scattered (14.9). The other suits are cloth. |
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
  `spellbound_immune` (the Freeze, the Petrify, the Time Lock), `confused_immune` and `telepathy_immune`; his sword, meteors
  and orb are in `telekinesis_immune`, `shapeshift_immune` and `reality_proof`; a phantom is in `shapeshift_immune` and `boss_minions`
  (Wanda can lift one).
* **Hooks (`BossHooks`):** Quicksilver Time slows him to one half and not at all with the Time Stone; Magneto's strip takes
  nothing; Spider-Man's web locks the Gauntlet; Thor's lightning is a hook of the framework.
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
or lifted with telekinesis; the Hex bounces him and never kills him (from the lift's lock to the end of the fight it lets him, the Sentries and the fighters through). Prime cannot drop below 10% before the island is up.

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
resistance 0.2, no fall damage; immune to fire, drowning, poison and the Wither; **magnetic** (Magneto can lift and throw
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
  animals and items stay on the ground. Telekinesis, Magneto's holds and Mjolnir's pins are released at the lock (a thrown
  hammer is not recalled: it comes back to its owner as after any throw).
* **On the island:** more Sentries, and every 5 s, for every two fighters, a **red ring** (radius 3) under one of them for
  1.5 s and a piece of Sokovia falls on it: **10 damage**, nothing breaks.
* **The field.** A fighter who strays more than 4 past the rim, 3 below the cone or 80 above is put back on the island,
  takes 2 damage that cannot kill, gets Slowness for 3 s, at most once in 5 s. On the ground the same field is at the rim + 12
  and also catches anyone more than 12 blocks underground. A fighter who died stands in the **pit** for 2 s to be carried up
  again (3 times).
* **The arena is protected** from the ritual to the end: no block placed or broken, no fluid, no piston, explosions break nothing,
  mobs break nothing, no natural spawn; the mod's own block-changing abilities refuse; teleports that would leave the cylinder
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
* **Heart rip.** A Scarlet Witch using telekinesis on a body at or below 15% of its health tears out its core: it is
  destroyed at once. Launching a Sentry she holds crushes it.
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
breath, immune to Poison and the Wither; he is **not magnetic** (vibranium), and **lightning does 1.25 times** (he is electronics).

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
and Mjolnir's pin work on him; he cannot lift Mjolnir; Magneto's metal control does nothing to him; nothing he does breaks a block.

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
Glowing); the Hex bounces him (never kills him); Mjolnir's pin, Billy's Freeze, Loki's Scepter, telekinesis, shapeshifting and the Phoenix's
reading and possession do nothing. A hit takes at most **12 percent** of his maximum health (`maxHitFraction`), explosions do half.
His health follows the number of fighters: it is recomputed at the start of every transition and every phase, and he keeps the fraction of health
he has.

### 17.3 The shield: the one rule

**Mind power wears the shield down. Nothing else touches him.** The test (`apocalypse/MindPower`) is a pure function of the damage: it counts if and
only if its damage type is in the data tag `enhanced:mind_power` (the Chaos Beam, a telekinetic slam, the Phoenix Burst), its causing
entity is a player whose power is in `mindPowerHeroes` (Scarlet Witch and Phoenix by default, so a slam thrown by Magneto or Billy is not mind
power), and it arrives as damage. The same function is the hero's shield's own rule (17.9).

| Hit | While the shield is up |
|---|---|
| Chaos Beam | amount x `chaosBeamFactor` (1): 5 a hit, one hit per attacker every `mindHitCooldownTicks` (10) |
| A telekinetic throw (a creature, an item, a block) | amount x `slamFactor` (1) |
| Phoenix Burst | amount x `phoenixBurstFactor` (**5**): a Burst of 30 takes 150, **breaking shield 1 at once for a lone fighter** (150; 203 for two) |
| Phoenix throws | as the Scarlet Witch's |
| Phoenix Mind Control, Psionic Field | a **strain** (25 and 60 points) even if refused, once a cast: a Phoenix strains his shield with each of them at most once per her own recharge of it (`mindControlCooldown`, `psionicCooldown`), however often she presses the key |
| Everything else (swords, arrows, TNT, fire, potions, pets, Mjolnir, lightning, Magneto's shrapnel, Vision's beam, Billy's Hex Bolt, the stones, the other heroes) | **nothing**: no damage, no knockback, a golden ripple and a chime (`apocalypse_shield_hit`) |

The server owner can add `"billy"` or `"vision"` to `mindPowerHeroes` and they then count with the same damage types. The shield's strength is
`shieldStrength` (150) x `shieldGrowth` (1.25) ^ (phase - 1) x (1 + `shieldPerExtraPlayer` (0.35) (n - 1)): **150, 188, 234** for one fighter,
255, 319, 398 for three. It knits in 40 ticks, 2 seconds (nothing hurts him then), mends 3 percent a second once 8 seconds (`shieldRegenDelayTicks`
160) have passed with no mind power, and he heals 4 a second (`regenPerSecond`) while it is up, never above the part of the fight he is in.
The boss bar is blue (his health, with notches at the floors) and a second, yellow bar for the shield ("broken" with the seconds left,
"forming").

### 17.4 The fight, phase by phase

| Part | What happens |
|---|---|
| **Phase 1: The God Awakens** (100% to 66%) | Shield 1. Sand Lances, Sand Wave, Sand Spikes, Boulder Barrage, Portal Step, Backhand. |
| **Window 1** | The shield breaks: he kneels (staggered 3 s, attacking nothing), 10 s (`windowTicks` 200) in which **everything hurts him**, x1.25 (`windowDamageFactor`), armour counting. His health cannot fall below **two thirds** (the floor); reaching it ends the window. If nobody does enough, the shield re-forms at full strength. After the kneel he fights on with Lances, Backhand and Spikes at half speed. |
| **Transition 1** (6 s) | Untouchable; **the Four Horsemen** step out of four portals; shield 2 forms. *"Rise, my Horsemen."* |
| **Phase 2: The Horsemen** (66% to 33%) | Shield 2. **Entomb, Mind Crush and one Tornado** join. Window 2 (floor at one third). |
| **Transition 2: the Cairo Collapse** (8 s) | He floats up, the sky goes dark ochre, **three rings of sand** cross the arena (30 ticks apart); any Horseman still alive crumbles; shield 3 forms. *"Everything they've built will fall..."* |
| **Phase 3: Unmasked** (33% to 5%) | Shield 3. **Three Tornadoes at once.** Its break has **no window**: |
| **The armour falls** | The plates fly off (collar, pauldrons, chest, gauntlets, the crown of the cap), the shield is gone for good, he is in the **true form**, and **everything hurts him fully**, with no window; his health stops at 15% until the Escape has been tried, then at 5% (the kneel), so both always happen. If he is not hurt for 80 ticks he heals 3 a second (`trueFormRegenPerSecond`). |
| **The Escape** (once, at 15%) | A hand rises; a violet portal grows over him for 100 ticks (`escapeChannelTicks`). **Interrupted by any lightning damage (Thor's), by `escapeInterruptFraction` (4%) of his health in damage during the channel, or by a Hex wall.** Interrupted: staggered 6 s and never again. Not interrupted: he vanishes and returns 10 s later at `escapeHealTo` (40%) with a fresh shield 3. The blow that earns the Escape starts it before his healing can mend him. |
| **The end** (5%) | Untouchable and kneeling: *"All is revealed."* He dies **in the Phoenix's fire, layer by layer** if a Phoenix dealt Burst or throw damage in the last `phoenixFinishSeconds` (15), otherwise in a column of sand (`apocalypse_death`). He drops nothing. |

The fight is meant to last 6 to 10 minutes for two or three equipped players (a lone Scarlet Witch: 8 to 12, mostly dodging). The two
floors are why a fight can never be won early, whatever damage a group brings.

### 17.5 The attacks and their tells

Every attack that can hurt has a **tell** (a pose, a glow, a sound) and then a **fixed aim moment**; after it, it does not follow. His sand is
**sand, not metal** (Magneto's shield does not catch it). He chooses by weight among the attacks that are ready and in range, never the
same twice in a row; damage scales with difficulty like a mob's.

| Attack | From | Tell (and the lock) | What it does |
|---|---|---|---|
| **Sand Lances** | 1 | The right hand rises, the fingers pinch; five thin ochre lines from the palm to the chest (locked 6 ticks before) | `lanceCount` (5) in a flat fan, **6** each. Billy and the Phoenix can catch them and throw them back |
| **Sand Wave** | 1 | Both arms sweep low; a faint lane on the ground, 4 wide (locks just over halfway) | A ridge 1.1 blocks tall: **10**, knockback, Slowness II; lower than a jump's peak |
| **Sand Spikes** | 1 | A fist rises and slams; an amber ring under each of up to four players, tracking, then locked | Spikes in each ring: **9**, launched up |
| **Boulder Barrage** | 1 | Both arms rise; three blocks of sand tear out of the ground and orbit over his head with a growing hum; amber lines to up to three players | Three boulders: **11** each; can be caught and thrown back |
| **Portal Step** | all | Violet shards at his feet; a second ring opens at the exit, usually behind the player farthest from him | An arrival shockwave: **6**, knockback. A Hex wall refuses it |
| **Backhand** | all | The arm drawn across the chest (8 ticks) | **16**, strong knockback |
| **Entomb** | 2 | His hand reaches to the player who wore the shield most; a stripe of sand crawls to their feet and locks 10 ticks before the hand closes | If still on the spot: a **cocoon** for `entombTicks` (100): no movement, jumping, flight, attacks, items or powers; 1.5 a second, never below two hearts; `cocoonHealth` (20) to break it. A complete Chaos Shield skips it; a phased Vision is intangible; `enhanced:entomb_immune` |
| **Mind Crush** | 2 | Two fingers to the temple, his eyes flash white-violet, a violet ring contracts from 28 blocks to nothing (2 s) | Everyone within `mindCrushRange` (28): Nausea (120), Slowness 3 s, Blindness 2 s. **Magneto's helmet turns it away.** A Phoenix who touches him with Mind Control or the Psionic Field during the tell interrupts it. The Chaos Shield does not stop it |
| **Tornado** | 2 (one), 3 (three) | Both arms rise; the sky above darkens; dust spirals up at one to three spots | Funnels for 160 ticks that drift toward the nearest player, slower than a walk: 2 every half second, and they lift what is inside |
| **Cairo Collapse** | T2 | The ground ripples; he floats up, arms wide | Three rings: **12**, strong knockback |

The Lances and Boulders are entities (`sand_lance`, `sand_boulder`, both of `AbstractSandProjectile`) that implement `ShieldCost`, so the Chaos Shield
stops them and pays for their damage, and a projectile that Billy or the Phoenix catches is thrown back like any other. His blows are the damage types `sand`, `apocalypse_blow` and `entomb`.
**No attack ever changes a block**: the sand wave, the spikes, the tornadoes and the wall are effects and entities, so mobGriefing changes nothing and there is
nothing to restore after the fight.

### 17.6 The Four Horsemen

At Transition 1 he enhances **Husks** (vanilla desert mobs): `min(horsemenMax (4), 1 + fighters)`, in the order War, Famine, Pestilence, Death. Each is
named, glows, has `horsemanHealthFactor` (2) times a Husk's health, `horsemanDamageBonus` (4) more attack damage, a little more speed and armour,
and **drops nothing and gives no experience**. They live until killed or until Transition 2. They are ordinary creatures: Wanda can lift them,
a Phoenix can take one over, Billy can freeze them. A Magneto hears, once, "Join me, Erik."

| Horseman | Gear | Trick |
|---|---|---|
| **War** | iron helmet, chestplate, axe | **Shard Volley** every 5 s: three iron shards hover, then fly in a fan, 4 each (Magneto can catch them) |
| **Famine** | none | **Gale** every 6 s: white lightning on the hands, a bolt falls beside the target, a gust blows back (3; Thor takes no lightning damage but the wind still pushes) |
| **Pestilence** | iron sword | **Psi-Blade Dash** every 5 s: the sword glows violet, a six-block leap: 7 and Slowness |
| **Death** | none | **Dive** every 7 s: he springs up, hangs, dives with a slam: 9 in a radius of 2.5 |

### 17.7 The arena, and when it goes wrong

* **Protection** (from the planting to the end, a circle of the arena + 8): no block is placed or broken, no fluid, bucket, piston or TNT works,
  explosions break nothing, and the mod's own block-changing abilities refuse. **The wall of sand** (4 outside the arena, 40 high) is solid to
  players and to what they throw; whoever touches it is pushed back once a second for `fieldDamage` (1) that cannot kill, and flight ends at
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
wall. Magneto's powers do nothing to him unless he wears iron, chainmail or gold (he is flesh and sand is not metal). With **PvP off** his Wave, Entomb
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

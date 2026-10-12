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
| One power per player | A player has exactly one of: *none*, Magneto, Scarlet Witch, Quicksilver, Vision, Tommy, Billy, Loki, Thor, Phoenix, Spider-Man. Powers never stack. |
| Permanent | Once unlocked, a power survives death, logging out and changing dimension. Only an operator command can remove it (`/enhanced power set <player> none`). |
| Ability keys **(added)** | Ability 1–5 are bound to **R, G, V, B, N** by default (rebind under *Options → Controls → Enhanced Powers*). Each key triggers the ability with the same number in that hero's list. One more key, **Z** (*Vision, That One!*), belongs to the Scarlet Witch alone: it is her order to the Vision her Hex raises (section 2). |
| Flight | Flying works like Creative mode: double-tap **Space**, or press the hero's flight key. Quicksilver doesn't fly, he runs; Spider-Man doesn't fly either: he zips and swings on webs (section 14). |
| Flight and run animation **(added)** | Every hero's pose is blended from the situation instead of switched, so nothing snaps and other players see it too. Each flier has a flight of his own. Magneto glides with his arms spread, leaning about 40° at full speed. Wanda floats upright with her palms open and stretches out, arms ahead, about 60° forward in a sprint. Vision is a straight body with arms at his sides, leaning about 65° at speed. Billy flies like a kid, arms wide, then ahead at a sprint. Thor leans about 90° behind his weapon, which really is in his fist in every pose (Mjolnir's handle and Stormbreaker's pass through it), whirls on its strap with a swoosh while he takes off and hovers, and is thrust ahead of him at speed; he overshoots a little and settles like a heavy hero. Every flier takes off with a gesture of his own, banks into turns, tips over in a dive, leans back when he climbs and brakes when he stops (Thor lands as a hero, planting the weapon). Quicksilver (in Quicksilver Time and in Super Speed) and Tommy at super speed run with their own stride, with ghosts of the stride behind them and a skid when they stop, and everyone else sprints and walks with a character of his own. Every flier leaves a trail of his own along the path he flew (crimson threads from Wanda's hands, golden threads from Vision's shoulders, field lines from Magneto's, a double helix from Billy's, lightning from Thor's boots), with white wind streaks at speed, a ring at take-off, a shockwave when a hero lands and a sonic boom when Thor or Vision break the sound barrier. Three client options (in `config/enhanced-client.toml`, section `[animation]`, and behind the *Config* button of Enhanced in the Mods screen; nothing there is synced) change it: *Hero animations* turns the hero animation off (vanilla poses), *Reduced motion* tones it down (hover bob and drift at a third, no trembling hands, jitter or camera shake), and *Flight effects* turns off the trails, wind and rings. |
| Ability animation **(added)** | Every ability is acted, not just switched on, and other players see it too. **Magneto** draws his field out around him with both arms, closes a claw on the metal and wrenches it away, and holds what he lifts with a steady open hand. The **Scarlet Witch** gathers her arms across her chest and throws them open to raise the Chaos Shield, thrusts both hands out for the beam, raises both hands overhead to grow the Hex and moves them apart or together to make it bigger or smaller; her lifting hand is a raised, shaking claw. **Quicksilver** taps his goggles, starts like a sprinter, jabs a flurry of fists in his blitz and darts a hand out to snatch. **Vision** puts his fingers to his temple before the beam, floats his arms wide to let go of the world and braces to become dense. **Tommy** shakes his arms out, spins twice in his blitz, shivers before he goes through a wall and holds out a vibrating palm. **Billy** thrusts his hand at the target of the Hex Bolt, throws both hands up to freeze and presses them to his ears for the Noise. **Loki** flicks his daggers from over the shoulder, raises a spread hand to cast and roars as the Frost Giant; his Scepter jabs instead of chopping. **Thor** hauls back and whips his arm to throw the weapon, holds out his hand for it, is slapped by it as it comes home (with sparks), kicks back as the lightning leaves his hand and holds his arm out along the beam while he fires it, thrusts Mjolnir up as it bursts into fragments and closes his fist as they come together, brings it down on whoever he pins, and roars as the God of Thunder. Your own gesture starts the moment you press the key, without waiting for the server. In first person your hands move too (hands in and up for the shield, a palm thrust out for the bolt, the dagger flicked forward, the weapon whirling at the edge of the screen) and the beam, the tether and the bolt leave the hand where it is drawn. |
| HUD **(added)** | The top-left corner shows your hero, each ability with its key, cooldowns, toggles and resources. |
| Looks **(added)** | Vision and the newer heroes have several looks (variants), each with its own skin, cape and energy colours: White Vision, the TVA Loki, Mighty Thor, the Phoenix, Spider-Man... Press **H** to change look (**Sneak + H** goes back). A look changes only how the hero looks and the colour of his energy: the abilities are the same in every look (the one exception is Spider-Man, whose suits also differ in protection, and only the Iron Spider has the legs: section 14). Every look of the newer heroes includes *Your own skin*: the hero's powers in your own skin. |
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

---

## 10. Operator commands **(added)**

| Command | Effect |
|---|---|
| `/enhanced power get [player]` | Show a player's power. |
| `/enhanced power set <players> <none\|magneto\|scarlet_witch\|quicksilver\|vision\|tommy\|billy\|loki\|thor\|phoenix\|spider_man> [look]` | Give or remove a power, optionally straight into one of its looks. |
| `/enhanced look <players> <look>` | Change the look of every given player whose hero has that look (e.g. `tva`, `white_vision`, `mighty_thor`, `brand_new_day`, `iron_spider`). |
| `/enhanced hex list` | List all Hexes in the current dimension. |
| `/enhanced hex clear` | Dissolve every Hex in the current dimension. |

**(added)** `/enhanced_anim` is a client-only tool (no operator rights, nothing changes for anyone else) for looking at the
animation: `play <gesture>` starts any gesture on yourself (and can freeze it at a tick, with `hold` and `release`), `fly <s> <u> <turn>`
pretends a flight speed, climb and turn so a pose can be seen without flying that fast, `stance <stance> on|off|auto` forces a held
pose, `target <name>` acts on another player in view, and `state` shows the numbers behind the pose.

## 11. Server configuration **(added)**

All the main numbers (beam damage, Hex death chance, Hex radius, flight speeds,
shrapnel damage, shield reflect chance, the Chaos Shield's wind-up, time limit, strength and cooldowns, how much
Quicksilver Time slows the world, how much faster Quicksilver does everything, Tommy's speed, Loki's dagger damage, how
long Loki's Scepter confuses, Mjolnir's and Stormbreaker's damage and range, Thor's lightning beam (damage, range, its
strength with each weapon and as the God of Thunder, its hunger cost), the cooldown of shattering Mjolnir and the damage of
its fragments, how hard the Hex wall bites a Vision who pushes against it, the Vision of the Hex (health, punch,
beam, how long Wanda needs to rebuild him, or turning him off), how long it pins someone down, who is worthy of Mjolnir and whether it belongs to the first worthy player
who lifts it (`mjolnirOwnerOnly`), the Bifrost's
cooldown, how far the Phoenix's Mind Reading, Mind Sense and Mind Control reach and what they cost, how many creatures she can take over and for how long, the Psionic Field, and every number of the Phoenix Burst (charge time, radius, damage, what a player takes, wave speed, fire, knockback, whether it sets fires or hurts villagers and animals, hunger, cooldowns and the drain afterwards), Spider-Man's speed, jump, strength and fall damage, how long the Radioactive Spider's bite takes, how far spider-sense reaches and how often it scans, every number of the Web Shot, the Web Burst, the Web Zip (including the kick), the Web Swing and the Wall Crawl, how long webs and web blocks last and how many he can have, organic webs, each suit's protection and how long Magneto's strip lasts, the Iron Spider's legs, every number of Instant Kill (including whether it may strike players), and every number of the Black Eyes (what they give, what they cost, how long they last, the crash, and whether low health sets them off)…) can be changed in `world/serverconfig/enhanced-server.toml`. The four client options
(*Hero animations*, *Reduced motion*, *Flight effects*, *Hide heroes' armour*) are in `config/enhanced-client.toml` of the player's own game.

## 12. Sounds: the voices of the Infinity Stones **(added)**

Every Infinity Stone has a voice of its own, so the stones can be told apart by ear. They are synthesised from
scratch like all the mod's sounds (no samples), and mastered under the mod's other sounds, so that a stone sounds
**under** the portal, the awakening or the chime it comes with, never on top of it. Each stone has a short *use*
sound (two or three takes of it, so it is never quite the same twice) and a quiet *hum* that loops.

| Stone | Its voice | Where it sounds in the mod today |
|---|---|---|
| Space (the Tesseract) | A deep, swelling cosmic hum | Under the portal at both ends of Loki's escape and of a hero's jump, and when the Tesseract and the gene awaken the Scarlet Witch (there is no portal then); a soft hum while anyone holds the Tesseract. |
| Mind (Loki's Scepter, Vision's forehead) | A bright, shimmering, slightly eerie chime-hum | When the scepter's stone is touched (the awakening of the Scarlet Witch or of Vision, a hero it recognises, the touch that kills); under the chime of a hit that confuses (not over a creature the blow killed); as Vision's beam starts; a soft hum while anyone holds the scepter. |
| Power | A heavy violet roar | Registered, not used yet: it is for the Thanos boss (3.0). |
| Reality | A liquid, bending, backwards-running swell | The same. |
| Soul | A breathy, mournful choir tone | The same. |
| Time | A ticking, rushing, rewinding shimmer | The same. |

The hum of the Space and Mind Stones is a quiet client-side loop for as long as the Tesseract or the scepter is in
either hand of a player: everyone near hears it, it follows the player, fades in and out with the item and stops when
it is put away (the *Players* volume slider of the game turns it down). The Thanos boss will use the other four
voices as he gathers the stones: the sound events `power_stone`, `reality_stone`, `soul_stone` and `time_stone` (and
their `_hum` loops) are already registered, in `sounds.json` and in `ModSounds`, and `InfinityStone` lists all six.

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

**Mind Sense** (Sneak + R). A press that sets `Live.MIND_SENSING` for `mindSenseTicks` (120): a ring of violet motes spreads from her at 2 blocks a tick up to `mindSenseRange` (48), everyone sees it, and **only she** gets the glow through walls (an `EntityGlow` source): players `#E9E0FF`, hostile `#FF5C8A`, everyone else `#A98BFF`. Every mind counts (a hidden Loki, not his duplicates), and so do the helmeted as "silences". The action bar says "Minds nearby: 7 (2 players, 3 hostile)". Cooldown of slot 1: `mindSenseCooldown` (400), cost `mindSenseExhaustion` (2.0).

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
  alive and without a power, `Powers.awaken(SPIDER_MAN)` (the title "With great power comes great responsibility", the
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

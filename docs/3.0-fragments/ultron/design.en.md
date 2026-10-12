## 16. Ultron — the boss and the hero **(added)**

> "I'm here to help." In *Avengers: Age of Ultron* Tony Stark and Bruce Banner configure the neural net in the Mind Stone
> of Loki's Scepter; what wakes decides that peace means the end of humanity, and lifts a city into the sky to drop it. In
> Enhanced he is a **boss** (the biggest fight in the mod) and, for those who beat him, a **hero**.

Ultron has two parts that share a name: the **boss fight** (16.1 to 16.6), which is the only way to get the power, and the
**hero** (16.7 to 16.9) a player becomes with the reward. Both are in the **Enhanced** creative tab.

### 16.1 The Ultron Program and the ritual

* **The Ultron Program** ("Stark's peacekeeping program, waiting for a mind"). Crafted, never found in a chest, so
  Ultron is never met by surprise: a Nether Star in the middle, two Echo Shards above and below it, a Block of Redstone
  to its left and right and a Block of Iron in each corner (a Wither, an Ancient City and 36 iron). The recipe unlocks on
  picking up a Nether Star. Stack size 1, fireproof, epic, with the enchantment glint.
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
for undead only: he is a machine, so Harming hurts him). He is immune to fire, drowning, poison, the Wither and suffocation,
cannot be slowed (Quicksilver Time slows the Sentries, never the bodies), frozen, pinned, confused, shapeshifted, mind-controlled
or lifted with telekinesis; the Hex bounces him and never kills him. Prime cannot drop below 10% before the island is up.

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
A **plasma bolt** (range 4 to 22, every 2 to 3 s, a 0.5 s tell, 1.4 blocks a tick, **4 damage**), a **punch** (within 2.2,
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
  animals and items stay on the ground. Telekinesis, Magneto's holds and Mjolnir's pins are released at the lock, and thrown
  hammers are recalled.
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
  on the real ground at the same place relative to the middle, and the hidden columns come back.
* **The Tremor.** If the lift is switched off, the site cannot hold the island at the warning, or `liftMode` is `TREMOR`,
  the same warning, Sentries and falling rock happen on the ground and nothing moves.
* **Failure is one small rule.** The only record is a header of a few hundred bytes (`data/enhanced/sokovia`), written
  before the island is built and deleted after it is cleared. On a clean stop the island is cleared at once; after a crash the
  next start clears the island's shape at altitude, sets players down on the ground, discards the boss's leftover entities and
  deletes the file. A lost file leaves at worst a floating island of ordinary blocks (`/enhanced boss cleanup`), never a hole
  in the ground. `CUT_GROUND` (really lifting the ground) is a setting kept for later and is not in this release.

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
  into the inventory; if that is full it drops owner-only for 10 minutes; a fighter who is dead or offline gets it as a gift at the
  next login or respawn. Sentries and bodies drop nothing.
* **When the fight fails.** Everyone dead for 15 s (a wipe), nobody within 64 blocks for 90 s, or 25 minutes: the bosses dissolve,
  the island lowers, no reward and **no refund**. An operator's stop, a change to Peaceful, or the server stopping: the island is put
  back and the Program is given back. `/kill` on the body ends the fight with no reward.

### 16.7 Becoming Ultron

**Ultron's Core** ("The heart Wanda tore out. It still hums."): stack 1, fireproof, epic, with the glint, no recipe, never in a chest.
A player with **no power** who holds the Core in one hand and Loki's Scepter in the other and uses the Scepter awakens as Ultron:
the Core is used up (not in Creative), a red title, *"I was meant to be new. I was meant to be beautiful."*, a burst and a message to
the server. A player with a power keeps the Core and is told "Your body already holds a power"; the Core, like the gene, protects its
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
| 3 | Sneak + V | **Gravity Slam** | A ring of anti-gravity to 7 blocks: every creature in it that is not his takes **6 damage**, is thrown outward and pressed down, and is slowed for 3 s. Cooldown 8 s. |
| 4 | B | **Upload** | Teleports him into the place of the drone he looks at (or the nearest within 48 blocks); the drone bursts into data; he is healed 8 (4 hearts) and loses harmful effects and fire. Cooldown 25 s. |
| 4 | Sneak + B | **Hack** | A 1-second cast on an enemy Sentry within 32: it becomes his drone. At most 2 hacked at a time; never works on a body; in the boss fight the Sentry leaves the count. |
| 4 | passive | **Second Life** | A blow that would kill him (after a Totem of Undying), with ability 4 ready and a drone within 64 blocks: the death is cancelled, he is set to 25% inside the drone, Resistance IV for 3 s; ability 4 then recharges for **5 minutes**. Not against the void or `/kill`. |
| 5 | N | **Sentry Army** | Three drones assemble round him (1 s): 24 health, a 3-damage bolt every 1.5 s, red eyes. They follow him (catching up by teleport past 24 blocks), attack what hurt him or what he last hit and hostile mobs within 12, and never a player unless PvP is on and that player attacked. At most 6; each lasts 90 s and all fall apart if he dies, leaves, changes dimension or loses the power. Cooldown 20 s. |
| 5 | Sneak + N | **Command** | All drones attack the creature he looks at within 32, or gather round him. |

**Vision cuts the net:** within 48 blocks of a Vision (or White Vision), Upload, Hack and Second Life refuse ("Vision has cut
your connection") and the HUD says "cut off". **PvP off:** his bolts, Overcharge, Pull and Slam neither hurt nor move players and
his drones never target them. Creative and Spectator players are never touched by his abilities. **Rules:** the Hex bounces him like
any hero (his drones follow his permission, as pets do); Quicksilver Time slows him and hangs his bolts; Billy's Freeze, Loki's Scepter
and Mjolnir's pin work on him; he cannot lift Mjolnir; Magneto's metal control does nothing to him; nothing he does breaks a block.

### 16.9 Looks

Four looks, with **H** (Sneak + H goes back): **Ultron Prime (Age of Ultron)** (default: gunmetal plates, bronze pipes, a ridged
crest and antennae, red-orange slit eyes; amber-gold plasma with a red edge), **Ultron Sentry** (matte black, a domed head, a cyan cheek
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

* **Commands (operators):** `/enhanced boss start ultron [force]` starts the fight at your feet (no Program, no Scepter; `force` skips
  the site rules but not the sky), `/enhanced boss stop` ends it and gives the Program back, `/enhanced boss list`, `/enhanced boss cleanup
  <center> <radius>` removes a leftover island, `/enhanced power set <players> ultron [look]` and `/enhanced look <players> prime|sentry|human|own`.
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

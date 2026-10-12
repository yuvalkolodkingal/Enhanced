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

An operator calls him anywhere with `/enhanced boss start thanos [force]` (the site and the rest are skipped by `force`).

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
  (three times for stone-born damage); at `staggerPoise` (100) the move breaks and he is **staggered** for 40 ticks and his
  Gauntlet is dark for `lockTicks` (80): only the sword and fist moves. Soul Drain breaks at 60 poise, the Choke at 60
  damage. `lockGauntlet` (Spider-Man's web: `webLockTicks` 40, at most once in `webLockCooldown` 400) does the same to a
  clench in progress.
* **A mob's blow** (a golem, a wolf) does a quarter, and a golem that hits him is swatted dead with the back of his hand.

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
* **Wanda:** the Chaos Beam and a Telekinetic Slam tear plates; her shield stops everything of his that is a projectile or a
  beam. **Vision** is what he hunts first. **Loki's** duplicates lure him until the Soul Stone. **Thor:** Mjolnir breaks his
  helmet, Stormbreaker does 2.5 times and cuts a beam.

### 15.9 Anti-cheese

A pillar, a hole, a bunker or a boat: the unreachable rule and the Pull, the Step and the Meteors that reach anywhere in the
arena. Sniping from outside: the clock needs a fighter inside `arenaRadius + 24`. Standing out of the arena: no fighter near him
for `abandonSeconds` (90) and he leaves. Everyone dead for `wipeSeconds` (15): he leaves. The world is never changed.

### 15.10 The end and the reward

* **He falls.** A blow that would kill leaves him one health and he kneels for 100 ticks, invulnerable, says his last words at
  tick 40 and turns to dust from tick 60; the fight is won at once. The experience (`xpReward`, 12,000) falls where he stood.
* **The reward** (`BossRewards`): each participant who was there `rewardMinSeconds` and did at least `rewardMinShare` (3 percent)
  of his maximum health, and whoever struck the last blow, gets **one Infinity Gauntlet** and `xpRewardPerPlayer` points. A
  Creative player never does. It goes into the inventory; a full one drops it for that player alone; a dead or absent player's
  waits as a gift. The rest is `summonCooldownTicks` (12,000: ten minutes).
* **Any other end** (a wipe, `abandonSeconds`, the server stopping, an operator): he steps through a ring of Space (30 ticks)
  and the rest is half as long; nobody is rewarded.
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
* **Billy:** a Freeze holds his hand. **Loki:** the Soul Sight is the answer to the duplicates, the hiding and the shape.
* **Vision:** phased through everything he cannot be touched. **Magneto:** the Gauntlet is a model, not an item: nothing to
  take. **Quicksilver Time:** slows him like anyone; the Rewind and the Lock run on the game clock.
* **PvP off:** the Blast, Slam, Pull, Transmute, Drain and Lock do nothing to players and say so; Creative and Spectator
  players are never touched.
* **The Mad Titan:** the hero's Blast is a beam and the Space Barrier turns it away; the Drain is no projectile and passes
  it; the damage counts in the ledger; a boss cannot be pulled, turned to stone or locked (the tags) but is hurt by both.
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
> The boss is a model of his own and does not use this pipeline (15.6.1).

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
at the lower left of the screen, with the Gauntlet on it (the `PlayerRendererMixin` hook, 15.2.6), breathing a little. Every row
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

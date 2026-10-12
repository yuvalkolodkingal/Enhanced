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
| **True form** (after the third shield) | 1.2 times | the same | the same pool | none | 28 percent faster (0.36) |

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
| Phoenix Burst | amount x `phoenixBurstFactor` (**5**): a Burst of 30 takes 150, **breaking shield 1 at once** |
| Phoenix throws | as the Scarlet Witch's |
| Phoenix Mind Control, Psionic Field | a **strain** (25 and 60 points) even if refused |
| Everything else (swords, arrows, TNT, fire, potions, pets, Mjolnir, lightning, Magneto's shrapnel, Vision's beam, Billy's Hex Bolt, the stones, the other heroes) | **nothing**: no damage, no knockback, a golden ripple and a chime (`apocalypse_shield_hit`) |

The server owner can add `"billy"` or `"vision"` to `mindPowerHeroes` and they then count with the same damage types. The shield's strength is
`shieldStrength` (150) x `shieldGrowth` (1.25) ^ (phase - 1) x (1 + `shieldPerExtraPlayer` (0.35) (n - 1)): **150, 188, 234** for one fighter,
255, 319, 398 for three. It knits in 60 ticks (nothing hurts him then), mends 3 percent a second once 8 seconds (`shieldRegenDelayTicks`
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
| **The armour falls** | The plates fly off (collar, pauldrons, chest, gauntlets, the crown of the cap), the shield is gone for good, he is in the **true form**, and **everything hurts him fully**, no window and no floor. If he is not hurt for 80 ticks he heals 3 a second (`trueFormRegenPerSecond`). |
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
  `creativeGetsReward`); pets credit nobody. Each also gets 1,500 experience. It goes into the inventory, else drops owner-only for 10 minutes; a
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
| 4 | B (hold) | **Apocalypse's Shield** | A golden hexagonal sphere (radius 2.4): 10 ticks to form, **stops nothing until complete**, then stops every kind of damage **except mind power** (and except falls, drowning, suffocation, starvation, the void and `/kill`). Projectiles are neutralised before they hit; Mjolnir and Stormbreaker are turned back to Thor. Held up to **6 s**, **60 points of strength**; what it stops wears it by `shieldOtherWear` (0.5) of the damage, **mind power in full** (the Burst five times over), and when it breaks the rest of that blow comes through. Recharge 8 s (20 s after it shatters), a little food. |
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

* **Commands (operators):** `/enhanced boss start apocalypse [force]` starts the fight at your feet (no Capstone), `/enhanced boss stop` ends it and gives the Capstone
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

124 game tests in seven classes: `ApocalypseRitualTests` (8: every refusal of the ritual and what is used up), `ApocalypseShieldTests` (18: the rule of mind
power, every kind of hit against the shield, the windows, the floors), `ApocalypseBossTests` (19: each attack and its tell, the Horsemen), `ApocalypseFightTests`
(23: the phases, transitions, the Escape, the end, the rewards, the failures), `ApocalypseTests` (24: the hero's five abilities, the body, the Essence),
`ApocalypseAnimationTests` (15) and `ApocalypseClientTests` (17: the cube table, the poses, the tells, the music and the amber air, and the art). Any test that touches a fight has a
batch of its own (`apocalypse_...`), because a window nobody uses is lost; the pure ones share `apocalypse_pure`. The art is pinned by the tests too: no two regions of
the texture overlap, every texture is the size the model reads, and the glow masks light only a small part of the sheet. `DataTests` checks the lang in both languages, the subtitles and that every
`.ogg` exists.

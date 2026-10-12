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
* **Not the Mind Stone.** The Phoenix is touched with neither Loki's Scepter nor the Scarlet Witch Gene.

### 13.2 Abilities

Keys **R G V B N** are abilities 1 to 5. Every number is an option in the `[phoenix]` section of the server config.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R (toggle) | **Mind Reading** | `Toggle.MIND_READING`. Every 5 ticks the server finds the first living thing along her view that has a mind (line of sight, `mindReadRange` 24) and sends her a snapshot (13.3); the client shows it in a panel beside the crosshair. Costs `mindReadExhaustion` (0.01) a tick. The read player is told once per 10 seconds (`mindReadNotifies`). **Sneak + R: Mind Sense** (below). |
| 2 | G (toggle) | **Flight** | Survival flight, speed `flightSpeed` (0.09). Pinned: no. A Hex wall bounces her like every hero. |
| 3 | V | **Telekinesis** | The new `Telekinesis.Mode.JEAN` (violet): Wanda's and Billy's rules, so creatures, players, dropped items, vehicles, primed TNT, falling blocks and projectiles in flight, and blocks (hardness up to 5, no block entities, not `telekinesis_immovable`, spawn protection and claims respected). Press again to launch, Sneak + V to set down. What she throws is `enhanced:telekinetic_slam` with her as the causing entity: Phoenix mind power (13.7). Range, speed and block limits are the shared `magneto.*` and `scarlet_witch.blockLift*` options. |
| 4 | B | **Mind Control** | The creature in her line of sight within `mindControlRange` (10) gets the effect `enhanced:possessed` for `mindControlTicks` (300). A host never targets her, her pets or her team (a strict rule: a possessed mob takes no target but the ones she sends it after), and every 10 ticks it is steered: the creature that last hurt her (24 blocks), the one she last hit (5 seconds), the nearest hostile within 16 blocks of her, else it walks back to within 4 blocks and stands guard. `mindControlMax` (1) hosts at once: a new one frees the oldest, who wakes `CONFUSED` for 2 seconds. A player is only held (`SPELLBOUND`, `mindControlPlayerTicks` 60) and only with PvP on. Refused: `enhanced:telepathy_immune`, Creative and Spectator players, Magneto's helmet, Loki's duplicates, armour stands, a creature pinned by Mjolnir, a stranger's pet with PvP off. Cooldown `mindControlCooldown` (400), cost `mindControlExhaustion` (6.0). **Sneak + B: the Psionic Field**, everything living within `psionicRadius` (16) of her, through walls, is `SPELLBOUND` for `psionicTicks` (60); not herself, her pets and team, her host, Creative and Spectator players, `spellbound_immune` and `telepathy_immune` creatures or whoever wears Magneto's helmet; players only with PvP on. Cooldown `psionicCooldown` (1200) on the same slot, cost `psionicExhaustion` (8.0), none of either if nobody is held. |
| 5 | N (hold) | **Phoenix Burst** | 13.4. |

**Mind Sense** (Sneak + R). A press that sets `Live.MIND_SENSING` for `mindSenseTicks` (120): a ring of violet motes spreads from her at 2 blocks a tick up to `mindSenseRange` (48), everyone sees it, and **only she** gets the glow through walls (an `EntityGlow` source): players `#E9E0FF`, hostile `#FF5C8A`, everyone else `#A98BFF`. Every mind counts (a hidden Loki, not his duplicates), and so do the helmeted as "silences". The action bar says "Minds nearby: 7 (2 players, 3 hostile)". Cooldown of slot 1: `mindSenseCooldown` (400), cost `mindSenseExhaustion` (2.0).

**Passive: the Phoenix is not burned.** Damage of the `minecraft:is_fire` tag is cancelled (HIGH `LivingIncomingDamageEvent`) and she is put out each tick, if `fireImmune` (true). Unstoppable damage still hurts. Her own Burst never hurts her.

### 13.3 Mind Reading

* **The snapshot** (`ability/MindReading`, sent in a `MindReadPayload` to her alone, when the target changes or the content does, and at least every 10 ticks while she reads; the panel hides 15 ticks after the last one): the name and the entity type, the state (`OK`, `BLOCKED`, `ILLUSION`, `TOO_VAST`, `PLAYERS_OFF`, decided in that order), the mood (`CALM`, `ALERT`, `HOSTILE` with the name of its target, `AFRAID`), health, seven worn slots (main hand, off hand, head, chest, legs, feet, body), what it carries (a player's 36 slots; a creature's inventory, at most 8 stacks; an Enderman's block) and an extra line for a shapeshifted Loki or a disguised Vision.
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

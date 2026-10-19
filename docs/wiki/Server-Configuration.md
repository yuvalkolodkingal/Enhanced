# Server Configuration

Every balance number of the mod lives in one file, **`world/serverconfig/enhanced-server.toml`** in the world (or server) folder. The mod creates it when the world is first loaded, with every option at its default, and syncs it to the clients. It is a plain text file: open it in any editor, change a value and restart the world or the server.

The file has one section per hero (a boss has a section for his fight and one for his hero, and `[boss]` holds the rules every boss shares; [Build Magic](#build-magic) has three; the [Witches' Road](#the-witches-road), the [Book of the Vishanti](#the-book-of-the-vishanti), the [Darkhold](#the-darkhold), the [Hex Runes](#hex-runes), the [Hero Creator](#the-hero-creator), since 3.5 [Asgard](#asgard), [the Bifrost](#the-bifrost) and the [world's music](#music), and since 3.6 [the Infinity Stones](#the-infinity-stones-36), [the Soul Stone](#the-soul-stone-and-vormir), [the stone sites](#the-stone-sites), [Doctor Strange](#doctor-strange-the-eye-of-agamotto-and-the-sling-ring), [the Hulk](#the-hulk-and-the-gamma-emitter) and [the Hydra soldiers](#hydra-soldiers) have their own), and an option is written as `section.key`. For example, Wanda's Hex death chance is the `hexDeathChance` key in the `[scarlet_witch]` section:

```toml
[scarlet_witch]
    hexDeathChance = 0.95
```

Every number has an allowed range, and a value outside it is reset to the default when the file is loaded. A *tick* is 1/20 of a second. Food exhaustion of 4.0 costs 1 food point (one drumstick half). Flying speeds are in Minecraft's own units, where Creative flight is 0.05.

The mod has no server option for anything that is not listed here. Everything else (what counts as metal, what can be lifted, which bosses resist) is decided by datapack tags: see [Datapack tags](#datapack-tags) below.

## Magneto

Section `[magneto]`. The hero's page is [Magneto](Magneto).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `magneto.flightSpeed` | `0.075` | 0.01 to 0.5 | Magneto's flying speed (Creative flight is 0.05). |
| `magneto.telekinesisRange` | `32` | 4 to 128 | How far away (in blocks) every telekinesis (Magneto, Wanda, Billy, the Phoenix) can grab a target. |
| `magneto.throwSpeed` | `2.6` | 0.5 to 6.0 | Launch speed, in blocks per tick, of a telekinetic throw. |
| `magneto.shrapnelDamage` | `4.0` | 0.0 to 100.0 | Damage of one iron shrapnel shard. |
| `magneto.shrapnelPierce` | `3` | 1 to 16 | How many creatures one shard can pierce through. |
| `magneto.shrapnelFireInterval` | `2` | 1 to 20 | Ticks between shards while the Shrapnel Storm is held (2 = 10 shards per second). |
| `magneto.disarmRange` | `32` | 4 to 128 | Range of Armor & Weapon Strip, in blocks. |
| `magneto.disarmCooldown` | `60` | 0 to 6000 | Cooldown of Armor & Weapon Strip, in ticks (60 = 3 seconds). |
| `magneto.shieldReflectChance` | `0.5` | 0.0 to 1.0 | Chance that a metal projectile caught by the Magnetic Shield flies back at the shooter (otherwise it drops). |
| `magneto.blockLiftRange` | `24` | 2 to 128 | How far away (in blocks) Magneto can tear a metal block out of the ground with R. |
| `magneto.blockLiftMaxHardness` | `50.0` | 0.0 to 100.0 | The hardest metal block Magneto can lift with R (iron block = 5, ancient debris = 30, block of netherite = 50). |
| `magneto.pullRadius` | `8` | 2 to 32 | Radius (in blocks) of the Magnetic Pull (Sneak + R when R isn't holding anything): a sphere round where he pressed. Ores come through rock. |
| `magneto.pullMaxTargets` | `64` | 1 to 512 | The most things (blocks, items, creatures) one pull takes, the nearest first. |
| `magneto.pullPerTick` | `2` | 1 to 64 | Things the pull takes each tick (2 = 40 a second). |
| `magneto.pullScanPerTick` | `8192` | 512 to 131072 | Blocks the pull looks at in one tick while it searches (a big radius searches over a few ticks). |
| `magneto.pullCooldown` | `100` | 0 to 6000 | Ticks before Magneto can pull again (100 = 5 seconds). It starts only if the pull affected something (took, tugged, dissolved, crumbled, scattered). |
| `magneto.pullExhaustion` | `4.0` | 0.0 to 40.0 | Hunger one pull costs (4 = one hunger point). Nothing if the pull affected nothing. |
| `magneto.pullTakesBuiltMetal` | `true` | `true` or `false` | The pull also takes the metal blocks and fixtures people build with (rails, doors, lanterns, anvils, hoppers, pistons...), whole: never one that holds items, never an extended or powered piston. If `false`: only ores, raw-metal blocks, loose metal and creatures' gear. |
| `magneto.pullBuiltNeedsSight` | `true` | `true` or `false` | Built metal comes only if Magneto can see it (a clear line from his eyes). Ores and raw-metal blocks always come through rock. If `false`, built metal also comes through walls. |
| `magneto.pullBuiltCone` | `120` | 0 to 360 | Built metal comes only from within this cone (in degrees) round where he looks, so he turns round to pull the rest; 360 = all round him. Ores, raw-metal blocks, loose metal, gear and carts come from all round. |
| `magneto.pullFromStorage` | `false` | `true` or `false` | The pull also takes the metal out of chests and other containers, item frames and armor stands he can see (claims are asked; never a villager's work block). The container, frame or stand stays. If `false`, what is stored stays where it is. |
| `magneto.pullTakesGear` | `true` | `true` or `false` | The pull tears metal armor and weapons off hostile creatures in reach. |
| `magneto.pullTakesPlayerGear` | `true` | `true` or `false` | With PvP on, the pull also acts on players Magneto can see: one metal piece into his inventory, an Iron Spider's nanites scattered, or a metal body tugged, once per player for `pullPlayerGraceTicks`. If `false`, the pull never touches a player. |
| `magneto.pullPlayerGearToFeet` | `false` | `true` or `false` | The player's piece drops at that player's feet instead of flying into Magneto's inventory. |
| `magneto.pullPlayerGraceTicks` | `200` | 0 to 6000 | After the pull acted on a player, no pull acts on that player again for this many ticks (200 = 10 seconds). |
| `magneto.telekinesisSneakGraceTicks` | `20` | 0 to 100 | Every telekinesis (Magneto, Wanda, Billy, the Phoenix): for this many ticks after one of the hero's grips ends, Sneak + the key with nothing held does nothing (so a late "put it down" press is not a pull or a torn-out block). |
| `magneto.pullRetryTicks` | `10` | 0 to 100 | After a pull that found nothing, another press does nothing for this many ticks. |
| `magneto.pullAncientDebris` | `true` | `true` or `false` | The pull takes the netherite scrap out of ancient debris (the debris becomes netherrack). |
| `magneto.pullTug` | `1.0` | 0.0 to 3.0 | How hard the pull tugs Iron Golems and other metal bodies toward him (blocks per tick at most). 0 = it leaves them where they are. |
| `magneto.telekinesisSparesFriends` | `true` | `true` or `false` | Every telekinesis (Magneto, Wanda, Billy, the Phoenix): a player, or a creature another player owns (a pet, a drone, the Vision of a Hex), let go or thrown by someone who may not hurt them (PvP off, a teammate) takes no fall, wall or slam damage until it lands. |
| `magneto.telekinesisPlayerRegripTicks` | `60` | 0 to 1200 | Every telekinesis: after letting go of a player, the same hero cannot grip that player again for this many ticks (60 = 3 seconds). |

## Scarlet Witch (and the Hex)

Section `[scarlet_witch]`. The hero's page is [Scarlet Witch](Scarlet-Witch).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `scarlet_witch.flightSpeed` | `0.09` | 0.01 to 0.5 | Wanda's flying speed (Creative flight is 0.05). |
| `scarlet_witch.chaosBeamDamage` | `5.0` | 0.0 to 100.0 | Damage per Chaos Beam hit (5 = 2.5 hearts). Minecraft's damage cooldown allows two hits per second. |
| `scarlet_witch.chaosBeamRange` | `32` | 4 to 128 | Range in blocks of the Chaos Beam, and also of Vision's Mind Stone beam. |
| `scarlet_witch.chaosBeamNauseaTicks` | `100` | 0 to 6000 | Duration of the Nausea the Chaos Beam inflicts, in ticks (100 = 5 seconds). |
| `scarlet_witch.chaosShieldExhaustion` | `0.1` | 0.0 to 4.0 | The old rules (only when `shieldUnlimited` is `false`): Food exhaustion per tick while the Chaos Shield is held (4.0 exhaustion = 1 food point, so 0.1 is about 1 food point every 2 seconds). |
| `scarlet_witch.chaosShieldExplosionFactor` | `0.25` | 0.0 to 1.0 | Fraction of explosion damage that still gets through the Chaos Shield (0.25 = 75% less). |
| `scarlet_witch.chaosShieldWindUpTicks` | `15` | 0 to 200 | How long the Chaos Shield takes to form after B is pressed, in ticks (15 = 3/4 of a second). It blocks nothing until it is complete. Her raise (arms and effects) keeps time with it. |
| `scarlet_witch.chaosShieldMaxTicks` | `160` | 1 to 6000 | The old rules (only when `shieldUnlimited` is `false`): How long a complete Chaos Shield can be held before it drops, in ticks (160 = 8 seconds). |
| `scarlet_witch.chaosShieldStrength` | `30.0` | 1.0 to 1000.0 | The old rules (only when `shieldUnlimited` is `false`): Points of damage the Chaos Shield can stop before it shatters. Everything it stops wears it down: the damage of a projectile, and the explosion damage it takes away. |
| `scarlet_witch.chaosShieldCooldown` | `120` | 0 to 6000 | The old rules (only when `shieldUnlimited` is `false`): Ticks before the Chaos Shield can be raised again after it drops (120 = 6 seconds). |
| `scarlet_witch.chaosShieldBrokenCooldown` | `300` | 0 to 6000 | The old rules (only when `shieldUnlimited` is `false`): Ticks before the Chaos Shield can be raised again after it shattered or was held to its limit (300 = 15 seconds). |
| `scarlet_witch.shieldUnlimited` | `true` | `true` or `false` | 3.2: the Chaos Shield is unlimited. It costs no hunger, has no strength to wear down, never shatters, never runs out of time and needs no recharge after it drops (it still takes `chaosShieldWindUpTicks` to form, and it stops exactly what it stops today). `false`: the old rules, with `chaosShieldExhaustion`, `chaosShieldStrength`, `chaosShieldMaxTicks`, `chaosShieldCooldown` and `chaosShieldBrokenCooldown`. |
| `scarlet_witch.shieldUnlimitedVsBosses` | `true` | `true` or `false` | Whether the unlimited Chaos Shield is unlimited against bosses too. `true`: it is (it never wears down, whoever hits it). `false`: damage that comes from a boss fight (a boss, a minion of one, a projectile one of them shot) wears the shield down by the old rules (`chaosShieldStrength` points, then it shatters and needs `chaosShieldBrokenCooldown`), while ordinary mobs and players still cannot wear it. The time limit and the hunger stay off in both cases. |
| `scarlet_witch.blockLiftRange` | `24` | 2 to 128 | How far away (in blocks) Wanda and the Phoenix can tear a block out of the ground (Billy and Magneto have their own). |
| `scarlet_witch.blockLiftMaxHardness` | `5.0` | 0.0 to 100.0 | The hardest block Wanda and the Phoenix can lift (stone = 1.5, iron block = 5, obsidian = 50; Billy and Magneto have their own). |
| `scarlet_witch.hexDefaultRadius` | `32` | 4 to 256 | Radius, in blocks, of a newly created Hex. |
| `scarlet_witch.hexMinRadius` | `8` | 4 to 256 | Smallest radius Wanda can shrink her Hex to in Hex Control. |
| `scarlet_witch.hexMaxRadius` | `96` | 4 to 256 | Largest radius Wanda can grow her Hex to in Hex Control. |
| `scarlet_witch.hexDeathChance` | `0.95` | 0.0 to 1.0 | Chance that a creature without powers dies when it crosses the Hex wall without Wanda's permission (0.95 = 95%). The rest survive. |
| `scarlet_witch.hexHoldDamage` | `2.0` | 0.0 to 20.0 | Damage Vision takes the first time he is thrown back at the wall of a Hex he is inside (2 = one heart). He can't leave a standing Hex, and comes apart a little at its edge; it never kills him. |
| `scarlet_witch.hexHoldDamageMax` | `8.0` | 0.0 to 40.0 | The most one throw back at the wall can do to Vision. Each time he pushes again within three seconds it grows by `hexHoldDamage`, up to this; it never takes his last half heart. |
| `scarlet_witch.hexHoldReleaseHealth` | `0.3` | 0.0 to 1.0 | A badly hurt Wanda can no longer hold Vision in her Hex: at or below this share of her health (0.3 = 30%) he can leave it. Her death (which dissolves the Hex) always frees him; 0 = only that does. |

## The Vision of the Hex

Section `[hex_vision]`. When Wanda raises a Hex, a Vision appears to fight at her side (see [Scarlet Witch](Scarlet-Witch#the-vision-of-the-hex)).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `hex_vision.enabled` | `true` | `true` or `false` | If `true` (the default), a Hex brings a Vision to fight at Wanda's side while it stands. If `false`, no Hex does. |
| `hex_vision.health` | `80.0` | 1.0 to 1000.0 | Health of the Vision of the Hex (an Iron Golem has 100; 80 = 40 hearts). |
| `hex_vision.punchDamage` | `12.0` | 0.0 to 100.0 | Damage of one of his dense punches, about one a second (12 = 6 hearts). |
| `hex_vision.beamDamage` | `4.0` | 0.0 to 100.0 | Damage per hit of his Mind Stone beam, two hits a second. |
| `hex_vision.beamRange` | `16` | 2 to 64 | How far (in blocks) his Mind Stone beam reaches. Up close he punches instead. |
| `hex_vision.rebuildTicks` | `1200` | 20 to 72000 | How long, in ticks, Wanda needs to rebuild him after he falls, while the Hex stands (1200 = a minute). |

## Hex Runes

Section `[hex_runes]` (3.3). The page is [Scarlet Witch](Scarlet-Witch#hex-runes): from her Hex Control screen the Scarlet Witch inscribes runes on the four walls of her Hex, and while they glow every other sorcerer inside it (Agatha, Loki, Billy, another Scarlet Witch, and anyone holding the Darkhold or the Book of the Vishanti) cannot use powers.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `hex_runes.enabled` | `true` | `true` or `false` | The Hex Runes can be inscribed (false: the button does nothing and runes already up do nothing). |
| `hex_runes.cooldown` | `100` | 0 to 72000 | Ticks after inscribing the runes before she can inscribe them again (wiping them is free and at once). |
| `hex_runes.exhaustion` | `4.0` | 0.0 to 40.0 | Food exhaustion inscribing the runes costs (4.0 = 1 food point; Creative is free). On an empty stomach she cannot inscribe them. |

## Vision

Section `[vision]`. The hero's page is [Vision](Vision).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `vision.flightSpeed` | `0.08` | 0.01 to 0.5 | Vision's flying speed (Creative flight is 0.05). |
| `vision.mindStoneBeamDamage` | `6.0` | 0.0 to 100.0 | Damage per hit of the Mind Stone beam (two hits per second at most). The beam's range is `scarlet_witch.chaosBeamRange`. |

## Quicksilver

Section `[quicksilver]`. The hero's page is [Quicksilver](Quicksilver).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `quicksilver.slowMotionFactor` | `5` | 2 to 20 | During Quicksilver Time the world around him runs this many times slower (Tommy's Slow Time too, since 3.3). |
| `quicksilver.slowMotionRadius` | `32` | 4 to 128 | How far (in blocks) from Quicksilver time slows down (and from Tommy, in his Slow Time). |
| `quicksilver.speedBonus` | `3.0` | 0.0 to 10.0 | Extra movement speed while he runs in Quicksilver Time or Super Speed (3.0 = four times as fast). |
| `quicksilver.exhaustion` | `0.03` | 0.0 to 4.0 | Food exhaustion per tick while he runs in Quicksilver Time or Super Speed (4.0 = 1 food point). Quicksilver Time also costs half of this while he is not running. |
| `quicksilver.actionSpeed` | `4.0` | 1.0 to 10.0 | While Quicksilver Time or Super Speed is on he mines, attacks, eats, drinks, draws bows, loads crossbows, swims and places blocks this many times as fast (4.0 = four times as fast). |

## Billy (Wiccan)

Section `[billy]`. The hero's page is [Billy](Billy-and-the-Witches-Road). His block lifting (3.1), his Hex Beam and his Hex Barrier (3.2) are here; his Build Magic numbers are in [Build Magic](#build-magic).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `billy.flightSpeed` | `0.08` | 0.01 to 0.5 | Billy's flying speed (Creative flight is 0.05). |
| `billy.hexBoltDamage` | `6.0` | 0.0 to 100.0 | (Used when `billy.beam` is `false`.) Damage of a Hex Bolt to the creature it strikes. Each creature it forks to takes half. |
| `billy.freezeDuration` | `100` | 1 to 1200 | How long, in ticks, Billy's Freeze holds creatures spellbound (100 = 5 seconds). |
| `billy.noiseRange` | `32` | 4 to 64 | How far away (in blocks) Billy hears the thoughts of creatures while The Noise is on. |
| `billy.blockLifting` | `true` | `true` or `false` | Whether Billy's telekinesis tears blocks out of the ground and sets them down. If `false`: creatures, items and projectiles only (as in 3.0). |
| `billy.blockLiftRange` | `16` | 2 to 128 | How far away (in blocks) Billy can tear a block out of the ground. |
| `billy.blockLiftMaxHardness` | `3.0` | 0.0 to 100.0 | The hardest block Billy can lift (stone = 1.5, ores = 3, iron block = 5). |
| `billy.blockLiftCooldown` | `20` | 0 to 200 | Ticks between two blocks Billy tears out (catching anything else never waits). |
| `billy.beam` | `true` | `true` or `false` | 3.2: **R** is a continuous Hex Beam: held down, a beam leaves his hand along his look and hurts what the crosshair is on. If `false`: the old single Hex Bolt (`hexBoltDamage`), which forks to creatures near the one it struck. |
| `billy.beamDamagePerSecond` | `6.0` | 0.0 to 200.0 | Damage a second of the Hex Beam does to what the crosshair is on, at his first rank (6 = three hearts). It lands every `beamInterval` ticks, in equal parts, and grows with his Build Magic rank (`beamRankDamageBonus`). |
| `billy.beamRange` | `20` | 4 to 128 | How far (in blocks) the Hex Beam reaches at his first rank. It grows with his Build Magic rank (`beamRankRangeBonus`). |
| `billy.beamInterval` | `5` | 1 to 40 | Ticks between two hits of the Hex Beam (5 = four hits a second). The beam ignores the creature's damage cooldown, so every hit lands. |
| `billy.beamExhaustionPerSecond` | `0.8` | 0.0 to 40.0 | Food exhaustion a second of the Hex Beam costs (4.0 = 1 food point; the old bolt cost none). He cannot fire it on an empty stomach. |
| `billy.beamRankDamageBonus` | `1.0` | 0.0 to 10.0 | How much stronger the beam is at his highest rank (`build_magic_billy.maxRank`) than at his first, as a fraction: 1.0 = twice the damage. In between it grows evenly with the rank (rank 1: none). |
| `billy.beamRankRangeBonus` | `1.0` | 0.0 to 10.0 | The same for the range: 1.0 = twice as far at his highest rank. |
| `billy.barrier` | `true` | `true` or `false` | Sneak + **B**, held, raises Billy's Hex Barrier: a plate of blue magic in front of him that stops everything coming from the front (see `barrierArc`): projectiles, beams, blows, explosions and their push, lightning, grips, pulls and the attacks of bosses. 3.3: it has no time limit and costs no hunger; it has `barrierHealth`. If `false`, the key does nothing. |
| `billy.barrierHealth` | `60.0` | 1.0 to 1000.0 | 3.3.1: the health of the Hex Barrier. Only chaos magic (the Scarlet Witch's powers, the Chaos Beam) and dark magic (the Darkhold's, Agatha's) wear it down, each hit by what it would have done; everything else it stops without wearing. At 0 it shatters and cannot be raised for `barrierShatterCooldown` ticks. (A server file written by 3.3.0 still says `30.0`: the game never rewrites a value that is in the file; set it to `60.0` or delete the line.) It keeps what it has left while it is down and mends (`barrierRegenPerSecond`). |
| `billy.barrierRankHealthBonus` | `0.0` | 0.0 to 20.0 | How much more health the Hex Barrier has at his highest rank (`build_magic_billy.maxRank`) than at his first, as a fraction: 1.0 = twice the health. In between it grows evenly with the rank. 0 = `barrierHealth` at every rank. |
| `billy.barrierRegenPerSecond` | `1.5` | 0.0 to 100.0 | 3.3: health the Hex Barrier mends a second while it is lowered (1.5: from nothing to 60 in 40 seconds). It does not mend while it is raised. |
| `billy.barrierWindUpTicks` | `8` | 0 to 100 | How long (in ticks) the Hex Barrier takes to form after the key goes down. It stops nothing until it is complete. |
| `billy.barrierLowerCooldown` | `10` | 0 to 6000 | Ticks before **B** can be used again after the barrier was lowered (it was complete). (Called `barrierCooldown` in 3.2: renamed so the new default applies.) |
| `billy.barrierShatterCooldown` | `200` | 0 to 6000 | 3.3: ticks before the Hex Barrier (and **B**) can be used again after it shattered (200 = 10 seconds). |
| `billy.barrierArc` | `60.0` | 10.0 to 90.0 | Half the angle (in degrees) of the arc in front of him that the barrier covers: a blow, a projectile, a beam or an explosion from further round than this (from the side or from behind) is not stopped at all. |
| `billy.barrierWidth` | `1.4` | 0.5 to 6.0 | Width (in blocks) of the plate the barrier is drawn as. |
| `billy.barrierHeight` | `2.4` | 0.5 to 6.0 | Height (in blocks) of the plate. |
| `billy.barrierDistance` | `1.6` | 0.8 to 4.0 | How far (in blocks) in front of him the plate is held; projectiles are stopped when they reach it. |
| `billy.barrierSpeedFactor` | `0.6` | 0.1 to 1.0 | How fast he walks while the barrier is raised (1.0 = as usual, 0.6 = at three fifths of the speed). |

The 3.2 options `barrierStrength`, `barrierDuration`, `barrierRankStrengthBonus`, `barrierRankDurationBonus`, `barrierBrokenCooldown`, `barrierExhaustionPerSecond` and `barrierExplosionFactor` are gone in 3.3 (the barrier has health instead of strength, no time limit and no hunger, and stops all of an explosion).

## Build Magic

Sections `[build_magic]`, `[build_magic_wanda]` and `[build_magic_billy]`. The page is [Build Magic](Build-Magic). In a comment, an option of Billy's or Wanda's is always written with its section (`build_magic_billy.meter`), never as `billy.x`, which is the `[billy]` section of his other powers. Billy's numbers are those of his first rank, an Apprentice: the ranks grow from them. A family that wants him as strong as his mother sets the nine practice numbers to 0 and a large meter (`maxRank` is 10 already). Every value is read when it is used, so editing the file changes it live.

### `[build_magic]`: every builder

**The undo history** is written to the world folder before each spell changes the world (so a crash leaves it undoable), one folder for each player. While a spell works, its step is kept whole in memory at about 14 bytes a block; a spell whose step would not fit in `undoMemoryMb` (or half of `serverUndoMemoryMb`) is kept as a *lite step*, only a bit for each block in memory, and its records on disk are the step. That needs `undoSurvivesRestart` and a record that fits `undoDiskMb` (about half a byte a block); otherwise the spell says *"Too big to undo later"* and runs only if the player holds the right button, and without an undo. A Move is never lite: it is refused. At the defaults the line between whole and lite is about 4 million blocks. The caps are looked at once a second and when a spell ends. `/enhanced build forget` clears a player's.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `build_magic.enabled` | `true` | `true` or `false` | Build Magic exists on this server. |
| `build_magic.operatorsOnly` | `false` | `true` or `false` | Only operators (permission level 2) may use it. |
| `build_magic.requireMaterials` | `false` | `true` or `false` | If `false`: spells conjure their blocks from magic. If `true`: in Survival they take one item per block from the inventory, vanished blocks go into it, and undo gives back. Creative never pays. |
| `build_magic.millisPerTick` | `8` | 1 to 40 | Milliseconds of each server tick that all spells together may use (half while the server is slow). |
| `build_magic.blocksPerTick` | `20000` | 256 to 200000 | The most block writes all spells together may make in one tick (reads count a quarter). Every changed block is also sent to each player who sees it: on a server hosted at home or with a modest upload, lower this (5,000 keeps each watching player under about 0.4 MB a second). |
| `build_magic.sectionsPerTick` | `16` | 1 to 256 | The most 16×16×16 sections spells may start changing in one tick (bounds what clients download). |
| `build_magic.minSpellTicks` | `10` | 1 to 100 | A spell takes at least this long, so it is seen growing. |
| `build_magic.maxQueued` | `4` | 0 to 16 | Spells of one Scarlet Witch that may wait behind the one at work (Billy has none). |
| `build_magic.maxSpellsTotal` | `16` | 1 to 256 | Spells working or waiting on the whole server. |
| `build_magic.holdToConfirmBlocks` | `64` | 0 to 1000000 | A spell that removes or replaces more existing blocks than this needs the right button held (0 = every such spell). |
| `build_magic.holdToConfirmTicks` | `20` | 0 to 100 | How long that hold is. |
| `build_magic.hugeSpellBlocks` | `1000000` | 1000 to 2147483647 | A spell over this many blocks needs the longer hold. |
| `build_magic.hugeHoldTicks` | `40` | 0 to 200 | How long that hold is. |
| `build_magic.allArenasProtected` | `true` | `true` or `false` | No Build Magic spell and no Magnetic Pull changes a block in the arena of any running boss fight, Thanos's included. If `false`: only the protected arenas (and always the island in the sky). One switch for both, so the two never disagree about where they may change a block. |
| `build_magic.hexIsAClaim` | `false` | `true` or `false` | Inside a Hex, only its owner and the players she allowed may build with Build Magic or pull with the Magnetic Pull. (Telekinesis on one block asks the claims as a hand does, and is not covered.) 3.3: from **outside** a Hex, the Build Magic, block lifting and fire of anyone she has not allowed never reach inside it, whatever this says; this option extends the rule to builders who stand inside. |
| `build_magic.fullContainers` | `SKIP` | `SKIP` or `SPILL` | A container that holds items in a spell's way: `SKIP` leaves it as it is, `SPILL` spills its items on the spot. A Move always carries it. |
| `build_magic.liftCreatures` | `true` | `true` or `false` | Creatures inside new blocks are lifted on top (other players only with PvP on; never a creature Mjolnir pins). If `false`, their blocks stay empty. |
| `build_magic.moveCarriesCreatures` | `true` | `true` or `false` | Move takes the creatures, frames, paintings and armor stands inside along (not one Mjolnir pins). |
| `build_magic.holdGravityBlocks` | `true` | `true` or `false` | Sand, gravel and other falling blocks a spell places in the air stay where magic put them until something touches them. |
| `build_magic.postEventsPerBlock` | `true` | `true` or `false` | Ask claim mods (the break and place events) for every block. If `false`: once per 16×16×16 section. |
| `build_magic.previewChecks` | `8192` | 0 to 65536 | Blocks the server checks to preview a spell (more: a spread sample). 0 = no marks. |
| `build_magic.undoSurvivesRestart` | `true` | `true` or `false` | The undo history and the pocket are kept on disk with the world (`data/enhanced/build/<player id>/`). If `false`, they live in memory only: a restart loses them, and a history is dropped 30 minutes after its player leaves. |
| `build_magic.undoMemoryMb` | `64` | 4 to 4096 | Memory (MiB) one player's undo history may use; past it the oldest spells leave memory (they stay on disk and are read back when undone). |
| `build_magic.undoDiskMb` | `512` | 0 to 1048576 | Disk (MiB) one player's undo history may use; past it the oldest spells are forgotten (the newest never is). |
| `build_magic.serverUndoMemoryMb` | `512` | 64 to 65536 | Memory (MiB) all undo histories together may use; past it, the spells of the player using the most leave memory (they stay on disk). |
| `build_magic.serverUndoDiskMb` | `4096` | 0 to 4194304 | Disk (MiB) all undo histories together may use; past it the oldest spell of the player using the most is forgotten. |
| `build_magic.pocketMaxMb` | `64` | 1 to 4096 | The biggest copy (compressed, in MiB) one player may hold. |
| `build_magic.finishOnStopBlocks` | `200000` | 0 to 10000000 | When the server stops, the spells still working are finished while their blocks left add up to at most this; the largest are stopped first (and can be undone). |
| `build_magic.maxTicketChunks` | `256` | 16 to 4096 | Chunks one spell keeps loaded at once. |
| `build_magic.maxRequestsPerSecond` | `40` | 5 to 200 | Build messages a client may send per second; more are dropped. |
| `build_magic.deniedDimensions` | `[]` | a list of dimension ids | Dimensions where Build Magic does not work (ids, for example `"minecraft:the_end"`). |

### `[build_magic_wanda]`: the Scarlet Witch

Her magic has no gameplay limit: what is here is technical safety, and a few switches for big public servers.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `build_magic_wanda.enabled` | `true` | `true` or `false` | The Scarlet Witch has Build Magic. |
| `build_magic_wanda.reach` | `256` | 16 to 1024 | How far (in blocks) she points: anchors, shapes, picks. Her spells themselves have no size limit. |
| `build_magic_wanda.undoSteps` | `0` | 0 to 100000 | 0 = as many spells as `build_magic.undoMemoryMb` and `undoDiskMb` hold. Otherwise the most she can undo. |
| `build_magic_wanda.maxBlocks` | `0` | 0 to 2147483647 | 0 = no ceiling. Otherwise the most blocks one of her spells may cover (for big public servers). |
| `build_magic_wanda.copiesContents` | `true` | `true` or `false` | Her copies keep what is inside chests and other containers. |
| `build_magic_wanda.volatilePerSpell` | `4096` | 0 to 1000000 | The most TNT, lava and fire blocks one of her spells may make. |
| `build_magic_wanda.hexBoost` | `2.0` | 1.0 to 8.0 | Inside her own Hex her spells get this many shares of the server's work. |
| `build_magic_wanda.magicSelectMax` | `100000` | 1000 to 1000000 | The most blocks her double click selects (a selection is held block by block, so it stops at a million). |
| `build_magic_wanda.brushMax` | `16` | 1 to 64 | Her biggest Paint, Land and Grow brush (radius). Technical, not a limit of her magic: a brush pulse every 4 ticks of radius 16 is about 17,000 blocks, which the server keeps up with while she sweeps; at 32 (137,000) the stroke would trail seconds behind the cursor. |
| `build_magic_wanda.shapeMaxRadius` | `512` | 8 to 1024 | Her biggest shape (radius). |
| `build_magic_wanda.repeatMax` | `0` | 0 to 65536 | 0 = no count, only the spell's blocks. Otherwise the most copies in a Repeat. |
| `build_magic_wanda.instantMaxBlocks` | `250000` | 0 to 100000000 | Her Instant level (3.2): while its switch is on in build mode, a spell, an undo or a redo of at most this many blocks is written all at once, in the tick it is cast (no growing, no waiting). A bigger one goes at normal speed and she is told. 0 = no Instant level at all. Every rule still holds: arenas, claims, Hexes and loaded chunks only. The server stands still while it writes: 250,000 blocks take about a second, a million about four (with the undo kept on disk), so raise this only on a strong server. |
| `build_magic_wanda.instantMaxMillis` | `5000` | 100 to 50000 | The longest time (in milliseconds) one Instant spell may hold the server's tick (never more than a third of the server's `max-tick-time`, which kills a server stuck for longer). When the time is up, the blocks left are written at normal speed over the next ticks and she is told. |
| `build_magic_wanda.instantTickMillis` | `2000` | 100 to 50000 | The longest time (in milliseconds) ALL Instant spells together may hold ONE server tick: the casts, undos and redos of every Scarlet Witch that arrive in the same tick share it (so an autoclicker or two Wandas cannot pass the watchdog). When it is spent, the next Instant spell of that tick goes at normal speed and she is told. Never more than `instantMaxMillis` for one spell. |

### `[build_magic_billy]`: Billy, and how he learns

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `build_magic_billy.enabled` | `true` | `true` or `false` | Billy has Build Magic. |
| `build_magic_billy.meter` | `512` | 16 to 65536 | His magic meter (in blocks) as an Apprentice; it doubles with each rank to rank 4, and reaches 64 times as much at rank 10. |
| `build_magic_billy.refill` | `16.0` | 0.0 to 4096.0 | Blocks of magic he gets back each second as an Apprentice; half of it more each rank to rank 4, 12 times as much at rank 10. |
| `build_magic_billy.maxSide` | `24` | 1 to 256 | The longest side of anything he builds, copies or selects as an Apprentice; +8 each rank (96 at rank 10). |
| `build_magic_billy.reach` | `24` | 2 to 128 | How far (in blocks) he points as an Apprentice; +8 each rank (96 at rank 10). |
| `build_magic_billy.undoSteps` | `10` | 1 to 100 | Spells he can undo as an Apprentice; +2 each rank (28 at rank 10). |
| `build_magic_billy.repeatMax` | `8` | 1 to 64 | The most copies in a Repeat as an Apprentice; +4 each rank (44 at rank 10). |
| `build_magic_billy.rank2Practice` | `2000` | 0 to 10000000 | Blocks of magic he must cast to become a Spellcaster (rank 2). |
| `build_magic_billy.rank3Practice` | `8000` | 0 to 10000000 | Blocks of magic he must cast to become a Sorcerer (rank 3). |
| `build_magic_billy.rank4Practice` | `30000` | 0 to 10000000 | Blocks of magic he must cast to become Wiccan (rank 4). |
| `build_magic_billy.rank5Practice` | `60000` | 0 to 10000000 | Blocks of magic he must cast to become an Enchanter (rank 5). |
| `build_magic_billy.rank6Practice` | `120000` | 0 to 10000000 | Blocks of magic he must cast to become a Mystic (rank 6). |
| `build_magic_billy.rank7Practice` | `220000` | 0 to 10000000 | Blocks of magic he must cast to become a Wizard (rank 7). |
| `build_magic_billy.rank8Practice` | `380000` | 0 to 10000000 | Blocks of magic he must cast to become an Archmage (rank 8). |
| `build_magic_billy.rank9Practice` | `600000` | 0 to 10000000 | Blocks of magic he must cast to become a Reality Weaver (rank 9). |
| `build_magic_billy.rank10Practice` | `1000000` | 0 to 10000000 | Blocks of magic he must cast to become the Chaos Master (rank 10). |
| `build_magic_billy.maxRank` | `10` | 1 to 10 | The highest rank he can reach (1 = he never grows; 10 = the Chaos Master). Ranks 1 to 4 are those of 3.1. **A server file written by 3.1 still says `4`** (the game never rewrites a value that is in the file): set it to 10 to have all ten ranks. It also sets how far the Hex Beam, the Hex Barrier and the ward grow with his rank (with 4, rank 4 is the full bonus). |
| `build_magic_billy.castPause` | `10` | 0 to 200 | Ticks after one of his spells before the next. |
| `build_magic_billy.momRange` | `16` | 0 to 128 | How near (in blocks) a Scarlet Witch must be to help him (0 = never). |
| `build_magic_billy.momCostFactor` | `0.5` | 0.0 to 1.0 | What his spells cost near her (0.5 = half). |
| `build_magic_billy.copiesContents` | `false` | `true` or `false` | His copies keep what is inside containers (if `false`, they come out empty, so nothing is duplicated). |
| `build_magic_billy.limitsInCreative` | `true` | `true` or `false` | His limits apply in Creative too. |

## Tommy (Speed)

Section `[tommy]`. The hero's page is [Tommy](Tommy).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `tommy.speedBonus` | `2.5` | 0.0 to 10.0 | Extra movement speed while Tommy runs at super speed (2.5 = three and a half times as fast). |
| `tommy.exhaustion` | `0.02` | 0.0 to 4.0 | Food exhaustion per tick while Tommy runs at super speed (4.0 = 1 food point). |

## Loki

Section `[loki]`. The hero's page is [Loki](Loki).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `loki.daggerDamage` | `5.0` | 0.0 to 100.0 | Damage of one of Loki's conjured daggers (5 = 2.5 hearts). |
| `loki.scepterConfusionTicks` | `120` | 0 to 6000 | How long a creature stays confused after a melee hit with Loki's Scepter, in ticks (120 = 6 seconds). 0 turns it off. |
| `loki.scepterNauseaTicks` | `160` | 0 to 6000 | How long a player gets Nausea after a melee hit with Loki's Scepter, in ticks (160 = 8 seconds). 0 turns it off. |
| `loki.scepterSlownessTicks` | `60` | 0 to 6000 | How long a player gets Slowness I after a melee hit with Loki's Scepter, in ticks (60 = 3 seconds). 0 turns it off. |
| `loki.bodyHealthBonus` | `10.0` | 0.0 to 100.0 | 3.5: extra maximum health while a player is Loki (10 = five more hearts: 30 in all). |
| `loki.bodyArmor` | `6.0` | 0.0 to 30.0 | 3.5: Loki's armour points (the Frost Giant adds 4 on top). |
| `loki.bodyToughness` | `2.0` | 0.0 to 20.0 | 3.5: Loki's armour toughness. |

## Thor (Mjolnir, Stormbreaker and the Bifrost)

Section `[thor]`. The hero's page is [Thor](Thor).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thor.flightSpeed` | `0.11` | 0.01 to 0.5 | Thor's flying speed while he holds Mjolnir or Stormbreaker (Creative flight is 0.05). |
| `thor.anyoneWorthy` | `false` | `true` or `false` | If `true`, everyone is worthy of Mjolnir. If `false` (the default), only Thor, Vision, Creative and Spectator players and those who have earned the *Hero of the Village* advancement are worthy: they can claim a Mjolnir nobody owns yet, and lift it. |
| `thor.mjolnirOwnerOnly` | `true` | `true` or `false` | If `true` (the default), each Mjolnir belongs to the first worthy player who lifts it, and only they can lift it from then on (Creative and Spectator players always can, without taking it). If `false`, Mjolnirs are never claimed: the old rule, where any worthy player can lift any Mjolnir. Stormbreaker is never bound to anyone. |
| `thor.throwDamage` | `10.0` | 0.0 to 100.0 | Damage of a thrown Mjolnir, both ways. |
| `thor.stormbreakerThrowDamage` | `12.0` | 0.0 to 100.0 | Damage of a thrown Stormbreaker, both ways (a fifth more than Mjolnir's). |
| `thor.throwRange` | `40` | 4 to 128 | How far (in blocks) a thrown Mjolnir or Stormbreaker flies before it turns back. |
| `thor.recallRange` | `64` | 4 to 256 | How far away (in blocks) Thor can call Mjolnir or Stormbreaker back from. |
| `thor.beamDamage` | `2.0` | 0.0 to 100.0 | Damage of one hit of Thor's lightning beam, bare-handed (2 = one heart). Mjolnir in hand multiplies it by `thor.beamWeaponMultiplier`, Stormbreaker by `thor.stormbreakerBeamMultiplier`, the God of Thunder by `thor.godOfThunderBeamMultiplier` on top. |
| `thor.beamInterval` | `5` | 1 to 40 | Ticks between two hits of the lightning beam (5 = four hits a second). The beam ignores the creature's damage cooldown, so every hit lands. |
| `thor.beamRange` | `24` | 4 to 128 | How far (in blocks) Thor's lightning beam reaches bare-handed. |
| `thor.beamWeaponRange` | `48` | 4 to 256 | How far (in blocks) the beam reaches with Mjolnir in hand. |
| `thor.stormbreakerBeamRange` | `58` | 4 to 256 | How far (in blocks) the beam reaches with Stormbreaker in hand (a fifth farther than Mjolnir's). |
| `thor.beamWeaponMultiplier` | `2.0` | 1.0 to 20.0 | What Mjolnir in hand multiplies the beam's damage by. (With Mjolnir or Stormbreaker in hand the first hit on each creature also has the usual effects of lightning on it.) |
| `thor.stormbreakerBeamMultiplier` | `2.4` | 1.0 to 20.0 | What Stormbreaker in hand multiplies the beam's damage by (a fifth more than Mjolnir's). |
| `thor.godOfThunderBeamMultiplier` | `1.5` | 1.0 to 20.0 | What the God of Thunder multiplies the beam's damage by. Every hit also arcs to the next two creatures nearby. |
| `thor.beamExhaustion` | `0.03` | 0.0 to 4.0 | Food exhaustion per tick while the lightning beam is fired (4.0 = 1 food point). |
| `thor.godOfThunderExhaustion` | `0.04` | 0.0 to 4.0 | Food exhaustion per tick while the God of Thunder is unleashed (4.0 = 1 food point). |
| `thor.bifrostCooldown` | `1200` | 0 to 72000 | Cooldown of the Bifrost opened with a key (Thor's, Heimdall's, or anyone's who wields Mjolnir, Stormbreaker, a Hofund or his own Gungnir), in ticks (1200 = 60 seconds). Only the one who opened it pays it. Who travels along is the [`[bifrost]`](#the-bifrost) section (3.5). |
| `thor.shatterCooldown` | `300` | 0 to 72000 | Cooldown of shattering Mjolnir (Sneak + R), in ticks, from the moment it shatters (300 = 15 seconds). |
| `thor.shatterDamage` | `6.0` | 0.0 to 100.0 | Damage of each fragment of a shattered Mjolnir to the enemy it strikes. |
| `thor.pinDuration` | `1200` | 20 to 72000 | How long (in ticks) Mjolnir pins someone down before it lets go by itself (1200 = a minute). |

## Phoenix (Jean Grey)

Section `[phoenix]`. The hero's page is [Phoenix](Phoenix). Her telekinesis has no options of its own: it uses `magneto.telekinesisRange`, `magneto.throwSpeed` and `scarlet_witch.blockLift*`.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `phoenix.flightSpeed` | `0.09` | 0.01 to 0.5 | The Phoenix's flying speed (Creative flight is 0.05). |
| `phoenix.fireImmune` | `true` | `true` or `false` | If `true` (the default), fire, lava and burning cannot hurt the Phoenix (damage that goes through everything still does). |
| `phoenix.mindReadRange` | `24` | 4 to 64 | How far (in blocks) Mind Reading reaches. It needs a line of sight. |
| `phoenix.mindReadPlayers` | `true` | `true` or `false` | If `false`, Mind Reading cannot read players' minds (the panel says the server keeps them private). |
| `phoenix.mindReadInventory` | `true` | `true` or `false` | If `true` (the default) the panel lists a player's whole inventory; if `false`, only the hands and the armour. |
| `phoenix.mindReadNotifies` | `true` | `true` or `false` | If `true` (the default), the player who is read feels a presence in their mind (once per 10 seconds), so it is not a spy tool. |
| `phoenix.mindReadExhaustion` | `0.01` | 0.0 to 4.0 | Food exhaustion per tick while Mind Reading is on (4.0 = 1 food point). |
| `phoenix.mindSenseRange` | `48` | 8 to 128 | How far (in blocks) the Mind Sense ping reaches. |
| `phoenix.mindSenseTicks` | `120` | 20 to 1200 | How long (in ticks) the sensed minds glow on her screen (120 = 6 seconds). |
| `phoenix.mindSenseCooldown` | `400` | 0 to 72000 | Cooldown of Mind Sense (Sneak + R), in ticks (400 = 20 seconds). |
| `phoenix.mindSenseExhaustion` | `2.0` | 0.0 to 40.0 | Food exhaustion of one Mind Sense. |
| `phoenix.mindControlRange` | `10` | 2 to 32 | How far (in blocks) Mind Control reaches (10 = about 33 feet, as in the film). |
| `phoenix.mindControlTicks` | `300` | 20 to 2400 | How long (in ticks) a creature stays possessed (300 = 15 seconds). |
| `phoenix.mindControlMax` | `1` | 1 to 5 | How many creatures she can possess at once. Taking one more frees the oldest, who wakes confused. |
| `phoenix.mindControlPlayerTicks` | `60` | 0 to 1200 | How long (in ticks) Mind Control holds a player still (it needs PvP on). 0 turns it off for players. |
| `phoenix.mindControlCooldown` | `400` | 0 to 72000 | Cooldown of Mind Control (ability 4), in ticks (400 = 20 seconds). |
| `phoenix.mindControlExhaustion` | `6.0` | 0.0 to 40.0 | Food exhaustion of one Mind Control. |
| `phoenix.psionicRadius` | `16` | 4 to 64 | Radius (in blocks) of the Psionic Field (Sneak + B). It works through walls. |
| `phoenix.psionicTicks` | `60` | 10 to 600 | How long (in ticks) the Psionic Field holds everyone still (60 = 3 seconds; players only with PvP on). |
| `phoenix.psionicCooldown` | `1200` | 0 to 72000 | Cooldown of the Psionic Field, in ticks (1200 = 60 seconds). It shares ability slot 4 with Mind Control. |
| `phoenix.psionicExhaustion` | `8.0` | 0.0 to 40.0 | Food exhaustion of one Psionic Field. If it holds nobody it costs nothing and has no cooldown. |
| `phoenix.burstChargeTicks` | `60` | 10 to 200 | Ticks of charge for a full Phoenix Burst (60 = 3 seconds). It fires by itself at full charge. |
| `phoenix.burstMinChargeTicks` | `12` | 0 to 100 | A charge shorter than this (in ticks) is cancelled: no damage, no hunger, a short cooldown. |
| `phoenix.burstMinRadius` | `8` | 2 to 64 | Radius (in blocks) of a barely charged Burst. |
| `phoenix.burstMaxRadius` | `24` | 4 to 96 | Radius (in blocks) of a fully charged Burst. |
| `phoenix.burstMinDamage` | `8.0` | 0.0 to 200.0 | Damage of a barely charged Burst, at the centre. |
| `phoenix.burstMaxDamage` | `30.0` | 0.0 to 200.0 | Damage of a fully charged Burst, at the centre. It stays the same out to half the radius and falls to 60% at the edge. |
| `phoenix.burstPlayerFactor` | `0.5` | 0.0 to 1.0 | What a player takes of the Burst's damage (only with PvP on): 0.5 = half. |
| `phoenix.burstWaveSpeed` | `1.0` | 0.2 to 4.0 | How fast the wave spreads (blocks per tick). A creature 20 blocks away is hit 20 ticks after one at 1 block. |
| `phoenix.burstFireSeconds` | `10` | 0 to 60 | Seconds a creature burns after a full Burst (a weaker Burst burns 40% as long). |
| `phoenix.burstKnockback` | `1.2` | 0.0 to 5.0 | Knockback of the wave, away from the centre (none for creatures in `enhanced:pin_immune`). |
| `phoenix.burstSetsFire` | `true` | `true` or `false` | Whether the wave lights fires on flammable blocks (it also needs the `mobGriefing` game rule). It never breaks a block. |
| `phoenix.burstHurtsPassives` | `false` | `true` or `false` | If `true`, the wave also hurts villagers, Iron Golems, animals and other players' pets. By default it hurts only hostile creatures, creatures after her, the bosses and players (with PvP on). |
| `phoenix.burstMaxFireBlocks` | `40` | 0 to 400 | At most this many fires per Burst. |
| `phoenix.burstFoodCost` | `6` | 0 to 20 | Food points a full Burst costs, and the least hunger needed to start charging (a smaller charge costs less, at least 30%). |
| `phoenix.burstCooldown` | `1800` | 0 to 72000 | Cooldown of the Burst, in ticks (1800 = 90 seconds). |
| `phoenix.burstCancelCooldown` | `40` | 0 to 1200 | Cooldown after a cancelled charge, in ticks. |
| `phoenix.burstDrainTicks` | `100` | 0 to 1200 | How long (in ticks) Weakness and Slowness last after a Burst (none in Creative). |
| `phoenix.emberRitualTicks` | `60` | 20 to 400 | How long (in ticks) the Phoenix Ember must be held with right-click to awaken (60 = 3 seconds). |

## The speedsters

Section `[speedsters]` (3.3), shared by [Quicksilver](Quicksilver) and [Tommy](Tommy): running up walls and along ceilings, Tommy's Slow Time and the speed strikes (the Dash Strike of both, Quicksilver's Pebble Barrage, Tommy's Sonic Boom). Tommy's Slow Time uses Quicksilver's `quicksilver.slowMotionFactor` and `slowMotionRadius`.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `speedsters.wallRun` | `true` | `true` or `false` | Speedsters run up walls they sprint into at super speed (Quicksilver: Wall & Water Run on; Tommy: Wall Run on). False: they stop at walls as in 3.2. |
| `speedsters.ceilingRun` | `true` | `true` or `false` | A speedster running up a wall goes on along a ceiling or roof above it, and round the lip of an overhang. |
| `speedsters.surfaceSpeedFactor` | `0.8` | 0.1 to 2.0 | Speed on a wall or a ceiling, as a part of his speed on the ground (0.8 = four fifths). |
| `speedsters.surfaceMaxSpeed` | `1.0` | 0.1 to 3.0 | The fastest he runs on a wall or a ceiling, in blocks per tick (1.0 = 20 blocks a second). |
| `speedsters.surfaceMinSpeed` | `0.22` | 0.01 to 1.0 | Below this speed (blocks per tick) he can no longer hold a wall or a ceiling and drops off. |
| `speedsters.tommyTimeExhaustion` | `0.01` | 0.0 to 4.0 | Food exhaustion per tick of Tommy's slow time while he is not running (4.0 = 1 food point). (Running costs the exhaustion of his Super Speed instead.) |
| `speedsters.dashRange` | `12` | 2 to 32 | How far the Dash Strike (Sneak + G) carries him, in blocks. |
| `speedsters.dashDamage` | `5.0` | 0.0 to 100.0 | Damage of the Dash Strike to each creature in its path. |
| `speedsters.dashCooldown` | `100` | 0 to 72000 | Cooldown of the Dash Strike, in ticks. |
| `speedsters.barragePebbles` | `8` | 1 to 32 | How many pebbles Quicksilver's Pebble Barrage (N) flings. |
| `speedsters.barrageDamage` | `1.5` | 0.0 to 50.0 | Damage of each pebble (it ignores the hit cooldown, so pebbles add up). |
| `speedsters.barrageRange` | `24` | 4 to 64 | How far the pebbles fly, in blocks. |
| `speedsters.barrageCooldown` | `120` | 0 to 72000 | Cooldown of the Pebble Barrage, in ticks. |
| `speedsters.sonicBoomRadius` | `6.0` | 1.0 to 16.0 | Radius of Tommy's Sonic Boom (Sneak + V), in blocks. |
| `speedsters.sonicBoomDamage` | `4.0` | 0.0 to 100.0 | Damage of the Sonic Boom (half as much again when he was running at super speed). |
| `speedsters.sonicBoomCooldown` | `160` | 0 to 72000 | Cooldown of the Sonic Boom, in ticks. |

## Spider-Man

Section `[spider_man]`. The hero's page is [Spider-Man](Spider-Man). Ticks are 1/20 of a second, and "exhaustion" 4.0 is one food point. Every option is read when it is used, so editing the config file changes it live. There is no web fluid: hunger is the only resource.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `spider_man.speedBonus` | `0.15` | 0.0 to 2.0 | Extra walking speed of Spider-Man, as a fraction of the normal speed (0.15 = 15% faster). |
| `spider_man.jumpBonus` | `0.3` | 0.0 to 2.0 | Extra jump strength (0.3 is a jump of about 2.1 blocks instead of 1.25). |
| `spider_man.strengthBonus` | `2.0` | 0.0 to 20.0 | Extra melee damage (his bare fists hit for 3). |
| `spider_man.fallDamageReduction` | `0.5` | 0.0 to 1.0 | The part of fall damage he does not take (0.5 = half). |
| `spider_man.biteDelayTicks` | `140` | 20 to 1200 | How long (in ticks) the Radioactive Spider's bite takes to change a player (140 = a sick 7 seconds). |
| `spider_man.spiderSenseRange` | `12` | 4 to 48 | How far (in blocks) he feels danger coming. |
| `spider_man.spiderSenseInterval` | `5` | 1 to 40 | Ticks between two scans of his spider-sense for danger (5 = four times a second). |
| `spider_man.webShotRange` | `24` | 4 to 64 | Range (in blocks) of a Web Shot. |
| `spider_man.webShotCooldown` | `10` | 0 to 200 | Ticks between two Web Shots (hidden: the HUD shows only the Web Burst's cooldown). |
| `spider_man.webShotExhaustion` | `0.4` | 0.0 to 4.0 | Food exhaustion of one Web Shot (4.0 = 1 food point). |
| `spider_man.webbedTicks` | `120` | 20 to 1200 | How long (in ticks) a creature hit by a Web Shot stays webbed (120 = 6 seconds). |
| `spider_man.organicWebFactor` | `1.5` | 1.0 to 5.0 | Organic webs (the Brand New Day suit, or the Black Eyes) last this many times as long. |
| `spider_man.organicWebCostFactor` | `2.0` | 1.0 to 8.0 | Organic webs cost this many times the hunger. |
| `spider_man.webBlocks` | `true` | `true` or `false` | Whether webs also leave temporary web blocks (the cocoon round a creature, the patch on a wall). With `false`, webs only entangle creatures. |
| `spider_man.webBlockTicks` | `200` | 20 to 6000 | How long (in ticks) a web block lasts before it vanishes (200 = 10 seconds; organic webs last longer). It only ever replaces air. |
| `spider_man.webPatchRadius` | `1` | 0 to 2 | Radius of the patch a Web Shot leaves on a wall (1 = 3 by 3 blocks). |
| `spider_man.webBlockCap` | `64` | 8 to 512 | The most web blocks one Spider-Man can have at once; the oldest vanish first. |
| `spider_man.webBurstStrands` | `24` | 6 to 96 | Strands of a Web Burst (Sneak + R), spread evenly in every direction. |
| `spider_man.webBurstRange` | `10` | 3 to 24 | How far (in blocks) a strand of the burst flies. |
| `spider_man.webBurstCooldown` | `240` | 0 to 6000 | Cooldown of the Web Burst, in ticks (240 = 12 seconds). |
| `spider_man.webBurstExhaustion` | `2.0` | 0.0 to 8.0 | Food exhaustion of one Web Burst (2.0 = half a food point). |
| `spider_man.webBurstWebbedTicks` | `80` | 20 to 1200 | How long (in ticks) a Web Burst keeps a creature webbed (80 = 4 seconds). |
| `spider_man.zipRange` | `40` | 8 to 96 | How far (in blocks) a Web Zip reaches. |
| `spider_man.zipSpeed` | `1.3` | 0.3 to 3.0 | Speed of a zip in blocks per tick, before his speed factor (1.3 = 26 blocks a second). |
| `spider_man.zipCooldown` | `6` | 0 to 200 | Ticks before he can zip again (small, so zips chain). |
| `spider_man.zipMaxTicks` | `60` | 10 to 400 | The longest a zip can take, in ticks (60 = 3 seconds). |
| `spider_man.zipExhaustion` | `0.3` | 0.0 to 4.0 | Food exhaustion of one zip (4.0 = 1 food point). |
| `spider_man.zipKickDamage` | `4.0` | 0.0 to 40.0 | Damage of the kick when a zip aimed at a creature ends on it. 0 = no kick: the zip then goes only to blocks. |
| `spider_man.swingRange` | `40` | 8 to 96 | How far (in blocks) the anchor of a Web Swing can be. |
| `spider_man.swingMaxTicks` | `300` | 20 to 2400 | The longest a swing can last, in ticks (300 = 15 seconds). |
| `spider_man.swingMaxSpeed` | `1.8` | 0.5 to 3.0 | His top speed while swinging, in blocks per tick. |
| `spider_man.swingGravity` | `0.06` | 0.02 to 0.12 | Gravity while he swings, in blocks per tick squared (the normal one is 0.08). |
| `spider_man.swingExhaustion` | `0.01` | 0.0 to 1.0 | Food exhaustion per tick while he swings (4.0 = 1 food point; 0.01 is a food point every 20 seconds). |
| `spider_man.webAssist` | `true` | `true` or `false` | Whether Web Assist may be used: the swing chooses its own anchor ahead and above (trees count), keeps clear of the ground and the leaves, lets go at the best moment and swings on while G is held. Each Spider-Man switches it with the Web Assist key (J); it is on for him from the start. Off: every swing is manual. |
| `spider_man.webAssistOpenLand` | `sky` | `sky`, `zip` or `none` | What Web Assist does where there is nothing to swing from: "sky" (a web to the open sky ahead and above), "zip" (a short web boost forward and up) or "none". |
| `spider_man.webAssistRange` | `30` | 8 to 96 | How far (blocks) Web Assist looks for an anchor (never further than swingRange). |
| `spider_man.skyAnchorMaxTicks` | `60` | 10 to 400 | The longest a swing on a sky anchor lasts, in ticks. |
| `spider_man.webBoostCooldown` | `20` | 0 to 400 | Ticks between two web boosts (the "zip" of webAssistOpenLand). |
| `spider_man.crawlSpeed` | `0.22` | 0.05 to 0.6 | His speed on a wall or ceiling, in blocks per tick, before his speed factor (0.22 = 4.4 blocks a second). |
| `spider_man.crawlExhaustion` | `0.005` | 0.0 to 1.0 | Food exhaustion per tick while he moves on a wall or ceiling. |
| `spider_man.leapSpeed` | `0.9` | 0.3 to 2.0 | Speed of a leap off a wall, in blocks per tick, before his speed factor. |
| `spider_man.homemadeArmor` | `0.0` | 0.0 to 30.0 | Armor of the Homemade Suit (and of his own skin): a hoodie, no protection. |
| `spider_man.brandNewDayArmor` | `8.0` | 0.0 to 30.0 | Armor of the Brand New Day suit. |
| `spider_man.brandNewDayToughness` | `2.0` | 0.0 to 20.0 | Armor toughness of the Brand New Day suit. |
| `spider_man.ironSpiderArmor` | `14.0` | 0.0 to 30.0 | Armor of the Iron Spider. |
| `spider_man.ironSpiderToughness` | `6.0` | 0.0 to 20.0 | Armor toughness of the Iron Spider. |
| `spider_man.ironSpiderKnockbackResistance` | `0.2` | 0.0 to 1.0 | Knockback resistance of the Iron Spider (0.2 = 20%). |
| `spider_man.ironSpiderStripTicks` | `200` | 0 to 6000 | How long (in ticks) Magneto's Armor & Weapon Strip scatters the Iron Spider's nanites: its armor, its legs and Instant Kill are off meanwhile (200 = 10 seconds). |
| `spider_man.legsDamage` | `5.0` | 0.0 to 100.0 | Damage of one jab of the Iron Spider's legs. It is also all they do to a boss or a boss's minion, even in Instant Kill. |
| `spider_man.legsInterval` | `20` | 4 to 200 | Ticks between two jabs of the legs when they guard him (20 = once a second). |
| `spider_man.legsRange` | `4.0` | 1.0 to 10.0 | How far (in blocks) the legs jab a threat. |
| `spider_man.legsReachBonus` | `2.0` | 0.0 to 6.0 | Extra attack reach, in blocks, with the legs out. |
| `spider_man.legsExhaustion` | `0.01` | 0.0 to 1.0 | Food exhaustion per tick with the legs out. |
| `spider_man.instantKillRange` | `5.0` | 1.0 to 12.0 | How far (in blocks) Instant Kill reaches. |
| `spider_man.instantKillInterval` | `5` | 1 to 40 | Ticks between the strikes of Instant Kill; each time each of the four legs strikes one threat (5 = four times a second). |
| `spider_man.instantKillDamage` | `1000.0` | 1.0 to 10000.0 | Damage of one strike of Instant Kill (a kill). Bosses and their minions take only `legsDamage`. |
| `spider_man.instantKillMaxTicks` | `200` | 20 to 2400 | The longest Instant Kill lasts, in ticks (200 = 10 seconds). |
| `spider_man.instantKillCooldown` | `600` | 0 to 12000 | Cooldown in ticks after Instant Kill ends (600 = 30 seconds). |
| `spider_man.instantKillExhaustion` | `0.08` | 0.0 to 4.0 | Food exhaustion per tick while Instant Kill lasts (4.0 = 1 food point; the full 10 seconds cost about 4 points). |
| `spider_man.instantKillConfirmTicks` | `60` | 20 to 200 | The window, in ticks, to press Sneak + B a second time to confirm Instant Kill (60 = 3 seconds). |
| `spider_man.instantKillPlayers` | `false` | `true` or `false` | Whether Instant Kill may strike players. It never does by default, and PvP must be on as well. |
| `spider_man.blackEyesSpeedBonus` | `0.6` | 0.0 to 5.0 | Extra speed while the Black Eyes are on, as a fraction on top of his own (0.6 = 60% faster). |
| `spider_man.blackEyesStrengthBonus` | `4.0` | 0.0 to 40.0 | Extra melee damage while the Black Eyes are on (bare fists then hit for 7). |
| `spider_man.blackEyesAttackSpeedBonus` | `0.5` | 0.0 to 3.0 | Extra attack speed while the Black Eyes are on, as a fraction (0.5 = 50% faster). |
| `spider_man.blackEyesJumpBonus` | `0.25` | 0.0 to 2.0 | Extra jump strength while the Black Eyes are on, as a fraction. |
| `spider_man.blackEyesKnockbackResistance` | `0.3` | 0.0 to 1.0 | Knockback resistance while the Black Eyes are on (0.3 = 30%). |
| `spider_man.blackEyesExhaustion` | `0.05` | 0.0 to 4.0 | Food exhaustion per tick while the Black Eyes are on (0.05 = a food point every 4 seconds). |
| `spider_man.blackEyesMaxTicks` | `600` | 40 to 6000 | The longest the surge of the Black Eyes lasts, in ticks (600 = 30 seconds). |
| `spider_man.blackEyesCooldown` | `600` | 0 to 12000 | Cooldown in ticks after the surge ends (half of it when he calms himself). |
| `spider_man.blackEyesCrashTicks` | `200` | 0 to 2400 | How long (in ticks) the crash (Slowness II and Weakness) lasts when the surge runs out (200 = 10 seconds). A calm ending has no crash. |
| `spider_man.blackEyesPanicHealth` | `0.0` | 0.0 to 20.0 | If above 0, the Black Eyes switch on by themselves when his health falls to this many points (and can't be calmed for 5 seconds). 0 = never. |
| `spider_man.blackEyesSenseFactor` | `2.0` | 1.0 to 4.0 | How much the Black Eyes widen his spider-sense (and how much faster it scans). |

## Every boss fight

Section `[boss]`: the rules the three boss fights ([Thanos](Thanos), [Ultron](Ultron) and [Apocalypse](Apocalypse)) share, so that they behave alike. The numbers that belong to one boss are in his own sections below.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `boss.maxActiveFights` | `1` | 1 to 8 | How many boss fights may run at once on the whole server (Thanos, Ultron and Apocalypse together). A ritual that would start another one is refused. |
| `boss.wipeSeconds` | `15` | 1 to 600 | When every fighter is dead, the fight ends after this many seconds (the boss leaves, nobody is rewarded). |
| `boss.abandonSeconds` | `90` | 10 to 3600 | When no fighter has been near the boss for this many seconds, the fight ends and the boss leaves. |
| `boss.maxFightMinutes` | `25` | 1 to 240 | No fight lasts longer than this: after it the boss leaves (a fail-safe against a stuck fight). |
| `boss.rewardMinSeconds` | `60` | 0 to 3600 | A fighter earns the reward only after being in the fight at least this many seconds (and having hurt the boss; whoever lands the last blow earns it whatever the time). |
| `boss.creativeGetsReward` | `false` | `true` or `false` | If `true`, Creative players who fought earn the reward too. They never do by default. |
| `boss.xpRewardPerPlayer` | `1500` | 0 to 100000 | Experience points every rewarded fighter gets when the boss falls. |
| `boss.announceFights` | `true` | `true` or `false` | If `true` (the default), the server is told in chat when a boss is called and when it falls. |
| `boss.protectArena` | `true` | `true` or `false` | If `true` (the default), blocks in a boss's arena can't be broken, placed, burned or blown up while the fight lasts, so the world is the same when it ends. (Thanos's arena is never protected: he changes no block anyway.) The floating island of Ultron's fight is always protected: it is a copy of the ground, not the world. |
| `boss.difficultyScale` | `1.0` | 0.1 to 10.0 | Multiplies the health of every boss and of Ultron's Sentries, and the damage of Thanos, Ultron and the Sentries (1.0 = as designed, 0.5 = half as hard). Apocalypse's damage follows the game's difficulty instead. |
| `boss.telekinesisHoldsBosses` | `true` | `true` or `false` | Telekinesis (Wanda, Billy, the Phoenix, Magneto) can take hold of Ultron, Thanos and Apocalypse, as bosses: for a short time, then they break free. If `false`: they can't be held at all ("too powerful to lift", as in 3.0); Wanda's rip of Ultron's heart still works. |
| `boss.telekinesisHoldsVanillaBosses` | `true` | `true` or `false` | Telekinesis can take hold of the Wither, the Warden and the Elder Guardian, as bosses: for a short time, then they break free (the Ender Dragon is too vast to hold). If `false`: they are "too powerful to lift". |
| `boss.vanillaHoldTicks` | `60` | 10 to 600 | How long telekinesis holds the Wither, the Warden or the Elder Guardian before he breaks free (before the hero's factor, `billyHoldFactor` for Billy; Magneto holds none of the three: they wear no metal). |
| `boss.telekinesisCatchesBossShots` | `true` | `true` or `false` | Billy and the Phoenix (and Magneto, for metal) can catch Thanos's sword, meteors and orbs, and what they throw back is theirs (Ultron's bolts and Apocalypse's lances then hit their own side). If `false`: Thanos's shots can't be caught, and a caught bolt or lance stays the boss's (as in 3.0). |
| `boss.regripTicks` | `240` | 0 to 2400 | After a boss breaks free, is let go or lands from a throw, nobody can take hold of him again for this many ticks. |
| `boss.thrownStillTicks` | `20` | 0 to 40 | A boss thrown by telekinesis does nothing for at most this many ticks of his flight (his regrip time starts after it). |
| `boss.billyHoldFactor` | `0.5` | 0.1 to 2.0 | Billy holds a boss this many times as long as Wanda (bosses only: Thanos's sword he holds `thanos_boss.swordHoldTicks`, as the Phoenix does). |
| `boss.magnetoHoldFactor` | `1.5` | 0.1 to 3.0 | Magneto holds a metal boss, and every metal thing of a fight (Thanos's sword), this many times as long as Wanda. |
| `boss.lendArenaBlocks` | `true` | `true` or `false` | Block telekinesis can borrow the blocks of a protected arena: the holes mend, a borrowed block crumbles after a while, and everything goes back when the fight ends. If `false`: lifting is refused in the arenas (as in 3.0). |
| `boss.arenaMendTicks` | `100` | 20 to 1200 | Ticks after which the hole a borrowed block left mends. |
| `boss.borrowedBlockTicks` | `100` | 20 to 2400 | How long (in ticks) a borrowed block that is set down stands before it crumbles. |
| `boss.maxStandingPerPlayer` | `3` | 0 to 48 | At most this many borrowed blocks of one player stand at once; one more set down crumbles at once (0 = every borrowed block crumbles when it is set down). |
| `boss.maxBorrowedBlocks` | `48` | 1 to 512 | At most this many borrowed blocks (held, flying or standing) per fight. |

## Thanos (the boss)

Section `[thanos_boss]`. The boss's page is [Thanos](Thanos). The rules every boss shares (how many fights at once, the wipe and abandon times, the minimum time for a reward, the experience each rewarded fighter gets, the difficulty scale) are in `[boss]`. His own numbers are here. The hero's `[thanos]` section is below.

### The call

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos_boss.summonEnabled` | `true` | `true` or `false` | Whether the Tesseract and Loki's Scepter, held together, can call Thanos. |
| `thanos_boss.summonOverworldOnly` | `true` | `true` or `false` | Whether he answers only in the Overworld. |
| `thanos_boss.summonChargeTicks` | `100` | 20 to 100 | How long the two items are held up before the portal opens (100 is the longest they can be held: 5 seconds). |
| `thanos_boss.summonCountdownTicks` | `120` | 20 to 1200 | From the portal opening to his arrival (120 = 6 seconds). |
| `thanos_boss.summonSkyBlocks` | `24` | 4 to 64 | Clear air needed over the summoner's head. |
| `thanos_boss.summonOpenFraction` | `0.6` | 0.0 to 1.0 | The fraction of 24 sample spots round the summoner that need standing room. |
| `thanos_boss.summonCooldownTicks` | `12000` | 0 to 1728000 | The rest after a fight before he can be called again (12000 = 10 minutes; half after a wipe or an abandoned fight, none after the time limit or a stop). |

### The body, the clock and the arena

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos_boss.health` | `3000.0` | 100.0 to 100000.0 | His health for one fighter (before the shared `[boss].difficultyScale`). |
| `thanos_boss.healthPerExtraPlayer` | `0.5` | 0.0 to 2.0 | The fraction of his health added for each extra fighter. |
| `thanos_boss.maxFightersCounted` | `4` | 1 to 16 | How many fighters count for his health and for the targets of his area moves. |
| `thanos_boss.arenaRadius` | `40` | 16 to 96 | The radius of the fight round the summoning point, in blocks (the clock and the count of fighters reach 24 blocks beyond it). |
| `thanos_boss.xpReward` | `12000` | 0 to 100000 | The experience that falls where he stood when he is defeated. |
| `thanos_boss.maxStones` | `5` | 1 to 5 | The most Infinity Stones he can have. There is no sixth: the Mind Stone never comes. |
| `thanos_boss.stoneIntervalTicks` | `1800` | 200 to 36000 | Combat time between one stone and the next (1800 = 90 seconds). |
| `thanos_boss.surgeTicks` | `60` | 0 to 200 | The surge when a stone arrives: for this long he kneels and takes extra damage. |
| `thanos_boss.surgeDamageFactor` | `1.5` | 1.0 to 4.0 | The damage he takes in the surge, times. |
| `thanos_boss.damageScalePerStone` | `0.06` | 0.0 to 0.5 | The extra damage he deals for each stone beyond the first (0.06 = 6 percent). |
| `thanos_boss.enrageAfterTicks` | `3600` | 0 to 72000 | The time after his last stone before his damage starts to grow. |
| `thanos_boss.enrageStepPerMinute` | `0.04` | 0.0 to 0.5 | How much his damage grows each minute of enrage (0.04 = 4 percent). |
| `thanos_boss.enrageMax` | `0.40` | 0.0 to 2.0 | The most the enrage adds to his damage (0.4 = 40 percent). |
| `thanos_boss.gapPerExtraPlayer` | `2` | 0 to 10 | Ticks cut from his rest between moves for each extra fighter. |
| `thanos_boss.blockDamage` | `false` | `true` or `false` | If true, and the `mobGriefing` rule is on, Overload and the meteors break blocks with small explosions. Nothing else he does ever breaks a block. |

### His damage

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos_boss.swordDamage` | `30.0` | 0.0 to 400.0 | Damage: Sword Sweep. |
| `thanos_boss.smashDamage` | `26.0` | 0.0 to 400.0 | Damage: The impact of an Overhead Smash. |
| `thanos_boss.smashRingDamage` | `12.0` | 0.0 to 400.0 | Damage: The ground ring of an Overhead Smash. |
| `thanos_boss.jabDamage` | `14.0` | 0.0 to 400.0 | Damage: Each Gauntlet Jab (there are two). |
| `thanos_boss.swordThrowDamage` | `22.0` | 0.0 to 400.0 | Damage: Each pass of the thrown sword (out, and back). |
| `thanos_boss.chokeDamage` | `16.0` | 0.0 to 400.0 | Damage: The whole Choke hold (the throw at the end adds half again). |
| `thanos_boss.leapDamage` | `12.0` | 0.0 to 400.0 | Damage: The landing of a Leap. |
| `thanos_boss.powerBlastDamage` | `20.0` | 0.0 to 400.0 | Damage: The Power Blast beam. |
| `thanos_boss.powerBlastSplash` | `8.0` | 0.0 to 400.0 | Damage: The explosion at the end of the Power Blast. |
| `thanos_boss.overloadDamage` | `16.0` | 0.0 to 400.0 | Damage: Each sigil of Overload. |
| `thanos_boss.shockwaveDamage` | `12.0` | 0.0 to 400.0 | Damage: The Titan Shockwave ring. |
| `thanos_boss.meteorDamage` | `16.0` | 0.0 to 400.0 | Damage: Each meteor. |
| `thanos_boss.pullCrushDamage` | `10.0` | 0.0 to 400.0 | Damage: The crush at the end of the Gravity Pull. |
| `thanos_boss.soulDrainTick` | `3.0` | 0.0 to 400.0 | Damage: Each pulse of the Soul Drain (every 10 ticks; ignores armour and enchantments). |
| `thanos_boss.pulseDamage` | `12.0` | 0.0 to 400.0 | Damage: The Power Pulse that finds a fighter hiding behind walls. |
| `thanos_boss.infinityBeamDamage` | `40.0` | 0.0 to 400.0 | Damage: The Infinity Beam. |
| `thanos_boss.phantomDamage` | `3.5` | 0.0 to 400.0 | Damage: A jab of one of his illusions. |
| `thanos_boss.swordShieldFactor` | `2.0` | 1.0 to 10.0 | Damage: What his sword costs a Chaos Shield: this many times the damage it would have done. |

### His cooldowns (ticks)

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos_boss.swordCooldown` | `40` | 0 to 6000 | How long Sword Sweep waits before he can use it again. |
| `thanos_boss.smashCooldown` | `100` | 0 to 6000 | How long Overhead Smash waits before he can use it again. |
| `thanos_boss.jabCooldown` | `30` | 0 to 6000 | How long Gauntlet Jab waits before he can use it again. |
| `thanos_boss.swordThrowCooldown` | `280` | 0 to 6000 | How long Sword Throw waits before he can use it again. |
| `thanos_boss.chokeCooldown` | `400` | 0 to 6000 | How long Choke waits before he can use it again. |
| `thanos_boss.leapCooldown` | `160` | 0 to 6000 | How long Leap waits before he can use it again. |
| `thanos_boss.powerBlastCooldown` | `120` | 0 to 6000 | How long Power Blast waits before he can use it again. |
| `thanos_boss.overloadCooldown` | `200` | 0 to 6000 | How long Overload waits before he can use it again. |
| `thanos_boss.shockwaveCooldown` | `240` | 0 to 6000 | How long Titan Shockwave waits before he can use it again. |
| `thanos_boss.stepCooldown` | `160` | 0 to 6000 | How long Portal Step waits before he can use it again. |
| `thanos_boss.meteorCooldown` | `440` | 0 to 6000 | How long Meteor Hail waits before he can use it again. |
| `thanos_boss.pullCooldown` | `360` | 0 to 6000 | How long Gravity Pull waits before he can use it again. |
| `thanos_boss.barrierCooldown` | `300` | 0 to 6000 | How long Space Barrier waits before he can use it again. |
| `thanos_boss.veilCooldown` | `280` | 0 to 6000 | How long Reality Veil waits before he can use it again. |
| `thanos_boss.phantomsCooldown` | `560` | 0 to 6000 | How long Phantoms waits before he can use it again. |
| `thanos_boss.orbCooldown` | `240` | 0 to 6000 | How long Petrify Orb waits before he can use it again. |
| `thanos_boss.sightCooldown` | `240` | 0 to 6000 | How long Soul Sight waits before he can use it again. |
| `thanos_boss.drainCooldown` | `320` | 0 to 6000 | How long Soul Drain waits before he can use it again. |
| `thanos_boss.soulworldCooldown` | `800` | 0 to 6000 | How long Soul World waits before he can use it again. |
| `thanos_boss.rewindCooldown` | `600` | 0 to 6000 | How long Rewind waits before he can use it again. |
| `thanos_boss.snareCooldown` | `360` | 0 to 6000 | How long Time Snare waits before he can use it again. |
| `thanos_boss.infinityBeamCooldown` | `900` | 0 to 6000 | How long Infinity Beam waits before he can use it again. |
| `thanos_boss.pulseCooldown` | `80` | 0 to 6000 | How long Power Pulse (for each fighter) waits before he can use it again. |

### Durations and rules

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos_boss.barrierTicks` | `60` | 10 to 400 | How long the Space Barrier stands. |
| `thanos_boss.veilTicks` | `120` | 10 to 600 | How long his Reality Veil stands. |
| `thanos_boss.snareTicks` | `80` | 10 to 400 | How long the Time Snare locks whoever it catches. |
| `thanos_boss.soulworldTicks` | `80` | 10 to 400 | How long a soul stays in the Soul World. |
| `thanos_boss.soulworldHeal` | `0.03` | 0.0 to 0.5 | The fraction of his maximum health he heals for each soul taken into the Soul World. |
| `thanos_boss.drainHealFraction` | `0.4` | 0.0 to 2.0 | How much of the Soul Drain's damage he heals (0.4 = 40 percent). |
| `thanos_boss.rewindSeconds` | `6` | 1 to 20 | How far back his Rewind goes, in seconds. |
| `thanos_boss.rewindMaxHeal` | `0.08` | 0.0 to 0.5 | The most health a Rewind returns (a fraction of his maximum). |
| `thanos_boss.rewindTrigger` | `0.06` | 0.0 to 1.0 | The damage he took in that window that makes him rewind (a fraction of his maximum). |
| `thanos_boss.plateReduction` | `0.10` | 0.0 to 0.2 | The share of the damage each intact armour plate takes off (four plates: 0.1 means 40 percent). |
| `thanos_boss.plateTearSeconds` | `3` | 1 to 30 | Seconds of a Scarlet Witch's Chaos Beam on him that tear a plate away. |
| `thanos_boss.staggerPoise` | `100` | 10 to 1000 | The poise damage during a stone move's wind-up that breaks it and staggers him. |
| `thanos_boss.poiseHit` | `2.0` | 0.1 to 10.0 | Poise per point of damage he takes (three times that for the damage types in `enhanced:stone_born`: the Chaos Beam, Vision's Mind Stone beam and Stormbreaker). |
| `thanos_boss.lockTicks` | `80` | 10 to 400 | How long his stones stay dark after a stagger. |
| `thanos_boss.stormbreakerFactor` | `2.5` | 1.0 to 10.0 | The damage Stormbreaker does to him, times. |
| `thanos_boss.maxHitFraction` | `0.06` | 0.005 to 1.0 | The most one hit can take from him (a fraction of his maximum health). |
| `thanos_boss.maxDamagePerSecond` | `0.15` | 0.01 to 1.0 | The most one second can take from him (a fraction of his maximum health). |
| `thanos_boss.mobDamageFactor` | `0.25` | 0.0 to 1.0 | What golems, wolves and other creatures that are not players do to him, times. |
| `thanos_boss.meteorExtra` | `4` | 0 to 8 | Random meteors beyond one for each player. |
| `thanos_boss.unreachableTicks` | `160` | 40 to 1200 | How long a fighter can hide where he can neither walk nor see before the Power Pulse finds them. |
| `thanos_boss.idleRegenPerMinute` | `0.02` | 0.0 to 0.5 | His healing per minute while no fighter is in the arena (a fraction of his maximum). |
| `thanos_boss.rewardMinShare` | `0.03` | 0.0 to 0.5 | The damage (a fraction of his maximum health) that earns an Infinity Gauntlet, together with the shared `[boss].rewardMinSeconds`. Whoever strikes the last blow always earns one. |
| `thanos_boss.gauntletRitualTicks` | `60` | 20 to 400 | How long the Infinity Gauntlet must be held to become Thanos. |
| `thanos_boss.webLockTicks` | `40` | 0 to 400 | A Spider-Man's web locks his Gauntlet for this long (0 turns it off). |
| `thanos_boss.webLockCooldown` | `400` | 0 to 6000 | A web can lock his Gauntlet at most this often. |
| `thanos_boss.holdTicks` | `40` | 10 to 600 | How long telekinesis can hold Thanos before he breaks free (before the hero's factor, `boss.billyHoldFactor` or `boss.magnetoHoldFactor`). |
| `thanos_boss.swordHoldTicks` | `40` | 5 to 200 | How long telekinesis can hold his thrown sword before it tears free and flies home (every hero; Magneto `boss.magnetoHoldFactor` times as long). |

## Thanos (the hero)

Section `[thanos]`: the playable Thanos, whose page is [Playing Thanos](Thanos#playing-thanos). Ticks are 1/20 s. The *exhaustion* options are how much hunger an ability costs: 4.0 is one food point.

### The body

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos.healthBonus` | `20.0` | 0.0 to 100.0 | Extra health Thanos has (20 = 10 hearts). |
| `thanos.armorBonus` | `6.0` | 0.0 to 30.0 | Extra armour points. |
| `thanos.toughnessBonus` | `2.0` | 0.0 to 20.0 | Extra armour toughness. |
| `thanos.knockbackResistance` | `0.5` | 0.0 to 1.0 | Knockback resistance (1 = none at all). |
| `thanos.attackBonus` | `3.0` | 0.0 to 20.0 | Extra attack damage, with a bare fist or any weapon (Titan strength). |
| `thanos.scale` | `0.1` | 0.0 to 0.5 | Extra size (0.1 = 10 percent taller and wider). |
| `thanos.reachBonus` | `1.0` | 0.0 to 4.0 | Extra reach, in blocks (long arms). |
| `thanos.stepBonus` | `0.4` | 0.0 to 1.0 | Extra step height: a Titan's stride walks up whole blocks. |
| `thanos.regenPerSecond` | `0.5` | 0.0 to 10.0 | The Titan healing factor: health regained each second while unhurt. |
| `thanos.regenDelayTicks` | `100` | 0 to 1200 | Ticks without being hurt before the healing factor starts. |

### The Power Stone (R)

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos.powerBlastDamage` | `12.0` | 0.0 to 100.0 | Damage of the Power Blast. |
| `thanos.powerBlastSplash` | `6.0` | 0.0 to 100.0 | Damage of the explosion at the end of the Power Blast. |
| `thanos.powerBlastRange` | `28.0` | 4.0 to 64.0 | Range of the Power Blast, in blocks. |
| `thanos.powerBlastCooldown` | `70` | 0 to 6000 | Ticks before the Power Blast can be used again. |
| `thanos.powerBlastExhaustion` | `2.0` | 0.0 to 20.0 | Exhaustion of the Power Blast (4.0 = one food point). |
| `thanos.slamDamage` | `10.0` | 0.0 to 100.0 | Damage of the Titan Slam. |
| `thanos.slamRadius` | `7.0` | 2.0 to 24.0 | Radius of the Titan Slam's ring. |
| `thanos.slamCooldown` | `160` | 0 to 6000 | Ticks before the Titan Slam can be used again. |
| `thanos.slamExhaustion` | `4.0` | 0.0 to 20.0 | Exhaustion of the Titan Slam. |

### The Space Stone (G)

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos.spaceStepRange` | `40.0` | 4.0 to 128.0 | Range of the Portal Step, in blocks. |
| `thanos.spaceStepCooldown` | `100` | 0 to 6000 | Ticks before the Portal Step can be used again. |
| `thanos.stepExhaustion` | `1.0` | 0.0 to 20.0 | Exhaustion of the Portal Step. |
| `thanos.pullRange` | `24.0` | 4.0 to 64.0 | Range of the Space Pull, in blocks. |
| `thanos.pullCooldown` | `220` | 0 to 6000 | Ticks before the Space Pull can be used again. |
| `thanos.pullExhaustion` | `2.0` | 0.0 to 20.0 | Exhaustion of the Space Pull. |

### The Reality Stone (V)

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos.transmuteRange` | `20.0` | 4.0 to 64.0 | Range of the Transmute, in blocks. |
| `thanos.transmuteTicks` | `60` | 10 to 600 | How long a creature stays stone. |
| `thanos.transmuteCooldown` | `240` | 0 to 6000 | Ticks before the Transmute can be used again. |
| `thanos.transmuteExhaustion` | `2.0` | 0.0 to 20.0 | Exhaustion of the Transmute. |
| `thanos.petrifyDamageFactor` | `0.5` | 0.1 to 1.0 | The damage a petrified creature takes, times (it is stone). The boss's Petrify Orb does this too. |
| `thanos.veilRadius` | `8.0` | 2.0 to 32.0 | How close a projectile must come to be turned into a bubble by the Reality Veil. |
| `thanos.veilExhaustion` | `0.01` | 0.0 to 1.0 | Exhaustion of the Reality Veil each tick it is on. |

### The Soul Stone (B)

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos.drainRange` | `16.0` | 4.0 to 64.0 | Range of the Soul Drain, in blocks. |
| `thanos.drainDamagePerSecond` | `6.0` | 0.0 to 100.0 | Damage of the Soul Drain each second (it ignores armour and enchantments). |
| `thanos.drainHealFraction` | `0.5` | 0.0 to 2.0 | The share of the Soul Drain's damage that heals Thanos. |
| `thanos.drainExhaustion` | `0.02` | 0.0 to 1.0 | Exhaustion of the Soul Drain each tick. |
| `thanos.drainCooldown` | `40` | 0 to 6000 | Ticks before the Soul Drain can be used again after it ends. |
| `thanos.sightRange` | `32.0` | 4.0 to 64.0 | Range of the Soul Sight, in blocks. |
| `thanos.sightTicks` | `160` | 20 to 1200 | How long the Soul Sight lasts. |
| `thanos.sightCooldown` | `400` | 0 to 6000 | Ticks before the Soul Sight can be used again. |
| `thanos.sightBreaksIllusions` | `true` | `true` or `false` | Whether the Soul Sight dissolves Loki's duplicates and ends his disguises. |
| `thanos.sightExhaustion` | `1.0` | 0.0 to 20.0 | Exhaustion of the Soul Sight. |

### The Time Stone (N)

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thanos.rewindSeconds` | `5` | 1 to 15 | How far back the Rewind goes, in seconds. |
| `thanos.rewindMaxHeal` | `20.0` | 0.0 to 100.0 | The most health the Rewind returns. |
| `thanos.rewindCooldown` | `900` | 0 to 72000 | Ticks before the Rewind can be used again. |
| `thanos.rewindExhaustion` | `8.0` | 0.0 to 20.0 | Exhaustion of the Rewind. |
| `thanos.timeLockRadius` | `6.0` | 2.0 to 24.0 | Radius of the Time Lock's dome. |
| `thanos.timeLockTicks` | `100` | 10 to 600 | How long the Time Lock stands. |
| `thanos.timeLockCooldown` | `600` | 0 to 72000 | Ticks before the Time Lock can be used again. |
| `thanos.timeLockProjectileDrag` | `0.65` | 0.0 to 1.0 | The share of its speed a projectile loses each tick inside the Time Lock. |
| `thanos.timeLockExhaustion` | `3.0` | 0.0 to 20.0 | Exhaustion of the Time Lock. |

### Your own game: the music and the atmosphere of the fight

Two more options are about your own game only (they are not synced, and a server can't change them). They live in the client file, **`config/enhanced-client.toml`**, in a `[thanos]` section, next to the options of [Client options](#client-options-animations-effects-and-looks), and in the game under **Mods, Enhanced, Config**.

```toml
[thanos]
    bossMusic = true
    bossAtmosphere = true
```

| Option | Setting in the file | Default | What it does |
|---|---|---|---|
| Boss music | `bossMusic` | `true` | The music of the fight while a Thanos is near: five layers in D minor that play together, one for each stone he has (a drone and a heartbeat, then low strings, a high choir, a driving ostinato and bells), while the game's own music waits (it follows the music volume). Off: silence (the sounds of his moves stay). Since 3.5 it turns off [Hela's](Hela#the-music) music too. |
| Boss atmosphere | `bossAtmosphere` | `true` | The violet fog, the dark edges of the screen and the far thunder while a Thanos is near, and the violet haze of the call that wakes him. Off: the sky stays as it is. The grey of stone, the green of the Time Lock, the Soul World and the white flashes are what he does to you, so they always show. |

## The Ultron boss

Section `[ultron_boss]`. The boss and the hero share one page, [Ultron](Ultron); the hero's numbers are in the next section.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `ultron_boss.enabled` | `true` | `true` or `false` | Whether the Ultron ritual works at all. |
| `ultron_boss.bodyHealth` | `600.0` | 50.0 to 20000.0 | Health of the first body, Ultron Prime, for one player. (A creature can hold 1024 at most: for more, every blow is made smaller in proportion.) |
| `ultron_boss.bodyHealthDecay` | `0.8` | 0.3 to 1.0 | Each new body has this fraction of the last one's health. |
| `ultron_boss.bodyHealthFloor` | `0.25` | 0.05 to 1.0 | No body has less than this fraction of the first one's health. |
| `ultron_boss.healthPerExtraPlayer` | `0.35` | 0.0 to 2.0 | Extra health (as a fraction of the first body) for each participant after the first (at most 6 count). |
| `ultron_boss.bodyArmor` | `8.0` | 0.0 to 30.0 | Armour of Prime (a body later has less). |
| `ultron_boss.bodyToughness` | `4.0` | 0.0 to 20.0 | Armour toughness of Prime (a body later has less). |
| `ultron_boss.maxHitFraction` | `0.15` | 0.02 to 1.0 | The most a single hit can take from a body, as a fraction of its maximum health (before armour). |
| `ultron_boss.explosionDamageFactor` | `0.5` | 0.0 to 1.0 | Explosion damage to a body is multiplied by this. |
| `ultron_boss.liftAt` | `0.55` | 0.1 to 0.95 | Prime's health fraction at which the Sokovia phase starts. |
| `ultron_boss.heartRipThreshold` | `0.15` | 0.0 to 0.5 | A Scarlet Witch can tear a body's core out below this fraction of its health (0 turns it off). |
| `ultron_boss.volleyBoltDamage` | `6.0` | 0.0 to 100.0 | Damage of one Repulsor Volley bolt (Normal difficulty). |
| `ultron_boss.fingerBeamDamage` | `6.0` | 0.0 to 100.0 | Damage of one Finger Beams hit (Normal difficulty). |
| `ultron_boss.diveDamage` | `18.0` | 0.0 to 100.0 | Damage of the Thruster Dive's landing. |
| `ultron_boss.pullDamage` | `14.0` | 0.0 to 100.0 | Damage of the Gravity Pull's punch. |
| `ultron_boss.punchDamage` | `12.0` | 0.0 to 100.0 | Damage of the Punch. |
| `ultron_boss.debrisDamage` | `10.0` | 0.0 to 100.0 | Damage of a piece of falling Sokovia. |
| `ultron_boss.debrisInterval` | `100` | 20 to 1200 | Ticks between falling debris on the island, for every two participants. |
| `ultron_boss.sentryHealth` | `40.0` | 4.0 to 500.0 | Health of an Ultron Sentry of the fight. |
| `ultron_boss.sentryBoltDamage` | `4.0` | 0.0 to 100.0 | Damage of a Sentry's plasma bolt. |
| `ultron_boss.sentryMeleeDamage` | `6.0` | 0.0 to 100.0 | Damage of a Sentry's punch. |
| `ultron_boss.sentryStunTicks` | `60` | 0 to 600 | How long lightning stuns a Sentry. |
| `ultron_boss.sentryBudgetBase` | `20` | 4 to 200 | How many Sentries Ultron can still call, beyond the first ones, for one player. |
| `ultron_boss.sentryBudgetPerPlayer` | `8` | 0 to 50 | How many more Sentries Ultron can call for each extra player. |
| `ultron_boss.sentriesAtStart` | `6` | 0 to 40 | The Sentries beside him when he wakes. |
| `ultron_boss.sentryWaveSize` | `4` | 1 to 20 | How many Sentries come in a wave. |
| `ultron_boss.sentryWaveInterval` | `500` | 100 to 6000 | Ticks between waves of Sentries. |
| `ultron_boss.sentryMaxAlive` | `8` | 2 to 60 | How many Sentries may be alive at once, for one player. |
| `ultron_boss.sentryMaxAlivePerPlayer` | `3` | 0 to 20 | How many more Sentries may be alive for each extra player. |
| `ultron_boss.maxUploads` | `12` | 1 to 100 | Safety valve: after this many uploads Ultron is destroyed even if Sentries remain. |
| `ultron_boss.liftEnabled` | `true` | `true` or `false` | Whether the island rises at all; off means the Tremor (nothing moves). |
| `ultron_boss.liftMode` | `SKY_ISLAND` | `SKY_ISLAND`, `CUT_GROUND` or `TREMOR` | How the island is made: SKY_ISLAND (a copy built in the sky, the ground is never edited), CUT_GROUND (the real ground lifted and put back: reserved for a later release; in 3.0 it acts as TREMOR) or TREMOR (nothing moves). |
| `ultron_boss.hideGround` | `true` | `true` or `false` | Sky Island only: clients hide the columns the island was copied from while it is up (cosmetic; the server's ground never changes). |
| `ultron_boss.liftRequiresMobGriefing` | `true` | `true` or `false` | CUT_GROUND only: if the game rule mobGriefing is off, use the Tremor instead of lifting (no effect in 3.0, where CUT_GROUND is not built yet). |
| `ultron_boss.islandRadius` | `20` | 12 to 32 | Radius of the lifted ground, and of the ground fight before it. |
| `ultron_boss.liftHeight` | `40` | 16 to 80 | How far the island rises. |
| `ultron_boss.liftTicks` | `160` | 1 to 600 | How long the rise and the descent take, in ticks (1 = a cut). |
| `ultron_boss.skinDepth` | `5` | 3 to 8 | Layers of real ground that rise with the island. |
| `ultron_boss.coverHeight` | `16` | 8 to 24 | How far above the ground things are carried (grass, flowers, trees). |
| `ultron_boss.coneDepth` | `14` | 6 to 24 | The depth of the rock cone under the middle of the island. |
| `ultron_boss.maxRelief` | `6` | 2 to 12 | The most the ground may rise and fall inside the island's disc. |
| `ultron_boss.editsPerTick` | `2500` | 200 to 20000 | Block edits per tick while the island is built or taken down (lower it on a weak server). |
| `ultron_boss.rescueDamage` | `2.0` | 0.0 to 10.0 | Damage (never lethal) when the field pulls someone back onto the island. |
| `ultron_boss.rejoinLimit` | `3` | 0 to 20 | How many times a player may be taken up again by the pit's tractor. |
| `ultron_boss.visionBeamBonus` | `1.5` | 1.0 to 5.0 | The Mind Stone beam does this much to Ultron and the Sentries. |
| `ultron_boss.holdTicks` | `60` | 10 to 600 | How long telekinesis can hold a body of Ultron before he breaks free (before the hero's factor, `boss.billyHoldFactor` or `boss.magnetoHoldFactor`). |

## Ultron the hero

Section `[ultron]`. The lightning and Vision numbers also apply to the boss, its bodies and its Sentries.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `ultron.flightSpeed` | `0.10` | 0.01 to 0.5 | Ultron's flying speed (Creative flight is 0.05). |
| `ultron.bodyHealth` | `10.0` | 0.0 to 40.0 | Extra maximum health of the vibranium body. |
| `ultron.bodyArmor` | `8.0` | 0.0 to 30.0 | Extra armour of the vibranium body. |
| `ultron.knockbackResistance` | `0.3` | 0.0 to 1.0 | Extra knockback resistance of the vibranium body. |
| `ultron.lightningFactor` | `1.25` | 1.0 to 3.0 | Lightning does this much to Ultron, his bodies and the Sentries (they are electronics). |
| `ultron.visionCutRange` | `48` | 0 to 128 | A Vision this near cuts Ultron's uploads, the boss's and the hero's (0 turns it off). |
| `ultron.boltDamage` | `4.0` | 0.0 to 100.0 | Damage of a plasma bolt. |
| `ultron.boltSplash` | `1.2` | 0.0 to 4.0 | The radius in which others near the bolt's target take half. |
| `ultron.boltInterval` | `4` | 1 to 20 | Ticks between bolts while R is held. |
| `ultron.boltPool` | `30` | 5 to 500 | The plasma pool: how many bolts he can fire before he is empty. |
| `ultron.boltRegenPerSecond` | `4.0` | 0.0 to 100.0 | How fast the plasma pool refills, per second (after half a second without firing). |
| `ultron.boltRestart` | `6` | 1 to 100 | How much plasma he needs to fire again once the pool is empty. |
| `ultron.overchargeDamage` | `14.0` | 0.0 to 200.0 | The Overcharge's hit. |
| `ultron.overchargeSplash` | `8.0` | 0.0 to 200.0 | The Overcharge's splash damage. |
| `ultron.overchargeRadius` | `3.5` | 0.5 to 10.0 | The Overcharge's splash radius. |
| `ultron.overchargeCost` | `10` | 0 to 100 | How much plasma the Overcharge costs. |
| `ultron.overchargeChargeTicks` | `15` | 1 to 100 | How long the Overcharge charges. |
| `ultron.overchargeSlow` | `0.6` | 0.0 to 0.95 | How much slower he moves while he charges (0.6 = 40% of his speed). |
| `ultron.overchargeCooldown` | `160` | 0 to 6000 | The Overcharge's cooldown, in ticks. |
| `ultron.pullRange` | `24` | 4 to 64 | Gravity Pull: how far it reaches. |
| `ultron.pullDamage` | `3.0` | 0.0 to 100.0 | Gravity Pull: its damage. |
| `ultron.pullCooldown` | `80` | 0 to 6000 | Gravity Pull: its cooldown, in ticks. |
| `ultron.slamRadius` | `7.0` | 2.0 to 24.0 | Gravity Slam: its radius. |
| `ultron.slamDamage` | `6.0` | 0.0 to 100.0 | Gravity Slam: its damage. |
| `ultron.slamCooldown` | `160` | 0 to 6000 | Gravity Slam: its cooldown, in ticks. |
| `ultron.uploadRange` | `48` | 8 to 128 | Upload: how near a drone must be. |
| `ultron.uploadHeal` | `8.0` | 0.0 to 40.0 | Upload: how much health it heals. |
| `ultron.uploadCooldown` | `500` | 0 to 12000 | Upload: its cooldown, in ticks. |
| `ultron.secondLifeCooldown` | `6000` | 0 to 72000 | Second Life: its recharge, in ticks. |
| `ultron.secondLifeHealth` | `0.25` | 0.05 to 1.0 | Second Life: the health he comes back with, as a fraction of the maximum. |
| `ultron.secondLifeRange` | `64` | 8 to 256 | Second Life: how near a drone must be. |
| `ultron.hackRange` | `32` | 4 to 64 | Hack: how far it reaches. |
| `ultron.hackMax` | `2` | 0 to 6 | Hack: how many hacked Sentries he can have at a time. |
| `ultron.droneCount` | `3` | 1 to 6 | Sentry Army: how many Sentries one use calls. |
| `ultron.droneMax` | `6` | 1 to 12 | Sentry Army: the most drones he can have at a time. |
| `ultron.droneLifetime` | `1800` | 100 to 72000 | Sentry Army: how long a drone lasts, in ticks. |
| `ultron.droneCooldown` | `400` | 0 to 12000 | Sentry Army: its cooldown, in ticks. |
| `ultron.droneHealth` | `24.0` | 4.0 to 200.0 | A drone's health. |
| `ultron.droneDamage` | `3.0` | 0.0 to 100.0 | A drone's plasma bolt. |

## Apocalypse (the boss, his shield and the hero)

Sections `[apocalypse_shield]`, `[apocalypse_boss]` and `[apocalypse]`. **3.2: the master factor.** `apocalypse_boss.powerFactor` (0.65 by default) multiplies every damage he and his Horsemen deal and the numbers of his lances, Horsemen and tornadoes (rounded up, never below one), and it divides the speed of his attacks (the pause between two attacks and every cooldown last 1/powerFactor times as long). It does not touch his health, armour, shield, windows, the sand wall or the hero. With 1.0 he is the Apocalypse of 3.1 in everything it multiplies. The damage options below are the numbers before the factor. The fight itself and the hero are described in the Apocalypse page; the numbers shared by every boss (how many fights run at once, the wipe and abandon clocks, the reward's minimum time, the experience, the time limit, the protection of the arena) are in `[boss]`.

### `[apocalypse_shield]`: what counts as mind power

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `apocalypse_shield.mindPowerHeroes` | `["scarlet_witch", "phoenix"]` | a list of power names | Heroes whose mind power can wear down Apocalypse's shield (the boss's and the hero's). Add `"billy"` or `"vision"` to count theirs (and add their damage types to the datapack tag `enhanced:mind_power`: `enhanced:hex_bolt` and `enhanced:hex_beam` for Billy, `enhanced:mind_stone_beam` for Vision). |
| `apocalypse_shield.chaosBeamFactor` | `1.0` | 0.0 to 20.0 | Shield strength removed per point of Chaos Beam damage. |
| `apocalypse_shield.slamFactor` | `1.0` | 0.0 to 20.0 | Per point of telekinetic slam damage (a thing thrown by Wanda or the Phoenix). |
| `apocalypse_shield.borrowedSlamFactor` | `0.5` | 0.0 to 20.0 | A block borrowed from his own arena and thrown by mind power wears the shield by this many times what `slamFactor` gives. The critical factor comes on top. |
| `apocalypse_shield.phoenixBurstFactor` | `5.0` | 0.0 to 50.0 | Per point of Phoenix Burst damage (a full Burst of 30 removes 150). |
| `apocalypse_shield.otherMindFactor` | `1.0` | 0.0 to 20.0 | Per point of any other damage type in `enhanced:mind_power`. |
| `apocalypse_shield.phoenixMindControlStrain` | `25.0` | 0.0 to 1000.0 | Shield strength a Phoenix's Mind Control (even refused) removes. |
| `apocalypse_shield.phoenixPsionicStrain` | `60.0` | 0.0 to 1000.0 | Shield strength a Phoenix's Psionic Field that reaches him removes. |
| `apocalypse_shield.shieldRegenDelayTicks` | `200` | 0 to 2400 | The boss's shield regenerates only after this many ticks without mind-power damage. |
| `apocalypse_shield.shieldRegenFraction` | `0.02` | 0.0 to 1.0 | Fraction of its strength it regains each second. |
| `apocalypse_shield.mindHitCooldownTicks` | `10` | 1 to 40 | Ticks between two mind-power hits from the same attacker that the shield will count (the Chaos Beam hits every tick; a shield leaves no invulnerability frames, so it keeps its own). |
| `apocalypse_shield.critHeroes` | `["scarlet_witch", "phoenix"]` | a list of power names | Heroes whose mind power hits the boss Apocalypse critically, on his shield and on his body (only the boss, never the Apocalypse hero). Kept apart from `mindPowerHeroes`: a hero added there doesn't get critical hits too. |
| `apocalypse_shield.critShieldFactor` | `2.0` | 1.0 to 10.0 | Their mind power (and the Phoenix's Mind Control and Psionic Field strains) wears the boss's shield this many times as much. |
| `apocalypse_shield.critBodyFactor` | `2.0` | 1.0 to 10.0 | Their mind power hurts the boss's body this many times as much when his shield is down. |
| `apocalypse_shield.othersBodyFactor` | `0.4` | 0.05 to 1.0 | Every other blow to the boss's body is multiplied by this when his shield is down. |

### `[apocalypse_boss]`: the fight

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `apocalypse_boss.powerFactor` | `0.65` | 0.1 to 1.0 | 3.2: how strong the boss fights, one master knob (1.0 = the 3.1 numbers). It multiplies every damage he and his Horsemen deal and the number of his lances, Horsemen and tornadoes, and it slows his attacks: the pause between two attacks and every attack's cooldown last 1/powerFactor times as long. His health, his armour and his shield are not touched by it (they have their own options). |
| `apocalypse_boss.enabled` | `true` | `true` or `false` | If false the Capstone's ritual is refused. |
| `apocalypse_boss.ritualDimensions` | `["minecraft:overworld"]` | a list of dimension ids | Where he can be woken. |
| `apocalypse_boss.ritualNeedsSand` | `true` | `true` or `false` | The ritual block must be sand or sandstone (`enhanced:ritual_ground`). |
| `apocalypse_boss.ritualDaylightOnly` | `true` | `true` or `false` | It must be day, under the open sky, at the start of the ritual. |
| `apocalypse_boss.requireBreaker` | `true` | `true` or `false` | The ritual is refused unless a Scarlet Witch or a Phoenix (a hero in `mindPowerHeroes`) is within `breakerRange`. |
| `apocalypse_boss.breakerRange` | `64` | 8 to 256 | Blocks. |
| `apocalypse_boss.arenaRadius` | `24` | 12 to 48 | Radius of the circle he fights in. |
| `apocalypse_boss.arenaHeight` | `40` | 16 to 100 | How high the sand wall reaches. |
| `apocalypse_boss.fieldDamage` | `1.0` | 0.0 to 10.0 | Damage (never lethal) a player takes touching the sand wall, once a second. |
| `apocalypse_boss.bodyHealth` | `900.0` | 100.0 to 50000.0 | His health for one fighter. |
| `apocalypse_boss.healthPerExtraPlayer` | `0.5` | 0.0 to 3.0 | Extra health per additional fighter (a fraction of `bodyHealth`). |
| `apocalypse_boss.armor` | `10.0` | 0.0 to 30.0 | His armour in the armoured form. |
| `apocalypse_boss.armorToughness` | `6.0` | 0.0 to 20.0 | His armour toughness in the armoured form. |
| `apocalypse_boss.maxHitFraction` | `0.12` | 0.01 to 1.0 | The most one hit can take, as a fraction of his maximum health (before armour). |
| `apocalypse_boss.explosionDamageFactor` | `0.5` | 0.0 to 1.0 | Explosion damage to him (in a window and in the true form) is multiplied by this. |
| `apocalypse_boss.shieldStrength` | `100.0` | 10.0 to 10000.0 | Strength of shield 1 for one fighter. |
| `apocalypse_boss.shieldGrowth` | `1.2` | 1.0 to 3.0 | Each later shield is this many times stronger. |
| `apocalypse_boss.shieldPerExtraPlayer` | `0.3` | 0.0 to 3.0 | Extra shield strength per additional fighter (a fraction). |
| `apocalypse_boss.windowTicks` | `240` | 40 to 2400 | How long a broken shield leaves him vulnerable (240 = 12 s). |
| `apocalypse_boss.staggerTicks` | `80` | 0 to 400 | How long he is staggered and attacks nothing at the start of a window. |
| `apocalypse_boss.windowDamageFactor` | `1.25` | 1.0 to 5.0 | Damage taken in a window. |
| `apocalypse_boss.regenPerSecond` | `0.0` | 0.0 to 100.0 | His healing while a shield is up (never above the phase's ceiling). 0 = his health never heals (his shield still knits). |
| `apocalypse_boss.trueFormRegenPerSecond` | `0.0` | 0.0 to 100.0 | His healing in the true form after 80 ticks without being hurt. 0 = his health never heals. |
| `apocalypse_boss.escapeChannelTicks` | `100` | 20 to 600 | How long his escape portal takes. |
| `apocalypse_boss.escapeInterruptFraction` | `0.04` | 0.005 to 0.5 | Damage (a fraction of `H0`) that interrupts the escape; any lightning damage also does. |
| `apocalypse_boss.escapeHealTo` | `0.40` | 0.05 to 0.9 | The health he returns with if the escape was not interrupted. |
| `apocalypse_boss.phoenixFinishSeconds` | `15` | 0 to 120 | If a Phoenix dealt Burst or slam damage in this many seconds before the end, he dies in her fire. |
| `apocalypse_boss.lanceDamage` | `6.0` | 0.0 to 100.0 | One Sand Lance. |
| `apocalypse_boss.lanceCount` | `5` | 1 to 9 | Lances in a fan. |
| `apocalypse_boss.waveDamage` | `10.0` | 0.0 to 100.0 | The Sand Wave. |
| `apocalypse_boss.spikeDamage` | `9.0` | 0.0 to 100.0 | A Sand Spike. |
| `apocalypse_boss.boulderDamage` | `9.0` | 0.0 to 100.0 | A Sand Boulder. |
| `apocalypse_boss.backhandDamage` | `12.0` | 0.0 to 100.0 | The Backhand. |
| `apocalypse_boss.portalShockDamage` | `6.0` | 0.0 to 100.0 | The shockwave where he arrives. |
| `apocalypse_boss.stormDamage` | `2.0` | 0.0 to 100.0 | A Tornado's damage every 10 ticks. |
| `apocalypse_boss.tornadoTicks` | `160` | 20 to 1200 | How long a Tornado lasts. |
| `apocalypse_boss.ringDamage` | `10.0` | 0.0 to 100.0 | A ring of the Cairo Collapse. |
| `apocalypse_boss.entombTicks` | `80` | 10 to 1200 | How long the Entomb holds. |
| `apocalypse_boss.entombDamagePerSecond` | `1.5` | 0.0 to 20.0 | Damage while entombed (never below 2 hearts). |
| `apocalypse_boss.cocoonHealth` | `20.0` | 1.0 to 200.0 | Health of the boss's cocoon. |
| `apocalypse_boss.mindCrushRange` | `28` | 8 to 64 | Blocks. |
| `apocalypse_boss.mindCrushNauseaTicks` | `80` | 0 to 1200 | Nausea of the Mind Crush (Slowness 3 s and Blindness 2 s go with it). |
| `apocalypse_boss.horsemen` | `true` | `true` or `false` | He calls his Horsemen at Transition 1. |
| `apocalypse_boss.horsemenMax` | `4` | 0 to 8 | The most Horsemen: `min(ceil(horsemenMax x powerFactor), 1 + fighters)`, so 3 at the default `powerFactor`. |
| `apocalypse_boss.horsemanHealthFactor` | `1.5` | 1.0 to 10.0 | Husk health multiplier. |
| `apocalypse_boss.horsemanDamageBonus` | `3.0` | 0.0 to 50.0 | Added attack damage. |
| `apocalypse_boss.adaptTicks` | `80` | 0 to 1200 | Quicksilver Time slows him for this long, then he adapts. |
| `apocalypse_boss.adaptedTicks` | `600` | 0 to 12000 | How long he then ignores it. |
| `apocalypse_boss.magnetoStripExtendTicks` | `100` | 0 to 1200 | A Magneto's Strip on him in a window extends it by this much (once per window). |
| `apocalypse_boss.noBreakerSeconds` | `60` | 10 to 600 | He withdraws if no breaker is in the arena while his shield is up for this long. |
| `apocalypse_boss.dropsPhoenixEmber` | `false` | `true` or `false` | Also give a Phoenix Ember to a rewarded fighter who has no power (and is online when he falls). |
| `apocalypse_boss.holdTicks` | `60` | 10 to 600 | How long telekinesis can hold Apocalypse before he breaks free (before the hero's factor, `boss.billyHoldFactor` or `boss.magnetoHoldFactor`). |

### `[apocalypse]`: the hero (a player who won the Essence of En Sabah Nur)

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `apocalypse.flightSpeed` | `0.08` | 0.01 to 0.5 | Levitation speed (Creative flight is 0.05). |
| `apocalypse.bodyHealthBonus` | `20.0` | 0.0 to 200.0 | Extra max health (20 = 10 hearts, so 40). |
| `apocalypse.armor` | `10.0` | 0.0 to 30.0 | Armour while Apocalypse. |
| `apocalypse.armorToughness` | `4.0` | 0.0 to 20.0 | Armour toughness while Apocalypse. |
| `apocalypse.knockbackResistance` | `0.5` | 0.0 to 1.0 | Knockback resistance while Apocalypse. |
| `apocalypse.regenPerSecond` | `1.0` | 0.0 to 20.0 | Healing factor: health a second after `regenDelayTicks` without damage. 0 turns it off. |
| `apocalypse.regenDelayTicks` | `100` | 0 to 1200 | Ticks without damage before the healing factor starts. |
| `apocalypse.waveDamage` | `8.0` | 0.0 to 100.0 | The Sand Wave. |
| `apocalypse.waveLength` | `18` | 4 to 48 | Blocks. |
| `apocalypse.waveSpeed` | `0.9` | 0.2 to 3.0 | Blocks per tick. |
| `apocalypse.waveSlowTicks` | `40` | 0 to 600 | Slowness II duration. |
| `apocalypse.waveCooldown` | `60` | 0 to 72000 | Ticks. |
| `apocalypse.waveExhaustion` | `1.0` | 0.0 to 40.0 | Hunger the Sand Wave costs. |
| `apocalypse.entombRange` | `24` | 4 to 64 | Blocks. |
| `apocalypse.entombTicks` | `80` | 10 to 1200 | How long the Entomb holds. |
| `apocalypse.entombDamagePerSecond` | `1.0` | 0.0 to 20.0 | Damage while entombed (never below 2 hearts). |
| `apocalypse.cocoonHealth` | `16.0` | 1.0 to 200.0 | Health of the cocoon (a friend breaks it by hitting it). |
| `apocalypse.entombCooldown` | `240` | 0 to 72000 | Ticks (shared with the Wave: the same key). |
| `apocalypse.entombExhaustion` | `3.0` | 0.0 to 40.0 | Hunger the Entomb costs. |
| `apocalypse.portalRange` | `40` | 4 to 128 | Blocks. |
| `apocalypse.portalAllyRadius` | `3` | 0 to 16 | Pets, teammates and sneaking players within this come along. |
| `apocalypse.portalCooldown` | `160` | 0 to 72000 | Ticks. |
| `apocalypse.portalExhaustion` | `2.0` | 0.0 to 40.0 | Hunger the Portal costs. |
| `apocalypse.shieldRadius` | `2.4` | 1.0 to 6.0 | Drawn radius of the sphere (and how close a projectile is neutralised). |
| `apocalypse.shieldWindUpTicks` | `10` | 0 to 200 | The shield stops nothing until it is complete. |
| `apocalypse.shieldMaxTicks` | `120` | 1 to 6000 | How long a complete shield can be held. |
| `apocalypse.shieldStrength` | `60.0` | 1.0 to 1000.0 | How much mind power it takes to break it. |
| `apocalypse.shieldOtherWear` | `0.5` | 0.0 to 1.0 | The part of any other damage it stops that wears it down (0 = it costs nothing, the literal rule; the boss's shield always uses 0). |
| `apocalypse.shieldCooldown` | `160` | 0 to 6000 | Ticks after a drop. |
| `apocalypse.shieldBrokenCooldown` | `400` | 0 to 6000 | Ticks after it shattered or was held to its limit. |
| `apocalypse.shieldExhaustion` | `0.1` | 0.0 to 4.0 | Hunger per tick while held. |
| `apocalypse.bestowRange` | `24` | 4 to 64 | Blocks. |
| `apocalypse.bestowTicks` | `1200` | 20 to 72000 | How long Empowered lasts. |
| `apocalypse.bestowMax` | `2` | 1 to 8 | Allies empowered at once. |
| `apocalypse.bestowCooldown` | `400` | 0 to 72000 | Ticks. |
| `apocalypse.bestowExhaustion` | `6.0` | 0.0 to 40.0 | Hunger Power Bestowal costs. |
| `apocalypse.empowerCooldownBoost` | `1.5` | 1.0 to 5.0 | A hero's ability cooldowns recharge this many times as fast while Empowered. |
| `apocalypse.stormRadius` | `7` | 2 to 32 | Blocks. |
| `apocalypse.stormTicks` | `120` | 20 to 1200 | How long the Sandstorm lasts. |
| `apocalypse.stormDamage` | `2.0` | 0.0 to 100.0 | Every 10 ticks. |
| `apocalypse.stormCooldown` | `600` | 0 to 72000 | Ticks (shared with Power Bestowal: the same key). |
| `apocalypse.stormExhaustion` | `8.0` | 0.0 to 40.0 | Hunger the Sandstorm costs. |
| `apocalypse.essenceRitualTicks` | `60` | 20 to 400 | How long the Essence must be held to awaken. |

## Agatha Harkness

Section `[agatha]` (3.3). The hero's page is [Agatha](Agatha). A player with no power who holds use on the Darkhold becomes her: R the Purple Beam (Sneak: the hex mark or the mind glimpse), G flight, V the Power Drain, B the Witch's Shield (hold), N the Rune Trap; magic used on her poisons whoever used it. The hex mark, the mind glimpse and the poison use the numbers of [`[darkhold]`](#the-darkhold).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `agatha.darkholdAwakens` | `true` | `true` or `false` | A player with no power who holds use on the Darkhold becomes Agatha (false: the book only refuses them, as in 3.2). |
| `agatha.awakenTicks` | `60` | 1 to 1200 | How long use is held on the Darkhold to become Agatha, in ticks (60 = 3 seconds). The book is kept. |
| `agatha.flightSpeed` | `0.085` | 0.01 to 0.5 | Agatha's flying speed (Creative flight is 0.05). |
| `agatha.bodyHealthBonus` | `20.0` | 0.0 to 1000.0 | Extra maximum health while a player is Agatha (20 = ten more hearts: 40 in all). |
| `agatha.retaliates` | `true` | `true` or `false` | Magic used on Agatha poisons whoever used it, with or without the Darkhold in hand (the numbers and the target are the [darkhold] poison options). |
| `agatha.beamDamagePerSecond` | `8.0` | 0.0 to 200.0 | Damage a second of her Purple Beam (R, hold) does to what the crosshair is on (8 = four hearts). |
| `agatha.beamRange` | `24` | 4 to 128 | How far (blocks) her Purple Beam reaches. |
| `agatha.beamInterval` | `5` | 1 to 40 | Ticks between two hits of her Purple Beam (5 = four hits a second). |
| `agatha.beamExhaustionPerSecond` | `1.0` | 0.0 to 40.0 | Food exhaustion a second of her Purple Beam costs (4.0 = 1 food point). Creative players pay nothing. |
| `agatha.drainRange` | `16` | 2 to 64 | How far (blocks) the Power Drain (V) reaches: the creature or player under the crosshair. |
| `agatha.drainTicks` | `200` | 20 to 72000 | How long a drained player's power is gone, in ticks (200 = 10 seconds): no ability key works and a flier falls. Players are drained only where PvP lets her hurt them. |
| `agatha.drainDamage` | `6.0` | 0.0 to 200.0 | Damage the Power Drain does to a creature (its life is drained; a player loses their power instead). |
| `agatha.drainHeal` | `4.0` | 0.0 to 200.0 | Health Agatha takes back from every drain (4 = two hearts). |
| `agatha.drainCooldown` | `300` | 0 to 72000 | Cooldown of the Power Drain, in ticks. |
| `agatha.drainExhaustion` | `2.0` | 0.0 to 40.0 | Food exhaustion of a Power Drain (4.0 = 1 food point). Creative players pay nothing. |
| `agatha.drainOnlySorcerers` | `true` | `true` or `false` | 3.5: the Power Drain takes only a witch's or sorcerer's power: Loki, the Scarlet Witch, Billy, Agatha, or a custom hero with any of their abilities. Other heroes are refused, and a refusal costs nothing. False: any hero's power, as before 3.5. (A creature always loses life.) |
| `agatha.shieldRadius` | `1.9` | 1.0 to 6.0 | The radius (blocks) of the Witch's Shield (B, hold), the purple sphere round her. |
| `agatha.shieldWindUpTicks` | `8` | 0 to 100 | Ticks the shield takes to form after B is pressed; it stops nothing until then. |
| `agatha.shieldMaxTicks` | `200` | 20 to 72000 | How long a complete shield can be held, in ticks (200 = 10 seconds). |
| `agatha.shieldStrength` | `40.0` | 1.0 to 10000.0 | The shield's strength: every blow it stops takes the damage it would have done from it; at 0 it shatters. |
| `agatha.shieldCooldown` | `40` | 0 to 72000 | Ticks before the shield can be raised again after it was let go of. |
| `agatha.shieldBrokenCooldown` | `200` | 0 to 72000 | Ticks before the shield can be raised again after it shattered or was held to its limit. |
| `agatha.shieldExhaustion` | `0.03` | 0.0 to 4.0 | Food exhaustion every tick the shield is held (4.0 = 1 food point). Creative players pay nothing. |
| `agatha.runeRange` | `24` | 2 to 64 | How far (blocks) she can lay the Rune Trap (N): where she looks, or round her own feet if at nothing. |
| `agatha.runeRadius` | `4.0` | 1.0 to 16.0 | The radius (blocks) of the circle of runes. |
| `agatha.runeTicks` | `100` | 20 to 1200 | How long the runes burn, in ticks (100 = 5 seconds): what stands in them is bound (Spellbound) and hurt. |
| `agatha.runeDamagePerSecond` | `2.0` | 0.0 to 200.0 | Damage a second of the runes does to every creature bound in them. |
| `agatha.runeCooldown` | `400` | 0 to 72000 | Cooldown of the Rune Trap, in ticks. |
| `agatha.runeExhaustion` | `3.0` | 0.0 to 40.0 | Food exhaustion of a Rune Trap (4.0 = 1 food point). Creative players pay nothing. |

## Cyclops

Section `[cyclops]` (3.3). The hero's page is [Cyclops](Cyclops): his body, the Optic Blast he fires through his ruby-quartz glasses, the giant beam that pours out when he takes them off, and his other abilities.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `cyclops.bodyHealthBonus` | `10.0` | 0.0 to 200.0 | Extra max health while Cyclops (10 = 5 hearts, so 30). |
| `cyclops.armor` | `4.0` | 0.0 to 30.0 | Armour while Cyclops (his field suit). |
| `cyclops.opticRange` | `32.0` | 4.0 to 128.0 | Optic blast (R held, glasses on): how far the beam reaches, in blocks. |
| `cyclops.opticDamage` | `2.0` | 0.0 to 100.0 | Optic blast: the damage of one hit on what the crosshair is on. |
| `cyclops.opticInterval` | `5` | 1 to 40 | Optic blast: ticks between two hits (2.0 every 5 ticks is 8 a second). |
| `cyclops.opticPush` | `0.35` | 0.0 to 3.0 | Optic blast: how hard each hit pushes what it hits away (his blasts are force, not heat). |
| `cyclops.opticExhaustionPerSecond` | `0.6` | 0.0 to 20.0 | Optic blast: food exhaustion a second of the beam costs (4 = one food point). |
| `cyclops.unleashedRange` | `48.0` | 4.0 to 128.0 | The glasses off: how far the giant beam reaches, in blocks. It never stops until the glasses are back on, and it never hurts Cyclops himself; it ends when he dies or leaves. |
| `cyclops.unleashedDamage` | `6.0` | 0.0 to 100.0 | The glasses off: the damage of one hit on everything in the giant beam. |
| `cyclops.unleashedInterval` | `10` | 1 to 40 | The glasses off: ticks between two hits of the giant beam. |
| `cyclops.unleashedRadius` | `1.2` | 0.2 to 4.0 | The glasses off: the radius of the giant beam, in blocks (what is that close to its line is hit). |
| `cyclops.unleashedPush` | `0.9` | 0.0 to 4.0 | The glasses off: how hard each hit pushes what it hits along the beam. |
| `cyclops.unleashedBreaksBlocks` | `false` | `true` or `false` | The glasses off: whether the giant beam breaks the blocks it strikes (off by default; never in a boss arena or where the player may not break, and never the blocks round his own feet). |
| `cyclops.unleashedMaxHardness` | `3.0` | 0.0 to 100.0 | The glasses off, with unleashedBreaksBlocks: the hardest block it breaks (stone is 1.5, obsidian 50). |
| `cyclops.ricochetDamage` | `7.0` | 0.0 to 100.0 | Ricochet Shot (Sneak + R): the damage to each creature the bolt hits (each once). |
| `cyclops.ricochetBounces` | `3` | 0 to 8 | Ricochet Shot: how many times the bolt bounces off a block. |
| `cyclops.ricochetRange` | `24.0` | 2.0 to 64.0 | Ricochet Shot: how far each stretch of the bolt flies, in blocks. |
| `cyclops.ricochetCooldown` | `40` | 0 to 1200 | Ricochet Shot: cooldown, in ticks. |
| `cyclops.ricochetExhaustion` | `0.5` | 0.0 to 20.0 | Ricochet Shot: food exhaustion. |
| `cyclops.concussionRange` | `7.0` | 1.0 to 24.0 | Concussion Blast (G): how far the short, wide blast reaches, in blocks. |
| `cyclops.concussionAngle` | `40.0` | 5.0 to 90.0 | Concussion Blast: the half-angle of its cone, in degrees. |
| `cyclops.concussionDamage` | `6.0` | 0.0 to 100.0 | Concussion Blast: the damage to each creature in the cone. |
| `cyclops.concussionKnockback` | `1.6` | 0.0 to 5.0 | Concussion Blast: how far it throws them back. |
| `cyclops.concussionCooldown` | `100` | 0 to 1200 | Concussion Blast: cooldown, in ticks. |
| `cyclops.concussionExhaustion` | `1.0` | 0.0 to 20.0 | Concussion Blast: food exhaustion. |
| `cyclops.leapUp` | `1.1` | 0.1 to 3.0 | Optic Leap (V): a blast at the ground throws him up at this speed (blocks a tick). |
| `cyclops.leapForward` | `0.9` | 0.0 to 3.0 | Optic Leap: and forward, the way he faces, at this speed. |
| `cyclops.leapBlastRadius` | `2.5` | 0.0 to 8.0 | Optic Leap: the radius of the blast where he leaves the ground. |
| `cyclops.leapBlastDamage` | `3.0` | 0.0 to 100.0 | Optic Leap: the damage of that blast to the creatures in it. |
| `cyclops.leapCooldown` | `80` | 0 to 1200 | Optic Leap: cooldown, in ticks. He takes no fall damage from the leap he makes. |
| `cyclops.leapExhaustion` | `0.8` | 0.0 to 20.0 | Optic Leap: food exhaustion. |
| `cyclops.rallyRadius` | `16` | 1 to 64 | Rally (B, "To me, X-Men!"): the radius in which his allies (players, tamed pets, golems) and he are given Speed, Strength and Resistance. |
| `cyclops.rallyTicks` | `300` | 20 to 6000 | Rally: how long the effects last, in ticks. |
| `cyclops.rallyCooldown` | `900` | 0 to 12000 | Rally: cooldown, in ticks. |
| `cyclops.rallyExhaustion` | `1.0` | 0.0 to 20.0 | Rally: food exhaustion. |

## Professor X

Section `[professor_x]` (3.3). The hero's page is [Professor X](Professor-X): the wheelchair's ritual, the Mental Blast (R), Hear Thoughts (G), Take Over a Body (V), Freeze Minds (B) and Cerebro (N).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `professor_x.wheelchairRitualTicks` | `60` | 1 to 600 | How long (ticks) right-click is held on Professor X's wheelchair to awaken as him. |
| `professor_x.blastRange` | `24` | 4 to 64 | How far (blocks) the Mental Blast reaches (line of sight needed). |
| `professor_x.blastDamage` | `6.0` | 0.0 to 100.0 | Damage of the Mental Blast (it ignores armour; players only with PvP on). |
| `professor_x.blastDizzyTicks` | `60` | 0 to 600 | How long whoever the Mental Blast hits is dizzy and slowed. |
| `professor_x.blastCooldown` | `40` | 0 to 72000 | Cooldown of the Mental Blast (ability 1), in ticks. |
| `professor_x.thoughtsRange` | `24` | 4 to 64 | How far away (blocks) Professor X hears the thoughts of players and creatures while Hear Thoughts is on. |
| `professor_x.linkRange` | `24` | 2 to 64 | How far (blocks) Professor X can reach a body to take it over (line of sight needed). |
| `professor_x.linkMaxDistance` | `48` | 4 to 128 | A body he steers that goes further than this (blocks) from his own is let go. |
| `professor_x.linkCreatureTicks` | `600` | 20 to 6000 | How long (ticks) he can stay in a creature's body (600 = 30 s). |
| `professor_x.linkPlayerTicks` | `160` | 0 to 1200 | How long (ticks) he can steer a player's body (needs PvP). 0 turns it off for players. |
| `professor_x.linkCooldown` | `300` | 0 to 72000 | Cooldown of Take Over a Body (ability 3) after he leaves a body or is thrown out, in ticks. |
| `professor_x.linkBreaksOnHurt` | `true` | `true` or `false` | If true, any damage to his own body (left behind while he steers another) brings him back. |
| `professor_x.backlashDamage` | `4.0` | 0.0 to 40.0 | Damage he takes when a stronger telepath (the Scarlet Witch, the Phoenix, Billy) throws him out. |
| `professor_x.backlashStunTicks` | `60` | 0 to 600 | How long he is stunned (dizzy and slowed) after he is thrown out. |
| `professor_x.freezeRadius` | `12` | 2 to 48 | Radius (blocks) of Freeze Minds. |
| `professor_x.freezeTicks` | `100` | 10 to 1200 | How long Freeze Minds holds everyone still (players only with PvP on). |
| `professor_x.freezeCooldown` | `600` | 0 to 72000 | Cooldown of Freeze Minds (ability 4), in ticks (600 = 30 s). |
| `professor_x.cerebroRange` | `256` | 16 to 4096 | How far (blocks) Cerebro finds the minds of players, in his dimension. |
| `professor_x.cerebroTicks` | `200` | 20 to 1200 | How long Cerebro shows the minds it found (200 = 10 s). |
| `professor_x.cerebroCooldown` | `1200` | 0 to 72000 | Cooldown of Cerebro (ability 5), in ticks (1200 = 60 s). |

## Asgard

Section `[asgard]` (3.5). The pages are [Asgard](Asgard) and [Asgardians](Asgardians): whether the realm can be reached, its rules, the rune stones, Hofund as a key, and its people.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `asgard.enabled` | `true` | `true` or `false` | Asgard can be reached. False: the Bifrost doesn't offer it, the rune stones fizzle, nobody can go in (operators may still `/tp`), and a player who logs in there is sent to his respawn point. Leaving is always allowed. |
| `asgard.protectCity` | `true` | `true` or `false` | The city of Asgard (its districts, the palace, the bridge, the Observatory) can't be broken, built on, burnt or blown up. The wild land and the sea are free; operators in Creative may edit. |
| `asgard.keepInventory` | `true` | `true` or `false` | A player who dies in Asgard keeps his inventory and experience. False: he drops them as anywhere, except Mjolnir, Stormbreaker, Hofund, Gungnir and Hela's Headdress, which are always kept. |
| `asgard.hofundOpensBifrost` | `true` | `true` or `false` | Anyone holding a Hofund (anyone's) can open the Bifrost, as with Mjolnir and Stormbreaker. False: only Heimdall's own key opens it. |
| `asgard.runeStoneCooldown` | `1200` | 20 to 72000 | Ticks a Bifrost Rune Stone rests after each journey (1200 = 60 seconds). It is the stone's own time, not the player's. |
| `asgard.voidCatchY` | `-56` | -64 to 0 | A player who falls below this height in Asgard is caught by Heimdall and set down in the Observatory, unhurt. |
| `asgard.edgeRadius` | `640` | 400 to 2000 | How far (blocks) from the centre of Asgard a player may go before he is pushed back toward the city. |
| `asgard.npcs` | `true` | `true` or `false` | The [Asgardians](Asgardians) live in the city. False: none at all (the city removes its own). |
| `asgard.talkDistance` | `6.0` | 2.0 to 16.0 | How far (blocks) a player may be from an Asgardian to talk to him. |

## The Bifrost

Section `[bifrost]` (3.5): who the Bifrost takes along, for **every** Bifrost (Thor's, Heimdall's, Hofund's, Gungnir's, a rune stone's, Heimdall sending a friend). See [Asgard](Asgard#the-bifrost-takes-everyone-in-its-circle). The Bifrost's cooldown is still `thor.bifrostCooldown`.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `bifrost.groupRadius` | `3.2` | 0.0 to 16.0 | The radius (blocks) of the Bifrost's circle: every player and creature whose feet are inside it (and no more than 3 blocks above or below) travels with the one who opened it. The default is the circle of runes it burns. **0**: only the one who opened it goes, as before 3.5. Heimdall's friend may step this far (at least 1 block) before he stays. |
| `bifrost.sneakToStay` | `false` | `true` or `false` | A player inside the circle who is sneaking stays behind. False (the default): everyone inside goes; step out of the circle to stay. |

## Heimdall

Section `[heimdall]` (3.5). The hero's page is [Heimdall](Heimdall), with the Guardian's Watch.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `heimdall.sightRange` | `96` | 16 to 256 | How far (blocks) the All-Seeing Sight shows creatures through walls. (Players in the same world are marked at any distance.) |
| `heimdall.seeOtherRealms` | `true` | `true` or `false` | The Sight lists the players of other worlds on Heimdall's HUD. |
| `heimdall.slashDamage1` | `9.0` | 0.0 to 100.0 | Damage of the first blow of the Hofund Slash. |
| `heimdall.slashDamage2` | `11.0` | 0.0 to 100.0 | Damage of the second blow. |
| `heimdall.slashDamage3` | `14.0` | 0.0 to 100.0 | Damage of the third blow (it knocks back). |
| `heimdall.lungeDamage` | `12.0` | 0.0 to 100.0 | Damage of the Lunge (Sneak + R) to the first creature in the way. |
| `heimdall.lungeCooldown` | `80` | 0 to 72000 | Cooldown of the Lunge, in ticks (80 = 4 seconds). |
| `heimdall.wardSeconds` | `10` | 1 to 120 | How long the Guardian's Ward (B, hold) can be held, in seconds. |
| `heimdall.wardCooldown` | `160` | 0 to 72000 | Cooldown of the Ward, in ticks (160 = 8 seconds). |
| `heimdall.callCooldown` | `900` | 0 to 72000 | Cooldown of the Gatekeeper's Call (Sneak + B), in ticks (900 = 45 seconds). |
| `heimdall.callSeconds` | `40` | 1 to 600 | How long the Einherjar of the Call stay, in seconds. |
| `heimdall.strikeDamage` | `14.0` | 0.0 to 100.0 | Damage of the Bifrost Strike (N), within 3 blocks of where it falls. |
| `heimdall.strikeCooldown` | `140` | 0 to 72000 | Cooldown of the Bifrost Strike, in ticks (140 = 7 seconds). Sending a friend (Sneak + N) uses it too. |
| `heimdall.sendRange` | `24` | 1 to 128 | How far (blocks) the player Heimdall sends with the Bifrost may be. |
| `heimdall.watchSeconds` | `180` | 30 to 1200 | How long the Guardian's Watch lasts, in seconds (180 = 3 minutes). |
| `heimdall.watchRetry` | `1200` | 0 to 72000 | Ticks before a player who lost the Watch may take it again (1200 = 60 seconds). |
| `heimdall.hofundBonus` | `0.3` | 0.0 to 5.0 | How much more damage Heimdall deals with Hofund (0.3 = 30% more). |
| `heimdall.postSeconds` | `600` | 1 to 7200 | How long (seconds) the Heimdall of the city hands his post (the Observatory and the Bifrost) to a player who is Heimdall, while he rests nearby (3.5.1; see [Heimdall's post](Heimdall#two-guardians-heimdalls-post-351)). |

## Odin

Section `[odin]` (3.5). The hero's page is [Odin](Odin).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `odin.blastDamage` | `12.0` | 0.0 to 100.0 | Damage of the Gungnir Blast (R) to the first creature (half to those beside it). |
| `odin.blastCooldown` | `25` | 0 to 72000 | Cooldown of the Gungnir Blast, in ticks. |
| `odin.throwDamage` | `16.0` | 0.0 to 100.0 | Damage of the thrown Gungnir (Sneak + R) to each creature it passes. |
| `odin.throwCooldown` | `60` | 0 to 72000 | Cooldown of the Gungnir Throw, in ticks (60 = 3 seconds). |
| `odin.shieldSeconds` | `12` | 1 to 120 | How long the Odinforce Shield (G, hold) can be held, in seconds. |
| `odin.shieldCooldown` | `200` | 0 to 72000 | Cooldown of the shield, in ticks (half again after it was worn out or broken). |
| `odin.heal` | `8.0` | 0.0 to 100.0 | Health the Odinforce Heal (Sneak + G) gives Odin and each ally within 8 blocks. |
| `odin.healCooldown` | `600` | 0 to 72000 | Cooldown of the Heal, in ticks (600 = 30 seconds). |
| `odin.banishCooldown` | `400` | 0 to 72000 | Cooldown of Banish (V), in ticks (400 = 20 seconds). A creature that is spared costs nothing. |
| `odin.banishPlayers` | `true` | `true` or `false` | Banish sends a player home by the Bifrost (only where PvP is on). False: players are spared. |
| `odin.waveDamage` | `10.0` | 0.0 to 100.0 | Damage of the Odinforce Wave (Sneak + V). |
| `odin.waveCooldown` | `220` | 0 to 72000 | Cooldown of the Wave, in ticks. |
| `odin.ravensSeconds` | `20` | 1 to 600 | How long Huginn and Muninn (B) fly, in seconds. |
| `odin.ravensCooldown` | `600` | 0 to 72000 | Cooldown of the ravens, in ticks (600 = 30 seconds). |
| `odin.commandCooldown` | `1200` | 0 to 72000 | Cooldown of the Allfather's Command (N), in ticks (1200 = 60 seconds). |
| `odin.odinsleepMaxSeconds` | `30` | 1 to 600 | The longest the Odinsleep (Sneak + N) lasts, in seconds. |
| `odin.odinsleepCooldown` | `1800` | 0 to 72000 | Cooldown of the Odinsleep after it ends, in ticks (1800 = 90 seconds). |

## Hela

Two sections (3.5): `[hela_boss]` for the fight in Asgard's throne room and `[hela]` for the hero. The page is [Hela](Hela).

### `[hela_boss]`: the fight

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `hela_boss.summonEnabled` | `true` | `true` or `false` | Breaking Hela's Seal calls her. False: the seal refuses (an operator's `/enhanced boss start hela force` still works). |
| `hela_boss.health` | `600.0` | 1.0 to 100000.0 | Hela's health with one fighter. |
| `hela_boss.healthPerExtraFighter` | `0.35` | 0.0 to 10.0 | Extra health for each fighter after the first (0.35 = 35% more each), counted while fighters join in the first 20 seconds. |
| `hela_boss.armor` | `10.0` | 0.0 to 30.0 | Hela's armour. |
| `hela_boss.phase2At` | `0.7` | 0.0 to 1.0 | The share of her health at which the Army of the Dead begins. |
| `hela_boss.phase3At` | `0.35` | 0.0 to 1.0 | The share of her health at which the Strength of Asgard begins. |
| `hela_boss.volleyDamage` | `7.0` | 0.0 to 100.0 | Damage of each necrosword of her volleys. |
| `hela_boss.wallDamage` | `8.0` | 0.0 to 100.0 | Damage of each spike of her Blade Wall. |
| `hela_boss.rainDamage` | `6.0` | 0.0 to 100.0 | Damage of each blade of her Blade Rain. |
| `hela_boss.meleeDamage` | `9.0` | 0.0 to 100.0 | Damage of each blow of her blade combo. |
| `hela_boss.stormDamage` | `10.0` | 0.0 to 100.0 | Damage of her Blade Storm. |
| `hela_boss.channelSeconds` | `4` | 1 to 30 | How long she channels the Strength of Asgard, in seconds. |
| `hela_boss.channelEvery` | `20` | 5 to 600 | How often she channels in the third phase, in seconds. |
| `hela_boss.channelBreakFraction` | `0.06` | 0.0 to 1.0 | The share of her health the fighters must deal during a channel to break it (0.06 = 6%). |
| `hela_boss.channelHeal` | `0.1` | 0.0 to 1.0 | The share of her health a channel that is not broken heals. |
| `hela_boss.berserkerWave` | `4` | 1 to 20 | Berserkers in a wave with one fighter. |
| `hela_boss.berserkerPerFighter` | `2` | 0 to 10 | Extra berserkers in a wave for each fighter after the first. |
| `hela_boss.berserkerMax` | `10` | 1 to 40 | The most berserkers standing at once. |
| `hela_boss.fenrisHealth` | `140.0` | 1.0 to 10000.0 | The health of Fenris. |
| `hela_boss.restTicks` | `24000` | 0 to 720000 | Ticks before Hela's Seal can be broken again after she was beaten (24000 = 20 minutes); half of it after a fight that ended any other way. |
| `hela_boss.catchMjolnir` | `true` | `true` or `false` | Hela catches a thrown Mjolnir and shatters it (at most once a minute, from the second phase). |

### `[hela]`: the hero

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `hela.volleyDamage` | `7.0` | 0.0 to 100.0 | Damage of each necrosword of her Necrosword Volley (R). |
| `hela.volleyCooldown` | `30` | 0 to 72000 | Cooldown of the volley, in ticks. |
| `hela.rainCooldown` | `160` | 0 to 72000 | Cooldown of the Blade Rain (Sneak + R), in ticks. |
| `hela.wallDamage` | `8.0` | 0.0 to 100.0 | Damage of each spike of her Blade Wall (G). |
| `hela.wallCooldown` | `140` | 0 to 72000 | Cooldown of the Blade Wall, in ticks. |
| `hela.burstCooldown` | `120` | 0 to 72000 | Cooldown of the Blade Burst (Sneak + G), in ticks. |
| `hela.formStrength` | `2` | 1 to 5 | The level of Strength the Goddess of Death (V) gives. |
| `hela.dashCooldown` | `100` | 0 to 72000 | Cooldown of the Death Dash (Sneak + V), in ticks. |
| `hela.raiseCount` | `3` | 1 to 10 | How many berserkers Raise the Dead (B) calls. |
| `hela.raiseCountAsgard` | `5` | 1 to 10 | How many it calls in Asgard. |
| `hela.raiseCooldown` | `1200` | 0 to 72000 | Cooldown of Raise the Dead, in ticks (1200 = 60 seconds). |
| `hela.fenrisCooldown` | `1800` | 0 to 72000 | Cooldown of calling Fenris (Sneak + B), in ticks (1800 = 90 seconds). |
| `hela.graspSeconds` | `6` | 1 to 60 | How long Death's Grasp (N, hold) can be held, in seconds. |
| `hela.graspCooldown` | `100` | 0 to 72000 | Cooldown of Death's Grasp, in ticks. |
| `hela.asgardFactor` | `1.25` | 1.0 to 5.0 | Hela's strength from Asgard: her damage is multiplied by this in Asgard. |
| `hela.shatterMjolnir` | `true` | `true` or `false` | Death's Grasp catches and shatters a thrown Mjolnir. |

## The Infinity Stones (3.6)

Seven sections for the [Infinity Stones](Infinity-Stones): who may hold them, the Infinity Gauntlet and the Snap, and the numbers of each stone (the Soul Stone's are in [its own section](#the-soul-stone-and-vormir)). Every value is read when it is used.

### `[stones]`: who may hold them, the burn and the ledger

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `stones.uniqueness` | `PLAYER` | `PLAYER`, `WORLD` or `UNLIMITED` | How many of each stone found in a place (the Reality, Time, Power and Soul Stones) there are: `PLAYER` (each player may take each once), `WORLD` (one of each in the whole world) or `UNLIMITED` (every place gives its stone every time). See [the ledger](Infinity-Stones#how-many-stones-the-ledger). |
| `stones.graceSeconds` | `10` | 3 to 60 | Seconds a stone may stay in the inventory of one who may not hold it before it burns him and jumps out. |
| `stones.handGraceFactor` | `3` | 1 to 10 | How many times faster the grace runs while the stone is held in a hand. |
| `stones.rejectDamage` | `2.0` | 0.0 to 20.0 | Damage of a stone's burn when the grace runs out. It never kills: it leaves at least half a heart. |
| `stones.customBearers` | `true` | `true` or `false` | A custom hero with a god's ability (the Scarlet Witch's Hex, Loki's Frost Giant, or Thor's, the Phoenix's, Heimdall's, Odin's or Hela's body) may hold the stones as a god does. |
| `stones.visionHoldsMind` | `true` | `true` or `false` | Whether Vision may hold the Mind Stone (it is his own). |
| `stones.sorcerersHoldTime` | `true` | `true` or `false` | Whether a sorcerer may hold the Time Stone. |
| `stones.returnLost` | `true` | `true` or `false` | A lost stone (in the void, killed with `/kill`, lost with a sacrificed player's things) can be taken again: in `PLAYER` mode by the one who lost it, in `WORLD` mode from its places. |

### `[gauntlet]`: the Infinity Gauntlet and the Snap

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `gauntlet.craftable` | `true` | `true` or `false` | Whether the empty Infinity Gauntlet can be crafted. |
| `gauntlet.snapEnabled` | `true` | `true` or `false` | Whether the Snap works (all six stones in a Gauntlet): half of the hostile creatures near turn to dust. |
| `gauntlet.snapRadius` | `64` | 8 to 256 | Radius (blocks) of the Snap. |
| `gauntlet.snapFraction` | `0.5` | 0.0 to 1.0 | Fraction of the hostile creatures the Snap turns to dust. |
| `gauntlet.snapCooldownTicks` | `24000` | 0 to 720000 | Ticks before a Gauntlet can snap again (24000 = 20 minutes). |
| `gauntlet.snapHoldTicks` | `60` | 10 to 400 | Ticks the use is held (sneaking) for the Snap (60 = 3 seconds). |
| `gauntlet.snapInAsgard` | `false` | `true` or `false` | Whether the Snap works in Asgard. |
| `gauntlet.snapCostHealth` | `2.0` | 1.0 to 20.0 | Health a wielder other than Thanos is left with after the Snap. |
| `gauntlet.mindForThanos` | `true` | `true` or `false` | Whether the hero Thanos gets the Mind Stone's powers from a Gauntlet he holds with it. |

### `[reality_stone]`

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `reality_stone.radius` | `4.0` | 1.0 to 16.0 | Radius (blocks) in which projectiles heading for the holder turn to bubbles. |
| `reality_stone.exhaustionPerBubble` | `0.5` | 0.0 to 10.0 | Food exhaustion each bubble costs. |
| `reality_stone.bossRate` | `10` | 1 to 200 | In a boss arena, at most one bubble per this many ticks (10 = 0.5 seconds). |
| `reality_stone.bossCost` | `3.0` | 1.0 to 20.0 | How many times the hunger a bubble costs in a boss arena. |
| `reality_stone.transmuteRange` | `20` | 4 to 64 | Range (blocks) of the Transmute. |
| `reality_stone.transmuteTicks` | `60` | 10 to 600 | Ticks a creature stays stone after the Transmute (60 = 3 seconds). |
| `reality_stone.transmuteCooldown` | `240` | 0 to 72000 | Cooldown (ticks) of the Transmute (240 = 12 seconds). |
| `reality_stone.illusionTicks` | `120` | 20 to 1200 | Ticks the Illusion lasts (120 = 6 seconds). |
| `reality_stone.illusionCooldown` | `400` | 0 to 72000 | Cooldown (ticks) of the Illusion (400 = 20 seconds). |

### `[mind_stone]`

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `mind_stone.beamDamage` | `4.0` | 0.0 to 100.0 | Damage of each hit of the Mind Beam (twice a second). |
| `mind_stone.beamRange` | `32` | 4 to 128 | Range (blocks) of the Mind Beam. |
| `mind_stone.beamMaxTicks` | `120` | 10 to 1200 | Longest (ticks) the Mind Beam can be held (120 = 6 seconds). |
| `mind_stone.beamCooldown` | `60` | 0 to 72000 | Cooldown (ticks) of the Mind Beam after it was held to the end (60 = 3 seconds). |
| `mind_stone.confuseEvery` | `40` | 1 to 400 | The Mind Beam confuses the same target at most once per this many ticks (40 = 2 seconds). |
| `mind_stone.commandRange` | `16` | 4 to 64 | Range (blocks) of the Mind Command. |
| `mind_stone.commandTicks` | `400` | 20 to 6000 | Ticks a commanded creature fights for the holder (400 = 20 seconds). |
| `mind_stone.commandCooldown` | `600` | 0 to 72000 | Cooldown (ticks) of the Mind Command (600 = 30 seconds). |
| `mind_stone.exhaustionPerTick` | `0.03` | 0.0 to 1.0 | Food exhaustion per tick of the Mind Beam. |

### `[time_stone]`

These numbers are also those of the **Eye of Agamotto** and of Doctor Strange's **Y**.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `time_stone.slowRadius` | `12` | 2 to 64 | Radius (blocks) of the Slow Time zone round the holder. |
| `time_stone.slowFactor` | `3` | 2 to 20 | How many times slower time runs in the zone. |
| `time_stone.slowMaxTicks` | `200` | 20 to 2400 | Longest (ticks) Slow Time stays on (200 = 10 seconds). |
| `time_stone.slowCooldown` | `200` | 0 to 72000 | Cooldown (ticks) of Slow Time (200 = 10 seconds). |
| `time_stone.bossFactorHalf` | `true` | `true` or `false` | Whether bosses are slowed at half the factor. |
| `time_stone.rewindSeconds` | `5` | 2 to 30 | How far back (seconds) the Rewind goes. |
| `time_stone.rewindCooldown` | `600` | 0 to 72000 | Cooldown (ticks) of the Rewind (600 = 30 seconds). |
| `time_stone.cropMagic` | `true` | `true` or `false` | Whether the Time Stone grows (or, sneaking, un-grows) a crop, a sapling or a baby animal. |
| `time_stone.cropCooldown` | `10` | 0 to 72000 | Cooldown (ticks) of the crop magic (10 = 0.5 seconds). |

### `[power_stone]`

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `power_stone.beamDamage` | `4.0` | 0.0 to 100.0 | Damage per hit (every 10 ticks) of the Purple Beam. |
| `power_stone.beamRange` | `32` | 4 to 128 | Range (blocks) of the Purple Beam. |
| `power_stone.explodeEvery` | `10` | 1 to 200 | Ticks between two explosions at the end of the Purple Beam (10 = 0.5 seconds). |
| `power_stone.explosionDamage` | `6.0` | 0.0 to 100.0 | Damage of each explosion at the end of the Purple Beam. |
| `power_stone.explosionRadius` | `2.5` | 0.5 to 8.0 | Radius (blocks) of each explosion. |
| `power_stone.terrainDamage` | `false` | `true` or `false` | Whether the explosions break blocks (also needs the gamerule mobGriefing, never in protected places). |
| `power_stone.craterRadius` | `1.5` | 0.5 to 4.0 | Radius (blocks) of a crater when terrainDamage is on. |
| `power_stone.beamMaxTicks` | `60` | 10 to 600 | Longest (ticks) the Purple Beam can be held before it overheats (60 = 3 seconds). |
| `power_stone.overheatCooldown` | `140` | 0 to 72000 | Cooldown (ticks) after the Purple Beam overheats (140 = 7 seconds). |
| `power_stone.releaseCooldown` | `60` | 0 to 72000 | Cooldown (ticks) after the Purple Beam is let go of early (60 = 3 seconds). |
| `power_stone.boltDamage` | `12.0` | 0.0 to 100.0 | Damage of the Power Bolt. |
| `power_stone.boltSplash` | `6.0` | 0.0 to 100.0 | Splash damage of the Power Bolt. |
| `power_stone.boltRange` | `28` | 4 to 128 | Range (blocks) of the Power Bolt. |
| `power_stone.boltCooldown` | `70` | 0 to 72000 | Cooldown (ticks) of the Power Bolt (70 = 3.5 seconds). |

### `[space_stone]`

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `space_stone.stepRange` | `64` | 4 to 256 | Range (blocks) of the Portal Step. |
| `space_stone.stepCooldown` | `60` | 0 to 72000 | Cooldown (ticks) of the Portal Step (60 = 3 seconds). |
| `space_stone.realmCooldown` | `600` | 0 to 72000 | Cooldown (ticks) of the Realm Picker (600 = 30 seconds). |
| `space_stone.reachesAsgard` | `true` | `true` or `false` | Whether the Space Stone reaches Asgard (the one exception to "only the Bifrost"). |
| `space_stone.netherSearch` | `32` | 4 to 128 | Radius (blocks) searched for safe ground on arriving in the Nether. |
| `space_stone.endArrivalX` | `100` | 0 to 1000 | X of the arrival on the End's main island (Z 0). |
| `space_stone.arrivalResistanceTicks` | `40` | 0 to 600 | Ticks of Resistance V on arriving in a realm (40 = 2 seconds). |

## The Soul Stone and Vormir

Section `[soul_stone]` (3.6): the sacrifice at the Vormir Cliff (see [Vormir](Vormir)) and the Soul Stone's powers.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `soul_stone.enabled` | `true` | `true` or `false` | Whether the Vormir Cliff gives the Soul Stone for a sacrifice. |
| `soul_stone.playerSacrifice` | `true` | `true` or `false` | Whether a player can be the sacrifice (works only where PvP is on). |
| `soul_stone.petSacrifice` | `true` | `true` or `false` | Whether the seeker's own pet can be the sacrifice. |
| `soul_stone.destroyItems` | `true` | `true` or `false` | Whether a sacrificed player loses all his items (his ender chest is never touched). |
| `soul_stone.removePower` | `true` | `true` or `false` | Whether a sacrificed player loses his power. |
| `soul_stone.friendRule` | `ANY` | `ANY` or `TEAM` | Who is a friend: `ANY` other player (the default: PvP and the cliff's edge make it deliberate) or only a `TEAM` mate (the same scoreboard team). |
| `soul_stone.onePer` | `PLAYER` | `PLAYER`, `WORLD` or `UNLIMITED` | How many Soul Stones: `PLAYER` (each player once), `WORLD` (one in the whole world) or `UNLIMITED`. |
| `soul_stone.pushWindowTicks` | `100` | 20 to 400 | Ticks before the victim left the edge in which the seeker's push counts (100 = 5 seconds). |
| `soul_stone.minFall` | `30` | 10 to 100 | The shortest fall (blocks) that counts. |
| `soul_stone.deathWindowTicks` | `200` | 20 to 1200 | Ticks after leaving the edge in which the death must come (200 = 10 seconds). |
| `soul_stone.drainDamagePerSecond` | `6.0` | 0.0 to 100.0 | Damage per second of the Soul Drain. |
| `soul_stone.drainRange` | `16` | 4 to 64 | Range (blocks) of the Soul Drain. |
| `soul_stone.drainHealFraction` | `0.5` | 0.0 to 1.0 | Fraction of the drained health the holder gets back. |
| `soul_stone.drainCooldown` | `40` | 0 to 72000 | Cooldown (ticks) after the Soul Drain ends (40 = 2 seconds). |
| `soul_stone.sightRange` | `32` | 4 to 128 | Range (blocks) of the Soul Sight. |
| `soul_stone.sightTicks` | `160` | 20 to 1200 | Ticks the Soul Sight lasts (160 = 8 seconds). |
| `soul_stone.sightCooldown` | `400` | 0 to 72000 | Cooldown (ticks) of the Soul Sight (400 = 20 seconds). |

## The stone sites

Section `[stone_sites]` (3.6): the four new structures of the stones ([Structures](Structures#the-stone-sites-36)). Each one only stops **new** sites: those already built stay.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `stone_sites.sanctums` | `true` | `true` or `false` | Whether new Sanctums Sanctorum generate (the Time Stone's site). |
| `stone_sites.avengersTowers` | `true` | `true` or `false` | Whether new Avengers Towers generate (the Tesseract's site). |
| `stone_sites.endTunnels` | `true` | `true` or `false` | Whether new End tunnels generate (the Power Stone's site). |
| `stone_sites.vormirCliffs` | `true` | `true` or `false` | Whether new Vormir Cliffs generate in the Nether (the Soul Stone's site). |

## Doctor Strange, the Eye of Agamotto and the Sling Ring

Sections `[doctor_strange]`, `[eye_of_agamotto]` and `[sling_ring]` (3.6). The page is [Doctor Strange](Doctor-Strange). The Eye's Slow Time and Rewind use the Time Stone's numbers (`[time_stone]`, [above](#the-infinity-stones-36)).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `doctor_strange.flightSpeed` | `0.06` | 0.01 to 0.5 | Flight speed on the Cloak of Levitation (Creative flight is 0.05). |
| `doctor_strange.whipDamage1` | `7.0` | 0.0 to 100.0 | Damage of the first lash of the Eldritch Whip. |
| `doctor_strange.whipDamage2` | `7.0` | 0.0 to 100.0 | Damage of the second lash. |
| `doctor_strange.whipDamage3` | `10.0` | 0.0 to 100.0 | Damage of the third lash (it wraps and lifts). |
| `doctor_strange.whipReach` | `8.0` | 2.0 to 32.0 | Reach (blocks) of the Eldritch Whip. |
| `doctor_strange.grappleRange` | `24` | 4 to 64 | Range (blocks) of the Whip Grapple. |
| `doctor_strange.grappleCooldown` | `60` | 0 to 72000 | Cooldown (ticks) of the Whip Grapple (60 = 3 seconds). |
| `doctor_strange.shieldSeconds` | `10` | 1 to 120 | Longest (seconds) the Shield of the Seraphim is held. |
| `doctor_strange.shieldCooldown` | `120` | 0 to 72000 | Cooldown (ticks) of the Shield of the Seraphim (120 = 6 seconds). |
| `doctor_strange.domeSeconds` | `6` | 1 to 60 | Seconds the Seraphim Dome stands. |
| `doctor_strange.domeRadius` | `4.0` | 1.0 to 16.0 | Radius (blocks) of the Seraphim Dome. |
| `doctor_strange.domeCooldown` | `400` | 0 to 72000 | Cooldown (ticks) of the Seraphim Dome (400 = 20 seconds). |
| `doctor_strange.bandsRange` | `20` | 4 to 64 | Range (blocks) of the Crimson Bands of Cyttorak. |
| `doctor_strange.bandsTicks` | `80` | 10 to 600 | Ticks the Crimson Bands hold a creature (80 = 4 seconds). |
| `doctor_strange.bandsBossTicks` | `20` | 0 to 600 | Ticks the Crimson Bands hold a boss (20 = 1 second). |
| `doctor_strange.bandsCooldown` | `200` | 0 to 72000 | Cooldown (ticks) of the Crimson Bands (200 = 10 seconds). |
| `doctor_strange.astralSeconds` | `30` | 5 to 300 | Longest (seconds) the Astral Projection lasts. |
| `doctor_strange.astralRange` | `64` | 8 to 256 | How far (blocks) the spirit may go from the body. |
| `doctor_strange.astralCooldown` | `300` | 0 to 72000 | Cooldown (ticks) of the Astral Projection (300 = 15 seconds). |
| `doctor_strange.portalRange` | `64` | 4 to 256 | Range (blocks) of the Sling-Ring Portal's far side. |
| `doctor_strange.portalSeconds` | `8` | 1 to 60 | Seconds a pair of portals stays open. |
| `doctor_strange.portalCooldown` | `100` | 0 to 72000 | Cooldown (ticks) of the Sling-Ring Portal (100 = 5 seconds). |
| `doctor_strange.mirrorRadius` | `14` | 4 to 48 | Radius (blocks) of the Mirror Dimension. |
| `doctor_strange.mirrorSeconds` | `20` | 5 to 120 | Seconds the Mirror Dimension lasts. |
| `doctor_strange.mirrorCooldown` | `1200` | 0 to 72000 | Cooldown (ticks) of the Mirror Dimension (1200 = 1 minute). |
| `doctor_strange.mirrorDamageBonus` | `0.2` | 0.0 to 2.0 | How much more his spells hurt inside the Mirror Dimension (0.2: +20%). |
| `doctor_strange.healthBonus` | `6.0` | 0.0 to 40.0 | Health his body adds. |
| `doctor_strange.armorBonus` | `2.0` | 0.0 to 20.0 | Armour his body adds. |
| `doctor_strange.safeFall` | `12.0` | 0.0 to 64.0 | Falls (blocks) the Cloak catches without damage. |
| `doctor_strange.wardRadius` | `32` | 0 to 128 | Radius of the ward he raises with the Book of the Vishanti (he is a sorcerer; the Scarlet Witch's number). |

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `eye_of_agamotto.awakenTicks` | `60` | 10 to 400 | Ticks a powerless player holds the Eye to become Doctor Strange (60 = 3 seconds). |
| `eye_of_agamotto.ownerOnly` | `true` | `true` or `false` | Whether an Eye bound to a Doctor Strange awakens nobody else while he is one. |
| `eye_of_agamotto.nonSorcererMessage` | `true` | `true` or `false` | Whether a non-sorcerer is told that the Eye stays closed. |

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `sling_ring.places` | `8` | 1 to 16 | How many places a Sling Ring keeps. |
| `sling_ring.cooldown` | `200` | 0 to 72000 | Cooldown (ticks) of the Sling Ring's portal to a place (200 = 10 seconds). |
| `sling_ring.crossDimension` | `true` | `true` or `false` | Whether the Sling Ring opens a portal to a place in another dimension (never Asgard). |

## The Hulk and the Gamma Emitter

Sections `[hulk]` and `[gamma]` (3.6). The page is [Hulk](Hulk). Every damage of the Brand New Day Hulk is multiplied by `hulk.bndDamageFactor`, and his rage adds up to `hulk.rageDamageBonus` more.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `hulk.blockBreaking` | `SOFT` | `NONE`, `SOFT` or `ALL` | What the Hulk breaks: `NONE`, `SOFT` (glass, leaves, snow, ice, flowers, grass, vines, cobwebs; never doors, crops or beds) or `ALL` (also small craters in dirt, sand, stone and wood). |
| `hulk.needsMobGriefing` | `true` | `true` or `false` | Whether every block the Hulk breaks also needs the gamerule mobGriefing. |
| `hulk.healthBonus` | `20.0` | 0.0 to 200.0 | Health the Hulk's body adds. |
| `hulk.armorBonus` | `8.0` | 0.0 to 30.0 | Armour the Hulk's body adds. |
| `hulk.toughnessBonus` | `4.0` | 0.0 to 20.0 | Armour toughness the Hulk's body adds. |
| `hulk.knockbackResistance` | `0.9` | 0.0 to 1.0 | Knockback resistance of the Hulk. |
| `hulk.attackBonus` | `7.0` | 0.0 to 100.0 | Attack damage the Hulk's fists add. |
| `hulk.attackKnockback` | `1.5` | 0.0 to 10.0 | Attack knockback the Hulk's fists add. |
| `hulk.scaleRagnarok` | `0.5` | 0.0 to 1.5 | How much bigger the Ragnarok Hulk is (0.5: x1.5). |
| `hulk.scaleBrandNewDay` | `0.75` | 0.0 to 1.5 | How much bigger the Brand New Day Hulk is (0.75: x1.75). |
| `hulk.bndDamageFactor` | `1.15` | 1.0 to 2.0 | Every damage of the Brand New Day Hulk is multiplied by this. |
| `hulk.reachRagnarok` | `1.5` | 0.0 to 6.0 | Reach the Ragnarok Hulk's long arms add. |
| `hulk.reachBrandNewDay` | `2.0` | 0.0 to 6.0 | Reach the Brand New Day Hulk's long arms add. |
| `hulk.jumpBonus` | `0.5` | 0.0 to 2.0 | Jump strength the Hulk adds (his giant jumps). |
| `hulk.safeFall` | `24.0` | 0.0 to 256.0 | Safe fall distance the Hulk adds. |
| `hulk.stepBonus` | `0.6` | 0.0 to 2.0 | Step height the Hulk adds. |
| `hulk.speedBonus` | `0.15` | 0.0 to 1.0 | Movement speed the Hulk adds (0.15: +15%). |
| `hulk.breakSpeedFactor` | `4.0` | 1.0 to 20.0 | How many times faster the Hulk breaks blocks by hand. |
| `hulk.regenPerSecond` | `0.35` | 0.0 to 10.0 | Health the Hulk heals per second when not hurt for 5 seconds. |
| `hulk.exhaustionFactor` | `1.5` | 1.0 to 5.0 | How many times hungrier the Hulk gets. |
| `hulk.smashDamage` | `7.0` | 0.0 to 100.0 | Damage of Hulk Smash to everyone on the ground near. |
| `hulk.smashRadius` | `6` | 1 to 32 | Radius (blocks) of Hulk Smash. |
| `hulk.smashCooldown` | `60` | 0 to 72000 | Cooldown (ticks) of Hulk Smash (60 = 3 seconds). |
| `hulk.grabDamage1` | `4.0` | 0.0 to 100.0 | Damage of the first slam of the Grab Smash. |
| `hulk.grabDamage2` | `4.0` | 0.0 to 100.0 | Damage of the second slam. |
| `hulk.grabDamage3` | `6.0` | 0.0 to 100.0 | Damage of the third slam. |
| `hulk.grabBossPunch` | `8.0` | 0.0 to 100.0 | Damage of the one heavy punch a boss or a grab-immune creature gets instead. |
| `hulk.grabCooldown` | `160` | 0 to 72000 | Cooldown (ticks) of the Grab Smash (160 = 8 seconds). |
| `hulk.clapDamage` | `6.0` | 0.0 to 100.0 | Damage of the Thunder Clap. |
| `hulk.clapLength` | `14` | 2 to 64 | Length (blocks) of the Thunder Clap's shockwave. |
| `hulk.clapAngle` | `50` | 10 to 180 | Width (degrees) of the Thunder Clap's cone. |
| `hulk.clapKnockback` | `3.0` | 0.0 to 10.0 | How far the Thunder Clap throws. |
| `hulk.clapExtinguishes` | `true` | `true` or `false` | Whether the Thunder Clap puts out fire. |
| `hulk.clapCooldown` | `100` | 0 to 72000 | Cooldown (ticks) of the Thunder Clap (100 = 5 seconds). |
| `hulk.roarRadius` | `12` | 2 to 64 | Radius (blocks) of the Roar. |
| `hulk.roarFrightTicks` | `80` | 0 to 1200 | Ticks hostile creatures stay frightened by the Roar (80 = 4 seconds). |
| `hulk.roarWeaknessTicks` | `160` | 0 to 1200 | Ticks of Weakness the Roar gives (160 = 8 seconds). |
| `hulk.roarCooldown` | `300` | 0 to 72000 | Cooldown (ticks) of the Roar (300 = 15 seconds). |
| `hulk.leapDamage` | `6.0` | 0.0 to 100.0 | Damage where the Super Leap lands. |
| `hulk.leapRadius` | `4` | 1 to 16 | Radius (blocks) of the Super Leap's landing. |
| `hulk.leapRange` | `48` | 8 to 128 | Range (blocks) of the Super Leap. |
| `hulk.leapCooldown` | `120` | 0 to 72000 | Cooldown (ticks) of the Super Leap (120 = 6 seconds). |
| `hulk.boundDistance` | `24` | 4 to 64 | Distance (blocks) of the Bound. |
| `hulk.boundCooldown` | `30` | 0 to 72000 | Cooldown (ticks) of the Bound (30 = 1.5 seconds). |
| `hulk.boulderDamage` | `8.0` | 0.0 to 100.0 | Damage of the boulder to the creature it hits. |
| `hulk.boulderSplash` | `4.0` | 0.0 to 100.0 | Splash damage of the boulder. |
| `hulk.boulderCooldown` | `80` | 0 to 72000 | Cooldown (ticks) of the Boulder Throw (80 = 4 seconds). |
| `hulk.chargeDamage` | `6.0` | 0.0 to 100.0 | Damage of the Gamma Charge. |
| `hulk.chargeDistance` | `12` | 2 to 48 | Distance (blocks) of the Gamma Charge. |
| `hulk.chargeCooldown` | `140` | 0 to 72000 | Cooldown (ticks) of the Gamma Charge (140 = 7 seconds). |
| `hulk.transformTicks` | `40` | 1 to 200 | Ticks the change between Bruce Banner and the Hulk takes (40 = 2 seconds). |
| `hulk.transformCooldown` | `100` | 0 to 72000 | Cooldown (ticks) of the Transform (100 = 5 seconds). |
| `hulk.switchCooldown` | `40` | 0 to 72000 | Cooldown (ticks) of Switch Hulk (40 = 2 seconds). |
| `hulk.rage` | `true` | `true` or `false` | Whether the Hulk has his Rage meter. |
| `hulk.rageDamageBonus` | `0.3` | 0.0 to 2.0 | The most the Rage adds to his damage (0.3: +30%). |
| `hulk.bannerAngerTransform` | `true` | `true` or `false` | Whether Bruce Banner Hulks out by himself when angry. |
| `hulk.bannerAngerPerHeart` | `10` | 0 to 100 | Anger Banner gains per heart of damage. |
| `hulk.bannerLowHealthTransform` | `true` | `true` or `false` | Whether Banner gets much angrier below 4 health (the Hulk protects Banner). |
| `hulk.wieldsGauntlet` | `true` | `true` or `false` | Whether the Hulk may wield a filled Infinity Gauntlet and Snap (Endgame). |
| `hulk.armBurnedSeconds` | `600` | 0 to 7200 | Seconds the Hulk's arm stays burned after his Snap. |

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `gamma.emitterCraftable` | `true` | `true` or `false` | Whether the Gamma Emitter can be crafted. |
| `gamma.warmupTicks` | `60` | 0 to 400 | Ticks the Gamma Emitter warms up before its beam (60 = 3 seconds). |
| `gamma.exposureTicks` | `60` | 1 to 400 | Ticks a powerless player stands in the beam to become the Hulk (60 = 3 seconds). |
| `gamma.beamTicks` | `60` | 1 to 400 | Ticks the beam lasts (60 = 3 seconds). |
| `gamma.beamLength` | `6` | 1 to 16 | Length (blocks) of the beam. |
| `gamma.emitterCooldown` | `200` | 0 to 72000 | Cooldown (ticks) of the Gamma Emitter (200 = 10 seconds). |
| `gamma.healsHulk` | `true` | `true` or `false` | Whether the beam heals a Hulk in it. |
| `gamma.healPerSecond` | `2.0` | 0.0 to 20.0 | Health per second the beam gives a Hulk. |

## Hydra soldiers

Section `[hydra]` (3.6). The page is [Hydra Soldiers](Hydra-Soldiers).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `hydra.enabled` | `true` | `true` or `false` | Whether Hydra soldiers guard the Hydra outposts (false stops new ones; those there stay). |
| `hydra.garrisonSize` | `9` | 0 to 14 | Posts of an outpost's garrison. |
| `hydra.respawn` | `DAYS` | `NEVER`, `DAYS` or `ALWAYS` | When a beaten garrison comes back: `NEVER`, `DAYS` (after `respawnDays`, while nobody is near) or `ALWAYS`. |
| `hydra.respawnDays` | `3` | 1 to 100 | Minecraft days before a dead soldier's post fills again (with `respawn` = `DAYS`). |
| `hydra.activateRadius` | `64` | 16 to 256 | A garrison is filled while a player is within this many blocks. |
| `hydra.leashRadius` | `48` | 8 to 128 | The farthest (blocks) a soldier goes from his outpost. |
| `hydra.maxPerOutpost` | `14` | 0 to 64 | The most Hydra alive at one outpost (posts and reinforcements). |
| `hydra.maxLoaded` | `80` | 0 to 1000 | The most Hydra in one dimension. |
| `hydra.alarm` | `true` | `true` or `false` | Whether Hydra raises the alarm and calls reinforcements. |
| `hydra.alarmTicks` | `600` | 20 to 12000 | Ticks the alarm sounds (600 = 30 seconds). |
| `hydra.reinforcements` | `4` | 0 to 8 | Soldiers each wave of reinforcements brings. |
| `hydra.maxAlarmsPerDay` | `2` | 0 to 24 | The most waves of reinforcements per outpost per in-game day. |
| `hydra.sightRange` | `24` | 4 to 64 | How far (blocks) a soldier sees. |
| `hydra.sneakFactor` | `0.5` | 0.0 to 1.0 | How much of his sight a sneaking intruder leaves him. |
| `hydra.rifleDamage` | `4.0` | 0.0 to 100.0 | Damage of each bolt of a rifleman's burst. |
| `hydra.burstShots` | `3` | 1 to 10 | Bolts in a burst. |
| `hydra.burstCooldown` | `50` | 5 to 400 | Ticks between two bursts (50 = 2.5 seconds). |
| `hydra.sniperDamage` | `8.0` | 0.0 to 100.0 | Damage of the sniper's bolt. |
| `hydra.sniperRange` | `40` | 8 to 128 | Range (blocks) of the sniper. |
| `hydra.sniperAimTicks` | `30` | 0 to 200 | Ticks the sniper aims (his red laser shows) before he fires (30 = 1.5 seconds). |
| `hydra.batonDamage` | `6.0` | 0.0 to 100.0 | Damage of the enforcer's stun baton. |
| `hydra.pistolDamage` | `3.0` | 0.0 to 100.0 | Damage of the officer's energy pistol. |
| `hydra.boltSpeed` | `2.2` | 0.2 to 8.0 | Speed (blocks per tick) of a Hydra bolt. |
| `hydra.scepterTurns` | `true` | `true` or `false` | Whether a hit of Loki's Scepter turns a Hydra soldier to the hitter's side. |
| `hydra.scepterTurnTicks` | `1200` | 20 to 24000 | Ticks a turned soldier fights for the hitter (an officer half) (1200 = 1 minute). |
| `hydra.blasterEnabled` | `true` | `true` or `false` | Whether players can use the Hydra blaster. |
| `hydra.blasterDamage` | `5.0` | 0.0 to 100.0 | Damage of the Hydra blaster's bolt. |
| `hydra.blasterCooldown` | `20` | 0 to 1200 | Cooldown (ticks) of the Hydra blaster (20 = 1 second). |
| `hydra.blasterDrop` | `0.1` | 0.0 to 1.0 | Chance an officer drops a Hydra blaster. |
| `hydra.intelMapDrop` | `0.25` | 0.0 to 1.0 | Chance an officer drops Hydra intel (a map to an Avengers Tower). |
| `hydra.energyCellDrop` | `0.2` | 0.0 to 1.0 | Chance a rifleman drops an energy cell. |

## Music

Section `[music]` (3.5): [a world's own music](Music#a-worlds-own-music-servers), the songs a server owner puts in `<world>/serverconfig/enhanced/music/` to replace the Walkman songs and the Witches' Road for every player.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `music.enabled` | `true` | `true` or `false` | Send the songs of the world's music folder to the players. False: everyone hears the built-in music (or his own). |
| `music.maxFileKB` | `8192` | 16 to 65536 | The biggest song file the server sends, in KB (8192 = 8 MB); a bigger one is left out, and the log says so. |
| `music.chunksPerTick` | `2` | 1 to 16 | How many pieces of a song (30 KB each) a downloading player is sent each tick (2 = about 1.2 MB a second). |

## The Hero Creator

Section `[custom_hero]` (3.3; five options added in 3.4). The page is [Hero Creator](Hero-Creator): whether the creator works and for whom, the limits of a custom hero, the balance of the thirty powers, and (3.4) whether a custom hero may take the heroes' own abilities and look extras. The numbers of each power follow its strength (1 to 10); the scales below multiply them all. Every option is read when it is used; a hero already made keeps its powers and is judged by the new limits the next time it is saved. The ids for `disabledPowers` are in the table of [the thirty powers](Hero-Creator#the-thirty-powers) and on [Heroes' Abilities](Hero-Abilities).

**From 3.3:** 3.4 raised the defaults of `budget` (60 to 90) and `maxPowers` (8 to 10). A server file written by 3.3 still says 60 and 8 (the game never rewrites a value already in the file): set the new numbers, or delete the two lines. Every ready-made hero of the editor fits in 60.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `custom_hero.enabled` | `true` | `true` or `false` | The Hero Creator works (false: its screen says it is switched off, and custom heroes are judged by these limits when they are saved). |
| `custom_hero.operatorsOnly` | `false` | `true` or `false` | Only operators may use the Hero Creator (false: any player in Creative mode, and operators). |
| `custom_hero.allowChooseExisting` | `true` | `true` or `false` | The creator can also turn its user into any hero of the mod, in any look, at once. |
| `custom_hero.budget` | `90` | 1 to 600 | Power points a custom hero may spend: each power costs 1 to 3 points for each point of its strength (flight 2, durability 1, phasing 3...; strength 1 to 10); an ability with no slider costs as at strength 5; a power's other option on a Sneak box costs half the power again. The screen shows what is left. (60 before 3.4.) |
| `custom_hero.maxPowers` | `10` | 1 to 30 | How many different powers a custom hero may have (a hero's ability and its Sneak variant are one). (8 before 3.4.) |
| `custom_hero.maxStrength` | `10` | 1 to 10 | The highest strength of a power (the editor's slider goes from 1 to this). |
| `custom_hero.maxSkinBytes` | `16384` | 1024 to 24576 | The biggest custom skin (a 64x64 PNG) the server accepts and sends to everyone, in bytes. |
| `custom_hero.disabledPowers` | `[]` | a list of power ids | Powers a custom hero may not have on this server: their ids, e.g. `"time"`, `"weather"`; since 3.4 also a hero's ability (`"thor.lightning"`) or all of a hero's abilities (`"thor.*"`). |
| `custom_hero.applyCooldownTicks` | `40` | 0 to 1200 | Ticks a player waits between two heroes made or chosen with the creator. |
| `custom_hero.damageScale` | `1.0` | 0.0 to 10.0 | Multiplies the damage of every custom power (since 3.4 also of the heroes' abilities a custom hero has). |
| `custom_hero.cooldownScale` | `1.0` | 0.1 to 10.0 | Multiplies the cooldown of every custom power (since 3.4 also of the heroes' abilities a custom hero has). |
| `custom_hero.exhaustionScale` | `1.0` | 0.0 to 10.0 | Multiplies the hunger every custom power costs (0: free). |
| `custom_hero.flightSpeed` | `0.08` | 0.01 to 0.5 | Scales Flight's speed: at the default 0.08, strength 1 flies at 0.06 and strength 10 at 0.13; another value scales them all by it / 0.08 (Creative flight is 0.05). |
| `custom_hero.forceFieldStrength` | `35.0` | 1.0 to 1000.0 | Scales the Force Field: at the default 35, it takes 10 damage before it breaks at strength 1 and 80 at strength 10; another value scales them by it / 35. |
| `custom_hero.controlSeconds` | `20` | 1 to 600 | Scales how long Mind Control, Animal Control and Technopathy's golems hold: at the default 20, from 5 seconds (strength 1) to 30 (strength 10), golems 10 to 30; another value scales them by it / 20. |
| `custom_hero.teleportRange` | `32` | 4 to 128 | Scales Teleportation's reach: at the default 32, Blink reaches 8 to 48 blocks and Behind You 8 to 32 (strength 1 to 10); another value scales them by it / 32. |
| `custom_hero.suppressSeconds` | `8` | 1 to 120 | Scales how long Power Absorption takes another hero's powers away (PvP only): at the default 8, from 3 seconds (strength 1) to 12 (strength 10); another value scales them by it / 8. |
| `custom_hero.blockChanges` | `false` | `true` or `false` | Custom powers may change blocks: the Sonic Scream shatters glass (doors, levers and frozen water are always allowed, and every change still asks the claims, spawn protection and boss arenas). |
| `custom_hero.weatherChangesWorld` | `true` | `true` or `false` | Weather Control's "cycle" changes the world's weather (false: it only says it cannot). |
| `custom_hero.heroModules` | `true` | `true` or `false` | 3.4: a custom hero may take the abilities of the mod's own heroes (Thor's lightning, Agatha's shield...; the [abilities of the heroes](Hero-Abilities), 111 since 3.6). False: only the thirty powers, and a hero file's abilities are left out with a note. |
| `custom_hero.worldPowers` | `true` | `true` or `false` | 3.4: a custom hero may take the heroes' world-scale abilities: the Hex, the Bifrost, Thanos's Time Stone, Ultron's Upload and Sentry Army, Professor X's Take Over a Body, and the others marked "a world power" on [Heroes' Abilities](Hero-Abilities) (since 3.6 Doctor Strange's Sling-Ring Portal and the Space Stone's row). |
| `custom_hero.heroStrengthSpread` | `0.5` | 0.0 to 1.0 | 3.4: how much the strength (1 to 10) of a hero's ability changes it: its damage and its body times 1 + spread × (strength − 5) / 5, its cooldown divided by that. At 0.5, strength 1 is ×0.6 and strength 10 is ×1.5; at 0 strength changes nothing (every ability is the hero's own). Strength 5 is always the hero himself. |
| `custom_hero.layers` | `true` | `true` or `false` | 3.4: a custom hero's look may have extras: a cape, a hat, a visor or mask, glowing eyes, an aura (false: they are taken off, with a note). |
| `custom_hero.stoneRows` | `true` | `true` or `false` | 3.6: a custom hero may put an Infinity Stone on a key (a [stone row](Hero-Creator#the-stone-rows-36)); it works only while he carries that stone and may use it. False: the stone rows can't be picked ("Stone rows are off on this server."). |
| `custom_hero.chooseGivesItems` | `true` | `true` or `false` | 3.4: "Choose a Hero" also gives the hero's own items, as becoming him the usual way leaves them with him (Thor's Mjolnir or Stormbreaker, claimed; Magneto's helmet, Quicksilver's goggles and Cyclops's glasses, worn; Loki's Scepter, the Gauntlet, Ultron's Core, the wheelchair, the Darkhold, and since 3.6 Doctor Strange's Eye of Agamotto and Sling Ring). Never a second one. |

## The Witches' Road

Section `[witches_road]`. The page is [The Witches' Road](Witches-Road). Every trial has its options here, with the trial's name in front of the explanation. Every value is read when it is used, so editing the file changes it live. With `witches_road.enabled` set to `false` there are no trials and the door is the instant choice of 3.1.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `witches_road.enabled` | `true` | `true` or `false` | Whether the Witches' Road has trials. On: a player with no power who opens a Witches' Road door with a Scarlet Witch Gene is asked to walk the Road (the trials), and chooses Billy or Tommy at the end. `false`: the door gives the instant choice of Enhanced 3.1, and nobody can enter the Road dimension. |
| `witches_road.maxPlayers` | `32` | 1 to 512 | The most players walking the trials at once on the whole server (each has a private place). A player who would be one too many is told to wait. |
| `witches_road.courseSeed` | `0` | -9223372036854775808 to 9223372036854775807 | Mixed into every player's course. The courses are the same for the same player every time; change this to give every player new ones. |
| `witches_road.resumeOnLogin` | `true` | `true` or `false` | A player who logs in inside the Road (the server stopped, or he left the game, in a trial) goes on at his last checkpoint. `false`: he is taken back to where he entered, with his progress kept. |
| `witches_road.resumeAtCheckpoint` | `true` | `true` or `false` | A player who left a trial and walks the Road again starts at the checkpoint he had reached. `false`: he starts the trial from its beginning. |
| `witches_road.announce` | `true` | `true` or `false` | The server is told in chat when somebody finishes a trial of the Road. |
| `witches_road.voice` | `true` | `true` or `false` | The Road speaks to the player in the action bar and the title (the Voice of the Road). |
| `witches_road.healAtCheckpoint` | `true` | `true` or `false` | Reaching a checkpoint, or being put back at one, heals the player and fills his hunger. |
| `witches_road.fallDepth` | `12` | 3 to 64 | How far (in blocks) below the lowest part of a stretch of the course a fall counts: the player is then put back at his last checkpoint (nothing hurts him). |
| `witches_road.leaveConfirmSeconds` | `5` | 1 to 60 | Right-clicking the exit door asks for a second click within this many seconds to leave the Road. |
| `witches_road.buildBlocksPerTick` | `20000` | 500 to 1000000 | How many blocks of a new trial are placed in one tick (the Road is laid in a few ticks; a lower number means a smaller hitch on a slow server and a longer wait at the door). |
| `witches_road.removeBlocksPerTick` | `20000` | 500 to 1000000 | How many blocks of a finished trial are removed in one tick. |
| `witches_road.endingFarewellSeconds` | `5` | 0 to 30 | The ending: after the player has chosen Billy or Tommy in the Road (the Gene is used up and the power awakens), the Road says its farewell in a column of light for this many seconds, and then sends him back to where he entered. 0 = he is sent back at once. |
| `witches_road.parkourPads` | `40` | 8 to 150 | Trial 1, the Parkour: how many jumps there are from the start to the finish. |
| `witches_road.parkourDifficulty` | `2` | 1 to 3 | Trial 1, the Parkour: 1 = easy (gaps of one or two blocks, wide stones), 2 = normal (gaps up to three blocks late in the course, steps up and down), 3 = hard (smaller stones, gaps of three blocks sooner). No jump is ever beyond what a running jump can do. |
| `witches_road.parkourCheckpointEvery` | `6` | 2 to 30 | Trial 1, the Parkour: a checkpoint (a lit amethyst stone) after every this many jumps. |
| `witches_road.parkourTimeLimitSeconds` | `0` | 0 to 7200 | Trial 1, the Parkour: seconds allowed from the start to the finish (the clock shows in the action bar). When it runs out the player is put back at the start. 0 = no limit. |
| `witches_road.boatRaceLength` | `800` | 200 to 1400 | Trial 3, the Ice-Boat Race: how long the course is, from the start line to the finish line, in blocks. The course winds through the Road's place, so it is never longer than about 1400; the shortcuts make the shortest way shorter. |
| `witches_road.boatRaceTrackWidth` | `9` | 7 to 15 | Trial 3, the Ice-Boat Race: how wide the track is, in blocks (an odd number; the narrow stretches are 5 wide). |
| `witches_road.boatRaceDifficulty` | `2` | 1 to 3 | Trial 3, the Ice-Boat Race: 1 = easy (wide curves, one hairpin, no snow walls), 2 = normal, 3 = hard (tighter curves, three hairpins, more snow). Every curve can be driven at the speed the boat can reach on the straight before it. |
| `witches_road.boatRaceShortcuts` | `1` | 0 to 3 | Trial 3, the Ice-Boat Race: how many shortcuts the course may have. A shortcut is a straight across a loop of the track with a gap in it (the boat has to be fast to jump it, and falls into the void if it is not); the long way round is always open. |
| `witches_road.boatRaceObstacles` | `true` | `true` or `false` | Trial 3, the Ice-Boat Race: snow drifts (rough snow slows the boat) and snow walls to wind between. Nothing traps the boat. |
| `witches_road.boatRaceBlueIce` | `true` | `true` or `false` | Trial 3, the Ice-Boat Race: stripes of blue ice (the slipperiest ice, the boat gains speed on it) in the track. |
| `witches_road.boatRaceCheckpointEvery` | `100` | 40 to 400 | Trial 3, the Ice-Boat Race: a checkpoint about every this many blocks. A boat that falls off the track is put back on the last one. |
| `witches_road.boatRaceTimeLimitSeconds` | `240` | 0 to 3600 | Trial 3, the Ice-Boat Race: seconds allowed from the start of the trial (a clock shows at the top of the screen); never less than `boatRaceFairFactor` times the par time. When it runs out the player is put back at the start. 0 = no limit. |
| `witches_road.boatRaceFairFactor` | `3.0` | 0.0 to 20.0 | Trial 3, the Ice-Boat Race: the time limit is never shorter than the par time times this (a long course with the default limit would otherwise send a driver who is not fast back to the start every time). 0 = the limit is exactly `boatRaceTimeLimitSeconds`. |
| `witches_road.boatRaceParFactor` | `1.4` | 1.0 to 5.0 | Trial 3, the Ice-Boat Race: the par time is the time the boat's physics allow on this course (the fastest way through every curve) times this factor. The player is told how he did against it. |
| `witches_road.boatRaceRespawnCooldownSeconds` | `5` | 0 to 120 | Trial 3, the Ice-Boat Race: a player who is stuck can sneak (Shift) to be put back on the last checkpoint. This many seconds have to pass between two such returns (a sneak before that only keeps him in the boat). |
| `witches_road.boatRaceCountdownSeconds` | `3` | 1 to 10 | Trial 3, the Ice-Boat Race: the countdown (3, 2, 1, go) once the player is in his boat, before the barrier at the start line opens. |
| `witches_road.structureCopyTier` | `2` | 0 to 3 | Trial 2, Copy the Structure: how big the model is. 1 = small (up to 5x5x5 blocks), 2 = medium (up to 9x9x7), 3 = large (up to 11x11x9). 0 = it depends on the player (each player gets the same size every time, but not the same for everyone). |
| `witches_road.structureCopyTimeLimitSeconds` | `5400` | 0 to 86400 | Trial 2, Copy the Structure: seconds a player may stay in the trial. There is no failing: this is only so that nobody leaves a room of the Road standing for ever. When it runs out the room is taken down and the player is sent back to where he entered, with his Gene and his progress (the next time he builds a new model from the beginning). 0 = no limit. |
| `witches_road.structureCopySupplyPercent` | `150` | 100 to 1000 | Trial 2, Copy the Structure: how much of every block the model needs the player is given, in percent (at least 16 of each). The supply chest on the platform fills itself up again, so he cannot run out. |
| `witches_road.structureCopyAutoCheck` | `true` | `true` or `false` | Trial 2, Copy the Structure: the trial is won as soon as the plot looks exactly like the model, without pressing Check. `false`: the player has to press the Check button. |
| `witches_road.structureCopyHints` | `true` | `true` or `false` | Trial 2, Copy the Structure: the Hint button shows ghost outlines of the blocks that are missing. It costs nothing. `false`: the button does nothing. |
| `witches_road.structureCopyHintSeconds` | `8` | 1 to 60 | Trial 2, Copy the Structure: how long the ghost blocks of a hint stay. |
| `witches_road.structureCopyHintBlocks` | `40` | 1 to 300 | Trial 2, Copy the Structure: the most ghost blocks a hint shows (the lowest ones that are missing first). |
| `witches_road.structureCopyMarks` | `40` | 1 to 300 | Trial 2, Copy the Structure: the most places that Check marks with glowing outlines (missing, wrong or extra blocks). The count of correct blocks is always shown in full. |

The grid of places (2,048 blocks apart) and the course radius (450 blocks) are not options: they decide where the Road puts each player.

## The Book of the Vishanti

Section `[vishanti]`. The page is [The Sorcerers' Books](Sorcerers-Books#the-book-of-the-vishanti). The book is read by a sorcerer (Loki, the Scarlet Witch, Billy or Agatha); the ward's radius follows his strength: Wanda the most, Billy by his Build Magic rank, Loki and Agatha a fixed size. In 3.3 a ward keeps out every monster, boss and boss minion, what they shoot, and dark magic (see [The Sorcerers' Books](Sorcerers-Books#the-ward-what-it-keeps-out)). The ward is never bigger than 128 blocks, whatever these say.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `vishanti.enabled` | `true` | `true` or `false` | The Book of the Vishanti works (`false`: it can be held but nobody can read it). |
| `vishanti.teleportCooldown` | `600` | 0 to 72000 | Cooldown of the book's teleport, in ticks (600 = 30 seconds; 1200 = a minute). |
| `vishanti.teleportChargeTicks` | `20` | 1 to 200 | Ticks the sparks gather round the traveller before the book takes them. |
| `vishanti.teleportExhaustion` | `6.0` | 0.0 to 40.0 | Food exhaustion of one teleport (4.0 = 1 food point). Creative players pay nothing. |
| `vishanti.deniedDimensions` | `["enhanced:witches_road"]` | a list of dimension ids | Dimensions where the book can neither save a place, raise a ward nor teleport, and that it cannot take anyone into or out of (ids). The Witches' Road is always shut to it, whatever this says. |
| `vishanti.wardsEnabled` | `true` | `true` or `false` | Bases can carry a ward (`false`: bases are only places; existing wards sleep). |
| `vishanti.maxWardsPerPlayer` | `2` | 1 to 16 | How many bases (so how many wards) one player can have at a time. |
| `vishanti.wardCooldown` | `200` | 0 to 72000 | Cooldown, in ticks, between raising (or moving) one player's wards. |
| `vishanti.wardExhaustion` | `8.0` | 0.0 to 40.0 | Food exhaustion of raising a ward (4.0 = 1 food point). Creative players pay nothing. |
| `vishanti.wardRadiusWanda` | `48` | 4 to 128 | Radius (in blocks) of the ward of the Scarlet Witch, the strongest sorcerer: the most a ward can be. |
| `vishanti.wardRadiusBillyMin` | `12` | 4 to 128 | Radius of the ward of Billy at his first Build Magic rank (an Apprentice). |
| `vishanti.wardRadiusBillyMax` | `40` | 4 to 128 | Radius of the ward of Billy at his highest Build Magic rank (`build_magic_billy.maxRank`); between his first and highest rank it grows evenly. |
| `vishanti.wardRadiusLoki` | `24` | 4 to 128 | Radius of the ward of Loki. |
| `vishanti.wardRadiusAgatha` | `24` | 4 to 128 | 3.3: radius of the ward of Agatha Harkness. |
| `vishanti.wardCheckTicks` | `5` | 1 to 40 | Every how many ticks a ward looks for monsters (and bosses) inside it and pushes them out (one spatial lookup per ward; more is cheaper, fewer keeps the edge tighter). |
| `vishanti.wardPush` | `0.8` | 0.1 to 4.0 | How hard a ward throws a monster at its edge back out (blocks per tick of speed). |
| `vishanti.wardBlocksSpawns` | `true` | `true` or `false` | No monster spawns inside a ward (natural spawns, spawners, structures). |

## The Darkhold

Section `[darkhold]`. The page is [The Sorcerers' Books](Sorcerers-Books#the-darkhold). Its powers need the book in hand and never work in the Witches' Road.

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `darkhold.enabled` | `true` | `true` or `false` | The Darkhold works (`false`: it can be held but does nothing). |
| `darkhold.anyoneCanUse` | `false` | `true` or `false` | Anyone can use the Darkhold, not only the sorcerers (Loki, the Scarlet Witch, Billy and Agatha). While `agatha.darkholdAwakens` is on, a player with no power who holds use on it still becomes Agatha. |
| `darkhold.beamDamagePerSecond` | `8.0` | 0.0 to 200.0 | Damage a second of the purple beam does to what the crosshair is on (8 = four hearts). |
| `darkhold.beamRange` | `24` | 4 to 128 | How far (in blocks) the purple beam reaches. |
| `darkhold.beamInterval` | `5` | 1 to 40 | Ticks between two hits of the purple beam (5 = four hits a second). The beam ignores the creature's damage cooldown, so every hit lands. |
| `darkhold.beamExhaustionPerSecond` | `1.0` | 0.0 to 40.0 | Food exhaustion a second of the purple beam costs (4.0 = 1 food point). Creative players pay nothing. |
| `darkhold.poisonTarget` | `ATTACKER` | `ATTACKER` or `HOLDER` | Who is poisoned when magic is used on the holder of the Darkhold: `ATTACKER` (whoever used it, the default) or `HOLDER` (the holder pays for the magic the book drew). |
| `darkhold.poisonTicks` | `200` | 20 to 72000 | How long the poison lasts, in ticks (200 = 10 seconds). |
| `darkhold.poisonAmplifier` | `0` | 0 to 9 | The poison's level (0 = Poison I). |
| `darkhold.poisonCooldownTicks` | `40` | 0 to 72000 | Ticks before the same attacker can be poisoned again, so a beam does not renew it every tick. |
| `darkhold.markRange` | `24` | 2 to 128 | How far (in blocks) the hex mark reaches (Sneak + use on the creature you look at). |
| `darkhold.markTicks` | `200` | 20 to 72000 | How long a hex mark lasts, in ticks (200 = 10 seconds): the creature glows, is slowed and takes extra damage from the one who marked it. |
| `darkhold.markSlowAmplifier` | `1` | 0 to 9 | The Slowness of a marked creature (0 = Slowness I). |
| `darkhold.markDamageBonus` | `0.5` | 0.0 to 10.0 | Extra damage the marker does to a marked creature (0.5 = half as much again). |
| `darkhold.markCooldown` | `100` | 0 to 72000 | Cooldown of the hex mark, in ticks. |
| `darkhold.markExhaustion` | `2.0` | 0.0 to 40.0 | Food exhaustion of a hex mark (4.0 = 1 food point). Creative players pay nothing. |
| `darkhold.glimpseRange` | `24` | 2 to 128 | How far (in blocks) the mind glimpse sees (Sneak + use while looking at no creature). |
| `darkhold.glimpseTicks` | `100` | 20 to 72000 | How long the creatures glow after a mind glimpse, in ticks (100 = 5 seconds). |
| `darkhold.glimpseMax` | `16` | 1 to 64 | The most creatures one mind glimpse shows (the nearest ones). |
| `darkhold.glimpseCooldown` | `200` | 0 to 72000 | Cooldown of the mind glimpse, in ticks. |
| `darkhold.glimpseExhaustion` | `1.0` | 0.0 to 40.0 | Food exhaustion of a mind glimpse (4.0 = 1 food point). Creative players pay nothing. |

## Structures

The four stone sites of 3.6 can each be switched off: see [The stone sites](#the-stone-sites). The garrison of the Hydra outposts is the section [`[hydra]`](#hydra-soldiers). The three structures of 3.3 ([Hydra outposts, Mjolnir's craters and the Mutant School](Structures)) have **no server option**: they are data, so a data pack tunes them. `data/enhanced/worldgen/structure_set/` holds their spacing and separation (`hydra_outposts` 36 / 12 chunks, `mjolnir_craters` 32 / 10, `mutant_schools` 40 / 14; outposts and craters keep 6 chunks from a school), the biome tags `data/enhanced/tags/worldgen/biome/has_structure/hydra_outpost.json`, `mjolnir_crater.json` and `mutant_school.json` say where they stand (empty a tag to stop that structure in new chunks), `data/enhanced/worldgen/structure/` holds the structures themselves (`max_slope`, the crater's `radius` and `depth`), `data/enhanced/structure/` the building templates, and `data/enhanced/loot_table/chests/hydra_outpost/` and `chests/mutant_school/` what the chests hold.

## Datapack tags

Item, block, entity type and damage type tags under `data/enhanced/tags/` decide what the powers affect, so a datapack can change them without touching the mod.

| Tag | Kind | What it decides |
|---|---|---|
| `enhanced:magnetic` | item | Items that count as metal (iron, gold, copper, netherite and everything made with them): what Magneto lifts when dropped, takes from a hand, and pulls. |
| `enhanced:magnetic_armor` | item | Armor that makes its wearer a target of Magneto's telekinesis, and that Armor & Weapon Strip takes. |
| `enhanced:shrapnel_ammo` | item | Iron and chainmail tools, weapons and armor, and other iron items (shears, buckets, chains, bars, doors), that the Shrapnel Storm can melt into shards (18 each). |
| `enhanced:weapons` | item | What Tommy's Disarm Blitz takes from players. |
| `enhanced:cannot_be_snatched` | item | Items that speedsters can't snatch (Mjolnir, the Infinity Gauntlet and Ultron's Core). |
| `enhanced:magnetic_entities` | entity type | Creatures and vehicles made of metal that Magneto can lift (the Iron Golem, every minecart, the Sentries, and the Vision of a Hex, whom only Magneto can lift). |
| `enhanced:metallic_projectiles` | entity type | Projectiles the Magnetic Shield catches, and Magneto can catch in flight (arrows of every kind, tridents, iron shrapnel). |
| `enhanced:telekinesis_immune` | entity type | Creatures no telekinesis can lift: the Ender Dragon (too vast), the fragments of a shattered Mjolnir, the Vision of a Hex (only Magneto can), the Vibranium Key and a cocoon of sand. The three bosses of 3.0, Thanos's weapons and the Wither, the Warden and the Elder Guardian are not here any more: they can be held as bosses (see `boss.telekinesisHoldsBosses` and `boss.telekinesisHoldsVanillaBosses`). |
| `enhanced:held_as_bosses` | entity type | The Wither, the Warden and the Elder Guardian: telekinesis holds them as bosses, with no arena (`boss.vanillaHoldTicks`). |
| `enhanced:pull_immune` | entity type | What the villain heroes' own pulls (Ultron's Gravity Pull, Thanos's Space Pull) leave alone: everything in `telekinesis_immune` and `held_as_bosses`, the three bosses and Thanos's sword, meteors and orbs. Their pulls work as they did in 3.0. |
| `enhanced:conjured_bodies` | entity type | Bodies that are copies (Loki's Duplicates): they have no real metal, so Magneto's strip or pull dissolves them and Tommy's Disarm Blitz takes nothing from them. |
| `enhanced:hex_resistant` | entity type | Bosses the Hex wall can't kill: it throws them back instead (and the Vision of another Hex). |
| `enhanced:spellbound_immune` | entity type | Creatures Billy's Freeze can't hold (the Ender Dragon, the Wither and the three bosses of 3.0). |
| `enhanced:pin_immune` | entity type | Creatures Mjolnir can't pin down (the Ender Dragon, the Wither, the Warden and the three bosses of 3.0). |
| `enhanced:confused_immune` | entity type | Creatures Loki's Scepter can't confuse (the Ender Dragon, the Wither, the Warden and the three bosses of 3.0). |
| `enhanced:shapeshift_immune` | entity type | Creatures Loki can't take the shape of. |
| `enhanced:telepathy_immune` | entity type | Creatures too vast for a mind to be read, taken over or held by the Phoenix or by Professor X (the Ender Dragon, the Wither, the Warden and the 3.0 bosses). |
| `enhanced:web_immune` | entity type | Creatures Spider-Man's webs can't wrap (the bosses). Spider-Man himself, Creative and Spectator players, and players when PvP is off are never wrapped, whatever this tag says. |
| `enhanced:instant_kill_immune` | entity type | Creatures Instant Kill spares (the bosses and their minions: they take only the legs' normal damage). |
| `enhanced:bosses` | entity type | The bosses: the Ender Dragon, the Wither, the Warden, the Elder Guardian, Thanos, Ultron and Apocalypse. Webs and the Instant Kill do not take hold of them, an area attack always counts them as enemies, and the boss fights read it (whoever kills one struck the last blow). |
| `enhanced:boss_minions` | entity type | What the boss fights bring with them (Ultron's Sentries; Thanos's illusions, sword, meteors and orbs): the Instant Kill spares them, and an area attack always counts them as enemies. |
| `enhanced:entomb_immune` | entity type | Creatures Apocalypse's sand can't swallow in a cocoon (the Ender Dragon, the Wither, the Warden, the Elder Guardian and Apocalypse himself); a creature in `enhanced:spellbound_immune` slips out of it too. |
| `enhanced:reality_proof` | entity type | Projectiles the Reality Veil (the Mad Titan's and a Thanos player's) and, since 3.6, the [Reality Stone](Infinity-Stones#the-reality-stone-red)'s bubbles do not turn into bubbles: a thrown Mjolnir or Stormbreaker, Mjolnir's fragments, a thrown Gungnir, and the Mad Titan's own sword, meteors and orbs. (A Hydra bolt is not in it: the stone bubbles it.) |
| `enhanced:infinity_stones` | item | 3.6: the six Infinity Stones. |
| `enhanced:hydra` | entity type | 3.6: the Hydra soldier and the Hydra officer: they never hit each other, and an area attack of theirs spares their side. |
| `enhanced:grab_immune` | entity type | 3.6: what the Hulk's Grab Smash can't pick up (it gets one heavy punch instead): the bosses, the Stonekeeper and the Asgardians. |
| `enhanced:hulk_soft` | block | 3.6: what the Hulk breaks with `hulk.blockBreaking` = `SOFT`: glass and panes, leaves, snow, ice, small flowers, grass and ferns, vines, cobwebs, dead bushes. |
| `enhanced:hulk_smashable` | block | 3.6: what he also breaks with `ALL`: dirt, sand, gravel, stone and cobblestone, planks and logs, bricks, clay, netherrack, end stone... |
| `enhanced:hulk_unbreakable` | block | 3.6: what he never breaks: everything Build Magic never touches, obsidian, end portal frames, spawners, reinforced deepslate, the stone pedestals, the Tesseract's cradle, Hela's Seal and the Gamma Emitter. |
| `enhanced:on_avengers_tower_maps` | structure | 3.6: where the Hydra Intel map leads (Avengers Tower). |
| `enhanced:ultron_robots` | entity type | Ultron and his Sentries: lightning does `ultron.lightningFactor` (1.25) times to them, and the Mind Stone beam `ultron_boss.visionBeamBonus` (1.5) times. |
| `enhanced:telekinesis_immovable` | block | Blocks no telekinesis can tear out of the ground. |
| `enhanced:magnetic_blocks` | block | Metal blocks: what Magneto's R tears out and his Magnetic Pull takes whole (it holds `magnetic_ores`, the storage blocks, anvils, rails, chains, bars, doors, lanterns, hoppers, pistons, copper blocks and more; other mods' metals are optional entries). |
| `enhanced:magnetic_ores` | block | Ores whose metal the Magnetic Pull takes out (the ore turns into its stone). Iron, gold, copper, nether gold, ancient debris and gilded blackstone, and other mods' tin, lead, silver and so on. |
| `enhanced:build_untouchable` | block | Blocks Build Magic never touches: bedrock-like blocks, portals, spawners, vaults and Mjolnir lying as a block. |
| `enhanced:build_unconjurable` | block | Blocks Build Magic never makes (and may still vanish or move): everything untouchable, and budding amethyst, the dragon egg, infested blocks, frosted ice, Spider-Man's web and a few more. |
| `enhanced:build_volatile` | block | TNT, lava and fire: Billy never makes them, and Wanda at most `build_magic_wanda.volatilePerSpell` in one spell. |
| `enhanced:build_ground` | block | What counts as ground (dirt, stone, sand, ores, gravel, clay, snow, ice...): a double click on a block that is not ground selects the whole built thing, and Land moves only ground. |
| `enhanced:build_plants` | block | Logs, leaves, flowers, grass, vines, mushrooms and the like: what Vanish's "Plants and trees" style removes. |
| `enhanced:build_copies_data` | block entity type | The block entities whose data a copy keeps (signs, banners, chests, barrels and so on). Any other copies as the bare block. |
| `enhanced:build_holds_items` | block entity type | Those of them that hold items (chests, barrels, hoppers, furnaces...): Billy's copies of them come out empty, and a spell leaves a full one standing. |
| `enhanced:cannot_cling` | block | Blocks Spider-Man cannot stick to (empty by default; water, cobwebs, vines and ladders never hold him anyway). |
| `enhanced:ritual_ground` | block | What the Capstone must be planted on to wake Apocalypse (sand, red sand and every sandstone), while `apocalypse_boss.ritualNeedsSand` is on. |
| `enhanced:sokovia_liftable` | block | The natural blocks the island of Sokovia copies into the sky (stone, dirt, sand, ores, grass, leaves, logs, flowers, snow...); anything else, a house or a chest, is left out of the copy. |
| `enhanced:ward_immune` | entity type | What the ward of the Book of the Vishanti leaves alone: since 3.6 the Stonekeeper of Vormir (he is also in `telekinesis_immune`, `pull_immune` and `confused_immune`). **Empty in 3.3** (in 3.2 it held the bosses and their helpers, which a ward now keeps out): a data pack may add creatures. Pets with an owner, riders, named and leashed monsters and curing zombie villagers are spared anyway; bosses and fight members never are. |
| `enhanced:dark_magic` | damage type | 3.3: what counts as dark magic, which the ward keeps out: the Darkhold's purple beam (Agatha's too), Agatha's Power Drain and her Rune Trap. |
| `enhanced:dark_magic` | entity type | 3.3: projectiles that count as dark magic (empty by default, for data packs). |
| `enhanced:magic_damage` | damage type | What counts as magic used on the holder of the Darkhold, so that its revenge poisons the attacker: the Hex Bolt and Beam, the Chaos Beam, the telekinetic slam, the Mind Stone, the Stones' powers, the Phoenix Burst, the purple beam, Apocalypse's wall and Entomb, Professor X's Mental Blast, Agatha's Power Drain and Rune Trap, and the game's own `magic` and `indirect_magic`. Thor's lightning and Cyclops's optic blasts are not in it. |
| `enhanced:mind_power` | damage type | Damage that counts as a mind's power: the Chaos Beam, a telekinetic slam and the Phoenix Burst. It is what wears down Apocalypse's shield, when a hero in `apocalypse_shield.mindPowerHeroes` deals it. |
| `enhanced:stone_born` | damage type | Damage that counts three times toward breaking the Mad Titan's clench, and that his Space Barrier lets through: the Chaos Beam, Vision's Mind Stone beam and Stormbreaker. |

The recipes (`data/enhanced/recipe/`) and the loot chances (`data/enhanced/loot_modifiers/`) are datapack files too: see [Items and Crafting](Items-and-Crafting).

## Client options: animations, effects and looks

Fourteen options are about your own game only. They are not synced, and a server can't change them. They live in a second file, **`config/enhanced-client.toml`** in your Minecraft folder (the mod creates it the first time the game starts), in the `[animation]`, `[appearance]`, `[build_magic]`, `[spider_man]`, (3.5) `[music]` and (3.6) `[hulk]` sections (`[build_magic]` is for [Build Magic](Build-Magic); reduced motion calms its animations and the Magnetic Pull's streams too). The easiest way to change them is in the game: **Mods → Enhanced → Config**. You can also change a value in any text editor while the game is closed:

```toml
[animation]
    animations = true
    reducedMotion = false
    flightEffects = true

[appearance]
    hideHeroArmour = true

[build_magic]
    hints = true
    tips = true
    ghostDetail = 32768
    seeThrough = true
    showOthers = true

[spider_man]
    webAssist = true
    webAssistPreview = true

[music]
    quicksilverSong = "theme"
    ignoreServerMusic = false

[hulk]
    hulkModel = true
```

| Option | Setting in the file | Default | What it does |
|---|---|---|---|
| Hero animations | `animations` | `true` | The heroes' blended poses, flight, running, abilities and Thor's weapon in his fist. Off: only the usual Minecraft poses. |
| Reduced motion | `reducedMotion` | `false` | A calmer animation: the hover bob and drift at a third, no trembling hands, jitter or camera shake, and fainter rings. |
| Flight effects | `flightEffects` | `true` | Ribbons, rings, wind streaks and shockwaves around flying heroes, and the little flourishes of some abilities. Off: only a few trail particles are left. |
| Hide heroes' armour | `hideHeroArmour` | `true` | Players with a power are drawn without their armour (helmet, chestplate, leggings and boots, with their trims and dye): yours in third person, everyone else's, and in the inventory preview, so it doesn't hide the characters. The armour still protects as usual, and a player with no power looks as they always did. The elytra, a carved pumpkin or a mob head, and the mod's own hero gear (Magneto's Helmet, Wanda's Crown, Quicksilver's Goggles) are always drawn. Off: heroes show their armour like everyone else. |
| Build Magic hints | `hints` | `true` | The strip of mouse and key pictures at the bottom right while you build. |
| Build Magic tips | `tips` | `true` | The little animated tips the first times a spell is used. |
| Preview detail | `ghostDetail` | `32768` | The most blocks drawn one by one in a spell's preview (0 to 262144): a bigger one is drawn as its outside only, then as a tinted box. |
| See the box through walls | `seeThrough` | `true` | The box shows faintly through hills and walls. |
| Other builders | `showOthers` | `true` | See the boxes of other builders near you. |
| Web Assist | `webAssist` | `true` | 3.3: Spider-Man's [Web Assist](Spider-Man#web-assist-easy-swinging-33): the swing chooses its own anchor ahead and above (trees count), keeps clear of the ground and the leaves, lets go at the best moment and swings on while G is held. Off: the manual swing, aimed with the crosshair. The **J** key switches it too. |
| Web Assist preview | `webAssistPreview` | `true` | Show where Web Assist's next web would go (a small white web) while you are in the air or sneaking. |
| Quicksilver's song | `quicksilverSong` | `theme` | 3.5: what Quicksilver's Walkman plays: `theme` (his own song, "Silver Hours") or `sweet_dreams` (*Sweet Dreams (Are Made of This)*, as before 3.5). A song of your own, or the world's, plays instead of either. See [Music](Music). |
| The Hulk's body | `hulkModel` | `true` | 3.6: the Hulk is drawn with his own giant body. Off: the normal player model wearing his skin, made bigger (if another mod's player model clashes with his). See [Hulk](Hulk#looks-and-how-he-moves). |
| Ignore the world's music | `ignoreServerMusic` | `false` | 3.5: don't download or play the songs a server or world puts in its music folder; hear the built-in music, or your own. |

Two more client options, for the music and the air of the Thanos fight, are in a `[thanos]` section of the same file: see [above](#your-own-game-the-music-and-the-atmosphere-of-the-fight).

## Related

* [Commands](Commands): `/enhanced power`, `/enhanced look` and `/enhanced hex`, for operators.
* [Thor](Thor): how `thor.anyoneWorthy` and `thor.mjolnirOwnerOnly` change who can lift Mjolnir.

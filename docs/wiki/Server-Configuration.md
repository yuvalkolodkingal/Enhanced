# Server Configuration

Every balance number of the mod lives in one file, **`world/serverconfig/enhanced-server.toml`** in the world (or server) folder. The mod creates it when the world is first loaded, with every option at its default, and syncs it to the clients. It is a plain text file: open it in any editor, change a value and restart the world or the server.

The file has one section per hero (a boss has a section for his fight and one for his hero, and `[boss]` holds the rules every boss shares; [Build Magic](#build-magic) has three), and an option is written as `section.key`. For example, Wanda's Hex death chance is the `hexDeathChance` key in the `[scarlet_witch]` section:

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
| `scarlet_witch.chaosShieldExhaustion` | `0.1` | 0.0 to 4.0 | Food exhaustion per tick while the Chaos Shield is held (4.0 exhaustion = 1 food point, so 0.1 is about 1 food point every 2 seconds). |
| `scarlet_witch.chaosShieldExplosionFactor` | `0.25` | 0.0 to 1.0 | Fraction of explosion damage that still gets through the Chaos Shield (0.25 = 75% less). |
| `scarlet_witch.chaosShieldWindUpTicks` | `15` | 0 to 200 | How long the Chaos Shield takes to form after B is pressed, in ticks (15 = 3/4 of a second). It blocks nothing until it is complete. Her raise (arms and effects) keeps time with it. |
| `scarlet_witch.chaosShieldMaxTicks` | `160` | 1 to 6000 | How long a complete Chaos Shield can be held before it drops, in ticks (160 = 8 seconds). |
| `scarlet_witch.chaosShieldStrength` | `30.0` | 1.0 to 1000.0 | Points of damage the Chaos Shield can stop before it shatters. Everything it stops wears it down: the damage of a projectile, and the explosion damage it takes away. |
| `scarlet_witch.chaosShieldCooldown` | `120` | 0 to 6000 | Ticks before the Chaos Shield can be raised again after it drops (120 = 6 seconds). |
| `scarlet_witch.chaosShieldBrokenCooldown` | `300` | 0 to 6000 | Ticks before the Chaos Shield can be raised again after it shattered or was held to its limit (300 = 15 seconds). |
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
| `quicksilver.slowMotionFactor` | `5` | 2 to 20 | During Quicksilver Time the world around him runs this many times slower. |
| `quicksilver.slowMotionRadius` | `32` | 4 to 128 | How far (in blocks) from Quicksilver time slows down. |
| `quicksilver.speedBonus` | `3.0` | 0.0 to 10.0 | Extra movement speed while he runs in Quicksilver Time or Super Speed (3.0 = four times as fast). |
| `quicksilver.exhaustion` | `0.03` | 0.0 to 4.0 | Food exhaustion per tick while he runs in Quicksilver Time or Super Speed (4.0 = 1 food point). Quicksilver Time also costs half of this while he is not running. |
| `quicksilver.actionSpeed` | `4.0` | 1.0 to 10.0 | While Quicksilver Time or Super Speed is on he mines, attacks, eats, drinks, draws bows, loads crossbows, swims and places blocks this many times as fast (4.0 = four times as fast). |

## Billy (Wiccan)

Section `[billy]`. The hero's page is [Billy](Billy-and-the-Witches-Road). His block lifting (3.1) is here; his Build Magic numbers are in [Build Magic](#build-magic).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `billy.flightSpeed` | `0.08` | 0.01 to 0.5 | Billy's flying speed (Creative flight is 0.05). |
| `billy.hexBoltDamage` | `6.0` | 0.0 to 100.0 | Damage of a Hex Bolt to the creature it strikes. Each creature it forks to takes half. |
| `billy.freezeDuration` | `100` | 1 to 1200 | How long, in ticks, Billy's Freeze holds creatures spellbound (100 = 5 seconds). |
| `billy.noiseRange` | `32` | 4 to 64 | How far away (in blocks) Billy hears the thoughts of creatures while The Noise is on. |
| `billy.blockLifting` | `true` | `true` or `false` | Whether Billy's telekinesis tears blocks out of the ground and sets them down. If `false`: creatures, items and projectiles only (as in 3.0). |
| `billy.blockLiftRange` | `16` | 2 to 128 | How far away (in blocks) Billy can tear a block out of the ground. |
| `billy.blockLiftMaxHardness` | `3.0` | 0.0 to 100.0 | The hardest block Billy can lift (stone = 1.5, ores = 3, iron block = 5). |
| `billy.blockLiftCooldown` | `20` | 0 to 200 | Ticks between two blocks Billy tears out (catching anything else never waits). |

## Build Magic

Sections `[build_magic]`, `[build_magic_wanda]` and `[build_magic_billy]`. The page is [Build Magic](Build-Magic). In a comment, an option of Billy's or Wanda's is always written with its section (`build_magic_billy.meter`), never as `billy.x`, which is the `[billy]` section of his other powers. Billy's numbers are those of his first rank, an Apprentice: the ranks grow from them. A family that wants him as strong as his mother sets `build_magic_billy.maxRank` to 4, the three practice numbers to 0 and a large meter. Every value is read when it is used, so editing the file changes it live.

### `[build_magic]`: every builder

**The undo history** is written to the world folder before each spell changes the world (so a crash leaves it undoable), one folder for each player. While a spell works, its step is kept whole in memory at about 14 bytes a block; a spell whose step would not fit in `undoMemoryMb` (or half of `serverUndoMemoryMb`) is kept as a *lite step*, only a bit for each block in memory, and its records on disk are the step. That needs `undoSurvivesRestart` and a record that fits `undoDiskMb` (about half a byte a block); otherwise the spell says *"Too big to remember"* and runs only if the player holds the right button, and without an undo. A Move is never lite: it is refused. At the defaults the line between whole and lite is about 4 million blocks. The caps are looked at once a second and when a spell ends. `/enhanced build forget` clears a player's.

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
| `build_magic.hexIsAClaim` | `false` | `true` or `false` | Inside a Hex, only its owner and the players she allowed may build with Build Magic or pull with the Magnetic Pull. (Telekinesis on one block asks the claims as a hand does, and is not covered.) |
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

### `[build_magic_billy]`: Billy, and how he learns

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `build_magic_billy.enabled` | `true` | `true` or `false` | Billy has Build Magic. |
| `build_magic_billy.meter` | `512` | 16 to 65536 | His magic meter (in blocks) as an Apprentice; it doubles with each rank. |
| `build_magic_billy.refill` | `16.0` | 0.0 to 4096.0 | Blocks of magic he gets back each second as an Apprentice; half of it more each rank. |
| `build_magic_billy.maxSide` | `24` | 1 to 256 | The longest side of anything he builds, copies or selects as an Apprentice; +8 each rank. |
| `build_magic_billy.reach` | `24` | 2 to 128 | How far (in blocks) he points as an Apprentice; +8 each rank. |
| `build_magic_billy.undoSteps` | `10` | 1 to 100 | Spells he can undo as an Apprentice; +2 each rank. |
| `build_magic_billy.repeatMax` | `8` | 1 to 64 | The most copies in a Repeat as an Apprentice; +4 each rank. |
| `build_magic_billy.rank2Practice` | `2000` | 0 to 10000000 | Blocks of magic he must cast to become a Spellcaster (rank 2). |
| `build_magic_billy.rank3Practice` | `8000` | 0 to 10000000 | Blocks of magic he must cast to become a Sorcerer (rank 3). |
| `build_magic_billy.rank4Practice` | `30000` | 0 to 10000000 | Blocks of magic he must cast to become Wiccan (rank 4). |
| `build_magic_billy.maxRank` | `4` | 1 to 4 | The highest rank he can reach (1 = he never grows). |
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
| `thor.bifrostCooldown` | `1200` | 0 to 72000 | Cooldown of the Bifrost, Thor's or anyone's who wields Stormbreaker or Mjolnir, in ticks (1200 = 60 seconds). |
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
| Boss music | `bossMusic` | `true` | The music of the fight while a Thanos is near: five layers in D minor that play together, one for each stone he has (a drone and a heartbeat, then low strings, a high choir, a driving ostinato and bells), while the game's own music waits (it follows the music volume). Off: silence (the sounds of his moves stay). |
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

Sections `[apocalypse_shield]`, `[apocalypse_boss]` and `[apocalypse]`. The fight itself and the hero are described in the Apocalypse page; the numbers shared by every boss (how many fights run at once, the wipe and abandon clocks, the reward's minimum time, the experience, the time limit, the protection of the arena) are in `[boss]`.

### `[apocalypse_shield]`: what counts as mind power

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `apocalypse_shield.mindPowerHeroes` | `["scarlet_witch", "phoenix"]` | a list of power names | Heroes whose mind power can wear down Apocalypse's shield (the boss's and the hero's). Add `"billy"` or `"vision"` to count theirs (and add their damage types to the datapack tag `enhanced:mind_power`). |
| `apocalypse_shield.chaosBeamFactor` | `1.0` | 0.0 to 20.0 | Shield strength removed per point of Chaos Beam damage. |
| `apocalypse_shield.slamFactor` | `1.0` | 0.0 to 20.0 | Per point of telekinetic slam damage (a thing thrown by Wanda or the Phoenix). |
| `apocalypse_shield.borrowedSlamFactor` | `0.5` | 0.0 to 20.0 | A block borrowed from his own arena and thrown by mind power wears the shield by this many times what `slamFactor` gives. The critical factor comes on top. |
| `apocalypse_shield.phoenixBurstFactor` | `5.0` | 0.0 to 50.0 | Per point of Phoenix Burst damage (a full Burst of 30 removes 150). |
| `apocalypse_shield.otherMindFactor` | `1.0` | 0.0 to 20.0 | Per point of any other damage type in `enhanced:mind_power`. |
| `apocalypse_shield.phoenixMindControlStrain` | `25.0` | 0.0 to 1000.0 | Shield strength a Phoenix's Mind Control (even refused) removes. |
| `apocalypse_shield.phoenixPsionicStrain` | `60.0` | 0.0 to 1000.0 | Shield strength a Phoenix's Psionic Field that reaches him removes. |
| `apocalypse_shield.shieldRegenDelayTicks` | `160` | 0 to 2400 | The boss's shield regenerates only after this many ticks without mind-power damage. |
| `apocalypse_shield.shieldRegenFraction` | `0.03` | 0.0 to 1.0 | Fraction of its strength it regains each second. |
| `apocalypse_shield.mindHitCooldownTicks` | `10` | 1 to 40 | Ticks between two mind-power hits from the same attacker that the shield will count (the Chaos Beam hits every tick; a shield leaves no invulnerability frames, so it keeps its own). |
| `apocalypse_shield.critHeroes` | `["scarlet_witch", "phoenix"]` | a list of power names | Heroes whose mind power hits the boss Apocalypse critically, on his shield and on his body (only the boss, never the Apocalypse hero). Kept apart from `mindPowerHeroes`: a hero added there doesn't get critical hits too. |
| `apocalypse_shield.critShieldFactor` | `2.0` | 1.0 to 10.0 | Their mind power (and the Phoenix's Mind Control and Psionic Field strains) wears the boss's shield this many times as much. |
| `apocalypse_shield.critBodyFactor` | `2.0` | 1.0 to 10.0 | Their mind power hurts the boss's body this many times as much when his shield is down. |
| `apocalypse_shield.othersBodyFactor` | `0.4` | 0.05 to 1.0 | Every other blow to the boss's body is multiplied by this when his shield is down. |

### `[apocalypse_boss]`: the fight

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
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
| `apocalypse_boss.shieldStrength` | `150.0` | 10.0 to 10000.0 | Strength of shield 1 for one fighter. |
| `apocalypse_boss.shieldGrowth` | `1.25` | 1.0 to 3.0 | Each later shield is this many times stronger. |
| `apocalypse_boss.shieldPerExtraPlayer` | `0.35` | 0.0 to 3.0 | Extra shield strength per additional fighter (a fraction). |
| `apocalypse_boss.windowTicks` | `200` | 40 to 2400 | How long a broken shield leaves him vulnerable (200 = 10 s). |
| `apocalypse_boss.staggerTicks` | `60` | 0 to 400 | How long he is staggered and attacks nothing at the start of a window. |
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
| `apocalypse_boss.boulderDamage` | `11.0` | 0.0 to 100.0 | A Sand Boulder. |
| `apocalypse_boss.backhandDamage` | `16.0` | 0.0 to 100.0 | The Backhand. |
| `apocalypse_boss.portalShockDamage` | `6.0` | 0.0 to 100.0 | The shockwave where he arrives. |
| `apocalypse_boss.stormDamage` | `2.0` | 0.0 to 100.0 | A Tornado's damage every 10 ticks. |
| `apocalypse_boss.tornadoTicks` | `160` | 20 to 1200 | How long a Tornado lasts. |
| `apocalypse_boss.ringDamage` | `12.0` | 0.0 to 100.0 | A ring of the Cairo Collapse. |
| `apocalypse_boss.entombTicks` | `100` | 10 to 1200 | How long the Entomb holds. |
| `apocalypse_boss.entombDamagePerSecond` | `1.5` | 0.0 to 20.0 | Damage while entombed (never below 2 hearts). |
| `apocalypse_boss.cocoonHealth` | `20.0` | 1.0 to 200.0 | Health of the boss's cocoon. |
| `apocalypse_boss.mindCrushRange` | `28` | 8 to 64 | Blocks. |
| `apocalypse_boss.mindCrushNauseaTicks` | `120` | 0 to 1200 | Nausea of the Mind Crush (Slowness 3 s and Blindness 2 s go with it). |
| `apocalypse_boss.horsemen` | `true` | `true` or `false` | He calls the Four Horsemen at Transition 1. |
| `apocalypse_boss.horsemenMax` | `4` | 0 to 8 | The most Horsemen (`min(max, 1 + fighters)`). |
| `apocalypse_boss.horsemanHealthFactor` | `2.0` | 1.0 to 10.0 | Husk health multiplier. |
| `apocalypse_boss.horsemanDamageBonus` | `4.0` | 0.0 to 50.0 | Added attack damage. |
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
| `enhanced:telepathy_immune` | entity type | Creatures too vast for a mind to be read, taken over or held by the Phoenix (the Ender Dragon, the Wither, the Warden and the 3.0 bosses). |
| `enhanced:web_immune` | entity type | Creatures Spider-Man's webs can't wrap (the bosses). Spider-Man himself, Creative and Spectator players, and players when PvP is off are never wrapped, whatever this tag says. |
| `enhanced:instant_kill_immune` | entity type | Creatures Instant Kill spares (the bosses and their minions: they take only the legs' normal damage). |
| `enhanced:bosses` | entity type | The bosses: the Ender Dragon, the Wither, the Warden, the Elder Guardian, Thanos, Ultron and Apocalypse. Webs and the Instant Kill do not take hold of them, an area attack always counts them as enemies, and the boss fights read it (whoever kills one struck the last blow). |
| `enhanced:boss_minions` | entity type | What the boss fights bring with them (Ultron's Sentries; Thanos's illusions, sword, meteors and orbs): the Instant Kill spares them, and an area attack always counts them as enemies. |
| `enhanced:entomb_immune` | entity type | Creatures Apocalypse's sand can't swallow in a cocoon (the Ender Dragon, the Wither, the Warden, the Elder Guardian and Apocalypse himself); a creature in `enhanced:spellbound_immune` slips out of it too. |
| `enhanced:reality_proof` | entity type | Projectiles the Reality Veil (the Mad Titan's and a Thanos player's) does not turn into bubbles: a thrown Mjolnir or Stormbreaker, Mjolnir's fragments, and the Mad Titan's own sword, meteors and orbs. |
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
| `enhanced:mind_power` | damage type | Damage that counts as a mind's power: the Chaos Beam, a telekinetic slam and the Phoenix Burst. It is what wears down Apocalypse's shield, when a hero in `apocalypse_shield.mindPowerHeroes` deals it. |
| `enhanced:stone_born` | damage type | Damage that counts three times toward breaking the Mad Titan's clench, and that his Space Barrier lets through: the Chaos Beam, Vision's Mind Stone beam and Stormbreaker. |

The recipes (`data/enhanced/recipe/`) and the loot chances (`data/enhanced/loot_modifiers/`) are datapack files too: see [Items and Crafting](Items-and-Crafting).

## Client options: animations, effects and looks

Nine options are about your own game only. They are not synced, and a server can't change them. They live in a second file, **`config/enhanced-client.toml`** in your Minecraft folder (the mod creates it the first time the game starts), in the `[animation]`, `[appearance]` and `[build_magic]` sections (the last is for [Build Magic](Build-Magic); reduced motion calms its animations and the Magnetic Pull's streams too). The easiest way to change them is in the game: **Mods → Enhanced → Config**. You can also change a value in any text editor while the game is closed:

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

Two more client options, for the music and the air of the Thanos fight, are in a `[thanos]` section of the same file: see [above](#your-own-game-the-music-and-the-atmosphere-of-the-fight).

## Related

* [Commands](Commands): `/enhanced power`, `/enhanced look` and `/enhanced hex`, for operators.
* [Thor](Thor): how `thor.anyoneWorthy` and `thor.mjolnirOwnerOnly` change who can lift Mjolnir.

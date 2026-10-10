# Server Configuration

Every balance number of the mod lives in one file, **`world/serverconfig/enhanced-server.toml`** in the world (or server) folder. The mod creates it when the world is first loaded, with every option at its default, and syncs it to the clients. It is a plain text file: open it in any editor, change a value and restart the world or the server.

The file has one section per hero, and an option is written as `section.key`. For example, Wanda's Hex death chance is the `hexDeathChance` key in the `[scarlet_witch]` section:

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
| `magneto.telekinesisRange` | `32` | 4 to 128 | How far away (in blocks) Magneto and Wanda can grab a target with telekinesis. |
| `magneto.throwSpeed` | `2.6` | 0.5 to 6.0 | Launch speed, in blocks per tick, of a telekinetic throw. |
| `magneto.shrapnelDamage` | `4.0` | 0.0 to 100.0 | Damage of one iron shrapnel shard. |
| `magneto.shrapnelPierce` | `3` | 1 to 16 | How many creatures one shard can pierce through. |
| `magneto.shrapnelFireInterval` | `2` | 1 to 20 | Ticks between shards while the Shrapnel Storm is held (2 = 10 shards per second). |
| `magneto.disarmRange` | `32` | 4 to 128 | Range of Armor & Weapon Strip, in blocks. |
| `magneto.disarmCooldown` | `60` | 0 to 6000 | Cooldown of Armor & Weapon Strip, in ticks (60 = 3 seconds). |
| `magneto.shieldReflectChance` | `0.5` | 0.0 to 1.0 | Chance that a metal projectile caught by the Magnetic Shield flies back at the shooter (otherwise it drops). |

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
| `scarlet_witch.blockLiftRange` | `24` | 2 to 128 | How far away (in blocks) Wanda can tear a block out of the ground. |
| `scarlet_witch.blockLiftMaxHardness` | `5.0` | 0.0 to 100.0 | Hardest block Wanda can lift (stone = 1.5, iron block = 5, obsidian = 50). |
| `scarlet_witch.hexDefaultRadius` | `32` | 4 to 256 | Radius, in blocks, of a newly created Hex. |
| `scarlet_witch.hexMinRadius` | `8` | 4 to 256 | Smallest radius Wanda can shrink her Hex to in Hex Control. |
| `scarlet_witch.hexMaxRadius` | `96` | 4 to 256 | Largest radius Wanda can grow her Hex to in Hex Control. |
| `scarlet_witch.hexDeathChance` | `0.95` | 0.0 to 1.0 | Chance that a creature without powers dies when it crosses the Hex wall without Wanda's permission (0.95 = 95%). The rest survive. |

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

Section `[billy]`. The hero's page is [Billy](Billy-and-the-Witches-Road).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `billy.flightSpeed` | `0.08` | 0.01 to 0.5 | Billy's flying speed (Creative flight is 0.05). |
| `billy.hexBoltDamage` | `6.0` | 0.0 to 100.0 | Damage of a Hex Bolt to the creature it strikes. Each creature it forks to takes half. |
| `billy.freezeDuration` | `100` | 1 to 1200 | How long, in ticks, Billy's Freeze holds creatures spellbound (100 = 5 seconds). |
| `billy.noiseRange` | `32` | 4 to 64 | How far away (in blocks) Billy hears the thoughts of creatures while The Noise is on. |

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

## Thor (Mjolnir, Stormbreaker and the Bifrost)

Section `[thor]`. The hero's page is [Thor](Thor).

| Option | Default | Allowed values | Meaning |
|---|---|---|---|
| `thor.flightSpeed` | `0.11` | 0.01 to 0.5 | Thor's flying speed while he holds Mjolnir or Stormbreaker (Creative flight is 0.05). |
| `thor.anyoneWorthy` | `false` | `true` or `false` | If `true`, everyone is worthy of Mjolnir. If `false` (the default), only Thor, Vision, Creative and Spectator players and those who have earned the *Hero of the Village* advancement are worthy: they can claim a Mjolnir nobody owns yet, and lift it. |
| `thor.mjolnirOwnerOnly` | `true` | `true` or `false` | If `true` (the default), each Mjolnir belongs to the first worthy player who lifts it, and only they can lift it from then on (Creative and Spectator players always can, without taking it). If `false`, Mjolnirs are never claimed: the old rule, where any worthy player can lift any Mjolnir. Stormbreaker is never bound to anyone. |
| `thor.throwDamage` | `10.0` | 0.0 to 100.0 | Damage of a thrown Mjolnir, both ways. |
| `thor.stormbreakerThrowDamage` | `12.0` | 0.0 to 100.0 | Damage of a thrown Stormbreaker, both ways. |
| `thor.throwRange` | `40` | 4 to 128 | How far (in blocks) a thrown Mjolnir or Stormbreaker flies before it turns back. |
| `thor.recallRange` | `64` | 4 to 256 | How far away (in blocks) Thor can call Mjolnir or Stormbreaker back from. |
| `thor.lightningDamage` | `8.0` | 0.0 to 100.0 | Damage of Thor's lightning to whatever it strikes. |
| `thor.lightningRange` | `64` | 4 to 256 | How far away (in blocks) Thor can call lightning down. |
| `thor.godOfThunderExhaustion` | `0.04` | 0.0 to 4.0 | Food exhaustion per tick while the God of Thunder is unleashed (4.0 = 1 food point). |
| `thor.bifrostCooldown` | `1200` | 0 to 72000 | Cooldown of the Bifrost, Thor's or anyone's who wields Stormbreaker or Mjolnir, in ticks (1200 = 60 seconds). |

## Datapack tags

Item, block and entity tags under `data/enhanced/tags/` decide what the powers affect, so a datapack can change them without touching the mod.

| Tag | Kind | What it decides |
|---|---|---|
| `enhanced:magnetic` | item | Items that count as metal: what Magneto's telekinesis can lift when dropped, and what Armor & Weapon Strip takes from a hand. |
| `enhanced:magnetic_armor` | item | Armor that makes its wearer a target of Magneto's telekinesis, and that Armor & Weapon Strip takes. |
| `enhanced:shrapnel_ammo` | item | Iron and chainmail tools, weapons and armor, and other iron items (shears, buckets, chains, bars, doors), that the Shrapnel Storm can melt into shards (18 each). |
| `enhanced:weapons` | item | What Tommy's Disarm Blitz takes from players. |
| `enhanced:cannot_be_snatched` | item | Items that speedsters can't snatch (Mjolnir). |
| `enhanced:magnetic_entities` | entity type | Creatures and vehicles made of metal that Magneto can lift (the Iron Golem, minecarts). |
| `enhanced:metallic_projectiles` | entity type | Projectiles the Magnetic Shield catches. |
| `enhanced:telekinesis_immune` | entity type | Creatures too powerful to lift (the Ender Dragon, the Wither, the Warden, the Elder Guardian). |
| `enhanced:hex_resistant` | entity type | Bosses the Hex wall can't kill: it throws them back instead. |
| `enhanced:spellbound_immune` | entity type | Creatures Billy's Freeze can't hold (the Ender Dragon, the Wither). |
| `enhanced:shapeshift_immune` | entity type | Creatures Loki can't take the shape of. |
| `enhanced:telekinesis_immovable` | block | Blocks Wanda can't tear out of the ground. |

The recipes (`data/enhanced/recipe/`) and the loot chances (`data/enhanced/loot_modifiers/`) are datapack files too: see [Items and Crafting](Items-and-Crafting).

## Related

* [Commands](Commands): `/enhanced power`, `/enhanced look` and `/enhanced hex`, for operators.
* [Thor](Thor): how `thor.anyoneWorthy` and `thor.mjolnirOwnerOnly` change who can lift Mjolnir.

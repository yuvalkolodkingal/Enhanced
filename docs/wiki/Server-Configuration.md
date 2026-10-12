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
| `scarlet_witch.chaosShieldWindUpTicks` | `15` | 0 to 200 | How long the Chaos Shield takes to form after B is pressed, in ticks (15 = 3/4 of a second). It blocks nothing until it is complete. Her raise (arms and effects) keeps time with it. |
| `scarlet_witch.chaosShieldMaxTicks` | `160` | 1 to 6000 | How long a complete Chaos Shield can be held before it drops, in ticks (160 = 8 seconds). |
| `scarlet_witch.chaosShieldStrength` | `30.0` | 1.0 to 1000.0 | Points of damage the Chaos Shield can stop before it shatters. Everything it stops wears it down: the damage of a projectile, and the explosion damage it takes away. |
| `scarlet_witch.chaosShieldCooldown` | `120` | 0 to 6000 | Ticks before the Chaos Shield can be raised again after it drops (120 = 6 seconds). |
| `scarlet_witch.chaosShieldBrokenCooldown` | `300` | 0 to 6000 | Ticks before the Chaos Shield can be raised again after it shattered or was held to its limit (300 = 15 seconds). |
| `scarlet_witch.blockLiftRange` | `24` | 2 to 128 | How far away (in blocks) Wanda can tear a block out of the ground. |
| `scarlet_witch.blockLiftMaxHardness` | `5.0` | 0.0 to 100.0 | Hardest block Wanda can lift (stone = 1.5, iron block = 5, obsidian = 50). |
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

## Datapack tags

Item, block, entity type and damage type tags under `data/enhanced/tags/` decide what the powers affect, so a datapack can change them without touching the mod.

| Tag | Kind | What it decides |
|---|---|---|
| `enhanced:magnetic` | item | Items that count as metal: what Magneto's telekinesis can lift when dropped, and what Armor & Weapon Strip takes from a hand. |
| `enhanced:magnetic_armor` | item | Armor that makes its wearer a target of Magneto's telekinesis, and that Armor & Weapon Strip takes. |
| `enhanced:shrapnel_ammo` | item | Iron and chainmail tools, weapons and armor, and other iron items (shears, buckets, chains, bars, doors), that the Shrapnel Storm can melt into shards (18 each). |
| `enhanced:weapons` | item | What Tommy's Disarm Blitz takes from players. |
| `enhanced:cannot_be_snatched` | item | Items that speedsters can't snatch (Mjolnir). |
| `enhanced:magnetic_entities` | entity type | Creatures and vehicles made of metal that Magneto can lift (the Iron Golem, minecarts). |
| `enhanced:metallic_projectiles` | entity type | Projectiles the Magnetic Shield catches. |
| `enhanced:telekinesis_immune` | entity type | Creatures too powerful to lift (the Ender Dragon, the Wither, the Warden, the Elder Guardian), the fragments of a shattered Mjolnir, and the Vision of a Hex. |
| `enhanced:hex_resistant` | entity type | Bosses the Hex wall can't kill: it throws them back instead (and the Vision of another Hex). |
| `enhanced:spellbound_immune` | entity type | Creatures Billy's Freeze can't hold (the Ender Dragon, the Wither). |
| `enhanced:pin_immune` | entity type | Creatures Mjolnir can't pin down (the Ender Dragon, the Wither, the Warden). |
| `enhanced:confused_immune` | entity type | Creatures Loki's Scepter can't confuse (the Ender Dragon, the Wither, the Warden). |
| `enhanced:shapeshift_immune` | entity type | Creatures Loki can't take the shape of. |
| `enhanced:telepathy_immune` | entity type | Creatures too vast for a mind to be read, taken over or held by the Phoenix (the Ender Dragon, the Wither, the Warden and the 3.0 bosses). |
| `enhanced:web_immune` | entity type | Creatures Spider-Man's webs can't wrap (the bosses). Spider-Man himself, Creative and Spectator players, and players when PvP is off are never wrapped, whatever this tag says. |
| `enhanced:instant_kill_immune` | entity type | Creatures Instant Kill spares (the bosses and their minions: they take only the legs' normal damage). |
| `enhanced:telekinesis_immovable` | block | Blocks Wanda can't tear out of the ground. |
| `enhanced:cannot_cling` | block | Blocks Spider-Man cannot stick to (empty by default; water, cobwebs, vines and ladders never hold him anyway). |
| `enhanced:mind_power` | damage type | Damage that counts as a mind's power: the Chaos Beam, a telekinetic slam and the Phoenix Burst. |

The recipes (`data/enhanced/recipe/`) and the loot chances (`data/enhanced/loot_modifiers/`) are datapack files too: see [Items and Crafting](Items-and-Crafting).

## Client options: animations, effects and looks

Four options are about your own game only. They are not synced, and a server can't change them. They live in a second file, **`config/enhanced-client.toml`** in your Minecraft folder (the mod creates it the first time the game starts), in the `[animation]` and `[appearance]` sections. The easiest way to change them is in the game: **Mods → Enhanced → Config**. You can also change a value in any text editor while the game is closed:

```toml
[animation]
    animations = true
    reducedMotion = false
    flightEffects = true

[appearance]
    hideHeroArmour = true
```

| Option | Setting in the file | Default | What it does |
|---|---|---|---|
| Hero animations | `animations` | `true` | The heroes' blended poses, flight, running, abilities and Thor's weapon in his fist. Off: only the usual Minecraft poses. |
| Reduced motion | `reducedMotion` | `false` | A calmer animation: the hover bob and drift at a third, no trembling hands, jitter or camera shake, and fainter rings. |
| Flight effects | `flightEffects` | `true` | Ribbons, rings, wind streaks and shockwaves around flying heroes, and the little flourishes of some abilities. Off: only a few trail particles are left. |
| Hide heroes' armour | `hideHeroArmour` | `true` | Players with a power are drawn without their armour (helmet, chestplate, leggings and boots, with their trims and dye): yours in third person, everyone else's, and in the inventory preview, so it doesn't hide the characters. The armour still protects as usual, and a player with no power looks as they always did. The elytra, a carved pumpkin or a mob head, and the mod's own hero gear (Magneto's Helmet, Wanda's Crown, Quicksilver's Goggles) are always drawn. Off: heroes show their armour like everyone else. |

## Related

* [Commands](Commands): `/enhanced power`, `/enhanced look` and `/enhanced hex`, for operators.
* [Thor](Thor): how `thor.anyoneWorthy` and `thor.mjolnirOwnerOnly` change who can lift Mjolnir.

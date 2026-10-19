# Phoenix (Jean Grey)

The telepath who carries the Phoenix. She reads the mind of whoever she looks at (and sees what they carry), senses every mind around her through the walls, flies, lifts things and blocks with her mind, takes over a creature for a while, freezes everyone around her in a field of the mind, and, when she lets the cosmic fire go, charges the **Phoenix Burst**: a wave of golden-orange fire in the shape of a firebird that spreads in every direction. Telepathy and telekinesis are **violet**, the fire of the Phoenix is **gold and orange**, so you can always tell which one she is using.

## The quick version

Four tricks, in plain words. (**Sneak** is the Shift key, unless you changed it. Everyone nearby sees all of them.)

* **Press R, then look at someone: read their mind.** A violet panel appears beside your crosshair and shows what they carry: the item in each hand, their armour, and, for a player, everything in their inventory. It also says whether they are calm, alert, afraid or want to hurt someone. Press **R** again to switch it off. The player you read feels a presence in their mind. A mind behind **Magneto's helmet**, a Loki's illusion, or the biggest bosses can't be read.
* **Sneak + R: sense every mind around you, even through walls.** A ring of violet light spreads out from you and every creature in 48 blocks glows on your screen for six seconds: players white, monsters pink, everything else violet. The action bar tells you how many it found. Then wait 20 seconds.
* **Press B while looking at a creature: take it over for 15 seconds.** It fights for you: it goes for whatever hurt you, whatever you hit, or the nearest monster, and never for you or your friends. Use it again and the first one wakes up confused. **Sneak + B** is the **Psionic Field**: everything living within 16 blocks of you, even behind walls, is frozen on the spot for three seconds.
* **Hold N: charge the Phoenix Burst.** You rise in flames and a firebird forms behind you, bigger and brighter the longer you hold (three seconds is a full charge). Let go and a wave of cosmic fire spreads out from you in every direction and hurts the monsters it reaches. Let go too early and nothing happens. Then you are drained for five seconds and have to wait 90 seconds.

## Becoming the Phoenix

The Phoenix has no recipe. She wakes when a player with no power uses the **Phoenix Ember**, a spark of the cosmic fire, a very rare find in chests:

| Source | Chance |
|---|---|
| End City treasure chest | 6% |
| Bastion treasure chest | 5% |
| Ancient City chest | 4% |
| Stronghold library chest | 3% |
| Nether Fortress chest | 3% |
| Woodland Mansion chest | 3% |

The chance is per chest generated. The Ember doesn't stack, can't burn, and has no other use.

1. Hold the Ember in either hand, with no power.
2. **Hold right-click for 3 seconds.** Gold and orange flames spiral up around you, and the Phoenix gathers (you hear her roar start).
3. When the three seconds are over the Ember burns into you: **one Ember is used up** (not in Creative), a burst of fire goes out that hurts nobody, and you awaken as Jean Grey.

Let go early and the flames sputter out, and nothing is used up. A hero who tries is turned away at once with "Your body already holds a power", and the Ember is kept: a player has one power at a time. Creative players can use it and keep it, and an operator can give the power with `/enhanced power set <player> phoenix [look]` (see [Commands](Commands)). Loki's Scepter and the Scarlet Witch Gene do not awaken her: the Phoenix is a different kind of power, and the Ember is her way in.

## The Phoenix's body

* **She can't be burned.** Fire, lava and burning don't hurt her (the server option `phoenix.fireImmune`, on by default). Damage that goes through everything (the void, `/kill`) still hurts.
* Everything else about her is an ordinary player's: 20 health, no extra armour.

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (toggle) | Mind Reading | While it is on, look at a creature or player (in line of sight, up to 24 blocks) and the **Mind Reading panel** shows what is in their mind: mood, health, what they hold and wear, and for a player everything they carry. It costs a little food while it is on (none in Creative). The player you read is told "You feel a presence in your mind" (once every 10 seconds). **Sneak + R:** **Mind Sense** (see below). |
| 2 | **G** (toggle) | Flight | Calm telekinetic flight, speed 0.09, the same as Wanda's. You can also double-tap **Space**. A pinned Phoenix can't fly, and she can't cross a Hex wall unless Wanda lets her in. Her arms open like wings as she speeds up. |
| 3 | **V** | Telekinesis | Wanda's telekinesis in violet. See below. **Sneak + V** sets down what you hold. |
| 4 | **B** | Mind Control | Take over the creature you look at (up to 10 blocks) for 15 seconds. **Sneak + B:** the **Psionic Field**. See below. |
| 5 | **N** (hold) | Phoenix Burst | Hold to charge, let go to unleash. See below. |

### Mind Reading and Mind Sense (R)

**Mind Reading (R).** Press **R** and "Mind Reading: ON (look at someone to read their mind)" appears above your hotbar. Look at someone (the server looks again every quarter of a second) and the panel appears beside the crosshair. Look away and it fades. Reading changes nothing about its target: it works through a Hex wall, on a creature pinned by Mjolnir, in Quicksilver Time and with PvP off. It needs a line of sight: a block between you and them stops it.

What the panel shows is explained [below](#the-mind-reading-panel).

**Mind Sense (Sneak + R).** A ping like a small Cerebro. It works whether or not Mind Reading is on, and it works through walls.

* A **ring of violet motes** spreads out from you at 2 blocks a tick, up to 48 blocks. Everyone nearby sees the ring.
* **Only you** see the glow: every mind the ring passes glows through the walls until six seconds are over. **Players** glow white-violet, **hostile monsters** pink, **everything else** violet. A hidden Loki glows too (his illusions don't).
* The action bar says what you found: "Minds nearby: 7 (players: 2, hostile: 3)". If someone in range wears Magneto's helmet you also hear about "silences where a mind should be", one for each helmet.
* Costs 2 exhaustion. **Cooldown: 20 seconds.**

**Not minds.** Armour stands, you, Loki's duplicates, and players in Creative or Spectator mode aren't read, sensed or taken. **Magneto's helmet** blocks every power of the mind (reading, sensing, taking over, the field): in the X-Men films it keeps Xavier out too. It does not block her telekinesis or her fire. The biggest bosses (the Ender Dragon, the Wither, the Warden and the 3.0 bosses) are too vast to read ("A mind too vast to read") or take.

### Telekinesis (V)

Wanda's and Billy's telekinesis, in violet, in Jean's hands. Press **V** and look at something to lift it: **creatures, players, dropped items, minecarts and boats, primed TNT and falling blocks, arrows and other projectiles in flight** (an arrow you catch hangs in front of you pointing where you look), and **blocks** (the same rules as Wanda: hardness at most 5, no chests or other block entities, nothing from the `enhanced:telekinesis_immovable` tag, and spawn protection and claims are respected). Press **V** again to **launch** what you hold, **Sneak + V** to set it down gently. **When V isn't holding anything, Sneak + V tears out the block you look at**, even past a creature or a boss (not in the second after you let go of something). Anything you throw hits hard: the damage is the Phoenix's, and it counts as her mind power.

The range, the launch speed and the block limits are the shared numbers `magneto.telekinesisRange`, `magneto.throwSpeed` and `scarlet_witch.blockLift*`: the Phoenix has no options of her own for it. **The three villains (Ultron, Thanos, Apocalypse) and the Wither, the Warden and the Elder Guardian can be held as bosses** (3 seconds, then they break free, and nobody can hold them again for 12 seconds); the Ender Dragon is too vast to hold. **Apocalypse can be held even through his shield**, because her telekinesis is mind power. She catches Thanos's sword, meteors and orbs and throws them back. In a boss arena the blocks are only borrowed (see [Scarlet Witch](Scarlet-Witch#bosses-and-borrowed-blocks)). Nothing lifts Mjolnir, and you can hold a player for at most 30 seconds.

### Mind Control and the Psionic Field (B)

**Mind Control (B).** Look at a creature within 10 blocks (about 33 feet, as in the film) and press **B**. It is **possessed** for 15 seconds, with a violet stream of motes from your forehead to it:

* **It never targets you, your tamed pets or players on your team**, and it takes no other order than yours. Every half second you steer it: it goes for the creature that last hurt you (within 24 blocks), else the one you last hit (in the last 5 seconds), else the nearest hostile creature within 16 blocks of you. With nothing to fight it walks back to within 4 blocks of you and stands guard. A cow or a villager is a harmless puppet that follows you.
* **One at a time.** Taking a second one frees the first, who wakes **confused** for 2 seconds (`phoenix.mindControlMax` changes how many you can hold, up to 5). The possession also ends early if you die, log out, change dimension or lose your power, or if the host dies. The bosses are never a target for your host.
* **A player can't be driven**, because their controls are theirs. If PvP is on, a player you aim Mind Control at is only **held** for 3 seconds: they can't move, jump or attack, and their mind goes blank. With PvP off, "PvP is off: you can't reach their mind".
* **Refused with "That mind is closed to you":** the Ender Dragon, the Wither, the Warden and the 3.0 bosses, anyone wearing Magneto's helmet, Creative and Spectator players, Loki's duplicates, and (with PvP off) another player's tamed pet. A creature pinned under Mjolnir can't be taken either. A refused cast costs nothing.
* Cost: 6 exhaustion (a food point and a half). **Cooldown: 20 seconds.**

**The Psionic Field (Sneak + B).** From *Spider-Man: Brand New Day*, where Jean freezes a whole district in a bubble of the mind. Everything **living within 16 blocks** of you, **even behind walls**, is **held still for 3 seconds**: it can't move, jump or attack, and a monster forgets its target.

* **Not held:** you, your tamed pets, players on your team, your possessed host, Creative and Spectator players, the Ender Dragon and the Wither, the Warden, the 3.0 bosses, and anyone wearing Magneto's helmet. **Players are held only if PvP is on**; creatures always.
* If nobody is held ("Nobody in your field") it costs nothing and doesn't start the cooldown.
* Cost: 8 exhaustion (two food points). **Cooldown: 60 seconds.** Mind Control and the Psionic Field share the cooldown slot of ability 4: each starts its own length.
* Everyone nearby sees a violet dome grow from you to the edge of the field, hex lines on it, and hears a deep pulse.

### The Phoenix Burst (N)

From *X-Men: Apocalypse*, where Xavier tells Jean to let go and **bird-shaped flames** burn En Sabah Nur to ash.

**Hold N.**

1. **Checks.** The Burst must be rested, and you need at least 6 food points ("The Phoenix is still resting (12.3 s)", "Too hungry to hold the Phoenix"). Creative players pay nothing.
2. **Charging.** You **rise** (0.12 blocks a tick) until you hover 1.5 blocks above the ground, and your speed is cut to 40 percent (your walking speed, and your flying speed, which is what you move at while you hover). A **firebird of flame ribbons** forms behind and above you, and grows with the charge, flames spiral up your body, your eyes burn gold and the Phoenix's roar gathers. You are **not invulnerable**: charging is a risk. A pinned Phoenix doesn't rise.
3. **Release** (or full charge: after 3 seconds it goes off by itself). Let go before 12 ticks (0.6 s) and it is **cancelled**: "The Phoenix slips away", no damage, no hunger, and a 2 second wait. Otherwise the **Burst** goes off with the charge `f` (0 to 1):

| | At the smallest charge | At full charge |
|---|---|---|
| Radius of the wave | 8 blocks | 24 blocks |
| Damage at the centre | 8 (4 hearts) | 30 (15 hearts) |
| Burning | 4 seconds | 10 seconds |

* **The wave is a sphere that spreads at one block per tick** from where you hovered, so a creature 20 blocks away is hit 20 ticks after one at 1 block. Each creature is hurt **once**. Out to half the radius it takes the full damage, then it falls to **60 percent** at the edge. **Cover protects:** damage is multiplied by how much of the creature the wave reaches from the centre, so stone between you and the creature stops it, and creatures are knocked away from the centre (1.2) and a little up.
* **Who is hurt.** Hostile creatures, creatures that are after you or your team, the bosses, and players **if PvP is on**: a player takes only **half** the damage (`phoenix.burstPlayerFactor`). **Spared:** you, your tamed pets, players on your team, your possessed hosts, Creative and Spectator players, armour stands, and, **by default, villagers, Iron Golems, animals and other players' pets**, so one Burst can't wipe out a village or a farm (`phoenix.burstHurtsPassives` turns this off). A phased Vision takes nothing; fire-proof creatures (Blazes) and armoured ones take the full damage, the Burst goes through armour. The bosses are hurt but not pushed.
* **It never breaks a block.** Only if `mobGriefing` is on (and `phoenix.burstSetsFire` too), it may light up to 40 fires on flammable ground where the wave passes, never where spawn protection or a claim says no, and never in a boss arena. Vanilla lets them burn out.
* **A cleansing.** At the release the fire burns off what holds you: Spellbound, Confused, Nausea, Slowness, Blindness and Darkness.
* **A price.** It costs hunger: about 6 food points at full charge (at least 30 percent of that, whatever the charge). Afterwards you are **drained**: Weakness and Slowness for 5 seconds (not in Creative). Then the **cooldown is 90 seconds**.
* A creature the Burst kills leaves a puff of golden ash. Everyone near sees the firebird fly out and dissolve, a fire shell, scorch rings on the ground and 48 streaks of flame, hears the scream, and everyone in range gets a gold flash (not with Reduced motion).

### Against Apocalypse: critical hits

The Phoenix's mind power hits the boss Apocalypse **critically**, like Wanda's: the Phoenix Burst, a telekinetic slam, and the strains of Mind Control and the Psionic Field are all **twice as strong on his shield** and, when the shield is down, on his body. You see a burst of sparks, hear a crack, and the action bar says "Critical hit! ×2". Everyone else hits him much less. See [Apocalypse](Apocalypse#he-never-heals-wanda-and-the-phoenix-hit-critically).

## The Mind Reading panel

The panel sits 14 pixels to the right of the crosshair (to the left of it near the right edge of the screen), in dark violet with a violet frame. From the top:

1. **Name and type** (the name in violet bold, the type grey).
2. **Mood**, a coloured word with a dot: *Calm* (violet), *Alert* (a hostile creature with no target yet, gold), *Wants to hurt Steve* (it has a target, pink), *Afraid* (hurt a moment ago and not aggressive, blue).
3. **Health**, a bar with the hearts as a number. A player also shows food and level.
4. **Carrying:** seven item slots, in the order main hand, off hand, head, chest, legs, feet and body (a horse's armour), each with its stack count and durability bar. Below them the names of the two hands' items, in the colour of their rarity, with a small star if enchanted.
5. **In the pack:** a player's whole inventory as a 9 by 4 grid (hotbar first). A creature that carries things (villagers, piglins, allays, pillagers) shows a row of what it holds, and an Enderman the block it carries.
6. **Extras** in violet italics: "Really Loki, shaped as a Zombie" (the real name of a shapeshifted Loki) or "Vision (disguised)".

Some minds show no more than the title and one line: *A cold metal silence: this mind is shielded* (Magneto's helmet), *An illusion: there is no mind here* (a Loki duplicate), *A mind too vast to read* and *The server keeps players' minds private*. The panel hides when you look away or turn Mind Reading off, and 15 ticks after the last news from the server.

The server can turn reading a player's pack into reading only the hands and armour (`phoenix.mindReadInventory`), turn reading players off (`phoenix.mindReadPlayers`), or stop telling the player who is read (`phoenix.mindReadNotifies`).

## What other players see

Her head tilts while she reads (hand to the temple, then a small shake of the head to switch off); every half second six violet motes drift from her forehead to the head of the one she reads, and the one she reads sees them too. Her eyes glow **violet** while she reads, senses, takes over or freezes, and **burn gold** while she charges. Mind Control is a hand to the temple and an arm out, with the stream of motes and a ring of violet sparks round the host's head. The Psionic Field is both arms thrown out. The charge is arms thrown up and out in a V, trembling, her head back, a firebird behind her and a flame swirl over her body; the Burst throws the V open wide. In first person your hands do the same. Flying she leaves embers, with gold streams from the shoulders at speed.

## The HUD

| Key | Shows |
|---|---|
| **R** | *ON* or *OFF*, and while Mind Sense recovers `sense 12.3s` |
| **G** | *ON* or *OFF* |
| **V** | *holding* while she holds something |
| **B** | the cooldown, and `host: Zombie` in violet while a creature within 32 blocks is possessed |
| **N** | `charging 63%` in gold (orange above 90 percent) while she charges, else the cooldown |

## Looks

Press **H** to change look (**Sneak + H** goes back). The abilities are the same in every look. See [Looks](Looks).

| Look | Id | What it is |
|---|---|---|
| **X-Men: Apocalypse** (default) | `apocalypse` | The blue uniform of the last scene of the film, with grey side panels, two yellow stripes, a yellow collar, a black belt and gloves, and bright red hair in big waves. Her energy is the gold and orange of the fire. |
| **Spider-Man: Brand New Day** | `brand_new_day` | The hooded telepath of the film: an olive hood with a mustard lining, a mustard hoodie and a dark leather jacket with an orange topstitch, copper hair in a ragged fringe, freckles and silver rings. The generic energy (the awakening burst, the look change) is gold with a pale green. |
| **Your own skin** | `own` | Her powers in your own skin. |

## The Infinity Stones (3.6)

The Phoenix may hold every [Infinity Stone](Infinity-Stones) bare and use them, wield a crafted Infinity Gauntlet and Snap, and seek the Soul Stone at [Vormir](Vormir). Only the **Time Stone** stays quiet in her hand: it answers only a sorcerer.

## Numbers you can change

How fast she flies, whether fire hurts her, how far Mind Reading, Mind Sense and Mind Control reach and what they cost, how many creatures she can hold and for how long, the Psionic Field's radius, length and recharge, and every number of the Burst (charge time, radius, damage, how much a player takes, how fast the wave spreads, fire, knockback, whether it sets fires or hurts villagers and animals, hunger cost, cooldowns and the drain afterwards), and how long the Ember must be held, are the `phoenix.*` options on [Server Configuration](Server-Configuration#phoenix-jean-grey).

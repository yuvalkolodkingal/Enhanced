# Billy and the Witches' Road

Billy Maximoff (Wiccan), Wanda's other son. In *WandaVision* he hears every thought around him and freezes a squad of S.W.O.R.D. soldiers; in *Agatha All Along* his own chaos magic makes the Witches' Road real. In the mod, the Road is a door, three trials behind it, and the way to become Billy (or Tommy).

## The Witches' Road Door

**Crafting:**

```
A L A
T E T
A L A

A = Amethyst Shard
L = Soul Lantern
T = Dark Oak Trapdoor
E = Eye of Ender
```

It is a pair of weathered cellar doors set flat in the floor (you can stand on it), outlined in a faint blue-green glow, like the door in the basement in *Agatha All Along*. It is made of wood: an axe breaks it fastest, and pistons can't push it. Breaking it drops the door again.

### Seeking the Road: becoming Billy or Tommy

A player **with no power** right-clicks the door **holding a Scarlet Witch Gene**, in Survival mode.

1. A ring of blue-green light runs around the door for two seconds.
2. The doors open onto a spiral stair among glowing roots, and the Road asks: **"Seekest thou the Road?"** There is one big button, **Walk the Road**, and the screen says which trial waits ("Trial 1 of 3").
3. The player is taken to the Witches' Road, a dimension of its own, and walks **three trials**: the Parkour, Copy the Structure and the Ice-Boat Race. Each success unlocks the next one. They are all on [The Witches' Road](Witches-Road), and the Gene stays in the inventory the whole time.
4. At the end of the Road the player **chooses**: **Billy: Wiccan** (blue magic, flight, telekinesis) or **Tommy: Speed** (super speed, like his uncle). See [Tommy](Tommy). Only then is the Gene used up, and the player is taken back to the place where they came in.
5. The door closes again five seconds after it opens.

Without a gene the door won't open ("The Road only opens for witch blood"). A hero already "walks their own road" and cannot walk the trials. Each player walks alone, in a place of their own, and can leave at any time (the Gene and the progress are kept). A server can switch the trials off with `witches_road.enabled`: the door is then the instant choice of Enhanced 3.1, and anyone else holding a gene can join the ritual and choose too.

### Walking the Road

The **Scarlet Witch** and **Billy** (whose magic made the Road), and, in 3.3, **[Agatha](Agatha)** (who led the coven down it), can travel it. Right-clicking a door without a gene opens it and takes them to the **next Witches' Road door** in the same dimension (in the order the doors were placed, going round), arriving on it in a column of light. A door that is gone is skipped. Whatever they hold with telekinesis stays behind, and the Hex wall is respected. Cooldown 3 seconds.

## Billy's abilities

Billy's magic is blue with a white-hot core, crackling and branching like a Tesla coil.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (hold) | Hex Beam | A continuous beam of blue magic from his hand along the way he looks: he has to **aim** it. It hurts what the crosshair is on, 6 damage a second and 20 blocks at his first rank, growing to 12 damage a second and 40 blocks as a Chaos Master. See [below](#the-hex-beam-r-held). |
| 2 | **G** (toggle) | Flight | Calm, weightless flight with open hands, like his mother's (speed 0.08). You can also double-tap **Space**. |
| 3 | **V** | Telekinesis (Sneak: a block) | Wanda's telekinesis, in his blue: creatures, dropped items, minecarts, boats, primed TNT, falling blocks, **and projectiles in flight**: an arrow he catches hangs in front of him, pointing where he looks, and flies off at his command. **He tears blocks out of the ground too**: within 16 blocks, hardness 3 at most (stone, ores, planks; not iron blocks), one block a second. **Sneak + V** while he holds something sets it down (a block becomes a block again where it lands); **when V isn't holding anything, Sneak + V** takes the block he looks at, even past a creature (not in the second after he let go of something). Press again to launch. He holds the three villains and the Wither, the Warden and the Elder Guardian as bosses, **half as long as Wanda** (1.5 seconds), and Apocalypse only in a window, never through his shield. He catches Thanos's sword, meteors and orbs and throws them back. |
| 4 | **B** (Sneak + **B**, held: Hex Barrier) | Freeze | Every creature and player within 12 blocks in a 90 degree cone in front of him, in his line of sight, is **spellbound** for 5 seconds: they can't move, jump or attack, can't pick a new target, and blue sparks circle their heads. The Ender Dragon and the Wither are too strong for it. Cooldown 12 seconds. **Sneak + B, held, raises the Hex Barrier**, a plate of blue magic that stops what comes at him from the front: see [below](#the-hex-barrier-sneak--b-held). |
| 5 | **N** (toggle) | The Noise | He hears every mind within 32 blocks: every creature glows through walls, **red** if it is hostile and about to attack, **blue** otherwise. Whispers crowd his head, louder the more minds are near. **3.3:** he also sees their **thoughts** in words over their heads, as [Professor X](Professor-X#abilities) does. The Hex wall stops it: he hears nobody behind a Hex wall. |

### The Hex Beam (R, held)

Hold **R** and a **beam** of blue magic leaves his hand along the way he looks, with a white-hot core, crackling strands and little forks, and a hum that lasts as long as he fires. It replaces the single Hex Bolt of 3.1, and it still needs **aim**: it hurts what the crosshair is on, the first creature (or part of the Ender Dragon, or End crystal) the line meets, and a block stops it. A creature takes four hits a second.

* **Damage and reach grow with his Build Magic rank.** At rank 1 (Apprentice) the beam does **6 damage a second** (1.5 a hit) and reaches **20 blocks**; at the top rank (the Chaos Master, rank 10) **12 damage a second** and **40 blocks**; in between they grow evenly (Wizard, rank 7: 10 damage a second and 33 blocks). A server that sets `build_magic_billy.maxRank` lower reaches the top sooner.
* **It costs food.** 0.8 exhaustion a second, a food point about every five seconds (the old bolt cost none). He can't fire it on an empty stomach ("Too hungry to fire the Hex Beam"), and it stops when he lets go of **R**, when his hunger runs out, when he becomes a Spectator, logs out or loses the power.
* **It follows the rules of a beam.** A complete Chaos Shield stops it at its surface and pays for it, as it does Thor's lightning; with PvP off it does not hurt players; against Apocalypse's shield it does nothing (it is not mind power); every other boss takes it by the usual rules.
* **You see and hear it.** Everyone sees the beam, with flares at both ends, and his arm stays out along the look while he fires. The HUD shows `[R] Hex Beam  6.0 dmg/s, 20 blocks` (and `firing: ...` in green while he holds it).
* **The old bolt is still there if a server wants it:** `billy.beam = false` brings back the single Hex Bolt: a crackling blue arc to the first creature he looks at within 24 blocks, 6 damage, which forks on to the two nearest creatures within 5 blocks of it (3 damage each), with a cooldown of half a second.

### The Hex Barrier (Sneak + B, held)

Hold **Sneak** and press **B** (plain **B** is still the Freeze) and a tall **plate of blue magic**, 1.4 blocks wide and 2.4 high, forms 1.6 blocks in front of him in 8 ticks (0.4 seconds). It follows his look, up and down too, and is drawn with a glowing seal and a faint veil. Keep **B** down to keep it up; let go and it lowers (also if you let go of Sneak first).

* **It stops everything that comes from the front (3.3).** Within 60 degrees either side of where he looks: every projectile (it bursts into blue sparks), **every beam** (Thor's lightning, Vision's Mind Stone, the Darkhold's purple beam, another Billy's Hex Beam, Cyclops's blasts, the bosses' beams: they end at the plate), blows of creatures and players (the attacker is thrown back), **explosions, all of their damage and their push**, lightning, and the powers that reach for him: telekinetic grips, Magneto's strip and pull, Ultron's and Thanos's pulls, the Phoenix's mind powers, Quicksilver's pickpocket, Mjolnir's pin, the Darkhold's mark, curses and effects, and the bosses' attacks (Apocalypse's Backhand, Mind Crush and Entomb, Thanos's choke). What comes from the side or from behind passes whole, and so does harm that comes from nowhere (falling, hunger, poison already in him, fire, drowning, the void, `/kill`).
* **No time limit.** It stays up as long as he holds it, and it costs no food.
* **It has 60 health**, at every rank (a server can make it grow with his rank: `billy.barrierRankHealthBonus`). **Only chaos magic and dark magic wear it down** (3.3.1): a power of a **Scarlet Witch** (her Chaos Beam, her telekinetic grip, what she throws) or of the **Darkhold** and **Agatha** takes off the damage that hit would have done (a grip or a pull that does no damage costs nothing; a curse costs 2). Everything else it stops (arrows, blows, explosions, lightning, other beams, the bosses' attacks) **does not wear it at all**. The HUD shows a bar and `41/60` while it is up (green, yellow under half, red under a quarter), a big health bar sits under the crosshair, and **cracks** spread on the plate as it weakens.
* **At 0 it shatters**: shards fly, a ring of light flashes, glass breaks, and "The Hex Barrier broke!". Then it cannot be raised for **10 seconds**.
* **It mends while it is down**: 1.5 health a second (from nothing to 60 in 40 seconds), and it keeps what it has left between uses. With less than 1 health it is "still mending" and does not rise. After a normal lowering, B waits only half a second.
* **When chaos magic breaks it** (its last health taken by a Scarlet Witch's power) it says "Chaos magic shattered the Hex Barrier!", with red sparks in the ring. (Before 3.3.1 chaos magic shattered it at once.)
* **Its price**: he walks at three fifths of his speed while it is up. The HUD shows `[B] Freeze (Sneak: Hex Barrier)`, then "forming", its health, and the wait after it shattered.

**The crown.** While he flies, for three seconds after each spell, and while he builds with Build Magic, a glowing chrome crown forms on his head, in shades from ice-white to deep blue (the *Wiccan* look already has it).

## Build Magic

Billy builds too: press **Y**, click two corners and right-click, and a house grows out of the ground; **Ctrl + Z** takes it back. But he is an **apprentice who learns**. His spells cost *magic*, one point a block, from a meter that refills all the time, and the meter, how big he can build and how far he reaches grow with practice (every block he builds), through **ten ranks**, from Apprentice to Chaos Master:

| Rank | Practice | Meter | Refills | Longest side | Reach | Undos |
|---|---|---|---|---|---|---|
| 1 Apprentice | 0 | 512 | 16 a second | 24 | 24 | 10 |
| 2 Spellcaster | 2,000 | 1,024 | 24 a second | 32 | 32 | 12 |
| 3 Sorcerer | 8,000 | 2,048 | 32 a second | 40 | 40 | 14 |
| 4 Wiccan | 30,000 | 4,096 | 40 a second | 48 | 48 | 16 |
| 5 Enchanter | 60,000 | 6,144 | 56 a second | 56 | 56 | 18 |
| 6 Mystic | 120,000 | 8,192 | 72 a second | 64 | 64 | 20 |
| 7 Wizard | 220,000 | 12,288 | 96 a second | 72 | 72 | 22 |
| 8 Archmage | 380,000 | 16,384 | 128 a second | 80 | 80 | 24 |
| 9 Reality Weaver | 600,000 | 24,576 | 160 a second | 88 | 88 | 26 |
| 10 Chaos Master | 1,000,000 | 32,768 | 192 a second | 96 | 96 | 28 |

He has all eleven spells from the first minute; what grows is how big. Ranks 1 to 4 are those of 3.1: the meter doubles, the refill grows by half of the first, the side and the reach by 8 blocks, and he can undo 2 more spells. From rank 5 the meter and the refill grow faster (the meter is 64 times the Apprentice's at rank 10, the refill 12 times), the side and the reach go on by 8 blocks a rank, and he can undo 2 more spells and Repeat 4 more copies (44 at rank 10). A full meter at rank 10 refills in about three minutes. The highest rank is the server's `build_magic_billy.maxRank` (1 to 10, ten by default; a server file written by 3.1 keeps 4 until its owner changes it), and an operator can set a rank with `/enhanced build rank`. His beam and the reach of his ward from the [Book of the Vishanti](Sorcerers-Books) grow with the same rank (his barrier only if the server says so).

**Near a Scarlet Witch (16 blocks), his spells cost half**, and a thin scarlet thread runs from her hand to his. A spell too big for him never starts half-built. Everything is on the [Build Magic](Build-Magic) page.

## Looks

*Wiccan* (*Agatha All Along*), *Teen* (on the Witches' Road), *Halloween* (his *WandaVision* costume), *Wiccan (comics)*, and *Your own skin*. Press **H** to switch. See [Looks](Looks).

## Numbers you can change

Billy's flight speed, the Hex Beam (whether it is a beam at all, its damage, range, hit rate, hunger and how much it grows with his rank), the Hex Bolt's damage (when the beam is off), the Hex Barrier (its health, how fast it mends, the wait after it shattered, its arc, size, walking speed and rank growth), how long Freeze holds (5 seconds), how far The Noise reaches (32 blocks), whether he tears blocks out at all, how far and how hard a block, and his recharge between blocks are the `billy.*` options on [Server Configuration](Server-Configuration#billy-wiccan). His Build Magic numbers (the meter, the refill, the ten ranks and their practice, his reach and side, Mom's help) are the `build_magic_billy.*` options.

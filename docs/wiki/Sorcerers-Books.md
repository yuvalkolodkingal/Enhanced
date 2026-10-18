# The Sorcerers' Books

Two old books of magic can be crafted in survival: the **Book of the Vishanti**, which saves places and raises a ward round your base that keeps out every monster, every boss and dark magic, and the **Darkhold**, Agatha's book, with a purple beam, a revenge on whoever uses magic against you, and two small spells. Only a **sorcerer** can read them, and a player with **no power** who reads the Darkhold **becomes [Agatha](Agatha)**.

Hebrew: [עברית](Sorcerers-Books-HE)

## Who is a sorcerer?

| Hero | Sorcerer? | Strength of the ward |
|---|---|---|
| [Scarlet Witch](Scarlet-Witch) | Yes | The strongest: **48 blocks** |
| [Billy](Billy-and-the-Witches-Road) | Yes | Grows with his [Build Magic rank](Build-Magic#billy-an-apprentice-who-learns): **12 blocks** as an Apprentice up to **40** as the Chaos Master |
| [Loki](Loki) | Yes | **24 blocks** |
| [Agatha](Agatha) | Yes | **24 blocks** |
| Everyone else | No | (none) |

A player who is not a sorcerer can hold the books, but they only whisper: "The runes swim before your eyes. Only a sorcerer can read this book." and "The Darkhold whispers, but only a sorcerer can read it." (A server can let anyone use the Darkhold: `darkhold.anyoneCanUse`, off by default.)

**3.3: the Darkhold makes Agatha.** A player with **no power** who **holds use on the Darkhold for 3 seconds** becomes [Agatha Harkness](Agatha), and keeps the book. (While `agatha.darkholdAwakens` is on, the awakening comes first, even when `darkhold.anyoneCanUse` is on too.) A hero who is not a sorcerer is still refused.

Sorcerers matter in one more place in 3.3: the [Hex Runes](Scarlet-Witch#hex-runes) of a Scarlet Witch take away the powers of every other sorcerer inside her Hex, and of anyone who holds the Darkhold or the Book of the Vishanti there.

## The Book of the Vishanti

*"The old book of the sorcerers: the spells of Oshtur, Hoggoth and Agamotto."*

### Crafting

```
A E A
G B G
A L A

A = Amethyst Shard
E = Eye of Ender
G = Gold Ingot
B = Book
L = Lapis Lazuli
```

Four amethyst shards, an Eye of Ender, two gold ingots, a book and a lapis lazuli. The recipe appears in your recipe book when you pick up an Eye of Ender. The book does not stack and cannot burn.

### Using it

| Input | What it does |
|---|---|
| **Use** (right-click) | Opens the book screen. |
| **Sneak + use** | Takes you to your base (the first base you saved). |

The screen has a **name field** ("Name this place"), a **Save here** button (Enter in the field does the same), a **Save as base** button, a **Go to my base** button, and a row for each saved place with a **go** button and an **x** to forget it (a base is marked **[Base]**). Three places fit on a page, and the book holds up to **24 places**. The line under the title tells you how far your ward reaches. Saving a name that already exists moves that place to where you stand, and if it is a base its ward moves with it (with the ward's price, recharge and rules) and the book says "A ward rises round it". A ward is a "clear the monsters round me" button and has no claim awareness: it can be raised inside someone else's base, farm or the spawn, where it stops natural and spawner monster spawns and expels monsters (they land just outside the wall), but it touches no block. `maxWardsPerPlayer`, `wardCooldown` and `wardExhaustion` are the dials.

### Saved places and teleporting

* **Save here** remembers where you stand, in **any world** (the Overworld, the Nether, the End, a world another mod adds), facing the way you face.
* **Go** gathers sparks round you for one second (`teleportChargeTicks`, 20 ticks) and then takes you there. It needs **30 seconds** to recharge (`teleportCooldown`, 600 ticks) and costs **1.5 food points** of hunger (`teleportExhaustion` 6.0; Creative pays nothing). The book says "The book is gathering its strength" until it is ready, and if the arrival is refused you get the recharge back.
* **Where it will not go.** A boss fight keeps its fighters in and everyone else out; a Hex wall judges you as it does every traveller; a player pinned by Mjolnir cannot leave; and the **Witches' Road** is always shut to the book (you cannot save a place there, leave it or be taken into it). A server can add more dimensions to the list `vishanti.deniedDimensions`.
* The Bifrost of Thor's weapons keeps its own list; the Book of the Vishanti has a separate one, of its own, with its own recharge.

### The ward: what it keeps out

**Save as base** saves the place as a **base** and raises a **ward** round it: a sphere of shimmering light, centred where you stood. In 3.3 a ward keeps out **monsters, bosses, what they shoot, and dark magic**, and nothing else.

* **How big.** It depends on the sorcerer, as the first table shows: Wanda 48 blocks, Loki 24, Agatha 24, Billy from 12 to 40 by his rank (12, 15, 18, 21, 24, 28, 31, 34, 37 and 40 for ranks 1 to 10). It is a full sphere, so a base under the ground is covered too. The radius is refreshed every five seconds while you are online and whenever you save or forget a place, so a Billy who ranks up sees his ward grow.
* **Every monster and every boss.** The creatures of the monster category (zombies, skeletons, creepers, spiders, witches, phantoms and so on), **the bosses** (the Ender Dragon, the Wither, the Warden, the Elder Guardian, Thanos, Ultron and Apocalypse) **and their minions** (Ultron's Sentries, Thanos's illusions, sword, meteors and orbs) **do not spawn inside it** (natural spawns, spawners and structures), and one that is inside is **thrown back out** if it is near the edge, or set down just outside the wall if it is deeper, on safe ground (never in lava, fire, magma, a cactus, a berry bush, a wither rose or a campfire, never on an edge with no floor; a flier or a guardian in the right element). It looks (once every 5 ticks) at what is inside; nothing is placed or broken, so it is no trouble for claims.
* **What they launch stops at the edge.** An arrow, a fireball, a trident, a potion, a beam, a lance of sand, a meteor: anything a monster or a boss shoots or casts from outside ends at the ward, and their area attacks, explosions, effects, grips and pulls do not reach inside (the blocks and creatures inside are left untouched).
* **Dark magic stops at the edge**: the Darkhold's purple beam (drawn to the edge for everyone), its harm, its hex mark, its mind glimpse and its poison, and [Agatha's](Agatha#dark-magic) beam, Power Drain and runes, whoever casts them.
* **Nothing else.** Players' powers pass through, chaos magic, Vision's beam and Thor's lightning included, and so do animals, villagers, pets and the players themselves.
* **What it leaves alone.** Pets that have an owner (a hero's own Ultron drones too), creatures that ride or are ridden, and **what the player keeps**: a monster with a **name tag**, one on a **lead**, and a **zombie villager being cured**. A boss or a minion of a boss fight is never spared, named or not. Raiders (pillagers, ravagers, evokers), piglins and hoglins of a farm and slime, guardian or zombie farms **are** monsters: a ward is a "no monsters here" shield, so a raid on a warded village is expelled. Name the mob to keep it.
* A ward still cannot be raised over a boss arena (below), and a power that starts **inside** a ward is never stopped by it.
* **How many.** Up to **2 bases** (so 2 wards) at a time per player (`maxWardsPerPlayer`). Raising one needs **10 seconds** to recharge (`wardCooldown`) and costs **2 food points** of hunger (`wardExhaustion` 8.0). A ward cannot be raised in a boss arena, in the Witches' Road, or where it would reach into a boss arena. Forgetting a base takes its ward with it.
* **It stays.** Wards are saved with the world and keep working **whoever is online**, as long as their land is loaded. Only raising or forgetting needs the book. Turning wards off on the server (`vishanti.enabled` or `wardsEnabled`) silences every ward, an offline owner's too. A changed radius, or a new Billy rank, reaches the ward of an offline owner only at their next login. A ward whose owner is online but has lost the sorcerer's power sleeps (it is kept, but does nothing and is not drawn) and wakes if he is one again.
* **You see it:** (the game tells you only of the wards whose edge is within 256 blocks, and of your own wherever they are) a faint veil and hexagon lines on a sphere in the sorcerer's colour (crimson for Wanda, blue for Billy, green for Loki, violet for Agatha) and a ring on the ground, only when you are within 64 blocks of its edge. *Reduced motion* stops the drifting and breathing.
* **Operators:** `/enhanced ward list` lists every ward (whose, where, how far it reaches), and `/enhanced ward remove <player>` takes away every ward of a player (their bases stay as plain places). The player can be an online player, a selector, or the name or id of someone who is offline or gone: the wards go by id alone, and a wiped book loses its wards at the next login too.

## The Darkhold

*"Agatha's book of dark magic: it corrupts whoever reads it."*

### Crafting

```
N R N
E B E
N S N

N = Netherite Scrap
R = Wither Rose
E = Echo Shard
B = Book
S = Wither Skeleton Skull
```

On purpose it is **rare**: the rose needs a Wither's kill, the skull a Nether Fortress, the echo shards an Ancient City, the scrap some ancient debris. The recipe appears when you pick up a Wither Rose. The book does not stack and cannot burn.

### Using it

A sorcerer holds the Darkhold (in either hand) and has four powers from it. None works in the [Witches' Road](Witches-Road). A small panel in the top right of the screen names them while you hold it. (A player with no power sees "Hold use: read it and become Agatha" there instead.) [Agatha](Agatha) has the beam, the hex mark and the mind glimpse on her own keys and the revenge always, with or without the book.

| Power | Input | What it does |
|---|---|---|
| **Purple Beam** | **Hold use** | Agatha's purple laser. |
| **Revenge** | (nothing to press) | Whoever uses magic on you is **poisoned for 10 seconds**. |
| **Hex mark** | **Sneak + use** at a creature | It glows, is slowed and takes more damage from you. |
| **Mind glimpse** | **Sneak + use** at nothing | Every living thing near you glows, and you are told how many and how hurt the nearest is. |

#### The Purple Beam

Hold the right button and a **purple beam with a white core** leaves your hand along the way you look. You walk slowly meanwhile, like someone drawing a bow, and the beam needs **aim**: it hurts what the crosshair is on, the first creature (or dragon part, or End crystal) the line meets, and a block stops it.

* **8 damage a second** (a hit of 2 every 5 ticks, four a second, so every hit lands), reach **24 blocks**, and it costs **1 exhaustion a second** (a food point every four seconds). With an empty hunger bar: "Too hungry to fire the Purple Beam".
* It stops when you let go, put the book away, run out of food, become a Spectator or log out.
* A complete Chaos Shield stops it at its surface; with PvP off it does not hurt players; against Apocalypse's shield it does nothing. Every boss otherwise takes it by the usual rules.
* **Billy** fires this beam, not his own Hex Beam, while he holds the book; letting go of **R** does not stop it, and tapping the use button cannot make it hit faster than four times a second.
* The R key is not used, so it never clashes with a hero's own key: the Darkhold works for a Loki, a Wanda and a Billy alike.

#### The revenge: poison

When **magic** hits you while you hold the Darkhold, **whoever used it gets Poison** for **10 seconds** (200 ticks, Poison I). What counts as magic: the Hex Beam and Hex Bolt, the Chaos Beam, a telekinetic slam, the Mind Stone, the Stones' powers, the Phoenix Burst, the Purple Beam, Apocalypse's wall and Entomb, a witch's potions and anything the game itself calls magic. **Thor's lightning does not count** (it is his power, not magic). **Spells that put an effect on the holder answer too**: when a living creature lays Confusion or Spellbound on the holder (Loki's Scepter, Billy's Freeze, the Phoenix's mind powers), that creature is poisoned. Shapeshifting and telekinetic grabs are not effects and do not answer.

* It answers the **use** of magic even if a Chaos Shield stops the hit.
* The same attacker is not poisoned again for 2 seconds (`poisonCooldownTicks`, 40), so a beam does not renew it every tick.
* A hit with no living attacker, or your own, answers nothing. **Undead** shrug poison off, as in the game.
* A server can swap it: `darkhold.poisonTarget` = `ATTACKER` (the default) or `HOLDER` (you pay for the magic the book drew).

#### The hex mark

**Sneak + use** while you look at a creature within **24 blocks**: it is **marked** for 10 seconds. It **glows**, gets **Slowness II**, and takes **half as much damage again** (x1.5) from you, the one who marked it. **A boss** is marked too (it glows and takes the x1.5) but is **not slowed**. Cooldown 5 seconds, cost 2 exhaustion. A player is marked only where you could harm them (PvP on).

#### The mind glimpse

**Sneak + use** while you look at no creature: every living creature within **24 blocks** (the nearest 16) **glows for 5 seconds**, and the action bar says "The Darkhold shows creatures: 2. Nearest: Zombie, 5.0 of 20 health". It does not show what hides: invisible creatures, a Loki in his Vanish, a phased Vision. Cooldown 10 seconds, cost 1 exhaustion.

## Numbers you can change

The book and the ward are the `vishanti.*` options; the Darkhold is the `darkhold.*` options. Both are on [Server Configuration](Server-Configuration#the-book-of-the-vishanti) and [Server Configuration](Server-Configuration#the-darkhold). Three datapack tags matter: `enhanced:ward_immune` (what the ward leaves alone: empty in 3.3, for data packs to fill), `enhanced:dark_magic` (what dark magic is: the Darkhold's beam and Agatha's Drain and runes) and `enhanced:magic_damage` (what counts as magic for the Darkhold's revenge).

## Questions

**Can I use the books in the Witches' Road?** No: they are shut there, on purpose, so the trials stay the trials.

**Does the ward need me nearby?** No. It works whenever the land is loaded, whoever is online.

**My ward is smaller than my friend's.** The stronger the sorcerer, the bigger the ward: Wanda 48, Loki and Agatha 24, Billy from 12 up.

**Why did my friend's beam go through my ward?** A ward keeps out monsters, bosses, what they shoot and dark magic, nothing else: players' powers pass (and a player who wants in is not stopped either). To keep players out, a Scarlet Witch raises a [Hex](Scarlet-Witch#5-the-hex). Rank up in [Build Magic](Build-Magic) to grow Billy's.

**Why did the book not take me there?** The arrival can be refused by a boss fight, a Hex wall, Mjolnir's pin or a shut world. You get the recharge back when that happens.

## Related

[Items and Crafting](Items-and-Crafting) · [Agatha](Agatha) · [Loki](Loki) · [Scarlet Witch](Scarlet-Witch) · [Billy](Billy-and-the-Witches-Road) · [Thor](Thor) · [Commands](Commands) · [Controls](Controls)

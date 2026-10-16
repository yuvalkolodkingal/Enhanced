# The Sorcerers' Books

Two old books of magic can be crafted in survival: the **Book of the Vishanti**, which saves places and raises a ward round your base that no monster can enter, and the **Darkhold**, Agatha's book, with a purple beam, a revenge on whoever uses magic against you, and two small spells. Only a **sorcerer** can read them.

Hebrew: [עברית](Sorcerers-Books-HE)

## Who is a sorcerer?

| Hero | Sorcerer? | Strength of the ward |
|---|---|---|
| [Scarlet Witch](Scarlet-Witch) | Yes | The strongest: **48 blocks** |
| [Billy](Billy-and-the-Witches-Road) | Yes | Grows with his [Build Magic rank](Build-Magic#billy-an-apprentice-who-learns): **12 blocks** as an Apprentice up to **40** as the Chaos Master |
| [Loki](Loki) | Yes | **24 blocks** |
| Everyone else | No | (none) |

A player who is not a sorcerer can hold the books, but they only whisper: "The runes swim before your eyes. Only a sorcerer can read this book." and "The Darkhold whispers, but only a sorcerer can read it." (A server can let anyone use the Darkhold: `darkhold.anyoneCanUse`, off by default.)

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

The screen has a **name field** ("Name this place"), a **Save here** button (Enter in the field does the same), a **Save as base** button, a **Go to my base** button, and a row for each saved place with a **go** button and an **x** to forget it (a base is marked **[Base]**). Three places fit on a page, and the book holds up to **24 places**. The line under the title tells you how far your ward reaches. Saving a name that already exists moves that place to where you stand.

### Saved places and teleporting

* **Save here** remembers where you stand, in **any world** (the Overworld, the Nether, the End, a world another mod adds), facing the way you face.
* **Go** gathers sparks round you for one second (`teleportChargeTicks`, 20 ticks) and then takes you there. It needs **30 seconds** to recharge (`teleportCooldown`, 600 ticks) and costs **1.5 food points** of hunger (`teleportExhaustion` 6.0; Creative pays nothing). The book says "The book is gathering its strength" until it is ready, and if the arrival is refused you get the recharge back.
* **Where it will not go.** A boss fight keeps its fighters in and everyone else out; a Hex wall judges you as it does every traveller; a player pinned by Mjolnir cannot leave; and the **Witches' Road** is always shut to the book (you cannot save a place there, leave it or be taken into it). A server can add more dimensions to the list `vishanti.deniedDimensions`.
* The Bifrost of Thor's weapons keeps its own list; the Book of the Vishanti has a separate one, of its own, with its own recharge.

### The ward: no monster can enter your base

**Save as base** saves the place as a **base** and raises a **ward** round it: a sphere of shimmering light, centred where you stood, that **no monster can enter**.

* **How big.** It depends on the sorcerer, as the first table shows: Wanda 48 blocks, Loki 24, Billy from 12 to 40 by his rank (12, 15, 18, 21, 24, 28, 31, 34, 37 and 40 for ranks 1 to 10). It is a full sphere, so a base under the ground is covered too. The radius is refreshed every five seconds while you are online and whenever you save or forget a place, so a Billy who ranks up sees his ward grow.
* **What it does.** Monsters (the creatures of the monster category: zombies, skeletons, creepers, spiders, witches, phantoms and so on) **do not spawn inside it** (natural spawns, spawners and structures), and one that is inside is **thrown back out** if it is near the edge, or set down just outside the wall if it is deeper. It looks (once every 5 ticks) at the monsters inside; nothing is placed or broken, so it is no trouble for claims.
* **What it does not touch.** Bosses (the Ender Dragon, the Wither, the Warden, the Elder Guardian, Thanos, Ultron and Apocalypse and their helpers), anything in a boss fight or arena, pets that have an owner, creatures that ride or are ridden, and every creature that is not a monster (villagers, animals).
* **How many.** Up to **2 bases** (so 2 wards) at a time per player (`maxWardsPerPlayer`). Raising one needs **10 seconds** to recharge (`wardCooldown`) and costs **2 food points** of hunger (`wardExhaustion` 8.0). A ward cannot be raised in a boss arena, in the Witches' Road, or where it would reach into a boss arena. Forgetting a base takes its ward with it.
* **It stays.** Wards are saved with the world and keep working **whoever is online**, as long as their land is loaded. Only raising or forgetting needs the book. A ward whose owner is online but has lost the sorcerer's power sleeps (it is kept, but does nothing and is not drawn) and wakes if he is one again.
* **You see it:** a faint veil and hexagon lines on a sphere in the sorcerer's colour (crimson for Wanda, blue for Billy, green for Loki) and a ring on the ground, only when you are within 64 blocks of its edge. *Reduced motion* stops the drifting and breathing.
* **Operators:** `/enhanced ward list` lists every ward (whose, where, how far it reaches), and `/enhanced ward remove <player>` takes away every ward of a player (their bases stay as plain places).

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

A sorcerer holds the Darkhold (in either hand) and has four powers from it. None works in the [Witches' Road](Witches-Road). A small panel in the top right of the screen names them while you hold it.

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
* **Billy** fires this beam, not his own Hex Beam, while he holds the book.
* The R key is not used, so it never clashes with a hero's own key: the Darkhold works for a Loki, a Wanda and a Billy alike.

#### The revenge: poison

When **magic** hits you while you hold the Darkhold, **whoever used it gets Poison** for **10 seconds** (200 ticks, Poison I). What counts as magic: the Hex Beam and Hex Bolt, the Chaos Beam, Thor's lightning beam, a telekinetic slam, the Mind Stone, the Stones' powers, the Phoenix Burst, the Purple Beam, a witch's potions and anything the game itself calls magic.

* It answers the **use** of magic even if a Chaos Shield stops the hit.
* The same attacker is not poisoned again for 2 seconds (`poisonCooldownTicks`, 40), so a beam does not renew it every tick.
* A hit with no living attacker, or your own, answers nothing. **Undead** shrug poison off, as in the game.
* A server can swap it: `darkhold.poisonTarget` = `ATTACKER` (the default) or `HOLDER` (you pay for the magic the book drew).

#### The hex mark

**Sneak + use** while you look at a creature within **24 blocks**: it is **marked** for 10 seconds. It **glows**, gets **Slowness II**, and takes **half as much damage again** (x1.5) from you, the one who marked it. Cooldown 5 seconds, cost 2 exhaustion. A player is marked only where you could harm them (PvP on).

#### The mind glimpse

**Sneak + use** while you look at no creature: every living creature within **24 blocks** (the nearest 16) **glows for 5 seconds**, and the action bar says "The Darkhold shows 2 creatures. Nearest: Zombie, 5.0 of 20 health". Cooldown 10 seconds, cost 1 exhaustion.

## Numbers you can change

The book and the ward are the `vishanti.*` options; the Darkhold is the `darkhold.*` options. Both are on [Server Configuration](Server-Configuration#the-book-of-the-vishanti) and [Server Configuration](Server-Configuration#the-darkhold). Two datapack tags matter: `enhanced:ward_immune` (what the ward leaves alone: by default the bosses and their helpers) and `enhanced:magic_damage` (what counts as magic for the Darkhold's revenge).

## Questions

**Can I use the books in the Witches' Road?** No: they are shut there, on purpose, so the trials stay the trials.

**Does the ward need me nearby?** No. It works whenever the land is loaded, whoever is online.

**My ward is smaller than my friend's.** The stronger the sorcerer, the bigger the ward: Wanda 48, Loki 24, Billy from 12 up. Rank up in [Build Magic](Build-Magic) to grow Billy's.

**Why did the book not take me there?** The arrival can be refused by a boss fight, a Hex wall, Mjolnir's pin or a shut world. You get the recharge back when that happens.

## Related

[Items and Crafting](Items-and-Crafting) · [Loki](Loki) · [Scarlet Witch](Scarlet-Witch) · [Billy](Billy-and-the-Witches-Road) · [Thor](Thor) · [Commands](Commands) · [Controls](Controls)

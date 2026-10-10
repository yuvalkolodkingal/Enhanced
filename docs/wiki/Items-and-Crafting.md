# Items and Crafting

Every item you can use is in the **Enhanced** creative tab. Seven are crafted, and three are found in chests (or dropped). In the recipe grids below, `.` is an empty slot, and the recipes are **shaped**: the items must sit exactly as drawn.

## Crafted items

| Item | Becomes | Recipe book unlock |
|---|---|---|
| [Magneto's Helmet](#magnetos-helmet) | [Magneto](Magneto) | Picking up a Block of Netherite |
| [Wanda's Crown](#wandas-crown) | Transforms the [Scarlet Witch](Scarlet-Witch) | Picking up a Gold Ingot |
| [Quicksilver's Goggles](#quicksilvers-goggles) | [Quicksilver](Quicksilver) | Picking up Sugar |
| [Synthezoid Core](#synthezoid-core) | [Vision](Vision) (with Loki's Scepter) | Picking up a Nether Star |
| [Witches' Road Door](#witches-road-door) | [Tommy or Billy](Billy-and-the-Witches-Road) (with a gene) | Picking up an Eye of Ender |
| [Mjolnir](#mjolnir) | [Thor](Thor) (if worthy) | Picking up a Heavy Core |
| [Stormbreaker](#stormbreaker) | Nothing: anyone can wield it | Picking up a Nether Star |

### Magneto's Helmet

```
N N N
N . N

N = Block of Netherite
```

Worn, it makes you [Magneto](Magneto). It protects like a full Netherite set (20 armor, 12 toughness, 40% knockback resistance), blocks Nausea, and never breaks.

### Wanda's Crown

```
G R G
G . G

G = Gold Ingot
R = Block of Redstone
```

Given to the Scarlet Witch when she awakens, and craftable if lost. On her: 20 hearts and Diamond-level protection. It never breaks. On anyone else it is just a crown.

### Quicksilver's Goggles

```
S I S
G D G

S = Sugar
I = Iron Ingot
G = Glass Pane
D = Block of Diamond
```

Worn, they make you [Quicksilver](Quicksilver). They protect like an iron helmet and never break.

### Synthezoid Core

```
R N R
N S N
R N R

R = Block of Redstone
N = Netherite Ingot
S = Nether Star
```

A synthetic body waiting for a mind. Hold it in one hand and use Loki's Scepter with the other, and you become [Vision](Vision).

### Witches' Road Door

```
A L A
T E T
A L A

A = Amethyst Shard
L = Soul Lantern
T = Dark Oak Trapdoor
E = Eye of Ender
```

A door that is placed flat in the floor. Open it holding a Scarlet Witch Gene: see [Billy and the Witches' Road](Billy-and-the-Witches-Road). An axe breaks it fastest, pistons can't push it, and breaking it drops it again.

### Mjolnir

```
N S N
N H N
. R .

N = Netherite Ingot
S = Nether Star
H = Heavy Core
R = Breeze Rod
```

Only the worthy can lift it, and it is bound to the first worthy player who does (its tooltip says "Unclaimed" until then, and "Bound to" and the owner's name after). 10 attack damage (slow, like an axe), never breaks, fire and lava can't hurt it. In Thor's hands it shatters into fragments that strike his enemies (Sneak + R), and its owner can pin someone down with it (Sneak + right-click on a creature or player). See [Thor](Thor).

### Stormbreaker

```
N N
N S
. L

N = Netherite Ingot
S = Nether Star
L = any log
```

An axe that hits harder than any other (12 attack damage), never breaks, can't burn. Anyone can wield it, and whoever holds it can call the Bifrost. See [Thor](Thor).

## Items found in the world

These three are never crafted. Each chance is **per chest generated** (or per kill, for the mobs): a chest of that kind has that chance to hold the item.

### Scarlet Witch Gene

Very rare. On its own it does nothing at all: it is held while touching the Mind Stone ([Scarlet Witch](Scarlet-Witch)) or while opening the Witches' Road Door ([Billy and the Witches' Road](Billy-and-the-Witches-Road)).

| Source | Chance |
|---|---|
| Woodland Mansion chest | 5% |
| Stronghold library chest | 4% |
| Stronghold corridor chest | 2% |
| Stronghold crossing chest | 2% |
| Nether Fortress chest | 3% |
| Bastion treasure chest | 5% |
| Ancient City chest | 4% |
| End City treasure chest | 5% |
| Jungle Temple chest | 3% |
| Desert Pyramid chest | 2% |
| Witch, killed by a player | 1% (+0.5% per Looting level) |
| Evoker, killed by a player | 2% (+1% per Looting level) |

A stack holds up to 16 genes.

### Loki's Scepter

The Mind Stone sits in the blue gem of its golden blade. Touching it with the gene in your other hand makes you the Scarlet Witch, with a Synthezoid Core makes you Vision, and with neither **kills you**. It is also a strong melee weapon (8 attack damage, never breaks).

| Source | Chance |
|---|---|
| End City treasure chest | 10% |
| Stronghold library chest | 5% |
| Bastion treasure chest | 4% |
| Ancient City chest | 3% |
| Woodland Mansion chest | 3% |

### Tesseract

The Space Stone in its cube. Using it as a player with no power makes you [Loki](Loki), far away; a hero who uses it jumps to the block they look at. It is never used up.

| Source | Chance |
|---|---|
| End City treasure chest | 8% |
| Stronghold library chest | 6% |
| Woodland Mansion chest | 5% |
| Ancient City chest | 4% |

## Changing the loot

The chances above are NeoForge *global loot modifiers* in the mod's data (`data/enhanced/loot_modifiers/`), so a datapack can change them. The recipes are ordinary recipe files (`data/enhanced/recipe/`).

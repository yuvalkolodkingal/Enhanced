# Stone Locator

Eilon's idea (3.7): **you must already have one Infinity Stone.** The **Stone Locator** reads that stone's energy and searches for **similar energies**: a compass needle that points to the **nearest place that still has a stone you haven't got**. A gold-and-black disc of netherite with a glass lens over the needle, and a socket that glows in the colour of the stone it was tuned to.

Hebrew: [עברית](Stone-Locator-HE)

![The Infinity Stones](images/114-infinity-stones.png)

## The quick version

* **Craft it** with 4 netherite blocks, 3 diamond blocks, a wither skeleton skull and **an Infinity Stone in the middle**. **The stone comes back** to you.
* **Hold it**: the needle points to the nearest site of a stone you lack, and a line over your hotbar says which, how far and which way: "Soul Stone · 810 blocks · north-east".
* **Use** (right-click): a trail of sparks flies toward it. **Sneak + use**: choose which stone to look for.
* **It spins** when what you look for isn't in this world. See [why](#why-is-it-spinning).

## The recipe

![Books in item frames](images/116-item-frames-books.png)

```
N K N
D S D
N D N

N = Block of Netherite
K = Wither Skeleton Skull
D = Block of Diamond
S = an Infinity Stone, or a stone's vessel
```

* **In the middle:** any of the six [Infinity Stones](Infinity-Stones), or a **vessel** of one: **Loki's Scepter** (the Mind Stone), the **Tesseract** (Space), the **Eye of Agamotto** (Time), **the Orb** (Power), or a crafted **Infinity Gauntlet** with at least one stone in it. (A vessel counts because only the gods may hold a bare stone.) Never Thanos's Gauntlet, and never an empty one.
* **The stone comes back.** It stays in the middle of the grid when you take the Locator: exactly the same stone, scepter or Gauntlet, with its owner's name and its stones. Only the netherite, the diamonds and the skull are used up. (A bare stone in the grid is safe there; once you take it out, the usual [rules for holding a stone](Infinity-Stones#who-may-hold-a-stone) apply.)
* **It is tuned** to that stone: "It hums with the Time Stone's energy." Its socket and lens glow in that stone's colour. It searches the same whatever it was tuned to.
* The recipe shows in your recipe book when you pick up a stone or a vessel. The advancement **Similar Energies** is for making one. A server can switch the recipe off (`stone_locator.craftable`) or keep the stone (`stone_locator.returnsCore`).

**Anyone** may hold and use the Locator: the stone's energy is in the device now. It doesn't stack, and it doesn't burn.

## What it points to

![The Scepter and the Tesseract](images/115-artifacts-scepter-tesseract.png)

**The stones you haven't got.** A stone counts as *got* when you **carry** it, in any form: the bare stone, in a Gauntlet, in its vessel, inside the Orb. The four stones of the sites (Reality, Time, Power, Soul) also count as got when the server's [ledger](Infinity-Stones#how-many-stones-the-ledger) **won't give it to you again** (you took that one before). Of the ones you lack, the needle points to the **nearest site**.

| Stone | Where it points | The site |
|---|---|---|
| **Mind** | a [Hydra outpost](Structures#a-hydra-outpost) (its vault holds Loki's Scepter) | Overworld |
| **Time** | a [Sanctum Sanctorum](Structures#the-sanctum-sanctorum) | Overworld |
| **Space** | an [Avengers Tower](Structures#avengers-tower) (the Tesseract on its roof) | Overworld |
| **Power** | an [End tunnel](Structures#the-end-tunnel) (the Orb) | the End |
| **Soul** | a [Vormir Cliff](Vormir) | the Nether |
| **Reality** | the pedestal in Odin's vault | [Asgard](Asgard) |

**Sites that have nothing left are skipped:** a tower whose Tesseract was taken, an End tunnel whose vault chest was opened, a Hydra outpost whose vault chest was opened. The Sanctums, the Vormir Cliffs and Asgard's vault are never skipped: their pedestals keep a stone for each player who may still take one.

**Use** (right-click): a **spark trail** in the stone's colour flies 12 blocks toward the site and fades, with a deep hum; everyone near sees it. Then 5 seconds before the next.

**Sneak + use** chooses what it looks for: **every stone you lack** (the nearest of them), then **each missing stone in turn**: "The Locator looks for the Soul Stone." The tooltip shows it too.

**You have all six?** "You have all six. Nothing is left to find." The needle points north and the lens glows white.

## In the wrong world

Some stones are in other worlds. Where the way there is easy to find, the needle points to the **way**:

| Where you are | Looking for | The Locator |
|---|---|---|
| Overworld | the **Power Stone** | points to the nearest **stronghold** (the way to the End): "The Power Stone is in the End: the way there is a stronghold." |
| Overworld | the **Reality Stone** | points to the nearest **Bifrost site** (its rune stone takes you to Asgard): "The Reality Stone is beyond the Bifrost, in Asgard." Before Hela's fall: "The Reality Stone is sealed in Asgard's vault until Hela falls." (still the Bifrost site) |
| Overworld | the **Soul Stone** | spins: "The Soul Stone's energy comes from the Nether." |
| the Nether | the **Soul Stone** | a Vormir Cliff; every other stone spins: "Not in this realm." |
| the End | the **Power Stone** | an End tunnel; every other stone spins |
| Asgard | the **Reality Stone** | the vault's pedestal (while the vault is open); every other stone: "Not in Asgard." |
| the Witches' Road | anything | spins: "The Road hides everything." |

A server can switch off the ways (`stone_locator.pointsToGateways`): then it spins there too, with the same words.

## Why is it spinning?

* The stone you look for is in **another world** (the table above), or you are on the **Witches' Road**.
* **"Searching for the stones…"**: it is still looking (the needle sweeps slowly). Far places take a few seconds.
* **"Too far: no site within 4096 blocks."** Nothing of that kind that close (1024 blocks in the Nether). Explore, or choose another stone with **Sneak + use**.
* **"Someone else holds the Time Stone."**: on a server where each site stone exists **once per world**, another player has it, so the Locator skips it.
* **You have all six** (then it points north).

## The HUD

While you hold it, one line over the hotbar: the stone's gem and colour, "Soul Stone · 810 blocks · north-east", or what it is doing ("Searching for the Soul Stone…", "Not in this realm.", "Too far…"), with a grey second line for the way to another world. In Hebrew too.

## It never lags the server

It searches only **while someone holds a Locator**, a little at a time: at most 2 far places are looked at in a tick on the whole server, and what one player found is shared with the others. Its answer is kept for 30 seconds, or until you walk 64 blocks.

## Cerebro uses it too

At the [Cerebro console](Mutant-School-Lab#cerebro) of the X-Men lower lab, the amplified Cerebro marks the nearest stone sites of its world on its sphere, with the same search.

## Numbers you can change

Section `[stone_locator]`: the recipe on or off, whether the stone comes back, how far it searches (4096, the Nether 1024), how much it may search in a tick, how long an answer is kept and how far you may walk, the spark trail's wait, and whether it points to the ways to other worlds. See [Server Configuration](Server-Configuration#the-stone-locator).

## Related

[Infinity Stones](Infinity-Stones) · [Structures](Structures#the-stone-sites-36) · [Vormir](Vormir) · [Asgard](Asgard) · [Items and Crafting](Items-and-Crafting#stone-locator-37) · [FAQ](FAQ#why-is-my-locator-spinning)

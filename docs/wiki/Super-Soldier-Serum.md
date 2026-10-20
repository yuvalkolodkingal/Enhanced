# The Super Soldier Serum

A glass vial of glowing blue serum in a steel cage (3.8): Dr. Erskine's formula of *Captain America: The First Avenger*, the one that turned Steve Rogers into a super-soldier. Use it and a screen asks you **which super-soldier you want to be**: **[Captain America](Captain-America)**, **[Red Hulk](Red-Hulk)** or **[Falcon](Falcon)**. Falcon is locked until you carry his **Exo-Wings**.

Hebrew: [עברית](Super-Soldier-Serum-HE)

## The quick version

* **Where:** in the **lab** of the [Avengers Tower](Structures#avengers-tower), on the workbench between the brewing stands, in a glass **Tech Case** with the sign "Project Rebirth". Right-click the case: one serum for each player.
* **Use it** (right-click): the choice opens. Pick a card and press **Choose** (or the keys **1**, **2**, **3** and **Enter**).
* **Your old hero is replaced**, exactly as `/enhanced power set` would do it. The serum is used up only when you choose: **Escape** or **Cancel** keeps it.
* **Falcon needs his wings:** carry the **Falcon Exo-Wings** (from the very top of the tower) anywhere in your inventory, and his card unlocks.
* There is **no Iron Man card**: you become [Iron Man](Iron-Man) with an **Arc Reactor**.

## Getting a serum

1. Find an **Avengers Tower** (`/locate structure enhanced:avengers_tower`), the tall dark glass tower with the "A" ring near the top.
2. Climb the ladder in its core to the floor whose door says **THE LAB**.
3. On the iron workbench, between the two brewing stands, stands a glass **Tech Case** with a blue vial turning inside, and a sign "Project Rebirth" on the bench's front. **Right-click it**: the lid slides up and the serum is yours.

Each case gives **one serum to each player** ("You already took the Super Soldier Serum from this case."), so two brothers both get one. A second tower has a second case. Towers built before 3.8 get the case too, the first time someone comes near (see [the refit](Structures#towers-built-before-38-the-refit)).

The serum has **no recipe** unless a server switches one on (`super_soldier_serum.craftable`, for worlds with no towers):

```
B G B
D W D
B N B

B = Blue Stained Glass Pane
G = Golden Apple
D = Diamond
W = Water Bottle
N = Nether Star
```

## The choice

Right-click with the serum (in either hand) and the vial glows and hums. A dark-blue screen opens, **"Super Soldier Serum: Choose your super-soldier."**, with three tall cards. On each card the hero stands and turns slowly, in his own look:

| Key | Card | What it says |
|---|---|---|
| **1** | **[Captain America](Captain-America)** | Strength, speed and a vibranium shield |
| **2** | **[Red Hulk](Red-Hulk)** | Bigger, stronger, hotter when angry |
| **3** | **[Falcon](Falcon)** | Flight with the Exo-Wings and Redwing |

* **Click a card** (or press its number) to pick it; a gold frame shows which. Then press **Choose** on it, or **Enter**.
* **Escape** or **Cancel** closes the screen and nothing happens: you keep the serum.
* The screen fits every GUI scale, and in Hebrew the text reads right to left (the cards stay in the same order).

When you choose:

* The serum is **used up** (not in Creative), a rising chord plays, and you **become that hero** with his title and flash, and everyone is told ("Eilon took the Super Soldier Serum and became Captain America!").
* **Your old hero is cleared completely**, the same way as `/enhanced power set`: his toggles, his Hex, his Build Magic, the gear he wore goes into your bag.
* **Captain America** also gets a **[Vibranium Shield](Captain-America#the-vibranium-shield)** if he has none: it goes into the hand that held the serum.
* **Red Hulk** grows from your size into his, if there is room ([Red Hulk](Red-Hulk#becoming-red-hulk)).
* **Falcon** gets nothing more: his wings are the ones you carry.

## Why is Falcon locked?

Sam Wilson is not a super-soldier: his wings are his power. So the serum's Falcon card **needs the wings**: it is grey with a padlock and says **"Needs the Falcon Exo-Wings (the top of the Avengers Tower)."** until you carry a pair of **Falcon Exo-Wings** anywhere: in your inventory, in your off hand or on your chest.

The wings are in the **observation deck**, the highest room of the [Avengers Tower](Structures#avengers-tower), in a Tech Case between two end rods, with the sign "FALCON / Exo-Wings". One pair for each player. Take them, use the serum again, and the Falcon card is open.

The server checks again when you choose: if the wings are no longer with you, you hear "Needs the Falcon Exo-Wings" and keep your serum.

## When the serum says no

The serum is never used up when a choice is refused. You are told why, in the action bar:

| You see | Why |
|---|---|
| "You are already Captain America." | You chose the hero you already are. |
| "You waited too long. Use the serum again." | The screen was open for more than 30 seconds (`super_soldier_serum.pendingTicks`). |
| "Keep the serum in the same hand to choose." | The serum left the hand (or the slot) you used it from. |
| "Use the serum first." | A choice arrived with no open serum (for example after you died or changed world). |
| "Needs the Falcon Exo-Wings (the top of the Avengers Tower)." | Falcon, without the wings. |
| "The serum does nothing on this server." | The server turned it off (`super_soldier_serum.enabled`). |

Dying, logging out or changing dimension forgets an open choice. Spectators can't use it.

## For operators

`/enhanced serum open <player>` opens the choice for a player without any serum: nothing is used up. See [Commands](Commands#the-avengers-38).

## The advancement

**Super-soldier** ("Find a Super Soldier Serum") is the first advancement of the new **Avengers** tab.

## Numbers you can change

Section `[super_soldier_serum]`: `enabled`, `pendingTicks` (600), `craftable` (false) and `casePerPlayer` (1, how many serums each player may take from one case; 0: no limit). See [Server Configuration](Server-Configuration#the-super-soldier-serum).

## Related

[Captain America](Captain-America) · [Red Hulk](Red-Hulk) · [Falcon](Falcon) · [Iron Man](Iron-Man) · [Structures](Structures#avengers-tower) · [Items and Crafting](Items-and-Crafting#the-avengers-gear-38)

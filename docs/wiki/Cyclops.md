# Cyclops

Scott Summers, the leader of the X-Men. His eyes fire a red blast of pure force that he cannot shut off: only his **ruby-quartz glasses** hold it back. In the mod he fires an aimed **Optic Blast** through the glasses, a bouncing shot, a wide blast, a leap on his own beam, and calls the X-Men to him. And if the glasses come off, a **giant red beam** pours out of his eyes and does not stop until they are back on.

Hebrew: [עברית](Cyclops-HE)

![Cyclops](images/045-cyclops.png)

## Becoming Cyclops

Find his **Ruby-Quartz Glasses** and **put them on** (the head slot, like a helmet) with no power: "The ruby quartz holds it back", and you are Cyclops, in the *X-Men: Apocalypse* suit. Everyone on the server is told.

* **Where they are.** In the [Mutant School](Structures#the-mutant-school), in a chest in **Scott Summers' room** (always one pair). Rarely, in a woodland mansion chest or a stronghold library chest (2% each). They have no recipe.
* On a hero who already has a power they are only glasses (red sunglasses), and change nothing.
* They give 1 point of armour, never break, cannot burn and do not stack.

## His body

* **Max health 30** (15 hearts) and **4 armour** from his field suit.
* He does not fly: he leaps on his own blast (**V**).
* He is a **mutant without telepathy**: [Professor X](Professor-X) can hear his thoughts and take over his body (with PvP on).

## Abilities

![Cyclops's optic blast](images/046-cyclops-optic-blast.png)

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (hold) | Optic Blast (Sneak: Ricochet Shot) | A straight red beam from his eyes to what the crosshair is on, for as long as you hold R: **2 damage every 5 ticks** (8 a second), **32 blocks**, and each hit shoves what it hits back (his blasts are force, not heat). It costs a little food (0.6 exhaustion a second). **Sneak + R** fires the **Ricochet Shot**: one bolt that bounces off blocks up to **3 times**, 24 blocks a stretch, and hits each creature on its path once for **7 damage**. Cooldown 2 seconds. A Chaos Shield or a Hex Barrier stops it. |
| 2 | **G** | Concussion Blast | A short, wide blast in front of him (a cone of 40 degrees either side, 7 blocks, in his line of sight): **6 damage** and a strong throw back, and it turns projectiles away. It spares allies, players when PvP is off, and peaceful animals. Cooldown 5 seconds. |
| 3 | **V** | Optic Leap | He blasts the ground under him (it must be within 4 blocks below) and is thrown **up and forward**; the blast hurts the creatures round the spot (radius 2.5, 3 damage), and **he takes no fall damage** until he lands. Cooldown 4 seconds. |
| 4 | **B** | Rally | "To me, X-Men!": he, the players, tamed pets and golems within **16 blocks** get **Speed I, Strength I and Resistance I** for **15 seconds**. It works with the glasses off too. Cooldown 45 seconds. |
| 5 | **N** (Sneak + N) | Glasses | **Sneak + N** takes the glasses **off** (into your hand, else your inventory); **N** puts them back **on**. N never has to wait. |

While the glasses are off, the eye blasts (R, Sneak + R, G and V) say "You need your glasses to aim your eyes". The HUD line of N says `ON`, `OFF`, or, in red, `EYES OPEN`.

## The glasses off: the giant beam

The moment the glasses leave his head, **in any way** (Sneak + N, dragged out of the slot in the inventory, a command), a **giant red beam** erupts from his eyes, where he looks, with three spiralling strands. "The glasses are off! Put them back on to stop it."

* **It never stops by itself.** It burns until the glasses are back on his head: press **N** (from your hand, the inventory or the off hand), right-click them, or put them in the head slot. If he has no glasses anywhere, **N** makes him squeeze his eyes shut, and that stops it too.
* **It is big and strong.** 48 blocks long, 1.2 blocks thick: everything near its line up to the first block is hit for **6 damage** twice a second and shoved along it, through creatures. An uncontrolled beam hurts animals too; players only with PvP on; his teammates and pets are spared.
* **It is safe for him.** It never hurts Cyclops, costs no food, and ends when he **dies, logs out, loses the power or becomes a Spectator**. Coming back (respawn or login) without the glasses never starts it again: only taking them off does.
* **It breaks no blocks** unless the server turns that on (`cyclops.unleashedBreaksBlocks`); even then only blocks no harder than 3 (stone is 1.5), never round his own feet, never in a boss arena or where he may not break.
* He holds both hands to his temples and trembles while it pours out; his eyes blaze.

## Looks

![Cyclops in the classic suit](images/044-cyclops-classic-suit.png)

| Look | Id | What it is |
|---|---|---|
| **X-Men: Apocalypse (1983)** (default) | `apocalypse` | The field suit of *X-Men: Apocalypse*: navy with black side panels and quilted shoulders, steel-blue piping, a silver X on the belt buckle, black gloves and boots, brown hair, and a gunmetal wrap-around visor with one ruby lens. |
| **Classic (comics)** | `classic` | The comics' blue bodysuit with a yellow X harness, belt, gloves and boots, a blue cowl, and a yellow visor band all round with a red lens. |
| **Your own skin** | `own` | Cyclops's powers in your own skin, with red sunglasses. |

The glasses are drawn over the face of every look, and the lens glows a little all the time and brightly while he fires. Press **H** to switch. See [Looks](Looks).

## Numbers you can change

His health and armour, the Optic Blast (range, damage, rate, push, food), the giant beam (range, damage, rate, thickness, push, and whether it breaks blocks and how hard), the Ricochet Shot, the Concussion Blast, the Optic Leap and the Rally are the `cyclops.*` options. See [Server Configuration](Server-Configuration#cyclops).

## Related

[Professor X](Professor-X) · [The Mutant School](Structures#the-mutant-school) · [Controls](Controls) · [Becoming a Hero](Becoming-a-Hero)

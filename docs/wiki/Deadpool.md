# Deadpool

Wade Wilson (3.7), the mercenary with a mouth, two katanas, two pistols, a glitchy teleport belt and the same **healing factor** as Wolverine. He knows he's in a game, and says so. He cuts, shoots, blinks across the battlefield, drops in on his foes from above, makes monsters chase him instead of his friends, and whistles for **Dogpool**. Kid-safe: his pistols are bright cartoon pistols, nobody bleeds, and every joke is our own.

Hebrew: [עברית](Deadpool-HE)

## The quick version

* **How:** in the [X-Men lower lab](Mutant-School-Lab), with **no power**, step into the **Weapon X Pod** and right-click it. Five seconds later you are Deadpool.
* **R** Katanas, **G** Pistols, **V** Teleport Belt, **B** Taunt, **N** Dogpool. **Sneak** changes each of them: Parry (hold), Trick Shot, Drop-In, Chimichanga Break, Smoke Vanish.
* **The [healing factor](Healing-Factor):** he heals all the time, and a blow that would kill him only knocks him down for 5 seconds. ("Nap time...")
* He doesn't fly: he blinks.

## Becoming Deadpool: the Weapon X Pod

1. Go down to the [X-Men lower lab](Mutant-School-Lab) under a Mutant School, to the **Weapon X lab** on the lower level. The **Weapon X Pod** stands at its end: a two-high steel pod with a glass front and a green light.
2. With **no power**, **step inside the pod** and **right-click it**. The hatch closes ("The hatch closes. The treatment begins."), you can't move, and for **5 seconds** bubbles rise, a hum climbs and a heart beats: "Treatment… 5… 4…".
3. A red flash: you are **Deadpool** ("Healing factor: on"), and he says his first joke. Everyone is told: "Eilon came out of the Weapon X Pod as Deadpool!"

**Sneak**, or stepping out, stops the treatment and opens the hatch. A **hero** is refused: "The pod beeps: this one already has powers." One player at a time ("Someone is already in the pod."). The pod is a machine: it works again and again, for every powerless player who steps in.

You also become Deadpool with **Choose a Hero** in the [Hero Creator](Hero-Creator) (with **3 chimichangas**), or with `/enhanced power set <player> deadpool` (and a look: `classic`, `trainee`, `kidpool` or `own`).

**Losing it:** the Mutant Cure, `/enhanced power set <player> none`, being sacrificed at Vormir, or choosing another hero.

## The body

* **30 health** (15 hearts), **15% faster**, every blow **+2** stronger and faster to swing, a little higher jump, 3 more blocks of fall before it hurts.
* **The [healing factor](Healing-Factor)**, the same as Wolverine's.
* Not heavy: he swims.
* His two katanas are always on his back and his two pistols on his hips; they come into his empty hands when he uses them.

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Katanas | Both swords on the first creature in front of him (a 70° cone, 3.5 blocks): **5**, **5**, then an X-cut of **8** that sweeps up to three. A cut every 8 ticks; after a second and a half without one, the combo starts again. **Sneak + R, held: Parry**: the swords spin in front of him, and arrows, bolts and fireballs coming from the front **bounce off the way he looks** (beams don't); a blow from the front loses 60%. At most 4 seconds, then **R** rests 3 seconds. |
| 2 | **G** | Pistols | A pair of shots, left and right: bright gold tracers, **4** each, 48 blocks. **16 shots**, then he reloads by himself in 2 seconds (a twirl and a click: "Reloading!"). **Sneak + G: Trick Shot**: one shot of **8** that bounces off the first block it hits toward the nearest creature (within 12 blocks of the bounce) he may hurt: round a corner. Then **G** rests 4 seconds. His shots fly past villagers and animals. |
| 3 | **V** | Teleport Belt | A red-black flicker and a "zzt", and he stands on the block he looks at (up to **24 blocks**), or the nearest safe spot next to it. Never into a wall or water, never across a Hex's wall, a boss arena's edge or a Road trial's. The belt **glitches** now and then (one time in ten): he lands 3 to 6 blocks off, still somewhere safe, with a joke ("Wrong turn. The belt is having a day."). Nowhere to land: nothing happens, and nothing waits. Cooldown 2 seconds. **Sneak + V: Drop-In**: he appears **8 blocks above** the creature he looks at (up to 32 blocks, with room above it) and drops on it sword first: **8**, a ring of dust, no fall damage. Cooldown 8 seconds. |
| 4 | **B** | Taunt | He strikes a pose (a wave, finger guns or a heart) and a **joke** pops up over his head ("Hey, over here! I'm the one with snacks."). Every monster within **16 blocks** (not a boss) **turns on him** for 5 seconds: protect your friends. Cooldown 10 seconds. **Sneak + B: Chimichanga Break**: the [Healing Burst](Healing-Factor#the-healing-burst), half his health back. With a **chimichanga** in his bag he eats it (a munch, a happy sigh): **all his health** back and **Absorption II** for 30 seconds. Refused while poisoned, and then nothing is eaten. It waits 20 seconds on its own clock. |
| 5 | **N** | Dogpool | He whistles, and **Dogpool** pops out of a puff of red smoke: a little dog in a red-and-black Deadpool hood and goggles. **20 health**, a bite of **4**; he follows Deadpool, attacks what Deadpool attacks and what attacks him, and never bites a player while PvP is off. He stays **2 minutes**, or until **N** sends him home. At 0 health Dogpool **goes home for a nap** in a poof: he never dies. Nobody can sit, feed, breed or leash him. One at a time; cooldown 60 seconds from the whistle. **Sneak + N: Smoke Vanish**: a smoke bomb: **invisible for 4 seconds**, and every monster within 16 blocks loses him. It waits 20 seconds on its own clock. |

With PvP off, his abilities don't hurt players.

## His jokes

Deadpool talks. His lines pop up in a **speech bubble** over his head for everyone within 24 blocks, each player in their own language, and in his own action bar: when he taunts, goes down ("Nap time..."), rises ("Ow. Okay. I'm fine. Totally fine."), glitches, calls Dogpool, eats a chimichanga ("Best. Snack. Ever."), is poisoned ("Ugh, poison! My healing just went on a coffee break."), meets a Wolverine ("Nice claws. Do they come in pink?"), lies down next to another healer ("We could do this all day. And we will."), and now and then to *you* ("Hi, whoever is holding the mouse!"). Forty-two lines, at most one every 10 seconds (the Taunt's always). The client option `deadpoolLines` hides them for you, and a server can turn them off (`deadpool.linesEnabled`).

[Professor X](Professor-X) hears his thoughts: "Did I leave the oven on?", "Note to self: buy more chimichangas." And Dogpool's: "Ball? BALL?"

## The chimichanga

A crunchy, warm snack, and Deadpool's favourite. Anyone can eat it (**4 drumsticks**). Crafted from a **bread**, a **cooked beef, porkchop or chicken** and a **carrot** (any order): **two** chimichangas. Found in the kitchen chest of [Agatha's Cottage](Agathas-Cottage) too. Deadpool's **Chimichanga Break** eats one for a bigger heal. The advancement **Maximum Snacks** is for eating one.

## Looks

| Look | Id | What it is |
|---|---|---|
| **Classic suit** (default) | `classic` | The red-and-black suit of *Deadpool & Wolverine*: red, black side panels, black eye patches with white eyes, a grey harness, a dark belt with our own red-and-black buckle. |
| **X-Men trainee** | `trainee` | The school's yellow-and-black trainee uniform, with his red mask. |
| **Kidpool** | `kidpool` | A kid Deadpool of the Deadpool Corps: **smaller** (four-fifths of a player), brighter red, a big mask, a short red cape. The same powers. |
| **Your own skin** | `own` | His powers in your own skin. |

Press **H** to switch. See [Looks](Looks).

## The HUD

Next to the usual panel: the [healing factor](Healing-Factor#the-hud)'s line, the cut of the combo ("cut 2/3"), the Parry's seconds, the magazine ("16/16" or "Reloading…"), how many chimichangas he carries, and Dogpool's time and health ("Dogpool 1:58 (20♥)").

## Against the others

* **What defeats him:** the same as Wolverine: the Infinity Stones, chaos magic, Odin's power, the Phoenix, the void, `/kill`, and anything while he is poisoned. See [Healing Factor](Healing-Factor#what-defeats-him).
* **[Wolverine](Wolverine)** and Deadpool can't finish each other: "Good news: nobody loses. Bad news: nobody wins."
* At [Agatha's Cottage](Agathas-Cottage): "You're not getting the recipe."

## Deadpool in the Hero Creator

His abilities are in the [Hero Creator](Hero-Creator) (`deadpool.katanas`, `deadpool.pistols`, `deadpool.belt`, `deadpool.taunt`, `deadpool.dogpool`, `deadpool.body`), and his five Sneak variants are cards of their own (`deadpool.parry`, `deadpool.trick_shot`, `deadpool.drop_in`, `deadpool.chimichanga`, `deadpool.smoke`). **Deadpool's body** gives a custom hero the healing factor. The Parry is a shield: it can't go with another hero's shield. See [Heroes' Abilities](Hero-Abilities#deadpool-6). The ready-made hero is *Deadpool-like*, and *Deadpool* is a run style.

## Numbers you can change

Every number of his body, abilities, lines and the pod is a `deadpool.*` option, Dogpool's are `dogpool.*`. See [Server Configuration](Server-Configuration#deadpool-and-dogpool).

## Related

[Healing Factor](Healing-Factor) · [Wolverine](Wolverine) · [The X-Men lower lab](Mutant-School-Lab) · [Controls](Controls) · [Looks](Looks)

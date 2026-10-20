# Iron Man

Tony Stark (3.8): a genius whose power is his **suits**. Press an **Arc Reactor** to your chest and you are Iron Man; then take the suits from **Iron Man's workshop** in the [Avengers Tower](Structures#avengers-tower) and put them on with **H**. Six suits, each with its own moves and its own **suit-up**: the crude **Mark 1** from the cave, the red-and-gold **Mark 3**, the **Mark 5** that unfolds out of a suitcase, the nanotech **Mark 50** and **Mark 85** that flow out of the **Nano Arc Reactor** on your chest, and the giant **Hulkbuster** that Veronica drops from the sky.

Hebrew: [עברית](Iron-Man-HE)

## The quick version

* **Become Iron Man:** take an **Arc Reactor** from Tony's desk in the tower's workshop and **hold right-click for 2 seconds**: it goes into your chest. (Or wear the **Nano Arc Reactor** on your chest.) Your old hero is replaced.
* **Get the suits:** in the same workshop, right-click each **Suit Display**: one of each suit for each player.
* **H** puts a suit on and takes it off. **Sneak + H** chooses which suit H puts on next (with the Nano Arc Reactor on: switches between the Mark 50 and the Mark 85).
* **G** flies (not the Mark 1: its **G** is one rocket boost). **R V B N** are the worn suit's moves; **Sneak** changes some of them.
* Flying and moves use **suit energy** (the blue bar). It comes back by itself, fastest on the ground.
* Without a suit, Tony has no powers: "You need a suit."

## Becoming Iron Man: the Arc Reactors

There are two reactors. Both are on **Tony's desk** in Iron Man's workshop, each in a glass **Tech Case** behind the sign "Arc Reactors": right-click a case to take it (one of each for each player).

* **The Arc Reactor**: a round reactor, a white-blue glowing core in a steel ring. **Hold right-click for 2 seconds**: you press it to your chest, blue light pulls in, it clicks into place and you are **Iron Man** ("Time to suit up!"). Everyone is told: "Eilon put an Arc Reactor in their chest and became Iron Man!" **The reactor is kept**: carried anywhere in your inventory, it powers the classic suits. Using it again: "Your Arc Reactor glows at full power."
* **The Nano Arc Reactor**: the rounded-triangle reactor of *Infinity War* and *Endgame*, in a dark hexagonal case. **Hold right-click for 2 seconds, or put it on your chest slot**: you become Iron Man the same way. Worn on the chest, it is the home of the **nanotech suits** (the Mark 50 and the Mark 85).

Becoming Iron Man **replaces your hero**, exactly as `/enhanced power set` does (a server can make the reactors refuse heroes instead: `iron_man.reactorReplacesHero`; then a hero hears "The Arc Reactor won't go in: you are already a hero.").

You also become Iron Man with **Choose a Hero** in the [Hero Creator](Hero-Creator) (with an **Arc Reactor and a Mark 3**), or with `/enhanced power set <player> iron_man` (and a look: `tony` or `own`). The [Super Soldier Serum](Super-Soldier-Serum) has no Iron Man card.

The reactors have **no recipe** unless a server switches them on (`iron_man.reactorsCraftable`, for worlds with no towers):

```
Arc Reactor            Nano Arc Reactor
I R I                  . N .
R N R                  N A N
I R I                  . N E

I = Block of Iron      N = Netherite Ingot
R = Block of Redstone  A = Arc Reactor
N = Nether Star        E = Echo Shard
```

**Losing it:** the Mutant Cure, `/enhanced power set <player> none`, the [Super Soldier Serum](Super-Soldier-Serum), [Eternity's](Eternity) Love, being sacrificed at Vormir, or choosing another hero. The suits stop working (they stay items), a nanotech suit flows back into the reactor, and a Hulkbuster shrinks back at once.

## The six suits

Every suit stands on a **Suit Display** in Iron Man's workshop (the floor whose door says **IRON MAN workshop**): the Mark 1, 3, 5, 50 and 85 in a row along the north wall under their signs, the Hulkbuster in its own bay south of the desk. **Right-click a display**: its suit is yours, **one of each per player** ("You already took the Mark 3 from this display."). The suit stays on show for the next player.

| Suit | How it is worn | Armour | Flight | Its moves |
|---|---|---|---|---|
| **Mark 1** (the cave, *Iron Man*) | on the chest; an Arc Reactor in your bag | 18, toughness 3; 20% slower | no: one rocket boost, then a crash | flamethrower, rocket boost, arm rocket, brace, heavy punch |
| **Mark 3** (red and gold, *Iron Man*) | on the chest; an Arc Reactor in your bag | 16, toughness 2 | yes | repulsor, double blast, Unibeam, flares, shoulder missiles |
| **Mark 5** (the suitcase, *Iron Man 2*) | use the suitcase anywhere: it unfolds onto you; an Arc Reactor in your bag | 12, toughness 1; 5% faster | yes (the slowest) | repulsor, Unibeam, dash punch, laser burst |
| **Mark 50** (nanotech, *Infinity War*) | the Nano Arc Reactor on the chest, the Mark 50 in your bag; **H** | 20, toughness 8; 5% faster | yes | nano repulsor, nano blade, Unibeam, nano shield, nano cannon |
| **Mark 85** (nanotech, *Endgame*) | the Nano Arc Reactor on the chest, the Mark 85 in your bag; **H** | 20, toughness 12 (as strong as netherite); 10% faster | yes (the fastest) | boosted repulsors, energy blades, giant chest laser, giant energy shield, Lightning Refocuser, homing missiles, nanotech drones |
| **Hulkbuster** (Mark 44, *Age of Ultron*) | use it under the open sky: **Veronica** drops it; an Arc Reactor in your bag | 20, toughness 12; can't be pushed; **explosions can't hurt you**; 3.5 times your size | yes, slowly | rapid punches, heavy Unibeam, grab and throw, Veronica's new arm |

* **Only Iron Man can wear a suit**: anyone else hears "Only Iron Man can wear this suit. Use an Arc Reactor first." (Creative players may, for building.)
* **A suit works** while you are Iron Man, it is on, its reactor is with you (any reactor anywhere in your bag for the classic suits; the Nano Arc Reactor on your chest for the nanotech ones) and it is not broken. Without a reactor its eyes go dark: "No reactor!". Its armour still protects you.
* **Wear and repair.** Hits wear the suit as armour wears, but a suit never breaks: at its last point it goes **offline** ("Suit offline! Repair it."): the armour still protects, but no moves and no flight. Repair it on an **anvil**: iron ingots for the Mark 1, 3 and 5, diamonds for the Mark 50, netherite scrap for the Mark 85, iron blocks for the Hulkbuster. The nanotech suits also **rebuild** by themselves, a point every 2 seconds while they are stowed, and the Hulkbuster's Veronica brings a new arm (below).
* **Hard landings** never hurt Iron Man in a working suit: the suit takes them (5 points).
* Suits, housings and reactors are items: they drop when you die, under the usual rules.

## H: suiting up

For Iron Man the Change Look key, **H**, is the suit key:

| You are… | **H** | **Sneak + H** |
|---|---|---|
| without a suit, the Nano Arc Reactor not on your chest | **suit up** from your bag: the suit you last wore (if you carry it), else the Mark 5's suitcase, else the Mark 3, else the Mark 1 | choose which carried suit **H** puts on next ("Next suit: Iron Man Mark 3") |
| in the Mark 1, the Mark 3 or the Hulkbuster | **take it off** (it goes into your bag) | |
| in the Mark 5 | **fold it back** into the suitcase, in your hand | |
| with the Nano Arc Reactor on your chest | **deploy** the chosen nanotech suit, or **retract** it | **switch** between the Mark 50 and the Mark 85 (if one is out, it changes at once) |
| in the middle of a suit-up | **skip it** to the end | |

You can also **right-click with a classic suit** (or the suitcase) to put it on, or put it on your chest slot by hand. The **Hulkbuster** is put on only by using it (or by hand): H never picks it from your bag.

### The suit-up

Every suit goes on with its **own animation**, which everyone sees, in third person and in first person:

* **Mark 1:** the crude plates **clank on one by one**: boots, shins, thighs, body, arms, gloves, and the **helmet last**, each with a jolt, sparks at the joints and steam from the back. 3 seconds.
* **Mark 3:** near a Suit Display (within 4 blocks), the workshop **gantry's robot arms** reach down and bolt the pieces on; anywhere else the plates **slide in** from a ring round you. The faceplate swings shut and the eyes light up. 2.5 seconds.
* **Mark 5:** the **suitcase** at your feet **snaps open**, its panels unfold up your legs and arms, wrap your body, the helmet folds over from behind and the **faceplate slams shut** (*Iron Man 2*). 2 seconds.
* **Mark 50 and Mark 85:** **nanites flow out of the Nano Arc Reactor** on your chest and spread over your body **in a wave**, glittering at the edge, and the **helmet forms last**, closing from the back (*Infinity War*, *Endgame*). 1.5 seconds and 1.2 seconds. You can walk slowly while they flow.
* **Hulkbuster:** **Veronica drops the frame** from the sky with burning thrusters, it lands with a boom and a shake, **opens**, you are drawn in, it **closes** round you and you **grow** to 3.5 times your size (*Age of Ultron*). 4 seconds.

In first person a faceplate closes over the edges of your screen and the helmet boots: "SYSTEMS ONLINE".

**Taking a suit off** plays the same thing backwards, in half the time: the nanites flow back into the reactor, the suitcase folds back into your hand, the Hulkbuster shrinks, opens, you step out and the frame flies away.

**The rules of a suit-up**, so it can't be used as a trick:

* While a suit goes on you stand still (the nanotech suits: half speed), use no item, no move and don't fly. **Hits on you are halved** while a suit goes on (`iron_man.suitUpDamageFactor`), never more; while one comes off, no protection.
* **H** during a suit-up skips it to the end, and the protection ends at once.
* After a suit-up or a take-off, wait **2 seconds** before the next ("The suit is still settling.").
* If you die, lose the power, log out or change worlds in the middle, the suit simply ends up on (or off), with no animation.

## Suit energy

One **blue bar** (0 to 100) for all your suits (it is the reactor's charge, so it stays the same when you change suits). **Flying** and **every move** use it; each suit's costs are on its table below and in [Server Configuration](Server-Configuration#iron-man-and-his-suits).

* It **recharges** by itself a second after your last move: **3 a second on the ground**, 1 a second in the air when you are not flying.
* The bar turns amber below 25 and blinks red below 10.
* **Empty** ("Suit energy empty!"): moves are refused, and if you were flying, flight ends with **Slow Falling for 5 seconds**, so you land softly, never fall to your death. Then it recharges as usual.

## Flight

Press **G** (or double-tap Space) in a suit that flies: the Mark 3, 5, 50, 85 and the Hulkbuster. You hover upright with your palms down, and fly fast lying flat with your arms along your body, with blue-white flames from your palms and boots that everyone sees, and the soft roar of the thrusters.

| Suit | Speed | Energy a second |
|---|---|---|
| Mark 3 | 0.10 | 0.6 |
| Mark 5 | 0.08 | 0.5 |
| Mark 50 | 0.11 | 0.4 |
| Mark 85 | 0.12 (the fastest) | 0.35 |
| Hulkbuster | 0.06 (heavy and upright) | 1 |

A suit **won't fly** when: it is the **Mark 1** (its G is the Rocket Boost); it doesn't work (no reactor, broken, or a nanotech suit without the Nano Arc Reactor on your chest); a suit-up is playing; the **energy is empty**; or you are bracing (Mark 1), holding a creature (Hulkbuster) or charging the Nano Cannon (Mark 50).

## The moves of each suit

The damage of every move goes only to creatures it may hurt (with PvP off, never to players). Small booms **never break blocks** (unless a server allows it: `iron_man.boomsBreakBlocks` with `mobGriefing`).

### Mark 1: the cave suit

Crude grey iron with rivets and rust, a slit visor, the reactor glowing through a grille, huge forearms with flamethrower nozzles. **Very tough and slow** (20% slower).

| Key | Move |
|---|---|
| **R** (hold) | **Flamethrower**: fire pours from both forearms in a cone 5 blocks long: **2** every half second to every creature in it, set **burning for 4 seconds**. Up to 3 seconds, 4 energy a second. Blocks catch fire only if a server allows it (`mark_1.mark1FlameSetsBlocks` with `mobGriefing`). Cooldown 2 seconds. |
| **G** | **Rocket Boost**: the boots fire: half a second straight up, then a second forward where you look; then they sputter out and you **fall**. The **crash landing** doesn't hurt you (the suit takes 30 points), and throws up a ring of dust: **3** to creatures within 2 blocks. 30 energy. Cooldown 10 seconds. |
| **V** | **Arm Rocket**: one rocket from the left forearm: **8** and a small boom. 15 energy. Cooldown 4 seconds. |
| **B** (hold) | **Brace**: you plant your feet and cross your arms: hits on you are **60% weaker**, nothing pushes you, but you can't move. Up to 5 seconds, 2 energy a second. |
| **N** | **Heavy Punch**: **10** to the creature in front, with a big knockback. Cooldown 2 seconds. |

### Mark 3: the classic

Red with a gold faceplate, gold arms and shins, the round reactor, missile pods on the shoulders.

| Key | Move |
|---|---|
| **R** | **Repulsor**: a blast from your palm that hits **at once** what you look at (up to 32 blocks): **6** and a small boom (2 to creatures right round it). 4 energy; one every half second. **Sneak + R: Double-Hand Blast**: both palms: **10** and a bigger boom. 8 energy. Cooldown 2 seconds. |
| **V** (hold) | **Unibeam**: a beam from the chest reactor, 24 blocks: **4** every half second to the first creature in its way. Up to 3 seconds, 6 energy a second. |
| **B** | **Flares**: a fan of bright flares from your hips: **every monster within 16 blocks that is after you loses you** and can't target you for 5 seconds, and homing missiles chase the flares instead of you. 10 energy. Cooldown 15 seconds. |
| **N** | **Shoulder Mini-Missiles**: the pods open and **4 missiles** fly **straight** where you look: **3** and a small boom each. 12 energy. Cooldown 5 seconds. |

### Mark 5: the suitcase suit

Silver with a red helmet, collar and shoulders: light and quick (5% faster). In your hand it is a **silver suitcase**; **use it anywhere** and it unfolds onto you; **H** folds it back into your hand.

| Key | Move |
|---|---|
| **R** | **Repulsor**: **5** and a small boom. 3 energy. |
| **V** (hold) | **Unibeam**: **3** every half second. 5 energy a second. |
| **B** | **Dash Punch**: a thruster dash **6 blocks** forward: the first creature in the way takes **8** and is thrown. 10 energy. Cooldown 3 seconds. |
| **N** | **Laser Burst**: a thin red-white laser sweeps once round you at chest height, **6 blocks** out: **6** to every creature it may hurt. 20 energy. Cooldown 10 seconds. |

### Mark 50: the first nanotech suit

Sleek, segmented crimson and gold with the rounded-triangle reactor. It lives in its **housing** (a hexagonal canister in your bag) and flows out of the Nano Arc Reactor with **H**.

| Key | Move |
|---|---|
| **R** | **Nano Repulsor**: **7** and a small boom. 4 energy; one every half second. **Sneak + R: Nano Blade**: a blade forms on your right forearm for **5 seconds**: your empty hand hits for **9** and sweeps. 8 energy. Cooldown 8 seconds ("The nano blade is re-forming: 3 s more."). |
| **V** (hold) | **Unibeam**: **5** every half second. 6 energy a second. |
| **B** (hold) | **Nano Shield**: a round nanite shield on your left arm: from the front, **hits are 80% weaker** and arrows and shots bounce off. Up to 8 seconds; 3 energy a second, and 1 more for every 2 health it stops. |
| **N** (hold, let go) | **Nano Cannon**: both forearms become a cannon. Hold **N** for a second to charge it, let go to fire: a blast of **14** at the first creature or block within 32, and a boom. Let go too early and it doesn't fire (half the energy comes back). 25 energy. Cooldown 8 seconds. |

### Mark 85: the strongest suit

Deep metallic red with a **gold faceplate** and gold shoulders, the rounded-triangle reactor glowing white-blue, blue lights at the shoulders and forearms, and four panels on the back that fan out in flight. **As strong as netherite armour**, and the fastest flier.

| Key | Plain | Sneak + key |
|---|---|---|
| **R** | **Boosted Repulsors**: two blasts, one from each palm, **9** each with a small boom. 6 energy. | **Energy Blades** (on/off): long glowing blue blades **come out of your hands**. While they are out, **R** swings them: **10**, **10**, then **14** that sweeps three creatures; your empty hand hits for **12**. 1 energy a second; they go when the energy runs out. |
| **G** | flight (the fastest) | |
| **V** (hold) | **Giant Chest Laser**: a wide beam from the reactor, **32 blocks**, through **every** creature along it: **7** each every half second. Up to 4 seconds, 8 energy a second. | |
| **B** (hold) | **Giant Energy Shield**: the big curved hexagonal shield of *Endgame*, 4 blocks wide and 3 high, in front of you: it stops **everything from the front**, explosions and beams too, and shots bounce off. Up to 10 seconds; 4 energy a second, and 1 more for every 2 health it stops. | **Lightning Refocuser**: chain lightning from your raised gauntlet: **10** to the creature you aim at (within 24), then it **jumps** to up to 4 more, each within 6 blocks of the last (**8, 6, 5, 4**). 25 energy. Cooldown 12 seconds. **Charged**: if lightning struck you in the last 10 seconds (a storm's, [Thor's](Thor) or [Love's](Love)), it is free and **1.5 times stronger** ("Refocuser charged!"), as in *Endgame*. |
| **N** | **Homing Mini-Missiles**: **6** tiny missiles fan out from your shoulders and **lock on** to up to 6 different foes within 24 blocks (brackets on your screen show the locks); they **chase their targets for 10 seconds** and make a **small boom**: **4** each. If a target dies, the missile finds the next. With no target, they fly straight and pop. 15 energy. Cooldown 6 seconds. | **Nanotech Drones**: **2** small red-and-gold drones form behind your shoulders and fly with you for **20 seconds**: every three quarters of a second each fires a **strong laser** (**6**) at the nearest monster, or whoever attacks you, within 20 blocks (never a pet, never a player with PvP off). 30 energy. Cooldown 45 seconds ("The drones are rebuilding: 30 s more."). |

### The Hulkbuster

Massive red-and-gold armour, a small helmet sunk between huge shoulders, giant fists. **Use it under the open sky** and **Veronica** drops the frame for you; you grow to **3.5 times your size**, can't be pushed, **explosions can't hurt you** (your own booms neither), and you reach further and step over blocks. You can't ride or sleep in it.

* **Indoors** Veronica can't reach you ("Veronica can't reach you in here."): put the Hulkbuster **on your chest slot by hand** instead, and the frame folds round you in 2 seconds.
* It needs **room**: if there is none, or the room runs out while you grow, "No room for the Hulkbuster here." and you step out.

| Key | Move |
|---|---|
| **R** | **Rapid Punch Combo**: five punches at the creature in front: **6** each, and the last **10** with a huge knockback. Cooldown 3 seconds. |
| **G** | slow, heavy flight |
| **V** (hold) | **Heavy Unibeam**: a wide beam, 32 blocks: **8** every half second. 10 energy a second. |
| **B** | **Grab & Throw**: grab the creature in front (not a boss, not one wider than 3 blocks; a player only with PvP on) and hold it in your fist up to 3 seconds; **B** again (or the time) **throws** it 12 blocks: **10** when it lands, to it and to what it lands on. Cooldown 8 seconds. |
| **N** | **Veronica**: under the open sky, a **new arm** drops from orbit and clamps on: the suit is **repaired by half**. Cooldown 1 minute. |

## The HUD

While suited, a panel on the left: the suit's name, the **energy bar**, the suit's durability ("Suit 84%"), the moves' cooldowns, "Drones: 12 s", "Refocuser charged!", and the warnings "No reactor!" and "Suit offline!". Unsuited: "Tony · no suit (H: suit up)" and which suit **H** would put on.

In first person a thin **helmet frame** (blue arcs at the edges of the screen, with your altitude and speed while you fly). The client option `ironHelmetHud` turns it off, and `suitUpOverlay` the faceplate of the suit-up (with *Reduced motion* it is a short fade). See [Server Configuration](Server-Configuration#client-options-animations-effects-and-looks).

## Looks

Iron Man's look is his **suit**. Under it, Tony wears plain dark clothes (`tony`), or your own skin (`own`); choose with `/enhanced power set <player> iron_man own` or in Choose a Hero. **H** is his suit key, not a look key.

## Against the others

* **[Magneto](Magneto)** can lift Iron Man in a working **classic** suit (Mark 1, 3, 5, the Hulkbuster): it is iron. The nanotech suits are not.
* Lightning on a Mark 85 **charges the Lightning Refocuser**.
* Red Hulk's **Energy Absorb** soaks up repulsors and beams.
* Nothing of his keeps [Wolverine](Wolverine) or [Deadpool](Deadpool) down.

## Advancements

**Suit up** (put on one of Iron Man's suits) and **Mark 85** (get the Mark 85), in the **Avengers** tab.

## Iron Man in the Hero Creator

A custom hero wears no suit item: his Iron Man cards are the **Mark 85's** set. `iron_man.repulsor` (boosted repulsors; Sneak: the energy blades), `iron_man.unibeam` (the giant chest laser), `iron_man.shield` (the energy shield; Sneak: the Lightning Refocuser), `iron_man.missiles` (homing missiles; Sneak: the drones), and `iron_man.suit`, his **Suit**: +8 armour, +4 toughness and the suit energy meter that powers the moves. The three Sneak variants are cards of their own (`iron_man.energy_blades`, `iron_man.lightning_refocuser`, `iron_man.drones`). The other suits' moves (the flamethrower, the flares, the dash punch, the nano cannon, Veronica...) are only the suits'. He flies with the **Flight** power, and *Iron Man (the thrusters)* is a flight style; *Iron Man* is a run style. See [Heroes' Abilities](Hero-Abilities#iron-man-5). The ready-made hero is *Iron Man-like*.

## Numbers you can change

The power, the reactors, the energy and the suit-ups are `iron_man.*`; each suit has its own section: `mark_1.*`, `mark_3.*`, `mark_5.*`, `mark_50.*`, `mark_85.*`, `hulkbuster.*` (armour, durability, flight, every move). The displays are `avengers_tower.*`. See [Server Configuration](Server-Configuration#iron-man-and-his-suits).

## Related

[Structures: Avengers Tower](Structures#avengers-tower) · [Captain America](Captain-America) · [Falcon](Falcon) · [Red Hulk](Red-Hulk) · [Controls](Controls) · [Items and Crafting](Items-and-Crafting#the-avengers-gear-38)

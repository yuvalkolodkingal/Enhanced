# Wolverine

Logan (3.7), the mutant whose **healing factor** let the Weapon X program bond **adamantium** to his bones and his claws. Three unbreakable blades come out of each fist with a *snikt*. He slashes, spins, **leaps onto his prey and stabs**, flies into a **berserker rage**, smells every creature near him through the walls, and follows one scent across the land. He is heavy as metal: he sinks in water, and Magneto can lift him. And he **won't stay down**: only the Infinity Stones, chaos magic, the Allfather's power, the Phoenix or poison can defeat him.

Hebrew: [עברית](Wolverine-HE)

## The quick version

* **How:** in the [X-Men lower lab](Mutant-School-Lab) under every Mutant School, take an **Adamantium Injection** from the **Weapon X rack**, and with **no power** hold right-click on it for 3 seconds.
* **R** Claw Slash, **G** Claws, **V** Pounce, **B** Berserker Rage, **N** Sniff. **Sneak** changes each of them: Spin Slash, Claw Climb, Feral Leap, Healing Burst, Track.
* **The [healing factor](Healing-Factor):** he heals all the time, and a blow that would kill him only knocks him down for 5 seconds.
* He doesn't fly: he leaps, climbs and runs (20% faster than anyone).

## Becoming Wolverine: the Adamantium Injection

1. Find a **Mutant School** (`/locate structure enhanced:mutant_school`) and go down to its **lower lab**: the two bookshelves north of the fireplace in the Professor's study open, and the lift is behind them. See [The X-Men lower lab](Mutant-School-Lab).
2. In the **Weapon X lab** on the lower level, right-click the **Weapon X rack**: it gives you an **Adamantium Injection**. Each rack gives **one to each player**, so two brothers each get one ("You already took an injection from this rack.").
3. With **no power**, **hold right-click** with the injection for **3 seconds**: silver motes flow into your arm. At the end the injection is used up (not in Creative), the screen flashes silver, a deep *snikt*, and you are **Wolverine** ("Weapon X"). Your claws come out for 2 seconds. Everyone is told: "Eilon took the Adamantium Injection and became Wolverine!" The advancement **Snikt!** is for finding the injection.

Letting go early, swapping slots or dying changes nothing. A **hero** is refused, and nothing is used up: "The adamantium needs a body that can heal. Take the Mutant Cure first." The **Cure Station** in the lab's medical bay gives you one (bring a glass bottle). A Deadpool hears "Your healing factor is taken. Also: rude." A server can let the injection replace any power instead (`adamantium_injection.heroesRefused`). The injection only ever goes into yourself.

The injection has **no recipe** unless a server switches one on (`adamantium_injection.craftable`, for worlds without schools):

```
I N I
N E N
I N I

I = Block of Iron
N = Netherite Ingot
E = Echo Shard
```

You also become Wolverine with **Choose a Hero** in the [Hero Creator](Hero-Creator) (no item needed), or with `/enhanced power set <player> wolverine` (and a look: `classic`, `x_men_2000` or `own`).

**Losing it:** the Mutant Cure, `/enhanced power set <player> none`, being sacrificed at Vormir, or choosing another hero. There is nothing to give back: the adamantium goes with the power.

## The body

* **30 health** (15 hearts), **+4 armour** and **+2 toughness** (adamantium bones), knockback resistance 0.4.
* **20% faster**, every blow **+2** stronger and faster to swing, a little higher jump, and 4 more blocks of fall before it hurts.
* **The [healing factor](Healing-Factor)**: 1 health a second, 3 out of a fight, bad effects last half as long, and a deadly blow only knocks him down for 5 seconds.
* **Heavy as metal.** In water and lava he **sinks** and walks along the bottom: he can't swim up faster than a crawl, and can't sprint-swim. A boat carries him, and a bubble column lifts him. If he drowns he goes down and his healing factor drags him to the shore.
* **Magnetic.** [Magneto](Magneto)'s telekinesis and Magnetic Pull take hold of him by his adamantium (X-Men, 2000): he can be lifted, held and thrown (a player only with PvP on, as always).
* His mind is open: [Professor X](Professor-X) hears him and can take over his body.

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Claw Slash | The claws pop out if they are in, and he slashes the nearest creature in front of him (a 60° cone, 3.5 blocks): **6**, **6**, then a cross-slash of **9** that hits up to three creatures in front and knocks them back. Half a second between slashes; after a second and a half without one, the combo starts again. **Sneak + R: Spin Slash**: a full turn with both fists out, **7** to everyone within 3 blocks he may hurt, and arrows flying at him are knocked down. Cooldown 4 seconds. |
| 2 | **G** | Claws (on/off) | *Snikt*: three adamantium blades from each fist; *shink*: back in. Out, his **empty hand** hits for **7** (1 + 2 + the claws' 4), and a full swing on the ground also grazes creatures right next to the one he hits. He still uses items normally (an item in his hand hides that hand's claws). **Sneak + G: Claw Climb** (on/off, needs the claws out): pressed against a wall he climbs it by his claws, scraping marks into the stone; hold **Sneak** to hang still. Not on glass, panes, ice, slime or honey. No fall damage while he clings. |
| 3 | **V** | Pounce | He crouches, then **leaps onto the creature he looks at** (up to **16 blocks**, in sight) and stabs it: **12** damage, and it is **pinned** for a second (it can't run or jump). A boss only takes the damage. He lands on his feet beside it. With no prey in sight, a plain leap to where he looks. No fall damage for his leaps. Cooldown 5 seconds. **Sneak + V: Feral Leap**: a long, low running leap, about 10 blocks ahead. Cooldown 1.5 seconds. |
| 4 | **B** | Berserker Rage | **10 seconds** of rage: **+40% damage**, **25% faster**, harder to push, **healing twice as fast**, the claws out (G can't put them away) and slashes faster. But he is **too angry to use items** ("Too angry to eat!"). His screen has a red edge (it pulses, unless *Reduced motion*), and he growls. After it he is **tired**: Weakness I for 5 seconds. Cooldown 45 seconds. **Sneak + B: Healing Burst**: [half his health back at once](Healing-Factor#the-healing-burst), the bad effects gone (not Poison). The Rage and the Burst share **B**'s cooldown. |
| 5 | **N** | Sniff | He lifts his head and sniffs: every creature within **48 blocks** **glows through the walls** for 10 seconds, in its scent's colour: monsters **red**, animals **green**, villagers, Asgardians and the cottage's family **yellow**, players **white** (or their hero's colour), anyone poisoned **purple**. The **scent trails** of the players and monsters near him (where they walked in the last minute) drift as faint motes, for him alone. Cooldown 15 seconds. **Sneak + N: Track** (on/off): he takes the scent of the creature or player he looks at (up to 64 blocks), or of the last one that hurt him: for **60 seconds** a trail of motes leads from him to it, through walls, it glows gold, and the HUD shows an arrow and the distance ("↗ 34 m"). A tracked player is told "Wolverine has your scent." It ends when it dies, leaves the dimension or goes more than 256 blocks away ("The scent is lost."), when the minute is up, or with Sneak + N again. Cooldown 15 seconds after it ends. |

With PvP off, his abilities don't hurt players. Everything he pushes at the edge of the [Vormir Cliff](Vormir) counts as his push.

## Looks

| Look | Id | What it is |
|---|---|---|
| **Classic suit** (default) | `classic` | The yellow-and-blue suit of *Deadpool & Wolverine* (2024), the comics' colours on screen at last: yellow, with blue shoulders and sides striped in black, long blue gloves, blue boots, a black belt with a round red-and-silver buckle, and the yellow cowl with its two black fins. |
| **X-Men (2000) leather** | `x_men_2000` | The black quilted leather of *X-Men* (2000), with an orange-yellow piping V from the shoulders to the belt and down the arms and legs; no mask: his hair swept up into two points, long sideburns. |
| **Your own skin** | `own` | His powers in your own skin. |

Press **H** to switch. See [Looks](Looks). The claws are drawn in first and third person, sliding out over a moment.

## The HUD

Next to the usual panel: the [healing factor](Healing-Factor#the-hud)'s line, the combo ("Combo 2/3"), the claws ("Claws out · climbing" or "Claws in"), the Rage's red bar ("RAGE ▌▌▌▌ 7s") and Track's arrow and distance.

## Against the others

* **What defeats him:** the Infinity Stones, chaos magic (the Scarlet Witch, Billy), Odin's power, the Phoenix, the void, `/kill`, and anything at all while he is **poisoned**. See [Healing Factor](Healing-Factor#what-defeats-him).
* **[Deadpool](Deadpool)** has the same healing factor: his katanas and Wolverine's claws can't finish each other. Deadpool has a few things to say about the claws.
* At [Agatha's Cottage](Agathas-Cottage), Agatha asks him to keep his claws off the furniture.

## Wolverine in the Hero Creator

His abilities are in the [Hero Creator](Hero-Creator) (`wolverine.slash`, `wolverine.claws`, `wolverine.pounce`, `wolverine.berserker`, `wolverine.senses`, `wolverine.body`), and his five Sneak variants are cards of their own (`wolverine.spin_slash`, `wolverine.claw_climb`, `wolverine.feral_leap`, `wolverine.healing_burst`, `wolverine.track`). **Wolverine's body** gives a custom hero the healing factor and the adamantium (heavy, magnetic). See [Heroes' Abilities](Hero-Abilities#wolverine-6). The ready-made hero is *Wolverine-like*, and *Wolverine* is a run style.

## Numbers you can change

Every number of his body and abilities is a `wolverine.*` option, the injection's are `adamantium_injection.*`, and the healing factor's are `healing_factor.*`. See [Server Configuration](Server-Configuration#wolverine-and-the-adamantium-injection).

## Related

[Healing Factor](Healing-Factor) · [Deadpool](Deadpool) · [The X-Men lower lab](Mutant-School-Lab) · [Controls](Controls) · [Looks](Looks)

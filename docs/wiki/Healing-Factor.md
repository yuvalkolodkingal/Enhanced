# Healing Factor

**Wolverine** and **Deadpool** (3.7) share one power under their skin: the **healing factor**. Their wounds close as you watch, harmful effects slide off them, and a blow that would kill anyone else only **knocks them down for 5 seconds**: they kneel, lie still, and get back up. Eilon's rule, made exact in the mod: **only the Infinity Stones, chaos magic (the Scarlet Witch, Billy), the Allfather's power (Odin), the Phoenix, or poison** can defeat them.

Hebrew: [עברית](Healing-Factor-HE)

![Wolverine in the classic suit](images/050-wolverine-classic-suit.png)

## The quick version

* **Who has it:** [Wolverine](Wolverine), [Deadpool](Deadpool), and a [custom hero](Hero-Creator) with Wolverine's or Deadpool's body.
* **Healing:** half a heart a second, always; **three times that** after 3 seconds without being hurt.
* **A blow that would kill him** leaves him at 1 health and **DOWN for 5 seconds**: nobody can touch him, and monsters lose interest. Then he **rises** with 8 health (4 hearts).
* **What really defeats him:** the Infinity Stones, chaos magic, Odin's power, the Phoenix, the void and `/kill`. And **poison switches the healing off** while it lasts: then anything can defeat him.
* **Healing Burst** (**Sneak + B**): half his health back at once.

## How fast he heals

![Wolverine's adamantium claws](images/051-wolverine-adamantium-claws.png)

| What | How much |
|---|---|
| Always, even in a fight | **1 health a second** (half a heart) |
| After **3 seconds** without taking damage | **3 health a second** (a heart and a half) |
| In Wolverine's **Berserker Rage** | twice as fast (2 or 6 a second) |
| With an empty food bar | a quarter as fast, never nothing |

Healing makes him a little hungrier (0.05 food exhaustion for each health healed). His ordinary food healing works as for everyone.

**Bad effects slide off him:** Slowness, Weakness, Wither, Nausea, Blindness, Darkness, Hunger and Mining Fatigue last **half as long** on him. Not **Poison** (below), and not the mod's own effects (Spellbound, Webbed, Confused, Frightened, Drained, Time Locked...): those last their full time.

Soft green-white glints rise from a hurt healer while he heals, for everyone to see. Never blood.

## Down, and back on his feet

When damage would bring him to 0 health and it is **not** one of the things that defeat him:

1. The blow is cut short: he is left with **1 health**, and a white-green ring flashes where he was hit (so the attacker sees it did not kill).
2. He is **DOWN** for **5 seconds**: he drops to one knee, then lies on his side. Nothing can hurt him (except the things that defeat him), he can't move, use his keys, use items, hit or open anything, and **monsters forget him** and wander off. His own screen darkens at the edges and a ring over the crosshair counts down: "Healing… 3".
3. He **rises** with **8 health** and **3 seconds of Resistance I**, shakes his head, and the action bar says "Healing factor: back on your feet!" Wolverine growls with his claws out; Deadpool cracks a joke.

There is no limit on how many times. The first time, the chat explains it: "Your healing factor brought you back! Only the Infinity Stones, chaos magic (the Scarlet Witch, Billy), the Allfather's power, the Phoenix or poison can defeat you." (The list follows the server's options.) His first rise also gives the advancement **Back on Your Feet**.

* **A totem of undying is never used up** by a blow he survives: the would-be death never happens.
* **Logged out while down?** He logs back in already risen.
* **No soft-locks:** if he is down **under water without air, in lava or fire, in powder snow, inside a wall, or under the bottom of the world**, he is moved when he rises to the **nearest dry, safe spot** within 32 blocks, the fire put out and his air back ("Your healing factor dragged you to safe ground."). If there is none, to his bed or the world's spawn. Inside a boss arena he is only moved inside the arena.

Starving and drowning never finish him either: he goes down and rises (and is dragged out of the water).

## What defeats him

These go straight through the healing factor. Each one can be switched off by a server.

| What | Which attacks | Option |
|---|---|---|
| **The Infinity Stones** | every power of the [Infinity Stones](Infinity-Stones) (the Mind Stone in Loki's Scepter and in Vision's forehead too, Thanos the hero's and the boss's stones, the Snap) | `healing_factor.stonesDefeat` |
| **Chaos magic** | the [Scarlet Witch](Scarlet-Witch)'s Chaos Beam and Hex wall, [Billy](Billy-and-the-Witches-Road)'s Hex Bolt and Hex Beam | `healing_factor.chaosDefeats` |
| **The Allfather's power** | [Odin](Odin)'s Odinforce and Gungnir | `healing_factor.odinDefeats` |
| **The Phoenix** | the [Phoenix](Phoenix) Burst | `healing_factor.phoenixDefeats` |
| **The Soul Stone's price** | being pushed off the [Vormir Cliff](Vormir) in a sacrifice | `healing_factor.vormirDefeats` |
| **The void and `/kill`** | always | — |
| **The Darkhold** (Agatha) | **off** by default: Agatha's dark magic lost to chaos magic in *WandaVision* | `healing_factor.darkholdDefeats` |

**Not** on the list: monsters, players' swords and arrows, falls, fire, lava, explosions, Thor's lightning and Mjolnir, the Hulk, Hela's blades, Hydra's blasters, the bosses' own attacks (only Thanos fights with stones). And **Wolverine's claws and Deadpool's katanas can't finish each other**: two healers can fight all day ("We could do this all day. And we will.").

When one of them gets through, everyone near hears it in gold, before the usual death message: "Eilon's healing factor could not stop chaos magic."

## Poison switches it off

**Poison stops the healing** (*The Wolverine*, 2013: Viper's poison). While he is poisoned:

* no healing, no shrugging, no Healing Burst ("The poison stops your healing!"),
* and **no protection**: a zombie, a fall or an arrow can defeat him.
* Poison itself never kills (it stops at half a heart, as for everyone).
* **Milk**, or the end of the Poison, turns it back on at once: "Healing factor: back ON!"

The HUD's healing icon turns **red and crossed out**: "Healing factor OFF — poisoned!", with a hiss and a sour tone; a bright chime when it comes back. Poison comes from Poison potions (also splash and lingering), arrows of Poison, cave spiders, bees, pufferfish, witches' potions and suspicious stew. So the way for a friend (with PvP on) to beat a Wolverine is a **splash potion of Poison**, then a sword.

## The Healing Burst

![Wolverine's berserker rage](images/052-wolverine-berserker-rage.png)

**Sneak + B** for both heroes (Wolverine's **Healing Burst**, Deadpool's **Chimichanga Break**): **half his health back at once** and the bad effects gone (not Poison). A soft green-white shimmer. Cooldown 20 seconds. Refused while poisoned, and then nothing is spent. Deadpool with a [chimichanga](Deadpool#the-chimichanga) in his bag eats it: **all his health** back and Absorption II for 30 seconds.

## The HUD

Under the powers panel, a line for every healer: a green "+" in a ring and **"Healing factor: ON (+1.0/s)"** (+3.0/s out of a fight). Poisoned: the red crossed icon, "OFF — poisoned!". Down: "Healing… 3", the screen's edges dark, and the ring over the crosshair.

## With the rest of the mod

* **[Vormir](Vormir):** a healer **pushed** off the cliff by a seeker (with PvP on) and given to the fall **dies**: the Soul Stone's own magic. One who **jumps or falls alone** goes down on the pit's floor and rises; the tunnel leads out.
* **The [Witches' Road](Witches-Road):** the trials put a fallen player back by their own rule, first.
* **Boss fights:** a healer who is down counts as still in the fight, so the fight never ends as "everyone fell" while he lies there.
* **The [Mutant Cure](Items-and-Crafting):** takes his power, healing factor and adamantium and all, as for every hero.
* **Agatha's Power Drain** does nothing to him (it takes only sorcerers' powers).
* **Mind powers** work on him: his mind is open to Professor X, the Mind Stone and the Scepter.
* **Death:** while a blow only knocks him down, he never dies, so he never [meets Death](Meeting-Death).
* **PvP off:** players can't hurt each other at all, as always.

## For a custom hero

A [custom hero](Hero-Creator) with **Wolverine's body** (`wolverine.body`) or **Deadpool's body** (`deadpool.body`) has the healing factor too (unless `healing_factor.customHeroes` is off). The two Healing Bursts (`wolverine.healing_burst`, `deadpool.chimichanga`) also work on a hero without it: a plain heal. See [Heroes' Abilities](Hero-Abilities#wolverine-6).

## Numbers you can change

Section `[healing_factor]`: whether it works at all, for custom heroes, the healing rates, the hunger, the shrug, how long he stays down, the health and Resistance he rises with, whether monsters forget him, the rescue's reach, each thing that defeats him, poison, the Burst, and whether the tip shows only once. The tags `enhanced:defeats_healing/stones`, `chaos`, `odin`, `phoenix` and `darkhold` (damage types) and `enhanced:stops_healing` (effects) are data, so a data pack can change them. See [Server Configuration](Server-Configuration#the-healing-factor).

## Related

[Wolverine](Wolverine) · [Deadpool](Deadpool) · [FAQ](FAQ#why-wont-wolverine-die) · [Infinity Stones](Infinity-Stones) · [Vormir](Vormir)

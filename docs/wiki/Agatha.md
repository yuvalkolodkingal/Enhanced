# Agatha

Agatha Harkness, the witch of *WandaVision* and *Agatha All Along*: "it was Agatha all along". In the mod she is the hero the **Darkhold** makes. A player with no power who reads the book becomes her: the book opens all its dark magic to her, and she adds flight, a stronger body and the tricks of the shows: she **drains the power out of another hero**, raises a **purple witch's shield** and lays a **circle of runes** that binds whatever stands in it. Her magic is violet.

Hebrew: [עברית](Agatha-HE)

## Becoming Agatha

1. Get the **Darkhold**. It is crafted (netherite scrap, a wither rose, echo shards, a book and a wither skeleton skull): the recipe is on [Items and Crafting](Items-and-Crafting#darkhold) and [The Sorcerers' Books](Sorcerers-Books#the-darkhold).
2. With **no power**, hold it and **hold right-click (use) for 3 seconds**. Violet motes spiral into you and the book's whispers rise. Let go early and nothing happens: the whispers fade with a sigh.
3. At the end the Darkhold opens to you: you awaken as **Agatha** ("The Darkhold opens to you"), and everyone on the server is told "it was Agatha Harkness all along!". **The book is kept**: you can still read it like any sorcerer.

A hero who is not a sorcerer is refused by the book, as before ("The Darkhold whispers, but only a sorcerer can read it"). A sorcerer (Loki, the Scarlet Witch, Billy, or Agatha herself) uses the book as in 3.2. The book is silent on the [Witches' Road](Witches-Road), so nobody becomes Agatha there. A server can turn the awakening off (`agatha.darkholdAwakens`): the book then only refuses a reader with no power, as in 3.2.

## Her body

* **Max health 40** (20 hearts): the Darkhold gives her 20 more than a normal player (`agatha.bodyHealthBonus`).
* **She flies** (**G**, or double-tap **Space**), at 0.085.
* **Magic used on her poisons whoever used it**, with or without the Darkhold in her hand: the revenge of the book is part of her (Poison I for 10 seconds, the [Darkhold's own numbers](Sorcerers-Books#the-revenge-poison)). Spells that lay Confusion, Spellbound or Drained on her answer too.
* Her eyes glow violet while her beam burns, her shield is up or she casts, and for a moment after a drain or the runes.

## Abilities

None of them needs the book in her hand: the Darkhold opened her powers to her.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (hold) | Purple Beam (Sneak: Hex Mark) | The Darkhold's **purple beam with a white core**, from her hand along her look, for as long as she holds R: **8 damage a second** (four hits a second), **24 blocks**, 1 exhaustion a second. It needs aim: the first creature on the line takes it, and a block stops it. **Sneak + R** casts the book's **hex mark** on the creature she looks at (it glows, is slowed and takes half as much damage again from her for 10 seconds), or, looking at nothing, its **mind glimpse** (every living thing within 24 blocks glows for 5 seconds). |
| 2 | **G** (toggle) | Flight | Calm flight, speed 0.085. You can also double-tap **Space**. |
| 3 | **V** | Power Drain | What she does to Wanda in *WandaVision*: she draws the power out of whoever is under her crosshair, within **16 blocks**. **A player with a power** is **Drained** for **10 seconds**: no ability key works, a flier comes down, and whatever they hold with telekinesis is let go (only where PvP lets her hurt them). **A creature** loses its life instead: **6 damage**. Either way she **heals 2 hearts**. Cooldown 15 seconds. **Since 3.5 it takes only a witch's or a sorcerer's power**: Loki, the Scarlet Witch, Billy, another Agatha, or a custom hero with any of their abilities. Any other hero is refused ("Only a witch's or sorcerer's power can be drained: Peter's power is no magic"); a creature still loses its life. If there is nothing to drain (a player with no power, a hero who is no sorcerer, a player she may not hurt, nothing in sight, a ward), it costs nothing. A server can let her drain every hero again (`agatha.drainOnlySorcerers`). |
| 4 | **B** (hold) | Witch's Shield | A **violet sphere** with a lattice of runes round her (radius 1.9), formed in 8 ticks. It stops blows, projectiles (they burst into violet sparks), beams and explosions, as Apocalypse's shield does, up to its **strength of 40**: each hit it stops takes its damage off, and at 0 it **shatters** and the rest goes through. It can be held for **10 seconds** at most and costs a little food while it stands. It needs 2 seconds to recharge after you let go, 10 after it shattered or ran out. Falling, drowning and the void are not stopped. |
| 5 | **N** | Rune Trap | A **circle of glowing runes** (radius 4) where she looks, up to 24 blocks away (looking at a wall: on the ground at its foot; at nothing: round her own feet). For **5 seconds**, whatever stands in it and her area attacks may hurt (hostile creatures, creatures after her, players with PvP on) is **bound** (Spellbound: it cannot move or attack) and **burns for 2 damage a second**. Bosses are burned but not bound. One trap at a time; cooldown 20 seconds. |

The HUD shows `[R] Purple Beam (Sneak: Hex Mark)` with "active" while it burns, flight on or off, the Drain's cooldown, the shield's state (forming, its strength and seconds left), and `runes 3.2s` while the circle burns.

## Dark magic

Her beam, her Drain and her runes are **dark magic** (the datapack tag `enhanced:dark_magic`, with the Darkhold's beam).

* **The ward of the Book of the Vishanti keeps dark magic out.** Her beam stops at the edge of a ward, and her Drain and her runes do nothing inside a ward she is not in herself ("A ward of the Vishanti keeps your dark magic out"). See [The Sorcerers' Books](Sorcerers-Books#the-ward-what-it-keeps-out).
* **She is a sorcerer**, so the [Hex Runes](Scarlet-Witch#hex-runes) of a Scarlet Witch take her powers away while she is inside that Hex.
* She reads the [Book of the Vishanti](Sorcerers-Books#the-book-of-the-vishanti) too, and her ward reaches **24 blocks** (`vishanti.wardRadiusAgatha`).
* She walks the [Witches' Road](Billy-and-the-Witches-Road) from door to door like Wanda and Billy: she led the coven down the Road.
* She is **not a telepath**: [Professor X](Professor-X) can take over her body (with PvP on), and her mind is open to him.

## Looks

| Look | Id | What it is |
|---|---|---|
| **WandaVision** (default) | `wandavision` | Agatha revealed at the end of *WandaVision*: a deep plum velvet witch's dress with a high black collar and the **gold brooch with an amethyst** at the throat, flared black cuffs, a long skirt, dark brown waves. |
| **Agatha All Along (the Road)** | `all_along` | Her outfit on the Witches' Road: a dark green velvet coat over a black dress, a silver clasp and a black belt, and her **pointed witch's hat** (black felt, a green band with a silver buckle, the tip bent back; a helmet hides it). |
| **Your own skin** | `own` | Agatha's powers in your own skin. |

Press **H** to switch. See [Looks](Looks).

## The Infinity Stones (3.6)

Agatha is a sorceress: she may hold and use the **Time Stone** (Slow Time; Sneak: Rewind; on a crop it grows), and the **Eye of Agamotto** gives her the same powers. The other stones are too strong for her. The **Sling Ring** answers her too. See [Infinity Stones](Infinity-Stones) and [Doctor Strange](Doctor-Strange#the-eye-for-everyone-else).

## Numbers you can change

Everything above is in the `[agatha]` section: the awakening (on or off, how long), her flight and health, the revenge, the beam (damage, range, rate, food), the Drain (reach, how long a hero is drained, damage, healing, cooldown, food, and since 3.5 whether only sorcerers can be drained), the shield (size, wind-up, time, strength, recharges, food) and the Rune Trap (reach, size, time, damage, cooldown, food). The hex mark, the mind glimpse and the poison use the `[darkhold]` numbers. See [Server Configuration](Server-Configuration#agatha-harkness).

## Related

[The Sorcerers' Books](Sorcerers-Books) · [Scarlet Witch](Scarlet-Witch) · [Billy](Billy-and-the-Witches-Road) · [Controls](Controls) · [Becoming a Hero](Becoming-a-Hero)

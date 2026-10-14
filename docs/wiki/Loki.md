# Loki

The God of Mischief, the Loki of the MCU films and series: a master of daggers, illusions and escapes, and, underneath it all, a Frost Giant. In the mod he walks (he does not fly), and his five tricks are the same in every look.

## Becoming Loki: the Tesseract

In *Avengers: Endgame* the 2012 Loki grabs the Tesseract and vanishes in a blue swirl. That escape is how a player becomes Loki.

* **Finding it.** The **Tesseract** is not crafted. It turns up in chests, and the chance is per chest that is generated:

  | Source | Chance |
  |---|---|
  | End City treasure chest | 8% |
  | Stronghold library chest | 6% |
  | Woodland Mansion chest | 5% |
  | Ancient City chest | 4% |

* **Using it.** A player **with no power** who uses it (right-click) is wrapped in a swirl of Space Stone light, vanishes in a blue flash and reappears **100 to 250 blocks away**, in a random direction, on safe ground in the same dimension (solid ground underfoot, room to stand, no liquid, nothing burning, and no Hex wall in between). They awaken as **Loki**. If the mod does not find a safe spot after 12 tries, you stay where you are, but you still become Loki.
* **With a Scarlet Witch Gene.** A player with no power who holds a gene in the other hand awakens as the [Scarlet Witch](Scarlet-Witch) instead of Loki, the same way as with Loki's Scepter: the gene is used up, Wanda's Crown appears, and nobody is taken anywhere. Without the gene the cube makes Loki, as above.
* **The cube is never used up.** After the escape it stays in your inventory.
* **Its sound.** The Space Stone swells with a deep cosmic hum under the sound of the portal, at both ends of the escape and of a jump; and the cube hums softly in anyone's hand.
* **Heroes use it as a jump.** A hero (Loki included) who uses the Tesseract is not changed: the Space Stone takes them to the block they look at, up to 48 blocks away, standing on top of it if there is room. The cube then needs 5 seconds before it can be used again, and a Hex wall they are not allowed through still stops them.

## Loki's Scepter

Anyone can swing **Loki's Scepter** (a relic found in chests: see [Items and Crafting](Items-and-Crafting)): 8 attack damage, and it never breaks. In the films the Mind Stone in it takes over the mind of whoever it touches. That is too strong for the game, so here a melee hit **confuses**:

* **A player** gets **Nausea** for 8 seconds and **Slowness I** for 3. (Magneto's helmet turns the Nausea away.)
* **Any other creature** is **Confused** for 6 seconds. It forgets its target and can't pick a new one, and it stumbles about aimlessly: it heads for random spots nearby, a little faster than it wanders when idle, and changes its mind every few ticks. Small blue-green sparks swirl round its head while it lasts.
* The **Ender Dragon, the Wither and the Warden** are immune (the `enhanced:confused_immune` tag).

Touching the stone itself is another matter: see [Scarlet Witch](Scarlet-Witch).

## His body

* **24 health** (12 hearts): an Asgardian is tougher than a human.
* He does not fly. He vanishes instead (ability 4).

## Looks

Loki has four looks, and **H** switches between them (**Sneak + H** goes back). They change his skin, his cape and the shade of his magic, nothing else: every look has the same five abilities. See [Looks](Looks).

| Look | What it is |
|---|---|
| **God of Mischief** (the default) | The MCU's Loki: the black and green leathers with gold trim, the golden horned helm and a cape. |
| **TVA Variant** | The Loki of the Time Variance Authority, in the beige TVA jacket, with no cape. |
| **God of Stories** | The Loki of the *Loki* season 2 finale: green and gold, with a cape. His magic is olive and gold. |
| **Your own skin** | Loki's powers and magic colours, in your own Minecraft skin. |

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Daggers | Conjures a dagger out of thin air and throws it. **Sneak + R** throws a fan of three. |
| 2 | **G** | Duplicates | Loki turns invisible and three copies of him walk off in different directions. Everything that was hunting him goes after a copy instead. |
| 3 | **V** | Shapeshift | Take the shape of the creature or player you look at. Look at nothing to be yourself again. |
| 4 | **B** | Vanish | Vanish in a green flash and reappear where you look. |
| 5 | **N** (toggle) | Frost Giant | Take your true Jotun form, or become Asgardian again. |

### 1. Daggers

* A conjured dagger is slim, fast and nearly flat in flight, trailing a faint shimmer of his magic.
* **Damage:** 5 per dagger (2.5 hearts), set by `loki.daggerDamage`.
* **Cooldown:** 0.4 seconds for a single dagger, 1.5 seconds for a fan. A fan is three daggers, 10 degrees apart.
* A dagger exists only while it flies: whatever it hits, it vanishes in a spray of green sparks, and it vanishes by itself after 3 seconds of flight. It can never be picked up. It passes through Loki's own duplicates.

### 2. Duplicates

* "Surprise!" Three copies of Loki appear around him, evenly spaced, and each **walks off in its own direction**, up to 12 blocks away. A copy looks exactly like him: his look, and whatever he holds and wears.
* **Loki turns invisible for 5 seconds** (nothing of him is drawn, not even what he holds). Drinking milk, or anything else that ends invisibility, ends the trick early.
* **Decoys.** Every creature within 24 blocks that was after him goes after the nearest copy instead.
* The copies last **8 seconds**. They have 1 health: one hit and a copy dissolves into warm-white streaks, with a flash of Loki's magic where it was touched. A copy deals no damage, drops nothing and gives no experience. Magneto's magnetism finds no real metal in a copy: his strip or pull makes it dissolve, and Tommy's Disarm Blitz takes nothing from it.
* **Cooldown:** 15 seconds.

### 3. Shapeshift

* Look at a creature (a mob or an animal) or another player within **16 blocks** and press **V**: in a flash of green light Loki takes its shape.
  * In a creature's shape, everyone else sees that creature in place of his body (his position, his turn of the head, his walk), with no name tag. Creatures of that kind stop hunting him at once, and never go after him while he looks like one of them.
  * In another player's shape, he wears that player's skin and shows that player's name.
  * In first person, nothing changes for Loki himself.
* Press **V** while looking at nothing, and he is himself again.
* **Beyond his magic:** the Ender Dragon, the Wither, the Warden, the Elder Guardian and his own duplicates cannot be copied. The `enhanced:shapeshift_immune` entity tag lists them.
* No cooldown, no cost. The HUD shows what he looks like now.

### 4. Vanish

* Loki vanishes in a green flash and comes back where he looks, up to **20 blocks** away:
  * **on top of the block** he looks at (or beside it, if there is no room on top);
  * **right behind the creature** he looks at, facing its back;
  * in mid-air where he looks, if there is nothing in reach.
* If there is no room to appear, nothing happens and there is no cooldown.
* A Hex wall he is not allowed through stops him. **Cooldown:** 3 seconds.

### 5. Frost Giant

Loki's true Jotun form, from his birth. Toggle it with **N**.

* His skin turns the blue-grey of a Frost Giant, and snowflakes and a faint icy shimmer drift off him.
* **+4 armor** while in this form.
* A Frost Giant is made of the cold: he **never freezes** (freezing damage and powder snow do nothing to him).
* **Still water freezes over where he walks**, in a circle of 3 blocks round him, like Frost Walker.
* **His blows freeze.** A creature he hits is slowed (Slowness II for 3 seconds) and frosts over, shivering as in powder snow.

Frost Giant is ability 5 in **every** look.

## Numbers you can change

The dagger's damage and how long the Scepter confuses are the Loki options: `loki.daggerDamage`, `loki.scepterConfusionTicks`, `loki.scepterNauseaTicks` and `loki.scepterSlownessTicks` on [Server Configuration](Server-Configuration). The Tesseract's chances are in the loot modifiers (datapacks can change them), listed on [Items and Crafting](Items-and-Crafting).

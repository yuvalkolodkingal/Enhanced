# Enhanced

**Enhanced** is a Minecraft mod (NeoForge, Minecraft 1.21.1) where you can become one of the *Enhanced*: a hero from the Marvel films and shows, with the powers, the look and the colours you know from the screen. You can be **Magneto**, the Master of Magnetism; **Wanda Maximoff, the Scarlet Witch**; **Quicksilver**, who slows the world down around him (to *Sweet Dreams*) or just outruns it; **Vision**, the Mind Stone in a synthetic body (or **White Vision**); Wanda's sons **Tommy (Speed)** and **Billy (Wiccan)**; **Loki**, the God of Mischief; or, if you are worthy, **Thor**, with **Mjolnir** and **Stormbreaker**.

Original hero concepts by Eilon Kolodkin-Gal. This wiki is the player's guide. The full design, with every rule and number, is in [docs/DESIGN.md](https://github.com/yuvalkolodkingal/enhanced/blob/main/docs/DESIGN.md).

Hebrew: [עברית, דף הבית](Home-HE)

| | | |
|---|---|---|
| ![Magneto](images/magneto.png) | ![Scarlet Witch](images/scarlet-witch.png) | ![The Hex](images/hex.png) |
| Magneto | Scarlet Witch | The Hex |
| ![Quicksilver](images/quicksilver.png) | ![Vision](images/vision.png) | ![Quicksilver running](images/quicksilver-run.png) |
| Quicksilver | Vision | Quicksilver Time |

## The heroes

| Hero | Who | How you become one | Known for |
|---|---|---|---|
| [Magneto](Magneto) | The Master of Magnetism | Craft Magneto's Helmet and put it on | Metal telekinesis, iron shrapnel storm, magnetic shield |
| [Scarlet Witch](Scarlet-Witch) | Wanda Maximoff | Touch the Mind Stone in Loki's Scepter (or use the Tesseract) while holding the Scarlet Witch Gene | Chaos beam, telekinesis, the Hex |
| [Quicksilver](Quicksilver) | Peter Maximoff | Craft Quicksilver's Goggles and put them on | Quicksilver Time: the world slows down and plays *Sweet Dreams*; or Super Speed, where only he is fast |
| [Vision](Vision) | The Mind Stone in a synthetic body | Touch the Mind Stone while holding a Synthezoid Core | Mind Stone beam, phasing through walls, density; White Vision look |
| [Tommy](Tommy) | Tommy Maximoff (Speed) | Open the Witches' Road Door with a Scarlet Witch Gene and choose Tommy | Super speed, disarm blitz, whirlwind |
| [Billy](Billy-and-the-Witches-Road) | Billy Maximoff (Wiccan) | Open the Witches' Road Door with a Scarlet Witch Gene and choose Billy | Hex Bolt, flight, freeze, The Noise |
| [Loki](Loki) | The God of Mischief | Use the Tesseract (without a gene in your other hand) | Daggers, duplicates, shapeshifting, vanishing, Frost Giant form |
| [Thor](Thor) | The God of Thunder | Be worthy and lift Mjolnir | Throwing, shattering and pinning with Mjolnir, flight, the lightning beam, the Bifrost |

How each hero is unlocked, step by step, is on [Becoming a Hero](Becoming-a-Hero). Every key is on [Controls](Controls), and the heroes with several looks (Vision, Tommy, Billy, Loki and Thor) are on [Looks](Looks).

## What's new in 2.2

* **Everyone moves like the character on screen.** New poses for every hero, a flight of his own for every flier, every ability acted out (other players see it too), and trails, rings and wind behind flying heroes. See *Animations and effects* below.
* **Thor's lightning beam.** Hold **V** and lightning pours from his hand, or from the head of Mjolnir or Stormbreaker: longer and twice as strong through the weapon, stronger still as the God of Thunder. See [Thor](Thor).
* **Shatter Mjolnir.** **Sneak + R** and Mjolnir bursts into glowing pieces that strike his enemies, then fly back and make it whole in his hand again.
* **Pin someone with Mjolnir.** **Sneak + right-click** a creature or player and Mjolnir pins them down until its owner lifts it.
* **A harder Chaos Shield.** It takes 3/4 of a second to form, holds for 8 seconds, has 30 points of strength, shatters when those run out, and needs 6 to 15 seconds to recharge. See [Scarlet Witch](Scarlet-Witch).
* **The Tesseract awakens Wanda too.** Use it with a Scarlet Witch Gene in your other hand and you become the Scarlet Witch instead of Loki.
* **Loki's Scepter confuses.** A hit makes a creature forget its target and wander about, and gives a player Nausea and Slowness. See [Loki](Loki).
* **Thor's Love and Thunder look, redrawn** to match the film: navy, gold and black armour, long ash-blond hair swept back from a bare forehead, a full beard, a red cape. And a new look, **Love and Thunder, helmet**, with the winged helmet from the New Asgard fight. See [Looks](Looks).

## Animations and effects

Every hero moves like the character on screen: each flier has a flight of his own (Thor even whirls Mjolnir on its strap while he hovers, with the handle really in his fist), every ability is acted out, and your own hands move in first person. Other players see all of it too. Flying leaves trails, rings and wind behind you, and Thor and Vision can break the sound barrier. The client options *Hero animations*, *Reduced motion* and *Flight effects* tone it down or turn it off: they are in `config/enhanced-client.toml` and behind the *Config* button of Enhanced in the Mods screen, see [Server Configuration](Server-Configuration#client-options-animations-and-effects). The details are in [docs/DESIGN.md](https://github.com/yuvalkolodkingal/enhanced/blob/main/docs/DESIGN.md).

## Download and install

Every push to `main` builds the mod, runs the in-game tests and publishes the jar on the [Releases](https://github.com/yuvalkolodkingal/enhanced/releases) page. The newest build is always marked *Latest*.

* **Minecraft** 1.21.1
* **Mod loader** NeoForge 21.1.x
* **Install it on** the client, and on the server too for multiplayer

Put the jar in the `mods` folder and start the game. The details are on [Installation](Installation). The jar contains a copyrighted song, so it is for personal use only: do not share it publicly.

## Where to go next

* New here? [Installation](Installation), then [Becoming a Hero](Becoming-a-Hero), then [Controls](Controls).
* Recipes and where the rare items are found: [Items and Crafting](Items-and-Crafting).
* Running a server: [Commands](Commands) and [Server Configuration](Server-Configuration).
* Questions and mysteries (why can't I lift Mjolnir?): [FAQ](FAQ).
* Building the mod yourself, or improving this wiki: [Building from Source](Building-from-Source).

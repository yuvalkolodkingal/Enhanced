# Quicksilver

Peter Maximoff, the fastest man alive, as in the X-Men films: everything around him slows down, he speeds up, and the world starts playing *Sweet Dreams (Are Made of This)*. Or he can just be fast himself, and leave the world alone.

![Quicksilver](images/quicksilver.png)

## Becoming Quicksilver

Craft **Quicksilver's Goggles** and put them on. Like Magneto's Helmet, the first time you wear them you become Quicksilver for good, if you have no other power.

```
S I S
G D G

S = Sugar
I = Iron Ingot
G = Glass Pane
D = Block of Diamond
```

While he wears the goggles, Quicksilver's skin becomes Peter Maximoff's outfit from the movies: silver hair, a shiny silver jacket over a grey T-shirt, headphones and jeans. The goggles protect like an iron helmet and never break.

## Quicksilver Time

The main power. **Press R and time slows down around him, whether he runs or not.** He can stand still, walk, mine, build, fight or eat in the middle of it.

* Everything within **32 blocks** runs **5 times slower**: monsters, animals, villagers, falling blocks, TNT fuses, dropped items, smoke, sparks and splashes, and **arrows and thrown things, which hang in the air** and crawl forward. An arrow that finally lands still hits as hard as it would have.
* Other players in range are slowed down heavily (a strong Slowness).
* **Quicksilver speeds up when he sprints:** he runs **4 times faster** than normal and steps up whole blocks without jumping. He takes no fall damage while running. Walking, he moves at his normal speed, so he can still do precise work.
* **The world starts playing his song.** Everyone near him hears the theme from his Walkman for as long as Quicksilver Time is on, running or not (if his Walkman is on). It fades in when he turns Quicksilver Time on, keeps playing from where it was if he turns it off and on again within 10 seconds, and fades out when he turns it off.
* A cool grey shade at the edges of the screen shows that time is slowed, and while he runs Quicksilver leaves a grey blur and fading afterimages behind him (in the X-Men films his speed has no coloured glow).
* **Cost:** it burns hunger the whole time it is on, **half** as fast while he stands or walks and at the full rate while he runs. When the hunger bar is empty, Quicksilver Time switches itself off (with a message) and can't be switched on again until he has eaten. Creative players never run out.

![Quicksilver Time: sand and arrows hang in the air](images/quicksilver-time.png)

![Quicksilver running past, as another player sees him](images/quicksilver-run.png)

## Super Speed

A second mode, for when only **he** should be fast. **Sneak + R** turns Super Speed on or off.

* **The world is not slowed at all:** no slow motion, no song, no grey shade. Everyone else lives at the normal pace.
* When he sprints he runs at the **same super speed** as in Quicksilver Time (4 times faster, steps up whole blocks, no fall damage), with the same grey blur and afterimages, and Wall & Water Run works too.
* **Cost:** hunger only while he runs, at the full rate. With an empty hunger bar he can't run, but Super Speed stays on.
* **One mode at a time:** turning one on turns the other off. Both start off, and the choice is saved with the player.

## Doing everything faster

While Quicksilver Time **or** Super Speed is on, Quicksilver does everything **4 times as fast**, whether he runs or not:

| What | How it is faster |
|---|---|
| Mining | His block break speed is multiplied by 4: stone with a pickaxe takes a quarter of the time. |
| Attacking | His attack speed is multiplied by 4, so the attack cooldown recharges 4 times faster. |
| Eating, drinking, bows, crossbows and everything else you hold the use button for | The use runs 4 times as fast: a steak takes 8 ticks instead of 32, a bow is fully drawn in 5 ticks instead of 20, a crossbow is loaded in 7 instead of 25. Food is still eaten and arrows still fly at full strength. (A spyglass, a brush and a goat horn keep their own pace: there is nothing in them to wait for.) |
| Swimming | His swim speed is multiplied by 4. |
| Placing and breaking blocks one after another | On his own screen, the game's built-in pause between one block and the next (after a right click, and after a block is broken) is cut by the same factor, so holding the button places or breaks blocks without waiting. |

The movement speed bonus and the step-up still need **sprinting**, so he can still walk normally and precisely.

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (toggle) | Quicksilver Time | Turn Quicksilver Time on or off: the world around him is slowed, whether he runs or not. Off by default. **Sneak + R** turns Super Speed on or off instead: only he is fast. |
| 2 | **G** | Speed Blitz | Look at a creature up to 24 blocks away: Quicksilver is next to it in an instant and lands 6 rapid punches (2 damage each, ignoring the hit cooldown), then knocks it back. Cooldown 4 seconds. |
| 3 | **V** | Pickpocket | Snatch whatever the creature or player you look at (up to 24 blocks) holds in its hand, straight into your inventory. Cooldown 5 seconds. |
| 4 | **B** (toggle) | Wall & Water Run | While running (in Quicksilver Time or Super Speed), run straight up walls you run into, and across water as long as you keep running. |
| 5 | **N** (toggle) | Walkman | Turn the theme music on or off. |

## The song and your own music

The Walkman plays *Sweet Dreams (Are Made of This)* by Eurythmics, which is built into the mod. The recording is copyrighted, so the mod is for personal use only: do not share the jar publicly (see the [FAQ](FAQ)).

**To play a different song:**

1. Start the game once. The mod creates the folder `config/enhanced/music/` in your Minecraft folder, with a short `README.txt`.
2. Put an `.mp3`, `.ogg` or `.wav` file in that folder.
3. If there is more than one file, the first one in alphabetical order is played. Changes are picked up the next time the music starts.
4. Remove the file to go back to *Sweet Dreams*.

The song is played by each player's own game, from that player's own folder: whoever should hear your song needs it in their `config/enhanced/music/` folder. The mod does not download anything.

## Numbers you can change

How many times slower the world runs (5), how far time slows (32 blocks), Quicksilver's speed bonus (3.0, which is four times as fast), how many times as fast he does everything else (`actionSpeed`, 4) and the hunger cost are the `quicksilver.*` options on [Server Configuration](Server-Configuration).

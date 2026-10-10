# Quicksilver

Peter Maximoff, the fastest man alive, as in the X-Men films: when he runs, everything slows down, he speeds up, and the world starts playing *Sweet Dreams (Are Made of This)*.

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

The main power. **When Quicksilver sprints, time slows down around him:**

* Everything within **32 blocks** runs **5 times slower**: monsters, animals, villagers, falling blocks, TNT fuses, dropped items, smoke, sparks and splashes, and **arrows and thrown things, which hang in the air** and crawl forward. An arrow that finally lands still hits as hard as it would have.
* Other players in range are slowed down heavily (a strong Slowness).
* **Quicksilver speeds up:** he runs **4 times faster** than normal and steps up whole blocks without jumping. He takes no fall damage while running.
* **The world starts playing his song.** Everyone near him hears the theme from his Walkman. It fades in when he starts running, keeps playing from where it was if he stops and starts again within 10 seconds, and fades out when he stops.
* A cool grey shade at the edges of the screen shows that time is slowed, and Quicksilver leaves a grey blur and fading afterimages behind him (in the X-Men films his speed has no coloured glow).
* **Cost:** it burns hunger while he runs. With an empty hunger bar, he can only run normally.

![Quicksilver Time: sand and arrows hang in the air](images/quicksilver-time.png)

![Quicksilver running past, as another player sees him](images/quicksilver-run.png)

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (toggle) | Quicksilver Time | On by default: sprinting starts Quicksilver Time. Turn it off to sprint normally. |
| 2 | **G** | Speed Blitz | Look at a creature up to 24 blocks away: Quicksilver is next to it in an instant and lands 6 rapid punches (2 damage each, ignoring the hit cooldown), then knocks it back. Cooldown 4 seconds. |
| 3 | **V** | Pickpocket | Snatch whatever the creature or player you look at (up to 24 blocks) holds in its hand, straight into your inventory. Cooldown 5 seconds. |
| 4 | **B** (toggle) | Wall & Water Run | During Quicksilver Time, run straight up walls you run into, and across water as long as you keep running. |
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

How many times slower the world runs (5), how far time slows (32 blocks), Quicksilver's speed bonus (3.0, which is four times as fast) and the hunger cost are the `quicksilver.*` options on [Server Configuration](Server-Configuration).

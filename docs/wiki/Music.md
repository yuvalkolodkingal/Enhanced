# Music

The mod's music (3.5): the Walkman songs of the speedsters, the Witches' Road's ballad in a version for every part, the music of Asgard and its fights, and how a server, or you, can put other songs in their place. All the music written for the mod is original: no melody, bass line or chords are taken from any song or film.

Hebrew: [עברית](Music-HE)

![A speed run across the map](images/018-tommy-speed-run.png)

## The Walkman: Quicksilver and Speed

### Quicksilver: "Silver Hours"

Since 3.5 Quicksilver's Walkman plays **his own theme, "Silver Hours"**: an original 80s synth-pop song, written in the spirit of songs like *Sweet Dreams (Are Made of This)* (only the style): a pumping synth bass, a drum machine, a cold pad and a bell-like lead, 63 seconds, looping.

The rules haven't changed: it plays while **Quicksilver Time** is on (running or standing still; Super Speed alone plays nothing), for everyone near enough to be caught in the slow motion, and only while his Walkman is on (**Sneak + R**). See [Quicksilver](Quicksilver).

**Want *Sweet Dreams*?** Since 3.8.4 it is no longer built into the mod, but you can play your own copy: put it in **`config/enhanced/music/`** as `sweet_dreams.mp3` (or `.ogg` or `.wav`), and in your own game's options, **`config/enhanced-client.toml`**, section `[music]`, set `quicksilverSong = "sweet_dreams"` (the default is `"theme"`). The game's **Mods → Enhanced → Config** screen has it too. Only your own game changes. Without the file he plays *Silver Hours*, and the game tells you once where to put it.

### Speed: "Westview Blur"

New in 3.5: **Tommy (Speed)** has a Walkman too, with **his own theme, "Westview Blur"**: brighter and faster original synth-pop (a driving octave bass, four-on-the-floor drums, claps, a bright arpeggio), 75 seconds, looping.

* It plays while he **runs with Super Speed** (**G** on, and running) and while his **Slow Time** (**R**) is on.
* When he stops running it keeps going for 2 seconds, so it doesn't stutter, then fades out; for 10 more seconds it plays on silently, so if he runs again soon it carries on where it was.
* Speed has no Walkman key: his Walkman is on. (It is the same Walkman switch as Quicksilver's.)

See [Tommy](Tommy).

### For both

* Two speedsters near you: you hear **your own** Walkman first, otherwise the nearest one's. A change of song crossfades.
* The volume is the **Jukebox/Note Blocks** slider. The game's own music waits while a Walkman song plays.
* A [custom hero](Hero-Creator) with Quicksilver Time plays Quicksilver's song; one with Speed's Super Speed or Slow Time plays Speed's.

## The Witches' Road: a version of the ballad for every part

![A cabin on the Witches' Road](images/043-witches-road-cabin.png)

The [Witches' Road](Witches-Road) plays its ballad (the mod's own tune of 3.3) in a different version at every part of the Road, as the *Ballad of the Witches' Road* has many versions in the show. 3.5 adds a sixth, for the **choice**:

| Part | When | Version |
|---|---|---|
| The entrance | At the door during its ritual, while it stands open for you, and on the way in | The coven hums the tune over a drone, bells and a heartbeat drum |
| The Parkour | Trial 1 | Lute and harpsichord, a frame drum, a shaker |
| Copy the Structure | Trial 2 | A music box over a soft choir |
| The Ice-Boat Race | Trial 3 | Driving strings, drums and horns |
| **The choice** (new) | In the hall at the end, while the Road waits for you to choose Billy or Tommy | A solo cello over a low humming choir and a slow tick; then the harp answers in E major |
| The end of the Road | In the hall at the end, while the light gathers, and in the farewell | The whole ensemble, ending bright in E major |

The music crossfades (2 seconds) from one part to the next and fades out (3 seconds) when you leave the Road. It plays on the **Music** slider.

## Asgard, the Watch and Hela

![The palace of Asgard](images/092-asgard-palace.png)

[Asgard](Asgard) has three pieces of its own (*Realm Eternal*, *The Golden Halls*, *Edge of the Realms*) and its own air (wind, a far hum, chimes); [Heimdall's](Heimdall) **Guardian's Watch** has a tense theme of hourglass ticks, pizzicato and his horn call; and the fight with [Hela](Hela) has a battle theme in three layers, one for each phase. All original.

## A world's own music (servers)

New in 3.5: a server owner can replace the Walkman songs and the Road's music **for every player**, with no resource pack and nothing for the players to install.

### The folder

**`<world>/serverconfig/enhanced/music/`**: on a dedicated server `world/serverconfig/enhanced/music/`, in single player `saves/<world>/serverconfig/enhanced/music/`. The server makes it, with a `README.txt`, when it starts.

### The names

| File | Replaces |
|---|---|
| `quicksilver.ogg` | Quicksilver's Walkman song |
| `speed.ogg` | Speed's Walkman song |
| `witches_road.ogg` | Every part of the Witches' Road |
| `witches_road_entrance.ogg`, `witches_road_parkour.ogg`, `witches_road_structure_copy.ogg`, `witches_road_ice_boat.ogg`, `witches_road_choice.ogg`, `witches_road_ending.ogg` | One part of the Road only (a trial added later gets `witches_road_<its id>.ogg`; the README lists them) |
| `music.ogg` | Any of the above that has no file of its own |

**Which one plays** (the first that exists):

* a part of the Road: `witches_road_<part>.ogg` → `witches_road.ogg` → `music.ogg` → the mod's own;
* Quicksilver: `quicksilver.ogg` → `music.ogg` → his theme;
* Speed: `speed.ogg` → `music.ogg` → his theme.

### The files

* **Ogg Vorbis only** (`.ogg`; the mod checks the file itself, not just its name), so every player can play it with nothing extra. Anything else (an `.mp3`, a broken or empty file, a name the mod doesn't know) is left out, and the server's log says why.
* At most **8 MB** a file (`music.maxFileKB`); bigger files are left out.
* After you change the files: `/reload`, or **`/enhanced music reload`**. **`/enhanced music list`** shows the folder, every song (its size and the start of its hash) and every file left out, with the reason. Both are for operators. See [Commands](Commands#music-enhanced-music).

### How the players get them

* When a player joins (and after a reload), the server tells him which songs it has. A song he doesn't have yet is downloaded in small pieces while he plays (about 1.2 MB a second, one song at a time), checked, and kept in **`config/enhanced/music_cache/`**. Each song is downloaded **once**, even if several servers use it.
* Until a song has arrived, the mod's own plays; when it arrives, the music crossfades to it.
* A player with an older version of the mod can still join: he simply hears the mod's own music. (The songs travel on a channel of their own, apart from the mod's network protocol.)
* **Don't want the world's songs?** In your `config/enhanced-client.toml`, section `[music]`, set `ignoreServerMusic = true`: your game downloads and plays none of them.

The server options are `music.enabled` (true; false sends no songs), `music.maxFileKB` (8192) and `music.chunksPerTick` (2): see [Server Configuration](Server-Configuration#music).

## Your own music (players)

* The folder **`config/enhanced/music/`** in your Minecraft folder (the mod makes it, with a `README.txt`), as before. Any `.mp3`, `.ogg` or `.wav`.
* **New:** files named after the music: `quicksilver.*`, `speed.*`, `witches_road.*` and `witches_road_<part>.*` (the same parts as above).
* **As before:** any other song file there (`my song.mp3`, `music.mp3`...) is Quicksilver's; with several, the first in alphabetical order.
* **`sweet_dreams.*`** (3.8.4): your own copy of *Sweet Dreams*. With `quicksilverSong = "sweet_dreams"` it plays for Quicksilver before any other file.
* Your own songs play only in your own game. **Your song beats the world's, and the world's beats the mod's.**

## Options

| Where | Option | Default | What it does |
|---|---|---|---|
| Client (`enhanced-client.toml`, `[music]`) | `quicksilverSong` | `theme` | Quicksilver's song: `theme` ("Silver Hours") or `sweet_dreams` (your own copy, `sweet_dreams.*` in `config/enhanced/music/`; without it, "Silver Hours"). |
| Client (`enhanced-client.toml`, `[music]`) | `ignoreServerMusic` | `false` | Don't download or play the world's songs. |
| Server (`[music]`) | `enabled` | `true` | Send the songs of the world's music folder to the players. |
| Server (`[music]`) | `maxFileKB` | `8192` | The biggest song sent, in KB (16 to 65536). |
| Server (`[music]`) | `chunksPerTick` | `2` | Pieces of 30 KB sent to a downloading player each tick (1 to 16; 2 is about 1.2 MB a second). |

## Related

[Quicksilver](Quicksilver) · [Tommy](Tommy) · [Witches' Road](Witches-Road) · [Commands](Commands) · [Server Configuration](Server-Configuration#music) · [Credits](Credits)

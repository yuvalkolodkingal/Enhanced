# Installation

## What you need

| | |
|---|---|
| **Minecraft** | Java Edition 1.21.1 |
| **Mod loader** | NeoForge 21.1.256 or newer (the 21.1.x line is the one for Minecraft 1.21.1) |
| **Java** | 21 (the Minecraft launcher already includes it) |
| **Install it on** | The client, and the server too for multiplayer |

In multiplayer, the mod adds items, a block, entities and key bindings, so the server and every player who joins need the same version of it.

## Install the mod

1. Install **NeoForge** for Minecraft 1.21.1 (the installer is on [neoforged.net](https://neoforged.net/)) and start the game once with the NeoForge profile, so it creates the `mods` folder.
2. Download the newest jar from the [Releases](https://github.com/yuvalkolodkingal/enhanced/releases) page. It is the build marked *Latest*, named like `enhanced-1.0.0-build.12.jar`. (To build the jar yourself, see [Building from Source](Building-from-Source).)
3. Put the jar in the `mods` folder of your Minecraft folder:
   * Windows: `%appdata%\.minecraft\mods`
   * macOS: `~/Library/Application Support/minecraft/mods`
   * Linux: `~/.minecraft/mods`
4. Start Minecraft with the NeoForge profile. *Enhanced* appears in the Mods list, and a creative tab named **Enhanced** holds every item.

## Install it on a server

Put the same jar in the `mods` folder of a NeoForge 21.1 server and start it. Players need the mod on their own client to join. A single-player world works like a server of its own.

## What the mod creates on first start

| What | Where | Why |
|---|---|---|
| Music folder | `config/enhanced/music/` in the Minecraft folder (with a short `README.txt`) | Put a song here to replace Quicksilver's or Speed's Walkman song or the Witches' Road's music, in your own game. See [Music](Music). |
| The world's music folder (3.5) | `<world>/serverconfig/enhanced/music/` (with a `README.txt`), made when the server starts | Songs here replace the Walkman songs and the Road's music for every player, who download them by themselves. See [Music](Music#a-worlds-own-music-servers). |
| Server settings | `world/serverconfig/enhanced-server.toml`, created when the world is first loaded | Every balance number of the mod. See [Server Configuration](Server-Configuration). |
| Key bindings | *Options, Controls, Enhanced Powers* | The five ability keys and the Change Look key. See [Controls](Controls). |

## Updating

Replace the old jar in the `mods` folder with the new one (on the client and on the server). Your hero, your look and your saved Bifrost places are saved with the player. **3.8 needs the server and every player updated together** (network protocol 12; 3.8.1 is protocol 13, so update 3.8.0 to 3.8.1 on the server and every player together too; 3.8.1 to 3.8.4 are all protocol 13 and play together): a 3.7 player cannot join a 3.8 server, or the other way round. (3.7, 3.7.1 and 3.7.2 were protocol 11, 3.6 and 3.6.1 were 10, 3.5 was 9.) Worlds of 3.7 open in 3.8 with every hero as they left them: every Avengers Tower gets its [armoury](Structures#the-armoury-38) when a player first comes near, and the Shrines of the Cosmic Entities appear in End islands not explored yet. (From 3.6 to 3.7, every Mutant School got its [lower lab](Mutant-School-Lab) when a player first came near, and Agatha's Cottages appeared in land not explored yet.) (From 3.5 to 3.6, the new places appeared in land not explored yet, and every Infinity Gauntlet already in the world stayed Thanos's Gauntlet.)

## Quicksilver's song

Quicksilver's Walkman plays his own theme, *Silver Hours*. Since 3.8.4 *Sweet Dreams (Are Made of This)* by Eurythmics is no longer built into the jar: to hear it, put your own copy in `config/enhanced/music/` as `sweet_dreams.mp3` (or `.ogg` or `.wav`) and set the client option `quicksilverSong = "sweet_dreams"`. See [Music](Music).

## Troubleshooting

| Problem | Try this |
|---|---|
| "Missing mod" or you cannot join a server | Install the same jar on the client and on the server. |
| The game says NeoForge is too old | Update NeoForge to 21.1.256 or newer. |
| The keys do nothing | Keys work only for a hero, with no screen open. See [Becoming a Hero](Becoming-a-Hero), and check the bindings under *Options, Controls, Enhanced Powers*. |
| I cannot lift Mjolnir | See the [FAQ](FAQ) and [Thor](Thor). |
| Quicksilver's song does not play | The song plays for as long as Quicksilver Time is on (press R), whether Quicksilver runs or not, and **Sneak + R** turns the Walkman on and off. Super Speed (G) alone plays no song. A song of your own must be an `.mp3`, `.ogg` or `.wav` file in `config/enhanced/music/`. See [Music](Music). |

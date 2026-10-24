# Commands

All commands start with `/enhanced` and are for **operators** (permission level 2): server owners and players who can use `/gamemode`. (Two small commands are for everyone: `/road leave` and `/road status`, below.) Heroes are otherwise permanent, so these commands are also the only way to take a power away. (The exceptions are `/enhanced_anim`, `/enhanced_model` and `/enhanced_ultron_fx`, for looking at the animations, the bosses and Ultron's effects: see below.)

| Command | Effect |
|---|---|
| `/enhanced power get [player]` | Shows a player's power. Without a player, it shows yours. |
| `/enhanced power set <players> <power> [look]` | Gives or removes a power, optionally straight into one of its looks. |
| `/enhanced look <players> <look>` | Changes the look of every given player whose hero has that look. |
| `/enhanced hex list` | Lists all Hexes in the current dimension (the owner, the position, the radius and how many players are allowed in). |
| `/enhanced hex clear` | Dissolves every Hex in the current dimension. |
| `/enhanced boss start thanos [force]` | Calls the Mad Titan where you stand, as the Tesseract and Loki's Scepter do: a portal opens and six seconds later he lands 14 to 20 blocks from you. `force` skips the site check, the Overworld rule, the server's `summonEnabled` switch and the rest (Peaceful and one fight at a time still apply; given in the air, the fight is made on the ground under you). See [Thanos](Thanos). |
| `/enhanced boss start ultron [force]` | Without `force` it is the ritual without the Scepter: you need the Ultron Program in your hand (it is used up), Ultron wakes on the block you look at, and every check is made. With `force` he wakes at your feet with no Program, and the checks of the ground are skipped (not the open sky, and never when he is switched off, in Peaceful or while another boss fight runs). See [Ultron](Ultron). |
| `/enhanced boss start apocalypse [force]` | With `force`, wakes En Sabah Nur at your feet, with no Capstone, and skips the checks of the ground, the sky and the need for a Scarlet Witch or a Phoenix (not Peaceful, not the switch that turns him off, not the one-fight-at-a-time rule). Without it the command is the ritual itself: it needs a Capstone in your hand and a block in sight, and every check. See [Apocalypse](Apocalypse). |
| `/enhanced boss start hela [force]` | (3.5) Starts the fight with [Hela](Hela) in Asgard's throne room. Without `force` the seal's rules apply: you must be in Asgard, not in Creative or Spectator, the server's `summonEnabled` must be on and the seal must have healed. With `force` you may be anywhere and in any mode: only an Asgard is needed (its chunks are loaded; you are not moved). Never in Peaceful, and one fight at a time. |
| `/enhanced boss stop` | Ends the running boss fight. A used Ultron Program or Capstone is given back, and the island of Sokovia is put back. A second `stop` while a boss is still leaving ends it at once. |
| `/enhanced boss list` | Lists the running boss fights. |
| `/enhanced boss cleanup <center> <radius>` | Removes every creature and projectile of a boss's kind within that radius, whether a fight owns it or not: Thanos, his sword, meteors, orbs and illusions; Ultron, the Sentries (a hero Ultron's too), his bolts and the Vibranium Key; Apocalypse, his lances, boulders and cocoons. It removes no blocks. |
| `/enhanced build undo <player> [steps]` | Undoes that player's last Build Magic steps (1 by default, 1 to 1000), newest first, as their Ctrl + Z would, with the same safe rule: blocks someone changed since stay as they are. A parent can fix a child's mistake. **The player may be offline** (see below). See [Build Magic](Build-Magic). |
| `/enhanced build stop [<player>\|all]` | Stops the Build Magic spells that are working, where they are (what they did stays, and can be undone), and drops the ones waiting. With a player, only his; with `all` or nothing, everybody's. |
| `/enhanced build list` | Lists the spells working or waiting on the server: the player, the spell, the dimension, the percent done and the blocks. |
| `/enhanced build history <player>` | Lists that player's steps, oldest first, numbered from 1: "1. Fill, 140 blocks, 2 min ago, overworld". The steps he has undone (the ones Ctrl + Y could do again) come last. Works for a player who is offline. |
| `/enhanced build rank <player> [1-10]` | Shows Billy's rank and practice. With a number it sets the rank (his practice becomes that rank's number, or stays above it if it was higher and the rank goes up, and his meter fills to its new top). Only works on a Billy. |
| `/enhanced build meter <player> <value>` | Sets Billy's magic meter (0 up to the top of his rank). Only works on a Billy. |
| `/enhanced build forget <player>` | Clears that player's Build Magic history and pocket, from memory and from disk. |
| `/enhanced ward list` | Lists every ward of the [Book of the Vishanti](Sorcerers-Books#the-book-of-the-vishanti) on the server: whose, its name, where it is (in which world) and how far it reaches. |
| `/enhanced ward remove <profile>` | Takes away every ward of that player (their bases stay as plain saved places). The profile is an online player, a selector, or the name or id of someone who is offline or gone (the wards go by id alone). Wards are otherwise kept until their base is forgotten, so this is the operator's way to take one down. |
| `/enhanced road status [player]` | How many trials of the [Witches' Road](Witches-Road) a player has won, whether they are on the Road, and how many times they fell. Without a player: you. |
| `/enhanced road enter <players> [trial]` | Sends players onto the Road at once, with no Gene needed (an operator's way to test). `trial` is 1 (the Parkour), 2 (Copy the Structure) or 3 (the Ice-Boat Race); without it they start the trial they reached. |
| `/enhanced road leave <players>` | Takes players off the Road, back to where they came in, keeping their Gene and progress. |
| `/enhanced road reset <players> [newcourse]` | Forgets their Road progress; with `newcourse` they also get new courses. |
| `/enhanced road skip <players>` | Wins the trial they are in (or counts the next as won). |
| `/enhanced road set <players> <won>` | Sets how many trials they have won (0 to 99). |
| `/enhanced road layout [player]` | Shows where the checkpoints, the finish and the gate of a player's trial are. |
| `/enhanced road cells` | How many places of the Road are in use or being cleared, and how many players are on it. |
| `/road leave` | **Anyone** may use it: leaves the Witches' Road, keeping the Gene and the progress. |
| `/road status` | **Anyone** may use it: how many trials you have won. |
| `/road choose` | **Anyone** may use it, at the end of the Road: the choice of Billy or Tommy is asked again. |

## Asgard: /enhanced asgard

![The palace of Asgard](images/092-asgard-palace.png)

(3.5) For [Asgard](Asgard):

| Command | Effect |
|---|---|
| `/enhanced asgard tp [players]` | Takes you (or the players) to Heimdall's Observatory in Asgard at once. An operator's teleport passes Asgard's gate. |
| `/enhanced asgard state` | Asgard's saved state: whether Odin is awake, how often Hela was beaten, the murals, the residents, when the seal is ready, and how long the city's chunks took to build. |
| `/enhanced asgard mendhall` | (3.7.2) Mends the Hall of Feasts now: a hall built before 3.7.2 gets its new tables, seats, lanterns and kitchen doors; a block a player changed there is left alone. It is done by itself once, the first time the hall is loaded; the hall must be loaded (go near it). |
| `/enhanced asgard genbench` | For testing: builds a block of 9 × 9 chunks of the city (and one of the sea) in memory, throws them away, and prints how many milliseconds a chunk took. The world is not touched. |

Asgard has no portal: an operator can also use `/tp` or `/execute in enhanced:asgard`. Every other way in or out is the Bifrost's.

## Music: /enhanced music

![A speed run across the map](images/018-tommy-speed-run.png)

(3.5) For [a world's own music](Music#a-worlds-own-music-servers), the songs in `<world>/serverconfig/enhanced/music/`:

| Command | Effect |
|---|---|
| `/enhanced music reload` | Reads the folder again (as `/reload` does) and sends the players the new list of songs: "The world's music: 3 song(s) ready, 1 file(s) left out". |
| `/enhanced music list` | Shows the folder, each song (its size and the start of its hash) and every file that was left out, with the reason (not Ogg Vorbis, too big, an unknown name, empty). |

## Structures: /enhanced structure

![A cabin on the Witches' Road](images/043-witches-road-cabin.png)

| Command | Effect |
|---|---|
| `/enhanced structure school_lab [radius]` | (3.7) Builds the [X-Men lower lab](Mutant-School-Lab) of the Mutant School nearest to you (within 128 blocks, or `radius`, 16 to 256) now, or finishes one being built: "The lower lab is built. Its lift starts in the study at …", or "This school's lab is already built." The lab is otherwise built by itself when a player comes near (`school_lab.autoBuild`). |
| `/enhanced structure school_lab force` | (3.7) The same, and puts the secret entrance in the study even over a player's blocks (without `force`, a block a player put there keeps the entrance out: "a player's block stands where the entrance goes"). |
| `/enhanced structure school_lab status` | (3.7) The nearest school's lab: not begun, being built, built or blocked (and why), where the study's and the lab's pads are, how it is turned, and for a built lab its holes, stray water or lava inside, and doors without a frame (0, 0 and 0 in a whole lab). |
| `/enhanced structure refit_tower [radius]` | (3.8) Refits the [Avengers Tower](Structures#towers-built-before-38-the-refit) nearest to you (within 96 blocks, or `radius`, 16 to 512) now: a tower built before 3.8 gets its armoury (the suit displays, the reactor, serum, shield and wings cases, the signs), only where the 3.6.1 tower's own blocks still stand; the old armour stands and the shield's frame go only if they are still the tower's. It tells you how many blocks it set, how many stands it took away, and what of the players' it left. A tower is otherwise refitted by itself when a player comes near (`avengers_tower.refit`). A tower of the old 3.6.0 design is left as it is. The chunks round the armoury must be loaded ("come closer and try again"). |
| `/enhanced structure refit_tower [radius] force` | (3.8) The same, and puts the blocks even where a player changed them (what stood there drops as if broken). A player's armour stand or item frame is never touched, and nobody's takes are forgotten. |
| `/enhanced structure refit_tower status` | (3.8) The nearest tower: refitted or not yet, its armoury's version, how many blocks the refit set, stands it took away, and players' blocks it left. |
| `/enhanced structure repair_school [radius]` | (3.5) Mends a [Mutant School](Structures#the-mutant-school) built before 3.3.1, the hole round its front door: only the 76 blocks of the door and the tower that 3.3.1 changed, in the school's own stone. It looks for the school nearest to you within 128 blocks (or `radius`, 16 to 256). A block a player has changed there is left alone and counted ("kept"); run it again any time, and it says when the school is whole. |

To **find** a structure, use the game's own `/locate structure <id>`: `enhanced:hydra_outpost`, `enhanced:mjolnir_crater`, `enhanced:mutant_school`, `enhanced:bifrost_site`, and since 3.6 `enhanced:sanctum`, `enhanced:avengers_tower`, `enhanced:end_tunnel` (run it in the End) and `enhanced:vormir_cliff` (run it in the Nether), and since 3.7 `enhanced:agatha_cottage`, and since 3.8 `enhanced:eternity_shrine` (in the End). See [Structures](Structures#the-stone-sites-36). The schools' lower labs are not structures of their own: they are under the schools.

## The Avengers (3.8)

![The Avengers Tower from outside](images/085-avengers-tower-exterior.png)

| Command | Effect |
|---|---|
| `/enhanced serum open <player>` | Opens the [Super Soldier Serum](Super-Soldier-Serum)'s choice for a player who holds no serum: Captain America, Red Hulk or Falcon (Falcon still only with the Exo-Wings). Nothing is used up. "Opened the serum's choice for Eilon." |
| `/enhanced eternity reset <player>` | The player may make a wish to [Eternity](Eternity) again ("Eilon may make a wish to Eternity again."). His visits and his way home are kept. |
| `/enhanced eternity send <player>` | Takes the player to Eternity's realm, as the altar would (his way home is where he stands now), or, if he is in the realm, sends him home. |

`/enhanced structure refit_tower` (above) refits an old Avengers Tower. Iron Man's suits, the reactors, the serum, the shield and the wings can also be given with `/give` (`enhanced:iron_man_mark_85`, `enhanced:arc_reactor`, `enhanced:special_soldier_serum`, `enhanced:vibranium_shield`, `enhanced:falcon_exo_wings`...).

## Build Magic: /enhanced build

![Build Magic: the spell hints on the HUD](images/006-scarlet-witch-build-magic.png)

The `build` commands are for the operator who looks after the builders: they use the same safe rules as the heroes' own Ctrl + Z. `rank`, `meter`, `forget` and `stop <player>` name a player who is **online** (as in `/tp`). `undo` and `history` also take a player who is **offline**: give a name the server has seen (it is in its `usercache.json`) or paste his UUID; anything else is refused. His history is read from the disk the first time it is asked for, and his profile acts for him, so he need not be there. The history itself is on the disk, and a step that is no longer in memory is read back as needed.

* **`undo`** takes back a player's steps one by one, newest first, even a step from another day, as long as it is still in their history. Each step runs as a spell of that player, queued behind whatever he is doing, with the ripple running backwards, and the answer says how many steps were queued ("Undid 3 spells of Steve"). A block that someone has changed since stays as it is (the player is told how many), and a part that is protected now can be tried again later. A step whose file cannot be read is skipped.
* **`stop`** is the emergency brake for a spell that is eating the server's time: it stops where it is and leaves what it did. `/enhanced build list` shows what is working.
* **`rank`** and **`meter`** are for Billy only (they say "is not Billy" for anyone else): `rank` shows or sets his rank (1 to 10, or up to the server's `build_magic_billy.maxRank`), `meter` sets how much magic he has right now.
* **`forget`** deletes a player's history and pocket, from the memory and from the disk (`data/enhanced/build/<id>/` in the world folder): use it when one player's history has grown big. (For a player who is gone for good, delete his folder there while the server is stopped.)

| Example | What it does |
|---|---|
| `/enhanced build undo Steve` | Takes back Steve's last spell. |
| `/enhanced build undo Steve 5` | Takes back his last five. |
| `/enhanced build stop all` | Stops every spell on the server. |
| `/enhanced build rank Eilon 4` | Makes Billy a Wiccan. |
| `/enhanced build rank Eilon 10` | Makes Billy the Chaos Master. |
| `/enhanced build meter Eilon 0` | Empties his meter. |

## Powers

The `<power>` argument is one of:

| Power | Hero |
|---|---|
| `none` | No powers (removes the player's hero) |
| `magneto` | [Magneto](Magneto) |
| `scarlet_witch` | [Scarlet Witch](Scarlet-Witch) |
| `quicksilver` | [Quicksilver](Quicksilver) |
| `vision` | [Vision](Vision) |
| `tommy` | [Tommy](Tommy) |
| `billy` | [Billy](Billy-and-the-Witches-Road) |
| `loki` | [Loki](Loki) |
| `thor` | [Thor](Thor) |
| `phoenix` | [Phoenix](Phoenix) |
| `spider_man` | [Spider-Man](Spider-Man) |
| `thanos` | [Thanos](Thanos) |
| `ultron` | [Ultron](Ultron) |
| `apocalypse` | [Apocalypse](Apocalypse) |
| `agatha` | [Agatha](Agatha) |
| `cyclops` | [Cyclops](Cyclops) |
| `professor_x` | [Professor X](Professor-X) |
| `heimdall` | [Heimdall](Heimdall) (3.5) |
| `odin` | [Odin](Odin) (3.5) |
| `hela` | [Hela](Hela#becoming-hela-the-hero) (3.5) |
| `doctor_strange` | [Doctor Strange](Doctor-Strange) (3.6) |
| `hulk` | [Hulk](Hulk) (3.6) |
| `wolverine` | [Wolverine](Wolverine) (3.7) |
| `deadpool` | [Deadpool](Deadpool) (3.7) |
| `captain_america` | [Captain America](Captain-America) (3.8) |
| `red_hulk` | [Red Hulk](Red-Hulk) (3.8) |
| `iron_man` | [Iron Man](Iron-Man) (3.8) |
| `falcon` | [Falcon](Falcon) (3.8) |
| `love` | [Love](Love) (3.8) |
| `custom` | The player's own hero from the [Hero Creator](Hero-Creator): the one they last made (an empty hero, with no powers, if they never made one) |

The command suggests the names as you type. A player who is given a power awakens as that hero, with the title, the sound and an announcement to everyone, as if they had earned it; since 3.8.1 the announcement says plainly that they became the hero ("Steve has become Thor!"), not the story of the hero's item. If a player already has that power and you give a look, only the look changes.

## Looks

The `[look]` or `<look>` argument is the id of a look (see [Looks](Looks)):

| Hero | Look ids |
|---|---|
| Vision | `vision`, `white_vision` |
| Tommy | `halloween`, `tracksuit`, `speed`, `own` |
| Billy | `wiccan`, `teen`, `halloween`, `comics`, `own` |
| Loki | `loki`, `tva`, `god_of_stories`, `own` |
| Thor | `thor`, `ragnarok`, `love_and_thunder`, `love_and_thunder_helmet`, `mighty_thor`, `own` |
| Phoenix | `apocalypse`, `brand_new_day`, `own` |
| Spider-Man | `homemade`, `iron_spider`, `brand_new_day`, `own` |
| Thanos | `infinity_war`, `endgame`, `own` |
| Ultron | `prime`, `sentry`, `human`, `own` |
| Apocalypse | `apocalypse`, `en_sabah_nur`, `own` |
| Agatha | `wandavision`, `all_along`, `own` |
| Cyclops | `apocalypse`, `classic`, `own` |
| Professor X | `young`, `old`, `own` |
| Heimdall | `guardian`, `ragnarok`, `own` |
| Odin | `allfather`, `wanderer`, `own` |
| Hela | `goddess`, `arrival`, `own` |
| Doctor Strange | `sorcerer_supreme`, `multiverse`, `defender`, `own` |
| Hulk | `ragnarok`, `brand_new_day`, `banner`, `own` (`banner` and `own` are Bruce Banner, with no powers) |
| Wolverine | `classic`, `x_men_2000`, `own` |
| Deadpool | `classic`, `trainee`, `kidpool`, `own` |
| Captain America | `classic`, `stealth`, `own` |
| Red Hulk | `red_hulk`, `own` |
| Iron Man | `tony`, `own` (his suits are items, not looks) |
| Falcon | `endgame`, `winter_soldier`, `own` |
| Love | `love`, `own` |

Magneto, the Scarlet Witch and Quicksilver have no looks to choose, and a custom hero's look is chosen in the Hero Creator. Professor X given without a look is the young Charles (the screen that asks which Charles he is opens only after the wheelchair; **H** changes it). `/enhanced look` skips players whose hero does not have the look, and it fails with "Unknown look" if no given player has it.

## Examples

| Command | What it does |
|---|---|
| `/enhanced power get Steve` | Tells you what Steve is. |
| `/enhanced power set Steve thor` | Makes Steve Thor. |
| `/enhanced power set @a loki tva` | Makes every player Loki, in the TVA Variant look. |
| `/enhanced power set Steve phoenix brand_new_day` | Makes Steve the Phoenix, in the Brand New Day look. |
| `/enhanced power set Steve spider_man iron_spider` | Makes Steve Spider-Man, in the Iron Spider suit. |
| `/enhanced power set Steve thanos endgame` | Makes Steve Thanos, in the Endgame armour (he skips the Mad Titan and the Gauntlet). |
| `/enhanced power set Steve ultron sentry` | Makes Steve Ultron, in the Sentry look. |
| `/enhanced power set Steve apocalypse en_sabah_nur` | Makes Steve Apocalypse, in the 3600 BC look (he skips the boss and the Essence). |
| `/enhanced power set Steve agatha all_along` | Makes Steve Agatha, in her outfit of the Witches' Road. |
| `/enhanced power set Steve professor_x old` | Makes Steve Professor X, the old one in his wheelchair. |
| `/enhanced power set Steve cyclops classic` | Makes Steve Cyclops, in the comics' suit (he still needs his glasses on his head to fire). |
| `/enhanced power set Steve none` | Takes Steve's power away. |
| `/enhanced power set Steve doctor_strange` | Makes Steve Doctor Strange (he gets no Eye this way: give him one, or use Choose a Hero). |
| `/enhanced power set Steve hulk brand_new_day` | Makes Steve the Hulk, the Brand New Day Hulk at once. |
| `/enhanced power set Steve wolverine x_men_2000` | Makes Steve Wolverine, in the *X-Men* (2000) leather (he skips the injection). |
| `/enhanced power set Steve deadpool kidpool` | Makes Steve Deadpool, as Kidpool. |
| `/enhanced structure school_lab status` | Tells you about the lab of the nearest school. |
| `/enhanced power set Steve captain_america stealth` | Makes Steve Captain America, in the stealth suit (give him a shield, or use Choose a Hero). |
| `/enhanced power set Steve iron_man` | Makes Steve Iron Man (give him an Arc Reactor and a suit, or use Choose a Hero). |
| `/enhanced serum open Steve` | Opens the serum's choice for Steve. |
| `/enhanced eternity reset Steve` | Gives Steve his wish back. |
| `/enhanced structure refit_tower status` | Tells you about the armoury of the nearest Avengers Tower. |
| `/enhanced look Steve white_vision` | Puts Steve, a Vision, in the White Vision look. |
| `/enhanced hex list` | Lists the Hexes in your dimension. |
| `/enhanced hex clear` | Dissolves them all. |
| `/enhanced boss start thanos` | Calls the Mad Titan where you stand, if the site would do for the ritual. |
| `/enhanced boss start ultron force` | Wakes Ultron where you stand, with no Program or Scepter. |
| `/enhanced boss start apocalypse force` | Wakes En Sabah Nur where you stand, with no Capstone and no one to break his shield. |
| `/enhanced boss cleanup ~ ~ ~ 40` | Removes whatever a boss left within 40 blocks of you: his creatures and his projectiles. |
| `/enhanced road enter Steve 2` | Sends Steve to the second trial of the Witches' Road, Copy the Structure. |
| `/enhanced road skip Steve` | Wins the trial Steve is in. |
| `/enhanced ward list` | Lists every ward of the Book of the Vishanti. |

## Looking at the animations: /enhanced_anim

`/enhanced_anim` is a different kind of command. It runs only in your own game, needs no operator rights and changes nothing for anyone else. It is for looking closely at the heroes' animation, for example to take a screenshot of a pose. The commands act on you (as a hero), or on another player in view with `target`.

| Command | What it does |
|---|---|
| `/enhanced_anim play <gesture>` | Starts a gesture on you whatever you are doing, for example `THOR_SHATTER`, `THOR_PIN` or `WANDA_SHIELD_RAISE`. The game suggests the names as you type. Add a tick (0 to 100) to freeze the gesture there. |
| `/enhanced_anim hold <ticks>` | Jumps every gesture that is playing to this tick (0 to 100) and freezes the animation. |
| `/enhanced_anim release` | Lets the animation run again. |
| `/enhanced_anim fly <s> <u> <turn>` | Pretends you fly at speed `s` (1 is cruising, 2 a sprint), climbing `u` (-1 diving, 1 climbing) and turning `turn` degrees per tick (-20 to 20), so you can see that pose without flying that fast. `/enhanced_anim fly off` goes back to your real flight. |
| `/enhanced_anim stance <stance> on` | Forces a stance (a pose that is held, such as `SHIELD_WANDA` or `THOR_BEAM`) on. With `off` it is forced off, with `auto` it is left to the game, and `/enhanced_anim stance off` puts every stance back to normal. |
| `/enhanced_anim target <name>` | Makes the commands act on another player in view instead of you. `/enhanced_anim target self` goes back to you. |
| `/enhanced_anim state` | Shows or hides the numbers behind the pose. |

## Looking at the bosses: /enhanced_model

`/enhanced_model` runs only in your own game too, needs no operator rights and changes nothing for anyone else. It shows a copy of a boss, or of something of his fight, that only you see, so the people who draw can look at it closely. `/enhanced_model list` shows what can be shown, and `/enhanced_model clear` takes the copy away.

* `/enhanced_model <thanos|thanos_phantom|thanos_sword|thanos_meteor|thanos_orb> <state> [tick]` shows the Mad Titan in a state (`idle`, and `idle_2` to `idle_5` for the stones he holds, `broken`, `barrier`, `veil`, `surge`, `stagger`, `falling`, `arriving`, `dying`) or at a tick of any of his moves (`sword_sweep`, `power_blast`, `infinity_beam`...), his illusion, his sword, a meteor or the Petrify Orb.
* `/enhanced_model <ultron|ultron_sentry|vibranium_key> <state> [tick]` shows Ultron in a state (`idle`, `volley`, `beams`, `dive`, `pull`, `call`, `punch`, `stagger`, `lift`, `transfer`, `death`, `awakening`, `hybrid`, `ultron3`...), a Sentry or the Vibranium Key.
* `/enhanced_model apocalypse <state> [tick]` shows En Sabah Nur in a state (`idle`, `awakening`, `tell_lance`, `tell_wave`, `tell_spikes`, `tell_boulders`, `tell_entomb`, `tell_mind_crush`, `tell_portal`, `backhand`, `tell_tornado`, `staggered`, `hover`, `escaping`, `kneeling`, `armour_fall`, `dying`, `withdrawing`, `true_<state>` for the frail body, `dying_fire`, and the shield's states `shield_up`, `shield_cracked`, `shield_forming`, `shield_reforming` and `shield_broken`); `sand_cocoon`, `sand_lance` and `sand_boulder` show his cocoon, a lance and a boulder.
* `/enhanced_ultron_fx <effect> [ticks]` plays one effect of the Ultron fight (a tell, a beam, a ring, the assembly...) a few blocks in front of you, on your side only; nothing is sent to the server. The effects of a body, such as `beams_fire`, need one shown with `/enhanced_model ultron`.

## Related

* Balance numbers are in the server config: [Server Configuration](Server-Configuration).
* Creative and Spectator players are never killed by the Mind Stone or the Hex wall, and can lift any Mjolnir without taking it over. See [FAQ](FAQ).

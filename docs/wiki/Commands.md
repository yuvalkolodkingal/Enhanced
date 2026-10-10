# Commands

All commands start with `/enhanced` and are for **operators** (permission level 2): server owners and players who can use `/gamemode`. Heroes are otherwise permanent, so these commands are also the only way to take a power away. (The one exception is `/enhanced_anim`, for looking at the animations: see below.)

| Command | Effect |
|---|---|
| `/enhanced power get [player]` | Shows a player's power. Without a player, it shows yours. |
| `/enhanced power set <players> <power> [look]` | Gives or removes a power, optionally straight into one of its looks. |
| `/enhanced look <players> <look>` | Changes the look of every given player whose hero has that look. |
| `/enhanced hex list` | Lists all Hexes in the current dimension (the owner, the position, the radius and how many players are allowed in). |
| `/enhanced hex clear` | Dissolves every Hex in the current dimension. |

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

The command suggests the names as you type. A player who is given a power awakens as that hero, with the title, the sound and the announcement to everyone, exactly as if they had earned it. If a player already has that power and you give a look, only the look changes.

## Looks

The `[look]` or `<look>` argument is the id of a look (see [Looks](Looks)):

| Hero | Look ids |
|---|---|
| Vision | `vision`, `white_vision` |
| Tommy | `halloween`, `tracksuit`, `speed`, `own` |
| Billy | `wiccan`, `teen`, `halloween`, `comics`, `own` |
| Loki | `loki`, `tva`, `god_of_stories`, `own` |
| Thor | `thor`, `ragnarok`, `love_and_thunder`, `mighty_thor`, `own` |

Magneto, the Scarlet Witch and Quicksilver have no looks to choose. `/enhanced look` skips players whose hero does not have the look, and it fails with "Unknown look" if no given player has it.

## Examples

| Command | What it does |
|---|---|
| `/enhanced power get Steve` | Tells you what Steve is. |
| `/enhanced power set Steve thor` | Makes Steve Thor. |
| `/enhanced power set @a loki tva` | Makes every player Loki, in the TVA Variant look. |
| `/enhanced power set Steve none` | Takes Steve's power away. |
| `/enhanced look Steve white_vision` | Puts Steve, a Vision, in the White Vision look. |
| `/enhanced hex list` | Lists the Hexes in your dimension. |
| `/enhanced hex clear` | Dissolves them all. |

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

## Related

* Balance numbers are in the server config: [Server Configuration](Server-Configuration).
* Creative and Spectator players are never killed by the Mind Stone or the Hex wall, and can lift any Mjolnir without taking it over. See [FAQ](FAQ).

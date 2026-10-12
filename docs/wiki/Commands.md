# Commands

All commands start with `/enhanced` and are for **operators** (permission level 2): server owners and players who can use `/gamemode`. Heroes are otherwise permanent, so these commands are also the only way to take a power away. (The exceptions are `/enhanced_anim` and `/enhanced_model`, for looking at the animations and at the bosses: see below.)

| Command | Effect |
|---|---|
| `/enhanced power get [player]` | Shows a player's power. Without a player, it shows yours. |
| `/enhanced power set <players> <power> [look]` | Gives or removes a power, optionally straight into one of its looks. |
| `/enhanced look <players> <look>` | Changes the look of every given player whose hero has that look. |
| `/enhanced hex list` | Lists all Hexes in the current dimension (the owner, the position, the radius and how many players are allowed in). |
| `/enhanced hex clear` | Dissolves every Hex in the current dimension. |
| `/enhanced boss start thanos [force]` | Calls the Mad Titan where you stand, as the Tesseract and Loki's Scepter do: a portal opens and six seconds later he lands 14 to 20 blocks from you. `force` skips the site check, the Overworld rule, the server's `summonEnabled` switch and the rest (Peaceful and one fight at a time still apply). See [Thanos](Thanos). |
| `/enhanced boss start ultron [force]` | Without `force` it is the ritual without the Scepter: you need the Ultron Program in your hand (it is used up), Ultron wakes on the block you look at, and every check is made. With `force` he wakes at your feet with no Program, and the checks of the ground are skipped (not the open sky, and never when he is switched off, in Peaceful or while another boss fight runs). See [Ultron](Ultron). |
| `/enhanced boss start apocalypse [force]` | With `force`, wakes En Sabah Nur at your feet, with no Capstone, and skips the checks of the ground, the sky and the need for a Scarlet Witch or a Phoenix (not Peaceful, not the switch that turns him off, not the one-fight-at-a-time rule). Without it the command is the ritual itself: it needs a Capstone in your hand and a block in sight, and every check. See [Apocalypse](Apocalypse). |
| `/enhanced boss stop` | Ends the running boss fight. A used Ultron Program or Capstone is given back, and the island of Sokovia is put back. A second `stop` while a boss is still leaving ends it at once. |
| `/enhanced boss list` | Lists the running boss fights. |
| `/enhanced boss cleanup <center> <radius>` | Removes every creature and projectile of a boss's kind within that radius, whether a fight owns it or not: Thanos, his sword, meteors, orbs and illusions; Ultron, the Sentries (a hero Ultron's too), his bolts and the Vibranium Key; Apocalypse, his lances, boulders and cocoons. It removes no blocks. |

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

The command suggests the names as you type. A player who is given a power awakens as that hero, with the title, the sound and the announcement to everyone, exactly as if they had earned it. If a player already has that power and you give a look, only the look changes.

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

Magneto, the Scarlet Witch and Quicksilver have no looks to choose. `/enhanced look` skips players whose hero does not have the look, and it fails with "Unknown look" if no given player has it.

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
| `/enhanced power set Steve none` | Takes Steve's power away. |
| `/enhanced look Steve white_vision` | Puts Steve, a Vision, in the White Vision look. |
| `/enhanced hex list` | Lists the Hexes in your dimension. |
| `/enhanced hex clear` | Dissolves them all. |
| `/enhanced boss start thanos` | Calls the Mad Titan where you stand, if the site would do for the ritual. |
| `/enhanced boss start ultron force` | Wakes Ultron where you stand, with no Program or Scepter. |
| `/enhanced boss start apocalypse force` | Wakes En Sabah Nur where you stand, with no Capstone and no one to break his shield. |
| `/enhanced boss cleanup ~ ~ ~ 40` | Removes whatever a boss left within 40 blocks of you: his creatures and his projectiles. |

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

## Related

* Balance numbers are in the server config: [Server Configuration](Server-Configuration).
* Creative and Spectator players are never killed by the Mind Stone or the Hex wall, and can lift any Mjolnir without taking it over. See [FAQ](FAQ).

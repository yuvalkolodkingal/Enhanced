# Commands

All commands start with `/enhanced` and are for **operators** (permission level 2): server owners and players who can use `/gamemode`. Heroes are otherwise permanent, so these commands are also the only way to take a power away.

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

## Related

* Balance numbers are in the server config: [Server Configuration](Server-Configuration).
* Creative and Spectator players are never killed by the Mind Stone or the Hex wall, and can lift any Mjolnir without taking it over. See [FAQ](FAQ).

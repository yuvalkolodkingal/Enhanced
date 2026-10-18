# Hero Creator

The **Hero Creator** is a Creative-mode item that opens a screen where you can **become any hero of the mod** at once, in any look, or **make a hero of your own**: a name, up to eight powers from a catalogue of **the thirty most common superpowers** of comics and films, each with its own **strength**, the keys they go on, your colours and **your own skin**. A hero you make can be saved to a file and loaded again, on any server.

Hebrew: [עברית](Hero-Creator-HE)

## Getting it and who can use it

* It is only in the **Enhanced creative tab**. It has no recipe and is never found in a chest.
* It works for a player in **Creative mode**, or for an **operator** (in any mode). A server can allow operators only (`custom_hero.operatorsOnly`) or switch the creator off (`custom_hero.enabled`). The server checks every button again, so a changed client cannot get round it.
* Right-click it to open the screen. Its first page has three big buttons: **Choose a Hero**, **Create My Hero** (**Edit My Hero** once you are one) and **Stop Being a Hero**.

## Choose a Hero

A card for every hero of the mod, with the hero's face drawn from its skin (the heroes of 3.3 are there too; scroll for more). Click a hero to see its looks, pick one and press **Become!**: you are that hero at once, with the title and the announcement, as if you had earned it. A server can turn this page off (`custom_hero.allowChooseExisting`). Between two heroes made or chosen you wait 2 seconds (`custom_hero.applyCooldownTicks`).

## Create My Hero

The editor has five tabs, and a paper-doll preview of your hero's skin and colours on every one.

| Tab | What you do there |
|---|---|
| **Name** | Your hero's name (up to 24 characters; colour codes are removed, and a hero with no name is called "My Hero"), the **main colour** (beams, particles, the HUD) and the **glow colour**, each with a palette and sliders for the colour and the light. |
| **Powers** | The catalogue of thirty powers, in five coloured groups (body, movement, energy, elements, mind). **Add** a power, choose its **option** (for example a beam, bolts or a burst), set its **strength** with the slider (1 to 10) and, if you like, give it **its own colour**. Next to the slider you see the real numbers (damage, range, cooldown...) change as you move it, and the points it costs. The points bar at the top shows what you have spent of the budget. |
| **Keys** | Five big buttons, **R G V B N**: click one to choose which of your powers goes on it. A power that is *always on* (Durability, Night Vision...) needs no key. |
| **Look** | **My Own Skin**, **A Hero's Skin** (any look of any hero, as a base), or **A Skin File**: drop a 64x64 PNG skin on the window, or press **Pick a File...**; **Open Skins Folder** opens `enhanced/skins/` in your game folder. **Paint a copy of this skin** writes the chosen hero skin into that folder so you can paint over it and pick it back. **Arms: Slim / Wide** chooses the arm width. |
| **Save** | **Become this hero!**, **Save to File** (also copies it), **Copy**, **Import from Clipboard**, and the list of your saved heroes in `enhanced/heroes/` (click one to load it; **Open Heroes Folder** opens the folder). You can also drop a hero `.json` file on the window. |

Your hero is saved with your player: it survives death and logging out, and if you become another hero for a while, **Edit My Hero** brings it back as you left it.

## The power budget

Every power costs **points for each point of its strength**: 1, 2 or 3 points a point (the strong ones cost 3). A hero has **60 points** to spend by default (`custom_hero.budget`), at most **8 powers** (`custom_hero.maxPowers`) and strength at most **10** (`custom_hero.maxStrength`). For example: Flight at 5 (10 points), Energy Blast at 6 (12), Durability at 5 (5), Healing Factor at 4 (8) and Lightning at 5 (10) spend 45 of the 60.

## The thirty powers

The research behind the list (which powers turn up most often in comics and films) counted the abilities of the most published Marvel and DC characters. Close variants are one power with options: Healing Factor and Regeneration, Phasing and Intangibility, Shapeshifting and Size Change, Energy and Power Absorption. The **first option is the default**. *Press*: once per press, then a cooldown. *Hold*: while the key is down. *On / off*: a switch. *Always on*: no key. Every number grows (or, for cooldowns and hunger, shrinks) evenly from strength 1 to strength 10; the table gives both ends. Damage is in points (2 points = one heart).

| # | Power (id) | Points a strength | Options | From strength 1 to 10 |
|---|---|---|---|---|
| 1 | Super Strength (`super_strength`) | 2 | Mighty Punch (press), Ground Slam (press), Super Leap (press) | Blows +1 to +8. Punch 6 to 18 damage, cooldown 3 to 1 s; Slam 4 to 12 damage round you, radius 3 to 7; Leap 1.0 to 2.6, cooldown 3 to 1 s |
| 2 | Durability (`durability`) | 1 | Armour Skin (always on), More Health (always on) | Armour 3 to 15 and toughness 1 to 8, knockback resistance 10% to 60%; or 4 to 30 more health |
| 3 | Super Speed (`super_speed`) | 2 | Super Run (on / off), Blitz Dash (press) | Run 25% to 160% faster; Dash 6 to 20 blocks, 3 to 10 damage to all in the way, cooldown 4 to 1.5 s |
| 4 | Flight (`flight`) | 2 | Hover, Jet, Wings (on / off) | Speed 0.06 to 0.13 (a ring of light, a trail of fire, or wings of light) |
| 5 | Healing Factor (`healing`) | 2 | Regeneration (always on), Healing Burst (press) | 0.25 to 2 health a second; or a burst of 4 to 20 health, cooldown 20 to 8 s |
| 6 | Energy Blast (`energy_blast`) | 2 | Beam (hold), Bolts (press), Burst (press) | Beam 1 to 4 a hit, 16 to 40 blocks; Bolts 3 to 12 damage, 20 to 48 blocks; Burst 4 to 14 damage round you, radius 3 to 8 |
| 7 | Super Senses (`super_senses`) | 1 | Night Vision (always on), Danger Sense (always on), X-Ray Vision (on / off) | Danger felt 12 to 40 blocks away; X-ray 12 to 48 blocks |
| 8 | Telepathy (`telepathy`) | 1 | Sense Minds (on / off), Read a Mind (press) | Minds glow through walls 12 to 48 blocks away; read a creature 12 to 40 blocks away |
| 9 | Teleportation (`teleport`) | 2 | Blink (press), Behind You (press) | Blink 8 to 48 blocks, cooldown 4 to 0.8 s; behind a creature 8 to 32 blocks away |
| 10 | Telekinesis (`telekinesis`) | 2 | Creatures (press), Everything (press, blocks too) | Reach 8 to 32 blocks, throws 0.6 to 1.6 times as hard |
| 11 | Wall-Crawling (`wall_crawl`) | 1 | Steady, Fast (on / off) | Climbs at 0.12 to 0.35 (Fast: half again) |
| 12 | Force Field (`force_field`) | 2 | Bubble (hold), Reflect (hold: shots go back) | Takes 10 to 80 damage before it breaks, lasts 5 to 20 s, cooldown 10 to 3 s |
| 13 | Phasing (`phasing`) | 3 | Through Walls, Ghost (on / off) | Hunger 0.5 to 0.1 a second |
| 14 | Animal Control (`animal_control`) | 1 | My Pack (press), Calm (press) | Radius 6 to 20, 5 to 30 s, cooldown 15 to 6 s |
| 15 | Mind Control (`mind_control`) | 3 | Ally (press), Calm (press) | Reach 8 to 24 blocks, 5 to 30 s, cooldown 30 to 10 s |
| 16 | Magic (`magic`) | 2 | Mystic Bolts (press), Healing Ward (press), Curse (press) | Bolts 3 to 11 damage; a ward of radius 3 to 8 for 5 to 15 s; a curse 5 to 20 s |
| 17 | Ice (`ice`) | 2 | Freeze Ray (press), Ice Path (on / off), Blizzard (press) | Ray 2 to 8 damage and frozen 2 to 10 s; path radius 1 to 5; blizzard 2 to 8 damage, radius 3 to 9 |
| 18 | Weather Control (`weather`) | 2 | Storm (press), Change the Weather (press) | Storm 4 to 12 damage a strike, radius 10 to 24, 5 to 20 s; weather cooldown 30 to 5 s |
| 19 | Lightning (`lightning`) | 2 | Strike (press), Chain Lightning (press) | Strike 5 to 16 damage, 20 to 48 blocks; chain 4 to 12 damage, 1 to 5 jumps |
| 20 | Shapeshifting (`shapeshift`) | 1 | Grow (on / off), Shrink (on / off), Creature (press) | Grow 1.25 to 3 times your size; shrink 0.75 to 0.25; take a creature's shape 8 to 24 blocks away |
| 21 | Invisibility (`invisibility`) | 2 | Full, Shimmer (on / off) | Hunger 0.6 to 0.1 a second (Full: even monsters lose you) |
| 22 | Absorption (`absorption`) | 2 | Energy (press), Powers (press) | Store 10 to 60 of fire, lightning and blasts and let it out (radius 3 to 8); or drain 3 to 10 damage and take a hero's powers for 3 to 12 s (PvP only) |
| 23 | Fire (`fire`) | 2 | Fireball (press), Flamethrower (hold), Fire Aura (on / off) | Fireball 3 to 10 damage, burns 3 to 10 s; flame 1 to 3 damage, 4 to 10 blocks; aura 1 to 3 damage, radius 2 to 5. Fire never burns you |
| 24 | Duplication (`duplication`) | 2 | Clones (press), Decoys (press) | 1 to 4 clones that fight, 8 to 30 s; or 1 to 5 decoys, 6 to 20 s |
| 25 | Water (`water`) | 1 | Water Jet (hold), Wave (press) | Jet pushes 0.4 to 1.6, 6 to 16 blocks; wave 2 to 8 damage, radius 3 to 8. You never drown |
| 26 | Time (`time`) | 3 | Slow Time (on / off), Stop Time (press), Rewind (press) | Slow radius 4 to 14; a creature stands still 2 to 10 s; back to where you were 3 to 10 s ago |
| 27 | Technopathy (`technopathy`) | 1 | Hack (press), Command Golems (press) | Doors, gates and levers flip within 3 to 12 blocks; golems fight for you 10 to 30 s |
| 28 | Sonic Scream (`sonic_scream`) | 2 | Scream (press), Shockwave (press) | Cone 3 to 12 damage, 6 to 20 blocks; ring 2 to 10 damage, radius 4 to 12 |
| 29 | Earth (`earth`) | 2 | Tremor (press), Spikes (press) | Tremor 3 to 12 damage, radius 3 to 10; spikes 4 to 14 damage in a line 6 to 20 blocks long |
| 30 | Wind (`wind`) | 1 | Gust (press), Updraft (press) | Gust pushes 0.8 to 3, 5 to 14 blocks; updraft lifts 0.7 to 2. Softer falls |

The powers reuse the mod's own: the Energy Blast beam is the beam of Billy and the Darkhold in your colours, Telekinesis is Wanda's, Phasing is Vision's, the Creature shape is Loki's, the Clones are Loki's duplicates that fight, Stop Time is Billy's Freeze, and Super Run gives you Quicksilver's trails. No power of a custom hero crosses a Hex wall or a Chaos Shield, area powers spare what the other heroes' area attacks spare (allies, and players when PvP is off), and teleports respect arenas, the Witches' Road and the Hex like every other teleport. Earth changes no block, and the Sonic Scream breaks glass only if the server allows it (`custom_hero.blockChanges`).

## Your skin, seen by everyone

A custom skin must be a **PNG of exactly 64x64 pixels**, at most **16 KB** (`custom_hero.maxSkinBytes`, up to 24 KB). The server checks it and **sends it to every player**, so everyone sees your hero as you drew it. A skin that is too big, not a PNG or the wrong size is refused, and your hero wears your own skin instead.

## Saving, sharing and loading: the JSON file

**Save to File** writes your hero to `enhanced/heroes/<name>.json` in your game folder and copies the same text to the clipboard. **Copy** only copies it. Paste it anywhere to share your hero; a friend loads it with **Import from Clipboard** or by putting the file in their own `enhanced/heroes/` folder. It works on any server: that server's own limits decide what is kept.

```json
{
  "format": "enhanced:custom_hero",
  "version": 1,
  "name": "Storm Girl",
  "color": "#3FA9F5",
  "glow": "#E0F4FF",
  "look": {"base": "thor/ragnarok", "slim": false},
  "powers": [
    {"id": "flight", "strength": 5, "option": "jet"},
    {"id": "energy_blast", "strength": 6, "option": "beam", "color": "#FF4040"},
    {"id": "durability", "strength": 5, "option": "armor"},
    {"id": "healing", "strength": 4, "option": "steady"},
    {"id": "lightning", "strength": 5, "option": "chain"}
  ],
  "keys": ["energy_blast", "flight", "lightning", "", ""]
}
```

| Field | Meaning |
|---|---|
| `format`, `version` | Always `enhanced:custom_hero` and `1`. A file from a newer version is read as far as it is understood ("some of it may be left out"). |
| `name` | The hero's name (up to 24 characters). |
| `color`, `glow` | The main and glow colours, as `#RRGGBB`. |
| `look` | `base` is `own` (your own skin), `skin` (the skin in the file) or a hero's look as `<hero>/<look>`, for example `thor/ragnarok` or `agatha/all_along` (Magneto, the Scarlet Witch and Quicksilver: `magneto/suit`). `slim` is the arm width. |
| `skin` | Only with `"base": "skin"`: the 64x64 PNG, written as base64. |
| `powers` | Each power: its `id` (the table above), its `strength` (1 to 10; a hand-written `"level": 1`, `2` or `3` reads as 3, 6 or 10; none at all reads as 5), its `option`, and an optional `color` of its own. |
| `keys` | Five power ids, for **R G V B N** in that order; `""` leaves a key empty. |

**What the server checks** when a hero is made, chosen or loaded: the name is cleaned; unknown powers, powers the server has switched off (`custom_hero.disabledPowers`) and repeated powers are left out; strengths are cut to the server's highest; an unknown option becomes the default; powers past the budget or the count are left out from the end; a key whose power is missing (or is always on) is cleared; a bad skin or an unknown hero look falls back to your own skin. You are told about each change ("Unknown power left out", "left out: the budget is 60 points", "strength set to"...). Unknown fields are ignored, so a hand-written file loads as far as it can.

## Playing your hero

* Your keys do what you put on them; each key has its own cooldown. The top-left panel shows your hero's name in your colours, each key's power, and an *Always on* line for the powers that need no key.
* Beams, particles and the HUD use your colours, or a power's own colour if you gave it one.
* A custom hero flies only with the Flight power (on its key, or double-tap **Space**).
* **Stop Being a Hero** takes your power away; your hero stays saved for later.
* An operator can also give it with `/enhanced power set <player> custom`: the player becomes the hero they last made (an empty hero if they never made one). See [Commands](Commands).

## For servers

Section `[custom_hero]`: switch the creator off, allow operators only, turn off Choose a Hero, change the budget, the number of powers, the highest strength, the skin size, the powers that are not allowed, the wait between heroes, and scale every custom power's damage, cooldowns and hunger (and the flight speed, the Force Field, the control times, the teleport reach and how long Power Absorption takes a hero's powers), whether custom powers may change blocks and whether Weather Control changes the world's weather. Every option is read when it is used; a hero already made is judged by the new limits the next time it is saved. See [Server Configuration](Server-Configuration#the-hero-creator).

## Related

[Becoming a Hero](Becoming-a-Hero) · [Looks](Looks) · [Controls](Controls) · [Items and Crafting](Items-and-Crafting#hero-creator)

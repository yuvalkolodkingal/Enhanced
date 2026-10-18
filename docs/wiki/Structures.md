# Structures

Three places of the films are built into the world in 3.3, each with the item of a hero inside: a **Hydra outpost**, where **Loki's Scepter** is kept; **Mjolnir's crater**, where the hammer lies where it fell from the sky; and the **Mutant School**, Xavier's School for Gifted Youngsters, with **Professor X's wheelchair** and **Cyclops's glasses**.

Hebrew: [עברית](Structures-HE)

## Finding them

* They are in the **Overworld** only, in the biomes listed below, and only on fairly **level, dry ground** (a site on a slope, or over water, is skipped). So they are rarer than their spacing alone says: in test worlds the nearest outpost was about 870 and 1,470 blocks from the spawn, and the nearest school about 2,000.
* **`/locate structure`** finds them (operators, or anyone in a world with cheats): `/locate structure enhanced:hydra_outpost`, `/locate structure enhanced:mjolnir_crater` and `/locate structure enhanced:mutant_school`.
* **Old worlds get them too**: they generate in every **new chunk** of a world made before 3.3. Explore land you have not been to yet.
* Outposts and craters keep at least 6 chunks away from a school.

| Structure | Where | Spacing / separation (chunks) |
|---|---|---|
| Hydra outpost | Taigas (every kind), snowy plains, grove, forest, birch forests, plains, meadow, windswept forest | 36 / 12 |
| Mjolnir's crater | Desert, badlands (every kind), savannas (every kind), plains, sunflower plains | 32 / 10 |
| Mutant School | Plains, sunflower plains, meadow | 40 / 14 |

## A Hydra outpost

A small Hydra base behind a fence of posts and iron bars, with a gate between two lanterns and signs that say "HYDRA / Restricted Area / Keep Out!". Round a yard of gravel and stone stand a concrete **bunker** with blackstone pillars and a red band, Hydra banners (black, with a red skull and tentacles), a **watchtower**, a **helipad**, a truck, crates and sandbags. Inside: a barracks with three beds, a command room with consoles and a map table, and on the roof a control room with a radio mast and a dish. A stair goes down to the cellar: a **lab** with brewing stands, a cauldron and a glass containment tube, and **the vault**: behind iron bars and an iron door, a chest on a pedestal lit blue from below.

* **The vault's chest always holds Loki's Scepter**: exactly one in every outpost, with some gold, iron, emeralds, ender pearls, bottles o' enchanting, a diamond or amethyst.
* The other chests hold Hydra's supplies: food, arrows, a crossbow, an iron sword and chainmail in the barracks; potions, brewing ingredients and books in the lab; iron, gunpowder, TNT, a compass, a spyglass and maps in the supply crates.
* **Hydra agents** guard it: pillagers spawn in the outpost, as at a pillager outpost.
* The outposts are now the **only place Loki's Scepter is found**: it is no longer in End City, stronghold, bastion, Ancient City or woodland mansion chests. It still has no recipe, and an operator can still give it with `/give`.

## Mjolnir's crater

A place hit from the sky, as in New Mexico in *Thor*: a bowl with a radius of 9 to 12 blocks, 4 to 6 deep, not quite round, with a floor of burnt rock (blackstone, basalt, tuff, coal, a few glowing magma blocks in the middle), gravel and tuff towards the rim, lumps of rubble and two campfires still smoking. A rim of thrown-up earth (sand in deserts and badlands, coarse dirt and gravel elsewhere) and scorched streaks and a scorched ring lie round it; in sand some of the scorch is fused into glass.

* **In the middle, on polished blackstone, lies Mjolnir**, handle up, as a dropped Mjolnir lies: a Mjolnir **nobody owns yet**.
* The usual rules apply: **the first worthy player who right-clicks it lifts it and claims it** (and becomes [Thor](Thor) if they have no power); everyone else gets "It won't budge". Who is worthy is on [Thor](Thor#the-worthy).
* Mjolnir can still be **forged** as before; the crater is a second way to get one.

## The Mutant School

Xavier's School for Gifted Youngsters: a grey stone mansion with mossy and cracked stone, slate roofs and chimneys, and a crenellated tower over the door with the school's banner (black, with a golden ring and an X). Its grounds have hedges and flowers, a gravel drive round a fountain, a gate with the sign "Xavier's School for Gifted Youngsters", a basketball court and a garden with a pond.

Inside are the entrance hall (double height, a red carpet, a grand staircase, a chandelier), a dining table, a classroom with desks and a blackboard, the library gallery, two dormitories, and:

* **The Professor's study** (in the west wing: bookshelves, a fireplace, a desk and a chess table). Its chest **always holds [Professor X's Wheelchair](Professor-X)**, with books and maps.
* **Scott Summers' room** (red and blue, with ruby-red windows). Its chest **always holds [Cyclops's Ruby-Quartz Glasses](Cyclops)**.
* **Cerebro**, below the house: white light panels and a walkway to the console, and a chest of redstone, quartz and a diamond.

## For servers and data packs

The structures are data: their sets (spacing, separation, salt), the biome tags that say where they may stand (`enhanced:has_structure/hydra_outpost`, `mjolnir_crater` and `mutant_school`), the buildings themselves (structure templates) and their loot tables (`enhanced:chests/hydra_outpost/vault`, `lab`, `barracks`, `supply`; `enhanced:chests/mutant_school/study`, `cyclops`, `cerebro`, `dormitory`, `library`, `kitchen`) are all in the mod's data, so a data pack can make them rarer or more common, move them to other biomes, rebuild them or change what is in the chests. They have no server config options. See [Server Configuration](Server-Configuration#structures).

## Related

[Loki](Loki) · [Thor](Thor) · [Professor X](Professor-X) · [Cyclops](Cyclops) · [Items and Crafting](Items-and-Crafting)

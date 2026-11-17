# Vision

The synthezoid with the Mind Stone in his forehead. In the films the same stone that sits in Loki's Scepter gives Vision life, so the mod makes him the second way to use it. His second look is **White Vision** (*WandaVision*, episode 9).

![Vision](images/008-vision.png)

## Becoming Vision

Craft a **Synthezoid Core**, a synthetic body waiting for a mind:

```
R N R
N S N
R N R

R = Block of Redstone
N = Netherite Ingot
S = Nether Star
```

Hold **Loki's Scepter** in one hand and the core in the other, and use the scepter. The Mind Stone moves into the body: the core is used up and you **become Vision** for good. Holding the core also protects you from the stone, exactly like the gene does for Wanda. (Loki's Scepter is kept in the vault of a [Hydra outpost](Structures#a-hydra-outpost): see [Items and Crafting](Items-and-Crafting#lokis-scepter).)

## The synthetic body

![Vision, intangible](images/012-vision-intangible.png)

* Vision's skin becomes the MCU Vision: red synthetic face and hands, the golden **Mind Stone in his forehead** (it glows in the dark), a green suit with gold trim and a yellow cape.
* His second look is **White Vision**: the same body drained of colour, chalk white with glowing pale cyan eyes and no Mind Stone, only an empty hexagonal socket of copper circuitry in his forehead. His beam is pale cyan-white instead of gold, and he phases in a white aura. Press **H** to switch (see [Looks](Looks)).
* **VisionQuest**: since 3.16.1 the VisionQuest suit is the Source Code's alone (below); it is no longer a look of its own.
* **Source Code** (3.12): earned from the Human Torch in Vision's mind, and taken in as White Vision. Change Look skips it until then. Since 3.16 his Mind Stone beam hits 1.5 times as hard in it, and a server can make him **Solar Jewel Vision** instead: see below.
* **Max health 30** (15 hearts) and **8 armor** from his vibranium body. Vibranium is metal: **Magneto can lift and throw you**, unless you are phased: then you slip out of his grip.
* He never gets hungry and never runs out of air.
* Vision is one of the worthy: he can claim a Mjolnir nobody owns yet, and fight with it, but he stays Vision. See [Thor](Thor).

## Abilities

![Vision's Mind Stone beam](images/009-vision-mind-stone-beam.png)

![Vision phasing through a wall](images/010-vision-phasing.png)

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (hold) | Mind Stone Beam | A pale gold beam from the stone in his forehead, 32 blocks long: 6 damage per hit (two hits per second). Hits creatures, players, the Ender Dragon and End Crystals. The stone sings its own chime-hum as the beam starts. |
| 2 | **G** (toggle) | Flight | Survival flight at 0.08, upright and calm. You can also double-tap **Space**. |
| 3 | **V** (toggle) | Phase | Become intangible: walk and float **through walls, floors and creatures**. Nothing can hurt you and projectiles pass through you, but you can't attack or fire the beam. You float instead of falling. You can't turn solid again inside a block (if you are only a little sunk into the floor, you rise out of it). Phasing doesn't get you through Wanda's Hex wall: it is a wall of reality, not of stone. |
| 4 | **B** (toggle) | Density | Become as hard as diamond: +12 armor, no knockback, +6 punch damage with heavy knockback. Too heavy to fly, 30% slower, and he falls faster. Phasing ends density, and you can't turn dense while phased. |
| 5 | **N** (toggle) | Human Disguise | Take on the human look from *WandaVision*: a 1950s sitcom husband in a shirt, tie and cardigan. Only the looks change. |
## On fire, in Ultimate Mode (3.17)

With [Ultimate Mode](Ultimate-Mode) on (and only then), Source Code Vision **burns** while he flies or uses a power, like the Human Torch: his **whole body** is a figure of molten red with bright cracks of fire running over every part of it, a sheath of flame hugging his silhouette, long tongues of flame and a trail of fire streaming behind him, embers and sparks, a crackle while he burns and a whoosh as he lights. It is only light: nothing is set on fire and he never burns (it is never the game's fire). In first person it stays faint and low, out of the view. With Ultimate Mode off he is the Source Code Vision of 3.16 and does not burn.

## Solar Jewel Vision (3.16)

Part of [Ultimate Mode](Ultimate-Mode) since 3.17 (off by default: `/enhanced ultimate on`, or `[ultimate] enabled = true` in the server settings; 3.16's `vision.solar_jewel.enabled` is gone, and a config that had it on turns Ultimate Mode on). Then, in his Source Code look (and in any look while his ultimate form lasts), Vision runs on the **Solar Jewel** in his brow, as in the comics, instead of the Mind Stone. The jewel drinks in sunlight and stores it: about ten seconds of open daylight fill it, shade gives a little, the night nothing (he runs on what he stored). It glows brighter the more it holds, and the HUD shows how full it is.

| Key | Power |
|---|---|
| **R** (hold) | **Solar Beam**: golden light from the jewel, a white-hot core with strands of sunlight twisting round it and a turning sunburst at his brow. It burns what it hits. |
| **Sneak + R** | **Jewel Overload**: he opens his arms to the sun, gold rings close in on his brow and sparks spiral into it, then everything he stored pours out at once in one great beam that strikes everything in its path and throws it back. It needs 40% of a full jewel, takes all of it, and leaves the jewel drained for 20 seconds. |
| **N** (hold) | **Infrared eye beams**: two thin beams of heat from his eyes that burn creatures and melt snow and ice. |
| **Sneak + N** | The Mind Palace, as before. |
| **G, V, B** | Flight, Phase and Density, as before (the synthezoid body's, not the stone's). |

The numbers are in the server config, `[vision.solar_jewel]`.

## Wanda's Hex holds him

![White Vision's beam](images/011-vision-white-vision.png)

Inside a standing Hex, Vision **can't leave it**, in either look, whether or not Wanda let him in: in *WandaVision* he starts to come apart at the edge of Westview. Walking, flying, phasing, portals and every teleport out are refused at the wall. He comes apart a little there (pieces flake off in orange and yellow, with a crackle), is thrown back inside and told "Wanda's Hex holds you: only she can let you go". It costs him 1 heart the first time and up to 4 hearts if he keeps pushing, and never kills him. White Vision doesn't come apart: he is only thrown back, unhurt. **To escape, hurt Wanda badly** (30% of her health or less) or defeat her: then her Hex can't hold him. He is also free when the Hex dissolves, when Wanda shrinks it past him or leaves the world, and in Creative or Spectator. Getting in needs Wanda's permission, as for any hero. Wanda's Hex also brings a Vision of its own to fight at her side. All of it is on [Scarlet Witch](Scarlet-Witch).
## The Infinity Stones (3.6)

Vision may hold the **Mind Stone** bare, and use it (the Mind Beam; Sneak: the Mind Command): it is his own. The other five are too strong for him, as for anyone who is not a god. See [Infinity Stones](Infinity-Stones).

## Numbers you can change

Vision's flight speed (0.08) and the Mind Stone Beam's damage (6) are the `vision.*` options. The beam's range is the same option as the Chaos Beam's, `scarlet_witch.chaosBeamRange`. See [Server Configuration](Server-Configuration).

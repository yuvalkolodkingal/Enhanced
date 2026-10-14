# Vision

The synthezoid with the Mind Stone in his forehead. In the films the same stone that sits in Loki's Scepter gives Vision life, so the mod makes him the second way to use it. His second look is **White Vision** (*WandaVision*, episode 9).

![Vision](images/vision.png)

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

Hold **Loki's Scepter** in one hand and the core in the other, and use the scepter. The Mind Stone moves into the body: the core is used up and you **become Vision** for good. Holding the core also protects you from the stone, exactly like the gene does for Wanda. (Loki's Scepter is found in chests: see [Items and Crafting](Items-and-Crafting).)

## The synthetic body

* Vision's skin becomes the MCU Vision: red synthetic face and hands, the golden **Mind Stone in his forehead** (it glows in the dark), a green suit with gold trim and a yellow cape.
* His second look is **White Vision**: the same body drained of colour, chalk white with glowing pale cyan eyes and no Mind Stone, only an empty hexagonal socket of copper circuitry in his forehead. His beam is pale cyan-white instead of gold, and he phases in a white aura. Press **H** to switch (see [Looks](Looks)).
* **Max health 30** (15 hearts) and **8 armor** from his vibranium body. Vibranium is metal: **Magneto can lift and throw you**, unless you are phased: then you slip out of his grip.
* He never gets hungry and never runs out of air.
* Vision is one of the worthy: he can claim a Mjolnir nobody owns yet, and fight with it, but he stays Vision. See [Thor](Thor).

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (hold) | Mind Stone Beam | A pale gold beam from the stone in his forehead, 32 blocks long: 6 damage per hit (two hits per second). Hits creatures, players, the Ender Dragon and End Crystals. The stone sings its own chime-hum as the beam starts. |
| 2 | **G** (toggle) | Flight | Survival flight at 0.08, upright and calm. You can also double-tap **Space**. |
| 3 | **V** (toggle) | Phase | Become intangible: walk and float **through walls, floors and creatures**. Nothing can hurt you and projectiles pass through you, but you can't attack or fire the beam. You float instead of falling. You can't turn solid again inside a block (if you are only a little sunk into the floor, you rise out of it). Phasing doesn't get you through Wanda's Hex wall: it is a wall of reality, not of stone. |
| 4 | **B** (toggle) | Density | Become as hard as diamond: +12 armor, no knockback, +6 punch damage with heavy knockback. Too heavy to fly, 30% slower, and he falls faster. Phasing ends density, and you can't turn dense while phased. |
| 5 | **N** (toggle) | Human Disguise | Take on the human look from *WandaVision*: a 1950s sitcom husband in a shirt, tie and cardigan. Only the looks change. |

![The Mind Stone beam](images/mind-stone-beam.png)

![Vision phasing through a wall](images/vision-phase.png)

![Vision in his WandaVision human disguise](images/vision-disguise.png)

## Wanda's Hex holds him

Inside a standing Hex, Vision **can't leave it**, in either look, whether or not Wanda let him in: in *WandaVision* he starts to come apart at the edge of Westview. Walking, flying, phasing, portals and every teleport out are refused at the wall. He comes apart a little there (pieces flake off in orange and yellow, with a crackle), is thrown back inside and told "Wanda's Hex holds you: only she can let you go". It costs him 1 heart the first time and up to 4 hearts if he keeps pushing, and never kills him. White Vision doesn't come apart: he is only thrown back, unhurt. **To escape, hurt Wanda badly** (30% of her health or less) or defeat her: then her Hex can't hold him. He is also free when the Hex dissolves, when Wanda shrinks it past him or leaves the world, and in Creative or Spectator. Getting in needs Wanda's permission, as for any hero. Wanda's Hex also brings a Vision of its own to fight at her side. All of it is on [Scarlet Witch](Scarlet-Witch).

![Vision coming apart at the wall of Wanda's Hex](images/vision-held.png)

## Numbers you can change

Vision's flight speed (0.08) and the Mind Stone Beam's damage (6) are the `vision.*` options. The beam's range is the same option as the Chaos Beam's, `scarlet_witch.chaosBeamRange`. See [Server Configuration](Server-Configuration).

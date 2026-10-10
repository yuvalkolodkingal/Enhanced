# Scarlet Witch

Wanda Maximoff. Chaos magic in a crimson tangle of energy, with hot pink highlights and eyes that glow red: a beam, a shield, telekinesis, fast flight, and the Hex, a giant dome that only she controls.

![The Scarlet Witch](images/scarlet-witch-sheet.png)

## Becoming the Scarlet Witch

**Step 1: find the gene.** The **Scarlet Witch Gene** is a very rare item. On its own it does nothing at all. Where to find it is on [Items and Crafting](Items-and-Crafting).

**Step 2: touch the Mind Stone.** The Mind Stone sits inside **Loki's Scepter** (the blue gem in its golden blade), a relic found in chests (also on [Items and Crafting](Items-and-Crafting)). To touch the stone, hold the scepter and **right-click**:

* **Holding the gene** in your other hand: the gene and the stone's cosmic energy fuse. The gene is used up and you awaken as the Scarlet Witch, with all of Wanda's powers. Wanda's Crown appears in your inventory.
* **Not holding the gene:** the Mind Stone's energy tears you apart. You **explode and die instantly** (the explosion does not break blocks).
* **Holding a Synthezoid Core** instead: you become [Vision](Vision).
* If you already are the Scarlet Witch or Vision, touching the stone is safe. If you have a different power, the gene or core can't bond: nothing happens, the item is not used up, and holding it still protects you from the stone.

**The Tesseract works too.** Hold the gene in one hand and use the **Tesseract** with the other, instead of the scepter: you awaken as the Scarlet Witch in exactly the same way (the gene is used up, Wanda's Crown appears, everyone on the server gets the message). The Tesseract is not used up and you are not taken anywhere. Without a gene it makes you [Loki](Loki), and a hero who uses it is simply taken to the block they look at.

The scepter is also a strong melee weapon (8 attack damage, never breaks). In the films it takes over the mind of whoever it touches; that would be too strong here, so a hit only **confuses**: a player gets **Nausea** for 8 seconds and **Slowness I** for 3, and any other creature is **Confused** for 6 seconds. A confused creature forgets its target, can't pick a new one and stumbles about aimlessly, with small blue-green sparks swirling round its head. The Ender Dragon, the Wither and the Warden are immune. See [Loki](Loki).

![Touching the Mind Stone with the gene](images/mind-stone-awakening.png)

There is a second way to become a witch's child: the [Witches' Road Door](Billy-and-the-Witches-Road) turns you into Tommy or Billy, and the Scarlet Witch herself can travel the Road.

## Wanda's Crown: the transformation

The crown is given when you awaken. If it is lost, craft a new one:

```
G R G
G . G

G = Gold Ingot
R = Block of Redstone
```

When the Scarlet Witch wears the crown she **transforms**:

* Her skin becomes **the Scarlet Witch's suit**: copper hair, glowing red eyes, the burgundy suit and her crown, with a burgundy cape. The skin is the community skin ["Scarlet Witch"](https://www.minecraftskins.com/skin/18465369/scarlet-witch/) from minecraftskins.com; it draws its own crown, so the crown model is hidden while she is transformed.
* **Max health 40** (20 hearts instead of 10).
* **Armor equal to a full Diamond set** (20 armor, 8 toughness).
* The crown never breaks.

On anyone else the crown is just a crown.

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (hold) | Chaos Energy Beam | A glowing, crackling red beam from Wanda's hand. |
| 2 | **G** (toggle) | Enhanced Flight | Fast, smooth flight with red energy around both hands. |
| 3 | **V** | Telekinesis | Lift creatures, dropped items **or blocks**, move them with your gaze, then launch them. |
| 4 | **B** (hold) | Chaos Shield | A red glowing sphere that stops projectiles and softens explosions. It takes time to form, wears out and can shatter. |
| 5 | **N** | The Hex | A giant red energy dome that only Wanda controls. |

### 1. Chaos Energy Beam

* **Look:** a bright, shimmering crimson beam with hot pink wisps, a near-white core, a crackling outer glow and sparks where it hits. While she wields chaos magic (beam, shield, telekinesis) her eyes glow red, as in the WandaVision finale.
* **Range:** 32 blocks. The beam stops at the first creature or block.
* **Damage:** **2.5 hearts (5 HP) per hit** for as long as the beam stays on the target. Minecraft lets a creature be hurt at most twice per second, so a target takes about 5 hearts per second. The Ender Dragon (100 hearts) survives about 20 seconds of beam if every hit lands on its head, and roughly twice as long on its body or wings: strong, but not a one-shot.
* **Debuff:** every creature it hits gets **Nausea** for 5 seconds. (Magneto's helmet blocks this, but not the damage.)

![The Chaos Beam](images/chaos-beam.png)

### 2. Enhanced Flight

* Survival flight. Fast (0.09, almost twice Creative's speed) but always controllable. You can also double-tap **Space**.
* While she flies, two glowing red energy auras swirl around Wanda's hands, trailing red sparks.

![Red energy around Wanda's hands in flight](images/scarlet-witch-flight.png)

### 3. Telekinesis

* **Targets:** any monster, animal, villager, player or dropped item within 32 blocks. Bosses (Ender Dragon, Wither, Warden, Elder Guardian) are too powerful to lift.
* **Control:** while held, the target floats where Wanda looks. Press **V** again to **launch** it at high speed. When it crashes into a wall or the floor it takes heavy impact damage, and it hurts any creature it slams into.
* **Blocks:** look at a block (up to 24 blocks away) and press **V** to tear it out of the ground. Throw it at enemies like a shell (8+ damage). It shatters on impact and drops its normal item.
  * Liftable: any block with hardness 5 or less (dirt, stone, logs, ores, iron blocks and so on) that has no inventory or other block data and is not in the `enhanced:telekinesis_immovable` tag. Bedrock, obsidian, portals, chests and spawners can't be lifted.
  * Lifting respects spawn protection, Adventure mode and claim mods.
* **Sneak + V** sets the target down gently. A held block placed this way lands like falling sand.

### 4. Chaos Shield

* Hold **B**. A red, glowing sphere of chaos energy grows round Wanda. It is a commitment, not a reflex:
  * **It takes 3/4 of a second to form.** It grows from her hands to its full size, and it **blocks nothing until it is complete.**
  * **It holds for at most 8 seconds,** counted from when it is complete. Then it drops.
  * **It has 30 points of strength.** Everything it stops wears it down by that much. At 0 it **shatters**, in a burst of red shards and with a glassy sound.
  * **It has to recharge** before she can raise it again: **6 seconds** after it drops (when she lets go of B, or when she is too hungry), **15 seconds** if it shattered or was held to its limit. While it recharges, the HUD counts down and B does nothing.
* **Blocks every projectile** it meets while it is up: arrows, tridents, Ghast and Blaze fireballs, Wither skulls, shulker bullets, potions and snowballs. Projectiles disintegrate on the sphere. Each one costs the shield what it would have done to Wanda: an arrow its damage at its speed (about 4 to 6 points), a trident 8, a fireball 6, a thrown Mjolnir, Stormbreaker, dagger or iron shard its own damage, and 2 for anything else. The projectile that breaks the shield is still stopped. A thrown Mjolnir or Stormbreaker is one of a kind, so it is not destroyed: the shield turns it round and it flies home to Thor.
* **Explosion damage is reduced by 75%.** The 75% the shield takes away is what wears it down. If less strength is left than that, the shield shatters and the rest gets through. Melee blows are not stopped.
* **Cost:** it burns hunger quickly while held (about 1 food point every 2 seconds), while it forms too. When the hunger bar is empty, the shield collapses and has to recharge.
* **HUD:** while it forms it shows "forming". Once it is up it shows what is left of its strength (green, then yellow, then red) and the seconds it can still be held, and after it drops it shows the cooldown. The sphere flickers as it weakens or when its time is nearly up. Her arms gather across her chest while it forms and are thrown open as it completes (let go of earlier, she only lowers them).

![The Chaos Shield](images/chaos-shield.png)

### 5. The Hex

Press **N** to raise the Hex: a **giant red energy dome**, radius 32 blocks (adjustable from 8 to 96), centred on where Wanda stands. It rises over a couple of seconds. From outside it looks like the Westview Hex: a raspberry-red wall of glowing TV-static bricks and scanlines that tints the sky behind it.

![The Hex](images/hex.png)

* One Hex per Wanda. It is saved with the world and collapses if Wanda dies.
* **Only Wanda decides who may enter or leave.** Anyone she has not allowed meets the price of the wall (below).

#### Hex Control

Press **N** again, while you have a Hex, to open the **Hex Control** screen:

* Allow or block each online player.
* Allow or block creatures (mobs and animals).
* Make the Hex bigger or smaller (radius 8 to 96 by default).
* Dissolve the Hex (or press **Sneak + N**).

The screen reminds you that anyone you have not allowed has a 95% chance to die crossing the wall.

![Wanda's Hex Control screen](images/hex-control.png)

#### The price of the Hex wall

When any creature or player **without powers** crosses the wall without Wanda's permission:

* **95%: instant death** ("was rewritten by the Hex"). Armor, Totems of Undying and effects don't help.
* **5%: they survive.** The rare survivor simply makes it through, and nothing else happens to them.

That 95% is the `scarlet_witch.hexDeathChance` option (see [Server Configuration](Server-Configuration)).

Other rules of the wall:

* **Crossing includes teleporting.** Ender Pearls, Chorus Fruit, Endermen, Loki's Vanish and Tesseract, and Thor's Bifrost (arriving inside a Hex counts as crossing its wall) all face the wall's verdict: the 95% / 5% roll for someone without powers, the bounce for a hero. Tommy can't vibrate through a Hex he is not allowed into.
* Anyone **with powers** (a hero) and bosses (the Ender Dragon, the Wither, the Warden and the Elder Guardian) can't be killed by the wall. They are thrown back as if they hit glass, unless Wanda allowed them in. Phasing doesn't get Vision through: it is a wall of reality, not of stone.
* Creative and Spectator players pass through.
* Pets owned by an allowed player pass through safely.

## Numbers you can change

Wanda's flight speed, the beam's damage, range and Nausea, the shield's cost, wind-up, strength, time limit, cooldowns and explosion factor, how far she can lift blocks and how hard they may be, and the Hex's default, smallest and largest radius and its death chance are the `scarlet_witch.*` options on [Server Configuration](Server-Configuration).

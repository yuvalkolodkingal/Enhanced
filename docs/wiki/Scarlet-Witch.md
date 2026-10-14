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
| 4 | **B** (hold) | Chaos Shield | A red glowing sphere that stops projectiles and melee blows and softens explosions. It takes time to form, wears out and can shatter. |
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

* **Targets:** any monster, animal, villager, player, dropped item, minecart or falling block within 32 blocks. **Ultron, Thanos and Apocalypse, and the Wither, the Warden and the Elder Guardian, are held as bosses**: for 3 seconds, then they break free, and nobody can take hold of them again for 12 seconds (see each boss's page). The Ender Dragon is too vast to hold. Apocalypse's shield turns every other power away, but not hers: **mind power gets through**.
* **Control:** while held, the target floats where Wanda looks. Press **V** again to **launch** it at high speed. When it crashes into a wall or the floor it takes heavy impact damage, and it hurts any creature it slams into.
* **Blocks:** when there is nothing to take, **V** tears out the block you look at (up to 24 blocks away). **Sneak + V**, when V isn't holding anything, tears out the block you look at even past a creature, an item or a boss; not in the second after you let go of something. Throw it at enemies like a shell (8+ damage). It shatters on impact and drops its normal item.
  * Liftable: any block with hardness 5 or less (dirt, stone, logs, ores, iron blocks and so on) that has no inventory or other block data and is not in the `enhanced:telekinesis_immovable` tag. Bedrock, obsidian, portals, chests and spawners can't be lifted.
  * Lifting respects spawn protection, Adventure mode and claim mods.
* **Sneak + V** while you hold something sets it down gently. A held block placed this way **becomes a block again where it lands**, under the same rules as building (spawn protection, claims, Adventure mode, never where someone stands, never for a holder who left the game). If it can't be placed, it drops its item (a glass block falls as glass).

#### Bosses and borrowed blocks

In the arena of a boss fight (Ultron's and Apocalypse's, which are protected), telekinesis never breaks the world: the block is **borrowed**. The hole **mends after 5 seconds**; a borrowed block you set down **crumbles 5 seconds later** (at most 3 of yours stand at once, never on top of each other or right next to anyone: no building in a boss fight); it never drops anything, and everything goes back when the fight ends. Sand or gravel that hangs over a gap is not lent, nor a block with a painting, a frame or a lead on it. The island of Sokovia is borrowed from too, but the ground under it lends nothing. In Thanos's arena, which is not protected, the blocks are real.

A held boss does nothing and can't be carried out of his arena. A boss you throw crashes where he comes down (dust, an anvil's clang) and takes the impact through his own limits. **Ultron's heart can be torn out also while you hold him** (see [Ultron](Ultron)).

### 4. Chaos Shield

* Hold **B**. A red, glowing sphere of chaos energy grows round Wanda. It is a commitment, not a reflex:
  * **It takes 3/4 of a second to form.** It grows from her hands to its full size, and it **blocks nothing until it is complete.**
  * **It holds for at most 8 seconds,** counted from when it is complete. Then it drops.
  * **It has 30 points of strength.** Everything it stops wears it down by that much. At 0 it **shatters**, in a burst of red shards and with a glassy sound.
  * **It has to recharge** before she can raise it again: **6 seconds** after it drops (when she lets go of B, or when she is too hungry), **15 seconds** if it shattered or was held to its limit. While it recharges, the HUD counts down and B does nothing.
* **Blocks every projectile** it meets while it is up: arrows, tridents, Ghast and Blaze fireballs, Wither skulls, shulker bullets, potions and snowballs. Projectiles disintegrate on the sphere. Each one costs the shield what it would have done to Wanda: an arrow its damage at its speed (about 4 to 6 points), a trident 8, a fireball 6, a thrown Mjolnir, Stormbreaker, dagger or iron shard its own damage, and 2 for anything else. The projectile that breaks the shield is still stopped. A thrown Mjolnir or Stormbreaker is one of a kind, so it is not destroyed: the shield turns it round and it flies home to Thor. So does a fragment of a shattered Mjolnir that is sent at her (the shield pays what its strike would have done, 6), while a fragment already flying home passes through.
* **Stops Thor's lightning beam.** The sphere is solid to a beam as it is to an arrow: the beam ends on its surface (that is where it is drawn to), Wanda is not touched, nothing beyond the sphere is hit (and no lightning arcs on from it), and the shield pays the beam's damage for every hit: 2 bare-handed, 4 with Mjolnir or Stormbreaker, 6 with the God of Thunder on top. The beam that breaks the shield is stopped too. Lightning Thor sends at her from inside the sphere, or that leaps from a blow of his, is stopped and paid for in the same way. A shield that is still forming stops nothing, and on a server with PvP off the beam costs the shield nothing.
* **Stops melee blows too.** Close in is no way round it: a swung sword, a fist, a zombie's or a Warden's swipe, a spider's bite, a Ravager's charge, a bee's sting and Quicksilver's blitz never touch Wanda. The sphere takes the blow like an arrow: it costs the shield what it would have done to her (a diamond sword 7, a Warden's swipe 30; the blow that breaks the shield is stopped too), red sparks fly where it struck, the sphere rings, and the attacker is thrown back out of the dome (a creature that can't be knocked back, like the Warden, stays, and whoever Mjolnir pins is held where it is). A shield that is still forming stops nothing. Thor's blows are stopped like anyone's, and the God of Thunder's lightning doesn't leap from them. On a server with PvP off a player's blow can't hurt her anyway, so it costs the shield nothing. Her own blows and her beam still work out of the dome, and what is not a blow still gets through: falling, hunger, fire, lava, drowning, the void, `/kill`, magic and potions, a Guardian's beam, thorns and a Warden's sonic boom.
* **Explosion damage is reduced by 75%.** The 75% the shield takes away is what wears it down. If less strength is left than that, the shield shatters and the rest gets through.
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
* Tell [your Vision](#the-vision-of-the-hex) to follow you or to hold his post.
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
* **Vision can't leave a standing Hex**, whether or not you let him in: see below.

#### Vision can't leave your Hex

You are in control of the Hex, and in *WandaVision* Vision starts to come apart at the edge of Westview. So a player with Vision's power (White Vision too) who is inside a standing Hex **can't leave it**. Walking, flying, phasing, portals and every teleport out (Ender Pearls, Chorus Fruit, the Bifrost, Loki's Tesseract, the Witches' Road, Tommy's Vibrate) are refused at the wall, even when you let him in.

* At the wall Vision **comes apart**: pieces of him flake off in orange and yellow with a crackling sound, he is thrown back inside, and he is told "Wanda's Hex holds you: only she can let you go".
* It hurts him a little: **1 heart** the first time, and 1 heart more for every push within 3 seconds of the last, up to **4 hearts**. However hard he leans on the wall it bites at most twice a second. It never kills him (the last half heart is never taken), and it starts over after 3 seconds without pushing. These are `scarlet_witch.hexHoldDamage` and `scarlet_witch.hexHoldDamageMax`.
* **White Vision doesn't come apart**: the wall just throws him back, like glass, and doesn't hurt him. He is held in all the same.
* **He can escape by hurting you.** When you are **badly hurt** (30% of your health or less, `scarlet_witch.hexHoldReleaseHealth`) your Hex can no longer hold him and he can walk out, allowed in or not; if he **kills** you, your Hex dissolves and he is free anyway. With you gone from the world (logged out) nobody holds him either.
* **He is also free** when the Hex dissolves, when you shrink it past him (the wall then runs inside him, and he is not dragged back), and if he is in Creative or Spectator.
* Getting **into** a Hex keeps the old rules, for both looks: your permission lets him in, and without it he is thrown back like any hero.

#### The Vision of the Hex

*"When Wanda raises the Hex, Vision appears and attacks whoever she tells him to."* When you raise a Hex, a **Vision appears beside you**, as if you brought him back to life inside it.

* **Look.** He is drawn like Vision: Vision's skin and yellow cape, and the Mind Stone glowing in his forehead (brighter while he fires). He floats and flies, and has poses of his own for hovering, flying (leaning into it, arms swept back, cape streaming), standing guard and punching.
* **His place.** He follows you inside the Hex and **can never leave it**: the wall holds him exactly as it holds Vision. There is one per Hex, and he is there as long as it stands.
* **He protects you** like a tamed wolf. He goes for whatever attacks you, whatever you attack, whatever hits him, and whatever you point him at (below). Up close he fights with **dense punches**: 12 damage (6 hearts), about one a second. From a way off he fires a short **Mind Stone beam**: 4 damage a hit, two hits a second, 16 blocks. He has 80 health (40 hearts; an Iron Golem has 100) and 8 armor.
* **He never harms** you, players you allowed into the Hex, the pets of either, other Visions, or Creative and Spectator players, and no player at all on a server with PvP off. Your Chaos Beam and blows pass him by, and no telekinesis lifts him, except Magneto's: he is vibranium. A Hex still holds him in, and with PvP off no throw or fall hurts him.
* **Orders.** The **Z** key, *Vision, That One!* (it can be changed under *Options, Controls, Key Binds, Enhanced Powers*):
  * **Look at a creature and press Z**: he goes for that one, whatever else he is doing.
  * **Look at nothing and press Z**: he comes back to your side.
  * **Sneak + Z**: he holds his post where he is, or follows you again. The Hex Control screen has the same choice as a button, *Vision: follows you / holds his post*, and the Hex remembers it.
* **If he falls** he fades to nothing, and you **rebuild him 60 seconds later** while the Hex stands. He fades away when the Hex dissolves, and when you leave (log out or change dimension); your Hex raises him again when you are back. Nothing of him is saved.

![The Vision of the Hex](images/hex-vision.png)

## Build Magic

Wanda can **build with her magic**: press **Y** and click two corners to fill, empty, change or paint them, double-click a whole tree or house, copy a castle and stamp it somewhere else, raise hills. **Her magic has no limit** (only the server's safety keeps the game running smoothly), she points as far as 256 blocks, **and inside her own Hex her spells go twice as fast.** **Ctrl + Z** takes it all back, even tomorrow. And when she stands near Billy, his spells cost half. See [Build Magic](Build-Magic).

## Critical against Apocalypse

Her mind power hits the boss Apocalypse **critically**: the Chaos Beam, a telekinetic slam, a borrowed block she throws, all are twice as strong on his shield and, when his shield is down, on his body. Everyone else hits him for much less then. See [Apocalypse](Apocalypse#he-never-heals-wanda-and-the-phoenix-hit-critically).

## Numbers you can change

Wanda's flight speed, the beam's damage, range and Nausea, the shield's cost, wind-up, strength, time limit, cooldowns and explosion factor, how far she can lift blocks and how hard they may be (Wanda's and the Phoenix's; Billy and Magneto have their own), and the Hex's default, smallest and largest radius, its death chance and how hard it bites a Vision who pushes against it are the `scarlet_witch.*` options on [Server Configuration](Server-Configuration). The Vision of the Hex (his health, punch and beam, how long you need to rebuild him, and turning him off) is the `hex_vision.*` options.

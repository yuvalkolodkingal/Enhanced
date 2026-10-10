# Magneto

The Master of Magnetism. He lifts anything made of metal, fires a machine-gun stream of iron shrapnel, strips his enemies of their armor and weapons, and catches arrows in mid-air. In the X-Men films his power has no glow at all: metal just moves, and so it does here.

![Magneto](images/magneto.png)

## Becoming Magneto

**Magneto's Helmet** is crafted from exactly 5 Netherite Blocks in the normal helmet shape:

```
N N N
N . N

N = Netherite Block
```

Equipping the helmet for the first time permanently unlocks all of Magneto's abilities, if you have no other power. While Magneto wears the helmet, his whole skin turns into **Magneto's classic suit**: a red suit with purple gloves, boots and cape.

## The helmet

While the helmet is worn by Magneto:

* **Armor:** the helmet alone gives the protection of a full Netherite set: 20 armor, 12 armor toughness and 40% knockback resistance.
* **Infinite durability:** the helmet never takes damage and never breaks.
* **Mental immunity:** 100% immune to **Nausea** (the dizziness from Wanda's Chaos Beam, pufferfish and so on). Nausea he already has is removed the moment the helmet goes on.
* **No damage immunity:** Magneto still takes the full physical damage from the Chaos Beam and all of Wanda's other attacks. Only the Nausea is blocked.

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Metal Telekinesis | Look at a target up to 32 blocks away and press **R** to lift it. While held, it floats where you look. Press **R** again to **launch** it, or **Sneak + R** to set it down gently. |
| 2 | **G** (hold) | Iron Shrapnel Storm | Hold **G** to fire a machine-gun stream of iron shrapnel (10 shards per second), paid for with iron from your inventory. |
| 3 | **V** | Armor & Weapon Strip | Look at an enemy and press **V**. Every metal weapon and armor piece is ripped off and drops to the ground. |
| 4 | **B** (toggle) | Magnetic Shield | An invisible magnetic field that catches metal projectiles. |
| 5 | **N** (toggle) | Magnetic Flight | Smooth, steady flight. |

### 1. Metal Telekinesis

* **Valid targets:** any mob or player wearing at least one piece of **Iron, Chainmail or Gold armor**, and **Iron Golems**. Minecarts and dropped metal items count too, because they are made of metal.
* **Iron Golem control:** Magneto can lift any Iron Golem and throw it at enemies. A thrown golem hits like a wrecking ball (up to 15 damage to whatever it slams into), takes no damage itself, never turns on Magneto and starts fighting the monster it hit.
* **Launch:** launched targets fly at high speed (2.6 blocks per tick by default). When they slam into a wall or the floor they take heavy impact damage (up to 16) and hurt any creature they crash into (up to 6 damage).
* One target at a time. The hold breaks automatically if the target dies, leaves the dimension or gets more than 48 blocks away. A held *player* is released after 30 seconds, so nobody is trapped forever.

![Magneto lifting an Iron Golem](images/magneto-telekinesis.png)

### 2. Iron Shrapnel Storm

* Needs iron in the inventory. Shards are paid for from an internal *iron charge*. When the charge runs out, one more iron item is melted into shards. Plain iron is used first, smallest first, and tools and armor only when no plain iron is left:

  | Item | Shards |
  |---|---|
  | Iron Nugget | 1 |
  | Iron Ingot, Raw Iron | 9 |
  | Block of Iron, Block of Raw Iron | 81 |
  | Iron tools, weapons and armor (and other items tagged `enhanced:shrapnel_ammo`) | 18 |

  Enchanted items are never melted, so a Mending pickaxe is safe.
* Each shard does **4 damage**, **pierces through up to 3 targets** and **ignores the normal hit cooldown**, so the full stream deals about 40 damage per second before armor.
* Creative players need no iron. The HUD shows how many shards you have stored.

### 3. Armor & Weapon Strip

* Works on players, zombies, skeletons, piglins and every other creature, up to 32 blocks away.
* Rips off every metal item in a hand or armor slot (iron, chainmail and gold armor, iron and gold weapons and tools, shears, flint & steel, metal horse armor). The items fly out and land on the ground as normal items.
* Cooldown: 3 seconds.

![Ripping a zombie's iron gear off](images/magneto-disarm.png)

### 4. Magnetic Shield

* Toggle on and off. While it is on it works automatically.
* **Metal projectiles** (arrows, spectral arrows, crossbow bolts, tridents and other Magnetos' shrapnel) coming at Magneto are **caught in mid-air** about 4.5 blocks out, like the bullets and missiles he stops in the X-Men films. Each one hangs there, trembling, for almost a second, while Magneto's hand points at it. Then it either **drops to the floor** or **flies straight back at whoever shot it** (50/50 by default).
* You can see the field while it is on: faint, colourless field lines loop around him like iron filings round a magnet, and every caught projectile shimmers with a ring and a line of force to his hand.
* A metal projectile that would hit Magneto while the shield is on never deals damage.
* It does not stop fireballs, potions or snowballs, because they are not metal.

### 5. Magnetic Flight

* Survival flight, steady and fully controllable. You can also double-tap **Space**.
* Flying speed is **0.075** (Creative is 0.05), a bit slower than Wanda's 0.09.

## Numbers you can change

Server owners can change Magneto's flight speed, telekinesis range, throw speed, shrapnel damage, piercing and fire rate, the strip's range and cooldown, and the shield's reflect chance. They are the `magneto.*` options on [Server Configuration](Server-Configuration). Which items count as metal is decided by datapack tags, described there too.

![Magneto's abilities on the HUD](images/magneto-back.png)

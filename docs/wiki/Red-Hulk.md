# Red Hulk

Thaddeus "Thunderbolt" Ross as the Red Hulk of *Captain America: Brave New World* (3.8): one and a half times your size, red skin that glows **hotter the angrier he gets**, steam rising off his shoulders, and fire that can't hurt him. He smashes the ground, breathes a wave of heat, charges a huge jump straight up, soaks up beams and lightning, and when his **Anger** reaches 100 it bursts out of him in a **ring of fire**. **N** turns him back into Ross, small enough for doors.

Hebrew: [עברית](Red-Hulk-HE)

![Red Hulk](images/063-red-hulk.png)

## The quick version

* **How:** use a **[Super Soldier Serum](Super-Soldier-Serum)** (in the lab of the Avengers Tower) and choose **Red Hulk**.
* **R** Ground Smash, **G** Heat Wave, **V** Super Jump (hold), **B** Anger Burst, **N** Transform (Ross). **Sneak** changes four of them: Grab & Slam, Roar, Bound, Energy Absorb (hold).
* **Anger** fills when he is hurt and when he hits. At **100** he bursts: fire all round him.
* **Fire, lava and magma never hurt him.** He doesn't fly: he jumps very, very high.

## Becoming Red Hulk

* Use a [Super Soldier Serum](Super-Soldier-Serum) and choose the **Red Hulk** card. Your old hero is replaced, and over a second and a half you **grow** into Red Hulk ("Hotter when angry"). If there is no room for him where you stand, you stay **Ross** for now ("No room for Red Hulk here."): go outside and press **N**.
* Or **Choose a Hero** in the [Hero Creator](Hero-Creator), or `/enhanced power set <player> red_hulk` (and a look: `red_hulk` or `own`).

**Losing it:** the Mutant Cure, `/enhanced power set <player> none`, another serum choice, an [Arc Reactor](Iron-Man#becoming-iron-man-the-arc-reactors), [Eternity's](Eternity) Love, being sacrificed at Vormir, or choosing another hero. He shrinks back at once (if he stood in a tight spot, he is moved to the nearest free place).

## The body

Only as Red Hulk, not as Ross:

* **1.5 times as big** (everyone sees it), **44 health** (22 hearts), **+6 armour** and **+3 toughness**, and very hard to push.
* **Strength IV:** every blow **+12**.
* **Slowness I:** 15% slower.
* **Fire Resistance:** fire, lava, magma blocks and campfires never hurt him, and he never keeps burning. He still drowns, falls and is hurt by explosions.
* A longer reach, he steps up a block, jumps higher, and falls 20 blocks before it hurts.
* His vanilla armour is hidden, as for every hero.

## Anger

![Red Hulk's anger burst](images/068-red-hulk-anger-burst.png)

His **Anger** goes from 0 to 100, and everyone can see it: his skin's cracks glow brighter as it rises, steam pours off his shoulders from 75, and from 90 he pulses with heat.

* **It fills** when he is hurt (**3 for each health** he loses) and when he hurts someone (**1 for each health** he deals); the Roar adds 10, and the Energy Absorb 2 for each health it soaks.
* **It cools** after 5 seconds out of the fight: 4 a second.
* **At 100: the Anger Burst.** A ring of fire and heat rolls out from him to **6 blocks**: **10** at the middle (5 at the edge) to every creature it may hurt, which **burns for 5 seconds** and is pushed away. **Blocks catch fire** only if the game rule `mobGriefing` is on (and the server's `red_hulk.burstSetsFire`), never in a protected area or a boss arena. Then his anger is 0. The camera shakes near him (not with *Reduced motion*). The advancement **Hotter when angry** is for his first burst.
* **Ross** gets angry too, at half the rate, and at 100 **he turns into Red Hulk** (if there is room) instead of bursting.

## Abilities

![Red Hulk's ground smash](images/064-red-hulk-ground-smash.png)

![Red Hulk's leap](images/065-red-hulk-leap.png)

![Red Hulk's heat wave](images/067-red-hulk-heat-wave.png)

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Ground Smash | Both fists into the ground: a ring runs out from his feet to **6 blocks**, **8** to every creature on the ground in it, thrown up into the air; the ground's own crumbs and cracks fly (no block is broken, unless a server allows it: `red_hulk.smashBreaksBlocks` with `mobGriefing`). Cooldown 4 seconds. **Sneak + R: Grab & Slam**: he grabs the creature in front, slams it down twice and throws it (**5**, **5**, **7**), as the Hulk does. Cooldown 8 seconds. |
| 2 | **G** | Heat Wave | He spreads his arms and a cone of heat rolls out, **10 blocks** long: **5** to every creature in it, set **burning for 4 seconds**. With `mobGriefing` it melts snow and ice in its way. Cooldown 6 seconds. **Sneak + G: Roar**: every monster within **12 blocks** is frightened and runs away for 4 seconds, and is weakened for 8; his anger +10. Cooldown 15 seconds. |
| 3 | **V** | Super Jump (hold) | Hold **V** on the ground: he crouches and **charges** (a ring round the crosshair fills, up to a second and a half; he can't walk meanwhile). **Let go**: he launches nearly straight up, **8 blocks** with no charge up to **30 blocks** fully charged, drifting a little where he looks. **His next landing does no fall damage**, and it makes a small smash (**4** within 3 blocks). Cooldown 3 seconds after the landing. **Sneak + V: Bound**: a long low leap, **20 blocks** forward, with a safe landing. Cooldown 2 seconds. |
| 4 | **B** | Anger Burst | With at least **50 anger**: the burst now, as big and as strong as his anger (at 50 it is half). Below 50: "Not angry enough… yet." (nothing is spent). Cooldown 10 seconds. **Sneak + B, held: Energy Absorb** (up to 4 seconds): he plants his feet and red light pulls into him: **fire, lightning, explosions and beams don't hurt him**, they make him **angrier** (+2 for each health). Blows and arrows still hurt. He can't walk while he absorbs. Cooldown 10 seconds after. |
| 5 | **N** | Transform (Ross) | Red Hulk ↔ **Ross**, in a second and a half. Ross is a man of normal size in a dark navy suit, a white shirt and a red tie: he fits through doors, has no Red Hulk body, and every key but **N** says "Ross can't do that.". Growing back needs room ("No room for Red Hulk here."). Cooldown 2 seconds. |

With PvP off, his abilities don't hurt players. Everything he pushes at the edge of the [Vormir Cliff](Vormir) counts as his push.

## Looks

![Red Hulk, close up](images/066-red-hulk-close-up.png)

| Look | Id | What it is |
|---|---|---|
| **Red Hulk** (default) | `red_hulk` | Deep red skin with darker shading and glowing cracks, grey-white swept-back hair and a grey moustache (Ross is an older man), yellow-orange eyes, black torn trousers with a dark belt, bare feet. Drawn with [the Hulk's](Hulk) big body, at 1.5 times your size. |
| **Your own skin** | `own` | His powers in your own skin, at his size. |

**Ross** is not a look: he is you, in a dark suit and a red tie. If the client option *The Hulk's body* (`hulkModel`) is off, Red Hulk is drawn as a big player in his skin.

## The HUD

The **Anger bar**: red, then orange, then white-hot, with flames from 75, a pulse from 90 and a mark at 50 (the least for **B**). As Ross: "Ross: N to become Red Hulk". Also "Changing…", "Absorbing", the jump's charge ring round the crosshair, and the cooldowns.

## Against the others

* He is no Banner: the [Hulk's](Hulk) Gamma Emitter does nothing to him. Two Hulks, a green and a red, can fight (with PvP on).
* [Magneto](Magneto) can't lift him (not metal), and his mind is open to [Professor X](Professor-X).
* His blows and his burst can't keep [Wolverine](Wolverine) or [Deadpool](Deadpool) down.

## Red Hulk in the Hero Creator

His abilities are in the [Hero Creator](Hero-Creator) (`red_hulk.smash`, `red_hulk.heat_wave`, `red_hulk.super_jump`, `red_hulk.burst`, `red_hulk.transform`, `red_hulk.body`), and his four Sneak variants are cards of their own (`red_hulk.grab_slam`, `red_hulk.roar`, `red_hulk.bound`, `red_hulk.absorb`). **Red Hulk's Body** brings the size, the fire resistance and the anger meter; the **Energy Absorb** is a shield (it can't go with another hero's shield); and **Transform** is a human form, like the Hulk's: **N** turns every power off until you press it again. See [Heroes' Abilities](Hero-Abilities#red-hulk-6). The ready-made hero is *Red Hulk-like*, and *Red Hulk* is a run style.

## Numbers you can change

Every number of his body, his anger and his abilities is a `red_hulk.*` option. See [Server Configuration](Server-Configuration#red-hulk).

## Related

[Super Soldier Serum](Super-Soldier-Serum) · [Hulk](Hulk) · [Captain America](Captain-America) · [Falcon](Falcon) · [Controls](Controls) · [Looks](Looks)

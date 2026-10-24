# Hulk

Bruce Banner (3.6), whose gamma accident made him **the Hulk**, the strongest one there is. One hero, two states: **Bruce Banner**, a man with no powers at all, and the green giant, either the gladiator Hulk of *Thor: Ragnarok* or the even bigger, savage Hulk of *Spider-Man: Brand New Day*. He smashes the ground, claps a shockwave, leaps across the land, throws boulders and charges through everything, and the angrier he gets, the harder he hits.

Hebrew: [עברית](Hulk-HE)

![The Hulk in containment](images/061-hulk-containment.png)

## The quick version

* **How:** craft a **Gamma Emitter**, place it, click it, and with **no power** stand in its green beam for 3 seconds. You become Bruce Banner, and Hulk out at once.
* **R** Hulk Smash, **G** Thunder Clap, **V** Super Leap, **B** Boulder Throw, **N** Transform (Banner and the Hulk). **Sneak** changes each of them: Grab Smash, Roar, Bound, Gamma Charge, Switch Hulk.
* **Rage** fills as he fights and makes him hit harder. **Banner's anger** turns him into the Hulk by himself.
* He doesn't fly: he leaps.

## Becoming the Hulk: the Gamma Emitter

1. **Craft the Gamma Emitter:** iron blocks in the four corners, lime stained glass on the four sides, an **emerald block** in the middle and a **redstone block** at the bottom middle (`I G I / G E G / I R I`). A server can switch the recipe off (`gamma.emitterCraftable`).
2. **Place it** and **right-click it**, or power it with redstone. It **warms up** for 3 seconds (a hum and a green glow), then a **green beam** shines out of its face, **6 blocks** long, for **3 seconds**. Then it rests for 10 seconds ("The Gamma Emitter is cooling down").
3. **Stand in the beam** with **no power** for the whole 3 seconds. The gamma takes you: "Hulk smash!" Everyone is told: "Eilon stood in the gamma beam and became the Hulk!" You are Bruce Banner, and **the first change happens at once**.

A Hulk who stands in the beam is **healed** (2 health a second) and gets angrier. On any other hero: "The gamma does nothing to you."

You also become the Hulk with **Choose a Hero** in the [Hero Creator](Hero-Creator), or with `/enhanced power set <player> hulk` (and a look: `ragnarok`, `brand_new_day` or `banner`).

## His forms

![The Hulk in containment, second view](images/062-hulk-containment-2.png)

| Look | Id | What it is |
|---|---|---|
| **Ragnarok Hulk** | `ragnarok` | The gladiator of Sakaar: **×1.5** as big as a player, with a belt, a kilt, bracers and one pauldron. |
| **Brand New Day Hulk** | `brand_new_day` | The savage Hulk: **×1.75** as big, in torn trousers, and every blow **15% harder**. |
| **Bruce Banner** | `banner` | The human: a purple shirt, a brown jacket and khaki trousers. **No powers at all.** |
| **Your own skin** | `own` | You, as Banner: human too. |

**Switching:**

* **N** turns Banner into the Hulk he was last, and the Hulk back into Banner. A change takes **2 seconds**: Banner hunches, clenches his fists, grows and roars; the Hulk kneels and shrinks. While he changes nothing can push him and the keys do nothing. Cooldown 5 seconds.
* **Sneak + N** (as a Hulk) switches between the two Hulks in a green shimmer (1 second). Cooldown 2 seconds. As Banner: "Only the Hulk has two Hulks."
* The **Change Look** key (**H**) changes them too, with the same transformation.
* **No room to Hulk out here.** The Hulk needs room to grow, and the room is checked the whole way through: he never gets stuck in a wall. Under a low roof Banner stays Banner (his anger waits). A door two blocks high is too small for the Hulk: be Banner indoors.
* As Banner, every key but **N** says "Bruce can't do that. Press N to Hulk out."

## Rage and Banner's anger

**Rage** (the Hulk): a green bar on the HUD, from 0 to 100.

* It fills when he is hurt (4 for every heart), when he lands a blow (2) and when he roars (20). After 10 seconds out of a fight it drains, 2 a second.
* From **50** he is **enraged**: his eyes glow, and Nausea and confusion slide off him: "Hulk too angry."
* Rage makes **every blow stronger**: up to **30% more** at full rage.

**Anger** (Bruce Banner): the HUD shows his **heart rate**, from 70 to 200 beats a minute, and you hear it beat faster.

* It rises when he is hurt (10 for every heart), and jumps once when his health falls below 4: the Hulk protects Banner. It calms down by itself.
* At **200 beats a minute** Banner **Hulks out by himself**, whether you want it or not.
* Unless he carries a **Gamma Inhibitor**: a black band on his wrist keeps him human however angry he is ("Inhibitor on" on the HUD). It has no recipe: it is in the Creative menu.

## The body

As a Hulk:

* **40 health** (20 hearts), **+8 armour** and **+4 toughness**, and hardly any knockback.
* Fists that hit **+7** harder (the Brand New Day Hulk 15% more again), throw creatures back, and reach further (his long arms).
* **Giant jumps**, **no fall damage**, a step over a whole block, and he runs 15% faster.
* He **breaks blocks by hand four times faster**.
* He **heals** by himself after 5 seconds without being hurt, but he is **hungrier** (one and a half times).

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Hulk Smash | Both fists into the ground: a ring runs out from his feet and hits **everyone on the ground within 6 blocks** for **7** damage (3.5 hearts). Whoever is flying or in the middle of a jump is not hit. Cooldown 3 seconds. **Sneak + R: Grab Smash**: he grabs the creature in front of him by the legs and slams it into the ground left, right, left: **4**, **4** and **6**. A boss is too big to grab: it gets one heavy punch of **8**. Cooldown 8 seconds. |
| 2 | **G** | Thunder Clap | He claps: a shockwave runs out in front of him (14 blocks long), **6** damage, and it **throws** everything it hits far away. It turns arrows and other shots back, and **puts out fire**. Cooldown 5 seconds. **Sneak + G: Roar**: every monster within 12 blocks is **frightened** (it runs away from him for 4 seconds) and weakened for 8 seconds; his rage jumps. Bosses are not frightened. Cooldown 15 seconds. |
| 3 | **V** | Super Leap | He jumps very high and falls fast to **where he looks** (up to 48 blocks): **6** damage (3 hearts) to everyone within 4 blocks where he lands. Cooldown 6 seconds. **Sneak + V: Bound**: a long, low leap of 24 blocks forward. Again and again, every 1.5 seconds. |
| 4 | **B** | Boulder Throw | He tears a **boulder** out of the ground (it looks like the block he stands on, but the ground stays as it was), lifts it over his head and throws it where he looks: **8** to the creature it hits and **4** to everyone within 2 blocks. Cooldown 4 seconds. **Sneak + B: Gamma Charge**: he charges **12 blocks**, shoulder first, and everything in his way takes **6** and is thrown aside. A wall stops him. Cooldown 7 seconds. |
| 5 | **N** | Transform | Banner ↔ the Hulk (above). **Sneak + N: Switch Hulk.** |

With PvP off, his abilities don't hurt players.

## Breaking blocks

The Hulk smashes what is soft. A server chooses how much (`hulk.blockBreaking`: `NONE`, `SOFT` or `ALL`):

* **`SOFT`** (the default): glass and glass panes, leaves, snow, ice, flowers, grass, vines and cobwebs, round the Smash, where the Leap lands and along the Charge. The Clap shatters only glass and snow.
* **`ALL`**: also small **craters** in dirt, sand, gravel, stone and wood (anything hardness 3 or less).
* **`NONE`**: nothing.

Every break also needs the game rule **`mobGriefing`** on, and never happens in a protected place (a boss's arena, the city of Asgard, a Hex, a ward, the Mirror Dimension, spawn protection, claims). The Hulk **never** breaks doors, trapdoors, beds, crops, chests or anything else that holds things, and never bedrock or obsidian.

## The Gauntlet and the Snap

The Hulk may **wield a filled [Infinity Gauntlet](Infinity-Stones#the-infinity-gauntlet)** (as a Hulk, never as Banner) and use its stones, and with all six he can **Snap**, as in *Endgame*. The Snap costs no health, but **it burns his right arm**: "The Snap burned the Hulk's arm." His arm is drawn burned, the HUD counts it down ("Burned arm 9:41") for **10 minutes**, and he has Weakness II for a minute. He can't hold a bare stone: he is no god.

## Looks and how he moves

The Hulk has a **giant body of his own** (a model in his proportions, not a stretched player), with eyes that glow gamma green when he is enraged. He stands hunched, runs heavily and shakes the camera of everyone near when he smashes or lands (not with *Reduced motion*). In first person you see his huge forearms. If another mod's player model clashes with his body, the client option `hulk.hulkModel` (in *Mods, Enhanced, Config*) draws the normal player model in his skin instead.

## The HUD

Next to the usual panel: the **rage bar** (green, with a mark at 50; "Enraged"), or, as Banner, his **heart rate** ("♥ 97 bpm", red when it is high), "Inhibitor on", the burned arm's time, "Changing…" while he transforms, and "Bruce can't" on the keys Banner can't use.

## Against the others

* **The Gamma Emitter** heals him.
* **Mind powers** work on Banner and on the Hulk (he is no telepath), but an enraged Hulk shrugs off Nausea and confusion.
* At [Vormir](Vormir) his push is only a fall: he is not a Bearer.

## The Hulk in the Hero Creator

His abilities are in the [Hero Creator](Hero-Creator) (`hulk.smash`, `hulk.clap`, `hulk.leap`, `hulk.boulder`, `hulk.transform`, `hulk.body`), and four Sneak variants are cards of their own (`hulk.grab_smash`, `hulk.roar`, `hulk.bound`, `hulk.gamma_charge`); see [Heroes' Abilities](Hero-Abilities#the-hulk-6). For a custom hero, `hulk.transform` is the **Human Form**: **N** turns every power off and shows your own skin, until **N** again. **Switch Hulk** is only the real Hulk's. The ready-made hero is *Hulk-like*.

## Numbers you can change

Every number of his body, his abilities, his rage and his transformation is a `hulk.*` option, the Gamma Emitter's are `gamma.*`. See [Server Configuration](Server-Configuration#the-hulk-and-the-gamma-emitter).

## Related

[Infinity Stones](Infinity-Stones) · [Items and Crafting](Items-and-Crafting#gamma-emitter-36) · [Controls](Controls) · [Looks](Looks)

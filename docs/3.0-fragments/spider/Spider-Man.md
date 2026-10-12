# Spider-Man

Tom Holland's Peter Parker, the friendly neighbourhood Spider-Man. He cannot fly: he goes everywhere by **web**. He webs creatures and rolls them up, bursts a nova of webs in every direction, **zips** to the point he looks at (then the web vanishes and he zips on to the next wall), **swings** on a web, **sticks to walls and ceilings and walks on them**, feels danger coming, and wears one of three suits: the **Homemade Suit** of *Homecoming*, the **Iron Spider** with its four mechanical legs and Instant Kill, or the **Brand New Day** suit with organic webs. A fifth power, from *Spider-Man: Brand New Day*, turns his eyes black and makes him much faster and stronger for a while, at a price.

## The quick version

Four tricks, in plain words. (**Sneak** is the Shift key, unless you changed it. Everyone nearby sees all of them.)

* **Press R while looking at a creature: web it.** A silver web flies out of your wrist, and the creature is wrapped for 6 seconds: it can barely move and can't jump. Web blocks appear round it and vanish by themselves after 10 seconds. Look at a block and you leave a patch of web on it instead. **Sneak + R** is the **Web Burst**: 24 strands fly out in every direction and wrap everything near you. Then wait 12 seconds.
* **Press G while looking at a block: zip to it.** The web pulls you there, then it vanishes and you stick to the wall (or the ceiling) you arrived at. **Press G again** to zip on to the next one, as fast as you like. **Sneak + G, and keep holding G**, is the **Web Swing**: you swing on the web until you let go.
* **Jump at a wall and keep pushing: climb it.** You stick to the wall and walk along it, up it and over the top, and even upside down on a ceiling. Press **Space** to leap off.
* **Sneak + B, twice** (Iron Spider only): **Instant Kill**. And **N**: the **Black Eyes**.

## Becoming Spider-Man

He wakes when a player with no power is bitten by a **Radioactive Spider**: a small spider in a jar, glowing sickly green. You craft it or find it.

```
F G F
G B G
F G F

F = Fermented Spider Eye
G = Glow Ink Sac
B = Slime Block
```

| Where it is found | Chance |
|---|---|
| Abandoned Mineshaft chest | 4% |
| Dungeon chest | 5% |
| Trial Chambers reward chest | 3% |
| Ancient City chest | 2% |
| Cave Spider, killed by a player | 0.4% (+0.3% per Looting level) |

The Radioactive Spider is an epic item, up to 16 to a stack, and it is used up by nothing but the bite.

1. With **no power**, right-click with it in your hand. You hear the bite and green motes rise from your hand. **One spider is used up** (not in Creative) and you take **1 damage** (a bite never takes your last half heart).
2. **For 7 seconds you are sick**: the *Radioactive Bite* effect, with Nausea, Weakness and Slowness, and Poison for the last 3 seconds (Poison never kills).
3. When the 7 seconds are over, you awaken as **Spider-Man**: the title "With great power comes great responsibility", the awakening burst and a message to everyone on the server. You start in the **Homemade Suit**.

Drink milk and the bite is cured: nothing happens ("The bite fades"), and the spider is gone. The same if you die, or get another power, during the 7 seconds. Log out and the bite keeps counting. A hero who uses the spider is turned away ("The spider's bite changes nothing: your power does not mix") and keeps it; so is a player who is still sick from the last bite. Creative players are bitten and keep the spider. An operator can use `/enhanced power set <player> spider_man [look]` (see [Commands](Commands)).

## The three suits

Press **H** to change suit (**Sneak + H** goes back). The suits are looks (see [Looks](Looks)), but Spider-Man is the one hero whose looks **are not all the same**: they differ in **protection**, and only the Iron Spider has **B**.

| Suit | Id | Protection | What it is |
|---|---|---|---|
| **Homemade Suit (Homecoming)** (default) | `homemade` | none | The suit of *Civil War* and the end of *Homecoming*: a red knit mask with round goggles, a red hooded vest with a hand-painted black spider and a brass zipper pull, a blue-grey sweatshirt, fingerless gloves with the brass web-shooters, baggy sweatpants, red socks. |
| **Iron Spider** | `iron_spider` | armor 14, toughness 6, 20% knockback resistance | Tony's nanotech suit of *Infinity War* and *Endgame*: crimson mesh, big white lenses, black armoured bands from the shoulders into a gold hexagon on the chest, gold trim. It has **four mechanical legs** and **Instant Kill** (ability 4). It is made of metal: Magneto can lift it and scatter its nanites. |
| **Brand New Day** | `brand_new_day` | armor 8, toughness 2 | The suit Peter sews himself in 2026: orange-red and turquoise-leaning blue, raised silver web lines, a big black spider with long thin legs. His webs are **organic**. |
| **Your own skin** | `own` | none | His powers in your own skin. |

For scale, a full diamond set is 20 armor and 8 toughness. Nothing is worn: the suit's protection comes with the look, and works at once when you change it, even in the air. Spider-Man has the normal 20 health: he is protected, not tough.

## Spider-Man's body

* **Quicker, higher, stronger:** +15% walking speed, a jump of about 2.1 blocks instead of 1.25, +2 melee damage (his bare fists hit for 3).
* **A soft landing:** half the fall damage. He takes none at all while he clings, zips or swings, and none with the legs out.
* **Immune to webs.** Any Spider-Man is never wrapped by a web, his own or another's, and a web block lets him through (so does a flying Creative player). A vanilla cobweb still holds him.
* **Organic webs.** In the Brand New Day suit, or whenever the Black Eyes are on, his webs are *organic*: thicker, creamier, they hold **50% longer** and cost **twice the hunger**, and he needs something in his stomach to make them. The other suits' mechanical web-shooters have no such need.
* **Hunger is the only resource.** There is no web fluid: every ability costs a little food (none in Creative).

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Web Shot / **Sneak: Web Burst** | See below. The burst has a **12 second** cooldown, shown on the R line (`burst 9.5s`). The shot has a hidden half-second pause. |
| 2 | **G** | Web Zip / **Sneak + hold: Web Swing** | See below. Zips can be chained: only 0.3 seconds between two. The swing lasts as long as G is held. |
| 3 | **V** (toggle) | Wall Crawl / **Sneak: Spider-Sense** | Wall Crawl is on by default; Sneak + **V** switches Spider-Sense, also on by default. |
| 4 | **B** (toggle) | Spider Legs / **Sneak, twice: Instant Kill** | **Iron Spider only.** |
| 5 | **N** (toggle) | Black Eyes | The *Brand New Day* surge. Lasts 30 seconds at most. |

**G is the only key whose release matters**: the swing ends when you let go.

### Webs: the shot and the burst

**Web Shot (R).** Look at a creature or a block, up to 24 blocks away, and press **R**. A silver strand shoots from your hand to the impact and a web splat sticks to the wall.

* **A creature** is **wrapped** for 6 seconds (9 with organic webs): it moves at a fraction of its speed, can't jump, and a flying one barely flies. It can still hit what is next to it. Two web blocks appear at its feet and at chest height, if there is free air. A hard blow (6 damage or more) tears 1 second off the web, so a wrapped creature that is being beaten breaks free sooner.
* **A block** gets a **patch of web**: the free air in front of the face you hit, 3 by 3 at most. It closes a corridor, a door, or a pit.
* **Nothing to web** (the aim ends in the air): "Nothing to web" and nothing is spent.
* A **complete Chaos Shield** (the Scarlet Witch's) stops the strand: the web splats on the shield and costs it 2 points.
* Costs 0.4 exhaustion (a tenth of a food point), twice for organic webs.

**Web Burst (Sneak + R).** Needs at least one creature within 10 blocks that you can see ("Nobody close enough to web" otherwise). Then **24 strands** leave your chest in a sphere, evenly in every direction. Each strand flies up to 10 blocks and stops at the first block or creature. **Each creature a strand hits is wrapped for 4 seconds** (6 with organic webs), however many strands hit it. Each strand that hits a block leaves one web block in front of the face. A burst leaves a loose cocoon of webs round you and over the nearby walls, and everything near you stuck. A shield that is complete takes the strands that reach it, half a point each (never more than 12 points of its 30). Costs 2 exhaustion (half a food point), twice for organic webs. **Cooldown: 12 seconds.**

**Web blocks.** The webs a shot or burst leaves are a block of their own, the **Spider Web** (you can't craft it or pick it up). It is like a cobweb, but it holds everyone **except Spider-Man**, and it **vanishes by itself after 10 seconds** (15 with organic webs): the world never stays webbed. It only appears in **free air**, never over a block, a liquid or a plant, never where spawn protection or a claim says no, and never inside a boss arena. One Spider-Man can have 64 at once; past that the oldest vanish first. A hand or a sword clears one at once, and it makes a cobweb's breaking sound.

**Who is immune.** The big bosses (the Ender Dragon, the Wither, the Warden, the Elder Guardian and the bosses of 3.0): they are not wrapped (and Thanos's Gauntlet feels the web: it locks for two seconds). Their minions are wrapped like any creature. Another Spider-Man, Creative and Spectator players, and players when **PvP is off**.

### Web Zip and Web Swing (G)

**Web Zip (G).** Look at a block, up to 40 blocks away, and press **G**. A web flies to it and **pulls you there**, fast: 26 blocks a second, so 40 blocks take about a second and a half.

* The web is gone when you arrive, fading and falling as white motes. If you arrive at a **wall or a ceiling** and Wall Crawl is on, you **stick to it at once**, and can crawl, leap or **press G again to zip on to the next wall**. Arrive on top of a block and you just land standing.
* Press **Space** during the zip to cancel it: you keep your speed in a flying leap. It also ends if you hit something, if the block you aimed at is gone, after 3 seconds, or if you are held (Mjolnir, Billy's spell, telekinesis).
* **The Hex stops it:** if your stand point is across a wall of Wanda's Hex ("The Hex stops your web"). A complete Chaos Shield in the way stops the web and gets 2 damage. Nothing is spent when a zip is refused.
* **Aim at a creature** and you zip to it and **kick** it as you arrive (4 damage and a shove; never a Creative player or a boss's protected target, a player only with PvP on). The server can turn the kick off.
* Costs 0.3 exhaustion, twice for organic webs.

**Web Swing (Sneak + G, hold G).** The same aim, up to 40 blocks, and a web from your hand to the anchor. **While you hold G** you swing on it like a pendulum, a little lighter than vanilla gravity, with a top speed of 1.8 blocks a tick. **W** reels the web in, **S** lets it out, the sideways keys push you a little. **Let go of G** and you fly off with the speed you had (**Space** gives it a push). The web also lets go if you touch the ground for half a second, if the anchor block is gone, after 15 seconds, or if you are held. **Press G without Sneak** while swinging to let go and zip to the new aim: a chain from swing to wall. Costs a food point every 20 seconds of swinging (twice that with organic webs). Everyone hears a creaking while you swing.

### Wall Crawl (V)

Wall Crawl is **on by default**. With it on:

* **Enter a wall:** jump at it and **keep pushing into it** (in the air, the keys that press you into the wall are enough), or stand against it and **hold Space**. Walking into a wall on the ground does nothing, so you can still walk along a house and build as usual.
* **Enter a ceiling:** hold **Space** with your head against it, or climb a wall until your head meets one.
* **Move:** the movement keys now move you along the wall. **Look along the wall and W goes that way; look at the wall or up and W climbs; look down more than about 27 degrees and W climbs down.** A and D go sideways. Over the top of a block you **mantle** onto it. On a ceiling you walk upside down, the way you face.
* **Leap:** a new press of **Space** (not the one that began the climb) throws you off the wall, the way you look, away from the surface.
* **Let go:** walk down onto the ground, switch Wall Crawl off with **V**, or be held (Mjolnir's pin, telekinesis). Billy's spell freezes you where you are on the wall; you don't fall.
* **Speed:** 4.4 blocks a second, times your speed (Slowness, Quicksilver's slowed zone, the Webbed effect and the Black Eyes all change it), 25% more with the Iron Spider's legs out, **1.6 times as fast** with the Black Eyes. You use up a little food while you move: a food point for every 40 seconds of crawling.
* **What you can stick to:** any solid block, but not water, and not cobwebs, vines and ladders (nothing to hold). A server can list blocks you may not stick to (`enhanced:cannot_cling`). You can still use items, place blocks and attack while you cling.
* Everyone nearby sees you **against the wall, facing it, climbing**, or upside down on the ceiling.

### Spider-Sense (Sneak + V)

A warning of danger, and no more. On by default; Sneak + **V** switches it off and on. When something is about to hurt you, your head snaps up and your arms go into a guard (everyone sees it), a tingle sounds for you alone, the edge of your screen flashes white-blue with ripples, a small arrow at the edge points to the strongest threat, and the threats are **outlined through walls** for a second and a half. A threat is:

* **a projectile** (an arrow, a trident, a fireball, a wither skull, a shulker bullet) that is not yours and will pass within 1.2 blocks of you in the next second and a quarter;
* **a hostile mob that is about to strike**: within 6 blocks, awake, and after you (a neutral mob only if it targets you);
* **a fuse:** a creeper swelling within 7 blocks, or TNT with 2 seconds or less to go within 8 blocks;
* **a player about to hit you** (PvP on): within 4 blocks, facing you, and has just swung.

It scans every quarter of a second, up to **12 blocks** away, and it costs nothing. It never changes what happens: it is only a warning. With the **Black Eyes** on its range is doubled, it scans more often ("sensory overload"), and **every hostile creature in range is outlined**, not only the threats.

### Iron Spider systems (B)

**Iron Spider look only.** In another look, **B** says "Only the Iron Spider suit has legs".

**Spider Legs (B).** Four **gold-and-black mechanical legs** unfold from the gold circle on your back (14 ticks), with a ratcheting sound, and sway over your shoulders. Press **B** again to fold them. While they are out:

* **no fall damage at all** (you land on them), and your **reach is 2 blocks longer**;
* the legs **guard you**: every second the nearest threat within 4 blocks is jabbed by a leg for 5 damage and a little knockback (a stab, not a kill);
* the legs **plant on the wall, the ceiling or the ground** where you walk, and step with you; you crawl 25% faster;
* a food point every 20 seconds.

They fold by themselves if you change look, or when **Magneto's Armor and Weapon Strip** scatters the nanites (the suit's armor, the legs and Instant Kill are **off for 10 seconds**: "The nanites are scattered").

**Instant Kill (Sneak + B).** From *Endgame*, where Peter turns it on on purpose and the legs kill the Outriders. You need the **legs out**. **Press Sneak + B, then press it again within 3 seconds to confirm** ("Instant Kill: press Sneak + B again to confirm"; the HUD says `confirm?`). Your lenses close to **two red slits**, a low alert sounds, and for up to **10 seconds** every quarter of a second **each of the four legs strikes one threat** within 5 blocks, nearest first, for **1000 damage** (a kill), in a red-gold spark. It costs food (about 4 points over the full 10 seconds), and ends by itself, with Sneak + B, when the legs fold, when you are out of food or held. Then wait **30 seconds** (the HUD says `recharging`).

**Who it kills.** A **hostile** monster that is alive and not owned or tamed; a neutral mob (an Enderman, a zombified piglin) only if it targets you. **Never** a villager, a golem, a pet, an animal, a boat or an armor stand, a Loki illusion, **or a player** (unless the server turns `spider_man.instantKillPlayers` on, and PvP is on too). **Bosses and their minions** (the Ender Dragon, the Wither, the Warden, the Elder Guardian, the bosses of 3.0 and their robots, Horsemen and phantoms) take only the legs' normal 5 damage, so that one Iron Spider can't erase the army a boss fight is built around. A complete Chaos Shield between a leg and its victim stops the strike.

### Black Eyes (N)

From *Spider-Man: Brand New Day*, where his eyes turn fully black and he lets the arachnid genes take over. **N** switches it on; you need at least **6 food points** and it must be rested. Your lenses go black with a dark red glint, a heartbeat sounds, and black-red wisps stream off your shoulders. You are hunched and trembling. **N again calms it at once**, with no crash (and half the wait). While it lasts (**30 seconds at most**):

* **speed +60%** on top of your own (you run, climb, zip and swing faster: climbing 1.6 times, zips and swings about 1.26 times), **+4 melee damage**, **+50% attack speed**, **+25% jump**, **30% knockback resistance**, and an even softer landing. Bare fists hit 6 times a second for 7 damage;
* **organic webs** in every suit, and a spider-sense that reaches twice as far (see above);
* in first person the edges of your screen darken and a slow heartbeat throbs (only the dark edge with *Reduced motion*);
* it **burns hunger**: a food point every 4 seconds.

**The price.** When the 30 seconds run out, or your stomach is empty, you **crash**: Slowness II and Weakness for 10 seconds ("The surge passes. You are drained."), and a 30 second wait (the HUD says `recharging`). A calm ending has no crash. If the server sets `spider_man.blackEyesPanicHealth`, the Black Eyes come on by themselves when your health falls that low ("his genes take over") and can't be calmed for 5 seconds. It never comes by itself otherwise. There is no night vision: that is from the comics.

## What other players see

His arm thrusts at what he aims at and a silver strand flies to it, with a splat on the wall. A burst: he crouches with his arms across his chest, then flings them wide and 24 strands flash out. A zip: the arm thrown at the anchor, the web from his hand, and him streaking there leaning into it, the web fading when he arrives. A swing: knees tucked, the web up to the anchor. On a wall: against it, facing it, arms reaching up; on a ceiling, upside down. The spider-sense: his head snaps up, his arms go into a guard. The Iron Spider's legs unfold from his back, sway, plant on the wall or the ground and jab. In Instant Kill his lenses are two red slits and the legs stab in turn. With the Black Eyes: black lenses, wisps and a hunched, trembling stance. Everyone hears all of it, and the webs, the legs and the eyes are drawn by every player's game.

## The HUD

| Key | Shows |
|---|---|
| **R** | `burst 9.5s` in grey while the burst recharges, `organic` in aqua when his webs are organic |
| **G** | `zipping` or `swinging` in aqua while it lasts |
| **V** | `ON` or `OFF`, then `clinging` in aqua while he clings, `sense OFF` if spider-sense is off, and `DANGER!` in red for a second after a warning |
| **B** | `Iron Spider only` in another suit; `nanites scattered` in red; the legs `ON` or `OFF`; `confirm?` in gold while he waits for the second press; `INSTANT KILL 7.5s` in bold red; `recharging 21.0s` |
| **N** | `ON 18.4s` in red while it lasts, `recharging 24.0s` after |

## How he gets on with the others

| With | What happens |
|---|---|
| **Wanda's Hex** | The wall bounces him like glass, like every hero: a zip, swing or cling it throws ends. A zip whose stand point is across the wall is refused. A shot or burst passes through the wall. |
| **The Chaos Shield** | A complete shield is solid to his webs, zips and legs (2 points for a shot, half a point for a strand, the strike's damage for a leg). He can't cling to a shield. |
| **Magneto** | The **Iron Spider** is metal: Magneto can lift and throw it (30 seconds at most) and **scatter its nanites** (10 seconds). The other suits are cloth. |
| **Mjolnir** | Pinned, he can't use a key, and every web, zip, swing and cling ends. Mjolnir can't be webbed, and he can't pin anyone with webs. |
| **Billy's Freeze** | Frozen, he can press nothing; a zip or swing ends, and a cling holds him in place on the wall. |
| **Telekinesis** | Held, he can't act (and a held Spider-Man drops off the wall). Spider-sense warns of a held player's slam. |
| **Loki** | Loki's duplicates dissolve at a web or a leg, and are never a threat. A shapeshifted Loki is webbed as himself. Loki's Scepter makes a Spider-Man dizzy and slow: nothing else. |
| **Quicksilver** | Slowness, Quicksilver Time's slowed zone and the Webbed effect slow his crawl, zip and swing (they all lower his speed). Quicksilver can be webbed, and a webbed speedster is slowed hard. |
| **Vision** | Phased, Vision is nothing to web; a dense Vision can be webbed. |
| **PvP off** | Webs, bursts and kicks don't touch players, and Instant Kill never targets one. |
| **Creative and Spectator** | In Creative everything works and costs nothing, and Creative players are not webbed or struck. A Spectator can use no key. |
| **Claims and protection** | A web block is only placed where a player may place a block, and never inside a boss arena. A shot still wraps creatures there. |

## Looks

*Homemade Suit (Homecoming)*, *Iron Spider*, *Brand New Day* and *Your own skin*. Press **H** to switch. The webs are always silver-white; the suit's colours are the accent of the title and of the look-change burst, and the Iron Spider's change is a nano-sweep in gold and crimson. See [Looks](Looks).

## Numbers you can change

His speed, jump, strength and fall damage; how long the bite takes; how far spider-sense reaches and how often it scans; the Web Shot's range, pause, hunger and how long a creature stays wrapped; organic webs' length and cost; whether webs leave web blocks, how long they last, the patch size and the cap per player; every number of the Web Burst (strands, range, cooldown, hunger, how long it wraps); the zip's range, speed, cooldown, longest time, hunger and kick; the swing's range, longest time, top speed, gravity and hunger; the crawl's speed and hunger and the leap's speed; each suit's protection and how long Magneto's strip lasts; the legs' damage, pace, range, reach and hunger; every number of Instant Kill (range, pace, damage, longest, cooldown, hunger, confirm window, whether it may hit players); and every number of the Black Eyes (speed, strength, attack speed, jump, knockback resistance, hunger, longest, cooldown, crash, stress surge, sense) are the `spider_man.*` options on [Server Configuration](Server-Configuration#spider-man). Three datapack tags (`enhanced:web_immune`, `enhanced:instant_kill_immune`, `enhanced:cannot_cling`) change who is webbed, who Instant Kill spares and what he can stick to.

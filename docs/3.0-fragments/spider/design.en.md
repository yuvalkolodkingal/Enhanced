## 14. Spider-Man **(added in 3.0)**

> In *Spider-Man: Homecoming* (2017) a fifteen-year-old "gained spider-like abilities after being bitten by a radioactive
> spider" and goes out in the suit he made himself; in *Infinity War* and *Endgame* Tony's Iron Spider gives him four
> mechanical legs and an Instant Kill mode; in *Spider-Man: Brand New Day* (2026) he sews a new suit, makes webs of his
> own body, and his eyes turn black when the spider in him takes over. All three suits and the black-eyed surge are
> played here, with one set of abilities. He cannot fly: he goes everywhere by web. (The film does not say how long
> the surge lasts or what it costs: both are a design choice, and the colour of the webs is ours.)

### 14.1 Becoming Spider-Man

* **The Radioactive Spider** (`radioactive_spider`): a small spider in a jar, glowing sickly green (`0x7CFC00`).
  Crafted (shaped, `data/enhanced/recipe/radioactive_spider.json`) and found:

  ```
  F G F      F = Fermented Spider Eye
  G B G      G = Glow Ink Sac
  F G F      B = Slime Block
  ```

  | Source | Chance |
  |---|---|
  | Abandoned Mineshaft chest | 4% |
  | Dungeon chest | 5% |
  | Trial Chambers reward chest | 3% |
  | Ancient City chest | 2% |
  | Cave Spider killed by a player | 0.4% (+0.3% per Looting level) |

  Five `neoforge:add_table` modifiers in the global list (table `enhanced:inject/radioactive_spider`) and a recipe
  advancement unlocked by a Glow Ink Sac. Stack 16, epic, foil; used up by nothing but the bite.
* **The bite** (`RadioactiveSpiderItem.use`, only a player with no power, not already sick): one point of damage (never the
  last), `biteDelayTicks` (140 = 7 s) of the effect `enhanced:radioactive_bite` with Nausea, Weakness I and Slowness I,
  and Poison I for the last 3 seconds (it never kills); sound `spider_bite`, green motes. One spider is used up (not in
  Creative). When the effect **expires** (`MobEffectEvent.Expired` in `SpiderEvents`) and the player is still online,
  alive and without a power, `Powers.awaken(SPIDER_MAN)` (the title "With great power comes great responsibility", the
  shared awakening sound and burst, a message to the server). He wakes in the Homemade Suit.
* **Cancelled** by milk or anything that removes the effect, by death, or by another power meanwhile ("The bite
  fades"). The effect is saved with the player, so a logout does not lose it. A hero is turned away with "The spider's
  bite changes nothing: your power does not mix" and keeps the item; a player already bitten with "You are still sick
  from the last bite". `/enhanced power set <player> spider_man [look]` works as for every hero.

### 14.2 The three suits

Looks are the `H` key's variants; the first is the default. Spider-Man is the **one exception to "the abilities are the
same in every look"** (section 0): the suits differ in **armour**, and **B** (legs, Instant Kill) works in the Iron
Spider look only. All three are slim-armed, with no cape.

| Look | Id | Armour | Toughness | Knockback resistance | Skin |
|---|---|---|---|---|---|
| Homemade Suit (Homecoming) | `homemade` | 0 | 0 | 0 | red knit mask with round goggles, red hooded vest with a black spider and a brass zipper pull, blue-grey sweatshirt, fingerless gloves with brass web-shooters, baggy sweatpants, red socks and shoes |
| Iron Spider | `iron_spider` | **14** | 6 | 0.2 | crimson mesh, big white lenses, gunmetal bands from the shoulders into a gold-outlined hexagon, gold trim, a gold circle on the back where the legs come from |
| Brand New Day | `brand_new_day` | **8** | 2 | 0 | orange-red and turquoise-leaning blue, raised silver web lines, a big black spider with long thin legs, tear-shaped lenses, a silver plate on each forearm |
| Your own skin | `own` | 0 | 0 | 0 | the player's skin |

The protection is **by look**, as permanent attribute modifiers (`enhanced:spider_suit_armor`, `..._toughness`,
`..._knockback`, idempotent, set by `SpiderServer.applyBody` for everyone and removed from anyone who is not a
Spider-Man): nothing is worn, so the rule that hides vanilla armour on heroes does not matter. A look change in the air
or mid-zip takes effect at once. The numbers are the options `homemadeArmor`, `brandNewDayArmor`, `brandNewDayToughness`,
`ironSpiderArmor`, `ironSpiderToughness`, `ironSpiderKnockbackResistance`. Spider-Man keeps the normal 20 health.

### 14.3 The body

| Attribute | Modifier | Amount | Option |
|---|---|---|---|
| `MOVEMENT_SPEED` | `enhanced:spider_speed` | +0.15 (ADD_MULTIPLIED_BASE) | `speedBonus` |
| `JUMP_STRENGTH` | `enhanced:spider_jump` | +0.30 (a jump of about 2.1 blocks) | `jumpBonus` |
| `ATTACK_DAMAGE` | `enhanced:spider_strength` | +2.0 (fists hit for 3) | `strengthBonus` |
| `FALL_DAMAGE_MULTIPLIER` | `enhanced:spider_fall` | -0.5 | `fallDamageReduction` |
| `ENTITY_INTERACTION_RANGE` | `enhanced:spider_legs_reach` | +2.0, legs out only | `legsReachBonus` |
| `FALL_DAMAGE_MULTIPLIER` | `enhanced:spider_legs_fall` | -1.0, legs out only (no fall damage) | |

Fall distance is reset every tick he clings, zips or swings. **Immune to webs:** a Spider-Man is never Webbed
(`MobEffectEvent.Applicable` at HIGH priority) and never held by a Spider Web block; a vanilla cobweb still holds him.
**Organic webs** (`SpiderServer.organic`): the look is `brand_new_day`, or `Live.BLACK_EYES` is on. His webs then last
`organicWebFactor` (1.5) times as long, look thicker and creamier, cost `organicWebCostFactor` (2.0) times the hunger and
need a non-empty stomach ("Too hungry to make webs"). Hunger is the only resource: `player.causeFoodExhaustion`, 4.0 = one
food point, nothing in Creative. New shared state, all appended: `Toggle.WALL_CRAWL` and `SPIDER_SENSE` (on at start)
and `SPIDER_LEGS` (off); `Live.CLINGING_WALL`, `CLINGING_CEILING`, `ZIPPING`, `SWINGING`, `SENSING`, `BLACK_EYES`,
`INSTANT_KILL` and `CASTING` for 6 ticks after a shot. No new `PowerData` field: `cooldownUntil(1)` is the burst's
cooldown, `(2)` the zip's, `(4)` and `(5)` the end of Instant Kill and of the Black Eyes while they last and then their
cooldown.

### 14.4 Abilities

Keys **R G V B N** are abilities 1 to 5. Only **G** needs the release of its key. Every number is an option in the
`[spider_man]` section of the server config.

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | R | **Web Shot** / **Sneak: Web Burst** | 14.5. |
| 2 | G | **Web Zip** (chainable) / **Sneak, held: Web Swing** | 14.6. |
| 3 | V (toggle) | **Wall Crawl** / **Sneak: Spider-Sense** | 14.7 and 14.8. |
| 4 | B (toggle) | **Spider Legs** / **Sneak, twice: Instant Kill** | Iron Spider look only, 14.9. |
| 5 | N (toggle) | **Black Eyes** | 14.10. |

A `blocked` Spider-Man (a spectator, riding, pinned by Mjolnir, held by Spellbound or by telekinesis, flying in
Creative or gliding) ignores every key, and every web, zip and swing he had ends; a cling ends too, except under
Billy's Spellbound, which holds him where he is on the wall. A dead player's tick and `PowerTicker.stopHoldAbilities`
clear the seven `Live` bits (`SpiderServer.stopHold`).

### 14.5 Webs

**Web Shot (R).** Needs the hidden half-second timer (`webShotCooldown` 10 ticks) and, if organic, food. The aim is the
first creature on the look ray within `webShotRange` (24) not behind a block, else the first block. Nothing to hit:
"Nothing to web", nothing spent. A complete Chaos Shield in the way takes the web (it pays 2: `WandaAbilities.wear`, only if
the Spider-Man may hurt its owner) and nothing beyond is touched. **A creature** (not in `enhanced:web_immune`, not a Creative
or Spectator player, not another Spider-Man, a player only with PvP on) gets **Webbed** for `webbedTicks` (120, organic
x1.5), a mob stops walking, and two web blocks are laid at its feet and at chest height. A web that reaches a `web_immune`
entity calls `BossHooks.onWebShot(spider)` on it (Thanos locks his Gauntlet) and applies nothing. **A block** gets a patch
of web blocks on the free air in front of the face, in its plane, within `webPatchRadius` (1: at most 3 x 3). Costs
`webShotExhaustion` (0.4, organic x2); sounds `web_shot` at him and `web_splat` at the impact; `Live.CASTING` 6 ticks;
a `SpiderFxPayload` SHOT to everyone within 64 blocks.

**Web Burst (Sneak + R).** Needs `isReady(1)` (`webBurstCooldown` 240) and a creature within `webBurstRange` (10) in
line of sight. `webBurstStrands` (24) rays leave his chest over a Fibonacci sphere turned by a random angle
(`y = 1 - 2(i + 0.5)/n`, azimuth `i * 2.399963`), each stopping at the first block or creature. A creature hit by any
strands is Webbed once for `webBurstWebbedTicks` (80, organic x1.5); a strand that hits a block lays one web block in front
of the face. Each strand that meets a complete Chaos Shield stops there and pays 0.5, at most 12 in all. Costs
`webBurstExhaustion` (2.0, organic x2); sound `web_burst`; a payload BURST with the strand ends.

**Webbed** (`WebbedEffect`, harmful, `0xF2F6FA`): `MOVEMENT_SPEED` and `FLYING_SPEED` -0.85 (ADD_MULTIPLIED_TOTAL), `JUMP_STRENGTH` -1.0,
silver motes round it. A blow of 6 or more takes 20 ticks off (`SpiderEvents`). **Immune:** `enhanced:web_immune`
(default `#enhanced:bosses`), any Spider-Man, Creative and Spectator players, and players when PvP is off.

**The Spider Web block** (`SpiderWebBlock`, `ModBlocks.SPIDER_WEB`, no item, no loot, hardness 0.5, destroyed by
pushing): a cobweb that does not hold a Spider-Man or a flying Creative player and **removes itself** by a scheduled tick
after `webBlockTicks` (200, organic x1.5), so the world never stays webbed and no `mobGriefing` gate is needed. Only into
free air (`isEmptyBlock`), never over anything else, with `mayInteract`, outside Adventure mode, inside the world border and
never where `BossFights.protects(level, pos)` says it is a boss arena. `webBlockCap` (64) live blocks per Spider-Man (the
oldest go first); `webBlocks` false turns the blocks off, the webs still wrap. Breaking sound: vanilla's cobweb; placing:
`web_splat`.

### 14.6 Zip and swing

Both aim with a block-only ray (`Raycasts.block`, through fluids).

**Web Zip (G).** Needs `isReady(2)` (`zipCooldown` 6) and not `blocked`. The anchor is the point hit within `zipRange`
(40); no block, "Nothing to zip to". The **stand point** `T` (`SpiderMath.standAt`) is where his body ends up: 0.35 out
from a wall face with the anchor at chest height, hanging under a ceiling, standing on top of a block, else the first
free offset. If `Teleports.crossesHex(level, player, T)`, "The Hex stops your web" (the same rule as Vanish and the Bifrost);
a complete Chaos Shield in the way takes the web and pays 2; both spend nothing. Otherwise `Live.ZIPPING` is raised, the
cooldown and `zipExhaustion` (0.3, organic x2) are spent, a swing is let go of, and `SpiderFxPayload` ZIP_START goes to
everyone near and to him. **The pull is the client's** (`SpiderMotion`):
`v = min(zipSpeed * sqrt(speedFactor), 0.3 + 0.1 t, 0.2 + 0.12 |T - p|)` along the line to `T`, with `zipSpeed` 1.3
blocks a tick. It ends on arrival, on a collision after 3 ticks, after `zipMaxTicks` (60), on Space (he keeps his speed),
when the anchor is gone, on a jump of the position (a Hex bounce or a teleport) or when he is blocked. The server
validates every tick (the anchor block, the distance, the time, a position that jumps more than a hitch can explain)
and resets `connection.aboveGroundTickCount` and the fall distance. On arrival at a wall or a ceiling with Wall Crawl on he
clings at once (`state.surface`, a grace of 8 ticks); on a floor he lands; with Wall Crawl off he keeps 40% of his speed.
**G again at any time** is a new zip: the chain. A zip aimed at a **creature** ends in a kick (`zipKickDamage` 4.0,
`enhanced:web_kick`, never a Creative or protected target; 0 turns it off).

**Web Swing (Sneak + G, held).** The same aim within `swingRange` (40); the rope is `L = clamp(distance, 4, swingRange)`.
While G is held (`HeroServer.stopHold` clears it when he dies) the client runs a pendulum, `SpiderMath.swingStep`:
`vel' = (vel + (0, -g, 0) + airControl) * 0.992`, clamped to `swingMaxSpeed` (1.8), with `g = swingGravity` (0.06); a
position past the rope length is pulled back onto the sphere, so the speed along the web is lost and the speed across it
kept. W reels the rope in (down to 3), S lets it out (to 1.25 times the first length). He lets go on release (keeping
his speed, plus a push if Space is held), after 10 ticks on the ground, if the anchor is gone, after `swingMaxTicks`
(300) or when blocked. G without Sneak while swinging zips to the new aim. Costs `swingExhaustion` (0.01) a tick.
A loop `web_swing` follows him (client, `PowerLoopSounds`). ZIP_END and SWING_END carry the reason: 0 arrived,
1 cancelled, 2 refused, 3 blocked, 4 timeout, 5 released, 6 anchor lost.

### 14.7 Wall Crawl

`Toggle.WALL_CRAWL`, on by default; "Wall Crawl on" or "off". A movement rule and a render transform: gravity is switched off
and his own keys move him along the surface, the player keeps the standing box, no pose is forced, so a cling can neither
suffocate nor get stuck. `SpiderClimb` has no client imports and is shared by the client (movement) and the server (the
flag others see).

* **Surfaces** (`SpiderClimb.detect`, reach 0.12): a wall on one of the four sides, or a ceiling above the head, made of a
  block with a collision shape that is not in `enhanced:cannot_cling`. A cobweb, a vine, a ladder never count. At a corner he
  picks the wall he presses into.
* **Entering:** Wall Crawl on, not blocked, not in a liquid, not riding or gliding, food above 0 (Creative excepted), 6
  ticks since a leap. A wall: he presses into it (`wish . -normal >= 0.3`) **in the air**, or on the ground holds Space.
  A ceiling: Space with his head against it, or a climb that meets one. A zip or swing that ends at a surface enters with
  no input.
* **Moving:** the velocity is set each tick: `v = speed * (F * forward + S * side) + (-normal) * 0.05`, with
  `speed = crawlSpeed (0.22) * speedFactor * (legs out ? 1.25 : 1)` and `speedFactor = clamp(MOVEMENT_SPEED /
  (sprinting ? 1.3 : 1) / 0.1, 0, 4)` (1.15 for Spider-Man, 1.84 with the Black Eyes). The attribute is the key: Slowness,
  Quicksilver's zone, Webbed, Spellbound (a factor of 0: frozen on the wall) and Loki's Scepter slow his crawl, zip and
  swing. On a wall `F = (a L + (b + 0.45) UP) / max(|w|, 0.25)` with `a` the look along the wall and `b` its height:
  looking along the wall W goes that way, at it or up W climbs, down more than about 27 degrees W climbs down, shrinking
  smoothly through the dead zone so there is no jitter. On a ceiling W walks the way he faces. The keys are read from the
  key bindings and cleared in `MovementInputUpdateEvent` while he clings.
* **Leaving:** a **leap** (a new press of Space at least 4 ticks after entering) along `normalize(look + normal * 0.35)` at
  `leapSpeed` (0.9) * sqrt(speedFactor); walking onto the ground for 2 ticks, V off, being pinned or held, the surface
  vanishing for 4 ticks, no food, water, riding, or a teleport. A **mantle** over the top of a block, and a flip from a wall
  to a ceiling and back, are handled at the edges. The optional corner climb is not built: he picks the wall he presses into.
* **Cost:** `crawlExhaustion` (0.005) a tick while he moves.
* **What the server sees:** a cling is raised from his position, `onGround` and the vertical history (he is beside or under a
  surface, in the air, not falling, not on the arc of a jump) and lasts 4 ticks after contact is lost; the client sends a
  keepalive position while it hangs still, and the server zeroes the downward motion of its copy while the flag is up.
  While clinging, zipping or swinging the server resets `aboveGroundTickCount` and the fall distance every tick.

### 14.8 Spider-Sense

`Toggle.SPIDER_SENSE`, on by default, Sneak + V. A warning of danger only. `SpiderSense.threats` runs every
`spiderSenseInterval` (5) ticks within `spiderSenseRange` (12), by spherical distance, and finds: a projectile not shot by
him whose straight path comes within 1.2 blocks of his body in the next 25 ticks; a hostile mob within 6 blocks that is
aggressive and targets him (a neutral mob only if it does); a creeper swelling within 7 or TNT with 40 ticks or fewer within 8;
(PvP on) a player within 4 who looks at him and has swung in the last 10 ticks. A threat not warned of in the last 40 ticks sets
`Live.SENSING` for 12 ticks (the pose) and sends `SpiderSensePayload(ids, strong)` to him alone (`strong` when a hit is
less than 12 ticks away): the client outlines the threats through walls (`EntityGlow`, `0xDDEEFF`) for 1.5 s, flashes the
screen edge, draws ripples and an arrow to the strongest threat, and the tingle `spider_sense` plays to him alone. It costs
nothing and changes nothing; with the toggle off nothing is sent. With the Black Eyes the range is x`blackEyesSenseFactor`
(2), the interval 2 ticks, and every hostile in range is listed and outlined. `SpiderSense.warn(player, source)` is public so
a mind-reading threat could be added later.

### 14.9 Iron Spider systems

Look `iron_spider` only ("Only the Iron Spider suit has legs"; the HUD says `Iron Spider only`).

**Spider Legs (B, toggle).** Four gold-and-black legs unfold from the gold circle on his back (14 ticks, sound
`spider_legs`, folding is 8 ticks at a lower pitch). While out: no fall damage, `legsReachBonus` (2) more reach, hunger
`legsExhaustion` (0.01) a tick, a crawl 25% faster, and every `legsInterval` (20) ticks the nearest **threat** within `legsRange`
(4.0) is jabbed for `legsDamage` (5.0) with a little knockback (`enhanced:spider_legs`, in the `melee_blows` tag). They
fold when the look changes, when the nanites are scattered or when B is pressed again. The server forces the toggle off in
any other look.

**Instant Kill (Sneak + B).** Needs the legs out. **Pressed twice within `instantKillConfirmTicks` (60)**: the first press
says "Instant Kill: press Sneak + B again to confirm". It needs `cooldownUntil(4)` passed and a non-empty stomach. On:
`Live.INSTANT_KILL`, the lenses close to two red slits, sound `instant_kill_on`, and for up to `instantKillMaxTicks` (200) every
`instantKillInterval` (5) ticks each of the four legs strikes one threat within `instantKillRange` (5.0), nearest first, for
`instantKillDamage` (1000), with a red-gold spark and `leg_strike`. Hunger `instantKillExhaustion` (0.08) a tick (about 4 food
points in all). It ends by itself, with Sneak + B, when the legs fold, at 0 hunger or when blocked; then `instantKillCooldown`
(600). **A threat** (`SpiderServer.isThreat`): a hostile `Enemy` mob that is alive, not owned or tamed; a neutral mob only if
it targets him; never a villager, golem, pet, animal, boat or armour stand, a Loki illusion, nor a player unless
`instantKillPlayers` (default off) and PvP is on. A creature in `enhanced:instant_kill_immune` (default `#enhanced:bosses`
and `#enhanced:boss_minions`) or for which `BossFights.isMinion` is true takes only `legsDamage`: four legs would clear a
boss's finite army in seconds. A complete Chaos Shield between a leg and its victim stops the strike (it pays its damage,
capped at 5). **Magneto:** `Telekinesis.isMagnetic` answers true for an Iron Spider, and the Armor and Weapon Strip
(`MagnetoAbilities.disarm`, via `SpiderServer.onStripped`) scatters his nanites for `ironSpiderStripTicks` (200): the suit's
modifiers, the legs and Instant Kill are off and "The nanites are scattered".

### 14.10 Black Eyes

**N**, a toggle. Needs `now >= cooldownUntil(5)` and at least 6 food points (Creative excepted). On: black lenses with a dark
red glint, the heartbeat `black_eyes_on`, wisps. **N again calms it** (no crash, half the cooldown). While it lasts (at most
`blackEyesMaxTicks`, 600; the end shows in `cooldownUntil(5)`), as `enhanced:spider_eyes_*` modifiers: speed +60%
(`blackEyesSpeedBonus`, ADD_MULTIPLIED_TOTAL: 1.15 x 1.6 = 1.84 times the normal speed; climbing 1.6 times, zip and swing
about 1.26 times, as they use the square root), melee damage +4 (`blackEyesStrengthBonus`; bare fists with the +50% attack
speed hit 6 times a second for 7: 42 a second, level with Magneto's shrapnel, the 3.0 review lowered it from +6), attack
speed +50%, jump +25%, knockback resistance 0.3, another -0.25 on fall damage; organic webs in every look; a wider spider-sense;
hunger `blackEyesExhaustion` (0.05) a tick. **The price:** when it runs out or the stomach is empty he **crashes**: Slowness II
and Weakness I for `blackEyesCrashTicks` (200), "The surge passes. You are drained.", and `blackEyesCooldown` (600).
**Stress surge (off by default):** if `blackEyesPanicHealth` is above 0 and his health falls to that, the eyes switch on by
themselves and cannot be calmed for 5 seconds. It never switches on otherwise and gives no night vision.

### 14.11 Interactions

| Rule or hero | What happens |
|---|---|
| One power at a time | The spider turns a hero away. Losing the power (`SpiderServer.endAll`, death, logout, dimension change, server stop) drops every web, zip, swing, cling, mode and modifier; the power, the look and the three toggles survive death. Web blocks already placed vanish by their own tick. |
| The Hex (Wanda) | He bounces off the wall like every hero: a zip, swing or cling it throws ends. A zip across the wall is refused at the press; a shot or burst passes the wall. |
| The Chaos Shield | A complete shield is solid to his webs, zips and legs (2 for a shot, 0.5 for a strand, the damage of a strike capped at 5); one that is still forming stops nothing; with PvP off the web costs the shield nothing. He cannot cling to a shield. |
| Magneto | The Iron Spider is metal: he can be lifted (30 s for a player) and his nanites scattered (14.9). The other suits are cloth. |
| Mjolnir's pin | Every key does nothing, every web ends. Mjolnir cannot be webbed. |
| Billy's Spellbound | `blocked`, speed factor 0: a zip or swing ends and a cling holds him in place. |
| Loki | His duplicates dissolve at a web or a leg and are never a threat; a shapeshifted Loki is webbed as himself. |
| Quicksilver Time | The slowed zone, Slowness, Webbed and Spellbound slow his crawl, zip and swing through the speed attribute. |
| Telekinesis | Held, he is `blocked`. Spider-sense warns of a held player's slam. |
| Vision | A phased Vision is nothing to web; a dense one can be webbed. |
| PvP off | Webs, bursts and kicks do not affect players; Instant Kill never targets one. |
| Bosses | `enhanced:web_immune` and `enhanced:instant_kill_immune` default to `#enhanced:bosses` (and the latter `#enhanced:boss_minions`); minions are webbed normally but take only `legsDamage`. No web block inside a protected boss arena. Wall Crawl and zips work on any block. |
| mobGriefing | Nothing he does breaks or changes a block except placing self-removing webs into free air. |
| Creative and Spectator | Creative: everything works and costs nothing. Spectator: every key is ignored. |

### 14.12 What others see, the HUD and the animation

His **client side** is `SpiderMotion` (his own movement, in `PlayerTickEvent.Pre`, with prediction: a zip or swing starts
at once on his own aim, the server's `ZIP_START` confirms it or its anchor takes over if it differs by more than a block,
an unanswered prediction is dropped after 12 ticks), `SpiderEffects` (the web lines, splats and the FOV kick), the layers
`SpiderLegsLayer` (`LegIk`, a two-bone solve, three rigid segments of 9, 10 and 7 pixels, planting on the wall, ceiling or
ground, striking on `LEG_STRIKE`) and `BlackEyesLayer` (`spider_eyes_black_<look>` and `spider_eyes_kill_iron`, the second
pass drawn glowing), and `SpiderSenseClient` with `SpiderSenseOverlay`. Everyone near sees the strand to a shot, the 24 strands
of a burst, the web from his hand to the anchor for as long as a zip or swing lasts (then it detaches, flies to the anchor and
fades), the splat decals (20 ticks) and, on a wall, a soft stick at each hand.

**HUD** (`SpiderClient.hudStatus`): R `burst 9.5s` (grey) or `organic` (aqua); G `zipping` or `swinging` (aqua); V `ON` or `OFF`,
then `clinging`, `sense OFF` and `DANGER!` for a second after a warning; B `Iron Spider only`, `nanites scattered`, the legs
`ON` or `OFF`, `confirm?`, `INSTANT KILL 7.5s` (bold red), `recharging 21.0s`; N `ON 18.4s` (red) or `recharging 24.0s`.

**Animation.** Seven stances (`SPIDER_CLING_WALL`, `_CLING_CEILING`, `_SURGE`, `_KILL`, `_ALERT`, `_ZIP`, `_SWING`) with their
rows in `SpiderPoses`: against the wall facing it, upside down on the ceiling (the body flips about a pivot 0.9 blocks up),
the hunched trembling surge, the menacing kill stance, the alert guard, the zip lean, the swing with the knees tucked; a
crawl cycle of one stride per 1.1 blocks. Fifteen gestures (`SpiderGestures`, `SpiderEdges`): shoot, burst, zip start,
swing start, crawl on and off, sense toggle, alert, legs out and in, kill on, surge on, the landing (a bend of the knees after a fall
of a few blocks, a three-point landing with a ring of motes after a long one), the leap off a wall and the arrival of a zip. His own key press acts before the server answers (`SpiderActing`). In first person the hands reach up for a wall, thrust
out for a shot, throw wide for the burst and shrug when the legs come out. Flourishes (`SpiderGestureFx`): motes at the
shooting hand, the burst ring, black-red wisps at the head for the surge, gold sparks at the leg roots, a red flicker for Instant
Kill, two pale rings for the sense; thinned by the client's *Flight effects* option, with *Reduced motion* the FOV kick and the
screen pulses are off. His walk and sprint have a character of their own and a lope while the surge lasts. He does not fly, so
he has no flight profile.

### 14.13 Colours, sounds and files

The webs are always silver-white (`0xF2F6FA`, shimmer `0xBFD3E6`; organic `0xF4EFE3`, `0xE7DCC4`); spider-sense is core
`0xDDEEFF`, ring `0x8EC5FF`; the Black Eyes are black `0x0A0305` with a glint `0x8A1020` and aura wisps `0x1A0608`; Instant Kill
is `0xFF2A2A` with gold `0xFFC247` at the leg tips; the Iron Spider's look change is a nano-sweep in gold `0xC9A66B` and crimson
`0x8E1B22`; the Radioactive Spider glows `0x7CFC00`. `Power.SPIDER_MAN` is spider red `0xDC2B2F` with a web-silver glow
`0xDCE6F2`; the looks' accents are `0xC9232D` / `0xB9CDE8` (homemade), `0xC9A66B` / `0xE7D8A8` (Iron Spider) and `0xD9381E` /
`0x5BB6D6` (Brand New Day). Thirteen sounds, synthesised from scratch with the mod's toolkit (mono, 44.1 kHz): `web_shot` (the
"thwip": a sine glide from 2400 to 700 Hz with a click, an air layer and a thump), `web_zip` (a thwip, then a rising reel with a
whoosh, a string twang and a closing tck), `web_burst` (24 miniature thwips, a whump and a shower), `web_splat`, `web_stick`,
`web_swing` (the loop `web_swing_loop`: a taut-web creak), `spider_sense` (a trembling 1760 Hz tingle with a swell and pings),
`black_eyes_on` (an inhale, a double heartbeat and a growl) and `black_eyes_off` (an exhale), `spider_legs` (ratchet chirps and
a shing), `leg_strike`, `instant_kill_on` (a descending three-note alert and a sub-bass drop) and `spider_bite`; each with English
and Hebrew subtitles. Vanilla's cobweb break sound is used for the web block and the shared awakening sound for the awakening.

The damage types `web_kick` and `spider_legs` are in the tag `enhanced:melee_blows` (the Chaos Shield's melee rule, because the
legs are his hands). Entity tags `web_immune` and `instant_kill_immune`, block tag `cannot_cling` (empty). Textures: the three
suits (`entity/suit/spider_*`), the five eye overlays and the legs (`entity/spider_*`), the web splat (`effect`), the Spider Web
block, the animated jar of the item (4 frames, a green pulse) and the two effect icons.

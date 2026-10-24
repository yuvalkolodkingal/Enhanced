# Apocalypse

En Sabah Nur, "the first mutant", worshipped as a god for five thousand years, buried under his own pyramid and woken in Cairo in 1983 (*X-Men: Apocalypse*). In the mod he is a **boss**: a three-block giant in verdigris armour, wrapped in a **golden energy shield that stops everything except mind power**: the Chaos Beam and the thrown things of the **Scarlet Witch**, and the Burst of the **Phoenix**. He fights with **sand** and violet portals, calls **his Horsemen**, and when he falls, everyone who fought gets **the Essence of En Sabah Nur**: hold it for three seconds with no power and you become **Apocalypse**, with sand, levitation, a portal, a golden shield of your own and the power to make your friends stronger.

Hebrew: [עברית](Apocalypse-HE)

![The Phoenix's burst](images/030-phoenix-burst.png)

## The quick version

* **To wake him** you need a **Capstone** (a Nether Star, five Gold Blocks and a Lapis Block), **sand under your feet, daylight and open sky**, and **a Scarlet Witch or a Phoenix within 64 blocks**: without one nobody could break his shield, so he does not wake. **Sneak + right-click** the sand with the Capstone.
* **His shield is the whole fight.** While it is up, **everything but mind power does exactly nothing to him**: no damage, no knockback, nothing. Arrows, swords, Mjolnir, Thor's lightning, Magneto's shrapnel, the Mind Stone, tamed wolves, TNT: all turned away. Only the **Scarlet Witch's Chaos Beam and what she throws with telekinesis**, and the **Phoenix's Burst, her throws and her mind powers**, wear it down, and **their hits are critical**: twice as strong.
* **When the shield breaks, it is everyone's turn.** For twelve seconds he kneels and **takes damage from everyone** (a quarter more than usual), down to a floor. Then he gets up, a new, stronger shield forms, and the next phase begins. Three shields, three phases.
* **He never heals.** His health never comes back (his shield still knits and re-forms). **Wanda's and the Phoenix's mind power hits him critically**, on his shield and on his body; everyone else hurts him much less. See below.
* **He is weaker in 3.2.** One master knob, `apocalypse_boss.powerFactor` (0.65 by default), cuts everything he and his Horsemen deal by a third, makes him attack more slowly and call fewer helpers, and his first shield is a third weaker (100 instead of 150). See [How strong he is](#how-strong-he-is-the-master-factor).
* **Everything he does has a warning**: a pose, a glow and a sound, and a moment after which the attack stops following you. Learn the tells, leave the ring, jump the wave.
* **When he falls, everyone who fought earns the Essence.** Hold it for three seconds with no power to become Apocalypse.

## Waking him

### The Capstone

"The golden tip of the pyramid. The sun feeds it."

```
. S .
G L G
G G G

S = Nether Star (it takes a Wither)
G = Gold Block (the golden veins)
L = Lapis Block (the sapphire soul)
```

The Capstone is never found in a chest and he is never found asleep anywhere: you make him, and the recipe is mid-game. It is fireproof and does not stack. A plain right-click only says "Plant it in the sand, in the sun".

### The ritual

**Sneak + right-click on a block with the Capstone in either hand.** All the checks come first; if one fails, a red message in the action bar says which, and **nothing is used up**.

| Why it refuses | What it says |
|---|---|
| The server turned him off | "En Sabah Nur sleeps (switched off on this server)" |
| The difficulty is Peaceful | "Apocalypse cannot wake in Peaceful" |
| Another boss is fighting | "(the boss that is fighting, for example Thanos) is already awake at (x), (z)" |
| Not the Overworld (a server can add worlds) | "He does not sleep in this world" |
| It is night, or there is a roof over the block you clicked | "The Capstone feeds on sunlight: it must be day, under the open sky" |
| The block is not sand or sandstone | "He sleeps under sand" |
| Not enough room (a circle of 8 blocks round the click must have 12 blocks of free air above it, no liquid and at most 40 percent of its columns blocked) | "Not here: he needs room" |
| The arena would reach past the world border, or the spot is protected (spawn protection, a claim) | "Not here: the arena must fit inside the world border", "This ground is protected" |
| No Scarlet Witch and no Phoenix within 64 blocks (Creative and Spectator ones do not count) | "Nothing here can break his shield: bring the Scarlet Witch or the Phoenix" |

Then the Capstone is used up (not in Creative) and **eight seconds** pass:

1. A **column of gold light** drives into the ground, a deep rumble swells and everyone within 96 blocks is told "The ground trembles".
2. **The sky and the air turn amber** (for everyone within 64 blocks of the arena), and a **wall of sand** rises round a circle of 24 blocks radius, 40 blocks high, whirling. The camera trembles a little for those inside.
3. A **violet portal** opens over the middle of the circle.
4. A flash, and **he steps out**, facing whoever woke him. For the next three seconds he is untouchable, his shield knits itself round him from the ground up, and his eyes light white. He says "Who wakes the First One?" and then "Only the strong will survive."

An operator can start the fight at their feet, with no Capstone, with `/enhanced boss start apocalypse force` (see [Commands](Commands)): it skips every check of the ground, the sky and the need for someone who can break his shield, but not the server option that switches him off, Peaceful or the one-fight-at-a-time rule.

If the fight fails for a reason that was not the players' doing (the server stops, the world goes Peaceful, an operator stops it, he withdraws because nobody could break his shield), **the Capstone is given back**. After a wipe, an abandoned fight or the time limit it is not.

## The shield: the one rule

> **He takes damage from mind power. Nothing else touches him.**

Here is what counts, in one picture:

| What you hit him with | While the shield is up |
|---|---|
| **Scarlet Witch: the Chaos Beam** (R) | **Wears the shield down, critically**: 10 a hit (5 twice over), two hits a second: **20 a second** |
| **Scarlet Witch: telekinesis throws** (a creature, an item or a block that hits him) | **Wears it down, critically**: twice the damage of the hit (12 for a light thing, 30 for a heavy one); a block borrowed from his own arena wears it half as much as other thrown things (0.5, then ×2) |
| **Phoenix: the Burst** (N) | **Wears it down five times over, and critically**: a Burst of 30 takes 300, **enough to break any of the three shields in one go when she fights him alone** (with two fighters shield 1 has 130) |
| **Phoenix: her throws** | Wears it down, critically, as the Scarlet Witch's |
| **Phoenix: Mind Control and the Psionic Field** | A **strain** on the shield, critical too: 50 and 120 points, even if they are refused, once a cast (pressing the key again before her power has recharged strains it no more) |
| Billy's Hex Beam (and his old Hex Bolt), Vision's Mind Stone beam | Nothing, *unless the server owner adds them* (see [Server Configuration](Server-Configuration)) |
| Magneto, Thor, Loki, Quicksilver, Tommy, Ultron, Thanos, Spider-Man, the Darkhold's purple beam, any sword, arrow, TNT, fire, potion, tamed animal | **Exactly nothing**: no damage, no knockback, a golden ripple and a chime where it hits |

The shield has **strength** (for one fighter: **100, then 120, then 144** for the three shields; more fighters make it stronger: for two, 130, 156 and 187; for three, 160, 192 and 230). When the strength reaches zero it **shatters**, and for a moment he is helpless:

* **A window (12 seconds).** He kneels and attacks nothing for the first four seconds. For the whole twelve, **everything can hurt him**: Wanda's and the Phoenix's mind power twice as much (critical), everything else only 0.4 times as much, and then a quarter more because of the window, armour still counting; no single hit can take more than 12 percent of his health.
* **A floor.** In the first window his health will not fall below **two thirds**, in the second below **one third**: nobody can kill him early, however much damage they bring. Reaching the floor ends the window and starts the next part of the fight.
* **If nobody does enough**, the shield re-forms at full strength after a short pause. **He does not heal meanwhile: his health never comes back.**
* **The shield mends itself** slowly (2 percent a second) once ten seconds have passed without mind power touching it, so a steady beam is never wasted.
* The **third shield has no window**: its break is the end of his armour (below).

The Chaos Beam hits every tick, but the shield counts one hit from each attacker every half second, so a Scarlet Witch holding the beam wears it down by 5 every ten ticks, the same as against anything else.

The boss bar shows his health in blue (the notches are the floors) and the shield as a second, yellow bar: its strength, "broken" with the seconds left in a window, or "forming".

## He never heals; Wanda and the Phoenix hit critically

**He never heals.** In 3.0 he healed while his shield was up and in his true form; now his health **never comes back** (a splashed potion of Healing does nothing to him either). His **shield** still knits (it mends 2 percent a second after ten seconds without mind power, and re-forms at every phase), and the Escape is unchanged: if you don't stop it he vanishes and returns ten seconds later at 40 percent of his health with a fresh shield 3, a set piece you can stop, not healing.

**Critical hits.** A hit of the **Scarlet Witch's or the Phoenix's mind power** (the Chaos Beam, a telekinetic slam, the Phoenix Burst, and the strains of Mind Control and the Psionic Field) is a **critical hit** on the boss, and it is heard: a heavy glassy crack with a low gong under it (the subtitle says "Critical hit on Apocalypse"), across the whole arena, at most once per attacker every half second:

| Where | A critical hit | Everyone else |
|---|---|---|
| **His shield** (up) | **twice** as strong | mind power of the others (if the server adds them): as before; anything else does nothing |
| **His body**, when his shield is down (a window, the stagger of an interrupted Escape, the true form) | **twice** as strong | **0.4 times** as strong |

So, for one fighter, with the defaults: shield 1 (100) takes **10 beam hits instead of 20**; a Phoenix Burst of 30 takes 300, breaking any of the three shields (100, 120, 144) at once; Mind Control strains 50 and a Psionic Field 120. In a window, Wanda's beam of 5 takes 12.5 off his health before his armour counts (5 × 2 × 1.25 for the window), and a sword of 7 takes 3.5 (7 × 0.4 × 1.25). In the true form the same without the 1.25. The cap and the floors still hold, so no phase is skipped. A critical blow also counts **double** toward interrupting the Escape (4 percent of his health in the channel), and Thor's lightning interrupts it whatever its damage. The Phoenix's fire finish is unchanged.

**You see and hear it:** a burst of sparks at the hit (20 critical-hit and 10 enchanted-hit particles), a heavy glassy crack, and "Critical hit! ×2" on the attacker's action bar (at most once every half second per attacker). The golden ripple of a hit that does nothing is unchanged. Only the **boss** is changed: the Apocalypse **hero's** own shield and healing are as they were.

## The fight, phase by phase

| Part | What happens |
|---|---|
| **Phase 1: The God Awakens** (100% to 66%) | Shield 1. Lances, Wave, Spikes, Boulders, Portal Step and the Backhand. |
| **Window 1** | The shield breaks: he kneels, 12 seconds, floor at 66%. After the kneel he fights on with only the Lances, the Backhand and the Spikes, at half speed. |
| **Transition 1** (6 s) | He is untouchable and rises. **His Horsemen** (three by default) step out of portals. Shield 2 forms. "Rise, my Horsemen." |
| **Phase 2: The Horsemen** (66% to 33%) | Shield 2. **Entomb, Mind Crush and a Tornado** join his attacks. Window 2, floor at 33%. |
| **Transition 2: the Cairo Collapse** (8 s) | He floats up, the sky goes dark ochre, and **three rings of sand** cross the arena. Any Horsemen still alive crumble. Shield 3 forms. "Everything they've built will fall..." |
| **Phase 3: Unmasked** (33% to 5%) | Shield 3, stronger still. **Two Tornadoes at once** (three with `powerFactor` 1.0). When it breaks there is **no window**: |
| **The armour falls** | His plates fly off him (the collar, the pauldrons, the chest plate, the gauntlets, the crown of his cap), the shield is gone **for good**, and in his frail **true form** (smaller, faster, grey-blue, with a cracked cap glowing gold) **everything can hurt him**, with no window (Wanda's and the Phoenix's mind power critically, everyone else's at 0.4); his health stops only at 15 percent (until he has tried the Escape) and at 5 percent, where he kneels. He does not heal there either. |
| **The Escape** (once, at 15%) | He raises a hand and **a violet portal grows over him for five seconds**: "No!" **Thor's lightning stops it** (as Storm's did in the film), and so does **taking 4 percent of his health in damage** during the channel (a critical blow counts double), or **a Hex wall**. If you stop it he is staggered for six seconds and can never try again. If you do not, he vanishes, and **returns ten seconds later healed to 40 percent with a new shield 3**: a heavy setback, never a loss. |
| **The end** (5%) | He becomes untouchable and kneels: "All is revealed." Then he dies: **in the Phoenix's fire, layer by layer**, if a Phoenix dealt Burst or throw damage in the last 15 seconds, otherwise in a column of sand. He drops nothing: the reward is the Essence (below). |

The whole fight is meant to last **six to ten minutes** for two or three well-equipped players (Wanda alone: eight to twelve, mostly dodging).

## His health and the numbers

* **900 health for one fighter**, and half as much again for each extra one (up to six counted): 1,350 for two, 1,800 for three, 2,250 for four. Creative and Spectator players do not count. His armour is 10 (toughness 6) in the armoured form, none in the true form.
* **His biggest hit is 7.8** (the Backhand); most are 4 to 6.5. These are the options' numbers times `powerFactor` (see below). Every damage scales with difficulty as a mob's does.
* He **cannot be pushed, set on fire, poisoned, withered, drowned or suffocated**, and **every status effect is refused** (except Glowing). The Hex bounces him, Mjolnir cannot pin him, Billy's Freeze and the Scepter do nothing, nobody can shapeshift into him, and the Phoenix cannot read or possess him. **While his shield is up only mind power can hold him** (the Scarlet Witch, the Phoenix); in a window anyone's telekinesis can, as a boss (3 seconds, Billy 1.5, Magneto 4.5 while he wears his armour; then not again for 12 seconds). A block of his own arena thrown at his shield wears it half as much as other thrown things.

## How strong he is: the master factor

In 3.1 he was too strong for most worlds, so 3.2 turns him down with **one master knob**: `apocalypse_boss.powerFactor`, **0.65** by default (allowed 0.1 to 1.0; **1.0 is the 3.1 fight**). It multiplies:

* **Everything he and his Horsemen deal**: Lances, Wave, Spikes, Boulders, the Backhand, the Portal Step's shock, Tornadoes, the rings of the Cairo Collapse, the Entomb's damage a second, and the Horsemen's added damage and their Gale, Dash and Dive.
* **How many there are**: Lances (5 become 4), Horsemen (4 become 3) and Tornadoes in phase 3 (3 become 2). Rounded up, never fewer than one.
* **How often he attacks**: the pause between two attacks and every attack's own cooldown last 1 divided by the factor times as long (about 1.5 times as long at 0.65).

It does **not** touch his health, his armour, his shield, the length of his windows, the sand wall's 1 damage, the lengths of his tells or how fast his projectiles fly, and the Apocalypse **hero** (a player) is as he was. His shields, windows and a few hits that were plainly unfair have their own, lower, defaults:

| Option | 3.1 | 3.2 | Now, after the factor |
|---|---|---|---|
| Shield 1 / 2 / 3 for one fighter (`shieldStrength`, `shieldGrowth`) | 150 / 188 / 234 | 100 / 120 / 144 | (the factor does not touch shields) |
| Extra shield for each more fighter (`shieldPerExtraPlayer`) | 0.35 | 0.3 | for three fighters 160 / 192 / 230 |
| A window (`windowTicks`) | 10 seconds | 12 seconds | |
| The stagger at its start (`staggerTicks`) | 3 seconds | 4 seconds | |
| The shield mends itself (`apocalypse_shield.shieldRegenFraction`, `shieldRegenDelayTicks`) | 3 percent a second after 8 seconds | 2 percent a second after 10 seconds | |
| The Backhand (`backhandDamage`) | 16 | 12 | 7.8 |
| A Boulder (`boulderDamage`) | 11 | 9 | 5.85 |
| A ring of the Cairo Collapse (`ringDamage`) | 12 | 10 | 6.5 |
| The Entomb (`entombTicks`) | 5 seconds | 4 seconds | 0.98 damage a second |
| Nausea of the Mind Crush (`mindCrushNauseaTicks`) | 6 seconds | 4 seconds | |
| A Horseman's health and added damage (`horsemanHealthFactor`, `horsemanDamageBonus`) | 2 times, +4 | 1.5 times, +3 | +1.95 |
| Lance / Wave / Spike / Portal shock / Tornado (`lanceDamage`, `waveDamage`, `spikeDamage`, `portalShockDamage`, `stormDamage`) | 6 / 10 / 9 / 6 / 2 | the same | 3.9 / 6.5 / 5.85 / 3.9 / 1.3 |

Want the old fight back? Set `powerFactor` to 1.0 and put the other numbers of the first column back. Want it even gentler for a younger player? Lower the factor: at 0.4 he hits for 40 percent and attacks about two and a half times as slowly. The numbers in the attack table below are the **defaults, after the factor**.

## His attacks, and their tells

Every attack that can hurt has a **tell**: a pose, a glow and a sound, then a **fixed aim moment** after which it does not follow you. His sand is **sand, not metal**: Magneto's Magnetic Shield does not catch it.

| Attack | From | The tell (and when it locks) | What it does | How to meet it |
|---|---|---|---|---|
| **Sand Lances** | phase 1 | His right hand rises and the fingers pinch; **four thin ochre lines** run from his palm to your chest (locked 6 ticks before it fires) | Four lances in a flat fan, **3.9 damage** each | Strafe and jump. The Chaos Shield stops each. Billy and the Phoenix can catch them in the air and throw them back |
| **Sand Wave** | phase 1 | Both arms sweep low; a **faint lane** marks the ground, 4 wide (locks just over halfway) | A rolling ridge, 1.1 blocks tall: **6.5 damage**, knockback, Slowness II | Leave the lane. **Jump it** (the wave is lower than a jump's peak). Fly |
| **Sand Spikes** | phase 1 | A fist rises and slams; **an amber ring on the ground under each of up to four players**, tracking, then locked | Spikes erupt in each ring: **5.85 damage**, launched upward | Leave the ring. Fly higher than three blocks |
| **Boulder Barrage** | phase 1 | Both arms rise; **three blocks of sand tear out of the ground and orbit over his head** with a growing hum; amber lines to the targets | Three boulders, one at each of up to three players: **5.85 damage** | Sidestep. The Chaos Shield stops them. Billy catches them and throws them back |
| **Portal Step** | all | Violet shards swirl at his feet and **a second ring opens at the exit**, usually behind the player farthest from him | He steps out of the ring: an arrival shockwave, **3.9 damage**, knockback | Watch the exit ring. **A Hex wall refuses it**: he walks instead |
| **Backhand** | all | The arm is drawn across the chest (8 ticks) | **7.8 damage**, strong knockback | Step back or aside |
| **Entomb** (the grasp) | phase 2 | His hand reaches toward one player (the one who wore his shield down most); **a stripe of sand crawls across the ground to their feet** and locks 10 ticks before the hand closes | If you are still on that spot: **you are entombed**: a cocoon of sand, four seconds (80 ticks), unable to move, jump, fly, attack, use items or use powers; **about 1 damage a second (0.98) but never below two hearts** | **Keep moving** after the lock, or fly. **Your friends break the cocoon** (20 health: three sword hits). A complete Chaos Shield is skipped. A phased Vision is intangible |
| **Mind Crush** | phase 2 | Two fingers to his temple, his eyes flash white-violet and **a violet ring contracts** from 28 blocks to nothing round him (2 seconds) | Everyone within 28 blocks gets **Nausea (4 seconds), Slowness and Blindness** | **Magneto's helmet turns it all away.** **A Phoenix who touches him with Mind Control or the Psionic Field during the tell interrupts it** (the film's duel of minds). The Chaos Shield does not stop it |
| **Tornado** | phase 2 (one), phase 3 (two) | Both arms rise, the sky above him darkens and **dust spirals up from the ground** at one to three spots | Funnels that last eight seconds and **drift toward the nearest player**, slower than you can walk, hurting (1.3 every half second) and lifting everything inside | Outrun them or fly out of the column. The Chaos Shield stops the damage |
| **Cairo Collapse** | transition 2 | The ground ripples outward; he floats up, arms wide | **Three rings of sand** cross the arena, 1.1 blocks tall: **6.5 damage**, strong knockback | Jump or fly over each ring (30 ticks between them). The Chaos Shield stops each |

He chooses his next attack by weight among those that are ready and in range, never the same twice in a row. **He walks** (no flight), keeps 8 to 14 blocks from you, closes in when nobody is near and backs off when you press him; in the true form he is 28 percent faster. When he has found no path to anyone for three seconds, he steps through a portal.

## The Horsemen

"Maybe empowering nearby creatures": in the film he **enhances** Storm, Psylocke, Archangel and Magneto. In the mod, at Transition 1 he enhances **Husks** (vanilla desert mobs): one more than there are fighters, up to three by default (`horsemenMax` is 4, times the master factor, rounded up; at `powerFactor` 1.0 all four come), in this order. Each is named, glows, has one and a half times a Husk's health, about two more attack damage (1.95), a little more speed and armour, and **drops nothing and gives no experience**: they cannot be farmed.

| Horseman | Gear | His one trick |
|---|---|---|
| **War** | iron helmet, chestplate and axe (so Magneto can lift it, strip it and his shield catches its shards) | **Shard Volley** every five seconds: he raises a hand and three iron shards hover, then fly in a fan, 4 damage each |
| **Famine** | none | **Gale** every six seconds: white lightning crackles on his hands, then a bolt falls beside you and a gust blows you back (1.95 damage; Thor is not hurt by the lightning, but the wind still pushes) |
| **Pestilence** | iron sword | **Psi-Blade Dash** every five seconds: the sword glows violet, then a six-block leap at you: 4.55 damage and Slowness |
| **Death** | none | **Dive** every seven seconds: he springs up, hangs, and dives with a slam: 5.85 damage in a radius of two and a half blocks. (With fewer than four, in a fight big enough for four, Death takes the place of the third: War, Famine and Death.) |

They live until killed, or until Transition 2, when they crumble to sand. They are ordinary creatures: Wanda can lift them, a Phoenix can take one over (a possessed Horseman fights for her), Billy can freeze them. To a **Magneto** he says, once, "Join me, Erik." (it means nothing more).

## The arena: what you cannot do

From the moment the Capstone is planted, a circle of 32 blocks round it (the arena and 8 more) is protected:

* **No block can be placed or broken, no fluid, bucket, piston or TNT** works, and **explosions break nothing**. **Telekinesis only borrows blocks** (the hole mends, the block crumbles). He never places or breaks a block himself, so mobGriefing changes nothing. Whatever the fight does to the air is an effect, not a block.
* **The wall of sand** (4 blocks outside the arena) is solid to players and to what telekinesis throws: whoever touches it is pushed back in, once a second, for **1 damage that cannot kill**, and flight stops at the top of the wall (40 blocks up).
* **No Ender Pearl, Chorus Fruit, Bifrost, Vanish, Tesseract jump, Witches' Road or Vibrate** takes anyone out of the circle, and nothing takes anyone in from another dimension's portal.
* **Anyone more than 8 blocks below the surface** is lifted back up by a violet tractor of sand ("The sand lifts you back"), so a hole is no hiding place.
* **A dead fighter's drops never despawn while the fight lasts**, and get a fresh five minutes when it ends.
* **One boss fight runs at a time** on the whole server.

## When it goes wrong

* **Everyone dies:** if nobody is alive in the arena for 15 seconds, he says "You are not worthy yet." and **withdraws** through his portal, the Horsemen crumbling to sand. No reward, no refund. Creative players can't die, so an operator must end a fight with only them.
* **Everyone leaves:** nobody within 64 blocks for 90 seconds: the fight is abandoned.
* **Nobody can break his shield:** if no Scarlet Witch or Phoenix is alive in the arena for 60 seconds while his shield is up, he withdraws and **the Capstone is given back**.
* **25 minutes:** the longest a fight can last. After it he withdraws (no refund).
* **The server stops, or the world goes Peaceful, or an operator stops it:** the fight ends and the Capstone is returned (as a gift at your next login, if the server is stopping).
* **A crash** leaves no boss behind: he is not saved, and any Horseman that survives a restart is swept away as an orphan the moment it loads.

## The other heroes against him

| Hero | Against the boss |
|---|---|
| **Scarlet Witch** | The one who breaks his shield. Her **Chaos Beam and telekinesis throws** wear it down, **critically**; in a window they hurt him twice as much as anyone's. She can **hold him even through his shield**, and throwing him wears it; she lifts the Horsemen and **a cocooned friend (the cocoon breaks: she frees him)**. The **Chaos Shield** stops every sand attack and the Horsemen's iron shards, but not the Mind Crush; his grasp skips her. Since 3.2 her shield is unlimited: it pays nothing and never breaks (`scarlet_witch.shieldUnlimited`; with it `false` the shield pays the damage, as in 3.1; `scarlet_witch.shieldUnlimitedVsBosses` `false` brings the old wear back against his blows only). Her **Hex** is a perfect arena: it bounces him, **refuses his Portal Step and his Escape**, and kills the Horsemen at the wall. |
| **Phoenix** | The other breaker. Her **Burst breaks any shield in one go when she fights him alone** (with more fighters it takes most of it), **critically**; her throws, Mind Control and Psionic Field wear and strain it (also critically); touching him interrupts the Mind Crush. He is immune to being read or possessed. **Her fire finishes him**: if she dealt the last blows he dies in orange-gold fire, layer by layer. |
| **Magneto** | His helmet turns away the Mind Crush entirely. His **Armor & Weapon Strip** on the boss in a window cuts his armour to nothing for that window and **adds five seconds** to it (once per window): "Erik and Jean dismantled it". The War Horseman is iron, so he can lift and strip it. He can hold Apocalypse in a window, until his true form (4.5 seconds), never while the shield is up; his strip and his pull tear War's and Pestilence's iron off them, and it crumbles (the Horsemen are never farmed). His shrapnel and thrown golems are *not* mind power: they hurt the body at 0.4. |
| **Quicksilver** | Quicksilver Time slows him fully for **four seconds**, then he **adapts** and is immune to it for half a minute, and answers by **entombing the speedster's feet** (the film). Slowed Lances and Boulders hang in the air like arrows. Blitz and punches do nothing while the shield is up, and in a window a Blitz does 12. |
| **Thor** | Mjolnir cannot pin him. Thrown weapons and fragments are **turned back to Thor, never destroyed**. The lightning beam is deflected while the shield is up, but **lightning interrupts his Escape**. An **entombed Thor** cannot use his abilities. |
| **Billy** | The Freeze does not touch him. **Telekinesis catches his Lances and Boulders in flight and throws them back** (they hurt him only in a window). He can hold Apocalypse only in a window. His Hex Beam does not wear the shield, and his Hex Barrier stops the sand's blows from the front (it breaks fast: 20 points at his first rank). |
| **Loki** | The Scepter does not confuse him, nobody can shapeshift into him. Duplicates draw the Horsemen, though he picks only players. |
| **Vision** | The Mind Stone beam is deflected while the shield is up (7.5 in a window). A **phased Vision cannot be entombed** and his Lances pass through. |
| **Tommy** | Vibrate cannot free him from a cocoon: no key works while he is entombed, so his friends must break it (or lift him out with telekinesis). The Whirlwind lifts the Horsemen. |
| **Spider-Man, Ultron, Thanos** | Webs and the Instant Kill do nothing to him, and the Instant Kill cannot one-shot a Horseman. Nothing of theirs is mind power. |
| **PvP off** | He is a mob: **his attacks hurt players whatever PvP says.** |
| **Creative and Spectator** | Never targeted, never hurt by him. A Creative player can hurt him but does not count as a fighter. |

## The reward: the Essence of En Sabah Nur

"A sapphire soul in gold veins. It looks for a body." It is never crafted and never found in a chest.

* **Who gets one:** at the end, **every fighter who stayed at least a minute and dealt any damage to him *or to his shield*** (so a Scarlet Witch who only wore the shield down is rewarded as much as the one who landed blows), and **whoever landed the last blow** whatever their time. Creative players do not (unless the server turns on `boss.creativeGetsReward`), and Spectators never do. Pets and tamed mobs credit nobody, so there is no reward by proxy. Each also gets **1,500 experience**.
* **Where it goes:** straight into your inventory. If it is full it drops at your feet, **yours alone for as long as it lies there** and never despawning. If you were dead or offline, it waits for you as a gift at your next login or respawn.
* **What it does:** a player with **no power** who **holds right-click for three seconds** is *moved into* the Essence: sapphire-blue wisps with gold veins spiral into you with a soft heartbeat, your skin turns blue, then a flash of lightning (it hurts nobody) and **you become Apocalypse**. Let go early and nothing is used up. A player who already has a power is told "Your body already holds a power" and nothing is used up (they can keep it, trade it, give it to a friend). An Apocalypse is told "You already are En Sabah Nur".
* **A server option** can also give a Phoenix Ember to a fighter with no power (`apocalypse_boss.dropsPhoenixEmber`, off by default): the film's Jean awakens here.

## Becoming Apocalypse: the hero

Five abilities on **R G V B N** (Sneak for the other ones). The keys and the numbers are in the server options; the defaults are here.

| Key | Ability | Does |
|---|---|---|
| **R** | **Sand Control** | **Sand Wave:** a wave of sand rolls from your feet along the ground, 18 blocks long and 4 wide, **8 damage**, knockback and Slowness II, each creature once. You need ground within 6 blocks below you ("No ground to pull sand from"). Cooldown 3 seconds. |
| **Sneak + R** | **Entomb** | Look at a creature or player within 24 blocks: sand crawls up its legs and **swallows it in a cocoon** for four seconds: it cannot move, jump, fly, attack, use items or use powers, takes 1 damage a second (never below two hearts), and its friends free it by hitting the cocoon (16 health). Players only with PvP on. Refused for bosses, for a phased Vision, for Creative and Spectator, and for a creature whose shield is up. Cooldown 12 seconds. |
| **G** | **Levitation** | A calm, slow flight (speed 0.08: Creative's is 0.05). A swirl of sand motes circles your feet. |
| **V** | **Portal** | Look at a spot within 40 blocks and step through a **violet portal** to it (on top of the block, or in front of it). You take with you **every tamed pet and teammate within 3 blocks** and **every other player within 3 blocks who is sneaking** (stepping in is consent). The Hex wall judges every traveller, a pinned one stays behind, and nobody leaves a boss arena. Cooldown 8 seconds. |
| **B** | **Apocalypse's Shield** (hold) | A golden hexagonal sphere grows round you. It takes half a second to form and **stops nothing until it is complete**; then it **stops every kind of damage except mind power**: arrows, tridents, blows, explosions (all of it), beams, lightning, magic, fire (falls, drowning, suffocation, starvation, the void and /kill are not stopped). Projectiles are neutralised before they hit (Mjolnir and Stormbreaker are turned back to Thor). It can be held **six seconds**, has **60 points of strength**, and what it stops **wears it down by half of the damage** (so it stops 120 points of arrows before it shatters). **Mind power wears it down in full**, the Phoenix's Burst five times over, and **when it breaks the rest of that blow comes through**. A Phoenix's Mind Control and Psionic Field strain it by 25 and 60 points, once a cast (with PvP on). Recharge 8 seconds after you let go (20 after it shatters or was held to its limit), and it costs a little food. |
| **N** | **Power Bestowal** | Look at an ally within 24 blocks (**a player, a tamed pet, an Iron Golem or a villager**, never a hostile creature: "You only enhance your own"): they are **Empowered** for a minute: +3 attack damage, +4 armour, +10 percent speed, half a heart every two seconds, and, if they are heroes, **their ability cooldowns recharge half as fast again** (1.5 times as fast), with a golden aura round them. At most two at once; a third replaces the oldest. Cooldown 20 seconds. |
| **Sneak + N** | **Sandstorm** | A **storm of sand rises round you** for six seconds, 7 blocks in radius (14 across), following you: every half second everything it may hurt takes **2 damage and Slowness**, **projectiles that enter it are slowed to half**, and the storm blots out the sight of those inside it. Allies in it are unaffected. Cooldown 30 seconds. |

**The body.** While you are Apocalypse you have **40 health** (twenty hearts), **armour 10** and toughness 4, and half knockback. Your **healing factor** mends half a heart a second after five seconds without damage. Your own sand never hurts you.

**Other heroes and rules.** The Hex bounces you like any hero, unless Wanda allowed you; Mjolnir's pin stops your levitation and your portal; your sand and storm ignore the wall of a Hex. Magneto's powers do nothing to you unless you wear or hold metal (you are flesh, and sand is not metal). The Chaos Shield stops your Wave and Sandstorm and the Entomb skips a complete shield. **With PvP off your Wave, Entomb and Sandstorm do not hurt, hold or slow any player** (mobs are always affected); your shield, portal and Bestowal still work. A Spectator Apocalypse cannot use any ability, and Creative players are never swept or entombed and pay no hunger.

### Looks

Press **H** to change look (**Sneak + H** goes back). The abilities are the same in every look.

| Look | What it is |
|---|---|
| **X-Men: Apocalypse (1983)** (default) | Teal-blue skin with blank white eyes, a domed gunmetal cap engraved with a leaf on the brow, ribbed black cables down both sides of the face, a wide pewter lace collar and shoulder plates, dark verdigris armour with a glowing cyan glyph on the chest, black gloves, and a hooded teal cloak. |
| **En Sabah Nur (3600 BC)** | The ancient god-king in gold: a falcon-beaked helm with red cabochon eyes, a broad collar of gold, teal, lapis and red, teal chevrons on gold armour, a pleated kilt and sandals. His eyes glow red. |
| **Your own skin** | Apocalypse's powers in your own skin. |

The golden shield, the portal's violet and the sand's ochre are the same in every look.

### What you see

* **The HUD:** each ability with its key and cooldown. The shield shows "forming", then its strength (green, yellow, red) and the seconds you can still hold it, then the recharge; **N** shows "storm 4.2s" in gold while the Sandstorm lasts.
* **Other players** see everything: your arms sweep low for the Wave, your hand reach out and **close** for the Entomb, spread wide for the Portal, rise to the chest and open for the Shield, hold out a palm to an ally for the Bestowal, and fly out wide and up for the Sandstorm. A ridge of sand rolls away across the ground, a stripe of sand crawls to the victim's feet and climbs it, and a rotating funnel of sand rises round the storm.
* **Flying** you are calm and upright, arms a little open, with sand motes at your feet and a ring of sand at take-off; you walk with the unhurried step of a being who has never needed to hurry.
* **Client options** (*Reduced motion*, *Hero animations*, *Flight effects*, in `config/enhanced-client.toml`) tone it down: no amber sky, no fog, no camera tremor, a wall that does not scroll.

## Sounds and music

Every sound is original, synthesised for the mod; none is the film's score. The ritual starts with a deep rumble, grinding stone and a choir chord rising to a low gong; his portal is a reversed crystalline shimmer and a soft whump; the sand has its own voice (a rolling hiss for the Wave, a stone crack for the Spikes, a pour and a squeeze for the Entomb, a roar of wind and grain for the storm); the shield rings like gold chimes when it forms, a glassy tink where it turns a blow, and shatters like glass over a sub-boom; his voice is a low breathy "ah" under every line he says in chat.

**The music** is a loop that plays while he is within 96 blocks of you (full volume within 64): a drone, a frame drum in 6/8, an end-blown flute on a Phrygian-dominant line and a choir. In the third phase a second, fuller loop takes over, with a second drum, rising strings and a faster pulse. It fades in over two seconds, quiets the game's own music, fades out five seconds after he is gone, and obeys the *Music* volume slider. With subtitles on, every sound effect has a caption in English and Hebrew (the music has none).

## For servers

All numbers are in the server config, in the sections `[apocalypse_shield]` (what counts as mind power: to count Billy's or Vision's, add `"billy"` or `"vision"` and also their damage types (`enhanced:hex_bolt`, `enhanced:hex_beam`, `enhanced:mind_stone_beam`) to the datapack tag `enhanced:mind_power`), `[apocalypse_boss]` (the fight) and `[apocalypse]` (the hero), and the numbers every boss shares (how many fights at once, the wipe and abandon clocks, the reward's minimum time, the experience, the time limit, the protection of the arena) are in `[boss]`. See [Server Configuration](Server-Configuration#apocalypse-the-boss-his-shield-and-the-hero).

* The master factor is `apocalypse_boss.powerFactor` (0.65; 1.0 brings back the 3.1 strength of his blows, his counts and his pace); his shields and windows are `shieldStrength`, `shieldGrowth`, `shieldPerExtraPlayer`, `windowTicks` and `staggerTicks`, and the damage of each attack is its own `...Damage` option. They are all in [Server Configuration](Server-Configuration#apocalypse-the-boss-his-shield-and-the-hero).
* His health never heals because `apocalypse_boss.regenPerSecond` and `apocalypse_boss.trueFormRegenPerSecond` are **0** by default (put a number back to let him heal). The critical hits are `apocalypse_shield.critHeroes` (which heroes), `critShieldFactor` and `critBodyFactor` (2.0), and `othersBodyFactor` (0.4); `apocalypse_shield.borrowedSlamFactor` is how much a borrowed block wears the shield.
* `apocalypse_boss.requireBreaker` can be turned off, so the ritual works with nobody who can break his shield (the fail-safe still withdraws him).
* `apocalypse.shieldOtherWear` set to **0** makes the hero's shield cost nothing for what it stops (the literal rule). **His own shield always uses the literal rule.**
* Nothing he does ever changes a block, so there is nothing to restore after a fight.
* No fight survives a restart.

## Questions

**Why does nothing hurt him?** His shield stops everything but mind power. Bring a Scarlet Witch or a Phoenix: only they can break it. See [FAQ](FAQ#why-does-nothing-hurt-apocalypse).

**I have no Wanda.** Then you cannot wake him: the ritual refuses, and if she dies mid-fight he withdraws after a minute and gives the Capstone back. (A server can switch the check off.)

**Does he break my world?** Never: he places and breaks no block, and the arena is protected from the moment the Capstone is planted.

## Related

[Scarlet Witch](Scarlet-Witch) · [Thor](Thor) · [Loki](Loki) · [Becoming a Hero](Becoming-a-Hero) · [Controls](Controls) · [Items and Crafting](Items-and-Crafting) · [Looks](Looks) · [Commands](Commands) · [Server Configuration](Server-Configuration) · [FAQ](FAQ)

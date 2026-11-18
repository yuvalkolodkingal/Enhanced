# Jim Hammond

The first Human Torch: the android that Vision's body was built from. He burns, he flies in flames, and fire cannot hurt him. (3.17)

## Becoming Jim Hammond

In survival there is **one way**: use the **Source Code** (the gem of Vision's brow) as a player who is **not Vision**, with no power or with any other hero. You take the gem into yourself, the item is used up, the whole server is told, and you are Jim Hammond. (Vision's own use of it, as White Vision, is unchanged.) There is no room, recipe or loot for him.

The Source Code comes from Vision's mind (the Human Torch hands it over at the bottom of it), from the Creative tab, or from `/give`; a Vision who has it can hand it to someone else. A server can keep it from replacing another hero with `jim.sourceCodeReplacesHero = false`. Operators can also use `/enhanced power set <player> jim_hammond [android|classic|human]`, and in Creative the **Hero Creator** lists him ("become an existing hero", with all his looks) and every one of his abilities as a module you can pick.

## The six things he can do

| Key | Ability |
|---|---|
| **R** (hold) | **Fire Beam**: a **continuous beam**. While R is held a steady beam of fire follows where he looks (white-yellow core, orange glow, red edge, as Vision's beam but in fire): it hurts and **sets alight** what it touches and **burns the ground** along it, until he lets go or his **solar charge** runs out. No charge-up; it **drains the solar charge** every second it burns (120 a second by default), and a short cooldown follows. It leaves fire, never magma. |
| **G** | **Flight**, as Vision's. While he flies **his whole body is in flames** (head, torso, arms, legs, in fire colours, following the tilt of flight), and **whoever strikes him in melee is set alight**, though the blow still hurts him. |
| **V** | **Fireball**: he **winds the throwing arm back and hurls it forward** (every player sees the throw, with a small flare of flame in his hand at the release) and a ball of fire flies where he looks: a glowing, slowly turning core with flames streaming behind it. It hurts and sets alight what it hits and leaves fire on the ground. |
| **Nova Flame**: works like **Vision's Charged Beam**, from his jewel. Hold B to **charge** (a glow builds at his jewel and the HUD counts the percent), release to fire **one huge beam of fire**: its **damage, reach and width grow with the charge**, and a quick tap fires a weak one. Where it strikes it **explodes** (scaled by the charge, never hurting him). **Everything along and around its path burns**: every creature within about 2 blocks of the beam's line (a tap) growing to about 4 (a full charge) is set alight, and fire is placed on the open faces of the blocks along that band. A charge of **80% or more** also turns the ground along the path into **magma**. It costs **solar charge** (more the longer it was charged; he needs a high minimum to begin) and then a **long cooldown** (90 seconds). The jewel is the **gold diamond on the android suit's chest**, the **gold jewel on the classic suit's forehead** (it follows his head), and for the human disguise, which has none, the centre of the chest. |
| **N** | **Human Disguise**: he looks human (his flames go out in it), and back. |

And always: **total immunity to fire** (fire, lava, burning, magma, fire attacks), a body **almost as durable as Vision's** (+8 health and +6 armour; Vision's are +10 and +8), and **healing five times as fast** as a normal player.

## Looks

Change Look (**H**): the **android suit** of the comics (white, with the red face, yellow eyes, gold neck and a long white cape), the **classic red suit** and the **human disguise**; and "your own skin". The faces are built from Vision's own: the same shape and shading, in each suit's own colours.

## What he kills drops cooked

Like a Fire Aspect kill: whatever dies to Jim's fire (the Fire Beam, fireballs, the Nova Flame with its burning band and explosion) or his fists drops **cooked** meat, and so does whatever dies burning from his flames. Nobody else's kills change. Source Code Vision's Solar Beam and Charged Beam do the same (his other looks do not).

## Solar charge

Jim charges on the sun, by the Solar Jewel's own rule (3.17). His **solar meter** (0 to 100%, on the HUD at the Fire Beam's line) fills **under the open sky by day** (fast), a little by day out of the sun (shade, rain, a roof with daylight), and **not at all at night, underground or in the Nether and End**. He **starts full** when he becomes Jim. His attacks spend it: the **Fire Beam** a little every tick it burns (a cost per second), a **fireball** a little, the **Nova Flame** a lot (and he needs a high minimum even to begin gathering it). With too little charge the attack does not fire and the action bar says why. Flight, fire immunity, his body, healing and the burning body cost nothing. With `jim.solarCharge = false` his attacks are free. (Heroes made in the Hero Creator with his modules are free of it.)

## Fire is only placed, never taken

The fire the Fire Beam, the fireballs and the Nova Flame leave is **placed** (as flint and steel does) and nothing is deleted. The magma of a charged Nova Flame (and its explosion) change blocks, so they follow the same rules as every block change of the mod: never with the game rule `mobGriefing` off, never in a boss arena, in Asgard's city, in someone's Hex, at the spawn protection or where a claim mod refuses, and never blocks harder than 8 (obsidian is 50).

## Numbers

Every number is in the server config, section `[jim]` (in game: `/enhanced config list jim`): the Fire Beam's damage, reach, width, burn seconds, fire, cost per second, minimum charge and cooldown; the fireballs' damage, speed, cooldown, range, fire and cost; the Nova's charge time, tap time, damage, reach, width, band radius (tap and full), burn seconds, fire, magma (and its minimum charge and radius), explosion, costs, minimum charge and cooldown; the solar charge; his health and armour, the healing multiplier and how long an attacker burns.

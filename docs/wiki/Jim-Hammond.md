# Jim Hammond

The first Human Torch: the android that Vision's body was built from. He burns, he flies in flames, and fire cannot hurt him. (3.17)

## Becoming Jim Hammond

In survival there is **one way**: use the **Source Code** (the gem of Vision's brow) as a player who is **not Vision**, with no power or with any other hero. You take the gem into yourself, the item is used up, the whole server is told, and you are Jim Hammond. (Vision's own use of it, as White Vision, is unchanged.) There is no room, recipe or loot for him.

The Source Code comes from Vision's mind (the Human Torch hands it over at the bottom of it), from the Creative tab, or from `/give`; a Vision who has it can hand it to someone else. A server can keep it from replacing another hero with `jim.sourceCodeReplacesHero = false`. Operators can also use `/enhanced power set <player> jim_hammond [android|classic|human]`, and in Creative the **Hero Creator** lists him ("become an existing hero", with all his looks) and every one of his abilities as a module you can pick.

## The six things he can do

| Key | Ability |
|---|---|
| **R** (tap, or hold) | **Fire Beam**: a tap fires a weak beam of fire; hold to **charge** it (a glow builds round his brow, a note rises and the HUD counts the charge) and release for a stronger and longer one. Where it hits the ground it **burns the ground**; a beam charged to 80% or more also turns the ground it hits into **magma blocks**. Cooldown 3 seconds. |
| **G** | **Flight**, as Vision's. While he flies **his whole body is in flames** (head, torso, arms, legs, in fire colours, following the tilt of flight), and **whoever strikes him in melee is set alight**, though the blow still hurts him. |
| **V** | **Fireball**: he throws a ball of fire. It hurts and sets alight what it hits and leaves fire on the ground. |
| **B** (hold) | **Nova Flame**: he must **gather his energy** first: hold B for 3 seconds, and when it is full a great blast of fire goes off round him (12 blocks): it hurts, burns and throws back everything near (never him) and sets the ground alight. Let go before it is full and the energy is lost. Then a **long cooldown** (90 seconds). |
| **N** | **Human Disguise**: he looks human (his flames go out in it), and back. |

And always: **total immunity to fire** (fire, lava, burning, magma, fire attacks), a body **almost as durable as Vision's** (+8 health and +6 armour; Vision's are +10 and +8), and **healing five times as fast** as a normal player.

## Looks

Change Look (**H**): the **android suit** of the comics (white, with the red face, yellow eyes, gold neck and a long white cape), the **classic red suit** and the **human disguise**; and "your own skin". The faces are built from Vision's own: the same shape and shading, in each suit's own colours.

## Fire is only placed, never taken

The fire the Fire Beam, the fireballs and the Nova Flame leave is **placed** (as flint and steel does) and nothing is deleted. The magma of a charged beam replaces blocks, so it follows the same rules as every block change of the mod: never with the game rule `mobGriefing` off, never in a boss arena, in Asgard's city, in someone's Hex, at the spawn protection or where a claim mod refuses, and never blocks harder than 8 (obsidian is 50).

## Numbers

Every number is in the server config, section `[jim]` (in game: `/enhanced config list jim`): the beam's charge time, damage, reach, burn seconds, fire and magma (and the radius of the magma), the fireballs' damage, speed, cooldown and range, the Nova's charge time, radius, damage, push and cooldown, his health and armour, the healing multiplier and how long an attacker burns.

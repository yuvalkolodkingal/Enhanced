# Heimdall

The Guardian of the Bifrost (3.5), as in the *Thor* films: he stands in his Observatory at the end of the Rainbow Bridge, sees everything in the Nine Realms, and opens the Bifrost with his great sword **Hofund**. As a hero he fights with Hofund's golden blows, sees every creature through walls and every player far away, opens the Bifrost with no item at all, plants his sword in a golden dome, calls the Einherjar, strikes with a column of rainbow light and sends his friends wherever they need to go.

Hebrew: [עברית](Heimdall-HE)

## The quick version

* **How:** go to [Asgard](Asgard), talk to Heimdall in the Observatory and **Take the Watch**: find and strike seven Shadows of Hela in the city in three minutes. Win, and he gives you **Hofund**. With no power, **hold your own Hofund** and you become Heimdall.
* **R** swings Hofund (three blows), **G** opens his all-seeing Sight, **V** opens the Bifrost, **B** (hold) raises the Guardian's Ward, **N** brings down the Bifrost Strike. **Sneak** changes each of them.
* He doesn't fly: the Bifrost carries him.

## Becoming Heimdall: the Guardian's Watch

1. Reach [Asgard](Asgard) by the Bifrost (a [rune stone](Asgard#the-bifrost-rune-stones-for-players-with-no-power) does it with no power).
2. In the **Observatory**, right-click **Heimdall** to talk to him. "Seven shadows of Hela hide in the city. See them as I see them, and strike them before the sand runs out." Answer **Take the Watch**. Anyone may take it, a hero or not.
3. **The Watch** lasts **3 minutes**. Seven **Shadows of Hela**, small wisps of black-green smoke, hide in the city: in houses, halls, the library, the stables. For the Watch you have **Heimdall's Sight**: the shadows glow gold through walls at any distance, a needle at the top of the screen points to the nearest one, and a gold bar counts the time and the shadows ("The Guardian's Watch — 4 / 7 — 1:32"). The Watch's own music plays.
4. **Strike** each shadow once. They drift, and flee a few blocks when you come within 3. Only **your** blow counts: nobody else can hurt your shadows.
5. **Win** (all seven in time): "You saw what I see. Hofund is yours now: hold it, and the watch is yours." **Hofund**, bound to you, is in your hand, and the Bifrost brings you back to the Observatory.
6. **Become Heimdall:** with **no power**, hold **your own Hofund**. The Bifrost's light falls on you: you are Heimdall. (A hero who already has a power keeps Hofund as a sword and a key to the Bifrost.)

**Lose** (the time runs out, you die, you leave Asgard or log out, or you tell Heimdall "I give up the Watch") and the shadows slip away: "Try again when you are ready." You may take the Watch again after **60 seconds**. You keep the Watch only once: after you have won, Heimdall gives you a new Hofund if you lose yours ("I lost Hofund").

## Two guardians: Heimdall's post (3.5.1)

There is one of each character in Asgard (see [Asgardians](Asgardians#one-of-each-character)), but **Heimdall is the exception**: when you are Heimdall, the Heimdall of the Observatory **stays**, and shares his watch with you.

* **When:** the first time you come near him as Heimdall (within 6 blocks), and again whenever you talk to him after that, he **hands you his post**: "We are both guardians of Asgard. Keep the Bifrost for me a while: my socket answers to you now. I will rest my eyes over there."
* **For how long:** **10 minutes** (`heimdall.postSeconds`). A gold bar at the top of the screen counts it down: "Keeper of the Bifrost — 9:41".
* **He steps aside** to a spot a few blocks from his post and watches it while you keep it.
* **While you keep it:**
  * **Hofund's socket answers to you:** right-click it and the Bifrost mechanism spins up; if another player is in the Observatory, Heimdall's Bifrost screen opens to **send the nearest one** wherever you choose (home, Asgard, your places).
  * **You send others with no cooldown** from the Observatory (your Bifrost for a friend, Sneak + N, as the Heimdall of the city does it).
  * Others who talk to him hear who keeps the Bifrost ("Eilon keeps the Bifrost for me now"), and he can still send them home.
* **It ends** when the time is up, when you give it back (talk to him: **Take your post back**), or when you stop being Heimdall, leave Asgard, die or log out. He walks back to his post: "Thank you, guardian. I take my post again. We are both guardians of Asgard."
* One keeper at a time: a second Heimdall waits until the post is free.

## Hofund

Heimdall's sword: a long golden broadsword.

* **For anyone:** a sword that hits for **10** (a little more than netherite), sweeps, never breaks and can't burn. **Anyone holding a Hofund, anyone's, can open the Bifrost** with it: right-click opens the Bifrost screen, Sneak + right-click takes you to where you look (as with Mjolnir and Stormbreaker; a server can switch this off with `asgard.hofundOpensBifrost`).
* **Bound to its owner:** the tooltip says "Bound to" and his name. Only your own Hofund makes you Heimdall.
* **Heimdall hits 30% harder with it** (`heimdall.hofundBonus`), with his abilities and with plain blows.
* **Lost it?** As Heimdall, press **R** with no Hofund in your hand and none in your inventory: **Hofund returns** to your hand in a golden flash (every 10 seconds at most). The old one crumbles to golden dust the next time it is seen, in a chest or on the ground, so there is only ever one.
* It is in Asgard's never-lost list: if you die in Asgard you keep it.

## The body

* **+8 health** (14 hearts), **+4 armour**, steady on his feet (much less knockback), and **no damage from falls shorter than 8 blocks**.
* He doesn't fly; the Bifrost is how he travels.

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Hofund Slash | A wide golden arc in front of him (3.5 blocks, 100°). Three blows in a row: **9**, **11** and **14** damage; the third knocks back. Press again within a second and a half to go on with the combo. Needs Hofund in his hand ("Hofund is not in your hand"; with none anywhere, R calls it back). **Sneak + R: Lunge**: a dash of 6 blocks and a thrust, **12** damage to the first creature in the way; it stops at walls. Cooldown 4 seconds. |
| 2 | **G** (toggle) | All-Seeing Sight | "You see all that is in the Nine Realms." Every living thing within **96 blocks** glows through walls: **players gold, monsters red, animals green, Asgardians blue**. Every player in the same world, however far, is a gold marker with his name and distance; players in other worlds are listed on the HUD ("Peter — Asgard"). Night Vision while it is on, and his eyes glow amber. **Sneak + G (hold): Far Sight**: the view zooms in ×6; the **mouse wheel** zooms between ×6 and ×10. |
| 3 | **V** | Bifrost | Opens the **Bifrost screen** with nothing in his hand: Asgard, Home and his saved places (see [Asgard](Asgard#the-bifrost-screen)). **Sneak + V**: the Bifrost to where he looks. It takes everyone in its circle along. |
| 4 | **B** (hold) | Guardian's Ward | He plants Hofund in the ground and a **golden dome** (5 blocks) rises round him: arrows and other shots stop at it, monsters are pushed out, he takes **40% less damage** and can't be knocked back, but he can't move. Up to **10 seconds**; cooldown 8 seconds. Hofund doesn't need to be in his hand. **Sneak + B: Gatekeeper's Call**: a horn call, and **three Einherjar** (four in Asgard) appear in a golden flash, follow him and fight what he fights, or what attacks him, for **40 seconds**. Cooldown 45 seconds. |
| 5 | **N** | Bifrost Strike | A **column of rainbow light** strikes where he looks (up to 48 blocks): **14** damage within 3 blocks, and everything is thrown up. Nothing catches fire. Cooldown 7 seconds. **Sneak + N: Bifrost for a friend**: look at a player (up to 24 blocks) and his Bifrost screen opens in **Send** mode ("Send Peter by the Bifrost"): choose Home, Asgard or one of your saved places, and the Bifrost gathers round your friend and takes him there. Your friend is told, and **can stay**: he sneaks, or steps out of the light, before it takes him. |

### Heimdall's Bifrost

Heimdall opens the Bifrost as Thor does, but with **no item**: **V** is his key. It has the same screen, the same 60-second cooldown and the same [circle that takes everyone in it](Asgard#the-bifrost-takes-everyone-in-its-circle). His Send is his own: it uses the Bifrost Strike's cooldown, not the Bifrost's, and only Heimdall can send someone else.

## Looks

Press **H** to switch. See [Looks](Looks).

| Look | Id | What it is |
|---|---|---|
| **Guardian of the Bifrost** (default) | `guardian` | His golden plated armour, and the great gold **helmet with two horns** that sweep forward and up. |
| **Ragnarok** | `ragnarok` | The hooded brown-green cloak over leather of *Thor: Ragnarok*, when he hid the people of Asgard; no helmet. |
| **Your own skin** | `own` | His powers in your own skin. |

His eyes glow amber while the Sight is open. He moves like himself: heavy and upright when he runs, the sword turned in both hands when the Bifrost opens, planted before him in the Ward. Everyone sees the arcs, the rainbow strike and the dome.

## The HUD

Next to the usual panel: the Sight (on, and how many players are far away), the blow of the combo (1, 2 or 3), the Ward's seconds, and a warning when Hofund is not in your hand ("R: call Hofund back"). With the Sight on, the players of other worlds are listed at the side.

## Heimdall in the Hero Creator

His six rows are in the [Hero Creator](Hero-Creator) (ids `heimdall.hofund`, `heimdall.sight`, `heimdall.bifrost`, `heimdall.ward`, `heimdall.strike`, `heimdall.body`; see [Heroes' Abilities](Hero-Abilities#heimdall-6)). A custom hero's Hofund Slash works with **any sword** (with Hofund, 30% more); his Bifrost (a world power) opens the Bifrost with no item, Asgard included. The ready-made *Heimdall-like* hero starts with Hofund, the Sight and the Bifrost.

## The Infinity Stones (3.6)

Heimdall is a god: he may hold every [Infinity Stone](Infinity-Stones) bare and use it, wield a crafted Infinity Gauntlet and Snap, and seek the Soul Stone at [Vormir](Vormir). Only the **Time Stone** stays quiet in his hand: it answers only a sorcerer.

## Numbers you can change

The Sight's range and whether it lists other worlds, the three blows and the Lunge, the Ward's time and cooldown, the Call's time and cooldown, the Strike's damage and cooldown, how far a friend may be sent from, the Watch's time and how long to wait after losing it, and Hofund's bonus are the `heimdall.*` options on [Server Configuration](Server-Configuration#heimdall). Whether Hofund opens the Bifrost for anyone is `asgard.hofundOpensBifrost`.

## Related

[Asgard](Asgard) · [Asgardians](Asgardians) · [Odin](Odin) · [Thor](Thor) · [Controls](Controls) · [Looks](Looks)

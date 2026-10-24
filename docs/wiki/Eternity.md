# Eternity

From *Thor: Love and Thunder* (3.8, Eilon's idea): **Eternity**, the cosmic being at the heart of the universe, grants **one wish** to whoever reaches him. Far out in **the End** stands the **Shrine of the Cosmic Entities**, with the statues of the great powers of the universe round **Eternity's altar**. Lay a **key of the Bifrost** on the altar (Stormbreaker, Mjolnir, Hofund or your own Gungnir) and you are taken to **Eternity's realm**: a white world of mist above a mirror of still water, where Eternity sits, giant and meditating, his body full of stars. There you may ask for **anything in the world**, or to **become [Love](Love)**, Thor's daughter.

Hebrew: [עברית](Eternity-HE)

![Love at the Shrine of Eternity](images/101-love-eternity-shrine.png)

## The quick version

1. **Get a key of the Bifrost:** [Stormbreaker](Thor#stormbreaker) (anyone can craft and hold it), your own claimed [Mjolnir](Thor#mjolnir), [Hofund](Heimdall) or your own [Gungnir](Odin).
2. **Find the shrine** on the outer islands of the End: `/locate structure enhanced:eternity_shrine`.
3. **Right-click the altar** in the middle with the key in your hand and **stay by it for 3 seconds**.
4. In Eternity's realm, his screen opens: **Ask for anything** (pick any item) or **Become Love**. **One wish per player.**
5. Then you are sent home, to the very spot you left.

## The Shrine of the Cosmic Entities

A round plaza of end-stone bricks with rings of purpur and black stone, on the **outer islands of the End** (the highlands and midlands, far from the dragon's island), lit by purple lights and end rods. Three steps lead up to a dais in the middle, with **Eternity's altar**: a pedestal of obsidian and purpur with a starry top, a soft violet ring breathing on it. You hear the shrine's low hum near the altar.

Behind the altar sits **Eternity's statue**, 16 blocks high, cross-legged: black stone and obsidian filled with purple, blue and magenta glass and hidden lights like a starfield, a dark helmet with a pointed crown and four bent antenna-horns.

Round the plaza, on plinths facing the middle, stand **eight more cosmic entities**, each with a glowing sign:

| Statue | What it looks like |
|---|---|
| **Infinity** | A tall robed shape of black and light-blue glass, dotted with end rods for stars. |
| **Death** | A kind, hooded figure in dark green and black, holding a flower (our Death is Rio, the one you [meet when you die](Meeting-Death): no skull). |
| **The Living Tribunal** | A giant in robes whose golden head has **three faces**. |
| **Galactus** | A huge purple-and-blue helmet with two tall fins over a stern face. |
| **Arishem the Celestial** | A towering red-armoured figure with a round, faceless dome for a head. |
| **Oblivion** | A dark, swirling hooded shape of black and grey glass, with two soul lanterns for eyes. |
| **Master Order** | A white quartz figure of squares and straight lines... |
| **Lord Chaos** | ...facing him across the ring: jagged crying obsidian. |

By the stair, a lectern holds the book **"The Cosmic Entities"**: a page about each of them, and the last: "To speak with Eternity, bring a key of the Bifrost to his altar." The shrine has no chests: Eternity's gift is the treasure.

**Finding it:** `/locate structure enhanced:eternity_shrine` (in the End). Shrines appear only in **new chunks** of the End's outer islands, so in an old world, fly out to islands you have not seen yet. A server can stop new ones with `eternity.enabled`.

## The way in: the altar

![Love at the Shrine, second view](images/102-love-eternity-shrine-2.png)

* **Right-click the altar** with a **key of the Bifrost** in either hand: **Stormbreaker**, **Mjolnir**, **Hofund** or **Gungnir** (Gungnir only if it is your own). The key **stays in your hand**; a glowing copy of it **floats up over the altar and spins**, faster and faster, in a violet column of light, while stars rise from it.
* **Stay by the altar for 3 seconds** with the key in your hand. **Sneaking**, walking more than 3 blocks away, letting go of the key, leaving or dying stops it ("The key stops floating. The way to Eternity closes." / "Stay by the altar while it calls Eternity.").
* Then **you alone** are taken (not your pets, not what you ride): a white flash, and you arrive in Eternity's realm.
* Without a key: "The altar waits for a key of the Bifrost — Stormbreaker, Mjolnir, Hofund or Gungnir."
* One traveller at a time: "The altar is taking someone else. Wait your turn."
* Not during a boss fight, and not inside a boss's arena.

The altar of a shrine can't be broken in Survival.

## Eternity's realm

A world of its own: **white and silent**, a pale sky with soft grey cloud bands, **no ground but a still mirror of shallow water** that stretches away forever, and **white mist** on the horizon. You can walk on the water: your steps make soft rings of light. Everything is **mirrored** in it: the clouds, Eternity, and you.

**Eternity** sits far in front of you, **about 44 blocks tall**, cross-legged and meditating, hands on his knees. His body is a **window onto deep space**: purple, blue and magenta nebulae with twinkling stars that drift slowly; his head is a dark faceted helmet with a pointed crown and **four tall, bent antenna-horns**. His shoulders rise as he breathes, and his head turns slowly toward you.

* **Safe:** nothing can hurt you there (only `/kill`), there are no monsters, and you may fly.
* A warm, slowly breathing music plays, with slow chimes.
* **Two seconds after you arrive, Eternity speaks**, and his screen opens: "Few find their way to me. Fewer know what to ask. Ask, and it will be."
* If you close the screen you stay standing on the water. Right-click the **wishing stone** (an altar just in front of where you arrived) to open it again.
* The client options `eternityReflections` (the mirror's reflections; off for slow computers) and `eternityFog` (the white mist) are yours to change. See [Server Configuration](Server-Configuration#client-options-animations-effects-and-looks).

## The wish

![Love at the Shrine, third view](images/103-love-eternity-shrine-3.png)

Eternity's screen has two big choices, and **Leave**:

### Ask for anything

A picker of **every item in the game**, in the order of the creative tabs, with a **search box** (type the item's name, in your language, or its id) and tooltips. Pick one and press **Wish for it**: you get **a full stack** of it (64 diamonds, 16 ender pearls, one sword), in your hand if it is empty, else in your bag, and what doesn't fit at your feet.

Its rules, which the server checks on every wish:

| Item | The rule |
|---|---|
| **Operators' and impossible blocks** (command blocks, structure blocks, barriers, light blocks, the debug stick, spawners, bedrock, end portal frames, reinforced deepslate...) | Never: they are not even in the picker ("Eternity does not grant…"). |
| **An Infinity Stone** | Only if you may hold it bare (a god, the Scarlet Witch or the Phoenix: [who may hold a stone](Infinity-Stones#who-may-hold-a-stone)). The **Reality, Time, Power and Soul Stones** also follow the world's **stone ledger**, exactly as if you took them from their site: if the server's rules say someone else already has that stone, it is locked with the ledger's own reason. The Mind and Space Stones are free, as their Scepter and Tesseract are. |
| **Thanos's Infinity Gauntlet** | Given **empty**: each stone is a wish of its own. |
| **Gungnir** | Only if the world's **one Gungnir** has no other bearer ("Gungnir already has its one bearer."); you become its bearer. |
| **Mjolnir** | Only for the **worthy** ("Only the worthy may hold Mjolnir."); it is claimed by you. |
| **Anything else** | Yours: one full stack (a server can make it one: `eternity.wishFullStack`). |

Locked items are greyed in the picker with a ✖ and the reason.

### Become Love

"Thor's daughter: lightning, the axe, purple eyes, 32 health. Replaces your hero." Choose it and you become **[Love](Love)**: your old hero is replaced (the `/enhanced power set` way), and you get a **Stormbreaker** if you carry none. Everyone is told: "Eilon made a wish to Eternity and became Love, Thor's daughter!" A server can take this choice away (`eternity.loveWish`).

### After the wish

A golden-white burst of stars round you, Eternity says **"It is done."**, and **3 seconds later you are home**.

* **One wish per player**, for good (it is kept when you die). A player who has had his wish may still visit: "You have had your wish. Go well, traveller.", and leave. An operator can give a wish back: `/enhanced eternity reset <player>`. A server can allow more (`eternity.wishesPerPlayer`; 0 = no limit).
* **Leave** takes you home at once, with your wish unspent.

## Going home

You go back **to exactly where you stood and the way you faced**, in the world you came from. If that place is no longer safe (it is inside blocks now, or over the void), you land on the nearest safe spot within 8 blocks, else at your respawn point.

* Logging out in the realm: you are home when you log in again.
* Leaving the realm another way (a command, the Bifrost) ends your visit; dying there (only `/kill` can) is a normal respawn.

## Advancements

In the **Avengers** tab: **The Gates of Eternity** (reach Eternity's realm), **Be careful what you wish for** (make a wish) and **Thunder runs in the family** (become Love).

## For operators

* `/enhanced eternity reset <player>`: the player may wish again.
* `/enhanced eternity send <player>`: takes a player to Eternity's realm as the altar would (his way home is where he stands), or home from it.

See [Commands](Commands#the-avengers-38).

## Numbers you can change

Section `[eternity]`: `enabled` (the shrines in new chunks), `altarChargeTicks` (60), `wishesPerPlayer` (1), `wishFullStack` (true), `loveWish` (true), `returnDelayTicks` (60), `screenDelayTicks` (40). The structure's spacing is data (`eternity_shrines`: 40 / 14 chunks). See [Server Configuration](Server-Configuration#eternity-and-love).

## Related

[Love](Love) · [Thor](Thor) · [Infinity Stones](Infinity-Stones) · [Odin](Odin) · [Heimdall](Heimdall) · [Structures](Structures#the-shrine-of-the-cosmic-entities-38) · [Meeting Death](Meeting-Death)

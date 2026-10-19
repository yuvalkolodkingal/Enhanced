# Vormir and the Soul Stone

The Soul Stone (3.6) is not found in a chest or on a pedestal. As on Vormir in *Avengers: Infinity War*, it asks **a soul for a soul**: on the summit of the **Vormir Cliff**, in the Nether, someone who seeks the stone must give **one he loves** to the fall. The **Stonekeeper** waits there and tells you the price.

Hebrew: [עברית](Vormir-HE)

## The quick version

* **Where:** a **Vormir Cliff**, a tall pinnacle of blackstone and basalt in a **soul sand valley** or the **basalt deltas** of the Nether. `/locate structure enhanced:vormir_cliff` finds one.
* **Who:** the seeker must be a **Bearer**: Thor, Loki, Heimdall, Odin, Hela, the Scarlet Witch or the Phoenix (or a custom hero with a god's ability).
* **How:** **push** a friend (with **PvP on**), or **your own pet**, off the **cliff edge** on the summit, into the chasm. If the fall kills them, you wake in the orange pool **holding the Soul Stone**.
* **The price:** a friend loses **all his items and his power**, and comes back as a player with no power. A pet leaves nothing.

## The Vormir Cliff

A pinnacle of blackstone and basalt rises from the floor of a great Nether cavern, more than 70 blocks high. A **path** cut into its side, three blocks wide and walled on the outside, climbs round it three times up to the summit.

* **The summit:** a floor of polished blackstone with a low wall round its rim; a ring of chiselled blackstone and soul lanterns; the **pool** of orange light; and the **chasm**, nine blocks across, walled on three sides.
* **The cliff edge:** the fourth side of the chasm, on the south, between two basalt pillars. A low lip of stone (knee high) runs along it: you have to jump over it, or be pushed. The strip of floor behind the lip is **the edge**.
* **The pit:** the chasm falls about 60 blocks to a floor of soul soil, bones and soul fire, with a chest. A tunnel leads out of the pit, down to the cavern floor beside the foot of the path.
* A summit wind howls there; you hear it louder the nearer you are to the Stonekeeper.

## The Stonekeeper

A hooded figure in a ragged dark robe, floating a little over the summit floor, his eyes glowing under the hood. He can't be hurt, pushed or moved; he is always put back in his place. Right-click him and he speaks, three lines:

1. "Welcome to the summit. I am the keeper of the stone."
2. "It asks a soul for a soul: one you love, given to the fall."
3. "Think well. What the fall takes, it does not give back."

His third line tells you if something is wrong: "Your hands could not hold what you would win. Go home." (you are not a Bearer), "You already carry a soul.", "The stone has been taken.", "The stone sleeps. No soul is asked of you today." (the sacrifice is switched off), "One who cannot fall cannot give to the fall." (Creative), "Only a creature you love may be given here." (players can't be given on this server), "Here no one may harm another. The stone stays where it is." (PvP is off).

## The sacrifice, step by step

1. **Go up** to the summit with the one you will give: a friend, or a tamed pet of yours (a wolf, a cat, a parrot, a horse...).
2. **They stand on the edge** (the strip behind the low lip).
3. **You push them off.** Anything of yours counts: a **hit**, an **arrow** or other shot, a **bite of your pet**, a **power** that shoves (Thanos's slam, the Hulk's Smash, a telekinetic throw or drop...), a **wind charge**. The push must come at most **5 seconds** before they leave the edge, and **the last push counts**: a creature's hit after yours takes the credit away.
4. **The fall must kill them** in the chasm: at least **30 blocks** down, within **10 seconds** of leaving the edge.
5. **The gift:** you are taken to the orange pool, the Soul Stone is in your hand, and the Stonekeeper says "**A soul for a soul.**" An orange light fills your screen: "The Soul Stone — Given for the one you lost". The advancement **A Soul for a Soul** is yours.

If the one you gave was a pet, its soul rises from it to you as an orange wisp.

## What the friend loses

A sacrificed **player**:

* **loses every item**: his inventory, his armour, his off hand, what he held on the cursor, even with `keepInventory` on. **His ender chest is never touched.** Stones he carried are lost too (and can be taken again: see [the ledger](Infinity-Stones#how-many-stones-the-ledger)).
* **loses his power** (he has no hero any more) and all his experience.
* **respawns as normal**, and reads: "You were given for the Soul Stone. You lost your things and your powers, but you are alive." and "Your ender chest was not touched."

A sacrificed **pet** dies and drops nothing.

## What does not count

The stone is never given by accident. **Nothing happens** (it is just a fall) for:

* a **solo fall**: nobody pushed you, or you jumped;
* a push by a **creature** (a zombie, a ghast's blast), or someone else's pet;
* a fall from **another cliff**, or from anywhere but the edge strip (the other three sides of the chasm are walled);
* a friend when **PvP is off** (with PvP off a player can't push another at all);
* a **totem of undying** that saves them, or a fall they **survive**;
* a seeker in **Creative**, or one who is **not a Bearer** (the Hulk's push, too, is only a fall);
* **someone else's pet**, or a creature that is nobody's;
* a seeker who **already has his stone** (once per player), or the world's stone **already taken** (once per world);
* a push **more than 5 seconds** before the fall, or a death more than 10 seconds after leaving the edge;
* a friend who **logs out** in the air.

## Rules for servers

Section `[soul_stone]` on [Server Configuration](Server-Configuration#the-soul-stone-and-vormir):

| Option | Default | What it does |
|---|---|---|
| `enabled` | `true` | The cliff gives the Soul Stone for a sacrifice. |
| `playerSacrifice` | `true` | A player can be the sacrifice (only where PvP is on). |
| `petSacrifice` | `true` | The seeker's own pet can be the sacrifice. |
| `destroyItems` | `true` | A sacrificed player loses all his items (never his ender chest). |
| `removePower` | `true` | A sacrificed player loses his power. |
| `friendRule` | `ANY` | Who is a friend: `ANY` other player, or only a `TEAM` mate (the same scoreboard team). |
| `onePer` | `PLAYER` | How many Soul Stones: `PLAYER` (each player once), `WORLD` (one in the world) or `UNLIMITED`. |
| `pushWindowTicks` | `100` | How long before leaving the edge a push counts (100 ticks = 5 seconds). |
| `minFall` | `30` | The shortest fall that counts, in blocks. |
| `deathWindowTicks` | `200` | How soon after leaving the edge the death must come (200 ticks = 10 seconds). |

New Vormir Cliffs can be switched off with `stone_sites.vormirCliffs`.

## Related

[Infinity Stones](Infinity-Stones) · [Structures](Structures#the-vormir-cliff) · [FAQ](FAQ#the-infinity-stones-36)

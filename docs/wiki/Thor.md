# Thor

The God of Thunder. *"Whosoever holds this hammer, if he be worthy, shall possess the power of Thor."* Thor throws Mjolnir and calls it back, flies by spinning it, calls lightning down through it, and opens the Bifrost to take himself, or anyone who holds his weapons, anywhere in any dimension.

## Mjolnir

**Forging Mjolnir.** "Forged in the heart of a dying star, from the sacred metal of Uru":

```
N S N
N H N
. R .

N = Netherite Ingot (Uru)
S = Nether Star (the heart of a dying star)
H = Heavy Core
R = Breeze Rod (the handle)
```

Mjolnir hits hard (10 attack damage, slow like an axe), never breaks, and fire and lava can't destroy it. Speedsters can't snatch it and telekinesis can't lift it.

### The worthy

Only the worthy can lift a Mjolnir nobody owns yet:

* **Thor** himself,
* **Vision**, who lifted it in *Age of Ultron*,
* anyone who has proven themselves a hero by **saving a village from a raid** (the *Hero of the Village* advancement),
* players in **Creative or Spectator** mode,
* or **everyone**, if the server option `thor.anyoneWorthy` is turned on.

### Mjolnir is bound to its owner

* **A Mjolnir nobody has claimed yet (a freshly forged one, or one from the Creative tab) belongs to nobody.** The **first worthy player to lift it claims it, for good**: when it comes into their inventory or hand, when they lift it off the ground, when they forge it, or when Thor calls it to himself. "Mjolnir is yours now: only you can lift it." From then on, **only its owner can lift it**: not another Hero of the Village, not Vision, not another Thor.
* The tooltip says **"Bound to"** followed by the owner's name, or **"Unclaimed: the first worthy hand to lift it claims it"** until someone claims it.
* **Anyone else** finds it far too heavy: in their inventory it slips out of their hands and falls at their feet ("Mjolnir slips from your grasp: only Steve can lift it", with the owner's name). They can't pick it up, and where it rests as a block it won't budge for them ("It won't budge. Only Steve can lift it.").
* **Thor's recall never takes someone else's Mjolnir**, wherever it is. He calls his own, a Mjolnir nobody owns yet (he claims it when it reaches him), or Stormbreaker.
* **Creative and Spectator players can still lift any Mjolnir**, so an operator can move one, but holding someone else's never changes its owner. A Creative player who lifts a Mjolnir that nobody owns claims it like any other worthy player; a Spectator never does.
* **Stormbreaker is not affected.** Anyone can wield it.
* A server can turn ownership off with `thor.mjolnirOwnerOnly` (on by default). With it off, Mjolnirs are never claimed: any worthy player can lift any Mjolnir. See [Server Configuration](Server-Configuration).

| Who | A Mjolnir nobody owns | A Mjolnir bound to someone else |
|---|---|---|
| A worthy player | Lifts it and becomes its owner | Can't lift it |
| A player who is not worthy | Can't lift it (it slips away unclaimed) | Can't lift it |
| A Creative player | Lifts it and becomes its owner, like any worthy player | Lifts it, and it stays its owner's |

### Where Mjolnir rests

A dropped Mjolnir lands and stands on its head, handle up, like in the crater in New Mexico: a block that **nothing can break, blow up or push**, and that remembers whose it is. It never despawns before it lands. Only its owner can lift it again (right-click). Thor can set it down himself with **Sneak + right-click** on a block.

### Becoming Thor

The first time a worthy player with no other power lifts (and so claims) a Mjolnir nobody owned, or lifts their own, lightning strikes and they **become Thor**. Vision can claim it and fight with it, but stays Vision.

## Stormbreaker

Forged in Nidavellir in the heart of a dying star, its handle grown from Groot's arm:

```
N N
N S
. L

N = Netherite Ingot (Uru)
S = Nether Star (the dying star)
L = any log (Groot's arm)
```

Stormbreaker carries no enchantment of worthiness: **anyone can wield it**, and nobody owns it. It is an axe (it chops wood, strips logs and knocks shields aside) that hits harder than any other (12 attack damage), never breaks and can't burn. It doesn't make anyone Thor, but whoever holds it can call **the Bifrost** (below). In Thor's hands it does everything Mjolnir does: he throws it, calls it back, flies with it and calls lightning down through it. Unlike Mjolnir, a speedster can snatch it out of a hand.

## The Asgardian body

* **Max health 30** (15 hearts), **6 armor**, half knockback.
* Lightning never hurts or burns Thor. With Mjolnir or Stormbreaker in hand he takes no fall damage.

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Throw / Call Back | With Mjolnir or Stormbreaker in hand: throw it. It flies straight, tumbling, smashing everything in its path (Mjolnir 10 damage, Stormbreaker 12, and heavy knockback), until it hits a wall or has flown 40 blocks, then turns back and flies home to his hand, straight through walls, hitting everything on the way back too. With neither in hand: **call the nearest one back**, from his inventory, the air, the ground anywhere within 64 blocks (dropped, or Mjolnir resting as a block), or the hands of a creature that picked it up (never from another player, and never another player's Mjolnir: he calls his own, one nobody owns yet, or Stormbreaker). |
| 2 | **G** (toggle) | Flight | Thor flies by spinning his weapon: only with Mjolnir or Stormbreaker in hand (or while it flies back to him). Fast: 0.11. Hovering, he holds it up to the sky; at speed it leads the way. You can also double-tap **Space**. |
| 3 | **V** | Lightning | He raises his weapon and lightning pours down into it from the sky and leaps to what he looks at, up to 64 blocks away: 8 damage there and less to everything within 3 blocks, with the usual effects of lightning on creatures (creepers become charged, pigs turn into zombified piglins), but no fire on blocks. Without a weapon he can still call it, straight from the sky. Cooldown 1.5 seconds. |
| 4 | **B** (toggle) | God of Thunder | From *Thor: Ragnarok*: his eyes blaze white-blue and lightning crawls over his body. +4 attack damage, +6 armor toughness, every blow he lands arcs lightning to the next two creatures nearby (4 damage each), a thrown weapon arcs too, and each Lightning strikes three times. It burns hunger; with an empty hunger bar the thunder calms. |
| 5 | **N** | Bifrost | "Heimdall! Open the Bifrost!" With Mjolnir or Stormbreaker in hand: **N** opens the Bifrost screen (home, his saved places, save this one), **Sneak + N** takes him to the spot he looks at, up to 512 blocks away. |

All of Thor's lightning is the MCU's: a white-hot core, electric blue and a deep blue halo.

## The Bifrost and its saved places

* **The Bifrost answers the weapon, not the power.** Thor calls it with **N**. Anyone else holding **Mjolnir or Stormbreaker** calls it by **using the weapon** (right-click). With neither in hand it doesn't come.
* **The Bifrost screen** shows:
  * **Home:** the respawn point (bed or respawn anchor, otherwise the world spawn), in whatever dimension it is;
  * every **saved place**, with how far away it is, or which world it is in (the Overworld, the Nether, the End, or a dimension added by another mod). The **X** button forgets one;
  * a field to **save the place you stand on**, under a name of up to 32 characters, facing the way you face. Saving a name again moves that place. You can save up to **24 places**.
* **Sneak + N** (or Sneak + right-click with the weapon) takes you to the spot you look at, up to 512 blocks away.
* **Any dimension.** A column of swirling rainbow light gathers around the traveller for a second and a half, holding them in place, then takes them, **across dimensions if need be**. They land exactly on the saved spot, facing the saved way. If it has been built over since, they land on the first free spot above it. The Bifrost burns its circle of runes into the ground where it lands and where it left, glowing gold and fading over a minute.
* Arriving inside a Hex counts as crossing its wall: the Hex judges the traveller as it judges everyone else.
* **Cooldown: 60 seconds**, the same for everyone, shown on the screen (and on Thor's HUD).
* The places and the cooldown are saved with the player and kept on death, and only that player sees them.

## Looks

*Thor* (2011 and *The Avengers*: black and silver armour with the six chest discs, red cape), *Ragnarok* (the God of Thunder at the end of the film: short hair, eyepatch), *Love and Thunder* (navy, gold and black armour, bare arms, red cape), *Mighty Thor* (Jane Foster), and *Your own skin*. Press **H** to switch. See [Looks](Looks).

## Numbers you can change

Thor's flight speed, who is worthy of Mjolnir, whether Mjolnir is bound to its owner, the throw damage (Mjolnir and Stormbreaker), the throw range and recall range, the lightning's damage and range, the hunger cost of the God of Thunder and the Bifrost's cooldown are the `thor.*` options on [Server Configuration](Server-Configuration).

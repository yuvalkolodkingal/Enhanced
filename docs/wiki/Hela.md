# Hela

The Goddess of Death (3.5), Odin's first-born, as in *Thor: Ragnarok*: black necroswords, walls of blades, the antlered headdress, the army of the dead and the great wolf **Fenris**. She is a **boss**, fought in the throne room of [Asgard](Asgard); beat her and you can **become Hela** yourself.

Hebrew: [עברית](Hela-HE)

## The quick version

* **Calling her:** in Asgard's throne room, right-click **Hela's Seal** in the wall behind the Throne of Asgard, then right-click it again within 5 seconds to break it.
* **The fight:** three phases. *The Goddess* (blades), *The Army of the Dead* (berserkers and Fenris; the warriors of Asgard join you), *The Strength of Asgard* (she draws on Asgard to heal: break it with damage). Bring friends.
* **The reward:** everyone who fought well gets **Hela's Headdress** (hold it for 3 seconds with no power: you become Hela) and the right to take **Gungnir** and become [Odin](Odin). Her first defeat wakes Odin and opens his vault.

## Calling Hela

* **Hela's Seal** is a block in the north wall of the throne room, behind the throne. Right-click it: "Hela's Seal. Break it, and the Goddess of Death returns. (Click again to break it.)" A second click **within 5 seconds** breaks it and the fight begins.
* Not in Creative or Spectator mode ("Hela will not come for a player in Creative or Spectator mode").
* **The seal heals** after a fight: 20 minutes after she is beaten, 10 after a fight that ends any other way ("The seal is still healing (12 more minutes)").
* Only one boss fight at a time, and the usual rules of every boss fight (see [Server Configuration](Server-Configuration#every-boss-fight)).
* A server can stop the seal calling her (`hela_boss.summonEnabled`). An operator can always start the fight with `/enhanced boss start hela force`, from anywhere and in any mode: it happens in Asgard's throne room. (Without `force`, the command follows the seal's rules.)

## The fight

**The arena** is the throne room (34 blocks round its middle): a wall keeps the fighters in, and nothing of the hall can be broken. The murals on the ceiling crack, one after another, to show Hela's conquests. Black-green smoke pours from the seal, and **she forms on the dais** (about 6 seconds), her headdress growing, and flings her arms wide: "So this is the house my father built on my silence."

**Health:** 600, and 35% more for each fighter after the first (counted as fighters join in the first 20 seconds); armour 10. Fire, falls, drowning and every effect but Glowing do nothing to her. Her bar is black and green.

### Phase 1: The Goddess (100% to 70%)

Every few seconds one of:

* **Necrosword Volley**: blades appear behind her with a green glint, then fly at you in a fan: **3** (5 when there are three fighters or more), **7** damage each.
* **Blade Wall**: cracks run across the floor toward you, then a line of black spikes bursts up, **8** damage each and a throw into the air. The spikes stand for a few seconds and block the way.
* **Shadow Step**: she melts into smoke and steps out behind the fighter farthest from her, then strikes.
* **Blade Melee**: close up, a combo of blows with two blades, **9** each.

### Phase 2: The Army of the Dead (70% to 35%)

"Rise. The dead of Asgard remember their queen." She rises on the dais and the **berserkers** climb out of the floor: undead Asgardian warriors with green eyes (24 health). A wave is **4**, and **2 more** for each fighter after the first (never more than 10 standing); a new wave comes when only two are left. **Fenris**, the giant wolf, joins once: **140 health and a bar of his own**, a bite of **10**, a pounce, and a howl that slows everyone near. She adds **Blade Rain**: a circle on the floor where you stand, and a second later twelve blades fall into it (**6** each).

**The warriors of Asgard join you:** Einherjar, Sif, Valkyrie and the Warriors Three come into the hall and fight the army (and Hela, when no berserker is near). See [Asgardians](Asgardians#fighting-alongside-you).

### Phase 3: The Strength of Asgard (35% to 0%)

"Asgard is my strength." Every **20 seconds** she kneels on the dais and **channels** for 4 seconds: green-gold threads rise from the floor to her and her bar flashes.

* **Break it:** deal **6% of her health** in those 4 seconds. "Her channel is broken! Strike now!" She is **stunned for 3 seconds** and takes **25% more damage** while she is.
* **Don't**, and Asgard heals her by **10%**.

Every 15 seconds the **Blade Storm**: a ring of spikes spreads out from her (to 12 blocks): **jump over it**. Her volleys are 5 blades now.

### Mjolnir and Stormbreaker

From the second phase, **she catches a thrown Mjolnir** (at most once a minute): "A hammer? I have broken better things." It **shatters in her hand**, as on the cliff in *Ragnarok*: the pieces fly at Thor's enemies and come back together in his hand, as when Thor shatters it himself (see [Thor](Thor#shattering-mjolnir)). Nothing is lost. **Stormbreaker** she can't stop: it flies past her hand. A server can turn the catch off (`hela_boss.catchMjolnir`).

### The other heroes against her

Shields that stop blades (the Chaos Shield, Agatha's, Apocalypse's, the Odinforce...) stop her necroswords; her blades are dark magic, so the ward of the Book of the Vishanti keeps them out. Telekinesis holds her for 2 seconds at most, never while she arrives, rises or falls. She can't be pinned or banished.

### The end

* **She falls:** "This... is not... the end of me." She staggers, the headdress cracks and she dissolves into black smoke. The murals turn back to peace, her army crumbles, and the **rewards** are given. The **first time**, **Odin wakes** ("Odin wakes: the Allfather rises from the Odinsleep") and **his vault opens**.
* **Any other end** (everyone dead or gone): "I will wait. I am good at waiting." She sinks back into the seal.

### The music

Three layers of an original battle theme play while you fight her: war drums and a deep drone, then driving low strings and a choir in phase 2, then brass and faster drums in phase 3. They fade with distance. The client option `bossMusic` turns them off with the other bosses' music.

## Rewards

Everyone who did at least **2% of her health** in damage, or struck the last blow, gets:

* **Hela's Headdress**: hold it for **3 seconds with no power** and you **become Hela** (below); the Headdress is used up. Someone who already has a power keeps it, or gives it to a friend. It never burns, and in Asgard it is never lost.
* **The right to Gungnir**: you may take Gungnir from its stand in Odin's vault and [become Odin](Odin#becoming-odin). (A headdress of the reward remembers whom it was given to: a winner who was away gets his right when it reaches him.)

A player with no power chooses by what he does: hold the Headdress and be Hela, or take Gungnir to the throne and be Odin.

## Becoming Hela: the hero

Hold **Hela's Headdress** with **no power** for **3 seconds**. Black smoke rises, the antlers grow from your hood, and you are **Hela**: "The Goddess of Death returns."

### The body

* **+10 health** (15 hearts) and **+4 armour**.
* **Strength from Asgard:** in Asgard she always has **Regeneration I** and deals **25% more damage** (`hela.asgardFactor`).
* She doesn't fly.

### Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** | Necrosword Volley | **3** black blades (5 with the Goddess of Death on) fly from behind her shoulders in a fan at what she looks at, **7** damage each. Cooldown 1.5 seconds. **Sneak + R: Blade Rain**: twelve blades fall over a 5-block circle where she looks (up to 32 blocks), **6** each. Cooldown 8 seconds. |
| 2 | **G** | Blade Wall | A line of spikes **10 blocks** long bursts from her feet toward where she looks: **8** damage and a throw into the air. They stand 6 seconds and block the way. Cooldown 7 seconds. **Sneak + G: Blade Burst**: a ring of spikes bursts up round her (4 blocks), **10** damage. Cooldown 6 seconds. |
| 3 | **V** (on/off) | Goddess of Death | The **headdress grows**: Strength II, Resistance I, and her blades hit **30% harder**. It costs a point of hunger every 10 seconds ("Too hungry for the form of the Goddess of Death"). **Sneak + V: Death Dash**: she dashes 8 blocks in smoke, through creatures, **6** damage to each she passes. Cooldown 5 seconds. |
| 4 | **B** | Raise the Dead | **3 berserkers** (5 in Asgard) climb out of the ground and fight for her for **40 seconds**: they attack what she attacks, or what attacks her, and never her friends. Cooldown 60 seconds. **Sneak + B: Fenris**: the great wolf fights beside her for **a minute**. Cooldown 90 seconds. The two share one key, so one waits for the other. |
| 5 | **N** (hold) | Death's Grasp | Her hand forward: every arrow or other shot that comes within 3 blocks in front of it **crumbles**. A thrown **Mjolnir is caught** and shatters (once a minute; it comes back to its Thor); a thrown **Stormbreaker** is stopped and drops to the ground. Up to **6 seconds**; cooldown 5 seconds. |

Her risen dead crumble when their time is up, or when she dies, leaves or loses her power.

### Looks

Press **H** to switch. See [Looks](Looks).

| Look | Id | What it is |
|---|---|---|
| **Goddess of Death** (default) | `goddess` | Her flat black suit with dark emerald lines and the black-green hood; with the form on, the **antlered headdress**: eight long black horns with a dark green sheen that fan out round her head and sweep back, the inner ones with a short prong (redrawn in 3.5.1). |
| **Arrival** | `arrival` | The long black cloak and swept-back hair of her arrival; the headdress only while the form is on. |
| **Your own skin** | `own` | Her powers in your own skin. |

With the form on she holds a black blade in each hand. Everyone sees the blades, the spikes, the smoke and the risen dead.

The same horned crown is worn by the boss (it grows during her arrival) and by her Shadows in [Heimdall's Watch](Heimdall#becoming-heimdall-the-guardians-watch); the Headdress item shows it from the front.

### The HUD

The form ("Goddess"), her risen dead and their seconds ("3 risen, 28s"), and the Grasp's seconds.

### Hela in the Hero Creator

Her six rows are in the [Hero Creator](Hero-Creator) (ids `hela.necroswords`, `hela.blade_wall`, `hela.goddess`, `hela.army`, `hela.grasp`, `hela.body`; see [Heroes' Abilities](Hero-Abilities#hela-6)). They need no item; Raise the Dead is a world power. The ready-made *Hela-like* hero starts with the necroswords, the walls of blades and the army of the dead.

## The Infinity Stones (3.6)

Hela is a god: she may hold every [Infinity Stone](Infinity-Stones) bare and use it, wield a crafted Infinity Gauntlet and Snap, and seek the Soul Stone at [Vormir](Vormir). Only the **Time Stone** stays quiet in her hand: it answers only a sorcerer.

## Numbers you can change

The fight is the `hela_boss.*` options (whether the seal calls her, her health and armour, the phases, every damage, the channel, the berserkers, Fenris's health, how long the seal heals, the Mjolnir catch); the hero is the `hela.*` options (damages, cooldowns, the form's strength, how many dead rise, the Grasp, Asgard's factor, and whether the Grasp breaks Mjolnir). See [Server Configuration](Server-Configuration#hela).

## Related

[Asgard](Asgard) · [Odin](Odin) · [Asgardians](Asgardians) · [Thor](Thor) · [Controls](Controls) · [Looks](Looks)

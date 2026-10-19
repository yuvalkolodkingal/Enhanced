# Hydra Soldiers

Since 3.6 every [Hydra outpost](Structures#a-hydra-outpost) is guarded by **Hydra soldiers**: agents in black tactical gear with Hydra's red emblem on the shoulder, armed with blue energy rifles powered like the Tesseract. They stand at the gate, watch from the tower, patrol the yard and the fence, and if they see you, they raise the **alarm** and call **reinforcements**. Beat them and an officer may drop a map to **Avengers Tower**, where the Tesseract is kept.

Hebrew: [עברית](Hydra-Soldiers-HE)

## The quick version

* **Where:** every Hydra outpost, the old ones of 3.3 too. They appear when a player comes within 64 blocks.
* **Who:** riflemen, enforcers with batons and riot shields, a sniper on the watchtower, and an officer.
* **Careful:** the officer raises the **alarm** the moment he sees you. A siren, red lamps, and **4 reinforcements**. Kill him first, or switch the alarm off at the radio console on the roof.
* **Loot:** energy cells, sometimes a **Hydra blaster** anyone can fire, and **Hydra Intel**, a map to the nearest Avengers Tower.
* They **come back** three Minecraft days later, while nobody is near.

## The garrison

Nine soldiers guard each outpost, each at his **post**:

| Post | Who | What he does |
|---|---|---|
| The gate | 2 **riflemen** | Stand guard either side of the gate. |
| The watchtower | 1 **sniper** | Watches from the top of the tower and never leaves it. |
| The yard | 1 **rifleman** and 1 **enforcer** | Patrol round the bunker. |
| The fence | 2 **riflemen** | Patrol a loop 6 blocks outside the fence. |
| The cellar | 1 **enforcer** | Stands at the foot of the cellar stairs. |
| The command room or the radio room | the **officer** | The command room by day, the radio room on the roof by night. |

They are not there until a player comes **within 64 blocks**; then they appear at their posts, out of sight. The **pillagers** that used to spawn at an outpost don't any more. A server can give an outpost up to 14 posts (`hydra.garrisonSize`: a door guard, the helipad, the command room, a third yard patrol, the radio room) or none.

## The ranks

| Rank | Health / armour | Weapon |
|---|---|---|
| **Rifleman** | 24 / 6 | An energy rifle: **bursts of 3 blue bolts**, 4 damage each. Every 6 bolts he stops to change the cell (a second and a half: your chance). Up close he hits with the rifle butt. |
| **Enforcer** | 30 / 10 | A **stun baton**: 6 damage and Slowness II for a second. His **riot shield** stops arrows and bolts from the front: go round him. |
| **Sniper** | 20 / 4 | A long rifle. A **red laser** points at you for a second and a half first (the warning), then one bolt of **8**. He reaches 40 blocks. |
| **Officer** | 36 / 8 | An energy pistol (3). He **raises the alarm at once**, and his men within 16 blocks get Resistance while he lives. |

Their bolts are blue streaks that don't burn and never break blocks. They pass through other Hydra soldiers (they never hit their own side).

## How they see you

* A soldier sees in a **cone of 120°** in front of him, up to **24 blocks**, if nothing is in the way. **Sneaking** halves that; at night in the dark it is less.
* He **hears** a block broken, an arrow landing or a fight within 8 blocks, and goes to look.
* They **never see** players in Creative or Spectator, invisible players, **Loki's illusions** or **Doctor Strange's astral spirit**.
* Riflemen keep their distance, strafe, and take **cover** behind the sandbags, the crates and the truck when they are hurt. Enforcers charge. Nobody goes more than 48 blocks from his outpost; then he walks back to his post and heals.

## The alarm

* **Who raises it:** the **officer**, the moment he sees an intruder. Any other soldier after **3 seconds of fighting**, if the officer is alive: he runs to a **radio console** (or calls the officer on his radio). **Kill him on the way**, or kill the officer, and the alarm never sounds.
* **The alarm:** "Hydra raises the alarm!" A **siren** wails, the **red lamps** of the radio room and the command room light up, and every soldier of the outpost comes for you.
* **The reinforcements:** 10 seconds later, "Reinforcements are coming!": **4 soldiers** (3 on Easy) come up the cellar stairs, and two of them through the gate from the road. At most **2 waves** per outpost per Minecraft day. They leave two minutes after the fight is over.
* **Switching it off:** go up to the **radio room** on the bunker's roof and **hold right-click for 3 seconds on a console** (an observer block): "Switching the alarm off... 2". Then "Alarm off.": the lamps go dark, no new wave comes, and the alarm stays off for the rest of the day.

## Drops and loot

Rare drops come only when **a player** kills the soldier (Looting adds 5% for each level), and **never from reinforcements**:

| Who | Drops |
|---|---|
| Rifleman | iron nuggets, gunpowder; a **Hydra Energy Cell** 20% |
| Enforcer | an iron ingot; an energy cell 15% |
| Sniper | iron nuggets; a **spyglass** 10%, an energy cell 30% |
| Officer | emeralds; **always** an energy cell; the **Hydra Blaster** 10%; **Hydra Intel** 25% |

* **Hydra Intel** is a map with a red **X** on the nearest **Avengers Tower** (where the [Tesseract](Infinity-Stones#the-six-stones) waits). No tower anywhere near: no map.
* **The outpost's chests** hold energy cells too (supplies 30%, the barracks 20%, the lab 40%), and the supply crates sometimes a blaster (5%). The vault still holds **Loki's Scepter**.

### The Hydra Blaster

**Anyone** can fire it, with or without a power: **right-click** shoots one blue bolt (5 damage), once a second. It has **96 shots**; a **Hydra Energy Cell** repairs it in an anvil. A server can turn it into dead metal (`hydra.blasterEnabled`): "The blaster is dead: this server switched it off."

## The heroes and the soldiers

* **Loki's Scepter** (MCU: Hydra kept it): hit a soldier with it and he is **turned**: "The Hydra soldier fights for you now!" His eyes glow blue; for **60 seconds** (an officer 30) he follows you, fights Hydra, never raises the alarm, and the others shoot at him as a traitor.
* **The Mind Stone's Mind Command** turns a soldier the same way, for its 20 seconds.
* **[Professor X](Professor-X)** hears their thoughts ("My boots hurt.", "Who left the gate open?", "Who keeps moving my coffee mug?"). He can **take over** a soldier: your click fires his rifle, and his friends shoot back at the traitor. **Freeze Minds** stops a soldier from raising the alarm.
* **The Phoenix's** Mind Control works on them as on any creature.
* **Shields** (Wanda's, Billy's, Doctor Strange's) and the **Reality Stone's bubbles** stop their bolts.
* **The Snap** counts them: they are monsters, and half of them turn to dust.
* Turned soldiers **never attack players**.

## Coming back

A dead soldier's post fills again after **3 Minecraft days**, but only while **nobody is within 64 blocks**: they come back while you are away. A server can choose `hydra.respawn` = `NEVER` (a cleared outpost stays empty) or `ALWAYS`. In **Peaceful** there are none.

## For servers

Every number is a `hydra.*` option on [Server Configuration](Server-Configuration#hydra-soldiers): whether there are soldiers at all, the garrison's size, the respawn, how far they see, their damage, the alarm, the reinforcements, the Scepter's turn, the blaster and the drop chances. Creative players have spawn eggs for a soldier and an officer.

## Related

[Structures](Structures#a-hydra-outpost) · [Loki](Loki) · [Professor X](Professor-X) · [Infinity Stones](Infinity-Stones) · [FAQ](FAQ#hydra-soldiers-36)

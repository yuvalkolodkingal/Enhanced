# Tommy

Tommy Maximoff (Speed), Wanda's speedster son from *WandaVision*. His speed showed up on Halloween night ("Chip off the old Maximoff block! You've got super speed!"), and in the Battle of Westview he crossed the street as a teal-green ghost and disarmed a whole S.W.O.R.D. squad.

![Tommy, close up](images/017-tommy-close-up.png)

## Becoming Tommy

Through the Witches' Road, with a Scarlet Witch Gene: craft and place a **Witches' Road Door**, open it holding the gene, walk the three trials of [The Witches' Road](Witches-Road), and choose **Tommy: Speed** at the end of the Road. The whole ritual is on [Billy and the Witches' Road](Billy-and-the-Witches-Road), and the first steps are on [Becoming a Hero](Becoming-a-Hero).

## Super speed and slow time

![Super speed](images/016-tommy-super-speed.png)

Tommy's speed leaves a translucent teal-green ghost trail and afterimages, low along the ground, like in the Battle of Westview. In 3.3 he has **two separate powers**, like [Quicksilver](Quicksilver): **G** makes him run, and **R** slows the world (new). Use either one alone, or both together.

* **Super Speed (G)** is on from the start: sprinting is super speed, three and a half times as fast, stepping up whole blocks, with no fall damage while running. It burns hunger slowly while he runs; with an empty hunger bar he only runs normally. Turn it off to sprint normally.
* **Slow Time (R)** slows everything within 32 blocks to a fifth of its speed, as Quicksilver Time does (arrows hang in the air, monsters crawl), whether he runs or not, with a **teal** shade and tint for everyone inside it. Since 3.5 it plays his song (below). It costs a little hunger while he is not running (running costs only the running), and it stops by itself when he is starving ("Slow Time stopped: you are starving").
* **Wall & Water Run** (on from the start; **Sneak + R** switches it): sprinting into a wall at super speed, he runs up it, along ceilings and round overhangs, and (3.6.1) **across water** as long as he keeps running, exactly as Quicksilver does. See [Running up walls and across ceilings](Quicksilver#running-up-walls-and-across-ceilings).
* **Like his uncle** (3.6.1): everything Quicksilver does on his feet, Tommy does too: super speed, stepping up whole blocks, no fall damage while running, walls, ceilings and water, the running pose and the trail (his own teal one), and **faster swimming** while his Super Speed or Slow Time is on.

## His Walkman: "Westview Blur" (3.5)

![A speed run across the map](images/018-tommy-speed-run.png)

Tommy has a Walkman of his own, with **his own theme, *Westview Blur***: bright, fast original synth-pop. It plays for everyone near him while he **runs with Super Speed** and while his **Slow Time** is on. When he stops running it keeps going for 2 seconds, then fades out, and if he runs again within 10 seconds it carries on where it was. He has no Walkman key: it is on. A server can give him another song (`speed.ogg` in the world's music folder), and you can play your own (`speed.mp3` in `config/enhanced/music/`). See [Music](Music).

## Abilities

| # | Key | Ability | How it works |
|---|---|---|---|
| 1 | **R** (toggle) | Slow Time (Sneak: Wall & Water Run on/off) | The world around him slows down (above). Off by default. **Sneak + R** switches Wall & Water Run on or off. |
| 2 | **G** (toggle) | Super Speed (Sneak: Dash Strike) | Sprinting is super speed (above). **Sneak + G** is the **Dash Strike**: in a blink he dashes up to **12 blocks** along his look, stopping at the first wall, and everything in his path takes **5 damage** and is knocked aside. Cooldown 5 seconds. |
| 3 | **V** | Whirlwind (Sneak: Sonic Boom) | For 3 seconds he runs circles around himself: everything within 8 blocks (creatures, players, arrows in flight) is pulled onto a spinning ring, lifted up to 3 blocks, and flung away when the whirlwind ends. Ghosts of Tommy race round the ring. Cooldown 10 seconds. **Sneak + V** is the **Sonic Boom**: he stops dead and the air he pushed breaks in a shockwave of **radius 6**: **4 damage** (half as much again if he was running at super speed, less towards the edge), creatures are thrown away and projectiles turned aside, with a teal ring. Cooldown 8 seconds (the two share the key's cooldown). |
| 4 | **B** | Vibrate Through Walls (Sneak: Disarm Blitz) | He vibrates his molecules and passes straight through the wall in front of him, up to 4 blocks thick (from the *VisionQuest* trailer and the comics). Not through bedrock or other unbreakable blocks, and not through a Hex he isn't allowed into. Cooldown 2 seconds. **Sneak + B** is the **Disarm Blitz**: in a blink he circles every armed creature within 12 blocks (up to 8 of them) and is back before they notice, their weapons in his arms: creatures lose whatever they hold, players only their weapon (swords, axes, bows, crossbows, tridents, maces, Stormbreaker). Everyone disarmed is slowed for 2 seconds. Mjolnir is far too heavy to snatch. Cooldown 8 seconds (on the same key). |
| 5 | **N** | Molecular Acceleration | From the comics: he vibrates whatever he touches (up to 5 blocks away) until it shatters. A block breaks and drops its item (anything up to the hardness of obsidian, never bedrock; it respects spawn protection and claims), and a creature takes 6 damage and is blasted back. Cooldown 1 second. |

Tommy does not fly. The Dash Strike and the Sonic Boom hurt players only with PvP on.

## Looks

![A speed run at sunset](images/019-tommy-speed-run-sunset.png)

*Halloween* (his WandaVision costume, a mini-Quicksilver: silver hair teased up, a blue tee with a silver lightning bolt), *Battle of Westview* (the green track jacket), *Speed* (the comics costume, green and white with amber goggles), and *Your own skin*. Press **H** to switch. See [Looks](Looks).

## Numbers you can change

Tommy's speed bonus (2.5, which is three and a half times as fast) and the hunger cost of running are the `tommy.*` options on [Server Configuration](Server-Configuration). His Slow Time uses Quicksilver's radius and factor (`quicksilver.*`) and costs `speedsters.tommyTimeExhaustion`; wall and ceiling running, the Dash Strike and the Sonic Boom are the `speedsters.*` options: see [Server Configuration](Server-Configuration#the-speedsters).

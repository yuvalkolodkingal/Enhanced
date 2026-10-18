# FAQ

## Becoming a hero

### How do I become a hero?

Each hero has one way in: a helmet, goggles or crown to wear, a stone to touch, a door to open (and the three trials behind it), a Tesseract to use, a hammer to lift, an Ember to hold, a spider's bite, a book to read (Agatha), glasses to put on (Cyclops), a wheelchair to hold (Professor X), or, for Thanos, Ultron and Apocalypse, a boss to beat. In Creative mode the Hero Creator makes any hero at once. See [Becoming a Hero](Becoming-a-Hero).

### I touched Loki's Scepter and died. Why?

The Mind Stone tears apart anyone who touches it **without** a Scarlet Witch Gene or a Synthezoid Core in the other hand (or the Ultron Program, Ultron's Core or the Tesseract, which wake Ultron, make you Ultron or call Thanos). Hold the gene (to become the Scarlet Witch) or the core (to become Vision) in one hand and use the scepter with the other. Creative and Spectator players are never killed. See [Scarlet Witch](Scarlet-Witch).

### Can I have two powers?

No. A player has exactly one power, and powers never stack. A hero who touches the Mind Stone with a gene in hand is told the gene can't bond, and nothing is used up.

### How do I get rid of my power, or switch hero?

A power is permanent: it survives death, logging out and changing dimension. Only an operator can change it, with `/enhanced power set <player> none` (or straight to another hero). See [Commands](Commands).

### I used the Tesseract and I am not Loki. Why?

The Tesseract makes you Loki only if you have **no power**, and no **Scarlet Witch Gene** in your other hand (with the gene it awakens the Scarlet Witch instead). A hero who uses it is simply taken to the block they look at, up to 48 blocks away. See [Loki](Loki).

## Mjolnir

### Why can't I lift Mjolnir?

There are two reasons:

1. **You are not worthy.** Only Thor, Vision, anyone with the *Hero of the Village* advancement (save a village from a raid) and Creative or Spectator players are worthy, unless the server turned on `thor.anyoneWorthy`, which makes everybody worthy.
2. **It is bound to someone else.** The first worthy player who lifts a Mjolnir claims it, and from then on only that player can lift it. Its tooltip says "Bound to" followed by the owner's name. Anyone else finds it far too heavy, and it slips out of their hands.

A Mjolnir nobody has claimed yet (its tooltip says "Unclaimed") belongs to the first worthy player who lifts it. If you are worthy and forge one yourself, it is yours the moment you take it from the table. If you are not worthy, it stays unclaimed and slips out of your hands: leave it on the ground for a worthy player to claim.

### Can I use a Mjolnir that belongs to someone else?

Not in Survival. Creative and Spectator players can always lift any Mjolnir that has an owner, without taking it over, so an operator can move one that way. A server that does not want owners at all can set `thor.mjolnirOwnerOnly` to `false` in the [server config](Server-Configuration).

### Is Stormbreaker bound to an owner too?

No. Anyone can wield Stormbreaker, and anyone who holds it can call the Bifrost. See [Thor](Thor).

### Can Thor call back someone else's Mjolnir?

No. Thor's recall never takes a Mjolnir that is bound to someone else: he calls his own, one nobody owns yet, or Stormbreaker.

### Why won't Mjolnir pin that player or creature?

Mjolnir can't pin the bosses (the Ender Dragon, the Wither and the Warden), players who are in Creative or Spectator mode, or a player at all if the server has PvP off. Only the hammer's owner can pin someone (or any worthy player, with `thor.mjolnirOwnerOnly` off), and only its owner can lift it off again. See [Thor](Thor).

## Build Magic

### Can Billy build as big as Wanda?

Not at first. Wanda's magic has no limit; Billy's is an apprentice's: a meter of 512 blocks that refills 16 a second, 24 blocks on a side, 24 blocks of reach, 10 undos, one spell at a time. It grows as he practises (every block he builds counts) through ten ranks, up to the Chaos Master, whose meter is 64 times as big (and whose Hex Beam is twice as strong). Near a Scarlet Witch his spells cost half. See [Build Magic](Build-Magic#billy-an-apprentice-who-learns).

### Can someone wreck my house with Build Magic?

Not by accident, and not on your land. A spell asks every claim and protection mod for every block (and the world border, spawn protection and Adventure mode), leaves chests that hold things alone (a Move spares a full chest too, and a Copy keeps a chest's things only where the caster may break it), never touches bedrock-like blocks, spawners or Mjolnir, never buries or lifts another player when PvP is off (nor puts water, lava or fire where one stands), and a spell that removes or replaces more than 64 blocks needs the right button held for a second. And **Ctrl + Z** undoes it, even tomorrow, without destroying anything built since. A server can set `build_magic.operatorsOnly`, and an operator can undo a child's mistake with `/enhanced build undo`. See [Build Magic](Build-Magic#what-magic-will-not-touch).

### How much disk does the undo history take?

Up to 512 MiB for each player and 4,096 MiB for all of them together, at the default settings (`build_magic.undoDiskMb`, `build_magic.serverUndoDiskMb`); past that the oldest spells are forgotten and the player is told. It lives in the world folder under `data/enhanced/build/`, one folder for each player, written before a spell changes the world, so even a crash leaves it undoable. A huge spell (over about 4 million blocks) is kept as a "lite step" on the disk instead of in memory, and `/enhanced build forget <player>` clears one player's history. See [Build Magic](Build-Magic#undo-ctrl--z-even-tomorrow).

### Does Build Magic lag the server?

It is built not to. Every spell is cut into small slices, and all the spells of the server together get at most 8 milliseconds of a tick (half when the server is struggling), so a huge spell just takes longer and the game keeps its 20 ticks a second. What a big spell does cost is **network**: every changed block is sent to every player who sees it. On a server hosted at home or rented with a modest upload, lower `build_magic.blocksPerTick` (5,000 keeps each watching player under about 0.4 MB a second). See [Server Configuration](Server-Configuration#build-magic).

### Why are some keys red in the Controls screen?

Build Magic uses Ctrl + Z, Ctrl + Y, Ctrl + R, Ctrl + F, Ctrl + C, Ctrl + X and Ctrl + V, and Minecraft's Controls screen marks a key as clashing when it shares a key with another, even though the game sends each press to the right place (Ctrl + Z of a witch never reaches Wanda's Z). So you will see red marks on those keys and on the keys they share (Sprint, Swap Item With Offhand, the toolbar keys, and the mod's **R**, **V**, **Z** and **Y**). It is only the screen being careful. See [Controls](Controls#build-magic-y).

## The Witches' Road, the books and the new powers

### I opened the Witches' Road Door and it did not ask me to choose Billy or Tommy. Why?

Since 3.2 the door asks you to **walk the Road** first: three trials (a Parkour, a structure to copy and an ice-boat race), and only at the end do you choose. A server can switch the trials off with `witches_road.enabled`; then the door is the instant choice of 3.1. See [The Witches' Road](Witches-Road).

### I fell off the Road. Did I lose my Gene?

No. Nothing on the Road can hurt you or take your Gene: a fall (or anything that would kill you) puts you back at your last checkpoint. The Gene is used up only at the end, when you choose. You can also leave at any time (right-click the exit door twice, or type `/road leave`) and keep it.

### Can my friends come with me on the Road?

No. Each player walks in a place of their own and nobody sees anybody (this is on purpose, to keep it simple and safe). Your friends can cheer from the world, and the server is told in chat when you win a trial.

### I cannot use my power, or the Book of the Vishanti, on the Road.

That is the rule: the Road is for people with no power, and the books are shut there, so the trials stay the trials.

### The boat does not move, or I am stuck. What do I do?

Hold **W**. If you are stuck against a wall or in the snow, press **Shift**: it puts you back on the last checkpoint, in a new boat (at most once every 5 seconds). The checkpoints cost nothing. See [The Witches' Road](Witches-Road#trial-3-the-ice-boat-race).

### Why can't I use the Book of the Vishanti or the Darkhold?

Only a **sorcerer** (Loki, the Scarlet Witch, Billy or Agatha) can read them. Another hero hears the runes whisper; a player with **no power** who holds use on the Darkhold for 3 seconds becomes [Agatha](Agatha). Inside a Hex with the [Hex Runes](Scarlet-Witch#hex-runes) up, the books do nothing. A server can let anyone use the Darkhold with `darkhold.anyoneCanUse`. See [The Sorcerers' Books](Sorcerers-Books).

### Do I have to be nearby for my base's ward to work?

No. A ward keeps working whoever is online, as long as its land is loaded. Only raising or forgetting it needs the book. No monster or boss can spawn or stay inside it, and what they shoot and dark magic stop at its edge; pets, animals, villagers and players' powers are not affected. An operator can list and remove wards with `/enhanced ward`.

### Why does the Chaos Shield never run out?

Since 3.2 it is unlimited: no hunger, no strength, no time limit, no recharge. It still takes three quarters of a second to form and still stops exactly what it stopped before. A server that wants the old limits sets `scarlet_witch.shieldUnlimited` to `false`. See [Scarlet Witch](Scarlet-Witch#4-chaos-shield).

### What is Instant?

Wanda's Instant level: in build mode, **Ctrl + I** makes her spells, undos and redos appear all at once instead of growing. It works up to 250,000 blocks a spell; a bigger one goes at normal speed. See [Build Magic](Build-Magic#the-instant-level-ctrl--i).

### Why is Apocalypse easier than before?

Because 3.2 weakened him: `apocalypse_boss.powerFactor` is 0.65, so he hits for about two thirds of the old damage, calls fewer lances, Horsemen and tornadoes and attacks more slowly, and his first shield is 100 instead of 150. Put the factor back to 1.0 for the old fight. See [Apocalypse](Apocalypse#how-strong-he-is-the-master-factor).

### The Darkhold poisoned me! Why?

It does not poison its holder by default: it poisons whoever **used magic on the holder**. If you are the one poisoned, the person who hit you with magic was holding a Darkhold. (A server can swap this with `darkhold.poisonTarget`.) Milk cures poison.

## The new heroes, the walls and the Hero Creator (3.3)

### Where is Loki's Scepter now?

Only in the vault of a **Hydra outpost**: a fenced Hydra base in taigas, snowy plains, forests and plains. The chest behind the iron bars in its cellar always holds one. `/locate structure enhanced:hydra_outpost` finds the nearest. It is no longer in End City, stronghold, bastion, Ancient City or woodland mansion chests. See [Structures](Structures#a-hydra-outpost).

### Can I find Mjolnir instead of forging it?

Yes: in a **Mjolnir's crater** (deserts, badlands, savannas, plains) it lies in the middle, owned by nobody. Only a worthy player can lift it. You can still forge one. See [Structures](Structures#mjolnirs-crater).

### I can't find the new places in my old world.

They are built only in **new chunks**: travel to land you have never visited. They are also rare, and only on level, dry ground in their biomes. `/locate structure enhanced:mutant_school` (or `hydra_outpost`, `mjolnir_crater`) tells you where the nearest is.

### I read the Darkhold and became Agatha. Can I undo it?

A power is permanent, as always: only an operator can take it away (`/enhanced power set <player> none`). Agatha is only made from a player with **no power** who holds use on the book for 3 full seconds; let go early and nothing happens. A server can turn the awakening off with `agatha.darkholdAwakens`. See [Agatha](Agatha).

### Cyclops's beam will not stop!

His glasses are off. Press **N** to put them back on (from your hand, inventory or off hand), or put them in your head slot. If you lost them, **N** shuts your eyes and stops it. It never hurts you, costs no food and ends if you die or log out. See [Cyclops](Cyclops#the-glasses-off-the-giant-beam).

### Why can't Professor X take over the Scarlet Witch, the Phoenix or Billy?

They are stronger telepaths: they throw him out, and he is hurt and stunned. Another Professor X and anyone under Magneto's Helmet are closed to him too, and players only with PvP on. See [Professor X](Professor-X#telepathy-whose-minds-are-open).

### My friend is steering my body! What do I do?

Wait: a player is steered for 8 seconds at most, and only with PvP on. Hurting Professor X's own body (it stands in a trance) sends him back at once, and putting on Magneto's Helmet closes your mind. See [Professor X](Professor-X#take-over-a-body).

### Why did Billy's Hex Barrier break at once?

Any power of a **Scarlet Witch** shatters it, whatever health it had (chaos magic). Anything else wears down its 30 health. When it shatters, wait 10 seconds; it mends while it is down. See [Billy](Billy-and-the-Witches-Road#the-hex-barrier-sneak--b-held).

### Why does nothing get through Wanda's Hex wall any more?

In 3.3 the wall keeps every power out unless Wanda allowed its user in: arrows, beams, explosions, telekinesis, mind powers, other people's Build Magic. And it lets in only those she approves, by any way: walking, flying, portals, teleports. See [Scarlet Witch](Scarlet-Witch#the-wall-keeps-powers-out).

### My powers stopped inside a Hex!

You are a sorcerer (Agatha, Loki, Billy, or holding the Darkhold or the Book of the Vishanti) and the Scarlet Witch has put the **Hex Runes** on its walls. Leave the Hex, or ask her to wipe them. See [Scarlet Witch](Scarlet-Witch#hex-runes).

### Can I use the Hero Creator in survival?

Only as an operator. It works in Creative mode for everyone (unless the server allows operators only) and comes from the creative tab. See [Hero Creator](Hero-Creator).

### Why does my custom hero look like me?

The skin you chose was refused: it must be a PNG of exactly 64x64 pixels, no bigger than the server allows (16 KB by default). Pick or drop another, or use a hero's skin as the base. See [Hero Creator](Hero-Creator#your-skin-seen-by-everyone).

### Some powers of my hero file were left out.

The server judged the file by its own rules: its budget, how many powers, the highest strength, and the powers it does not allow. You are told what changed. See [Hero Creator](Hero-Creator#saving-sharing-and-loading-the-json-file).

### Quicksilver Time no longer makes me fast!

In 3.3 slowing time and running are two keys: **R** slows the world, **G** turns Super Speed on (then sprint). Turn on both for the old feeling of running through a slowed world. Tommy has the same two (his Slow Time is new). See [Quicksilver](Quicksilver#two-powers-two-keys-33).

### How do I run up a wall as Quicksilver or Tommy?

Turn Super Speed on (**G**) and sprint straight into the wall, holding **W**. Quicksilver also needs **Wall & Water Run** on (**B**); Tommy's Wall Run is on unless you turned it off (**Sneak + R**). Keep **W** and the sprint going or you slow down and drop off; **Space** leaps off. You take no fall damage afterwards. See [Quicksilver](Quicksilver#running-up-walls-and-across-ceilings).

### Swinging as Spider-Man is too hard.

Make sure **Web Assist** is on (the **J** key; the HUD's G line says `assist`). Then **Sneak + G** and keep holding **G**: the web picks its own anchor, keeps you off the ground and out of the leaves, and swings on from web to web. In open land it swings from the sky (if the server allows it). Under a very dense, low canopy (a dark forest) there is nothing to swing from: climb up or zip out first. See [Spider-Man](Spider-Man#web-assist-easy-swinging-33).

### I want to aim my swings myself.

Press **J** to turn Web Assist off: the swing is the manual one again, aimed with the crosshair, **W** reels in and **S** lets out. A server can also turn Web Assist off for everyone.

## Magneto

### How do I mine with Magneto?

Crouch and press **R** when R isn't holding anything, ideally facing the wall: every ore within 8 blocks gives you its metal, through the rock, and turns to stone. It recharges in 5 seconds and costs a little hunger. See [Magneto](Magneto#the-magnetic-pull-sneak--r).

### Why didn't the chest give me its iron?

The pull never opens chests, barrels or shulker boxes, or takes what is in item frames and armor stands: it all belongs to someone (a sibling's chest). A server owner can turn `magneto.pullFromStorage` on to let it take the metal stacks out of what you can see. Claims and spawn protection stop it too.

### Why did the lanterns behind me stay?

The metal people build with (lanterns, rails, doors, hoppers) comes only from the cone you face (120 degrees) and only if you can see it, so your own base behind you is safe. Turn round to pull the rest. A hopper or a minecart that holds things, and a piston that is working, never come at all (empty it first).

## The Phoenix

### How do I become the Phoenix?

Find a **Phoenix Ember** in a chest (End City, bastion, Ancient City, stronghold library, Nether fortress and woodland mansion chests), hold it with no power, and hold right-click for 3 seconds. See [Phoenix](Phoenix) and [Becoming a Hero](Becoming-a-Hero#phoenix). A hero can't use it: "Your body already holds a power".

### Why can't I read, take over or freeze Magneto?

His helmet. As in the X-Men films, **Magneto's helmet** blocks every power of the mind: Mind Reading ("A cold metal silence: this mind is shielded"), Mind Sense (one "silence where a mind should be" for each helmet), Mind Control and the Psionic Field. It doesn't block telekinesis or the Phoenix Burst. The same goes for the biggest bosses (the Ender Dragon, the Wither, the Warden and the 3.0 bosses): they are too vast, and Loki's duplicates are illusions with no mind. Creative and Spectator players have no mind to read either.

### Why did the Burst not hurt that villager, golem or animal?

On purpose: one Burst must not wipe out a village or a farm. By default it hurts only hostile creatures, creatures that are after the Phoenix or her team, the bosses and players (with PvP on), and it spares villagers, Iron Golems, animals and other players' pets. A server owner can turn that off with `phoenix.burstHurtsPassives` (see [Server Configuration](Server-Configuration#phoenix-jean-grey)). It also spares the Phoenix, her tamed pets and teammates, and the creatures she has taken over.

### Why did the Burst, Mind Control or the Psionic Field not touch that player?

They don't affect players when **PvP is off** ("PvP is off: you can't reach their mind"), and never a player in Creative or Spectator mode. With PvP on, a player takes half of the Burst's damage (`phoenix.burstPlayerFactor`), is **held** for 3 seconds by Mind Control (it can't drive a player, whose controls are theirs) and for 3 seconds by the Psionic Field. Creatures are always affected.

### Why doesn't the Phoenix's Burst set the grass on fire, or break anything?

It never breaks a block. It lights fires only where the `mobGriefing` game rule is on and `phoenix.burstSetsFire` is on, only on flammable ground, never where spawn protection or a claim says no, and never inside a boss arena. Vanilla fire then burns out by itself.

### Why can't the Phoenix rise when she charges the Burst?

A Phoenix pinned under Mjolnir can't rise (or fly); she can still charge in place. The charge needs 6 food points and a rested Burst ("Too hungry to hold the Phoenix", "The Phoenix is still resting (12.3 s)").

## Spider-Man

### How do I become Spider-Man?

Get bitten by a **Radioactive Spider**: craft it (Fermented Spider Eyes, Glow Ink Sacs and a Slime Block) or find it in a mineshaft, dungeon or Ancient City chest, a Trial Chambers reward chest, or, rarely, from a Cave Spider you kill. With no power, right-click with it. You are sick for 7 seconds, and then you awaken. Drinking milk during those 7 seconds cures the bite and nothing happens. A hero can't use it: "The spider's bite changes nothing: your power does not mix". See [Spider-Man](Spider-Man) and [Becoming a Hero](Becoming-a-Hero#spider-man).

### Why can't Spider-Man fly?

He never could. He goes by **web**: **G** zips him to the block he looks at (up to 40 blocks) and on to the next, and **Sneak + G**, held, swings him on a web. Double-tapping **Space** does nothing for him. Jump at a wall and keep pushing to **climb** it, and he can walk on ceilings too.

### What can Spider-Man stick to?

Any solid block, a wall, a ceiling or the top of a block, except water, cobwebs, vines and ladders (nothing to hold) and the blocks a server lists in the tag `enhanced:cannot_cling`. He can't cling to a Chaos Shield, the Hex wall bounces him off like glass, and he lets go if he is held (Mjolnir's pin, telekinesis, Billy's spell freezes him in place instead). To go up a wall, jump at it and keep pushing, or stand against it and hold **Space**; walking into a wall on the ground does nothing, so he can still walk along a house. Switch the climbing off with **V**.

### Why did the webs vanish?

On purpose: the **Spider Web** blocks that a shot or a burst leaves **vanish by themselves after 10 seconds** (15 for organic webs), so the world never stays webbed. A hand or a sword clears one at once. A server can turn the blocks off (`spider_man.webBlocks`) or change how long they last (`spider_man.webBlockTicks`). A creature that is wrapped is held for 6 seconds, no longer than that.

### Why won't my legs come out?

Only the **Iron Spider** has them. In another look, **B** says "Only the Iron Spider suit has legs": press **H** until you wear the Iron Spider. They also stay in for 10 seconds after **Magneto's Armor & Weapon Strip** scatters the nanites ("The nanites are scattered"). They fold by themselves when you change look.

### Can Instant Kill hit players?

Not by default. Instant Kill strikes hostile monsters (and a neutral one only if it is after you), never a villager, a golem, a pet, an animal or a player, and the bosses and their minions take only the legs' normal 5 damage. A server owner can let it strike players with `spider_man.instantKillPlayers`, and then PvP must be on as well. Instant Kill needs the legs out, and **Sneak + B** twice within 3 seconds to confirm.

### Why is Spider-Man so slow on the wall?

He crawls at about 4.4 blocks a second (25% faster with the Iron Spider's legs, 1.6 times as fast with the Black Eyes), a bit slower than a sprint, and Slowness, a Webbed effect or Quicksilver Time slow him more. For speed, **zip** (26 blocks a second) or **swing**. A server owner can change the crawl with `spider_man.crawlSpeed`.

### What are the Black Eyes, and why did I crash?

**N** turns his eyes black for up to 30 seconds: much faster and stronger, and his webs are organic. It needs 6 food points, burns a food point every 4 seconds, and when it runs out (or your stomach is empty) you **crash**: Slowness II and Weakness for 10 seconds. Press **N** again to calm it before that and there is no crash. See [Spider-Man](Spider-Man#black-eyes-n).

### Why can't I web that creature or player?

The big bosses (the Ender Dragon, the Wither, the Warden, the Elder Guardian and the 3.0 bosses) can't be wrapped, nor can another Spider-Man, Creative and Spectator players, or players when **PvP is off**. A complete Chaos Shield stops a web, and Wanda's Hex wall refuses a zip across it. The Web Burst also spares villagers, Iron Golems, animals and pets, as every area attack does (a single Web Shot does wrap them). Spider-Man himself is never wrapped by a web.

## The bosses

### The bosses kill me in seconds. Can they be easier?

Thanos, Ultron and Apocalypse are made for heroes who come ready: good armour (diamond or netherite), food, and a hero's powers, best two or three of you, with a Scarlet Witch or a Phoenix for Apocalypse. A player in plain clothes falls in a few seconds. To make them easier, set `difficultyScale` in the `[boss]` section of `enhanced-server.toml` (in your world's `serverconfig` folder, or the server's) to `0.5`: every boss and Sentry has half the health, and Thanos, Ultron and the Sentries hit half as hard. Apocalypse's blows follow the game's difficulty instead, so set the world to Easy for him. See [Server Configuration](Server-Configuration).

## Thanos

### How do I call Thanos?

Hold the Tesseract in one hand and Loki's Scepter in the other, and hold right-click for five seconds. See [Thanos](Thanos#calling-him). Neither item is used up.

### I held the Tesseract and the Scepter and the Titan did not come. Why?

The action bar says why, in red, and nothing else happens (you do not explode and you are not taken anywhere). The usual reasons: you are not on solid ground with open sky above you and open country round you, the difficulty is Peaceful, he is already here or another boss is fighting, he fought less than ten minutes ago (five if every fighter fell or everyone left), you are not in the Overworld, or the server turned the call off. The table is on [Thanos](Thanos#calling-him).

### Why can't I kill Thanos quickly?

No hit takes more than 6 percent of his health and no second more than 15 percent, and he takes a new Infinity Stone every 90 seconds, each with new moves. The fight is meant to be won by a team, fast, by breaking his clench (100 poise of blows while he closes the Gauntlet) and by using the openings: the three seconds he kneels when a stone arrives, and the seconds after a stagger. See [Thanos](Thanos#how-the-fight-is-beaten).

### Does Thanos break my builds?

No. He never breaks or places a block, unless the server turned `blockDamage` on (and then only the Overload and the meteors make small craters). The Soul World, the Petrify Orb and the Time Snare change creatures, never the world.

### Can Thanos fly?

No. He steps through portals (the Space Stone), turns things to stone (the Reality Stone), drains souls, rewinds himself and locks time in a dome, but he walks. See [Playing Thanos](Thanos#playing-thanos).

### Can I get the Snap?

No, and never the sixth stone. Thanos the hero has the five stones the Mad Titan held; the Mind Stone belongs to Vision and to Loki's Scepter.

### Why do none of the stones answer me?

You are held: Billy's Freeze, the Phoenix's Mind Control or Psionic Field, a body turned to stone, the Soul World, or a cocoon of Apocalypse's sand. The film's flaw: a restrained Gauntlet hand cannot snap. Wait for it to end.

### Can I be Thanos and another hero?

No: one power per player. The Gauntlet works only for someone with no power, and a Thanos can give it to a friend.

## Ultron and Sokovia

### Does the island rise out of the real ground? Will my world be dug up?

No. At the warning the mod **copies** the island (the top few layers of the ground, what grows on it, and a cone of
rock made up under it) into the sky above the fight and builds it there; the real ground is never touched. The fight
goes on on the copy, and when it is over the copy is cleared and everyone is set down on the ground where the fight
began. Buildings and chests standing on the ground are left where they are (only the natural ground is copied); the
ritual refuses a site with something hanging in the way (an item frame, a painting, a boat) or something in the sky above.

### The server crashed (or I killed it) while Sokovia was up. What now?

Nothing, if you let the server start again. Every fight keeps a small note in the world folder
(`data/enhanced/sokovia`): first it only says who is owed the Program, and once the island is built it describes the island. At
the next start the mod reads the notes, clears any floating copy and, if a player's Ultron Program was used up for a fight
that never ended (on the ground too), gives it back. A server that is *stopped* normally (`/stop`) puts
everything back before it closes. Players who were up when it happened are set down on the ground, and are told so,
when they log in.

### I logged out in the sky. Where will I be?

The island may be gone when you come back. You are put on the ground where the fight was if it is, or on the island if it
is still there; either way you are not hurt. A player who is carried up cannot fall and cannot be hurt until they are set
down.

### Can I turn the lift off?

Yes. Set `ultron_boss.liftMode` to `TREMOR` (or `ultron_boss.liftEnabled` to `false`): the ground shakes and the fight
goes on where it began, with the same waves of Sentries and the same falling debris. Nothing is built and nothing needs recovering.
If the site cannot be lifted (protected ground, a claim, something in the sky) the ritual says why and uses nothing up.

### Why does Ultron refuse my ground?

The action bar says why, and nothing is used up. The most common reasons: the dimension has no open sky (not the Nether or the End); the ground is not level (it may rise and fall by 6 blocks at most across the island's circle (a radius of 20 blocks, about 40 across); trees do not count) or is mostly water, lava or non-walkable; the sky above, with a margin of 6 blocks, is not empty (a mountain, a tree canopy, a floating build); an item frame, painting, armour stand, boat or minecart is inside the circle; the ground is protected by spawn protection or a claim; it is too near the world border; the difficulty is Peaceful; or another boss fight is already running. Houses and chests on the ground are *not* a reason: the island is a copy, and only the natural ground is copied.

### Why can't I build, dig or run away on the island?

From the ritual until the fight is over, the arena is protected on purpose: nothing can be placed or broken, there is no water, lava or pistons, explosions break nothing, and teleports out are cancelled. Otherwise people would pillar up, dig a hole or hide and cheese the fight. Telekinesis may still borrow a block to throw: the hole mends in seconds and the block crumbles. A borrowed block you set down stands only 5 seconds, at most 3 of yours at once, never on another one or right next to anyone, so nobody can build a pillar, a wall or a hut out of the arena. Build Magic and Magneto's Magnetic Pull change no block there at all. A field pulls anyone who strays too far back onto the island (2 damage that cannot kill), so nobody falls to their death. Everything goes back to normal when the fight ends.

## Apocalypse

### How do I wake Apocalypse?

Craft a **Capstone** (a Nether Star, five Gold Blocks and a Lapis Block) and **Sneak + right-click it on sand** (or sandstone), by day, under the open sky, with **a Scarlet Witch or a Phoenix within 64 blocks**. See [Apocalypse](Apocalypse#waking-him).

### Why does nothing hurt Apocalypse?

While his golden shield is up, **everything but mind power does exactly nothing to him**: arrows, swords, Mjolnir, lightning, shrapnel, the Mind Stone, tamed wolves, TNT. Only the **Scarlet Witch's Chaos Beam and what she throws with telekinesis**, and the **Phoenix's Burst, her throws and her mind powers**, wear it down. When it breaks he kneels for twelve seconds and **everyone** can hurt him, down to a floor; then a stronger shield forms. Bring one of the two, then bring everyone else for the windows. See [Apocalypse](Apocalypse#the-shield-the-one-rule).

### Why does Apocalypse take so little from my sword?

Because only **Wanda's and the Phoenix's mind power** hits him *critically*: twice as strong on his shield and, when the shield is down, on his body. Everything else (a sword, an arrow, Thor's lightning, Magneto's shrapnel) does only 0.4 times as much to his body, even in a window, and nothing at all to his shield. And his health never heals, so the hits you do land stay. See [Apocalypse](Apocalypse#he-never-heals-wanda-and-the-phoenix-hit-critically).

### Why can I not wake him?

The action bar says why, in red, and nothing is used up. The usual reasons: it is night, there is a roof over the sand, the block is not sand or sandstone, there is not enough room round it, the spot is protected, the difficulty is Peaceful, another boss is fighting, or **no Scarlet Witch or Phoenix is within 64 blocks**: without one nobody could break his shield, so he does not wake. The table is on [Apocalypse](Apocalypse#the-ritual).

### I have no Scarlet Witch. Can I still fight him?

Not through the ritual: it refuses, so that nobody walks into a fight that cannot be won. A server owner can turn the check off (`apocalypse_boss.requireBreaker`); he still withdraws, and gives the Capstone back, if nobody who can break his shield is left for a minute.

### The Capstone is gone and he left. Do I get it back?

If he withdrew because nobody could break his shield, because the server stopped, because the world went Peaceful or because an operator stopped the fight: **yes** (at your next login if the server stopped). After a wipe, an abandoned fight or the 25-minute limit: no.

### Does Apocalypse break my builds?

No. He never places or breaks a block: his sand, his waves, his tornadoes and his rings are effects and damage, not blocks. The arena is also protected from the moment the Capstone is planted: nothing can be placed, broken, burned or blown up in it. See [Apocalypse](Apocalypse#the-arena-what-you-cannot-do).

### I was swallowed by sand and I cannot move. What do I do?

You are **entombed**. Inside it you can do nothing (no key, no blow, no item): **your friends break the cocoon** by hitting it (three sword hits), or a Scarlet Witch, a Phoenix, Billy or Magneto (if you wear metal) frees you by taking hold of you with telekinesis (and if you were holding something, you let go of it). It lets go by itself after five seconds, and the damage inside never takes you below two hearts. To avoid it, keep moving after the stripe of sand locks on your feet.

### What is the Essence of En Sabah Nur?

The reward of the fight. Every fighter who stayed a minute and hurt him or his shield gets one, and so does whoever landed the last blow. With **no power**, hold right-click with it for **three seconds** and you become Apocalypse. With a power you cannot use it (nothing is used up): keep it, or give it to a friend. See [Apocalypse](Apocalypse#the-reward-the-essence-of-en-sabah-nur).

### Why do the abilities of Apocalypse not hurt other players?

With PvP off the Sand Wave, the Entomb and the Sandstorm never hurt, hold or slow a player (mobs are always affected); the shield, the portal and Power Bestowal still work. See [Apocalypse](Apocalypse#becoming-apocalypse-the-hero).

## Abilities and keys

### The keys do nothing.

Ability keys work only for a hero, with no screen open. If you are a hero, check that the keys are not rebound or taken by another mod under *Options, Controls, Key Binds, Enhanced Powers*. See [Controls](Controls).

### How do I change my look?

Press **H**. **Sneak + H** goes back. Only Vision, Tommy, Billy, Loki and Thor have several looks. See [Looks](Looks).

### Why can't I see my armour?

Heroes are drawn without their armour, so it doesn't hide the characters. It still protects you as usual, and a player with no power is drawn with theirs. The elytra, a carved pumpkin or mob head and Magneto's Helmet, Wanda's Crown and Quicksilver's Goggles are always drawn. To see armour on heroes again, turn off *Hide heroes' armour* under *Mods, Enhanced, Config, Hero appearance*. See [Server Configuration](Server-Configuration#client-options-animations-effects-and-looks).

### Why can't Thor fly?

Thor flies by spinning his weapon, so only with Mjolnir or Stormbreaker in hand. See [Thor](Thor).

### Why did I die crossing the Hex wall?

When anyone without powers crosses Wanda's Hex wall without her permission, there is a 95% chance that they die: "rewritten by the Hex". The 5% who survive are thrown back out (3.3): only those she approves get in. Heroes can't be killed by the wall, but are thrown back. Wanda decides who may pass on the Hex Control screen. See [Scarlet Witch](Scarlet-Witch).

### Why can't Vision leave the Hex?

Wanda is in control of her Hex, and in *WandaVision* Vision starts to come apart at the edge of Westview. A Vision (either look) inside a standing Hex can't leave it, whether or not Wanda let him in: he is thrown back, comes apart a little (White Vision is only thrown back), and is told "Wanda's Hex holds you: only she can let you go". He can escape by hurting Wanda badly (30% of her health or less) or by defeating her. He is also free when the Hex dissolves, when Wanda shrinks it past him or leaves the world, and in Creative or Spectator. See [Scarlet Witch](Scarlet-Witch#vision-cant-leave-your-hex).

### How do I send Vision at an enemy?

When you raise a Hex as the Scarlet Witch, a Vision appears at your side. Look at a creature and press **Z** and he goes for it; look at nothing and press **Z** to call him back; **Sneak + Z** makes him hold his post. See [Scarlet Witch](Scarlet-Witch#the-vision-of-the-hex).

## Music and sharing

### Why is the mod for personal use only?

Quicksilver's Walkman plays *Sweet Dreams (Are Made of This)* by Eurythmics, and the recording is built into the mod. It is copyrighted, so the repository and the jars built from it are for personal use only. Keep the repository private and do not share the jar publicly.

### Can I change Quicksilver's song?

Yes. Put an `.mp3`, `.ogg` or `.wav` file in `config/enhanced/music/` in your Minecraft folder (the mod creates the folder the first time it starts). The first file in alphabetical order is played, and removing it brings back *Sweet Dreams*. See [Quicksilver](Quicksilver).

### Why does my Tesseract (or Loki's Scepter) hum? Can I turn it down?

Every Infinity Stone has a voice of its own. The Space Stone in the Tesseract and the Mind Stone in Loki's Scepter hum softly, to you and to everyone near you, for as long as the item is in a hand, and stop when you put it away. It is quiet on purpose. To turn it down, use the **Players** slider under *Options > Music & Sounds*. See [Items and Crafting](Items-and-Crafting).

## Servers

### Do players need the mod too?

Yes. The server and every player who joins need the same jar. See [Installation](Installation).

### Where are the balance numbers?

In `world/serverconfig/enhanced-server.toml`. Every option is on [Server Configuration](Server-Configuration).

### Does it respect claim and protection mods?

Telekinesis on blocks (Wanda, Billy, the Phoenix, Magneto), Build Magic, Magneto's Magnetic Pull and Tommy's Molecular Acceleration respect spawn protection, Adventure mode and claim mods, when a block is torn out and when it is set down. A block is never set down where someone stands, and a block whose holder has left the game falls as an item.

## This wiki

### The wiki is not showing, or is empty.

The wiki is published by a GitHub Action from `docs/wiki/` in the repository. The wiki must be enabled in the repository's settings (**Settings, General, Features, Wikis**), and its first page created once by hand in the GitHub UI, because GitHub makes the wiki's git repository only then. The *Publish the wiki* Action then fills it. Wikis of private repositories need a GitHub Pro, Team or Enterprise plan. The steps are on [Building from Source](Building-from-Source).

### I found a mistake on a page. Can I fix it here?

Edit the file in `docs/wiki/` in the repository instead: a change made on the wiki itself is overwritten by the next publish.

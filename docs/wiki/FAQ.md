# FAQ

## Becoming a hero

### How do I become a hero?

Each hero has one way in: a helmet, goggles or crown to wear, a stone to touch, a door to open, a Tesseract to use, a hammer to lift, an Ember to hold, or a spider's bite. See [Becoming a Hero](Becoming-a-Hero).

### I touched Loki's Scepter and died. Why?

The Mind Stone tears apart anyone who touches it **without** a Scarlet Witch Gene or a Synthezoid Core in the other hand. Hold the gene (to become the Scarlet Witch) or the core (to become Vision) in one hand and use the scepter with the other. Creative and Spectator players are never killed. See [Scarlet Witch](Scarlet-Witch).

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

When anyone without powers crosses Wanda's Hex wall without her permission, there is a 95% chance that they die: "rewritten by the Hex". The 5% who survive just keep going. Heroes can't be killed by the wall, but are thrown back. Wanda decides who may pass on the Hex Control screen. See [Scarlet Witch](Scarlet-Witch).

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

Wanda's telekinesis on blocks and Tommy's Molecular Acceleration respect spawn protection, Adventure mode and claim mods.

## This wiki

### The wiki is not showing, or is empty.

The wiki is published by a GitHub Action from `docs/wiki/` in the repository. The wiki must be enabled in the repository's settings (**Settings, General, Features, Wikis**), and its first page created once by hand in the GitHub UI, because GitHub makes the wiki's git repository only then. The *Publish the wiki* Action then fills it. Wikis of private repositories need a GitHub Pro, Team or Enterprise plan. The steps are on [Building from Source](Building-from-Source).

### I found a mistake on a page. Can I fix it here?

Edit the file in `docs/wiki/` in the repository instead: a change made on the wiki itself is overwritten by the next publish.

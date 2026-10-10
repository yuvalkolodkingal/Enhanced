# FAQ

## Becoming a hero

### How do I become a hero?

Each hero has one way in: a helmet, goggles or crown to wear, a stone to touch, a door to open, a Tesseract to use, or a hammer to lift. See [Becoming a Hero](Becoming-a-Hero).

### I touched Loki's Scepter and died. Why?

The Mind Stone tears apart anyone who touches it **without** a Scarlet Witch Gene or a Synthezoid Core in the other hand. Hold the gene (to become the Scarlet Witch) or the core (to become Vision) in one hand and use the scepter with the other. Creative and Spectator players are never killed. See [Scarlet Witch](Scarlet-Witch).

### Can I have two powers?

No. A player has exactly one power, and powers never stack. A hero who touches the Mind Stone with a gene in hand is told the gene can't bond, and nothing is used up.

### How do I get rid of my power, or switch hero?

A power is permanent: it survives death, logging out and changing dimension. Only an operator can change it, with `/enhanced power set <player> none` (or straight to another hero). See [Commands](Commands).

### I used the Tesseract and I am not Loki. Why?

The Tesseract makes you Loki only if you have **no power**. A hero who uses it is simply taken to the block they look at, up to 48 blocks away. See [Loki](Loki).

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

## Abilities and keys

### The keys do nothing.

Ability keys work only for a hero, with no screen open. If you are a hero, check that the keys are not rebound or taken by another mod under *Options, Controls, Key Binds, Enhanced Powers*. See [Controls](Controls).

### How do I change my look?

Press **H**. **Sneak + H** goes back. Only Vision, Tommy, Billy, Loki and Thor have several looks. See [Looks](Looks).

### Why can't Thor fly?

Thor flies by spinning his weapon, so only with Mjolnir or Stormbreaker in hand. See [Thor](Thor).

### Why did I die crossing the Hex wall?

When anyone without powers crosses Wanda's Hex wall without her permission, there is a 95% chance that they die: "rewritten by the Hex". The 5% who survive just keep going. Heroes can't be killed by the wall, but are thrown back. Wanda decides who may pass on the Hex Control screen. See [Scarlet Witch](Scarlet-Witch).

## Music and sharing

### Why is the mod for personal use only?

Quicksilver's Walkman plays *Sweet Dreams (Are Made of This)* by Eurythmics, and the recording is built into the mod. It is copyrighted, so the repository and the jars built from it are for personal use only. Keep the repository private and do not share the jar publicly.

### Can I change Quicksilver's song?

Yes. Put an `.mp3`, `.ogg` or `.wav` file in `config/enhanced/music/` in your Minecraft folder (the mod creates the folder the first time it starts). The first file in alphabetical order is played, and removing it brings back *Sweet Dreams*. See [Quicksilver](Quicksilver).

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

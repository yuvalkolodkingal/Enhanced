# The heroes and bosses of 3.0: how they hang on the shared code

Version 3.0 added five heroes (the Phoenix, Spider-Man, Thanos, Ultron, Apocalypse), built in parallel, one *lane* each,
on a **skeleton** that did every edit of a shared file for all five, and a shared **boss framework** (`boss/`). This was
the lanes' guide while they were built; they are merged now, and it stays as the reference for the plug-ins, the regions,
the event priorities, the boss framework and the rules they count on. Where it speaks of a lane, read "the hero's own
code". The plan it implemented is `integration.md` of the 3.0 design (section 5).

The order of the five, everywhere (regions, lang, docs, tests): `phoenix`, `spider`, `thanos`, `ultron`, `apocalypse`,
the order of Eilon's list.

## 1. Where the documentation is

Each hero has its wiki pages (`docs/wiki/<Hero>.md` and `<Hero>-HE.md`) and a section of `docs/DESIGN.md` and
`docs/DESIGN.he.md`: **13 Phoenix, 14 Spider-Man, 15 Thanos, 16 Ultron, 17 Apocalypse** (12 is 2.3's voices of the
Infinity Stones), since 3.1 **18 Build Magic**, and since 3.2 **19 the Witches' Road trials, 20 the sorcerers' books, 21 the beam system, 22 the rest of 3.2** (with Billy's Hex Beam and Barrier in 6.4 and 6.5). Their rows are in the shared pages (Home, Controls, Looks, Items and Crafting, Commands, Server
Configuration, FAQ, Becoming a Hero, Credits, the sidebar) and in the README, in both languages. While 3.0 was built
each lane wrote these as fragments that were stitched in at the end; a change now goes straight into the pages, English
and Hebrew together.

## 2. What belongs to whom

* **One owner per file.** A file belongs to the skeleton, to the framework (step 2) or to exactly one lane. A lane
  edits a shared file only in the *region* the skeleton reserved for it. If it needs a change in a shared file, it asks
  the skeleton owner for a ten-line change to the `v3` branch, merged within the day.
* **No lane edits** `CommonEvents`, `EnhancedConfig`, `Power`, `Toggle`'s or `Live`'s other regions, `AbilityHandler`,
  `PowerTicker`, `ModNetwork`, `PowerHud`, `ClientEvents`, `HeroAnimator`, `GestureRoutes` outside its own lines, or
  either lang file outside its block. They are done.
* **Regions.** In every append-only Java file the skeleton wrote five delimited regions, in hero order, marked
  `// ===== phoenix =====`, `// ===== spider =====`, `// ===== thanos =====`, `// ===== ultron =====`,
  `// ===== apocalypse =====`. A lane writes only between its own marker and the next. They are in `Toggle`, `Live`,
  `Stance`, `GestureId`, `ModTags`, `ModDamageTypes`, `ModItems`, `ModEffects`, `ModEntities`, `ModSounds`,
  `PowerLoopSounds.Kind`, `GestureRoutes` (the toggle, acting and state arms), `FlightProfile.of` and `RunPoses` (one
  line per hero), and in `ClientSetup` (renderers, model layers, player layers, screens, glow sources). A new enum
  constant at the end of a region needs the comma the region's last constant already has.
* **Toggle and Live bits are positions.** Adding a constant to a region moves the bits of the regions after it. That is
  free until a version is released; after 3.0.0 never insert, only append to the last region.
* **Merge rule when a conflict happens anyway:** keep both sides (everything is additive), then `./gradlew build`.

## 3. The plug-ins: how a hero hangs on the shared code

| Server (`ability/`) | Client (`client/render/`) |
|---|---|
| `HeroServer`: `onKey`, `tick`, `everyPlayerTick`, `applyBody`, `endAll`, `stopHold`, `onLogin`, `onLogout`, `onDeath`, `onRespawn`, `onChangeDimension`, `onServerStopped` | `HeroClient`: `hudStatus`, `shieldStyle`, `eyes`, `stanceTargets`, `edges`, `takeoffGesture`, `lean`, `drop`, `ambient`, `aimed`, `predictedGesture`, `registerGestures`, `cooldownGesture`, `flagGesture`, `liveGesture`, `grabGesture`, `flingGesture`, `startedElsewhere`, `gestureStarted`, `firstPerson`, `flightParticles`, `flightRibbons`, `tick`, `clear` |
| `HeroServers` (one line per hero), `HeroLifecycle` (what `CommonEvents` calls) | `HeroClients` (one line per hero) |
| your class: `PhoenixServer`, `SpiderServer`, `ThanosServer`, `UltronServer`, `ApocalypseServer` | your class: `PhoenixClient`, ... `ApocalypseClient` |

Every method defaults to doing nothing. The client plug-ins are in `client/render/` (not `client/hero/` as the plan said)
because what their animation hooks work on (`PoseContext`, `HeroAnimator.start`, `FirstPersonArms.Offset`, the helpers
of `FlightEffects`) is package-private there. Also shared and finished: `EntityGlow` (who glows through walls and in what
colour: register a source in `ClientSetup`, never edit the two mixins), `AreaTargets.mayHurt` (who an area attack may
hurt), `AwakeningItem` (the hold-to-use items), `ScepterRituals` (the table of what Loki's Scepter and the Tesseract do),
`ShieldCost` (the interface a projectile implements for Wanda's shield), `BossHooks` (implemented by boss and minion
entities), `BossFights` and the rest of the `boss/` package (the boss framework: section 7), `BossConfig` (`[boss]`).
Since 3.1 also: **`ability/Metal`**, the one rule of what is metal (items, blocks, entities, projectiles in flight, gear that is
real or crumbles): *every* power of Magneto asks it, and `Telekinesis.isMagnetic` is a one-line call to it (a new hero
power that touches metal asks `Metal`, never its own list); **`ability/BlockRights`**, the one rule book of changing a block
(`mayBreak`, `mayPlace`, `vanillaRights`, `postBreak`, `postPlace`, and `massEditRefuses`, the area rule of the two mass edits,
Build Magic and the Magnetic Pull); and the **`build/`** package, Build Magic's common and server code (`SpellPlanner` and
`SpellMath` are pure and shared by the client's ghost and the server's cells; `BuildRules`, `BuildJob`, `BuildScheduler`,
`UndoStep`/`UndoHistory`/`UndoFiles`, `Pocket`, `BuildMagic`, `BuildEvents`; nothing in it names a client class; the client's
half is `client/build/`; design in `DESIGN.md` section 18).

* **Config.** Each lane has its own class in `config/` with `define(Builder)`; `EnhancedConfig` calls it. Add your
  options there. A static field is assigned in `define`.
* **Hold abilities** (a key held down) must be released when the player dies, loses the power or opens a screen:
  list them in `HeroServer.stopHold`; `PowerTicker.stopHoldAbilities` calls it for every hero.
* **Events** live in your own `@EventBusSubscriber` class (`PhoenixEvents`, `SpiderEvents`, ...). The priorities that
  make independent subscribers compose (NeoForge: `HIGHEST` runs first):

| Event | HIGHEST | HIGH | NORMAL | LOW |
|---|---|---|---|---|
| `LivingIncomingDamageEvent` | Ultron: a carried player is immune except `BYPASSES_INVULNERABILITY`; Apocalypse: the hero's shield `guard` (before Wanda's branch) | Phoenix: fire damage cancelled; Ultron: lightning factor, Vision bonus; boss damage runs inside the boss's own `hurt` | the existing chain of `CommonEvents` | Thor's melee arcs (so a stopped blow lands nothing); Thanos stone effects (petrify factor) |
| `LivingDeathEvent` | | Ultron: Second Life (after vanilla's totem, which runs inside `hurt`) | `CommonEvents.onDeath` releases (and `HeroLifecycle.onDeath`) | |
| `ProjectileImpactEvent` | `SlowMotion.unslow` (existing) | Apocalypse boss: neutralise before impact | Wanda, Magneto, Vision (existing) | |
| `MobEffectEvent.Applicable` | | boss tags refuse effects | existing | |
| `LivingChangeTargetEvent` | | Phoenix: possessed creatures never target Jean's side | existing | |
| `EntityJoinLevelEvent` | one orphan sweep in `BossFights` for all marked entities | | | |
| `PlayerLoggedIn / Respawn / ChangedDimension` | | | `BossGifts.deliver`, existing calls, `HeroLifecycle` | |
| `ServerStoppingEvent` / `ServerStartingEvent` | `BossFights.stopAll` (restores any island) / Sokovia recovery | | | |
| `LevelTickEvent.Post` | | | `BossFights.tickLevel`, `SandHazards`, `PhoenixBurst`, existing | |

## 4. Lang, sounds, art

* **Lang.** `en_us.json` and `he_il.json` keep each hero's keys together, in hero order after the shared boss keys, with
  the same keys and the same `%` placeholders in both files (`DataTests` fails otherwise), the Hebrew natural, never a
  transliteration. (While the lanes were built each block started with a sentinel line, `"_lane.phoenix": ""` ...
  `"_end": ""`; they were removed at 3.0.0 and `SkeletonTests.noLangSentinelsRemain` keeps them out.) Prefixes: Phoenix
  `message.enhanced.mind_*`, `mind_control`, `psionic_field`, `phoenix*`, `screen.enhanced.mind_read.*`,
  `hud.enhanced.phoenix_*`; Spider-Man `message.enhanced.web*`, `zip`, `swing`, `crawl`, `sense`, `legs`,
  `instant_kill`, `black_eyes`, `bite`; Thanos `boss.enhanced.thanos*`, `message.enhanced.thanos*`, `stone`; Ultron
  `boss.enhanced.ultron*`, `message.enhanced.ultron*`; Apocalypse `boss.enhanced.apocalypse*`,
  `message.enhanced.apocalypse*`. Shared: `message.enhanced.boss.*`, `commands.enhanced.boss.*` (the existing commands
  use `commands.`, with an s).
* **Sounds.** Register your events in your `ModSounds` region and your entries in `sounds.json` (every entry needs its
  `.ogg`, a `subtitle` and the subtitle's translations: `DataTests`). `sounds.json` is a plain JSON object whose keys
  must be registered sound events, so it has no sentinel lines: add your block **right before the entry of the first hero
  after you** (Phoenix: before the first Spider-Man entry; the last lane appends at the end), so that two lanes never
  insert at the same place. Prefixes: `phoenix_`, `mind_`, `psionic_` | `web_`, `spider_`, `leg_`, `instant_kill_`,
  `black_eyes_` | `thanos_`, `gauntlet_`, `soul_`, `time_`, `reality_` | `ultron_`, `sentry_` | `apocalypse_`, `sand_`,
  `entomb`, `essence_` | `build_` (3.1, Build Magic's eight; `apocalypse_crit` is Apocalypse's). Files: the 3.0 voices are in
  `sounds/sfx/` (named `enhanced:sfx/<name>` in `sounds.json`); the nine of 3.1 are at the top of `sounds/` (`sounds/<name>.ogg`,
  `enhanced:<name>`). The six Infinity Stone voices are 2.3's, named `<stone>_stone` and `<stone>_stone_hum`
  (`space`, `mind`, `power`, `reality`, `soul`, `time`; the enum is `item/InfinityStone`): use them, do not make
  new ones.
* **Art.** The skeleton drew flat placeholder skins (`textures/entity/suit/*`), item textures, effect icons and the web
  block; a lane overwrites its own files (`gen_<lane>.py`). The names are fixed by `Variants` and the registries.

## 5. Tests

* Test classes are named for the lane (`PhoenixTests`, `SpiderManTests`, `ThanosBossTests`, ...). The state-heavy ones
  have a **batch of their own** with the lane's prefix: `phoenix_burst`, `spider_climb`, `thanos_fight`,
  `ultron_lift`, `apocalypse_shield`, `boss_framework`.
* A test of a boss fight, or of anything that keeps server-wide state from one tick to the next, uses
  `TestKit.bossFixture(helper)`: it clears `BossFights` and every hero's statics when the test ends, and since 3.1 also Build Magic (`BuildMagic.clear()`, `BuildScheduler.clear()`: jobs, histories, sessions, Billy's meter) and the Magnetic Pull (`MagnetPull.clear()`); a test of telekinesis clears `Telekinesis` and `BossGrip` in its own `f.onClose`. Drive a fight by
  `forceState` and `everyTick` instead of waiting.
* `SkeletonTests` pins the skeleton: the order of the powers, the tag files (every entry names something real), the melee
  tag, the Scepter and Tesseract table, the awakening items, the area rule, the boss facade, the payloads, the protocol
  (`ModNetwork.PROTOCOL_VERSION`, **"6"** since 3.2 (`theProtocolIsSix`; "5" in 3.1), and the round trip of the payloads of 3.1). Keep it green. It runs in its own batch, `skeleton`, on purpose: the grid puts
  tests side by side and a test that finishes in its first tick lets go of its chunks. A thrown hammer that overshoots
  its arena (`ThorTests.thrownMjolnirHitsAndComesBack`, `...Stormbreaker...`) then lands in a chunk that has stopped
  ticking, freezes and never comes home (it failed 2 times in 22 runs): those tests now throw only 12 blocks, which
  keeps the whole flight inside the 21-wide arena. A test of yours that lets something fly must keep it inside its own
  arena too.
  A lane's quick tests belong in the lane's own batch (one batch per lane, not one per test), not in the default one.
* Two **release gates** are on since 3.0.0: no pending animation route (`GestureRoutes.PENDING_*`: every ability,
  toggle and state of a hero has a gesture or a reason of its own), and no lang sentinel.
* A test that changes a server-wide setting (`f.config`, `f.pvp`) runs in a **batch of its own**: the tests of a batch
  run side by side and would see its value. `TestKit` puts a setting back only when the last test that changed it ends.

## 6. Merge order and gates (how 3.0 was put together)

Phoenix, Spider-Man, Thanos, Apocalypse, Ultron. A lane merges when: the build is green; `runGameTestServer` is green
twice in a row; its fragments and both languages of its lang block are delivered; its in-game checklist is done by someone
other than the author; it edited no file outside its ownership; and no existing test changed.

## 7. The boss framework (step 2)

The three boss lanes (Thanos, Ultron, Apocalypse) extend one framework in `boss/`; the rest of the mod talks only to the
facade `BossFights`. What a boss lane writes is what is *its own*: the entity, the brain, the attacks, the states, the
speech, the reward items.

**What the lane writes**

| You write | The framework gives you |
|---|---|
| `XFight extends BossFight` | the participants (everyone who comes within `arena.joinRadius`; Creative too, Spectators never), the ledger (`credit(player, "damage", n)`, `Participant.counter`), time present, the last blow, two boss bars (`bars()`), wipe, abandon, time-out, Peaceful, the end, rewards, refunds, region tickets, `speak`, `tell`, `spawn`/`adopt` |
| `XLane implements BossLane` | `refusal(starter, force)` (the site, the items, a hero nearby: all before anything is used up) and `create(starter, force)` (use up the ritual item, call `fight.refundOnFailure(item)`, return the fight); registered once with `BossFights.register(BossKind.X, lane)` |
| an arena: `new BossArena(level, center, radius, Containment.FIELD/WALL/NONE, protectBlocks)` (or a subclass) | protection of blocks, the field or the wall, the tractor, the teleport gate, drops that stay (`keepDrops`) |
| `onTick()`, `rewards(participant)` | the rest is optional: `onBegin`, `onEnd(why)`, `windDown(ticks)`, `onFinished(forced)`, `rewardMinDamage()` (Thanos: 3 percent), `rewardCounters()` (Apocalypse: damage + shield), `restTicks(why)`, `creditsDamageEvents()`, `onTelekinesisPress/Launch` (Ultron's heart; Launch is called for Magneto too since 3.1), `onJoin`, `status()`; **since 3.1** `hidesGround(pos)` (the columns the clients hide under an island), `lendRefusal(pos)` (why a block may not be borrowed), `locks(entity)` (an entity the fight carries), `carryLimit(entity, wanted)` (how far a held thing may be carried) |

**The rules it enforces, so the lanes do not**

* **One fight at a time** on the server (`[boss].maxActiveFights`, default 1, forced starts too), none in Peaceful, a boss that
  rests (`restTicks`) is not called by a ritual; each refusal is told to the starter in the action bar. A kind with no lane
  registered says "Nothing answers the call yet".
* **Ending.** `fight.end(BossEnd.X)`; the first ending wins. `VICTORY` rewards (present at least `rewardMinSeconds` *and* the
  fight's minimum damage, *or* the last blow; Creative never unless `creativeGetsReward`; into the inventory, else an owner-only
  drop that never despawns, else a gift in `BossGifts` that arrives at login, respawn or dimension change; plus `xpRewardPerPlayer`).
  `WITHDRAW`, `COMMAND`, `PEACEFUL` and `SERVER_STOPPING` refund the ritual item (to the inventory, or as a gift when the server
  stops); `WIPE`, `ABANDON`, `TIME_OUT` and `ABORT` do not.
* **Winding down.** After the end the fight keeps ticking, protected, with the arena still holding its fighters, until your
  `windDown(ticks)` answers true (the boss leaves, the island lowers); the default is at once, and the framework forces it after
  5 minutes. Then `onFinished(false)`, the fight's marked entities are discarded, the tickets and the bars go. A server stop
  (`ServerStoppingEvent`) and a second `/enhanced boss stop` call `onFinished(true)`: put everything back *now*, synchronously.
* **Entities.** Add your boss, minions and projectiles with `fight.spawn(entity)` (or `fight.adopt(entity)` *before* you add
  them yourself). An entity of a boss type (`BossFights.isFightEntity`) that no running fight owns never joins a level (the orphan
  sweep); an entity with an owner is exempt, and `BossFights.exemptFromSweep(predicate)` exempts more (the Ultron hero's drones).
  `BossFights.isMinion(entity)` is a marked entity that is not in the `bosses` tag.
* **Damage** players do to marked entities is credited by `LivingDamageEvent.Post` (turn it off with `creditsDamageEvents()` if
  the lane credits by itself); whoever kills a marked `bosses` entity struck the last blow, and a boss that never dies calls
  `fight.lastBlow(player)`.
* **The arena** cancels block breaking, placing, fluids, buckets, pistons, explosions, mob griefing and natural spawns inside
  its cylinder (`BossArenaEvents`); every ability of the mod that changes a block asks `BossFights.protects(level, pos)`
  (Tommy's acceleration, Loki's ice: **yours must too**: Spider-Man's webs, the Phoenix's fires), **through `BlockRights`** since 3.1; the one exception is block telekinesis, which *borrows*
  (`BossFights.lending`, `ArenaLoans`): the hole mends, the block crumbles, everything goes back when the fight ends. The two mass edits (Build Magic, the Magnetic Pull)
  ask `BlockRights.massEditRefuses`, which also closes every running arena (`build_magic.allArenasProtected`) and the ground the island hides. The teleport
  gate is one line in `HexManager.onTeleport` plus the Chorus Fruit and portal events. `FIELD` brings a stray fighter back (a
  damage that cannot kill, Slowness), `WALL` pushes back once a second and stops flight at `top`; a subclass overrides
  `rescueSpot`, `pushSpot` or `surfaceSpot` for its own ground (Ultron's island).
* **Hooks into the heroes** (done, do not repeat): `HexManager.judge` (`BossFights.isHexExempt`, set by `fight.setHexExempt`),
  `HexManager.onTeleport` (`allowsTeleport`), `SlowMotion` (`BossHooks.slowFactor`: 0 = not slowed), Magneto's strip and pull
  (`BossHooks.onMagnetoStrip`), every telekinesis (`onTelekinesisPress/Launch`, and since 3.1 `gripRefusal`, `holdTicks`, `onGripped`, `onLetGo`, `onGripRefused`, `isMetal`, `throwBack`: a boss is *held* as a boss by `BossGrip`), `AreaTargets` (a boss's slam spares its own
  minions).

**Wanda's shield, for the projectiles of the villains.** A projectile implements `ShieldCost` (`shieldCost()`; `oneOfAKind()`,
`turnHome()` and `returning()` for Thanos's sword, which the shield turns back and then lets by). `WandaAbilities.neutralize(level,
projectile[, colour])` is the shared half of `block`; `disintegrate(level, projectile, colour)` the sparks. A hero with a shield of
its own (Apocalypse) says `HeroClient.shieldStyle()` (a `ShieldStyle`: veil and line colours, texture, radius, maximum strength
and hold time) and the world renderer draws it; its HUD line is `PowerHud.shield(data, now, max, maxTicks)`.

**Client.** `BossBarOverride.register("boss.enhanced.thanos", drawer)` replaces the vanilla bar of every event whose name is
that translation key (name the bar with `Component.translatable(key)`). `ModelViewer.register(name, subject)` lets
`/enhanced_model <boss> <state> [tick]` show a client-only copy of a model in a state, frozen at a tick, for the people who draw.

**Tests.** `TestKit.bossFixture(helper)` lets every fight go, puts back the lanes the mod registered and the gifts that waited.
The framework's own tests (`BossFrameworkTests`, `BossRewardTests`, `BossArenaTests`, `BossHookTests`, `BossShieldTests`,
`BossClientTests`) use `BossTestSupport.TestFight`, `TestArena` and `TestLane` as a boss that does nothing; each fight test has a
batch of its own. A test class must not have a client class (`GuiGraphics`, `LayeredDraw`) in any method signature, lambdas
included: the test server cannot load it, and **a crash at registration still ends the Gradle build with "BUILD SUCCESSFUL"**
(look for `All N required tests passed` in the log).


## 8. What the review (step 3) fixed, and what a lane can count on

An adversarial review of the skeleton and the framework (every changed line of an existing hero, the boss lifecycle, a
dedicated server and a client in play) found the following. All of it is in the code and pinned by tests.

**Fixed**

* **A refused summoning no longer kills.** Once the Thanos lane registers its hold (`ScepterRituals.registerThanosHold`),
  the rows of the Mad Titan are the lane's alone: `Hold.begin` answers false after telling the player why, and *nothing else
  happens* (before, the Scepter fell through to its explosion, which kills, and the Tesseract to Loki's escape, which
  teleports a hundred blocks and makes the player Loki). `Hold.begin(player, hand)` gets the hand of the item that is being
  used. `ScepterRituals.callsThanos(player, hand)` says whether a use of the Tesseract is the call; `registerThanosHold` returns
  the hold it replaced (tests put it back; `TestKit.bossFixture` does).
* **`PhoenixPower` exists** (`ability/PhoenixPower`): `isPhoenix`, `isPhoenixMindPower(source)` (the `mind_power` tag with a
  Phoenix as the causing entity), `Kind`, `Listener`, `addListener`, `touched(phoenix, target, kind, amount)`. The Phoenix lane
  *calls* `touched` (before any refusal, once per cast); the Apocalypse lane *registers* a listener. The plan promised a stub
  and the skeleton had none.
* **`BossHooks.onLightning` has a call site** (`BossEvents.onLightning`, `LivingIncomingDamageEvent`, lowest priority, not for a
  cancelled hit): any entity that implements `BossHooks` and takes damage of the `minecraft:is_lightning` tag (Thor's beam, his
  arcs, the weather) is told, with Thor if the bolt is his. `onWebShot` stays the Spider lane's to call.
* **The gesture routes ask the hero's client first.** `GestureRoutes.cooldown/flag/live/grab/fling/startedElsewhere` and
  `GestureEffects.started` call `HeroClient.cooldownGesture(ability, left)`, `flagGesture(flag, on)`, `liveGesture(live)`,
  `grabGesture()`, `flingGesture()`, `startedElsewhere(id)` and `gestureStarted(player, state, id, param)` for a hero of 3.0. A lane
  never edits the arms of the others: those switches were the merge hot spots left. (`toggle`, `acting` and `reason` keep their
  regions.)
* **`AnimationMathTests` no longer needs a hero added to `heroes`** (every power with abilities is in), and its three
  data-driven arrays (the flight profiles, the take-off hand-overs, the gesture/stance pairs) have a region for each lane.
* `cannot_be_snatched` holds the Infinity Gauntlet (a speedster cannot pickpocket the reward, as the Thanos spec says).
  Whether the Essence and Ultron's Core join it is the lanes' call.
* `HeroLifecycle.onServerStopped` clears the boss fights even if a hero's own clean-up throws; `HeroServers.register` and
  `HeroClients.register` are public and return what they replaced (a test puts a recording plug-in in and the old one back).
* The Hebrew `power.enhanced.apocalypse.awaken` says "בתוכך" like the awakening lines before it.

**The contract of a `HeroServer`, tested by `SkeletonTests.aHeroPlugInIsCalledForTheRightPlayersAndOncePerSessionEvent`**

| Call | When |
|---|---|
| `onKey` | the hero's own player only, from `AbilityHandler` (an Entombed player's presses are dropped, their releases are not) |
| `everyPlayerTick`, `applyBody` | every alive player, every tick, whatever their power: the hero checks whose it is, and removes its modifiers from anyone who is not it |
| `tick` | the hero's own alive player, after the above |
| `stopHold(data)` | a dead player's tick (and `stopHoldAbilities`): clear your `Live` bits and flags that stand for a held key |
| `endAll(player, data)` | the power is being taken away: `data.power()` is *still* the hero; `applyBody` follows with the new one |
| `onLogin`, `onLogout`, `onDeath`, `onRespawn`, `onChangeDimension` | once per event for **every** plug-in and every player (check the player is yours); `onDeath` is the real `LivingDeathEvent` |
| `onServerStopped` | once, with the boss fights cleared even if you throw |

**Rules the lanes must follow that the framework cannot enforce**

* **A boss entity must stay.** A `Monster` despawns when no player is near and in Peaceful: the boss and its minions override
  `removeWhenFarAway` (false), call `setPersistenceRequired()` and, if they must not vanish in Peaceful, `shouldDespawnInPeaceful`
  (false); the fight ends the Peaceful case itself (`BossEnd.PEACEFUL`).
* **Boss minions have no owner.** The orphan sweep leaves alone any `OwnableEntity` whose owner is set (that is how the Ultron
  hero's drones survive a restart) and any `Projectile` a player shot (the Ultron hero's bolt, the Apocalypse hero's lance);
  for the rest of a hero's own boss-type entities (the Apocalypse hero's cocoon) use `BossFights.exemptFromSweep(predicate)`.
  Everything else of a boss type that a fight made must be `fight.spawn(...)`ed or `fight.adopt(...)`ed before it is added:
  a boss's bolt, orb, sword, lance or meteor too (an unmarked one is swept the moment it joins). A Sentry of the fight, a Phantom, a Horseman's Husk must report `getOwnerUUID() == null`, or
  the sweep would never remove them after a crash. If an entity must be owned, set the owner *before* `addFreshEntity`.
* **A payload handler is one static call into a client class** (`ThorFxPayload.handle` is the pattern): the client class is
  loaded only when the handler runs, on a client. Never name a client class anywhere else in a payload or in common code; the
  dedicated server (`./gradlew runServer`, with a client class on its path) refuses to load it.
* **Lane tests go in the lane's own classes** (`PhoenixTests`, `PhoenixAnimationTests`, ...), never in `AnimationMathTests`,
  `AnimationSignalTests`, `DataTests` or `SkeletonTests`, except for the regions in `AnimationMathTests`. A test of
  server-wide state takes `TestKit.bossFixture` and a batch of its own.
* **Do not edit `PowerAuraLayer`** (Phoenix's flame, Apocalypse's tint): add a `RenderLayer` of your own in your region of
  `ClientSetup.onAddLayers`. Do not edit `GestureEffects`: implement `HeroClient.gestureStarted`.
* A new gesture needs its keys (`HeroClient.registerGestures`), a route (`cooldownGesture`, `flagGesture`, `liveGesture`, a `toggle`
  arm of its own, or `HeroClient.startedElsewhere` for a payload or an edge) and, for a hero that flies with a profile, a take-off
  (`HeroClient.takeoffGesture`); `AnimationMathTests` fails an orphan.

**Shared files with exactly one lane that edits them (outside the regions)**

| File | Lane | Edit |
|---|---|---|
| `Telekinesis`, `TelekineticBlockEntity` | Phoenix | `Mode.JEAN` and a colour per mode |
| `Telekinesis` | Spider-Man | `isMagnetic`: one line for an Iron Spider (since 3.1 the rule is `Metal.isMagnetic`, which keeps that row) |
| `Telekinesis`, `TommyAbilities` | Apocalypse | `hold`, `vibrate`: `SandCocoonEntity.freeFrom` (two lines) |
| `LokiAbilities` | Thanos | two public methods for the Soul Sight |
| `mixin/client/PlayerRendererMixin` | Thanos | one inject for the first-person Gauntlet |
| `EnhancedRenderTypes` | Spider-Man | `webDecal` |
| `PoseContext`, `HeroAnimState`, `HeroAnimator` (`applyBody`, `onSpiderFx`) | Spider-Man | the wall and ceiling body turn, the anchor target |
| `ModAttachments` | Ultron | `SOKOVIA_RELOCATIONS` |
| `data/neoforge/loot_modifiers/global_loot_modifiers.json` | Phoenix, Spider-Man | the chest and spider loot (a JSON list: the second to merge keeps both sides) |


## 9. How 3.1 was put together

3.1 (Build Magic, telekinesis in the boss fights, Magneto's metal, Apocalypse's healing and critical hits) was built by one
method, and the rules are worth keeping for the next version.

* **One skeleton, then lanes.** A skeleton step (S) made, once and with the behaviour unchanged, everything shared: every key
  mapping, payload (protocol "5", six new payloads), attachment (`BUILD_DATA`, `BUILD_VIEW`), config option (every file under
  `config/`), tag file, lang key (both languages) and seam. Then lanes (TK telekinesis and Apocalypse, MAG Magneto, BS and BC Build
  Magic's server and client, ART sounds and icons, DOCS) each in its own git worktree, merged in the order MAG, TK, BS, BC. After
  the skeleton **no lane edits a lang file, a config file, `ModNetwork`, a payload record, `ModAttachments`, `ModTags`, a tag
  file, `BlockRights`, `Raycasts`, `PowerHud`, `ClientSetup`, `GlowingEyesLayer`, `TestKit`, `DataTests` or `V31SkeletonTests`**:
  a needed change is an entries file or a minimal `request(I)` commit for the integrator.
* **Seams.** The skeleton put five behaviour-neutral seams into `Telekinesis`/`BlockLifting` (the metal rule moved to `Metal`, its
  grip check `Metal.staysMagnetic`, Magneto in the projectile raycast, `MagnetPull.press` for Sneak + R, the time a grip ended), so the
  Magneto lane never opened files the telekinesis lane rewrote. **One owner per file**, the whole merged `press` order written once.
* **One rule book.** Everything that changes a block asks `BlockRights` (section 3); Build Magic never borrows from an arena.
* **What Build Magic shipped with.** All eleven spells: Land and Grow (Swell, Shrink, Bloom) are in 3.1, not 3.2; the wheel shows
  eleven pictures, with the digits 1 to 9 and 0 for the first ten. Nine sounds (eight of Build Magic, Apocalypse's critical hit) and
  45 pictures of `textures/gui/build/` (11 spells, 19 styles, 8 shapes at 32×32, 7 small ones at 16×16; the four `mouse_*` are spare,
  the client draws its mice). The server code is `build/` (design `DESIGN.md` 18), the client's `client/build/`; the operator's
  tools are `/enhanced build` (`BuildCommands`, joined in `EnhancedCommand`).
* **Journals forget only what a save holds.** The arena journal (`ArenaLoans`, `ArenaJournalWriter`) and the island's note
  (`SokoviaJournal`) retire a repair only after **two** world saves of its dimension (an autosave queues its chunks to the IO worker
  *before* `LevelEvent.Save`; the island's note gets a `<fight>.nbt.retiring` file, state `LANDED`, rewritten with `refund` false at the first
  save and deleted at the second or at `ServerStoppedEvent`; a ritual's note is still deleted at once).
* **Write ahead.** Build Magic writes a section's undo record before it changes the section's first cell (`WriteAhead` reads up to
  32,768 cells ahead of the writing cursor; `BuildJob.gate` lets a cell be written only when its record is on disk), so a crash never
  leaves a change its saved record lacks; the history is `data/enhanced/build/<uuid>/` (`history.dat`, `step-*.bin`, `pocket.bin`),
  written off the main thread and forced at every world save. A Move is compare-and-swap per cell, and its block-entity data is
  linked from source to target, so a chest is never in two places after an undo or a redo (a Move also spares a full chest at its
  target).
* **The budget of memory.** A step is held whole in memory only while it fits (about 14 bytes a cell and 12 KiB a section against
  `undoMemoryMb` and half of `serverUndoMemoryMb`); a bigger spell is a *lite step* (a bit a cell, its records on disk are the step; a
  Move is never lite), and past the disk share it asks for the long hold and runs without a step ("Too big to remember"). The caps
  look at memory and disk once a second. Findings that shaped this: an unbounded working step was 4.5 GB for a radius-400 ball.
* **Reviews.** Each of the lanes had independent reviews (one for the spec, one that attacked it) with fixer rounds after them,
  and Build Magic had a joint check of its server and its client in a real game (a dedicated server, two clients, Billy and Wanda, English and Hebrew); the
  reviews found, among other things, a Move that duplicated chest contents through undo, a Move that overwrote a full chest, Copy and
  Repeat that read a claim's vault, PvP-off protection that ignored fluids and fire, and Billy's cast arc drawn by nobody. Their
  tests are `BuildReviewTests`.
* **Event priorities** (5.6 of the spec; HIGH for the landing guard and the journals' recovery, LOW for Build Magic at server stop, so
  arenas are put back first, islands cleared second and spells last) are in each handler's Javadoc.
* **Testing.** Server-wide state (`BuildScheduler`, `MagnetPull`, `Telekinesis`, `BossGrip`) gets a batch of its own and
  `TestKit.bossFixture`; a test never names a client type in a signature or lambda, and none touches a class with client-typed static
  state (the pure `MagnetStreamMath`, `BuildGrammar`, `BuildHints` are tested instead). Every merge passed the full GameTests twice in a row
  (1,315 required tests at the end).
* **The joint check's findings were all fixed** (ten, none blocking), after the lanes were merged: every line the game says is at most 56
  characters and plural-safe, and the action bar's line stands aside from the hint strip; a scroll gesture keeps the face it pushes; the
  Copy ghost stays for the next stamp; a tap of Y with the wheel open closes it and build mode stays on (and a digit with Y held closes
  it for good); the hold rows say each spell's own verb; a spell whose every cell is protected is refused before it is cast
  (`NOTHING_CAN_CHANGE`, appended as code 41: a fizzle, a grey ghost, no sound, no charge, no step); the cast, the hum and the undo are
  heard at the caster. Four tests were added (line length, the face latch, the all-protected refusal, the sounds at the caster).

## 10. How 3.2 was put together

3.2 (the Witches' Road trials, Billy's Hex Beam and Barrier and ten ranks, Wanda's unlimited shield and Instant level, the Book of the Vishanti and the Darkhold, a weaker Apocalypse) used the method of 3.1 with fewer, bigger lanes, each in its own git worktree and branch, merged into an integration
branch (`v32`). The player-facing description of every rule is in `DESIGN.md` **19 (the Road), 20 (the books), 21 (the beam system), 22 (the rest of 3.2)** and, for Billy and Wanda, **6.4, 6.5, 18.6 and 18.14**.

* **Lanes.** ABW (the Apocalypse master factor, Wanda's shield, the beam infrastructure, Billy's beam and barrier), BUILD (Billy's ten ranks and the Instant level), ITEMS (the Book of the Vishanti, then the Darkhold on ABW's beam), ROAD (four steps, one agent each: the framework and the Parkour, Copy the Structure, the Ice-Boat Race, the ending) and DOCS. Shared files (`EnhancedConfig` and the config classes,
  both lang files, `ModNetwork`, `ModAttachments`, `ModTags`, `ClientSetup`, `PowerHud`, `TestKit`, `DataTests`) were edited minimally and additively, every key in both languages; the integrator resolved the conflicts (they were all "keep both sides") and bumped the protocol once.
* **Protocol "6".** `ModNetwork.PROTOCOL_VERSION` is **"6"** since 3.2 (`SkeletonTests.theProtocolIsSix`): the payloads `BeamPayload`, `VishantiActionPayload`, `WardSyncPayload`, `BuildActionPayload.INSTANT`, `BuildFxPayload.POP` and the changed `OpenRoadPayload` and `RoadChoicePayload`. A 3.1 client cannot join a 3.2 server.
* **The beam is shared code.** A second beam (the Darkhold's) is a `BeamStyle` and a `BeamSpec` on the one `ContinuousBeam`; see `DESIGN.md` 21 before writing a third.
* **The Road is a framework.** A trial implements `RoadChallenge`, draws on a `RoadCanvas` for one player in one cell of the dimension `enhanced:witches_road`, and is registered in `RoadChallenges` with an order number; the framework does the cells, tickets, persistence, checkpoints, falls, death, the timer, the gate to the next trial, abandon, login, the rules and the commands (`DESIGN.md` 19.6). A test that swaps the
  registry (`RoadChallenges.swap`) has a batch of its own.
* **Tests.** 1,315 game tests in 3.1; **1,471** on the last merged tree before the Road's ending (`All 1471 required tests passed`, about 5.4 minutes), more with the ending. New batch families: `abw_*` (Apocalypse numbers, shield, beam, barrier), `build_instant_*`, `items_*` (Book, ward, Darkhold), `road_*` (framework, Parkour, structure copy, boat). Two things to remember when running them: delete `run/world` **and**
  `run/config/enhanced-server.toml` first (a generated 3.1 server file keeps the old defaults and fails the new-number tests), and expect the shared memory of a machine running several gradle builds to kill a JVM now and then (exit 137 is not a test failure). `theIslandIsSafeFromEveryFeature` (V31CrossTests) once timed out under load; it now stands the cast preview's clock still.
  `TestKit.fixture()` switches `scarlet_witch.shieldUnlimited` and `billy.beam` off for every test (so the older shield and Hex Bolt tests keep testing the limits and the bolt); a test of the new behaviour sets them back with `f.config`. A test that polls from inside a callback must use `ApocalypseTestSupport.every` (`f.everyTick` inside a callback crashes the server tick).
* **The wiki's config page and its checker.** Every option has a row with its real default and range, in both languages; `check_config32.py` (the 3.1 checker with three additions: sections pushed with a comment, helper methods inlined, the rank-practice loop expanded) compares each config class with the page, and `check_wiki.py` and `parity.py` are run on the pages.

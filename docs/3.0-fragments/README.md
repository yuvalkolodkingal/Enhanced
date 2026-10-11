# 3.0: the lanes' guide and the documentation fragments

Version 3.0 adds five heroes (the Phoenix, Spider-Man, Thanos, Ultron, Apocalypse) that are built in parallel, one lane
each. The **skeleton** (this commit series) already did every edit of a shared file, for all five, so that no lane edits
one. This file says what the skeleton gives a lane, what a lane may touch, and how its documentation reaches the
shared pages. The plan it implements is `integration.md` of the 3.0 design folder (section 5); where this file and a
spec disagree, `integration.md` wins.

Lanes: `phoenix`, `spider`, `thanos`, `ultron`, `apocalypse` (the order of Eilon's list, and the order of everything
below). The folder of each lane in this directory is where its documentation fragments go.

## 1. Documentation fragments

A lane never edits a shared page. It writes, in `docs/3.0-fragments/<lane>/`:

| File | What |
|---|---|
| `Phoenix.md`, `Phoenix-HE.md` (and so on) | the new wiki pages of the hero, complete, English and Hebrew |
| `design.en.md`, `design.he.md` | the hero's section of `docs/DESIGN.md` and `docs/DESIGN.he.md`, complete |
| `Home.row`, `Controls.rows`, `Looks.section`, `Items.rows`, `Commands.rows`, `Server-Configuration.section`, `FAQ.entries`, `Becoming-a-Hero.section`, `README.rows` | one small file per shared page, holding only that lane's row or paragraph (a file that is not needed is left out) |

The documentation step (Z) applies all fragments in one commit, in hero order, to both languages.

**DESIGN section numbers.** Section 12 is the voices of the Infinity Stones (2.3). The heroes of 3.0 are
**13 Phoenix, 14 Spider-Man, 15 Thanos, 16 Ultron, 17 Apocalypse**. Sections 0 to 12 keep their numbers; nothing is
renumbered.

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
| `HeroServer`: `onKey`, `tick`, `everyPlayerTick`, `applyBody`, `endAll`, `stopHold`, `onLogin`, `onLogout`, `onDeath`, `onRespawn`, `onChangeDimension`, `onServerStopped` | `HeroClient`: `hudStatus`, `eyes`, `stanceTargets`, `edges`, `takeoffGesture`, `lean`, `drop`, `ambient`, `aimed`, `predictedGesture`, `registerGestures`, `firstPerson`, `flightParticles`, `flightRibbons`, `tick`, `clear` |
| `HeroServers` (one line per hero), `HeroLifecycle` (what `CommonEvents` calls) | `HeroClients` (one line per hero) |
| your class: `PhoenixServer`, `SpiderServer`, `ThanosServer`, `UltronServer`, `ApocalypseServer` | your class: `PhoenixClient`, ... `ApocalypseClient` |

Every method defaults to doing nothing. The client plug-ins are in `client/render/` (not `client/hero/` as the plan said)
because what their animation hooks work on (`PoseContext`, `HeroAnimator.start`, `FirstPersonArms.Offset`, the helpers
of `FlightEffects`) is package-private there. Also shared and finished: `EntityGlow` (who glows through walls and in what
colour: register a source in `ClientSetup`, never edit the two mixins), `AreaTargets.mayHurt` (who an area attack may
hurt), `AwakeningItem` (the hold-to-use items), `ScepterRituals` (the table of what Loki's Scepter and the Tesseract do),
`ShieldCost` (the interface a projectile implements for Wanda's shield), `BossHooks` (implemented by boss and minion
entities), `BossFights` and the rest of the `boss/` package (the boss framework: section 7), `BossConfig` (`[boss]`).

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

* **Lang.** `en_us.json` and `he_il.json` have one block per lane, each starting with a sentinel line
  (`"_lane.phoenix": ""` ... `"_end": ""`; Minecraft ignores unknown keys). The skeleton filled the names of every
  registered thing; a lane adds its messages, HUD words, subtitles and screens after its own sentinel, in both files with
  the same keys and the same `%` placeholders (`DataTests` fails otherwise), the Hebrew natural, never a
  transliteration. Prefixes: Phoenix `message.enhanced.mind_*`, `mind_control`, `psionic_field`, `phoenix*`,
  `screen.enhanced.mind_read.*`, `hud.enhanced.phoenix_*`; Spider-Man `message.enhanced.web*`, `zip`, `swing`, `crawl`,
  `sense`, `legs`, `instant_kill`, `black_eyes`, `bite`; Thanos `boss.enhanced.thanos*`, `message.enhanced.thanos*`,
  `stone`; Ultron `boss.enhanced.ultron*`, `message.enhanced.ultron*`; Apocalypse `boss.enhanced.apocalypse*`,
  `message.enhanced.apocalypse*`. Shared: `message.enhanced.boss.*`, `commands.enhanced.boss.*`
  (the existing commands use `commands.`, with an s). The sentinels are removed at the release; a game test fails after
  the 3.0.0 tag while any remains.
* **Sounds.** Register your events in your `ModSounds` region and your entries in `sounds.json` (every entry needs its
  `.ogg`, a `subtitle` and the subtitle's translations: `DataTests`). `sounds.json` is a plain JSON object whose keys
  must be registered sound events, so it has no sentinel lines: add your block **right before the entry of the first hero
  after you** (Phoenix: before the first Spider-Man entry; the last lane appends at the end), so that two lanes never
  insert at the same place. Prefixes: `phoenix_`, `mind_`, `psionic_` | `web_`, `spider_`, `leg_`, `instant_kill_`,
  `black_eyes_` | `thanos_`, `gauntlet_`, `soul_`, `time_`, `reality_` | `ultron_`, `sentry_` | `apocalypse_`, `sand_`,
  `entomb`, `essence_`. The six Infinity Stone voices are 2.3's, named `<stone>_stone` and `<stone>_stone_hum`
  (`space`, `mind`, `power`, `reality`, `soul`, `time`; the enum is `item/InfinityStone`): use them, do not make
  new ones.
* **Art.** The skeleton drew flat placeholder skins (`textures/entity/suit/*`), item textures, effect icons and the web
  block; a lane overwrites its own files (`gen_<lane>.py`). The names are fixed by `Variants` and the registries.

## 5. Tests

* Test classes are named for the lane (`PhoenixTests`, `SpiderManTests`, `ThanosBossTests`, ...). The state-heavy ones
  have a **batch of their own** with the lane's prefix: `phoenix_burst`, `spider_climb`, `thanos_fight`,
  `ultron_lift`, `apocalypse_shield`, `boss_framework`.
* A test of a boss fight, or of anything that keeps server-wide state from one tick to the next, uses
  `TestKit.bossFixture(helper)`: it clears `BossFights` and every hero's statics when the test ends. Drive a fight by
  `forceState` and `everyTick` instead of waiting.
* `SkeletonTests` pins the skeleton: the order of the powers, the tag files (every entry names something real), the melee
  tag, the Scepter and Tesseract table, the awakening items, the area rule, the boss facade, the payloads, the protocol
  (`ModNetwork.PROTOCOL_VERSION`, "4"). Keep it green. It runs in its own batch, `skeleton`, on purpose: the grid puts
  tests side by side and a test that finishes in its first tick lets go of its chunks, which can freeze a weapon that
  `ThorTests.thrownStormbreakerHitsAndComesBack` throws out of its arena (it failed 3 times in 23 runs before the move).
  A lane's quick tests belong in the lane's own batch (one batch per lane, not one per test), not in the default one.
* Two **release gates** pass until the version is 3.0.0 and then fail while the work is unfinished: no pending animation
  route (`GestureRoutes.PENDING_*`: every ability, toggle and state of your hero has a gesture or a reason of its own),
  and no lang sentinel.

## 6. Merge order and gates

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
| `onTick()`, `rewards(participant)` | the rest is optional: `onBegin`, `onEnd(why)`, `windDown(ticks)`, `onFinished(forced)`, `rewardMinDamage()` (Thanos: 3 percent), `rewardCounters()` (Apocalypse: damage + shield), `restTicks(why)`, `creditsDamageEvents()`, `onTelekinesisPress/Launch` (Ultron's heart), `onJoin`, `status()` |

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
  (Wanda's lifting, Tommy's acceleration, Loki's ice: **yours must too**: Spider-Man's webs, the Phoenix's fires). The teleport
  gate is one line in `HexManager.onTeleport` plus the Chorus Fruit and portal events. `FIELD` brings a stray fighter back (a
  damage that cannot kill, Slowness), `WALL` pushes back once a second and stops flight at `top`; a subclass overrides
  `rescueSpot`, `pushSpot` or `surfaceSpot` for its own ground (Ultron's island).
* **Hooks into the heroes** (done, do not repeat): `HexManager.judge` (`BossFights.isHexExempt`, set by `fight.setHexExempt`),
  `HexManager.onTeleport` (`allowsTeleport`), `SlowMotion` (`BossHooks.slowFactor`: 0 = not slowed), Magneto's strip
  (`BossHooks.onMagnetoStrip`), Wanda's telekinesis (`onTelekinesisPress/Launch`), `AreaTargets` (a boss's slam spares its own
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


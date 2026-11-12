# One jar for every NeoForge version above 1.21.1: the plan

This is a plan, not a description of something built. It says what "one jar for 1.21.1 and everything newer"
means for Enhanced, how such a jar can work at all, what it costs (measured, see the audit), and the order to do it in.
The same plan, with its own numbers, is in Enlightened's `docs/MULTIVERSION.md`.

## 1. The versions

NeoForge has a stable build for these Minecraft versions above 1.21.1 (checked on NeoForge's Maven on 2026-11-11).
Versions that only ever had beta builds (1.21.2, 1.21.6, 1.21.7, 1.21.9, 26.1 and 26.1.1) are left out: nobody plays
them, and each has a stable neighbour.

| Minecraft | NeoForge | Java | Audit: errors | Audit: files |
|---|---|---|---|---|
| 1.21.1 (today) | 21.1.x | 21 | 0 | 0 |
| 1.21.3 | 21.3.100 | 21 | 2,205 | 882 |
| 1.21.4 | 21.4.161 | 21 | 2,254 | 890 |
| 1.21.5 | 21.5.101 | 21 | 911 (cut short) | 546 |
| 1.21.8 | 21.8.57 | 21 | 1,221 (cut short) | 668 |
| 1.21.10 | 21.10.64 | 21 | 1,357 (cut short) | 738 |
| 1.21.11 | 21.11.48 | 21 | 2,130 (cut short) | 968 |
| 26.1.2 | 26.1.2.126 | 25 | not audited | |
| 26.2 | 26.2.0.93 | 25 | not audited | |
| 26.3 | 26.3.0.102 | 25 | not audited | |

**The audit.** The mod's main sources (1,575 Java files) were compiled, unchanged, against each NeoForge version,
and javac's errors counted. "Files" is how many source files have at least one error. The numbers are a lower bound:
javac reports the errors it can see, and fixing a missing class often uncovers uses of its changed methods. They
measure the distance from 1.21.1 to each version, not from one version to the next: most errors of 1.21.4 are the
errors of 1.21.3 again.

What the numbers say:

* **1.21.3 already breaks half the mod** (882 of 1,575 files), and 1.21.4 adds almost nothing to that. A few causes
  make up most of it: the player model lost its type parameter and its parts moved (the render-state change, about
  500 errors with the overrides that no longer match), `InteractionResultHolder` is gone (123), `MobSpawnType`,
  `DimensionTransition`, `ArmorMaterial`, `DirectionProperty` and `UseAnim` were renamed or replaced (about 40 each),
  the build height methods were renamed (87), and entity types are now created with a registry key (49).
  57 % of the errors are in the client code.
* **"Cut short"** from 1.21.5 on: so many classes the sources import are gone that javac stops before it checks the
  method bodies. The real count is higher than the one for 1.21.4, not lower; the file count is the better measure.
* **1.21.11**: one rename alone, `ResourceLocation` to `Identifier`, is 765 of the errors. That one is mechanical.
* **26.1.2 could not be audited** with today's build: NeoForge 26.x only offers itself to a Java 25 build, and the
  project is set up for Java 21. Setting that up is part of phase 0.

## 2. How one jar can serve many versions

A NeoForge mod jar is not tied to a Minecraft version by its format: `neoforge.mods.toml` is read the same way by
every version in the table, and at run time the game's classes have their real names on all of them. What ties a jar
to a version is its **code** (it calls methods that exist in one version only), its **mixins** (they name methods to
patch) and some **data files** (recipes, item models). So one jar has to carry one copy of the code per version and
make sure only the right copy is ever touched.

### The jar

```
enhanced-4.0.0.jar
  META-INF/neoforge.mods.toml         one mod "enhanced", minecraft range [1.21.1,)
  com/kolodkingal/enhanced/boot/      the entry: tiny, uses nothing that differs between versions
  com/kolodkingal/enhanced/v1_21_1/   the whole mod, built against 1.21.1
  com/kolodkingal/enhanced/v1_21_4/   the whole mod, built against 1.21.4
  ...
  enhanced.v1_21_1.mixins.json        one mixin config per version
  assets/, data/                      shared; version differences in pack overlays
```

* **The entry** (`boot.EnhancedBoot`, the only `@Mod` class) asks FML which Minecraft this is
  (`ModList.get().getModContainerById("minecraft")`, the same call on every version) and creates
  `com.kolodkingal.enhanced.<version>.Enhanced` by name, passing on the mod bus and the container. If there is no
  copy for the running version it stops with a clear message ("Enhanced 4.0.0 supports 1.21.1, 1.21.4, ...").
* **One copy of the mod per version**, each moved into its own package by the build (relocation, as the Shadow
  plugin does it). Java only loads a class when something uses it, so the copies for other versions are never
  loaded and their references to missing methods never matter.
* **Nothing in a copy may be found by scanning.** FML scans every class of the jar for `@Mod` and
  `@EventBusSubscriber` and loads what it finds. A copy for another version must not be loaded, so the copies carry
  neither annotation. Enhanced is nearly there since the loader-independent port: only the three
  `@EventBusSubscriber` classes in `platform/neoforge` are left, and they become plain registrations from the entry.
* **Mixins**: every version's config is listed in `neoforge.mods.toml`, and each names one small mixin plugin from
  `boot` whose `shouldApplyMixin` says yes only on its own version.
* **No access transformers** (they cannot be switched by version). The port already replaced all seven with accessor
  mixins.
* **Class file version.** The copies for 26.x need Java 25 to run but are compiled to Java 21 bytecode
  (`--release 21` cannot be used against Minecraft's own Java 25 classes, so this is done with `-source/-target`
  or, if that fails, by keeping the 26.x copies in a nested library jar that FML does not scan). This is the one part
  that has to be proven first: see the spike below.
* **Resources.** Textures, sounds, language files and most models are the same for every version and are stored
  once. Files whose format changed (recipes in 1.21.2, item definitions in 1.21.4, equipment models, data components
  in 1.21.5) go into **pack overlays**: folders that `pack.mcmeta` switches on by pack format, a vanilla feature made
  for exactly this.
* **Network.** The protocol version includes the Minecraft version, so a 1.21.4 client is refused by a 1.21.1
  server by vanilla anyway.

Jar size: the mod's classes are about 24 MB on disk per copy (2,875 class files), perhaps 7 to 9 MB each once
compressed; the 28 MB of assets are stored once. Ten copies therefore add something like 70 to 80 MB to the jar.
That is large for a mod, and a reason to consider shipping fewer versions in the jar (for example 1.21.1, 1.21.4,
1.21.8, 1.21.11 and the newest 26.x). Loading time grows by FML's scan of the unused copies.

### The fallback

If the spike shows that a single jar cannot be made reliable (FML refusing the jar on one version, or a scan
problem with newer class files), the same build still produces **one jar per version** from one source tree, which is
what almost every mod ships. Everything in sections 3 and 4 stays the same; only the last packaging step differs.

## 3. One source tree, many versions

Section 1's numbers rule out the simple idea of "common code plus a small per-version layer": at the first step
(1.21.3) half of the files already have an error. The differences are everywhere a vanilla class is touched.
So the source stays **one tree**, and the build turns it into each version's sources:

* **Stonecutter** (the Gradle plugin most multi-version mods use): version conditions written as comments,
  `//? if >=1.21.4 { ... //?} else { ... //?}`, with one version active in the IDE at a time. The build produces
  each version's sources, compiles them against that version's NeoForge, and relocates the result.
* **Seams first, comments last.** A condition in 900 files is unmaintainable. Most errors come from a few causes
  (see the audit notes: the same 15 causes make up most of the count). Each of those gets a seam in the existing
  `platform` layer, written once per version: for example `Compat.useResult(...)` for the `InteractionResultHolder`
  change, or one base class for player layers that hides the render-state change. The rule of the port
  ("loader API only in `platform/neoforge`") gets a twin: **version conditions only in `platform/compat`**, checked by
  the same lint.
* **Rendering is the exception.** The client's renderers, models and layers (about 300 files) changed shape three
  times: render states (1.21.2), render pipelines (1.21.5), and the submit-based renderer (1.21.9). They cannot be
  hidden behind a seam cheaply. The plan is one small drawing facade of the mod's own (`Draw.quad`, `Draw.model`,
  `Draw.beam`: what the effects actually use) that the effects are moved onto on 1.21.1 first, so that only the
  facade is rewritten per era.

## 4. The order

Each phase ends with the same gate: it compiles on every version done so far, the GameTests pass on each, and the
**screenshot tour** shows the same pictures as 1.21.1. (The tour is a scripted client run that becomes every hero,
presses every ability and saves a screenshot of each; a first version of it was written while looking for the
flashing bug of 3.9.0 and ran in a headless container. It should be committed as a development tool before
phase 1: it is the only practical way to check 30 heroes on 10 versions.)

| Phase | What | Done when |
|---|---|---|
| 0. Spike | A two-version jar of a toy mod with one mixin and one overlay, for 1.21.1 and 26.3, built the way section 2 says. | It loads on both, on a client and a dedicated server. If not: the fallback. |
| 1. The build | Stonecutter, the per-version projects, relocation, the entry, the mixin plugin, the lint. Still only 1.21.1. | The 1.21.1 copy inside the new jar passes every GameTest and the tour. This is a release on its own (4.0.0), with no new version yet. |
| 2. The seams | On 1.21.1 only: the compat seams for the top error causes, and the drawing facade under the effects. | No behaviour change; the audit numbers for 1.21.3 drop sharply. |
| 3. 1.21.3 and 1.21.4 | Render states, the new item definitions, recipes. The two are nearly the same. | Gate. |
| 4. 1.21.5 | Render pipelines (the custom render types and shaders), data component and NBT changes. | Gate. |
| 5. 1.21.8 | The HUD and screens (GUI rendering changed in 1.21.6), entity saving through the new value input and output. | Gate. |
| 6. 1.21.10 and 1.21.11 | The submit-based entity and block entity renderers; the 1.21.11 renames. | Gate. |
| 7. 26.1.2, 26.2, 26.3 | Java 25, the unobfuscated toolchain, whatever the audit shows by then. | Gate. |

Each of phases 3 to 7 can be released when it is done: the jar simply gains a version.

### Phase 0, first result (2026-11-11)

A toy mod built the way section 2 describes was tried: one jar holding a `boot` entry class and one `Entry` class per
version (`spike.v1_21_1.Entry` using `ResourceLocation`, `spike.v1_21_11.Entry` using its 1.21.11 name `Identifier`),
each compiled against its own NeoForge. **The same jar loaded on a dedicated server of NeoForge 21.1.265 (1.21.1) and
of 21.11.48 (1.21.11)**, and on each the entry of that version ran. So the core of the approach holds across the whole
1.21 range.

Still to prove before phase 1: a mixin config per version switched by a plugin, a pack overlay, a client (only
servers were started), and 26.x with its Java 25 classes. The two small projects are in `tools/multiversion-spike/`.

**Start with Enlightened.** It is a seventh of the size (234 files against 1,575), so it is the cheap place to find
out what phases 0 to 3 really cost before Enhanced commits to them.

## 5. What it costs, plainly

* Phases 0 to 2 are build and refactoring work with no new version at the end: a few weeks.
* Every later phase touches most of the client code. Even with seams, 1.21.5 and 1.21.10 are each closer to a port
  than to an update.
* From then on every feature is written once but **tested ten times**. CI time for the GameTests alone becomes
  ten times today's; the release workflow has to build ten copies.
* A new Minecraft version means a new copy in the jar and a new release of the same jar.

What is uncertain: the per-version notes in section 4 for 1.21.5 and later are from NeoForge's release notes and from
memory, not from doing the work, and 26.2 and 26.3 have not been audited at all. Phase 0 and the audit numbers are
the facts; the rest should be re-measured at the start of each phase.

## Progress (3.11.0): 1.21.1 and 1.21.4 in one jar

Phases 0 to 3 are done for 1.21.4 (1.21.3 is left out: nobody plays it, and 1.21.4 has the same changes), in the
same way as Enlightened 1.0.29.

* **The jar**: `boot.EnhancedBoot` is the only `@Mod`; `boot.Versions` lists the copies; `boot.MixinGate` lets a copy's
  mixins apply only on its version. `NeoForgeBridge` is installed by the entry instead of being found by scanning.
  The build makes each copy's mixin config from `enhanced.mixins.json` plus the version's own mixins
  (`versions/1.21.4/mixins.json`).
* **The sources**: one tree. `versions/1.21.4` makes the 1.21.4 copy with the `Preprocess` task (buildSrc): version
  blocks (`//#if >=1.21.4` ... `//#else` ... `//#endif`), whole-file replacements in `versions/1.21.4/src/main/java`
  and about 300 renames in `versions/1.21.4/renames.txt` (most of the differences are mechanical: new parameters, a
  render state instead of the entity, the teleport and item-result types). The `@Subscribe` index is generated for the
  copy too (the processor takes the copy's package, `-Aenhanced.basePackage`).
* **1.21.4's own code** (`versions/1.21.4/src/main/java`): the render-state bridge and the `Legacy*` model and renderer
  bases, so the heroes' renderers, models and layers draw unchanged from a render state; `compat.Ids` (an item's and a
  block's id is given to its properties while it is registered, and the mod's own durability and attributes stay);
  the armour materials as 1.21.4 values with equipment assets; `compat.AttachSync` (NeoForge 21.4 has no attachment
  sync); `compat.LegacyItems` (item properties, item colours and the items drawn by code, behind 1.21.4's item
  models); the stone extraction recipe; the client mixins whose targets changed.
* **Resources**: pack overlays made by `tools/overlays.py`: recipes, biomes and NeoForge's load conditions in 1.21.4's
  format; an item model definition for every item (the overrides as range dispatches, the separate GUI models as a
  display-context select, the spawn eggs' and the Stone Locator's tints, the Vibranium Shield as a special model); the
  models that used NeoForge 21.1's `item_layers` and `separate_transforms` loaders; the slow-motion post effects.
* **Checked**: both copies compile; the jar starts a dedicated server on 1.21.1 and on 1.21.4 (with Enlightened beside
  it); the GameTests pass on 1.21.1. The GameTests are not built for 1.21.4 yet.
* **Known differences on 1.21.4**: items' emissive layers (the Infinity Stones' glow in the inventory) are drawn
  plainly (21.4 has no per-layer light for generated items); Hela's fading uses the model's full alpha; Wanda's crown is
  drawn on the Scarlet Witch's suit too.

## Progress (3.13.0): 1.21.11 in the same jar

Phases 4 to 6 are done for 1.21.11, the newest 1.21 version (1.21.5, 1.21.8 and 1.21.10 are not shipped: nobody
needs them next to 1.21.11). The jar now holds three copies: 1.21.1 (as written), 1.21.4 and 1.21.11.

* **How the copy is made.** `versions/1.21.11` is built from 1.21.4's rules and files, then 1.21.5's (a step that is
  not shipped on its own: `-Pwip=1.21.5`), then its own: 244 renames in `versions/1.21.11/renames.txt` and 67 whole
  files in `versions/1.21.11/src/main/java`. A file that starts with `//#norenames` is only relocated (the renames
  are for the shared sources, not for the copy's own code). The javac errors went from about 1,240 (the copy as 1.21.4
  and 1.21.5 left it) to 0.
* **Drawing (1.21.9's submit renderer).** Entity, layer and block entity renderers no longer draw: they hand
  geometry to a `SubmitNodeCollector`. `compat.SubmitBuffers` is a `MultiBufferSource` that records the vertices the
  mod's renderers write, per render type, and hands them over with `submitCustomGeometry`; so the heroes' models,
  layers, beams and effects still draw with 1.21.1's code. The `Legacy*` renderer, layer and model bases are rewritten
  on top of it; `compat.ItemDraw` draws items, `compat.RenderBridge` entities. Render types are made with
  `RenderSetup` (`EnhancedRenderTypes`, `SandRenderTypes`). The few places that drew with GL state (the skies, Build
  Magic's ghost, Sokovia) go through `compat.Gl`, which makes a render pipeline per state.
* **The GUI (1.21.6).** Screens and HUD layers draw in 2D (`compat.GuiPose`, `GuiStack`, `GuiTint`, `GuiQuads` for
  the mod's own geometry, `GuiEntity` for the models in screens), HUD layers are NeoForge `GuiLayer`s
  (`compat.LayeredDraw`), key input comes as events, and text colours without alpha are drawn opaque as on 1.21.1
  (`compat.GuiText`).
* **Saving (1.21.6).** Entities and block entities save through `ValueInput`/`ValueOutput`; `compat.ValueBridge`
  hands the mod's code the compound tag it used before, so saved worlds keep their shape. Attachments keep their
  compound shape too (`NeoAttach`), and saved data uses `compat.SavedBridge`.
* **Other changes:** permissions (`compat.Perms`), game profiles (`compat.Profiles`), game rules, the time of day,
  fog (`ViewportEvent` and NeoForge's fog events), the dimension effects (the dimension type's attributes and
  NeoForge's custom sky, cloud and weather renderers: `compat.DimensionSpecialEffects`), particles (single-quad
  particles), key categories, item capabilities (`Capabilities.Item`), FML 10, the abstract `RenderLevelStageEvent`
  (the bridge listens to its sub-events), the entity packages that moved.
* **Resources:** `tools/overlays.py` makes the 1.21.11 overlays (data format 94, assets 75, with `min_format` and
  `max_format`): dimension types with environment attributes, skyboxes and timelines (a fixed-time timeline for the
  mind realm), biomes with attributes, the post effects, `minecraft:chain` as `minecraft:iron_chain`, dyed colours as
  plain numbers, and loot conditions that 1.21.11 checks differently.
* **Checked:** the three copies compile; `./gradlew build` builds the jar; the jar starts a 1.21.11 dedicated server
  (NeoForge 21.11.48), which stops and starts again on the same world; a 1.21.11 client joins a world, walks the mind
  tour and the port tour (`-Denhanced.porttour=true`, a development tool in the 1.21.11 copy: every hero, creature,
  block entity, screen and realm, with a screenshot each) without an error; the GameTests pass on 1.21.1.
* **Known differences on 1.21.11** (each is a small, commented fallback in the 1.21.11 code):
  * A downloaded skin keeps its own colours in the Frost Giant form (1.21.11 gives no way to read a skin back from the
    graphics card; the default skins are recoloured as before).
  * The Witches' Road door block has no tooltip line (1.21.11 dropped the block's tooltip method).
  * Thor's grip on Mjolnir ignores the left-hand flag; the same hand is drawn for both.
  * The Eternity realm's fog is always the terrain fog (1.21.11 has no separate sky fog to skip).
  * The custom skies ignore the projection matrix they were given (1.21.11 uses the frame's).
  * Sokovia's lifted ground draws its translucent blocks with the cutout ones.
  * The wolf's howl is the wolf's growl (1.21.11 has wolf sound variants, not one howl).
  * Whether a mob of the mod goes away in peaceful is decided by a mixin calling the mob's own 1.21.1 answer
    (1.21.11 decides it by the entity type, which the mod's types do not set).
  * The mod's geometry of one render type is merged per entity (drawn in one go, not in the order it was written).
  * The glowing outline of the mod's effects uses the outline render types.

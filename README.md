# Enhanced

A Minecraft mod where you can become one of the *Enhanced*:
**Magneto**, the Master of Magnetism, or **Wanda Maximoff, the Scarlet Witch**.
And if you're lucky enough to survive Wanda's Hex wall, **Photon**.

Original hero concepts by Eilon Kolodkin-Gal. The full design, with every rule and
number, is in [docs/DESIGN.md](docs/DESIGN.md) (Hebrew: [docs/DESIGN.he.md](docs/DESIGN.he.md)).

| Magneto | Scarlet Witch | The Hex |
|---|---|---|
| ![Magneto](docs/screenshots/magneto.png) | ![Scarlet Witch](docs/screenshots/scarlet-witch.png) | ![The Hex](docs/screenshots/hex.png) |

* **Minecraft** 1.21.1
* **Mod loader** NeoForge 21.1.x
* **Install it on** the client, plus the server for multiplayer

## Becoming a hero

| Hero | How |
|---|---|
| **Magneto** | Craft **Magneto's Helmet** from 5 Netherite Blocks (helmet shape) and put it on. |
| **Scarlet Witch** | Find the very rare **Scarlet Witch Gene** (mansions, strongholds, fortresses, bastions, ancient and End cities, witches, evokers). Hold it in one hand and use **Loki's Scepter** (End City treasure and other rare chests) with the other to touch the Mind Stone. **Without the gene, you explode.** |
| **Photon** | Cross a Scarlet Witch's Hex wall without her permission and survive the 5% chance. |

## Controls

| Key | Magneto | Scarlet Witch | Photon |
|---|---|---|---|
| **R** | Metal Telekinesis | Chaos Energy Beam (hold) | Photon Blast |
| **G** | Iron Shrapnel Storm (hold) | Enhanced Flight | Photon Flight |
| **V** | Armor & Weapon Strip | Telekinesis (creatures, items, blocks) | Energy Release |
| **B** | Magnetic Shield (toggle) | Chaos Shield (hold) | Phase Shift |
| **N** | Magnetic Flight | The Hex (again: Hex Control, Sneak: dissolve) | Spectrum Vision (toggle) |

All heroes can also fly by double-tapping **Space**. With telekinesis, press the key again
to launch what you hold, or **Sneak** + the key to put it down gently. Keys can be changed
under *Options → Controls → Enhanced Powers*. The top-left corner of the screen always
shows your abilities, cooldowns and resources.

## Screenshots

| | |
|---|---|
| ![Lifting an Iron Golem](docs/screenshots/magneto-telekinesis.png) Magneto lifting an Iron Golem | ![Disarm](docs/screenshots/magneto-disarm.png) Ripping a zombie's iron gear off |
| ![Chaos Beam](docs/screenshots/chaos-beam.png) The Chaos Beam | ![Chaos Shield](docs/screenshots/chaos-shield.png) The Chaos Shield |
| ![Awakening](docs/screenshots/mind-stone-awakening.png) Touching the Mind Stone with the gene | ![Hex Control](docs/screenshots/hex-control.png) Wanda's Hex Control screen |
| ![Flight](docs/screenshots/scarlet-witch-flight.png) Red energy around Wanda's hands in flight | ![Photon](docs/screenshots/photon.png) Photon phasing, with Spectrum Vision on |

## Building

You need Java 21.

```sh
./gradlew build              # the mod jar ends up in build/libs/
./gradlew runClient          # play in a development client
./gradlew runGameTestServer  # run the automated in-game tests
```

To install, copy `build/libs/enhanced-<version>.jar` into the `mods` folder of a NeoForge
1.21.1 instance.

## For server owners

* `/enhanced power get|set …` and `/enhanced hex list|clear` (operators only).
* Balance numbers (beam damage, Hex death chance and radius, flight speeds, shrapnel…)
  are in `world/serverconfig/enhanced-server.toml`.
* Item, block, entity and damage tags under `data/enhanced/tags/` decide what counts as
  metal, what can be lifted, what the shield repels, which bosses resist the Hex and
  which damage Photon absorbs, so datapacks can change them.

# The one-jar spike (phase 0 of docs/MULTIVERSION.md)

Two tiny NeoForge projects whose classes were merged into one jar that loaded on both Minecraft 1.21.1 and 1.21.11.

* `p1211` (NeoForge 21.1.265): `spike.boot.SpikeBoot`, the only `@Mod` class, and `spike.v1_21_1.Entry`.
* `p12111` (NeoForge 21.11.48): `spike.v1_21_11.Entry` (its own mod id, `spikepart`, only so that it builds).

To repeat it: copy the Gradle wrapper into each folder, run `./gradlew jar` in both, then put the
`spike/v1_21_11/` classes of p12111's jar into p1211's jar. Put that jar into the `mods` folder of a server of either
version: the log says `SPIKE boot on <version> -> spike.<version>.Entry` and `SPIKE entry <version> ok spike:a`.

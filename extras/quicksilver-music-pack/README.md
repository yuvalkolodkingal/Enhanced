# Quicksilver music pack

Quicksilver's Walkman plays a song while he runs in Quicksilver Time. The mod comes with an
original placeholder tune. To hear any song you own instead (for example "Sweet Dreams (Are
Made of This)", like in *X-Men: Apocalypse*):

1. Convert your song to **Ogg Vorbis** (`.ogg`). Audacity can do this (File → Export → Ogg),
   or run `ffmpeg -i song.mp3 -c:a libvorbis -q:a 5 theme.ogg`.
2. Name the file `theme.ogg` and put it in
   `assets/enhanced/sounds/quicksilver/` inside this folder.
3. Copy this whole `quicksilver-music-pack` folder into your Minecraft `resourcepacks`
   folder, then turn it on under *Options → Resource Packs*.

The song loops while Quicksilver runs and fades out when he stops. Everyone who has the pack
and is near him hears it. The volume is set by the "Jukebox/Note Blocks" slider.

The song isn't included here because it's copyrighted. Use your own copy.

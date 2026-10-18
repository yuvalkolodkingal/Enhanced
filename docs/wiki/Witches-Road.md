# The Witches' Road

In *Agatha All Along* the Witches' Road is a place of trials that only a witch's child can walk. In the mod it is a **dimension of its own** (`enhanced:witches_road`) with **three trials**: a **Parkour** over the void, a structure you must **copy** block by block, and a long **ice-boat race**. Win one and the next opens. At the end you choose to become **Billy** or **Tommy**.

Hebrew: [עברית](Witches-Road-HE)

## The quick version

* **To start** you need a **Scarlet Witch Gene** and a [Witches' Road Door](Items-and-Crafting#witches-road-door) (it is on [Billy and the Witches' Road](Billy-and-the-Witches-Road#seeking-the-road-becoming-billy-or-tommy)). You must have **no power** and be in **Survival** mode. Right-click the door with the Gene in your hand, and press **Walk the Road**.
* **Three trials, one after the other:** 1. The Parkour, 2. Copy the Structure, 3. The Ice-Boat Race. Beating a trial opens a gate to the next. Every trial is built for you alone, in a place of your own: nobody else can see you or get in your way.
* **Nothing can hurt you.** Falling, the void and everything else just put you back at your last checkpoint. You cannot die on the Road.
* **You can stop whenever you like.** Leave through the exit door on the start platform (right-click it twice) or type `/road leave`. You keep your Gene, and next time you start the trial you had reached.
* **The Gene is used up only at the end**, when you choose Billy or Tommy.
* **The same course every time.** Your courses are made from your own player ID, so you can learn them. Another player gets other ones.

## Walking the Road

1. A player **with no power**, in **Survival** mode, right-clicks a Witches' Road door **holding a Scarlet Witch Gene**. A ring of blue-green light runs round the door and the doors open.
2. The Road asks **"Seekest thou the Road?"** and the screen shows one big button, **Walk the Road**, and "Trial 1 of 3" (the number of the trial you have reached).
3. In a moment the Road is built for you (a few ticks: the door pours light while it works), and you are taken there in a column of light. The place where you stood, and your game mode, are remembered.
4. You play the trial. When you win it, a gate on the finish platform opens ("The Parkour: done!") and the next trial is built while you stand there. Step through the gate to go on.
5. After the third trial comes [the end](#the-end-of-the-road).

Each trial starts with its name and, underneath, what to do: *The Parkour: Cross the void to the gate*, *Copy the Structure: Build a copy of the model* (its title is shortened to "2. The Copy" so that it fits the window), *Ice-Boat Race: Sail the ice to the finish*. The **Voice of the Road** speaks in the action bar, a few friendly lines for each moment ("The Road remembers you." "Not yet. Up you go." "Well walked. Step through the gate."), and when you win the whole server is told in chat.

### What the Road looks like

A dark, quiet place with a blue-green fog and no sky, like a Nether with no walls: dark oak and amethyst, soul lanterns, roots hanging down, glow lichen and warped spores floating by. There are no monsters. The start platform of each trial has the **exit door** at its edge.

### The rules on the Road

Only people with no power walk the Road, so the rules are few:

* You are in **Survival**, and you **cannot fly or glide** (no elytra, Levitation, Slow Falling or Jump Boost). Ender Pearls, Chorus Fruit, Wind Charges and firework rockets do nothing, and no portal or teleport takes you off the Road.
* **Nothing hurts you.** Anything that would kill you (a fall into the void, `/kill`) puts you back at the last checkpoint instead. Your food bar stays full.
* **You cannot break or place blocks**, except on the plot of the second trial.
* **You cannot drop the Gene**: if you throw it (the drop key, or dragging it out of your inventory) it jumps straight back, so you will always have it at the end.
* **You cannot travel into the Road from outside**, only walk in through the door. Boats, minecarts, armor stands, paintings, item frames and spawn eggs cannot be used on the Road.
* Powers do not work. If an operator gives you one, the Road sends you back to the world.
* **Everyone has a place of their own.** Friends cannot join your trial (the Road is solo, on purpose, to keep it simple and safe), and at most 32 players walk the Road at once (`witches_road.maxPlayers`); if it is full, wait for somebody to finish.
* Creative and Spectator players are refused at the door ("The Road tests only those who can fall").

## The Road's music

In 3.3 the Road has a song, a **witchy ballad of its own**, in a different arrangement at every stage, as the show's ballad has many versions; 3.5 adds a sixth, for the moment you choose. (It is an original tune in the spirit of *Agatha All Along*, not the show's song: its first notes are the coven's tune that the Road's door has always played.)

| Stage | What you hear |
|---|---|
| **The entrance** (from the moment you ask a door, while it opens and you are taken in) | The coven hums it over a low drone, with bells and a heartbeat drum. |
| **The Parkour** | Lute arpeggios and a harpsichord, with a bass, a frame drum and a shaker. |
| **Copy the Structure** | A music box over a choir; the second time round the choir sings the tune. |
| **The Ice-Boat Race** | Fast drums, driving strings and violins, a break of low strings and toms, and horns. |
| **The choice** (3.5: in the hall at the end, while the Road waits for you to choose Billy or Tommy) | A solo cello over a low humming choir and a slow tick; then the harp turns the tune to E major. |
| **The end of the Road** (in the hall, while the light gathers, and in the farewell) | Everything at once: choir, strings, horns, harp and timpani, and the second half in a major key, triumphant. |

* It plays on the **Music** volume slider (at 0 it is silent), fades in over 2 seconds, crossfades to the next stage, fades out over 3 seconds when you leave the Road, and holds the game's own music back while it plays. Each arrangement loops smoothly.
* **A server's own music (3.5).** A server can replace the Road's music for every player with no resource pack: put `witches_road.ogg` (every part) or `witches_road_<part>.ogg` (`entrance`, `parkour`, `structure_copy`, `ice_boat`, `choice`, `ending`) in `<world>/serverconfig/enhanced/music/`, and the players download it by themselves. A player can also put his own `witches_road.mp3` (or one part's) in `config/enhanced/music/`. See [Music](Music).
* **Replacing it with a resource pack.** A resource pack (or a server resource pack) can swap any of the six for music of your own: in its `assets/enhanced/sounds.json`, define the sound event with `"replace": true` and your own `.ogg` files, and keep `"stream": true` (the game loops them). The events are:

| Stage | Sound event | File in the mod |
|---|---|---|
| The entrance | `enhanced:music.road.entrance` | `assets/enhanced/sounds/music/road_entrance.ogg` (32 s) |
| The Parkour | `enhanced:music.road.parkour` | `music/road_parkour.ogg` (48 s) |
| Copy the Structure | `enhanced:music.road.copy` | `music/road_copy.ogg` (58 s) |
| The Ice-Boat Race | `enhanced:music.road.boat` | `music/road_boat.ogg` (48 s) |
| The choice (3.5) | `enhanced:music.road.choice` | `music/road_choice.ogg` (32 s) |
| The end of the Road | `enhanced:music.road.ending` | `music/road_ending.ogg` (51 s) |

```json
{
  "music.road.parkour": {
    "replace": true,
    "sounds": [{"name": "mypack:my_parkour_song", "stream": true}]
  }
}
```

(That file goes in `assets/enhanced/sounds.json` of your pack, and the song in `assets/mypack/sounds/my_parkour_song.ogg`.) A trial added later without its own arrangement plays the Parkour's.

## Trial 1: The Parkour

*Cross the void to the gate.* You start on a platform with the exit door and have to jump from stone to stone, over nothing, to the finish platform with the gate. Every sixth jump there is a **checkpoint**: a platform of lit amethyst, where you are healed and put back if you fall.

* The stones are dark oak in the fog, and the course winds outward from the start in a spiral, so it never runs into itself. By default there are **40 jumps**.
* It **gets harder as it goes**: the stones shrink and the gaps grow. No jump needs more than a running jump (at most 3 blocks of air, and a step up only comes with a gap of 2 or less); the 3-block gaps and the steps up and down come later in the course.
* **Falling** (12 blocks below the stones) puts you at the last checkpoint, with no damage and no loss.
* A clock can be set by the server (`parkourTimeLimitSeconds`); by default there is **no time limit**.

*Tips:* sprint and jump from the very edge of the stone, look at the next stone while you jump, and use the checkpoints to breathe.

## Trial 2: Copy the Structure

*Build a copy of the model.* You step into a big platform. On the left stands the **Model**, a finished little building on a floor with an amethyst outline; on the right is your empty **Plot**, the same size and facing the same way, with a sea-lantern outline. In front of them is the **console**: a **Check** button (warped), a **Hint** button (cherry) and the **supply chest** between them. The floating text says "The Model" and "Your Plot".

* **Your blocks.** The blocks of the model are already in your hotbar and backpack (150 percent of what the model needs, at least 16 of each kind), and the supply chest next to the console refills itself every second, so you can never run out. Your own backpack is put aside for the trial and given back afterwards, even if the game crashes. The blocks of the trial cannot leave the Road.
* **Build by hand** on the plot: place the blocks the way the model has them. Breaking a block on the plot is instant and gives it back. You cannot build or break anything else on the platform.
* **What "exactly" means.** Every block must be the **same block**, facing the same way, with the same half (stairs), slab type (bottom, top or double), axis (logs and pillars) and hanging lanterns. What the game works out by itself does not count: the shape of stair corners and the joints of fences, walls and panes, water and power.
* **Check** (right-click the warped button). It counts the blocks that are right ("Correct 41/48" over the plot and in the action bar) and tells you how many are **missing**, **wrong** or **extra**, with glowing outlines for 8 seconds, only for you: blue-green for a missing block, red for a wrong one, orange for an extra one (the lowest 40 at most). It also says a friendly word: "Not yet. The marks will guide you." "So close! Only a few blocks more."
* **Hint** (right-click the cherry button). Free: ghost blocks, glowing in the Road's teal, show the real blocks that belong in the missing cells (up to 40 for 8 seconds, the lowest first).
* **You win** when the plot looks exactly like the model: **Perfect! It looks exactly like the model.** The Road notices a perfect copy by itself, once a second, so you need not press Check (a server can switch that off).
* **No failing.** There is no health or timer. If you fall off the platform you are put back at the start with your build intact. There is only a soft limit of 90 minutes a visit (`structureCopyTimeLimitSeconds`), so nobody leaves a room standing for ever: then you are sent home, with your Gene and your things, and the next time you get a new room with the same model.

There are **five kinds of building**: a **hut** (a gabled cottage), a **tower** (with a roof walk and a flag), a **bridge** (a humpback arch over a tunnel), a **temple** (rings of colour, pillars, an altar) and a **witch's cauldron altar** (a cauldron over magma in a circle of stone). By default they fit in 9 x 9 x 7 blocks (about 160 blocks); a server can make them smaller (5 x 5 x 5) or bigger (11 x 11 x 9). Which one you get depends on your player ID, and it is always the same for you.

*Tips:* build the floor first, then the walls, then the roof; use Check often; stairs and slabs must face the right way; and look at the model from above, from the side and from the inside.

## Trial 3: The Ice-Boat Race

*Sail the ice to the finish.* A **huge course of packed ice** winds through the fog between walls, with sea lanterns in the walls and amethyst arches over the checkpoints. It is **800 blocks long** by default and 9 blocks wide. You start on the Road's usual platform, walk to the **boat** at the start line (it takes you in when you come near) and wait for the **countdown**: 3, 2, 1, **GO!**, while a purple glass barrier holds you back.

* **Steering:** **W** goes, **A** and **D** steer, **S** goes back, **Shift** climbs out of the boat.
* **The course:** straights, 90 degree curves, S-bends, hairpins (with a strip of rough snow in front of them to slow you), narrow stretches (5 wide), snow drifts, chicanes of snow walls, and stripes of fast blue ice. Walls do not hurt: you slide along them and keep your speed along the wall.
* **Checkpoints** (amethyst arches) about every **100 blocks**. If you **fall off** the ice or climb out, you come back in a **new boat on the last checkpoint**. If you are stuck, press **Shift**: it puts you back at the checkpoint (at most once every 5 seconds; an earlier Shift only keeps you in the boat). A hint appears after 4 seconds without moving.
* **Shortcuts.** Some loops have a **shortcut**: a straight across the loop with a 5-block gap in it, and the far side one block lower. A fast boat jumps it; a slow one falls and goes back to the checkpoint. The long way round is always open.
* **The clock.** You have **4 minutes** from the start of the trial (a bar at the top shows the time left, from blue to yellow to red), and a **par time**: how fast a perfect driver could go, times 1.4. On a long course the limit is stretched so that it is never shorter than 3 times the par time (`boatRaceFairFactor`), so a driver who is not the fastest can still finish. At the finish you are told your time against par ("Faster than par! Well sailed.") and your best time. If the time runs out you go back to the start platform and try again.
* **Winning:** cross the **finish line** (a checkered stripe and an arch). The boat is taken away, you land on the finish platform, and the gate opens.
* A speed (blocks a second) shows in the action bar, and frost and aurora lights play in the sky.

*Tips about the boat on ice:* it slides like a puck and its top speed on packed ice is about 40 blocks a second. Steering turns the boat, not the way it moves, so start your turn early and let go of **W** for a sharp curve. A boat does not climb anything. Rough snow slows it a lot (and is a way to brake before a hairpin). Keep to the middle, and do not be afraid of the checkpoints: they are free.

## The end of the Road

After the third gate you step into the **end hall**: a round hall of dark oak and amethyst, with a dais in the middle, in a place of its own. The title says "The Road's End", the Road speaks, the light gathers over the dais for three seconds, and then the Road asks once more, "Seekest thou the Road?" and the choice screen opens. It shows the same two roads as the door gave in 3.1:

* **Billy: Wiccan**: blue magic, flight, telekinesis, the Hex Beam and Hex Barrier, and Build Magic that grows with practice. See [Billy](Billy-and-the-Witches-Road).
* **Tommy: Speed**: super speed, like his uncle. See [Tommy](Tommy).

It says "One Gene will be used up." and has two buttons, **Not yet** and **Leave the Road**. You need a Gene anywhere in your inventory: without one the Road says so and does not ask.

**When you choose,** one Gene is used up (only now) and you awaken with the power you picked, with the title, the sound and the message to the whole server. For a few seconds (`endingFarewellSeconds`, 5 by default, 0 to 30; 0 sends you at once) you are held where you stand in a rising helix of light, with amethyst chimes and a last line from the Road. Then the Road **takes you out**, to the first of these places that is safe: the place where you came in (or up to 6 blocks above it), the Witches' Road door you opened, or the world's spawn on the surface. A place is unsafe when a block is in it now, there is no floor within 24 blocks below, there is lava, fire, a cactus, magma or a campfire, it is outside the world border, or the world is not there. You arrive in your own game mode, with your things, your experience, full health and hunger and every effect cleared, and a chat line tells you what you have become. The Road forgets your progress, and while you have a power you cannot walk it again.

* **Not yet** closes the screen and changes nothing. Right-click the door lying in the floor of the hall, or type `/road choose`, and the Road asks again.
* **Leave the Road** (or `/road leave`, or the exit door) takes you back to where you came in **with the Gene**. The next Witches' Road door you open offers the choice at once, because you have walked all of it.
* **Nothing can kill you in the hall.** A fall puts you back at the start of the hall, and the question stands. In the farewell a death is turned away and you still leave.
* **If you log out in the hall,** it is built again at your next login and the question comes back. If you log out in the farewell, the power is already yours: at your next login the Road sends you home.
* If another mod refuses the way out, you stay in the hall and the Road tries again every moment.
* Operators: `/enhanced road enter <players> 4` (one more than the number of trials) sends a player to the hall; he must have won every trial (`/enhanced road set <players> 99`) or the choice is refused. `skip` has nothing to win there, and `set` to fewer trials sends him home from the hall.

A server that switches the trials off (`witches_road.enabled` is `false`) gets the 3.1 door: right-click it with a Gene and choose at once.

## Leaving, coming back, and what is saved

* **Leaving.** Right-click the **exit door** on the start platform of a trial; it asks "Right-click again to leave the Road. Your Gene stays with you." Right-click it again within 5 seconds. Or type **`/road leave`** anywhere on the Road. You come back to the place where you came in. **`/road status`** says how far you are.
* **Your progress is saved** with you (and survives logging out, a crash and a restart): how many trials you have won, which checkpoint you reached, how many times you fell, and your best boat time. A trial you leave is rebuilt for you at your last checkpoint (the structure trial starts again with a fresh room and the same model: your build is not kept).
* **Logging out on the Road.** If you log out in a trial, the Road is taken down. When you come back, you are put at your last checkpoint (`resumeOnLogin`), or, if a server turns that off, back where you entered.
* **If the server stops** while you are on the Road, the places it built are cleared away the next time it starts.
* **Where friends see you.** In the world they see the ring of light on the door, you leaving in a column of light, and you coming back the same way. On the Road nobody sees anybody. When someone wins a trial the server is told in chat ("Eilon won a trial of the Witches' Road: The Parkour").

## Commands

| Command | Who | What it does |
|---|---|---|
| `/road leave` | everyone | Leaves the Road, keeping the Gene and the progress. |
| `/road status` | everyone | How many trials you have won, and where you are. |
| `/road choose` | everyone | At the end of the Road: asks the question again (the choice screen opens). |
| `/enhanced road status [player]` | operators | The same for any player: trials won, in the Road or not, how many falls. |
| `/enhanced road enter <players> [trial]` | operators | Sends players onto the Road (no Gene needed); `trial` is 1, 2 or 3 (4 is the end hall). |
| `/enhanced road leave <players>` | operators | Takes players off the Road. |
| `/enhanced road reset <players> [newcourse]` | operators | Forgets their progress; with `newcourse` they also get new courses. |
| `/enhanced road skip <players>` | operators | Wins the trial a player is in, or counts the next as won. |
| `/enhanced road set <players> <won>` | operators | Sets how many trials a player has won. |
| `/enhanced road layout [player]` | operators | Where the checkpoints, the finish and the gate of a player's trial are. |
| `/enhanced road cells` | operators | How many places are in use or being cleared, and how many players are on the Road. |

Operators are free of the Road's rules (they can place blocks and teleport) unless they are in a trial, where the trial's own rules apply; an operator who teleports out of a trial ends it.

## For servers

All the numbers are in the `[witches_road]` section of the server config (see [Server Configuration](Server-Configuration#the-witches-road)): whether the trials exist at all (`enabled`; off gives the 3.1 door), how many players may walk at once, the seed of the courses, whether players resume at their checkpoint, the chat announcement, the Voice, how a fall is measured, the Parkour's length, difficulty, checkpoints and time limit, the structure's size, supply, hints and time limit, and the boat race's length, width, difficulty, shortcuts, snow, blue ice, checkpoints, time limit, par factor and countdown. Everything is read live. The music of the Road has no server option: a resource pack replaces it ([above](#the-roads-music)).

Each trial is **built by code** for the player who walks it, in a private place of the dimension 2,048 blocks from the next, and **taken down again** when he leaves, so the world's save does not grow and nothing is left behind. A crash leaves nothing unaccounted: the places that were open are cleared at the next start.

## Questions

**I fell. Did I lose?** No. Nothing on the Road can hurt you: you just go back to the last checkpoint.

**Can my friends help me?** Not on the Road: it is one player at a time. They can cheer from the world.

**Can I use my powers on the Road?** No: only people with no power can enter, and a power given on the Road sends you out.

**I left. Do I start again?** No: you start the trial you reached, at your last checkpoint (except the structure trial, which builds a fresh room).

**The boat will not move!** Press and hold **W**. If it is still stuck, press **Shift** to go back to the last checkpoint. If you fall, the Road gives you a new boat.

## Related

[Billy and the Witches' Road](Billy-and-the-Witches-Road) · [Tommy](Tommy) · [Scarlet Witch](Scarlet-Witch) · [Items and Crafting](Items-and-Crafting) · [Commands](Commands) · [Server Configuration](Server-Configuration) · [Controls](Controls)

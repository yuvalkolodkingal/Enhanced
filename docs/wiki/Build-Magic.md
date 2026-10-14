# Build Magic

Wanda and Billy can **build with their magic**, the way builders do with Axiom or WorldEdit, but with no commands to type: you click, you see what will happen, you click again, and it happens. Everything is seen before it is done, and **Ctrl + Z takes it back**, even tomorrow.

* **Wanda's magic has no limit.** She can fill a valley, grow a mountain, copy a castle. Only the server's safety keeps her from freezing the game (see [her part](#wandas-magic-has-no-limit)).
* **Billy's magic is an apprentice's, and it grows.** Every block he builds is practice. He starts small (a house at a time) and learns his way up to **Wiccan**, four times as strong. Near his mother his spells cost half. See [Billy](#billy-an-apprentice-who-learns).

The two heroes have the same **eleven spells** and the same keys. What differs is *how big*.

## Press Y

**Y** is the Build Magic key. It is the sixth line of your hero panel in the top-left corner: **`[Y] Build Magic  OFF`**.

* **Tap Y** (let go at once): build mode on, or off. When it is on, your crown glows (Billy) or your eyes glow (Wanda), sparks gather at your fingers, and a strip of little mouse pictures appears at the bottom right.
* **Hold Y** (a third of a second): the **Spell Wheel**, a ring of eleven big pictures (see [below](#the-spell-wheel)). Out of build mode, holding Y turns it on first.

Only the Scarlet Witch and Billy have it. For every other hero Y does nothing.

Build mode turns off by itself when you die or respawn, change your power, go to Spectator or Adventure mode, or log out. A change of dimension keeps it on (but you let go of your box). Everything else you can do (flying with **G**, fighting with **R** and **B**) works in build mode too.

Xaero's Minimap also uses **Y** by default. Rebind one of the two in *Options, Controls, Key Binds* (Build Magic is in *Enhanced Powers*).

## The Spell Wheel

Hold **Y** and a ring of eleven pictures opens round your crosshair, one for each spell, clockwise from the top: Fill, Vanish, Change, Paint, Pull & Push, Shapes, Copy, Move, Repeat, Land and Grow. Point at one and its name and a line about it appear in the middle. The game does not pause, so everyone can still see you.

* **Choose with the mouse.** Move onto a picture and **let go of Y**: that spell is chosen and the wheel closes. Let go in the middle and the wheel stays open: click a picture to choose it. A click on another spell chooses it and the wheel stays open, so you can pick its style next; a click on the chosen spell closes the wheel.
* **Choose with the keys.** **1** to **9** choose the first nine spells and **0** the tenth (Land), and the wheel closes. Grow, the eleventh, has no number: use the mouse. The **mouse wheel** steps from spell to spell (down is clockwise).
* **Esc** closes the wheel and changes nothing. (**Y** closes it too, but in this version that tap of Y also switches build mode off, so press it once more.)
* **The right side** shows the styles of the chosen spell (Solid, Walls, Room... for Fill; Ball, Dome... for Shapes, with a Solid or Hollow switch). Under the ring are the **dice** (Mix), your nine hotbar slots, and the **Blocks** button.
* **The left side** has **Undo** and **Redo** with how many steps each holds (grey at 0). The **door** in the middle starts or stops building.
* **Over the ring** Wanda's says "Chaos Magic" and "∞"; Billy's shows his name, his stars and rank, his meter and how many blocks of magic he still needs for the next rank.
* **Blocks** opens a page of big block pictures from the game's four creative tabs (Building, Coloured, Natural and Functional), with a row of the last 16 you used and a search box. Click one and your magic makes that block, even if you do not carry it (in Survival with `requireMaterials` on, only what your inventory holds is shown). It lasts until you change the hotbar slot or what is in your hand.

## The mouse: one grammar for every spell

Each button means one thing in every spell, and **the little strip at the bottom right always tells you what the buttons do right now**. No Sneak is ever needed.

| You do | It means | Where it is a little different |
|---|---|---|
| **Left click** a block | **Where**: the first corner, then the second corner. A third click starts a new box. | Shapes: plant the shape here. Paint, Pull & Push, Land and Grow: nothing. A left click never breaks a block in build mode. |
| **Left press, look, let go** | Drag a box from where you pressed to where you let go. | |
| **Double left click** | **Magic select**: the whole thing you clicked (a tree, a house). Double click another thing to add it. | |
| **Left click the sky** | **Let go** of the box. If nothing is selected, it marks a corner in the air, 6 blocks ahead: you can build a platform in the sky. | |
| **Left click a creature** | Hits it, as usual. | |
| **Right click** | **Do it.** With nothing ready it is plain Minecraft (doors, chests, eating, placing one block). | Hold it and sweep: Paint, Land and Grow. Copy: the first copies, the next ones stamp. Move: the first picks up, the second puts down. More than 64 blocks removed or replaced: **hold** it for one second until the ring round your crosshair is full; more than a million blocks: two seconds. |
| **Scroll** | The main number: push the side of the box you look at, the size of a shape, the number of copies. With nothing to adjust it is your hotbar, as always. | |
| **Ctrl + scroll** | The same, 8 at a time. | |
| **Alt + scroll** (Left Alt) | **Which kind**: the style, the shape, turning a quarter, the gap of a Repeat. | |
| **Middle click** a block | Take **that exact block** (turned, upside down, open) as your material. | |
| **Ctrl + Z** / **Ctrl + Y** | **Undo** / **redo**. | |
| **Ctrl + R** / **Ctrl + F** | Turn a ghost a quarter / flip it. | |
| **Ctrl + C** / **Ctrl + X** / **Ctrl + V** | Copy the box / pick the box up (Move) / bring the copy back as a ghost. | |
| **Delete** | Vanish what is in the box. | |

## Choosing what to build

### The box, corners and growing

Click one corner, then another: a glowing **box** (the *frame*) stretches between them, with its size and the number of blocks floating above it, like **"7 × 4 × 5 · 140"**. Look at one side of the box and scroll: the side moves one block a notch (Ctrl: 8). A white arrow shows which way scroll-up pushes. The box also shows faintly through hills, so you can push a side that is inside the ground. Left click the sky to let go.

The box stays when you choose another spell, so you can Fill it, then Vanish it, then Change it.

### Magic select: the whole thing

**Double click** a block, and the magic finds the *whole thing*:

* on something **built** (a house, a tree, a statue), every block of it that touches, even by a corner. A house on a meadow stops at the grass, a tree at its dirt;
* on **ground** (sand, a granite boulder in the stone), the connected blocks of that same block.

The glow spreads through it as it is found, and the label gives the number of blocks. Then the spell works on *exactly that thing*: Fill turns a tree into gold, Vanish erases only the tree, Copy takes the tree without the air around it. Double click something else to add it (up to 8 things).

If the thing is too big, *nothing* is selected (never half a tree): "Too big to hold in mind". Billy's limit is the size of his meter (512 at the start); Wanda's is 100,000 blocks, and for her it says "Mark it with two corners instead".

### The block

**The block in your main hand is your material** (or the off hand if the main one is empty). Change the hotbar slot and the material changes. The block comes the way you would place it by hand: stairs face away from you, logs follow the side you look at.

* **Middle click** a block in the world to take exactly it, in the state it is in. Changing the hotbar slot goes back to the hand.
* **Mix**: in the wheel, the dice switches on a mix of your hotbar's blocks, weighted by how many you carry in each slot (32 stone and 8 cobblestone is 80% and 20%). The ghost shows exactly the mix that will be built.
* **Blocks**: in the wheel, a page of big block pictures from the game's creative tabs, with the last 16 you used and a search box. Pick one and your magic makes it, even if you do not carry it.
* A **water bucket** in your hand makes water, and a **lava bucket** lava (Wanda only). Placed water flows afterwards by itself.
* **Doors, beds and tall flowers** cannot be the material (two blocks tall), but Copy, Move and Repeat carry them whole, and no spell ever cuts one in half.

By default the blocks **come from magic**: nothing is taken from you. A server can switch on `requireMaterials`: then, in Survival, each block costs one item from your inventory and vanished blocks go into it (Land and Grow move and grow the ground and are never paid with items).

## The eleven spells

Choose with the wheel (the number keys 1 to 9 and 0 choose too; see [above](#the-spell-wheel)). Each spell takes at least half a second, however small, so you *see* the world grow: fills rise out of the ground, shapes swell from their heart, vanishing melts from the top. **One spell is one step for Ctrl + Z.**

| # | Spell | What it does | Right click | Scroll | Alt + scroll |
|---|---|---|---|---|---|
| 1 | **Fill** | Fills the box (or the selected thing) with your block | fill | push a side | the style |
| 2 | **Vanish** | Makes what is in the box disappear | vanish | push a side | the style |
| 3 | **Change** | Turns one kind of block into yours, keeping its shape | change | push a side | the style |
| 4 | **Paint** | A brush: paints surfaces | hold and sweep | brush size | |
| 5 | **Pull & Push** | Pulls a wall, floor or tower out, or pushes it in | pull | layers out (up) or in (down) | |
| 6 | **Shapes** | A ball, a dome, a tower... that grows | place | size | which shape |
| 7 | **Copy** | Copies the box into your pocket, then stamps it anywhere | copy, then stamp | lift | turn a quarter |
| 8 | **Move** | Picks up the box and puts it down somewhere else | pick up, then put down | lift | turn a quarter |
| 9 | **Repeat** | Copies in a row: a bridge, a fence, a street | repeat | copies | gap |
| 10 | **Land** | A brush that raises hills, digs valleys and smooths the ground | hold and sweep | brush size | the style |
| 11 | **Grow** | A brush that swells rocks, shrinks them, and makes flowers bloom | hold and sweep | brush size | the style |

(Undo and Redo are keys and wheel buttons, not spells.)

### Fill

| Style | What it fills | A 5 × 4 × 5 box |
|---|---|---|
| **Solid** | every block | 100 |
| **Walls** | the four sides, floor to roof, with no floor or roof | 64 |
| **Room** | walls, floor and roof, and **the inside is emptied**: an instant house | 82 placed, 18 emptied |
| **Floor** | the bottom layer | 25 |
| **Frame** (שלד in Hebrew: just the edges) | the twelve edges | 40 |
| **Only the empty** | only air and what a block replaces (grass, flowers, snow, water): everything else stays | |

A Room costs less than Solid, and Walls even less: *hollow is clever*. With a magic selection, Fill turns the selected thing's own blocks into the material.

### Vanish

* **Everything**: every block in the box becomes air (**Delete** does this at once). Water may flow in afterwards, as after digging.
* **Hollow out**: everything except the outer layer: a room carved inside a hill.
* **Plants and trees**: only logs, leaves, flowers, grass, vines, mushrooms, bamboo, kelp and the like: "clear the land for a house".
* **This kind**: only the kind of block you look at inside the box (every leaf, every cobweb).

Blocks vanish **without dropping anything** (it is magic, not mining), in a puff of dust in your colour.

### Change

* **This kind** (the kind you look at): becomes your block, **keeping its shape**: oak stairs become spruce stairs if your block is spruce planks or spruce stairs.
* **Everything**: every non-air block.
* **Only the outside**: only blocks that touch air: repaint the skin of a hill or a house.

Change never makes air (that is Vanish) and never fills air (that is Fill).

### Paint

No box. **Hold right click and sweep**: every surface block under the brush (a ball round the block you look at) is changed the way Change does it, so stairs stay stairs and the inside of a hill stays untouched. Scroll sets the radius (Billy: 3 at the start; Wanda: up to 16). The whole stroke, from press to let go, is one undo step. With a magic selection, only that thing is painted.

### Pull & Push

No box. Look at a wall, a floor or a roof: the magic finds that *whole face* (all the blocks of the same kind that are connected to it). **Scroll up** shows new layers coming out of it; **scroll down** shows layers going into it, in red. "Pull this wall thicker, this floor lower, this tower taller."

### Shapes

Eight shapes with Alt + scroll: **Ball, Dome, Tower, Ring, Cone, Pyramid, Arch** and **Heart**, each **solid or hollow** (a switch in the wheel). The ghost **sits on the surface you look at**, like a block you are about to place. Scroll is the radius. **Click plants it**; for a Tower, Ring, Cone or Pyramid the **height now follows where you look**: look up and the tower rises. Click again to fix the height; right click casts either way. The Arch and the Heart stand upright, facing you.

Round shapes swell out of their heart, shell by shell. A hollow shape is one block thick; shapes that stand on the ground (Dome, Tower, Cone, Pyramid) have no floor, since the ground is their floor. A few numbers: a hollow dome of radius 3 is 57 blocks, a hollow ball of radius 6 is 414 blocks (it fits Billy's first meter), radius 7 is 530 (it does not).

### Copy and the pocket

The first right click **copies** the box (or the selected thing) into your **pocket**; nothing in the world changes, and a copy of it appears at your crosshair as a ghost. Each right click then **stamps** one, built from the ground up. Scroll lifts it, **Alt + scroll** or **Ctrl + R** turns it a quarter, **Ctrl + F** flips it. A copy of a box brings its air (a house stamped into a hill gets its rooms); a copy of a *selected thing* brings only the thing (a tree stamped into a forest keeps the forest). A left click starts a new box; **Ctrl + V** brings the pocket's ghost back.

The pocket is kept on the server (and, in your folder on its disk, with your undo history): it survives dying, logging out, a change of dimension (copy a house in the Overworld, stamp it in the Nether) and a restart. Billy's pocket is at most his longest side on every axis; a copy of more than `pocketMaxMb` (64 MiB compressed at the start) is too big for anyone's pocket. Copies never copy creatures, so nobody makes a herd of villagers.

**What is inside chests.** Billy's copies of chests always come out empty. Wanda's keep what is inside, but **only where she may break the block**: in a claim, a protected place or a vault that is not hers, the copy is the bare, empty chest, so a Copy or a Repeat is never a way to read what a claim keeps closed.

### Move

Like Copy, but the first right click **picks up** (the old place gets a red veil) and the second **puts down**: one spell, one undo step. Chests move with everything in them, and nothing is ever lost or doubled:

* **A Move spares a full chest.** If the new place holds a chest with things inside, that chest is left standing (and the block that was to land there stays where it was), just like a Fill would leave it. A server can set `fullContainers` to `SPILL`; then the chest's things spill on the spot.
* **The contents are never in two places.** A block someone changed in the old place meanwhile is left alone. Undo and redo of a Move take the same care: a chest is restored only if it is exactly where the Move put it, and a redo never wipes a chest that someone filled after the undo.
* **Creatures go along** (animals, item frames, paintings and armour stands in the box) only with blocks that really moved, and only where you may build.
* A Move needs room in the server's undo memory (at the default settings, a Move of more than about 4 million blocks is refused): see [Undo](#undo-ctrl--z-even-tomorrow).

### Repeat

Copies of the box in a row, in the direction you look, with the arrows on the box showing it. Scroll is the number of copies (Ctrl: 4 at a time), Alt + scroll the gap (0 to 16). A bridge, a fence, a street of houses, a tower of floors.

### Land

A brush for the **ground**. Hold right click and sweep, scroll for the size, Alt + scroll for the style:

* **Raise** lifts the ground (the block on top moves up one, and plants ride up with it), **Lower** sinks it, **Smooth** evens it out. Each pulse (every 4 ticks) moves a column by a little, and the middle of the brush more than its edge, so a hill rises smoothly.
* It moves *ground*, whole columns at a time. Trees and houses stand where they are, never float, and are left alone. Land never copies ores.
* Creatures standing on a rising column are lifted with it.

### Grow

A brush for **living and stony things**, in Land's way: no box, **hold right click and sweep**, scroll for the size, Alt + scroll for the style. A ball of magic round the block you look at changes a little every 4 ticks, so a rock swells layer by layer while you sweep.

* **Swell** makes a rock grow into a boulder: every empty spot (or plant or snow) inside the ball that touches a solid block inside it becomes that block (the one most of its neighbours are; a tie goes to the one below).
* **Shrink** is the opposite: a solid block with air on two or more sides vanishes, so corners and edges melt first and a hill shrinks.
* **Bloom** is Westview's garden: dirt, coarse dirt and rooted dirt on top become grass; on grass and moss a quarter of the spots sprout short grass, one in eight a flower and one in thirty-two a tall flower; netherrack turns into crimson nylium (warped nylium in a warped forest) with roots; mycelium sprouts mushrooms. Anywhere else the game says "Nothing grows here".

Grow works on whole blocks only: chests, furnaces and anything else that holds things never swell or shrink, protected places and boss arenas are left alone, and the whole stroke is one undo step. Billy pays one point for each block it changes, and his brush is an eighth of his longest side (3 at the start); Wanda's is up to 16, the server's brush limit. Like Land it never takes items from you.

## What you see

* **The ghost** is the result, in see-through blocks tinted in your colour, before you cast. It is the same plan the server will build, so what you see is what you get (unless the world changes in between; then the server's world is the truth). Very big ghosts are drawn from the outside only.
* **The badge** next to your crosshair shows your material, the size, the spell and style, and Billy's meter (see below). Over the whole world the crosshair turns into a little rune in your colour.
* **The server's marks.** The server looks at the spell before you cast it and puts small marks on places that will be **left alone**: a **red cross** is protected (a boss arena, spawn, a claim), a **yellow** cube is a chest with things in it, **white** means someone stands there, **grey** is a place that is not loaded. The ghost round them turns grey. If the server would refuse the whole spell (a box inside a boss arena, say), the whole ghost goes grey and the right button only fizzles.
* **The rings.** Round your crosshair, a ring fills while you hold the right button for a big spell (red for blocks that disappear), another shows your spell's progress while it works.
* **Tips.** The first times you use a spell, and whenever you do something that does nothing, a small card plays a three-second picture of what to press.
* **Other builders.** Everyone sees a builder's box as thin edges in his colour within 64 blocks, and sees his spells grow: a glowing frame and a ripple sweeping the spell, sparkles, and for Billy a Hex Bolt arc from his hand. A thin scarlet thread runs from Wanda's hand to Billy's when she is helping him.

* **Sounds.** Eight moments have a sound of their own, with a subtitle: build mode on (a different one for each hero), a spell cast (Wanda's, Billy's), the hum while a big spell works, a refusal (a short "pfft", only for the builder), an undo or redo, and Billy's rank-up. The little sounds of the corners and clicks are the game's own.

Every one of these can be switched off in *Mods, Enhanced, Config, Build Magic* (see [Server Configuration](Server-Configuration#client-options-animations-effects-and-looks)).

## Billy: an apprentice who learns

Billy has every spell from the first minute. What grows is **how big**.

* **The magic meter.** A spell costs one point per block it really changes (copying and looking are free). The meter refills all the time, also when you are not building, and even when you are logged out. A spell that costs more than you have **does not start** (never a half-built house), and says what to do: *wait*, *make it smaller*, *try a hollow style* or *find Mom*.
* **Undo gives the points back.** Redo costs them again.
* **Practice.** Every block of magic you cast is practice, and **an undo takes the practice back**, so fill-and-undo cannot make you a Wiccan. Your rank never goes down.

| Rank | Practice (blocks of magic) | Meter | Refills | Longest side | Reach | Undos | Repeat | Brush |
|---|---|---|---|---|---|---|---|---|
| 1 **Apprentice** | 0 | 512 | 16 a second | 24 | 24 | 10 | 8 | 3 |
| 2 **Spellcaster** | 2,000 | 1,024 | 24 a second | 32 | 32 | 12 | 12 | 4 |
| 3 **Sorcerer** | 8,000 | 2,048 | 32 a second | 40 | 40 | 14 | 16 | 5 |
| 4 **Wiccan** | 30,000 | 4,096 | 40 a second | 48 | 48 | 16 | 20 | 6 |

Each rank adds the same step to the Apprentice's numbers: the meter **doubles** (512, 1,024, 2,048, 4,096), the refill grows by half of the first (16, 24, 32, 40 a second), the longest side and the reach grow by **8 blocks**, he can undo **2** more spells and Repeat **4** more copies, and the brush is an eighth of the side. A server can change all of it (see [Server Configuration](Server-Configuration#build-magic)), and an operator can set a rank with `/enhanced build rank`. Rank 1 fills the meter from empty in 32 seconds. When you rank up, your crown flares white, runes spin round you, everyone near hears a fanfare, and the meter fills to its new top as a gift: *"You learned more magic! Rank 2: Spellcaster."*

**Mom's help.** When a Scarlet Witch (any player with her power, alive, not in Spectator mode and in the same world) is within **16 blocks**, your spell costs **half** (rounded up). A thin scarlet thread runs from her hand to yours, the badge shows a scarlet stripe and "½", and the first time it says *"Mom is near: your magic costs half."* Your practice still counts the real blocks. Wanda just has to be there.

What Billy is told, and what to do:

| When | He sees | What to do |
|---|---|---|
| Not enough magic *right now* | an orange chunk on the meter and a clock, "Your magic is refilling: 7 more seconds" | wait |
| Too big for the *whole* meter | a grey ghost, "Bigger than your magic yet: 530 of 512" | make it smaller, a hollow style, or cast it near Mom |
| Longer than his side | the side stops growing, "Too big for you yet: at most 24 blocks on a side" | smaller, or practise |
| Too far | "Too far for your magic (at most 24 blocks)" | go closer |
| One spell at a time | "Wait for your spell to finish" (and a pause of half a second after each) | wait |
| TNT, lava and fire | "too dangerous for your magic" | never: only Wanda |
| His copies of chests | come out empty | (so nobody can copy diamonds) |

A hollow ball of radius 6 (414) fits his first meter; a house shell of 7 × 4 × 5 (110) can be cast four times in a row. A castle takes several spells and a refill, like every hero's cooldowns. In Creative his limits still apply (a server can change that).

## Wanda's magic has no limit

Wanda has **no meter, no cost, no ranks and no size limit**: every spell and style, any shape up to radius 512, as many undos as the server's memory and disk hold, copies of chests with what is inside (where she may break them), lava and TNT. She points as far as **256 blocks**, and she can fly to the far corner of a valley and click it there. Up to four of her spells wait behind the one at work, and run one after the other.

**Inside her own Hex, her spells go twice as fast:** reality obeys her faster at home.

Under it there is only **technical safety**, so the game never freezes. You do not feel it in normal play, but you will see the ripple take its time with a huge spell:

* every spell is cut into small slices, and all the spells of the server together get at most 8 milliseconds of every tick (half of that when the server is struggling): a platform of 200,000 blocks takes a few seconds, a million blocks about 6 to 25 seconds, and the game keeps its 20 ticks a second;
* a spell changes only the chunks that are **loaded** when it is cast, and never makes new ones (a spell keeps at most 256 chunks loaded at a time while it works; a box with almost nothing loaded ends at once and says how many blocks were too far away);
* at most 16 spells work or wait on the whole server at any moment ("Too much magic at once on this server: wait a moment");
* at most 4,096 TNT, lava or fire in one spell;
* her Paint, Land and Grow **brush stops at radius 16**. That is the *server's* limit, not her magic's: a brush spends its work every 4 ticks while she sweeps, and at radius 16 it is about 17,000 blocks a pulse, which the server keeps up with; at 32 it would be 137,000 and the stroke would trail seconds behind her hand. Scroll just stops there, and nothing is ever said.

## Undo: Ctrl + Z, even tomorrow

**Ctrl + Z** takes back your last spell, **Ctrl + Y** does it again. They work **even after you turned build mode off**. What Ctrl + Z does, in order:

1. a spell of Wanda's that is still **waiting** is dropped, and never happened;
2. otherwise, if your spell is **working**, it stops and is taken back;
3. otherwise your newest spell is undone, the ripple running backwards;
4. "Nothing to undo".

Undo is **safe**. It puts a block back only if the block is still exactly what the spell made: if a sister built a door into your wall since then, or someone put things into a chest Wanda stamped, those blocks stay, and the game says *"3 blocks changed since then stay as they are."* Undo never destroys anyone's later work. If part of the area has become protected since, those blocks stay in the step and you can try again later. Undo does not bring back what the world did by itself afterwards (water that flowed, redstone that powered), nor items a spill dropped.

### The history on the disk

Your spells are kept **on the server's disk**, in the world's folder, one folder for each player: `data/enhanced/build/<the player's id>/`. It holds a list of your spells, a file for each spell, and your pocket. So you can close the game, even the whole server, and undo tomorrow.

* **Written ahead.** Before a spell changes a part of the world, what is there (and what the spell will put there) is already written to the file, a slice at a time, off the game's main thread. A crash or a power cut in the middle of even a huge spell leaves a record of every block it changed. The next time you log in the game says *"Your last spell stopped when the world closed: Ctrl + Z undoes what it did"*, and it does: each block it reached goes back if it still holds what the spell put there.
* **When the server stops,** the spells still working are finished if what is left of them adds up to at most `finishOnStopBlocks` (200,000 blocks at the start); the biggest are stopped where they are, and can be undone.
* **After a restart** your list is read when you log in, and a spell's file is read only when you press Ctrl + Z for it (a moment's wait). A spell in a world that no longer exists is dropped.
* With `undoSurvivesRestart` off nothing is written: the history lives in memory, is lost when the server stops, and is dropped 30 minutes after you leave.

### How much is kept

* **How many spells:** Billy 10 and 2 more a rank (up to 16); Wanda as many as fit.
* **Memory and disk.** Each player may use 64 MiB of the server's memory and 512 MiB of its disk for the history (`undoMemoryMb`, `undoDiskMb`), and all players together 512 MiB and 4,096 MiB (`serverUndoMemoryMb`, `serverUndoDiskMb`). Past the memory limit the **oldest** spells leave memory (they stay on the disk, and come back when you undo them). Past a disk limit the oldest spell is **forgotten** (of the player using the most, for the server's limit) and you are told *"Your oldest spell can no longer be undone"*. Your newest spell is never lost to a disk limit.
* **Huge spells ("lite steps").** While a spell works, its step is kept whole in memory, about 14 bytes for every block. A spell of more than about 4 million blocks (at the start settings) would not fit, so the server keeps only a bit for each block and lets the file's records on disk be the step. You notice nothing: it is undone like any other, read back from the disk. This needs the history to be written to disk, and the record of the spell (about half a byte for every block) to fit in your disk share, which at the start settings is hundreds of millions of blocks.
* **Too big to remember.** If even that does not fit (the disk share is full or switched off), the game says *"Too big to remember: this one can't be undone. Hold to cast it anyway."* Holding the right button casts it without a step, and nothing can bring it back. A **Move** that does not fit is refused outright, held or not.

An operator can look at any player's history and undo it with [`/enhanced build`](Commands#build-magic-enhanced-build).

## What magic will not touch

Build Magic follows every rule of the world. A spell leaves these exactly as they are and, at the end, tells you how many:

* **Protected places**: spawn protection, the world border, **claims** (FTB Chunks, Open Parties and Claims and the like are asked for every block), and **Adventure mode** (build mode will not even start).
* **Boss arenas.** **No spell changes a block in the arena of any running boss fight**, Thanos's included (so nobody cages him in a ball of obsidian). A spell that is working when a fight starts there stops. After the fight, undo works.
* **The island in the sky** of Ultron's fight, and the ground that clients hide under it.
* **Chests and other containers that hold things** stay (unless the server sets them to spill). A Move carries them with their contents, but a Move never runs over a full chest at its new place either. A Copy or a Repeat keeps a container's contents only where you may break it.
* **Blocks that are beyond magic**: bedrock, barriers, command and structure blocks, portals, spawners, vaults, reinforced deepslate, and **Mjolnir lying as a block**. Magic never *makes* a few blocks either: budding amethyst, the dragon egg, infested blocks, frosted ice, Spider-Man's web. And **TNT, lava and fire**: never Billy, and at most 4,096 a spell for Wanda.
* **People.** Creatures inside a block that appears are lifted on top of it, unharmed (and you too: build a floor under your own feet and it raises you). **Another player is lifted only when PvP is on**; with PvP off, nobody is ever buried by someone else's spell, and a Move never carries a player. With PvP off, nobody is put into a danger either: **water, lava, fire, TNT, powder snow, cobwebs, berry bushes, magma blocks, wither roses and campfires** are not placed where another player stands, and that is the same for the blocks of a fluid, not only for solid ones. A creature **Mjolnir pins** is never lifted or carried.
* **Falling blocks** a spell places in the air (sand, gravel) **stay where the magic put them** until something touches them, so a sand roof stays a roof.
* **Telekinesis blocks** a hero holds are entities, not blocks: spells pass them by.

Witches' Road doors are ordinary blocks to magic: build or vanish one and it joins or leaves the Road by itself.

## Questions

**I vanished my house!** Press **Ctrl + Z**. Even tomorrow.

**The box (or the ghost) is grey.** The server will leave those blocks alone: look for the little marks. Red is protected, yellow is a chest with things in it, white is someone standing there, grey is not loaded. If the whole ghost is grey, nothing can be built there (or, for Billy, it is too big for his magic).

**Why did my chest stay?** A container with things inside is never removed or replaced. Empty it, or Move it (a Move carries what is inside). A chest at the place a Move is going to is spared the same way.

**Can I undo after the server restarted?** Yes. Your spells are on the disk; Ctrl + Z reads the newest one back. Only a spell that was "too big to remember", or one that fell off the end of the memory and disk limits, is gone.

**Why does my sand float?** Magic holds what it places until something touches it. Break a block next to it and it falls.

**Why can't Billy build a castle in one go?** His meter holds 512 blocks at the start, and a spell that doesn't fit never starts half-built. Make it hollow, build it in pieces, build next to Wanda (half the cost), and practise: at Wiccan his meter holds 4,096.

**Nothing happens when I right click.** Build mode needs something *ready*: a box (or a thing you double clicked) for Fill, Vanish, Change, Copy, Move and Repeat, and a block in your hand for Fill, Change, Paint and Shapes. The strip at the bottom right says what is missing.

**Ctrl + Z does something else!** Out of build mode, Ctrl + Z and Ctrl + Y are still undo and redo for Wanda and Billy. The game's Controls screen marks them (and the keys they share) in red: that is normal, and it still works. See [Controls](Controls#build-magic-y).

## For servers

Build Magic has three sections in the server config: `[build_magic]`, `[build_magic_wanda]` and `[build_magic_billy]` (the time and the speed budget, the undo history with its memory and disk limits, the hold-to-confirm rules, Wanda's reach and caps, Billy's meter and ranks, and a switch for each hero), and five client options. See [Server Configuration](Server-Configuration#build-magic). Operators can undo, stop and inspect spells with `/enhanced build ...`: see [Commands](Commands#build-magic-enhanced-build).

A few notes for hosting: every block a spell changes is also sent to every player who sees it. For a server at home or with a modest upload, lower `build_magic.blocksPerTick` (5,000 keeps each watching player under about 0.4 MB a second). The undo histories live in `data/enhanced/build/` in the world folder: with the defaults they may take up to 4 GiB of the disk and 512 MiB of the memory in all, and `/enhanced build forget <player>` clears one player's. A boss arena is always safe from Build Magic while `build_magic.allArenasProtected` is on. And `build_magic.operatorsOnly` limits it to operators, if you prefer.

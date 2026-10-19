# Asgard

**Asgard, the Realm Eternal** (3.5): a whole new world of gold, floating over a sea of stars, as in *Thor*, *The Dark World* and *Ragnarok*. Odin's palace with its spires, the throne room, the Rainbow Bridge and Heimdall's Observatory at its end, gardens, halls, houses, training grounds and waterfalls that pour off the edge of the world into space. It is a dimension of its own, and **only the Bifrost goes there** (and, since 3.6, the Space Stone).

Hebrew: [עברית](Asgard-HE)

## The quick version

* **Getting there:** open the **Bifrost** and choose **Asgard** at the top of its screen. The Bifrost answers Mjolnir, Stormbreaker, **Hofund** (Heimdall's sword), your own **Gungnir**, and the heroes Heimdall and Odin. With no power and none of those, find a **Bifrost Rune Stone** in the Overworld and right-click it.
* **You land** in **Heimdall's Observatory**, the golden dome at the end of the Rainbow Bridge. Walk the bridge north to the city.
* **It is safe:** no monster is born there, the city can't be broken, a fall off the edge is caught by Heimdall, and if you die there you keep your things.
* **Getting home:** the Bifrost again (**Home** on its screen), or ask Heimdall in the Observatory to send you home.
* **What to do there:** meet [the Asgardians](Asgardians), take [Heimdall's](Heimdall) **Guardian's Watch**, break **Hela's Seal** behind the throne and fight [Hela](Hela), then take **Gungnir** and become [Odin](Odin).

## Only the Bifrost

No portal leads to Asgard or out of it. A Nether portal cannot be lit there, the [Book of the Vishanti](Sorcerers-Books) can neither save a place in Asgard nor travel to or from it ("The Vishanti cannot reach the Realm Eternal."), and the Tesseract's own jump, a Sling Ring and other mods' teleports are turned back: "Only the Bifrost reaches Asgard." (An operator's `/tp` and `/execute in` still work.)

**The one exception** (3.6) is the **Space Stone**: its **Realm Picker** (Sneak + right-click the stone, or the Tesseract) takes you to Heimdall's Observatory, and back out to the Overworld, the Nether or the End. A server can turn that off (`space_stone.reachesAsgard`). The Snap and the Mirror Dimension don't work in Asgard. See [Infinity Stones](Infinity-Stones#the-space-stone-blue).

### The keys of the Bifrost

| Who | How |
|---|---|
| Anyone holding **Mjolnir** or **Stormbreaker** | Use the weapon (right-click): the Bifrost screen. Sneak + use: to where you look. [Thor](Thor) uses **N**. |
| Anyone holding a **Hofund**, anyone's | The same (right-click; Sneak: to where you look). A server can switch this off (`asgard.hofundOpensBifrost`). |
| The owner of a **Gungnir** | The same, but only with his own Gungnir. Somebody else's is just a spear. |
| [Heimdall](Heimdall) | **V**, with nothing in his hand (Sneak + V: to where he looks). |
| [Odin](Odin) | Use his Gungnir, as any owner. |
| A [custom hero](Hero-Creator) | With Thor's or Heimdall's Bifrost ability, on the key you gave it. |

### The Bifrost screen

Since 3.5 the screen has a row of **realms** at the top:

* **Asgard** (gold and rainbow), only while you are outside Asgard. It always takes you to the Observatory.
* **Home**: your bed or respawn anchor, otherwise the world spawn. Inside Asgard it is called **Home (Midgard)**.

Below them are your **saved places**, as before (see [Thor](Thor#the-bifrost-and-its-saved-places)). You can save places inside Asgard too, and use them from inside Asgard; from another world, a saved place in Asgard lands you in the Observatory, because the Bifrost always enters Asgard there.

### The Bifrost Rune Stones (for players with no power)

Somewhere in the Overworld, in **plains, meadows, snowy plains, taigas, windswept hills and badlands**, stands a **Bifrost site**: a ring of standing stones round a circle of Bifrost runes burned into the ground, and the **Bifrost Rune Stone** in the middle. They are rare (in those biomes, about 750 blocks apart); `/locate structure enhanced:bifrost_site` finds the nearest.

* **Right-click the rune stone.** You call out "Heimdall! Open the Bifrost!" (everyone within 32 blocks reads it), the Bifrost gathers round you for **3 seconds**, and takes you, and everyone in its circle, to the Observatory. No key, no power and no cooldown of your own.
* The **stone** rests **60 seconds** after each journey (`asgard.runeStoneCooldown`): "The stone is resting. Try again in 42 seconds."
* If the server has closed Asgard, the runes flicker and go dark.
* Heimdall remembers where you came from: in the Observatory he can **take you back** to the stone.
* The stone can't be broken.

### Leaving Asgard

* With a key: the Bifrost screen's **Home (Midgard)**, or a saved place anywhere.
* With no key: talk to **Heimdall** in the Observatory: **"Send me home"** (your bed or the world spawn) or **"Take me back where I came from"** (the rune stone, or the place you left from). See [the Asgardians](Asgardians).

## The Bifrost takes everyone in its circle

Since 3.5 **every** Bifrost, Thor's too, carries everyone who stands inside the **circle of runes** it burns into the ground (radius 3.2 blocks: the circle you see), not only the one who opened it:

* **Who goes:** every player and every creature whose feet are inside the circle (and no more than 3 blocks above or below it): your friends, your pets, villagers, a horse with its rider (both go, and he is riding it again when they land), dropped items, boats and minecarts. The nearest 64 at most.
* **Who stays:** bosses and everything their fight made, spectators, armour stands, someone pinned down by Mjolnir, someone already on a Bifrost journey of his own, and anyone a gate turns back (Asgard's, a boss arena, a Hex wall, the Witches' Road): he stays and the others go.
* **Sneaking does not keep you out**, unless the server turns on `bifrost.sneakToStay`. To stay, step out of the circle before the light takes you.
* **Landing:** everyone keeps his place in the circle round the opener, moved up or down at most 4 blocks to the first spot where he fits and stands on something; if there is none, he lands on the opener's spot. Nobody ever lands inside a block.
* **Only the opener pays** the Bifrost's cooldown.
* A server can change the circle with `bifrost.groupRadius` (0 to 16; **0** = only the opener goes, as before 3.5). See [Server Configuration](Server-Configuration#the-bifrost).

## The city

You arrive facing north, toward the city. The map, from the edge of the world to the palace:

| Place | What is there |
|---|---|
| **Heimdall's Observatory** | A golden dome over the void at the southern end of the Rainbow Bridge. In the middle, **Hofund's Socket** and round it the **Bifrost Mechanism**, a gold ring that spins whenever the Bifrost leaves or lands near it. [Heimdall](Heimdall) keeps watch here. |
| **The Rainbow Bridge** | About 230 blocks of shining, flowing rainbow crystal, 7 wide, with golden rails, from the Observatory to the gate of the city. The last stretch runs over the void. |
| **The bridge gate** | Where the bridge meets the city; Einherjar stand guard. |
| **The Golden Road** | The city's main street, north to the palace: golden paving, braziers, statues of Asgard's heroes, and the **Arch of Victory**. |
| **The plaza** | A fountain, two shrines, market stalls and wells, at the foot of the palace steps. |
| **Odin's palace** | Its golden spires (the tallest rises to about height 200), the east and west wings, the palace library and **Odin's chamber**, where Odin sleeps the Odinsleep. |
| **The throne room** | A long hall of gold pillars with a seashell floor, the **Throne of Asgard** on its dais at the north end, **Hela's Seal** in the wall behind it, and murals of Asgard's peace on the ceiling that crack to show Hela's conquests when she comes. |
| **Odin's vault** | Under the throne room, closed by a wall of gold until Hela first falls. **Gungnir's Stand** is inside, and since 3.6 the **Reality Stone** on its pedestal (the Aether, a red swirling orb). |
| **Frigga's gardens** | West of the palace: pavilions, fountains, little bridges, golden-leaf and blossom trees, and waterways that fall into a deep chasm. |
| **The training grounds** | East of the palace: the round sparring ring, weapon racks and a pavilion. Sif and the Warriors Three train here. |
| **The Hall of Feasts** | East of the Golden Road, with its kitchen. Volstagg is usually eating. |
| **The barracks** | Far to the south-east: the Einherjar's hall, their yard and two watchtowers. |
| **The east district** | The Hall of Healing, the Hall of Craft, the forge, the granary and the bathhouse. |
| **The houses** | West of the Golden Road: streets of houses where the citizens live, the Hall of Scribes, the Hall of Valor and the scribes' tower. |
| **The stables** | In the far south-west: Valkyrie and her horses. |
| **The docks** | On the western shore: a pier and two skiffs on the sea. |
| **The edge** | Round the land, the sea; round the sea, a stone lip, and over it the **cosmic falls** pour down into the stars. |

The city is the same in every world: it is built by the mod as the chunks are made, not by the world's seed. Chests in the houses and halls hold modest things (bread, gold nuggets, arrows, now and then an enchanted book), never a hero's item.

### The sky and the music

* **The sky:** blue and gold by day with a faint nebula, deep space at night, and stars all round, even below the horizon, so the void under the land looks like space. A ringed planet and two moons cross it. No clouds and no rain.
* **The music:** three original pieces play now and then, as the game's own music does elsewhere: *Realm Eternal* (harp, strings and a horn), *The Golden Halls* (a processional horn chorale) and *Edge of the Realms* (a choir and a celesta over the cosmic sea). Soft wind, a far hum and distant chimes in the air, and the roar of the falls near the edge. All written for the mod; no film music.

### The blocks of Asgard

The city is made of new blocks: **Asgardian Gold Plating**, **Gold Trim**, **Gold Pillar** and **Gold Lattice**; **Realm Stone**, its bricks (with stairs, slab and wall), polished and pillar; the **Rainbow Bridge** (it glows, and its colours flow along it); the **Cosmic Falls**; the **Mural of Asgard**; the **Asgardian Brazier**; **Golden Leaves of Asgard**; and the **Asgardian Bench** (sit on it). They are in the Creative tab; none of them can be crafted.

## The rules of Asgard

Asgard is made to be explored safely.

| Rule | What happens |
|---|---|
| **No monsters by nature** | No monster is ever born there by itself. (Spawn eggs, spawners an operator places and commands still work, and so do the mod's own creatures, such as Hela's army.) |
| **The city is protected** | In the city (the districts, the palace, the bridge, the Observatory) nobody can break or place blocks, empty buckets, light fires, trample farmland or push blocks with pistons, and explosions and creatures break nothing: "The city of Asgard is not yours to change." Fire that starts there goes out. The **wild land and the sea** round the city are free. Operators in Creative can edit (`asgard.protectCity`). Every power that changes blocks (Build Magic, the Magnetic Pull...) asks the same rule. |
| **Heimdall catches you** | Fall off the edge into the void (below height −56) and the Bifrost sets you down in the Observatory, unhurt: "Heimdall caught you." |
| **The edge of the realm** | Fly more than 640 blocks from the centre and you are pushed back: "The edge of the Realm Eternal." |
| **Your home is in Midgard** | A bed lets you sleep (the night passes) but never sets your spawn there: "Your home is in Midgard." Respawn anchors can't be placed. |
| **You keep your things** | If you die in Asgard you keep your inventory and your experience (`asgard.keepInventory`). With that off you still keep **Mjolnir, Stormbreaker, Hofund, Gungnir and Hela's Headdress**: you never lose a key to a realm you need it to reach. |
| **Closed Asgard** | If a server closes Asgard (`asgard.enabled` false), the Bifrost doesn't offer it, the rune stones fizzle, nobody can go in (operators can still `/tp`), and a player who logs in there is sent to his respawn point. Leaving is always allowed. |

## Things to do

1. **Meet the Asgardians**: Heimdall, Frigga, Sif, Valkyrie, the Warriors Three and the people of the city. They talk, give hints, trade and fight beside you. See [Asgardians](Asgardians).
2. **Take the Guardian's Watch** in the Observatory, win **Hofund**, and become [Heimdall](Heimdall).
3. **Break Hela's Seal** behind the throne and fight [Hela](Hela), the Goddess of Death. Win her **Headdress** (become Hela) and the right to Gungnir.
4. **Take Gungnir** from the vault and **sit on the Throne of Asgard** to become [Odin](Odin).

## For operators

* `/enhanced asgard tp [players]`: to the Observatory. `/enhanced asgard state`: Asgard's saved state (Odin awake or asleep, how often Hela was beaten, the seal, the residents) and how long the city took to build. See [Commands](Commands#asgard-enhanced-asgard).
* The options are in the `[asgard]` and `[bifrost]` sections: [Server Configuration](Server-Configuration#asgard).

## Related

[Heimdall](Heimdall) · [Odin](Odin) · [Hela](Hela) · [Asgardians](Asgardians) · [Thor](Thor) · [Structures](Structures#the-bifrost-rune-stones) · [Commands](Commands) · [Server Configuration](Server-Configuration#asgard)

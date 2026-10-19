# Meeting Death

Eilon's request (3.7): **"Every time you die, you meet Death, and she lets you start again, or start again in Hell."** Death is **Rio Vidal, the Green Witch**, of *Agatha All Along*, the same gentle gardener who lives at [Agatha's Cottage](Agathas-Cottage). When you die, instead of the plain red death screen you meet her in a misty green clearing, and **you choose your road**.

Hebrew: [עברית](Meeting-Death-HE)

## The screen

When you die, the screen fades into a **green clearing**: vines overhead, grass and little flowers underfoot, mist drifting by, leaves and petals falling. **Rio** stands on the left, tall and calm, her eyes following your mouse. On the right:

* **"You met Death"** and her name, **Rio Vidal, the Green Witch**;
* one short line of hers, a different one each time: "Not today. Choose your road.", "Every road leads back to the garden. Which one is yours?", "Rest is over, little hero. Up you get.", "I only came to say hello. Where to now?", "Even flowers grow back. So will you.", "The forest isn't ready for you yet. Off you go.";
* how you died, and your score, as on the usual screen;
* **your two roads**, and the small **Title screen** button under them.

Nothing scary, nothing gory. As on the usual screen, the buttons wake after a second (so a click meant for the fight doesn't choose for you) and **Esc** doesn't close it. Her soft harp of blessing plays when she appears.

## The two roads

| Road | Where you wake |
|---|---|
| **Start again** | At home, as always: in your bed, at your respawn anchor, or at the world's spawn. ("Wake up at home: in your bed, at your respawn anchor, or at the world's spawn.") |
| **Start again in the Nether** | In **the Nether**, on a **safe spot** near the place there that matches your home, with **10 seconds of Fire Resistance**. ("Wake up in the Nether, on a safe spot near the place that matches your home. Fire can't hurt you for 10 seconds.") |

**In the Nether:**

* The place that matches your home is the Nether's way of counting: your spawn point's x and z **divided by 8** (a bed at x 800, z −1600 matches x 100, z −200 in the Nether), at about height 64.
* Rio looks for a **safe floor** within 16 blocks of it: solid ground (not bedrock), two blocks of air, no lava, fire or magma right next to it, and under the Nether's bedrock roof.
* If there is none, she **makes one**: a small **obsidian floor** in a ring of netherrack, with air above it, and the lava and fire round it turned to netherrack.
* You wake there with **Fire Resistance** (10 seconds), a few green sparkles, and her line in green: "Rio opened a road to the Nether for you. Fire can't hurt you for 10 seconds."
* If a charged **respawn anchor** in the Nether is your home, both roads already lead there.

Your things follow the game's usual rules: they drop where you died (unless `keepInventory` is on). [Asgard](Asgard) still keeps them for you if you die there.

**Logged out before you chose?** When you log back in, still dead, you meet her again.

## When you don't meet her

* **When you don't die.** If something saves you, there is no death and no Death: a hero's [healing factor](Healing-Factor) (you only go down for 5 seconds), a **totem of undying**, a fall on the [Witches' Road](Witches-Road), or Ultron's Second Life.
* **In a hardcore world**, and with the game rule **`doImmediateRespawn`** on: the game's own way, with no screen.
* **On a server that switched it off** (`death_meeting.enabled`): the plain death screen.
* **Only "Start again"**, with no Nether road, if the server switched the Nether road off (`death_meeting.allowNether`) or the world has no Nether.
* **If you'd rather not meet her:** the client option *Meet Death when you die* (`showDeathMeeting`, in **Mods → Enhanced → Config → Meeting Death**) brings back the plain death screen for you alone.

Death by the [Vormir](Vormir) sacrifice and in a boss's arena are deaths like any other: you meet her, and both roads are open.

## Numbers you can change

Section `[death_meeting]` of the server's file: `enabled` (true), `allowNether` (true) and `fireResistanceSeconds` (10; 0: none, and the line says "Careful, it's hot!"). See [Server Configuration](Server-Configuration#meeting-death). The client's own switch is in `config/enhanced-client.toml`, section `[death_meeting]`: `showDeathMeeting` (true). See [Client options](Server-Configuration#client-options-animations-effects-and-looks).

## Related

[Agatha's Cottage](Agathas-Cottage) · [Healing Factor](Healing-Factor) · [FAQ](FAQ#how-do-i-start-again-in-the-nether) · [Server Configuration](Server-Configuration#meeting-death)

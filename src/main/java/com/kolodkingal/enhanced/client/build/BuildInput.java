package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildGrammar;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.VanishStyle;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Build Magic's input on the client (D-B 2, 11.10): the Y key, the two Ctrl keys a witch has at any time, the
 * build-mode shortcuts, and the mouse taken through NeoForge's two input events. Nothing else in the mod listens to
 * those two events, and {@code ClientEvents.handleKeys} is not touched: the hero keys behave as before, and the key
 * contexts ({@link BuildKeys}) keep Ctrl + Z, Ctrl + R and Ctrl + V of a builder off Z, R and V.
 *
 * <ul>
 *     <li><b>Y</b>: released within {@value #WHEEL_TICKS} ticks, build mode on or off; held that long, build mode on
 *     and the Spell Wheel opens ({@link SpellWheelScreen}). Only for a Scarlet Witch or Billy, with no screen open and not as a Spectator; for
 *     anyone else it does nothing and says nothing.</li>
 *     <li><b>Ctrl + Z, Ctrl + Y</b> ({@link BuildKeys.Context#WITCH}): undo and redo, in build mode or out of
 *     it (R42).</li>
 *     <li><b>In build mode</b>: Ctrl + R and Ctrl + F turn and flip a ghost, Ctrl + C copies the box, Ctrl + X picks it
 *     up, Ctrl + V brings the pocket's ghost back, Delete vanishes the box (with its hold).</li>
 *     <li><b>The left button</b> in build mode never breaks a block: on a block, or on the sky while the grammar takes
 *     clicks, the attack is cancelled (also held mining's repeats) and its press and release go to the grammar; on a
 *     creature it is vanilla's (you can hit things).</li>
 *     <li><b>The right button</b> is the spell's when it has something ready and the crosshair is not on a creature:
 *     one press casts once (vanilla's repeats every 4 ticks are cancelled), a hold spell goes when its ring is full, a
 *     brush dabs while held. Otherwise it is vanilla's: doors, chests, eating, placing one block.</li>
 *     <li><b>Middle click</b> picks the block as the material (as far as the hero's reach), and vanilla pick-block
 *     still happens.</li>
 *     <li><b>The scroll wheel</b> is the grammar's when it has something to change (Ctrl: 8 at a time; Left Alt held:
 *     the second knob), else the hotbar's.</li>
 * </ul>
 *
 * <p>A right click or a scroll that did nothing shows the tip that says how ({@link BuildTips}). At logout everything
 * of the builder is forgotten ({@link BuildClient#clear}, {@link BuildFx#clear}, {@link BuildRenderer#clear},
 * {@link BuildHud#clear}, {@link BlockPaletteScreen#clear}).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class BuildInput {
    /** Ticks Y is held before it is a hold (the Spell Wheel) rather than a tap (D-B 2.2, 14.7). */
    public static final int WHEEL_TICKS = 6;

    /** Ticks Y has been down. */
    private static int magicHeld;
    /** Whether a left press of the grammar's is down (its release goes to the grammar). */
    private static boolean leftHeld;
    /** The key of a right press of the spell's while it is down (the use key, or Delete), or null. */
    private static @Nullable KeyMapping rightKey;
    /** Whether the use key was down at the end of the last tick: a press held from before stays vanilla's. */
    private static boolean useWasDown;
    /** The scroll wheel's fractions (a smooth wheel or a touchpad), and whether the last notch was the grammar's. */
    private static double scrollRest;
    private static boolean scrollTaken;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        // Every click is drained each tick, so a click from a moment the key did nothing is never acted on later.
        boolean undo = clicked(BuildKeys.UNDO);
        boolean redo = clicked(BuildKeys.REDO);
        boolean tapped = clicked(BuildKeys.MAGIC);
        boolean turn = clicked(BuildKeys.TURN);
        boolean flip = clicked(BuildKeys.FLIP);
        boolean copy = clicked(BuildKeys.COPY);
        boolean cut = clicked(BuildKeys.CUT);
        boolean paste = clicked(BuildKeys.PASTE);
        boolean vanish = clicked(BuildKeys.VANISH);
        if (player == null || mc.level == null) {
            reset();
            return;
        }
        BuildClient.tick(mc, player);
        BuildHud.tick(player);
        BuildFx.tick(mc, mc.level);
        boolean usable = mc.screen == null && !player.isSpectator() && BuildClient.isBuilder(Powers.power(player));
        if (!usable) {
            magicHeld = 0;
            letGoOfButtons();
            useWasDown = mc.options.keyUse.isDown();
            return;
        }
        if (undo) {
            BuildClient.undo();
        }
        if (redo) {
            BuildClient.redo();
        }
        magicKey(tapped);
        if (!BuildClient.buildMode()) {
            letGoOfButtons();
            useWasDown = mc.options.keyUse.isDown();
            return;
        }
        BuildGrammar grammar = BuildClient.grammar();
        if (turn) {
            BuildFeedback.heard(grammar.turnGhost() ? BuildGrammar.Result.STYLE : BuildGrammar.Result.NOTHING, grammar);
        }
        if (flip) {
            BuildFeedback.heard(grammar.flipGhost(player.getViewVector(1.0F)) ? BuildGrammar.Result.STYLE : BuildGrammar.Result.NOTHING, grammar);
        }
        if (copy) {
            takeTheBox(player, Spell.COPY, SpellRecipe.Op.TAKE);
        }
        if (cut) {
            takeTheBox(player, Spell.MOVE, SpellRecipe.Op.PICK_UP);
        }
        if (paste) {
            BuildClient.paste(player);
        }
        if (vanish) {
            vanishTheBox(player);
        }
        buttons(mc, player);
    }

    /** Y: a tap toggles build mode, a hold turns it on and opens the Spell Wheel. */
    private static void magicKey(boolean tapped) {
        if (BuildKeys.MAGIC.isDown()) {
            magicHeld++;
            if (magicHeld == WHEEL_TICKS) {
                // Out of build mode, holding Y turns it on first (D-B 2.2); then the Spell Wheel opens.
                BuildClient.setMode(true);
                SpellWheelScreen.open();
            }
            return;
        }
        // A tap may begin and end between two ticks: then only its click says it happened.
        if (magicHeld > 0 && magicHeld < WHEEL_TICKS || magicHeld == 0 && tapped) {
            BuildClient.toggle();
            letGoOfButtons();
        }
        magicHeld = 0;
    }

    /** Ctrl + C, Ctrl + X: that spell, and the box copied or picked up at once when there is one. */
    private static void takeTheBox(LocalPlayer player, Spell spell, SpellRecipe.Op op) {
        BuildClient.grammar().dropGhost();
        BuildClient.choose(player, spell);
        SpellRecipe recipe = BuildClient.recipe();
        if (rightKey == null && recipe != null && recipe.op() == op) {
            BuildClient.cast(player, 0);
        }
    }

    /** Delete: Vanish, everything, on the current box, as a right click; held while its ring needs it (D-B 4.3). */
    private static void vanishTheBox(LocalPlayer player) {
        BuildGrammar grammar = BuildClient.grammar();
        grammar.spell(Spell.VANISH);
        grammar.style(VanishStyle.ALL.ordinal());
        BuildClient.update(player);
        if (rightKey == null && BuildClient.ready()) {
            rightKey = BuildKeys.VANISH;
            BuildClient.rightDown(player);
        }
    }

    /** The buttons held down: a left release goes to the grammar; a right press fills its ring, dabs, or ends. */
    private static void buttons(Minecraft mc, LocalPlayer player) {
        if (leftHeld && !mc.options.keyAttack.isDown()) {
            leftHeld = false;
            BuildClient.leftUp(player);
        }
        if (rightKey != null) {
            if (rightKey.isDown()) {
                BuildClient.rightHeld(player);
            } else {
                rightKey = null;
                BuildClient.rightUp();
            }
        }
        useWasDown = mc.options.keyUse.isDown();
    }

    /** Build mode ended, or a screen opened: no button is the spell's any more (a ring not full sends nothing). */
    private static void letGoOfButtons() {
        leftHeld = false;
        if (rightKey != null) {
            rightKey = null;
            BuildClient.rightUp();
        }
    }

    /**
     * Vanilla's attack, use and pick-block (D-B 11.10), in build mode with no screen open. Both hands' use get the same
     * answer: cancelling the main hand's returns before the off hand's is asked.
     */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !BuildClient.buildMode()) {
            return;
        }
        if (event.isPickBlock()) {
            // Picked in onMouseButton or onKey, where a block beyond vanilla's reach is picked too.
            return;
        }
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            // A creature in reach: hit it, trade with it, ride it, as always.
            return;
        }
        BuildGrammar grammar = BuildClient.grammar();
        if (event.isAttack()) {
            boolean onBlock = hit != null && hit.getType() == HitResult.Type.BLOCK;
            if (!onBlock && !grammar.takesClicks()) {
                return;
            }
            // Never a broken block in build mode, Creative included; held mining's repeats are cancelled too.
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!leftHeld && grammar.takesClicks()) {
                leftHeld = true;
                BuildClient.leftDown(player);
            }
            return;
        }
        if (!event.isUseItem()) {
            return;
        }
        if (rightKey != null) {
            // Vanilla repeats the use every 4 ticks while the button stays down: one press casts once.
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (useWasDown) {
            // A press held from before (a door opened, a block placed) keeps repeating as vanilla's.
            return;
        }
        BuildClient.update(player);
        if (!BuildClient.ready()) {
            // Vanilla's click (a door, a block placed); and if the spell lacks a box or a block, a tip says how.
            SpellRecipe recipe = BuildClient.recipe();
            BuildTips.rightClickDidNothing(grammar, recipe != null && recipe.spell().needsMaterial() && BuildClient.material() == null);
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        rightKey = mc.options.keyUse;
        BuildClient.rightDown(player);
    }

    /**
     * Middle click (the pick-block key, wherever it is bound), seen as it is pressed: vanilla asks its own event only
     * for a block within a hand's reach, and the builder picks one as far as his magic reaches; vanilla's pick runs.
     */
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            pressed(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            pressed(InputConstants.getKey(event.getKey(), event.getScanCode()));
        }
    }

    private static void pressed(InputConstants.Key key) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.screen == null && BuildClient.buildMode() && mc.options.keyPickItem.isActiveAndMatches(key)) {
            BuildClient.pick(player);
        }
    }

    /** The scroll wheel in build mode: the grammar's when it has a use for it, else the hotbar's. */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !BuildClient.buildMode()) {
            scrollRest = 0.0;
            return;
        }
        scrollRest += event.getScrollDeltaY();
        int notches = (int) scrollRest;
        scrollRest -= notches;
        if (notches == 0) {
            // A fraction of a notch: kept for the next, and taken from the hotbar while the wheel is the grammar's.
            if (scrollTaken) {
                event.setCanceled(true);
            }
            return;
        }
        scrollTaken = BuildClient.scroll(player, notches, Screen.hasControlDown(), BuildKeys.MORE.isDown()) != BuildGrammar.Result.UNUSED;
        if (scrollTaken) {
            event.setCanceled(true);
        } else {
            scrollRest = 0.0;
            BuildTips.scrollDidNothing(BuildClient.grammar());
        }
    }

    /**
     * Logging out forgets everything of the builder: his state, the grammar, the spells seen, the glow kept on the
     * graphics card (spec 5.6).
     */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
        BuildClient.clear();
        BuildFx.clear();
        BuildRenderer.clear();
        BuildHud.clear();
        BlockPaletteScreen.clear();
    }

    private static void reset() {
        magicHeld = 0;
        leftHeld = false;
        rightKey = null;
        useWasDown = false;
        scrollRest = 0.0;
        scrollTaken = false;
    }

    /** Whether {@code key} was pressed since the last tick (presses in the same tick count once). */
    private static boolean clicked(KeyMapping key) {
        boolean clicked = false;
        while (key.consumeClick()) {
            clicked = true;
        }
        return clicked;
    }

    private BuildInput() {
    }
}

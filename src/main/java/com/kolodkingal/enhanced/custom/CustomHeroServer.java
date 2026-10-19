package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.custom.modules.BodyPowers;
import com.kolodkingal.enhanced.custom.modules.ElementPowers;
import com.kolodkingal.enhanced.custom.modules.EnergyPowers;
import com.kolodkingal.enhanced.custom.modules.MindPowers;
import com.kolodkingal.enhanced.custom.modules.MovePowers;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.stones.StoneRowKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The server side of the custom hero (3.3), the plug-in of {@code Power.CUSTOM}: its keys go to the module the player
 * put on each, every power he picked ticks, shapes his body, and ends when he loses it. The thirty modules are
 * registered once, by group ({@link BodyPowers}, {@link MovePowers}, {@link EnergyPowers}, {@link ElementPowers},
 * {@link MindPowers}).
 *
 * <p>3.4, the key router: each of the five keys has a plain and a Sneak binding ({@link HeroKit}); a key going down takes
 * the Sneak one when Sneak is held and the cell is not free, and remembers it, so the release reaches the binding that
 * went down whatever Sneak does in between. A binding of a built-in hero's ability goes to that hero's code
 * ({@link HeroKeys#press}); a module's goes to its {@link ModuleLogic} (in the option the binding names). When a key
 * going down changed anything of the hero (a cooldown, a switch, a state), the cast marker ({@link CustomState#cast})
 * tells every client which binding acted. Every tick runs the upkeep of each hero he has abilities of, then his modules.
 */
public final class CustomHeroServer implements HeroServer {
    private static final Map<PowerModule, ModuleLogic> LOGIC = new EnumMap<>(PowerModule.class);
    /** The binding each key went down on (by key 1-5), per player, until it comes up: a release always reaches it. */
    private static final Map<UUID, HeroKit.Binding[]> HELD = new HashMap<>();

    static {
        BodyPowers.register(LOGIC);
        MovePowers.register(LOGIC);
        EnergyPowers.register(LOGIC);
        ElementPowers.register(LOGIC);
        MindPowers.register(LOGIC);
    }

    /** The logic of a module (every module has one). */
    public static ModuleLogic logic(PowerModule module) {
        return LOGIC.get(module);
    }

    /** Every module's logic, for the tests' check that none is missing. */
    public static Map<PowerModule, ModuleLogic> all() {
        return Collections.unmodifiableMap(LOGIC);
    }

    /** The context of one of a custom hero's powers, or null if he is not a custom hero with it. */
    @Nullable
    public static ModuleLogic.Ctx ctx(ServerPlayer player, PowerModule module) {
        PowerData data = Powers.get(player);
        if (data.power() != Power.CUSTOM) {
            return null;
        }
        CustomHero hero = CustomHeroes.hero(player);
        CustomHero.Pick pick = hero.pick(module);
        return pick == null ? null : new ModuleLogic.Ctx(player, data, hero, CustomHeroes.state(player), pick, module, hero.keyOf(module));
    }

    @Override
    public Power power() {
        return Power.CUSTOM;
    }

    /** The custom hero's kit (the one on his power data, or built now). */
    static HeroKit kit(ServerPlayer player, PowerData data) {
        HeroKit kit = data.kit();
        return kit != null ? kit : CustomHeroes.state(player).kitFor(CustomHeroes.hero(player));
    }

    /** The key router (see the class comment). {@code column} is the key, 1-5. */
    @Override
    public void onKey(ServerPlayer player, PowerData data, int column, boolean down) {
        if (column < 1 || column > CustomHero.KEYS) {
            return;
        }
        HeroKit kit = kit(player, data);
        HeroKit.Binding binding;
        if (down) {
            binding = kit.binding(column, player.isShiftKeyDown());
            HELD.computeIfAbsent(player.getUUID(), u -> new HeroKit.Binding[CustomHero.KEYS + 1])[column] = binding;
        } else {
            HeroKit.Binding[] held = HELD.get(player.getUUID());
            binding = held != null && held[column] != null ? held[column] : kit.plain(column);
            if (held != null) {
                held[column] = null;
            }
        }
        if (binding == null) {
            if (down) {
                player.displayClientMessage(Component.translatable("message.enhanced.custom.free_key").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        if (down && CustomHeroes.isSuppressed(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.custom.suppressed").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        // 3.6: in his human form (the Hulk's Transform) every key but the Transform itself is refused.
        if (down && data.toggle(Toggle.HUMAN_FORM) && binding.ability() != HeroAbility.HULK_TRANSFORM) {
            player.displayClientMessage(Component.translatable("message.enhanced.hulk.human_form")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        CustomHero hero = kit.hero();
        CustomState state = CustomHeroes.state(player);
        Snap before = down ? new Snap(data, state) : null;
        HeroAbility ability = binding.ability();
        if (ability != null) {
            data.pressing(column, binding.sneak());
            try {
                HeroKeys.press(player, data, ability, down, binding.isAlt());
            } finally {
                data.pressing(0, false);
            }
        } else if (binding.stone() != null) {
            // 3.6: a stone row: its key is the stone's (lane L2), never a hero's.
            StoneRowKeys.press(player, data, binding.stone(), column, down, binding.isAlt());
        } else {
            PowerModule module = binding.module();
            CustomHero.Pick pick = module == null ? null : hero.pick(module);
            if (pick == null) {
                return;
            }
            boolean other = !HeroKit.MAIN.equals(binding.action());
            if (other) {
                pick = pick.withOption(binding.action());
            }
            LOGIC.get(module).key(new ModuleLogic.Ctx(player, data, hero, state, pick, module, column, other, binding.sneak()), down);
        }
        if (before != null && before.changed(data, state)) {
            state.cast(column, binding.sneak());
        }
        CustomHeroes.syncState(player);
    }

    /** What a key may change of the hero, before it acts: if any of it moved, the binding acted. */
    private static final class Snap {
        private final long[] cooldowns = new long[PowerData.ABILITIES + 1];
        private final int toggles;
        private final long live;
        private final int flags;
        private final int on;
        private final boolean field;

        Snap(PowerData data, CustomState state) {
            for (int i = 1; i <= PowerData.ABILITIES; i++) {
                cooldowns[i] = data.cooldownUntil(i);
            }
            toggles = data.toggleBits();
            live = data.liveBits();
            flags = data.flagBits();
            on = state.onBits();
            field = state.field();
        }

        boolean changed(PowerData data, CustomState state) {
            for (int i = 1; i <= PowerData.ABILITIES; i++) {
                if (cooldowns[i] != data.cooldownUntil(i)) {
                    return true;
                }
            }
            return toggles != data.toggleBits() || live != data.liveBits() || flags != data.flagBits() || on != state.onBits()
                    || field != state.field();
        }
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        CustomHero hero = CustomHeroes.hero(player);
        CustomState state = CustomHeroes.state(player);
        HeroKeys.tick(player, data, kit(player, data));
        // 3.6: in the Hulk's Human Form his modules rest (their ends ran when it began).
        for (CustomHero.Pick pick : data.toggle(Toggle.HUMAN_FORM) ? List.<CustomHero.Pick>of() : hero.powers()) {
            PowerModule module = pick.module();
            if (module != null) {
                LOGIC.get(module).tick(new ModuleLogic.Ctx(player, data, hero, state, pick, module, hero.keyOf(module)));
            }
        }
        CustomHeroes.landed(player);
        CustomHeroes.syncState(player);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        // (3.6: in the Hulk's Human Form no module gives anything.)
        CustomHero hero = data.power() == Power.CUSTOM && !data.toggle(Toggle.HUMAN_FORM) ? CustomHeroes.hero(player)
                : null;
        for (Map.Entry<PowerModule, ModuleLogic> entry : LOGIC.entrySet()) {
            entry.getValue().body(player, hero == null ? null : hero.pick(entry.getKey()));
        }
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        endPowers(player, data);
    }

    /** Every power of the hero ends (he is losing the power, or a new definition replaces it), his state is cleared. */
    public static void endPowers(ServerPlayer player, PowerData data) {
        CustomHero hero = CustomHeroes.hero(player);
        CustomState state = CustomHeroes.state(player);
        HeroKeys.endAll(player, data, state.kitFor(hero));
        HELD.remove(player.getUUID());
        for (CustomHero.Pick pick : hero.powers()) {
            PowerModule module = pick.module();
            if (module != null) {
                LOGIC.get(module).end(new ModuleLogic.Ctx(player, data, hero, state, pick, module, hero.keyOf(module)));
            }
        }
        state.reset();
        data.set(Live.SPEEDING, false);
        CustomHeroes.syncState(player);
    }

    @Override
    public void stopHold(PowerData data) {
        // (The held powers, the beam, the flamethrower, the water jet and the Force Field, end at death: onDeath.)
    }

    /** A hero was just made: the switches that start on (Wall-Crawling). */
    static void onBecome(ServerPlayer player, CustomHero hero, CustomState state) {
        if (hero.has(PowerModule.WALL_CRAWL)) {
            state.set(PowerModule.WALL_CRAWL, true);
        }
    }

    @Override
    public void onLogin(ServerPlayer player) {
        if (Powers.is(player, Power.CUSTOM)) {
            onBecome(player, CustomHeroes.hero(player), CustomHeroes.state(player));
            CustomHeroes.syncState(player);
        }
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (Powers.is(player, Power.CUSTOM)) {
            endPowers(player, Powers.get(player));
        }
        for (ModuleLogic logic : LOGIC.values()) {
            logic.forget(player);
        }
        HELD.remove(player.getUUID());
        CustomHeroes.forget(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        if (Powers.is(player, Power.CUSTOM)) {
            endPowers(player, Powers.get(player));
        }
    }

    @Override
    public void onRespawn(ServerPlayer player) {
        if (Powers.is(player, Power.CUSTOM)) {
            onBecome(player, CustomHeroes.hero(player), CustomHeroes.state(player));
            CustomHeroes.syncState(player);
        }
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        if (Powers.is(player, Power.CUSTOM)) {
            PowerData data = Powers.get(player);
            CustomHero hero = CustomHeroes.hero(player);
            CustomState state = CustomHeroes.state(player);
            for (CustomHero.Pick pick : hero.powers()) {
                PowerModule module = pick.module();
                // (What he left in the old world ends; his switches stay as they were.)
                if (module == PowerModule.MIND_CONTROL || module == PowerModule.ANIMAL_CONTROL || module == PowerModule.TECHNOPATHY
                        || module == PowerModule.DUPLICATION || module == PowerModule.MAGIC || module == PowerModule.WEATHER) {
                    LOGIC.get(module).end(new ModuleLogic.Ctx(player, data, hero, state, pick, module, hero.keyOf(module)));
                }
            }
        }
    }

    @Override
    public void onServerStopped() {
        for (ModuleLogic logic : LOGIC.values()) {
            logic.clear();
        }
        HELD.clear();
        CustomHeroes.clear();
    }
}

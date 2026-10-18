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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The server side of the custom hero (3.3), the plug-in of {@code Power.CUSTOM}: its keys go to the module the player
 * put on each, every power he picked ticks, shapes his body, and ends when he loses it. The thirty modules are
 * registered once, by group ({@link BodyPowers}, {@link MovePowers}, {@link EnergyPowers}, {@link ElementPowers},
 * {@link MindPowers}).
 */
public final class CustomHeroServer implements HeroServer {
    private static final Map<PowerModule, ModuleLogic> LOGIC = new EnumMap<>(PowerModule.class);

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

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        CustomHero hero = CustomHeroes.hero(player);
        CustomHero.Pick pick = hero.onKey(ability);
        if (pick == null || pick.module() == null) {
            if (down) {
                player.displayClientMessage(Component.translatable("message.enhanced.custom.free_key").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        if (down && CustomHeroes.isSuppressed(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.custom.suppressed").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        PowerModule module = pick.module();
        LOGIC.get(module).key(new ModuleLogic.Ctx(player, data, hero, CustomHeroes.state(player), pick, module, ability), down);
        CustomHeroes.syncState(player);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        CustomHero hero = CustomHeroes.hero(player);
        CustomState state = CustomHeroes.state(player);
        for (CustomHero.Pick pick : hero.powers()) {
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
        CustomHero hero = data.power() == Power.CUSTOM ? CustomHeroes.hero(player) : null;
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
        CustomHeroes.clear();
    }
}

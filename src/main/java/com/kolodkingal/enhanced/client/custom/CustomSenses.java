package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * What a custom hero's senses show the player himself (3.3), as a source of {@code EntityGlow}: with Super Senses "xray"
 * switched on every creature within its range glows through walls in his glow colour, with Telepathy "sense" every mind
 * (players and creatures that think: mobs) in his main colour.
 */
public final class CustomSenses {
    /** Whether the local player's senses show {@code entity} through walls now. */
    public static boolean reveals(Entity entity) {
        return range(entity) > 0;
    }

    public static int color(Entity entity) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) {
            return 0xFFFFFF;
        }
        CustomHero hero = CustomHeroes.hero(me);
        return CustomHeroes.isOn(me, PowerModule.SUPER_SENSES) && within(me, entity, PowerModule.SUPER_SENSES, ModuleStats.XRAY_RANGE)
                ? hero.glow() : hero.colorOf(hero.pick(PowerModule.TELEPATHY));
    }

    private static double range(Entity entity) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null || entity == me || !(entity instanceof LivingEntity) || !entity.isAlive() || !CustomHeroes.is(me)) {
            return 0;
        }
        if (CustomHeroes.isOn(me, PowerModule.SUPER_SENSES) && CustomHeroes.has(me, PowerModule.SUPER_SENSES, "xray")
                && within(me, entity, PowerModule.SUPER_SENSES, ModuleStats.XRAY_RANGE)) {
            return 1;
        }
        if (CustomHeroes.isOn(me, PowerModule.TELEPATHY) && CustomHeroes.has(me, PowerModule.TELEPATHY, "sense")
                && (entity instanceof Player || entity instanceof Mob) && within(me, entity, PowerModule.TELEPATHY, ModuleStats.SENSE_RANGE)) {
            return 1;
        }
        return 0;
    }

    private static boolean within(LocalPlayer me, Entity entity, PowerModule module, ModuleStats.Stat stat) {
        CustomHero.Pick pick = CustomHeroes.pick(me, module);
        if (pick == null) {
            return false;
        }
        double range = stat.value(pick.strength());
        return entity.distanceToSqr(me) <= range * range;
    }

    private CustomSenses() {
    }
}

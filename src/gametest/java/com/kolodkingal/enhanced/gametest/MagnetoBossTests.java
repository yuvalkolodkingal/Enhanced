package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Magneto's metal against the bosses and the bodies of metal (3.1, D-M 5, 6; spec 18.3): what his strip and his pull do to
 * a fight and to what is metal through and through. Every test uses {@link TestKit#bossFixture} and has a batch of its own
 * ({@code magneto_boss_*}).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class MagnetoBossTests {

    /**
     * M40a. When the metal is the body itself (a Sentry, an Iron Golem, a Vision player with PvP on), V has nothing to tear
     * off: it says to lift it with R ({@code disarm.metal_body}) and starts no cooldown.
     */
    @GameTest(template = ARENA, batch = "magneto_boss_bodies")
    public static void aMetalBodyHasNothingToStrip(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(true);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        PowerData data = Powers.get(magneto);
        ServerPlayer ultron = f.player(18.5, FLOOR, 18.5, Power.ULTRON);
        UltronSentryEntity sentry = UltronSentryEntity.drone(f.level(), helper.absoluteVec(new Vec3(10.5, FLOOR + 1.0, 4.5)),
                ultron.getUUID(), false, false);
        sentry.setHeld(true);
        f.level().addFreshEntity(sentry);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(10.5, FLOOR, 10.5));
        ServerPlayer vision = f.player(10.5, FLOOR, 16.5, Power.VISION);

        for (LivingEntity body : List.<LivingEntity>of(sentry, golem, vision)) {
            float health = body.getHealth();
            lookAt(magneto, body);
            BossTestSupport.lastActionBarKey(magneto);
            AbilityHandler.onKey(magneto, 3, true);
            String said = BossTestSupport.lastActionBarKey(magneto);
            check("message.enhanced.disarm.metal_body".equals(said), name(body) + ": nothing to tear off, the metal is the body (said " + said + ")");
            check(data.isReady(3, helper.getLevel().getGameTime()), name(body) + ": and the strip does not rest");
            check(body.isAlive() && body.getHealth() == health, name(body) + ": nor is it hurt");
        }
        helper.succeed();
    }

    private static String name(Entity entity) {
        return entity.getType().getDescriptionId();
    }

    private MagnetoBossTests() {
    }
}

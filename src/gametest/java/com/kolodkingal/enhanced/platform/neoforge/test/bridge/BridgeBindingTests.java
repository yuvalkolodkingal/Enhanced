package com.kolodkingal.enhanced.platform.neoforge.test.bridge;

import com.kolodkingal.enhanced.gametest.TestKit;
import com.kolodkingal.enhanced.platform.event.Cancellable;
import com.kolodkingal.enhanced.platform.event.EventHub;
import com.kolodkingal.enhanced.platform.event.Events;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.neoforge.bridge.Binding;
import com.kolodkingal.enhanced.platform.neoforge.bridge.NeoForgeBridge;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.platform.neoforge.test.bridge.BridgeTestSupport.BATCH;

/**
 * The NeoForge bridge's bindings (port P0, S-A; moved out of {@code BridgeHubTests} by L8, since they are NeoForge's):
 * an adapted NeoForge event reaches its common hub, and every binding agrees with its NeoForge event.
 */
public final class BridgeBindingTests {
    private BridgeBindingTests() {
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anAdaptedEventIsPostedToItsCommonHub(GameTestHelper helper) {
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 8.5));
        LivingDeathEvent adapted = BridgeTestSupport.adapt(LivingDeathEvent.class,
                new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(zombie,
                        helper.getLevel().damageSources().generic()));
        check(Events.commonType(adapted.getClass()) == LivingDeathEvent.class,
                "the bridge's subclass belongs to the common type");
        List<Object> seen = new ArrayList<>();
        try (EventHub.Registration r = EventHub.of(LivingDeathEvent.class).register(Priority.LOWEST, true,
                e -> {
                    if (e == adapted) {
                        seen.add(e);
                    }
                })) {
            Events.post(adapted);
        }
        check(seen.size() == 1, "Events.post reaches the common type's hub");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyBindingAgreesWithItsNeoForgeEvent(GameTestHelper helper) {
        int bindings = 0;
        for (Binding<?, ?> binding : NeoForgeBridge.bindings()) {
            check(!Modifier.isAbstract(binding.common().getModifiers()), binding.common() + " is concrete");
            check(!Modifier.isAbstract(binding.nativeType().getModifiers()), binding.nativeType() + " is concrete");
            check(binding.cancellable() == Cancellable.class.isAssignableFrom(binding.common()),
                    binding.common().getName() + " is cancellable exactly when " + binding.nativeType().getName()
                            + " is");
            check(binding.common().getPackageName().startsWith("com.kolodkingal.enhanced.platform.event"),
                    binding.common() + " is a common event");
            bindings++;
        }
        check(bindings >= 78, "the server's bindings are installed: " + bindings);
        helper.succeed();
    }
}

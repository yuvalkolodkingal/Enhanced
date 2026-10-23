package com.kolodkingal.enhanced.gametest;

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

import static com.kolodkingal.enhanced.gametest.BridgeTestSupport.BATCH;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The hub's own rules (port P0, S-A): NeoForge's bus semantics without NeoForge (priorities, registration order,
 * cancellation and receiveCanceled, unregistering), and the table of bindings.
 */
public final class BridgeHubTests {
    private BridgeHubTests() {
    }

    /** An event only these tests post (no NeoForge event, so its hub stays off NeoForge's bus). */
    private static final class Probe implements Cancellable {
        private boolean canceled;

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void prioritiesRunInOrderAndListenersInTheirOrder(GameTestHelper helper) {
        EventHub<Probe> hub = EventHub.of(Probe.class);
        List<String> ran = new ArrayList<>();
        List<EventHub.Registration> registrations = new ArrayList<>();
        registrations.add(hub.register(Priority.LOW, e -> ran.add("LOW 1")));
        registrations.add(hub.register(Priority.HIGHEST, e -> ran.add("HIGHEST")));
        registrations.add(hub.register(e -> ran.add("NORMAL 1")));
        registrations.add(hub.register(Priority.LOWEST, e -> ran.add("LOWEST")));
        registrations.add(hub.register(e -> ran.add("NORMAL 2")));
        registrations.add(hub.register(Priority.HIGH, e -> ran.add("HIGH")));
        registrations.add(hub.register(Priority.LOW, e -> ran.add("LOW 2")));
        hub.post(new Probe());
        registrations.forEach(EventHub.Registration::close);
        check(ran.equals(List.of("HIGHEST", "HIGH", "NORMAL 1", "NORMAL 2", "LOW 1", "LOW 2", "LOWEST")),
                "by priority, then in the order registered: " + ran);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCancelledEventSkipsAllButTheListenersThatReceiveIt(GameTestHelper helper) {
        EventHub<Probe> hub = EventHub.of(Probe.class);
        List<String> ran = new ArrayList<>();
        try (EventHub.Registration a = hub.register(Priority.HIGH, false, e -> {
            ran.add("cancel");
            e.setCanceled(true);
        });
             EventHub.Registration b = hub.register(Priority.HIGH, false, e -> ran.add("skipped"));
             EventHub.Registration c = hub.register(Priority.NORMAL, true, e -> ran.add("receives"));
             EventHub.Registration d = hub.register(Priority.LOWEST, false, e -> ran.add("skipped too"))) {
            Probe probe = hub.post(new Probe());
            check(probe.isCanceled(), "the event comes back cancelled");
        }
        check(ran.equals(List.of("cancel", "receives")), "only receiveCanceled runs after a cancel: " + ran);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void closingARegistrationRemovesOnlyThatListener(GameTestHelper helper) {
        EventHub<Probe> hub = EventHub.of(Probe.class);
        List<String> ran = new ArrayList<>();
        EventHub.Registration first = hub.register(e -> ran.add("first"));
        EventHub.Registration second = hub.register(e -> ran.add("second"));
        first.close();
        first.close();
        hub.post(new Probe());
        second.close();
        hub.post(new Probe());
        check(ran.equals(List.of("second")), "the closed listener is gone, the other stays until closed: " + ran);
        check(!hub.hasListeners(Priority.NORMAL), "nothing is left");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void postingOnePriorityRunsOnlyIt(GameTestHelper helper) {
        EventHub<Probe> hub = EventHub.of(Probe.class);
        List<String> ran = new ArrayList<>();
        try (EventHub.Registration a = hub.register(Priority.HIGH, e -> ran.add("HIGH"));
             EventHub.Registration b = hub.register(Priority.LOW, e -> ran.add("LOW"))) {
            hub.post(Priority.LOW, new Probe());
        }
        check(ran.equals(List.of("LOW")), "the bridge's native listener of LOW runs LOW only: " + ran);
        helper.succeed();
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

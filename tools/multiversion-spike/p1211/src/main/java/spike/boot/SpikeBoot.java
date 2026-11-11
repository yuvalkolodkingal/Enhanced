package spike.boot;

import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod("spike")
public class SpikeBoot {
    public SpikeBoot() {
        String mc = ModList.get().getModContainerById("minecraft").get().getModInfo().getVersion().toString();
        String name;
        if (mc.equals("1.21.1")) {
            name = "spike.v1_21_1.Entry";
        } else if (mc.equals("1.21.11")) {
            name = "spike.v1_21_11.Entry";
        } else {
            throw new IllegalStateException("SPIKE unsupported minecraft version " + mc);
        }
        System.out.println("SPIKE boot on " + mc + " -> " + name);
        try {
            Object o = Class.forName(name).getDeclaredConstructor().newInstance();
            System.out.println("SPIKE loaded " + o.getClass().getName());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("SPIKE failed to load " + name, e);
        }
    }
}

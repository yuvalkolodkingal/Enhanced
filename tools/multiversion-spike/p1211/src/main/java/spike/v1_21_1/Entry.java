package spike.v1_21_1;

import net.minecraft.resources.ResourceLocation;

public class Entry {
    public Entry() {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("spike", "a");
        System.out.println("SPIKE entry 1.21.1 ok " + rl);
    }
}

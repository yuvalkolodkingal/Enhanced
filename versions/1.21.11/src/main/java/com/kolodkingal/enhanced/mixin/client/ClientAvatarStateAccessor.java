package com.kolodkingal.enhanced.mixin.client;
//#norenames

import net.minecraft.client.entity.ClientAvatarState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.21.11 only: the cloak's sway and the walk's bob of an avatar (the editor's puppet sets them, as 1.21.1's fields). */
@Mixin(ClientAvatarState.class)
public interface ClientAvatarStateAccessor {
    @Accessor("xCloak")
    double enhanced$xCloak();

    @Accessor("zCloak")
    double enhanced$zCloak();

    @Accessor("xCloak")
    void enhanced$setXCloak(double v);

    @Accessor("yCloak")
    void enhanced$setYCloak(double v);

    @Accessor("zCloak")
    void enhanced$setZCloak(double v);

    @Accessor("xCloakO")
    void enhanced$setXCloakO(double v);

    @Accessor("yCloakO")
    void enhanced$setYCloakO(double v);

    @Accessor("zCloakO")
    void enhanced$setZCloakO(double v);

    @Accessor("bob")
    float enhanced$bob();

    @Accessor("bob")
    void enhanced$setBob(float v);

    @Accessor("bobO")
    void enhanced$setBobO(float v);

    @Accessor("walkDist")
    float enhanced$walkDist();

    @Accessor("walkDist")
    void enhanced$setWalkDist(float v);

    @Accessor("walkDistO")
    void enhanced$setWalkDistO(float v);
}

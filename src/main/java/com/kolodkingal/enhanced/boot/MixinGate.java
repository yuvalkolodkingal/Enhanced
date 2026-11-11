package com.kolodkingal.enhanced.boot;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * The plugin of every copy's mixin config: a copy's mixins apply only when it is the copy of the running Minecraft
 * version (each copy's config names its own package, which says whose it is).
 */
public final class MixinGate implements IMixinConfigPlugin {
    private boolean mine;

    @Override
    public void onLoad(String mixinPackage) {
        String pkg = Versions.packageForRunning();
        mine = pkg != null && mixinPackage.equals(pkg + ".mixin");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return mine;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

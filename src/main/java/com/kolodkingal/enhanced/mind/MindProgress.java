package com.kolodkingal.enhanced.mind;

import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * A player's way through Vision's mind (3.12), kept in the player's own data (it survives death): his plot, whether
 * the robots let him pass, whether the Human Torch gave him the Source Code (then the floors below are gone), and where
 * he came in from.
 */
public final class MindProgress {
    private static final String TAG = "enhanced_mind";
    private final CompoundTag tag;

    private MindProgress(CompoundTag tag) {
        this.tag = tag;
    }

    public static MindProgress of(Player player) {
        CompoundTag data = ModAttachments.PERSISTENT_DATA.get(player);
        if (!data.contains(TAG)) {
            data.put(TAG, new CompoundTag());
        }
        return new MindProgress(data.getCompound(TAG));
    }

    public int plot() {
        return tag.contains("Plot") ? tag.getInt("Plot") : -1;
    }

    public void setPlot(int plot) {
        tag.putInt("Plot", plot);
    }

    public boolean gateOpen() {
        return tag.getBoolean("Gate");
    }

    public void setGateOpen(boolean open) {
        tag.putBoolean("Gate", open);
    }

    public boolean torchDone() {
        return tag.getBoolean("Torch");
    }

    public void setTorchDone(boolean done) {
        tag.putBoolean("Torch", done);
    }

    /** Where he came in from: the dimension's id, the place and the turn. */
    public void setReturn(String dimension, double x, double y, double z, float yaw) {
        CompoundTag back = new CompoundTag();
        back.putString("Dim", dimension);
        back.putDouble("X", x);
        back.putDouble("Y", y);
        back.putDouble("Z", z);
        back.putFloat("Yaw", yaw);
        tag.put("Return", back);
    }

    public CompoundTag returnPoint() {
        return tag.getCompound("Return");
    }

    public boolean hasReturn() {
        return tag.contains("Return");
    }

    public void clearReturn() {
        tag.remove("Return");
    }
}

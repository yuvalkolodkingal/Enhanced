package com.kolodkingal.enhanced.compat;

import com.kolodkingal.enhanced.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.21.4 only: from 1.21.2 a humanoid's hat is a child of its head, the jacket of the body, the sleeves of the arms and
 * the trousers of the legs; 1.21.1's meshes have them at the root. This moves them, with their pose made relative to
 * the new parent, so the mod's own humanoid meshes bake into 1.21.4's models.
 */
public final class Meshes {
    private static final String[][] MOVES = {{"hat", "head"}, {"jacket", "body"}, {"left_sleeve", "left_arm"},
            {"right_sleeve", "right_arm"}, {"left_pants", "left_leg"}, {"right_pants", "right_leg"}};

    private Meshes() {
    }

    public static ModelPart humanoid(ModelPart root) {
        Map<String, ModelPart> top = new LinkedHashMap<>(((ModelPartAccessor) (Object) root).enhanced$children());
        boolean changed = false;
        for (String[] move : MOVES) {
            ModelPart child = top.get(move[0]);
            ModelPart parent = top.get(move[1]);
            if (child == null || parent == null) {
                continue;
            }
            top.remove(move[0]);
            PartPose p = parent.getInitialPose();
            PartPose c = child.getInitialPose();
            PartPose relative = PartPose.offsetAndRotation(c.x() - p.x(), c.y() - p.y(), c.z() - p.z(),
                    c.xRot() - p.xRot(), c.yRot() - p.yRot(), c.zRot() - p.zRot());
            child.setInitialPose(relative);
            child.loadPose(relative);
            ModelPartAccessor a = (ModelPartAccessor) (Object) parent;
            Map<String, ModelPart> kids = new LinkedHashMap<>(a.enhanced$children());
            kids.put(move[0], child);
            ModelPart rebuilt = new ModelPart(a.enhanced$cubes(), kids);
            rebuilt.setInitialPose(p);
            rebuilt.loadPose(p);
            top.put(move[1], rebuilt);
            changed = true;
        }
        if (!changed) {
            return root;
        }
        ModelPart out = new ModelPart(((ModelPartAccessor) (Object) root).enhanced$cubes(), top);
        out.setInitialPose(root.getInitialPose());
        out.loadPose(root.getInitialPose());
        return out;
    }
}

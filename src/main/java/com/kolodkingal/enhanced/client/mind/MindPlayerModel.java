package com.kolodkingal.enhanced.client.mind;

import com.kolodkingal.enhanced.mind.MindNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** The player model of the mind's people (3.12): Ultron on his sofa and the Human Torch in his armchair sit. */
public class MindPlayerModel extends PlayerModel<MindNpcEntity> {
    public MindPlayerModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(MindNpcEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch) {
        riding = entity.role() == MindNpcEntity.Role.ULTRON || entity.role() == MindNpcEntity.Role.TORCH && !entity.ablaze();
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity.role() == MindNpcEntity.Role.TORCH && entity.ablaze()) {
            // arms a little out from the body, the flames rising off them
            rightArm.zRot += 0.35F;
            leftArm.zRot -= 0.35F;
        }
    }
}

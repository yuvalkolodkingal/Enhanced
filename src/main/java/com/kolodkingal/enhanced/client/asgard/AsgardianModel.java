package com.kolodkingal.enhanced.client.asgard;

import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * The body of an Asgardian (3.5): the player model (wide or slim arms, as the role's skin is), posed by
 * {@link AsgardianPoses} from the synced state after the vanilla walk and swing.
 */
public class AsgardianModel extends PlayerModel<AsgardianEntity> {
    private float partialTick;

    public AsgardianModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void prepareMobModel(AsgardianEntity npc, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(npc, limbSwing, limbSwingAmount, partialTick);
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(AsgardianEntity npc, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        AsgardianPoses.before(this, npc, partialTick);
        super.setupAnim(npc, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        AsgardianPoses.after(this, npc, ageInTicks, partialTick);
    }
}

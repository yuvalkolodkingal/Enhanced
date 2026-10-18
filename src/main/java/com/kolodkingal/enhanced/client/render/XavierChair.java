package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.XavierAbilities;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The old Professor X's wheelchair and Cerebro's helmet (3.3), drawn as a layer of the player: the chair is his, it goes
 * where he goes (he sits in it, the stance {@link Stance#XAVIER_SEATED}), and its two big wheels turn with how far each
 * side of it has rolled (forward, back, and against each other when he turns on the spot), eased between ticks so they
 * never snap. The helmet is on his head while Cerebro searches ({@link Live#CEREBRO}), whichever version he is.
 *
 * <p>The chair (original art, after the professor's chair of the films: a polished steel frame, black leather, two large
 * spoked wheels with black tyres, small casters and a footrest) is a model of its own, in the player model's pixels
 * (the ground at y = 24, the front toward -z), {@code textures/entity/xavier_chair.png}; the helmet is drawn round the
 * head, {@code textures/entity/cerebro_helmet.png}. Neither is drawn when he swims, sleeps, glides on an elytra or rides
 * something else.
 */
public final class XavierChair extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("xavier_chair"), "main");
    public static final ModelLayerLocation HELMET_LAYER = new ModelLayerLocation(Enhanced.id("cerebro_helmet"), "main");
    private static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/xavier_chair.png");
    private static final ResourceLocation HELMET_TEXTURE = Enhanced.id("textures/entity/cerebro_helmet.png");

    /** The radius of the big wheels (blocks), and half the distance between them. */
    static final float WHEEL_RADIUS = 5.5F / 16.0F;
    static final float HALF_TRACK = 6.6F / 16.0F;
    /** The tyre is this many segments. */
    private static final int TYRE_SEGMENTS = 12;

    private final ModelPart chair;
    private final ModelPart leftWheel;
    private final ModelPart rightWheel;
    private final ModelPart helmet;

    public XavierChair(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart chair, ModelPart helmet) {
        super(parent);
        this.chair = chair;
        this.leftWheel = chair.getChild("left_wheel");
        this.rightWheel = chair.getChild("right_wheel");
        this.helmet = helmet;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The model

    public static LayerDefinition createChairLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The leather (rows 32..51 of the texture): the seat and the back.
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(0, 32).addBox(-5.0F, 13.9F, -6.0F, 10.0F, 1.6F, 9.5F), PartPose.ZERO);
        root.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 44).addBox(-5.0F, 2.5F, 3.0F, 10.0F, 11.5F, 1.2F), PartPose.ZERO);
        // The steel (rows 0..15): the frame, the handles, the arm posts, the footrest's tubes.
        PartDefinition frame = root.addOrReplaceChild("frame", CubeListBuilder.create()
                // under the seat, along each side
                .texOffs(0, 0).addBox(-5.6F, 15.5F, -6.5F, 1.0F, 1.0F, 10.5F)
                .texOffs(0, 0).addBox(4.6F, 15.5F, -6.5F, 1.0F, 1.0F, 10.5F)
                // the posts of the back, up to the handles
                .texOffs(24, 0).addBox(-5.4F, 1.0F, 4.2F, 1.0F, 14.5F, 1.0F)
                .texOffs(24, 0).addBox(4.4F, 1.0F, 4.2F, 1.0F, 14.5F, 1.0F)
                // the cross bar under the seat
                .texOffs(0, 12).addBox(-4.6F, 15.8F, 2.0F, 9.2F, 0.8F, 0.8F)
                // the caster posts
                .texOffs(30, 0).addBox(-4.9F, 16.5F, -6.6F, 0.8F, 4.6F, 0.8F)
                .texOffs(30, 0).addBox(4.1F, 16.5F, -6.6F, 0.8F, 4.6F, 0.8F)
                // the posts of the armrests
                .texOffs(34, 0).addBox(-6.1F, 9.6F, -3.2F, 0.8F, 6.0F, 0.8F)
                .texOffs(34, 0).addBox(5.3F, 9.6F, -3.2F, 0.8F, 6.0F, 0.8F)
                .texOffs(34, 0).addBox(-6.1F, 9.6F, 2.6F, 0.8F, 6.0F, 0.8F)
                .texOffs(34, 0).addBox(5.3F, 9.6F, 2.6F, 0.8F, 6.0F, 0.8F), PartPose.ZERO);
        // The footrest's tubes, slanting down and forward from the front of the seat.
        frame.addOrReplaceChild("foot_tubes", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-4.4F, 0.0F, -0.4F, 0.8F, 6.2F, 0.8F)
                .texOffs(40, 0).addBox(3.6F, 0.0F, -0.4F, 0.8F, 6.2F, 0.8F),
                PartPose.offsetAndRotation(0.0F, 15.6F, -6.4F, 0.5F, 0.0F, 0.0F));
        // The rubber (rows 16..31): the pads of the armrests, the handle grips, the footrest plate, the casters.
        root.addOrReplaceChild("rubber", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-6.4F, 8.6F, -4.2F, 1.4F, 1.0F, 8.0F)
                .texOffs(0, 16).addBox(5.0F, 8.6F, -4.2F, 1.4F, 1.0F, 8.0F)
                .texOffs(20, 16).addBox(-5.4F, 0.8F, 5.2F, 1.0F, 1.0F, 2.6F)
                .texOffs(20, 16).addBox(4.4F, 0.8F, 5.2F, 1.0F, 1.0F, 2.6F)
                .texOffs(30, 16).addBox(-4.4F, 20.6F, -11.4F, 8.8F, 0.6F, 3.4F)
                .texOffs(0, 26).addBox(-5.0F, 21.0F, -7.8F, 1.0F, 3.0F, 3.0F)
                .texOffs(0, 26).addBox(4.0F, 21.0F, -7.8F, 1.0F, 3.0F, 3.0F), PartPose.ZERO);
        wheel(root, "left_wheel", 6.7F);
        wheel(root, "right_wheel", -6.7F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** A big wheel: a tyre of segments, twelve spokes, a hub and a hand rim, turning about its axle at (x, 18.5, 0.5). */
    private static void wheel(PartDefinition root, String name, float x) {
        PartDefinition wheel = root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(44, 0).addBox(-0.9F, -0.9F, -0.9F, 1.8F, 1.8F, 1.8F), PartPose.offset(x, 18.5F, 0.5F));
        float side = Math.signum(x);
        for (int i = 0; i < TYRE_SEGMENTS; i++) {
            float a = i * Mth.TWO_PI / TYRE_SEGMENTS;
            wheel.addOrReplaceChild("tyre" + i, CubeListBuilder.create().texOffs(36, 16).addBox(-0.7F, -5.6F, -1.5F, 1.4F, 1.3F, 3.0F),
                    PartPose.rotation(a, 0.0F, 0.0F));
            wheel.addOrReplaceChild("rim" + i, CubeListBuilder.create().texOffs(48, 0).addBox(0.75F * side - 0.2F, -4.6F, -1.2F, 0.4F, 0.4F, 2.4F),
                    PartPose.rotation(a + Mth.PI / TYRE_SEGMENTS, 0.0F, 0.0F));
        }
        for (int i = 0; i < 6; i++) {
            wheel.addOrReplaceChild("spoke" + i, CubeListBuilder.create().texOffs(52, 0).addBox(-0.15F, -4.4F, -0.15F, 0.3F, 8.8F, 0.3F),
                    PartPose.rotation(i * Mth.PI / 6.0F, 0.0F, 0.0F));
        }
    }

    public static LayerDefinition createHelmetLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // A band of steel round the brow, two arches over the crown, struts between, and the glowing nodes at the temples
        // and on the crown (head space: the head is the box -4..4, -8..0).
        root.addOrReplaceChild("band", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.7F, -6.4F, -4.7F, 9.4F, 1.4F, 0.6F)
                .texOffs(0, 0).addBox(-4.7F, -6.4F, 4.1F, 9.4F, 1.4F, 0.6F)
                .texOffs(0, 4).addBox(-4.7F, -6.4F, -4.1F, 0.6F, 1.4F, 8.2F)
                .texOffs(0, 4).addBox(4.1F, -6.4F, -4.1F, 0.6F, 1.4F, 8.2F)
                .texOffs(0, 16).addBox(-0.5F, -8.8F, -4.7F, 1.0F, 0.6F, 9.4F)
                .texOffs(0, 16).addBox(-4.7F, -8.8F, -0.5F, 9.4F, 0.6F, 1.0F)
                .texOffs(24, 0).addBox(-0.5F, -8.6F, -4.7F, 1.0F, 2.4F, 0.6F)
                .texOffs(24, 0).addBox(-0.5F, -8.6F, 4.1F, 1.0F, 2.4F, 0.6F)
                .texOffs(24, 0).addBox(-4.7F, -8.6F, -0.5F, 0.6F, 2.4F, 1.0F)
                .texOffs(24, 0).addBox(4.1F, -8.6F, -0.5F, 0.6F, 2.4F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("nodes", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-5.1F, -6.6F, -1.6F, 0.8F, 1.8F, 1.8F)
                .texOffs(32, 0).addBox(4.3F, -6.6F, -1.6F, 0.8F, 1.8F, 1.8F)
                .texOffs(32, 0).addBox(-0.9F, -9.2F, -0.9F, 1.8F, 0.8F, 1.8F)
                .texOffs(32, 0).addBox(-1.4F, -6.6F, -5.1F, 2.8F, 1.8F, 0.8F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    /** Whether this player sits in the chair now (see {@link XavierAbilities#sits}). */
    public static boolean sits(Player player) {
        return XavierAbilities.sits(player);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        PowerData data = Powers.get(player);
        if (data.power() != Power.PROFESSOR_X) {
            return;
        }
        if (sits(player)) {
            poseStack.pushPose();
            if (player.isCrouching()) {
                // A crouching player is drawn an eighth of a block lower; the chair stays on the ground.
                poseStack.translate(0.0F, -0.125F, 0.0F);
            }
            Wheels w = WHEELS.get(player.getId());
            leftWheel.xRot = w == null ? 0.0F : Mth.lerp(partialTick, w.leftBefore, w.left);
            rightWheel.xRot = w == null ? 0.0F : Mth.lerp(partialTick, w.rightBefore, w.right);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            chair.render(poseStack, vc, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        if (data.is(Live.CEREBRO)) {
            poseStack.pushPose();
            getParentModel().head.translateAndRotate(poseStack);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(HELMET_TEXTURE));
            // Its steel catches the light of its own nodes: drawn full bright.
            helmet.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The wheels

    /** The turn of the two wheels (radians), this tick and the last, and where the chair was. */
    static final class Wheels {
        float left;
        float right;
        float leftBefore;
        float rightBefore;
        double x;
        double z;
        float yaw;
    }

    private static final Map<Integer, Wheels> WHEELS = new HashMap<>();

    /**
     * How far each wheel rolls (radians) when the chair moves {@code forward} blocks along where it faces and turns by
     * {@code turnDegrees} (clockwise seen from above): the left wheel rolls on the outside of a right turn.
     */
    static float[] roll(double forward, float turnDegrees) {
        double turn = turnDegrees * Mth.DEG_TO_RAD * HALF_TRACK;
        return new float[]{(float) ((forward + turn) / WHEEL_RADIUS), (float) ((forward - turn) / WHEEL_RADIUS)};
    }

    /** Once a client tick: every chair in the world rolls by what its owner moved. */
    static void tick(ClientLevel level) {
        for (Player player : level.players()) {
            if (!sits(player)) {
                continue;
            }
            Wheels w = WHEELS.computeIfAbsent(player.getId(), id -> {
                Wheels n = new Wheels();
                n.x = player.getX();
                n.z = player.getZ();
                n.yaw = player.yBodyRot;
                return n;
            });
            double dx = player.getX() - w.x;
            double dz = player.getZ() - w.z;
            float yaw = player.yBodyRot;
            double forward = -dx * Mth.sin(yaw * Mth.DEG_TO_RAD) + dz * Mth.cos(yaw * Mth.DEG_TO_RAD);
            // A jump of more than a few blocks (a teleport) does not spin the wheels.
            if (dx * dx + dz * dz > 16.0) {
                forward = 0.0;
            }
            float[] r = roll(forward, Mth.wrapDegrees(yaw - w.yaw));
            w.leftBefore = w.left;
            w.rightBefore = w.right;
            w.left += r[0];
            w.right += r[1];
            // Kept small, so the floats never lose their fractions (the before and after are moved together).
            if (Math.abs(w.left) > 1000.0F) {
                float k = Mth.TWO_PI * (float) Math.floor(w.left / Mth.TWO_PI);
                w.left -= k;
                w.leftBefore -= k;
            }
            if (Math.abs(w.right) > 1000.0F) {
                float k = Mth.TWO_PI * (float) Math.floor(w.right / Mth.TWO_PI);
                w.right -= k;
                w.rightBefore -= k;
            }
            w.x = player.getX();
            w.z = player.getZ();
            w.yaw = yaw;
        }
        Iterator<Map.Entry<Integer, Wheels>> it = WHEELS.entrySet().iterator();
        while (it.hasNext()) {
            if (!(level.getEntity(it.next().getKey()) instanceof Player p) || !sits(p)) {
                it.remove();
            }
        }
    }

    static void clear() {
        WHEELS.clear();
    }
}
